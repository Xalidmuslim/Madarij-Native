package ru.madarij.nativeapp

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.madarij.nativeapp.data.*
import java.util.Date

@Composable
internal fun HomeScreen(
    vm: BookViewModel,
    open: (String, String?) -> Unit,
    navigate: (String) -> Unit,
    reminder: KnowledgeReminder,
    reducedMotion: Boolean,
    inkFinished: Boolean,
    onInkFinished: () -> Unit,
    onOpenReminder: (KnowledgeReminder) -> Unit
) {
    val chapters by vm.chapters.collectAsStateWithLifecycle()
    val read by vm.read.collectAsStateWithLifecycle()
    val last by vm.last.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val bookmarks by vm.bookmarks.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val groups = rememberStructure()
    val context = LocalContext.current
    val learning = remember(context) { LearningRepository(context.applicationContext) }
    val attempts by learning.attempts.collectAsStateWithLifecycle(emptyMap())
    LaunchedEffect(learning) { runCatching { learning.load() } }
    val due = attempts.count { it.value.due <= System.currentTimeMillis() }
    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).format(Date())
    val minutes = history.filter { it.date == today }.sumOf { it.seconds } / 60
    // Volume 2 is only a preview. Do not make it distort volume 1 reading progress.
    val mainVolume = chapters.filter { it.volume == 1 }
    val percent = if (mainVolume.isEmpty()) 0 else read.count { item -> mainVolume.any { it.id == item.chapterId } } * 100 / mainVolume.size
    val lastTitle = chapters.find { it.id == last?.chapterId }?.let(groups::findTitle)

    // BookRouteSurface paints the aged-paper sheet once beneath this screen.
    // A solid beige HomeScreen layer previously concealed that book texture.
    Box(Modifier.fillMaxSize()) {
        PreloadedBookImage(R.drawable.library_hero,
            Modifier.align(Alignment.TopCenter).fillMaxWidth().height(358.dp),
            contentScale = ContentScale.Crop, alignment = Alignment.Center)
        LazyColumn(
            // Reserve the real floating-bar footprint so dashboard metrics
            // never render underneath its opaque leather surface.
            modifier = Modifier.fillMaxSize().padding(
                bottom = WindowInsets.navigationBars.asPaddingValues()
                    .calculateBottomPadding() + 68.dp
            ),
            contentPadding = PaddingValues(start = 0.dp, end = 0.dp, bottom = 16.dp),
            // Leave room for the last metric above the floating navigation on phones.
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Box(Modifier.fillMaxWidth().height(258.dp)) {
                    Row(Modifier.fillMaxWidth().statusBarsPadding()
                        .padding(start = 22.dp, end = 12.dp, top = 16.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("Степени\nидущих",
                                style = MaterialTheme.typography.headlineLarge.copy(fontFamily = BookSerif,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
                                    fontSize = 46.sp, lineHeight = 43.sp),
                                color = Color(0xFFFFEDD0))
                            Row(Modifier.widthIn(max = 210.dp).padding(top = 9.dp, bottom = 7.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                HorizontalDivider(Modifier.weight(1f), thickness = .6.dp,
                                    color = Color(0xFFC79C64).copy(alpha = .65f))
                                Text("❖", Modifier.padding(horizontal = 6.dp),
                                    fontFamily = BookSerif, fontSize = 11.sp,
                                    color = Color(0xFFEAC28B))
                                HorizontalDivider(Modifier.weight(1f), thickness = .6.dp,
                                    color = Color(0xFFC79C64).copy(alpha = .65f))
                            }
                            Text("Ибн аль-Каййим\nаль-Джаузийя",
                                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = BookSerif,
                                    fontSize = 18.sp, lineHeight = 22.sp),
                                color = Color(0xFFEFC489))
                        }
                        Box(Modifier.size(width = 80.dp, height = 42.dp)
                            .vintageFrame(VintageFrameKind.FONT_SETTINGS)
                            .clickable { navigate("settings") }
                            .semantics { contentDescription = "Настройки чтения" },
                            contentAlignment = Alignment.Center) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Aa", fontFamily = BookSerif, fontSize = 17.sp,
                                    color = Color(0xFF533A27))
                                androidx.compose.foundation.Canvas(
                                    Modifier.size(width = 20.dp, height = 18.dp)
                                ) {
                                    val ink = Color(0xFF725034)
                                    repeat(3) { n ->
                                        val y = size.height * (.22f + n * .28f)
                                        drawLine(ink, androidx.compose.ui.geometry.Offset(0f, y),
                                            androidx.compose.ui.geometry.Offset(size.width, y),
                                            strokeWidth = 1.35.dp.toPx())
                                        drawCircle(ink, radius = 2.dp.toPx(),
                                            center = androidx.compose.ui.geometry.Offset(
                                                size.width * (if (n == 1) .72f else .34f), y
                                            ))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item(key = "knowledge-reminder") {
                Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp)
                    .vintageFrame(VintageFrameKind.LIGHT_WIDE)) {
                    Row(Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f).clickable {
                            onOpenReminder(reminder)
                        }) {
                            ReedPenWritingText(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 12.dp),
                                text = reminder.text,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = FontFamily(Font(R.font.bad_script)),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = CompactUi.homeQuoteSizeSp.sp,
                                    lineHeight = CompactUi.homeQuoteLineHeightSp.sp,
                                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                        includeFontPadding = true
                                    )
                                ),
                                color = Color(0xFF3E281A),
                                reducedMotion = reducedMotion,
                                animate = !inkFinished,
                                onFinished = onInkFinished
                            )
                            Text("Из книги · том ${reminder.volume}, стр. ${reminder.page}", Modifier.padding(top = 6.dp),
                                style = MaterialTheme.typography.labelSmall, color = BookColors.muted)
                        }
                        IconButton(onClick = { navigate("bookmarks") },
                            modifier = Modifier.size(30.dp)) {
                            NavigationGlyph("bookmarks", selected = true,
                                modifier = Modifier.size(18.dp), tint = BookColors.gold)
                        }
                    }
                }
            }
            item {
                Box(Modifier.fillMaxWidth().padding(horizontal=14.dp)
                    .heightIn(min=54.dp).vintageFrame(VintageFrameKind.DARK_WIDE)
                    .clickable(enabled=chapters.isNotEmpty()) {
                        (last?.chapterId ?: chapters.firstOrNull()?.id)?.let { open(it,null) }
                    }) {
                    Row(Modifier.fillMaxWidth().padding(horizontal=22.dp,vertical=13.dp),
                        verticalAlignment=Alignment.CenterVertically) {
                        NavigationGlyph("contents",selected=true,modifier=Modifier.size(22.dp),
                            tint=BookColors.lightGold)
                        Spacer(Modifier.width(11.dp))
                        Text("Продолжить чтение", Modifier.weight(1f),
                            style=MaterialTheme.typography.titleSmall.copy(
                                fontFamily=BookSerif,fontSize=17.sp),color=Color(0xFFF8E7CE))
                        Text("›",style=MaterialTheme.typography.headlineSmall,color=Color(0xFFF8E7CE))
                    }
                }
            }
            item {
                Text("Быстрый доступ", Modifier.padding(start = 16.dp, top = 4.dp, bottom = 2.dp),
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = BookSerif, fontSize = 24.sp, lineHeight = 29.sp),
                    color = BookColors.ink)
            }
            item {
                Row(Modifier.padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomeAction("Оглавление", "Главы и темы", "contents", Modifier.weight(1f)) { navigate("contents") }
                    HomeAction("Поиск", "По двум томам", "search", Modifier.weight(1f)) { navigate("search") }
                }
            }
            item {
                Row(Modifier.padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomeAction("Изучение", "Проверки и задания", "study", Modifier.weight(1f)) { navigate("study") }
                    HomeAction("Словарь", "Термины и контекст", "glossary", Modifier.weight(1f)) { navigate("glossary") }
                }
            }
            item {
                Row(Modifier.padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomeAction("Закладки", "Сохранено · ${bookmarks.size}", "bookmarks", Modifier.weight(1f)) { navigate("bookmarks") }
                    HomeAction("Заметки", "Личные записи · ${notes.size}", "notes", Modifier.weight(1f)) { navigate("notes") }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    listOf(
                        "${read.size}" to "Прочитано",
                        "$minutes мин" to "Сегодня",
                        "$due" to "Повторить"
                    ).forEachIndexed { index, (number, label) ->
                        if (index > 0) VerticalDivider(Modifier.height(37.dp),
                            color = BookColors.gold.copy(alpha = .28f))
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(number, fontFamily = BookSerif, fontSize = 19.sp, color = BookColors.gold)
                            Text(label, style = MaterialTheme.typography.labelSmall, color = BookColors.muted)
                        }
                    }
                }
            }
            if (chapters.isEmpty()) item { InfoCard("Подготовка книги", status) }
        }
    }

}

