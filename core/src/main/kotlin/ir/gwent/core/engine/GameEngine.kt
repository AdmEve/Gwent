package ir.gwent.core.engine

import ir.gwent.core.model.*
import kotlin.random.Random

/** A thing a player can do on their turn. */
sealed interface Action {
    /** Play a card from hand into [row] at [position] (defaults to the right end of the row). */
    data class PlayCard(val handIndex: Int, val row: Row, val position: Int = -1, val target: Int? = null) : Action

    /** Trigger an Order ability on a unit already on the board. */
    data class UseOrder(val uid: Int, val target: Int? = null) : Action

    /** Use the once-per-match leader ability. */
    data class UseLeader(val target: Int? = null) : Action

    /** Hand control to the opponent, having played or triggered something. */
    data object EndTurn : Action

    /** Withdraw from the round. Only legal if nothing was played or triggered this turn. */
    data object Pass : Action
}

/** Why an action was refused. Returned rather than thrown so the AI can probe legality cheaply. */
data class Rejected(val reason: String)

class GameEngine(val state: GameState) {

    /**
     * Whether the player to move has already spent their turn. Post-Homecoming a turn does not end
     * when a card is played — the player may then fire Order abilities — but passing is illegal
     * once anything has been done.
     */
    var actedThisTurn: Boolean = false
        private set

    // ---------------------------------------------------------------- setup

    companion object {
        fun start(deckA: Deck, deckB: Deck, rng: Random = Random.Default): GameEngine {
            val a = PlayerState(Side.A, deckA, deckA.cards.shuffled(rng).toMutableList())
            val b = PlayerState(Side.B, deckB, deckB.cards.shuffled(rng).toMutableList())
            val state = GameState(a, b, rng)
            val engine = GameEngine(state)
            engine.beginMatch()
            return engine
        }
    }

    private fun beginMatch() {
        // The coin flip. The player going first is compensated with an extra mulligan and
        // their stratagem on the board; the other player's stratagem never enters the game.
        state.starter = if (state.rng.nextBoolean()) Side.A else Side.B
        state.turn = state.starter

        listOf(state.playerA, state.playerB).forEach { p ->
            repeat(STARTING_HAND) { drawOne(p) }
            p.mulligansLeft = MULLIGANS_BY_ROUND.getValue(1)
        }
        val first = state.player(state.starter)
        first.mulligansLeft += 1
        first.deckList.stratagem?.let { strat ->
            first.stratagem = UnitInstance(strat, state.allocateUid())
        }
    }

    // ---------------------------------------------------------------- draw & mulligan

    private fun drawOne(p: PlayerState): Card? {
        val card = p.deck.removeFirstOrNull() ?: return null
        // Hand is capped at 10; anything drawn beyond that is discarded to the graveyard.
        if (p.hand.size >= HAND_LIMIT) {
            p.graveyard += card
            return null
        }
        p.hand += card
        return card
    }

    /** Return a card to the deck and draw a replacement. */
    fun mulligan(side: Side, handIndex: Int): Rejected? {
        val p = state.player(side)
        if (p.mulligansLeft <= 0) return Rejected("no mulligans left")
        val card = p.hand.getOrNull(handIndex) ?: return Rejected("no card at $handIndex")
        p.hand.removeAt(handIndex)
        p.deck += card
        p.deck.shuffle(state.rng)
        drawOne(p)
        p.mulligansLeft--
        return null
    }

    // ---------------------------------------------------------------- actions

