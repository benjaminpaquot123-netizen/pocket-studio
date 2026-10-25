package com.pocketstudio.model.guide

import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.AutomationLane
import com.pocketstudio.model.Buses
import com.pocketstudio.model.ChordTrack
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Insert
import com.pocketstudio.model.LoudnessTarget
import com.pocketstudio.model.LoudnessTargets
import com.pocketstudio.model.MelodyMetrics
import com.pocketstudio.model.MelodyRules
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.Project
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.sceneSteps
import com.pocketstudio.model.songBars
import com.pocketstudio.model.track
import kotlin.math.abs
import kotlin.math.roundToInt

/** How much a fact matters: [ALERT] comes before every card (a mistake to fix), [WARN] a card will deal with it, [INFO] just so. */
enum class GuideSeverity { INFO, WARN, ALERT }

/**
 * Something the analysis read in the project (« la nappe joue pendant tout le Drop », « 6 pistes
 * sans pan »): [id] (« pad.everywhere »), the tracks it is about ([GuideTrack.key]), its numbers
 * ([values]) and words ([words]: section names, effect names, rule codes), and [text], the sentence
 * in the interface's language.
 */
data class GuideFact(
    val id: String,
    val severity: GuideSeverity,
    val text: String,
    val tracks: List<String> = emptyList(),
    val values: List<Float> = emptyList(),
    val words: List<String> = emptyList(),
)

/** A card of the plan as it stands: whether it applies to this song, its status, and whether the analysis ticked it. */
data class GuideCardStatus(
    val card: GuideCard,
    val applicable: Boolean,
    val status: GuideStatus,
    /** Ticked by the analysis (done in the editor, or by the assistant), not by a decision. */
    val auto: Boolean,
    val alert: Boolean = false,
) {
    val settled: Boolean get() = status.settled
}

/** What the Guide knows of a song at one moment: its facts, its plan, the next card, « où on en est », the track sheets. */
data class GuideReport(
    val parcours: GuideParcours,
    val facts: List<GuideFact>,
    /** The serious alerts' cards, first in line. */
    val alerts: List<GuideCardStatus>,
    /** Every card of the parcours (the Mode simple's only, in it), in order, with its status. */
    val cards: List<GuideCardStatus>,
    val next: GuideCardStatus?,
    val done: Int,
    val applicable: Int,
    /** « Jump up DnB · 4:14 · 8 pistes · étape 12/34 : la basse au Drop 2 ». */
    val header: String,
    /** « Où on en est » in three lines: where it was, what is done, what remains. */
    val summary: List<String>,
    val sheets: List<GuideSheet>,
    val simple: Boolean,
) {
    val percent: Int get() = if (applicable == 0) 0 else (100f * done / applicable).roundToInt()

    /** The cards still to decide (applicable, not settled). */
    val toDecide: Int get() = cards.count { it.applicable && !it.settled }

    fun status(id: String): GuideCardStatus? = (alerts + cards).firstOrNull { it.card.id == id }

    fun fact(id: String): GuideFact? = facts.firstOrNull { it.id == id }
}

/**
 * The Guide's brain (docs/PLAN-GUIDE.md §4.2): reads a project (never the audio thread) and
 * produces its facts, ticks the cards they say are done, picks the next card (a serious alert
 * first, then the first card not settled whose conditions hold, in the parcours' order, the cards
 * put off « plus tard » last) and says where the song stands. Deterministic: the same project and
 * state give the same report.
 */
object GuideAnalyzer {
    /** The report on [project] (its [Project.guide] state), the Mode simple's cards only when [simple]; [now]: for « te revoilà ». */
    fun analyze(project: Project, simple: Boolean = false, now: Long = 0L, parcours: GuideParcours = GuideLibrary.forProject(project)): GuideReport {
        val facts = GuideFacts.of(project)
        val state = project.guide
        val alerts = facts.list.filter { it.severity == GuideSeverity.ALERT }.mapNotNull { f ->
            val base = GuideLibrary.alert(f.id) ?: return@mapNotNull null
            val role = f.tracks.firstOrNull()?.let { k -> facts.trackOf(k)?.role } ?: base.target
            val c = base.copy(target = role)
            if (state.refused(c.id)) return@mapNotNull null
            // An alert is never settled while its fact holds: only « plus tard » is kept (or « c'est voulu », above).
            val st = if (state.statusOf(c.id) == GuideStatus.LATER) GuideStatus.LATER else GuideStatus.TODO
            GuideCardStatus(c, true, st, false, alert = true)
        }
        val cards = parcours.cardsFor(simple).map { c -> statusOf(c, facts, state) }
        val applicable = cards.filter { it.applicable }
        val next = alerts.firstOrNull { it.status != GuideStatus.LATER }
            ?: applicable.firstOrNull { !it.settled && it.status != GuideStatus.LATER }
            ?: alerts.firstOrNull()
            ?: applicable.firstOrNull { !it.settled }
        val done = applicable.count { it.status.done }
        val header = header(project, facts, applicable, next, simple)
        val summary = summary(project, state, applicable, next, simple, now)
        return GuideReport(parcours, facts.list, alerts, cards, next, done, applicable.size, header, summary, GuideSheets.of(project, facts, parcours, simple), simple)
    }

