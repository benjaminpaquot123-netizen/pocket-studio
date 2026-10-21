package com.pocketstudio.app.generate

import com.pocketstudio.model.i18n.tr

import android.content.Context
import com.pocketstudio.app.download.VerifiedFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Stable Audio Open Small (Stability AI Community License), ONNX, shrunk to int8 where it keeps the
 * sound by scripts/models/build-stable-audio.sh and hosted on our own Release with its licence.
 */
object GenerationModel {
    const val VERSION = "stable-audio-open-small-1"
    private const val BASE = "https://github.com/benjaminpaquot123-netizen/pocket-studio/releases/download/modele-$VERSION/"

    val SPECS = listOf(
        VerifiedFile(BASE + "cond_q8.onnx", "cond_q8.onnx", "31952d152e570649ef9e49882ea8a41d25a2746f2f6184c607e871b3ae636c82", 184_299_806L),
        VerifiedFile(BASE + "dit_l8.onnx", "dit_l8.onnx", "ee73eb76a354a3bb0676bd185956bb78d169f8535a0e23188212f0cc002bcf08", 359_327_993L),
        VerifiedFile(BASE + "dec.onnx", "dec.onnx", "96029859b4d563f3aff266e97e715190e8d53278c6e27ab94095535a0da4c911", 312_575_029L),
        VerifiedFile(BASE + "tokenizer.json", "tokenizer.json", "d2acde0d8d71dd30a711834b07781b9c89feaac33fd332f60507699282740066", 1_389_353L),
        VerifiedFile(BASE + "LICENSE", "LICENSE", "d6f6b1a4dce5c852bd6d7d9482d002baf0ccdb71e662250b73be9eec8764ee8d", 11_852L),
        VerifiedFile(BASE + "NOTICE", "NOTICE", "556b1b69ff89db713abcd7aebac1c5fd0e81d24161a6d4dfb4960b17939251f0", 2_130L),
    )
    val TOTAL_BYTES = SPECS.sumOf { it.sizeBytes }
    val SIZE_LABEL: String get() = tr("%d Mo", (TOTAL_BYTES + 500_000) / 1_000_000)

    fun dir(context: Context) = File(File(context.filesDir, "models"), VERSION)

    fun file(context: Context, spec: VerifiedFile) = File(dir(context), spec.fileName)

    fun isInstalled(context: Context) = SPECS.all { file(context, it).length() == it.sizeBytes }

    fun remove(context: Context) = dir(context).deleteRecursively()
}

/** What the model download is doing (written by the separation service, read by the screen). */
sealed interface ModelDownload {
    data object Idle : ModelDownload
    data class Running(val progress: Float) : ModelDownload
    data class Failed(val message: String) : ModelDownload
}

object GenerationJob {
    private val _download = MutableStateFlow<ModelDownload>(ModelDownload.Idle)
    val download: StateFlow<ModelDownload> = _download.asStateFlow()

    internal fun set(state: ModelDownload) {
        _download.value = state
    }

    /** A generation runs: with a separation at the same time, memory would run out. */
    val generating = java.util.concurrent.atomic.AtomicBoolean(false)
}
