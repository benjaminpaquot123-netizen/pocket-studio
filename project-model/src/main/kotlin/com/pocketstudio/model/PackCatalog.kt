package com.pocketstudio.model

import java.text.Normalizer

/**
 * What « Tout télécharger » and « Tout compléter » fetch, from the published index and the packs
 * on the phone. A pack is outdated when the index has a higher [PackEntry.version] than the one
 * installed; new when the user has never seen it in the index ([seen]: ids already offered, so a
 * pack they deleted on purpose is not brought back by « Tout compléter »).
 */
object PackCatalog {
    fun outdated(index: PackIndex, installed: List<PackManifest>): List<PackEntry> {
        val have = installed.associate { it.id to it.version }
        return index.packs.filter { e -> have[e.id]?.let { it < e.version } == true }
    }

    fun missing(index: PackIndex, installed: List<PackManifest>): List<PackEntry> =
        index.packs.filter { e -> installed.none { it.id == e.id } }

    /** Every pack not on the phone or older there, smallest first (the first ones are heard sooner). */
    fun downloadAll(index: PackIndex, installed: List<PackManifest>): List<PackEntry> =
        (outdated(index, installed) + missing(index, installed)).sortedBy { it.sizeBytes }

    /** Updates of the installed packs, and the packs published since the user last looked. */
    fun complete(index: PackIndex, installed: List<PackManifest>, seen: Set<String>): List<PackEntry> =
        (outdated(index, installed) + missing(index, installed).filter { it.id !in seen }).sortedBy { it.sizeBytes }

    fun totalBytes(entries: List<PackEntry>): Long = entries.sumOf { it.sizeBytes }
}

/** The library's search: a sound of an installed pack, by its name, tags, kind or pack (accents and case ignored). */
object PackSearch {
    data class Hit(val pack: PackManifest, val sound: PackSound)

    fun find(query: String, installed: List<PackManifest>, limit: Int = 60): List<Hit> {
        val words = fold(query).split(' ').filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()
        val hits = ArrayList<Hit>()
        for (m in installed.sortedBy { it.name }) {
            val packText = fold(m.name)
            for (s in m.sounds) {
                val text = fold(listOf(s.name, s.tags.joinToString(" "), SoundPacks.KIND_LABELS[s.kind].orEmpty(), s.kind).joinToString(" ")) + " " + packText
                if (words.all { it in text }) {
                    hits += Hit(m, s)
                    if (hits.size >= limit) return hits
                }
            }
        }
        return hits
    }

    fun fold(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").replace(Regex("[^a-z0-9]+"), " ").trim()
}

/**
 * Downloads a list of packs one after the other: a pack that fails is tried again ([attempts],
 * waiting a little longer each time), then skipped so the others still come; a cancel stops the
 * queue. [install] is blocking and leaves nothing half-installed when it throws (PackStore.install).
 */
class PackQueue(
    private val install: (PackEntry, onProgress: (Float) -> Unit, isCancelled: () -> Boolean) -> Unit,
    private val attempts: Int = 3,
    private val pause: (Long) -> Unit = { Thread.sleep(it) },
) {
    /** [done] packs finished (installed or failed) of [count]; [fraction] of all the bytes. */
    data class Progress(val done: Int, val count: Int, val current: PackEntry?, val fraction: Float)

    data class Result(val installed: List<String>, val failed: Map<String, String>, val cancelled: Boolean)

    fun run(entries: List<PackEntry>, isCancelled: () -> Boolean, onProgress: (Progress) -> Unit): Result {
        val total = PackCatalog.totalBytes(entries).coerceAtLeast(1)
        var before = 0L
        val installed = ArrayList<String>()
        val failed = LinkedHashMap<String, String>()
        entries.forEachIndexed { i, e ->
            if (isCancelled()) return Result(installed, failed, true)
            onProgress(Progress(i, entries.size, e, before.toFloat() / total))
            var error: Throwable? = null
            for (attempt in 1..attempts) {
                error = runCatching {
                    install(e, { p -> onProgress(Progress(i, entries.size, e, (before + p * e.sizeBytes) / total)) }, isCancelled)
                }.exceptionOrNull()
                if (error == null || isCancelled()) break
                if (attempt < attempts) pause(2_000L * attempt * attempt)
            }
            if (isCancelled()) return Result(installed, failed, true)
            if (error == null) installed += e.id else failed[e.id] = error.message ?: error.javaClass.simpleName
            before += e.sizeBytes
        }
        onProgress(Progress(entries.size, entries.size, null, 1f))
        return Result(installed, failed, false)
    }
}
