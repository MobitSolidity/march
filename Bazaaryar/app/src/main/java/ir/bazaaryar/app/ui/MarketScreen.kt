package ir.bazaaryar.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.bazaaryar.app.data.Coin
import ir.bazaaryar.app.data.LiveMode
import ir.bazaaryar.app.data.MarketOverview
import ir.bazaaryar.app.data.MarketState
import java.util.Locale

private enum class Sort(val label: String) {
    RANK("رتبه"), GAIN("بیشترین رشد"), LOSS("بیشترین افت"), VOLUME("حجم معاملات")
}

@Composable
fun MarketScreen(
    state: MarketState,
    watch: Set<String>,
    toman: Boolean,
    onToggle: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpen: (String) -> Unit,
) {
    var q by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(Sort.RANK) }
    val rate = state.usdt?.toman
    val shown = remember(state.coins, q, sort) {
        val needle = q.trim()
        val f = state.coins.filter { needle.isEmpty() || it.symbol.contains(needle, true) || it.name.contains(needle, true) }
        val (pinned, rest) = f.partition { it.isToman }
        pinned + when (sort) {
            Sort.RANK -> rest
            Sort.GAIN -> rest.sortedByDescending { it.change24h }
            Sort.LOSS -> rest.sortedBy { it.change24h }
            Sort.VOLUME -> rest.sortedByDescending { it.volume24h }
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item(key = "head") { MarketHeader(state, onRefresh) }
        val ov = state.overview
        if (ov != null) item(key = "overview") { OverviewStrip(ov) }
        item(key = "search") {
            TextField(
                value = q, onValueChange = { q = it }, singleLine = true,
                placeholder = { Text("جستجوی نماد یا نام (مثلاً BTC یا تتر)") },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = C.Muted) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = C.Soft, unfocusedContainerColor = C.Soft,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        item(key = "sort") {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Sort.entries.forEach { s -> Pill(s.label, sort == s) { sort = s } }
            }
        }
        if (shown.isEmpty() && state.coins.isNotEmpty()) {
            item(key = "empty") {
                Text("چیزی پیدا نشد", color = C.Muted, fontSize = 15.sp, modifier = Modifier.padding(24.dp))
            }
        }
        items(shown, key = { it.id }) { c ->
            Column(Modifier.padding(horizontal = 12.dp).background(C.Card)) {
                CoinRow(c, c.id in watch, toman, rate, onToggle = { onToggle(c.id) }, onOpen = { onOpen(c.id) })
                HorizontalDivider(color = Color(0xFFE3ECF0))
            }
        }
    }
}

@Composable
private fun MarketHeader(state: MarketState, onRefresh: () -> Unit) {
    val liveNow = state.live == LiveMode.BINANCE || state.live == LiveMode.NOBITEX
    val live = when (state.live) {
        LiveMode.BINANCE -> "● لحظه‌ای · بایننس"
        LiveMode.NOBITEX -> "● لحظه‌ای · نوبیتکس"
        LiveMode.POLLING -> "هر ۶۰ ثانیه · CoinGecko"
        LiveMode.CONNECTING -> "در حال اتصال…"
    }
    val failed = state.error != null && state.coins.size <= 1
    val status = when {
        failed -> "اتصال برقرار نشد: ${state.error}"
        state.updatedAt > 0 -> live + " · " + faClock(state.updatedAt)
        else -> live
    }
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("۲۰۰ ارز برتر + تتر تومانی", color = C.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                status, fontSize = 13.sp,
                color = when {
                    failed -> C.Crimson
                    liveNow -> C.Up
                    else -> C.Muted
                },
            )
            if (state.error != null && !failed) {
                Text(
                    "CoinGecko: ${state.error}", color = C.Crimson, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (state.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = C.Lead)
        IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, contentDescription = "به‌روزرسانی", tint = C.Lead) }
    }
}

@Composable
private fun OverviewStrip(ov: MarketOverview) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (ov.totalCap > 0) {
            StatBox(
                "ارزش کل بازار", fmtCap(ov.totalCap), fmtPct(ov.capChange24h),
                if (ov.capChange24h >= 0) C.Up else C.Crimson, Modifier.weight(1f),
            )
        }
        if (ov.btcDominance > 0) {
            StatBox("دامیننس بیت‌کوین", String.format(Locale.US, "%.1f%%", ov.btcDominance), "", C.Muted, Modifier.weight(1f))
        }
        val fg = ov.fearGreed
        if (fg != null) StatBox("شاخص ترس و طمع", fg.toString(), ov.fearLabel, fgTone(fg), Modifier.weight(1f))
    }
}

private fun fgTone(v: Int): Color = when {
    v < 25 -> C.Crimson
    v < 45 -> C.Ember
    v <= 55 -> C.Muted
    else -> C.Up
}

@Composable
fun CoinRow(c: Coin, starred: Boolean, toman: Boolean, rate: Double?, onToggle: () -> Unit, onOpen: () -> Unit) {
    val tone = if (c.change24h >= 0) C.Up else C.Crimson
    val price = coinPrice(c, toman, rate)
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (c.isToman) "IR" else "${c.rank}",
            color = if (c.isToman) C.Ember else C.Muted, fontSize = 13.sp,
            fontWeight = if (c.isToman) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center, modifier = Modifier.width(30.dp),
        )
        Column(Modifier.weight(1f).padding(horizontal = 6.dp)) {
            Text(c.symbol, color = C.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(c.name, color = C.Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.size(width = 56.dp, height = 24.dp)) {
            Sparkline(c.spark.takeLast(if (c.isToman) 120 else 24), tone, Modifier.fillMaxSize())
        }
        Column(Modifier.width(124.dp), horizontalAlignment = Alignment.End) {
            FlashText(price, c.price, C.Ink, if (price.length > 12) 13.sp else 15.sp)
            Text(fmtPct(c.change24h), color = tone, fontSize = 13.sp)
        }
        IconButton(onClick = onToggle) {
            Icon(
                if (starred) Icons.Filled.Star else Icons.Outlined.StarBorder, contentDescription = "واچ‌لیست",
                tint = if (starred) C.Ember else Color(0xFF9DB3BD),
            )
        }
    }
}
