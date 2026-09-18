package ir.gwent.android.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import ir.gwent.core.model.Ability
import ir.gwent.core.model.Row

/**
 * Procedural artwork. Every card gets a heraldic device derived from its id, so a deck reads
 * as a set of distinct painted cards rather than rows of identical panels, and the board gets
 * a lit table surface instead of a flat fill. Real illustrations, when they exist, are drawn
 * over the top of this by [CardView].
 */

/** Small deterministic generator: the same card always paints the same way. */
private class ArtRng(seed: Int) {
    private var state: Long = (seed.toLong() and 0xFFFFFFFFL) or 1L

    fun next(): Float {
        state = state * 6364136223846793005L + 1442695040888963407L
        val bits = ((state ushr 33).toInt() and 0x7FFFFFFF)
        return bits.toFloat() / 0x7FFFFFFF.toFloat()
    }

    fun range(from: Float, to: Float): Float = from + (to - from) * next()
    fun pick(count: Int): Int = (next() * count).toInt().coerceIn(0, count - 1)
}

/**
 * The battlefield itself: a dark table, a warm pool of light where the armies meet, long
 * grain streaks, and a heavy vignette so the middle of the screen carries the eye.
 */
fun Modifier.tableSurface(): Modifier = this.drawBehind {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF0A0B0E), Color(0xFF15171D), Color(0xFF191B20), Color(0xFF0A0B0E)),
        )
    )

    // Pool of light across the centre line.
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x2ED8B98A), Color(0x14A88A5E), Color.Transparent),
            center = Offset(size.width * 0.5f, size.height * 0.5f),
            radius = size.maxDimension * 0.62f,
        )
    )

    // Grain: long, very faint horizontal streaks.
    val rng = ArtRng(4711)
    repeat(26) {
        val y = rng.range(0f, size.height)
        val h = rng.range(0.6f, 2.4f)
        drawRect(
            color = Color.White.copy(alpha = rng.range(0.004f, 0.016f)),
            topLeft = Offset(0f, y),
            size = Size(size.width, h),
        )
    }

    // Vignette.
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color(0x66000000), Color(0xCC000000)),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = size.maxDimension * 0.70f,
        )
    )
}

/**
 * The painted face of a card: a lit ground in the faction's colours, a heraldic device, and
 * a hint of what the card does. Drawn into whatever space the caller gives it.
 */
