package com.pocketstudio.model

import com.pocketstudio.model.i18n.tr
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.nio.file.StandardCopyOption

/**
 * Stores each project in its own folder (`<root>/<id>/project.json`, audio files next to it later).
 *
 * Writes never lose work, even if the app is killed mid-write:
 * 1. the new content goes to `project.json.tmp` and is flushed to disk (fsync);
 * 2. the current file is copied to `project.json.bak`;
 * 3. the temporary file atomically replaces `project.json`.
 * Loading falls back to the backup when the main file is missing or unreadable.
 *
 * [backup]: where a compressed copy of each project's file goes after each save
 * (`<backup>/<id>.json.gz`, best effort), for Android's cloud backup: capped at 25 MB, it cannot
 * hold the projects' sounds, so it holds these copies alone (see BackupMirrors in the app).
 * The first save of each session ([keepOpening]) keeps the file as it was when the project opened
 * (`project.open.json`): « la version d'avant cette séance ».
 */
class ProjectStore(private val root: File, private val backup: File? = null) {

    fun folder(id: String): File = File(root, id)

    fun list(): List<String> =
        root.listFiles()?.filter { File(it, MAIN).exists() || File(it, BACKUP).exists() }?.map { it.name }?.sorted()
            ?: emptyList()

    @Throws(IOException::class)
    fun save(id: String, project: Project) {
        val dir = folder(id).apply { mkdirs() }
        val main = File(dir, MAIN)
        val tmp = File(dir, TMP)
        val bytes = ProjectJson.encode(project).toByteArray(Charsets.UTF_8)
        FileOutputStream(tmp).use { out ->
            out.write(bytes)
            out.fd.sync()
        }
        if (main.exists()) {
            val backup = File(dir, BACKUP)
            Files.copy(main.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING)
            FileOutputStream(backup, true).use { it.fd.sync() }
        }
        Files.move(tmp.toPath(), main.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        syncDirectory(dir)
        mirror(id, bytes)
    }

    /** The compressed copy for the cloud backup ([backup]); never fails a save. */
    private fun mirror(id: String, bytes: ByteArray) {
        val to = backup ?: return
        runCatching {
            to.mkdirs()
            val tmp = File(to, "$id$MIRROR.tmp")
            // The fastest level: done after every save, and JSON shrinks about tenfold even so.
            object : java.util.zip.GZIPOutputStream(FileOutputStream(tmp)) {
                init { def.setLevel(java.util.zip.Deflater.BEST_SPEED) }
            }.use { it.write(bytes) }
            // Renamed whole: a backup taken meanwhile never holds half a file.
            Files.move(tmp.toPath(), File(to, "$id$MIRROR").toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        }
    }

    /** The copy of [id] for the cloud backup is forgotten (the project deleted). */
    fun forgetMirror(id: String) {
        backup?.let { File(it, "$id$MIRROR").delete() }
    }

    /** The copy of [id] for the cloud backup made again from its file (a project back from the trash). */
    fun refreshMirror(id: String) {
        if (backup == null) return
        runCatching { mirror(id, File(folder(id), MAIN).readBytes()) }
    }

    /**
     * Keeps [id]'s file as it is now as « the version before this session » (`project.open.json`),
     * replacing the last session's; called once per session, before its first save. Best effort.
     */
    fun keepOpening(id: String) {
        val main = File(folder(id), MAIN)
        if (!main.isFile) return
        runCatching {
            val tmp = File(folder(id), "$OPENING.tmp")
            Files.copy(main.toPath(), tmp.toPath(), StandardCopyOption.REPLACE_EXISTING)
            FileOutputStream(tmp, true).use { it.fd.sync() }
            Files.move(tmp.toPath(), File(folder(id), OPENING).toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        }
    }

    /** « The version before the last session » of [id] ([keepOpening]); null when none or unreadable. */
    fun loadOpening(id: String): Project? {
        val f = File(folder(id), OPENING)
        if (!f.isFile) return null
        return runCatching { ProjectJson.decode(f.readText(Charsets.UTF_8)) }.getOrNull()
    }

    /** Loads a project, or null if it does not exist. Throws only if every copy is unreadable. */
    @Throws(IOException::class)
    fun load(id: String): Project? {
        val dir = folder(id)
        val candidates = listOf(File(dir, MAIN), File(dir, BACKUP)).filter { it.exists() }
        if (candidates.isEmpty()) return null
        var lastError: Exception? = null
        for (file in candidates) {
            try {
                return ProjectJson.decode(file.readText(Charsets.UTF_8))
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw IOException(tr("projet illisible (%s)", id), lastError)
    }

    // Makes the rename itself durable (power cut right after a save). Best effort: not every platform allows it.
    private fun syncDirectory(dir: File) {
        runCatching { FileChannel.open(dir.toPath(), StandardOpenOption.READ).use { it.force(true) } }
    }

    fun delete(id: String) {
        folder(id).deleteRecursively()
    }

    companion object {
        const val MAIN = "project.json"
        const val BACKUP = "project.json.bak"
        const val TMP = "project.json.tmp"
        /** « La version d'avant cette séance » ([keepOpening]). */
        const val OPENING = "project.open.json"
        /** A project's copy for the cloud backup: `<id>.json.gz`. */
        const val MIRROR = ".json.gz"
    }
}
