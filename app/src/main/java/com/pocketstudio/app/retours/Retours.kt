package com.pocketstudio.app.retours

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import com.pocketstudio.app.AppInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** One line of the « Mode retours » log: when (wall clock; video [segment] and time inside it, 0: not filming), what, where. */
data class RetoursEvent(val wallMs: Long, val segment: Int, val inSegmentMs: Long, val kind: String, val detail: String)

/** A note written from the bubble, with where the app was and its screenshot (file name in screens/, or null). */
data class RetoursNote(
    val n: Int,
    val wallMs: Long,
    val segment: Int,
    val inSegmentMs: Long,
    val text: String,
    val screen: String,
    val tuto: String,
    val screenshot: String?,
)

/** The screen recording, as the bubble shows it. */
sealed interface RecState {
    data object Idle : RecState
    /** The system's consent dialog is up, or the encoders are starting. */
    data object Starting : RecState
    data class Recording(val segment: Int, val sinceNanos: Long) : RecState
    /** The last segment is being finished. */
    data object Stopping : RecState
}

/**
 * « Mode retours » (owner feedback while testing): notes, an event log and screen recordings,
 * kept in filesDir/retours until « Envoyer mes retours » shares them (never uploaded by itself).
 * Thread-safe; never call from the audio thread. Writes go to one background thread, in order.
 */
object Retours {
    const val DIR = "retours"
    private const val MAX_EVENTS = 20_000
    private const val RECENT = 500

    /** Monotonic clock (System.nanoTime: the screen frames' time base) and wall clock; tests set them. */
    @Volatile var clock: () -> Long = System::nanoTime
    @Volatile var wall: () -> Long = System::currentTimeMillis

    /** The mode is on (Réglages, default on in this build): events are logged only then. */
    @Volatile var active: Boolean = false

    @Volatile var screen: String = ""
        private set
    @Volatile var tuto: String = ""
        private set

    private val _rec = MutableStateFlow<RecState>(RecState.Idle)
    val rec: StateFlow<RecState> = _rec.asStateFlow()
    private val _notes = MutableStateFlow<List<RetoursNote>>(emptyList())
    val notes: StateFlow<List<RetoursNote>> = _notes.asStateFlow()
    /** A short message for the bubble (refused, stopped, no room…), then null. */
    val message = MutableStateFlow<String?>(null)
    /** « Envoyer mes retours » open (from the bubble or Aide). */
    val exportOpen = MutableStateFlow(false)

    /** The video file being written (not offered for sending until finished). */
    @Volatile var recordingFile: File? = null

    @Volatile private var segment = 0
    @Volatile private var segmentStart = 0L
    @Volatile private var sessionStart = System.nanoTime()
    private val recent = ArrayDeque<RetoursEvent>()
    private var logged = 0
    @Volatile private var dir: File? = null
    private val writer = Executors.newSingleThreadExecutor { r -> Thread(r, "retours-writer").apply { isDaemon = true } }

    /** The folder (made on first use) and what an earlier start left there. */
    fun attach(context: Context) {
        if (dir != null) return
        val d = File(context.applicationContext.filesDir, DIR)
        dir = d
        sessionStart = clock()
        writer.execute {
            runCatching {
                d.mkdirs()
                File(d, "videos").mkdirs()
                File(d, "screens").mkdirs()
                val f = File(d, "notes.jsonl")
                if (f.exists()) _notes.value = f.readLines().mapNotNull { noteOf(it) }
                logged = File(d, "events.jsonl").takeIf { it.exists() }?.useLines { it.count() } ?: 0
            }
        }
    }

    fun folder(): File? = dir
    fun videosDir(): File? = dir?.let { File(it, "videos") }

    /** Logs [kind] [detail] (when the mode is on), timed in the current video segment. */
    fun event(kind: String, detail: String) {
        if (!active) return
        val e = RetoursEvent(wall(), segment, sinceSegmentMs(), kind, detail.take(300))
        synchronized(recent) {
            if (recent.size == RECENT) recent.removeFirst()
            recent.addLast(e)
            if (logged >= MAX_EVENTS) return
            logged++
        }
        val d = dir ?: return
        writer.execute { runCatching { File(d, "events.jsonl").appendText(eventJson(e).toString() + "\n") } }
    }

