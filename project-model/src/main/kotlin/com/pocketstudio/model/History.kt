package com.pocketstudio.model

/**
 * Unlimited undo/redo over immutable values.
 *
 * Consecutive edits with the same [coalesceKey] made within [coalesceWindowMs] merge into one
 * step, so dragging a slider is undone in one go instead of a hundred.
 */
class History<T>(initial: T, private val coalesceWindowMs: Long = 600) {
    var current: T = initial
        private set

    // Each entry: the value on the other side of a step, and that step's tag (see [push]).
    private val undoStack = ArrayDeque<Pair<T, Any?>>()
    private val redoStack = ArrayDeque<Pair<T, Any?>>()
    private var lastKey: String? = null

    /** The tag of the step the last [undo] or [redo] went through (null: none, or untagged). */
    var lastTag: Any? = null
        private set
    private var lastTime = Long.MIN_VALUE

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val undoDepth: Int get() = undoStack.size

    /**
     * Records [next] as the new value. Returns false (and records nothing) if nothing changed.
     * [tag]: what the step was, handed back as [lastTag] when it is undone or redone (a merged
     * step keeps its first tag).
     */
    fun push(next: T, coalesceKey: String? = null, nowMs: Long = System.currentTimeMillis(), tag: Any? = null): Boolean {
        if (next == current) return false
        val merge = coalesceKey != null && coalesceKey == lastKey && nowMs - lastTime <= coalesceWindowMs && canUndo
        if (!merge) undoStack.addLast(current to tag)
        current = next
        redoStack.clear()
        lastKey = coalesceKey
        lastTime = nowMs
        return true
    }

    /**
     * Changes the current value without an undo step (e.g. following the playing scene). The redo
     * steps stay: following the song only changes the scene shown, and a live take (the other
     * quiet change) clears them when it is committed ([commitFrom]).
     */
    fun replace(next: T) {
        current = next
        lastKey = null
    }

    /**
     * After quiet changes ([replace]) made since [before] (a live recording), makes the whole
     * change one undo step back to [before]. [depth]: [undoDepth] when the take started; the
     * steps recorded since (an edit made during the take) are folded into it, or undoing them
     * later would bring back half of the take.
     */
    fun commitFrom(before: T, depth: Int = undoStack.size) {
        if (current == before) return
        while (undoStack.size > depth.coerceAtLeast(0)) undoStack.removeLast()
        undoStack.addLast(before to null)
        redoStack.clear()
        lastKey = null
    }

    fun undo(): T? {
        val (previous, tag) = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(current to tag)
        current = previous
        lastKey = null
        lastTag = tag
        return current
    }

    fun redo(): T? {
        val (next, tag) = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(current to tag)
        current = next
        lastKey = null
        lastTag = tag
        return current
    }
}
