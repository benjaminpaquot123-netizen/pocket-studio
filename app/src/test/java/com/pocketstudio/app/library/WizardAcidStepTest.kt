package com.pocketstudio.app.library

import android.app.Application
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.model.AcidStep
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.WizardAcid
import com.pocketstudio.model.allScenes
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The assistant's 303 step: the line made there is the song's, and the summary lists only what plays. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h2400dp")
class WizardAcidStepTest {
    @get:Rule
    val compose = createComposeRule()
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val acidHouse = StarterWizard.style("acid_house")!!

    private fun vmOnAcidStep(): SoundLibraryViewModel {
        val vm = SoundLibraryViewModel(app)
        vm.openWizard()
        vm.chooseStyle(acidHouse.id)
        vm.setMood(StarterWizard.Mood.Dark)
        vm.stopPreview()
        while (vm.wizard.value.current != StarterWizard.Step.Acid) vm.next()
        return vm
    }

    @Test
    fun editingTheLineChangesWhatTheSongGets() {
        val vm = vmOnAcidStep()
        val recipe = WizardAcid.recipe(acidHouse, StarterWizard.Mood.Dark)!!
        assertEquals(recipe.steps, vm.wizard.value.acidPattern!!.steps)
        // Step 2 holds the root (A): a touch goes up the minor scale to B, a long touch puts an accent.
        vm.acidTapStep(1)
        vm.acidHoldStep(1)
        val step = vm.wizard.value.acidPattern!!.steps[1]
        assertEquals(AcidStep(note = 2, gate = true, accent = true), step)
        // Two bars, a sound, the cutoff turned.
        vm.setAcidBars(2)
        vm.chooseAcidSound("Saturée")
        vm.setAcidKnob(0, 0.8f)
        val w = vm.wizard.value
        assertEquals(32, w.acidPattern!!.length)
        assertEquals(0.8f, w.acidPattern!!.sound.cutoff)
        assertEquals(WizardAcid.sound("Saturée")!!.drive, w.acidPattern!!.sound.drive)
        assertEquals("Acid 303 · Saturée · 2 mesures · ta ligne", acidSummary(w))
        // The generator: a new line on the scale; "Celle du style" brings the style's back (the sound stays).
        vm.setAcidDensity(1f)
        vm.generateAcid()
        assertNotEquals(recipe.steps.take(32), vm.wizard.value.acidPattern!!.steps.take(32))
        vm.acidFromStyle()
        assertEquals(recipe.steps, vm.wizard.value.acidPattern!!.steps)
        assertEquals(0.8f, vm.wizard.value.acidPattern!!.sound.cutoff)
    }

    @Test
    fun theSongBuiltHasTheLineOfTheStep() {
        val vm = vmOnAcidStep()
        vm.acidTapStep(1)
        vm.chooseAcidSound("Criarde")
        val w = vm.wizard.value
        val sounds = acidHouse.lines.associate { it.role to null as com.pocketstudio.model.PadSample? }
        // As create() builds it.
        val project = StarterWizard.build(acidHouse, sounds, w.mood, emptyMap(), acid = w.acidPlays, chosenAcid = w.acidPattern)
        w.plan.acid.forEach { scene ->
            assertEquals(w.acidPattern!!.steps, project.allScenes[scene].acid.steps)
        }
        assertEquals(WizardAcid.sound("Criarde")!!.resonance, project.acid.sound.resonance)
    }

    @Test
    fun theSummaryListsOnlyWhatReallyPlays() {
        val vm = vmOnAcidStep()
        // Acid house as Benjamin made it: chords and lead left off, then the 303 too.
        StarterWizard.Part.entries.forEach { vm.setPartEnabled(it, false) }
        vm.setAcidEnabled(false)
        val off = vm.wizard.value
        val everything = (0 until off.plan.count).flatMap { off.plan.contents(it) }
        listOf("Basse", "Nappe / accords", "Lead / mélodie", "Ligne acide").forEach { assertFalse(it, it in everything) }
        val guide = songGuide(acidHouse.name, off.plan, inKey = true, song = songSummary(acidHouse, off.plan), acid = off.acidPlays)
        val text = guide.flatMap { it.lines }.joinToString("\n")
        listOf("Nappe", "Lead", "Ligne acide", "Acide :").forEach { assertFalse(it, text.contains(it)) }
        assertEquals("pas utilisée", acidSummary(off))
        // The 303 kept, its notes all taken out: it does not play either.
        vm.setAcidEnabled(true)
        val line = vm.wizard.value.acidPattern!!
        (0 until line.length).filter { line.steps[it].gate }.forEach { i ->
            while (vm.wizard.value.acidPattern!!.steps[i].gate) vm.acidTapStep(i)
        }
        val silent = vm.wizard.value
        assertFalse(silent.acidPlays)
        assertTrue(silent.plan.acid.isEmpty())
        assertFalse((0 until silent.plan.count).flatMap { silent.plan.contents(it) }.contains("Ligne acide"))
        // Kept with notes: in the plan, and the guide says where it plays.
        vm.acidFromStyle()
        val on = vm.wizard.value
        assertTrue(on.acidPlays)
        val guideOn = songGuide(acidHouse.name, on.plan, inKey = true, acid = on.acidPlays).flatMap { it.lines }
        assertTrue(guideOn.any { it.startsWith("Acide : ta ligne 303 joue dans Groove, Drop et Break.") })
    }