    private fun statusOf(c: GuideCard, facts: GuideFacts, state: GuideState): GuideCardStatus {
        val applicable = c.applicable.all { facts.holds(it, c.target) }
        val stored = state.statusOf(c.id)
        // Refused, or every option refused: nothing left to offer.
        val allRefused = c.proposals.isNotEmpty() && c.proposals.all { state.refused(c.id, it.id) }
        if (state.refused(c.id) || allRefused) return GuideCardStatus(c, applicable, GuideStatus.SKIPPED, false)
        if (stored == GuideStatus.DONE || stored == GuideStatus.SKIPPED) return GuideCardStatus(c, applicable, stored, false)
        val ticked = applicable && c.done.isNotEmpty() && c.done.all { facts.holds(it, c.target) }
        return if (ticked) GuideCardStatus(c, applicable, GuideStatus.DONE_ELSEWHERE, true)
        else GuideCardStatus(c, applicable, if (stored == GuideStatus.DONE_ELSEWHERE) GuideStatus.TODO else stored, false)
    }

    /** « m:ss » of [p]'s song (its scene when it has none). */
    fun duration(p: Project): String {
        val bars = if (p.song.isEmpty()) p.sceneSteps(p.scene) / p.stepsPerBar.coerceAtLeast(1) else p.songBars
        val seconds = (bars * p.beatsPerBar * 60.0 / p.tempoBpm).roundToInt()
        return "%d:%02d".format(seconds / 60, seconds % 60)
    }

    private fun header(p: Project, facts: GuideFacts, applicable: List<GuideCardStatus>, next: GuideCardStatus?, simple: Boolean): String {
        val tracks = facts.tracks.count { !it.key.startsWith("pad:") }
        if (next == null) return tr("%1\$s · %2\$s · %3\$d pistes · tout est fait", p.name, duration(p), tracks)
        val index = applicable.indexOfFirst { it.card.id == next.card.id }
        return if (index < 0) tr("%1\$s · %2\$s · %3\$d pistes · à corriger : %4\$s", p.name, duration(p), tracks, next.card.titleFor(simple))
        else tr("%1\$s · %2\$s · %3\$d pistes · étape %4\$d/%5\$d : %6\$s", p.name, duration(p), tracks, index + 1, applicable.size, next.card.titleFor(simple))
    }

    /** The three lines that welcome every reopening (§3.6). */
    private fun summary(p: Project, state: GuideState, applicable: List<GuideCardStatus>, next: GuideCardStatus?, simple: Boolean, now: Long): List<String> {
        val at = applicable.firstOrNull { it.card.id == state.step && !it.settled } ?: next
        val back = state.lastUsed > 0 && now - state.lastUsed >= DAY_MS
        val first = when {
            at == null -> tr("Tout est fait : ton morceau est prêt à partager.")
            back && state.step.isNotEmpty() -> tr("Te revoilà ! Tu en étais à : %s.", at.card.titleFor(simple))
            state.step.isNotEmpty() -> tr("Tu en étais à : %s.", at.card.titleFor(simple))
            else -> tr("On commence par : %s.", at.card.titleFor(simple))
        }
        val stages = applicable.groupBy { it.card.stage }.toSortedMap()
        val finished = stages.filter { (_, cs) -> cs.all { it.settled } }.keys.map(GuideStages::short)
        val remaining = stages.filter { (_, cs) -> cs.any { !it.settled } }.keys.map(GuideStages::short)
        val done = if (finished.isEmpty()) tr("Fait : rien encore, tout est devant toi.") else tr("Fait : %s.", finished.joinToString(", "))
        val rest = if (remaining.isEmpty()) tr("Reste : rien, bravo.") else tr("Reste : %s. On reprend ?", remaining.joinToString(", "))
        return listOf(first, done, rest)
    }

    const val DAY_MS = 24L * 3600 * 1000
}

/**
 * The facts of a project and the detectors the cards ask about ([holds]). Built once per
 * analysis; [list] in a fixed order (the alerts first).
 */
class GuideFacts private constructor(val project: Project, val song: GuideSong) {
    val tracks: List<GuideTrack> get() = song.tracks
    private val byRole: Map<String, List<GuideTrack>> = song.tracks.groupBy { it.role }
    private val predicates = HashMap<String, Boolean>()
    val list = ArrayList<GuideFact>()

