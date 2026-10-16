package ru.madarij.nativeapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.madarij.nativeapp.data.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
private fun ProgressCircle(value: Int, maximum: Int, label: String,
                           modifier: Modifier = Modifier) {
    val percent = if (maximum <= 0) 0 else (value * 100 / maximum).coerceIn(0, 100)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(135.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val strokeSize = 16.dp.toPx()
                val side = size.minDimension - strokeSize
                val offset = Offset((size.width - side) / 2, (size.height - side) / 2)
                drawArc(HeritageBronze.copy(alpha = .30f), 0f, 360f,
                    false, offset, Size(side, side), style = Stroke(strokeSize))
                if (percent > 0) drawArc(HeritageBrown, -90f, percent * 3.6f,
                    false, offset, Size(side, side), style = Stroke(strokeSize,
                        cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$percent%", fontFamily = BookSerif, fontSize = 32.sp,
                    color = HeritageBrown)
                Text(if (label == "Прочитано") "◫" else "✓",
                    fontSize = 20.sp, color = HeritageBronze)
            }
        }
        Text("$value / $maximum", fontFamily = BookSerif,
            fontSize = 19.sp, color = HeritageBrown)
        Text(label, fontFamily = BookSerif, fontSize = 18.sp, color = HeritageBrown)
    }
}

@Composable
private fun ProgressDetail(name: String, value: String, icon: String,
                           onClick: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().then(if (onClick == null) Modifier
        else Modifier.clickable(onClick = onClick))
        .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AntiqueBookIcon(icon, Modifier.size(30.dp))
        Text(name, Modifier.weight(1f), fontFamily = BookSerif, fontSize = 16.sp,
            color = HeritageBrown)
        Text(value, fontFamily = BookSerif, fontSize = 16.sp, color = HeritageBrown)
        if (onClick != null) Text("›", fontSize = 23.sp, color = HeritageBronze)
    }
}

private data class ActivityBar(val label: String, val period: String, val minutes: Int)

private fun computeBars(minutes: Map<String, Int>, range: String): List<ActivityBar> {
    val now = LocalDate.now()
    val locale = Locale("ru", "RU")
    return when (range) {
        "month" -> (3 downTo 0).map { back ->
            val end = now.minusWeeks(back.toLong())
            ActivityBar((4 - back).toString() + " нед.", end.toString(),
                (0..6).sumOf { d -> minutes[end.minusDays(d.toLong()).toString()] ?: 0 })
        }
        "all" -> (5 downTo 0).map { back ->
            val month = now.minusMonths(back.toLong()).toString().take(7)
            val label = now.minusMonths(back.toLong())
                .format(DateTimeFormatter.ofPattern("MMM", locale))
            ActivityBar(label, month, minutes.entries.sumOf {
                if (it.key.startsWith(month)) it.value else 0
            })
        }
        else -> (6 downTo 0).map { back ->
            val day = now.minusDays(back.toLong())
            ActivityBar(day.format(DateTimeFormatter.ofPattern("EE", locale)),
                day.toString(), minutes[day.toString()] ?: 0)
        }
    }
}