    fun legal(side: Side, action: Action): Rejected? {
        if (state.matchOver) return Rejected("match is over")
        if (state.turn != side) return Rejected("not your turn")
        val p = state.player(side)
        if (p.passed) return Rejected("you have passed this round")
        return when (action) {
            is Action.PlayCard -> {
                val card = p.hand.getOrNull(action.handIndex)
                    ?: return Rejected("no card at ${action.handIndex}")
                if (actedThisTurn) return Rejected("already played a card this turn")
                val target = if (card.disloyal) state.opponent(side) else p
                if (card.isUnit && !target.rowHasSpace(action.row)) {
                    Rejected("${action.row} is full (max $ROW_CAPACITY)")
                } else null
            }

            is Action.UseOrder -> {
                val unit = findUnit(side, action.uid) ?: return Rejected("no such unit")
                if (!unit.abilitiesActive) return Rejected("${unit.card.name} is locked")
                if (!unit.orderReady) return Rejected("${unit.card.name} is not ready yet")
                if (unit.cooldownLeft > 0) return Rejected("${unit.card.name} is on cooldown")
                if (unit.charges <= 0) return Rejected("${unit.card.name} has no charges left")
                if (unit.card.abilities.none { it.trigger == Trigger.ORDER }) {
                    Rejected("${unit.card.name} has no Order ability")
                } else null
            }

            is Action.UseLeader ->
                if (p.leaderUsed) Rejected("leader ability already used") else null

            Action.Pass ->
                // Passing requires a clean turn: no card played, no Order or leader used.
                if (actedThisTurn) Rejected("cannot pass after acting this turn") else null

            Action.EndTurn ->
                if (!actedThisTurn) Rejected("nothing done this turn — play a card or pass") else null
        }
    }

    fun perform(side: Side, action: Action): Rejected? {
        legal(side, action)?.let { return it }
        val p = state.player(side)
        when (action) {
            is Action.PlayCard -> {
                val card = p.hand.removeAt(action.handIndex)
                playCard(p, card, action.row, action.position, action.target)
                actedThisTurn = true
            }

            is Action.UseOrder -> {
                val unit = findUnit(side, action.uid)!!
                val ability = unit.card.abilities.first { it.trigger == Trigger.ORDER }
                resolve(p, ability.effect, action.target, unit)
                unit.charges--
                unit.cooldownLeft = ability.cooldown
                unit.orderReady = unit.charges > 0 && ability.cooldown == 0
                actedThisTurn = true
            }

            is Action.UseLeader -> {
                resolve(p, p.leader.ability.effect, action.target, null)
                p.leaderUsed = true
                actedThisTurn = true
            }

            Action.Pass -> {
                p.passed = true
                endTurn(side)
            }

            Action.EndTurn -> endTurn(side)
        }
        return null
    }

    private fun playCard(p: PlayerState, card: Card, row: Row, position: Int, target: Int?) {
        // Disloyal cards are played onto the opponent's side and pick up Spying there.
        val owner = if (card.disloyal) state.opponent(p.side) else p
        if (card.isUnit || card.type == CardType.ARTIFACT) {
            val unit = UnitInstance(card, state.allocateUid())
            if (card.disloyal) unit.apply(Status.SPYING)
            val slot = owner.rows.getValue(row)
            val index = if (position in 0..slot.size) position else slot.size
            slot.add(index, unit)
            // Deploy fires only on a played card, and only if the row clause matches.
            card.abilities
                .filter { it.trigger == Trigger.DEPLOY && (it.row == null || it.row == row) }
                .forEach { resolve(p, it.effect, target, unit) }
        } else {
            // Specials resolve and go straight to the graveyard.
            card.abilities.filter { it.trigger == Trigger.DEPLOY }
                .forEach { resolve(p, it.effect, target, null) }
            p.graveyard += card
        }
        // Anything played triggers allies that watch for it (Harmony, Thrive, Resupply...).
        p.units().filter { it.abilitiesActive }.forEach { ally ->
            ally.card.abilities.filter { it.trigger == Trigger.ON_ALLY_PLAYED }
                .forEach { resolve(p, it.effect, null, ally) }
        }
        cleanupDead()
    }

    // ---------------------------------------------------------------- effects

    private fun findUnit(side: Side, uid: Int): UnitInstance? =
        state.player(side).units().firstOrNull { it.uid == uid }

    private fun anyUnit(uid: Int): UnitInstance? =
        (state.playerA.units() + state.playerB.units()).firstOrNull { it.uid == uid }

