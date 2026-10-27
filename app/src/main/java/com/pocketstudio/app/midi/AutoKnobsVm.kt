package com.pocketstudio.app.midi

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketstudio.app.mixer.StripInfo
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.Project
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.midi.Control
import com.pocketstudio.model.midi.ControlButton
import com.pocketstudio.model.midi.ControllerProfile
import com.pocketstudio.model.midi.Controllers
import com.pocketstudio.model.midi.KnobLayout
import com.pocketstudio.model.midi.KnobModeGuess
import com.pocketstudio.model.midi.KnobSettings
import com.pocketstudio.model.midi.SoftTakeover
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/** What a button, the main encoder or a pad of the controller asks of the open project. */
sealed interface ControllerAction {
    /** ▶ / ■ ([live]: on the Live page, ▶ plays the scene shown, as its own ▶ does). */
    data class TogglePlay(val live: Boolean) : ControllerAction
    data object Stop : ControllerAction
    /** ● on the Live page: the performance take. */
    data object Record : ControllerAction
    data class Tempo(val delta: Int) : ControllerAction
    /** The instrument track the keys play: the next ([delta] > 0) or the one before. */
    data class Track(val delta: Int) : ControllerAction
    /** A row of the Live grid (a whole scene), from the next launch line. */
    data class LaunchRow(val row: Int) : ControllerAction
    /** One pad of the Live grid: column [column] (its key) in row [row]. */
    data class LaunchCell(val column: String, val row: Int) : ControllerAction
    /** M of the Live grid's column [column] (0: the first). */
    data class Mute(val column: Int) : ControllerAction
    /** A live pad (sample pads of the Live page). */
    data class LivePad(val pad: Int) : ControllerAction
}

/** What the knobs read and move: the open project, the selected track, the Live page's knobs. */
interface KnobHost {
    val project: Project
    val track: Int
    val live: LiveKnobs
    val strips: List<StripInfo>
    fun apply(t: KnobTarget, value: Float)
    /** Does [a]; the words to flash on screen (« Tempo 124 », « Ligne B »), null when nothing was done. */
    fun act(a: ControllerAction): String? = null
}

/**
 * The small hint on screen: the [screen]'s eight knobs ([slots], with their [values] and the CC
 * each answers to), the one just turned ([touched]) and, while it has not picked up its setting
 * yet, which way to turn it ([turn]: +1 up, −1 down, 0 picked up). [learning]: « Apprendre »
 * waits for a knob to be turned for that place. [faders]: the controller's faders instead.
 */
data class KnobHint(
    val screen: KnobScreen,
    val slots: List<KnobSlot>,
    val values: List<Float>,
    val ccs: List<Int?> = emptyList(),
    val touched: Int = -1,
    val turn: Int = 0,
    val learning: Int? = null,
    val device: String? = null,
    val faders: Boolean = false,
)

/**
 * « Potards automatiques » (phase 39), grown into the controller's whole map (MiniLab lot): the
 * first eight knobs of a MIDI controller move the eight most useful settings of the screen shown
 * ([KnobPages]), each with soft takeover when it is absolute (it only moves its setting once it has
 * reached it: no jump) or by steps when it is relative; its faders move the first tracks' volumes
 * (soft takeover too); its main encoder, its click and its transport buttons, and its pads on the
 * Live page, do what [ControllerAction] says for the screen. MIDI learn mappings come first (the
 * app's caller). The values go through the same calls as the screens' own knobs (the engine's
 * command queue). Pure logic: [KnobsViewModel] keeps the settings and shows the hint.
 */
