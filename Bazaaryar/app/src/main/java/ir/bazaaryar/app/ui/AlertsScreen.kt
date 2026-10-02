package ir.bazaaryar.app.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.bazaaryar.app.data.AlertDirection
import ir.bazaaryar.app.data.AlertScope
import ir.bazaaryar.app.data.AlertSettings
import ir.bazaaryar.app.data.AlertWindow
import ir.bazaaryar.app.data.Coin
import ir.bazaaryar.app.data.CoinRule
import ir.bazaaryar.app.data.USDT_IRT_ID
import kotlin.math.roundToInt

@Composable
fun AlertsScreen(
    s: AlertSettings,
    rules: Map<String, CoinRule>,
    coins: List<Coin>,
    onUpdate: ((AlertSettings) -> AlertSettings) -> Unit,
    onRemoveRule: (String) -> Unit,
    onOpen: (String) -> Unit,
    onTest: () -> Unit,
) {
    val ctx = LocalContext.current
    val ruleList = remember(rules) { rules.values.toList() }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "main") {
            Section {
                SwitchRow(
                    "هشدار نوسان شدید",
                    "اعلان فوری و شخصی وقتی قیمت در یک بازه‌ی کوتاه تند حرکت کند",
                    s.enabled,
                ) { v -> onUpdate { it.copy(enabled = v) } }
            }
        }
        item(key = "config") {
            Section {
                var pct by remember(s.thresholdPct) { mutableFloatStateOf(s.thresholdPct) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "حداقل درصد تغییر", color = C.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(fa(fmtNum(pct.toDouble())) + "٪", color = C.Ember, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = pct,
                    onValueChange = { pct = (it * 2).roundToInt() / 2f },
                    onValueChangeFinished = { onUpdate { it.copy(thresholdPct = pct) } },
                    valueRange = 0.5f..20f,
                    colors = SliderDefaults.colors(thumbColor = C.Lead, activeTrackColor = C.Lead),
                )
                FieldLabel("در چه بازه‌ای")
                Pills { AlertWindow.entries.forEach { w -> Pill(w.label, s.window == w) { onUpdate { it.copy(window = w) } } } }
                FieldLabel("جهت حرکت")
                Pills { AlertDirection.entries.forEach { d -> Pill(d.label, s.direction == d) { onUpdate { it.copy(direction = d) } } } }
                FieldLabel("برای کدام ارزها")
                Pills { AlertScope.entries.forEach { sc -> Pill(sc.label, s.scope == sc) { onUpdate { it.copy(scope = sc) } } } }
                FieldLabel("فاصله‌ی دو اعلان برای یک ارز")
                Pills {
                    listOf(5, 15, 30, 60).forEach { m ->
                        Pill(fa(m.toString()) + " دقیقه", s.cooldownMin == m) { onUpdate { it.copy(cooldownMin = m) } }
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color(0xFFE3ECF0))
                SwitchRow(
                    "نوسان تتر / تومان هم اعلام شود",
                    "حتی اگر تتر در واچ‌لیستت نباشد",
                    s.includeUsdt,
                ) { v -> onUpdate { it.copy(includeUsdt = v) } }
            }
        }
        item(key = "personal") {
            Section {
                Text("شخصی‌سازی اعلان", color = C.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                var name by remember { mutableStateOf(s.name) }
                OutlinedTextField(
                    value = name,
                    onValueChange = { v ->
                        name = v.take(24)
                        onUpdate { it.copy(name = name.trim()) }
                    },
                    singleLine = true,
                    label = { Text("اسمت (اختیاری)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                val who = name.trim().let { if (it.isEmpty()) "" else "${it}، " }
                Text(
                    "پیش‌نمایش: 🚀 " + who + "BTC در " + s.window.label + " " + fa(fmtNum(s.thresholdPct.toDouble())) + "٪ رشد کرد",
                    color = C.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp),
                )
                Button(
                    onClick = onTest,
                    colors = ButtonDefaults.buttonColors(containerColor = C.Lead),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                ) { Text("ارسال اعلان آزمایشی") }
            }
        }
        item(key = "background") {
            Section {
                SwitchRow(
                    "پایش زنده در پس‌زمینه",
                    "تا وقتی اپ بسته است هم اعلان فوری بگیری. یک اعلان ثابت کوچک نمایش داده می‌شود.",
                    s.background,
                ) { v -> onUpdate { it.copy(background = v) } }
                Text(
                    if (s.scope == AlertScope.ALL) {
                        "پایش همه‌ی ۲۰۰ ارز در پس‌زمینه اینترنت بیشتری مصرف می‌کند."
                    } else {
                        "در پس‌زمینه فقط ارزهای واچ‌لیست و هشدارهای اختصاصی پایش می‌شوند (کم‌مصرف)."
                    },
                    color = C.Muted, fontSize = 12.sp,
                )
                TextButton(onClick = {
                    runCatching {
                        ctx.startActivity(
                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                }) { Text("حذف محدودیت باتری برای بازاریار", color = C.Lead) }
            }
        }
        item(key = "rules-title") {
            Text(
                "هشدارهای اختصاصی ارزها", color = C.Muted, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
            )
        }
        if (ruleList.isEmpty()) {
            item(key = "rules-empty") {
                Section {
                    Text(
                        "در تب بازار روی هر ارز بزن تا آستانه یا قیمت هدف مخصوص خودش را بگذاری.",
                        color = C.Muted, fontSize = 14.sp,
                    )
                }
            }
        }
        items(ruleList, key = { it.id }) { r ->
            RuleRow(r, coins.firstOrNull { it.id == r.id }, onOpen = { onOpen(r.id) }, onRemove = { onRemoveRule(r.id) })
        }
    }
}

@Composable
private fun FieldLabel(t: String) {
    Text(t, color = C.Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
}

@Composable
private fun Pills(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun RuleRow(r: CoinRule, coin: Coin?, onOpen: () -> Unit, onRemove: () -> Unit) {
    val toman = r.id == USDT_IRT_ID
    val unit: (Double) -> String = { v -> if (toman) fmtToman(v) else "$" + fmtPrice(v) }
    val parts = buildList<String> {
        r.pct?.let { add("آستانه " + fa(fmtNum(it.toDouble())) + "٪") }
        r.above?.let { add("بالای " + unit(it)) }
        r.below?.let { add("زیر " + unit(it)) }
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.Card).clickable(onClick = onOpen)
            .padding(start = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(if (toman) "تتر / تومان" else (coin?.symbol ?: r.id), color = C.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(parts.joinToString(" · "), color = C.Muted, fontSize = 13.sp)
        }
        IconButton(onClick = onRemove) { Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = C.Crimson) }
    }
}
