package ir.gwent.core.engine

import ir.gwent.core.model.Ability
import ir.gwent.core.model.Card
import ir.gwent.core.model.CardDatabase
import ir.gwent.core.model.Faction
import ir.gwent.core.model.GameState
import ir.gwent.core.model.PlayerState
import ir.gwent.core.model.Row
import ir.gwent.core.model.RoundResult
import ir.gwent.core.model.Side
import ir.gwent.core.model.other

sealed class GameEvent {
    data class CardPlayed(val side: Side, val card: Card) : GameEvent()
    data class Scorched(val destroyed: List<Card>) : GameEvent()
    data class Passed(val side: Side) : GameEvent()
    data class RoundEnded(val result: RoundResult) : GameEvent()
    data class MatchEnded(val winner: Side?) : GameEvent()
    data class InvalidMove(val reason: String) : GameEvent()
}

const val ROUNDS_TO_WIN = 2

object GameEngine {

    fun newMatch(factionA: Faction, factionB: Faction): GameState {
        val a = PlayerState(Side.A, factionA, CardDatabase.deckFor(factionA).toMutableList())
        val b = PlayerState(Side.B, factionB, CardDatabase.deckFor(factionB).toMutableList())
        return GameState(a, b)
    }

    /** Effective power of a card given the current contents of the row it sits in. */
    fun effectivePower(card: Card, rowCards: List<Card>): Int {
        if (card.isHero) return card.basePower
        val horned = rowCards.any { it.ability == Ability.HORN }
        return if (horned) card.basePower * 2 else card.basePower
    }

    fun rowPower(state: GameState, side: Side, row: Row): Int {
        val rowCards = state.player(side).board.getValue(row)
        return rowCards.sumOf { effectivePower(it, rowCards) }
    }

    fun totalPower(state: GameState, side: Side): Int =
        Row.entries.sumOf { rowPower(state, side, it) }

    fun playCard(state: GameState, side: Side, cardId: String): List<GameEvent> {
        val events = mutableListOf<GameEvent>()
        if (state.matchWinner != null) return listOf(GameEvent.InvalidMove("Match is already over"))
        if (state.turn != side) return listOf(GameEvent.InvalidMove("Not this player's turn"))
        val player = state.player(side)
        if (player.passed) return listOf(GameEvent.InvalidMove("Player has already passed this round"))
        val card = player.hand.find { it.id == cardId }
            ?: return listOf(GameEvent.InvalidMove("Card not in hand: $cardId"))

        player.hand.remove(card)
        player.board.getValue(card.row).add(card)
        events += GameEvent.CardPlayed(side, card)

        if (card.ability == Ability.SCORCH) {
            events += applyScorch(state)
        }

        advanceTurn(state)
        events += maybeResolveRound(state)
        return events
    }

    fun pass(state: GameState, side: Side): List<GameEvent> {
        if (state.matchWinner != null) return listOf(GameEvent.InvalidMove("Match is already over"))
        if (state.turn != side) return listOf(GameEvent.InvalidMove("Not this player's turn"))
        val player = state.player(side)
        if (player.passed) return listOf(GameEvent.InvalidMove("Player has already passed this round"))

        player.passed = true
        val events = mutableListOf<GameEvent>(GameEvent.Passed(side))
        advanceTurn(state)
        events += maybeResolveRound(state)
        return events
    }

    private fun applyScorch(state: GameState): GameEvent.Scorched {
        val all = (Row.entries.flatMap { state.playerA.board.getValue(it) } +
            Row.entries.flatMap { state.playerB.board.getValue(it) })
            .filter { !it.isHero }
        if (all.isEmpty()) return GameEvent.Scorched(emptyList())
        val highest = all.maxOf { c ->
            val rowCards = state.player(sideOf(state, c)).board.getValue(c.row)
            effectivePower(c, rowCards)
        }
        val destroyed = all.filter { c ->
            val rowCards = state.player(sideOf(state, c)).board.getValue(c.row)
            effectivePower(c, rowCards) == highest
        }
        destroyed.forEach { c ->
            val owner = state.player(sideOf(state, c))
            owner.board.getValue(c.row).remove(c)
        }
        return GameEvent.Scorched(destroyed)
    }

    private fun sideOf(state: GameState, card: Card): Side =
        if (Row.entries.any { state.playerA.board.getValue(it).contains(card) }) Side.A else Side.B

    private fun advanceTurn(state: GameState) {
        val other = state.turn.other()
        state.turn = when {
            !state.player(other).passed -> other
            !state.player(state.turn).passed -> state.turn
            else -> state.turn // both passed; caller resolves the round next
        }
    }

    private fun maybeResolveRound(state: GameState): List<GameEvent> {
        if (!(state.playerA.passed && state.playerB.passed)) return emptyList()

        val powerA = totalPower(state, Side.A)
        val powerB = totalPower(state, Side.B)
        val winner = when {
            powerA > powerB -> Side.A
            powerB > powerA -> Side.B
            else -> null
        }
        winner?.let { state.player(it).roundsWon++ }
        val result = RoundResult(state.round, powerA, powerB, winner)
        state.roundHistory.add(result)

        val events = mutableListOf<GameEvent>(GameEvent.RoundEnded(result))

        val matchWinner = when {
            state.playerA.roundsWon >= ROUNDS_TO_WIN -> Side.A
            state.playerB.roundsWon >= ROUNDS_TO_WIN -> Side.B
            state.round >= 3 -> if (state.playerA.roundsWon != state.playerB.roundsWon) {
                if (state.playerA.roundsWon > state.playerB.roundsWon) Side.A else Side.B
            } else null
            else -> null
        }

        if (matchWinner != null || state.round >= 3) {
            state.matchWinner = matchWinner
            events += GameEvent.MatchEnded(matchWinner)
            return events
        }

        // Start next round: loser (or previous starter, on a draw) goes first.
        state.starter = winner?.other() ?: state.starter
        state.round++
        state.turn = state.starter
        state.playerA.passed = false
        state.playerB.passed = false
        return events
    }
}
