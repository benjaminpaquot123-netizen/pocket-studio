package com.pocketstudio.app.tuto

import android.os.Looper
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.Project
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.tuto.TutoChecks
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoNeed
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoTarget
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * Tour 9, F038 (t9-18, t9-28, t9-36): « Aller dans la partie » switched the part but the card stayed on it, or the
 * screen opened was not the step's. Acceptance: « Aller à la partie X » ouvre la partie X et l'étape avance.
 */
@RunWith(RobolectricTestRunner::class)
class Tour9TutoGoToThePartTest {
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

    @After
    fun end() = scope.cancel()

    private fun idle(ms: Long = 50) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    /** Whether [ok] holds within [ms] (the looks answered). */
    private fun within(ms: Long, ok: () -> Boolean): Boolean {
        val end = System.currentTimeMillis() + ms
        while (!ok() && System.currentTimeMillis() < end) { idle(50); Thread.sleep(5) }
        return ok()
    }

    @Test
    fun goingToThePartOpensItAndTheStepGoesOn() {
        val checked = ArrayList<String>()
        for (style in TutoCourse.STYLES) {
            val target = TutoTarget.of(style)!!
            for (level in listOf(TutoLevel.Facile, TutoLevel.Pro)) {
                val steps = TutoCourse.of(style)!!.chaptersFor(level).flatMap { it.steps }
                // The steps that ask to go to their part, on the project the steps before them made.
                var p: Project = target.blank()
                val asks = ArrayList<Pair<String, Project>>()
                for (s in steps) {
                    if (TutoChecks.need(s, p, target) is TutoNeed.Scene) asks += s.id to p
                    if (!TutoChecks.done(s, p, target)) p = TutoChecks.show(s, p, target) ?: p
                }
                for ((id, at) in asks.take(2)) {
                    val flow = MutableStateFlow(at)
                    val goes = ArrayList<TutoGo>()
                    val c = TutoController(
                        scope, "p-$style", flow, edit = { f -> flow.value = flow.value.f() }, store = MemoryStore(),
                        render = { FloatArray(OfflineExport.SAMPLE_RATE * 2) { 0.1f } }, player = Player(), debounceMs = 10,
                    )
                    scope.launch { c.goes.collect { goes += it } }
                    idle()
                    c.start(style, level)
                    idle()
                    val chapters = c.ui.value!!.chapters
                    val ch = chapters.indexOfFirst { x -> x.steps.any { it.id == id } }
                    c.goTo(ch, chapters[ch].steps.indexOfFirst { it.id == id }); idle(200)
                    val step = c.ui.value!!.current
                    assertEquals(id, step.id)
                    val need = c.ui.value!!.need
                    assertTrue("$style/$level/$id: « Aller dans la partie » offered, not $need", need is TutoNeed.Scene)
                    val n = goes.size
                    c.fulfilNeed(); idle(300)
                    val label = (need as TutoNeed.Scene).label
                    val now = flow.value.allScenes[flow.value.scene].label
                    assertTrue("$style/$level/$id: in « $now », not in « $label »", now.contains(label, ignoreCase = true) || label.contains(now, ignoreCase = true))
                    assertTrue(
                        "$style/$level/$id: the card still on « Aller dans la partie »: ${c.ui.value!!.need}",
                        within(5_000) { c.ui.value!!.need !is TutoNeed.Scene },
                    )
                    assertEquals("$style/$level/$id: the step changed", id, c.ui.value!!.current.id)
                    val go = goes.drop(n).lastOrNull()
                    assertTrue("$style/$level/$id: no screen opened", go != null)
                    if (go!!.open != null) assertEquals("$style/$level/$id: the wrong screen", step.spot, go.open!!.spot)
                    checked += "$style/$level/$id"
                }
            }
        }
        assertTrue("checked: $checked", checked.size >= 3)
    }
}
