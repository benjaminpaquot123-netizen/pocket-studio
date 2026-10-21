package com.pocketstudio.app.latency

import com.pocketstudio.model.i18n.tr
import com.pocketstudio.app.help.Glossary
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
    // A page to read: centred at a comfortable width on a tablet.
    com.pocketstudio.ui.ReadingWidth {
        Column(
            modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).testTag("latency"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(tr("Latence"), style = MaterialTheme.typography.headlineMedium, color = Neon.TextPrimary)
            Text(tr("Le temps entre ton doigt et tes oreilles. Objectif : moins de 20 ms."), color = Neon.TextSecondary)

            NeonCard(accent = Neon.Cyan) {
                SectionLabel(tr("Aller-retour mesuré"), help = Glossary.LATENCY)
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
                    shown?.let { IssueReport.formatMs(it, onScreen = true) } ?: "– ms",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
                when (state) {
                    is LatencyUiState.Running -> {
                        Text(tr("Essai %d… chut !", state.run), color = Neon.Cyan)
                        LinearProgressIndicator(
                            progress = { (state.run - 1f) / state.total },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    is LatencyUiState.Measured -> Text(
                        tr("Mesures : %s", state.runs.joinToString(" · ") { IssueReport.formatMs(it, onScreen = true) }),
                        color = Neon.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    is LatencyUiState.Failed -> {
                        Text(state.message, color = Neon.Red)
                        lastRoundTripMs?.let {
                            Text(tr("Dernière mesure réussie : %s", IssueReport.formatMs(it, onScreen = true)), color = Neon.TextSecondary)
                        }
                        Text(
                            tr("Détails techniques (joints au rapport) : %s", state.diagnostic),
                            color = Neon.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    LatencyUiState.Idle -> Text(
                        tr("Le téléphone émet un petit « pschht » et l'écoute avec son micro."),
                        color = Neon.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Button(
                    onClick = onMeasure,
                    enabled = engineAvailable && state !is LatencyUiState.Running,
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("measure"),
                ) {
                    Text(if (shown == null) tr("Mesurer la latence") else tr("Mesurer à nouveau"))
                }
            }

            NeonCard {
                SectionLabel(tr("Sortie seule (estimation du système)"))
                StatRow(tr("Latence de sortie"), IssueReport.formatMs(stream.outputLatencyMs, onScreen = true))
                StatRow(tr("Mode audio"), IssueReport.describeStream(stream, onScreen = true))
            }

            NeonCard(accent = Neon.Amber) {
                SectionLabel(tr("Pour une mesure fiable"), color = Neon.Amber)
                Text(tr("• Retire casque et écouteurs Bluetooth."), style = MaterialTheme.typography.bodyMedium)
                Text(tr("• Volume média au maximum, pièce calme."), style = MaterialTheme.typography.bodyMedium)
                Text(tr("• Pour jouer en direct, préfère un casque filaire USB-C : le Bluetooth ajoute 100 à 200 ms."), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
