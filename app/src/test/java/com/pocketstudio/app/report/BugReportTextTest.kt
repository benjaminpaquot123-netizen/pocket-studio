package com.pocketstudio.app.report

import android.media.AudioDeviceInfo
import com.pocketstudio.engine.StreamInfo
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.AutomationLane
import com.pocketstudio.model.Project
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipFile

/** report.md, the zip and the crash's journal on disk. */
class BugReportTextTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Before
    fun fresh() {
        Journal.clear()
        HealthMonitor.reset()
    }

    @After
    fun back() {
        Journal.clear()
        HealthMonitor.reset()
    }

    private val now = 1_790_000_000_000L

    private fun input(
        text: String = "Le son a coupé dans Live",
        events: List<JournalEvent> = (1..80).map { JournalEvent(now - (80 - it) * 1000L, JournalEvent.Kind.NAV, "écran $it") },
        health: List<HealthSample> = (0 until 240).map { i -> HealthSample(now - 60_000 + i * 250L + 1, peak = 0.5f, shortTermLufs = -11f, cpu = 0.4f, xruns = if (i > 200) 2 else 0, playing = true) },
        crash: String? = null,
        stream: StreamInfo? = StreamInfo(sampleRate = 48_000, framesPerBurst = 96, bufferSize = 192, bufferCapacity = 3072, exclusive = true, aaudio = true, mmap = true, xruns = 2, restarts = 1, outputLatencyMs = 10.2, running = true),
        route: String? = AudioRoute.describe(listOf(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP), listOf(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)),
        project: ProjectSummary? = ProjectSummary("Nuit d'été", "acid-techno", 128.0, "Drop · mes. 14/60", true, 6, 60, 5, -1.5f, "AUTO", masterLane = true),
    ) = BugReportInput(
        createdAtMs = now, userText = text, versionName = "0.22.345", versionCode = 345, channel = "preview",
        device = "HONOR Magic 8 Pro", android = "16 (API 36)", stream = stream, roundTripMs = 44.5, route = route, project = project,
        health = health, events = events, errors = listOf(JournalEvent(now - 5000, JournalEvent.Kind.ERROR, "export : disque plein")),
        crash = crash, attachments = listOf(Attachment("screenshot-1.png", "capture automatique")),
    )

    @Test
    fun saysEverythingInFrench() {
        val md = BugReportText.markdown(input())
        listOf(
            "# Rapport de bug : BenStudio", "## Ce qui s'est passé", "Le son a coupé dans Live", "## Appli et téléphone",
            "Version : 0.22.345 (code 345, canal preview)", "Téléphone : HONOR Magic 8 Pro", "Android : 16 (API 36)",
            "Sortie du son : Bluetooth (A2DP) (branchés : haut-parleur du téléphone, Bluetooth (A2DP))",
            "AAudio, exclusif, MMAP, 48000 Hz, burst 96", "Latence de sortie : 10,2 ms", "Décrochages audio depuis le démarrage : 2",
            "son rouvert par Android : 1 fois", "## Projet", "« Nuit d'été »", "acid-techno", "128,0 BPM", "Drop · mes. 14/60 (en lecture)",
            "automation du volume master : oui", "## Son des 60 dernières secondes", "crête   |", "repères |", "Crête (dBFS), une valeur par seconde",
            "Volume court terme (LUFS)", "Charge du fil audio (%)", "## Erreurs (1)", "export : disque plein", "## Pièces jointes",
            "screenshot-1.png : capture automatique", "journal.json",
        ).forEach { assertTrue("missing « $it »:\n$md", md.contains(it)) }
        // The last 50 events only, the newest last.
        assertTrue(md.contains("## Journal (50 derniers événements sur 80)"))
        assertTrue(md.contains("[écran] écran 80"))
        assertTrue(md.contains("[écran] écran 31"))
        assertFalse(md.contains("[écran] écran 30\n"))
        // The sparkline is 60 characters between its bars; the underrun shows.
        val spark = md.lines().first { it.startsWith("crête   |") }
        assertEquals(60, spark.removePrefix("crête   |").removeSuffix("|").length)
        assertTrue(md.lines().first { it.startsWith("repères |") }.contains('x'))
        assertFalse(md.contains("## Plantage"))
    }

    @Test
    fun anEmptyStateStillMakesAReport() {
        val md = BugReportText.markdown(input(text = "  ", events = emptyList(), health = emptyList(), stream = null, project = null, route = null).copy(errors = emptyList(), attachments = emptyList()))
        assertTrue(md.contains("(rien d'écrit)"))
        assertTrue(md.contains("Audio : moteur absent"))
        assertTrue(md.contains("Sortie du son : inconnue"))
        assertTrue(md.contains("Aucun projet ouvert."))
        assertTrue(md.contains("Aucune mesure"))
        assertTrue(md.contains("Aucune erreur relevée."))
        assertTrue(md.contains("Journal vide."))
    }

    @Test
    fun theSoundsOutputIsSaidByKindNeverByName() {
        val speaker = AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        val wired = AudioDeviceInfo.TYPE_WIRED_HEADPHONES
        assertNull(AudioRoute.describe(emptyList(), emptyList()))
        assertEquals("haut-parleur du téléphone", AudioRoute.describe(listOf(speaker), listOf(speaker)))
        assertEquals("casque filaire (branchés : haut-parleur du téléphone, casque filaire)", AudioRoute.describe(listOf(wired), listOf(speaker, wired, wired)))
        // Before Android 13 the routed output is not known: the connected ones are said.
        assertEquals("inconnue (branchés : haut-parleur du téléphone, Bluetooth LE (casque))", AudioRoute.describe(emptyList(), listOf(speaker, AudioDeviceInfo.TYPE_BLE_HEADSET)))
        assertEquals("appareil de type 9999", AudioRoute.typeName(9999))
    }

    @Test
    fun aCrashComesFirstAfterTheUsersWords() {
        val md = BugReportText.markdown(input(crash = "Version 0.22 · thread main\njava.lang.IllegalStateException: boum\n\tat Foo.bar"))
        assertTrue(md.indexOf("## Plantage de la session précédente") in md.indexOf("## Ce qui s'est passé")..md.indexOf("## Appli et téléphone"))
        assertTrue(md.contains("IllegalStateException: boum"))
    }

    @Test
    fun theZipHoldsTheReportTheJournalThePicturesAndTheProject() {
        val shot = tmp.newFile("a.png").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val missing = File(tmp.root, "gone.png")
        val out = File(tmp.root, "exports/" + BugReportZip.fileName(now))
        BugReportZip.write(
            out, "# rapport", "{}", listOf(BugReportZip.Shot(shot, "screenshot-1.png"), BugReportZip.Shot(missing, "screenshot-2.png")),
            projectJson = "{\"name\":\"x\"}", extra = listOf("journal-plantage.json" to "{\"crash\":1}"),
        )
        assertTrue(out.name.startsWith("rapport-pocket-studio-") && out.name.endsWith(".zip"))
        ZipFile(out).use { z ->
            assertEquals(listOf("report.md", "journal.json", "screenshot-1.png", "project.json", "journal-plantage.json"), z.entries().toList().map { it.name })
            assertEquals("# rapport", z.getInputStream(z.getEntry("report.md")).readBytes().toString(Charsets.UTF_8))
            assertEquals(3, z.getInputStream(z.getEntry("screenshot-1.png")).readBytes().size)
        }
        assertFalse(File(out.parentFile, out.name + ".tmp").exists())
    }

    @Test
    fun aCrashLeavesItsJournalForTheNextLaunch() {
        val dir = tmp.newFolder("files")
        Journal.screen("Live › Live")
        Journal.action("▶ Lecture")
        assertTrue(JournalFiles.saveCrash(dir, "main", IllegalStateException("boum")))
        val saved = JournalFiles.crashJournal(dir)
        assertNotNull(saved)
        assertTrue(saved!!.contains("\"crash\":\"java.lang.IllegalStateException: boum"))
        assertTrue(saved.contains("Live › Live"))
        assertTrue(saved.contains("plantage (thread main)"))
        // Reported or dismissed: gone.
        JournalFiles.clearCrash(dir)
        assertNull(JournalFiles.crashJournal(dir))
    }

    @Test
    fun aNativeCrashFindsTheLastLiveCopy() {
        val dir = tmp.newFolder("files2")
        Journal.action("geste avant le plantage natif")
        assertTrue(JournalFiles.saveLive(dir))
        // The next launch: the live copy becomes the previous session's journal.
        JournalFiles.startSession(dir)
        assertFalse(File(dir, JournalFiles.LIVE).exists())
        assertTrue(JournalFiles.crashJournal(dir)!!.contains("geste avant le plantage natif"))
    }

    @Test
    fun theMasterLaneIsReadAtTheSongPosition() {
        val lane = AutomationLane(AutoTarget.masterVolume(), mode = AutomationLane.SONG).withSongPoint(0, 1f).withSongPoint(4, 0f)
        val p = Project(automation = listOf(lane))
        assertNotNull(masterLane(p))
        assertEquals(1f, masterLaneValue(p, 0)!!, 1e-4f)
        assertEquals(0f, masterLaneValue(p, 4 * p.stepsPerBar)!!, 1e-4f)
        assertEquals(0.5f, masterLaneValue(p, 2 * p.stepsPerBar)!!, 1e-4f)
        // Held after the last point.
        assertEquals(0f, masterLaneValue(p, 7 * p.stepsPerBar)!!, 1e-4f)
        assertNull(masterLaneValue(p, -1))
        assertNull(masterLaneValue(Project(), 10))
    }
}
