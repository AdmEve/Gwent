package ir.gwent.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.gwent.core.ai.Move
import ir.gwent.core.ai.SimpleAi
import ir.gwent.core.engine.GameEngine
import ir.gwent.core.engine.GameEvent
import ir.gwent.core.engine.PlayTarget
import ir.gwent.core.model.Ability
import ir.gwent.core.model.Card as GwentCard
import ir.gwent.core.model.Faction
import ir.gwent.core.model.GameState
import ir.gwent.core.model.Row as GwentRow
import ir.gwent.core.model.STARTING_LIVES
import ir.gwent.core.model.Side
import kotlinx.coroutines.delay

private val HUMAN = Side.A
private val AI = Side.B
private val ROW_ORDER_TOP_DOWN = listOf(GwentRow.SIEGE, GwentRow.RANGED, GwentRow.MELEE)

@Composable
private fun ArmyTotal(total: Int, leading: Boolean, modifier: Modifier = Modifier) {
    val shown by animateIntAsState(targetValue = total, animationSpec = tween(420), label = "total")
    Box(
        modifier = modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(
                if (leading) Brush.radialGradient(listOf(Color(0xFFFFE9AE), Color(0xFFB4841E)))
                else Brush.radialGradient(listOf(Color(0xFF39424F), Color(0xFF191E27)))
            )
            .border(2.dp, if (leading) MetalGold else MetalSilver, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = shown.toString(),
            color = if (leading) Color(0xFF14181F) else GoldText,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
        )
    }
}

/** Deck and graveyard counts, shown as small stacked piles like the real board's side columns. */
@Composable
private fun Piles(deck: Int, graveyard: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PileCount("DECK", deck)
        PileCount("GRAVE", graveyard)
    }
}

