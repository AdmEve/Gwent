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
 * A heuristic (not optimal) opponent: reads the board rather than the deck it hasn't
 * seen, weighs Scorch/Weather by whether they net positive value, spies for card
 * advantage when behind on cards, and bluffs a pass only when comfortably ahead.
 * Good enough to make round-conceding decisions feel meaningful; not meant to be
 * unbeatable.
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
            return bestMove(state, side) ?: Move.Pass
        }

        val best = bestMove(state, side) ?: return Move.Pass
        val comfortablyAhead = myTotal > oppTotal + 5
        val lastRound = state.round == 3
        if (comfortablyAhead && !lastRound && me.hand.size <= opp.hand.size) {
            return Move.Pass
        }
        return best
    }

    private fun bestMove(state: GameState, side: Side): Move.PlayCard? {
        val me = state.player(side)
        val opp = state.player(side.other())
        if (me.hand.isEmpty()) return null

        val scorch = me.hand.find { it.ability == Ability.SCORCH }
        if (scorch != null && scorchTargetsEnemy(state, side)) return Move.PlayCard(scorch.id)

        val horn = me.hand
            .filter { it.ability == Ability.HORN && me.board.getValue(it.row).isNotEmpty() }
            .maxByOrNull { candidate -> me.board.getValue(candidate.row).sumOf { it.basePower } }
        if (horn != null) return Move.PlayCard(horn.id)

        val weather = me.hand
            .filter { it.ability == Ability.WEATHER }
            .filter { GameEngine.rowPower(state, side.other(), it.row) > GameEngine.rowPower(state, side, it.row) + 3 }
            .maxByOrNull { GameEngine.rowPower(state, side.other(), it.row) }
        if (weather != null) return Move.PlayCard(weather.id)

        val spy = me.hand.find { it.ability == Ability.SPY }
        if (spy != null && me.hand.size <= opp.hand.size) return Move.PlayCard(spy.id)

        val decoyCard = me.hand.find { it.ability == Ability.DECOY }
        val decoyTargetCard = decoyCard?.let {
            Row.entries.flatMap { row -> me.board.getValue(row) }
                .filter { c -> !c.isHero }
                .minByOrNull { c -> c.basePower }
        }

        val excludedFromPlain = setOf(
            Ability.HORN, Ability.WEATHER, Ability.CLEAR_WEATHER, Ability.DECOY, Ability.SPY, Ability.SCORCH,
        )
        val plain = me.hand.filter { it.ability !in excludedFromPlain }.maxByOrNull { it.basePower }
        val best = plain ?: (decoyCard?.takeIf { decoyTargetCard != null }) ?: me.hand.maxByOrNull { it.basePower }

        return when {
            best == null -> null
            best.ability == Ability.MEDIC -> {
                val revive = me.discard.maxByOrNull { it.basePower }
                Move.PlayCard(best.id, revive?.let { PlayTarget.MedicRevive(it.id) })
            }
            best.ability == Ability.DECOY -> {
                val target = decoyTargetCard ?: return null
                Move.PlayCard(best.id, PlayTarget.DecoyTarget(target.id))
            }
            else -> Move.PlayCard(best.id)
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
        return all.any { ownerOf(it) != side && effective(it) == highest }
    }
}
