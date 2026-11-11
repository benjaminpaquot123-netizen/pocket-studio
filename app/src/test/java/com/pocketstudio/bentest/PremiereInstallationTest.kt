package com.pocketstudio.bentest

import android.view.inspector.WindowInspector
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.pocketstudio.app.journey.ComposeRuleDriver
import com.pocketstudio.app.journey.Journey
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.app.onboarding.Intro
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * The essai éclair as Benjamin's phone ran it on 2026-11-11 (docs/tests-telephone/2026-11-11-eclair-1):
 * BenStudio Test just installed, so a first launch (the welcome still to come, the Mode simple, no
 * « Que veux-tu faire ? » yet), and the robot starting while the opening still plays over the studio.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class PremiereInstallationTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)
    private var opened = 0L

    /** The test's thread is the app's main thread; the opening ends 1.5 s after the studio is asked for (6.5 s on the phone). */
    private val here = object : MainThread {
        override fun <T> run(block: () -> T): T = block()
        override fun pause(ms: Long) {
            ComposeRuleDriver(compose).idle(ms)
            if (Intro.playing && opened > 0 && System.currentTimeMillis() - opened > 1_500) Intro.playing = false
        }
    }

    @After
    fun back() {
        Intro.playing = false
        h.finish()
    }

    private val dir by lazy { File(h.app.cacheDir, "premiere-installation").apply { deleteRecursively() } }
    private val carnet by lazy { Carnet(File(dir, "carnet.tsv")) }
    private val driver = InAppDriver(here).also { d -> d.note = { carnet.add(Carnet.NOTE, "robot", it) } }

    /** No engine here: the export's file is made up (the robot's touches on Mix › Exporter are the real ones), nothing plays. */
    private val appli = object : Appli {
        private var exported: File? = null

        override fun ouvrirStudio() {
            opened = System.currentTimeMillis()
        }

        override fun export(): Appli.ExportVu {
            val f = exported ?: File(dir, "essai.m4a").also { it.writeBytes(ByteArray(20_000)); exported = it }
            return Appli.ExportVu(false, listOf(f), null)
        }

        override fun capture(file: File) = Captures.windows(WindowInspector.getGlobalWindowViews(), file)
        override fun demander(question: String, maxMs: Long): Boolean? = null
    }
    private val silent = object : Sonde {
        override fun lire() = Sonde.Lecture(0, 0f, 0f, false, true, playing = false)
    }

    /** As MainActivity at a start: the opening plays, the studio starts underneath, the welcome waits for its end. */
    private fun firstInstall(): EssaiEclair {
        Intro.playing = true
        h.start(firstLaunch = true, launchChoice = true)
        return EssaiEclair(Journey(driver), silent, appli, carnet, dir, ecouteMs = 1_000, ecran = driver::ecran)
    }

    /** The steps named [names] run as on the phone (each with its longest time, short here); what the logbook holds of them. */
    private fun run(essai: EssaiEclair, vararg names: String): List<Carnet.Ligne> {
        Passage(carnet, essai.etapes().filter { it.name in names }).run { driver.deadlineMs = System.currentTimeMillis() + 90_000 }
        driver.deadlineMs = Long.MAX_VALUE
        return carnet.lines()
    }

    private fun text(lines: List<Carnet.Ligne>) = lines.joinToString("\n") { "${it.kind} ${it.step}: ${it.text}" }

    /**
     * The phone's order: the opening (the welcome answered once the opening is over, not the studio
     * seen under it), then the first song's path, from the Mode simple, up to « Créer maintenant »
     * (the song itself needs the engine: on the phone only). It stopped at « launch » on 2026-11-11.
     */
    @Test
    fun aFirstInstallReachesTheFirstSongsCreation() {
        val essai = firstInstall()
        val lines = run(essai, "ouverture")
        assertEquals(text(lines), listOf("premier lancement (« Plus tard »), studio ouvert en Mode simple"), lines.filter { it.kind == Carnet.REUSSI }.map { it.text })
        val walked = runCatching { essai.versCreer("jump_up") }.onFailure { essai.noterEcran("style_jump_up") }
        assertTrue("walked: $walked\n${text(carnet.lines())}", walked.isSuccess && Journey(driver).exists("wizard_create_now"))
        assertFalse(text(carnet.lines()), carnet.lines().any { it.text.startsWith("touché sous") })
    }

    /**
     * The export and the five screens after a first install: the app is in the Mode simple, they ask
     * for Pro first (« nothing tagged tab_Mix », « nothing tagged tab_Structure » on 2026-11-11); then
     * a song's path from Pro too.
     */
    @Test
    fun aFirstInstallExportsAndShowsItsFiveScreens() {
        val essai = firstInstall()
        val lines = run(essai, "ouverture", "export", "ecrans")
        assertEquals(text(lines), listOf("ouverture", "export", "ecrans"), lines.filter { it.kind == Carnet.REUSSI }.map { it.step })
        assertEquals(text(lines), emptyList<String>(), lines.filter { it.kind == Carnet.ECHEC || it.text.startsWith("touché sous") }.map { it.text })
        assertEquals(5, dir.listFiles { f -> f.extension == "jpg" }.orEmpty().size)
        val walked = runCatching { essai.versCreer("house") }.onFailure { essai.noterEcran("style_house") }
        assertTrue("walked: $walked\n${text(carnet.lines())}", walked.isSuccess && Journey(driver).exists("wizard_create_now"))
    }

    /** A step that fails says what was on screen: at most 20 tags, the screens' names first, the top window's before the rest. */
    @Test
    fun aFailedStepSaysWhatWasOnScreen() {
        h.start(firstLaunch = true, launchChoice = true)
        val essai = EssaiEclair(Journey(driver), silent, appli, carnet, dir, ecran = driver::ecran)
        // The welcome is up (no opening here); the question is not asked while no song played.
        val lines = run(essai, "question")
        assertEquals("pas posée : aucun morceau n'a joué", lines.single { it.kind == Carnet.ECHEC }.text)
        val note = lines.single { it.kind == Carnet.NOTE }.text
        assertTrue(note, note.startsWith("à l'écran : welcome, "))
        val tags = note.removePrefix("à l'écran : ").substringBefore(" (+").split(", ")
        assertTrue(note, tags.size in 3..EssaiEclair.TAGS_MAX && "space_bar" in tags && tags.any { it.startsWith("welcome_") })
    }
}
