package ru.madarij.nativeapp

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Distinct miniature 3D bronze quill and inkpot for the Continue reading button.
 * Drawn at 28dp without a card or background so it sits within the original leather frame.
 * The TOC/book glyph and all other UI elements retain their existing resources.
 */
@Composable
internal fun ContinueReadingQuillIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val darkBronze = Color(0xFF573016)
        val bronze = Color(0xFF9A5C29)
        val gold = Color(0xFFDDAA60)
        val lightGold = Color(0xFFFFDF9E)

        // Soft cast shadow gives the engraved metal a shallow raised profile.
        drawOval(
            color = Color(0xFF160B06).copy(alpha = .70f),
            topLeft = Offset(w * .17f, h * .86f),
            size = Size(w * .68f, h * .10f)
        )
        // Vessel: broad base, tapered shoulders, burnished gold face and dark rim.
        val pot = Path().apply {
            moveTo(w * .26f, h * .72f)
            quadraticTo(w * .50f, h * .64f, w * .75f, h * .72f)
            lineTo(w * .81f, h * .83f)
            quadraticTo(w * .80f, h * .92f, w * .71f, h * .92f)
            lineTo(w * .30f, h * .92f)
            quadraticTo(w * .20f, h * .92f, w * .20f, h * .83f)
            close()
        }
        drawPath(pot, brush = Brush.verticalGradient(
            listOf(lightGold, gold, bronze, darkBronze),
            startY = h * .68f, endY = h * .94f
        ))
        drawPath(pot, color = darkBronze, style = Stroke(width = .85.dp.toPx()))
        drawOval(
            brush = Brush.verticalGradient(
                listOf(lightGold, bronze, darkBronze),
                startY = h * .67f, endY = h * .81f
            ),
            topLeft = Offset(w * .24f, h * .68f),
            size = Size(w * .54f, h * .12f)
        )
        drawOval(
            color = Color(0xFF2A170E),
            topLeft = Offset(w * .32f, h * .71f),
            size = Size(w * .38f, h * .058f)
        )

        // Feather is inclined away from the bottom-navigation book icon.
        val feather = Path().apply {
            moveTo(w * .47f, h * .70f)
            cubicTo(w * .24f, h * .55f, w * .21f, h * .35f, w * .47f, h * .23f)
            cubicTo(w * .58f, h * .16f, w * .75f, h * .12f, w * .85f, h * .07f)
            cubicTo(w * .82f, h * .27f, w * .74f, h * .41f, w * .61f, h * .52f)
            cubicTo(w * .56f, h * .57f, w * .49f, h * .65f, w * .47f, h * .70f)
            close()
        }
        drawPath(feather, color = darkBronze, style = Stroke(width = 1.dp.toPx()))
        drawPath(feather, brush = Brush.linearGradient(
            colors = listOf(Color(0xFFB87939), lightGold, gold, darkBronze),
            start = Offset(w * .25f, h * .26f),
            end = Offset(w * .79f, h * .57f)
        ))
        drawPath(feather, color = Color(0xFFFFE1A2).copy(alpha = .60f),
            style = Stroke(width = .55.dp.toPx()))
        // Quill spine and vanes (minimal at icon scale).
        drawLine(
            color = darkBronze,
            start = Offset(w * .45f, h * .72f),
            end = Offset(w * .79f, h * .14f),
            strokeWidth = 1.35.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = lightGold,
            start = Offset(w * .44f, h * .67f),
            end = Offset(w * .77f, h * .14f),
            strokeWidth = .65.dp.toPx(),
            cap = StrokeCap.Round
        )
        for (i in 0..3) {
            val t = i / 4f
            drawLine(
                color = bronze.copy(alpha = .75f),
                start = Offset(w * (.39f + t * .12f), h * (.36f + t * .17f)),
                end = Offset(w * (.63f + t * .10f), h * (.31f + t * .13f)),
                strokeWidth = .55.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        // A single small glint ties the metal treatment to the navigation icons.
        drawLine(
            color = lightGold.copy(alpha = .85f),
            start = Offset(w * .31f, h * .81f),
            end = Offset(w * .42f, h * .81f),
            strokeWidth = .65.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}
