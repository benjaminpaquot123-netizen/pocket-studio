package com.pocketstudio.app.tuto

import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.tuto.TutoChecks
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoStep
import com.pocketstudio.model.tuto.TutoTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 9 lot L5a: what a course's step says against what the screen shows and what the step accepts
 * (F059 dotted cells, F062 texts, F069 roll bar, F070 Fin, F071 Pont, F072 copy, F080 bar, F083 listening order,
 * F091/F092 swing, F095 mix level).
 */
class Tour9TutoL5aCourseTest {
    private fun course(style: String) = TutoCourse.of(style)!!

    private fun steps(style: String): List<TutoStep> = course(style).chapters.flatMap { it.steps }

    private fun step(style: String, id: String): TutoStep = steps(style).first { it.id == id }

    /** "step:Note+Note:len[:vel]" → step to its notes. */
    private fun notes(line: String): List<Pair<Int, List<String>>> = line.trim().split(Regex("\\s+")).map { n ->
        val f = n.split(':')
        f[0].toInt() to f[1].split('+')
    }

    private val pitchOrder = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    private fun midi(note: String): Int {
        val m = Regex("^([A-G]#?)(-?\\d)$").matchEntire(note)!!
        return (m.groupValues[2].toInt() + 1) * 12 + pitchOrder.indexOf(m.groupValues[1])
    }

    @Test
    fun afroHouseThirdChordIsTheMinorFourOfTheCourse() {
        // E minor, « i i iv III »: bar 3 is A minor (Am7), not C major 7 (F059, t9-07).
        val bar3 = notes(course("afro_house").lines.getValue("pad")).first { it.first / 16 == 2 }.second
        assertEquals("afro_house bar 3 chord ${bar3.joinToString("+")}", "A", bar3.minBy(::midi).dropLast(1))
    }

    @Test
    fun ukGarageLeadPlaysTheSameTwoNotesEveryBar() {
        // A 3rd note at the end of bar 4 was dotted though no text speaks of it (F059, t9-76).
        val perBar = notes(course("uk_garage").lines.getValue("lead")).groupingBy { it.first / 16 }.eachCount()
        assertEquals("uk_garage lead notes by bar", mapOf(0 to 2, 1 to 2, 2 to 2, 3 to 2), perBar)
    }

    @Test
    fun liquidGhostNotesAreTheOnesTheProTextNames() {
        // « notes fantômes en 2.4 et 3.2 »: the dotted soft hits are elsewhere (F059, t9-15).
        val line = TutoTarget.of("liquid_dnb")!!.line("snare")!!
        val ghosts = StarterWizard.hits(line, line.pattern.length).filter { it.value.velocity < 0.5f }.keys.sorted()
        val text = step("liquid_dnb", "snare_do").textFor(TutoLevel.Pro)
        val labels = ghosts.map { "${(it % 16) / 4 + 1}.${it % 4 + 1}" }
        assertTrue("ghosts at $ghosts ($labels), text: $text", ghosts.isNotEmpty() && labels.all { it in text })
    }

    @Test
    fun retroTechnoRollIsInTheBarTheTextSays() {
        val cells = course("retro_techno").drums.getValue(TutoCourse.ALL_PARTS).getValue("snare")
        val bar = cells.indexOfFirst { it != '.' } / 16 + 1
        val s = step("retro_techno", "snare_do")
        for (level in TutoLevel.entries) {
            val text = s.textFor(level)
            val said = Regex("(\\d+)e mesure").find(text)?.groupValues?.get(1)?.toInt()
            assertEquals("retro_techno snare_do ($level): $text", bar, said)
        }
    }

    @Test
    fun retroTechnoFinIsTheDrumMachineAloneNotTheIntro() {
        // « la 909 seule », but the Fin kept the Intro's zap and lost the clap (F070, t9-59).
        val fin = step("retro_techno", "make_fin").keep.split(',').toSet()
        val intro = step("retro_techno", "make_intro").keep.split(',').toSet()
        assertFalse("Fin = Intro: $fin", fin == intro)
        assertEquals("the 909's voices", setOf("kick", "clap", "hat", "open_hat", "snare"), fin)
    }

    @Test
    fun ukGaragePontKeepsBothCharleys() {
        // « le shaker et les charleys restent »: the open hat was dropped (F071, t9-76).
        val pont = step("uk_garage", "make_pont")
        assertTrue(pont.text, "charleys" in pont.text && "open_hat" in pont.keep.split(','))
    }

    @Test
    fun industrialLeadStepSaysToCopyOnTheNextBars() {
        // The step needs the note on every bar: « Copier sur les mesures suivantes » was never said (F072, t9-35).
        val lead = course("industrial_techno").chaptersFor(TutoLevel.Facile).flatMap { it.steps }
            .filter { it.target == "lead" && it.check == "notes:lead" }.map { it.textFor(TutoLevel.Facile) }
        // A Facile consigne never names the engine's button (one moment: docs/tuto/AUTHORING.md), it says what it does.
        assertTrue(lead.joinToString("\n"), lead.any { "recopie" in it && "mesures suivantes" in it })
    }

