package com.pocketstudio.app.latency

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketstudio.app.report.IssueReport
import com.pocketstudio.engine.StreamInfo
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel
import com.pocketstudio.ui.StatRow

private const val TARGET_MS = 20.0

@Composable
fun LatencyScreen(
    state: LatencyUiState,
    stream: StreamInfo,
    lastRoundTripMs: Double?,
    engineAvailable: Boolean,
    onMeasure: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).testTag("latency"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Latence", style = MaterialTheme.typography.headlineMedium, color = Neon.TextPrimary)
        Text("Le temps entre ton doigt et tes oreilles. Objectif : moins de 20 ms.", color = Neon.TextSecondary)

        NeonCard(accent = Neon.Cyan) {
            SectionLabel("Aller-retour mesuré")
            // A failed run shows no number: the previous success is mentioned below instead.
            val shown = when (state) {
                is LatencyUiState.Measured -> state.roundTripMs
                is LatencyUiState.Failed -> null
                else -> lastRoundTripMs
            }
            val color = when {
                shown == null -> Neon.TextSecondary
                shown < TARGET_MS -> Neon.Acid
                shown < 30 -> Neon.Amber
                else -> Neon.Red
            }
            Text(
                shown?.let { IssueReport.formatMs(it) } ?: "– ms",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            when (state) {
                is LatencyUiState.Running -> {
                    Text("Essai ${state.run}… chut !", color = Neon.Cyan)
                    LinearProgressIndicator(
                        progress = { (state.run - 1f) / state.total },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                is LatencyUiState.Measured -> Text(
                    "Mesures : " + state.runs.joinToString(" · ") { IssueReport.formatMs(it) },
                    color = Neon.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                is LatencyUiState.Failed -> {
                    Text(state.message, color = Neon.Red)
                    lastRoundTripMs?.let {
                        Text("Dernière mesure réussie : ${IssueReport.formatMs(it)}", color = Neon.TextSecondary)
                    }
                    Text(
                        "Détails techniques (joints au rapport) : ${state.diagnostic}",
                        color = Neon.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                LatencyUiState.Idle -> Text(
                    "Le téléphone émet un petit « pschht » et l'écoute avec son micro.",
                    color = Neon.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(
                onClick = onMeasure,
                enabled = engineAvailable && state !is LatencyUiState.Running,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("measure"),
            ) {
                Text(if (shown == null) "Mesurer la latence" else "Mesurer à nouveau")
            }
        }

        NeonCard {
            SectionLabel("Sortie seule (estimation du système)")
            StatRow("Latence de sortie", IssueReport.formatMs(stream.outputLatencyMs))
            StatRow("Mode audio", IssueReport.describeStream(stream))
        }

        NeonCard(accent = Neon.Amber) {
            SectionLabel("Pour une mesure fiable", color = Neon.Amber)
            Text("• Retire casque et écouteurs Bluetooth.", style = MaterialTheme.typography.bodyMedium)
            Text("• Volume média au maximum, pièce calme.", style = MaterialTheme.typography.bodyMedium)
            Text("• Pour jouer en direct, préfère un casque filaire USB-C : le Bluetooth ajoute 100 à 200 ms.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
