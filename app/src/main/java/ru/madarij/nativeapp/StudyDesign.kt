package ru.madarij.nativeapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * All learning visuals use the application's existing aged-paper cards,
 * brown/gold colors and serif system. No new backgrounds or graphics layers.
 */
@Composable
internal fun StudyOverview(
    readChapters: Int,
    allChapters: Int,
    answered: Int,
    allQuestions: Int,
    due: Int,
    writingTasks: Int,
    scopeLabel: String,
    onStart: () -> Unit = {}
) {
    AgedPaperCard(Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(.8.dp,
            BookColors.gold.copy(alpha = .3f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.82f))) {
        Column(Modifier.padding(horizontal=15.dp,vertical=12.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Мой путь по книге",
                style=MaterialTheme.typography.headlineSmall.copy(
                    fontFamily=BookSerif,fontSize=22.sp),color=heritageInk())
            Text("Читайте, проверяйте понимание и возвращайтесь к трудным вопросам.",
                style=MaterialTheme.typography.bodyMedium,color=heritageMuted())
            HorizontalDivider(color=BookColors.gold.copy(alpha=.23f))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(11.dp)) {
                Column(Modifier.weight(1f),
                    verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        NavigationGlyph("contents",modifier=Modifier.size(26.dp),
                            tint=BookColors.gold)
                        Column {
                            Text("$readChapters / $allChapters",
                                style=MaterialTheme.typography.titleLarge.copy(
                                    fontFamily=BookSerif,fontSize=19.sp),color=heritageInk())
                            Text("Прочитано",style=MaterialTheme.typography.labelMedium,
                                color=heritageMuted())
                        }
                    }
                    LinearProgressIndicator(
                        progress={if(allChapters==0) 0f else readChapters.toFloat()/allChapters},
                        modifier=Modifier.fillMaxWidth().height(4.dp),
                        color=BookColors.gold,
                        trackColor=BookColors.gold.copy(alpha=.17f))
                }
                Column(Modifier.weight(1f),
                    verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        NavigationGlyph("study",modifier=Modifier.size(26.dp),
                            tint=BookColors.gold)
                        Column {
                            Text("$answered / $allQuestions",
                                style=MaterialTheme.typography.titleLarge.copy(
                                    fontFamily=BookSerif,fontSize=19.sp),color=heritageInk())
                            Text("Проверено",style=MaterialTheme.typography.labelMedium,
                                color=heritageMuted())
                        }
                    }
                    LinearProgressIndicator(
                        progress={if(allQuestions==0) 0f else answered.toFloat()/allQuestions},
                        modifier=Modifier.fillMaxWidth().height(4.dp),
                        color=BookColors.gold,
                        trackColor=BookColors.gold.copy(alpha=.17f))
                }
            }
            Text("$writingTasks письменных заданий · $scopeLabel",
                style = MaterialTheme.typography.labelSmall,
                color = heritageMuted())
            Button(onClick=onStart,enabled=allQuestions>0,
                modifier=Modifier.fillMaxWidth().heightIn(min=CompactUi.actionMinHeightDp.dp),
                shape=androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                colors=ButtonDefaults.buttonColors(
                    containerColor=BookColors.leather,
                    contentColor=androidx.compose.ui.graphics.Color(0xFFFFEBD1))) {
                Text("▶   Начать проверку · 10 вопросов   ›",
                    fontFamily=BookSerif,fontSize=16.sp)
            }
            if (due > 0) Text("К повторению: $due вопросов",
                style=MaterialTheme.typography.labelSmall,color=heritageMuted())
        }
    }
}

