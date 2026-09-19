package ir.gwent.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/*
 * The ground.
 *
 * GWENT is not played on a black screen with boxes ruled onto it — it is played on a place. The
 * reference board is a forest clearing: a worn cobbled path running through the middle, packed
 * earth and moss either side of it, loose rock, grass pushing up between the stones, dead leaves,
 * roots crossing the soil, and warm light falling through a gap in the canopy onto the centre.
 *
 * None of that is a photograph here. It is drawn, every frame, out of a few hundred small vector
 * shapes whose positions come from a fixed seed — so the same board comes back identically on
 * every launch, and nothing has to be shipped as an asset. The layers are painted in the order
 * the real ground was made: soil, then what grew and fell on it, then what light reaches it.
 */

/** Earth, stone and green. Sampled to sit under the existing gold-leaf palette. */
object Ground {
    val soilBlack = Color(0xFF0A0804)
    val soilDeep = Color(0xFF1A1409)
    val soilMid = Color(0xFF2E2213)
    val soilWarm = Color(0xFF41301A)
    val clay = Color(0xFF5B4123)
    val dust = Color(0xFF6E5432)

    val stoneDark = Color(0xFF26261F)
    val stoneMid = Color(0xFF474438)
    val stoneLit = Color(0xFF6C6654)
    val stonePale = Color(0xFF908871)

    val mossDeep = Color(0xFF16240F)
    val mossMid = Color(0xFF2B4420)
    val mossLit = Color(0xFF44622B)
    val grassLit = Color(0xFF6B8B38)
    val grassDry = Color(0xFF7C6B33)

    val leafRust = Color(0xFF6B3F17)
    val leafOchre = Color(0xFF8B6626)
}

// --- scatter elements -------------------------------------------------------

private class Mottle(val c: Offset, val r: Float, val color: Color, val alpha: Float)
private class Cobble(val path: Path, val cy: Float, val hh: Float, val tone: Float, val mossy: Boolean)
private class Rock(val body: Path, val facet: Path, val c: Offset, val rx: Float, val ry: Float, val tone: Float)
private class Tuft(val blades: List<Path>, val color: Color)
private class Pebble(val c: Offset, val r: Float, val tone: Float)
private class Leaf(val path: Path, val color: Color)
private class Root(val path: Path, val width: Float)

/**
 * Everything on the ground, laid out once for a given size.
 *
 * Generated in the constructor rather than in the draw pass: a board redraw is a tap, not a
 * frame loop, but allocating three hundred [Path]s per redraw would still be waste.
 */
private class GroundPlan(val w: Float, val h: Float, seed: Int) {

    val mottles = ArrayList<Mottle>()
    val mossBeds = ArrayList<Mottle>()
    val cobbles = ArrayList<Cobble>()
    val rocks = ArrayList<Rock>()
    val tufts = ArrayList<Tuft>()
    val pebbles = ArrayList<Pebble>()
    val leaves = ArrayList<Leaf>()
    val roots = ArrayList<Root>()
    val pathBed: Path

    /*
     * The path's two edges wander, so stone meets earth on an organic line. Two sine terms at
     * different frequencies is enough — one alone reads as a ruled wave.
     */
    private fun wobble(x: Float, freq: Float, phase: Float): Float {
        val t = x / w
        return sin(t * freq + phase) * 0.62f + sin(t * freq * 2.7f + phase * 1.9f) * 0.38f
    }

    /*
     * Where the stone runs. Placed against the board's own layout rather than the middle of the
     * screen: the four rows occupy roughly the top three quarters, so the centre line between
     * the two sides falls near 0.40 of the height. The path is laid over that, which puts both
     * melee rows on stone and leaves the two ranged rows — and the hand below them — on grass,
     * exactly as the reference board is arranged.
     */
    fun pathTop(x: Float) = h * 0.175f + wobble(x, 5.7f, 1.7f) * h * 0.032f
    fun pathBottom(x: Float) = h * 0.625f + wobble(x, 4.9f, 4.2f) * h * 0.035f

