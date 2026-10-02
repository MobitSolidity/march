package ir.bazaaryar.app.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.bazaaryar.app.MarketState
import ir.bazaaryar.app.data.Coin

@Composable
fun MarketScreen(state: MarketState, watch: Set<String>, onToggle: (String) -> Unit, onRefresh: () -> Unit) {
    var q by rememberSaveable { mutableStateOf("") }
    val shown = state.coins.filter { q.isBlank() || it.symbol.contains(q, true) || it.name.contains(q, true) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("۱۰۰ ارز برتر", color = C.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                val status = when {
                    state.error != null && state.coins.isEmpty() -> "اتصال برقرار نشد: ${state.error}"
                    state.error != null -> "آخرین به‌روزرسانی ${faTime(state.updatedAt)} · خطا در دریافت تازه"
                    state.updatedAt > 0 -> "زنده · به‌روزرسانی ${faTime(state.updatedAt)}"
                    else -> "در حال دریافت…"
                }
                Text(status, color = if (state.error != null) C.Crimson else C.Muted, fontSize = 14.sp)
            }
            if (state.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = C.Lead)
            IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, contentDescription = "به‌روزرسانی", tint = C.Lead) }
        }
        TextField(
            value = q, onValueChange = { q = it }, singleLine = true,
            placeholder = { Text("جستجوی نماد یا نام") },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = C.Muted) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = C.Soft, unfocusedContainerColor = C.Soft,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        )
        LazyColumn(Modifier.padding(horizontal = 12.dp).clip(RoundedCornerShape(16.dp)).background(C.Card)) {
            items(shown, key = { it.id }) { c ->
                CoinRow(c, c.id in watch) { onToggle(c.id) }
                HorizontalDivider(color = Color(0xFFE3ECF0))
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
fun CoinRow(c: Coin, starred: Boolean, onToggle: () -> Unit) {
    val up = c.change24h >= 0
    val tone = if (up) C.Up else C.Crimson
    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("${c.rank}", color = C.Muted, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.width(28.dp))
        Column(Modifier.weight(1f).padding(horizontal = 6.dp)) {
            Text(c.symbol, color = C.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(c.name, color = C.Muted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.size(width = 60.dp, height = 24.dp)) { Sparkline(c.spark, tone, Modifier.fillMaxSize()) }
        Column(Modifier.width(100.dp), horizontalAlignment = Alignment.End) {
            Text("$" + fmtPrice(c.price), color = C.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(fmtPct(c.change24h), color = tone, fontSize = 14.sp)
        }
        IconButton(onClick = onToggle) {
            Icon(if (starred) Icons.Filled.Star else Icons.Outlined.StarBorder, contentDescription = "واچ‌لیست", tint = if (starred) C.Ember else Color(0xFF9DB3BD))
        }
    }
}
