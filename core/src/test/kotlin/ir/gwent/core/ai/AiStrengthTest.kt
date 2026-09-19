package ir.gwent.core.ai

import ir.gwent.core.engine.GameEngine
import ir.gwent.core.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.random.Random

/**
 * AI strength, measured rather than asserted.
 *
 * "The new AI is better" is a claim worth nothing on its own, so the old greedy opponent is kept
 * as a yardstick and the new one has to beat it across many seeds, from both sides of the table.
 */
class AiStrengthTest {

    private data class Tally(var tactical: Int = 0, var greedy: Int = 0, var drawn: Int = 0) {
        val played: Int get() = tactical + greedy + drawn
        /** Draws count as half, so a 50% rate means genuinely no better. */
        val winRate: Double get() = if (played == 0) 0.0 else (tactical + drawn * 0.5) / played
    }

    /** Play one match and report who won. [tacticalSide] says which seat the new AI takes. */
    private fun play(seed: Long, tacticalSide: Side): Side? {
        val engine = GameEngine.start(
            CardDatabase.starterDeck(Leaders.FOLTEST),
            CardDatabase.starterDeck(Leaders.EREDIN),
            Random(seed),
        )
        val ais: Map<Side, Ai> = mapOf(
            tacticalSide to TacticalAi(tacticalSide),
            tacticalSide.other() to GreedyAi(tacticalSide.other()),
        )
        ais.values.forEach { it.mulligan(engine) }

        var guard = 0
        while (!engine.state.matchOver && guard++ < 4000) {
            ais.getValue(engine.state.turn).takeTurn(engine)
        }
        assertTrue(engine.state.matchOver, "seed $seed never finished")
        return engine.state.matchWinner
    }

    @Test
    fun `the tactical ai beats the greedy one over many matches`() {
        val tally = Tally()
        // Both seats, so a first-move advantage cannot be mistaken for skill.
        for (seed in 0L until 40L) {
            listOf(Side.A, Side.B).forEach { seat ->
                when (play(seed, seat)) {
                    seat -> tally.tactical++
                    null -> tally.drawn++
                    else -> tally.greedy++
                }
            }
        }
        println(
            "tactical ${tally.tactical} / greedy ${tally.greedy} / drawn ${tally.drawn} " +
                "-> win rate %.1f%%".format(tally.winRate * 100),
        )
        assertEquals(80, tally.played, "every match should have been played")
        assertTrue(
            tally.winRate > 0.55,
            "the tactical AI should be clearly stronger, not noise: ${tally.winRate}",
        )
    }

    @Test
    fun `both ais always finish a match`() {
        listOf<(Side) -> Ai>({ GreedyAi(it) }, { TacticalAi(it) }).forEach { make ->
            for (seed in 0L until 10L) {
                val e = GameEngine.start(
                    CardDatabase.starterDeck(Leaders.BRAN),
                    CardDatabase.starterDeck(Leaders.CLEAVER),
                    Random(seed),
                )
                val ais = mapOf(Side.A to make(Side.A), Side.B to make(Side.B))
                ais.values.forEach { it.mulligan(e) }
                var guard = 0
                while (!e.state.matchOver && guard++ < 4000) ais.getValue(e.state.turn).takeTurn(e)
                assertTrue(e.state.matchOver, "seed $seed hung")
                assertTrue(e.state.round <= 3, "seed $seed ran past round 3")
            }
        }
    }

    @Test
    fun `the tactical ai banks a won round instead of padding it`() {
        val e = GameEngine.start(
            CardDatabase.starterDeck(Leaders.FOLTEST),
            CardDatabase.starterDeck(Leaders.EREDIN),
            Random(1),
        )
        val me = e.state.playerA
        val them = e.state.playerB
        // Opponent has passed and we are comfortably ahead: the round is already won.
        them.passed = true
        me.rows.getValue(Row.MELEE) += UnitInstance(
            CardDatabase.byId("neu_champion")!!, e.state.allocateUid(),
        )
        e.state.turn = Side.A
        val handBefore = me.hand.size

        TacticalAi(Side.A).takeTurn(e)

        // Passing when the opponent has already passed ends the round, and the engine clears
        // `passed` for the next one — so the evidence is the finished round, not the flag.
        val round = e.state.roundHistory.lastOrNull()
        assertNotNull(round, "the round should have ended")
        assertEquals(Side.A, round!!.winner, "and it should have been won")
        assertEquals(handBefore, me.hand.size, "without spending a card to do it")
    }

    @Test
    fun `the tactical ai concedes a round it cannot reach`() {
        val e = GameEngine.start(
            CardDatabase.starterDeck(Leaders.FOLTEST),
            CardDatabase.starterDeck(Leaders.EREDIN),
            Random(2),
        )
        val me = e.state.playerA
        val them = e.state.playerB
        them.passed = true
        // An unreachable wall: more points than the whole hand could ever answer.
        repeat(6) {
            val u = UnitInstance(CardDatabase.byId("neu_champion")!!, e.state.allocateUid())
            u.boost(40)
            them.rows.getValue(Row.MELEE) += u
        }
        e.state.turn = Side.A
        val handBefore = me.hand.size

        TacticalAi(Side.A).takeTurn(e)

        val round = e.state.roundHistory.lastOrNull()
        assertNotNull(round, "conceding ends the round")
        assertEquals(Side.B, round!!.winner, "the unreachable opponent takes it")
        assertEquals(handBefore, me.hand.size, "and the hand is kept for the next round")
    }

    @Test
    fun `in the final round the tactical ai spends rather than concedes`() {
        val e = GameEngine.start(
            CardDatabase.starterDeck(Leaders.FOLTEST),
            CardDatabase.starterDeck(Leaders.EREDIN),
            Random(3),
        )
        val me = e.state.playerA
        val them = e.state.playerB
        e.state.round = 3
        them.passed = true
        repeat(6) {
            val u = UnitInstance(CardDatabase.byId("neu_champion")!!, e.state.allocateUid())
            u.boost(40)
            them.rows.getValue(Row.MELEE) += u
        }
        e.state.turn = Side.A
        val handBefore = me.hand.size

        TacticalAi(Side.A).takeTurn(e)

        // There is no next round to save for, so holding cards back is pure loss.
        assertTrue(
            me.hand.size < handBefore || me.passed,
            "in the decider it must at least try",
        )
        assertFalse(
            me.passed && me.hand.size == handBefore,
            "conceding the decider with a full hand is never right",
        )
    }
}
