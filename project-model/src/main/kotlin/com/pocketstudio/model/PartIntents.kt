package com.pocketstudio.model

/**
 * The part sheet (« Variation », « Fill à la fin », « Break », « Montée avant », « Fondu
 * d'entrée / de sortie », « Fin », « Énergie », length): each is one edit of the project (one
 * undo step), written with the project's ordinary scenes, song parts and song curves, so the
 * Mixeur, the scene grid and the timeline show what each button wrote and can change it. When
 * there is no room (24 scenes, 128 parts, 16 curves) the project comes back as it is.
 */
object PartIntents {
    const val BREAK_LABEL = "Break"
    const val FILL_LABEL = "Fill"
    const val ENDING_LABEL = "Fin"

    /** Bars a Montée rises over, a Fin lasts, a Break lasts unless asked longer. */
    const val BARS = 4
    const val LONG_BARS = 8

    /** The kick pads (the kit's two kicks), taken out by a Break and a Fin. */
    val KICK_LANES = setOf(0, 1)

    /** Where the DJ filter rests (open) and how far Énergie 1 and 2 close it (a low-pass: darker). */
    const val FILTER_OPEN = 0.5f
    const val ENERGY_1_FILTER = 0.3f
    const val ENERGY_2_FILTER = 0.4f

    /** The build-up (« Montée ») just before the part it leads to. */
    const val BUILD_UP_TOP = 0.85f

    /** Where a fade-out lands on its last bar (−22 dB; the engine holds the last point). */
    const val FADE_FLOOR = 0.08f

    /** Fills by style family: a snare roll, a run down the toms, or hats that climb (see [fillOf]). */
    enum class Fill { Snare, Toms, Hats }

    /** The fill the style family of [family] calls for (none: the toms, the plainest). */
    fun fillOf(family: String?): Fill = when (family) {
        "Techno", "House" -> Fill.Snare
        "Bass music", "Hip-hop" -> Fill.Hats
        else -> Fill.Toms
    }

    private const val SNARE = 2
    private const val CLAP = 4
    private const val HAT = 5
    private const val TOM_LOW = 7
    private const val TOM_MID = 8
    private const val TOM_HIGH = 9

    /**
     * The last bar of a part's drums ([bar], [stepsPerBar] steps) with [fill] written over its
     * second half: the kick and the rest stay, the fill's own lanes are replaced there.
     */
    fun withFill(bar: DrumPattern, stepsPerBar: Int, fill: Fill): DrumPattern {
        val spb = stepsPerBar.coerceAtLeast(8)
        val half = spb / 2
        val lanes = bar.lanes.toMutableList()
        fun clear(lane: Int) { lanes[lane] = lanes[lane].copy(hits = lanes[lane].hits.filterKeys { it < half }) }
        fun put(lane: Int, step: Int, velocity: Float, repeats: Int = 1, ramp: Int = 0) {
            if (step !in 0 until spb) return
            lanes[lane] = lanes[lane].copy(hits = lanes[lane].hits + (step to Hit(velocity.coerceIn(0.2f, 1f), repeats, ramp = ramp)))
        }
        val n = spb - half
        when (fill) {
            Fill.Snare -> {
                clear(SNARE)
                // Eighths, then sixteenths climbing, the last two doubled.
                for (i in 0 until n) {
                    val step = half + i
                    val late = i >= n / 2
                    if (!late && i % 2 != 0) continue
                    put(SNARE, step, 0.5f + 0.5f * i / (n - 1), if (i >= n - 2) 2 else 1, if (i >= n - 2) 1 else 0)
                }
            }
            Fill.Hats -> {
                clear(HAT)
                // Sixteenths climbing, 32nds on the last beat, a doubled snare on the very end.
                for (i in 0 until n) put(HAT, half + i, 0.4f + 0.6f * i / (n - 1), if (i >= n - 4) 2 else 1, if (i >= n - 4) 1 else 0)
                put(SNARE, spb - 1, 1f, 2, 1)
            }
            Fill.Toms -> {
                listOf(TOM_HIGH, TOM_MID, TOM_LOW, SNARE).forEach(::clear)
                // Down the toms in eighths, two doubled snares to end.
                val order = listOf(TOM_HIGH, TOM_HIGH, TOM_MID, TOM_MID, TOM_LOW, TOM_LOW, SNARE, SNARE)
                for (i in 0 until n) {
                    val lane = order[(i * order.size / n).coerceIn(0, order.size - 1)]
                    if (lane != SNARE && i % 2 != 0) continue
                    put(lane, half + i, 0.75f + 0.25f * i / (n - 1), if (lane == SNARE) 2 else 1, if (lane == SNARE) 1 else 0)
                }
            }
        }
        return bar.copy(lanes = lanes)
    }

