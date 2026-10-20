package com.pocketstudio.app.generate

import android.app.ActivityManager
import android.app.Application
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketstudio.app.audio.AudioEdits
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.download.VerifiedDownload
import com.pocketstudio.app.remix.SeparationService
import com.pocketstudio.app.report.AppLog
import com.pocketstudio.model.T5Tokenizer
import com.pocketstudio.separation.StableAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt
import kotlin.random.Random

/** A generated sound waiting to be heard and placed. */
class Generated(val pcm: Pcm, val prompt: String, val seed: Long) {
    val name: String get() = prompt.trim().replaceFirstChar { it.uppercase() }.take(32)
}

sealed interface GenerateState {
    data object Idle : GenerateState
    data object Checking : GenerateState
    data class Running(val progress: Float) : GenerateState
    data class Failed(val message: String) : GenerateState
}

/**
 * Text to sound on the phone (Stable Audio Open Small, powered by Stability AI). The model is
 * downloaded by the separation service; generation itself takes seconds, so it runs here.
 */
class GenerateViewModel(app: Application) : AndroidViewModel(app) {
    val download: StateFlow<ModelDownload> = GenerationJob.download

    private val _installed = MutableStateFlow(false)
    val installed: StateFlow<Boolean> = _installed.asStateFlow()

    private val _state = MutableStateFlow<GenerateState>(GenerateState.Idle)
    val state: StateFlow<GenerateState> = _state.asStateFlow()

    private val _result = MutableStateFlow<Generated?>(null)
    val result: StateFlow<Generated?> = _result.asStateFlow()

    private val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing.asStateFlow()

    private val _prompt = MutableStateFlow("")
    val prompt: StateFlow<String> = _prompt.asStateFlow()
    private val _seconds = MutableStateFlow(4f)
    val seconds: StateFlow<Float> = _seconds.asStateFlow()

    fun setPrompt(text: String) {
        _prompt.value = text.take(300)
        dismissError()
    }

    fun setSeconds(value: Float) {
        _seconds.value = value.coerceIn(1f, StableAudio.MAX_SECONDS)
    }

    fun generate() = generate(_prompt.value, _seconds.value)

    private val cancelled = AtomicBoolean(false)
    private var tokenizer: T5Tokenizer? = null
    private var player: AudioTrack? = null

    init {
        viewModelScope.launch {
            GenerationJob.download.collect { if (it !is ModelDownload.Running) refresh() }
        }
    }

    private suspend fun refresh() {
        _installed.value = withContext(Dispatchers.IO) { GenerationModel.isInstalled(getApplication()) }
    }

    fun downloadModel() {
        GenerationJob.set(ModelDownload.Running(0f))
        SeparationService.downloadGenerationModel(getApplication())
    }

    fun cancelDownload() = SeparationService.cancel(getApplication())

    fun removeModel() {
        stopPreview()
        viewModelScope.launch {
            withContext(Dispatchers.IO) { GenerationModel.remove(getApplication()) }
            verified = false
            tokenizer = null
            refresh()
        }
    }

