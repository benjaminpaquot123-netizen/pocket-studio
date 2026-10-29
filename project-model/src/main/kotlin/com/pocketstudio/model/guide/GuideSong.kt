package com.pocketstudio.model.guide

import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.AutomationLane
import com.pocketstudio.model.DrumArrangement
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.PartIntents
import com.pocketstudio.model.Project
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.partSteps

/**
 * The song bar by bar, as the Guide reads it: its sections ([GuideSection]: the parts of one section
 * name in a row, its fills and crashes with it), and for each track what sounds in each bar (a hash
 * of the notes or hits it plays there, 0 when silent: muted in the part, nothing written, or its
 * volume curve at zero there). Pure, off the audio thread.
 */
class GuideSong(val project: Project, val tracks: List<GuideTrack> = project.guideTracks()) {
    val stepsPerBar = project.stepsPerBar.coerceAtLeast(1)

    /** The first bar and the length in bars of each song part. */
    val partStarts: IntArray
    val partBars: IntArray
    val bars: Int

    /** Per track key: what it plays in each bar (0: silent). */
    private val content: Map<String, IntArray>

    /** The same, as written: a volume curve at zero does not silence it (the notes are there). */
    private val written: Map<String, IntArray>

    /** The bars where the drums play a fill or a crash scene (left out when Drops are compared). */
    val fillBars: BooleanArray

    val sections: List<GuideSection>

    init {
        val song = project.song
        partStarts = IntArray(song.size)
        partBars = IntArray(song.size)
        var bar = 0
        for (i in song.indices) {
            partStarts[i] = bar
            partBars[i] = ((project.partSteps(song[i]) + stepsPerBar - 1) / stepsPerBar).coerceAtLeast(1)
            bar += partBars[i]
        }
        bars = bar
        val scenes = project.allScenes
        val silences = volumeSilences()
        fillBars = BooleanArray(bars)
        for (i in song.indices) {
            val label = stem(scenes.getOrNull(song[i].sceneFor(SongPart.DRUMS))?.label.orEmpty())
            if (label in DRUM_ONLY) for (b in 0 until partBars[i]) fillBars[partStarts[i] + b] = true
        }
        written = tracks.associate { t -> t.key to contentOf(t, scenes, null) }
        content = tracks.associate { t -> t.key to silences[t.channel].let { mute -> written.getValue(t.key).let { w -> if (mute == null) w else IntArray(bars) { b -> if (mute[b]) 0 else w[b] } } } }
        sections = sectionsOf(scenes)
    }

    /** What [key] plays in [bar] (0: nothing heard). */
    fun at(key: String, bar: Int): Int = content[key]?.getOrNull(bar) ?: 0

    fun sounds(key: String, bar: Int): Boolean = at(key, bar) != 0

    /** What [key]'s notes are in [bar], heard or not (its volume curve aside). */
    fun writtenAt(key: String, bar: Int): Int = written[key]?.getOrNull(bar) ?: 0

    /** The bars [key] sounds in, over the song. */
    fun soundingBars(key: String): Int = content[key]?.count { it != 0 } ?: 0

    /** [key] plays somewhere: in the song, or (no song yet) in some scene. */
    fun plays(key: String): Boolean {
        if (bars > 0) return soundingBars(key) > 0
        return hasNotes(key)
    }

    private fun hasNotes(key: String): Boolean {
        val scenes = project.allScenes
        return when {
            key.startsWith("pad:") -> key.removePrefix("pad:").toIntOrNull()?.let { lane -> scenes.any { s -> s.drums.lanes.getOrNull(lane)?.hits?.keys?.any { it < s.drums.steps } == true } } == true
            key == GuideTrack.DRUMS -> scenes.any { s -> s.drums.lanes.any { l -> l.hits.keys.any { it < s.drums.steps } } }
            key == GuideTrack.ACID -> scenes.any { s -> s.acid.steps.take(s.acid.length).any { it.gate } }
            key.startsWith("inst:") -> key.removePrefix("inst:").toIntOrNull()?.let { id -> scenes.any { clipOf(it, id)?.notes?.isNotEmpty() == true } } == true
            key.startsWith("audio:") -> true
            else -> false
        }
    }