    /** [d] with Énergie 4's extra layer: hats on the off-eighths and claps on the even beats, where the loop has none. */
    fun withExtraLayer(d: DrumPattern, stepsPerBar: Int): DrumPattern {
        val spb = stepsPerBar.coerceAtLeast(8)
        val lanes = d.lanes.toMutableList()
        val hats = (0 until d.steps).filter { it % 4 == 2 && it !in lanes[HAT].hits }.associateWith { Hit(0.55f) }
        val claps = (0 until d.steps).filter { (it % spb) % 4 == 0 && ((it % spb) / 4) % 2 == 1 && it !in lanes[CLAP].hits }.associateWith { Hit(0.8f) }
        lanes[HAT] = lanes[HAT].copy(hits = lanes[HAT].hits + hats)
        lanes[CLAP] = lanes[CLAP].copy(hits = lanes[CLAP].hits + claps)
        return d.copy(lanes = lanes)
    }

    /** [d] without its kicks (a Break, a Fin). */
    fun withoutKick(d: DrumPattern): DrumPattern = d.copy(lanes = d.lanes.mapIndexed { i, l -> if (i in KICK_LANES) l.copy(hits = emptyMap()) else l })
}

/** The style family the song was made in (« Techno », « House », …), null for a project made by hand. */
val Project.styleFamily: String? get() = style?.let { StarterWizard.style(it)?.family }

/** How many parts of the song play the scene of part [index] (this one included; 0: no such part). */
fun Project.partPlayedTimes(index: Int): Int {
    val s = song.getOrNull(index)?.scene ?: return 0
    return song.count { it.scene == s || it.play.containsValue(s) }
}

/** The first slot free for a new scene (empty, unnamed, not shown, no take, not in the song), or null when all are taken. */
internal fun Project.freeSceneSlot(): Int? {
    val all = allScenes
    val takes = scenesWithTakes()
    val used = song.flatMap { listOf(it.scene) + it.play.values }.toSet()
    return all.indices.firstOrNull { it != scene && all[it].isEmpty && all[it].label.isBlank() && it !in takes && it !in used }
        ?: all.size.takeIf { it < Scene.MAX }
}

/** Scene [from] copied into slot [to] (its per-scene automation and its effects' scenes too) under [label]; the scene shown stays. */
internal fun Project.copyScene(from: Int, to: Int, label: String): Project {
    val shown = scene
    return switchScene(from).copySceneTo(to).renameScene(to, label).switchScene(shown)
}

/** Scene [index] changed by [transform] (the scene shown included: its content follows). */
internal fun Project.editScene(index: Int, transform: (Scene) -> Scene): Project {
    val synced = syncScenes()
    val all = synced.allScenes
    if (index !in all.indices) return synced
    val list = all.mapIndexed { i, s -> if (i == index) transform(s) else s }
    return if (index == synced.scene) synced.showing(list, index) else synced.copy(scenes = list).syncScenes()
}

/** The label of a variation of a scene labelled [label]: "Drop 2", then "Drop 3" (a free number); blank stays blank. */
internal fun Project.variationLabel(label: String): String {
    if (label.isBlank()) return ""
    val numbered = Regex("""^(.+) (\d+)$""").matchEntire(label)
    val base = numbered?.groupValues?.get(1) ?: label
    val taken = allScenes.map { it.label }.toSet()
    var n = (numbered?.groupValues?.get(2)?.toIntOrNull() ?: 1) + 1
    while ("$base $n" in taken) n++
    return "$base $n".take(Scene.MAX_LABEL)
}

/** Part [index] with [edit]. */
private fun Project.editPart(index: Int, edit: (SongPart) -> SongPart): Project =
    copy(song = song.mapIndexed { i, p -> if (i == index) edit(p) else p })

/** The song curve on [target] changed by [transform] (a new song curve when none; null when the 16 lanes are taken). */
internal fun Project.withSongLane(target: AutoTarget, songOnly: Boolean = false, transform: (AutomationLane) -> AutomationLane): Project? {
    val i = automation.indexOfFirst { it.target == target }
    if (i >= 0) {
        val lane = automation[i]
        // A loop or per-scene lane on this setting: its song curve is written beside its steps (the song mode wins while the song plays).
        val song = if (lane.mode == AutomationLane.SONG) lane else lane.copy(mode = AutomationLane.SONG)
        return copy(automation = automation.mapIndexed { k, l -> if (k == i) transform(song).sanitized() else l })
    }
    if (automation.size >= AutomationLane.MAX_LANES) return null
    val lane = AutomationLane(target, mode = AutomationLane.SONG, songOnly = songOnly)
    return copy(automation = automation + transform(lane).sanitized())
}

