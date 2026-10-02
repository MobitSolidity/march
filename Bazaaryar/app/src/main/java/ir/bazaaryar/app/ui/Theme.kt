package ir.bazaaryar.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object C {
    val Deep = Color(0xFF004759)
    val Lead = Color(0xFF006289)
    val Ember = Color(0xFFE84A00)
    val Crimson = Color(0xFFBB1D2C)
    val Up = Color(0xFF0B6B4A)
    val Paper = Color(0xFFEEF3F6)
    val Card = Color(0xFFF8FBFC)
    val Ink = Color(0xFF10303B)
    val Muted = Color(0xFF5B7480)
    val Soft = Color(0xFFDDE8ED)
    val OnDeepMuted = Color(0xFFC2E7F5)
}

@Composable
fun BazaaryarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = C.Lead, onPrimary = Color.White,
            background = C.Paper, surface = C.Card, onSurface = C.Ink, onBackground = C.Ink,
            secondaryContainer = Color(0xFFCFE4EC),
        ),
        content = content,
    )
}