@Composable
internal fun StudyModeGrid(selectedTab: Int,
    onSections: () -> Unit, onQuiz: () -> Unit,
    onWriting: () -> Unit, onReview: () -> Unit) {
    HeritageSectionHeading("Режим изучения")
    val actions=listOf(
        Triple("Разделы", "contents", onSections),
        Triple("Викторина", "study", onQuiz),
        Triple("Письменные", "notes", onWriting),
        Triple("Повторение", "more", onReview)
    )
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        (0..1).forEach { row ->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                (0..1).forEach { column ->
                    val index=row*2+column
                    val (title,icon,action)=actions[index]
                    AgedPaperCard(onClick=action,modifier=Modifier.weight(1f),
                        colors=CardDefaults.cardColors(
                            containerColor=if(selectedTab==index)
                                BookColors.gold.copy(alpha=.24f)
                            else BookColors.card.copy(alpha=.56f)),
                        border=androidx.compose.foundation.BorderStroke(
                            if(selectedTab==index) 1.2.dp else .7.dp,
                            BookColors.gold.copy(alpha=if(selectedTab==index) .72f else .3f)),
                        shape=androidx.compose.foundation.shape.RoundedCornerShape(18.dp)) {
                        Row(Modifier.fillMaxWidth().heightIn(min=58.dp)
                            .padding(horizontal=9.dp,vertical=7.dp),
                            verticalAlignment=Alignment.CenterVertically,
                            horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                            NavigationGlyph(icon,modifier=Modifier.size(24.dp),
                                tint=BookColors.gold)
                            Text(title,Modifier.weight(1f),
                                style=MaterialTheme.typography.titleSmall.copy(
                                    fontFamily=BookSerif,fontSize=16.sp),
                                color=heritageInk(),maxLines=2,
                                overflow=TextOverflow.Ellipsis)
                            Text("›",style=MaterialTheme.typography.titleLarge,
                                color=BookColors.gold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Three obvious tasks, one tap each. There is no redundant 2x2 tab grid.
 * Each task opens its own uncluttered screen.
 */
@Composable
internal fun StudyMainActions(
    availableQuestions: Int,
    writingTasks: Int,
    due: Int,
    onQuiz: () -> Unit,
    onWriting: () -> Unit,
    onReview: () -> Unit
) {
    Text("Как будем заниматься?", style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold)
    StudyActionRow(
        title = "Начать проверку · 10 вопросов",
        subtitle = "Вопросы по выбранному материалу · всего $availableQuestions",
        marker = "01", prominent = true,
        enabled = availableQuestions > 0, onClick = onQuiz
    )
    StudyActionRow(
        title = "Ответить письменно",
        subtitle = "$writingTasks заданий · ответ сохранится в заметках",
        marker = "02", enabled = writingTasks > 0, onClick = onWriting
    )
    StudyActionRow(
        title = "Повторить материал",
        subtitle = if (due > 0) "$due вопросов ждут повторения"
            else "Проверить ошибки и назначенные перечитывания",
        marker = "03", enabled = true, onClick = onReview
    )
}

@Composable
private fun StudyActionRow(title: String, subtitle: String, marker: String,
    prominent: Boolean = false, enabled: Boolean, onClick: () -> Unit
) {
    AgedPaperCard(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (prominent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .58f)
                else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(marker, style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary, fontFamily = BookSerif)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** A mode has one exit to Overview, not a second navigation system. */
@Composable
internal fun StudyModeHeader(tab: Int, onBack: () -> Unit) {
    val (heading, description) = when (tab) {
        1 -> "Викторина" to "Проверьте знания по выбранному материалу."
        2 -> "Письменные задания" to "Записывайте свои ответы и сверяйте с книгой."
        else -> "Повторение" to "Вернитесь к трудным вопросам и разделам."
    }
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        TextButton(onClick = onBack,
            contentPadding = PaddingValues(horizontal = 0.dp)) {
            Text("‹ К обзору изучения")
        }
        Text(heading, style = MaterialTheme.typography.titleLarge,
            fontFamily = BookSerif)
        Text(description, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun StudyQuestionHeading(
    chapter: String,
    level: String,
    ordinal: Int?,
    total: Int?,
    category: String
) {
    AgedPaperCard(Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(chapter, style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("$level · $category", style = MaterialTheme.typography.labelSmall)
                if (ordinal != null && total != null) {
                    Text("$ordinal / $total", style = MaterialTheme.typography.labelLarge)
                }
            }
            if (ordinal != null && total != null && total > 0)
                LinearProgressIndicator(
                    progress = { ((ordinal - 1).toFloat() / total).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(3.dp)
                )
        }
    }
}

@Composable
internal fun StudyAnswerFeedback(
    isCorrect: Boolean,
    detail: String,
    onSource: () -> Unit,
    onContinue: () -> Unit,
    continueLabel: String
) {
    AgedPaperCard(Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (isCorrect) "Верно по тексту книги" else "Стоит повторить",
                style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onSource) { Text("Открыть источник") }
                Button(onClick = onContinue) { Text(continueLabel) }
            }
        }
    }
}

/**
 * Single, consistent book/section selector shown across study modes.
 * The same selection applies to the overview, quiz, writing and revisions.
 */
/**
 * One compact selector instead of three volume chips AND a section picker.
 * Both parameters are a unified scope inside the bottom sheet.
 */
@Composable
internal fun StudyScopeBar(selectedLabel: String, onChooseSection: () -> Unit) {
    HeritageSectionHeading("Изучаемый материал")
    AgedPaperCard(onClick=onChooseSection,modifier=Modifier.fillMaxWidth(),
        shape=androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        border=androidx.compose.foundation.BorderStroke(.9.dp,
            BookColors.gold.copy(alpha=.4f)),
        colors=CardDefaults.cardColors(containerColor=BookColors.card.copy(alpha=.75f))) {
        Row(Modifier.fillMaxWidth().heightIn(min=52.dp)
            .padding(horizontal=11.dp,vertical=7.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            NavigationGlyph("contents",modifier=Modifier.size(25.dp),
                tint=BookColors.gold)
            Text(selectedLabel,Modifier.weight(1f),
                style=MaterialTheme.typography.bodyMedium.copy(
                    fontFamily=BookSerif,fontSize=16.sp),
                color=BookColors.ink,maxLines=2,overflow=TextOverflow.Ellipsis)
            Text("⌄",style=MaterialTheme.typography.titleLarge,
                color=BookColors.gold)
        }
    }
}
