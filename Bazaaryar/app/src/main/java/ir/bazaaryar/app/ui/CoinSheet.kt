package ir.bazaaryar.app.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.bazaaryar.app.data.Coin
import ir.bazaaryar.app.data.CoinRule
import ir.bazaaryar.app.data.Source
import kotlin.math.roundToInt

/** Coin details: live price, 7d chart, key stats, and per-coin alert settings. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinSheet(
    c: Coin,
    toman: Boolean,
    rate: Double?,
    starred: Boolean,
    rule: CoinRule?,
    defaultPct: Float,
    session: List<Double>,
    onToggleStar: () -> Unit,
    onSaveRule: (CoinRule) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = C.Card) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            ) {
                SheetBody(c, toman, rate, starred, rule, defaultPct, session, onToggleStar, onSaveRule)
            }
        }
    }
}

@Composable
private fun SheetBody(
    c: Coin,
    toman: Boolean,
    rate: Double?,
    starred: Boolean,
    rule: CoinRule?,
    defaultPct: Float,
    session: List<Double>,
    onToggleStar: () -> Unit,
    onSaveRule: (CoinRule) -> Unit,
) {
    val ctx = LocalContext.current
    val tone = if (c.change24h >= 0) C.Up else C.Crimson
    val unit: (Double) -> String = { v -> if (c.isToman) fmtToman(v) else "$" + fmtPrice(v) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(c.symbol, color = C.Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(
                if (c.isToman) c.name else c.name + " · رتبه " + fa(c.rank.toString()),
                color = C.Muted, fontSize = 14.sp,
            )
        }
        IconButton(onClick = onToggleStar) {
            Icon(
                if (starred) Icons.Filled.Star else Icons.Outlined.StarBorder, contentDescription = "واچ‌لیست",
                tint = if (starred) C.Ember else Color(0xFF9DB3BD),
            )
        }
    }

    val price = coinPrice(c, toman, rate)
    FlashText(price, c.price, C.Ink, if (price.length > 14) 24.sp else 30.sp, Modifier.padding(top = 8.dp), FontWeight.Bold)
    val alt = when {
        c.isToman -> null
        toman -> "$" + fmtPrice(c.price)
        rate != null && rate > 0 -> "≈ " + fmtToman(c.price * rate)
        else -> null
    }
    if (alt != null) Text(alt, color = C.Muted, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))
    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Tag("۲۴ ساعت " + fmtPct(c.change24h), tone)
        if (!c.isToman && c.change7d != 0.0) Tag("۷ روز " + fmtPct(c.change7d), if (c.change7d >= 0) C.Up else C.Crimson)
        Tag(
            when (c.source) {
                Source.BINANCE -> "لحظه‌ای · بایننس"
                Source.NOBITEX -> "لحظه‌ای · صرافی ایرانی"
                Source.COINGECKO -> "CoinGecko"
            },
            C.Lead,
        )
    }

    val weekly = !c.isToman && c.spark.size >= 2
    val chart = if (weekly) c.spark else session
    Text(
        if (weekly) "نمودار ۷ روز گذشته" else "نمودار از زمان باز شدن اپ",
        color = C.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp),
    )
    if (chart.size >= 2) {
        AreaChart(
            chart, if (chart.last() >= chart.first()) C.Up else C.Crimson,
            Modifier.fillMaxWidth().height(160.dp).padding(top = 8.dp),
        )
    } else {
        Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
            Text("داده‌ی نمودار در حال جمع‌آوری است…", color = C.Muted, fontSize = 13.sp)
        }
    }

    val stats = buildList<Pair<String, String>> {
        if (c.high24h > 0) add("سقف ۲۴ ساعت" to unit(c.high24h))
        if (c.low24h > 0) add("کف ۲۴ ساعت" to unit(c.low24h))
        if (c.volume24h > 0) add("حجم ۲۴ ساعت" to fmtCap(c.volume24h))
        if (c.marketCap > 0) add("ارزش بازار" to fmtCap(c.marketCap))
        if (c.ath > 0) add("سقف تاریخی (ATH)" to unit(c.ath))
        if (c.ath > 0) add("فاصله تا ATH" to fmtPct(c.athChange))
    }
    stats.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { (k, v) -> StatBox(k, v, "", C.Muted, Modifier.weight(1f), bg = C.Paper) }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }

    // ---- Per-coin alert ----
    HorizontalDivider(Modifier.padding(vertical = 16.dp), color = Color(0xFFE3ECF0))
    Text("🔔 هشدار اختصاصی " + c.symbol, color = C.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    var custom by remember(c.id, rule) { mutableStateOf(rule?.pct != null) }
    var pct by remember(c.id, rule) { mutableFloatStateOf(rule?.pct ?: defaultPct) }
    var above by remember(c.id, rule) { mutableStateOf(rule?.above?.let { plainNum(it) } ?: "") }
    var below by remember(c.id, rule) { mutableStateOf(rule?.below?.let { plainNum(it) } ?: "") }

    SwitchRow(
        "آستانه‌ی نوسان مخصوص این ارز",
        if (custom) "" else "الان از تنظیم کلی (" + fa(fmtNum(defaultPct.toDouble())) + "٪) استفاده می‌شود",
        custom,
    ) { custom = it }
    if (custom) {
        Text(fa(fmtNum(pct.toDouble())) + "٪", color = C.Ember, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Slider(
            value = pct, onValueChange = { pct = (it * 2).roundToInt() / 2f }, valueRange = 0.5f..20f,
            colors = SliderDefaults.colors(thumbColor = C.Lead, activeTrackColor = C.Lead),
        )
    }
    val unitLabel = if (c.isToman) "تومان" else "دلار"
    OutlinedTextField(
        value = above, onValueChange = { above = it }, singleLine = true,
        label = { Text("اعلان وقتی بالاتر از ($unitLabel)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
    OutlinedTextField(
        value = below, onValueChange = { below = it }, singleLine = true,
        label = { Text("اعلان وقتی پایین‌تر از ($unitLabel)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
    Text(
        "هشدار قیمت هدف یک بار ارسال می‌شود و بعد خودکار پاک می‌شود.",
        color = C.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp),
    )
    Button(
        onClick = {
            onSaveRule(CoinRule(c.id, pct = if (custom) pct else null, above = parseNum(above), below = parseNum(below)))
            Toast.makeText(ctx, "هشدار " + c.symbol + " ذخیره شد", Toast.LENGTH_SHORT).show()
        },
        colors = ButtonDefaults.buttonColors(containerColor = C.Lead),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    ) { Text("ذخیره هشدار") }
}
