package ir.bazaaryar.app.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ir.bazaaryar.app.MainActivity
import ir.bazaaryar.app.R
import ir.bazaaryar.app.data.AlertDirection
import ir.bazaaryar.app.data.AlertScope
import ir.bazaaryar.app.data.AlertSettings
import ir.bazaaryar.app.data.AlertWindow
import ir.bazaaryar.app.data.Coin
import ir.bazaaryar.app.data.CoinRule
import ir.bazaaryar.app.data.MarketState
import ir.bazaaryar.app.data.PriceHistory
import ir.bazaaryar.app.data.RingMode
import ir.bazaaryar.app.data.USDT_IRT_ID
import ir.bazaaryar.app.ui.fa
import ir.bazaaryar.app.ui.fmtNum
import ir.bazaaryar.app.ui.fmtPct
import ir.bazaaryar.app.ui.fmtPrice
import ir.bazaaryar.app.ui.fmtToman
import ir.bazaaryar.app.ui.tr
import kotlin.math.abs

/**
 * Instant, personalised price alerts.
 * - Sharp moves: |change| over the user's window (5m/15m/1h from live samples, 24h from the feed) >= threshold.
 * - Per-coin overrides: own threshold, one-shot target prices above/below.
 * Depending on [RingMode] the strongest hit rings the phone like an alarm clock ([AlarmRinger]).
 * Evaluated on every live update (~1 s) by [ir.bazaaryar.app.data.PriceHub].
 */
object PriceAlerts {
    const val CHANNEL = "price_alerts"
    const val EXTRA_COIN = "coin"
    private const val MAX_PER_ROUND = 3
    private val GREEN = 0xFF059669.toInt()
    private val RED = 0xFFE11D48.toInt()
    private val ORANGE = 0xFFEA580C.toInt()
    private val INDIGO = 0xFF6366F1.toInt()

    private val lastFired = HashMap<String, Long>()
    private val firedTargets = HashSet<String>()

