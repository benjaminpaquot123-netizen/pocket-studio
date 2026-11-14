package com.pocketstudio.app.nav

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.model.guide.GuideLevels
import com.pocketstudio.model.instrumentTracks
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
 * Tour 5, « un mot par chose » (audit de prise en main § 5, « Volume »): « fort ou faible » is the only
 * word for a level in the Mode simple. No « dB » on the screens walked here: the first screen of the four tabs,
 * the ⋮ menu, the Mix (tip, drums open, Guide open) and the Batterie's track sheet; in texts and in what
 * TalkBack says (a fader's state, an action's name). Pro keeps its numbers. Also walked (tour 5, « restes »): the
 * effects box opened from Pad Live, the Guide's track sheet (layers, effects), the Guide's reference listening.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class SimpleNoDecibelTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @After
    fun done() = h.finish()

    private fun idle(ms: Long = 300) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
        compose.mainClock.advanceTimeBy(ms)
        compose.waitForIdle()
    }

    private fun tap(tag: String) {
        val node = compose.onNodeWithTag(tag)
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }

    private fun tapIfThere(tag: String) {
        if (compose.onAllNodes(androidx.compose.ui.test.hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) tap(tag)
    }

    /** Every text, TalkBack name and state on the screen (all windows), as « scene: text ». */
    private fun said(scene: String): List<String> {
        val out = mutableListOf<String>()
        fun walk(n: SemanticsNode) {
            n.config.getOrNull(SemanticsProperties.Text)?.forEach { out += "$scene: ${it.text}" }
            n.config.getOrNull(SemanticsProperties.ContentDescription)?.forEach { out += "$scene: $it" }
            n.config.getOrNull(SemanticsProperties.StateDescription)?.let { out += "$scene: $it" }
            n.config.getOrNull(androidx.compose.ui.semantics.SemanticsActions.OnClick)?.label?.let { out += "$scene: $it" }
            n.children.forEach(::walk)
        }
        compose.onAllNodes(androidx.compose.ui.test.isRoot(), useUnmergedTree = true).fetchSemanticsNodes().forEach(::walk)
        return out
    }

    private fun noDb(seen: List<String>) {
        val bad = seen.filter { it.contains("dB") }.distinct()
        assertTrue("« dB » in the Mode simple:\n" + bad.joinToString("\n"), bad.isEmpty())
    }

    @Test
    fun noDecibelOnTheTabsAndTheMore() {
        h.start(simple = true) { session.edit { acidTechnoSong() } }
        val seen = mutableListOf<String>()
        seen += said("Morceau")
        tap("tab_simple_Rythme"); seen += said("Rythme")
        tap("tab_simple_Clavier"); seen += said("Clavier")
        tap("tab_simple_Envoyer"); seen += said("Envoyer")
        tap("top_more"); seen += said("Plus (⋮)")
        noDb(seen)
    }

    /** Mix: the 303 pushed up (a tip shows), meters swinging, the drums open, the Guide open. */
    @Test
    fun noDecibelOnTheMix() {
        h.start(simple = true) { session.edit { acidTechnoSong() } }
        val seen = mutableListOf<String>()
        tap("tab_simple_Mix")
        h.vm.setChannelVolume(1, GuideLevels.faderOf(GuideLevels.faderDb(h.vm.project.value.mixer.channels[1].volume) + 3f))
        val (m, l) = swingingMeters()
        h.vm.showMetersForTest(m, l)
        idle(800)
        seen += said("Mix")
        tap("mix_drums_expand"); idle(); seen += said("Mix, batterie ouverte")
        tap("guide_bar"); idle(); seen += said("Mix, Guide ouvert")
        noDb(seen)
    }

    /** A track's sheet (its volume slider, in Morceau). */
    @Test
    fun noDecibelOnATracksSheet() {
        h.start(simple = true) { session.edit { acidTechnoSong() } }
        for (key in listOf(com.pocketstudio.model.SongPart.DRUMS)) {
            tap("simple_volume_$key")
            idle()
            val seen = said("Fiche de piste")
            assertTrue("the sheet opened", compose.onAllNodes(androidx.compose.ui.test.hasTestTag("simple_track_volume"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
            noDb(seen)
        }
    }

    /** Pad Live › Plus › Effets: a compressor (threshold, make-up gain, reduction meter) and an equaliser, opened. */
    @Test
    fun noDecibelInTheEffectsBoxFromPadLive() {
        // The open project is the « Live » one, so « Continuer » goes on in Pad Live (the activity is not rebuilt here).
        h.start(simple = true, launchChoice = true) { session.edit { com.pocketstudio.model.PadLive.newProject() } }
        h.awaitTag("launch_continue")
        tap("launch_continue")
        h.awaitTag("padlive_more")
        tap("padlive_more")
        h.vm.setInsertType(3, 0, com.pocketstudio.model.EffectType.COMPRESSOR)
        h.vm.setInsertType(3, 1, com.pocketstudio.model.EffectType.EQ)
        tap("padlive_effects")
        idle()
        val seen = mutableListOf<String>()
        tap("fx_chip_3_0"); idle(); seen += said("Effets, compresseur")
        assertTrue("the compressor's knobs showed", compose.onAllNodes(androidx.compose.ui.test.hasTestTag("fxp_3_0_0"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        tap("fx_chip_3_1"); idle(); seen += said("Effets, égaliseur")
        assertTrue("the equaliser's knobs showed", compose.onAllNodes(androidx.compose.ui.test.hasTestTag("fxp_3_1_0"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        noDb(seen)
    }

    /** The Guide's card for the bass › its track sheet › the layers editor with one layer (its volume), and the sheet's chain. */
    @Test
    fun noDecibelOnTheGuidesTrackSheet() {
        val style = com.pocketstudio.model.StarterWizard.style("jump_up")!!
        val jumpUp = com.pocketstudio.model.StarterWizard.build(style, style.lines.associate { it.role to null as com.pocketstudio.model.PadSample? }, style.melody.mood,
            com.pocketstudio.model.StarterWizard.defaultParts(style).associateWith { com.pocketstudio.model.StarterWizard.soundsFor(it, style).first() })
        h.start(simple = true) { session.edit { jumpUp } }
        val end = System.currentTimeMillis() + 60_000
        while (h.vm.guide.ui.value.loading || h.vm.guide.ui.value.next == null) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100)); compose.waitForIdle()
            check(System.currentTimeMillis() < end) { "first analysis: not in time" }
            Thread.sleep(5)
        }
        val bass = h.vm.project.value.instrumentTracks.first { it.name == "Basse" }.id
        h.vm.addLayer(bass, com.pocketstudio.model.LayerSound.synth(com.pocketstudio.model.SoundLibrary.SYNTH[0].sound as com.pocketstudio.model.SynthPatch), com.pocketstudio.model.LayerRole.AIR, com.pocketstudio.model.SoundLibrary.SYNTH[0].name)
        h.vm.setInsertType(com.pocketstudio.model.InstrumentTrack.mixerChannel(bass), 0, com.pocketstudio.model.EffectType.COMPRESSOR)
        idle(800)
        tap("guide_bar"); idle()
        tapIfThere("guide_further_toggle"); idle()
        tap("guide_stage_2"); idle()
        tap("guide_item_layers.bass"); idle()
        tap("guide_sheet"); idle()
        assertTrue("the sheet opened", compose.onAllNodes(androidx.compose.ui.test.hasTestTag("track_sheet"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        val seen = mutableListOf<String>()
        seen += said("Fiche de piste du Guide")
        tap("track_sheet_layers"); idle()
        seen += said("Couches")
        assertTrue("a layer showed", compose.onAllNodes(androidx.compose.ui.test.hasTestTag("layer_gain_0"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        noDb(seen)
    }

    /** The Guide's reference listening: what it says about how much louder the reference was. */
    @Test
    fun noDecibelInTheReferenceListening() {
        h.start(simple = true) { session.edit { acidTechnoSong() } }
        for (db in listOf(4.2f, -3.1f, 0.2f)) {
            val note = com.pocketstudio.app.guide.GuideListen.louderNote(db)
            noDb(listOf("Écoute de référence ($db): $note"))
        }
    }
}
