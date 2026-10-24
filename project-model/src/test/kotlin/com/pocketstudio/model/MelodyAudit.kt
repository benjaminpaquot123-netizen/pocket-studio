package com.pocketstudio.model

import com.pocketstudio.model.StarterWizard.LeadKind
import com.pocketstudio.model.StarterWizard.Mood
import com.pocketstudio.model.StarterWizard.Part
import com.pocketstudio.model.StarterWizard.Version
import com.pocketstudio.model.StarterWizard.Style

/**
 * The melody rules (docs/hits/autres-et-melodie.md § B13, M1–M20) applied to every style and
 * mood of the assistant: what [MelodyRulesTest] asserts and [MelodyReportTest] tabulates.
 */
object MelodyAudit {
    /** One line of the song judged: the clip, its report and what it fails. */
    data class Judged(val style: Style, val mood: Mood, val line: String, val clip: NoteClip, val report: MelodyMetrics.Report?, val failures: List<String>)

    fun context(style: Style, mood: Mood): MelodyMetrics.Context {
        val (_, degrees) = StarterWizard.progressionOf(style, mood)
        val seventh = style.melody.voicing in setOf(Voicing.Seventh, Voicing.Ninth, Voicing.Rootless7, Voicing.Rootless9)
        return MelodyMetrics.Context(StarterWizard.harmonyOf(style, mood), degrees, seventh)
    }

    /** The style has a written lead (a riff, a hook, chops) to judge as a line. */
    fun hasLeadLine(style: Style): Boolean = style.melody.leadPattern.isNotBlank() && style.melody.leadKind != LeadKind.Arp

    /** The style's lead is an arpeggio. */
    fun hasLeadArp(style: Style): Boolean = style.melody.leadArp != null || (style.melody.leadPattern.isBlank() && style.melody.leadKind == LeadKind.Arp)

    fun lead(style: Style, mood: Mood, version: Version = Version.Chorus): NoteClip =
        StarterWizard.startingNotes(Part.Lead, style, mood, StarterWizard.harmonyOf(style, mood), version = version)

    fun bass(style: Style, mood: Mood): NoteClip = StarterWizard.startingNotes(Part.Bass, style, mood, StarterWizard.harmonyOf(style, mood))

    fun pad(style: Style, mood: Mood): NoteClip = StarterWizard.startingNotes(Part.Pad, style, mood, StarterWizard.harmonyOf(style, mood))

    /** The song the assistant makes with every default part and the lead. */
    fun song(style: Style, mood: Mood): Project {
        val lines = style.lines.filter { !it.off }.associate { it.role to null as PadSample? }
        val parts = (StarterWizard.defaultParts(style) + Part.Lead).associateWith { StarterWizard.soundsFor(it, style).first() }
        return StarterWizard.build(style, lines, mood, parts)
    }

    /** M1–M15, M18 on the lead's Refrain version, plus M19 and M20 from its other versions and the song. */
    fun judgeLead(style: Style, mood: Mood, withSong: Boolean = true): Judged {
        val ctx = context(style, mood)
        val chorus = lead(style, mood)
        val report = MelodyMetrics.analyse(chorus, ctx)
        val failures = ArrayList(MelodyRules.failures(report, MelodyRules.forLead(style.melody.leadKind)))
        // M19: the Couplet keeps at most 60 % of the Refrain's notes, the Intro a preview, the second Refrain an octave more.
        val verse = MelodyMetrics.variant(lead(style, mood, Version.Verse), chorus)
        if (!verse.subset || verse.share > 0.6f || verse.share == 0f) failures += "M19 verse share ${"%.2f".format(java.util.Locale.ROOT, verse.share)} subset ${verse.subset}"
        val intro = lead(style, mood, Version.Intro)
        if (intro.notes.isEmpty() || !MelodyMetrics.variant(intro, chorus).subset) failures += "M19 intro is not a preview of the hook"
        val second = lead(style, mood, Version.Chorus2)
        if (second.notes == chorus.notes) failures += "M19 second refrain unchanged"
        if (withSong) {
            // M20: the hook is heard in at least three sections of the song.
            val p = song(style, mood)
            val leadTrack = p.instrumentTracks.firstOrNull { it.name == "Lead" }
            val sections = StarterWizard.songSections(p)
            val heard = sections.count { sec -> leadTrack != null && (p.allScenes[sec.scene].let { if (leadTrack.id == 0) it.synthNotes else it.trackNotes[leadTrack.id] }?.notes?.isNotEmpty() == true) }
            // A song previews its hook and plays it in both Refrains and the outro; club music plays it in its Drops (and its Break).
            val wanted = if (style.form == StarterWizard.SongForm.Song) 3 else 2
            if (heard < wanted) failures += "M20 hook heard in $heard sections"
        }
        return Judged(style, mood, "lead", chorus, report, failures)
    }

