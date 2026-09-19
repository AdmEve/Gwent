package ir.gwent.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import ir.gwent.android.ui.BoardScreen
import ir.gwent.android.ui.FactionPickerScreen
import ir.gwent.android.ui.GwentTheme
import ir.gwent.android.ui.MulliganScreen
import ir.gwent.core.ai.SimpleAi
import ir.gwent.core.engine.GameEngine
import ir.gwent.core.model.CardDatabase
import ir.gwent.core.model.Leader
import ir.gwent.core.model.Side

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GwentTheme {
                Surface(modifier = Modifier.fillMaxSize()) { AppRoot() }
            }
        }
    }
}

private enum class Phase { PICK, MULLIGAN, BOARD }

@Composable
fun AppRoot() {
    var phase by remember { mutableStateOf(Phase.PICK) }
    var engine by remember { mutableStateOf<GameEngine?>(null) }
    var opponent by remember { mutableStateOf<SimpleAi?>(null) }
    // The engine mutates in place, so the board needs an explicit nudge to recompose.
    var revision by remember { mutableIntStateOf(0) }

    when (phase) {
        Phase.PICK -> FactionPickerScreen { mine: Leader, theirs: Leader ->
            val e = GameEngine.start(
                CardDatabase.starterDeck(mine),
                CardDatabase.starterDeck(theirs),
            )
            val ai = SimpleAi(Side.B)
            ai.mulligan(e)
            engine = e
            opponent = ai
            phase = Phase.MULLIGAN
        }

        Phase.MULLIGAN -> engine?.let { e ->
            MulliganScreen(e) {
                phase = Phase.BOARD
                // If the opponent won the coin flip, let them open before handing over control.
                runOpponent(e, opponent)
                revision++
            }
        }

        Phase.BOARD -> engine?.let { e ->
            BoardScreen(
                engine = e,
                revision = revision,
                onExit = {
                    engine = null
                    opponent = null
                    phase = Phase.PICK
                },
            )
            // After every player action, let the AI take its turns until control returns.
            LaunchedEffect(revision, e.state.turn, e.state.matchOver) {
                if (runOpponent(e, opponent)) revision++
            }
        }
    }
}

/** Runs the AI while it is its turn. Returns true if anything happened. */
private fun runOpponent(engine: GameEngine, ai: SimpleAi?): Boolean {
    if (ai == null) return false
    var acted = false
    var guard = 0
    while (!engine.state.matchOver && engine.state.turn == Side.B && guard++ < 50) {
        val before = engine.state.turn
        ai.takeTurn(engine)
        acted = true
        // takeTurn can leave the turn with B when A has already passed; break rather than spin.
        if (engine.state.turn == before && engine.state.player(Side.B).passed) break
    }
    return acted
}
