package ir.gwent.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.gwent.android.GwentApp
import ir.gwent.core.ai.Move
import ir.gwent.core.ai.SimpleAi
import ir.gwent.core.engine.GameEngine
import ir.gwent.core.engine.GameEvent
import ir.gwent.core.engine.PlayTarget
import ir.gwent.core.model.Ability
import ir.gwent.core.model.Card as GwentCard
import ir.gwent.core.model.Faction
import ir.gwent.core.model.Row as GwentRow
import ir.gwent.core.model.STARTING_LIVES
import ir.gwent.core.model.Side
import kotlinx.coroutines.delay

private val HUMAN = Side.A
private val AI = Side.B
private val ROW_ORDER_TOP_DOWN = listOf(GwentRow.SIEGE, GwentRow.RANGED, GwentRow.MELEE)

/**
 * One line of the battlefield: a carved banner holding the row's score, then the units. Card
 * size is derived from the height the row was given, so the whole board fits any phone
 * without ever scrolling.
 */
@Composable
fun RowSlot(
    row: RowUi,
    modifier: Modifier = Modifier,
    selectableTargets: Set<String> = emptySet(),
    onCardTap: ((GwentCard) -> Unit)? = null,
) {
    val targeting = selectableTargets.isNotEmpty() && onCardTap != null
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        if (targeting) RowSlotLit else Color(0x66101620),
                        if (row.weathered) Color(0xAA16222E) else Color(0x4D0E131B),
                        if (targeting) RowSlotLit else Color(0x66101620),
                    )
                )
            )
            .border(1.dp, if (targeting) GoldDeep else Color(0x552A3240), RoundedCornerShape(6.dp)),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val cardHeight = maxHeight * 0.86f
            val cardWidth = cardHeight * 0.70f

            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                RowBanner(
                    row = row.row,
                    total = row.total,
                    weathered = row.weathered,
                    modifier = Modifier.width(34.dp).fillMaxHeight(0.8f),
                )
                Spacer(modifier = Modifier.width(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(row.cards, key = { it.id }) { card ->
                        val selectable = card.id in selectableTargets
                        // Cards land rather than appear.
                        val land = remember(card.id) { Animatable(0.72f) }
                        LaunchedEffect(card.id) { land.animateTo(1f, tween(240)) }
                        CardView(
                            card = card,
                            displayPower = row.power[card.id],
                            selected = selectable,
                            dimmed = targeting && !selectable,
                            weathered = row.weathered && !card.isHero,
                            width = cardWidth,
                            height = cardHeight,
                            modifier = Modifier.graphicsLayer {
                                scaleX = land.value
                                scaleY = land.value
                            },
                            onClick = if (onCardTap != null && (selectableTargets.isEmpty() || selectable)) {
                                { onCardTap(card) }
                            } else null,
                        )
                    }
                }
            }
        }

        if (row.weathered) {
            WeatherOverlay(row = row.row, modifier = Modifier.fillMaxSize())
        }
    }
}

/** The hand as an arc of overlapping cards, the way you'd actually hold them. */
@Composable
private fun FannedHand(
    cards: List<GwentCard>,
    enabled: Boolean,
    pendingId: String?,
    onCardTap: (GwentCard) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val cardHeight = maxHeight * 0.86f
        val cardWidth = cardHeight * 0.70f
        val count = cards.size
        if (count == 0) return@BoxWithConstraints

        val room = maxWidth - cardWidth - 8.dp
        val step = if (count > 1) (room / (count - 1)).coerceAtMost(cardWidth * 0.82f) else 0.dp
        val spread = cardWidth + step * (count - 1)
        val startX = (maxWidth - spread) / 2

        cards.forEachIndexed { index, card ->
            val centred = index - (count - 1) / 2f
            val isPending = pendingId == card.id
            val lift by animateDpAsState(
                targetValue = if (isPending) (-14).dp else (centred * centred * 0.9f).dp,
                animationSpec = tween(180),
                label = "handLift",
            )
            Box(
                modifier = Modifier
                    .offset(x = startX + step * index, y = lift)
                    .graphicsLayer {
                        rotationZ = centred * 3.4f
                        transformOrigin = TransformOrigin(0.5f, 1.35f)
                    },
            ) {
                CardView(
                    card = card,
                    selected = isPending,
                    dimmed = !enabled,
                    width = cardWidth,
                    height = cardHeight,
                    onClick = if (enabled) {
                        { onCardTap(card) }
                    } else null,
                )
            }
        }
    }
}

