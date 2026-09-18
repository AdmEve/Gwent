package ir.gwent.core.engine

import ir.gwent.core.ai.Move
import ir.gwent.core.ai.SimpleAi
import ir.gwent.core.model.Ability
import ir.gwent.core.model.Faction
import ir.gwent.core.model.GameState
import ir.gwent.core.model.Row
import ir.gwent.core.model.Side
import ir.gwent.core.model.other
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

/**
 * Plays complete matches to catch what single-move unit tests miss. The important one: the UI
 * drives the AI with `while (turn == AI && !matchOver)`, so any AI move the engine rejects
 * freezes the app. A draw used to do exactly that, because a finished match with no winner
 * was indistinguishable from a match still in progress.
 */
class MatchSimulationTest {

    private val moveBudget = 4000

    /** Mirrors the UI's AI runner and fails on any move the engine refuses. */
    private fun runAi(state: GameState) {
        var guard = 0
        while (state.turn == Side.B && !state.matchOver) {
            val move = SimpleAi.chooseMove(state, Side.B)
            val events = when (move) {
                is Move.Pass -> GameEngine.pass(state, Side.B)
                is Move.UseLeader -> GameEngine.useLeader(state, Side.B)
                is Move.PlayCard -> GameEngine.playCard(state, Side.B, move.cardId, move.target)
            }
            val invalid = events.filterIsInstance<GameEvent.InvalidMove>()
            assertTrue(
                invalid.isEmpty(),
                "AI chose a move the engine rejected (${invalid.map { it.reason }}); the UI would hang. move=$move",
            )
            guard++
            assertTrue(guard < 500, "AI took an implausible number of consecutive turns")
        }
    }

    /** Plays the human side the way a player tapping around the UI would. */
    private fun humanMove(state: GameState, rng: Random) {
        val me = state.player(Side.A)
        if (me.hand.isEmpty() || rng.nextInt(100) < 12) {
            GameEngine.pass(state, Side.A)
            return
        }
        val card = me.hand.random(rng)
        val target = when (card.ability) {
            Ability.DECOY -> {
                val candidates = Row.entries.flatMap { me.board.getValue(it) }.filter { !it.isHero }
                if (candidates.isEmpty()) null else PlayTarget.DecoyTarget(candidates.random(rng).id)
            }
            Ability.MEDIC -> if (me.discard.isEmpty()) null else PlayTarget.MedicRevive(me.discard.random(rng).id)
            else -> null
        }
        if (GameEngine.rejectionReason(state, Side.A, card.id, target) != null) {
            // The UI would not offer this move; take any legal one instead.
            val legal = me.hand.firstOrNull { GameEngine.rejectionReason(state, Side.A, it.id, null) == null }
            if (legal == null) GameEngine.pass(state, Side.A) else GameEngine.playCard(state, Side.A, legal.id, null)
            return
        }
        GameEngine.playCard(state, Side.A, card.id, target)
    }

    private fun playOut(state: GameState, rng: Random) {
        var moves = 0
        runAi(state)
        while (!state.matchOver) {
            if (state.turn == Side.A) humanMove(state, rng) else runAi(state)
            runAi(state)
            moves++
            assertTrue(moves < moveBudget, "match never finished; round=${state.round} turn=${state.turn}")
        }
    }

    @Test
    fun `every faction pairing plays to a finished match without hanging`() {
        val rng = Random(20260918)
        for (a in Faction.entries) {
            for (b in Faction.entries) {
                repeat(12) {
                    val state = GameEngine.newMatch(a, b, Random(rng.nextInt()))
                    playOut(state, rng)
                    assertTrue(state.matchOver, "$a vs $b ended without matchOver set")
                    assertTrue(state.round <= 3, "match ran past round 3: ${state.round}")
                    assertTrue(
                        state.playerA.lives <= 0 || state.playerB.lives <= 0,
                        "match ended while both players still had gems",
                    )
                }
            }
        }
    }

    @Test
    fun `a finished match reports a winner that actually has gems left, or a draw`() {
        val rng = Random(7)
        repeat(80) {
            val state = GameEngine.newMatch(Faction.MARVEL, Faction.GREEK_MYTH, Random(rng.nextInt()))
            playOut(state, rng)
            when (val winner = state.matchWinner) {
                null -> assertTrue(
                    state.playerA.lives <= 0 && state.playerB.lives <= 0,
                    "a draw should mean both players ran out of gems",
                )
                else -> {
                    assertTrue(state.player(winner).lives > 0, "winner should still hold a gem")
                    assertTrue(state.player(winner.other()).lives <= 0, "loser should be out of gems")
                }
            }
        }
    }

    @Test
    fun `no card is ever duplicated across hand deck board and graveyard`() {
        val rng = Random(99)
        repeat(40) {
            val state = GameEngine.newMatch(Faction.ONE_PIECE, Faction.PAHLAVAN, Random(rng.nextInt()))
            var moves = 0
            while (!state.matchOver && moves < moveBudget) {
                if (state.turn == Side.A) humanMove(state, rng) else runAi(state)
                moves++
                val ids = listOf(state.playerA, state.playerB).flatMap { p ->
                    p.hand + p.deck + p.discard + Row.entries.flatMap { p.board.getValue(it) }
                }.map { it.id }
                assertTrue(
                    ids.size == ids.toSet().size,
                    "a card exists twice: ${ids.groupBy { it }.filterValues { v -> v.size > 1 }.keys}",
                )
            }
        }
    }

    @Test
    fun `the hand has to last the whole match - no cards are drawn between rounds`() {
        val state = GameEngine.newMatch(Faction.MARVEL, Faction.DIV, Random(1))
        val deckAtStart = state.playerA.deck.size
        // Force round one to end without either side drawing.
        GameEngine.pass(state, state.turn)
        GameEngine.pass(state, state.turn)
        assertEquals(2, state.round)
        assertEquals(deckAtStart, state.playerA.deck.size, "players should not draw between rounds")
    }
}