    init {
        val rnd = Random(seed)

        // --- soil: broken colour, so the earth is not a gradient ------------
        val soils = listOf(Ground.soilDeep, Ground.soilMid, Ground.soilWarm, Ground.clay, Ground.soilBlack, Ground.dust)
        repeat(130) {
            val c = Offset(rnd.nextFloat() * w, rnd.nextFloat() * h)
            mottles += Mottle(c, h * (0.04f + rnd.nextFloat() * 0.18f), soils.random(rnd), 0.08f + rnd.nextFloat() * 0.20f)
        }

        // --- moss beds: the green either side of the path -------------------
        val mosses = listOf(Ground.mossDeep, Ground.mossMid, Ground.mossLit)
        repeat(46) {
            val cx = rnd.nextFloat() * w
            val above = rnd.nextBoolean()
            val cy = if (above) rnd.nextFloat() * pathTop(cx) else pathBottom(cx) + rnd.nextFloat() * (h - pathBottom(cx))
            mossBeds += Mottle(Offset(cx, cy), h * (0.06f + rnd.nextFloat() * 0.16f), mosses.random(rnd), 0.14f + rnd.nextFloat() * 0.24f)
        }

        // --- the path bed: dark earth showing through every gap -------------
        pathBed = Path().apply {
            val step = w / 56f
            moveTo(0f, pathTop(0f))
            var x = 0f
            while (x <= w) { lineTo(x, pathTop(x)); x += step }
            lineTo(w, pathTop(w))
            lineTo(w, pathBottom(w))
            x = w
            while (x >= 0f) { lineTo(x, pathBottom(x)); x -= step }
            lineTo(0f, pathBottom(0f))
            close()
        }

        // --- cobbles --------------------------------------------------------
        val stoneH = h * 0.050f
        val stoneW = h * 0.080f
        var y = h * 0.12f
        var band = 0
        while (y < h * 0.70f) {
            val stagger = if (band % 2 == 0) 0f else stoneW * 0.5f
            var x = -stoneW + stagger
            while (x < w + stoneW) {
                val cx = x + (rnd.nextFloat() - 0.5f) * stoneW * 0.30f
                val cy = y + (rnd.nextFloat() - 0.5f) * stoneH * 0.34f
                val top = pathTop(cx)
                val bottom = pathBottom(cx)
                // A worn path is missing stones — a few in the middle, many at its edges.
                val keep = when {
                    cy > top + stoneH * 0.4f && cy < bottom - stoneH * 0.4f -> rnd.nextFloat() > 0.09f
                    cy > top - stoneH && cy < bottom + stoneH -> rnd.nextFloat() > 0.64f
                    else -> false
                }
                if (keep) {
                    val hw = stoneW * (0.36f + rnd.nextFloat() * 0.13f)
                    val hh = stoneH * (0.36f + rnd.nextFloat() * 0.15f)
                    cobbles += Cobble(
                        path = blob(cx, cy, hw, hh, 7, 0.28f, rnd),
                        cy = cy,
                        hh = hh,
                        tone = 0.68f + rnd.nextFloat() * 0.62f,
                        mossy = rnd.nextFloat() < 0.16f,
                    )
                }
                x += stoneW
            }
            y += stoneH
            band++
        }

        // --- loose rock ------------------------------------------------------
        repeat(38) {
            val c = Offset(rnd.nextFloat() * w, h * 0.04f + rnd.nextFloat() * h * 0.92f)
            val rx = h * (0.016f + rnd.nextFloat() * 0.032f)
            val ry = rx * (0.58f + rnd.nextFloat() * 0.34f)
            rocks += Rock(
                body = blob(c.x, c.y, rx, ry, 9, 0.32f, rnd),
                facet = blob(c.x - rx * 0.24f, c.y - ry * 0.28f, rx * 0.50f, ry * 0.44f, 7, 0.36f, rnd),
                c = c, rx = rx, ry = ry,
                tone = 0.72f + rnd.nextFloat() * 0.56f,
            )
        }

        // --- gravel ----------------------------------------------------------
        repeat(260) {
            pebbles += Pebble(
                Offset(rnd.nextFloat() * w, rnd.nextFloat() * h),
                h * (0.0022f + rnd.nextFloat() * 0.0055f),
                0.55f + rnd.nextFloat() * 0.85f,
            )
        }

        // --- roots and twigs across the soil ---------------------------------
        repeat(15) {
            val x0 = -w * 0.06f + rnd.nextFloat() * w
            val y0 = rnd.nextFloat() * h
            val len = w * (0.10f + rnd.nextFloat() * 0.28f)
            fun drift() = (rnd.nextFloat() - 0.5f) * h * 0.11f
            roots += Root(
                Path().apply {
                    moveTo(x0, y0)
                    cubicTo(x0 + len * 0.30f, y0 + drift(), x0 + len * 0.68f, y0 + drift(), x0 + len, y0 + drift() * 0.5f)
                },
                h * (0.0035f + rnd.nextFloat() * 0.0065f),
            )
        }

        // --- fallen leaves ----------------------------------------------------
        val leafTones = listOf(Ground.leafRust, Ground.leafOchre, Ground.grassDry, Ground.clay)
        repeat(70) {
            val c = Offset(rnd.nextFloat() * w, rnd.nextFloat() * h)
            val len = h * (0.012f + rnd.nextFloat() * 0.020f)
            leaves += Leaf(leafPath(c, len, len * (0.34f + rnd.nextFloat() * 0.26f), rnd.nextFloat() * PI.toFloat()), leafTones.random(rnd))
        }

        // --- grass ------------------------------------------------------------
        repeat(175) {
            val cx = rnd.nextFloat() * w
            val top = pathTop(cx)
            val bottom = pathBottom(cx)
            // Grass belongs off the path, creeps back over its edges, and sprouts in its gaps.
            val cy = when (rnd.nextInt(10)) {
                0, 1, 2 -> rnd.nextFloat() * top
                3, 4, 5 -> bottom + rnd.nextFloat() * (h - bottom)
                6, 7 -> top + (rnd.nextFloat() - 0.5f) * h * 0.07f
                8 -> bottom + (rnd.nextFloat() - 0.5f) * h * 0.07f
                else -> top + rnd.nextFloat() * (bottom - top)
            }.coerceIn(h * 0.01f, h * 0.99f)

            val clump = h * (0.018f + rnd.nextFloat() * 0.042f)
            val blades = ArrayList<Path>(7)
            repeat(3 + rnd.nextInt(4)) {
                blades += bladePath(
                    bx = cx + (rnd.nextFloat() - 0.5f) * clump * 0.95f,
                    by = cy,
                    hgt = clump * (0.52f + rnd.nextFloat() * 0.70f),
                    lean = (rnd.nextFloat() - 0.5f) * 1.15f,
                )
            }
            val tone = when (rnd.nextInt(9)) {
                0 -> Ground.grassDry
                1, 2 -> Ground.mossDeep
                3, 4, 5 -> Ground.mossMid
                6, 7 -> Ground.mossLit
                else -> Ground.grassLit
            }
            tufts += Tuft(blades, tone)
        }
    }
}

