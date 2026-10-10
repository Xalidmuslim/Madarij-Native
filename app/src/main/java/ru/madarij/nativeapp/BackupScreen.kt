package ru.madarij.nativeapp

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import ru.madarij.nativeapp.data.LearningRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupScreen(vm: BookViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val learning = remember(context) { LearningRepository(context.applicationContext) }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<JSONObject?>(null) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try {
                val data = vm.backup.export().put("learning", learning.exportState())
                withContext(Dispatchers.IO) {
                    requireNotNull(context.contentResolver.openOutputStream(uri, "wt")).bufferedWriter(Charsets.UTF_8).use { it.write(data.toString(2)) }
                }
                result = "Копия сохранена: заметки, закладки, место чтения, настройки и результаты обучения."
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                result = "Не удалось сохранить копию. Выберите доступную папку и повторите."
            } finally { busy = false }
        }
    }
    val choose = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try {
                val data = withContext(Dispatchers.IO) {
                    val text = requireNotNull(context.contentResolver.openInputStream(uri)).bufferedReader(Charsets.UTF_8).use { reader ->
                        val chars = CharArray(8192)
                        val content = StringBuilder()
                        while (true) {
                            val count = reader.read(chars)
                            if (count < 0) break
                            require(content.length + count <= 5_000_000) { "Backup too large" }
                            content.append(chars, 0, count)
                        }
                        content.toString()
                    }
                    JSONObject(text).also {
                        require(it.optString("format") == "madarij-personal" && it.optInt("version") == 1)
                    }
                }
                pending = data
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                result = "Файл не является поддерживаемой копией приложения или недоступен для чтения."
            } finally { busy = false }
        }
    }
    LazyColumn(contentPadding = PaddingValues(start=20.dp,top=20.dp,end=20.dp,bottom=124.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Резервная копия", style = MaterialTheme.typography.headlineSmall) }
        item { Text("Сохраните личные данные перед заменой телефона или переустановкой. Книга уже находится в приложении и в копию не добавляется.") }
        item { Card { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Сохранение", style = MaterialTheme.typography.titleMedium)
            Text("Папку выбираете вы. Файл содержит ваши личные записи. Приложение не отправляет его автоматически.")
            Button(enabled = !busy, onClick = { export.launch("Madarij-backup-${SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())}.json") }, modifier = Modifier.fillMaxWidth()) { Text("Сохранить копию") }
        } } }
        item { Card { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Восстановление", style = MaterialTheme.typography.titleMedium)
            Text("Данные объединяются с текущими. Записи к отсутствующим фрагментам пропускаются. Параметры чтения берутся из копии.")
            OutlinedButton(enabled = !busy, onClick = { choose.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, modifier = Modifier.fillMaxWidth()) { Text("Выбрать копию") }
        } } }
        if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Обработка файла…", Modifier.padding(top = 12.dp)) }
        if (result.isNotBlank()) item { Text(result, color = MaterialTheme.colorScheme.primary) }
    }
    pending?.let { data -> AlertDialog(
        onDismissRequest = { if (!busy) pending = null },
        title = { Text("Восстановить личные данные?") },
        text = { Text("Новые записи будут добавлены; более свежие заметки и позиции чтения сохранятся. Настройки будут заменены параметрами из выбранной копии.") },
        confirmButton = { TextButton(enabled = !busy, onClick = {
            scope.launch {
                busy = true
                try {
                    val count = vm.backup.import(data)
                    data.optJSONObject("learning")?.let { learning.importState(it) }
                    result = "Копия восстановлена. Обработано личных записей: $count."
                    pending = null
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    result = "Не удалось восстановить все данные. Проверьте файл и повторите; записи, уже обработанные из копии, повторно не дублируются."
                    pending = null
                } finally { busy = false }
            }
        }) { Text("Восстановить") } },
        dismissButton = { TextButton(enabled = !busy, onClick = { pending = null }) { Text("Отмена") } }
    ) }
}
