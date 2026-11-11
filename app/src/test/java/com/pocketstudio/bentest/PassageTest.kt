package com.pocketstudio.bentest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** BenStudio Test's logbook and its run (docs/PLAN-TEST-APK.md): on disk, picked up after a crash, a failure never stops the rest. */
class PassageTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun carnet() = Carnet(java.io.File(tmp.root, "bentest/carnet.tsv"))

    private fun steps(done: MutableList<String>, failing: String? = null) = listOf("a", "b", "c").map { n ->
        Etape(n, "Étape $n", 10_000) { done += n; if (n == failing) error("$n a raté") else "$n vu" }
    }

    @Test
    fun aLineKeepsItsTabsAndLineBreaks() {
        val c = carnet()
        c.add(Carnet.NOTE, "x\ty", "deux\nlignes\tet \\ un trait")
        val read = Carnet(c.file).lines()
        assertEquals("one line read back: $read", 1, read.size)
        assertEquals(Carnet.Ligne(read[0].atMs, Carnet.NOTE, "x\ty", "deux\nlignes\tet \\ un trait"), read[0])
    }

    @Test
    fun aFailedStepDoesNotStopTheOthers() {
        val done = ArrayList<String>()
        val c = carnet()
        Passage(c, steps(done, failing = "b")).run()
        assertEquals(listOf("a", "b", "c"), done)
        val ends = c.lines().filter { it.kind == Carnet.REUSSI || it.kind == Carnet.ECHEC }.map { it.step to it.kind }
        assertEquals(listOf("a" to Carnet.REUSSI, "b" to Carnet.ECHEC, "c" to Carnet.REUSSI), ends)
        assertTrue(c.lines().last().kind == Carnet.FINI)
    }

    /** The app died in « b »: the next launch notes the crash with its name and goes on with « c » only. */
    @Test
    fun aCrashIsNotedAndTheRunGoesOnWithTheNextStep() {
        val c = carnet()
        c.add(Carnet.DEBUT, "a")
        c.add(Carnet.REUSSI, "a", "a vu")
        c.add(Carnet.DEBUT, "b")
        val done = ArrayList<String>()
        Passage(c, steps(done), crashText = { "java.lang.IllegalStateException: boum\n\tat x" }).run()
        assertEquals(listOf("c"), done)
        val crash = c.lines().single { it.kind == Carnet.PLANTAGE }
        assertEquals("b", crash.step)
        assertTrue(crash.text, crash.text.contains("boum"))
        // Run again: nothing left, the crash noted once.
        Passage(c, steps(done)).run()
        assertEquals(listOf("c"), done)
        assertEquals(1, c.lines().count { it.kind == Carnet.PLANTAGE })
    }

    @Test
    fun aStepPastItsLongestTimeFails() {
        var now = 0L
        val c = Carnet(java.io.File(tmp.root, "c.tsv")) { now }
        Passage(c, listOf(Etape("lent", "Lent", 1_000) { now += 5_000; "fini" }), clock = { now }).run()
        val end = c.lines().single { it.step == "lent" && it.kind != Carnet.DEBUT }
        assertEquals(Carnet.ECHEC, end.kind)
        assertTrue(end.text, end.text.startsWith("trop longue"))
    }

    @Test
    fun theReportSaysTheVerdictInPlainWords() {
        val c = carnet()
        c.add(Carnet.DEBUT, "a"); c.add(Carnet.REUSSI, "a", "ok")
        c.add(Carnet.DEBUT, "b"); c.add(Carnet.PLANTAGE, "b", "boum")
        val etapes = steps(ArrayList())
        val md = Rapport.markdown(c.lines(), etapes, listOf("Version" to "0.22.7"))
        assertTrue(md, md.contains("1 réussie, 0 ratée, 1 plantage, 1 pas faite"))
        assertTrue(md, md.contains("| Étape b | 💥 plantage |"))
        c.add(Carnet.MESURE, "a", "xruns=2;cpu=0.41;running=true")
        val json = Rapport.json(c.lines(), listOf("Version" to "0.22.7"))
        assertTrue(json, json.contains("\"xruns\":2.000") && json.contains("\"running\":\"true\""))
    }
}