    /** The last events logged (tests, the notes' context). */
    fun recentEvents(): List<RetoursEvent> = synchronized(recent) { recent.toList() }

    /** The screen shown changed (tab › page). */
    fun screen(name: String) {
        if (name == screen) return
        screen = name
        event("écran", name)
    }

    /** Where the tutorial is (« chapitre 2/9 Le rythme · étape 3/5 »), "" when it is not open. */
    fun tutoAt(where: String) {
        if (where == tuto) return
        tuto = where
        if (where.isNotEmpty()) event("tuto", where)
    }

    /** A line of the app's journal (errors among them) while the mode is on. */
    fun fromLog(line: String) = event("journal", line)

    fun setRec(state: RecState) {
        _rec.value = state
    }

    /** Segment [n] of the video starts at [startNanos] (its first picture's time). */
    fun segmentStarted(n: Int, startNanos: Long) {
        segment = n
        segmentStart = startNanos
        _rec.value = RecState.Recording(n, startNanos)
        event("vidéo", "segment $n")
    }

    /** The recording ended ([why]: a message for the bubble, or null when asked). */
    fun recordingEnded(why: String?) {
        event("vidéo", "fin" + (why?.let { " : $it" } ?: ""))
        segment = 0
        recordingFile = null
        _rec.value = RecState.Idle
        if (why != null) message.value = why
    }

    private fun sinceSegmentMs(): Long = ((clock() - if (segment > 0) segmentStart else sessionStart) / 1_000_000L).coerceAtLeast(0L)

