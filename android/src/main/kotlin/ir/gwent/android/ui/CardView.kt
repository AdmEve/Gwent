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

/**
 * Card sizes.
 *
 * Tuned for a phone in landscape (~891x411dp). Four rows and a hand have to share 411dp of
 * height, which is what caps the board card — but the cards still need to dominate the board
 * rather than float in it, so they take most of each row band.
 */
val CardWidth = 48.dp
val CardHeight = 66.dp
val HandCardWidth = 66.dp
val HandCardHeight = 94.dp

/**
 * The power gem: a diamond in the top-left corner carrying the unit's current power.
 *
 * The colour is the single most important readout on the board — green means boosted above base,
 * red means damaged below it, parchment means untouched.
 */
@Composable
private fun PowerGem(power: Int, base: Int, size: Dp) {
    val tint = powerColor(power, base)
    Box(
        modifier = Modifier.size(size).rotate(45f)
            .background(
                Brush.verticalGradient(listOf(Color(0xFF1C1810), Color(0xFF080706))),
                RoundedCornerShape(3.dp),
            )
            .border(1.dp, tint.copy(alpha = 0.9f), RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = power.toString(),
            style = PowerNumeral.copy(color = tint, fontSize = (size.value * 0.46f).sp),
            modifier = Modifier.rotate(-45f),
        )
    }
}

/** A steel pip for armour, shown only when the unit has some. */
@Composable
private fun ArmorPip(armor: Int, size: Dp) {
    Box(
        modifier = Modifier.size(size)
            .background(Color(0xE6202A34), RoundedCornerShape(size / 2))
            .border(1.dp, ArmorSteel.copy(alpha = 0.85f), RoundedCornerShape(size / 2)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            armor.toString(),
            style = PowerNumeral.copy(fontSize = (size.value * 0.55f).sp, color = ArmorSteel),
        )
    }
}

/** Status glyphs run along the foot of the card, as they do on the real board. */
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
 * A unit on the battlefield.
 *
 * Board cards in GWENT carry no rules text — you read them from the art, the power gem and the
 * frame. The frame colour is the card's class: gold for gold, bronze for bronze.
 */
@Composable
fun BoardCardView(
    unit: UnitInstance,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .size(CardWidth, CardHeight)
            .clip(RoundedCornerShape(3.dp))
            .background(cardArt(unit.card.faction))
            .border(
                width = if (selected) 2.dp else 1.dp,
                brush = if (selected) MetalGold else frameBrush(unit.card.color),
                shape = RoundedCornerShape(3.dp),
            )
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
    ) {
        CardSigil(unit.card, Modifier.fillMaxSize())
        PowerGem(unit.power, unit.basePower, 21.dp)

        if (unit.armor > 0) {
            Box(Modifier.align(Alignment.TopEnd).padding(2.dp)) { ArmorPip(unit.armor, 15.dp) }
        }

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            unit.statuses.keys.mapNotNull { statusGlyph(it) }.take(4).forEach { (glyph, tint) ->
                Text(glyph, style = CardName.copy(color = tint, fontSize = 8.sp))
            }
        }

        Text(
            text = unit.card.name,
            style = CardName.copy(fontSize = 7.sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 8.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(NamePlate)
                .padding(horizontal = 1.dp, vertical = 1.dp),
        )
    }
}

/**
 * A card in hand. Larger than a board card, and carries its provision cost — in hand that is
 * what tells you whether the card earned its slot in the deck.
 */
@Composable
fun HandCardView(
    card: Card,
    selected: Boolean = false,
    playable: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .size(HandCardWidth, HandCardHeight)
            // The selected card lifts slightly, as it does when you pick it up in the real game.
            .padding(bottom = if (selected) 0.dp else 4.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(cardArt(card.faction))
            .border(
                width = if (selected) 3.dp else 1.dp,
                brush = if (selected) MetalGold else frameBrush(card.color),
                shape = RoundedCornerShape(4.dp),
            )
            .then(if (onClick != null && playable) Modifier.clickable { onClick() } else Modifier),
    ) {
        CardSigil(card, Modifier.fillMaxSize())
        if (card.isUnit) PowerGem(card.basePower, card.basePower, 25.dp)

        // Provision cost, top-right, in a gold roundel.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(3.dp)
                .size(19.dp)
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF2A2210), Color(0xFF13100A))),
                    RoundedCornerShape(10.dp),
                )
                .border(1.dp, GoldDeep, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                card.provisions.toString(),
                style = PowerNumeral.copy(fontSize = 11.sp, color = GoldMuted),
            )
        }

        Text(
            text = card.name,
            style = CardName.copy(fontSize = 8.sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 9.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(NamePlate)
                .padding(horizontal = 2.dp, vertical = 2.dp),
        )
    }
}

/** A face-down stack, used for the deck and graveyard piles. */
@Composable
fun PileView(count: Int, label: String, width: Dp = 26.dp, height: Dp = 36.dp) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(width, height)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF2A2214), Color(0xFF120E08))))
                .border(1.dp, GoldDeep.copy(alpha = 0.7f), RoundedCornerShape(2.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(count.toString(), style = PowerNumeral.copy(fontSize = 12.sp, color = GoldMuted))
        }
        Text(label, style = CardName.copy(fontSize = 6.sp, color = MutedText))
    }
}
