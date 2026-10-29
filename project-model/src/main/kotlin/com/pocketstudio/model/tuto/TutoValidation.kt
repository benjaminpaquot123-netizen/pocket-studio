package com.pocketstudio.model.tuto

import com.pocketstudio.model.EffectType
import com.pocketstudio.model.LoudnessTarget
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.StarterWizard.Part

/**
 * What is wrong in a course file, in words an author can act on (« make_intro: no part
 * « Couplet » to listen to »): every field the screen reads, every detector, every part named, every
 * note readable. Empty: the course can be played. The tests run it on each file of `tutos/`
 * (docs/tuto/AUTHORING.md).
 */
object TutoValidation {
    /** The screens « Je le fais » can open (the app's GuideNav.SPOTS, and the key's own sheet). */
    val SPOTS = setOf(
        "tempo", "key", "song.parts", "sounds.track", "sounds.favorite", "sounds.layers", "rhythm.kit", "rhythm.pattern", "song.automation",
        "record.voice", "mixer.master", "mixer.whatplays", "notes.track", "mixer.strip", "share.check", "share.export", "share.video", "acid", "acid.generate",
        "song.add", "tuto.key",
    )

    /** The roles a step can name: the drum roles, the tracks' and the 303. */
    val ROLES: Set<String> = TutoChecks.DRUM_ROLES.toSet() + setOf("bass", "sub", "pad", "lead", "acid")

    /** What "keep" can hold besides [ROLES]: every brick ("*"), every pad ("drums"), the pads no line names ("perc"). */
    private val KEEP_EXTRA = setOf("*", "drums", "perc")

    private val KNOBS = setOf("CUTOFF", "RESONANCE", "OSC2_DETUNE", "GLIDE", "A_ATTACK", "UNISON_DETUNE")

    /** The checks with no argument. */
    private val PLAIN = setOf("manual", "listen", "tempo", "key", "swing", "velocity:hat", "track:sub", "automation", "master", "shared", "song:order", "song:phrases", "fill", "crash", "gap")

