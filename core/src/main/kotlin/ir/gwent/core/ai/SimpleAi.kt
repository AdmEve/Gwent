package ir.gwent.core.ai

import ir.gwent.core.engine.GameEngine
import ir.gwent.core.engine.PlayTarget
import ir.gwent.core.model.Ability
import ir.gwent.core.model.Card
import ir.gwent.core.model.GameState
import ir.gwent.core.model.Row
import ir.gwent.core.model.Side
import ir.gwent.core.model.other

sealed class Move {
    data class PlayCard(val cardId: String, val target: PlayTarget? = null) : Move()
    data object Pass : Move()
}

/**
 * A heuristic (not optimal) opponent: reads the board rather than the deck it hasn't seen,
 * weighs Scorch and Weather by whether they net positive value, spies for card advantage when
 * behind on cards, and concedes a round it cannot win rather than burning its hand.
 *
 * Every move is checked against the engine before being returned, so a caller that drives the
 * AI in a loop can rely on each move actually advancing the game.
 */
object SimpleAi {

    fun chooseMove(state: GameState, side: Side): Move {
        if (state.matchOver || state.turn != side) return Move.Pass
        val me = state.player(side)
        if (me.passed || me.hand.isEmpty()) return Move.Pass

        val opp = state.player(side.other())
        val myTotal = GameEngine.totalPower(state, side)
        val oppTotal = GameEngine.totalPower(state, side.other())
        val lastStand = me.lives <= 1 // losing this round ends the match

        if (opp.passed) {
            // Already ahead with the opponent done: bank the round.
            if (myTotal > oppTotal) return Move.Pass
            // Can't catch up and it isn't fatal? Concede and keep the cards.
            val deficit = oppTotal - myTotal
            if (!lastStand && deficit > reachableGain(state, side) && state.round < 3) return Move.Pass
            return validated(state, side, bestMove(state, side))
        }

        val comfortablyAhead = myTotal > oppTotal + 8
        if (comfortablyAhead && !lastStand && me.hand.size <= opp.hand.size && state.round < 3) {
            return Move.Pass
        }
        return validated(state, side, bestMove(state, side))
    }

    /** Rough ceiling on how much power this hand could still add, used to decide whether to concede. */
    private fun reachableGain(state: GameState, side: Side): Int {
        val me = state.player(side)
        return me.hand.filter { it.ability != Ability.WEATHER && it.ability != Ability.CLEAR_WEATHER }
            .sumOf { it.basePower }
    }

    private fun validated(state: GameState, side: Side, move: Move.PlayCard?): Move {
        if (move != null && GameEngine.rejectionReason(state, side, move.cardId, move.target) == null) {
            return move
        }
        // Fall back to any legal card before giving up the round.
        val me = state.player(side)
        me.hand.forEach { card ->
            val target = defaultTargetFor(state, side, card)
            if (GameEngine.rejectionReason(state, side, card.id, target) == null) {
                return Move.PlayCard(card.id, target)
            }
        }
        return Move.Pass
    }

    private fun defaultTargetFor(state: GameState, side: Side, card: Card): PlayTarget? {
        val me = state.player(side)
        return when (card.ability) {
            Ability.DECOY -> weakestOwnUnit(state, side)?.let { PlayTarget.DecoyTarget(it.id) }
            Ability.MEDIC -> PlayTarget.MedicRevive(me.discard.maxByOrNull { it.basePower }?.id)
            else -> null
        }
    }

    private fun weakestOwnUnit(state: GameState, side: Side): Card? {
        val me = state.player(side)
        return Row.entries.flatMap { me.board.getValue(it) }
            .filter { !it.isHero }
            .minByOrNull { it.basePower }
    }

