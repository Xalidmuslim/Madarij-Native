package ru.madarij.nativeapp

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import ru.madarij.nativeapp.data.*
import java.text.DateFormat
import java.util.Date
import kotlin.random.Random

/** Tabs: 0 checks, 1 quiz, 2 written tasks, 3 review. */
@Composable
fun StudyScreen(vm: BookViewModel, onOpenParagraph: (String) -> Unit, initialChapterId: String? = null, initialTab: Int = 0) {
    val context=LocalContext.current
    val repo=remember(context) { LearningRepository(context.applicationContext) }
    val attempts by repo.attempts.collectAsState(initial=emptyMap())
    val chapterReviews by repo.chapterReviews.collectAsState(initial=emptyMap())
    var chapterReviewMessage by remember {mutableStateOf("")}
    val chapters by vm.chapters.collectAsState()
    val read by vm.read.collectAsState()
    val last by vm.last.collectAsState()
    val notes by vm.notes.collectAsState()
    val structure=rememberStructure()
    val chapterTitles=remember(chapters,structure) { chapters.associate { it.id to structure.findTitle(it) } }
    val scope=rememberCoroutineScope()
    var loaded by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf(false) }
    LaunchedEffect(repo) { try {repo.load();loaded=true} catch(e:Exception) {if(e is CancellationException) throw e;loadError=true} }
    var tab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0,3)) }
    var level by rememberSaveable { mutableStateOf("advanced") }
    var chapterId by rememberSaveable { mutableStateOf(initialChapterId ?: "") }
    var questionId by rememberSaveable { mutableStateOf("") }
    var answer by rememberSaveable { mutableIntStateOf(-1) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var confidence by rememberSaveable { mutableStateOf(false) }
    var optionOrder by rememberSaveable { mutableStateOf("") }
    var sessionRotation by rememberSaveable { mutableIntStateOf(Random.nextInt(4)) }
    var saveError by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var chapterMenu by remember { mutableStateOf(false) }
    var chapterSearch by rememberSaveable { mutableStateOf("") }
    var checksFilter by rememberSaveable { mutableStateOf("in_progress") }
    var showChecksHelp by rememberSaveable { mutableStateOf(false) }
    var visibleQuestions by rememberSaveable { mutableIntStateOf(15) }
    var visibleExercises by rememberSaveable { mutableIntStateOf(15) }
    var exerciseId by rememberSaveable { mutableStateOf("") }
    var reflection by rememberSaveable { mutableStateOf("") }
    var hintVisible by rememberSaveable { mutableStateOf(false) }
    var noteSaved by rememberSaveable { mutableStateOf(false) }
    val bank=if(loaded) repo.questions else emptyList()
    val dueQuestions=attempts.count { it.value.due<=System.currentTimeMillis() }
    val q=bank.find { it.id==questionId }
    val exercise=if(loaded) repo.exercises.find{it.id==exerciseId} else null
    fun start(item: StudyQuestion) {
        questionId=item.id;answer=-1;submitted=false;confidence=false;saveError=""
        val serial=bank.indexOfFirst{it.id==item.id}.coerceAtLeast(0)
        val correct=item.options.indexOfFirst{it.correct}
        val wrong=item.options.indices.filter{it!=correct}.shuffled().toMutableList()
        wrong.add((serial+sessionRotation+(attempts[item.id]?.count ?: 0))%item.options.size,correct)
        optionOrder=wrong.joinToString(",")
    }
    @Composable fun chapterPicker() {
        OutlinedButton(onClick={chapterMenu=true},modifier=Modifier.fillMaxWidth()) {Text(chapterTitles[chapterId] ?: "Весь том",maxLines=1)}
        if(chapterMenu) AlertDialog(
            onDismissRequest={chapterMenu=false},
            title={Text("Выбрать раздел")},
            text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(chapterSearch,{chapterSearch=it},modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text("Поиск раздела")})
                LazyColumn(Modifier.heightIn(max=360.dp),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                    item {TextButton(onClick={chapterId="";exerciseId="";chapterMenu=false;chapterSearch=""},modifier=Modifier.fillMaxWidth()) {Text("Весь том",Modifier.fillMaxWidth())}}
                    items(chapters.filter { chapterSearch.isBlank() || normalizedText(chapterTitles[it.id].orEmpty()).text.contains(normalizedText(chapterSearch).text) },key={it.id}) { c ->
                        TextButton(onClick={chapterId=c.id;exerciseId="";reflection="";hintVisible=false;chapterMenu=false;chapterSearch="";visibleQuestions=15;visibleExercises=15},modifier=Modifier.fillMaxWidth()) {Text(chapterTitles[c.id] ?: c.title,Modifier.fillMaxWidth(),maxLines=2)}
                    }
                }
            }},
            confirmButton={TextButton(onClick={chapterMenu=false}) {Text("Закрыть")}}
        )
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(9.dp),contentPadding=PaddingValues(start=0.dp,top=12.dp,end=0.dp,bottom=124.dp)) {
        item { Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)) {Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {Text("Изучение книги",style=MaterialTheme.typography.titleLarge);Text("${read.size} разделов прочитано · ${attempts.size} вопросов пройдено · $dueQuestions на повторение",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onPrimaryContainer)}} }
        if(chapterReviewMessage.isNotBlank()) item {Text(chapterReviewMessage,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.primary)}
        item { Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)) {listOf("Проверки","Викторина","Задания","Повторение").forEachIndexed { i,t -> FilterChip(selected=tab==i,onClick={tab=i;questionId="";visibleQuestions=15;visibleExercises=15},label={Text(t)}) }} }
        if(!loaded) item { Text(if(loadError) "Не удалось загрузить учебные материалы. Откройте раздел снова." else "Загрузка учебных материалов…") }
        else if(q!=null && (tab==1 || tab==3)) {
            item { Text(q.prompt,style=MaterialTheme.typography.titleLarge);Text(levelLabel(q.level),style=MaterialTheme.typography.labelLarge) }
            val indices=optionOrder.split(",").mapNotNull{it.toIntOrNull()}.filter{it in q.options.indices}.ifEmpty {q.options.indices.toList()}
            items(indices,key={"option:$it"}) { index -> val option=q.options[index]; val label=('A'.code+indices.indexOf(index)).toChar()
                OutlinedButton(onClick={answer=index},enabled=!submitted && !saving,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).semantics {contentDescription="Вариант $label. ${option.text}${if(answer==index) ". Выбран" else ""}"}) {Text("$label. ${if(answer==index) "✓ " else ""}${option.text}")}
                if(submitted) Text((if(option.correct) "Ответ по тексту. " else "Другой вариант. ")+option.explanation)
            }
            item {
                if(!submitted) {
                    Row { Checkbox(confidence,{confidence=it},enabled=!saving);Text("Ответ неуверенный",Modifier.padding(top=12.dp)) }
                    Button(enabled=answer>=0 && !saving,onClick={saving=true;scope.launch {try {repo.record(q.id,q.options[answer].correct,confidence);submitted=true} catch(e:Exception) {if(e is CancellationException) throw e;saveError="Не удалось сохранить попытку. Повторите."} finally {saving=false}}}) {Text("Проверить ответ")}
                    if(saveError.isNotBlank()) Text(saveError)
                } else {Text("Исходный фрагмент",style=MaterialTheme.typography.titleMedium);Text(q.sourceExcerpt);TextButton(onClick={onOpenParagraph(q.sourceParagraphId)}) {Text("Открыть абзац в книге")} }
                TextButton(onClick={questionId=""}) {Text("К списку вопросов")}
            }
        } else when(tab) {
            0 -> {
                item {
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        FilterChip(checksFilter=="in_progress",{checksFilter="in_progress"},label={Text("В работе")})
                        FilterChip(checksFilter=="all",{checksFilter="all"},label={Text("Все разделы")})
                    }
                    chapterPicker()
                    TextButton(onClick={showChecksHelp=!showChecksHelp}) {Text(if(showChecksHelp) "Скрыть пояснение" else "Как считается понимание")}
                    if(showChecksHelp) Text("«Понято по проверкам»: не менее двух ответов, из них не менее 80% правильных. Это показатель работы с вопросами; чтение отмечается отдельно.",style=MaterialTheme.typography.bodySmall)
                }
                val activeIds=read.map{it.chapterId}.toSet()+listOfNotNull(last?.chapterId)
                val checksChapters=chapters.filter{(chapterId.isBlank() || it.id==chapterId) && (checksFilter=="all" || activeIds.isEmpty() || it.id in activeIds)}
                items(checksChapters,key={"section:${it.id}"}) { c ->
                    val questions=bank.filter{it.chapterId==c.id}; val tried=questions.mapNotNull{attempts[it.id]}.filter{it.count>0};val correct=tried.count{it.latestCorrect};val understood=tried.size>=2 && correct.toDouble()/tried.size>=0.8
                    var actions by remember {mutableStateOf(false)}
                    Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)) {Column(Modifier.padding(horizontal=14.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        Text(chapterTitles[c.id] ?: c.title,style=MaterialTheme.typography.titleMedium)
                        Text("${if(read.any{it.chapterId==c.id}) "Прочитано" else "Не прочитано"} · ${if(understood) "Понято по проверкам" else "Нужна проверка"} · $correct/${tried.size} правильных",style=MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick={chapterId=c.id;tab=1;questionId=""}) {Text("Проверить")}
                            Box {
                                TextButton(onClick={actions=true}) {Text("Ещё")}
                                DropdownMenu(expanded=actions,onDismissRequest={actions=false}) {
                                    DropdownMenuItem(text={Text("Письменные задания")},onClick={actions=false;chapterId=c.id;tab=2;exerciseId=""})
                                    DropdownMenuItem(text={Text("Повторить через 3 дня")},onClick={actions=false;scope.launch {try {repo.scheduleChapterReview(c.id);chapterReviewMessage="Перечитывание раздела назначено через 3 дня."} catch(e:Exception) {if(e is CancellationException) throw e;chapterReviewMessage="Не удалось назначить перечитывание. Повторите."}}})
                                }
                            }
                        }
                        chapterReviews[c.id]?.let {review ->Text("Перечитать: ${date(review.due)}",style=MaterialTheme.typography.bodySmall)}
                    }}
                }
            }
            2 -> {
                item {chapterPicker();Text("Письменный ответ сохраняется как личная заметка. Редакторский ориентир открывается после того, как вы напишете ответ.")}
                val exercises=repo.exercises.filter{chapterId.isBlank() || it.chapterId==chapterId}
                if(exercise==null) {
                    items(exercises.take(visibleExercises),key={it.id}) { e ->Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(14.dp)) {Text(chapterTitles[e.chapterId] ?: "Раздел",style=MaterialTheme.typography.labelLarge);Text(e.prompt);TextButton(onClick={exerciseId=e.id;reflection="";hintVisible=false;noteSaved=false}) {Text("Написать ответ")}}} }
                    if(exercises.size>visibleExercises) item {TextButton(onClick={visibleExercises+=15}) {Text("Показать ещё · ${exercises.size-visibleExercises}")}}
                }
                else {
                    item {Text(exercise.prompt,style=MaterialTheme.typography.titleLarge);OutlinedTextField(reflection,{reflection=it;noteSaved=false},label={Text("Ваш ответ")},modifier=Modifier.fillMaxWidth(),minLines=5)}
                    item {Button(enabled=reflection.isNotBlank() && !noteSaved,onClick={vm.addNote(exercise.chapterId,exercise.sourceParagraphId,"Задание: ${exercise.prompt}\n\n$reflection");noteSaved=true}) {Text(if(noteSaved) "Сохранено в заметках" else "Сохранить ответ")};TextButton(enabled=reflection.trim().isNotEmpty(),onClick={hintVisible=true}) {Text("Показать ориентир после ответа")};if(hintVisible) Text(exercise.hint);TextButton(onClick={onOpenParagraph(exercise.sourceParagraphId)}) {Text("Открыть опорный абзац")};TextButton(onClick={exerciseId=""}) {Text("К заданиям")}}
                    items(notes.filter{it.chapterId==exercise.chapterId && it.paragraphId==exercise.sourceParagraphId},key={"note:${it.id}"}) {note ->Card {Text(note.text,Modifier.padding(16.dp))} }
                }
            }
            else -> {
                item {chapterPicker();if(tab==1) {Text("Уровень — сложность редакторской проверки. Для полной проверки раздела доступны вопросы разных уровней.");Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf("basic","advanced","expert").forEach {v->FilterChip(level==v,{level=v},label={Text(levelLabel(v))})}}} else Text("Повторение: ошибки и неуверенные ответы — через день; уверенные правильные — через 3, 7 и 14 дней. Очередь учитывает ошибки, уверенность и срок.")}
                val now=System.currentTimeMillis()
                if(tab==3) {
                    val scheduled=chapterReviews.filter {chapterId.isBlank() || it.key==chapterId}.toList().sortedBy {it.second.due}
                    item {Text("Перечитывание разделов",style=MaterialTheme.typography.titleMedium);Text("Эти задания назначаются отдельно от викторины и не меняют результаты ответов.",style=MaterialTheme.typography.bodySmall)}
                    if(scheduled.isEmpty()) item {Text("Перечитывание разделов пока не назначено. Его можно назначить во вкладке «Проверки» или при чтении.")}
                    items(scheduled,key={"chapter-review:${it.first}"}) { (id,review) ->Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text(chapterTitles[id] ?: "Раздел $id",style=MaterialTheme.typography.titleMedium)
                        Text("${if(review.due<=now) "Пора перечитать" else "Перечитать"}: ${date(review.due)}")
                        TextButton(onClick={scope.launch {try {val pid=vm.repository.dao.paragraphs(id).firstOrNull()?.id;if(pid!=null) onOpenParagraph(pid) else chapterReviewMessage="Не удалось найти текст раздела."} catch(e:Exception) {if(e is CancellationException) throw e;chapterReviewMessage="Не удалось открыть раздел. Повторите."}}}) {Text("Открыть раздел")}
                        TextButton(onClick={scope.launch {try {repo.clearChapterReview(id);chapterReviewMessage="Перечитывание отмечено выполненным."} catch(e:Exception) {if(e is CancellationException) throw e;chapterReviewMessage="Не удалось сохранить отметку. Повторите."}}}) {Text("Перечитано — завершить")}
                    }} }
                    item {Text("Повторение вопросов",style=MaterialTheme.typography.titleMedium)}
                }
                val filtered=bank.filter{item -> (chapterId.isBlank() || item.chapterId==chapterId) && if(tab==3) (attempts[item.id]?.due ?: Long.MAX_VALUE)<=now else item.level==level }.let { list ->if(tab==3) list.sortedWith(compareByDescending<StudyQuestion>{attempts[it.id]?.let{a->a.errors.toDouble()/a.count.coerceAtLeast(1)} ?: 0.0}.thenByDescending{attempts[it.id]?.lowConfidence==true}.thenBy{attempts[it.id]?.due ?: 0L}) else list }
                if(filtered.isEmpty()) item {Text(if(tab==3) "Назначенных повторений на сегодня нет." else "Для выбранного уровня в этом разделе вопросов нет. Выберите другой уровень.")}
                items(filtered.take(visibleQuestions),key={it.id}) {item->Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(14.dp)) {Text(chapterTitles[item.chapterId] ?: "Раздел",style=MaterialTheme.typography.labelLarge);Text(item.prompt);attempts[item.id]?.let{a->Text("Попыток: ${a.count} · ошибок: ${a.errors} · ${if(a.lowConfidence) "неуверенный ответ · " else ""}повторить: ${date(a.due)}",style=MaterialTheme.typography.bodySmall)};TextButton(onClick={start(item)}) {Text("Ответить")}}} }
                if(filtered.size>visibleQuestions) item {TextButton(onClick={visibleQuestions+=15}) {Text("Показать ещё · ${filtered.size-visibleQuestions}")}}
                if(tab==3) {
                    val future=bank.filter{(chapterId.isBlank() || it.chapterId==chapterId) && (attempts[it.id]?.due ?: 0L)>now}.sortedBy{attempts[it.id]?.due}
                    if(future.isNotEmpty()) item {Text("Следующие повторения",style=MaterialTheme.typography.titleMedium)}
                    items(future.take(visibleQuestions),key={"future:${it.id}"}) {item->Column {Text(item.prompt,style=MaterialTheme.typography.bodySmall);Text("${date(attempts.getValue(item.id).due)}",style=MaterialTheme.typography.labelMedium);TextButton(onClick={start(item)}) {Text("Повторить раньше")}}}
                    if(future.size>visibleQuestions) item {TextButton(onClick={visibleQuestions+=15}) {Text("Показать ещё повторения · ${future.size-visibleQuestions}")}}
                    val new=bank.count{(chapterId.isBlank() || it.chapterId==chapterId) && attempts[it.id]==null};item {Text("Ещё не отвечали: $new. Новые вопросы доступны во вкладке «Викторина».")}
                }
            }
        }
    }
}
private fun levelLabel(level: String)=when(level) {"basic"->"Базовый";"expert"->"Экспертный";else->"Продвинутый"}
private fun date(value:Long)=DateFormat.getDateInstance().format(Date(value))
@Composable
fun GlossaryScreen(onOpenParagraph: (String) -> Unit) {
    val context=LocalContext.current;val repo=remember(context) {LearningRepository(context.applicationContext)}
    var query by rememberSaveable {mutableStateOf("")};var expanded by rememberSaveable {mutableStateOf("")};var loaded by remember {mutableStateOf(false)};var error by remember {mutableStateOf(false)}
    LaunchedEffect(repo) {try {repo.load();loaded=true} catch(e:Exception) {if(e is CancellationException) throw e;error=true}}
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=124.dp)) {
        item {Text("Термины",style=MaterialTheme.typography.headlineSmall);Text("Редакторские пояснения к фрагментам непроверенного перевода. Значение уточняется в контексте.");OutlinedTextField(query,{query=it},label={Text("Поиск термина")},modifier=Modifier.fillMaxWidth())}
        if(!loaded) item {Text(if(error) "Не удалось загрузить термины." else "Загрузка…")}
        else items(repo.terms.filter{query.isBlank() || "${it.ar} ${it.transcript} ${it.definition}".contains(query,ignoreCase=true)},key={it.id}) {term->Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {Text(term.ar,fontFamily=FontFamily(Font(R.font.naskh)),fontSize=28.sp);Text(term.transcript,style=MaterialTheme.typography.titleMedium);Text(term.definition);TextButton(onClick={expanded=if(expanded==term.id) "" else term.id}) {Text(if(expanded==term.id) "Свернуть" else "Подробности и связи")};if(expanded==term.id) {Text(term.extendedDefinition);term.relatedTerms.mapNotNull{id->repo.terms.find{it.id==id}}.forEach{related->TextButton(onClick={query=related.transcript;expanded=related.id}) {Text("Связанный термин: ${related.transcript}")}}};term.sourceParagraphIds.ifEmpty{listOf(term.sourceParagraphId)}.forEachIndexed { i,pid->TextButton(onClick={onOpenParagraph(pid)}) {Text(if(i==0) "Открыть контекст" else "Другой фрагмент ${i+1}")}}}} }
    }
}
