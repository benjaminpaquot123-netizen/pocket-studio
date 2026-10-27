package com.pocketstudio.model

import com.pocketstudio.model.StarterWizard.Part
import com.pocketstudio.model.StarterWizard.Role
import com.pocketstudio.model.StarterWizard.ScenePlan
import com.pocketstudio.model.StarterWizard.SongForm
import com.pocketstudio.model.StarterWizard.Style
import com.pocketstudio.model.StarterWizard.Version

/**
 * The shape of the assistant's songs (plan « son pro », chantier S2: an arrangement that drops).
 *
 * A song is a list of stretches ([Span]: an Intro, a Groove or a Couplet, the Montée that leads
 * into a Drop, the Drop, a Break…) laid out per form ([layout]), and each stretch is played by
 * scenes: the plan's own ones (Intro, Groove/Couplet/Montée, Drop/Refrain, Break/Pont) and
 * variants of them ([SceneSpec]):
 * - the Montée before each Drop (8 bars: two scenes of 4), the kick and the bass out of its last
 *   4 bars (S5b; 2 before, a song's last bar), its last beat silent, the snare rolling faster and
 *   faster under it, held under the Drop;
 * - the Drop in scenes of 8 bars: the second one ([Version.Chorus2]) fuller than the first, an
 *   8-bar mute in techno, a crash, an impact and a sub drop on the first one;
 * - a new layer every 8 bars in the long intros and grooves, leaving again in the outro;
 * - the Pré-refrain, the Pont and the Post-refrain of a song; the Couplet of a rap song whose
 *   kick and 808 stop 2 bars before each Refrain; the riddim's fake drop.
 * Techno and bass music last 5 to 6 minutes, the rest about 3 (Benjamin, 2026-10-23).
 */
object SongShape {
    /** What a stretch of the song does; [base]: the plan's scene it is made from. */
    enum class Sec(val code: Char, val base: Int) {
        Intro('I', 0),
        /** The Groove (club music) or the Couplet (a song). */
        Verse('V', 1),
        /** The bars that lead into a Drop or a Refrain: the Montée, the Pré-refrain, the end of a rap Couplet. */
        Lead('L', 1),
        Drop('D', 2),
        /** The riddim's fake drop: the Drop's beat without its bass, then a beat of silence before the real one. */
        Fake('F', 2),
        /** A song's Post-refrain: the hook again over the beat, without the chords. */
        Post('P', 2),
        /** The Break (club music) or the Pont (a song). */
        Break('B', 3),
        Outro('O', 0),
    }

    data class Span(val sec: Sec, val bars: Int)

    /** "I16 V8 L8 D16" as spans. */
    fun parse(layout: String): List<Span> = layout.split(' ').filter { it.isNotBlank() }.map { t ->
        Span(Sec.entries.first { it.code == t[0] }, t.substring(1).toInt())
    }

    /** Club music of about 3 minutes (house): Intro, Groove, Montée, Drop, Break, Montée, Drop, outro. */
    private val CLUB = listOf("I16 V8 L8 D16 B8 L8 D16 O16", "I8 V8 L8 D16 B8 L8 D16 O8", "I8 V8 L4 D16 B8 L4 D16 O8")

    /** Techno of 5 to 6 minutes: a DJ's intro of 32 bars, sections of 32, a Break of 16 (Beatportal, Futureproof). */
    private val CLUB_LONG = listOf(
        "I32 V24 L8 D32 B16 L8 D32 B8 L8 D32 O24", "I32 V24 L8 D32 B16 L8 D32 O24", "I16 V16 L8 D32 B16 L8 D32 O16",
    ) + CLUB

    /** The rave (acidcore): the 303 alone, then two Breaks. */
    private val RAVE = listOf("I16 V8 L8 D16 B8 L8 D16 B8 L8 D16 O8", "I8 V8 L8 D16 B8 L8 D16 O8")
    private val RAVE_LONG = listOf("I32 V24 L8 D32 B16 L8 D32 B8 L8 D32 O16") + CLUB_LONG.take(3) + RAVE

