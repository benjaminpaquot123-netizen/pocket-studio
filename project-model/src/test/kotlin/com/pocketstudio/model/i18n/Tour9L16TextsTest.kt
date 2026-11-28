package com.pocketstudio.model.i18n

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 9, lot L16: tutorial words the seats misread in their language (finitions F081, F084, F087).
 */
class Tour9L16TextsTest {
    /** Every text of [lang], all areas: (area, French key, text). */
    private fun texts(lang: Lang): List<Triple<String, String, String>> =
        I18n.AREAS.flatMap { a -> I18n.area(lang, a).map { Triple(a, it.key, it.value) } }

    private fun report(what: String, wrong: List<String>) = assertTrue("$what (${wrong.size}):\n" + wrong.take(40).joinToString("\n"), wrong.isEmpty())

    /**
     * F081 (seat t9-06, ko): « 여기 4번 더 쳤어요 » (« here you hit 4 times more ») read like a mistake of the user's,
     * while the tutorial asks to take hits away. The crossed-out cells say how many hits to remove.
     */
    @Test
    fun koreanCrossedOutHitsSayWhatToRemove() {
        val keys = listOf("%d coup en trop ici (barré)", "%d coups en trop ici (barrés)")
        val ko = texts(Lang.KO).filter { it.second in keys }
        val wrong = keys.filterNot { k -> ko.any { it.second == k } }.map { "missing « $it »" } +
            ko.filter { "더 쳤" in it.third || "지우" !in it.third }.map { "${it.first}: « ${it.second} » → « ${it.third} »" }
        report("ko texts of the crossed-out hits that do not say to remove them", wrong)
    }

    /**
     * F084 (seat t9-47, es): « Detener » and « Dejar de escuchar » for the same stop gesture. In every language,
     * « Arrêter l'écoute » uses the word of « Arrêter ».
     */
    @Test
    fun stopListeningUsesTheStopWordInEveryLanguage() {
        val wrong = Lang.entries.filter { it != Lang.FR }.mapNotNull { l ->
            val all = texts(l)
            val stop = all.firstOrNull { it.second == "Arrêter" }?.third ?: return@mapNotNull "${l.code}: no « Arrêter »"
            val listen = all.firstOrNull { it.second == "Arrêter l'écoute" }?.third ?: return@mapNotNull "${l.code}: no « Arrêter l'écoute »"
            "${l.code}: « $stop » / « $listen »".takeIf { !listen.lowercase().contains(stop.lowercase()) }
        }
        report("languages where « Arrêter l'écoute » does not use the word of « Arrêter »", wrong)
    }

    /**
     * F087 (seat t9-26, tr): « Bravo, bölüm bitti! » while « bölüm » is also a part of the song (« Partie » → « Bölüm »).
     * A chapter is « aşama », as the header « Aşama 9/15 » says: no text about a chapter (and not about a part) says « bölüm ».
     */
    @Test
    fun turkishChapterIsNeverTheWordOfAPart() {
        val part = texts(Lang.TR).first { it.second == "Partie" }.third
        assertTrue("tr « Partie » is « $part »", part.lowercase().startsWith("bölüm"))
        val chapter = Regex("(?i)chapitre")
        val wrong = texts(Lang.TR).filter { chapter.containsMatchIn(it.second) && !it.second.contains("partie", ignoreCase = true) && it.third.contains("bölüm", ignoreCase = true) }
            .map { "${it.first}: « ${it.second} » → « ${it.third} »" }
        report("tr texts about a chapter that say « bölüm » (a part of the song)", wrong)
    }
}
