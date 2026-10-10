package ru.madarij.nativeapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import ru.madarij.nativeapp.data.ReadingSettings
import java.text.Normalizer

internal object AppSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 28.dp
}

internal object AppRadius {
    val small = 14.dp
    val medium = 18.dp
    val large = 20.dp
    val hero = 22.dp
}

internal object AppElevation {
    val level0 = 0.dp
    val level1 = 0.dp
    val level2 = 0.dp
    val level3 = 0.dp
    val level4 = 0.dp
}

internal object AppMotion {
    const val fast = 160
    const val normal = 220
    const val screen = 280
}

internal val MadarijTypography = Typography(
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 35.sp, letterSpacing = (-.25).sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 25.sp, lineHeight = 31.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 23.sp, lineHeight = 29.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 23.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 17.sp, lineHeight = 27.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 15.sp, lineHeight = 23.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp)
)

@Composable
internal fun Soft3DBackdrop(modifier: Modifier = Modifier, calm: Boolean = false) {
    Box(modifier.background(MaterialTheme.colorScheme.background))
}

// Kept signatures preserve existing callers while replacing their visual surface.
@Composable
internal fun Soft3DPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(AppRadius.large),
    elevation: Dp = AppElevation.level2,
    warm: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val base = Modifier.clip(shape).background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .32f), shape)
    Box(modifier.then(if (onClick != null) base.clickable(onClick = onClick) else base), content = content)
}

@Composable
internal fun Soft3DSagePanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(AppRadius.hero),
    elevation: Dp = AppElevation.level2,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val base = Modifier.clip(shape).background(MaterialTheme.colorScheme.secondaryContainer)
        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .3f), shape)
    Box(modifier.then(if (onClick != null) base.clickable(onClick = onClick) else base), content = content)
}

@Composable
internal fun Soft3DIconPuck(kind: String, modifier: Modifier = Modifier) {
    Box(modifier.size(40.dp), contentAlignment = Alignment.Center) {
        NavigationGlyph(kind, selected = true, modifier = Modifier.size(25.dp))
    }
}

