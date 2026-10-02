package ir.bazaaryar.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.LayoutDirection

enum class Lang { FA, EN }

/**
 * Current app language. Snapshot state: every Composable that calls [tr] (directly or through enum labels)
 * recomposes when it changes, so switching language is instant and needs no activity restart.
 */
object L {
    var lang by mutableStateOf(Lang.FA)
    val en: Boolean get() = lang == Lang.EN
    val dir: LayoutDirection get() = if (en) LayoutDirection.Ltr else LayoutDirection.Rtl
}

/** Picks the Persian or English text for the current language. */
fun tr(fa: String, en: String): String = if (L.en) en else fa
