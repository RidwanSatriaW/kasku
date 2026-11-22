package id.kasku.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/** Satu set warna lengkap untuk satu palet dalam satu mode. */
class KPal(
    val bg: Color, val card: Color, val card2: Color, val line: Color,
    val text: Color, val muted: Color,
    val accent: Color, val onAccent: Color,
    val income: Color, val expense: Color, val warn: Color,
    val nav: Color,
    val g1: Color, val g2: Color, val g3: Color,
    val fabA: Color, val fabB: Color, val onFab: Color
)

private fun k(h: Long) = Color(0xFF000000 or h)

private val AuroraDark = KPal(k(0x0A0F1E), k(0x131B30), k(0x1B2644), k(0x26325A), k(0xEEF2FF), k(0x94A3C4), k(0x22D3EE), k(0x04211E), k(0x34D399), k(0xFB7185), k(0xFBBF24), k(0x0F1730), k(0x4F46E5), k(0x2563EB), k(0x06B6D4), k(0x6366F1), k(0x22D3EE), k(0xFFFFFF))
private val AuroraLight = KPal(k(0xF4F6FD), k(0xFFFFFF), k(0xEEF1FB), k(0xDDE3F3), k(0x0F172A), k(0x5B6B8C), k(0x4F46E5), k(0xFFFFFF), k(0x059669), k(0xE11D48), k(0xB45309), k(0xFFFFFF), k(0x4F46E5), k(0x2563EB), k(0x06B6D4), k(0x6366F1), k(0x22D3EE), k(0xFFFFFF))
private val ZamrudDark = KPal(k(0x07110E), k(0x0F1C17), k(0x15281F), k(0x1E3A30), k(0xECFDF5), k(0x8DB3A3), k(0x2DD4BF), k(0x04211E), k(0x86EFAC), k(0xFB7185), k(0xFBBF24), k(0x0B1713), k(0x047857), k(0x0D9488), k(0x2DD4BF), k(0x0D9488), k(0x2DD4BF), k(0x04211E))
private val ZamrudLight = KPal(k(0xF1F8F5), k(0xFFFFFF), k(0xE6F2EC), k(0xD3E6DC), k(0x0B1F18), k(0x4F6F62), k(0x0D9488), k(0xFFFFFF), k(0x15803D), k(0xE11D48), k(0xB45309), k(0xFFFFFF), k(0x047857), k(0x0D9488), k(0x2DD4BF), k(0x0D9488), k(0x2DD4BF), k(0xFFFFFF))
private val SenjaDark = KPal(k(0x140E14), k(0x1F1620), k(0x2A1E2C), k(0x3A2A3C), k(0xFFF1F5), k(0xBDA3B6), k(0xFDBA74), k(0x2A1000), k(0x4ADE80), k(0xFF6B6B), k(0xFACC15), k(0x190F19), k(0xEA580C), k(0xDB2777), k(0x9333EA), k(0xEA580C), k(0xDB2777), k(0xFFFFFF))
private val SenjaLight = KPal(k(0xFFF7F5), k(0xFFFFFF), k(0xFDECEF), k(0xF3D9DF), k(0x2A1420), k(0x8A6275), k(0xDB2777), k(0xFFFFFF), k(0x16A34A), k(0xDC2626), k(0xB45309), k(0xFFFFFF), k(0xEA580C), k(0xDB2777), k(0x9333EA), k(0xEA580C), k(0xDB2777), k(0xFFFFFF))

val PALETTES = listOf("aurora" to "Aurora", "zamrud" to "Zamrud", "senja" to "Senja")

fun palette(name: String, dark: Boolean): KPal = when (name) {
    "zamrud" -> if (dark) ZamrudDark else ZamrudLight
    "senja" -> if (dark) SenjaDark else SenjaLight
    else -> if (dark) AuroraDark else AuroraLight
}

val LocalPal = compositionLocalOf { AuroraDark }
val LocalDark = compositionLocalOf { true }

@Composable
fun KaskuTheme(name: String, dark: Boolean, content: @Composable () -> Unit) {
    val p = palette(name, dark)
    val scheme = if (dark) darkColorScheme(
        primary = p.accent, onPrimary = p.onAccent, primaryContainer = p.card2, onPrimaryContainer = p.text,
        secondaryContainer = p.card2, onSecondaryContainer = p.text,
        background = p.bg, onBackground = p.text, surface = p.card, onSurface = p.text,
        surfaceVariant = p.card2, onSurfaceVariant = p.muted, outline = p.line, outlineVariant = p.line, error = p.expense,
        surfaceContainerLowest = p.bg, surfaceContainerLow = p.card, surfaceContainer = p.card,
        surfaceContainerHigh = p.card2, surfaceContainerHighest = p.card2
    ) else lightColorScheme(
        primary = p.accent, onPrimary = p.onAccent, primaryContainer = p.card2, onPrimaryContainer = p.text,
        secondaryContainer = p.card2, onSecondaryContainer = p.text,
        background = p.bg, onBackground = p.text, surface = p.card, onSurface = p.text,
        surfaceVariant = p.card2, onSurfaceVariant = p.muted, outline = p.line, outlineVariant = p.line, error = p.expense,
        surfaceContainerLowest = p.bg, surfaceContainerLow = p.card, surfaceContainer = p.card,
        surfaceContainerHigh = p.card2, surfaceContainerHighest = p.card2
    )
    CompositionLocalProvider(LocalPal provides p, LocalDark provides dark) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

@Composable
fun incomeColor(): Color = LocalPal.current.income

val CAT_COLORS = listOf(
    Color(0xFFF97316), Color(0xFF0EA5E9), Color(0xFF8B5CF6), Color(0xFFEC4899),
    Color(0xFF14B8A6), Color(0xFF84CC16), Color(0xFFF59E0B), Color(0xFF64748B)
)

fun catColor(cat: String, income: Boolean = false): Color {
    val i = if (income) listOf("Gaji", "Bonus", "Usaha").indexOf(cat).let { if (it < 0) -1 else it + 4 }
    else OUT_CATS.indexOf(cat)
    return CAT_COLORS[if (i < 0) 7 else i]
}

fun goalColor(i: Int): Color = CAT_COLORS[i.coerceIn(0, 7)]
