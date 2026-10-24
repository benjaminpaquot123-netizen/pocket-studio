package com.pocketstudio.app.report

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketstudio.app.AppInfo
import com.pocketstudio.app.Metrics
import com.pocketstudio.app.export.WavExporter
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.i18n.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** A picture joined to the report: its file in the cache, a small copy to show, where it came from. */
data class BugShot(val id: Long, val file: File, val thumb: ImageBitmap?, val source: Source) {
    enum class Source { AUTO, TAKEN, PICKED }
}

/** The open project for the report: what it says ([summary]) and, if joined, its file ([json], made when asked). */
class ReportProject(val summary: ProjectSummary, val json: () -> String)

data class BugReportState(
    val open: Boolean = false,
    /** « Prendre une capture »: the sheet hidden, the 📸 chip waiting for a touch. */
    val capturing: Boolean = false,
    /** Asked from somewhere ([BugReportViewModel.request]): the screen is captured, then the sheet opens. */
    val requested: String? = null,
    val text: String = "",
    /** The screen behind the sheet, captured as it opened. */
    val autoShot: BugShot? = null,
    val attachAuto: Boolean = true,
    /** Pictures taken with the 📸 chip or chosen on the phone, at most [BugReportViewModel.MAX_ADDED]. */
    val shots: List<BugShot> = emptyList(),
    val attachProject: Boolean = false,
    /** Reporting the previous session's crash: its text, and its journal (journal.json of then). */
    val crash: String? = null,
    val crashJournal: String? = null,
    /** « Voir le rapport »: report.md shown in the sheet. */
    val previewOpen: Boolean = false,
    val busy: Boolean = false,
    /** A word for the user (« Copié », « Image illisible »…), in the interface's language. */
    val message: String? = null,
)

/**
 * « Signaler un bug »: what the sheet holds, the pictures (kept as files in the cache, a thumbnail
 * in memory), and « Partager » (a zip in the cache, then Android's share menu) or « Copier le texte ».
 * Nothing is sent by the app itself.
 */
