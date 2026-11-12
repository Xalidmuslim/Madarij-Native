package ru.madarij.nativeapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Refined navigation-style bronze numerals, without medallions or plaques.
 * An offset dark imprint beneath a warm metallic gradient creates the same
 * restrained embossed depth as the bottom navigation's miniature icons.
 */
@Composable
private fun HeritageBronzeGlyph(
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
    isArrow: Boolean = false
) {
    val fontSize = when {
        isArrow -> 21.sp
        label.length > 1 -> 16.sp
        else -> 19.sp
    }
    val glyphStyle = MaterialTheme.typography.bodyMedium.copy(
        fontFamily = BookSerif,
        fontSize = fontSize,
        lineHeight = 23.sp,
        fontWeight = FontWeight.SemiBold,
        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
    )
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Text(
            label,
            modifier = Modifier.offset(x = .6.dp, y = 1.2.dp),
            style = glyphStyle,
            color = Color(0xFF4C2D1A),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            label,
            style = glyphStyle.copy(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFFE4BC76), Color(0xFFA96D31), Color(0xFF80502B))
                )
            ),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
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
            HeritageBronzeGlyph(number.toString(), size = 30.dp)
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
                Modifier.size(44.dp).clickable(onClick = onToggle),
                contentAlignment = Alignment.Center
            ) {
                HeritageBronzeGlyph(marker, size = 30.dp, isArrow = marker == "↳")
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
