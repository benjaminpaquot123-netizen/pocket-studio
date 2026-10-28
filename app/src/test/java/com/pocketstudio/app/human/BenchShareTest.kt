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
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.StudioDrums
import com.pocketstudio.model.tuto.TutoTarget
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    /** Android's share menus opened so far (the files they send). */
    private fun menus(): List<String> = buildList {
        while (true) {
            val next = shadowOf(h.app).nextStartedActivity ?: break
            if (next.action != Intent.ACTION_CHOOSER) continue
            @Suppress("DEPRECATION")
            val send = next.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
            @Suppress("DEPRECATION")
            val uri = send?.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)
                ?: send?.getParcelableArrayListExtra<android.net.Uri>(Intent.EXTRA_STREAM)?.firstOrNull()
            add(uri?.lastPathSegment ?: "?")
        }
    }

    /** Time goes by until a share menu opens (at most [ms] of real time). */
    private fun awaitMenu(ms: Long = 30_000): List<String> {
        val until = System.currentTimeMillis() + ms
        val got = ArrayList<String>()
        while (got.isEmpty()) {
            assertTrue("no share menu: ${h.vm.export.value}", System.currentTimeMillis() < until)
            pass(500)
            got += menus()
        }
        // A little longer: a second menu would come now.
        pass(3000)
        got += menus()
        return got
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
