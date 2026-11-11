package com.pocketstudio.bentest

import androidx.compose.ui.geometry.Rect
import com.pocketstudio.app.journey.Journey
import com.pocketstudio.app.journey.JourneyDriver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** The essai éclair's listening: 4 readings a second in the logbook, the dropouts counted from the start of the song. */
class EssaiEclairTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private object Waits : JourneyDriver {
        override fun count(tag: String, merged: Boolean) = 0
        override fun bounds(tag: String) = Rect.Zero
        override fun scrollTo(tag: String) = Unit
        override fun click(tag: String) = Unit
        override fun idle(ms: Long) = Thread.sleep(ms)
        override fun tags(pattern: Regex) = emptyList<String>()
    }

    @Test
    fun theSoundIsReadFourTimesASecond() {
        var reads = 0
        val sonde = object : Sonde {
            // Two dropouts before the song (not counted), then one every 4 readings.
            override fun lire() = Sonde.Lecture(xruns = 2 + reads++ / 4, cpu = 0.4f, peak = 0.5f, nonFinite = false, running = true)
        }
        val carnet = Carnet(java.io.File(tmp.root, "c.tsv"))
        val m = EssaiEclair(Journey(Waits), sonde, object : Appli {
            override fun ouvrirStudio() = Unit
            override fun export() = Appli.ExportVu(false, emptyList(), null)
            override fun capture(file: java.io.File) = false
            override fun demander(question: String, maxMs: Long): Boolean? = null
        }, carnet, tmp.root).mesurer("style_x", 2_000)
        assertTrue("${m.reads} readings in 2 s", m.reads in 7..8)
        assertEquals(m.reads, carnet.lines().count { it.kind == Carnet.MESURE })
        assertEquals((m.reads) / 4, m.xruns)
        assertTrue(m.text(), m.loudSeconds >= 1 && m.text().contains("décrochage"))
    }

    /** A screen made of [tags]; a touch is told to [touched]. */
    private class Screen(val tags: Set<String>, val touched: (String) -> Unit) : JourneyDriver {
        override fun count(tag: String, merged: Boolean) = if (tag in tags) 1 else 0
        override fun bounds(tag: String) = Rect(0f, 0f, 10f, 10f)
        override fun scrollTo(tag: String) = Unit
        override fun click(tag: String) = touched(tag)
        override fun idle(ms: Long) = Thread.sleep(ms)
        override fun tags(pattern: Regex) = tags.filter { pattern.matches(it) }
    }

    private object NoAppli : Appli {
        override fun ouvrirStudio() = Unit
        override fun export() = Appli.ExportVu(false, emptyList(), null)
        override fun capture(file: java.io.File) = false
        override fun demander(question: String, maxMs: Long): Boolean? = true
    }

    private fun essai(driver: JourneyDriver, sonde: Sonde) =
        EssaiEclair(Journey(driver), sonde, NoAppli, Carnet(java.io.File(tmp.root, "c.tsv")), tmp.root, ecouteMs = 500, demarrageMs = 400)

    /** ▶ is proven by the engine: the Guide's button that starts nothing is followed by the song's ▶, which does. */
    @Test
    fun playIsTriedUntilTheEngineSaysItPlays() {
        var playing = false
        val touched = ArrayList<String>()
        val screen = Screen(setOf("guide_option_drop", "thumb_play")) { touched += it; if (it == "thumb_play") playing = !playing }
        val sonde = object : Sonde {
            override fun lire() = Sonde.Lecture(0, 0.2f, if (playing) 0.4f else 0f, false, true, playing)
        }
        val e = essai(screen, sonde)
        assertEquals("thumb_play", e.lancerLecture())
        assertEquals(listOf("guide_option_drop", "thumb_play"), touched)
        // Already playing: nothing touched (a second touch on ▶ would stop it).
        assertEquals("déjà en lecture", e.lancerLecture())
        assertEquals(2, touched.size)
    }

    @Test
    fun aPlayThatNeverStartsOrMakesNoSoundFails() {
        val still = object : Sonde {
            override fun lire() = Sonde.Lecture(0, 0.1f, 0f, false, true, playing = false)
        }
        val never = runCatching { essai(Screen(setOf("thumb_play")) {}, still).lancerLecture() }.exceptionOrNull()?.message
        assertEquals("la lecture ne démarre pas (touché : thumb_play ; moteur en marche)", never)
        val mute = object : Sonde {
            override fun lire() = Sonde.Lecture(0, 0.1f, 0f, false, true, playing = true)
        }
        val silence = runCatching { essai(Screen(setOf("thumb_play")) {}, mute).lancerLecture() }.exceptionOrNull()?.message
        assertEquals("lecture lancée (déjà) mais aucun son ne sort : crête 0.00", silence)
    }

    /** The question waits for a song the engine played; a failed step leaves what was on screen in the logbook. */
    @Test
    fun theQuestionIsOnlyAskedOnceASongPlayed() {
        val carnet = Carnet(java.io.File(tmp.root, "q.tsv"))
        val e = EssaiEclair(Journey(Screen(setOf("welcome", "pad_3", "pad_4", "space_bar")) {}), object : Sonde {
            override fun lire() = Sonde.Lecture(0, 0f, 0f, false, true)
        }, NoAppli, carnet, tmp.root)
        Passage(carnet, e.etapes().filter { it.name == "question" }).run()
        assertEquals(listOf("à l'écran : welcome, space_bar, pad_*", "pas posée : aucun morceau n'a joué"), carnet.lines().filter { it.kind != Carnet.DEBUT && it.kind != Carnet.FINI }.map { it.text })
        carnet.clear()
        carnet.add(Carnet.REUSSI, "style_house", "lecture par guide_option_drop : 30 s")
        Passage(carnet, e.etapes().filter { it.name == "question" }).run()
        assertEquals("oui, la musique sortait du téléphone", carnet.lines().last { it.kind == Carnet.REUSSI }.text)
    }
}
