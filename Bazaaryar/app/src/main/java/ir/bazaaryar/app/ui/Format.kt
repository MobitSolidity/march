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
import ir.bazaaryar.app.data.Coin
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.math.MathContext
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10

private val FA = ULocale("fa_IR@calendar=persian")
private val EN = ULocale("en_US")
private val TZ = TimeZone.getTimeZone("Asia/Tehran")
private fun dateFmt(pattern: String, loc: ULocale) = SimpleDateFormat(pattern, loc).apply { timeZone = TZ }
private val dayFa = dateFmt("EEEE d MMMM", FA)
private val dayEn = dateFmt("EEEE, MMM d", EN)
private val timeFa = dateFmt("HH:mm", FA)
private val timeEn = dateFmt("HH:mm", EN)
private val clockFa = dateFmt("HH:mm:ss", FA)
private val clockEn = dateFmt("HH:mm:ss", EN)
private val keyFmt = SimpleDateFormat("yyyy-MM-dd", ULocale.ENGLISH).apply { timeZone = TZ }

/** Weekday + date in the app language (Persian calendar in FA), Tehran time. */
fun faDay(ms: Long): String = (if (L.en) dayEn else dayFa).format(Date(ms))
fun faTime(ms: Long): String = (if (L.en) timeEn else timeFa).format(Date(ms))
fun faClock(ms: Long): String = (if (L.en) clockEn else clockFa).format(Date(ms))
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
    v >= 1e6 -> String.format(Locale.US, "$%.1fM", v / 1e6)
    else -> String.format(Locale.US, "$%,.0f", v)
}
fun fmtPct(v: Double): String = (if (v >= 0) "▲ " else "▼ ") + String.format(Locale.US, "%.2f%%", abs(v))

/** Toman amount with the Toman suffix of the current language. */
fun fmtToman(v: Double): String = fmtPrice(v) + tr(" ت", " T")

/** Price as the user wants to see it. USDT/IRT is always shown in Toman. */
fun coinPrice(c: Coin, toman: Boolean, rate: Double?): String = when {
    c.isToman -> fmtToman(c.price)
    toman && rate != null && rate > 0 -> fmtToman(c.price * rate)
    else -> "$" + fmtPrice(c.price)
}

/** Latin digits to Persian digits (only in Persian mode). */
fun fa(s: String): String = if (L.en) s else buildString {
    for (ch in s) append(if (ch in '0'..'9') '۰' + (ch - '0') else ch)
}

fun fmtNum(v: Double): String =
    if (abs(v - Math.rint(v)) < 1e-9) String.format(Locale.US, "%.0f", v) else String.format(Locale.US, "%.1f", v)

fun plainNum(v: Double): String = BigDecimal.valueOf(v).stripTrailingZeros().toPlainString()

/** Value rounded to [digits] significant digits, as plain text for an input field. */
fun sigNum(v: Double, digits: Int = 6): String =
    BigDecimal.valueOf(v).round(MathContext(digits)).stripTrailingZeros().toPlainString()

/** Parses user input with Persian/Arabic digits and thousands separators. Null if empty or not positive. */
fun parseNum(s: String): Double? {
    val t = buildString {
        for (ch in s.trim()) {
            when (ch) {
                in '۰'..'۹' -> append('0' + (ch - '۰'))
                in '٠'..'٩' -> append('0' + (ch - '٠'))
                ',', '٬', ' ' -> Unit
                '٫' -> append('.')
                else -> append(ch)
            }
        }
    }
    return t.toDoubleOrNull()?.takeIf { it > 0 }
}

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
