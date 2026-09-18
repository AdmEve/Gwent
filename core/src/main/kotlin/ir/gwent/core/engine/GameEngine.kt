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
import kotlin.random.Random

sealed class GameEvent {
    data class CardPlayed(val side: Side, val card: Card) : GameEvent()
    data class Scorched(val destroyed: List<Card>) : GameEvent()
    data class WeatherChanged(val side: Side, val row: Row) : GameEvent()
    data class WeatherCleared(val side: Side) : GameEvent()
    data class Decoyed(val side: Side, val returned: Card, val decoy: Card) : GameEvent()
    data class MedicRevived(val side: Side, val revived: Card) : GameEvent()
    data class SpyInfiltrated(val side: Side, val card: Card, val cardsDrawn: Int) : GameEvent()
    data class Passed(val side: Side) : GameEvent()
    data class RoundEnded(val result: RoundResult) : GameEvent()
    data class RoundStarted(val round: Int) : GameEvent()
    data class MatchEnded(val winner: Side?) : GameEvent()
    data class InvalidMove(val reason: String) : GameEvent()
}

/** Extra input a move needs beyond a card id, for abilities that require a target. */
sealed class PlayTarget {
    /** DECOY: id of the friendly, non-hero board card to swap back to hand. */
    data class DecoyTarget(val boardCardId: String) : PlayTarget()

    /** MEDIC: id of the card in the caster's discard pile to return to hand, if any. */
    data class MedicRevive(val discardCardId: String?) : PlayTarget()
}

const val ROUNDS_TO_WIN = 2
const val INITIAL_HAND_SIZE = 10
const val ROUND_DRAW = 2

object GameEngine {

    fun newMatch(factionA: Faction, factionB: Faction, rng: Random = Random.Default): GameState {
        val a = PlayerState(Side.A, factionA, CardDatabase.deckFor(factionA).shuffled(rng).toMutableList())
        val b = PlayerState(Side.B, factionB, CardDatabase.deckFor(factionB).shuffled(rng).toMutableList())
        val state = GameState(a, b)
        drawCards(a, INITIAL_HAND_SIZE)
        drawCards(b, INITIAL_HAND_SIZE)
        return state
    }

    /** Effective power of a card given the row it sits in and that row's current contents. */
    fun effectivePower(state: GameState, card: Card, row: Row, rowCards: List<Card>): Int {
        if (card.isHero) return card.basePower
        val weathered = row in state.weatheredRows
        val base = if (weathered) 1 else card.basePower
        val horned = rowCards.any { it.ability == Ability.HORN }
        return if (horned) base * 2 else base
    }

    fun rowPower(state: GameState, side: Side, row: Row): Int {
        val rowCards = state.player(side).board.getValue(row)
        return rowCards.sumOf { effectivePower(state, it, row, rowCards) }
    }

    fun totalPower(state: GameState, side: Side): Int =
        Row.entries.sumOf { rowPower(state, side, it) }

