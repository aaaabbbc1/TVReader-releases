package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.data.model.AstigmaPreset
import com.example.data.model.ReaderTheme

object TvColors {
    val Background = Color(0xFF121316)
    val Surface = Color(0xFF1A1C21)
    val SurfaceVariant = Color(0xFF22252C)
    val SurfaceContainer = Color(0xFF17191E)
    val BorderSubtle = Color(0xFF313540)

    val Primary = Color(0xFF0B4EA2)
    val PrimaryBright = Color(0xFF1967D2)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryTextAccent = Color(0xFF6FA8FF)

    val FocusBorder = Color(0xFF84B6FF)
    val FocusContainer = Color(0xFF1E3250)

    val ActiveChipContainer = Color(0xFF4C4763)
    val ActiveChipText = Color(0xFFECE7FA)

    val HeroStart = Color(0xFF0D3B76)
    val HeroMid = Color(0xFF112C53)
    val HeroEnd = Color(0xFF1A1E26)

    val TextPrimary = Color(0xFFEDEFF5)
    val TextSecondary = Color(0xFF9BA2B0)
    val TextMuted = Color(0xFF6B7280)

    val ErrorRed = Color(0xFFCF6679)
    val SuccessGreen = Color(0xFF4CAF88)
}

data class ReaderPalette(
    val background: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val quoteBackground: Color,
    val accent: Color
)

fun resolveReaderPalette(
    theme: ReaderTheme,
    astigmaPreset: AstigmaPreset
): ReaderPalette = when (theme) {
    ReaderTheme.LIGHT -> ReaderPalette(
        background = Color(0xFFFAF9F6),
        textPrimary = Color(0xFF1B1C1E),
        textSecondary = Color(0xFF62666E),
        divider = Color(0xFFE0DFDA),
        quoteBackground = Color(0xFFF0EFEA),
        accent = Color(0xFF0B4EA2)
    )
    ReaderTheme.DARK -> ReaderPalette(
        background = Color(0xFF141518),
        textPrimary = Color(0xFFDCDFE6),
        textSecondary = Color(0xFF8E95A3),
        divider = Color(0xFF292C33),
        quoteBackground = Color(0xFF1C1E24),
        accent = Color(0xFF6FA8FF)
    )
    ReaderTheme.SEPIA -> ReaderPalette(
        background = Color(0xFFF4ECD8),
        textPrimary = Color(0xFF3E3021),
        textSecondary = Color(0xFF7B6853),
        divider = Color(0xFFDECFAF),
        quoteBackground = Color(0xFFEBE0C6),
        accent = Color(0xFF8C531E)
    )
    ReaderTheme.AMOLED -> ReaderPalette(
        background = Color(0xFF000000),
        textPrimary = Color(0xFF9A9A9A),
        textSecondary = Color(0xFF565656),
        divider = Color(0xFF1C1C1C),
        quoteBackground = Color(0xFF0C0C0C),
        accent = Color(0xFF4D88DB)
    )
    ReaderTheme.ASTIGMA -> ReaderPalette(
        background = Color(astigmaPreset.backgroundHex),
        textPrimary = Color(astigmaPreset.textHex),
        textSecondary = Color(astigmaPreset.secondaryTextHex),
        divider = Color(astigmaPreset.secondaryTextHex).copy(alpha = 0.28f),
        quoteBackground = Color(astigmaPreset.textHex).copy(alpha = 0.06f),
        accent = Color(astigmaPreset.textHex)
    )
}
