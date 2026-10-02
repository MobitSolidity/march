package ir.bazaaryar.app.ui

import android.icu.text.SimpleDateFormat
import android.icu.util.TimeZone
import android.icu.util.ULocale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10

private val FA = ULocale("fa_IR@calendar=persian")
private val TZ = TimeZone.getTimeZone("Asia/Tehran")
private val dayFmt = SimpleDateFormat("EEEE d MMMM", FA).apply { timeZone = TZ }
private val timeFmt = SimpleDateFormat("HH:mm", FA).apply { timeZone = TZ }
private val keyFmt = SimpleDateFormat("yyyy-MM-dd", ULocale.ENGLISH).apply { timeZone = TZ }

fun faDay(ms: Long): String = dayFmt.format(Date(ms))
fun faTime(ms: Long): String = timeFmt.format(Date(ms))
fun dayKey(ms: Long): String = keyFmt.format(Date(ms))

data class Split(val d: Long, val h: Long, val m: Long, val s: Long)
fun split(ms: Long): Split {
    val t = (ms / 1000).coerceAtLeast(0)
    return Split(t / 86400, (t % 86400) / 3600, (t % 3600) / 60, t % 60)
}
fun pad(n: Long) = n.toString().padStart(2, '0')
fun shortLeft(ms: Long): String {
    val (d, h, m, s) = split(ms)
    return when {
        d > 0 -> "${d}d ${pad(h)}h"
        h > 0 -> "${pad(h)}:${pad(m)}:${pad(s)}"
        else -> "${pad(m)}:${pad(s)}"
    }
}

fun fmtPrice(p: Double): String = when {
    p >= 1000 -> String.format(Locale.US, "%,.0f", p)
    p >= 1 -> String.format(Locale.US, "%,.2f", p)
    p >= 0.01 -> String.format(Locale.US, "%.4f", p)
    p <= 0 -> "0"
    else -> String.format(Locale.US, "%.${(3 - floor(log10(p)).toInt()).coerceAtMost(10)}f", p)
}
fun fmtCap(v: Double): String = when {
    v >= 1e12 -> String.format(Locale.US, "$%.2fT", v / 1e12)
    v >= 1e9 -> String.format(Locale.US, "$%.1fB", v / 1e9)
    else -> String.format(Locale.US, "$%.0fM", v / 1e6)
}
fun fmtPct(v: Double): String = (if (v >= 0) "▲ " else "▼ ") + String.format(Locale.US, "%.2f%%", abs(v))

/** Ticks once a second while on screen. */
@Composable
fun rememberNow(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000 - now % 1000)
        }
    }
    return now
}
