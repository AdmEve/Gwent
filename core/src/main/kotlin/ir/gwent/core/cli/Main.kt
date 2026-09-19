package ir.gwent.core.cli

import ir.gwent.core.ai.TacticalAi
import ir.gwent.core.engine.Action
import ir.gwent.core.engine.GameEngine
import ir.gwent.core.model.*
import kotlin.random.Random

/**
 * A headless match runner. Useful for sanity-checking the rules without the Android UI:
 * `./gradlew :core:run --args="--seed 7"`.
 */
fun main(args: Array<String>) {
    val seed = args.indexOf("--seed").takeIf { it >= 0 }
        ?.let { args.getOrNull(it + 1)?.toLongOrNull() } ?: System.currentTimeMillis()
    val rng = Random(seed)

    val leaderA = Leaders.FOLTEST
    val leaderB = Leaders.EREDIN
    val deckA = CardDatabase.starterDeck(leaderA)
    val deckB = CardDatabase.starterDeck(leaderB)

    println("GWENT — seed $seed")
    listOf(deckA, deckB).forEach { d ->
        val errs = d.validate()
        println(
            "  ${d.leader.name.padEnd(22)} ${d.cards.size} cards, " +
                "${d.provisionsSpent}/${d.provisionLimit} provisions" +
                if (errs.isEmpty()) "" else "  INVALID: $errs"
        )
    }
    println()

    val engine = GameEngine.start(deckA, deckB, rng)
    val aiA = TacticalAi(Side.A)
    val aiB = TacticalAi(Side.B)
    aiA.mulligan(engine)
    aiB.mulligan(engine)
    println("first move: ${engine.state.starter}")

    var guard = 0
    var lastRound = 0
    while (!engine.state.matchOver && guard++ < 2000) {
        val mover = engine.state.turn
        val ai = if (mover == Side.A) aiA else aiB
        val acted = ai.takeTurn(engine)
        if (!acted && engine.state.playerA.passed && engine.state.playerB.passed) {
            // finishRound() runs inside the engine; loop continues into the next round.
        }
        engine.state.roundHistory.lastOrNull()?.let { r ->
            if (r.round != lastRound) {
                lastRound = r.round
                val verdict = r.winner?.let { "$it wins" } ?: "draw — both take a crown"
                println("round ${r.round}: A ${r.scoreA} — B ${r.scoreB}   ($verdict)")
            }
        }
    }

    println()
    val result = engine.result()
    println(
        when (val w = result?.winner) {
            null -> "match drawn ${engine.state.playerA.crowns}—${engine.state.playerB.crowns}"
            else -> "$w wins the match ${engine.state.playerA.crowns}—${engine.state.playerB.crowns}"
        }
    )
}
