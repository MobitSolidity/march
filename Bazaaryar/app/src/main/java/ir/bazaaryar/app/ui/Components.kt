package ir.bazaaryar.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Mini chart with a soft fill under the line. */
@Composable
fun Sparkline(data: List<Double>, color: Color, modifier: Modifier = Modifier) {
    if (data.size < 2) return
    Canvas(modifier) {
        val min = data.min()
        val range = (data.max() - min).takeIf { it > 0 } ?: 1.0
        val line = Path()
        val fill = Path()
        data.forEachIndexed { i, v ->
            // Canvas is drawn in LTR coordinates regardless of layout direction: time flows left to right.
            val x = size.width * i / (data.size - 1)
            val y = (size.height - ((v - min) / range * (size.height - 2)) - 1).toFloat()
            if (i == 0) {
                line.moveTo(x, y)
                fill.moveTo(x, size.height)
                fill.lineTo(x, y)
            } else {
                line.lineTo(x, y)
                fill.lineTo(x, y)
            }
        }
        fill.lineTo(size.width, size.height)
        fill.close()
        drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.22f), Color.Transparent)))
        drawPath(line, color, style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Bigger chart with a gradient fill that draws itself in from the start. */
@Composable
fun AreaChart(data: List<Double>, color: Color, modifier: Modifier = Modifier) {
    if (data.size < 2) return
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
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
        clipRect(right = w * reveal.value) {
            drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.30f), color.copy(alpha = 0f))))
            drawPath(line, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
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

/** Selectable chip with animated colours and press bounce. */
@Composable
fun Pill(label: String, on: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) C.Lead else C.Soft, tween(220), label = "pillBg")
    val fg by animateColorAsState(if (on) C.OnLead else C.Ink, tween(220), label = "pillFg")
    Text(
        label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = fg, maxLines = 1,
        modifier = Modifier.bounceClick(RoundedCornerShape(50), onClick = onClick).background(bg)
            .padding(horizontal = 14.dp, vertical = 7.dp),
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

/** Small stat card; [progress] (0..1) adds an animated bar. */
@Composable
fun StatBox(
    label: String,
    value: String,
    sub: String,
    tone: Color,
    modifier: Modifier = Modifier,
    bg: Color = C.Card,
    progress: Float? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(modifier.clip(shape).background(bg).border(1.dp, C.Line, shape).padding(12.dp)) {
        Text(label, color = C.Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, color = C.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        if (sub.isNotEmpty()) Text(sub, color = tone, fontSize = 12.sp, maxLines = 1)
        if (progress != null) {
            val bar = remember { Animatable(0f) }
            LaunchedEffect(progress) { bar.animateTo(progress.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing)) }
            Box(
                Modifier.padding(top = 6.dp).fillMaxWidth().height(4.dp)
                    .clip(RoundedCornerShape(2.dp)).background(C.Soft),
            ) {
                Box(Modifier.fillMaxWidth(bar.value).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(tone))
            }
        }
    }
}

/** Rounded card container that animates its size when content appears/disappears. */
@Composable
fun Section(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(C.Card).border(1.dp, C.Line, shape)
            .animateContentSize().padding(16.dp),
        content = content,
    )
}

@Composable
fun SwitchRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onChange(!checked) }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, color = C.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (sub.isNotEmpty()) Text(sub, color = C.Muted, fontSize = 13.sp)
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = C.Lead, checkedThumbColor = C.OnLead,
                uncheckedTrackColor = C.Soft, uncheckedThumbColor = C.Faint, uncheckedBorderColor = C.Line,
            ),
        )
    }
}

@Composable
fun ImpactBars(n: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        for (i in 1..3) {
            val c = if (i <= n) (if (n == 3) C.Crimson else C.Ember) else C.Line
            Box(Modifier.size(width = 5.dp, height = 12.dp).background(c, RoundedCornerShape(2.dp)))
        }
    }
}