    /** Bass music whose Drop drops (Intro, Montée, Drop, Break, Montée, Drop, outro): NI, EDMProd, Mixed In Key. */
    private val BUILD = listOf("I16 L8 D32 B8 L8 D32 O16", "I16 L8 D16 B8 L8 D16 O16", "I8 L8 D16 B8 L8 D16 O8")
    private val BUILD_LONG = listOf("I32 L8 D32 B16 L8 D32 B16 L8 D32 O32", "I32 L8 D32 B16 L8 D32 B8 L8 D16 O16") + BUILD

    /**
     * A song: Intro, Couplet, Pré-refrain, Refrain, Post-refrain, Couplet, Pré-refrain, Refrain,
     * Pont, Refrain, outro; the first Refrain within 45 s, the whole under 3 min 30.
     */
    private val SONG = listOf(
        "I8 V8 L8 D16 P4 V8 L8 D16 B8 D16 O8", "I4 V8 L8 D16 P4 V8 L8 D16 B8 D16 O4", "I4 V8 L4 D16 P4 V8 L4 D16 B8 D16 O4",
        "I4 V8 L4 D16 P4 V8 L4 D8 B8 D16 O4", "I4 V8 L4 D8 V8 L4 D8 B8 D8 O4", "I4 V8 L4 D8 V8 L4 D8 O4",
    )

    /**
     * A rap song (Beatstorapon, the hits of hits/hiphop.md G4): Intro 8, Refrain, Couplet 16 (its
     * last 8 leading into the Refrain), Refrain, Couplet, Pont, the last Refrain, outro.
     */
    private val RAP = listOf(
        "I8 D16 V8 L8 D16 V16 B8 D16 O8", "I8 D8 V8 L8 D8 V16 B8 D16 O8", "I4 D8 V8 L8 D8 V8 L8 D8 O4", "I4 D8 V8 L8 D8 O4",
    )

    /** The longest song of a family: techno and bass music 6 minutes, the others 3 min 30 (Benjamin, 2026-10-23). */
    fun maxSeconds(long: Boolean): Double = if (long) LONG_SECONDS else StarterWizard.MAX_SONG_SECONDS

    const val LONG_SECONDS = 360.0

    /** A song's first Refrain starts within 45 seconds (streaming: the hook before the listener skips). */
    const val FIRST_CHORUS_SECONDS = 45.0

    /** The families whose songs are long (5 to 6 minutes, like the real tracks a DJ mixes). */
    val LONG_FAMILIES = setOf("Techno", "Bass music")

    fun isLong(style: Style): Boolean = style.family in LONG_FAMILIES

    /**
     * The song of [form] at [bpm]: the first candidate that fits under [maxSeconds] (and puts a
     * song's first Refrain within [FIRST_CHORUS_SECONDS]). Without a Break ([hasBreak] false) club
     * music grooves on instead and a song has no Pont; [fakeDrop] puts a fake drop before the first Drop.
     */
    fun layout(form: SongForm, bpm: Double, hasBreak: Boolean, long: Boolean = false, fakeDrop: Boolean = false, beatsPerBar: Int = 4): List<Span> {
        val list = when (form) {
            SongForm.Song -> SONG
            SongForm.Rap -> RAP
            SongForm.Club -> if (long) CLUB_LONG else CLUB
            SongForm.Rave -> if (long) RAVE_LONG else RAVE
            SongForm.Build -> if (long) BUILD_LONG else BUILD
        }.map { adapt(parse(it), form, hasBreak, fakeDrop) }
        fun seconds(bars: Int) = bars * beatsPerBar * 60.0 / bpm
        val max = maxSeconds(long)
        fun fits(l: List<Span>) = seconds(l.sumOf { it.bars }) <= max
        fun early(l: List<Span>) = !form.sung || seconds(l.takeWhile { it.sec != Sec.Drop }.sumOf { it.bars }) <= FIRST_CHORUS_SECONDS
        return list.firstOrNull { fits(it) && early(it) } ?: list.firstOrNull { fits(it) } ?: list.last()
    }

