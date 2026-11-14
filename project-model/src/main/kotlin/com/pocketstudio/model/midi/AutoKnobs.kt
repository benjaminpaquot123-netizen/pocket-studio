package com.pocketstudio.model.midi

import kotlinx.serialization.Serializable
import kotlin.math.abs

/**
 * « Potards automatiques » (phase 39, Benjamin's idea): what the app remembers, for every screen,
 * of the controller's knobs, saved in the settings (not in a project). [devices]: for each
 * controller (by name), which knob (MIDI CC) is knob 1..8: learnt by « Apprendre », or the
 * order its knobs were first turned in when the app does not know it. [overrides]: for a screen
 * (its [KnobScreen] name), a knob given another setting than the one the app chose (a packed
 * target, see the app's KnobTarget). [enabled]: off, the knobs only do what MIDI learn maps.
 */
@Serializable
data class KnobSettings(
    val enabled: Boolean = true,
    val devices: Map<String, Map<Int, Int>> = emptyMap(),
    val overrides: Map<String, Map<Int, Long>> = emptyMap(),
    /** For each controller (by name): which CC is fader 1..8, learnt by « Apprendre ». */
    val faders: Map<String, Map<Int, Int>> = emptyMap(),
    /** For each controller: the CC of a button ([ControlButton] name), learnt by « Apprendre ». */
    val buttons: Map<String, Map<String, Int>> = emptyMap(),
    /** For each controller: its pads 1..16 as « Apprendre mes pads » heard them ([Controllers.packPad]). */
    val pads: Map<String, List<Int>> = emptyMap(),
    /** For each controller: how its knobs turn ([KnobMode] name), chosen or guessed from what they send. */
    val modes: Map<String, String> = emptyMap(),
    /** The small legend of what the controller does on the screen shown. */
    val legend: Boolean = true,
) {
    fun modeOf(device: String): KnobMode? = modes[device]?.let { m -> KnobMode.entries.firstOrNull { it.name == m } }

    fun withMode(device: String, mode: KnobMode?): KnobSettings =
        copy(modes = if (mode == null) modes - device else modes + (device to mode.name))

    /** One CC is one control of a device: it leaves whatever knob, fader or button had it. */
    private fun freed(device: String, cc: Int): KnobSettings = copy(
        devices = devices[device]?.let { m -> devices + (device to m.filterValues { it != cc }) } ?: devices,
        faders = faders[device]?.let { m -> faders + (device to m.filterValues { it != cc }) } ?: faders,
        buttons = buttons[device]?.let { m -> buttons + (device to m.filterValues { it != cc }) } ?: buttons,
    )

    fun withFader(device: String, index: Int, cc: Int): KnobSettings {
        val s = freed(device, cc)
        return s.copy(faders = s.faders + (device to (s.faders[device].orEmpty() + (index to cc))))
    }

    fun withButton(device: String, button: ControlButton, cc: Int): KnobSettings {
        val s = freed(device, cc)
        return s.copy(buttons = s.buttons + (device to (s.buttons[device].orEmpty() + (button.name to cc))))
    }

    fun withPads(device: String, pads: List<Int>?): KnobSettings =
        copy(pads = if (pads.isNullOrEmpty()) this.pads - device else this.pads + (device to pads.take(Controllers.PADS)))

    /** Whether the user changed anything for [device] (its own profile on top of the app's). */
    fun customized(device: String): Boolean =
        !devices[device].isNullOrEmpty() || !faders[device].isNullOrEmpty() || !buttons[device].isNullOrEmpty() || !pads[device].isNullOrEmpty() || device in modes

    /** « Oublier mes réglages » for [device]: its knobs, faders, buttons, pads and mode as the app sets them. */
    fun forget(device: String): KnobSettings = copy(devices = devices - device, faders = faders - device, buttons = buttons - device, pads = pads - device, modes = modes - device)

    /** The CC of knob [slot] on [device], when it is known for this device. */
    fun learnt(device: String, slot: Int): Int? = devices[device]?.get(slot)

    fun withKnob(device: String, slot: Int, cc: Int): KnobSettings {
        // One CC is one knob: it leaves the slot it had (and a fader or a button that had it).
        val s = copy(faders = faders[device]?.let { m -> faders + (device to m.filterValues { it != cc }) } ?: faders,
            buttons = buttons[device]?.let { m -> buttons + (device to m.filterValues { it != cc }) } ?: buttons)
        val mine = s.devices[device].orEmpty().filter { (k, c) -> k != slot && c != cc } + (slot to cc)
        return s.copy(devices = s.devices + (device to mine))
    }

    fun withOverride(screen: String, slot: Int, target: Long?): KnobSettings {
        val mine = overrides[screen].orEmpty().let { if (target == null) it - slot else it + (slot to target) }
        return copy(overrides = if (mine.isEmpty()) overrides - screen else overrides + (screen to mine))
    }

    /** « Remettre comme au départ » for [screen] and [device]: the app's choices, its knobs in their own order. */
    fun reset(screen: String, device: String?): KnobSettings =
        copy(overrides = overrides - screen, devices = if (device == null) devices else devices - device)

    fun sanitized(): KnobSettings = copy(
        devices = devices.mapValues { (_, m) -> m.filter { (s, c) -> s in 0 until KnobLayout.SLOTS && c in 0..119 } }.filterValues { it.isNotEmpty() },
        overrides = overrides.mapValues { (_, m) -> m.filterKeys { it in 0 until KnobLayout.SLOTS } }.filterValues { it.isNotEmpty() },
        faders = faders.mapValues { (_, m) -> m.filter { (s, c) -> s in 0 until Controllers.FADERS && c in 0..119 } }.filterValues { it.isNotEmpty() },
        buttons = buttons.mapValues { (_, m) -> m.filter { (b, c) -> ControlButton.entries.any { it.name == b } && c in 0..119 } }.filterValues { it.isNotEmpty() },
        pads = pads.mapValues { (_, l) -> l.filter { it in 0 until (16 shl 7) }.distinct().take(Controllers.PADS) }.filterValues { it.isNotEmpty() },
        modes = modes.filterValues { m -> KnobMode.entries.any { it.name == m } },
    )
}

