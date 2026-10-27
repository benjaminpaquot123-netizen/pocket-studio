package com.pocketstudio.app.guide

import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectSession
import com.pocketstudio.model.guide.GuideActions
import com.pocketstudio.model.guide.GuideAnalyzer
import com.pocketstudio.model.guide.GuideCard
import com.pocketstudio.model.guide.GuideCardStatus
import com.pocketstudio.model.guide.GuideFact
import com.pocketstudio.model.guide.GuideOutcome
import com.pocketstudio.model.guide.GuideProposal
import com.pocketstudio.model.guide.GuideReport
import com.pocketstudio.model.guide.GuideSheet
import com.pocketstudio.model.guide.GuideStages
import com.pocketstudio.model.guide.GuideStatus
import com.pocketstudio.model.guide.guideText
import com.pocketstudio.model.guide.guideTracks
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.Buses
import com.pocketstudio.model.EffectType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** An option of a card as the panel shows it; [opens]: the screen it opens instead of changing the song (« Voir le réglage »). */
data class GuideOptionUi(
    val id: String, val label: String, val why: String, val value: String?, val opens: String?, val refused: Boolean,
    /** It changes the song. */
    val edits: Boolean = false,
    /** Its change can be heard with and without before it is applied (▶; not the master's target nor curves elsewhere: G2 review). */
    val heard: Boolean = edits,
    /** G4: a check to hear ("mono", "phone", "reference"): the option itself plays it. */
    val listen: String? = null,
)

/**
 * G4: one effect the assistant (or anyone) put on a track, as « Voilà ce que j'ai mis » lists it:
 * the track, the effect's name and its why, whether it is set aside ([off]), and the option that
 * retires it or puts it back ([option], heard first with ▶ like any other).
 */
data class GuideFxUi(val track: String, val trackName: String, val name: String, val why: String, val off: Boolean, val option: String)

/** A card as the panel shows it (texts in the interface's language, everyday words in the Mode simple). */
data class GuideCardUi(
    val id: String,
    val stage: Int,
    val target: String,
    val kind: String,
    val title: String,
    val why: String,
    val options: List<GuideOptionUi>,
    val elsewhere: List<GuideOptionUi>,
    val lexicon: String,
    val status: GuideStatus,
    val alert: Boolean,
    /** Its choice can be undone (« Annuler mon choix »): it changed the song and that change is still there. */
    val undoable: Boolean,
    /** G3: the track it is about ([com.pocketstudio.model.guide.GuideTrack.key]) and its name, for « La fiche de la piste » (null: the song). */
    val sheet: String? = null,
    val sheetName: String = "",
    /** G4: the effects it is about (stage 3), track by track. */
    val effects: List<GuideFxUi> = emptyList(),
    /** G4: what the analysis reads in the song about it (« En mono, le morceau ne perd que 0,8 dB »), in the interface's language. */
    val heard: List<String> = emptyList(),
    /** Its stage's title, in everyday words in the Mode simple. */
    val stageTitle: String = GuideStages.title(stage),
)

/** A line of the plan: a card, its status (faite ✓, sautée ⟶, à revoir ↺), ticked by the analysis or decided. */
data class GuidePlanItem(val id: String, val title: String, val status: GuideStatus, val auto: Boolean)

/** A stage of the plan, folded under its title, with its cards (the applicable ones). */
data class GuideStageUi(val stage: Int, val title: String, val items: List<GuidePlanItem>) {
    val done: Int get() = items.count { it.status.done }
    val settled: Boolean get() = items.all { it.status.settled }
}

