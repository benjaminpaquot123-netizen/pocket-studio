package com.pocketstudio.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pocketstudio.app.Metrics
import com.pocketstudio.app.latency.LatencyScreen
import com.pocketstudio.app.latency.LatencyViewModel
import com.pocketstudio.app.recette.RecetteScreen
import com.pocketstudio.app.recette.RecetteViewModel
import com.pocketstudio.app.studio.StudioScreen
import com.pocketstudio.app.studio.StudioViewModel
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.ui.Neon
import kotlinx.coroutines.delay

enum class Tab(val label: String, val icon: ImageVector) {
    Studio("Studio", Icons.Filled.GraphicEq),
    Latence("Latence", Icons.Filled.Timer),
    Recette("Recette", Icons.Filled.Checklist),
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val mainVm: MainViewModel = viewModel()
    val studioVm: StudioViewModel = viewModel()
    val latencyVm: LatencyViewModel = viewModel()
    val recetteVm: RecetteViewModel = viewModel()
    var tab by rememberSaveable { mutableStateOf(Tab.Studio) }

    val engineAvailable = NativeEngine.isAvailable
    val stream by Metrics.streamInfo.collectAsStateWithLifecycle()
    val roundTrip by Metrics.roundTripMs.collectAsStateWithLifecycle()

    // The audio stream lives while the app is visible (background playback arrives in phase 1).
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        if (engineAvailable && NativeEngine.start()) studioVm.pushAll()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (engineAvailable) NativeEngine.stop()
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(engineAvailable) {
        if (!engineAvailable) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                Metrics.updateStream(NativeEngine.streamInfo())
                delay(100)
            }
        }
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) latencyVm.measure()
    }

    Scaffold(
        containerColor = Neon.Background,
        bottomBar = {
            NavigationBar(containerColor = Neon.Surface) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(t.label) },
                        modifier = Modifier.testTag("tab_${t.name}"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Neon.Cyan,
                            selectedTextColor = Neon.Cyan,
                            indicatorColor = Neon.SurfaceHigh,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                Tab.Studio -> {
                    val state by studioVm.state.collectAsStateWithLifecycle()
                    StudioScreen(
                        state = state,
                        stream = stream,
                        engineAvailable = engineAvailable,
                        onTogglePlay = studioVm::togglePlaying,
                        onFrequency = studioVm::setFrequency,
                        onVolume = studioVm::setVolume,
                    )
                }
                Tab.Latence -> {
                    val state by latencyVm.state.collectAsStateWithLifecycle()
                    LatencyScreen(
                        state = state,
                        stream = stream,
                        lastRoundTripMs = roundTrip,
                        engineAvailable = engineAvailable,
                        onMeasure = {
                            if (hasMic(context)) latencyVm.measure()
                            else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                        },
                    )
                }
                Tab.Recette -> {
                    val state by recetteVm.state.collectAsStateWithLifecycle()
                    RecetteScreen(
                        state = state,
                        stream = stream,
                        roundTripMs = roundTrip,
                        onUpdate = recetteVm::update,
                        onSendReport = { recetteVm.buildReport { openUri(context, it) } },
                        onReportProblem = { text -> recetteVm.buildProblem(text) { openUri(context, it) } },
                    )
                }
            }
        }
    }

    LaunchPrompts(mainVm)
}

@Composable
private fun LaunchPrompts(vm: MainViewModel) {
    val context = LocalContext.current
    val crash by vm.crash.collectAsStateWithLifecycle()
    val update by vm.update.collectAsStateWithLifecycle()

    if (crash != null) {
        AlertDialog(
            onDismissRequest = vm::dismissCrash,
            title = { Text("Aïe, l'appli est tombée") },
            text = { Text("Elle s'est fermée brutalement la dernière fois. Envoyer le rapport ? Il est déjà rédigé, tu n'as qu'à appuyer sur Envoyer.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.crashReportUri()?.let { openUri(context, it) }
                    vm.dismissCrash()
                }) { Text("Envoyer le rapport") }
            },
            dismissButton = { TextButton(onClick = vm::dismissCrash) { Text("Ignorer") } },
        )
        return
    }

    when (val u = update) {
        is UpdateState.Available, is UpdateState.Failed -> {
            val info = if (u is UpdateState.Available) u.info else (u as UpdateState.Failed).info
            AlertDialog(
                onDismissRequest = vm::dismissUpdate,
                title = { Text("Nouvelle version ${info.versionName}") },
                text = {
                    Column {
                        if (u is UpdateState.Failed) Text("Le téléchargement a échoué, on retente ?", color = Neon.Red)
                        Text(info.notes.ifBlank { "Une nouvelle version est prête." })
                    }
                },
                confirmButton = { TextButton(onClick = vm::installUpdate) { Text("Installer") } },
                dismissButton = { TextButton(onClick = vm::dismissUpdate) { Text("Plus tard") } },
            )
        }
        is UpdateState.Downloading -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Téléchargement…") },
            text = { LinearProgressIndicator(progress = { u.progress }) },
            confirmButton = {},
        )
        UpdateState.None -> Unit
    }
}

private fun hasMic(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

private fun openUri(context: Context, uri: Uri) {
    context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
