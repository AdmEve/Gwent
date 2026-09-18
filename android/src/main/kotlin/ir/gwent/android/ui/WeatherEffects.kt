package ir.gwent.android.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import ir.gwent.core.model.Row as GwentRow

/**
 * Drifting weather over a row under an active effect: frost on the melee line, fog over
 * the archers, rain on the siege line. Particle positions come from the index rather than
 * a random source so they stay put across recompositions.
 */
@Composable
fun WeatherOverlay(row: GwentRow, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "weather")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 5200, easing = LinearEasing)),
        label = "phase",
    )

    Canvas(modifier = modifier) {
        val count = 22
        for (i in 0 until count) {
            val seedX = ((i * 37) % 101) / 101f
            val seedY = ((i * 59) % 97) / 97f
            val seedS = ((i * 17) % 13) / 13f
            val p = (phase + seedY) % 1f

            when (row) {
                GwentRow.MELEE -> {
                    // Frost: fine specks sinking and sliding sideways.
                    val x = (seedX * size.width + p * size.width * 0.08f) % size.width
                    val y = p * size.height
                    drawCircle(
                        color = FrostTint.copy(alpha = 0.25f + seedS * 0.45f),
                        radius = 1f + seedS * 2.0f,
                        center = Offset(x, y),
                    )
                }
                GwentRow.RANGED -> {
                    // Fog: soft banks rolling across.
                    val x = (seedX * size.width + p * size.width * 0.35f) % size.width
                    val y = seedY * size.height
                    val r = size.height * (0.18f + seedS * 0.30f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x33C9D6E4), Color.Transparent),
                            center = Offset(x, y),
                            radius = r,
                        ),
                        radius = r,
                        center = Offset(x, y),
                    )
                }
                GwentRow.SIEGE -> {
                    // Rain: slanted streaks.
                    val x = (seedX * size.width + p * size.width * 0.10f) % size.width
                    val y = p * size.height
                    val len = size.height * (0.16f + seedS * 0.16f)
                    drawLine(
                        color = Color(0xFF8FB6D8).copy(alpha = 0.18f + seedS * 0.35f),
                        start = Offset(x, y),
                        end = Offset(x - len * 0.30f, y + len),
                        strokeWidth = 1.2f,
                    )
                }
            }
        }
    }
}
