package com.pocketstudio.app.tuto

import android.os.Looper
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.Project
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.tuto.TutoChecks
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoNeed
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoTarget
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * Tour 9, F045 (t9-38, t9-65, t9-69): « Partie créée … : sans … » listed what the part leaves out with Latin commas
 * in Chinese and Japanese. Acceptance: le message « partie créée » est écrit entièrement dans ma langue, avec sa
 * ponctuation.
 */
@RunWith(RobolectricTestRunner::class)
class Tour9TutoPartMadeInItsLanguageTest {
    private class MemoryStore : TutoStore {
        val saved = HashMap<String, TutoProgress>()
        override fun load(projectId: String) = saved[projectId]
        override fun save(projectId: String, progress: TutoProgress?) {
            if (progress == null) saved.remove(projectId) else saved[projectId] = progress
        }
    }

    private class Player : GuideListenPlayer {
        override fun play(pcm: Pcm, ended: () -> Unit): Boolean = true
        override fun stop() {}
        override fun heard(): Long = 0L
    }

    private val scope = MainScope()
    private val style = "techno"
    private val target = TutoTarget.of(style)!!
    private val steps = TutoCourse.of(style)!!.chapters.flatMap { it.steps }.associateBy { it.id }
    private val was = I18n.lang

    @After
    fun end() {
        scope.cancel()
        I18n.lang = was
    }

    private fun idle(ms: Long = 50) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    private fun upTo(vararg ids: String): Project {
        var p = target.blank()
        for (id in ids) if (!TutoChecks.done(steps.getValue(id), p, target)) p = TutoChecks.show(steps.getValue(id), p, target)!!
        return p
    }

    /** The message of « Créer la partie « Intro » » on a full Groove, in [lang]. */
    private fun introMade(lang: Lang): String {
        I18n.lang = lang
        val flow = MutableStateFlow(upTo("kick_do", "rumble_do", "clap_do", "open_hat_do", "perc_do", "bass_do", "bass_end", "pad_do", "pad_end", "lead_do", "groove_name"))
        val c = TutoController(
            scope, "p1", flow, edit = { f -> flow.value = flow.value.f() }, store = MemoryStore(),
            render = { FloatArray(OfflineExport.SAMPLE_RATE * 2) { 0.1f } }, player = Player(), debounceMs = 10,
        )
        idle()
        c.start(style, TutoLevel.Facile)
        idle()
        val chapters = c.ui.value!!.chapters
        val ch = chapters.indexOfFirst { x -> x.steps.any { it.id == "make_intro" } }
        c.goTo(ch, chapters[ch].steps.indexOfFirst { it.id == "make_intro" }); idle()
        assertTrue("$lang: « Créer la partie » offered, not ${c.ui.value!!.need}", c.ui.value!!.need is TutoNeed.Part)
        c.fulfilNeed(); idle(100)
        return c.ui.value!!.made.orEmpty()
    }

    @Test
    fun whatThePartLeavesOutIsListedWithTheLanguagesOwnCommas() {
        val fr = introMade(Lang.FR)
        // The Intro leaves several things out: a list, written as French writes it.
        assertTrue("« $fr »: no list to read", ", " in fr.substringAfter("sans", ""))
        for (lang in listOf(Lang.ZH_CN, Lang.JA)) {
            val made = introMade(lang)
            assertTrue("$lang: « $made »", made.isNotEmpty())
            assertFalse("$lang: « $made » lists with a Latin comma", ',' in made)
            assertTrue("$lang: « $made » lists with no comma of its own", '、' in made)
        }
        assertFalse("ar: Latin comma in an Arabic list", ',' in introMade(Lang.AR))
    }
}
