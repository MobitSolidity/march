package ir.bazaaryar.app.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ir.bazaaryar.app.MainActivity
import ir.bazaaryar.app.data.EVENTS
import ir.bazaaryar.app.data.EconEvent
import ir.bazaaryar.app.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val CHANNEL = "releases"
const val LEAD_MINUTES = 10L

object Reminders {
    private fun pending(context: Context, e: EconEvent): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra("title", e.title)
            .putExtra("id", e.id)
        return PendingIntent.getBroadcast(
            context, e.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun schedule(context: Context, e: EconEvent) {
        val trigger = e.at - LEAD_MINUTES * 60_000
        if (trigger <= System.currentTimeMillis()) return
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        // Inexact-while-idle: no special permission needed; Android may deliver it a few minutes late.
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending(context, e))
    }

    fun cancel(context: Context, e: EconEvent) {
        context.getSystemService(AlarmManager::class.java)?.cancel(pending(context, e))
    }

    /** Re-registers every saved reminder. Safe to call repeatedly (same PendingIntent is replaced). */
    fun rescheduleAll(context: Context, ids: Set<String>) {
        EVENTS.filter { it.id in ids }.forEach { schedule(context, it) }
    }

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "انتشار اخبار اقتصادی", NotificationManager.IMPORTANCE_HIGH))
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.ensureChannel(context)
        val title = intent.getStringExtra("title") ?: return
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText("$LEAD_MINUTES دقیقه تا انتشار")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setColor(0xFFE84A00.toInt())
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(intent.getStringExtra("id").hashCode(), n)
        } catch (_: SecurityException) {
            // Notification permission denied by the user.
        }
    }
}

/** Alarms are wiped on reboot and on app update: put the saved reminders back. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val app = context.applicationContext
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Reminders.ensureChannel(app)
                Reminders.rescheduleAll(app, Prefs(app).alarms.first())
            } finally {
                result.finish()
            }
        }
    }
}
