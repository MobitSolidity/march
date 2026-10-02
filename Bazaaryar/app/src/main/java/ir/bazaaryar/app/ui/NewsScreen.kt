package ir.bazaaryar.app.ui

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.bazaaryar.app.data.EconEvent
import ir.bazaaryar.app.data.FLAGS

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
            Row(Modifier.padding(16.dp, 16.dp, 16.dp, 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("همه", !onlyHigh) { onlyHigh = false }
                Chip("فقط پراهمیت", onlyHigh) { onlyHigh = true }
            }
        }
        groups.forEach { (key, list) ->
            item(key = "h-$key") {
                Text(
                    (if (key == today) "امروز · " else "") + faDay(list.first().at),
                    color = if (key == today) Color(0xFF9C2F00) else C.Muted,
                    fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp),
                )
            }
            item(key = "g-$key") {
                Column(Modifier.padding(horizontal = 12.dp).clip(RoundedCornerShape(16.dp)).background(C.Card)) {
                    list.forEachIndexed { i, e ->
                        if (i > 0) HorizontalDivider(color = Color(0xFFE3ECF0))
                        EventRow(e, now, e.id in alarms) { onToggle(e.id) }
                    }
                }
            }
        }
        item {
            Text(
                "به وقت تهران، از تقویم فدرال رزرو و BLS. ۱۰ دقیقه قبل از هر خبرِ زنگوله‌دار اعلان می‌گیری.",
                color = C.Muted, fontSize = 14.sp, modifier = Modifier.padding(20.dp),
            )
        }
    }
}

@Composable
private fun Chip(label: String, on: Boolean, onClick: () -> Unit) {
    Text(
        label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
        color = if (on) C.Paper else C.Ink,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) C.Ink else C.Soft)
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

@Composable
private fun Board(events: List<EconEvent>, now: Long) {
    val next = events.firstOrNull { it.impact == 3 && it.at > now } ?: events.firstOrNull { it.at > now } ?: return
    val left = next.at - now
    val hot = left < HOUR
    val (d, h, m, s) = split(left)
    val cells = if (d > 0) listOf(d to "روز", h to "ساعت", m to "دقیقه", s to "ثانیه") else listOf(h to "ساعت", m to "دقیقه", s to "ثانیه")
    val sameTime = events.count { it.at == next.at }
    Column(Modifier.fillMaxWidth().background(C.Deep).padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("انتشار بعدی", color = C.OnDeepMuted, fontSize = 14.sp)
            Text("${faDay(next.at)} · ${faTime(next.at)}", color = C.OnDeepMuted, fontSize = 14.sp)
        }
        // Countdown reads left-to-right like a clock, even in an RTL layout.
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr,
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Top) {
                cells.forEachIndexed { i, (v, label) ->
                    if (i > 0) Text(":", color = C.Paper.copy(alpha = .4f), fontSize = 48.sp, modifier = Modifier.padding(horizontal = 4.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AnimatedContent(v, transitionSpec = { (slideInVertically { -it / 3 } + fadeIn()) togetherWith fadeOut() }, label = "flip") { value ->
                            Text(pad(value), color = if (hot) Color(0xFFFFB896) else C.Paper, fontSize = 60.sp, fontWeight = FontWeight.Medium, lineHeight = 64.sp)
                        }
                        Text(label, color = C.OnDeepMuted, fontSize = 14.sp)
                    }
                }
            }
        }
        Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(FLAGS[next.country] ?: "", fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(next.title, color = C.Paper, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (sameTime > 1) Text("+ ${sameTime - 1} انتشار هم‌زمان", color = C.OnDeepMuted, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun EventRow(e: EconEvent, now: Long, on: Boolean, onToggle: () -> Unit) {
    val left = e.at - now
    val past = left <= 0
    val hot = !past && left < HOUR
    val soon = !past && left < DAY
    val pulse by rememberInfiniteTransition(label = "p").animateFloat(1f, if (hot) .55f else 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "a")
    Row(
        Modifier.fillMaxWidth().background(if (past) C.Paper else Color.Transparent).padding(horizontal = 14.dp, vertical = 12.dp),
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
                Text(e.title, color = C.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (e.forecast.isNotEmpty() || e.previous.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 2.dp)) {
                    if (e.forecast.isNotEmpty()) Text("پیش‌بینی ${e.forecast}", color = C.Muted, fontSize = 14.sp)
                    if (e.previous.isNotEmpty()) Text("قبلی ${e.previous}", color = C.Muted, fontSize = 14.sp)
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            if (past) {
                Text("منتشر شد", color = C.Muted, fontSize = 14.sp)
            } else {
                Text(
                    shortLeft(left), fontSize = 15.sp, fontWeight = FontWeight.Medium,
                    color = if (hot || soon) Color.White else C.Ink,
                    modifier = Modifier.alpha(pulse).clip(RoundedCornerShape(6.dp))
                        .background(if (hot) C.Ember else if (soon) C.Lead else C.Soft)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
                IconButton(onClick = onToggle) {
                    Icon(
                        if (on) Icons.Filled.NotificationsActive else Icons.Outlined.Notifications,
                        contentDescription = "یادآور", tint = if (on) C.Ember else Color(0xFF9DB3BD),
                    )
                }
            }
        }
    }
}