    private fun bestMove(state: GameState, side: Side): Move.PlayCard? {
        val me = state.player(side)
        val opp = state.player(side.other())
        if (me.hand.isEmpty()) return null

        // Scorch, but only when the biggest unit on the board belongs to the opponent.
        me.hand.firstOrNull { it.ability == Ability.SCORCH }?.let { scorch ->
            if (scorchTargetsEnemy(state, side)) return Move.PlayCard(scorch.id)
        }

        // Horn, on whichever of our rows it multiplies the most.
        me.hand.filter { it.ability == Ability.HORN && me.board.getValue(it.row).isNotEmpty() }
            .maxByOrNull { candidate -> me.board.getValue(candidate.row).sumOf { it.basePower } }
            ?.let { return Move.PlayCard(it.id) }

        // Weather, when it costs the opponent materially more than us.
        me.hand.filter { it.ability == Ability.WEATHER && it.row !in state.weatheredRows }
            .filter { GameEngine.rowPower(state, side.other(), it.row) > GameEngine.rowPower(state, side, it.row) + 4 }
            .maxByOrNull { GameEngine.rowPower(state, side.other(), it.row) }
            ?.let { return Move.PlayCard(it.id) }

        // Clear weather, when our own board is the one suffering.
        if (state.weatheredRows.isNotEmpty()) {
            val myLoss = state.weatheredRows.sumOf { weatherLoss(state, side, it) }
            val oppLoss = state.weatheredRows.sumOf { weatherLoss(state, side.other(), it) }
            if (myLoss > oppLoss + 4) {
                me.hand.firstOrNull { it.ability == Ability.CLEAR_WEATHER }?.let { return Move.PlayCard(it.id) }
            }
        }

        // Spy, while we are behind on cards and can afford the tempo.
        if (me.hand.size <= opp.hand.size && me.deck.isNotEmpty()) {
            me.hand.firstOrNull { it.ability == Ability.SPY }?.let { return Move.PlayCard(it.id) }
        }

        // Medic, bringing back the strongest thing in the graveyard.
        me.hand.firstOrNull { it.ability == Ability.MEDIC }?.let { medic ->
            val revive = me.discard.maxByOrNull { it.basePower }
            if (revive != null) return Move.PlayCard(medic.id, PlayTarget.MedicRevive(revive.id))
        }

        // Otherwise the biggest body that isn't a utility card we might want later.
        val held = setOf(
            Ability.HORN, Ability.WEATHER, Ability.CLEAR_WEATHER,
            Ability.DECOY, Ability.SPY, Ability.SCORCH,
        )
        me.hand.filter { it.ability !in held }.maxByOrNull { it.basePower }
            ?.let { return Move.PlayCard(it.id, defaultTargetFor(state, side, it)) }

        return me.hand.maxByOrNull { it.basePower }?.let {
            Move.PlayCard(it.id, defaultTargetFor(state, side, it))
        }
    }

    /** Power this side would regain in [row] if the weather lifted. */
    private fun weatherLoss(state: GameState, side: Side, row: Row): Int {
        val cards = state.player(side).board.getValue(row)
        val horned = cards.any { it.ability == Ability.HORN }
        return cards.filter { !it.isHero }.sumOf { c ->
            val full = if (horned) c.basePower * 2 else c.basePower
            val now = if (horned) 2 else 1
            (full - now).coerceAtLeast(0)
        }
    }

    /** Only worth playing Scorch if the current board-wide highest-power unit is the enemy's. */
    private fun scorchTargetsEnemy(state: GameState, side: Side): Boolean {
        val all = (Row.entries.flatMap { state.playerA.board.getValue(it) } +
            Row.entries.flatMap { state.playerB.board.getValue(it) }).filter { !it.isHero }
        if (all.isEmpty()) return false

        fun ownerOf(card: Card): Side =
            if (state.playerA.board.getValue(card.row).contains(card)) Side.A else Side.B

        fun effective(card: Card): Int =
            GameEngine.effectivePower(state, card, card.row, state.player(ownerOf(card)).board.getValue(card.row))

        val highest = all.maxOf { effective(it) }
        val enemyHolds = all.any { ownerOf(it) != side && effective(it) == highest }
        val weHold = all.any { ownerOf(it) == side && effective(it) == highest }
        return enemyHolds && !weHold
    }
}
