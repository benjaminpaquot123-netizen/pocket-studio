package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Audit étage 0 and §3: the test songs of the repository are synthetic ([SyntheticSongs]) and frozen.
 * `PS_REGENERATE=1 ./gradlew :project-model:test --tests '*SyntheticSongsTest*'` writes them again
 * (then look at what changed: the tests that read them say why they matter); otherwise each file
 * is only checked: readable, in shape, its sounds in its folder.
 */
class SyntheticSongsTest {
    private val root = File("..").canonicalFile
    private val draftSamples = File(root, "$REAL/brouillon-jumpup/samples")

    private fun seconds(file: String): Double {
        val b = File(draftSamples, file).readBytes()
        fun le(at: Int, n: Int) = (0 until n).fold(0L) { acc, k -> acc or ((b[at + k].toLong() and 0xff) shl (8 * k)) }
        var at = 12
        var rate = 48000L
        var frameBytes = 2L
        while (at + 8 <= b.size) {
            val id = String(b, at, 4, Charsets.US_ASCII)
            val size = le(at + 4, 4)
            if (id == "fmt ") { rate = le(at + 12, 4); frameBytes = le(at + 20, 2) }
            if (id == "data") return Math.round(size.toDouble() / frameBytes / rate * 1000.0) / 1000.0
            at += 8 + size.toInt() + (size.toInt() and 1)
        }
        error("no data in $file")
    }

    private val outputs: List<Pair<String, () -> Project>> = listOf(
        "project-model/src/test/resources/guide/brouillon-jumpup.json" to SyntheticSongs::jumpUpDraft,
        "$REAL/brouillon-jumpup/project.json" to SyntheticSongs::jumpUpDraft,
        "$REFERENCE/vide.json" to SyntheticSongs::empty,
        "$REFERENCE/petit/project.json" to SyntheticSongs::small,
        "$REFERENCE/enorme/project.json" to { SyntheticSongs.huge(::seconds) },
    )

    private fun read(path: String): Project = ProjectJson.decode(File(root, path).readText())

    @Test
    fun theTestSongsAreThereAndRegenerateOnDemand() {
        for ((path, make) in outputs) {
            val file = File(root, path)
            if (System.getenv("PS_REGENERATE") == "1") {
                file.parentFile.mkdirs()
                file.writeText(ProjectJson.encode(make()))
            }
            assertTrue("$path is missing: PS_REGENERATE=1", file.isFile)
            // Read, written and read again: the same project (nothing lost on a save).
            val p = read(path)
            assertEquals(path, p, ProjectJson.decode(ProjectJson.encode(p)))
        }
    }

    @Test
    fun theReferenceProjectsHaveTheirShape() {
        val empty = read("$REFERENCE/vide.json")
        assertTrue(empty.song.isEmpty() && empty.tracks.isEmpty() && empty.audioTracks.isEmpty() && empty.allScenes.all { it.isEmpty })
        val small = read("$REFERENCE/petit/project.json")
        assertTrue("${small.songBars} bars", small.songBars in 4..16)
        val huge = read("$REFERENCE/enorme/project.json")
        assertEquals(100, huge.songBars)
        assertEquals(16, huge.partKeys.size)
        assertEquals(LivePads.MAX, huge.live.clips.count { it != null })
        assertEquals(DrumPattern.PAD_COUNT, huge.drums.lanes.count { it.sample != null })
        assertTrue(huge.audioTracks.all { it.clip != null && it.clip!!.seconds > 0.0 })
    }

    @Test
    fun everySoundTheyNameIsInTheirFolderAndComesFromTheAppsLibrary() {
        val library = SyntheticSongs.SAMPLES.map { "${it.first}.wav" }.toSet()
        for (dir in listOf("$REAL/brouillon-jumpup", "$REFERENCE/petit", "$REFERENCE/enorme")) {
            val p = read("$dir/project.json")
            val named = (p.drums.lanes.mapNotNull { it.sample?.file } + p.tracks.mapNotNull { it.sampled.file } +
                p.audioTracks.mapNotNull { it.clip?.file } + p.live.clips.mapNotNull { it?.file }).toSet()
            val files = File(root, "$dir/samples").listFiles()?.map { it.name }?.toSet().orEmpty()
            assertEquals(dir, named, files)
            assertTrue(dir, library.containsAll(files))
        }
    }

    private companion object {
        const val REAL = "app/src/test/resources/real-projects"
        const val REFERENCE = "app/src/test/resources/reference-projects"
    }
}
