package com.pocketstudio.app.instruments

import com.pocketstudio.model.i18n.tr

import android.content.Context
import com.pocketstudio.app.download.VerifiedDownload
import com.pocketstudio.app.download.VerifiedFile
import java.io.File

/** A downloadable SoundFont bank, hosted on our own GitHub Release and pinned by SHA-256. */
data class Bank(
    val id: String,
    val name: String,
    val description: String,
    val tag: String,
    val fileName: String,
    val sha256: String,
    val sizeBytes: Long,
) {
    val url get() = "https://github.com/benjaminpaquot123-netizen/pocket-studio/releases/download/$tag/$fileName"
    val sizeLabel get() = tr("%d Mo", sizeBytes / 1_000_000)
    val spec get() = VerifiedFile(url, fileName, sha256, sizeBytes)
}

/**
 * The instrument banks (General MIDI, free licences): each is downloaded once from our own
 * GitHub Release, checked against its SHA-256 before it is ever parsed, and kept in the app's files.
 */
object InstrumentLibrary {
    /** Built on each read, so the descriptions follow the interface language. */
    val BANKS get() = listOf(
        Bank(
            "generaluser", "GeneralUser GS", tr("Léger et équilibré : 287 sons, 13 kits de batterie."),
            "sons-generaluser-gs-2.0.3", "GeneralUser-GS.sf2",
            "9575028c7a1f589f5770fccc8cff2734566af40cd26ed836944e9a5152688cfe", 32_319_396L,
        ),
        Bank(
            "fluidr3", "FluidR3 GM", tr("Le classique : 189 sons, 31 kits de batterie."),
            "sons-fluidr3-gm-3.1", "FluidR3_GM.sf2",
            "74594e8f4250680adf590507a306655a299935343583256f3b722c48a1bc1cb0", 148_398_306L,
        ),
        Bank(
            "musescore", "MuseScore General", tr("Le plus riche : 309 sons, 37 kits de batterie."),
            "sons-musescore-general-0.2", "MuseScore_General.sf2",
            "ee51d2c4b1525e70f19a45909c4fd7a2e26d91d115fa89dbf5a6bc413d8b9bf3", 215_614_036L,
        ),
    )

    fun bank(id: String) = BANKS.firstOrNull { it.id == id } ?: BANKS[0]

    fun file(context: Context, bank: Bank) = File(File(context.filesDir, "sounds"), bank.fileName)

    fun isInstalled(context: Context, bank: Bank) = file(context, bank).length() == bank.sizeBytes

    /** Blocking (background). [onProgress] 0..1. Throws on network error or a wrong file. */
    fun download(context: Context, bank: Bank, onProgress: (Float) -> Unit, isCancelled: () -> Boolean = { false }): File =
        VerifiedDownload.fetch(bank.spec, file(context, bank), onProgress, isCancelled)

    /** Re-checks an installed file before parsing it. */
    fun verify(context: Context, bank: Bank): Boolean = VerifiedDownload.verify(bank.spec, file(context, bank))

    /** The 16 General MIDI families (8 programs each), in French (SoundLooks keys; shown through tr). */
    val FAMILIES = listOf(
        "Pianos", "Percussions mélodiques", "Orgues", "Guitares", "Basses", "Cordes solo", "Ensembles", "Cuivres",
        "Anches", "Flûtes", "Synthé lead", "Synthé nappe", "Effets synthé", "Ethnique", "Percussions", "Bruitages",
    )

    /** General MIDI drum notes for our 16 pads, in pad order (DrumKitInfo.names). */
    val KIT_NOTES = listOf(36, 35, 38, 40, 39, 42, 46, 41, 45, 48, 37, 56, 49, 70, 63, 54)
}