    fun problems(course: TutoCourse, file: String): List<String> {
        val out = ArrayList<String>()
        fun bad(what: String) { out += what }
        if (course.style.isEmpty() || course.style != file.removeSuffix(".json")) bad("style « ${course.style} » is not the file's name « $file »")
        val style = StarterWizard.style(course.style) ?: return out.also { bad("no style « ${course.style} » in the assistant") }
        val target = TutoTarget.of(style.id) ?: return out
        if (course.mood.isNotEmpty() && StarterWizard.Mood.entries.none { it.id == course.mood }) bad("mood « ${course.mood} »: one of ${StarterWizard.Mood.entries.map { it.id }}")
        if (course.key.isNotEmpty() && TutoCourse.keyOf(course.key) == null) bad("key « ${course.key} »: « A minor », « F# dorian »…")
        if (course.chords.isNotEmpty() && TutoCourse.degreesOf(course.chords).isNullOrEmpty()) bad("chords « ${course.chords} »: Roman numerals, « i iv VI »")
        course.lines.forEach { (role, line) ->
            if (role !in TutoCourse.LINE_ROLES) bad("lines: role « $role » (one of ${TutoCourse.LINE_ROLES})")
            runCatching { TutoTarget.entries(line) }.onFailure { bad("lines.$role: ${it.message}") }.onSuccess { e ->
                if (e.isEmpty()) bad("lines.$role: no note")
                e.flatMap { it.pitches }.filter { it !in 12..108 }.forEach { bad("lines.$role: pitch $it out of reach") }
                val bars = course.lineBars[role]
                if (bars != null && e.any { it.step >= bars * 16 }) bad("lines.$role: a note after its $bars bars")
            }
            if (role == TutoTarget.ACID && target.acid == null) bad("lines.acid: the style has no 303")
        }
        course.lineBars.forEach { (role, bars) -> if (bars !in listOf(1, 2, 4, 8) || role !in course.lines) bad("lineBars.$role: $bars (1, 2, 4 or 8, for a line written)") }
        course.follow.forEach { if (it !in course.lines) bad("follow: « $it » has no line") }
        val steps = course.chapters.flatMap { it.steps }
        if (course.chapters.isEmpty()) bad("no chapter")
        (course.chapters.map { it.id }.groupBy { it } + steps.map { it.id }.groupBy { it }).filterValues { it.size > 1 }.keys.forEach { bad("id « $it » twice") }
        course.chapters.forEach { c ->
            if (c.levels !in LEVELS) bad("${c.id}: levels « ${c.levels} »")
            if (c.title.isBlank()) bad("${c.id}: no title")
            if (c.steps.isEmpty()) bad("${c.id}: no step")
        }
        for (level in TutoLevel.entries) if (course.chaptersFor(level).isEmpty()) bad("nothing in ${level.name}")
        // The parts the course makes (its part: and label: steps): those a step may work in.
        val made = steps.mapNotNull { s ->
            when {
                s.check.startsWith("part:") -> s.check.removePrefix("part:").substringBeforeLast(':')
                s.check.startsWith("label:") -> s.check.removePrefix("label:")
                else -> null
            }
        }.toSet()
        fun madeOrEnding(label: String) = label in made || (label in TutoTarget.ENDINGS && "Intro" in made)
        for (s in steps) {
            val id = s.id
            if (s.text.isBlank()) bad("$id: no text")
            if (s.levels !in LEVELS) bad("$id: levels « ${s.levels} »")
            checkProblem(s, target, made)?.let { bad("$id: $it") }
            if (s.spot.isNotEmpty() && s.spot !in SPOTS) bad("$id: spot « ${s.spot} » (one of $SPOTS)")
            s.target.split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach { if (it !in ROLES && it != "drop") bad("$id: target « $it » (one of $ROLES)") }
            s.keep.split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach { if (it !in ROLES && it !in KEEP_EXTRA) bad("$id: keep « $it » (one of ${ROLES + KEEP_EXTRA})") }
            if (s.keep.isNotEmpty() && !s.check.startsWith("part:")) bad("$id: keep without a part: check")
            if (s.lanesFrom.isNotEmpty() && target.sceneOf(s.lanesFrom) < 0) bad("$id: lanesFrom « ${s.lanesFrom} »: no such part in the style's song")
            if (s.build.isNotEmpty() && s.build !in TutoChecks.DRUM_ROLES) bad("$id: build « ${s.build} » is no drum role")
            if (s.transpose != 0 && !s.check.startsWith("part:")) bad("$id: transpose without a part: check")
            if (s.transpose != 0 && Part.Bass.id !in course.lines && target.notes(Part.Bass).notes.isEmpty()) bad("$id: transpose without a bass")
            if (s.demo.isNotEmpty() && (s.demo != "kick" || TutoTarget.kitOf(target.style) == null)) bad("$id: demo « ${s.demo} » (« kick », for a style starting on a kit of TutoTarget.KITS)")
            if (s.part.isNotEmpty()) TutoChecks.partsOf(s).forEach { if (!madeOrEnding(it)) bad("$id: part « $it » is made by no step") }
            if (s.length < 0 || s.length > 64) bad("$id: length ${s.length}")
            listenProblem(s.listen, target)?.let { bad("$id: listen « ${s.listen} »: $it") }
            if (s.listening && s.listen.isEmpty()) bad("$id: a listening step with nothing to listen to")
            if (s.hearMine && !s.listening) bad("$id: hearMine on a step that is not a listening one")
            if (s.song.isNotEmpty()) {
                val parts = s.song.split(',').map { it.trim() }
                if (parts.size < 3) bad("$id: song « ${s.song} »: three parts or more")
                parts.forEach { e ->
                    val label = e.substringBeforeLast(':').trim()
                    val bars = e.substringAfterLast(':', "").toIntOrNull()
                    if (':' !in e || bars == null || bars !in 1..128) bad("$id: song « $e »: Label:bars")
                    if (!madeOrEnding(label)) bad("$id: song: part « $label » is made by no step")
                }
            }
        }
        return out
    }

    private val LEVELS = setOf("f", "p", "fp")

