package com.pocketstudio.bentest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipFile
import kotlin.random.Random

/** The report's zip stays under 15 MB: the texts always in it, the pictures while they fit. */
class RapportZipTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun theZipStaysUnderItsSizeWithTheTextsAlwaysIn() {
        // 20 pictures of 1 MB that do not compress (as JPEGs): 20 MB in all.
        val pictures = (1..20).map { i -> "captures/$i.jpg" to File(tmp.root, "$i.jpg").apply { writeBytes(Random(i).nextBytes(1024 * 1024)) } }
        val texts = listOf("rapport.md" to "# Rapport", "mesures.json" to "{}", "carnet.tsv" to "1\tdebut\ta\t")
        val written = RapportZip.write(File(tmp.root, "out/rapport.zip"), texts, pictures)
        assertTrue("${written.zip.length()} bytes", written.zip.length() <= RapportZip.MAX_BYTES)
        val names = ZipFile(written.zip).use { z -> z.entries().toList().map { it.name } }
        assertTrue(names.containsAll(texts.map { it.first }))
        assertTrue("some pictures in: $names", names.count { it.startsWith("captures/") } >= 10)
        assertEquals(20, names.count { it.startsWith("captures/") } + written.leftOut.size)
    }
}
