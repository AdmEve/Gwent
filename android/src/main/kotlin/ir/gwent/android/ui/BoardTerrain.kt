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
 * The ground, seen in perspective.
 *
 * GWENT's board is a plane lying away from the camera, not a flat backdrop: the earth converges
 * toward the top of the screen, the stones of the path get smaller and closer together with
 * distance, and the far end of the clearing goes cold and hazy while the near end stays warm.
 * That recession is most of what makes the real board read as a place rather than a picture.
 *
 * So nothing here is positioned in screen coordinates. Every stone, tuft and pebble is placed on
 * the ground plane as (u, t) — u across, t away from the viewer — and projected. The projection
 * is the ordinary one: an object's size falls off as 1/(1 + depth), and equal steps of ground
 * bunch together toward the horizon because of it.
 *
 * Colours are sampled from CD PROJEKT RED's own in-game screenshots rather than invented: the
 * real ground is a cool grey-green in the distance and a warm olive up close, which is why an
 * orange-brown board never looked right no matter how much detail went into it.
 */

/** Earth, stone and green, sampled from the reference screenshots. */
object Ground {
    // near earth — #433F2C / #453D35 in the reference
    val soilNear = Color(0xFF4A4334)
    val soilMid = Color(0xFF3A362B)
    val soilDeep = Color(0xFF242A22)
    val soilBlack = Color(0xFF0E1412)
    val clay = Color(0xFF5C4E45)      // #5c4e45, the lit earth beside the melee rows
    val dust = Color(0xFF6E6154)

    // the cold, hazy far end — #111E1D / #253027
    val distance = Color(0xFF141E1C)

    val stoneDark = Color(0xFF26251F)
    val stoneMid = Color(0xFF4C463C)
    val stoneLit = Color(0xFF6E6154)
    val stonePale = Color(0xFF8E8472)

    val mossDeep = Color(0xFF1A2716)
    val mossMid = Color(0xFF2F4224)
    val mossLit = Color(0xFF466030)
    val grassLit = Color(0xFF688A38)
    val grassDry = Color(0xFF79683A)

    val leafRust = Color(0xFF63401C)
    val leafOchre = Color(0xFF84642C)
}

// --- scatter elements, already projected ------------------------------------

private class Mottle(val c: Offset, val r: Float, val color: Color, val alpha: Float)
private class Cobble(val path: Path, val shadow: Path, val cy: Float, val hh: Float, val lit: Color, val mid: Color, val dark: Color)
private class Rock(val body: Path, val facet: Path, val c: Offset, val rx: Float, val ry: Float, val lit: Color, val mid: Color)
private class Tuft(val blades: List<Path>, val color: Color)
private class Pebble(val c: Offset, val r: Float, val color: Color)
private class Grain(val c: Offset, val r: Float, val color: Color)
private class Leaf(val path: Path, val color: Color)
private class Root(val path: Path, val width: Float, val color: Color)

/**
 * The ground plane, laid out once for a given size.
 *
 * Built in the constructor rather than in the draw pass: a board redraw is a tap, not a frame
 * loop, but allocating several hundred [Path]s per redraw would still be waste.
 */
private class GroundPlan(val w: Float, val h: Float, seed: Int) {

    // --- the camera ---------------------------------------------------------
    private val depth = GwentBoard.GROUND_DEPTH
    private val scaleFar = 1f / (1f + depth)

    /** How large something at ground depth [t] appears. 1 at the viewer's feet, less beyond. */
    fun scaleAt(t: Float) = 1f / (1f + depth * t)

    /** Where depth [t] lands on screen: 0 at the bottom edge, 1 at the horizon. */
    private fun screenFrac(t: Float) = (1f - scaleAt(t)) / (1f - scaleFar)

    fun yAt(t: Float) = h * (1f - screenFrac(t))

    fun xAt(u: Float, t: Float) = w * 0.5f + (u - 0.5f) * w * scaleAt(t)

    /** The inverse of [yAt]: which ground depth shows at this fraction down the screen. */
    fun depthAtY(yFrac: Float): Float {
        val s = 1f - (1f - yFrac) * (1f - scaleFar)
        return ((1f / s) - 1f) / depth
    }

    /**
     * Distance drains colour and warmth. Everything far away is mixed toward [Ground.distance]
     * and dimmed, which is what separates the far rows from the near ones as strongly as size
     * does — and what the flat board had none of.
     */
    private fun hazed(color: Color, t: Float) = mix(color, Ground.distance, t * 0.62f)

