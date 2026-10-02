package ir.bazaaryar.app.data

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
    val title: String,
    val impact: Int,
    val previous: String = "",
    val forecast: String = "",
)

// ---- Price alerts ----

enum class AlertWindow(val minutes: Int, val label: String) {
    M5(5, "۵ دقیقه"), M15(15, "۱۵ دقیقه"), H1(60, "۱ ساعت"), H24(1440, "۲۴ ساعت"),
}

enum class AlertScope(val label: String) { WATCHLIST("واچ‌لیست من"), ALL("همه‌ی ۲۰۰ ارز") }

enum class AlertDirection(val label: String) { BOTH("رشد و ریزش"), UP("فقط رشد"), DOWN("فقط ریزش") }

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
