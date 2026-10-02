package ir.bazaaryar.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Clickable surface that springs down a little while pressed. Clips to [shape] so the ripple matches. */
fun Modifier.bounceClick(
    shape: Shape = RoundedCornerShape(16.dp),
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.96f else 1f,
        spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
        .clip(shape)
        .clickable(interactionSource = source, indication = LocalIndication.current, enabled = enabled, onClick = onClick)
}

/** Small pulsing "live" dot. */
@Composable
fun LiveDot(color: Color, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "live")
    val k by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "k")
    Box(modifier.size(12.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(8.dp).graphicsLayer {
                val s = 1f + 1.6f * k
                scaleX = s
                scaleY = s
                alpha = 0.55f * (1f - k)
            }.background(color, CircleShape),
        )
        Box(Modifier.size(7.dp).background(color, CircleShape))
    }
}

/** Moving highlight used for loading placeholders. */
@Composable
fun shimmerBrush(): Brush {
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(-400f, 1400f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "x")
    val hi = if (C.isDark) C.Line else C.Card
    return Brush.linearGradient(listOf(C.Soft, hi, C.Soft), start = Offset(x, 0f), end = Offset(x + 400f, 200f))
}

/** Skeleton coin row shown while the first price list loads. */
@Composable
fun ShimmerRow() {
    val b = shimmerBrush()
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp)).background(C.Card).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(b))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Box(Modifier.fillMaxWidth(0.35f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(b))
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth(0.55f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(b))
        }
        Box(Modifier.width(84.dp).height(14.dp).clip(RoundedCornerShape(7.dp)).background(b))
    }
}

/** Round coloured avatar with the coin's initial; colour is stable per symbol. */
@Composable
fun CoinBadge(symbol: String, toman: Boolean, size: Dp = 36.dp) {
    val hue = ((symbol.hashCode() % 360) + 360) % 360
    val base = if (toman) C.Up else Color.hsl(hue.toFloat(), 0.62f, if (C.isDark) 0.62f else 0.50f)
    Box(
        Modifier.size(size).clip(CircleShape)
            .background(Brush.linearGradient(listOf(base, base.copy(alpha = 0.7f)))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (toman) "₮" else symbol.take(1),
            color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = (size.value * 0.42f).sp,
        )
    }
}

/** Watch-list star that pops when toggled. */
@Composable
fun StarButton(starred: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scale = remember { Animatable(1f) }
    var seen by remember { mutableStateOf(starred) }
    LaunchedEffect(starred) {
        if (seen != starred) {
            seen = starred
            scale.snapTo(0.55f)
            scale.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessMedium))
        }
    }
    val tint by animateColorAsState(if (starred) C.Ember else C.Faint, label = "star")
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            if (starred) Icons.Filled.Star else Icons.Outlined.StarBorder,
            contentDescription = tr("واچ‌لیست", "Watchlist"),
            tint = tint,
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
        )
    }
}
