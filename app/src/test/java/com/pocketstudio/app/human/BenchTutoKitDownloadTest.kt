package com.pocketstudio.app.human

import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.audio.FloatWav
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.export.FlacEncoder
import com.pocketstudio.app.library.IndexState
import com.pocketstudio.app.library.PackStore
import com.pocketstudio.app.library.PacksUi
import com.pocketstudio.app.tuto.PrefsTutoStore
import com.pocketstudio.app.tuto.TutoTestHooks
import com.pocketstudio.model.PackEntry
import com.pocketstudio.model.PackIndex
import com.pocketstudio.model.PackManifest
import com.pocketstudio.model.PackSound
import com.pocketstudio.model.tuto.TutoKit
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoTarget
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Tour 9, D17 (seat t9-75, UK Garage, Pro): « Kit UK Garage downloaden… 0 % » vanished, the kit stayed « Studio ». The pack's
 * FLACs land in the middle of a wait (a real download); the tutorial decodes them as soon as the main looper sees the pack,
 * before the bench had shown them to Robolectric's extractor ([BenchFlac] only looked before and after each gesture): every
 * sound was « illisible (geen audiospoor) » and the kit fell back to the built-in one. On a phone the FLAC decodes, the kit stays.
 *
 * Here the pack (real FLACs, 44.1 kHz stereo as the release's) lands 300 ms into « attends 3 » on the main looper's clock,
 * between two of the bench's looks: the seat must keep the kit (kit = pack, its pads loaded).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class BenchTutoKitDownloadTest : HumanTester("tuto-kit-download") {
    private val packs = MutableStateFlow(PacksUi())
    private val style = "uk_garage"
    private val target = TutoTarget.of(style)!!
    private val entries: List<PackEntry> by lazy {
        val s = target.style
        val ids = (s.lines.filter { it.voice == null }.mapNotNull { s.kit[it.role] } + s.id).distinct()
        ids.map { PackEntry(it, "Pack $it", "$it.zip", "a".repeat(64), 3_000_000, 12) }
    }
    private val installed: List<PackManifest> by lazy {
        val kinds = target.style.lines.map { it.role.packKind }.distinct()
        entries.map { e -> PackManifest(e.id, e.name, sounds = kinds.map { k -> PackSound("$k.flac", k, "$k ${e.id}") }) }
    }

    @Before
    fun notOnTheBench() = assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())

    @After
    fun noHook() {
        TutoTestHooks.packs = null
    }

    private fun ready(installed: List<PackManifest> = emptyList(), downloading: Map<String, Float> = emptyMap()) =
        PacksUi(IndexState.Ready(PackIndex(entries)), installed, downloading)

    @Test
    fun aKitDownloadedDuringAWaitStaysOnThePads() {
        val (o, warnings) = SeatOptions.parse(mapOf("lang" to "nl", "device" to "phone", "font" to "100", "mode" to "pro", "amenee" to "tuto:$style:1"))
        assertTrue(warnings.toString(), warnings.isEmpty())
        packs.value = ready()
        TutoTestHooks.packs = packs
        begin(o)
        val journal = File.createTempFile("amenee", ".txt").apply { deleteOnExit() }
        val failed = runCatching { amenee("tuto:$style:1", journal) }.exceptionOrNull()
        val told = { "${failed?.message}\n${journal.readText()}\n${screenLines().joinToString("\n")}" }
        assertTrue(told(), failed == null)
        // The card's « Télécharger » (the text above it read to its end first, as a person scrolls it).
        repeat(12) { if (centerOf("tuto_kit_download") == null && centerOf("tuto_text_more") != null) act("touche ${centerOf("tuto_text_more")!!.first} ${centerOf("tuto_text_more")!!.second}") }
        val at = reachTag("tuto_kit_download")
        assertNotNull("the kit's card and its button\n${told()}", at)
        act("touche ${at!!.first} ${at.second}")
        packs.value = ready(downloading = entries.associate { it.id to 0f })
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        // A real FLAC (the app's own encoder), as the pack's zip holds.
        val wav = File(app.cacheDir, "d17-src.wav")
        FloatWav.write(wav, Pcm(FloatArray(2 * 65_557) { k -> 0.4f * kotlin.math.sin(k * 0.05f) * (1f - k / 131_114f) }, 2, 44_100))
        val flac = File(app.cacheDir, "d17-src.flac").also { FlacEncoder.encode(wav, it) }
        // Landed from the main looper's clock, 300 ms into the wait: the app sees the pack between two of the bench's looks.
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            installed.forEach { m -> m.sounds.forEach { s -> PackStore.file(app, m.id, s).apply { parentFile!!.mkdirs(); writeBytes(flac.readBytes()) } } }
            packs.value = ready(installed)
        }, 300)
        act("attends 3")
        act("attends 2")
        val vm = rhythm()!!
        val progress = PrefsTutoStore(app).load(vm.projectId)!!
        val pads = vm.project.value.drums.lanes.withIndex().filter { it.value.sample != null }.map { it.index }.toSet()
        val want = TutoKit.sounds(target, installed).keys
        val log = com.pocketstudio.app.report.AppLog.dump(20)
        assertEquals("the kit is the pack (log: $log)\n${screenLines().joinToString("\n")}", TutoProgress.KIT_PACK, progress.kit)
        assertEquals("the pack's sounds are on their pads (log: $log)", want, pads)
    }
}
