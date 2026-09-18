package ir.gwent.android.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ir.gwent.core.model.Faction
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Composition-level smoke tests. These exist because the app has no device in its loop: a
 * screen that throws on first frame, or a turn loop that never terminates, otherwise only
 * shows up as the app vanishing on someone's phone.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class UiSmokeTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `faction picker renders`() {
        compose.setContent { GwentTheme { FactionPickerScreen { _, _ -> } } }
        compose.onNodeWithText("GWENT-VERSE").assertIsDisplayed()
        compose.onNodeWithText("TO BATTLE").assertIsDisplayed()
    }

    @Test
    fun `a board composes and shows both armies`() {
        compose.setContent {
            GwentTheme { BoardScreen(playerFaction = Faction.ONE_PIECE, aiFaction = Faction.GREEK_MYTH) {} }
        }
        compose.onNodeWithText("Pass round").assertIsDisplayed()
        compose.onNodeWithText("One Piece").assertIsDisplayed()
        compose.onNodeWithText("Greek Myth").assertIsDisplayed()
    }

    @Test
    fun `passing repeatedly ends the match and shows a result`() {
        compose.setContent {
            GwentTheme { BoardScreen(playerFaction = Faction.MARVEL, aiFaction = Faction.DIV) {} }
        }

        // Three rounds at most, so a handful of passes always finishes a match. If the turn
        // loop ever hangs, this call never returns and the test times out rather than passing.
        repeat(8) {
            val pass = compose.onAllNodesWithText("Pass round").fetchSemanticsNodes()
            if (pass.isNotEmpty()) {
                runCatching { compose.onNodeWithText("Pass round").performClick() }
            }
            compose.waitForIdle()
        }

        val finished = listOf("VICTORY", "DEFEAT", "DRAW").any { label ->
            compose.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue("match should have finished with a result overlay", finished)
    }
}