    fun trackOf(key: String): GuideTrack? = song.tracks.firstOrNull { it.key == key }

    /** The tracks of [role] that play in the song. */
    fun playing(role: String): List<GuideTrack> = byRole[role].orEmpty().filter { song.plays(it.key) }

    /**
     * Detector [pred] for a card about [target]: "name" or "name:role" (none: the card's target),
     * "!" in front negates. Unknown names never hold (a newer parcours read by an older app).
     */
    fun holds(pred: String, target: String): Boolean {
        if ('|' in pred) return pred.split('|').any { holds(it, target) }
        if (pred.startsWith("!")) return !holds(pred.substring(1), target)
        val name = pred.substringBefore(':')
        val role = pred.substringAfter(':', target)
        return when (name) {
            "has" -> playing(role).isNotEmpty()
            "fx" -> if (role == "any") anyFx else playing(role).let { ts -> ts.isNotEmpty() && ts.all { hasFx(it) } }
            "changed" -> changed(role)
            "layered" -> layered(role)
            "peaks" -> song.sectionsOf(GuideSection.Kind.PEAK).size >= (role.toIntOrNull() ?: 1)
            else -> predicates[name] ?: false
        }
    }

    // --- the strips --------------------------------------------------------------------------------

    private fun inserts(t: GuideTrack): List<Insert> = if (t.channel == Mixer.MASTER) project.mixer.master else project.mixer.channels.getOrNull(t.channel)?.inserts.orEmpty()

    private fun live(t: GuideTrack): List<Insert> = inserts(t).filter { it.effect != EffectType.NONE && !it.bypass }

    private fun hasFx(t: GuideTrack): Boolean = live(t).isNotEmpty()

    private val anyFx: Boolean get() = song.tracks.any { hasFx(it) } || project.mixer.master.any { it.effect != EffectType.NONE }

    /** A low cut on [t]: a high-pass filter, or the parametric EQ's low cut. */
    fun hasLowCut(t: GuideTrack): Boolean = live(t).any(::isLowCut)

    private fun hasSpace(t: GuideTrack): Boolean =
        project.mixer.channels.getOrNull(t.channel)?.sends?.any { it > 0.01f } == true ||
            live(t).any { it.effect == EffectType.REVERB || it.effect == EffectType.ECHO || it.effect == EffectType.PING_PONG }

    private fun panOf(t: GuideTrack): Float =
        if (t.key.startsWith("pad:")) project.drums.lanes.getOrNull(t.key.removePrefix("pad:").toInt())?.pan ?: 0f
        else project.mixer.channels.getOrNull(t.channel)?.pan ?: 0f

    /** The tracks the mix acts on one by one: the instruments, the 303, the audio tracks. */
    private val melodic: List<GuideTrack> get() = song.tracks.filter { it.key.startsWith("inst:") || it.key == GuideTrack.ACID || it.key.startsWith("audio:") }.filter { song.plays(it.key) }

    // --- detectors with a role ------------------------------------------------------------------------

    private fun changed(role: String): Boolean {
        val seen = project.guide.seen
        if (seen.isEmpty()) return false
        return when (role) {
            "sidechain" -> seen["sidechain"]?.let { it != GuideSignatures.sidechain(project) } ?: false
            GuideRoles.MASTER -> seen[GuideRoles.MASTER]?.let { it != GuideSignatures.master(project) } ?: false
            "lanes" -> seen["lanes"]?.let { it != GuideSignatures.lanes(project) } ?: false
            "pans" -> seen["pans"]?.let { it != GuideSignatures.pans(project) } ?: false
            GuideRoles.SENDS -> seen[GuideRoles.SENDS]?.let { it != GuideSignatures.sends(project) } ?: false
            else -> playing(role).any { t -> seen[t.key]?.let { it != GuideSignatures.chain(project, t) } ?: false }
        }
    }

    private fun layered(role: String): Boolean = when (role) {
        // A sub track, or a bass that stacks layers (G3: its sheet's « Couches »).
        GuideRoles.BASS -> playing(GuideRoles.SUB).isNotEmpty() ||
            playing(GuideRoles.BASS).any { t -> t.key.removePrefix("inst:").toIntOrNull()?.let { project.track(it).layers.isNotEmpty() } == true }
        "808" -> playing(GuideRoles.KICK).flatMap { k -> playing(GuideRoles.BASS).map { k to it } }.any { (k, b) -> (0 until song.bars).any { song.sounds(k.key, it) && song.sounds(b.key, it) } }
        GuideRoles.SNARE -> project.allScenes.any { s -> s.drums.lanes.getOrNull(StarterWizard.Role.Snare.lane)?.hits?.isNotEmpty() == true } &&
            project.allScenes.any { s -> s.drums.lanes.getOrNull(StarterWizard.Role.Clap.lane)?.hits?.isNotEmpty() == true }
        else -> false
    }

