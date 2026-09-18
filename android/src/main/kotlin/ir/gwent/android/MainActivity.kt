package ir.gwent.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.gwent.android.ui.BoardScreen
import ir.gwent.android.ui.DangerRed
import ir.gwent.android.ui.FactionPickerScreen
import ir.gwent.android.ui.GwentTheme
import ir.gwent.android.ui.Ink
import ir.gwent.android.ui.MutedText
import ir.gwent.android.ui.SectionTitle
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
    val context = LocalContext.current
    var crash by remember { mutableStateOf(GwentApp.lastCrash(context)) }

    val pendingCrash = crash
    if (pendingCrash != null) {
        CrashScreen(
            trace = pendingCrash,
            onDismiss = {
                GwentApp.clearLastCrash(context)
                crash = null
            },
        )
        return
    }

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

/** Shown once after a crash so the failure can actually be read and reported. */
@Composable
private fun CrashScreen(trace: String, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("THE APP CRASHED LAST TIME", style = SectionTitle, color = DangerRed)
        Text(
            "Send this text to the developer, then continue.",
            color = MutedText,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Continue to the game") }
        Text(
            text = trace,
            color = MutedText,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}
