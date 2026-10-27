package com.pocketstudio.model

import com.pocketstudio.model.i18n.tr
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
    /** Its file could not be read: it is never opened (that would set it aside again), only sent as it is. */
    val unreadable: Boolean = false,
    /** « La version d'avant la dernière séance » is kept ([ProjectCatalog.restoreOpening]). */
    val hasOpening: Boolean = false,
)

/** A project .zip brought in: the new project's [id], and the sounds it named but did not hold (left out). */
data class ImportedProject(val id: String, val missingSounds: Int = 0)

/**
 * Every project on the phone: one folder each under [root] (see [ProjectStore]). Deleting moves
 * the folder to [trash] so a slip of the finger never loses work; [purgeTrash] forgets old ones.
 * [stems]: where separated songs live (`stems/<set id>/`), so a remix's .zip carries its song.
 * Blocking file work: call it off the main thread.
 */
class ProjectCatalog(private val root: File, private val trash: File, private val stems: File? = null, private val backup: File? = null) {
    val store = ProjectStore(root, backup)

    fun list(): List<ProjectInfo> = store.list().mapNotNull { info(it) }.sortedByDescending { it.modified }

    fun info(id: String): ProjectInfo? {
        val dir = store.folder(id)
        val modified = listOf(File(dir, ProjectStore.MAIN), File(dir, ProjectStore.BACKUP)).filter { it.exists() }.maxOfOrNull { it.lastModified() }
            ?: return null
        val p = runCatching { store.load(id) }.getOrNull()
            ?: return ProjectInfo(id, tr("Projet illisible"), modified, 0.0, 0, 0, false, unreadable = true)
        val scene = p.currentSceneContent()
        return ProjectInfo(
            id, p.name, modified, p.tempoBpm, scene.stepsFor(p.stepsPerBar) / p.stepsPerBar, p.allScenes.size, p.remix.setId != null,
            hasOpening = File(dir, ProjectStore.OPENING).isFile,
        )
    }

    /**
     * A project's folder as it is on the phone (its files, read or not, and its sounds) as a .zip:
     * the only way out for a project the app cannot read (« Envoyer le fichier brut »), to be
     * repaired elsewhere. Nothing is decoded.
     */
    @Throws(IOException::class)
    fun exportRawZip(id: String, out: java.io.OutputStream) {
        val dir = store.folder(id)
        if (!dir.isDirectory) throw IOException(tr("projet introuvable"))
        java.util.zip.ZipOutputStream(out.buffered()).use { zip ->
            zip.setLevel(java.util.zip.Deflater.BEST_SPEED)
            dir.walkTopDown().filter { it.isFile }.sortedBy { it.path }.forEach { f ->
                zip.putFile(f.relativeTo(dir).invariantSeparatorsPath, f)
            }
        }
    }

    /**
     * « La version d'avant la dernière séance » of [id] ([ProjectStore.keepOpening]) as a new
     * project named [name] (its sounds copied): nothing is replaced. Returns the new id.
     */
    @Throws(IOException::class)
    fun restoreOpening(id: String, name: String): String {
        val before = store.loadOpening(id) ?: throw IOException(tr("aucune version d'avant"))
        val copy = newId()
        try {
            val samples = File(store.folder(id), SAMPLES)
            if (samples.isDirectory) samples.copyRecursively(File(store.folder(copy), SAMPLES), overwrite = true)
            store.save(copy, before.copy(name = name.take(MAX_NAME)).sanitized())
        } catch (e: Exception) {
            store.folder(copy).deleteRecursively()
            throw if (e is IOException) e else IOException(tr("copie impossible"), e)
        }
        return copy
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
        val source = store.load(id) ?: throw IOException(tr("projet introuvable"))
        val copy = newId()
        try {
            val samples = File(store.folder(id), SAMPLES)
            if (samples.isDirectory) samples.copyRecursively(File(store.folder(copy), SAMPLES), overwrite = true)
            store.save(copy, source.copy(name = name.take(MAX_NAME)))
        } catch (e: Exception) {
            // A half-copied folder (storage full…) would show up as a broken project: remove it.
            store.folder(copy).deleteRecursively()
            throw if (e is IOException) e else IOException(tr("copie impossible"), e)
        }
        return copy
    }