    @Test
    fun theLoopWithTheBeatGoesRoundAndRestartsWithEachChange() {
        assertTrue(loops(SoundRef.AcidDemo))
        assertTrue(restartsOnEdit(SoundRef.AcidDemo, null))
        assertTrue(restartsOnEdit(null, SoundRef.AcidDemo))
        assertFalse(restartsOnEdit(SoundRef.StyleDemo("house"), null))
        assertFalse(loops(SoundRef.StyleDemo("house")))
        // One loop exactly as long as its bars, the ringing end laid over the start.
        val render = FloatArray(14) { if (it >= 10) 1f else 0.5f }
        val loop = SoundLibraryViewModel.loopOf(render, 5)
        assertEquals(10, loop.size)
        assertEquals(listOf(1.5f, 1.5f, 1.5f, 1.5f, 0.5f), loop.take(5))
    }

    @Test
    fun aTouchOnAStepOfTheGridEditsIt() {
        val vm = vmOnAcidStep()
        compose.setContent {
            val ui by vm.wizard.collectAsState()
            PocketStudioTheme {
                WizardScreen(
                    ui, PacksUi(IndexState.Unavailable), null,
                    WizardActions(acidTap = vm::acidTapStep, acidHold = vm::acidHoldStep, setAcidBars = vm::setAcidBars),
                )
            }
        }
        val cell = compose.onNodeWithTag("wizard_acid_step_1")
        cell.performScrollTo().assert(hasText("La"))
        cell.performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("wizard_acid_step_1").assert(hasText("Si"))
        assertEquals(2, vm.wizard.value.acidPattern!!.steps[1].note)
        compose.onNodeWithTag("wizard_acid_step_1").performTouchInput { longClick() }
        compose.waitForIdle()
        assertTrue(vm.wizard.value.acidPattern!!.steps[1].accent)
        // A second bar shows its steps.
        compose.onNodeWithTag("wizard_acid_bars_2").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("wizard_acid_step_17").performScrollTo().assert(hasText("Si"))
    }

    @Test
    fun changingTheMoodAfterWritingTheLineKeepsItInTheSongsKey() {
        val vm = vmOnAcidStep()
        // A note of this mood's scale that another mood's scale does not have.
        val dark = vm.wizard.value.acidHarmony!!
        val (other, offset) = StarterWizard.Mood.entries.firstNotNullOf { m ->
            val h = StarterWizard.harmonyOf(acidHouse, m)
            WizardAcid.offsets(dark).firstOrNull { !h.inScale(h.root + it) }?.let { m to it }
        }
        // Step 2 taken up the scale to it, with an accent.
        while (vm.wizard.value.acidPattern!!.steps[1].let { !it.gate || it.note + 12 * it.octave != offset }) vm.acidTapStep(1)
        vm.acidHoldStep(1)
        val written = vm.wizard.value.acidPattern!!
        vm.setMood(other)
        vm.stopPreview()
        val h = StarterWizard.harmonyOf(acidHouse, other)
        val moved = vm.wizard.value.acidPattern!!
        moved.steps.take(moved.length).filter { it.gate }.forEach { assertTrue("$it", h.inScale(h.root + it.note + 12 * it.octave)) }
        assertEquals(written.steps.map { it.gate to it.accent }, moved.steps.map { it.gate to it.accent })
        assertEquals(written.length, moved.length)
    }

    @Test
    fun anotherStyleStartsFromTheUsualDensity() {
        val vm = vmOnAcidStep()
        vm.setAcidDensity(0.2f)
        vm.chooseStyle("acid_techno")
        vm.stopPreview()
        assertEquals(com.pocketstudio.model.AcidPattern().density, vm.wizard.value.acidDensity)
    }

    @Test
    fun aSoundAlreadyHeardIsNotLeftPreparing() {
        val vm = SoundLibraryViewModel(app)
        // A kit voice rendered before: it comes from the cache without waiting.
        vm.kitCache[0] = com.pocketstudio.app.audio.Pcm(FloatArray(960), 2, 48000)
        val ref = SoundRef.Kit(0)
        repeat(3) {
            vm.preview(ref)
            assertEquals(null, vm.preparing.value)
            vm.stopPreview()
        }
        // While it plays, a touch stops it; the next one plays it again.
        vm.preview(ref)
        assertEquals(null, vm.preparing.value)
    }
}
