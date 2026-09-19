package ir.gwent.core.ai

import ir.gwent.core.engine.GameEngine

/**
 * An opponent.
 *
 * Kept as an interface so that a stronger AI can be measured against a weaker one rather than
 * merely asserted to be better: [GreedyAi] is the baseline, [TacticalAi] has to beat it.
 */
interface Ai {
    /** Take the current turn. Returns false when the AI has finished acting. */
    fun takeTurn(engine: GameEngine): Boolean

    /** Spend the redraws available at the start of a round. */
    fun mulligan(engine: GameEngine)
}