@Composable
fun BoardScreen(playerFaction: Faction, aiFaction: Faction, onExit: () -> Unit) {
    val context = LocalContext.current
    var engine by remember(playerFaction, aiFaction) {
        mutableStateOf(GameEngine.newMatch(playerFaction, aiFaction))
    }

    // Everything drawn below reads this immutable snapshot, never the live game state.
    var ui by remember(playerFaction, aiFaction) { mutableStateOf(snapshotOf(engine, HUMAN)) }
    var pendingCard by remember { mutableStateOf<GwentCard?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var mulliganDone by remember { mutableStateOf(false) }

    fun refresh() {
        ui = snapshotOf(engine, HUMAN)
    }

    fun safely(what: String, block: () -> Unit) {
        GwentApp.note(context, "r${engine.round} $what")
        runCatching(block).onFailure { error ->
            GwentApp.recordHandled(context, error)
            message = "Something went wrong (${error::class.java.simpleName}). Reopen the app to see the report."
        }
        refresh()
    }

    fun runAiIfNeeded() {
        var guard = 0
        while (engine.turn == AI && !engine.matchOver && guard < 200) {
            val events = when (val move = SimpleAi.chooseMove(engine, AI)) {
                is Move.Pass -> GameEngine.pass(engine, AI)
                is Move.UseLeader -> GameEngine.useLeader(engine, AI)
                is Move.PlayCard -> GameEngine.playCard(engine, AI, move.cardId, move.target)
            }
            if (events.any { it is GameEvent.InvalidMove }) break
            guard++
        }
        if (guard >= 200) {
            GwentApp.note(context, "AI RUNNER HIT ITS CAP — turn=${engine.turn} round=${engine.round}")
            message = "The opponent got stuck and was stopped. Please report this."
        }
    }

    fun play(cardId: String, target: PlayTarget?) {
        message = null
        safely("play $cardId") {
            val events = GameEngine.playCard(engine, HUMAN, cardId, target)
            events.filterIsInstance<GameEvent.InvalidMove>().firstOrNull()?.let { message = it.reason }
            runAiIfNeeded()
        }
    }

    fun restart() {
        engine = GameEngine.newMatch(playerFaction, aiFaction)
        pendingCard = null
        message = null
        mulliganDone = false
        refresh()
    }

    fun onHandCardTap(card: GwentCard) {
        if (!ui.yourTurn) return
        when (card.ability) {
            Ability.DECOY -> {
                if (ui.decoyTargets.isNotEmpty()) pendingCard = card
                else message = "No unit on your board to swap with Decoy."
            }
            Ability.MEDIC -> {
                if (ui.graveyard.isNotEmpty()) pendingCard = card else play(card.id, null)
            }
            else -> play(card.id, null)
        }
    }

    LaunchedEffect(engine, mulliganDone) {
        if (mulliganDone) safely("opening turn") { runAiIfNeeded() }
    }

    if (!mulliganDone) {
        MulliganScreen(
            hand = ui.hand,
            swapsLeft = ui.mulligansLeft,
            playerFaction = playerFaction,
            aiFaction = aiFaction,
            onSwap = { card -> safely("swap ${card.id}") { GameEngine.mulligan(engine, HUMAN, card.id) } },
            onReady = {
                safely("begin match") {
                    repeat(engine.playerB.mulligansLeft) {
                        val worst = engine.playerB.hand.minByOrNull { it.basePower }
                        if (worst != null) GameEngine.mulligan(engine, AI, worst.id)
                    }
                }
                mulliganDone = true
            },
        )
        return
    }

    val decoyTargets = if (pendingCard?.ability == Ability.DECOY) ui.decoyTargets else emptySet()

    var showRoundBanner by remember { mutableStateOf(false) }
    LaunchedEffect(engine, ui.round) {
        showRoundBanner = true
        delay(1300)
        showRoundBanner = false
    }

    Box(modifier = Modifier.fillMaxSize().tableSurface()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 4.dp)) {

            // ---- Opponent's side of the table ----
            ArmyBar(
                faction = aiFaction,
                side = ui.opponent,
                leading = ui.opponent.total > ui.you.total,
                leaderReady = false,
                showHandBacks = true,
                onLeader = null,
                trailing = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ROUND ${ui.round}", color = MutedText, fontSize = 9.sp, letterSpacing = 1.5.sp)
                        Text(
                            text = when {
                                ui.matchOver -> "—"
                                ui.yourTurn -> "YOUR MOVE"
                                else -> "OPPONENT"
                            },
                            color = if (ui.yourTurn) GoldLight else MutedText,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        )
                    }
                },
            )

            // ---- The battlefield: six rows, sized to whatever is left ----
            Column(
                modifier = Modifier.weight(1f).padding(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                ROW_ORDER_TOP_DOWN.forEach { r ->
                    RowSlot(row = ui.opponent.rows.first { it.row == r }, modifier = Modifier.weight(1f))
                }

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CentreLine(modifier = Modifier.fillMaxWidth())
                    if (ui.weathered.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            ui.weathered.sortedBy { it.name }.forEach { r -> Chip(weatherName(r), WeatherTint) }
                        }
                    }
                }

                ROW_ORDER_TOP_DOWN.reversed().forEach { r ->
                    RowSlot(
                        row = ui.you.rows.first { it.row == r },
                        modifier = Modifier.weight(1f),
                        selectableTargets = decoyTargets,
                        onCardTap = if (pendingCard?.ability == Ability.DECOY) {
                            { target ->
                                val decoy = pendingCard
                                pendingCard = null
                                if (decoy != null) play(decoy.id, PlayTarget.DecoyTarget(target.id))
                            }
                        } else null,
                    )
                }
            }

            // ---- Your side ----
            ArmyBar(
                faction = playerFaction,
                side = ui.you,
                leading = ui.you.total > ui.opponent.total,
                leaderReady = ui.leaderReady,
                showHandBacks = false,
                onLeader = {
                    message = null
                    safely("leader") {
                        GameEngine.useLeader(engine, HUMAN)
                        runAiIfNeeded()
                    }
                },
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        CarvedButton(
                            text = "PASS",
                            primary = true,
                            enabled = ui.yourTurn,
                            onClick = {
                                message = null
                                safely("pass") {
                                    GameEngine.pass(engine, HUMAN)
                                    runAiIfNeeded()
                                }
                            },
                        )
                        CarvedButton(text = "EXIT", onClick = onExit)
                    }
                },
            )

            message?.let {
                Text(it, color = DangerRed, fontSize = 10.sp, modifier = Modifier.padding(start = 4.dp))
            }

            // ---- Hand ----
            FannedHand(
                cards = ui.hand,
                enabled = ui.yourTurn && pendingCard == null,
                pendingId = pendingCard?.id,
                onCardTap = { onHandCardTap(it) },
                modifier = Modifier.fillMaxWidth().height(104.dp),
            )
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
                Text("Choose a unit to pull back", color = GoldLight, fontFamily = FontFamily.Serif, fontSize = 12.sp)
                CarvedButton(text = "CANCEL", onClick = { pendingCard = null })
            }
        }

        // ---- Round banner ----
        AnimatedVisibility(
            visible = showRoundBanner && !ui.matchOver,
            enter = fadeIn(tween(300)) + scaleIn(tween(420), initialScale = 0.85f),
            exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 1.1f),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ROUND ${ui.round}", style = BannerText)
                ThinRule(modifier = Modifier.width(200.dp).padding(top = 6.dp))
            }
        }

        // ---- Match result ----
        if (ui.matchOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.radialGradient(listOf(Color(0xE60B0E14), Color(0xF505070A)))),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = when (ui.winner) {
                            Side.A -> "VICTORY"
                            Side.B -> "DEFEAT"
                            null -> "DRAW"
                        },
                        style = BannerText,
                        color = when (ui.winner) {
                            Side.A -> GoldLight
                            Side.B -> DangerRed
                            null -> MutedText
                        },
                    )
                    ThinRule(modifier = Modifier.width(220.dp).padding(vertical = 10.dp))
                    Text(
                        text = "rounds ${ui.you.roundsWon} — ${ui.opponent.roundsWon}",
                        color = MutedText,
                        fontFamily = FontFamily.Serif,
                        fontSize = 16.sp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 18.dp)) {
                        CarvedButton(text = "REMATCH", primary = true, onClick = { restart() })
                        CarvedButton(text = "CHANGE DECK", onClick = onExit)
                    }
                }
            }
        }
    }

    val medic = pendingCard
    if (medic != null && medic.ability == Ability.MEDIC) {
        AlertDialog(
            onDismissRequest = { pendingCard = null },
            containerColor = PanelBackground,
            title = { Text("Raise a fallen card", style = SectionTitle) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("${medic.name} can return one card from your graveyard.", color = MutedText, fontSize = 12.sp)
                    ui.graveyard.forEach { c ->
                        TextButton(onClick = {
                            pendingCard = null
                            play(medic.id, PlayTarget.MedicRevive(c.id))
                        }) {
                            Text("${c.name}  ·  ${c.basePower}", color = GoldText, fontFamily = FontFamily.Serif)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    pendingCard = null
                    play(medic.id, PlayTarget.MedicRevive(null))
                }) { Text("Skip", color = MutedText) }
            },
        )
    }
}

/** The strip carrying an army's leader, gems, total and piles. */
@Composable
private fun ArmyBar(
    faction: Faction,
    side: SideUi,
    leading: Boolean,
    leaderReady: Boolean,
    showHandBacks: Boolean,
    onLeader: (() -> Unit)?,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LeaderBadge(
                name = side.leaderName,
                accent = factionPalette(faction).accent,
                used = side.leaderUsed,
                ready = leaderReady,
                onClick = onLeader,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(factionLabel(faction), style = SectionTitle, fontSize = 12.sp)
                Gems(lives = side.lives, total = STARTING_LIVES, modifier = Modifier.padding(top = 2.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            ArmyGem(total = side.total, leading = leading)
            if (showHandBacks) {
                Spacer(modifier = Modifier.width(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    repeat(side.handCount.coerceAtMost(6)) {
                        CardBack(width = 13.dp, height = 18.dp)
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CardPile(count = side.deckCount, label = "DECK")
            CardPile(count = side.graveCount, label = "GRAVE")
            trailing()
        }
    }
}

private fun weatherName(row: GwentRow): String = when (row) {
    GwentRow.MELEE -> "FROST"
    GwentRow.RANGED -> "FOG"
    GwentRow.SIEGE -> "RAIN"
}
