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
import androidx.lifecycle.viewModelScope
import com.pocketstudio.app.report.AppLog
import com.pocketstudio.engine.NativeEngine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
)

/** What a MIDI device asks of the project (the tempo and the transport are the project's). */
sealed interface MidiCommand {
    data object Start : MidiCommand
    data object Stop : MidiCommand
    data class Tempo(val bpm: Double) : MidiCommand
    /** A note received (it already sounds): for live recording. Channel 10 (9) = pads. */
    data class Note(val channel: Int, val pitch: Int, val velocity: Float, val on: Boolean) : MidiCommand
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
    private val held = HashSet<Int>()  // notes down on the synth, released on disconnect
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
        _state.value = _state.value.copy(sources = sources)
        if (autoConnect && _state.value.connected == null) list.firstOrNull { it.type != MidiDeviceInfo.TYPE_VIRTUAL }?.let { connect(idOf(it)) }
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.safeName(): String = runCatching { name }.getOrNull() ?: "Clavier Bluetooth"

    fun connect(id: String) {
        val m = manager ?: return
        disconnect()
        val opened = { d: MidiDevice? ->
            if (d == null) {
                AppLog.add("MIDI : ouverture impossible")
                _state.value = _state.value.copy(last = "Connexion impossible")
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
            _state.value = _state.value.copy(last = "Cet appareil n'envoie rien")
            return
        }
        device = d
        port = out
        tempo.reset()
        val parser = MidiParser(listener)
        out.connect(object : MidiReceiver() {
            override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) = parser.feed(msg, offset, count)
        })
        val name = _state.value.sources.firstOrNull { it.id == id }?.name ?: d.info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
        AppLog.add("MIDI connecté : $name")
        _state.value = _state.value.copy(connected = id, last = null, clockBpm = null)
        refresh(autoConnect = false)
    }

    fun disconnect() {
        runCatching { port?.close() }
        runCatching { device?.close() }
        port = null
        device = null
        synchronized(held) {
            if (NativeEngine.isAvailable) held.forEach { NativeEngine.liveNoteOff(it) }
            held.clear()
        }
        _state.value = _state.value.copy(connected = null, clockBpm = null)
    }

    fun setFollowClock(on: Boolean) {
        _state.value = _state.value.copy(followClock = on)
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
            _state.value = _state.value.copy(last = "Bluetooth éteint")
            return
        }
        runCatching {
            scanner.startScan(listOf(ScanFilter.Builder().setServiceUuid(bleMidi).build()),
                ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), scanCallback)
        }.onFailure {
            AppLog.add("MIDI : recherche Bluetooth impossible (${it.message})")
            return
        }
        _state.value = _state.value.copy(scanning = true)
        viewModelScope.launch {
            kotlinx.coroutines.delay(10_000)
            runCatching { scanner.stopScan(scanCallback) }
            _state.value = _state.value.copy(scanning = false)
        }
    }

    // --- Incoming events (MIDI thread) -----------------------------------------------------------

    private val listener = object : MidiListener {
        override fun noteOn(channel: Int, note: Int, velocity: Int) {
            if (!NativeEngine.isAvailable) return
            val v = velocity / 127f
            if (channel == DRUM_CHANNEL) {
                if (note in FIRST_PAD_NOTE until FIRST_PAD_NOTE + NativeEngine.LANES) NativeEngine.liveTriggerPad(note - FIRST_PAD_NOTE, v)
            } else {
                synchronized(held) { held += note }
                NativeEngine.liveNoteOn(note, v)
            }
            _commands.tryEmit(MidiCommand.Note(channel, note, v, true))
            show("Note ${noteLabel(note)}" + if (channel == DRUM_CHANNEL) " (pads)" else "")
        }

        override fun noteOff(channel: Int, note: Int) {
            if (!NativeEngine.isAvailable || channel == DRUM_CHANNEL) return
            synchronized(held) { held -= note }
            NativeEngine.liveNoteOff(note)
            _commands.tryEmit(MidiCommand.Note(channel, note, 0f, false))
        }

        override fun controlChange(channel: Int, controller: Int, value: Int) {
            show("Bouton $controller : $value")
            if (controller == CC_ALL_NOTES_OFF || controller == CC_ALL_SOUND_OFF) {
                synchronized(held) {
                    if (NativeEngine.isAvailable) held.forEach { NativeEngine.liveNoteOff(it) }
                    held.clear()
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
                _state.value = _state.value.copy(clockBpm = rounded, last = "Horloge ${"%.1f".format(rounded)} BPM")
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
        _state.value = _state.value.copy(last = text)
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
        private val NAMES = listOf("Do", "Do#", "Ré", "Ré#", "Mi", "Fa", "Fa#", "Sol", "Sol#", "La", "La#", "Si")
        fun noteLabel(note: Int) = "${NAMES[note % 12]} ${note / 12 - 1}"
    }
}
