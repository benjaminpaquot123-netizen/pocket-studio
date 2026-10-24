package com.pocketstudio.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * A studio voice of the engine played by a drum pad instead of its kit voice (phase 34, S3;
 * the engine's DrumKit::Kind, NativeEngine.PAD_VOICE_*): the tuned 808 bass, the short studio
 * kick, its hard variant, the filtered rumble. [params]: the voice's settings that differ from
 * its defaults (DrumKit::VoiceParam → value in that setting's unit: [TUNE] a MIDI note for the
 * 808, hertz for the kicks and the rumble; [DECAY] seconds; the others 0..1, [GLIDE] seconds).
 *
 * The engine resets a voice's settings when the pad changes voice or the kit changes version:
 * they are sent after both ([engineCommands]).
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PadVoice(
    val kind: Int,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val params: Map<Int, Float> = emptyMap(),
) {
    fun sanitized(): PadVoice = copy(
        kind = kind.coerceIn(BASS_808, RUMBLE),
        params = params.filter { (k, v) -> k in TUNE..TONE && v.isFinite() }.mapValues { (k, v) -> v.coerceIn(range(k)) },
    )

    /** A setting of this voice: its own value, or null (the engine's default). */
    fun param(param: Int): Float? = params[param]

    fun with(param: Int, value: Float): PadVoice = copy(params = params + (param to value)).sanitized()

    /** One of the studio kicks (a kit chosen on the pads replaces them). */
    val isKick: Boolean get() = kind == KICK_STUDIO || kind == KICK_HARD

    /** The voice's name on its pad (saved in French, translated at display). */
    val name: String get() = NAMES.getOrElse(kind) { "" }

    companion object {
        // The engine's DrumKit::Kind (append only).
        const val KIT = 0
        const val BASS_808 = 1
        const val KICK_STUDIO = 2
        const val KICK_HARD = 3
        const val RUMBLE = 4

        // The engine's DrumKit::VoiceParam (append only).
        const val TUNE = 0
        const val DECAY = 1
        const val DRIVE = 2
        const val CLICK = 3
        const val GLIDE = 4
        const val SUB = 5
        const val TONE = 6

        /** The settings an 808 track sends (the engine's Track808::Param; its tune is its notes'). */
        val TRACK_808_PARAMS = listOf(DECAY, DRIVE, CLICK, GLIDE, TONE)

        /** An 808 track's setting when the track has none of its own (the engine's Track808::prepare: notes held up to 2.5 s). */
        fun track808Default(param: Int): Float = when (param) {
            DECAY -> 2.5f
            DRIVE -> 0.4f
            CLICK -> 0.4f
            GLIDE -> 0.08f
            TONE -> 0.5f
            SUB -> 0.5f
            else -> 0f
        }

        /** The names the pads show for each voice. */
        val NAMES = listOf("", "808", "Kick studio", "Kick dur", "Rumble")

        /** The range the engine takes each setting in (it clamps the same). */
        fun range(param: Int): ClosedFloatingPointRange<Float> = when (param) {
            TUNE -> 12f..400f
            DECAY -> 0.02f..2.5f
            GLIDE -> 0f..0.5f
            else -> 0f..1f
        }

        /** Pads 0..15 only, voices other than the kit's own (which needs no entry). */
        fun sanitize(voices: Map<Int, PadVoice>): Map<Int, PadVoice> =
            voices.filter { (lane, v) -> lane in 0 until DrumPattern.PAD_COUNT && v.kind in BASS_808..RUMBLE }.mapValues { it.value.sanitized() }
    }
}
