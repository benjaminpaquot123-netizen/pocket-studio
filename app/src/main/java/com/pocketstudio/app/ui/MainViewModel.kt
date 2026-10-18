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

    init {
        viewModelScope.launch {
            UpdateChecker.check()?.let { _update.value = UpdateState.Available(it) }
        }
    }

    fun crashReportUri(): Uri? = _crash.value?.let { crash ->
        IssueReport.crashIssue(crash, IssueReport.currentContext(Metrics.streamInfo.value, Metrics.roundTripMs.value, ""))
    }

    fun dismissCrash() {
        CrashReporter.clear()
        _crash.value = null
    }

    fun dismissUpdate() {
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
        viewModelScope.launch {
            _update.value = UpdateState.Downloading(info, 0f)
            runCatching {
                UpdateInstaller.downloadAndInstall(app, info) { p -> _update.value = UpdateState.Downloading(info, p) }
            }.onSuccess {
                _update.value = UpdateState.None
            }.onFailure {
                _update.value = UpdateState.Failed(info)
            }
        }
    }
}
