package ir.gwent.android.ui

import androidx.compose.ui.test.assertIsEnabled
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
 *
 * Assertions use assertExists rather than assertIsDisplayed: off-device there is no real
 * window, so "is this laid out on screen" is not a question Robolectric answers reliably,
 * while "did this screen compose and put the node in the tree" is exactly what we want.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w411dp-h891dp")
class UiSmokeTest {

    @get:Rule
    val compose = createComposeRule()

    private fun exists(text: String): Boolean =
        compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun `faction picker renders`() {
        compose.setContent { GwentTheme { FactionPickerScreen { _, _ -> } } }
        compose.onNodeWithText("GWENT-VERSE").assertExists()
        compose.onNodeWithText("TO BATTLE").assertExists()
        compose.onNodeWithText("Marvel").assertExists()
    }

    @Test
    fun `a match opens on the redraw and then shows the board`() {
        compose.setContent {
            GwentTheme { BoardScreen(playerFaction = Faction.ONE_PIECE, aiFaction = Faction.GREEK_MYTH) {} }
        }

        compose.onNodeWithText("OPENING HAND").assertExists()
        compose.onNodeWithText("BEGIN THE MATCH").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Pass round").assertExists()
        compose.onNodeWithText("One Piece").assertExists()
        compose.onNodeWithText("Greek Myth").assertExists()
    }

    @Test
    fun `passing repeatedly ends the match and shows a result`() {
        compose.setContent {
            GwentTheme { BoardScreen(playerFaction = Faction.MARVEL, aiFaction = Faction.DIV) {} }
        }
        compose.onNodeWithText("BEGIN THE MATCH").performClick()
        compose.waitForIdle()

        // A match is at most three rounds, so a handful of passes always finishes one. If the
        // turn loop ever hangs, this never returns and the test times out instead of passing.
        repeat(10) {
            if (exists("Pass round")) {
                runCatching { compose.onNodeWithText("Pass round").performClick() }
                compose.waitForIdle()
            }
        }

        val finished = listOf("VICTORY", "DEFEAT", "DRAW").any { exists(it) }
        assertTrue("match should have finished with a result overlay", finished)
    }

    @Test
    fun `the leader ability is offered once and then spent`() {
        compose.setContent {
            GwentTheme { BoardScreen(playerFaction = Faction.MARVEL, aiFaction = Faction.DIV) {} }
        }
        compose.onNodeWithText("BEGIN THE MATCH").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Nick Fury").assertExists()
        compose.onNodeWithText("Use").assertIsEnabled()
        compose.onNodeWithText("Use").performClick()
        compose.waitForIdle()

        assertTrue("the spent leader should say so", exists("Already used this match"))
    }
}
