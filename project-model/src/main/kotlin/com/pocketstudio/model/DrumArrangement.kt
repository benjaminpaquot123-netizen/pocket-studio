package com.pocketstudio.model

import com.pocketstudio.model.StarterWizard.Style
import com.pocketstudio.model.SongShape.Sec

/**
 * The drummer across the song (plan « son pro », chantier S3): a scene's beat loops its 4 bars,
 * so what happens once every 8 or 16 bars cannot live in it. Here those bars become one-bar song
 * parts whose drums play a scene of their own (as the part sheet's « Fill à la fin » does, see
 * [fillAtEnd]): a bigger fill on the last bar of each 8 (two beats) and of each 16 or section
 * (two beats, down the toms), the crash on the first bar of each 16 of a Drop (or a Refrain) and
 * of a Break, and never on every loop. The other tracks carry on as they were.
 *
 * Identical bars share their scene; when the scenes run short (24, a few kept free for the user)
 * the least important ones are left out: first the crashes, then the fills at the 16, then at the 8.
 */
object DrumArrangement {
    /** Scene slots left free for the user (a « Variation », a « Break » of the part sheet). */
    const val FREE_SLOTS = 3

    /** The label of the one-bar scene that plays a crash (saved in French, translated at display). */
    const val CRASH_LABEL = "Crash"

    private enum class Kind { Crash, Fill16, Fill8 }

    private data class Mark(val bar: Int, val kind: Kind)

    /** The song's stretches (consecutive blocks of one kind), with their first bar. */
    private data class Stretch(val sec: Sec, val start: Int, val bars: Int, val scenes: List<SongShape.Block>)

