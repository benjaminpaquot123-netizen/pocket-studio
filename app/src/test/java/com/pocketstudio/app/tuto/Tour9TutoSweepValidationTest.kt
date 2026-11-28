package com.pocketstudio.app.tuto

import com.pocketstudio.model.Hit
import com.pocketstudio.model.Note
import com.pocketstudio.model.Project
import com.pocketstudio.model.Scene
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.tuto.TutoChecks
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoPlay
import com.pocketstudio.model.tuto.TutoStep
import com.pocketstudio.model.tuto.TutoTarget
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Tour 9, « balayage » (docs/audits/ultra/sieges/tour-9/balayage/): a measuring tool, not a gate. Every step of every
 * course, at both levels, played as the app plays it ([TutoPlay.play], the walk of the « Montre-moi partout » test),
 * then questioned on what its tick means (family 3, « fausse validation »):
 *  - never ticked (NeverDone, DeadEnd), or ticked by the buttons alone while its words ask a gesture;
 *  - ticked with part of what « Montre-moi » lays (1 of n), or with fewer cells / notes than its words count
 *    (tour 8, « maracas »: « touche 4 cases » ticked at 1 of 4);
 *  - a step that speaks of velocity, ticked with the velocities put back;
 *  - « Déjà fait, rien à toucher » once the step's work is taken back (the app keeps a tick unless
 *    [TutoPlay.undone] says otherwise; tour 8, maracas, second grave).
 * Runs only with `-Pbench.out=<dir>` (scripts/bench/tuto-sweep.sh); writes `<dir>/validation.tsv`, never fails.
 */
class Tour9TutoSweepValidationTest {
    /** One thing that differs between two projects: a drum hit or a note, in a scene. */
    private data class Key(val scene: Int, val kind: Char, val a: Int, val b: Int, val c: Int)
    private data class Change(val key: Key, val old: Any?, val new: Any?)

    private fun elements(s: Scene, i: Int): Map<Key, Any> {
        val m = LinkedHashMap<Key, Any>()
        s.drums.lanes.forEachIndexed { lane, l -> l.hits.forEach { (st, h) -> m[Key(i, 'd', lane, st, 0)] = h } }
        s.synthNotes.notes.forEach { n -> m[Key(i, 'n', 0, n.start, n.pitch)] = n }
        s.trackNotes.forEach { (id, clip) -> clip.notes.forEach { n -> m[Key(i, 'n', id, n.start, n.pitch)] = n } }
        return m
    }

    private fun diff(from: Project, to: Project): List<Change> {
        val out = ArrayList<Change>()
        val a = from.allScenes
        val b = to.allScenes
        for (i in b.indices) {
            val x = a.getOrNull(i)?.let { elements(it, i) } ?: emptyMap()
            val y = elements(b[i], i)
            for ((k, v) in y) if (x[k] != v) out += Change(k, x[k], v)
            for ((k, v) in x) if (k !in y) out += Change(k, v, null)
        }
        return out
    }

    private fun put(s: Scene, k: Key, v: Any?): Scene = when (k.kind) {
        'd' -> s.copy(drums = s.drums.copy(lanes = s.drums.lanes.mapIndexed { lane, l ->
            if (lane != k.a) l else l.copy(hits = if (v == null) l.hits - k.b else l.hits + (k.b to v as Hit))
        }))
        else -> {
            fun edit(notes: List<Note>) = notes.filterNot { it.start == k.b && it.pitch == k.c } + listOfNotNull(v as Note?)
            if (k.a == 0) s.copy(synthNotes = s.synthNotes.copy(notes = edit(s.synthNotes.notes)))
            else s.trackNotes[k.a]?.let { clip -> s.copy(trackNotes = s.trackNotes + (k.a to clip.copy(notes = edit(clip.notes)))) } ?: s
        }
    }

    private fun withScene(p: Project, i: Int, s: Scene): Project =
        if (p.scenes.isEmpty() || i == p.scene) p.copy(drums = s.drums, synthNotes = s.synthNotes, tracks = p.tracks.map { t -> s.trackNotes[t.id]?.let { t.copy(notes = it) } ?: t })
        else p.copy(scenes = p.scenes.mapIndexed { j, x -> if (j == i) s else x })

