package com.pocketstudio.bentest

import com.pocketstudio.bentest.Scenarios.Scenario
import java.util.Locale

/**
 * The catalogue's « save, projects and export » theme (tour 9, docs/audits/ultra/PLAN-APRES-ROBOT.md): exports of
 * styles no other scenario exports, each in the format that shows its own risk (WAV: the peak kept bit for bit,
 * so a style's clipping; FLAC: the advanced chip and the lossless encoder; M4A: the encoder's own delay and length).
 * Every one reuses the « export » gesture (a new song of the style by the assistant, then Mix › Exporter, the file
 * read back from the disk: its length against the app's, its true peak, neither silence nor saturation, the time taken).
 */
object ScenariosSauve {
    /** The styles exported, by format: the loudest ones in WAV, where a saturation is not hidden by an encoder. */
    val EXPORTS: List<Pair<String, String>> = listOf(
        "hard_techno" to "wav", "gabber" to "wav", "riddim" to "wav", "neurofunk" to "wav",
        "industrial_techno" to "wav", "phonk" to "wav", "jump_up" to "wav",
        "acid_techno" to "flac", "drill" to "flac", "jungle" to "flac", "amapiano" to "flac", "synthwave" to "flac",
        "deep_house" to "m4a", "disco" to "m4a", "afro_house" to "m4a", "reggaeton" to "m4a", "grime" to "m4a", "uk_garage" to "m4a",
    )

    private fun niveau(format: String) = when (format) {
        "wav" -> Scenarios.NIVEAU_MESURE
        "flac" -> Scenarios.NIVEAU_PRO
        else -> Scenarios.NIVEAU_MOYEN
    }

    private fun qui(niveau: String) = when (niveau) {
        Scenarios.NIVEAU_PRO -> "Pro"
        Scenarios.NIVEAU_MOYEN -> "Moyen"
        else -> "Mesure"
    }

    val LISTE: List<Scenario> = EXPORTS.map { (st, format) ->
        val n = niveau(format)
        Scenario(
            "sav_export_${st}_$format",
            "${qui(n)} : un morceau ${st.replace('_', ' ')} exporté en ${format.uppercase(Locale.ROOT)}",
            n, 600_000, 75,
            "le fichier relu sur le disque : durée contre celle de l'appli, crête (ni silence ni saturation), temps pris",
            "export:$st:$format",
        )
    }
}
