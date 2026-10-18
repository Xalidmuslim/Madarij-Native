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
    Box(modifier.heightIn(min=CompactUi.choiceMinHeightDp.dp)
        .vintageFrame(if(selected) VintageFrameKind.DARK_COMPACT else VintageFrameKind.LIGHT_COMPACT)
        .clickable(onClick=onClick)
        .padding(horizontal=12.dp,vertical=7.dp),
        contentAlignment=Alignment.Center) {
        Text(label, style=MaterialTheme.typography.labelMedium.copy(
            fontFamily=BookSerif,fontSize=14.sp),
            textAlign=TextAlign.Center,maxLines=2,
            color=if(selected) Color(0xFFFFE9C0) else Color(0xFF51341F))
    }
}

/**
 * Original miniature parchment leaf, drawn entirely by Compose Canvas.
 *
 * The organic left/right edges and single subtle folded corner deliberately
 * avoid the thick horizontal roll caps that looked like cropped picture tiles.
 * The same 32dp silhouette is used by all chapters; only the numeral changes.
 * This is vector geometry, not a bitmap crop of the user's reference.
 */
@Composable
internal fun HeritageManuscriptNumber(number: Int, modifier: Modifier = Modifier) {
    Box(
        modifier.size(CompactUi.tocManuscriptIconDp.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val parchment = Path().apply {
                moveTo(w * .20f, h * .11f)
                quadraticBezierTo(w * .29f, h * .075f, w * .49f, h * .105f)
                quadraticBezierTo(w * .61f, h * .09f, w * .69f, h * .105f)
                lineTo(w * .80f, h * .22f)
                quadraticBezierTo(w * .76f, h * .36f, w * .80f, h * .49f)
                quadraticBezierTo(w * .75f, h * .66f, w * .79f, h * .81f)
                quadraticBezierTo(w * .64f, h * .88f, w * .49f, h * .86f)
                quadraticBezierTo(w * .32f, h * .90f, w * .20f, h * .84f)
                quadraticBezierTo(w * .17f, h * .69f, w * .21f, h * .54f)
                quadraticBezierTo(w * .16f, h * .34f, w * .20f, h * .11f)
                close()
            }
            // Warm matte leaf. No horizontal roll caps, texture crops or
            // rectangular plaques: only the organic parchment silhouette.
            drawPath(
                parchment,
                brush = Brush.verticalGradient(
                    listOf(Color(0xFFF7E2BD), Color(0xFFE5C396), Color(0xFFEDD2AA))
                )
            )
            drawPath(
                parchment, color = Color(0xFF98734D),
                style = Stroke(width = .9.dp.toPx())
            )
            // Tiny upper-right dog-ear: one fold, not a decorative frame.
            val dogEar = Path().apply {
                moveTo(w * .68f, h * .105f)
                quadraticBezierTo(w * .66f, h * .19f, w * .68f, h * .235f)
                quadraticBezierTo(w * .74f, h * .23f, w * .80f, h * .22f)
                close()
            }
            drawPath(dogEar, color = Color(0xFFD1AB77).copy(alpha = .8f))
            drawLine(
                Color(0xFFAD875C).copy(alpha = .7f),
                Offset(w * .68f, h * .235f),
                Offset(w * .77f, h * .222f),
                strokeWidth = .6.dp.toPx()
            )
            // Restrained left manuscript edge gives depth at tiny sizes.
            val innerEdge = Path().apply {
                moveTo(w * .27f, h * .19f)
                quadraticBezierTo(w * .23f, h * .46f, w * .275f, h * .76f)
            }
            drawPath(
                innerEdge,
                color = Color(0xFF9B744E).copy(alpha = .35f),
                style = Stroke(width = .65.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Text(
            number.toString(),
            modifier = Modifier.offset(x = 0.dp, y = 1.dp),
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = BookSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                lineHeight = 17.sp
            ),
            color = Color(0xFF51341F),
            maxLines = 1
        )
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
    Box(
        modifier.heightIn(min = CompactUi.tocVolumeMinHeightDp.dp)
            .vintageFrame(if (selected) VintageFrameKind.DARK_WIDE else VintageFrameKind.LIGHT_WIDE)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = BookSerif, fontSize = 17.sp, fontWeight = FontWeight.Medium),
            color = if (selected) Color(0xFFFFEDCD) else Color(0xFF4C301F),
            maxLines = 1, textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun HeritageTocFilterTab(
    label: String, kind: String, selected: Boolean,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    val ink = if (selected) Color(0xFFFBE5C5) else Color(0xFF5A3825)
    Box(
        modifier.heightIn(min = CompactUi.tocFilterMinHeightDp.dp)
            .vintageFrame(if (selected) VintageFrameKind.DARK_COMPACT else VintageFrameKind.LIGHT_COMPACT)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            when(kind) {
                "all" -> NavigationGlyph("contents", modifier=Modifier.size(13.dp), tint=ink)
                "read" -> Text("✓", color=ink, fontSize=14.sp)
                else -> NavigationGlyph("bookmarks", modifier=Modifier.size(13.dp),tint=ink)
            }
            Spacer(Modifier.width(4.dp))
            Text(label, style=MaterialTheme.typography.labelMedium.copy(
                fontFamily=BookSerif,fontSize=12.sp,lineHeight=14.sp, fontWeight=FontWeight.Medium),
                color=ink,textAlign=TextAlign.Center,maxLines=2,
                overflow=TextOverflow.Ellipsis)
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

/** One lightweight paper sheet per menu category. Compact native rows save
 * vertical space and prevent the floating navigation from obscuring Settings. */
@Composable
internal fun HeritageMenuGroup(
    items: List<Triple<String, String, String>>,
    navigate: (String) -> Unit
) {
    AgedPaperCard(modifier = Modifier.fillMaxWidth(),
        frameKind = VintageFrameKind.LIGHT_WIDE) {
        items.forEachIndexed { index, item ->
            Row(
                Modifier.fillMaxWidth().clickable { navigate(item.first) }
                    .heightIn(min = 61.dp)
                    .padding(horizontal = 7.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(31.dp), contentAlignment = Alignment.Center) {
                    HeritageMenuIcon(item.first)
                }
                Column(Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(item.second,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = BookSerif, fontSize = 17.sp),
                        color = heritageInk(), maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    Text(item.third,
                        style = MaterialTheme.typography.bodySmall,
                        color = heritageMuted(), maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                }
                Text("›", fontSize = 22.sp, color = BookColors.gold)
            }
            if (index != items.lastIndex) HorizontalDivider(
                modifier = Modifier.padding(horizontal = 8.dp),
                color = BookColors.gold.copy(alpha = .16f),
                thickness = .5.dp
            )
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
