package com.pocketstudio.model

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
 * slider drag writes once, not sixty times a second. [flush] forces the pending save, e.g.
 * when the app goes to the background.
 */
class ProjectSession(
    val id: String,
    initial: Project,
    private val store: ProjectStore,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher,
    private val autosaveDelayMs: Long = 400,
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

    /** Applies [transform] to the project. Same [coalesceKey] in a quick burst = one undo step. */
    fun edit(coalesceKey: String? = null, transform: (Project) -> Project) {
        val next = transform(history.current).sanitized()
        if (history.push(next, coalesceKey, clock())) publish()
    }

    fun undo() {
        if (history.undo() != null) publish()
    }

    fun redo() {
        if (history.redo() != null) publish()
    }

    /** Writes any pending change now. */
    suspend fun flush() {
        pendingSave?.cancel()
        saveNow()
    }

    private fun publish() {
        _project.value = history.current
        _canUndo.value = history.canUndo
        _canRedo.value = history.canRedo
        dirty = true
        pendingSave?.cancel()
        pendingSave = scope.launch {
            delay(autosaveDelayMs)
            saveNow()
        }
    }

    private suspend fun saveNow() = saveLock.withLock {
        if (!dirty) return@withLock
        val snapshot = history.current
        dirty = false
        try {
            withContext(io) { store.save(id, snapshot) }
            _lastSaveError.value = null
        } catch (e: Exception) {
            dirty = true
            _lastSaveError.value = e
        }
    }
}
