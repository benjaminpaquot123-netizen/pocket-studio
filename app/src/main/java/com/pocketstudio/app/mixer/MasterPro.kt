package com.pocketstudio.app.mixer

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.report.AppLog
import com.pocketstudio.app.audio.AudioDecoder
import com.pocketstudio.app.audio.FloatWav
import com.pocketstudio.app.export.Bands
import com.pocketstudio.app.export.Loudness
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.engine.NativeSample
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Insert
import com.pocketstudio.model.LoudnessTargets
import com.pocketstudio.model.MasterPro
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.Project
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.ProSlider
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.pillColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

/**
 * Lot E (« Master pro », PLAN-AUDITS-FINAUX §1 bis points 1 to 3): « Avancé » under the master in Pro
 * mode. The target in integrated LUFS and the true-peak ceiling, the master's chain in order (EQ, glue,
 * width with the bass in mono, saturation, limiter's attack and release), « A/B au même volume » and a
 * reference song heard at the same loudness. The Mode simple never shows it (its three choices stay).
 */
data class MasterProLive(
    /** « A/B au même volume » on (the engine measures and matches both sides). */
    val compare: Boolean = false,
    /** NativeEngine.LISTEN_*: what is heard. */
    val listen: Int = NativeEngine.LISTEN_MASTER,
    /** The reference song is being read. */
    val busy: Boolean = false,
    val error: String? = null,
)