    fun playCard(state: GameState, side: Side, cardId: String, target: PlayTarget? = null): List<GameEvent> {
        if (state.matchWinner != null) return listOf(GameEvent.InvalidMove("Match is already over"))
        if (state.turn != side) return listOf(GameEvent.InvalidMove("Not this player's turn"))
        val player = state.player(side)
        if (player.passed) return listOf(GameEvent.InvalidMove("Player has already passed this round"))
        val card = player.hand.find { it.id == cardId }
            ?: return listOf(GameEvent.InvalidMove("Card not in hand: $cardId"))

        val events = mutableListOf<GameEvent>()
        when (card.ability) {
            Ability.DECOY -> {
                val decoyTarget = target as? PlayTarget.DecoyTarget
                    ?: return listOf(GameEvent.InvalidMove("Decoy requires a target card"))
                val boardCard = findOnBoard(player, decoyTarget.boardCardId)
                    ?: return listOf(GameEvent.InvalidMove("Target is not on your board"))
                if (boardCard.isHero) return listOf(GameEvent.InvalidMove("Heroes are immune to Decoy"))

                player.hand.remove(card)
                player.board.getValue(boardCard.row).remove(boardCard)
                player.hand.add(boardCard)
                val decoy = card.copy(row = boardCard.row, basePower = 0, ability = Ability.NONE, isHero = false)
                player.board.getValue(boardCard.row).add(decoy)
                events += GameEvent.CardPlayed(side, card)
                events += GameEvent.Decoyed(side, boardCard, decoy)
            }

            Ability.WEATHER -> {
                player.hand.remove(card)
                player.discard.add(card)
                state.weatheredRows.add(card.row)
                events += GameEvent.CardPlayed(side, card)
                events += GameEvent.WeatherChanged(side, card.row)
            }

            Ability.CLEAR_WEATHER -> {
                player.hand.remove(card)
                player.discard.add(card)
                state.weatheredRows.clear()
                events += GameEvent.CardPlayed(side, card)
                events += GameEvent.WeatherCleared(side)
            }

            Ability.SPY -> {
                player.hand.remove(card)
                state.player(side.other()).board.getValue(card.row).add(card)
                drawCards(player, 2)
                events += GameEvent.CardPlayed(side, card)
                events += GameEvent.SpyInfiltrated(side, card, 2)
            }

            Ability.MEDIC -> {
                player.hand.remove(card)
                player.board.getValue(card.row).add(card)
                events += GameEvent.CardPlayed(side, card)
                val revive = (target as? PlayTarget.MedicRevive)?.discardCardId
                if (revive != null) {
                    val revived = player.discard.find { it.id == revive }
                        ?: return listOf(GameEvent.InvalidMove("Card not in discard: $revive"))
                    player.discard.remove(revived)
                    player.hand.add(revived)
                    events += GameEvent.MedicRevived(side, revived)
                }
            }

            Ability.SCORCH -> {
                player.hand.remove(card)
                player.board.getValue(card.row).add(card)
                events += GameEvent.CardPlayed(side, card)
                events += applyScorch(state)
            }

            Ability.NONE, Ability.HORN -> {
                player.hand.remove(card)
                player.board.getValue(card.row).add(card)
                events += GameEvent.CardPlayed(side, card)
            }
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

    private fun findOnBoard(player: PlayerState, cardId: String): Card? =
        Row.entries.firstNotNullOfOrNull { row -> player.board.getValue(row).find { it.id == cardId } }

    private fun drawCards(player: PlayerState, n: Int) {
        repeat(n) {
            if (player.deck.isNotEmpty()) player.hand.add(player.deck.removeAt(player.deck.lastIndex))
        }
    }

    private fun applyScorch(state: GameState): GameEvent.Scorched {
        val all = allBoardCards(state).filter { !it.isHero }
        if (all.isEmpty()) return GameEvent.Scorched(emptyList())
        val highest = all.maxOf { c ->
            val owner = state.player(sideOf(state, c))
            effectivePower(state, c, c.row, owner.board.getValue(c.row))
        }
        val destroyed = all.filter { c ->
            val owner = state.player(sideOf(state, c))
            effectivePower(state, c, c.row, owner.board.getValue(c.row)) == highest
        }
        destroyed.forEach { c ->
            val owner = state.player(sideOf(state, c))
            owner.board.getValue(c.row).remove(c)
            owner.discard.add(c)
        }
        return GameEvent.Scorched(destroyed)
    }

    private fun allBoardCards(state: GameState): List<Card> =
        Row.entries.flatMap { state.playerA.board.getValue(it) } +
            Row.entries.flatMap { state.playerB.board.getValue(it) }

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

        // Board clears between rounds: every card in play is discarded, like a fresh battlefield.
        discardBoard(state.playerA)
        discardBoard(state.playerB)
        state.weatheredRows.clear()

        state.starter = winner?.other() ?: state.starter
        state.round++
        state.turn = state.starter
        state.playerA.passed = false
        state.playerB.passed = false
        drawCards(state.playerA, ROUND_DRAW)
        drawCards(state.playerB, ROUND_DRAW)
        events += GameEvent.RoundStarted(state.round)
        return events
    }

    private fun discardBoard(player: PlayerState) {
        Row.entries.forEach { row ->
            val cards = player.board.getValue(row)
            player.discard.addAll(cards)
            cards.clear()
        }
    }
}
