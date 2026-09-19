package ir.gwent.core.model

/**
 * The battlefield has two rows per side. Melee sits against the centre line, Ranged behind it.
 * (Standalone GWENT dropped the Siege row of the Witcher 3 mini-game in the Homecoming update.)
 */
enum class Row { MELEE, RANGED }

fun Row.other(): Row = if (this == Row.MELEE) Row.RANGED else Row.MELEE

/** Maximum units a single row can hold. Nothing may be placed or moved into a full row. */
const val ROW_CAPACITY = 9

/**
 * Card colour, which governs deck legality rather than power: a deck may hold two copies of any
 * bronze and only one of any gold. Gold carries no combat immunity — that was the Witcher 3
 * "hero" rule. Protection in GWENT is explicit: Shield, Veil, Immunity, Defender.
 */
enum class CardColor { BRONZE, GOLD }

/** The maximum copies of a card a legal deck may contain, by colour. */
val CardColor.copyLimit: Int
    get() = when (this) {
        CardColor.BRONZE -> 2
        CardColor.GOLD -> 1
    }

enum class Faction {
    NEUTRAL,
    MONSTERS,
    NILFGAARD,
    NORTHERN_REALMS,
    SCOIATAEL,
    SKELLIGE,
    SYNDICATE,
}

/** A card is deck-legal if it matches the leader's faction or is Neutral. */
fun Faction.playableIn(deckFaction: Faction): Boolean =
    this == Faction.NEUTRAL || this == deckFaction

/**
 * Categories printed on a card. These are load-bearing, not flavour: Harmony counts distinct
 * primary tags, Crew looks for adjacent Soldiers, Resupply counts Warfare, Intimidate counts Crime.
 */
enum class Tag {
    // unit kinds
    HUMAN, ELF, DWARF, DRYAD, GNOME, BEAST, INSECTOID, VAMPIRE, SPECTER, CONSTRUCT,
    DRACONID, NECROPHAGE, OGROID, RELICT, CURSED, WITCHER, MAGE, PIRATE, SOLDIER,
    KNIGHT, ARISTOCRAT, BANDIT, DRUID, TREANT, MACHINE, SIEGE_ENGINE, LEADER_TAG,
    // special-card kinds
    TACTIC, SPELL, WARFARE, ALCHEMY, ORGANIC, CRIME, NATURE, ITEM,
}

/** What kind of card this is, which decides where it goes and whether it scores. */
enum class CardType {
    /** Has power, occupies a row slot, contributes to the score. */
    UNIT,

    /** Resolves and goes straight to the graveyard; never sits on the board. */
    SPECIAL,

    /** Sits on the board, scores nothing, and ordinary cards cannot damage it. */
    ARTIFACT,

    /** One per deck. Deployed only when going first; a non-interactable Order. */
    STRATAGEM,

    /** Multi-stage card that advances through its chapters on a stated condition. */
    SCENARIO,
}

/**
 * A keyword ability. Rather than a closed enum of concrete effects (the mini-game's approach),
 * an ability pairs a [trigger] describing *when* it fires with an [effect] describing *what*
 * it does, so new cards are data rather than new engine branches.
 */
data class Ability(
    val trigger: Trigger,
    val effect: Effect,
    /** Row restriction: a `Deploy (Melee)` clause only fires from the melee row. */
    val row: Row? = null,
    /** Order abilities only: how many times it may be used. */
    val charges: Int = 1,
    /** Order abilities only: turns before it may be used again. */
    val cooldown: Int = 0,
    /** Order abilities only: usable the turn the card lands. */
    val zeal: Boolean = false,
)

/** When an ability fires. */
enum class Trigger {
    /** The card was played from hand. Explicitly not fired by Summon. */
    DEPLOY,

    /** Manually activated by the player; unusable the turn it lands unless it has Zeal. */
    ORDER,

    /** The card moved to the graveyard. */
    DEATHWISH,

    /** Fires at the end of the controller's turn. */
    END_OF_TURN,

    /** Fires at the start of the controller's turn. */
    START_OF_TURN,

    /** Fires when this card's controller plays another card. */
    ON_ALLY_PLAYED,

    /** Passive: continuously true while on the board. */
    PASSIVE,
}

/**
 * What an ability does. Targets are resolved by the engine, not encoded here, so that the same
 * effect can be driven by a player choice, by the AI, or by a scripted test.
 */
sealed interface Effect {
    /** Raise current power. Boost is temporary — stripped by Reset and not carried between rounds. */
    data class Boost(val amount: Int) : Effect

    /** Raise base power permanently. */
    data class Strengthen(val amount: Int) : Effect

    /** Reduce current power; armour absorbs it first. */
    data class Damage(val amount: Int) : Effect

    /** Boost back up to, at most, base power. */
    data class Heal(val amount: Int) : Effect

    /** Return to base power, removing boosts or damage. */
    data object Reset : Effect

    /** Send a unit to the graveyard. */
    data object Destroy : Effect

    /** Remove from the game entirely. Does not count as destroyed, so Deathwish does not fire. */
    data object Banish : Effect

    /** Apply a status for the given duration (0 = indefinite). */
    data class Apply(val status: Status, val duration: Int = 0) : Effect

    /** Strip every status from a card. */
    data object Purify : Effect

    /** Put a specific card onto the board. Does not count as played. */
    data class Summon(val cardId: String) : Effect

    /** Draw cards from the top of the deck. */
    data class Draw(val count: Int) : Effect

    /** Gain Syndicate coins, capped at [COIN_CAP]. */
    data class Profit(val amount: Int) : Effect

    /** Do nothing. Used by vanilla units so every card can share one shape. */
    data object None : Effect
}

/**
 * A card as printed: immutable reference data. What happens to a copy of it during a match lives
 * on [UnitInstance] instead, so the same definition can back any number of board units.
 */
data class Card(
    val id: String,
    val name: String,
    val faction: Faction,
    val type: CardType = CardType.UNIT,
    val color: CardColor = CardColor.BRONZE,
    /** Deck-building cost. No effect in play, except for cards that read it (e.g. Thrive). */
    val provisions: Int = 4,
    val basePower: Int = 0,
    val armor: Int = 0,
    val tags: Set<Tag> = emptySet(),
    val abilities: List<Ability> = emptyList(),
    /** Statuses the card enters play with, e.g. a Doomed token. */
    val innateStatuses: Set<Status> = emptySet(),
    /** Disloyal cards are played onto the opponent's side of the battlefield. */
    val disloyal: Boolean = false,
    val text: String = "",
) {
    /** The primary tag drives Harmony, which counts distinct primary tags on your side. */
    val primaryTag: Tag? get() = tags.firstOrNull()

    val isUnit: Boolean get() = type == CardType.UNIT
    val scoresPoints: Boolean get() = type == CardType.UNIT
}