    // --- building ----------------------------------------------------------------------------------

    private fun fact(id: String, severity: GuideSeverity, text: String, tracks: List<String> = emptyList(), values: List<Float> = emptyList(), words: List<String> = emptyList()) {
        list += GuideFact(id, severity, text, tracks, values, words)
    }

    private fun names(ts: List<GuideTrack>): String = ts.joinToString(", ") { com.pocketstudio.model.DefaultNames.shown(it.name) }

    private fun build() {
        val p = project
        val peaks = song.sectionsOf(GuideSection.Kind.PEAK)
        val chorus = peaks.firstOrNull()?.label == "Refrain"
        val peakWord = if (chorus) tr("refrain") else tr("Drop")
        val peakWords = if (chorus) tr("refrains") else tr("Drops")

        // The frame.
        predicates["style"] = p.style != null
        predicates["by_hand"] = p.style == null
        predicates["key"] = p.harmony.lock || p.style != null
        predicates["form"] = song.sections.size >= 3 && song.sections.all { it.bars % 4 == 0 || it.bars <= 1 }
        predicates["target"] = p.mixer.target != LoudnessTarget.OFF
        predicates["build"] = song.sectionsOf(GuideSection.Kind.BUILD).isNotEmpty()
        predicates["break"] = song.sectionsOf(GuideSection.Kind.BREAK).isNotEmpty()

        // Alerts first: what is wrong, not just unfinished.
        val songBars = p.songBars
        val long = p.automation.filter { l ->
            l.mode == AutomationLane.SONG && p.song.isNotEmpty() && l.songValues.indexOfLast { it != null } + 1 > (songBars + 4) * l.pointsPerBar(p.beatsPerBar)
        }
        if (long.isNotEmpty()) {
            val bars = long.maxOf { (it.songValues.indexOfLast { v -> v != null } + 1) / it.pointsPerBar(p.beatsPerBar) }
            fact("curves.too_long", GuideSeverity.ALERT, tr("Des courbes durent %1\$d mesures pour un morceau de %2\$d : elles sont sans doute décalées.", bars, songBars),
                long.mapNotNull { l -> song.tracks.firstOrNull { it.channel == l.target.ch }?.key }.distinct(), listOf(bars.toFloat(), songBars.toFloat()))
        }
        val offCentre = song.tracks.filter { it.role in setOf(GuideRoles.KICK, GuideRoles.BASS, GuideRoles.SUB) && song.plays(it.key) && abs(panOf(it)) > 0.25f }
        if (offCentre.isNotEmpty()) {
            fact("low.off_center", GuideSeverity.ALERT, tr("Pas au centre : %s. Le grave doit rester au milieu.", names(offCentre)), offCentre.map { it.key }, offCentre.map { panOf(it) })
        }
        val solos = song.tracks.filter { p.mixer.channels.getOrNull(it.channel)?.solo == true }
        if (solos.isNotEmpty()) fact("mix.solo", GuideSeverity.ALERT, tr("Un solo est resté allumé : %s. Les autres pistes ne s'entendent plus.", names(solos)), solos.map { it.key })
        p.guide.measures?.truePeakDb?.let { tp ->
            if (tp > -0.1f) fact("loudness.peak", GuideSeverity.ALERT, tr("Le morceau sature : crête à %s dBTP.", LoudnessTargets.format(tp.toDouble())), values = listOf(tp))
        }

        // Tracks.
        val empty = instruments.filter { t -> !song.plays(t.key) && p.allScenes.none { s -> notesOf(s, t)?.notes?.isNotEmpty() == true } }
        if (empty.isNotEmpty()) fact("track.empty", GuideSeverity.WARN, tr("Pistes vides : %s.", names(empty)), empty.map { it.key })
        val bare = melodic.filter { it.key.startsWith("inst:") && !hasFx(it) }
        if (bare.isNotEmpty()) fact("track.no_inserts", GuideSeverity.WARN, tr("Sans aucun effet : %s.", names(bare)), bare.map { it.key })
        val needCut = melodic.filter { it.role !in GuideRoles.LOW }
        val noCut = needCut.filter { !hasLowCut(it) }
        predicates["lowcut"] = noCut.isEmpty()
        if (noCut.isNotEmpty()) fact("track.no_lowcut", GuideSeverity.WARN, tr("Sans coupe-bas : %s.", names(noCut)), noCut.map { it.key })
        val dry = melodic.filter { it.role in setOf(GuideRoles.PAD, GuideRoles.LEAD, GuideRoles.COUNTER, GuideRoles.VOCAL, GuideRoles.ACID) && !hasSpace(it) }
        if (dry.isNotEmpty()) fact("track.no_sends", GuideSeverity.WARN, tr("Sans réverbe ni écho : %s.", names(dry)), dry.map { it.key })
        val placed = melodic.filter { it.role !in GuideRoles.LOW && it.role != GuideRoles.VOCAL && it.role != GuideRoles.RISER }
        predicates["pans"] = placed.any { abs(panOf(it)) > 0.01f }
        if (placed.isNotEmpty() && placed.all { abs(panOf(it)) <= 0.01f }) {
            fact("mix.no_pans", GuideSeverity.WARN, tr("%d pistes, toutes au centre : rien n'est placé dans la stéréo.", placed.size), placed.map { it.key }, listOf(placed.size.toFloat()))
        }
        val faders = (melodic + song.tracks.filter { it.key == GuideTrack.DRUMS }).mapNotNull { p.mixer.channels.getOrNull(it.channel)?.volume }
        predicates["faders"] = faders.any { abs(it - 0.8f) > 0.005f }
        if (faders.isNotEmpty() && faders.all { abs(it - 0.8f) <= 0.005f }) fact("mix.faders_flat", GuideSeverity.WARN, tr("Tous les faders sont à 0 dB : la balance n'est pas encore faite."))
        val busy = song.tracks.filter { t -> moving(t.channel).size > 2 }
        if (busy.isNotEmpty()) fact("curves.too_many", GuideSeverity.WARN, tr("Plus de deux courbes sur : %s. Le mix bouge peut-être trop.", names(busy)), busy.map { it.key })

        // The sends and the sidechain.
        val b = p.mixer.buses
        predicates["returns"] = (0 until Buses.COUNT).all { i -> b.getOrNull(i)?.let { it.getOrElse(Buses.LOW_CUT) { 0f } > 0.01f && it.getOrElse(Buses.HIGH_CUT) { 1f } < 0.99f } == true }
        val bassStrips = (playing(GuideRoles.BASS) + playing(GuideRoles.SUB) + playing(GuideRoles.BASS2)).map { it.channel }.toSet()
        val bus = p.mixer.musicBus
        predicates["sidechain"] = (p.mixer.sidechainKey >= 0 && bus != null && (bus.depth > 0.05f || bus.lowDepth > 0.05f) && bus.members.any { it in bassStrips }) ||
            bassStrips.any { ch -> p.mixer.channels.getOrNull(ch)?.inserts?.any { it.effect == EffectType.PUMP && !it.bypass } == true }

        // The arrangement.
        arrangement(peaks, peakWord, peakWords)

        // The movement.
        movement(peaks)

        // The master.
        val lufs = LoudnessTargets.lufs(p.mixer.target, p.style, p.mixVersion)
        predicates["master"] = p.mixer.target != LoudnessTarget.OFF && p.mixer.masterDb <= 0.5f && p.mixer.master.any { it.effect != EffectType.NONE }
        fact(
            "master.headroom", if (p.mixer.target == LoudnessTarget.OFF && p.mixer.masterDb > 0f) GuideSeverity.WARN else GuideSeverity.INFO,
            if (lufs != null) tr("Master à %1\$s dB, cible %2\$s LUFS.", LoudnessTargets.format(p.mixer.masterDb.toDouble()), LoudnessTargets.formatTarget(lufs))
            else tr("Master à %s dB, sans cible de volume.", LoudnessTargets.format(p.mixer.masterDb.toDouble())),
            listOf(GuideTrack.MASTER), listOfNotNull(p.mixer.masterDb, lufs),
        )
        val m = p.guide.measures
        predicates["measured"] = m != null
        val peakLufs = m?.sections?.get(GuideSection.Kind.PEAK.name)
        val breakLufs = m?.sections?.get(GuideSection.Kind.BREAK.name)
        predicates["contrast"] = peakLufs != null && breakLufs != null && peakLufs - breakLufs >= CONTRAST_DB
        if (peakLufs != null && breakLufs != null) {
            val d = peakLufs - breakLufs
            fact("loudness.contrast", if (d < CONTRAST_DB) GuideSeverity.WARN else GuideSeverity.INFO,
                tr("%1\$s − Break : %2\$s dB (4 au moins).", peakWord, LoudnessTargets.format(d.toDouble())), values = listOf(d))
        }

        // The melody and the chords, in the first Drop (or Refrain).
        melody(peaks)
    }

