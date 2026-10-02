package ir.bazaaryar.app.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

/** Live top-100 prices from the free CoinGecko API (no key needed, ~30 calls/min). */
object CoinRepository {
    private const val URL_TOP100 =
        "https://api.coingecko.com/api/v3/coins/markets" +
            "?vs_currency=usd&order=market_cap_desc&per_page=100&page=1" +
            "&sparkline=true&price_change_percentage=24h"

    private class HttpError(val code: Int) : IOException("HTTP $code")

    suspend fun fetchTop100(): List<Coin> = withContext(Dispatchers.IO) { fetchWithRetry() }

    private suspend fun fetchWithRetry(): List<Coin> {
        var lastError: Exception? = null
        for (attempt in 0 until 2) {
            try {
                return fetchOnce()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                if (attempt == 0) delay(1_500)
            }
        }
        throw IOException(friendly(lastError), lastError)
    }

    private fun friendly(e: Exception?): String = when (e) {
        is UnknownHostException -> "اینترنت در دسترس نیست"
        is SocketTimeoutException -> "زمان اتصال تمام شد؛ شاید VPN لازم باشد"
        is HttpError -> when (e.code) {
            429 -> "محدودیت درخواست CoinGecko؛ کمی صبر کن"
            403, 451 -> "دسترسی از این IP مسدود است؛ با VPN امتحان کن"
            else -> "خطای سرور (${e.code})"
        }
        else -> e?.message ?: "خطای شبکه"
    }

    private fun fetchOnce(): List<Coin> {
        val conn = (URL(URL_TOP100).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Bazaaryar/1.1 (Android)")
        }
        try {
            val code = conn.responseCode
            if (code != 200) throw HttpError(code)
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            return parse(body)
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(json: String): List<Coin> {
        val arr = JSONArray(json)
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            val sparkArr = o.optJSONObject("sparkline_in_7d")?.optJSONArray("price")
            val spark = if (sparkArr == null) emptyList() else
                List(sparkArr.length()) { k -> sparkArr.optDouble(k) }.filter { !it.isNaN() }.takeLast(24)
            Coin(
                id = o.getString("id"),
                rank = o.optInt("market_cap_rank", i + 1),
                symbol = o.getString("symbol").uppercase(),
                name = o.getString("name"),
                price = o.optDouble("current_price", 0.0).let { if (it.isNaN()) 0.0 else it },
                change24h = o.optDouble("price_change_percentage_24h", 0.0).let { if (it.isNaN()) 0.0 else it },
                marketCap = o.optDouble("market_cap", 0.0).let { if (it.isNaN()) 0.0 else it },
                spark = spark,
            )
        }
    }
}
