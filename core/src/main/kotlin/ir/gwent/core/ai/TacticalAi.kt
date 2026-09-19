package ir.gwent.core.ai

import ir.gwent.core.engine.Action
import ir.gwent.core.engine.GameEngine
import ir.gwent.core.model.*

/**
 * An opponent that plays the match rather than the turn.
 *
 * GWENT is not won by putting the most points on the board; it is won by spending fewer cards to
 * do it. A hand is a fixed resource across three rounds, so the decisions that matter are about
 * *when to stop*: concede a round you cannot win, bank one you have already won, and never trade
 * two cards for the opponent's one. This AI reasons about card advantage and round context
 * first, and only then about which card is worth the most points.
 */
class TacticalAi(private val side: Side) : Ai {

    override fun takeTurn(engine: GameEngine): Boolean {
        val state = engine.state
        if (state.matchOver || state.turn != side) return false
        val me = state.player(side)
        val them = state.opponent(side)
        if (me.passed) return false

        // An empty hand passes automatically; ask for it so it is recorded as a pass either way.
        if (me.hand.isEmpty()) {
            engine.perform(side, Action.Pass)
            return false
        }

        if (shouldPass(engine, me, them)) {
            engine.perform(side, Action.Pass)
            return false
        }

        // Orders are free value — they cost no card — so fire them before spending from hand.
        useOrders(engine, me, them)

        val played = playBestCard(engine, me, them)
        if (!played && !engine.actedThisTurn) {
            engine.perform(side, Action.Pass)
            return false
        }
        engine.perform(side, Action.EndTurn)
        return true
    }

    // ------------------------------------------------------------ when to stop

    /**
     * The central judgement. Passing is how you keep cards, and keeping cards is how you win the
     * next round, so the AI looks for every reason to stop before it looks for a card to play.
     */
    private fun shouldPass(engine: GameEngine, me: PlayerState, them: PlayerState): Boolean {
        val state = engine.state
        val myScore = me.score()
        val theirScore = them.score()
        val lead = myScore - theirScore
        val finalRound = state.round >= 3 || me.crowns == CROWNS_TO_WIN - 1 && them.crowns == CROWNS_TO_WIN - 1

        // In the decider there is no next round to save for: spend everything.
        if (finalRound) return false

        if (them.passed) {
            // Ahead with the opponent already out: bank it rather than pad the score.
            if (lead > 0) return true
            // Behind and unable to reach them even by emptying the hand: concede cheaply.
            if (!canReach(me, -lead)) return true
            // Otherwise play on — the round is still winnable.
            return false
        }

        // Losing the card war badly while behind on board means this round is costing too much.
        val cardLead = me.hand.size - them.hand.size
        if (cardLead <= -2 && lead < 0 && state.round == 1) return true

        // Round 1 with a commanding lead and the card advantage: stop and keep the surplus.
        if (state.round == 1 && lead >= 20 && cardLead >= 1) return true

        return false
    }

    /** Could the hand still close a deficit of [deficit] points? */
    private fun canReach(me: PlayerState, deficit: Int): Boolean =
        me.hand.sumOf { maxOf(it.basePower, 0) } >= deficit

    // ------------------------------------------------------------ what to play

    private fun playBestCard(engine: GameEngine, me: PlayerState, them: PlayerState): Boolean {
        val ranked = me.hand.indices.sortedByDescending { scoreOf(me.hand[it], me, them, engine) }
        for (index in ranked) {
            val card = me.hand[index]
            val target = pickTarget(card, me, them)
            for (row in rowPreference(card)) {
                if (engine.perform(side, Action.PlayCard(index, row, target = target)) == null) return true
            }
        }
        return false
    }

    /**
     * What a card is worth here, in points it will actually add.
     *
     * Removal is only worth what it can remove, healing only what is missing, and a card that
     * draws is worth more than its body because it does not cost a card on net.
     */
    private fun scoreOf(card: Card, me: PlayerState, them: PlayerState, engine: GameEngine): Int {
        var value = card.basePower
        val enemies = them.units().filterNot { it.has(Status.IMMUNITY) }
        val allies = me.units()

        card.abilities.forEach { ability ->
            value += when (val e = ability.effect) {
                // Damage is worth the smaller of its size and the target it can actually kill.
                is Effect.Damage -> enemies.maxOfOrNull { minOf(e.amount, it.power) } ?: 0
                Effect.Destroy -> enemies.maxOfOrNull { it.power } ?: 0
                is Effect.Boost -> if (allies.isNotEmpty()) e.amount else 0
                is Effect.Strengthen -> if (allies.isNotEmpty()) e.amount else 0
                // Healing is capped by how damaged the best candidate actually is.
                is Effect.Heal -> allies.maxOfOrNull { minOf(e.amount, it.basePower - it.power) } ?: 0
                // A card that draws roughly pays for itself, so it is worth a body's worth more.
                is Effect.Draw -> e.count * 5
                Effect.Consume -> allies.minOfOrNull { it.power } ?: 0
                Effect.Resurrect -> me.graveyard.filter { it.isUnit }.maxOfOrNull { it.basePower } ?: 0
                is Effect.Summon -> CardDatabase.byId(e.cardId)?.basePower ?: 0
                is Effect.Apply -> statusValue(e.status, enemies, allies)
                is Effect.Profit -> if (me.faction == Faction.SYNDICATE) e.amount / 2 else 0
                // Weather pays out every turn it survives, so its worth is the damage it will
                // do on the crowded row, multiplied by the turns likely left in the round.
                is Effect.Weather -> weatherValue(e.kind, them)
                Effect.ClearWeather ->
                    if (engine.state.rowEffects.any { it.side == side }) 6 else 0
                else -> 0
            }
        }
        // An Order that survives is worth more than its printed body, since it pays again later.
        if (card.abilities.any { it.trigger == Trigger.ORDER }) value += 3
        return value
    }

