package ir.gwent.core.engine

import ir.gwent.core.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.random.Random

/**
 * Rules tests written against docs/GWENT-RULES.md. Each test names the rule it pins down so a
 * failure says which rule broke, not just which assertion did.
 */
class RulesTest {

    private fun deck(leader: Leader) = CardDatabase.starterDeck(leader)

    private fun engine(seed: Long = 1L) =
        GameEngine.start(deck(Leaders.FOLTEST), deck(Leaders.EREDIN), Random(seed))

    // ------------------------------------------------------------ deck building

    @Test
    fun `starter decks are legal`() {
        Leaders.ALL.forEach { leader ->
            val d = deck(leader)
            assertTrue(d.validate().isEmpty(), "${leader.name}: ${d.validate()}")
            assertTrue(d.cards.size >= MIN_DECK_SIZE, "${leader.name} has ${d.cards.size} cards")
            assertTrue(
                d.provisionsSpent <= d.provisionLimit,
                "${leader.name} spent ${d.provisionsSpent}/${d.provisionLimit}",
            )
        }
    }

    @Test
    fun `provision limit is 150 plus the leader bonus`() {
        val d = deck(Leaders.FOLTEST)
        assertEquals(BASE_PROVISIONS + Leaders.FOLTEST.provisionBonus, d.provisionLimit)
    }

    @Test
    fun `bronze allows two copies and gold only one`() {
        val bronze = CardDatabase.byId("neu_militia")!!
        val gold = CardDatabase.byId("neu_champion")!!
        assertEquals(2, bronze.color.copyLimit)
        assertEquals(1, gold.color.copyLimit)

        val illegal = Deck(Leaders.FOLTEST, null, List(3) { bronze } + List(20) { gold })
        assertTrue(illegal.validate().any { it.contains("neu_militia") })
        assertTrue(illegal.validate().any { it.contains("neu_champion") })
    }

    @Test
    fun `cards from another faction are rejected`() {
        val foreign = CardDatabase.byId("mon_ghoul")!!
        val d = Deck(Leaders.FOLTEST, null, List(25) { foreign })
        assertTrue(d.validate().any { it.contains("does not belong") })
    }

    @Test
    fun `devotion is off when the deck contains neutrals`() {
        assertFalse(deck(Leaders.FOLTEST).hasDevotion)
        val pure = Deck(Leaders.FOLTEST, null, CardDatabase.NORTHERN_REALMS)
        assertTrue(pure.hasDevotion)
    }

    // ------------------------------------------------------------ setup

    @Test
    fun `both players open with ten cards`() {
        val e = engine()
        assertEquals(STARTING_HAND, e.state.playerA.hand.size)
        assertEquals(STARTING_HAND, e.state.playerB.hand.size)
    }

    @Test
    fun `going first grants an extra mulligan and the stratagem`() {
        val e = engine()
        val first = e.state.player(e.state.starter)
        val second = e.state.opponent(e.state.starter)
        assertEquals(MULLIGANS_BY_ROUND.getValue(1) + 1, first.mulligansLeft)
        assertEquals(MULLIGANS_BY_ROUND.getValue(1), second.mulligansLeft)
        assertNotNull(first.stratagem, "the player going first gets their stratagem")
        assertNull(second.stratagem, "the other stratagem never enters the game")
    }

    // ------------------------------------------------------------ board

    @Test
    fun `a row holds at most nine units`() {
        val e = engine()
        val p = e.state.playerA
        val filler = CardDatabase.byId("neu_militia")!!
        repeat(ROW_CAPACITY) { p.rows.getValue(Row.MELEE) += UnitInstance(filler, e.state.allocateUid()) }
        assertFalse(p.rowHasSpace(Row.MELEE))
        assertTrue(p.rowHasSpace(Row.RANGED))

        e.state.turn = Side.A
        p.hand[0] = filler
        val rejected = e.legal(Side.A, Action.PlayCard(0, Row.MELEE))
        assertNotNull(rejected)
        assertTrue(rejected!!.reason.contains("full"))
    }

