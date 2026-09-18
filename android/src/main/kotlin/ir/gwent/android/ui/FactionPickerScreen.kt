package ir.gwent.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.gwent.core.model.CardDatabase
import ir.gwent.core.model.Faction

@Composable
fun FactionPickerScreen(onStart: (playerFaction: Faction, aiFaction: Faction) -> Unit) {
    var playerFaction by remember { mutableStateOf<Faction?>(null) }
    var aiFaction by remember { mutableStateOf<Faction?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Ink, BoardMid, Ink)))
            .vignette(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 22.dp),
        ) {
            Text("GWENT-VERSE", style = DisplayTitle)
            OrnateDivider(modifier = Modifier.padding(top = 4.dp, bottom = 2.dp))
            Text(
                "Three rows. Three rounds. Spend your hand wisely.",
                color = MutedText,
                fontSize = 12.sp,
                fontFamily = FontFamily.Serif,
            )

            Text("YOUR ARMY", style = SectionTitle, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp))
            Faction.entries.forEach { faction ->
                FactionBanner(
                    faction = faction,
                    selected = playerFaction == faction,
                    onClick = { playerFaction = faction },
                )
            }

            Text("OPPOSING ARMY", style = SectionTitle, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp))
            Faction.entries.forEach { faction ->
                FactionBanner(
                    faction = faction,
                    selected = aiFaction == faction,
                    onClick = { aiFaction = faction },
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = playerFaction != null && aiFaction != null,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2E3647),
                    contentColor = GoldLight,
                    disabledContainerColor = Color(0xFF181D26),
                    disabledContentColor = Color(0xFF55606F),
                ),
                onClick = { onStart(playerFaction!!, aiFaction!!) },
            ) {
                Text(
                    "TO BATTLE",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 3.sp,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FactionBanner(faction: Faction, selected: Boolean, onClick: () -> Unit) {
    val palette = factionPalette(faction)
    val shape = RoundedCornerShape(8.dp)
    val heroes = remember(faction) { CardDatabase.deckFor(faction).count { it.isHero } }
    val cards = remember(faction) { CardDatabase.deckFor(faction).size }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .shadow(if (selected) 10.dp else 2.dp, shape, ambientColor = palette.accent, spotColor = palette.accent)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(palette.deep, PanelBackground, Color(0xFF12161E))
                )
            )
            .border(if (selected) 2.dp else 1.dp, if (selected) MetalGold else MetalSilver, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            // Faction sigil: glowing disc with the initial struck into it.
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(palette.accent, palette.deep)))
                    .border(1.5.dp, if (selected) MetalGold else MetalSilver, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = factionLabel(faction).first().toString(),
                    color = Color(0xFF0D1015),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = factionLabel(faction),
                    color = if (selected) GoldLight else Color(0xFFD7DEE9),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    letterSpacing = 1.sp,
                )
                Text(factionMotto(faction), color = MutedText, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$cards cards", color = MutedText, fontSize = 10.sp)
                Text("$heroes heroes", color = palette.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
