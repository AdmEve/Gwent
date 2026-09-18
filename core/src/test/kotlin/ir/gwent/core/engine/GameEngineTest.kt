package ir.gwent.core.engine

import ir.gwent.core.model.Ability
import ir.gwent.core.model.CardDatabase
import ir.gwent.core.model.Faction
import ir.gwent.core.model.GameState
import ir.gwent.core.model.PlayerState
import ir.gwent.core.model.Row
import ir.gwent.core.model.Side
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameEngineTest {

    /** Deals the whole deck straight into hand (no shuffle) so tests can play cards by id deterministically. */
    private fun testMatch(factionA: Faction = Faction.PAHLAVAN, factionB: Faction = Faction.DIV): GameState {
        val a = PlayerState(Side.A, factionA, mutableListOf(), CardDatabase.deckFor(factionA).toMutableList())
        val b = PlayerState(Side.B, factionB, mutableListOf(), CardDatabase.deckFor(factionB).toMutableList())
        return GameState(a, b)
    }

    @Test
    fun `playing a card moves it from hand to board and flips the turn`() {
        val state = testMatch()
        val card = state.playerA.hand.first()

        GameEngine.playCard(state, Side.A, card.id)

        assertTrue(state.playerA.hand.none { it.id == card.id })
        assertEquals(card.id, state.playerA.board.getValue(card.row).single().id)
        assertEquals(Side.B, state.turn)
    }

    @Test
    fun `wrong player's turn is rejected`() {
        val state = testMatch()
        val card = state.playerB.hand.first()

        val events = GameEngine.playCard(state, Side.B, card.id)

        assertTrue(events.single() is GameEvent.InvalidMove)
        assertTrue(state.playerB.hand.any { it.id == card.id })
    }

    @Test
    fun `both players passing resolves the round for the higher total`() {
        val state = testMatch()
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
    fun `board clears to discard between rounds`() {
        val state = testMatch()
        GameEngine.playCard(state, Side.A, "pah-rostam")
        GameEngine.pass(state, Side.B)
        GameEngine.pass(state, Side.A)

        assertTrue(Row.entries.all { state.playerA.board.getValue(it).isEmpty() })
        assertTrue(state.playerA.discard.any { it.id == "pah-rostam" })
    }

    @Test
    fun `horn doubles non-hero units in its row`() {
        val state = testMatch()
        GameEngine.playCard(state, Side.A, "pah-piran") // 5 power, SIEGE
        GameEngine.pass(state, Side.B)
        GameEngine.playCard(state, Side.A, "pah-kaveh") // HORN, SIEGE, 3 power

        // Piran (5 -> 10) and Kaveh itself (3 -> 6) are both doubled by the horn's presence.
        val siegeTotal = GameEngine.rowPower(state, Side.A, Row.SIEGE)
        assertEquals(16, siegeTotal)
    }

    @Test
    fun `scorch destroys the highest power non-hero unit board-wide`() {
        val state = testMatch()
        GameEngine.playCard(state, Side.A, "pah-piran") // 5 power
        GameEngine.playCard(state, Side.B, "div-arzhang") // 5 power, ties with Piran
        val events = GameEngine.playCard(state, Side.A, "pah-simurgh") // SCORCH

        val scorched = events.filterIsInstance<GameEvent.Scorched>().single()
        assertEquals(setOf("pah-piran", "div-arzhang"), scorched.destroyed.map { it.id }.toSet())
        assertTrue(state.playerA.discard.any { it.id == "pah-piran" })
    }

    @Test
    fun `hero cards are immune to scorch`() {
        val state = testMatch()
        GameEngine.playCard(state, Side.A, "pah-rostam") // hero, 10 power
        GameEngine.playCard(state, Side.B, "div-zahhak") // hero, 9 power
        val events = GameEngine.playCard(state, Side.A, "pah-simurgh") // SCORCH

        val scorched = events.filterIsInstance<GameEvent.Scorched>().single()
        assertTrue(scorched.destroyed.none { it.isHero })
    }

    @Test
    fun `weather caps non-hero row power at 1`() {
        val state = testMatch()
        GameEngine.playCard(state, Side.A, "pah-piran") // 5 power, SIEGE
        GameEngine.pass(state, Side.B)
        val events = GameEngine.playCard(state, Side.A, "pah-flood") // WEATHER, SIEGE

        assertTrue(events.any { it is GameEvent.WeatherChanged })
        assertEquals(1, GameEngine.rowPower(state, Side.A, Row.SIEGE))
    }

    @Test
    fun `hero cards ignore weather`() {
        val state = testMatch()
        GameEngine.playCard(state, Side.A, "pah-rostam") // hero, MELEE
        GameEngine.pass(state, Side.B)
        GameEngine.playCard(state, Side.A, "pah-blizzard") // WEATHER, MELEE

        assertEquals(10, GameEngine.rowPower(state, Side.A, Row.MELEE))
    }

    @Test
    fun `clear weather removes all active weather`() {
        val state = testMatch()
        GameEngine.playCard(state, Side.A, "pah-blizzard") // WEATHER, MELEE, turn -> B
        GameEngine.pass(state, Side.B) // turn -> A
        GameEngine.playCard(state, Side.A, "pah-fog") // WEATHER, RANGED
        assertEquals(setOf(Row.MELEE, Row.RANGED), state.weatheredRows)

        GameEngine.playCard(state, Side.A, "pah-sorush") // CLEAR_WEATHER
        assertTrue(state.weatheredRows.isEmpty())
    }

    @Test
    fun `decoy swaps a board unit back to hand for a 0-power dummy`() {
        val state = testMatch()
        GameEngine.playCard(state, Side.A, "pah-piran") // 5 power, SIEGE, turn -> B
        GameEngine.pass(state, Side.B) // turn -> A
        val events = GameEngine.playCard(state, Side.A, "pah-manijeh", PlayTarget.DecoyTarget("pah-piran"))

        val decoyed = events.filterIsInstance<GameEvent.Decoyed>().single()
        assertEquals("pah-piran", decoyed.returned.id)
        assertTrue(state.playerA.hand.any { it.id == "pah-piran" })
        val siege = state.playerA.board.getValue(Row.SIEGE)
        assertEquals(1, siege.size)
        assertEquals(0, siege.single().basePower)
    }

    @Test
    fun `decoy cannot target a hero`() {
        val state = testMatch()
        GameEngine.playCard(state, Side.A, "pah-rostam") // hero
        GameEngine.pass(state, Side.B)
        val events = GameEngine.playCard(state, Side.A, "pah-manijeh", PlayTarget.DecoyTarget("pah-rostam"))

        assertTrue(events.single() is GameEvent.InvalidMove)
        assertTrue(state.playerA.board.getValue(Row.MELEE).any { it.id == "pah-rostam" })
    }

    @Test
    fun `medic can revive a card from discard`() {
        val state = testMatch()
        GameEngine.playCard(state, Side.A, "pah-piran") // 5 power, only non-hero on board, turn -> B
        GameEngine.pass(state, Side.B) // turn -> A
        GameEngine.playCard(state, Side.A, "pah-simurgh") // SCORCH kills Piran; turn stays A (B already passed)

        val events = GameEngine.playCard(state, Side.A, "pah-zal", PlayTarget.MedicRevive("pah-piran"))

        val revived = events.filterIsInstance<GameEvent.MedicRevived>().single()
        assertEquals("pah-piran", revived.revived.id)
        assertTrue(state.playerA.hand.any { it.id == "pah-piran" })
        assertTrue(state.playerA.discard.none { it.id == "pah-piran" })
    }

    @Test
    fun `spy places the unit on the opponent's board and draws from the caster's own deck`() {
        val gordafarid = CardDatabase.pahlavan.first { it.id == "pah-gordafarid" }
        val restOfDeck = CardDatabase.pahlavan.filter { it.id != "pah-gordafarid" }.take(2)
        val a = PlayerState(Side.A, Faction.PAHLAVAN, restOfDeck.toMutableList(), mutableListOf(gordafarid))
        val b = PlayerState(Side.B, Faction.DIV, CardDatabase.div.toMutableList())
        val state = GameState(a, b)

        val events = GameEngine.playCard(state, Side.A, "pah-gordafarid")

        assertTrue(events.any { it is GameEvent.SpyInfiltrated })
        assertTrue(state.playerB.board.getValue(Row.RANGED).any { it.id == "pah-gordafarid" })
        assertTrue(state.playerA.board.getValue(Row.RANGED).none { it.id == "pah-gordafarid" })
        assertEquals(2, state.playerA.hand.size)
        assertTrue(state.playerA.deck.isEmpty())
    }

    @Test
    fun `match ends after a player wins two rounds`() {
        // B always passes immediately; A plays its highest-power playable card. A should sweep 2-0.
        val state = testMatch()
        while (!state.matchOver) {
            if (state.turn == Side.A) {
                // Decoy needs a target we're not supplying here, so skip it to avoid a stuck InvalidMove loop.
                val card = state.player(Side.A).hand.filter { it.ability != Ability.DECOY }.maxByOrNull { it.basePower }
                if (card != null) GameEngine.playCard(state, Side.A, card.id) else GameEngine.pass(state, Side.A)
            } else {
                GameEngine.pass(state, Side.B)
            }
        }
        assertEquals(Side.A, state.matchWinner)
    }
}
