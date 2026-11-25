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
        var partie = -1
        /** The other project opened from « Mes projets » (see [click]); null: the harness's own. */
        var ailleurs: String? = null
        override fun partieJouee(): Int? = partie.takeIf { playing && it >= 0 }
        /** The guided pack: Benjamin's « Fait » plugs a headset in ([branche]), and an unplug may stop the song ([arretAuChangement]). */
        var sortie = Materiel(casqueFilaire = false, bluetooth = false)
        var branche = true
        var arretAuChangement = false
        var eco: Boolean? = true
        val questions = ArrayList<String>()
        override fun materiel() = sortie
        override fun economieEnergie() = eco
        override fun poser(question: Question, maxMs: Long): Reponse {
            questions += question.id
            if (question.id == Epreuves.SORTIE && branche) sortie = Materiel(casqueFilaire = true, bluetooth = false)
            if (question.id == Epreuves.SORTIE && arretAuChangement) playing = false
            return Reponse(Verdict.OUI)
        }
        val touched = ArrayList<String>()
        /**
         * The rows of « Mes projets » come after their screen (the list is read from the disk on another thread):
         * missing from this many readings of the rows. CI 462 lost that race once.
         */
        var lignesEnRetard = 0
        override fun tags(pattern: Regex): List<String> {
            val all = d.tags(pattern)
            val rows = pattern.matches("project_x") && !pattern.matches("projects")
            if (!rows || lignesEnRetard <= 0) return all
            lignesEnRetard--
            return all.filterNot { it.startsWith("project_") }
        }
        /** A hole while the kit sheet is open (the gestures of « Changement de kit »): silence although it plays. */
        var muetKit = false
        @Volatile private var kitOuvert = false

        override fun click(tag: String) {
            touched += tag
            if (tag == Epreuves.DRUMS_NAME) kitOuvert = true
            if (tag.startsWith("kit_choice_") || tag == "kit_sheet_close") kitOuvert = false
            if (tag in setOf("thumb_play", "top_song", "live_play")) playing = !playing
            // A pad of the Live launches its cell; « ■ Tout » stops them.
            if (tag.startsWith("live_pad_")) playing = true
            if (tag == "live_stop_all") playing = false
            // A part's ▶ on the Structure plays the song from it (or queues it while the song plays).
            if (tag.startsWith("part_play_")) { playing = true; partie = tag.removePrefix("part_play_").toInt() }
            // Another project in « Mes projets »: the app rebuilds its activity on it (Activity.recreate), which leaves
            // no screen to this rule under Robolectric. Played here: this one saved, the list closed, the other one
            // read from the disk as the open one; the song stops with the old screens. The list is closed by the open
            // project's own row (the app's « resume », no rebuild). The phone does the real rebuild.
            val id = tag.removePrefix("project_")
            if (tag.startsWith("project_") && id in ProjectOpening.projectStore(h.app).list()) {
                if (id != h.vm.projectId) {
                    kotlinx.coroutines.runBlocking { h.vm.flushNow() }
                    playing = false
                    ailleurs = id
                    d.click("project_${h.vm.projectId}")
                    return
                }
                ailleurs = null
            }
            d.click(tag)
        }

        override fun lire() = Sonde.Lecture(0, 0.2f, if (playing && !(muetKit && kitOuvert)) 0.5f else 0f, false, true, playing, cpuWorst = 0.3f, gapWorst = 1f)
        override fun ouvrirStudio() = Unit

        /** « Another app » takes the sound: a duck lowers it (plays on), a call stops the song as PlaybackService does. */
        val focus = ArrayList<Boolean>()
        override fun prendreSon(duck: Boolean, pendant: () -> Unit): Boolean {
            focus += duck
            if (!duck) playing = false
            pendant()
            return true
        }
        override fun export() = Appli.ExportVu(false, emptyList(), null)
        override fun memoireMo() = 200
        override fun imagesFin() = 100 to 2
        override fun capture(file: File) = false
        override fun demander(question: String, maxMs: Long): Boolean? = null

        override fun projets(): Map<String, ProjetVu.Disque> {
            val store = ProjectOpening.projectStore(h.app)
            return store.list().mapNotNull { id -> store.load(id)?.let { id to ProjetVu.Disque(it.name, it.song.size, emptyList()) } }.toMap()
        }

        override fun projet(): ProjetVu {
            ailleurs?.let { id ->
                val o = ProjectOpening.projectStore(h.app).load(id) ?: error("$id n'est plus sur le disque")
                return ProjetVu(id, o.name, o.song.size, emptyList(), ProjetVu.Disque(o.name, o.song.size, emptyList()))
            }
            val p = h.vm.project.value
            fun effets(of: Project) = of.mixer.channels.map { ch -> ch.inserts.filter { it.effect != EffectType.NONE }.map { it.effect.name } }
            val disk = runCatching { ProjectOpening.projectStore(h.app).load(h.vm.projectId) }.getOrNull()
            return ProjetVu(
                h.vm.projectId, p.name, p.song.size, effets(p), disk?.let { ProjetVu.Disque(it.name, it.song.size, effets(it), montee = ProjetVu.montee(it)) },
                tempo = p.tempoBpm, montee = ProjetVu.montee(p), pads = ProjetVu.pads(p),
            )
        }
    }

    private fun scenarios(v: Vrai, courtMs: Long = 3_000) = Scenarios(
        Journey(v), v, v, Carnet(tmp.newFile()), tmp.newFolder(), ecran = { v.tags(Regex(".+")) }, courtMs = courtMs,
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

    /**
     * Runs the scenario [id] as the robot does (its reset first, its oracle at the end). A red one also writes what the
     * app was doing (its journal, the stacks of its threads) in the test's output, kept in the CI's report: « Mes
     * projets » still loading after 25 s was seen once in some twenty runs, with nothing to read its cause from (CI 462).
     */
    private fun jouer(v: Vrai, id: String, courtMs: Long = 3_000): String = try {
        scenarios(v, courtMs).etapes().first { it.name == id }.work(Long.MAX_VALUE)
    } catch (t: Throwable) {
        println("scenario $id red: ${t.message}")
        println("app journal: " + runCatching { com.pocketstudio.app.report.AppLog.dump(30) }.getOrNull())
        Thread.getAllStackTraces().forEach { (thread, stack) ->
            if (stack.any { it.className.startsWith("com.pocketstudio") || it.className.startsWith("kotlinx.coroutines") }) {
                println("thread ${thread.name} ${thread.state}\n" + stack.take(28).joinToString("\n") { "    at $it" })
            }
        }
        throw t
    }

    /** Chef 2b: the part changed 10 times by its ▶ on the Structure while the song plays; each chosen part is the one played. */
    @Test
    fun thePartIsChangedTenTimesWhileTheSongPlays() {
        h.start()
        val v = Vrai()
        val text = jouer(v, "changer_partie_10", courtMs = 500)
        assertTrue(text, text.startsWith("10 fois"))
        val taps = v.touched.filter { it.startsWith("part_play_") }
        assertEquals(taps.toString(), 10, taps.size)
        assertTrue(taps.toString(), taps.toSet().size >= 2)
        assertTrue(text, !v.playing)
        assertTrue(Scenarios.liste(Scenarios.COMPLET).first { it.id == "changer_partie_10" }.maxMs <= 180_000)
    }

    /** Chef 2c: Mes projets while the song plays, another project opened, then the first one again: intact, silent after ■. */
    @Test
    fun theSongIsLeftWhilePlayingForAnotherProjectThenComesBackIntact() {
        h.start()
        val v = Vrai()
        val first = v.projet()
        val store = ProjectOpening.projectStore(h.app)
        store.save("autre_robot", h.vm.project.value.copy(name = "Autre"))
        val text = jouer(v, "quitter_en_jouant", courtMs = 500)
        assertTrue(text, text.contains("rouvert") && !text.startsWith("non mesuré"))
        assertTrue(v.touched.toString(), "project_autre_robot" in v.touched && "project_${first.id}" in v.touched)
        assertEquals(first.id, h.vm.projectId)
        assertEquals(first.parties, store.load(first.id)?.song?.size)
        assertTrue(text, !v.playing)
        assertTrue(Scenarios.liste(Scenarios.COMPLET).first { it.id == "quitter_en_jouant" }.maxMs <= 180_000)
    }

    /** The rows of « Mes projets » may come after their screen: the robot waits for them as a user does, it does not say « none ». */
    @Test
    fun theOtherProjectIsWaitedForWhenTheRowsComeAfterTheirScreen() {
        h.start()
        val v = Vrai()
        val first = v.projet()
        ProjectOpening.projectStore(h.app).save("autre_robot", h.vm.project.value.copy(name = "Autre"))
        v.lignesEnRetard = 3
        val text = jouer(v, "quitter_en_jouant", courtMs = 500)
        assertTrue(text, text.contains("rouvert") && !text.startsWith("non mesuré"))
        assertEquals(0, v.lignesEnRetard)
        assertTrue(v.touched.toString(), "project_autre_robot" in v.touched && "project_${first.id}" in v.touched)
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

    private fun epreuves(v: Vrai, ecouteMs: Long = 1_000) = Epreuves(
        Journey(v), v, v, Carnet(tmp.newFile()), tmp.newFolder(), ecouteMs = ecouteMs, ecran = { v.tags(Regex(".+")) },
        perteMs = 1_000, nouveauMorceau = {}, enduranceMs = 2_000, allers = 2, imagesMs = 200,
    )

    /** Lot A2, 1: the sound lowered by a notification plays on; taken by a call it stops, then ▶ brings it back with sound. */
    @Test
    fun theSoundTakenByAnotherAppComesBackWithPlay() {
        h.start()
        val v = Vrai()
        val text = epreuves(v).perteSon()
        assertEquals(listOf(true, false), v.focus)
        assertTrue(text, text.contains("la lecture s'est arrêtée, ▶ l'a relancée"))
        assertTrue(text, v.playing.not())
    }

    /** Lot A2, 2: two kits put on the drums from their name while the song plays, the song never stopped. */
    @Test
    fun twoKitsArePutOnTheDrumsWhileTheSongPlays() {
        h.start()
        val v = Vrai()
        val before = h.vm.project.value
        val text = epreuves(v, ecouteMs = 10_000).changementKit()
        assertTrue(text, text.startsWith("kits "))
        assertEquals(2, v.touched.count { it.startsWith("kit_choice_") })
        assertEquals(2, v.touched.count { it == Epreuves.DRUMS_NAME })
        assertEquals(0, v.count("kit_sheet", false))
        assertTrue(text, before != h.vm.project.value)
    }

    /** Fresh review, grave 2: a hole during the kit gestures themselves is heard (the listening's reads wait for them). */
    @Test
    fun aHoleWhileTheKitIsChangedIsRed() {
        h.start()
        val v = Vrai().apply { muetKit = true }
        val err = runCatching { epreuves(v, ecouteMs = 10_000).changementKit() }.exceptionOrNull()
        assertTrue(err.toString(), err?.message?.contains("muet") == true)
    }

    /** Lot A2, 3: the long listening plays on and reads the memory at both ends. */
    @Test
    fun theEnduranceListensThroughAndReadsTheMemory() {
        h.start()
        val v = Vrai()
        val text = epreuves(v).endurance()
        assertTrue(text, text.startsWith("2 s de lecture") && text.contains("mémoire 200 Mo puis 200 Mo"))
        assertTrue(text, !v.playing)
    }

    /** Lot A2, 3: Mes projets then « Continuer », again and again, comes back to the same song. */
    @Test
    fun theProjectIsOpenedAndClosedAgainAndAgain() {
        h.start()
        val v = Vrai()
        val id = h.vm.projectId
        val text = epreuves(v).fuiteMemoire()
        assertTrue(text, text.startsWith("2 allers-retours"))
        assertEquals(2, v.touched.count { it == "top_projects" })
        assertEquals(id, h.vm.projectId)
    }

    /** Lot A2, 3: no restart here: « non mesuré », and the tempo raised for the trial is put back. */
    @Test
    fun theHardStopPutsTheTempoBackWhenItCannotRestart() {
        h.start()
        val v = Vrai()
        val tempo = h.vm.project.value.tempoBpm
        val text = epreuves(v).arretBrutal()
        assertTrue(text, text.startsWith("non mesuré"))
        assertEquals(1, v.touched.count { it == "tempo_plus" })
        assertEquals(tempo, h.vm.project.value.tempoBpm, 0.001)
        assertEquals(0, v.count("tempo_sheet", false))
        assertTrue(epreuves(v).demarrageFroid().startsWith("non mesuré"))
    }

    /** Lot A2, 3: « Très sûr » chosen in Réglages for one listening, then « Rapide » back. */
    @Test
    fun theLargeBufferIsListenedThenTheNormalOneComesBack() {
        h.start()
        val v = Vrai()
        val text = epreuves(v).tamponLarge()
        assertTrue(text, text.startsWith("normal : ") && text.contains("très sûr : "))
        assertEquals(listOf("buffer_6", "buffer_2"), v.touched.filter { it.startsWith("buffer_") })
        assertEquals(2, com.pocketstudio.app.settings.SettingsStore.state.value.bufferBursts)
    }

    /** Lot A2, 3: Réglages (the témoin) then each screen, their frames counted, the song stopped after. */
    @Test
    fun eachScreenIsCountedNextToTheStillOne() {
        h.start()
        val v = Vrai()
        val text = epreuves(v).imagesTemoin()
        assertTrue(text, text.contains("reglages 2 %") && EssaiEclair.SCREENS.all { text.contains("${it.first} 2 %") })
        assertTrue(text, !v.playing)
        assertTrue(v.count("space_bar", false) + v.count("space_rail", false) > 0)
    }

    /** Lot A2, 4: a headset plugged in while it plays, the sound heard again at once; never in « Tout tester ». */
    @Test
    fun theSoundIsBackAfterTheOutputChanged() {
        h.start()
        val v = Vrai()
        val text = epreuves(v).sortieChange()
        assertTrue(text, text.startsWith("son revenu en "))
        assertEquals(listOf(Epreuves.SORTIE), v.questions)
        assertTrue(text, !v.playing)
        val night = ToutTester(Journey(v), v, v, Carnet(tmp.newFile()), tmp.newFolder()).etapes().map { it.name }
        assertEquals(listOf(Epreuves.SORTIE, Epreuves.ECO), epreuves(v).guides().map { it.name })
        assertTrue(night.none { it == Epreuves.SORTIE || it == Epreuves.ECO })
    }

    /** Lot A2, 4: an unplugged headset stops the song (Android's « noisy »): ▶ brings the sound back; no change seen: « non mesuré ». */
    @Test
    fun anUnpluggedHeadsetStopsThenPlayBringsTheSoundBack() {
        h.start()
        val v = Vrai().apply { arretAuChangement = true }
        val text = epreuves(v).sortieChange()
        assertTrue(text, text.startsWith("la lecture s'est arrêtée, ▶ : son revenu en "))
        val same = Vrai().apply { branche = false }
        assertTrue(epreuves(same).sortieChange().startsWith("non mesuré"))
        assertTrue(!same.playing)
    }

    /** Lot A2, 4: the battery saver read on, the song plays through, then Benjamin is asked to turn it off; off: « non mesuré ». */
    @Test
    fun theBatterySaverIsReadOnThenTheSongPlaysThrough() {
        h.start()
        val v = Vrai()
        val text = epreuves(v).economie()
        assertTrue(text, text.startsWith("économie d'énergie allumée"))
        assertEquals(listOf(Epreuves.ECO, "${Epreuves.ECO}_fin"), v.questions)
        assertTrue(!v.playing)
        val off = Vrai().apply { eco = false }
        assertTrue(epreuves(off).economie().startsWith("non mesuré"))
        assertEquals(listOf(Epreuves.ECO, "${Epreuves.ECO}_fin"), off.questions)
    }
}

/** The same guard on a tablet: what a narrow phone finds, a tablet finds too. */
@Config(qualifiers = "w800dp-h1280dp-hdpi")
class ScenariosVraisEcransTabletteTest : ScenariosVraisEcransTest()