    private fun adapt(spans: List<Span>, form: SongForm, hasBreak: Boolean, fakeDrop: Boolean): List<Span> {
        var out = if (hasBreak) spans else when (form) {
            // Club music without a Break grooves on before its Montée.
            SongForm.Club, SongForm.Rave -> spans.map { if (it.sec == Sec.Break) it.copy(sec = Sec.Verse) else it }
            else -> spans.filter { it.sec != Sec.Break }
        }
        if (fakeDrop) {
            val first = out.indexOfFirst { it.sec == Sec.Drop }
            if (first > 0) out = out.take(first) + Span(Sec.Fake, 4) + out.drop(first)
        }
        return out
    }

    /** Musical content: the drum lines, the parts and the acid line a scene plays. */
    data class Content(val lines: Set<Role>, val parts: Set<Part>, val acid: Boolean) {
        val size: Int get() = lines.size + parts.size + (if (acid) 1 else 0)
    }

    /** What the riser track plays in a scene: nothing, a note held all along (its filter opens it), or a hit on its first step. */
    enum class Riser { None, Held, Hit }

    /**
     * A scene of the song: one of the plan's ([base] its own index) or a variant of scene [base].
     * [half]: -1 its whole loop, 0 or 1 the first or the last 4 bars of its 8-bar phrase (a scene of 4 bars);
     * [cutBars]: the kick and the bass (808, rumble, sub, second bass) out of its last bars;
     * [silence]: its last beat silent, every track; [roll]: the roll line (snare or clap) rolls, 1 the
     * first half of an 8-bar roll (quarters, eighths), 2 its second half (sixteenths, then rolls of 2,
     * 3, 4), 3 a one-bar roll at its end (a song's); [capped]: a Montée, held under the Drop (its roll,
     * chords and riser softer); [accent]: the first beat of a Drop (crash, impact, [subDrop]);
     * [counter]: the counter-melody plays; [bass]: the sub and the second bass play with the bass;
     * [vocal]: the vocal track plays.
     */
    data class SceneSpec(
        val base: Int,
        val label: String,
        val content: Content,
        val version: Version,
        val half: Int = -1,
        val cutBars: Int = 0,
        val silence: Boolean = false,
        val roll: Int = 0,
        val capped: Boolean = false,
        val accent: Boolean = false,
        val subDrop: Boolean = accent,
        val counter: Boolean = false,
        val riser: Riser = Riser.None,
        val bass: Boolean = true,
        val vocal: Boolean = false,
        /** The last Drop's own scene (G0): the hook doubled and a drum layer more ([StarterWizard.withDropLayer]). */
        val extra: Boolean = false,
    )

    /** What a block can leave out of its parts (their tracks muted there): the chords, the hook, the counter-melody, the vocal. */
    enum class Layer { Pad, Lead, Counter, Vocal }

    /** [scene] played for [bars] bars, as part of a stretch [sec], without its [mute] layers. */
    data class Block(val scene: Int, val bars: Int, val sec: Sec, val mute: Set<Layer> = emptySet())

    /** The scenes of a song (the plan's first, then the variants) and the blocks that play them. */
    data class Shape(val scenes: List<SceneSpec>, val blocks: List<Block>, val spans: List<Span>) {
        /**
         * The song: each block's scene repeated over its bars ([barsOf]: a scene's length), the tracks
         * of its [Block.mute] layers muted there ([keysOf]: a layer's track keys).
         */
        fun parts(barsOf: (Int) -> Int, keysOf: (Layer) -> Set<String> = { emptySet() }): List<SongPart> {
            val out = ArrayList<SongPart>()
            for (b in blocks) {
                var left = (b.bars / barsOf(b.scene).coerceAtLeast(1)).coerceAtLeast(1)
                val muted = b.mute.flatMap(keysOf).toSet()
                // Consecutive blocks of one scene make one part (as many as a part can repeat).
                val last = out.lastOrNull()
                if (last != null && last.scene == b.scene && last.muted == muted && last.repeats < SongPart.MAX_REPEATS) {
                    val n = minOf(left, SongPart.MAX_REPEATS - last.repeats)
                    out[out.size - 1] = last.copy(repeats = last.repeats + n)
                    left -= n
                }
                while (left > 0) {
                    val n = left.coerceAtMost(SongPart.MAX_REPEATS)
                    out += SongPart(b.scene, n, muted)
                    left -= n
                }
            }
            return out
        }

