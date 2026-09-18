package ir.gwent.core.ai

import ir.gwent.core.engine.GameEngine
import ir.gwent.core.model.Ability
import ir.gwent.core.model.Card
import ir.gwent.core.model.GameState
import ir.gwent.core.model.Row
import ir.gwent.core.model.Side
import ir.gwent.core.model.other

sealed class Move {
    data class PlayCard(val cardId: String) : Move()
    data object Pass : Move()
}

/**
 * A heuristic (not optimal) opponent: reads the board rather than the deck it hasn't
 * seen, weighs Scorch by whether it nets positive value, and bluffs a pass only when
 * comfortably ahead. Good enough to make round-conceding decisions feel meaningful;
 * not meant to be unbeatable.
 */
object SimpleAi {

    fun chooseMove(state: GameState, side: Side): Move {
        val me = state.player(side)
        val opp = state.player(side.other())
        if (me.hand.isEmpty()) return Move.Pass

        val myTotal = GameEngine.totalPower(state, side)
        val oppTotal = GameEngine.totalPower(state, side.other())

        if (opp.passed) {
            if (myTotal > oppTotal) return Move.Pass
            val best = bestCard(state, side)
            return best?.let { Move.PlayCard(it.id) } ?: Move.Pass
        }

        val best = bestCard(state, side) ?: return Move.Pass
        val comfortablyAhead = myTotal > oppTotal + 5
        val lastRound = state.round == 3
        if (comfortablyAhead && !lastRound && me.hand.size <= opp.hand.size) {
            return Move.Pass
        }
        return Move.PlayCard(best.id)
    }

    private fun bestCard(state: GameState, side: Side): Card? {
        val me = state.player(side)
        if (me.hand.isEmpty()) return null

        val scorch = me.hand.find { it.ability == Ability.SCORCH }
        if (scorch != null && scorchTargetsEnemy(state, side)) return scorch

        val horn = me.hand
            .filter { it.ability == Ability.HORN && me.board.getValue(it.row).isNotEmpty() }
            .maxByOrNull { candidate -> me.board.getValue(candidate.row).sumOf { it.basePower } }
        if (horn != null) return horn

        return me.hand.filter { it.ability != Ability.HORN }.maxByOrNull { it.basePower }
            ?: me.hand.maxByOrNull { it.basePower }
    }

    /** Only worth playing Scorch if the current board-wide highest-power unit is the enemy's. */
    private fun scorchTargetsEnemy(state: GameState, side: Side): Boolean {
        val all = (Row.entries.flatMap { state.playerA.board.getValue(it) } +
            Row.entries.flatMap { state.playerB.board.getValue(it) }).filter { !it.isHero }
        if (all.isEmpty()) return false

        fun ownerOf(card: Card): Side =
            if (state.playerA.board.getValue(card.row).contains(card)) Side.A else Side.B

        fun effective(card: Card): Int =
            GameEngine.effectivePower(card, state.player(ownerOf(card)).board.getValue(card.row))

        val highest = all.maxOf { effective(it) }
        return all.any { ownerOf(it) != side && effective(it) == highest }
    }
}
