package com.pocketstudio.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Where a microphone take was recorded from: step [from] of the scene, or of the song when [song]. */
data class TakeAnchor(val song: Boolean = false, val from: Int = 0)

/**
 * Where the clip of a take recorded from [anchor] starts. A retake keeps the start of the clip it
 * replaces ([previous], same anchor, not before the recording began): what was recorded before
 * that step is then cut ([TakeAnchor.from] to the start).
 */
fun takeStart(previous: AudioClip?, anchor: TakeAnchor): Int =
    if (previous != null && previous.song == anchor.song && previous.start >= anchor.from) previous.start else anchor.from

/**
 * A finished microphone take, its sound file already in the project's `samples/`, waiting to be
 * put on audio track [track]. It is written next to the project ([PendingTakes]) before the app
 * puts it on the track, so a take survives the app being closed in the middle.
 */
@Serializable
data class PendingTake(
    val track: Int,
    val file: String,
    val name: String,
    val seconds: Double,
    /** The tempo it was recorded at. */
    val bpm: Double,
    /** A song clip (from "Tout le morceau"), else a scene clip. */
    val song: Boolean = false,
    /** Its first step (see [takeStart]). */
    val start: Int = 0,
    /** The clip file the track had when the take began (null: none): the take only replaces that one. */
    val replaces: String? = null,
    /** A scene take: the scene it was recorded on (it plays in that scene's parts only). */
    val scene: Int? = null,
)

/**
 * [take] on its track: a retake keeps the previous clip's volume, fades, start and « Décaler la
 * prise »; only what depends on its length (the loop, the seconds) and its tempo are new. A new
 * scene take plays once per part. Nothing changes when the take is already there, or when the
 * track has moved on since the take began (another clip than [PendingTake.replaces]: a newer take
 * is never replaced by an older one); a track removed meanwhile comes back.
 */
fun Project.withTake(take: PendingTake): Project {
    if (take.track !in 0 until AudioTrack.MAX) return this
    var p = this
    if (p.audioTracks.none { it.id == take.track }) {
        val color = (0 until InstrumentTrack.COLORS).firstOrNull { c -> p.instrumentTracks.none { it.color == c } && p.audioTracks.none { it.color == c } } ?: 0
        p = p.copy(audioTracks = (p.audioTracks + AudioTrack(take.track, voiceName(p), color)).sortedBy { it.id })
    }
    val track = p.audioTracks.first { it.id == take.track }
    val previous = track.clip
    if (previous?.file == take.file) return p
    if (previous != null && previous.file != take.replaces) return this
    val sameAnchor = previous != null && previous.song == take.song
    val bars = AudioClip.barsFor(take.seconds, take.bpm, beatsPerBar)
    val clip = (previous ?: AudioClip(take.file, take.name)).copy(
        file = take.file,
        name = take.name,
        seconds = take.seconds,
        start = take.start,
        song = take.song,
        loopSteps = bars * stepsPerBar,
        once = if (sameAnchor) previous!!.once else !take.song,
        bpm = take.bpm,
        scene = if (take.song) null else take.scene,
    )
    // The take it replaces stays one touch away (« Prise précédente »).
    return p.withAudioTrack(track.copy(clip = clip, previous = previous ?: track.previous))
}

/** The name of an audio track made for a voice: « Voix », then « Voix 2 »… (saved in French). */
fun voiceName(p: Project): String {
    val names = p.audioTracks.map { it.name }.toSet()
    return (listOf("Voix") + (2..AudioTrack.MAX + 1).map { "Voix $it" }).first { it !in names }
}

/**
 * The audio track the top bar's ● records the microphone on, for a take from [anchor] (scene
 * [scene] shown): the track recorded on last ([last]) when it is empty or its clip is the same
 * passage (a retake), else an empty track. Never another passage's take: null when a track must
 * be added for it (or none can be: all four hold a take).
 */
fun Project.micTrackFor(last: Int?, anchor: TakeAnchor, scene: Int): Int? {
    fun samePassage(c: AudioClip): Boolean =
        if (anchor.song) c.song && partAt(c.start) == partAt(anchor.from) else !c.song && c.belongsTo(scene)
    audioTracks.firstOrNull { it.id == last }?.let { t -> if (t.clip == null || samePassage(t.clip)) return t.id }
    return audioTracks.firstOrNull { it.clip == null }?.id
}

/** The song part playing at song step [step] (the last one past the end; -1 without a song). */
fun Project.partAt(step: Int): Int {
    if (song.isEmpty()) return -1
    var at = 0
    song.forEachIndexed { i, part ->
        at += partSteps(part)
        if (step < at) return i
    }
    return song.lastIndex
}

/** Takes not yet on their track, one small file each in `<project>/takes/`. Blocking (background). */
object PendingTakes {
    const val DIR = "takes"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Written atomically: a crash leaves the old state or the new one, never half a file. */
    fun write(projectFolder: File, take: PendingTake) {
        val dir = File(projectFolder, DIR).apply { mkdirs() }
        val tmp = File(dir, "${take.file}.json.tmp")
        FileOutputStream(tmp).use { out ->
            out.write(json.encodeToString(PendingTake.serializer(), take).toByteArray(Charsets.UTF_8))
            out.fd.sync()
        }
        Files.move(tmp.toPath(), File(dir, "${take.file}.json").toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    /** Every waiting take whose sound file is there, oldest first. */
    fun read(projectFolder: File): List<PendingTake> {
        val files = File(projectFolder, DIR).listFiles { f -> f.name.endsWith(".json") } ?: return emptyList()
        return files.sortedBy { it.lastModified() }.mapNotNull { f ->
            runCatching { json.decodeFromString(PendingTake.serializer(), f.readText(Charsets.UTF_8)) }.getOrNull()
                ?.takeIf { PadSample(it.file, "").isSafe && File(projectFolder, "samples/${it.file}").exists() }
        }
    }

    /** The take of [file] is on its track and saved with the project. */
    fun remove(projectFolder: File, file: String) {
        File(File(projectFolder, DIR), "$file.json").delete()
    }

    /** [takes] forgotten (they are in the saved project); notes written since stay. */
    fun clear(projectFolder: File, takes: List<PendingTake>) {
        takes.forEach { remove(projectFolder, it.file) }
    }

    /** The sound files every note names (whether or not they are there yet): never cleaned up. */
    fun files(projectFolder: File): Set<String> {
        val notes = File(projectFolder, DIR).listFiles { f -> f.name.endsWith(".json") } ?: return emptySet()
        return notes.mapNotNull { f -> runCatching { json.decodeFromString(PendingTake.serializer(), f.readText(Charsets.UTF_8)).file }.getOrNull() }.toSet()
    }
}

/** [takes] put on their tracks, in order. */
fun Project.withTakes(takes: List<PendingTake>): Project = takes.fold(this) { p, t -> p.withTake(t) }
