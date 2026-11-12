package com.pocketstudio.bentest

import java.io.File

/** The phone's microphone (the real one in [MicroReel]). */
interface Micro {
    val rate: Int

    /** RECORD_AUDIO granted. */
    fun autorise(): Boolean

    /** Listens [ms], handing each block to [recevoir] (samples, count); the blocks are not kept. */
    fun ecouter(ms: Long, recevoir: (FloatArray, Int) -> Unit)
}

/** The engine playing a sound of the robot's (the sweep) and the phone's music volume. */
interface HautParleur {
    /** [pcm] (mono, 48 kHz) played by the engine; returns once it plays. */
    fun jouer(pcm: FloatArray)
    fun arreter()

    /** The music volume set for the measure (kept to be put back). */
    fun reglerVolume()
    fun remettreVolume()
}

/**
 * « Écoute au micro » (docs/PLAN-TEST-APK.md, T1b, about 5 minutes, the phone lying in a quiet room): the room's
 * noise, the speaker's curve (a sweep), then 4 songs (jump up, house, trap, techno) heard by the microphone, each
 * reduced to its energy per band. Nothing is recorded but 2 s around an anomaly ([Guet]). Microphone refused: every
 * step « passée ».
 */
class EcouteMicro(
    private val prep: Preparateur,
    private val micro: Micro,
    private val hp: HautParleur,
    private val carnet: Carnet,
    private val sons: File,
    private val ecouteMs: Long = 30_000,
    private val bruitMs: Long = 4_000,
    private val balayageS: Double = 12.0,
    private val styles: List<String> = STYLES,
) {
    fun etapes(): List<Etape> = emptyList()

    companion object {
        val STYLES = listOf("jump_up", "house", "trap", "techno")
        const val REFUSE = "passée : micro refusé"

        /** rapport.md's section. */
        fun section(lines: List<Carnet.Ligne>): String = ""

        /** mesures.json's « micro » object (null: no such step in the logbook). */
        fun json(lines: List<Carnet.Ligne>): String? = null

        /** The speaker's curve the logbook holds (dB per third-octave, [Tiers.NOMS]); empty: none. */
        fun courbe(lines: List<Carnet.Ligne>): List<Double?> = emptyList()

        /** Each style's fingerprint the logbook holds (dB per octave, [Empreinte.OCTAVES]). */
        fun empreintes(lines: List<Carnet.Ligne>): Map<String, List<Double?>> = emptyMap()

        /**
         * « Écoute au micro » touched, its sentence read, « Commencer »: the microphone asked for now if it is not
         * granted ([demander], whose answer then calls [lancer]), else the run started at once.
         */
        fun demarrer(autorise: Boolean, demander: () -> Unit, lancer: () -> Unit) = Unit
    }
}
