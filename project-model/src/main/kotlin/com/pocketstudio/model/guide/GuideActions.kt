package com.pocketstudio.model.guide

import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.AutomationLane
import com.pocketstudio.model.Buses
import com.pocketstudio.model.Channel
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Insert
import com.pocketstudio.model.LoudnessTarget
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.MusicBus
import com.pocketstudio.model.Project
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.removeSongPart
import com.pocketstudio.model.songBars
import com.pocketstudio.model.syncScenes
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.TrackLayer
import com.pocketstudio.model.Layers
import com.pocketstudio.model.track
import com.pocketstudio.model.withTrack
import com.pocketstudio.model.freeInsertSlot
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.longFxLimitNotice
import com.pocketstudio.model.longFxOverLimit
import com.pocketstudio.model.withoutStaleInsertLanes
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.float
import kotlin.math.log10
import kotlin.math.pow

/**
 * What the Guide's buttons do to a project (each one becomes one undo step in the session): apply
 * an option, skip, put off, refuse, undo a choice, bring one change back, note. Every change it
 * makes on a track goes into [GuideState.history] with its value before, so nothing is lost.
 * Pure functions: the project in, the project out.
 */
object GuideActions {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; coerceInputValues = true }

    /** The card [cardId] of [p]'s parcours, or the alert card of that id (its target from the fact). */
    fun cardOf(p: Project, cardId: String, parcours: GuideParcours = GuideLibrary.forProject(p)): GuideCard? {
        parcours.card(cardId)?.let { return it }
        if (!isAlert(cardId)) return null
        val factId = cardId.removePrefix("alert.")
        val base = GuideLibrary.alert(factId) ?: return null
        val facts = GuideFacts.of(p)
        val role = facts.list.firstOrNull { it.id == factId }?.tracks?.firstOrNull()?.let { k -> facts.trackOf(k)?.role }
        return base.copy(target = role ?: base.target)
    }

    /** An alert's card ("alert.<fact>"): it comes back whenever its fact does, only « C'est voulu » is kept. */
    fun isAlert(cardId: String) = cardId.startsWith("alert.")

    /**
     * Option [optionId] of [card]: one of its own, or (a card of the effects, stage 3) one effect of
     * « Voilà ce que j'ai mis » retired or put back ([dynamicOption]).
     */
    fun optionOf(p: Project, card: GuideCard, optionId: String): GuideProposal? =
        (card.proposals + card.elsewhere).firstOrNull { it.id == optionId } ?: if (card.stage == 3) dynamicOption(p, optionId) else null

    /** An option made for one effect of one track, not written in a parcours: "<verb>@<track key>@<slot or bus>". */
    fun isDynamic(optionId: String) = optionId.count { it == '@' } == 2

    /** « Retirer » effect [slot] of [track] (set aside, its settings kept). */
    fun effectOff(track: String, slot: Int) = "off@$track@$slot"

    /** « Remettre » it. */
    fun effectOn(track: String, slot: Int) = "on@$track@$slot"

    /** « Retirer » [track]'s send to bus [bus]. */
    fun sendOff(track: String, bus: Int) = "dry@$track@$bus"

    /** « Remettre » it at what it was before the Guide took it off ([sendOff]). */
    fun sendOn(track: String, bus: Int) = "wet@$track@$bus"

    /**
     * The option [optionId] names ([effectOff], [effectOn], [sendOff], [sendOn]) on [p], or null when
     * its track, slot or bus is not there (or a send was never taken off by the Guide).
     */
    fun dynamicOption(p: Project, optionId: String): GuideProposal? {
        if (!isDynamic(optionId)) return null
        val (verb, track, n) = optionId.split('@')
        val i = n.toIntOrNull() ?: return null
        if (p.guideTracks().none { it.key == track } && track != GuideTrack.MASTER) return null
        val action = when (verb) {
            "off", "on" -> {
                if (i !in 0 until Mixer.INSERTS) return null
                GuideAction(type = GuideAction.SLOT, track = track, param = i, value = if (verb == "off") 1f else 0f)
            }
            "dry" -> GuideAction(type = GuideAction.SEND, track = track, bus = i, value = 0f).takeIf { i in 0 until Buses.COUNT } ?: return null
            "wet" -> {
                val was = p.guide.history.lastOrNull { it.track == track && it.field == "$SEND$i" && !it.reverted && it.after == JsonPrimitive(0f) }
                    ?.before?.let { (it as? JsonPrimitive)?.content?.toFloatOrNull() } ?: return null
                GuideAction(type = GuideAction.SEND, track = track, bus = i, value = was)
            }
            else -> return null
        }
        val label = when (verb) {
            "off", "dry" -> tr("Retirer")
            else -> tr("Le remettre")
        }
        return GuideProposal(optionId, label, action = action)
    }

    /** [apply]'s project only (the problem, if any, left aside). */
    fun apply(p: Project, cardId: String, optionId: String, at: Long, parcours: GuideParcours = GuideLibrary.forProject(p)): Project =
        tryApply(p, cardId, optionId, at, parcours).project

    /**
     * Option [optionId] of card [cardId] applied at [at]: its action on the project, the card done
     * with that option (refused for good when the action says so), its changes in the history as one
     * group ([GuideChange.id]). Unchanged when there is no such card or option; unchanged with a
     * [GuideOutcome.problem] when the option finds nothing to change or would go past the engine's
     * long effects ([com.pocketstudio.model.longFxOverLimit]).
     */
    fun tryApply(p: Project, cardId: String, optionId: String, at: Long, parcours: GuideParcours = GuideLibrary.forProject(p)): GuideOutcome {
        val card = cardOf(p, cardId, parcours) ?: return GuideOutcome(p)
        val option = optionOf(p, card, optionId) ?: return GuideOutcome(p)
        val action = option.action
        if (action.type == GuideAction.OPEN || action.type == GuideAction.LISTEN) return GuideOutcome(p)
        if (action.type == GuideAction.BALANCE && GuideFacts.of(p).fresh == null) return GuideOutcome(p, tr("Il faut d'abord mesurer le morceau."))
        if (action.type == GuideAction.LATER) return GuideOutcome(later(p, card.id, at, parcours))
        val keys = if (isAlert(card.id)) GuideFacts.of(p).list.firstOrNull { it.id == card.id.removePrefix("alert.") }?.tracks else null
        val edited = if (action.edits) perform(p, card, action, keys) ?: return GuideOutcome(p, tr("Plus de place pour une courbe : le morceau en a déjà 16.")) else p
        if (action.edits && edited == p) return GuideOutcome(p, tr("Rien à changer ici : la piste n'a pas cet effet ou il est déjà réglé."))
        edited.mixer.longFxOverLimit(p.mixer)?.let { return GuideOutcome(p, longFxLimitNotice(it)) }
        // The edit's own state (« Placer chaque piste » moves the measures with the faders), its baseline from before.
        val base = edited.guide.withBase(p, parcours)
        val id = "c${base.nextChange}"
        val changes = changesBetween(p, edited, card.id, card.target, at, id)
        var state = base.withChanges(changes).copy(nextChange = base.nextChange + 1, step = card.id, lastUsed = maxOf(base.lastUsed, at))
        // An alert is not ticked for good: it comes back when its fact does; one effect of « Voilà ce que
        // j'ai mis » retired or put back does not decide the whole card.
        if (!isAlert(card.id) && !isDynamic(option.id)) state = state.withStatus(card.id, GuideStatus.DONE, at, option.id)
        if (action.type == GuideAction.REFUSE) state = state.withRefusal(card.id, "", at)
        state = state.copy(seen = seenAfter(state.seen, p, edited, card))
        return GuideOutcome(edited.copy(guide = state))
    }

    /** Card [cardId] passed over (« Sauter »); [forever]: refused, never offered again. An alert is only put off (or refused). */
    fun skip(p: Project, cardId: String, at: Long, forever: Boolean = false, parcours: GuideParcours = GuideLibrary.forProject(p)): Project {
        var state = p.guide.withStatus(cardId, if (isAlert(cardId)) GuideStatus.LATER else GuideStatus.SKIPPED, at).copy(step = cardId)
        if (forever) state = state.withRefusal(cardId, "", at)
        return p.copy(guide = state.withBase(p, parcours))
    }

    /** Card [cardId] put off (« Plus tard »): offered again once the others are decided. */
    fun later(p: Project, cardId: String, at: Long, parcours: GuideParcours = GuideLibrary.forProject(p)): Project =
        p.copy(guide = p.guide.withStatus(cardId, GuideStatus.LATER, at).copy(step = cardId).withBase(p, parcours))

    /** Option [optionId] of card [cardId] refused: the card offers its others. */
    fun refuseOption(p: Project, cardId: String, optionId: String, at: Long): Project =
        p.copy(guide = p.guide.withRefusal(cardId, optionId, at))

    /** [tryUndoChoice]'s project. */
    fun undoChoice(p: Project, cardId: String, at: Long): Project = tryUndoChoice(p, cardId, at).project

    /**
     * The choice made on card [cardId] undone: its changes brought back (newest first), the card to
     * decide again, its refusal forgotten. Refused as a whole (nothing changes, the card keeps its
     * status) when one of its tracks was changed since: bringing it back would erase that.
     */
    fun tryUndoChoice(p: Project, cardId: String, at: Long): GuideOutcome {
        var out = p
        val groups = p.guide.history.filter { it.card == cardId && !it.reverted }.map { it.id }.distinct().reversed()
        for (g in groups) {
            val r = tryRevert(out, g, at)
            if (r.problem != null) return GuideOutcome(p, r.problem)
            out = r.project
        }
        val state = out.guide.withStatus(cardId, GuideStatus.TODO, at).let { s -> s.copy(refusals = s.refusals.filterNot { it.card == cardId }, step = cardId) }
        return GuideOutcome(out.copy(guide = state))
    }

    /** [tryRevert]'s project. */
    fun revert(p: Project, id: String, at: Long): Project = tryRevert(p, id, at).project

    /**
     * The changes of group [id] brought back (« Ramener »): each value before put back, the changes
     * kept, marked undone. Refused (nothing changes) when one of those settings is no longer what the
     * Guide left there: it was changed since, and bringing the old value back would erase that.
     */
    fun tryRevert(p: Project, id: String, at: Long): GuideOutcome {
        val group = p.guide.history.filter { it.id == id && !it.reverted }
        if (group.isEmpty()) return GuideOutcome(p)
        if (group.any { !stillAfter(p, it) }) {
            return GuideOutcome(p, tr("Ce réglage a changé depuis : le ramener effacerait ce que tu as fait après. Change-le à la main, ou annule d'abord le reste."))
        }
        var q = p
        val gone = tr("Cette piste n'est plus là : rien à ramener.")
        fun structural(c: GuideChange) = c.field.startsWith(PARTS) || c.field.startsWith(SCENE)
        for (c in group.asReversed()) if (c.kind != GuideChange.LANE && !structural(c)) q = withField(q, c.track, c.field, c.before) ?: return GuideOutcome(p, gone)
        // G4: the parts first (their curves follow them back), then the scenes made for them.
        for (c in group.filter { it.field.startsWith(PARTS) }.asReversed()) q = partsBack(q, c) ?: return GuideOutcome(p, gone)
        for (c in group.filter { it.field.startsWith(SCENE) }) q = sceneBack(q, c.field.removePrefix(SCENE).toInt()) ?: return GuideOutcome(p, gone)
        for (c in group.filter { it.field == LANE_SET }.asReversed()) {
            val now = json.decodeFromJsonElement(AutomationLane.serializer(), c.after!!)
            val was = c.before?.let { json.decodeFromJsonElement(AutomationLane.serializer(), it) }
            q = q.copy(automation = if (was == null) q.automation.filter { it.target != now.target } else q.automation.map { if (it.target == now.target) was else it })
        }
        // The curves last, all at once: a chain shifted by one slot shifts back in one move.
        val moves = group.filter { it.field == LANE_TARGET }.associate { lane(it.after!!) to lane(it.before!!) }
        if (moves.isNotEmpty()) q = q.copy(automation = q.automation.map { l -> moves[l.target]?.let { l.copy(target = it) } ?: l })
        val removed = group.filter { it.field == LANE }.map { json.decodeFromJsonElement(AutomationLane.serializer(), it.before!!) }
        if (removed.isNotEmpty()) {
            if (q.automation.size + removed.size > AutomationLane.MAX_LANES) return GuideOutcome(p, tr("Trop de courbes dans le morceau pour ramener celle-ci."))
            q = q.copy(automation = q.automation + removed)
        }
        val history = p.guide.history.map { if (it.id == id) it.copy(reverted = true) else it }
        return GuideOutcome(q.copy(guide = p.guide.copy(history = history, lastUsed = maxOf(p.guide.lastUsed, at))))
    }

    /** The parts of change [c] as they were: an added part taken out (what follows moves back, its curves too), parts cut put together again. */
    private fun partsBack(p: Project, c: GuideChange): Project? {
        val at = c.field.removePrefix(PARTS).toInt()
        val was = json.decodeFromJsonElement(partList, c.before ?: return null)
        val now = json.decodeFromJsonElement(partList, c.after ?: return null)
        if (was.isEmpty() && now.size == 1) return p.removeSongPart(at)
        val q = p.copy(song = p.song.take(at) + was + p.song.drop(at + now.size))
        // Parts cut (a fill) last as long as the part they came from: anything else would move the curves.
        return if (q.songBars == p.songBars) q else null
    }

    /** Scene [i] made by the Guide emptied again, once no part plays it. */
    private fun sceneBack(p: Project, i: Int): Project? {
        if (p.song.any { it.scene == i || i in it.play.values }) return null
        val all = p.allScenes
        if (i !in all.indices || i == p.scene) return null
        return p.copy(scenes = all.mapIndexed { k, s -> if (k == i) Scene() else s }, automation = p.automation.map { it.copy(sceneValues = it.sceneValues - i) }).syncScenes()
    }

    /** A free note on the song (or on track [track]). */
    fun note(p: Project, text: String, at: Long, track: String = ""): Project = p.copy(guide = p.guide.withNote(text, at, track))

    /**
     * The state as the Guide first looks at [p]: the parcours it follows and the signature of each
     * chain ([GuideState.seen]), so that a chain changed later in the mixer is told apart. Unchanged
     * once there.
     */
    fun withBaseline(p: Project, parcours: GuideParcours = GuideLibrary.forProject(p)): Project {
        val state = p.guide.withBase(p, parcours)
        return if (state == p.guide) p else p.copy(guide = state)
    }

    private fun GuideState.withBase(p: Project, parcours: GuideParcours): GuideState =
        if (seen.isNotEmpty() && this.parcours.isNotEmpty()) this
        else copy(parcours = parcours.style, parcoursVersion = parcours.version, seen = seen.ifEmpty { GuideSignatures.all(p) })

    /**
     * What the Guide has seen, after its own edit: the signatures it changed are taken as seen (its
     * own work is not « done elsewhere »), except those the card itself watches ([GuideCard.done]'s
     * `changed:` detectors), which its decision settles anyway.
     */
    private fun seenAfter(seen: Map<String, Int>, before: Project, after: Project, card: GuideCard): Map<String, Int> {
        val old = GuideSignatures.all(before)
        val now = GuideSignatures.all(after)
        val watched = card.done.flatMap { it.split('|') }.filter { it.startsWith("changed:") }.map { it.removePrefix("changed:") }.toSet()
        val roles = after.guideTracks().associate { it.key to it.role }
        return seen + now.filter { (k, v) -> old[k] != v && k !in watched && roles[k] !in watched }
    }

    // --- the actions --------------------------------------------------------------------------------

    private fun tracksOf(p: Project, role: String): List<GuideTrack> {
        val all = p.guideTracks().filter { it.role == role }
        val song = GuideSong(p, all)
        return all.filter { song.plays(it.key) }.ifEmpty { all }
    }

    /** [a] on [p]: on the tracks of [keys] when given (an alert's own tracks), else on its track, else on the role's. Null: no room for a curve. */
    private fun perform(p: Project, card: GuideCard, a: GuideAction, keys: List<String>? = null): Project? {
        val role = a.target.ifEmpty { card.target }
        val targets = keys?.let { ks -> p.guideTracks().filter { it.key in ks } }
            ?: a.track.takeIf { it.isNotEmpty() }?.let { k -> p.guideTracks().filter { it.key == k } }
            ?: tracksOf(p, role).let { ts ->
                // Audit final: the bass's settings go to the Basse 2 too (the bass of the later Drops in bass music).
                if (role == GuideRoles.BASS && a.type in STRIP) ts + p.guideTracks().filter { it.role == GuideRoles.BASS2 } else ts
            }
        return when (a.type) {
            GuideAction.SLOT -> slot(p, a.track, a.param, (a.value ?: 1f) >= 0.5f)
            GuideAction.SUBTRACT -> GuideMoves.subtract(p, a.name)
            GuideAction.CURVE -> GuideMoves.curve(p, a.name, role)
            GuideAction.FILL -> GuideMoves.fills(p)
            GuideAction.BALANCE -> GuideMoves.balance(p)
            GuideAction.MONO -> GuideMoves.mono(p)
            GuideAction.INSERT, GuideAction.NUDGE, GuideAction.CHAIN, GuideAction.BYPASS, GuideAction.SEND, GuideAction.VOLUME ->
                targets.fold(p) { q, t -> onStrip(q, t, a) }
            GuideAction.PAN -> targets.fold(p) { q, t -> withPan(q, t, a.value ?: 0f) }
            GuideAction.TARGET -> runCatching { LoudnessTarget.valueOf(a.name) }.getOrNull()?.let { p.copy(mixer = p.mixer.copy(target = it)) } ?: p
            GuideAction.DUCK -> duck(p, a.value ?: 0.55f)
            GuideAction.LOW_CUT -> lowCuts(p)
            GuideAction.PANS -> pans(p)
            GuideAction.RETURNS -> p.copy(mixer = p.mixer.copy(buses = p.mixer.buses.map { b ->
                b.toMutableList().also { it[Buses.LOW_CUT] = Buses.lowCutValue(200f); it[Buses.HIGH_CUT] = Buses.highCutValue(10_000f) }
            }))
            GuideAction.MUTE -> mute(p, role, a)
            GuideAction.COPY_CHAIN -> copyChain(p, role, a.to)
            GuideAction.UNSOLO -> p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.map { if (it.solo) it.copy(solo = false) else it }))
            else -> p
        }
    }

    /** Effect [slot] of track [track] (the master's too) set aside ([off]) or heard again, its settings kept. */
    private fun slot(p: Project, track: String, slot: Int, off: Boolean): Project {
        val m = p.mixer
        if (track == GuideTrack.MASTER) {
            val ins = m.master.getOrNull(slot)?.takeIf { it.effect != EffectType.NONE } ?: return p
            return p.copy(mixer = m.copy(master = m.master.toMutableList().also { it[slot] = ins.copy(bypass = off) }))
        }
        val t = p.guideTracks().firstOrNull { it.key == track } ?: return p
        val c = strip(p, t) ?: return p
        val ins = c.inserts.getOrNull(slot)?.takeIf { it.effect != EffectType.NONE } ?: return p
        return withStrip(p, t.channel, c.copy(inserts = c.inserts.toMutableList().also { it[slot] = ins.copy(bypass = off) }))
    }

    private fun strip(p: Project, t: GuideTrack): Channel? = p.mixer.channels.getOrNull(t.channel)?.takeIf { t.channel != Mixer.MASTER }

    private fun withStrip(p: Project, ch: Int, c: Channel): Project =
        p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, old -> if (i == ch) c.sanitized() else old }))

    private fun onStrip(p: Project, t: GuideTrack, a: GuideAction): Project {
        val c = strip(p, t) ?: return p
        val next = when (a.type) {
            GuideAction.INSERT -> c.copy(inserts = withInsert(c.inserts, a))
            GuideAction.CHAIN -> c.copy(inserts = a.inserts.fold(c.inserts) { list, sub -> withInsert(list, sub) })
            GuideAction.NUDGE -> {
                val type = effect(a.effect) ?: return p
                val i = c.inserts.indexOfFirst { it.effect == type }
                if (i >= 0) c.copy(inserts = c.inserts.toMutableList().also { list ->
                    val ins = list[i]
                    list[i] = ins.copy(params = ins.params.mapIndexed { k, v -> if (k == a.param) (v + (a.delta ?: 0f)).coerceIn(0f, 1f) else v }, bypass = false)
                }) else if (a.params.isNotEmpty()) c.copy(inserts = withInsert(c.inserts, a)) else c
            }
            GuideAction.BYPASS -> {
                val type = effect(a.effect) ?: return p
                c.copy(inserts = c.inserts.map { if (it.effect == type) it.copy(bypass = (a.value ?: 1f) >= 0.5f) else it })
            }
            GuideAction.SEND -> {
                // Audit final: « Plus d'écho / Plus sec » on a track whose echo is its own (an echo in its chain, the
                // send nearly silent): that echo's mix moves, the one heard.
                val echo = if (a.bus == ECHO_BUS && a.value == null && a.delta != null) {
                    c.inserts.indexOfFirst { !it.bypass && (it.effect == EffectType.ECHO || it.effect == EffectType.PING_PONG) }
                } else -1
                if (echo >= 0) c.copy(inserts = c.inserts.toMutableList().also { list ->
                    val ins = list[echo]
                    list[echo] = ins.copy(params = ins.params.mapIndexed { k, v -> if (k == ECHO_MIX) (v + a.delta!!).coerceIn(0f, 1f) else v })
                })
                else c.copy(sends = c.sends.mapIndexed { b, v ->
                    if (b != a.bus) v else (a.value ?: (v + (a.delta ?: 0f))).coerceIn(0f, 1f)
                })
            }
            GuideAction.VOLUME -> {
                val db = 40f * log10((c.volume / 0.8f).coerceAtLeast(1e-4f)) + (a.delta ?: 0f)
                c.copy(volume = (0.8f * 10f.pow(db / 40f)).coerceIn(0f, 1f))
            }
            else -> c
        }
        return if (next == c) p else withStrip(p, t.channel, next)
    }

    /** The actions on a track's strip (the bass's reach its Basse 2 too). */
    private val STRIP = setOf(GuideAction.INSERT, GuideAction.NUDGE, GuideAction.CHAIN, GuideAction.BYPASS, GuideAction.SEND, GuideAction.VOLUME)

    /** The echo send bus, and the « Mélange » knob of an echo insert ([EffectType.ECHO], [EffectType.PING_PONG]). */
    private const val ECHO_BUS = 1
    private const val ECHO_MIX = 3

    private fun effect(name: String): EffectType? = EffectType.entries.firstOrNull { it.name == name && it != EffectType.NONE }

    /** [a]'s effect with its settings: on the first one of that type there, else in a free slot (none free: unchanged). */
    private fun withInsert(inserts: List<Insert>, a: GuideAction): List<Insert> {
        val type = effect(a.effect) ?: return inserts
        val params = a.params + type.defaults.drop(a.params.size)
        val same = inserts.indexOfFirst { it.effect == type }
        if (same >= 0) return inserts.toMutableList().also { it[same] = it[same].copy(params = params, bypass = false) }
        val slot = freeInsertSlot(inserts) ?: return inserts
        return inserts.toMutableList().also { it[slot] = Insert(type.ordinal, params).sanitized() }
    }

    private fun withPan(p: Project, t: GuideTrack, pan: Float): Project {
        if (t.key.startsWith("pad:")) return p.copy(drums = p.drums.withPadPan(t.key.removePrefix("pad:").toInt(), pan))
        val c = strip(p, t) ?: return p
        return withStrip(p, t.channel, c.copy(pan = pan.coerceIn(-1f, 1f)))
    }

    /** The Musique bus ducking under the kick, its lows by [low] (the bus made when the song has none). */
    private fun duck(p: Project, low: Float): Project {
        val kick = Mixer.padChannel(StarterWizard.Role.Kick.lane)
        val members = p.guideTracks().filter { (it.key.startsWith("inst:") && it.role != GuideRoles.SUB_DROP) || it.key == GuideTrack.ACID }.map { it.channel }.toSet()
        val bus = (p.mixer.musicBus ?: MusicBus(members = members, returns = true)).copy(lowDepth = low.coerceIn(0f, 0.8f), depth = (low * 0.3f).coerceIn(0f, 1f))
        return p.copy(mixer = p.mixer.copy(musicBus = bus.sanitized(), sidechainKey = if (p.mixer.sidechainKey >= 0) p.mixer.sidechainKey else kick))
    }

    /** The height of each role's low cut, in Hz (values the parcours' low cuts use). */
    private fun lowCutHz(role: String): Float = when (role) {
        GuideRoles.PAD -> 200f
        GuideRoles.LEAD, GuideRoles.COUNTER -> 250f
        GuideRoles.VOCAL, GuideRoles.AUDIO -> 120f
        else -> 150f
    }

    /** The parametric EQ's low cut knob for [hz] (expMap 20..500 Hz). */
    private fun lowCutKnob(hz: Float): Float = (kotlin.math.ln(hz / 20f) / kotlin.math.ln(25f)).coerceIn(0.01f, 1f)

    private fun lowCuts(p: Project): Project {
        val facts = GuideFacts.of(p)
        var q = p
        for (t in facts.tracks) {
            if (!(t.key.startsWith("inst:") || t.key == GuideTrack.ACID || t.key.startsWith("audio:")) || t.role in GuideRoles.LOW || !facts.song.plays(t.key) || facts.hasLowCut(t)) continue
            val c = strip(q, t) ?: continue
            val knob = lowCutKnob(lowCutHz(t.role))
            val eq = c.inserts.indexOfFirst { it.effect == EffectType.PARAM_EQ && !it.bypass }
            val inserts = if (eq >= 0) c.inserts.toMutableList().also { it[eq] = it[eq].copy(params = it[eq].params.mapIndexed { k, v -> if (k == 3) knob else v }) }
            else {
                // First in the chain: the cut before the compressor and the colour (the pro order).
                val free = c.inserts.indexOfFirst { it.effect == EffectType.NONE }
                if (free < 0) continue
                val cut = Insert(EffectType.PARAM_EQ.ordinal, listOf(0.5f, 0.5f, 0.4f, knob)).sanitized()
                (listOf(cut) + c.inserts.filterIndexed { i, _ -> i != free }).take(Mixer.INSERTS)
            }
            q = withStrip(q, t.channel, c.copy(inserts = inserts))
            if (eq < 0) q = q.copy(automation = shiftedLanes(q, t.channel, c.inserts.indexOfFirst { it.effect == EffectType.NONE }))
        }
        return q
    }

    /** The effect lanes of strip [ch] after a new first slot (the ones before [free] move one down). */
    private fun shiftedLanes(p: Project, ch: Int, free: Int) = p.automation.map { l ->
        val t = l.target
        if (t.kind == AutoTarget.INSERT_PARAM && t.ch == ch && t.slot < free) l.copy(target = t.copy(slot = t.slot + 1)) else l
    }

    /** Each track at its place: the low end and the voice in the centre, the others a little apart, one side then the other. */
    private fun pans(p: Project): Project {
        val facts = GuideFacts.of(p)
        val placed = facts.tracks.filter {
            (it.key.startsWith("inst:") || it.key == GuideTrack.ACID || it.key.startsWith("audio:")) && facts.song.plays(it.key) &&
                it.role !in GuideRoles.LOW && it.role != GuideRoles.VOCAL && it.role != GuideRoles.RISER
        }
        var q = p
        placed.forEachIndexed { i, t ->
            val side = if (i % 2 == 0) 1f else -1f
            val width = PAN_STEPS.getOrElse(i / 2) { PAN_STEPS.last() } * if (t.role == GuideRoles.PAD) 0.5f else 1f
            q = withPan(q, t, side * width)
        }
        return q
    }

    private val PAN_STEPS = listOf(0.15f, 0.25f, 0.3f, 0.35f)

    /** [role]'s tracks out of the [GuideAction.half] of the [GuideAction.occurrence]-th section of kind [GuideAction.section] ("peak"). */
    private fun mute(p: Project, role: String, a: GuideAction): Project {
        val song = GuideSong(p)
        val kind = if (a.section.isEmpty() || a.section == "peak") GuideSection.Kind.PEAK else runCatching { GuideSection.Kind.valueOf(a.section.uppercase()) }.getOrNull() ?: return p
        val s = song.sectionsOf(kind).getOrNull(a.occurrence) ?: return p
        val keys = tracksOf(p, role).mapNotNull { GuideTrack.partKey(it.key) }.toSet()
        if (keys.isEmpty()) return p
        val mid = s.start + s.bars / 2
        val parts = s.parts.filter { i -> if (a.half == "second") song.partStarts[i] >= mid else song.partStarts[i] + song.partBars[i] <= mid }
        if (parts.isEmpty()) return p
        return p.copy(song = p.song.mapIndexed { i, part -> if (i in parts) part.copy(muted = part.muted + keys) else part })
    }

    private fun copyChain(p: Project, from: String, to: String): Project {
        val source = tracksOf(p, from).firstOrNull()?.let { strip(p, it) } ?: return p
        return tracksOf(p, to).fold(p) { q, t ->
            val c = strip(q, t) ?: return@fold q
            // The target's curves on effects that are no longer in their slot go (as the mixer does).
            withStrip(q, t.channel, c.copy(inserts = source.inserts, sends = source.sends)).withoutStaleInsertLanes(q)
        }
    }

    // --- G3: edits made by hand from a track's sheet ------------------------------------------------

    /**
     * An edit made by hand on a track (its layers from its sheet, a chain copied: [before] → [after])
     * kept in the per-track history like the Guide's own, setting by setting with the value before,
     * origin [GuideChange.USER], card [SHEET], one group: « Ramener » brings it back. [merge]: a drag
     * of a slider (one undo step per drag in the app), folded into the last line when that line is
     * the same track's layers settled the same way (no layer added or removed) and still what it left.
     * [after] unchanged when nothing the history keeps changed.
     */
    fun withTrackEdit(before: Project, after: Project, at: Long, merge: Boolean = false): Project {
        val state = after.guide
        val id = "c${state.nextChange}"
        val changes = changesBetween(before, after, SHEET, "", at, id, GuideChange.USER)
        if (changes.isEmpty()) return after
        val last = state.history.lastOrNull()
        val c = changes.singleOrNull()
        if (merge && c != null && last != null && c.field == LAYERS && last.field == LAYERS && last.track == c.track && last.origin == GuideChange.USER &&
            last.card == SHEET && !last.reverted && last.after == c.before && sameStack(last.before, last.after) && sameStack(c.before, c.after) &&
            state.history.count { it.id == last.id } == 1
        ) {
            return after.copy(guide = state.copy(history = state.history.dropLast(1) + last.copy(after = c.after, at = at)))
        }
        return after.copy(guide = state.withChanges(changes).copy(nextChange = state.nextChange + 1))
    }

    /** Two layer lists (as JSON) with the same sounds in the same order: one was only set, not added to or taken from. */
    private fun sameStack(a: JsonElement?, b: JsonElement?): Boolean = runCatching {
        val x = layersOf(a)
        val y = layersOf(b)
        x.size == y.size && x.indices.all { x[it].sound == y[it].sound && x[it].role == y[it].role }
    }.getOrDefault(false)

    /**
     * « Copier cette chaîne sur… » (the sheet's « Ailleurs ? »): the effects and sends of track [from]
     * ([GuideTrack.key]) on track [to], its curves on effects no longer in their slot gone (as the
     * mixer does), in the history ([withTrackEdit]). Unchanged with a [GuideOutcome.problem] when the
     * chain is already the same or the engine's long effects would be past their limit.
     */
    fun tryCopyChain(p: Project, from: String, to: String, at: Long): GuideOutcome {
        val tracks = p.guideTracks()
        val source = tracks.firstOrNull { it.key == from }?.let { strip(p, it) } ?: return GuideOutcome(p)
        val target = tracks.firstOrNull { it.key == to }?.takeIf { it.key != from } ?: return GuideOutcome(p)
        val c = strip(p, target) ?: return GuideOutcome(p)
        val edited = withStrip(p, target.channel, c.copy(inserts = source.inserts, sends = source.sends)).withoutStaleInsertLanes(p)
        if (edited == p) return GuideOutcome(p, tr("Cette piste a déjà la même chaîne."))
        edited.mixer.longFxOverLimit(p.mixer)?.let { return GuideOutcome(p, longFxLimitNotice(it)) }
        return GuideOutcome(withTrackEdit(p, edited, at))
    }

    private val layerList = ListSerializer(TrackLayer.serializer())

    /** Layers as the history keeps them: their defaults left out (a synth patch in full is ~1 kB). */
    private val compact = Json { ignoreUnknownKeys = true; encodeDefaults = false; coerceInputValues = true }

    /** A [LAYERS] value of the history as layers (none for null or what does not read). */
    fun layersOf(value: JsonElement?): List<TrackLayer> =
        runCatching { compact.decodeFromJsonElement(layerList, value ?: JsonArray(emptyList())) }.getOrDefault(emptyList())

    /** An effect slot's value of the history ([SLOT]) as its effect, or null. */
    fun insertOf(value: JsonElement?): Insert? = value?.let { runCatching { json.decodeFromJsonElement(Insert.serializer(), it) }.getOrNull() }

    /** The layers of instrument track [key] ("inst:3"), or null when there is no such track. */
    private fun Project.layersAt(key: String): List<TrackLayer>? {
        val id = key.takeIf { it.startsWith("inst:") }?.removePrefix("inst:")?.toIntOrNull() ?: return null
        if (id != 0 && tracks.none { it.id == id }) return null
        return track(id).layers
    }

    // --- the history: one entry per setting touched -------------------------------------------------

    /** What [card] changed between [before] and [after], setting by setting, with both values: group [id]. */
    private fun changesBetween(
        before: Project, after: Project, cardId: String, target: String, at: Long, id: String, origin: String = GuideChange.GUIDE,
    ): List<GuideChange> {
        val out = ArrayList<GuideChange>()
        fun add(track: String, kind: String, field: String) {
            val a = fieldValue(before, track, field)
            val b = fieldValue(after, track, field)
            if (a != b) out += GuideChange(track, kind, field, a, b, at, origin, cardId, id = id)
        }
        val tracks = (before.guideTracks() + after.guideTracks()).distinctBy { it.key }
        for (t in tracks) {
            val a = before.mixer.channels.getOrNull(t.channel)
            val b = after.mixer.channels.getOrNull(t.channel)
            if (t.channel != Mixer.MASTER && a != null && b != null && a != b) {
                for (i in 0 until Mixer.INSERTS) if (a.inserts.getOrNull(i) != b.inserts.getOrNull(i)) add(t.key, GuideChange.INSERT, "$SLOT$i")
                for (i in 0 until Buses.COUNT) add(t.key, GuideChange.VALUE, "$SEND$i")
                add(t.key, GuideChange.VALUE, PAN)
                add(t.key, GuideChange.VALUE, VOLUME)
                add(t.key, GuideChange.VALUE, SOLO)
            }
            if (t.key.startsWith("pad:")) add(t.key, GuideChange.VALUE, PAD_PAN)
            // G3: an instrument track's layers, as one setting (its stack, brought back whole).
            if (t.key.startsWith("inst:") && before.layersAt(t.key) != after.layersAt(t.key)) add(t.key, GuideChange.LAYER, LAYERS)
        }
        for (i in 0 until Mixer.INSERTS) if (before.mixer.master.getOrNull(i) != after.mixer.master.getOrNull(i)) add(GuideTrack.MASTER, GuideChange.INSERT, "$SLOT$i")
        add(GuideTrack.MASTER, GuideChange.VALUE, TARGET)
        add(GuideTrack.MASTER, GuideChange.VALUE, DUCKING)
        add(GuideTrack.MASTER, GuideChange.VALUE, MONO_BASS)
        for (b in 0 until Buses.COUNT) for (k in 0 until Buses.PARAM_COUNT) {
            if (before.mixer.buses.getOrNull(b)?.getOrNull(k) != after.mixer.buses.getOrNull(b)?.getOrNull(k)) add(GuideTrack.bus(b), GuideChange.VALUE, "$PARAM$k")
        }
        val onlyMutes = before.song.size == after.song.size && before.song.indices.all { before.song[it].copy(muted = emptySet()) == after.song[it].copy(muted = emptySet()) }
        if (before.song != after.song && onlyMutes) {
            val track = tracksOf(before, target).firstOrNull()?.key ?: GuideTrack.MASTER
            for (i in before.song.indices) if (before.song[i].muted != after.song[i].muted) add(track, GuideChange.PART, "$MUTED$i:${before.song[i].scene}")
        }
        // G4: parts added or cut (a Break made from the Drop, a fill): the parts that changed, as one setting.
        val reshaped = before.song != after.song && !onlyMutes
        if (reshaped) {
            val a = before.song
            val b = after.song
            var head = 0
            while (head < a.size && head < b.size && a[head] == b[head]) head++
            var tail = 0
            while (tail < a.size - head && tail < b.size - head && a[a.size - 1 - tail] == b[b.size - 1 - tail]) tail++
            out += GuideChange(GuideTrack.MASTER, GuideChange.PART, "$PARTS$head", parts(a.subList(head, a.size - tail)), parts(b.subList(head, b.size - tail)), at, origin, cardId, id = id)
        }
        // The scenes it made for them, each one taken back when the parts go.
        val sa = before.allScenes
        val sb = after.allScenes
        for (i in sb.indices) {
            val was = sa.getOrNull(i)
            if ((was == null || (was.isEmpty && was.label.isBlank())) && (!sb[i].isEmpty || sb[i].label.isNotBlank())) {
                out += GuideChange(GuideTrack.MASTER, GuideChange.PART, "$SCENE$i", null, json.encodeToJsonElement(Scene.serializer(), sb[i]), at, origin, cardId, id = id)
            }
        }
        // The curves follow the parts by themselves (their way back too): only a curve drawn is kept.
        if (before.automation != after.automation && !reshaped) {
            if (before.automation.size == after.automation.size) {
                for (i in before.automation.indices) {
                    val ta = before.automation[i].target
                    val tb = after.automation[i].target
                    if (ta != tb) out += GuideChange(GuideTrack.MASTER, GuideChange.LANE, LANE_TARGET, enc(ta), enc(tb), at, origin, cardId, id = id)
                }
            }
            val targetsMoved = before.automation.size == after.automation.size && before.automation.indices.any { before.automation[it].target != after.automation[it].target }
            if (!targetsMoved) {
                val kept = after.automation.map { it.target }.toSet()
                for (l in before.automation) if (l.target !in kept) {
                    out += GuideChange(GuideTrack.MASTER, GuideChange.LANE, LANE, json.encodeToJsonElement(AutomationLane.serializer(), l), null, at, origin, cardId, id = id)
                }
                // G4: a curve drawn or changed by the Guide (a filter that opens, a throw): its value before (none: it was not there).
                for (l in after.automation) {
                    val was = before.automation.firstOrNull { it.target == l.target }
                    if (was != l) {
                        out += GuideChange(
                            GuideTrack.MASTER, GuideChange.LANE, LANE_SET, was?.let { json.encodeToJsonElement(AutomationLane.serializer(), it) },
                            json.encodeToJsonElement(AutomationLane.serializer(), l), at, origin, cardId, id = id,
                        )
                    }
                }
            }
        }
        return out
    }

    private val partList = ListSerializer(SongPart.serializer())

    private fun parts(list: List<SongPart>): JsonElement = json.encodeToJsonElement(partList, list)

    private fun enc(t: AutoTarget): JsonElement = json.encodeToJsonElement(AutoTarget.serializer(), t)

    private fun lane(e: JsonElement): AutoTarget = json.decodeFromJsonElement(AutoTarget.serializer(), e)

    /** Change [c]'s setting is still what the Guide left there (nothing changed it since). */
    private fun stillAfter(p: Project, c: GuideChange): Boolean = runCatching {
        when (c.field) {
            LANE_TARGET -> p.automation.any { it.target == lane(c.after!!) }
            LANE -> json.decodeFromJsonElement(AutomationLane.serializer(), c.before!!).target.let { t -> p.automation.none { it.target == t } }
            LANE_SET -> json.decodeFromJsonElement(AutomationLane.serializer(), c.after!!).let { l -> p.automation.firstOrNull { it.target == l.target } == l }
            else -> if (c.field.startsWith(PARTS)) {
                val at = c.field.removePrefix(PARTS).toInt()
                val now = json.decodeFromJsonElement(partList, c.after!!)
                at + now.size <= p.song.size && p.song.subList(at, at + now.size) == now
            } else if (c.field.startsWith(SCENE)) {
                val i = c.field.removePrefix(SCENE).toInt()
                p.scene != i && p.allScenes.getOrNull(i) == json.decodeFromJsonElement(Scene.serializer(), c.after!!)
            } else fieldValue(p, c.track, c.field) == c.after
        }
    }.getOrDefault(false)

    /** The value of setting [field] of track [track] in [p] as JSON (null: none, the track is not there). */
    internal fun fieldValue(p: Project, track: String, field: String): JsonElement? {
        val m = p.mixer
        when {
            track == GuideTrack.MASTER && field.startsWith(SLOT) -> return m.master.getOrNull(field.removePrefix(SLOT).toInt())?.let { json.encodeToJsonElement(Insert.serializer(), it) }
            field == TARGET -> return JsonPrimitive(m.target.name)
            field == DUCKING -> return json.encodeToJsonElement(Ducking.serializer(), Ducking(m.musicBus, m.sidechainKey))
            field == MONO_BASS -> return JsonPrimitive(m.monoBassHz)
            field.startsWith(PARTS) || field.startsWith(SCENE) || field == LANE_SET -> return null  // see [stillAfter], [tryRevert]
            track.startsWith("bus:") -> return m.buses.getOrNull(track.removePrefix("bus:").toInt())?.getOrNull(field.removePrefix(PARAM).toInt())?.let(::JsonPrimitive)
            field.startsWith(MUTED) -> {
                val (i, scene) = field.removePrefix(MUTED).split(':').map { it.toInt() }
                val part = p.song.getOrNull(i)?.takeIf { it.scene == scene } ?: return null
                return JsonArray(part.muted.sorted().map(::JsonPrimitive))
            }
            field == LANE_TARGET || field == LANE -> return null  // the curves: see [stillAfter]
            field == PAD_PAN -> return p.drums.lanes.getOrNull(track.removePrefix("pad:").toInt())?.pan?.let(::JsonPrimitive)
            field == LAYERS -> return p.layersAt(track)?.let { compact.encodeToJsonElement(layerList, it) }
        }
        if (track.startsWith("inst:") && p.guideTracks().none { it.key == track }) return null
        val c = m.channels.getOrNull(GuideTrack.channelOf(track) ?: return null) ?: return null
        return when {
            field.startsWith(SLOT) -> c.inserts.getOrNull(field.removePrefix(SLOT).toInt())?.let { json.encodeToJsonElement(Insert.serializer(), it) }
            field.startsWith(SEND) -> c.sends.getOrNull(field.removePrefix(SEND).toInt())?.let(::JsonPrimitive)
            field == PAN -> JsonPrimitive(c.pan)
            field == VOLUME -> JsonPrimitive(c.volume)
            field == SOLO -> JsonPrimitive(c.solo)
            else -> null
        }
    }

    /** [p] with setting [field] of [track] at [value]; null when it cannot be (the track or the part is gone). */
    private fun withField(p: Project, track: String, field: String, value: JsonElement?): Project? = runCatching {
        val m = p.mixer
        fun float() = (value as JsonPrimitive).float
        when {
            track == GuideTrack.MASTER && field.startsWith(SLOT) -> {
                val i = field.removePrefix(SLOT).toInt()
                p.copy(mixer = m.copy(master = m.master.toMutableList().also { it[i] = json.decodeFromJsonElement(Insert.serializer(), value!!) }))
            }
            field == TARGET -> p.copy(mixer = m.copy(target = LoudnessTarget.valueOf((value as JsonPrimitive).content)))
            field == DUCKING -> json.decodeFromJsonElement(Ducking.serializer(), value!!).let { d -> p.copy(mixer = m.copy(musicBus = d.bus, sidechainKey = d.key)) }
            field == MONO_BASS -> p.copy(mixer = m.copy(monoBassHz = float()))
            track.startsWith("bus:") -> {
                val b = track.removePrefix("bus:").toInt()
                val k = field.removePrefix(PARAM).toInt()
                p.copy(mixer = m.copy(buses = m.buses.mapIndexed { i, list -> if (i == b) list.toMutableList().also { it[k] = float() } else list }))
            }
            field.startsWith(MUTED) -> {
                val i = field.removePrefix(MUTED).substringBefore(':').toInt()
                val muted = (value as JsonArray).map { (it as JsonPrimitive).content }.toSet()
                p.copy(song = p.song.mapIndexed { j, part -> if (j == i) part.copy(muted = muted) else part })
            }
            field == PAD_PAN -> p.copy(drums = p.drums.withPadPan(track.removePrefix("pad:").toInt(), float()))
            field == LAYERS -> {
                p.layersAt(track) ?: return null
                val t = p.track(track.removePrefix("inst:").toInt())
                p.withTrack(t.copy(layers = Layers.sanitize(compact.decodeFromJsonElement(layerList, value ?: JsonArray(emptyList())))))
            }
            else -> {
                val ch = GuideTrack.channelOf(track) ?: return null
                if (track.startsWith("inst:") && p.guideTracks().none { it.key == track }) return null
                val c = m.channels.getOrNull(ch) ?: return null
                val next = when {
                    field.startsWith(SLOT) -> c.copy(inserts = c.inserts.toMutableList().also { it[field.removePrefix(SLOT).toInt()] = json.decodeFromJsonElement(Insert.serializer(), value!!) })
                    field.startsWith(SEND) -> c.copy(sends = c.sends.toMutableList().also { it[field.removePrefix(SEND).toInt()] = float() })
                    field == PAN -> c.copy(pan = float())
                    field == VOLUME -> c.copy(volume = float())
                    field == SOLO -> c.copy(solo = (value as JsonPrimitive).boolean)
                    else -> return null
                }
                withStrip(p, ch, next)
            }
        }
    }.getOrNull()

    /** The Musique bus and the sidechain's key, kept together in a change. */
    @kotlinx.serialization.Serializable
    private data class Ducking(val bus: MusicBus? = null, val key: Int = -1)

    const val SLOT = "slot:"
    const val SEND = "send:"
    const val PAN = "pan"
    const val VOLUME = "volume"
    const val SOLO = "solo"
    const val PAD_PAN = "padPan"
    const val TARGET = "target"
    const val DUCKING = "ducking"
    const val PARAM = "param:"
    const val MUTED = "muted:"
    const val LANE_TARGET = "laneTarget"
    const val LANE = "lane"
    /** G4: a curve as the Guide drew it (its value before: none, or the curve it replaced). */
    const val LANE_SET = "laneSet"
    /** G4: the song's parts from that place on ("parts:3"): those that were there, those the Guide put. */
    const val PARTS = "parts:"
    /** G4: a scene the Guide made ("scene:7"), emptied again when its parts go. */
    const val SCENE = "scene:"
    /** G4: the master's mono below a frequency ([Mixer.monoBassHz]). */
    const val MONO_BASS = "monoBass"
    /** G3: an instrument track's layers ([com.pocketstudio.model.InstrumentTrack.layers]), as one setting. */
    const val LAYERS = "layers"
    /** The « card » of the changes made by hand from a track's sheet ([withTrackEdit]): no card of the parcours. */
    const val SHEET = "sheet"
}

/** What an action did: the project after it, or the same project and why not ([problem], shown to the user). */
data class GuideOutcome(val project: Project, val problem: String? = null)
