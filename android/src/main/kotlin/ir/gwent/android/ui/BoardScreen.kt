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
    // The card being inspected, and the unit whose Order is armed and awaiting a target.
    var inspectedCard by remember { mutableStateOf<Card?>(null) }
    var inspectedUnit by remember { mutableStateOf<UnitInstance?>(null) }
    var pendingOrder by remember { mutableStateOf<Int?>(null) }
    @Suppress("UNUSED_EXPRESSION") revision

    val me = state.playerA
    val them = state.playerB

    Box(modifier = Modifier.fillMaxSize().background(Ink)) {
        // The board is a place, not a backdrop. Soil, a worn cobbled path, moss, loose rock and
        // grass are drawn underneath everything else, so the cards are played on ground rather
        // than on a ruled black screen.
        BoardTerrain(Modifier.fillMaxSize())

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

            Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(vertical = 3.dp)) {
                // The playing field is a centred column, not the full width: nine cards at 48dp
                // is ~470dp, so a wider band only ever adds empty ground either side. The hand
                // below stays full width, because ten cards need more room than the field does.
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier.widthIn(max = 520.dp).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        // The far rows are narrower than the near ones, so they have to be
                        // centred for the four of them to read as one receding plane.
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        BoardRow(them, Row.RANGED, engine, selectedHand, selectedTarget,
                            onTarget = { uid -> onUnitTapped(engine, them, uid, pendingOrder,
                                fire = { o, t -> engine.perform(Side.A, Action.UseOrder(o, t)); pendingOrder = null },
                                inspect = { c, u -> inspectedCard = c; inspectedUnit = u },
                                select = { selectedTarget = it }) },
                            tag = "opp-ranged", depth = 0)
                        BoardRow(them, Row.MELEE, engine, selectedHand, selectedTarget,
                            onTarget = { uid -> onUnitTapped(engine, them, uid, pendingOrder,
                                fire = { o, t -> engine.perform(Side.A, Action.UseOrder(o, t)); pendingOrder = null },
                                inspect = { c, u -> inspectedCard = c; inspectedUnit = u },
                                select = { selectedTarget = it }) },
                            tag = "opp-melee", depth = 1)

                        CentreLine(state)

                        BoardRow(me, Row.MELEE, engine, selectedHand, selectedTarget,
                            onTarget = { uid -> onUnitTapped(engine, me, uid, pendingOrder,
                                fire = { o, t -> engine.perform(Side.A, Action.UseOrder(o, t)); pendingOrder = null },
                                inspect = { c, u -> inspectedCard = c; inspectedUnit = u },
                                select = { selectedTarget = it }) },
                            tag = "my-melee", depth = 2,
                            onPlay = { r ->
                                playSelected(engine, selectedHand, r, selectedTarget)
                                    .also { if (it) { selectedHand = null; selectedTarget = null } }
                            })
                        BoardRow(me, Row.RANGED, engine, selectedHand, selectedTarget,
                            onTarget = { uid -> onUnitTapped(engine, me, uid, pendingOrder,
                                fire = { o, t -> engine.perform(Side.A, Action.UseOrder(o, t)); pendingOrder = null },
                                inspect = { c, u -> inspectedCard = c; inspectedUnit = u },
                                select = { selectedTarget = it }) },
                            tag = "my-ranged", depth = 3,
                            onPlay = { r ->
                                playSelected(engine, selectedHand, r, selectedTarget)
                                    .also { if (it) { selectedHand = null; selectedTarget = null } }
                            })
                    }
                }

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
                            // First tap selects the card to play; tapping the selected card
                            // again opens the inspector, so its text is always one tap away.
                            onClick = {
                                if (selectedHand == i) {
                                    inspectedCard = me.hand[i]; inspectedUnit = null
                                } else {
                                    selectedHand = i
                                }
                            },
                        )
                    }
                }
            }

            ScoreRail(engine, onExit = onExit)
        }

        // A banner while an Order is armed, so the player knows a target is expected.
        if (pendingOrder != null) {
            Box(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 3.dp)
                    .background(Color(0xE62A2210), RoundedCornerShape(3.dp))
                    .border(1.dp, GoldBright, RoundedCornerShape(3.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text("CHOOSE A TARGET — tap again to cancel", style = SectionTitle.copy(fontSize = 9.sp))
            }
        }

        inspectedCard?.let { card ->
            val unit = inspectedUnit
            val order = unit?.card?.abilities?.firstOrNull { it.trigger == Trigger.ORDER }
            val mine = unit != null && me.units().any { it.uid == unit.uid }
            val ready = unit != null && mine && order != null &&
                unit.orderReady && unit.charges > 0 && unit.cooldownLeft == 0 &&
                state.turn == Side.A && !state.matchOver
            CardDetailPanel(
                card = card,
                unit = unit,
                canUseOrder = ready,
                onUseOrder = {
                    // Effects that need no target fire at once; the rest arm and wait for one.
                    if (order != null && needsTarget(order.effect)) {
                        pendingOrder = unit!!.uid
                    } else {
                        engine.perform(Side.A, Action.UseOrder(unit!!.uid))
                    }
                    inspectedCard = null; inspectedUnit = null
                },
                onDismiss = { inspectedCard = null; inspectedUnit = null },
            )
        }
    }
}

