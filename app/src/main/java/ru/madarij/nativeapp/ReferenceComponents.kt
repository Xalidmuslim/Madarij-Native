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
            Canvas(Modifier.fillMaxWidth().height(3.dp)) {
                drawRoundRect(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .35f),cornerRadius = CornerRadius(size.height))
                drawRoundRect(MaterialTheme.colorScheme.primary,size = Size(size.width*((value-range.start)/(range.endInclusive-range.start)).coerceIn(0f,1f),size.height),cornerRadius = CornerRadius(size.height))
            }
        })
}

@Composable internal fun ReaderFrontispiece(ordinal: Int, title: String, subtitle: String?, dark: Boolean) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().aspectRatio(2f)) {
            Image(painterResource(R.drawable.reference_chapter), null, Modifier.matchParentSize(),contentScale = ContentScale.Crop, alignment = Alignment.TopCenter, alpha = if(dark) .4f else 1f)
            Surface(color = Color(0xFF9D713E), contentColor = BookColors.parchment, shape = CircleShape, border = androidx.compose.foundation.BorderStroke(1.dp,BookColors.lightGold), modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp).size(46.dp)) {
                Box(contentAlignment = Alignment.Center) { Text("$ordinal",style = MaterialTheme.typography.titleLarge.copy(fontFamily = BookSerif)) }
            }
        }
        Text(title, Modifier.fillMaxWidth().padding(horizontal = 24.dp),style = MaterialTheme.typography.headlineMedium.copy(fontFamily = BookSerif,fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,fontSize = 28.sp,lineHeight = 34.sp),textAlign = androidx.compose.ui.text.style.TextAlign.Center,color = MaterialTheme.colorScheme.onSurface)
        if(!subtitle.isNullOrBlank()) Text(subtitle.replace(" · название для навигации",""),Modifier.padding(horizontal = 24.dp,vertical = 10.dp),style = MaterialTheme.typography.bodySmall.copy(fontFamily = BookSerif),textAlign = androidx.compose.ui.text.style.TextAlign.Center,color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
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
