package ru.madarij.nativeapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable internal fun heritageInk(): Color =
    if (MaterialTheme.colorScheme.background == BookColors.nightBackground)
        BookColors.nightText else BookColors.ink

@Composable internal fun heritageMuted(): Color =
    if (MaterialTheme.colorScheme.background == BookColors.nightBackground)
        BookColors.nightMuted else BookColors.muted

@Composable internal fun heritageCard(): Color =
    if (MaterialTheme.colorScheme.background == BookColors.nightBackground)
        BookColors.nightCard else BookColors.card

/**
 * Decorative presentation primitives. No decoded images, GPU saveLayers or
 * effects are added to the existing paper background and leather navigation.
 * All labels and icons remain native, accessible text and vector artwork.
 */
@Composable
internal fun HeritageSectionHeading(title: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = BookSerif, fontSize = CompactUi.sectionTitleSp.sp, fontWeight = FontWeight.SemiBold),
            color = heritageInk())
        Box(Modifier.weight(1f).height(1.dp)
            .background(BookColors.gold.copy(alpha = .35f)))
        if (trailing != null) Text(trailing,
            style = MaterialTheme.typography.labelSmall, color = heritageMuted())
        else Text("✥", fontSize = 12.sp, color = BookColors.gold)
    }
}

@Composable
internal fun HeritageChoice(label: String, selected: Boolean, modifier: Modifier = Modifier,
                            onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(modifier.heightIn(min = CompactUi.choiceMinHeightDp.dp).clip(shape)
        .background(if (selected) BookColors.leather else heritageCard().copy(alpha = .64f))
        .border(.9.dp, if (selected) BookColors.gold.copy(alpha = .8f)
                      else BookColors.gold.copy(alpha = .32f), shape)
        .clickable(onClick = onClick).padding(horizontal = 7.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center) {
        Text(label, style = MaterialTheme.typography.labelMedium.copy(
            fontFamily = BookSerif, fontSize = 14.sp),
            textAlign = TextAlign.Center, maxLines = 2,
            color = if (selected) Color(0xFFFFE9C0) else heritageInk())
    }
}

/**
 * A single 38dp old manuscript icon is used for every TOC group.
 * The page silhouette stays identical; only its printed numeral changes.
 * Rendered with lightweight native Canvas geometry, never screenshot slices.
 */
@Composable
internal fun HeritageManuscriptNumber(number: Int, modifier: Modifier = Modifier) {
    Box(modifier.size(CompactUi.tocManuscriptIconDp.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val stroke = .95.dp.toPx()
            val page = Path().apply {
                moveTo(w * .25f, h * .11f)
                quadraticTo(w * .69f, h * .07f, w * .82f, h * .15f)
                lineTo(w * .80f, h * .83f)
                quadraticTo(w * .50f, h * .88f, w * .21f, h * .83f)
                close()
            }
            // Restrained warm-ink shadow, aged page and curling edges.
            drawRoundRect(
                color = Color(0xFF634025).copy(alpha = .17f),
                topLeft = Offset(w * .26f, h * .16f),
                size = Size(w * .63f, h * .76f),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
            drawPath(page, brush = Brush.verticalGradient(listOf(
                Color(0xFFFFE9BD), Color(0xFFE9C595), Color(0xFFD5AE79)
            )))
            drawPath(page, color = Color(0xFF926741), style = Stroke(stroke))
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFFFBE2B4), Color(0xFFCE9C61))),
                topLeft = Offset(w * .16f, h * .09f),
                size = Size(w * .62f, h * .13f),
                cornerRadius = CornerRadius(h * .065f)
            )
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFFD8AA75), Color(0xFFF6D5A5))),
                topLeft = Offset(w * .15f, h * .77f),
                size = Size(w * .67f, h * .13f),
                cornerRadius = CornerRadius(h * .06f)
            )
            drawLine(Color(0xFF976A44), Offset(w * .19f,h * .19f),
                Offset(w * .77f,h * .19f), strokeWidth = .7.dp.toPx())
            drawLine(Color(0xFFAA7848), Offset(w * .18f,h * .83f),
                Offset(w * .79f,h * .83f), strokeWidth = .7.dp.toPx())
        }
        Text(number.toString(),
            modifier = Modifier.offset(x = 1.dp, y = 1.dp),
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = BookSerif, fontWeight = FontWeight.Bold, fontSize = 18.sp),
            color = Color(0xFF58351E))
    }
}

