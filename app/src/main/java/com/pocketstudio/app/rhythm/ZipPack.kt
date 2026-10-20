package com.pocketstudio.app.rhythm

import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Sample packs shipped as a .zip: the audio files inside (any folder) are copied out under
 * plain names; nothing else is written. Guards against archives that would fill the phone.
 */
object ZipPack {
    const val MAX_ENTRY_BYTES = 64L * 1024 * 1024
    const val MAX_TOTAL_BYTES = 256L * 1024 * 1024

    class TooBigException : Exception("archive trop grosse")

    /**
     * Blocking. Writes into [dir] (created, emptied first) the first [maxFiles] audio files of [zip]
     * in name order (not archive order: a pack lists the same way on every phone), and returns them
     * with their original names, sorted by name.
     */
    fun extractAudio(zip: InputStream, dir: File, maxFiles: Int, extensions: Set<String>): List<Pair<File, String>> {
        dir.deleteRecursively()
        dir.mkdirs()
        val kept = ArrayList<Pair<File, String>>()
        var keptBytes = 0L
        var count = 0
        val byName = compareBy<Pair<File, String>> { it.second.lowercase() }
        ZipInputStream(zip.buffered()).use { z ->
            while (maxFiles > 0) {
                val entry = z.nextEntry ?: break
                if (entry.isDirectory) continue
                // Only the last part of the path, never "../" or folders: no writing outside `dir`.
                val name = entry.name.substringAfterLast('/').substringAfterLast('\\')
                if (name.isEmpty() || name.startsWith(".") || name.startsWith("__MACOSX")) continue
                if (entry.name.contains("__MACOSX")) continue
                val ext = name.substringAfterLast('.', "").lowercase()
                if (ext !in extensions) continue
                // Full and this one comes after every kept name: not needed.
                if (kept.size >= maxFiles && name.lowercase() >= kept.maxWith(byName).second.lowercase()) continue
                val target = File(dir, "%03d.%s".format(count++, ext))
                var size = 0L
                target.outputStream().use { o ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = z.read(buf)
                        if (n < 0) break
                        size += n
                        if (size > MAX_ENTRY_BYTES || keptBytes + size > MAX_TOTAL_BYTES) {
                            o.close()
                            dir.deleteRecursively()
                            throw TooBigException()
                        }
                        o.write(buf, 0, n)
                    }
                }
                kept += target to name
                keptBytes += size
                if (kept.size > maxFiles) {
                    val last = kept.maxWith(byName)
                    keptBytes -= last.first.length()
                    last.first.delete()
                    kept.remove(last)
                }
            }
        }
        return kept.sortedWith(byName)
    }
}
