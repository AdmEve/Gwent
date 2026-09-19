package ir.gwent.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.gwent.core.model.*

/**
 * Deck building — where GWENT's real decision is made.
 *
 * The budget is finite, so every gold is paid for with weaker bronzes elsewhere. The screen is
 * built around that trade: the provision counter is the largest thing on it, the pool greys out
 * what you can no longer afford, and a card that is refused says why rather than just failing.
 */
@Composable
fun DeckBuilderScreen(
    leader: Leader,
    onPlay: (Deck) -> Unit,
    onBack: () -> Unit,
) {
    val builder = remember(leader) { DeckBuilder.fromStarter(leader) }
    var revision by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    @Suppress("UNUSED_EXPRESSION") revision

    val pool = remember(leader) {
        CardDatabase.ALL
            .filter { it.faction.playableIn(leader.faction) }
            .sortedWith(compareBy({ it.provisions }, { it.name }))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BoardGround)
            .padding(8.dp),
    ) {
        // ---- header: the budget, stated plainly ------------------------------
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(leader.name, style = SectionTitle.copy(fontSize = 16.sp, color = GoldLight))
                Text(
                    "${factionLabel(leader.faction)} · ${leader.text}",
                    style = BodyText.copy(fontSize = 9.sp, color = MutedText),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Budget("PROVISIONS", builder.provisionsSpent, builder.provisionLimit,
                over = builder.provisionsSpent > builder.provisionLimit)
            Spacer(Modifier.width(10.dp))
            Budget("CARDS", builder.size, MIN_DECK_SIZE, over = false, atLeast = true)
        }

        Spacer(Modifier.height(6.dp))

        Row(modifier = Modifier.weight(1f)) {
            // ---- the deck -----------------------------------------------------
            Column(modifier = Modifier.weight(1f)) {
                Text("YOUR DECK — tap to remove", style = SectionTitle.copy(fontSize = 9.sp))
                Spacer(Modifier.height(3.dp))
                val grouped = builder.cards.groupBy { it.id }.values.sortedWith(
                    compareByDescending<List<Card>> { it.first().provisions }.thenBy { it.first().name },
                )
                LazyColumn(
                    modifier = Modifier.weight(1f).testTag("deck-list"),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(grouped.size) { i ->
                        val group = grouped[i]
                        CardRow(
                            card = group.first(),
                            count = group.size,
                            enabled = true,
                            onClick = { builder.remove(group.first()); message = null; revision++ },
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // ---- the pool -----------------------------------------------------
            Column(modifier = Modifier.weight(1f)) {
                Text("AVAILABLE — tap to add", style = SectionTitle.copy(fontSize = 9.sp))
                Spacer(Modifier.height(3.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f).testTag("pool-list"),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(pool.size) { i ->
                        val card = pool[i]
                        CardRow(
                            card = card,
                            count = builder.copiesOf(card),
                            enabled = builder.canAdd(card),
                            onClick = {
                                message = builder.add(card)
                                revision++
                            },
                        )
                    }
                }
            }
        }

        // ---- footer: why you cannot start, and the button --------------------
        Spacer(Modifier.height(5.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = message ?: builder.problems().firstOrNull() ?: "Deck is legal.",
                style = BodyText.copy(
                    fontSize = 10.sp,
                    color = if (message != null || builder.problems().isNotEmpty()) DamageRed else BoostGreen,
                ),
                modifier = Modifier.weight(1f).testTag("builder-status"),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            FooterButton("BACK", true, onBack)
            Spacer(Modifier.width(6.dp))
            FooterButton("AUTOFILL", builder.size < MIN_DECK_SIZE) {
                builder.autoComplete(CardDatabase.ALL); message = null; revision++
            }
            Spacer(Modifier.width(6.dp))
            FooterButton("PLAY", builder.isComplete, tag = "builder-play") {
                onPlay(builder.build())
            }
        }
    }
}

/** A large budget readout. Provisions is the number the whole screen is about. */
@Composable
private fun Budget(label: String, value: Int, limit: Int, over: Boolean, atLeast: Boolean = false) {
    val ok = if (atLeast) value >= limit else value <= limit
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = CardName.copy(fontSize = 7.sp, color = MutedText))
        Text(
            "$value/$limit",
            style = ScoreNumeral.copy(
                fontSize = 20.sp,
                color = when {
                    over -> DamageRed
                    ok -> GoldLight
                    else -> Parchment
                },
            ),
            modifier = Modifier.testTag("budget-$label"),
        )
    }
}

/** One card in either list: provisions, power, name, and how many copies are in the deck. */
@Composable
private fun CardRow(card: Card, count: Int, enabled: Boolean, onClick: () -> Unit) {
    val palette = factionPalette(card.faction)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        palette.accent.copy(alpha = if (enabled) 0.22f else 0.06f),
                        Color.Transparent,
                    ),
                ),
            )
            .border(
                1.dp,
                if (card.color == CardColor.GOLD) GoldDeep.copy(alpha = if (enabled) 0.9f else 0.3f)
                else Color(0x33FFFFFF),
                RoundedCornerShape(2.dp),
            )
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Provision cost leads, because that is the currency being spent.
        Text(
            card.provisions.toString(),
            style = PowerNumeral.copy(
                fontSize = 12.sp,
                color = if (enabled) GoldMuted else GoldMuted.copy(alpha = 0.35f),
            ),
            modifier = Modifier.width(18.dp),
            textAlign = TextAlign.Center,
        )
        Text(
            card.name,
            style = CardName.copy(
                fontSize = 9.sp,
                color = if (enabled) Parchment else Parchment.copy(alpha = 0.35f),
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (card.isUnit) {
            Text(
                card.basePower.toString(),
                style = PowerNumeral.copy(
                    fontSize = 10.sp,
                    color = if (enabled) ArmorSteel else ArmorSteel.copy(alpha = 0.3f),
                ),
                modifier = Modifier.width(16.dp),
                textAlign = TextAlign.Center,
            )
        } else {
            Spacer(Modifier.width(16.dp))
        }
        Text(
            if (count > 0) "x$count" else " ",
            style = CardName.copy(fontSize = 8.sp, color = GoldBright),
            modifier = Modifier.width(18.dp),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun FooterButton(label: String, enabled: Boolean, tag: String = "btn-$label", onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(28.dp)
            .widthIn(min = 62.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (enabled) Color(0xFF2A2210) else Color(0xFF141109))
            .border(1.dp, if (enabled) GoldBright else Color(0xFF2A2519), RoundedCornerShape(3.dp))
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 10.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = SectionTitle.copy(fontSize = 10.sp, color = if (enabled) GoldLight else MutedText),
        )
    }
}
