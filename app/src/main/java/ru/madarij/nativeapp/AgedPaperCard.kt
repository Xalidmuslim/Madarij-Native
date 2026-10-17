package ru.madarij.nativeapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale

/**
 * Thin presentation wrapper around Material3 Card.
 * The original Card's click behavior, sizing, accessibility and state stay intact.
 * All light cards share one cached static edge texture; dark cards are untouched.
 */
@Composable
internal fun AgedPaperCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = modifier, shape = shape, colors = colors, elevation = elevation, border = border) {
        Box {
            if (MaterialTheme.colorScheme.background != BookColors.nightBackground) {
                PreloadedBookImage(R.drawable.card_paper, Modifier.matchParentSize(), ContentScale.FillBounds)
            }
            Column { content() }
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
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        onClick = onClick, modifier = modifier, enabled = enabled, shape = shape,
        colors = colors, elevation = elevation, border = border
    ) {
        Box {
            if (MaterialTheme.colorScheme.background != BookColors.nightBackground) {
                PreloadedBookImage(R.drawable.card_paper, Modifier.matchParentSize(), ContentScale.FillBounds)
            }
            Column { content() }
        }
    }
}
