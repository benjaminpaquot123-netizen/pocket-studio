package com.pocketstudio.bentest

import androidx.compose.ui.geometry.Rect
import com.pocketstudio.app.journey.Journey
import com.pocketstudio.app.journey.JourneyDriver
import org.junit.Assert.assertEquals
import org.junit.Test

/** The robot's way through the assistant's first step when a touch is swallowed (v5, Liquid DnB on the phone). */
class ChoixDeStyleTest {
    /** A style step whose first touch on « Choisir ce style » does nothing (a list still moving), then works. */
    private class Assistant(var swallow: Int) : JourneyDriver {
        val tags = mutableSetOf("wizard", "wizard_family_Bass music", "wizard_style_liquid_dnb")
        val touched = ArrayList<String>()
        override fun count(tag: String, merged: Boolean) = if (tag in tags) 1 else 0
        override fun bounds(tag: String) = Rect(0f, 0f, 10f, 10f)
        override fun scrollTo(tag: String) = Unit
        override fun idle(ms: Long) = Thread.sleep(minOf(ms, 5))
        override fun tags(pattern: Regex) = tags.filter { pattern.matches(it) }
        override fun click(tag: String) {
            touched += tag
            when (tag) {
                "wizard_style_liquid_dnb" -> if ("wizard_style_choose_liquid_dnb" in tags) tags -= "wizard_style_choose_liquid_dnb" else tags += "wizard_style_choose_liquid_dnb"
                "wizard_style_choose_liquid_dnb" -> if (swallow > 0) swallow-- else tags += "wizard_create_now"
            }
        }
    }

    @Test
    fun `rouge - a swallowed Choisir ce style is touched again`() {
        val a = Assistant(swallow = 1)
        Journey(a).chooseStyle("liquid_dnb", "Bass music")
        assertEquals(2, a.touched.count { it == "wizard_style_choose_liquid_dnb" })
        assertEquals(1, a.touched.count { it == "wizard_style_liquid_dnb" })
    }
}