        val bars: Int get() = blocks.sumOf { it.bars }
    }

    /**
     * Bars a club Montée plays without its kick and its bass before the Drop: its whole second half
     * (S5b: 2 before). Real builds take the kick out there and let the roll, the riser and the filter
     * carry the rise; with the kick in, the Montée's level fell as it left 2 bars before the Drop.
     * The 303 of the acid styles plays on up to the silent beat ([ACID_CUT_BARS]): it carries their
     * build (its filter opening), and it stopping 2 bars before the Drop made their Montée fall.
     */
    const val CLUB_CUT_BARS = 4

    /** A song's Montée (its Pré-refrain) and a light club one ([StarterWizard.Style.lightBuild]): the kick out of its last bar only. */
    const val LIGHT_CUT_BARS = 1
    const val ACID_CUT_BARS = 0

    /** Bars of a Montée scene (a half of the 8-bar phrase), of a Post-refrain, of a fake drop. */
    const val HALF_BARS = 4

    /** The drum lines in the order they come in, a new one every 8 bars (The Bells, Energy Flash): the kick and its rumble first. */
    private val ENTRY = listOf(Role.Kick, Role.Perc, Role.Tom, Role.Hat, Role.Perc2, Role.Clap, Role.Snare, Role.OpenHat, Role.Ride, Role.Bass808)

    /** The lines a techno Drop takes out for 8 bars (one of them, the first it plays): mutes by loops. */
    private val MUTES = listOf(Role.Perc2, Role.Perc, Role.Tom, Role.OpenHat, Role.Ride, Role.Hat)

    /** The lines that are the bass of the beat: out of the bars before a Drop with the bass. */
    val BASS_LINES = setOf(Role.Kick, Role.Bass808, Role.Rumble)

    /**
     * [n] contents from [from] to [to], one more layer every block (several when there are more
     * layers than blocks), the last one [to]: drum lines in the order they come in ([ENTRY], the
     * rumble with the kick), then the acid line, then the parts (bass, chords, lead).
     */
    fun ladder(from: Content, to: Content, n: Int): List<Content> {
        if (n <= 1) return listOf(to)
        val items = ArrayList<Content>()
        for (r in ENTRY) if (r in to.lines && r !in from.lines) {
            items += Content(if (r == Role.Kick && Role.Rumble in to.lines) setOf(r, Role.Rumble) else setOf(r), emptySet(), false)
        }
        // Lines that ENTRY does not list come with the first one.
        val listed = items.flatMap { it.lines }.toSet()
        val rest = to.lines - from.lines - listed
        if (rest.isNotEmpty()) {
            if (items.isEmpty()) items += Content(rest, emptySet(), false) else items[0] = items[0].copy(lines = items[0].lines + rest)
        }
        if (to.acid && !from.acid) items += Content(emptySet(), emptySet(), true)
        for (p in Part.entries) if (p in to.parts && p !in from.parts) items += Content(emptySet(), setOf(p), false)
        val m = items.size
        return List(n) { k ->
            if (k == n - 1 || m == 0) to
            else {
                val take = ((k + 1) * m + n - 1) / n
                items.take(take.coerceIn(1, m)).fold(from.copy(lines = from.lines intersect to.lines, parts = from.parts intersect to.parts, acid = from.acid && to.acid)) { acc, c ->
                    Content(acc.lines + c.lines, acc.parts + c.parts, acc.acid || c.acid)
                }
            }
        }
    }