class AutoKnobs(private val host: KnobHost, settings: KnobSettings = KnobSettings()) {
    var settings: KnobSettings = settings
        private set
    var screen: KnobScreen = KnobScreen.Mixeur
    /** The connected controller's profile, as the phone's USB ids said (null: found from its name, else generic). */
    var profile: ControllerProfile? = null
    /** The profile in use: [profile], else the one [device]'s name gives. */
    private val active: ControllerProfile? get() = profile ?: Controllers.detect(device)
    /** « Apprendre »: the next knob turned becomes knob [learning] (null: not learning). */
    var learning: Int? = null
    /** « Apprendre » of a fader or a button: the next control change moved becomes it. */
    var learningControl: Control? = null
    private val takeovers = Array(KnobLayout.SLOTS) { SoftTakeover() }
    private val faderTakeovers = Array(Controllers.FADERS) { SoftTakeover() }
    private val guess = KnobModeGuess()
    var last: KnobHint? = null
        private set
    /** The words of the last button, encoder or pad (« Tempo 124 »), for a moment on screen. */
    var flash: String? = null
        private set

    /** The controller's name (its settings are kept under it). */
    var device: String? = null

    /** How many faders the controller has (its profile's, or as many as were learnt). */
    fun faderCount(device: String? = this.device): Int =
        maxOf((profile ?: Controllers.detect(device))?.faders?.size ?: 0, device?.let { settings.faders[it]?.keys?.maxOrNull()?.plus(1) } ?: 0)

    /** The knobs of the screen shown, as the user set them. */
    fun slots(): List<KnobSlot> =
        KnobPages.resolved(screen, host.project, host.track, host.strips, settings.overrides[screen.name].orEmpty(), faderCount())

    /** What the faders move: the first tracks' volumes, as many as the controller has (four at least). */
    fun faderSlots(): List<KnobSlot> = KnobPages.faders(host.strips).take(faderCount().coerceAtLeast(4))

    fun hint(touched: Int = -1, turn: Int = 0, device: String? = this.device): KnobHint {
        val slots = slots()
        val p = host.project
        val live = host.live
        return KnobHint(
            screen, slots, slots.map { KnobPages.valueOf(it.target, p, live) },
            ccs = List(slots.size) { KnobLayout.ccOf(it, device, settings, profile ?: Controllers.detect(device)) },
            touched = touched, turn = turn, learning = learning, device = device,
        ).also { last = it }
    }

    private fun faderHint(touched: Int, turn: Int, device: String?): KnobHint {
        val slots = faderSlots()
        return KnobHint(
            screen, slots, slots.map { KnobPages.valueOf(it.target, host.project, host.live) },
            ccs = List(slots.size) { i -> device?.let { settings.faders[it]?.get(i) } ?: active?.faders?.getOrNull(i) },
            touched = touched, turn = turn, device = device, faders = true,
        ).also { last = it }
    }

    /**
     * Control change [cc] (on [channel]) of [device] at [value] (0..127). True when it was taken (a
     * knob, a fader, a button, or learnt now); false when it is none of the controller's (the caller
     * leaves it alone).
     */
    fun control(cc: Int, value: Int, device: String, channel: Int = 0): Boolean {
        this.device = device
        flash = null
        if (!settings.enabled) return false
        learning?.let { slot ->
            learning = null
            settings = settings.withKnob(device, slot, cc)
            takeovers[slot] = SoftTakeover()
            hint(touched = slot, device = device)
            return true
        }
        learningControl?.let { c ->
            learningControl = null
            settings = when (c) {
                is Control.Fader -> settings.withFader(device, c.index, cc).also { faderTakeovers[c.index] = SoftTakeover() }
                is Control.Button -> settings.withButton(device, c.button, cc)
                is Control.Knob -> settings.withKnob(device, c.index, cc)
            }
            flash = tr("Appris : CC %d", cc)
            return true
        }
        val (control, next) = Controllers.controlOf(cc, channel, device, active, settings) ?: return false
        settings = next
        return when (control) {
            is Control.Knob -> knob(control, cc, value, device)
            is Control.Fader -> fader(control.index, value, device)
            is Control.Button -> button(control, value)
        }
    }

