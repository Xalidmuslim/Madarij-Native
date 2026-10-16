package ru.madarij.nativeapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    val chapters by vm.chapters.collectAsStateWithLifecycle()
    val members = remember(chapters, chapter.id) {
        LogicalReadingSections.members(chapters, chapter.id).ifEmpty { listOf(chapter) }
    }
    val memberIds = remember(members) { members.map { it.id } }
    val sectionKey = remember(memberIds) { memberIds.joinToString("|") }
    val sectionRoot = members.first()
    val sectionId = sectionRoot.id
    val logicalChapters = remember(chapters) { LogicalReadingSections.roots(chapters) }
    val chapterIndex = logicalChapters.indexOfFirst { it.id == sectionId }
    val listState = remember(sectionId) { androidx.compose.foundation.lazy.LazyListState() }
    val read by vm.read.collectAsStateWithLifecycle()
    val bookmarks by vm.bookmarks.collectAsStateWithLifecycle()
    val allNotes by vm.notes.collectAsStateWithLifecycle()
    val notes = remember(allNotes, memberIds) { allNotes.filter { it.chapterId in memberIds } }
    val groups = rememberStructure()
    val chapterGroup = groups.find { g -> g.items.any { it.chapterId == sectionId } }
    val chapterStructure = chapterGroup?.items?.find { it.chapterId == sectionId }
    val sectionTopics = remember(groups, memberIds) {
        groups.flatMap { it.items }.filter { it.chapterId in memberIds }.flatMap { it.topics }
    }
    // Editorial navigation maps topics to paragraph IDs, not individual words.
    // Show the mapped paragraph as an explicit landmark instead of silently
    // dropping the reader into the middle of a long logical section.
    val selectedTopic = remember(sectionTopics, anchor) {
        sectionTopics.firstOrNull { it.paragraphId == anchor }
    }
    val selectedTopicHeading = selectedTopic?.startText?.takeIf { query.isBlank() }
    val readerTitle = selectedTopic?.title?.takeIf { query.isBlank() }
        ?: chapterStructure?.title ?: sectionRoot.title
    val notesByParagraph = remember(notes) { notes.groupBy { it.paragraphId } }
    val learning = remember { LearningRepository(context.applicationContext) }
    val termsByParagraph = remember(learning) { learning.terms.groupBy { it.sourceParagraphId } }
    var paragraphs by remember(sectionKey) { mutableStateOf<List<Paragraph>>(emptyList()) }
    var loaded by remember(sectionKey) { mutableStateOf(false) }
    var ready by remember(sectionKey) { mutableStateOf(false) }
    var preparedRussian by remember(sectionKey) { mutableStateOf<Map<String, String>>(emptyMap()) }
    // A newly opened TOC link always gets its explicit target, even if this
    // destination was previously displayed and Compose saved another position.
    var anchorConsumed by remember(chapter.id, anchor) { mutableStateOf(false) }
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
    val displayRussian = remember(paragraphs, query, preparedRussian) {
        if (query.isBlank()) preparedRussian else paragraphs.associate { it.id to it.ru }
    }
    val currentParagraph by remember(visible) { derivedStateOf { if (visible.isEmpty()) null else visible[(listState.firstVisibleItemIndex - 1).coerceIn(0, visible.lastIndex)] } }
    // Keep the reader invisible while restoring the saved position AND locating a
    // topic inside a long printed-page paragraph. Otherwise the user sees the
    // first line before a second bringIntoView moves it out of sight.
    val exactTargetAvailable = query.isBlank() && anchor != null &&
        selectedTopicHeading != null &&
        displayRussian[anchor]?.contains(selectedTopicHeading, ignoreCase = true) == true
    var exactTargetSettled by remember(sectionKey, anchor) { mutableStateOf(false) }
    var viewportTopY by remember(sectionKey) { mutableFloatStateOf(Float.NaN) }
    var targetTopY by remember(sectionKey, anchor) { mutableFloatStateOf(Float.NaN) }
    val alignDensity = androidx.compose.ui.platform.LocalDensity.current
    // Locate the exact author-heading or editorial phrase once, then place it
    // against the TOP of the reading viewport (not merely "somewhere visible").
    LaunchedEffect(ready, exactTargetAvailable, viewportTopY, targetTopY) {
        if (ready && exactTargetAvailable && !exactTargetSettled &&
            viewportTopY.isFinite() && targetTopY.isFinite()) {
            val inset = with(alignDensity) { 8.dp.toPx() }
            listState.scrollBy(targetTopY - viewportTopY - inset)
            exactTargetSettled = true
        }
    }
    // Inconsistent legacy metadata must never leave the reading surface blank.
    LaunchedEffect(ready, exactTargetAvailable, sectionKey, anchor) {
        if (ready && exactTargetAvailable && !exactTargetSettled) {
            delay(190)
            exactTargetSettled = true
        }
    }
    val searchTargetAvailable = query.isNotBlank() && anchor != null &&
        visible.any { it.id == anchor }
    val readerSettled = ready &&
        (if (searchTargetAvailable) matchRevealed else (!exactTargetAvailable || exactTargetSettled))
    val readerAlpha by animateFloatAsState(
        targetValue = if (readerSettled) 1f else 0f,
        animationSpec = tween(durationMillis = if (settings.reducedMotion) 0 else 125),
        label = "reader-position-ready"
    )
    fun savePosition() {
        if (readerSettled) currentParagraph?.let { vm.savePosition(sectionId, it.id, if (listState.firstVisibleItemIndex !in 1..visible.size) 0 else listState.firstVisibleItemScrollOffset) }
    }
    fun recordTime() {
        if (activeSince[0] > 0L) {
            val now = SystemClock.elapsedRealtime()
            val seconds = ((now - activeSince[0]) / 1000).toInt()
            if (seconds > 0 && ready) currentParagraph?.let { vm.recordReading(sectionId, it.id, seconds) }
            activeSince[0] = now
        }
    }
    LaunchedEffect(sectionKey, retry) {
        try {
            // Read the chapter on IO; reconcile sentences across printed pages
            // on Default so the first Compose frame never performs this scan.
            val loadedParagraphs = withContext(Dispatchers.IO) {
                members.flatMap { vm.repository.dao.paragraphs(it.id) }
            }
            val reconciled = withContext(Dispatchers.Default) {
                buildReaderDisplayRussian(loadedParagraphs)
            }
            preparedRussian = reconciled
            paragraphs = loadedParagraphs
            loaded = true
            loadingError = ""
        }
        catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (_: Exception) { loadingError = "Не удалось открыть текст. Повторите загрузку." }
    }
    LaunchedEffect(chapter.id, loaded, anchor, forceTop) {
        if (loaded && visible.isNotEmpty()) {
            if (forceTop) {
                // Opening a section from the contents or by horizontal paging must show
                // its actual section heading instead of restoring an old paragraph offset.
                listState.scrollToItem(0, 0)
            } else if (anchor != null && !anchorConsumed) {
                // An explicitly chosen TOC topic/search hit wins over history.
                // Never wait for another DAO query before placing it.
                val found = visible.indexOfFirst { it.id == anchor }
                if (found >= 0) listState.scrollToItem(found + 1, 0)
            } else {
                val old = vm.repository.dao.position(chapter.id)
                    ?: vm.repository.dao.position(sectionId)
                val found = visible.indexOfFirst { it.id == old?.paragraphId }
                if (found >= 0) listState.scrollToItem(found + 1, old?.offset ?: 0)
            }
            anchorConsumed = true
            ready = true
        }
    }
    LaunchedEffect(readerSettled, visible) {
        if (readerSettled) snapshotFlow { listState.isScrollInProgress }
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
    fun fullChapter() = "Степени идущих — Мадаридж ас-саликин\nИбн аль-Каййим\nТом ${chapter.volume} · ${chapterStructure?.title ?: sectionRoot.title}\n" +
        (if (chapter.volume == 2) "Литературная сверка завершена · научная проверка источников не завершена" else "Литературная сверка завершена · проверка источников отдельно") +
        "\n\n" + paragraphs.joinToString("\n\n") { textFor(it) }
    fun copy(text: String) { (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText(chapter.title, text)); message = "Текст скопирован" }
    fun share(text: String) { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, chapter.title); putExtra(Intent.EXTRA_TEXT, text) }, "Поделиться текстом")) }
    fun newBookmark(paragraph: Paragraph?) { bookmarkAnchor = paragraph?.id; val existing=bookmarks.find {it.chapterId==(paragraph?.chapterId ?: sectionId) && it.paragraphId==paragraph?.id};bookmarkTitle = existing?.title ?: (chapterStructure?.title ?: sectionRoot.title); bookmarkNote = existing?.note.orEmpty(); bookmarkDialog = true }
    fun newNote(paragraph: Paragraph?) { noteAnchor = paragraph?.id; noteText = ""; noteDialog = true }
    val progress by remember(visible) { derivedStateOf { if (visible.isEmpty()) 0 else ((listState.firstVisibleItemIndex.toFloat() / visible.size) * 100).toInt().coerceIn(0, 100) } }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val backGestureInset = with(density) { 48.dp.toPx() }
    val swipeThreshold = with(density) { 56.dp.toPx() }
    val swipeLockDistance = with(density) { 9.dp.toPx() }
    val swipeModifier = Modifier.pointerInput(sectionId, logicalChapters, backGestureInset, swipeThreshold, swipeLockDistance) {
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
                logicalChapters.getOrNull(targetIndex)?.let { target ->
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
                        Text(readerTitle,
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
        BoxWithConstraints(
            Modifier.weight(1f).fillMaxWidth().background(Color.Transparent)
                .padding(bottom = readerBottomInset + 76.dp)
        ) {
            // A near-final topic must still have enough trailing scroll extent
            // to bring its first word to the top without a second animation.
            val topicBottomPadding = if (exactTargetAvailable)
                (maxHeight - 12.dp).coerceAtLeast(22.dp) else 22.dp
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
                    .onGloballyPositioned { viewportTopY = it.positionInWindow().y }
                    .graphicsLayer { alpha = readerAlpha }
                    .then(swipeModifier),
                contentPadding = PaddingValues(top = 8.dp, bottom = topicBottomPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
            item(key = "heading") {
                Column(Modifier.fillMaxWidth()) {
                    ReaderFrontispiece(logicalChapters.take(chapterIndex + 1).count { it.volume == chapter.volume },
                        readerTitle,
                        LogicalReadingSections.pageRange(sectionId), settings.theme == "dark")
                    if (chapter.volume == 2) Text(
                        "Том 2 · литературно-смысловая редактура завершена. Сверка со сканами издания 2019 года и научная проверка источников ещё не выполнены.",
                        Modifier.padding(horizontal = 24.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                    if (p.id == anchor && query.isBlank() && selectedTopic != null && selectedTopicHeading == null) {
                        // Use the same Russian text as the reader itself: the
                        // display layer can reconcile a sentence split across pages.
                        val shownOpeningText = displayRussian[p.id].orEmpty().ifBlank { p.ru }
                        val startWords = remember(p.id, shownOpeningText) {
                            shownOpeningText.replace(Regex("""\s+"""), " ").trim().take(96).trimEnd()
                        }
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .28f)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(11.dp)
                            ) {
                                Box(
                                    Modifier.width(3.dp).height(70.dp)
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp))
                                )
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("ВЫБРАННАЯ ТЕМА", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary)
                                    Text(selectedTopic.title, style = MaterialTheme.typography.titleMedium.copy(fontFamily = BookSerif),
                                        color = MaterialTheme.colorScheme.onSurface)
                                    if (startWords.isNotBlank()) Text(
                                        "Ориентир — начало абзаца: «" + startWords +
                                            (if (shownOpeningText.length > 96) "…" else "") + "»",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    if (isNote) {
                        Soft3DPanel(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            elevation = AppElevation.level2,
                            warm = true,
                            onClick = { expanded = !expanded }
                        ) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.width(3.dp).heightIn(min = 29.dp)
                                    .background(BookColors.gold.copy(alpha = .80f), RoundedCornerShape(3.dp)))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                        NavigationGlyph("notes", selected = false,
                                            modifier = Modifier.size(16.dp), tint = BookColors.gold)
                                        Text((if (p.role == "editor_note") "Пояснение редакции" else "Примечание издания") +
                                            (if (expanded) " · Скрыть" else " · Открыть"),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary)
                                    }
                                    if (expanded) {
                                        if (settings.showArabic && p.ar.isNotBlank()) SelectionContainer { ArabicText(p.ar, settings) }
                                        SelectionContainer { SearchAnchorText(
                                            highlight(p.ru, query, MaterialTheme.colorScheme.primary),
                                            p.ru, query, readerStyle(readerSettings),
                                            ready && p.id==anchor && !matchRevealed) {matchRevealed=true} }
                                    }

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
                                        is ReaderTextBlock.Section -> {
                                            // Some authorial headings start half-way through a
                                            // page-sized paragraph. For those TOC links the
                                            // actual section heading, not the page opening,
                                            // is the navigation target and visible landmark.
                                            val exactHeading = selectedTopicHeading?.let { phrase ->
                                                p.id == anchor && block.title?.contains(phrase, ignoreCase = true) == true
                                            } == true
                                            if (exactHeading) {
                                                Column(
                                                    Modifier.fillMaxWidth()
                                                        .onGloballyPositioned { targetTopY = it.positionInWindow().y },
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Surface(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = RoundedCornerShape(14.dp),
                                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .28f)
                                                    ) {
                                                        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                                            Text("НАЧАЛО ВЫБРАННОЙ ТЕМЫ",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.primary)
                                                            Text(selectedTopic?.title.orEmpty(),
                                                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = BookSerif),
                                                                color = MaterialTheme.colorScheme.onSurface)
                                                        }
                                                    }
                                                    ReaderSectionCard(block, settings, arabic = false)
                                                }
                                             } else ReaderSectionCard(block, settings, arabic = false)
                                        }
                                        is ReaderTextBlock.Body -> {
                                            // The editor's TOC may point to a new authorial
                                            // section *within* a printed page. Scrolling only
                                            // to that page wrongly shows the previous topic.
                                            // Split this DISPLAY block at the verified text
                                            // anchor; leave stored Arabic/Russian untouched.
                                            val exactOffset = if (p.id == anchor && query.isBlank() && selectedTopicHeading != null)
                                                block.text.indexOf(selectedTopicHeading, ignoreCase = true) else -1
                                            if (exactOffset >= 0) {
                                                val previousText = block.text.substring(0, exactOffset)
                                                val selectedText = block.text.substring(exactOffset)
                                                if (previousText.isNotBlank()) ReaderRichTextBlock(
                                                    text = previousText, query = query,
                                                    settings = readerSettings, terms = terms,
                                                    reveal = false, onRevealed = {},
                                                    onOpenTerm = { termDialog = it }
                                                )
                                                Surface(
                                                    modifier = Modifier.fillMaxWidth()
                                                        .onGloballyPositioned { targetTopY = it.positionInWindow().y },
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .28f)
                                                ) {
                                                    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                                        Text("НАЧАЛО ВЫБРАННОЙ ТЕМЫ",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.primary)
                                                        Text(selectedTopic?.title.orEmpty(),
                                                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = BookSerif),
                                                            color = MaterialTheme.colorScheme.onSurface)
                                                    }
                                                }
                                                 ReaderRichTextBlock(
                                                    text = selectedText, query = query,
                                                    settings = readerSettings, terms = terms,
                                                    reveal = false, onRevealed = {},
                                                    onOpenTerm = { termDialog = it }
                                                )
                                            } else ReaderRichTextBlock(
                                                text = block.text,
                                                query = query,
                                                settings = readerSettings,
                                                terms = terms,
                                                // Search results alone use the second
                                                // intra-paragraph bringIntoView pass.
                                                reveal = ready && query.isNotBlank() && p.id == anchor && !matchRevealed && !arabicMatch,
                                                onRevealed = { matchRevealed = true },
                                                onOpenTerm = { termDialog = it }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Warnings are explicitly editorial and never blended into the author's prose.
                    if (chapter.volume == 2) {
                        val sourceWarning = when (p.id) {
                            "madarij-v2-p005-06" ->
                                "Примечание редакции: сообщение о сглазе имеет различные хадисоведческие оценки; " +
                                "нельзя обозначать его как безусловно достоверное без независимой сверки."
                            "madarij-v2-p046-draft" ->
                                "Примечание редакции: выражение «Кто познал себя, тот познал своего Господа» " +
                                "сам автор прямо называет НЕ хадисом Посланника Аллаха."
                            "madarij-v2-p053-draft", "madarij-v2-p054-draft" ->
                                "Примечание редакции: длинное сообщение, приписываемое Пророку, " +
                                "требует проверки источника, текста и цепочки передачи. Достоверность пока не установлена."
                            "madarij-v2-p063-draft" ->
                                "Примечание редакции: приведённая передача о мучениках " +
                                "пока не прошла независимую хадисоведческую проверку."
                            "madarij-v2-p078-draft" ->
                                "Примечание редакции: слова о сорокакратном повторении мольбы " +
                                "переданы как наставление Ибн Таймийи, а не как установленная Сунна."
                            "madarij-v2-p082-draft", "madarij-v2-p083-draft", "madarij-v2-p095-draft" ->
                                "Примечание редакции: приведённые хадисы требуют самостоятельной сверки текста, " +
                                "источника и оценки достоверности."
                            "madarij-v2-p101-draft", "madarij-v2-p102-draft" ->
                                "Примечание редакции: высказывания и хадисы о верви Аллаха нужно " +
                                "сверить по первоисточникам и цепочкам передачи."
                            "madarij-v2-p139-draft" ->
                                "Примечание редакции: сообщение о разговоре шайтана с Господом " +
                                "передаётся в разных формах и пока не признано подтверждённым хадисом."
                            "madarij-v2-p147-draft" ->
                                "Примечание редакции: здесь изложена правовая аргументация автора. " +
                                "Её не следует представлять как единогласное мнение всех учёных."
                            "madarij-v2-p156-draft", "madarij-v2-p166-draft",
                            "madarij-v2-p193-draft", "madarij-v2-p197-draft",
                            "madarij-v2-p218-draft", "madarij-v2-p219-draft" ->
                                "Примечание редакции: в извлечённом арабском источнике " +
                                "есть повреждённые или обрывающиеся фрагменты коранических цитат. " +
                                "Русский текст необходимо дословно сверить с Кораном перед публикацией."
                            "madarij-v2-p158-draft", "madarij-v2-p171-draft",
                            "madarij-v2-p179-draft" ->
                                "Примечание редакции: оценки переданных сообщений и их цепочек " +
                                "пока не подтверждены независимой хадисоведческой проверкой."
                            "madarij-v2-p202-draft", "madarij-v2-p206-draft",
                            "madarij-v2-p208-draft" ->
                                "Примечание редакции: автор рассматривает разные мнения о молитве " +
                                "без сосредоточенности и в итоге предпочитает второе. " +
                                "Это не следует считать единогласным решением всех правовых школ."
                            "madarij-v2-p224-draft", "madarij-v2-p267-draft",
                            "madarij-v2-p277-draft", "madarij-v2-p278-draft" ->
                                "Примечание редакции: переданные религиозные сообщения " +
                                "нуждаются в независимой проверке первоисточников и степени достоверности."
                            "madarij-v2-p293-draft", "madarij-v2-p294-draft" ->
                                "Примечание редакции: здесь перечисляются спорные и отвергаемые автором " +
                                "правовые послабления, а не общепринятые дозволения."
                            "madarij-v2-p297-draft", "madarij-v2-p298-draft" ->
                                "Примечание редакции: в этом месте важны границы арабского аята " +
                                "и его грамматический разбор; требуется точная сверка с Кораном."
                            "madarij-v2-p319-draft" ->
                                "Примечание редакции: в извлечённой арабской транскрипции " +
                                "кораническая цитата 22:30 обрывается; до публикации сверить с Кораном."
                            "madarij-v2-p337-draft", "madarij-v2-p338-draft",
                            "madarij-v2-p339-draft" ->
                                "Примечание редакции: здесь изложена богословская позиция автора. " +
                                "Её толкования и утверждения о согласии ранних учёных требуют отдельной научной проверки."
                            "madarij-v2-p347-draft", "madarij-v2-p350-draft",
                            "madarij-v2-p382-draft", "madarij-v2-p388-draft" ->
                                "Примечание редакции: сообщения, их формулировки и цепочки передачи " +
                                "пока не проверены независимо по хадисным первоисточникам."
                            "madarij-v2-p411-draft", "madarij-v2-p413-draft",
                            "madarij-v2-p414-draft" ->
                                "Примечание редакции: обсуждается порицаемое попрошайничество; " +
                                "это не отменяет дозволенной помощи нуждающимся и прав на социальную поддержку."
                            "madarij-v2-p433-draft", "madarij-v2-p434-draft",
                            "madarij-v2-p459-draft", "madarij-v2-p476-draft" ->
                                "Примечание редакции: источник и достоверность приведённого " +
                                "изречения требуют отдельной хадисоведческой проверки."
                            "madarij-v2-p488-draft", "madarij-v2-p489-draft" ->
                                "Примечание редакции: отдельная арабская цитата из Корана " +
                                "в исходной транскрипции повреждена; сверить текст 89:27–30 по мусхафу."
                            "madarij-v2-p501-draft", "madarij-v2-p509-draft",
                            "madarij-v2-p510-draft" ->
                                "Примечание редакции: здесь важно различать религиозную волю " +
                                "Аллаха и всеобщее предопределение. Это изложение позиции автора."
                            "madarij-v2-p514-draft", "madarij-v2-p518-draft" ->
                                "Примечание редакции: историческое и богословское изложение " +
                                "не является самостоятельным современным правовым предписанием."
                            "madarij-v2-p545-draft", "madarij-v2-p546-draft",
                            "madarij-v2-p547-draft", "madarij-v2-p550-draft" ->
                                "Примечание редакции: приведённые назидательные предания и рассказы " +
                                "не прошли независимую проверку подлинности; не считайте их достоверными хадисами."
                            "madarij-v2-p553-draft", "madarij-v2-p554-draft",
                            "madarij-v2-p578-draft", "madarij-v2-p580-draft",
                            "madarij-v2-p588-draft" ->
                                "Примечание редакции: молитвы и религиозные сообщения ещё необходимо " +
                                "сверить с первоисточниками и оценками хадисоведов."
                            "madarij-v2-p566-draft", "madarij-v2-p568-draft",
                            "madarij-v2-p569-draft", "madarij-v2-p575-draft" ->
                                "Примечание редакции: осуждение необоснованных просьб не отменяет закят, " +
                                "дозволенную помощь нуждающимся и законно причитающиеся права."
                            "madarij-v2-p583-draft", "madarij-v2-p584-draft",
                            "madarij-v2-p607-draft" ->
                                "Примечание редакции: здесь разбирается спорная духовная терминология. " +
                                "Фана не объявляется автором обязательным путём для верующих."
                            "madarij-v2-p613-draft", "madarij-v2-p614-draft",
                            "madarij-v2-p616-draft", "madarij-v2-p617-draft" ->
                                "Примечание редакции: приведённые исторические и божественные высказывания " +
                                "не подтверждены независимой проверкой цепочек передачи."
                            "madarij-v2-p622-draft", "madarij-v2-p623-draft" ->
                                "Примечание редакции: отдельные арабские коранические цитаты " +
                                "в оцифрованном издании повреждены. Сверить по мусхафу и проверить хадисы."
                            "madarij-v2-p626-draft" ->
                                "Примечание редакции: сообщение о непрерывном возрастании благ Рая " +
                                "требует отдельной проверки первоисточника и оценки достоверности."
                            "madarij-v2-p638-draft", "madarij-v2-p639-draft" ->
                                "Примечание редакции: приводятся разные толкования аята 2:94, " +
                                "включая личное предпочтение автора. Это не утверждение единогласия."
                            "madarij-v2-p654-draft", "madarij-v2-p655-draft" ->
                                "Примечание редакции: неподтверждённые экстатические изречения " +
                                "упоминаются как предмет критического богословского обсуждения."
                            "madarij-v2-p657-draft", "madarij-v2-p658-draft",
                            "madarij-v2-p659-draft" ->
                                "Примечание редакции: это изложение позиции автора и нескольких " +
                                "толкований Корана 50:16; независимая религиозная проверка не завершена."
                            else -> null
                        }
                        if (sourceWarning != null) Text(
                            sourceWarning,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 3.dp)
                        )
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
                    val isRead = members.all { member -> read.any { it.chapterId == member.id } }
                    Button(onClick = { members.forEach { if (isRead) vm.markUnread(it.id) else vm.markRead(it.id) } }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary, contentColor = MaterialTheme.colorScheme.onTertiary), shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp), elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)) { Text(if (isRead) "Прочитано ✓ · Снять отметку" else "Отметить прочитанным") }
                    OutlinedButton(onClick = { savePosition(); onStudy(chapter.id) }, modifier = Modifier.fillMaxWidth()) { Text("Проверить понимание") }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { TextButton(onClick = { newBookmark(null) }) { Text("Закладка") }; TextButton(onClick = { newNote(null) }) { Text("Заметка") }; TextButton(onClick = { copy(fullChapter()) }) { Text("Копировать") }; TextButton(onClick = { share(fullChapter()) }) { Text("Поделиться") } }
                    notes.filter { it.paragraphId == null }.forEach { Text("Моя заметка · ${it.text}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { savePosition(); recordTime(); logicalChapters.getOrNull(chapterIndex - 1)?.let { navigate(it.id) } }, modifier = Modifier.weight(1f), enabled = chapterIndex > 0) { Text("← Предыдущий") }
                        OutlinedButton(onClick = { savePosition(); recordTime(); logicalChapters.getOrNull(chapterIndex + 1)?.let { navigate(it.id) } }, modifier = Modifier.weight(1f), enabled = chapterIndex >= 0 && chapterIndex < logicalChapters.lastIndex) { Text("Следующий →") }
                    }
                    if (sectionTopics.isNotEmpty()) {
                        Text("Темы этого раздела", style = MaterialTheme.typography.titleSmall)
                        sectionTopics.forEach { topic -> TextButton(onClick = { savePosition(); onOpenParagraph(topic.paragraphId) }, modifier = Modifier.fillMaxWidth()) { Text(topic.title, Modifier.fillMaxWidth(), maxLines = 2, overflow = TextOverflow.Ellipsis) } }
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
                    Text(readerTitle, Modifier.weight(1f),
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
                            items(sectionTopics, key = { it.paragraphId + it.title }) { topic ->
                                ChapterTopicRow(topic.title) { revealParagraph(topic.paragraphId) }
                            }
                            if (sectionTopics.isEmpty()) item { Text("В этом разделе нет отдельных подразделов.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        1 -> {
                            val own = bookmarks.filter { it.chapterId in memberIds }
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
    if (noteDialog) AlertDialog(onDismissRequest = { noteDialog = false }, title = { Text(if (noteAnchor == null) "Заметка к разделу" else "Заметка к абзацу") }, text = { OutlinedTextField(noteText, { noteText = it }, label = { Text("Ваши мысли") }, modifier = Modifier.fillMaxWidth(), minLines = 5) }, confirmButton = { TextButton(onClick = { vm.addNote(paragraphs.firstOrNull { it.id == noteAnchor }?.chapterId ?: sectionId, noteAnchor, noteText); noteDialog = false; message = "Заметка сохранена" }, enabled = noteText.isNotBlank()) { Text("Сохранить") } }, dismissButton = { TextButton(onClick = { noteDialog = false }) { Text("Отмена") } })
    if (bookmarkDialog) AlertDialog(onDismissRequest = { bookmarkDialog = false }, title = { Text("Сохранить закладку") }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { OutlinedTextField(bookmarkTitle, { bookmarkTitle = it }, label = { Text("Название") }, modifier = Modifier.fillMaxWidth()); OutlinedTextField(bookmarkNote, { bookmarkNote = it }, label = { Text("Комментарий · необязательно") }, modifier = Modifier.fillMaxWidth(), minLines = 3) } }, confirmButton = { TextButton(onClick = { vm.bookmarkParagraph(paragraphs.firstOrNull { it.id == bookmarkAnchor }?.chapterId ?: sectionId, bookmarkAnchor, bookmarkTitle, bookmarkNote); bookmarkDialog = false; message = "Закладка сохранена" }, enabled = bookmarkTitle.isNotBlank()) { Text("Сохранить") } }, dismissButton = { TextButton(onClick = { bookmarkDialog = false }) { Text("Отмена") } })
    if (sourceDialog) AlertDialog(onDismissRequest = { sourceDialog = false }, title = { Text("Источник раздела") }, text = { SelectionContainer { Text(chapter.source.ifBlank { "Источник требует редакционной сверки." }) } }, confirmButton = { TextButton(onClick = { sourceDialog = false }) { Text("Закрыть") } })
    termDialog?.let { term -> AlertDialog(onDismissRequest = { termDialog = null }, title = { Text(term.transcript) }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { ArabicText(term.ar, settings); Text(term.definition); Text("Пояснение редакции к этому фрагменту", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }, confirmButton = { TextButton(onClick = { val id = term.sourceParagraphId; termDialog = null; onOpenParagraph(id) }) { Text("Исходный контекст") } }, dismissButton = { TextButton(onClick = { termDialog = null }) { Text("Закрыть") } }) }
}

@Composable
private fun ReaderSectionCard(block: ReaderTextBlock.Section, settings: ReadingSettings, arabic: Boolean) {
    val sourceTitle = block.title?.takeIf { it.isNotBlank() }
    if (sourceTitle == null) {
        // The source has a section break but no heading. Rendering the word
        // "РАЗДЕЛ" alone looked like an unnamed new chapter. Keep the actual
        // textual boundary as a modest book ornament without inventing a title.
        Row(
            Modifier.fillMaxWidth().padding(vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                Modifier.weight(1f), thickness = .65.dp,
                color = BookColors.gold.copy(alpha = .28f)
            )
            Text("❖", Modifier.padding(horizontal = 10.dp),
                fontFamily = BookSerif,
                color = BookColors.gold.copy(alpha = .8f))
            HorizontalDivider(
                Modifier.weight(1f), thickness = .65.dp,
                color = BookColors.gold.copy(alpha = .28f)
            )
        }
    } else {
        Column(
            Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 5.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (arabic) ArabicText(block.label, settings, Modifier.heightIn(min = 28.dp))
            else Text(block.label.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
            if (arabic) ArabicText(sourceTitle, settings)
            else Text(sourceTitle, style = MaterialTheme.typography.titleMedium,
                fontFamily = russianFamily(settings),
                color = MaterialTheme.colorScheme.onSurface)
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
                    annotatedReaderText(segment.raw, query, MaterialTheme.colorScheme.primary, terms, onOpenTerm),
                    style = readerStyle(settings),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                )
                is ReaderRichSegment.Quote -> ReaderQuoteCard(segment, settings, query)
                is ReaderRichSegment.Numbered -> ReaderNumberedBlock(segment, settings, query)
                is ReaderRichSegment.Heading -> Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 11.dp, bottom = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.width(3.dp).height(34.dp)
                        .background(BookColors.gold.copy(alpha = .80f), RoundedCornerShape(3.dp)))
                    Text(segment.raw,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = BookSerif),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f))
                }
                is ReaderRichSegment.Poetry -> AgedPaperCard(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .46f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        segment.raw,
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
    // Antique parchment and existing gold/brown ink only; background images,
    // page border, paper tone and global MaterialTheme are never changed.
    val subtitle = when (segment.kind) {
        ReaderCitationKind.VERSE -> "АЯТ КОРАНА"
        ReaderCitationKind.PROPHETIC_REPORT -> "СЛОВА ПРОРОКА"
        ReaderCitationKind.COMPANION_SAYING -> "СЛОВА СПОДВИЖНИКА"
        ReaderCitationKind.SAYING -> "ВЫСКАЗЫВАНИЕ"
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            Modifier.width(3.dp).heightIn(min = 64.dp).fillMaxHeight()
                .background(BookColors.gold.copy(alpha = .86f), RoundedCornerShape(3.dp))
        )
        AgedPaperCard(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .74f)),
            border = androidx.compose.foundation.BorderStroke(
                .7.dp, BookColors.lightGold.copy(alpha = .34f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (segment.kind == ReaderCitationKind.VERSE) {
                        NavigationGlyph("contents", selected = true,
                            modifier = Modifier.size(19.dp), tint = BookColors.gold)
                    } else {
                        Text(if (segment.kind == ReaderCitationKind.PROPHETIC_REPORT) "✧" else "❖",
                            fontFamily = BookSerif,
                            color = BookColors.gold,
                            style = MaterialTheme.typography.titleSmall)
                    }
                    Text(subtitle, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                if (segment.label.isNotBlank() && segment.label != "Аят") {
                    Text(segment.label, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // The original quoted punctuation/citation is displayed intact,
                // even when the source has no verified hadith grading.
                Text(highlight(segment.body, query, MaterialTheme.colorScheme.primary),
                    style = readerStyle(settings).copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ReaderNumberedBlock(
    segment: ReaderRichSegment.Numbered,
    settings: ReadingSettings,
    query: String
) {
    // Inline emphasis instead of a separate badge/column: text keeps the
    // entire page width, so "Первое / Второе" doesn't force excessive wrapping.
    // Use the original source string verbatim, including its punctuation.
    val accent = MaterialTheme.colorScheme.primary
    val numbered = remember(segment.raw, segment.label, query, accent) {
        buildAnnotatedString {
            append(highlight(segment.raw, query, accent))
            val offset = segment.raw.indexOf(segment.label)
            if (offset >= 0) {
                addStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = accent),
                    offset, offset + segment.label.length)
            }
        }
    }
    Text(
        numbered,
        style = readerStyle(settings),
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
    )
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