    /** Defender forces targeting onto itself while it is on the row. */
    private fun targetable(unit: UnitInstance): Boolean {
        if (unit.has(Status.IMMUNITY)) return false
        val owner = if (state.playerA.units().contains(unit)) state.playerA else state.playerB
        val row = owner.rows.entries.firstOrNull { it.value.contains(unit) }?.key ?: return true
        val defender = owner.rows.getValue(row).firstOrNull { it.has(Status.DEFENDER) }
        return defender == null || defender === unit
    }

    private fun resolve(actor: PlayerState, effect: Effect, targetUid: Int?, self: UnitInstance?) {
        val target = targetUid?.let { anyUnit(it) }?.takeIf { targetable(it) }
        when (effect) {
            is Effect.Boost -> (target ?: self)?.boost(effect.amount)
            is Effect.Strengthen -> (target ?: self)?.let {
                it.basePower += effect.amount
                it.power += effect.amount
            }
            is Effect.Damage -> target?.let { applyDamage(it, effect.amount) }
            is Effect.Heal -> (target ?: self)?.heal(effect.amount)
            Effect.Reset -> (target ?: self)?.reset()
            Effect.Destroy -> target?.let { it.power = 0 }
            Effect.Banish -> target?.let { banish(it) }
            is Effect.Apply -> (target ?: self)?.let { applyStatus(it, effect.status, effect.duration) }
            Effect.Purify -> (target ?: self)?.purify()
            is Effect.Summon -> summon(actor, effect.cardId, rowOf(self) ?: Row.MELEE)
            Effect.Consume -> consume(actor, target, self)
            Effect.Resurrect -> resurrect(actor, rowOf(self) ?: Row.MELEE)
            Effect.Move -> target?.let { moveToOtherRow(it) }
            is Effect.Weather -> layWeather(actor, effect.kind, target)
            Effect.ClearWeather -> state.rowEffects.removeAll { it.side == actor.side }
            is Effect.Draw -> repeat(effect.count) { drawOne(actor) }
            is Effect.Profit -> actor.addCoins(effect.amount)
            Effect.None -> Unit
        }
        cleanupDead()
    }

    /** Which row a unit is standing on, or null if it is not on the board. */
    private fun rowOf(unit: UnitInstance?): Row? {
        if (unit == null) return null
        listOf(state.playerA, state.playerB).forEach { p ->
            p.rows.forEach { (row, units) -> if (units.contains(unit)) return row }
        }
        return null
    }

    /** Put a card on the board without playing it: no Deploy, and the row cap still applies. */
    private fun place(player: PlayerState, card: Card, row: Row): UnitInstance? {
        if (!player.rowHasSpace(row)) return null
        val unit = UnitInstance(card, state.allocateUid())
        player.rows.getValue(row) += unit
        return unit
    }

    /**
     * Summon by id: prefer a copy already in the deck, so summoning thins it as the real game
     * does; fall back to spawning a fresh copy when the deck has none left.
     */
    private fun summon(player: PlayerState, cardId: String, row: Row) {
        val fromDeck = player.deck.indexOfFirst { it.id == cardId }
        val card = if (fromDeck >= 0) player.deck.removeAt(fromDeck) else CardDatabase.byId(cardId) ?: return
        place(player, card, row)
    }

    /** Destroy an allied unit and take its power. Consuming the graveyard banishes instead. */
    private fun consume(player: PlayerState, target: UnitInstance?, self: UnitInstance?) {
        val eater = self ?: return
        if (target != null && target !== eater) {
            eater.boost(maxOf(target.power, 0))
            target.power = 0
            return
        }
        // Nothing on the board to eat: take the top of the graveyard instead, and banish it.
        val corpse = player.graveyard.removeLastOrNull() ?: return
        eater.boost(maxOf(corpse.basePower, 0))
        player.banished += corpse
    }

    /** Return the strongest unit in the graveyard to the board. */
    private fun resurrect(player: PlayerState, row: Row) {
        val best = player.graveyard.filter { it.isUnit }.maxByOrNull { it.basePower } ?: return
        player.graveyard.remove(best)
        place(player, best, row)
    }

