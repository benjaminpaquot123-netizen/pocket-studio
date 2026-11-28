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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import java.io.File

/**
 * Tour 9, D17 (seat t9-75): « Kit UK Garage downloaden… 0 % » vanished without a word, the kit stayed « Studio ».
 * Replays the seat's moves without the bench: card → download → « Étape suivante » while it runs → the pack lands.
 * (a) with the phone's decoder (a sound decodes): the kit stays on the pads;
 * (b) with the real AudioDecoder under Robolectric (MediaCodec has no codec here, as on the bench): the pads stay empty,
 *     the kit falls back to the built-in one and the card goes away with no message — the seat's screen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class T9D17KitDisappearsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)
    private val style = "uk_garage"
    private val target = TutoTarget.of(style)!!
    private val packs = MutableStateFlow(PacksUi())

    private val entries: List<PackEntry> by lazy {
        val s = target.style
        val ids = (s.lines.filter { it.voice == null }.mapNotNull { s.kit[it.role] } + s.id).distinct()
        ids.map { PackEntry(it, "Pack $it", "$it.zip", "a".repeat(64), 3_000_000, 12) }
    }
    private val installed: List<PackManifest> by lazy {
        val kinds = target.style.lines.map { it.role.packKind }.distinct()
        entries.map { e -> PackManifest(e.id, e.name, sounds = kinds.map { k -> PackSound("$k.flac", k, "$k ${e.id}") }) }
    }

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
            check(System.currentTimeMillis() < end) { "$what: not in time (kit \"${runCatching { progress().kit }.getOrNull()}\", pads ${padsWithSamples()}, log ${com.pocketstudio.app.report.AppLog.dump(8)})" }
        }
    }
    private fun has(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    private fun click(tag: String) {
        if (tag.startsWith("tuto_kit")) readToTheEnd()
        val node = compose.onNodeWithTag(tag)
        runCatching { node.performScrollTo() }
        node.performClick()
        compose.waitForIdle()
    }
    private fun readToTheEnd() {
        repeat(12) {
            if (compose.onAllNodesWithTag("tuto_text_more", useUnmergedTree = true).fetchSemanticsNodes().isEmpty()) return
            compose.onNodeWithTag("tuto_text_more").performClick()
            compose.mainClock.advanceTimeBy(600)
            compose.waitForIdle()
        }
    }
    private fun progress(): TutoProgress = PrefsTutoStore(h.app).load(h.vm.projectId)!!
    private fun padsWithSamples(): Set<Int> = h.vm.project.value.drums.lanes.withIndex().filter { it.value.sample != null }.map { it.index }.toSet()
    private val packPads: Set<Int> by lazy { TutoKit.sounds(target, installed).keys }
    private fun ready(installed: List<PackManifest> = emptyList(), downloading: Map<String, Float> = emptyMap()) =
        PacksUi(IndexState.Ready(PackIndex(entries)), installed, downloading)

    /** The seat's course: UK Garage, Pro, its first step done (step=ref done=1), the pack not on the phone. */
    private fun open(phoneDecoder: Boolean) {
        assertTrue(entries.isNotEmpty())
        val first = TutoCourse.of(style)!!.chaptersFor(TutoLevel.Pro).first().steps.first().id
        packs.value = ready()
        TutoTestHooks.packs = packs
        TutoTestHooks.player = null
        h.start(simple = false, build = {
            tutoKitDecodeForTest = if (phoneDecoder) { _ -> Pcm(FloatArray(4800) { 0.4f * kotlin.math.sin(it * 0.07f) * (1f - it / 4800f) }, 1, 48_000) } else null
            session.edit { target.blank() }
            PrefsTutoStore(h.app).save(projectId, TutoProgress(style, TutoLevel.Pro, done = setOf(first)))
        })
        await("panel") { has("tuto_panel") || has("tuto_kit") }
        com.pocketstudio.engine.NativeEngine.sink = Sink()
        await("the kit's card") { has("tuto_kit") }
        compose.mainClock.advanceTimeBy(1_100L)
        compose.waitForIdle()
    }

    /** #3 « Kit downloaden », the bar at 0 %, #4 « Volgende stap » while it runs, then the pack lands (its real files on disk). */
    private fun replay(): Boolean {
        click("tuto_kit_download")
        packs.value = ready(downloading = entries.associate { it.id to 0f })
        compose.waitForIdle()
        assertTrue("the bar while it downloads", has("tuto_kit"))
        val next = has("tuto_next_big")
        if (next) click("tuto_next_big")
        // A real (valid) FLAC where PackStore puts each sound.
        val flac = File("../scripts/bench/oracles-niveaux/pistes-separees/ne-tient-pas/etat/partage/Trois.flac").takeIf { it.exists() }
            ?: File("scripts/bench/oracles-niveaux/pistes-separees/ne-tient-pas/etat/partage/Trois.flac")
        installed.forEach { m -> m.sounds.forEach { s -> com.pocketstudio.app.library.PackStore.file(h.app, m.id, s).apply { parentFile!!.mkdirs(); writeBytes(flac.readBytes()) } } }
        packs.value = ready(installed)
        return next
    }

    @Test
    fun a_withThePhonesDecoderTheKitStays() {
        open(phoneDecoder = true)
        val next = replay()
        await("kit decided") { progress().kit.isNotEmpty() }
        println("D17(a): next pressed=$next kit=${progress().kit} pads=${padsWithSamples()} card=${has("tuto_kit")}")
        assertEquals(TutoProgress.KIT_PACK, progress().kit)
        assertEquals(packPads, padsWithSamples())
    }

    @Test
    fun b_withTheRealDecoderUnderRobolectricTheKitThatCannotLoadIsSaid() {
        open(phoneDecoder = false)
        val next = replay()
        await("kit decided") { progress().kit.isNotEmpty() }
        // Tour 9 L11 (D17): the kit that cannot load is said on its card, which stays (« Télécharger » tries again).
        await("the error said") { has("tuto_kit_error") }
        val log = com.pocketstudio.app.report.AppLog.dump(20)
        println("D17(b): next pressed=$next kit=${progress().kit} pads=${padsWithSamples()} error=${has("tuto_kit_error")} log=$log")
        assertEquals(TutoProgress.KIT_BUILT_IN, progress().kit)
        assertTrue(padsWithSamples().isEmpty())
        assertTrue("the card stays, to try again", has("tuto_kit"))
        assertTrue("the decoder failed", log.contains("son du kit illisible"))
    }
}
