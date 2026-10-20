package com.pocketstudio.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketstudio.app.Metrics
import com.pocketstudio.app.report.CrashReporter
import com.pocketstudio.app.report.IssueReport
import com.pocketstudio.app.update.UpdateChecker
import com.pocketstudio.app.update.UpdateInfo
import com.pocketstudio.app.update.UpdateInstaller
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UpdateState {
    data object None : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class Downloading(val info: UpdateInfo, val progress: Float) : UpdateState
    data class Failed(val info: UpdateInfo) : UpdateState
}

/** App-wide prompts shown at launch: previous crash report and available update. */
class MainViewModel(private val app: Application) : AndroidViewModel(app) {
    private val _crash = MutableStateFlow(CrashReporter.pendingCrash())
    val crash: StateFlow<String?> = _crash.asStateFlow()

    private val _update = MutableStateFlow<UpdateState>(UpdateState.None)
    val update: StateFlow<UpdateState> = _update.asStateFlow()

    private val prefs = app.getSharedPreferences("update", android.content.Context.MODE_PRIVATE)
    private var download: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            // "Plus tard" holds for that version: opening a project (which rebuilds this) does not ask again.
            UpdateChecker.check()?.takeIf { it.versionCode != prefs.getInt(DISMISSED, -1) }?.let { _update.value = UpdateState.Available(it) }
        }
    }

    fun crashReportUri(): Uri? = _crash.value?.let { crash ->
        IssueReport.crashIssue(crash, IssueReport.currentContext(Metrics.streamInfo.value, Metrics.roundTripMs.value, ""))
    }

    fun dismissCrash() {
        CrashReporter.clear()
        _crash.value = null
    }

    /** Closes the update offer; only an explicit « Plus tard » ([remember]) skips that version for good. */
    fun dismissUpdate(remember: Boolean = true) {
        val s = _update.value
        if (remember && s is UpdateState.Available) prefs.edit().putInt(DISMISSED, s.info.versionCode).apply()
        _update.value = UpdateState.None
    }

    /** Stops a download in progress (a network that went away, a change of mind). */
    fun cancelUpdate() {
        download?.cancel()
        download = null
        _update.value = UpdateState.None
    }

    fun installUpdate() {
        val info = when (val s = _update.value) {
            is UpdateState.Available -> s.info
            is UpdateState.Failed -> s.info
            else -> return
        }
        if (!UpdateInstaller.canInstall(app)) {
            UpdateInstaller.openInstallPermissionSettings(app)
            return
        }
        download = viewModelScope.launch {
            _update.value = UpdateState.Downloading(info, 0f)
            runCatching {
                UpdateInstaller.downloadAndInstall(app, info) { p -> _update.value = UpdateState.Downloading(info, p) }
            }.onSuccess {
                _update.value = UpdateState.None
            }.onFailure { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                _update.value = UpdateState.Failed(info)
            }
        }
    }

    private companion object {
        const val DISMISSED = "dismissedVersion"
    }
}
