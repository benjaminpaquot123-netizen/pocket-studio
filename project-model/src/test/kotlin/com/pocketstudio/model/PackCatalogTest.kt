package com.pocketstudio.model

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException

class PackCatalogTest {
    private fun entry(id: String, size: Long = 10, version: Int = 1) = PackEntry(id, id.uppercase(), "$id.zip", "a".repeat(64), size, 3, version)
    private fun manifest(id: String, version: Int = 1) = PackManifest(id, id, version = version, sounds = listOf(PackSound("a.flac", "kick", "A")))

    private val index = PackIndex(listOf(entry("house", 30, 2), entry("trap", 20), entry("bruitages", 50), entry("jazz", 5)))

    @Test
    fun downloadAllTakesWhatIsMissingOrOlderSmallestFirst() {
        val installed = listOf(manifest("house", 1), manifest("trap", 1))
        assertEquals(listOf("house"), PackCatalog.outdated(index, installed).map { it.id })
        assertEquals(listOf("jazz", "house", "bruitages"), PackCatalog.downloadAll(index, installed).map { it.id })
        assertEquals(85L, PackCatalog.totalBytes(PackCatalog.downloadAll(index, installed)))
        // Up to date: nothing to fetch.
        val all = index.packs.map { manifest(it.id, it.version) }
        assertTrue(PackCatalog.downloadAll(index, all).isEmpty())
        assertTrue(PackCatalog.complete(index, all, emptySet()).isEmpty())
    }

    @Test
    fun completeBringsUpdatesAndNewPacksButNotTheOnesDeletedOnPurpose() {
        val installed = listOf(manifest("house", 1), manifest("trap", 1))
        // « jazz » was in the index before and the user did not take it (or deleted it); « bruitages » is new.
        val seen = setOf("house", "trap", "jazz")
        assertEquals(listOf("house", "bruitages"), PackCatalog.complete(index, installed, seen).map { it.id })
        // A newer version installed than the index (an older catalogue) is not an update.
        assertTrue(PackCatalog.outdated(index, listOf(manifest("house", 3))).isEmpty())
    }

    @Test
    fun versionsAndReleasesAreReadAndChecked() {
        val parsed = SoundPacks.parseIndex(
            """{"packs":[
              {"id":"a","name":"A","file":"a.zip","sha256":"${"a".repeat(64)}","sizeBytes":10,"version":3,"release":"sons-packs-5"},
              {"id":"b","name":"B","file":"b.zip","sha256":"${"a".repeat(64)}","sizeBytes":10},
              {"id":"c","name":"C","file":"c.zip","sha256":"${"a".repeat(64)}","sizeBytes":10,"release":"../evil"},
              {"id":"d","name":"D","file":"d.zip","sha256":"${"a".repeat(64)}","sizeBytes":10,"version":0}
            ]}""",
        )
        assertEquals(listOf("a" to 3, "b" to 1), parsed.packs.map { it.id to it.version })
        assertEquals("sons-packs-5", parsed.packs[0].release)
        val m = SoundPacks.parseManifest("""{"id":"x","name":"X","version":2,"sounds":[{"file":"foley_doigts_01.flac","kind":"foley","name":"Doigts 1","tags":["doigts","snap"]}]}""")
        assertEquals(2, m.version)
        assertEquals(listOf("doigts", "snap"), m.sounds.single().tags)
    }

    @Test
    fun searchFindsSoundsByNameTagKindOrPackIgnoringAccents() {
        val foley = PackManifest(
            "bruitages_maison", "Bruitages : maison et objets",
            sounds = listOf(
                PackSound("clap_doigts_01.flac", "clap", "Claquement de doigts 1", listOf("doigts", "snap")),
                PackSound("perc_pingpong_01.flac", "perc", "Balle de ping-pong 1", listOf("ping-pong", "balle")),
                PackSound("foley_vaisselle_01.flac", "foley", "Verres et vaisselle 1", listOf("verre", "cuisine")),
            ),
        )
        val drums = PackManifest("house", "House", sounds = listOf(PackSound("kick_01.flac", "kick", "Kick 1")))
        val packs = listOf(drums, foley)
        assertEquals(listOf("clap_doigts_01.flac"), PackSearch.find("SNAP", packs).map { it.sound.file })
        assertEquals(listOf("perc_pingpong_01.flac"), PackSearch.find("ping pong", packs).map { it.sound.file })
        assertEquals(listOf("foley_vaisselle_01.flac"), PackSearch.find("vaissélle", packs).map { it.sound.file })
        assertEquals(listOf("kick_01.flac"), PackSearch.find("kicks house", packs).map { it.sound.file })
        assertEquals(3, PackSearch.find("bruitages", packs).size)
        assertTrue(PackSearch.find("  ", packs).isEmpty())
    }