    private fun knob(control: Control.Knob, cc: Int, value: Int, device: String): Boolean {
        var mode = control.mode
        // A controller the app does not know: its knobs may be endless (relative); found from what they send.
        if (active == null && settings.modeOf(device) == null) {
            guess.feed(cc, value)?.takeIf { it.relative }?.let { found ->
                settings = settings.withMode(device, found)
                mode = found
            }
        }
        val slot = control.index
        val target = slots().getOrNull(slot) ?: return true
        val current = KnobPages.valueOf(target.target, host.project, host.live)
        val delta = mode.delta(value)
        if (delta != null) {
            // Endless knob: a step a click, never a jump.
            if (delta != 0) host.apply(target.target, (current + delta * STEP).coerceIn(0f, 1f))
            takeovers[slot].follow(target.target.key)
            hint(touched = slot, device = device)
            return true
        }
        val t = takeovers[slot]
        t.follow(target.target.key)
        t.move(value / 127f, current)?.let { host.apply(target.target, it) }
        hint(touched = slot, turn = t.direction(current), device = device)
        return true
    }

    private fun fader(index: Int, value: Int, device: String): Boolean {
        val target = faderSlots().getOrNull(index) ?: return true
        val t = faderTakeovers[index]
        t.follow(target.target.key)
        val current = KnobPages.valueOf(target.target, host.project, host.live)
        t.move(value / 127f, current)?.let { host.apply(target.target, it) }
        faderHint(index, t.direction(current), device)
        return true
    }

    private fun button(control: Control.Button, value: Int): Boolean {
        val pressed = value >= 64
        val action: ControllerAction? = when (control.button) {
            ControlButton.ENCODER, ControlButton.ENCODER_SHIFT -> {
                val step = (control.mode.delta(value) ?: 0).coerceIn(-4, 4)
                when {
                    step == 0 -> null
                    control.button == ControlButton.ENCODER_SHIFT || screen.isLive -> ControllerAction.Tempo(step)
                    else -> ControllerAction.Track(if (step > 0) 1 else -1)
                }
            }
            ControlButton.CLICK, ControlButton.PLAY -> if (pressed) ControllerAction.TogglePlay(screen.isLive) else null
            ControlButton.CLICK_SHIFT, ControlButton.STOP -> if (pressed) ControllerAction.Stop else null
            ControlButton.RECORD -> if (pressed && screen.isLive) ControllerAction.Record else null
            ControlButton.SHIFT -> null
        }
        if (action != null) flash = host.act(action)
        return true
    }

    /**
     * Pad [index] (0..15, bank B from 8) hit while the pads do not play the drums: on the Live page
     * the first eight launch the rows (the scenes), the next eight mute or bring back the columns;
     * with the 303 panel, the first eight launch the 303's line of each row; with the live pads,
     * each pad its live pad. Returns the action done, if any.
     */
    fun pad(index: Int): ControllerAction? {
        flash = null
        val action = when (screen) {
            KnobScreen.LivePads -> ControllerAction.LivePad(index)
            KnobScreen.LiveAcide -> if (index < 8) ControllerAction.LaunchCell(SongPart.ACID, index) else ControllerAction.Mute(index - 8)
            KnobScreen.Live -> if (index < 8) ControllerAction.LaunchRow(index) else ControllerAction.Mute(index - 8)
            else -> null
        } ?: return null
        flash = host.act(action)
        return action
    }

    fun activeProfile(): ControllerProfile? = active

    /** « Changer »: knob [slot] of this screen moves [target] from now on (null: the app's choice again). */
    fun override(slot: Int, target: KnobTarget?) {
        settings = settings.withOverride(screen.name, slot, target?.key)
        takeovers[slot] = SoftTakeover()
    }

    fun setEnabled(on: Boolean) {
        settings = settings.copy(enabled = on)
    }

    fun setLegend(on: Boolean) {
        settings = settings.copy(legend = on)
    }

    /** « Apprendre mes pads »: [pads] (packed notes) are [device]'s pads 1, 2… from now on (empty: its own again). */
    fun setPads(device: String, pads: List<Int>) {
        settings = settings.withPads(device, pads)
    }

