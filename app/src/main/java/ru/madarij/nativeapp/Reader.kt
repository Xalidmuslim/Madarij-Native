package ru.madarij.nativeapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import ru.madarij.nativeapp.data.*
import ru.madarij.nativeapp.data.Paragraph
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun Reader(
    vm: BookViewModel, chapter: Chapter, settings: ReadingSettings, anchor: String? = null, query: String = "", forceTop: Boolean = false,
    onStudy: (String) -> Unit = {}, onOpenParagraph: (String) -> Unit = {}, onBack: () -> Unit = {}, navigate: (String) -> Unit
) {
    var resizing by remember { mutableStateOf(false) }
    var draftFontSize by remember { mutableFloatStateOf(settings.russianSize) }
    val readerSettings = settings.copy(russianSize = draftFontSize)
    LaunchedEffect(settings.russianSize) { if (!resizing) draftFontSize = settings.russianSize }
    var chapterPanel by rememberSaveable(chapter.id) { mutableStateOf(false) }
    var chapterTab by rememberSaveable(chapter.id) { mutableIntStateOf(0) }
    val context = LocalContext.current
    val actionScope = rememberCoroutineScope()
    // A chapter transition must never inherit the previous chapter's end-of-list
    // position. Otherwise the 'Отметить прочитанным' footer flashes briefly.
    val listState = remember(chapter.id) { androidx.compose.foundation.lazy.LazyListState() }
    val chapters by vm.chapters.collectAsStateWithLifecycle()
    val read by vm.read.collectAsStateWithLifecycle()
    val bookmarks by vm.bookmarks.collectAsStateWithLifecycle()
    val notesFlow = remember(chapter.id) { vm.repository.dao.notes(chapter.id) }
    val notes by notesFlow.collectAsStateWithLifecycle(emptyList())
    val groups = rememberStructure()
    val chapterGroup = groups.find { g -> g.items.any { it.chapterId == chapter.id } }
    val chapterStructure = chapterGroup?.items?.find { it.chapterId == chapter.id }
    val notesByParagraph = remember(notes) { notes.groupBy { it.paragraphId } }
    val learning = remember { LearningRepository(context.applicationContext) }
    val termsByParagraph = remember(learning) { learning.terms.groupBy { it.sourceParagraphId } }
    var paragraphs by remember(chapter.id) { mutableStateOf<List<Paragraph>>(emptyList()) }
    var loaded by remember(chapter.id) { mutableStateOf(false) }
    var ready by remember(chapter.id) { mutableStateOf(false) }
    var anchorConsumed by rememberSaveable(chapter.id, anchor) { mutableStateOf(false) }
    var matchRevealed by rememberSaveable(chapter.id, anchor, query) { mutableStateOf(false) }
    var loadingError by remember(chapter.id) { mutableStateOf("") }
    var retry by remember(chapter.id) { mutableIntStateOf(0) }
    var panel by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var actionsMode by rememberSaveable { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Paragraph?>(null) }
    var noteDialog by remember { mutableStateOf(false) }
    var noteAnchor by remember { mutableStateOf<String?>(null) }
    var noteText by remember { mutableStateOf("") }
    var bookmarkDialog by remember { mutableStateOf(false) }
    var bookmarkAnchor by remember { mutableStateOf<String?>(null) }
    var bookmarkTitle by remember { mutableStateOf("") }
    var bookmarkNote by remember { mutableStateOf("") }
    var sourceDialog by remember { mutableStateOf(false) }
    var termDialog by remember { mutableStateOf<StudyTerm?>(null) }
    val snackbar = remember { SnackbarHostState() }
    var message by remember { mutableStateOf("") }
    val activeSince = remember(chapter.id) { longArrayOf(0L) }
    val visible = remember(paragraphs, settings.showNotes, anchor) { paragraphs.filter { settings.showNotes || it.id == anchor || it.role !in listOf("editor_note", "edition_note") } }
    val normalizedQuery = remember(query) { normalizedText(query).text }
    val displayRussian = remember(paragraphs, query) { if (query.isBlank()) buildReaderDisplayRussian(paragraphs) else paragraphs.associate { it.id to it.ru } }
    val currentParagraph by remember(visible) { derivedStateOf { if (visible.isEmpty()) null else visible[(listState.firstVisibleItemIndex - 1).coerceIn(0, visible.lastIndex)] } }
    fun savePosition() {
        if (ready) currentParagraph?.let { vm.savePosition(chapter.id, it.id, if (listState.firstVisibleItemIndex !in 1..visible.size) 0 else listState.firstVisibleItemScrollOffset) }
    }
    fun recordTime() {
        if (activeSince[0] > 0L) {
            val now = SystemClock.elapsedRealtime()
            val seconds = ((now - activeSince[0]) / 1000).toInt()
            if (seconds > 0 && ready) currentParagraph?.let { vm.recordReading(chapter.id, it.id, seconds) }
            activeSince[0] = now
        }
    }
    LaunchedEffect(chapter.id, retry) {
        try { paragraphs = vm.repository.dao.paragraphs(chapter.id); loaded = true; loadingError = "" }
        catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (_: Exception) { loadingError = "Не удалось открыть текст. Повторите загрузку." }
    }
    LaunchedEffect(chapter.id, loaded, anchor, forceTop) {
        if (loaded && visible.isNotEmpty()) {
            if (forceTop) {
                // Opening a section from the contents or by horizontal paging must show
                // its actual section heading instead of restoring an old paragraph offset.
                listState.scrollToItem(0, 0)
            } else {
                val old = vm.repository.dao.position(chapter.id)
                val target = if (anchor != null && !anchorConsumed) anchor else old?.paragraphId
                val found = visible.indexOfFirst { it.id == target }
                if (found >= 0) listState.scrollToItem(found + 1, if (target == anchor && !anchorConsumed) 0 else old?.offset ?: 0)
            }
            anchorConsumed = true
            ready = true
        }
    }
    LaunchedEffect(ready, visible) {
        if (ready) snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling -> if (!scrolling) savePosition() }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(chapter.id, ready, visible, lifecycle) {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) activeSince[0] = SystemClock.elapsedRealtime()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> activeSince[0] = SystemClock.elapsedRealtime()
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> { savePosition(); recordTime(); activeSince[0] = 0L }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { savePosition(); recordTime(); activeSince[0] = 0L; lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(chapter.id, ready) { if (ready) while (true) { delay(30_000); recordTime() } }
    LaunchedEffect(message) { if (message.isNotEmpty()) { snackbar.showSnackbar(message); message = "" } }
    fun textFor(p: Paragraph): String = (when (p.role) { "editor_note" -> "[Пояснение редакции приложения]\n"; "edition_note" -> "[Примечание издания]\n"; else -> "" }) + (if (settings.showArabic && p.ar.isNotBlank()) p.ar + "\n\n" else "") + p.ru
    fun fullChapter() = "Степени идущих — Мадаридж ас-саликин\nИбн аль-Каййим\nТом ${chapter.volume} · ${chapter.title}\nЛитературная сверка завершена · проверка источников отдельно\n\n" + paragraphs.joinToString("\n\n") { textFor(it) }
    fun copy(text: String) { (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText(chapter.title, text)); message = "Текст скопирован" }
    fun share(text: String) { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, chapter.title); putExtra(Intent.EXTRA_TEXT, text) }, "Поделиться текстом")) }
    fun newBookmark(paragraph: Paragraph?) { bookmarkAnchor = paragraph?.id; val existing=bookmarks.find {it.chapterId==chapter.id && it.paragraphId==paragraph?.id};bookmarkTitle = existing?.title ?: chapter.title; bookmarkNote = existing?.note.orEmpty(); bookmarkDialog = true }
    fun newNote(paragraph: Paragraph?) { noteAnchor = paragraph?.id; noteText = ""; noteDialog = true }
    val progress by remember(visible) { derivedStateOf { if (visible.isEmpty()) 0 else ((listState.firstVisibleItemIndex.toFloat() / visible.size) * 100).toInt().coerceIn(0, 100) } }
    val chapterIndex = chapters.indexOfFirst { it.id == chapter.id }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val backGestureInset = with(density) { 48.dp.toPx() }
    val swipeThreshold = with(density) { 56.dp.toPx() }
    val swipeLockDistance = with(density) { 9.dp.toPx() }
    val swipeModifier = Modifier.pointerInput(chapter.id, chapters, backGestureInset, swipeThreshold, swipeLockDistance) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var totalX = 0f
            var totalY = 0f
            var horizontalLocked = false
            var verticalLocked = false
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                val delta = change.position - change.previousPosition
                totalX += delta.x
                totalY += delta.y

                if (!horizontalLocked && !verticalLocked && (abs(totalX) > swipeLockDistance || abs(totalY) > swipeLockDistance)) {
                    val systemBackEdge = down.position.x <= backGestureInset && totalX > 0f
                    verticalLocked = abs(totalY) > abs(totalX) * 1.15f || systemBackEdge
                    horizontalLocked = !verticalLocked && abs(totalX) > abs(totalY) * 1.15f
                }
                if (horizontalLocked) change.consume()
            }

            if (horizontalLocked) {
                val direction = classifyReaderSwipe(
                    down.position.x, size.width.toFloat(), totalX, totalY,
                    backGestureInset, swipeThreshold
                )
                val targetIndex = when (direction) {
                    ReaderPageSwipe.NEXT -> chapterIndex + 1
                    ReaderPageSwipe.PREVIOUS -> chapterIndex - 1
                    null -> -1
                }
                chapters.getOrNull(targetIndex)?.let { target ->
                    savePosition()
                    recordTime()
                    navigate(target.id)
                }
            }
        }
    }
    Column(Modifier.fillMaxSize().background(Color.Transparent)) {
        MaterialTheme(colorScheme = MaterialTheme.colorScheme) {
        Box(Modifier.fillMaxWidth().background(Color.Transparent).statusBarsPadding()) {
            Box(Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReaderControl("‹") { onBack() }
                    Column(Modifier.weight(1f).clickable { chapterPanel = true },horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Том ${chapter.volume}",style = MaterialTheme.typography.bodySmall.copy(fontFamily = BookSerif),color = MaterialTheme.colorScheme.onSurface)
                        // Full editorial title belongs in the frontispiece / chapter
                        // contents. In this compact toolbar its ellipsis looked like
                        // a broken text transfer during scrolling.
                        Text("Раздел ${chapterIndex + 1}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = BookSerif, fontWeight = FontWeight.Normal),
                            color = MaterialTheme.colorScheme.onSurface)

                    }
                    IconButton(onClick = { newBookmark(currentParagraph) }) {
                        NavigationGlyph("bookmarks", selected = true, modifier = Modifier.size(22.dp))
                    }
                    TextButton(onClick = { panel = true }, modifier = Modifier.width(42.dp),
                        contentPadding = PaddingValues(0.dp)) {
                        Text("Aa", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                    }
                    Box {
                        ReaderControl("⋯") { menu = true }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem({ Text("Настройки чтения") }, { panel = true; menu = false })
                            DropdownMenuItem({ Text("Содержание раздела") }, { chapterPanel = true; menu = false })
                            DropdownMenuItem({ Text("Закладка главы") }, { newBookmark(null); menu = false })
                            DropdownMenuItem({ Text("Заметка к главе") }, { newNote(null); menu = false })
                            DropdownMenuItem({ Text("Повторить раздел через 3 дня") }, {
                                menu = false
                                actionScope.launch {
                                    try { learning.scheduleChapterReview(chapter.id); message = "Раздел добавлен на повторение через 3 дня" }
                                    catch (e: kotlinx.coroutines.CancellationException) { throw e }
                                    catch (_: Exception) { message = "Не удалось назначить повторение. Повторите действие." }
                                }
                            })
                            DropdownMenuItem({ Text(if (actionsMode) "Скрыть действия с абзацами" else "Действия с абзацами") }, { actionsMode = !actionsMode; menu = false })
                            DropdownMenuItem({ Text("Скопировать главу целиком") }, { copy(fullChapter()); menu = false }, enabled = loaded)
                            DropdownMenuItem({ Text("Поделиться главой") }, { share(fullChapter()); menu = false }, enabled = loaded)
                            DropdownMenuItem({ Text("Источник главы") }, { sourceDialog = true; menu = false })
                        }
                    }
                }
            }
        }
        }
        if (actionsMode) Text("Режим абзацев: закладки, заметки и копирование", Modifier.padding(horizontal = 20.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().height(2.dp), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.primary.copy(alpha = .14f))
        if (loadingError.isNotEmpty()) { InfoCard("Текст недоступен", loadingError); TextButton(onClick = { retry++ }) { Text("Повторить") } }
        // The floating bottom bar overlays the paper image, but must NOT cover
        // actual reading text. Reserve its 62dp height, its outer 4dp gap, the
        // system gesture inset and a small breathing gap in the reader viewport.
        // The paper texture still extends behind the floating navigation.
        val readerBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Box(
            Modifier.weight(1f).fillMaxWidth().background(Color.Transparent)
                .padding(bottom = readerBottomInset + 76.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().then(swipeModifier),
                contentPadding = PaddingValues(top = 8.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
            item(key = "heading") {
                Column(Modifier.fillMaxWidth()) {
                    ReaderFrontispiece(chapterIndex + 1, chapterStructure?.title ?: chapter.title, chapterStructure?.subtitle, settings.theme == "dark")
                    if(query.isNotBlank()) Text("Найденный текст: «$query»",Modifier.padding(horizontal = 24.dp,vertical = 8.dp),style = MaterialTheme.typography.labelMedium,color = MaterialTheme.colorScheme.primary)
                }
            }
            items(visible, key = { it.id }) { p ->
                val terms = termsByParagraph[p.id].orEmpty()
                val isNote = p.role in listOf("editor_note", "edition_note")
                var expanded by rememberSaveable(p.id) { mutableStateOf(anchor == p.id && isNote) }
                Column(Modifier.fillMaxWidth(settings.textWidth).padding(horizontal = if (settings.textWidth < .85f) 16.dp else 28.dp).combinedClickable(onClick = {}, onLongClick = { selected = p }), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Editorial TOC headings belong to "Содержание раздела", not
                    // to the author's continuous prose. Source section markers
                    // are rendered from the real book text by splitReaderText().
                    if (isNote) {
                        Soft3DPanel(
                            modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                            elevation = AppElevation.level2,
                            warm = true,
                            onClick = { expanded = !expanded }
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text((if (p.role == "editor_note") "Пояснение редакции" else "Примечание издания") + (if (expanded) " · Скрыть" else " · Открыть"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                if (expanded) {
                                    if (settings.showArabic && p.ar.isNotBlank()) SelectionContainer { ArabicText(p.ar, settings) }
                                    SelectionContainer { SearchAnchorText(highlight(p.ru, query, MaterialTheme.colorScheme.primary),p.ru,query,readerStyle(readerSettings),ready && p.id==anchor && !matchRevealed) {matchRevealed=true} }
                                } else {
                                    Text("Развёрнутое объяснение и дополнительные детали", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    } else {
                        val displayRu = displayRussian[p.id].orEmpty()
                        val arabicMatch = p.id == anchor && normalizedQuery.isNotBlank() && normalizedText(p.ar).text.contains(normalizedQuery) && !normalizedText(displayRu).text.contains(normalizedQuery)
                        val arabicBlocks = remember(p.id, p.ar) { splitReaderText(p.ar, "فصل", "فصل") }
                        val russianBlocks = remember(p.id, displayRu) { splitReaderText(displayRu, "Раздел", "Раздел") }
                        if ((settings.showArabic || arabicMatch) && p.ar.isNotBlank()) SelectionContainer {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                arabicBlocks.forEach { block ->
                                    when (block) {
                                        is ReaderTextBlock.Section -> ReaderSectionCard(block, settings, arabic = true)
                                        is ReaderTextBlock.Body -> readerParagraphs(block.text).forEach { paragraph ->
                                            if (arabicMatch) SearchAnchorText(highlight(paragraph, query, MaterialTheme.colorScheme.primary), paragraph, query,
                                                androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(if(settings.arabicFont=="alternate") R.font.arabic_naskh_alt else R.font.naskh)),fontSize=settings.arabicSize.sp,lineHeight=(settings.arabicSize*1.85f).sp,textDirection=androidx.compose.ui.text.style.TextDirection.Rtl,textAlign=androidx.compose.ui.text.style.TextAlign.Right),
                                                ready && !matchRevealed) { matchRevealed = true }
                                            else ArabicText(paragraph, settings)
                                        }
                                    }
                                }
                            }
                        }
                        SelectionContainer {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                russianBlocks.forEach { block ->
                                    when (block) {
                                        is ReaderTextBlock.Section -> ReaderSectionCard(block, settings, arabic = false)
                                        is ReaderTextBlock.Body -> ReaderRichTextBlock(
                                            text = block.text,
                                            query = query,
                                            settings = readerSettings,
                                            terms = terms,
                                            reveal = ready && p.id == anchor && !matchRevealed && !arabicMatch,
                                            onRevealed = { matchRevealed = true },
                                            onOpenTerm = { termDialog = it }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    notesByParagraph[p.id].orEmpty().forEach { note ->
                        AgedPaperCard(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { Text("Моя заметка", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary); Text(note.text, style = MaterialTheme.typography.bodyMedium) } }
                    }
                    if (actionsMode) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = { newBookmark(p) }) { Text(if (bookmarks.any { it.paragraphId == p.id }) "Закладка ✓" else "Закладка") }
                        TextButton(onClick = { newNote(p) }) { Text("Заметка") }
                        TextButton(onClick = { copy(textFor(p)) }) { Text("Копировать") }
                    }
                }
            }
            // The completion footer only exists after the new chapter's content
            // and scroll position have both been restored. Mark-read logic is kept.
            if (loaded && ready && visible.isNotEmpty()) item(key = "end") {
                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    HorizontalDivider()
                    Text("Конец раздела", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val isRead = read.any { it.chapterId == chapter.id }
                    Button(onClick = { if (isRead) vm.markUnread(chapter.id) else vm.markRead(chapter.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary, contentColor = MaterialTheme.colorScheme.onTertiary), shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp), elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)) { Text(if (isRead) "Прочитано ✓ · Снять отметку" else "Отметить прочитанным") }
                    OutlinedButton(onClick = { savePosition(); onStudy(chapter.id) }, modifier = Modifier.fillMaxWidth()) { Text("Проверить понимание") }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { TextButton(onClick = { newBookmark(null) }) { Text("Закладка") }; TextButton(onClick = { newNote(null) }) { Text("Заметка") }; TextButton(onClick = { copy(fullChapter()) }) { Text("Копировать") }; TextButton(onClick = { share(fullChapter()) }) { Text("Поделиться") } }
                    notes.filter { it.paragraphId == null }.forEach { Text("Моя заметка · ${it.text}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { savePosition(); recordTime(); chapters.getOrNull(chapterIndex - 1)?.let { navigate(it.id) } }, modifier = Modifier.weight(1f), enabled = chapterIndex > 0) { Text("← Предыдущий") }
                        OutlinedButton(onClick = { savePosition(); recordTime(); chapters.getOrNull(chapterIndex + 1)?.let { navigate(it.id) } }, modifier = Modifier.weight(1f), enabled = chapterIndex >= 0 && chapterIndex < chapters.lastIndex) { Text("Следующий →") }
                    }
                    if (chapterStructure?.topics?.isNotEmpty() == true) {
                        Text("Темы этого раздела", style = MaterialTheme.typography.titleSmall)
                        chapterStructure.topics.forEach { topic -> TextButton(onClick = { savePosition(); onOpenParagraph(topic.paragraphId) }, modifier = Modifier.fillMaxWidth()) { Text(topic.title, Modifier.fillMaxWidth(), maxLines = 2, overflow = TextOverflow.Ellipsis) } }
                    }
                }
            }
            }
        }
        SnackbarHost(snackbar)
    }
    if (chapterPanel) MaterialTheme(colorScheme = MaterialTheme.colorScheme) {
        androidx.activity.compose.BackHandler { chapterPanel = false }
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
            Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { }.statusBarsPadding().padding(horizontal = AppSpacing.lg)) {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),verticalAlignment = Alignment.CenterVertically) {
                    ReaderControl("‹") { chapterPanel = false }
                    Text(chapterStructure?.title ?: chapter.title, Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = BookSerif,
                            fontWeight = FontWeight.Normal),
                        color = MaterialTheme.colorScheme.onSurface, softWrap = true)
                    IconButton(onClick = { newBookmark(currentParagraph) }) { NavigationGlyph("bookmarks",selected = true,modifier = Modifier.size(21.dp)) }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    listOf("Содержание", "Закладки", "Заметки").forEachIndexed { index, label ->
                        FilterChip(chapterTab == index, { chapterTab = index }, { Text(label) })
                    }
                }
                fun revealParagraph(id: String?) {
                    actionScope.launch {
                        val index = visible.indexOfFirst { it.id == id }
                        // Same chapter, same LazyColumn: no duplicate reader or new back-stack entry.
                        if (id != null && index < 0) {
                            savePosition()
                            onOpenParagraph(id)
                        } else listState.scrollToItem(if (index >= 0) index + 1 else 0)
                        chapterPanel = false
                    }
                }
                LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    when (chapterTab) {
                        0 -> {
                            item { ChapterTopicRow("Начало раздела") { revealParagraph(null) } }
                            items(chapterStructure?.topics.orEmpty(), key = { it.paragraphId + it.title }) { topic ->
                                ChapterTopicRow(topic.title) { revealParagraph(topic.paragraphId) }
                            }
                            if (chapterStructure?.topics.isNullOrEmpty()) item { Text("В этом разделе нет отдельных подразделов.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        1 -> {
                            val own = bookmarks.filter { it.chapterId == chapter.id }
                            if (own.isEmpty()) item { Text("Закладок в этом разделе пока нет.") }
                            items(own, key = { it.id }) { bookmark ->
                                AgedPaperCard(onClick = { revealParagraph(bookmark.paragraphId) }, modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(AppSpacing.lg)) { Text(bookmark.title); if (bookmark.note.isNotBlank()) Text(bookmark.note) }
                                }
                            }
                        }
                        2 -> {
                            if (notes.isEmpty()) item { Text("Заметок в этом разделе пока нет.") }
                            items(notes, key = { it.id }) { note ->
                                AgedPaperCard(onClick = { revealParagraph(note.paragraphId) }, modifier = Modifier.fillMaxWidth()) {
                                    Text(note.text, Modifier.padding(AppSpacing.lg))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (panel) MaterialTheme(colorScheme = MaterialTheme.colorScheme) { ModalBottomSheet(onDismissRequest = { panel = false }, containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) { DarkSheetSystemBars(); Box(Modifier.fillMaxHeight(.9f)) { SettingsPanel(settings, vm::settings) } } }
    selected?.let { p -> AlertDialog(onDismissRequest = { selected = null }, title = { Text("Действия с абзацем") }, text = { Column { TextButton(onClick = { newBookmark(p); selected = null }) { Text("Добавить закладку") }; TextButton(onClick = { newNote(p); selected = null }) { Text("Написать заметку") }; TextButton(onClick = { copy(textFor(p)); selected = null }) { Text("Копировать абзац") }; TextButton(onClick = { share(textFor(p)); selected = null }) { Text("Поделиться абзацем") } } }, confirmButton = { TextButton(onClick = { selected = null }) { Text("Закрыть") } }) }
    if (noteDialog) AlertDialog(onDismissRequest = { noteDialog = false }, title = { Text(if (noteAnchor == null) "Заметка к разделу" else "Заметка к абзацу") }, text = { OutlinedTextField(noteText, { noteText = it }, label = { Text("Ваши мысли") }, modifier = Modifier.fillMaxWidth(), minLines = 5) }, confirmButton = { TextButton(onClick = { vm.addNote(chapter.id, noteAnchor, noteText); noteDialog = false; message = "Заметка сохранена" }, enabled = noteText.isNotBlank()) { Text("Сохранить") } }, dismissButton = { TextButton(onClick = { noteDialog = false }) { Text("Отмена") } })
    if (bookmarkDialog) AlertDialog(onDismissRequest = { bookmarkDialog = false }, title = { Text("Сохранить закладку") }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { OutlinedTextField(bookmarkTitle, { bookmarkTitle = it }, label = { Text("Название") }, modifier = Modifier.fillMaxWidth()); OutlinedTextField(bookmarkNote, { bookmarkNote = it }, label = { Text("Комментарий · необязательно") }, modifier = Modifier.fillMaxWidth(), minLines = 3) } }, confirmButton = { TextButton(onClick = { vm.bookmarkParagraph(chapter.id, bookmarkAnchor, bookmarkTitle, bookmarkNote); bookmarkDialog = false; message = "Закладка сохранена" }, enabled = bookmarkTitle.isNotBlank()) { Text("Сохранить") } }, dismissButton = { TextButton(onClick = { bookmarkDialog = false }) { Text("Отмена") } })
    if (sourceDialog) AlertDialog(onDismissRequest = { sourceDialog = false }, title = { Text("Источник раздела") }, text = { SelectionContainer { Text(chapter.source.ifBlank { "Источник требует редакционной сверки." }) } }, confirmButton = { TextButton(onClick = { sourceDialog = false }) { Text("Закрыть") } })
    termDialog?.let { term -> AlertDialog(onDismissRequest = { termDialog = null }, title = { Text(term.transcript) }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { ArabicText(term.ar, settings); Text(term.definition); Text("Пояснение редакции к этому фрагменту", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }, confirmButton = { TextButton(onClick = { val id = term.sourceParagraphId; termDialog = null; onOpenParagraph(id) }) { Text("Исходный контекст") } }, dismissButton = { TextButton(onClick = { termDialog = null }) { Text("Закрыть") } }) }
}

@Composable
private fun ReaderSectionCard(block: ReaderTextBlock.Section, settings: ReadingSettings, arabic: Boolean) {
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 5.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (arabic) ArabicText(block.label, settings, Modifier.heightIn(min = 28.dp))
        else Text(block.label.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        block.title?.let { title ->
            if (arabic) ArabicText(title, settings)
            else Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = russianFamily(settings),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun readerParagraphs(text: String): List<String> =
    text.split(Regex("\\r?\\n[ \\t]*\\r?\\n+"))
        .map { it.trim('\r', '\n', ' ') }
        .filter { it.isNotBlank() }

private val numberedLeadRegex = Regex(
    """(?im)^(?:(?:первая|вторая|третья|четвёртая|пятая|шестая|седьмая|восьмая|девятая|десятая|одиннадцатая|двенадцатая|первое|второе|третье|четвёртое|пятое|шестое|седьмое|восьмое|девятое|десятое|одиннадцатое|двенадцатое)(?:\s+[^\n:]{1,42})?:|во-(?:первых|вторых|третьих|четвёртых|пятых|шестых|седьмых|восьмых|девятых|десятых)[,.:]?|(?:\d{1,3}|[ivxlcdm]+)[.)])\s*"""
)

private val standaloneHeadingRegex = Regex(
    """(?i)^(?:глава|раздел|степень|ступень|смысл|польза|правило|основа|вопрос|ответ|положение|сторона)(?:\s+[^.!?…]{0,82})?$"""
)

private fun annotatedReaderText(text: String, query: String, color: androidx.compose.ui.graphics.Color, terms: List<StudyTerm>, open: (StudyTerm) -> Unit): AnnotatedString = buildAnnotatedString {
    append(highlight(text, query, color))

    numberedLeadRegex.findAll(text).forEach { match ->
        addStyle(
            SpanStyle(color = color, fontWeight = FontWeight.SemiBold),
            match.range.first, match.range.last + 1
        )
    }
    val trimmed = text.trim()
    if (!trimmed.contains('\n') && trimmed.length <= 100 && standaloneHeadingRegex.matches(trimmed)) {
        val start = text.indexOf(trimmed)
        if (start >= 0) addStyle(
            SpanStyle(fontWeight = FontWeight.SemiBold),
            start, start + trimmed.length
        )
    }

    terms.forEach { term ->
        val definitionLead = term.definition.substringBefore(';').substringBefore('.').substringBefore(',').trim()
        val transcriptBare = term.transcript.removePrefix("аль-").removePrefix("ал-").trim()
        val candidates = listOf(term.transcript.trim(), transcriptBare, definitionLead).filter { it.length >= 3 }.distinct()
        val hit = candidates.mapNotNull { candidate ->
            text.indexOf(candidate, ignoreCase = true).takeIf { it >= 0 }?.let { it to candidate }
        }.minByOrNull { it.first }
        if (hit != null) {
            val (first, label) = hit
            addLink(
                LinkAnnotation.Clickable(
                    "term:${term.transcript}",
                    TextLinkStyles(style = SpanStyle(color = color, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)),
                    linkInteractionListener = { open(term) }
                ),
                first, first + label.length
            )
        }
    }
}

private sealed interface ReaderRichSegment {
    data class Paragraph(val text: String) : ReaderRichSegment
    data class Quote(val label: String, val quote: String, val reference: String?) : ReaderRichSegment
    data class Numbered(val label: String, val body: String) : ReaderRichSegment
    data class Heading(val text: String) : ReaderRichSegment
    data class Poetry(val text: String) : ReaderRichSegment
}

@Composable
private fun ReaderRichTextBlock(
    text: String,
    query: String,
    settings: ReadingSettings,
    terms: List<StudyTerm>,
    reveal: Boolean,
    onRevealed: () -> Unit,
    onOpenTerm: (StudyTerm) -> Unit
) {
    val segments = remember(text) { parseReaderRichSegments(text) }
    val requester = remember { BringIntoViewRequester() }
    Column(
        modifier = Modifier.fillMaxWidth().bringIntoViewRequester(requester),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        segments.forEach { segment ->
            when (segment) {
                is ReaderRichSegment.Paragraph -> Text(
                    annotatedReaderText(segment.text, query, MaterialTheme.colorScheme.primary, terms, onOpenTerm),
                    style = readerStyle(settings),
                    modifier = Modifier.fillMaxWidth()
                )
                is ReaderRichSegment.Quote -> ReaderQuoteCard(segment, settings, query)
                is ReaderRichSegment.Numbered -> ReaderNumberedBlock(segment, settings, query)
                is ReaderRichSegment.Heading -> Text(
                    segment.text,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = russianFamily(settings),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                )
                is ReaderRichSegment.Poetry -> AgedPaperCard(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .46f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        segment.text,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        style = readerStyle(settings).copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    )
                }
            }
        }
    }
    LaunchedEffect(reveal) {
        if (reveal) {
            withFrameNanos { }
            requester.bringIntoView()
            onRevealed()
        }
    }
}

@Composable
private fun ReaderQuoteCard(segment: ReaderRichSegment.Quote, settings: ReadingSettings, query: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier.width(4.dp).fillMaxHeight().defaultMinSize(minHeight = 64.dp)
                .background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
        )
        AgedPaperCard(
            modifier = Modifier.weight(1f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .15f)),
            border = androidx.compose.foundation.BorderStroke(.7.dp,MaterialTheme.colorScheme.primary.copy(alpha = .3f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(segment.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(
                    highlight(segment.quote, query, MaterialTheme.colorScheme.primary),
                    style = readerStyle(settings).copy(fontWeight = FontWeight.Normal),
                    modifier = Modifier.fillMaxWidth()
                )
                if (!segment.reference.isNullOrBlank()) {
                    Text(segment.reference, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ReaderNumberedBlock(segment: ReaderRichSegment.Numbered, settings: ReadingSettings, query: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .34f)
        ) {
            Text(
                segment.label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            highlight(segment.body, query, MaterialTheme.colorScheme.primary),
            style = readerStyle(settings),
            modifier = Modifier.weight(1f)
        )
    }
}

private val quoteLeadRegex = Regex(
    """^(?<label>(?:Всевышний|Аллах|Пророк|Посланник Аллаха|Было сказано|Сказано|[А-ЯЁA-Z][^.!?]{0,120}?) сказал:)\s*[«"](?<quote>.+)[»"]\s*(?<ref>\[[^\]]+\])?[.!?…]*$"""
)
private val quranRefRegex = Regex("""\[[^\]]*\d{1,3}:\d{1,3}(?:[–-]\d{1,3})?[^\]]*]""")
private val numberedSentenceRegex = Regex(
    """^(?<lead>Первое|Второе|Третье|Четвёртое|Четвертое|Пятое|Шестое|Седьмое|Восьмое|Девятое|Десятое|Одиннадцатое|Двенадцатое):\s*(?<body>.+)$""",
    RegexOption.IGNORE_CASE
)

private fun parseReaderRichSegments(text: String): List<ReaderRichSegment> {
    val rawParagraphs = text.replace("\r", "")
        .split(Regex("""\n[ \t]*\n+"""))
        .map { it.trim() }
        .filter { it.isNotBlank() }
    if (rawParagraphs.isEmpty()) return emptyList()

    val result = mutableListOf<ReaderRichSegment>()
    rawParagraphs.forEach { raw ->
        if (raw.startsWith("«") && raw.contains('\n')) {
            result += ReaderRichSegment.Poetry(raw)
            return@forEach
        }
        if (raw.length <= 110 && standaloneHeadingRegex.matches(raw)) {
            result += ReaderRichSegment.Heading(raw)
            return@forEach
        }

        val flat = raw.replace(Regex("""[ \t]*\n[ \t]*"""), " ").replace(Regex("""[ \t]+"""), " ").trim()
        val sentences = Regex("""(?<=[.!?…»] |[.!?…»]\s)(?=[А-ЯЁ«])""")
            .split(flat)
            .flatMap { chunk -> Regex("""(?<=[.!?…»])\s+(?=[А-ЯЁ«])""").split(chunk) }
            .map { it.trim() }
            .filter { it.isNotBlank() }

        var index = 0
        while (index < sentences.size) {
            val sentence = sentences[index]
            val quoteMatch = quoteLeadRegex.matchEntire(sentence)
            if (quoteMatch != null) {
                result += ReaderRichSegment.Quote(
                    label = quoteMatch.groups["label"]!!.value.trim(),
                    quote = quoteMatch.groups["quote"]!!.value.trim(),
                    reference = quoteMatch.groups["ref"]?.value?.trim()
                )
                index++
                continue
            }

            if (quranRefRegex.containsMatchIn(sentence) && sentence.contains('«') && sentence.contains('»')) {
                val first = sentence.indexOf('«')
                val last = sentence.lastIndexOf('»')
                val reference = quranRefRegex.find(sentence)?.value
                if (first >= 0 && last > first) {
                    val prefix = sentence.substring(0, first).trim().trimEnd(':')
                    result += ReaderRichSegment.Quote(
                        label = prefix.takeIf { it.isNotBlank() } ?: "Аят",
                        quote = sentence.substring(first + 1, last).trim(),
                        reference = reference
                    )
                    index++
                    continue
                }
            }

            val numberMatch = numberedSentenceRegex.matchEntire(sentence)
            if (numberMatch != null) {
                result += ReaderRichSegment.Numbered(
                    numberMatch.groups["lead"]!!.value.trim(),
                    numberMatch.groups["body"]!!.value.trim()
                )
                index++
                continue
            }

            val paragraphParts = mutableListOf(sentence)
            var cursor = index + 1
            while (cursor < sentences.size) {
                val candidate = sentences[cursor]
                if (quoteLeadRegex.matchEntire(candidate) != null || numberedSentenceRegex.matchEntire(candidate) != null || (quranRefRegex.containsMatchIn(candidate) && candidate.contains('«') && candidate.contains('»'))) break
                paragraphParts += candidate
                cursor++
            }
            result += ReaderRichSegment.Paragraph(paragraphParts.joinToString(" "))
            index = cursor
        }
    }
    return result
}

private fun buildReaderDisplayRussian(paragraphs: List<Paragraph>): Map<String, String> {
    val adjusted = paragraphs.associate { it.id to it.ru }.toMutableMap()
    val authorIndexes = paragraphs.indices.filter { paragraphs[it].role !in listOf("editor_note", "edition_note") }
    for (position in 0 until authorIndexes.lastIndex) {
        val current = paragraphs[authorIndexes[position]]
        val next = paragraphs[authorIndexes[position + 1]]
        var left = adjusted[current.id].orEmpty().trimEnd()
        var right = adjusted[next.id].orEmpty().trimStart()
        if (!needsContinuation(left, right)) continue
        val match = Regex("""^(.+?[.!?…](?:[»”"])?)(?=\s|$)""", setOf(RegexOption.DOT_MATCHES_ALL)).find(right) ?: continue
        val prefix = match.groupValues[1].trim()
        left = (left + " " + prefix).replace(Regex("""[ \t]+"""), " ")
        right = right.substring(match.range.last + 1).trimStart()
        adjusted[current.id] = left
        adjusted[next.id] = right
    }
    return adjusted
}

private fun needsContinuation(left: String, right: String): Boolean {
    if (left.isBlank() || right.isBlank()) return false
    if (Regex("""[.!?…][»”"]?$""").containsMatchIn(left)) return false
    val first = right.firstOrNull { it.isLetter() } ?: return false
    return first.isLowerCase()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchAnchorText(text:AnnotatedString,raw:String,query:String,style:TextStyle,reveal:Boolean,onRevealed:()->Unit) {
    val requester=remember {BringIntoViewRequester()}
    var bounds by remember(raw,query) {mutableStateOf<Rect?>(null)}
    Text(text,modifier=Modifier.fillMaxWidth().bringIntoViewRequester(requester),style=style,onTextLayout={layout ->
        val needle=normalizedText(query).text
        if(needle.isNotBlank()) {
            val normalized=normalizedText(raw);val match=normalized.text.indexOf(needle)
            if(match>=0) {val box=layout.getBoundingBox(normalized.offsets[match]);bounds=Rect(0f,box.top,layout.size.width.toFloat(),box.bottom)}
        }
    })
    LaunchedEffect(reveal,bounds) {
        if(reveal && bounds!=null) {withFrameNanos {};requester.bringIntoView(bounds);onRevealed()}
    }
}