    /** The song's sections of [kind], in order. */
    fun sectionsOf(kind: GuideSection.Kind): List<GuideSection> = sections.filter { it.kind == kind }

    /** The keys sounding in [bar]: the pads count as one, « drums ». */
    fun activeIn(bar: Int): Set<String> = tracks.filter { !it.key.startsWith("pad:") && sounds(it.key, bar) }.mapTo(HashSet()) { it.key }

    /** The most tracks sounding together in [s] (the pads as one). */
    fun fullness(s: GuideSection): Int = (s.start until s.end).maxOfOrNull { activeIn(it).size } ?: 0

    // --- building -----------------------------------------------------------------------------------

    private fun clipOf(s: Scene, id: Int): NoteClip? = if (id == 0) s.synthNotes else s.trackNotes[id]

    /** Per strip: the bars its volume curve holds at (near) zero. */
    private fun volumeSilences(): Map<Int, BooleanArray> {
        if (bars == 0) return emptyMap()
        val out = HashMap<Int, BooleanArray>()
        for (lane in project.automation) {
            val t = lane.target
            if (t.kind != AutoTarget.VOLUME || t.ch == com.pocketstudio.model.Mixer.MASTER) continue
            if (lane.mode != AutomationLane.SONG || lane.songValues.none { it != null }) continue
            val per = lane.pointsPerBar(project.beatsPerBar)
            val curve = lane.songCurve(bars * per)
            val silent = BooleanArray(bars) { b -> (0 until per).all { k -> (curve.getOrNull(b * per + k) ?: 1f) < SILENT } }
            out[t.ch] = out[t.ch]?.let { prev -> BooleanArray(bars) { prev[it] || silent[it] } } ?: silent
        }
        return out
    }

    private fun contentOf(t: GuideTrack, scenes: List<Scene>, silent: BooleanArray?): IntArray {
        val out = IntArray(bars)
        val song = project.song
        val strip = project.mixer.channels.getOrNull(t.channel)
        if (strip?.mute == true) return out
        val partKey = GuideTrack.partKey(t.key) ?: return out
        val barTicks = stepsPerBar * NoteClip.TICKS_PER_STEP
        for (i in song.indices) {
            val part = song[i]
            if (!part.plays(partKey)) continue
            val scene = scenes.getOrNull(part.sceneFor(partKey)) ?: continue
            val from = part.fromBars(partKey)
            for (b in 0 until partBars[i]) {
                val bar = partStarts[i] + b
                if (silent?.getOrNull(bar) == true) continue
                out[bar] = when {
                    t.key.startsWith("pad:") -> padBar(scene, t.key.removePrefix("pad:").toInt(), from + b)
                    t.key == GuideTrack.DRUMS -> scene.drums.lanes.indices.fold(0) { acc, lane -> 31 * acc + padBar(scene, lane, from + b) }
                    t.key == GuideTrack.ACID -> acidBar(scene, from + b)
                    t.key.startsWith("inst:") -> clipOf(scene, t.key.removePrefix("inst:").toInt())?.let { notesBar(it, from + b, barTicks) } ?: 0
                    else -> 1  // an audio track: heard where the song plays it (its clips are not read here)
                }
            }
        }
        return out
    }

    private fun padBar(scene: Scene, lane: Int, bar: Int): Int {
        val d = scene.drums
        val l = d.lanes.getOrNull(lane) ?: return 0
        val loop = l.loopSteps(d.steps).coerceAtLeast(1)
        var h = 0
        var any = false
        for (step in 0 until stepsPerBar) {
            val at = Math.floorMod(bar * stepsPerBar + step, loop)
            val hit = l.hits[at] ?: continue
            if (at >= d.steps) continue
            any = true
            h = 31 * h + step * 7 + (hit.velocity * 20).toInt() + hit.repeats * 1000 + (hit.span - 1) * 100_000
        }
        return if (any) h or 1 else 0
    }

