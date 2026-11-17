package com.pocketstudio.app.tuto

import android.os.Looper
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.Project
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import com.pocketstudio.model.allScenes
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * Tour 7, tuto3 (seat d, House « Les transitions »): with one scene « A » and no « Groove » in the song, « Créer la
 * partie » said « (copie du Groove) »: the part copied is said by its name.
 */
@RunWith(RobolectricTestRunner::class)
class Tour7TutoPartMadeSourceTest {
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

    @After
    fun end() = scope.cancel()

    private fun idle(ms: Long = 50) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    private fun upTo(vararg ids: String): Project {
        var p = target.blank()
        for (id in ids) if (!TutoChecks.done(steps.getValue(id), p, target)) p = TutoChecks.show(steps.getValue(id), p, target)!!
        return p
    }

    @Test
    fun aCopyOfAIsSaidACopyOfA() {
        // The kick and the clap dotted in « A », never named « Groove ».
        val flow = MutableStateFlow(upTo("kick_do", "clap_do"))
        assertTrue(flow.value.allScenes.none { it.label.equals("Groove", ignoreCase = true) })
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
        assertEquals(TutoNeed.Part("Intro"), c.ui.value!!.need)
        c.fulfilNeed(); idle(100)
        val made = c.ui.value!!.made.orEmpty()
        assertFalse("« $made » names a Groove the song has not", "Groove" in made)
        assertTrue("« $made » does not name « A »", "« A »" in made)
    }
}