@Composable
private fun AntiqueHomeIcon(glyph: String) {
    // The user's generated brass-and-leather icon, not doubled vector outlines.
    AntiqueBookIcon(glyph, Modifier.size(31.dp))
}

@Composable
private fun HomeAction(title: String, subtitle: String, glyph: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Soft3DPanel(
        modifier = modifier.heightIn(min = 62.dp),
        shape = RoundedCornerShape(AppRadius.medium),
        elevation = AppElevation.level3,
        warm = true,
        onClick = onClick
    ) {
        // Soft3DPanel is a BoxScope; Center anchors the ENTIRE icon/text/arrow
        // row inside each aged-paper card instead of placing it at the top.
        Row(
            Modifier.align(Alignment.Center).fillMaxWidth()
                .padding(horizontal = 9.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AntiqueHomeIcon(glyph)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = BookSerif, fontSize = 15.sp, lineHeight = 19.sp),
                    color = BookColors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 13.sp),
                    color = BookColors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("›", fontFamily = BookSerif, fontSize = 20.sp,
                color = BookColors.gold.copy(alpha = .86f))
        }
    }
}

// Printed-page labels are derived from navigation metadata, never from
// position in the Compose list. Page ranges in the book remain stable.
private val tocPageIdRegex = Regex("""-p(\d{3})(?:-|$)""")
private val tocRangeRegex = Regex("""(?i)страниц[аы]?\s*(\d+)\s*[–-]\s*(\d+)""")
private fun tocPage(id: String?): Int? =
    id?.let { tocPageIdRegex.find(it)?.groupValues?.get(1)?.toIntOrNull() }