/** The song curve without its points (and jumps) on bars [from]..[to]. */
internal fun AutomationLane.clearSongRange(from: Int, to: Int): AutomationLane {
    if (to < from) return this
    val values = songValues.mapIndexed { i, v -> if (i in from..to) null else v }
    return copy(songValues = values.dropLastWhile { it == null }, songJumps = songJumps.filter { it !in from..to })
}

/** The song curve's value on bar [bar] as it is now (its setting when it has no points). */
private fun Project.songValueAt(lane: AutomationLane, bar: Int): Float =
    if (lane.hasPoints) lane.songCurve(maxOf(songBars, bar + 1).coerceAtMost(lane.maxSongPoints)).getOrNull(bar) ?: settingOf(lane.target) else settingOf(lane.target)

/** The bar song part [index] starts on. */
private fun Project.partBar(index: Int): Int = songStepOf(index) / stepsPerBar

/** Bars song part [index] lasts. */
private fun Project.partBars(index: Int): Int = (partSteps(song[index]) / stepsPerBar).coerceAtLeast(1)

/**
 * « Variation »: part [index] gets a copy of its scene, its own to change (the parts that shared
 * the scene keep it), and the copy is shown for editing. Unchanged when the 24 scenes are taken.
 */
fun Project.variationOfPart(index: Int): Project {
    val part = song.getOrNull(index) ?: return this
    val slot = freeSceneSlot() ?: return this
    val p = copyScene(part.scene, slot, variationLabel(allScenes.getOrNull(part.scene)?.label.orEmpty()))
    return p.editPart(index) { it.copy(scene = slot, play = it.play.filterValues { s -> s != slot }) }.switchScene(slot)
}

/**
 * « Fill à la fin »: the last bar of part [index] plays a one-bar « Fill » scene on the drums
 * only (its last bar with the style family's fill, [PartIntents.withFill]); the other tracks
 * carry on as before. The part is cut in two for it (the rest of the song keeps its place).
 * Unchanged when it already ends on a fill, or when scenes or parts are all taken.
 */
fun Project.fillAtEnd(index: Int, family: String? = styleFamily): Project {
    val part = song.getOrNull(index) ?: return this
    if (isFillPart(part)) return this
    val total = partBars(index)
    if (total < 1 || (total > 1 && song.size >= SongPart.MAX_PARTS)) return this
    val slot = freeSceneSlot() ?: return this
    val spb = stepsPerBar
    // The drums heard on the part's last bar: the loop's bar that falls there.
    val drums = allScenes.getOrNull(part.sceneFor(SongPart.DRUMS))?.drums ?: DrumPattern()
    val loopBars = (drums.steps / spb).coerceAtLeast(1)
    val lastBar = (part.fromBars(SongPart.DRUMS) + total - 1) % loopBars
    val bar = DrumPattern(
        steps = spb,
        lanes = drums.lanes.map { lane ->
            // A pad's own loop (a polyrhythm) would cut the fill's bar: the fill plays its bar straight.
            lane.copy(hits = lane.hits.filterKeys { it in lastBar * spb until (lastBar + 1) * spb }.mapKeys { it.key - lastBar * spb }, sample = null, pan = 0f, length = 0)
        },
    )
    val fill = Scene(drums = PartIntents.withFill(bar, spb, PartIntents.fillOf(family)), label = PartIntents.FILL_LABEL)
    val synced = syncScenes()
    val list = synced.allScenes + List((slot + 1 - synced.allScenes.size).coerceAtLeast(0)) { Scene() }
    var p = synced.copy(scenes = list.mapIndexed { i, s -> if (i == slot) fill else s }).syncScenes()
    val keys = p.partKeys
    val last = part.copy(
        bars = 1, repeats = 1,
        from = keys.filter { it != SongPart.DRUMS }.associateWith { (part.fromBars(it) + total - 1).coerceAtMost(SongPart.MAX_FROM) },
        play = part.play + (SongPart.DRUMS to slot),
    )
    if (total == 1) return p.editPart(index) { last.copy(bars = part.bars, repeats = part.repeats, from = part.from - SongPart.DRUMS) }
    val sceneBars = (p.sceneSteps(part.scene) / spb).coerceAtLeast(1)
    val first = if (part.bars == null && (total - 1) % sceneBars == 0) part.copy(repeats = (total - 1) / sceneBars) else part.copy(bars = total - 1, repeats = 1)
    // A cut, as « Couper la partie » makes one: the song lasts as long, its curves and clips stay put.
    return p.copy(song = p.song.take(index) + first + last + p.song.drop(index + 1))
}

