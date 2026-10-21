package com.pocketstudio.app.remix

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketstudio.app.report.AppLog
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.engine.NativeStems
import com.pocketstudio.app.audio.finishOrRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.pocketstudio.model.i18n.tr

class RemixViewModel(app: Application) : AndroidViewModel(app) {
    val state: StateFlow<SeparationState> = SeparationJob.state

    private val _sets = MutableStateFlow<List<StemSet>>(emptyList())
    val sets: StateFlow<List<StemSet>> = _sets.asStateFlow()

    private val _modelInstalled = MutableStateFlow(false)
    val modelInstalled: StateFlow<Boolean> = _modelInstalled.asStateFlow()

    private val _preview = MutableStateFlow<StemPreview?>(null)
    /** The song whose stems are being listened to on this screen, if any. */
    val preview: StateFlow<StemPreview?> = _preview.asStateFlow()
    private var previewStems: NativeStems? = null
    private var previewJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            // Refresh the list and the model flag whenever a job ends (and once at start).
            SeparationJob.state.collect { if (!it.busy) refresh() }
        }
    }

    private suspend fun refresh() {
        val app = getApplication<Application>()
        val (sets, installed) = withContext(Dispatchers.IO) { StemStore.list(app) to SeparationModel.isInstalled(app) }
        _sets.value = sets
        _modelInstalled.value = installed
    }

    fun downloadModel() = SeparationService.downloadModel(getApplication())

    fun separate(uri: Uri) {
        val app = getApplication<Application>()
        // Keep read access while the service works, even if the picker's grant ends.
        runCatching { app.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        val name = runCatching {
            app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        }.getOrNull() ?: uri.lastPathSegment ?: tr("Morceau")
        stop()
        SeparationService.separate(app, uri, name.substringBeforeLast('.').ifBlank { name })
    }

    fun cancel() = SeparationService.cancel(getApplication())

    fun dismiss() = SeparationJob.dismiss()

    /** Plays the four stems of [set] together, apart from the project (all on by default). */
    fun playAll(set: StemSet) {
        val p = _preview.value
        if (p != null && p.setId == set.id) {
            if (!p.loading && NativeEngine.isAvailable) {
                NativeEngine.playPreview(true)
                _preview.value = p.copy(playing = true)
            }
            return
        }
        start(set, List(STEMS) { true })
    }

    /** Switches one stem on or off; it never stops the others. Starts [set] if another song played. */
    fun toggleStem(set: StemSet, stem: Int) {
        val p = _preview.value
        if (p == null || p.setId != set.id) {
            start(set, List(STEMS) { it == stem })
            return
        }
        val on = p.on.mapIndexed { i, v -> if (i == stem) !v else v }
        _preview.value = p.copy(on = on)
        if (NativeEngine.isAvailable) NativeEngine.setPreviewParam(stem, MUTE, if (on[stem]) 0f else 1f)
    }

    private fun start(set: StemSet, on: List<Boolean>) {
        stop()
        if (!NativeEngine.isAvailable) return
        _preview.value = StemPreview(set.id, on, loading = true)
        previewJob = viewModelScope.launch {
            // A bundle loaded while the user moved on is closed, not leaked.
            val loaded = finishOrRelease(Dispatchers.IO, NativeStems::close) {
                runCatching {
                    NativeStems.load(set.stem("drums").path, set.stem("bass").path, set.stem("other").path, set.stem("vocals").path)
                }.getOrNull()
            }
            val p = _preview.value
            if (loaded == null || p == null || p.setId != set.id || !isActive) {
                loaded?.close()
                if (loaded == null && isActive) {
                    AppLog.add("Remix : écoute impossible de « ${set.name} »")
                    _preview.value = null
                }
                return@launch
            }
            previewStems = loaded
            NativeEngine.setPreviewStems(loaded)
            p.on.forEachIndexed { i, v -> NativeEngine.setPreviewParam(i, MUTE, if (v) 0f else 1f) }
            NativeEngine.playPreview(true)
            _preview.value = p.copy(loading = false, playing = true, frames = loaded.frames)
        }
    }

    fun stop() {
        previewJob?.cancel()
        previewJob = null
        if (NativeEngine.isAvailable) {
            NativeEngine.playPreview(false)
            NativeEngine.setPreviewStems(null)
        }
        previewStems?.close()  // the engine keeps its own reference until it lets go
        previewStems = null
        _preview.value = null
    }

    /** Follows the playhead about 30 times a second while the Remix screen is visible. */
    suspend fun pollPreview() {
        if (!NativeEngine.isAvailable) return
        while (true) {
            val p = _preview.value
            if (p != null && p.playing) {
                val pos = NativeEngine.previewPosition()
                // Past the end, or stopped: the stems stay loaded, ready to play again.
                _preview.value = if (pos < 0 && p.position >= 0) p.copy(playing = false, position = -1) else p.copy(position = pos)
            }
            kotlinx.coroutines.delay(33)
        }
    }

    fun pause() {
        val p = _preview.value ?: return
        if (NativeEngine.isAvailable) NativeEngine.playPreview(false)
        _preview.value = p.copy(playing = false, position = -1)
    }

    /** Deletes the song's stems, never those a project plays along (the screen asks first). */
    fun delete(set: StemSet) {
        if (_preview.value?.setId == set.id) stop()
        viewModelScope.launch {
            val deleted = withContext(Dispatchers.IO) { StemStore.delete(getApplication(), set) }
            if (!deleted) AppLog.add("Remix : « ${set.name} » gardé, un projet l'utilise")
            refresh()
        }
    }

    override fun onCleared() {
        stop()
    }
}

/** Listening to a song's stems together; [on] says which ones sound. [position] -1 = stopped. */
data class StemPreview(
    val setId: String,
    val on: List<Boolean>,
    val loading: Boolean = false,
    val playing: Boolean = false,
    val position: Long = -1,
    val frames: Long = 0,
)

private const val STEMS = 4
private const val MUTE = 1  // StemPlayer::Mute
