package com.pocketstudio.app.remix

import android.content.Context
import com.pocketstudio.model.i18n.tr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File

/** What the separation service is doing, for the Remix screen and the notification. */
sealed interface SeparationState {
    data object Idle : SeparationState
    data class DownloadingModel(val progress: Float) : SeparationState
    data class Decoding(val name: String) : SeparationState
    data class Separating(val name: String, val progress: Float) : SeparationState
    data class Analysing(val name: String) : SeparationState
    data class Done(val set: StemSet) : SeparationState
    data class Failed(val message: String) : SeparationState

    /**
     * Android stopped the app while it separated [name] (most often for memory): found on the next
     * start. [uri]: the song, still readable, to start again; null when it cannot be.
     */
    data class Interrupted(val name: String, val uri: String?) : SeparationState {
        val message: String get() = tr("La séparation de « %s » a été interrompue : Android a fermé l'appli (souvent par manque de mémoire).", name)
    }

    val busy get() = this is DownloadingModel || this is Decoding || this is Separating || this is Analysing
}

/** Process-wide state shared by [SeparationService] (writer) and the UI (reader). */
object SeparationJob {
    private val _state = MutableStateFlow<SeparationState>(SeparationState.Idle)
    val state: StateFlow<SeparationState> = _state.asStateFlow()

    internal fun set(state: SeparationState) {
        _state.value = state
    }

    /** Clears a finished or failed result once the screen has shown it. */
    fun dismiss() {
        _state.compareAndSet(_state.value.takeIf { !it.busy } ?: return, SeparationState.Idle)
    }

    // --- A separation the app did not live to finish (audit 02 G2) -----------------------------

    /** Written while a song is separated, deleted when the job ends in any way. */
    internal fun marker(context: Context) = File(File(context.filesDir, "separation"), "running.json")

    internal fun markRunning(context: Context, name: String, uri: String?) {
        runCatching {
            val file = marker(context)
            file.parentFile?.mkdirs()
            file.writeText(JSONObject().put("name", name).put("uri", uri ?: "").put("at", System.currentTimeMillis()).toString())
        }
    }

    internal fun clearRunning(context: Context) {
        marker(context).delete()
    }

    /**
     * On start (the Remix page's model): a marker left while nothing runs means Android stopped the
     * app mid-separation; the screen then says so (and offers to start again) instead of nothing.
     */
    fun recover(context: Context) {
        if (_state.value.busy) return
        val file = marker(context)
        if (!file.isFile) return
        val json = runCatching { JSONObject(file.readText()) }.getOrNull()
        file.delete()
        val name = json?.optString("name").orEmpty().ifBlank { tr("Morceau") }
        val uri = json?.optString("uri").orEmpty().ifBlank { null }
        _state.value = SeparationState.Interrupted(name, uri)
    }

    // --- « Touche pour l'ouvrir » (audit 02 M5) --------------------------------------------------

    private val _openRemix = MutableStateFlow(false)
    /** The separation's notification was touched: the app shows the Remix page. */
    val openRemix: StateFlow<Boolean> = _openRemix.asStateFlow()

    fun requestOpen() {
        _openRemix.value = true
    }

    fun openHandled() {
        _openRemix.value = false
    }
}
