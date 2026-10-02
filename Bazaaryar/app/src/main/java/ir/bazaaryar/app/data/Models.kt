package ir.bazaaryar.app.data

import ir.bazaaryar.app.ui.tr

/** Synthetic coin id for USDT priced in Toman on Iranian exchanges. */
const val USDT_IRT_ID = "usdt-irt"

enum class Source { COINGECKO, NOBITEX, BINANCE }

data class Coin(
    val id: String,
    val rank: Int,
    val symbol: String,
    val name: String,
    /** USD, except when [isToman] (USDT/IRT) where it is Toman. */
    val price: Double,
    val change24h: Double,
    val marketCap: Double,
    val volume24h: Double = 0.0,
    val high24h: Double = 0.0,
    val low24h: Double = 0.0,
    val change7d: Double = 0.0,
    val ath: Double = 0.0,
    val athChange: Double = 0.0,
    /** 7 days, hourly (CoinGecko). For USDT/IRT: samples collected this session. */
    val spark: List<Double> = emptyList(),
    val isToman: Boolean = false,
    val source: Source = Source.COINGECKO,
    val tickAt: Long = 0L,
)

data class LiveQuote(val price: Double, val change24h: Double, val at: Long)

data class UsdtRate(
    val toman: Double,
    val change24h: Double,
    val high: Double,
    val low: Double,
    val bestBuy: Double,
    val bestSell: Double,
    val source: String,
    val at: Long,
)

data class MarketOverview(
    val totalCap: Double,
    val capChange24h: Double,
    val btcDominance: Double,
    val fearGreed: Int? = null,
    /** Raw alternative.me classification ("Extreme Fear" ... "Extreme Greed"), translated in the UI. */
    val fearLabel: String = "",
)

enum class LiveMode { CONNECTING, BINANCE, NOBITEX, POLLING }

data class MarketState(
    val coins: List<Coin> = emptyList(),
    val usdt: UsdtRate? = null,
    val overview: MarketOverview? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val updatedAt: Long = 0L,
    val live: LiveMode = LiveMode.CONNECTING,
)

data class EconEvent(
    val id: String,
    val at: Long,
    val country: String,
    /** Persian title. */
    val title: String,
    val impact: Int,
    val previous: String = "",
    val forecast: String = "",
    /** English title. */
    val en: String = "",
) {
    /** Title in the current app language. */
    val label: String get() = tr(title, en.ifEmpty { title })
}

// ---- Price alerts ----

enum class AlertWindow(val minutes: Int, private val faL: String, private val enL: String) {
    M5(5, "۵ دقیقه", "5 min"),
    M15(15, "۱۵ دقیقه", "15 min"),
    H1(60, "۱ ساعت", "1 hour"),
    H24(1440, "۲۴ ساعت", "24 hours");

    val label: String get() = tr(faL, enL)
}

enum class AlertScope(private val faL: String, private val enL: String) {
    WATCHLIST("واچ‌لیست من", "My watchlist"),
    ALL("همه‌ی ۲۰۰ ارز", "All 200 coins");

    val label: String get() = tr(faL, enL)
}

enum class AlertDirection(private val faL: String, private val enL: String) {
    BOTH("رشد و ریزش", "Up & down"),
    UP("فقط رشد", "Up only"),
    DOWN("فقط ریزش", "Down only");

    val label: String get() = tr(faL, enL)
}

/** How hard an alert tries to get your attention. */
enum class RingMode(private val faL: String, private val enL: String) {
    OFF("فقط اعلان", "Notification only"),
    TARGETS("زنگ برای قیمت هدف", "Ring on targets"),
    ALL("زنگ برای همه", "Ring on everything");

    val label: String get() = tr(faL, enL)
}

data class AlertSettings(
    val enabled: Boolean = true,
    val thresholdPct: Float = 5f,
    val window: AlertWindow = AlertWindow.M15,
    val scope: AlertScope = AlertScope.WATCHLIST,
    val direction: AlertDirection = AlertDirection.BOTH,
    val cooldownMin: Int = 30,
    /** Keep a foreground service alive so alerts fire while the app is closed. */
    val background: Boolean = true,
    val includeUsdt: Boolean = true,
    /** Used to personalise notification text. */
    val name: String = "",
    /** Ring like an alarm clock (full screen, alarm stream, vibration) instead of a plain notification. */
    val ring: RingMode = RingMode.ALL,
)

/** Per-coin overrides: own % threshold and one-shot target prices (USD, or Toman for USDT/IRT). */
data class CoinRule(
    val id: String,
    val pct: Float? = null,
    val above: Double? = null,
    val below: Double? = null,
) {
    val isEmpty: Boolean get() = pct == null && above == null && below == null
}
