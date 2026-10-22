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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Audit 28 review (G2, M1, M2): the journal of a Live take is written by one writer, never lands
 * after its take was finished, is never written into the song twice, and stays small on a long set.
 */
class LiveTakeJournalTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val acidB = AcidPattern(steps = List(AcidPattern.MAX_STEPS) { i -> AcidStep(gate = i % 4 == 0) })

    private fun project(): Project = Project(
        scenes = listOf(
            Scene("A", drums = DrumPattern().toggle(0, 0)),
            Scene("B", drums = DrumPattern().toggle(1, 0), acid = acidB.copy(length = 32)),
        ),
        scene = 0, drums = DrumPattern().toggle(0, 0),
    ).sanitized()

    private fun journal(take: String, project: String = "p-1", end: Int = 64): TakeJournal {
        val t = PerformanceTake(0, LiveState.on(0, 0), id = take)
        t.launched(LiveSlots.ACID, 1, 16)
        t.gesture(AutoTarget.acid(0), 0.8f, 20.0, 0.5f)
        return t.journal(project, end, append = false)
    }

    // --- G2 / M1: one writer ------------------------------------------------------------------------

    /** The 5 s write is under way when ■ Fin comes: it never lands after (the race of G2). */
    @Test
    fun aWriteUnderWayWhenTheTakeEndsNeverLands() {
        val store = TakeJournalStore(tmp.root)
        val reached = CountDownLatch(1)
        val go = CountDownLatch(1)
        store.beforeMove = { reached.countDown(); go.await(5, TimeUnit.SECONDS) }
        val write = store.write(journal("t1"))
        assertTrue(reached.await(5, TimeUnit.SECONDS))
        // ■ Fin: sealed, then the journal dropped (after the song is saved).
        store.seal("t1")
        val drop = store.drop("p-1", "t1")
        go.countDown()
        write.get(5, TimeUnit.SECONDS)
        drop.get(5, TimeUnit.SECONDS)
        store.beforeMove = null
        assertFalse(store.file("p-1").exists())
        assertNull(store.read("p-1"))
        // And a write of it still queued after the seal is dropped too.
        store.write(journal("t1")).get(5, TimeUnit.SECONDS)
        assertFalse(store.file("p-1").exists())
        assertTrue(tmp.root.listFiles()!!.none { it.name.endsWith(".tmp") })
    }

    /** M1: the background write and the forced one (ON_STOP, closing) never share a file: the last asked is on disk. */
    @Test
    fun writesTakeTurnsAndTheLastOneIsKept() {
        val store = TakeJournalStore(tmp.root)
        val futures = (1..40).map { k -> store.write(journal("t2", end = 64 + k * 16)) }
        futures.forEach { it.get(5, TimeUnit.SECONDS) }
        assertEquals(64 + 40 * 16, store.read("p-1")!!.end)
        assertTrue(tmp.root.listFiles()!!.none { it.name.endsWith(".tmp") })
    }

    /** A later take's journal is not deleted by the drop of an earlier one (its song saved late). */
    @Test
    fun theDropOfAnEarlierTakeKeepsALaterOne() {
        val store = TakeJournalStore(tmp.root)
        store.write(journal("t3"))
        store.seal("t3")
        store.write(journal("t4"))
        store.drop("p-1", "t3")
        assertEquals("t4", store.read("p-1")!!.takeId)
        // A journal of an earlier run of the app (not known to this writer) is read to know whose it is.
        val again = TakeJournalStore(tmp.root)
        again.drop("p-1", "t3").get(5, TimeUnit.SECONDS)
        assertTrue(again.file("p-1").exists())
        again.drop("p-1", "t4").get(5, TimeUnit.SECONDS)
        assertFalse(again.file("p-1").exists())
    }

    /** m2: journals (and temporary files left by a kill) of projects gone for good are deleted; the others stay. */
    @Test
    fun journalsOfProjectsGoneAreForgotten() {
        val store = TakeJournalStore(tmp.root)
        store.write(journal("a", project = "p-kept"))
        store.write(journal("b", project = "p-gone"))
        store.awaitIdle(5000)
        File(tmp.root, "live-take-p-kept.json.7.tmp").writeText("{")
        File(tmp.root, "favorites.json").writeText("{}")
        store.dropMissing(setOf("p-kept")).get(5, TimeUnit.SECONDS)
        assertEquals(setOf("live-take-p-kept.json", "favorites.json"), tmp.root.list()!!.toSet())
    }

    /** An unreadable journal is dropped at reading. */
    @Test
    fun anUnreadableJournalIsDropped() {
        val store = TakeJournalStore(tmp.root)
        store.file("p-1").writeText("{ abîmé")
        assertNull(store.read("p-1"))
        assertFalse(store.file("p-1").exists())
    }

    // --- G2: never written twice --------------------------------------------------------------------

    /** The same journal found again (a kill after the song was saved, before the journal went): nothing more. */
    @Test
    fun aJournalAlreadyInTheSongIsNotWrittenAgain() {
        val p = project()
        val j = journal("t5", project = "p")
        val once = p.writeJournal(j, "p")
        assertTrue(once.parts > 0)
        assertEquals("t5", once.project.liveTake)
        val twice = once.project.writeJournal(j, "p")
        assertTrue(twice.already)
        assertEquals(0, twice.parts)
        assertEquals(once.project, twice.project)
        // Appended: not twice after the song either.
        val appended = p.writeJournal(j.copy(append = true), "p").project
        assertEquals(appended.song, appended.writeJournal(j.copy(append = true), "p").project.song)
        // Kept through a save.
        assertEquals("t5", ProjectJson.decode(ProjectJson.encode(once.project)).liveTake)
    }

    // --- M2: few moves kept, the same song written ---------------------------------------------------

    /**
     * The moves dropped by [PerformanceTake.gesture] change nothing in what [writeTake] writes:
     * random sets of drags, pauses, jumps, back-and-forth and on/off presses, with launches.
     */
    @Test
    fun keepingFewerMovesWritesTheSameSong() {
        val targets = listOf(AutoTarget.acid(0), AutoTarget.acid(1), AutoTarget.live(AutoTarget.LIVE_FILTER), AutoTarget.live(AutoTarget.LIVE_REPEAT))
        val p = project()
        var kept = 0
        var all = 0
        repeat(300) { seed ->
            val r = Random(seed)
            val start = 16 * r.nextInt(0, 3)
            val compact = PerformanceTake(start, LiveState.on(0, 0))
            val full = PerformanceTake(start, LiveState.on(0, 0), compact = false)
            val value = FloatArray(targets.size) { 0.5f }
            var at = start - r.nextDouble(0.0, 8.0)
            var launchAt = start + 16
            repeat(r.nextInt(50, 600)) {
                at += when (r.nextInt(10)) {
                    0 -> r.nextDouble(1.0, 6.0)          // a pause: the next move jumps
                    1 -> 0.0                            // two moves at once
                    else -> r.nextDouble(0.02, 0.4)     // a finger dragging (60 moves a second and less)
                }
                if (at > launchAt) {
                    val slot = r.nextInt(LiveSlots.COUNT)
                    val scene = r.nextInt(-1, 2)
                    compact.launched(slot, scene, launchAt)
                    full.launched(slot, scene, launchAt)
                    launchAt += 16 * r.nextInt(1, 4)
                }
                val k = r.nextInt(targets.size)
                val t = targets[k]
                val before = value[k]
                value[k] = when {
                    t.isOnOff -> if (value[k] > 0f) 0f else 1f
                    r.nextInt(6) == 0 -> before                              // the same value again
                    r.nextInt(8) == 0 -> 0.5f                                // back where it was
                    else -> ((before + r.nextInt(-3, 4) / 16f).coerceIn(0f, 1f))
                }
                val jump = r.nextInt(25) == 0
                compact.gesture(t, value[k], at, before, jump)
                full.gesture(t, value[k], at, before, jump)
            }
            kept += compact.gestureCount
            all += full.gestureCount
            for (end in listOf(start + 32, start + 64, ((at.toInt() / 16) + 1) * 16, ((at.toInt() / 16) + 2) * 16)) {
                for (append in listOf(false, true)) {
                    val base = if (append) p.copy(song = listOf(SongPart(0, 1), SongPart(1, 2))) else p
                    val a = base.writeTake(compact, end, append)
                    val b = base.writeTake(full, end, append)
                    assertEquals("seed $seed end $end append $append", b.project, a.project)
                    assertEquals(b.parts, a.parts)
                    assertEquals(b.settings, a.settings)
                    assertEquals(b.cut, a.cut)
                    assertEquals(b.skipped, a.skipped)
                    assertEquals(b.late > 0, a.late > 0)
                }
            }
            // The journal of it writes the same song too.
            val end = ((at.toInt() / 16) + 1) * 16
            assertEquals(p.writeTake(full, end, false).project.song, p.writeJournal(TakeJournal.decode(compact.journal("p", end, false).encode())!!, "p").project.song)
        }
        assertTrue("moves kept: $kept of $all", kept < all)
    }

    /**
     * M2 cost: a 30-minute set at 120 BPM with an XY pad (two settings) and a knob dragged half
     * the time, 60 moves a second each. The take keeps at most three moves per half beat and
     * setting, the snapshot taken on the screen's thread is quick and the journal stays small.
     */
    @Test
    fun aLongSetKeepsTheJournalSmall() {
        val targets = listOf(AutoTarget.live(AutoTarget.LIVE_FILTER), AutoTarget.live(AutoTarget.LIVE_RESONANCE), AutoTarget.acid(0))
        val take = PerformanceTake(0, LiveState.on(0, 0))
        val steps = 30 * 60 * 8  // 30 min at 120 BPM: 8 steps a second
        val perStep = 60.0 / 8   // 60 moves a second
        var moves = 0
        val halves = HashSet<Long>()
        var k = 0
        val r = Random(7)
        while (k < steps * perStep) {
            val at = k / perStep
            // Dragged during every other 8 bars.
            if ((at.toInt() / 128) % 2 == 0) {
                halves += kotlin.math.floor(at / 2).toLong()
                for (t in targets) {
                    take.gesture(t, r.nextFloat(), at, 0.5f)
                    moves++
                }
            }
            k++
        }
        assertTrue("${take.gestureCount} kept of $moves", take.gestureCount <= 3 * targets.size * halves.size)
        assertTrue(take.gestureCount * 4 < moves)
        // The screen's thread only copies the lists (the writer encodes).
        val t0 = System.nanoTime()
        var journal: TakeJournal? = null
        repeat(10) { journal = take.journal("p", steps, false) }
        val snapshotMs = (System.nanoTime() - t0) / 10 / 1e6
        assertTrue("snapshot $snapshotMs ms", snapshotMs < 20)
        val bytes = journal!!.encode().length
        println("Long set: ${take.gestureCount} of $moves moves kept, snapshot $snapshotMs ms, journal $bytes bytes")
        assertTrue("journal $bytes bytes", bytes < 4 * 1024 * 1024)
        assertNotNull(TakeJournal.decode(journal!!.encode()))
    }
}
