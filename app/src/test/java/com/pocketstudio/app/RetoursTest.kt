package com.pocketstudio.app

import android.content.Context
import android.media.MediaCodec
import android.media.MediaFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.pocketstudio.app.export.video.MuxSink
import com.pocketstudio.app.retours.RecState
import com.pocketstudio.app.retours.RecordSize
import com.pocketstudio.app.retours.Retours
import com.pocketstudio.app.retours.RetoursExportPanel
import com.pocketstudio.app.retours.RetoursHelpCard
import com.pocketstudio.app.retours.RetoursHost
import com.pocketstudio.app.retours.SegmentMuxer
import com.pocketstudio.app.retours.SegmentNames
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.Palettes
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.nio.ByteBuffer
import java.util.zip.ZipFile

/** « Mode retours »: the bubble (on by default), notes with their context, the event log, the files to send, the 20 MB segments. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class RetoursTest {
    @get:Rule
    val compose = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private var now = 1_000_000_000L

    @Before
    fun fresh() {
        Retours.resetForTests()
        File(context.filesDir, Retours.DIR).deleteRecursively()
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().clear().commit()
        SettingsStore.forgetForTests()
        SettingsStore.load(context)
        Retours.clock = { now }
        Retours.wall = { 1_793_000_000_000L + now / 1_000_000L }
        Retours.attach(context)
        Retours.flush()
    }

    @After
    fun back() {
        Retours.resetForTests()
        Retours.clock = System::nanoTime
        Retours.wall = System::currentTimeMillis
        File(context.filesDir, Retours.DIR).deleteRecursively()
    }

    /** The app as MainActivity lays it out: the host over a stand-in screen with a tagged button. */
    private fun app(extra: @Composable () -> Unit = {}) {
        compose.setContent {
            val settings by SettingsStore.state.collectAsState()
            PocketStudioTheme(Palettes.Studio) {
                RetoursHost(show = settings.retoursMode) {
                    Column(Modifier.fillMaxSize().background(Neon.Background).padding(16.dp)) {
                        Text("Rythme")
                        Button(onClick = {}, modifier = Modifier.testTag("play")) { Text("Lecture") }
                        extra()
                    }
                }
            }
        }
    }

    @Test
    fun theBubbleShowsByDefaultAndTheSwitchHidesIt() {
        assertTrue("on by default in this build", SettingsStore.state.value.retoursMode)
        app { RetoursHelpCard() }
        compose.onNodeWithTag("retours_bubble").assertExists()
        compose.onNodeWithTag("retours_rec").assertExists()
        compose.onNodeWithTag("retours_note").assertExists()
        compose.onNodeWithTag("help_retours_switch").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("retours_bubble").assertDoesNotExist()
        // Kept: the next start reads it off.
        SettingsStore.forgetForTests()
        assertFalse(SettingsStore.load(context).retoursMode)
        assertFalse("nothing logged while off", Retours.active)
    }

    @Test
    fun aNoteKeepsTheScreenTheTutorialStepAndAScreenshot() {
        app()
        Retours.screen("Rythme")
        Retours.tutoAt("chapitre 2/9 « Le rythme » · étape 3/5 (kick)")
        compose.onNodeWithTag("retours_note").performClick()
        compose.onNodeWithTag("retours_note_sheet").assertExists()
        compose.onNodeWithTag("retours_note_text").performTextInput("Le bouton Lecture ne répond pas")
        compose.onNodeWithTag("retours_note_save").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("retours_note_sheet").assertDoesNotExist()
        Retours.flush()
        val note = Retours.notes.value.single()
        assertEquals("Le bouton Lecture ne répond pas", note.text)
        assertEquals("Rythme", note.screen)
        assertTrue(note.tuto.startsWith("chapitre 2/9"))
        assertNotNull("a screenshot", note.screenshot)
        assertTrue(File(context.filesDir, "retours/screens/${note.screenshot}").length() > 0)
        assertTrue(File(context.filesDir, "retours/notes.jsonl").readText().contains("ne répond pas"))
        // Read back at the next start.
        Retours.resetForTests()
        Retours.attach(context)
        Retours.flush()
        assertEquals("ne répond pas", Retours.notes.value.single().text.substringAfter("Lecture "))
    }

    @Test
    fun eventsAreTimedInTheirVideoSegmentAndTapsSayWhatWasTouched() {
        Retours.screen("Morceau")
        Retours.segmentStarted(2, now)
        now += 1_500_000_000L
        Retours.event("tuto fait", "kick")
        val e = Retours.recentEvents().last()
        assertEquals(2, e.segment)
        assertEquals(1500L, e.inSegmentMs)
        assertEquals("tuto fait", e.kind)
        assertTrue(Retours.recentEvents().any { it.kind == "écran" && it.detail == "Morceau" })
        app()
        compose.onNodeWithTag("play").performClick()
        compose.waitForIdle()
        val tap = Retours.recentEvents().last { it.kind == "toucher" }
        assertTrue(tap.detail, tap.detail.contains("[play]") && tap.detail.contains("Lecture"))
        // The mode off: nothing more.
        Retours.active = false
        val count = Retours.recentEvents().size
        Retours.event("toucher", "x")
        assertEquals(count, Retours.recentEvents().size)
        Retours.flush()
        assertTrue(File(context.filesDir, "retours/events.jsonl").readText().contains("\"inSegmentMs\":1500"))
    }

    @Test
    fun theExportListsEachVideoAndOneNotesZipThenDeletes() {
        val videos = File(context.filesDir, "retours/videos")
        File(videos, SegmentNames.name("20261028-101500", 1)).writeBytes(ByteArray(3000))
        File(videos, SegmentNames.name("20261028-101500", 2)).writeBytes(ByteArray(2000))
        val current = File(videos, SegmentNames.name("20261028-101500", 3)).apply { writeBytes(ByteArray(10)) }
        Retours.recordingFile = current  // still being written: not offered
        Retours.screen("Rythme")
        Retours.addNote("Trop de boutons", android.graphics.Bitmap.createBitmap(1080, 2400, android.graphics.Bitmap.Config.ARGB_8888))
        val files = Retours.exportFiles()
        assertEquals(listOf("BenStudio-retours-20261028-101500-1.mp4", "BenStudio-retours-20261028-101500-2.mp4", "notes.zip"), files.map { it.name })
        ZipFile(files.last()).use { z ->
            val md = z.getInputStream(z.getEntry("notes.md")).readBytes().decodeToString()
            assertTrue(md, md.contains("## Note 1") && md.contains("Trop de boutons") && md.contains("Écran : Rythme"))
            assertTrue(z.getInputStream(z.getEntry("events.json")).readBytes().decodeToString().contains("\"kind\": \"note\""))
            val shot = android.graphics.BitmapFactory.decodeStream(z.getInputStream(z.getEntry("screenshots/note-1.png")))
            assertEquals("downscaled", 720, shot.height)
        }
        Retours.recordingFile = null
        compose.setContent { PocketStudioTheme(Palettes.Studio) { RetoursExportPanel(close = {}) } }
        shown("retours_file_2")
        compose.onNodeWithTag("retours_delete").performClick()
        compose.onNodeWithTag("retours_delete").performClick()
        shown("retours_export_empty")
        assertTrue(Retours.videos().isEmpty() && Retours.notes.value.isEmpty())
    }

    /** Waits for [tag] (the list is built off the main thread). */
    private fun shown(tag: String) {
        val until = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < until) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            compose.waitForIdle()
            if (compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()) return
            Thread.sleep(20)
        }
        compose.onNodeWithTag(tag).assertExists()
    }

    /** A fake MP4 writer: what each file got. */
    private class FakeSink(val n: Int) : MuxSink {
        val writes = mutableListOf<Triple<Int, Long, Int>>()  // track, time, flags
        var bytes = 0L
        var tracks = 0
        var stopped = false
        override fun addTrack(format: MediaFormat) = tracks++
        override fun start() = Unit
        override fun write(track: Int, data: ByteBuffer, info: MediaCodec.BufferInfo) {
            writes += Triple(track, info.presentationTimeUs, info.flags)
            bytes += info.size
        }
        override fun stop() { stopped = true }
        override fun release() = Unit
    }

    @Test
    fun segmentsStayUnder20MbAndEachStartsOnAKeyPicture() {
        val sinks = mutableListOf<FakeSink>()
        var keyAsked = false
        val started = mutableListOf<Int>()
        val m = SegmentMuxer(open = { n -> FakeSink(n).also { sinks += it } }, requestKeyFrame = { keyAsked = true }, onSegment = { n, _ -> started += n })
        val data = ByteBuffer.allocate(64)
        fun info(size: Int, us: Long, flags: Int) = MediaCodec.BufferInfo().apply { set(0, size, us, flags) }
        // Sound before the first file: dropped.
        m.writeAudio(data, info(400, 0, 0))
        m.videoFormat(MediaFormat.createVideoFormat("video/avc", 576, 1280))
        m.audioFormat(MediaFormat.createAudioFormat("audio/mp4a-latm", 48_000, 2))
        val start = 5_000_000L
        // 150 s at 30 images/s, ~2.9 Mbit/s, a key picture each 2 s or when asked.
        for (i in 0 until 4500) {
            val us = start + i * 33_333L
            val key = i % 60 == 0 || keyAsked
            if (key) keyAsked = false
            m.writeVideo(data, info(if (key) 60_000 else 11_000, us, if (key) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0))
            if (i % 2 == 0) m.writeAudio(data, info(380, us + 5_000, 0))
        }
        m.finish()
        assertTrue("several files: ${sinks.size}", sinks.size >= 3)
        assertEquals((1..sinks.size).toList(), started)
        sinks.forEach { s ->
            assertTrue("file ${s.n}: ${s.bytes} bytes", s.bytes <= SegmentMuxer.MAX_BYTES - SegmentMuxer.INDEX_ROOM)
            val first = s.writes.first()
            assertEquals("file ${s.n} starts with a picture", 0, first.first)
            assertEquals(0L, first.second)
            assertTrue(first.third and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0)
            assertTrue("sound too", s.writes.any { it.first == 1 })
            assertTrue(s.writes.all { it.second >= 0 })
            assertTrue(s.stopped)
        }
        // Nothing lost but the sound before the first picture: every picture is in a file.
        assertEquals(4500, sinks.sumOf { s -> s.writes.count { it.first == 0 } })
        assertEquals("BenStudio-retours-20261028-101500-2.mp4", SegmentNames.name("20261028-101500", 2))
    }

    @Test
    fun theVideoFitsIn720pAndItsSizesAreMultiplesOf16() {
        assertEquals(576 to 1280, RecordSize.of(1080, 2400))
        assertEquals(1280 to 576, RecordSize.of(2400, 1080))
        assertEquals(720 to 1280, RecordSize.of(720, 1280))
        assertEquals(720 to 1152, RecordSize.of(1600, 2560))
    }

    @Test
    fun screenshots() {
        app()
        compose.onRoot().captureRoboImage("screenshots/retours_bubble.png")
        Retours.segmentStarted(1, now)
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("screenshots/retours_bubble_rec.png")
        Retours.setRec(RecState.Idle)
        Retours.screen("Rythme")
        Retours.tutoAt("chapitre 2/9 « Le rythme » · étape 3/5 (kick)")
        compose.onNodeWithTag("retours_note").performClick()
        compose.onNodeWithTag("retours_note_text").performTextInput("Je ne trouve pas le charleston")
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("screenshots/retours_note.png")
    }

    @Test
    fun exportScreenshot() {
        val videos = File(context.filesDir, "retours/videos")
        File(videos, SegmentNames.name("20261028-101500", 1)).writeBytes(ByteArray(19_400_000))
        File(videos, SegmentNames.name("20261028-101500", 2)).writeBytes(ByteArray(7_300_000))
        compose.setContent { PocketStudioTheme(Palettes.Studio) { Box(Modifier.fillMaxSize().background(Neon.Background)) { RetoursExportPanel(close = {}) } } }
        shown("retours_file_1")
        compose.onRoot().captureRoboImage("screenshots/retours_export.png")
    }
}
