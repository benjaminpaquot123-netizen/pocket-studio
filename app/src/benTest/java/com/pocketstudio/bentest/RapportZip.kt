package com.pocketstudio.bentest

import java.io.File
import java.io.FileOutputStream
import java.io.FilterOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * BenStudio Test's report as one zip, under [MAX_BYTES] (docs/PLAN-TEST-APK.md, « Le rapport »):
 * the texts always (rapport.md, mesures.json, the logbook, the journal), then the pictures while
 * they fit. It is only written here: Benjamin sends it himself (« Envoyer le rapport »).
 */
object RapportZip {
    /** 15 MB, what a mail or a chat takes. */
    const val MAX_BYTES = 15L * 1024 * 1024

    /** What was written: the zip and the files left out for room. */
    data class Ecrit(val zip: File, val leftOut: List<String>)

    fun write(out: File, texts: List<Pair<String, String>>, files: List<Pair<String, File>>, maxBytes: Long = MAX_BYTES): Ecrit {
        out.parentFile?.mkdirs()
        val tmp = File(out.parentFile, out.name + ".tmp")
        val leftOut = ArrayList<String>()
        val counted = Counting(FileOutputStream(tmp))
        ZipOutputStream(counted).use { zip ->
            texts.forEach { (name, body) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(body.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            // Room kept for each entry's headers and the zip's directory at the end.
            val room = maxBytes - 64 * 1024
            files.filter { it.second.isFile }.forEach { (name, file) ->
                zip.flush()
                if (counted.count + file.length() + 1024 > room) {
                    leftOut += name
                    return@forEach
                }
                zip.putNextEntry(ZipEntry(name))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        out.delete()
        check(tmp.renameTo(out)) { "zip not moved" }
        return Ecrit(out, leftOut)
    }

    private class Counting(out: OutputStream) : FilterOutputStream(out) {
        var count = 0L
        override fun write(b: Int) {
            out.write(b)
            count++
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            out.write(b, off, len)
            count += len
        }
    }
}
