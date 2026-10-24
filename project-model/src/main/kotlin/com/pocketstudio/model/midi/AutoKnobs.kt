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
) {
    /** The CC of knob [slot] on [device], when it is known for this device. */
    fun learnt(device: String, slot: Int): Int? = devices[device]?.get(slot)

    fun withKnob(device: String, slot: Int, cc: Int): KnobSettings {
        // One CC is one knob: it leaves the slot it had.
        val mine = devices[device].orEmpty().filter { (s, c) -> s != slot && c != cc } + (slot to cc)
        return copy(devices = devices + (device to mine))
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
    )
}

/**
 * Which knob of a controller is knob 1..8. Controllers send knobs on different CCs: the ones the
 * app knows by their name (Arturia MiniLab 3, Novation Launchkey, Korg nanoKONTROL, Akai LPD8),
 * then the blocks most controllers use (21–28, 70–77, 16–23, 102–109), then the order the knobs
 * are first turned in, remembered for that device. A knob learnt (« Apprendre ») always wins.
 */
object KnobLayout {
    const val SLOTS = 8

    /** CCs never taken as knobs: bank select, mod wheel, data entry, volume, the pedals, NRPN, the channel modes. */
    val RESERVED: Set<Int> = setOf(0, 1, 6, 7, 32, 38) + (64..69) + (96..101) + (120..127)

    /** The controllers the app knows: a part of their name (lower case), their knobs 1..8. */
    val PROFILES: List<Pair<String, List<Int>>> = listOf(
        "minilab 3" to listOf(74, 71, 76, 77, 93, 18, 19, 16),
        "minilab3" to listOf(74, 71, 76, 77, 93, 18, 19, 16),
        "launchkey" to (21..28).toList(),
        "launch control" to (21..28).toList(),
        "nanokontrol" to (16..23).toList(),
        "lpd8" to (1..8).toList(),
    )

    /** Blocks of eight knobs many controllers send by default. */
    val BLOCKS: List<IntRange> = listOf(21..28, 70..77, 16..23, 102..109)

    fun profileOf(device: String?): List<Int>? {
        val name = device?.lowercase() ?: return null
        return PROFILES.firstOrNull { name.contains(it.first) }?.second
    }

    /** Knob 0..7 the app gives [cc] on [device] by itself: its place in the device's profile, else in a common block. */
    fun defaultSlot(cc: Int, device: String?): Int? {
        profileOf(device)?.let { profile -> return profile.indexOf(cc).takeIf { it >= 0 } }
        return BLOCKS.firstOrNull { cc in it }?.let { cc - it.first }
    }

    /**
     * Knob 0..7 for [cc] on [device] with [settings], and the settings with a new knob remembered
     * (a CC turned for the first time on a controller the app does not know takes the first free
     * knob). Null when it is no knob: reserved, another control of a known controller, its place
     * taken by a knob learnt, or all eight taken.
     */
    fun slotOf(cc: Int, device: String, settings: KnobSettings): Pair<Int, KnobSettings>? {
        val known = settings.devices[device].orEmpty()
        known.entries.firstOrNull { it.value == cc }?.let { return it.key to settings }
        defaultSlot(cc, device)?.let { slot -> return if (slot in known) null else slot to settings }
        if (cc in RESERVED || cc !in 0..119 || profileOf(device) != null) return null
        // First turned, first knob: remembered for this device.
        val free = (0 until SLOTS).firstOrNull { it !in known } ?: return null
        return free to settings.withKnob(device, free, cc)
    }

    /** The CC knob [slot] answers to on [device] (null: not known yet), for the hint. */
    fun ccOf(slot: Int, device: String?, settings: KnobSettings): Int? {
        device?.let { settings.learnt(it, slot) }?.let { return it }
        profileOf(device)?.let { return it.getOrNull(slot) }
        return null
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
