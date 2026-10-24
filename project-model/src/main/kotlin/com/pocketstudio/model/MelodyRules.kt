package com.pocketstudio.model

import com.pocketstudio.model.StarterWizard.LeadKind

/**
 * The thresholds the assistant's melodies must meet (docs/hits/autres-et-melodie.md § B13, one
 * table per kind of line) and the judgement of a [MelodyMetrics.Report] against them. A hook is
 * held to every rule; a riff may leap, repeat and stay on one note; vocal chops are sparse; a
 * bass is judged on its form, rests and life, not on its contour; an arpeggio on M17 alone.
 */
object MelodyRules {
    data class Thresholds(
        /** M1: lengths allowed, in steps. */
        val steps: Set<Int>,
        /** M2: motif occurrences over 16 bars. */
        val motif: IntRange?,
        /** M3. */
        val identicalPairsMax: Int,
        val identicalShareMax: Float,
        /** M4: rests of a beat per phrase; a two-beat rest at each phrase's end (or at the last one's only). */
        val restsPerPhraseMin: Int,
        val phraseEndRest: Boolean,
        val phraseEndRestEvery: Boolean = true,
        /** M5: phrase ends on a held chord tone. */
        val phraseEnd: Boolean,
        /** M6: beats per note (null: not judged). */
        val density: ClosedFloatingPointRange<Float>?,
        /** M7: the top note sounds at most this often; it first sounds from bar 3 on a strong beat. */
        val peakMax: Int?,
        val peakLate: Boolean,
        /** M8 (null: not judged). */
        val conjunct: ClosedFloatingPointRange<Float>?,
        val leapsPerPhraseMax: Int?,
        val leapsResolved: Boolean,
        /** M9. */
        val chordTones: ClosedFloatingPointRange<Float>,
        val strongChordTonesMin: Float,
        /** M10. */
        val tensions: Boolean,
        /** M11: range; the middle note (null: not judged). */
        val ambitus: IntRange,
        val median: IntRange?,
        /** M12. */
        val velocityRangeMin: Float,
        val modalShareMax: Float,
        val strongWeakMin: Float,
        val crescendoMin: Float?,
        /** M13: at least 30 % of the off-beat-one notes nudged, by 3 ticks at most, the same way per position. */
        val humanize: Boolean,
        /** M14. */
        val distinctLengthsMin: Int,
        val commonLengthShareMax: Float,
        /** M15: notes off the beat per 8 bars. */
        val syncopatedMin: Int,
        /** M18. */
        val voiceLeading: Boolean,
        val transposedBarsMax: Int?,
        val distinctPitchesMin: Int,
    )

    /** The singable hook: every rule of B13. */
    val HOOK = Thresholds(
        steps = setOf(128), motif = 3..6, identicalPairsMax = 1, identicalShareMax = 0.5f,
        restsPerPhraseMin = 1, phraseEndRest = true, phraseEnd = true, density = 0.35f..2.5f,
        peakMax = 2, peakLate = true, conjunct = 0.5f..0.95f, leapsPerPhraseMax = 1, leapsResolved = true,
        chordTones = 0.55f..1f, strongChordTonesMin = 0.8f, tensions = true, ambitus = 7..17, median = 52..88,
        velocityRangeMin = 0.25f, modalShareMax = 0.6f, strongWeakMin = 0.08f, crescendoMin = 0.05f, humanize = true,
        distinctLengthsMin = 3, commonLengthShareMax = 0.7f, syncopatedMin = 1, voiceLeading = true, transposedBarsMax = 2,
        distinctPitchesMin = 3,
    )

    /** A riff (techno, minimal, acid, grime): repetition and leaps are its nature, one note is allowed. */
    val RIFF = HOOK.copy(
        motif = 3..16, identicalShareMax = 0.6f, density = 0.25f..4f, peakMax = null, peakLate = false,
        conjunct = null, leapsPerPhraseMax = null, leapsResolved = false, chordTones = 0.6f..1f, ambitus = 0..24, median = 40..88,
        distinctLengthsMin = 2, commonLengthShareMax = 0.92f, syncopatedMin = 0, voiceLeading = false, transposedBarsMax = null,
        distinctPitchesMin = 1,
    )