/**
 * A tap on a board unit means one of three things: fire an armed Order at it, pick it as the
 * target for the card in hand, or ask what it is.
 */
private fun onUnitTapped(
    engine: GameEngine,
    owner: PlayerState,
    uid: Int?,
    pendingOrder: Int?,
    fire: (Int, Int?) -> Unit,
    inspect: (Card, UnitInstance?) -> Unit,
    select: (Int?) -> Unit,
) {
    val unit = uid?.let { u -> owner.units().firstOrNull { it.uid == u } }
    when {
        uid == null -> select(null)
        pendingOrder != null -> fire(pendingOrder, uid)
        unit != null -> { select(uid); inspect(unit.card, unit) }
        else -> select(uid)
    }
}

/** Whether an effect needs something pointed at before it can resolve. */
private fun needsTarget(effect: Effect): Boolean = when (effect) {
    is Effect.Damage, Effect.Destroy, Effect.Banish, Effect.Move,
    is Effect.Apply, Effect.Purify, Effect.Reset, is Effect.Boost,
    is Effect.Strengthen, is Effect.Heal, Effect.Consume,
    -> true
    else -> false
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
    tag: String,
    /** 0 is the row furthest from the player, 3 the nearest. Drives the whole perspective. */
    depth: Int,
    onPlay: ((Row) -> Boolean)? = null,
) {
    val units = player.rows.getValue(row)
    val effect = engine.state.effectOn(player.side, row)
    val canDrop = onPlay != null && selectedHand != null && player.rowHasSpace(row)

    Row(
        modifier = Modifier
            // Width, height and card size all come from GWENT's own measurements: the nearest
            // row is a quarter taller and an eighth wider than the furthest, which is the
            // recession the flat board was missing.
            .fillMaxWidth(GwentBoard.ROW_WIDTH[depth])
            .weight(GwentBoard.ROW_WEIGHT[depth])
            // Deliberately no box. In GWENT a row is ground, and the ground runs unbroken from
            // one row into the next — the cards and the row marker say where a row is, not a
            // rule drawn round it. A row lights up only when the held card can be dropped on it.
            .then(
                if (canDrop) {
                    Modifier
                        .clip(RoundedCornerShape(2.dp))
                        .background(RowBandLit)
                        .border(1.dp, GoldBright.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                } else {
                    Modifier
                },
            )
            .then(if (canDrop) Modifier.clickable { onPlay!!(row) } else Modifier)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowMarker(row, effect)
        Row(
            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            units.forEach { unit ->
                BoardCardView(
                    unit = unit,
                    selected = selectedTarget == unit.uid,
                    scale = GwentBoard.ROW_SCALE[depth],
                    onClick = { onTarget(if (selectedTarget == unit.uid) null else unit.uid) },
                )
            }
        }
        Spacer(Modifier.width(22.dp))
    }
}

/**
 * The carved tile beside a row: which row it is, and any weather sitting on it.
 *
 * It carries no number. The reference keeps every score in the right-hand rail and leaves the
 * board itself clear, which is also why the rows here have no boxes drawn round them.
 */
@Composable
private fun RowMarker(row: Row, effect: RowEffectKind?) {
    Column(
        modifier = Modifier
            .width(22.dp)
            .padding(vertical = 4.dp, horizontal = 2.dp)
            .clip(RoundedCornerShape(2.dp))
            // A slab of stone set into the ground, the way the real board marks its rows —
            // lit along the top edge, in shadow at the foot, and cut into the earth by a
            // dark rim rather than floating on it.
            .background(
                Brush.verticalGradient(
                    listOf(Ground.stoneMid, Ground.stoneDark, Ground.soilBlack),
                ),
            )
            .border(1.dp, Ground.soilBlack.copy(alpha = 0.85f), RoundedCornerShape(2.dp))
            .padding(vertical = 3.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (row == Row.MELEE) "⚔" else "➹",
            style = CardName.copy(color = GoldMuted, fontSize = 11.sp),
        )
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
        // The rail reads top to bottom in the same order as the rows it describes, exactly as
        // the reference does: each side's two row scores bracket its total, and the coin sits
        // between the two sides.
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RowScore(them.scoreOf(Row.RANGED), "rail-opp-ranged")
            RowScore(them.scoreOf(Row.MELEE), "rail-opp-melee")
            Spacer(Modifier.height(2.dp))
            TotalScore(them.score(), highlight = !leading && them.score() > 0, tag = "score-opponent")
        }

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

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TotalScore(me.score(), highlight = leading, tag = "score-me")
            Spacer(Modifier.height(2.dp))
            RowScore(me.scoreOf(Row.MELEE), "rail-my-melee")
            RowScore(me.scoreOf(Row.RANGED), "rail-my-ranged")
        }
    }
}

/**
 * One row's score in the rail.
 *
 * Smaller than the total and without a frame, which is how the reference distinguishes the two:
 * the big numeral is what you are racing on, the small ones say where it came from.
 */
@Composable
private fun RowScore(score: Int, tag: String) {
    Text(
        text = score.toString(),
        style = ScoreNumeral.copy(fontSize = 15.sp, color = Parchment.copy(alpha = 0.92f)),
        modifier = Modifier.testTag(tag),
    )
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
