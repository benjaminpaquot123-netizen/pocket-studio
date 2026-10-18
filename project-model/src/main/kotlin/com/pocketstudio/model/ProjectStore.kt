package com.pocketstudio.model

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Stores each project in its own folder (`<root>/<id>/project.json`, audio files next to it later).
 *
 * Writes never lose work, even if the app is killed mid-write:
 * 1. the new content goes to `project.json.tmp` and is flushed to disk (fsync);
 * 2. the current file is copied to `project.json.bak`;
 * 3. the temporary file atomically replaces `project.json`.
 * Loading falls back to the backup when the main file is missing or unreadable.
 */
class ProjectStore(private val root: File) {

    fun folder(id: String): File = File(root, id)

    fun list(): List<String> =
        root.listFiles()?.filter { File(it, MAIN).exists() || File(it, BACKUP).exists() }?.map { it.name }?.sorted()
            ?: emptyList()

    @Throws(IOException::class)
    fun save(id: String, project: Project) {
        val dir = folder(id).apply { mkdirs() }
        val main = File(dir, MAIN)
        val tmp = File(dir, TMP)
        FileOutputStream(tmp).use { out ->
            out.write(ProjectJson.encode(project).toByteArray(Charsets.UTF_8))
            out.fd.sync()
        }
        if (main.exists()) Files.copy(main.toPath(), File(dir, BACKUP).toPath(), StandardCopyOption.REPLACE_EXISTING)
        Files.move(tmp.toPath(), main.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
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
        throw IOException("Project $id is unreadable", lastError)
    }

    fun delete(id: String) {
        folder(id).deleteRecursively()
    }

    companion object {
        const val MAIN = "project.json"
        const val BACKUP = "project.json.bak"
        const val TMP = "project.json.tmp"
    }
}