@Composable
private fun ActivityGraph(bars: List<ActivityBar>, selected: Int,
                          onSelected: (Int) -> Unit) {
    val maximum = (bars.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(1)
    Row(Modifier.fillMaxWidth().height(148.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom) {
        bars.forEachIndexed { index, day ->
            Column(Modifier.weight(1f).fillMaxHeight()
                .clickable { onSelected(index) },
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(day.minutes.toString(), style = MaterialTheme.typography.labelSmall,
                    color = HeritageMuted)
                Spacer(Modifier.height(4.dp))
                val height = (12f + 88f * day.minutes / maximum).dp
                Box(Modifier.fillMaxWidth(.70f).height(height)
                    .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                    .background(if (index == selected) HeritageBrown
                        else HeritageBronze.copy(alpha = .75f)))
                Spacer(Modifier.height(4.dp))
                Text(day.label, style = MaterialTheme.typography.labelSmall,
                    color = HeritageBrown, maxLines = 1)
            }
        }
    }
}

@Composable
internal fun ProgressScreen(
    vm: BookViewModel,
    open: (String, String?) -> Unit,
    navigate: (String) -> Unit
) {
    val chapters by vm.chapters.collectAsStateWithLifecycle()
    val read by vm.read.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val learning = remember(context) { LearningRepository(context.applicationContext) }
    val attempts by learning.attempts.collectAsStateWithLifecycle(emptyMap())
    var totals by remember { mutableStateOf(0 to 0) }
    LaunchedEffect(learning) {
        totals = withContext(Dispatchers.IO) {
            learning.questions.size to learning.exercises.size
        }
    }
    val readIds = remember(read) { read.map { it.chapterId }.toSet() }
    val readCount = remember(chapters, readIds) {
        chapters.count { it.id in readIds }
    }
    val roots = remember(chapters) { LogicalReadingSections.roots(chapters) }
    val completedRoots = remember(roots, chapters, readIds) {
        roots.count { root ->
            val members = LogicalReadingSections.members(chapters, root.id)
            members.isNotEmpty() && members.all { it.id in readIds }
        }
    }
    val minutesByDate = remember(history) {
        history.groupBy { it.date }.mapValues { (_, visits) ->
            visits.sumOf { it.seconds }.coerceAtLeast(0) / 60
        }
    }
    val totalMinutes = remember(history) { history.sumOf { it.seconds }.coerceAtLeast(0) / 60 }
    val lastAnswer = if (attempts.isEmpty()) "—" else {
        (attempts.values.count { it.latestCorrect } * 100 / attempts.size).toString() + "%"
    }
    var tab by rememberSaveable { mutableStateOf("overview") }
    var period by rememberSaveable { mutableStateOf("week") }
    var selected by rememberSaveable(period) { mutableIntStateOf(1000) }
    val bars = remember(minutesByDate, period) { computeBars(minutesByDate, period) }
    val chosen = selected.coerceIn(0, (bars.size - 1).coerceAtLeast(0))

    LazyColumn(contentPadding = PaddingValues(start = 15.dp, end = 15.dp,
        top = 10.dp, bottom = 125.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Row(Modifier.fillMaxWidth().clip(HeritageShape)
                .border(.8.dp, HeritageBronze, HeritageShape)
                .background(HeritageCream.copy(alpha = .72f))) {
                listOf("overview" to "Общий", "sections" to "По разделам")
                    .forEach { (key, name) ->
                        Box(Modifier.weight(1f).clip(HeritageShape)
                            .background(if (tab == key) HeritageBrown else Color.Transparent)
                            .clickable { tab = key }.padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center) {
                            Text(name, fontFamily = BookSerif, fontSize = 19.sp,
                                color = if (tab == key) HeritageCream else HeritageBrown)
                        }
                    }
            }
        }
        if (tab == "overview") {
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly) {
                    ProgressCircle(readCount, chapters.size, "Прочитано", Modifier.weight(1f))
                    ProgressCircle(attempts.size, totals.first, "Проверено", Modifier.weight(1f))
                }
            }
            item {
                Soft3DPanel(Modifier.fillMaxWidth(), shape = HeritageShape, warm = true) {
                    Column {
                        ProgressDetail("Завершено разделов",
                            "$completedRoots из " + roots.size, "contents")
                        HorizontalDivider(color = HeritageBronze.copy(alpha = .25f))
                        ProgressDetail("Письменные задания",
                            totals.second.toString() + " доступно", "study",
                            { navigate("study") })
                        HorizontalDivider(color = HeritageBronze.copy(alpha = .25f))
                        ProgressDetail("Последний результат", lastAnswer, "progress",
                            { navigate("study") })
                        HorizontalDivider(color = HeritageBronze.copy(alpha = .25f))
                        ProgressDetail("Время чтения", "$totalMinutes мин", "progress")
                    }
                }
            }
            item {
                Text("Активность", Modifier.fillMaxWidth(),
                    fontFamily = BookSerif, fontSize = 26.sp,
                    color = HeritageBrown, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("week" to "Неделя", "month" to "Месяц",
                        "all" to "Все время").forEach { (key, name) ->
                        Box(Modifier.weight(1f).clip(HeritageShape)
                            .background(if (period == key) HeritageBrown
                                else HeritageCream.copy(alpha = .75f))
                            .border(.6.dp, HeritageBronze.copy(alpha = .4f), HeritageShape)
                            .clickable { period = key; selected = 1000 }
                            .padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                            Text(name, fontFamily = BookSerif, fontSize = 16.sp,
                                color = if (period == key) HeritageCream else HeritageBrown)
                        }
                    }
                }
            }
            item {
                Soft3DPanel(Modifier.fillMaxWidth(), shape = HeritageShape, warm = true) {
                    Column(Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ActivityGraph(bars, chosen) { selected = it }
                        HorizontalDivider(color = HeritageBronze.copy(alpha = .32f))
                        Text("Период: " + bars.getOrNull(chosen)?.period.orEmpty(),
                            fontFamily = BookSerif, fontSize = 18.sp, color = HeritageBrown)
                        Text("Время чтения: " + (bars.getOrNull(chosen)?.minutes ?: 0) + " мин",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HeritageMuted)
                        if (totalMinutes == 0) Text("История чтения пока пуста.",
                            style = MaterialTheme.typography.bodySmall, color = HeritageMuted)
                    }
                }
            }
        } else {
            item {
                Text("Завершённые разделы учитываются по отметкам фактического чтения.",
                    style = MaterialTheme.typography.bodySmall, color = HeritageMuted)
            }
            items(roots, key = { "root:" + it.id }) { root ->
                val members = remember(chapters, root.id) {
                    LogicalReadingSections.members(chapters, root.id)
                }
                val done = members.count { it.id in readIds }
                val percent = if (members.isEmpty()) 0 else done * 100 / members.size
                Soft3DPanel(Modifier.fillMaxWidth(), shape = HeritageShape,
                    onClick = { open(root.id, null) }) {
                    Column(Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Том " + root.volume, style = MaterialTheme.typography.labelSmall,
                                color = HeritageMuted)
                            Text(root.title, Modifier.weight(1f), fontFamily = BookSerif,
                                fontSize = 18.sp, color = HeritageBrown)
                            Text("›", fontSize = 23.sp, color = HeritageBronze)
                        }
                        LinearProgressIndicator(progress = { percent / 100f },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = HeritageBrown,
                            trackColor = HeritageBronze.copy(alpha = .22f))
                        Text("$done из " + members.size + " фрагментов · $percent%",
                            style = MaterialTheme.typography.labelMedium,
                            color = HeritageMuted)
                    }
                }
            }
        }
    }
}