    @Test
    fun theQueueRetriesThenSkipsAFailingPackAndInstallsTheOthers() {
        val calls = ArrayList<String>()
        val pauses = ArrayList<Long>()
        val queue = PackQueue(
            install = { e, progress, _ ->
                calls += e.id
                if (e.id == "trap") throw IOException("HTTP 500")
                if (e.id == "house" && calls.count { it == "house" } == 1) throw IOException("connexion coupée")
                progress(1f)
            },
            pause = { pauses += it },
        )
        val fractions = ArrayList<Float>()
        val result = queue.run(listOf(entry("house", 30), entry("trap", 20), entry("jazz", 50)), { false }) { fractions += it.fraction }
        assertEquals(listOf("house", "jazz"), result.installed)
        assertEquals(setOf("trap"), result.failed.keys)
        assertEquals(listOf("house", "house", "trap", "trap", "trap", "jazz"), calls)
        assertEquals(listOf(2_000L, 2_000L, 8_000L), pauses)
        assertEquals(1f, fractions.last())
        assertTrue("progress only grows", fractions.zipWithNext().all { (a, b) -> b >= a })
    }

    @Test
    fun aCancelStopsTheQueueWithoutRetrying() {
        var cancelled = false
        val calls = ArrayList<String>()
        val queue = PackQueue(install = { e, _, _ ->
            calls += e.id
            cancelled = true
            throw IOException("annulé")
        }, pause = { error("no retry after a cancel") })
        val result = queue.run(listOf(entry("a"), entry("b")), { cancelled }) {}
        assertTrue(result.cancelled)
        assertEquals(listOf("a"), calls)
        assertTrue(result.installed.isEmpty() && result.failed.isEmpty())
    }

    /** The packs the CI builds from free libraries: kinds the app keeps, safe ids, names translated. */
    @Test
    fun theLibraryPacksMatchTheApp() {
        val root = Json.parseToJsonElement(File("../scripts/packs/libraries.json").readText()).jsonObject
        val sources = root["sources"]!!.jsonArray.map { it.jsonObject["id"]!!.jsonPrimitive.content }.toSet()
        val packs = root["packs"]!!.jsonArray.map { it.jsonObject }
        assertTrue(packs.size >= 5)
        for (p in packs) {
            val id = p["id"]!!.jsonPrimitive.content
            assertTrue(id, PackManifest.safeName(id))
            assertTrue(id, p["version"]!!.jsonPrimitive.int >= 1)
            assertTrue(id, p["sources"]!!.jsonArray.all { it.jsonPrimitive.content in sources })
            for (r in p["rules"]!!.jsonArray.map { it.jsonObject }) {
                val kind = r["kind"]!!.jsonPrimitive.content
                assertTrue("$id: $kind", kind in SoundPacks.KIND_LABELS)
                val file = "${kind}_${r["slug"]!!.jsonPrimitive.content}_99.flac"
                assertTrue(file, PackManifest.safeName(file))
            }
            val name = p["name"]!!.jsonPrimitive.content
            for (lang in listOf(Lang.EN, Lang.NL)) assertTrue("$name ($lang)", I18n.table(lang)[name].orEmpty().isNotEmpty())
        }
        // Every kind the generated packs give their sounds is one the app keeps too.
        val styles = Json.parseToJsonElement(File("../scripts/packs/styles.json").readText()).jsonObject
        for ((k, v) in styles["kinds"]!!.jsonObject) {
            val kind = v.jsonObject["as"]?.jsonPrimitive?.content ?: k
            assertTrue(kind, kind in SoundPacks.KIND_LABELS)
        }
    }
}
