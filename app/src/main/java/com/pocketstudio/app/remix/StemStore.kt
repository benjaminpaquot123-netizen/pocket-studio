package com.pocketstudio.app.remix

import android.content.Context
import com.pocketstudio.app.projects.projectCatalog
import com.pocketstudio.separation.Demucs
import com.pocketstudio.model.i18n.tr
import org.json.JSONObject
import java.io.File

/** What the analysis found: [bpm] 0 when unknown, [root] 0 = Do … 11 = Si or null. */
data class SongInfo(val bpm: Double = 0.0, val firstBeat: Double = 0.0, val root: Int? = null, val minor: Boolean = false) {
    /** "La mineur": note names as Réglages › Notation says, the words in the interface's language. */
    val keyLabel: String? get() = root?.let {
        val note = com.pocketstudio.model.NoteNames.key(it, com.pocketstudio.app.settings.SettingsStore.state.value.englishNotes)
        if (minor) tr("%s mineur", note) else tr("%s majeur", note)
    }
}

/**
 * One separated song: four 32-bit float stereo WAVs at 44.1 kHz in [dir]. [usedBy]: names of the
 * saved projects that play it along (filled by [StemStore.list]).
 */
data class StemSet(
    val dir: File,
    val name: String,
    val seconds: Double,
    val createdAt: Long,
    val info: SongInfo = SongInfo(),
    val usedBy: List<String> = emptyList(),
) {
    val id: String get() = dir.name
    fun stem(name: String) = File(dir, "$name.wav")
}

/**
 * Finished separations live in files/stems/<key>, where the key is the SHA-256 of the decoded
 * audio plus the model version: the same song is never separated twice. A float stem weighs about
 * 21 MB per minute, so only the last [KEEP] songs are kept, plus every song a project plays along.
 */
object StemStore {
    const val KEEP = 3

    /** A stem's name ([Demucs.STEMS]) in the interface's language. */
    fun label(stem: String): String = when (stem) {
        "drums" -> tr("Batterie")
        "bass" -> tr("Basse")
        "other" -> tr("Autres")
        "vocals" -> tr("Voix")
        else -> stem
    }

    private fun root(context: Context) = File(context.filesDir, "stems")

    fun dirFor(context: Context, key: String) = File(root(context), key)

    /** Blocking (reads every project to know which sets are in use): call it off the main thread. */
    fun list(context: Context): List<StemSet> {
        val users = runCatching { projectCatalog(context).stemSetUsers() }.getOrDefault(emptyMap())
        return root(context).listFiles().orEmpty().mapNotNull(::read).sortedByDescending { it.createdAt }
            .map { it.copy(usedBy = users[it.id].orEmpty()) }
    }

    fun read(dir: File): StemSet? {
        val meta = File(dir, META)
        if (!dir.isDirectory || !meta.isFile || Demucs.STEMS.any { !File(dir, "$it.wav").isFile }) return null
        return runCatching {
            val json = JSONObject(meta.readText())
            val info = SongInfo(
                json.optDouble("bpm", 0.0),
                json.optDouble("firstBeat", 0.0),
                json.optInt("root", -1).takeIf { it in 0..11 },
                json.optBoolean("minor", false),
            )
            StemSet(dir, json.getString("name"), json.getDouble("seconds"), json.getLong("createdAt"), info)
        }.getOrNull()
    }

    /** Marks [dir] complete: the meta file is written last, so a half-written folder never lists. */
    fun commit(dir: File, name: String, seconds: Double, info: SongInfo): StemSet {
        val set = StemSet(dir, name, seconds, System.currentTimeMillis(), info)
        val tmp = File(dir, "$META.tmp")
        tmp.writeText(
            JSONObject().put("name", name).put("seconds", seconds).put("createdAt", set.createdAt)
                .put("bpm", info.bpm).put("firstBeat", info.firstBeat).put("root", info.root ?: -1).put("minor", info.minor)
                .toString(),
        )
        if (!tmp.renameTo(File(dir, META))) throw java.io.IOException(tr("enregistrement impossible"))
        return set
    }

    /** Marks [set] as just used, so pruning keeps it. */
    fun touch(set: StemSet): StemSet = runCatching { commit(set.dir, set.name, set.seconds, set.info) }.getOrDefault(set)

    /** The set the project plays along (never pruned). */
    fun pin(context: Context, id: String?) {
        val f = pinFile(context)
        if (id == null) f.delete() else runCatching { f.writeText(id) }
    }

    private fun pinFile(context: Context) = File(context.filesDir, "stems-pinned.txt")

    /**
     * Ids of the sets a project needs: those saved projects (trashed ones included) play along, and
     * the pinned one (the open project, maybe not saved yet). Null when the projects cannot be read.
     */
    fun inUse(context: Context): Set<String>? {
        val users = runCatching { projectCatalog(context).stemSetUsers() }.getOrNull() ?: return null
        return users.keys + listOfNotNull(pinned(context))
    }

    private fun pinned(context: Context) = runCatching { pinFile(context).readText().trim() }.getOrNull()?.ifEmpty { null }

    /** Removes broken folders and everything beyond the [KEEP] most recent sets, except the sets in use. */
    fun prune(context: Context) {
        // Projects unreadable as a whole (storage error): delete nothing rather than a song in use.
        prune(root(context), inUse(context) ?: return)
    }

    internal fun prune(root: File, inUse: Set<String>) {
        val all = root.listFiles().orEmpty()
        val good = all.mapNotNull(::read).sortedByDescending { it.createdAt }
        val keep = (good.take(KEEP) + good.filter { it.id in inUse }).map { it.dir }.toSet()
        all.filter { it !in keep }.forEach { it.deleteRecursively() }
    }

    /** Deletes [set] unless a project uses it; returns false (nothing deleted) when one does. */
    fun delete(context: Context, set: StemSet): Boolean {
        val used = inUse(context) ?: return false
        if (set.id in used) return false
        set.dir.deleteRecursively()
        return true
    }

    private const val META = "stems.json"
}
