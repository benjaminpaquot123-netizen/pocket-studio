package com.pocketstudio.model

import java.io.File
import java.io.IOException
import java.util.UUID

/** What the project browser shows for one project, without opening it. */
data class ProjectInfo(
    val id: String,
    val name: String,
    /** Last save, in milliseconds since 1970. */
    val modified: Long,
    val tempoBpm: Double,
    /** Length of the longest part of the current scene, in bars. */
    val bars: Int,
    val scenes: Int,
    val hasRemix: Boolean,
)

/**
 * Every project on the phone: one folder each under [root] (see [ProjectStore]). Deleting moves
 * the folder to [trash] so a slip of the finger never loses work; [purgeTrash] forgets old ones.
 * Blocking file work: call it off the main thread.
 */
class ProjectCatalog(private val root: File, private val trash: File) {
    val store = ProjectStore(root)

    fun list(): List<ProjectInfo> = store.list().mapNotNull { info(it) }.sortedByDescending { it.modified }

    fun info(id: String): ProjectInfo? {
        val dir = store.folder(id)
        val modified = listOf(File(dir, ProjectStore.MAIN), File(dir, ProjectStore.BACKUP)).filter { it.exists() }.maxOfOrNull { it.lastModified() }
            ?: return null
        val p = runCatching { store.load(id) }.getOrNull()
            ?: return ProjectInfo(id, "Projet illisible", modified, 0.0, 0, 0, false)
        val scene = p.currentSceneContent()
        return ProjectInfo(id, p.name, modified, p.tempoBpm, scene.steps / 16, p.allScenes.size, p.remix.setId != null)
    }

    /** Saves [project] as a new project and returns its id. */
    @Throws(IOException::class)
    fun create(project: Project): String {
        val id = newId()
        store.save(id, project.sanitized())
        return id
    }

    /** A copy of [id] (its sounds included) named [name]; returns the new id. */
    @Throws(IOException::class)
    fun duplicate(id: String, name: String): String {
        val source = store.load(id) ?: throw IOException("projet introuvable")
        val copy = newId()
        val samples = File(store.folder(id), SAMPLES)
        if (samples.isDirectory) samples.copyRecursively(File(store.folder(copy), SAMPLES), overwrite = true)
        store.save(copy, source.copy(name = name.take(MAX_NAME)))
        return copy
    }

    /** Renames a project that is not open (an open one is renamed through its session). */
    @Throws(IOException::class)
    fun rename(id: String, name: String) {
        val p = store.load(id) ?: throw IOException("projet introuvable")
        store.save(id, p.copy(name = name.take(MAX_NAME)))
    }

    /** Moves the project to the trash; returns the trash entry to [restore] it. */
    @Throws(IOException::class)
    fun delete(id: String): String {
        trash.mkdirs()
        val entry = "$id@${System.currentTimeMillis()}"
        val from = store.folder(id)
        if (!from.exists()) throw IOException("projet introuvable")
        if (!from.renameTo(File(trash, entry))) {
            // Another file system: copy, then remove the original only once the copy is complete.
            if (!from.copyRecursively(File(trash, entry), overwrite = true)) throw IOException("copie impossible")
            from.deleteRecursively()
        }
        return entry
    }

    /** Puts a trashed project back (under its old id if free, else a new one); returns its id. */
    @Throws(IOException::class)
    fun restore(entry: String): String {
        val from = File(trash, entry)
        if (!from.isDirectory) throw IOException("rien à restaurer")
        val wanted = entry.substringBefore('@')
        val id = if (store.folder(wanted).exists()) newId() else wanted
        if (!from.renameTo(store.folder(id))) throw IOException("restauration impossible")
        return id
    }

    /** Forgets trashed projects older than [maxAgeMs]. */
    fun purgeTrash(maxAgeMs: Long, now: Long = System.currentTimeMillis()) {
        trash.listFiles()?.forEach { f ->
            val at = f.name.substringAfterLast('@').toLongOrNull() ?: return@forEach
            if (now - at > maxAgeMs) f.deleteRecursively()
        }
    }

    /** "Mon projet 3": the first free number among the existing names. */
    fun nextName(prefix: String = "Mon projet"): String {
        val names = list().map { it.name }.toSet()
        return generateSequence(1) { it + 1 }.map { "$prefix $it" }.first { it !in names }
    }

    private fun newId() = "p-" + UUID.randomUUID().toString().take(8)

    companion object {
        const val SAMPLES = "samples"
        const val MAX_NAME = 40
        const val TRASH_DAYS = 30L
    }
}
