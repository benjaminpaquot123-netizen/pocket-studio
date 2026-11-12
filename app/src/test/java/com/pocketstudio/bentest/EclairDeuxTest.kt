package com.pocketstudio.bentest

import android.os.Looper
import android.view.inspector.WindowInspector
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

/**
 * Benjamin's second essai éclair (2026-11-12, docs/tests-telephone/2026-11-12-eclair-2): his HONOR
 * (1080 x 2412 px at 420 dpi, so 411 x 919 dp, default font), where « Plus tard » is below the
 * welcome's fold and a style's « Choisir » only shows once its card was touched.
 *
 * The phone's time, not the test rule's: here a pause moves the app's clock frame by frame while
 * the robot looks, as on the phone, so an animation (a list scrolled to its target) takes the time
 * it takes there. Under the rule's own clock each animation ends at once and the robot never meets
 * a list still moving: the path that worked here failed on the phone.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h919dp-420dpi")
class EclairDeuxTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)
    private var opened = 0L

    /** The phone's main thread: each pause lets the frames come one by one (16 ms each) and the looper run. */
    private val phone = object : MainThread {
        override fun <T> run(block: () -> T): T = block()
        override fun pause(ms: Long) {
            compose.mainClock.autoAdvance = false
            var left = ms
            while (left > 0) {
                val frame = minOf(FRAME_MS, left)
                compose.mainClock.advanceTimeBy(frame)
                shadowOf(Looper.getMainLooper()).idle()
                left -= frame
            }
            // Work off the main thread (a project made, a list read) has the real time it needs.
            Thread.sleep(minOf(ms, 5L))
            if (Intro.playing && opened > 0 && System.currentTimeMillis() - opened > 1_500) Intro.playing = false
        }
    }

    @After
    fun back() {
        Intro.playing = false
        compose.mainClock.autoAdvance = true
        h.finish()
    }

    private val dir by lazy { File(h.app.cacheDir, "eclair-2").apply { deleteRecursively() } }
    private val carnet by lazy { Carnet(File(dir, "carnet.tsv")) }
    private val driver = InAppDriver(phone).also { d -> d.note = { carnet.add(Carnet.NOTE, "robot", it) } }

    /** No engine here: the export's file is made up once Exporter was really touched; nothing plays. */
    private val appli = object : Appli {
        override fun ouvrirStudio() {
            opened = System.currentTimeMillis()
        }

        override fun export(): Appli.ExportVu {
            val f = File(dir, "essai.m4a").also { if (!it.exists()) it.writeBytes(ByteArray(20_000)) }
            return Appli.ExportVu(false, listOf(f), null)
        }

        // Not drawn: under the frame-by-frame clock Robolectric fails to draw some vectors (no bitmap); the path is what is tested.
        override fun capture(file: File): Boolean {
            file.parentFile?.mkdirs()
            file.writeText(WindowInspector.getGlobalWindowViews().size.toString())
            return true
        }
        override fun demander(question: String, maxMs: Long): Boolean? = null
    }
    private val silent = object : Sonde {
        override fun lire() = Sonde.Lecture(0, 0f, 0f, false, true, playing = false)
    }

    private fun essai() = EssaiEclair(Journey(driver), silent, appli, carnet, dir, ecouteMs = 1_000, ecran = driver::ecran)

    private fun run(essai: EssaiEclair, vararg names: String): List<Carnet.Ligne> {
        Passage(carnet, essai.etapes().filter { it.name in names }).run { driver.deadlineMs = System.currentTimeMillis() + 90_000 }
        driver.deadlineMs = Long.MAX_VALUE
        return carnet.lines()
    }

    private fun text(lines: List<Carnet.Ligne>) = lines.joinToString("\n") { "${it.kind} ${it.step}: ${it.text}" }

    /** Is [tag] seen whole on the screen (not clipped by its list)? */
    private fun whole(tag: String): Boolean {
        val n = compose.onAllNodes(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNodes().single()
        return n.boundsInWindow.height >= n.size.height - 1f && n.boundsInWindow.height > 0f
    }

    /** « welcome still there after 15000 ms » (the first three steps): « Plus tard » is below the fold, the touch must reach it. */
    @Test
    fun plusTardBelowTheFoldClosesTheWelcome() {
        Intro.playing = true
        h.start(firstLaunch = true, launchChoice = true)
        val essai = essai()
        // The opening ends, the welcome comes (as on the phone before the robot looks).
        opened = System.currentTimeMillis()
        val end = System.currentTimeMillis() + 20_000
        while (!Journey(driver).exists("welcome") && System.currentTimeMillis() < end) phone.pause(100)
        phone.pause(1_000)
        assertTrue("the welcome is up", Journey(driver).exists("welcome"))
        assertFalse("« Plus tard » is below the fold on the HONOR", whole("welcome_later"))
        val lines = run(essai, "ouverture")
        assertEquals(text(lines), listOf("ouverture"), lines.filter { it.kind == Carnet.REUSSI }.map { it.step })
        assertFalse(Journey(driver).exists("welcome"))
    }

    /** « nothing tagged wizard_style_choose_techno »: a touch on the card opens it, then « Choisir ce style ». */
    @Test
    fun aStyleIsChosenFromItsCard() {
        h.start(launchChoice = true)
        val essai = essai()
        val walked = runCatching { essai.versCreer("techno") }.onFailure { essai.noterEcran("style_techno") }
        assertTrue("walked: $walked\n${text(carnet.lines())}", walked.isSuccess && Journey(driver).exists("wizard_create_now"))
        assertFalse(text(carnet.lines()), carnet.lines().any { it.text.startsWith("touché sous") })
    }

    /**
     * A style that failed leaves the assistant open over the studio: the export and the five screens
     * close it first, and never touch what it covers (« touché sous une autre fenêtre » on the phone,
     * then 300 s of export and five pictures of the assistant).
     */
    @Test
    fun theStepsAfterAFailedStyleCloseTheAssistantFirst() {
        h.start(launchChoice = true)
        val essai = essai()
        val j = Journey(driver)
        j.newSongInAssistant(simple = false)
        j.await("wizard")
        val lines = run(essai, "export", "ecrans")
        assertEquals(text(lines), listOf("export", "ecrans"), lines.filter { it.kind == Carnet.REUSSI }.map { it.step })
        assertFalse(text(lines), lines.any { it.text.startsWith("touché sous") })
        assertFalse(j.exists("wizard"))
    }

    companion object {
        const val FRAME_MS = 16L
    }
}
