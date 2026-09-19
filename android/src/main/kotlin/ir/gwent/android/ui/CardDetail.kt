package ir.gwent.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.gwent.core.model.*

/**
 * The card inspector.
 *
 * A board card carries no rules text, exactly as in the real game — which only works if the
 * player can ask what a card does. Without this the pool's 65 ability texts were unreadable and
 * informed play was impossible.
 *
 * It doubles as the only place a unit's Order can be fired, since an Order is a property of the
 * card rather than of the board.
 */
@Composable
fun CardDetailPanel(
    card: Card,
    unit: UnitInstance? = null,
    canUseOrder: Boolean = false,
    onUseOrder: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val palette = factionPalette(card.faction)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(300.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1C1810), Color(0xFF0B0A06))))
                .border(2.dp, frameBrush(card.color), RoundedCornerShape(5.dp))
                .padding(12.dp)
                .testTag("card-detail"),
        ) {
            // --- title line: name, power, provisions --------------------------
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(card.name, style = SectionTitle.copy(fontSize = 15.sp, color = GoldLight))
                    Text(
                        buildString {
                            append(if (card.color == CardColor.GOLD) "Gold" else "Bronze")
                            append(" · ").append(factionLabel(card.faction))
                            append(" · ").append(
                                when (card.type) {
                                    CardType.UNIT -> "Unit"
                                    CardType.SPECIAL -> "Special"
                                    CardType.ARTIFACT -> "Artifact"
                                    CardType.STRATAGEM -> "Stratagem"
                                },
                            )
                        },
                        style = BodyText.copy(fontSize = 9.sp, color = MutedText),
                    )
                }
                if (card.isUnit) {
                    Stat(
                        label = "POWER",
                        value = (unit?.power ?: card.basePower).toString(),
                        tint = powerColor(unit?.power ?: card.basePower, unit?.basePower ?: card.basePower),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Stat("PROV", card.provisions.toString(), GoldMuted)
            }

            Spacer(Modifier.height(8.dp))
            Divider()
            Spacer(Modifier.height(8.dp))

            // --- what it actually does ----------------------------------------
            Text(
                text = card.text.ifBlank { "No ability." },
                style = BodyText.copy(fontSize = 12.sp, color = Parchment),
                modifier = Modifier
                    .heightIn(max = 110.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("card-text"),
            )

            if (card.tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    card.tags.joinToString(" · ") { it.name.lowercase().replace('_', ' ') },
                    style = BodyText.copy(fontSize = 9.sp, color = palette.glow.copy(alpha = 0.8f)),
                )
            }

            // --- live state, when this is a unit on the board ------------------
            unit?.let { u ->
                if (u.armor > 0 || u.statuses.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Divider()
                    Spacer(Modifier.height(6.dp))
                    if (u.armor > 0) {
                        Text("Armor ${u.armor}", style = BodyText.copy(fontSize = 10.sp, color = ArmorSteel))
                    }
                    u.statuses.forEach { (status, turns) ->
                        Text(
                            text = status.name.lowercase().replaceFirstChar { it.uppercase() } +
                                if (turns > 0) " ($turns)" else "",
                            style = BodyText.copy(fontSize = 10.sp, color = GoldMuted),
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row {
                if (canUseOrder && onUseOrder != null) {
                    DetailButton("USE ORDER", enabled = true, tag = "btn-use-order") { onUseOrder() }
                    Spacer(Modifier.width(8.dp))
                }
                DetailButton("CLOSE", enabled = true, tag = "btn-close-detail") { onDismiss() }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, tint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = CardName.copy(fontSize = 7.sp, color = MutedText))
        Text(value, style = ScoreNumeral.copy(fontSize = 18.sp, color = tint))
    }
}

@Composable
private fun Divider() {
    Box(
        Modifier.fillMaxWidth().height(1.dp).background(
            Brush.horizontalGradient(listOf(Color.Transparent, GoldDeep, Color.Transparent)),
        ),
    )
}

@Composable
private fun DetailButton(label: String, enabled: Boolean, tag: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(30.dp)
            .widthIn(min = 80.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (enabled) Color(0xFF2A2210) else Color(0xFF141109))
            .border(1.dp, if (enabled) GoldBright else Color(0xFF2A2519), RoundedCornerShape(3.dp))
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 12.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = SectionTitle.copy(fontSize = 10.sp, color = if (enabled) GoldLight else MutedText))
    }
}
