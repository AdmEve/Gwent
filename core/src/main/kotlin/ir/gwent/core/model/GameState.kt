package ir.gwent.core.model

enum class Side { A, B }

fun Side.other(): Side = if (this == Side.A) Side.B else Side.A

data class PlayerState(
    val side: Side,
    val faction: Faction,
    val deck: MutableList<Card>,
    val hand: MutableList<Card> = mutableListOf(),
    val discard: MutableList<Card> = mutableListOf(),
    val board: MutableMap<Row, MutableList<Card>> =
        Row.entries.associateWith { mutableListOf<Card>() }.toMutableMap(),
    var roundsWon: Int = 0,
    var passed: Boolean = false,
)

data class RoundResult(val round: Int, val powerA: Int, val powerB: Int, val winner: Side?)

class GameState(val playerA: PlayerState, val playerB: PlayerState) {
    var round: Int = 1
    var turn: Side = Side.A
    var starter: Side = Side.A
    var matchWinner: Side? = null
    val roundHistory: MutableList<RoundResult> = mutableListOf()

    /** Rows currently debuffed by an active weather effect (shared by both players). */
    val weatheredRows: MutableSet<Row> = mutableSetOf()

    fun player(side: Side): PlayerState = if (side == Side.A) playerA else playerB
}
