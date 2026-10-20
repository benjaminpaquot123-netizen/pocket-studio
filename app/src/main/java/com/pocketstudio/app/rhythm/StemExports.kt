package com.pocketstudio.app.rhythm

import com.pocketstudio.model.AudioTrack
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.Project
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.partView

/**
 * The files of a « Piste par piste » export: (label, mixer channel) of each strip that sounds in
 * it. A strip muted in the mixer, or with nothing to play in the exported scene (or in any part
 * of the song when [song]), gives no file: an empty WAV only gets in the way. Live pads are only
 * launched by hand, so they never play in an export.
 */
internal fun stemExports(p: Project, song: Boolean): List<Pair<String, Int>> {
    val views = if (song && p.song.isNotEmpty()) p.song.map { p.partView(it) } else listOf(p)
    fun slug(name: String) = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(16)
    val all = listOf(
        "batterie" to 0 to views.any { v -> v.drums.lanes.any { lane -> lane.hits.keys.any { it < v.drums.steps } } },
        "acide" to 1 to views.any { v -> v.acid.steps.take(v.acid.length).any { it.gate } },
        "synthe" to 2 to views.any { v -> v.synthNotes.notes.isNotEmpty() },
        "remix" to 3 to (p.remix.setId != null),
    ) + p.tracks.map { t ->
        // One file per added instrument track, named after it.
        "piste${t.id + 1}-${slug(t.name)}" to InstrumentTrack.mixerChannel(t.id) to
            views.any { v -> v.tracks.firstOrNull { it.id == t.id }?.notes?.notes?.isNotEmpty() == true }
    } + p.audioTracks.map { t ->
        "audio${t.id + 1}-${slug(t.name)}" to AudioTrack.mixerChannel(t.id) to
            (t.clip != null && (!song || p.song.isEmpty() || p.song.any { it.plays(SongPart.audio(t.id)) }))
    }
    return all.filter { (stem, sounds) -> sounds && p.mixer.channels.getOrNull(stem.second)?.mute != true }.map { it.first }
}
