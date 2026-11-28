package com.pocketstudio.model.tuto

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 7, control seat c (techno, Facile, chapters 9 to 12, defect c-V3): the « Montée » arrived with its roll
 * already laid (every hit of its last bar read « ×2 »), a roll the user never made and that no step of the Facile
 * course asks for; where a step does teach the roll, its pad was rolled before the step. In every course whose part
 * builds up, at both levels: the part its step makes holds no rolled hit on the pad that accelerates, by its button
 * and by « Montre-moi », and it is still so when the step that teaches the roll comes: the roll is the user's.
 * The words follow: no step of a part that builds up says its rolls are given.
 */
class Tour7TutoBuildNotRolledTest {
    /** « The last burst » / « already », per language: a roll the user is about to make, said as given. */
    private val alreadyThere = mapOf(
        Lang.EN to Regex("last burst|already", RegexOption.IGNORE_CASE),
        Lang.DE to Regex("letzte Salve|schon|bereits", RegexOption.IGNORE_CASE),
        Lang.ES to Regex("última ráfaga|ya ", RegexOption.IGNORE_CASE),
        Lang.IT to Regex("ultima raffica|già", RegexOption.IGNORE_CASE),
        Lang.PT_BR to Regex("última rajada|já ", RegexOption.IGNORE_CASE),
        Lang.NL to Regex("laatste salvo|al ", RegexOption.IGNORE_CASE),
        Lang.PL to Regex("ostatni[aą] seri|już", RegexOption.IGNORE_CASE),
        Lang.TR to Regex("son (bir )?seri|zaten", RegexOption.IGNORE_CASE),
        Lang.ID to Regex("semburan terakhir|sudah", RegexOption.IGNORE_CASE),
        Lang.HI to Regex("आख़िरी बौछार|आखिरी बौछार|पहले से"),
        Lang.JA to Regex("最後の連打|すでに|もう"),
        Lang.KO to Regex("마지막 연타|이미"),
        Lang.ZH_CN to Regex("最后一阵|已经"),
        Lang.AR to Regex("رشقة أخيرة|الرشقة الأخيرة|بالفعل"),
    )

    private fun rolled(p: com.pocketstudio.model.Project, label: String, role: String, target: TutoTarget): List<Int> {
        val i = TutoChecks.sceneLabelled(p, label)
        if (i < 0) return emptyList()
        val d = TutoChecks.sceneAt(p, i).drums
        return TutoChecks.lanesOf(role, target).flatMap { l -> d.lanes.getOrNull(l)?.hits?.filterValues { it.repeats > 1 }?.keys.orEmpty() }.sorted()
    }

    @Test
    fun thePartThatBuildsUpComesWithoutARollAndItsRollIsTheUsers() {
        val bad = ArrayList<String>()
        var made = 0
        var taught = 0
        for (style in TutoCourse.STYLES) for (level in TutoLevel.entries) {
            val target = TutoTarget.of(style)!!
            val build = target.buildStep ?: continue
            val label = TutoChecks.partLabel(build)
            var p = target.blank()
            for (s in TutoCourse.of(style)!!.chaptersFor(level).flatMap { it.steps }.filterNot { it.manual || it.listening || it.check == "shared" }) {
                if (s.check == "fill" && TutoChecks.buildsIn(s, target)) {
                    taught++
                    val cells = rolled(p, label, build.build, target)
                    if (cells.isNotEmpty()) bad += "$style $level ${s.id}: « $label » already rolled on ${cells.size} cells when its roll's step comes"
                }
                p = TutoPlay.play(s, p, target, level).project
                if (s.id == build.id) {
                    made++
                    val cells = rolled(p, label, build.build, target)
                    if (cells.isNotEmpty()) bad += "$style $level ${s.id}: « $label » made with ${cells.size} rolled cells (${cells.first()}..${cells.last()})"
                    // The same part by « Montre-moi » alone.
                    TutoChecks.makePart(target.blank(), s, target, level)?.let { q ->
                        val shown = rolled(q, label, build.build, target)
                        if (shown.isNotEmpty()) bad += "$style $level ${s.id}: « $label » shown with ${shown.size} rolled cells"
                    }
                }
            }
        }
        assertTrue("parts made: $made, roll steps: $taught", made >= 30 && taught >= 15)
        assertTrue("${bad.size} on $made parts and $taught roll steps\n" + bad.joinToString("\n"), bad.isEmpty())
    }

    @Test
    fun noStepSaysTheRollsOfThePartThatBuildsUpAreGiven() {
        val bad = ArrayList<String>()
        for (style in TutoCourse.STYLES) {
            val target = TutoTarget.of(style)!!
            val build = target.buildStep ?: continue
            for (s in TutoCourse.of(style)!!.chapters.flatMap { it.steps }) for (level in TutoLevel.entries) {
                val words = s.textFor(level)
                val given = (s.id == build.id && Regex("roulements|rafales").containsMatchIn(words)) ||
                    (s.check == "fill" && TutoChecks.buildsIn(s, target) && "déjà" in words)
                if (given) bad += "$style $level ${s.id}: « $words »"
                // Tour 8 (T0c): the 13 other languages say the same, never a burst already there.
                if (s.check == "fill" && TutoChecks.buildsIn(s, target) || s.id == build.id) for (lang in Lang.entries - Lang.FR) {
                    val t = I18n.table(lang)[words] ?: continue
                    if (alreadyThere.getValue(lang).containsMatchIn(t)) bad += "$style $level ${s.id} ${lang.code}: « $t »"
                }
            }
        }
        assertTrue("${bad.size} steps say the rolls are given\n" + bad.distinct().joinToString("\n"), bad.isEmpty())
    }
}
