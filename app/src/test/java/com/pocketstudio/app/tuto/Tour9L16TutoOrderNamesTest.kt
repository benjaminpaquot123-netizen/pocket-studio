package com.pocketstudio.app.tuto

import com.pocketstudio.model.Scene
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 9, lot L16, F078 (seat t9-47, es: « Faltan: … Outro » while no part « Outro » is offered): the course's « Outro »
 * is made with the Intro button (« puis Intro pour l'Outro »). The « Encore » and « Pas dans l'ordre » lines only name
 * parts the user can add.
 */
class Tour9L16TutoOrderNamesTest {
    private val wanted = listOf("Intro", "Couplet", "Refrain", "Outro")

    private fun addable(line: String) {
        assertFalse("« $line » names an Outro", line.contains("Outro"))
    }

    @Test
    fun whatIsLeftNamesTheIntroButtonForTheOutro() {
        val line = TutoController.orderLine(wanted, listOf("Intro", "Couplet"))
        addable(line)
        assertTrue(line, line.contains(Scene.shownLabel("Refrain")) && line.trimEnd('.').endsWith(Scene.shownLabel("Intro")))
    }

    @Test
    fun outOfOrderNamesTheIntroButtonForTheOutro() {
        addable(TutoController.orderLine(listOf("Outro", "Intro"), listOf("Intro")))
        addable(TutoController.orderLine(wanted, listOf("Intro", "Couplet", "Refrain", "Couplet")))
    }

    @Test
    fun aRealEndPartKeepsItsName() {
        assertTrue(TutoController.orderLine(listOf("Intro", "Fin"), listOf("Intro")).contains(Scene.shownLabel("Fin")))
    }
}
