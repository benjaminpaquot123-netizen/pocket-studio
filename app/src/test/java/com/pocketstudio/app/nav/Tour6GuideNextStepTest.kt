package com.pocketstudio.app.nav

import android.app.Application
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.guide.GuideCard
import com.pocketstudio.app.guide.GuideCardUi
import com.pocketstudio.app.guide.GuidePanelActions
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Project
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.guide.GuideAction
import com.pocketstudio.model.guide.GuideLibrary
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.time.Duration

/**
 * Audit ultra round 6, G3 (seat F b, screen 41, in Polish): « Twoja kolej: dotknij „Później” ». On « Fais une deuxième
 * partie » the Guide's one sentence (« À toi : … ») told the beginner to touch « Plus tard », as if putting the card
 * off were the task: its real choice opens a screen (« Voir les parties ») and « Plus tard » was the only other one.
 * The sentence never names a choice that puts the card off; it names the screen to open instead.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-xhdpi")
class Tour6GuideNextStepTest {
    @get:Rule
    val compose = createComposeRule()
    private val app: Application = ApplicationProvider.getApplicationContext()

    @After
    fun back() {
        I18n.lang = Lang.FR
        I18n.everyday = false
    }

    private fun await(what: String, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + 60_000
        while (!ok()) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
            check(System.currentTimeMillis() < end) { "$what: not in time" }
            Thread.sleep(5)
        }
    }

    /** The Guide's cards of the song [build] makes, as its panel gets them, and the song. */
    private fun cardsOf(simple: Boolean, build: RhythmViewModel.() -> Unit): Pair<Map<String, GuideCardUi>, Project> {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app).also(build)
        vm.guide.setSimple(simple)
        await("the Guide's analysis") { !vm.guide.ui.value.loading && vm.guide.ui.value.simple == simple && vm.guide.ui.value.cards.isNotEmpty() }
        return vm.guide.ui.value.cards to vm.project.value
    }

    /** The labels, as shown, of the choices of [p]'s cards that only put the card off (« Plus tard »). */
    private fun laterLabels(p: Project, simple: Boolean): Map<String, Set<String>> =
        GuideLibrary.forProject(p).cards.associate { c -> c.id to c.proposals.filter { it.action.type == GuideAction.LATER }.map { it.labelFor(simple) }.toSet() }

    /** A song made by hand: one beat, one part (« Fais une deuxième partie » is to do). */
    private val byHand: RhythmViewModel.() -> Unit = { toggleHit(0, 0) }

    private val jumpUp: RhythmViewModel.() -> Unit = {
        val style = StarterWizard.style("jump_up")!!
        val song = StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? }, style.melody.mood,
            StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() })
        session.edit { song }
    }

    /** Each card of the song drawn in turn: what its « À toi : … » says, by card; and the song. */
    private fun sentences(simple: Boolean, lang: Lang, build: RhythmViewModel.() -> Unit): Pair<Map<String, String>, Project> {
        I18n.lang = lang
        val (cards, p) = cardsOf(simple, build)
        var shown by mutableStateOf<GuideCardUi?>(null)
        compose.setContent { PocketStudioTheme { shown?.let { GuideCard(it, null, GuidePanelActions()) } } }
        return cards.mapValues { (_, card) ->
            shown = card
            compose.waitForIdle()
            compose.onAllNodesWithTag("guide_next_step", useUnmergedTree = true).fetchSemanticsNodes().flatMap { Tour6Shell.textsOf(it) }.joinToString(" ")
        } to p
    }

    private fun neverAsksToPutOff(simple: Boolean, lang: Lang, build: RhythmViewModel.() -> Unit) {
        val (said, p) = sentences(simple, lang, build)
        val later = laterLabels(p, simple)
        assertTrue("the song has cards that can be put off", later.values.any { it.isNotEmpty() })
        val wrong = said.filter { (id, text) -> later[id].orEmpty().any { text.contains(it) } }
        assertTrue("the Guide asks to touch « Plus tard » as if it were the task: $wrong", wrong.isEmpty())
    }

    /** Seat F b: « Fais une deuxième partie », in Polish: the sentence names « Voir les parties », the screen to open. */
    @Test
    fun aSecondPartAsksToSeeThePartsNotToPutItOff() {
        val said = sentences(simple = false, lang = Lang.PL, build = byHand).first["start.parts"]
        assertNotNull("« Fais une deuxième partie » is one of the song's cards", said)
        assertTrue("« $said » asks to touch « ${tr("Plus tard")} »", !said!!.contains(tr("Plus tard")))
        assertTrue("« $said » names « ${tr("Voir les parties")} »", said.contains(tr("Voir les parties")))
    }

    @Test fun noCardOfASongByHandAsksToPutItOff() = neverAsksToPutOff(simple = false, lang = Lang.FR, build = byHand)

    @Test fun noCardOfASongByHandAsksToPutItOffInSimple() = neverAsksToPutOff(simple = true, lang = Lang.FR, build = byHand)

    @Test fun noCardOfAStyleSongAsksToPutItOff() = neverAsksToPutOff(simple = false, lang = Lang.FR, build = jumpUp)
}
