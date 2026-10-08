package ru.madarij.nativeapp
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import ru.madarij.nativeapp.data.ReadingSettings

internal object BookColors {
    val background = Color(0xFFF2E5CC)
    val secondaryBackground = Color(0xFFEBDDCA)
    val card = Color(0xFFF9F0DF)
    val gold = Color(0xFF805B35)
    val lightGold = Color(0xFFE2BF89)
    val parchment = Color(0xFFF4E8D1)
    val ink = Color(0xFF372B21)
    val text = Color(0xFF372B21)
    val muted = Color(0xFF786953)
    val nightBackground = Color(0xFF11110D)
    val nightSecondaryBackground = Color(0xFF191814)
    val nightCard = Color(0xFF1D1B17)
    val nightText = Color(0xFFF2E8D8)
    val nightMuted = Color(0xFFA99F91)
}

internal fun bookDarkColors() = darkColorScheme(
    primary=BookColors.lightGold, onPrimary=BookColors.nightBackground,
    primaryContainer=Color(0xFF3C3020), onPrimaryContainer=BookColors.nightText,
    secondary=BookColors.lightGold, onSecondary=BookColors.nightBackground,
    secondaryContainer=Color(0xFF30291F), onSecondaryContainer=BookColors.nightText,
    tertiary=BookColors.lightGold, onTertiary=BookColors.ink,
    tertiaryContainer=Color(0xFF3C3020), onTertiaryContainer=BookColors.nightText,
    background=BookColors.nightBackground, onBackground=BookColors.nightText,
    surface=BookColors.nightCard, onSurface=BookColors.nightText,
    surfaceVariant=BookColors.nightSecondaryBackground, onSurfaceVariant=BookColors.nightMuted,
    surfaceContainer=BookColors.nightCard, surfaceContainerHigh=Color(0xFF25221C),
    surfaceContainerHighest=Color(0xFF302B23),
    outline=Color(0xFF68563E), outlineVariant=Color(0xFF3A3328)
)

@Suppress("UNUSED_PARAMETER")
internal fun bookReaderColors(settings: ReadingSettings, systemDark: Boolean): ColorScheme {
    if (settings.theme == "dark") return bookDarkColors()
    val paper = if (settings.theme == "light") Color(0xFFFAF6EF) else BookColors.parchment
    return lightColorScheme(
        primary=BookColors.gold, onPrimary=Color.White,
        primaryContainer=Color(0xFFEAD8B8), onPrimaryContainer=BookColors.ink,
        secondary=BookColors.gold, onSecondary=Color.White,
        secondaryContainer=Color(0xFFEDDFC6), onSecondaryContainer=BookColors.ink,
        tertiary=BookColors.gold, onTertiary=Color.White,
        tertiaryContainer=Color(0xFFEDDFC6), onTertiaryContainer=BookColors.ink,
        background=paper, onBackground=BookColors.ink,
        surface=paper, onSurface=BookColors.ink,
        surfaceVariant=Color(0xFFEFE0C4), onSurfaceVariant=BookColors.muted,
        surfaceContainer=paper, surfaceContainerHigh=Color(0xFFEFE0C4),
        surfaceContainerHighest=Color(0xFFEAD8B8),
        outline=Color(0xFFAF9774), outlineVariant=Color(0xFFD6C4A5)
    )
}
