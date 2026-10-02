package ir.bazaaryar.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import ir.bazaaryar.app.data.Net
import ir.bazaaryar.app.ui.tr
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException

/**
 * In-app updater: reads the latest GitHub Release of MobitSolidity/march, downloads its APK and hands it
 * to the system installer. CI publishes a release on every push to main (see Bazaaryar/ci/build-and-release.yml).
 */
object Updater {
    const val OWNER = "MobitSolidity"
    const val REPO = "march"
    private const val API = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"
    private const val AUTO_EVERY_MS = 6 * 3_600_000L

    data class Release(
        val tag: String,
        val version: String,
        val notes: String,
        val apkUrl: String,
        val size: Long,
        val page: String,
    )

    sealed interface State {
        data object Idle : State
        data object Checking : State
        data class UpToDate(val current: String) : State
        data class Available(val r: Release) : State
        data class Downloading(val r: Release, val progress: Float) : State
        data class Ready(val r: Release, val file: File) : State
        data class Failed(val message: String, val r: Release? = null) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private var lastAuto = 0L

    fun currentVersion(context: Context): String = try {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    } catch (_: Exception) {
        ""
    }

    /** [auto] = silent background check on app start (at most every 6 hours, errors are ignored). */
    fun check(context: Context, auto: Boolean = false) {
        val app = context.applicationContext
        if (job?.isActive == true) return
        val busy = _state.value
        if (busy is State.Downloading || busy is State.Ready) return
        if (auto) {
            val now = System.currentTimeMillis()
            if (now - lastAuto < AUTO_EVERY_MS) return
            lastAuto = now
        }
        job = scope.launch {
            if (!auto) _state.value = State.Checking
            try {
                val r = fetchLatest()
                val cur = currentVersion(app)
                _state.value = if (r != null && isNewer(r.version, cur)) State.Available(r) else State.UpToDate(cur)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!auto) _state.value = State.Failed(friendly(e))
            }
        }
    }

    fun download(context: Context, r: Release) {
        val app = context.applicationContext
        if (job?.isActive == true) return
        job = scope.launch {
            _state.value = State.Downloading(r, 0f)
            try {
                val dir = File(app.cacheDir, "updates").apply { mkdirs() }
                dir.listFiles()?.forEach { it.delete() }
                val out = File(dir, "march-${r.tag}.apk")
                val req = Request.Builder().url(r.apkUrl).header("User-Agent", "March-Updater (Android)").build()
                Net.client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) throw Net.HttpError(resp.code)
                    val body = resp.body ?: throw IOException("empty body")
                    val total = body.contentLength().takeIf { it > 0 } ?: r.size
                    body.byteStream().use { input ->
                        out.outputStream().use { o ->
                            val buf = ByteArray(64 * 1024)
                            var done = 0L
                            var emitted = 0L
                            while (true) {
                                val n = input.read(buf)
                                if (n < 0) break
                                o.write(buf, 0, n)
                                done += n
                                if (total > 0 && done - emitted >= 128 * 1024) {
                                    emitted = done
                                    _state.value = State.Downloading(r, (done.toFloat() / total).coerceIn(0f, 1f))
                                }
                            }
                        }
                    }
                }
                _state.value = State.Ready(r, out)
                withContext(Dispatchers.Main) { install(app, out) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = State.Failed(friendly(e), r)
            }
        }
    }

    /** Opens the system installer. First time, Android asks to allow "install unknown apps" for March. */
    fun install(context: Context, file: File) {
        val ctx = context.applicationContext
        if (!ctx.packageManager.canRequestPackageInstalls()) {
            runCatching {
                ctx.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + ctx.packageName))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
            return
        }
        try {
            val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".updates", file)
            ctx.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (_: Exception) {
            val r = (_state.value as? State.Ready)?.r
            _state.value = State.Failed(tr("باز کردن نصب‌کننده ممکن نشد", "Couldn't open the installer"), r)
        }
    }

    private fun fetchLatest(): Release? {
        val o = JSONObject(Net.get(API))
        val tag = o.optString("tag_name")
        val assets = o.optJSONArray("assets")
        var url = ""
        var size = 0L
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val a = assets.optJSONObject(i) ?: continue
                if (a.optString("name").endsWith(".apk", ignoreCase = true)) {
                    url = a.optString("browser_download_url")
                    size = a.optLong("size")
                    break
                }
            }
        }
        if (tag.isEmpty() || url.isEmpty()) return null
        return Release(
            tag = tag,
            version = tag.trimStart('v', 'V'),
            notes = o.optString("body"),
            apkUrl = url,
            size = size,
            page = o.optString("html_url"),
        )
    }

    /** Numeric, dot-separated comparison: 2.2.10 > 2.2.9 > 2.2. */
    internal fun isNewer(remote: String, local: String): Boolean {
        fun parts(v: String) = v.split('.', '-', '+', '_').map { p -> p.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
        val a = parts(remote)
        val b = parts(local)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    private fun friendly(e: Exception): String =
        if (e is Net.HttpError && e.code == 404) {
            tr("هنوز نسخه‌ای در گیت‌هاب منتشر نشده", "No release published on GitHub yet")
        } else {
            Net.friendly(e, "GitHub")
        }
}