    /** Renames a project that is not open (an open one is renamed through its session). */
    @Throws(IOException::class)
    fun rename(id: String, name: String) {
        val p = store.load(id) ?: throw IOException(tr("projet introuvable"))
        store.save(id, p.copy(name = name.take(MAX_NAME)))
    }

    /** Moves the project to the trash; returns the trash entry to [restore] it. */
    @Throws(IOException::class)
    fun delete(id: String): String {
        trash.mkdirs()
        val entry = "$id@${System.currentTimeMillis()}"
        val from = store.folder(id)
        if (!from.exists()) throw IOException(tr("projet introuvable"))
        if (!from.renameTo(File(trash, entry))) {
            // Another file system: copy, then remove the original only once the copy is complete.
            if (!from.copyRecursively(File(trash, entry), overwrite = true)) throw IOException(tr("copie impossible"))
            from.deleteRecursively()
        }
        // A phone restored from the cloud backup does not bring a deleted project back.
        store.forgetMirror(id)
        return entry
    }

    /** Puts a trashed project back (under its old id if free, else a new one); returns its id. */
    @Throws(IOException::class)
    fun restore(entry: String): String {
        val from = File(trash, entry)
        if (!from.isDirectory) throw IOException(tr("rien à restaurer"))
        val wanted = entry.substringBefore('@')
        val id = if (store.folder(wanted).exists()) newId() else wanted
        if (!from.renameTo(store.folder(id))) throw IOException(tr("restauration impossible"))
        store.refreshMirror(id)
        return id
    }

    /**
     * The projects of the cloud backup's copies ([ProjectStore] `backup`) that this phone does not
     * have (a phone set up again from Android's backup, which holds no sounds): each one written
     * back without its sounds (its pads back to the kit's). Never one in the list or the trash.
     * Returns how many came back.
     */
    fun restoreFromBackup(): Int {
        val dir = backup ?: return 0
        val known = knownIds()
        var restored = 0
        dir.listFiles().orEmpty().filter { it.isFile && it.name.endsWith(ProjectStore.MIRROR) }.forEach { f ->
            val id = f.name.removeSuffix(ProjectStore.MIRROR)
            if (id in known || !PadSample(id, "").isSafe) return@forEach
            val read = runCatching {
                java.util.zip.GZIPInputStream(f.inputStream()).bufferedReader(Charsets.UTF_8).use { ProjectJson.decode(it.readText()) }.sanitized()
            }.getOrNull() ?: return@forEach
            runCatching { store.save(id, read.withoutSounds(read.sampleFiles)) }.onSuccess { restored++ }
        }
        return restored
    }

    /** A copy for the cloud backup of each project that has none (saved before the copies existed). */
    fun mirrorMissing() {
        val dir = backup ?: return
        store.list().forEach { id -> if (!File(dir, "$id${ProjectStore.MIRROR}").exists()) store.refreshMirror(id) }
    }

    /** The projects in the trash, most recently deleted first (each one can be [restore]d). */
    fun trashed(): List<TrashedProject> {
        val bin = ProjectStore(trash)
        return bin.list().mapNotNull { entry ->
            val at = entry.substringAfterLast('@').toLongOrNull() ?: return@mapNotNull null
            val name = runCatching { bin.load(entry)?.name }.getOrNull() ?: tr("Projet illisible")
            TrashedProject(entry, name, at)
        }.sortedByDescending { it.deletedAt }
    }

    /**
     * Separated songs (stem set ids) played along by a project, with the names of those projects.
     * Trashed projects count too: they may come back. An unreadable project still protects the set
     * its file names, so cleaning up never removes stems someone may need.
     */
    fun stemSetUsers(): Map<String, List<String>> {
        val users = LinkedHashMap<String, MutableList<String>>()
        fun scan(from: ProjectStore, trashed: Boolean) = from.list().forEach { id ->
            val p = runCatching { from.load(id) }.getOrNull()
            val sets = if (p != null) listOfNotNull(p.remix.setId) else rawSetIds(from.folder(id))
            val name = (p?.name ?: tr("Projet illisible")) + if (trashed) tr(" (corbeille)") else ""
            sets.forEach { users.getOrPut(it) { mutableListOf() } += name }
        }
        scan(store, trashed = false)
        scan(ProjectStore(trash), trashed = true)
        return users
    }

