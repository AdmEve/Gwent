package ir.gwent.core.model

enum class Row { MELEE, RANGED, SIEGE }

enum class Faction {
    PAHLAVAN,
    DIV,
    MARVEL,
    ONE_PIECE,
    GREEK_MYTH,
}

enum class Ability {
    NONE,
    /** Doubles the power of every non-hero unit in the row this card is played to. */
    HORN,
    /** Destroys the highest-power non-hero unit(s) on the entire board. */
    SCORCH,
    /** Enters play on the OPPONENT's board instead of the caster's; caster draws 2 cards. */
    SPY,
    /** After entering play normally, the caster may return one card from their discard pile to hand. */
    MEDIC,
    /** Swaps a friendly non-hero unit already on the board back to hand, taking its row/spot as a 0-power dummy. */
    DECOY,
    /** Caps every non-hero unit's power at 1 in the row named by [Card.row] until the round ends. */
    WEATHER,
    /** Clears every active weather effect. */
    CLEAR_WEATHER,
}

/**
 * [row] means "the row this card occupies" for units, "the row this debuffs" for WEATHER
 * cards, and is unused (but still present for schema simplicity) for DECOY and CLEAR_WEATHER,
 * whose placement/scope is decided at play time.
 */
data class Card(
    val id: String,
    val name: String,
    val faction: Faction,
    val row: Row,
    val basePower: Int = 0,
    val ability: Ability = Ability.NONE,
    val isHero: Boolean = false,
)