    private fun acidBar(scene: Scene, bar: Int): Int {
        val a = scene.acid
        val loop = a.length.coerceAtLeast(1)
        var h = 0
        var any = false
        for (step in 0 until stepsPerBar) {
            val s = a.steps.getOrNull(Math.floorMod(bar * stepsPerBar + step, loop)) ?: continue
            if (!s.gate) continue
            any = true
            h = 31 * h + step * 131 + s.hashCode()
        }
        return if (any) h or 1 else 0
    }

    private fun notesBar(clip: NoteClip, bar: Int, barTicks: Int): Int {
        if (clip.notes.isEmpty() || clip.ticks <= 0) return 0
        val loopBars = ((clip.ticks + barTicks - 1) / barTicks).coerceAtLeast(1)
        val from = Math.floorMod(bar, loopBars) * barTicks
        val to = from + barTicks
        var h = 0
        var any = false
        for (n in clip.notes.sortedWith(compareBy({ it.start }, { it.pitch }))) {
            if (n.end <= from || n.start >= to) continue
            any = true
            h = 31 * h + ((n.start - from) * 128 + n.pitch) * 997 + n.length
        }
        return if (any) h or 1 else 0
    }

    private fun sectionsOf(scenes: List<Scene>): List<GuideSection> {
        val song = project.song
        if (song.isEmpty()) return emptyList()
        data class Run(val stem: String, val scene: Int, val first: Int, var last: Int)
        val runs = ArrayList<Run>()
        for (i in song.indices) {
            val label = scenes.getOrNull(song[i].scene)?.label.orEmpty()
            val stem = stem(label).ifBlank { "#${song[i].scene}" }
            val last = runs.lastOrNull()
            when {
                stem in DRUM_ONLY && last != null -> last.last = i
                last != null && last.stem == stem -> last.last = i
                else -> runs += Run(stem, song[i].scene, i, i)
            }
        }
        val out = ArrayList<GuideSection>()
        var peaks = 0
        val counts = HashMap<GuideSection.Kind, Int>()
        for (r in runs) {
            var kind = kindOf(r.stem)
            if (kind == GuideSection.Kind.INTRO && peaks > 0) kind = GuideSection.Kind.OUTRO
            if (kind == GuideSection.Kind.PEAK) peaks++
            val start = partStarts[r.first]
            val end = partStarts[r.last] + partBars[r.last]
            val n = counts.getOrDefault(kind, 0)
            counts[kind] = n + 1
            out += GuideSection(kind, r.stem, r.scene, start, end - start, (r.first..r.last).toList(), n)
        }
        return out
    }

    companion object {
        /** Under this, a volume curve is silence. */
        const val SILENT = 0.02f

        /** One-bar drum scenes, part of the section they sit in. */
        private val DRUM_ONLY = setOf(PartIntents.FILL_LABEL, DrumArrangement.CRASH_LABEL)

        /** "Drop 2" → "Drop", "Refrain +" → "Refrain". */
        fun stem(label: String): String {
            val l = label.trim().removeSuffix(Scene.ENERGY_SUFFIX).trim()
            return Regex("""^(.+) \d+$""").matchEntire(l)?.groupValues?.get(1) ?: l
        }

        fun kindOf(stem: String): GuideSection.Kind = when (stem) {
            "Intro" -> GuideSection.Kind.INTRO
            "Fin" -> GuideSection.Kind.OUTRO
            "Couplet", "Groove" -> GuideSection.Kind.VERSE
            "Montée", "Pré-refrain", "Faux drop" -> GuideSection.Kind.BUILD
            "Drop", "Refrain" -> GuideSection.Kind.PEAK
            "Post-refrain" -> GuideSection.Kind.POST
            "Break", "Pont" -> GuideSection.Kind.BREAK
            else -> GuideSection.Kind.OTHER
        }
    }
}

/**
 * A section of the song: [kind], its name ([label], saved in French: « Drop », « Refrain »), the scene
 * it starts with, its bars ([start] until [end]), the song parts in it and which of its kind it is
 * ([occurrence], from 0: the second Drop is 1).
 */
data class GuideSection(val kind: Kind, val label: String, val scene: Int, val start: Int, val bars: Int, val parts: List<Int>, val occurrence: Int) {
    val end: Int get() = start + bars

    enum class Kind { INTRO, VERSE, BUILD, PEAK, POST, BREAK, OUTRO, OTHER }
}
