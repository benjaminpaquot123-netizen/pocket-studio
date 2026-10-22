package com.pocketstudio.model

import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * The Live takes kept on disk while they record ([TakeJournal]): one file per project in [dir]
 * (`live-take-<project id>.json`), written into the song at the next opening when the app could
 * not finish the take.
 *
 * One writer: every write, read and deletion runs in turn on one thread of its own, in the order
 * asked, so neither two writes nor a write and a deletion ever overlap, and the caller's thread
 * (the screen's) only hands over a snapshot ([TakeJournal] holds copies of the take's lists): the
 * encoding happens here. A finished or cancelled take is [seal]ed: a write of it still waiting is
 * dropped, checked again just before its file replaces the journal. Each write goes to a temporary
 * file of its own, then replaces the journal in one move: the journal on disk is always a whole
 * one, the last one, or none. [onError]: a write or deletion that failed (the take goes on).
 */
class TakeJournalStore(val dir: File, private val onError: (Throwable) -> Unit = {}) {
    // One thread at most, gone when idle; its queue keeps the order of the calls.
    private val writer = ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, LinkedBlockingQueue()) { r ->
        Thread(r, "take-journal").apply { isDaemon = true }
    }.apply { allowCoreThreadTimeOut(true) }

    private val sealed: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /** The take each journal written by this process holds (writer thread only). */
    private val onDisk = HashMap<String, String>()
    private var serial = 0L

    /** Tests: runs on the writer once a journal is in its temporary file, before it replaces the one on disk. */
    @Volatile var beforeMove: (() -> Unit)? = null

    /** Project [projectId]'s journal. */
    fun file(projectId: String): File = File(dir, "$PREFIX${safe(projectId)}$SUFFIX")

    /** Keeps [journal] as its project's journal (on the writer), unless its take is [seal]ed by then. */
    fun write(journal: TakeJournal): Future<*> = writer.submit(Runnable {
        if (journal.takeId in sealed) return@Runnable
        val target = file(journal.projectId)
        val tmp = File(dir, "${target.name}.${++serial}.tmp")
        try {
            dir.mkdirs()
            tmp.writeText(journal.encode())
            beforeMove?.invoke()
            // Finished or cancelled while it was being written: the song has it, or nobody wants it.
            if (journal.takeId in sealed) return@Runnable
            move(tmp, target)
            onDisk[target.name] = journal.takeId
        } catch (e: Exception) {
            onError(e)
        } finally {
            tmp.delete()
        }
    })

    /** Take [takeId] is finished or cancelled: none of its writes still waiting reaches the disk. */
    fun seal(takeId: String) {
        if (takeId.isNotEmpty()) sealed += takeId
    }

    /**
     * Deletes project [projectId]'s journal (on the writer, after every write asked before) when
     * it holds take [takeId] (whatever it holds when null, or when it cannot be read).
     */
    fun drop(projectId: String, takeId: String?): Future<*> = writer.submit(Runnable {
        val f = file(projectId)
        if (!f.exists()) return@Runnable
        if (takeId != null) {
            val holds = onDisk[f.name] ?: runCatching { TakeJournal.decode(f.readText())?.takeId }.getOrNull()
            if (holds != null && holds != takeId) return@Runnable  // a later take's
        }
        if (f.delete() || !f.exists()) onDisk.remove(f.name) else onError(java.io.IOException("${f.name} : suppression impossible"))
    })

    /**
     * Project [projectId]'s journal (after every write asked before), null when there is none or
     * it does not come within [timeoutMs]; an unreadable one is deleted.
     */
    fun read(projectId: String, timeoutMs: Long = 3000): TakeJournal? = runCatching {
        writer.submit(Callable {
            val f = file(projectId)
            if (!f.exists()) return@Callable null
            val journal = runCatching { TakeJournal.decode(f.readText()) }.getOrNull()
            if (journal == null && f.delete()) onDisk.remove(f.name)
            journal
        }).get(timeoutMs, TimeUnit.MILLISECONDS)
    }.getOrNull()

    /**
     * Deletes the journals (and temporary files left by a kill) of projects that are gone: those
     * whose id is not in [projectIds] (the projects and the trashed ones, which may come back).
     */
    fun dropMissing(projectIds: Set<String>): Future<*> = writer.submit(Runnable {
        val kept = projectIds.map { "$PREFIX${safe(it)}$SUFFIX" }.toSet()
        dir.listFiles()?.forEach { f ->
            if (!f.name.startsWith(PREFIX)) return@forEach
            val journal = f.name.substringBefore(SUFFIX) + SUFFIX
            val stale = f.name.endsWith(".tmp")  // left by a kill: no write is under way here
            if ((stale || journal !in kept) && f.delete()) onDisk.remove(f.name)
        }
    })

    /** Waits (at most [timeoutMs]) for every call made before to be done; false when it is not. */
    fun awaitIdle(timeoutMs: Long): Boolean = runCatching { writer.submit(Runnable {}).get(timeoutMs, TimeUnit.MILLISECONDS) }.isSuccess

    private fun move(from: File, to: File) {
        try {
            Files.move(from.toPath(), to.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: AtomicMoveNotSupportedException) {
            // Never deletes the journal first: the old one stays until the new one replaces it.
            Files.move(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    companion object {
        const val PREFIX = "live-take-"
        private const val SUFFIX = ".json"

        private fun safe(projectId: String) = projectId.filter { it.isLetterOrDigit() || it == '-' || it == '_' }
    }
}
