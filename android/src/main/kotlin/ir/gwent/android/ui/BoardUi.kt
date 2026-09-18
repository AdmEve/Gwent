package ir.gwent.android.ui

import ir.gwent.core.engine.GameEngine
import ir.gwent.core.model.Card
import ir.gwent.core.model.GameState
import ir.gwent.core.model.Row
import ir.gwent.core.model.Side

/**
 * An immutable picture of the board, rebuilt after every action.
 *
 * The engine mutates its state in place, and handing those live lists to a LazyRow crashes:
 * the lazy list caches an item count and then indexes the list again during measurement,
 * which happens while scrolling — so playing a card mid-fling makes it read past the end.
 * Copying into immutable lists and reassigning this object is also what lets Compose see the
 * change properly, instead of being poked to redraw and re-reading mutable data.
 */
data class RowUi(
    val row: Row,
    val cards: List<Card>,
    val total: Int,
    val weathered: Boolean,
    val power: Map<String, Int>,
)

data class SideUi(
    val faction: ir.gwent.core.model.Faction,
    val rows: List<RowUi>,
    val total: Int,
    val handCount: Int,
    val deckCount: Int,
    val graveCount: Int,
    val lives: Int,
    val roundsWon: Int,
    val leaderName: String,
    val leaderDescription: String,
    val leaderUsed: Boolean,
)

data class BoardUi(
    val round: Int,
    val matchOver: Boolean,
    val winner: Side?,
    val yourTurn: Boolean,
    val weathered: Set<Row>,
    val you: SideUi,
    val opponent: SideUi,
    val hand: List<Card>,
    val graveyard: List<Card>,
    val mulligansLeft: Int,
    val leaderReady: Boolean,
) {
    /** Non-hero units of yours that a Decoy could pull back. */
    val decoyTargets: Set<String> =
        you.rows.flatMap { it.cards }.filter { !it.isHero }.map { it.id }.toSet()
}

private fun sideUi(state: GameState, side: Side): SideUi {
    val player = state.player(side)
    val rows = Row.entries.map { row ->
        val cards = player.board.getValue(row).toList() // copy: the engine keeps mutating the original
        RowUi(
            row = row,
            cards = cards,
            total = GameEngine.rowPower(state, side, row),
            weathered = row in state.weatheredRows,
            power = cards.associate { it.id to GameEngine.effectivePower(state, it, row, cards) },
        )
    }
    return SideUi(
        faction = player.faction,
        rows = rows,
        total = GameEngine.totalPower(state, side),
        handCount = player.hand.size,
        deckCount = player.deck.size,
        graveCount = player.discard.size,
        lives = player.lives,
        roundsWon = player.roundsWon,
        leaderName = player.leader.name,
        leaderDescription = player.leader.description,
        leaderUsed = player.leaderUsed,
    )
}

fun snapshotOf(state: GameState, human: Side): BoardUi = BoardUi(
    round = state.round.coerceAtMost(3),
    matchOver = state.matchOver,
    winner = state.matchWinner,
    yourTurn = state.turn == human && !state.matchOver,
    weathered = state.weatheredRows.toSet(),
    you = sideUi(state, human),
    opponent = sideUi(state, if (human == Side.A) Side.B else Side.A),
    hand = state.player(human).hand.toList(),
    graveyard = state.player(human).discard.toList(),
    mulligansLeft = state.player(human).mulligansLeft,
    leaderReady = GameEngine.canUseLeader(state, human),
)
