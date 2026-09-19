package ir.gwent.android.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import ir.gwent.core.engine.GameEngine
import ir.gwent.core.model.*
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.random.Random

/**
 * Renders the real screens to PNG so the UI can be reviewed without installing the APK.
 * Robolectric's native graphics backend rasterises the same Compose drawing code a device runs,
 * so what lands in build/screenshots is the shipped UI rather than a mock-up.
 *
 * The board is rendered in landscape, which is how GWENT is laid out.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w891dp-h411dp-xxhdpi")
class ScreenshotTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val outputDir = File("build/screenshots").apply { mkdirs() }

    private fun show(content: @Composable () -> Unit) {
        compose.setContent { GwentTheme { content() } }
        compose.waitForIdle()
    }

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

    private fun engine(): GameEngine = GameEngine.start(
        CardDatabase.starterDeck(Leaders.FOLTEST),
        CardDatabase.starterDeck(Leaders.EREDIN),
        Random(4),
    )

    private fun place(e: GameEngine, p: PlayerState, row: Row, id: String, apply: (UnitInstance) -> Unit = {}) {
        val card = CardDatabase.byId(id) ?: return
        val unit = UnitInstance(card, e.state.allocateUid())
        apply(unit)
        p.rows.getValue(row) += unit
    }

    @Test
    fun `01 leader picker`() {
        show { FactionPickerScreen { _, _ -> } }
        shoot("01-leader-picker")
    }

    @Test
    fun `01b deck builder`() {
        show { DeckBuilderScreen(Leaders.FOLTEST, onPlay = {}, onBack = {}) }
        shoot("01b-deck-builder")
    }

    @Test
    fun `02 mulligan`() {
        val e = engine()
        show { MulliganScreen(e) {} }
        shoot("02-mulligan")
    }

    @Test
    fun `03 board opening`() {
        val e = engine()
        e.state.turn = Side.A
        show { BoardScreen(e) }
        shoot("03-board-opening")
    }

    /**
     * A mid-game position chosen to exercise the readouts that matter: a boosted unit (green),
     * a damaged one (red), armour, and a spread of statuses.
     */
    @Test
    fun `04 board in play`() {
        val e = engine()
        val me = e.state.playerA
        val them = e.state.playerB

        place(e, me, Row.MELEE, "nor_knight") { it.power = 9 }                  // boosted -> green
        place(e, me, Row.MELEE, "nor_infantry")
        place(e, me, Row.MELEE, "neu_shieldbearer") { it.apply(Status.SHIELD) }
        place(e, me, Row.RANGED, "nor_ballista")
        place(e, me, Row.RANGED, "neu_archer") { it.apply(Status.VITALITY, 3) }
        place(e, me, Row.RANGED, "nor_vernon") { it.apply(Status.RESILIENCE) }

        place(e, them, Row.MELEE, "mon_werewolf") { it.power = 3 }              // damaged -> red
        place(e, them, Row.MELEE, "mon_ghoul") { it.apply(Status.BLEEDING, 2) }
        place(e, them, Row.MELEE, "mon_arachas")
        place(e, them, Row.RANGED, "mon_vampire") { it.apply(Status.POISON) }
        place(e, them, Row.RANGED, "mon_harpy") { it.apply(Status.LOCKED) }

        e.state.rowEffects += RowEffect(Side.B, Row.RANGED, RowEffectKind.FROST)
        e.state.turn = Side.A
        e.state.round = 2
        me.crowns = 1

        show { BoardScreen(e) }
        shoot("04-board-in-play")
    }

    @Test
    fun `04b card detail`() {
        val card = CardDatabase.byId("nor_ballista")!!
        val unit = UnitInstance(card, 1)
        unit.apply(Status.SHIELD)
        unit.apply(Status.VITALITY, 3)
        unit.boost(2)
        show {
            CardDetailPanel(card = card, unit = unit, canUseOrder = true, onUseOrder = {}) {}
        }
        shoot("04b-card-detail")
    }

    @Test
    fun `05 full row`() {
        val e = engine()
        val me = e.state.playerA
        // Nine units is the row cap; render it so the layout is checked at its limit.
        repeat(ROW_CAPACITY) { place(e, me, Row.MELEE, "nor_infantry") }
        place(e, e.state.playerB, Row.MELEE, "mon_ghoul")
        e.state.turn = Side.A
        show { BoardScreen(e) }
        shoot("05-full-row")
    }

    @Test
    fun `06 match over`() {
        val e = engine()
        e.state.matchOver = true
        e.state.matchWinner = Side.A
        e.state.playerA.crowns = 2
        e.state.playerB.crowns = 1
        place(e, e.state.playerA, Row.MELEE, "nor_vernon")
        show { BoardScreen(e) }
        shoot("06-match-over")
    }
}
