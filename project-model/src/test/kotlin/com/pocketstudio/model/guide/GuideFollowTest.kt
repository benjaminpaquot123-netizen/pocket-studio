package com.pocketstudio.model.guide

import com.pocketstudio.model.DropVariation
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Project
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.StarterWizard.Mood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * G6 (docs/PLAN-GUIDE.md §4.5, docs/guide/G6-verification.md): what the acceptance criteria say of
 * the 37 styles. « Couverture »: every track of the assistant has its whole sheet, every effect its
 * why. « Son »: a song followed to the end of its parcours ([GuideFollow]) keeps its Drops different
 * (« variation entre Drops »), a low cut on every track but the kick and the sub, its master's
 * margin. (Drop − Break is measured by the song bench: scripts/song-bench.sh --guide.)
 */
class GuideFollowTest {
    private fun build(style: StarterWizard.Style, mood: Mood = style.melody.mood): Project =
        StarterWizard.build(style, style.lines.filter { !it.off }.associate { it.role to null as PadSample? }, mood,
            StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() })

    /** The roles §4.5 leaves without a low cut: the kick and the sub (the sub drop and the kick's rumble are of them). */
    private val noCutRoles = setOf(GuideRoles.KICK, GuideRoles.SUB, GuideRoles.SUB_DROP, GuideRoles.RUMBLE)

    /** The tracks that play and have a strip of their own (instruments, 303, audio) but no low cut, other than the kick and the sub. */
    private fun withoutLowCut(p: Project): List<String> {
        val facts = GuideFacts.of(p)
        return facts.tracks.filter { t ->
            (t.key.startsWith("inst:") || t.key == GuideTrack.ACID || t.key.startsWith("audio:")) && facts.song.plays(t.key) &&
                t.role !in noCutRoles && !facts.hasLowCut(t)
        }.map { "${it.name} (${it.role})" }
    }

    @Test
    fun everyTrackOfEveryStyleHasItsWholeSheet() {
        val problems = ArrayList<String>()
        var sheets = 0
        for (style in StarterWizard.STYLES) for (mood in Mood.entries) {
            val p = build(style, mood)
            val r = GuideAnalyzer.analyze(p)
            val what = "${style.id}/${mood.id}"
            assertTrue(what, r.sheets.isNotEmpty())
            for (s in r.sheets) {
                sheets++
                val t = "$what ${s.track.name}"
                // Son, Quand, Quel effet et combien, Combien de fois, Ailleurs (the screen adds « copier la chaîne » and the favourites to every one).
                if (s.sound.label.isBlank()) problems += "$t: no sound"
                if (s.parts.size != p.song.size) problems += "$t: ${s.parts.size} parts of ${p.song.size}"
                if (GuideSong(p).plays(s.track.key) && s.parts.none { it }) problems += "$t: plays nowhere"
                if (s.plays.isEmpty()) problems += "$t: no sections"
                s.chain.filter { it.why.isBlank() || it.value.isBlank() }.forEach { problems += "$t: ${it.type} without its why or value" }
                // (A song-wide curve is listed without its sections: docs/REVIEW.md, G6.)
            }
        }
        assertTrue("${problems.size} problems in $sheets sheets: ${problems.take(20)}", problems.isEmpty())
    }

    @Test
    fun aSongFollowedToTheEndOfItsParcoursKeepsItsDropsApartItsLowCutsAndItsMargin() {
        val report = StringBuilder("style;policy;decisions;applied;drops;variation;untouched variation;without low cut;master\n")
        val low = ArrayList<String>()
        val uncut = ArrayList<String>()
        for (style in StarterWizard.STYLES) {
            val song = build(style)
            val untouched = DropVariation.of(song)
            for (policy in GuideFollow.Policy.entries) {
                val f = GuideFollow.follow(song, policy)
                val p = f.project
                val what = "${style.id}/${policy.name.lowercase()}"
                // The parcours ends: nothing left to offer, every card of it settled or put aside.
                assertEquals(what, null, GuideAnalyzer.analyze(p, now = 1L).next)
                assertTrue("$what: ${f.decisions.size} decisions", f.decisions.size in 10..60)
                val v = DropVariation.of(p)
                val drops = DropVariation.drops(p).size
                assertTrue("$what: ${drops} Drops", drops >= 2)
                if (v == null || v < DropVariation.MIN) low += "$what ($v)"
                val missing = withoutLowCut(p)
                if (policy == GuideFollow.Policy.ACCEPT && missing.isNotEmpty()) uncut += "$what: $missing"
                val facts = GuideFacts.of(p)
                // The master's margin: a loudness target, the master at 0 dB or under (+0.5 tolerated), its chain.
                assertTrue("$what: master ${p.mixer.target} ${p.mixer.masterDb} dB", facts.holds("master", GuideRoles.MASTER))
                val applied = f.decisions.count { !it.endsWith(":sauter") && !it.endsWith(":keep") }
                report.append("${style.id};${policy.name.lowercase()};${f.decisions.size};$applied;$drops;${"%.3f".format(v ?: -1f)};${"%.3f".format(untouched ?: -1f)};${missing.size};${"%.1f".format(p.mixer.masterDb)}\n")
            }
        }
        println(report)
        assertTrue("Drops too alike once followed: $low", low.isEmpty())
        assertTrue("tracks without a low cut once followed: $uncut", uncut.isEmpty())
    }

    /** The decisions of a followed song are the parcours' own cards, each one once (or skipped when met again). */
    @Test
    fun followingTakesEachCardOnceInTheGuidesOrder() {
        val style = StarterWizard.style("jump_up")!!
        val f = GuideFollow.follow(build(style))
        val cards = f.decisions.map { it.substringBefore(':') }
        val parcours = GuideLibrary.forProject(f.project).cards.map { it.id }.toSet()
        assertTrue(cards.toString(), cards.all { it in parcours || it.startsWith("alert.") })
        // Each card decided once; a card met again is only skipped.
        cards.groupBy { it }.filter { it.value.size > 1 }.forEach { (card, _) ->
            assertTrue(f.decisions.toString(), f.decisions.filter { it.startsWith("$card:") }.drop(1).all { it.endsWith(":sauter") })
        }
        // What the Guide applies is in the song's history, one group per option.
        assertTrue(f.project.guide.history.map { it.card }.toSet().containsAll(listOf("fx.drums", "arr.subtract", "move.build").filter { c -> f.decisions.any { it.startsWith("$c:") && !it.endsWith(":keep") && !it.endsWith(":sauter") } }))
    }
}
