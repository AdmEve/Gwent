package ir.gwent.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.gwent.core.model.Faction

@Composable
fun FactionPickerScreen(onStart: (playerFaction: Faction, aiFaction: Faction) -> Unit) {
    var playerFaction by remember { mutableStateOf<Faction?>(null) }
    var aiFaction by remember { mutableStateOf<Faction?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(BoardBackground).padding(20.dp)) {
        Text("Gwent-verse", color = GoldText, fontWeight = FontWeight.Bold, fontSize = 26.sp)
        Text("Pick your deck, then the AI's opponent deck", color = MutedText, fontSize = 13.sp)

        Text("Your faction", color = GoldText, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
        FactionGrid(selected = playerFaction, onSelect = { playerFaction = it })

        Text("AI faction", color = GoldText, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
        FactionGrid(selected = aiFaction, onSelect = { aiFaction = it })

        Button(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            enabled = playerFaction != null && aiFaction != null,
            onClick = { onStart(playerFaction!!, aiFaction!!) },
        ) { Text("Start match") }
    }
}

/**
 * Plain rows rather than a LazyVerticalGrid: a lazy grid nested in a Column gets
 * unbounded height constraints and throws at measure time, and with five factions
 * there is nothing to virtualize anyway.
 */
@Composable
private fun FactionGrid(selected: Faction?, onSelect: (Faction) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Faction.entries.chunked(2).forEach { rowFactions ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowFactions.forEach { faction ->
                    val accent = factionAccent(faction)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected == faction) accent.copy(alpha = 0.25f) else PanelBackground)
                            .border(2.dp, if (selected == faction) accent else PanelBackground, RoundedCornerShape(10.dp))
                            .clickable { onSelect(faction) }
                            .padding(14.dp),
                    ) {
                        Text(factionLabel(faction), color = accent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
                if (rowFactions.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