/** Volumes: understated old binding tabs, not generic rounded Material chips. */
@Composable
internal fun HeritageTocVolumeTab(
    label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit
) {
    val shape = RoundedCornerShape(topStart = 13.dp, topEnd = 13.dp,
        bottomStart = 5.dp, bottomEnd = 5.dp)
    val rim = if (selected) Color(0xFFC69B61) else Color(0xFFC0A078)
    Box(
        modifier.heightIn(min = CompactUi.tocVolumeMinHeightDp.dp)
            .clip(shape)
            .background(Brush.verticalGradient(
                if (selected) listOf(Color(0xFF573723), Color(0xFF28180F))
                else listOf(Color(0xFFFFF3DE), Color(0xFFE6CFAB))), shape)
            .border(1.dp, rim, shape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawLine(rim.copy(alpha = .7f),
                Offset(8.dp.toPx(), 5.dp.toPx()),
                Offset(size.width - 8.dp.toPx(), 5.dp.toPx()),
                strokeWidth = .65.dp.toPx())
            drawLine(rim.copy(alpha = .40f),
                Offset(5.dp.toPx(), size.height - 4.dp.toPx()),
                Offset(size.width - 5.dp.toPx(), size.height - 4.dp.toPx()),
                strokeWidth = .7.dp.toPx())
        }
        Text(label,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = BookSerif, fontSize = 17.sp, fontWeight = FontWeight.Medium),
            color = if (selected) Color(0xFFFFE9C3) else Color(0xFF422A1A),
            maxLines = 1, textAlign = TextAlign.Center)
    }
}

