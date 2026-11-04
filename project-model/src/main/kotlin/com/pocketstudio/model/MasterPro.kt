package com.pocketstudio.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * Lot E (« Master pro », PLAN-AUDITS-FINAUX §1 bis): the master's settings a pro asks for, behind
 * « Avancé » in Pro mode ([Mixer.pro], null until one is touched: the master as before, the Mode
 * simple's three choices untouched).
 * - [targetLufs]: a target in integrated LUFS (Apple Music −16, or a value from [MIN_LUFS] to [MAX_LUFS];
 *   −14 from before reads « Streaming ») aimed at the whole song; null: [Mixer.target]. Set only through
 *   [LoudnessTargets.choose] (audit ultra round 1: one target in the whole app).
 * - [ceilingDb]: the true-peak ceiling (−1, −0,5 or −0,3 dBTP), held after the encoding (M4A included).
 * - [attack], [release]: the limiter's (MasterChain: a share of its 1 ms look-ahead, a scale of its
 *   release times); the look-ahead itself is fixed (the latency never changes).
 * - [reference]: a song imported to compare with (its file in the app's storage) and its loudness.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class MasterPro(
    @EncodeDefault(EncodeDefault.Mode.NEVER) val targetLufs: Float? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val ceilingDb: Float = DEFAULT_CEILING,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val attack: Float = 1f,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val release: Float = 1f,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val reference: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val referenceName: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val referenceLufs: Float? = null,
    /** Audit ultra round 1: [targetLufs] is « Libre » (a value chosen, even −14 or −16), not « Streaming » nor « Apple Music ». */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val free: Boolean = false,
) {
    fun sanitized(): MasterPro = copy(
        targetLufs = targetLufs?.takeIf { it.isFinite() }?.coerceIn(MIN_LUFS, MAX_LUFS),
        ceilingDb = ceilingDb.takeIf { it in CEILINGS } ?: CEILINGS.minBy { kotlin.math.abs(it - (ceilingDb.takeIf { c -> c.isFinite() } ?: DEFAULT_CEILING)) },
        attack = attack.takeIf { it.isFinite() }?.coerceIn(0.2f, 1f) ?: 1f,
        release = release.takeIf { it.isFinite() }?.coerceIn(0.25f, 4f) ?: 1f,
        referenceLufs = referenceLufs?.takeIf { it.isFinite() && it > -70f },
    )

    /** Nothing changed from the master as it was. */
    val isDefault: Boolean get() = this == MasterPro()

    companion object {
        const val MIN_LUFS = -23f
        const val MAX_LUFS = -6f
        const val DEFAULT_CEILING = -1f
        val CEILINGS = listOf(-1f, -0.5f, -0.3f)
    }
}
