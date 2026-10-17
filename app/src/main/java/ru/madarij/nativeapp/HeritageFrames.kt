package ru.madarij.nativeapp

import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** The complete, individually generated collection. */
internal enum class HeritageFrameKind {
    TAB_ACTIVE, TAB_INACTIVE, SEARCH, FILTER_ACTIVE, FILTER_INACTIVE,
    SECTION, SUBSECTION, SMALL_BUTTON
}

internal enum class VintageAssetIconKind {
    SEARCH, ALL, READ, BOOKMARK, ARROW_RIGHT, EXPAND, SETTINGS
}

internal object HeritageFrameTokens {
    val exterior = Color(0xFF805837)
    val interior = Color(0xFFE1BE8E)
    val highlight = Color(0xFFFFF3D9)
    val nightExterior = Color(0xFFAD885B)
    val nightInterior = Color(0xFF7D694E)
    val radius = 17.dp
    val fieldRadius = 14.dp
}

@Composable
internal fun VintageAssetIcon(kind: VintageAssetIconKind, modifier: Modifier = Modifier) {
    val resource = when(kind) {
        VintageAssetIconKind.SEARCH -> R.drawable.vintage_icon_search
        VintageAssetIconKind.ALL -> R.drawable.vintage_icon_all
        VintageAssetIconKind.READ -> R.drawable.vintage_icon_read
        VintageAssetIconKind.BOOKMARK -> R.drawable.vintage_icon_bookmark
        VintageAssetIconKind.ARROW_RIGHT -> R.drawable.vintage_icon_arrow_right
        VintageAssetIconKind.EXPAND -> R.drawable.vintage_icon_expand
        VintageAssetIconKind.SETTINGS -> R.drawable.vintage_icon_settings
    }
    Image(
        painter = painterResource(resource),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}

/**
 * Real bitmap artwork with eight separately authored frames and transparent
 * centers. The rectangle's center is NEVER drawn; native text and click
 * semantics remain real Compose elements. Corners are not stretched.
 *
 * This modifier is shared with card, button, field, dialog and sheet surfaces.
 */
@Composable
internal fun Modifier.heritageEtching(
    cornerRadius: Dp = HeritageFrameTokens.radius,
    prominence: Float = 1f,
    kind: HeritageFrameKind = HeritageFrameKind.SECTION
): Modifier {
    val resource = when (kind) {
        HeritageFrameKind.TAB_ACTIVE -> R.drawable.vintage_tab_active
        HeritageFrameKind.TAB_INACTIVE -> R.drawable.vintage_tab_inactive
        HeritageFrameKind.SEARCH -> R.drawable.vintage_search_frame
        HeritageFrameKind.FILTER_ACTIVE -> R.drawable.vintage_filter_active
        HeritageFrameKind.FILTER_INACTIVE -> R.drawable.vintage_filter_inactive
        HeritageFrameKind.SECTION -> R.drawable.vintage_section_card
        HeritageFrameKind.SUBSECTION -> R.drawable.vintage_subsection_card
        HeritageFrameKind.SMALL_BUTTON -> R.drawable.vintage_top_small_button
    }
    val frame = ImageBitmap.imageResource(resource)
    val night = MaterialTheme.colorScheme.background == BookColors.nightBackground
    // Heavy generated frames must not overpower short book labels.
    // Volume selectors remain more pronounced than chapter or menu cards.
    val visualWeight = when (kind) {
        HeritageFrameKind.SECTION -> .82f
        HeritageFrameKind.SUBSECTION -> .78f
        HeritageFrameKind.SEARCH -> .84f
        HeritageFrameKind.SMALL_BUTTON -> .82f
        else -> .92f
    }
    val amount = prominence.coerceIn(0f, 1f) * visualWeight
    return drawWithCache {
        val w = size.width.roundToInt()
        val h = size.height.roundToInt()
        val compact = minOf(w,h)
        val radius = cornerRadius.toPx().coerceAtMost(size.minDimension/2f)
        val targetX = (radius + 3.dp.toPx()).roundToInt()
            .coerceAtLeast(1).coerceAtMost(minOf(w / 2, 18.dp.toPx().roundToInt()))
        val targetY = (radius + 3.dp.toPx()).roundToInt()
            .coerceAtLeast(1).coerceAtMost(minOf(h / 2, 15.dp.toPx().roundToInt()))
        val sourceX = (frame.width * .29f).roundToInt().coerceIn(1,frame.width/2-1)
        val sourceY = (frame.height * .32f).roundToInt().coerceIn(1,frame.height/2-1)
        val srcX = intArrayOf(0,sourceX,frame.width-sourceX,frame.width)
        val srcY = intArrayOf(0,sourceY,frame.height-sourceY,frame.height)
        val dstX = intArrayOf(0,targetX,w-targetX,w)
        val dstY = intArrayOf(0,targetY,h-targetY,h)
        val nightBrush = Brush.verticalGradient(
            listOf(HeritageFrameTokens.nightExterior.copy(alpha = .62f * amount),
                HeritageFrameTokens.nightInterior.copy(alpha = .56f * amount),
                HeritageFrameTokens.nightExterior.copy(alpha = .48f * amount)),
            0f, size.height.coerceAtLeast(1f)
        )
        onDrawWithContent {
            drawContent()
            if (compact < 12 || amount <= 0f) return@onDrawWithContent
            if (night) {
                drawRoundRect(
                    brush=nightBrush,
                    topLeft=Offset(1.3.dp.toPx(),1.3.dp.toPx()),
                    size=Size((size.width-2.6.dp.toPx()).coerceAtLeast(0f),
                              (size.height-2.6.dp.toPx()).coerceAtLeast(0f)),
                    cornerRadius=CornerRadius((radius-1.3.dp.toPx()).coerceAtLeast(0f)),
                    style=Stroke(.9.dp.toPx())
                )
            }
            // Keep the individually aged corners in night mode as well.
            // A reduced alpha preserves readability against the dark paper.
            for (row in 0..2) for (col in 0..2) {
                    if (col==1 && row==1) continue
                    val dw=dstX[col+1]-dstX[col]
                    val dh=dstY[row+1]-dstY[row]
                    val sw=srcX[col+1]-srcX[col]
                    val sh=srcY[row+1]-srcY[row]
                    if(dw>0 && dh>0 && sw>0 && sh>0) {
                        drawImage(
                            image=frame,
                            srcOffset=IntOffset(srcX[col],srcY[row]),
                            srcSize=IntSize(sw,sh),
                            dstOffset=IntOffset(dstX[col],dstY[row]),
                            dstSize=IntSize(dw,dh),
                            alpha=if (night) amount * .36f else amount,
                            filterQuality=FilterQuality.Low
                        )
                    }
                }
        }
    }
}
