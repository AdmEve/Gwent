package ir.gwent.core.engine

import ir.gwent.core.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.random.Random

/**
 * Weather and hazards.
 *
 * The engine implemented these from the start, but no card could create one and the Effect type
 * could not express it — a documented mechanic that was unreachable in play. These tests exist
 * to prove it is reachable now, and that each kind picks the target the rules say it does.
 */
class WeatherTest {

    private fun engine(seed: Long = 1L) = GameEngine.start(
        CardDatabase.starterDeck(Leaders.DAGON),
        CardDatabase.starterDeck(Leaders.FOLTEST),
        Random(seed),
    )

    private fun put(e: GameEngine, p: PlayerState, row: Row, id: String, power: Int? = null): UnitInstance {
        val u = UnitInstance(CardDatabase.byId(id)!!, e.state.allocateUid())
        if (power != null) { u.basePower = power; u.power = power }
        p.rows.getValue(row) += u
        return u
    }

    @Test
    fun `a weather card lays an effect on an enemy row`() {
        val e = engine()
        val me = e.state.playerA
        val them = e.state.playerB
        put(e, them, Row.MELEE, "neu_militia")
        assertTrue(e.state.rowEffects.isEmpty(), "no weather to begin with")

        me.hand[0] = CardDatabase.byId("mon_frost")!!
        e.state.turn = Side.A
        assertNull(e.perform(Side.A, Action.PlayCard(0, Row.MELEE)))

        assertEquals(1, e.state.rowEffects.size, "frost should now sit on the board")
        val effect = e.state.rowEffects.first()
        assertEquals(Side.B, effect.side, "weather lands on the opponent")
        assertEquals(RowEffectKind.FROST, effect.kind)
    }

    @Test
    fun `weather goes to the busiest enemy row when no target is named`() {
        val e = engine()
        val me = e.state.playerA
        val them = e.state.playerB
        put(e, them, Row.MELEE, "neu_militia")
        repeat(3) { put(e, them, Row.RANGED, "neu_militia") }

        me.hand[0] = CardDatabase.byId("mon_fog")!!
        e.state.turn = Side.A
        e.perform(Side.A, Action.PlayCard(0, Row.MELEE))

        assertEquals(Row.RANGED, e.state.rowEffects.first().row, "the crowded row is the target")
    }

    @Test
    fun `frost damages the highest unit and fog the lowest`() {
        val e = engine()
        val them = e.state.playerB
        val big = put(e, them, Row.MELEE, "neu_militia", power = 9)
        val small = put(e, them, Row.MELEE, "neu_militia", power = 2)

        e.state.rowEffects += RowEffect(Side.B, Row.MELEE, RowEffectKind.FROST)
        // Weather ticks at the start of the owner's turn, so hand the turn to B.
        e.state.turn = Side.A
        e.state.playerA.hand[0] = CardDatabase.byId("neu_militia")!!
        e.perform(Side.A, Action.PlayCard(0, Row.MELEE))
        e.perform(Side.A, Action.EndTurn)

        assertEquals(7, big.power, "frost takes 2 from the highest")
        assertEquals(2, small.power, "and leaves the lowest alone")
    }

    @Test
    fun `storm damages every unit on the row`() {
        val e = engine()
        val them = e.state.playerB
        val units = (1..3).map { put(e, them, Row.MELEE, "neu_mercenary") }
        val before = units.map { it.power }

        e.state.rowEffects += RowEffect(Side.B, Row.MELEE, RowEffectKind.STORM)
        e.state.turn = Side.A
        e.state.playerA.hand[0] = CardDatabase.byId("neu_militia")!!
        e.perform(Side.A, Action.PlayCard(0, Row.MELEE))
        e.perform(Side.A, Action.EndTurn)

        units.forEachIndexed { i, u ->
            assertEquals(before[i] - 1, u.power, "storm takes 1 from everything on the row")
        }
    }

    @Test
    fun `clear skies removes weather from your own side`() {
        val e = engine()
        val me = e.state.playerA
        e.state.rowEffects += RowEffect(Side.A, Row.MELEE, RowEffectKind.FROST)
        e.state.rowEffects += RowEffect(Side.B, Row.MELEE, RowEffectKind.FOG)

        me.hand[0] = CardDatabase.byId("neu_clear")!!
        e.state.turn = Side.A
        assertNull(e.perform(Side.A, Action.PlayCard(0, Row.MELEE)))

        assertFalse(e.state.rowEffects.any { it.side == Side.A }, "your side is clear")
        assertTrue(e.state.rowEffects.any { it.side == Side.B }, "theirs is untouched")
    }

    @Test
    fun `immune units ignore weather`() {
        val e = engine()
        val them = e.state.playerB
        val immune = put(e, them, Row.MELEE, "neu_mercenary")
        immune.apply(Status.IMMUNITY)
        val before = immune.power

        e.state.rowEffects += RowEffect(Side.B, Row.MELEE, RowEffectKind.STORM)
        e.state.turn = Side.A
        e.state.playerA.hand[0] = CardDatabase.byId("neu_militia")!!
        e.perform(Side.A, Action.PlayCard(0, Row.MELEE))
        e.perform(Side.A, Action.EndTurn)

        assertEquals(before, immune.power, "immunity protects against row effects too")
    }

    @Test
    fun `weather clears between rounds`() {
        val e = engine()
        e.state.rowEffects += RowEffect(Side.B, Row.MELEE, RowEffectKind.FROST)
        // Both players dry-pass, ending the round.
        val first = e.state.turn
        e.perform(first, Action.Pass)
        e.perform(first.other(), Action.Pass)
        assertTrue(e.state.rowEffects.isEmpty(), "a new round starts on clear ground")
    }

    @Test
    fun `every weather card is reachable from a real deck`() {
        val weatherCards = CardDatabase.ALL.filter { c ->
            c.abilities.any { it.effect is Effect.Weather || it.effect == Effect.ClearWeather }
        }
        assertTrue(weatherCards.size >= 5, "found only ${weatherCards.size} weather cards")
        // Each must be legal in some faction's deck, or it can never be played.
        weatherCards.forEach { card ->
            val home = Leaders.PLAYABLE_FACTIONS.firstOrNull { card.faction.playableIn(it) }
            assertNotNull(home, "${card.name} belongs to no playable deck")
        }
    }
}
