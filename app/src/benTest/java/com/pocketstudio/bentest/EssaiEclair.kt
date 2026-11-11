package com.pocketstudio.bentest

import com.pocketstudio.app.journey.Journey
import com.pocketstudio.model.StarterWizard
import java.io.File
import java.util.Locale

/** The sound engine's numbers, as the robot reads them (NativeEngine on the phone, made up in the tests). */
interface Sonde {
    data class Lecture(val xruns: Int, val cpu: Float, val peak: Float, val nonFinite: Boolean, val running: Boolean)

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

    data class ExportVu(val running: Boolean, val files: List<File>, val error: String?)
}

/**
 * The « essai éclair » (docs/PLAN-TEST-APK.md, T0): five minutes that prove the robot works on
 * Benjamin's phone before the rest is built. Three styles made by the assistant and played, one
 * export, five screens visited (a picture of each), one question. Each step is short, has its
 * longest time, and says what it saw in a few words.
 */
class EssaiEclair(
    private val j: Journey,
    private val sonde: Sonde,
    private val appli: Appli,
    private val carnet: Carnet,
    private val captures: File,
    private val ecouteMs: Long = 30_000,
) {
    fun etapes(): List<Etape> = listOf(
        Etape("ouverture", "Ouverture du studio", 120_000) { ouverture() },
    ) + STYLES.map { s -> Etape("style_$s", "Style ${StarterWizard.style(s)?.name ?: s}", 360_000) { style(s) } } + listOf(
        Etape("export", "Export M4A", 300_000) { export() },
        Etape("ecrans", "Cinq écrans", 120_000) { ecrans() },
        Etape("question", "Une question", 600_000) { question() },
    )

    /** The studio in front, past the opening and its first questions (a first launch: « Plus tard », every tool). */
    fun ouverture(): String {
        appli.ouvrirStudio()
        j.awaitGone("intro", 60_000)
        return when (j.awaitAny("welcome", "launch", *STUDIO, timeoutMs = 60_000)) {
            "welcome" -> {
                j.firstLaunch(simple = false, guided = false)
                j.awaitAny(*STUDIO, timeoutMs = 30_000)
                "premier lancement, studio ouvert"
            }
            "launch" -> {
                j.continueLastProject()
                j.awaitAny(*STUDIO, timeoutMs = 30_000)
                "studio ouvert (Continuer)"
            }
            else -> "studio ouvert"
        }
    }

    /** « Que veux-tu faire ? » › the assistant › [style] › Créer maintenant, then the song played [ecouteMs] and measured 4 times a second. */
    fun style(style: String): String {
        allerAuChoix()
        j.newSongInAssistant()
        j.chooseStyle(style, StarterWizard.style(style)?.family ?: error("unknown style $style"))
        j.createNow()
        j.await("guide_card_start.ready", 300_000)
        j.tap("guide_option_drop")
        val m = mesurer("style_$style", ecouteMs)
        arreter()
        if (j.exists("guide_panel")) runCatching { j.tap("guide_bar") }
        check(m.reads > 0) { "aucune mesure" }
        check(!m.nonFinite) { "valeur invalide (NaN) dans le son" }
        check(m.loudSeconds > 0) { "silence : ${m.text()}" }
        return m.text()
    }

    /** Mix › Exporter › « La scène » en M4A: the file there, read back by its size. */
    fun export(): String {
        val started = System.currentTimeMillis()
        j.tap("tab_Mix")
        j.await("mixer")
        j.tap("export_what_Loop")
        j.tap("export_format_M4a")
        j.tap("export")
        var e = appli.export()
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
        val answer = appli.demander(QUESTION, 600_000)
        carnet.add(Carnet.NOTE, "question", "$QUESTION ${answer?.let { if (it) "oui" else "non" } ?: "pas de réponse"}")
        check(answer != null) { "pas de réponse" }
        check(answer) { "Benjamin n'a pas entendu la musique" }
        return "oui, la musique sortait du téléphone"
    }

    /** From anywhere in the studio to « Que veux-tu faire ? » (Mes projets › ←). */
    private fun allerAuChoix() {
        if (j.exists("launch")) return
        j.tap("top_projects")
        j.await("projects")
        j.tap("projects_back")
        j.await("launch")
    }

    /** ■ on what plays (the thumb bar's or the top bar's « le morceau »). */
    private fun arreter() {
        runCatching { j.tap(if (j.exists("thumb_play")) "thumb_play" else "top_song") }
    }

    data class Mesure(val reads: Int, val xruns: Int, val cpuMax: Float, val cpuMean: Float, val peakMax: Float, val loudSeconds: Int, val nonFinite: Boolean) {
        fun text() = "${reads / 4} s, $xruns décrochage(s), charge moyenne ${pct(cpuMean)} (pire ${pct(cpuMax)}), crête ${fmt(peakMax.toDouble())}"
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
            if (reads % 4 == 0) {
                if (secondPeak > 0.01f) loud++
                secondPeak = 0f
            }
            carnet.add(Carnet.MESURE, step, "xruns=${r.xruns};cpu=${fmt(r.cpu.toDouble())};peak=${fmt(r.peak.toDouble())};running=${r.running}")
            last = r
        }
        return Mesure(reads, last.xruns - first.xruns, cpuMax, if (reads == 0) 0f else cpuSum / reads, peakMax, loud, nonFinite)
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

        const val QUESTION = "Pendant l'essai, as-tu entendu la musique sortir du téléphone ?"

        fun fmt(v: Double) = String.format(Locale.ROOT, "%.2f", v)
        fun pct(v: Float) = String.format(Locale.ROOT, "%.0f %%", v * 100)
    }
}
