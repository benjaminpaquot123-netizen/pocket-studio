package com.pocketstudio.app.human

import android.content.Intent
import android.media.MediaFormat
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.semantics.getOrNull
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.app.rhythm.stemExports
import com.pocketstudio.app.rhythm.stripSounds
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.StudioDrums
import com.pocketstudio.model.TrackKind
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.tuto.TutoTarget
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowMediaCodec
import java.time.Duration

/**
 * Lot « tuto 5 », P0: the four testers' last gesture, « Partager » on the tutorial's finished song,
 * done as the « testeur humain » bench does it (the bench's engine plugged in at the touch, the time
 * going by in slices of 50 ms), up to Android's share menu. The bench used to freeze there: with its
 * engine plugged in, AppRoot read the native audio stream (a library Robolectric has not, fixed in
 * fc367c0), and « Prêt à partager ? » opened again read the native meters: each threw in the
 * composition, so « Annuler » and the menu never came. Also: « Annuler » and the back gesture stop
 * and close at once, a double touch makes one song, and the bar really moves.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = HumanTester.PHONE)
class BenchShareTest {
    /** PLAN-RESTE 4.5: the state inherited from the test before must be pristine (else: red, naming that test). */
    @get:Rule
    val pristine = BenchPristineRule()
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)
    private val engine = BenchEngine()
    /** Every value the window's bar showed while the song was made. */
    private val seen = ArrayList<Float>()

    @Before
    fun codec() {
        val passThrough = ShadowMediaCodec.CodecConfig.Codec { input, output ->
            val n = minOf(input.remaining(), output.remaining())
            output.put(input.duplicate().also { it.limit(it.position() + n) })
            input.position(input.position() + n)
        }
        ShadowMediaCodec.addEncoder(MediaFormat.MIMETYPE_AUDIO_AAC, ShadowMediaCodec.CodecConfig(16 * 1024, 16 * 1024, passThrough))
        // FileProvider keeps the first test's folders (another application's): each test its own.
        runCatching {
            androidx.core.content.FileProvider::class.java.declaredFields.filter { java.lang.reflect.Modifier.isStatic(it.modifiers) && Map::class.java.isAssignableFrom(it.type) }
                .forEach { f -> f.isAccessible = true; synchronized(f.get(null)) { (f.get(null) as MutableMap<*, *>).clear() } }
        }
    }

    @After
    fun end() {
        NativeEngine.sink = null
        ShadowMediaCodec.clearCodecs()
        h.finish()
        // PLAN-RESTE 4.5: the ViewModel factory kept this test's Application (as HumanTester.end).
        androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory::class.java.getDeclaredField("_instance").apply { isAccessible = true }.set(null, null)
    }

    private fun shown(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    /** As the bench: [ms] go by in slices of 50 ms (the app's clocks and animations), the workers run meanwhile. */
    private fun pass(ms: Long) {
        var left = ms
        while (left > 0) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            runCatching { compose.mainClock.advanceTimeBy(50) }
            Thread.sleep(5)
            if (h.vm.export.value.sharing && h.vm.export.value.running) seen += h.vm.export.value.progress
            left -= 50
        }
        runCatching { compose.waitForIdle() }
    }

    private fun tap(tag: String) {
        val n = compose.onNodeWithTag(tag)
        runCatching { n.performScrollTo() }
        n.performClick()
        pass(900)
    }

    /** The tutorial's whole song (its parts, the jump up kit) open, [simple] or the five spaces. */
    private fun open(simple: Boolean) {
        h.start(simple = simple, build = {
            session.edit { StudioDrums.withDnbKit(TutoTarget.of("jump_up")!!.song.copy(name = "Mon projet 1")) }
        })
        pass(1500)
        if (simple) {
            if (shown("tab_Envoyer")) tap("tab_Envoyer") else { compose.onAllNodesWithText("Envoyer")[0].performClick(); pass(900) }
        } else {
            tap("top_share")
        }
        assertTrue("« Partager » in sight", shown("share_go"))
    }

    /** One share menu Android got: how it sends ([action]: one file, or several at once), the files (by name) and its title. */
    private data class Menu(val action: String?, val files: List<String>, val title: String?)

    /**
     * Android's share menus opened so far. PLAN-RESTE 5.5 (control 6; round 3 F d #56, C7 of the meta-audit): EVERY file
     * the menu sends is read (« Partager les 2 fichiers »: ACTION_SEND_MULTIPLE), one entry per menu, never the first only.
     */
    private fun menusOpened(): List<Menu> = buildList {
        while (true) {
            val next = shadowOf(h.app).nextStartedActivity ?: break
            if (next.action != Intent.ACTION_CHOOSER) continue
            @Suppress("DEPRECATION")
            val send = next.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
            @Suppress("DEPRECATION")
            val uris = send?.getParcelableArrayListExtra<android.net.Uri>(Intent.EXTRA_STREAM)
                ?: listOfNotNull(runCatching { send?.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM) }.getOrNull())
            add(Menu(send?.action, uris.mapNotNull { it.lastPathSegment?.substringAfterLast('/') }, next.getCharSequenceExtra(Intent.EXTRA_TITLE)?.toString()))
        }
    }

    /** The menus opened so far, each one's files joined (« 01-batterie.wav, 02-synthe.wav »). */
    private fun menus(): List<String> = menusOpened().map { it.files.joinToString(", ").ifEmpty { "?" } }

    /** Time goes by until a share menu opens (at most [ms] of real time); a little longer, for a second one. */
    private fun awaitMenus(ms: Long = 30_000): List<Menu> {
        val until = System.currentTimeMillis() + ms
        val got = ArrayList<Menu>()
        while (got.isEmpty()) {
            assertTrue("no share menu: ${h.vm.export.value}", System.currentTimeMillis() < until)
            pass(500)
            got += menusOpened()
        }
        // A little longer: a second menu would come now.
        pass(3000)
        got += menusOpened()
        return got
    }

    private fun awaitMenu(ms: Long = 30_000): List<String> = awaitMenus(ms).map { it.files.joinToString(", ").ifEmpty { "?" } }

    /**
     * A project in Pro whose « Piste par piste » export gives [stems] files: the drums and the synth sound, then one more
     * instrument track with a note each (StemExports: a strip with nothing to play gives no file). In the seat's [lang].
     */
    private fun openWithStems(stems: Int, lang: Lang = Lang.FR) {
        h.start(simple = false, lang = lang, build = {
            // A new install's starter project comes with a beat and an acid line: cleared, the strips that sound are chosen here.
            session.edit { it.copy(drums = com.pocketstudio.model.DrumPattern(), acid = com.pocketstudio.model.AcidPattern()) }
            toggleHit(0, 0)
            selectTrack(0)
            addNote(Note(0, 2 * NoteClip.TICKS_PER_STEP, 60))
            for (i in 0 until stems - 2) {
                addTrackOf(if (i % 2 == 0) TrackKind.Bass else TrackKind.Melody)
                addNote(Note(0, 2 * NoteClip.TICKS_PER_STEP, 48 + 12 * i))
            }
        })
        pass(1500)
        val p = h.vm.project.value
        assertEquals("$stems strips sound: ${stripSounds(p, false)}", stems, stemExports(p, false).size)
    }

    /** Mix › Avancé › « Piste par piste » › Exporter, rendered by the bench's engine, until its files (or its error) are there. */
    private fun exportStems(): com.pocketstudio.app.rhythm.ExportUi {
        tap("tab_Mix")
        tap("export_advanced")
        tap("export_what_Stems")
        NativeEngine.sink = engine
        tap("export")
        val until = System.currentTimeMillis() + 120_000
        while (h.vm.export.value.let { it.running || (it.files.isEmpty() && it.error == null) }) {
            assertTrue("the export never ended: ${h.vm.export.value}", System.currentTimeMillis() < until)
            pass(500)
        }
        return h.vm.export.value
    }

    /**
     * PLAN-RESTE 5.5 (control 6): [n] tracks exported apart leave through ONE menu that sends [n] files (ACTION_SEND_MULTIPLE,
     * [n] EXTRA_STREAM), the files made. Round 3 F d #56 (« Partager les 2 fichiers » sent one): the bench read the first
     * stream only, and its test was green all the same (ci-242 n° 7): this one counts them.
     */
    private fun stemsLeaveTogether(n: Int) {
        openWithStems(n)
        val export = exportStems()
        assertNull("no error", export.error)
        assertEquals("$n files made: ${export.files.map { it.name }}", n, export.files.size)
        tap("share")
        val menus = awaitMenus()
        assertEquals("one menu: $menus", 1, menus.size)
        val menu = menus.single()
        assertEquals("the $n files sent at once: $menu", Intent.ACTION_SEND_MULTIPLE, menu.action)
        assertEquals("$n files → $n EXTRA_STREAM: $menu", n, menu.files.size)
        assertEquals("each file once: $menu", n, menu.files.toSet().size)
        assertEquals("the files made are the files sent", export.files.map { it.name }.toSet(), menu.files.toSet())
    }

    @Test
    fun twoStemsLeaveAsTwoFiles() = stemsLeaveTogether(2)

    @Test
    fun fourStemsLeaveAsFourFiles() = stemsLeaveTogether(4)

    /**
     * Control 6, the sheet's language (round 1 n° 18, C5 of the meta-audit: the bench drew the share menu in French for
     * every seat): on a seat in Japanese, « Prêt à partager ? » and the menu's title the app gives are in Japanese.
     */
    @Test
    fun theShareMenuSpeaksTheSeatsLanguage() {
        h.start(simple = false, lang = Lang.JA, build = {
            session.edit { StudioDrums.withDnbKit(TutoTarget.of("jump_up")!!.song.copy(name = "Mon projet 1")) }
        })
        pass(1500)
        tap("top_share")
        assertTrue("« Partager » in sight", shown("share_go"))
        val words = compose.onNodeWithTag("share_go").fetchSemanticsNode().config
            .getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Text)?.joinToString(" ") { it.text }.orEmpty()
        assertTrue("the sheet's button in the seat's language (ja): « $words »", tr("Partager") in words && "Partager" !in words)
        NativeEngine.sink = engine
        tap("share_go")
        val menus = awaitMenus()
        assertEquals("one menu: $menus", 1, menus.size)
        assertEquals("the menu's title in the seat's language (ja): $menus", tr("Partager mon morceau"), menus.single().title)
        assertNotEquals("never the French title on a Japanese seat", "Partager mon morceau", menus.single().title)
    }

    @Test
    fun theTutoSongGoesOutAsAWavFromTheSheet() {
        open(simple = false)
        tap("share_format_Wav")
        NativeEngine.sink = engine
        tap("share_go")
        val menus = awaitMenu()
        assertEquals(1, menus.size)
        assertTrue(menus.single(), menus.single().endsWith(".wav"))
        assertFalse("the window is gone", shown("share_progress"))
        // The bar moved: between empty and full on the way, never full from the start.
        assertTrue("the bar moved: $seen", seen.any { it > 0f && it < 1f })
        assertTrue("never full at once: $seen", seen.first() < 1f)
    }

    @Test
    fun aDoubleTouchInFacileMakesOneSongAndOneMenu() {
        open(simple = true)
        NativeEngine.sink = engine
        // Lucas: two quick touches on « Partager ».
        compose.onNodeWithTag("share_go").performTouchInput { click(); advanceEventTime(90); click() }
        pass(900)
        assertTrue(shown("share_progress"))
        val menus = awaitMenu()
        assertEquals("one menu: $menus", 1, menus.size)
        assertTrue(menus.single(), menus.single().endsWith(".m4a"))
        assertEquals("one song made", 1, engine.renders)
    }

    @Test
    fun annulerStopsAndClosesAtOnce() {
        open(simple = false)
        engine.renderMs = 30_000
        NativeEngine.sink = engine
        tap("share_go")
        val until = System.currentTimeMillis() + 15_000
        while (h.vm.export.value.progress <= 0f) {
            assertTrue("the render never began", System.currentTimeMillis() < until)
            pass(200)
        }
        assertTrue(shown("share_progress_percent"))
        compose.onNodeWithTag("share_cancel").performClick()
        compose.waitForIdle()
        // Closed on the touch, not when the render notices.
        assertFalse("closed at once", shown("share_progress"))
        assertFalse(h.vm.export.value.sharing)
        val end = System.currentTimeMillis() + 10_000
        while (h.vm.export.value.running) {
            assertTrue("the render never stopped", System.currentTimeMillis() < end)
            pass(200)
        }
        pass(2000)
        assertEquals(emptyList<String>(), menus())
        assertNull(h.vm.export.value.error)
        assertTrue(h.vm.export.value.files.isEmpty())
        // « Partager » works again.
        engine.renderMs = BenchEngine.RENDER_MS
        tap("top_share")
        tap("share_go")
        assertEquals(1, awaitMenu().size)
    }

    @Test
    fun theBackGestureStopsItToo() {
        open(simple = false)
        engine.renderMs = 30_000
        NativeEngine.sink = engine
        tap("share_go")
        assertTrue(shown("share_progress"))
        androidx.test.espresso.Espresso.pressBackUnconditionally()
        compose.waitForIdle()
        assertFalse(shown("share_progress"))
        val end = System.currentTimeMillis() + 10_000
        while (h.vm.export.value.running) {
            assertTrue("the render never stopped", System.currentTimeMillis() < end)
            pass(200)
        }
        assertEquals(emptyList<String>(), menus())
        assertNotNull(h.vm.export.value)
    }
}