@Composable
fun InfoCard(title: String, body: String, modifier: Modifier = Modifier) {
    Soft3DPanel(modifier = modifier.fillMaxWidth(), elevation = AppElevation.level2) {
        Column(Modifier.padding(AppSpacing.lg), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun ScreenHeading(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(vertical = 2.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun MadarijTopBar(
    title: String, subtitle: String?, canBack: Boolean, onBack: () -> Unit,
    showSearch: Boolean, onSearch: () -> Unit,
    showTextSettings: Boolean, onTextSettings: () -> Unit, onBookmarks: () -> Unit = {}
) {
    Column(Modifier.fillMaxWidth().statusBarsPadding()) {
        if (title == "Оглавление") {
            Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 10.dp, top = 11.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = BookSerif, fontSize = 32.sp, lineHeight = 37.sp),
                        color = BookColors.ink)
                    if (subtitle != null) Text(subtitle,
                        style = MaterialTheme.typography.bodySmall, color = BookColors.muted)
                }
                IconButton(onClick = onSearch) {
                    NavigationGlyph("search", modifier = Modifier.size(23.dp), tint = BookColors.ink)
                }
                TextButton(onClick = onTextSettings, modifier = Modifier.width(44.dp),
                    contentPadding = PaddingValues(0.dp)) {
                    Text("Aa", color = BookColors.gold, fontSize = 17.sp)
                }
            }
        } else {
            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack, modifier = Modifier.size(44.dp),
                    contentPadding = PaddingValues(0.dp)) {
                    Text("‹", style = MaterialTheme.typography.headlineSmall)
                }
                Text(title, Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = BookSerif, fontWeight = FontWeight.Normal, fontSize = 20.sp),
                    textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (showSearch) IconButton(onClick = onSearch) {
                    NavigationGlyph("search", modifier = Modifier.size(21.dp))
                } else Spacer(Modifier.width(44.dp))
            }
        }
        HorizontalDivider(color = BookColors.gold.copy(alpha = .15f))
    }
}
@Composable
internal fun MadarijBottomBar(items: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Box(Modifier.fillMaxWidth().height(66.dp)
        .clip(RoundedCornerShape(19.dp))
        .background(BookColors.leather)
        .border(.7.dp, BookColors.lightGold.copy(alpha = .43f), RoundedCornerShape(19.dp))) {
        PreloadedBookImage(R.drawable.navigation_leather,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.FillBounds)
        Row(
            Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (destination, label) ->
                val active = selected == destination
                val labelColor = androidx.compose.animation.animateColorAsState(
                    targetValue = if (active) BookColors.lightGold else Color(0xFFB9A793),
                    animationSpec = androidx.compose.animation.core.tween(110),
                    label = "tab-color"
                ).value
                Column(
                    Modifier.weight(1f).fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelect(destination) }
                        .padding(vertical = 1.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    NavigationGlyph(
                        destination, selected = active,
                        modifier = Modifier.size(22.dp),
                        tint = labelColor
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = labelColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
@Composable
internal fun AppActionGlyph(kind: String, modifier: Modifier = Modifier) {
    Soft3DIconPuck(kind, modifier)
}

@Composable
internal fun ReaderControl(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.size(48.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

internal fun russianFamily(value: ReadingSettings): FontFamily = when (value.russianFont) {
    "sans", "sans-serif" -> FontFamily.SansSerif
    "book" -> FontFamily(Font(R.font.russian_book))
    "mono", "monospace" -> FontFamily.Monospace
    else -> FontFamily.Serif
}

internal fun readerStyle(value: ReadingSettings) = TextStyle(
    fontFamily = russianFamily(value), fontSize = value.russianSize.sp,
    lineHeight = (value.russianSize * value.lineHeight).sp, textDirection = TextDirection.Ltr,
    color = Color.Unspecified,
    textAlign = when (value.alignment) { "justify" -> TextAlign.Justify; "center" -> TextAlign.Center; else -> TextAlign.Start }
)

@Composable
internal fun ArabicText(text: String, settings: ReadingSettings, modifier: Modifier = Modifier) {
    androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(text, modifier.fillMaxWidth(), style = TextStyle(
            fontFamily = FontFamily(Font(if (settings.arabicFont == "alternate") R.font.arabic_naskh_alt else R.font.naskh)),
            fontSize = settings.arabicSize.sp, lineHeight = (settings.arabicSize * 1.85f).sp,
            textDirection = TextDirection.Rtl, textAlign = TextAlign.Start
        ))
    }
}

internal data class NormalizedText(val text: String, val offsets: List<Int>)
internal fun normalizedText(value: String): NormalizedText {
    val output = StringBuilder(); val offsets = mutableListOf<Int>()
    value.forEachIndexed { index, character ->
        val plain = Normalizer.normalize(character.toString(), Normalizer.Form.NFD)
        plain.forEach { c ->
            if (Character.getType(c) != Character.NON_SPACING_MARK.toInt() && c != 'ـ') {
                output.append(when(c.lowercaseChar()) { 'ё' -> 'е'; 'أ', 'إ', 'آ', 'ٱ' -> 'ا'; 'ى' -> 'ي'; else -> c.lowercaseChar() }); offsets += index
            }
        }
    }
    return NormalizedText(output.toString(), offsets)
}

internal fun highlight(text: String, query: String, color: Color): AnnotatedString = buildAnnotatedString {
    append(text)
    val normalized = normalizedText(text)
    val needle = normalizedText(query.trim()).text
    if (needle.isNotBlank()) {
        var at = normalized.text.indexOf(needle)
        while (at >= 0) {
            val start = normalized.offsets[at]
            val end = normalized.offsets[at + needle.length - 1] + 1
            addStyle(SpanStyle(background = color.copy(alpha = .16f), fontWeight = FontWeight.SemiBold), start, end)
            at = normalized.text.indexOf(needle, at + needle.length)
        }
    }
}

@Composable
internal fun NavigationGlyph(kind: String, selected: Boolean = false, modifier: Modifier = Modifier, tint: Color? = null) {
    val color = tint ?: if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val accent = tint ?: MaterialTheme.colorScheme.secondary
    Canvas(modifier) {
        val s = size.width
        val stroke = Stroke(
            width = if (selected) 1.9.dp.toPx() else 1.65.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        val fine = Stroke(width = stroke.width * .72f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun path(style: Stroke = stroke, block: Path.() -> Unit) {
            drawPath(Path().apply(block), color, style = style)
        }
        fun accentDot(x: Float, y: Float, radius: Float = .045f) {
            drawCircle(accent.copy(alpha = if (selected) .95f else .72f), s * radius, Offset(s * x, s * y))
        }
        when (kind) {
            "toc", "more" -> {
                repeat(3) { i ->
                    val y = s*(.24f + i*.25f)
                    drawCircle(color,s*.04f,Offset(s*.16f,y))
                    drawLine(color,Offset(s*.33f,y),Offset(s*.87f,y),stroke.width,cap=StrokeCap.Round)
                }
            }
            "home" -> {
                path {
                    moveTo(s*.12f,s*.47f); lineTo(s*.5f,s*.15f); lineTo(s*.88f,s*.47f)
                    moveTo(s*.22f,s*.43f); lineTo(s*.22f,s*.86f); lineTo(s*.78f,s*.86f); lineTo(s*.78f,s*.43f)
                    moveTo(s*.41f,s*.86f); lineTo(s*.41f,s*.63f); quadraticBezierTo(s*.5f,s*.58f,s*.59f,s*.63f); lineTo(s*.59f,s*.86f)
                }
                accentDot(.5f,.15f,.035f)
            }
            "contents" -> {
                path {
                    moveTo(s*.5f,s*.24f); cubicTo(s*.39f,s*.13f,s*.17f,s*.12f,s*.12f,s*.19f)
                    lineTo(s*.12f,s*.79f); cubicTo(s*.28f,s*.74f,s*.4f,s*.78f,s*.5f,s*.88f)
                    cubicTo(s*.6f,s*.78f,s*.72f,s*.74f,s*.88f,s*.79f); lineTo(s*.88f,s*.19f)
                    cubicTo(s*.83f,s*.12f,s*.61f,s*.13f,s*.5f,s*.24f); lineTo(s*.5f,s*.88f)
                }
                drawLine(accent.copy(alpha=.9f), Offset(s*.25f,s*.37f), Offset(s*.41f,s*.4f), fine.width, cap=StrokeCap.Round)
                drawLine(accent.copy(alpha=.7f), Offset(s*.59f,s*.4f), Offset(s*.75f,s*.37f), fine.width, cap=StrokeCap.Round)
            }
            "study" -> {
                path {
                    moveTo(s*.09f,s*.32f); lineTo(s*.5f,s*.12f); lineTo(s*.91f,s*.32f); lineTo(s*.5f,s*.53f); close()
                    moveTo(s*.24f,s*.43f); lineTo(s*.24f,s*.67f); cubicTo(s*.38f,s*.8f,s*.62f,s*.8f,s*.76f,s*.67f); lineTo(s*.76f,s*.43f)
                    moveTo(s*.82f,s*.36f); lineTo(s*.82f,s*.67f)
                }
                accentDot(.82f,.73f,.045f)
            }
            "search" -> {
                drawCircle(color, s*.255f, Offset(s*.42f,s*.4f), style=stroke)
                drawLine(color, Offset(s*.61f,s*.59f), Offset(s*.87f,s*.85f), stroke.width, cap=StrokeCap.Round)
                accentDot(.35f,.34f,.045f)
            }
            "glossary" -> {
                path {
                    moveTo(s*.2f,s*.12f); lineTo(s*.74f,s*.12f); quadraticBezierTo(s*.82f,s*.12f,s*.82f,s*.2f)
                    lineTo(s*.82f,s*.82f); quadraticBezierTo(s*.82f,s*.88f,s*.74f,s*.88f); lineTo(s*.2f,s*.88f); close()
                    moveTo(s*.31f,s*.12f); lineTo(s*.31f,s*.88f)
                }
                drawLine(color,Offset(s*.43f,s*.34f),Offset(s*.69f,s*.34f),fine.width,cap=StrokeCap.Round)
                drawLine(color,Offset(s*.43f,s*.5f),Offset(s*.69f,s*.5f),fine.width,cap=StrokeCap.Round)
                drawLine(color,Offset(s*.43f,s*.66f),Offset(s*.62f,s*.66f),fine.width,cap=StrokeCap.Round)
                drawLine(accent.copy(alpha=.9f),Offset(s*.26f,s*.28f),Offset(s*.26f,s*.54f),fine.width,cap=StrokeCap.Round)
            }
            "bookmarks" -> {
                path {
                    moveTo(s*.27f,s*.12f); quadraticBezierTo(s*.27f,s*.1f,s*.31f,s*.1f); lineTo(s*.69f,s*.1f)
                    quadraticBezierTo(s*.73f,s*.1f,s*.73f,s*.14f); lineTo(s*.73f,s*.89f); lineTo(s*.5f,s*.72f); lineTo(s*.27f,s*.89f); close()
                }
                accentDot(.5f,.28f,.045f)
            }
            "notes" -> {
                path {
                    moveTo(s*.17f,s*.12f); lineTo(s*.68f,s*.12f); lineTo(s*.84f,s*.28f); lineTo(s*.84f,s*.86f); lineTo(s*.17f,s*.86f); close()
                    moveTo(s*.68f,s*.12f); lineTo(s*.68f,s*.28f); lineTo(s*.84f,s*.28f)
                }
                drawLine(color,Offset(s*.31f,s*.42f),Offset(s*.7f,s*.42f),fine.width,cap=StrokeCap.Round)
                drawLine(color,Offset(s*.31f,s*.57f),Offset(s*.66f,s*.57f),fine.width,cap=StrokeCap.Round)
                drawLine(color,Offset(s*.31f,s*.72f),Offset(s*.56f,s*.72f),fine.width,cap=StrokeCap.Round)
                accentDot(.75f,.2f,.035f)
            }
            else -> {
                drawCircle(color, radius = s*.055f, center = Offset(s*.22f,s*.5f))
                drawCircle(accent.copy(alpha=.9f), radius = s*.065f, center = Offset(s*.5f,s*.5f))
                drawCircle(color, radius = s*.055f, center = Offset(s*.78f,s*.5f))
            }
        }
    }
}

@Composable
internal fun SettingSwitch(label: String, checked: Boolean, change: (Boolean) -> Unit, description: String? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) { Text(label); if (description != null) Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Switch(checked, change)
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(AppRadius.medium),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .42f)),
        shadowElevation = 0.dp
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun ReadingSlider(label: String, valueLabel: String, value: Float, range: ClosedFloatingPointRange<Float>, onValue: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(valueLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BookTextSlider(value, onValue, {}, range = range)
    }
}

@Composable
fun SettingsPanel(value: ReadingSettings, update: (ReadingSettings) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(horizontal=16.dp,vertical=10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            SettingsGroup("Оформление") {
                Text("Фон чтения", style = MaterialTheme.typography.labelLarge)
                ReadingPaperSelector(value.paperTone) { newTone ->
                    update(value.copy(paperTone = newTone, theme = if (value.theme == "dark") "sepia" else value.theme))
                }
                SettingSwitch("Ночной режим", value.theme == "dark",
                    { update(value.copy(theme = if (it) "dark" else "sepia")) })
                Text("Русский шрифт", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement=Arrangement.spacedBy(6.dp)) { listOf("sans" to "Стандартный", "book" to "Книжный", "serif" to "С засечками").forEach { (key,label) -> FilterChip(value.russianFont == key, { update(value.copy(russianFont = key)) }, { Text(label) }) } }
                ReadingSlider("Размер русского текста", "${value.russianSize.toInt()}", value.russianSize, 14f..36f) { update(value.copy(russianSize = it)) }
                ReadingSlider("Межстрочный интервал", "${"%.1f".format(value.lineHeight)}", value.lineHeight, 1.3f..2.2f) { update(value.copy(lineHeight = it)) }
                ReadingSlider("Ширина текста", "${(value.textWidth * 100).toInt()}%", value.textWidth, .65f..1f) { update(value.copy(textWidth = it)) }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement=Arrangement.spacedBy(6.dp)) { FilterChip(value.alignment == "start", { update(value.copy(alignment = "start")) }, { Text("Слева") }); FilterChip(value.alignment == "justify", { update(value.copy(alignment = "justify")) }, { Text("По ширине") }) }
            }
        }
        item {
            SettingsGroup("Арабский оригинал") {
                SettingSwitch("Показывать арабский", value.showArabic, { update(value.copy(showArabic = it)) })
                if (value.showArabic) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement=Arrangement.spacedBy(6.dp)) { FilterChip(value.arabicFont == "naskh", { update(value.copy(arabicFont = "naskh")) }, { Text("Насх") }); FilterChip(value.arabicFont == "alternate", { update(value.copy(arabicFont = "alternate")) }, { Text("Насх · второй") }) }
                    ReadingSlider("Размер арабского текста", "${value.arabicSize.toInt()}", value.arabicSize, 20f..44f) { update(value.copy(arabicSize = it)) }
                }
            }
        }
        item {
            SettingsGroup("Дополнительно") {
                SettingSwitch("Примечания редакции", value.showNotes, { update(value.copy(showNotes = it)) })
                SettingSwitch("Автояркость устройства", value.brightness < 0f, { update(value.copy(brightness = if(it) -1f else .5f)) })
                if (value.brightness >= 0f) ReadingSlider("Яркость экрана чтения", "${(value.brightness * 100).toInt()}%", value.brightness, .05f..1f) { update(value.copy(brightness = it)) }
                SettingSwitch("Уменьшить движение", value.reducedMotion, { update(value.copy(reducedMotion = it)) })
            }
        }
        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(AppRadius.large),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .18f))
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement=Arrangement.spacedBy(8.dp), horizontalAlignment=Alignment.CenterHorizontally) {
                    Text("Предпросмотр",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
                    Text("Чтение помогает остановиться, вдуматься и вернуться к смыслу текста.",Modifier.fillMaxWidth(value.textWidth),style=readerStyle(value))
                    if(value.showArabic) ArabicText("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",value,Modifier.fillMaxWidth(value.textWidth))
                }
            }
        }
        item { TextButton(onClick={update(ReadingSettings())}) { Text("Сбросить настройки") } }
    }
}

@Composable
internal fun ReadingPaperSelector(selected: String, change: (String) -> Unit) {
    val options = listOf(
        Triple("warm", "Тёплый", R.drawable.reference_paper),
        Triple("light", "Светлый", R.drawable.paper_light),
        Triple("sage", "Светлый шалфей", R.drawable.paper_sage)
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (key, label, drawable) ->
            val active = selected == key
            val frame = RoundedCornerShape(13.dp)
            Column(
                Modifier.weight(1f).clip(frame)
                    .border(if (active) 1.5.dp else .7.dp,
                        if (active) BookColors.gold else BookColors.gold.copy(alpha = .27f), frame)
                    .clickable { change(key) }.padding(5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(9.dp))) {
                    PreloadedBookImage(drawable, modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop)
                    Text("Степени\nидущих", Modifier.align(Alignment.CenterStart).padding(start = 6.dp),
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = BookSerif),
                        color = BookColors.ink)
                }
                Text(label, style = MaterialTheme.typography.labelMedium,
                    color = if (active) BookColors.gold else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1)
            }
        }
    }
}