fun DrawScope.drawCardArt(
    seed: Int,
    accent: Color,
    deep: Color,
    isHero: Boolean,
    ability: Ability,
    row: Row,
) {
    val rng = ArtRng(seed)
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val cy = h * 0.44f

    // Ground: deep faction colour, lifted where the light falls.
    drawRect(Brush.verticalGradient(listOf(deep, Color(0xFF0C0E12))))
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(accent.copy(alpha = 0.55f), accent.copy(alpha = 0.12f), Color.Transparent),
            center = Offset(cx + rng.range(-0.12f, 0.12f) * w, cy - h * 0.12f),
            radius = w * rng.range(0.85f, 1.25f),
        )
    )

    // Two broad brush arcs, like paint strokes behind the device.
    repeat(2) {
        val r = w * rng.range(0.45f, 0.8f)
        drawArc(
            color = Color.Black.copy(alpha = 0.18f),
            startAngle = rng.range(150f, 220f),
            sweepAngle = rng.range(80f, 160f),
            useCenter = false,
            topLeft = Offset(cx - r, cy - r),
            size = Size(r * 2, r * 2),
            style = Stroke(width = w * rng.range(0.05f, 0.12f)),
        )
    }

    val ink = Color(0xFF0D0F13)
    val leaf = if (isHero) GoldLight else Color(0xFFE6DCC6)
    val device = if (ability != Ability.NONE) 6 else rng.pick(6)
    val r = w * 0.26f

    // Backing disc so the device reads against the ground.
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color.Black.copy(alpha = 0.42f), Color.Transparent),
            center = Offset(cx, cy),
            radius = r * 1.9f,
        ),
        radius = r * 1.9f,
        center = Offset(cx, cy),
    )

    when (device) {
        0 -> { // Crossed blades
            listOf(-38f, 38f).forEach { angle ->
                rotate(angle, Offset(cx, cy)) {
                    drawLine(leaf, Offset(cx, cy - r * 1.15f), Offset(cx, cy + r * 0.9f), strokeWidth = w * 0.045f)
                    drawLine(leaf, Offset(cx - r * 0.34f, cy + r * 0.5f), Offset(cx + r * 0.34f, cy + r * 0.5f), strokeWidth = w * 0.035f)
                }
            }
        }
        1 -> { // Chevron shield
            val path = Path().apply {
                moveTo(cx - r, cy - r * 0.9f)
                lineTo(cx + r, cy - r * 0.9f)
                lineTo(cx + r, cy + r * 0.2f)
                quadraticBezierTo(cx, cy + r * 1.3f, cx - r, cy + r * 0.2f)
                close()
            }
            drawPath(path, ink.copy(alpha = 0.55f))
            drawPath(path, leaf, style = Stroke(width = w * 0.035f))
            drawLine(leaf, Offset(cx - r * 0.7f, cy + r * 0.1f), Offset(cx, cy - r * 0.45f), strokeWidth = w * 0.045f)
            drawLine(leaf, Offset(cx, cy - r * 0.45f), Offset(cx + r * 0.7f, cy + r * 0.1f), strokeWidth = w * 0.045f)
        }
        2 -> { // Sunburst
            repeat(12) { i ->
                rotate(i * 30f, Offset(cx, cy)) {
                    drawLine(
                        leaf.copy(alpha = if (i % 2 == 0) 0.95f else 0.45f),
                        Offset(cx, cy - r * 0.55f),
                        Offset(cx, cy - r * 1.25f),
                        strokeWidth = w * 0.03f,
                    )
                }
            }
            drawCircle(leaf, radius = r * 0.42f, center = Offset(cx, cy), style = Stroke(width = w * 0.04f))
        }
        3 -> { // Crescent and star
            drawCircle(leaf, radius = r * 0.85f, center = Offset(cx, cy), style = Stroke(width = w * 0.05f))
            drawCircle(deep, radius = r * 0.78f, center = Offset(cx + r * 0.34f, cy - r * 0.16f))
            drawCircle(leaf, radius = r * 0.16f, center = Offset(cx + r * 0.72f, cy + r * 0.62f))
        }
        4 -> { // Keep / tower
            val bw = r * 1.15f
            drawRect(ink.copy(alpha = 0.5f), Offset(cx - bw / 2, cy - r * 0.5f), Size(bw, r * 1.5f))
            drawRect(leaf, Offset(cx - bw / 2, cy - r * 0.5f), Size(bw, r * 1.5f), style = Stroke(width = w * 0.032f))
            repeat(3) { i ->
                val x = cx - bw / 2 + bw * (0.12f + i * 0.33f)
                drawRect(leaf, Offset(x, cy - r * 0.78f), Size(bw * 0.18f, r * 0.3f))
            }
        }
        5 -> { // Watching eye
            val path = Path().apply {
                moveTo(cx - r, cy)
                quadraticBezierTo(cx, cy - r * 0.95f, cx + r, cy)
                quadraticBezierTo(cx, cy + r * 0.95f, cx - r, cy)
                close()
            }
            drawPath(path, ink.copy(alpha = 0.5f))
            drawPath(path, leaf, style = Stroke(width = w * 0.035f))
            drawCircle(leaf, radius = r * 0.3f, center = Offset(cx, cy))
            drawCircle(deep, radius = r * 0.13f, center = Offset(cx, cy))
        }
        else -> drawAbilityDevice(ability, cx, cy, r, leaf, ink)
    }

    if (isHero) {
        // A laurel halo marks the ones nothing can touch.
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.Transparent, GoldLight.copy(alpha = 0.22f), Color.Transparent),
                center = Offset(cx, cy),
                radius = r * 2.2f,
            ),
            radius = r * 2.2f,
            center = Offset(cx, cy),
        )
        listOf(-1f, 1f).forEach { side ->
            drawArc(
                color = GoldLight.copy(alpha = 0.8f),
                startAngle = if (side < 0) 110f else -70f,
                sweepAngle = if (side < 0) 140f else 140f,
                useCenter = false,
                topLeft = Offset(cx - r * 1.5f, cy - r * 1.5f),
                size = Size(r * 3f, r * 3f),
                style = Stroke(width = w * 0.03f),
            )
        }
    }

    // A quiet mark of which line this unit fights in.
    drawRowMark(row, Offset(w * 0.5f, h * 0.80f), w * 0.09f, leaf.copy(alpha = 0.35f))

    // Light falls from the top-left, so darken the opposite corner.
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
            start = Offset(0f, 0f),
            end = Offset(w, h),
        )
    )
}

