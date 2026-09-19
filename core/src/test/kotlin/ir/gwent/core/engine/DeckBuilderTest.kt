package ir.gwent.core.engine

import ir.gwent.core.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Deck-building rules. These are the constraints that make provisions a decision rather than a
 * number, so each test names the rule it holds.
 */
class DeckBuilderTest {

    private val bronze = CardDatabase.byId("nor_infantry")!!   // 4 provisions, Northern Realms
    private val gold = CardDatabase.byId("nor_vernon")!!       // 10 provisions, gold
    private val neutral = CardDatabase.byId("neu_militia")!!   // 4 provisions, neutral
    private val foreign = CardDatabase.byId("mon_ghoul")!!     // Monsters

    private fun builder() = DeckBuilder(Leaders.FOLTEST)

    @Test
    fun `budget is 150 plus the leader bonus`() {
        val b = builder()
        assertEquals(BASE_PROVISIONS + Leaders.FOLTEST.provisionBonus, b.provisionLimit)
        assertEquals(b.provisionLimit, b.provisionsLeft, "an empty deck has spent nothing")
    }

    @Test
    fun `spending reduces what is left`() {
        val b = builder()
        b.add(gold)
        assertEquals(gold.provisions, b.provisionsSpent)
        assertEquals(b.provisionLimit - gold.provisions, b.provisionsLeft)
    }

    @Test
    fun `a bronze may be taken twice and no more`() {
        val b = builder()
        assertNull(b.add(bronze))
        assertNull(b.add(bronze))
        val third = b.add(bronze)
        assertNotNull(third, "a third copy of a bronze is illegal")
        assertTrue(third!!.contains("2 copies"), third)
        assertEquals(2, b.copiesOf(bronze))
    }

    @Test
    fun `a gold may be taken once and no more`() {
        val b = builder()
        assertNull(b.add(gold))
        val second = b.add(gold)
        assertNotNull(second, "a second copy of a gold is illegal")
        assertTrue(second!!.contains("1 copy"), second)
    }

    @Test
    fun `neutrals are legal in any deck but foreign faction cards are not`() {
        val b = builder()
        assertNull(b.add(neutral), "neutral cards belong in every deck")
        val rejected = b.add(foreign)
        assertNotNull(rejected)
        assertTrue(rejected!!.contains("MONSTERS"), rejected)
    }

    @Test
    fun `a card that would break the budget is refused, and says so`() {
        val b = builder()
        // Spend the budget down to less than one gold.
        while (b.provisionsLeft > gold.provisions - 1) {
            if (b.add(bronze) != null && b.add(neutral) != null) break
        }
        val rejection = b.rejectionFor(gold)
        if (b.provisionsLeft < gold.provisions) {
            assertNotNull(rejection, "cannot afford it, so it must be refused")
            assertTrue(rejection!!.contains("provisions left"), rejection)
        }
        assertTrue(b.provisionsSpent <= b.provisionLimit, "never over budget")
    }

    @Test
    fun `removing a card frees its provisions`() {
        val b = builder()
        b.add(gold)
        val spent = b.provisionsSpent
        assertTrue(b.remove(gold))
        assertEquals(spent - gold.provisions, b.provisionsSpent)
        assertFalse(b.remove(gold), "removing what is not there reports false")
    }

    @Test
    fun `a deck is incomplete until it reaches the minimum size`() {
        val b = builder()
        assertFalse(b.isComplete)
        assertTrue(b.problems().any { it.contains("more card") })
        b.autoComplete(CardDatabase.ALL)
        assertTrue(b.isComplete, "autoComplete should produce a legal deck: ${b.problems()}")
        assertTrue(b.problems().isEmpty(), "${b.problems()}")
    }

    @Test
    fun `autoComplete produces a legal deck for every leader`() {
        Leaders.ALL.forEach { leader ->
            val b = DeckBuilder(leader)
            b.autoComplete(CardDatabase.ALL)
            assertTrue(b.size >= MIN_DECK_SIZE, "${leader.name}: only ${b.size} cards")
            assertTrue(
                b.provisionsSpent <= b.provisionLimit,
                "${leader.name}: ${b.provisionsSpent}/${b.provisionLimit}",
            )
            assertTrue(b.build().validate().isEmpty(), "${leader.name}: ${b.build().validate()}")
        }
    }

    @Test
    fun `the starter deck loads into the builder unchanged`() {
        val b = DeckBuilder.fromStarter(Leaders.EREDIN)
        val starter = CardDatabase.starterDeck(Leaders.EREDIN)
        assertEquals(starter.cards.size, b.size)
        assertEquals(starter.provisionsSpent, b.provisionsSpent)
        assertTrue(b.isComplete)
    }

    @Test
    fun `stratagems are chosen separately, not added as deck cards`() {
        val b = builder()
        val strat = CardDatabase.STRATAGEMS.first()
        val rejected = b.add(strat)
        assertNotNull(rejected)
        assertTrue(rejected!!.contains("separately"), rejected)
        assertNull(b.setStratagem(strat))
        assertEquals(strat, b.stratagem)
    }

    @Test
    fun `a built deck plays`() {
        val a = DeckBuilder.fromStarter(Leaders.FOLTEST).build()
        val c = DeckBuilder.fromStarter(Leaders.EREDIN).build()
        val engine = GameEngine.start(a, c, kotlin.random.Random(3))
        assertEquals(STARTING_HAND, engine.state.playerA.hand.size)
        assertFalse(engine.state.matchOver)
    }
}
