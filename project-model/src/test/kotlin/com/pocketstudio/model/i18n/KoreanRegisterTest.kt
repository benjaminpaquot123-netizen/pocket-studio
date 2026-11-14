package com.pocketstudio.model.i18n

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Audit ultra round 2, n° 29 (seat B b, a Korean guitarist): the app speaks to the person in 해요체,
 * but some lines said « 당신의 » (a word-for-word « your »), gave orders in 반말 (« …내려. ▶ 를 눌러 …
 * 들어봐. »), or switched to 합쇼체 (« 납니다 »); a particle stood one space after a symbol (« ▶ 를 »).
 * The whole Korean catalogue is read, and the Japanese one for the space before a particle.
 */
class KoreanRegisterTest {
    private val ko = I18n.table(Lang.KO)
    private val ja = I18n.table(Lang.JA)

    private fun sentences(text: String): List<String> =
        text.split(Regex("""(?<=[.!?])\s+|\n""")).map { it.trim().trimEnd('.', '!', '?', '…', '”', '"', ')') }

    @Test
    fun noWordForWordYou() {
        val found = ko.filter { (_, v) -> "당신" in v }
        assertTrue("« 당신 » said word for word:\n" + found.entries.joinToString("\n"), found.isEmpty())
    }

    /** A sentence that ends on an order or a statement in 반말 (« 내려 », « 들어봐 », « 해 줘 »), or opens on « 네 차례 ». */
    @Test
    fun noBanmal() {
        val ending = Regex("""(내려|올려|눌러|들어봐|해봐|해 봐|만들어 줘|써 줘|해 줘|넣어 줘|열어 줘|봐|줘)$""")
        val found = ko.filter { (_, v) -> "네 차례" in v || sentences(v).any { ending.containsMatchIn(it) } }
        assertTrue("반말 in a 해요체 app:\n" + found.entries.joinToString("\n"), found.isEmpty())
    }

    @Test
    fun noFormalEndings() {
        val found = ko.filter { (_, v) -> sentences(v).any { Regex("""[가-힣](습|ㅂ|)니다$""").containsMatchIn(it) && !it.endsWith("떠다니다") } }
        assertTrue("합쇼체 in a 해요체 app:\n" + found.entries.joinToString("\n"), found.isEmpty())
    }

    /** « ▶를 », « ▶をタップ »: a particle is written against the symbol it follows. */
    @Test
    fun noSpaceBeforeAParticleAfterASymbol() {
        val koSpace = Regex("""[▶☆★↶↷■●📸⟳…−+」”)] (을|를|이|가|은|는|와|과|로|으로|의|에|에서|도|만)(?=[\s,.:;)!?]|$)""")
        val jaSpace = Regex("""[▶☆★↶↷■●📸⟳…−+」”)] (を|が|に|は|で|と|の|へ|も|から|まで|より)""")
        val found = ko.filter { koSpace.containsMatchIn(it.value) } + ja.filter { jaSpace.containsMatchIn(it.value) }
        assertTrue("a space before a particle:\n" + found.entries.joinToString("\n"), found.isEmpty())
    }
}