/** What the Guide's panel (G2) binds to: « Où on en est », the card, the plan, the track sheets. */
data class GuideUi(
    /** True until the first analysis is in. */
    val loading: Boolean = true,
    val header: String = "",
    /** « Tu en étais à… », « Fait : … », « Reste : … ». */
    val summary: List<String> = emptyList(),
    val percent: Int = 0,
    val done: Int = 0,
    val total: Int = 0,
    val toDecide: Int = 0,
    val next: GuideCardUi? = null,
    val alerts: List<GuideCardUi> = emptyList(),
    val plan: List<GuideStageUi> = emptyList(),
    /** Every card of the plan, by id (to open one from the plan or a sheet). */
    val cards: Map<String, GuideCardUi> = emptyMap(),
    val sheets: List<GuideSheet> = emptyList(),
    val facts: List<GuideFact> = emptyList(),
    val simple: Boolean = false,
    /** Where [next] is in the plan (from 1; 0: an alert, or nothing left), for « Étape 12/34 ». */
    val step: Int = 0,
    /** The Mode simple: the other stages, folded under « Aller plus loin » (empty in the full app). */
    val further: List<GuideStageUi> = emptyList(),
    /** The Guide was last used a day ago or more: its panel opens on « où on en est ». */
    val back: Boolean = false,
    /** G4: the song is being measured (each track alone, mono, phone), and its measures are those of the song as it is. */
    val measuring: Boolean = false,
    val measured: Boolean = false,
)

/** A screen to open for an option (« Voir le réglage »): the spot ([com.pocketstudio.model.guide.GuideAction.spot]) and the track role. */
data class GuideOpen(val spot: String, val target: String)

/** The tag of a Guide's undo step (the session's [ProjectSession.undo] returns it). */
data class GuideEdit(val card: String, val what: String)

/**
 * The Guide's plumbing between the open project and its panel: the analysis runs again after each
 * change of the project (debounced, off the main thread) and is published as [ui]; the buttons
 * write the Guide's state, and the changes an option makes, through the session, one undo step
 * each, worked out off the main thread. [onEdited] gets the project as it was before each such
 * step (the engine and the playback follow it). No screen here: the panel binds to [ui] and calls
 * the actions.
 */
