package ir.bazaaryar.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
        item { Header("ارزهای من") }
        if (mine.isEmpty()) item { Empty("با ستاره از تب بازار اضافه کن") { go(1) } }
        mine.chunked(2).forEach { pair ->
            item(key = pair.joinToString { it.id }) {
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { c ->
                        WatchCard(c, toman, rate, Modifier.weight(1f), onRemove = { onToggleCoin(c.id) }, onOpen = { onOpen(c.id) })
                    }
                    if (pair.size == 1) Box(Modifier.weight(1f))
                }
            }
        }
        item { Header("یادآورهای خبری") }
        if (myEvents.isEmpty()) {
            item { Empty("با زنگوله از تب اخبار اضافه کن") { go(0) } }
        } else {
            item {
                Column(Modifier.clip(RoundedCornerShape(16.dp)).background(C.Card)) {
                    myEvents.forEachIndexed { i, e ->
                        if (i > 0) HorizontalDivider(color = Color(0xFFE3ECF0))
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
private fun Empty(t: String, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(2.dp, Color(0xFFC4D5DC), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(vertical = 28.dp),
        contentAlignment = Alignment.Center,
    ) { Text(t, color = C.Muted, fontSize = 15.sp) }
}

@Composable
private fun WatchCard(c: Coin, toman: Boolean, rate: Double?, modifier: Modifier, onRemove: () -> Unit, onOpen: () -> Unit) {
    val tone = if (c.change24h >= 0) C.Up else C.Crimson
    val price = coinPrice(c, toman, rate)
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(C.Card).clickable(onClick = onOpen).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(c.symbol, color = C.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "حذف", tint = Color(0xFF9DB3BD)) }
        }
        if (c.isToman) Text("تتر / تومان", color = C.Muted, fontSize = 12.sp)
        FlashText(price, c.price, C.Ink, if (price.length > 12) 16.sp else 20.sp, Modifier.padding(top = 6.dp))
        Text(fmtPct(c.change24h), color = tone, fontSize = 14.sp)
        Sparkline(c.spark.takeLast(if (c.isToman) 120 else 48), tone, Modifier.fillMaxWidth().height(34.dp).padding(top = 6.dp))
    }
}