    /** « Oublier mes réglages » for [device]. */
    fun forget(device: String) {
        settings = settings.forget(device)
        guess.reset()
        takeovers.indices.forEach { takeovers[it] = SoftTakeover() }
        faderTakeovers.indices.forEach { faderTakeovers[it] = SoftTakeover() }
    }

    /** « Remettre comme au départ » for this screen and [device]. */
    fun reset(device: String?) {
        settings = settings.reset(screen.name, device)
        takeovers.indices.forEach { takeovers[it] = SoftTakeover() }
    }

    companion object {
        /** An endless knob's click: a hundredth of the setting (100 clicks from one end to the other). */
        const val STEP = 1f / 100f
    }
}

/** The knobs' host on the open project: values read from it, moves sent through its own setters. */
fun rhythmKnobHost(vm: RhythmViewModel): KnobHost = object : KnobHost {
    override val project: Project get() = vm.project.value
    override val track: Int get() = vm.selectedTrack.value
    override val live: LiveKnobs get() = LiveKnobs(vm.liveFx.value, vm.liveReturns.value, vm.liveAcid.value)
    override val strips: List<StripInfo> get() = com.pocketstudio.app.ui.mixerStrips(vm.project.value)
    override fun apply(t: KnobTarget, value: Float) {
        val a = t.target
        when (a.kind) {
            AutoTarget.ACID -> if (t.live) vm.setLiveAcid(a.param, value) else vm.setAcidSound(a.param, value)
            AutoTarget.SYNTH -> if (a.track == vm.selectedTrack.value) vm.setSynthParam(a.param, value)
            AutoTarget.PLAITS -> if (a.track == vm.selectedTrack.value) vm.setPlaitsParam(a.param, value)
            AutoTarget.VOLUME -> vm.setChannelVolume(a.ch, value)
            AutoTarget.PAN -> vm.setChannelPan(a.ch, value * 2f - 1f)
            AutoTarget.SEND -> vm.setSend(a.ch, a.slot, value)
            AutoTarget.MACRO -> vm.setMacro(a.ch, a.param, value)
            AutoTarget.LIVE -> when (a.param) {
                AutoTarget.LIVE_REVERB_RETURN -> vm.setLiveReturn(0, value)
                AutoTarget.LIVE_ECHO_RETURN -> vm.setLiveReturn(1, value)
                else -> vm.livePerformance(a.param, value)
            }
        }
    }

    override fun act(a: ControllerAction): String? {
        val p = vm.project.value
        val playing = vm.transport.value.playing
        return when (a) {
            is ControllerAction.TogglePlay -> {
                // ▶ on the Live page plays what the grid shows (the scene), as its own ▶.
                if (a.live && !playing) vm.launchRow(p.scene) else vm.togglePlay()
                if (playing) tr("Arrêt") else tr("Lecture")
            }
            ControllerAction.Stop -> if (playing) { vm.togglePlay(); tr("Arrêt") } else null
            ControllerAction.Record -> { vm.togglePerformance(); tr("Enregistrer") }
            is ControllerAction.Tempo -> {
                vm.nudgeTempo(a.delta)
                tr("Tempo %d", Math.round(vm.project.value.tempoBpm).toInt())
            }
            is ControllerAction.Track -> {
                val tracks = p.instrumentTracks
                val at = tracks.indexOfFirst { it.id == vm.selectedTrack.value }.coerceAtLeast(0)
                val next = tracks.getOrNull(Math.floorMod(at + a.delta, tracks.size)) ?: return null
                vm.selectTrack(next.id)
                tr("Piste : %s", com.pocketstudio.model.DefaultNames.shown(next.name))
            }
            is ControllerAction.LaunchRow -> {
                if (a.row !in p.allScenes.indices) return null
                vm.launchRow(a.row)
                tr("Ligne %s", ('A' + a.row).toString())
            }
            is ControllerAction.LaunchCell -> {
                if (a.row !in p.allScenes.indices) return null
                vm.launchPad(a.column, a.row)
                tr("Ligne %s", ('A' + a.row).toString())
            }
            is ControllerAction.Mute -> {
                val c = com.pocketstudio.app.live.liveColumnsOf(p).getOrNull(a.column) ?: return null
                vm.toggleLiveMute(c.key)
                if (c.key in vm.liveMuted.value) tr("%s coupé", c.name) else tr("%s rallumé", c.name)
            }
            is ControllerAction.LivePad -> {
                if (p.live.clips.getOrNull(a.pad) == null) return null
                vm.toggleLive(a.pad)
                tr("Pad live %d", a.pad + 1)
            }
        }
    }
}

