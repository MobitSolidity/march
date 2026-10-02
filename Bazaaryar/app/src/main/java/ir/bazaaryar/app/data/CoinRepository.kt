package ir.bazaaryar.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Top-200 list, ranks, caps and 7d sparkline from the free CoinGecko API, plus market overview. */
object CoinRepository {
    const val TOP_N = 200
    private const val BASE = "https://api.coingecko.com/api/v3"

    private fun marketsUrl(sparkline: Boolean) =
        "$BASE/coins/markets?vs_currency=usd&order=market_cap_desc&per_page=$TOP_N&page=1" +
            "&sparkline=$sparkline&price_change_percentage=24h,7d"

    suspend fun fetchTop(sparkline: Boolean = true): List<Coin> = withContext(Dispatchers.IO) {
        retrying("CoinGecko") { parseMarkets(Net.get(marketsUrl(sparkline))) }
    }

    /** Total cap, BTC dominance and the Fear & Greed index. Null if both sources fail. */
    suspend fun fetchOverview(): MarketOverview? = withContext(Dispatchers.IO) {
        val global = runCatching { parseGlobal(Net.get("$BASE/global")) }.getOrNull()
        val fng = runCatching { parseFng(Net.get("https://api.alternative.me/fng/?limit=1")) }.getOrNull()
        if (global == null && fng == null) {
            null
        } else {
            (global ?: MarketOverview(0.0, 0.0, 0.0)).copy(fearGreed = fng?.first, fearLabel = fng?.second ?: "")
        }
    }

    private fun parseMarkets(json: String): List<Coin> {
        val arr = JSONArray(json)
        val now = System.currentTimeMillis()
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            val sparkArr = o.optJSONObject("sparkline_in_7d")?.optJSONArray("price")
            val spark = if (sparkArr == null) emptyList() else
                List(sparkArr.length()) { k -> sparkArr.optDouble(k) }.filter { !it.isNaN() }
            Coin(
                id = o.getString("id"),
                rank = o.optInt("market_cap_rank", i + 1).takeIf { it > 0 } ?: (i + 1),
                symbol = o.getString("symbol").uppercase(),
                name = o.getString("name"),
                price = o.num("current_price") ?: 0.0,
                change24h = o.num("price_change_percentage_24h") ?: 0.0,
                marketCap = o.num("market_cap") ?: 0.0,
                volume24h = o.num("total_volume") ?: 0.0,
                high24h = o.num("high_24h") ?: 0.0,
                low24h = o.num("low_24h") ?: 0.0,
                change7d = o.num("price_change_percentage_7d_in_currency") ?: 0.0,
                ath = o.num("ath") ?: 0.0,
                athChange = o.num("ath_change_percentage") ?: 0.0,
                spark = spark,
                source = Source.COINGECKO,
                tickAt = now,
            )
        }
    }

    private fun parseGlobal(json: String): MarketOverview {
        val d = JSONObject(json).getJSONObject("data")
        return MarketOverview(
            totalCap = d.optJSONObject("total_market_cap")?.num("usd") ?: 0.0,
            capChange24h = d.num("market_cap_change_percentage_24h_usd") ?: 0.0,
            btcDominance = d.optJSONObject("market_cap_percentage")?.num("btc") ?: 0.0,
        )
    }

    private fun parseFng(json: String): Pair<Int, String>? {
        val o = JSONObject(json).getJSONArray("data").getJSONObject(0)
        val v = o.optString("value").toIntOrNull() ?: return null
        val label = when (o.optString("value_classification")) {
            "Extreme Fear" -> "ترس شدید"
            "Fear" -> "ترس"
            "Neutral" -> "خنثی"
            "Greed" -> "طمع"
            "Extreme Greed" -> "طمع شدید"
            else -> ""
        }
        return v to label
    }
}
