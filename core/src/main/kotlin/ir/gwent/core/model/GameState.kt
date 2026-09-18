package ir.gwent.core.model

import kotlin.random.Random

enum class Side { A, B }

fun Side.other(): Side = if (this == Side.A) Side.B else Side.A

/** How a finished match ended. [winner] is null for a draw. */
data class MatchResult(val winner: Side?)

data class PlayerState(
    val side: Side,
    val faction: Faction,
    val deck: MutableList<Card>,
    val hand: MutableList<Card> = mutableListOf(),
    val discard: MutableList<Card> = mutableListOf(),
    val board: MutableMap<Row, MutableList<Card>> =
        Row.entries.associateWith { mutableListOf<Card>() }.toMutableMap(),
    /** Gems, as in Gwent: lose two and you lose the match. A drawn round costs both players one. */
    var lives: Int = STARTING_LIVES,
    var roundsWon: Int = 0,
    var passed: Boolean = false,
    val leader: Leader = Leaders.forFaction(faction),
    val trait: FactionTrait = Leaders.traitFor(faction),
    var leaderUsed: Boolean = false,
    /** Cards still swappable before the match starts. */
    var mulligansLeft: Int = MULLIGAN_ALLOWANCE,
)

const val STARTING_LIVES = 2
const val MULLIGAN_ALLOWANCE = 2

data class RoundResult(val round: Int, val powerA: Int, val powerB: Int, val winner: Side?)

class GameState(
    val playerA: PlayerState,
    val playerB: PlayerState,
    /** Kept on the state so trait effects that pick at random stay reproducible under a seed. */
    val rng: Random = Random.Default,
) {
    var round: Int = 1
    var turn: Side = Side.A
    var starter: Side = Side.A

    /**
     * Whether the match has finished. Kept separate from [matchWinner] because a draw is a
     * finished match with no winner — collapsing the two lets callers loop forever on a draw.
     */
    var matchOver: Boolean = false
    var matchWinner: Side? = null

    val roundHistory: MutableList<RoundResult> = mutableListOf()

    /** Rows currently debuffed by an active weather effect (shared by both players). */
    val weatheredRows: MutableSet<Row> = mutableSetOf()

    fun player(side: Side): PlayerState = if (side == Side.A) playerA else playerB
}
