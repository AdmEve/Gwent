package ir.gwent.android.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import ir.gwent.core.model.Faction
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the real screens to PNG files so the UI can be reviewed without installing the APK
 * on a phone every time. Robolectric's native graphics backend rasterises the same Compose
 * drawing code the device runs, so what lands in build/screenshots is the actual shipped UI,
 * not a mock-up.
 *
 * Output: PNGs under android/build/screenshots, picked up by CI and attached to the release.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val outputDir = File("build/screenshots").apply { mkdirs() }

    private fun show(content: @Composable () -> Unit) {
        compose.setContent { GwentTheme { content() } }
        compose.waitForIdle()
    }

    /** Lays the content out at full screen size and draws it into a bitmap. */
    private fun shoot(name: String) {
        compose.waitForIdle()
        val root: View = compose.activity.findViewById(android.R.id.content)
        val metrics = compose.activity.resources.displayMetrics
        var width = root.width
        var height = root.height

        // Off-device the window is not always laid out for us; do it by hand when it wasn't.
        if (width <= 0 || height <= 0) {
            width = metrics.widthPixels
            height = metrics.heightPixels
            root.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
            )
            root.layout(0, 0, width, height)
            compose.waitForIdle()
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(AndroidColor.BLACK)
        root.draw(canvas)

        val file = File(outputDir, "$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue("screenshot $name should not be empty", file.length() > 2_000L)
        println("wrote ${file.absolutePath} (${file.length()} bytes)")
    }

    private fun exists(text: String): Boolean =
        compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()


    /**
     * Taps a card in the fanned hand. The hand has no stable text to match on — the deck is
     * shuffled — so it is found by position: a clickable node sitting in the bottom band of
     * the screen, which is where the hand lives and nothing else does.
     */
    private fun tapAHandCard() {
        val screenHeight = compose.activity.resources.displayMetrics.heightPixels.toFloat()
        val clickable = compose.onAllNodes(hasClickAction())
        val nodes = clickable.fetchSemanticsNodes()
        val index = nodes.indices.lastOrNull { nodes[it].boundsInRoot.top > screenHeight * 0.80f }
            ?: return
        runCatching { clickable[index].performClick() }
    }

    @Test
    fun `01 faction picker`() {
        show { FactionPickerScreen { _, _ -> } }
        shoot("01-faction-picker")

        // And again with both armies chosen, so the selected state is visible too.
        runCatching { compose.onAllNodesWithText("Marvel")[0].performScrollTo().performClick() }
        runCatching { compose.onAllNodesWithText("Greek Myth")[1].performScrollTo().performClick() }
        runCatching { compose.onNodeWithText("YOUR ARMY").performScrollTo() }
        shoot("02-faction-picker-chosen")
    }

    @Test
    fun `03 opening hand`() {
        show { BoardScreen(playerFaction = Faction.MARVEL, aiFaction = Faction.GREEK_MYTH) {} }
        compose.onNodeWithText("OPENING HAND").assertExists()
        shoot("03-opening-hand")
    }

    @Test
    fun `04 the board`() {
        show { BoardScreen(playerFaction = Faction.PAHLAVAN, aiFaction = Faction.DIV) {} }
        compose.onNodeWithText("BEGIN THE MATCH").performClick()
        compose.waitForIdle()
        shoot("04-board-opening")
    }

    @Test
    fun `05 the board once cards are down`() {
        show { BoardScreen(playerFaction = Faction.ONE_PIECE, aiFaction = Faction.MARVEL) {} }
        compose.onNodeWithText("BEGIN THE MATCH").performClick()
        compose.waitForIdle()

        // Play out a few cards so the rows are populated and the totals mean something.
        repeat(4) {
            tapAHandCard()
            compose.waitForIdle()
        }
        shoot("05-board-in-play")
    }

    @Test
    fun `06 the result`() {
        show { BoardScreen(playerFaction = Faction.DIV, aiFaction = Faction.PAHLAVAN) {} }
        compose.onNodeWithText("BEGIN THE MATCH").performClick()
        compose.waitForIdle()
        repeat(10) {
            if (exists("PASS")) {
                runCatching { compose.onNodeWithText("PASS").performClick() }
                compose.waitForIdle()
            }
        }
        shoot("06-result")
    }
}
