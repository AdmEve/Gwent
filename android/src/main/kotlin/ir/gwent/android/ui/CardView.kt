package ir.gwent.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    Ability.SPY -> Color(0xFF6B5FB0)
    Ability.MEDIC -> Color(0xFF3F9C6C)
    Ability.DECOY -> Color(0xFF3D8FE8)
    Ability.WEATHER -> WeatherTint
    Ability.CLEAR_WEATHER -> Color(0xFFE8D93D)
    Ability.NONE -> Color.Transparent
}

/**
 * A single card: portrait frame, faction accent border, name, power badge (or an ability
 * chip for weather/clear-weather cards, which have no power of their own), hero star.
 */
@Composable
fun CardView(
    card: GwentCard,
    displayPower: Int? = null,
    selected: Boolean = false,
    dimmed: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val accent = factionAccent(card.faction)
    val isEffect = card.ability == Ability.WEATHER || card.ability == Ability.CLEAR_WEATHER
    Card(
        modifier = modifier
            .size(width = 92.dp, height = 128.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = if (dimmed) CardBackground.copy(alpha = 0.45f) else CardBackground),
        border = BorderStroke(if (selected) 3.dp else if (card.isHero) 2.dp else 1.dp, if (selected) GoldText else if (card.isHero) HeroGold else accent),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(accent),
                )
                if (card.isHero) {
                    Text("★", color = HeroGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1.1f), contentAlignment = Alignment.Center) {
                Text(
                    text = card.name,
                    color = Color(0xFFE7E9EE),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                if (!isEffect) {
                    val power = displayPower ?: card.basePower
                    val weakened = displayPower != null && displayPower < card.basePower
                    Text(
                        text = power.toString(),
                        color = if (weakened) Color(0xFF6FA8DC) else GoldText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                } else {
                    Box(modifier = Modifier.size(1.dp))
                }
                abilityBadge(card.ability)?.let { label ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(abilityColor(card.ability))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    ) {
                        Text(label, color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SmallStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = GoldText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, color = MutedText, fontSize = 10.sp)
    }
}

val CardChipPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)

@Composable
fun Chip(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .padding(CardChipPadding),
    ) {
        Text(text, color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
