package ir.gwent.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import ir.gwent.core.model.Row as GwentRow

/** Darkens the edges so the lit center of the board draws the eye. */
fun Modifier.vignette(): Modifier = this.drawWithContent {
    drawContent()
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color(0x55000000), Color(0xBB000000)),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = size.maxDimension * 0.72f,
        )
    )
}

/** Gold hairline with a faceted diamond at the center, separating the two armies. */
@Composable
fun OrnateDivider(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(14.dp)) {
        val midY = size.height / 2f
        val gap = size.width * 0.06f
        val line = Brush.horizontalGradient(
            listOf(Color.Transparent, GoldDeep, GoldLight, GoldDeep, Color.Transparent)
        )
        drawRect(
            brush = line,
            topLeft = Offset(0f, midY - 0.6f),
            size = androidx.compose.ui.geometry.Size(size.width / 2f - gap, 1.2f),
        )
        drawRect(
            brush = line,
            topLeft = Offset(size.width / 2f + gap, midY - 0.6f),
            size = androidx.compose.ui.geometry.Size(size.width / 2f - gap, 1.2f),
        )
        val r = size.height * 0.32f
        val cx = size.width / 2f
        val diamond = Path().apply {
            moveTo(cx, midY - r)
            lineTo(cx + r, midY)
            lineTo(cx, midY + r)
            lineTo(cx - r, midY)
            close()
        }
        drawPath(diamond, color = GoldLight, style = Stroke(width = 1.4f))
        drawPath(diamond, color = Color(0x33F7E6B6))
    }
}

/** Simple struck-metal glyph marking what kind of row this is. */
@Composable
fun RowGlyph(row: GwentRow, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.09f)
        when (row) {
            GwentRow.MELEE -> {
                // Sword: blade, crossguard, pommel.
                drawLine(tint, Offset(w * 0.5f, h * 0.08f), Offset(w * 0.5f, h * 0.78f), strokeWidth = w * 0.12f)
                drawLine(tint, Offset(w * 0.26f, h * 0.62f), Offset(w * 0.74f, h * 0.62f), strokeWidth = w * 0.10f)
                drawCircle(tint, radius = w * 0.08f, center = Offset(w * 0.5f, h * 0.88f))
            }
            GwentRow.RANGED -> {
                // Bow: arc plus drawn string and arrow.
                drawArc(
                    color = tint,
                    startAngle = -70f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(w * 0.16f, h * 0.10f),
                    size = androidx.compose.ui.geometry.Size(w * 0.62f, h * 0.80f),
                    style = stroke,
                )
                drawLine(tint, Offset(w * 0.34f, h * 0.16f), Offset(w * 0.34f, h * 0.84f), strokeWidth = w * 0.06f)
                drawLine(tint, Offset(w * 0.30f, h * 0.5f), Offset(w * 0.86f, h * 0.5f), strokeWidth = w * 0.07f)
            }
            GwentRow.SIEGE -> {
                // Trebuchet: throwing arm over a braced frame.
                drawLine(tint, Offset(w * 0.18f, h * 0.80f), Offset(w * 0.82f, h * 0.80f), strokeWidth = w * 0.10f)
                drawLine(tint, Offset(w * 0.34f, h * 0.80f), Offset(w * 0.52f, h * 0.34f), strokeWidth = w * 0.09f)
                drawLine(tint, Offset(w * 0.66f, h * 0.80f), Offset(w * 0.52f, h * 0.34f), strokeWidth = w * 0.09f)
                drawLine(tint, Offset(w * 0.30f, h * 0.20f), Offset(w * 0.76f, h * 0.52f), strokeWidth = w * 0.08f)
                drawCircle(tint, radius = w * 0.10f, center = Offset(w * 0.26f, h * 0.17f))
            }
        }
    }
}
