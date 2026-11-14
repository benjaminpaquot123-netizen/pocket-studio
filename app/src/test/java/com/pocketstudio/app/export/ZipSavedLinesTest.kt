package com.pocketstudio.app.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tour 5, « restes » (review of the share): « Projet sauvegardé » must not be said when the copy in Documents failed. */
class ZipSavedLinesTest {
    @Test
    fun savedIsNotSaidWhenTheCopyInDocumentsFailed() {
        val kept = zipSavedLines("a.pocketstudio.zip", kept = true)
        assertEquals("Projet sauvegardé : a.pocketstudio.zip", kept.first())
        val failed = zipSavedLines("a.pocketstudio.zip", kept = false)
        assertTrue(failed.toString(), failed.none { "Projet sauvegardé" in it })
        assertTrue(failed.toString(), failed.any { "a.pocketstudio.zip" in it && "n'a pas pu" in it })
    }
}
