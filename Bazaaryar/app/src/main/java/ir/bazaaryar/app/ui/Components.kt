package ir.bazaaryar.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

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

@Composable
fun ImpactBars(n: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        for (i in 1..3) {
            val c = if (i <= n) (if (n == 3) C.Crimson else C.Ember) else Color(0xFFCFDBE1)
            Box(Modifier.size(width = 5.dp, height = 12.dp).background(c, RoundedCornerShape(2.dp)))
        }
    }
}
