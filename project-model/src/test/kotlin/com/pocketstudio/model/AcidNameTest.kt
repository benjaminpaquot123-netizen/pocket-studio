package com.pocketstudio.model

import com.pocketstudio.model.guide.guideTracks
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.learn.SoundGlossary
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Benjamin (2026-10-29): the 303 is called « Acid 303 » everywhere and in every language (it was
 * « Basse acide », « Acid bass », « Acid-Bass »…; « TB-303 » is Roland's mark). Only the words
 * around it are translated.
 */
class AcidNameTest {
    @After
    fun french() {
        I18n.lang = Lang.FR
    }

    @Test
    fun theNameIsTheSameInEveryLanguage() {
        val song = Project().let { p -> p.copy(acid = p.acid.copy(steps = p.acid.steps.mapIndexed { i, st -> if (i == 0) st.copy(gate = true) else st })) }
        Lang.entries.forEach { lang ->
            I18n.lang = lang
            assertEquals(lang.code, "Acid 303", tr("Acid 303"))
            assertEquals(lang.code, "Acid 303", DefaultNames.shown(DefaultNames.ACID))
            // A name saved before the rename (a live pad cut from the 303) shows the new one.
            assertEquals(lang.code, "Acid 303", DefaultNames.shown("Basse acide"))
            assertEquals(lang.code, "Acid 303", tr(SoundKind.ACID.label))
            assertEquals(lang.code, "Acid 303", SoundGlossary.ALL.first { it.key == "acid" }.term)
            assertEquals(lang.code, "Acid 303", DefaultNames.shown(song.guideTracks().first { it.key == com.pocketstudio.model.guide.GuideTrack.ACID }.name))
        }
        assertEquals("Acid 303", DefaultNames.french("Basse acide"))
    }

    /** The old names are gone from the catalogues and from the Guide's and the tutorials' texts. */
    @Test
    fun noTextSaysTheOldNames() {
        val old = Regex(
            """(?i)basse acide|\(TB-303\)|(Acid bass|Acid-Bass|Bajo ácido|Basso acid|Baixo acid|Asit bas|Bas acid|Bass acid|Acid-bas|Acidbas|アシッドベース|애시드 베이스|Acid 贝斯|एसिड बेस)(?! house)""",
        )
        val found = listOf("src/main/resources/i18n", "src/main/resources/guides", "src/main/resources/tutos").map(::File)
            .flatMap { dir -> dir.walkTopDown().filter { it.extension == "json" }.toList() }
            .flatMap { f -> f.readLines().filter { old.containsMatchIn(it) }.map { "${f.parentFile.name}/${f.name}: ${it.trim().take(120)}" } }
        assertTrue("old names of the 303:\n" + found.joinToString("\n"), found.isEmpty())
    }
}
