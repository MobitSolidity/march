package ir.bazaaryar.app.data

data class Coin(
    val id: String,
    val rank: Int,
    val symbol: String,
    val name: String,
    val price: Double,
    val change24h: Double,
    val marketCap: Double,
    val spark: List<Double>,
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
