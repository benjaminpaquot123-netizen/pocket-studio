package com.pocketstudio.model.midi

/**
 * How a knob says it turned. [ABSOLUTE]: its position 0..127 (it needs soft takeover). The
 * relative ones send a step: [RELATIVE_OFFSET] around 64 (65 = one step up, 63 = one down; Arturia
 * « Relative 1 », Ableton's « binary offset »), [RELATIVE_TWOS] two's complement (1 up, 127 down),
 * [RELATIVE_SIGNED] sign and size (1 up, 65 down).
 */
enum class KnobMode {
    ABSOLUTE, RELATIVE_OFFSET, RELATIVE_TWOS, RELATIVE_SIGNED;

    /** The steps turned for [value] (+ clockwise); null for [ABSOLUTE]. */
    fun delta(value: Int): Int? = when (this) {
        ABSOLUTE -> null
        RELATIVE_OFFSET -> value - 64
        RELATIVE_TWOS -> if (value < 64) value else value - 128
        RELATIVE_SIGNED -> if (value < 64) value else -(value - 64)
    }

    val relative: Boolean get() = this != ABSOLUTE
}

/** The buttons of a controller the app gives a job to (a press is a value of 64 or more). */
enum class ControlButton {
    /** The main encoder turned (relative), alone or with Shift held. */
    ENCODER, ENCODER_SHIFT,
    /** The main encoder pressed, alone or with Shift held. */
    CLICK, CLICK_SHIFT,
    /** Shift itself: taken, so it never becomes a knob. */
    SHIFT,
    PLAY, STOP, RECORD,
}

/** What a control change of a controller is, for the app. */
sealed interface Control {
    data class Knob(val index: Int, val mode: KnobMode) : Control
    data class Fader(val index: Int) : Control
    data class Button(val button: ControlButton, val mode: KnobMode = KnobMode.RELATIVE_OFFSET) : Control
}

/**
 * A MIDI controller the app knows (« profil »): its name as the phone sees it ([names]: parts of
 * it, compared without case, spaces nor dashes), its USB ids when known, its eight [knobs] (and
 * [altKnobs], the same knobs in another of its programs, e.g. the MiniLab 3's « DAW »), its
 * [faders], its pads ([padChannel] 0..15, [padNotes]: pad 1, 2… in order, bank B after bank A)
 * and its buttons. [sure]: taken from the maker's manual or its DAW script; otherwise from
 * community documentation (to check on the real device).
 */
data class ControllerProfile(
    val id: String,
    val name: String,
    val names: List<String>,
    val usb: List<Pair<Int, Int>> = emptyList(),
    val knobs: List<Int> = emptyList(),
    val knobMode: KnobMode = KnobMode.ABSOLUTE,
    val altKnobs: List<Int> = emptyList(),
    val altKnobMode: KnobMode = KnobMode.RELATIVE_OFFSET,
    val faders: List<Int> = emptyList(),
    val altFaders: List<Int> = emptyList(),
    val padChannel: Int = 9,
    val padNotes: List<Int> = (36..51).toList(),
    val buttons: Map<ControlButton, Int> = emptyMap(),
    /** The channel its buttons send on (null: any). */
    val buttonChannel: Int? = null,
    val sure: Boolean = true,
) {
    val hasFaders: Boolean get() = faders.isNotEmpty()
}

/** What the phone says of a MIDI device: its name, maker, product, USB ids (-1: unknown). */
data class DeviceId(val name: String, val manufacturer: String? = null, val product: String? = null, val vendorId: Int = -1, val productId: Int = -1)

/**
 * The controllers the app knows and how a control change, a note becomes a knob, a fader, a
 * button or a pad on a given device: the user's own choices first (« Apprendre », kept per device in
 * [KnobSettings]), then the device's profile, then what most controllers send.
 */
object Controllers {
    const val ARTURIA = 0x1C75
    const val MINILAB3_USB = 0x220B