    /** Vocal chops: one call, one answer, long echoes. */
    val CHOPS = RIFF.copy(density = 0.25f..6f, chordTones = 0.5f..1f, ambitus = 0..19, distinctLengthsMin = 1, commonLengthShareMax = 1f)

    /** A bass line: 4 or 8 bars, breathing at the phrase ends, roots on the strong beats, alive in velocity and length. */
    val BASS = Thresholds(
        steps = setOf(64, 128), motif = null, identicalPairsMax = 3, identicalShareMax = 0.75f,
        restsPerPhraseMin = 1, phraseEndRest = true, phraseEndRestEvery = false, phraseEnd = false, density = null,
        peakMax = null, peakLate = false, conjunct = null, leapsPerPhraseMax = null, leapsResolved = false,
        chordTones = 0.6f..1f, strongChordTonesMin = 0.8f, tensions = false, ambitus = 0..24, median = null,
        velocityRangeMin = 0.2f, modalShareMax = 0.7f, strongWeakMin = 0f, crescendoMin = null, humanize = false,
        distinctLengthsMin = 2, commonLengthShareMax = 1f, syncopatedMin = 0, voiceLeading = false, transposedBarsMax = null,
        distinctPitchesMin = 2,
    )

    fun forLead(kind: LeadKind): Thresholds = when (kind) {
        LeadKind.Hook, LeadKind.Arp -> HOOK
        LeadKind.Riff -> RIFF
        LeadKind.Chops -> CHOPS
    }

    /** M17: an arpeggio's own rules: four pitches over an octave and a half, a fixed pattern (or two octaves), accents. */
    data class ArpThresholds(val distinctPitchesMin: Int = 4, val rangeMin: Int = 19, val accentMin: Float = 0.15f)

    val ARP = ArpThresholds()

