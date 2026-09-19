package ir.gwent.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.gwent.core.model.*

/** Board cards sit at roughly 2:3, as in the real game. */
val CardWidth = 44.dp
val CardHeight = 62.dp
val HandCardWidth = 52.dp
val HandCardHeight = 76.dp

/**
 * The power gem: a diamond in the top-left corner carrying the unit's current power.
 *
 * The colour is the single most important readout on the board — green means the unit is boosted
 * above its base, red means it has been damaged below it, parchment means untouched.
 */
@Composable
private fun PowerGem(power: Int, base: Int, size: Dp = 20.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .rotate(45f)
            .background(Color(0xE6101010), RoundedCornerShape(3.dp))
            .border(1.dp, powerColor(power, base).copy(alpha = 0.85f), RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = power.toString(),
            style = PowerNumeral.copy(color = powerColor(power, base)),
            modifier = Modifier.rotate(-45f),
        )
    }
}

/** A small steel pip for armour, shown only when the unit actually has some. */
@Composable
private fun ArmorPip(armor: Int) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .background(Color(0xE6182028), RoundedCornerShape(8.dp))
            .border(1.dp, ArmorSteel.copy(alpha = 0.8f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(armor.toString(), style = PowerNumeral.copy(fontSize = 10.sp, color = ArmorSteel))
    }
}

/** Status glyphs run along the bottom of the card, as in the real game. */
private fun statusGlyph(status: Status): Pair<String, Color>? = when (status) {
    Status.BLEEDING -> "●" to DamageRed
    Status.VITALITY -> "●" to BoostGreen
    Status.POISON -> "☠" to Color(0xFF8ED04A)
    Status.SHIELD -> "◆" to ArmorSteel
    Status.LOCKED -> "✖" to Color(0xFFB07BD0)
    Status.DOOMED -> "☠" to Color(0xFFB0A0C0)
    Status.IMMUNITY -> "✦" to GoldPale
    Status.VEIL -> "○" to Color(0xFFA8B8D8)
    Status.RESILIENCE -> "↻" to GoldMuted
    Status.DEFENDER -> "▲" to ArmorSteel
    Status.SPYING -> "◉" to Color(0xFFD08A4A)
    Status.RUPTURE -> "✳" to DamageRed
    Status.BOUNTY -> "●" to GoldBright
    Status.ARMOR -> null
}

/**
 * A unit on the battlefield. Card art fills the face and the name sits on a scrim at the foot —
 * board cards carry no rules text in GWENT; you read them from the art, the number and the gem.
 */
@Composable
fun BoardCardView(
    unit: UnitInstance,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val palette = factionPalette(unit.card.faction)
    Box(
        modifier = Modifier
            .size(CardWidth, CardHeight)
            .clip(RoundedCornerShape(4.dp))
            .background(Brush.verticalGradient(listOf(palette.deep, Color(0xFF0B0A06))))
            .border(
                width = if (selected) 2.dp else 1.dp,
                brush = if (selected) MetalGold else frameBrush(unit.card.color),
                shape = RoundedCornerShape(4.dp),
            )
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
    ) {
        // Stand-in for card art until real artwork exists.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(palette.accent.copy(alpha = 0.30f), Color.Transparent),
                    ),
                ),
        )

        PowerGem(unit.power, unit.basePower, 20.dp)

        if (unit.armor > 0) {
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(2.dp)) { ArmorPip(unit.armor) }
        }

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            unit.statuses.keys.mapNotNull { statusGlyph(it) }.take(4).forEach { (glyph, tint) ->
                Text(glyph, style = CardName.copy(color = tint, fontSize = 9.sp))
            }
        }

        Text(
            text = unit.card.name,
            style = CardName,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xCC000000))
                .padding(horizontal = 2.dp, vertical = 1.dp),
        )
    }
}

/**
 * A card in hand. Shows provisions rather than nothing in the corner opposite power, because in
 * hand the provision cost is what tells you whether it was worth its slot in the deck.
 */
@Composable
fun HandCardView(
    card: Card,
    selected: Boolean = false,
    playable: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val palette = factionPalette(card.faction)
    Box(
        modifier = Modifier
            .size(HandCardWidth, HandCardHeight)
            .clip(RoundedCornerShape(5.dp))
            .background(Brush.verticalGradient(listOf(palette.deep, Color(0xFF0B0A06))))
            .border(
                width = if (selected) 3.dp else 1.dp,
                brush = if (selected) MetalGold else frameBrush(card.color),
                shape = RoundedCornerShape(5.dp),
            )
            .then(if (onClick != null && playable) Modifier.clickable { onClick() } else Modifier),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(listOf(palette.accent.copy(alpha = 0.32f), Color.Transparent)),
                ),
        )

        if (card.isUnit) PowerGem(card.basePower, card.basePower, 22.dp)

        // Provision cost, top-right.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(3.dp)
                .size(17.dp)
                .background(Color(0xE61B160A), RoundedCornerShape(9.dp))
                .border(1.dp, GoldDeep, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(card.provisions.toString(), style = PowerNumeral.copy(fontSize = 10.sp, color = GoldMuted))
        }

        Text(
            text = card.name,
            style = CardName,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xCC000000))
                .padding(horizontal = 2.dp, vertical = 2.dp),
        )
    }
}
