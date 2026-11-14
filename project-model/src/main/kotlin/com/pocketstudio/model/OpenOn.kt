package com.pocketstudio.model

/**
 * Where a project opens (owner's choice 1A, 2026-10-30): on its song (« Mon morceau ») when the song
 * has something to hear, else on the drum pads, where an empty project starts.
 *
 * Something to hear: a song part whose scene holds a note, a drum hit, a 303 step or a chord; an
 * audio clip (recorded or imported); a separated song (Remix). Song parts over empty scenes, a beat
 * not placed in the song yet, a drum kit or sounds chosen without a hit: nothing to hear in the song,
 * the project opens on the pads (where that beat is).
 */
object OpenOn {
    fun song(p: Project): Boolean {
        val scenes = p.allScenes
        return p.song.any { part -> scenes.getOrNull(part.scene)?.isEmpty == false } ||
            p.audioTracks.any { it.clip != null } ||
            p.remix.setId != null
    }

    fun pads(p: Project): Boolean = !song(p)
}
