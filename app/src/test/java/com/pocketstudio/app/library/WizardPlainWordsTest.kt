package com.pocketstudio.app.library

import android.app.Application
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.app.sounds.SoundLooks
import com.pocketstudio.model.PackEntry
import com.pocketstudio.model.PackIndex
import com.pocketstudio.model.PackManifest
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.SynthPatch
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Remix testers (lot « remix 1 », Sarah in the Mode simple): the assistant's words of the trade get a
 * plain line, « A C » and « Scie · 5 voix · te… » go, the style's ▶ that cannot play says so, and a
 * download ends with « C'est prêt ».
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h2400dp")
class WizardPlainWordsTest {
    @get:Rule
    val compose = createComposeRule()
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val jumpUp = StarterWizard.style("jump_up")!!

    @After
    fun full() = SettingsStore.update(app) { it.copy(simpleMode = false) }

    private fun simple(on: Boolean) = SettingsStore.update(app) { it.copy(simpleMode = on) }

    @Test
    fun theWordsOfTheTradeAreFoundInTheFrenchTexts() {
        val kick = jumpUp.lines.first { it.role == StarterWizard.Role.Kick }.tip
        assertTrue(PlainWords.of(kick).single().startsWith("Two-step :"))
        val sub = PlainWords.of("Une piste « Sub » la double une octave plus bas, pour le hoover.")
        assertEquals(listOf("Hoover", "Sub", "Octave"), sub.map { it.substringBefore(" :") })
        assertEquals(listOf("BPM"), PlainWords.of("Bass music · 174 BPM").map { it.substringBefore(" :") })
        // Whole words only: « subtil », « suboctave » or « hoovering » are not them.
        assertEquals(emptyList<String>(), PlainWords.of("Un son subtil", null))
        assertEquals(emptyList<String>(), PlainWords.of("Pas de jargon ici."))
    }

    @Test
    fun theModeSimpleSaysTwoEverydayWordsOnASynthSound() {
        val wide = SynthPatch().with(SynthPatch.OSC1_WAVE, 0f).with(SynthPatch.UNISON, 4f / 6)
        assertTrue(SoundLooks.describe(wide), SoundLooks.describe(wide).startsWith("Scie · 5 voix"))
        assertEquals("Brillant · épais", SoundLooks.describePlain(wide))
        val soft = SynthPatch().with(SynthPatch.OSC1_WAVE, 1f).with(SynthPatch.UNISON, 0f).with(SynthPatch.A_ATTACK, 0f).with(SynthPatch.A_SUSTAIN, 1f)
        assertEquals("Doux · tenu", SoundLooks.describePlain(soft))
    }

    private fun vmOnKick(): SoundLibraryViewModel {
        val vm = SoundLibraryViewModel(app)
        vm.openWizard()
        vm.chooseStyle(jumpUp.id)
        vm.stopPreview()
        while (vm.wizard.value.current != StarterWizard.Step.Drum(jumpUp.lines.first())) vm.next()
        return vm
    }

    private fun show(vm: SoundLibraryViewModel, packs: PacksUi = PacksUi()) = compose.setContent {
        val ui by vm.wizard.collectAsState()
        PocketStudioTheme { WizardScreen(ui, packs, null, WizardActions()) }
    }

    @Test
    fun inTheModeSimpleTheKickStepExplainsTwoStepAndHidesTheLetters() {
        simple(true)
        show(vmOnKick())
        compose.onNodeWithTag("wizard_plain_words").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("• Two-step : le nom de ce rythme de batterie, deux coups par mesure.").assertExists()
        // « Joue dans Intro et Drop » stays, without the bricks' letters.
        compose.onNodeWithTag("wizard_scene_badge").assertExists()
        compose.onNode(hasText("A") and hasAnyAncestor(hasTestTag("wizard_scene_badge"))).assertDoesNotExist()
    }

    @Test
    fun theFullAppKeepsItsWords() {
        simple(false)
        show(vmOnKick())
        compose.onNodeWithTag("wizard_scene_badge").assertExists()
        compose.onNodeWithTag("wizard_plain_words").assertDoesNotExist()
        compose.onNode(hasText("A") and hasAnyAncestor(hasTestTag("wizard_scene_badge"))).assertExists()
    }

    /** Sarah touched ▶ twice: nothing at all. A preview that cannot be made (no engine here) says so. */
    @Test
    fun aStylePreviewThatCannotBeMadeSaysSo() {
        val vm = SoundLibraryViewModel(app)
        vm.openWizard()
        vm.preview(SoundRef.StyleDemo(jumpUp.id))
        val end = System.currentTimeMillis() + 10_000
        while (vm.packs.value.message == null && System.currentTimeMillis() < end) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }
        assertEquals("Cette écoute n'a pas pu être préparée : réessaie dans un instant.", vm.packs.value.message)
        compose.setContent {
            val ui by vm.wizard.collectAsState()
            val packs by vm.packs.collectAsState()
            PocketStudioTheme { WizardScreen(ui, packs, null, WizardActions()) }
        }
        compose.onNodeWithTag("wizard_style_message").assertIsDisplayed()
    }

    @Test
    fun theStylesSoundsSayHowFarThenThatTheyAreReady() {
        val vm = SoundLibraryViewModel(app)
        vm.openWizard()
        vm.chooseStyle(jumpUp.id)
        vm.stopPreview()
        assertEquals(StarterWizard.Step.Mood, vm.wizard.value.current)
        val index = PackIndex(jumpUp.packs.map { PackEntry(it, "Pack $it", "$it.zip", "a".repeat(64), 1_200_000, 60) })
        var packs by androidx.compose.runtime.mutableStateOf(PacksUi(IndexState.Ready(index), downloading = jumpUp.packs.associateWith { 0.4f }))
        compose.setContent {
            val ui by vm.wizard.collectAsState()
            PocketStudioTheme { WizardScreen(ui, packs, null, WizardActions()) }
        }
        compose.onNodeWithTag("wizard_style_packs_progress").assertIsDisplayed()
        compose.onNodeWithText("Téléchargement… 40 %").assertExists()
        // Every pack in: the card goes, a green line says it.
        packs = PacksUi(IndexState.Ready(index), jumpUp.packs.map { PackManifest(it, "Pack $it") }, readyNote = "C'est prêt : le pack « Drum & bass » est sur ton téléphone.")
        compose.waitForIdle()
        compose.onNodeWithTag("wizard_style_packs").assertDoesNotExist()
        compose.onNodeWithTag("wizard_style_packs_ready").assertIsDisplayed()
    }
}
