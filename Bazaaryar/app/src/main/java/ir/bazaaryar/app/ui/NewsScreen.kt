package ir.bazaaryar.app.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.bazaaryar.app.data.EconEvent
import ir.bazaaryar.app.data.FLAGS
import ir.bazaaryar.app.notify.ClockApp

private const val HOUR = 3_600_000L
private const val DAY = 86_400_000L

@Composable
fun NewsScreen(events: List<EconEvent>, now: Long, alarms: Set<String>, onToggle: (String) -> Unit) {
    var onlyHigh by rememberSaveable { mutableStateOf(false) }
    val groups = remember(onlyHigh) { events.filter { !onlyHigh || it.impact == 3 }.groupBy { dayKey(it.at) }.toList() }
    val today = dayKey(now)
    LazyColumn(Modifier.fillMaxWidth()) {
        item { Board(events, now) }
        item {
            Row(Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(tr("همه", "All"), !onlyHigh) { onlyHigh = false }
                Pill(tr("فقط پراهمیت", "High impact"), onlyHigh) { onlyHigh = true }
            }
        }
        groups.forEach { (key, list) ->
            item(key = "h-$key") {
                Text(
                    (if (key == today) tr("امروز · ", "Today · ") else "") + faDay(list.first().at),
                    color = if (key == today) C.Ember else C.Muted,
                    fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    modifier = Modifier.animateItem().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp),
                )
            }
            item(key = "g-$key") {
                val shape = RoundedCornerShape(20.dp)
                Column(
                    Modifier.animateItem().padding(horizontal = 12.dp).clip(shape).background(C.Card).border(1.dp, C.Line, shape),
                ) {
                    list.forEachIndexed { i, e ->
                        if (i > 0) HorizontalDivider(color = C.Line)
                        EventRow(e, now, e.id in alarms) { onToggle(e.id) }
                    }
                }
            }
        }
        item {
            Text(
                tr(
                    "به وقت تهران، از تقویم فدرال رزرو و BLS. ۱۰ دقیقه قبل از هر خبرِ زنگوله‌دار اعلان می‌گیری. " +
                        "با آیکون ⏰ همان یادآور را در برنامه‌ی ساعت گوشی هم می‌گذاری (برای خبرهای ۲۴ ساعت آینده).",
                    "Tehran time, from the Fed and BLS calendars. You get a notification 10 minutes before every belled release. " +
                        "Tap ⏰ to also set it in your phone's Clock app (releases in the next 24 hours).",
                ),
                color = C.Muted, fontSize = 14.sp, modifier = Modifier.padding(20.dp),
            )
        }
    }
}

@Composable
private fun Board(events: List<EconEvent>, now: Long) {
    val next = events.firstOrNull { it.impact == 3 && it.at > now } ?: events.firstOrNull { it.at > now } ?: return
    val left = next.at - now
    val hot = left < HOUR
    val (d, h, m, s) = split(left)
    val cells = if (d > 0) {
        listOf(d to tr("روز", "days"), h to tr("ساعت", "hrs"), m to tr("دقیقه", "min"), s to tr("ثانیه", "sec"))
    } else {
        listOf(h to tr("ساعت", "hrs"), m to tr("دقیقه", "min"), s to tr("ثانیه", "sec"))
    }
    val sameTime = events.count { it.at == next.at }
    Column(
        Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp).fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)).background(C.headerBrush).padding(20.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(tr("انتشار بعدی", "Next release"), color = C.OnDeepMuted, fontSize = 14.sp)
            Text("${faDay(next.at)} · ${faTime(next.at)}", color = C.OnDeepMuted, fontSize = 14.sp)
        }
        // Countdown reads left-to-right like a clock, even in an RTL layout.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Top) {
                cells.forEachIndexed { i, (v, label) ->
                    if (i > 0) Text(":", color = C.OnDeep.copy(alpha = .4f), fontSize = 44.sp, modifier = Modifier.padding(horizontal = 4.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AnimatedContent(v, transitionSpec = { (slideInVertically { -it / 2 } + fadeIn()) togetherWith fadeOut() }, label = "flip") { value ->
                            Text(
                                fa(pad(value)), color = if (hot) Color(0xFFFFB896) else C.OnDeep,
                                fontSize = 54.sp, fontWeight = FontWeight.Bold, lineHeight = 60.sp,
                            )
                        }
                        Text(label, color = C.OnDeepMuted, fontSize = 13.sp)
                    }
                }
            }
        }
        Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(FLAGS[next.country] ?: "", fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(next.label, color = C.OnDeep, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (sameTime > 1) {
                    Text(
                        tr("+ " + fa((sameTime - 1).toString()) + " انتشار هم‌زمان", "+ ${sameTime - 1} at the same time"),
                        color = C.OnDeepMuted, fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun EventRow(e: EconEvent, now: Long, on: Boolean, onToggle: () -> Unit) {
    val ctx = LocalContext.current
    val left = e.at - now
    val past = left <= 0
    val hot = !past && left < HOUR
    val soon = !past && left < DAY
    val pulse by rememberInfiniteTransition(label = "p").animateFloat(1f, if (hot) .55f else 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "a")
    Row(
        Modifier.fillMaxWidth().background(if (past) C.Paper else Color.Transparent).padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(faTime(e.at), color = C.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(FLAGS[e.country] ?: "", fontSize = 16.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ImpactBars(e.impact)
                Spacer(Modifier.width(8.dp))
                Text(e.label, color = C.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (e.forecast.isNotEmpty() || e.previous.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 2.dp)) {
                    if (e.forecast.isNotEmpty()) Text(tr("پیش‌بینی ", "Forecast ") + e.forecast, color = C.Muted, fontSize = 13.sp)
                    if (e.previous.isNotEmpty()) Text(tr("قبلی ", "Previous ") + e.previous, color = C.Muted, fontSize = 13.sp)
                }
            }
        }
        if (past) {
            Text(tr("منتشر شد", "Released"), color = C.Muted, fontSize = 14.sp, modifier = Modifier.padding(end = 10.dp))
        } else {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    shortLeft(left), fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = if (hot) Color.White else if (soon) C.OnLead else C.Ink,
                    modifier = Modifier.padding(end = 10.dp).alpha(pulse).clip(RoundedCornerShape(8.dp))
                        .background(if (hot) C.Ember else if (soon) C.Lead else C.Soft)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (ClockApp.canSet(e.at, now)) {
                        IconButton(onClick = {
                            if (!ClockApp.setAlarm(ctx, e.at, e.label)) {
                                Toast.makeText(ctx, tr("برنامه‌ی ساعت پیدا نشد", "No Clock app found"), Toast.LENGTH_SHORT).show()
                            }
                        }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Filled.AlarmAdd, contentDescription = tr("زنگ در برنامه‌ی ساعت", "Set in Clock app"), tint = C.Lead)
                        }
                    }
                    IconButton(onClick = onToggle, modifier = Modifier.size(40.dp)) {
                        Icon(
                            if (on) Icons.Filled.NotificationsActive else Icons.Outlined.Notifications,
                            contentDescription = tr("یادآور", "Reminder"), tint = if (on) C.Ember else C.Faint,
                        )
                    }
                }
            }
        }
    }
}
