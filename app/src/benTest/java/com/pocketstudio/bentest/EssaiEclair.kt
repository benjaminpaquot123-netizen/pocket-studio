package com.pocketstudio.bentest

import com.pocketstudio.app.journey.Journey
import com.pocketstudio.model.StarterWizard
import java.io.File
import java.util.Locale

/** The sound engine's numbers, as the robot reads them (NativeEngine on the phone, made up in the tests). */
interface Sonde {
    /** [playing]: the transport runs (the engine's own word, not the screen's). */
    data class Lecture(
        val xruns: Int, val cpu: Float, val peak: Float, val nonFinite: Boolean, val running: Boolean, val playing: Boolean = false,
        /** T1: the master's short-term loudness (LUFS), the live loudness gain (dB), the song's curve on the master (0..1, null: none). */
        val lufs: Float = Float.NaN, val drive: Float = Float.NaN, val lane: Float? = null,
    )

    fun lire(): Lecture
}

/** What the robot needs of the app besides its screens. */
interface Appli {
    /** Opens the studio (the real app's screen) in front. */
    fun ouvrirStudio()

    /** The export as the app shows it: running, its files, its error. */
    fun export(): ExportVu

    /** A picture of every window of the app, written in [file] (JPEG); false when it could not be taken. */
    fun capture(file: File): Boolean

    /** BenStudio Test's screen in front with [question]; waits (at most [maxMs]) for the answer, null without one. */
    fun demander(question: String, maxMs: Long): Boolean?

    /** [seconds]: the length the app measured on its file (null: none). */
    data class ExportVu(val running: Boolean, val files: List<File>, val error: String?, val seconds: Double? = null)

    // T1 (« Tout tester »): what the robot reads of the phone and the app besides the screens. The defaults: nothing known.

    /** The video export (« Vidéo pour les réseaux »): running, its file, its error. */
    fun video(): VideoVu = VideoVu(false, null, null)

    /** [file] read back as a player reads it; null: unreadable. */
    fun relire(file: File): Relecture.Relu? = null

    /** The phone's heat: battery temperature, thermal status, headroom. */
    fun chaleur(): Chaleur? = null

    /** One round-trip latency measure (ms), null: failed (its reason in [latenceEchec]). */
    fun latence(): Double? = null

    /** What the last failed [latence] said. */
    fun latenceEchec(): String = ""

    /** The app's memory (PSS, MB). */
    fun memoireMo(): Int? = null

    /** Starts counting the studio's frames; [imagesFin]: drawn, slow (over 16.7 ms). */
    fun imagesDebut() = Unit
    fun imagesFin(): Pair<Int, Int>? = null

    /** The studio turned (true: lying, false: standing, null: as the phone wants). */
    fun orienter(couche: Boolean?) = Unit

    /** The screen is on. */
    fun ecranAllume(): Boolean = true

    /** The studio's screen kept on (the robot's touches are not a finger's) or left to go off by itself. */
    fun garderEcran(on: Boolean) = Unit

    /** Stops what plays, without a touch (the screen may be off). */
    fun arreterLecture() = Unit

    data class VideoVu(val running: Boolean, val file: File?, val error: String?, val seconds: Int? = null)

    /** [batterieC]: °C; [statut]: PowerManager's thermal status (0 none … 6 shutdown); [marge]: thermal headroom (1 = throttling). */
    data class Chaleur(val batterieC: Float?, val statut: Int?, val marge: Float?) {
        fun text(): String = listOfNotNull(
            batterieC?.let { String.format(Locale.ROOT, "batterie %.1f °C", it) },
            statut?.let { "bridage " + (STATUTS.getOrNull(it) ?: "$it") },
            marge?.takeIf { it.isFinite() }?.let { String.format(Locale.ROOT, "marge %.2f", it) },
        ).joinToString(", ").ifEmpty { "chaleur inconnue" }

        fun mesure(): String = "batterie=${batterieC ?: ""};statut=${statut ?: ""};marge=${marge?.takeIf { it.isFinite() } ?: ""}"

        companion object {
            val STATUTS = listOf("aucun", "léger", "modéré", "fort", "critique", "urgence", "arrêt")
        }
    }
}

