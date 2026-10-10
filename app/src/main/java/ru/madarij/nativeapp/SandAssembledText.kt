package ru.madarij.nativeapp

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Native, composition-local reveal: glyphs settle into view in a soft wave of
 * parchment-colored dust. The complete sentence is always laid out at its final
 * size, avoiding text reflow, overlaid page surfaces and navigation stutter.
 */
@Composable
internal fun SandAssembledText(
    text: String,
    style: TextStyle,
    color: Color,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = remember(text) { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(text, reducedMotion) {
        if (reducedMotion) {
            progress.snapTo(1f)
        } else {
            progress.snapTo(0f)
            progress.animateTo(
                1f,
                tween(durationMillis = (1250 + text.length * 5).coerceAtMost(2450), easing = LinearEasing)
            )
        }
    }
    val position = progress.value * text.length
    val full = if (progress.value >= .999f) text.length else (position - 3f).toInt().coerceIn(0, text.length)
    val glyphString = remember(text, full, position, color) {
        buildAnnotatedString {
            append(text)
            if (full > 0) addStyle(SpanStyle(color = color), 0, full)
            var i = full
            while (i < text.length && i < position + 1f) {
                val opacity = ((position - i) / 3f).coerceIn(0f, 1f)
                addStyle(SpanStyle(color = color.copy(alpha = opacity)), i, i + 1)
                i++
            }
            if (i < text.length) addStyle(SpanStyle(color = color.copy(alpha = 0f)), i, text.length)
        }
    }
    val lastLayout = remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    Box(modifier) {
        Text(
            glyphString,
            modifier = Modifier.fillMaxWidth(),
            style = style,
            maxLines = 8,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (lastLayout.value == null) lastLayout.value = it }
        )
        val layout = lastLayout.value
        if (!reducedMotion && progress.value > .02f && progress.value < .985f && layout != null && text.isNotEmpty()) {
            Canvas(Modifier.matchParentSize()) {
                val index = floor(position).toInt().coerceIn(0, text.length - 1)
                val rect = layout.getBoundingBox(index)
                val grain = Color(0xFF9A7B50)
                // A few deterministic grains drift in from the left, then settle
                // into each glyph. This avoids a full-screen GPU particle layer.
                repeat(22) { particle ->
                    val v = particle.toFloat()
                    val drift = (17f + (particle % 6) * 4f).dp.toPx()
                    val x = rect.left - drift * (1f - ((position % 1f) * .7f)) +
                        sin(v * 2.78f + position * .35f) * 7.dp.toPx()
                    val y = rect.center.y + cos(v * 1.94f + position * .21f) * (rect.height * .78f)
                    if (x in 0f..size.width && y in 0f..size.height) {
                        drawCircle(
                            color = grain,
                            radius = (0.45f + (particle % 3) * .22f).dp.toPx(),
                            center = Offset(x, y),
                            alpha = .12f + (particle % 4) * .04f
                        )
                    }
                }
            }
        }
    }
}
