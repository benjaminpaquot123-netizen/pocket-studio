package com.pocketstudio.app.midi

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import androidx.lifecycle.AndroidViewModel
import com.pocketstudio.app.Metrics
import androidx.lifecycle.viewModelScope
import com.pocketstudio.app.report.AppLog
import com.pocketstudio.engine.NativeEngine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/** A MIDI source the user can pick. */
data class MidiSource(val id: String, val name: String, val kind: String)

data class MidiUi(
    /** False when the phone has no MIDI support at all. */
    val available: Boolean = true,
    val sources: List<MidiSource> = emptyList(),
    val connected: String? = null,
    /** The last thing received, in words ("Note Do 4", "Horloge 124 BPM"). */
    val last: String? = null,
    val followClock: Boolean = true,
    val clockBpm: Double? = null,
    val scanning: Boolean = false,
    /** Send our tempo, start and stop to the connected device. */
    val sendClock: Boolean = false,
)

/** What a MIDI device asks of the project (the tempo and the transport are the project's). */
sealed interface MidiCommand {
    data object Start : MidiCommand
    data object Stop : MidiCommand
    data class Tempo(val bpm: Double) : MidiCommand
    /** A note received (it already sounds): for live recording. Channel 10 (9) = pads. */
    data class Note(val channel: Int, val pitch: Int, val velocity: Float, val on: Boolean) : MidiCommand
    /** A knob or fader moved (MIDI learn, mapped controls). */
    data class Control(val channel: Int, val controller: Int, val value: Int) : MidiCommand
}

/**
 * MIDI keyboards and controllers over USB or Bluetooth (android.media.midi). Notes play the synth
 * track at once (channel 10: notes 36-51 hit the 16 pads); clock, start and stop follow another
 * machine when [MidiUi.followClock] is on. Notes go straight to the engine from the MIDI thread;
 * the rest goes through [commands] to the main thread.
 */
class MidiViewModel(app: Application) : AndroidViewModel(app) {
    private val manager: MidiManager? =
        if (app.packageManager.hasSystemFeature(PackageManager.FEATURE_MIDI)) app.getSystemService(MidiManager::class.java) else null
    private val main = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow(MidiUi(available = manager != null))
    val state: StateFlow<MidiUi> = _state.asStateFlow()

    private val _commands = MutableSharedFlow<MidiCommand>(extraBufferCapacity = 256)
    val commands: SharedFlow<MidiCommand> = _commands.asSharedFlow()

    private val infos = HashMap<String, MidiDeviceInfo>()
    private val bluetooth = HashMap<String, BluetoothDevice>()
    private var device: MidiDevice? = null
    private var port: MidiOutputPort? = null
    /** Where we send (clock out); null when the device takes nothing. */
    @Volatile private var sendPort: android.media.midi.MidiInputPort? = null
    /** Counts connect and disconnect calls: a device that opens late for an old one is closed. */
    private var connection = 0
    private val held = HashSet<Int>()  // notes down on the synth, released on disconnect
    private val playedAs = IntArray(128) { -1 }  // key → the note it plays (scale lock), under held's lock
    private val keysOn = IntArray(128)  // keys down per played note: two keys can snap to one note
    // The track each sounding note started on: its release goes there even if another track was
    // selected meanwhile (else the note would hang on the old one). Under held's lock.
    private val trackOf = IntArray(128)

    /** The project's key: when locked, keys out of the scale play the nearest note of it. */
    @Volatile var harmony: com.pocketstudio.model.Harmony = com.pocketstudio.model.Harmony()
    private val tempo = ClockTempo()
    private var lastTempoSent = 0.0

    private val callback = object : MidiManager.DeviceCallback() {
        override fun onDeviceAdded(device: MidiDeviceInfo) = refresh(autoConnect = true)
        override fun onDeviceRemoved(device: MidiDeviceInfo) {
            if (_state.value.connected == idOf(device)) disconnect()
            refresh(autoConnect = false)
        }
    }

