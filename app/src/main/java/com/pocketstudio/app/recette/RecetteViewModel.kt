package com.pocketstudio.app.recette

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketstudio.app.Metrics
import com.pocketstudio.app.report.IssueReport
import com.pocketstudio.app.report.LogTail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RecetteUiState(
    val checklist: Checklist = Checklist("", "", emptyList()),
    val results: Map<String, CheckResult> = emptyMap(),
    val changelog: String = "",
)

class RecetteViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = RecetteRepository(app)
    private val _state = MutableStateFlow(RecetteUiState())
    val state: StateFlow<RecetteUiState> = _state.asStateFlow()

    init {
        val checklist = repository.load()
        _state.value = RecetteUiState(checklist, repository.results(checklist), repository.changelog())
    }

    fun update(itemId: String, result: CheckResult) {
        val checklist = _state.value.checklist
        repository.save(checklist, itemId, result)
        _state.update { it.copy(results = it.results + (itemId to result)) }
    }

    fun buildReport(onReady: (Uri) -> Unit) = viewModelScope.launch {
        val s = _state.value
        val uri = withContext(Dispatchers.IO) {
            IssueReport.recetteIssue(
                s.checklist.milestone,
                ChecklistParser.reportLines(s.checklist, s.results),
                IssueReport.currentContext(Metrics.streamInfo.value, Metrics.roundTripMs.value, LogTail.read()),
            )
        }
        onReady(uri)
    }

    fun buildProblem(description: String, onReady: (Uri) -> Unit) = viewModelScope.launch {
        val uri = withContext(Dispatchers.IO) {
            IssueReport.problemIssue(
                description,
                IssueReport.currentContext(Metrics.streamInfo.value, Metrics.roundTripMs.value, LogTail.read()),
            )
        }
        onReady(uri)
    }
}
