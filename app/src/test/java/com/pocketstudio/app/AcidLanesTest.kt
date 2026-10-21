package com.pocketstudio.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.mixer.BASIC_STRIPS
import com.pocketstudio.app.mixer.StripInfo
import com.pocketstudio.app.mixer.automationLabel
import com.pocketstudio.app.mixer.automationTargets
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.Mixer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.model.AutomationLane
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectJson
import com.pocketstudio.model.acidKnobsDriven
import com.pocketstudio.model.automatedAcidKnobs
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** The 303's knobs in « Automatiser un réglage » and in a project's lanes. */
@RunWith(RobolectricTestRunner::class)
class AcidLanesTest {
    @get:Rule
    val compose = androidx.compose.ui.test.junit4.createComposeRule()
    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun theAcidStripOffersIts303Knobs() {
        val mixer = Mixer()
        val targets = automationTargets(mixer.channels, mixer.master)
        val acid = AutoTarget.ACID_PARAMS.map { AutoTarget.acid(it) }
        assertTrue(targets.containsAll(acid))
        // First in the acid line's group, before its volume.
        assertEquals(acid, targets.subList(targets.indexOf(acid.first()), targets.indexOf(acid.first()) + acid.size))
        assertTrue(targets.indexOf(acid.last()) < targets.indexOf(AutoTarget(AutoTarget.VOLUME, 1)))
        assertEquals("303 Coupure · Basse acide", automationLabel(AutoTarget.acid(0), mixer.channels, mixer.master))
        assertEquals("303 Saturation · Basse acide", automationLabel(AutoTarget.acid(7), mixer.channels, mixer.master))
        // Without the acid strip (a list of other strips), none of them.
        val noAcid = automationTargets(mixer.channels, mixer.master, listOf(StripInfo(0, "Batterie")))
        assertTrue(noAcid.none { it.kind == AutoTarget.ACID })
        assertTrue(BASIC_STRIPS.any { it.ch == AutoTarget.ACID_CHANNEL })
    }

    @Test
    fun aLaneOnThe303CutoffIsKept() {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        vm.addAutomation(AutoTarget.acid(0))
        vm.setAcidSound(0, 0.6f)  // not recording: the knob only
        assertEquals(listOf(AutoTarget.acid(0)), vm.project.value.automation.map { it.target })
        assertEquals(0.6f, vm.project.value.acid.sound.cutoff, 1e-6f)
    }

    @Test
    fun aKnobTurnedWhileRecordingIsWrittenIntoItsLane() {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        // No lane yet: the gesture makes one, on the step the engine is at.
        vm.recordGesture(AutoTarget.acid(1), 0.7f, step = 3, songStep = -1)
        val lane = vm.project.value.automation.single()
        assertEquals(AutoTarget.acid(1), lane.target)
        assertEquals(0.7f, lane.values[3]!!, 1e-6f)
        assertNull(lane.values[2])
        // A song curve: on the bar the song is at (step 35 of the song: bar 2).
        vm.setAutomationMode(0, AutomationLane.SONG)
        vm.recordGesture(AutoTarget.acid(1), 0.4f, step = 3, songStep = 35)
        assertEquals(0.4f, vm.project.value.automation[0].songValues[2]!!, 1e-6f)
        // Out of the loop, or a knob that cannot be automated: nothing written.
        val before = vm.project.value
        vm.recordGesture(AutoTarget.acid(1), 0.9f, step = AutomationLane.STEPS, songStep = -1)
        vm.recordGesture(AutoTarget.acid(5), 0.9f, step = 1, songStep = -1)
        assertEquals(before, vm.project.value)
    }