// --- shape helpers ----------------------------------------------------------

/** An irregular closed polygon. Straight edges, because stone has facets. */
private fun blob(cx: Float, cy: Float, rx: Float, ry: Float, points: Int, jitter: Float, rnd: Random): Path {
    val p = Path()
    for (i in 0 until points) {
        val a = (i.toFloat() / points) * 2f * PI.toFloat() + (rnd.nextFloat() - 0.5f) * 0.22f
        val f = 1f - jitter * 0.5f + rnd.nextFloat() * jitter
        val x = cx + cos(a) * rx * f
        val yy = cy + sin(a) * ry * f
        if (i == 0) p.moveTo(x, yy) else p.lineTo(x, yy)
    }
    p.close()
    return p
}

/** One blade of grass: wide at the root, a point at the tip, leaning as it rises. */
private fun bladePath(bx: Float, by: Float, hgt: Float, lean: Float): Path {
    val bw = (hgt * 0.11f).coerceAtLeast(0.6f)
    val tipX = bx + lean * hgt
    val tipY = by - hgt
    val c1x = bx + lean * hgt * 0.10f
    val c1y = by - hgt * 0.44f
    val c2x = bx + lean * hgt * 0.56f
    val c2y = by - hgt * 0.80f
    return Path().apply {
        moveTo(bx - bw, by)
        cubicTo(c1x - bw * 0.7f, c1y, c2x - bw * 0.35f, c2y, tipX, tipY)
        cubicTo(c2x + bw * 0.55f, c2y, c1x + bw * 0.9f, c1y, bx + bw, by)
        close()
    }
}

