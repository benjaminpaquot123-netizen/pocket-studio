package com.pocketstudio.model

import com.pocketstudio.model.i18n.tr
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
        "ride" to "Cymbales",
        "808" to "Basses 808",
        "bass" to "Basses",
        "stab" to "Stabs et hoovers",
        "pad" to "Nappes",
        "lead" to "Leads",
        "pluck" to "Plucks",
        "keys" to "Claviers",
        "mallet" to "Percussions mélodiques",
        "vocal" to "Voix",
        "foley" to "Bruitages",
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

/**
 * One pack on a Release: [file] is a .zip on [release] (empty: the release of the index itself).
 * [version] rises when the pack is rebuilt with more or better sounds: the app then offers it again.
 */
@Serializable
data class PackEntry(
    val id: String,
    val name: String,
    val file: String,
    val sha256: String,
    val sizeBytes: Long,
    val sounds: Int = 0,
    val version: Int = 1,
    val release: String = "",
    val family: String = "",
) {
    val isSafe: Boolean get() = PackManifest.safeName(id) && PackManifest.safeName(file) && file.endsWith(".zip") &&
        sha256.matches(Regex("[0-9a-f]{64}")) && sizeBytes in 1..MAX_BYTES && version >= 1 &&
        (release.isEmpty() || release.matches(Regex("sons-packs(-[0-9]{1,4})?")))

    val sizeLabel: String get() = tr("%d Mo", ((sizeBytes + 500_000) / 1_000_000).coerceAtLeast(1))

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
    /** The [PackEntry.version] installed (packs built before versions are 1). */
    val version: Int = 1,
) {
    fun sanitized() = copy(
        name = name.take(40),
        sounds = sounds.filter { safeName(it.file) && it.kind in SoundPacks.KIND_LABELS }
            .map { it.copy(name = it.name.take(40), tags = it.tags.take(8).map { t -> t.take(30) }) },
    )

    /** The pack's sounds of [kind], best first (see [PackPicks]). */
    fun ofKind(kind: String) = PackPicks.sorted(id, kind, sounds.filter { it.kind == kind }) { it.file }

    companion object {
        /** A plain file name (never a path) from a downloaded file. */
        fun safeName(s: String) = s.length in 1..80 && s.matches(Regex("[A-Za-z0-9._-]+")) && !s.startsWith(".")
    }
}

/** [tags]: words the library's search finds the sound by (« doigts », « verre »…), in French. */
@Serializable
data class PackSound(val file: String, val kind: String, val name: String, val tags: List<String> = emptyList())
