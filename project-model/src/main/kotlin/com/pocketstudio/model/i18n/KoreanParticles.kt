package com.pocketstudio.model.i18n

/**
 * Korean particles that depend on the word before them (« 킥을 », « 셰이커를 »): when a name is
 * swapped for another in a text (the Mode simple's everyday words, [I18n.everyday]), the particle
 * right after it follows the new name's last sound.
 */
internal object KoreanParticles {
    /** Particle pairs: (after a final consonant, after a vowel). */
    private val PAIRS = listOf("이에요" to "예요", "으로" to "로", "을" to "를", "은" to "는", "과" to "와", "이" to "가")

    /** The final consonant of [c]: null when unknown (a Latin letter, a sign), 0 for none, 8 for ㄹ, else another. */
    private fun coda(c: Char): Int? = when {
        c in '가'..'힣' -> (c - '가') % 28
        // Digits as read in Sino-Korean: 영 일 이 삼 사 오 육 칠 팔 구.
        c in '0'..'9' -> intArrayOf(21, 8, 0, 16, 0, 0, 1, 8, 8, 0)[c - '0']
        else -> null
    }

    /**
     * The final sound of a Latin word as Korean reads it (round 3, n° 38: « “Bass”을(를) »): a letter alone by
     * its name (L, M, N, R end on a consonant: 엘, 엠, 엔, 알), a word by its last letters (« kick » 킥, « drum »
     * 드럼, « Live » 라이브, « beat » 비트).
     */
    private fun latinCoda(word: String): Int? {
        val w = word.takeLastWhile { it.isLetter() && it.code < 0x250 }.lowercase()
        if (w.isEmpty()) return null
        if (w.length == 1) return if (w[0] in "lmnr") (if (w[0] == 'l' || w[0] == 'r') 8 else 4) else 0
        return when {
            w.endsWith("ng") || w.endsWith("m") || w.endsWith("n") -> 4
            w.endsWith("ll") || (w.endsWith("l") && !w.endsWith("le")) -> 8
            w.endsWith("ck") || w.endsWith("k") || w.endsWith("p") -> 1
            else -> 0
        }
    }

    private fun pick(word: String, pair: Pair<String, String>): String? {
        val last = word.lastOrNull() ?: return null
        val coda = coda(last) ?: (if (last.isLetter()) latinCoda(word) else null) ?: return null
        // « 로 » after ㄹ as after a vowel (« 손뼉으로 », « 레일로 »).
        if (pair.first == "으로") return if (coda == 0 || coda == 8) pair.second else pair.first
        return if (coda == 0) pair.second else pair.first
    }

    /** Particles written both ways for a word put in later: « 을(를) », « (이)란 », « (으)로 »… */
    private val BOTH = listOf(
        "을(를)" to ("을" to "를"), "를(을)" to ("을" to "를"), "은(는)" to ("은" to "는"), "는(은)" to ("은" to "는"),
        "이(가)" to ("이" to "가"), "가(이)" to ("이" to "가"), "과(와)" to ("과" to "와"), "와(과)" to ("과" to "와"),
        "(으)로" to ("으로" to "로"), "(이)란" to ("이란" to "란"), "(이)에요" to ("이에요" to "예요"),
    )

    /**
     * [text] with each particle written both ways (« “%s”을(를) », the catalogue's way for a name given later)
     * made the one the word before it takes (audit ultra round 3, n° 38: « “최종 파일”을(를) » → « “최종 파일”을 »).
     * Left as it is after a sign whose sound is unknown.
     */
    fun resolve(text: String): String {
        if ('(' !in text) return text
        var out = text
        for ((both, pair) in BOTH) {
            if (both !in out) continue
            out = Regex("""(\S+?)([”"’」』»)]?)""" + Regex.escape(both)).replace(out) { m ->
                val word = m.groupValues[1]
                val chosen = pick(word, pair)
                if (chosen == null) m.value else word + m.groupValues[2] + chosen
            }
        }
        return out
    }

    /** [text] with the particle right after each of [names] (inside its closing quote) matching that name. */
    fun follow(text: String, names: Set<String>): String {
        if (names.isEmpty()) return text
        val alternatives = names.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }
        val particles = PAIRS.flatMap { listOf(it.first, it.second) }.sortedByDescending { it.length }.joinToString("|")
        val regex = Regex("($alternatives)([”\"’」』»])($particles)(?![가-힣])|($alternatives)([”\"’」』»])(이에요|예요)")
        return regex.replace(text) { m ->
            val name = m.groupValues[1].ifEmpty { m.groupValues[4] }
            val quote = m.groupValues[2].ifEmpty { m.groupValues[5] }
            val particle = m.groupValues[3].ifEmpty { m.groupValues[6] }
            val pair = PAIRS.first { particle == it.first || particle == it.second }
            name + quote + (pick(name, pair) ?: particle)
        }
    }
}
