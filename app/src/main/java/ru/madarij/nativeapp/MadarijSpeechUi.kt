package ru.madarij.nativeapp

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MadarijAudioSheet(
    controller: MadarijSpeechController,
    canPlay: Boolean,
    onPlay: () -> Unit,
    onSelectVoice: (VoiceChoice) -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onClose) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Озвучка главы", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("Бесплатный RuVoice · русский текст · офлайн",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            controller.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        if (controller.status == SpeechStatus.Playing) controller.pause()
                        else if (controller.status == SpeechStatus.Paused) controller.resume()
                        else onPlay()
                    },
                    enabled = canPlay && controller.status != SpeechStatus.Preparing &&
                        controller.status != SpeechStatus.Error,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(when (controller.status) {
                        SpeechStatus.Playing -> "Пауза"
                        SpeechStatus.Paused -> "Продолжить"
                        SpeechStatus.Preparing -> "Подготовка…"
                        else -> "▶ Слушать"
                    })
                }
                TextButton(
                    onClick = { controller.stop() },
                    enabled = controller.status == SpeechStatus.Playing ||
                        controller.status == SpeechStatus.Paused,
                ) { Text("Стоп") }
                Spacer(Modifier.weight(1f))
                if (controller.totalChunks > 0) {
                    Text(
                        (controller.playedChunks + 1).coerceAtMost(controller.totalChunks).toString() +
                            "/" + controller.totalChunks,
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider()
            Text("Мужской голос", style = MaterialTheme.typography.titleSmall)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                VoiceChoice.entries.forEach { voice ->
                    FilterChip(
                        selected = controller.selectedVoice == voice,
                        onClick = { onSelectVoice(voice) },
                        label = { Text(voice.label, maxLines = 1) }
                    )
                }
            }
            Text("Скорость", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.85f, 1.0f, 1.15f).forEach { rate ->
                    FilterChip(
                        selected = controller.speed == rate,
                        onClick = { onSelectSpeed(rate) },
                        label = { Text(String.format(Locale.ROOT, "%.2f×", rate)) }
                    )
                }
            }
            Text(
                "Арабские цитаты не читаются русским голосом. При выборе другого голоса " +
                    "воспроизведение останавливается: нажмите «Слушать» ещё раз.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_MAIN).setClassName(
                        RUVOICE_PACKAGE, "ru.kost.ruvoice.SettingsActivity"))
                } catch (_: Exception) {
                    Toast.makeText(context, "Установите RuVoice TTS", Toast.LENGTH_LONG).show()
                }
            }) { Text("Открыть настройки RuVoice") }
        }
    }
}

@Composable
internal fun MadarijAudioMiniBar(
    controller: MadarijSpeechController,
    onOpen: () -> Unit,
) {
    if (controller.status != SpeechStatus.Playing && controller.status != SpeechStatus.Paused) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = {
            if (controller.status == SpeechStatus.Playing) controller.pause()
            else controller.resume()
        }) { Text(if (controller.status == SpeechStatus.Playing) "Ⅱ Пауза" else "▶ Продолжить") }
        TextButton(onClick = onOpen, modifier = Modifier.weight(1f)) {
            Text(controller.selectedVoice.label, maxLines = 1,
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = controller::stop) { Text("Стоп") }
    }
}