/**
 * Keeps [AutoKnobs] for the app: its settings in SharedPreferences "midi_knobs" (the same for
 * every project), the screen shown, and the hint, shown for a few seconds after a knob turns.
 */
class KnobsViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private var knobs: AutoKnobs? = null
    private val _hint = MutableStateFlow<KnobHint?>(null)
    /** The hint while it shows (null: hidden). */
    val hint: StateFlow<KnobHint?> = _hint.asStateFlow()
    private val _enabled = MutableStateFlow(load().enabled)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()
    private val _settings = MutableStateFlow(load())
    /** The controllers' settings as saved (the legend, each device's own choices, its pads). */
    val settings: StateFlow<KnobSettings> = _settings.asStateFlow()
    private val _screen = MutableStateFlow(KnobScreen.Mixeur)
    /** The screen the controller follows now. */
    val screenShown: StateFlow<KnobScreen> = _screen.asStateFlow()
    private val _flash = MutableStateFlow<String?>(null)
    /** What a button, the encoder or a pad just did (« Tempo 124 »), for a moment. */
    val flash: StateFlow<String?> = _flash.asStateFlow()
    private val _learningControl = MutableStateFlow<Control?>(null)
    /** « Apprendre » of a fader or a button, waiting for it to be moved. */
    val learningControl: StateFlow<Control?> = _learningControl.asStateFlow()
    private var hide: Job? = null
    private var hideFlash: Job? = null
    /** The sheet « Potards du contrôleur » is open: the hint stays. */
    var pinned = false
        private set

    private var host: KnobHost? = null

    /** The open project's knobs (once: the ViewModel lives with the project's screen). */
    fun attach(host: KnobHost) {
        if (knobs != null) return
        this.host = host
        knobs = AutoKnobs(host, load()).also { it.screen = _screen.value }
    }

    var screen: KnobScreen
        get() = knobs?.screen ?: _screen.value
        set(value) {
            _screen.value = value
            val k = knobs ?: return
            if (k.screen == value) return
            k.screen = value
            val h = _hint.value
            if (h != null && !h.faders) _hint.value = k.hint(device = h.device)
        }

    /** The connected controller ([name] null: none) and its profile (null: found from its name, else generic). */
    fun setController(name: String?, profile: ControllerProfile?) {
        val k = knobs ?: return
        k.profile = profile
        if (name != null) k.device = name
    }

    /** A CC no MIDI learn took: one of the controller's controls, or nothing. [device]: the controller's name. */
    fun control(cc: Int, value: Int, device: String?, channel: Int = 0): Boolean {
        val k = knobs ?: return false
        val before = k.settings
        val wasLearning = k.learningControl != null
        val taken = k.control(cc, value, device ?: UNKNOWN, channel)
        if (k.settings != before) save(k.settings)
        if (wasLearning) _learningControl.value = k.learningControl
        val f = k.flash
        if (f != null) flash(f) else if (taken) show(k.last)
        return taken
    }

    /** A pad on the Live page (the MIDI thread left it to the app): its launch, mute or live pad. */
    fun pad(index: Int): ControllerAction? {
        val k = knobs ?: return null
        val a = k.pad(index)
        k.flash?.let(::flash)
        return a
    }

    private fun flash(text: String) {
        _flash.value = text
        hideFlash?.cancel()
        hideFlash = viewModelScope.launch { delay(FLASH_MS); _flash.value = null }
    }

    private fun show(h: KnobHint?) {
        if (h == null) return
        _hint.value = h
        hide?.cancel()
        if (!pinned) hide = viewModelScope.launch { delay(HINT_MS); _hint.value = null }
    }

    /** The full list (a touch on the hint, or Outils › MIDI): it stays until closed. */
    fun open(device: String?) {
        val k = knobs ?: return
        pinned = true
        hide?.cancel()
        _hint.value = k.hint(device = device)
    }

    fun close() {
        pinned = false
        knobs?.learning = null
        _hint.value = null
    }

    fun learn(slot: Int?) {
        val k = knobs ?: return
        k.learning = slot
        _hint.value = k.hint(device = _hint.value?.device)
    }

    /** « Apprendre » of a fader or a button ([control] null: cancel). */
    fun learnControl(control: Control?) {
        knobs?.learningControl = control
        _learningControl.value = control
    }

    fun override(slot: Int, target: KnobTarget?) {
        val k = knobs ?: return
        k.override(slot, target)
        save(k.settings)
        _hint.value = k.hint(device = _hint.value?.device)
    }

    fun reset() {
        val k = knobs ?: return
        k.reset(_hint.value?.device)
        save(k.settings)
        _hint.value = k.hint(device = _hint.value?.device)
    }

    fun setEnabled(on: Boolean) {
        knobs?.setEnabled(on)
        _enabled.value = on
        save(knobs?.settings ?: load().copy(enabled = on))
    }

    fun setLegend(on: Boolean) {
        val k = knobs
        if (k != null) {
            k.setLegend(on)
            save(k.settings)
        } else {
            save(load().copy(legend = on))
        }
    }

    /** « Apprendre mes pads » ended: [pads] (packed notes heard) are [device]'s pads now. */
    fun setPads(device: String, pads: List<Int>) {
        val k = knobs ?: return
        k.setPads(device, pads)
        save(k.settings)
    }

    fun forget(device: String) {
        val k = knobs ?: return
        k.forget(device)
        save(k.settings)
    }

    /** What « Changer » offers for this screen. */
    fun candidates(): List<KnobSlot> {
        val k = knobs ?: return emptyList()
        val h = host ?: return emptyList()
        return KnobPages.candidates(k.screen, h.project, h.track, h.strips, k.faderCount())
    }

    /** What the controller does on [screen] now, for the legend. */
    fun legend(screen: KnobScreen): List<LegendRow> {
        val k = knobs ?: return emptyList()
        val h = host ?: return emptyList()
        val faders = k.faderCount()
        val knobsHere = KnobPages.resolved(screen, h.project, h.track, h.strips, k.settings.overrides[screen.name].orEmpty(), faders)
        val learnt = k.device?.let { d -> k.settings.buttons[d]?.keys?.mapNotNull { b -> ControlButton.entries.firstOrNull { it.name == b } }?.toSet() }.orEmpty()
        return ControllerLegend.rows(screen, knobsHere, if (faders > 0) k.faderSlots() else emptyList(), k.activeProfile(), h.project, learnt)
    }

    private fun load(): KnobSettings = runCatching {
        prefs.getString(KEY, null)?.let { JSON.decodeFromString(KnobSettings.serializer(), it).sanitized() }
    }.getOrNull() ?: KnobSettings()

    private fun save(s: KnobSettings) {
        _settings.value = s
        prefs.edit().putString(KEY, JSON.encodeToString(KnobSettings.serializer(), s)).apply()
    }

    companion object {
        const val PREFS = "midi_knobs"
        private const val KEY = "settings"
        const val HINT_MS = 2_500L
        const val FLASH_MS = 1_500L
        /** A controller with no name: its knobs are remembered under this one. */
        const val UNKNOWN = "?"
        private val JSON = Json { ignoreUnknownKeys = true }
    }
}
