package ru.madarij.nativeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Pure Compose decorations using the existing book resources; not screenshots.
internal val HeritageBrown = Color(0xFF56331E)
internal val HeritageBronze = Color(0xFFA88252)
internal val HeritageCream = Color(0xFFF5E8CF)
internal val HeritageMuted = Color(0xFF80694D)
internal val HeritageShape = RoundedCornerShape(19.dp)

@Composable
internal fun HeritageMedallion(number: Int, modifier: Modifier = Modifier) {
    Box(modifier.border(1.2.dp, HeritageBronze, CircleShape).padding(4.dp)
        .clip(CircleShape).background(HeritageBrown)
        .border(.8.dp, Color(0xFFCDAA77), CircleShape),
        contentAlignment = Alignment.Center) {
        Text(number.toString(), fontFamily = BookSerif, fontSize = 25.sp,
            color = HeritageCream)
    }
}

private data class MenuItem(
    val route: String, val title: String, val subtitle: String, val glyph: String
)

@Composable
private fun MenuGroup(heading: String, entries: List<MenuItem>, navigate: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(heading, style = MaterialTheme.typography.labelLarge.copy(
            fontFamily = BookSerif, fontWeight = FontWeight.Normal, fontSize = 15.sp),
            color = HeritageBrown, modifier = Modifier.padding(start = 8.dp))
        Soft3DPanel(modifier = Modifier.fillMaxWidth(), shape = HeritageShape, warm = true) {
            Column {
                entries.forEachIndexed { index, entry ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 76.dp)
                        .clickable { navigate(entry.route) }
                        .padding(horizontal = 17.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                        AntiqueBookIcon(entry.glyph, Modifier.size(39.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(entry.title, fontFamily = BookSerif, fontSize = 19.sp,
                                color = HeritageBrown, maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                            Text(entry.subtitle, style = MaterialTheme.typography.bodySmall,
                                color = HeritageMuted, maxLines = 2)
                        }
                        Text("›", fontSize = 28.sp, fontFamily = BookSerif, color = HeritageBrown)
                    }
                    if (index != entries.lastIndex) HorizontalDivider(
                        Modifier.padding(horizontal = 15.dp), thickness = .6.dp,
                        color = HeritageBronze.copy(alpha = .29f))
                }
            }
        }
    }
}

@Composable
internal fun MoreScreen(navigate: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(start = 17.dp, end = 17.dp,
        top = 18.dp, bottom = 125.dp), verticalArrangement = Arrangement.spacedBy(21.dp)) {
        item { MenuGroup("ИЗУЧЕНИЕ", listOf(
            MenuItem("study", "Изучение", "Проверки, задания и повторение", "study"),
            MenuItem("progress", "Мой прогресс", "Чтение, изучение и повторение", "progress")
        ), navigate) }
        item { MenuGroup("ЛИЧНОЕ", listOf(
            MenuItem("bookmarks", "Закладки", "Сохранённые главы, цитаты и места", "bookmarks"),
            MenuItem("notes", "Мои заметки", "Личные записи и размышления", "notes")
        ), navigate) }
        item { MenuGroup("СПРАВКА", listOf(
            MenuItem("glossary", "Словарь терминов", "Пояснения с исходным контекстом", "glossary"),
            MenuItem("about", "О книге", "Автор, источник и статус перевода", "about")
        ), navigate) }
        item { MenuGroup("НАСТРОЙКИ", listOf(
            MenuItem("settings", "Настройки", "Шрифты, тема и яркость", "settings")
        ), navigate) }
    }
}