    /** What [report] fails in [t], one line per rule, empty when it passes. */
    fun failures(report: MelodyMetrics.Report, t: Thresholds): List<String> {
        val out = ArrayList<String>()
        val r = report
        if (r.notes == 0) return listOf("no note")
        if (r.steps !in t.steps) out += "M1 steps ${r.steps} not in ${t.steps}"
        t.motif?.let { if (r.motifOccurrences !in it) out += "M2 motif ${r.motifOccurrences} not in $it" }
        if (r.identicalPairs > t.identicalPairsMax) out += "M3 identical pairs ${r.identicalPairs} > ${t.identicalPairsMax}"
        if (r.identicalShare > t.identicalShareMax) out += "M3 identical share ${r.identicalShare.f()} > ${t.identicalShareMax}"
        if (r.restsPerPhrase.any { it < t.restsPerPhraseMin }) out += "M4 rests per phrase ${r.restsPerPhrase} < ${t.restsPerPhraseMin}"
        if (t.phraseEndRest && (if (t.phraseEndRestEvery) r.phraseEndRest.any { !it } else r.phraseEndRest.lastOrNull() == false)) out += "M4 phrase-end rest ${r.phraseEndRest}"
        if (t.phraseEnd && r.phraseEndOk.any { !it }) out += "M5 phrase end ${r.phraseEndOk}"
        t.density?.let { if (r.density !in it) out += "M6 density ${r.density.f()} not in $it" }
        t.peakMax?.let { if (r.peakCount > it) out += "M7 peak sounds ${r.peakCount} times > $it" }
        if (t.peakLate && (r.peakStart < 2 * MelodyMetrics.BAR || !r.peakStrong)) out += "M7 peak at ${r.peakStart} (strong ${r.peakStrong})"
        t.conjunct?.let { if (r.conjunct !in it) out += "M8 conjunct ${r.conjunct.f()} not in $it" }
        t.leapsPerPhraseMax?.let { if (r.leapsPerPhrase > it) out += "M8 leaps per phrase ${r.leapsPerPhrase} > $it" }
        if (t.leapsResolved && r.leapsResolved < 1f) out += "M8 leaps resolved ${r.leapsResolved.f()}"
        if (r.chordTones !in t.chordTones) out += "M9 chord tones ${r.chordTones.f()} not in ${t.chordTones}"
        if (r.strongChordTones < t.strongChordTonesMin) out += "M9 strong chord tones ${r.strongChordTones.f()} < ${t.strongChordTonesMin}"
        if (t.tensions && r.tensionsResolved < 1f) out += "M10 tensions resolved ${r.tensionsResolved.f()}"
        if (t.tensions && r.longClashes > 0) out += "M10 long clashes ${r.longClashes}"
        if (r.ambitus !in t.ambitus) out += "M11 ambitus ${r.ambitus} not in ${t.ambitus}"
        t.median?.let { if (r.median !in it) out += "M11 median ${r.median} not in $it" }
        if (r.velocityRange < t.velocityRangeMin) out += "M12 velocity range ${r.velocityRange.f()} < ${t.velocityRangeMin}"
        if (r.modalShare > t.modalShareMax) out += "M12 modal velocity share ${r.modalShare.f()} > ${t.modalShareMax}"
        if (r.strongWeak < t.strongWeakMin) out += "M12 strong − weak ${r.strongWeak.f()} < ${t.strongWeakMin}"
        t.crescendoMin?.let { if (r.crescendo < it) out += "M12 crescendo ${r.crescendo.f()} < $it" }
        if (t.humanize) {
            if (r.shiftedShare < 0.3f) out += "M13 shifted share ${r.shiftedShare.f()} < 0.3"
            if (r.maxShift > 3) out += "M13 max shift ${r.maxShift} > 3"
            if (!r.profileStable) out += "M13 shift profile unstable"
        } else if (r.maxShift > 3) out += "M13 max shift ${r.maxShift} > 3"
        if (r.distinctLengths < t.distinctLengthsMin) out += "M14 distinct lengths ${r.distinctLengths} < ${t.distinctLengthsMin}"
        if (r.commonLengthShare > t.commonLengthShareMax) out += "M14 common length share ${r.commonLengthShare.f()} > ${t.commonLengthShareMax}"
        if (r.syncopated < t.syncopatedMin) out += "M15 syncopated ${r.syncopated} < ${t.syncopatedMin}"
        if (t.voiceLeading && r.chordChanges > 0) {
            val needBoth = r.chordChanges >= 6
            if (needBoth && (r.commonTones < 1 || r.stepEntries < 1)) out += "M18 common tones ${r.commonTones}, step entries ${r.stepEntries}"
            if (!needBoth && r.commonTones + r.stepEntries < 1) out += "M18 no common tone nor step entry at a chord change"
        }
        t.transposedBarsMax?.let { if (r.transposedBars > it) out += "M18 transposed bars ${r.transposedBars} > $it" }
        if (r.distinctPitches < t.distinctPitchesMin) out += "M11 distinct pitches ${r.distinctPitches} < ${t.distinctPitchesMin}"
        return out
    }

    /** What an arpeggio fails in M17. */
    fun arpFailures(arp: MelodyMetrics.Arp, bpm: Double, t: ArpThresholds = ARP): List<String> {
        val out = ArrayList<String>()
        if (arp.distinctPitches < t.distinctPitchesMin) out += "M17 pitches ${arp.distinctPitches} < ${t.distinctPitchesMin}"
        if (arp.range < t.rangeMin) out += "M17 range ${arp.range} < ${t.rangeMin}"
        if (!arp.fixedPattern && arp.range < 24) out += "M17 no fixed pattern (mode ${arp.mode}) under two octaves"
        if (arp.mode == ArpMode.Random) out += "M17 random"
        val wanted = if (bpm >= 160) 2 * NoteClip.TICKS_PER_STEP else NoteClip.TICKS_PER_STEP
        if (arp.rate != wanted) out += "M17 rate ${arp.rate} (wanted $wanted at $bpm BPM)"
        if (arp.accent < t.accentMin) out += "M17 accent ${arp.accent.f()} < ${t.accentMin}"
        return out
    }

    private fun Float.f() = "%.2f".format(java.util.Locale.ROOT, this)
}
