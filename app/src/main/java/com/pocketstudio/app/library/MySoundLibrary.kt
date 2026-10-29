package com.pocketstudio.app.library

import android.content.Context
import com.pocketstudio.app.audio.FloatWav
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.report.AppLog
import com.pocketstudio.model.MySound
import com.pocketstudio.model.MySoundStore
import com.pocketstudio.model.i18n.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * « Ma bibliothèque de sons » (owner, 2026-10-29): the sounds the user kept, for every project, in
 * `files/mysounds/` ([MySoundStore]). A sound used in a project is copied into it like a pack's
 * sound (a project never depends on the library: deleting one never breaks the other, and a .zip
 * carries the copy). Every call reads or writes off the main thread; [sounds] follows the disk.
 */
object MySoundLibrary {
    private val _sounds = MutableStateFlow<List<MySound>>(emptyList())
    val sounds: StateFlow<List<MySound>> = _sounds.asStateFlow()

    private var store: MySoundStore? = null

    @Synchronized
    fun store(context: Context): MySoundStore {
        val dir = File(context.applicationContext.filesDir, "mysounds")  // MySoundStore.DIR
        return store?.takeIf { it.dir == dir } ?: MySoundStore(dir).also { store = it }
    }

    /** Reads the list again (and clears what an interrupted keep left behind). */
    suspend fun refresh(context: Context): List<MySound> = withContext(Dispatchers.IO) {
        val s = store(context)
        runCatching { s.cleanUp() }
        s.list().also { _sounds.value = it }
    }

    /**
     * Keeps [pcm] as a new sound: its audio written first (through a .tmp), then listed. Its name
     * is made unique. Throws when the phone is full or the list cannot be written.
     */
    suspend fun keep(
        context: Context, pcm: Pcm, name: String, kind: String,
        bpm: Double = 0.0, root: Int? = null, minor: Boolean = false, origin: String = "",
    ): MySound = withContext(Dispatchers.IO) {
        val s = store(context)
        val file = s.newFile()
        try {
            FloatWav.write(file, pcm)
            val kept = s.add(
                MySound(
                    id = file.nameWithoutExtension, name = name, kind = kind, file = file.name, seconds = pcm.seconds,
                    bpm = bpm, root = root ?: -1, minor = minor, createdAt = System.currentTimeMillis(), origin = origin,
                ),
            )
            AppLog.add("Ma bibliothèque : « ${kept.name} » gardé (${kept.kind}, ${"%.1f".format(kept.seconds)} s)")
            _sounds.value = s.list()
            kept
        } catch (e: Throwable) {
            file.delete()
            throw e
        }
    }

    suspend fun rename(context: Context, id: String, name: String) = edit(context) { it.rename(id, name) }

    suspend fun setKind(context: Context, id: String, kind: String) = edit(context) { it.setKind(id, kind) }

    suspend fun delete(context: Context, id: String) = edit(context) { it.delete(id) }

    private suspend fun edit(context: Context, change: (MySoundStore) -> Unit) = withContext(Dispatchers.IO) {
        val s = store(context)
        runCatching { change(s) }.onFailure { AppLog.add("Ma bibliothèque : modification impossible (${it.message})") }
        _sounds.value = s.list()
    }

    /** The sound's audio (null when it is gone or unreadable). */
    suspend fun load(context: Context, id: String): Pcm? = withContext(Dispatchers.IO) {
        val s = store(context)
        val sound = s.list().firstOrNull { it.id == id } ?: return@withContext null
        val file = s.file(sound) ?: return@withContext null
        runCatching { FloatWav.read(file) }.onFailure { AppLog.add("Ma bibliothèque : « ${sound.name} » illisible (${it.message})") }.getOrNull()
    }
}

/** What is said once a sound is kept: its name, its family, where to find it. */
fun keptNotice(kept: MySound): String = tr(
    "« %1\$s » est dans ta bibliothèque (%2\$s) : tu le retrouves dans « Changer le son », pour tous tes projets.",
    com.pocketstudio.model.DefaultNames.shownSample(kept.name),
    kindName(kept.kind),
)

/** A family's name in the interface's language (« Voix », « Boucles »). */
fun kindName(kind: String): String {
    val label = com.pocketstudio.model.SoundPacks.KIND_LABELS[kind] ?: return kind
    return tr(label)
}
