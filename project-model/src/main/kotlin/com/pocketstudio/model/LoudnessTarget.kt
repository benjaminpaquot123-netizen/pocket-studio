package com.pocketstudio.model

import com.pocketstudio.model.i18n.tr
import kotlinx.serialization.Serializable

/**
 * The master's « Cible »: the loudness the engine brings the mix to, with a gain before its
 * clipper and its true-peak limiter (ceiling −1 dBTP). [OFF] keeps the master as it is (projects
 * saved before phase 33); [AUTO] follows the project's style (son.md §2.4); [CLUB] −8 LUFS (or the style's own when louder);
 * [STREAMING] −14 LUFS, where the platforms turn songs down to.
 */
@Serializable
enum class LoudnessTarget {
    OFF, AUTO, CLUB, STREAMING;

    /** Its short name in the master's settings. */
    val label: String get() = when (this) {
        OFF -> tr("Désactivée")
        AUTO -> tr("Auto (style)")
        CLUB -> tr("Club")
        STREAMING -> tr("Streaming")
    }
}

object LoudnessTargets {
    const val CLUB_LUFS = -8f
    const val STREAMING_LUFS = -14f
    /** A project made by hand (no style): between club and streaming. */
    const val DEFAULT_LUFS = -9f

    /** son.md §2.4: the integrated loudness of a finished track of each assistant's style (±1 LU). */
    val BY_STYLE: Map<String, Float> = buildMap {
        for (s in listOf("techno", "hard_techno", "acid_techno", "industrial_techno", "acidcore")) put(s, -7f)
        put("gabber", -6f)
        for (s in listOf("house", "tech_house", "acid_house", "afro_house", "uk_garage", "speed_garage", "disco", "amapiano")) put(s, -8f)
        put("deep_house", -9f)
        for (s in listOf("dnb", "jump_up", "jungle", "neurofunk", "dubstep", "riddim")) put(s, -7f)
        for (s in listOf("liquid_dnb", "trance", "synthwave", "retro_techno", "detroit", "minimal")) put(s, -8f)
        for (s in listOf("trap", "drill", "phonk", "grime", "jersey_club", "reggaeton", "afrobeats", "pop")) put(s, -8.5f)
        put("boom_bap", -10f)
        put("lofi", -12f)
    }

    /**
     * Phase 34 (S5, [Project.MIX_V2]): the loudness of each style's Drop (its loudest part, which
     * the target aims at since then), from the study of 10 hits a style (docs/hits): the softer house
     * (deep, afro, amapiano, disco) at −9, hard techno at −6, phonk at −7, trap and drill at −8.5;
     * pop, afrobeats and reggaeton's choruses at −9 (their songs at −10 to −12 integrated); techno and
     * bass music at −7.5 and gabber at −7, where their mixes get with the chain taking 3 LU at most
     * (S5 bench; the hits' −4 to −6 come from far denser, clipped productions).
     */
    val BY_STYLE_V2: Map<String, Float> = BY_STYLE + buildMap {
        for (s in listOf("techno", "acid_techno", "industrial_techno", "acidcore")) put(s, -7.5f)
        for (s in listOf("dnb", "jump_up", "jungle", "neurofunk", "dubstep", "riddim")) put(s, -7.5f)
        put("gabber", -7f)
        for (s in listOf("deep_house", "afro_house", "amapiano", "disco")) put(s, -9f)
        put("hard_techno", -6f)
        put("phonk", -7f)
        for (s in listOf("trap", "drill")) put(s, -8.5f)
        for (s in listOf("pop", "afrobeats", "reggaeton")) put(s, -9f)
    }

    /** The style's loudness ([DEFAULT_LUFS] without one, or for a style not in the table), by mix version. */
    fun forStyle(style: String?, mixVersion: Int = Project.MIX_V1): Float =
        style?.let { (if (mixVersion >= Project.MIX_V2) BY_STYLE_V2 else BY_STYLE)[it] } ?: DEFAULT_LUFS

    /** The LUFS the engine aims at for [target] in a project of [style]; null: no target. */
    fun lufs(target: LoudnessTarget, style: String?, mixVersion: Int = Project.MIX_V1): Float? = when (target) {
        LoudnessTarget.OFF -> null
        LoudnessTarget.AUTO -> forStyle(style, mixVersion)
        // Final audit (report 04): « Club » never quieter than the style's own (drum & bass at −7.5, hard techno at −6).
        LoudnessTarget.CLUB -> clubLufs(forStyle(style, mixVersion))
        LoudnessTarget.STREAMING -> STREAMING_LUFS
    }

    /** « Club » for a project whose « Auto » aims at [styleLufs]: [CLUB_LUFS], or the style's own when louder. */
    fun clubLufs(styleLufs: Float): Float = maxOf(CLUB_LUFS, styleLufs)

    /** "−8,1" (one decimal, a true minus sign, the language's decimal mark). */
    fun format(value: Double): String =
        String.format(com.pocketstudio.model.i18n.I18n.lang.locale, "%.1f", value).replace('-', '−')

    /** A target's value: "−8", or "−8,5" when it has a half. */
    fun formatTarget(value: Float): String =
        if (value == Math.round(value).toFloat()) "${Math.round(value)}".replace('-', '−') else format(value.toDouble())
}