    private fun hazeAlpha(t: Float) = 1f - t * 0.35f

    val mottles = ArrayList<Mottle>()
    val mossBeds = ArrayList<Mottle>()
    val cobbles = ArrayList<Cobble>()
    val rocks = ArrayList<Rock>()
    val tufts = ArrayList<Tuft>()
    val pebbles = ArrayList<Pebble>()
    val grain = ArrayList<Grain>()
    val leaves = ArrayList<Leaf>()
    val roots = ArrayList<Root>()
    val pathBed: Path

    // The path carries both melee rows and the centre line between them; grass takes the two
    // ranged rows and the hand. Both edges are read off the measured board rather than guessed.
    private val pathNearT = depthAtY(GwentBoard.HAND_TOP - 0.055f)
    private val pathFarT = depthAtY(GwentBoard.FIELD_TOP + 0.045f)

    private fun wobble(u: Float, freq: Float, phase: Float) =
        sin(u * freq + phase) * 0.62f + sin(u * freq * 2.7f + phase * 1.9f) * 0.38f

    private fun pathNear(u: Float) = pathNearT + wobble(u, 5.7f, 1.7f) * 0.022f
    private fun pathFar(u: Float) = pathFarT + wobble(u, 4.9f, 4.2f) * 0.026f

    init {
        val rnd = Random(seed)

        // The plane is generated wider than the screen: at the far end it is squeezed to half
        // its near width, so u must run well past [0,1] or the top corners come out empty.
        val uMin = -1.1f
        val uMax = 2.1f
        fun u(rnd: Random) = uMin + rnd.nextFloat() * (uMax - uMin)

        // --- soil: broken colour, so the earth is not a gradient ---------------
        val soils = listOf(Ground.soilMid, Ground.soilNear, Ground.soilDeep, Ground.clay, Ground.soilBlack, Ground.dust)
        repeat(140) {
            val t = rnd.nextFloat()
            val c = Offset(xAt(u(rnd), t), yAt(t))
            mottles += Mottle(
                c = c,
                r = h * (0.05f + rnd.nextFloat() * 0.20f) * scaleAt(t),
                color = hazed(soils.random(rnd), t),
                alpha = (0.10f + rnd.nextFloat() * 0.22f) * hazeAlpha(t),
            )
        }

        // --- moss, either side of the path --------------------------------------
        val mosses = listOf(Ground.mossDeep, Ground.mossMid, Ground.mossLit)
        repeat(54) {
            val uu = u(rnd)
            val t = if (rnd.nextBoolean()) {
                rnd.nextFloat() * pathNear(uu)
            } else {
                pathFar(uu) + rnd.nextFloat() * (1.05f - pathFar(uu))
            }.coerceIn(0f, 1.05f)
            mossBeds += Mottle(
                c = Offset(xAt(uu, t), yAt(t)),
                r = h * (0.07f + rnd.nextFloat() * 0.17f) * scaleAt(t),
                color = hazed(mosses.random(rnd), t),
                alpha = (0.16f + rnd.nextFloat() * 0.26f) * hazeAlpha(t),
            )
        }

        // --- the bed the stones are set into -------------------------------------
        pathBed = Path().apply {
            val step = (uMax - uMin) / 60f
            var uu = uMin
            moveTo(xAt(uu, pathNear(uu)), yAt(pathNear(uu)))
            while (uu <= uMax) { lineTo(xAt(uu, pathNear(uu)), yAt(pathNear(uu))); uu += step }
            uu = uMax
            while (uu >= uMin) { lineTo(xAt(uu, pathFar(uu)), yAt(pathFar(uu))); uu -= step }
            close()
        }

        // --- cobbles, laid on the plane so they converge with it -------------------
        // Constant steps of ground depth, which the projection turns into the bands
        // bunching up toward the far end all by itself.
        val du = 0.080f
        val dt = 0.052f
        var t = 0f
        var band = 0
        while (t < 1.06f) {
            val stagger = if (band % 2 == 0) 0f else du * 0.5f
            var uu = uMin + stagger
            // A stone is as tall as the gap to the next band and as wide as the gap to its
            // neighbour, both already foreshortened by the projection.
            // Sized to overlap their neighbours slightly, so the path is continuous stone with
            // joints rather than islands with mud between them.
            val hhScreen = (yAt(t) - yAt(t + dt)) * 0.72f
            val hwScreen = du * w * scaleAt(t) * 0.66f
            while (uu < uMax) {
                val cu = uu + (rnd.nextFloat() - 0.5f) * du * 0.30f
                val ct = t + (rnd.nextFloat() - 0.5f) * dt * 0.32f
                val near = pathNear(cu)
                val far = pathFar(cu)
                val keep = when {
                    ct > near + dt * 0.4f && ct < far - dt * 0.4f -> rnd.nextFloat() > 0.09f
                    ct > near - dt && ct < far + dt -> rnd.nextFloat() > 0.64f
                    else -> false
                }
                if (keep && hhScreen > 0.6f) {
                    val cx = xAt(cu, ct)
                    val cy = yAt(ct)
                    // A wide tonal spread is what stone actually looks like. The first attempt
                    // used a narrow one and every stone came out the same grey.
                    val tone = 0.48f + rnd.nextFloat() * 1.05f
                    val warm = rnd.nextFloat() < 0.4f
                    val rx = hwScreen * (0.66f + rnd.nextFloat() * 0.52f)
                    val ry = hhScreen * (0.62f + rnd.nextFloat() * 0.56f)
                    cobbles += Cobble(
                        path = blob(cx, cy, rx, ry, 6 + rnd.nextInt(4), 0.42f, rnd),
                        shadow = blob(cx, cy + ry * 0.30f, rx * 1.02f, ry * 0.92f, 7, 0.40f, rnd),
                        cy = cy,
                        hh = ry,
                        lit = hazed((if (warm) Ground.dust else Ground.stoneLit).shade(tone), ct),
                        mid = hazed(Ground.stoneMid.shade(tone * 0.86f), ct),
                        dark = hazed(Ground.stoneDark.shade(tone * 0.62f), ct),
                    )
                }
                uu += du
            }
            t += dt
            band++
        }

        // --- loose rock -------------------------------------------------------------
        repeat(42) {
            val tt = rnd.nextFloat()
            val c = Offset(xAt(u(rnd), tt), yAt(tt))
            val rx = h * (0.018f + rnd.nextFloat() * 0.034f) * scaleAt(tt)
            val ry = rx * (0.56f + rnd.nextFloat() * 0.34f)
            val tone = 0.72f + rnd.nextFloat() * 0.54f
            rocks += Rock(
                body = blob(c.x, c.y, rx, ry, 9, 0.32f, rnd),
                facet = blob(c.x - rx * 0.24f, c.y - ry * 0.28f, rx * 0.50f, ry * 0.44f, 7, 0.36f, rnd),
                c = c, rx = rx, ry = ry,
                lit = hazed(Ground.stoneLit.shade(tone), tt),
                mid = hazed(Ground.stoneMid.shade(tone), tt),
            )
        }

        // --- gravel -------------------------------------------------------------------
        repeat(300) {
            val tt = rnd.nextFloat()
            pebbles += Pebble(
                c = Offset(xAt(u(rnd), tt), yAt(tt)),
                r = (h * (0.0026f + rnd.nextFloat() * 0.0062f) * scaleAt(tt)).coerceAtLeast(0.5f),
                color = hazed(Ground.stoneMid.shade(0.6f + rnd.nextFloat() * 0.8f), tt)
                    .copy(alpha = 0.55f * hazeAlpha(tt)),
            )
        }

        // --- grain ----------------------------------------------------------------------
        // Ground at this distance is not a collection of things you can name, it is texture.
        // Without a field of small marks breaking the fill, every surface above reads as a flat
        // wash with cartoon objects sitting on it — which is exactly how the first attempt came
        // out. These go over the stones as well as the soil, so nothing stays perfectly smooth.
        val grains = listOf(Ground.soilBlack, Ground.soilNear, Ground.clay, Ground.dust, Ground.stoneDark, Ground.mossDeep)
        repeat(1800) {
            val tt = rnd.nextFloat()
            grain += Grain(
                c = Offset(xAt(u(rnd), tt), yAt(tt)),
                r = (h * (0.0012f + rnd.nextFloat() * 0.0042f) * scaleAt(tt)).coerceAtLeast(0.45f),
                color = hazed(grains.random(rnd), tt).copy(alpha = (0.10f + rnd.nextFloat() * 0.26f) * hazeAlpha(tt)),
            )
        }

        // --- roots crossing the soil ------------------------------------------------
        repeat(16) {
            val tt = rnd.nextFloat()
            val u0 = u(rnd)
            val len = (uMax - uMin) * (0.08f + rnd.nextFloat() * 0.20f)
            fun drift() = (rnd.nextFloat() - 0.5f) * 0.06f
            roots += Root(
                path = Path().apply {
                    moveTo(xAt(u0, tt), yAt(tt))
                    cubicTo(
                        xAt(u0 + len * 0.30f, tt + drift()), yAt(tt + drift()),
                        xAt(u0 + len * 0.68f, tt + drift()), yAt(tt + drift()),
                        xAt(u0 + len, tt), yAt(tt + drift() * 0.5f),
                    )
                },
                width = (h * (0.004f + rnd.nextFloat() * 0.007f) * scaleAt(tt)).coerceAtLeast(0.7f),
                color = hazed(Ground.clay, tt).copy(alpha = 0.45f * hazeAlpha(tt)),
            )
        }

        // --- leaf litter ----------------------------------------------------------------
        val leafTones = listOf(Ground.leafRust, Ground.leafOchre, Ground.grassDry, Ground.clay)
        repeat(80) {
            val tt = rnd.nextFloat()
            val c = Offset(xAt(u(rnd), tt), yAt(tt))
            val len = h * (0.013f + rnd.nextFloat() * 0.021f) * scaleAt(tt)
            leaves += Leaf(
                path = leafPath(c, len, len * (0.34f + rnd.nextFloat() * 0.26f), rnd.nextFloat() * PI.toFloat()),
                color = hazed(leafTones.random(rnd), tt).copy(alpha = 0.44f * hazeAlpha(tt)),
            )
        }

        // --- grass -------------------------------------------------------------------------
        repeat(320) {
            val uu = u(rnd)
            val near = pathNear(uu)
            val far = pathFar(uu)
            // Grass belongs off the path, creeps back over its edges, and sprouts in its gaps.
            val tt = when (rnd.nextInt(10)) {
                0, 1, 2 -> rnd.nextFloat() * near
                3, 4, 5 -> far + rnd.nextFloat() * (1.05f - far)
                6, 7 -> near + (rnd.nextFloat() - 0.5f) * 0.06f
                8 -> far + (rnd.nextFloat() - 0.5f) * 0.06f
                else -> near + rnd.nextFloat() * (far - near)
            }.coerceIn(0f, 1.05f)

            val cx = xAt(uu, tt)
            val cy = yAt(tt)
            val clump = h * (0.011f + rnd.nextFloat() * 0.030f) * scaleAt(tt)
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
            // Held well back in value. Grass this far from the camera is a texture, and the
            // saturated green spikes of the first attempt read as a cartoon lawn.
            tufts += Tuft(blades, mix(hazed(tone, tt), Ground.soilDeep, 0.34f)
                .copy(alpha = (0.42f + rnd.nextFloat() * 0.26f) * hazeAlpha(tt)))
        }
    }
}

