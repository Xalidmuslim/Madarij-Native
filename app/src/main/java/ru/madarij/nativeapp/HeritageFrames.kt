package ru.madarij.nativeapp

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * One reusable engraved parchment edge for the application's interactive
 * surfaces. This is code-drawn vector detail, not a cropped reference image.
 * It reuses the existing static paper texture under the content.
 *
 * drawWithCache avoids allocating brushes and geometry for every animation
 * frame. The etching is an overlay and does not modify semantics, clipping,
 * touch targets, scroll state or reading fonts.
 */
internal object HeritageFrameTokens {
    val exterior = Color(0xFF805837)
    val interior = Color(0xFFE1BE8E)
    val highlight = Color(0xFFFFF3D9)
    val nightExterior = Color(0xFFAD885B)
    val nightInterior = Color(0xFF7D694E)
    val radius = 17.dp
    val fieldRadius = 14.dp
}

/** Invoke from a composable so night / parchment color changes are reflected. */
@Composable
internal fun Modifier.heritageEtching(
    cornerRadius: Dp = HeritageFrameTokens.radius,
    prominence: Float = 1f
): Modifier {
    val night = MaterialTheme.colorScheme.background == BookColors.nightBackground
    val outer = if (night) HeritageFrameTokens.nightExterior else HeritageFrameTokens.exterior
    val inner = if (night) HeritageFrameTokens.nightInterior else HeritageFrameTokens.interior
    val gloss = if (night) Color(0xFFAC9876) else HeritageFrameTokens.highlight
    val amount = prominence.coerceIn(0f, 1f)
    return this.drawWithCache {
        val w = size.width
        val h = size.height
        val one = 1.dp.toPx()
        val outerInset = 1.3f * one
        val innerInset = 3.8f * one
        val r = cornerRadius.toPx().coerceAtMost(minOf(w, h) / 2f)
        val outerBrush = Brush.verticalGradient(
            listOf(outer.copy(alpha = .53f * amount),
                inner.copy(alpha = .66f * amount),
                outer.copy(alpha = .42f * amount)),
            startY = 0f, endY = h.coerceAtLeast(1f)
        )
        onDrawWithContent {
            drawContent()
            if (w <= 11f * one || h <= 11f * one) return@onDrawWithContent
            drawRoundRect(
                brush = outerBrush,
                topLeft = Offset(outerInset, outerInset),
                size = Size(w - outerInset * 2f, h - outerInset * 2f),
                cornerRadius = CornerRadius((r - outerInset).coerceAtLeast(0f)),
                style = Stroke(width = .85f * one)
            )
            // Second hairline evokes an old printed border, not a modern
            // double-outline plastic button.
            drawRoundRect(
                color = gloss.copy(alpha = (if (night) .19f else .56f) * amount),
                topLeft = Offset(innerInset, innerInset),
                size = Size(w - innerInset * 2f, h - innerInset * 2f),
                cornerRadius = CornerRadius((r - innerInset).coerceAtLeast(0f)),
                style = Stroke(width = .55f * one)
            )
            // Few deterministic irregular engraver marks along the corners.
            // No noisy texture, shadow bitmap or randomness.
            val margin = (r + 5f * one).coerceAtMost(w * .37f)
            drawLine(
                color = outer.copy(alpha = .26f * amount),
                start = Offset(margin, 2.6f * one),
                end = Offset((margin + 7f * one).coerceAtMost(w - margin), 2.35f * one),
                strokeWidth = .55f * one
            )
            drawLine(
                color = inner.copy(alpha = .36f * amount),
                start = Offset((w - margin - 5f * one).coerceAtLeast(margin), h - 2.8f * one),
                end = Offset(w - margin, h - 2.55f * one),
                strokeWidth = .50f * one
            )
        }
    }
}
