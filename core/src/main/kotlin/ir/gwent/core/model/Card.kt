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
    /**
     * Tight Bond: units sharing a group multiply each other in the same row — two copies are
     * worth double each, three are worth triple each.
     */
    val bondGroup: String? = null,
    /**
     * Muster: playing this card immediately calls every other card sharing the group out of
     * the owner's deck and hand.
     */
    val musterGroup: String? = null,
)

/** A once-per-match ability, played instead of a card. */
enum class LeaderAbility {
    /** Clears every active weather effect. */
    CLEAR_ALL_WEATHER,
    /** Destroys the opponent's strongest non-hero unit. */
    SCORCH_ENEMY_STRONGEST,
    /** Draws a card from your own deck. */
    DRAW_CARD,
    /** Doubles your strongest row, as a Commander's Horn would. */
    HORN_STRONGEST_ROW,
    /** Brings harsh weather down on whichever enemy row is strongest. */
    WEATHER_ENEMY_STRONGEST_ROW,
}

data class Leader(
    val id: String,
    val name: String,
    val faction: Faction,
    val ability: LeaderAbility,
    val description: String,
)

/** The passive each army carries all match, in the spirit of Gwent's faction perks. */
enum class FactionTrait {
    /** Draw a card after winning a round. */
    DRAW_ON_ROUND_WIN,
    /** Keep one random unit on the board when the round ends. */
    KEEP_RANDOM_UNIT,
    /** Win rounds that end level. */
    WIN_TIES,
    /** Always opens the match, whatever the coin says. */
    ALWAYS_OPENS,
    /** Return a random card from the graveyard to hand when round three begins. */
    RECOVER_AT_ROUND_THREE,
}
