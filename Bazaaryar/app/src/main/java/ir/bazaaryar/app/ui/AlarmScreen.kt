package ir.bazaaryar.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.bazaaryar.app.notify.AlarmRinger

/** Full-screen ringing alarm shown by [ir.bazaaryar.app.AlarmActivity] over the lock screen. */
@Composable
fun AlarmScreen(
    info: AlarmRinger.Info,
    onStop: () -> Unit,
    onSnooze: () -> Unit,
    onOpen: () -> Unit,
) {
    val accent = Color(info.color)
    val now = rememberNow()
    val pulse = rememberInfiniteTransition(label = "alarmPulse")
    val k by pulse.animateFloat(
        0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart), label = "k",
    )
    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(C.Deep, Color(0xFF0B1020))))
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Text(fa(faTime(now)), color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Bold)
        Text(faDay(now), color = C.OnDeepMuted, fontSize = 15.sp)
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(120.dp).graphicsLayer {
                    val s = 1f + 0.45f * k
                    scaleX = s
                    scaleY = s
                    alpha = 0.5f * (1f - k)
                }.background(accent, CircleShape),
            )
            Box(Modifier.size(110.dp).background(accent, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Alarm, contentDescription = null, tint = Color.White, modifier = Modifier.size(52.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            info.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            info.body, color = C.OnDeepMuted, fontSize = 16.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.weight(1f))
        if (info.coinId != null) {
            AlarmButton(tr("مشاهده‌ی ارز", "Open coin"), Color.White.copy(alpha = 0.14f), Modifier.fillMaxWidth(), onOpen)
            Spacer(Modifier.height(12.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AlarmButton(
                tr("تعویق " + fa(AlarmRinger.SNOOZE_MIN.toString()) + " دقیقه", "Snooze " + AlarmRinger.SNOOZE_MIN + " min"),
                Color.White.copy(alpha = 0.14f), Modifier.weight(1f), onSnooze,
            )
            AlarmButton(tr("قطع زنگ", "Stop"), accent, Modifier.weight(1f), onStop)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AlarmButton(label: String, bg: Color, modifier: Modifier, onClick: () -> Unit) {
    Text(
        label, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
        modifier = modifier.bounceClick(RoundedCornerShape(50), onClick = onClick).background(bg).padding(vertical = 16.dp),
    )
}
