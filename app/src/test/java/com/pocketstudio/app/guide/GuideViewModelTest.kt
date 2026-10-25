package com.pocketstudio.app.guide

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Project
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.guide.GuideStatus
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.guide.guideTracks
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration

/** The Guide's plumbing (docs/PLAN-GUIDE.md G1): the analysis follows the project, each button is one undo step through the session. */
@RunWith(RobolectricTestRunner::class)
class GuideViewModelTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    private fun jumpUp(): Project {
        val style = StarterWizard.style("jump_up")!!
        return StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? }, style.melody.mood,
            StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() })
    }

    private fun vmWith(p: Project): RhythmViewModel {
        File(app.filesDir, "projects").deleteRecursively()
        return RhythmViewModel(app).also { vm -> vm.session.edit { p } }
    }

    /** Runs the main looper (its clock too: the analysis waits for the edits to settle) until [ok]. */
    private fun await(what: String, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + 60_000
        while (!ok()) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
            check(System.currentTimeMillis() < end) { "$what: not in time" }
            Thread.sleep(5)
        }
    }

    private fun Project.subStrip() = mixer.channels[InstrumentTrack.mixerChannel(instrumentTracks.first { it.name == "Sub" }.id)]

    /** The Sub's strip with the Guide's saturation (after « Basse audible », the assistant's own effect there). */
    private fun com.pocketstudio.model.Channel.saturated() = inserts.any { it.effect == EffectType.SATURATION }

    @Test
    fun theAnalysisFollowsTheProjectAndTheCardsAreOneUndoStepEach() {
        val vm = vmWith(jumpUp())
        val guide = vm.guide
        await("first analysis") { !guide.ui.value.loading && guide.ui.value.next != null }
        val ui = guide.ui.value
        assertEquals("fx.review", ui.next!!.id)
        assertEquals(3, ui.summary.size)
        assertTrue(ui.header, ui.header.contains("Jump up DnB"))
        assertTrue(ui.percent in 30..55)
        assertTrue(ui.plan.isNotEmpty() && ui.plan.first().settled)
        assertTrue(ui.sheets.any { it.track.name == "Basse" })
        // The chains as the Guide first saw them, kept quietly (no undo step for it).
        assertTrue(vm.project.value.guide.seen.isNotEmpty())

        // « Écouter piste par piste »: the card is done, the next one comes; only the Guide's state changed: no engine update.
        val syncs = vm.guideSyncs
        guide.apply("fx.review", "keep")
        await("after apply") { guide.ui.value.next?.id == "fx.drums" }
        assertEquals(GuideStatus.DONE, vm.project.value.guide.statusOf("fx.review"))
        assertEquals(syncs, vm.guideSyncs)

        // An option that changes the song: the sub gets its saturation (the engine follows); one undo takes it back with the card.
        val before = vm.project.value.subStrip()
        guide.apply("fx.sub", "light")
        await("sub saturated") { vm.project.value.subStrip().saturated() }
        assertEquals(syncs + 1, vm.guideSyncs)
        await("sub undoable") { guide.ui.value.cards["fx.sub"]?.undoable == true }
        vm.undo()
        assertEquals(before, vm.project.value.subStrip())
        assertEquals(GuideStatus.TODO, vm.project.value.guide.statusOf("fx.sub"))
        vm.redo()
        assertTrue(vm.project.value.subStrip().saturated())
        // « Annuler mon choix » is a step of its own.
        guide.undoChoice("fx.sub")
        await("undone") { vm.project.value.subStrip() == before }
        vm.undo()
        assertTrue(vm.project.value.subStrip().saturated())

        // « Plus tard » and « Sauter »: one step each.
        guide.later("fx.drums")
        await("later") { guide.ui.value.next?.id == "fx.bass" }
        guide.skip("fx.bass")
        await("skipped") { vm.project.value.guide.statusOf("fx.bass") == GuideStatus.SKIPPED }
        vm.undo()
        assertEquals(GuideStatus.TODO, vm.project.value.guide.statusOf("fx.bass"))
        assertEquals(GuideStatus.LATER, vm.project.value.guide.statusOf("fx.drums"))
    }

    /** Review G2: a choice that cannot come back without erasing a later edit is refused, and said. */
    @Test
    fun aRefusedUndoIsSaid() {
        val vm = vmWith(jumpUp())
        val guide = vm.guide
        await("first analysis") { !guide.ui.value.loading }
        val notices = ArrayList<String>()
        val job = kotlinx.coroutines.MainScope().launch(kotlinx.coroutines.Dispatchers.Main.immediate) { guide.notices.collect { notices += it } }
        guide.apply("fx.sub", "light")
        await("applied") { vm.project.value.guide.history.isNotEmpty() }
        // The same effect changed by hand since.
        val ch = InstrumentTrack.mixerChannel(vm.project.value.instrumentTracks.first { it.name == "Sub" }.id)
        vm.session.edit { p -> p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c ->
            if (i == ch) c.copy(inserts = c.inserts.toMutableList().also { l ->
                val k = l.indexOfFirst { it.effect == EffectType.SATURATION }
                l[k] = l[k].copy(params = listOf(0.9f) + l[k].params.drop(1))
            }) else c
        })) }
        val edited = vm.project.value.subStrip()
        guide.undoChoice("fx.sub")
        await("said") { notices.isNotEmpty() }
        assertEquals(edited, vm.project.value.subStrip())
        assertEquals(GuideStatus.DONE, vm.project.value.guide.statusOf("fx.sub"))
        job.cancel()
    }

    @Test
    fun anOptionThatOpensAScreenAsksForItAndChangesNothing() {
        val vm = vmWith(jumpUp())
        val guide = vm.guide
        await("first analysis") { !guide.ui.value.loading }
        val opened = ArrayList<GuideOpen>()
        val job = kotlinx.coroutines.MainScope().launchCollect(guide, opened)
        val before = vm.project.value
        guide.apply("fx.review", "all")
        await("opened") { opened.isNotEmpty() }
        assertEquals("mixer.whatplays", opened.single().spot)
        assertEquals(before, vm.project.value)
        job.cancel()
    }

    @Test
    fun theModeSimpleAndTheEditorAreFollowed() {
        val vm = vmWith(jumpUp())
        val guide = vm.guide
        await("first analysis") { !guide.ui.value.loading }
        guide.setSimple(true)
        await("simple") { guide.ui.value.simple }
        assertTrue(guide.ui.value.plan.all { it.stage in setOf(1, 4, 6, 7) })
        // A pan moved in the mixer: the analysis sees it, the pans card is ticked without the Guide.
        val lead = InstrumentTrack.mixerChannel(vm.project.value.instrumentTracks.first { it.name == "Lead" }.id)
        vm.session.edit { p -> p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c -> if (i == lead) c.copy(pan = 0.3f) else c })) }
        await("pans ticked") { guide.ui.value.cards["mix.pans"]?.status == GuideStatus.DONE_ELSEWHERE }
        assertFalse(guide.ui.value.facts.any { it.id == "mix.no_pans" })
        // A burst of edits is analysed once it settles, not at each one.
        val count = guide.analyses
        repeat(20) { k -> vm.session.edit("burst") { p -> p.copy(tempoBpm = 170.0 + k * 0.1) } }
        await("burst") { guide.analyses > count }
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1000))
        assertTrue("${guide.analyses - count} analyses", guide.analyses - count <= 2)
    }

    /**
     * G4: « Voilà ce que j'ai mis » lists every effect with its why, one is retired without deciding
     * the card; « Placer chaque piste pour moi » measures the song first (the app's renders), then
     * moves the faders; a measure taken on another sound is left aside.
     */
    @Test
    fun theEffectsAreListedAndTheBalanceIsMeasuredFirst() {
        val vm = vmWith(jumpUp())
        await("first analysis") { !vm.guide.ui.value.loading }
        val measured = ArrayList<Project>()
        val scope = kotlinx.coroutines.MainScope()
        val guide = GuideViewModel(vm.session, scope, measurer = { p ->
            measured += p
            val kick = p.guideTracks().first { it.role == com.pocketstudio.model.guide.GuideRoles.KICK }.key
            val pad = "inst:" + p.instrumentTracks.first { it.name == "Nappe" }.id
            com.pocketstudio.model.guide.GuideMeasures(at = 1L, tracks = mapOf(kick to -10f, pad to -12f), monoLossDb = 1f, phoneDb = -9f,
                signature = com.pocketstudio.model.guide.GuideSignatures.sound(p))
        })
        await("analysis") { !guide.ui.value.loading && guide.ui.value.cards.isNotEmpty() }
        val review = guide.ui.value.cards.getValue("fx.review")
        assertTrue(review.effects.size >= 10)
        val bass = review.effects.first { it.trackName == "Basse" }
        assertTrue(bass.why, bass.why.isNotBlank())
        assertFalse(bass.off)
        // The bass's own card lists the bass's effects only.
        assertTrue(guide.ui.value.cards.getValue("fx.bass").effects.all { it.track == bass.track })
        guide.apply("fx.review", bass.option)
        await("retired") { guide.ui.value.cards.getValue("fx.review").effects.first { it.option.endsWith(bass.option.substringAfter("off")) }.off }
        assertEquals(GuideStatus.TODO, vm.project.value.guide.statusOf("fx.review"))
        // The balance: measured, then the pad brought back 6 dB.
        val pad = InstrumentTrack.mixerChannel(vm.project.value.instrumentTracks.first { it.name == "Nappe" }.id)
        val fader = vm.project.value.mixer.channels[pad].volume
        assertFalse(guide.ui.value.measured)
        guide.apply("mix.balance", "balance")
        await("balanced") { vm.project.value.mixer.channels[pad].volume < fader }
        assertEquals(1, measured.size)
        assertEquals(1, guide.measureCount)
        await("measured") { guide.ui.value.measured && guide.ui.value.cards["mix.balance"]?.status == GuideStatus.DONE }
        // The checks: what the Guide hears, said on their cards.
        assertTrue(guide.ui.value.cards.getValue("mix.mono").heard.single().contains("1"))
        assertEquals(GuideStatus.DONE_ELSEWHERE, guide.ui.value.cards.getValue("mix.phone").status)
        // Measured again only once the sound changed.
        guide.measure()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500))
        assertEquals(1, measured.size)
        vm.session.edit { p -> p.copy(tempoBpm = p.tempoBpm + 1) }
        assertFalse(com.pocketstudio.model.guide.GuideFacts.of(vm.project.value).holds("measured", "song"))
        guide.measure()
        await("measured again") { measured.size == 2 }
        scope.cancel()
    }
}

/** Collects [guide]'s screens to open into [into]. */
private fun kotlinx.coroutines.CoroutineScope.launchCollect(guide: GuideViewModel, into: MutableList<GuideOpen>) =
    launch(kotlinx.coroutines.Dispatchers.Main.immediate) { guide.opens.collect { into += it } }