/**
 * The « essai éclair » (docs/PLAN-TEST-APK.md, T0): five minutes that prove the robot works on
 * Benjamin's phone before the rest is built. Three styles made by the assistant and played, one
 * export, five screens visited (a picture of each), one question. Each step is short, has its
 * longest time, and says what it saw in a few words.
 *
 * Each step starts from whatever is on screen (Benjamin's first run, 2026-11-11: a first launch, the
 * welcome over the studio, the Mode simple): the welcome is answered, a song is made from « Que
 * veux-tu faire ? » as a Pro project (the same path in both modes), the Pro tabs are asked for
 * before they are touched, and the question is only asked once a song was heard by the engine.
 * A step that fails writes what was on screen ([ecran]) in the logbook.
 */
class EssaiEclair(
    private val j: Journey,
    private val sonde: Sonde,
    private val appli: Appli,
    private val carnet: Carnet,
    private val captures: File,
    private val ecouteMs: Long = 30_000,
    /** The tags on screen, the top window's first (the robot's driver knows the windows; any driver's tags otherwise). */
    private val ecran: () -> List<String> = { j.tagsMatching(Regex(".+")) },
    /** How long ▶ may take to start the transport, and the sound to come out once it runs. */
    private val demarrageMs: Long = 10_000,
) {
    fun etapes(): List<Etape> = listOf(
        etape("ouverture", "Ouverture du studio", 120_000) { ouverture() },
    ) + STYLES.map { s -> etape("style_$s", "Style ${StarterWizard.style(s)?.name ?: s}", 360_000) { style(s) } } + listOf(
        etape("export", "Export M4A", 300_000) { export() },
        etape("ecrans", "Cinq écrans", 120_000) { ecrans() },
        etape("question", "Une question", 600_000) { question() },
    )

    /** A step whose failure leaves what the robot saw in the logbook. */
    private fun etape(name: String, label: String, maxMs: Long, work: () -> String) = Etape(name, label, maxMs) {
        try {
            work()
        } catch (t: Throwable) {
            noterEcran(name)
            throw t
        }
    }

    /** « à l'écran : … »: at most [TAGS_MAX] tags, the known screens' first, numbered ones folded (pad_3 → pad_*). */
    fun noterEcran(step: String) {
        runCatching {
            val all = ecran().map { it.replace(Regex("\\d+"), "*") }.distinct()
            val known = all.filter { it in REPERES }
            val shown = (known + (all - known.toSet())).take(TAGS_MAX)
            carnet.add(Carnet.NOTE, step, "à l'écran : " + (if (shown.isEmpty()) "rien" else shown.joinToString(", ")) + (if (all.size > shown.size) " (+${all.size - shown.size})" else ""))
        }.onFailure { carnet.add(Carnet.NOTE, step, "à l'écran : illisible (${it.message?.take(80)})") }
    }

    /**
     * The studio in front, past the opening and what it holds back: at a first launch the welcome
     * only shows once the opening is over (the studio is already there underneath), « Plus tard ».
     */
    fun ouverture(): String {
        appli.ouvrirStudio()
        // The studio's own screen first: the robot starts from BenStudio Test's, where nothing of it is.
        j.awaitAny("intro", "welcome", "launch", *STUDIO, timeoutMs = 60_000)
        j.awaitGone("intro", 60_000)
        // The welcome and « Que veux-tu faire ? » come a moment after the opening leaves.
        val first = runCatching { j.awaitAny("welcome", "launch", timeoutMs = APRES_OUVERTURE_MS) }.getOrNull()
        return when (first) {
            "welcome" -> {
                plusTard()
                j.awaitAny(*STUDIO, timeoutMs = 30_000)
                "premier lancement (« Plus tard »), studio ouvert en ${mode()}"
            }
            "launch" -> "accueil « Que veux-tu faire ? »"
            else -> {
                j.awaitAny(*STUDIO, timeoutMs = 30_000)
                "studio ouvert en ${mode()}"
            }
        }
    }

    /**
     * « Que veux-tu faire ? » › Nouveau morceau › the assistant › Pro › [style] › Créer maintenant, then
     * the song played (the engine says it plays and sound comes out) [ecouteMs], measured 4 times a second.
     */
    fun style(style: String): String {
        versCreer(style)
        j.createNow()
        j.await("guide_card_start.ready", 300_000)
        val by = lancerLecture()
        val m = mesurer("style_$style", ecouteMs)
        arreter()
        if (j.exists("guide_panel")) runCatching { j.tap("guide_bar") }
        check(m.reads > 0) { "aucune mesure" }
        check(!m.nonFinite) { "valeur invalide (NaN) dans le son" }
        check(m.loudSeconds > 0) { "silence : ${m.text()}" }
        check(m.playing * 2 >= m.reads) { "la lecture s'est arrêtée toute seule : ${m.text()}" }
        return "lecture par $by : ${m.text()}"
    }

    /** From whatever is on screen to the assistant's « Créer maintenant » for [style], as a Pro project. */
    fun versCreer(style: String) {
        allerAuChoix()
        j.newSongInAssistant(simple = false)
        j.chooseStyle(style, StarterWizard.style(style)?.family ?: error("unknown style $style"))
    }

    /**
     * ▶, then the proof that it plays: the engine's transport runs and its output level is not zero.
     * « Écouter le drop » of the Guide's card first, else the song's ▶. Returns the tag that started it.
     */
    fun lancerLecture(): String {
        val touched = ArrayList<String>()
        if (!sonde.lire().playing) {
            for (tag in PLAY) {
                if (!j.exists(tag)) continue
                j.tap(tag)
                touched += tag
                if (attendre(demarrageMs) { sonde.lire().playing }) break
            }
        }
        val r = sonde.lire()
        check(r.playing) { "la lecture ne démarre pas (touché : ${touched.ifEmpty { listOf("rien") }.joinToString(", ")} ; moteur ${if (r.running) "en marche" else "arrêté"})" }
        var peak = 0f
        check(attendre(demarrageMs) { peak = maxOf(peak, sonde.lire().peak); peak > NIVEAU_MIN }) { "lecture lancée (${touched.lastOrNull() ?: "déjà"}) mais aucun son ne sort : crête ${fmt(peak.toDouble())}" }
        return touched.lastOrNull() ?: "déjà en lecture"
    }

    private fun attendre(ms: Long, ok: () -> Boolean): Boolean {
        val end = System.currentTimeMillis() + ms
        while (!ok()) {
            if (System.currentTimeMillis() >= end) return false
            j.idle(100)
        }
        return true
    }

    /** Mix › Exporter › « La scène » en M4A: the file there, read back by its size. */
    fun export(): String {
        val started = System.currentTimeMillis()
        auStudioPro()
        j.tap("tab_Mix")
        j.await("mixer")
        j.tap("export_what_Loop")
        j.tap("export_format_M4a")
        j.tap("export")
        var e = appli.export()
        // The touch is proven by the export starting (or its file there), not waited for until the step's end.
        val startBy = System.currentTimeMillis() + 15_000
        while (!e.running && e.files.none { it.extension == "m4a" && it.lastModified() >= started - 2_000 }) {
            check(e.error == null) { "export : ${e.error}" }
            check(System.currentTimeMillis() < startBy) { "l'export n'a pas démarré 15 s après « Exporter »" }
            j.idle(250)
            e = appli.export()
        }
        while (e.running || e.files.none { it.extension == "m4a" && it.lastModified() >= started - 2_000 }) {
            check(e.error == null) { "export : ${e.error}" }
            j.idle(250)
            e = appli.export()
        }
        val file = e.files.filter { it.extension == "m4a" }.maxBy { it.lastModified() }
        check(file.length() > 10_000) { "fichier trop petit : ${file.length()} octets" }
        val took = (System.currentTimeMillis() - started) / 1000.0
        carnet.add(Carnet.MESURE, "export", "m4a;octets=${file.length()};secondes=${fmt(took)}")
        return "M4A de ${file.length() / 1024} Ko en ${fmt(took)} s"
    }

    /** Structure, Sons, Mix, Live, Mes projets: each one opened, recognised, pictured. */
    fun ecrans(): String {
        auStudioPro()
        val seen = ArrayList<String>()
        for ((name, open, mark) in SCREENS) {
            // Mes projets opens from the top bar, which some spaces (Live) leave out: from Structure then.
            if (!j.exists(open)) j.tap("tab_Structure")
            j.tap(open)
            if (mark.endsWith("_")) {
                val end = System.currentTimeMillis() + 15_000
                while (j.tagsMatching(Regex(Regex.escape(mark) + ".+")).isEmpty()) {
                    check(System.currentTimeMillis() < end) { "$name : écran pas reconnu" }
                    j.idle(100)
                }
            } else {
                j.await(mark)
            }
            j.idle(600)
            val shot = File(captures, "ecran-${seen.size + 1}-$name.jpg")
            if (!appli.capture(shot)) carnet.add(Carnet.NOTE, "ecrans", "$name : pas de capture")
            seen += name
        }
        // Back to the song (« ← », then « Continuer »).
        j.tap("projects_back")
        j.tap("launch_continue")
        return "vus : ${seen.joinToString(", ")}"
    }

    fun question(): String {
        // Only once the engine played a song: with none, « non » would say nothing of the phone's sound.
        val heard = carnet.lines().count { it.kind == Carnet.REUSSI && it.step.startsWith("style_") }
        check(heard > 0) { "pas posée : aucun morceau n'a joué" }
        val answer = appli.demander(QUESTION, 600_000)
        carnet.add(Carnet.NOTE, "question", "$QUESTION ${answer?.let { if (it) "oui" else "non" } ?: "pas de réponse"}")
        check(answer != null) { "pas de réponse" }
        check(answer) { "Benjamin n'a pas entendu la musique" }
        return "oui, la musique sortait du téléphone"
    }

    /** The first launch's welcome: « Plus tard » (the starter beat, the mode it suggests). */
    private fun plusTard() {
        // Below the fold on the phone (2026-11-12): scrolled to by the touch, which is then proven by the welcome
        // leaving; one more touch when it stays (a list still moving takes a touch as « stop »).
        j.tap("welcome_later")
        if (runCatching { j.awaitGone("welcome", 5_000) }.isFailure) {
            carnet.add(Carnet.NOTE, "robot", "« Plus tard » touché, la bienvenue reste : nouveau toucher")
            j.tap("welcome_later")
            j.awaitGone("welcome")
        }
    }

    /** The assistant left open (a style that failed) covers the studio: closed first (« ✕ », « Quitter » when asked). */
    internal fun fermerAssistant() {
        if (j.exists("wizard")) {
            j.tap("wizard_close")
            if (runCatching { j.await("wizard_close_confirm", 2_000) }.isSuccess) j.tap("wizard_close_confirm")
            j.awaitGone("wizard")
        }
        // « Nouveau morceau », under the assistant, covers the studio too: « ← ».
        if (j.exists("new_project")) {
            j.tap("new_project_back")
            j.awaitGone("new_project")
        }
    }

    internal fun mode() = if (j.exists("tab_simple_Morceau")) "Mode simple" else "Pro"

    /** From anywhere (the welcome, the studio in either mode, Mes projets) to « Que veux-tu faire ? » (Mes projets › ←). */
    internal fun allerAuChoix() {
        if (j.exists("welcome")) plusTard()
        fermerAssistant()
        if (j.exists("launch")) return
        if (!j.exists("projects")) {
            if (j.exists("guide_panel")) runCatching { j.tap("guide_bar") }
            // Mes projets opens from the top bar, which some spaces (Live) leave out: from the song's then.
            if (!j.exists("top_projects")) j.tap(if (j.exists("tab_simple_Morceau")) "tab_simple_Morceau" else "tab_Structure")
            j.tap("top_projects")
            j.await("projects")
        }
        j.tap("projects_back")
        j.await("launch")
    }

    /** From anywhere to the open song with the Pro tabs (the top bar's mode chip › Pro when the app is in the Mode simple). */
    internal fun auStudioPro() {
        if (j.exists("welcome")) plusTard()
        fermerAssistant()
        if (j.exists("projects") && !j.exists("launch")) {
            j.tap("projects_back")
            j.await("launch")
        }
        if (j.exists("launch")) j.continueLastProject()
        j.awaitAny(*STUDIO, timeoutMs = 30_000)
        if (j.exists("guide_panel")) runCatching { j.tap("guide_bar") }
        if (!j.exists("tab_Mix")) {
            j.tap("top_mode")
            j.tap("mode_card_pro")
            j.awaitGone("mode_switch")
            j.await("tab_Mix")
        }
    }

    /** ■ on what plays (the thumb bar's or the top bar's « le morceau »). */
    internal fun arreter() {
        if (!sonde.lire().playing) return
        runCatching { j.tap(if (j.exists("thumb_play")) "thumb_play" else "top_song") }
        attendre(5_000) { !sonde.lire().playing }
    }

    /** [playing]: the readings the transport ran in. */
    data class Mesure(val reads: Int, val xruns: Int, val cpuMax: Float, val cpuMean: Float, val peakMax: Float, val loudSeconds: Int, val nonFinite: Boolean, val playing: Int = 0) {
        fun text() = "${reads / 4} s (en lecture ${playing / 4} s), $xruns décrochage(s), charge moyenne ${pct(cpuMean)} (pire ${pct(cpuMax)}), crête ${fmt(peakMax.toDouble())}"
    }

    /** [ms] of sound read 4 times a second, each read in the logbook. */
    fun mesurer(step: String, ms: Long): Mesure {
        val first = sonde.lire()
        var reads = 0
        var cpuMax = 0f
        var cpuSum = 0f
        var peakMax = 0f
        var nonFinite = false
        var loud = 0
        var playing = 0
        var secondPeak = 0f
        var last = first
        val end = System.currentTimeMillis() + ms
        while (System.currentTimeMillis() < end) {
            j.idle(250)
            val r = sonde.lire()
            reads++
            cpuMax = maxOf(cpuMax, r.cpu)
            cpuSum += r.cpu
            peakMax = maxOf(peakMax, r.peak)
            secondPeak = maxOf(secondPeak, r.peak)
            nonFinite = nonFinite || r.nonFinite
            if (r.playing) playing++
            if (reads % 4 == 0) {
                if (secondPeak > 0.01f) loud++
                secondPeak = 0f
            }
            carnet.add(Carnet.MESURE, step, "xruns=${r.xruns};cpu=${fmt(r.cpu.toDouble())};peak=${fmt(r.peak.toDouble())};running=${r.running};playing=${r.playing}")
            last = r
        }
        return Mesure(reads, last.xruns - first.xruns, cpuMax, if (reads == 0) 0f else cpuSum / reads, peakMax, loud, nonFinite, playing)
    }

    companion object {
        /** Three of the heavy and the common: jump up (the heaviest), house, techno. */
        val STYLES = listOf("jump_up", "house", "techno")

        /** Tags that say the studio is there (the bottom bar or the side rail). */
        val STUDIO = arrayOf("space_bar", "space_rail")

        /** name, the tag that opens it, the tag that says it is there (ending in _: a prefix). */
        val SCREENS = listOf(
            Triple("structure", "tab_Structure", "structure_parts"),
            Triple("sons", "tab_Sons", "track_"),
            Triple("mix", "tab_Mix", "mixer"),
            Triple("live", "tab_Live", "live_screen"),
            Triple("projets", "top_projects", "projects"),
        )

        /** What starts the song, in the order tried: the Guide's « Écouter le drop », the thumb bar's ▶, the top bar's « le morceau ». */
        val PLAY = listOf("guide_option_drop", "thumb_play", "top_song")

        /** An output peak above this is sound (-60 dBFS). */
        const val NIVEAU_MIN = 0.001f

        /** How long the welcome or « Que veux-tu faire ? » may take to show once the opening is gone. */
        const val APRES_OUVERTURE_MS = 3_000L

        /** A failed step's note: at most this many tags. */
        const val TAGS_MAX = 20

        /** The tags that name a screen, said first in a failed step's note. */
        val REPERES = setOf(
            "intro", "welcome", "launch", "projects", "wizard", "mode_switch", "guide_panel", "space_bar", "space_rail",
            "tab_Structure", "tab_Mix", "tab_simple_Morceau", "tab_simple_Mix", "top_projects", "top_mode", "thumb_play", "top_song",
            "mixer", "simple_mix", "share_sheet",
        )

        const val QUESTION = "Pendant l'essai, as-tu entendu la musique sortir du téléphone ?"

        fun fmt(v: Double) = String.format(Locale.ROOT, "%.2f", v)
        fun pct(v: Float) = String.format(Locale.ROOT, "%.0f %%", v * 100)
    }
}
