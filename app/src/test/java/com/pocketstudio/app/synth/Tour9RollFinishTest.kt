package com.pocketstudio.app.synth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.Project
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Tour 9, lot L7, second pass: the piano roll's and the Clavier's finishes (F112 to F118, F142). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-port-hdpi")
class Tour9RollFinishTest {
    @get:Rule val compose = createComposeRule()

    private fun bigLetters(content: @Composable () -> Unit) = compose.setContent {
        val d = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(d.density, 2f)) { PocketStudioTheme { content() } }
    }

    /** F142: « Après un enregistrement, l'espacement du synthé revient à la normale. » */
    @Test
    fun theKeysGetTheirHeightBackOnceTheTakesCardIsGone() {
        val take = mutableStateOf<(@Composable (Boolean) -> Unit)?>(null)
        bigLetters { SynthScreen(Project().synth, 4, SynthActions(), simple = true, record = take.value) }
        compose.waitForIdle()
        val before = compose.onNodeWithTag("synth_keys_first").fetchSemanticsNode().size.height
        take.value = { Box(Modifier.fillMaxWidth().height(720.dp)) }
        compose.waitForIdle()
        val during = compose.onNodeWithTag("synth_keys_first").fetchSemanticsNode().size.height
        take.value = null
        compose.waitForIdle()
        val after = compose.onNodeWithTag("synth_keys_first").fetchSemanticsNode().size.height
        assertEquals("the keys, before a take ($before px) and during it ($during px)", true, during < before)
        assertEquals("the keys once the take's card is gone", before, after)
    }

    /** F114: « Une piste mélodique s'ouvre sans basse fantôme sélectionnée. » */
    @Test
    fun aMelodyTrackOpensWithoutTheGhostBassTicked() {
        val t = NoteClip.TICKS_PER_STEP
        compose.setContent {
            PocketStudioTheme { PianoRoll(NoteClip(steps = 64), listOf(Note(0, 4 * t, 36)), -1, PianoRollActions(), trackName = "Mélodie") }
        }
        compose.onNodeWithText("Basse en fantôme").assertIsNotSelected()
    }

    /**
     * F113: « La note mise en évidence est celle que la consigne demande de toucher. » A chords step asks for « the
     * bottom dotted note of each chord » (a touch there writes the whole chord): the note lit is the chord's bottom one.
     */
    @Test
    fun theNoteLitInAChordIsItsBottomOne() {
        val t = NoteClip.TICKS_PER_STEP
        val chords = listOf(0, 16).flatMap { s -> listOf(56, 60, 63).map { p -> Note(s * t, 8 * t, p) } }
            .sortedWith(compareBy({ it.start }, { -it.pitch }))
        val view = com.pocketstudio.model.tuto.TutoRollView(0f, 1000f, 0f, 2000f, 20f, 20f, 100)
        val asked = aimedNotes(chords, chordMode = true)
        val lit = com.pocketstudio.model.tuto.TutoRollHint.lit(asked, view, 16)
        assertEquals("the chord's bottom note", Note(0, 8 * t, 56), lit)
        assertEquals("the next one, the next chord's bottom", listOf(56, 56), asked.map { it.pitch })
        // Without the chords' mode each note is touched: they all stay asked for.
        assertEquals(chords, aimedNotes(chords, chordMode = false))
    }
    /**
     * F118 (seat t9-27, id): the tutorial's motif copied over bars 1 to 5, the roll on bar 2, the button read « Tempel
     * birama 6 » (bar 6, out of sight). A paste goes on the bar chosen or after a copy while it is in sight, else on
     * the bar the eye is on (the middle of the roll), and on the bar playing while it plays.
     */
    @Test
    fun thePasteGoesOnABarInSight() {
        val bar = 300f
        // Bar 2 in sight (from 300 to 600 px), the target after the copy is bar 6 (index 5): the paste goes on bar 2.
        assertEquals(1, pasteBarFor(playBar = -1, targetBar = 5, left = 300f, right = 600f, barPx = bar, lastBar = 7))
        // The target in sight is kept (copy bar 1, paste on bar 2 next to it).
        assertEquals(1, pasteBarFor(playBar = -1, targetBar = 1, left = 0f, right = 700f, barPx = bar, lastBar = 7))
        // The bar just after the one in sight is kept (a one-bar loop copied, pasted beside it as it grows).
        assertEquals(1, pasteBarFor(playBar = -1, targetBar = 1, left = 0f, right = 300f, barPx = bar, lastBar = 7))
        // Playing: the bar playing, whatever is in sight.
        assertEquals(4, pasteBarFor(playBar = 4, targetBar = 1, left = 0f, right = 700f, barPx = bar, lastBar = 7))
        // Nothing laid out yet (no width): the target as before.
        assertEquals(5, pasteBarFor(playBar = -1, targetBar = 5, left = 0f, right = 0f, barPx = bar, lastBar = 7))
        // Never past the last bar a clip can hold.
        assertEquals(7, pasteBarFor(playBar = -1, targetBar = 9, left = 2100f, right = 2400f, barPx = bar, lastBar = 7))
    }
    /**
     * F116 (seats t9-27, t9-58, t9-69): a library sound, then a knob turned (the tutorial's step), the tab read « Melodi
     * Synth » / « Mélodie Plaits »: the engine's name, the sound's lost. It keeps the sound's name; a fresh synth's stays.
     */
    @Test
    fun aSoundWithAKnobTurnedKeepsItsNameOnTheTrack() {
        fun Float.turned() = if (this < 0.5f) this + 0.3f else this - 0.3f
        val lead = com.pocketstudio.model.SoundLibrary.SYNTH.first { it.category == "Leads" }
        val synth = com.pocketstudio.model.SynthPatch.RESONANCE
        val t = com.pocketstudio.model.InstrumentTrack(id = 0, name = "Mélodie", synth = lead.sound.with(synth, lead.sound.get(synth).turned()))
        assertEquals("the engine's name before", t.soundLabel, com.pocketstudio.model.guide.GuideSheets.soundName(t))
        assertEquals(lead.name, trackSoundName(t))
        val pl = com.pocketstudio.model.SoundLibrary.PLAITS.first()
        val tp = com.pocketstudio.model.InstrumentTrack(id = 0, name = "Mélodie", engine = com.pocketstudio.model.InstrumentTrack.ENGINE_PLAITS,
            plaits = pl.sound.copy(timbre = pl.sound.timbre.turned()))
        assertEquals(pl.name, trackSoundName(tp))
        // A fresh track's sound is named as before, never after a library sound near it.
        val fresh = com.pocketstudio.model.InstrumentTrack(id = 0, name = "Piste 1")
        assertEquals(com.pocketstudio.model.guide.GuideSheets.soundName(fresh), trackSoundName(fresh))
        val freshPlaits = fresh.copy(engine = com.pocketstudio.model.InstrumentTrack.ENGINE_PLAITS)
        assertEquals(com.pocketstudio.model.guide.GuideSheets.soundName(freshPlaits), trackSoundName(freshPlaits))
    }

    /**
     * F117 (seat t9-05, phone360, Korean, 200 %): « Le clavier tient à l'écran à 200 % et son « ? » explique ce qu'il
     * fait. » The room above the tutorial's panel held the track's bar, the take's card and the keys' top only; the one
     * « ? » near them was the take's (about recording).
     */
    @Test
    fun theKeysFitAboveTheTutorialAndTheirHelpSaysWhatTheyDo() {
        val room = 260.dp
        val tracks = TracksUi(listOf(com.pocketstudio.model.InstrumentTrack(id = 0, name = "Acid 303")), selected = 0)
        bigLetters {
            Box(Modifier.fillMaxWidth().height(room).testTag("room")) {
                SynthScreen(Project().synth, 4, SynthActions(), simple = true, tracks = tracks,
                    record = { Box(Modifier.fillMaxWidth().height(56.dp)) })
            }
        }
        compose.waitForIdle()
        val box = compose.onNodeWithTag("room").fetchSemanticsNode().boundsInRoot
        val keys = compose.onNodeWithTag("synth_keys_first").fetchSemanticsNode().boundsInRoot
        val dp = compose.onNodeWithTag("room").fetchSemanticsNode().layoutInfo.density.density
        assertEquals("the keys ($keys) inside the room ($box)", true, keys.top >= box.top - 0.5f && keys.bottom <= box.bottom + 0.5f)
        assertEquals("the keys' height (${keys.height / dp} dp)", true, keys.height / dp >= 109.5f)
        // The keyboard's own « ? » says what the keys do, in the scale or not.
        val locked = com.pocketstudio.model.Harmony(scale = com.pocketstudio.model.Scale.Minor, lock = true)
        assertEquals("Le clavier : joue, il reste dans la gamme du morceau : aucune fausse note possible.", keyboardHelp(locked, chordPads = false)?.text)
        assertEquals("Le clavier : joue pour essayer des notes par-dessus le rythme.", keyboardHelp(com.pocketstudio.model.Harmony(), chordPads = false)?.text)
        compose.onNodeWithTag("help_Clavier", useUnmergedTree = true).assertExists()
    }
    /** F115: « Les noms de touches et de mesures se lisent sans se chevaucher, même à 200 %. » (tablet: every key named) */
    @Test
    @Config(qualifiers = "w800dp-h1280dp-port-mdpi")
    fun theRollsKeyAndBarNamesFitTheirPlaceAtTwoHundredPercentOnATablet() = rollNamesFit()

    /** F115 on a phone: the C's names and « mes. » in the 36 dp wide keys. */
    @Test
    fun theRollsKeyAndBarNamesFitTheirPlaceAtTwoHundredPercentOnAPhone() = rollNamesFit()

    private fun rollNamesFit() {
        bigLetters { PianoRoll(NoteClip(steps = 64), emptyList(), -1, PianoRollActions(), trackName = "Mélodie") }
        compose.waitForIdle()
        val texts = compose.onAllNodes(
            androidx.compose.ui.test.hasAnyAncestor(androidx.compose.ui.test.hasTestTag("roll_keys")) and
                androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult),
            useUnmergedTree = true,
        ).fetchSemanticsNodes()
        assertEquals("some keys are named", true, texts.isNotEmpty())
        val bar = compose.onAllNodes(
            androidx.compose.ui.test.hasText("mes.") and
                androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult),
            useUnmergedTree = true,
        ).fetchSemanticsNodes()
        assertEquals("the bars' label is there", 1, bar.size)
        (texts + bar).forEach { n ->
            val r = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            n.config[androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult].action!!.invoke(r)
            val said = r.single().layoutInput.text.text
            // Higher than its row, a name runs over its neighbours'; wider than its box, it is cut.
            assertEquals("« $said » cut or over its neighbours", true, com.pocketstudio.ui.WholeWordsAutoSize.fits(r.single(), said))
        }
    }
}