    private fun rawSetIds(dir: File): Set<String> =
        listOf(ProjectStore.MAIN, ProjectStore.BACKUP).flatMap { f ->
            val text = runCatching { File(dir, f).readText() }.getOrDefault("")
            SET_ID.findAll(text).map { it.groupValues[1] }.toList()
        }.toSet()

    /** Deletes a trashed project for good (its sounds with it): the place comes back now. */
    @Throws(IOException::class)
    fun deleteForever(entry: String) {
        val f = File(trash, entry)
        // Only an entry of the trash itself: never a path elsewhere.
        if (entry.isEmpty() || entry.contains('/') || entry.contains('\\') || entry.startsWith(".") || f.parentFile != trash) throw IOException(tr("rien à supprimer"))
        if (f.exists() && !f.deleteRecursively()) throw IOException(tr("suppression impossible"))
    }

    /** Empties the trash (« Vider la corbeille »); returns how many projects went. */
    @Throws(IOException::class)
    fun emptyTrash(): Int {
        val entries = trash.listFiles().orEmpty()
        val projects = entries.count { it.isDirectory }
        entries.forEach { if (!it.deleteRecursively()) throw IOException(tr("suppression impossible")) }
        return projects
    }

    /**
     * Folders left by a creation, import or copy cut short (the app killed): sounds but no
     * project.json, never listed. Those untouched for [maxAgeMs] go, except [keep] (the open
     * project, which may not be saved yet). A complete first save cut short just before its last
     * step (only project.json.tmp) is put back instead. Returns how many folders went.
     */
    fun cleanOrphans(keep: Set<String>, maxAgeMs: Long = 3_600_000, now: Long = System.currentTimeMillis()): Int {
        var removed = 0
        root.listFiles()?.forEach { dir ->
            if (!dir.isDirectory || dir.name in keep) return@forEach
            if (File(dir, ProjectStore.MAIN).exists() || File(dir, ProjectStore.BACKUP).exists()) return@forEach
            if (dir.walkTopDown().any { it.lastModified() > now - maxAgeMs }) return@forEach
            val tmp = File(dir, ProjectStore.TMP)
            if (tmp.isFile && runCatching { ProjectJson.decode(tmp.readText(Charsets.UTF_8)) }.isSuccess && tmp.renameTo(File(dir, ProjectStore.MAIN))) return@forEach
            if (dir.deleteRecursively()) removed++
        }
        return removed
    }

    /** The ids of every project folder, in the list or in the trash (which may come back), readable or not. */
    fun knownIds(): Set<String> =
        root.listFiles().orEmpty().filter { it.isDirectory }.map { it.name }.toSet() +
            trash.listFiles().orEmpty().filter { it.isDirectory }.map { it.name.substringBefore('@') }

    /**
     * Forgets trashed projects older than [maxAgeMs]. Never a project set aside as unreadable
     * (`-illisible-` in its id) nor one whose file cannot be read: it may be the only copy of
     * someone's work, to be repaired; only « Supprimer définitivement » removes it.
     */
    fun purgeTrash(maxAgeMs: Long, now: Long = System.currentTimeMillis()) {
        val bin = ProjectStore(trash)
        trash.listFiles()?.forEach { f ->
            val at = f.name.substringAfterLast('@').toLongOrNull() ?: return@forEach
            if (now - at <= maxAgeMs) return@forEach
            if (f.name.contains(UNREADABLE) || runCatching { bin.load(f.name) }.isFailure) return@forEach
            f.deleteRecursively()
        }
    }

