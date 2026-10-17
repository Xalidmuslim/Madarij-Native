package ru.madarij.nativeapp

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
    var level by rememberSaveable { mutableStateOf("all") }
    var questionType by rememberSaveable { mutableStateOf("all") }
    var quizSearch by rememberSaveable { mutableStateOf("") }
    var showQuizFilters by rememberSaveable { mutableStateOf(false) }
    var browseQuestions by rememberSaveable { mutableStateOf(false) }
    var showFutureReviews by rememberSaveable { mutableStateOf(false) }
    var sessionIds by rememberSaveable { mutableStateOf("") }
    var sessionIndex by rememberSaveable { mutableIntStateOf(0) }
    var sessionCorrect by rememberSaveable { mutableIntStateOf(0) }
    var sessionFinished by rememberSaveable { mutableStateOf(false) }
    var sessionLastSize by rememberSaveable { mutableIntStateOf(0) }
    var chapterId by rememberSaveable { mutableStateOf(initialChapterId ?: "") }
    var selectedVolume by rememberSaveable { mutableIntStateOf(0) }
    var questionId by rememberSaveable { mutableStateOf("") }
    var answer by rememberSaveable { mutableIntStateOf(-1) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var confidence by rememberSaveable { mutableStateOf(false) }
    var optionOrder by rememberSaveable { mutableStateOf("") }
    var sessionRotation by rememberSaveable { mutableIntStateOf(Random.nextInt(4)) }
    var saveError by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var chapterMenu by remember { mutableStateOf(false) }
    var sessionNotice by remember { mutableStateOf("") }
    var checksFilter by rememberSaveable { mutableStateOf("all") }
    var showAllStudySections by rememberSaveable { mutableStateOf(false) }
    var showChecksHelp by rememberSaveable { mutableStateOf(false) }
    var visibleQuestions by rememberSaveable { mutableIntStateOf(8) }
    var visibleExercises by rememberSaveable { mutableIntStateOf(8) }
    var exerciseId by rememberSaveable { mutableStateOf("") }
    var reflection by rememberSaveable { mutableStateOf("") }
    var hintVisible by rememberSaveable { mutableStateOf(false) }
    var noteSaved by rememberSaveable { mutableStateOf(false) }
    val bank=if(loaded) repo.questions else emptyList()
    val bankByChapter=remember(bank) { bank.groupBy { it.chapterId } }
    val questionCounts=remember(bankByChapter) { bankByChapter.mapValues { it.value.size } }
    val chapterVolumes=remember(chapters) { chapters.associate { it.id to it.volume } }
    val readIds=remember(read) { read.map { it.chapterId }.toSet() }
    fun inStudyScope(id: String): Boolean =
        studyScopeMatches(id, chapterId, selectedVolume, chapterVolumes)
    val scopedBank=remember(bank, chapterId, selectedVolume, chapterVolumes) {
        bank.filter { studyScopeMatches(it.chapterId, chapterId, selectedVolume, chapterVolumes) }
    }
    val dueQuestions=scopedBank.count { (attempts[it.id]?.due ?: Long.MAX_VALUE) <= System.currentTimeMillis() }
    val scopedChapters=remember(chapters,chapterId,selectedVolume,chapterVolumes) {
        chapters.filter { studyScopeMatches(it.id, chapterId, selectedVolume, chapterVolumes) }
    }
    val listState=rememberLazyListState()
    LaunchedEffect(tab, questionId, exerciseId, chapterId, selectedVolume) {
        listState.scrollToItem(0)
    }
    val q=bank.find { it.id==questionId }
    val exercise=if(loaded) repo.exercises.find{it.id==exerciseId} else null
    fun start(item: StudyQuestion) {
        questionId=item.id;answer=-1;submitted=false;confidence=false;saveError=""
        val serial=bank.indexOfFirst{it.id==item.id}.coerceAtLeast(0)
        val correct=item.options.indexOfFirst{it.correct}
        val wrong=item.options.indices.filter{it!=correct}.shuffled().toMutableList()
        if (correct >= 0) wrong.add((serial+sessionRotation+(attempts[item.id]?.count ?: 0))%item.options.size,correct)
        else wrong.addAll(item.options.indices.filter { it !in wrong })
        optionOrder=wrong.joinToString(",")
    }
    fun beginSession(forChapterId: String = chapterId) {
        val eligible = bank.filter { item ->
            studyScopeMatches(item.chapterId, forChapterId, selectedVolume, chapterVolumes) &&
                (level == "all" || item.level == level) &&
                (questionType == "all" ||
                    (item.id.startsWith("book-recognition-") == (questionType == "recognition")))
        }
        val picked = eligible.shuffled().take(10)
        if (picked.isEmpty()) {
            sessionNotice = "По выбранной теме и фильтрам вопросов нет. Выберите другой уровень или тип."
        } else {
            sessionNotice = ""
            sessionIds = picked.joinToString("|") { it.id }
            sessionIndex = 0
            sessionCorrect = 0
            sessionLastSize = picked.size
            sessionFinished = false
            tab = 1
            start(picked.first())
        }
    }
    val bankById=remember(bank) { bank.associateBy { it.id } }
    val sessionQuestions = remember(sessionIds, bankById) {
        if (sessionIds.isBlank()) emptyList()
        else sessionIds.split("|").mapNotNull { id -> bankById[id] }
    }
    if (chapterMenu) {
        StudyChapterPicker(
            selectedVolume = selectedVolume,
            selectedChapterId = chapterId,
            chapters = chapters,
            chapterTitles = chapterTitles,
            questionCounts = questionCounts,
            readIds = readIds,
            onSelectAll = { volume ->
                selectedVolume = volume
                chapterId = ""
                questionId = ""
                exerciseId = ""
                sessionIds = ""
                sessionFinished = false
                checksFilter = "all"
                visibleQuestions = 8
                visibleExercises = 8
                chapterMenu = false
            },
            onSelectChapter = { id, volume ->
                selectedVolume = volume
                chapterId = id
                questionId = ""
                exerciseId = ""
                reflection = ""
                hintVisible = false
                sessionIds = ""
                sessionFinished = false
                checksFilter = "all"
                visibleQuestions = 8
                visibleExercises = 8
                chapterMenu = false
            },
            onDismiss = { chapterMenu = false }
        )
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=16.dp),
        state=listState,
        verticalArrangement=Arrangement.spacedBy(10.dp),
        contentPadding=PaddingValues(start=0.dp,top=12.dp,end=0.dp,bottom=124.dp)) {
        if (q == null && exercise == null) {
            if (tab == 0) {
                item {
                    StudyOverview(
                        readChapters = scopedChapters.count { it.id in readIds },
                        allChapters = scopedChapters.size,
                        answered = scopedBank.count { (attempts[it.id]?.count ?: 0) > 0 },
                        allQuestions = scopedBank.size,
                        due = dueQuestions,
                        writingTasks = if(loaded)
                            repo.exercises.count { inStudyScope(it.chapterId) } else 0,
                        scopeLabel = when {
                            chapterId.isNotBlank() -> "выбранная тема"
                            selectedVolume == 1 -> "первый том"
                            selectedVolume == 2 -> "второй том"
                            else -> "оба тома"
                        },
                        onStart = { beginSession() }
                    )
                }
                item {
                    StudyModeGrid(
                        selectedTab = 0,
                        onSections = {tab=0},
                        onQuiz = {tab=1;questionId="";sessionIds=""},
                        onWriting = {tab=2;exerciseId=""},
                        onReview = {tab=3;questionId="";sessionIds=""}
                    )
                }
            } else {
                item {
                    StudyModeHeader(tab) {
                        tab=0
                        questionId=""
                        sessionIds=""
                        sessionFinished=false
                    }
                }
            }
            item {
                StudyScopeBar(
                    selectedLabel = chapterTitles[chapterId] ?: when(selectedVolume) {
                        1 -> "Все разделы · том 1"
                        2 -> "Все разделы · том 2"
                        else -> "Все разделы · оба тома"
                    },
                    onChooseSection = {chapterMenu=true}
                )
            }
        } else item {
            TextButton(onClick = {
                if (q != null) {
                    questionId = ""
                    sessionIds = ""
                } else exerciseId = ""
            }) {
                Text(if (q != null) "‹ К вопросам" else "‹ К письменным заданиям")
            }
        }
        if(!loaded) item { Text(if(loadError) "Не удалось загрузить учебные материалы. Откройте раздел снова." else "Загрузка учебных материалов…") }
        else if(q!=null && (tab==1 || tab==3)) {
            val ordinal = if (sessionQuestions.isNotEmpty()) sessionIndex + 1 else null
            val total = if (sessionQuestions.isNotEmpty()) sessionQuestions.size else null
            item {
                StudyQuestionHeading(
                    chapter = chapterTitles[q.chapterId] ?: "Раздел книги",
                    level = levelLabel(q.level),
                    ordinal = ordinal,
                    total = total,
                    category = if (q.id.startsWith("book-recognition-")) "Узнайте фрагмент" else "Понимание смысла"
                )
                Spacer(Modifier.height(9.dp))
                Text(q.prompt, style = MaterialTheme.typography.titleLarge)
            }
            val indices=optionOrder.split(",").mapNotNull{it.toIntOrNull()}
                .filter{it in q.options.indices}.ifEmpty {q.options.indices.toList()}
            items(indices,key={"option:$it"}) { index ->
                val option=q.options[index]
                val label=('A'.code+indices.indexOf(index)).toChar()
                OutlinedButton(
                    onClick={answer=index},
                    enabled=!submitted && !saving,
                    modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).semantics {
                        contentDescription="Вариант $label. ${option.text}${if(answer==index) ". Выбран" else ""}"
                    },
                    colors=ButtonDefaults.outlinedButtonColors(
                        containerColor=if(answer==index) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface.copy(alpha=.35f)
                    )
                ) {
                    Text("$label. ${if(answer==index) "✓ " else ""}${option.text}",
                        modifier=Modifier.fillMaxWidth())
                }
            }
            item {
                if(!submitted) {
                    Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(confidence,{confidence=it},enabled=!saving)
                        Text("Ответ неуверенный",style=MaterialTheme.typography.bodySmall)
                    }
                    Button(enabled=answer>=0 && !saving,
                        modifier=Modifier.fillMaxWidth(),
                        onClick={
                            saving=true
                            scope.launch {
                                try {
                                    val correct=q.options[answer].correct
                                    repo.record(q.id,correct,confidence)
                                    submitted=true
                                    if(correct && sessionQuestions.isNotEmpty()) sessionCorrect++
                                } catch(e:Exception) {
                                    if(e is CancellationException) throw e
                                    saveError="Не удалось сохранить попытку. Повторите."
                                } finally { saving=false }
                            }
                        }) { Text("Проверить ответ") }
                    if(saveError.isNotBlank()) Text(saveError)
                } else {
                    val selected=q.options.getOrNull(answer)
                    val correct=q.options.firstOrNull { it.correct }
                    val good=selected?.correct == true
                    StudyAnswerFeedback(
                        isCorrect=good,
                        detail=buildString {
                            if(!good && correct!=null) append("Правильный вариант: ${correct.text}\n\n")
                            append(if(good) correct?.explanation.orEmpty()
                                else selected?.explanation.orEmpty())
                        },
                        onSource={onOpenParagraph(q.sourceParagraphId)},
                        onContinue={
                            val next=sessionQuestions.getOrNull(sessionIndex+1)
                            if(next!=null) {
                                sessionIndex++
                                start(next)
                            } else {
                                questionId=""
                                sessionFinished=sessionQuestions.isNotEmpty()
                                sessionIds=""
                            }
                        },
                        continueLabel=if(sessionQuestions.isNotEmpty() &&
                            sessionIndex+1<sessionQuestions.size) "Следующий" else "К вопросам"
                    )
                    Spacer(Modifier.height(9.dp))
                    Text("Опорный фрагмент",style=MaterialTheme.typography.titleSmall)
                    Text(q.sourceExcerpt.take(420) +
                        if(q.sourceExcerpt.length>420) "…" else "",
                        style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick={questionId="";sessionIds=""}) {Text("Завершить просмотр")}
            }
        } else when(tab) {
            0 -> {
                val scoped=chapters.filter { inStudyScope(it.id) }
                val completedIds = scoped.filter { c ->
                    val tried=bankByChapter[c.id].orEmpty()
                        .mapNotNull { attempts[it.id] }.filter { it.count>0 }
                    tried.size>=2 && c.id in readIds &&
                        tried.count { it.latestCorrect }.toDouble()/tried.size>=.8
                }.map {it.id}.toSet()
                val workingIds=scoped.filter { c ->
                    c.id !in completedIds &&
                        (c.id in readIds || bankByChapter[c.id].orEmpty()
                            .any { (attempts[it.id]?.count ?: 0)>0 })
                }.map {it.id}.toSet()
                val matchingSections = scoped.filter { c -> when(checksFilter) {
                    "not_read" -> c.id !in readIds && c.id !in workingIds
                    "in_progress" -> c.id in workingIds
                    "completed" -> c.id in completedIds
                    else -> true
                } }
                val checksChapters = if (showAllStudySections || chapterId.isNotBlank())
                    matchingSections else matchingSections.take(3)
                item {
                    HeritageSectionHeading("Разделы книги",
                        trailing="${if(loaded) repo.exercises.count { inStudyScope(it.chapterId) } else 0} заданий")
                    Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        listOf(
                            listOf("all" to "Все","not_read" to "Не прочитано"),
                            listOf("in_progress" to "В работе","completed" to "Завершено")
                        ).forEach { row ->
                            Row(Modifier.fillMaxWidth(),
                                horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                                row.forEach { (id,label) ->
                                    HeritageChoice(label,checksFilter==id,Modifier.weight(1f)) {
                                        checksFilter=id
                                        showAllStudySections=false
                                    }
                                }
                            }
                        }
                    }
                    if(showChecksHelp)
                        Text("Раздел считается изученным по заданиям после двух ответов " +
                            "с долей последних правильных не ниже 80% и отметки чтения. " +
                            "Это учебный ориентир, не научная оценка усвоения.",
                            style=MaterialTheme.typography.bodySmall,color=BookColors.muted)
                    TextButton(onClick={showChecksHelp=!showChecksHelp}) {
                        Text(if(showChecksHelp) "Скрыть пояснение" else "Как считается результат?")
                    }
                }
                if(matchingSections.isEmpty()) item {
                    Text("По этому фильтру пока нет разделов.",
                        style=MaterialTheme.typography.bodyMedium,color=BookColors.muted)
                }
                items(checksChapters,key={"section:${it.id}"}) { c ->
                    val tried=bankByChapter[c.id].orEmpty()
                        .mapNotNull{attempts[it.id]}.filter{it.count>0}
                    val correct=tried.count{it.latestCorrect}
                    val understood=c.id in completedIds
                    val status=when {
                        understood -> "Завершено"
                        c.id in workingIds -> "В работе"
                        else -> "Не прочитано"
                    }
                    val ordinal=scoped.indexOfFirst {it.id==c.id}+1
                    val writingCount=repo.exercises.count {it.chapterId==c.id}
                    var actions by remember {mutableStateOf(false)}
                    AgedPaperCard(Modifier.fillMaxWidth(),
                        colors=CardDefaults.cardColors(
                            containerColor=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.76f)),
                        border=androidx.compose.foundation.BorderStroke(.75.dp,
                            BookColors.gold.copy(alpha=.31f)),
                        shape=androidx.compose.foundation.shape.RoundedCornerShape(19.dp)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=11.dp),
                            horizontalArrangement=Arrangement.spacedBy(10.dp),
                            verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                            HeritageMedallion(ordinal)
                            Column(Modifier.weight(1f),
                                verticalArrangement=Arrangement.spacedBy(5.dp)) {
                                Text(chapterTitles[c.id] ?: c.title,
                                    style=MaterialTheme.typography.titleMedium.copy(
                                        fontFamily=BookSerif,fontSize=17.sp),
                                    color=BookColors.ink)
                                Text("$writingCount заданий · $status",
                                    style=MaterialTheme.typography.labelSmall,color=BookColors.muted)
                                if(tried.isNotEmpty()) Text(
                                    "$correct/${tried.size} последних ответов верны",
                                    style=MaterialTheme.typography.labelSmall,
                                    color=BookColors.muted)
                                Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                                    TextButton(onClick={chapterId=c.id;beginSession(c.id)},
                                        contentPadding=PaddingValues(horizontal=6.dp)) {
                                        Text("Проверить")
                                    }
                                    Box {
                                        TextButton(onClick={actions=true},
                                            contentPadding=PaddingValues(horizontal=6.dp)) {
                                            Text("Ещё")
                                        }
                                        DropdownMenu(expanded=actions,onDismissRequest={actions=false}) {
                                            DropdownMenuItem(text={Text("Письменные задания")},
                                                onClick={actions=false;chapterId=c.id;tab=2;exerciseId=""})
                                            DropdownMenuItem(text={Text("Повторить через 3 дня")},
                                                onClick={actions=false;scope.launch {
                                                    try {
                                                        repo.scheduleChapterReview(c.id)
                                                        chapterReviewMessage="Перечитывание назначено через 3 дня."
                                                    } catch(e:Exception) {
                                                        if(e is CancellationException) throw e
                                                        chapterReviewMessage="Не удалось назначить перечитывание."
                                                    }
                                                }})
                                        }
                                    }
                                }
                                chapterReviews[c.id]?.let { review ->
                                    Text("Перечитать: ${date(review.due)}",
                                        style=MaterialTheme.typography.labelSmall)
                                }
                            }
                            Text("›",style=MaterialTheme.typography.titleLarge,
                                color=BookColors.gold)
                        }
                    }
                }
                if(!showAllStudySections && matchingSections.size>checksChapters.size) item {
                    TextButton(onClick={showAllStudySections=true},
                        modifier=Modifier.fillMaxWidth()) {
                        Text("Показать все ${matchingSections.size} разделов")
                    }
                }
            }
            2 -> {
                item {Text("Ответы сохраняются в личных заметках. Ориентир можно открыть после написания ответа.",
                    style=MaterialTheme.typography.bodySmall)}
                val exercises=repo.exercises.filter{inStudyScope(it.chapterId)}
                if(exercise==null) {
                    items(exercises.take(visibleExercises),key={it.id}) { e ->AgedPaperCard(Modifier.fillMaxWidth()) {Column(Modifier.padding(14.dp)) {Text(chapterTitles[e.chapterId] ?: "Раздел",style=MaterialTheme.typography.labelLarge);Text(e.prompt);TextButton(onClick={exerciseId=e.id;reflection="";hintVisible=false;noteSaved=false}) {Text("Написать ответ")}}} }
                    if(exercises.size>visibleExercises) item {TextButton(onClick={visibleExercises+=15}) {Text("Показать ещё · ${exercises.size-visibleExercises}")}}
                }
                else {
                    item {Text(exercise.prompt,style=MaterialTheme.typography.titleLarge);OutlinedTextField(reflection,{reflection=it;noteSaved=false},label={Text("Ваш ответ")},modifier=Modifier.fillMaxWidth(),minLines=5)}
                    item {Button(enabled=reflection.isNotBlank() && !noteSaved,onClick={vm.addNote(exercise.chapterId,exercise.sourceParagraphId,"Задание: ${exercise.prompt}\n\n$reflection");noteSaved=true}) {Text(if(noteSaved) "Сохранено в заметках" else "Сохранить ответ")};TextButton(enabled=reflection.trim().isNotEmpty(),onClick={hintVisible=true}) {Text("Показать ориентир после ответа")};if(hintVisible) Text(exercise.hint);TextButton(onClick={onOpenParagraph(exercise.sourceParagraphId)}) {Text("Открыть опорный абзац")};TextButton(onClick={exerciseId=""}) {Text("К заданиям")}}
                    items(notes.filter{it.chapterId==exercise.chapterId && it.paragraphId==exercise.sourceParagraphId},key={"note:${it.id}"}) {note ->Card {Text(note.text,Modifier.padding(16.dp))} }
                }
            }
            else -> {
                item {
                    if (tab == 1) {
                        if (sessionFinished) {
                            AgedPaperCard(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp),
                                    verticalArrangement=Arrangement.spacedBy(5.dp)) {
                                    Text("Проверка завершена",style=MaterialTheme.typography.titleMedium)
                                    Text("Правильных ответов: ${sessionCorrect} из ${sessionLastSize}. Ошибки будут доступны в повторении.")
                                    TextButton(onClick={beginSession()}) { Text("Пройти ещё 10 вопросов") }
                                }
                            }
                        }
                        Text("10 вопросов из выбранного материала",style=MaterialTheme.typography.titleMedium)
                        Button(onClick={beginSession()},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {
                            Text("Начать проверку")
                        }
                        if(sessionNotice.isNotBlank()) {
                            Text(sessionNotice,style=MaterialTheme.typography.bodySmall,
                                color=MaterialTheme.colorScheme.error)
                        }
                        TextButton(onClick={showQuizFilters=!showQuizFilters}) {
                            Text(if(showQuizFilters) "Скрыть дополнительные настройки"
                                else "Изменить сложность или тип вопросов")
                        }
                        if(showQuizFilters) {
                        Text("Уровень сложности",style=MaterialTheme.typography.titleSmall)
                        Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                            listOf(
                                listOf("all", "basic"),
                                listOf("advanced", "expert")
                            ).forEach { pair ->
                                Row(Modifier.fillMaxWidth(),
                                    horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                    pair.forEach { v ->
                                        FilterChip(
                                            selected=level==v,
                                            onClick={level=v;visibleQuestions=15;sessionFinished=false},
                                            label={Text(if(v=="all") "Все уровни" else levelLabel(v))},
                                            modifier=Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                            listOf(
                                "all" to "Все",
                                "reasoning" to "Понимание",
                                "recognition" to "Фрагменты"
                            ).forEach { (id,label) ->
                                FilterChip(
                                    selected=questionType==id,
                                    onClick={questionType=id;visibleQuestions=15;sessionFinished=false},
                                    label={Text(label,maxLines=1)},
                                    modifier=Modifier.weight(1f)
                                )
                            }
                        }
                        OutlinedTextField(
                            quizSearch, {quizSearch=it;visibleQuestions=15},
                            modifier=Modifier.fillMaxWidth(),singleLine=true,
                            label={Text("Найти вопрос")}
                        )
                        Text("Узнавание текста — дословные фразы книги. Вопросы на понимание проверяют смысл.",
                            style=MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick={browseQuestions=!browseQuestions}) {
                            Text(if(browseQuestions) "Скрыть каталог вопросов"
                                else "Выбрать отдельный вопрос из каталога")
                        }
                    } else {
                        Text("Повторение: ошибки и неуверенные ответы — на следующий день; уверенные правильные — через 3, 7 и 14 дней.",
                            style=MaterialTheme.typography.bodySmall)
                    }
                }
                val now=System.currentTimeMillis()
                if(tab==3) {
                    val scheduled=chapterReviews.filter {inStudyScope(it.key)}.toList().sortedBy {it.second.due}
                    item {Text("Перечитывание разделов",style=MaterialTheme.typography.titleMedium);Text("Эти задания назначаются отдельно от викторины и не меняют результаты ответов.",style=MaterialTheme.typography.bodySmall)}
                    if(scheduled.isEmpty()) item {Text("Пока нет назначенного перечитывания. Его можно назначить из «Обзора» или во время чтения.")}
                    items(scheduled,key={"chapter-review:${it.first}"}) { (id,review) ->AgedPaperCard(Modifier.fillMaxWidth()) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text(chapterTitles[id] ?: "Раздел $id",style=MaterialTheme.typography.titleMedium)
                        Text("${if(review.due<=now) "Пора перечитать" else "Перечитать"}: ${date(review.due)}")
                        TextButton(onClick={scope.launch {try {val pid=vm.repository.dao.paragraphs(id).firstOrNull()?.id;if(pid!=null) onOpenParagraph(pid) else chapterReviewMessage="Не удалось найти текст раздела."} catch(e:Exception) {if(e is CancellationException) throw e;chapterReviewMessage="Не удалось открыть раздел. Повторите."}}}) {Text("Открыть раздел")}
                        TextButton(onClick={scope.launch {try {repo.clearChapterReview(id);chapterReviewMessage="Перечитывание отмечено выполненным."} catch(e:Exception) {if(e is CancellationException) throw e;chapterReviewMessage="Не удалось сохранить отметку. Повторите."}}}) {Text("Перечитано — завершить")}
                    }} }
                    item {Text("Повторение вопросов",style=MaterialTheme.typography.titleMedium)}
                }
                val filtered=bank.filter { item ->
                    val inChapter=inStudyScope(item.chapterId)
                    if (tab==3) inChapter && (attempts[item.id]?.due ?: Long.MAX_VALUE)<=now
                    else inChapter && (level=="all" || item.level==level) &&
                        (questionType=="all" || (item.id.startsWith("book-recognition-") == (questionType=="recognition"))) &&
                        (quizSearch.isBlank() ||
                            item.prompt.contains(quizSearch,ignoreCase=true) ||
                            chapterTitles[item.chapterId].orEmpty().contains(quizSearch,ignoreCase=true))
                }.let { list ->if(tab==3) list.sortedWith(compareByDescending<StudyQuestion>{attempts[it.id]?.let{a->a.errors.toDouble()/a.count.coerceAtLeast(1)} ?: 0.0}.thenByDescending{attempts[it.id]?.lowConfidence==true}.thenBy{attempts[it.id]?.due ?: 0L}) else list }
                if(tab==1 && browseQuestions) item {
                    Text("Вопросов по выбранным фильтрам: ${filtered.size}",
                        style=MaterialTheme.typography.labelLarge)
                }
                if((tab==3 || browseQuestions) && filtered.isEmpty()) item {
                    Text(if(tab==3) "На сегодня нет вопросов к повторению." else
                        "Подходящих вопросов нет. Сбросьте поиск, уровень или выберите другой раздел.")
                }
                if(tab==3 || browseQuestions) items(filtered.take(visibleQuestions),key={it.id}) { item ->
                    AgedPaperCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(horizontal=14.dp,vertical=11.dp),
                            verticalArrangement=Arrangement.spacedBy(7.dp)) {
                            Text(chapterTitles[item.chapterId] ?: "Раздел",
                                style=MaterialTheme.typography.labelLarge,
                                color=MaterialTheme.colorScheme.primary)
                            Text(if(item.id.startsWith("book-recognition-"))
                                "Узнайте фрагмент · Базовый" else levelLabel(item.level),
                                style=MaterialTheme.typography.labelSmall,
                                color=MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(item.prompt,style=MaterialTheme.typography.bodyLarge)
                            attempts[item.id]?.let { a ->
                                Text("Попыток: ${a.count} · ошибок: ${a.errors} · повторить: ${date(a.due)}",
                                    style=MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick={sessionIds="";sessionFinished=false;start(item)}) {
                                Text("Ответить")
                            }
                        }
                    }
                }
                if((tab==3 || browseQuestions) && filtered.size>visibleQuestions) item {
                    TextButton(onClick={visibleQuestions+=15}) {
                        Text("Показать ещё · ${filtered.size-visibleQuestions}")
                    }
                }
                if(tab==3) {
                    val future=bank.filter{inStudyScope(it.chapterId) && (attempts[it.id]?.due ?: 0L)>now}.sortedBy{attempts[it.id]?.due}
                    if(future.isNotEmpty()) item {
                        TextButton(onClick={showFutureReviews=!showFutureReviews}) {
                            Text(if(showFutureReviews) "Скрыть будущие повторения"
                                else "Будущие повторения · ${future.size}")
                        }
                    }
                    if(showFutureReviews) {
                        items(future.take(visibleQuestions),key={"future:${it.id}"}) {item ->
                            Column {
                                Text(item.prompt,style=MaterialTheme.typography.bodySmall)
                                Text("${date(attempts.getValue(item.id).due)}",style=MaterialTheme.typography.labelMedium)
                                TextButton(onClick={start(item)}) {Text("Повторить раньше")}
                            }
                        }
                        if(future.size>visibleQuestions) item {
                            TextButton(onClick={visibleQuestions+=15}) {
                                Text("Показать ещё · ${future.size-visibleQuestions}")
                            }
                        }
                    }
                    val new=bank.count{inStudyScope(it.chapterId) && attempts[it.id]==null};item {Text("Не пройдено вопросов: $new. Они доступны во вкладке «Вопросы».")}
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
        item {Text("Термины",style=MaterialTheme.typography.headlineSmall);Text("Редакторские пояснения к переводу. Значение терминов уточняется по контексту и первоисточникам.");OutlinedTextField(query,{query=it},label={Text("Поиск термина")},modifier=Modifier.fillMaxWidth())}
        if(!loaded) item {Text(if(error) "Не удалось загрузить термины." else "Загрузка…")}
        else items(repo.terms.filter{query.isBlank() || "${it.ar} ${it.transcript} ${it.definition}".contains(query,ignoreCase=true)},key={it.id}) {term->AgedPaperCard(Modifier.fillMaxWidth()) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {Text(term.ar,fontFamily=FontFamily(Font(R.font.naskh)),fontSize=28.sp);Text(term.transcript,style=MaterialTheme.typography.titleMedium);Text(term.definition);TextButton(onClick={expanded=if(expanded==term.id) "" else term.id}) {Text(if(expanded==term.id) "Свернуть" else "Подробности и связи")};if(expanded==term.id) {Text(term.extendedDefinition);term.relatedTerms.mapNotNull{id->repo.terms.find{it.id==id}}.forEach{related->TextButton(onClick={query=related.transcript;expanded=related.id}) {Text("Связанный термин: ${related.transcript}")}}};term.sourceParagraphIds.ifEmpty{listOf(term.sourceParagraphId)}.forEachIndexed { i,pid->TextButton(onClick={onOpenParagraph(pid)}) {Text(if(i==0) "Открыть контекст" else "Другой фрагмент ${i+1}")}}}} }
    }
}