    /** What weather is worth: per-tick damage on the busiest enemy row, over a few turns. */
    private fun weatherValue(kind: RowEffectKind, them: PlayerState): Int {
        val busiest = them.rows.values.maxByOrNull { it.size } ?: return 0
        if (busiest.isEmpty()) return 0
        val perTurn = when (kind) {
            RowEffectKind.STORM -> busiest.size          // hits everything
            RowEffectKind.RAIN -> minOf(2, busiest.size) // two random units
            RowEffectKind.FOG, RowEffectKind.FROST -> 2  // one unit, for 2
        }
        return perTurn * 2
    }

    private fun statusValue(
        status: Status,
        enemies: List<UnitInstance>,
        allies: List<UnitInstance>,
    ): Int = when (status) {
        Status.POISON -> enemies.maxOfOrNull { it.power } ?: 0
        Status.BLEEDING -> if (enemies.isNotEmpty()) 3 else 0
        Status.VITALITY -> if (allies.isNotEmpty()) 3 else 0
        Status.LOCKED -> if (enemies.any { it.card.abilities.isNotEmpty() }) 4 else 0
        Status.SHIELD, Status.ARMOR -> if (allies.isNotEmpty()) 2 else 0
        Status.RESILIENCE -> 4
        Status.DEFENDER -> 3
        else -> 1
    }

    /** Respect a printed row clause; otherwise keep melee free and stack ranged. */
    private fun rowPreference(card: Card): List<Row> {
        val required = card.abilities.firstNotNullOfOrNull { it.row }
        return if (required != null) listOf(required, required.other())
        else listOf(Row.MELEE, Row.RANGED)
    }

    private fun pickTarget(card: Card, me: PlayerState, them: PlayerState): Int? {
        val harmful = card.abilities.any {
            it.effect is Effect.Damage || it.effect is Effect.Destroy ||
                (it.effect as? Effect.Apply)?.status in setOf(Status.POISON, Status.BLEEDING, Status.LOCKED)
        }
        val consuming = card.abilities.any { it.effect == Effect.Consume }
        val weathering = card.abilities.any { it.effect is Effect.Weather }
        return when {
            // Point weather at the busiest enemy row by naming a unit standing on it.
            weathering -> them.rows.values.maxByOrNull { it.size }?.firstOrNull()?.uid
            // Eat the weakest ally: the points transfer, so spend the cheapest body.
            consuming -> me.units().minByOrNull { it.power }?.uid
            harmful -> them.units().filterNot { it.has(Status.IMMUNITY) }.maxByOrNull { it.power }?.uid
            // Help the ally that gains the most: the most damaged, else the biggest.
            else -> me.units().filter { it.isDamaged }.maxByOrNull { it.basePower - it.power }?.uid
                ?: me.units().maxByOrNull { it.power }?.uid
        }
    }

    // ------------------------------------------------------------ free value

    /** Orders cost no card, so there is never a reason to leave a useful one unused. */
    private fun useOrders(engine: GameEngine, me: PlayerState, them: PlayerState) {
        me.units().filter { it.orderReady && it.charges > 0 }.forEach { unit ->
            val ability = unit.card.abilities.firstOrNull { it.trigger == Trigger.ORDER } ?: return@forEach
            val target = when (ability.effect) {
                is Effect.Damage, Effect.Destroy ->
                    them.units().filterNot { it.has(Status.IMMUNITY) }.maxByOrNull { it.power }?.uid
                is Effect.Heal -> me.units().filter { it.isDamaged }.maxByOrNull { it.basePower - it.power }?.uid
                else -> me.units().maxByOrNull { it.power }?.uid
            } ?: return@forEach
            engine.perform(side, Action.UseOrder(unit.uid, target))
        }
    }

    // ------------------------------------------------------------ mulligan

    /**
     * Throw back what the deck cannot use: the weakest bodies, and anything whose ability needs
     * a board that does not exist yet.
     */
    override fun mulligan(engine: GameEngine) {
        val me = engine.state.player(side)
        var guard = 0
        while (me.mulligansLeft > 0 && guard++ < 20) {
            val worst = me.hand.indices.minByOrNull { i ->
                val card = me.hand[i]
                // Removal with nothing to remove is dead in the opening hand.
                val deadOnArrival = card.abilities.any {
                    it.effect is Effect.Damage || it.effect is Effect.Heal || it.effect == Effect.Consume
                } && engine.state.opponent(side).units().isEmpty()
                card.basePower - if (deadOnArrival) 4 else 0
            } ?: break
            if (engine.mulligan(side, worst) != null) break
        }
    }
}
