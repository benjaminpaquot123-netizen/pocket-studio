package com.pocketstudio.app.playback

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel
import com.pocketstudio.model.i18n.tr

data class BackgroundState(val notifications: Boolean, val batteryFree: Boolean, val honor: Boolean)

fun backgroundState(context: Context) = BackgroundState(
    notifications = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED,
    batteryFree = context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName),
    honor = Build.MANUFACTURER.equals("HONOR", ignoreCase = true) || Build.MANUFACTURER.equals("HUAWEI", ignoreCase = true),
)

/** Guides the three settings that let the music keep playing with the screen off. */
@Composable
fun BackgroundPage(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(backgroundState(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { state = backgroundState(context) }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        state = backgroundState(context)
    }
    BackgroundScreen(
        state = state,
        onNotifications = { notifications.launch(Manifest.permission.POST_NOTIFICATIONS) },
        onBattery = { requestBatteryExemption(context) },
        onAppSettings = { openAppSettings(context) },
        modifier = modifier,
    )
}

@Composable
fun BackgroundScreen(
    state: BackgroundState,
    onNotifications: () -> Unit,
    onBattery: () -> Unit,
    onAppSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // A page to read: centred at a comfortable width on a tablet.
    com.pocketstudio.ui.ReadingWidth {
        Column(
            modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp).testTag("background"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                tr("Pour que la musique continue écran éteint ou dans une autre appli :"),
                style = MaterialTheme.typography.bodyMedium,
            )
            Step(1, tr("Notification de lecture"), tr("Elle montre que le studio joue, avec un bouton Arrêter."), state.notifications, tr("Autoriser"), onNotifications)
            Step(2, tr("Batterie sans restriction"), tr("Android ne coupe plus le son pour économiser la batterie."), state.batteryFree, tr("Autoriser"), onBattery)
            if (state.honor) {
                NeonCard(accent = Neon.Amber) {
                    SectionLabel(tr("3. Réglage Honor"), color = Neon.Amber)
                    Text(
                        tr("Paramètres › Batterie › Lancement d'applications › Pocket Studio : désactive « Gérer automatiquement », puis laisse les trois interrupteurs allumés."),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedButton(onClick = onAppSettings, modifier = Modifier.fillMaxWidth()) { Text(tr("Ouvrir les réglages de l'appli")) }
                }
            }
        }
    }
}

@Composable
private fun Step(number: Int, title: String, text: String, done: Boolean, action: String, onClick: () -> Unit) {
    NeonCard(accent = if (done) Neon.Acid else Neon.Cyan) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("$number. $title", Modifier.weight(1f), color = if (done) Neon.Acid else Neon.Cyan)
            Text(if (done) tr("✓ fait") else tr("à faire"), color
 = if (done) Neon.Acid else Neon.TextSecondary)
        }
        Text(text, style = MaterialTheme.typography.bodyMedium)
        if (!done) Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(action) }
    }
}

@SuppressLint("BatteryLife")  // Personal studio app: the exemption is the point of this page.
private fun requestBatteryExemption(context: Context) {
    val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
    runCatching { context.startActivity(direct) }
        .onFailure { runCatching { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) } }
}

private fun openAppSettings(context: Context) {
    runCatching {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
    }
}
