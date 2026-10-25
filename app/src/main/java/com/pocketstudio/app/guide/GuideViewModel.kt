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
    /** It changes the song: it can be heard with and without before it is applied. */
    val edits: Boolean = false,
)

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
    private val clock: () -> Long = System::currentTimeMillis,
    private val debounceMs: Long = DEBOUNCE_MS,
    private val onEdited: (Project) -> Unit = {},
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
            combine(session.project, simple) { p, s -> p to s }.debounce(debounceMs).collectLatest { (p, s) ->
                if (p.style != null && p.guide.seen.isEmpty()) {
                    // An assistant's song the Guide sees for the first time: what each chain is now, quietly (not an undo step).
                    session.editQuietly { GuideActions.withBaseline(it) }
                    return@collectLatest  // the edit brings the project round again
                }
                val now = clock()
                val r = withContext(worker) { GuideAnalyzer.analyze(p, s, now) }
                // The Mode simple folds the other stages under « Aller plus loin »: they come from the whole parcours.
                val full = if (s) withContext(worker) { GuideAnalyzer.analyze(p, false, now) } else null
                analyses++
                report = r
                fullReport = full
                _ui.value = uiOf(r, p, full, now)
            }
        }
    }

    /** The Mode simple's cards and words (the settings say). */
    fun setSimple(on: Boolean) {
        simple.value = on
    }

    /** Option [optionId] of card [cardId]: an option that opens a screen asks for it ([opens]); the others change the song, one undo step. */
    fun apply(cardId: String, optionId: String) {
        val card = cardOf(cardId) ?: return
        val option = (card.proposals + card.elsewhere).firstOrNull { it.id == optionId } ?: return
        if (option.action.type == com.pocketstudio.model.guide.GuideAction.OPEN) {
            _opens.tryEmit(GuideOpen(option.action.spot, option.action.target.ifEmpty { card.target }))
            return
        }
        edit(cardId, "apply:$optionId") { GuideActions.tryApply(it, cardId, optionId, clock()) }
    }

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

    /** Stops following the project (the screen that owns it is gone). */
    fun close() {
        job?.cancel()
    }

    private fun cardOf(id: String): GuideCard? =
        report?.status(id)?.card ?: fullReport?.status(id)?.card ?: GuideActions.cardOf(session.project.value, id)

    /**
     * [transform] worked out on [worker] (the analysis it needs takes tens of ms), then written on the
     * session's thread as one undo step: again on the project as it is when it changed meanwhile. A
     * problem it gives is said ([notices]); [onEdited] gets the project from before the step.
     */
    private fun edit(cardId: String, what: String, transform: (Project) -> GuideOutcome) {
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
        }
    }

    /** How many actions changed the project (tests). */
    @Volatile var edits = 0
        private set

    private fun uiOf(r: GuideReport, p: Project, full: GuideReport? = null, now: Long = 0L): GuideUi {
        val undoable = p.guide.history.filter { !it.reverted }.map { it.card }.toSet()
        fun option(card: GuideCard, o: GuideProposal) = GuideOptionUi(
            o.id, guideText(o.label), guideText(o.why), o.value?.let(::guideText),
            o.action.spot.takeIf { o.action.type == com.pocketstudio.model.guide.GuideAction.OPEN }, p.guide.refusals.any { it.card == card.id && it.option == o.id },
            edits = o.action.edits,
        )
        // The first track of each role (« Voir le réglage » › its sheet).
        val byRole = p.guideTracks().groupBy { it.role }.mapValues { it.value.first() }
        fun cardUi(s: GuideCardStatus): GuideCardUi {
            val track = byRole[s.card.target]?.takeIf { it.channel != com.pocketstudio.model.Mixer.MASTER }
            return GuideCardUi(
                s.card.id, s.card.stage, s.card.target, s.card.kind, s.card.titleFor(r.simple), s.card.whyFor(r.simple),
                s.card.proposals.map { option(s.card, it) }.filter { !it.refused }, s.card.elsewhere.map { option(s.card, it) }, s.card.lexicon,
                s.status, s.alert, s.card.id in undoable, track?.key, track?.name?.let(com.pocketstudio.model.DefaultNames::shown).orEmpty(),
            )
        }
        fun planOf(cards: List<GuideCardStatus>, simple: Boolean) = cards.groupBy { it.card.stage }.toSortedMap().map { (stage, cs) ->
            GuideStageUi(stage, GuideStages.title(stage), cs.map { GuidePlanItem(it.card.id, it.card.titleFor(simple), it.status, it.auto) })
        }
        val applicable = r.cards.filter { it.applicable }
        val plan = planOf(applicable, r.simple)
        // The Mode simple's « Aller plus loin »: the stages it leaves out, as the full app has them.
        val furtherCards = full?.cards?.filter { it.applicable && it.card.stage !in GuideStages.SIMPLE }.orEmpty()
        val step = r.next?.let { n -> applicable.indexOfFirst { it.card.id == n.card.id } + 1 } ?: 0
        val back = p.guide.lastUsed > 0 && now - p.guide.lastUsed >= GuideAnalyzer.DAY_MS
        return GuideUi(
            loading = false, header = r.header, summary = r.summary, percent = r.percent, done = r.done, total = r.applicable, toDecide = r.toDecide,
            next = r.next?.let(::cardUi), alerts = r.alerts.map(::cardUi), plan = plan,
            cards = (furtherCards + r.alerts + applicable).associate { it.card.id to cardUi(it) }, sheets = r.sheets, facts = r.facts, simple = r.simple,
            step = step, further = planOf(furtherCards, false), back = back,
        )
    }

    companion object {
        /** A burst of edits (a slider drag) is analysed once, this long after the last one. */
        const val DEBOUNCE_MS = 300L
    }
}
