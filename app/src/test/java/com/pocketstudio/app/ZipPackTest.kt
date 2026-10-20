package com.pocketstudio.app

import com.pocketstudio.app.rhythm.ZipPack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipPackTest {
    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { z ->
            entries.forEach { (name, data) ->
                z.putNextEntry(ZipEntry(name))
                z.write(data)
                z.closeEntry()
            }
        }
        return bytes.toByteArray()
    }

    private val exts = setOf("wav", "mp3")

    @Test
    fun keepsOnlyAudioUnderSafeNames() {
        val root = Files.createTempDirectory("zp").toFile()
        val dir = File(root, "pack")
        val data = zip(
            "Kit/Snare.wav" to byteArrayOf(1, 2), "Kit/kick.WAV" to byteArrayOf(3), "readme.txt" to byteArrayOf(4),
            "../../evil.wav" to byteArrayOf(5), "__MACOSX/Kit/._kick.wav" to byteArrayOf(6), "Kit/" to ByteArray(0),
        )
        val files = ZipPack.extractAudio(data.inputStream(), dir, 16, exts)
        assertEquals(listOf("evil.wav", "kick.WAV", "Snare.wav"), files.map { it.second })
        assertTrue(files.all { it.first.parentFile == dir })
        assertFalse(File(root.parentFile, "evil.wav").exists())
        assertEquals(3, files.first { it.second == "kick.WAV" }.first.readBytes().single().toInt())
        assertEquals(2, ZipPack.extractAudio(data.inputStream(), dir, 2, exts).size)
        root.deleteRecursively()
    }
}
