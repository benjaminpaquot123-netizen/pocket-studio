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
import com.pocketstudio.model.StarterWizard
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
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import kotlin.math.abs

/**
 * Owner (2026-10-28): every course of `tutos/` (jump up first, each style's file then: a course
 * added is played here without a line of code), Facile and Pro, done in the app the way a user
 * does it, step after step: the step's main button, « Je le fais » (the right screen, its dotted
 * cells or notes in sight), the cells and the notes touched, the sheets' choices. At each step:
 * nothing appears by itself, « Montre-moi » shows then leaves the project exactly as it was (and
 * nothing in « Annuler »), « Écouter l'exemple » plays something to hear, the step is ticked only
 * once done. (That the examples are really audible through the engine: TutoExampleSoundBench.)
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class TutoJourneyTest(private val style: String, private val level: TutoLevel) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0} {1}")
        fun courses(): List<Array<Any>> = TutoCourse.STYLES.flatMap { s -> TutoLevel.entries.map { arrayOf<Any>(s, it) } }
    }

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)
    private val target = TutoTarget.of(style)!!
    private val course = TutoCourse.of(style)!!

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

    /** The panel unfolded (« Je le fais », a step's button, « Montre-moi » fold it out of the way). */
    private fun unfold() {
        if (has("tuto_folded_text") || has("tuto_next_folded") || has("tuto_need_folded")) click("tuto_fold")
        await("unfolded") { has("tuto_prev") }
    }

    private fun closeSheet() {
        androidx.test.espresso.Espresso.pressBack()
        compose.waitForIdle()
    }

    @Test
    fun theWholeCourse() = journey(level)

    /** The part the user adds to the song for [label]: its own, or the Intro again for a « Fin » the course does not make. */
    private fun sceneFor(label: String): Int = p().allScenes.indexOfFirst { it.label == label }.takeIf { it >= 0 }
        ?: if (label in TutoTarget.ENDINGS) p().allScenes.indexOfFirst { it.label == "Intro" } else -1

    private fun journey(level: TutoLevel) {
        TutoTestHooks.player = player
        h.start(simple = level == TutoLevel.Facile, build = {
            // « J'apprends » › level › Jump up: the tutorial's own empty project.
            session.edit { target.blank() }
            PrefsTutoStore(h.app).save(projectId, TutoProgress(style, level))
        })
        // A short tone stands for the engine's render (Robolectric has none).
        h.vm.guideRenderForTest = { a, _ -> FloatArray(OfflineExport.SAMPLE_RATE * 2) { 0.3f * kotlin.math.sin(it * 0.05f) } to FloatArray(2) }
        await("panel") { has("tuto_panel") }
        // Truly empty: no hit, no note, the first track hidden (no « Synthé » in « Mes pistes »).
        assertTrue(p().drums.lanes.all { it.hits.isEmpty() })
        assertTrue(TutoChecks.blankFirst(p()))
        val steps = course.chaptersFor(level).flatMap { it.steps }
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
            // The share: only the app sees the song go out (no « Montre-moi » can share it).
            if (!has("tuto_show") && !s.manual && !s.listening && !s.check.startsWith("changed:synth") && s.check != "shared") noDemo += s.id
            if (has("tuto_show")) {
                click("tuto_show")
                await("${s.id}: demo shown") { has("tuto_demo_caption") }
                // Testers (lot « tuto 3 »): the demonstration is seen where it happens, the panel folded.
                assertFalse("${s.id}: panel open during the demo", has("tuto_text"))
                await("${s.id}: demo over", 60_000) { !has("tuto_demo_caption") }
                idle(400)
                unfold()
                assertEquals("${s.id}: Montre-moi left something", content(start), content(p()))
                assertEquals("${s.id}: Montre-moi in Annuler", depth, h.vm.session.undoDepth)
                assertFalse("${s.id}: ticked by Montre-moi", s.id in progress().done)
                demos++
            }
            // « Écoute ta boucle, puis l'exemple » (lot « tuto 4 »): the user's own music first, the example then ticks it.
            if (s.hearMine && TutoController.mineFor(s, p())) {
                click("tuto_play_mine")
                compose.mainClock.advanceTimeBy(5_000)
                idle(400)
                assertFalse("${s.id}: ticked before the example", s.id in progress().done)
                if (has("tuto_play_mine")) click("tuto_play_mine")
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
            unfold()
            if (s.check.startsWith("lane:")) assertTrue("${s.id}: « Écouter ton résultat »", has("tuto_play_mine"))
            click("tuto_next_big")
        }
        assertTrue(steps.all { it.id in progress().done })
        // Only a step a former one already did may be ticked before it is reached.
        assertTrue("ticked before reached: $early", early.size <= 2)
        // Every step with something to show showed it, every example was played.
        assertTrue("no Montre-moi: $noDemo", noDemo.isEmpty())
        if (style == "jump_up") assertTrue("$demos demos", demos >= 18)
        assertEquals("early: $early", steps.count { it.listen.isNotEmpty() && it.id !in early }, examples)
        // The song made by hand: its parts in the course's order (jump up: seven parts).
        steps.firstOrNull { it.check == "song:order" }?.let { order ->
            val wanted = order.song.split(',').map { it.substringBeforeLast(':').trim() }
            assertEquals(wanted.map { l -> p().allScenes[sceneFor(l)].label }, p().song.map { p().allScenes[it.scene].label })
        }
        if (style == "jump_up") assertEquals(listOf("Intro", "Montée", "Drop", "Break", "Montée", "Drop 2", "Fin"), p().song.map { p().allScenes[it.scene].label })
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
                h.vm.setTempo(target.style.bpm)
                closeSheet()
            }
            c == "key" -> {
                click("tuto_do")
                await("key sheet") { has("tuto_key_sheet") }
                click("tuto_scale_${target.harmony.scale.name}")
                click("tuto_root_${target.harmony.root}")
                await("key set") { p().harmony.root == target.harmony.root && p().harmony.scale == target.harmony.scale }
                closeSheet()
            }
            c.startsWith("lane:") -> {
                if (has("tuto_need")) click("tuto_need")
                await("need met") { !has("tuto_need") }
                if (!has("tuto_do")) unfold()
                click("tuto_do")
                await("${s.id}: the grid on its pad, cells dotted") { has("rhythm") && has("tuto_hint") }
                // « Je le fais »: a ring that goes by itself, no card to close; the panel folds out of the way (lot « tuto 3 »).
                assertFalse(has("guide_spot"))
                assertTrue(has("tuto_folded_text"))
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
                // "lane:<role>:rolls" (Pro): each roll of the line made as a user would (long press › « Roulement » ×N).
                if (c.endsWith(":rolls")) {
                    val line = target.line(c.removePrefix("lane:").substringBefore(':'))!!
                    StarterWizard.hits(line, p().drums.steps).filterValues { it.repeats > 1 }.forEach { (cell, hit) ->
                        h.vm.setRoll(cell, hit.repeats, 0)
                        compose.waitForIdle()
                    }
                }
            }
            c == "velocity:hat" -> {
                click("tuto_do")
                val lane = TutoChecks.laneOf("hat")
                h.vm.selectPad(lane)
                // Long presses › « Doux »: the 4 hats on the beats of the first bar, then « Copier sur les mesures suivantes ».
                listOf(0, 4, 8, 12).forEach { h.vm.setHitVelocity(it, 0.2f) }
                copy(s)
            }
            // « Copier la basse une octave plus bas »: the Sub in one touch (lot « tuto 4 »).
            c == "track:sub" -> {
                assertTrue("${s.id}: its button", has("tuto_need"))
                click("tuto_need")
            }
            // Lot « tuto 5 »: a bar's dotted notes touched, « Copier sur les mesures suivantes », the next bar to write,
            // « Copier » again; the Drop 2's bass in its part (Pro: then its Sub, the bass an octave down).
            // The 303: its dotted steps written in the Acide tab, then « Copier » (lot « tous les styles »).
            c == "notes:acid" -> {
                if (has("tuto_do")) click("tuto_do")
                var rounds = 0
                while (s.id !in progress().done) {
                    assertTrue("${s.id}: $rounds rounds", ++rounds < 8)
                    await("${s.id}: the dotted steps of now") { TutoHints.acid == TutoChecks.acidWanted(s, p(), target) }
                    val wanted = TutoHints.acid!!
                    h.vm.session.edit { q -> q.copy(acid = q.acid.copy(root = target.acid!!.root, length = maxOf(q.acid.length, ((wanted.keys.maxOrNull() ?: 0) / 16 + 1) * 16), steps = q.acid.steps.mapIndexed { i, st -> wanted[i] ?: st })) }
                    compose.waitForIdle()
                    copy(s)
                    compose.waitForIdle()
                }
            }
            c.startsWith("notes:") || (c.startsWith("part:") && s.transpose != 0) -> {
                assertTrue("${s.id}: « Créer la piste … » offered", has("tuto_need"))
                click("tuto_need")
                await("${s.id}: its notes, dotted") { has("piano_roll") && TutoHints.notes?.second?.isNotEmpty() == true }
                val (track, notes) = TutoHints.notes!!
                assertEquals(track, h.vm.selectedTrack.value)
                assertTrue("${s.id}: nothing written by the button", p().track(track).notes.notes.isEmpty())
                assertTrue("${s.id}: a bar or two dotted", notes.map { it.start / 384 }.distinct().size <= 2)
                var rounds = 0
                while (s.id !in progress().done) {
                    assertTrue("${s.id}: $rounds rounds", ++rounds < 8)
                    await("${s.id}: the dotted notes of now") { TutoHints.notes?.second == TutoChecks.ghostNotes(s, p(), target)?.second }
                    val have = p().track(track).notes.notes
                    TutoHints.notes!!.second.filter { g -> have.none { it.pitch == g.pitch && it.start == g.start } }.forEach { h.vm.addNote(it) }
                    copy(s)
                    compose.waitForIdle()
                }
            }
            c.startsWith("changed:synth:") -> {
                click("tuto_do")
                val spec = c.removePrefix("changed:synth:")
                val id = TutoChecks.trackFor(p(), spec.substringBefore(':'))!!
                h.vm.selectTrack(id)
                // The knobs the step names, each turned up (« Coupure », « Résonance »…).
                val knobs = spec.substringAfter(':', "").split('+').filter { it.isNotBlank() }.map { SynthPatch::class.java.getField(it).getInt(null) }
                for (k in knobs.ifEmpty { listOf(SynthPatch.CUTOFF) }) h.vm.setSynthParam(k, (p().track(id).synth.get(k) + 0.3f).coerceAtMost(1f))
                closeSheetIfAny()
            }
            c.startsWith("part:") || c.startsWith("label:") || c == "song:phrases" -> {
                assertTrue("${s.id}: its button", has("tuto_need"))
                click("tuto_need")
            }
            // The song exported and its share sheet opened: the app marks it (as ShareSongProgress does).
            c == "shared" -> {
                click("tuto_do")
                h.vm.guide.markDone(TutoChecks.SHARED_CARD, "keep")
            }
            c == "song:order" -> {
                click("tuto_do")
                await("the song's page") { has("session") }
                for (label in s.song.split(',').map { it.substringBeforeLast(':').trim() }) {
                    val i = sceneFor(label)
                    assertTrue(label, i >= 0)
                    click("add_part_$i")
                }
            }
            else -> {
                // The part a transition works in, then the gesture (a mixer's, a curve's, a pad's) as its target has it.
                if (has("tuto_need")) click("tuto_need")
                if (has("tuto_do")) click("tuto_do")
                // Testers (lot « tuto 3 »): the hits to take away are crossed out where they are, their bar shown.
                if (c == "gap") {
                    // The page turns to the bar holding them once the last touch was seen (the composition's clock).
                    compose.mainClock.advanceTimeBy(2_000)
                    await("${s.id}: the hits to take away, crossed out") { has("tuto_erase") }
                }
                closeSheetIfAny()
                val done = TutoChecks.show(s, p(), target)
                assertNotNull("${s.id}: no way to do it", done)
                h.vm.session.edit { done!! }
            }
        }
    }

    /** The step's main button once the user's part is done (« Copier sur les mesures suivantes », « Copier la basse… »). */
    private fun copy(s: TutoStep) {
        await("${s.id}: its copy button") { s.id in progress().done || h.vm.project.value.let { TutoChecks.need(s, it, target) } != null }
        if (s.id in progress().done) return
        unfold()
        await("${s.id}: the button") { has("tuto_need") }
        click("tuto_need")
    }

    private fun closeSheetIfAny() {
        compose.waitForIdle()
        if (has("sound_chooser") || has("kit_sheet") || has("tempo_sheet")) closeSheet()
        if (has("fx_close")) click("fx_close")
    }
}