    private fun notesOf(s: com.pocketstudio.model.Scene, t: GuideTrack): NoteClip? {
        val id = t.key.removePrefix("inst:").toIntOrNull() ?: return null
        return if (id == 0) s.synthNotes else s.trackNotes[id]
    }

    private val instruments: List<GuideTrack> get() = song.tracks.filter { it.key.startsWith("inst:") }

    private fun arrangement(peaks: List<GuideSection>, peakWord: String, peakWords: String) {
        val p = project
        // Peaks compared bar by bar as written, whatever track plays the notes (a bass switched to another
        // sound plays the same music), the drums left out where either plays a fill or a crash.
        fun notes(s: GuideSection, i: Int, heard: Boolean): List<Int> = song.tracks.filter { !it.key.startsWith("pad:") && it.key != GuideTrack.DRUMS }
            .map { if (heard) song.at(it.key, s.start + i) else song.writtenAt(it.key, s.start + i) }.filter { it != 0 }.sorted()
        fun voices(s: GuideSection): List<List<Pair<String, Int>>> = (s.start until s.end).map { bar ->
            song.tracks.filter { it.key.startsWith("inst:") || it.key == GuideTrack.ACID }.map { it.key to song.at(it.key, bar) }.filter { it.second != 0 }
        }
        fun same(a: GuideSection, b: GuideSection): Boolean = a.bars == b.bars && (0 until a.bars).all { i ->
            notes(a, i, false) == notes(b, i, false) &&
                (song.fillBars[a.start + i] || song.fillBars[b.start + i] || song.writtenAt(GuideTrack.DRUMS, a.start + i) == song.writtenAt(GuideTrack.DRUMS, b.start + i))
        }
        if (peaks.size >= 2) {
            val identical = peaks.indices.filter { i -> peaks.indices.any { j -> j != i && same(peaks[i], peaks[j]) } }
            predicates["peaks_differ"] = identical.isEmpty()
            if (identical.isNotEmpty()) {
                val mixOnly = identical.map { voices(peaks[it]) }.distinct().size > 1
                fact("peaks.identical", GuideSeverity.WARN,
                    if (identical.size == peaks.size && mixOnly) tr("Les %1\$d %2\$s jouent les mêmes notes : seuls un son ou un volume changent.", peaks.size, peakWords)
                    else if (identical.size == peaks.size) tr("Les %1\$d %2\$s sont identiques.", peaks.size, peakWords)
                    else tr("%1\$d %2\$s sur %3\$d sont identiques.", identical.size, peakWords, peaks.size),
                    values = listOf(identical.size.toFloat(), peaks.size.toFloat()), words = identical.map { "${peaks[it].occurrence}" } + if (mixOnly) listOf("mix") else emptyList())
            }
        } else predicates["peaks_differ"] = peaks.size == 1

        // The bass: its loop in each peak.
        val basses = (playing(GuideRoles.BASS) + playing(GuideRoles.BASS2)).map { it.key }
        if (basses.isNotEmpty() && peaks.isNotEmpty()) {
            var varies = true
            var worst = 0 to 0
            for (s in peaks) {
                val seq = (s.start until s.end).map { bar -> basses.map { song.at(it, bar) }.filter { it != 0 }.sorted().hashCode() }
                val period = (1..seq.size).first { per -> (per until seq.size).all { seq[it] == seq[it - per] } }
                if (seq.size >= 16 && period <= seq.size / 2) varies = false
                val repeats = seq.size / period
                if (repeats > worst.second) worst = period to repeats
            }
            predicates["bass_varies"] = varies
            if (!varies) fact("bass.repeats", GuideSeverity.WARN, tr("La basse rejoue la même boucle de %1\$d mesures %2\$d fois de suite dans le %3\$s.", worst.first, worst.second, peakWord),
                basses, listOf(worst.first.toFloat(), worst.second.toFloat()))
        } else predicates["bass_varies"] = true

        // The pad in the peaks.
        val pads = playing(GuideRoles.PAD).map { it.key }
        if (pads.isNotEmpty() && peaks.isNotEmpty()) {
            val shares = peaks.map { s -> (s.start until s.end).count { bar -> pads.any { song.sounds(it, bar) } }.toFloat() / s.bars.coerceAtLeast(1) }
            predicates["pad_room"] = shares.any { it <= 0.75f }
            if (shares.all { it >= 0.99f }) fact("pad.everywhere", GuideSeverity.WARN, tr("Les accords jouent pendant tout le %1\$s (%2\$d sur %2\$d).", peakWord, peaks.size), pads, shares)
        } else predicates["pad_room"] = pads.isEmpty()

        // Subtraction: the other sections play fewer tracks than the peak.
        val full = peaks.maxOfOrNull { song.fullness(it) } ?: 0
        val lighter = song.sections.filter { it.kind in setOf(GuideSection.Kind.INTRO, GuideSection.Kind.BUILD, GuideSection.Kind.BREAK) }
        predicates["subtractive"] = peaks.isNotEmpty() && lighter.isNotEmpty() && lighter.all { song.fullness(it) < full }

        // A cut before each peak: the kick out of the bar before, a silence, a tape stop.
        val kicks = playing(GuideRoles.KICK).map { it.key }
        val volumeDips = p.automation.filter { it.mode == AutomationLane.SONG && ((it.target.kind == AutoTarget.VOLUME && it.target.ch == Mixer.MASTER) || (it.target.kind == AutoTarget.LIVE && it.target.param == AutoTarget.LIVE_TAPE_STOP)) }
        fun cutBefore(s: GuideSection): Boolean {
            val bar = s.start - 1
            if (bar < 0) return true
            if (kicks.isNotEmpty() && kicks.none { song.sounds(it, bar) }) return true
            return volumeDips.any { lane ->
                val per = lane.pointsPerBar(p.beatsPerBar)
                val curve = lane.songCurve(song.bars * per)
                val last = (0 until per).mapNotNull { curve.getOrNull(bar * per + it) }
                if (lane.target.kind == AutoTarget.LIVE) last.any { it > 0.5f } else last.any { it < 0.3f }
            }
        }
        predicates["cut"] = peaks.isNotEmpty() && peaks.filter { it.start > 0 }.all(::cutBefore)
    }

