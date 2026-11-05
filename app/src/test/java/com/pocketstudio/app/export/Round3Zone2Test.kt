package com.pocketstudio.app.export

import android.app.Application
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.printToString
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.mixer.ExportCard
import com.pocketstudio.app.mixer.MixerActions
import com.pocketstudio.app.mixer.TargetPick
import com.pocketstudio.app.mixer.targetHint
import com.pocketstudio.app.rhythm.ExportUi
import com.pocketstudio.app.rhythm.ExportWhat
import com.pocketstudio.app.rhythm.REMIX_STEM_BASE
import com.pocketstudio.app.rhythm.stemExports
import com.pocketstudio.app.rhythm.stemSkips
import com.pocketstudio.app.settings.HelpActions
import com.pocketstudio.app.settings.HelpPage
import com.pocketstudio.app.ui.mixerStrips
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Project
import com.pocketstudio.model.Remix
import com.pocketstudio.model.StemMix
import com.pocketstudio.model.TargetChoice
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

/** Audit ultra round 3, zone 2 (n° 10 to 14): exports, sharing, going on elsewhere. Each test failed before its fix. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h2400dp")
class Round3Zone2Test {
    @get:Rule
    val compose = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()

    @After
    fun french() {
        I18n.lang = Lang.FR
    }

    private val beat = Project(name = "Nuit de printemps", drums = DrumPattern().toggle(0, 0))

    /** n° 10 (A d 3, F b 26): the target is chosen in « Prêt à partager ? », the same setting as Mix's. */
    @Test
    fun theShareSheetChoosesTheTarget() {
        val chosen = ArrayList<TargetChoice>()
        val ui = ShareSheetUi(name = "Nuit", seconds = 60, parts = 3, checks = emptyList(), target = TargetPick(TargetChoice.OFF))
        compose.setContent { PocketStudioTheme { ShareSheetContent(ui, ShareSheetActions(chooseTarget = { chosen += it })) } }
        compose.onNodeWithTag("share_target_CLUB").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("share_target_STREAMING").assertIsDisplayed().performClick()
        assertEquals(listOf(TargetChoice.CLUB, TargetChoice.STREAMING), chosen)
        compose.onNodeWithTag("share_target_hint").assertIsDisplayed()
    }

    /** n° 10 (A d 4): the export card chooses it too, not five swipes away under the master's reverbs. */
    @Test
    fun theExportCardChoosesTheTarget() {
        val chosen = ArrayList<TargetChoice>()
        val actions = MixerActions(chooseTarget = { c, _ -> chosen += c })
        compose.setContent {
            PocketStudioTheme { ExportCard(ExportUi(what = ExportWhat.Song), actions, hasSong = true, target = TargetPick(TargetChoice.CLUB, styleLufs = -7.5f)) }
        }
        compose.onNodeWithTag("export_target_STREAMING").performClick()
        assertEquals(listOf(TargetChoice.STREAMING), chosen)
    }

    /** n° 10 (A d 12): a song with no Drop read « mesuré sur le Drop ». */
    @Test
    fun theTargetsHelpNeverAssumesADrop() {
        listOf(TargetChoice.AUTO, TargetChoice.CLUB).forEach { c ->
            val hint = targetHint(c, -8f, -7.5f)
            assertFalse(hint, "Drop" in hint)
            assertTrue(hint, "le passage le plus fort" in hint)
        }
        assertFalse("Drop" in com.pocketstudio.app.help.ScreenHelp.masterTarget(-7.5f).text)
        assertFalse("Drop" in com.pocketstudio.app.mixer.lossText(4.0))
    }

    /** n° 12 (F b 58): « aucune piste coupée » over the remix's voice muted. */
    @Test
    fun aMutedRemixStemIsPointedOut() {
        val stems = List(Remix.STEMS) { if (it == 3) StemMix(mute = true) else StemMix() }
        val p = beat.copy(remix = Remix(setId = "s", name = "Ma chanson", stems = stems))
        val checks = shareChecks(p, mixerStrips(p), ShareLevels(), loopBars = 8)
        val muted = checks.filterIsInstance<ShareCheck.Muted>().single()
        assertEquals(listOf("Voix (Ma chanson)"), muted.names)
        assertEquals(listOf(3), muted.stems)
        // A stem alone in solo: the others would not be heard.
        val solo = p.copy(remix = p.remix.copy(stems = List(Remix.STEMS) { StemMix(solo = it == 0) }))
        assertEquals(listOf(0), shareChecks(solo, mixerStrips(solo), ShareLevels(), 8).filterIsInstance<ShareCheck.Solo>().single().stems)
    }

    /** n° 12 (F b 53): « Piste par piste » gave one « remix » file for the four separated parts. */
    @Test
    fun theRemixGivesOneFilePerStem() {
        val stems = List(Remix.STEMS) { if (it == 3) StemMix(mute = true) else StemMix() }
        val p = beat.copy(remix = Remix(setId = "s", stems = stems))
        assertEquals(
            listOf("batterie" to 0, "remix-batterie" to REMIX_STEM_BASE - 0, "remix-basse" to REMIX_STEM_BASE - 1, "remix-reste" to REMIX_STEM_BASE - 2),
            stemExports(p, song = false),
        )
        // The voice muted in the remix: said, not left out without a word.
        assertEquals(listOf("Acid 303 (vide)", "Synthé (vide)", "Voix (coupée)"), stemSkips(p, song = false, names = mapOf(0 to "Batterie", 1 to "Acid 303", 2 to "Synthé", 3 to "Ma chanson")))
    }

    /** n° 12 (F d 54): Synth missing from the files, without a word. */
    @Test
    fun aTrackWithoutAFileIsSaid() {
        val names = mapOf(0 to "Batterie", 1 to "Acid 303", 2 to "Synthé")
        assertEquals(listOf("Acid 303 (vide)", "Synthé (vide)"), stemSkips(beat, song = false, names = names))
        compose.setContent {
            PocketStudioTheme { ExportCard(ExportUi(what = ExportWhat.Stems), MixerActions(), hasSong = false, stemSkips = listOf("Synthé (vide)")) }
        }
        compose.onNodeWithTag("export_stem_skips").assertIsDisplayed()
    }

    /** n° 12 (F d 48): an export made before the song changed stayed offered with « Partager ». */
    @Test
    fun aStaleExportIsNotOffered() {
        val file = File(app.cacheDir, "old.m4a").apply { writeText("x") }
        val made = ExportUi(files = listOf(file), madeFrom = ExportUi.madeHash(beat))
        assertFalse(made.staleFor(beat))
        assertFalse("a rename is no change of sound", made.staleFor(beat.copy(name = "Autre nom")))
        val changed = beat.copy(drums = beat.drums.toggle(0, 4))
        assertTrue(made.staleFor(changed))
        compose.setContent { PocketStudioTheme { ExportCard(made, MixerActions(), hasSong = true, stale = true) } }
        compose.onNodeWithTag("export_stale").assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithTag("share").fetchSemanticsNodes().size)
    }

    /** n° 11 (A d 57): « Annuler » on a new export took away the last one's « Sauvegardé dans… ». */
    @Test
    fun aCancelledRunGivesBackTheLastResult() {
        val file = File(app.cacheDir, "kept.wav").apply { writeText("x") }
        val earlier = ExportUi(files = listOf(file), savedInMusic = true, loudness = -14.0)
        val after = ExportUi(running = false).withResultOf(earlier)
        assertEquals(listOf(file), after.files)
        assertTrue(after.savedInMusic)
    }

    /** n° 12 (F d 56): « Partager les 2 fichiers » sends both (the bench read only the first one: banc). */
    @Test
    fun twoFilesLeaveTogether() {
        val a = WavExporter.newFile(app, "Beat-01-drums").apply { writeText("a") }
        val b = File(a.parentFile, "Beat-02-acid.wav").apply { writeText("b") }
        assertTrue(WavExporter.share(app, listOf(a, b)))
        val chooser = shadowOf(app).nextStartedActivity
        @Suppress("DEPRECATION")
        val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals(Intent.ACTION_SEND_MULTIPLE, send.action)
        @Suppress("DEPRECATION")
        assertEquals(2, send.getParcelableArrayListExtra<android.net.Uri>(Intent.EXTRA_STREAM)!!.size)
    }

    /** n° 13 (F b 41): the name typed for a version renamed the project at each export. */
    @Test
    fun aVersionsNameLeavesTheProjectsName() {
        assertFalse(shareRenamesProject("Nuit de printemps", "Nuit de printemps (MR)"))
        // A name the app gave (« Mon projet 3 ») still takes the song's.
        assertTrue(shareRenamesProject("Mon projet 3", "Nuit de printemps"))
        val ui = ShareSheetUi(name = "Nuit de printemps (MR)", seconds = 60, parts = 3, checks = emptyList(), projectName = "Nuit de printemps")
        compose.setContent { PocketStudioTheme { ShareSheetContent(ui, ShareSheetActions()) } }
        compose.onNodeWithTag("share_keeps_name").assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithTag("share_renames").fetchSemanticsNodes().size)
    }

    /** n° 13 (F d 71): « Beat for Tom (copie) » in the English app's name field. */
    @Test
    fun aCopysNameIsShownInTheAppsLanguage() {
        I18n.lang = Lang.EN
        assertEquals("Beat for Tom (copy)", initialShareName("Beat for Tom (copie)"))
    }

    /** n° 13 (F d 22, 61): sending the project to go on elsewhere, where one sends, and in the help. */
    @Test
    fun theProjectIsSentFromShareAndTheHelpSaysHow() {
        var sent = 0
        val ui = ShareSheetUi(name = "Nuit", seconds = 60, parts = 3, checks = emptyList())
        compose.setContent { PocketStudioTheme { ShareSheetContent(ui, ShareSheetActions(sendProject = { sent++ })) } }
        compose.onNodeWithTag("share_project").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(1, sent)
    }

    @Test
    fun theHelpSaysHowToGoOnElsewhere() {
        compose.setContent { PocketStudioTheme { HelpPage(HelpActions()) } }
        compose.onNodeWithTag("help_other_device").performScrollTo().assertIsDisplayed()
    }

    /** n° 14 (A c 47, C c 46, D c 60): the Mode simple's « C'est prêt » was full of LUFS, dBTP, kHz, kb/s. */
    @Test
    fun theSimpleReportHasNoJargon() {
        val done = ExportUi(files = listOf(File("a.m4a")), loudness = -9.0, truePeakDb = -1.4, seconds = 58.0, writtenFormat = com.pocketstudio.app.rhythm.ExportFormat.M4a,
            targetChoice = TargetChoice.AUTO, targetLufs = -8.0)
        val plain = shareReportLines(done, simple = true)
        assertTrue(plain.toString(), plain.isNotEmpty())
        listOf("LUFS", "dBTP", "kHz", "kb/s", "LU").forEach { unit -> assertTrue("$unit in $plain", plain.none { unit in it }) }
        assertTrue(plain.any { "Durée : 58 s" in it })
        // The Mode pro keeps its numbers.
        assertTrue(shareReportLines(done, simple = false).any { "LUFS" in it })
    }

    /** n° 14 (A d 3, F d 7): « Partager » said 8 bars (0:16), Mix « 1 mesure », for the same loop. */
    @Test
    fun oneLoopLengthEverywhere() {
        assertEquals(ShareSong.loopBars(1), ExportUi().forProject(sceneBars = 1, hasSong = false).bars)
        assertEquals(8, ExportUi().forProject(sceneBars = 1, hasSong = false).bars)
    }

    /** n° 11 (A d 45): « Mes exports » in the app, from « Prêt à partager ? » and the export card. */
    @Test
    fun myExportsAreListed() {
        val ui = ShareSheetUi(name = "Nuit", seconds = 60, parts = 3, checks = emptyList())
        compose.setContent { PocketStudioTheme { ShareSheetContent(ui, ShareSheetActions()) } }
        compose.onNodeWithTag("share_my_exports").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("my_exports").assertIsDisplayed()
        // Robolectric's media store holds none of ours: an empty list, never a crash.
        assertEquals(emptyList<MyExport>(), MyExports.list(app))
    }
}
