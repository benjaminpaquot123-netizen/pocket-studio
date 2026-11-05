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

    private fun pick(word: String, pair: Pair<String, String>): String? {
        val coda = coda(word.lastOrNull() ?: return null) ?: return null
        // « 로 » after ㄹ as after a vowel (« 손뼉으로 », « 레일로 »).
        if (pair.first == "으로") return if (coda == 0 || coda == 8) pair.second else pair.first
        return if (coda == 0) pair.second else pair.first
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
