package com.pocketstudio.app.human

import android.app.Application
import android.net.Uri
import android.os.Looper
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.export.ExportBilan
import com.pocketstudio.app.export.MyExport
import com.pocketstudio.app.export.MyExportsList
import com.pocketstudio.app.export.shareReportLines
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.rhythm.ExportFormat
import com.pocketstudio.app.rhythm.ExportUi
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.session.livePadColor
import com.pocketstudio.app.ui.playPosition
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LiveStarter
import com.pocketstudio.model.LoudnessTarget
import com.pocketstudio.model.LoudnessTargets
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.PadChallenge
import com.pocketstudio.model.PadLive
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.Remix
import com.pocketstudio.model.SongLength
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.TargetChoice
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.switchScene
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.time.Duration

/**
 * Lot « banc6d » (PLAN-RESTE 6.3, end): the lines of verdicts.tsv left « non-tranché » that need a test without the bench
 * but no phone. No HumanTester, no BenchEngine: the app's own code (its view model, its texts, its composables), against
 * an engine scripted as a phone's ([Tour5AppProof3Test.SongEngine]) where the engine matters.
 * - T5-Fa-11, T5-Aa-43: « Partager » in the Mode simple said the level twice, « très fort » and « à la cible »;
 * - T5-Cc-23: « −14 LUFS sur tout le morceau » for a loop: the measure of the whole exported file (wanted);
 * - T5-Bb-58: 572 bars after « Annuler »: a 4-bar melody in a 1-bar scene makes each part on it 4 bars (wanted);
 * - T5-Cb-67: the column of the song muted in the Live: the app mutes its channel; the bench's sound line did not read it;
 * - T5-Fa-5: the assistant's ▶ starts before the Drop (wanted, n° 12) and the counter follows the engine from there;
 * - T5-Cc-41c: « Mes exports » gives each file its time (which one is up to date is not known by the list: wanted);
 * - T5-Ca-14: the challenge's dots at level 2 are coloured by sound family: they name no pad.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = HumanTester.PHONE)
class Tour5AppProof4Test {
    @get:Rule
    val pristine = BenchPristineRule()
    @get:Rule
    val knownDefects: TestRule = KnownDefectRule()
    @get:Rule
    val compose = androidx.compose.ui.test.junit4.createComposeRule()

    private val app: Application get() = ApplicationProvider.getApplicationContext()
    private val stores = ArrayList<androidx.lifecycle.ViewModelStore>()
    private var lang: Lang? = null

    @Before
    fun french() {
        lang = I18n.lang
        I18n.lang = Lang.FR
    }

    @After
    fun clear() {
        stores.forEach { it.clear() }
        com.pocketstudio.app.rhythm.ClosingSaves.awaitIdleBlocking(15_000)
        NativeEngine.sink = null
        lang?.let { I18n.lang = it }
    }

    private fun idle(ms: Long = 20) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    /** The app's view model on [p], made as a screen's (as Tour5AppProof3Test's). */
    private fun open(p: Project, ready: (Project) -> Boolean = { it.song.size == p.song.size }): RhythmViewModel {
        val id = "banc6d-${System.nanoTime()}"
        ProjectStore(File(app.filesDir, "projects")).save(id, p)
        CurrentProject.set(app, id)
        val store = androidx.lifecycle.ViewModelStore().also { stores += it }
        val factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = RhythmViewModel(app) as T
        }
        val vm = androidx.lifecycle.ViewModelProvider(store, factory)[RhythmViewModel::class.java]
        val until = System.currentTimeMillis() + 10_000
        while (!ready(vm.project.value)) {
            assertTrue("the project never opened", System.currentTimeMillis() < until)
            idle(); Thread.sleep(5)
        }
        return vm
    }

    /**
     * T5-Fa-11, T5-Aa-43: the Mode simple's « C'est prêt » under « Partager », for a song exported at its Auto target (a
     * techno style aims at −8 LUFS, the file reached it, as on a phone): the level is said ONCE, against the target. The
     * plain words of the loudness (« très fort (les plateformes le baisseront) ») came under it too: two lines « Niveau »
     * that contradict each other, whatever the figure (the bench's or a phone's).
     */
    @Test
    fun theSimpleShareReportSaysTheLevelOnce() {
        val done = ExportUi(files = listOf(File("a.m4a")), loudness = -8.0, truePeakDb = -1.2, seconds = 58.0, writtenFormat = ExportFormat.M4a,
            targetChoice = TargetChoice.AUTO, targetLufs = -8.0)
        val lines = shareReportLines(done, simple = true)
        val levels = lines.filter { it.startsWith("Niveau") }
        assertEquals("one level line, against the target: $lines", listOf("Niveau : à la cible (Auto (style))"), levels)
        assertTrue("the rest of the report is still there: $lines", lines.any { it.startsWith("Durée") } && lines.any { "M4A" in it })
        // Without a target, the plain words of the loudness stay (nothing else says the level).
        val free = shareReportLines(done.copy(targetChoice = TargetChoice.OFF, targetLufs = null), simple = true)
        assertEquals("no target: the plain words: $free", listOf("Niveau : très fort (les plateformes le baisseront)"), free.filter { it.startsWith("Niveau") })
    }

    /**
     * T5-Cc-23 (« −14 LUFS over the whole song » while « The looping part » was exported): the Streaming target is measured
     * on the whole exported file, a loop as much as a song (LoudnessTargets.wholeSong, the export's report): « sur tout
     * le morceau » sets it against Auto and Club, measured on the loudest passage. A loop at −14 LUFS whose loudest
     * passage is −10 is at the target.
     */
    @Test
    fun theStreamingTargetIsMeasuredOnTheWholeExportedFile() {
        val hint = com.pocketstudio.app.mixer.targetHint(TargetChoice.STREAMING, -14f)
        assertTrue(hint, "sur tout le morceau" in hint)
        assertTrue("Auto: the loudest passage", "passage le plus fort" in com.pocketstudio.app.mixer.targetHint(TargetChoice.AUTO, -8f))
        assertTrue("Streaming aims at the whole file", LoudnessTargets.wholeSong(Project().mixer.copy(target = LoudnessTarget.STREAMING)))
        val loop = ExportBilan(lufs = -14.0, truePeakDb = -1.0, format = "M4A", seconds = 16.0, choice = TargetChoice.STREAMING, targetLufs = -14.0,
            wholeSong = true, loudLufs = -10.0)
        assertEquals("Niveau : à la cible (Streaming)", loop.levelWords())
    }

    /**
     * T5-Bb-58 (« Annuler » of a Live take gave 572 bars, 176 before): « Annuler » gives the song back as it was
     * (Tour5AppProof2Test); the 572 bars were there before the take. A scene is as long as its longest loop
     * (Scenes.stepsFor): a melody of 4 bars kept in a scene of 1 bar makes every part that plays that scene 4 bars long
     * (176 + 3 × 132 = 572). Three parts of one bar: 3 bars, 12 with the melody.
     */
    @Test
    fun aFourBarMelodyInAOneBarSceneLengthensEachPartOnIt() {
        val song = Project().let { it.copy(drums = it.drums.toggle(0, 0)) }.copy(song = List(3) { SongPart(0) }, tempoBpm = 120.0)
        assertEquals(3, SongLength.of(song).bars)
        val kept = song.copy(synthNotes = NoteClip(steps = 64, notes = listOf(Note(0, 4, 60), Note(48, 4, 67))))
        assertEquals("each part on the scene is 4 bars now", 12, SongLength.of(kept).bars)
        // A part whose length is set keeps it.
        assertEquals(1 + 4 + 4, SongLength.of(kept.copy(song = listOf(SongPart(0, bars = 1), SongPart(0), SongPart(0)))).bars)
    }

    /**
     * T5-Cb-67 (the song's column muted in the Live, the bench's sound line said « avec la chanson … entière »): the
     * Live's mute is not written in the project (the song's parts stay on); the app mutes the song's channel on the
     * engine at once. The bench's line read only the parts of the song (HumanTester.remixHeard), never the Live's mute.
     */
    @Test
    fun theSongsColumnMutedInTheLiveMutesItsChannel() {
        val engine = Tour5AppProof3Test.SongEngine()
        NativeEngine.sink = engine
        val vm = open(Project().copy(remix = Remix(setId = "banc6d-song", name = "Ma chanson préférée")), ready = { it.remix.setId != null })
        assertTrue("not cut elsewhere", SongPart.REMIX !in vm.liveCutElsewhere())
        vm.toggleLiveMute(SongPart.REMIX)
        idle()
        assertTrue("muted in the Live", SongPart.REMIX in vm.liveMuted.value)
        val mutes = engine.posts.filter { it[0] == CMD_CHANNEL_MUTE }
        assertTrue("the song's channel (3) muted on the engine: $mutes", mutes.lastOrNull()?.let { it[1].toFloat() == 1f && it[2].toInt() == 3 } == true)
        assertTrue("the song's parts stay on (the bench's line reads these only)", vm.project.value.remix.stems.none { it.mute })
    }

    /**
     * T5-Fa-5 (the assistant's ▶: the song started at bar 25 of 96, « Montée », the counter stood at 25/96): « Créer et
     * écouter » plays from about 8 bars before the first Drop (audit ultra round 2, n° 12: AppRoot › listenFromDrop ›
     * playSongPart). From a part in the middle of the song, the counter follows the engine: bar 2/3 then 3/3.
     */
    @Test
    fun theSongStartedInItsMiddleCountsOn() {
        NativeEngine.sink = Tour5AppProof3Test.SongEngine()
        val p = Project().let { it.copy(drums = it.drums.toggle(0, 0)) }.switchScene(1)
            .let { it.copy(drums = it.drums.toggle(0, 4)) }.switchScene(0)
            .copy(song = listOf(SongPart(0), SongPart(1), SongPart(0)), tempoBpm = 120.0)
        val vm = open(p)
        val poll = vm.viewModelScope.launch { vm.pollPosition() }
        try {
            vm.playSongPart(1)
            val bars = ArrayList<String>()
            var waited = 0L
            while (waited < 9_000 && (bars.isEmpty() || vm.transport.value.playing)) {
                idle(50); waited += 50
                if (!vm.transport.value.playing) continue
                val q = vm.project.value
                playPosition(q, true, vm.sessionUi.value.songIndex, vm.playBar.value, vm.playFrom.value, vm.loopPart.value)
                    ?.let { pos -> "${pos.bar}/${pos.bars}".takeIf { bars.lastOrNull() != it }?.let(bars::add) }
            }
            assertEquals("the counter starts at the part asked and goes on (${waited} ms)", listOf("2/3", "3/3"), bars)
        } finally {
            poll.cancel()
        }
    }

    /**
     * T5-Cc-41c (« Mes exports »: which version is up to date?): each file is listed with its name (the time of the export in
     * it, WavExporter.fileName) and its date and time. Which one matches the song now is not something the list knows (no
     * link between a file sent and a version of the song): an idea, not a defect.
     */
    @Test
    fun myExportsGivesEachFileItsTime() {
        val newer = MyExport("Mon-morceau-20261105-211648.m4a", Uri.parse("content://x/2"), 900_000, 1_762_377_408L, "audio/mp4")
        val older = MyExport("Mon-morceau-20261105-211622.m4a", Uri.parse("content://x/1"), 880_000, 1_762_377_382L, "audio/mp4")
        compose.setContent { com.pocketstudio.ui.PocketStudioTheme { MyExportsList(listOf(newer, older)) {} } }
        compose.waitForIdle()
        val details = compose.onAllNodesWithTag("my_export_detail", useUnmergedTree = true).fetchSemanticsNodes()
            .map { n -> n.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text }.orEmpty() }
        assertEquals("two rows: $details", 2, details.size)
        assertTrue("each row its own date and time: $details", details[0] != details[1] && details.all { it.contains(":") })
        assertTrue(exportNameHasItsTime())
    }

    private fun exportNameHasItsTime(): Boolean =
        com.pocketstudio.app.export.WavExporter.fileName("Mon morceau", java.time.LocalDateTime.of(2026, 11, 5, 21, 16, 48)).contains("211648")

    /**
     * T5-Ca-14 (the challenge at level 2: yellow dots, no pad named; four yellow pads): the rhythm's dots take their pad's
     * colour, and a pad's colour is its sound's family (livePadColor). On the page the Pad Live gets (LiveStarter), a dot
     * of level 2 shares its colour with other pads: it designates none.
     */
    @Test
    @KnownDefect("T5-DEFI-COULEURS", "a dot of the challenge has the colour of several pads")
    fun theChallengesDotsNameTheirPad() {
        val clips = LiveStarter.PADS.map { LiveClip("${it.name}.wav", it.name, it.beats, oneShot = it.oneShot) }
        val pads = PadLive.placed(PadLive.newProject().live, 0, clips)
        val c = PadChallenge.make(2, PadChallenge.shotsOf(pads, 0), 120.0)
        assertTrue("a challenge of level 2", c != null && c.hits.isNotEmpty())
        val colour = (0 until PadLive.PAGE_SIZE).associateWith { i -> pads.at(i)?.let { livePadColor(it.name, i) } }
        for (pad in c!!.hits.map { it.pad }.distinct()) {
            val same = colour.filter { (i, col) -> i != pad && col == colour[pad] }.keys.map { pads.at(it)?.name }
            assertTrue("a dot of the challenge has the colour of several pads: « ${pads.at(pad)?.name} » and $same", same.isEmpty())
        }
    }

    private companion object {
        const val CMD_CHANNEL_MUTE = 22
    }
}