/** True when [part] is the one-bar fill [fillAtEnd] writes (its drums play a « Fill » scene). */
fun Project.isFillPart(part: SongPart): Boolean =
    part.play[SongPart.DRUMS]?.let { allScenes.getOrNull(it)?.label } == PartIntents.FILL_LABEL

/**
 * « Break »: a new part right after part [index], [bars] bars long, playing a copy of its scene
 * without the kicks, with the 303 and the bass tracks silent; the pads, the voices and the rest
 * stay. Unchanged when scenes or parts are all taken.
 */
fun Project.breakAfter(index: Int, bars: Int = PartIntents.BARS): Project {
    val part = song.getOrNull(index) ?: return this
    if (song.size >= SongPart.MAX_PARTS) return this
    val slot = freeSceneSlot() ?: return this
    var p = copyScene(part.scene, slot, PartIntents.BREAK_LABEL).editScene(slot) { s -> s.copy(drums = PartIntents.withoutKick(s.drums)) }
    val sceneBars = (p.sceneSteps(slot) / stepsPerBar).coerceAtLeast(1)
    val n = bars.coerceIn(1, SongPart.MAX_BARS)
    val muted = part.muted + p.bassKeys()
    val added = if (n % sceneBars == 0 && n / sceneBars <= SongPart.MAX_REPEATS) SongPart(slot, n / sceneBars, muted) else SongPart(slot, 1, muted, bars = n)
    p = p.copy(song = p.song.take(index + 1) + added + p.song.drop(index + 1))
    return p.followArrangement(this, song.indices.toList().let { it.take(index + 1) + null + it.drop(index + 1) })
}

/**
 * « Montée avant »: over the [bars] bars before part [index], the whole mix's build-up (the
 * Live page's « Montée »: a filter that climbs, then stutters) rises from nothing to
 * [PartIntents.BUILD_UP_TOP] and drops back on the part's first bar; the assistant's riser
 * track, when the part before holds its note, opens with it and hits on the part's first beat.
 * Song curves only: a part looped on its own stays quiet. Unchanged for the first part, or
 * with the 16 curves taken.
 */
fun Project.buildUpBefore(index: Int, bars: Int = PartIntents.BARS): Project {
    if (index !in song.indices) return this
    val start = partBar(index)
    val rise = minOf(bars, start)
    if (rise <= 0 || start >= AutomationLane.MAX_SONG_BARS - 1) return this
    val from = start - rise
    var p = withSongLane(AutoTarget.live(AutoTarget.LIVE_BUILD_UP), songOnly = true) { lane ->
        lane.clearSongRange(from, start)
            .withSongPoint(from, 0f).withSongPoint(start - 1, PartIntents.BUILD_UP_TOP)
            .withSongPoint(start, 0f).withSongJump(start, true)
    } ?: return this
    // The riser: its filter opens over the Montée and its impact lands on the first beat.
    val riser = p.riserTrackId() ?: return p
    val ch = InstrumentTrack.mixerChannel(riser)
    if (p.mixer.insertAt(ch, 0)?.effect != EffectType.FILTER) return p
    val key = SongPart.inst(riser)
    val before = p.song[index - 1]
    val held = p.allScenes.getOrNull(before.sceneFor(key))?.let { s -> if (riser == 0) s.synthNotes else s.trackNotes[riser] }?.notes?.isNotEmpty() == true
    if (!held) return p
    p = p.withSongLane(AutoTarget(AutoTarget.INSERT_PARAM, ch, 0, 0), songOnly = true) { lane ->
        var l = lane.clearSongRange(from, start + 1)
        if (from > 0) l = l.withSongPoint(from - 1, 0f)
        l.withSongPoint(from, 0.3f).withSongPoint(start - 1, 0.9f).withSongPoint(start, 1f).withSongJump(start, true).withSongPoint(start + 1, 0f)
    } ?: p
    return p
}

/** « Fondu d'entrée »: the whole mix rises from silence over part [index] (a song curve on [AutoTarget.masterVolume]). */
fun Project.fadeIn(index: Int): Project = fade(index, inward = true)

