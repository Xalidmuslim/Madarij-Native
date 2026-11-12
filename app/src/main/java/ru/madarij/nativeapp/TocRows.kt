package ru.madarij.nativeapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The common marker column keeps titles aligned for 1, 2 and 3 digit labels. */
private val TOC_MARKER_WIDTH = 44.dp
private const val TOC_ATLAS_TILE = 96

private fun bronzeGlyphIndex(symbol: Char): Int = when (symbol) {
    in '0'..'9' -> symbol - '0'
    '›' -> 10
    '⌄' -> 11
    '↳' -> 12
    else -> -1
}

/** Text-width compensation for a single graphic and compact multi-digit numbers. */
private fun glyphSlotWidth(count: Int): Dp = when {
    count <= 1 -> 21.dp
    count == 2 -> 18.dp
    else -> 12.dp
}

/** Single uniformly rendered 384px atlas; crop away transparent tile margins. */
@Composable
private fun HeritageBronzeGlyph(
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = TOC_MARKER_WIDTH,
    isArrow: Boolean = false
) {
    val glyphs = label.map(::bronzeGlyphIndex)
    if (glyphs.isEmpty() || glyphs.any { it < 0 }) {
        Text(label, modifier = modifier.size(size),
            fontFamily = BookSerif, fontSize = if (isArrow) 18.sp else 17.sp,
            color = Color(0xFF724B2A), textAlign = TextAlign.Center)
        return
    }
    val atlas = ImageBitmap.imageResource(R.drawable.toc_bronze_sprite)
    Canvas(modifier.size(size)) {
        val oneWidth = glyphSlotWidth(glyphs.size).toPx()
        val gap = if (glyphs.size > 1) .6.dp.toPx() else 0f
        val desired = glyphs.size * oneWidth + (glyphs.size - 1) * gap
        val fit = (this.size.width * .91f / desired).coerceAtMost(1f)
        val glyphWidth = (oneWidth * fit).roundToInt().coerceAtLeast(1)
        val glyphHeight = ((if (glyphs.size > 1) 25.dp else 27.dp).toPx() * fit)
            .roundToInt().coerceAtLeast(1)
        val spacing = (gap * fit).roundToInt()
        val rowWidth = glyphWidth * glyphs.size + spacing * (glyphs.size - 1)
        val firstX = ((this.size.width - rowWidth) / 2f).roundToInt()
        val firstY = ((this.size.height - glyphHeight) / 2f).roundToInt()
        glyphs.forEachIndexed { i, index ->
            drawImage(
                image = atlas,
                srcOffset = IntOffset((index % 4) * TOC_ATLAS_TILE + 16,
                    (index / 4) * TOC_ATLAS_TILE + 8),
                srcSize = IntSize(64, 80),
                dstOffset = IntOffset(firstX + i * (glyphWidth + spacing), firstY),
                dstSize = IntSize(glyphWidth, glyphHeight),
                filterQuality = FilterQuality.Medium
            )
        }
    }
}

/** The read action remains a 44dp touch target but has no decorative frame. */
@Composable
private fun TocReadButton(onClick: () -> Unit) {
    Box(
        Modifier.widthIn(min = 62.dp).heightIn(min = 44.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Читать" }
            .padding(horizontal = 5.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Читать",
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = BookSerif,
                fontSize = 13.sp
            ),
            color = Color(0xFF654326),
            maxLines = 1
        )
    }
}

/** Same section row for the whole four-volume corpus. */
@Composable
internal fun TocSectionRow(
    number: Int,
    title: String,
    meta: String,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth()
                .heightIn(min = CompactUi.contentsGroupMinHeightDp.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 9.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            HeritageBronzeGlyph(number.toString(), size = TOC_MARKER_WIDTH)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = BookSerif, fontSize = 17.sp, lineHeight = 21.sp
                    ),
                    color = heritageInk()
                )
                Text(meta, style = MaterialTheme.typography.labelSmall, color = heritageMuted())
            }
            HeritageBronzeGlyph(if (expanded) "⌄" else "›", size = 30.dp, isArrow = true)
        }
        HorizontalDivider(
            thickness = .7.dp,
            color = BookColors.gold.copy(alpha = .34f)
        )
    }
}

/** Identical chapter and continuation styling with independent expand/read actions. */
@Composable
internal fun TocChapterRow(
    marker: String,
    title: String,
    isContinuation: Boolean,
    topicCount: Int,
    expanded: Boolean,
    page: String?,
    percent: Int,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit,
    onRead: () -> Unit
) {
    Column(modifier.fillMaxWidth().padding(start = 12.dp)) {
        Row(
            Modifier.fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(start = 2.dp, end = 1.dp, top = 7.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                Modifier.size(TOC_MARKER_WIDTH).clickable(onClick = onToggle),
                contentAlignment = Alignment.Center
            ) {
                HeritageBronzeGlyph(marker, size = TOC_MARKER_WIDTH, isArrow = marker == "↳")
            }
            Column(
                Modifier.weight(1f).clickable(onClick = onToggle),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (isContinuation) Text(
                    "ПРОДОЛЖЕНИЕ ТЕКСТА",
                    style = MaterialTheme.typography.labelSmall,
                    color = heritageMuted()
                )
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = BookSerif,
                        fontSize = 15.sp,
                        lineHeight = 19.sp
                    ),
                    color = heritageInk()
                )
                if (topicCount > 0) Text(
                    (if (expanded) "⌄" else "›") + " Содержание · $topicCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = heritageMuted()
                )
            }
            TocReadButton(onClick = onRead)
        }
        page?.let {
            Text(
                it, modifier = Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 5.dp),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelSmall,
                color = heritageMuted()
            )
        }
        if (percent > 0) LinearProgressIndicator(
            progress = { percent / 100f },
            modifier = Modifier.fillMaxWidth().height(1.dp),
            color = BookColors.gold,
            trackColor = Color.Transparent
        )
        HorizontalDivider(
            modifier = Modifier.padding(start = 54.dp, top = 7.dp),
            thickness = .7.dp,
            color = BookColors.gold.copy(alpha = .28f)
        )
    }
}

/** Book topic stays frameless; chapters/anchors and printed page metadata unchanged. */
@Composable
internal fun TocTopicRow(
    label: String,
    title: String,
    page: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier.fillMaxWidth()
            .padding(start = 64.dp)
            .clickable(onClick = onClick)
            .padding(end = 11.dp, top = 9.dp, bottom = 7.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = heritageMuted())
        Text(title, style = MaterialTheme.typography.bodyMedium, color = heritageInk())
        page?.let {
            Text(
                it, modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelSmall,
                color = heritageMuted()
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = 4.dp),
            thickness = .7.dp,
            color = BookColors.gold.copy(alpha = .24f)
        )
    }
}