    @Test
    fun aFingerOnAKnobTakesItOverUntilLetGo() {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        vm.holdAcidKnob(0, true)
        assertEquals(setOf(AutoTarget.acid(0)), vm.heldSettingsForTests)
        vm.holdAcidKnob(5, true)  // tuning: no lane drives it, nothing to take over
        assertEquals(setOf(AutoTarget.acid(0)), vm.heldSettingsForTests)
        vm.holdAcidKnob(0, false)
        assertTrue(vm.heldSettingsForTests.isEmpty())
        // The knob reports the finger down and up.
        val holds = ArrayList<Pair<Int, Boolean>>()
        compose.setContent {
            com.pocketstudio.ui.PocketStudioTheme {
                com.pocketstudio.app.acid.Panel303(com.pocketstudio.model.AcidSound().values(), { _, _ -> }) { i, held -> holds += i to held }
            }
        }
        compose.onNodeWithTag("acid_knob_0").performTouchInput { down(center); moveBy(androidx.compose.ui.geometry.Offset(0f, -40f)) }
        compose.waitForIdle()
        assertEquals(listOf(0 to true), holds)
        compose.onNodeWithTag("acid_knob_0").performTouchInput { up() }
        compose.waitForIdle()
        assertEquals(listOf(0 to true, 0 to false), holds)
    }

    @Test
    fun theKnobsALaneMovesAreNamedUnderThem() {
        val p = Project().copy(automation = listOf(AutomationLane(AutoTarget.acid(0), mode = AutomationLane.SONG).withSongPoint(0, 0.3f)))
        assertEquals(setOf(0), p.automatedAcidKnobs)
        compose.setContent {
            com.pocketstudio.ui.PocketStudioTheme {
                com.pocketstudio.app.acid.AcidScreen(p.acid, playing = true, currentStep = 0, selected = 0, actions = com.pocketstudio.app.acid.AcidActions(), automated = p.automatedAcidKnobs)
            }
        }
        compose.onNodeWithTag("acid_automated").performScrollTo()
            .assert(hasText("Bouge tout seul (courbe d'automation) : Coupure. Garde le doigt dessus pour le reprendre."))
        // A lane without a point moves nothing.
        assertTrue(Project().copy(automation = listOf(AutomationLane(AutoTarget.acid(1)))).automatedAcidKnobs.isEmpty())
    }

    @Test
    fun aSceneLeavesAloneOnlyTheKnobsItsLanesMove() {
        // Cutoff per scene, drawn in scene A only; resonance on the song curve; decay on a loop lane.
        val p = Project().copy(
            automation = listOf(
                AutomationLane(AutoTarget.acid(0), mode = AutomationLane.PER_SCENE).withPoint(0, 0.2f, scene = 0),
                AutomationLane(AutoTarget.acid(1), mode = AutomationLane.SONG).withSongPoint(0, 0.5f),
                AutomationLane(AutoTarget.acid(3)).withPoint(4, 0.6f),
                AutomationLane(AutoTarget.acid(4)),  // no point: moves nothing
            ),
        )
        assertEquals(setOf(0, 1, 3), p.acidKnobsDriven(0, inSong = true))
        assertEquals(setOf(1, 3), p.acidKnobsDriven(1, inSong = true))  // scene B: the cutoff back to its knob
        assertEquals(setOf(3), p.acidKnobsDriven(1, inSong = false))  // B alone, not in the song: no curve there
    }

    @Test
    fun oldProjectsWithUnknownLanesAreCleanedAtLoad() {
        val p = Project().copy(automation = listOf(AutomationLane(AutoTarget.acid(0)).withPoint(0, 0.5f)))
        val text = ProjectJson.encode(p)
        assertTrue(text.contains("\"kind\": 5") || text.contains("\"kind\":5"))
        // A lane of a kind this version does not know, and a 303 lane on tuning (not automatable).
        val bad = text.replace(Regex("\"kind\"\\s*:\\s*5"), "\"kind\": 9")
        assertTrue(ProjectJson.decode(bad).automation.isEmpty())
        val tuning = text.replace(Regex("\"param\"\\s*:\\s*0"), "\"param\": 5").let {
            if (it.contains("\"param\"")) it else it.replace("\"kind\"", "\"param\": 5, \"kind\"")
        }
        assertTrue(ProjectJson.decode(tuning).automation.isEmpty())
        assertFalse(ProjectJson.decode(text).automation.isEmpty())
    }
}