    /** The Arturia MiniLab 3: manual 1.0.5 (Arturia mode) and Ableton Live's MiniLab_3 script (DAW mode). */
    val MINILAB3 = ControllerProfile(
        id = "minilab3", name = "Arturia MiniLab 3", names = listOf("minilab3"),
        usb = listOf(ARTURIA to MINILAB3_USB),
        knobs = listOf(74, 71, 76, 77, 93, 18, 19, 16),
        altKnobs = listOf(86, 87, 89, 90, 110, 111, 116, 117), altKnobMode = KnobMode.RELATIVE_OFFSET,
        faders = listOf(82, 83, 85, 17), altFaders = listOf(14, 15, 30, 31),
        buttons = mapOf(
            ControlButton.ENCODER to 28, ControlButton.ENCODER_SHIFT to 29,
            ControlButton.CLICK to 118, ControlButton.CLICK_SHIFT to 119, ControlButton.SHIFT to 27,
        ),
    )

    val PROFILES: List<ControllerProfile> = listOf(
        MINILAB3,
        ControllerProfile(
            id = "minilab2", name = "Arturia MiniLab mkII", names = listOf("minilabmkii", "minilab mk2"),
            knobs = listOf(74, 71, 76, 77, 93, 18, 19, 16), sure = false,
        ),
        ControllerProfile(
            id = "mpkmini", name = "Akai MPK mini", names = listOf("mpkmini", "mpk mini"),
            knobs = (70..77).toList(), sure = false,
        ),
        ControllerProfile(
            id = "launchkeymini", name = "Novation Launchkey Mini", names = listOf("launchkeymini"),
            knobs = (21..28).toList(),
            buttons = mapOf(ControlButton.PLAY to 115, ControlButton.RECORD to 117), buttonChannel = 15, sure = false,
        ),
        ControllerProfile(id = "launchkey", name = "Novation Launchkey", names = listOf("launchkey", "launchcontrol"), knobs = (21..28).toList(), sure = false),
        ControllerProfile(
            id = "nanokontrol2", name = "Korg nanoKONTROL2", names = listOf("nanokontrol2"),
            knobs = (16..23).toList(), faders = (0..7).toList(),
            buttons = mapOf(ControlButton.PLAY to 41, ControlButton.STOP to 42, ControlButton.RECORD to 45),
        ),
        ControllerProfile(id = "nanokontrol", name = "Korg nanoKONTROL", names = listOf("nanokontrol"), knobs = (16..23).toList(), sure = false),
        ControllerProfile(id = "lpd8", name = "Akai LPD8", names = listOf("lpd8"), knobs = (1..8).toList(), sure = false),
        ControllerProfile(id = "vmini", name = "Alesis V-Mini", names = listOf("vmini"), knobs = (20..23).toList(), sure = false),
    )

    /** For matching names: lower case, without spaces, dashes nor dots. */
    fun normalize(s: String): String = s.lowercase().filter { it.isLetterOrDigit() }

    /** The profile of [id]: its USB ids first, then its name, maker and product. Null: a controller the app does not know (generic). */
    fun detect(id: DeviceId): ControllerProfile? {
        PROFILES.firstOrNull { p -> p.usb.any { (v, prod) -> v == id.vendorId && prod == id.productId } }?.let { return it }
        val text = normalize(listOfNotNull(id.name, id.manufacturer, id.product).joinToString(" "))
        return PROFILES.firstOrNull { p -> p.names.any { text.contains(normalize(it)) } }
    }

    /** By name only (the settings know devices by their name). */
    fun detect(name: String?): ControllerProfile? = name?.let { detect(DeviceId(it)) }

    fun byId(id: String?): ControllerProfile? = PROFILES.firstOrNull { it.id == id }