/** A dead leaf: a pointed oval, lying at whatever angle it fell. */
private fun leafPath(c: Offset, len: Float, wid: Float, rot: Float): Path {
    val cosR = cos(rot)
    val sinR = sin(rot)
    fun at(lx: Float, ly: Float) = Offset(c.x + lx * cosR - ly * sinR, c.y + lx * sinR + ly * cosR)
    val tail = at(-len, 0f)
    val tip = at(len, 0f)
    val upA = at(-len * 0.35f, -wid)
    val upB = at(len * 0.35f, -wid)
    val dnA = at(len * 0.35f, wid)
    val dnB = at(-len * 0.35f, wid)
    return Path().apply {
        moveTo(tail.x, tail.y)
        cubicTo(upA.x, upA.y, upB.x, upB.y, tip.x, tip.y)
        cubicTo(dnA.x, dnA.y, dnB.x, dnB.y, tail.x, tail.y)
        close()
    }
}

/** Multiplies a colour's brightness, so one stone palette can carry a hundred stones. */
private fun Color.shade(f: Float) = Color(
    red = (red * f).coerceIn(0f, 1f),
    green = (green * f).coerceIn(0f, 1f),
    blue = (blue * f).coerceIn(0f, 1f),
    alpha = alpha,
)

// --- the composable ---------------------------------------------------------

/**
 * Paints the battlefield floor. Fills whatever it is given; the board's contents are drawn over
 * the top of it.
 */
@Composable
fun BoardTerrain(modifier: Modifier = Modifier, seed: Int = 1257) {
    BoxWithConstraints(modifier) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        // Off-device the board is occasionally measured before it has a size; painting a plan
        // built from a zero or unbounded box would divide by zero.
        if (w > 1f && h > 1f && w < 20_000f && h < 20_000f) {
            val plan = remember(w, h, seed) { GroundPlan(w, h, seed) }
            Canvas(Modifier.fillMaxSize()) { drawGround(plan) }
        }
    }
}

