package ru.madarij.nativeapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * A paper sheet is intentionally NOT nested inside Material Card: the latter
 * clips irregular translucent paper edges to a geometric rounded rectangle.
 * Painting on the parent Box also keeps all original live text and content
 * above the image and preserves screen-reader and click handling.
 */
@Composable
internal fun AgedPaperCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    frameKind: VintageFrameKind = VintageFrameKind.LIGHT_WIDE,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier.vintageFrame(frameKind)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp)) {
            content()
        }
    }
}

@Composable
internal fun AgedPaperCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.medium,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    frameKind: VintageFrameKind = VintageFrameKind.LIGHT_WIDE,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier.vintageFrame(frameKind).clickable(enabled = enabled, onClick = onClick)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp)) {
            content()
        }
    }
}
