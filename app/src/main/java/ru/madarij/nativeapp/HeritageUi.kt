package ru.madarij.nativeapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
internal fun HeritageMedallion(number: Int, modifier: Modifier = Modifier) {
    Box(modifier.size(CompactUi.tocMedallionDp.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val outer = size.minDimension * .46f
            drawCircle(BookColors.gold.copy(alpha = .42f), outer, center,
                style = Stroke(1.5.dp.toPx()))
            drawCircle(BookColors.leather, outer * .8f, center)
            drawCircle(BookColors.lightGold, outer * .77f, center,
                style = Stroke(1.dp.toPx()))
            listOf(0f, 1f, 2f, 3f).forEach { i ->
                val x = when (i.toInt()) { 0 -> center.x; 1 -> center.x + outer; 2 -> center.x; else -> center.x - outer }
                val y = when (i.toInt()) { 0 -> center.y - outer; 1 -> center.y; 2 -> center.y + outer; else -> center.y }
                drawCircle(BookColors.gold.copy(alpha = .78f), 2.5.dp.toPx(), Offset(x, y))
            }
        }
        Text(number.toString(), style = MaterialTheme.typography.titleLarge.copy(
            fontFamily = BookSerif, fontSize = 20.sp),
            color = Color(0xFFFFEBD1))
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
