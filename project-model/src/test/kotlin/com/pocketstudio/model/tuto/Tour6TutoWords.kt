package com.pocketstudio.model.tuto

/** Tour 6: what the course's French consignes say, read the way a beginner reads them (shared by the Tour6Tuto tests). */
object Tour6TutoWords {
    /** The imperative verbs of a gesture (« touche », « monte »…): a whitelist, the 2nd person singular. */
    val VERBS = (
        "touche appuie allume éteins écris fais crée mets règle monte baisse nomme ajoute efface choisis copie donne glisse " +
            "écoute regarde passe ouvre tourne pose exporte partage automatise envoie coupe garde change décale dessine enlève retire " +
            "tape joue enregistre remplis active vide prends recopie laisse place tire descends reviens essaie compare sélectionne " +
            "duplique renomme maintiens utilise transpose déplace refais remets termine finis vérifie trace supprime colle lance " +
            "arrête ralentis accélère accorde"
        ).split(' ')

    /** Where a gesture starts: the text's start, after « : », « . », « , »… or « puis », « et », « ensuite ». */
    private const val START = """(?:^|[:.;,(!?]|\bpuis|\bet|\bensuite|\benfin|\babord|\balors|\bou)"""
    private val ACT = Regex(START + """\s*(?:""" + VERBS.joinToString("|") + """|appui long)(?=[\s\-,:.]|$)""", RegexOption.IGNORE_CASE)
    /** A button pressed without a verb (« puis « Copier » encore »). */
    private val BUTTON = Regex(START + """\s*« (?:Copier|Passer à)[^»]*»""", RegexOption.IGNORE_CASE)
    private val QUOTED = Regex("""«[^»]*»""")

    /** The gestures a consigne asks for, in order (each verb at the start of a clause, each button pressed on its own). */
    fun actions(text: String): List<String> {
        val plain = QUOTED.replace(text) { m -> if (Regex("""« (Copier|Passer à)""").containsMatchIn(m.value)) m.value else "«»" }
        return ACT.findAll(plain).map { it.value.trim() }.toList() + BUTTON.findAll(text).map { it.value.trim() }.toList()
    }

    /** Every step of every course with each of its consignes: (style/id/level, text). */
    fun consignes(): List<Pair<String, String>> = TutoCourse.STYLES.flatMap { style ->
        TutoLevel.entries.flatMap { level ->
            TutoCourse.of(style)!!.chaptersFor(level).flatMap { it.steps }.map { "$style/${it.id}/${level.code}" to it.textFor(level) }
        }
    }.distinctBy { it.second + it.first.substringBeforeLast('/') }
}