/**
 * Which knob of a controller is knob 1..8. Controllers send knobs on different CCs: the ones the
 * app knows ([Controllers.PROFILES]: Arturia MiniLab 3, Akai MPK mini, Novation Launchkey, Korg nanoKONTROL…),
 * then the blocks most controllers use (21–28, 70–77, 16–23, 102–109), then the order the knobs
 * are first turned in, remembered for that device. A knob learnt (« Apprendre ») always wins.
 */
object KnobLayout {
    const val SLOTS = 8

    /** CCs never taken as knobs: bank select, mod wheel, data entry, volume, the pedals, NRPN, the channel modes. */
    val RESERVED: Set<Int> = setOf(0, 1, 6, 7, 32, 38) + (64..69) + (96..101) + (120..127)

    /** Blocks of eight knobs many controllers send by default. */
    val BLOCKS: List<IntRange> = listOf(21..28, 70..77, 16..23, 102..109)

    /** The knobs 1..8 of [device]'s profile (null: a controller the app does not know). */
    fun profileOf(device: String?): List<Int>? = Controllers.detect(device)?.knobs

    /**
     * Knob 0..7 the app gives [cc] by itself: its place in [profile]'s knobs (or the same knobs of
     * its other program), else in a common block for a controller it does not know. A profile
     * without knobs (a keyboard only) leaves its CCs to the common blocks.
     */
    fun defaultSlot(cc: Int, device: String?, profile: ControllerProfile? = Controllers.detect(device)): Int? {
        if (profile != null && profile.knobs.isNotEmpty()) {
            return (profile.knobs.indexOf(cc).takeIf { it >= 0 } ?: profile.altKnobs.indexOf(cc).takeIf { it >= 0 })
        }
        return BLOCKS.firstOrNull { cc in it }?.let { cc - it.first }
    }

    /**
     * Knob 0..7 for [cc] on [device] with [settings], and the settings with a new knob remembered
     * (a CC turned for the first time on a controller the app does not know takes the first free
     * knob). Null when it is no knob: reserved, another control of a known controller, its place
     * taken by a knob learnt, or all eight taken.
     */
    fun slotOf(cc: Int, device: String, settings: KnobSettings, profile: ControllerProfile? = Controllers.detect(device)): Pair<Int, KnobSettings>? {
        val known = settings.devices[device].orEmpty()
        known.entries.firstOrNull { it.value == cc }?.let { return it.key to settings }
        defaultSlot(cc, device, profile)?.let { slot -> return if (slot in known) null else slot to settings }
        if (cc in RESERVED || cc !in 0..119 || (profile != null && profile.knobs.isNotEmpty())) return null
        // A CC the user gave a fader or a button is not a knob.
        if (settings.faders[device]?.containsValue(cc) == true || settings.buttons[device]?.containsValue(cc) == true) return null
        // First turned, first knob: remembered for this device.
        val free = (0 until SLOTS).firstOrNull { it !in known } ?: return null
        return free to settings.withKnob(device, free, cc)
    }

    /** The CC knob [slot] answers to on [device] (null: not known yet), for the hint. */
    fun ccOf(slot: Int, device: String?, settings: KnobSettings, profile: ControllerProfile? = Controllers.detect(device)): Int? {
        device?.let { settings.learnt(it, slot) }?.let { return it }
        return profile?.knobs?.getOrNull(slot)
    }
}

/**
 * Soft takeover (« pickup »): a knob only moves its setting once it has come to where the setting
 * is, so turning it never makes the sound jump. It picks up when within [TOLERANCE] of the
 * setting, or when it crosses it between two messages; a setting moved meanwhile on screen (or
 * another setting behind the knob, a new screen) waits for the knob again.
 */
class SoftTakeover {
    private var key: Long? = null
    private var picked = false
    private var lastKnob: Float? = null
    private var lastSent: Float? = null

    /** Whether the knob moves its setting now. */
    val pickedUp: Boolean get() = picked

    /** The setting under the knob is [target] now: a new one is waited for again. */
    fun follow(target: Long) {
        if (key == target) return
        key = target
        picked = false
        lastKnob = null
        lastSent = null
    }

    /**
     * The knob at [knob] (0..1) while its setting is at [current]: the value to send, or null
     * while it has not picked up.
     */
    fun move(knob: Float, current: Float): Float? {
        if (picked && lastSent != null && abs(current - lastSent!!) > TOLERANCE) picked = false
        if (!picked) {
            val before = lastKnob
            val crossed = before != null && (before - current) * (knob - current) <= 0f
            if (abs(knob - current) <= TOLERANCE || crossed) picked = true
        }
        lastKnob = knob
        if (!picked) return null
        lastSent = knob
        return knob
    }

    /** Which way to turn to pick up: +1 up, −1 down, 0 picked (or nothing known yet). */
    fun direction(current: Float): Int {
        val k = lastKnob ?: return 0
        if (picked) return 0
        return if (current > k) 1 else -1
    }

    companion object {
        /** Two steps of a 7-bit knob. */
        const val TOLERANCE = 2f / 127f
    }
}