private fun DrawScope.drawAbilityDevice(
    ability: Ability,
    cx: Float,
    cy: Float,
    r: Float,
    leaf: Color,
    ink: Color,
) {
    val w = size.width
    when (ability) {
        Ability.WEATHER -> { // Cloud with rain
            drawCircle(leaf.copy(alpha = 0.85f), radius = r * 0.45f, center = Offset(cx - r * 0.35f, cy - r * 0.15f))
            drawCircle(leaf.copy(alpha = 0.85f), radius = r * 0.55f, center = Offset(cx + r * 0.2f, cy - r * 0.25f))
            repeat(4) { i ->
                val x = cx - r * 0.6f + i * r * 0.42f
                drawLine(leaf.copy(alpha = 0.7f), Offset(x, cy + r * 0.4f), Offset(x - r * 0.18f, cy + r * 1.05f), strokeWidth = w * 0.025f)
            }
        }
        Ability.CLEAR_WEATHER -> { // Sun breaking through
            drawCircle(leaf, radius = r * 0.5f, center = Offset(cx, cy))
            repeat(8) { i ->
                rotate(i * 45f, Offset(cx, cy)) {
                    drawLine(leaf.copy(alpha = 0.8f), Offset(cx, cy - r * 0.7f), Offset(cx, cy - r * 1.15f), strokeWidth = w * 0.03f)
                }
            }
        }
        Ability.SCORCH -> { // Flame
            val path = Path().apply {
                moveTo(cx, cy - r * 1.1f)
                quadraticBezierTo(cx + r * 0.9f, cy - r * 0.1f, cx + r * 0.35f, cy + r * 0.8f)
                quadraticBezierTo(cx, cy + r * 1.1f, cx - r * 0.35f, cy + r * 0.8f)
                quadraticBezierTo(cx - r * 0.9f, cy - r * 0.1f, cx, cy - r * 1.1f)
                close()
            }
            drawPath(path, Color(0xFFD9642F).copy(alpha = 0.75f))
            drawPath(path, leaf, style = Stroke(width = w * 0.03f))
        }
        Ability.HORN -> { // War horn
            drawArc(
                color = leaf,
                startAngle = 20f,
                sweepAngle = 200f,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r * 0.8f),
                size = Size(r * 2f, r * 1.6f),
                style = Stroke(width = w * 0.06f),
            )
            drawCircle(leaf, radius = r * 0.2f, center = Offset(cx + r * 0.85f, cy - r * 0.2f))
        }
        Ability.MEDIC -> { // Cross
            drawRect(leaf, Offset(cx - r * 0.18f, cy - r * 0.9f), Size(r * 0.36f, r * 1.8f))
            drawRect(leaf, Offset(cx - r * 0.9f, cy - r * 0.18f), Size(r * 1.8f, r * 0.36f))
        }
        Ability.SPY -> { // Masked eye
            drawRect(ink.copy(alpha = 0.6f), Offset(cx - r, cy - r * 0.3f), Size(r * 2f, r * 0.6f))
            drawRect(leaf, Offset(cx - r, cy - r * 0.3f), Size(r * 2f, r * 0.6f), style = Stroke(width = w * 0.025f))
            drawCircle(leaf, radius = r * 0.16f, center = Offset(cx - r * 0.4f, cy))
            drawCircle(leaf, radius = r * 0.16f, center = Offset(cx + r * 0.4f, cy))
        }
        Ability.DECOY -> { // Two faces, one false
            drawCircle(leaf, radius = r * 0.55f, center = Offset(cx - r * 0.35f, cy), style = Stroke(width = w * 0.035f))
            drawCircle(leaf.copy(alpha = 0.45f), radius = r * 0.55f, center = Offset(cx + r * 0.35f, cy), style = Stroke(width = w * 0.035f))
        }
        Ability.NONE -> Unit
    }
}

