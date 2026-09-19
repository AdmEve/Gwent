package ir.gwent.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.gwent.core.engine.Action
import ir.gwent.core.engine.GameEngine
import ir.gwent.core.model.*

/**
 * The battlefield.
 *
 * Laid out the way the real game lays it out: four rows stacked around a centre line, with the
 * opponent's ranged row furthest away and yours nearest, the score HUD as a vertical column on
 * the right, and each player's leader anchored at the left edge.
 */
@Composable
fun BoardScreen(
    engine: GameEngine,
    onExit: () -> Unit = {},
    revision: Int = 0,
) {
    val state = engine.state
    var selectedHand by remember { mutableStateOf<Int?>(null) }
    var selectedTarget by remember { mutableStateOf<Int?>(null) }
    // Reading `revision` here keeps the board recomposing as the mutable engine advances.
    @Suppress("UNUSED_EXPRESSION") revision

    val me = state.playerA
    val them = state.playerB

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BoardDeep, BoardMid, BoardDeep))),
    ) {
        // ---- left rail: leaders and deck counts -----------------------------
        Column(
            modifier = Modifier.width(78.dp).fillMaxHeight().padding(4.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LeaderPanel(them, engine, enabled = false)
            PilePanel(them)
            Spacer(Modifier.weight(1f))
            PilePanel(me)
            LeaderPanel(me, engine, enabled = state.turn == Side.A && !state.matchOver)
        }

        // ---- the rows -------------------------------------------------------
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            BoardRow(them, Row.RANGED, engine, selectedHand, onTarget = { selectedTarget = it },
                selectedTarget = selectedTarget, tag = "opp-ranged")
            BoardRow(them, Row.MELEE, engine, selectedHand, onTarget = { selectedTarget = it },
                selectedTarget = selectedTarget, tag = "opp-melee")

            CentreLine(state)

            BoardRow(me, Row.MELEE, engine, selectedHand, onTarget = { selectedTarget = it },
                selectedTarget = selectedTarget, tag = "my-melee",
                onPlay = { row -> playSelected(engine, selectedHand, row, selectedTarget)
                    .also { if (it) { selectedHand = null; selectedTarget = null } } })
            BoardRow(me, Row.RANGED, engine, selectedHand, onTarget = { selectedTarget = it },
                selectedTarget = selectedTarget, tag = "my-ranged",
                onPlay = { row -> playSelected(engine, selectedHand, row, selectedTarget)
                    .also { if (it) { selectedHand = null; selectedTarget = null } } })

            Spacer(Modifier.height(2.dp))

            // ---- hand -------------------------------------------------------
            LazyRow(
                modifier = Modifier.fillMaxWidth().height(HandCardHeight + 4.dp).testTag("hand"),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items(me.hand.size) { i ->
                    HandCardView(
                        card = me.hand[i],
                        selected = selectedHand == i,
                        playable = state.turn == Side.A && !state.matchOver,
                        onClick = { selectedHand = if (selectedHand == i) null else i },
                    )
                }
            }
        }

        // ---- right rail: score HUD and controls ------------------------------
        ScoreRail(engine, onExit = onExit)
    }
}

private fun playSelected(engine: GameEngine, hand: Int?, row: Row, target: Int?): Boolean {
    val index = hand ?: return false
    return engine.perform(Side.A, Action.PlayCard(index, row, target = target)) == null
}

/** One row of the battlefield, with its own score tab at the left edge. */
@Composable
private fun BoardRow(
    player: PlayerState,
    row: Row,
    engine: GameEngine,
    selectedHand: Int?,
    selectedTarget: Int?,
    onTarget: (Int?) -> Unit,
    tag: String,
    onPlay: ((Row) -> Boolean)? = null,
) {
    val units = player.rows.getValue(row)
    val effect = engine.state.effectOn(player.side, row)
    val canDrop = onPlay != null && selectedHand != null && player.rowHasSpace(row)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(CardHeight + 6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (canDrop) RowSlotLit else RowSlot)
            .then(if (canDrop) Modifier.border(1.dp, GoldDeep, RoundedCornerShape(3.dp)) else Modifier)
            .then(if (canDrop) Modifier.clickable { onPlay!!(row) } else Modifier)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowScoreTab(player.scoreOf(row), row, effect)
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 3.dp),
            // Units sit centred on the row, as they do in the real game.
            horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            units.forEach { unit ->
                BoardCardView(
                    unit = unit,
                    selected = selectedTarget == unit.uid,
                    onClick = { onTarget(if (selectedTarget == unit.uid) null else unit.uid) },
                )
            }
        }
    }
}

/** The per-row score shield, plus a marker when a row effect is active on it. */
@Composable
private fun RowScoreTab(score: Int, row: Row, effect: RowEffectKind?) {
    Column(
        modifier = Modifier.width(30.dp).fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (row == Row.MELEE) "M" else "R",
            style = CardName.copy(color = GoldDeep, fontSize = 8.sp),
        )
        Text(score.toString(), style = ScoreNumeral.copy(fontSize = 15.sp, color = GoldMuted))
        if (effect != null) {
            Text(
                text = effect.name.take(1),
                style = CardName.copy(color = Color(0xFF79C4E0), fontSize = 8.sp),
            )
        }
    }
}