    /** The lanes of strip [ch] that move over the song (a song curve, a loop, a scene's own). */
    private fun moving(ch: Int): List<AutomationLane> = project.automation.filter { l ->
        l.target.ch == ch && l.target.kind != AutoTarget.BUS_PARAM && l.target.kind != AutoTarget.LIVE && rangeOf(l, 0, song.bars) > MOVE
    }

    /** How far lane [l] moves over bars [from] until [to] (the bar before included: a change on entering counts). */
    private fun rangeOf(l: AutomationLane, from: Int, to: Int): Float {
        return when (l.mode) {
            AutomationLane.SONG -> {
                if (song.bars == 0) return 0f
                val per = l.pointsPerBar(project.beatsPerBar)
                val curve = l.songCurve(song.bars * per)
                val vs = ((from - 1).coerceAtLeast(0) * per until (to * per).coerceAtMost(curve.size)).map { curve[it] }
                if (vs.isEmpty()) 0f else vs.max() - vs.min()
            }
            AutomationLane.PER_SCENE -> l.sceneValues.values.maxOfOrNull { v -> v.filterNotNull().let { if (it.isEmpty()) 0f else it.max() - it.min() } } ?: 0f
            else -> l.values.filterNotNull().let { if (it.isEmpty()) 0f else it.max() - it.min() }
        }
    }

