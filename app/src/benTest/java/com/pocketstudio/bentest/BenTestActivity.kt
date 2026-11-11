package com.pocketstudio.bentest

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.PocketStudioTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * BenStudio Test's own screen (docs/PLAN-TEST-APK.md): the preparation (the microphone and the
 * notifications, two touches), « Lancer l'essai éclair », the run's progress, its question, and
 * « Envoyer le rapport ». The real studio opens over it while the robot works.
 */
class BenTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Robot.look(this)
        setContent {
            PocketStudioTheme {
                val etat by Robot.etat.collectAsStateWithLifecycle()
                var refresh by remember { mutableIntStateOf(0) }
                val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
                val granted = remember(refresh) { PERMISSIONS.associate { (p, _) -> p to (ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED) } }
                LaunchedEffect(Unit) { Robot.look(this@BenTestActivity) }
                BenTestScreen(
                    etat = etat,
                    granted = PERMISSIONS.map { (p, label) -> Triple(p, label, granted[p] == true) },
                    actions = BenTestActions(
                        allow = { p -> ask.launch(p) },
                        start = { Robot.start(application) },
                        answer = Robot::answer,
                        reset = { Robot.reset(this) },
                        send = ::send,
                    ),
                )
            }
        }
    }

    private fun send() {
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) { runCatching { Robot.share(this@BenTestActivity) }.getOrDefault(false) }
            if (!ok) android.widget.Toast.makeText(this@BenTestActivity, "Le rapport n'a pas pu être préparé.", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        /** What the robot cannot accept by itself: the microphone (it listens to the phone, later steps) and the notifications. */
        val PERMISSIONS = listOf(Manifest.permission.RECORD_AUDIO to "Autoriser le micro", Manifest.permission.POST_NOTIFICATIONS to "Autoriser les notifications")
    }
}

class BenTestActions(
    val allow: (String) -> Unit = {},
    val start: () -> Unit = {},
    val answer: (Boolean) -> Unit = {},
    val reset: () -> Unit = {},
    val send: () -> Unit = {},
)

/** The screen, from [etat] alone ([granted]: each permission, its button's words, given or not). */
@Composable
fun BenTestScreen(etat: Robot.Etat, granted: List<Triple<String, String, Boolean>>, actions: BenTestActions) {
    Column(
        Modifier.fillMaxSize().background(Neon.Background).safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("BenStudio Test", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Neon.TextPrimary)
        Text("Le robot teste l'appli tout seul. Tes projets de la vraie appli ne risquent rien.", color = Neon.TextSecondary)
        val question = etat.question
        when {
            question != null -> {
                Text(question, style = MaterialTheme.typography.titleLarge, color = Neon.TextPrimary, modifier = Modifier.testTag("bentest_question"))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { actions.answer(true) }, modifier = Modifier.heightIn(min = 56.dp).weight(1f).testTag("bentest_oui")) { Text("✅ Oui") }
                    Button(onClick = { actions.answer(false) }, modifier = Modifier.heightIn(min = 56.dp).weight(1f).testTag("bentest_non")) { Text("❌ Non") }
                }
            }
            etat.running -> {
                Text("Le robot travaille… Pose le téléphone, il revient ici tout seul.", color = Neon.TextPrimary)
                etat.step?.let { Text("Étape : $it", color = Neon.Cyan, modifier = Modifier.testTag("bentest_step")) }
            }
            else -> {
                Text("Préparation", style = MaterialTheme.typography.titleMedium, color = Neon.TextPrimary)
                granted.forEach { (permission, label, ok) ->
                    if (ok) Text("✓ ${label.removePrefix("Autoriser ")} : autorisé", color = Neon.Acid, modifier = Modifier.testTag("bentest_ok_$permission"))
                    else OutlinedButton(onClick = { actions.allow(permission) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("bentest_allow_$permission")) { Text(label) }
                }
                etat.interrompu?.let {
                    Text("Le passage s'est arrêté pendant « $it ». Le plantage sera noté et le robot passera à la suite.", color = Neon.Amber, modifier = Modifier.testTag("bentest_interrompu"))
                    Button(onClick = actions.start, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("bentest_reprendre")) { Text("Reprendre") }
                }
                etat.verdict?.let {
                    Text("Essai éclair terminé : $it", style = MaterialTheme.typography.titleMedium, color = Neon.TextPrimary, modifier = Modifier.testTag("bentest_verdict"))
                    Button(onClick = actions.send, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("bentest_envoyer")) { Text("Envoyer le rapport") }
                }
                if (etat.interrompu == null && etat.verdict == null) {
                    Text("Essai éclair : environ 5 minutes. Le robot fait 3 morceaux, 1 export, visite 5 écrans, puis te pose 1 question.", color = Neon.TextSecondary)
                    Button(onClick = actions.start, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("bentest_lancer")) { Text("Lancer l'essai éclair") }
                }
                if (etat.interrompu != null || etat.verdict != null) {
                    OutlinedButton(onClick = actions.reset, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("bentest_recommencer")) { Text("Tout recommencer") }
                }
                etat.message?.let { Text(it, color = Neon.TextSecondary) }
            }
        }
    }
}
