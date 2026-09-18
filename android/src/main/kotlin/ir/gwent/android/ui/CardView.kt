package ir.gwent.android.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.gwent.core.model.Ability
import ir.gwent.core.model.Card as GwentCard

fun abilityBadge(ability: Ability): String? = when (ability) {
    Ability.HORN -> "HORN"
    Ability.SCORCH -> "SCORCH"
    Ability.SPY -> "SPY"
    Ability.MEDIC -> "MEDIC"
    Ability.DECOY -> "DECOY"
    Ability.WEATHER -> "WEATHER"
    Ability.CLEAR_WEATHER -> "CLEAR"
    Ability.NONE -> null
}

private fun abilityColor(ability: Ability): Color = when (ability) {
    Ability.HORN -> Color(0xFFE8A33D)
    Ability.SCORCH -> Color(0xFFCC4B37)
    Ability.SPY -> Color(0xFF7A6BC4)
    Ability.MEDIC -> Color(0xFF3F9C6C)
    Ability.DECOY -> Color(0xFF3D8FE8)
    Ability.WEATHER -> WeatherTint
    Ability.CLEAR_WEATHER -> Color(0xFFE8D93D)
    Ability.NONE -> Color.Transparent
}

/**
 * A card in layers, back to front: drop shadow, painted face (a real illustration if one has
 * been added, otherwise the generated heraldry), a darkened nameplate, the struck-metal frame
 * with corner ornaments, the power gem, and a frost wash when the row is under weather.
 */
@Composable
fun CardView(
    card: GwentCard,
    displayPower: Int? = null,
    selected: Boolean = false,
    dimmed: Boolean = false,
    weathered: Boolean = false,
    width: Dp = 92.dp,
    height: Dp = 132.dp,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val palette = factionPalette(card.faction)
    val shape = RoundedCornerShape((width.value * 0.09f).dp)
    val isEffect = card.ability == Ability.WEATHER || card.ability == Ability.CLEAR_WEATHER
    val compact = width < 72.dp

    val frameBrush = when {
        selected -> Brush.linearGradient(listOf(GoldLight, Color(0xFFFFF6DA), GoldLight))
        card.isHero -> heroFrameBrush()
        else -> MetalSilver
    }

    val artRes = cardArtRes(card)
    val seed = remember(card.id) { card.id.hashCode() }

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .shadow(
                elevation = if (selected) 18.dp else 8.dp,
                shape = shape,
                ambientColor = if (card.isHero) HeroGold else Color.Black,
                spotColor = if (card.isHero) HeroGold else Color.Black,
            )
            .clip(shape)
            .then(if (dimmed) Modifier.alpha(0.45f) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        // Painted face.
        if (artRes != 0) {
            Image(
                painter = painterResource(id = artRes),
                contentDescription = card.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCardArt(
                    seed = seed,
                    accent = palette.accent,
                    deep = palette.deep,
                    isHero = card.isHero,
                    ability = card.ability,
                    row = card.row,
                )
            }
        }

        if (weathered) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(FrostTint.copy(alpha = 0.34f), Color(0x662E5C80)))),
            )
        }

        // Nameplate.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE0070A0F), Color(0xFF05070A))))
                .padding(horizontal = 4.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            abilityBadge(card.ability)?.takeIf { !compact }?.let { label ->
                Box(
                    modifier = Modifier
                        .padding(bottom = 2.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(abilityColor(card.ability).copy(alpha = 0.92f))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                ) {
                    Text(label, color = Color(0xFF0B0D11), fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp)
                }
            }
            Text(
                text = card.name,
                style = CardName,
                fontSize = if (compact) 8.sp else 10.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Power gem, or a weather sigil for effect cards which have no power of their own.
        val gemSize = if (compact) 21.dp else 27.dp
        if (!isEffect) {
            val power = displayPower ?: card.basePower
            val weakened = displayPower != null && displayPower < card.basePower
            val boosted = displayPower != null && displayPower > card.basePower
            val gemFill = when {
                card.isHero -> Brush.radialGradient(listOf(Color(0xFFFFE9AE), Color(0xFFB98A24)))
                weakened -> Brush.radialGradient(listOf(Color(0xFFD6ECFF), Color(0xFF3F6E96)))
                boosted -> Brush.radialGradient(listOf(Color(0xFFFFF0C4), Color(0xFFC9922C)))
                else -> Brush.radialGradient(listOf(Color(0xFFE8EDF5), Color(0xFF49535F)))
            }
            Box(
                modifier = Modifier
                    .padding(3.dp)
                    .size(gemSize)
                    .clip(CircleShape)
                    .background(gemFill)
                    .border(1.5.dp, if (card.isHero) MetalGold else MetalSilver, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = power.toString(),
                    color = Color(0xFF14181F),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 12.sp else 15.sp,
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .padding(3.dp)
                    .size(gemSize)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(FrostTint, WeatherTint)))
                    .border(1.5.dp, MetalSilver, CircleShape),
            )
        }

        if (card.isHero && !compact) {
            Text(
                text = "★",
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                color = GoldLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        // Frame last, so it reads as the card's edge.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(if (selected || card.isHero) 2.dp else 1.dp, frameBrush, shape)
                .ornateCorners(
                    tint = if (card.isHero || selected) GoldLight.copy(alpha = 0.85f) else Color(0xFFBFC9D6).copy(alpha = 0.55f),
                ),
        )
    }
}

/**
 * Looks for this card's illustration by convention: a card with id `mar-iron-man` uses the
 * drawable `card_mar_iron_man`. Drop artwork into `android/src/main/res/drawable/` under that
 * name and it replaces the generated heraldry with no code change.
 */
@Composable
private fun cardArtRes(card: GwentCard): Int {
    val context = LocalContext.current
    return remember(card.id) {
        @Suppress("DiscouragedApi") // by-name lookup is the point: art is added without code changes
        context.resources.getIdentifier(
            "card_" + card.id.replace('-', '_'),
            "drawable",
            context.packageName,
        )
    }
}

@Composable
private fun heroFrameBrush(): Brush {
    val shimmer = rememberInfiniteTransition(label = "hero-frame")
    val sweep by shimmer.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 3200, easing = LinearEasing)),
        label = "sweep",
    )
    return Brush.linearGradient(
        colors = listOf(GoldDeep, GoldLight, Color(0xFFB08A38), GoldLight, GoldDeep),
        start = Offset(sweep * 520f - 260f, 0f),
        end = Offset(sweep * 520f + 140f, 520f),
    )
}

