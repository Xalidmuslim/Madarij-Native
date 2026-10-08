package ru.madarij.nativeapp

import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import ru.madarij.nativeapp.data.ReadingSettings

internal object BookColors {
    val background = Color(0xFF11110D)
    val secondaryBackground = Color(0xFF191814)
    val card = Color(0xFF1D1B17)
    val gold = Color(0xFFD2A15D)
    val lightGold = Color(0xFFE6C184)
    val parchment = Color(0xFFF6EBD4)
    val ink = Color(0xFF30271E)
    val text = Color(0xFFF2E8D8)
    val muted = Color(0xFFA99F91)
}

internal fun bookDarkColors() = darkColorScheme(
    primary = BookColors.gold, onPrimary = BookColors.background,
    primaryContainer = Color(0xFF3C3020), onPrimaryContainer = BookColors.text,
    secondary = BookColors.lightGold, onSecondary = BookColors.background,
    secondaryContainer = Color(0xFF30291F), onSecondaryContainer = BookColors.text,
    tertiary = BookColors.lightGold, onTertiary = BookColors.ink,
    tertiaryContainer = Color(0xFF3C3020), onTertiaryContainer = BookColors.text,
    background = BookColors.background, onBackground = BookColors.text,
    surface = BookColors.card, onSurface = BookColors.text,
    surfaceVariant = BookColors.secondaryBackground, onSurfaceVariant = BookColors.muted,
    surfaceContainer = BookColors.card, surfaceContainerHigh = Color(0xFF25221C),
    surfaceContainerHighest = Color(0xFF302B23),
    outline = Color(0xFF68563E), outlineVariant = Color(0xFF3A3328)
)

internal fun bookReaderColors(settings: ReadingSettings, systemDark: Boolean): ColorScheme {
    if (settings.theme == "dark" || settings.theme == "system" && systemDark) return bookDarkColors()
    val paper = if (settings.theme == "light") Color(0xFFFAF6EF) else BookColors.parchment
    return lightColorScheme(
        primary = Color(0xFF785323), onPrimary = Color.White,
        primaryContainer = Color(0xFFEAD8B8), onPrimaryContainer = BookColors.ink,
        secondary = Color(0xFF785323), onSecondary = Color.White,
        secondaryContainer = Color(0xFFEDDFC6), onSecondaryContainer = BookColors.ink,
        tertiary = BookColors.lightGold, onTertiary = BookColors.ink,
        tertiaryContainer = Color(0xFFEDDFC6), onTertiaryContainer = BookColors.ink,
        background = paper, onBackground = BookColors.ink,
        surface = paper, onSurface = BookColors.ink,
        surfaceVariant = Color(0xFFEFE0C4), onSurfaceVariant = Color(0xFF74624D),
        surfaceContainer = paper, surfaceContainerHigh = Color(0xFFEFE0C4),
        surfaceContainerHighest = Color(0xFFEAD8B8),
        outline = Color(0xFFAF9774), outlineVariant = Color(0xFFD6C4A5)
    )
}