    init {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            manager?.registerDeviceCallback(MidiManager.TRANSPORT_MIDI_BYTE_STREAM, { main.post(it) }, callback)
        } else {
            @Suppress("DEPRECATION")
            manager?.registerDeviceCallback(callback, main)
        }
        refresh(autoConnect = true)
    }

    private fun idOf(info: MidiDeviceInfo) = "midi-${info.id}"

    private fun kindOf(info: MidiDeviceInfo) = when (info.type) {
        MidiDeviceInfo.TYPE_USB -> "USB"
        MidiDeviceInfo.TYPE_BLUETOOTH -> "Bluetooth"
        else -> "Appli"
    }

    private fun nameOf(info: MidiDeviceInfo): String {
        val p = info.properties
        return p.getString(MidiDeviceInfo.PROPERTY_NAME) ?: p.getString(MidiDeviceInfo.PROPERTY_PRODUCT) ?: "Appareil MIDI"
    }

    private fun refresh(autoConnect: Boolean) {
        val m = manager ?: return
        // Only devices that send something (a keyboard has output ports).
        val all = if (android.os.Build.VERSION.SDK_INT >= 33) {
            m.getDevicesForTransport(MidiManager.TRANSPORT_MIDI_BYTE_STREAM).toList()
        } else {
            @Suppress("DEPRECATION")
            m.devices.toList()
        }
        val list = all.filter { it.outputPortCount > 0 }
        infos.clear()
        list.forEach { infos[idOf(it)] = it }
        val sources = list.map { MidiSource(idOf(it), nameOf(it), kindOf(it)) } +
            bluetooth.filterKeys { k -> list.none { kindOf(it) == "Bluetooth" && nameOf(it) == bluetooth[k]?.safeName() } }
                .map { (k, d) -> MidiSource(k, d.safeName(), "Bluetooth") }
        _state.update { it.copy(sources = sources) }
        if (autoConnect && _state.value.connected == null) list.firstOrNull { it.type != MidiDeviceInfo.TYPE_VIRTUAL }?.let { connect(idOf(it)) }
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.safeName(): String = runCatching { name }.getOrNull() ?: "Clavier Bluetooth"

    fun connect(id: String) {
        val m = manager ?: return
        disconnect()
        val attempt = ++connection
        val opened = { d: MidiDevice? ->
            if (attempt != connection) {
                d?.close()  // another device was chosen meanwhile
            } else if (d == null) {
                AppLog.add("MIDI : ouverture impossible")
                _state.update { it.copy(last = "Connexion impossible") }
            } else {
                attach(id, d)
            }
        }
        val info = infos[id]
        val ble = bluetooth[id]
        when {
            info != null -> m.openDevice(info, { opened(it) }, main)
            ble != null -> m.openBluetoothDevice(ble, { opened(it) }, main)
        }
    }

    private fun attach(id: String, d: MidiDevice) {
        val out = d.openOutputPort(0)
        if (out == null) {
            d.close()
            _state.update { it.copy(last = "Cet appareil n'envoie rien") }
            return
        }
        device = d
        port = out
        sendPort = runCatching { d.openInputPort(0) }.getOrNull()
        tempo.reset()
        val parser = MidiParser(listener)
        out.connect(object : MidiReceiver() {
            override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) = parser.feed(msg, offset, count)
        })
        val name = _state.value.sources.firstOrNull { it.id == id }?.name ?: d.info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
        AppLog.add("MIDI connecté : $name")
        _state.update { it.copy(connected = id, last = null, clockBpm = null) }
        refresh(autoConnect = false)
    }

    fun disconnect() {
        ++connection
        clockJob?.cancel()
        runCatching { sendPort?.close() }
        sendPort = null
        runCatching { port?.close() }
        runCatching { device?.close() }
        port = null
        device = null
        synchronized(held) {
            if (NativeEngine.isAvailable) held.forEach { NativeEngine.liveNoteOff(it, trackOf[it]) }
            held.clear()
            playedAs.fill(-1)
            keysOn.fill(0)
        }
        _state.update { it.copy(connected = null, clockBpm = null) }
    }

    fun setFollowClock(on: Boolean) {
        _state.update { it.copy(followClock = on, sendClock = if (on) false else it.sendClock) }
        if (on) stopClock()
    }

    // --- Bluetooth LE MIDI: devices must be found by a scan before they can be opened ----------

    private val bleMidi = ParcelUuid(UUID.fromString("03B80E5A-EDE8-4B33-A751-6CE34EC4C700"))
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val d = result.device
            val id = "ble-${d.address}"
            if (bluetooth.put(id, d) == null) main.post { refresh(autoConnect = false) }
        }
    }

    /** Needs BLUETOOTH_SCAN and BLUETOOTH_CONNECT (asked by the screen first). Ten seconds. */
    @SuppressLint("MissingPermission")
    fun scanBluetooth() {
        val scanner = getApplication<Application>().getSystemService(BluetoothManager::class.java)?.adapter?.bluetoothLeScanner
        if (scanner == null) {
            _state.update { it.copy(last = "Bluetooth éteint") }
            return
        }
        runCatching {
            scanner.startScan(listOf(ScanFilter.Builder().setServiceUuid(bleMidi).build()),
                ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), scanCallback)
        }.onFailure {
            AppLog.add("MIDI : recherche Bluetooth impossible (${it.message})")
            return
        }
        _state.update { it.copy(scanning = true) }
        viewModelScope.launch {
            kotlinx.coroutines.delay(10_000)
            runCatching { scanner.stopScan(scanCallback) }
            _state.update { it.copy(scanning = false) }
        }
    }

    // --- Incoming events (MIDI thread) -----------------------------------------------------------

    // --- Clock out: 24 ticks a beat, sent ahead with timestamps (Android delivers them on time) ---

    @Volatile private var clockBpm = 120.0
    private var clockJob: kotlinx.coroutines.Job? = null

    private var playing = false

    fun setSendClock(on: Boolean) {
        // Following and sending at once with the same device would loop the clock back.
        _state.update { it.copy(sendClock = on, followClock = if (on) false else it.followClock) }
        if (!on) stopClock() else transport(playing, clockBpm)
    }

    /** The transport started or stopped, or the tempo changed (from the main thread). */
    fun transport(playing: Boolean, bpm: Double) {
        this.playing = playing
        clockBpm = bpm
        if (!_state.value.sendClock || sendPort == null) return
        if (playing && clockJob?.isActive != true) startClock() else if (!playing) stopClock()
    }

    private fun send(byte: Int, at: Long) {
        runCatching { sendPort?.send(byteArrayOf(byte.toByte()), 0, 1, at) }
    }

    private fun startClock() {
        clockJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            // Wait for the first step (after any count-in): the other machine starts with it.
            while (NativeEngine.isAvailable && NativeEngine.absoluteStep() < 0) kotlinx.coroutines.delay(2)
            val latencyNs = (Metrics.streamInfo.value.outputLatencyMs.takeIf { it > 0 } ?: 0.0) * 1_000_000
            val position = NativeEngine.playPosition().coerceAtLeast(0.0)  // sixteenths already played
            val stepNs = 15e9 / clockBpm
            var next = System.nanoTime() + latencyNs.toLong() - (position * stepNs).toLong()
            val tickNs = (60e9 / (clockBpm * 24)).toLong()
            if (position < 1.0) {
                send(0xFA, next)  // from the top: Start on the first step
            } else {
                // Switched on while playing: no burst of past ticks, carry on from now.
                val now = System.nanoTime()
                while (next < now) next += tickNs
                send(0xFB, next)
            }
            while (true) {
                val horizon = System.nanoTime() + 60_000_000
                while (next < horizon) {
                    send(0xF8, next)
                    next += (60e9 / (clockBpm * 24)).toLong()
                }
                kotlinx.coroutines.delay(20)
            }
        }
    }

    private fun stopClock() {
        if (clockJob == null) return
        clockJob?.cancel()
        clockJob = null
        send(0xFC, System.nanoTime())
    }

    private val listener = object : MidiListener {
        override fun noteOn(channel: Int, note: Int, velocity: Int) {
            if (!NativeEngine.isAvailable) return
            val v = velocity / 127f
            if (channel == DRUM_CHANNEL) {
                if (note in FIRST_PAD_NOTE until FIRST_PAD_NOTE + NativeEngine.LANES) NativeEngine.liveTriggerPad(note - FIRST_PAD_NOTE, v)
                _commands.tryEmit(MidiCommand.Note(channel, note, v, true))
            } else {
                val h = harmony
                val played = if (h.lock) h.snap(note).coerceIn(0, 127) else note
                synchronized(held) {
                    if (note in playedAs.indices) playedAs[note] = played
                    held += played
                    if (keysOn[played]++ == 0) trackOf[played] = NativeEngine.liveTrack
                    NativeEngine.liveNoteOn(played, v, trackOf[played])
                }
                _commands.tryEmit(MidiCommand.Note(channel, played, v, true))
            }
            show("Note ${noteLabel(note)}" + if (channel == DRUM_CHANNEL) " (pads)" else "")
        }

        override fun noteOff(channel: Int, note: Int) {
            if (!NativeEngine.isAvailable || channel == DRUM_CHANNEL) return
            val played = synchronized(held) {
                val p = playedAs.getOrNull(note)?.takeIf { it >= 0 } ?: note
                if (note in playedAs.indices) playedAs[note] = -1
                if (p in keysOn.indices) keysOn[p] = (keysOn[p] - 1).coerceAtLeast(0)
                // Another key still holds the same note: it keeps sounding.
                if (keysOn.getOrElse(p) { 0 } == 0) {
                    held -= p
                    NativeEngine.liveNoteOff(p, trackOf.getOrElse(p) { NativeEngine.liveTrack })
                }
                p
            }
            _commands.tryEmit(MidiCommand.Note(channel, played, 0f, false))
        }

        override fun pitchBend(channel: Int, value: Int) {
            if (NativeEngine.isAvailable && channel != DRUM_CHANNEL) NativeEngine.liveExpression(NativeEngine.EXPR_BEND, value / 8192f * BEND_RANGE)
        }

        override fun controlChange(channel: Int, controller: Int, value: Int) {
            show("Bouton $controller : $value")
            if (NativeEngine.isAvailable && channel != DRUM_CHANNEL) {
                when (controller) {
                    CC_MOD_WHEEL -> NativeEngine.liveExpression(NativeEngine.EXPR_MOD, value / 127f)
                    CC_SUSTAIN -> NativeEngine.liveExpression(NativeEngine.EXPR_SUSTAIN, if (value >= 64) 1f else 0f)
                }
            }
            _commands.tryEmit(MidiCommand.Control(channel, controller, value))
            if (controller == CC_ALL_NOTES_OFF || controller == CC_ALL_SOUND_OFF) {
                synchronized(held) {
                    if (NativeEngine.isAvailable) held.forEach { NativeEngine.liveNoteOff(it, trackOf[it]) }
                    held.clear()
                    playedAs.fill(-1)
                    keysOn.fill(0)
            playedAs.fill(-1)
            keysOn.fill(0)
                }
            }
        }

        override fun clock() {
            val bpm = tempo.tick(System.nanoTime()) ?: return
            if (!_state.value.followClock) return
            // Round to 0.1 BPM and only send real changes: the clock jitters a little.
            val rounded = Math.round(bpm * 10) / 10.0
            if (kotlin.math.abs(rounded - lastTempoSent) >= 0.25) {
                lastTempoSent = rounded
                _commands.tryEmit(MidiCommand.Tempo(rounded))
                _state.update { it.copy(clockBpm = rounded, last = "Horloge ${"%.1f".format(rounded)} BPM") }
            }
        }

        override fun start() {
            tempo.reset()
            if (_state.value.followClock) _commands.tryEmit(MidiCommand.Start)
            show("Départ")
        }

        override fun continuePlayback() = start()

        override fun stop() {
            if (_state.value.followClock) _commands.tryEmit(MidiCommand.Stop)
            show("Arrêt")
        }
    }

    private fun show(text: String) {
        _state.update { it.copy(last = text) }
    }

    override fun onCleared() {
        manager?.unregisterDeviceCallback(callback)
        disconnect()
    }

    companion object {
        const val DRUM_CHANNEL = 9
        const val FIRST_PAD_NOTE = 36  // General MIDI kick: pad 1
        const val CC_ALL_SOUND_OFF = 120
        const val CC_ALL_NOTES_OFF = 123
        const val CC_MOD_WHEEL = 1
        const val CC_SUSTAIN = 64
        /** Pitch bend wheel range in semitones. */
        const val BEND_RANGE = 2f
        private val NAMES = listOf("Do", "Do#", "Ré", "Ré#", "Mi", "Fa", "Fa#", "Sol", "Sol#", "La", "La#", "Si")
        fun noteLabel(note: Int) = "${NAMES[note % 12]} ${note / 12 - 1}"
    }
}