    /** [p] with each of [changes] set to the value [pick] gives it. */
    private fun set(p: Project, changes: List<Change>, pick: (Change) -> Any?): Project {
        var q = p
        for ((scene, cs) in changes.groupBy { it.key.scene }) {
            var s = q.allScenes.getOrNull(scene) ?: continue
            for (c in cs) s = put(s, c.key, pick(c))
            q = withScene(q, scene, s)
        }
        return q.sanitized()
    }

    private val numbers = mapOf("deux" to 2, "trois" to 3, "quatre" to 4, "cinq" to 5, "six" to 6, "sept" to 7, "huit" to 8)
    private val countRe = Regex("""(\d+|deux|trois|quatre|cinq|six|sept|huit) (cases|coups|notes|croix|accords)""")
    private val velocityRe = Regex("""vélocité|doux|douce|plus fort|accent|nuance""", RegexOption.IGNORE_CASE)

    private fun claimed(text: String): Int? = countRe.findAll(text).mapNotNull { m -> m.groupValues[1].toIntOrNull() ?: numbers[m.groupValues[1]] }.maxOrNull()

    private fun velocityOf(v: Any?): Float? = when (v) { is Hit -> v.velocity; is Note -> v.velocity; else -> null }
    private fun withVelocity(v: Any?, vel: Float): Any? = when (v) { is Hit -> v.copy(velocity = vel); is Note -> v.copy(velocity = vel); else -> v }