/** « Fondu de sortie »: the whole mix dies away over part [index]; the next part, if any, plays at its level again. */
fun Project.fadeOut(index: Int): Project = fade(index, inward = false)

private fun Project.fade(index: Int, inward: Boolean): Project {
    if (index !in song.indices) return this
    val start = partBar(index)
    val last = AutomationLane.MAX_SONG_BARS - 1
    if (start >= last) return this
    val end = minOf(start + partBars(index), last)
    val finalBar = maxOf(start, end - 1)
    val total = songBars
    return withSongLane(AutoTarget.masterVolume()) { lane ->
        val before = if (start > 0) songValueAt(lane, start - 1) else null
        val after = songValueAt(lane, end)
        var l = lane.clearSongRange(start, end)
        if (inward) {
            if (before != null) l = l.withSongPoint(start - 1, before)
            l = l.withSongPoint(start, 0f).withSongJump(start, start > 0).withSongPoint(finalBar, 1f)
            if (end < total) l = l.withSongPoint(end, after)
        } else {
            l = l.withSongPoint(start, 1f).withSongPoint(finalBar, PartIntents.FADE_FLOOR)
            if (end < total) l = l.withSongPoint(end, after).withSongJump(end, true)
        }
        l
    } ?: this
}

/**
 * « Fin »: a real ending after the last part: a copy of it without the kicks and the basses,
 * [PartIntents.BARS] bars long, the riser's impact on its first beat when the project has that
 * track, the whole mix fading out over it, and the song stops there. Unchanged when scenes or
 * parts are all taken.
 */
fun Project.withEnding(): Project {
    val lastIndex = song.lastIndex
    if (lastIndex < 0 || song.size >= SongPart.MAX_PARTS) return this
    val last = song[lastIndex]
    val slot = freeSceneSlot() ?: return this
    val riser = riserTrackId()
    var p = copyScene(last.scene, slot, PartIntents.ENDING_LABEL).editScene(slot) { s ->
        var out = s.copy(drums = PartIntents.withoutKick(s.drums))
        if (riser != null) {
            val hit = NoteClip(steps = s.stepsFor(stepsPerBar).coerceIn(NoteClip.MIN_STEPS, NoteClip.MAX_STEPS), notes = listOf(Note(0, 2 * NoteClip.TICKS_PER_STEP, StarterWizard.RISER_PITCH, 1f)))
            out = if (riser == 0) out.copy(synthNotes = hit) else out.copy(trackNotes = out.trackNotes + (riser to hit))
        }
        out
    }
    val sceneBars = (p.sceneSteps(slot) / stepsPerBar).coerceAtLeast(1)
    val n = PartIntents.BARS
    val muted = (last.muted + p.bassKeys()) - listOfNotNull(riser?.let { SongPart.inst(it) }).toSet()
    val added = if (n % sceneBars == 0) SongPart(slot, n / sceneBars, muted) else SongPart(slot, 1, muted, bars = n)
    p = p.copy(song = p.song + added, songLoops = false)
        .followArrangement(this, song.indices.toList() + null)
    val ending = p.song.lastIndex
    if (riser != null) {
        val ch = InstrumentTrack.mixerChannel(riser)
        val start = p.partBar(ending)
        if (p.mixer.insertAt(ch, 0)?.effect == EffectType.FILTER && start < AutomationLane.MAX_SONG_BARS - 1) {
            p = p.withSongLane(AutoTarget(AutoTarget.INSERT_PARAM, ch, 0, 0), songOnly = true) { lane ->
                var l = lane.clearSongRange(start, start + 1)
                if (start > 0) l = l.withSongPoint(start - 1, 0f)
                l.withSongPoint(start, 1f).withSongJump(start, true).withSongPoint(start + 1, 0f)
            } ?: p
        }
    }
    return p.fadeOut(ending)
}

/**
 * The energy of part [index], 1 to 4, read from what plays in it: 1 pads (and voices) only,
 * 2 with the basses, 3 with the drums, 4 with the drums' extra layer ([withPartEnergy]).
 */
fun Project.partEnergy(index: Int): Int {
    val part = song.getOrNull(index) ?: return 3
    val drumsOn = part.plays(SongPart.DRUMS)
    val layer = part.play[SongPart.DRUMS]?.let { allScenes.getOrNull(it)?.label?.endsWith(Scene.ENERGY_SUFFIX) } == true
    val bassOn = bassKeys().any { part.plays(it) }
    return when {
        drumsOn && layer -> 4
        drumsOn -> 3
        bassOn -> 2
        else -> 1
    }
}

