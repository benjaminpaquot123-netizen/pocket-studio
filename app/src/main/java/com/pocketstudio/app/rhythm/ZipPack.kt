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
     * Blocking. Writes at most [maxFiles] audio files of [zip] into [dir] (created, emptied first)
     * and returns them with their original names, sorted by name.
     */
    fun extractAudio(zip: InputStream, dir: File, maxFiles: Int, extensions: Set<String>): List<Pair<File, String>> {
        dir.deleteRecursively()
        dir.mkdirs()
        val out = ArrayList<Pair<File, String>>()
        var total = 0L
        ZipInputStream(zip.buffered()).use { z ->
            while (out.size < maxFiles) {
                val entry = z.nextEntry ?: break
                if (entry.isDirectory) continue
                // Only the last part of the path, never "../" or folders: no writing outside `dir`.
                val name = entry.name.substringAfterLast('/').substringAfterLast('\\')
                if (name.isEmpty() || name.startsWith(".") || name.startsWith("__MACOSX")) continue
                if (entry.name.contains("__MACOSX")) continue
                val ext = name.substringAfterLast('.', "").lowercase()
                if (ext !in extensions) continue
                val target = File(dir, "%03d.%s".format(out.size, ext))
                var size = 0L
                target.outputStream().use { o ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = z.read(buf)
                        if (n < 0) break
                        size += n
                        total += n
                        if (size > MAX_ENTRY_BYTES || total > MAX_TOTAL_BYTES) {
                            o.close()
                            dir.deleteRecursively()
                            throw TooBigException()
                        }
                        o.write(buf, 0, n)
                    }
                }
                out += target to name
            }
        }
        val sorted = out.sortedBy { it.second.lowercase() }
        return sorted
    }
}
