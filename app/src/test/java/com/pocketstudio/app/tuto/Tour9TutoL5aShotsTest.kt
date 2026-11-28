package com.pocketstudio.app.tuto

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.tuto.TutoCourse
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

/**
 * Tour 9 lot L5a, F063 on the seats' phone (360 dp, 100 % and 130 %, Simple and Pro): the acid_techno 303 step in
 * Arabic names its notes as the Arabic 303 keys do (« لا# », solfège), no Latin letter and no flat. Simple goes through
 * the Facile « acid_show » step (note names are Pro-only), Pro through « acid_do ».
 */
abstract class Tour9TutoL5aShots(private val name: String) {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)

    init { TutoTestHooks.kitOffer = false }

    @After
    fun end() {
        TutoTestHooks.kitOffer = true
        h.finish()
    }

    private fun has(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun await(what: String, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + 30_000
        while (!ok()) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            compose.mainClock.advanceTimeBy(50)
            compose.waitForIdle()
            check(System.currentTimeMillis() < end) { "$what: not in time" }
        }
    }

    private fun shot(level: TutoLevel, simple: Boolean, step: String, spelt: String?) {
        h.start(simple = simple, lang = Lang.AR, build = {
            session.edit { TutoTarget.of("acid_techno")!!.blank() }
            PrefsTutoStore(h.app).save(projectId, TutoProgress("acid_techno", level, current = step))
        })
        await("the panel") { has("tuto_text") }
        repeat(20) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100)); compose.mainClock.advanceTimeBy(100); compose.waitForIdle() }
        val text = compose.onNodeWithTag("tuto_text", useUnmergedTree = true).fetchSemanticsNode()
            .config[SemanticsProperties.Text].joinToString()
        compose.onRoot().captureRoboImage("screenshots/tour9_l5a_acid_ar_${if (simple) "simple" else "pro"}_$name.png")
        assertTrue("$step not shown: $text", text.isNotBlank() && TutoCourse.of("acid_techno")!!.chaptersFor(level)
            .flatMap { it.steps }.any { it.id == step })
        if (spelt != null) assertTrue("« $spelt » missing: $text", spelt in text)
        assertTrue(text, "♭" !in text && Regex("(?<![A-Za-z])[A-G](?![A-Za-z0-9])").find(text) == null)
    }

    @Test
    fun simpleAcidStepNamesNotesAsTheKeys() = shot(TutoLevel.Facile, simple = true, step = "acid_show", spelt = null)

    @Test
    fun proAcidStepNamesNotesAsTheKeys() = shot(TutoLevel.Pro, simple = false, step = "acid_do", spelt = "لا#")
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-port-hdpi", fontScale = 1.0f)
class Tour9TutoL5aShotsTest : Tour9TutoL5aShots("100")

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-port-hdpi", fontScale = 1.3f)
class Tour9TutoL5aShotsLargeTextTest : Tour9TutoL5aShots("130")
