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
        return ProjectInfo(id, p.name, modified, p.tempoBpm, scene.stepsFor(p.stepsPerBar) / p.stepsPerBar, p.allScenes.size, p.remix.setId != null)
    }

    /**
     * Saves [project] as a new project and returns its id. [samples] first writes the sounds it
     * uses into its samples folder (the project only appears once they are all there).
     */
    @Throws(IOException::class)
    fun create(project: Project, samples: (File) -> Unit = {}): String {
        val id = newId()
        try {
            samples(File(store.folder(id), SAMPLES))
            store.save(id, project.sanitized())
        } catch (e: Exception) {
            store.folder(id).deleteRecursively()
            throw e
        }
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

    /** The project and its sounds as a .zip (project.json + samples/…), for a backup or another phone. */
    @Throws(IOException::class)
    fun exportZip(id: String, out: java.io.OutputStream) {
        val project = store.load(id) ?: throw IOException("projet introuvable")
        java.util.zip.ZipOutputStream(out.buffered()).use { zip ->
            zip.putNextEntry(java.util.zip.ZipEntry(ProjectStore.MAIN))
            zip.write(ProjectJson.encode(project).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            val used = project.sampleFiles
            File(store.folder(id), SAMPLES).listFiles()?.filter { it.name in used }?.sortedBy { it.name }?.forEach { f ->
                zip.putNextEntry(java.util.zip.ZipEntry("$SAMPLES/${f.name}"))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    /**
     * A project .zip made by [exportZip] becomes a new project (never replaces one); returns its
     * id. Only project.json and plain sample files are read; size limits guard the phone.
     */
    @Throws(IOException::class)
    fun importZip(input: java.io.InputStream): String {
        val id = newId()
        val dir = store.folder(id)
        var project: Project? = null
        var total = 0L
        val seen = HashSet<String>()
        try {
            java.util.zip.ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue
                    val name = entry.name.replace('\\', '/')
                    if (!seen.add(name)) throw IOException("fichier en double dans le .zip")
                    val target = when {
                        name == ProjectStore.MAIN -> null
                        name.startsWith("$SAMPLES/") && PadSample(name.removePrefix("$SAMPLES/"), "").isSafe ->
                            File(dir, name).also { it.parentFile.mkdirs() }
                        else -> continue  // anything else in the archive is ignored
                    }
                    val bytes = java.io.ByteArrayOutputStream()
                    val buf = ByteArray(64 * 1024)
                    var size = 0L
                    while (true) {
                        val n = zip.read(buf)
                        if (n < 0) break
                        size += n
                        total += n
                        if (size > (if (target == null) MAX_PROJECT_JSON else MAX_ZIP_ENTRY) || total > MAX_ZIP_TOTAL) throw IOException("projet trop gros")
                        if (target == null) bytes.write(buf, 0, n) else target.appendBytes(buf.copyOf(n))
                    }
                    if (target == null) project = ProjectJson.decode(bytes.toString(Charsets.UTF_8.name()))
                }
            }
            // Cleaned first: only plain file names count (a path like ../x is dropped here).
            val p = (project ?: throw IOException("ce .zip n'est pas un projet Pocket Studio")).sanitized()
            val missing = p.sampleFiles.filterNot { File(dir, "$SAMPLES/$it").exists() }
            if (missing.isNotEmpty()) throw IOException("${missing.size} son(s) manquant(s) dans le .zip")
            store.save(id, p)
            return id
        } catch (e: Throwable) {
            // Also an OutOfMemoryError: nothing half-imported stays behind, and the app does not crash.
            dir.deleteRecursively()
            throw if (e is IOException) e else IOException(e.message ?: "fichier illisible")
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
        const val MAX_ZIP_ENTRY = 128L * 1024 * 1024
        const val MAX_PROJECT_JSON = 4L * 1024 * 1024
        const val MAX_ZIP_TOTAL = 1024L * 1024 * 1024
    }
}
