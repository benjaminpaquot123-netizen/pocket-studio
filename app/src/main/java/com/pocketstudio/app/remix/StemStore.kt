package com.pocketstudio.app.remix

import android.content.Context
import com.pocketstudio.separation.Demucs
import org.json.JSONObject
import java.io.File

/** One separated song: four 32-bit float stereo WAVs at 44.1 kHz in [dir]. */
data class StemSet(val dir: File, val name: String, val seconds: Double, val createdAt: Long) {
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
            StemSet(dir, json.getString("name"), json.getDouble("seconds"), json.getLong("createdAt"))
        }.getOrNull()
    }

    /** Marks [dir] complete: the meta file is written last, so a half-written folder never lists. */
    fun commit(dir: File, name: String, seconds: Double): StemSet {
        val set = StemSet(dir, name, seconds, System.currentTimeMillis())
        val tmp = File(dir, "$META.tmp")
        tmp.writeText(JSONObject().put("name", name).put("seconds", seconds).put("createdAt", set.createdAt).toString())
        tmp.renameTo(File(dir, META))
        return set
    }

    /** Removes broken folders and everything beyond the [KEEP] most recent sets. */
    fun prune(context: Context) {
        val all = root(context).listFiles().orEmpty()
        val good = all.mapNotNull(::read).sortedByDescending { it.createdAt }
        val keep = good.take(KEEP).map { it.dir }.toSet()
        all.filter { it !in keep }.forEach { it.deleteRecursively() }
    }

    fun delete(set: StemSet) {
        set.dir.deleteRecursively()
    }

    private const val META = "stems.json"
}