/** What the master pro panel needs from the ViewModel (held by RhythmViewModel in one field). */
class MasterProController(
    private val scope: CoroutineScope,
    private val app: Application,
    val project: StateFlow<Project>,
    private val edit: (label: String, transform: (MasterPro) -> MasterPro) -> Unit,
    private val sync: () -> Unit,
    private val setInsertType: (ch: Int, slot: Int, type: EffectType) -> Unit,
    private val setMonoBass: (Float) -> Unit,
    private val projectId: () -> String,
) {
    private val _live = MutableStateFlow(MasterProLive())
    val live: StateFlow<MasterProLive> = _live.asStateFlow()
    private var installedFor: String? = null
    private var bands: Pair<String, DoubleArray?>? = null

    fun setTarget(lufs: Float?) {
        edit("pro-target") { it.copy(targetLufs = lufs?.coerceIn(MasterPro.MIN_LUFS, MasterPro.MAX_LUFS)) }
        sync()
    }

    fun setCeiling(db: Float) {
        edit("pro-ceiling") { it.copy(ceilingDb = db) }
        sync()
    }

    fun setAttack(share: Float) {
        edit("pro-attack") { it.copy(attack = share.coerceIn(0.2f, 1f)) }
        sync()
    }

    fun setRelease(scale: Float) {
        edit("pro-release") { it.copy(release = scale.coerceIn(0.25f, 4f)) }
        sync()
    }

    fun setMonoBassHz(hz: Float) = setMonoBass(hz)

    /** « A/B au même volume »: on, the engine measures both sides and brings them to the quietest one's loudness. */
    fun setCompare(on: Boolean) {
        if (NativeEngine.isAvailable) {
            NativeEngine.setMasterCompare(on)
            if (!on) NativeEngine.setMasterListen(NativeEngine.LISTEN_MASTER)
        }
        _live.update { it.copy(compare = on, listen = if (on) it.listen else NativeEngine.LISTEN_MASTER) }
    }

    fun setListen(listen: Int) {
        if (listen != NativeEngine.LISTEN_MASTER && !_live.value.compare) setCompare(true)
        if (NativeEngine.isAvailable) NativeEngine.setMasterListen(listen)
        _live.update { it.copy(listen = listen) }
    }

    /** The panel is closed: the master heard as always. */
    fun leave() {
        if (_live.value.compare || _live.value.listen != NativeEngine.LISTEN_MASTER) setCompare(false)
    }

    /** The master's chain in order, each stage in the master's slots (the effects already there kept). */
    fun putChain() {
        val master = project.value.mixer.master
        val free = master.indices.filter { master[it].effect == EffectType.NONE }.toMutableList()
        for (type in CHAIN) {
            if (master.any { it.effect in stageTypes(type) }) continue
            val slot = free.removeFirstOrNull() ?: break
            setInsertType(Mixer.MASTER, slot, type)
        }
        if (project.value.mixer.monoBassHz <= 0f) setMonoBass(DEFAULT_MONO_BASS)
    }

    private fun refFile(id: String = projectId()) = File(app.filesDir, "references/$id.wav")
    private fun coverFile(id: String = projectId()) = File(app.filesDir, "covers/$id.jpg")

    /** The reference song: its first [REFERENCE_SECONDS] seconds decoded, measured, kept with the project, heard on demand. */
    fun importReference(uri: Uri) {
        if (_live.value.busy) return
        _live.update { it.copy(busy = true, error = null) }
        val id = projectId()
        scope.launch {
            try {
                val (lufs, name) = withContext(Dispatchers.IO) {
                    val raw = File(app.cacheDir, "reference-${System.nanoTime()}.raw")
                    try {
                        val audio = AudioDecoder.decodeToFile(app, uri, raw, REFERENCE_SECONDS.toDouble(), { false }, cut = true)
                        val frames = audio.frames.coerceAtMost(REFERENCE_SECONDS.toLong() * audio.sampleRate).toInt()
                        val data = AudioDecoder.readRaw(raw, frames, audio.channels, mono = false)
                        val meter = Loudness.Meter(audio.sampleRate, audio.channels)
                        meter.add(data, frames)
                        val lufs = meter.integrated() ?: throw java.io.IOException(tr("La référence est silencieuse."))
                        refFile(id).parentFile?.mkdirs()
                        FloatWav.write(refFile(id), data, 0, frames, audio.channels, audio.sampleRate)
                        lufs.toFloat() to (uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')?.take(40) ?: tr("Référence"))
                    } finally {
                        raw.delete()
                    }
                }
                bands = null
                installedFor = null
                edit("pro-reference") { it.copy(reference = refFile(id).name, referenceName = name, referenceLufs = lufs) }
                attach()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.add("Référence : lecture impossible (${e.message})")
                _live.update { it.copy(error = tr("Impossible de lire ce morceau.")) }
            } finally {
                _live.update { it.copy(busy = false) }
            }
        }
    }

    fun clearReference() {
        refFile().delete()
        bands = null
        installedFor = null
        if (NativeEngine.isAvailable) NativeEngine.setReference(null, Float.NaN)
        if (_live.value.listen == NativeEngine.LISTEN_REFERENCE) setListen(NativeEngine.LISTEN_MASTER)
        edit("pro-reference") { it.copy(reference = null, referenceName = null, referenceLufs = null) }
    }

    /** The panel is shown: the project's reference (if any) in the engine, once per project. */
    fun attach() {
        val pro = project.value.mixer.pro ?: return
        val lufs = pro.referenceLufs ?: return
        val id = projectId()
        val file = refFile(id)
        if (installedFor == id || !file.exists() || !NativeEngine.isAvailable) return
        installedFor = id
        scope.launch {
            val sample = withContext(Dispatchers.IO) {
                runCatching { FloatWav.read(file) }.getOrNull()?.let { NativeSample.create(it.data, it.channels, it.sampleRate) }
            } ?: return@launch
            NativeEngine.setReference(sample, lufs)
            sample.close()  // the engine holds its own reference
        }
    }

    /** The reference's octave bands (dB against its whole), for the export's report; null without one. Worker thread. */
    fun referenceBands(): DoubleArray? {
        val file = refFile()
        if (project.value.mixer.pro?.reference == null || !file.exists()) return null
        bands?.takeIf { it.first == file.path }?.let { return it.second }
        return Bands.of(file).also { bands = file.path to it }
    }

    /** The cover for the files' tags (JPEG), null without one. Worker thread. */
    fun coverBytes(): ByteArray? = coverFile().takeIf { it.exists() && it.length() in 1..MAX_COVER_BYTES }?.readBytes()

    fun hasCover(): Boolean = coverFile().exists()

    /** A cover picked: kept as a JPEG at most 600 px wide. */
    fun setCover(uri: Uri?) {
        val file = coverFile()
        scope.launch(Dispatchers.IO) {
            if (uri == null) {
                file.delete()
                return@launch
            }
            runCatching {
                val bitmap = app.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it) } ?: return@runCatching
                val scale = minOf(1f, 600f / maxOf(bitmap.width, bitmap.height))
                val small = if (scale < 1f) android.graphics.Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).roundToInt(), (bitmap.height * scale).roundToInt(), true) else bitmap
                file.parentFile?.mkdirs()
                file.outputStream().use { small.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, it) }
            }.onFailure { AppLog.add("Pochette : lecture impossible (${it.message})") }
        }
    }

    companion object {
        const val REFERENCE_SECONDS = 90
        const val MAX_COVER_BYTES = 2L * 1024 * 1024
        const val DEFAULT_MONO_BASS = 120f
        val MONO_BASS = listOf(0f, 80f, 120f, 160f)
        /** The chain's stages, in the order they play. */
        val CHAIN = listOf(EffectType.PARAM_EQ, EffectType.COMPRESSOR, EffectType.WIDENER, EffectType.SATURATION)

        /** The effects that count as [stage] (any EQ is the EQ). */
        fun stageTypes(stage: EffectType): Set<EffectType> = if (stage == EffectType.PARAM_EQ) setOf(EffectType.PARAM_EQ, EffectType.EQ) else setOf(stage)

        fun stageName(stage: EffectType): String = when (stage) {
            EffectType.PARAM_EQ -> tr("Égaliseur")
            EffectType.COMPRESSOR -> tr("Compresseur de bus (glue)")
            EffectType.WIDENER -> tr("Largeur stéréo")
            else -> tr("Saturation douce")
        }
    }
}