    /**
     * The project and its sounds as a .zip (project.json + samples/…), for a backup or another
     * phone; a remix's separated song goes with it (stems/<set id>/…). Returns how many sounds the
     * project names that are not on the phone any more (left out: the import goes on without them).
     */
    @Throws(IOException::class)
    fun exportZip(id: String, out: java.io.OutputStream): Int {
        val project = store.load(id) ?: throw IOException(tr("projet introuvable"))
        // The takes kept and the versions' pads go too: « Ramener » a version on the other phone keeps its sounds.
        val used = project.sampleFiles + project.previousTakeFiles + project.versionSampleFiles
        val files = File(store.folder(id), SAMPLES).listFiles()?.filter { it.isFile && it.name in used }?.sortedBy { it.name }.orEmpty()
        java.util.zip.ZipOutputStream(out.buffered()).use { zip ->
            zip.putNextEntry(java.util.zip.ZipEntry(ProjectStore.MAIN))
            zip.write(ProjectJson.encode(project).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            files.forEach { f -> zip.putFile("$SAMPLES/${f.name}", f) }
            stemSetDir(project.remix.setId)?.let { set ->
                // Float WAVs hardly compress: the fastest level, not minutes of work for a few percent.
                zip.setLevel(java.util.zip.Deflater.BEST_SPEED)
                set.listFiles()?.filter { it.isFile && PadSample(it.name, "").isSafe }?.sortedBy { it.name }
                    ?.forEach { f -> zip.putFile("$STEMS/${set.name}/${f.name}", f) }
            }
        }
        return (project.sampleFiles - files.map { it.name }.toSet()).size
    }

    private fun java.util.zip.ZipOutputStream.putFile(name: String, f: File) {
        putNextEntry(java.util.zip.ZipEntry(name))
        f.inputStream().use { it.copyTo(this) }
        closeEntry()
    }

    /** The folder of separated song [setId] on this phone, when there is one (and a safe name). */
    private fun stemSetDir(setId: String?): File? {
        val dir = File(stems ?: return null, setId?.takeIf { PadSample(it, "").isSafe } ?: return null)
        return dir.takeIf { it.isDirectory }
    }

    /**
     * A project .zip made by [exportZip] becomes a new project (never replaces one). Only
     * project.json, plain sample files and the remix's song are read; size limits guard the phone.
     * A sound the .zip lacks is left out of the project (its pad goes back to the kit sound), and
     * counted in the result so the screen can say so.
     */
    @Throws(IOException::class)
    fun importZip(input: java.io.InputStream): ImportedProject {
        val id = newId()
        val dir = store.folder(id)
        // The remix's song is unpacked beside the project first, moved to [stems] at the end.
        val stemsIn = File(dir, ".stems")
        // project.json goes to a file first, then is read as it goes (never whole in memory as text).
        val projectIn = File(dir, ".project.json")
        var project: Project? = null
        var total = 0L
        val seen = HashSet<String>()
        try {
            java.util.zip.ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue
                    val name = entry.name.replace('\\', '/')
                    if (!seen.add(name)) throw IOException(tr("fichier en double dans le .zip"))
                    val parts = name.split('/')
                    val target = when {
                        name == ProjectStore.MAIN -> projectIn.also { it.parentFile.mkdirs() }
                        parts.size == 2 && parts[0] == SAMPLES && PadSample(parts[1], "").isSafe ->
                            File(dir, name).also { it.parentFile.mkdirs() }
                        stems != null && parts.size == 3 && parts[0] == STEMS && parts.drop(1).all { PadSample(it, "").isSafe } ->
                            File(stemsIn, "${parts[1]}/${parts[2]}").also { it.parentFile.mkdirs() }
                        else -> continue  // anything else in the archive is ignored
                    }
                    val buf = ByteArray(64 * 1024)
                    var size = 0L
                    target.outputStream().buffered().use { sink ->
                        while (true) {
                            val n = zip.read(buf)
                            if (n < 0) break
                            size += n
                            total += n
                            if (size > (if (target == projectIn) MAX_PROJECT_JSON else MAX_ZIP_ENTRY) || total > MAX_ZIP_TOTAL) throw IOException(tr("projet trop gros"))
                            sink.write(buf, 0, n)
                        }
                    }
                    if (target == projectIn) project = projectIn.inputStream().buffered().use { ProjectJson.decode(it) }
                }
            }
            // Cleaned first: only plain file names count (a path like ../x is dropped here).
            val read = (project ?: throw IOException(tr("ce .zip n'est pas un projet Pocket Studio"))).sanitized()
            val missing = read.sampleFiles.filterNot { File(dir, "$SAMPLES/$it").isFile }.toSet()
            val p = if (missing.isEmpty()) read else read.withoutSounds(missing)
            store.save(id, p)
            moveStems(stemsIn, p.remix.setId)
            return ImportedProject(id, missing.size)
        } catch (e: Throwable) {
            // Also an OutOfMemoryError: nothing half-imported stays behind, and the app does not crash.
            dir.deleteRecursively()
            // Our own messages are translated; a library's (JSON parser, zip reader) is not shown as is.
            throw if (e.javaClass == IOException::class.java) e else IOException(tr("fichier illisible ou abîmé"), e)
        } finally {
            stemsIn.deleteRecursively()
            projectIn.delete()
        }
    }

    /** The imported remix's song into [stems], unless this phone already has it (the same song gives the same id). */
    private fun moveStems(from: File, setId: String?) {
        val root = stems ?: return
        val set = File(from, setId?.takeIf { PadSample(it, "").isSafe } ?: return)
        if (!set.isDirectory) return
        val target = File(root, set.name)
        if (target.exists()) return
        root.mkdirs()
        // The same file system: a rename. A song that cannot move is not worth failing the import:
        // the Remix tab then says the song is not on the phone, as for an older .zip.
        set.renameTo(target)
    }

    /** "Mon projet 3" (saved in French, see [DefaultNames]): the first free number among the existing names, in any language. */
    fun nextName(prefix: String = "Mon projet", ignoring: String? = null): String {
        val names = list().filter { it.id != ignoring }.map { DefaultNames.shown(it.name) }.toSet()
        return generateSequence(1) { it + 1 }.map { "$prefix $it" }.first { DefaultNames.shown(it) !in names }
    }

    /** [name], or « [name] 2 », « 3 »… when a project already has it (as shown): never two songs alike in the list. */
    fun freeName(name: String): String {
        val names = list().map { DefaultNames.shown(it.name) }.toSet()
        if (DefaultNames.shown(name) !in names) return name
        val base = name.take(MAX_NAME - 4)
        return generateSequence(2) { it + 1 }.map { "$base $it" }.first { DefaultNames.shown(it) !in names }
    }

    /**
     * True when [id] is a blank project nobody edited (only its name may differ from a fresh one) and
     * has no sounds of its own: nothing to keep, so it needs no trash.
     */
    fun isUntouched(id: String): Boolean {
        val p = runCatching { store.load(id) }.getOrNull() ?: return false
        val fresh = Project(name = "").asNewProject().sanitized()
        if (p.copy(name = "") != fresh.copy(name = "")) return false
        return File(store.folder(id), SAMPLES).let { !it.exists() || it.list().isNullOrEmpty() }
    }

    private fun newId() = "p-" + UUID.randomUUID().toString().take(8)

    companion object {
        const val SAMPLES = "samples"
        const val STEMS = "stems"
        const val MAX_NAME = 40
        const val TRASH_DAYS = 30L
        /** A 7-minute stem (the longest song separated) is about 150 MB. */
        const val MAX_ZIP_ENTRY = 256L * 1024 * 1024
        /**
         * Guards memory: project.json is read as it goes, but the project it holds is whole in
         * memory. The fullest project the app allows is about
         * 7 MB compact (29 MB in the indented files of older versions, which still import).
         */
        const val MAX_PROJECT_JSON = 32L * 1024 * 1024
        const val MAX_ZIP_TOTAL = 2048L * 1024 * 1024
        /** In the folder name of a project the app could not read and set aside (`<id>-illisible-<time>`). */
        const val UNREADABLE = "-illisible-"

        /** The name of « the version before the session » of a project named [name] (see [DefaultNames]). */
        fun beforeSessionName(name: String): String = DefaultNames.BEFORE_SESSION.format(name.take(MAX_NAME - 20))
        private val SET_ID = Regex("\"setId\"\\s*:\\s*\"([^\"]+)\"")
    }
}

/** A project in the trash: [entry] is what [ProjectCatalog.restore] takes. */
data class TrashedProject(val entry: String, val name: String, val deletedAt: Long)
