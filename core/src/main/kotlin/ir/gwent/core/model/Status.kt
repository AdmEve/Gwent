package ir.gwent.core.model

/**
 * Statuses attached to a unit on the battlefield.
 *
 * Every status here is removable by Purify, and [VEIL] blocks new ones from being applied.
 * Durations matter for the two stacking statuses ([BLEEDING] and [VITALITY]); the rest are
 * either present or absent, which is why duration is tracked per-unit rather than on the enum.
 */
enum class Status {
    /** Absorbs damage before power is lost. Tracked as points, not a flag — see [UnitInstance.armor]. */
    ARMOR,

    /** Lose 1 power at the end of the owner's turn. Stacks in duration. Countered by Vitality. */
    BLEEDING,

    /** Boost by 1 at the end of the owner's turn. Stacks in duration. Countered by Bleeding. */
    VITALITY,

    /** Applying poison to an already-poisoned unit destroys it. */
    POISON,

    /** At end of the owner's turn, damage by base power; if it survives, the status is removed. */
    RUPTURE,

    /** Ignores the first instance of damage, then is removed. */
    SHIELD,

    /** Survives into the next round, at base power — boosts and armour are not carried over. */
    RESILIENCE,

    /** When the card leaves the battlefield it is banished rather than moved. */
    DOOMED,

    /** Cannot be targeted directly. Row and board effects still apply. */
    IMMUNITY,

    /** Ability text is ignored while on the battlefield. Does not affect statuses. */
    LOCKED,

    /** Prevents the unit gaining further statuses. Does not remove existing ones. */
    VEIL,

    /** While present on a row, the opponent cannot target other units on that row. */
    DEFENDER,

    /** Set on a card sitting on the side that does not own it. */
    SPYING,

    /** When destroyed or banished, the opponent gains coins equal to its base power. */
    BOUNTY,
}

/** Statuses that tick at the end of the controller's turn, and so need a duration. */
val STACKING_STATUSES = setOf(Status.BLEEDING, Status.VITALITY)

/** Bleeding and Vitality cancel each other rather than coexisting. */
fun Status.counteredBy(): Status? = when (this) {
    Status.BLEEDING -> Status.VITALITY
    Status.VITALITY -> Status.BLEEDING
    else -> null
}

/**
 * A live card on the battlefield.
 *
 * [Card] is the printed definition and never changes; everything that a match does to a copy of
 * it lives here. Base and current power are kept apart because a large family of mechanics reads
 * the difference between them — Reset, Heal, Inspired, Berserk, Veteran.
 */
data class UnitInstance(
    val card: Card,
    /** Unique per board unit, so two copies of the same bronze can be told apart. */
    val uid: Int,
    /** Mutable base: Strengthen and Veteran change this permanently. */
    var basePower: Int = card.basePower,
    /** Current power including boosts and damage. Zero or less destroys the unit. */
    var power: Int = card.basePower,
    var armor: Int = card.armor,
    /** Active statuses mapped to remaining duration; 0 means indefinite. */
    val statuses: MutableMap<Status, Int> = card.innateStatuses.associateWith { 0 }.toMutableMap(),
    /** Remaining Order uses. */
    var charges: Int = card.abilities.firstOrNull { it.trigger == Trigger.ORDER }?.charges ?: 0,
    /** Turns left before the Order may be used again. */
    var cooldownLeft: Int = 0,
    /** False until the controller's next turn, unless the ability has Zeal. */
    var orderReady: Boolean = card.abilities.any { it.trigger == Trigger.ORDER && it.zeal },
) {
    fun has(status: Status): Boolean = statuses.containsKey(status)

    /** Boosted means current power exceeds base — the condition Inspired reads. */
    val isBoosted: Boolean get() = power > basePower

    val isDamaged: Boolean get() = power < basePower

    /** Ability text is ignored while locked, so the engine must not fire abilities on a locked unit. */
    val abilitiesActive: Boolean get() = !has(Status.LOCKED)

    /** Veil blocks new statuses; it does not remove what is already applied. */
    fun canGain(status: Status): Boolean = !has(Status.VEIL) || status == Status.VEIL

    fun apply(status: Status, duration: Int = 0) {
        if (!canGain(status)) return
        val counter = status.counteredBy()
        if (counter != null && has(counter)) {
            // Bleeding and Vitality cancel out rather than stacking against each other.
            val remaining = (statuses[counter] ?: 0) - duration
            statuses.remove(counter)
            if (remaining > 0) statuses[counter] = remaining
            else if (remaining < 0) statuses[status] = -remaining
            return
        }
        statuses[status] = if (status in STACKING_STATUSES) (statuses[status] ?: 0) + duration else duration
    }

    fun purify() = statuses.clear()

    /** Damage lands on armour first, and only the remainder reduces power. */
    fun takeDamage(amount: Int): Int {
        if (amount <= 0) return 0
        if (has(Status.SHIELD)) {
            statuses.remove(Status.SHIELD)
            return 0
        }
        val absorbed = minOf(armor, amount)
        armor -= absorbed
        val toPower = amount - absorbed
        power -= toPower
        return toPower
    }

    fun boost(amount: Int) {
        if (amount > 0) power += amount
    }

    /** Heal cannot take a unit above its base power. */
    fun heal(amount: Int) {
        if (amount > 0) power = minOf(basePower, power + amount)
    }

    fun reset() {
        power = basePower
    }

    val isDead: Boolean get() = power <= 0
}