class GuideViewModel(
    private val session: ProjectSession,
    private val scope: CoroutineScope,
    private val worker: CoroutineDispatcher = Dispatchers.Default,
    private val clock: () -> Long = { clockForTest?.invoke() ?: System.currentTimeMillis() },
    private val debounceMs: Long = DEBOUNCE_MS,
    private val onEdited: (Project) -> Unit = {},
    /** G4: measures the song (the app's offline renders), null when it cannot. */
    private val measurer: (suspend (Project) -> com.pocketstudio.model.guide.GuideMeasures?)? = null,
    /** The song playing stops before a measure asked for (two things at once would load the phone). */
    private val pauseMusic: () -> Unit = {},
    /** False while a take records: no measure then (G4 review: the phone is busy, the take comes first). */
    private val canMeasure: () -> Boolean = { true },
    /** The song is playing: no measure starts by itself then (G5 review). */
    private val playing: () -> Boolean = { false },
) {
    private val _ui = MutableStateFlow(GuideUi())
    val ui: StateFlow<GuideUi> = _ui.asStateFlow()

    private val _opens = MutableSharedFlow<GuideOpen>(extraBufferCapacity = 4)
    /** The screens the options ask to open. */
    val opens: SharedFlow<GuideOpen> = _opens.asSharedFlow()

    // No replay: a notice closed does not come back after a rotation or on another song (G2 review).
    private val _notices = MutableSharedFlow<String>(extraBufferCapacity = 4)
    /** What an action could not do, and why (« ce réglage a changé depuis… »), to show in a line. */
    val notices: SharedFlow<String> = _notices.asSharedFlow()

    private val simple = MutableStateFlow(false)
    private val measuring = MutableStateFlow(false)
    /** One measure at a time: a balance asked for while one runs waits for it (G4 review). */
    private val measureLock = kotlinx.coroutines.sync.Mutex()
    /**
     * Every measure (and a balance waiting for one) runs under this parent: [cancelMeasure] stops them
     * all at once when the music starts or the panel closes (G5 review: one job overwritten by another survived).
     */
    private val measures = kotlinx.coroutines.SupervisorJob(scope.coroutineContext[Job])

    /** The last report, for the actions (null before the first analysis). */
    @Volatile private var report: GuideReport? = null
    @Volatile private var fullReport: GuideReport? = null
    private var job: Job? = null

    /** How many analyses ran (tests). */
    @Volatile var analyses = 0
        private set

    init {
        start()
    }

    @OptIn(FlowPreview::class)
    private fun start() {
        job = scope.launch {
            combine(session.project, simple, measuring) { p, s, m -> Triple(p, s, m) }.debounce(debounceMs).collectLatest { (p, s, busy) ->
                if (p.style != null && p.guide.seen.isEmpty()) {
                    // An assistant's song the Guide sees for the first time: what each chain is now, quietly (not an undo step).
                    session.editQuietly { GuideActions.withBaseline(it) }
                    return@collectLatest  // the edit brings the project round again
                }
                val now = clock()
                val r = withContext(worker) { GuideAnalyzer.analyze(p, s, now) }
                // The Mode simple folds the other stages under « Aller plus loin »: they come from the whole parcours.
                val full = if (s) withContext(worker) { GuideAnalyzer.analyze(p, false, now) } else null
                // The song's sound signature once (a JSON of the whole song, G5 review): both checks read it.
                val (fresh, levels) = withContext(worker) {
                    val m = p.guide.measures
                    val same = m != null && m.signature != 0 && m.signature == com.pocketstudio.model.guide.GuideSignatures.sound(p)
                    (same && !m!!.partial) to same
                }
                analyses++
                report = r
                fullReport = full
                _ui.value = uiOf(r, p, full, now, levels).copy(measuring = busy, measured = fresh)
            }
        }
    }

    /** The Mode simple's cards and words (the settings say). */
    fun setSimple(on: Boolean) {
        simple.value = on
    }

    /**
     * Option [optionId] of card [cardId]: an option that opens a screen asks for it ([opens]); a check
     * is heard, not applied (the panel plays it); the others change the song, one undo step (one that
     * needs the song's measures measures it first).
     */
    fun apply(cardId: String, optionId: String) {
        val card = cardOf(cardId) ?: return
        val option = GuideActions.optionOf(session.project.value, card, optionId) ?: return
        when (option.action.type) {
            com.pocketstudio.model.guide.GuideAction.OPEN -> {
                _opens.tryEmit(GuideOpen(option.action.spot, option.action.target.ifEmpty { card.target }))
                return
            }
            com.pocketstudio.model.guide.GuideAction.LISTEN -> return
            // G5: « Écouter » (« Ton morceau est prêt »): the song plays, and the card is decided.
            com.pocketstudio.model.guide.GuideAction.PLAY -> _opens.tryEmit(GuideOpen(PLAY, option.action.name))
        }
        if (option.action.needsMeasure && measurer != null) {
            if (!canMeasure()) {
                _notices.tryEmit(tr("Le Guide écoutera le morceau après la prise."))
                return
            }
            scope.launch(measures) {
                if (!measured()) {
                    pauseMusic()
                    // A measure already running is waited for (not given up silently).
                    if (!measureNow()) return@launch
                }
                // The faders moved: the song's loudness, its sections and mono are measured again, as any measure.
                edit(cardId, "apply:$optionId", after = { measure() }) { GuideActions.tryApply(it, cardId, optionId, clock()) }
            }
            return
        }
        edit(cardId, "apply:$optionId") { GuideActions.tryApply(it, cardId, optionId, clock()) }
    }

    /** The song's measures are those of the song as it is. */
    private suspend fun measured(): Boolean = withContext(worker) { isFresh(session.project.value) }

    private fun isFresh(p: Project): Boolean =
        p.guide.measures?.let { !it.partial && it.signature != 0 && it.signature == com.pocketstudio.model.guide.GuideSignatures.sound(p) } == true

    /**
     * G4: the song measured (each track alone, mono, phone, its sections), unless its measures are
     * those of the song as it is; quietly (not an undo step), and only when nothing changed meanwhile.
     * The panel asks for it when a card of the levels shows, the music stopped.
     */
    fun measure() {
        if (measurer == null || measuring.value || !canMeasure() || playing()) return
        scope.launch(measures) { if (!measured()) measureNow(say = false) }
    }

    /** G4 review: every measure running (about 15 renders each) stops: the music starts, the panel closes. */
    fun cancelMeasure() {
        measures.children.forEach { it.cancel() }
    }

    /**
     * One measure now, or the one running waited for; false when it could not be taken (said in
     * [notices] when [say]: asked for by a button).
     */
    private suspend fun measureNow(say: Boolean = true): Boolean {
        val m = measurer ?: return false
        measureLock.lock()
        measuring.value = true
        try {
            // Taken meanwhile (the one this waited for): nothing more to do.
            if (measured()) return true
            val p = session.project.value
            val measures = m(p)
            if (measures == null) {
                if (say) _notices.tryEmit(tr("Le Guide n'a pas pu écouter le morceau ici."))
                return false
            }
            val now = session.project.value
            val same = now === p || withContext(worker) { com.pocketstudio.model.guide.GuideSignatures.sound(now) } == measures.signature
            if (!same) return false
            session.editQuietly { cur -> cur.copy(guide = cur.guide.copy(measures = measures)) }
            measureCount++
            return true
        } finally {
            measuring.value = false
            measureLock.unlock()
        }
    }

    /** How many measures were taken (tests). */
    @Volatile var measureCount = 0
        private set

    /** « Sauter »; [forever]: refused, never offered again. */
    fun skip(cardId: String, forever: Boolean = false) = edit(cardId, if (forever) "refuse" else "skip") { GuideOutcome(GuideActions.skip(it, cardId, clock(), forever)) }

    /** « Plus tard ». */
    fun later(cardId: String) = edit(cardId, "later") { GuideOutcome(GuideActions.later(it, cardId, clock())) }

    /** One option of a card refused (the others stay). */
    fun refuseOption(cardId: String, optionId: String) = edit(cardId, "refuse:$optionId") { GuideOutcome(GuideActions.refuseOption(it, cardId, optionId, clock())) }

    /** « Annuler mon choix »: what the card changed comes back, the card is to decide again (refused, with a [notices], when a track changed since). */
    fun undoChoice(cardId: String) = edit(cardId, "undo") { GuideActions.tryUndoChoice(it, cardId, clock()) }

    /** « Ramener » the changes of group [id] ([com.pocketstudio.model.guide.GuideChange.id], a track sheet's line). */
    fun revert(id: String) = edit("", "revert:$id") { GuideActions.tryRevert(it, id, clock()) }

    /** A free note, on the song or on track [track]. */
    fun note(text: String, track: String = "") = edit("", "note") { GuideOutcome(GuideActions.note(it, text, clock(), track)) }

    /**
     * G5: « Mon premier morceau » on the open song (the old Missions, now its first cards), quietly
     * (not an undo step); a song already on it stays where it was.
     */
    fun beginFirstSong() {
        session.editQuietly { com.pocketstudio.model.guide.GuideStart.begin(it, com.pocketstudio.model.guide.GuideStart.FIRST) }
    }

    /** G5: a user mid-mission (the old settings): the open song becomes a first song, on the step reached. */
    fun fromMissions(step: Int, skipped: Set<Int>) {
        session.editQuietly { com.pocketstudio.model.guide.GuideStart.fromMissions(it, step, skipped, clock()) }
    }

    /**
     * G5: card [cardId] decided by what the app saw (the song shared: « Prêt à partager ? »), quietly;
     * a card already settled stays as it is.
     */
    fun markDone(cardId: String, optionId: String = "") {
        session.editQuietly { p ->
            if (p.guide.statusOf(cardId).settled) p
            else p.copy(guide = p.guide.withStatus(cardId, GuideStatus.DONE, clock(), optionId))
        }
    }

    /** G5 review: card [cardId] put off quietly (its panel folded on it), unless already decided or put off. */
    fun markLater(cardId: String) {
        session.editQuietly { p ->
            if (p.guide.statusOf(cardId) != GuideStatus.TODO) p
            else p.copy(guide = p.guide.withStatus(cardId, GuideStatus.LATER, clock()))
        }
    }

    /** Stops following the project (the screen that owns it is gone). */
    fun close() {
        job?.cancel()
        cancelMeasure()
    }

    private fun cardOf(id: String): GuideCard? =
        report?.status(id)?.card ?: fullReport?.status(id)?.card ?: GuideActions.cardOf(session.project.value, id)

    /**
     * [transform] worked out on [worker] (the analysis it needs takes tens of ms), then written on the
     * session's thread as one undo step: again on the project as it is when it changed meanwhile. A
     * problem it gives is said ([notices]); [onEdited] gets the project from before the step.
     */
    private fun edit(cardId: String, what: String, after: (suspend () -> Unit)? = null, transform: (Project) -> GuideOutcome) {
        scope.launch {
            val before = session.project.value
            val worked = withContext(worker) { transform(before) }
            val now = session.project.value
            val out = if (now === before) worked else transform(now)
            out.problem?.let { _notices.tryEmit(it) }
            if (out.project == now) return@launch
            session.edit(tag = GuideEdit(cardId, what)) { cur -> if (cur === now) out.project else cur }
            if (session.project.value !== now) onEdited(now)
            edits++
            after?.invoke()
        }
    }

    /** How many actions changed the project (tests). */
    @Volatile var edits = 0
        private set

    /** [levels]: the tracks' levels were measured on the song as it is (G4 review: the balance is heard only then). */
    private fun uiOf(r: GuideReport, p: Project, full: GuideReport? = null, now: Long = 0L, levels: Boolean = false): GuideUi {
        val undoable = p.guide.history.filter { !it.reverted }.map { it.card }.toSet()
        // Audit final: the generic parcours says the song's own tempo (« {bpm} » in its texts).
        val bpm = if (p.tempoBpm % 1.0 == 0.0) p.tempoBpm.toInt().toString() else com.pocketstudio.model.LoudnessTargets.format(p.tempoBpm)
        fun said(text: String) = if ("{bpm}" in text) text.replace("{bpm}", bpm) else text
        fun option(card: GuideCard, o: GuideProposal) = GuideOptionUi(
            o.id, said(o.labelFor(r.simple)), said(o.whyFor(r.simple)), o.valueFor(r.simple)?.let { said(it) },
            o.action.spot.takeIf { o.action.type == com.pocketstudio.model.guide.GuideAction.OPEN }, p.guide.refusals.any { it.card == card.id && it.option == o.id },
            edits = o.action.edits, heard = o.action.heard && (o.action.type != com.pocketstudio.model.guide.GuideAction.BALANCE || levels),
            listen = o.action.name.takeIf { o.action.type == com.pocketstudio.model.guide.GuideAction.LISTEN },
        )
        val effects = effectsOf(r, p)
        // The first track of each role (« Voir le réglage » › its sheet).
        val byRole = p.guideTracks().groupBy { it.role }.mapValues { it.value.first() }
        // G5: « Ton morceau est prêt »: « Depuis le début » only when « Écouter » starts later; its guide when the assistant wrote one.
        val later = com.pocketstudio.model.FirstListen.startsLater(p)
        fun shown(card: GuideCard, o: GuideOptionUi): Boolean = card.id != READY ||
            (o.id != "start" || later) && (o.id != "guide" || p.guide.songGuide.isNotEmpty())
        fun cardUi(s: GuideCardStatus): GuideCardUi {
            val track = byRole[s.card.target]?.takeIf { it.channel != com.pocketstudio.model.Mixer.MASTER }
            return GuideCardUi(
                s.card.id, s.card.stage, s.card.target, s.card.kind, said(s.card.titleFor(r.simple)), said(s.card.whyFor(r.simple)),
                s.card.proposals.map { option(s.card, it) }.filter { !it.refused && shown(s.card, it) }, s.card.elsewhere.map { option(s.card, it) }, s.card.lexicon,
                s.status, s.alert, s.card.id in undoable, track?.key, track?.name?.let(com.pocketstudio.model.DefaultNames::shown).orEmpty(),
                heard = if (s.card.id == READY) listOf(readyLine(p)) else FACTS[s.card.id].orEmpty().mapNotNull { id -> r.fact(id)?.text },
                effects = if (s.card.stage == 3 && s.card.id !in NO_EFFECTS) effects.filter { fx -> s.card.target == com.pocketstudio.model.guide.GuideRoles.SONG || fx.role in rolesOf(s.card.target) }.map { it.ui } else emptyList(),
                stageTitle = GuideStages.title(s.card.stage, r.simple),
            )
        }
        fun planOf(cards: List<GuideCardStatus>, simple: Boolean) = cards.groupBy { it.card.stage }.toSortedMap().map { (stage, cs) ->
            GuideStageUi(stage, GuideStages.title(stage, simple), cs.map { GuidePlanItem(it.card.id, said(it.card.titleFor(simple)), it.status, it.auto) })
        }
        val applicable = r.cards.filter { it.applicable }
        val plan = planOf(applicable, r.simple)
        // The Mode simple's « Aller plus loin »: the stages it leaves out, as the full app has them.
        val furtherCards = full?.cards?.filter { it.applicable && it.card.stage !in GuideStages.SIMPLE }.orEmpty()
        // Audit final: « Étape n/N » counts the cards settled (the same count as the % and « à décider »), not the card's place in the plan.
        val step = r.next?.takeIf { n -> applicable.any { it.card.id == n.card.id } }?.let { applicable.count { it.settled } + 1 } ?: 0
        val back = p.guide.lastUsed > 0 && now - p.guide.lastUsed >= GuideAnalyzer.DAY_MS
        return GuideUi(
            loading = false, header = r.header, summary = r.summary, percent = r.percent, done = r.done, total = r.applicable, toDecide = r.toDecide,
            next = r.next?.let(::cardUi), alerts = r.alerts.map(::cardUi), plan = plan,
            cards = (furtherCards + r.alerts + applicable).associate { it.card.id to cardUi(it) }, sheets = r.sheets, facts = r.facts, simple = r.simple,
            step = step, further = planOf(furtherCards, false), back = back,
        )
    }

    private class Fx(val role: String, val ui: GuideFxUi)

    private fun effectName(t: EffectType): String = tr(t.label)

    private fun busName(bus: Int): String = tr(Buses.NAMES.getOrElse(bus) { "Réverbe" })

    /** Every effect and send on the song's tracks and its master, in the pro order (the sheets'), with its why. */
    private fun effectsOf(r: GuideReport, p: Project): List<Fx> {
        val out = ArrayList<Fx>()
        for (sheet in r.sheets) {
            val t = sheet.track
            val name = com.pocketstudio.model.DefaultNames.shown(t.name)
            for (e in sheet.chain) {
                val id = if (e.bypassed) GuideActions.effectOn(t.key, e.slot) else GuideActions.effectOff(t.key, e.slot)
                out += Fx(t.role, GuideFxUi(t.key, name, effectName(e.type), guideText(e.why), e.bypassed, id))
            }
            sheet.sends.forEachIndexed { bus, level ->
                val back = GuideActions.sendOn(t.key, bus)
                val off = level <= 0.01f
                if (off && GuideActions.dynamicOption(p, back) == null) return@forEachIndexed
                val type = if (bus == 0) com.pocketstudio.model.EffectType.REVERB else com.pocketstudio.model.EffectType.ECHO
                val label = tr("Envoi : %s", busName(bus))
                out += Fx(t.role, GuideFxUi(t.key, name, label, guideText(r.parcours.why(t.role, type)), off, if (off) back else GuideActions.sendOff(t.key, bus)))
            }
        }
        val master = com.pocketstudio.model.guide.GuideTrack.MASTER
        p.mixer.master.forEachIndexed { slot, ins ->
            if (ins.effect == com.pocketstudio.model.EffectType.NONE) return@forEachIndexed
            val id = if (ins.bypass) GuideActions.effectOn(master, slot) else GuideActions.effectOff(master, slot)
            out += Fx(com.pocketstudio.model.guide.GuideRoles.MASTER, GuideFxUi(master, tr("Sortie générale"), effectName(ins.effect),
                guideText(r.parcours.why(com.pocketstudio.model.guide.GuideRoles.MASTER, ins.effect)), ins.bypass, id))
        }
        return out
    }

    /** « House du soir · 2:40 · 6 parties »: the song just made, in a line (as « Ton morceau est prêt » said it). */
    private fun readyLine(p: Project): String = listOf(
        com.pocketstudio.model.DefaultNames.shown(p.name), GuideAnalyzer.duration(p),
        if (p.song.size == 1) tr("1 partie") else tr("%d parties", p.song.size),
    ).filter { it.isNotBlank() }.joinToString(" · ")

    companion object {
        /**
         * Tests (G6): the Guide's clock, moved days ahead without waiting (« Te revoilà »); null: the
         * phone's. Read at each use, by every Guide made with the default clock.
         */
        @androidx.annotation.VisibleForTesting
        @Volatile
        var clockForTest: (() -> Long)? = null

        /** « Ton morceau est prêt » (G5): the first card of a song just made. */
        const val READY = com.pocketstudio.model.guide.GuideStart.READY

        /** The screen [GuideOpen.spot] of a [com.pocketstudio.model.guide.GuideAction.PLAY]: the song plays ([GuideOpen.target] "drop" or "start"). */
        const val PLAY = "play"

        /** A burst of edits (a slider drag) is analysed once, this long after the last one. */
        const val DEBOUNCE_MS = 300L

        /** The facts each card shows (« ce que le Guide entend »). */
        private val FACTS = mapOf(
            "fx.lowcut" to listOf("track.no_lowcut"),
            // Audit final: the melody rules the analysis checks, shown on the card of the line they are about.
            "sound.lead" to listOf("melody.lead"),
            "sound.bass" to listOf("melody.bass"),
            "sound.counter" to listOf("melody.counter"),
            "arr.differ" to listOf("peaks.identical"),
            "arr.bass" to listOf("bass.repeats"),
            "arr.pad" to listOf("pad.everywhere"),
            "move.sections" to listOf("automation.missing"),
            "mix.balance" to listOf("mix.levels", "mix.faders_flat"),
            "mix.pans" to listOf("mix.no_pans"),
            "mix.master" to listOf("master.headroom"),
            "mix.contrast" to listOf("loudness.contrast"),
            "mix.mono" to listOf("mono.loss"),
            "mix.phone" to listOf("phone.low"),
        )

        /** The cards of stage 3 that are about something else than a track's effects (the ducking, the returns, the low cuts). */
        private val NO_EFFECTS = setOf("fx.sidechain", "fx.sends", "fx.lowcut")

        /** The tracks a card about [target] lists: the drums' card every drum line. */
        private fun rolesOf(target: String): Set<String> = when (target) {
            com.pocketstudio.model.guide.GuideRoles.DRUMS -> with(com.pocketstudio.model.guide.GuideRoles) { setOf(KICK, SNARE, HATS, PERC, RUMBLE, DRUMS) }
            else -> setOf(target)
        }
    }
}
