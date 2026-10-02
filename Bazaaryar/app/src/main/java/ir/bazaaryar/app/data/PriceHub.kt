package ir.bazaaryar.app.data

import android.content.Context
import ir.bazaaryar.app.notify.PriceAlerts
import ir.bazaaryar.app.ui.L
import ir.bazaaryar.app.ui.tr
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import kotlin.math.abs

/**
 * Single source of truth for prices, shared by the UI and the background alert service.
 *
 * - CoinGecko: top 200 list, ranks, caps, 7d sparkline (60 s while the UI is open, 5 min in background).
 * - Binance market-data WebSocket: tick prices (~1 s) for the coins we need.
 * - Nobitex (fallback Wallex): USDT/Toman every 10 s, plus coin/USDT prices when Binance is unreachable.
 *
 * Runs only while someone holds it (visible activity and/or [ir.bazaaryar.app.notify.LiveService]).
 */
object PriceHub {
    private const val GECKO_UI = 60_000L
    private const val GECKO_BG = 5 * 60_000L
    private const val GECKO_429 = 2 * 60_000L
    private const val IRAN_EVERY = 10_000L
    private const val OVERVIEW_EVERY = 5 * 60_000L
    private const val SAMPLE_EVERY = 5_000L
    private const val WS_LIVE_MS = 15_000L
    private const val WS_QUOTE_MAX_AGE = 10 * 60_000L

    private val SYMBOL_OK = Regex("^[A-Z0-9]{2,15}$")

    private val _state = MutableStateFlow(MarketState())
    val state: StateFlow<MarketState> = _state.asStateFlow()
    val history = PriceHistory()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val kick = Channel<Unit>(Channel.CONFLATED)
    private var job: Job? = null
    private var users = 0
    @Volatile private var uiUsers = 0

    // Guarded by `this`.
    private var base: List<Coin> = emptyList()
    private val binance = HashMap<String, LiveQuote>()
    private var iran: Map<String, LiveQuote> = emptyMap()
    private var iranAt = 0L
    private var usdt: UsdtRate? = null
    private var overview: MarketOverview? = null
    private var error: String? = null
    private var geckoAt = 0L
    @Volatile private var lastWsAt = 0L
    private var dirty = true

    @Volatile private var settings = AlertSettings()
    @Volatile private var rules: Map<String, CoinRule> = emptyMap()
    @Volatile private var watch: Set<String> = emptySet()

    @Synchronized
    fun acquire(context: Context, ui: Boolean) {
        users++
        if (ui) uiUsers++
        if (users == 1) start(context.applicationContext) else if (ui) kick.trySend(Unit)
    }

    @Synchronized
    fun release(ui: Boolean) {
        if (users == 0) return
        users--
        if (ui && uiUsers > 0) uiUsers--
        if (users == 0) {
            job?.cancel()
            job = null
        }
    }

    fun refreshNow() {
        kick.trySend(Unit)
    }

    private val wsLive: Boolean get() = System.currentTimeMillis() - lastWsAt < WS_LIVE_MS

    private fun start(app: Context) {
        val prefs = Prefs(app)
        job = scope.launch {
            launch { prefs.alertSettings.collect { settings = it } }
            launch { prefs.rules.collect { rules = it } }
            launch { prefs.watch.collect { watch = it } }
            // Background-only runs (service after reboot) still need the user's language for alert text.
            launch {
                prefs.look.collect {
                    L.lang = it.lang
                    synchronized(this@PriceHub) { dirty = true }
                }
            }
            launch { geckoLoop() }
            launch { overviewLoop() }
            launch { iranLoop() }
            launch { binanceLoop() }
            launch { publishLoop(app, prefs) }
        }
    }

