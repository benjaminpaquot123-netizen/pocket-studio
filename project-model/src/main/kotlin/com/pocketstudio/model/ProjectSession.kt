package com.pocketstudio.model

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The open project: edits, undo/redo and autosave.
 *
 * Every edit schedules a save [autosaveDelayMs] later (successive edits push it back), so a
 * slider drag writes once, not sixty times a second; but never more than [maxSaveDelayMs] after
 * the first change still waiting, so minutes of non-stop recording (pads, keys) are on disk as
 * they go. [flush] forces the pending save, e.g. when the app goes to the background.
 */
class ProjectSession(
    val id: String,
    initial: Project,
    private val store: ProjectStore,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher,
    private val autosaveDelayMs: Long = 400,
    private val maxSaveDelayMs: Long = 2500,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val history = History(initial.sanitized())
    private val _project = MutableStateFlow(history.current)
    val project: StateFlow<Project> = _project.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _lastSaveError = MutableStateFlow<Throwable?>(null)
    val lastSaveError: StateFlow<Throwable?> = _lastSaveError.asStateFlow()

    private val saveLock = Mutex()
    private var pendingSave: Job? = null
    private var dirty = false
        set(value) {
            field = value
            _unsaved.value = value
        }
    /** [clock] when the oldest change not yet on disk was made (see [maxSaveDelayMs]). */
    private var dirtySince = 0L

    private val _unsaved = MutableStateFlow(false)
    /** True while changes wait to be written (the autosave delay, or a save that failed). */
    val unsaved: StateFlow<Boolean> = _unsaved.asStateFlow()

    /**
     * Everything is on disk: nothing waits ([unsaved]) and no save is being written (during one,
     * its changes are no longer [unsaved] but not on disk yet: a screen closing then still
     * [flush]es, which waits for it).
     */
    val settled: Boolean get() = !_unsaved.value && !saveLock.isLocked

    /**
     * Applies [transform] to the project. Same [coalesceKey] in a quick burst = one undo step.
     * [tag]: what the step was, returned by [undo] and [redo] when they go through it.
     */
    fun edit(coalesceKey: String? = null, tag: Any? = null, transform: (Project) -> Project) {
        val next = apply(transform)
        if (history.push(next, coalesceKey, clock(), tag)) publish()
    }

    /** Like [edit] but not undoable: for changes the app makes on its own, like following the song. */
    fun editQuietly(transform: (Project) -> Project) {
        val next = apply(transform)
        if (next == history.current) return
        history.replace(next)
        publish()
    }

    /** How many undo steps there are (to note when a live recording starts). */
    val undoDepth: Int get() = history.undoDepth

    /**
     * Turns the quiet edits made since [before] into one undo step (end of a live recording);
     * [depth]: [undoDepth] when it started, so edits made during it fold into the same step.
     */
    fun commitQuietEdits(before: Project, depth: Int = history.undoDepth) {
        history.commitFrom(before.sanitized(), depth)
        publish()
    }

    /**
     * [transform] on the current project, cleaned; a scene that now lasts longer or shorter (or
     * another time signature) moves what the song places after it (see [followPartLengths]).
     */
    private fun apply(transform: (Project) -> Project): Project {
        val before = history.current
        val next = clean(transform(before))
        val followed = next.followPartLengths(before)
        return if (followed === next) next else clean(followed)
    }

    /**
     * [p] sanitized; when that changes nothing, [p] itself, which shares all it did not change with
     * the previous step (sanitized() rebuilds everything: each undo step would hold a whole copy).
     */
    private fun clean(p: Project): Project {
        val s = p.sanitized()
        return if (s == p) p else s
    }

    /** Goes one step back; returns that step's tag (see [edit]), null when untagged or none. */
    fun undo(): Any? {
        if (history.undo() == null) return null
        publish()
        return history.lastTag
    }

    /** Goes one step forward again; returns that step's tag, null when untagged or none. */
    fun redo(): Any? {
        if (history.redo() == null) return null
        publish()
        return history.lastTag
    }

    /** Writes any pending change now; false when the save failed (the change is still waiting). */
    suspend fun flush(): Boolean {
        pendingSave?.cancel()
        return saveNow()
    }

    private fun publish() {
        _project.value = history.current
        _canUndo.value = history.canUndo
        _canRedo.value = history.canRedo
        val now = clock()
        if (!dirty) dirtySince = now
        dirty = true
        // Pushed back by each edit, but not past the deadline of the oldest unsaved change.
        val wait = autosaveDelayMs.coerceAtMost((dirtySince + maxSaveDelayMs - now).coerceAtLeast(0))
        pendingSave?.cancel()
        pendingSave = scope.launch {
            delay(wait)
            // Only the wait is pushed back: a save under way finishes (the next edit cancelling it
            // each time would keep a non-stop recording off the disk).
            withContext(NonCancellable) { saveNow() }
        }
    }

    /** True when everything up to now is on disk. */
    private suspend fun saveNow(): Boolean = saveLock.withLock {
        if (!dirty) return@withLock true
        val snapshot = history.current
        dirty = false
        try {
            withContext(io) { store.save(id, snapshot) }
            _lastSaveError.value = null
            true
        } catch (e: CancellationException) {
            dirty = true  // superseded by a newer save (flush): not an error
            throw e
        } catch (e: Exception) {
            dirty = true
            dirtySince = clock()  // a full phone: try again later, not at every gesture
            _lastSaveError.value = e
            false
        }
    }
}
