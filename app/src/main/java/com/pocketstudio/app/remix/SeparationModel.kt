package com.pocketstudio.app.remix

import android.content.Context
import com.pocketstudio.app.download.VerifiedFile
import java.io.File

/** HT-Demucs v4 (MIT) exported to ONNX by scripts/models/build-demucs.sh, hosted on our own Release. */
object SeparationModel {
    const val VERSION = "htdemucs-v4-onnx-1"
    /** Where the model comes from (a test points it at a stand-in server). */
    var SPEC = VerifiedFile(
        "https://github.com/benjaminpaquot123-netizen/pocket-studio/releases/download/modele-$VERSION/htdemucs.onnx",
        "htdemucs.onnx",
        "ba2224192729d6992c64a8cc440f1b9977b84519247b3f6c459c57cd35f9597d",
        174_263_526L,
    )
        internal set

    fun file(context: Context) = File(File(context.filesDir, "models"), SPEC.fileName)

    fun isInstalled(context: Context) = file(context).length() == SPEC.sizeBytes
}
