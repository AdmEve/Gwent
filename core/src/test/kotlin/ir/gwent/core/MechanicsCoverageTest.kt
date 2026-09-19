package ir.gwent.core

import ir.gwent.core.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Mechanics coverage audit.
 *
 * Two bugs of the same shape have already shipped: Effect.Summon matched in the engine and did
 * nothing, and "Biting Frost" was plain damage wearing a weather name. Both read as implemented.
 * This audit exists to make that class of gap visible rather than discovered by accident.
 *
 * It reports three things:
 *   UNREACHABLE  the engine supports it, but no card or leader can produce it
 *   UNUSED       a status nothing ever applies
 *   MISLABELLED  card text names a mechanic its effect does not implement
 */
class MechanicsCoverageTest {

    private val allCards = CardDatabase.ALL + CardDatabase.STRATAGEMS
    private val allAbilities = allCards.flatMap { it.abilities } + Leaders.ALL.map { it.ability }
    private val allEffects = allAbilities.map { it.effect }

    private fun effectName(e: Effect): String = e::class.simpleName ?: "?"

    @Test
    fun `audit which mechanics are reachable from real cards`() {
        println("\n" + "=".repeat(64))
        println("MECHANICS COVERAGE AUDIT")
        println("=".repeat(64))

        // ---- effects ----------------------------------------------------
        val effectKinds = listOf(
            "Boost", "Strengthen", "Damage", "Heal", "Reset", "Destroy", "Banish",
            "Apply", "Purify", "Summon", "Consume", "Resurrect", "Move",
            "Weather", "ClearWeather", "Draw", "Profit", "None",
        )
        val usedEffects = allEffects.map { effectName(it) }.toSet()
        println("\n[EFFECTS] ${usedEffects.size}/${effectKinds.size} reachable from a card or leader")
        effectKinds.forEach { kind ->
            val n = allEffects.count { effectName(it) == kind }
            val mark = if (n > 0) "ok  " else "UNREACHABLE"
            println("  %-11s %-12s %s".format(mark, kind, if (n > 0) "$n card(s)" else ""))
        }

        // ---- statuses ---------------------------------------------------
        val appliedStatuses = allEffects.filterIsInstance<Effect.Apply>().map { it.status }.toSet() +
            allCards.flatMap { it.innateStatuses }
        println("\n[STATUSES] ${appliedStatuses.size}/${Status.entries.size} applied by some card")
        Status.entries.forEach { s ->
            val n = allEffects.filterIsInstance<Effect.Apply>().count { it.status == s } +
                allCards.count { s in it.innateStatuses }
            println("  %-11s %-12s %s".format(if (n > 0) "ok  " else "UNUSED", s.name, if (n > 0) "$n card(s)" else ""))
        }

        // ---- triggers ---------------------------------------------------
        println("\n[TRIGGERS] which fire from real cards")
        Trigger.entries.forEach { t ->
            val n = allAbilities.count { it.trigger == t }
            println("  %-11s %-16s %s".format(if (n > 0) "ok  " else "UNREACHABLE", t.name, if (n > 0) "$n card(s)" else ""))
        }

        // ---- card types -------------------------------------------------
        println("\n[CARD TYPES]")
        CardType.entries.forEach { ct ->
            val n = allCards.count { it.type == ct }
            println("  %-11s %-12s %s".format(if (n > 0) "ok  " else "UNREACHABLE", ct.name, if (n > 0) "$n card(s)" else ""))
        }

        // ---- mislabelled: text names a mechanic the effect does not do ----
        println("\n[MISLABELLED] card text names a mechanic its effect does not implement")
        // A claim can be honoured by an effect OR by a status, because some statuses *are* the
        // mechanic: a card with Doomed truthfully says it is "banished", without any Effect.Banish.
        val claims = mapOf(
            "Frost" to setOf("Weather"),
            "Fog" to setOf("Weather"),
            "Rain" to setOf("Weather"),
            "Storm" to setOf("Weather"),
            "Consume" to setOf("Consume"),
            "Resurrect" to setOf("Resurrect"),
            "Summon" to setOf("Summon"),
            "Banish" to setOf("Banish", "DOOMED"),
            "Purify" to setOf("Purify"),
            "Reset" to setOf("Reset"),
        )
        val lies = mutableListOf<String>()
        allCards.forEach { card ->
            val provides = card.abilities.map { effectName(it.effect) }.toSet() +
                card.innateStatuses.map { it.name } +
                card.abilities.mapNotNull { (it.effect as? Effect.Apply)?.status?.name }
            claims.forEach { (word, satisfiedBy) ->
                if (card.text.contains(word, ignoreCase = true) && provides.intersect(satisfiedBy).isEmpty()) {
                    lies += "${card.id}: text says \"$word\" but provides $provides"
                }
            }
        }
        assertTrue(lies.isEmpty(), "card text must match behaviour:\n  " + lies.joinToString("\n  "))
    }
}
