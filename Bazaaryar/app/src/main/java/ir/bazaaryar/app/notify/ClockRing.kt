package ir.bazaaryar.app.notify

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings

/**
 * Rings a price alert through the phone's own Clock app, exactly like a wake-up alarm/timer:
 * a 1-second timer is started with [AlarmClock.ACTION_SET_TIMER] (no UI), so the Clock app plays its alarm
 * sound full screen until the user stops it.
 *
 * Android only lets an app open another app's screen while it is visible, or when the user granted
 * "Display over other apps". Without either, [ringNow] returns false and the caller falls back to
 * March's own alarm ([AlarmRinger]), so the phone always rings.
 */
object ClockRing {
    /** True while MainActivity is started (set from its lifecycle observer). */
    @Volatile
    var appVisible: Boolean = false

    fun canOverlay(context: Context): Boolean = Settings.canDrawOverlays(context)

    /** True if a Clock app on this phone accepts timers. */
    fun clockAvailable(context: Context): Boolean =
        Intent(AlarmClock.ACTION_SET_TIMER).resolveActivity(context.packageManager) != null

    fun openOverlaySettings(context: Context) {
        val pkg = Uri.parse("package:" + context.packageName)
        try {
            context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }

    /** Starts the Clock app alarm now. False when Android would block it or no Clock app handles timers. */
    fun ringNow(context: Context, label: String): Boolean {
        val ctx = context.applicationContext
        if (!appVisible && !canOverlay(ctx)) return false
        val i = Intent(AlarmClock.ACTION_SET_TIMER)
            .putExtra(AlarmClock.EXTRA_LENGTH, 1)
            .putExtra(AlarmClock.EXTRA_MESSAGE, label.take(60))
            .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            ctx.startActivity(i)
            true
        } catch (_: Exception) {
            false
        }
    }
}