/** What the mixer screen gives the panel (MixerActions.pro). */
data class MasterProActions(
    val controller: MasterProController? = null,
    val live: StateFlow<MasterProLive> = MutableStateFlow(MasterProLive()),
    /** « Avancé » › the file's final form (ExportUi.pro). */
    val setExportOptions: (com.pocketstudio.app.export.ExportOptions) -> Unit = {},
)

/** « Avancé » under the master (Pro mode). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MasterProPanel(styleLufs: Float, actions: MasterProActions, onStyleTarget: () -> Unit) {
    val c = actions.controller ?: return
    val project by c.project.collectAsState()
    MasterProPanel(project.mixer.pro, project.mixer.master, project.mixer.monoBassHz, styleLufs, actions, onStyleTarget)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MasterProPanel(pro: MasterPro?, master: List<Insert>, monoBassHz: Float, styleLufs: Float, actions: MasterProActions, onStyleTarget: () -> Unit) {
    val c = actions.controller ?: return
    var open by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { open = !open }, modifier = Modifier.fillMaxWidth().testTag("master_pro")) {
        Text(tr("Avancé : master pro"), Modifier.weight(1f), color = Neon.Cyan)
        Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, tint = Neon.Cyan)
    }
    if (!open) return
    DisposableEffect(c) { onDispose { c.leave() } }
    LaunchedEffect(c, pro?.reference) { c.attach() }
    val live by actions.live.collectAsState()
    val p = pro ?: MasterPro()
    val colors = pillColors(Neon.Cyan)
    val small = MaterialTheme.typography.bodySmall
    Column(Modifier.testTag("master_pro_panel"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // 1. The target: in integrated LUFS, or the style's (the « Volume visé » above).
        Text(tr("Cible de volume (morceau entier)"), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MasterPro.Preset.entries.forEach { preset ->
                val label = when (preset) {
                    MasterPro.Preset.SPOTIFY -> tr("Spotify, YouTube · −14")
                    MasterPro.Preset.APPLE -> tr("Apple Music · −16")
                    MasterPro.Preset.CLUB -> tr("Club · −9")
                    MasterPro.Preset.STYLE -> tr("Comme le style · %s", LoudnessTargets.formatTarget(styleLufs))
                }
                val selected = if (preset == MasterPro.Preset.STYLE) p.targetLufs == null else p.targetLufs == preset.lufs
                FilterChip(selected = selected, onClick = {
                    c.setTarget(preset.lufs)
                    if (preset == MasterPro.Preset.STYLE) onStyleTarget()
                }, label = { Text(label) }, colors = colors, modifier = Modifier.testTag("pro_target_${preset.name}"))
            }
            val free = p.targetLufs != null && MasterPro.presetOf(p.targetLufs) == null
            FilterChip(selected = free, onClick = { c.setTarget(p.targetLufs ?: -11f) }, label = { Text(tr("Libre")) }, colors = colors,
                modifier = Modifier.testTag("pro_target_free"))
        }
        val freeLufs = p.targetLufs
        if (freeLufs != null && MasterPro.presetOf(freeLufs) == null) {
            Text(tr("%s LUFS", LoudnessTargets.formatTarget(freeLufs)), style = small, color = Neon.Cyan)
            ProSlider(value = freeLufs, onValueChange = { c.setTarget((it * 2).roundToInt() / 2f) },
                valueRange = MasterPro.MIN_LUFS..MasterPro.MAX_LUFS, modifier = Modifier.testTag("pro_target_slider"))
        }
        Text(tr("Plafond de crête (mesuré après compression)"), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MasterPro.CEILINGS.forEach { db ->
                FilterChip(selected = p.ceilingDb == db, onClick = { c.setCeiling(db) }, label = { Text("${LoudnessTargets.format(db.toDouble())} dBTP") },
                    colors = colors, modifier = Modifier.testTag("pro_ceiling_$db"))
            }
        }
        // 2. The chain, in the order it plays.
        Text(tr("La chaîne du master, dans l'ordre"), style = MaterialTheme.typography.bodyMedium)
        MasterProController.CHAIN.forEachIndexed { i, stage ->
            val there = master.any { it.effect in MasterProController.stageTypes(stage) && !it.bypass }
            Text("${i + 1}. ${MasterProController.stageName(stage)} · " + if (there) tr("en place") else tr("absent"),
                style = small, color = if (there) Neon.Acid else Neon.TextSecondary, modifier = Modifier.testTag("pro_stage_$i"))
        }
        Text(tr("Grave en mono sous"), style = small)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MasterProController.MONO_BASS.forEach { hz ->
                FilterChip(selected = monoBassHz == hz, onClick = { c.setMonoBassHz(hz) },
                    label = { Text(if (hz == 0f) tr("Non") else "${hz.roundToInt()} Hz") }, colors = colors, modifier = Modifier.testTag("pro_mono_${hz.roundToInt()}"))
            }
        }
        Text("5. " + tr("Limiteur : look-ahead 1 ms"), style = small, color = Neon.Acid)
        Text(tr("Attaque : %d %% du look-ahead", (p.attack * 100).roundToInt()), style = small)
        ProSlider(value = p.attack, onValueChange = { c.setAttack((it * 20).roundToInt() / 20f) }, valueRange = 0.2f..1f, modifier = Modifier.testTag("pro_attack"))
        Text(tr("Relâchement : ×%s", LoudnessTargets.format(p.release.toDouble())), style = small)
        ProSlider(value = p.release, onValueChange = { c.setRelease((it * 4).roundToInt() / 4f) }, valueRange = 0.25f..4f, modifier = Modifier.testTag("pro_release"))
        OutlinedButton(onClick = c::putChain, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("pro_chain")) {
            Text(tr("Mettre la chaîne pro (réglages dans « Effets »)"))
        }
        // 3. A/B and the reference at the same loudness.
        Text(tr("Écouter"), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOfNotNull(
                NativeEngine.LISTEN_MASTER to tr("A · avec le master"),
                NativeEngine.LISTEN_WITHOUT to tr("B · sans le master"),
                (NativeEngine.LISTEN_REFERENCE to tr("Référence")).takeIf { p.reference != null },
            ).forEach { (l, label) ->
                FilterChip(selected = live.listen == l, onClick = { c.setListen(l) }, label = { Text(label) }, colors = colors,
                    modifier = Modifier.testTag("pro_listen_$l"))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = live.compare, onClick = { c.setCompare(!live.compare) }, label = { Text(tr("Au même volume")) }, colors = colors,
                modifier = Modifier.testTag("pro_compare"))
        }
        Text(
            if (live.compare) tr("Les deux côtés sont mis au même volume : tu entends ce que le master change, pas qu'il est plus fort.")
            else tr("Sans « Au même volume », le plus fort semble souvent le meilleur."),
            style = small, color = Neon.TextSecondary,
        )
        val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(c::importReference) }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = { pick.launch(arrayOf("audio/*")) }, enabled = !live.busy, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("pro_reference")) {
                Text(if (live.busy) tr("Lecture…") else if (p.reference == null) tr("Importer une référence") else tr("Changer la référence"), maxLines = 2)
            }
            if (p.reference != null) TextButton(onClick = c::clearReference, modifier = Modifier.heightIn(min = 48.dp).testTag("pro_reference_clear")) { Text(tr("Retirer")) }
        }
        p.reference?.let {
            Text(tr("Référence : « %1\$s », %2\$s LUFS", p.referenceName ?: "", LoudnessTargets.format((p.referenceLufs ?: 0f).toDouble())),
                style = small, color = Neon.TextSecondary, modifier = Modifier.testTag("pro_reference_name"))
        }
        live.error?.let { Text(it, style = small, color = Neon.RedText) }
    }
}
