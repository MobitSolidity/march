package ir.bazaaryar.app.data

import ir.bazaaryar.app.ui.tr
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/** One shared OkHttp client (connection pool, gzip, WebSocket pings) for every data source. */
internal object Net {
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    class HttpError(val code: Int) : IOException("HTTP $code")

    /** Blocking GET. Call from Dispatchers.IO. */
    fun get(url: String): String {
        val req = Request.Builder().url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "Bazaaryar/2.1 (Android)")
            .build()
        return client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw HttpError(r.code)
            r.body?.string() ?: throw IOException("empty body")
        }
    }

    fun friendly(e: Throwable?, service: String): String = when (e) {
        is UnknownHostException -> tr("اینترنت در دسترس نیست", "No internet connection")
        is SocketTimeoutException -> tr("زمان اتصال به $service تمام شد", "$service timed out")
        is HttpError -> when (e.code) {
            429 -> tr("محدودیت درخواست $service؛ کمی صبر کن", "$service rate limit, please wait")
            403, 451 -> tr("دسترسی به $service از این IP مسدود است", "$service is blocked from this IP")
            else -> tr("خطای سرور $service (${e.code})", "$service server error (${e.code})")
        }
        else -> e?.message ?: tr("خطای شبکه", "Network error")
    }
}

/** Two attempts with a short pause, then a user-friendly localized error (original error kept as cause). */
internal suspend fun <T> retrying(service: String, block: () -> T): T {
    var last: Exception? = null
    for (attempt in 0 until 2) {
        try {
            return block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            last = e
            if (attempt == 0) delay(1_500)
        }
    }
    throw IOException(Net.friendly(last, service), last)
}

/** Reads a number that may arrive as a JSON number or a numeric string. */
internal fun JSONObject.num(key: String): Double? {
    val v = when (val raw = opt(key)) {
        is Number -> raw.toDouble()
        is String -> raw.replace(",", "").toDoubleOrNull()
        else -> null
    }
    return v?.takeIf { !it.isNaN() && !it.isInfinite() }
}
