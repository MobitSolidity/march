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
import ir.bazaaryar.app.data.USDT_IRT_ID
import ir.bazaaryar.app.ui.fa
import ir.bazaaryar.app.ui.fmtNum
import ir.bazaaryar.app.ui.fmtPct
import ir.bazaaryar.app.ui.fmtPrice
import ir.bazaaryar.app.ui.fmtToman
import kotlin.math.abs

/**
 * Instant, personalised price alerts.
 * - Sharp moves: |change| over the user's window (5m/15m/1h from live samples, 24h from the feed) >= threshold.
 * - Per-coin overrides: own threshold, one-shot target prices above/below.
 * Evaluated on every live update (~1 s) by [ir.bazaaryar.app.data.PriceHub].
 */
object PriceAlerts {
    const val CHANNEL = "price_alerts"
    const val EXTRA_COIN = "coin"
    private const val MAX_PER_ROUND = 3

    private val lastFired = HashMap<String, Long>()
    private val firedTargets = HashSet<String>()

    private class Hit(val coin: Coin, val pct: Double, val threshold: Double)

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val ch = NotificationChannel(CHANNEL, "هشدار نوسان قیمت", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "اعلان فوری وقتی قیمت به آستانه‌ی تو می‌رسد"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250, 150, 250)
            enableLights(true)
        }
        nm.createNotificationChannel(ch)
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
        hits.take(MAX_PER_ROUND).forEach { postMove(context, s, it, rate) }
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
        post(
            context, "test",
            "✅ ${greeting(s)}اعلان‌های بازاریار فعال است",
            "هر وقت ارزی در ${s.window.label} بیش از ${fa(fmtNum(s.thresholdPct.toDouble()))}٪ جابه‌جا شد، همین‌طور فوری خبرت می‌کنم.",
            null, 0xFF006289.toInt(),
        )
    }

    private fun greeting(s: AlertSettings): String = s.name.trim().let { if (it.isEmpty()) "" else "${it}، " }

    private fun priceLine(c: Coin, rate: Double?): String = when {
        c.isToman -> fmtToman(c.price)
        rate != null && rate > 0 -> "$" + fmtPrice(c.price) + " (≈ " + fmtToman(c.price * rate) + ")"
        else -> "$" + fmtPrice(c.price)
    }

    private fun postMove(context: Context, s: AlertSettings, h: Hit, rate: Double?) {
        val c = h.coin
        val up = h.pct > 0
        val title = (if (up) "🚀 " else "🔻 ") + greeting(s) +
            "${c.symbol} در ${s.window.label} ${fa(fmtNum(abs(h.pct)))}٪ " + (if (up) "رشد کرد" else "ریزش کرد")
        val body = "قیمت الان " + priceLine(c, rate) +
            "\nتغییر ۲۴ ساعته: " + fmtPct(c.change24h) +
            " · آستانه‌ی تو: " + fa(fmtNum(h.threshold)) + "٪"
        post(context, "${c.id}|move", title, body, c.id, if (up) 0xFF0B6B4A.toInt() else 0xFFBB1D2C.toInt())
    }

    private fun postTarget(context: Context, s: AlertSettings, c: Coin, target: Double, above: Boolean, rate: Double?) {
        val t = if (c.isToman) fmtToman(target) else "$" + fmtPrice(target)
        val title = "🎯 " + greeting(s) + "${c.symbol} به قیمت هدفت رسید"
        val body = (if (above) "بالای " else "زیر ") + t + " · قیمت الان " + priceLine(c, rate)
        post(context, "${c.id}|target", title, body, c.id, 0xFFE84A00.toInt())
    }

    private fun postSummary(context: Context, s: AlertSettings, rest: List<Hit>) {
        val title = "⚡ " + greeting(s) + "نوسان شدید در " + fa(rest.size.toString()) + " ارز دیگر"
        val body = rest.take(8).joinToString("، ") { it.coin.symbol + " " + fmtPct(it.pct) }
        post(context, "summary", title, body, null, 0xFFE84A00.toInt())
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
