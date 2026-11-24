package com.pocketstudio.bentest

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.pocketstudio.app.journey.ComposeRuleDriver
import com.pocketstudio.app.journey.Journey
import com.pocketstudio.app.journey.JourneyDriver
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.app.rhythm.ProjectOpening
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Project
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * « Scénarios » on the app's real screens (AppRoot, no engine): each gesture finds its button where the
 * robot looks for it, and the oracles read the real project and the real file. What the engine says
 * (playing, the level) is made up here from the ▶ touched; the phone and the emulator say the rest.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
open class ScenariosVraisEcransTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val tmp = TemporaryFolder()
    private val h = AppHarness(compose)

    @After
    fun back() = h.finish()

    /** The real screens, and an engine that plays when a ▶ was touched an odd number of times. */
    private inner class Vrai(private val d: JourneyDriver = ComposeRuleDriver(compose)) : JourneyDriver by d, Sonde, Appli {
        var playing = false
        val touched = ArrayList<String>()

        override fun click(tag: String) {
            touched += tag
            if (tag in setOf("thumb_play", "top_song", "live_play")) playing = !playing
            // A pad of the Live launches its cell; « ■ Tout » stops them.
            if (tag.startsWith("live_pad_")) playing = true
            if (tag == "live_stop_all") playing = false
            d.click(tag)
        }

        override fun lire() = Sonde.Lecture(0, 0.2f, if (playing) 0.5f else 0f, false, true, playing, cpuWorst = 0.3f, gapWorst = 1f)
        override fun ouvrirStudio() = Unit
        override fun export() = Appli.ExportVu(false, emptyList(), null)
        override fun capture(file: File) = false
        override fun demander(question: String, maxMs: Long): Boolean? = null

        override fun projet(): ProjetVu {
            val p = h.vm.project.value
            fun effets(of: Project) = of.mixer.channels.map { ch -> ch.inserts.filter { it.effect != EffectType.NONE }.map { it.effect.name } }
            val disk = runCatching { ProjectOpening.projectStore(h.app).load(h.vm.projectId) }.getOrNull()
            return ProjetVu(
                h.vm.projectId, p.name, p.song.size, effets(p), disk?.let { ProjetVu.Disque(it.name, it.song.size, effets(it), montee = ProjetVu.montee(it)) },
                montee = ProjetVu.montee(p),
            )
        }
    }

    private fun scenarios(v: Vrai) = Scenarios(
        Journey(v), v, v, Carnet(tmp.newFile()), tmp.newFolder(), ecran = { v.tags(Regex(".+")) },
        ecouteMs = 1_000, decrochagesMs = 1_000, sauvegardeMs = 20_000, liveMs = 5_000, liveEcouteMs = 1_000, effetEcouteMs = 1_000,
    )

    @Test
    fun theProjectIsOnTheDiskInMesProjetsAndReopened() {
        h.start(launchChoice = true)
        val v = Vrai()
        val text = scenarios(v).projet()
        assertTrue(text, text.contains("relu sur le disque (3 parties), dans Mes projets, rouvert par « Continuer »"))
    }

    @Test
    fun aBreakGoesInTheSongAndOnTheDisk() {
        h.start()
        val v = Vrai()
        val text = scenarios(v).structure()
        assertTrue(text, text.contains("3 parties, puis 4"))
        assertEquals(4, h.vm.project.value.song.size)
        assertEquals(4, v.projet().disque?.parties)
    }

    @Test
    fun anEqualiserGoesOnAStripAndOnTheDisk() {
        h.start()
        val v = Vrai()
        val before = v.projet().effets
        val text = scenarios(v).effets()
        assertTrue(text, text.contains("égaliseur ajouté à la piste"))
        val after = v.projet()
        assertEquals(before.sumOf { it.size } + 1, after.effets.sumOf { it.size })
        assertEquals(after.effets, after.disque?.effets)
        // Left as a user leaves it: no sheet over the mixer.
        assertEquals(0, v.count("strip_sheet", false))
    }

    @Test
    fun theLiveHasItsPlayAndTheRobotComesBackToTheSong() {
        h.start()
        val v = Vrai()
        val text = scenarios(v).live()
        assertTrue(text, text.startsWith("le Live joue"))
        assertEquals(2, v.touched.count { it == "live_play" })
        assertEquals(1, v.count("tab_Structure", false))
    }

    /** Runs the scenario [id] as the robot does (its reset first, its oracle at the end). */
    private fun jouer(v: Vrai, id: String): String = scenarios(v).etapes().first { it.name == id }.work(Long.MAX_VALUE)

    @Test
    fun aBuildUpAddsItsCurveNotAPartThenUndoTakesItBack() {
        h.start()
        val v = Vrai()
        val parts = v.projet().parties
        val text = jouer(v, "partie_montee")
        assertTrue(text, text.contains("courbe de montée"))
        assertEquals(parts, h.vm.project.value.song.size)
        // « Annuler » took the curve back: the project is as before.
        assertEquals(0, ProjetVu.montee(h.vm.project.value))
    }

    @Test
    fun aPadOfTheLiveIsFoundByItsRealNameThenTheSecondOne() {
        h.start()
        val v = Vrai()
        val first = jouer(v, "pads_live")
        assertTrue(first, first.contains("le son sort"))
        val second = jouer(v, "pad_2")
        assertTrue(second, second.contains("le son sort"))
        assertTrue(v.touched.toString(), v.touched.any { Regex("live_pad_.+_\\d+").matches(it) })
    }
}

/** The same guard on a tablet: what a narrow phone finds, a tablet finds too. */
@Config(qualifiers = "w800dp-h1280dp-hdpi")
class ScenariosVraisEcransTabletteTest : ScenariosVraisEcransTest()
