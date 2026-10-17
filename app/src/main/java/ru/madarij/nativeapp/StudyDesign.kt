package ru.madarij.nativeapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

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
    scopeLabel: String
) {
    AgedPaperCard(Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text("Мой прогресс", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold)
            Text("Что уже пройдено по выбранному материалу.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            val progress = if (allQuestions > 0) answered.toFloat() / allQuestions else 0f
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(4.dp)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StudyStat("$answered/$allQuestions", "Проверено", Modifier.weight(1.15f))
                StudyStat("$due", "Повторить", Modifier.weight(.8f))
                StudyStat("$readChapters/$allChapters", "Прочитано", Modifier.weight(1.0f))
            }
            Text("$writingTasks письменных заданий · $scopeLabel",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StudyStat(value: String, description: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.titleMedium,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(description, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
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
        Text(heading, style = MaterialTheme.typography.headlineSmall,
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
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
internal fun StudyScopeBar(
    selectedLabel: String,
    onChooseSection: () -> Unit
) {
    OutlinedButton(
        onClick = onChooseSection,
        modifier = Modifier.fillMaxWidth().heightIn(min = 57.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("ИЗУЧАЕМЫЙ МАТЕРИАЛ", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(selectedLabel, maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.width(8.dp))
        Text("Изменить ›", style = MaterialTheme.typography.labelMedium)
    }
}
