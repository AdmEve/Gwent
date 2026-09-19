package ir.gwent.core.model

import kotlin.random.Random

enum class Side { A, B }

fun Side.other(): Side = if (this == Side.A) Side.B else Side.A

/** How a finished match ended. [winner] is null for a draw. */
data class MatchResult(val winner: Side?)

/** Deck-building limits. Provisions are 150 plus whatever the leader contributes. */
const val BASE_PROVISIONS = 150
const val MIN_DECK_SIZE = 25
const val STARTING_HAND = 10
const val HAND_LIMIT = 10
const val DRAW_PER_ROUND = 3
const val CROWNS_TO_WIN = 2
const val COIN_CAP = 9

/** Mulligans allowed at the start of each round. The player going first gets one extra in round 1. */
val MULLIGANS_BY_ROUND = mapOf(1 to 3, 2 to 1, 3 to 1)

/**
 * A deck as built: a leader, a stratagem and the card list. Validity is checked rather than
 * assumed so that the rules live in one place and tests can assert on them directly.
 */
data class Deck(
    val leader: Leader,
    val stratagem: Card?,
    val cards: List<Card>,
) {
    val faction: Faction get() = leader.faction

    val provisionLimit: Int get() = BASE_PROVISIONS + leader.provisionBonus

    val provisionsSpent: Int get() = cards.sumOf { it.provisions }

    /** True when the deck contains no Neutral cards, which switches on Devotion abilities. */
    val hasDevotion: Boolean get() = cards.none { it.faction == Faction.NEUTRAL }

    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        if (cards.size < MIN_DECK_SIZE) {
            errors += "deck has ${cards.size} cards, minimum is $MIN_DECK_SIZE"
        }
        if (provisionsSpent > provisionLimit) {
            errors += "deck costs $provisionsSpent provisions, limit is $provisionLimit"
        }
        cards.groupBy { it.id }.forEach { (id, copies) ->
            val limit = copies.first().color.copyLimit
            if (copies.size > limit) {
                errors += "$id appears ${copies.size} times, limit is $limit for ${copies.first().color}"
            }
        }
        cards.filterNot { it.faction.playableIn(faction) }.map { it.name }.distinct().forEach {
            errors += "$it does not belong to $faction or Neutral"
        }
        return errors
    }

    val isValid: Boolean get() = validate().isEmpty()
}

class PlayerState(
    val side: Side,
    val deckList: Deck,
    val deck: MutableList<Card>,
    val hand: MutableList<Card> = mutableListOf(),
    val graveyard: MutableList<Card> = mutableListOf(),
    val banished: MutableList<Card> = mutableListOf(),
    /** Two rows per side, each capped at [ROW_CAPACITY]. */
    val rows: MutableMap<Row, MutableList<UnitInstance>> =
        Row.entries.associateWith { mutableListOf<UnitInstance>() }.toMutableMap(),
    /** Round wins. Two crowns takes the match. */
    var crowns: Int = 0,
    var passed: Boolean = false,
    var leaderUsed: Boolean = false,
    /** Syndicate economy; halved (rounded down) between rounds. */
    var coins: Int = 0,
    var mulligansLeft: Int = 0,
    /** Set only for the player going first; otherwise the stratagem never enters the game. */
    var stratagem: UnitInstance? = null,
) {
    val leader: Leader get() = deckList.leader
    val faction: Faction get() = deckList.faction
    val hasDevotion: Boolean get() = deckList.hasDevotion

    fun units(): List<UnitInstance> = rows.values.flatten()

    /** Only units score. Artifacts sit on the board contributing nothing. */
    fun score(): Int = units().filter { it.card.scoresPoints }.sumOf { it.power }

    fun scoreOf(row: Row): Int =
        rows.getValue(row).filter { it.card.scoresPoints }.sumOf { it.power }

    fun rowHasSpace(row: Row): Boolean = rows.getValue(row).size < ROW_CAPACITY

    fun addCoins(amount: Int) {
        coins = (coins + amount).coerceAtMost(COIN_CAP)
    }

    fun spendCoins(amount: Int): Boolean {
        if (coins < amount) return false
        coins -= amount
        return true
    }
}

data class RoundResult(val round: Int, val scoreA: Int, val scoreB: Int, val winner: Side?)

/** Row effects (weather and hazards) attach to one row on one side, not to a row type globally. */
data class RowEffect(val side: Side, val row: Row, val kind: RowEffectKind)

enum class RowEffectKind {
    /** Damage the lowest-power unit in the row by 2. */
    FOG,

    /** Damage the highest-power unit in the row by 2. */
    FROST,

    /** Damage 2 random units in the row by 1. */
    RAIN,

    /** Damage every unit in the row by 1. */
    STORM,
}

class GameState(
    val playerA: PlayerState,
    val playerB: PlayerState,
    /** Held on the state so that random effects stay reproducible under a fixed seed. */
    val rng: Random = Random.Default,
) {
    var round: Int = 1
    var turn: Side = Side.A

    /** Who moves first this round: random in round 1, then the previous round's winner. */
    var starter: Side = Side.A

    var matchOver: Boolean = false
    var matchWinner: Side? = null

    val roundHistory: MutableList<RoundResult> = mutableListOf()

    val rowEffects: MutableList<RowEffect> = mutableListOf()

    /** Monotonic source of [UnitInstance.uid]. */
    private var nextUid: Int = 0

    fun allocateUid(): Int = nextUid++

    fun player(side: Side): PlayerState = if (side == Side.A) playerA else playerB

    fun opponent(side: Side): PlayerState = player(side.other())

    fun effectOn(side: Side, row: Row): RowEffectKind? =
        rowEffects.firstOrNull { it.side == side && it.row == row }?.kind

    /** Both players having passed ends the round. */
    val roundOver: Boolean get() = playerA.passed && playerB.passed
}
