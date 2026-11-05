package com.pocketstudio.app.human

import android.graphics.Rect
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsNode

/**
 * The bench's windows as a phone places them (lot « tuto 5 »). Robolectric lays every window out
 * at its own (0, 0) and takes its touches there, while the screen the tester sees (Roborazzi's
 * screenshot) draws it where Android's window manager puts it: by its gravity and position in the
 * screen ([Gravity.apply], as Roborazzi does). A centred dialog (« Quitter sans créer le
 * morceau ? », the « Bienvenue » guide, « Je prépare ton morceau… ») was then drawn hundreds of
 * pixels lower than where it took touches: a touch on its button fell beside it.
 */
internal object BenchWindows {
    class Win(val view: View, val params: WindowManager.LayoutParams, val index: Int) {
        val location: IntArray get() = IntArray(2).also { view.getLocationOnScreen(it) }

        /** Where the screen shows it (sub-windows over the screen too: their parents fill it here). */
        fun drawn(screen: DisplayMetrics): Rect = Rect().also { out ->
            Gravity.apply(params.gravity, view.width, view.height, Rect(0, 0, screen.widthPixels, screen.heightPixels), params.x, params.y, out)
        }

        /** How far the screen shows it from where it takes touches. */
        fun shift(screen: DisplayMetrics): Pair<Float, Float> {
            val d = drawn(screen)
            val at = location
            return (d.left - at[0]).toFloat() to (d.top - at[1]).toFloat()
        }

        val touchable: Boolean get() = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE == 0 && view.visibility == View.VISIBLE
        /** Takes every touch, beside it too (a dialog): as Android, a window that cannot take focus is not modal. */
        val touchModal: Boolean get() = params.flags and (WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) == 0
    }

    /** The app's windows, the top one first (as Robolectric hands a touch: by type, then the newest). */
    fun windows(): List<Win> {
        val global = Class.forName("android.view.WindowManagerGlobal").getMethod("getInstance").invoke(null)
        val roots = field(global, "mRoots") as List<*>
        val params = field(global, "mParams") as List<*>
        return roots.indices.mapNotNull { i ->
            val view = roots[i]?.javaClass?.getMethod("getView")?.invoke(roots[i]) as? View ?: return@mapNotNull null
            Win(view, params[i] as WindowManager.LayoutParams, i)
        }.sortedWith(compareByDescending<Win> { it.params.type }.thenByDescending { it.index })
    }

    /**
     * Audit ultra round 3 (n° 16, Zone3DialogDiagTest): a window over the activity (a dialog) holding a text field. Under
     * Robolectric, Compose never comes back to rest while one is open (no line of the app needed): the bench waits there by
     * frames, a bounded wait. Read from the windows' own semantics, never through the test rule (which would wait for rest).
     */
    fun textFieldInADialog(): Boolean = runCatching {
        val dialogs = windows().filter { it.params.type != WindowManager.LayoutParams.TYPE_BASE_APPLICATION }
        val found = dialogs.firstOrNull { w ->
            // Audit ultra round 4 (seats C a, E a: « Renommer » › ✕ « Effacer », 5 min without answer): read in the middle of
            // a change (the ✕ just went with the text), a window's semantics may throw; the bench then took the window for
            // one without a field, waited for Compose's rest and never came back. The window that held a field still does.
            runCatching { composeRoots(w.view).any { root -> hasTextField(root.semanticsOwner.unmergedRootSemanticsNode) } }
                .getOrElse { w.view === lastFieldWindow?.get() }
        }
        lastFieldWindow = found?.view?.let { java.lang.ref.WeakReference(it) }
        found != null
    }.getOrDefault(lastFieldWindow?.get()?.isAttachedToWindow == true)

    /**
     * Audit method v2 (round 4, « Renommer » › ✕ « Effacer », 240 s; a test thread's stack proved it): a text field holding
     * the cursor, in any window. Its cursor blinks for ever, so Compose never rests under Robolectric (a phone has no such
     * wait); the bench reads such a screen without waiting for rest, as for a dialog's field.
     */
    fun focusedTextField(): Boolean = runCatching {
        windows().any { w ->
            runCatching { composeRoots(w.view).any { root -> hasFocusedField(root.semanticsOwner.unmergedRootSemanticsNode) } }.getOrDefault(false)
        }
    }.getOrDefault(false)

    private fun hasFocusedField(n: SemanticsNode): Boolean =
        (androidx.compose.ui.semantics.SemanticsActions.SetText in n.config &&
            n.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.Focused) { false }) || n.children.any(::hasFocusedField)

    /** A window over the activity (a dialog, a popup) is open. */
    fun dialogOpen(): Boolean = runCatching {
        windows().any { it.params.type != WindowManager.LayoutParams.TYPE_BASE_APPLICATION }
    }.getOrDefault(true)

    /** The dialog window last seen holding a text field (see [textFieldInADialog]). */
    private var lastFieldWindow: java.lang.ref.WeakReference<View>? = null

    private fun composeRoots(v: View): List<androidx.compose.ui.node.RootForTest> = buildList {
        if (v is androidx.compose.ui.node.RootForTest) add(v)
        if (v is android.view.ViewGroup) for (i in 0 until v.childCount) addAll(composeRoots(v.getChildAt(i)))
    }

    private fun hasTextField(n: SemanticsNode): Boolean =
        androidx.compose.ui.semantics.SemanticsActions.SetText in n.config || n.children.any(::hasTextField)

    /** The gap of the window a phone gives a touch at ([x], [y]) (the screen's pixels) to. */
    fun shiftAt(x: Float, y: Float, screen: DisplayMetrics): Pair<Float, Float> = runCatching {
        windows().filter { it.touchable }.firstOrNull { w -> w.touchModal || w.drawn(screen).contains(x.toInt(), y.toInt()) }?.shift(screen)
    }.getOrNull() ?: (0f to 0f)

    /** Where the screen shows [node]'s top left corner. */
    fun drawnPosition(node: SemanticsNode, screen: DisplayMetrics): Offset {
        val at = node.positionOnScreen
        val shift = runCatching {
            val layoutNode = field(node, "layoutNode")!!
            val owner = field(layoutNode, "owner") as View
            windows().firstOrNull { it.view === owner.rootView }?.shift(screen)
        }.getOrNull() ?: (0f to 0f)
        return Offset(at.x + shift.first, at.y + shift.second)
    }

    /** The window [node] is drawn in (null: not found). */
    fun windowOf(node: SemanticsNode): Win? = runCatching {
        val owner = field(field(node, "layoutNode")!!, "owner") as View
        windows().firstOrNull { it.view === owner.rootView }
    }.getOrNull()

    /** The part of [node] the screen shows (clipped by its parents), in the screen's pixels. */
    fun drawnBounds(node: SemanticsNode, screen: DisplayMetrics): androidx.compose.ui.geometry.Rect {
        val b = node.boundsInWindow
        val origin = windowOf(node)?.drawn(screen)
        return if (origin == null) b else b.translate(origin.left.toFloat(), origin.top.toFloat())
    }

    private fun field(of: Any, name: String): Any? {
        var c: Class<*>? = of.javaClass
        while (c != null) {
            runCatching { return c.getDeclaredField(name).apply { isAccessible = true }.get(of) }
            c = c.superclass
        }
        error("no field $name in ${of.javaClass.name}")
    }
}
