package com.pocketstudio.bentest

import com.pocketstudio.app.journey.Journey
import com.pocketstudio.model.StarterWizard
import java.io.File
import java.util.Locale
import kotlin.math.abs

/**
 * « Tout tester » (docs/PLAN-TEST-APK.md, niveau 1, T1): the robot measures, Benjamin does nothing.
 * The 37 styles played live 45 s each (jump up and dnb at the start, cold, and again at the end,
 * hot, the phone's heat noted), the exports (M4A, WAV, FLAC, the 9:16 video) read back, the
 * round-trip latency, the Guide followed to its end on 3 styles, the memory and the slow frames of
 * each screen, the turn, and last the song playing on with the screen off. Each step leaves its
 * light ([Feu]) in the logbook. Same robot, logbook and report as the essai éclair ([EssaiEclair]).
 *
 * Left for T1b: the speaker's curve (« le téléphone s'écoute »), the export's fingerprint against
 * the Linux bench, the Remix.
 */
class ToutTester(
    private val j: Journey,
    private val sonde: Sonde,
    private val appli: Appli,
    private val carnet: Carnet,
    private val captures: File,
    private val ecouteMs: Long = 45_000,
    private val ecran: () -> List<String> = { j.tagsMatching(Regex(".+")) },
    private val styles: List<String> = StarterWizard.STYLES.map { it.id },
    /** The screen off: how long it may take to go off by itself, then how long it is listened to. */
    private val attenteEteintMs: Long = 300_000,
    private val eteintMs: Long = 180_000,
    /** The Guide: how long a card may take to move once touched. */
    private val carteMs: Long = 10_000,
) {
    private val e = EssaiEclair(j, sonde, appli, carnet, captures, ecouteMs, ecran)

    fun etapes(): List<Etape> = buildList {
        add(etape("ouverture", "Ouverture du studio", 120_000) { e.ouverture() })
        for (s in LOURDS) add(etapeStyle(s, "froid"))
        for (s in styles.filter { it !in LOURDS }) add(etapeStyle(s, null))
        // dnb then jump up: jump up is the song open for the exports.
        for (s in LOURDS.reversed()) add(etapeStyle(s, "chaud"))
        for ((f, label) in FORMATS) add(etape("export_$f", "Export $label", 600_000) { export(f) })
        add(etape("latence", "Latence aller-retour", 120_000) { latence() })
        for (s in GUIDE) add(etape("guide_$s", "Guide sur ${nom(s)}", 480_000) { guide(s) })
        add(etape("images", "Mémoire et images lentes", 240_000) { images() })
        add(etape("rotation", "Rotation", 120_000) { rotation() })
        add(etape("ecran_eteint", "Écran éteint", attenteEteintMs + eteintMs + 240_000) { ecranEteint() })
    }

    private fun nom(style: String) = StarterWizard.style(style)?.name ?: style

    private fun etapeStyle(s: String, phase: String?): Etape {
        val name = "style_$s" + (phase?.let { "_$it" } ?: "")
        val label = "Style ${nom(s)}" + (phase?.let { " ($it)" } ?: "")
        return etape(name, label, 360_000) { style(name, s) }
    }

    private fun etape(name: String, label: String, maxMs: Long, work: () -> String) = Etape(name, label, maxMs) {
        try {
            work()
        } catch (t: Throwable) {
            e.noterEcran(name)
            throw t
        }
    }

    private fun feu(step: String, f: Pair<Feu, String>) = carnet.add(Carnet.FEU, step, f.first.mot + ";" + f.second)

    private fun chaleur(step: String): String {
        val c = appli.chaleur() ?: return ""
        carnet.add(Carnet.MESURE, step, "chaleur;" + c.mesure())
        return c.text()
    }

    /** The assistant's song of [style] made, played [ecouteMs] and heard: its light from the dropouts, the worst block, the alerts. */
    fun style(step: String, style: String): String {
        e.versCreer(style)
        j.createNow()
        j.await("guide_card_start.ready", 300_000)
        val heat = chaleur(step)
        val by = e.lancerLecture()
        val b = ecouter(step, ecouteMs)
        e.arreter()
        if (j.exists("guide_panel")) runCatching { j.tap("guide_bar") }
        feu(step, Feux.style(b))
        check(b.reads > 0) { "aucune mesure" }
        check(b.playing * 2 >= b.reads) { "la lecture s'est arrêtée toute seule : ${b.text()}" }
        check(b.secondes.any { it.peakDb > Ecoute.SILENCE_DB }) { "silence : ${b.text()}" }
        return "lecture par $by : ${b.text()}" + (if (heat.isNotEmpty()) " ; $heat" else "")
    }

    /** [ms] of what plays, read 4 times a second ([Ecoute.Lu] each in the logbook), then the level of each second. */
    fun ecouter(step: String, ms: Long, also: (Ecoute.Lu) -> Unit = {}): Ecoute.Bilan {
        val first = sonde.lire()
        val reads = ArrayList<Ecoute.Lu>()
        val end = System.currentTimeMillis() + ms
        while (System.currentTimeMillis() < end) {
            j.idle(250)
            val r = sonde.lire()
            val lu = Ecoute.Lu(System.currentTimeMillis(), r.xruns, r.cpu, r.peak, r.nonFinite, r.playing, r.lufs, r.drive, r.lane)
            reads += lu
            also(lu)
            carnet.add(
                Carnet.MESURE, step,
                "xruns=${r.xruns};cpu=${fmt(r.cpu)};peak=${fmt(r.peak)};lufs=${fmt(r.lufs)};drive=${fmt(r.drive)};lane=${r.lane?.let(::fmt) ?: ""};running=${r.running};playing=${r.playing}",
            )
        }
        val b = Ecoute.bilan(reads, first.xruns)
        carnet.add(Carnet.MESURE, step, "niveaux=${b.niveaux()}")
        return b
    }

    /**
     * Mix › Exporter › « La scène » in [format] (m4a, wav, flac), or the 9:16 video: the file read
     * back, its length against the app's, its true peak, the time it took against the time planned.
     */
    fun export(format: String): String {
        val started = System.currentTimeMillis()
        e.auStudioPro()
        if (format == "video") return video(started)
        j.tap("tab_Mix")
        j.await("mixer")
        j.tap("export_what_Loop")
        when (format) {
            "m4a" -> j.tap("export_format_M4a")
            "wav" -> j.tap("export_format_Wav")
            "flac" -> {
                // FLAC is in « Avancé », a chip that swaps with WAV: WAV first, then FLAC.
                j.tap("export_format_Wav")
                if (!j.exists("export_format_Flac")) j.tap("export_advanced")
                j.tap("export_format_Flac")
            }
        }
        j.tap("export")
        fun mine(v: Appli.ExportVu) = v.files.filter { it.extension == format && it.lastModified() >= started - 2_000 }
        var v = appli.export()
        val startBy = System.currentTimeMillis() + 15_000
        while (!v.running && mine(v).isEmpty()) {
            check(v.error == null) { "export : ${v.error}" }
            check(System.currentTimeMillis() < startBy) { "l'export n'a pas démarré 15 s après « Exporter »" }
            j.idle(250)
            v = appli.export()
        }
        while (v.running || mine(v).isEmpty()) {
            check(v.error == null) { "export : ${v.error}" }
            j.idle(250)
            v = appli.export()
        }
        val took = (System.currentTimeMillis() - started) / 1000.0
        val file = mine(v).maxBy { it.lastModified() }
        return relu("export_$format", format.uppercase(Locale.ROOT), file, took, v.seconds, PREVU.getValue(format))
    }

    /** Partager › « Vidéo pour les réseaux » › Créer: the MP4 read back (9:16 standing), then the share menu it opens left. */
    private fun video(started: Long): String {
        if (!j.exists("top_share")) j.tap("tab_Structure")
        j.tap("top_share")
        j.await("share_video", 30_000)
        j.tap("share_video")
        j.await("video_create", 30_000)
        j.tap("video_create")
        var v = appli.video()
        val startBy = System.currentTimeMillis() + 15_000
        while (!v.running && v.file == null) {
            check(v.error == null) { "vidéo : ${v.error}" }
            check(System.currentTimeMillis() < startBy) { "la vidéo n'a pas démarré 15 s après « Créer »" }
            j.idle(250)
            v = appli.video()
        }
        while (v.running || v.file == null) {
            check(v.error == null) { "vidéo : ${v.error}" }
            j.idle(250)
            v = appli.video()
        }
        val took = (System.currentTimeMillis() - started) / 1000.0
        // The share menu opens by itself once the video is made: the studio brought back over it.
        j.idle(1_500)
        appli.ouvrirStudio()
        j.awaitAny(*EssaiEclair.STUDIO, "video_sheet", timeoutMs = 30_000)
        if (j.exists("video_close")) runCatching { j.tap("video_close") }
        if (j.exists("share_close")) runCatching { j.tap("share_close") }
        return relu("export_video", "Vidéo 9:16", v.file!!, took, v.seconds?.toDouble(), PREVU.getValue("video"), video = true)
    }

    private fun relu(step: String, label: String, file: File, took: Double, attendu: Double?, prevuParSeconde: Double, video: Boolean = false): String {
        val r = appli.relire(file)
        if (r == null) {
            feu(step, Feu.ROUGE to "fichier illisible")
            error("$label : fichier de ${file.length() / 1024} Ko illisible")
        }
        val ecart = attendu?.let { abs(r.secondes - it) }
        var (f, why) = Feux.export(true, ecart, r.creteDbtp, took, r.secondes * prevuParSeconde)
        if (video && !r.portrait916) {
            f = Feu.pire(listOf(f, Feu.ORANGE))
            why = listOf(why, "image ${r.largeur} x ${r.hauteur}, pas en 9:16").filter { it.isNotEmpty() }.joinToString(", ")
        }
        carnet.add(Carnet.MESURE, step, "octets=${file.length()};secondes=${fmt(took)};duree=${fmt(r.secondes)};attendue=${attendu?.let(::fmt) ?: ""};crete=${fmt(r.creteDbtp)};largeur=${r.largeur};hauteur=${r.hauteur}")
        feu(step, f to why)
        return "$label de ${file.length() / 1024} Ko en ${fmt(took)} s : relu, durée ${fmt(r.secondes)} s" + (attendu?.let { " (attendue ${fmt(it)} s)" } ?: "") +
            ", crête ${fmt(r.creteDbtp)} dBTP" + (if (r.largeur > 0) ", image ${r.largeur} x ${r.hauteur}" else "")
    }

    /** Three round trips (the speaker heard by the microphone), their median for the light. */
    fun latence(): String {
        e.auStudioPro()
        val values = ArrayList<Double>()
        val fails = ArrayList<String>()
        repeat(3) {
            val ms = appli.latence()
            if (ms != null) values += ms else fails += appli.latenceEchec()
            carnet.add(Carnet.MESURE, "latence", "ms=${ms?.let(::fmt) ?: ""}")
            j.idle(500)
        }
        if (values.isEmpty()) {
            feu("latence", Feu.ROUGE to "aucune mesure")
            error("aucune mesure : ${fails.distinct().joinToString(" ; ")}")
        }
        val median = values.sorted()[values.size / 2]
        feu("latence", Feux.latence(median).let { it to if (it == Feu.VERT) "" else "${fmt(median)} ms" })
        return "${fmt(median)} ms (mesures : ${values.joinToString(", ") { fmt(it) }}" + (if (fails.isEmpty()) ")" else ", ${fails.size} ratée(s))")
    }

    /**
     * The Guide followed to its end on [style]'s new song: each card's first option touched, or
     * « Passer » when there is none or the card stays; the time its measures take.
     */
    fun guide(style: String): String {
        val step = "guide_$style"
        val started = System.currentTimeMillis()
        e.versCreer(style)
        j.createNow()
        j.await("guide_card_start.ready", 300_000)
        var applied = 0
        var skipped = 0
        var measuringMs = 0L
        val seen = LinkedHashSet<String>()
        var done = false
        while (seen.size < GUIDE_MAX_CARTES) {
            if (!j.exists("guide_panel")) j.tap("guide_bar")
            j.await("guide_panel")
            if (j.exists("guide_measuring")) {
                val m0 = System.currentTimeMillis()
                while (j.exists("guide_measuring") && System.currentTimeMillis() - m0 < 180_000) j.idle(200)
                measuringMs += System.currentTimeMillis() - m0
            }
            if (j.exists("guide_all_done")) {
                done = true
                break
            }
            val card = carte() ?: run {
                j.idle(500)
                carte() ?: error("aucune carte ni « Tout est fait » (${seen.size} cartes vues)")
            }
            seen += card
            val option = j.tagsMatching(Regex("guide_option_.+")).firstOrNull()
            if (option != null && runCatching { j.tap(option) }.isSuccess && bouge(card)) {
                applied++
                continue
            }
            check(j.exists("guide_skip")) { "carte $card : ni option qui avance ni « Passer »" }
            j.tap("guide_skip")
            check(bouge(card)) { "carte $card : reste après « Passer »" }
            skipped++
        }
        e.arreter()
        if (j.exists("guide_panel")) runCatching { j.tap("guide_bar") }
        val took = (System.currentTimeMillis() - started) / 1000
        carnet.add(Carnet.MESURE, step, "cartes=${seen.size};appliquees=$applied;passees=$skipped;mesures_ms=$measuringMs;secondes=$took")
        val text = "${seen.size} cartes ($applied appliquées, $skipped passées), mesures du Guide ${measuringMs / 1000} s, en $took s"
        feu(step, if (done) Feu.VERT to "" else Feu.ORANGE to "pas allé au bout")
        return (if (done) "jusqu'au bout : " else "arrêté à $GUIDE_MAX_CARTES cartes : ") + text
    }

    /** The Guide's card on screen (its tag's id). */
    private fun carte(): String? = j.tagsMatching(Regex("guide_card_.+")).firstOrNull { it != "guide_card_status" }

    /** The card [card] gave way (another one, or « Tout est fait ») within [carteMs]. */
    private fun bouge(card: String): Boolean {
        val end = System.currentTimeMillis() + carteMs
        while (System.currentTimeMillis() < end) {
            j.idle(200)
            if (j.exists("guide_all_done") || (j.exists("guide_panel") && !j.exists(card) && !j.exists("guide_loading"))) return true
            if (!j.exists("guide_panel")) return carte() != card
        }
        return false
    }

    /** Each screen of [EssaiEclair.SCREENS] 5 s with the song playing: its frames, the slow ones; then the app's memory. */
    fun images(): String {
        e.auStudioPro()
        e.lancerLecture()
        val parts = ArrayList<String>()
        val feux = ArrayList<Feu>()
        val why = ArrayList<String>()
        for ((name, open, mark) in EssaiEclair.SCREENS) {
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
            j.idle(1_000)
            appli.imagesDebut()
            j.idle(IMAGES_MS)
            val (n, slow) = appli.imagesFin() ?: (0 to 0)
            val part = if (n == 0) 0.0 else slow.toDouble() / n
            val f = Feux.imagesLentes(part)
            feux += f
            if (f != Feu.VERT) why += "$name ${pctD(part)}"
            parts += "$name ${pctD(part)} ($n images)"
            carnet.add(Carnet.MESURE, "images", "ecran=$name;images=$n;lentes=$slow")
        }
        j.tap("projects_back")
        j.tap("launch_continue")
        e.arreter()
        val mem = appli.memoireMo()
        carnet.add(Carnet.MESURE, "images", "memoire_mo=${mem ?: ""}")
        feu("images", Feu.pire(feux) to why.joinToString(", "))
        return "images lentes : ${parts.joinToString(", ")} ; mémoire ${mem?.let { "$it Mo" } ?: "inconnue"}"
    }

    /** The studio lying, then standing again: recognised each time, a picture of it lying. */
    fun rotation(): String {
        e.auStudioPro()
        try {
            val t0 = System.currentTimeMillis()
            appli.orienter(true)
            j.idle(1_500)
            j.awaitAny(*EssaiEclair.STUDIO, timeoutMs = 30_000)
            val lying = (System.currentTimeMillis() - t0) / 1000.0
            j.idle(600)
            if (!appli.capture(File(captures, "rotation-couche.jpg"))) carnet.add(Carnet.NOTE, "rotation", "couché : pas de capture")
            val t1 = System.currentTimeMillis()
            appli.orienter(false)
            j.idle(1_500)
            j.awaitAny(*EssaiEclair.STUDIO, timeoutMs = 30_000)
            val standing = (System.currentTimeMillis() - t1) / 1000.0
            feu("rotation", Feu.VERT to "")
            return "couché : studio en ${fmt(lying)} s ; debout : studio en ${fmt(standing)} s"
        } finally {
            appli.orienter(null)
        }
    }

    /**
     * Last: jump up plays, the screen is left to go off by itself, then [eteintMs] are heard with
     * it off (the robot's own readings: the app's stop with the screen). Never off within
     * [attenteEteintMs] (the phone's sleep set longer): not conclusive, not red.
     */
    fun ecranEteint(): String {
        val step = "ecran_eteint"
        e.versCreer(LOURDS.first())
        j.createNow()
        j.await("guide_card_start.ready", 300_000)
        e.lancerLecture()
        if (j.exists("guide_panel")) runCatching { j.tap("guide_bar") }
        appli.garderEcran(false)
        try {
            val waitStart = System.currentTimeMillis()
            while (appli.ecranAllume() && System.currentTimeMillis() - waitStart < attenteEteintMs) j.idle(500)
            val offAfter = (System.currentTimeMillis() - waitStart) / 1000
            if (appli.ecranAllume()) {
                feu(step, Feu.GRIS to "l'écran ne s'est pas éteint en ${attenteEteintMs / 60_000} min")
                return "non concluant : l'écran ne s'est pas éteint en ${attenteEteintMs / 60_000} min (mise en veille du téléphone plus longue ?)"
            }
            var onAgain = 0
            val b = ecouter(step, eteintMs) { if (appli.ecranAllume()) onAgain++ }
            val (f, why) = Feux.style(b)
            feu(step, if (b.playing * 2 < b.reads) Feu.ROUGE to "la lecture s'est arrêtée écran éteint" else f to why)
            return "écran éteint après $offAfter s ; puis ${b.text()}" + (if (onAgain > 0) " (écran rallumé ${onAgain / 4} s)" else "")
        } finally {
            appli.arreterLecture()
            appli.garderEcran(true)
        }
    }

    companion object {
        /** The two heaviest styles, played cold at the start and hot at the end. */
        val LOURDS = listOf("jump_up", "dnb")

        val FORMATS = listOf("m4a" to "M4A", "wav" to "WAV", "flac" to "FLAC", "video" to "vidéo 9:16")

        /** The time planned for an export, per second of what it makes (the essai éclair n° 3: 18 s of M4A in 7 s). */
        val PREVU = mapOf("m4a" to 0.5, "wav" to 0.4, "flac" to 0.5, "video" to 2.0)

        /** The Guide followed on three families' styles. */
        val GUIDE = listOf("house", "trap", "techno")

        const val GUIDE_MAX_CARTES = 60

        /** How long each screen's frames are counted. */
        const val IMAGES_MS = 5_000L

        fun fmt(v: Double) = String.format(Locale.ROOT, "%.2f", v)
        fun fmt(v: Float) = if (v.isFinite()) String.format(Locale.ROOT, "%.2f", v) else ""
        fun pctD(v: Double) = String.format(Locale.ROOT, "%.0f %%", v * 100)
    }
}
