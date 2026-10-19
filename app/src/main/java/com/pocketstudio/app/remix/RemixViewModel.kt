package com.pocketstudio.app.remix

import android.app.Application
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketstudio.app.report.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RemixViewModel(app: Application) : AndroidViewModel(app) {
    val state: StateFlow<SeparationState> = SeparationJob.state

    private val _sets = MutableStateFlow<List<StemSet>>(emptyList())
    val sets: StateFlow<List<StemSet>> = _sets.asStateFlow()

    private val _modelInstalled = MutableStateFlow(false)
    val modelInstalled: StateFlow<Boolean> = _modelInstalled.asStateFlow()

    private val _playing = MutableStateFlow<Pair<String, String>?>(null)
    val playing: StateFlow<Pair<String, String>?> = _playing.asStateFlow()

    private var player: MediaPlayer? = null

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
        }.getOrNull() ?: uri.lastPathSegment ?: "Morceau"
        stop()
        SeparationService.separate(app, uri, name.substringBeforeLast('.').ifBlank { name })
    }

    fun cancel() = SeparationService.cancel(getApplication())

    fun dismiss() = SeparationJob.dismiss()

    fun play(set: StemSet, stem: String) {
        stop()
        val mp = MediaPlayer()
        player = mp
        _playing.value = set.id to stem
        try {
            mp.setDataSource(set.stem(stem).absolutePath)
            mp.setOnPreparedListener { it.start() }
            mp.setOnCompletionListener { stop() }
            mp.setOnErrorListener { _, what, extra ->
                AppLog.add("Remix : lecture impossible ($what/$extra)")
                stop()
                true
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            AppLog.add("Remix : lecture impossible (${e.message})")
            stop()
        }
    }

    fun stop() {
        player?.release()
        player = null
        _playing.value = null
    }

    fun delete(set: StemSet) {
        if (_playing.value?.first == set.id) stop()
        viewModelScope.launch {
            withContext(Dispatchers.IO) { StemStore.delete(set) }
            refresh()
        }
    }

    override fun onCleared() {
        stop()
    }
}
