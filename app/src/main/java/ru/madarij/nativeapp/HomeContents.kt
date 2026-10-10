package ru.madarij.nativeapp

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
    onInkFinished: () -> Unit
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
    val percent = if (chapters.isEmpty()) 0 else read.size * 100 / chapters.size
    val lastTitle = chapters.find { it.id == last?.chapterId }?.let(groups::findTitle)

    // BookRouteSurface paints the aged-paper sheet once beneath this screen.
    // A solid beige HomeScreen layer previously concealed that book texture.
    Box(Modifier.fillMaxSize()) {
        PreloadedBookImage(R.drawable.library_hero,
            Modifier.align(Alignment.TopCenter).fillMaxWidth().height(358.dp),
            contentScale = ContentScale.Crop, alignment = Alignment.Center)
        LazyColumn(
            contentPadding = PaddingValues(start = 0.dp, end = 0.dp, bottom = 116.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        TextButton(onClick = { navigate("settings") },
                            modifier = Modifier.size(42.dp),
                            contentPadding = PaddingValues(0.dp)) {
                            Text("Aa", color = Color(0xFFF1CC92), fontSize = 17.sp)
                        }
                    }
                }
            }
            item(key = "knowledge-reminder") {
                val shape = RoundedCornerShape(17.dp)
                Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp).clip(shape)
                    .background(BookColors.card)
                    .border(.65.dp, BookColors.gold.copy(alpha = .22f), shape)) {
                    PreloadedBookImage(R.drawable.card_paper, Modifier.matchParentSize(), ContentScale.FillBounds)
                    Row(Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f).clickable {
                            open(reminder.chapterId, reminder.paragraphId)
                        }) {
                            ReedPenWritingText(
                                text = reminder.text,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = FontFamily(Font(R.font.marck_script)),
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 22.sp, lineHeight = 31.sp
                                ),
                                color = Color(0xFF62452F),
                                reducedMotion = reducedMotion,
                                animate = !inkFinished,
                                onFinished = onInkFinished
                            )
                            Text("Из книги · том 1, стр. ${reminder.page}", Modifier.padding(top = 8.dp),
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
                Surface(
                    onClick = { (last?.chapterId ?: chapters.firstOrNull()?.id)?.let { open(it, null) } },
                    enabled = chapters.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(48.dp),
                    shape = RoundedCornerShape(15.dp),
                    color = BookColors.leather,
                    contentColor = Color(0xFFF8E7CE),
                    shadowElevation = 0.dp
                ) {
                    Box(Modifier.fillMaxSize()) {
                        PreloadedBookImage(R.drawable.cta_leather,
                            modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            NavigationGlyph("contents", selected = true, modifier = Modifier.size(23.dp),
                                tint = BookColors.lightGold)
                            Spacer(Modifier.width(11.dp))
                            Text("Продолжить чтение", Modifier.weight(1f),
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = BookSerif, fontSize = 17.sp),
                                color = Color(0xFFF8E7CE))
                            Text("›", style = MaterialTheme.typography.headlineSmall,
                                color = Color(0xFFF8E7CE))
                        }
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
                    HomeAction("Содержание", "Главы и темы", "contents", Modifier.weight(1f)) { navigate("contents") }
                    HomeAction("Поиск", "По всему тому", "search", Modifier.weight(1f)) { navigate("search") }
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
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp),
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
    AntiqueBookIcon(glyph, Modifier.size(35.dp))
}

