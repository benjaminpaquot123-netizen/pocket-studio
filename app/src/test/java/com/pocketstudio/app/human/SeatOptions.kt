package com.pocketstudio.app.human

import com.pocketstudio.app.settings.AppTheme
import com.pocketstudio.model.i18n.Lang
import java.io.File
import kotlin.math.hypot

/**
 * A seat's options (Audit ultra §3.1, « banc 2 »), read from its start.txt (`key=value` lines):
 *
 * - `lang=` one of the 15 codes ([Lang]): the interface's language (and the phone's locale);
 * - `device=` phone (the owner's Honor, the default), the five profiles of CaptureMatrixTest
 *   (phone360, phone411, phoneLand, tablet8, tablet11Land) or foldable (a Fold's inner screen);
 * - `font=100|130|200`: Android's font size (Settings › Display), in percent;
 * - `mode=simple|pro`, `theme=studio|night|daltonien`: the app's own settings, set before it opens;
 * - `fresh=reel`: nothing set ahead (no permission granted, no language chosen: the phone's locale
 *   is the seat's language), the welcome and the system's permission questions are played;
 * - `gestes.max=N`: the gesture budget (an image looked at counts half); past it the bench ends
 *   the seat with « abandon : budget »;
 * - `lit=non`: texts under 18 sp are grey bars on the image and « ▭▭▭ » in the screen's text;
 * - `main=une|gauche`: one hand (the right, or the left): touches beyond the thumb's reach are refused;
 * - `amenee=` a scripted start (premier-morceau, remix, padlive): the app is brought to the
 *   mission's starting point before the tester gets it;
 * - `timeout.minutes=` how long the bench waits for a gesture (20);
 * - `rejeu=<seat folder>` and `rejeu.n=` (scripts/human-replay.sh): the gestures of another seat;
 * - `mission=`, `profil=`, `niveau=`: for the oracle and the dashboard (scripts/bench/seat-metrics.py), not read here.
 */