/**
 * « Énergie » [level] (1..4) for part [index]: 1 the pads and the voices alone, the mix darkened
 * (the DJ filter closed a little over the part); 2 the basses back, a touch darker; 3 everything
 * as the scene plays it, the filter open; 4 the drums with an extra layer (hats on the
 * off-beats, claps on the even beats) in a scene of their own (« Drop + ») for this part. The
 * pads and the voices keep what they had. The filter's curve is only written when the level
 * asks for it or a curve already moves it.
 */
fun Project.withPartEnergy(index: Int, level: Int): Project {
    val part = song.getOrNull(index) ?: return this
    val l = level.coerceIn(1, 4)
    val keys = partKeys
    val bass = bassKeys()
    val kept = (padTrackIds().map { SongPart.inst(it) } + audioTracks.map { SongPart.audio(it.id) }).toSet() + SongPart.REMIX
    val others = keys.filter { it != SongPart.DRUMS && it !in bass && it !in kept }.toSet()
    val silent = when (l) {
        1 -> setOf(SongPart.DRUMS) + bass + others
        2 -> setOf(SongPart.DRUMS) + others
        else -> emptySet()
    }
    var p = editPart(index) { it.copy(muted = silent + (it.muted intersect kept)) }
    val layer = part.play[SongPart.DRUMS]?.takeIf { allScenes.getOrNull(it)?.label?.endsWith(Scene.ENERGY_SUFFIX) == true }
    if (l == 4 && layer == null) {
        val slot = p.freeSceneSlot()
        if (slot != null) {
            val base = part.sceneFor(SongPart.DRUMS)
            val label = (p.allScenes.getOrNull(base)?.label?.ifBlank { null } ?: Scene.NAMES.getOrElse(base) { "?" }).take(Scene.MAX_LABEL - Scene.ENERGY_SUFFIX.length) + Scene.ENERGY_SUFFIX
            p = p.copyScene(base, slot, label).editScene(slot) { s -> s.copy(drums = PartIntents.withExtraLayer(s.drums, stepsPerBar)) }
            p = p.editPart(index) { it.copy(play = it.play + (SongPart.DRUMS to slot)) }
        }
    } else if (l < 4 && layer != null) {
        p = p.editPart(index) { it.copy(play = it.play - SongPart.DRUMS) }
    }
    // The filter: closed a little for 1 and 2, open for 3 and 4 (only when a curve already moves it).
    val target = AutoTarget.live(AutoTarget.LIVE_FILTER)
    val value = when (l) { 1 -> PartIntents.ENERGY_1_FILTER; 2 -> PartIntents.ENERGY_2_FILTER; else -> PartIntents.FILTER_OPEN }
    if (l <= 2 || p.automation.any { it.target == target }) {
        val start = p.partBar(index)
        val end = start + p.partBars(index) - 1
        if (end < AutomationLane.MAX_SONG_BARS) {
            p = p.withSongLane(target, songOnly = true) { it.withSongZone(start, end, value, PartIntents.FILTER_OPEN, p.songBars) } ?: p
        }
    }
    return p
}

/**
 * Part [index] [bars] bars long: whole passes of its scene when they fit (its repeats), else a
 * length of its own. What is placed after it follows. Unchanged when it already lasts that long.
 */
fun Project.withPartBars(index: Int, bars: Int): Project {
    val part = song.getOrNull(index) ?: return this
    val n = bars.coerceIn(1, SongPart.MAX_BARS)
    val sceneBars = (sceneSteps(part.scene) / stepsPerBar).coerceAtLeast(1)
    val next = if (part.from.isEmpty() && n % sceneBars == 0 && n / sceneBars <= SongPart.MAX_REPEATS) part.copy(bars = null, repeats = n / sceneBars)
    else part.copy(bars = n, repeats = 1)
    if (next == part) return this
    return editPart(index) { next }.followArrangement(this, song.indices.toList())
}

/** « Allonger » / « Raccourcir »: part [index] one pass of its scene longer ([by] = 1) or shorter (-1). */
fun Project.lengthenPart(index: Int, by: Int): Project {
    val part = song.getOrNull(index) ?: return this
    if (part.bars == null) return withSongRepeats(index, part.repeats + by)
    val sceneBars = (sceneSteps(part.scene) / stepsPerBar).coerceAtLeast(1)
    return withPartBars(index, part.bars + by * sceneBars)
}