    /**
     * Put weather on an enemy row: the target's row when one was chosen, otherwise whichever
     * enemy row is most crowded. Only one effect sits on a row at a time, so it replaces.
     */
    private fun layWeather(actor: PlayerState, kind: RowEffectKind, target: UnitInstance?) {
        val enemy = state.opponent(actor.side)
        val row = target?.let { t -> enemy.rows.entries.firstOrNull { it.value.contains(t) }?.key }
            ?: enemy.rows.maxByOrNull { it.value.size }?.key
            ?: Row.MELEE
        state.rowEffects.removeAll { it.side == enemy.side && it.row == row }
        state.rowEffects += RowEffect(enemy.side, row, kind)
    }

    private fun moveToOtherRow(unit: UnitInstance) {
        val owner = owningPlayer(unit) ?: return
        val from = rowOf(unit) ?: return
        val to = from.other()
        if (!owner.rowHasSpace(to)) return
        owner.rows.getValue(from).remove(unit)
        owner.rows.getValue(to) += unit
    }

    /** Poison destroys a unit that is already poisoned rather than stacking. */
    private fun applyStatus(unit: UnitInstance, status: Status, duration: Int) {
        if (status == Status.POISON && unit.has(Status.POISON)) {
            unit.power = 0
            return
        }
        unit.apply(status, duration)
    }

    private fun applyDamage(unit: UnitInstance, amount: Int) {
        unit.takeDamage(amount)
    }

    private fun banish(unit: UnitInstance) {
        owningPlayer(unit)?.let { owner ->
            owner.rows.values.forEach { it.remove(unit) }
            owner.banished += unit.card
        }
    }

    private fun owningPlayer(unit: UnitInstance): PlayerState? = when {
        state.playerA.units().contains(unit) -> state.playerA
        state.playerB.units().contains(unit) -> state.playerB
        else -> null
    }