    private class Hit(val coin: Coin, val pct: Double, val threshold: Double)

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val ch = NotificationChannel(CHANNEL, tr("هشدار نوسان قیمت", "Price move alerts"), NotificationManager.IMPORTANCE_HIGH).apply {
            description = tr("اعلان فوری وقتی قیمت به آستانه‌ی تو می‌رسد", "Instant notification when a price crosses your threshold")
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250, 150, 250)
            enableLights(true)
        }
        nm.createNotificationChannel(ch)
        AlarmRinger.ensureChannel(context)
    }

    private fun rings(s: AlertSettings, target: Boolean): Boolean = when (s.ring) {
        RingMode.OFF -> false
        RingMode.TARGETS -> target
        RingMode.ALL -> true
    }

    @Synchronized
    fun evaluate(
        context: Context,
        state: MarketState,
        history: PriceHistory,
        s: AlertSettings,
        rules: Map<String, CoinRule>,
        watch: Set<String>,
        clearTarget: (String, Boolean) -> Unit,
    ) {
        val now = System.currentTimeMillis()
        val rate = state.usdt?.toman
        val hits = ArrayList<Hit>()
        for (c in state.coins) {
            if (c.price <= 0) continue
            val rule = rules[c.id]

            // 1) Target prices: one-shot, independent of the sharp-move switch.
            rule?.above?.let { target ->
                if (c.price >= target && firedTargets.add("${c.id}|a|$target")) {
                    postTarget(context, s, c, target, true, rate)
                    clearTarget(c.id, true)
                }
            }
            rule?.below?.let { target ->
                if (c.price <= target && firedTargets.add("${c.id}|b|$target")) {
                    postTarget(context, s, c, target, false, rate)
                    clearTarget(c.id, false)
                }
            }

            // 2) Sharp moves.
            if (!s.enabled) continue
            val custom = rule?.pct != null
            val inScope = when {
                c.id == USDT_IRT_ID -> s.includeUsdt || c.id in watch || custom
                s.scope == AlertScope.ALL -> true
                else -> c.id in watch || custom
            }
            if (!inScope) continue
            val (rise, drop) = move(c, history, s.window, now) ?: continue
            val pick = when (s.direction) {
                AlertDirection.UP -> rise
                AlertDirection.DOWN -> drop
                AlertDirection.BOTH -> if (rise >= -drop) rise else drop
            }
            val threshold = (rule?.pct ?: s.thresholdPct).toDouble()
            if (pick == 0.0 || abs(pick) < threshold) continue
            val key = c.id + (if (pick > 0) "|up" else "|down")
            if (now - (lastFired[key] ?: 0L) < s.cooldownMin * 60_000L) continue
            lastFired[key] = now
            hits += Hit(c, pick, threshold)
        }
        if (hits.isEmpty()) return
        hits.sortByDescending { abs(it.pct) }
        val ringTop = rings(s, target = false)
        hits.take(MAX_PER_ROUND).forEachIndexed { i, h -> postMove(context, s, h, rate, ring = ringTop && i == 0) }
        if (hits.size > MAX_PER_ROUND) postSummary(context, s, hits.drop(MAX_PER_ROUND))
    }

    /** (rise %, drop %) inside the window: rise from the window low, drop from the window high. */
    private fun move(c: Coin, history: PriceHistory, w: AlertWindow, now: Long): Pair<Double, Double>? {
        if (w == AlertWindow.H24) return maxOf(c.change24h, 0.0) to minOf(c.change24h, 0.0)
        val (lo, hi) = history.range(c.id, now - w.minutes * 60_000L) ?: return null
        val min = minOf(lo, c.price)
        val max = maxOf(hi, c.price)
        if (min <= 0 || max <= 0) return null
        return (c.price - min) / min * 100 to (c.price - max) / max * 100
    }

    fun test(context: Context, s: AlertSettings) {
        val pct = fmtNum(s.thresholdPct.toDouble())
        post(
            context, "test",
            "✅ " + greeting(s) + tr("اعلان‌های بازاریار فعال است", "Bazaaryar alerts are on"),
            tr(
                "هر وقت ارزی در ${s.window.label} بیش از ${fa(pct)}٪ جابه‌جا شد، همین‌طور فوری خبرت می‌کنم.",
                "Whenever a coin moves more than $pct% within ${s.window.label}, you'll hear about it right away.",
            ),
            null, INDIGO,
        )
    }

    /** Rings the phone exactly like a real alert would. */
    fun testAlarm(context: Context, s: AlertSettings) {
        AlarmRinger.fire(
            context,
            "⏰ " + greeting(s) + tr("آزمایش زنگ هشدار", "Alarm test"),
            tr(
                "وقتی قیمت به هدفت برسد گوشی همین‌طور زنگ می‌خورد. برای قطع، «قطع زنگ» را بزن.",
                "This is how your phone rings when a price hits your target. Tap Stop to silence it.",
            ),
            null, ORANGE,
        )
    }

    private fun greeting(s: AlertSettings): String = s.name.trim().let { if (it.isEmpty()) "" else it + tr("، ", ", ") }

    private fun priceLine(c: Coin, rate: Double?): String = when {
        c.isToman -> fmtToman(c.price)
        rate != null && rate > 0 -> "$" + fmtPrice(c.price) + " (≈ " + fmtToman(c.price * rate) + ")"
        else -> "$" + fmtPrice(c.price)
    }

    private fun postMove(context: Context, s: AlertSettings, h: Hit, rate: Double?, ring: Boolean) {
        val c = h.coin
        val up = h.pct > 0
        val pct = fmtNum(abs(h.pct))
        val title = (if (up) "🚀 " else "🔻 ") + greeting(s) + tr(
            "${c.symbol} در ${s.window.label} ${fa(pct)}٪ " + (if (up) "رشد کرد" else "ریزش کرد"),
            "${c.symbol} " + (if (up) "rose" else "fell") + " $pct% in ${s.window.label}",
        )
        val body = tr("قیمت الان ", "Now ") + priceLine(c, rate) +
            "\n" + tr("تغییر ۲۴ ساعته: ", "24h change: ") + fmtPct(c.change24h) +
            " · " + tr("آستانه‌ی تو: ", "your threshold: ") + fa(fmtNum(h.threshold)) + tr("٪", "%")
        val color = if (up) GREEN else RED
        if (ring) {
            AlarmRinger.fire(context, title, body, c.id, color)
        } else {
            post(context, "${c.id}|move", title, body, c.id, color)
        }
    }

    private fun postTarget(context: Context, s: AlertSettings, c: Coin, target: Double, above: Boolean, rate: Double?) {
        val t = if (c.isToman) fmtToman(target) else "$" + fmtPrice(target)
        val title = "🎯 " + greeting(s) + tr("${c.symbol} به قیمت هدفت رسید", "${c.symbol} hit your target")
        val body = (if (above) tr("بالای ", "Above ") else tr("زیر ", "Below ")) + t +
            " · " + tr("قیمت الان ", "now ") + priceLine(c, rate)
        if (rings(s, target = true)) {
            AlarmRinger.fire(context, title, body, c.id, ORANGE)
        } else {
            post(context, "${c.id}|target", title, body, c.id, ORANGE)
        }
    }

    private fun postSummary(context: Context, s: AlertSettings, rest: List<Hit>) {
        val n = rest.size.toString()
        val title = "⚡ " + greeting(s) + tr("نوسان شدید در " + fa(n) + " ارز دیگر", "Sharp moves in $n more coins")
        val body = rest.take(8).joinToString(tr("، ", ", ")) { it.coin.symbol + " " + fmtPct(it.pct) }
        post(context, "summary", title, body, null, ORANGE)
    }

    private fun post(context: Context, key: String, title: String, body: String, coinId: String?, color: Int) {
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (coinId != null) intent.putExtra(EXTRA_COIN, coinId)
        val open = PendingIntent.getActivity(
            context, key.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_pulse)
            .setContentTitle(title)
            .setContentText(body.lineSequence().first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setColor(color)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(key.hashCode(), n)
        } catch (_: SecurityException) {
            // Notification permission denied by the user.
        }
    }
}
