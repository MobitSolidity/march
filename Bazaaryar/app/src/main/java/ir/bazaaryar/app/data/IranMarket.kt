package ir.bazaaryar.app.data

import ir.bazaaryar.app.ui.tr
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException

data class IranSnapshot(val usdt: UsdtRate?, val quotes: Map<String, LiveQuote>)

/**
 * Iranian exchanges (reachable from Iranian IPs, no VPN):
 * - USDT/Toman from Nobitex, falling back to Wallex.
 * - Coin/USDT prices from Nobitex, used as the live source when Binance is unreachable.
 * Nobitex quotes IRT markets in Rial, so values are divided by 10. Rate limit: 20 req/min.
 */
object IranMarket {
    private const val NOBITEX = "https://api.nobitex.ir/market/stats"
    private const val WALLEX = "https://api.wallex.ir/v1/markets"

    suspend fun fetch(withCoins: Boolean): IranSnapshot = withContext(Dispatchers.IO) {
        val errors = ArrayList<Exception>()
        val usdt = attempt(errors) { nobitexUsdt() } ?: attempt(errors) { wallexUsdt() }
        val quotes: Map<String, LiveQuote> =
            if (withCoins) (attempt(errors) { nobitexQuotes() } ?: emptyMap()) else emptyMap()
        if (usdt == null && quotes.isEmpty()) {
            val first = errors.firstOrNull()
            throw IOException(Net.friendly(first, tr("نوبیتکس/والکس", "Nobitex/Wallex")), first)
        }
        IranSnapshot(usdt, quotes)
    }

    private inline fun <T> attempt(errors: MutableList<Exception>, block: () -> T?): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        errors += e
        null
    }

    private fun nobitexUsdt(): UsdtRate? {
        val stats = JSONObject(Net.get("$NOBITEX?srcCurrency=usdt&dstCurrency=rls")).optJSONObject("stats") ?: return null
        val o = stats.optJSONObject("usdt-rls") ?: return null
        val last = o.num("latest")?.takeIf { it > 0 } ?: return null
        return UsdtRate(
            toman = last / 10,
            change24h = o.num("dayChange") ?: 0.0,
            high = (o.num("dayHigh") ?: 0.0) / 10,
            low = (o.num("dayLow") ?: 0.0) / 10,
            bestBuy = (o.num("bestBuy") ?: 0.0) / 10,
            bestSell = (o.num("bestSell") ?: 0.0) / 10,
            source = tr("نوبیتکس", "Nobitex"),
            at = System.currentTimeMillis(),
        )
    }

    private fun nobitexQuotes(): Map<String, LiveQuote> {
        val stats = JSONObject(Net.get("$NOBITEX?dstCurrency=usdt")).optJSONObject("stats") ?: return emptyMap()
        val now = System.currentTimeMillis()
        val out = HashMap<String, LiveQuote>()
        for (key in stats.keys()) {
            if (!key.endsWith("-usdt")) continue
            val o = stats.optJSONObject(key) ?: continue
            if (o.optBoolean("isClosed", false)) continue
            val p = o.num("latest")?.takeIf { it > 0 } ?: continue
            out[key.removeSuffix("-usdt").uppercase()] = LiveQuote(p, o.num("dayChange") ?: 0.0, now)
        }
        return out
    }

    private fun wallexUsdt(): UsdtRate? {
        val st = JSONObject(Net.get(WALLEX)).optJSONObject("result")?.optJSONObject("symbols")
            ?.optJSONObject("USDTTMN")?.optJSONObject("stats") ?: return null
        val last = st.num("lastPrice")?.takeIf { it > 0 } ?: return null
        return UsdtRate(
            toman = last,
            change24h = st.num("24h_ch") ?: 0.0,
            high = st.num("24h_highPrice") ?: 0.0,
            low = st.num("24h_lowPrice") ?: 0.0,
            bestBuy = st.num("bidPrice") ?: 0.0,
            bestSell = st.num("askPrice") ?: 0.0,
            source = tr("والکس", "Wallex"),
            at = System.currentTimeMillis(),
        )
    }
}
