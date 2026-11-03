package com.pocketstudio.app.audit

import kotlin.random.Random

/**
 * All-pairs cases (audit stage 1: « combinaisons tirées par paires, pas le produit complet »): every pair
 * of values of any two factors is in at least one case. Greedy and deterministic (AETG's idea, as
 * scripts/bench/pairwise.py does for the export matrix): each new case is the best of a few candidates,
 * each built from one pair not covered yet and then, factor by factor, the value covering the most pairs
 * still missing. The same factors give the same cases on every run.
 */
object Pairwise {
    fun cases(factors: List<List<String>>, candidates: Int = 24, seed: Int = 1): List<List<String>> {
        val n = factors.size
        require(n >= 2 && factors.all { it.isNotEmpty() })
        val uncovered = LinkedHashSet<Long>()
        for (i in 0 until n) for (j in i + 1 until n) for (a in factors[i].indices) for (b in factors[j].indices) uncovered += key(i, a, j, b)
        val out = mutableListOf<List<String>>()
        val random = Random(seed)
        while (uncovered.isNotEmpty()) {
            val first = uncovered.first()
            var best: IntArray? = null
            var bestGain = -1
            repeat(candidates) {
                val case = IntArray(n) { -1 }
                val (i, a, j, b) = unkey(first)
                case[i] = a; case[j] = b
                for (f in (0 until n).shuffled(random)) {
                    if (case[f] >= 0) continue
                    var bestV = 0
                    var bestC = -1
                    for (v in factors[f].indices) {
                        var c = 0
                        for (g in 0 until n) if (case[g] >= 0) {
                            if (uncovered.contains(if (g < f) key(g, case[g], f, v) else key(f, v, g, case[g]))) c++
                        }
                        if (c > bestC) { bestC = c; bestV = v }
                    }
                    case[f] = bestV
                }
                val gain = pairsOf(case).count { it in uncovered }
                if (gain > bestGain) { bestGain = gain; best = case }
            }
            val chosen = best!!
            uncovered.removeAll(pairsOf(chosen).toSet())
            out += chosen.mapIndexed { f, v -> factors[f][v] }
        }
        return out
    }

    private fun pairsOf(c: IntArray): List<Long> {
        val out = ArrayList<Long>()
        for (i in c.indices) for (j in i + 1 until c.size) out += key(i, c[i], j, c[j])
        return out
    }

    private fun key(i: Int, a: Int, j: Int, b: Int): Long = (i.toLong() shl 48) or (a.toLong() shl 32) or (j.toLong() shl 16) or b.toLong()

    private fun unkey(k: Long): List<Int> = listOf((k shr 48).toInt() and 0xFFFF, (k shr 32).toInt() and 0xFFFF, (k shr 16).toInt() and 0xFFFF, k.toInt() and 0xFFFF)
}
