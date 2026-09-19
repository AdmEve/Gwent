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

/**
 * The signature effects. Summon was a no-op stub until now, so these exist to prove the
 * implementations actually move cards rather than silently doing nothing.
 */
class SignatureEffectsTest {

    private fun engine(seed: Long = 5L) = GameEngine.start(
        CardDatabase.starterDeck(Leaders.CRACH),
        CardDatabase.starterDeck(Leaders.DAGON),
        kotlin.random.Random(seed),
    )

    @Test
    fun `summon pulls a named card out of the deck onto the board`() {
        val e = engine()
        val p = e.state.playerA
        val nekker = CardDatabase.byId("mon_nekker")!!
        p.deck.add(0, nekker)
        val deckBefore = p.deck.size
        val onBoardBefore = p.units().size

        // Mobilisation-style summon: resolve it through a played card's Deploy.
        p.hand[0] = Card(
            "test_summoner", "Summoner", Faction.NEUTRAL, CardType.SPECIAL, provisions = 4,
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Summon("mon_nekker"))),
        )
        e.state.turn = Side.A
        assertNull(e.perform(Side.A, Action.PlayCard(0, Row.MELEE)))

        assertEquals(onBoardBefore + 1, p.units().size, "summon should put a unit on the board")
        assertEquals(deckBefore - 1, p.deck.size, "and take it out of the deck")
        assertTrue(p.units().any { it.card.id == "mon_nekker" })
    }

    @Test
    fun `consume destroys an ally and takes its power`() {
        val e = engine()
        val p = e.state.playerA
        val food = UnitInstance(CardDatabase.byId("neu_militia")!!, e.state.allocateUid())
        p.rows.getValue(Row.MELEE) += food
        val foodPower = food.power

        // The Griffin's own Deploy is Consume, so play it and point it at the meal.
        val griffin = CardDatabase.byId("mon_griffin")!!
        p.hand[0] = griffin
        e.state.turn = Side.A
        assertNull(e.perform(Side.A, Action.PlayCard(0, Row.MELEE, target = food.uid)))

        val eater = p.units().first { it.card.id == "mon_griffin" }
        assertEquals(griffin.basePower + foodPower, eater.power, "the eater grows by what it ate")
        assertFalse(p.units().any { it.uid == food.uid }, "the meal is gone from the board")
        assertTrue(p.graveyard.any { it.id == "neu_militia" }, "and lies in the graveyard")
    }

    @Test
    fun `resurrect returns the strongest fallen unit to the board`() {
        val e = engine()
        val p = e.state.playerA
        val weak = CardDatabase.byId("neu_militia")!!      // 4 power
        val strong = CardDatabase.byId("neu_mercenary")!!  // 6 power
        p.graveyard += weak
        p.graveyard += strong
        val graveBefore = p.graveyard.size

        e.state.turn = Side.A
        p.hand[0] = Card(
            "test_raise", "Raise", Faction.NEUTRAL, CardType.SPECIAL, provisions = 4,
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Resurrect)),
        )
        assertNull(e.perform(Side.A, Action.PlayCard(0, Row.MELEE)))

        // The played special lands in the graveyard itself once it has resolved, so the count
        // nets out. Assert on which card moved, not on how many are there.
        assertTrue(
            p.units().any { it.card.id == strong.id },
            "the strongest fallen unit is the one raised",
        )
        assertFalse(p.graveyard.any { it.id == strong.id }, "and it left the graveyard")
        assertTrue(p.graveyard.any { it.id == weak.id }, "the weaker corpse stays put")
        assertTrue(p.graveyard.any { it.id == "test_raise" }, "the spell itself is spent")
        assertEquals(graveBefore, p.graveyard.size, "one out, the spent spell in")
    }

    @Test
    fun `move sends a unit to the other row`() {
        val e = engine()
        val p = e.state.playerA
        val unit = UnitInstance(CardDatabase.byId("neu_militia")!!, e.state.allocateUid())
        p.rows.getValue(Row.MELEE) += unit
        assertEquals(1, p.rows.getValue(Row.MELEE).size)

        e.state.turn = Side.A
        p.hand[0] = Card(
            "test_move", "Reposition", Faction.NEUTRAL, CardType.SPECIAL, provisions = 4,
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Move)),
        )
        assertNull(e.perform(Side.A, Action.PlayCard(0, Row.MELEE, target = unit.uid)))

        assertEquals(0, p.rows.getValue(Row.MELEE).size, "it left the melee row")
        assertEquals(1, p.rows.getValue(Row.RANGED).size, "and arrived on the ranged row")
    }

    @Test
    fun `the expanded pool still builds legal decks for every leader`() {
        Leaders.ALL.forEach { leader ->
            val d = CardDatabase.starterDeck(leader)
            assertTrue(d.validate().isEmpty(), "${leader.name}: ${d.validate()}")
        }
    }

    @Test
    fun `the pool offers enough cards for deck building to be a choice`() {
        Leaders.PLAYABLE_FACTIONS.forEach { faction ->
            val options = CardDatabase.forFaction(faction)
            assertTrue(options.size >= 20, "$faction has only ${options.size} cards to choose from")
        }
    }
}