    /**
     * Move dead units off the board. Doomed sends them to banishment instead of the graveyard,
     * and a Deathwish fires only on a genuine move to the graveyard.
     */
    private fun cleanupDead() {
        listOf(state.playerA, state.playerB).forEach { p ->
            p.rows.forEach { (_, units) ->
                units.filter { it.isDead }.forEach { dead ->
                    units.remove(dead)
                    if (dead.has(Status.DOOMED)) {
                        p.banished += dead.card
                    } else {
                        p.graveyard += dead.card
                        if (dead.abilitiesActive) {
                            dead.card.abilities.filter { it.trigger == Trigger.DEATHWISH }
                                .forEach { resolve(p, it.effect, null, null) }
                        }
                    }
                    if (dead.has(Status.BOUNTY)) {
                        state.opponent(p.side).addCoins(dead.basePower)
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- turn flow

    private fun endTurn(side: Side) {
        tickEndOfTurn(state.player(side))
        actedThisTurn = false

        // Running out of cards passes you automatically — including on the turn you spend
        // your last card, before control ever reaches the opponent.
        val acting = state.player(side)
        if (acting.hand.isEmpty() && !acting.passed) acting.passed = true

        if (state.roundOver) {
            finishRound()
            return
        }
        // Control passes to the opponent unless they have already passed.
        val next = side.other()
        state.turn = if (state.player(next).passed) side else next

        val mover = state.player(state.turn)
        if (mover.hand.isEmpty() && !mover.passed) {
            mover.passed = true
            if (state.roundOver) { finishRound(); return }
            state.turn = state.turn.other()
        }
        startTurn(state.player(state.turn))
    }

    private fun startTurn(p: PlayerState) {
        applyRowEffects(p)
        p.units().forEach { unit ->
            if (unit.cooldownLeft > 0) unit.cooldownLeft--
            if (unit.cooldownLeft == 0 && unit.charges > 0) unit.orderReady = true
            if (unit.abilitiesActive) {
                unit.card.abilities.filter { it.trigger == Trigger.START_OF_TURN }
                    .forEach { resolve(p, it.effect, null, unit) }
            }
        }
        cleanupDead()
    }

    /** Weather and hazards tick at the start of the owner's turn, on one row at a time. */
    private fun applyRowEffects(p: PlayerState) {
        Row.entries.forEach { row ->
            val kind = state.effectOn(p.side, row) ?: return@forEach
            val units = p.rows.getValue(row).filter { !it.has(Status.IMMUNITY) }
            if (units.isEmpty()) return@forEach
            when (kind) {
                RowEffectKind.FOG -> units.minByOrNull { it.power }?.let { applyDamage(it, 2) }
                RowEffectKind.FROST -> units.maxByOrNull { it.power }?.let { applyDamage(it, 2) }
                RowEffectKind.RAIN -> units.shuffled(state.rng).take(2).forEach { applyDamage(it, 1) }
                RowEffectKind.STORM -> units.forEach { applyDamage(it, 1) }
            }
        }
        cleanupDead()
    }

    /** Bleeding, Vitality and Rupture resolve at the end of the controller's turn. */
    private fun tickEndOfTurn(p: PlayerState) {
        p.units().forEach { unit ->
            unit.statuses[Status.BLEEDING]?.let { turns ->
                applyDamage(unit, 1)
                if (turns <= 1) unit.statuses.remove(Status.BLEEDING)
                else unit.statuses[Status.BLEEDING] = turns - 1
            }
            unit.statuses[Status.VITALITY]?.let { turns ->
                unit.boost(1)
                if (turns <= 1) unit.statuses.remove(Status.VITALITY)
                else unit.statuses[Status.VITALITY] = turns - 1
            }
            if (unit.has(Status.RUPTURE)) {
                applyDamage(unit, unit.basePower)
                if (!unit.isDead) unit.statuses.remove(Status.RUPTURE)
            }
            if (unit.abilitiesActive) {
                unit.card.abilities.filter { it.trigger == Trigger.END_OF_TURN }
                    .forEach { resolve(p, it.effect, null, unit) }
            }
        }
        cleanupDead()
    }

    // ---------------------------------------------------------------- rounds

    private fun finishRound() {
        val a = state.playerA.score()
        val b = state.playerB.score()
        // A tie awards a crown to both players, which can end the match 2-2.
        val winner = when {
            a > b -> Side.A
            b > a -> Side.B
            else -> null
        }
        when (winner) {
            Side.A -> state.playerA.crowns++
            Side.B -> state.playerB.crowns++
            null -> { state.playerA.crowns++; state.playerB.crowns++ }
        }
        state.roundHistory += RoundResult(state.round, a, b, winner)

        if (state.playerA.crowns >= CROWNS_TO_WIN || state.playerB.crowns >= CROWNS_TO_WIN) {
            state.matchOver = true
            state.matchWinner = when {
                state.playerA.crowns > state.playerB.crowns -> Side.A
                state.playerB.crowns > state.playerA.crowns -> Side.B
                else -> null
            }
            return
        }
        beginNextRound(winner)
    }

    private fun beginNextRound(previousWinner: Side?) {
        state.round++
        listOf(state.playerA, state.playerB).forEach { p ->
            // Units leave the board unless they have Resilience, which returns them at base power
            // with boosts and armour stripped.
            p.rows.forEach { (_, units) ->
                val survivors = units.filter { it.has(Status.RESILIENCE) }
                units.filterNot { it.has(Status.RESILIENCE) }.forEach { gone ->
                    if (gone.has(Status.DOOMED)) p.banished += gone.card else p.graveyard += gone.card
                }
                units.clear()
                survivors.forEach { s ->
                    s.statuses.remove(Status.RESILIENCE)
                    s.power = s.basePower
                    s.armor = 0
                    units += s
                }
            }
            p.passed = false
            p.coins /= 2
            repeat(DRAW_PER_ROUND) { drawOne(p) }
            p.mulligansLeft = MULLIGANS_BY_ROUND[state.round] ?: 1
        }
        state.rowEffects.clear()
        // The previous round's winner moves first; after a draw, the round's starter keeps it.
        state.starter = previousWinner ?: state.starter
        state.turn = state.starter
        actedThisTurn = false
    }

    fun result(): MatchResult? = if (state.matchOver) MatchResult(state.matchWinner) else null
}
