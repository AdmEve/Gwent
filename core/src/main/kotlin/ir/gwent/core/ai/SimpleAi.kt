package ir.gwent.core.ai

import ir.gwent.core.engine.Action
import ir.gwent.core.engine.GameEngine
import ir.gwent.core.model.*

/**
 * A deliberately modest opponent: it plays for points, uses removal on the biggest threat, and
 * knows the one strategic rule that matters most in GWENT — do not keep spending cards into a
 * round you have already lost.
 */
class SimpleAi(private val side: Side) {

    /** Decide and perform the next action. Returns false when the AI has finished its turn. */
    fun takeTurn(engine: GameEngine): Boolean {
        val state = engine.state
        if (state.matchOver || state.turn != side) return false
        val me = state.player(side)
        val them = state.opponent(side)
        if (me.passed) return false

        // Nothing left to play: passing is automatic, but ask for it explicitly so the engine
        // records it the same way as a chosen pass.
        if (me.hand.isEmpty()) {
            engine.perform(side, Action.Pass)
            return false
        }

        val myScore = me.score()
        val theirScore = them.score()

        // If the opponent has passed and we are ahead, bank the round rather than overcommitting.
        if (them.passed && myScore > theirScore) {
            engine.perform(side, Action.Pass)
            return false
        }

        // A round we cannot reach is not worth cards; concede it and keep the hand for the next.
        if (them.passed && !canCatchUp(me, theirScore - myScore)) {
            engine.perform(side, Action.Pass)
            return false
        }

        val played = playBestCard(engine, me, them)
        if (!played) {
            engine.perform(side, Action.Pass)
            return false
        }
        useOrders(engine, me, them)
        engine.perform(side, Action.EndTurn)
        return true
    }

    /** Could the cards in hand still close a deficit of [deficit] points? */
    private fun canCatchUp(me: PlayerState, deficit: Int): Boolean =
        me.hand.sumOf { maxOf(it.basePower, 0) } >= deficit

    private fun playBestCard(engine: GameEngine, me: PlayerState, them: PlayerState): Boolean {
        val candidates = me.hand.indices.sortedByDescending { valueOf(me.hand[it], them) }
        for (index in candidates) {
            val card = me.hand[index]
            val row = preferredRow(card)
            val target = pickTarget(card, me, them)
            for (r in listOf(row, row.other())) {
                if (engine.perform(side, Action.PlayCard(index, r, target = target)) == null) return true
            }
        }
        return false
    }

    /** Rough worth: raw points, plus credit for removal when there is something to remove. */
    private fun valueOf(card: Card, them: PlayerState): Int {
        var score = card.basePower
        card.abilities.forEach { ability ->
            score += when (val e = ability.effect) {
                is Effect.Damage -> if (them.units().isNotEmpty()) e.amount else 0
                Effect.Destroy -> them.units().maxOfOrNull { it.power } ?: 0
                is Effect.Boost -> e.amount
                is Effect.Draw -> e.count * 2
                else -> 0
            }
        }
        return score
    }

    /** Respect printed row clauses; otherwise melee, so ranged stays free for archers. */
    private fun preferredRow(card: Card): Row =
        card.abilities.firstNotNullOfOrNull { it.row } ?: Row.MELEE

    /** Harmful effects go at their strongest unit; helpful ones at our weakest damaged unit. */
    private fun pickTarget(card: Card, me: PlayerState, them: PlayerState): Int? {
        val harmful = card.abilities.any {
            it.effect is Effect.Damage || it.effect is Effect.Destroy ||
                (it.effect as? Effect.Apply)?.status in setOf(Status.POISON, Status.BLEEDING)
        }
        return if (harmful) {
            them.units().filterNot { it.has(Status.IMMUNITY) }.maxByOrNull { it.power }?.uid
        } else {
            me.units().filter { it.isDamaged }.minByOrNull { it.power }?.uid
                ?: me.units().maxByOrNull { it.power }?.uid
        }
    }

    private fun useOrders(engine: GameEngine, me: PlayerState, them: PlayerState) {
        me.units().filter { it.orderReady && it.charges > 0 }.forEach { unit ->
            val ability = unit.card.abilities.firstOrNull { it.trigger == Trigger.ORDER } ?: return@forEach
            val harmful = ability.effect is Effect.Damage || ability.effect is Effect.Destroy
            val target = if (harmful) {
                them.units().filterNot { it.has(Status.IMMUNITY) }.maxByOrNull { it.power }?.uid
            } else {
                me.units().filter { it.isDamaged }.minByOrNull { it.power }?.uid
            }
            if (target != null) engine.perform(side, Action.UseOrder(unit.uid, target))
        }
    }

    /** Throw back the weakest cards while mulligans remain. */
    fun mulligan(engine: GameEngine) {
        val me = engine.state.player(side)
        while (me.mulligansLeft > 0) {
            val worst = me.hand.indices.minByOrNull { me.hand[it].basePower } ?: break
            if (engine.mulligan(side, worst) != null) break
        }
    }
}
