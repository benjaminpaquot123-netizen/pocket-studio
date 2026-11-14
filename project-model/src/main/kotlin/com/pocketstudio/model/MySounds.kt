package com.pocketstudio.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

/**
 * A sound the user kept (« Garder dans ma bibliothèque »): a piece of a separated song, a pad's
 * own sound, an imported file. Shared by every project, shown with the app's sounds of the same
 * [kind] ([SoundPacks.KIND_LABELS] id: « vocal », « loop », « kick »…) with an « À moi » badge.
 * [file] is a plain name in the library's folder; [bpm] 0 and [root] -1 when unknown.
 */
@Serializable
data class MySound(
    val id: String,
    val name: String,
    val kind: String,
    val file: String,
    val seconds: Double = 0.0,
    val bpm: Double = 0.0,
    val root: Int = -1,
    val minor: Boolean = false,
    val createdAt: Long = 0L,
    /** Where it came from, in French (« Remix : Ma chanson », « Pad 3 »), for the tile's subtitle. */
    val origin: String = "",
) {
    fun sanitized(): MySound = copy(
        name = name.trim().take(MySounds.MAX_NAME).ifEmpty { "Son" },
        kind = kind.takeIf { it in SoundPacks.KIND_LABELS } ?: MySounds.DEFAULT_KIND,
        seconds = seconds.takeIf { it.isFinite() }?.coerceIn(0.0, 3600.0) ?: 0.0,
        bpm = bpm.takeIf { it.isFinite() }?.coerceIn(0.0, Project.MAX_TEMPO) ?: 0.0,
        root = root.takeIf { it in 0..11 } ?: -1,
        origin = origin.take(80),
    )

    /** Only an id and a file name the library made itself, never a path. */
    val isSafe: Boolean get() = id.matches(SAFE) && file.matches(SAFE) && file.endsWith(".wav") && !file.startsWith(".")

    private companion object {
        val SAFE = Regex("[A-Za-z0-9_-][A-Za-z0-9._-]{0,79}")
    }
}

@Serializable
private data class MySoundIndex(val version: Int = 1, val sounds: List<MySound> = emptyList())

/** Pure rules of the personal library: the kind a new sound gets, its default name. */
object MySounds {
    const val MAX_NAME = 40
    /** A kind for a sound nothing else tells about. */
    const val DEFAULT_KIND = "fx"
    /** Longer than this, a drum or a melodic piece is a loop rather than a hit. */
    const val LOOP_SECONDS = 1.5

    /** The kinds a kept sound can be filed under, in the order the edit sheet offers them. */
    val KINDS: List<String> = listOf("vocal", "loop", "kick", "snare", "clap", "hat", "open_hat", "perc", "tom", "ride", "808", "bass", "stab", "pad", "lead", "pluck", "keys", "mallet", "foley", "fx")

    /** The kind of a piece of stem [stem] (0 drums, 1 bass, 2 other, 3 voice) lasting [seconds]. */
    fun kindOfStem(stem: Int, seconds: Double): String = when (stem) {
        0 -> if (seconds > LOOP_SECONDS) "loop" else "perc"
        1 -> "bass"
        2 -> if (seconds > LOOP_SECONDS) "loop" else "stab"
        3 -> "vocal"
        else -> DEFAULT_KIND
    }

    /** The kind of a pad's sound: its pad's kind ([padKind]) for a hit, a loop when it lasts. */
    fun kindOfPad(padKind: String?, seconds: Double, name: String): String = when {
        name.startsWith("Voix") -> "vocal"
        seconds > LOOP_SECONDS + 0.5 -> "loop"
        padKind != null && padKind in SoundPacks.KIND_LABELS -> padKind
        else -> DEFAULT_KIND
    }

    /** The family of drum pad [lane]'s own voice (pad 1 a kick, pad 3 a snare…), as the library opens on it. */
    fun padKind(lane: Int): String = when (lane) {
        0, 1 -> "kick"
        2, 3, 10 -> "snare"
        4 -> "clap"
        5, 13 -> "hat"
        6, 12 -> "open_hat"
        else -> "perc"
    }

    /** [base], or « [base] 2 », « [base] 3 »… the first one not in [names]. */
    fun freeName(base: String, names: Collection<String>): String {
        val whole = base.trim().take(MAX_NAME).ifEmpty { "Son" }
        if (whole !in names) return whole
        val b = whole.take(MAX_NAME - 4).trimEnd()
        var n = 2
        while ("$b $n" in names) n++
        return "$b $n"
    }
}

