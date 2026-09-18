package ir.gwent.core.engine

import ir.gwent.core.model.CardDatabase
import ir.gwent.core.model.Faction
import ir.gwent.core.model.GameState
import ir.gwent.core.model.PlayerState
import ir.gwent.core.model.Side
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Regression cover for the bug that froze the app: a match that ends level has no winner, and
 * the old code signalled "finished" only by setting a winner. The UI then kept asking a player
 * who had already passed to move, in a loop, on the main thread.
 */
class DrawnMatchTest {

    private fun emptyHandedMatch(): GameState {
        // No cards anywhere, so every round is a 0-0 tie.
        val a = PlayerState(Side.A, Faction.MARVEL, mutableListOf())
        val b = PlayerState(Side.B, Faction.DIV, mutableListOf())
        return GameState(a, b)
    }

    /** Both sides pass, so the round is a 0-0 tie and costs each of them a gem. */
    private fun passRound(state: GameState) {
        GameEngine.pass(state, state.turn)
        GameEngine.pass(state, state.turn)
    }

    @Test
    fun `a level match is marked over and reports no winner`() {
        val state = emptyHandedMatch()
        passRound(state) // 2 gems -> 1 each
        assertFalse(state.matchOver, "one tied round should not end the match")
        passRound(state) // 1 gem -> 0 each

        assertTrue(state.matchOver, "a drawn match must be marked over")
        assertNull(state.matchWinner, "a drawn match has no winner")
    }

    @Test
    fun `a finished match refuses further moves instead of silently doing nothing`() {
        val state = emptyHandedMatch()
        passRound(state)
        passRound(state)

        val events = GameEngine.pass(state, Side.A)
        assertTrue(events.single() is GameEvent.InvalidMove)
        assertFalse(GameEngine.canPass(state, Side.A), "canPass must be false once the match is over")
    }

    @Test
    fun `a tied round costs both players a gem`() {
        val a = PlayerState(Side.A, Faction.MARVEL, mutableListOf(), CardDatabase.marvel.toMutableList())
        val b = PlayerState(Side.B, Faction.DIV, mutableListOf(), CardDatabase.div.toMutableList())
        val state = GameState(a, b)

        passRound(state)

        assertTrue(state.playerA.lives == 1 && state.playerB.lives == 1, "both sides should be down to one gem")
    }
}
