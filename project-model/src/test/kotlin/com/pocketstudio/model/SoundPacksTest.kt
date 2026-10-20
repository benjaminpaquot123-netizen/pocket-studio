package com.pocketstudio.model

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class SoundPacksTest {
    private fun resource(name: String) = javaClass.classLoader!!.getResource(name)!!.readText()

    @Test
    fun thePublishedIndexIsReadInFull() {
        // A copy of index.json from the sons-packs-3 Release, the one PackStore pins.
        val index = SoundPacks.parseIndex(resource("packs-index.json"))
        assertEquals(51, index.packs.size)
        assertTrue(index.packs.all { it.isSafe && it.sounds > 0 })
        assertTrue(
            listOf("retro_techno", "uk_garage", "detroit", "dnb", "dubstep", "tr808", "tr909", "juno", "dx7", "modern_808", "hyperpop", "fx")
                .all { id -> index.packs.any { it.id == id } },
        )
    }

    @Test
    fun aPublishedManifestKeepsEverySound() {
        val manifest = SoundPacks.parseManifest(resource("pack-retro_techno.json"))
        assertEquals("retro_techno", manifest.id)
        assertEquals(68, manifest.sounds.size)
        assertEquals(16, manifest.ofKind("kick").size)
        assertEquals(10, manifest.ofKind("stab").size)
    }

    @Test
    fun unsafeEntriesAreDropped() {
        val index = SoundPacks.parseIndex(
            """{"packs":[
              {"id":"ok","name":"Ok","file":"ok.zip","sha256":"${"a".repeat(64)}","sizeBytes":10},
              {"id":"../up","name":"Up","file":"up.zip","sha256":"${"a".repeat(64)}","sizeBytes":10},
              {"id":"big","name":"Big","file":"big.zip","sha256":"${"a".repeat(64)}","sizeBytes":999999999999},
              {"id":"ok","name":"Again","file":"ok2.zip","sha256":"${"b".repeat(64)}","sizeBytes":10}
            ]}""",
        )
        assertEquals(listOf("Ok"), index.packs.map { it.name })
        val manifest = SoundPacks.parseManifest(
            """{"id":"x","name":"X","sounds":[{"file":"a.flac","kind":"kick","name":"A"},{"file":"../b.flac","kind":"kick","name":"B"},{"file":"c.flac","kind":"laser","name":"C"}]}""",
        )
        assertEquals(listOf("a.flac"), manifest.sounds.map { it.file })
    }
}
