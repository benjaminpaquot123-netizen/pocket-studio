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
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
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
                // « Écoute au micro » waits for the microphone's answer (kept out of the screen: a turn does not lose it);
                // refused, its steps are « passées ».
                val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                    refresh++
                    if (EcouteMicro.reponseMicro()) Robot.start(application, Robot.MICRO)
                }
                val granted = remember(refresh) { PERMISSIONS.associate { (p, _) -> p to (ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED) } }
                LaunchedEffect(Unit) { Robot.look(this@BenTestActivity) }
                val candidats = remember(etat.running, etat.verdict) { Robot.candidats(this@BenTestActivity) }
                BenTestScreen(
                    etat = etat,
                    candidats = candidats,
                    granted = PERMISSIONS.map { (p, label) -> Triple(p, label, granted[p] == true) },
                    actions = BenTestActions(
                        allow = { p -> ask.launch(p) },
                        start = { Robot.start(application, Robot.ECLAIR) },
                        startTout = { Robot.start(application, Robot.TOUT) },
                        startRetest = { ids -> Robot.start(application, Robot.TOUT, ids) },
                        startOreille = { Robot.start(application, Robot.OREILLE) },
                        startMicro = {
                            EcouteMicro.demarrer(
                                autorise = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
                                demander = { ask.launch(Manifest.permission.RECORD_AUDIO) },
                                lancer = { Robot.start(application, Robot.MICRO) },
                            )
                        },
                        resume = { Robot.start(application) },
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
    /** « Tout tester » (T1). */
    val startTout: () -> Unit = {},
    /** « Retester les corrections »: the chosen steps' ids. */
    val startRetest: (Set<String>) -> Unit = {},
    /** « Questions à l'oreille » (T3). */
    val startOreille: () -> Unit = {},
    /** « Écoute au micro » (T1b), once its sentence is read: the microphone asked for if needed, then the run. */
    val startMicro: () -> Unit = {},
    /** « Reprendre »: the run the logbook holds. */
    val resume: () -> Unit = {},
    val answer: (Boolean) -> Unit = {},
    val reset: () -> Unit = {},
    val send: () -> Unit = {},
)

/** The screen, from [etat] alone ([granted]: each permission, its button's words, given or not). */
@Composable
fun BenTestScreen(etat: Robot.Etat, granted: List<Triple<String, String, Boolean>>, actions: BenTestActions, candidats: List<Robot.Candidat> = emptyList()) {
    var liste by remember { mutableStateOf(false) }
    var micro by remember { mutableStateOf(false) }
    val coches = remember(candidats) { mutableStateMapOf<String, Boolean>().apply { candidats.forEach { put(it.id, it.coche) } } }
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
                    Button(onClick = actions.resume, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("bentest_reprendre")) { Text("Reprendre") }
                }
                etat.verdict?.let {
                    Text((when (etat.passage) { Robot.TOUT -> "Tout tester terminé : "; Robot.OREILLE -> "Questions à l'oreille terminées : "; Robot.MICRO -> "Écoute au micro terminée : "; else -> "Essai éclair terminé : " }) + it, style = MaterialTheme.typography.titleMedium, color = Neon.TextPrimary, modifier = Modifier.testTag("bentest_verdict"))
                    if (etat.extraits > 0) Text(
                        "Le rapport contient ${etat.extraits} extrait" + (if (etat.extraits > 1) "s" else "") + " de 2 s enregistré" + (if (etat.extraits > 1) "s" else "") + " par le micro, autour d'un problème entendu. Rien d'autre du micro.",
                        color = Neon.Amber, modifier = Modifier.testTag("bentest_extraits"),
                    )
                    Button(onClick = actions.send, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("bentest_envoyer")) { Text("Envoyer le rapport") }
                }
                if (etat.interrompu == null && etat.verdict == null && micro) {
                    Text("Écoute au micro", style = MaterialTheme.typography.titleMedium, color = Neon.TextPrimary)
                    Text(EcouteMicro.PHRASE, color = Neon.TextPrimary, modifier = Modifier.testTag("bentest_micro_phrase"))
                    Button(onClick = { micro = false; actions.startMicro() }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("bentest_micro_go")) { Text("Commencer") }
                    OutlinedButton(onClick = { micro = false }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("bentest_micro_retour")) { Text("Retour") }
                } else if (etat.interrompu == null && etat.verdict == null && liste) {
                    Text("Retester les corrections : coche les étapes à refaire", style = MaterialTheme.typography.titleMedium, color = Neon.TextPrimary)
                    if (candidats.none { it.coche }) {
                        Text(
                            (if (candidats.none { it.feu != null }) "Aucun passage enregistré sur ce téléphone. " else "") + "Coche les étapes à revoir.",
                            color = Neon.TextSecondary, modifier = Modifier.testTag("bentest_retest_aide"),
                        )
                    }
                    candidats.forEach { c ->
                        // The whole row ticks (a finger on the step's name, not only on the small box).
                        Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .toggleable(value = coches[c.id] == true, role = androidx.compose.ui.semantics.Role.Checkbox, onValueChange = { coches[c.id] = it })
                                .testTag("bentest_retest_${c.id}"),
                        ) {
                            androidx.compose.material3.Checkbox(checked = coches[c.id] == true, onCheckedChange = null)
                            Text(c.label + (c.feu?.let { " (dernier passage : ${it.mot})" } ?: ""), color = Neon.TextPrimary)
                        }
                    }
                    val n = coches.count { it.value }
                    Button(
                        onClick = { actions.startRetest(coches.filterValues { it }.keys.toSet()); liste = false },
                        enabled = n > 0, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("bentest_retest_go"),
                    ) { Text(if (n > 0) "Retester $n étape" + (if (n > 1) "s" else "") else "Coche au moins une étape") }
                    OutlinedButton(onClick = { liste = false }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("bentest_retest_retour")) { Text("Retour") }
                } else if (etat.interrompu == null && etat.verdict == null) {
                    Text(
                        "Tout tester : environ 1 heure, sans rien toucher. Avant : chargeur branché, pièce calme, « Ne pas déranger » activé, volume à moitié. " +
                            "Le robot joue les 37 styles, fait 4 exports, mesure la latence, suit le Guide, tourne l'écran, puis laisse l'écran s'éteindre.",
                        color = Neon.TextSecondary,
                    )
                    Button(onClick = actions.startTout, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("bentest_tout")) { Text("Tout tester") }
                    Text("Essai éclair : environ 5 minutes. Le robot fait 3 morceaux, 1 export, visite 5 écrans, puis te pose 1 question.", color = Neon.TextSecondary)
                    OutlinedButton(onClick = actions.start, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("bentest_lancer")) { Text("Lancer l'essai éclair") }
                    Text("Questions à l'oreille : environ 10 minutes. Le robot lance les morceaux, une question à la fois s'affiche en haut de l'écran : ✅ ou ❌.", color = Neon.TextSecondary)
                    OutlinedButton(onClick = actions.startOreille, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("bentest_oreille")) { Text("Questions à l'oreille") }
                    Text("Écoute au micro : environ 5 minutes, téléphone posé, pièce calme. Le robot écoute le haut-parleur avec le micro.", color = Neon.TextSecondary)
                    OutlinedButton(onClick = { micro = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("bentest_micro")) { Text("Écoute au micro") }
                    OutlinedButton(onClick = { liste = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("bentest_retest")) { Text("Retester les corrections") }
                }
                if (etat.interrompu != null || etat.verdict != null) {
                    OutlinedButton(onClick = actions.reset, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("bentest_recommencer")) { Text("Tout recommencer") }
                }
                etat.message?.let { Text(it, color = Neon.TextSecondary) }
            }
        }
    }
}
