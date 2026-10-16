package ru.madarij.nativeapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.madarij.nativeapp.data.Chapter

/**
 * One native, searchable bottom sheet for both volume and chapter selection.
 * No dialog nested inside another scrollable Study screen; the long book index
 * is virtualized and the chosen scope applies to all three learning modes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StudyChapterPicker(
    selectedVolume: Int,
    selectedChapterId: String,
    chapters: List<Chapter>,
    chapterTitles: Map<String, String>,
    questionCounts: Map<String, Int>,
    readIds: Set<String>,
    onSelectAll: (Int) -> Unit,
    onSelectChapter: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var search by rememberSaveable { mutableStateOf("") }
    var showAllChapters by rememberSaveable { mutableStateOf(false) }
    var pickerVolume by rememberSaveable {
        mutableIntStateOf(chapters.firstOrNull { it.id == selectedChapterId }?.volume
            ?: selectedVolume)
    }
    val matches = remember(chapters, chapterTitles, pickerVolume, search) {
        val needle = normalizedText(search.trim()).text
        chapters.asSequence().filter { chapter ->
            (pickerVolume == 0 || chapter.volume == pickerVolume) &&
                (needle.isBlank() || normalizedText(
                    chapterTitles[chapter.id] ?: chapter.title
                ).text.contains(needle))
        }.sortedWith(compareBy({ it.volume }, { it.ordinal })).toList()
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Что будем изучать?", style = MaterialTheme.typography.headlineSmall,
                fontFamily = BookSerif)
            Text("1. Выберите том. 2. Выберите тему или оставьте весь том.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(0 to "Оба тома", 1 to "Том 1", 2 to "Том 2").forEach { (volume, label) ->
                    FilterChip(
                        selected = pickerVolume == volume,
                        onClick = { pickerVolume = volume },
                        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.weight(1f).heightIn(min = 45.dp)
                    )
                }
            }
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Поиск по названию") },
                placeholder = { Text("Например, покаяние") }
            )
            Button(onClick = { onSelectAll(pickerVolume) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(if (pickerVolume == 0) "Выбрать оба тома целиком"
                     else "Выбрать весь том $pickerVolume")
            }
            HorizontalDivider(color = BookColors.gold.copy(alpha = .3f))
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Или выберите отдельную тему",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface)
                Text("${matches.size} тем",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (search.isBlank() && matches.size > 12) {
                TextButton(onClick = { showAllChapters = !showAllChapters }) {
                    Text(if (showAllChapters) "Свернуть список"
                        else "Показать все темы · ${matches.size}")
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                for (volume in 1..2) {
                    val pages = matches.filter { it.volume == volume }
                    val visiblePages = studyPickerPreview(
                        pages, searching = search.isNotBlank(), expanded = showAllChapters
                    )
                    if (pages.isNotEmpty()) {
                        item(key = "v:$volume") {
                            Text("ТОМ $volume",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 10.dp, bottom = 5.dp))
                        }
                        items(visiblePages, key = { it.id }) { chapter ->
                            TextButton(
                                onClick = { onSelectChapter(chapter.id, chapter.volume) },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 9.dp)
                            ) {
                                Column(Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        (if (chapter.id == selectedChapterId) "✓ " else "") +
                                            (chapterTitles[chapter.id] ?: chapter.title),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if(chapter.id == selectedChapterId)
                                            FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    Text(
                                        "${questionCounts[chapter.id] ?: 0} вопросов" +
                                            if(chapter.id in readIds) " · Прочитано" else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        if (visiblePages.size < pages.size) item(key = "more:$volume") {
                            TextButton(
                                onClick = { showAllChapters = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Ещё темы тома $volume · ${pages.size-visiblePages.size}")
                            }
                        }
                    }
                }
                if(matches.isEmpty()) item {
                    Text("Раздел не найден. Измените запрос или выберите другой том.",
                        modifier = Modifier.padding(vertical = 20.dp),
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
