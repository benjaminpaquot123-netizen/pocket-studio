package com.pocketstudio.app.tuto

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.Project
import com.pocketstudio.model.SynthPatch
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.track
import com.pocketstudio.model.tuto.TutoChecks
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoStep
import com.pocketstudio.model.tuto.TutoTarget
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import kotlin.math.abs

/**
 * Owner (2026-10-28): the whole jump up course, Facile and Pro, done in the app the way a user
 * does it, step after step: the step's main button, « Je le fais » (the right screen, its dotted
 * cells or notes in sight), the cells and the notes touched, the sheets' choices. At each step:
 * nothing appears by itself, « Montre-moi » shows then leaves the project exactly as it was (and
 * nothing in « Annuler »), « Écouter l'exemple » plays something to hear, the step is ticked only
 * once done. (That the examples are really audible through the engine: TutoExampleSoundBench.)
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class TutoJourneyTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)
    private val target = TutoTarget.of("jump_up")!!

    private class Player : GuideListenPlayer {
        val played = ArrayList<Pcm>()
        override fun play(pcm: Pcm, ended: () -> Unit): Boolean { played += pcm; return true }
        override fun stop() = Unit
        override fun heard(): Long = played.lastOrNull()?.frames?.toLong() ?: 0L
    }

    private val player = Player()

    @After
    fun end() {
        TutoTestHooks.player = null
        h.finish()
    }

    private fun idle(ms: Long = 50) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    private fun await(what: String, timeoutMs: Long = 30_000, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (!ok()) {
            idle(50)
            compose.waitForIdle()
            check(System.currentTimeMillis() < end) { "$what: not in time" }
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
    private fun p(): Project = h.vm.project.value

    /** What the user sees of the project (content only; the shown part may change with a step's button). */
    private fun content(q: Project) = q.copy(guide = Project().guide).sanitized()

    private fun closeSheet() {
        androidx.test.espresso.Espresso.pressBack()
        compose.waitForIdle()
    }

    @Test
    fun theWholeCourseInFacile() = journey(TutoLevel.Facile)

    @Test
    fun theWholeCourseInPro() = journey(TutoLevel.Pro)

    private fun journey(level: TutoLevel) {
        TutoTestHooks.player = player
        h.start(simple = level == TutoLevel.Facile, build = {
            // « J'apprends » › level › Jump up: the tutorial's own empty project.
            session.edit { target.blank() }
            PrefsTutoStore(h.app).save(projectId, TutoProgress("jump_up", level))
        })
        // A short tone stands for the engine's render (Robolectric has none).
        h.vm.guideRenderForTest = { a, _ -> FloatArray(OfflineExport.SAMPLE_RATE * 2) { 0.3f * kotlin.math.sin(it * 0.05f) } to FloatArray(2) }
        await("panel") { has("tuto_panel") }
        // Truly empty: no hit, no note, the first track hidden (no « Synthé » in « Mes pistes »).
        assertTrue(p().drums.lanes.all { it.hits.isEmpty() })
        assertTrue(TutoChecks.blankFirst(p()))
        val steps = TutoCourse.of("jump_up")!!.chaptersFor(level).flatMap { it.steps }
        var demos = 0
        val noDemo = ArrayList<String>()
        var examples = 0
        val early = ArrayList<String>()
        for (s in steps) {
            await("${s.id} shown") { progress().current == s.id }
            if (s.id in progress().done) {
                early += s.id
                if (has("tuto_next_big")) click("tuto_next_big")
                continue
            }
            val start = p()
            val depth = h.vm.session.undoDepth
            // « Montre-moi »: shown, then taken back; never ticks, never in « Annuler ».
            if (!has("tuto_show") && !s.manual && !s.listening && s.check != "changed:kit" && !s.check.startsWith("changed:synth")) noDemo += s.id
            if (has("tuto_show")) {
                click("tuto_show")
                await("${s.id}: demo shown") { has("tuto_demo_caption") }
                await("${s.id}: demo over", 60_000) { !has("tuto_demo_caption") }
                idle(400)
                assertEquals("${s.id}: Montre-moi left something", content(start), content(p()))
                assertEquals("${s.id}: Montre-moi in Annuler", depth, h.vm.session.undoDepth)
                assertFalse("${s.id}: ticked by Montre-moi", s.id in progress().done)
                demos++
            }
            // « Écouter l'exemple »: something to hear, the project untouched.
            if (s.listen.isNotEmpty()) {
                val before = player.played.size
                click("tuto_listen")
                await("${s.id}: example played") { player.played.size > before }
                assertTrue("${s.id}: silent example", player.played.last().data.any { abs(it) > 0.01f })
                examples++
                assertEquals("${s.id}: the example touched the project", content(start), content(p()))
                if (s.listening) {
                    await("${s.id}: ticked once heard") { s.id in progress().done }
                    await("« Étape suivante »") { has("tuto_next_big") }
                    click("tuto_next_big")
                    continue
                }
                click("tuto_listen")  // stops it
            }
            assertFalse("${s.id}: done before it was done", s.id in progress().done)
            assertEquals("${s.id}: something appeared by itself", content(start), content(p()))
            perform(s, level)
            await("${s.id}: ticked once done") { s.id in progress().done }
            // « C'est fait » goes on (the user's own touch); a step seen done stays (owner, 2026-10-28).
            if (s.manual) continue
            idle(400)
            assertEquals("${s.id}: went on by itself", s.id, progress().current)
            if (steps.all { it.id in progress().done }) break
            await("${s.id}: « Bravo »") { has("tuto_bravo") }
            if (s.check.startsWith("lane:")) assertTrue("${s.id}: « Écouter ton résultat »", has("tuto_play_mine"))
            click("tuto_next_big")
        }
        assertTrue(steps.all { it.id in progress().done })
        // Only a step a former one already did may be ticked before it is reached.
        assertTrue("ticked before reached: $early", early.size <= 2)
        // Every step with something to show showed it, every example was played.
        assertTrue("no Montre-moi: $noDemo", noDemo.isEmpty())
        assertTrue("$demos demos", demos >= 18)
        assertEquals("early: $early", steps.count { it.listen.isNotEmpty() && it.id !in early }, examples)
        // The song made by hand: seven parts in order.
        assertEquals(listOf("Intro", "Montée", "Drop", "Break", "Montée", "Drop 2", "Fin"), p().song.map { p().allScenes[it.scene].label })
    }

    /** [s] done as a user does it. */
    private fun perform(s: TutoStep, level: TutoLevel) {
        val c = s.check
        if (s.manual) {
            click("tuto_mark")
            return
        }
        when {
            c == "tempo" -> {
                click("tuto_do")
                await("tempo sheet") { has("tempo_sheet") }
                h.vm.setTempo(174.0)
                closeSheet()
            }
            c == "key" -> {
                click("tuto_do")
                await("key sheet") { has("tuto_key_sheet") }
                click("tuto_scale_Phrygian")
                click("tuto_root_5")
                await("key set") { p().harmony.root == 5 }
                closeSheet()
            }
            c.startsWith("lane:") -> {
                if (has("tuto_need")) click("tuto_need")
                await("need met") { !has("tuto_need") }
                click("tuto_do")
                await("${s.id}: the grid on its pad, cells dotted") { has("rhythm") && has("tuto_hint") }
                // « Je le fais »: a ring that goes by itself, no card to close, the panel still open.
                assertFalse(has("guide_spot"))
                assertTrue(has("tuto_text"))
                if (s.id == "kick_do") {
                    await("the ring") { has("tuto_spot_ring") }
                    compose.mainClock.advanceTimeBy(3_000)
                    await("the ring goes by itself") { !has("tuto_spot_ring") }
                    assertTrue("the dotted cells stay", has("tuto_hint"))
                }
                val (lane, cells) = TutoHints.cells!!
                assertEquals(lane, h.vm.transport.value.selectedPad)
                for (cell in cells.sorted()) {
                    if (has("step_$cell")) click("step_$cell") else h.vm.toggleStep(cell)
                    compose.waitForIdle()
                }
            }
            c == "velocity:hat" -> {
                click("tuto_do")
                val lane = TutoChecks.laneOf("hat")
                h.vm.selectPad(lane)
                // Long presses: two hits softer.
                p().drums.lanes[lane].hits.keys.sorted().take(2).forEach { h.vm.cycleVelocity(it) }
            }
            c.startsWith("notes:") || c == "track:sub" -> {
                assertTrue("${s.id}: « Créer la piste … » offered", has("tuto_need"))
                click("tuto_need")
                await("${s.id}: its notes, dotted") { has("piano_roll") && TutoHints.notes?.second?.isNotEmpty() == true }
                val (track, notes) = TutoHints.notes!!
                assertEquals(track, h.vm.selectedTrack.value)
                assertTrue("${s.id}: nothing written by the button", p().track(track).notes.notes.isEmpty())
                notes.forEach { h.vm.addNote(it) }
            }
            c == "changed:kit" -> {
                // The kick's sheet: a shorter, drier kick (its decay).
                click("tuto_do")
                closeSheetIfAny()
                h.vm.session.edit { q -> q.copy(padVoices = q.padVoices + (0 to (q.padVoices[0] ?: com.pocketstudio.model.PadVoice(com.pocketstudio.model.PadVoice.KICK_STUDIO)).with(com.pocketstudio.model.PadVoice.DECAY, 0.2f))) }
            }
            c.startsWith("changed:synth:") -> {
                click("tuto_do")
                val id = TutoChecks.trackFor(p(), c.removePrefix("changed:synth:"))!!
                h.vm.selectTrack(id)
                h.vm.setSynthParam(SynthPatch.CUTOFF, 0.93f)
                closeSheetIfAny()
            }
            c.startsWith("part:") || c.startsWith("label:") || c == "song:phrases" -> {
                assertTrue("${s.id}: its button", has("tuto_need"))
                click("tuto_need")
            }
            c == "song:order" -> {
                click("tuto_do")
                await("the song's page") { has("session") }
                for (label in listOf("Intro", "Montée", "Drop", "Break", "Montée", "Drop 2", "Fin")) {
                    val i = p().allScenes.indexOfFirst { it.label == label }
                    assertTrue(label, i >= 0)
                    click("add_part_$i")
                }
            }
            else -> {
                // The part a transition works in, then the gesture (a mixer's, a curve's, a pad's) as its target has it.
                if (has("tuto_need")) click("tuto_need")
                if (has("tuto_do")) click("tuto_do")
                closeSheetIfAny()
                val done = TutoChecks.show(s, p(), target)
                assertNotNull("${s.id}: no way to do it", done)
                h.vm.session.edit { done!! }
            }
        }
    }

    private fun closeSheetIfAny() {
        compose.waitForIdle()
        if (has("sound_chooser") || has("kit_sheet") || has("tempo_sheet")) closeSheet()
    }
}
