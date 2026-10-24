package ru.madarij.nativeapp

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Unobtrusive opt-in audio bar; no expensive model or layout during book scrolling
 * unless opened via the existing overflow menu.
 */
@Composable
internal fun ChapterAudioControls(
    voice: ChapterAudioVoice,
    controller: ChapterAudioController?,
    readyToPlay: Boolean,
    onVoice: (ChapterAudioVoice) -> Unit,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onSpeed: () -> Unit,
    onClose: () -> Unit,
) {
    val status = controller?.status ?: ChapterAudioStatus.READY
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Озвучка русского перевода", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onClose) { Text("Скрыть") }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ChapterAudioVoice.entries.forEach { option ->
                FilterChip(
                    selected = voice == option,
                    onClick = { onVoice(option) },
                    label = { Text(option.displayName) }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            OutlinedButton(
                enabled = readyToPlay && status != ChapterAudioStatus.LOADING,
                onClick = onPlayPause
            ) {
                Text(
                    when (status) {
                        ChapterAudioStatus.PLAYING -> "Пауза"
                        ChapterAudioStatus.PAUSED -> "Продолжить"
                        ChapterAudioStatus.LOADING -> "Подключение…"
                        else -> "Слушать"
                    }
                )
            }
            TextButton(
                enabled = controller != null && status in setOf(
                    ChapterAudioStatus.PLAYING, ChapterAudioStatus.PAUSED
                ),
                onClick = onStop
            ) { Text("Стоп") }
            TextButton(
                enabled = controller != null && status != ChapterAudioStatus.LOADING,
                onClick = onSpeed
            ) { Text(String.format(java.util.Locale.ROOT, "%.1f×", controller?.speed ?: 1.0f)) }
        }
        if (status == ChapterAudioStatus.PLAYING || status == ChapterAudioStatus.PAUSED) {
            Text(
                "Фрагмент " + ((controller?.chunkIndex ?: 0) + 1) +
                    " из " + (controller?.chunkCount ?: 0),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        controller?.message?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        if (controller == null) {
            Text(
                "Требуется установленный RuVoice. Арабский оригинал не озвучивается русским голосом.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
