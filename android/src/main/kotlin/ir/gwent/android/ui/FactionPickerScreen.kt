package ir.gwent.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.gwent.core.model.CardDatabase
import ir.gwent.core.model.Leader
import ir.gwent.core.model.Leaders

/**
 * Leader selection. In GWENT the leader *is* the deck choice — it fixes the faction and sets the
 * provision budget — so the picker shows the provision trade-off rather than just a faction name.
 */
@Composable
fun FactionPickerScreen(onStart: (Leader, Leader) -> Unit) {
    var mine by remember { mutableStateOf<Leader?>(null) }
    var theirs by remember { mutableStateOf<Leader?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BoardDeep, BoardMid, BoardDeep)))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("GWENT", style = DisplayTitle)
        Text(
            "Two rows. Three rounds. One hand to spend across all of them.",
            style = BodyText.copy(color = MutedText),
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )

        Text("YOUR LEADER", style = SectionTitle)
        Spacer(Modifier.height(6.dp))
        Leaders.ALL.forEach { leader ->
            LeaderRow(leader, selected = mine == leader) { mine = leader }
        }

        Spacer(Modifier.height(16.dp))
        Text("OPPONENT", style = SectionTitle)
        Spacer(Modifier.height(6.dp))
        Leaders.ALL.forEach { leader ->
            LeaderRow(leader, selected = theirs == leader, tagPrefix = "opp") { theirs = leader }
        }

        Spacer(Modifier.height(18.dp))
        val ready = mine != null && theirs != null
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (ready) Color(0xFF2A2210) else Color(0xFF15130D))
                .border(1.dp, if (ready) GoldBright else Color(0xFF2A2519), RoundedCornerShape(4.dp))
                .then(if (ready) Modifier.clickable { onStart(mine!!, theirs!!) } else Modifier)
                .testTag("start-match"),
            contentAlignment = Alignment.Center,
        ) {
            Text("BEGIN", style = SectionTitle.copy(color = if (ready) GoldLight else MutedText))
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LeaderRow(
    leader: Leader,
    selected: Boolean,
    tagPrefix: String = "me",
    onClick: () -> Unit,
) {
    val palette = factionPalette(leader.faction)
    val deck = remember(leader) { CardDatabase.starterDeck(leader) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(palette.accent.copy(alpha = if (selected) 0.45f else 0.16f), Color.Transparent),
                ),
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) GoldBright else Color(0xFF2A2519),
                shape = RoundedCornerShape(4.dp),
            )
            .clickable { onClick() }
            .padding(10.dp)
            .testTag("$tagPrefix-${leader.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(leader.name, style = SectionTitle.copy(fontSize = 15.sp, color = GoldLight))
            Text(factionLabel(leader.faction), style = BodyText.copy(fontSize = 11.sp))
            Text(leader.text, style = BodyText.copy(fontSize = 10.sp, color = MutedText))
        }
        Column(horizontalAlignment = Alignment.End) {
            // The trade-off that defines deck-building: ability strength against provisions.
            Text("+${leader.provisionBonus}", style = ScoreNumeral.copy(fontSize = 16.sp, color = GoldMuted))
            Text("provisions", style = BodyText.copy(fontSize = 8.sp, color = MutedText))
            Text(
                "${deck.provisionsSpent}/${deck.provisionLimit}",
                style = BodyText.copy(fontSize = 9.sp, color = MutedText),
            )
        }
    }
}
