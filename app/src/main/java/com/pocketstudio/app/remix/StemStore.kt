package com.pocketstudio.app.remix

import android.content.Context
import com.pocketstudio.separation.Demucs
import org.json.JSONObject
import java.io.File

/** What the analysis found: [bpm] 0 when unknown, [root] 0 = Do … 11 = Si or null. */
data class SongInfo(val bpm: Double = 0.0, val firstBeat: Double = 0.0, val root: Int? = null, val minor: Boolean = false) {
    val keyLabel: String? get() = root?.let { "${NOTE_NAMES[it]} ${if (minor) "mineur" else "majeur"}" }

    companion object {
        val NOTE_NAMES = listOf("Do", "Do#", "Ré", "Mib", "Mi", "Fa", "Fa#", "Sol", "Lab", "La", "Sib", "Si")
    }
}

/** One separated song: four 32-bit float stereo WAVs at 44.1 kHz in [dir]. */
data class StemSet(val dir: File, val name: String, val seconds: Double, val createdAt: Long, val info: SongInfo = SongInfo()) {
    val id: String get() = dir.name
    fun stem(name: String) = File(dir, "$name.wav")
}

/**
 * Finished separations live in files/stems/<key>, where the key is the SHA-256 of the decoded
 * audio plus the model version: the same song is never separated twice. A float stem weighs about
 * 21 MB per minute, so only the last [KEEP] songs are kept.
 */
object StemStore {
    const val KEEP = 3

    /** French names, in [Demucs.STEMS] order. */
    val LABELS = mapOf("drums" to "Batterie", "bass" to "Basse", "other" to "Autres", "vocals" to "Voix")

    private fun root(context: Context) = File(context.filesDir, "stems")

    fun dirFor(context: Context, key: String) = File(root(context), key)

    fun list(context: Context): List<StemSet> =
        root(context).listFiles().orEmpty().mapNotNull(::read).sortedByDescending { it.createdAt }

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
        if (!tmp.renameTo(File(dir, META))) throw java.io.IOException("rename failed")
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

    /** Removes broken folders and everything beyond the [KEEP] most recent sets, except the pinned one. */
    fun prune(context: Context) {
        val all = root(context).listFiles().orEmpty()
        val good = all.mapNotNull(::read).sortedByDescending { it.createdAt }
        val pinned = runCatching { pinFile(context).readText().trim() }.getOrNull()
        val keep = (good.take(KEEP).map { it.dir } + good.filter { it.id == pinned }.map { it.dir }).toSet()
        all.filter { it !in keep }.forEach { it.deleteRecursively() }
    }

    fun delete(set: StemSet) {
        set.dir.deleteRecursively()
    }

    private const val META = "stems.json"
}
