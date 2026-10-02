package ir.bazaaryar.app.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ir.bazaaryar.app.MainActivity
import ir.bazaaryar.app.R
import ir.bazaaryar.app.data.Prefs
import ir.bazaaryar.app.data.PriceHub
import ir.bazaaryar.app.ui.fmtPrice
import ir.bazaaryar.app.ui.fmtToman
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps [PriceHub] (and therefore alert evaluation) running while the app is closed.
 * In background only watch-list / custom-rule coins are streamed, so data usage stays low.
 */
class LiveService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var acquired = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel(this)
        PriceAlerts.ensureChannel(this)
        goForeground(build("در حال اتصال به بازار…"))
        PriceHub.acquire(this, ui = false)
        acquired = true
        scope.launch {
            var last = 0L
            PriceHub.state.collect { st ->
                val now = System.currentTimeMillis()
                if (now - last < 20_000L) return@collect
                val parts = ArrayList<String>()
                st.coins.firstOrNull { it.id == "bitcoin" }?.let { parts += "BTC $" + fmtPrice(it.price) }
                st.usdt?.let { parts += "تتر " + fmtToman(it.toman) }
                if (parts.isEmpty()) return@collect
                last = now
                try {
                    NotificationManagerCompat.from(this@LiveService).notify(ID, build(parts.joinToString(" · ")))
                } catch (_: SecurityException) {
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            scope.launch {
                Prefs(applicationContext).updateSettings { it.copy(background = false) }
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        if (acquired) {
            PriceHub.release(ui = false)
            acquired = false
        }
        super.onDestroy()
    }

    private fun goForeground(n: Notification) {
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(ID, n)
        }
    }

    private fun build(text: String): Notification {
        val open = PendingIntent.getActivity(
            this, 1,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this, 2,
            Intent(this, LiveService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_pulse)
            .setContentTitle("پایش زنده‌ی قیمت فعال است")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(open)
            .addAction(0, "توقف", stop)
            .build()
    }

    companion object {
        private const val ID = 4201
        private const val CHANNEL = "live_monitor"
        const val ACTION_STOP = "ir.bazaaryar.app.action.STOP_LIVE"

        fun ensureChannel(context: Context) {
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(CHANNEL, "پایش زنده (پس‌زمینه)", NotificationManager.IMPORTANCE_LOW),
            )
        }

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, LiveService::class.java))
            } catch (_: Exception) {
                // Background-start restrictions: it will be started next time the app is opened.
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LiveService::class.java))
        }
    }
}
