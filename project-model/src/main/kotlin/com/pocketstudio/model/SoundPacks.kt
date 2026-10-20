package com.pocketstudio.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Downloadable sound packs, one per style of [StarterWizard] (same ids): kicks, snares, hats…
 * as FLAC files plus a pack.json. The list of packs (sizes, SHA-256) is an index on our own
 * Release, itself checked by SHA-256 like the models.
 */
object SoundPacks {
    /** Sound kinds, the same ids as [StarterWizard.Role] plus the ones only the library shows. */
    val KIND_LABELS = linkedMapOf(
        "kick" to "Kicks",
        "snare" to "Caisses claires",
        "clap" to "Claps",
        "hat" to "Charleys fermés",
        "open_hat" to "Charleys ouverts",
        "perc" to "Percussions",
        "tom" to "Toms",
        "808" to "Basses 808",
        "bass" to "Basses",
        "stab" to "Stabs et hoovers",
        "pad" to "Nappes",
        "lead" to "Leads",
        "pluck" to "Plucks",
        "keys" to "Claviers",
        "vocal" to "Voix",
        "fx" to "Effets",
        "loop" to "Boucles",
    )

    private val json = Json { ignoreUnknownKeys = true }

    fun parseIndex(text: String): PackIndex = json.decodeFromString(PackIndex.serializer(), text).sanitized()

    fun parseManifest(text: String): PackManifest = json.decodeFromString(PackManifest.serializer(), text).sanitized()
}

@Serializable
data class PackIndex(val packs: List<PackEntry> = emptyList()) {
    fun sanitized() = copy(packs = packs.filter { it.isSafe }.distinctBy { it.id })
}

/** One pack on the Release: [file] is a .zip next to the index. */
@Serializable
data class PackEntry(
    val id: String,
    val name: String,
    val file: String,
    val sha256: String,
    val sizeBytes: Long,
    val sounds: Int = 0,
) {
    val isSafe: Boolean get() = PackManifest.safeName(id) && PackManifest.safeName(file) && file.endsWith(".zip") &&
        sha256.matches(Regex("[0-9a-f]{64}")) && sizeBytes in 1..MAX_BYTES

    val sizeLabel: String get() = "${((sizeBytes + 500_000) / 1_000_000).coerceAtLeast(1)} Mo"

    companion object {
        const val MAX_BYTES = 200_000_000L
    }
}

@Serializable
data class PackManifest(
    val id: String,
    val name: String,
    val family: String = "",
    val bpm: Double = 120.0,
    val sounds: List<PackSound> = emptyList(),
) {
    fun sanitized() = copy(
        name = name.take(40),
        sounds = sounds.filter { safeName(it.file) && it.kind in SoundPacks.KIND_LABELS }.map { it.copy(name = it.name.take(40)) },
    )

    fun ofKind(kind: String) = sounds.filter { it.kind == kind }

    companion object {
        /** A plain file name (never a path) from a downloaded file. */
        fun safeName(s: String) = s.length in 1..80 && s.matches(Regex("[A-Za-z0-9._-]+")) && !s.startsWith(".")
    }
}

@Serializable
data class PackSound(val file: String, val kind: String, val name: String)
