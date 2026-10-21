package com.pocketstudio.app

import com.pocketstudio.app.help.Glossary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlossaryTest {
    @Test
    fun everyTermIsListedOnceAndShort() {
        val all = Glossary.ALL
        assertEquals(all.size, all.map { it.term }.toSet().size)
        // Every Help declared in Glossary appears in the lexicon (getters: translated when read).
        val declared = Glossary::class.java.declaredMethods
            .filter { it.returnType == com.pocketstudio.ui.Help::class.java && it.parameterCount == 0 }
            .map { m -> m.isAccessible = true; m.invoke(Glossary) }
        assertTrue(declared.isNotEmpty())
        assertTrue(declared.all { it in all })
        // One or two sentences: readable in the bubble without scrolling.
        assertTrue(all.all { it.text.length in 20..320 && it.term.isNotBlank() })
    }
}
