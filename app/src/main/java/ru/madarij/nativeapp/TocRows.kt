package ru.madarij.nativeapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A small manuscript cartouche, not a circular medallion.
 * Geometry is intentionally native and reusable for any chapter number,
 * including two-digit numbers and the continuation arrow.
 */
private fun bookplatePath(width: Float, height: Float, inset: Float): Path {
    val w = (width - inset * 2f).coerceAtLeast(1f)
    val h = (height - inset * 2f).coerceAtLeast(1f)
    fun x(v: Float) = inset + w * v
    fun y(v: Float) = inset + h * v

    return Path().apply {
        moveTo(x(.19f), y(.09f))
        lineTo(x(.38f), y(.09f))
        quadraticBezierTo(x(.50f), y(.005f), x(.62f), y(.09f))
        lineTo(x(.81f), y(.09f))
        quadraticBezierTo(x(.86f), y(.09f), x(.86f), y(.15f))
        lineTo(x(.86f), y(.22f))
        quadraticBezierTo(x(.97f), y(.32f), x(.97f), y(.39f))
        lineTo(x(.97f), y(.61f))
        quadraticBezierTo(x(.97f), y(.68f), x(.86f), y(.78f))
        lineTo(x(.86f), y(.85f))
        quadraticBezierTo(x(.86f), y(.91f), x(.81f), y(.91f))
        lineTo(x(.62f), y(.91f))
        quadraticBezierTo(x(.50f), y(.995f), x(.38f), y(.91f))
        lineTo(x(.19f), y(.91f))
        quadraticBezierTo(x(.14f), y(.91f), x(.14f), y(.85f))
        lineTo(x(.14f), y(.78f))
        quadraticBezierTo(x(.03f), y(.68f), x(.03f), y(.61f))
        lineTo(x(.03f), y(.39f))
        quadraticBezierTo(x(.03f), y(.32f), x(.14f), y(.22f))
        lineTo(x(.14f), y(.15f))
        quadraticBezierTo(x(.14f), y(.09f), x(.19f), y(.09f))
        close()
    }
}

@Composable
private fun HeritageBookplateBadge(
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 35.dp,
    isArrow: Boolean = false
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            // Matte aged parchment, restrained metallic line, and a tiny inner etching.
            // No repeated bitmap decoding, round backgrounds, gradients on the whole row,
            // or separate geometry for the different volumes.
            val outer = bookplatePath(this.size.width, this.size.height, 1.dp.toPx())
            val inner = bookplatePath(this.size.width, this.size.height, 3.1.dp.toPx())
            drawPath(
                outer,
                brush = Brush.verticalGradient(
                    listOf(Color(0xFFFFE8C2), Color(0xFFE9CCA1), Color(0xFFDDB98B))
                )
            )
            drawPath(outer, color = Color(0xFF926B43), style = Stroke(width = 1.dp.toPx()))
            drawPath(
                inner,
                color = Color(0xFFBF9258).copy(alpha = .8f),
                style = Stroke(width = .55.dp.toPx())
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = BookSerif,
                fontSize = if (isArrow) 18.sp else if (label.length > 2) 14.sp else 17.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                    includeFontPadding = false
                )
            ),
            color = Color(0xFF4C301F),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/** Small parchment read action – distinct from navigation, with at least 44dp hit height. */
@Composable
private fun TocReadButton(onClick: () -> Unit) {
    val corners = RoundedCornerShape(7.dp)
    Box(
        Modifier.widthIn(min = 78.dp).heightIn(min = 44.dp)
            .clip(corners)
            .background(
                Brush.verticalGradient(listOf(Color(0xFFFBE9CA), Color(0xFFEBD0A9))),
                corners
            )
            .border(.75.dp, Color(0xFFC19B67).copy(alpha = .75f), corners)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Читать" }
            .padding(horizontal = 9.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Читать",
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = BookSerif, fontSize = 13.sp
            ),
            color = Color(0xFF573B27),
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
            HeritageBookplateBadge(number.toString(), size = 36.dp)
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
            HeritageBookplateBadge(if (expanded) "⌄" else "›", size = 30.dp, isArrow = true)
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
                Modifier.size(44.dp).clickable(onClick = onToggle),
                contentAlignment = Alignment.Center
            ) {
                HeritageBookplateBadge(marker, size = 35.dp, isArrow = marker == "↳")
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
