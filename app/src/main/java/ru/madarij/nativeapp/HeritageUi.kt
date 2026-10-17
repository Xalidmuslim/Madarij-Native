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
                quadraticBezierTo(w * .69f, h * .07f, w * .82f, h * .15f)
                lineTo(w * .80f, h * .83f)
                quadraticBezierTo(w * .50f, h * .88f, w * .21f, h * .83f)
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

/** Preserve older learning-screen callers while using the same manuscript artwork. */
@Composable
internal fun HeritageMedallion(number: Int, modifier: Modifier = Modifier) {
    HeritageManuscriptNumber(number, modifier)
}

/**
 * Two natively drawn parchment/leather page-tabs.
 * Unlike a bitmap cut from a reference, the frame scales with the screen,
 * responds to theme and selected state, and remains a real accessible tab.
 */
@Composable
internal fun HeritageTocVolumeTab(
    label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit
) {
    val shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp,
        bottomStart = 7.dp, bottomEnd = 7.dp)
    val rim = if (selected) Color(0xFFCDA36D) else Color(0xFFBD9870)
    Box(
        modifier.heightIn(min = CompactUi.tocVolumeMinHeightDp.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    if (selected) listOf(Color(0xFF503321), Color(0xFF2C1B13))
                    else listOf(Color(0xFFFFF2D8), Color(0xFFEBD5B2))
                ), shape
            )
            .border(.9.dp, rim, shape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        HeritageTocEngraving(rim, selected, Modifier.matchParentSize())
        Text(
            label, style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = BookSerif, fontSize = 17.sp, fontWeight = FontWeight.Medium),
            color = if (selected) Color(0xFFFFEDCD) else Color(0xFF4C301F),
            maxLines = 1, textAlign = TextAlign.Center
        )
    }
}

/**
 * A restrained old-document rim: a continuous etched inner outline and two
 * tiny binding marks. Native vector drawing only, no copied screenshot assets.
 */
@Composable
private fun HeritageTocEngraving(
    color: Color, selected: Boolean, modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val inset = 3.dp.toPx()
        val bodyWidth = (size.width - inset * 2f).coerceAtLeast(0f)
        val bodyHeight = (size.height - inset * 2f).coerceAtLeast(0f)
        drawRoundRect(
            color = color.copy(alpha = if (selected) .70f else .52f),
            topLeft = Offset(inset, inset),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = CornerRadius(7.dp.toPx()),
            style = Stroke(.6.dp.toPx())
        )
        val y = 6.dp.toPx()
        val x1 = 12.dp.toPx()
        val x2 = (size.width - x1).coerceAtLeast(x1)
        drawLine(
            color = Color.White.copy(alpha = if (selected) .12f else .44f),
            start = Offset(x1, y), end = Offset(x2, y),
            strokeWidth = .7.dp.toPx()
        )
    }
}

/** Same parchment-frame vocabulary for all three filtering actions. */
@Composable
internal fun HeritageTocFilterTab(
    label: String, kind: String, selected: Boolean,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    val shape = RoundedCornerShape(11.dp)
    val rim = if (selected) Color(0xFFCDA36D) else Color(0xFFC3A17D)
    val ink = if (selected) Color(0xFFFBE5C5) else Color(0xFF5A3825)
    Box(
        modifier.heightIn(min = CompactUi.tocFilterMinHeightDp.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    if (selected) listOf(Color(0xFF503321), Color(0xFF2B1B14))
                    else listOf(Color(0xFFFFEFD5), Color(0xFFECD6B8))
                ), shape
            )
            .border(.9.dp, rim, shape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        HeritageTocEngraving(rim, selected, Modifier.matchParentSize())
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            when (kind) {
                "all" -> Canvas(Modifier.size(14.dp)) {
                    repeat(3) { index ->
                        val y = size.height * (.14f + index * .26f)
                        drawRoundRect(
                            color = ink, topLeft = Offset(size.width * .16f, y),
                            size = Size(size.width * .70f, size.height * .14f),
                            cornerRadius = CornerRadius(.8.dp.toPx())
                        )
                    }
                }
                "read" -> Canvas(Modifier.size(14.dp)) {
                    drawCircle(ink, radius = size.minDimension * .41f,
                        style = Stroke(1.3.dp.toPx()))
                    val check = Path().apply {
                        moveTo(size.width * .24f, size.height * .51f)
                        lineTo(size.width * .44f, size.height * .69f)
                        lineTo(size.width * .75f, size.height * .30f)
                    }
                    drawPath(check, ink, style = Stroke(1.35.dp.toPx(), cap = StrokeCap.Round))
                }
                else -> NavigationGlyph("bookmarks",
                    modifier = Modifier.size(14.dp), tint = ink)
            }
            Spacer(Modifier.width(4.dp))
            Text(
                label, style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = BookSerif, fontSize = 13.sp,
                    lineHeight = 15.sp, fontWeight = FontWeight.Medium
                ),
                color = ink, textAlign = TextAlign.Center, maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
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
