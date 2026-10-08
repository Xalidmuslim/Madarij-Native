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
import ru.madarij.nativeapp.data.*
import java.util.Date

@Composable
internal fun HomeScreen(vm: BookViewModel, open: (String, String?) -> Unit, navigate: (String) -> Unit) {
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

    var excerpt by remember { mutableStateOf("") }
    LaunchedEffect(chapters.firstOrNull()?.id) {
        chapters.firstOrNull()?.id?.let { id ->
            val text = vm.repository.dao.paragraphs(id).firstOrNull {
                it.role !in listOf("editor_note", "edition_note") && it.ru.length > 80
            }?.ru.orEmpty()
            val sentences = Regex("[^.!?]+[.!?]").findAll(text).map { it.value.trim() }.take(2).toList()
            excerpt = sentences.getOrNull(1) ?: sentences.firstOrNull() ?: text
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val viewport = maxHeight
        val pageHeight = viewport.coerceAtLeast(600.dp) * maxOf(1f, androidx.compose.ui.platform.LocalDensity.current.fontScale / 1.15f)
        Image(painterResource(R.drawable.reference_home), null, Modifier.matchParentSize(), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, BookColors.background.copy(alpha = .15f), BookColors.background.copy(alpha = .65f)))))
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(Modifier.fillMaxWidth().height(pageHeight).statusBarsPadding().padding(top = 28.dp, bottom = 14.dp)) {
                Box(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth(.88f)) {
                        Text("Степени\nидущих", style = MaterialTheme.typography.headlineMedium.copy(fontFamily = BookSerif, fontWeight = androidx.compose.ui.text.font.FontWeight.Normal, fontSize = 48.sp, lineHeight = 48.sp), color = BookColors.text)
                        Text("Ибн аль-Каййим\nаль-Джаузийя", Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyLarge.copy(fontFamily = BookSerif, fontSize = 20.sp, lineHeight = 25.sp), color = BookColors.lightGold)
                    }
                    IconButton(onClick = { navigate("settings") }, modifier = Modifier.align(Alignment.TopEnd)) { SettingsWheel(Modifier.size(23.dp)) }
                }
                Spacer(Modifier.weight(1f).heightIn(min = 140.dp))
                if (excerpt.isNotBlank()) {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(BookColors.card.copy(alpha = .94f)).then(Modifier.border(1.dp, BookColors.gold.copy(alpha = .65f), RoundedCornerShape(20.dp)))) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("“", style = TextStyle(fontFamily = BookSerif, fontSize = 42.sp), color = BookColors.gold.copy(alpha = .7f))
                            Column(Modifier.weight(1f)) {
                                Text(excerpt, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = BookSerif, fontSize = 16.sp, lineHeight = 22.sp), color = BookColors.text)
                                Text("Ибн аль-Каййим", Modifier.fillMaxWidth().padding(top = 8.dp), style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = BookColors.muted)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Button(onClick = { (last?.chapterId ?: chapters.firstOrNull()?.id)?.let { open(it, null) } }, enabled = chapters.isNotEmpty(), modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp), shape = RoundedCornerShape(22.dp), colors = ButtonDefaults.buttonColors(containerColor = BookColors.lightGold, contentColor = BookColors.ink), elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)) {
                    NavigationGlyph("contents", selected = true, modifier = Modifier.size(28.dp), tint = BookColors.ink)
                    Spacer(Modifier.width(16.dp))
                    Text("Продолжить чтение", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    Text("›", style = MaterialTheme.typography.headlineSmall)
                }
            }
        }

        item { Text("Быстрый доступ", style = MaterialTheme.typography.titleMedium) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HomeAction("Содержание", "Главы и темы", "contents", Modifier.weight(1f)) { navigate("contents") }
                HomeAction("Поиск", "По всему тому", "search", Modifier.weight(1f)) { navigate("search") }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HomeAction("Изучение", "Проверки и задания", "study", Modifier.weight(1f)) { navigate("study") }
                HomeAction("Словарь", "Термины и контекст", "glossary", Modifier.weight(1f)) { navigate("glossary") }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HomeAction("Закладки", "Сохранено · ${bookmarks.size}", "bookmarks", Modifier.weight(1f)) { navigate("bookmarks") }
                HomeAction("Заметки", "Личные записи · ${notes.size}", "notes", Modifier.weight(1f)) { navigate("notes") }
            }
        }

        item {
            Soft3DPanel(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppRadius.medium),
                elevation = AppElevation.level2,
                warm = true
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${read.size}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Text("Прочитано", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    VerticalDivider(Modifier.height(38.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = .35f))
                    Column(Modifier.weight(1f).padding(start = 16.dp)) {
                        Text("$minutes мин", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Text("Сегодня", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    VerticalDivider(Modifier.height(38.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = .35f))
                    Column(Modifier.weight(1f).padding(start = 16.dp)) {
                        Text("$due", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Text("Повторить", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            Text(
                "Литературная сверка первого тома завершена · проверка источников отдельно",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp)
            )
        }
        if (chapters.isEmpty()) item { InfoCard("Подготовка книги", status) }
    }
    }
}

@Composable
private fun HomeAction(title: String, subtitle: String, glyph: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Soft3DPanel(
        modifier = modifier.heightIn(min = 96.dp),
        shape = RoundedCornerShape(AppRadius.medium),
        elevation = AppElevation.level3,
        warm = true,
        onClick = onClick
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppActionGlyph(glyph)
            Spacer(Modifier.height(2.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
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
    var toolsVisible by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf("all") }
    var openedGroups by rememberSaveable { mutableStateOf(listOf<String>()) }
    var openedChapters by rememberSaveable { mutableStateOf(listOf<String>()) }
    var percents by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    val readIds = remember(read) { read.map { it.chapterId }.toSet() }
    val savedIds = remember(bookmarks) { bookmarks.map { it.chapterId }.toSet() }

    LaunchedEffect(positions, read) {
        val result = mutableMapOf<String, Int>()
        positions.forEach { p ->
            val paragraphs = vm.repository.dao.paragraphs(p.chapterId)
            val ordinal = paragraphs.indexOfFirst { it.id == p.paragraphId }
            result[p.chapterId] = if (paragraphs.isEmpty()) 0 else ((ordinal + 1) * 100 / paragraphs.size).coerceIn(0, 99)
        }
        read.forEach { result[it.chapterId] = 100 }
        percents = result
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

    LazyColumn(state = listState, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(BookColors.secondaryBackground).padding(4.dp)) {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(BookColors.lightGold).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Text("Том 1", style = MaterialTheme.typography.labelLarge, color = BookColors.ink)
                    }
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
                Card(
                    onClick = { openedGroups = if (group.id in openedGroups) openedGroups - group.id else openedGroups + group.id },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .90f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                    shape = RoundedCornerShape(AppRadius.medium),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (expanded) "⌄" else "›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(group.title, style = MaterialTheme.typography.labelMedium, color = BookColors.muted)
                            
                        }
                    }
                }
            }
            if (group.id in openedGroups || query.isNotBlank()) group.items.forEach { chapter ->
                item(key = "chapter:${chapter.chapterId}") {
                    val expanded = chapter.chapterId in openedChapters || query.isNotBlank()
                    val ordinal = chapters.indexOfFirst { it.id == chapter.chapterId } + 1
                    val light = ordinal != 1
                    val ink = if (light) BookColors.ink else BookColors.text
                    val shape = RoundedCornerShape(17.dp)
                    Box(Modifier.fillMaxWidth().clip(shape).background(if(light) BookColors.parchment else BookColors.card).border(.7.dp, BookColors.lightGold.copy(alpha = .35f), shape)) {
                        Image(painterResource(chapterArt(ordinal)), null, Modifier.matchParentSize(), contentScale = ContentScale.Crop, alignment = Alignment.CenterEnd)
                        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf((if(light) BookColors.parchment else BookColors.card).copy(alpha = .96f), (if(light) BookColors.parchment else BookColors.card).copy(alpha = .82f), Color.Transparent))))
                        Column {
                            Row(Modifier.fillMaxWidth().heightIn(min = 70.dp).padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.size(33.dp).clip(androidx.compose.foundation.shape.CircleShape).background(if(light) Color(0xFF9D713E) else BookColors.gold.copy(alpha = .22f)), contentAlignment = Alignment.Center) {
                                    Text("$ordinal", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = BookSerif), color = BookColors.text)
                                }
                                Column(Modifier.weight(1f).clickable { if (chapter.topics.isNotEmpty()) openedChapters = if(expanded) openedChapters - chapter.chapterId else openedChapters + chapter.chapterId else open(chapter.chapterId,chapter.entryParagraphId) }) {
                                    Text(chapter.title, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = BookSerif, fontSize = 16.sp, lineHeight = 20.sp), color = ink)
                                    if(chapter.topics.isNotEmpty()) Text("${if(expanded) "⌄" else "›"} Содержание · ${chapter.topics.size}", style = MaterialTheme.typography.labelSmall, color = ink.copy(alpha = .7f))
                                }
                                IconButton(onClick = { open(chapter.chapterId, chapter.entryParagraphId) }, modifier = Modifier.semantics { contentDescription = "Читать" }) { Text("›", style = MaterialTheme.typography.headlineSmall, color = ink) }
                            }
                            val percent = percents[chapter.chapterId] ?: 0
                            if(percent > 0) LinearProgressIndicator(progress = {percent / 100f}, modifier = Modifier.fillMaxWidth().height(1.dp), color = BookColors.gold, trackColor = Color.Transparent)
                        }
                    }
                }

                if (chapter.chapterId in openedChapters || query.isNotBlank()) items(chapter.topics, key = { "topic:${chapter.chapterId}:${it.paragraphId}:${it.title}" }) { topic ->
                    Card(
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