    /**
     * The shape of [plan]'s song at [bpm] ([style] gives the family and the transitions: the long
     * forms, the techno mutes, the fake drop, the vocals).
     */
    fun shape(style: Style?, plan: ScenePlan, bpm: Double = style?.bpm ?: 120.0, beatsPerBar: Int = 4): Shape {
        val long = style != null && isLong(style)
        val form = plan.form
        val spans = layout(form, bpm, plan.hasBreak, long, fakeDrop = style?.fakeDrop == true, beatsPerBar = beatsPerBar)
        fun content(i: Int) = Content(
            plan.lines.filterValues { i in it }.keys, plan.parts.filterValues { i in it }.keys, i in plan.acid,
        )
        val club = form == SongForm.Club || form == SongForm.Rave || form == SongForm.Build
        // Final audit (report 11): club music that does not build like EDM (Style.lightBuild).
        val light = club && form != SongForm.Build && style?.lightBuild == true
        val vocalIn = style?.vocal?.let { vocalScenes(it) }.orEmpty()
        val scenes = ArrayList<SceneSpec>()
        for (i in 0 until plan.count) {
            scenes += SceneSpec(
                base = i, label = plan.labels[i], content = content(i),
                version = when (i) { 0 -> Version.Intro; 1 -> Version.Verse; else -> Version.Chorus },
                roll = if (form == SongForm.Build && i == 1) 1 else 0,
                capped = form == SongForm.Build && i == 1,
                accent = i == 2, subDrop = i == 2 && !light, counter = i == 2,
                riser = when (i) { 2 -> Riser.Hit; 1, 3 -> Riser.Held; else -> Riser.None },
                vocal = i in vocalIn,
            )
        }
        // Nothing to play (no line, no part, no acid line): the plan's sections alone.
        if (plan.isEmpty) return Shape(scenes, spans.map { Block(it.sec.base.coerceAtMost(plan.count - 1), it.bars, it.sec) }, spans)
        val numbers = HashMap<String, Int>()
        /** The scene [spec] plays: an existing one with the same content and treatment, or a new variant. */
        fun sceneOf(spec: SceneSpec): Int {
            val same = scenes.indexOfFirst { it.copy(label = "") == spec.copy(label = "") }
            if (same >= 0) return same
            scenes += spec
            return scenes.size - 1
        }
        fun numbered(label: String): String {
            val n = (numbers[label] ?: 1) + 1
            numbers[label] = n
            return "$label $n"
        }
        val dropLabel = plan.labels[2]
        val verseLabel = plan.labels[1]
        // The roll line of a Montée: the snare, or the clap (the hats in a song without either).
        val rollLine = listOf(Role.Snare, Role.Clap).firstOrNull { r -> plan.lines[r].orEmpty().isNotEmpty() }
        val leadContent = content(1).let { c -> if (club && rollLine != null) c.copy(lines = c.lines + rollLine) else c }
        val leadALabel = when (form) {
            SongForm.Song -> PRE_CHORUS
            SongForm.Build -> plan.labels[1]
            SongForm.Rap -> "$verseLabel 2"
            else -> MONTEE
        }
        // Whether the Montées have their two halves (else the second alone, named as the first).
        val halves = form == SongForm.Build || spans.any { it.sec == Sec.Lead && it.bars >= 2 * HALF_BARS }
        // The Montée in two scenes of 4 bars (the Build's own Montée is its first half). Each variant is
        // made when the song first needs it: the scenes come in the order they are heard.
        val leadA by lazy {
            if (form == SongForm.Build) 1
            else sceneOf(SceneSpec(
                base = 1, label = if (form == SongForm.Rap) numbered(verseLabel) else leadALabel,
                content = leadContent, version = Version.Verse, half = 0, roll = if (club && !light) 1 else 0, capped = club,
                riser = Riser.Held, vocal = 1 in vocalIn,
            ))
        }
        val leadB by lazy {
            sceneOf(SceneSpec(
                base = 1, label = if (form == SongForm.Rap) numbered(verseLabel) else if (halves) "$leadALabel 2" else leadALabel,
                content = if (form == SongForm.Build) content(1) else leadContent, version = Version.Verse, half = 1,
                cutBars = when {
                    form == SongForm.Song || light -> LIGHT_CUT_BARS
                    // Something else plays there (a beat without kick, a part, the 303): else the kick stays its first 2 bars.
                    club && (leadContent.lines.any { it !in BASS_LINES } || leadContent.parts.any { it != Part.Bass } || leadContent.acid) -> CLUB_CUT_BARS
                    else -> 2
                },
                silence = !light, roll = if (club && !light) 2 else 3, capped = club,
                riser = Riser.Held, vocal = 1 in vocalIn,
            ))
        }
        // The Drop's second scene: the hook doubled (Chorus2); in techno a line muted for 8 bars.
        val drop = scenes[2]
        val drop2 by lazy {
            sceneOf(drop.copy(label = numbered(dropLabel), version = Version.Chorus2, accent = false, riser = Riser.None, vocal = 2 in vocalIn || VOCAL_SECOND in vocalIn))
        }
        val mute by lazy {
            if (style?.family == "Techno" && long) MUTES.firstOrNull { it in drop.content.lines && drop.content.lines.size > 2 }?.let { r ->
                sceneOf(drop.copy(label = numbered(dropLabel), content = drop.content.copy(lines = drop.content.lines - r), accent = false, riser = Riser.None))
            } else null
        }
        val post by lazy {
            val withoutChords = Part.Lead in drop.content.parts && Part.Pad != style?.melody?.signature
            sceneOf(drop.copy(
                label = POST_CHORUS, content = drop.content.copy(parts = drop.content.parts - (if (withoutChords) setOf(Part.Pad) else emptySet())),
                version = Version.Chorus2, half = 0, accent = false, counter = false, riser = Riser.None,
            ))
        }
        // The fake drop needs something besides the bass to play (none: no fake drop).
        val fake by lazy {
            val beat = drop.content.copy(lines = drop.content.lines - Role.Bass808 - Role.Rumble, parts = drop.content.parts - Part.Bass)
            if (beat.size == 0) -1 else sceneOf(drop.copy(
                label = FAKE_DROP, content = drop.content.copy(lines = drop.content.lines - Role.Bass808 - Role.Rumble, parts = drop.content.parts - Part.Bass),
                half = 0, silence = true, subDrop = false, counter = false, bass = false,
            ))
        }
        // A new layer every 8 bars in the long intros and grooves of club music; they leave again in the outro.
        val introSpan = spans.firstOrNull { it.sec == Sec.Intro }?.bars ?: 0
        val introLadder by lazy {
            if (club && introSpan >= 16) ladder(Content(emptySet(), emptySet(), false), content(0), introSpan / 8)
                .map { c -> if (c == content(0)) 0 else sceneOf(scenes[0].copy(label = numbered(plan.labels[0]), content = c)) }
            else listOf(0)
        }
        val grooveSpan = spans.firstOrNull { it.sec == Sec.Verse }?.bars ?: 0
        val grooveLadder by lazy {
            if (club && form != SongForm.Build && grooveSpan >= 16) ladder(content(0), content(1), grooveSpan / 8)
                .map { c -> if (c == content(1)) 1 else sceneOf(scenes[1].copy(label = numbered(verseLabel), content = c)) }
            else listOf(1)
        }
        // G0: the last Drop's own scene (the hook doubled, a drum layer more). One scene only: with its accent
        // (crash, impact, sub drop on its first beat) when a Drop opens on it (a single-block last Drop, a
        // middle Drop of 16), then also where the last Drop's second 8 bars play it (a Drop hit again).
        val dropSpans = spans.filter { it.sec == Sec.Drop }
        val dropCount = dropSpans.size
        val opensOnFinale = dropSpans.withIndex().any { (i, sp) ->
            val n = sp.bars / 8
            (n <= 1 && dropCount >= 2 && i == dropCount - 1) || (n == 2 && i in 1 until dropCount - 1)
        }
        val finale by lazy {
            sceneOf(drop.copy(
                label = numbered(dropLabel), version = Version.Chorus2, vocal = 2 in vocalIn || VOCAL_SECOND in vocalIn, extra = true,
                accent = opensOnFinale && drop.accent, riser = if (opensOnFinale) drop.riser else Riser.None,
            ))
        }
        val lean = style?.id in LEAN_DROP_STYLES && (Part.Pad in drop.content.parts || Part.Lead in drop.content.parts)
        // A middle Drop of 8 bars breathes without its chords (without its hook when it has none).
        val breathe = if (Part.Pad in drop.content.parts) setOf(Layer.Pad) else setOf(Layer.Lead, Layer.Counter)
        var drops = 0
        val blocks = ArrayList<Block>()
        var verses = 0
        for (span in spans) {
            fun add(scene: Int, bars: Int, mute: Set<Layer> = emptySet()) { blocks += Block(scene, bars, span.sec, mute) }
            fun ladderOf(ladder: List<Int>) {
                val n = span.bars / 8
                for (k in 0 until n) add(ladder[(k * ladder.size / n).coerceAtMost(ladder.size - 1)], 8)
            }
            when (span.sec) {
                Sec.Intro -> if (introLadder.size > 1) ladderOf(introLadder) else add(0, span.bars)
                Sec.Outro -> if (introLadder.size > 1 && span.bars >= 16) ladderOf(introLadder.reversed()) else add(0, span.bars)
                // The first Groove brings its layers in; the later ones play it whole.
                Sec.Verse -> if (verses++ == 0 && grooveLadder.size > 1 && span.bars >= 16) ladderOf(grooveLadder) else add(1, span.bars)
                Sec.Lead -> if (span.bars >= 2 * HALF_BARS) {
                    add(leadA, HALF_BARS)
                    add(leadB, span.bars - HALF_BARS)
                } else add(leadB, span.bars)
                Sec.Drop -> {
                    // G0: no two Drops alike. The first opens on the bass and the beat alone in bass music
                    // (its first 8 bars, when it has more); a middle one ends on the last Drop's scene (one
                    // of 16 opens on it, one of 8 breathes without its chords); the last one answers each 8
                    // with its own scene, fuller.
                    val d = drops++
                    val last = dropCount >= 2 && d == dropCount - 1
                    val middle = d in 1 until dropCount - 1
                    val n = span.bars / 8
                    if (n <= 1) add(if (last) finale else 2, span.bars, if (middle) breathe else emptySet())
                    else for (k in 0 until n) {
                        var scene = if (k % 2 == 1) drop2 else if (k % 4 == 2) mute ?: 2 else 2
                        val muted = if (d == 0 && k == 0 && lean) LEAN else emptySet()
                        if (last && k % 2 == 1) scene = finale
                        if (middle && n >= 3 && k >= n / 2) scene = finale
                        if (middle && n == 2 && k == 0) scene = finale
                        add(scene, 8, muted)
                    }
                }
                Sec.Fake -> if (fake >= 0) add(fake, span.bars)
                Sec.Post -> add(post, span.bars)
                Sec.Break -> add(3, span.bars)
            }
        }
        return Shape(scenes, blocks, spans)
    }

