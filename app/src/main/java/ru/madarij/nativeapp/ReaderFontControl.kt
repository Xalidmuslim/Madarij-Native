package ru.madarij.nativeapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Top-right reader typography control. There is no bottom reader overlay. */
@Composable
internal fun ReaderFontControl(
    value: Float,
    onChange: (Float) -> Unit,
    onCommit: (Float) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val latestValue by rememberUpdatedState(value)
    Box {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier.size(44.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("Aa", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onCommit(latestValue); expanded = false },
            modifier = Modifier.width(296.dp)
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text("Размер текста", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            val next = (latestValue - 1f).coerceAtLeast(14f)
                            onChange(next)
                            onCommit(next)
                        },
                        contentPadding = PaddingValues(3.dp)
                    ) { Text("A−") }
                    BookTextSlider(
                        value = value,
                        onValueChange = onChange,
                        onFinished = { onCommit(latestValue) },
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            val next = (latestValue + 1f).coerceAtMost(36f)
                            onChange(next)
                            onCommit(next)
                        },
                        contentPadding = PaddingValues(3.dp)
                    ) { Text("A+") }
                }
            }
        }
    }
}
