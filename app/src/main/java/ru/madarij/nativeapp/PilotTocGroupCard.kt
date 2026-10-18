package ru.madarij.nativeapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

/**
 * Single-card Figma pilot. The first TOC group uses the new native shell;
 * every other group keeps the existing AgedPaperCard pixel-for-pixel.
 *
 * The AI-generated resource is deliberately PAPER TEXTURE ONLY.
 * No bitmap frame, corners, numbers, text, arrows or buttons are stretched.
 * Compose renders the scalable border and the original live row on top.
 */
@Composable
internal fun PilotTocGroupCard(
    pilot: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(19.dp),
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (!pilot || MaterialTheme.colorScheme.background == BookColors.nightBackground) {
        AgedPaperCard(
            onClick = onClick, modifier = modifier, shape = shape,
            colors = colors, elevation = elevation, border = border,
            content = content
        )
        return
    }

    val pilotShape = RoundedCornerShape(15.dp)
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = pilotShape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1DDBA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Color(0xFF9E713F))
    ) {
        Box(
            Modifier.fillMaxWidth().background(
                Brush.verticalGradient(
                    listOf(Color(0xFFF8EBCF), Color(0xFFF1DEBC))
                )
            )
        ) {
            // Figma AI image is a low-opacity MATERIAL layer, never the border.
            PreloadedBookImage(
                R.drawable.figma_parchment_pilot,
                Modifier.matchParentSize().alpha(.17f),
                ContentScale.Crop
            )
            // Insets keep the title and progress above the trim at any text scale.
            Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
                content()
            }
        }
    }
}