    @Test
    fun detroitGapSpeaksOfNoBarTheGridCannotPick() {
        // A one-bar grid has no bar picker: « dernière mesure » could not be followed (F080, t9-18).
        val s = step("detroit", "gap")
        for (level in TutoLevel.entries) assertFalse(s.textFor(level), "dernière mesure" in s.textFor(level))
    }

    @Test
    fun courseTextsSayWhatTheScreenShows() {
        // F062: strings that are no track (t9-17), a Montée made 4 bars long (t9-53), « 30 » bars on screen (t9-60),
        // a tom « between the kicks » that lands on one (t9-28).
        val fin = step("detroit", "make_fin").text
        assertTrue(fin, "cordes" !in fin && "« Accords »" in fin)
        val montee = step("gabber", "make_montee").textFor(TutoLevel.Pro)
        assertFalse(montee, "8 mesures" in montee)
        val bars = step("drill", "map_bars").text
        assertTrue(bars, "Couplet 30" in bars && "Pré-refrain 2" in bars)
        val tom = step("deep_house", "tom_do").text
        val target = TutoTarget.of("deep_house")!!
        val kicks = target.line("kick")!!.let { StarterWizard.hits(it, 16).keys }
        val toms = target.line("tom")!!.let { StarterWizard.hits(it, 16).keys }
        assertFalse("$tom (toms $toms, kicks $kicks)", "entre les kicks" in tom && toms.any { it in kicks })
    }

    @Test
    fun everyCourseListensToItsPartsInTheSongsOrder() {
        // jungle played the Montée before the Drop, the map puts it after the Break (F083, t9-42).
        val bad = ArrayList<String>()
        for (style in TutoCourse.STYLES) {
            val all = TutoCourse.of(style)?.chapters?.flatMap { it.steps } ?: continue
            val song = all.firstOrNull { it.song.isNotBlank() }?.song?.split(',')?.map { it.substringBefore(':').trim() } ?: continue
            val heard = all.filter { it.id.startsWith("map_") && it.listen.startsWith("part:") }.map { it.listen.removePrefix("part:") }
            var at = 0
            for (p in heard) {
                val i = song.subList(at, song.size).indexOf(p)
                if (i < 0) { bad += "$style: $heard against $song"; break }
                at += i + 1
            }
        }
        assertTrue("map listened out of the song's order:\n" + bad.joinToString("\n"), bad.isEmpty())
    }

    @Test
    fun swingTextsAskForTheValueTheStepAccepts() {
        // house: « entre 55 et 80 % » while the step wants 33 (F091, t9-33). MPC values are said as such.
        val bad = ArrayList<String>()
        val mpc = Regex("\\([^)]*MPC[^)]*\\)|\\d+(?:,\\d+)? % sur une MPC(?: \\([^)]*\\))?")
        for (style in TutoCourse.STYLES) {
            val c = TutoCourse.of(style) ?: continue
            val want = TutoChecks.swingShown(TutoTarget.of(style)!!.style.swing)
            c.chapters.flatMap { it.steps }.filter { it.check == "swing" }.forEach { s ->
                for (level in TutoLevel.entries) {
                    val t = s.textFor(level)
                    Regex("(\\d+)(?:-(\\d+))? %").findAll(mpc.replace(t, "")).forEach { m ->
                        m.groupValues.drop(1).filter { it.isNotEmpty() }.map { it.toInt() }
                            .filter { kotlin.math.abs(it - want) > TutoChecks.SWING_POINTS }
                            .forEach { bad += "$style/$level wants $want: « ${m.value} » in $t" }
                    }
                }
            }
        }
        assertTrue("swing values the step does not accept:\n" + bad.joinToString("\n"), bad.isEmpty())
    }

    @Test
    fun swingStepsDoNotPlaceTheSliderUnderTheGrid() {
        // On a foldable the Swing slider is at the top left (F092, t9-46).
        val bad = TutoCourse.STYLES.flatMap { style ->
            (TutoCourse.of(style)?.chapters?.flatMap { it.steps } ?: emptyList()).filter { it.check == "swing" }
                .flatMap { s -> TutoLevel.entries.map { s.textFor(it) } }.filter { "sous la grille" in it }.map { "$style: $it" }
        }.distinct()
        assertTrue("Swing said « sous la grille »:\n" + bad.joinToString("\n"), bad.isEmpty())
    }

    @Test
    fun mixStepsNameTheFirstLevelTheyAccept() {
        // « Normal » devient « −2 », but a fader a little lower (« −1 ») already ticks the step (F095, t9-74).
        val bad = TutoCourse.STYLES.flatMap { style ->
            (TutoCourse.of(style)?.chapters?.flatMap { it.steps } ?: emptyList()).filter { it.check.startsWith("changed:volumes") }
                .flatMap { s -> TutoLevel.entries.map { s.textFor(it) } }
                .filter { t -> Regex("devient « −(\\d+) »").find(t)?.groupValues?.get(1)?.let { it != "1" } == true }
                .map { "$style: $it" }
        }.distinct()
        assertTrue("mix steps naming a level past the first:\n" + bad.joinToString("\n"), bad.isEmpty())
    }
}
