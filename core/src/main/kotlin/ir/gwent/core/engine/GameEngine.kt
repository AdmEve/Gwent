package ir.gwent.core.engine

import ir.gwent.core.model.Ability
import ir.gwent.core.model.Card
import ir.gwent.core.model.CardDatabase
import ir.gwent.core.model.Faction
import ir.gwent.core.model.FactionTrait
import ir.gwent.core.model.GameState
import ir.gwent.core.model.Leader
import ir.gwent.core.model.LeaderAbility
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
    data class Mustered(val side: Side, val called: List<Card>) : GameEvent()
    data class LeaderUsed(val side: Side, val leader: Leader) : GameEvent()
    data class Mulliganed(val side: Side, val returned: Card, val drawn: Card?) : GameEvent()
    data class TraitTriggered(val side: Side, val trait: FactionTrait) : GameEvent()
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

/** Cards drawn at the start of a match. As in Gwent, this hand has to last all three rounds. */
const val INITIAL_HAND_SIZE = 10

object GameEngine {

    fun newMatch(factionA: Faction, factionB: Faction, rng: Random = Random.Default): GameState {
        val a = PlayerState(Side.A, factionA, CardDatabase.deckFor(factionA).shuffled(rng).toMutableList())
        val b = PlayerState(Side.B, factionB, CardDatabase.deckFor(factionB).shuffled(rng).toMutableList())
        val state = GameState(a, b, rng)
        drawCards(a, INITIAL_HAND_SIZE)
        drawCards(b, INITIAL_HAND_SIZE)

        // A coin toss opens the match, unless exactly one army always takes the first move.
        val aOpens = a.trait == FactionTrait.ALWAYS_OPENS
        val bOpens = b.trait == FactionTrait.ALWAYS_OPENS
        state.starter = when {
            aOpens && !bOpens -> Side.A
            bOpens && !aOpens -> Side.B
            else -> if (rng.nextBoolean()) Side.A else Side.B
        }
        state.turn = state.starter
        return state
    }

    /**
     * Effective power of a card given the row it sits in and that row's current contents.
     * Order matters and follows Gwent: weather floors the value, Tight Bond multiplies it by
     * the number of copies standing together, then a Horn doubles the result.
     */
    fun effectivePower(state: GameState, card: Card, row: Row, rowCards: List<Card>): Int {
        if (card.isHero) return card.basePower
        val weathered = row in state.weatheredRows
        var power = if (weathered) 1 else card.basePower
        card.bondGroup?.let { group ->
            val copies = rowCards.count { it.bondGroup == group }
            if (copies > 1) power *= copies
        }
        if (rowCards.any { it.ability == Ability.HORN }) power *= 2
        return power
    }

    fun rowPower(state: GameState, side: Side, row: Row): Int {
        val rowCards = state.player(side).board.getValue(row)
        return rowCards.sumOf { effectivePower(state, it, row, rowCards) }
    }

    fun totalPower(state: GameState, side: Side): Int =
        Row.entries.sumOf { rowPower(state, side, it) }

    /**
     * Why this move would be rejected, or null if it is legal. Exposed so callers that drive
     * turns in a loop (the AI runner in the UI) can never pick a move that changes nothing and
     * spin forever.
     */
    fun rejectionReason(state: GameState, side: Side, cardId: String, target: PlayTarget?): String? {
        if (state.matchOver) return "Match is already over"
        if (state.turn != side) return "Not this player's turn"
        val player = state.player(side)
        if (player.passed) return "Player has already passed this round"
        val card = player.hand.find { it.id == cardId } ?: return "Card not in hand: $cardId"

        when (card.ability) {
            Ability.DECOY -> {
                val decoyTarget = target as? PlayTarget.DecoyTarget ?: return "Decoy requires a target card"
                val boardCard = findOnBoard(player, decoyTarget.boardCardId) ?: return "Target is not on your board"
                if (boardCard.isHero) return "Heroes are immune to Decoy"
            }
            Ability.MEDIC -> {
                val reviveId = (target as? PlayTarget.MedicRevive)?.discardCardId
                if (reviveId != null && player.discard.none { it.id == reviveId }) {
                    return "Card not in discard: $reviveId"
                }
            }
            else -> Unit
        }
        return null
    }

    fun canPass(state: GameState, side: Side): Boolean =
        !state.matchOver && state.turn == side && !state.player(side).passed

