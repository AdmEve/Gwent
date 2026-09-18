package ir.gwent.core.engine

import ir.gwent.core.model.CardDatabase
import ir.gwent.core.model.Faction
import ir.gwent.core.model.FactionTrait
import ir.gwent.core.model.GameState
import ir.gwent.core.model.PlayerState
import ir.gwent.core.model.Row
import ir.gwent.core.model.Side
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class AbilitiesTest {

    private fun testMatch(
        factionA: Faction = Faction.PAHLAVAN,
        factionB: Faction = Faction.DIV,
    ): GameState {
        val a = PlayerState(Side.A, factionA, mutableListOf(), CardDatabase.deckFor(factionA).toMutableList())
        val b = PlayerState(Side.B, factionB, mutableListOf(), CardDatabase.deckFor(factionB).toMutableList())
        return GameState(a, b, Random(1))
    }

    @Test
    fun `tight bond multiplies each copy by how many stand together`() {
        val state = testMatch()
        GameEngine.playCard(state, Side.A, "pah-guard-1") // 3 alone
        assertEquals(3, GameEngine.rowPower(state, Side.A, Row.MELEE))

        GameEngine.pass(state, Side.B)
        GameEngine.playCard(state, Side.A, "pah-guard-2") // two copies -> 3*2 each
        assertEquals(12, GameEngine.rowPower(state, Side.A, Row.MELEE))

        GameEngine.playCard(state, Side.A, "pah-guard-3") // three copies -> 3*3 each
        assertEquals(27, GameEngine.rowPower(state, Side.A, Row.MELEE))
    }

    @Test
    fun `muster calls the rest of the group out of the deck`() {
        val a = PlayerState(
            Side.A,
            Faction.PAHLAVAN,
            deck = CardDatabase.pahlavan.filter { it.id == "pah-archer-2" }.toMutableList(),
            hand = CardDatabase.pahlavan.filter { it.id == "pah-archer-1" }.toMutableList(),
        )
        val b = PlayerState(Side.B, Faction.DIV, mutableListOf(), CardDatabase.div.toMutableList())
        val state = GameState(a, b, Random(1))

        val events = GameEngine.playCard(state, Side.A, "pah-archer-1")

        val mustered = events.filterIsInstance<GameEvent.Mustered>().single()
        assertEquals(listOf("pah-archer-2"), mustered.called.map { it.id })
        assertEquals(2, state.playerA.board.getValue(Row.RANGED).size)
        assertTrue(state.playerA.deck.isEmpty(), "the called copy should have left the deck")
    }

    @Test
    fun `a leader ability can only be used once and costs the turn`() {
        val state = testMatch(Faction.MARVEL, Faction.DIV) // Marvel leader draws a card
        val handBefore = state.playerA.hand.size
        // Give the deck something to draw.
        state.playerA.deck.add(CardDatabase.marvel.first())

        assertTrue(GameEngine.canUseLeader(state, Side.A))
        GameEngine.useLeader(state, Side.A)

        assertEquals(handBefore + 1, state.playerA.hand.size)
        assertTrue(state.playerA.leaderUsed)
        assertFalse(GameEngine.canUseLeader(state, Side.A))
        assertEquals(Side.B, state.turn, "using the leader should pass the turn")

        val second = GameEngine.useLeader(state, Side.A)
        assertTrue(second.single() is GameEvent.InvalidMove)
    }

    @Test
    fun `the serpent king's leader destroys only the enemy's strongest unit`() {
        val state = testMatch(Faction.DIV, Faction.PAHLAVAN) // A is Div: scorch enemy strongest
        GameEngine.playCard(state, Side.A, "div-akvan") // 6, ours, must survive
        GameEngine.playCard(state, Side.B, "pah-arash") // 8, theirs, must die

        val events = GameEngine.useLeader(state, Side.A)

        val scorched = events.filterIsInstance<GameEvent.Scorched>().single()
        assertEquals(listOf("pah-arash"), scorched.destroyed.map { it.id })
        assertTrue(state.playerA.board.getValue(Row.MELEE).any { it.id == "div-akvan" })
    }

    @Test
    fun `winning ties is a faction trait, not a rule`() {
        // Marvel wins level rounds; Div does not.
        val state = testMatch(Faction.MARVEL, Faction.DIV)
        GameEngine.pass(state, Side.A)
        GameEngine.pass(state, Side.B)

        assertEquals(1, state.playerA.roundsWon, "Marvel should take the level round")
        assertEquals(2, state.playerA.lives, "and keep both gems")
        assertEquals(1, state.playerB.lives)
    }

    @Test
    fun `mulligan swaps a card back into the deck for another`() {
        val state = GameEngine.newMatch(Faction.MARVEL, Faction.DIV, Random(3))
        val victim = state.playerA.hand.first()
        val handSize = state.playerA.hand.size
        val deckSize = state.playerA.deck.size

        val events = GameEngine.mulligan(state, Side.A, victim.id, Random(5))

        assertTrue(events.single() is GameEvent.Mulliganed)
        assertEquals(handSize, state.playerA.hand.size, "hand size should be unchanged")
        assertEquals(deckSize, state.playerA.deck.size, "deck size should be unchanged")
        assertTrue(state.playerA.deck.any { it.id == victim.id }, "the swapped card goes back to the deck")
        assertEquals(1, state.playerA.mulligansLeft)
    }

    @Test
    fun `mulligans run out and are refused once the match has begun`() {
        val state = GameEngine.newMatch(Faction.MARVEL, Faction.DIV, Random(3))
        repeat(2) { GameEngine.mulligan(state, Side.A, state.playerA.hand.first().id, Random(it)) }
        assertEquals(0, state.playerA.mulligansLeft)

        val refused = GameEngine.mulligan(state, Side.A, state.playerA.hand.first().id)
        assertTrue(refused.single() is GameEvent.InvalidMove)
    }

    @Test
    fun `the monsters-style trait keeps one unit on the board between rounds`() {
        // Div keeps a random unit; Pahlavan does not.
        val state = testMatch(Faction.DIV, Faction.PAHLAVAN)
        GameEngine.playCard(state, Side.A, "div-akvan")
        GameEngine.playCard(state, Side.B, "pah-giv")
        GameEngine.pass(state, Side.A)
        GameEngine.pass(state, Side.B)

        val divUnits = Row.entries.sumOf { state.playerA.board.getValue(it).size }
        val pahUnits = Row.entries.sumOf { state.playerB.board.getValue(it).size }
        assertEquals(1, divUnits, "the Div should hold one unit over")
        assertEquals(0, pahUnits, "the Pahlavans should not")
        assertEquals(FactionTrait.KEEP_RANDOM_UNIT, state.playerA.trait)
    }
}
