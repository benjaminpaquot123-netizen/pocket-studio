package com.pocketstudio.app.midi

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketstudio.app.mixer.StripInfo
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.Project
import com.pocketstudio.model.midi.KnobLayout
import com.pocketstudio.model.midi.KnobSettings
import com.pocketstudio.model.midi.SoftTakeover
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/** What the knobs read and move: the open project, the selected track, the Live page's knobs. */
interface KnobHost {
    val project: Project
    val track: Int
    val live: LiveKnobs
    val strips: List<StripInfo>
    fun apply(t: KnobTarget, value: Float)
}

/**
 * The small hint on screen: the [screen]'s eight knobs ([slots], with their [values] and the CC
 * each answers to), the one just turned ([touched]) and, while it has not picked up its setting
 * yet, which way to turn it ([turn]: +1 up, −1 down, 0 picked up). [learning]: « Apprendre »
 * waits for a knob to be turned for that place.
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
)

/**
 * « Potards automatiques » (phase 39): the first eight knobs of a MIDI controller move the eight
 * most useful settings of the screen shown ([KnobPages]), each with soft takeover (it only moves
 * its setting once it has reached it: no jump), unless MIDI learn maps the knob already. The
 * values go through the same calls as the screens' own knobs (the engine's command queue).
 * Pure logic: [KnobsViewModel] keeps the settings and shows the hint.
 */
class AutoKnobs(private val host: KnobHost, settings: KnobSettings = KnobSettings()) {
    var settings: KnobSettings = settings
        private set
    var screen: KnobScreen = KnobScreen.Mixeur
    /** « Apprendre »: the next knob turned becomes knob [learning] (null: not learning). */
    var learning: Int? = null
    private val takeovers = Array(KnobLayout.SLOTS) { SoftTakeover() }
    var last: KnobHint? = null
        private set

    /** The knobs of the screen shown, as the user set them. */
    fun slots(): List<KnobSlot> =
        KnobPages.resolved(screen, host.project, host.track, host.strips, settings.overrides[screen.name].orEmpty())

    fun hint(touched: Int = -1, turn: Int = 0, device: String? = null): KnobHint {
        val slots = slots()
        val p = host.project
        val live = host.live
        return KnobHint(
            screen, slots, slots.map { KnobPages.valueOf(it.target, p, live) },
            ccs = List(slots.size) { KnobLayout.ccOf(it, device, settings) }, touched = touched, turn = turn, learning = learning, device = device,
        ).also { last = it }
    }

    /**
     * Knob [cc] of [device] at [value] (0..127). True when it was taken (a knob of the screen, or
     * learnt now); false when it is no knob of the eight (the caller leaves it alone).
     */
    fun control(cc: Int, value: Int, device: String): Boolean {
        if (!settings.enabled) return false
        learning?.let { slot ->
            learning = null
            settings = settings.withKnob(device, slot, cc)
            takeovers[slot] = SoftTakeover()
            hint(touched = slot, device = device)
            return true
        }
        val (slot, next) = KnobLayout.slotOf(cc, device, settings) ?: return false
        settings = next
        val target = slots().getOrNull(slot) ?: return true
        val t = takeovers[slot]
        t.follow(target.target.key)
        val current = KnobPages.valueOf(target.target, host.project, host.live)
        t.move(value / 127f, current)?.let { host.apply(target.target, it) }
        hint(touched = slot, turn = t.direction(current), device = device)
        return true
    }

    /** « Changer »: knob [slot] of this screen moves [target] from now on (null: the app's choice again). */
    fun override(slot: Int, target: KnobTarget?) {
        settings = settings.withOverride(screen.name, slot, target?.key)
        takeovers[slot] = SoftTakeover()
    }

    fun setEnabled(on: Boolean) {
        settings = settings.copy(enabled = on)
    }

    /** « Remettre comme au départ » for this screen and [device]. */
    fun reset(device: String?) {
        settings = settings.reset(screen.name, device)
        takeovers.indices.forEach { takeovers[it] = SoftTakeover() }
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
    private var hide: Job? = null
    /** The sheet « Potards du contrôleur » is open: the hint stays. */
    var pinned = false
        private set

    private var host: KnobHost? = null

    /** The open project's knobs (once: the ViewModel lives with the project's screen). */
    fun attach(host: KnobHost) {
        if (knobs != null) return
        this.host = host
        knobs = AutoKnobs(host, load())
    }

    var screen: KnobScreen
        get() = knobs?.screen ?: KnobScreen.Mixeur
        set(value) {
            val k = knobs ?: return
            if (k.screen == value) return
            k.screen = value
            if (_hint.value != null) _hint.value = k.hint(device = _hint.value?.device)
        }

    /** A CC no MIDI learn took: one of the screen's knobs, or nothing. [device]: the controller's name. */
    fun control(cc: Int, value: Int, device: String?): Boolean {
        val k = knobs ?: return false
        val before = k.settings
        val taken = k.control(cc, value, device ?: UNKNOWN)
        if (k.settings != before) save(k.settings)
        if (taken) show(k.last)
        return taken
    }

    private fun show(h: KnobHint?) {
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

    /** What « Changer » offers for this screen. */
    fun candidates(): List<KnobSlot> {
        val k = knobs ?: return emptyList()
        val h = host ?: return emptyList()
        return KnobPages.candidates(k.screen, h.project, h.track, h.strips)
    }

    private fun load(): KnobSettings = runCatching {
        prefs.getString(KEY, null)?.let { JSON.decodeFromString(KnobSettings.serializer(), it).sanitized() }
    }.getOrNull() ?: KnobSettings()

    private fun save(s: KnobSettings) {
        prefs.edit().putString(KEY, JSON.encodeToString(KnobSettings.serializer(), s)).apply()
    }

    companion object {
        const val PREFS = "midi_knobs"
        private const val KEY = "settings"
        const val HINT_MS = 2_500L
        /** A controller with no name: its knobs are remembered under this one. */
        const val UNKNOWN = "?"
        private val JSON = Json { ignoreUnknownKeys = true }
    }
}