    fun playCard(state: GameState, side: Side, cardId: String, target: PlayTarget? = null): List<GameEvent> {
        rejectionReason(state, side, cardId, target)?.let { return listOf(GameEvent.InvalidMove(it)) }

        val player = state.player(side)
        val card = player.hand.first { it.id == cardId }
        val events = mutableListOf<GameEvent>()

        when (card.ability) {
            Ability.DECOY -> {
                val boardCard = findOnBoard(player, (target as PlayTarget.DecoyTarget).boardCardId)!!
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
                val drawn = drawCards(player, 2)
                events += GameEvent.CardPlayed(side, card)
                events += GameEvent.SpyInfiltrated(side, card, drawn)
            }

            Ability.MEDIC -> {
                val reviveId = (target as? PlayTarget.MedicRevive)?.discardCardId
                player.hand.remove(card)
                player.board.getValue(card.row).add(card)
                events += GameEvent.CardPlayed(side, card)
                if (reviveId != null) {
                    val revived = player.discard.first { it.id == reviveId }
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

        events += applyMuster(state, side, card)
        advanceTurn(state)
        events += maybeResolveRound(state)
        return events
    }

    fun pass(state: GameState, side: Side): List<GameEvent> {
        if (state.matchOver) return listOf(GameEvent.InvalidMove("Match is already over"))
        if (state.turn != side) return listOf(GameEvent.InvalidMove("Not this player's turn"))
        val player = state.player(side)
        if (player.passed) return listOf(GameEvent.InvalidMove("Player has already passed this round"))

        player.passed = true
        val events = mutableListOf<GameEvent>(GameEvent.Passed(side))
        advanceTurn(state)
        events += maybeResolveRound(state)
        return events
    }

    /** Muster: the played card calls every other copy of its group out of the deck and hand. */
    private fun applyMuster(state: GameState, side: Side, played: Card): List<GameEvent> {
        val group = played.musterGroup ?: return emptyList()
        val player = state.player(side)
        val called = (player.deck + player.hand).filter { it.musterGroup == group && it.id != played.id }
        if (called.isEmpty()) return emptyList()
        called.forEach { card ->
            player.deck.remove(card)
            player.hand.remove(card)
            player.board.getValue(card.row).add(card)
        }
        return listOf(GameEvent.Mustered(side, called))
    }

    /** Swap a card back into the deck for a fresh one, before the match begins. */
    fun mulligan(state: GameState, side: Side, cardId: String, rng: Random = Random.Default): List<GameEvent> {
        if (state.round != 1 || state.roundHistory.isNotEmpty()) {
            return listOf(GameEvent.InvalidMove("Cards can only be swapped before the match starts"))
        }
        val player = state.player(side)
        if (player.mulligansLeft <= 0) return listOf(GameEvent.InvalidMove("No swaps left"))
        val card = player.hand.find { it.id == cardId }
            ?: return listOf(GameEvent.InvalidMove("Card not in hand: $cardId"))
        if (player.deck.isEmpty()) return listOf(GameEvent.InvalidMove("Deck is empty"))

        player.hand.remove(card)
        val replacement = player.deck.removeAt(rng.nextInt(player.deck.size))
        player.hand.add(replacement)
        player.deck.add(card)
        player.mulligansLeft--
        return listOf(GameEvent.Mulliganed(side, card, replacement))
    }

    fun canUseLeader(state: GameState, side: Side): Boolean =
        !state.matchOver && state.turn == side && !state.player(side).passed && !state.player(side).leaderUsed

    /** Leader abilities are once per match and cost the turn, as playing a card would. */
    fun useLeader(state: GameState, side: Side): List<GameEvent> {
        if (state.matchOver) return listOf(GameEvent.InvalidMove("Match is already over"))
        if (state.turn != side) return listOf(GameEvent.InvalidMove("Not this player's turn"))
        val player = state.player(side)
        if (player.passed) return listOf(GameEvent.InvalidMove("Player has already passed this round"))
        if (player.leaderUsed) return listOf(GameEvent.InvalidMove("Leader ability already used"))

        player.leaderUsed = true
        val events = mutableListOf<GameEvent>(GameEvent.LeaderUsed(side, player.leader))

        when (player.leader.ability) {
            LeaderAbility.CLEAR_ALL_WEATHER -> {
                state.weatheredRows.clear()
                events += GameEvent.WeatherCleared(side)
            }
            LeaderAbility.DRAW_CARD -> {
                drawCards(player, 1)
            }
            LeaderAbility.SCORCH_ENEMY_STRONGEST -> {
                val enemy = state.player(side.other())
                val candidates = Row.entries.flatMap { enemy.board.getValue(it) }.filter { !it.isHero }
                if (candidates.isNotEmpty()) {
                    val highest = candidates.maxOf { c ->
                        effectivePower(state, c, c.row, enemy.board.getValue(c.row))
                    }
                    val destroyed = candidates.filter { c ->
                        effectivePower(state, c, c.row, enemy.board.getValue(c.row)) == highest
                    }
                    destroyed.forEach { c ->
                        enemy.board.getValue(c.row).remove(c)
                        enemy.discard.add(c)
                    }
                    events += GameEvent.Scorched(destroyed)
                }
            }
            LeaderAbility.HORN_STRONGEST_ROW -> {
                val best = Row.entries.maxByOrNull { rowPower(state, side, it) }
                if (best != null && player.board.getValue(best).isNotEmpty()) {
                    val banner = Card(
                        id = "${player.leader.id}-banner",
                        name = "${player.leader.name}'s Banner",
                        faction = player.faction,
                        row = best,
                        basePower = 0,
                        ability = Ability.HORN,
                    )
                    player.board.getValue(best).add(banner)
                    events += GameEvent.CardPlayed(side, banner)
                }
            }
            LeaderAbility.WEATHER_ENEMY_STRONGEST_ROW -> {
                val best = Row.entries.maxByOrNull { rowPower(state, side.other(), it) }
                if (best != null) {
                    state.weatheredRows.add(best)
                    events += GameEvent.WeatherChanged(side, best)
                }
            }
        }

        advanceTurn(state)
        events += maybeResolveRound(state)
        return events
    }

    /** Returns how many cards were actually drawn, which can be fewer than asked near deck-out. */
    private fun drawCards(player: PlayerState, n: Int): Int {
        var drawn = 0
        repeat(n) {
            if (player.deck.isNotEmpty()) {
                player.hand.add(player.deck.removeAt(player.deck.lastIndex))
                drawn++
            }
        }
        return drawn
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

    private fun findOnBoard(player: PlayerState, cardId: String): Card? =
        Row.entries.firstNotNullOfOrNull { row -> player.board.getValue(row).find { it.id == cardId } }

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
        val events = mutableListOf<GameEvent>()

        // A level round is lost by both — unless exactly one army wins ties.
        val aWinsTies = state.playerA.trait == FactionTrait.WIN_TIES
        val bWinsTies = state.playerB.trait == FactionTrait.WIN_TIES
        val winner = when {
            powerA > powerB -> Side.A
            powerB > powerA -> Side.B
            aWinsTies && !bWinsTies -> Side.A.also {
                events += GameEvent.TraitTriggered(Side.A, FactionTrait.WIN_TIES)
            }
            bWinsTies && !aWinsTies -> Side.B.also {
                events += GameEvent.TraitTriggered(Side.B, FactionTrait.WIN_TIES)
            }
            else -> null
        }

        when (winner) {
            Side.A -> { state.playerA.roundsWon++; state.playerB.lives-- }
            Side.B -> { state.playerB.roundsWon++; state.playerA.lives-- }
            null -> { state.playerA.lives--; state.playerB.lives-- }
        }

        // The winner's army may draw on a won round.
        winner?.let { side ->
            val p = state.player(side)
            if (p.trait == FactionTrait.DRAW_ON_ROUND_WIN && p.deck.isNotEmpty()) {
                drawCards(p, 1)
                events += GameEvent.TraitTriggered(side, FactionTrait.DRAW_ON_ROUND_WIN)
            }
        }

        val result = RoundResult(state.round, powerA, powerB, winner)
        state.roundHistory.add(result)
        events += GameEvent.RoundEnded(result)

        val aOut = state.playerA.lives <= 0
        val bOut = state.playerB.lives <= 0
        if (aOut || bOut) {
            state.matchOver = true
            state.matchWinner = when {
                aOut && bOut -> null // both ran out in the same round: a drawn match
                aOut -> Side.B
                else -> Side.A
            }
            events += GameEvent.MatchEnded(state.matchWinner)
            return events
        }

        // Board clears between rounds: every card in play is discarded, like a fresh battlefield.
        listOf(state.playerA, state.playerB).forEach { player ->
            val kept = discardBoard(player, state.rng)
            if (kept != null) events += GameEvent.TraitTriggered(player.side, FactionTrait.KEEP_RANDOM_UNIT)
        }
        state.weatheredRows.clear()

        // The player who lost the round opens the next one; after a draw the opener is unchanged.
        state.starter = winner?.other() ?: state.starter
        state.round++
        state.turn = state.starter
        state.playerA.passed = false
        state.playerB.passed = false

        if (state.round == 3) {
            listOf(state.playerA, state.playerB).forEach { player ->
                if (player.trait == FactionTrait.RECOVER_AT_ROUND_THREE && player.discard.isNotEmpty()) {
                    val recovered = player.discard.removeAt(state.rng.nextInt(player.discard.size))
                    player.hand.add(recovered)
                    events += GameEvent.TraitTriggered(player.side, FactionTrait.RECOVER_AT_ROUND_THREE)
                }
            }
        }

        events += GameEvent.RoundStarted(state.round)
        return events
    }

    /** Clears the board to the graveyard, returning the unit held back by a faction trait. */
    private fun discardBoard(player: PlayerState, rng: Random): Card? {
        val onBoard = Row.entries.flatMap { player.board.getValue(it) }
        val keep = if (player.trait == FactionTrait.KEEP_RANDOM_UNIT && onBoard.isNotEmpty()) {
            onBoard.filter { it.basePower > 0 }.randomOrNull(rng)
        } else null

        Row.entries.forEach { row ->
            val cards = player.board.getValue(row)
            player.discard.addAll(cards.filter { it.id != keep?.id })
            cards.retainAll { it.id == keep?.id }
        }
        return keep
    }
}
