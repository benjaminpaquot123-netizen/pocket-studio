package com.pocketstudio.bentest

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inspector.WindowInspector
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getAllSemanticsNodes
import androidx.compose.ui.semantics.getOrNull
import com.pocketstudio.app.journey.JourneyDriver
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Where the robot reaches the app's screens from: [run] on the main thread, [pause] lets it work. */
interface MainThread {
    fun <T> run(block: () -> T): T
    fun pause(ms: Long)
}

/** The phone's: the robot on its own thread, each look and touch posted to the main thread and waited for. */
class LooperMainThread(private val timeoutMs: Long = 10_000) : MainThread {
    private val handler = Handler(Looper.getMainLooper())

    override fun <T> run(block: () -> T): T {
        if (Looper.myLooper() == Looper.getMainLooper()) return block()
        val done = CountDownLatch(1)
        var result: Result<T>? = null
        handler.post {
            result = runCatching(block)
            done.countDown()
        }
        check(done.await(timeoutMs, TimeUnit.MILLISECONDS)) { "main thread busy for $timeoutMs ms" }
        return result!!.getOrThrow()
    }

    override fun pause(ms: Long) = Thread.sleep(ms)
}

/**
 * BenStudio Test's [JourneyDriver] (docs/PLAN-TEST-APK.md, « Le robot est dans l'appli »): it reads
 * the screen through Compose's semantics tree, in every window of the app ([windows]: the activity,
 * its dialogs, sheets and popups), and touches it with real touch events sent to the window, as a
 * finger would (a button that only listens to the finger answers too). No test library, no
 * instrumentation: it runs inside the app on the phone.
 */
