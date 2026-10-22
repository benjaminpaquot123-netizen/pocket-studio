package com.pocketstudio.model

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File

/** Phase 28 data audit: nothing the user made may be lost, refused back or left to crash the app. */
@OptIn(ExperimentalCoroutinesApi::class)
class SaveSafetyTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun catalog(name: String = "a", stems: File? = null) =
        ProjectCatalog(tmp.newFolder("$name-projects"), File(tmp.root, "$name-trash"), stems)

    /** 24 scenes of 8 tracks, 128 notes each: a busy project within the app's limits. */
    private fun busyProject(): Project {
        var p = Project(name = "Gros")
        repeat(InstrumentTrack.MAX - 1) { p = p.addTrack()!! }
        fun clip(seed: Int) = NoteClip(steps = 128, notes = List(128) { i -> Note(i * 24, 12, 36 + (i * 7 + seed) % 48, 0.5f + (i % 5) / 10f) })
        val scene = { i: Int -> Scene(drums = DrumPattern(steps = 64).fill(0, 2).fill(5, 1, 0.5f), synthNotes = clip(i), trackNotes = p.tracks.associate { it.id to clip(i + it.id) }) }
        return p.copy(scenes = List(Scene.MAX) { scene(it) }).sanitized()
    }

    @Test
    fun theZipOfABusyProjectImportsBack() {
        val p = busyProject()
        val pretty = kotlinx.serialization.json.Json { prettyPrint = true; encodeDefaults = true }.encodeToString(Project.serializer(), p)
        assertTrue("an indented file of it is over the old 4 MB limit", pretty.length > 4 * 1024 * 1024)
        assertTrue("written compact", ProjectJson.encode(p).length * 3 < pretty.length)
        val from = catalog("a")
        val id = from.create(p)
        val zip = ByteArrayOutputStream().also { from.exportZip(id, it) }.toByteArray()
        val back = catalog("b").importZip(zip.inputStream())
        assertEquals(0, back.missingSounds)
        // The indented file of an older version imports too.
        val old = ByteArrayOutputStream().also { out ->
            java.util.zip.ZipOutputStream(out).use { z -> z.putNextEntry(java.util.zip.ZipEntry("project.json")); z.write(pretty.toByteArray()); z.closeEntry() }
        }.toByteArray()
        catalog("c").importZip(old.inputStream())
    }

    @Test
    fun nonStopRecordingIsSavedAsItGoes() = runTest {
        val store = ProjectStore(tmp.root)
        val io = StandardTestDispatcher(testScheduler)
        val session = ProjectSession("p", Project(), store, this, io, clock = { testScheduler.currentTime })
        // A note every 300 ms for 6 s: never 400 ms of calm.
        repeat(20) { i ->
            session.editQuietly { it.copy(synthNotes = it.synthNotes.add(Note(i * 12, 12, 60))) }
            advanceTimeBy(300)
        }
        val saved = store.load("p")
        assertNotNull("saved while playing", saved)
        // At most 2.5 s behind.
        assertTrue(saved!!.synthNotes.notes.size >= 20 - 9)
        advanceTimeBy(1000)
        runCurrent()
        assertEquals(20, store.load("p")!!.synthNotes.notes.size)
    }

    private fun damaged(): String {
        // Another scene of a file edited by hand: loops of no length, lengths out of range, a path.
        val bad = """{"name":"B","drums":{"steps":200,"lanes":[{"hits":{"0":{"velocity":1.0,"repeats":99}},"sample":{"file":"../../x","name":"x"}}]},
            "acid":{"length":-1,"steps":[{"gate":true},{"gate":true},{"gate":true}]},
            "synthNotes":{"steps":0,"notes":[{"start":0,"length":24,"pitch":300},{"start":-5,"length":24,"pitch":60}]}}"""
        return """{"name":"Abîmé","scenes":[{"name":"A"},$bad],"scene":0,"song":[{"scene":0},{"scene":1}]}"""
    }

    @Test
    fun everySceneIsCleanedAtLoad() {
        val p = ProjectJson.decode(damaged())
        val b = p.scenes[1]
        assertEquals(DrumPattern.MAX_STEPS, b.drums.steps)
        assertEquals(DrumPattern.PAD_COUNT, b.drums.lanes.size)
        assertNull("never a path", b.drums.lanes[0].sample)
        assertEquals(DrumPattern.MAX_REPEATS, b.drums.lanes[0].hits.getValue(0).repeats)
        assertEquals(1, b.acid.length)
        assertEquals(AcidPattern.MAX_STEPS, b.acid.steps.size)
        assertEquals(NoteClip.MIN_STEPS, b.synthNotes.steps)
        assertEquals(listOf(127), b.synthNotes.notes.map { it.pitch })
        // What crashed: the MIDI export (an endless loop) and the live grid.
        assertTrue(MidiFile.export(p).isNotEmpty())
        assertTrue(p.cellHasContent(SongPart.ACID, 1))
        // Cleaning twice changes nothing.
        assertEquals(p, p.sanitized())
    }

    @Test(timeout = 10_000)
    fun theMidiExportAndTheLiveGridSurviveDataNeverCleaned() {
        val raw = Project(
            scenes = listOf(Scene(), Scene(synthNotes = NoteClip(steps = 0, notes = listOf(Note(0, 24, 60))), acid = AcidPattern(length = -1, steps = List(3) { AcidStep(gate = true) }), drums = DrumPattern(steps = 0))),
            song = listOf(SongPart(0), SongPart(1)),
        )
        MidiFile.export(raw)
        assertFalse(raw.cellHasContent(SongPart.ACID, 1))
        assertFalse(raw.scenes[1].isEmpty)  // took a negative count before
    }

    @Test
    fun valuesThatAreNotNumbersNeverStopTheSaves() {
        val nan = Project(
            tempoBpm = Double.NaN, swing = Double.NaN,
            drums = DrumPattern().toggle(0, 0, Float.NaN),
            mixer = Mixer(channels = List(Mixer.CHANNELS) { Channel(volume = Float.NaN, pan = Float.NaN) }),
            synth = SynthPatch(List(SynthPatch.PARAM_COUNT) { Float.NaN }),
            acid = AcidPattern(density = Float.NaN, sound = AcidSound(cutoff = Float.NaN)),
        ).sanitized()
        val text = ProjectJson.encode(nan)  // threw before
        val back = ProjectJson.decode(text)
        assertEquals(120.0, back.tempoBpm, 0.0)
        assertEquals(1f, back.drums.lanes[0].hits.getValue(0).velocity)
        assertEquals(0.8f, back.mixer.channels[0].volume)
        assertEquals(SynthPatch.DEFAULTS, back.synth.values)
        assertEquals(AcidSound().cutoff, back.acid.sound.cutoff)
        ProjectStore(tmp.root).save("nan", nan)
    }

    @Test
    fun theTrashCanBeEmptiedForGood() {
        val c = catalog()
        val a = c.delete(c.create(Project(name = "Un")))
        val b = c.delete(c.create(Project(name = "Deux")))
        c.create(Project(name = "Gardé"))
        File(tmp.root, "a-trash/$a/samples").apply { mkdirs() }.resolve("voix.wav").writeBytes(ByteArray(1000))
        c.deleteForever(a)
        assertEquals(listOf("Deux"), c.trashed().map { it.name })
        assertFalse(File(tmp.root, "a-trash/$a").exists())
        // Only an entry of the trash: never a path out of it.
        assertTrue(runCatching { c.deleteForever("../a-projects") }.isFailure)
        assertEquals(listOf("Gardé"), c.list().map { it.name })
        assertEquals(1, c.emptyTrash())
        assertTrue(c.trashed().isEmpty())
        assertEquals(listOf("Gardé"), c.list().map { it.name })
        assertTrue(runCatching { c.deleteForever(b) }.isSuccess)  // already gone: nothing to do
    }

    @Test
    fun foldersLeftByAnInterruptedCreationAreCleanedUp() {
        val root = tmp.newFolder("projects")
        val c = ProjectCatalog(root, File(tmp.root, "trash"))
        val kept = c.create(Project(name = "Ok"))
        val now = System.currentTimeMillis()
        fun orphan(name: String, age: Long) = File(root, "$name/samples").apply { mkdirs() }.resolve("son.wav").apply {
            writeBytes(ByteArray(10)); setLastModified(now - age); parentFile.setLastModified(now - age); parentFile.parentFile.setLastModified(now - age)
        }
        orphan("p-old", 2 * 3_600_000)
        orphan("p-new", 60_000)  // maybe being written right now
        orphan("p-open", 2 * 3_600_000)  // the open project, not saved yet
        // A first save cut short just before its rename: the complete .tmp is put back, not thrown away.
        File(root, "p-tmp").mkdirs()
        File(root, "p-tmp/${ProjectStore.TMP}").apply { writeText(ProjectJson.encode(Project(name = "Presque"))); setLastModified(now - 2 * 3_600_000) }
        File(root, "p-tmp").setLastModified(now - 2 * 3_600_000)
        assertEquals(1, c.cleanOrphans(keep = setOf("p-open"), now = now))
        assertEquals(setOf(kept, "p-new", "p-open", "p-tmp"), root.list()!!.toSet())
        assertEquals("Presque", c.store.load("p-tmp")!!.name)
    }

    @Test
    fun aZipMissingASoundStillImports() {
        val p = Project(
            name = "Troué",
            drums = DrumPattern().withSample(0, PadSample("kick.wav", "Kick")).withSample(1, PadSample("gone.wav", "Parti")),
            live = LivePads().with(0, LiveClip("gone.wav", "Parti")),
        )
        val zip = ByteArrayOutputStream().also { out ->
            java.util.zip.ZipOutputStream(out).use { z ->
                z.putNextEntry(java.util.zip.ZipEntry("project.json")); z.write(ProjectJson.encode(p).toByteArray()); z.closeEntry()
                z.putNextEntry(java.util.zip.ZipEntry("samples/kick.wav")); z.write(1); z.closeEntry()
            }
        }.toByteArray()
        val c = catalog()
        val imported = c.importZip(zip.inputStream())
        assertEquals(1, imported.missingSounds)
        val back = c.store.load(imported.id)!!
        assertEquals("kick.wav", back.drums.lanes[0].sample?.file)
        assertNull("back to the kit sound", back.drums.lanes[1].sample)
        assertNull(back.live.clips[0])
        assertEquals(setOf("kick.wav"), back.sampleFiles)
        // The export says how many sounds it could not carry.
        val again = c.create(p)
        File(c.store.folder(again), "samples").apply { mkdirs() }.resolve("kick.wav").writeText("boum")
        assertEquals(1, c.exportZip(again, ByteArrayOutputStream()))
    }

    @Test
    fun aRemixZipCarriesItsSeparatedSong() {
        val stemsHere = tmp.newFolder("stems-here")
        File(stemsHere, "abc123").apply { mkdirs() }.let { set ->
            listOf("drums", "bass", "other", "vocals").forEach { File(set, "$it.wav").writeText("pistes $it") }
            File(set, "stems.json").writeText("{\"name\":\"Chanson\"}")
        }
        File(stemsHere, "other-song").mkdirs()
        val from = catalog("a", stemsHere)
        val id = from.create(Project(name = "Remix", remix = Remix(setId = "abc123", name = "Chanson")))
        val zip = ByteArrayOutputStream().also { from.exportZip(id, it) }.toByteArray()
        // Another phone: no stems at all.
        val stemsThere = File(tmp.root, "stems-there")
        val to = catalog("b", stemsThere)
        val back = to.importZip(zip.inputStream())
        assertEquals("abc123", to.store.load(back.id)!!.remix.setId)
        assertEquals("pistes vocals", File(stemsThere, "abc123/vocals.wav").readText())
        assertEquals("{\"name\":\"Chanson\"}", File(stemsThere, "abc123/stems.json").readText())
        assertEquals(listOf("abc123"), stemsThere.list()!!.toList())
        assertFalse("nothing left beside the project", File(to.store.folder(back.id), ".stems").exists())
        // Imported again where the song already is: the song there is kept as it is.
        File(stemsThere, "abc123/vocals.wav").writeText("déjà là")
        to.importZip(zip.inputStream())
        assertEquals("déjà là", File(stemsThere, "abc123/vocals.wav").readText())
    }

    @Test
    fun unreadableFavoritesAreSetAsideNotOverwritten() {
        val file = File(tmp.root, "favorites.json").apply { writeText("{ abîmé") }
        val favorites = FavoritesFile(file)
        val loaded = favorites.load(now = 42)
        assertEquals(Favorites(), loaded.favorites)
        assertEquals("favorites-illisible-42.json", loaded.setAside)
        assertEquals("{ abîmé", File(tmp.root, "favorites-illisible-42.json").readText())
        val one = Favorites(synth = listOf(NamedSound("Mon son 1", SoundCategories.FAVORITES, SynthPatch())))
        favorites.save(one)
        assertEquals(one, FavoritesFile(file).load().favorites)
        assertEquals("{ abîmé", File(tmp.root, "favorites-illisible-42.json").readText())
        // A readable file is just read.
        assertNull(FavoritesFile(file).load().setAside)
    }

    @Test
    fun anEditOnABigProjectRebuildsOnlyWhatChanged() {
        val p = busyProject()
        val again = p.sanitized()
        // Each scene's loops are the very same objects: nothing sorted, copied or compared note by note.
        p.scenes.indices.filter { it != p.scene }.forEach { i ->
            assertTrue(again.scenes[i] === p.scenes[i])
        }
        assertTrue(again.synthNotes === p.synthNotes && again.drums === p.drums && again.acid === p.acid)
        assertTrue(again.tracks.zip(p.tracks).all { (a, b) -> a.notes === b.notes })
        assertEquals(p, again)
    }

    /** Audit 28 review m5: a save being written is not « settled »: a screen closing then flushes, which waits for it. */
    @Test
    fun aSaveBeingWrittenIsNotSettledAndAFlushWaitsForIt() = kotlinx.coroutines.runBlocking {
        val store = ProjectStore(tmp.root)
        val gate = java.util.concurrent.CountDownLatch(1)
        val writer = java.util.concurrent.Executors.newSingleThreadExecutor()
        writer.submit { gate.await(5, java.util.concurrent.TimeUnit.SECONDS) }  // the disk busy: the save waits
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)
        try {
            val session = ProjectSession("p", Project(), store, scope, writer.asCoroutineDispatcher(), autosaveDelayMs = 10)
            assertTrue(session.settled)
            session.edit { it.copy(name = "B") }
            kotlinx.coroutines.withTimeout(5000) { while (session.unsaved.value) kotlinx.coroutines.delay(5) }
            // Under way: nothing waits any more, but nothing is on disk either.
            assertFalse(session.unsaved.value)
            assertFalse(session.settled)
            assertNull(store.load("p"))
            val closing = scope.async { session.flush() }
            gate.countDown()
            assertTrue(closing.await())
            assertTrue(session.settled)
            assertEquals("B", store.load("p")!!.name)
        } finally {
            scope.cancel()
            writer.shutdown()
        }
    }

    /** Audit 28 review m7: the remix's song in a .zip cannot write anywhere but in its own set. */
    @Test
    fun stemPathTricksInAZipAreIgnored() {
        val stems = tmp.newFolder("stems")
        val c = catalog("z", stems)
        val zip = ByteArrayOutputStream().also { out ->
            java.util.zip.ZipOutputStream(out).use { z ->
                fun put(name: String, text: String) {
                    z.putNextEntry(java.util.zip.ZipEntry(name))
                    z.write(text.toByteArray())
                    z.closeEntry()
                }
                put("project.json", ProjectJson.encode(Project(name = "Piégé", remix = Remix(setId = "ok", name = "Chanson"))))
                put("stems/ok/drums.wav", "bon")
                for (bad in listOf(
                    "stems/../evil1", "stems/../x/evil2", "stems/a/../evil3", "stems/a/b/evil4", "stems/..\\evil5",
                    "stems\\..\\evil6", "stems\\a\\..\\evil7", "stems/./evil8", "stems/ok/../evil9", "stems//evil10",
                    "stems/ok/", "stems/.hidden/evil11", "samples/../evil12", "samples/a/evil13", "../evil14", "/abs/evil15",
                )) put(bad, "piège")
            }
        }.toByteArray()
        val imported = c.importZip(zip.inputStream())
        assertEquals("Piégé", c.store.load(imported.id)!!.name)
        assertEquals("bon", File(stems, "ok/drums.wav").readText())
        assertEquals(listOf("ok"), stems.list()!!.toList())
        assertEquals(listOf("drums.wav"), File(stems, "ok").list()!!.toList())
        assertTrue("nothing written by the traps", tmp.root.walkTopDown().none { it.name.startsWith("evil") })
        assertFalse("no file left beside the project", File(c.store.folder(imported.id), ".project.json").exists())
    }

    /** m7: project.json is read as it goes (a big one, indented as older versions wrote it, imports). */
    @Test
    fun aBigProjectJsonIsReadAsItGoes() {
        val p = busyProject()
        val pretty = kotlinx.serialization.json.Json { prettyPrint = true; encodeDefaults = true }.encodeToString(Project.serializer(), p)
        assertEquals(p, pretty.byteInputStream().use { ProjectJson.decode(it) })
        val zip = ByteArrayOutputStream().also { out ->
            java.util.zip.ZipOutputStream(out).use { z -> z.putNextEntry(java.util.zip.ZipEntry("project.json")); z.write(pretty.toByteArray()); z.closeEntry() }
        }.toByteArray()
        val c = catalog("big")
        val back = c.importZip(zip.inputStream())
        assertEquals(p.copy(name = p.name), c.store.load(back.id)!!.copy(name = p.name))
        assertEquals(listOf(ProjectStore.MAIN), c.store.folder(back.id).list()!!.filter { !it.endsWith(".bak") }.sorted())
    }
}
