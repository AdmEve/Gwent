package ir.gwent.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import ir.gwent.core.model.Card
import ir.gwent.core.model.CardColor
import ir.gwent.core.model.CardType
import ir.gwent.core.model.Tag

/**
 * Card artwork, drawn rather than loaded.
 *
 * GWENT gives every card a painted illustration. With no artwork to ship, the alternative is not
 * a blank frame — it is a drawn one. Each card resolves to an [ArtMotif] from its tags and type,
 * and the motif is rendered as vector paths: a sword for a soldier, a ship for a pirate, a gear
 * for a siege engine. Vectors stay crisp at any size, cost nothing to ship, and take the faction
 * palette, so the board reads as a set of real cards rather than a grid of gradients.
 *
 * All geometry is authored in a unit box (0..1) and scaled to the card, so one motif serves the
 * small board card and the larger hand card alike.
 */
enum class ArtMotif {
    SWORD,      // soldiers, infantry, generic fighters
    SHIELD,     // armoured units, defenders
    BOW,        // archers and ranged specialists
    FANG,       // beasts, necrophages, monsters
    GEAR,       // machines and siege engines
    ARCANE,     // mages and spells
    SHIP,       // pirates and Skellige raiders
    COIN,       // Syndicate, crimes, bounty
    SKULL,      // cursed, doomed, the graveyard-minded
    CROWN,      // aristocrats, leaders, knights of rank
    LEAF,       // dryads, treants, nature
    FLASK,      // alchemy, organic, potions
}

/**
 * Art direction, per card.
 *
 * Inference from tags gets most cards right, but not all: an archer is tagged Human and Soldier
 * like any infantryman, so tag order alone drew it a sword. Anything whose subject the tags do
 * not capture is named here explicitly — this map is the art director's override, and it wins.
 */
private val EXPLICIT: Map<String, ArtMotif> = mapOf(
    "neu_archer" to ArtMotif.BOW,          // Crossbowman — a bow, not a blade
    "sco_dryad" to ArtMotif.BOW,           // Dryad Ranger
    "nor_ballista" to ArtMotif.BOW,        // Siege Ballista is a bow the size of a cart
    "nor_trebuchet" to ArtMotif.GEAR,
    "neu_medic" to ArtMotif.FLASK,         // Field Medic tends wounds, not swings
    "nil_spy" to ArtMotif.COIN,            // Imperial Informant deals in secrets sold
    "nil_bribery" to ArtMotif.COIN,
    "ske_priestess" to ArtMotif.ARCANE,
    "mon_harpy" to ArtMotif.FANG,
    "neu_scorch" to ArtMotif.ARCANE,
    "neu_alzurs_thunder" to ArtMotif.ARCANE,
    "neu_swallow" to ArtMotif.FLASK,
)

/** Pick the motif for a card: the explicit override first, then inference from tags. */
fun motifFor(card: Card): ArtMotif {
    EXPLICIT[card.id]?.let { return it }
    val t = card.tags
    return when {
        Tag.SIEGE_ENGINE in t || Tag.MACHINE in t -> ArtMotif.GEAR
        Tag.PIRATE in t -> ArtMotif.SHIP
        Tag.CRIME in t || Tag.BANDIT in t -> ArtMotif.COIN
        Tag.CURSED in t || Tag.SPECTER in t || Tag.NECROPHAGE in t -> ArtMotif.SKULL
        Tag.TREANT in t || Tag.DRYAD in t || Tag.NATURE in t -> ArtMotif.LEAF
        Tag.ALCHEMY in t || Tag.ORGANIC in t -> ArtMotif.FLASK
        Tag.MAGE in t || Tag.SPELL in t -> ArtMotif.ARCANE
        Tag.ARISTOCRAT in t || Tag.KNIGHT in t -> ArtMotif.CROWN
        Tag.BEAST in t || Tag.INSECTOID in t || Tag.VAMPIRE in t ||
            Tag.DRACONID in t || Tag.OGROID in t || Tag.RELICT in t -> ArtMotif.FANG
        card.type == CardType.SPECIAL -> ArtMotif.ARCANE
        // A card that only ever acts from the ranged row is an archer, whatever else it is.
        card.abilities.any { it.row == ir.gwent.core.model.Row.RANGED } -> ArtMotif.BOW
        card.armor > 0 -> ArtMotif.SHIELD
        Tag.ELF in t || Tag.DWARF in t -> ArtMotif.BOW
        Tag.SOLDIER in t || Tag.HUMAN in t || Tag.WITCHER in t -> ArtMotif.SWORD
        else -> ArtMotif.SWORD
    }
}

