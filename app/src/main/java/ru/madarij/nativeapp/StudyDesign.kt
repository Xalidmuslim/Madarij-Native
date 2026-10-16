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
    onStart: () -> Unit
) {
    AgedPaperCard(Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text("Мой путь по книге", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold)
            Text("Читайте, проверяйте понимание и возвращайтесь к трудным вопросам.",
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
            Text("Вопросы на понимание и узнавание текста · $writingTasks письменных заданий",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onStart, enabled = allQuestions > 0,
                modifier = Modifier.fillMaxWidth().heightIn(min = 45.dp)) {
                Text("Начать проверку · 10 вопросов")
            }
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

@Composable
internal fun StudyModeTabs(tab: Int, onSelect: (Int) -> Unit) {
    // Two rows instead of a cropped horizontally scrolling strip.
    val names = listOf("Разделы", "Викторина", "Письменные", "Повторение")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in 0..1) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (column in 0..1) {
                    val index = row * 2 + column
                    FilterChip(
                        selected = tab == index,
                        onClick = { onSelect(index) },
                        label = {
                            Text(names[index], maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 43.dp)
                    )
                }
            }
        }
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
