package com.pocketstudio.app.export

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.pocketstudio.app.help.ScreenHelp
import com.pocketstudio.app.mixer.ExportCard
import com.pocketstudio.app.mixer.MixerActions
import com.pocketstudio.app.mixer.exportProgressText
import com.pocketstudio.app.mixer.exportSavedLines
import com.pocketstudio.app.mixer.targetChipLabel
import com.pocketstudio.app.mixer.targetHint
import com.pocketstudio.app.rhythm.ExportFormat
import com.pocketstudio.app.rhythm.ExportUi
import com.pocketstudio.app.rhythm.ExportWhat
import com.pocketstudio.model.LoudnessTargets
import com.pocketstudio.model.TargetChoice
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Audit ultra round 2, n° 9 and 10 (seat A d, a club DJ in Pro, Techno: Auto and Club at −7,5 LUFS), tested
 * without the bench:
 * - one value per target everywhere: the help says the chips' own values (it said « Club (−8 LUFS) »);
 * - « Auto » and « Club » told apart when they aim at the same loudness;
 * - a club export's report never says « comme la musique en streaming », and no green « Prêt » above a gap;
 * - an M4A says its bit rate (not « 16 bits »); where the file is, in every report; the export's progress in
 *   words and for TalkBack; the share sheet says the target the file will aim at.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h1600dp-xxhdpi")
class ExportTargetRound2Test {
    @get:Rule
    val compose = createComposeRule()

    @Before
    fun french() {
        I18n.lang = Lang.FR
    }

    /** Seat A d, screen 16-17: chip « Club · −7,5 LUFS », help « Club (−8 LUFS) ». */
    @Test
    fun theHelpSaysTheChipsOwnValues() {
        val techno = LoudnessTargets.forStyle("techno", com.pocketstudio.model.Project.MIX_V2)
        assertEquals(-7.5f, techno)
        val club = LoudnessTargets.label(TargetChoice.CLUB, LoudnessTargets.shown(TargetChoice.CLUB, techno))
        assertEquals("Club · −7,5 LUFS", club)
        val help = ScreenHelp.masterTarget(techno)
        assertEquals("Volume visé", help.term)
        assertTrue(help.text, "Club (−7,5 LUFS)" in help.text)
        assertTrue(help.text, "Auto (−7,5 LUFS" in help.text)
        assertFalse(help.text, "−8 LUFS" in help.text)
        // A house project (−8): its own value.
        assertTrue("Club (−8 LUFS)" in ScreenHelp.masterTarget(-8f).text)
    }

    /** Seat A d, screen 16: « Auto (style) · −7,5 LUFS » and « Club · −7,5 LUFS » could not be told apart. */
    @Test
    fun autoAndClubAreToldApart() {
        val auto = targetChipLabel(TargetChoice.AUTO, -7.5f, "Techno")
        val club = targetChipLabel(TargetChoice.CLUB, -7.5f, "Techno")
        assertEquals("Auto (Techno) · −7,5 LUFS", auto)
        assertEquals("Club · −7,5 LUFS", club)
        // The club's hint says why they aim at the same loudness here.
        assertTrue(targetHint(TargetChoice.CLUB, -7.5f, -7.5f).contains("Club et Auto visent le même volume"))
        // A softer style (deep house −9): Club is louder, its hint the usual one.
        assertFalse(targetHint(TargetChoice.CLUB, -8f, -9f).contains("même volume"))
        // Without a style: « Auto (style) », as before.
        assertEquals("Auto (style) · −9 LUFS", targetChipLabel(TargetChoice.AUTO, -9f, null))
    }

    private fun clubExport(lufs: Double) = ExportUi(
        files = listOf(File("Techno-song-club-20261105-033338.wav")), savedInMusic = true, loudness = lufs, truePeakDb = -1.2,
        seconds = 320.0, writtenFormat = ExportFormat.Wav, targetLufs = -7.5, targetChoice = TargetChoice.CLUB, targetWholeSong = false,
    )

