package ir.gwent.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.gwent.core.engine.GameEngine
import ir.gwent.core.model.Side

/**
 * The redraw step at the start of each round.
 *
 * The count is not fixed: the player going first gets an extra, and it shrinks in later rounds,
 * so the screen shows what is actually left rather than a hardcoded number.
 */
@Composable
fun MulliganScreen(engine: GameEngine, onDone: () -> Unit) {
    var revision by remember { mutableIntStateOf(0) }
    val me = engine.state.playerA
    @Suppress("UNUSED_EXPRESSION") revision

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BoardDeep, BoardMid, BoardDeep)))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("REDRAW", style = DisplayTitle.copy(fontSize = 26.sp))
        Text(
            text = if (me.mulligansLeft > 0) {
                "Tap a card to put it back and draw another. ${me.mulligansLeft} left."
            } else {
                "No redraws left."
            },
            style = BodyText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp).testTag("mulligan-count"),
        )

        if (engine.state.starter == Side.A) {
            Text(
                "You move first, so you get an extra redraw and your stratagem on the board.",
                style = BodyText.copy(fontSize = 10.sp, color = GoldMuted),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(12.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth().weight(1f).testTag("mulligan-hand"),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(me.hand.size) { i ->
                HandCardView(
                    card = me.hand[i],
                    playable = me.mulligansLeft > 0,
                    onClick = {
                        if (engine.mulligan(Side.A, i) == null) revision++
                    },
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF2A2210))
                .border(1.dp, GoldBright, RoundedCornerShape(4.dp))
                .clickable { onDone() }
                .testTag("mulligan-done"),
            contentAlignment = Alignment.Center,
        ) {
            Text("TO BATTLE", style = SectionTitle.copy(color = GoldLight))
        }
    }
}