/** Build the motif's outline inside a unit box, then scale it to the drawing area. */
private fun motifPath(motif: ArtMotif, w: Float, h: Float): Path {
    val p = Path()
    fun x(v: Float) = v * w
    fun y(v: Float) = v * h

    when (motif) {
        ArtMotif.SWORD -> {
            // Blade
            p.moveTo(x(0.50f), y(0.10f))
            p.lineTo(x(0.57f), y(0.24f))
            p.lineTo(x(0.55f), y(0.62f))
            p.lineTo(x(0.45f), y(0.62f))
            p.lineTo(x(0.43f), y(0.24f))
            p.close()
            // Crossguard
            p.moveTo(x(0.26f), y(0.62f))
            p.lineTo(x(0.74f), y(0.62f))
            p.lineTo(x(0.74f), y(0.69f))
            p.lineTo(x(0.26f), y(0.69f))
            p.close()
            // Grip and pommel
            p.moveTo(x(0.46f), y(0.69f))
            p.lineTo(x(0.54f), y(0.69f))
            p.lineTo(x(0.54f), y(0.86f))
            p.lineTo(x(0.46f), y(0.86f))
            p.close()
            p.addOval(androidx.compose.ui.geometry.Rect(x(0.44f), y(0.85f), x(0.56f), y(0.95f)))
        }

        ArtMotif.SHIELD -> {
            p.moveTo(x(0.50f), y(0.10f))
            p.lineTo(x(0.82f), y(0.24f))
            p.lineTo(x(0.82f), y(0.56f))
            p.cubicTo(x(0.82f), y(0.76f), x(0.66f), y(0.88f), x(0.50f), y(0.94f))
            p.cubicTo(x(0.34f), y(0.88f), x(0.18f), y(0.76f), x(0.18f), y(0.56f))
            p.lineTo(x(0.18f), y(0.24f))
            p.close()
            // Boss
            p.addOval(androidx.compose.ui.geometry.Rect(x(0.42f), y(0.44f), x(0.58f), y(0.60f)))
        }

        ArtMotif.BOW -> {
            // Stave
            p.moveTo(x(0.34f), y(0.10f))
            p.cubicTo(x(0.74f), y(0.30f), x(0.74f), y(0.70f), x(0.34f), y(0.90f))
            // String
            p.moveTo(x(0.34f), y(0.10f))
            p.lineTo(x(0.34f), y(0.90f))
            // Arrow
            p.moveTo(x(0.22f), y(0.50f))
            p.lineTo(x(0.68f), y(0.50f))
            p.moveTo(x(0.62f), y(0.43f))
            p.lineTo(x(0.72f), y(0.50f))
            p.lineTo(x(0.62f), y(0.57f))
        }

        ArtMotif.FANG -> {
            // Three claw marks
            for (i in 0..2) {
                val off = 0.26f + i * 0.20f
                p.moveTo(x(off), y(0.14f))
                p.cubicTo(x(off + 0.07f), y(0.42f), x(off + 0.05f), y(0.66f), x(off - 0.02f), y(0.88f))
            }
        }

        ArtMotif.GEAR -> {
            val cx = 0.50f; val cy = 0.50f; val rOuter = 0.34f; val rInner = 0.24f
            val teeth = 8
            for (i in 0 until teeth * 2) {
                val r = if (i % 2 == 0) rOuter else rInner
                val a = (i * Math.PI / teeth).toFloat()
                val px = cx + r * kotlin.math.cos(a)
                val py = cy + r * kotlin.math.sin(a)
                if (i == 0) p.moveTo(x(px), y(py)) else p.lineTo(x(px), y(py))
            }
            p.close()
            p.addOval(androidx.compose.ui.geometry.Rect(x(0.42f), y(0.42f), x(0.58f), y(0.58f)))
        }

        ArtMotif.ARCANE -> {
            // Six-pointed star drawn as two triangles
            p.moveTo(x(0.50f), y(0.14f))
            p.lineTo(x(0.79f), y(0.64f))
            p.lineTo(x(0.21f), y(0.64f))
            p.close()
            p.moveTo(x(0.50f), y(0.86f))
            p.lineTo(x(0.21f), y(0.36f))
            p.lineTo(x(0.79f), y(0.36f))
            p.close()
        }

        ArtMotif.SHIP -> {
            // Hull
            p.moveTo(x(0.14f), y(0.66f))
            p.lineTo(x(0.86f), y(0.66f))
            p.cubicTo(x(0.78f), y(0.86f), x(0.22f), y(0.86f), x(0.14f), y(0.66f))
            p.close()
            // Mast
            p.moveTo(x(0.48f), y(0.16f))
            p.lineTo(x(0.52f), y(0.16f))
            p.lineTo(x(0.52f), y(0.66f))
            p.lineTo(x(0.48f), y(0.66f))
            p.close()
            // Sail
            p.moveTo(x(0.52f), y(0.22f))
            p.lineTo(x(0.78f), y(0.44f))
            p.lineTo(x(0.52f), y(0.60f))
            p.close()
        }

        ArtMotif.COIN -> {
            p.addOval(androidx.compose.ui.geometry.Rect(x(0.20f), y(0.20f), x(0.80f), y(0.80f)))
            p.addOval(androidx.compose.ui.geometry.Rect(x(0.32f), y(0.32f), x(0.68f), y(0.68f)))
            p.moveTo(x(0.50f), y(0.34f))
            p.lineTo(x(0.50f), y(0.66f))
        }

        ArtMotif.SKULL -> {
            // Cranium
            p.moveTo(x(0.26f), y(0.48f))
            p.cubicTo(x(0.26f), y(0.18f), x(0.74f), y(0.18f), x(0.74f), y(0.48f))
            p.lineTo(x(0.68f), y(0.66f))
            p.lineTo(x(0.32f), y(0.66f))
            p.close()
            // Eye sockets
            p.addOval(androidx.compose.ui.geometry.Rect(x(0.34f), y(0.38f), x(0.45f), y(0.50f)))
            p.addOval(androidx.compose.ui.geometry.Rect(x(0.55f), y(0.38f), x(0.66f), y(0.50f)))
            // Jaw
            p.moveTo(x(0.38f), y(0.66f))
            p.lineTo(x(0.62f), y(0.66f))
            p.lineTo(x(0.58f), y(0.80f))
            p.lineTo(x(0.42f), y(0.80f))
            p.close()
        }

        ArtMotif.CROWN -> {
            p.moveTo(x(0.18f), y(0.70f))
            p.lineTo(x(0.24f), y(0.30f))
            p.lineTo(x(0.37f), y(0.52f))
            p.lineTo(x(0.50f), y(0.24f))
            p.lineTo(x(0.63f), y(0.52f))
            p.lineTo(x(0.76f), y(0.30f))
            p.lineTo(x(0.82f), y(0.70f))
            p.close()
            p.moveTo(x(0.20f), y(0.76f))
            p.lineTo(x(0.80f), y(0.76f))
            p.lineTo(x(0.80f), y(0.84f))
            p.lineTo(x(0.20f), y(0.84f))
            p.close()
        }

        ArtMotif.LEAF -> {
            p.moveTo(x(0.50f), y(0.12f))
            p.cubicTo(x(0.84f), y(0.34f), x(0.80f), y(0.72f), x(0.50f), y(0.90f))
            p.cubicTo(x(0.20f), y(0.72f), x(0.16f), y(0.34f), x(0.50f), y(0.12f))
            p.close()
            // Midrib
            p.moveTo(x(0.50f), y(0.20f))
            p.lineTo(x(0.50f), y(0.86f))
        }

        ArtMotif.FLASK -> {
            // Neck
            p.moveTo(x(0.42f), y(0.14f))
            p.lineTo(x(0.58f), y(0.14f))
            p.lineTo(x(0.58f), y(0.38f))
            p.lineTo(x(0.42f), y(0.38f))
            p.close()
            // Body
            p.moveTo(x(0.42f), y(0.38f))
            p.cubicTo(x(0.18f), y(0.58f), x(0.24f), y(0.90f), x(0.50f), y(0.90f))
            p.cubicTo(x(0.76f), y(0.90f), x(0.82f), y(0.58f), x(0.58f), y(0.38f))
            p.close()
        }
    }
    return p
}

