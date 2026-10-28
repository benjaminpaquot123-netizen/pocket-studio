package com.pocketstudio.app.tuto

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoTarget
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

/** The tutorial in the app (app/screenshots/tuto_app_*.png): the snare's step on the grid, the bass's dotted notes. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class TutoAppScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)

    @After
    fun end() = h.finish()

    private fun has(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun await(what: String, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + 20_000
        while (!ok()) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            compose.waitForIdle()
            check(System.currentTimeMillis() < end) { "$what: not in time" }
        }
    }

    /** Saved (« Sauvegardé » shown each time, the image does not depend on when the save ended). */
    private fun settle() {
        kotlinx.coroutines.runBlocking { h.vm.flushNow() }
        await("saved") { has("save_status") || true }
        repeat(10) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100)); compose.waitForIdle() }
    }

    private fun open(step: String) {
        h.start(simple = true, build = {
            val target = TutoTarget.of("jump_up")!!
            // The kick done, the tempo and the key set: the snare is next.
            session.edit {
                val b = target.blank()
                b.copy(tempoBpm = 174.0, harmony = target.harmony, drums = b.drums.copy(lanes = b.drums.lanes.mapIndexed { i, l -> if (i == 0) l.copy(hits = mapOf(0 to com.pocketstudio.model.Hit(1f), 10 to com.pocketstudio.model.Hit(1f))) else l }))
            }
            PrefsTutoStore(h.app).save(projectId, TutoProgress("jump_up", TutoLevel.Facile, setOf("ref", "tempo", "key", "kick_hear", "kick_do"), step))
        })
        await("panel") { has("tuto_panel") }
    }

    /**
     * The snare: « Passer à 2 mesures », then its dotted cells on the grid, all the step long; the
     * panel folds out of the way (lot « tuto 3 »), the bar pages right above the cells.
     */
    @Test
    fun snareOnTheGrid() {
        open("snare_do")
        compose.onNodeWithTag("tuto_need").performClick()
        await("2 bars") { h.vm.project.value.drums.steps == 32 }
        await("dotted cells") { has("tuto_hint") }
        assertTrue("folded", has("tuto_folded_text"))
        compose.mainClock.advanceTimeBy(3_000)
        await("the ring gone") { !has("tuto_spot_ring") }
        settle()
        compose.onRoot().captureRoboImage("screenshots/tuto_app_snare.png")
    }

    /** The bass: « Créer la piste Basse » opens its notes, the line dotted where the user taps. */
    @Test
    fun bassNotesDotted() {
        open("bass_do")
        await("need") { has("tuto_need") }
        compose.onNodeWithTag("tuto_need").performClick()
        await("piano roll") { has("piano_roll") && TutoHints.notes != null }
        compose.waitForIdle()
        settle()
        compose.onRoot().captureRoboImage("screenshots/tuto_app_bass.png")
    }
}