private fun DrawScope.drawGround(plan: GroundPlan) {
    val w = plan.w
    val h = plan.h

    // 1. bare earth, dark at the far and near edges, warm through the middle
    drawRect(
        Brush.verticalGradient(
            0.00f to Ground.soilBlack,
            0.15f to Ground.soilDeep,
            0.34f to Ground.soilMid,
            0.50f to Ground.soilWarm,
            0.66f to Ground.soilMid,
            0.85f to Ground.soilDeep,
            1.00f to Ground.soilBlack,
        ),
    )

    // 2. broken colour in the soil
    plan.mottles.forEach { m ->
        drawCircle(
            brush = Brush.radialGradient(
                0f to m.color.copy(alpha = m.alpha),
                1f to Color.Transparent,
                center = m.c,
                radius = m.r,
            ),
            radius = m.r,
            center = m.c,
        )
    }

    // 3. moss, either side of where the path will go
    plan.mossBeds.forEach { m ->
        drawCircle(
            brush = Brush.radialGradient(
                0f to m.color.copy(alpha = m.alpha),
                0.7f to m.color.copy(alpha = m.alpha * 0.45f),
                1f to Color.Transparent,
                center = m.c,
                radius = m.r,
            ),
            radius = m.r,
            center = m.c,
        )
    }

    // 4. the bed the stones are set into, so every gap reads as dirt
    drawPath(plan.pathBed, Ground.soilBlack.copy(alpha = 0.62f))
    drawPath(plan.pathBed, Ground.dust.copy(alpha = 0.10f), style = Stroke(width = h * 0.012f))

    // 5. the cobbles
    plan.cobbles.forEach { c ->
        drawPath(
            c.path,
            Brush.verticalGradient(
                listOf(
                    Ground.stoneLit.shade(c.tone),
                    Ground.stoneMid.shade(c.tone),
                    Ground.stoneDark.shade(c.tone * 0.88f),
                ),
                startY = c.cy - c.hh,
                endY = c.cy + c.hh,
            ),
        )
        // The mortar gap is the shadow between stones, and is what makes them read as separate.
        drawPath(c.path, Ground.soilBlack.copy(alpha = 0.80f), style = Stroke(width = 1.2f))
        if (c.mossy) drawPath(c.path, Ground.mossMid.copy(alpha = 0.34f))
    }

    // 6. loose rock, each sitting in its own shadow
    plan.rocks.forEach { r ->
        drawOval(
            color = Ground.soilBlack.copy(alpha = 0.45f),
            topLeft = Offset(r.c.x - r.rx * 1.2f, r.c.y + r.ry * 0.15f),
            size = Size(r.rx * 2.4f, r.ry * 1.0f),
        )
        drawPath(
            r.body,
            Brush.verticalGradient(
                listOf(Ground.stoneLit.shade(r.tone), Ground.stoneMid.shade(r.tone), Ground.stoneDark.shade(r.tone * 0.8f)),
                startY = r.c.y - r.ry,
                endY = r.c.y + r.ry,
            ),
        )
        drawPath(r.facet, Ground.stonePale.copy(alpha = 0.18f))
        drawPath(r.body, Ground.soilBlack.copy(alpha = 0.55f), style = Stroke(width = 1f))
    }

    // 7. gravel
    plan.pebbles.forEach { p ->
        drawCircle(Ground.stoneMid.shade(p.tone).copy(alpha = 0.55f), radius = p.r, center = p.c)
    }

    // 8. roots crossing the ground
    plan.roots.forEach { r ->
        drawPath(r.path, Ground.soilBlack.copy(alpha = 0.55f), style = Stroke(width = r.width * 1.7f))
        drawPath(r.path, Ground.clay.copy(alpha = 0.40f), style = Stroke(width = r.width))
    }

    // 9. leaf litter
    plan.leaves.forEach { l -> drawPath(l.path, l.color.copy(alpha = 0.42f)) }

    // 10. grass, last of the ground layers because it stands above all of it
    plan.tufts.forEach { t ->
        t.blades.forEachIndexed { i, blade ->
            // One dark blade behind the clump is enough to seat it in the soil; painting a
            // shadow under every blade doubles the work for a difference no one can see.
            if (i == 0) drawPath(blade, Ground.soilBlack.copy(alpha = 0.55f))
            drawPath(blade, t.color.copy(alpha = 0.80f))
        }
    }

    // 11. the light that reaches the clearing
    drawRect(
        Brush.radialGradient(
            0.00f to Color(0x26FFE7AC),
            0.42f to Color(0x12F2D79C),
            1.00f to Color.Transparent,
            center = Offset(w * 0.5f, h * 0.40f),
            radius = maxOf(w, h) * 0.60f,
        ),
    )

    // 12. and the dark it falls away into
    drawRect(
        Brush.radialGradient(
            0.52f to Color.Transparent,
            1.00f to Color(0xDB070604),
            center = Offset(w * 0.5f, h * 0.42f),
            radius = maxOf(w, h) * 0.76f,
        ),
    )
    drawRect(
        Brush.verticalGradient(
            0.00f to Color(0xC2050403),
            0.13f to Color.Transparent,
            0.87f to Color.Transparent,
            1.00f to Color(0xC2050403),
        ),
    )
}