    /** Seat A d, screen 31: « ✓ Prêt à partager » in green, « Niveau : comme la musique en streaming (−14) », then « Écart : −5,4 LU ». */
    @Test
    fun aClubReportNeverSaysStreamingNorReadyAboveAGap() {
        val far = clubExport(-12.9)
        val bilan = far.bilan!!
        assertEquals(-5.4, bilan.gapLu()!!, 0.05)
        assertEquals("Niveau : moins fort que la cible (Club)", bilan.levelWords())
        assertFalse("streaming" in bilan.levelWords()!!.lowercase())
        val (verdict, green) = exportVerdict(far.summary!!, bilan)
        assertFalse(green)
        assertEquals("Prêt, mais moins fort que la cible (−5,4 LU)", verdict)
        // At the target: green, and said so.
        val near = clubExport(-7.7)
        assertEquals(null, near.bilan!!.gapLu())
        assertEquals("Niveau : à la cible (Club)", near.bilan!!.levelWords())
        assertEquals("Prêt à partager" to true, exportVerdict(near.summary!!, near.bilan))
        // A streaming export 1,1 LU too loud.
        val loud = clubExport(-12.9).copy(targetLufs = -14.0, targetChoice = TargetChoice.STREAMING, targetWholeSong = true)
        assertEquals("Niveau : plus fort que la cible (Streaming)", loud.bilan!!.levelWords())
        assertEquals("Prêt, mais plus fort que la cible (+1,1 LU)", exportVerdict(loud.summary!!, loud.bilan).first)
    }

    /** The card as the seat saw it: the verdict amber, no « streaming » anywhere in the club report. */
    @Test
    fun theCardShowsTheClubReport() {
        compose.setContent { PocketStudioTheme { ExportCard(clubExport(-12.9).copy(what = ExportWhat.Song), MixerActions(), hasSong = true) } }
        compose.onNodeWithTag("export_verdict").assertTextEquals("Prêt, mais moins fort que la cible (−5,4 LU)")
        compose.onAllNodes(SemanticsMatcher("streaming") { n ->
            n.config.getOrElseNullable(SemanticsProperties.Text) { null }.orEmpty().any { "streaming" in it.text.lowercase() }
        }).assertCountEquals(0)
    }

    /** Seat A d, screen 50: « Fichier final : 48 kHz · 16 bits » with M4A; its report « M4A, 48 kHz ». */
    @Test
    fun anM4aSaysItsBitRate() {
        assertEquals("48 kHz · 256 kb/s", finalFileSummary(ExportUi(format = ExportFormat.M4a)))
        assertEquals("48 kHz · 24 bits", finalFileSummary(ExportUi(format = ExportFormat.Wav)))
        assertEquals("M4A, 48 kHz, 256 kb/s", ExportSummary(60.0, -9.0, -1.5, format = "M4A").formatLine())
    }

    /** Seat A d, screens 31, 47, 55: the WAV said « Prêt : name », where the M4A said « Sauvegardé dans Musique › BenStudio ». */
    @Test
    fun everyReportSaysWhereTheFileIs() {
        val wav = File("Techno-song-club.wav")
        assertEquals(listOf("Sauvegardé dans Musique › BenStudio : Techno-song-club.wav"), exportSavedLines(ExportUi(files = listOf(wav), savedInMusic = true)))
        val notCopied = exportSavedLines(ExportUi(files = listOf(wav), savedInMusic = false)).single()
        assertTrue(notCopied, notCopied.startsWith("Pas copié dans Musique › BenStudio") && "Partager" in notCopied && wav.name in notCopied)
        // The text report beside it: in Documents (Musique takes sound only), said apart.
        val withReport = exportSavedLines(ExportUi(files = listOf(wav, File("Techno-song-club.txt")), savedInMusic = true, reportInDocuments = true))
        assertEquals(listOf("Sauvegardé dans Musique › BenStudio : Techno-song-club.wav", "Rapport dans Documents › BenStudio : Techno-song-club.txt"), withReport)
    }

    /** Seat A d, screen 45: one or two minutes of greyed chips, a thin bar and « Annuler »: the progress in words, and for TalkBack. */
    @Test
    fun theExportSaysHowFarItIs() {
        val running = ExportUi(what = ExportWhat.Song, running = true, progress = 0.42f)
        assertEquals("Fabrication du son · 42 %", exportProgressText(running))
        assertEquals("Compression M4A · 80 %", exportProgressText(running.copy(stage = "Compression M4A", progress = 0.8f)))
        compose.setContent { PocketStudioTheme { ExportCard(running, MixerActions(), hasSong = true) } }
        compose.onNodeWithTag("export_progress_text").assertIsDisplayed().assertTextEquals("Fabrication du son · 42 %")
        compose.onNodeWithTag("export_progress").assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Fabrication du son · 42 %")))
    }

    /** Seat A d, screen 41: « Prêt à partager ? » said nothing of the target the file aims at. */
    @Test
    fun theShareSheetSaysTheTarget() {
        val ui = ShareSheetUi(name = "Techno", seconds = 320, parts = 34, checks = emptyList(), target = "Club · −7,5 LUFS")
        compose.setContent { PocketStudioTheme { ShareSheetContent(ui, ShareSheetActions()) } }
        compose.onNodeWithTag("share_target").assertIsDisplayed().assertTextEquals("Volume visé : Club · −7,5 LUFS (se règle dans Mix)")
        assertNotEquals(null, ui.target)
    }
}
