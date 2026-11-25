package com.pocketstudio.bentest

import com.pocketstudio.bentest.Scenarios.Companion.NIVEAU_DEBUTANT
import com.pocketstudio.bentest.Scenarios.Companion.NIVEAU_MOYEN
import com.pocketstudio.bentest.Scenarios.Companion.NIVEAU_PRO
import com.pocketstudio.bentest.Scenarios.Scenario

/**
 * The big catalogue's mix theme (2026-11-25): each strip's sliders pushed to both ends, and the mute and solo
 * of the strips the table of [Scenarios] leaves out. Each one starts from a clean project and reads its oracle
 * in the project and on the disk; a song without that strip is « non mesuré ».
 */
object ScenariosMix {
    /** The strips tried: a song's eight first (instruments, presets, sampler, drums, bass, synth). */
    const val PISTES = 8

    /** The slider ends, as the « mix » gesture names them. */
    val BOUTS = listOf(
        Triple("vol_max", "le volume au maximum", "le volume de la piste monté au bout, dans le projet et sur le disque"),
        Triple("vol_min", "le volume à zéro", "le volume de la piste descendu à zéro, dans le projet et sur le disque"),
        Triple("pan_g", "tout à gauche", "le panoramique de la piste à gauche, dans le projet et sur le disque"),
        Triple("pan_d", "tout à droite", "le panoramique de la piste à droite, dans le projet et sur le disque"),
    )

    val LISTE: List<Scenario> = buildList {
        for (ch in 0 until PISTES) for ((bout, quoi, oracle) in BOUTS) {
            val niveau = if (bout.startsWith("vol")) NIVEAU_DEBUTANT else NIVEAU_MOYEN
            add(Scenario("mix_piste${ch + 1}_$bout", "Mix : la piste ${ch + 1}, $quoi", niveau, 90_000, 6, oracle, "mix:$bout:$ch"))
        }
        // The table covers mute on strips 2 to 4 and solo on 1, 3, 4 (the quick pack: mute 1, solo 2).
        for (ch in 4 until PISTES) add(Scenario("mix_piste${ch + 1}_muet", "Mix : couper la piste ${ch + 1} (Muet)", NIVEAU_DEBUTANT, 90_000, 6, "le « muet » de la piste dans le projet et sur le disque", "muet:$ch"))
        for (ch in 4 until PISTES) add(Scenario("mix_piste${ch + 1}_solo", "Mix : la piste ${ch + 1} en solo", NIVEAU_PRO, 90_000, 6, "le « solo » de la piste dans le projet et sur le disque", "solo:$ch"))
    }
}