class BugReportViewModel(private val app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(BugReportState())
    val state: StateFlow<BugReportState> = _state.asStateFlow()

    /** The open project, set by the screen that has it. */
    @Volatile var project: (() -> ReportProject?)? = null

    private val shotsDir get() = File(app.cacheDir, "bugreport")
    private var nextId = 1L

    /** Opens the sheet from [from] (a word for the journal): the screen is captured first. */
    fun request(from: String, crash: String? = null, crashJournal: String? = null) {
        Journal.action("Signaler un bug ($from)")
        _state.update { it.copy(requested = from, crash = crash ?: it.crash, crashJournal = crashJournal ?: it.crashJournal, attachAuto = crash == null) }
    }

    /** The screen behind, captured on the main thread ([screen] null: could not), then the sheet. */
    fun opened(screen: Bitmap?) {
        val old = _state.value.autoShot
        _state.update { it.copy(requested = null, open = true, autoShot = null, message = null) }
        old?.file?.delete()
        if (screen != null) viewModelScope.launch {
            val shot = withContext(Dispatchers.IO) { save(screen, BugShot.Source.AUTO) }
            if (shot != null) _state.update { it.copy(autoShot = shot) }
        }
    }

    fun close() = _state.update { it.copy(open = false, capturing = false, requested = null, message = null) }

    fun setText(text: String) = _state.update { it.copy(text = text.take(MAX_TEXT)) }
    fun setAttachAuto(on: Boolean) = _state.update { it.copy(attachAuto = on) }
    fun setAttachProject(on: Boolean) = _state.update { it.copy(attachProject = on) }
    fun togglePreview() = _state.update { it.copy(previewOpen = !it.previewOpen) }
    fun dismissMessage() = _state.update { it.copy(message = null) }

    val roomLeft: Int get() = MAX_ADDED - _state.value.shots.size

    /** « Prendre une capture »: the sheet goes, the 📸 chip waits while the user goes to the right screen. */
    fun startCapture() {
        if (roomLeft <= 0) return
        _state.update { it.copy(open = false, capturing = true, message = null) }
    }

    fun cancelCapture() = _state.update { it.copy(capturing = false, open = true) }

    /** The 📸 chip touched: [screen] is joined, the sheet comes back. */
    fun captured(screen: Bitmap?) {
        _state.update { it.copy(capturing = false, open = true) }
        if (screen == null) {
            _state.update { it.copy(message = tr("La capture n'a pas marché.")) }
            return
        }
        Journal.action("Capture d'écran prise pour le rapport")
        viewModelScope.launch {
            val shot = withContext(Dispatchers.IO) { save(screen, BugShot.Source.TAKEN) }
            _state.update { s -> if (shot != null && s.shots.size < MAX_ADDED) s.copy(shots = s.shots + shot) else s }
        }
    }

    /** Pictures chosen on the phone (Android's photo picker): copied into the cache, at most what is left. */
    fun picked(uris: List<Uri>) {
        val take = uris.take(roomLeft.coerceAtLeast(0))
        if (take.isEmpty()) return
        viewModelScope.launch {
            var failed = 0
            for (uri in take) {
                val shot = withContext(Dispatchers.IO) { copyPicked(uri) }
                if (shot == null) failed++
                else _state.update { s -> if (s.shots.size < MAX_ADDED) s.copy(shots = s.shots + shot) else s }
            }
            if (failed > 0) _state.update { it.copy(message = tr("Une image n'a pas pu être ajoutée.")) }
        }
    }

    fun remove(id: Long) {
        val s = _state.value
        if (s.autoShot?.id == id) {
            _state.update { it.copy(attachAuto = false) }
            return
        }
        s.shots.firstOrNull { it.id == id }?.file?.delete()
        _state.update { st -> st.copy(shots = st.shots.filterNot { it.id == id }) }
    }

    /** The pictures that go in the zip, in order, with their names there (screenshot-1.png…). */
    fun zipShots(s: BugReportState = _state.value): List<Pair<BugShot, String>> {
        val all = listOfNotNull(s.autoShot?.takeIf { s.attachAuto }) + s.shots
        return all.mapIndexed { i, shot -> shot to "screenshot-${i + 1}.${shot.file.extension.ifBlank { "png" }}" }
    }

    /** report.md's facts, now. */
    fun input(s: BugReportState = _state.value, project: ReportProject? = runCatching { this.project?.invoke() }.getOrNull()): BugReportInput {
        val shots = zipShots(s).map { (shot, name) ->
            Attachment(
                name,
                when (shot.source) {
                    BugShot.Source.AUTO -> "capture de l'écran derrière la fiche, prise à son ouverture"
                    BugShot.Source.TAKEN -> "capture d'écran prise avec « Prendre une capture »"
                    BugShot.Source.PICKED -> "image choisie dans le téléphone"
                },
            )
        }
        val extra = listOfNotNull(
            s.crashJournal?.let { Attachment("journal-plantage.json", "le journal de la session qui s'est fermée d'un coup") },
            project?.takeIf { s.attachProject }?.let { Attachment("project.json", "le projet « ${it.summary.name} » (sans ses sons)") },
        )
        val settings = SettingsStore.state.value
        return BugReportInput(
            createdAtMs = System.currentTimeMillis(),
            userText = s.text,
            versionName = AppInfo.versionName,
            versionCode = AppInfo.versionCode,
            channel = AppInfo.channel,
            device = "${Build.MANUFACTURER} ${Build.MODEL}",
            android = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            stream = Metrics.streamInfo.value.takeIf { NativeEngine.isAvailable },
            roundTripMs = Metrics.roundTripMs.value,
            route = AudioRoute.current(app),
            project = project?.summary,
            health = HealthMonitor.samples.snapshot(),
            events = Journal.events.snapshot(),
            errors = Journal.errors.snapshot(),
            crash = s.crash,
            attachments = shots + extra,
            language = settings.language.code,
            simpleMode = settings.simpleMode,
        )
    }

    fun reportText(): String = BugReportText.markdown(input())

    /** « Copier le texte »: report.md in the clipboard (the pictures cannot go there). */
    fun copyText() {
        val text = reportText()
        runCatching {
            app.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(tr("Rapport de bug"), text))
        }.onSuccess {
            Journal.action("Rapport copié")
            _state.update { it.copy(message = tr("Texte copié : colle-le dans un message.")) }
        }.onFailure { _state.update { it.copy(message = tr("Copie impossible.")) } }
    }

    /** « Partager »: the zip written in the cache, then Android's share menu. */
    fun share() {
        val s = _state.value
        if (s.busy) return
        val project = runCatching { this.project?.invoke() }.getOrNull()
        val input = input(s, project)
        val md = BugReportText.markdown(input)
        val journal = JournalJson.current(listOf("app" to "${input.versionName} (${input.versionCode})", "device" to input.device, "android" to input.android))
        val projectJson = if (s.attachProject) runCatching { project?.json?.invoke() }.getOrNull() else null
        val shots = zipShots(s).map { (shot, name) -> BugReportZip.Shot(shot.file, name) }
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            val zip = withContext(Dispatchers.IO) {
                runCatching {
                    val dir = File(app.cacheDir, "exports")
                    dir.listFiles { f -> f.name.startsWith("rapport-pocket-studio-") }?.forEach { it.delete() }
                    val extraFiles = listOfNotNull(s.crashJournal?.let { "journal-plantage.json" to it })
                    BugReportZip.write(File(dir, BugReportZip.fileName(input.createdAtMs)), md, journal, shots, projectJson, extraFiles)
                }.onFailure { Journal.error("rapport", it) }.getOrNull()
            }
            val shared = zip != null && WavExporter.share(app, listOf(zip), tr("Envoyer le rapport"), subject = "Pocket Studio : rapport de bug ${input.versionName}", text = md.take(SHARE_TEXT))
            if (shared) {
                Journal.action("Rapport partagé")
                // Sent: the next report starts afresh.
                s.shots.forEach { it.file.delete() }
                s.autoShot?.file?.delete()
                _state.value = BugReportState()
            } else {
                _state.update { it.copy(busy = false, message = tr("Le rapport n'a pas pu être préparé.")) }
            }
        }
    }

    private fun save(bitmap: Bitmap, source: BugShot.Source): BugShot? = runCatching {
        shotsDir.mkdirs()
        val id = synchronized(this) { nextId++ }
        val file = File(shotsDir, "shot-$id-${System.currentTimeMillis()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        BugShot(id, file, thumbnail(bitmap), source)
    }.onFailure { Journal.error("capture", it) }.getOrNull()

    private fun copyPicked(uri: Uri): BugShot? = runCatching {
        shotsDir.mkdirs()
        val ext = when (app.contentResolver.getType(uri)) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            "image/heic", "image/heif" -> "heic"
            else -> "jpg"
        }
        val id = synchronized(this) { nextId++ }
        val file = File(shotsDir, "picked-$id-${System.currentTimeMillis()}.$ext")
        val input = app.contentResolver.openInputStream(uri) ?: error("no stream")
        input.use { inp ->
            file.outputStream().use { out ->
                val buf = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val n = inp.read(buf)
                    if (n < 0) break
                    total += n
                    check(total <= MAX_PICKED_BYTES) { "picture too big" }
                    out.write(buf, 0, n)
                }
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        while (bounds.outHeight / (sample * 2) >= THUMB_HEIGHT) sample *= 2
        val small = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
        BugShot(id, file, small?.let { thumbnail(it) }, BugShot.Source.PICKED)
    }.onFailure { Journal.error("image ajoutée", it) }.getOrNull()

    private fun thumbnail(b: Bitmap): ImageBitmap? = runCatching {
        val h = THUMB_HEIGHT
        val w = (b.width.toFloat() * h / b.height.coerceAtLeast(1)).toInt().coerceAtLeast(1)
        Bitmap.createScaledBitmap(b, w, h, true).asImageBitmap()
    }.getOrNull()

    override fun onCleared() {
        _state.value.autoShot?.file?.delete()
        _state.value.shots.forEach { it.file.delete() }
    }

    companion object {
        /** Pictures added besides the automatic capture. */
        const val MAX_ADDED = 5
        const val MAX_TEXT = 4000
        const val THUMB_HEIGHT = 240
        const val MAX_PICKED_BYTES = 20L * 1024 * 1024
        /** The report's beginning also goes as the message's text (a mail app shows it). */
        const val SHARE_TEXT = 3000
    }
}
