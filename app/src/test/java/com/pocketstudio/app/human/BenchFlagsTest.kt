package com.pocketstudio.app.human

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tour 9, banc A (drapeaux): the flag and verdict lines keep one shape, read back field by field. Pure text. */
class BenchFlagsTest {
    @Test
    fun aFlagIsOneLineOfKeyValuePairsReadBackAsWritten() {
        val line = BenchFlags.flag(3, "amenee.wait", "les chapitres", "après touche tuto_chapters@439,438", 120_034, "tuto_panel/tab_simple_Rythme")
        assertEquals("#3 BENCH-FLAG where=amenee.wait want=\"les chapitres\" saw=\"après touche tuto_chapters@439,438\" waited=120034 screen=tuto_panel/tab_simple_Rythme", line)
        assertEquals(
            mapOf("kind" to "BENCH-FLAG", "where" to "amenee.wait", "want" to "les chapitres", "saw" to "après touche tuto_chapters@439,438", "waited" to "120034", "screen" to "tuto_panel/tab_simple_Rythme"),
            BenchFlags.parse(line),
        )
    }

    @Test
    fun aValueNeverBreaksTheLine() {
        // Line breaks, quotes, an equals sign, nothing at all, a text too long: still one line, one value per key.
        val line = BenchFlags.flag(0, "seat.crash", "le geste\njoué", "IllegalStateException: a=\"b\"\n\tat X", 5, null)
        assertTrue(line, '\n' !in line && '\t' !in line)
        val read = BenchFlags.parse(line)!!
        assertEquals("le geste joué", read["want"])
        assertEquals("IllegalStateException: a='b' at X", read["saw"])
        assertEquals("-", read["screen"])
        assertEquals("-", BenchFlags.value(""))
        assertEquals(200, BenchFlags.value("x".repeat(500)).length)
    }

    @Test
    fun aVerdictSaysOkOrFailWithThePlace() {
        assertEquals("#7 BENCH-VERDICT ok where=seat.fin", BenchFlags.verdict(7, true, "seat.fin"))
        val fail = BenchFlags.verdict(0, false, "amenee.wait", "amenée tuto:deep_house:7 : les chapitres n'est jamais venu")
        assertEquals("#0 BENCH-VERDICT fail where=amenee.wait why=\"amenée tuto:deep_house:7 : les chapitres n'est jamais venu\"", fail)
        assertEquals(mapOf("kind" to "BENCH-VERDICT", "result" to "fail", "where" to "amenee.wait", "why" to "amenée tuto:deep_house:7 : les chapitres n'est jamais venu"), BenchFlags.parse(fail))
        assertNull(BenchFlags.parse("#3 ROUTE home | MODE pro"))
        assertNull(BenchFlags.parse("#3 FIN : fin"))
    }

    @Test
    fun eachEndOfASeatHasItsVerdict() {
        // Every « FIN : … » HumanTester.seatLoop writes.
        val ends = mapOf(
            "fin" to (true to "seat.fin"),
            "rejeu : oracle tenu" to (true to "seat.rejeu"),
            "rejeu : reproduit" to (true to "seat.rejeu"),
            "abandon : budget (12 gestes, 12 faits, 0 images)" to (true to "seat.budget"),
            "rejeu : oracle non tenu : « Exporter » absent" to (false to "seat.oracle"),
            "rejeu : oracle non tenu : « Erreur » encore là" to (false to "seat.oracle"),
            "rejeu : divergé au pas 4 (pas de geste 4 à l'origine)" to (false to "seat.rejeu"),
            "rien ne change (5 écrans identiques)" to (false to "seat.rien-ne-change"),
            "abandon : plus de geste" to (false to "seat.abandon"),
            "banc : 4 exceptions du banc" to (false to "seat.banc"),
            "plantage" to (false to "seat.crash"),
            "autre chose" to (false to "seat.end"),
        )
        ends.forEach { (why, want) -> assertEquals(why, want, BenchFlags.ofEnd(why)) }
    }
}
