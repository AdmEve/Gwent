package ir.gwent.core.cli

import ir.gwent.core.ai.Move
import ir.gwent.core.ai.SimpleAi
import ir.gwent.core.engine.GameEngine
import ir.gwent.core.engine.GameEvent
import ir.gwent.core.model.Card
import ir.gwent.core.model.Faction
import ir.gwent.core.model.GameState
import ir.gwent.core.model.Row
import ir.gwent.core.model.Side

private val HUMAN = Side.A
private val AI = Side.B

fun main() {
    println("=== Gwent-e Shahnameh (prototype) ===")
    println("You play the Pahlavans. The AI plays the Div.")
    println()

    val state = GameEngine.newMatch(Faction.PAHLAVAN, Faction.DIV)

    while (state.matchWinner == null) {
        printBoard(state)
        val events = if (state.turn == HUMAN) humanTurn(state) else aiTurn(state)
        events.forEach { printEvent(it) }
        println()
    }

    println("=== Match over: ${describeWinner(state)} ===")
}

private fun humanTurn(state: GameState): List<GameEvent> {
    val hand = state.player(HUMAN).hand
    if (hand.isEmpty()) {
        println("Your hand is empty, you must pass.")
        return GameEngine.pass(state, HUMAN)
    }

    println("Your hand:")
    hand.forEachIndexed { i, c -> println("  ${i + 1}. ${describeCard(c)}") }
    println("Enter a card number to play, or 'p' to pass:")

    val input = readlnOrNull()?.trim().orEmpty()
    if (input.equals("p", ignoreCase = true)) {
        return GameEngine.pass(state, HUMAN)
    }
    val idx = input.toIntOrNull()
    if (idx == null || idx !in 1..hand.size) {
        println("Invalid input, try again.")
        return emptyList()
    }
    return GameEngine.playCard(state, HUMAN, hand[idx - 1].id)
}

private fun aiTurn(state: GameState): List<GameEvent> {
    return when (val move = SimpleAi.chooseMove(state, AI)) {
        is Move.Pass -> GameEngine.pass(state, AI)
        is Move.PlayCard -> GameEngine.playCard(state, AI, move.cardId)
    }
}

private fun printBoard(state: GameState) {
    println("--- Round ${state.round} | You ${state.playerA.roundsWon} - ${state.playerB.roundsWon} AI ---")
    println("AI board:")
    Row.entries.forEach { row -> println("  $row: ${rowLine(state.playerB.board.getValue(row))} (total ${GameEngine.rowPower(state, Side.B, row)})") }
    println("AI total: ${GameEngine.totalPower(state, Side.B)} | AI hand: ${state.playerB.hand.size} cards")
    println("Your board:")
    Row.entries.forEach { row -> println("  $row: ${rowLine(state.playerA.board.getValue(row))} (total ${GameEngine.rowPower(state, Side.A, row)})") }
    println("Your total: ${GameEngine.totalPower(state, Side.A)}")
}

private fun rowLine(cards: List<Card>): String =
    if (cards.isEmpty()) "-" else cards.joinToString(", ") { it.name }

private fun describeCard(c: Card): String {
    val tag = when {
        c.isHero -> " [HERO]"
        c.ability.name == "HORN" -> " [HORN]"
        c.ability.name == "SCORCH" -> " [SCORCH]"
        else -> ""
    }
    return "${c.name} (${c.row}, power ${c.basePower})$tag"
}

private fun printEvent(event: GameEvent) {
    when (event) {
        is GameEvent.CardPlayed -> println("${event.side} played ${event.card.name}")
        is GameEvent.Scorched -> if (event.destroyed.isNotEmpty()) {
            println("Scorch destroyed: ${event.destroyed.joinToString(", ") { it.name }}")
        }
        is GameEvent.Passed -> println("${event.side} passed")
        is GameEvent.RoundEnded -> println(
            "Round ${event.result.round} ended: You ${event.result.powerA} - ${event.result.powerB} AI. " +
                "Winner: ${event.result.winner?.toString() ?: "draw"}"
        )
        is GameEvent.MatchEnded -> {}
        is GameEvent.InvalidMove -> println("Invalid move: ${event.reason}")
    }
}

private fun describeWinner(state: GameState): String = when (state.matchWinner) {
    Side.A -> "You win!"
    Side.B -> "AI wins."
    null -> "Draw."
}