    private suspend fun geckoLoop() {
        while (true) {
            val ui = uiUsers > 0
            var wait = if (ui) GECKO_UI else GECKO_BG
            _state.update { it.copy(loading = true) }
            try {
                // Background polls skip the sparkline: ~6x smaller payload.
                val fresh = CoinRepository.fetchTop(sparkline = ui)
                synchronized(this) {
                    base = if (ui) {
                        fresh
                    } else {
                        val oldSpark: Map<String, List<Double>> = base.associate { it.id to it.spark }
                        fresh.map { c -> c.copy(spark = oldSpark[c.id] ?: emptyList()) }
                    }
                    geckoAt = System.currentTimeMillis()
                    error = null
                    dirty = true
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                synchronized(this) {
                    error = e.message ?: tr("خطای شبکه", "Network error")
                    dirty = true
                }
                if ((e.cause as? Net.HttpError)?.code == 429) wait = GECKO_429
            }
            _state.update { it.copy(loading = false) }
            withTimeoutOrNull(wait) { kick.receive() }
        }
    }

    private suspend fun overviewLoop() {
        while (true) {
            val ov = CoinRepository.fetchOverview()
            if (ov != null) synchronized(this) {
                overview = ov
                dirty = true
            }
            delay(OVERVIEW_EVERY)
        }
    }

    private suspend fun iranLoop() {
        while (true) {
            try {
                val snap = IranMarket.fetch(withCoins = !wsLive)
                synchronized(this) {
                    if (snap.usdt != null) usdt = snap.usdt
                    if (snap.quotes.isNotEmpty()) {
                        iran = snap.quotes
                        iranAt = System.currentTimeMillis()
                    }
                    dirty = true
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Keep the last known rate; the UI shows its age.
            }
            delay(IRAN_EVERY)
        }
    }

    private class Resubscribe : Exception()

    /** Symbols to stream: everything while the UI is open, otherwise only what alerts need. */
    private fun wantedSymbols(): Set<String> = synchronized(this) {
        val coins = if (uiUsers > 0 || settings.scope == AlertScope.ALL) {
            base
        } else {
            val ids = watch + rules.keys
            base.filter { it.id in ids }
        }
        coins.mapTo(HashSet<String>()) { it.symbol }.filterTo(HashSet<String>()) { it != "USDT" && SYMBOL_OK.matches(it) }
    }

    private suspend fun binanceLoop() {
        var backoff = 2_000L
        while (true) {
            val wanted = wantedSymbols()
            if (wanted.isEmpty()) {
                delay(2_000)
                continue
            }
            try {
                coroutineScope {
                    launch {
                        while (true) {
                            delay(10_000)
                            if (wantedSymbols() != wanted) throw Resubscribe()
                        }
                    }
                    BinanceStream.ticks(wanted).collect { batch ->
                        backoff = 2_000L
                        synchronized(this@PriceHub) {
                            binance.putAll(batch)
                            lastWsAt = System.currentTimeMillis()
                            dirty = true
                        }
                    }
                    throw IOException("stream closed")
                }
            } catch (e: Resubscribe) {
                continue
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Blocked / offline: Nobitex takes over as the live source until we reconnect.
            }
            delay(backoff)
            backoff = (backoff * 2).coerceAtMost(60_000L)
        }
    }

    private suspend fun publishLoop(app: Context, prefs: Prefs) {
        var lastSample = 0L
        while (true) {
            delay(1_000)
            val snap = synchronized(this) {
                if (!dirty) {
                    null
                } else {
                    dirty = false
                    compose()
                }
            } ?: continue
            _state.value = snap
            val now = System.currentTimeMillis()
            if (now - lastSample >= SAMPLE_EVERY) {
                lastSample = now
                for (c in snap.coins) history.add(c.id, now, c.price)
            }
            PriceAlerts.evaluate(app, snap, history, settings, rules, watch) { id, above ->
                scope.launch { prefs.clearTarget(id, above) }
            }
        }
    }

    /** Merges CoinGecko's list with the freshest live quote per symbol. Call while holding the lock. */
    private fun compose(): MarketState {
        val now = System.currentTimeMillis()
        val ws = now - lastWsAt < WS_LIVE_MS
        val iranFresh = now - iranAt < 3 * IRAN_EVERY
        val seen = HashSet<String>()
        val coins = ArrayList<Coin>(base.size + 1)
        usdt?.let { coins += usdtCoin(it) }
        for (c in base) {
            // Several CoinGecko coins can share a ticker: only the highest-ranked one gets live prices.
            if (!seen.add(c.symbol)) {
                coins += c
                continue
            }
            val b = if (ws) binance[c.symbol]?.takeIf { now - it.at < WS_QUOTE_MAX_AGE } else null
            val n = if (iranFresh) iran[c.symbol] else null
            val q = b ?: n
            // Sanity check against CoinGecko, so a same-ticker different token can't hijack the price.
            if (q != null && c.price > 0 && abs(q.price / c.price - 1) < 0.3) {
                coins += c.copy(
                    price = q.price,
                    change24h = q.change24h,
                    source = if (q === b) Source.BINANCE else Source.NOBITEX,
                    tickAt = q.at,
                )
            } else {
                coins += c
            }
        }
        val mode = when {
            ws -> LiveMode.BINANCE
            iranFresh && iran.isNotEmpty() -> LiveMode.NOBITEX
            base.isNotEmpty() -> LiveMode.POLLING
            else -> LiveMode.CONNECTING
        }
        return MarketState(
            coins = coins,
            usdt = usdt,
            overview = overview,
            loading = _state.value.loading,
            error = error,
            updatedAt = if (mode == LiveMode.BINANCE || mode == LiveMode.NOBITEX) now else geckoAt,
            live = mode,
        )
    }

    private fun usdtCoin(u: UsdtRate) = Coin(
        id = USDT_IRT_ID,
        rank = 0,
        symbol = "USDT",
        name = tr("تتر به تومان", "USDT in Toman") + " · " + u.source,
        price = u.toman,
        change24h = u.change24h,
        marketCap = 0.0,
        high24h = u.high,
        low24h = u.low,
        spark = history.series(USDT_IRT_ID),
        isToman = true,
        source = Source.NOBITEX,
        tickAt = u.at,
    )
}
