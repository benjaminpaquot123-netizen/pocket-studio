package com.pocketstudio.app.export

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Tour 6, control seat Ac-2 (E6, round 1): Mes projets › ⋮ › « Sauvegarder le morceau en .zip » opened Android's list
 * of apps to send to (the share menu titled the same). A button that sends says so: « Envoyer le projet (.zip) », in
 * the menu, in the share menu's title and on the share page, translated in every language (none falls back to French).
 */
class Tour6ExportsZipMenuWordTest {
    private val key = "Envoyer le projet (.zip)"

    @Test
    fun theZipThatIsSentIsCalledSendEverywhere() {
        val menu = File("src/main/java/com/pocketstudio/app/projects/ProjectsScreen.kt").readLines()
            .filter { "DropdownMenuItem" in it || "text = { Text(tr(" in it }
        val wrong = menu.filter { "Sauvegarder le morceau en .zip" in it }
        assertTrue("Mes projets › ⋮ still says « Sauvegarder » for the .zip it sends:\n" + wrong.joinToString("\n"), wrong.isEmpty())
        assertTrue("the menu says « $key »", menu.count { key in it } >= 2)
        val sender = File("src/main/java/com/pocketstudio/app/export/SaveProjectZip.kt").readText()
        assertTrue("the share menu's title says « $key »", "WavExporter.share(context, listOf(zip.file), tr(\"$key\"))" in sender)
        val missing = Lang.entries.filter { it != Lang.FR }.filter { I18n.table(it)[key].isNullOrBlank() || I18n.table(it)[key] == key }
        assertTrue("« $key » not translated in: $missing", missing.isEmpty())
    }
}