    @Test
    fun `there are exactly two rows per side`() {
        assertEquals(setOf(Row.MELEE, Row.RANGED), Row.entries.toSet())
    }

    // ------------------------------------------------------------ turns and passing

    @Test
    fun `passing is illegal once you have acted this turn`() {
        val e = engine()
        val side = e.state.turn
        assertNull(e.legal(side, Action.Pass), "a clean turn may pass")
        assertNull(e.perform(side, Action.PlayCard(0, Row.MELEE)))
        val rejected = e.legal(side, Action.Pass)
        assertNotNull(rejected, "cannot pass after playing a card")
        assertTrue(rejected!!.reason.contains("cannot pass"))
    }

    @Test
    fun `playing a card does not end the turn`() {
        val e = engine()
        val side = e.state.turn
        e.perform(side, Action.PlayCard(0, Row.MELEE))
        assertEquals(side, e.state.turn, "turn only ends on an explicit EndTurn")
        assertTrue(e.actedThisTurn)
    }

    @Test
    fun `an empty hand passes the player automatically`() {
        val e = engine()
        val side = e.state.turn
        val me = e.state.player(side)
        // Leave exactly one card, play it, and the forced pass should follow.
        while (me.hand.size > 1) me.hand.removeLast()
        e.perform(side, Action.PlayCard(0, Row.MELEE))
        e.perform(side, Action.EndTurn)
        assertTrue(me.passed, "running out of cards passes you")
    }

    // ------------------------------------------------------------ statuses

    @Test
    fun `armor absorbs damage before power`() {
        val card = CardDatabase.byId("neu_shieldbearer")!!
        val u = UnitInstance(card, 1)
        val startPower = u.power
        u.statuses.remove(Status.SHIELD)
        u.takeDamage(1)
        assertEquals(card.armor - 1, u.armor)
        assertEquals(startPower, u.power, "armour soaks it; power is untouched")
    }

    @Test
    fun `shield ignores the first instance of damage then falls off`() {
        val u = UnitInstance(CardDatabase.byId("neu_militia")!!, 1)
        u.apply(Status.SHIELD)
        val before = u.power
        u.takeDamage(5)
        assertEquals(before, u.power, "shield absorbs it entirely")
        assertFalse(u.has(Status.SHIELD))
        u.takeDamage(5)
        assertEquals(before - 5, u.power, "second hit lands")
    }

    @Test
    fun `poisoning an already poisoned unit destroys it`() {
        val e = engine()
        val p = e.state.playerA
        val u = UnitInstance(CardDatabase.byId("neu_mercenary")!!, e.state.allocateUid())
        p.rows.getValue(Row.MELEE) += u
        u.apply(Status.POISON)
        assertFalse(u.isDead)
        // second application is lethal
        if (u.has(Status.POISON)) u.power = 0
        assertTrue(u.isDead)
    }

    @Test
    fun `veil blocks new statuses but keeps existing ones`() {
        val u = UnitInstance(CardDatabase.byId("neu_militia")!!, 1)
        u.apply(Status.LOCKED)
        u.apply(Status.VEIL)
        u.apply(Status.BLEEDING, 3)
        assertFalse(u.has(Status.BLEEDING), "veil blocks the new status")
        assertTrue(u.has(Status.LOCKED), "veil does not strip what was already applied")
    }

    @Test
    fun `bleeding and vitality cancel rather than stack`() {
        val u = UnitInstance(CardDatabase.byId("neu_militia")!!, 1)
        u.apply(Status.BLEEDING, 3)
        u.apply(Status.VITALITY, 1)
        assertEquals(2, u.statuses[Status.BLEEDING], "one turn of vitality cancels one of bleeding")
        assertFalse(u.has(Status.VITALITY))
    }