/** Compact parchment filters for All / Read / With bookmarks. */
@Composable
internal fun HeritageTocFilterTab(
    label: String, kind: String, selected: Boolean,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    val shape = RoundedCornerShape(11.dp)
    val accent = if (selected) Color(0xFFF2D3A0) else Color(0xFF775035)
    Box(
        modifier.heightIn(min = CompactUi.tocFilterMinHeightDp.dp)
            .clip(shape)
            .background(Brush.verticalGradient(
                if (selected) listOf(Color(0xFF573723), Color(0xFF2D1A11))
                else listOf(Color(0xFFFFF1DB), Color(0xFFE9D4B4))), shape)
            .border(1.dp, if (selected) Color(0xFFBF925C) else Color(0xFFBDA07B), shape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            when (kind) {
                "all" -> Canvas(Modifier.size(16.dp)) {
                    val line = 1.25.dp.toPx()
                    for (index in 0..2) {
                        val y = size.height * (.15f + index * .23f)
                        drawRoundRect(color = accent, topLeft = Offset(size.width * .20f,y),
                            size = Size(size.width * .60f, size.height * .15f),
                            cornerRadius = CornerRadius(1.dp.toPx()))
                        drawLine(if (selected) Color(0xFF53331E) else Color(0xFFFFEAD0),
                            Offset(size.width * .28f,y + line),
                            Offset(size.width * .73f,y + line), line)
                    }
                }
                "read" -> Canvas(Modifier.size(16.dp)) {
                    drawCircle(accent, radius = size.minDimension * .42f,
                        style = Stroke(1.5.dp.toPx()))
                    val p = Path().apply {
                        moveTo(size.width * .24f, size.height * .51f)
                        lineTo(size.width * .44f, size.height * .70f)
                        lineTo(size.width * .76f, size.height * .29f)
                    }
                    drawPath(p, accent, style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round))
                }
                else -> NavigationGlyph("bookmarks",
                    modifier = Modifier.size(16.dp), tint = accent)
            }
            Spacer(Modifier.width(4.dp))
            Text(label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = BookSerif, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                color = if (selected) Color(0xFFFFE9C3) else Color(0xFF4C3322),
                textAlign = TextAlign.Center, maxLines = 2,
                overflow = TextOverflow.Clip, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun HeritageMenuIcon(kind: String) {
    when(kind) {
        "progress" -> Canvas(Modifier.size(26.dp)) {
            val bronze=BookColors.gold
            val w=size.width
            val h=size.height
            listOf(.35f,.68f,.94f).forEachIndexed { i, height ->
                drawRoundRect(bronze.copy(alpha=.9f),
                    topLeft=Offset(w*(.10f+i*.30f),h*(1f-height)),
                    size=androidx.compose.ui.geometry.Size(w*.17f,h*height),
                    cornerRadius=androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
            }
        }
        "settings" -> Canvas(Modifier.size(26.dp)) {
            val bronze=BookColors.gold
            val c=Offset(size.width/2,size.height/2)
            val r=size.minDimension*.29f
            drawCircle(bronze,r,c,style=Stroke(2.8.dp.toPx()))
            drawCircle(bronze,r*.38f,c,style=Stroke(2.dp.toPx()))
            repeat(8) { i ->
                val angle=i*Math.PI/4.0
                val dx=kotlin.math.cos(angle).toFloat()
                val dy=kotlin.math.sin(angle).toFloat()
                drawLine(bronze,
                    Offset(c.x+dx*r*.99f,c.y+dy*r*.99f),
                    Offset(c.x+dx*r*1.45f,c.y+dy*r*1.45f),
                    strokeWidth=3.3.dp.toPx(),cap=StrokeCap.Round)
            }
        }
        "about" -> Text("ⓘ",fontSize=26.sp,color=BookColors.gold,
            fontFamily=BookSerif)
        else -> NavigationGlyph(kind, modifier=Modifier.size(26.dp), tint=BookColors.gold)
    }
}

@Composable
internal fun HeritageMenuGroup(items: List<Triple<String, String, String>>, navigate: (String) -> Unit) {
    AgedPaperCard(Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = heritageCard().copy(alpha = .72f)),
        border = BorderStroke(.8.dp, BookColors.gold.copy(alpha = .32f)),
        shape = RoundedCornerShape(17.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column {
            items.forEachIndexed { index, item ->
                Row(Modifier.fillMaxWidth().clickable { navigate(item.first) }
                    .heightIn(min = CompactUi.menuRowMinHeightDp.dp)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                        HeritageMenuIcon(item.first)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(item.second, style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = BookSerif, fontSize = 17.sp), color = heritageInk())
                        Text(item.third, style = MaterialTheme.typography.bodySmall,
                            color = heritageMuted(), maxLines = 2,
                            overflow = TextOverflow.Ellipsis)
                    }
                    Text("›", fontSize = 23.sp, color = BookColors.gold)
                }
                if (index < items.lastIndex)
                    HorizontalDivider(color = BookColors.gold.copy(alpha = .16f),
                        thickness = .7.dp, modifier = Modifier.padding(horizontal = 14.dp))
            }
        }
    }
}

@Composable
internal fun HeritageProgressRing(
    fraction: Float, value: String, total: String, caption: String,
    icon: String, modifier: Modifier = Modifier
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(CompactUi.progressRingDp.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
                drawArc(BookColors.gold.copy(alpha = .22f), startAngle = -90f,
                    sweepAngle = 360f, useCenter = false, style = stroke)
                if (fraction > 0f) drawArc(BookColors.gold, startAngle = -90f,
                    sweepAngle = fraction.coerceIn(0f, 1f) * 360f,
                    useCenter = false, style = stroke)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(value, style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 22.sp, fontFamily = BookSerif),
                    color = heritageInk())
                NavigationGlyph(icon, modifier = Modifier.size(21.dp),
                    tint = BookColors.gold)
            }
        }
        Text(total, style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = BookSerif, fontSize = 16.sp), color = heritageInk())
        Text(caption, style = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = BookSerif), color = heritageInk())
    }
}

@Composable
internal fun HeritageMetricRow(icon: String, label: String, value: String,
                               onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = CompactUi.actionMinHeightDp.dp).clickable(onClick = onClick)
        .padding(horizontal = 11.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        NavigationGlyph(icon, modifier = Modifier.size(25.dp), tint = BookColors.gold)
        Text(label, Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = BookSerif),
            color = heritageInk())
        Text(value, style = MaterialTheme.typography.titleSmall.copy(fontFamily = BookSerif),
            color = heritageInk())
        Text("›", style = MaterialTheme.typography.titleLarge, color = BookColors.gold)
    }
}

@Composable
internal fun HeritageActivityBars(values: List<Int>, labels: List<String>,
                                  highlightIndex: Int = -1) {
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    Row(Modifier.fillMaxWidth().height(CompactUi.activityGraphDp.dp).padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom) {
        values.forEachIndexed { i, v ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(if (v > 0) "$v" else " ", style = MaterialTheme.typography.labelSmall,
                    color = heritageMuted())
                val height = (8f + v * 67f / max).dp
                Box(Modifier.fillMaxWidth(.75f).height(height)
                    .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp))
                    .background(if (i == highlightIndex)
                        BookColors.gold else BookColors.gold.copy(alpha = .47f)))
                Text(labels.getOrElse(i) { "" }, style = MaterialTheme.typography.labelSmall,
                    color = heritageInk(), textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}
