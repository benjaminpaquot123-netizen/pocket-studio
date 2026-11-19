package com.pocketstudio.app.human

import com.pocketstudio.app.library.IndexState
import com.pocketstudio.app.library.PacksUi
import com.pocketstudio.app.tuto.TutoTestHooks
import com.pocketstudio.model.PackEntry
import com.pocketstudio.model.PackIndex
import com.pocketstudio.model.tuto.TutoTarget
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
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
 * Tour 9 (seats t9-16 and t9-38): « amenée tuto:… : les chapitres n'est jamais venu », now and then, on a loaded machine.
 * Reproduced cold (16 CPU spinners, 4 cold JVMs, 1 amenée in 12): the tutorial's « Télécharger le kit » card waits for the
 * packs' catalogue, read off the main thread in real time. When it comes between the bench's aim at « Chapitres » and its
 * touch, the panel grows, the button moves up 40 px and the finger lands beside it; nothing looked at what the touch did,
 * and the amenée waited 120 s for a list that could not come.
 *
 * Here the catalogue comes exactly then, each time: the amenée must still bring the seat to its chapter.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class BenchAmeneeLateKitCardTest : HumanTester("amenee-kit-tardif") {
    private val packs = MutableStateFlow(PacksUi())

    @Before
    fun notOnTheBench() = assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())

    @After
    fun noHook() {
        TutoTestHooks.packs = null
        ameneeAfterAim = null
    }

    @Test
    fun theKitCardComingUnderTheFingerDoesNotRefuseTheAmenee() {
        val amenee = "tuto:deep_house:7"
        val (o, warnings) = SeatOptions.parse(mapOf("lang" to "es", "device" to "phone", "font" to "130", "mode" to "simple", "amenee" to amenee))
        assertTrue(warnings.toString(), warnings.isEmpty())
        TutoTestHooks.packs = packs // the catalogue is not there yet: no kit card
        begin(o)
        val journal = File.createTempFile("amenee", ".txt").apply { deleteOnExit() }
        ameneeCapMs = 20_000
        var aimedAt: Pair<Float, Float>? = null
        var came = false
        beforeGesture = { cmd ->
            // The first touch after the chapters are built is the one aimed at « Chapitres »: the catalogue comes now.
            if (!came && cmd.startsWith("touche") && journal.readText().contains("construits")) {
                came = true
                aimedAt = centerOf("tuto_chapters")
                val s = TutoTarget.of("deep_house")!!.style
                val ids = (s.lines.filter { it.voice == null }.mapNotNull { s.kit[it.role] } + s.id).distinct()
                packs.value = PacksUi(IndexState.Ready(PackIndex(ids.map { PackEntry(it, "Pack $it", "$it.zip", "a".repeat(64), 1_500_000, 12) })))
            }
        }
        val failed = runCatching { amenee(amenee, journal) }.exceptionOrNull()
        beforeGesture = null
        val lines = journal.readLines()
        val told = "${failed?.message}\n${lines.joinToString("\n")}\n${screenLines().joinToString("\n")}"
        assertTrue("the catalogue came during the amenée\n$told", came)
        // The trap is real: the card is there and it moved the button the bench had aimed at.
        assertTrue("the kit card moved « Chapitres » (aimed at $aimedAt, now ${centerOf("tuto_chapters")})\n$told", centerOf("tuto_kit") != null && centerOf("tuto_chapters") != aimedAt)
        assertTrue(told, failed == null && lines.last().startsWith("#0 AMENEE prête"))
        // The touch that went beside its target is said, once, where and why (tour 9, banc A: drapeaux).
        val missed = lines.mapNotNull { BenchFlags.parse(it) }.filter { it["where"] == "amenee.tap-missed" }
        assertTrue("one « amenee.tap-missed » flag naming the button and both places\n$told", missed.size == 1 && missed[0]["saw"]!!.startsWith("tuto_chapters visé ") && "puis tuto_chapters@" in missed[0]["saw"]!!)
        assertTrue("no verdict from an amenée that is ready\n$told", lines.none { BenchFlags.VERDICT in it })
    }

    /**
     * Tour 9 (seats t9-57 reggaeton:8 tr, t9-45 liquid_dnb:12 nl, phone360): the catalogue came between the bench's aim at
     * « Chapitres » and its reading of where that button was: the touch went at the old place (y 492), the button was
     * already at the new one (y 336), and the second touch compared the new place with itself: no « tap-missed », no second
     * touch, 120 s of waiting, « les chapitres n'est jamais venu ».
     */
    @Test
    fun reggaetonChapter8OnPhone360() = cardBetweenAimAndTouch("tuto:reggaeton:8", "reggaeton", mapOf("lang" to "tr", "device" to "phone360", "font" to "100", "mode" to "simple"))

    @Test
    fun liquidDnbChapter12OnPhone360() = cardBetweenAimAndTouch("tuto:liquid_dnb:12", "liquid_dnb", mapOf("lang" to "nl", "device" to "phone360", "font" to "100", "mode" to "pro"))

    private fun cardBetweenAimAndTouch(amenee: String, style: String, profile: Map<String, String>) {
        val (o, warnings) = SeatOptions.parse(profile + ("amenee" to amenee))
        assertTrue(warnings.toString(), warnings.isEmpty())
        TutoTestHooks.packs = packs // the catalogue is not there yet: no kit card
        begin(o)
        val journal = File.createTempFile("amenee", ".txt").apply { deleteOnExit() }
        ameneeCapMs = 20_000
        var aimedAt: Pair<Float, Float>? = null
        var movedTo: Pair<Float, Float>? = null
        ameneeAfterAim = { tag ->
            // The first aim at « Chapitres » after the chapters are built: the catalogue comes before the bench looks again.
            if (aimedAt == null && tag == "tuto_chapters" && "construits" in journal.readText()) {
                aimedAt = centerOf("tuto_chapters")
                val s = TutoTarget.of(style)!!.style
                val ids = (s.lines.filter { it.voice == null }.mapNotNull { s.kit[it.role] } + s.id).distinct()
                packs.value = PacksUi(IndexState.Ready(PackIndex(ids.map { PackEntry(it, "Pack $it", "$it.zip", "a".repeat(64), 1_500_000, 12) })))
                movedTo = centerOf("tuto_chapters") // where the button is before the touch (the screen has taken the card)
            }
        }
        val failed = runCatching { amenee(amenee, journal) }.exceptionOrNull()
        ameneeAfterAim = null
        val lines = journal.readLines()
        val told = "${failed?.message}\n${lines.joinToString("\n")}\n${screenLines().joinToString("\n")}"
        assertTrue("the catalogue came during the amenée\n$told", aimedAt != null)
        // The trap is real: the kit card is there and it moved « Chapitres » up from where the bench aimed (seats: y 492 → 336).
        assertTrue("the kit card moved « Chapitres » (aimed at $aimedAt, then at $movedTo)\n$told", movedTo != null && movedTo!!.second < aimedAt!!.second - 4f)
        assertTrue(told, failed == null && lines.last().startsWith("#0 AMENEE prête"))
        val missed = lines.mapNotNull { BenchFlags.parse(it) }.filter { it["where"] == "amenee.tap-missed" }
        assertTrue("one « amenee.tap-missed » flag naming the button and both places\n$told", missed.size == 1 && missed[0]["saw"]!!.startsWith("tuto_chapters visé ") && "puis tuto_chapters@" in missed[0]["saw"]!!)
        assertTrue("no verdict from an amenée that is ready\n$told", lines.none { BenchFlags.VERDICT in it })
    }
}
