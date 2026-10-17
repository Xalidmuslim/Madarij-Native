package ru.madarij.nativeapp

import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import ru.madarij.nativeapp.data.*
import java.text.DateFormat
import java.util.Date

internal data class TocTopic(val paragraphId: String, val title: String, val provenance: String, val startText: String? = null)
internal data class TocChapter(val chapterId: String, val title: String, val subtitle: String, val topics: List<TocTopic>, val entryParagraphId: String? = null, val entryNote: String? = null)
internal data class TocGroup(val id: String, val title: String, val provenance: String, val items: List<TocChapter>)
internal suspend fun loadStructure(context: Context): List<TocGroup> = withContext(Dispatchers.IO) {
    runCatching {
        val a = JSONObject(context.assets.open("structure.json").bufferedReader().use { it.readText() }).getJSONArray("groups")
        val second = runCatching {
            JSONObject(context.assets.open("volume2-structure.json").bufferedReader().use { it.readText() }).getJSONArray("groups")
        }.getOrNull()
        if (second != null) for (index in 0 until second.length()) a.put(second.getJSONObject(index))
        (0 until a.length()).map { i -> val g = a.getJSONObject(i); val items = g.getJSONArray("items")
            TocGroup(g.getString("id"),g.getString("title"),g.optString("provenance"),(0 until items.length()).map { j -> val c=items.getJSONObject(j);val topics=c.optJSONArray("topics")
                TocChapter(c.getString("chapterId"),c.getString("title"),c.optString("subtitle"),if(topics==null) emptyList() else (0 until topics.length()).map { k -> val t=topics.getJSONObject(k);TocTopic(t.getString("paragraphId"),t.getString("title"),t.optString("provenance"),t.optString("startText").takeIf { it.isNotBlank() }) },c.optString("entryParagraphId").takeIf { it.isNotBlank() },c.optString("entryNote").takeIf { it.isNotBlank() })
            })
        }
    }.getOrElse { emptyList() }
}
private var cachedStructure: List<TocGroup>? = null
@Composable internal fun rememberStructure(): List<TocGroup> {
    val context=LocalContext.current; var groups by remember(context) { mutableStateOf(cachedStructure.orEmpty()) }
    LaunchedEffect(context) {
        if(groups.isEmpty()) groups=(cachedStructure ?: loadStructure(context.applicationContext)).also { cachedStructure=it }
    }
    return groups
}
internal fun List<TocGroup>.findTitle(chapter: Chapter) = flatMap { it.items }.find { it.chapterId==chapter.id }?.title ?: chapter.title

