package ru.madarij.nativeapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val BookSerif = FontFamily(Font(R.font.russian_book))
internal fun chapterArt(ordinal: Int): Int = when ((ordinal - 1).mod(9)) {
    0 -> R.drawable.chapter_art_1
    1 -> R.drawable.chapter_art_2
    2 -> R.drawable.chapter_art_3
    3 -> R.drawable.chapter_art_4
    4 -> R.drawable.chapter_art_5
    5 -> R.drawable.chapter_art_6
    6 -> R.drawable.chapter_art_7
    7 -> R.drawable.chapter_art_8
    else -> R.drawable.chapter_art_9
}

@Composable internal fun SettingsWheel(modifier: Modifier) {
    Canvas(modifier) {
        val c = center; val r = size.minDimension * .28f
        drawCircle(BookColors.lightGold, r, style = Stroke(1.4.dp.toPx()))
        drawCircle(BookColors.lightGold, r * .38f, style = Stroke(1.4.dp.toPx()))
        repeat(8) { i ->
            val a = i * Math.PI / 4
            drawLine(BookColors.lightGold, Offset(c.x + kotlin.math.cos(a).toFloat()*r*1.2f,c.y + kotlin.math.sin(a).toFloat()*r*1.2f), Offset(c.x + kotlin.math.cos(a).toFloat()*r*1.65f,c.y + kotlin.math.sin(a).toFloat()*r*1.65f), 1.4.dp.toPx())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun BookTextSlider(value: Float, onValueChange: (Float)->Unit, onFinished: ()->Unit, modifier: Modifier = Modifier, range: ClosedFloatingPointRange<Float> = 14f..36f) {
    Slider(value = value, onValueChange = onValueChange, onValueChangeFinished = onFinished,
        valueRange = range, modifier = modifier,
        thumb = { Box(Modifier.size(18.dp).background(MaterialTheme.colorScheme.primary,CircleShape)) },
        track = {
            val trackTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .35f)
            val accentTint = MaterialTheme.colorScheme.primary
            Canvas(Modifier.fillMaxWidth().height(3.dp)) {
                drawRoundRect(trackTint,cornerRadius = CornerRadius(size.height))
                drawRoundRect(accentTint,size = Size(size.width*((value-range.start)/(range.endInclusive-range.start)).coerceIn(0f,1f),size.height),cornerRadius = CornerRadius(size.height))
            }
        })
}

@Composable internal fun ReaderFrontispiece(ordinal: Int, title: String, subtitle: String?, dark: Boolean) {
    Column(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 17.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.Start) {
        Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = .075f),
            border = androidx.compose.foundation.BorderStroke(.6.dp, MaterialTheme.colorScheme.primary.copy(alpha = .24f))) {
            Text("Раздел $ordinal", Modifier.padding(horizontal = 11.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(12.dp))
        Text(title, Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = BookSerif, fontSize = 30.sp, lineHeight = 35.sp),
            color = MaterialTheme.colorScheme.onSurface)
        if (!subtitle.isNullOrBlank()) Text(subtitle.replace(" · название для навигации", ""),
            Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = BookSerif),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = BookColors.gold.copy(alpha = .34f), thickness = .7.dp)
    }
}

@Composable internal fun ChapterTopicRow(title: String, onClick: () -> Unit) {
    Column {
        TextButton(onClick = onClick,modifier = Modifier.fillMaxWidth(),contentPadding = PaddingValues(horizontal = 8.dp,vertical = 12.dp)) {
            NavigationGlyph("notes",modifier = Modifier.size(22.dp),tint = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(14.dp))
            Text(title,Modifier.weight(1f),style = MaterialTheme.typography.bodyMedium,color = MaterialTheme.colorScheme.onSurface,textAlign = androidx.compose.ui.text.style.TextAlign.Start)
            Text("›",style = MaterialTheme.typography.titleMedium,color = BookColors.muted)
        }
        HorizontalDivider(color = BookColors.gold.copy(alpha = .12f))
    }
}

@Composable internal fun DarkSheetSystemBars() {
    val view = androidx.compose.ui.platform.LocalView.current
    val currentColors = MaterialTheme.colorScheme
    val lightSystemBars = currentColors.background != BookColors.nightBackground
    androidx.compose.runtime.SideEffect {
        view.post {
            var parent: android.view.ViewParent? = view as? android.view.ViewParent
            if (parent == null) parent = view.parent
            while (parent != null && parent !is androidx.compose.ui.window.DialogWindowProvider) parent = parent.parent
            (parent as? androidx.compose.ui.window.DialogWindowProvider)?.window?.let { window ->
                androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = lightSystemBars
                    isAppearanceLightNavigationBars = lightSystemBars
                }
                @Suppress("DEPRECATION")
                window.navigationBarColor = currentColors.background.toArgb()
                if(android.os.Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
            }
        }
    }
}