    /** What is wrong with [s]'s check, null when it is one the detectors know. */
    private fun checkProblem(s: TutoStep, target: TutoTarget, made: Set<String>): String? {
        val c = s.check
        val arg = TutoChecks.argOf(c)
        return when {
            c in PLAIN -> when {
                c == "swing" && target.style.swing <= 0.0 -> "swing: the style « ${target.style.id} » does not swing"
                (c == "fill" || c == "gap" || c == "automation") && s.part.isEmpty() && "Montée" !in made -> "$c in the Montée, which no step makes: give it a part"
                c == "crash" && s.part.isEmpty() && "Drop" !in made -> "crash in the Drop, which no step makes: give it a part"
                c == "fill" && s.target.isNotEmpty() && s.target !in TutoChecks.DRUM_ROLES -> "fill on « ${s.target} », no drum role"
                c == "track:sub" && target.notes(Part.Bass).notes.isEmpty() -> "track:sub without a bass"
                else -> null
            }
            c.startsWith("lane:") -> when {
                c.removePrefix("lane:$arg").let { it.isNotEmpty() && it != ":rolls" } -> "lane:<role> or lane:<role>:rolls"
                target.line(arg) == null -> "the style has no « $arg » line (${target.style.lines.map { it.role.id }})"
                else -> null
            }
            c == "notes:acid" -> if (target.acid == null) "the style has no 303 line" else null
            c.startsWith("notes:") -> when (val part = Part.entries.firstOrNull { it.id == arg }) {
                null -> "notes:<bass|pad|lead|acid>"
                else -> if (target.notes(part).notes.isEmpty()) "no notes for « $arg »" else null
            }
            c.startsWith("offset:") -> if (arg.substringBefore(':') !in TutoChecks.DRUM_ROLES || arg.substringAfter(':', "late") !in setOf("late", "early")) "offset:<drum role>:<late|early>" else null
            c.startsWith("automation:") -> if (arg !in TutoChecks.CURVE_SHAPES) "automation:<${TutoChecks.CURVE_SHAPES.joinToString("|")}>" else null
            c.startsWith("insert:") -> if (EffectType.entries.none { it.name == arg }) "no effect « $arg »" else null
            c.startsWith("send:") -> if (arg !in setOf("0", "1")) "send:0 (reverb) or send:1 (echo)" else null
            c.startsWith("loudness:") -> if (LoudnessTarget.entries.none { it.name == arg }) "loudness:<${LoudnessTarget.entries.joinToString("|")}>" else null
            c.startsWith("changed:volumes:") -> arg.removePrefix("volumes:").split(',').firstOrNull { it.trim() !in ROLES }?.let { "no role « $it »" }
            c == "changed:volumes" || c == "changed:kit" -> null
            c.startsWith("changed:synth:") -> {
                val spec = c.removePrefix("changed:synth:")
                when {
                    spec.substringBefore(':') !in setOf("bass", "sub", "pad", "lead") -> "changed:synth:<bass|sub|pad|lead>:<KNOB+KNOB>"
                    spec.substringAfter(':', "").split('+').filter { it.isNotEmpty() }.any { it !in KNOBS } -> "knobs: $KNOBS"
                    else -> null
                }
            }
            c.startsWith("changed:drums:") -> if (c.removePrefix("changed:drums:") !in made) "no step makes the part « ${c.removePrefix("changed:drums:")} »" else null
            c.startsWith("label:") -> if (arg.isBlank()) "label:<name>" else null
            c.startsWith("part:") -> {
                val spec = c.removePrefix("part:")
                val label = spec.substringBeforeLast(':')
                val modes = spec.substringAfterLast(':', "").split('+')
                when {
                    ':' !in spec || label.isBlank() -> "part:<Label>:<mode>"
                    modes.any { it !in TutoChecks.PART_MODES } -> "modes: ${TutoChecks.PART_MODES}"
                    else -> null
                }
            }
            else -> "unknown check « $c »"
        }
    }

    /** What is wrong with a step's [listen], null when its example can be made. */
    private fun listenProblem(l: String, target: TutoTarget): String? {
        if (l.isEmpty() || l == "song" || l == "fx:reverb") return null
        val arg = l.substringAfter(':', "")
        return when {
            l.startsWith("drums:") || l.startsWith("parts:") -> arg.split(',').map { it.trim() }.firstOrNull { r ->
                when {
                    r in TutoChecks.DRUM_ROLES -> target.line(r) == null
                    r == "acid" -> target.acid == null
                    r in setOf("bass", "pad", "lead") -> false
                    else -> true
                }
            }?.let { "« $it » is nothing the style plays" }
            l.startsWith("part:") -> if (target.sceneOf(arg) < 0) "no part « $arg » in the style's song" else null
            l.startsWith("vs:") -> arg.split(':').firstOrNull { target.sceneOf(it) < 0 }?.let { "no part « $it » in the style's song" }
            else -> "unknown"
        }
    }
}
