package ru.madarij.nativeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A single matte-bronze treatment for numbered sections, chapters and arrows. */
@Composable
private fun HeritageCircleBadge(label: String, modifier: Modifier = Modifier,
                                size: Dp = 34.dp, isArrow: Boolean = false) {
    Box(
        modifier.size(size).clip(CircleShape)
            .background(Brush.verticalGradient(listOf(
                Color(0xFFB48750), Color(0xFF9C713E), Color(0xFF805832)
            )))
            .border(.8.dp, Color(0xFFD8B786).copy(alpha = .58f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = BookSerif,
                fontSize = if (isArrow) 19.sp else 16.sp,
                lineHeight = if (isArrow) 20.sp else 18.sp,
                fontWeight = FontWeight.Medium,
                platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
            ),
            color = Color(0xFFFFF2DB), textAlign = TextAlign.Center, maxLines = 1
        )
    }
}

/** Same section row for each of the four volumes. */
@Composable
internal fun TocSectionRow(number: Int, title: String, meta: String,
                           expanded: Boolean, modifier: Modifier = Modifier,
                           onClick: () -> Unit) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = CompactUi.contentsGroupMinHeightDp.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 11.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            HeritageCircleBadge(number.toString())
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = BookSerif, fontSize = 17.sp, lineHeight = 21.sp
                    ),
                    color = heritageInk())
                Text(meta, style = MaterialTheme.typography.labelSmall, color = heritageMuted())
            }
            HeritageCircleBadge(if (expanded) "⌄" else "›", isArrow = true)
        }
        HorizontalDivider(thickness = .7.dp, color = BookColors.gold.copy(alpha = .34f))
    }
}

/** Same chapter/continuation styling; keeps independent expand and read actions. */
@Composable
internal fun TocChapterRow(marker: String, title: String, isContinuation: Boolean,
                           topicCount: Int, expanded: Boolean, page: String?, percent: Int,
                           modifier: Modifier = Modifier, onToggle: () -> Unit,
                           onRead: () -> Unit) {
    Column(modifier.fillMaxWidth().padding(start = 12.dp)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp)
                .padding(start = 6.dp, end = 0.dp, top = 7.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(Modifier.size(44.dp).clickable(onClick = onToggle),
                contentAlignment = Alignment.Center) {
                HeritageCircleBadge(marker, isArrow = marker == "↳")
            }
            Column(Modifier.weight(1f).clickable(onClick = onToggle),
                verticalArrangement = Arrangement.spacedBy(3.dp)) {
                if (isContinuation) Text("ПРОДОЛЖЕНИЕ ТЕКСТА",
                    style = MaterialTheme.typography.labelSmall, color = heritageMuted())
                Text(title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = BookSerif, fontSize = 15.sp, lineHeight = 19.sp
                    ), color = heritageInk())
                if (topicCount > 0) Text(
                    (if (expanded) "⌄" else "›") + " Содержание · $topicCount",
                    style = MaterialTheme.typography.labelSmall, color = heritageMuted())
            }
            TextButton(onClick = onRead,
                modifier = Modifier.semantics { contentDescription = "Читать" }) {
                Text("Читать", style = MaterialTheme.typography.labelMedium,
                    color = BookColors.gold)
            }
        }
        page?.let {
            Text(it, modifier = Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 5.dp),
                textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall,
                color = heritageMuted())
        }
        if (percent > 0) LinearProgressIndicator(
            progress = { percent / 100f }, modifier = Modifier.fillMaxWidth().height(1.dp),
            color = BookColors.gold, trackColor = Color.Transparent
        )
        HorizontalDivider(modifier = Modifier.padding(start = 54.dp, top = 7.dp),
            thickness = .7.dp, color = BookColors.gold.copy(alpha = .28f))
    }
}

/** Same unframed topic item in all volumes. */
@Composable
internal fun TocTopicRow(label: String, title: String, page: String?,
                         modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.fillMaxWidth().padding(start = 64.dp)
        .clickable(onClick = onClick)
        .padding(end = 11.dp, top = 9.dp, bottom = 7.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = heritageMuted())
        Text(title, style = MaterialTheme.typography.bodyMedium, color = heritageInk())
        page?.let {
            Text(it, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelSmall, color = heritageMuted())
        }
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp),
            thickness = .7.dp, color = BookColors.gold.copy(alpha = .24f))
    }
}