/** The little sword / bow / trebuchet mark used on cards and row banners. */
fun DrawScope.drawRowMark(row: Row, centre: Offset, radius: Float, tint: Color) {
    val s = radius
    when (row) {
        Row.MELEE -> {
            drawLine(tint, Offset(centre.x, centre.y - s), Offset(centre.x, centre.y + s * 0.8f), strokeWidth = s * 0.34f)
            drawLine(tint, Offset(centre.x - s * 0.6f, centre.y + s * 0.25f), Offset(centre.x + s * 0.6f, centre.y + s * 0.25f), strokeWidth = s * 0.26f)
        }
        Row.RANGED -> {
            drawArc(
                color = tint,
                startAngle = -70f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(centre.x - s * 0.9f, centre.y - s),
                size = Size(s * 1.8f, s * 2f),
                style = Stroke(width = s * 0.26f),
            )
            drawLine(tint, Offset(centre.x - s * 0.35f, centre.y - s * 0.85f), Offset(centre.x - s * 0.35f, centre.y + s * 0.85f), strokeWidth = s * 0.16f)
        }
        Row.SIEGE -> {
            drawLine(tint, Offset(centre.x - s * 0.9f, centre.y + s * 0.7f), Offset(centre.x + s * 0.9f, centre.y + s * 0.7f), strokeWidth = s * 0.28f)
            drawLine(tint, Offset(centre.x - s * 0.4f, centre.y + s * 0.7f), Offset(centre.x + s * 0.1f, centre.y - s * 0.7f), strokeWidth = s * 0.26f)
            drawCircle(tint, radius = s * 0.3f, center = Offset(centre.x - s * 0.6f, centre.y - s * 0.75f))
        }
    }
}

/**
 * Corner ornaments and a bevelled inner edge, which is most of what separates a game card
 * from a rounded rectangle.
 */
fun Modifier.ornateCorners(tint: Color, inset: Float = 3f): Modifier = this.drawWithContent {
    drawContent()
    val len = size.minDimension * 0.22f
    val stroke = Stroke(width = size.minDimension * 0.022f)
    val i = inset

    listOf(
        Triple(Offset(i, i), Offset(i + len, i), Offset(i, i + len)),
        Triple(Offset(size.width - i, i), Offset(size.width - i - len, i), Offset(size.width - i, i + len)),
        Triple(Offset(i, size.height - i), Offset(i + len, size.height - i), Offset(i, size.height - i - len)),
        Triple(Offset(size.width - i, size.height - i), Offset(size.width - i - len, size.height - i), Offset(size.width - i, size.height - i - len)),
    ).forEach { (corner, horizontal, vertical) ->
        drawLine(tint, corner, horizontal, strokeWidth = stroke.width)
        drawLine(tint, corner, vertical, strokeWidth = stroke.width)
    }
}
