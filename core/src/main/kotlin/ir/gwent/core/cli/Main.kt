package ir.gwent.core.cli

import ir.gwent.core.ai.Move
import ir.gwent.core.ai.SimpleAi
import ir.gwent.core.engine.GameEngine
import ir.gwent.core.engine.GameEvent
import ir.gwent.core.engine.PlayTarget
import ir.gwent.core.model.Ability
import ir.gwent.core.model.Card
import ir.gwent.core.model.Faction
import ir.gwent.core.model.GameState
import ir.gwent.core.model.Row
import ir.gwent.core.model.Side

private val HUMAN = Side.A
private val AI = Side.B

fun main() {
    println("=== Gwent-verse (prototype) ===")
    println("You play the Pahlavans. The AI plays the Div.")
    println()

    val state = GameEngine.newMatch(Faction.PAHLAVAN, Faction.DIV)

    while (!state.matchOver) {
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
    val card = hand[idx - 1]
    val target = promptTarget(state, card)
    return GameEngine.playCard(state, HUMAN, card.id, target)
}

private fun promptTarget(state: GameState, card: Card): PlayTarget? = when (card.ability) {
    Ability.DECOY -> {
        val candidates = Row.entries.flatMap { state.player(HUMAN).board.getValue(it) }.filter { !it.isHero }
        if (candidates.isEmpty()) {
            println("No eligible board card to swap; Decoy will be rejected.")
            null
        } else {
            println("Choose a friendly unit to swap back to hand:")
            candidates.forEachIndexed { i, c -> println("  ${i + 1}. ${describeCard(c)}") }
            val idx = readlnOrNull()?.trim()?.toIntOrNull()
            val chosen = idx?.let { candidates.getOrNull(it - 1) }
            chosen?.let { PlayTarget.DecoyTarget(it.id) }
        }
    }
    Ability.MEDIC -> {
        val discard = state.player(HUMAN).discard
        if (discard.isEmpty()) {
            null
        } else {
            println("Choose a card to revive from discard (or blank to skip):")
            discard.forEachIndexed { i, c -> println("  ${i + 1}. ${describeCard(c)}") }
            val input = readlnOrNull()?.trim().orEmpty()
            val idx = input.toIntOrNull()
            PlayTarget.MedicRevive(idx?.let { discard.getOrNull(it - 1)?.id })
        }
    }
    else -> null
}

private fun aiTurn(state: GameState): List<GameEvent> {
    return when (val move = SimpleAi.chooseMove(state, AI)) {
        is Move.Pass -> GameEngine.pass(state, AI)
        is Move.UseLeader -> GameEngine.useLeader(state, AI)
        is Move.PlayCard -> GameEngine.playCard(state, AI, move.cardId, move.target)
    }
}

private fun printBoard(state: GameState) {
    val weather = if (state.weatheredRows.isEmpty()) "clear" else state.weatheredRows.joinToString(", ")
    println("--- Round ${state.round} | You ${state.playerA.roundsWon} - ${state.playerB.roundsWon} AI | Weather: $weather ---")
    println("AI board:")
    Row.entries.forEach { row -> println("  $row: ${rowLine(state.playerB.board.getValue(row))} (total ${GameEngine.rowPower(state, Side.B, row)})") }
    println("AI total: ${GameEngine.totalPower(state, Side.B)} | AI hand: ${state.playerB.hand.size} | deck: ${state.playerB.deck.size} | discard: ${state.playerB.discard.size}")
    println("Your board:")
    Row.entries.forEach { row -> println("  $row: ${rowLine(state.playerA.board.getValue(row))} (total ${GameEngine.rowPower(state, Side.A, row)})") }
    println("Your total: ${GameEngine.totalPower(state, Side.A)} | deck: ${state.playerA.deck.size} | discard: ${state.playerA.discard.size}")
}

private fun rowLine(cards: List<Card>): String =
    if (cards.isEmpty()) "-" else cards.joinToString(", ") { it.name }

private fun describeCard(c: Card): String {
    val tag = when {
        c.isHero -> " [HERO]"
        c.ability == Ability.NONE -> ""
        else -> " [${c.ability}]"
    }
    val power = if (c.ability == Ability.WEATHER || c.ability == Ability.CLEAR_WEATHER) "" else ", power ${c.basePower}"
    return "${c.name} (${c.row}$power)$tag"
}

private fun printEvent(event: GameEvent) {
    when (event) {
        is GameEvent.CardPlayed -> println("${event.side} played ${event.card.name}")
        is GameEvent.Scorched -> if (event.destroyed.isNotEmpty()) {
            println("Scorch destroyed: ${event.destroyed.joinToString(", ") { it.name }}")
        }
        is GameEvent.WeatherChanged -> println("${event.row} is now under harsh weather (power capped at 1)")
        is GameEvent.WeatherCleared -> println("Weather cleared")
        is GameEvent.Decoyed -> println("${event.side} swapped ${event.returned.name} back to hand via ${event.decoy.name}")
        is GameEvent.MedicRevived -> println("${event.side} revived ${event.revived.name} from discard")
        is GameEvent.SpyInfiltrated -> println("${event.side} sent ${event.card.name} to spy on the enemy row, drawing ${event.cardsDrawn} cards")
        is GameEvent.Passed -> println("${event.side} passed")
        is GameEvent.RoundEnded -> println(
            "Round ${event.result.round} ended: You ${event.result.powerA} - ${event.result.powerB} AI. " +
                "Winner: ${event.result.winner?.toString() ?: "draw"}"
        )
        is GameEvent.RoundStarted -> println("Round ${event.round} begins")
        is GameEvent.Mustered -> println("Mustered: ${event.called.joinToString(", ") { it.name }}")
        is GameEvent.LeaderUsed -> println("${event.side} used ${event.leader.name}: ${event.leader.description}")
        is GameEvent.Mulliganed -> println("${event.side} swapped ${event.returned.name} for ${event.drawn?.name ?: "nothing"}")
        is GameEvent.TraitTriggered -> println("${event.side} faction trait: ${event.trait}")
        is GameEvent.MatchEnded -> {}
        is GameEvent.InvalidMove -> println("Invalid move: ${event.reason}")
    }
}

private fun describeWinner(state: GameState): String = when (state.matchWinner) {
    Side.A -> "You win!"
    Side.B -> "AI wins."
    null -> "Draw."
}