/** Card back, used for the opponent's hand and the deck pile. */
@Composable
fun CardBack(width: Dp = 46.dp, height: Dp = 66.dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape((width.value * 0.1f).dp)
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .shadow(4.dp, shape)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF2C2415), Color(0xFF120F09))))
            .border(1.dp, MetalBronze, shape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                brush = Brush.radialGradient(listOf(GoldDeep.copy(alpha = 0.85f), Color.Transparent), center = c, radius = size.minDimension * 0.5f),
                radius = size.minDimension * 0.5f,
                center = c,
            )
            drawCircle(GoldDeep.copy(alpha = 0.9f), radius = size.minDimension * 0.26f, center = c, style = androidx.compose.ui.graphics.drawscope.Stroke(width = size.minDimension * 0.05f))
        }
    }
}

@Composable
fun SmallStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = GoldText, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(label, color = MutedText, fontSize = 9.sp, letterSpacing = 1.sp)
    }
}

@Composable
fun Chip(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(color.copy(alpha = 0.9f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(text, color = Color(0xFF0B0D11), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
    }
}

/** The round-score gem beside each army's name. */
@Composable
fun RoundPip(won: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(13.dp)
            .clip(CircleShape)
            .background(
                if (won) Brush.radialGradient(listOf(Color(0xFFFFE9AE), Color(0xFFC08F22)))
                else Brush.radialGradient(listOf(Color(0xFF2A3140), Color(0xFF161B24)))
            )
            .border(1.dp, if (won) MetalGold else MetalSilver, CircleShape),
    )
}

/** Header/section rule with a faint gold gradient; caller decides the width. */
@Composable
fun ThinRule(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(1.dp)
            .background(Brush.horizontalGradient(listOf(Color.Transparent, GoldDeep, Color.Transparent))),
    )
}