@Composable
internal fun SearchScreen(vm:BookViewModel,open:(String,String?,String)->Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("all") }
    var results by remember { mutableStateOf<List<Paragraph>>(emptyList()) }
    var loading by remember { mutableStateOf(query.isNotBlank()) }
    var error by remember { mutableStateOf("") }
    var executedQuery by rememberSaveable { mutableStateOf(query) }
    var executedFilter by rememberSaveable { mutableStateOf(filter) }
    val chapters by vm.chapters.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val structure=rememberStructure()
    val context=LocalContext.current
    val learning=remember { LearningRepository(context.applicationContext) }
    val state=rememberLazyListState()
    LaunchedEffect(query,filter) {
        loading=query.isNotBlank()
        if(query!=executedQuery || filter!=executedFilter) {state.requestScrollToItem(0);executedQuery=query;executedFilter=filter}
        delay(250)
        try { results=if(query.isBlank() || filter=="term") emptyList() else vm.repository.search(query,filter);error="" }
        catch(e:kotlinx.coroutines.CancellationException) { throw e }
        catch(_:Exception) {error="Поиск не выполнен. Измените запрос и повторите.";results=emptyList()}
        finally {loading=false}
    }
    val ownNotes=if(filter=="note" && query.isNotBlank()) notes.filter { normalizedText(it.text).text.contains(normalizedText(query).text) } else emptyList()
    val terms=if(filter=="term" && query.isNotBlank()) learning.terms.filter { normalizedText("${it.transcript} ${it.ar} ${it.definition}").text.contains(normalizedText(query).text) } else emptyList()
    val found=results.size+ownNotes.size+terms.size
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)) {
        OutlinedTextField(query,{query=it},modifier=Modifier.fillMaxWidth().padding(top=8.dp).heritageEtching(cornerRadius = HeritageFrameTokens.fieldRadius, prominence = .74f),label={Text("Слово или фраза")},singleLine=true,trailingIcon={if(query.isNotEmpty()) TextButton(onClick={query=""}) {Text("Очистить")}})
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(top=6.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("all" to "Всё","title" to "Заголовки","ayat" to "Фрагменты с аятами","hadith" to "Фрагменты с хадисами","term" to "Термины","note" to "Заметки").forEach { (key,label) -> FilterChip(filter==key,{filter=key},{Text(label)},modifier=Modifier.heightIn(min=48.dp)) }
        }
        Text(when {loading -> "Поиск…";error.isNotBlank() -> error;query.isBlank() -> "Ищите в русском и арабском тексте. Огласовки и регистр не влияют на поиск.";else -> "Найдено: $found"},Modifier.padding(vertical=7.dp),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(filter=="ayat" || filter=="hadith") Text("Фильтр находит упоминания по словам исходного текста. Он не заменяет проверку цитаты и её атрибуции.",Modifier.padding(bottom=8.dp),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(!loading) LazyColumn(state=state,modifier=Modifier.weight(1f),contentPadding=PaddingValues(bottom=124.dp),verticalArrangement=Arrangement.spacedBy(CompactUi.listGapDp.dp)) {
            if(query.isNotBlank() && found==0 && error.isBlank()) item { InfoCard("Ничего не найдено","Попробуйте более короткое слово, другую форму или фильтр «Всё».") }
            items(results,key={"p:${it.id}"}) { p ->
                val c=chapters.find { it.id==p.chapterId }
                AgedPaperCard(onClick={open(p.chapterId,p.id,query)},modifier=Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text(if(c==null) "Первый том" else structure.findTitle(c),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
                        val arabicOnly=normalizedText(p.ar).text.contains(normalizedText(query).text) && !normalizedText(p.ru).text.contains(normalizedText(query).text)
                        if(arabicOnly) Text(highlight(searchExcerpt(p.ar,query),query,MaterialTheme.colorScheme.primary),maxLines=5,style=textDirectionStyle(),overflow=TextOverflow.Ellipsis)
                        else Text(highlight(searchExcerpt(p.ru,query),query,MaterialTheme.colorScheme.primary),maxLines=5,overflow=TextOverflow.Ellipsis)
                    }
                }
            }
            items(ownNotes,key={"n:${it.id}"}) { n -> AgedPaperCard(onClick={open(n.chapterId,n.paragraphId,query)},modifier=Modifier.fillMaxWidth()) {Column(Modifier.padding(12.dp)) {Text("Моя заметка",style=MaterialTheme.typography.labelLarge);Text(highlight(searchExcerpt(n.text,query),query,MaterialTheme.colorScheme.primary),maxLines=5,overflow=TextOverflow.Ellipsis)}} }
            items(terms,key={"t:${it.transcript}"}) { t -> val scope=rememberCoroutineScope();AgedPaperCard(onClick={scope.launch {vm.repository.dao.paragraph(t.sourceParagraphId)?.let {open(it.chapterId,it.id,query)}}},modifier=Modifier.fillMaxWidth()) {Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {Text("${t.transcript} · ${t.ar}",style=MaterialTheme.typography.titleMedium);Text(t.definition);Text("Открыть исходный контекст",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)}} }
        }
    }
}
private fun searchExcerpt(text:String,query:String):String {
    val normalized=normalizedText(text);val needle=normalizedText(query).text
    val found=if(needle.isBlank()) -1 else normalized.text.indexOf(needle)
    val rawIndex=if(found<0) 0 else normalized.offsets[found]
    val start=(rawIndex-60).coerceAtLeast(0);val end=(start+600).coerceAtMost(text.length)
    return (if(start>0) "…" else "")+text.substring(start,end)+(if(end<text.length) "…" else "")
}

private fun textDirectionStyle()=androidx.compose.ui.text.TextStyle(textDirection=androidx.compose.ui.text.style.TextDirection.Rtl,fontFamily=FontFamily(androidx.compose.ui.text.font.Font(R.font.naskh)))

@Composable
internal fun BookmarksScreen(vm:BookViewModel,open:(String,String?)->Unit) {
    val bookmarks by vm.bookmarks.collectAsStateWithLifecycle();val chapters by vm.chapters.collectAsStateWithLifecycle();var editing by remember {mutableStateOf<Bookmark?>(null)};var title by remember {mutableStateOf("")};var note by remember {mutableStateOf("")};var query by rememberSaveable {mutableStateOf("")}
    LazyColumn(contentPadding=PaddingValues(start=16.dp,top=9.dp,end=16.dp,bottom=124.dp),verticalArrangement=Arrangement.spacedBy(CompactUi.listGapDp.dp)) {
        item { Text("${bookmarks.size} сохранённых мест", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if(bookmarks.isNotEmpty()) item {OutlinedTextField(query,{query=it},label={Text("Найти закладку")},modifier=Modifier.fillMaxWidth().heritageEtching(cornerRadius = HeritageFrameTokens.fieldRadius, prominence = .74f),singleLine=true)}
        if(bookmarks.isEmpty()) item {InfoCard("Сохраните важное место","В меню чтения можно добавить закладку главы. Для отдельного абзаца включите «Действия с абзацами».")}
        items(bookmarks.filter {query.isBlank() || "${it.title} ${it.note}".contains(query,true)},key={it.id}) {b -> AgedPaperCard(Modifier.fillMaxWidth()) {Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {Text(b.title,style=MaterialTheme.typography.titleMedium);Text((if(b.paragraphId==null) "Глава" else "Абзац")+" · "+chapters.find {it.id==b.chapterId}?.title.orEmpty(),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);BookmarkExcerpt(vm, b);if(b.note.isNotBlank()) Text(b.note);Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)) {TextButton(onClick={open(b.chapterId,b.paragraphId)}) {Text("Открыть")};TextButton(onClick={editing=b;title=b.title;note=b.note}) {Text("Изменить")};TextButton(onClick={vm.deleteBookmark(b.id)}) {Text("Удалить")}}}} }
    }
    editing?.let {b -> AlertDialog(modifier = Modifier.heritageEtching(cornerRadius = 22.dp, prominence = .82f), onDismissRequest={editing=null},title={Text("Изменить закладку")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {OutlinedTextField(title,{title=it},label={Text("Название")},modifier=Modifier.fillMaxWidth().heritageEtching(cornerRadius = HeritageFrameTokens.fieldRadius, prominence = .74f));OutlinedTextField(note,{note=it},label={Text("Комментарий")},modifier=Modifier.fillMaxWidth().heritageEtching(cornerRadius = HeritageFrameTokens.fieldRadius, prominence = .74f),minLines=3)}},confirmButton={TextButton(onClick={vm.editBookmark(b.id,title,note);editing=null},enabled=title.isNotBlank()) {Text("Сохранить")}},dismissButton={TextButton(onClick={editing=null}) {Text("Отмена")}}) }
}

@Composable
private fun BookmarkExcerpt(vm: BookViewModel, bookmark: Bookmark) {
    var paragraph by remember(bookmark.chapterId, bookmark.paragraphId) { mutableStateOf<Paragraph?>(null) }
    LaunchedEffect(bookmark.chapterId, bookmark.paragraphId) {
        paragraph = bookmark.paragraphId?.let { vm.repository.dao.paragraph(it) }
            ?: vm.repository.dao.paragraphs(bookmark.chapterId).firstOrNull()
    }
    paragraph?.let { text ->
        Text(text.ru, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif),
            maxLines = 3, overflow = TextOverflow.Ellipsis)
        Text("Том 1 · абзац ${text.ordinal + 1}", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun NotesScreen(vm:BookViewModel,open:(String,String?)->Unit) {
    val notes by vm.notes.collectAsStateWithLifecycle();val chapters by vm.chapters.collectAsStateWithLifecycle();var editing by remember {mutableStateOf<Note?>(null)};var text by remember {mutableStateOf("")};var query by rememberSaveable {mutableStateOf("")}
    LazyColumn(contentPadding=PaddingValues(start=16.dp,top=9.dp,end=16.dp,bottom=124.dp),verticalArrangement=Arrangement.spacedBy(CompactUi.listGapDp.dp)) {
        item {ScreenHeading("Мои заметки","Личные записи рядом с текстом")}
        if(notes.isNotEmpty()) item {OutlinedTextField(query,{query=it},label={Text("Поиск по заметкам")},modifier=Modifier.fillMaxWidth().heritageEtching(cornerRadius = HeritageFrameTokens.fieldRadius, prominence = .74f),singleLine=true)}
        if(notes.isEmpty()) item {InfoCard("Ваши мысли рядом с книгой","Добавьте заметку через меню главы или действия с абзацами. Также можно записать размышление во вкладке изучения.")}
        items(notes.filter {query.isBlank() || it.text.contains(query,true)},key={it.id}) {n -> AgedPaperCard(Modifier.fillMaxWidth()) {Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {Text(chapters.find {it.id==n.chapterId}?.title.orEmpty(),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary);Text(n.text);Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(Date(n.updatedAt)),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Row(Modifier.horizontalScroll(rememberScrollState())) {TextButton(onClick={open(n.chapterId,n.paragraphId)}) {Text("К тексту")};TextButton(onClick={editing=n;text=n.text}) {Text("Изменить")};TextButton(onClick={vm.deleteNote(n.id)}) {Text("Удалить")}}}} }
    }
    editing?.let {n -> AlertDialog(modifier = Modifier.heritageEtching(cornerRadius = 22.dp, prominence = .82f), onDismissRequest={editing=null},title={Text("Моя заметка")},text={OutlinedTextField(text,{text=it},modifier=Modifier.fillMaxWidth().heritageEtching(cornerRadius = HeritageFrameTokens.fieldRadius, prominence = .74f),minLines=5)},confirmButton={TextButton(onClick={vm.editNote(n.id,text);editing=null},enabled=text.isNotBlank()) {Text("Сохранить")}},dismissButton={TextButton(onClick={editing=null}) {Text("Отмена")}}) }
}

private fun activitySummary(history: List<ReadingHistory>, period: String): Pair<List<String>, List<Int>> {
    val today = java.time.LocalDate.now()
    val days = history.mapNotNull { item ->
        runCatching { java.time.LocalDate.parse(item.date) }.getOrNull()?.let { it to item.chapterId }
    }
    val labels: List<String>
    val buckets: List<List<java.time.LocalDate>>
    when (period) {
        "month" -> {
            val start = today.minusDays(29)
            buckets = (0..5).map { part ->
                (0..4).map { start.plusDays((part * 5 + it).toLong()) }
            }
            labels = buckets.map { "${it.first().dayOfMonth}" }
        }
        "all" -> {
            val first = today.withDayOfMonth(1).minusMonths(5)
            buckets = (0..5).map { part ->
                val month = first.plusMonths(part.toLong())
                (1..month.lengthOfMonth()).map { month.withDayOfMonth(it) }
            }
            labels = (0..5).map {
                first.plusMonths(it.toLong()).format(
                    java.time.format.DateTimeFormatter.ofPattern("LLL", java.util.Locale("ru"))
                )
            }
        }
        else -> {
            val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
            buckets = (0..6).map { listOf(monday.plusDays(it.toLong())) }
            labels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
        }
    }
    return labels to buckets.map { bucket ->
        // A single day's multiple reading sessions for one chapter count once.
        days.filter { it.first in bucket }.map { it.first to it.second }.distinct().size
    }
}

@Composable
internal fun ProgressScreen(vm: BookViewModel,open:(String,String?)->Unit,navigate:(String)->Unit) {
    val chapters by vm.chapters.collectAsStateWithLifecycle()
    val read by vm.read.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val repo = remember(context) { LearningRepository(context.applicationContext) }
    val attempts by repo.attempts.collectAsStateWithLifecycle(emptyMap())
    var totals by remember { mutableStateOf(0 to 0) }
    var exercisePrompts by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(repo) {
        val prepared = withContext(Dispatchers.Default) {
            Triple(repo.questions.size, repo.exercises.size,
                repo.exercises.map { it.prompt })
        }
        totals = prepared.first to prepared.second
        exercisePrompts = prepared.third
    }
    var view by rememberSaveable { mutableStateOf("all") }
    var period by rememberSaveable { mutableStateOf("week") }
    val readIds = remember(read) { read.map { it.chapterId }.toSet() }
    val completedGroups = remember(chapters, readIds) {
        LogicalReadingSections.roots(chapters).count { root ->
            LogicalReadingSections.members(chapters, root.id).let { members ->
                members.isNotEmpty() && members.all { it.id in readIds }
            }
        }
    }
    val totalGroups = remember(chapters) { LogicalReadingSections.roots(chapters).size }
    val answered = attempts.count { it.value.count > 0 }
    val written = remember(notes, exercisePrompts) {
        val saved = notes.mapNotNull { note ->
            note.text.takeIf { it.startsWith("Задание: ") }
                ?.substringAfter("Задание: ")?.substringBefore("\n\n")
        }.toSet()
        // The bank is decoded off the UI thread; only cheap lookup remains.
        exercisePrompts.count { it in saved }
    }
    val minutes = history.sumOf { it.seconds } / 60
    val latestCorrect = attempts.values.count { it.count > 0 && it.latestCorrect }
    val latestAccuracy = if (answered > 0) "${latestCorrect * 100 / answered}%"
        else "—"
    val due = attempts.values.count { it.count > 0 && it.due <= System.currentTimeMillis() }
    val chart = remember(history, period) { activitySummary(history, period) }
    val today = java.time.LocalDate.now()
    val todayHistory = history.filter { it.date == today.toString() }
    val todaySections = todayHistory.map { it.chapterId }.distinct().size
    val todayMinutes = todayHistory.sumOf { it.seconds } / 60
    val todayAnswers = attempts.values.count { attempt ->
        attempt.count > 0 && attempt.updatedAt > 0 &&
            java.time.Instant.ofEpochMilli(attempt.updatedAt)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate() == today
    }

    LazyColumn(contentPadding=PaddingValues(start=16.dp,end=16.dp,top=7.dp,bottom=130.dp),
        verticalArrangement=Arrangement.spacedBy(CompactUi.listGapDp.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                HeritageChoice("Общий", view=="all", Modifier.weight(1f)) {view="all"}
                HeritageChoice("По разделам", view=="sections", Modifier.weight(1f)) {view="sections"}
            }
        }
        if(view=="all") {
            item {
                Row(Modifier.fillMaxWidth().padding(vertical=3.dp),
                    horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                    HeritageProgressRing(
                        fraction=if(chapters.isEmpty()) 0f else readIds.size.toFloat()/chapters.size,
                        value="${if(chapters.isEmpty()) 0 else readIds.size*100/chapters.size}%",
                        total="${readIds.size} / ${chapters.size}",
                        caption="Прочитано", icon="contents", modifier=Modifier.weight(1f))
                    HeritageProgressRing(
                        fraction=if(totals.first==0) 0f else answered.toFloat()/totals.first,
                        value="${if(totals.first==0) 0 else answered*100/totals.first}%",
                        total="$answered / ${totals.first}",
                        caption="Проверено", icon="study", modifier=Modifier.weight(1f))
                }
            }
            item {
                AgedPaperCard(Modifier.fillMaxWidth(),
                    border=androidx.compose.foundation.BorderStroke(.9.dp,
                        BookColors.gold.copy(alpha=.3f))) {
                    Column(Modifier.padding(vertical=2.dp)) {
                        HeritageMetricRow("contents", "Завершено смысловых разделов",
                            "$completedGroups из $totalGroups", {view="sections"})
                        HorizontalDivider(color=BookColors.gold.copy(alpha=.2f))
                        HeritageMetricRow("notes", "Письменных заданий",
                            "$written из ${totals.second}", {navigate("study")})
                        HorizontalDivider(color=BookColors.gold.copy(alpha=.2f))
                        HeritageMetricRow("study", "Последние ответы верны",
                            latestAccuracy, {navigate("study")})
                        HorizontalDivider(color=BookColors.gold.copy(alpha=.2f))
                        HeritageMetricRow("more", "Время в читалке", "$minutes мин",
                            {view="sections"})
                    }
                }
            }
            item {
                HeritageSectionHeading("Активность")
                Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    listOf("week" to "Неделя", "month" to "Месяц", "all" to "Всё время")
                        .forEach { (key,label) ->
                            HeritageChoice(label,period==key,Modifier.weight(1f)) {period=key}
                        }
                }
            }
            item {
                AgedPaperCard(Modifier.fillMaxWidth(),
                    colors=CardDefaults.cardColors(containerColor=heritageCard().copy(alpha=.82f)),
                    border=androidx.compose.foundation.BorderStroke(.8.dp,
                        BookColors.gold.copy(alpha=.29f))) {
                    Column(Modifier.padding(10.dp),
                        verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        Text("Прочитанные фрагменты", style=MaterialTheme.typography.labelMedium,
                            color=heritageMuted())
                        HeritageActivityBars(chart.second, chart.first,
                            highlightIndex=if(period=="week") today.dayOfWeek.value-1 else -1)
                        HorizontalDivider(color=BookColors.gold.copy(alpha=.22f))
                        Text("Сегодня · ${today.dayOfMonth} " +
                            today.format(java.time.format.DateTimeFormatter.ofPattern(
                                "LLL",java.util.Locale("ru"))),
                            style=MaterialTheme.typography.titleMedium.copy(fontFamily=BookSerif),
                            color=heritageInk())
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement=Arrangement.SpaceBetween) {
                            Text("Читали: $todaySections",style=MaterialTheme.typography.bodySmall)
                            Text("Проверяли: $todayAnswers",style=MaterialTheme.typography.bodySmall)
                            Text("Время: $todayMinutes мин",style=MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            if(due>0) item {
                AgedPaperCard(onClick={navigate("study?review=true")},
                    modifier=Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(11.dp),
                        verticalAlignment=Alignment.CenterVertically) {
                        Text("Вернуться к повторению · $due вопросов",
                            Modifier.weight(1f), color=heritageInk())
                        Text("›",style=MaterialTheme.typography.titleLarge,
                            color=BookColors.gold)
                    }
                }
            }
        } else {
            item { HeritageSectionHeading("Главы двух томов",trailing="Прочитано ${readIds.size} / ${chapters.size}") }
            items(chapters,key={it.id}) { chapter ->
                AgedPaperCard(Modifier.fillMaxWidth(),
                    border=androidx.compose.foundation.BorderStroke(.7.dp,
                        BookColors.gold.copy(alpha=.24f))) {
                    Column(Modifier.padding(horizontal=12.dp,vertical=8.dp),
                        verticalArrangement=Arrangement.spacedBy(3.dp)) {
                        Text("Том ${chapter.volume} · ${if(chapter.id in readIds) "Прочитано" else "Не прочитано"}",
                            style=MaterialTheme.typography.labelSmall,color=heritageMuted())
                        Text(chapter.title,
                            style=MaterialTheme.typography.titleSmall.copy(fontFamily=BookSerif),
                            color=heritageInk())
                        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                            TextButton(onClick={open(chapter.id,null)}){Text("Открыть")}
                            if(chapter.id in readIds)
                                TextButton(onClick={vm.markUnread(chapter.id)}) {
                                    Text("Снять отметку")
                                }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun MoreScreen(navigate:(String)->Unit) {
    LazyColumn(contentPadding=PaddingValues(start=16.dp,top=7.dp,end=16.dp,bottom=130.dp),
        verticalArrangement=Arrangement.spacedBy(CompactUi.listGapDp.dp)) {
        item {
            HeritageSectionHeading("ИЗУЧЕНИЕ")
            HeritageMenuGroup(listOf(
                Triple("study", "Изучение", "Викторины, письменные задания и повторение"),
                Triple("progress", "Мой прогресс", "Чтение, результаты и активность")
            ), navigate)
        }
        item {
            HeritageSectionHeading("ЛИЧНОЕ")
            HeritageMenuGroup(listOf(
                Triple("bookmarks", "Закладки", "Сохранённые главы, цитаты и места"),
                Triple("notes", "Мои заметки", "Личные записи и размышления")
            ), navigate)
        }
        item {
            HeritageSectionHeading("СПРАВКА")
            HeritageMenuGroup(listOf(
                Triple("glossary", "Словарь терминов", "Пояснения с исходным контекстом"),
                Triple("about", "О книге", "Автор, источники и статус перевода")
            ), navigate)
        }
        item {
            HeritageSectionHeading("НАСТРОЙКИ")
            HeritageMenuGroup(listOf(
                Triple("settings", "Настройки", "Шрифты, оформление и чтение")
            ), navigate)
        }
    }
}

@Composable
internal fun AboutScreen() {
    val context=LocalContext.current;var edition by remember {mutableStateOf<JSONObject?>(null)}
    LaunchedEffect(context) {edition=withContext(Dispatchers.IO) {runCatching {JSONObject(context.assets.open("corpus.json").bufferedReader().use {it.readText()}).getJSONObject("edition")}.getOrNull()}}
    LazyColumn(contentPadding=PaddingValues(start=20.dp,top=20.dp,end=20.dp,bottom=124.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item {ScreenHeading("Мадаридж ас-саликин","Степени идущих · Тома 1 и 2")}
        item {InfoCard("Автор","Ибн Каййим аль-Джаузийя (1292–1350). Книга посвящена ступеням духовного пути и построена вокруг осмысления слов суры «Аль-Фатиха».")}
        item {InfoCard("О происхождении русского перевода","Перевод подготовлен специально для данного приложения с применением искусственного интеллекта непосредственно по арабскому тексту. По сведениям составителя приложения, существующие русскоязычные переводы не использовались. Это самостоятельный рабочий перевод, не официальный и не академическое критическое издание.")}
        item {InfoCard("Статус сверки перевода","Том 1: литературно-смысловая сверка страниц 3–610 (644 авторских блока). Том 2: литературно-смысловая сверка страниц 3–659 (676 единиц). Часть исправлений повторно проверена ИИ. Постатейная визуальная сверка со сканами издания 2019 года и независимая научная, хадисоведческая и источниковедческая экспертиза не проводились; содержание может нуждаться в исправлениях.")}
        item {InfoCard("Современное арабское издание и права","Текст труда Ибн аль-Каййима находится в общественном достоянии по сроку охраны автора. Использовано современное арабское издание 2019 года; редакторские примечания, подготовка текста и иные современные элементы издания могут иметь самостоятельную правовую охрану. Источниковедческая и правовая проверка этих материалов до публикации необходима.")}
        item {InfoCard("Арабский источник",edition?.optString("source_edition").orEmpty().ifBlank {"Дар Атаат аль-Ильм / Дар Ибн Хазм, 2019"}+"\n"+edition?.optString("source_url").orEmpty())}
        item {InfoCard("Навигация и пояснения","Технические разделы для чтения не всегда совпадают с главами автора. Группы, темы и редакторские пояснения помогают ориентироваться; источник главы доступен из меню чтения. Вопросы и словарь составлены редакционно по тексту и требуют той же сверки.")}
        item {Text("Мадаридж · Тома 1 и 2",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
    }
}
