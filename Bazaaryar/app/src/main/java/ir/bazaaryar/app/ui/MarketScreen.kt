package ir.bazaaryar.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.bazaaryar.app.data.Coin
import ir.bazaaryar.app.data.LiveMode
import ir.bazaaryar.app.data.MarketOverview
import ir.bazaaryar.app.data.MarketState
import java.util.Locale

private enum class Sort(private val faL: String, private val enL: String) {
    RANK("رتبه", "Rank"),
    GAIN("بیشترین رشد", "Top gainers"),
    LOSS("بیشترین افت", "Top losers"),
    VOLUME("حجم معاملات", "Volume");

    val label: String get() = tr(faL, enL)
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
    val firstLoad = state.coins.none { !it.isToman } && state.error == null
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item(key = "head") { MarketHeader(state, onRefresh) }
        val ov = state.overview
        if (ov != null) item(key = "overview") { OverviewStrip(ov) }
        item(key = "search") { SearchBox(q) { q = it } }
        item(key = "sort") {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Sort.entries.forEach { s -> Pill(s.label, sort == s) { sort = s } }
            }
        }
        if (firstLoad) {
            items(8, key = { "shimmer$it" }) { ShimmerRow() }
        }
        if (shown.isEmpty() && state.coins.isNotEmpty()) {
            item(key = "empty") {
                Text(tr("چیزی پیدا نشد", "Nothing found"), color = C.Muted, fontSize = 15.sp, modifier = Modifier.padding(24.dp))
            }
        }
        items(shown, key = { it.id }) { c ->
            Box(Modifier.animateItem().padding(horizontal = 12.dp, vertical = 4.dp)) {
                CoinRow(c, c.id in watch, toman, rate, onToggle = { onToggle(c.id) }, onOpen = { onOpen(c.id) })
            }
        }
    }
}

@Composable
private fun SearchBox(q: String, onChange: (String) -> Unit) {
    TextField(
        value = q, onValueChange = onChange, singleLine = true,
        placeholder = { Text(tr("جستجوی نماد یا نام (مثلاً BTC یا تتر)", "Search symbol or name (e.g. BTC)"), color = C.Muted) },
        leadingIcon = { Icon(Icons.Filled.Search, null, tint = C.Muted) },
        trailingIcon = {
            AnimatedVisibility(q.isNotEmpty(), enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = tr("پاک کردن", "Clear"), tint = C.Muted)
                }
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = C.Soft, unfocusedContainerColor = C.Soft,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = C.Ink, unfocusedTextColor = C.Ink, cursorColor = C.Lead,
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun MarketHeader(state: MarketState, onRefresh: () -> Unit) {
    val liveNow = state.live == LiveMode.BINANCE || state.live == LiveMode.NOBITEX
    val live = when (state.live) {
        LiveMode.BINANCE -> tr("لحظه‌ای · بایننس", "Live · Binance")
        LiveMode.NOBITEX -> tr("لحظه‌ای · نوبیتکس", "Live · Nobitex")
        LiveMode.POLLING -> tr("هر ۶۰ ثانیه · CoinGecko", "Every 60 s · CoinGecko")
        LiveMode.CONNECTING -> tr("در حال اتصال…", "Connecting…")
    }
    val failed = state.error != null && state.coins.size <= 1
    val status = when {
        failed -> tr("اتصال برقرار نشد: ", "Connection failed: ") + state.error
        state.updatedAt > 0 -> live + " · " + faClock(state.updatedAt)
        else -> live
    }
    val tone = when {
        failed -> C.Crimson
        liveNow -> C.Up
        else -> C.Muted
    }
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(tr("۲۰۰ ارز برتر + تتر تومانی", "Top 200 coins + USDT/Toman"), color = C.Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (liveNow) {
                    LiveDot(C.Up)
                    Spacer(Modifier.width(6.dp))
                }
                Text(status, fontSize = 13.sp, color = tone, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (state.error != null && !failed) {
                Text(
                    "CoinGecko: ${state.error}", color = C.Crimson, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onRefresh) {
            if (state.loading) {
                val t = rememberInfiniteTransition(label = "spin")
                val r by t.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "r")
                Icon(
                    Icons.Filled.Refresh, contentDescription = tr("به‌روزرسانی", "Refresh"), tint = C.Lead,
                    modifier = Modifier.graphicsLayer { rotationZ = r },
                )
            } else {
                Icon(Icons.Filled.Refresh, contentDescription = tr("به‌روزرسانی", "Refresh"), tint = C.Lead)
            }
        }
    }
}

private fun fgLabel(raw: String): String = when (raw) {
    "Extreme Fear" -> tr("ترس شدید", "Extreme fear")
    "Fear" -> tr("ترس", "Fear")
    "Neutral" -> tr("خنثی", "Neutral")
    "Greed" -> tr("طمع", "Greed")
    "Extreme Greed" -> tr("طمع شدید", "Extreme greed")
    else -> raw
}

@Composable
private fun OverviewStrip(ov: MarketOverview) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (ov.totalCap > 0) {
            StatBox(
                tr("ارزش کل بازار", "Market cap"), fmtCap(ov.totalCap), fmtPct(ov.capChange24h),
                if (ov.capChange24h >= 0) C.Up else C.Crimson, Modifier.weight(1f),
            )
        }
        if (ov.btcDominance > 0) {
            StatBox(
                tr("دامیننس بیت‌کوین", "BTC dominance"), String.format(Locale.US, "%.1f%%", ov.btcDominance), "",
                C.Lead, Modifier.weight(1f), progress = (ov.btcDominance / 100).toFloat(),
            )
        }
        val fg = ov.fearGreed
        if (fg != null) {
            StatBox(
                tr("ترس و طمع", "Fear & Greed"), fa(fg.toString()), fgLabel(ov.fearLabel), fgTone(fg),
                Modifier.weight(1f), progress = fg / 100f,
            )
        }
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
    val tone by animateColorAsState(if (c.change24h >= 0) C.Up else C.Crimson, label = "tone")
    val price = coinPrice(c, toman, rate)
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().bounceClick(shape, onClick = onOpen).background(C.Card).border(1.dp, C.Line, shape)
            .padding(start = 12.dp, end = 2.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoinBadge(c.symbol, c.isToman)
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(c.symbol, color = C.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(Modifier.width(6.dp))
                Text(
                    if (c.isToman) "IR" else "#" + fa(c.rank.toString()),
                    color = if (c.isToman) C.Ember else C.Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(C.Soft).padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
            Text(c.name, color = C.Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.size(width = 52.dp, height = 26.dp)) {
            Sparkline(c.spark.takeLast(if (c.isToman) 120 else 24), tone, Modifier.fillMaxSize())
        }
        Column(Modifier.width(116.dp), horizontalAlignment = Alignment.End) {
            FlashText(price, c.price, C.Ink, if (price.length > 12) 13.sp else 15.sp)
            Text(fmtPct(c.change24h), color = tone, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        StarButton(starred, onToggle)
    }
}