@Composable
private fun PileCount(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(width = 22.dp, height = 30.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF2A2214), Color(0xFF131009))))
                .border(1.dp, MetalBronze, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(count.toString(), color = GoldText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Text(label, color = MutedText, fontSize = 7.sp, letterSpacing = 0.5.sp)
    }
}

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
    val targeting = selectableTargets.isNotEmpty() && onCardTap != null
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        if (targeting) RowSlotLit else RowSlotBackground,
                        if (weathered) Color(0xFF16222E) else RowSlotBackground,
                    )
                )
            )
            .border(1.dp, if (targeting) GoldDeep else Color(0xFF232B38), RoundedCornerShape(8.dp)),
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.width(44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                RowGlyph(
                    row = row,
                    tint = if (weathered) FrostTint else Color(0xFF6E7A8C),
                    modifier = Modifier.size(19.dp),
                )
                Text(
                    text = totalPower.toString(),
                    color = GoldText,
                    fontFamily = FontFamily.Serif,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(cards) { card ->
                    val selectable = card.id in selectableTargets
                    CardView(
                        card = card,
                        displayPower = GameEngine.effectivePower(state, card, row, cards),
                        selected = selectable,
                        dimmed = targeting && !selectable,
                        weathered = weathered && !card.isHero,
                        width = 60.dp,
                        height = 84.dp,
                        onClick = if (onCardTap != null && (selectableTargets.isEmpty() || selectable)) {
                            { onCardTap(card) }
                        } else null,
                    )
                }
            }
        }

        if (weathered) {
            WeatherOverlay(row = row, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
fun BoardScreen(playerFaction: Faction, aiFaction: Faction, onExit: () -> Unit) {
    var state by remember { mutableStateOf(GameEngine.newMatch(playerFaction, aiFaction)) }
    var tick by remember { mutableStateOf(0) }
    var pendingCard by remember { mutableStateOf<GwentCard?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var mulliganDone by remember { mutableStateOf(false) }

    /**
     * Lets the AI take its consecutive turns. Bails out on any rejected move and caps the
     * iterations: this runs on the main thread, so a logic bug must never become a freeze.
     */
    fun runAiIfNeeded() {
        var guard = 0
        while (state.turn == AI && !state.matchOver && guard < 200) {
            val events = when (val move = SimpleAi.chooseMove(state, AI)) {
                is Move.Pass -> GameEngine.pass(state, AI)
                is Move.UseLeader -> GameEngine.useLeader(state, AI)
                is Move.PlayCard -> GameEngine.playCard(state, AI, move.cardId, move.target)
            }
            if (events.any { it is GameEvent.InvalidMove }) break
            guard++
        }
    }

    fun play(cardId: String, target: PlayTarget?) {
        message = null
        val events = GameEngine.playCard(state, HUMAN, cardId, target)
        events.filterIsInstance<GameEvent.InvalidMove>().firstOrNull()?.let { message = it.reason }
        runAiIfNeeded()
        tick++
    }

    fun onHandCardTap(card: GwentCard) {
        if (state.turn != HUMAN || state.matchOver) return
        when (card.ability) {
            Ability.DECOY -> {
                val hasTarget = GwentRow.entries.any { r -> state.playerA.board.getValue(r).any { !it.isHero } }
                if (hasTarget) pendingCard = card else message = "No unit on your board to swap with Decoy."
            }
            Ability.MEDIC -> {
                if (state.playerA.discard.isNotEmpty()) pendingCard = card else play(card.id, null)
            }
            else -> play(card.id, null)
        }
    }

    @Suppress("UNUSED_EXPRESSION")
    tick // read so this composable recomposes whenever it's bumped

    // The coin toss can hand the opening turn to the AI, but not before both sides have
    // finished swapping cards.
    LaunchedEffect(state, mulliganDone) {
        if (mulliganDone) {
            runAiIfNeeded()
            tick++
        }
    }

    if (!mulliganDone) {
        MulliganScreen(
            state = state,
            playerFaction = playerFaction,
            aiFaction = aiFaction,
            onSwap = { card ->
                GameEngine.mulligan(state, HUMAN, card.id)
                tick++
            },
            onReady = {
                // The opponent ditches its two weakest cards before the match begins.
                repeat(state.playerB.mulligansLeft) {
                    val worst = state.playerB.hand.minByOrNull { it.basePower }
                    if (worst != null) GameEngine.mulligan(state, AI, worst.id)
                }
                mulliganDone = true
                tick++
            },
        )
        return
    }

    val decoyTargets: Set<String> = if (pendingCard?.ability == Ability.DECOY) {
        GwentRow.entries.flatMap { state.playerA.board.getValue(it) }.filter { !it.isHero }.map { it.id }.toSet()
    } else emptySet()

    val playerTotal = GameEngine.totalPower(state, Side.A)
    val aiTotal = GameEngine.totalPower(state, Side.B)
    val yourTurn = state.turn == HUMAN && !state.matchOver

    var showRoundBanner by remember { mutableStateOf(false) }
    LaunchedEffect(state, state.round) {
        showRoundBanner = true
        delay(1400)
        showRoundBanner = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Ink, BoardMid, Ink)))
            .vignette(),
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(10.dp)) {
            // ---- Header ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(factionLabel(aiFaction), style = SectionTitle)
                    Gems(lives = state.playerB.lives, modifier = Modifier.padding(top = 3.dp))
                    Text(
                        text = state.playerB.leader.name + if (state.playerB.leaderUsed) " (spent)" else "",
                        color = if (state.playerB.leaderUsed) Color(0xFF5B6675) else MutedText,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ROUND ${state.round.coerceAtMost(3)}", color = MutedText, fontSize = 10.sp, letterSpacing = 2.sp)
                    Text(
                        text = when {
                            state.matchOver -> "—"
                            yourTurn -> "YOUR MOVE"
                            else -> "OPPONENT"
                        },
                        color = if (yourTurn) GoldLight else MutedText,
                        fontFamily = FontFamily.Serif,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                }
                TextButton(onClick = onExit) { Text("Exit", color = MutedText, fontSize = 12.sp) }
            }

            ThinRule(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))

            // ---- Opponent ----
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ArmyTotal(total = aiTotal, leading = aiTotal > playerTotal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(state.playerB.hand.size.coerceAtMost(8)) { CardBack(width = 15.dp, height = 21.dp) }
                    }
                }
                Piles(deck = state.playerB.deck.size, graveyard = state.playerB.discard.size)
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ROW_ORDER_TOP_DOWN.forEach { row ->
                    RowSlot(row, state.playerB.board.getValue(row), state, GameEngine.rowPower(state, Side.B, row))
                }
            }

            // ---- Centre line, doubling as the weather slot ----
            OrnateDivider(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            if (state.weatheredRows.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    state.weatheredRows.sortedBy { it.name }.forEach { row ->
                        Chip(weatherName(row), WeatherTint)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // ---- Player ----
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ROW_ORDER_TOP_DOWN.reversed().forEach { row ->
                    RowSlot(
                        row = row,
                        cards = state.playerA.board.getValue(row),
                        state = state,
                        totalPower = GameEngine.rowPower(state, Side.A, row),
                        selectableTargets = decoyTargets,
                        onCardTap = if (pendingCard?.ability == Ability.DECOY) {
                            { target -> play(pendingCard!!.id, PlayTarget.DecoyTarget(target.id)); pendingCard = null }
                        } else null,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ArmyTotal(total = playerTotal, leading = playerTotal > aiTotal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(factionLabel(playerFaction), style = SectionTitle)
                        Gems(lives = state.playerA.lives, modifier = Modifier.padding(top = 3.dp))
                    }
                }
                Piles(deck = state.playerA.deck.size, graveyard = state.playerA.discard.size)
            }

            message?.let {
                Text(it, color = DangerRed, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }

            ThinRule(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))

            // ---- Hand ----
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(state.playerA.hand) { card ->
                    val isPending = pendingCard?.id == card.id
                    val lift by animateDpAsState(
                        targetValue = if (isPending) (-10).dp else 0.dp,
                        animationSpec = tween(180),
                        label = "lift",
                    )
                    CardView(
                        card = card,
                        selected = isPending,
                        dimmed = !yourTurn,
                        modifier = Modifier.offset(y = lift),
                        onClick = if (yourTurn && pendingCard == null) {
                            { onHandCardTap(card) }
                        } else null,
                    )
                }
            }

            // Leader ability: once per match, and it costs the turn like playing a card.
            val leaderReady = GameEngine.canUseLeader(state, HUMAN)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (leaderReady) Brush.horizontalGradient(listOf(Color(0xFF2A2214), PanelBackground)) else Brush.horizontalGradient(listOf(PanelBackground, PanelBackground)))
                    .border(1.dp, if (leaderReady) MetalGold else SolidColor(Color(0xFF2A3140)), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        state.playerA.leader.name,
                        color = if (leaderReady) GoldLight else Color(0xFF5B6675),
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                    Text(
                        if (state.playerA.leaderUsed) "Already used this match" else state.playerA.leader.description,
                        color = MutedText,
                        fontSize = 10.sp,
                    )
                }
                Button(
                    onClick = {
                        message = null
                        GameEngine.useLeader(state, HUMAN)
                        runAiIfNeeded()
                        tick++
                    },
                    enabled = leaderReady,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3A2F17),
                        contentColor = GoldLight,
                        disabledContainerColor = Color(0xFF1A1F29),
                        disabledContentColor = Color(0xFF5B6675),
                    ),
                ) { Text("Use", fontFamily = FontFamily.Serif) }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            ) {
                Button(
                    onClick = {
                        message = null
                        GameEngine.pass(state, HUMAN)
                        runAiIfNeeded()
                        tick++
                    },
                    enabled = yourTurn,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2A3140),
                        contentColor = GoldLight,
                        disabledContainerColor = Color(0xFF1A1F29),
                        disabledContentColor = MutedText,
                    ),
                ) { Text("Pass round", fontFamily = FontFamily.Serif, letterSpacing = 1.sp) }
                OutlinedButton(
                    onClick = {
                        state = GameEngine.newMatch(playerFaction, aiFaction)
                        mulliganDone = false
                        pendingCard = null
                        message = null
                        tick++
                    },
                ) { Text("New match", color = MutedText) }
            }
        }

        // ---- Decoy targeting prompt ----
        AnimatedVisibility(
            visible = pendingCard?.ability == Ability.DECOY,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xCC0B0E14), Color(0xFF0B0E14))))
                    .border(1.dp, GoldDeep)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Choose a unit to pull back to your hand", color = GoldLight, fontFamily = FontFamily.Serif, fontSize = 13.sp)
                TextButton(onClick = { pendingCard = null }) { Text("Cancel", color = MutedText) }
            }
        }

        // ---- Round banner ----
        AnimatedVisibility(
            visible = showRoundBanner && !state.matchOver,
            enter = fadeIn(tween(350)) + scaleIn(tween(450), initialScale = 0.85f),
            exit = fadeOut(tween(350)) + scaleOut(tween(350), targetScale = 1.1f),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ROUND ${state.round.coerceAtMost(3)}", style = BannerText)
                ThinRule(modifier = Modifier.width(200.dp).padding(top = 6.dp))
            }
        }

        // ---- Match result ----
        if (state.matchOver) {
            val winner = state.matchWinner
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.radialGradient(listOf(Color(0xE60B0E14), Color(0xF505070A)))),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = when (winner) {
                            Side.A -> "VICTORY"
                            Side.B -> "DEFEAT"
                            null -> "DRAW"
                        },
                        style = BannerText,
                        color = when (winner) {
                            Side.A -> GoldLight
                            Side.B -> DangerRed
                            null -> MutedText
                        },
                    )
                    ThinRule(modifier = Modifier.width(220.dp).padding(vertical = 10.dp))
                    Text(
                        text = "rounds ${state.playerA.roundsWon} — ${state.playerB.roundsWon}",
                        color = MutedText,
                        fontFamily = FontFamily.Serif,
                        fontSize = 17.sp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 18.dp)) {
                        Button(
                            onClick = {
                                state = GameEngine.newMatch(playerFaction, aiFaction)
                                mulliganDone = false
                                pendingCard = null
                                message = null
                                tick++
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A3140), contentColor = GoldLight),
                        ) { Text("Rematch", fontFamily = FontFamily.Serif) }
                        OutlinedButton(onClick = onExit) { Text("Change deck", color = MutedText) }
                    }
                }
            }
        }
    }

    if (pendingCard?.ability == Ability.MEDIC) {
        val medic = pendingCard!!
        AlertDialog(
            onDismissRequest = { pendingCard = null },
            containerColor = PanelBackground,
            title = { Text("Raise a fallen card", style = SectionTitle) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "${medic.name} can return one card from your graveyard.",
                        color = MutedText,
                        fontSize = 12.sp,
                    )
                    state.playerA.discard.forEach { c ->
                        TextButton(onClick = {
                            play(medic.id, PlayTarget.MedicRevive(c.id))
                            pendingCard = null
                        }) {
                            Text("${c.name}  ·  ${c.basePower}", color = GoldText, fontFamily = FontFamily.Serif)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { play(medic.id, PlayTarget.MedicRevive(null)); pendingCard = null }) {
                    Text("Skip", color = MutedText)
                }
            },
        )
    }
}

private fun weatherName(row: GwentRow): String = when (row) {
    GwentRow.MELEE -> "FROST"
    GwentRow.RANGED -> "FOG"
    GwentRow.SIEGE -> "RAIN"
}

/** The two gems each army starts with; losing both loses the match. */
@Composable
private fun Gems(lives: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(STARTING_LIVES) { index ->
            RoundPip(won = index < lives)
        }
    }
}
