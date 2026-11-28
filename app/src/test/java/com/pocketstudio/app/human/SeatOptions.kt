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
 * - `rejeu.attendu=` / `rejeu.absent=` (audit method v2, scripts/bench/rejeu-defauts.sh): a replay test of a fixed defect:
 *   the gestures are played without comparing screens (the fix changes them), then the screen and sound after the last one
 *   must hold the « attendu » text and not the « absent » one;
 * - `rejeu.origine=<seat folder>` (PLAN-RESTE 7.5, written by rejeu-defauts.sh from oracle.txt's `source=`): the seat the
 *   replay was made from (its journal and screens), to say in the verdict what differs from it (by default `rejeu=`);
 * - `rejeu.puis=main` (METHODE-V4 §4.5, « siège court ancré »): the replay goes on while the screen sticks to the origin's,
 *   then gives the seat to its tester (instead of a verdict): screen differs, origin's gestures over, `rejeu.n` reached or
 *   a tag gone; said « #N REJEU main rendue » in the journal and in rejeu.txt;
 * - `amenee=tuto:<style>:<chapitre>` (the same): the tutorial of that style started from « J'apprends » at the mode's
 *   level, then its chapter (from 1) chosen in « Chapitres »;
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
    val replayExpect: String? = null,
    val replayAbsent: String? = null,
    val replayOrigin: File? = null,
    val replayHandBack: Boolean = false,
) {
    /** A box on the screen, in pixels. */
    data class Box(val left: Float, val top: Float, val right: Float, val bottom: Float)

    enum class Hand(val word: String) { BOTH("à deux mains"), RIGHT("d'une main, la droite"), LEFT("d'une main, la gauche") }

    /** Robolectric's qualifiers for this seat: the phone's locale, then its screen. */
    val qualifiers: String get() = "${localeQualifier(lang)}-${DEVICES.getValue(device)}"

    val fontScale: Float get() = font / 100f

    /** One line for the journal. */
    fun describe(): String = "lang=${lang.code} device=$device font=$font mode=${simple?.let { if (it) "simple" else "pro" } ?: "-"} " +
        "theme=${theme?.name ?: "-"} fresh=${if (fresh) "reel" else "non"} gestes.max=${maxGestures ?: "-"} lit=${if (reads) "oui" else "non"} " +
        "main=${hand.name.lowercase()} amenee=${amenee ?: "-"}" + (replayOf?.let { " rejeu=${it.path} rejeu.n=$replayN" } ?: "") +
        (if (replayHandBack) " rejeu.puis=main" else "")

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

        /** The style and chapter (from 1) of `amenee=tuto:<style>:<chapitre>`; null for another amenée or a wrong one. */
        fun tutoAmenee(name: String): Pair<String, Int>? {
            val parts = name.split(':')
            if (parts.size != 3 || parts[0] != "tuto") return null
            val chapter = parts[2].toIntOrNull()?.takeIf { it >= 1 } ?: return null
            return parts[1].takeIf { it in com.pocketstudio.model.tuto.TutoCourse.STYLES }?.let { it to chapter }
        }

        /** Why a tutorial's amenée cannot be played at [level] (its chapter past the course), null when it can. */
        fun tutoAmeneeError(name: String, level: com.pocketstudio.model.tuto.TutoLevel): String? {
            val (style, chapter) = tutoAmenee(name) ?: return "amenée $name : tuto:<style>:<chapitre>"
            val count = com.pocketstudio.model.tuto.TutoCourse.of(style)?.chaptersFor(level)?.size ?: 0
            return if (chapter > count) "amenée $name : le tuto $style ($level) a $count chapitres" else null
        }

        /**
         * With `rejeu.puis=main`: why the replay gives the seat to its tester at turn [n] (null: it goes on): screen [n]
         * differs from the origin's ([diverged]), the origin has no gesture [n], `rejeu.n` ([replayN], 0: no bound) is past,
         * or the gesture's tag is not on the screen ([missingTag]).
         */
        fun handBackWhy(n: Int, replayN: Int, diverged: Boolean, originHasGesture: Boolean, missingTag: String? = null, gone: String? = null): String? = when {
            diverged -> "écran $n différent de l'origine (après le geste ${n - 1})"
            replayN in 1 until n -> "les $replayN gestes du rejeu joués"
            !originHasGesture -> "plus de geste à l'origine (pas $n)"
            missingTag != null -> "étiquette « $missingTag » introuvable au geste $n"
            gone != null -> "bouton « $gone » plus sous le doigt au geste $n"
            else -> null
        }

        /**
         * Tour 7, banc B2: the test tag a replayed touch aims at when the origin's finger was on a tagged button ([was], its
         * `#n DOIGT` line) and the same pixels are no longer on it ([now], the finger's line at the replay): the button moved.
         * Null when nothing tagged was aimed at, or when it is still under the finger.
         */
        fun reaimTag(was: String?, now: String, actionables: (String) -> Int = { 1 }): String? {
            fun key(u: String) = u.removePrefix("désactivé ").removePrefix("dans:").substringBefore(" «").trim()
            val k = was?.let(::key) ?: return null
            if (k.isEmpty() || k.startsWith("rien") || k.startsWith("#") || k == "vue" || k.contains(' ')) return null
            // Tour 8, banc9: « dans:x » names a container; its middle is one of its buttons only when it holds one.
            if (was.removePrefix("désactivé ").startsWith("dans:") && actionables(k) != 1) return null
            return k.takeIf { key(now) != it }
        }

        /** Tour 7, banc B2: what `rejeu.txt` says when the replay gives the seat back at gesture [n] ([diff]: the line that changed). */
        fun handBackNote(n: Int, replayN: Int, why: String, diff: String?, originDone: Boolean = false): String = "main rendue au pas $n : $why" +
            if (originDone || (replayN > 0 && n > replayN)) "" else if (replayN == 0) " ; rejeu non fidèle (${n - 1} gestes de l'origine rejoués)" + (diff?.let { " ; écran changé : $it" } ?: "") else " ; rejeu non fidèle (${n - 1} gestes de l'origine sur $replayN rejoués)" + (diff?.let { " ; écran changé : $it" } ?: "")

        /**
         * Tour 8, banc9 (point f): the words of an untagged button the origin's finger was on ([was], its `#n DOIGT`
         * line: `#id`, `vue` or `dans:` an untouchable container) when they are no longer under the finger ([now]).
         */
        fun goneWords(was: String?, now: String): String? {
            val w = was?.removePrefix("désactivé ") ?: return null
            val k = w.substringBefore(" «").trim()
            if (!(k.startsWith("#") || k == "vue" || k.startsWith("dans:"))) return null
            val words = Regex("« (.*) »").find(w)?.groupValues?.get(1)?.takeIf { it.isNotBlank() } ?: return null
            return words.takeIf { "« $it »" !in now }
        }

        /**
         * Tour 8, banc9 (point f): whether the finger's line [under] (ScreenText.under) is on [tag]; [inside] are the tags
         * drawn inside it (`remix_pick` around `remix_pick_thumb`): a touch landing on one of them is on [tag] too.
         */
        fun fingerOn(tag: String, under: String, inside: Set<String> = emptySet()): Boolean {
            if (tag.startsWith("vue_")) return under.startsWith("vue « ${tag.removePrefix("vue_")} »")
            val name = under.removePrefix("désactivé ").removePrefix("dans:").substringBefore(' ')
            return if (tag.endsWith("*")) name.startsWith(tag.dropLast(1)) else name == tag || name in inside
        }

        /**
         * Tour 8, banc9 (points a, b): where to aim at a node whose box is [node] and whose drawn part (clipped by its
         * parents and the window) is [drawn]: the middle of the drawn part, or null when that part is too small to touch
         * (under 24 dp, or under the node itself when smaller).
         */
        fun drawnAim(node: Box, drawn: Box, w: Int, h: Int, density: Float): Pair<Float, Float>? {
            val l = maxOf(drawn.left, 0f)
            val t = maxOf(drawn.top, 0f)
            val r = minOf(drawn.right, w.toFloat())
            val b = minOf(drawn.bottom, h.toFloat())
            val minW = maxOf(minOf(24f * density, node.right - node.left), 1f)
            val minH = maxOf(minOf(24f * density, node.bottom - node.top), 1f)
            if (r - l < minW || b - t < minH) return null
            return (l + r) / 2f to (t + b) / 2f
        }

        /** Android's resource qualifier for [lang] (« pt-rBR », « in » for Indonesian). */
        fun localeQualifier(lang: Lang): String = when (lang) {
            Lang.PT_BR -> "pt-rBR"
            Lang.ZH_CN -> "zh-rCN"
            Lang.ID -> "in"
            // A phone always has a region: « en » alone took the libraries' en-rXC strings (audit ultra tour 0, seat b: « Drag handle » wrapped in markers).
            Lang.EN -> "en-rUS"
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
                    "amenee", "amenée" -> when {
                        v in AMENEES || tutoAmenee(v) != null -> o = o.copy(amenee = v)
                        v.startsWith("tuto:") -> warn.add("amenee=$raw : tuto:<style>:<chapitre> (${com.pocketstudio.model.tuto.TutoCourse.STYLES.joinToString()}, chapitre à partir de 1)")
                        else -> warn.add("amenee=$raw : ${AMENEES.joinToString()}, tuto:<style>:<chapitre>")
                    }
                    "timeout.minutes" -> v.toLongOrNull()?.let { o = o.copy(timeoutMinutes = it) }
                    // For scripts/bench/seat-metrics.py (the mission's oracle) and the dashboard: not the bench's.
                    "mission", "profil", "niveau" -> Unit
                    "rejeu" -> o = o.copy(replayOf = File(raw))
                    "rejeu.n" -> v.toIntOrNull()?.let { o = o.copy(replayN = it) }
                    "rejeu.attendu" -> o = o.copy(replayExpect = raw)
                    "rejeu.absent" -> o = o.copy(replayAbsent = raw)
                    "rejeu.origine" -> o = o.copy(replayOrigin = File(raw))
                    "rejeu.puis" -> if (v == "main") o = o.copy(replayHandBack = true) else warn.add("rejeu.puis=$raw : main")
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
         * The point ([x], [y], the screen's pixels) is on the screen [w] x [h] and within the thumb's reach of [hand] holding
         * the phone at its bottom corner (right hand: bottom right), at [density]. A point off the screen (below its bottom
         * edge: review of rejeux6, grave 2) is never reached, whatever the hand.
         */
        fun reachable(hand: Hand, x: Float, y: Float, w: Int, h: Int, density: Float): Boolean {
            if (x < 0f || y < 0f || x >= w || y >= h) return false
            if (hand == Hand.BOTH) return true
            val px = if (hand == Hand.RIGHT) w.toFloat() else 0f
            return hypot(x - px, y - h.toFloat()) <= THUMB_REACH_DP * density
        }

        /**
         * Where a tag touch lands: the point nearest to [middle] of the [node]'s box cut to its [visible] part (its parents'
         * clipping: a list that scrolls it, a window) and to the screen [w] x [h], that [hand]'s thumb reaches and where [hits]
         * says the element under the finger is the target (or a part of it): nothing else is drawn over it there. Null when
         * no such point (the touch is refused: a finger could not touch that element there). Review of rejeux6, grave 2.
         */
        fun nearestReachable(
            hand: Hand, node: Box, visible: Box, middle: Pair<Float, Float>, w: Int, h: Int, density: Float,
            hits: (Float, Float) -> Boolean = { _, _ -> true },
        ): Pair<Float, Float>? {
            val left = maxOf(node.left, visible.left, 0f)
            val top = maxOf(node.top, visible.top, 0f)
            val right = minOf(node.right, visible.right, w.toFloat())
            val bottom = minOf(node.bottom, visible.bottom, h.toFloat())
            if (right - left < 1f || bottom - top < 1f) return null
            val candidates = mutableListOf<Pair<Float, Float>>()
            if (middle.first in left..right && middle.second in top..bottom) candidates += middle
            for (i in 1..9) for (j in 1..9) candidates += (left + (right - left) * i / 10f) to (top + (bottom - top) * j / 10f)
            // Nearest first: the hit test (the screen's nodes under a point) is only asked until one holds.
            return candidates.sortedBy { hypot(it.first - middle.first, it.second - middle.second) }
                .firstOrNull { (x, y) -> reachable(hand, x, y, w, h, density) && hits(x, y) }
        }
    }
}

/**
 * Tour 8, banc9 (point f): the replay's screen lines that differ from the origin's. [now] is this turn's (null when the
 * screen sticks again), [last] the last one seen, for the replay's verdict.
 */
class ReplayDiffs {
    var last: String? = null
        private set
    var now: String? = null
        private set

    fun turn(diff: String?) {
        now = diff
        if (diff != null) last = diff
    }
}
