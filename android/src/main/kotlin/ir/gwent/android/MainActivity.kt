package ir.gwent.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ir.gwent.android.ui.BoardScreen
import ir.gwent.android.ui.FactionPickerScreen
import ir.gwent.android.ui.GwentTheme
import ir.gwent.core.model.Faction

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GwentTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }
}

private sealed class Screen {
    data object Pick : Screen()
    data class Match(val playerFaction: Faction, val aiFaction: Faction) : Screen()
}

@Composable
private fun AppRoot() {
    var screen by remember { mutableStateOf<Screen>(Screen.Pick) }
    when (val current = screen) {
        is Screen.Pick -> FactionPickerScreen(onStart = { player, ai -> screen = Screen.Match(player, ai) })
        is Screen.Match -> BoardScreen(
            playerFaction = current.playerFaction,
            aiFaction = current.aiFaction,
            onExit = { screen = Screen.Pick },
        )
    }
}
