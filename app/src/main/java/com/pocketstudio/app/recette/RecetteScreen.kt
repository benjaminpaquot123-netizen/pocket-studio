package com.pocketstudio.app.recette

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.report.IssueReport
import com.pocketstudio.engine.StreamInfo
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel
import com.pocketstudio.ui.StatRow

private const val TARGET_ROUND_TRIP_MS = 20.0
private const val TARGET_BUFFER_FRAMES = 256

@Composable
fun RecetteScreen(
    state: RecetteUiState,
    stream: StreamInfo,
    roundTripMs: Double?,
    onUpdate: (String, CheckResult) -> Unit,
    onSendReport: () -> Unit,
    onReportProblem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showProblemDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("recette"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Recette", style = MaterialTheme.typography.headlineMedium, color = Neon.TextPrimary)
            Text(state.checklist.milestone, color = Neon.Magenta, style = MaterialTheme.typography.titleMedium)
        }
        if (state.changelog.isNotBlank()) {
            item {
                NeonCard(accent = Neon.Magenta) {
                    SectionLabel("Nouveautés", color = Neon.Magenta)
                    Text(state.changelog, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item { AutoMeasures(stream, roundTripMs) }
        items(state.checklist.items, key = { it.id }) { item ->
            CheckCard(item, state.results[item.id] ?: CheckResult(), onUpdate)
        }
        item {
            val done = state.results.values.count { it.status != CheckStatus.Untested }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "$done / ${state.checklist.items.size} vérifications faites",
                    color = Neon.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(onClick = onSendReport, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("Envoyer le rapport de recette")
                }
                OutlinedButton(onClick = { showProblemDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Signaler un problème")
                }
            }
        }
    }

    if (showProblemDialog) {
        ProblemDialog(
            onDismiss = { showProblemDialog = false },
            onSend = {
                showProblemDialog = false
                onReportProblem(it)
            },
        )
    }
}

@Composable
private fun AutoMeasures(stream: StreamInfo, roundTripMs: Double?) {
    NeonCard {
        SectionLabel("Mesures automatiques")
        val rtOk = roundTripMs != null && roundTripMs < TARGET_ROUND_TRIP_MS
        StatRow(
            "Latence aller-retour (< 20 ms)",
            roundTripMs?.let { IssueReport.formatMs(it) } ?: "à mesurer",
            if (roundTripMs == null) Neon.TextSecondary else if (rtOk) Neon.Acid else Neon.Amber,
        )
        StatRow("Décrochages audio (0)", stream.xruns.toString(), if (stream.xruns == 0) Neon.Acid else Neon.Red)
        val bufferOk = stream.bufferSize in 1..TARGET_BUFFER_FRAMES
        StatRow(
            "Buffer (≤ 256 échantillons)",
            if (stream.bufferSize > 0) stream.bufferSize.toString() else "flux fermé",
            if (stream.bufferSize == 0) Neon.TextSecondary else if (bufferOk) Neon.Acid else Neon.Amber,
        )
    }
}

@Composable
private fun CheckCard(item: CheckItem, result: CheckResult, onUpdate: (String, CheckResult) -> Unit) {
    val accent = when (result.status) {
        CheckStatus.Pass -> Neon.Acid
        CheckStatus.Fail -> Neon.Red
        CheckStatus.Untested -> Neon.Outline
    }
    NeonCard(accent = accent) {
        Text(item.title, style = MaterialTheme.typography.titleMedium)
        if (item.hint.isNotBlank()) {
            Text(item.hint, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip("✅ Ça marche", result.status == CheckStatus.Pass, Neon.Acid) {
                onUpdate(item.id, result.copy(status = toggle(result.status, CheckStatus.Pass)))
            }
            StatusChip("❌ Problème", result.status == CheckStatus.Fail, Neon.Red) {
                onUpdate(item.id, result.copy(status = toggle(result.status, CheckStatus.Fail)))
            }
        }
        if (result.status != CheckStatus.Untested || result.comment.isNotEmpty()) {
            OutlinedTextField(
                value = result.comment,
                onValueChange = { onUpdate(item.id, result.copy(comment = it)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Commentaire (facultatif)") },
            )
        }
    }
}

private fun toggle(current: CheckStatus, wanted: CheckStatus) =
    if (current == wanted) CheckStatus.Untested else wanted

@Composable
private fun StatusChip(label: String, selected: Boolean, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color.copy(alpha = 0.2f),
            selectedLabelColor = color,
        ),
    )
}

@Composable
private fun ProblemDialog(onDismiss: () -> Unit, onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Signaler un problème") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                placeholder = { Text("Qu'est-ce qui cloche ? (une phrase suffit)") },
            )
        },
        confirmButton = { TextButton(onClick = { onSend(text) }) { Text("Préparer le rapport") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
