package com.pocketstudio.app.tuto

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.library.IndexState
import com.pocketstudio.app.library.PacksUi
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.model.PackEntry
import com.pocketstudio.model.PackIndex
import com.pocketstudio.model.PackManifest
import com.pocketstudio.model.PackSound
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoKit
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
 * Socle 2 (every tutorial on its style's box of sounds), as the user meets it: « Le kit … » over
 * the course while its pack is not on the phone (« Plus tard » keeps the built-in kit), nothing
 * offline, the pack's sounds on the pads at once on a course not begun (one undo step), and
 * (review 2026-10-29, MOYEN 1) never in silence over a course under way: « Mettre les sons » asks.
 * The catalogue stands in [TutoTestHooks.packs]; the pack's sounds are decoded as a short tone.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class TutoKitTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)
    private val style = "jump_up"
    private val target = TutoTarget.of(style)!!
    private val packs = MutableStateFlow(PacksUi())

    /** The style's packs in the catalogue (each a drum pack: no preview). */
    private val entries: List<PackEntry> by lazy {
        val s = target.style
        val ids = (s.lines.filter { it.voice == null }.mapNotNull { s.kit[it.role] } + s.id).distinct()
        ids.map { PackEntry(it, "Pack $it", "$it.zip", "a".repeat(64), 1_500_000, 12) }
    }

    /** Those packs on the phone, a sound of every kind the style's drum lines take. */
    private val installed: List<PackManifest> by lazy {
        val kinds = target.style.lines.map { it.role.packKind }.distinct()
        entries.map { e -> PackManifest(e.id, e.name, sounds = kinds.map { k -> PackSound("$k.flac", k, "$k ${e.id}") }) }
    }

    /** The engine's place (Robolectric has none): a pad's sound is prepared, nothing plays. */
    private class Sink : com.pocketstudio.engine.CommandSink {
        private var next = 1L
        override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) = Unit
        override fun scheduleFrom(target: Long, frame: Long) = Unit
        override fun install(target: Long, kind: com.pocketstudio.engine.CommandSink.Install, index: Int, sample: Long) = Unit
        @Synchronized override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = next++
        override fun sampleFrames(handle: Long): Long = 4_800L
        override fun sampleRelease(handle: Long) = Unit
        @Synchronized override fun exportCreate(): Long = next++
        override fun exportRender(target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean, sampleRate: Int, bits: Int) =
            com.pocketstudio.engine.ExportResult.Ok
        override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray = FloatArray(9_600)
        override fun exportDestroy(target: Long) = Unit
    }

    @After
    fun end() {
        com.pocketstudio.engine.NativeEngine.sink = null
        TutoTestHooks.packs = null
        TutoTestHooks.kitOffer = true
        h.finish()
    }

    private fun idle(ms: Long = 50) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    private fun await(what: String, timeoutMs: Long = 20_000, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (!ok()) {
            idle(50)
            compose.waitForIdle()
            check(System.currentTimeMillis() < end) { "$what: not in time (kit \"${runCatching { progress().kit }.getOrNull()}\", pads ${padsWithSamples()}, log ${com.pocketstudio.app.report.AppLog.dump(6)})" }
        }
    }

    private fun has(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    private fun click(tag: String) {
        val node = compose.onNodeWithTag(tag)
        runCatching { node.performScrollTo() }
        node.performClick()
        compose.waitForIdle()
    }
    private fun progress(): TutoProgress = PrefsTutoStore(h.app).load(h.vm.projectId)!!

    /** The pads the pack's sounds go on. */
    private val packPads: Set<Int> by lazy { TutoKit.sounds(target, installed).keys }
    private fun padsWithSamples(): Set<Int> = h.vm.project.value.drums.lanes.withIndex().filter { it.value.sample != null }.map { it.index }.toSet()

    /** The tutorial's project of [style], [done] its steps already done (a course begun with an earlier version). */
    private fun open(ui: PacksUi, done: Set<String> = emptySet()) {
        assertTrue("the style has no pack to offer", entries.isNotEmpty())
        packs.value = ui
        TutoTestHooks.packs = packs
        TutoTestHooks.player = null
        h.start(simple = false, build = {
            // Before the app shows the project: the kit may be put at once.
            tutoKitDecodeForTest = { Pcm(FloatArray(4800) { 0.4f * kotlin.math.sin(it * 0.07f) * (1f - it / 4800f) }, 1, 48_000) }
            session.edit { target.blank() }
            PrefsTutoStore(h.app).save(projectId, TutoProgress(style, TutoLevel.Pro, done = done))
        })
        await("panel") { has("tuto_panel") }
        // Once the app is up (it opens its audio first): the pads' sounds are prepared through it.
        com.pocketstudio.engine.NativeEngine.sink = Sink()
    }

    private fun ready(installed: List<PackManifest> = emptyList()) = PacksUi(IndexState.Ready(PackIndex(entries)), installed)

    @Test
    fun theOfferShowsWhileThePackIsNotOnThePhoneAndPlusTardKeepsTheBuiltInKit() {
        open(ready())
        await("the kit's card") { has("tuto_kit") }
        assertTrue(has("tuto_kit_download"))
        assertEquals("", progress().kit)
        click("tuto_kit_later")
        await("« Plus tard » kept") { progress().kit == TutoProgress.KIT_BUILT_IN }
        await("the card gone") { !has("tuto_kit") }
        assertTrue("a sound put on a pad", padsWithSamples().isEmpty())
        // Not offered again, even once the pack comes.
        packs.value = ready(installed)
        idle(500)
        compose.waitForIdle()
        assertFalse(has("tuto_kit"))
        assertTrue(padsWithSamples().isEmpty())
    }

    @Test
    fun offlineNoOfferAndNothingDecided() {
        open(PacksUi(IndexState.Unavailable))
        idle(1_000)
        compose.waitForIdle()
        assertFalse(has("tuto_kit"))
        assertEquals("", progress().kit)
        assertTrue(padsWithSamples().isEmpty())
        // The catalogue back later: the offer then.
        packs.value = ready()
        await("the kit's card") { has("tuto_kit") }
    }

    @Test
    fun onACourseNotBegunThePacksSoundsGoOnThePadsInOneUndoStep() {
        // The catalogue comes a moment after the course opens (as on the phone).
        open(PacksUi())
        packs.value = ready(installed)
        await("the pack on the pads") { progress().kit == TutoProgress.KIT_PACK }
        assertTrue(packPads.isNotEmpty())
        assertEquals(packPads, padsWithSamples())
        assertFalse(has("tuto_kit"))
        // One « Annuler » takes the whole kit back.
        h.vm.undo()
        await("undone") { padsWithSamples().isEmpty() }
    }

    @Test
    fun onACourseUnderWayThePackIsOfferedNotPutInSilence() {
        val first = TutoCourse.of(style)!!.chaptersFor(TutoLevel.Pro).first().steps.first().id
        open(ready(installed), done = setOf(first))
        await("the kit's card") { has("tuto_kit") }
        idle(500)
        // Nothing replaced by itself.
        assertTrue("the pads' sounds replaced in silence", padsWithSamples().isEmpty())
        assertEquals("", progress().kit)
        // « Mettre les sons du kit »: now they come.
        click("tuto_kit_download")
        await("the pack on the pads") { progress().kit == TutoProgress.KIT_PACK }
        assertEquals(packPads, padsWithSamples())
        await("the card gone") { !has("tuto_kit") }
    }

    @Test
    fun aKitDownloadedFromTheCardGoesOnThePadsOnACourseUnderWay() {
        val first = TutoCourse.of(style)!!.chaptersFor(TutoLevel.Pro).first().steps.first().id
        open(ready(), done = setOf(first))
        await("the kit's card") { has("tuto_kit") }
        click("tuto_kit_download")
        // The download ends: the user asked for it, its sounds go on the pads.
        packs.value = ready(installed)
        await("the pack on the pads") { progress().kit == TutoProgress.KIT_PACK }
        assertEquals(packPads, padsWithSamples())
    }
}
