package com.pocketstudio.app.ui

import com.pocketstudio.model.i18n.tr
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tour 7, lot 3, D6: « Mes fichiers » (the songs exported, to listen to or share again) was missing from the Mode
 * simple's ⋮ (its Plus page) and from Pro's Plus. Both lists hold it, and its touch opens the list of files.
 */
@RunWith(RobolectricTestRunner::class)
class PlusMyFilesTest {
    private fun filesEntry(simple: Boolean): Boolean {
        var opened = false
        val entries = plusEntries(
            home = {}, projects = {}, glossary = {}, missions = {}, simple = simple, setSimple = {}, bugReport = {},
            simpleWays = SimplePlusWays({}, {}, {}, {}, {}), files = { opened = true },
        )
        val e = entries.firstOrNull { it.title == tr("Mes fichiers") }
        assertNotNull("« ${tr("Mes fichiers")} » missing (simple=$simple): ${entries.map { it.title }}", e)
        e!!.open?.invoke()
        return opened
    }

    @Test
    fun simplesMenuOpensMyFiles() = assertTrue(filesEntry(simple = true))

    @Test
    fun prosPlusOpensMyFiles() = assertTrue(filesEntry(simple = false))
}
