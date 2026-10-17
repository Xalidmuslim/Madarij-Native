package ru.madarij.nativeapp

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Shared actual aged frame graphic: original transparent parchment ornament,
 * generated expressly for the app and bundled as an Android PNG resource.
 *
 * The translucent PNG contains only corners and edges; the center is transparent.
 * Drawn as nine patches so corner wear and hairline shapes are NEVER stretched.
 * Existing card_paper and other app images remain beneath the decoration.
 *
 * Draw-with-cache stores destination geometry and avoids frame-time image
 * allocations, animations, and bitmap-resampling across unrelated cards.
 */
internal object HeritageFrameTokens {
    val exterior = Color(0xFF805837)
    val interior = Color(0xFFE1BE8E)
    val highlight = Color(0xFFFFF3D9)
    val nightExterior = Color(0xFFAD885B)
    val nightInterior = Color(0xFF7D694E)
    val radius = 17.dp
    val fieldRadius = 14.dp
    const val cornerSourcePx = 24
}

/** Invoke from a Composable so current night / parchment tone is observed. */
@Composable
internal fun Modifier.heritageEtching(
    cornerRadius: Dp = HeritageFrameTokens.radius,
    prominence: Float = 1f
): Modifier {
    val night = MaterialTheme.colorScheme.background == BookColors.nightBackground
    // imageResource caches decoded bitmap by resource/density configuration.
    val frame = ImageBitmap.imageResource(R.drawable.heritage_frame_parchment)
    val nightOuter = HeritageFrameTokens.nightExterior
    val nightInner = HeritageFrameTokens.nightInterior
    val amount = prominence.coerceIn(0f, 1f)
    return this.drawWithCache {
        val width = size.width.roundToInt().coerceAtLeast(1)
        val height = size.height.roundToInt().coerceAtLeast(1)
        val sourceEdge = HeritageFrameTokens.cornerSourcePx
            .coerceAtMost(minOf(frame.width, frame.height) / 2 - 1)
        val targetEdge = (cornerRadius.toPx() + 4.dp.toPx()).roundToInt()
            .coerceAtLeast(5).coerceAtMost(minOf(width, height) / 2)
        val sX = intArrayOf(0, sourceEdge, frame.width - sourceEdge, frame.width)
        val sY = intArrayOf(0, sourceEdge, frame.height - sourceEdge, frame.height)
        val dX = intArrayOf(0, targetEdge, width - targetEdge, width)
        val dY = intArrayOf(0, targetEdge, height - targetEdge, height)
        val strokeWidth = .8.dp.toPx()
        val radius = cornerRadius.toPx().coerceAtMost(minOf(size.width,size.height) / 2f)
        val hairline = Brush.verticalGradient(
            listOf(nightOuter.copy(alpha = .58f * amount),
                   nightInner.copy(alpha = .71f * amount),
                   nightOuter.copy(alpha = .5f * amount)),
            0f, size.height.coerceAtLeast(1f)
        )
        onDrawWithContent {
            drawContent()
            if (width <= 10 || height <= 10 || amount <= 0f) return@onDrawWithContent
            if (!night) {
                for (y in 0..2) for (x in 0..2) {
                    if (x == 1 && y == 1) continue
                    val dw = dX[x + 1] - dX[x]
                    val dh = dY[y + 1] - dY[y]
                    if (dw > 0 && dh > 0) {
                        drawImage(
                            image = frame,
                            srcOffset = IntOffset(sX[x], sY[y]),
                            srcSize = IntSize(sX[x + 1] - sX[x], sY[y + 1] - sY[y]),
                            dstOffset = IntOffset(dX[x], dY[y]),
                            dstSize = IntSize(dw, dh),
                            alpha = .95f * amount,
                            filterQuality = FilterQuality.Low
                        )
                    }
                }
            } else {
                // Night theme retains subtle vector etching, not a dark brown
                // raster laid over near-black controls.
                drawRoundRect(
                    brush = hairline, topLeft = Offset(1.4.dp.toPx(), 1.4.dp.toPx()),
                    size = Size((size.width - 2.8.dp.toPx()).coerceAtLeast(0f),
                                (size.height - 2.8.dp.toPx()).coerceAtLeast(0f)),
                    cornerRadius = CornerRadius((radius - 1.4.dp.toPx()).coerceAtLeast(0f)),
                    style = Stroke(strokeWidth)
                )
                drawRoundRect(
                    color = HeritageFrameTokens.highlight.copy(alpha = .15f * amount),
                    topLeft = Offset(3.5.dp.toPx(), 3.5.dp.toPx()),
                    size = Size((size.width - 7.dp.toPx()).coerceAtLeast(0f),
                                (size.height - 7.dp.toPx()).coerceAtLeast(0f)),
                    cornerRadius = CornerRadius((radius - 3.5.dp.toPx()).coerceAtLeast(0f)),
                    style = Stroke(.55.dp.toPx())
                )
            }
        }
    }
}