    fun judgeBass(style: Style, mood: Mood): Judged {
        val clip = bass(style, mood)
        val report = MelodyMetrics.analyse(clip, context(style, mood))
        return Judged(style, mood, "bass", clip, report, MelodyRules.failures(report, MelodyRules.BASS))
    }

    /** M16: the counter-melody answers in the lead's rests, rarely with it. */
    fun judgeCounter(style: Style, mood: Mood): Judged? {
        if (style.melody.counter == null || !hasLeadLine(style)) return null
        val p = song(style, mood)
        val track = p.instrumentTracks.firstOrNull { it.name == StarterWizard.COUNTER_TRACK } ?: return null
        val counter = p.allScenes[2].let { if (track.id == 0) it.synthNotes else it.trackNotes[track.id] } ?: return null
        val leadTrack = p.instrumentTracks.first { it.name == "Lead" }
        val leadClip = p.allScenes[2].let { if (leadTrack.id == 0) it.synthNotes else it.trackNotes[leadTrack.id] } ?: return null
        val response = MelodyMetrics.response(leadClip, counter)
        val failures = ArrayList<String>()
        if (counter.notes.isEmpty()) failures += "M16 no counter note"
        if (response.gaps > 0 && response.gapsAnswered < response.gaps) failures += "M16 rests answered ${response.gapsAnswered}/${response.gaps}"
        if (response.simultaneous >= 0.3f) failures += "M16 simultaneous ${"%.2f".format(java.util.Locale.ROOT, response.simultaneous)}"
        return Judged(style, mood, "counter", counter, null, failures)
    }

    /** M17: the arpeggios (a lead arpeggio, arpeggiated chords). */
    fun judgeArps(style: Style, mood: Mood): List<Judged> {
        val out = ArrayList<Judged>()
        if (hasLeadArp(style)) {
            val clip = lead(style, mood)
            out += Judged(style, mood, "lead arp", clip, null, MelodyRules.arpFailures(MelodyMetrics.arp(clip), style.bpm))
        }
        if (style.melody.chordArp != null) {
            val clip = pad(style, mood)
            out += Judged(style, mood, "chord arp", clip, null, MelodyRules.arpFailures(MelodyMetrics.arp(clip), style.bpm))
        }
        return out
    }

    /** Everything judged for every style and mood. */
    fun all(withSong: Boolean = true): List<Judged> = StarterWizard.STYLES.flatMap { style ->
        Mood.entries.flatMap { mood ->
            listOfNotNull(
                judgeLead(style, mood, withSong).takeIf { hasLeadLine(style) },
                judgeBass(style, mood),
                judgeCounter(style, mood).takeIf { withSong },
            ) + judgeArps(style, mood)
        }
    }

    /** The rhythm cell of a line's first bar: its onsets and lengths in steps (the "tell" when styles share it). */
    fun rhythmCell(clip: NoteClip): List<Pair<Int, Int>> = clip.notes
        .filter { MelodyMetrics.grid(it.start) < MelodyMetrics.BAR }
        .map { MelodyMetrics.grid(it.start) / MelodyMetrics.T to (it.length.toFloat() / MelodyMetrics.T).let { l -> Math.round(l).coerceAtLeast(1) } }
        .distinct().sortedBy { it.first }

    /** How many bars of [clip] are exactly the same as its most repeated bar. */
    fun mostRepeatedBar(clip: NoteClip): Int {
        val bars = clip.steps / 16
        val sigs = (0 until bars).map { b ->
            clip.notes.filter { MelodyMetrics.grid(it.start) / MelodyMetrics.BAR == b }
                .map { listOf(MelodyMetrics.grid(it.start) % MelodyMetrics.BAR, it.length, it.pitch) }.sortedBy { it[0] }
        }.filter { it.isNotEmpty() }
        return sigs.groupBy { it }.values.maxOfOrNull { it.size } ?: 0
    }
}
