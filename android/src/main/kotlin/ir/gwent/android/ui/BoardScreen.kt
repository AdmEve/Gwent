package ir.gwent.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.gwent.core.ai.Move
import ir.gwent.core.ai.SimpleAi
import ir.gwent.core.engine.GameEngine
import ir.gwent.core.engine.PlayTarget
import ir.gwent.core.model.Ability
import ir.gwent.core.model.Card as GwentCard
import ir.gwent.core.model.Faction
import ir.gwent.core.model.GameState
import ir.gwent.core.model.Row as GwentRow
import ir.gwent.core.model.Side

private val HUMAN = Side.A
private val AI = Side.B
private val ROW_ORDER_TOP_DOWN = listOf(GwentRow.SIEGE, GwentRow.RANGED, GwentRow.MELEE)

@Composable
fun RowSlot(
    row: GwentRow,
    cards: List<GwentCard>,
    state: GameState,
    totalPower: Int,
    selectableTargets: Set<String> = emptySet(),
    onCardTap: ((GwentCard) -> Unit)? = null,
) {
    val weathered = row in state.weatheredRows
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RowSlotBackground, RoundedCornerShape(8.dp))
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.width(56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(row.name.take(5), color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(totalPower.toString(), color = GoldText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            if (weathered) Chip("STORM", WeatherTint)
        }
        Spacer(modifier = Modifier.width(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(cards) { card ->
                val selectable = card.id in selectableTargets
                CardView(
                    card = card,
                    displayPower = GameEngine.effectivePower(state, card, row, cards),
                    selected = selectable,
                    dimmed = selectableTargets.isNotEmpty() && !selectable && onCardTap != null,
                    onClick = if (onCardTap != null && (selectableTargets.isEmpty() || selectable)) {
                        { onCardTap(card) }
                    } else null,
                )
            }
        }
    }
}

@Composable
fun BoardScreen(playerFaction: Faction, aiFaction: Faction, onExit: () -> Unit) {
    var state by remember { mutableStateOf(GameEngine.newMatch(playerFaction, aiFaction)) }
    var tick by remember { mutableStateOf(0) }
    var pendingCard by remember { mutableStateOf<GwentCard?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    fun runAiIfNeeded() {
        while (state.turn == AI && state.matchWinner == null) {
            when (val move = SimpleAi.chooseMove(state, AI)) {
                is Move.Pass -> GameEngine.pass(state, AI)
                is Move.PlayCard -> GameEngine.playCard(state, AI, move.cardId, move.target)
            }
        }
    }

    fun play(cardId: String, target: PlayTarget?) {
        GameEngine.playCard(state, HUMAN, cardId, target)
        runAiIfNeeded()
        tick++
    }

    fun onHandCardTap(card: GwentCard) {
        if (state.turn != HUMAN || state.matchWinner != null) return
        when (card.ability) {
            Ability.DECOY -> {
                val hasTarget = GwentRow.entries.any { r -> state.playerA.board.getValue(r).any { !it.isHero } }
                if (hasTarget) {
                    pendingCard = card
                } else {
                    message = "No eligible unit on your board to swap with Decoy."
                }
            }
            Ability.MEDIC -> {
                if (state.playerA.discard.isNotEmpty()) pendingCard = card else play(card.id, null)
            }
            else -> play(card.id, null)
        }
    }

    @Suppress("UNUSED_EXPRESSION")
    tick // read so this composable recomposes whenever it's bumped

    val decoyTargets: Set<String> = if (pendingCard?.ability == Ability.DECOY) {
        GwentRow.entries.flatMap { state.playerA.board.getValue(it) }.filter { !it.isHero }.map { it.id }.toSet()
    } else emptySet()

    Box(modifier = Modifier.fillMaxSize().background(BoardBackground)) {
    // Six row slots plus hand and controls overflow a phone screen, so the board scrolls.
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(10.dp)) {
        // Header: round + score.
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Round ${state.round.coerceAtMost(3)}", color = GoldText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SmallStat("YOU", "${state.playerA.roundsWon}")
                SmallStat("AI", "${state.playerB.roundsWon}")
            }
            TextButton(onClick = onExit) { Text("Exit") }
        }
        Spacer(modifier = Modifier.height(6.dp))

        // Opponent panel.
        Text("${factionLabel(aiFaction)} — hand ${state.playerB.hand.size} · deck ${state.playerB.deck.size} · discard ${state.playerB.discard.size}", color = MutedText, fontSize = 11.sp)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
            ROW_ORDER_TOP_DOWN.forEach { row ->
                RowSlot(row, state.playerB.board.getValue(row), state, GameEngine.rowPower(state, Side.B, row))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline))
        Spacer(modifier = Modifier.height(8.dp))

        // Player panel (mirrored: melee nearest the middle).
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ROW_ORDER_TOP_DOWN.reversed().forEach { row ->
                RowSlot(
                    row,
                    state.playerA.board.getValue(row),
                    state,
                    GameEngine.rowPower(state, Side.A, row),
                    selectableTargets = decoyTargets,
                    onCardTap = if (pendingCard?.ability == Ability.DECOY) {
                        { target -> play(pendingCard!!.id, PlayTarget.DecoyTarget(target.id)); pendingCard = null }
                    } else null,
                )
            }
        }
        Text("${factionLabel(playerFaction)} (you) — deck ${state.playerA.deck.size} · discard ${state.playerA.discard.size}", color = MutedText, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))

        message?.let {
            Text(it, color = Color(0xFFE87A5D), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Your hand", color = MutedText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
            items(state.playerA.hand) { card ->
                CardView(
                    card = card,
                    selected = pendingCard?.id == card.id,
                    onClick = if (state.turn == HUMAN && state.matchWinner == null && pendingCard == null) {
                        { onHandCardTap(card) }
                    } else null,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
            Button(
                onClick = { GameEngine.pass(state, HUMAN); runAiIfNeeded(); tick++ },
                enabled = state.turn == HUMAN && state.matchWinner == null,
            ) { Text("Pass") }
            OutlinedButton(onClick = {
                state = GameEngine.newMatch(playerFaction, aiFaction)
                pendingCard = null
                tick++
            }) { Text("New match") }
        }

        state.matchWinner?.let {
            Text(
                text = when (it) {
                    Side.A -> "You win!"
                    Side.B -> "AI wins."
                },
                color = GoldText,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    } // Column

    if (pendingCard?.ability == Ability.DECOY) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(PanelBackground)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Choose a unit on your board to swap back to hand", color = GoldText, fontSize = 12.sp)
            TextButton(onClick = { pendingCard = null }) { Text("Cancel") }
        }
    }
    } // Box

    if (pendingCard?.ability == Ability.MEDIC) {
        val medic = pendingCard!!
        AlertDialog(
            onDismissRequest = { pendingCard = null },
            title = { Text("Revive a card with ${medic.name}?") },
            text = {
                Column {
                    state.playerA.discard.forEach { c ->
                        TextButton(onClick = {
                            play(medic.id, PlayTarget.MedicRevive(c.id))
                            pendingCard = null
                        }) { Text("${c.name} (${c.basePower})") }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { play(medic.id, PlayTarget.MedicRevive(null)); pendingCard = null }) { Text("Skip revive") }
            },
        )
    }
}
