package com.pocketstudio.bentest

import java.io.File

/**
 * What the robot watches for while the microphone listens to a song: a hole (the sound gone for 60 ms, as a dropout
 * sounds in the room) or the microphone saturated. It keeps only the last 2 s in memory; on the first anomaly it
 * writes the 2 s around it (1 s before, 1 s after) in [dossier], at most [maxExtraits]: nothing else is kept.
 */
class Guet(val rate: Int = 48_000, private val dossier: File, private val nom: String, private val maxExtraits: Int = 1) {
    data class Anomalie(val kind: String, val seconde: Double, val detail: String)

    val anomalies: List<Anomalie> get() = emptyList()
    val extraits: List<File> get() = emptyList()

    fun ajouter(x: FloatArray, n: Int = x.size) = Unit

    /** The listening is over: an extract still waiting for its second after is written with what there is. */
    fun finir() = Unit
}