    /** Each call is a new take (a new random seed), even with the same words. */
    fun generate(prompt: String, seconds: Float) {
        if (_state.value is GenerateState.Running || _state.value is GenerateState.Checking) return
        val text = prompt.trim()
        if (text.isEmpty()) {
            _state.value = GenerateState.Failed("Écris d'abord ce que tu veux entendre.")
            return
        }
        if (!StableAudio.isAvailable) {
            _state.value = GenerateState.Failed("La génération n'est pas disponible sur ce téléphone.")
            return
        }
        stopPreview()
        cancelled.set(false)
        val seed = Random.nextLong(1, Long.MAX_VALUE)
        _state.value = GenerateState.Checking
        viewModelScope.launch {
            val app = getApplication<Application>()
            // The native work cannot be interrupted halfway: it always finishes (or sees the cancel flag).
            val outcome = withContext(Dispatchers.Default + NonCancellable) {
                runCatching {
                    if (!verify(app)) return@runCatching "Modèle abîmé, supprimé : retélécharge-le."
                    val memory = ActivityManager.MemoryInfo()
                    app.getSystemService(ActivityManager::class.java).getMemoryInfo(memory)
                    if (memory.availMem < NEEDED_RAM) {
                        return@runCatching "Pas assez de mémoire libre (${"%.1f".format(memory.availMem / 1e9)} Go, il en faut ${"%.1f".format(NEEDED_RAM / 1e9)}). Ferme des applis et réessaie."
                    }
                    val tokens = tokenizerFor(app).encode(text)
                    _state.value = GenerateState.Running(0f)
                    val started = System.nanoTime()
                    val audio = StableAudio.generate(GenerationModel.dir(app), tokens, seconds, seed) { p ->
                        _state.value = GenerateState.Running(p)
                        !cancelled.get()
                    }
                    if (audio == null) return@runCatching if (cancelled.get()) null else "Le modèle a échoué (mémoire ou fichiers) : réessaie."
                    AppLog.add("Génération : « ${text.take(40)} », ${seconds.roundToInt()} s en ${(System.nanoTime() - started) / 1_000_000_000} s")
                    _result.value = Generated(AudioEdits.normalize(Pcm(audio, 2, StableAudio.SAMPLE_RATE)), text, seed)
                    null
                }.getOrElse { e ->
                    AppLog.add("Génération : ${e.javaClass.simpleName} ${e.message}")
                    if (e is OutOfMemoryError) "Pas assez de mémoire : essaie un son plus court." else "Ça n'a pas marché (${e.javaClass.simpleName})."
                }
            }
            _state.value = outcome?.let { GenerateState.Failed(it) } ?: GenerateState.Idle
            if (outcome == null && _result.value != null && !cancelled.get()) play()
        }
    }

    fun cancel() = cancelled.set(true)

    fun dismissError() {
        if (_state.value is GenerateState.Failed) _state.value = GenerateState.Idle
    }

    fun discard() {
        stopPreview()
        _result.value = null
    }

    @Volatile private var verified = false

    // A corrupt model could crash ONNX Runtime: checked once per launch (a few seconds).
    private fun verify(app: Application): Boolean {
        if (verified) return true
        val ok = GenerationModel.SPECS.all { VerifiedDownload.verify(it, GenerationModel.file(app, it)) }
        if (!ok) {
            GenerationModel.remove(app)
            _installed.value = false
        }
        verified = ok
        return ok
    }

    private fun tokenizerFor(app: Application): T5Tokenizer = tokenizer ?: T5Tokenizer.fromJson(
        GenerationModel.file(app, GenerationModel.SPECS.first { it.fileName == "tokenizer.json" }).readText(),
    ).also { tokenizer = it }

    /** Plays the result on its own, outside the project (Android's player, not the engine). */
    fun play() {
        val r = _result.value ?: return
        stopPreview()
        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(
                    AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_FLOAT).setSampleRate(r.pcm.sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build(),
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(r.pcm.data.size * 4)
                .build()
        }.getOrNull() ?: return
        track.write(r.pcm.data, 0, r.pcm.data.size, AudioTrack.WRITE_BLOCKING)
        track.setNotificationMarkerPosition(r.pcm.frames)
        track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(t: AudioTrack) {
                if (player === t) stopPreview()
            }
            override fun onPeriodicNotification(t: AudioTrack) = Unit
        })
        player = track
        track.play()
        _playing.value = true
    }

    fun stopPreview() {
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
        _playing.value = false
    }

    override fun onCleared() {
        cancelled.set(true)
        stopPreview()
    }

    companion object {
        /** 11 s peaked at 1.4 GB (measured on Linux, arena off), plus margin. */
        const val NEEDED_RAM = 1_600_000_000L
    }
}
