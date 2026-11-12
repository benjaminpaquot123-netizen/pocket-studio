package com.pocketstudio.app.guide

import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

/** G4: the stage 6 checks on sound (mono, phone, a reference's loudest passage): what they measure and what they let through. */
class GuideChecksTest {
    private val rate = OfflineExport.SAMPLE_RATE

    @After
    fun french() {
        I18n.lang = Lang.FR
    }

    /** [seconds] of a sine at [hz], [left] and [right] its gain on each side (−1: upside down). */
    private fun sine(hz: Double, seconds: Double = 2.0, left: Float = 0.5f, right: Float = 0.5f): FloatArray {
        val n = (seconds * rate).toInt()
        return FloatArray(2 * n) { i -> val x = sin(2 * PI * hz * (i / 2) / rate).toFloat(); if (i % 2 == 0) x * left else x * right }
    }

    @Test
    fun monoSaysWhatCancels() {
        // The same on both sides: nothing lost; one side upside down: everything goes.
        assertEquals(0f, GuideChecks.monoLossDb(sine(440.0))!!, 0.05f)
        assertTrue(GuideChecks.monoLossDb(sine(440.0, left = 0.5f, right = -0.5f))!! > 20f)
        // One side only: half the energy on each, 3 dB down.
        assertEquals(3f, GuideChecks.monoLossDb(sine(440.0, left = 0.5f, right = 0f))!!, 0.3f)
        // Folded: both channels the mean.
        val m = GuideChecks.mono(sine(440.0, left = 0.5f, right = 0.1f))
        assertEquals(m[200], m[201], 0f)
    }

    @Test
    fun aPhoneKeepsNothingUnder300Hz() {
        // A 50 Hz sub: almost nothing through; a 1 kHz tone: nearly all.
        assertTrue(GuideChecks.phoneDb(sine(50.0))!! < -35f)
        assertTrue(GuideChecks.phoneDb(sine(1000.0))!! > -0.5f)
        // The same model, heard: the sub is gone, the tone stays.
        val sub = GuideChecks.phone(sine(50.0)).drop(rate).maxOf { kotlin.math.abs(it) }
        val tone = GuideChecks.phone(sine(1000.0)).drop(rate).maxOf { kotlin.math.abs(it) }
        assertTrue("sub $sub", sub < 0.01f)
        assertTrue("tone $tone", tone > 0.4f)
        assertEquals(null, GuideChecks.phoneDb(FloatArray(2000)))
    }

    @Test
    fun theReferencesLoudestPassageIsKeptAtTheEnginesRate() {
        // 44.1 kHz mono: 6 s quiet, 6 s loud, 6 s quiet.
        val r = 44100
        val data = FloatArray(18 * r) { i -> val s = i / r; (if (s in 6 until 12) 0.8f else 0.05f) * sin(2 * PI * 220 * i / r).toFloat() }
        val excerpt = GuideChecks.loudestExcerpt(Pcm(data, 1, r), 6.0)
        assertEquals(6 * rate * 2, excerpt.size)
        // The loud passage, on both sides.
        assertTrue(excerpt.maxOf { kotlin.math.abs(it) } > 0.7f)
        assertEquals(excerpt[1000], excerpt[1001], 0f)
        // A file shorter than asked: all of it.
        assertEquals(2.0 * 2 * rate, GuideChecks.loudestExcerpt(Pcm(FloatArray(2 * r) { 0.1f }, 1, r), 16.0).size.toDouble(), 8.0)
    }

    @Test
    fun theTakesAreNamedInTheInterfacesLanguage() {
        fun all() = listOf("mono", "phone", "reference").flatMap(GuideListen::checkTakes) +
            GuideListen.takesOf("off@inst:1@0") + GuideListen.takesOf("on@inst:1@0") + GuideListenUi.takes()
        val french = all()
        assertEquals(listOf("Avec l'effet", "Sans l'effet"), GuideListen.takesOf("off@inst:1@0"))
        assertEquals(listOf("Stéréo", "Mono"), GuideListen.checkTakes("mono"))
        for (lang in (Lang.entries - Lang.FR)) {
            I18n.lang = lang
            val shown = all()
            // Every name translated (« Mono » is the same word everywhere).
            val untranslated = shown.zip(french).filter { (a, b) -> a == b && b != "Mono" }
            assertTrue("${lang.code}: $untranslated", untranslated.isEmpty())
        }
    }
}