    /** Saves a note with where the app is and [screenshot] (downscaled to 720 px at most, PNG). */
    fun addNote(text: String, screenshot: Bitmap?): RetoursNote {
        val note = synchronized(this) {
            val n = (_notes.value.maxOfOrNull { it.n } ?: 0) + 1
            RetoursNote(n, wall(), segment, sinceSegmentMs(), text.trim(), screen, tuto, screenshot?.let { "note-$n.png" })
                .also { _notes.value = _notes.value + it }
        }
        event("note", "note ${note.n}")
        val d = dir ?: return note
        writer.execute {
            runCatching {
                File(d, "notes.jsonl").appendText(noteJson(note).toString() + "\n")
                if (screenshot != null) {
                    val scale = minOf(1f, 720f / maxOf(screenshot.width, screenshot.height).coerceAtLeast(1))
                    val small = if (scale < 1f) Bitmap.createScaledBitmap(screenshot, (screenshot.width * scale).toInt().coerceAtLeast(1), (screenshot.height * scale).toInt().coerceAtLeast(1), true) else screenshot
                    File(d, "screens").mkdirs()
                    File(d, "screens/${note.screenshot}").outputStream().use { small.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    if (small !== screenshot) small.recycle()
                }
            }
        }
        return note
    }

    /** Waits for the pending writes (export, tests). */
    fun flush() {
        runCatching { writer.submit {}.get(5, TimeUnit.SECONDS) }
    }

    /** The finished videos, oldest first. */
    fun videos(): List<File> =
        videosDir()?.listFiles { f -> f.extension == "mp4" && f.length() > 0 && f != recordingFile }?.sortedBy { it.name }.orEmpty()

    /**
     * What « Envoyer mes retours » offers: each finished video (≤ 20 MB each) and notes.zip (notes.md
     * in French, events.json, the screenshots), built now. Blocking: off the main thread.
     */
    fun exportFiles(): List<File> {
        flush()
        val d = dir ?: return emptyList()
        val zip = runCatching { buildZip(d) }.getOrNull()
        return videos() + listOfNotNull(zip)
    }

    private fun buildZip(d: File): File? {
        val notes = _notes.value
        val events = File(d, "events.jsonl").takeIf { it.exists() }?.readLines().orEmpty().filter { it.isNotBlank() }
        if (notes.isEmpty() && events.isEmpty()) return null
        val out = File(d, "export").apply { mkdirs() }
        val zip = File(out, "notes.zip")
        ZipOutputStream(zip.outputStream().buffered()).use { z ->
            z.putNextEntry(ZipEntry("notes.md"))
            z.write(notesMarkdown(notes, videos().map { it.name }).toByteArray())
            z.closeEntry()
            z.putNextEntry(ZipEntry("events.json"))
            z.write(JSONArray().apply { events.forEach { l -> runCatching { put(JSONObject(l)) } } }.toString(1).toByteArray())
            z.closeEntry()
            notes.mapNotNull { it.screenshot }.map { File(d, "screens/$it") }.filter { it.exists() }.forEach { f ->
                z.putNextEntry(ZipEntry("screenshots/${f.name}"))
                f.inputStream().use { it.copyTo(z) }
                z.closeEntry()
            }
        }
        return zip
    }

    /** notes.md: readable as it is (French), one section per note. */
    fun notesMarkdown(notes: List<RetoursNote>, videos: List<String>): String = buildString {
        val date = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
        fun at(ms: Long) = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault()).format(date)
        append("# Retours BenStudio\n\n")
        append("- Appli : ${runCatching { AppInfo.versionLabel }.getOrDefault("?")}\n")
        append("- Téléphone : ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
        append("- Vidéos : ${if (videos.isEmpty()) "aucune" else videos.joinToString(", ")}\n")
        append("- Journal détaillé : events.json (segment = numéro de la vidéo, inSegmentMs = moment dans cette vidéo)\n\n")
        if (notes.isEmpty()) append("Aucune note.\n")
        notes.forEach { n ->
            append("## Note ${n.n} — ${at(n.wallMs)}\n\n")
            append(if (n.segment > 0) "- Vidéo ${n.segment}, à ${clock(n.inSegmentMs)}\n" else "- Pas de vidéo en cours\n")
            append("- Écran : ${n.screen.ifEmpty { "?" }}\n")
            if (n.tuto.isNotEmpty()) append("- Tuto : ${n.tuto}\n")
            append("\n> ${n.text.ifEmpty { "(vide)" }.replace("\n", "\n> ")}\n\n")
            n.screenshot?.let { append("![capture](screenshots/$it)\n\n") }
        }
    }

    /** « 1:02.3 » for [ms] in a video. */
    fun clock(ms: Long): String = "%d:%02d.%d".format(ms / 60_000, ms / 1000 % 60, ms / 100 % 10)

    /** Deletes everything (videos, notes, log); the video being recorded is kept. */
    fun deleteAll() {
        val d = dir ?: return
        synchronized(this) { _notes.value = emptyList() }
        synchronized(recent) { recent.clear(); logged = 0 }
        writer.execute {
            runCatching {
                d.listFiles()?.forEach { f -> if (f.name != "videos") f.deleteRecursively() }
                videos().forEach { it.delete() }
                File(d, "screens").mkdirs()
            }
        }
        flush()
    }

    private fun eventJson(e: RetoursEvent) = JSONObject()
        .put("wallMs", e.wallMs).put("segment", e.segment).put("inSegmentMs", e.inSegmentMs).put("kind", e.kind).put("detail", e.detail)

    private fun noteJson(n: RetoursNote) = JSONObject()
        .put("n", n.n).put("wallMs", n.wallMs).put("segment", n.segment).put("inSegmentMs", n.inSegmentMs)
        .put("text", n.text).put("screen", n.screen).put("tuto", n.tuto).put("screenshot", n.screenshot ?: JSONObject.NULL)

    private fun noteOf(line: String): RetoursNote? = runCatching {
        val o = JSONObject(line)
        RetoursNote(
            o.getInt("n"), o.getLong("wallMs"), o.optInt("segment"), o.optLong("inSegmentMs"), o.optString("text"),
            o.optString("screen"), o.optString("tuto"), if (o.isNull("screenshot")) null else o.optString("screenshot"),
        )
    }.getOrNull()

    /** Tests: forget the folder and the state. */
    @androidx.annotation.VisibleForTesting
    internal fun resetForTests() {
        flush()
        dir = null
        segment = 0
        screen = ""
        tuto = ""
        recordingFile = null
        _rec.value = RecState.Idle
        _notes.value = emptyList()
        message.value = null
        exportOpen.value = false
        synchronized(recent) { recent.clear(); logged = 0 }
    }
}
