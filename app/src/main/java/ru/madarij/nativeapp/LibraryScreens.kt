package ru.madarij.nativeapp

import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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

internal data class TocTopic(val paragraphId: String, val title: String, val provenance: String)
internal data class TocChapter(val chapterId: String, val title: String, val subtitle: String, val topics: List<TocTopic>, val entryParagraphId: String? = null, val entryNote: String? = null)
internal data class TocGroup(val id: String, val title: String, val provenance: String, val items: List<TocChapter>)
internal suspend fun loadStructure(context: Context): List<TocGroup> = withContext(Dispatchers.IO) {
    runCatching {
        val a = JSONObject(context.assets.open("structure.json").bufferedReader().use { it.readText() }).getJSONArray("groups")
        (0 until a.length()).map { i -> val g = a.getJSONObject(i); val items = g.getJSONArray("items")
            TocGroup(g.getString("id"),g.getString("title"),g.optString("provenance"),(0 until items.length()).map { j -> val c=items.getJSONObject(j);val topics=c.optJSONArray("topics")
                TocChapter(c.getString("chapterId"),c.getString("title"),c.optString("subtitle"),if(topics==null) emptyList() else (0 until topics.length()).map { k -> val t=topics.getJSONObject(k);TocTopic(t.getString("paragraphId"),t.getString("title"),t.optString("provenance")) },c.optString("entryParagraphId").takeIf { it.isNotBlank() },c.optString("entryNote").takeIf { it.isNotBlank() })
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
    Column(Modifier.fillMaxSize().padding(horizontal=20.dp)) {
        OutlinedTextField(query,{query=it},modifier=Modifier.fillMaxWidth().padding(top=16.dp),label={Text("Слово или фраза")},singleLine=true,trailingIcon={if(query.isNotEmpty()) TextButton(onClick={query=""}) {Text("Очистить")}})
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(top=10.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("all" to "Всё","title" to "Заголовки","ayat" to "Фрагменты с аятами","hadith" to "Фрагменты с хадисами","term" to "Термины","note" to "Заметки").forEach { (key,label) -> FilterChip(filter==key,{filter=key},{Text(label)},modifier=Modifier.heightIn(min=48.dp)) }
        }
        Text(when {loading -> "Поиск…";error.isNotBlank() -> error;query.isBlank() -> "Ищите в русском и арабском тексте. Огласовки и регистр не влияют на поиск.";else -> "Найдено: $found"},Modifier.padding(vertical=12.dp),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(filter=="ayat" || filter=="hadith") Text("Фильтр находит упоминания по словам исходного текста. Он не заменяет проверку цитаты и её атрибуции.",Modifier.padding(bottom=12.dp),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(!loading) LazyColumn(state=state,modifier=Modifier.weight(1f),contentPadding=PaddingValues(bottom=124.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            if(query.isNotBlank() && found==0 && error.isBlank()) item { InfoCard("Ничего не найдено","Попробуйте более короткое слово, другую форму или фильтр «Всё».") }
            items(results,key={"p:${it.id}"}) { p ->
                val c=chapters.find { it.id==p.chapterId }
                Card(onClick={open(p.chapterId,p.id,query)},modifier=Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text(if(c==null) "Первый том" else structure.findTitle(c),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
                        val arabicOnly=normalizedText(p.ar).text.contains(normalizedText(query).text) && !normalizedText(p.ru).text.contains(normalizedText(query).text)
                        if(arabicOnly) Text(highlight(searchExcerpt(p.ar,query),query,MaterialTheme.colorScheme.primary),maxLines=5,style=textDirectionStyle(),overflow=TextOverflow.Ellipsis)
                        else Text(highlight(searchExcerpt(p.ru,query),query,MaterialTheme.colorScheme.primary),maxLines=5,overflow=TextOverflow.Ellipsis)
                    }
                }
            }
            items(ownNotes,key={"n:${it.id}"}) { n -> Card(onClick={open(n.chapterId,n.paragraphId,query)},modifier=Modifier.fillMaxWidth()) {Column(Modifier.padding(18.dp)) {Text("Моя заметка",style=MaterialTheme.typography.labelLarge);Text(highlight(searchExcerpt(n.text,query),query,MaterialTheme.colorScheme.primary),maxLines=5,overflow=TextOverflow.Ellipsis)}} }
            items(terms,key={"t:${it.transcript}"}) { t -> val scope=rememberCoroutineScope();Card(onClick={scope.launch {vm.repository.dao.paragraph(t.sourceParagraphId)?.let {open(it.chapterId,it.id,query)}}},modifier=Modifier.fillMaxWidth()) {Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {Text("${t.transcript} · ${t.ar}",style=MaterialTheme.typography.titleMedium);Text(t.definition);Text("Открыть исходный контекст",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)}} }
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
    LazyColumn(contentPadding=PaddingValues(start=20.dp,top=20.dp,end=20.dp,bottom=124.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { Text("${bookmarks.size} сохранённых мест", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if(bookmarks.isNotEmpty()) item {OutlinedTextField(query,{query=it},label={Text("Найти закладку")},modifier=Modifier.fillMaxWidth(),singleLine=true)}
        if(bookmarks.isEmpty()) item {InfoCard("Сохраните важное место","В меню чтения можно добавить закладку главы. Для отдельного абзаца включите «Действия с абзацами».")}
        items(bookmarks.filter {query.isBlank() || "${it.title} ${it.note}".contains(query,true)},key={it.id}) {b -> Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {Text(b.title,style=MaterialTheme.typography.titleMedium);Text((if(b.paragraphId==null) "Глава" else "Абзац")+" · "+chapters.find {it.id==b.chapterId}?.title.orEmpty(),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);BookmarkExcerpt(vm, b);if(b.note.isNotBlank()) Text(b.note);Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)) {TextButton(onClick={open(b.chapterId,b.paragraphId)}) {Text("Открыть")};TextButton(onClick={editing=b;title=b.title;note=b.note}) {Text("Изменить")};TextButton(onClick={vm.deleteBookmark(b.id)}) {Text("Удалить")}}}} }
    }
    editing?.let {b -> AlertDialog(onDismissRequest={editing=null},title={Text("Изменить закладку")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {OutlinedTextField(title,{title=it},label={Text("Название")},modifier=Modifier.fillMaxWidth());OutlinedTextField(note,{note=it},label={Text("Комментарий")},modifier=Modifier.fillMaxWidth(),minLines=3)}},confirmButton={TextButton(onClick={vm.editBookmark(b.id,title,note);editing=null},enabled=title.isNotBlank()) {Text("Сохранить")}},dismissButton={TextButton(onClick={editing=null}) {Text("Отмена")}}) }
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
    LazyColumn(contentPadding=PaddingValues(start=20.dp,top=20.dp,end=20.dp,bottom=124.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {ScreenHeading("Мои заметки","Личные записи рядом с текстом")}
        if(notes.isNotEmpty()) item {OutlinedTextField(query,{query=it},label={Text("Поиск по заметкам")},modifier=Modifier.fillMaxWidth(),singleLine=true)}
        if(notes.isEmpty()) item {InfoCard("Ваши мысли рядом с книгой","Добавьте заметку через меню главы или действия с абзацами. Также можно записать размышление во вкладке изучения.")}
        items(notes.filter {query.isBlank() || it.text.contains(query,true)},key={it.id}) {n -> Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {Text(chapters.find {it.id==n.chapterId}?.title.orEmpty(),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary);Text(n.text);Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(Date(n.updatedAt)),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Row(Modifier.horizontalScroll(rememberScrollState())) {TextButton(onClick={open(n.chapterId,n.paragraphId)}) {Text("К тексту")};TextButton(onClick={editing=n;text=n.text}) {Text("Изменить")};TextButton(onClick={vm.deleteNote(n.id)}) {Text("Удалить")}}}} }
    }
    editing?.let {n -> AlertDialog(onDismissRequest={editing=null},title={Text("Моя заметка")},text={OutlinedTextField(text,{text=it},modifier=Modifier.fillMaxWidth(),minLines=5)},confirmButton={TextButton(onClick={vm.editNote(n.id,text);editing=null},enabled=text.isNotBlank()) {Text("Сохранить")}},dismissButton={TextButton(onClick={editing=null}) {Text("Отмена")}}) }
}

@Composable
internal fun ProgressScreen(vm:BookViewModel,open:(String,String?)->Unit,navigate:(String)->Unit) {
    val chapters by vm.chapters.collectAsStateWithLifecycle();val read by vm.read.collectAsStateWithLifecycle();val history by vm.history.collectAsStateWithLifecycle();val context=LocalContext.current;val repo=remember {LearningRepository(context.applicationContext)};val attempts by repo.attempts.collectAsStateWithLifecycle(emptyMap());var questionChapters by remember {mutableStateOf<Map<String,String>>(emptyMap())}
    LaunchedEffect(chapters) {questionChapters=repo.questions.mapNotNull {q -> vm.repository.dao.paragraph(q.sourceParagraphId)?.let {q.id to it.chapterId}}.toMap()}
    val studied=questionChapters.filter {it.key in attempts}.values.toSet();val due=attempts.count {it.value.due<=System.currentTimeMillis()};val minutes=history.sumOf {it.seconds}/60
    LazyColumn(contentPadding=PaddingValues(start=20.dp,top=20.dp,end=20.dp,bottom=124.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item {ScreenHeading("Первый том", "Чтение и понимание учитываются отдельно")}
        item {InfoCard("Прочитано ${read.size} из ${chapters.size}","${if(chapters.isEmpty()) 0 else read.size*100/chapters.size}% разделов отмечено прочитанными. Отметка меняется вручную.")}
        item {InfoCard("Изучение · ${studied.size} разделов","Отвечено на ${attempts.size} из ${repo.questions.size} вопросов; назначено повторений: $due. Попытка ответа означает работу с вопросом, а не полное усвоение главы.")}
        item {Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {Button(onClick={navigate("study?review=true")},Modifier.weight(1f)) {Text("Повторить")};OutlinedButton(onClick={navigate("study")},Modifier.weight(1f)) {Text("Вопросы")}}}
        item {InfoCard("История чтения","$minutes минут в открытом читателе · ${history.map {it.date}.distinct().size} дней. Время учитывается только пока экран чтения активен.")}
        if(attempts.any {it.value.errors>0 || it.value.lowConfidence}) item {Text("Темы для повторения",style=MaterialTheme.typography.titleMedium)}
        items(repo.questions.filter {q -> attempts[q.id]?.let {it.errors>0 || it.lowConfidence}==true},key={"hard:${it.id}"}) {q -> Card(onClick={navigate("study?review=true")},modifier=Modifier.fillMaxWidth()) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {Text(q.prompt);Text("Ошибок: ${attempts[q.id]?.errors ?: 0}"+(if(attempts[q.id]?.lowConfidence==true) " · Неуверенный ответ" else ""),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
        if(read.isNotEmpty()) item {Text("Прочитанные разделы",style=MaterialTheme.typography.titleMedium)}
        items(chapters.filter {c -> read.any {it.chapterId==c.id}},key={it.id}) {c -> OutlinedCard(Modifier.fillMaxWidth()) {Column(Modifier.padding(16.dp)) {Text(c.title);Row {TextButton(onClick={open(c.id,null)}) {Text("Открыть")};TextButton(onClick={vm.markUnread(c.id)}) {Text("Снять отметку")}}}}}
        history.groupBy {it.date}.entries.take(30).forEach { (date,records) -> item(key="day:$date") {Text("$date · ${records.sumOf {it.seconds}/60} мин · ${records.map {it.chapterId}.distinct().size} разделов",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)} }
    }
}

@Composable
internal fun MoreScreen(navigate:(String)->Unit) {
    LazyColumn(contentPadding=PaddingValues(start=16.dp, top=12.dp, end=16.dp, bottom=124.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        item { ScreenHeading("Ещё", "Инструменты и личные материалы") }
        listOf(
            "study" to ("Изучение" to "Проверки, задания и повторение"),
            "bookmarks" to ("Закладки" to "Сохранённые главы, цитаты и места"),
            "notes" to ("Мои заметки" to "Личные записи и размышления"),
            "glossary" to ("Словарь терминов" to "Пояснения с исходным контекстом"),
            "progress" to ("Прогресс" to "Чтение, изучение и повторение"),
            "settings" to ("Настройки" to "Шрифты, тема и яркость"),
            "backup" to ("Резервная копия" to "Сохранить или перенести данные"),
            "about" to ("О книге" to "Автор, источник и статус перевода")
        ).forEach { (route, texts) ->
            item {
                Card(onClick={navigate(route)},modifier=Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal=14.dp,vertical=10.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                            Text(texts.first,style=MaterialTheme.typography.titleSmall)
                            Text(texts.second,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("›",style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
internal fun AboutScreen() {
    val context=LocalContext.current;var edition by remember {mutableStateOf<JSONObject?>(null)}
    LaunchedEffect(context) {edition=withContext(Dispatchers.IO) {runCatching {JSONObject(context.assets.open("corpus.json").bufferedReader().use {it.readText()}).getJSONObject("edition")}.getOrNull()}}
    LazyColumn(contentPadding=PaddingValues(start=20.dp,top=20.dp,end=20.dp,bottom=124.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item {ScreenHeading("Мадаридж ас-саликин","Степени идущих · Первый том")}
        item {InfoCard("Автор","Ибн Каййим аль-Джаузийя (1292–1350). Книга посвящена ступеням духовного пути и построена вокруг осмысления слов суры «Аль-Фатиха».")}
        item {InfoCard("Русский перевод","Первый том литературно сверён с содержащимся в корпусе арабским текстом: обработаны все 644 авторских блока, страницы 3–610. Русский текст отредактирован по смыслу, терминологии и литературной форме. Отдельная хадисоведческая, источниковедческая и богословская проверка цитат и атрибуций ещё не завершена; поэтому это не окончательное критическое издание.")}
        item {InfoCard("Арабский источник",edition?.optString("source_edition").orEmpty().ifBlank {"Дар Атаат аль-Ильм / Дар Ибн Хазм, 2019"}+"\n"+edition?.optString("source_url").orEmpty())}
        item {InfoCard("Навигация и пояснения","Технические разделы для чтения не всегда совпадают с главами автора. Группы, темы и редакторские пояснения помогают ориентироваться; источник главы доступен из меню чтения. Вопросы и словарь составлены редакционно по тексту и требуют той же сверки.")}
        item {InfoCard("На вашем устройстве","Без регистрации, аналитики и сетевых запросов. Книга, поиск, настройки, прогресс, заметки и закладки работают без интернета. Резервная копия позволяет перенести локальные данные; удаление данных приложения стирает их.")}
        item {Text("Мадаридж · 1.8 · Первый том",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
    }
}
