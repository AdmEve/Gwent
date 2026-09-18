package ir.gwent.android.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
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
 * A card rendered in layers, back to front: drop shadow, faction-tinted art panel with a
 * monogram and sheen, a nameplate, a struck-metal frame (gold and shimmering for heroes,
 * silver otherwise), the power gem, and a frost wash when the row is under weather.
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
    val shape = RoundedCornerShape(10.dp)
    val isEffect = card.ability == Ability.WEATHER || card.ability == Ability.CLEAR_WEATHER

    val shimmer = rememberInfiniteTransition(label = "frame")
    val sweep by shimmer.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 3200, easing = LinearEasing)),
        label = "sweep",
    )

    val frameBrush = when {
        selected -> Brush.linearGradient(listOf(GoldLight, Color(0xFFFFF6DA), GoldLight))
        card.isHero -> Brush.linearGradient(
            colors = listOf(GoldDeep, GoldLight, Color(0xFFB08A38), GoldLight, GoldDeep),
            start = Offset(sweep * 260f - 130f, 0f),
            end = Offset(sweep * 260f + 70f, 260f),
        )
        else -> MetalSilver
    }

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .shadow(
                elevation = if (selected) 16.dp else 7.dp,
                shape = shape,
                ambientColor = if (card.isHero) HeroGold else Color.Black,
                spotColor = if (card.isHero) HeroGold else Color.Black,
            )
            .clip(shape)
            .background(Brush.verticalGradient(listOf(CardBackgroundHi, CardBackground)))
            .then(if (dimmed) Modifier.alpha(0.42f) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        // Art panel: faction-colored glow behind an oversized monogram.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(palette.accent.copy(alpha = 0.42f), palette.deep.copy(alpha = 0.75f), Color.Transparent),
                        center = Offset(width.value * 1.4f, height.value * 0.9f),
                        radius = width.value * 2.6f,
                    )
                ),
        ) {
            Text(
                text = card.name.first().toString(),
                modifier = Modifier.align(Alignment.Center).alpha(0.30f),
                color = GoldLight,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = (height.value * 0.42f).sp,
            )
        }

        // Diagonal sheen across the glass.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.10f), Color.Transparent, Color.Transparent),
                        start = Offset(0f, 0f),
                        end = Offset(width.value * 2.2f, height.value * 2.2f),
                    )
                ),
        )

        if (weathered) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(listOf(FrostTint.copy(alpha = 0.30f), Color(0x552E5C80)))
                    ),
            )
        }

        // Nameplate.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6070A0F), Color(0xFF05070A))))
                .padding(horizontal = 5.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            abilityBadge(card.ability)?.let { label ->
                Box(
                    modifier = Modifier
                        .padding(bottom = 3.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(abilityColor(card.ability).copy(alpha = 0.92f))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                ) {
                    Text(label, color = Color(0xFF0B0D11), fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            }
            Text(
                text = card.name,
                style = CardName,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Power gem, or a weather sigil for effect cards that have no power of their own.
        if (!isEffect) {
            val power = displayPower ?: card.basePower
            val weakened = displayPower != null && displayPower < card.basePower
            val gemFill = when {
                card.isHero -> Brush.radialGradient(listOf(Color(0xFFFFE9AE), Color(0xFFB98A24)))
                weakened -> Brush.radialGradient(listOf(Color(0xFFD6ECFF), Color(0xFF3F6E96)))
                else -> Brush.radialGradient(listOf(Color(0xFFE8EDF5), Color(0xFF4C5766)))
            }
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(if (width > 80.dp) 27.dp else 23.dp)
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
                    fontSize = if (width > 80.dp) 15.sp else 13.sp,
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(23.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(FrostTint, WeatherTint)))
                    .border(1.5.dp, MetalSilver, CircleShape),
            )
        }

        if (card.isHero) {
            Text(
                text = "★",
                modifier = Modifier.align(Alignment.TopEnd).padding(5.dp),
                color = GoldLight,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        // Frame on top of everything so it reads as the card's edge.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(if (selected || card.isHero) 2.dp else 1.dp, frameBrush, shape),
        )
    }
}

/** Card back, for the opponent's hand. */
@Composable
fun CardBack(width: Dp = 46.dp, height: Dp = 66.dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .shadow(4.dp, shape)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF2A2214), Color(0xFF130F09))))
            .border(1.dp, MetalBronze, shape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width * 0.42f)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(GoldDeep.copy(alpha = 0.9f), Color.Transparent))),
        )
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

/** The big round-score gem next to each army's name. */
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