    private fun movement(peaks: List<GuideSection>) {
        val lanes = project.automation.filter { it.target.kind != AutoTarget.BUS_PARAM && !(it.target.kind == AutoTarget.VOLUME && it.target.ch == Mixer.MASTER) }
        val builds = song.sectionsOf(GuideSection.Kind.BUILD)
        predicates["build_moves"] = builds.isNotEmpty() && builds.all { s -> lanes.any { l -> l.mode == AutomationLane.SONG && rangeOf(l, s.start + 1, s.end) >= 0.15f } }
        predicates["send_moves"] = project.automation.any { l ->
            val throws = l.target.kind == AutoTarget.SEND || (l.target.kind == AutoTarget.MACRO && l.target.param >= AutoTarget.MACRO_THROW_REVERB) ||
                (l.target.kind == AutoTarget.LIVE && l.target.param in setOf(AutoTarget.LIVE_THROW_REVERB, AutoTarget.LIVE_THROW_ECHO))
            throws && rangeOf(l, 0, song.bars) > 0.1f
        }
        val kinds = song.sections.map { it.kind }.distinct()
        val still = kinds.filter { k -> song.sections.filter { it.kind == k }.none { s -> lanes.any { rangeOf(it, s.start, s.end) > MOVE } } }
        predicates["sections_move"] = song.sections.isNotEmpty() && still.isEmpty()
        if (still.isNotEmpty() && song.sections.isNotEmpty()) {
            val labels = still.map { k -> song.sections.first { it.kind == k }.label }
            fact("automation.missing", GuideSeverity.WARN, tr("Rien ne bouge dans : %s.", labels.joinToString(", ") { com.pocketstudio.model.Scene.shownLabel(it) }), words = labels)
        }
        val pads = playing(GuideRoles.PAD)
        predicates["pad_moves"] = pads.isNotEmpty() && pads.any { moving(it.channel).isNotEmpty() }
    }

