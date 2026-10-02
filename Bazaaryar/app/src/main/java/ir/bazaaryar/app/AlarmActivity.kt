package ir.bazaaryar.app

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.bazaaryar.app.data.Look
import ir.bazaaryar.app.data.Prefs
import ir.bazaaryar.app.notify.AlarmRinger
import ir.bazaaryar.app.notify.PriceAlerts
import ir.bazaaryar.app.ui.AlarmScreen
import ir.bazaaryar.app.ui.BazaaryarTheme
import ir.bazaaryar.app.ui.L
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Full-screen alarm shown over the lock screen while a price alert is ringing.
 * Closes itself when the alarm is stopped from anywhere (notification, auto-stop).
 */
class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        wakeScreen()
        val look = runBlocking { withTimeoutOrNull(300) { Prefs(applicationContext).look.first() } } ?: Look()
        L.lang = look.lang
        setContent {
            val info by AlarmRinger.state.collectAsStateWithLifecycle()
            LaunchedEffect(info) { if (info == null) finish() }
            BackHandler { stopAndClose() }
            BazaaryarTheme(look.theme) {
                CompositionLocalProvider(LocalLayoutDirection provides L.dir) {
                    val a = info
                    if (a != null) {
                        AlarmScreen(
                            info = a,
                            onStop = { stopAndClose() },
                            onSnooze = { snoozeAndClose() },
                            onOpen = { openCoin(a.coinId) },
                        )
                    }
                }
            }
        }
    }

    private fun wakeScreen() {
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun stopAndClose() {
        AlarmRinger.stop(this)
        finish()
    }

    private fun snoozeAndClose() {
        AlarmRinger.snooze(this)
        finish()
    }

    private fun openCoin(id: String?) {
        AlarmRinger.stop(this)
        val i = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (id != null) i.putExtra(PriceAlerts.EXTRA_COIN, id)
        getSystemService(KeyguardManager::class.java)?.requestDismissKeyguard(this, null)
        startActivity(i)
        finish()
    }
}
