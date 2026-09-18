package ir.gwent.core.engine

import ir.gwent.core.model.Faction
import ir.gwent.core.model.Side
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameEngineTest {

    @Test
    fun `playing a card moves it from hand to board and flips the turn`() {
        val state = GameEngine.newMatch(Faction.PAHLAVAN, Faction.DIV)
        val card = state.playerA.hand.first()

        GameEngine.playCard(state, Side.A, card.id)

        assertTrue(state.playerA.hand.none { it.id == card.id })
        assertEquals(card.id, state.playerA.board.getValue(card.row).single().id)
        assertEquals(Side.B, state.turn)
    }

    @Test
    fun `wrong player's turn is rejected`() {
        val state = GameEngine.newMatch(Faction.PAHLAVAN, Faction.DIV)
        val card = state.playerB.hand.first()

        val events = GameEngine.playCard(state, Side.B, card.id)

        assertTrue(events.single() is GameEvent.InvalidMove)
        assertTrue(state.playerB.hand.any { it.id == card.id })
    }

    @Test
    fun `both players passing resolves the round for the higher total`() {
        val state = GameEngine.newMatch(Faction.PAHLAVAN, Faction.DIV)
        // Rostam (10 power) beats anything the Div can put down with a single card.
        GameEngine.playCard(state, Side.A, "pah-rostam")
        GameEngine.pass(state, Side.B)
        val events = GameEngine.pass(state, Side.A)

        val roundEnded = events.filterIsInstance<GameEvent.RoundEnded>().single()
        assertEquals(Side.A, roundEnded.result.winner)
        assertEquals(1, state.playerA.roundsWon)
        assertEquals(2, state.round) // next round started
    }

    @Test
    fun `horn doubles non-hero units in its row`() {
        val state = GameEngine.newMatch(Faction.PAHLAVAN, Faction.DIV)
        GameEngine.playCard(state, Side.A, "pah-zal") // 6 power, SIEGE
        GameEngine.pass(state, Side.B)
        GameEngine.playCard(state, Side.A, "pah-kaveh") // HORN, SIEGE

        // Zal (6 -> 12) and Kaveh itself (3 -> 6) are both doubled by the horn's presence.
        val siegeTotal = GameEngine.rowPower(state, Side.A, ir.gwent.core.model.Row.SIEGE)
        assertEquals(18, siegeTotal)
    }

    @Test
    fun `scorch destroys the highest power non-hero unit board-wide`() {
        val state = GameEngine.newMatch(Faction.PAHLAVAN, Faction.DIV)
        GameEngine.playCard(state, Side.A, "pah-zal") // 6 power
        GameEngine.playCard(state, Side.B, "div-nahang") // 6 power, ties with Zal
        val events = GameEngine.playCard(state, Side.A, "pah-simurgh") // SCORCH

        val scorched = events.filterIsInstance<GameEvent.Scorched>().single()
        assertEquals(setOf("pah-zal", "div-nahang"), scorched.destroyed.map { it.id }.toSet())
    }

    @Test
    fun `hero cards are immune to scorch`() {
        val state = GameEngine.newMatch(Faction.PAHLAVAN, Faction.DIV)
        GameEngine.playCard(state, Side.A, "pah-rostam") // hero, 10 power
        GameEngine.playCard(state, Side.B, "div-zahhak") // hero, 9 power
        val events = GameEngine.playCard(state, Side.A, "pah-simurgh") // SCORCH

        val scorched = events.filterIsInstance<GameEvent.Scorched>().single()
        assertTrue(scorched.destroyed.none { it.isHero })
    }

    @Test
    fun `match ends after a player wins two rounds`() {
        // B always passes immediately; A plays every card it has. A should sweep 2-0.
        val state = GameEngine.newMatch(Faction.PAHLAVAN, Faction.DIV)
        while (state.matchWinner == null) {
            if (state.turn == Side.A) {
                val card = state.player(Side.A).hand.maxByOrNull { it.basePower }
                if (card != null) GameEngine.playCard(state, Side.A, card.id) else GameEngine.pass(state, Side.A)
            } else {
                GameEngine.pass(state, Side.B)
            }
        }
        assertEquals(Side.A, state.matchWinner)
    }
}
