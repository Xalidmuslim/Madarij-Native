package ru.madarij.nativeapp

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.sin

/** Natural, unhurried pen cadence for a Cyrillic quote card.
 * Real phrases take 5.5–12 seconds; the relative pace follows letter and punctuation weights.
 * Pure for regression checks, without a clock or device dependency.
 */
internal fun reedPenDurationMillis(charCount: Int): Int =
    (charCount.coerceAtLeast(0) * 58).coerceIn(5500, 12000)


/**
 * Models a reed nib moving across individual handwritten characters.
 * Whitespace advances quickly, letters draw at the normal pace and punctuation
 * allows a slight resting pause. The timeline is precomputed once per quote;
 * frame rendering uses a binary search without remeasuring any glyphs.
 */
internal class ReedPenCharacterTimeline(text: String) {
    private val cumulative = FloatArray(text.length + 1)
    val totalWeight: Float
    init {
        for (index in text.indices) {
            val weight = when (text[index]) {
                ' ', '\t' -> .22f
                '\n', '\r' -> .32f
                '.', '!', '?', '…', ';', ':', '—' -> 1.70f
                ',', '«', '»', '(', ')' -> 1.32f
                else -> 1.0f
            }
            cumulative[index + 1] = cumulative[index] + weight
        }
        totalWeight = cumulative.last()
    }

    fun positionAt(progress: Float): Float {
        if (cumulative.size <= 1) return 0f
        if (progress <= 0f) return 0f
        if (progress >= 1f) return (cumulative.size - 1).toFloat()
        val target = totalWeight * progress
        var low = 0
        var high = cumulative.lastIndex
        while (low < high) {
            val mid = (low + high + 1) ushr 1
            if (cumulative[mid] <= target) low = mid else high = mid - 1
        }
        val index = low.coerceAtMost(cumulative.lastIndex - 1)
        val weight = cumulative[index + 1] - cumulative[index]
        return index + ((target - cumulative[index]) / weight).coerceIn(0f, 1f)
    }
}

/**
 * The ink glyphs of Bad Script may extend below typography line metrics.
 * A reveal clipped exactly at getLineBottom() cuts Cyrillic descenders.
 * Keep a small ink-safe margin while staying within the text drawing area.
 */
internal fun reedPenVisibleLineBounds(
    lineTop: Float, lineBottom: Float,
    glyphTop: Float, glyphBottom: Float,
    overscan: Float, height: Float
): Pair<Float, Float> {
    val top = (minOf(lineTop, glyphTop) - overscan).coerceAtLeast(0f)
    val bottom = (maxOf(lineBottom, glyphBottom) + overscan).coerceAtMost(height)
    return top to bottom
}

/**
 * A compact, fully native reed-pen handwriting reveal.
 * All characters are measured in their FINAL positions. The ink mask follows
 * actual glyph bounding boxes and softly unveils each character at natural pace, while a small
 * drawn reed nib follows the text. No full-screen overlays, text reflow,
 * expensive blur, particles, or cached bitmap per frame.
 */
@Composable
internal fun ReedPenWritingText(
    text: String,
    style: TextStyle,
    color: Color,
    reducedMotion: Boolean,
    animate: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val active = animate && !reducedMotion
    val timeline = remember(text) { ReedPenCharacterTimeline(text) }
    val ink = remember(text) { Animatable(if (active) 0f else 1f) }
    val callback by rememberUpdatedState(onFinished)
    LaunchedEffect(text, active) {
        if (!active) ink.snapTo(1f)
        else {
            ink.snapTo(0f)
            ink.animateTo(
                1f, animationSpec = tween(
                    durationMillis = reedPenDurationMillis(text.length),
                    easing = LinearEasing
                )
            )
        }
        callback()
    }

    var measured by remember(text, style) { mutableStateOf<TextLayoutResult?>(null) }
    Box(modifier) {
        Text(
            text = text,
            color = color,
            style = style,
            modifier = Modifier.fillMaxWidth().drawWithContent {
                val amount = ink.value.coerceIn(0f, 1f)
                val layout = measured
                if (amount >= 0.9999f || text.isEmpty()) {
                    drawContent()
                } else if (layout != null && amount > 0f) {
                    // The first layout pass may not have bounds yet.
                    // Never flash a fully visible quotation before the pen starts.
                    val position = timeline.positionAt(amount)
                    val charIndex = floor(position).toInt().coerceIn(0, text.length - 1)
                    val fraction = (position - charIndex).coerceIn(0f, 1f)
                    val line = layout.getLineForOffset(charIndex)
                    val glyph = layout.getBoundingBox(charIndex)
                    val (lineTop, lineBottom) = reedPenVisibleLineBounds(
                        lineTop = layout.getLineTop(line).toFloat(),
                        lineBottom = layout.getLineBottom(line).toFloat(),
                        glyphTop = glyph.top,
                        glyphBottom = glyph.bottom,
                        overscan = 7.dp.toPx(),
                        height = size.height
                    )
                    val x = (glyph.left + glyph.width * fraction).coerceIn(0f, size.width)
                    val visibleInk = Path().apply {
                        if (lineTop > 0f) addRect(Rect(0f, 0f, size.width, lineTop))
                        if (x > 0f) addRect(Rect(0f, lineTop, x, lineBottom))
                    }
                    val contentScope = this
                    clipPath(visibleInk) { contentScope.drawContent() }
                }
            },
            onTextLayout = {
                if (measured?.size != it.size || measured?.lineCount != it.lineCount) {
                    measured = it
                }
            }
        )
        if (active) {
            Canvas(Modifier.matchParentSize()) {
                val progress = ink.value
                val layout = measured ?: return@Canvas
                if (text.isEmpty() || progress <= .006f || progress >= .998f) return@Canvas
                val cursor = timeline.positionAt(progress).coerceAtMost((text.length - 1).toFloat())
                val index = floor(cursor).toInt().coerceIn(0, text.lastIndex)
                val fraction = (cursor - index).coerceIn(0f, 1f)
                val glyph = layout.getBoundingBox(index)
                val tip = Offset(
                    (glyph.left + glyph.width * fraction).coerceIn(0f, size.width),
                    (glyph.bottom - 2.dp.toPx()).coerceIn(0f, size.height)
                )
                // Restrained diagonal reed shaft, with a flat-cut nib.
                val nibWobble = sin(cursor * .41f) * .7.dp.toPx()
                val handle = Offset(tip.x + 13.dp.toPx(), tip.y - 24.dp.toPx() + nibWobble)
                val reed = Color(0xFF74532F)
                val highlight = Color(0xFFC5A06A)
                val alpha = ((1f - progress) * 22f).coerceIn(0f, 1f)
                drawLine(reed, handle, tip, strokeWidth = 3.3.dp.toPx(),
                    cap = StrokeCap.Round, alpha = alpha)
                drawLine(highlight,
                    Offset(handle.x - 1.0.dp.toPx(), handle.y + 1.dp.toPx()),
                    Offset(tip.x - .4.dp.toPx(), tip.y),
                    strokeWidth = .8.dp.toPx(), alpha = alpha)
                val nib = Path().apply {
                    moveTo(tip.x - 1.8.dp.toPx(), tip.y - 2.dp.toPx())
                    lineTo(tip.x + 2.dp.toPx(), tip.y + .4.dp.toPx())
                    lineTo(tip.x - .7.dp.toPx(), tip.y + 1.5.dp.toPx())
                    close()
                }
                drawPath(nib, color = Color(0xFF3F2B1E), alpha = alpha)
            }
        }
    }
}
