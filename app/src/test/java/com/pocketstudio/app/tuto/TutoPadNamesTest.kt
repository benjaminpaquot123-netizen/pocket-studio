package com.pocketstudio.app.tuto

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.app.rhythm.DrumKitInfo
import com.pocketstudio.app.rhythm.padName
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoTarget
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * Audit ultra round 3, n° 29 (third time, round 2 n° 28): the tutorial said « sur le pad « Grosse caisse 1 » »
 * (seat A c, fr, Simple) and « op de pad ‘Basdrum 1’ » (seat E b, nl, Simple) while the pad showed « Grosse caisse
 * studio » / « Studiobasdrum » and its sound card « Basdrum 1 (gesynthetiseerd) ». One name: the pad's own, in
 * the step's words as on the pad, in every language and in Simple and Pro.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class TutoPadNamesTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)

    @After
    fun end() {
        TutoTestHooks.kitOffer = true
        h.finish()
    }

    private fun idle(ms: Long = 50) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
    private fun await(what: String, timeoutMs: Long = 30_000, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (!ok()) {
            idle(50)
            compose.mainClock.advanceTimeBy(50)
            compose.waitForIdle()
            check(System.currentTimeMillis() < end) { "$what: not in time" }
        }
    }
    private fun has(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    private fun text(tag: String): String = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().first()
        .config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }.orEmpty()

    /** The « Kick » step of [style] in [lang], [level]: the step's words name the pad as the pad shows it. */
    private fun kickStep(style: String, lang: Lang, level: TutoLevel) {
        val target = TutoTarget.of(style)!!
        val flat = TutoCourse.of(style)!!.chaptersFor(level).flatMap { it.steps }
        val step = flat.first { "« Kick »" in it.textFor(level) }
        val done = flat.takeWhile { it.id != step.id }.map { it.id }.toSet()
        TutoTestHooks.kitOffer = false
        h.start(simple = level == TutoLevel.Facile, lang = lang, build = {
            session.edit { target.blank() }
            PrefsTutoStore(h.app).save(projectId, TutoProgress(style, level, done = done, current = step.id))
        })
        await("the step's words") { has("tuto_text") }
        val words = text("tuto_text")
        val pad = padName(h.vm.project.value, 0)
        val kit = DrumKitInfo.names[0]
        assertTrue("the step names the pad « $pad » ($lang, $level): $words", pad in words)
        if (kit != pad) {
            assertFalse("the step names « $kit », a pad that is not on the screen ($lang, $level): $words",
                Regex("[«“„‘\"][\\s  ]?${Regex.escape(kit)}[\\s  ]?[»”“’\"]").containsMatchIn(words))
        }
        // The pad shows that very name.
        assertTrue("pad 0 does not show « $pad »", compose.onAllNodes(androidx.compose.ui.test.hasText(pad, substring = true), useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun frenchSimpleRetroTechno() = kickStep("retro_techno", Lang.FR, TutoLevel.Facile)

    @Test
    fun dutchSimpleRetroTechno() = kickStep("retro_techno", Lang.NL, TutoLevel.Facile)

    @Test
    fun frenchProTechno() = kickStep("techno", Lang.FR, TutoLevel.Pro)

    @Test
    fun turkishSimpleTechno() = kickStep("techno", Lang.TR, TutoLevel.Facile)
}