    @Test
    fun sweep() {
        val dir = System.getProperty("ps.bench.out")?.takeIf { it.isNotBlank() }
        assumeTrue("run with -Pbench.out=<dir> (scripts/bench/tuto-sweep.sh)", dir != null)
        val rows = ArrayList<String>()
        var steps = 0
        fun row(sev: String, kind: String, style: String, level: TutoLevel, ci: Int, chId: String, si: Int, s: TutoStep, what: String) {
            rows += listOf(sev, kind, style, if (level == TutoLevel.Pro) "Pro" else "Facile", "${ci + 1}", chId, "${si + 1}", s.id, s.check, what.replace('\t', ' ').replace('\n', ' ')).joinToString("\t")
        }
        for (style in TutoCourse.STYLES) for (level in TutoLevel.entries) {
            val target = TutoTarget.of(style) ?: continue
            var p = target.blank()
            val chapters = TutoCourse.of(style)!!.chaptersFor(level)
            val flat = chapters.flatMap { it.steps }
            chapters.forEachIndexed { ci, ch ->
                ch.steps.forEachIndexed { si, s ->
                    if (s.manual || s.listening || s.check == "shared") return@forEachIndexed
                    steps++
                    val from = p
                    val o = runCatching { TutoPlay.play(s, p, target, level) }.getOrElse { e ->
                        row("grave", "plantage", style, level, ci, ch.id, si, s, "TutoPlay.play lève ${e::class.simpleName}: ${e.message}")
                        return@forEachIndexed
                    }
                    p = o.project
                    val text = s.textFor(level)
                    val later = flat.drop(flat.indexOf(s) + 1)
                    fun r(sev: String, kind: String, what: String) = row(sev, kind, style, level, ci, ch.id, si, s, what)
                    fun done(q: Project) = runCatching { TutoPlay.done(s, q, target, TutoPlay.base(s, o.base, q)) }.getOrDefault(false)
                    when (o.end) {
                        TutoPlay.End.NeverDone -> r("grave", "jamais-validee", "ni les boutons ni « Montre-moi » ne la cochent en ${TutoPlay.MAX_TOUCHES} touches")
                        TutoPlay.End.DeadEnd -> r("grave", "impasse", "rien à toucher qui la fasse avancer (${o.why})")
                        TutoPlay.End.DoneWithoutGesture -> r("grave", "cochee-sans-geste", "cochée par le seul bouton qui la prépare, alors que la consigne demande un geste")
                        else -> {}
                    }
                    if (o.end != TutoPlay.End.Done && o.end != TutoPlay.End.DoneWithoutGesture) return@forEachIndexed
                    val start = o.before ?: from
                    val changes = diff(start, o.project)
                    val added = changes.filter { it.old == null && it.new != null }
                    // Ticked at 1 of n: the user who lays one of what « Montre-moi » lays.
                    if (o.before != null && added.size >= 2) {
                        val one = added.first()
                        val partial = set(o.project, changes) { c -> if (c === one || c.new == null) c.new else c.old }
                        if (done(partial)) r("finition", "validee-1-sur-n", "cochée avec 1 des ${added.size} ${if (one.key.kind == 'd') "coups" else "notes"} que pose « Montre-moi »")
                    }
                    // Fewer cells / notes than the words count: the user who touches k cells of the step's pad (its dotted
                    // ones first, else the free ones in order) in the part shown, k from 1 to n-1.
                    val n = claimed(text)
                    if (n != null && n >= 2) {
                        val lane = s.target.takeIf { it.isNotBlank() }?.let { TutoChecks.laneOf(it) }
                        val sc = start.allScenes.getOrNull(start.scene)
                        if (lane != null && sc != null && s.check.let { it.startsWith("changed:") || it.startsWith("lane:") || it == "fill" || it == "gap" }) {
                            val have = sc.drums.lanes.getOrNull(lane)?.hits?.keys.orEmpty()
                            val dotted = TutoPlay.hint(s, start, target, sc.drums.steps, null, false)?.takeIf { it.first == lane }?.second.orEmpty()
                            val cells = (dotted.sorted() + (0 until sc.drums.steps)).distinct().filter { it !in have }
                            val k = (1 until n).firstOrNull { k ->
                                var q: Scene = sc
                                for (c in cells.take(k)) q = put(q, Key(start.scene, 'd', lane, c, 0), Hit())
                                done(withScene(start, start.scene, q).sanitized())
                            }
                            if (k != null) r("grave", "compte-faux", "cochée après $k case(s) sur le pad demandé, la consigne en demande $n : « $text »")
                        }
                        if (o.before != null && added.size in 1 until n) r("finition", "montre-moi-court", "« Montre-moi » pose ${added.size} sur $n demandés et l'étape est cochée : « $text »")
                    }
                    // Velocity spoken of, not checked.
                    if (o.before != null && velocityRe.containsMatchIn(text)) {
                        fun plain(c: Change) = velocityOf(c.old) ?: if (c.new is Hit) 1f else 0.85f
                        val moved = changes.filter { it.new != null && velocityOf(it.new) != plain(it) }
                        val flat = set(o.project, changes) { c -> if (c.new == null) null else withVelocity(c.new, plain(c)) }
                        val sev = if (later.any { it.check.startsWith("velocity") }) "finition" else "grave"
                        if (moved.isEmpty() && changes.isNotEmpty()) r(sev, "velocite-non-verifiee", "la consigne parle de nuance, « Montre-moi » n'en change aucune et l'étape est cochée : « $text »")
                        else if (moved.isNotEmpty() && done(flat)) r(sev, "velocite-non-verifiee", "cochée avec les vélocités remises à leur valeur d'avant : « $text »")
                    }
                    // « Déjà fait, rien à toucher » once its work is taken back.
                    val back = if (changes.isEmpty()) null else set(o.project, changes) { it.old }
                    if (back != null && !done(back) && !TutoPlay.undone(s, back, target)) {
                        val sev = if (TutoPlay.asksAGesture(s)) "grave" else "finition"
                        r(sev, "deja-fait-faux", "travail repris (${changes.size} changements défaits) : le panneau dit « rien à toucher » alors que l'étape n'est plus faite")
                    }
                }
            }
        }
        val out = File(dir!!).apply { mkdirs() }
        File(out, "validation.tsv").writeText(
            "sev\tfamille\ttuto\tniveau\tchapitre\tchapitre_id\tetape\tetape_id\tcheck\tce_que_voit_l_utilisateur\n" + rows.joinToString("\n") + "\n",
        )
        File(out, "validation-meta.txt").writeText("steps=$steps rows=${rows.size}\n")
    }
}
