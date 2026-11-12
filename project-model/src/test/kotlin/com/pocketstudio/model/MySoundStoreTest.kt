package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** « Ma bibliothèque de sons »: kept, renamed, filed, deleted, and never lost to a bad write. */
class MySoundStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun store() = MySoundStore(File(tmp.root, MySoundStore.DIR))

    private fun keep(store: MySoundStore, name: String, kind: String = "vocal", bytes: Int = 64): MySound {
        val f = store.newFile()
        f.writeBytes(ByteArray(bytes) { it.toByte() })
        return store.add(MySound(f.nameWithoutExtension, name, kind, f.name, seconds = 2.0, bpm = 98.0, root = 9, minor = true, createdAt = 1L))
    }

    @Test
    fun aKeptSoundIsListedWithItsFileTempoAndKey() {
        val s = store()
        val kept = keep(s, "Voix · Ma chanson")
        val again = MySoundStore(s.dir).list().single()
        assertEquals(kept, again)
        assertEquals(98.0, again.bpm, 0.0)
        assertEquals(9, again.root)
        assertTrue(again.minor)
        assertEquals(64L, s.file(again)!!.length())
    }

    @Test
    fun twoSoundsNeverShareAName() {
        val s = store()
        keep(s, "Voix")
        assertEquals("Voix 2", keep(s, "Voix").name)
        assertEquals("Voix 3", keep(s, "Voix").name)
    }

    @Test
    fun renameAndFamilyAreKeptAndCheckedButTheFileStays() {
        val s = store()
        val a = keep(s, "Voix")
        keep(s, "Refrain")
        assertEquals("Refrain 2", s.rename(a.id, "  Refrain ")!!.name)
        assertEquals("loop", s.setKind(a.id, "loop")!!.kind)
        // A kind the app does not know falls back to one it does.
        assertEquals(MySounds.DEFAULT_KIND, s.setKind(a.id, "nimporte")!!.kind)
        assertEquals("x".repeat(MySounds.MAX_NAME), s.rename(a.id, "x".repeat(90))!!.name)
        assertEquals(a.file, s.list().first { it.id == a.id }.file)
        assertNull(s.rename("absent", "Nom"))
    }

    @Test
    fun deleteTakesTheEntryAndItsFile() {
        val s = store()
        val a = keep(s, "Voix")
        val b = keep(s, "Boucle", "loop")
        val file = s.file(a)!!
        assertTrue(s.delete(a.id))
        assertFalse(file.exists())
        assertEquals(listOf(b), s.list())
        assertFalse(s.delete(a.id))
    }

    @Test
    fun aPathNeverComesFromTheIndex() {
        val s = store()
        val f = s.newFile()
        f.writeBytes(ByteArray(4))
        val bad = runCatching { s.add(MySound("../evil", "x", "fx", "../evil.wav")) }
        assertTrue(bad.isFailure)
        // An index edited by hand with a path: that entry is ignored.
        File(s.dir, MySoundStore.INDEX).writeText("""{"sounds":[{"id":"a","name":"A","kind":"fx","file":"../../x.wav"},{"id":"b","name":"B","kind":"fx","file":"${f.name}"}]}""")
        assertEquals(listOf("B"), s.list().map { it.name })
    }

    @Test
    fun aSoundWithoutItsFileIsNeverAdded() {
        val s = store()
        val r = runCatching { s.add(MySound("abc", "Voix", "vocal", "abc.wav")) }
        assertTrue(r.isFailure)
        assertTrue(s.list().isEmpty())
    }

    @Test
    fun anUnreadableIndexIsSetAsideNotOverwritten() {
        val s = store()
        keep(s, "Voix")
        File(s.dir, MySoundStore.INDEX).writeText("{ pas du json")
        val fresh = MySoundStore(s.dir)
        assertTrue(fresh.list().isEmpty())
        val aside = s.dir.listFiles()!!.single { it.name.startsWith("index-illisible") }
        assertEquals("{ pas du json", aside.readText())
        // New sounds go on in a new index; the old one stays aside.
        keep(fresh, "Boucle", "loop")
        assertEquals(listOf("Boucle"), fresh.list().map { it.name })
        assertTrue(aside.exists())
    }

    @Test
    fun aHalfWrittenIndexLeavesTheOldOneWhole() {
        val s = store()
        keep(s, "Voix")
        // A write stopped before its rename leaves only a .tmp: the index read is the last whole one.
        File(s.dir, MySoundStore.INDEX + ".tmp").writeText("{\"sounds\":[")
        assertEquals(listOf("Voix"), MySoundStore(s.dir).list().map { it.name })
    }

    @Test
    fun cleanUpRemovesOnlyOldFilesNobodyLists() {
        val s = store()
        val kept = keep(s, "Voix")
        val orphan = s.newFile().apply { writeBytes(ByteArray(8)) }
        val young = s.newFile().apply { writeBytes(ByteArray(8)) }
        val now = System.currentTimeMillis()
        orphan.setLastModified(now - 2 * MySoundStore.STALE_MS)
        s.file(kept)!!.setLastModified(now - 10 * MySoundStore.STALE_MS)
        s.cleanUp(now)
        assertFalse(orphan.exists())
        assertTrue(young.exists())
        assertNotNull(s.file(kept))
    }

    @Test
    fun aKindForEachPieceOfASong() {
        assertEquals("vocal", MySounds.kindOfStem(3, 4.0))
        assertEquals("loop", MySounds.kindOfStem(0, 4.0))
        assertEquals("perc", MySounds.kindOfStem(0, 0.3))
        assertEquals("bass", MySounds.kindOfStem(1, 0.5))
        assertEquals("kick", MySounds.kindOfPad("kick", 0.4, "Mon kick"))
        assertEquals("loop", MySounds.kindOfPad("kick", 4.0, "Morceau 2 mesures"))
        assertEquals("vocal", MySounds.kindOfPad("snare", 4.0, "Voix 2"))
        assertTrue(MySounds.KINDS.all { it in SoundPacks.KIND_LABELS })
    }
}
