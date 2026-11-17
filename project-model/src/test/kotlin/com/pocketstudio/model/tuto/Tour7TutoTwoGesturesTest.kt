package com.pocketstudio.model.tuto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 7, D4 (seats 1a #34 and #43, 1c #51, 2a #25): a Facile consigne still chained four or five gestures (create the
 * bass track, bar 1, « Copier », bar 4, « Copier » again): the beginner skipped bar 3. In Facile, a consigne asks for two
 * gestures at most; the rest comes at the next step. Gestures: [Tour6TutoWords.actions] (imperative verbs, buttons
 * pressed on their own) and the gestures a « puis » chains without a verb (« puis les 4 notes de la mesure 2 »).
 */
class Tour7TutoTwoGesturesTest {
    @Test
    fun theCounterCountsChainedGestures() {
        assertEquals(3, gestures("À toi : touche « Créer la piste Basse », puis les 5 notes en pointillé de la mesure 1. « Copier sur les mesures suivantes » la fait suivre les accords.").size)
        assertEquals(3, gestures("À toi : touche « Créer la piste Accords ». Allume « Accords » : toucher la note du bas pose l'accord. Fais les 5 coups de la mesure 1.").size)
        assertEquals(2, gestures("À toi : touche « Créer la piste Basse », puis les 5 notes en pointillé de la mesure 1.").size)
        assertEquals(1, gestures("À toi : touche « Copier sur les mesures suivantes » : la basse suit les accords.").size)
        // A « puis » that only orders what is heard is no gesture.
        assertEquals(1, gestures("Écoute la Montée : le son s'ouvre, puis la tension monte jusqu'au Drop.").size)
    }

    @Test
    fun noFacileConsigneAsksForMoreThanTwoGestures() {
        val heavy = facile().map { (where, text) -> Triple(where, text, gestures(text)) }.filter { it.third.size > MAX }
        assertTrue(
            "${heavy.size} Facile consignes ask for more than $MAX gestures:\n" + heavy.joinToString("\n") { "${it.first}: ${it.third} | ${it.second}" },
            heavy.isEmpty(),
        )
    }

    companion object {
        const val MAX = 2

        private val VERB = Regex("""(?:^|[\s,])(?:""" + Tour6TutoWords.VERBS.joinToString("|") + """|appui long)(?=[\s\-,:.]|$)""", RegexOption.IGNORE_CASE)
        /** « puis » followed by what to write, with no verb of its own (« puis les accents », « puis sur le pad »). */
        private val CHAINED = Regex("""\bpuis\s+(?:les|la|le|l'|ceux|celles|celle|celui|un|une|des|sur|à|au|\d)""", RegexOption.IGNORE_CASE)
        private val LISTENED = Regex("""^\s*(?:écoute|regarde)\b""", RegexOption.IGNORE_CASE)
        private val QUOTED = Regex("""«[^»]*»""")

        /** The gestures [text] asks for: [Tour6TutoWords.actions] and each verbless gesture a « puis » chains. */
        fun gestures(text: String): List<String> {
            val plain = QUOTED.replace(text) { "«»" }
            val chained = CHAINED.findAll(plain).filter { m ->
                val clause = plain.substring(m.range.last + 1).split(Regex("[.;:]"), 2).first()
                val sentence = plain.substring(0, m.range.first).split(Regex("[.;:!?]")).last()
                // In a sentence that listens (« Écoute la Montée : …, puis la tension »), a « puis » orders what is heard.
                !VERB.containsMatchIn(clause) && !LISTENED.containsMatchIn(sentence) && !listens(plain, m.range.first)
            }.map { it.value }
            return Tour6TutoWords.actions(text) + chained.toList()
        }

        /** A consigne that starts with « Écoute » or « Regarde » and has no « À toi » before [at]: what follows is heard. */
        private fun listens(plain: String, at: Int): Boolean = LISTENED.containsMatchIn(plain) && "À toi" !in plain.substring(0, at)

        /** Every Facile consigne of every course: (style/id, text). */
        fun facile(): List<Pair<String, String>> = TutoCourse.STYLES.flatMap { style ->
            TutoCourse.of(style)!!.chaptersFor(TutoLevel.Facile).flatMap { it.steps }.map { "$style/${it.id}" to it.textFor(TutoLevel.Facile) }
        }
    }
}
