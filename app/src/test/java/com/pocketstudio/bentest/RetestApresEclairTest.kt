package com.pocketstudio.bentest

import android.app.Application
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.MainActivity
import com.pocketstudio.app.journey.Journey
import com.pocketstudio.app.onboarding.Intro
import com.pocketstudio.app.onboarding.LaunchRule
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import java.io.File
import java.time.Duration

/**
 * Benjamin's v6 run (2026-11-12, HONOR, Android 17): « Lancer l'essai éclair » went fine, then « Retester les
 * corrections » with five steps ticked (Style Techno, Style Hard techno, Style Liquid DnB, Mémoire et images lentes,
 * Écran éteint) failed all five in 15 s each (« nothing tagged launch after 15000 ms », « top_projects is not there
 * once after 15000 ms », « thumb_play is not there once after 15000 ms ») while each note listed the studio's tags.
 *
 * The same app session here: the real studio (MainActivity, rebuilt when a song opens) and BenStudio Test's own
 * screen as a second activity. Whichever is in front is resumed and drawn; the other one is stopped and its window
 * hidden, as Android does (ActivityThread hides a stopped activity's window, and Compose pauses its recomposer on
 * ON_STOP): the robot must neither read nor touch what is not on the screen. Opening the studio
 * ([Appli.ouvrirStudio]) brings it in front, as the phone's startActivity does; a run's end brings the bench back.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h919dp-420dpi")
class RetestApresEclairTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app: Application get() = ApplicationProvider.getApplicationContext()
    private var studio: ActivityScenario<MainActivity>? = null
    private var banc: ActivityController<ComponentActivity>? = null
    private var opened = 0L
    private var bancEtat by androidx.compose.runtime.mutableStateOf(Robot.Etat())

    /** The phone's main thread: each pause lets the frames come one by one and the looper run, its clock moving. */
    private val phone = object : MainThread {
        override fun <T> run(block: () -> T): T = block()
        override fun pause(ms: Long) {
            compose.mainClock.autoAdvance = false
            var left = ms
            while (left > 0) {
                val frame = minOf(FRAME_MS, left)
                runCatching { compose.mainClock.advanceTimeBy(frame) }
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(frame))
                left -= frame
            }
            // Work off the main thread (a project made, a list read) has the real time it needs.
            Thread.sleep(minOf(ms, 5L))
            if (Intro.playing && opened > 0 && System.currentTimeMillis() - opened > 1_500) Intro.playing = false
        }
    }

    private val dir by lazy { File(app.cacheDir, "retest-apres-eclair").apply { deleteRecursively(); mkdirs() } }
    private val driver = InAppDriver(phone)

    /** No engine here: the song « plays » as soon as it is asked to, at a level that is heard. */
    private val sonde = object : Sonde {
        override fun lire() = Sonde.Lecture(0, 0.2f, 0.5f, false, true, playing = true)
    }

    private val reponses = ArrayList<String>()

    private val appli = object : Appli {
        override fun ouvrirStudio() {
            opened = System.currentTimeMillis()
            studioDevant()
        }

        override fun export(): Appli.ExportVu {
            val f = File(dir, "essai.m4a").also { if (!it.exists()) it.writeBytes(ByteArray(20_000)) }
            return Appli.ExportVu(false, listOf(f), null)
        }

        override fun capture(file: File): Boolean {
            file.parentFile?.mkdirs()
            file.writeText("capture")
            return true
        }

        override fun demander(question: String, maxMs: Long): Boolean? = null

        // The banner's question, answered ✅ at once (Banniere's own buttons are tested in OreilleTest).
        override fun poser(question: Question, maxMs: Long): Reponse {
            reponses += question.id
            return Reponse(Verdict.OUI)
        }
    }

    @Before
    fun freshInstall() {
        for (d in listOf("projects", "projects-trash", "stems", "bentest")) File(app.filesDir, d).deleteRecursively()
        val names = File(app.filesDir, "../shared_prefs").listFiles()?.map { it.name.removeSuffix(".xml") }.orEmpty() + PREFS
        names.distinct().forEach { app.getSharedPreferences(it, 0).edit().clear().commit() }
        app.getSharedPreferences("settings", 0).edit().putString("language", "fr").commit()
        SettingsStore.forgetForTests()
        LaunchRule.askAtStartForTest = true
        shadowOf(app).grantPermissions(android.Manifest.permission.RECORD_AUDIO)
        forgetTheViewModelFactory()
        // The robot's watch on the activities, as Robot.start installs it (the banner in the studio's window).
        app.registerActivityLifecycleCallbacks(Tracker())
    }

    @After
    fun end() {
        Intro.playing = false
        compose.mainClock.autoAdvance = true
        runCatching { banc?.pause()?.stop()?.destroy() }
        runCatching { studio?.close() }
        com.pocketstudio.app.rhythm.ClosingSaves.awaitIdleBlocking(15_000)
        for (d in listOf("projects", "projects-trash", "stems", "bentest")) File(app.filesDir, d).deleteRecursively()
        LaunchRule.askAtStartForTest = true
        SettingsStore.forgetForTests()
        forgetTheViewModelFactory()
    }

    private fun forgetTheViewModelFactory() {
        ViewModelProvider.AndroidViewModelFactory::class.java.getDeclaredField("_instance").apply { isAccessible = true }.set(null, null)
    }

    /** What Android does to a window whose activity stops (hidden) or starts again (shown). */
    private fun montrer(activity: android.app.Activity, shown: Boolean) {
        activity.window.decorView.visibility = if (shown) View.VISIBLE else View.INVISIBLE
    }

    /** The studio in front (AppliReelle.ouvrirStudio: startActivity, singleTask): resumed and shown, the bench stopped and hidden. */
    private fun studioDevant() {
        banc?.let { b -> if (b.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) b.pause().stop(); montrer(b.get(), false) }
        val s = studio
        if (s == null) {
            Intro.playing = true
            studio = ActivityScenario.launch(MainActivity::class.java)
        } else {
            s.moveToState(Lifecycle.State.RESUMED)
            s.onActivity { montrer(it, true) }
        }
    }

    /** BenStudio Test's screen in front (its launcher, or Robot.bringBack at a run's end): the studio stopped and hidden. */
    private fun bancDevant(etat: Robot.Etat = Robot.Etat()) {
        studio?.let { s ->
            s.moveToState(Lifecycle.State.CREATED)
            s.onActivity { montrer(it, false) }
        }
        bancEtat = etat
        val b = banc
        if (b == null) {
            banc = Robolectric.buildActivity(ComponentActivity::class.java).setup().also { c ->
                c.get().setContent { PocketStudioTheme { BenTestScreen(bancEtat, emptyList(), BenTestActions()) } }
            }
        } else {
            b.restart().start().resume()
            montrer(b.get(), true)
        }
        phone.pause(200)
    }

    /** What the nodes tagged [tag] say (the assistant's error, for a failed step's note). */
    private fun textes(tag: String): List<String> = runCatching {
        compose.onAllNodes(androidx.compose.ui.test.hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().flatMap { n ->
            n.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.Text) { emptyList() }.map { "$tag=« $it »" }
        }
    }.getOrDefault(emptyList())

    private fun text(lines: List<Carnet.Ligne>) = lines.joinToString("\n") { "${it.kind} ${it.step}: ${it.text}" }

    private fun ends(lines: List<Carnet.Ligne>) = lines.filter { it.kind == Carnet.REUSSI || it.kind == Carnet.ECHEC || it.kind == Carnet.PLANTAGE }

    private fun run(carnet: Carnet, steps: List<Etape>): List<Carnet.Ligne> {
        Passage(carnet, steps).run { e -> driver.deadlineMs = System.currentTimeMillis() + e.maxMs }
        driver.deadlineMs = Long.MAX_VALUE
        return carnet.lines()
    }

    /** Benjamin opens BenStudio Test, « Lancer l'essai éclair » (its opening and its five screens here), the bench back at its end. */
    private fun essaiEclair(): EssaiEclair {
        bancDevant()
        val carnet = Carnet(File(dir, "eclair.tsv"))
        driver.note = { carnet.add(Carnet.NOTE, "robot", it) }
        val essai = EssaiEclair(Journey(driver), sonde, appli, carnet, File(dir, "captures"), ecouteMs = 1_000, ecran = driver::ecran)
        val lines = run(carnet, essai.etapes().filter { it.name in setOf("ouverture", "ecrans") })
        assertEquals(text(lines), listOf("ouverture", "ecrans"), ends(lines).filter { it.kind == Carnet.REUSSI }.map { it.step })
        bancDevant(Robot.Etat(verdict = "2 réussies", passage = Robot.ECLAIR))
        return essai
    }

    /** What the bench shows is never read as the studio, and the studio stopped under it is not read at all. */
    @Test
    fun aStoppedStudioUnderTheBenchIsNotReadByTheRobot() {
        essaiEclair()
        val j = Journey(driver)
        assertTrue("the bench is in front", j.exists("bentest_verdict"))
        assertFalse("the stopped studio's top bar is not there: ${driver.ecran()}", j.exists("top_projects"))
        assertFalse(driver.ecran().toString(), driver.ecran().any { it == "space_bar" || it == "thumb_play" })
    }

    /**
     * The phone's sequence: the essai éclair, then « Retester les corrections » with the five steps ticked. Each step
     * gets past where the phone failed: the studio is in front, Mes projets › « Que veux-tu faire ? » › the assistant
     * › « Créer maintenant » touched. Here (no sample library under Robolectric, only jump up is made: the assistant
     * says « un son n'a pas pu être lu ») the three styles then stop at their minute; the images and the screen-off
     * step (a jump up song) end done.
     */
    @Test
    fun theRetestOfFiveStepsAfterTheEssaiEclairStartsFromTheStudio() {
        essaiEclair()
        val chosen = listOf("style_techno", "style_hard_techno", "style_liquid_dnb", "images", "ecran_eteint")
        val carnet = Carnet(File(dir, "retest.tsv"))
        driver.note = { carnet.add(Carnet.NOTE, "robot", it) }
        var step = ""
        val taps = ArrayList<Pair<String, String>>()
        val j = Journey(driver) { t -> taps += step to t.removePrefix("tap ") }
        val tout = ToutTester(j, sonde, appli, carnet, File(dir, "captures"), ecouteMs = 1_000, ecran = { textes("wizard_error") + driver.ecran() }, attenteEteintMs = 1_000, eteintMs = 1_000)
        val steps = Robot.subset(tout.etapes(), Robot.TOUT, chosen.toSet())
        Passage(carnet, steps).run { e -> step = e.name; driver.deadlineMs = System.currentTimeMillis() + minOf(e.maxMs, STEP_MS) }
        driver.deadlineMs = Long.MAX_VALUE
        val lines = carnet.lines()
        val log = "--- logbook:\n${text(lines.filter { it.kind != Carnet.MESURE })}"
        // The phone's failures, none here.
        val phone = Regex("nothing tagged launch|is not there once|sous une autre fenêtre")
        assertTrue(log, lines.none { phone.containsMatchIn(it.text) })
        assertEquals(log, chosen, lines.filter { it.kind == Carnet.DEBUT }.map { it.step }.filter { it in chosen })
        for (s in chosen.filter { it.startsWith("style_") } + "ecran_eteint") {
            assertTrue("$s: « Créer maintenant » touched\n$log", taps.any { it == s to "wizard_create_now" })
        }
        for (s in listOf("images", "ecran_eteint")) assertTrue(log, ends(lines).any { it.step == s && it.kind == Carnet.REUSSI })
    }

    /** « Questions à l'oreille » started after another run: its opening brings the studio back, then the first question. */
    @Test
    fun theQuestionsAfterTheEssaiEclairStartFromTheStudio() {
        val essai = essaiEclair()
        val carnet = Carnet(File(dir, "oreille.tsv"))
        driver.note = { carnet.add(Carnet.NOTE, "robot", it) }
        val oreille = QuestionsOreille(Pose(essai, Journey(driver)), sonde, appli, carnet, File(dir, "captures"))
        val lines = run(carnet, oreille.etapes().filter { it.name in setOf("oreille_ouverture", "oreille_1_jump_up") })
        assertEquals(text(lines.filter { it.kind != Carnet.MESURE }), listOf("oreille_ouverture", "oreille_1_jump_up"), ends(lines).filter { it.kind == Carnet.REUSSI }.map { it.step })
        assertEquals(listOf("oreille_1_jump_up"), reponses)
    }

    /**
     * No run kept by this version (derniere.tsv is written since v6): the checklist reads the last saved logbook,
     * whatever run and version wrote it, and ticks what failed there. « Ouverture du studio » is not offered: it
     * always runs first, unseen.
     */
    @Test
    fun withNoRunKeptTheChecklistTicksWhatTheLastLogbookFailed() {
        val c = Robot.carnet(app)
        c.add(Carnet.DEBUT, "ouverture"); c.add(Carnet.REUSSI, "ouverture", "studio ouvert")
        c.add(Carnet.DEBUT, "style_house"); c.add(Carnet.REUSSI, "style_house", "ok")
        c.add(Carnet.DEBUT, "style_techno"); c.add(Carnet.ECHEC, "style_techno", "nothing tagged launch after 15000 ms")
        c.add(Carnet.FINI, "")
        File(Robot.dir(app), "passage.txt").writeText(Robot.ECLAIR)
        val candidats = Robot.candidats(app)
        assertFalse("the opening is not offered: ${candidats.map { it.id }}", candidats.any { it.id == "ouverture" })
        assertEquals(listOf("style_techno"), candidats.filter { it.coche }.map { it.id })
        assertEquals(Feu.VERT, candidats.single { it.id == "style_house" }.feu)
    }

    /** Nothing ticked (no run saved at all): the checklist says what to do; once « Retester » is touched it is gone. */
    @Test
    fun theChecklistWithNothingTickedSaysToTickAndClosesBeforeTheRun() {
        var etat by androidx.compose.runtime.mutableStateOf(Robot.Etat())
        var started: Set<String>? = null
        val candidats = Robot.candidats(app)
        assertTrue(candidats.none { it.coche })
        banc = Robolectric.buildActivity(ComponentActivity::class.java).setup().also { c ->
            c.get().setContent {
                PocketStudioTheme {
                    BenTestScreen(etat, emptyList(), BenTestActions(startRetest = { ids -> started = ids; etat = Robot.Etat(running = true, passage = Robot.TOUT, step = "Style Techno") }), candidats)
                }
            }
        }
        val j = Journey(driver)
        j.tap("bentest_retest")
        j.await("bentest_retest_aide")
        j.tap("bentest_retest_style_techno")
        j.tap("bentest_retest_go")
        assertEquals(setOf("style_techno"), started)
        j.await("bentest_step")
        assertTrue("nothing of the checklist is left: ${driver.ecran()}", j.tagsMatching(Regex("bentest_retest.*")).isEmpty())
    }

    companion object {
        const val FRAME_MS = 16L

        /** A step's longest time here (a style the assistant cannot make under Robolectric stops at it). */
        const val STEP_MS = 60_000L
        val PREFS = listOf("nav", "missions", "settings", "next_steps", "song_ready", "share", "tools", "launch", "projects")
    }
}