internal data class SeatOptions(
    val lang: Lang = Lang.FR,
    val device: String = "phone",
    val font: Int = 100,
    /** true: Mode simple, false: Pro, null: as the app starts. */
    val simple: Boolean? = null,
    val theme: AppTheme? = null,
    val fresh: Boolean = false,
    val maxGestures: Int? = null,
    val reads: Boolean = true,
    val hand: Hand = Hand.BOTH,
    val amenee: String? = null,
    val timeoutMinutes: Long = 20,
    val replayOf: File? = null,
    val replayN: Int = 0,
) {
    enum class Hand(val word: String) { BOTH("à deux mains"), RIGHT("d'une main, la droite"), LEFT("d'une main, la gauche") }

    /** Robolectric's qualifiers for this seat: the phone's locale, then its screen. */
    val qualifiers: String get() = "${localeQualifier(lang)}-${DEVICES.getValue(device)}"

    val fontScale: Float get() = font / 100f

    /** One line for the journal. */
    fun describe(): String = "lang=${lang.code} device=$device font=$font mode=${simple?.let { if (it) "simple" else "pro" } ?: "-"} " +
        "theme=${theme?.name ?: "-"} fresh=${if (fresh) "reel" else "non"} gestes.max=${maxGestures ?: "-"} lit=${if (reads) "oui" else "non"} " +
        "main=${hand.name.lowercase()} amenee=${amenee ?: "-"}" + (replayOf?.let { " rejeu=${it.path} rejeu.n=$replayN" } ?: "")

    companion object {
        /**
         * The screens, as CaptureMatrixTest's profiles (their dp), all at 1.5 px a dp (hdpi) as the bench's
         * phone always was: images small enough for a tester to read (decision ULTRA-P0-A).
         */
        val DEVICES = linkedMapOf(
            "phone" to "w392dp-h852dp-port-hdpi",
            "phone360" to "w360dp-h780dp-port-hdpi",
            "phone411" to "w411dp-h914dp-port-hdpi",
            "phoneLand" to "w800dp-h360dp-land-hdpi",
            "tablet8" to "w600dp-h1006dp-port-hdpi",
            "tablet11Land" to "w1280dp-h800dp-land-hdpi",
            // A Galaxy Z Fold's inner screen, unfolded (690 x 829 dp).
            "foldable" to "w690dp-h829dp-port-hdpi",
        )
        private val DEVICE_ALIASES = mapOf("pliable" to "foldable", "honor" to "phone")

        val AMENEES = listOf("premier-morceau", "remix", "padlive")

        /** Android's resource qualifier for [lang] (« pt-rBR », « in » for Indonesian). */
        fun localeQualifier(lang: Lang): String = when (lang) {
            Lang.PT_BR -> "pt-rBR"
            Lang.ZH_CN -> "zh-rCN"
            Lang.ID -> "in"
            else -> lang.code
        }

        fun read(start: File): Pair<SeatOptions, List<String>> =
            parse(start.readLines().mapNotNull { l -> l.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0].trim() to it[1].trim() } }.toMap())

        /** The options of a start.txt, and what was not understood (said in the journal, never fatal). */
        fun parse(map: Map<String, String>): Pair<SeatOptions, List<String>> {
            val warn = mutableListOf<String>()
            var o = SeatOptions()
            for ((k, raw) in map) {
                val v = raw.lowercase()
                when (k.lowercase()) {
                    "lang" -> Lang.entries.firstOrNull { it.code.equals(raw, ignoreCase = true) }?.let { o = o.copy(lang = it) } ?: warn.add("lang=$raw inconnue")
                    "device" -> (DEVICE_ALIASES[v] ?: DEVICES.keys.firstOrNull { it.equals(raw, ignoreCase = true) })?.let { o = o.copy(device = it) }
                        ?: warn.add("device=$raw inconnu (${DEVICES.keys.joinToString()})")
                    "font" -> v.removeSuffix("%").toIntOrNull()?.takeIf { it in listOf(100, 130, 200) }?.let { o = o.copy(font = it) } ?: warn.add("font=$raw : 100, 130 ou 200")
                    "mode" -> when (v) { "simple" -> o = o.copy(simple = true); "pro" -> o = o.copy(simple = false); else -> warn.add("mode=$raw : simple ou pro") }
                    // The first bench's start.txt (2026-10-28) said simple=true|false.
                    "simple" -> o = o.copy(simple = v == "true" || v == "oui")
                    "theme" -> when (v) {
                        "studio" -> o = o.copy(theme = AppTheme.Studio)
                        "night", "nuit" -> o = o.copy(theme = AppTheme.Night)
                        "daltonien", "colorblind" -> o = o.copy(theme = AppTheme.Colorblind)
                        else -> warn.add("theme=$raw : studio, night ou daltonien")
                    }
                    "fresh" -> o = o.copy(fresh = v == "reel" || v == "réel" || v == "oui" || v == "true")
                    "gestes.max" -> v.toIntOrNull()?.takeIf { it > 0 }?.let { o = o.copy(maxGestures = it) } ?: warn.add("gestes.max=$raw : un nombre")
                    "lit" -> o = o.copy(reads = v != "non")
                    "main" -> when (v) { "une", "droite" -> o = o.copy(hand = Hand.RIGHT); "gauche" -> o = o.copy(hand = Hand.LEFT); "deux" -> o = o.copy(hand = Hand.BOTH); else -> warn.add("main=$raw : une ou gauche") }
                    "amenee", "amenée" -> if (v in AMENEES) o = o.copy(amenee = v) else warn.add("amenee=$raw : ${AMENEES.joinToString()}")
                    "timeout.minutes" -> v.toLongOrNull()?.let { o = o.copy(timeoutMinutes = it) }
                    // For scripts/bench/seat-metrics.py (the mission's oracle) and the dashboard: not the bench's.
                    "mission", "profil", "niveau" -> Unit
                    "rejeu" -> o = o.copy(replayOf = File(raw))
                    "rejeu.n" -> v.toIntOrNull()?.let { o = o.copy(replayN = it) }
                    else -> warn.add("option inconnue : $k")
                }
            }
            if (o.fresh && (o.simple != null || o.theme != null)) {
                warn.add("fresh=reel : mode et thème ne sont pas réglés d'avance (la personne les choisit)")
                o = o.copy(simple = null, theme = null)
            }
            return o to warn
        }

        /** How far a thumb reaches from its corner of the screen's bottom edge (≈ 80 mm), in dp. */
        const val THUMB_REACH_DP = 500f

        /**
         * The point ([x], [y], the screen's pixels) is within the thumb's reach of [hand] holding the
         * phone at its bottom corner (right hand: bottom right), on a screen [w] x [h] at [density].
         */
        fun reachable(hand: Hand, x: Float, y: Float, w: Int, h: Int, density: Float): Boolean {
            if (hand == Hand.BOTH) return true
            val px = if (hand == Hand.RIGHT) w.toFloat() else 0f
            return hypot(x - px, y - h.toFloat()) <= THUMB_REACH_DP * density
        }
    }
}
