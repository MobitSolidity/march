package ir.bazaaryar.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.bazaaryar.app.data.Coin
import ir.bazaaryar.app.data.EconEvent

@Composable
fun WatchScreen(
    coins: List<Coin>, watch: Set<String>, events: List<EconEvent>, alarms: Set<String>, now: Long,
    toman: Boolean, rate: Double?,
    onToggleCoin: (String) -> Unit, onToggleAlarm: (String) -> Unit, onOpen: (String) -> Unit, go: (Int) -> Unit,
) {
    val mine = coins.filter { it.id in watch }
    val myEvents = events.filter { it.id in alarms && it.at > now }
    LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        item { Header(tr("ارزهای من", "My coins")) }
        if (mine.isEmpty()) {
            item { Empty(Icons.Outlined.StarBorder, tr("با ستاره از تب بازار اضافه کن", "Add coins with the star on the Market tab")) { go(1) } }
        }
        mine.chunked(2).forEach { pair ->
            item(key = pair.joinToString { it.id }) {
                Row(Modifier.animateItem().fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { c ->
                        WatchCard(c, toman, rate, Modifier.weight(1f), onRemove = { onToggleCoin(c.id) }, onOpen = { onOpen(c.id) })
                    }
                    if (pair.size == 1) Box(Modifier.weight(1f))
                }
            }
        }
        item { Header(tr("یادآورهای خبری", "News reminders")) }
        if (myEvents.isEmpty()) {
            item { Empty(Icons.Outlined.NotificationsNone, tr("با زنگوله از تب اخبار اضافه کن", "Add reminders with the bell on the News tab")) { go(0) } }
        } else {
            item {
                val shape = RoundedCornerShape(20.dp)
                Column(Modifier.clip(shape).background(C.Card).border(1.dp, C.Line, shape)) {
                    myEvents.forEachIndexed { i, e ->
                        if (i > 0) HorizontalDivider(color = C.Line)
                        EventRow(e, now, true) { onToggleAlarm(e.id) }
                    }
                }
            }
        }
        item { Box(Modifier.height(24.dp)) }
    }
}

@Composable
private fun Header(t: String) =
    Text(t, color = C.Muted, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 8.dp))

@Composable
private fun Empty(icon: ImageVector, t: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier.fillMaxWidth().bounceClick(shape, onClick = onClick).border(1.5.dp, C.Faint, shape).padding(vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = C.Faint, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(6.dp))
        Text(t, color = C.Muted, fontSize = 15.sp)
    }
}

@Composable
private fun WatchCard(c: Coin, toman: Boolean, rate: Double?, modifier: Modifier, onRemove: () -> Unit, onOpen: () -> Unit) {
    val tone by animateColorAsState(if (c.change24h >= 0) C.Up else C.Crimson, label = "tone")
    val price = coinPrice(c, toman, rate)
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.bounceClick(shape, onClick = onOpen)
            .background(Brush.verticalGradient(listOf(tone.copy(alpha = 0.14f), C.Card)))
            .border(1.dp, C.Line, shape).padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoinBadge(c.symbol, c.isToman, 28.dp)
            Spacer(Modifier.width(8.dp))
            Text(c.symbol, color = C.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Filled.Close, tr("حذف", "Remove"), tint = C.Faint)
            }
        }
        if (c.isToman) Text(tr("تتر / تومان", "USDT / Toman"), color = C.Muted, fontSize = 12.sp)
        FlashText(price, c.price, C.Ink, if (price.length > 12) 16.sp else 20.sp, Modifier.padding(top = 6.dp), FontWeight.Bold)
        Text(fmtPct(c.change24h), color = tone, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Sparkline(c.spark.takeLast(if (c.isToman) 120 else 48), tone, Modifier.fillMaxWidth().height(36.dp).padding(top = 6.dp))
    }
}
