package com.pocketstudio.model

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class T5TokenizerTest {
    private val tiny = T5Tokenizer(
        listOf("<pad>" to 0f, "</s>" to 0f, "<unk>" to 0f, "▁" to -2f, "▁ki" to -5f, "ck" to -5f, "▁kick" to -6f, "k" to -4f, "i" to -4f, "c" to -4f),
    )

    @Test
    fun theMostLikelySplitWins() {
        // "▁kick" as one piece (-6) beats "▁ki" + "ck" (-10).
        assertArrayEquals(intArrayOf(6, 1), tiny.encode("kick"))
        // An unknown letter is <unk> on its own; spaces separate words.
        assertArrayEquals(intArrayOf(6, 3, 2, 1), tiny.encode("kick  z"))
        assertEquals(3, tiny.encode("kick kick kick", max = 3).size)
        assertArrayEquals(intArrayOf(1), tiny.encode("   "))
    }

    /** With the real t5-base tokenizer.json (path in T5_TOKENIZER), the same ids as Hugging Face. */
    @Test
    fun sameIdsAsHuggingFace() {
        val path = System.getenv("T5_TOKENIZER") ?: return
        val t = T5Tokenizer.fromJson(File(path).readText())
        val expected = mapOf(
            "punchy kick drum one shot" to intArrayOf(11462, 63, 4583, 5253, 80, 2538, 1),
            "128 BPM tech house drum loop" to intArrayOf(209, 2577, 272, 6218, 5256, 629, 5253, 6494, 1),
            "Warm Rhodes piano chord, lo-fi" to intArrayOf(18171, 17903, 7, 8355, 20513, 6, 6899, 18, 89, 23, 1),
            "808 bass  glide, trap, C minor" to intArrayOf(505, 4018, 7981, 26537, 6, 9684, 6, 205, 4012, 1),
            "ambient pad with shimmering textures" to intArrayOf(21128, 8950, 28, 27536, 53, 17951, 1),
            "café crème déjà-vu ñ 😀 x²" to intArrayOf(11949, 20435, 4315, 18, 208, 76, 3, 2, 3, 2, 3, 226, 357, 1),
        )
        expected.forEach { (text, ids) -> assertArrayEquals(text, ids, t.encode(text)) }
    }
}