@Composable
private fun HomeAction(title: String, subtitle: String, glyph: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Soft3DPanel(
        modifier = modifier.heightIn(min = 73.dp),
        shape = RoundedCornerShape(AppRadius.medium),
        elevation = AppElevation.level3,
        warm = true,
        onClick = onClick
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 9.dp),
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

@Composable
internal fun ContentsScreen(vm: BookViewModel, open: (String, String?) -> Unit, resume: (String) -> Unit) {
    val chapters by vm.chapters.collectAsStateWithLifecycle()
    val read by vm.read.collectAsStateWithLifecycle()
    val bookmarks by vm.bookmarks.collectAsStateWithLifecycle()
    val last by vm.last.collectAsStateWithLifecycle()
    val positions by vm.positions.collectAsStateWithLifecycle()
    val groups = rememberStructure()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var savedIndex by rememberSaveable { mutableIntStateOf(0) }
    var savedOffset by rememberSaveable { mutableIntStateOf(0) }
    var restored by remember { mutableStateOf(false) }
    var initialized by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var toolsVisible by rememberSaveable { mutableStateOf(true) }
    var filter by rememberSaveable { mutableStateOf("all") }
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
            openedGroups = listOf(groups.find { group -> group.items.any { it.chapterId == last?.chapterId } }?.id ?: groups.first().id)
            if (last != null) openedChapters = listOf(last!!.chapterId)
            initialized = true
        }
        if (groups.isNotEmpty() && !restored) {
            listState.scrollToItem(savedIndex, savedOffset)
            restored = true
        }
    }
    LaunchedEffect(restored) {
        if (restored) snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) -> savedIndex = index; savedOffset = offset }
    }
    fun resetList() { savedIndex = 0; savedOffset = 0; scope.launch { listState.scrollToItem(0) } }

    val fallback = if (groups.isNotEmpty()) groups else listOf(TocGroup("volume1", "Первый том", "editorial_navigation", chapters.map { TocChapter(it.id, it.title, "", emptyList()) }))
    val normalizedQuery = normalizedText(query).text
    val matching = fallback.map { group ->
        group.copy(items = group.items.filter { chapter ->
            (query.isBlank() || normalizedText("${chapter.title} ${chapter.subtitle} ${chapter.topics.joinToString { it.title }}").text.contains(normalizedQuery)) && when (filter) {
                "read" -> chapter.chapterId in readIds
                "saved" -> chapter.chapterId in savedIds
                else -> true
            }
        })
    }.filter { it.items.isNotEmpty() }

    LazyColumn(state = listState, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 124.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // Single parchment label: the previous two stacked Boxes
                // formed an unwanted double contour around 'Том 1'.
                Box(
                    Modifier.weight(1f).heightIn(min = 43.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(BookColors.card)
                        .border(.7.dp, BookColors.gold.copy(alpha = .30f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    PreloadedBookImage(R.drawable.card_paper, Modifier.matchParentSize(), ContentScale.FillBounds)
                    Text("Том 1", style = MaterialTheme.typography.labelLarge.copy(fontFamily = BookSerif),
                        color = BookColors.ink)
                }
                IconButton(onClick = { toolsVisible = !toolsVisible }) { NavigationGlyph("search", selected = toolsVisible, modifier = Modifier.size(21.dp)) }
            }
        }
        if (toolsVisible) {
        item { Text("Том 1 · ${chapters.size} разделов · ${read.size} прочитано", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        last?.let { position ->
            item {
                Soft3DSagePanel(
                    onClick = { resume(position.chapterId) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppRadius.medium),
                    elevation = AppElevation.level2
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Продолжить с места остановки", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(groups.find { it.items.any { c -> c.chapterId == position.chapterId } }?.items?.find { it.chapterId == position.chapterId }?.title ?: "Последний раздел", style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text("→", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
        item { OutlinedTextField(
            query,
            { query = it; resetList() },
            label = { Text("Найти главу или тему") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(AppRadius.medium),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = .72f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = .54f),
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = .48f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = .32f)
            ),
            trailingIcon = { if (query.isNotEmpty()) TextButton(onClick = { query = ""; resetList() }) { Text("Очистить") } }
        ) }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("all" to "Все", "read" to "Прочитано", "saved" to "С закладками").forEach { (key, label) ->
                    FilterChip(
                        selected = filter == key,
                        onClick = { filter = key; resetList() },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { openedGroups = matching.map { it.id }; openedChapters = matching.flatMap { it.items }.map { it.chapterId } }, contentPadding = PaddingValues(horizontal = 6.dp)) { Text("Раскрыть всё") }
                TextButton(onClick = { openedGroups = emptyList(); openedChapters = emptyList() }, contentPadding = PaddingValues(horizontal = 6.dp)) { Text("Свернуть всё") }
            }
        }
        }
        if (matching.isEmpty()) item { InfoCard("Ничего не найдено", "Измените запрос или фильтр.") }
        matching.forEach { group ->
            item(key = "group:${group.id}") {
                val expanded = group.id in openedGroups || query.isNotBlank()
                val count = group.items.count { it.chapterId in readIds }
                AgedPaperCard(
                    onClick = { openedGroups = if (group.id in openedGroups) openedGroups - group.id else openedGroups + group.id },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .90f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                    shape = RoundedCornerShape(AppRadius.medium),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (expanded) "⌄" else "›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(group.title, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = BookSerif), color = BookColors.ink)
                            Text("$count из ${group.items.size} прочитано", style = MaterialTheme.typography.labelSmall, color = BookColors.muted)
                            
                        }
                    }
                }
            }
            if (group.id in openedGroups || query.isNotBlank()) group.items.forEach { chapter ->
                item(key = "chapter:${chapter.chapterId}") {
                    val expanded = chapter.chapterId in openedChapters || query.isNotBlank()
                    val ordinal = chapters.indexOfFirst { it.id == chapter.chapterId } + 1
                    val light = true
                    val ink = if (light) BookColors.ink else BookColors.text
                    val shape = RoundedCornerShape(17.dp)
                    Box(Modifier.fillMaxWidth().clip(shape).background(if(light) BookColors.parchment else BookColors.card).border(.7.dp, BookColors.lightGold.copy(alpha = .35f), shape)) {
                        PreloadedBookImage(R.drawable.card_paper, Modifier.matchParentSize(), ContentScale.FillBounds)
                        Column {
                            Row(Modifier.fillMaxWidth().heightIn(min = 70.dp).padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.size(33.dp).clip(androidx.compose.foundation.shape.CircleShape).background(if(light) Color(0xFF9D713E) else BookColors.gold.copy(alpha = .22f)), contentAlignment = Alignment.Center) {
                                    Text("$ordinal", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = BookSerif), color = Color(0xFFFFF3DC))
                                }
                                Column(Modifier.weight(1f).clickable { if (chapter.topics.isNotEmpty()) openedChapters = if(expanded) openedChapters - chapter.chapterId else openedChapters + chapter.chapterId else open(chapter.chapterId,chapter.entryParagraphId) }) {
                                    Text(chapter.title, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = BookSerif, fontSize = 16.sp, lineHeight = 20.sp), color = ink)
                                    if(chapter.topics.isNotEmpty()) Text("${if(expanded) "⌄" else "›"} Содержание · ${chapter.topics.size}", style = MaterialTheme.typography.labelSmall, color = ink.copy(alpha = .7f))
                                }
                                TextButton(onClick = { open(chapter.chapterId, chapter.entryParagraphId) }, modifier = Modifier.semantics { contentDescription = "Читать" }) { Text("Читать", style = MaterialTheme.typography.labelMedium, color = BookColors.gold) }
                            }
                            val percent = percents[chapter.chapterId] ?: 0
                            if(percent > 0) LinearProgressIndicator(progress = {percent / 100f}, modifier = Modifier.fillMaxWidth().height(1.dp), color = BookColors.gold, trackColor = Color.Transparent)
                        }
                    }
                }

                if (chapter.chapterId in openedChapters || query.isNotBlank()) items(chapter.topics, key = { "topic:${chapter.chapterId}:${it.paragraphId}:${it.title}" }) { topic ->
                    AgedPaperCard(
                        onClick = { open(chapter.chapterId, topic.paragraphId) },
                        modifier = Modifier.fillMaxWidth().padding(start = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .94f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(Modifier.padding(horizontal = 13.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(if (topic.provenance == "source_heading") "ТЕМА КНИГИ" else "ТЕМА", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(topic.title, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        item { Text("Следующие тома ещё не включены.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)) }
    }
}
