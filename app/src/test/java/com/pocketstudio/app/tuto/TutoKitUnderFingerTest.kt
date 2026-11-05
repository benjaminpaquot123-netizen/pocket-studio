package com.pocketstudio.app.tuto

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.app.library.IndexState
import com.pocketstudio.app.library.PacksUi
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.model.PackEntry
import com.pocketstudio.model.PackIndex
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoTarget
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * Audit ultra round 3, n° 28 (third time: rounds 1 and 2 « fixed » it with a 1 s guard): the kit's card came
 * when the catalogue did and took the tutorial panel's place while the user read it. Seat A c (fr, 200 %, phone,
 * screens 13-14): « Lire la suite » touched → « Le kit Rétro techno / Télécharger (6 Mo) » in its place. Seat E b
 * (nl, tablet, screens 4-5): « Luister naar het voorbeeld » touched → « Kit House downloaden », nothing played.
 * The touch came seconds after the card, so no guard by time could help: the root is that the card replaced the
 * panel. Now the offer sits at the end of the step's words, inside the panel: the panel, its words and its
 * buttons stay where the finger goes, and the touch does what the button says.
 */
@RunWith(RobolectricTestRunner::class)
class TutoKitUnderFingerTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)
    private val packs = MutableStateFlow(PacksUi())
    private var played = 0

    private val ears = object : GuideListenPlayer {
        override fun play(pcm: Pcm, ended: () -> Unit): Boolean { played++; return true }
        override fun stop() = Unit
    }

    @After
    fun end() {
        org.robolectric.RuntimeEnvironment.setFontScale(1f)
        TutoTestHooks.kitOffer = true
        TutoTestHooks.packs = null
        TutoTestHooks.player = null
        h.finish()
    }

    private fun idle(ms: Long = 50) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
    private fun await(what: String, timeoutMs: Long = 30_000, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (!ok()) {
            idle(50)
            compose.mainClock.advanceTimeBy(50)
            compose.waitForIdle()
            check(System.currentTimeMillis() < end) { "$what: not in time" }
        }
    }
    private fun has(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    private fun bounds(tag: String): Rect = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().first().boundsInRoot
    private fun progress(): TutoProgress = PrefsTutoStore(h.app).load(h.vm.projectId)!!

    /** The course of [style] at its first step, Facile, Mode simple, the catalogue not there yet (as on a first start). */
    private fun open(style: String, lang: Lang) {
        val target = TutoTarget.of(style)!!
        TutoTestHooks.packs = packs
        TutoTestHooks.player = ears
        h.start(simple = true, lang = lang, build = {
            session.edit { target.blank() }
            PrefsTutoStore(h.app).save(projectId, TutoProgress(style, TutoLevel.Facile))
        })
        h.vm.guideRenderForTest = { _, _ -> FloatArray(48_000) { 0.3f * kotlin.math.sin(it * 0.05f) } to FloatArray(2) }
        await("panel") { has("tuto_panel") && has("tuto_listen") }
    }

    /** The catalogue comes while the user reads; the user reads on for [readingMs] (the seats took seconds). */
    private fun catalogueComes(style: String, readingMs: Long = 4_000) {
        val s = TutoTarget.of(style)!!.style
        val ids = (s.lines.filter { it.voice == null }.mapNotNull { s.kit[it.role] } + s.id).distinct()
        packs.value = PacksUi(IndexState.Ready(PackIndex(ids.map { PackEntry(it, "Pack $it", "$it.zip", "a".repeat(64), 1_500_000, 12) })))
        await("the offer") { has("tuto_kit") }
        compose.mainClock.advanceTimeBy(readingMs)
        idle(100)
        compose.waitForIdle()
    }

    /** Seat A c, screens 13-14: 200 %, phone, « Rétro techno », « Lire la suite ». */
    @Test
    @Config(qualifiers = "w392dp-h852dp-port-hdpi")
    fun readOnAtTwoHundredPercentTheCatalogueComingDoesNotReplaceThePanel() {
        org.robolectric.RuntimeEnvironment.setFontScale(2f)
        open("retro_techno", Lang.FR)
        // Where the seat's finger went: « Lire la suite » (or, when the words fit, the button under them).
        val more = bounds(if (has("tuto_text_more")) "tuto_text_more" else "tuto_listen")
        val listen = bounds("tuto_listen")
        val panel = bounds("tuto_panel")
        catalogueComes("retro_techno")
        assertTrue("the course's panel replaced by the kit's card", has("tuto_panel") && has("tuto_text"))
        assertEquals("« Écouter l'exemple » moved", listen, bounds("tuto_listen"))
        assertEquals("the panel moved", panel, bounds("tuto_panel"))
        // The finger where « Lire la suite » was on screen 13.
        compose.onRoot().performTouchInput { click(more.center) }
        idle(300)
        compose.waitForIdle()
        assertEquals("a touch meant for « Lire la suite » decided the kit", "", progress().kit)
        compose.onRoot().performTouchInput { click(listen.center) }
        idle(300)
        compose.waitForIdle()
        assertEquals("a touch meant for « Écouter l'exemple » decided the kit", "", progress().kit)
        assertFalse("a download started", has("tuto_kit_progress"))
        assertTrue("the course's panel", has("tuto_panel") && has("tuto_text"))
        // The offer is still there to read, at the end of the words, and still works.
        repeat(12) {
            if (has("tuto_text_more")) { compose.onNodeWithTag("tuto_text_more").performClick(); compose.mainClock.advanceTimeBy(600); compose.waitForIdle() }
        }
        compose.onNodeWithTag("tuto_kit_later").performScrollTo().performClick()
        await("« Plus tard » kept") { progress().kit == TutoProgress.KIT_BUILT_IN }
        await("the offer gone") { !has("tuto_kit") }
        assertTrue(has("tuto_panel"))
    }

    /** Seat E b, screens 4-5: Dutch, tablet, « House », « Luister naar het voorbeeld ». */
    @Test
    @Config(qualifiers = "w600dp-h960dp")
    fun onATabletListenTouchedAfterTheCatalogueCameStillPlaysTheExample() {
        open("house", Lang.NL)
        val listen = bounds("tuto_listen")
        catalogueComes("house")
        assertTrue("the course's panel replaced by the kit's card", has("tuto_panel"))
        compose.onRoot().performTouchInput { click(listen.center) }
        await("the example plays") { played > 0 }
        assertEquals("", progress().kit)
        assertTrue("the offer still there to decide", has("tuto_kit_download"))
    }
}
