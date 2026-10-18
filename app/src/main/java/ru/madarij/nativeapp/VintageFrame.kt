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

/** Exactly seven user-provided image resources; no legacy strokes, borders or background textures. */
internal enum class VintageFrameKind(val drawableId: Int, val leftFraction: Float = .20f, val rightFraction: Float = .20f) {
    SEARCH(R.drawable.frame_search, .235f, .185f),
    LIGHT_COMPACT(R.drawable.frame_light_compact, .20f, .20f),
    LIGHT_WIDE(R.drawable.frame_light_wide, .19f, .19f),
    SECTION_NUMBERED(R.drawable.frame_section_numbered, .25f, .185f),
    FONT_SETTINGS(R.drawable.frame_font_settings, .12f, .12f),
    DARK_COMPACT(R.drawable.frame_dark_compact, .20f, .20f),
    DARK_WIDE(R.drawable.frame_dark_wide, .20f, .20f)
}

/**
 * Nine-slice image background drawn BEFORE live Compose content.
 * Left and right pictograms are wholly inside fixed end segments. Corner pixels
 * and built-in labels are not stretched by changes in the card's width or height.
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
        val srcT = (bitmap.height * .32f).roundToInt()
        val srcB = (bitmap.height * .32f).roundToInt()
        // Cap the fixed illustrated end segments so controls remain usable
        // even on smaller screens (and when system text scaling is increased).
        val fitScale = (h.toFloat() / bitmap.height).coerceAtLeast(.1f)
        val maxEnd = (w * .38f).roundToInt()
        val dstL = (srcL * fitScale).roundToInt().coerceIn(1,maxEnd.coerceAtLeast(1))
        val dstR = (srcR * fitScale).roundToInt().coerceIn(1,(w - dstL - 1).coerceAtLeast(1))
        val dstT = (h * .32f).roundToInt()
        val dstB = (h * .32f).roundToInt()
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
                    filterQuality = FilterQuality.Low
                )
            }
            drawContent()
        }
    }
}

/** The magnifier belongs to the actual search PNG; no duplicate leading icon. */
@Composable
internal fun VintageSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Найти главу или тему"
) {
    Box(modifier.heightIn(min = 56.dp).vintageFrame(VintageFrameKind.SEARCH),
        contentAlignment = Alignment.CenterStart) {
        Row(Modifier.fillMaxWidth().padding(start = 65.dp, end = 27.dp),
            verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = value, onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(color = if (MaterialTheme.colorScheme.background == BookColors.nightBackground) BookColors.nightText else BookColors.ink, fontSize = 16.sp,
                    fontWeight = FontWeight.Normal),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isBlank()) Text(placeholder, color = BookColors.muted,
                            maxLines=1, overflow=TextOverflow.Ellipsis, fontSize=15.sp)
                        inner()
                    }
                }
            )
            if(value.isNotBlank()) TextButton(onClick={ onValueChange("") },
                contentPadding=PaddingValues(0.dp),modifier=Modifier.width(32.dp)) {
                Text("×", fontSize = 19.sp,color=BookColors.gold)
            }
        }
    }
}