/**
 * The personal library on disk: `index.json` and one 32-bit float WAV per sound in [dir]. The index
 * is written through a flushed `.tmp`, then renamed; a sound's file is written (by the caller, at
 * [newFile]) before its entry is added, so a sound listed always has its file. A file nobody lists
 * (the app stopped between the two) is removed after an hour. An index that cannot be read is set
 * aside (`index-illisible-<time>.json`), never written over. Calls are serialised on this object.
 */
class MySoundStore(val dir: File) {
    private val index = File(dir, INDEX)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; coerceInputValues = true }

    /** False after an unreadable index could not be set aside: nothing is written then. */
    var writable = true
        private set

    @Synchronized
    fun list(): List<MySound> = read().filter { File(dir, it.file).isFile }

    /** Where to write a new sound's audio before [add] (a fresh name, never an existing file). */
    @Synchronized
    fun newFile(): File {
        dir.mkdirs()
        return File(dir, "${UUID.randomUUID().toString().replace("-", "").take(16)}.wav")
    }

    /** Lists [sound], whose audio is already at [newFile]'s [MySound.file]; the sound as kept. */
    @Synchronized
    @Throws(IOException::class)
    fun add(sound: MySound): MySound {
        val s = sound.sanitized()
        require(s.isSafe) { "nom de fichier refusé" }
        if (!File(dir, s.file).isFile) throw IOException("son absent : ${s.file}")
        val all = read()
        val kept = s.copy(name = MySounds.freeName(s.name, all.map { it.name }))
        write(all.filter { it.id != kept.id } + kept)
        return kept
    }

    @Synchronized
    @Throws(IOException::class)
    fun rename(id: String, name: String): MySound? = change(id) { it.copy(name = name) }

    @Synchronized
    @Throws(IOException::class)
    fun setKind(id: String, kind: String): MySound? = change(id) { it.copy(kind = kind) }

    /** Removes [id] from the list, then its file (projects that use it keep their own copy). */
    @Synchronized
    @Throws(IOException::class)
    fun delete(id: String): Boolean {
        val all = read()
        val gone = all.firstOrNull { it.id == id } ?: return false
        write(all - gone)
        File(dir, gone.file).delete()
        return true
    }

    @Synchronized
    fun file(sound: MySound): File? = File(dir, sound.file).takeIf { sound.isSafe && it.isFile }

    /**
     * Files no entry names, untouched for [STALE_MS] (a sound written while the app was stopped
     * before it was listed), and `.tmp` files as old: removed. Listed entries are never touched.
     */
    @Synchronized
    fun cleanUp(now: Long = System.currentTimeMillis()) {
        if (!writable) return
        val listed = read().map { it.file }.toSet() + INDEX
        dir.listFiles().orEmpty().filter {
            it.isFile && it.name !in listed && !it.name.startsWith("index-illisible") && it.lastModified() < now - STALE_MS
        }.forEach { it.delete() }
    }

    private fun change(id: String, edit: (MySound) -> MySound): MySound? {
        val all = read()
        val old = all.firstOrNull { it.id == id } ?: return null
        var new = edit(old).sanitized()
        if (new.name != old.name) new = new.copy(name = MySounds.freeName(new.name, all.filter { it.id != id }.map { it.name }))
        write(all.map { if (it.id == id) new else it })
        return new
    }

    private fun read(): List<MySound> {
        if (!index.exists()) return emptyList()
        return try {
            json.decodeFromString(MySoundIndex.serializer(), index.readText(Charsets.UTF_8)).sounds
                .map { it.sanitized() }.filter { it.isSafe }.distinctBy { it.id }
        } catch (e: Exception) {
            val aside = File(dir, "index-illisible-${System.currentTimeMillis()}.json")
            if (!index.renameTo(aside)) writable = false
            emptyList()
        }
    }

    private fun write(sounds: List<MySound>) {
        if (!writable) throw IOException("index illisible et pas mis de côté : pas réécrit")
        dir.mkdirs()
        val tmp = File(dir, "$INDEX.tmp")
        FileOutputStream(tmp).use { out ->
            out.write(json.encodeToString(MySoundIndex.serializer(), MySoundIndex(sounds = sounds)).toByteArray(Charsets.UTF_8))
            out.fd.sync()
        }
        if (!tmp.renameTo(index)) throw IOException("renommage impossible")
    }

    companion object {
        const val INDEX = "index.json"
        /** The library's folder under the app's files. */
        const val DIR = "mysounds"
        const val STALE_MS = 3_600_000L
    }
}