// --- shape and colour helpers -----------------------------------------------

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
    val bw = (hgt * 0.11f).coerceAtLeast(0.5f)
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

/** Multiplies a colour's brightness, so one stone palette can carry hundreds of stones. */
private fun Color.shade(f: Float) = Color(
    red = (red * f).coerceIn(0f, 1f),
    green = (green * f).coerceIn(0f, 1f),
    blue = (blue * f).coerceIn(0f, 1f),
    alpha = alpha,
)

/** Blends two colours. Used for aerial perspective, which is most of the sense of depth. */
private fun mix(a: Color, b: Color, f: Float): Color {
    val g = f.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * g,
        green = a.green + (b.green - a.green) * g,
        blue = a.blue + (b.blue - a.blue) * g,
        alpha = a.alpha,
    )
}

// --- the composable ---------------------------------------------------------

/**
 * Paints the battlefield floor in perspective. Fills whatever it is given; everything else on
 * the board is drawn over the top of it.
 */
@Composable
fun BoardTerrain(modifier: Modifier = Modifier, seed: Int = 1257) {
    BoxWithConstraints(modifier) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        // Off-device the board is occasionally measured before it has a size, and a plan built
        // from a zero or unbounded box would divide by zero.
        if (w > 1f && h > 1f && w < 20_000f && h < 20_000f) {
            val plan = remember(w, h, seed) { GroundPlan(w, h, seed) }
            Canvas(Modifier.fillMaxSize()) { drawGround(plan) }
        }
    }
}