    /**
     * What control change [cc] (on [channel]) is on [device] (its [profile]) with [settings], and the
     * settings with what it learnt (a knob turned for the first time on a controller the app does not
     * know). Null: nothing of the controller's (left to MIDI learn, the mod wheel…).
     */
    fun controlOf(cc: Int, channel: Int, device: String, profile: ControllerProfile?, settings: KnobSettings): Pair<Control, KnobSettings>? {
        // The user's own choices first.
        settings.buttons[device]?.entries?.firstOrNull { it.value == cc }?.let { (b, _) ->
            ControlButton.entries.firstOrNull { it.name == b }?.let { return Control.Button(it, settings.modeOf(device) ?: KnobMode.RELATIVE_OFFSET) to settings }
        }
        settings.faders[device]?.entries?.firstOrNull { it.value == cc }?.let { return Control.Fader(it.key) to settings }
        val learntKnob = settings.devices[device]?.entries?.firstOrNull { it.value == cc }
        if (learntKnob != null) return Control.Knob(learntKnob.key, knobModeOf(cc, device, profile, settings)) to settings
        if (profile != null && (profile.buttonChannel == null || profile.buttonChannel == channel)) {
            profile.buttons.entries.firstOrNull { it.value == cc && settings.buttons[device]?.containsKey(it.key.name) != true }?.let {
                return Control.Button(it.key) to settings
            }
        }
        if (profile != null) {
            val f = profile.faders.indexOf(cc).takeIf { it >= 0 } ?: profile.altFaders.indexOf(cc).takeIf { it >= 0 }
            if (f != null && settings.faders[device]?.containsKey(f) != true) return Control.Fader(f) to settings
        }
        val (slot, next) = KnobLayout.slotOf(cc, device, settings, profile) ?: return null
        return Control.Knob(slot, knobModeOf(cc, device, profile, next)) to next
    }

    /** How knob [cc] of [device] turns: the user's choice, else its profile's (its second program too), else as guessed. */
    fun knobModeOf(cc: Int, device: String, profile: ControllerProfile?, settings: KnobSettings): KnobMode {
        settings.modeOf(device)?.let { return it }
        if (profile != null) {
            if (cc in profile.altKnobs) return profile.altKnobMode
            return profile.knobMode
        }
        return KnobMode.ABSOLUTE
    }

    /** Pad 0..15 for a note: the pads the user taught first, else the profile's, else channel 10 notes 36–51. Null: a key. */
    fun padOf(channel: Int, note: Int, profile: ControllerProfile?, learnt: List<Int>?): Int? {
        if (!learnt.isNullOrEmpty()) return learnt.indexOf(packPad(channel, note)).takeIf { it >= 0 }
        if (profile != null) return if (channel == profile.padChannel) profile.padNotes.indexOf(note).takeIf { it in 0 until PADS } else null
        return if (channel == 9 && note in 36 until 36 + PADS) note - 36 else null
    }

    fun packPad(channel: Int, note: Int): Int = (channel and 0xF) shl 7 or (note and 0x7F)
    fun padChannel(packed: Int): Int = packed shr 7 and 0xF
    fun padNote(packed: Int): Int = packed and 0x7F

    const val PADS = 16
    /** Faders the app moves: the first strips' volumes. */
    const val FADERS = 8
}

/**
 * Tells relative knobs from absolute ones on a controller the app does not know, from what they
 * send. An absolute knob sends only when its value changes (never the same value twice in a row)
 * and anywhere in 0..127; a relative one sends the same small step again and again, near its
 * « nothing » value (64, or 0 in two's complement) which it never sends. Sure after [NEEDED]
 * messages of one knob; a value far from both bands says absolute at once.
 */
class KnobModeGuess {
    private val recent = HashMap<Int, ArrayDeque<Int>>()

    /** Knob [cc] sent [value]: the mode once it is sure, else null. */
    fun feed(cc: Int, value: Int): KnobMode? {
        if (value in 8..56 || value in 72..120) return KnobMode.ABSOLUTE
        val q = recent.getOrPut(cc) { ArrayDeque() }
        q.addLast(value)
        while (q.size > NEEDED) q.removeFirst()
        if (q.size < NEEDED) return null
        val repeats = q.zipWithNext().count { (a, b) -> a == b }
        if (repeats == 0) return KnobMode.ABSOLUTE
        if (q.all { it in 57..71 && it != 64 }) return KnobMode.RELATIVE_OFFSET
        if (q.all { it in 1..7 || it in 121..127 }) return KnobMode.RELATIVE_TWOS
        return KnobMode.ABSOLUTE
    }

    fun reset() = recent.clear()

    companion object {
        const val NEEDED = 6
    }
}
