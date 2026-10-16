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
    var chapterSearch by rememberSaveable { mutableStateOf("") }
    var sessionNotice by remember { mutableStateOf("") }
    var checksFilter by rememberSaveable { mutableStateOf("in_progress") }
    var showChecksHelp by rememberSaveable { mutableStateOf(false) }
    var visibleQuestions by rememberSaveable { mutableIntStateOf(15) }
    var visibleExercises by rememberSaveable { mutableIntStateOf(15) }
    var exerciseId by rememberSaveable { mutableStateOf("") }
    var reflection by rememberSaveable { mutableStateOf("") }
    var hintVisible by rememberSaveable { mutableStateOf(false) }
    var noteSaved by rememberSaveable { mutableStateOf(false) }
    val bank=if(loaded) repo.questions else emptyList()
    val bankByChapter=remember(bank) { bank.groupBy { it.chapterId } }
    val chapterVolumes=remember(chapters) { chapters.associate { it.id to it.volume } }
    val readIds=remember(read) { read.map { it.chapterId }.toSet() }
    fun inStudyScope(id: String): Boolean =
        studyScopeMatches(id, chapterId, selectedVolume, chapterVolumes)
    val dueQuestions=attempts.count { it.value.due<=System.currentTimeMillis() }
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
    fun beginSession() {
        val eligible = bank.filter { item ->
            inStudyScope(item.chapterId) &&
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
        val normalizedSearch=remember(chapterSearch) { normalizedText(chapterSearch).text }
        val matches=remember(chapters, chapterTitles, selectedVolume, normalizedSearch) {
            chapters.filter { c ->
                (selectedVolume == 0 || c.volume == selectedVolume) &&
                    (normalizedSearch.isEmpty() ||
                        normalizedText(chapterTitles[c.id] ?: c.title).text.contains(normalizedSearch))
            }.sortedWith(compareBy({ it.volume }, { it.ordinal }))
        }
        AlertDialog(
            onDismissRequest={chapterMenu=false},
            title={Text("Выбор раздела")},
            text={
                Column(verticalArrangement=Arrangement.spacedBy(9.dp)) {
                    Text(
                        when(selectedVolume) {
                            1 -> "Том 1 · ${matches.size} разделов"
                            2 -> "Том 2 · ${matches.size} разделов"
                            else -> "Оба тома · ${matches.size} разделов"
                        },
                        style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        chapterSearch, {chapterSearch=it},
                        modifier=Modifier.fillMaxWidth(),
                        singleLine=true, label={Text("Найти раздел")}
                    )
                    LazyColumn(Modifier.heightIn(max=390.dp),
                        verticalArrangement=Arrangement.spacedBy(3.dp)) {
                        item {
                            TextButton(
                                onClick={
                                    chapterId=""
                                    checksFilter="in_progress"
                                    exerciseId=""
                                    questionId=""
                                    chapterMenu=false
                                    chapterSearch=""
                                },
                                modifier=Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if(selectedVolume==0) "✓ Все разделы обоих томов"
                                    else "✓ Все разделы тома $selectedVolume",
                                    modifier=Modifier.fillMaxWidth()
                                )
                            }
                        }
                        for (volume in 1..2) {
                            val bookChapters=matches.filter { it.volume==volume }
                            if(bookChapters.isNotEmpty()) {
                                item(key="vol-header:$volume") {
                                    Text("ТОМ $volume · ${bookChapters.size} разделов",
                                        style=MaterialTheme.typography.labelMedium,
                                        color=MaterialTheme.colorScheme.primary,
                                        modifier=Modifier.padding(top=9.dp, bottom=3.dp))
                                }
                                items(bookChapters,key={it.id}) { c ->
                                    TextButton(
                                        onClick={
                                            chapterId=c.id
                                            selectedVolume=c.volume
                                            checksFilter="in_progress"
                                            questionId=""
                                            exerciseId=""
                                            reflection=""
                                            hintVisible=false
                                            chapterMenu=false
                                            chapterSearch=""
                                            visibleQuestions=15
                                            visibleExercises=15
                                        },
                                        modifier=Modifier.fillMaxWidth(),
                                        contentPadding=PaddingValues(horizontal=9.dp,vertical=8.dp)
                                    ) {
                                        Column(Modifier.fillMaxWidth()) {
                                            Text(chapterTitles[c.id] ?: c.title,
                                                maxLines=3,
                                                style=MaterialTheme.typography.bodyMedium)
                                            Text(
                                                "${bankByChapter[c.id]?.size ?: 0} вопросов",
                                                style=MaterialTheme.typography.labelSmall,
                                                color=MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if(matches.isEmpty()) item {
                            Text("Ничего не найдено. Измените поисковый запрос.")
                        }
                    }
                }
            },
            confirmButton={
                TextButton(onClick={chapterMenu=false}) {Text("Закрыть")}
            }
        )
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=16.dp),
        state=listState,
        verticalArrangement=Arrangement.spacedBy(10.dp),
        contentPadding=PaddingValues(start=0.dp,top=12.dp,end=0.dp,bottom=124.dp)) {
        // No large dashboard, duplicated mode tabs or section-picker on a
        // question or while composing a written answer.
        if(tab==0 && q==null && exercise==null) item {
            StudyOverview(
                readChapters=readIds.size,
                allChapters=chapters.size,
                answered=attempts.count { it.value.count>0 },
                allQuestions=bank.size,
                due=dueQuestions,
                writingTasks=if(loaded) repo.exercises.size else 0,
                onStart={if(loaded) beginSession()}
            )
        }
        if(chapterReviewMessage.isNotBlank()) item {
            Text(chapterReviewMessage,style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.primary)
        }
        if(sessionNotice.isNotBlank()) item {
            Text(sessionNotice,style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.primary)
        }
        if(q==null && exercise==null) {
            item {
                StudyScopeBar(
                    selectedLabel=chapterTitles[chapterId] ?:
                        when(selectedVolume) {
                            1 -> "Все разделы тома 1"
                            2 -> "Все разделы тома 2"
                            else -> "Все разделы"
                        },
                    selectedVolume=selectedVolume,
                    onChooseSection={
                        chapterSearch=""
                        chapterMenu=true
                    },
                    onSelectVolume={ volume ->
                        selectedVolume=volume
                        checksFilter="in_progress"
                        chapterId=selectedStudyChapterAfterVolumeChange(
                            chapterId,volume,chapterVolumes)
                        questionId=""
                        exerciseId=""
                        visibleQuestions=15
                        visibleExercises=15
                        sessionFinished=false
                    }
                )
            }
            item {
                StudyModeTabs(tab) { selected ->
                    tab=selected
                    questionId=""
                    sessionIds=""
                    sessionFinished=false
                    visibleQuestions=15
                    visibleExercises=15
                }
            }
        } else item {
            TextButton(onClick={
                if(q!=null) {
                    questionId=""
                    sessionIds=""
                } else exerciseId=""
            }) {
                Text(if(q!=null) "‹ Вернуться к вопросам" else "‹ К письменным заданиям")
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
                val activeIds=readIds+listOfNotNull(last?.chapterId)
                val scoped=chapters.filter { inStudyScope(it.id) }
                // Show a few actionable chapters by default, not all 99.
                val checksChapters=when {
                    chapterId.isNotEmpty() -> scoped
                    checksFilter=="all" -> scoped
                    activeIds.isEmpty() -> scoped.take(3)
                    else -> scoped.filter { it.id in activeIds }.ifEmpty { scoped.take(3) }
                }
                item {
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement=Arrangement.SpaceBetween,
                        verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                        Text(
                            if(checksFilter=="all") "Все разделы · ${scoped.size}"
                            else if(activeIds.isEmpty()) "С чего начать"
                            else "Мои разделы",
                            style=MaterialTheme.typography.titleMedium
                        )
                        if(chapterId.isEmpty()) {
                            TextButton(onClick={
                                checksFilter=if(checksFilter=="all") "in_progress" else "all"
                            }) {
                                Text(if(checksFilter=="all") "Свернуть" else "Все ${scoped.size}")
                            }
                        }
                    }
                    if(chapterId.isEmpty() && activeIds.isEmpty() && checksFilter!="all")
                        Text("Выберите тему и начните проверку. Полный список доступен по кнопке «Все».",
                            style=MaterialTheme.typography.bodySmall)
                    TextButton(onClick={showChecksHelp=!showChecksHelp}) {
                        Text(if(showChecksHelp) "Скрыть объяснение" else "Что значит «Проверено»?")
                    }
                    if(showChecksHelp) Text(
                        "Раздел считается освоенным по вопросам после двух ответов с точностью не ниже 80%. Это учебный показатель, а не научная оценка понимания.",
                        style=MaterialTheme.typography.bodySmall)
                }
                items(checksChapters,key={"section:${it.id}"}) { c ->
                    val questions=bankByChapter[c.id].orEmpty(); val tried=questions.mapNotNull{attempts[it.id]}.filter{it.count>0};val correct=tried.count{it.latestCorrect};val understood=tried.size>=2 && correct.toDouble()/tried.size>=0.8
                    var actions by remember {mutableStateOf(false)}
                    AgedPaperCard(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)) {Column(Modifier.padding(horizontal=14.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
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
                        Text("Быстрая проверка · 10 случайных вопросов",style=MaterialTheme.typography.titleMedium)
                        Button(onClick={beginSession()},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {
                            Text("Начать 10 вопросов")
                        }
                        TextButton(onClick={showQuizFilters=!showQuizFilters}) {
                            Text(if(showQuizFilters) "Скрыть настройку вопросов"
                                else "Настроить уровень, тип и поиск")
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
                if(tab==1) item {
                    Text("Вопросов по выбранным фильтрам: ${filtered.size}",
                        style=MaterialTheme.typography.labelLarge)
                }
                if(filtered.isEmpty()) item {
                    Text(if(tab==3) "На сегодня нет вопросов к повторению." else
                        "Подходящих вопросов нет. Сбросьте поиск, уровень или выберите другой раздел.")
                }
                items(filtered.take(visibleQuestions),key={it.id}) { item ->
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
                if(filtered.size>visibleQuestions) item {TextButton(onClick={visibleQuestions+=15}) {Text("Показать ещё · ${filtered.size-visibleQuestions}")}}
                if(tab==3) {
                    val future=bank.filter{inStudyScope(it.chapterId) && (attempts[it.id]?.due ?: 0L)>now}.sortedBy{attempts[it.id]?.due}
                    if(future.isNotEmpty()) item {Text("Следующие повторения",style=MaterialTheme.typography.titleMedium)}
                    items(future.take(visibleQuestions),key={"future:${it.id}"}) {item->Column {Text(item.prompt,style=MaterialTheme.typography.bodySmall);Text("${date(attempts.getValue(item.id).due)}",style=MaterialTheme.typography.labelMedium);TextButton(onClick={start(item)}) {Text("Повторить раньше")}}}
                    if(future.size>visibleQuestions) item {TextButton(onClick={visibleQuestions+=15}) {Text("Показать ещё повторения · ${future.size-visibleQuestions}")}}
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
