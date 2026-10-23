package com.pocketstudio.app.rhythm

import com.pocketstudio.model.AudioTrack
import com.pocketstudio.model.AutoTarget
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
    val tracks = stripSounds(p, song).filter { (stem, sounds) -> sounds && p.mixer.channels.getOrNull(stem.second)?.mute != true }.map { it.first }
    // The Live page's own sounds (riser, downlifter, the build-up's noise) come from no track: each
    // track's file leaves them out, and they get a file of their own, so the files add up to the mix.
    return if (tracks.isNotEmpty() && hasLiveNoises(p, song)) tracks + (LIVE_FX_STEM_LABEL to LIVE_FX_STEM) else tracks
}

/**
 * Each track's strip as (file label, mixer channel), and whether it has something to play in the
 * scene (or in any part of the song when [song]), whatever the mixer does with it. Also read by
 * « Prêt à partager ? »: a track muted in the mixer although it has notes.
 */
internal fun stripSounds(p: Project, song: Boolean): List<Pair<Pair<String, Int>, Boolean>> {
    val views = if (song && p.song.isNotEmpty()) p.song.map { p.partView(it) } else listOf(p)
    fun slug(name: String) = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(16)
    return listOf(
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
}

/** The « Effets live » file's channel in [stemExports]: no strip, the Live page's own sounds alone. */
internal const val LIVE_FX_STEM = -1
internal const val LIVE_FX_STEM_LABEL = "effets-live"

/** The Live params that make a sound of their own (not a change of the mix). */
private val LIVE_NOISES = setOf(AutoTarget.LIVE_RISER, AutoTarget.LIVE_DOWNLIFTER, AutoTarget.LIVE_BUILD_UP)

/** Whether a lane of [p] plays the riser, the downlifter or the build-up in the export ([song]: the whole song). */
internal fun hasLiveNoises(p: Project, song: Boolean): Boolean = p.automation.any { lane ->
    lane.target.kind == AutoTarget.LIVE && lane.target.param in LIVE_NOISES && (
        lane.values.any { (it ?: 0f) > 0f } || lane.sceneValues.values.any { v -> v.any { (it ?: 0f) > 0f } } ||
            ((song || !lane.songOnly) && lane.songValues.any { (it ?: 0f) > 0f })
        )
}