class InAppDriver(
    private val main: MainThread,
    private val windows: () -> List<View> = { WindowInspector.getGlobalWindowViews() },
) : JourneyDriver {
    /** The current step's end (BenStudio Test's [Passage]): a wait past it gives up. */
    @Volatile
    var deadlineMs: Long = Long.MAX_VALUE

    private class Found(val node: SemanticsNode, val root: ViewRootForTest)

    private fun roots(): List<ViewRootForTest> {
        val found = ArrayList<ViewRootForTest>()
        fun visit(v: View) {
            if (v is ViewRootForTest) found += v
            if (v is ViewGroup) for (i in 0 until v.childCount) visit(v.getChildAt(i))
        }
        windows().filter { it.isAttachedToWindow }.forEach(::visit)
        return found
    }

    private fun nodes(merged: Boolean): List<Found> =
        roots().flatMap { r -> r.semanticsOwner.getAllSemanticsNodes(mergingEnabled = merged).map { Found(it, r) } }

    private fun tagged(tag: String, merged: Boolean) = nodes(merged).filter { it.node.config.getOrNull(SemanticsProperties.TestTag) == tag }

    private fun one(tag: String): Found {
        val all = tagged(tag, merged = true)
        // Two windows can hold it while a sheet opens over the screen: the top one (the last window) is the one a finger meets.
        return all.lastOrNull() ?: error("nothing tagged $tag")
    }

    override fun count(tag: String, merged: Boolean): Int = main.run { tagged(tag, merged).size }

    override fun bounds(tag: String): Rect = main.run { one(tag).node.boundsInWindow }

    override fun tags(pattern: Regex): List<String> =
        main.run { nodes(merged = false).mapNotNull { it.node.config.getOrNull(SemanticsProperties.TestTag) }.filter { pattern.matches(it) } }

    /**
     * What is on screen for a failed step's note: the tags of the nodes that show (a size once
     * clipped), the top window's first (the last one: a dialog, a sheet), then the ones under it.
     */
    fun ecran(): List<String> = main.run {
        val out = LinkedHashSet<String>()
        for (r in roots().asReversed()) {
            for (n in r.semanticsOwner.getAllSemanticsNodes(mergingEnabled = false)) {
                val tag = n.config.getOrNull(SemanticsProperties.TestTag) ?: continue
                val b = n.boundsInWindow
                if (b.width > 0f && b.height > 0f) out += tag
            }
        }
        out.toList()
    }

    /** Hears what a finger could not have done (a touch on a node another window covers), once per tag. */
    @Volatile
    var note: (String) -> Unit = {}
    private val noted = HashSet<String>()

    /** True when a window above [view]'s covers the point ([x], [y] in [view]'s window): a finger would meet that one. */
    private fun covered(view: View, x: Float, y: Float): Boolean {
        val all = windows().filter { it.isAttachedToWindow && it.isShown && it.width > 0 && it.height > 0 }
        val at = all.indexOf(view)
        if (at < 0) return false
        val origin = IntArray(2).also { view.getLocationOnScreen(it) }
        val sx = origin[0] + x
        val sy = origin[1] + y
        return all.drop(at + 1).any { w ->
            val o = IntArray(2).also { w.getLocationOnScreen(it) }
            sx >= o[0] && sx < o[0] + w.width && sy >= o[1] && sy < o[1] + w.height
        }
    }

    override fun idle(ms: Long) {
        if (System.currentTimeMillis() > deadlineMs) throw EtapeTropLongue("l'étape a dépassé son temps")
        main.pause(ms)
    }

    /** Each list that holds [tag], from the nearest out, scrolled by what it lacks to show it whole. */
    override fun scrollTo(tag: String) {
        val moved = main.run {
            val node = one(tag).node
            // Where it is, whole: its bounds are clipped (zero) while the list hides it.
            var target = Rect(node.positionInRoot, Size(node.size.width.toFloat(), node.size.height.toFloat()))
            var parent = node.parent
            var any = false
            while (parent != null) {
                val by = parent.config.getOrNull(SemanticsActions.ScrollBy)
                if (by != null) {
                    val view = parent.boundsInRoot
                    val dx = delta(target.left, target.right, view.left, view.right)
                    val dy = delta(target.top, target.bottom, view.top, view.bottom)
                    if (dx != 0f || dy != 0f) {
                        val rx = parent.config.getOrNull(SemanticsProperties.HorizontalScrollAxisRange)?.reverseScrolling == true
                        val ry = parent.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)?.reverseScrolling == true
                        by.action?.invoke(if (rx) -dx else dx, if (ry) -dy else dy)
                        any = true
                    }
                    target = target.translate(-dx, -dy)
                }
                parent = parent.parent
            }
            any
        }
        if (moved) settle(tag)
    }

    /**
     * Waits (two seconds at most) until [tag] stops moving: a list scrolled by its semantics glides
     * there (an animation, on the phone in real time). Still for [STILL_READS] frames in a row, not
     * two: the glide's first frames move nothing, and a touch while a list still moves only stops it
     * (the phone's « Plus tard » and style cards, 2026-11-12).
     */
    private fun settle(tag: String) {
        var last: Rect? = null
        var still = 0
        repeat(120) {
            main.pause(FRAME_MS)
            val now = runCatching { bounds(tag) }.getOrNull()
            if (now != null && now == last) {
                if (++still >= STILL_READS) return
            } else {
                still = 0
            }
            last = now
        }
    }

    override fun click(tag: String) {
        val (view, x, y) = main.run {
            val f = one(tag)
            val c = f.node.boundsInWindow.center
            val root = f.root.view.rootView
            // A finger cannot reach what another window covers: no touch, the step fails (2026-11-12: five such
            // touches were taken as done while the assistant covered the studio).
            if (covered(root, c.x, c.y)) {
                val why = "touché sous une autre fenêtre (un doigt ne l'atteindrait pas) : $tag"
                if (noted.add(tag)) runCatching { note(why) }
                throw IllegalStateException(why)
            }
            Triple(root, c.x, c.y)
        }
        val down = SystemClock.uptimeMillis()
        main.run { touch(view, down, down, MotionEvent.ACTION_DOWN, x, y) }
        main.pause(PRESS_MS)
        main.run { touch(view, down, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y) }
        main.pause(FRAME_MS)
    }

    private fun touch(view: View, downTime: Long, time: Long, action: Int, x: Float, y: Float) {
        val e = MotionEvent.obtain(downTime, time, action, x, y, 0)
        e.source = InputDevice.SOURCE_TOUCHSCREEN
        try {
            view.dispatchTouchEvent(e)
        } finally {
            e.recycle()
        }
    }

    companion object {
        /** A finger's press, well under a long press (400 ms and more). */
        const val PRESS_MS = 60L
        const val FRAME_MS = 17L

        /** Frames in a row at the same place before a node counts as still. */
        const val STILL_READS = 6

        /** How far a list must move for [lo]..[hi] to sit inside [viewLo]..[viewHi] (its start first when it is bigger). */
        fun delta(lo: Float, hi: Float, viewLo: Float, viewHi: Float): Float = when {
            lo < viewLo -> lo - viewLo
            hi > viewHi -> minOf(hi - viewHi, lo - viewLo)
            else -> 0f
        }
    }
}
