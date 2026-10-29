package com.pocketstudio.app.human

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.audio.AudioDecoder
import com.pocketstudio.app.audio.FloatWav
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.export.FlacEncoder
import com.pocketstudio.app.library.PackStore
import com.pocketstudio.app.ui.DoubleTouchGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * The bugs Lucas (run 3, seat c: 17, in a hurry, touches everything) found outside the tutorial,
 * walked with his gestures on the « testeur humain » bench: touches on the screen's pixels as he
 * saw them in screen-N.png. Lot « tuto 5 », É4.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class HumanLucasBugsTest : HumanTester("lucas-bugs") {
    private val app: Application get() = ApplicationProvider.getApplicationContext()

    @Before
    fun notOnTheBench() {
        assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())
        DoubleTouchGuard.reset()
    }

    private fun tap(where: Pair<Float, Float>?, what: String): String {
        assertNotNull("nothing to touch: $what", where)
        return act("touche ${where!!.first} ${where.second}")
    }

    /** Brings the node tagged [tag] into the screen's middle part (a swipe at a time), then its middle. */
    private fun reach(tag: String): Pair<Float, Float> {
        repeat(10) {
            val at = centerOf(tag) ?: error("no $tag")
            if (at.second in 320f..(screenHeight - 360f)) return at
            val dy = (at.second - screenHeight / 2f).coerceIn(-400f, 400f)
            act("glisse 10 ${screenHeight / 2f + dy / 2} 10 ${screenHeight / 2f - dy / 2}")
        }
        return centerOf(tag)!!
    }

    /** Home › « Créer mon premier morceau (guidé) » › Bass music › Jump up DnB › « Choisir ce style » (c/1-5). */
    private fun wizardOnJumpUp() {
        tap(centerOfText("Créer mon premier morceau", substring = true), "the big orange button")
        // « Simple ou Pro ? » (owner, 2026-10-29): Simple, as a first launch suggests.
        tap(centerOf("mode_card_simple"), "Simple")
        tap(centerOfText("Bass music", substring = true), "Bass music")
        tap(centerOfText("Jump up DnB", substring = true), "the Jump up card")
        tap(centerOfText("Choisir ce style", substring = true), "Choisir ce style")
        assertNotNull("the assistant is not on its steps", centerOf("wizard_next"))
    }

    /**
     * c/41-45: « Quitter sans créer le morceau ? » › « Quitter » left him in the assistant. The
     * dialog is centred: the bench drew it there (screen-41.png, « Quitter » at 497, 746) but took
     * its touches as if it were at the screen's top, so his touch fell beside it and only closed it.
     */
    @Test
    fun quitterQuitsTheAssistantWhereTheScreenShowsIt() {
        begin()
        wizardOnJumpUp()
        tap(centerOf("wizard_next"), "Suivant")
        // The X (545, 47 on his screen), then « Quitter » where his screen showed it.
        act("touche 545 47")
        val quit = centerOfText("Quitter")
        assertNotNull("no « Quitter »", quit)
        assertEquals("« Quitter » is not where screen-41.png shows it", 746f, quit!!.second, 12f)
        act("touche 497 746")
        assertNull("still in the assistant", centerOf("wizard"))
    }

    /** c/72-77: « Revoir le guide des premiers pas »: « Suivant » and « Passer » did nothing (the same centred window). */
    @Test
    fun theWelcomeGuideAnswersSuivantAndPasser() {
        begin()
        tap(centerOfText("Partir de zéro"), "Partir de zéro")
        tap(centerOf("mode_card_simple"), "Simple")
        act("attends 2")
        // ⋮ › « Aide et premiers pas » › « Revoir le guide des premiers pas ».
        act("touche 543 43")
        tap(centerOf("more_help"), "Aide et premiers pas")
        tap(reach("help_guide"), "Revoir le guide des premiers pas")
        assertNotNull("no welcome guide", centerOfText("Bienvenue dans BenStudio"))
        tap(centerOf("guide_next"), "Suivant")
        assertNull("« Suivant » did not turn the page", centerOfText("Bienvenue dans BenStudio"))
        tap(centerOf("guide_skip"), "Passer")
        assertNull("« Passer » did not close it", centerOf("guide"))
    }

    /** c/215-221: ⋮ of a part › « Insérer après » › a part: the window closed, nothing was inserted. */
    @Test
    fun insererApresInsertsThePartPicked() {
        begin()
        tap(centerOfText("Partir de zéro"), "Partir de zéro")
        tap(centerOf("mode_card_simple"), "Simple")
        act("attends 2")
        // Three parts A, B, C in the song, as the tutorial's chapter 10 makes them.
        val vm = rhythm()!!
        vm.toggleHit(0, 0)
        vm.newScene()
        vm.toggleHit(0, 4)
        vm.newScene()
        vm.toggleHit(0, 8)
        while (project()!!.song.size < 3) vm.addSongPart(project()!!.song.size)
        act("attends 1")
        val before = project()!!.song.map { it.scene }
        assertEquals(3, before.size)
        if (centerOf("part_menu_1") == null) tap(centerOfText("morceau", substring = true), "the Mon morceau tab")
        tap(reach("part_menu_1"), "⋮ of part 2")
        tap(centerOf("part_insert_1"), "Insérer après")
        tap(centerOf("pick_scene_2") ?: centerOf("tl_pick_2"), "C")
        assertEquals("nothing inserted", before.take(2) + 2 + before.drop(2), project()!!.song.map { it.scene })
    }

    /**
     * c/115-118: two quick touches on « Créer la piste Basse »: the first opened the Clavier, the
     * second fell on its grid and wrote a stray note (screen-118.png; without the guard, this test
     * shows the same note). As the bench plays them: one after the other, at the same place.
     */
    @Test
    fun aDoubleTapOnCreerLaPisteBasseWritesNoNote() {
        begin()
        tap(centerOfText("J'apprends"), "J'apprends")
        // Simple: the tutorial's Facile level, not asked again.
        tap(centerOf("mode_card_simple"), "Simple")
        tap(centerOfText("Jump up DnB", substring = true), "Jump up")
        act("attends 4")
        // Chapter 5, « La basse », from the tutorial's list of chapters.
        tap(centerOf("tuto_chapters"), "the chapters")
        tap(centerOfText("La basse", substring = true), "La basse")
        act("attends 1")
        repeat(3) { if (centerOf("tuto_need") == null) tap(centerOf("tuto_next"), "Suivant") }
        val need = centerOf("tuto_need")
        assertNotNull("no « Créer la piste Basse »", centerOfText("Créer la piste Basse", substring = true))
        assertNotNull("no « Créer la piste Basse »", need)
        // His two touches, at the button's place.
        act("touche ${need!!.first} ${need.second}")
        act("touche ${need.first} ${need.second}")
        assertEquals("a stray note under the second touch", 0, notes())
        // A touch there later is the user's own: it reaches the grid.
        act("attends 2")
        act("touche ${need.first} ${need.second}")
        assertEquals("a later touch on the grid wrote nothing", 1, notes())
    }

    /**
     * c/19-40 (and Sarah, run3r/e/12-33): the « Drum & bass » pack downloaded, « Créer le morceau »
     * and « Créer tout de suite avec les choix conseillés » did nothing, an error hidden under the
     * step. Its FLAC sounds are real ones (as the phone gets them) but Robolectric has no FLAC
     * decoder: each of them is unreadable on the bench. The song is made all the same, those lines
     * on the built-in sounds, and the new project says so in plain words.
     */
    @Test
    fun anUnreadablePackSoundGivesWayToTheBuiltInOne() {
        val files = installDrumAndBassPack()
        // On the bench, a pack's FLAC cannot be read (no decoder): what Lucas and Sarah hit.
        assertTrue(runCatching { AudioDecoder.decode(app, Uri.fromFile(File(File(PackStore.dir(app), "dnb"), files.first()))) }.isFailure)
        begin()
        wizardOnJumpUp()
        tap(centerOf("wizard_next"), "Suivant")
        tap(centerOf("wizard_create_now"), "Créer tout de suite avec les choix conseillés")
        act("attends 3")
        assertNull("still in the assistant", centerOf("wizard"))
        val p = project()!!
        assertEquals("Jump up DnB", p.name)
        assertEquals(174.0, p.tempoBpm, 0.01)
        // Said on the new project, at once.
        val said = centerOfText("n'a pas pu être lu", substring = true) ?: centerOfText("n'ont pas pu être lus", substring = true)
        assertNotNull("nothing said about the pack", said)
        assertNotNull(centerOfText("Drum & bass", substring = true))
        tap(centerOfText("Compris"), "Compris")
        assertNull(centerOfText("n'ont pas pu être lus", substring = true))
    }

    /** The notes of the project's instrument tracks (the first one's and the added ones'). */
    private fun notes(): Int = project()!!.let { p -> p.synthNotes.notes.size + p.tracks.sumOf { it.notes.notes.size } }

    /** A « Drum & bass » pack (id dnb) as downloaded: its pack.json and real FLAC files. */
    private fun installDrumAndBassPack(): List<String> {
        val dir = File(PackStore.dir(app), "dnb").apply { deleteRecursively(); mkdirs() }
        val kinds = listOf("kick", "snare", "clap", "hat", "perc", "bass")
        val files = kinds.flatMap { k -> (1..2).map { i -> "$k-$i.flac" } }
        val wav = File(app.cacheDir, "pack-src.wav")
        FloatWav.write(wav, Pcm(FloatArray(2 * 4800) { k -> 0.4f * kotlin.math.sin(k * 0.05f) * (1f - k / 9600f) }, 2, 48_000))
        files.forEach { FlacEncoder.encode(wav, File(dir, it)) }
        val sounds = files.joinToString(",") { f ->
            val kind = f.substringBefore('-')
            val name = kind.replaceFirstChar { it.uppercase() } + " " + f.substringAfter('-').substringBefore('.')
            """{"file":"$f","kind":"$kind","name":"$name"}"""
        }
        File(dir, "pack.json").writeText("""{"id":"dnb","name":"Drum & bass","family":"Bass music","bpm":174.0,"sounds":[$sounds],"version":1}""")
        assertEquals(listOf("dnb"), PackStore.installed(app).map { it.id })
        return files
    }
}
