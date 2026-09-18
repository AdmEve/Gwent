package ir.gwent.android.ui

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import ir.gwent.android.GwentApp
import ir.gwent.core.model.Faction
import org.junit.Assert.assertTrue
import org.junit.Before
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
 *
 * The board now catches engine failures so a bug cannot kill the process, which means a
 * failure would otherwise be invisible here too — so every board test ends by asserting
 * nothing was caught, and prints the trace and the move trail when something was.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w411dp-h891dp")
class UiSmokeTest {

    @get:Rule
    val compose = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun clearDiagnostics() {
        GwentApp.clearLastCrash(context)
    }

    private fun exists(text: String): Boolean =
        compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    /** Fails with the swallowed stack trace and the move trail, which is the whole point. */
    private fun assertNothingWasCaught() {
        val caught = GwentApp.lastCrash(context)
        assertTrue(
            "an engine call failed during the match:\n$caught\n\nmove trail:\n${GwentApp.trail(context)}",
            caught == null,
        )
    }

    @Test
    fun `faction picker renders`() {
        compose.setContent { GwentTheme { FactionPickerScreen { _, _ -> } } }
        compose.onNodeWithText("GWENT-VERSE").assertExists()
        compose.onNodeWithText("TO BATTLE").assertExists()
        // Each faction is listed twice, once per side, so match on any rather than exactly one.
        assertTrue("the factions should be listed", exists("Marvel"))
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
        assertNothingWasCaught()
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

        assertNothingWasCaught()
        val finished = listOf("VICTORY", "DEFEAT", "DRAW").any { exists(it) }
        assertTrue(
            "match should have finished with a result overlay; trail:\n${GwentApp.trail(context)}",
            finished,
        )
    }

    @Test
    fun `the leader ability is offered once and then spent`() {
        compose.setContent {
            GwentTheme { BoardScreen(playerFaction = Faction.MARVEL, aiFaction = Faction.DIV) {} }
        }
        compose.onNodeWithText("BEGIN THE MATCH").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Nick Fury").assertExists()
        compose.onNodeWithText("Use").performClick()
        compose.waitForIdle()

        assertNothingWasCaught()
        assertTrue(
            "the spent leader should say so; trail:\n${GwentApp.trail(context)}",
            exists("Already used this match"),
        )
    }
}