private fun DrawScope.drawGround(plan: GroundPlan) {
    val w = plan.w
    val h = plan.h

    // 1. bare earth: cold and dark at the horizon, warm olive at the viewer's feet
    drawRect(
        Brush.verticalGradient(
            0.00f to Ground.distance,
            0.12f to Ground.soilBlack,
            0.30f to Ground.soilDeep,
            0.52f to Ground.soilMid,
            0.72f to Ground.soilDeep,
            1.00f to Ground.soilBlack,
        ),
    )

    // 2. broken colour in the soil
    plan.mottles.forEach { m -> softBlob(m) }

    // 3. moss, either side of the path
    plan.mossBeds.forEach { m -> softBlob(m) }

    // 4. the bed the stones are set into, so every gap reads as dirt
    drawPath(plan.pathBed, Ground.soilBlack.copy(alpha = 0.55f))

    // 5. the cobbles, converging with the plane
    // Each stone lays down the shadow in its own joint first, then covers most of it. There is
    // deliberately no outline: a line drawn round every stone is what made the first attempt
    // read as a cartoon rather than as paving.
    plan.cobbles.forEach { c ->
        drawPath(c.shadow, Ground.soilBlack.copy(alpha = 0.30f))
        drawPath(
            c.path,
            Brush.verticalGradient(listOf(c.lit, c.mid, c.dark), startY = c.cy - c.hh, endY = c.cy + c.hh),
        )
    }

    // 6. loose rock, each sitting in its own shadow
    plan.rocks.forEach { r ->
        drawOval(
            color = Ground.soilBlack.copy(alpha = 0.42f),
            topLeft = Offset(r.c.x - r.rx * 1.2f, r.c.y + r.ry * 0.15f),
            size = Size(r.rx * 2.4f, r.ry),
        )
        drawPath(
            r.body,
            Brush.verticalGradient(listOf(r.lit, r.mid, Ground.stoneDark), startY = r.c.y - r.ry, endY = r.c.y + r.ry),
        )
        drawPath(r.facet, Ground.stonePale.copy(alpha = 0.16f))
    }

    // 7. gravel, then the grain that keeps every surface from reading as a flat fill
    plan.pebbles.forEach { p -> drawCircle(p.color, radius = p.r, center = p.c) }
    plan.grain.forEach { g -> drawCircle(g.color, radius = g.r, center = g.c) }

    // 8. roots crossing the ground
    plan.roots.forEach { r ->
        drawPath(r.path, Ground.soilBlack.copy(alpha = 0.5f), style = Stroke(width = r.width * 1.7f))
        drawPath(r.path, r.color, style = Stroke(width = r.width))
    }

    // 9. leaf litter
    plan.leaves.forEach { l -> drawPath(l.path, l.color) }

    // 10. grass, last of the ground layers because it stands above all of it
    plan.tufts.forEach { t ->
        t.blades.forEachIndexed { i, blade ->
            // One dark blade behind the clump seats it in the soil; a shadow under every blade
            // doubles the work for a difference no one can see.
            if (i == 0) drawPath(blade, Ground.soilBlack.copy(alpha = 0.5f))
            drawPath(blade, t.color)
        }
    }

    // 11. the light that reaches the clearing, over the play rather than the window
    drawRect(
        Brush.radialGradient(
            0.00f to Color(0x24FFE7AC),
            0.42f to Color(0x10F2D79C),
            1.00f to Color.Transparent,
            center = Offset(w * 0.5f, h * GwentBoard.CENTRE_LINE),
            radius = maxOf(w, h) * 0.58f,
        ),
    )

    // 12. and the dark it falls away into
    drawRect(
        Brush.radialGradient(
            0.34f to Color.Transparent,
            1.00f to Color(0xF2040705),
            center = Offset(w * 0.5f, h * 0.46f),
            radius = maxOf(w, h) * 0.66f,
        ),
    )
    drawRect(
        Brush.verticalGradient(
            0.00f to Color(0xE8040706),
            0.20f to Color.Transparent,
            0.86f to Color.Transparent,
            1.00f to Color(0xC4040706),
        ),
    )
}

private fun DrawScope.softBlob(m: Mottle) {
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
