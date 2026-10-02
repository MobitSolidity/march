package ir.bazaaryar.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import ir.bazaaryar.app.R

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Indigo + cyan crypto palette, tuned separately for light and dark. */
class Palette(
    val deep: Color,
    val deep2: Color,
    val lead: Color,
    val onLead: Color,
    val aqua: Color,
    val ember: Color,
    val crimson: Color,
    val up: Color,
    val paper: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val soft: Color,
    val line: Color,
    val faint: Color,
    val onDeep: Color,
    val onDeepMuted: Color,
)

val LightPalette = Palette(
    deep = Color(0xFF312E81), deep2 = Color(0xFF4F46E5),
    lead = Color(0xFF6366F1), onLead = Color.White, aqua = Color(0xFF0891B2),
    ember = Color(0xFFEA580C), crimson = Color(0xFFE11D48), up = Color(0xFF059669),
    paper = Color(0xFFF4F6FB), card = Color(0xFFFFFFFF), ink = Color(0xFF0F172A), muted = Color(0xFF64748B),
    soft = Color(0xFFEDF0F7), line = Color(0xFFE4E8F1), faint = Color(0xFFA3AEC2),
    onDeep = Color.White, onDeepMuted = Color(0xFFC7D2FE),
)

val DarkPalette = Palette(
    deep = Color(0xFF1E1B4B), deep2 = Color(0xFF3730A3),
    lead = Color(0xFF818CF8), onLead = Color(0xFF0B1020), aqua = Color(0xFF22D3EE),
    ember = Color(0xFFF97316), crimson = Color(0xFFFB7185), up = Color(0xFF34D399),
    paper = Color(0xFF0B1020), card = Color(0xFF141A2E), ink = Color(0xFFE7EAF6), muted = Color(0xFF8D97B0),
    soft = Color(0xFF1D2540), line = Color(0xFF242C47), faint = Color(0xFF4E5978),
    onDeep = Color.White, onDeepMuted = Color(0xFFA5B4FC),
)

/**
 * App colours. Backed by snapshot state, so everything (including Canvas drawing) re-reads the
 * right colour when the theme flips. Set by [BazaaryarTheme].
 */
object C {
    private var p by mutableStateOf(LightPalette)

    val isDark: Boolean get() = p === DarkPalette
    val Deep: Color get() = p.deep
    val Lead: Color get() = p.lead
    val OnLead: Color get() = p.onLead
    val Aqua: Color get() = p.aqua
    val Ember: Color get() = p.ember
    val Crimson: Color get() = p.crimson
    val Up: Color get() = p.up
    val Paper: Color get() = p.paper
    val Card: Color get() = p.card
    val Ink: Color get() = p.ink
    val Muted: Color get() = p.muted
    val Soft: Color get() = p.soft
    val Line: Color get() = p.line
    val Faint: Color get() = p.faint
    val OnDeep: Color get() = p.onDeep
    val OnDeepMuted: Color get() = p.onDeepMuted
    val headerBrush: Brush get() = Brush.linearGradient(listOf(p.deep, p.deep2))

    fun use(dark: Boolean) {
        val next = if (dark) DarkPalette else LightPalette
        if (p !== next) p = next
    }
}

/** Vazirmatn: modern Persian typeface with matching Latin glyphs, so FA and EN look consistent. */
val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
    Font(R.font.vazirmatn_extrabold, FontWeight.ExtraBold),
)

private val AppType: Typography = Typography().let { t ->
    t.copy(
        displayLarge = t.displayLarge.copy(fontFamily = Vazirmatn),
        displayMedium = t.displayMedium.copy(fontFamily = Vazirmatn),
        displaySmall = t.displaySmall.copy(fontFamily = Vazirmatn),
        headlineLarge = t.headlineLarge.copy(fontFamily = Vazirmatn),
        headlineMedium = t.headlineMedium.copy(fontFamily = Vazirmatn),
        headlineSmall = t.headlineSmall.copy(fontFamily = Vazirmatn),
        titleLarge = t.titleLarge.copy(fontFamily = Vazirmatn),
        titleMedium = t.titleMedium.copy(fontFamily = Vazirmatn),
        titleSmall = t.titleSmall.copy(fontFamily = Vazirmatn),
        bodyLarge = t.bodyLarge.copy(fontFamily = Vazirmatn),
        bodyMedium = t.bodyMedium.copy(fontFamily = Vazirmatn),
        bodySmall = t.bodySmall.copy(fontFamily = Vazirmatn),
        labelLarge = t.labelLarge.copy(fontFamily = Vazirmatn),
        labelMedium = t.labelMedium.copy(fontFamily = Vazirmatn),
        labelSmall = t.labelSmall.copy(fontFamily = Vazirmatn),
    )
}

@Composable
fun ThemeMode.resolveDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun BazaaryarTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = mode.resolveDark()
    // Switch the shared palette before children read it (no extra frame in the old colours).
    Snapshot.withoutReadObservation { C.use(dark) }
    val p = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) {
        darkColorScheme(
            primary = p.lead, onPrimary = p.onLead, secondary = p.aqua, tertiary = p.ember,
            background = p.paper, onBackground = p.ink, surface = p.card, onSurface = p.ink,
            surfaceVariant = p.soft, onSurfaceVariant = p.muted, outline = p.faint, outlineVariant = p.line,
            secondaryContainer = p.soft, onSecondaryContainer = p.ink, error = p.crimson,
            surfaceContainer = p.card, surfaceContainerHigh = p.card, surfaceContainerLow = p.card,
        )
    } else {
        lightColorScheme(
            primary = p.lead, onPrimary = p.onLead, secondary = p.aqua, tertiary = p.ember,
            background = p.paper, onBackground = p.ink, surface = p.card, onSurface = p.ink,
            surfaceVariant = p.soft, onSurfaceVariant = p.muted, outline = p.faint, outlineVariant = p.line,
            secondaryContainer = p.soft, onSecondaryContainer = p.ink, error = p.crimson,
            surfaceContainer = p.card, surfaceContainerHigh = p.card, surfaceContainerLow = p.card,
        )
    }
    MaterialTheme(colorScheme = scheme, typography = AppType, content = content)
}
