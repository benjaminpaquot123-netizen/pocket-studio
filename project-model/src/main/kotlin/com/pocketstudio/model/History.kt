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

    private val undoStack = ArrayDeque<T>()
    private val redoStack = ArrayDeque<T>()
    private var lastKey: String? = null
    private var lastTime = Long.MIN_VALUE

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val undoDepth: Int get() = undoStack.size

    /** Records [next] as the new value. Returns false (and records nothing) if nothing changed. */
    fun push(next: T, coalesceKey: String? = null, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (next == current) return false
        val merge = coalesceKey != null && coalesceKey == lastKey && nowMs - lastTime <= coalesceWindowMs && canUndo
        if (!merge) undoStack.addLast(current)
        current = next
        redoStack.clear()
        lastKey = coalesceKey
        lastTime = nowMs
        return true
    }

    /** Changes the current value without an undo step (e.g. following the playing scene). */
    fun replace(next: T) {
        current = next
        lastKey = null
    }

    /**
     * After quiet changes ([replace]) made since [before] (a live recording), makes the whole
     * change one undo step back to [before].
     */
    fun commitFrom(before: T) {
        if (current == before) return
        undoStack.addLast(before)
        redoStack.clear()
        lastKey = null
    }

    fun undo(): T? {
        val previous = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(current)
        current = previous
        lastKey = null
        return current
    }

    fun redo(): T? {
        val next = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(current)
        current = next
        lastKey = null
        return current
    }
}
