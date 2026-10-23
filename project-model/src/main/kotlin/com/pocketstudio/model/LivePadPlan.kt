package com.pocketstudio.model

/**
 * A live pad prepared from the song: track [key]'s part of scene [scene] alone (null: the whole
 * scene, every track), rendered over [bars] bars, looping or played once ([oneShot]), named [name]
 * (a name the app gives: saved in French, shown translated by [DefaultNames]).
 */
data class LivePadSpec(val key: String?, val scene: Int, val bars: Int, val oneShot: Boolean, val name: String)

/**
 * What a song made by the assistant puts on its live pads, so « Pads live » plays the song from the
 * first touch: a loop of each track (drums, 303, bass, chords, lead…) taken from the scene where it
 * plays fullest (the drop), the riser's hit once (« Impact »: a one-shot from the Drop's first beat,
 * where the song opens its filter), then the whole fullest scene as one loop. In track order, as
 * many as the pads shown ([LivePads.size]).
 */
object LivePadPlan {
    /** A loop lasts its track's own loop, up to this many bars. */
    const val MAX_LOOP_BARS = 4

    /** A one-shot (the riser's impact) is taken from its scene, up to this many bars (its silent end is cut). */
    const val MAX_ONE_SHOT_BARS = 8

    fun of(p: Project, max: Int = p.live.size): List<LivePadSpec> {
        val count = p.allScenes.size
        if (count == 0 || max <= 0) return emptyList()
        val keys = p.partKeys.filter { it == SongPart.DRUMS || it == SongPart.ACID || it.startsWith("inst:") }
        fun filled(scene: Int) = keys.count { p.cellHasContent(it, scene) }
        // How long the song plays each scene: on a tie of fullness, the one heard most.
        val heard = HashMap<Int, Int>()
        p.song.forEach { part -> heard[part.scene] = (heard[part.scene] ?: 0) + p.partSteps(part) }
        val order = compareBy<Int>({ filled(it) }, { heard[it] ?: 0 }, { -it })
        val fullest = (0 until count).maxWith(order)
        if (filled(fullest) == 0) return emptyList()
        val out = ArrayList<LivePadSpec>()
        for (key in keys) {
            if (out.size >= max) break
            val scenes = (0 until count).filter { p.cellHasContent(key, it) }
            if (scenes.isEmpty()) continue
            val once = isOneShot(p, key)
            val scene = when {
                // The riser where the song opens it most: its hit on the Drop (an impact).
                once -> scenes.maxWith(compareBy<Int> { openness(p, key, it) }.thenBy { -it })
                fullest in scenes -> fullest
                else -> scenes.maxWith(order)
            }
            val bars = if (once) wholeBars(p.sceneSteps(scene), p.stepsPerBar).coerceIn(1, MAX_ONE_SHOT_BARS)
            else wholeBars(p.slotLoopSteps(key, scene), p.stepsPerBar).coerceIn(1, MAX_LOOP_BARS)
            out += LivePadSpec(key, scene, bars, once, if (once) IMPACT else nameOf(p, key))
        }
        // Several tracks: the whole scene too (one track alone would only play itself again).
        if (out.size in 2 until max) {
            val bars = wholeBars(p.sceneSteps(fullest), p.stepsPerBar).coerceIn(1, MAX_LOOP_BARS)
            out += LivePadSpec(null, fullest, bars, false, if (bars == 1) "Morceau 1 mesure" else "Morceau $bars mesures")
        }
        return out
    }

    /** The riser rises once: a pad that plays it from the start at each touch. */
    fun isOneShot(p: Project, key: String): Boolean = key.startsWith("inst:") &&
        p.instrumentTracks.firstOrNull { SongPart.inst(it.id) == key }?.name.let { it == StarterWizard.RISER_TRACK || it == StarterWizard.OLD_RISER_TRACK }

    /** The riser's pad: its hit with the filter open (the same word in English and Dutch). */
    const val IMPACT = "Impact"

    /** How far the song's curves on track [key]'s strip are open where the song first plays [scene] (0: none). */
    private fun openness(p: Project, key: String, scene: Int): Float {
        val id = key.removePrefix("inst:").toIntOrNull() ?: return 0f
        val ch = InstrumentTrack.mixerChannel(id)
        val all = p.copy(automation = p.automation.map { if (it.songOnly) it.copy(songOnly = false) else it })
        return all.songCurvesAt(scene).filterKeys { all.automation.getOrNull(it)?.target?.ch == ch }.values.maxOrNull() ?: 0f
    }

    private fun nameOf(p: Project, key: String): String = when {
        key == SongPart.DRUMS -> "Batterie"
        key == SongPart.ACID -> "Basse acide"
        else -> p.instrumentTracks.firstOrNull { SongPart.inst(it.id) == key }?.name ?: "Piste"
    }

    private fun wholeBars(steps: Int, stepsPerBar: Int): Int {
        val bar = stepsPerBar.coerceAtLeast(1)
        return (steps.coerceAtLeast(1) + bar - 1) / bar
    }
}

/**
 * The project as it sounds playing only track [key] in scene [scene] (null: every track of the
 * scene): the other tracks' notes and hits taken out, the audio clips of the others left out, no
 * live pad. The song's curves are held where the song first plays that scene (a riser's filter
 * open on the Drop's first beat: its impact), set as the settings themselves, since a scene
 * rendered on its own does not follow the song. Rendered offline, it is that track's loop, with
 * no loudness target: a pad then plays through the engine's master chain, where the song's
 * target is applied once (brought to the song's loudness alone, a hi-hat loop would be as loud as
 * the whole mix, then mastered again live).
 */
fun Project.stemView(key: String?, scene: Int): Project {
    val muted = if (key == null) emptySet() else partKeys.filter { it != key }.toSet()
    var v = partView(SongPart(scene = scene, muted = muted))
    val held = v.copy(automation = v.automation.map { if (it.songOnly) it.copy(songOnly = false) else it }).songCurvesAt(scene)
    held.forEach { (lane, value) -> v.automation.getOrNull(lane)?.let { v = v.withSetting(it.target, value) } }
    v = v.copy(automation = v.automation.filter { it.mode != AutomationLane.SONG })
    return v.copy(
        audioTracks = v.audioTracks.map { t -> if (key == null || SongPart.audio(t.id) == key) t else t.copy(clip = null) },
        live = LivePads(size = live.size),
        livePadsPending = false,
        mixer = v.mixer.copy(target = LoudnessTarget.OFF),
    )
}