private fun tocRange(text: String): Pair<Int, Int>? =
    tocRangeRegex.find(text)?.let { it.groupValues[1].toInt() to it.groupValues[2].toInt() }
private fun tocPageLabel(start: Int?, end: Int?): String? =
    start?.let { "стр. " + it + (if (end != null && end > it) "–$end" else "") }
private fun chapterPageLabel(chapter: TocChapter): String? {
    val pages = tocRange(chapter.subtitle)
    return tocPageLabel(tocPage(chapter.entryParagraphId ?: chapter.chapterId) ?: pages?.first, pages?.second)
}
private fun groupPageLabel(group: TocGroup): String? {
    val advertised = tocRange(group.title)
    val first = group.items.firstOrNull()
    val last = group.items.lastOrNull()
    return tocPageLabel(
        tocPage(first?.entryParagraphId ?: first?.chapterId) ?: tocRange(first?.subtitle.orEmpty())?.first ?: advertised?.first,
        tocRange(last?.subtitle.orEmpty())?.second ?: advertised?.second
    )
}
private fun groupDisplayTitle(group: TocGroup): String =
    if (group.title.startsWith("Том 2 · страницы") || group.title.startsWith("Второй том · страницы"))
        group.items.firstOrNull()?.title ?: group.title
    else group.title

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ContentsScreen(vm: BookViewModel, open: (String, String?) -> Unit, reducedMotion: Boolean = false, resume: (String) -> Unit) {
    val chapters by vm.chapters.collectAsStateWithLifecycle()
    val read by vm.read.collectAsStateWithLifecycle()
    val bookmarks by vm.bookmarks.collectAsStateWithLifecycle()
    val last by vm.last.collectAsStateWithLifecycle()
    val positions by vm.positions.collectAsStateWithLifecycle()
    val groups = rememberStructure()
    val scope = rememberCoroutineScope()
    var savedIndex by rememberSaveable { mutableIntStateOf(0) }
    var savedOffset by rememberSaveable { mutableIntStateOf(0) }
    // Initialize the list at its saved position before the first layout.
    // An effect-driven scrollToItem previously made the TOC visibly jump.
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = savedIndex,
        initialFirstVisibleItemScrollOffset = savedOffset
    )
    var restored by remember { mutableStateOf(false) }
    var initialized by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var toolsVisible by rememberSaveable { mutableStateOf(true) }
    var filter by rememberSaveable { mutableStateOf("all") }
    var selectedVolume by rememberSaveable { mutableIntStateOf(1) }
    val availableVolumes = remember(chapters) { chapters.map { it.volume }.distinct().sorted() }
    var openedGroups by rememberSaveable { mutableStateOf(listOf<String>()) }
    var openedChapters by rememberSaveable { mutableStateOf(listOf<String>()) }
    var percents by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    val readIds = remember(read) { read.map { it.chapterId }.toSet() }
    val savedIds = remember(bookmarks) { bookmarks.map { it.chapterId }.toSet() }

    LaunchedEffect(positions, read) {
        percents = withContext(Dispatchers.IO) {
            val result = mutableMapOf<String, Int>()
            positions.forEach { p ->
                val paragraphs = vm.repository.dao.paragraphs(p.chapterId)
                val ordinal = paragraphs.indexOfFirst { it.id == p.paragraphId }
                result[p.chapterId] = if (paragraphs.isEmpty()) 0 else
                    ((ordinal + 1) * 100 / paragraphs.size).coerceIn(0, 99)
            }
            read.forEach { result[it.chapterId] = 100 }
            result
        }
    }
    LaunchedEffect(groups, last?.chapterId) {
        if (groups.isNotEmpty() && !initialized) {
            // Match the compact reference list: initially show only semantic
            // chapter cards. Expanding a card reveals its fragments and topics.
            // The "Continue reading" card above retains the last position.
            openedGroups = emptyList()
            openedChapters = emptyList()
            initialized = true
        }
        if (groups.isNotEmpty() && !restored) restored = true
    }
    LaunchedEffect(restored) {
        if (restored) snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) -> savedIndex = index; savedOffset = offset }
    }
    fun resetList() {
        savedIndex = 0
        savedOffset = 0
        // Schedule the new index in the same remeasure as tab/filter changes.
        listState.requestScrollToItem(0)
    }

    val fallback = if (groups.isNotEmpty()) groups else listOf(TocGroup("volume1", "Первый том", "editorial_navigation", chapters.map { TocChapter(it.id, it.title, "", emptyList()) }))
    val normalizedQuery = normalizedText(query).text
    val selectedChapterIds = remember(chapters, selectedVolume) { chapters.filter { it.volume == selectedVolume }.map { it.id }.toSet() }
    val matching = fallback.map { group ->
        group.copy(items = group.items.filter { chapter ->
            chapter.chapterId in selectedChapterIds && (query.isBlank() || normalizedText("${chapter.title} ${chapter.subtitle} ${chapter.topics.joinToString { it.title }}").text.contains(normalizedQuery)) && when (filter) {
                "read" -> chapter.chapterId in readIds
                "saved" -> chapter.chapterId in savedIds
                else -> true
            }
        })
    }.filter { it.items.isNotEmpty() }

    if (!restored || groups.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Открываем оглавление…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(
            bottom = WindowInsets.navigationBars.asPaddingValues()
                .calculateBottomPadding() + 68.dp
        ),
        contentPadding = PaddingValues(start = 17.dp,end = 17.dp,top = 9.dp,bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(CompactUi.listGapDp.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                availableVolumes.forEach { volume ->
                    HeritageTocVolumeTab("Том $volume", volume==selectedVolume,
                        Modifier.weight(1f)) {
                        selectedVolume=volume
                        openedGroups=emptyList()
                        openedChapters=emptyList()
                        resetList()
                    }
                }
            }
        }
        item {
            val sections=LogicalReadingSections.roots(chapters)
                .count { it.volume==selectedVolume }
            Text("Том $selectedVolume · $sections смысловых разделов · " +
                    "${selectedChapterIds.size} фрагментов",
                style=MaterialTheme.typography.bodySmall,color=heritageMuted())
            if(selectedVolume==2) Text(
                "Тематические группы страниц. Сверка издания и научная проверка источников не завершены.",
                style=MaterialTheme.typography.bodySmall,color=heritageMuted())
        }
        if (last != null && last!!.chapterId in selectedChapterIds) {
            item {
                AgedPaperCard(onClick={resume(last!!.chapterId)},
                    modifier=Modifier.fillMaxWidth(),
                    border=BorderStroke(.8.dp,BookColors.gold.copy(alpha=.3f)),
                    colors=CardDefaults.cardColors(containerColor=heritageCard().copy(alpha=.86f))) {
                    Row(Modifier.fillMaxWidth().padding(horizontal=13.dp,vertical=8.dp),
                        verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                            Text("Продолжить с места остановки",
                                style=MaterialTheme.typography.titleSmall.copy(fontFamily=BookSerif),
                                color=heritageInk())
                            Text(groups.flatMap { it.items }
                                .find { it.chapterId==last!!.chapterId }?.title
                                    ?: "Последнее место чтения",
                                style=MaterialTheme.typography.bodyMedium,
                                color=heritageMuted(),maxLines=2,
                                overflow=TextOverflow.Ellipsis)
                        }
                        Text("›", style=MaterialTheme.typography.headlineSmall,
                            color=BookColors.gold)
                    }
                }
            }
        }
        item {
            VintageSearchField(
                value=query,
                onValueChange={query=it;resetList()},
                modifier=Modifier.fillMaxWidth(),
                placeholder="Найти главу или тему"
            )
        }
        item {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                listOf("all" to "Все","read" to "Прочитано","saved" to "С закладками")
                    .forEach { (key,label) ->
                        HeritageTocFilterTab(
                            label, key, filter==key,
                            Modifier.weight(when (key) {
                                "all" -> .82f
                                "read" -> 1.08f
                                else -> 1.30f
                            })
                        ) {
                            filter=key
                            resetList()
                        }
                    }
            }
        }
        item {
            HeritageSectionHeading("Разделы книги",
                trailing="${matching.sumOf { it.items.size }} фрагментов")
        }
        if (matching.isEmpty()) item { InfoCard("Ничего не найдено", "Измените запрос или фильтр.") }
        matching.forEach { group ->
            item(key = "group:${group.id}") {
                val expanded = group.id in openedGroups || query.isNotBlank()
                val count = group.items.count { it.chapterId in readIds }
                AgedPaperCard(
                    onClick = { openedGroups = if (group.id in openedGroups) openedGroups - group.id else openedGroups + group.id },
                    colors = CardDefaults.cardColors(containerColor = heritageCard().copy(alpha = .83f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                    border = BorderStroke(.8.dp, BookColors.gold.copy(alpha = .34f)),
                    shape = RoundedCornerShape(19.dp),
                    frameKind = VintageFrameKind.SECTION_NUMBERED,
                    modifier = Modifier.fillMaxWidth().animateItem(
                        fadeInSpec = tween(if (reducedMotion) 0 else 140),
                        placementSpec = tween(if (reducedMotion) 0 else 175),
                        fadeOutSpec = tween(if (reducedMotion) 0 else 105)
                    )
                ) {
                    Row(Modifier.fillMaxWidth().heightIn(min=CompactUi.contentsGroupMinHeightDp.dp)
                        .padding(horizontal=11.dp,vertical=8.dp),
                        horizontalArrangement=Arrangement.spacedBy(10.dp),
                        verticalAlignment=Alignment.CenterVertically) {
                        HeritageManuscriptNumber(
                            matching.indexOfFirst { it.id == group.id } + 1
                        )
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                            Text(groupDisplayTitle(group),
                                style=MaterialTheme.typography.titleMedium.copy(
                                    fontFamily=BookSerif,fontSize=17.sp,lineHeight=21.sp),
                                color=heritageInk())
                            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                                Text("$count из ${group.items.size} прочитано",
                                    style=MaterialTheme.typography.labelSmall,color=heritageMuted())
                                groupPageLabel(group)?.let { page ->
                                    Text("· $page",style=MaterialTheme.typography.labelSmall,
                                        color=heritageMuted())
                                }
                            }
                        }
                        Text(if (expanded) "⌄" else "›", fontSize = 22.sp,
                            color = BookColors.gold, modifier = Modifier.padding(end = 4.dp))
                    }
                }
            }
            if (group.id in openedGroups || query.isNotBlank()) group.items.forEach { chapter ->
                item(key = "chapter:${chapter.chapterId}") {
                    val expanded = chapter.chapterId in openedChapters || query.isNotBlank()
                    val readingMembers = LogicalReadingSections.members(chapters, chapter.chapterId)
                    val sectionStartId = readingMembers.firstOrNull()?.id ?: chapter.chapterId
                    val isPageContinuation = sectionStartId != chapter.chapterId && chapter.entryParagraphId == null
                    val ordinal = LogicalReadingSections.roots(chapters).indexOfFirst { it.id == sectionStartId } + 1
                    val light = true
                    val ink = if (light) BookColors.ink else BookColors.text
                    val shape = RoundedCornerShape(17.dp)
                    Box(Modifier.fillMaxWidth().animateItem(
                        fadeInSpec = tween(if (reducedMotion) 0 else 140),
                        placementSpec = tween(if (reducedMotion) 0 else 175),
                        fadeOutSpec = tween(if (reducedMotion) 0 else 105)
                    ).vintageFrame(VintageFrameKind.LIGHT_WIDE)) {
                        Column {
                            Row(Modifier.fillMaxWidth().heightIn(min = 62.dp).padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.size(30.dp).clip(androidx.compose.foundation.shape.CircleShape).background(if(light) Color(0xFF9D713E) else BookColors.gold.copy(alpha = .22f)), contentAlignment = Alignment.Center) {
                                    Text(if (isPageContinuation) "↳" else "$ordinal",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = BookSerif),
                                        color = Color(0xFFFFF3DC))
                                }
                                Column(Modifier.weight(1f).clickable { if (chapter.topics.isNotEmpty()) openedChapters = if(expanded) openedChapters - chapter.chapterId else openedChapters + chapter.chapterId else open(chapter.chapterId, chapter.entryParagraphId) }) {
                                    if (isPageContinuation) Text("ПРОДОЛЖЕНИЕ ТЕКСТА", style = MaterialTheme.typography.labelSmall, color = BookColors.muted)
                                    Text(chapter.title, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = BookSerif, fontSize = 15.sp, lineHeight = 19.sp), color = ink)
                                    if(chapter.topics.isNotEmpty()) Text("${if(expanded) "⌄" else "›"} Содержание · ${chapter.topics.size}", style = MaterialTheme.typography.labelSmall, color = ink.copy(alpha = .7f))
                                }
                                TextButton(onClick = { open(chapter.chapterId, chapter.entryParagraphId) }, modifier = Modifier.semantics { contentDescription = "Читать" }) { Text("Читать", style = MaterialTheme.typography.labelMedium, color = BookColors.gold) }
                            }
                            chapterPageLabel(chapter)?.let { page ->
                                Text(page, modifier = Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 5.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                    style = MaterialTheme.typography.labelSmall, color = BookColors.muted)
                            }
                            val percent = percents[chapter.chapterId] ?: 0
                            if(percent > 0) LinearProgressIndicator(progress = {percent / 100f}, modifier = Modifier.fillMaxWidth().height(1.dp), color = BookColors.gold, trackColor = Color.Transparent)
                        }
                    }
                }

                if (chapter.chapterId in openedChapters || query.isNotBlank()) items(chapter.topics, key = { "topic:${chapter.chapterId}:${it.paragraphId}:${it.title}" }) { topic ->
                    AgedPaperCard(
                        onClick = { open(chapter.chapterId, topic.paragraphId) },
                        modifier = Modifier.fillMaxWidth().animateItem(
                            fadeInSpec = tween(if (reducedMotion) 0 else 140),
                            placementSpec = tween(if (reducedMotion) 0 else 175),
                            fadeOutSpec = tween(if (reducedMotion) 0 else 105)
                        ).padding(start = 14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .94f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(Modifier.padding(horizontal = 11.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(if (topic.provenance == "source_heading") "ТЕМА КНИГИ" else "ТЕМА", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(topic.title, style = MaterialTheme.typography.bodyMedium)
                            tocPage(topic.paragraphId)?.let { page ->
                                Text("стр. $page", modifier = Modifier.fillMaxWidth(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                    style = MaterialTheme.typography.labelSmall, color = BookColors.muted)
                            }
                        }
                    }
                }
            }
        }
    }
}
