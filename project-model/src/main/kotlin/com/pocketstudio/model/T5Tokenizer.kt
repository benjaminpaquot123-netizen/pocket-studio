package com.pocketstudio.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.Normalizer

/**
 * The T5 tokenizer (SentencePiece unigram) that Stable Audio's text encoder expects: NFKC, words
 * split on spaces and prefixed with "▁", the most likely split of each word (Viterbi over the
 * piece scores), unknown characters as <unk>, then </s>. Matches Hugging Face `tokenizers` on
 * the tokenizer.json of google-t5/t5-base.
 */
class T5Tokenizer(pieces: List<Pair<String, Float>>, private val unkId: Int = 2) {
    private val ids = HashMap<String, Int>(pieces.size * 2)
    private val scores = FloatArray(pieces.size)
    private val maxLength: Int
    private val unkScore: Float

    init {
        pieces.forEachIndexed { i, (piece, score) ->
            ids.putIfAbsent(piece, i)
            scores[i] = score
        }
        maxLength = pieces.maxOfOrNull { it.first.codePointCount(0, it.first.length) } ?: 1
        unkScore = (pieces.minOfOrNull { it.second } ?: 0f) - 10f
    }

    /** Token ids of [text], ending with </s>, at most [max] of them (the end marker always kept). */
    fun encode(text: String, max: Int = 64): IntArray {
        val out = ArrayList<Int>()
        val normal = Normalizer.normalize(text, Normalizer.Form.NFKC)
        normal.split(Regex("\\s+")).filter { it.isNotEmpty() }.forEach { word -> out += encodeWord("▁$word") }
        val body = out.take(max.coerceAtLeast(1) - 1)
        return (body + EOS).toIntArray()
    }

    private fun encodeWord(word: String): List<Int> {
        // By code point: an emoji is one character (one <unk>), not two UTF-16 halves.
        val cps = word.codePoints().toArray()
        val n = cps.size
        val best = FloatArray(n + 1) { Float.NEGATIVE_INFINITY }
        val from = IntArray(n + 1) { -1 }
        val piece = IntArray(n + 1) { -1 }
        best[0] = 0f
        for (end in 1..n) {
            for (len in 1..minOf(maxLength, end)) {
                val start = end - len
                if (best[start] == Float.NEGATIVE_INFINITY) continue
                val id = ids[String(cps, start, len)] ?: continue
                val score = best[start] + scores[id]
                if (score > best[end]) {
                    best[end] = score
                    from[end] = start
                    piece[end] = id
                }
            }
            // A character no piece covers becomes <unk> on its own.
            if (best[end] == Float.NEGATIVE_INFINITY && best[end - 1] != Float.NEGATIVE_INFINITY) {
                best[end] = best[end - 1] + unkScore
                from[end] = end - 1
                piece[end] = unkId
            }
        }
        val result = ArrayList<Int>()
        var at = n
        while (at > 0) {
            result += piece[at]
            at = from[at]
        }
        // Like the reference tokenizer, a run of unknown characters is one <unk>.
        val ordered = result.reversed()
        return ordered.filterIndexed { k, id -> id != unkId || k == 0 || ordered[k - 1] != unkId }
    }

    companion object {
        const val EOS = 1

        /** From a Hugging Face tokenizer.json (unigram model). */
        fun fromJson(text: String): T5Tokenizer {
            val model = Json.parseToJsonElement(text).jsonObject.getValue("model").jsonObject
            val vocab = model.getValue("vocab").jsonArray.map { e ->
                val pair = e.jsonArray
                pair[0].jsonPrimitive.content to pair[1].jsonPrimitive.float
            }
            return T5Tokenizer(vocab, model["unk_id"]?.jsonPrimitive?.int ?: 2)
        }
    }
}
