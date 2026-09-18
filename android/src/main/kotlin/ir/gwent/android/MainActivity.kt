package ir.gwent.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.gwent.core.ai.Move
import ir.gwent.core.ai.SimpleAi
import ir.gwent.core.engine.GameEngine
import ir.gwent.core.model.Faction
import ir.gwent.core.model.GameState
import ir.gwent.core.model.Row as GwentRow
import ir.gwent.core.model.Side

private val HUMAN = Side.A
private val AI = Side.B

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GwentScreen()
                }
            }
        }
    }
}

@Composable
fun GwentScreen() {
    var state by remember { mutableStateOf(GameEngine.newMatch(Faction.PAHLAVAN, Faction.DIV)) }
    // GameState is mutated in place by the engine; bump this after every action so Compose
    // (which otherwise only reacts to `state` being reassigned) knows to recompose.
    var tick by remember { mutableStateOf(0) }

    fun runAiIfNeeded() {
        while (state.turn == AI && state.matchWinner == null) {
            when (val move = SimpleAi.chooseMove(state, AI)) {
                is Move.Pass -> GameEngine.pass(state, AI)
                is Move.PlayCard -> GameEngine.playCard(state, AI, move.cardId)
            }
        }
    }

    @Suppress("UNUSED_EXPRESSION")
    tick // read so this composable recomposes whenever it's bumped

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Round ${state.round} — You ${state.playerA.roundsWon} : ${state.playerB.roundsWon} AI")

        Text("AI (${state.playerB.hand.size} cards left)", style = MaterialTheme.typography.titleMedium)
        GwentRow.entries.forEach { row ->
            Text("$row: ${GameEngine.rowPower(state, Side.B, row)}")
        }

        Text("You", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        GwentRow.entries.forEach { row ->
            Text("$row: ${GameEngine.rowPower(state, Side.A, row)}")
        }

        Text("Your hand", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        LazyRow {
            items(state.player(HUMAN).hand) { card ->
                Card(
                    modifier = Modifier
                        .padding(4.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(card.name)
                        Text("${card.row} · ${card.basePower}")
                        Button(onClick = {
                            GameEngine.playCard(state, HUMAN, card.id)
                            runAiIfNeeded()
                            tick++
                        }) {
                            Text("Play")
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 16.dp)) {
            Button(onClick = {
                GameEngine.pass(state, HUMAN)
                runAiIfNeeded()
                tick++
            }) {
                Text("Pass")
            }
            Button(onClick = {
                state = GameEngine.newMatch(Faction.PAHLAVAN, Faction.DIV)
                tick++
            }) {
                Text("New match")
            }
        }

        state.matchWinner?.let {
            Text(
                text = when (it) {
                    Side.A -> "You win!"
                    Side.B -> "AI wins."
                },
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