    /**
     * G0 (docs/hits/bass.md § 5, « Drop 1A »): the bass music styles whose first Drop opens on the
     * bass and the beat alone for 8 bars (no chords, no hook, no counter-melody, no vocal). Liquid
     * keeps its chords (they are the genre), jungle its ride and its Hoover.
     */
    val LEAN_DROP_STYLES = setOf("dnb", "neurofunk", "jump_up", "dubstep", "riddim")

    /** What the lean opening of a Drop leaves out. */
    val LEAN = setOf(Layer.Pad, Layer.Lead, Layer.Counter, Layer.Vocal)

    /** The section names the variants take (saved in French, translated at display). */
    const val MONTEE = "Montée"
    const val PRE_CHORUS = "Pré-refrain"
    const val POST_CHORUS = "Post-refrain"
    const val FAKE_DROP = "Faux drop"

    /** A marker in [vocalScenes]: the vocal plays in the Drop's second scene. */
    private const val VOCAL_SECOND = -2

    /** The scenes of the plan a vocal of [kind] plays in (the Drop, the Refrain…). */
    private fun vocalScenes(kind: StarterWizard.Vocal): Set<Int> = when (kind) {
        // Chops that answer the hook, stabs every 4 bars: the Drop (the Refrain).
        StarterWizard.Vocal.Answer, StarterWizard.Vocal.Stab -> setOf(2)
        // An MC's shout: on the second scene of each Drop.
        StarterWizard.Vocal.Shout -> setOf(VOCAL_SECOND)
        // The phonk's crushed vocal hit: the Couplet and the Refrain.
        StarterWizard.Vocal.Crush -> setOf(1, 2)
    }
}