/** Round banner across the centre line. */
@Composable
private fun CentreLine(state: GameState) {
    Box(
        modifier = Modifier.fillMaxWidth().height(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.fillMaxWidth().height(1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, GoldDeep, Color.Transparent))),
        )
        Text(
            text = if (state.matchOver) "MATCH OVER" else "ROUND ${state.round}",
            style = SectionTitle.copy(fontSize = 11.sp),
            modifier = Modifier.background(BoardDeep).padding(horizontal = 10.dp),
        )
    }
}

/** Leader portrait and its once-per-match ability. */
@Composable
private fun LeaderPanel(player: PlayerState, engine: GameEngine, enabled: Boolean) {
    val palette = factionPalette(player.faction)
    val usable = enabled && !player.leaderUsed
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp, 62.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Brush.verticalGradient(listOf(palette.accent.copy(alpha = 0.5f), palette.deep)))
                .border(if (usable) 2.dp else 1.dp, if (usable) GoldBright else GoldDeep, RoundedCornerShape(4.dp))
                .then(if (usable) Modifier.clickable { engine.perform(player.side, Action.UseLeader()) } else Modifier)
                .testTag("leader-${player.side}"),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = player.leader.name.take(1),
                style = DisplayTitle.copy(fontSize = 22.sp, color = if (usable) GoldLight else MutedText),
            )
        }
        Text(
            text = factionLabel(player.faction),
            style = CardName.copy(fontSize = 7.sp, color = MutedText),
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        // Crowns: two wins the match.
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(CROWNS_TO_WIN) { i ->
                Box(
                    Modifier.size(7.dp).clip(CircleShape)
                        .background(if (i < player.crowns) GoldBright else Color(0x33FFFFFF)),
                )
            }
        }
    }
}

/** Deck, graveyard and coin counts. */
@Composable
private fun PilePanel(player: PlayerState) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        Pile("D", player.deck.size)
        Pile("G", player.graveyard.size)
        if (player.faction == Faction.SYNDICATE) Pile("C", player.coins)
    }
}

@Composable
private fun Pile(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count.toString(), style = PowerNumeral.copy(fontSize = 11.sp, color = GoldMuted))
        Text(label, style = CardName.copy(fontSize = 7.sp, color = MutedText))
    }
}

/**
 * The vertical score column on the right: opponent's rows and total above, yours below, with the
 * pass control between them — the same reading order as the real game.
 */
@Composable
private fun ScoreRail(engine: GameEngine, onExit: () -> Unit) {
    val state = engine.state
    val me = state.playerA
    val them = state.playerB
    val myTurn = state.turn == Side.A && !state.matchOver

    Column(
        modifier = Modifier.width(74.dp).fillMaxHeight().padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Text(them.scoreOf(Row.RANGED).toString(), style = ScoreNumeral.copy(fontSize = 14.sp, color = MutedText))
        Text(them.scoreOf(Row.MELEE).toString(), style = ScoreNumeral.copy(fontSize = 14.sp, color = MutedText))
        Text(
            them.score().toString(),
            style = ScoreNumeral.copy(color = if (them.score() > me.score()) GoldBright else Parchment),
            modifier = Modifier.testTag("score-opponent"),
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (state.matchOver) {
                Text(
                    text = when (state.matchWinner) {
                        Side.A -> "VICTORY"; Side.B -> "DEFEAT"; null -> "DRAW"
                    },
                    style = SectionTitle.copy(fontSize = 12.sp),
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    text = if (myTurn) "YOUR MOVE" else "WAITING",
                    style = CardName.copy(fontSize = 8.sp, color = if (myTurn) GoldBright else MutedText),
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(4.dp))
            RailButton("PASS", enabled = myTurn && !me.passed) {
                engine.perform(Side.A, Action.Pass)
            }
            RailButton("END", enabled = myTurn && engine.actedThisTurn) {
                engine.perform(Side.A, Action.EndTurn)
            }
            RailButton("EXIT", enabled = true, onClick = onExit)
        }

        Text(
            me.score().toString(),
            style = ScoreNumeral.copy(color = if (me.score() > them.score()) GoldBright else Parchment),
            modifier = Modifier.testTag("score-me"),
        )
        Text(me.scoreOf(Row.MELEE).toString(), style = ScoreNumeral.copy(fontSize = 14.sp, color = MutedText))
        Text(me.scoreOf(Row.RANGED).toString(), style = ScoreNumeral.copy(fontSize = 14.sp, color = MutedText))
    }
}

@Composable
private fun RailButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (enabled) Color(0xFF241D0F) else Color(0xFF14120C))
            .border(1.dp, if (enabled) GoldDeep else Color(0xFF2A2519), RoundedCornerShape(3.dp))
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .testTag("btn-$label"),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = SectionTitle.copy(fontSize = 10.sp, color = if (enabled) GoldText else MutedText))
    }
}
