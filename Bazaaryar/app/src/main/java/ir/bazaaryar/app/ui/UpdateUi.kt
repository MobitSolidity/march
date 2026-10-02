package ir.bazaaryar.app.ui

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.bazaaryar.app.update.Updater
import kotlin.math.roundToInt

private fun pct(p: Float): String = (p * 100).roundToInt().toString().let { tr(fa(it) + "٪", "$it%") }

private fun ver(v: String): String = tr(fa(v), v)

private fun releaseOf(s: Updater.State): Updater.Release? = when (s) {
    is Updater.State.Available -> s.r
    is Updater.State.Downloading -> s.r
    is Updater.State.Ready -> s.r
    is Updater.State.Failed -> s.r
    else -> null
}

/** "App update" card (Alerts tab): current version, check button, download progress, install. */
@Composable
fun UpdateSection() {
    val ctx = LocalContext.current
    val st by Updater.state.collectAsStateWithLifecycle()
    val current = remember { Updater.currentVersion(ctx) }
    Section {
        Text(tr("به‌روزرسانی برنامه", "App update"), color = C.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(tr("نسخه‌ی نصب‌شده: ", "Installed version: ") + ver(current), color = C.Muted, fontSize = 13.sp)
        StatusText(st)
        Button(
            onClick = { act(ctx, st) },
            enabled = st !is Updater.State.Checking && st !is Updater.State.Downloading,
            colors = ButtonDefaults.buttonColors(containerColor = C.Lead),
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        ) { Text(actionLabel(st)) }
    }
}

/** Pops up once per new version when the silent start-up check finds an update. */
@Composable
fun UpdatePrompt() {
    val ctx = LocalContext.current
    val st by Updater.state.collectAsStateWithLifecycle()
    var hidden by rememberSaveable { mutableStateOf("") }
    val s = st
    val r = releaseOf(s) ?: return
    if (hidden == r.tag) return
    AlertDialog(
        onDismissRequest = { hidden = r.tag },
        title = { Text(tr("نسخه‌ی جدید March", "New March version") + " " + ver(r.version)) },
        text = { Column { StatusText(s) } },
        confirmButton = {
            TextButton(
                onClick = { act(ctx, s) },
                enabled = s !is Updater.State.Downloading,
            ) { Text(actionLabel(s)) }
        },
        dismissButton = { TextButton(onClick = { hidden = r.tag }) { Text(tr("بعداً", "Later")) } },
    )
}

@Composable
private fun StatusText(s: Updater.State) {
    when (s) {
        is Updater.State.Checking -> Text(tr("در حال بررسی…", "Checking…"), color = C.Muted, fontSize = 13.sp)
        is Updater.State.UpToDate -> Text(tr("آخرین نسخه نصب است ✅", "You're on the latest version ✅"), color = C.Muted, fontSize = 13.sp)
        is Updater.State.Available -> {
            Text(
                tr("نسخه‌ی ", "Version ") + ver(s.r.version) + tr(" آماده است", " is available"),
                color = C.Lead, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            )
            val notes = s.r.notes.trim().take(500).ifEmpty { tr("بهبودها و رفع اشکال", "Improvements and fixes") }
            Text(notes, color = C.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
        is Updater.State.Downloading -> {
            Text(tr("در حال دانلود… ", "Downloading… ") + pct(s.progress), color = C.Muted, fontSize = 13.sp)
            LinearProgressIndicator(
                progress = { s.progress },
                color = C.Lead,
                trackColor = C.Soft,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
        is Updater.State.Ready -> Text(
            tr(
                "دانلود کامل شد. «نصب» را بزن؛ اگر اندروید اجازه‌ی نصب خواست، برای March روشنش کن و برگرد.",
                "Downloaded. Tap Install; if Android asks, allow installs from March and come back.",
            ),
            color = C.Muted, fontSize = 13.sp,
        )
        is Updater.State.Failed -> Text(s.message, color = C.Crimson, fontSize = 13.sp)
        else -> {}
    }
}

private fun actionLabel(s: Updater.State): String = when (s) {
    is Updater.State.Available -> tr("بروزرسانی", "Update")
    is Updater.State.Downloading -> pct(s.progress)
    is Updater.State.Ready -> tr("نصب", "Install")
    is Updater.State.Failed -> if (s.r != null) tr("تلاش دوباره", "Retry") else tr("بررسی دوباره", "Check again")
    else -> tr("بررسی نسخه‌ی جدید", "Check for updates")
}

private fun act(ctx: Context, s: Updater.State) {
    when (s) {
        is Updater.State.Available -> Updater.download(ctx, s.r)
        is Updater.State.Ready -> Updater.install(ctx, s.file)
        is Updater.State.Failed -> {
            val r = s.r
            if (r != null) Updater.download(ctx, r) else Updater.check(ctx)
        }
        is Updater.State.Downloading, is Updater.State.Checking -> {}
        else -> Updater.check(ctx)
    }
}
