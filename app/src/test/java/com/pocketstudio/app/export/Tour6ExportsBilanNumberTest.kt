package com.pocketstudio.app.export

import com.pocketstudio.app.rhythm.ExportFormat
import com.pocketstudio.app.rhythm.ExportUi
import com.pocketstudio.model.TargetChoice
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Tour 6, control seat Ac-2 (E1, round 1): after a « club » file in Mode simple, « C'est prêt » said « Niveau : à la
 * cible (Club) » with no number, while Pro gives the file's −8,1. The Simple report now carries the file's number too
 * (plain, no LUFS), still with one « Niveau » line.
 */
class Tour6ExportsBilanNumberTest {
    @Test
    fun theSimpleReportGivesTheFilesNumber() {
        I18n.lang = Lang.FR
        val done = ExportUi(files = listOf(File("a.wav")), loudness = -8.1, truePeakDb = -1.0, seconds = 178.0, writtenFormat = ExportFormat.Wav,
            targetChoice = TargetChoice.CLUB, targetLufs = -8.0)
        val lines = shareReportLines(done, simple = true)
        assertTrue("the file's number (−8,1) is said: $lines", lines.any { "−8,1" in it })
        assertEquals("one « Niveau » line: $lines", 1, lines.count { it.startsWith("Niveau") })
        assertTrue("no LUFS in Simple: $lines", lines.none { "LUFS" in it })
    }
}
