package ir.bazaaryar.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Sparkline(data: List<Double>, color: Color, modifier: Modifier = Modifier) {
    if (data.size < 2) return
    Canvas(modifier) {
        val min = data.min()
        val range = (data.max() - min).takeIf { it > 0 } ?: 1.0
        val path = Path()
        data.forEachIndexed { i, v ->
            // Canvas is drawn in LTR coordinates regardless of layout direction: time flows left to right.
            val p = Offset(size.width * i / (data.size - 1), (size.height - ((v - min) / range * (size.height - 2)) - 1).toFloat())
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        drawPath(path, color, style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Bigger chart with a soft gradient fill, for the coin sheet. */
@Composable
fun AreaChart(data: List<Double>, color: Color, modifier: Modifier = Modifier) {
    if (data.size < 2) return
    Canvas(modifier) {
        val min = data.min()
        val range = (data.max() - min).takeIf { it > 0 } ?: 1.0
        val w = size.width
        val h = size.height
        val line = Path()
        val fill = Path()
        data.forEachIndexed { i, v ->
            val x = w * i / (data.size - 1)
            val y = (h - ((v - min) / range * (h - 4)) - 2).toFloat()
            if (i == 0) {
                line.moveTo(x, y)
                fill.moveTo(x, h)
                fill.lineTo(x, y)
            } else {
                line.lineTo(x, y)
                fill.lineTo(x, y)
            }
        }
        fill.lineTo(w, h)
        fill.close()
        drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0f))))
        drawPath(line, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Price text that flashes green/red for a moment whenever the value ticks. */
@Composable
fun FlashText(
    text: String,
    value: Double,
    color: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.SemiBold,
) {
    val prev = remember { mutableDoubleStateOf(value) }
    val glow = remember { Animatable(0f) }
    var dir by remember { mutableIntStateOf(0) }
    LaunchedEffect(value) {
        val p = prev.doubleValue
        prev.doubleValue = value
        if (p > 0 && value != p) {
            dir = if (value > p) 1 else -1
            glow.snapTo(1f)
            glow.animateTo(0f, tween(900))
        }
    }
    val bg = (if (dir >= 0) C.Up else C.Crimson).copy(alpha = 0.22f * glow.value)
    Text(
        text, color = color, fontSize = fontSize, fontWeight = fontWeight, maxLines = 1, softWrap = false,
        modifier = modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 4.dp),
    )
}

@Composable
fun Pill(label: String, on: Boolean, onClick: () -> Unit) {
    Text(
        label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
        color = if (on) C.Paper else C.Ink,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) C.Ink else C.Soft)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

@Composable
fun Tag(text: String, tone: Color) {
    Text(
        text, color = tone, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(tone.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
fun StatBox(label: String, value: String, sub: String, tone: Color, modifier: Modifier = Modifier, bg: Color = C.Card) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(bg).padding(10.dp)) {
        Text(label, color = C.Muted, fontSize = 12.sp, maxLines = 1)
        Text(value, color = C.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        if (sub.isNotEmpty()) Text(sub, color = tone, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
fun Section(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.Card).padding(14.dp),
        content = content,
    )
}

@Composable
fun SwitchRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, color = C.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (sub.isNotEmpty()) Text(sub, color = C.Muted, fontSize = 13.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = C.Lead))
    }
}

@Composable
fun ImpactBars(n: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        for (i in 1..3) {
            val c = if (i <= n) (if (n == 3) C.Crimson else C.Ember) else Color(0xFFCFDBE1)
            Box(Modifier.size(width = 5.dp, height = 12.dp).background(c, RoundedCornerShape(2.dp)))
        }
    }
}
