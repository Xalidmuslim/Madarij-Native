package ru.madarij.nativeapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/** Seven generated soft-paper alpha textures; live text and icons stay native Compose elements. */
internal enum class VintageFrameKind(val drawableId: Int, val leftFraction: Float = .20f, val rightFraction: Float = .20f) {
    SEARCH(R.drawable.frame_search, .16f, .16f),
    LIGHT_COMPACT(R.drawable.frame_light_compact, .16f, .16f),
    LIGHT_WIDE(R.drawable.frame_light_wide, .16f, .16f),
    SECTION_NUMBERED(R.drawable.frame_section_numbered, .16f, .16f),
    FONT_SETTINGS(R.drawable.frame_font_settings, .16f, .16f),
    DARK_COMPACT(R.drawable.frame_dark_compact, .16f, .16f),
    DARK_WIDE(R.drawable.frame_dark_wide, .16f, .16f)
}

/**
 * Nine-slice image background drawn BEFORE live Compose content.
 * Light cards have no Material/Card clipping layer, so torn alpha edges survive.
 * The new artwork contains NO pictograms: only softly deckled paper texture.
 * Native chapter markers, search icon and chevrons never distort with resizing.
 *
 * The PNG/WebP is the only background; callers must not stack card_paper,
 * .border(), gradient, second chevrons or duplicated search/Aa icons over it.
 */
@Composable
internal fun Modifier.vintageFrame(kind: VintageFrameKind): Modifier {
    val bitmap = ImageBitmap.imageResource(kind.drawableId)
    val night = MaterialTheme.colorScheme.background == BookColors.nightBackground
    val nightColor = MaterialTheme.colorScheme.surface
    return this.drawWithCache {
        val w = size.width.roundToInt().coerceAtLeast(1)
        val h = size.height.roundToInt().coerceAtLeast(1)
        val srcL = (bitmap.width * kind.leftFraction).roundToInt()
        val srcR = (bitmap.width * kind.rightFraction).roundToInt()
        val srcT = (bitmap.height * .28f).roundToInt()
        val srcB = (bitmap.height * .28f).roundToInt()
        // Cap the fixed illustrated end segments so controls remain usable
        // even on smaller screens (and when system text scaling is increased).
        val fitScale = minOf(
            h.toFloat() / bitmap.height,
            w.toFloat() / bitmap.width
        ).coerceAtLeast(.01f)
        // Never allow large cards/long text to inflate ornate corners.
        // The numbered plaque and search icon need deliberately wider fixed ends.
        val maxEndDp = when(kind) {
            VintageFrameKind.SECTION_NUMBERED -> 30.dp
            VintageFrameKind.SEARCH -> 30.dp
            VintageFrameKind.FONT_SETTINGS -> 26.dp
            else -> 30.dp
        }
        val maxEnd = minOf((w * .32f).roundToInt(), maxEndDp.toPx().roundToInt())
        val dstL = (srcL * fitScale).roundToInt().coerceIn(1,maxEnd.coerceAtLeast(1))
        val dstR = (srcR * fitScale).roundToInt().coerceIn(1,minOf(maxEnd,(w - dstL - 1)).coerceAtLeast(1))
        // Preserve paper fibers at the top/bottom without enlarging the worn rim
        // when content makes the card taller. The middle alone absorbs growth.
        val maxVerticalEdge = minOf((h * .34f).roundToInt(), 21.dp.toPx().roundToInt())
        val dstT = (srcT * fitScale).roundToInt().coerceIn(1, maxVerticalEdge.coerceAtLeast(1))
        val dstB = (srcB * fitScale).roundToInt().coerceIn(1, minOf(maxVerticalEdge, h - dstT - 1).coerceAtLeast(1))
        val sx = intArrayOf(0,srcL,bitmap.width-srcR,bitmap.width)
        val sy = intArrayOf(0,srcT,bitmap.height-srcB,bitmap.height)
        val dx = intArrayOf(0,dstL,w-dstR,w)
        val dy = intArrayOf(0,dstT,h-dstB,h)
        onDrawWithContent {
            if (night) {
                // Keep existing readable night typography; antiqued images are day surfaces.
                drawRect(nightColor)
                drawContent()
                return@onDrawWithContent
            }
            for (iy in 0..2) for (ix in 0..2) {
                val sw=sx[ix+1]-sx[ix]; val sh=sy[iy+1]-sy[iy]
                val dw=dx[ix+1]-dx[ix]; val dh=dy[iy+1]-dy[iy]
                if (sw>0 && sh>0 && dw>0 && dh>0) drawImage(
                    bitmap,
                    srcOffset = IntOffset(sx[ix],sy[iy]),
                    srcSize = IntSize(sw,sh),
                    dstOffset = IntOffset(dx[ix],dy[iy]),
                    dstSize = IntSize(dw,dh),
                    filterQuality = FilterQuality.Medium
                )
            }
            drawContent()
        }
    }
}

/** The magnifier belongs to the actual search PNG; no duplicate leading icon. */
/** Search icon and input are native, not embedded inside the bitmap. */
@Composable
internal fun VintageSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Найти главу или тему"
) {
    Box(modifier.heightIn(min = 51.dp).vintageFrame(VintageFrameKind.SEARCH),
        contentAlignment = Alignment.CenterStart) {
        Row(Modifier.fillMaxWidth().padding(start = 17.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.Canvas(Modifier.size(22.dp)) {
                val ink = Color(0xFF876341)
                val center = Offset(size.width * .41f, size.height * .40f)
                drawCircle(ink, radius = size.minDimension * .29f, center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(1.7.dp.toPx()))
                drawLine(ink, Offset(size.width * .61f, size.height * .63f),
                    Offset(size.width * .90f, size.height * .91f),
                    strokeWidth = 1.8.dp.toPx())
            }
            Spacer(Modifier.width(13.dp))
            BasicTextField(
                value = value, onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(
                    color = if (MaterialTheme.colorScheme.background == BookColors.nightBackground)
                        BookColors.nightText else BookColors.ink,
                    fontSize = 16.sp, fontWeight = FontWeight.Normal
                ),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isBlank()) Text(placeholder, color = BookColors.muted,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 15.sp)
                        inner()
                    }
                }
            )
            if (value.isNotBlank()) TextButton(onClick = { onValueChange("") },
                contentPadding = PaddingValues(0.dp), modifier = Modifier.width(32.dp)) {
                Text("×", fontSize = 19.sp, color = BookColors.gold)
            }
        }
    }
}