    fun withFillsAndCrashes(project: Project, shape: SongShape.Shape, style: Style, feel: DrumHumanizer.Feel, seed: Long, crashLane: Int?): Project {
        if (project.song.isEmpty() || project.allScenes.none { s -> s.drums.lanes.any { it.hits.isNotEmpty() } }) return project
        val stretches = ArrayList<Stretch>()
        var bar = 0
        for (b in shape.blocks) {
            val last = stretches.lastOrNull()
            if (last != null && last.sec == b.sec) stretches[stretches.size - 1] = last.copy(bars = last.bars + b.bars, scenes = last.scenes + b)
            else stretches += Stretch(b.sec, bar, b.bars, listOf(b))
            bar += b.bars
        }
        if (bar != project.songBars) return project
        // The bars of a block that rolls, cuts its bass or ends on silence are left as they are (the Montée, the fake drop).
        val quiet = HashSet<Int>()
        var at = 0
        for (b in shape.blocks) {
            val spec = shape.scenes.getOrNull(b.scene)
            if (spec == null || spec.roll != 0 || spec.silence || spec.cutBars > 0 || b.sec == Sec.Lead || b.sec == Sec.Fake) quiet += at until at + b.bars
            at += b.bars
        }
        val marks = ArrayList<Mark>()
        stretches.forEachIndexed { i, st ->
            val next = stretches.getOrNull(i + 1)?.sec
            // The crash: on the 1 of each 16 of a Drop (a Refrain), on the first beat of a fake drop and of a Break.
            val crashes = crashLane != null && (st.sec == Sec.Drop || st.sec == Sec.Fake || (st.sec == Sec.Break && st.bars >= 8))
            if (crashes) for (k in 0 until st.bars step 16) if (st.sec == Sec.Drop || k == 0) marks += Mark(st.start + k, Kind.Crash)
            if (st.sec == Sec.Outro || st.sec == Sec.Lead || st.sec == Sec.Fake) return@forEachIndexed
            for (k in 7 until st.bars step 8) {
                val end = k == st.bars - 1
                // Into a Montée or a Drop the song has its own transition; the Intro's layers come in every 8.
                if (end && (next == Sec.Lead || next == Sec.Drop || next == Sec.Fake || next == null)) continue
                if (st.sec == Sec.Intro && !end) continue
                marks += Mark(st.start + k, if (end || (k + 1) % 16 == 0) Kind.Fill16 else Kind.Fill8)
            }
        }
        val chosen = marks.filter { it.kind == Kind.Crash || it.bar !in quiet }.sortedBy { it.kind.ordinal }
        if (chosen.isEmpty()) return project

        // Each part's first bar, to find the drums a bar plays.
        val starts = project.song.runningFold(0) { acc, p -> acc + project.partSteps(p) / project.stepsPerBar }
        fun partAt(b: Int): Int = starts.indexOfLast { it <= b }.coerceIn(0, project.song.size - 1)

        val scenes = project.allScenes.toMutableList()
        val byContent = HashMap<DrumPattern, Int>()
        val special = HashMap<Int, Int>()  // song bar -> the scene its drums play
        val sung = style.form.sung
        for (m in chosen) {
            val pi = partAt(m.bar)
            val part = project.song[pi]
            if (!part.plays(SongPart.DRUMS) || SongPart.DRUMS in part.play) continue
            val source = scenes.getOrNull(part.scene)?.drums ?: continue
            val spb = project.stepsPerBar
            val loopBars = (source.steps / spb).coerceAtLeast(1)
            val k = m.bar - starts[pi]
            val inLoop = (part.fromBars(SongPart.DRUMS) + k) % loopBars
            var lanes = source.lanes.map { lane ->
                lane.copy(hits = lane.hits.filterKeys { it in inLoop * spb until (inLoop + 1) * spb }.mapKeys { it.key - inLoop * spb }, sample = null, pan = 0f)
            }
            if (lanes.all { it.hits.isEmpty() }) continue
            val dice = DrumHumanizer.Dice(seed + 104729L * m.bar)
            lanes = when (m.kind) {
                Kind.Crash -> lanes.toMutableList().also { l ->
                    val c = crashLane ?: return@also
                    l[c] = l[c].copy(hits = l[c].hits + (0 to Hit(StarterWizard.CRASH_VELOCITY)))
                }
                Kind.Fill16 -> timed(DrumHumanizer.ending(lanes, spb, if (sung) DrumHumanizer.Ending.Hats else DrumHumanizer.Ending.Toms, beats = 2, sung = sung), spb - 8, feel, dice)
                Kind.Fill8 -> timed(DrumHumanizer.ending(lanes, spb, DrumHumanizer.endingOf(feel, seed, m.bar / 8), beats = 2, sung = sung), spb - 8, feel, dice)
            }
            val drums = DrumPattern(steps = spb, lanes = lanes)
            val slot = byContent[drums] ?: run {
                if (scenes.size >= Scene.MAX - FREE_SLOTS) return@run null
                scenes += Scene(drums = drums, label = if (m.kind == Kind.Crash) CRASH_LABEL else PartIntents.FILL_LABEL)
                (scenes.size - 1).also { byContent[drums] = it }
            } ?: continue
            special[m.bar] = slot
        }
        if (special.isEmpty()) return project
        val song = ArrayList<SongPart>()
        val keys = project.partKeys
        project.song.forEachIndexed { pi, part ->
            val first = starts[pi]
            val bars = starts[pi + 1] - first
            val inPart = special.keys.filter { it in first until first + bars }.map { it - first }.sorted()
            if (inPart.isEmpty()) {
                song += part
                return@forEachIndexed
            }
            val sceneBars = (project.sceneSteps(part.scene) / project.stepsPerBar).coerceAtLeast(1)
            fun carried(k: Int, drums: Boolean) = keys.filter { drums || it != SongPart.DRUMS }
                .associateWith { (part.fromBars(it) + k).coerceAtMost(SongPart.MAX_FROM) }.filterValues { it > 0 }
            fun run(from: Int, until: Int) {
                if (until <= from) return
                val n = until - from
                song += if (from == 0 && part.bars == null && part.from.isEmpty() && n % sceneBars == 0) part.copy(repeats = n / sceneBars)
                else part.copy(repeats = 1, bars = n, from = carried(from, drums = true))
            }
            var k = 0
            for (s in inPart) {
                run(k, s)
                song += part.copy(repeats = 1, bars = 1, from = carried(s, drums = false), play = part.play + (SongPart.DRUMS to special.getValue(first + s)))
                k = s + 1
            }
            run(k, bars)
        }
        if (song.size > SongPart.MAX_PARTS) return project
        return project.copy(scenes = scenes, song = song)
    }

    /** The fill's hits from [from] on (written plain) at their line's distance from the grid. */
    private fun timed(lanes: List<DrumLane>, from: Int, feel: DrumHumanizer.Feel, dice: DrumHumanizer.Dice): List<DrumLane> =
        lanes.mapIndexed { lane, l ->
            val base = when (lane) {
                StarterWizard.Role.Clap.lane -> feel.clapEarlyMs + 1f
                StarterWizard.Role.Snare.lane -> feel.snareLateMs
                StarterWizard.Role.Hat.lane -> feel.hatLateMs
                in DrumHumanizer.TOM_LANES -> 1f
                else -> return@mapIndexed l
            }
            l.copy(hits = l.hits.mapValues { (st, h) -> if (st < from || h.offset != 0f) h else h.copy(offset = (base + dice.signed()).coerceIn(-Hit.MAX_OFFSET_MS, Hit.MAX_OFFSET_MS)) })
        }
}
