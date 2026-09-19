package ir.gwent.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
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
 * The battlefield, in landscape.
 *
 * Four rows stacked around a centre line — opponent ranged furthest away, yours nearest — with
 * the leaders and piles on the left and the score rail on the right. The activity is locked to
 * landscape in the manifest, because this simply does not fit a phone held upright.
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
    @Suppress("UNUSED_EXPRESSION") revision

    val me = state.playerA
    val them = state.playerB

    Box(modifier = Modifier.fillMaxSize().background(BoardGround)) {
        // Vignette over the ground, so the light falls off toward the edges.
        Box(Modifier.fillMaxSize().background(BoardVignette))

        Row(modifier = Modifier.fillMaxSize()) {
            // ---- left rail: leaders and piles --------------------------------
            Column(
                modifier = Modifier.width(72.dp).fillMaxHeight().padding(horizontal = 4.dp, vertical = 3.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LeaderPanel(them, engine, enabled = false)
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    PileView(them.deck.size, "DECK")
                    PileView(them.graveyard.size, "GRAVE")
                }
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    PileView(me.deck.size, "DECK")
                    PileView(me.graveyard.size, "GRAVE")
                }
                LeaderPanel(me, engine, enabled = state.turn == Side.A && !state.matchOver)
            }

            // ---- the rows ----------------------------------------------------
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().padding(vertical = 3.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                BoardRow(them, Row.RANGED, engine, selectedHand, selectedTarget,
                    onTarget = { selectedTarget = it }, mine = false, tag = "opp-ranged")
                BoardRow(them, Row.MELEE, engine, selectedHand, selectedTarget,
                    onTarget = { selectedTarget = it }, mine = false, tag = "opp-melee")

                CentreLine(state)

                BoardRow(me, Row.MELEE, engine, selectedHand, selectedTarget,
                    onTarget = { selectedTarget = it }, mine = true, tag = "my-melee",
                    onPlay = { r ->
                        playSelected(engine, selectedHand, r, selectedTarget)
                            .also { if (it) { selectedHand = null; selectedTarget = null } }
                    })
                BoardRow(me, Row.RANGED, engine, selectedHand, selectedTarget,
                    onTarget = { selectedTarget = it }, mine = true, tag = "my-ranged",
                    onPlay = { r ->
                        playSelected(engine, selectedHand, r, selectedTarget)
                            .also { if (it) { selectedHand = null; selectedTarget = null } }
                    })

                // ---- hand ----------------------------------------------------
                LazyRow(
                    modifier = Modifier.fillMaxWidth().height(HandCardHeight).testTag("hand"),
                    horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    verticalAlignment = Alignment.Bottom,
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

            ScoreRail(engine, onExit = onExit)
        }
    }
}

private fun playSelected(engine: GameEngine, hand: Int?, row: Row, target: Int?): Boolean {
    val index = hand ?: return false
    return engine.perform(Side.A, Action.PlayCard(index, row, target = target)) == null
}

/** One band of terrain, with its score shield at the left edge. */
@Composable
private fun ColumnScope.BoardRow(
    player: PlayerState,
    row: Row,
    engine: GameEngine,
    selectedHand: Int?,
    selectedTarget: Int?,
    onTarget: (Int?) -> Unit,
    mine: Boolean,
    tag: String,
    onPlay: ((Row) -> Boolean)? = null,
) {
    val units = player.rows.getValue(row)
    val effect = engine.state.effectOn(player.side, row)
    val canDrop = onPlay != null && selectedHand != null && player.rowHasSpace(row)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .clip(RoundedCornerShape(2.dp))
            .background(if (canDrop) RowBandLit else rowBand(mine))
            .border(
                1.dp,
                if (canDrop) GoldBright.copy(alpha = 0.8f) else Color(0x22FFFFFF),
                RoundedCornerShape(2.dp),
            )
            .then(if (canDrop) Modifier.clickable { onPlay!!(row) } else Modifier)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowScoreShield(player.scoreOf(row), row, effect)
        Row(
            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
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
        Spacer(Modifier.width(30.dp))
    }
}

/** The per-row score, plus a marker when weather or a hazard sits on the row. */
@Composable
private fun RowScoreShield(score: Int, row: Row, effect: RowEffectKind?) {
    Column(
        modifier = Modifier.width(30.dp).fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (row == Row.MELEE) "⚔" else "➹",
            style = CardName.copy(color = GoldDeep, fontSize = 9.sp),
        )
        Text(score.toString(), style = ScoreNumeral.copy(fontSize = 16.sp, color = GoldMuted))
        if (effect != null) {
            // A glyph rather than a clipped word: "FROS" told the player nothing.
            Text(
                when (effect) {
                    RowEffectKind.FOG -> "\u2591"
                    RowEffectKind.FROST -> "\u2744"
                    RowEffectKind.RAIN -> "\u2614"
                    RowEffectKind.STORM -> "\u26A1"
                },
                style = CardName.copy(color = Color(0xFF79C4E0), fontSize = 11.sp),
            )
        }
    }
}

