package ir.gwent.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.gwent.core.model.Card as GwentCard
import ir.gwent.core.model.Faction
import ir.gwent.core.model.Leaders

/**
 * The pre-match redraw. Two swaps, as in Gwent — the hand has to carry all three rounds, so
 * dropping a dead card here matters.
 */
@Composable
fun MulliganScreen(
    hand: List<GwentCard>,
    swapsLeft: Int,
    playerFaction: Faction,
    aiFaction: Faction,
    onSwap: (GwentCard) -> Unit,
    onReady: () -> Unit,
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .tableSurface(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 18.dp),
        ) {
            Text("OPENING HAND", style = SectionTitle)
            Text(
                text = if (swapsLeft > 0) {
                    "Tap a card to send it back and draw another. $swapsLeft swap${if (swapsLeft == 1) "" else "s"} left."
                } else {
                    "No swaps left."
                },
                color = MutedText,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(hand, key = { it.id }) { card ->
                    CardView(
                        card = card,
                        onClick = if (swapsLeft > 0) {
                            { onSwap(card) }
                        } else null,
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            ArmyBrief(faction = playerFaction, label = "YOUR ARMY")
            Spacer(modifier = Modifier.height(10.dp))
            ArmyBrief(faction = aiFaction, label = "OPPOSING ARMY")

            Spacer(modifier = Modifier.height(20.dp))
            CarvedButton(
                text = "BEGIN THE MATCH",
                primary = true,
                onClick = onReady,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ArmyBrief(faction: Faction, label: String) {
    val palette = factionPalette(faction)
    val leader = Leaders.forFaction(faction)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(listOf(palette.deep, PanelBackground)),
                RoundedCornerShape(8.dp),
            )
            .border(1.dp, MetalSilver, RoundedCornerShape(8.dp))
            .padding(12.dp),
    ) {
        Text(label, color = MutedText, fontSize = 9.sp, letterSpacing = 1.5.sp)
        Text(
            factionLabel(faction),
            color = palette.accent,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
        Row(modifier = Modifier.padding(top = 6.dp)) {
            Chip("LEADER", palette.accent)
            Spacer(modifier = Modifier.padding(horizontal = 3.dp))
            Text(leader.name, color = GoldText, fontFamily = FontFamily.Serif, fontSize = 12.sp)
        }
        Text(leader.description, color = MutedText, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        Text(
            "Trait — ${Leaders.describeTrait(faction)}",
            color = MutedText,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
