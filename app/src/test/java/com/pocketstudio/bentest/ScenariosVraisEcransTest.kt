package com.pocketstudio.bentest

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performSemanticsAction
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
            fun disque(d: Project) = ProjetVu.Disque(
                d.name, d.song.size, effets(d), d.tempoBpm, d.mixer.channels.map { it.mute }, d.mixer.channels.map { it.solo },
                d.mixer.channels.map { it.volume }, d.mixer.channels.map { it.pan }, ProjetVu.montee(d), ProjetVu.reglages(d),
            )
            return ProjetVu(
                h.vm.projectId, p.name, p.song.size, effets(p), disk?.let(::disque),
                tempo = p.tempoBpm, muets = p.mixer.channels.map { it.mute }, solos = p.mixer.channels.map { it.solo },
                volumes = p.mixer.channels.map { it.volume }, pans = p.mixer.channels.map { it.pan },
                montee = ProjetVu.montee(p), pads = ProjetVu.pads(p), reglages = ProjetVu.reglages(p),
            )
        }

        /** The slider [tag] set by its own accessibility action, as the phone's driver does ([InAppDriver.regler]). */
        fun regler(tag: String, value: Float): Boolean {
            val nodes = compose.onAllNodesWithTag(tag).fetchSemanticsNodes()
            if (nodes.none { SemanticsActions.SetProgress in it.config }) return false
            compose.onAllNodesWithTag(tag)[nodes.indexOfLast { SemanticsActions.SetProgress in it.config }]
                .performSemanticsAction(SemanticsActions.SetProgress) { it(value) }
            compose.waitForIdle()
            return true
        }
    }

    private fun scenarios(v: Vrai, ids: Set<String> = emptySet()) = Scenarios(
        Journey(v), v, v, Carnet(tmp.newFile()), tmp.newFolder(), ecran = { v.tags(Regex(".+")) }, regler = v::regler,
        paquet = if (ids.isEmpty()) Scenarios.COMPLET else Scenarios.RATES, rates = ids,
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
    fun aBreakGoesInTheSongAndOnTheDiskThenUndoTakesItBack() {
        h.start()
        val v = Vrai()
        val text = scenarios(v).structure()
        assertTrue(text, text.contains("3 parties, puis 4"))
        assertTrue(text, text.contains("puis 3 après « Annuler »"))
        // The scenario tidies: the next one finds the song as this one did (audit E).
        assertEquals(3, h.vm.project.value.song.size)
        assertEquals(3, v.projet().disque?.parties)
    }

    @Test
    fun anEqualiserGoesOnAStripAndOnTheDisk() {
        h.start()
        val v = Vrai()
        val before = v.projet().effets
        val text = scenarios(v).effets()
        assertTrue(text, text.contains("égaliseur ajouté à la piste"))
        val after = v.projet()
        // Added, heard, then taken off by « Annuler »: the strips as they were, on the disk too (C10).
        assertTrue(text, text.contains("retiré par « Annuler »"))
        assertEquals(before, after.effets)
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
    private fun jouer(v: Vrai, id: String): String = scenarios(v, setOf(id)).etapes().first { it.name == id }.work(Long.MAX_VALUE)

    /** Plays [ids] of the catalogue one after the other: each must pass, and leave the song as it found it. */
    private fun catalogue(vararg ids: String): Vrai {
        h.start()
        val v = Vrai()
        val before = v.projet().trace()
        for (id in ids) {
            val text = jouer(v, id)
            assertTrue("$id : $text", !text.startsWith("non mesuré") && !text.startsWith("échoué"))
            assertEquals(id, before, v.projet().trace())
        }
        return v
    }

    @Test
    fun anEffectIsTakenOffByItsCrossOnTheRealStrip() {
        val v = catalogue("effet_retrait_eq", "effet_retrait_chorus")
        assertTrue(v.touched.toString(), v.touched.any { it.startsWith("fx_remove_") })
    }

    @Test
    fun anEffectsKnobIsTurnedToItsEndsOnTheRealEditor() {
        catalogue("effet_reglage_eq_max", "effet_reglage_compressor_min")
    }

    @Test
    fun eachTabIsFoundAgainAfterARotation() {
        catalogue("onglet_mix_rotation", "onglet_live_rotation", "onglet_projets_rotation")
    }

    @Test
    fun aPartsGestureIsRedoneByTheRealRedo() {
        val v = catalogue("partie_redo_insert", "partie_redo_duplicate")
        assertTrue(v.touched.toString(), v.touched.any { it in Scenarios.REDO })
    }

    @Test
    fun theTempoIsTypedToItsLimitsOnTheRealKeypad() {
        val v = catalogue("limite_tempo_max", "limite_tempo_min")
        assertTrue(v.touched.toString(), v.touched.contains("tempo_key_ok"))
    }

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

    @Test
    fun theEffectsScenariosLeaveTheStripsAsTheyFoundThem() {
        h.start()
        val v = Vrai()
        val before = v.projet().effets
        for (id in listOf("effets", "effets_plusieurs", "effet_chorus")) {
            val text = jouer(v, id)
            assertTrue("$id : $text", !text.startsWith("non mesuré"))
            // Each scenario took its effects off again: the next one finds the same room.
            assertEquals(id, before, v.projet().effets)
            assertEquals(id, before, v.projet().disque?.effets)
        }
    }
}

/** The same guard on a tablet: what a narrow phone finds, a tablet finds too. */
@Config(qualifiers = "w800dp-h1280dp-hdpi")
class ScenariosVraisEcransTabletteTest : ScenariosVraisEcransTest()