/** Draws the motif twice: a soft glow beneath, a crisp stroke on top. */
private fun DrawScope.drawMotif(
    motif: ArtMotif, tint: Color, glow: Color, area: Size, top: Float, left: Float = 0f,
) {
    val path = motifPath(motif, area.width, area.height)
    translate(left, top) {
        drawPath(path, color = glow.copy(alpha = 0.22f), style = Stroke(width = 3.2f))
        drawPath(path, color = tint.copy(alpha = 0.85f), style = Stroke(width = 1.1f))
    }
}

private inline fun DrawScope.translate(dx: Float, dy: Float, block: DrawScope.() -> Unit) {
    drawContext.transform.translate(dx, dy)
    block()
    drawContext.transform.translate(-dx, -dy)
}

/**
 * The card's art field: a lit ground, the motif, and a gold wash for golds.
 *
 * Sized to the upper portion of the card so the name plate at the foot never covers the subject.
 */
@Composable
fun CardSigil(card: Card, modifier: Modifier = Modifier) {
    val palette = factionPalette(card.faction)
    val motif = motifFor(card)

    // A stable per-card jitter: two Hired Blades should not be pixel-identical stamps.
    val seed = card.id.fold(17) { a, c -> a * 31 + c.code }
    val tilt = ((seed ushr 4) and 0xF) / 15f * 10f - 5f      // -5..+5 degrees
    val scale = 0.94f + ((seed ushr 9) and 0xF) / 15f * 0.12f // 0.94..1.06

    Canvas(modifier = modifier) {
        // The subject occupies the top ~72% of the card; the rest is name plate.
        val artH = size.height * 0.72f
        val area = Size(size.width * scale, artH * scale)

        // A pool of light behind the subject, so it reads as lit rather than flat.
        drawCircle(
            brush = Brush.radialGradient(
                listOf(palette.glow.copy(alpha = 0.30f), Color.Transparent),
                center = Offset(size.width / 2f, artH * 0.52f),
                radius = size.width * 0.62f,
            ),
            radius = size.width * 0.62f,
            center = Offset(size.width / 2f, artH * 0.52f),
        )

        val dx = (size.width - area.width) / 2f
        rotate(tilt, pivot = Offset(size.width / 2f, artH * 0.5f)) {
            drawMotif(motif, palette.glow, palette.accent, area, top = artH * 0.04f, left = dx)
        }

        // Golds carry a wash of light across the top edge, as gold cards do in the real game.
        if (card.color == CardColor.GOLD) {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(GoldLight.copy(alpha = 0.22f), Color.Transparent),
                    startY = 0f,
                    endY = size.height * 0.45f,
                ),
                size = size,
            )
        }
    }
}