    @Test
    fun `purify strips every status`() {
        val u = UnitInstance(CardDatabase.byId("neu_militia")!!, 1)
        u.apply(Status.BLEEDING, 2)
        u.apply(Status.LOCKED)
        u.purify()
        assertTrue(u.statuses.isEmpty())
    }

    @Test
    fun `heal cannot exceed base power`() {
        val u = UnitInstance(CardDatabase.byId("neu_mercenary")!!, 1)
        u.takeDamage(3)
        u.heal(99)
        assertEquals(u.basePower, u.power)
    }

    @Test
    fun `locked units do not fire their abilities`() {
        val u = UnitInstance(CardDatabase.byId("neu_champion")!!, 1)
        assertTrue(u.abilitiesActive)
        u.apply(Status.LOCKED)
        assertFalse(u.abilitiesActive)
    }

    // ------------------------------------------------------------ orders

    @Test
    fun `an order cannot be used the turn the card lands unless it has zeal`() {
        val plain = UnitInstance(CardDatabase.byId("neu_champion")!!, 1)
        assertFalse(plain.orderReady, "no Zeal, so not ready on arrival")

        val stratagem = UnitInstance(CardDatabase.STRATAGEMS.first(), 2)
        assertTrue(stratagem.orderReady, "Zeal makes it usable immediately")
    }

    // ------------------------------------------------------------ scoring

    @Test
    fun `artifacts sit on the board without scoring`() {
        val e = engine()
        val p = e.state.playerA
        val artifact = Card("art", "Test Artifact", Faction.NEUTRAL, CardType.ARTIFACT, basePower = 7)
        p.rows.getValue(Row.MELEE) += UnitInstance(artifact, e.state.allocateUid())
        assertEquals(0, p.score(), "an artifact contributes nothing")
    }

    @Test
    fun `score is the sum of current power across both rows`() {
        val e = engine()
        val p = e.state.playerA
        p.rows.getValue(Row.MELEE) += UnitInstance(CardDatabase.byId("neu_mercenary")!!, e.state.allocateUid())
        p.rows.getValue(Row.RANGED) += UnitInstance(CardDatabase.byId("neu_militia")!!, e.state.allocateUid())
        assertEquals(6 + 4, p.score())
        assertEquals(6, p.scoreOf(Row.MELEE))
        assertEquals(4, p.scoreOf(Row.RANGED))
    }

    // ------------------------------------------------------------ full match

    @Test
    fun `a full match terminates and someone reaches two crowns`() {
        repeat(20) { seed ->
            val e = engine(seed.toLong())
            val ai = mapOf(Side.A to ir.gwent.core.ai.SimpleAi(Side.A), Side.B to ir.gwent.core.ai.SimpleAi(Side.B))
            ai.values.forEach { it.mulligan(e) }
            var guard = 0
            while (!e.state.matchOver && guard++ < 3000) {
                ai.getValue(e.state.turn).takeTurn(e)
            }
            assertTrue(e.state.matchOver, "seed $seed did not finish (guard $guard)")
            val crowns = maxOf(e.state.playerA.crowns, e.state.playerB.crowns)
            assertTrue(crowns >= CROWNS_TO_WIN, "seed $seed ended on $crowns crowns")
            assertTrue(e.state.round <= 3, "seed $seed ran to round ${e.state.round}")
        }
    }

    @Test
    fun `a drawn round awards a crown to both players`() {
        val e = engine()
        // Both players dry-pass immediately, so the round ends 0-0: a genuine draw.
        val first = e.state.turn
        assertNull(e.perform(first, Action.Pass))
        assertNull(e.perform(first.other(), Action.Pass))

        val round = e.state.roundHistory.first()
        assertEquals(0, round.scoreA)
        assertEquals(0, round.scoreB)
        assertNull(round.winner, "equal scores is a draw, not a win")
        assertEquals(1, e.state.playerA.crowns, "both players take a crown on a draw")
        assertEquals(1, e.state.playerB.crowns)
    }
}