/** The centre line dividing the two sides, carrying the round number. */
@Composable
private fun CentreLine(state: GameState) {
    Box(modifier = Modifier.fillMaxWidth().height(15.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxWidth().height(1.dp).background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, GoldDeep, GoldBright, GoldDeep, Color.Transparent),
                ),
            ),
        )
        Text(
            text = if (state.matchOver) "MATCH OVER" else "ROUND ${state.round}",
            style = SectionTitle.copy(fontSize = 9.sp, letterSpacing = 3.sp),
            modifier = Modifier
                .background(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFF1B160A), Color.Transparent)))
                .padding(horizontal = 14.dp),
        )
    }
}

/** Leader portrait, its ability, and the crowns won so far. */
@Composable
private fun LeaderPanel(player: PlayerState, engine: GameEngine, enabled: Boolean) {
    val palette = factionPalette(player.faction)
    val usable = enabled && !player.leaderUsed
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(46.dp, 58.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(palette.glow.copy(alpha = 0.55f), palette.accent.copy(alpha = 0.5f), palette.deep),
                    ),
                )
                .border(if (usable) 2.dp else 1.dp, if (usable) GoldBright else GoldDeep, RoundedCornerShape(3.dp))
                .then(if (usable) Modifier.clickable { engine.perform(player.side, Action.UseLeader()) } else Modifier)
                .testTag("leader-${player.side}"),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                player.leader.name.take(1),
                style = DisplayTitle.copy(fontSize = 24.sp, color = if (usable) GoldLight else MutedText),
            )
        }
        Text(
            factionLabel(player.faction),
            style = CardName.copy(fontSize = 6.sp, color = MutedText),
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(CROWNS_TO_WIN) { i ->
                Box(
                    Modifier.size(8.dp).clip(CircleShape)
                        .background(if (i < player.crowns) GoldBright else Color(0x33FFFFFF))
                        .border(1.dp, GoldDeep.copy(alpha = 0.8f), CircleShape),
                )
            }
        }
    }
}

/**
 * The score rail. The totals sit large in the middle with the round coin between them, which is
 * the reading order the real game uses: their score above, yours below, the pass control between.
 */
@Composable
private fun ScoreRail(engine: GameEngine, onExit: () -> Unit) {
    val state = engine.state
    val me = state.playerA
    val them = state.playerB
    val myTurn = state.turn == Side.A && !state.matchOver
    val leading = me.score() > them.score()

    Column(
        modifier = Modifier.width(82.dp).fillMaxHeight().padding(horizontal = 5.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        TotalScore(them.score(), highlight = !leading && them.score() > 0, tag = "score-opponent")

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = when {
                    state.matchOver -> when (state.matchWinner) {
                        Side.A -> "VICTORY"; Side.B -> "DEFEAT"; null -> "DRAW"
                    }
                    myTurn -> "YOUR MOVE"
                    else -> "OPPONENT"
                },
                style = CardName.copy(
                    fontSize = 8.sp,
                    color = if (myTurn || state.matchOver) GoldBright else MutedText,
                ),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(5.dp))
            // The coin: passing in GWENT is done on the round coin, so it gets the round shape.
            PassCoin(enabled = myTurn && !me.passed) { engine.perform(Side.A, Action.Pass) }
            Spacer(Modifier.height(5.dp))
            RailButton("END TURN", myTurn && engine.actedThisTurn) {
                engine.perform(Side.A, Action.EndTurn)
            }
            RailButton("EXIT", true, onExit)
        }

        TotalScore(me.score(), highlight = leading, tag = "score-me")
    }
}

@Composable
private fun TotalScore(score: Int, highlight: Boolean, tag: String) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(
                        if (highlight) Color(0xFF3A2E10) else Color(0xFF1A1710),
                        Color(0xFF0B0A06),
                    ),
                ),
            )
            .border(2.dp, if (highlight) MetalGold else Brush.linearGradient(listOf(GoldDeep, GoldDeep)), CircleShape)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            score.toString(),
            style = ScoreNumeral.copy(fontSize = 21.sp, color = if (highlight) GoldLight else Parchment),
        )
    }
}

@Composable
private fun PassCoin(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(
                        if (enabled) Color(0xFF4A3A16) else Color(0xFF1A1710),
                        if (enabled) Color(0xFF241C0C) else Color(0xFF100E09),
                    ),
                ),
            )
            .border(2.dp, if (enabled) GoldBright else Color(0xFF2A2519), CircleShape)
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .testTag("btn-PASS"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "PASS",
            style = SectionTitle.copy(fontSize = 11.sp, color = if (enabled) GoldLight else MutedText),
        )
    }
}

@Composable
private fun RailButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(if (enabled) Color(0xFF2A2210) else Color(0xFF141109))
            .border(1.dp, if (enabled) GoldDeep else Color(0xFF2A2519), RoundedCornerShape(2.dp))
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .testTag("btn-$label"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = SectionTitle.copy(fontSize = 8.sp, color = if (enabled) GoldText else MutedText),
        )
    }
}