    private fun melody(peaks: List<GuideSection>) {
        val p = project
        val scene = peaks.firstOrNull()?.scene ?: if (p.song.isEmpty()) p.scene else return
        val s = p.allScenes.getOrNull(scene) ?: return
        val degrees = ChordTrack.guess(p, scene)
        fact("chords.guess", GuideSeverity.INFO, tr("Accords du %1\$s : %2\$s.", if (peaks.firstOrNull()?.label == "Refrain") tr("refrain") else tr("Drop"),
            degrees.joinToString(" – ") { ChordTrack.roman(p, it) }), values = degrees.map { it.toFloat() })
        val style = p.style?.let(StarterWizard::style)
        val ctx = MelodyMetrics.Context(p.harmony, ChordTrack.chordsOf(p, scene).ifEmpty { listOf(0) })
        for ((role, rules) in listOf(
            GuideRoles.LEAD to MelodyRules.forLead(style?.melody?.leadKind ?: StarterWizard.LeadKind.Hook),
            GuideRoles.BASS to MelodyRules.BASS,
            GuideRoles.COUNTER to MelodyRules.RIFF,
        )) {
            val t = playing(role).firstOrNull { it.key.startsWith("inst:") } ?: continue
            val clip = notesOf(s, t)?.takeIf { it.notes.isNotEmpty() } ?: continue
            val report = MelodyMetrics.analyse(clip, ctx)
            val fails = MelodyRules.failures(report, rules).map { it.substringBefore(' ') }.distinct()
            fact(
                "melody.$role", if (fails.isEmpty()) GuideSeverity.INFO else GuideSeverity.WARN,
                if (fails.isEmpty()) tr("%s : la ligne passe les 20 règles de mélodie.", com.pocketstudio.model.DefaultNames.shown(t.name))
                else tr("%1\$s : %2\$d règle(s) de mélodie sur 20 à revoir (%3\$s).", com.pocketstudio.model.DefaultNames.shown(t.name), fails.size, fails.joinToString(", ")),
                listOf(t.key), listOf(report.motifOccurrences.toFloat(), report.identicalShare, report.density, report.ambitus.toFloat()), fails,
            )
        }
    }

    companion object {
        /** Drop − Break at least this (LU). */
        const val CONTRAST_DB = 4f
        /** Under this a lane does not move. */
        const val MOVE = 0.05f

        /** The detectors a parcours may ask about ([holds]): plain names, and those taking a role ("has:bass"). */
        val DETECTORS = setOf(
            "style", "by_hand", "key", "form", "target", "build", "break", "lowcut", "pans", "faders", "returns", "sidechain", "peaks_differ",
            "bass_varies", "pad_room", "subtractive", "cut", "build_moves", "send_moves", "sections_move", "pad_moves", "master", "measured", "contrast",
        )
        val ROLE_DETECTORS = setOf("has", "fx", "changed", "layered", "peaks")

        fun of(project: Project): GuideFacts = GuideFacts(project, GuideSong(project)).also { it.build() }

        /** [i] cuts the lows: a high-pass filter, or the parametric EQ's low cut. */
        fun isLowCut(i: Insert): Boolean =
            (i.effect == EffectType.FILTER && i.params.getOrElse(2) { 0f } >= 2f / 3f) || (i.effect == EffectType.PARAM_EQ && i.params.getOrElse(3) { 0f } >= 0.01f)
    }
}

/** Signatures of what a card about effects watches, to tell a change made in the mixer ([GuideState.seen]). */
object GuideSignatures {
    fun chain(p: Project, t: GuideTrack): Int {
        val c = p.mixer.channels.getOrNull(t.channel) ?: return 0
        val inserts = if (t.channel == Mixer.MASTER) p.mixer.master else c.inserts
        return 31 * inserts.hashCode() + if (t.channel == Mixer.MASTER) 0 else c.sends.hashCode()
    }

    fun sidechain(p: Project): Int = 31 * p.mixer.sidechainKey + (p.mixer.musicBus?.hashCode() ?: 0)

    fun sends(p: Project): Int = p.mixer.buses.hashCode()

    /** Every hash here must be the same in every run of the app: no enum's hashCode (an identity), its ordinal. */
    fun master(p: Project): Int = 31 * (31 * p.mixer.master.hashCode() + p.mixer.target.ordinal) + p.mixer.masterDb.hashCode()

    fun lanes(p: Project): Int = p.automation.hashCode()

    /** Where every track sits in the stereo: the strips' pans, the pads' own (the project's [com.pocketstudio.model.DrumPattern], not a scene's copy). */
    fun pans(p: Project): Int = 31 * p.mixer.channels.map { it.pan }.hashCode() + p.drums.lanes.map { it.pan }.hashCode()

    /** Every signature of [p]: what [GuideState.seen] keeps when the Guide first looks at a song. */
    fun all(p: Project): Map<String, Int> =
        p.guideTracks().associate { it.key to chain(p, it) } +
            mapOf("sidechain" to sidechain(p), GuideRoles.SENDS to sends(p), GuideRoles.MASTER to master(p), "lanes" to lanes(p), "pans" to pans(p))
}
