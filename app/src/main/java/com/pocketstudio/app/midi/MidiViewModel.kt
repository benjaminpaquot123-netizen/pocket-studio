package com.pocketstudio.app.midi

import com.pocketstudio.model.i18n.tr
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
    /** The last thing received, in words ("Note Do4", "Horloge 124 BPM"). */
    val last: String? = null,
    val followClock: Boolean = true,
    val clockBpm: Double? = null,
    val scanning: Boolean = false,
    /** Send our tempo, start and stop to the connected device. */
    val sendClock: Boolean = false,
    /** The controller connected: its name and the profile the app found for it (null: none connected). */
    val controller: ControllerInfo? = null,
    /** « Apprendre mes pads »: the pads heard so far (null: not learning). */
    val learningPads: Int? = null,
)

/** The controller connected: [name] (the settings know it by it), its [kind] and the app's [profile] for it (null: generic). */
data class ControllerInfo(val name: String, val kind: String, val profile: com.pocketstudio.model.midi.ControllerProfile?)

/** What a MIDI device asks of the project (the tempo and the transport are the project's). */
sealed interface MidiCommand {
    data object Start : MidiCommand
    /** MIDI Continue: the other machine carries on where it stopped (not from the top). */
    data object Continue : MidiCommand
    data object Stop : MidiCommand
    data class Tempo(val bpm: Double) : MidiCommand
    /**
     * Lot B2: a controller's pad (as [Pad]) let go while the pads do not play the drums (Pad Live: a
     * « tenir » pad stops).
     */
    data class PadUp(val index: Int) : MidiCommand
    /** A note received (it already sounds): for live recording. Channel 10 (9) = pads. */
    data class Note(val channel: Int, val pitch: Int, val velocity: Float, val on: Boolean) : MidiCommand
    /** A knob or fader moved (MIDI learn, mapped controls). */
    data class Control(val channel: Int, val controller: Int, val value: Int) : MidiCommand
    /**
     * A pad of the controller (0..15, bank B from 8) hit while the pads do not play the drums (the
     * Live page: launches, mutes, live pads). The drums are played from the MIDI thread instead.
     */
    data class Pad(val index: Int, val velocity: Float) : MidiCommand
    /** « Apprendre mes pads »: a note heard (it plays nothing meanwhile). */
    data class PadLearnt(val packed: Int) : MidiCommand
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

    // Revue neuve (MOYEN 5): the monitor apart from [state] (a pitch bend wheel sends hundreds of
    // messages a second), shown at most every [MONITOR_MS], read by « Contrôleur MIDI » alone.
    private val monitorLines = ArrayDeque<String>()
    private val monitorPosted = java.util.concurrent.atomic.AtomicBoolean(false)
    private val _monitor = MutableStateFlow<List<String>>(emptyList())
    /** The last messages received, newest first (« Contrôleur MIDI »'s monitor). */
    val monitor: StateFlow<List<String>> = _monitor.asStateFlow()
    private val publishMonitor = Runnable {
        monitorPosted.set(false)
        _monitor.value = synchronized(monitorLines) { monitorLines.toList() }
    }

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

    /**
     * Which notes are pads, and whether pads play the drums at once from the MIDI thread (every
     * screen but the Live page, which takes them as launches). Set from the main thread, read on the
     * MIDI thread: immutable snapshots behind @Volatile.
     */
    @Volatile var pads: PadMap = PadMap()
    @Volatile var padsToDrums: Boolean = true

    /**
     * n° 9: an Acid 303 cell of the Live records: the keys play the 303 (the app sounds them and
     * writes them into the cell) instead of the synth. Set from the main thread.
     */
    @Volatile var keysToAcid: Boolean = false
    /** Keys down that went to the 303 ([keysToAcid] when pressed): their release goes there too. Under held's lock. */
    private val acidOn = BooleanArray(128)
    @Volatile private var padLearning: Boolean = false

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
        else -> tr("Appli")
    }

    private fun nameOf(info: MidiDeviceInfo): String {
        val p = info.properties
        return p.getString(MidiDeviceInfo.PROPERTY_NAME) ?: p.getString(MidiDeviceInfo.PROPERTY_PRODUCT) ?: tr("Appareil MIDI")
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
    private fun BluetoothDevice.safeName(): String = runCatching { name }.getOrNull() ?: tr("Clavier Bluetooth")

    fun connect(id: String) {
        val m = manager ?: return
        disconnect()
        val attempt = ++connection
        val opened = { d: MidiDevice? ->
            if (attempt != connection) {
                d?.close()  // another device was chosen meanwhile
            } else if (d == null) {
                AppLog.add("MIDI : ouverture impossible")
                _state.update { it.copy(last = tr("Connexion impossible")) }
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
            _state.update { it.copy(last = tr("Cet appareil n'envoie rien")) }
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
        // The kind only: a device's name can be its owner's (« Clavier de Benjamin »).
        val kind = when (d.info.type) {
            MidiDeviceInfo.TYPE_USB -> "USB"
            MidiDeviceInfo.TYPE_BLUETOOTH -> "Bluetooth"
            MidiDeviceInfo.TYPE_VIRTUAL -> "virtuel"
            else -> "autre"
        }
        AppLog.add("MIDI connecté : appareil $kind")
        val info = deviceIdOf(d.info)
        val profile = com.pocketstudio.model.midi.Controllers.detect(info)
        if (profile != null) AppLog.add("MIDI : profil ${profile.id}")
        val name = nameOf(d.info)
        pads = PadMap(profile)  // the user's own pads come from the settings (the app sets them)
        _state.update { it.copy(connected = id, last = null, clockBpm = null, controller = ControllerInfo(name, kindOf(d.info), profile)) }
        synchronized(monitorLines) { monitorLines.clear() }
        _monitor.value = emptyList()
        greet(profile, name)
        refresh(autoConnect = false)
        if (_state.value.sendClock) startClock()
    }

    fun disconnect() {
        ++connection
        stopClock()
        runCatching { sendPort?.close() }
        sendPort = null
        runCatching { port?.close() }
        runCatching { device?.close() }
        port = null
        device = null
        releaseHeld()
        padLearning = false
        _state.update { it.copy(connected = null, clockBpm = null, controller = null, learningPads = null) }
    }

    /**
     * Every key still down let go (unplugged, « all notes off »): silent in the engine, and ended for
     * the app too (step 4f: a cell recording or a Live take never keeps a note ringing to its end).
     */
    private fun releaseHeld() {
        val ended = synchronized(held) {
            if (NativeEngine.isAvailable) held.forEach { NativeEngine.liveNoteOff(it, trackOf[it]) }
            val notes = held.toList() + acidOn.indices.filter { acidOn[it] }.map { playedAs[it].takeIf { p -> p >= 0 } ?: it }
            held.clear()
            acidOn.fill(false)
            playedAs.fill(-1)
            keysOn.fill(0)
            notes
        }
        ended.forEach { _commands.tryEmit(MidiCommand.Note(0, it, 0f, false)) }
    }

    /** « MiniLab 3 connecté — prêt à jouer »: a short toast once a controller is ready. */
    private fun greet(profile: com.pocketstudio.model.midi.ControllerProfile?, name: String) {
        val text = greeting(profile, name)
        _state.update { it.copy(last = text) }
        runCatching { android.widget.Toast.makeText(getApplication(), text, android.widget.Toast.LENGTH_SHORT).show() }
    }

    private fun deviceIdOf(info: MidiDeviceInfo): com.pocketstudio.model.midi.DeviceId {
        val p = info.properties
        val usb = runCatching {
            @Suppress("DEPRECATION")
            p.getParcelable<android.hardware.usb.UsbDevice>(MidiDeviceInfo.PROPERTY_USB_DEVICE)
        }.getOrNull()
        return com.pocketstudio.model.midi.DeviceId(
            name = nameOf(info), manufacturer = p.getString(MidiDeviceInfo.PROPERTY_MANUFACTURER), product = p.getString(MidiDeviceInfo.PROPERTY_PRODUCT),
            vendorId = usb?.vendorId ?: -1, productId = usb?.productId ?: -1,
        )
    }

    /** « Apprendre mes pads »: the notes heard next become pads 1, 2… (they play nothing meanwhile). */
    fun learnPads(on: Boolean) {
        padLearning = on
        _state.update { it.copy(learningPads = if (on) 0 else null) }
    }

    /** One more pad learnt (from the main thread): [count] so far. */
    fun padsLearnt(count: Int) {
        _state.update { it.copy(learningPads = if (padLearning) count else null) }
    }

    /** A message for the monitor, newest first, the last [MONITOR] kept. */
    private fun monitor(text: String) {
        synchronized(monitorLines) {
            monitorLines.addFirst(text)
            while (monitorLines.size > MONITOR) monitorLines.removeLast()
        }
        if (monitorPosted.compareAndSet(false, true)) main.postDelayed(publishMonitor, MONITOR_MS)
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
        if (_state.value.scanning) return  // already looking: a second tap must not start a second scan
        val scanner = getApplication<Application>().getSystemService(BluetoothManager::class.java)?.adapter?.bluetoothLeScanner
        if (scanner == null) {
            _state.update { it.copy(last = tr("Bluetooth éteint")) }
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
        activeScanner = scanner
        viewModelScope.launch {
            kotlinx.coroutines.delay(10_000)
            stopScan()
            _state.update { it.copy(scanning = false) }
        }
    }

    /** The scanner of a search under way: stopped after ten seconds, or when this ViewModel goes. */
    private var activeScanner: android.bluetooth.le.BluetoothLeScanner? = null

    @SuppressLint("MissingPermission")
    private fun stopScan() {
        activeScanner?.let { runCatching { it.stopScan(scanCallback) } }
        activeScanner = null
    }

    // --- Incoming events (MIDI thread) -----------------------------------------------------------

    // --- Clock out (lot B2): the engine counts it on the beats' frames ([MidiClockOut]) ---------

    private var clockJob: kotlinx.coroutines.Job? = null

    private var playing = false

    /** Tests: where the clock goes instead of a device (status, time). */
    @androidx.annotation.VisibleForTesting
    internal var clockSentForTest: ((Int, Long) -> Unit)? = null
    /** Tests: the engine's clock queue in place of [NativeEngine.drainMidiClock]. */
    internal var clockDrainForTest: ((LongArray) -> Int)? = null
    /** The clock just switched off, still sending the engine's last messages (its Stop). */
    private var clockDraining: kotlinx.coroutines.Job? = null

    fun setSendClock(on: Boolean) {
        // Following and sending at once with the same device would loop the clock back.
        _state.update { it.copy(sendClock = on, followClock = if (on) false else it.followClock) }
        if (on) startClock() else stopClock()
    }

    /** The transport started or stopped, or the tempo changed (from the main thread): the engine sends the clock with it. */
    fun transport(playing: Boolean, @Suppress("UNUSED_PARAMETER") bpm: Double) {
        this.playing = playing
        if (_state.value.sendClock && clockJob?.isActive != true) startClock()
    }

    private fun send(byte: Int, at: Long) {
        clockSentForTest?.let { it(byte, at); return }
        runCatching { sendPort?.send(byteArrayOf(byte.toByte()), 0, 1, at) }
    }

    private fun startClock() {
        if (clockJob?.isActive == true || (sendPort == null && clockSentForTest == null)) return
        NativeEngine.setMidiClockOut(true)
        val out = clockDrainForTest?.let { MidiClockOut(::send, it) } ?: MidiClockOut(::send)
        // The engine's clock queue has one reader: the clock switched off a moment ago ends first.
        val previous = clockJob ?: clockDraining
        clockDraining = null
        clockJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            try {
                // Waited for even when switched off meanwhile: this job ends after it, so the next one waits for both.
                previous?.let { kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { it.join() } }
                while (true) {
                    out.pump()
                    kotlinx.coroutines.delay(MidiClockOut.PUMP_MS)
                }
            } finally {
                // The engine's Stop (sent when the clock is switched off while playing) goes out too.
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                    kotlinx.coroutines.delay(MidiClockOut.PUMP_MS * 4)
                    out.pump()
                }
            }
        }
    }

    private fun stopClock() {
        if (clockJob == null) return
        NativeEngine.setMidiClockOut(false)
        clockJob?.cancel()
        clockDraining = clockJob
        clockJob = null
    }

    private val listener = object : MidiListener {
        override fun noteOn(channel: Int, note: Int, velocity: Int) {
            if (padLearning) {
                _commands.tryEmit(MidiCommand.PadLearnt(com.pocketstudio.model.midi.Controllers.packPad(channel, note)))
                monitor(tr("Note %1\$s · canal %2\$d", noteLabel(note), channel + 1))
                return
            }
            val v = velocity / 127f
            val pad = pads.padOf(channel, note)
            monitor(
                if (pad != null) tr("Pad %1\$d · note %2\$s · canal %3\$d", pad + 1, noteLabel(note), channel + 1)
                else tr("Note %1\$s · force %2\$d · canal %3\$d", noteLabel(note), velocity, channel + 1),
            )
            if (pad != null) {
                if (!padsToDrums) {
                    _commands.tryEmit(MidiCommand.Pad(pad, v))
                } else {
                    if (NativeEngine.isAvailable && pad < NativeEngine.LANES) NativeEngine.liveTriggerPad(pad, v)
                    _commands.tryEmit(MidiCommand.Note(DRUM_CHANNEL, FIRST_PAD_NOTE + pad, v, true))
                }
                show(tr("Note %s (pads)", noteLabel(note)))
                return
            }
            if (channel == DRUM_CHANNEL) {
                // Revue neuve (MOYEN 6): a channel 10 note none of the pads has (GM kick 35, crashes,
                // pads set to another bank) plays and records as before profiles: a drum note.
                if (NativeEngine.isAvailable && note in FIRST_PAD_NOTE until FIRST_PAD_NOTE + NativeEngine.LANES) {
                    NativeEngine.liveTriggerPad(note - FIRST_PAD_NOTE, v)
                }
                _commands.tryEmit(MidiCommand.Note(channel, note, v, true))
                show(tr("Note %s (pads)", noteLabel(note)))
                return
            }
            if (!NativeEngine.isAvailable) return
            run {
                val h = harmony
                val played = if (h.lock) h.snap(note).coerceIn(0, 127) else note
                synchronized(held) {
                    if (note in playedAs.indices) playedAs[note] = played
                    if (keysToAcid && note in acidOn.indices) {
                        // The app sounds it on the 303 (RhythmViewModel.midiNote): not on the synth.
                        acidOn[note] = true
                    } else {
                        held += played
                        if (keysOn[played]++ == 0) trackOf[played] = NativeEngine.liveTrack
                        NativeEngine.liveNoteOn(played, v, trackOf[played])
                    }
                }
                _commands.tryEmit(MidiCommand.Note(channel, played, v, true))
            }
            show(tr("Note %s", noteLabel(note)))
        }

        override fun noteOff(channel: Int, note: Int) {
            if (padLearning) return
            // Pad Live: a pad let go (a « tenir » pad stops); the drums ignore it.
            val pad = pads.padOf(channel, note)
            if (pad != null && !padsToDrums) {
                _commands.tryEmit(MidiCommand.PadUp(pad))
                return
            }
            if (!NativeEngine.isAvailable || channel == DRUM_CHANNEL || pad != null) return
            val played = synchronized(held) {
                val p = playedAs.getOrNull(note)?.takeIf { it >= 0 } ?: note
                if (note in playedAs.indices) playedAs[note] = -1
                if (acidOn.getOrElse(note) { false }) {
                    // A 303 key: the app lets it go there.
                    acidOn[note] = false
                } else {
                    if (p in keysOn.indices) keysOn[p] = (keysOn[p] - 1).coerceAtLeast(0)
                    // Another key still holds the same note: it keeps sounding.
                    if (keysOn.getOrElse(p) { 0 } == 0) {
                        held -= p
                        NativeEngine.liveNoteOff(p, trackOf.getOrElse(p) { NativeEngine.liveTrack })
                    }
                }
                p
            }
            _commands.tryEmit(MidiCommand.Note(channel, played, 0f, false))
        }

        override fun pitchBend(channel: Int, value: Int) {
            monitor(tr("Pitch bend %1\$d · canal %2\$d", value, channel + 1))
            if (NativeEngine.isAvailable && channel != DRUM_CHANNEL) NativeEngine.liveExpression(NativeEngine.EXPR_BEND, value / 8192f * BEND_RANGE)
        }

        override fun controlChange(channel: Int, controller: Int, value: Int) {
            show(tr("Bouton %d : %d", controller, value))
            monitor(tr("CC %1\$d = %2\$d · canal %3\$d", controller, value, channel + 1))
            if (NativeEngine.isAvailable && channel != DRUM_CHANNEL) {
                when (controller) {
                    CC_MOD_WHEEL -> NativeEngine.liveExpression(NativeEngine.EXPR_MOD, value / 127f)
                    CC_SUSTAIN -> NativeEngine.liveExpression(NativeEngine.EXPR_SUSTAIN, if (value >= 64) 1f else 0f)
                }
            }
            _commands.tryEmit(MidiCommand.Control(channel, controller, value))
            if (controller == CC_ALL_NOTES_OFF || controller == CC_ALL_SOUND_OFF) releaseHeld()
        }

        override fun clock() {
            val bpm = tempo.tick(System.nanoTime()) ?: return
            if (!_state.value.followClock) return
            // Round to 0.1 BPM and only send real changes: the clock jitters a little.
            val rounded = Math.round(bpm * 10) / 10.0
            if (kotlin.math.abs(rounded - lastTempoSent) >= 0.25) {
                lastTempoSent = rounded
                _commands.tryEmit(MidiCommand.Tempo(rounded))
                _state.update { it.copy(clockBpm = rounded, last = tr("Horloge %.1f BPM", rounded)) }
            }
        }

        override fun start() {
            tempo.reset()
            if (_state.value.followClock) _commands.tryEmit(MidiCommand.Start)
            show(tr("Départ"))
        }

        override fun continuePlayback() {
            tempo.reset()
            if (_state.value.followClock) _commands.tryEmit(MidiCommand.Continue)
            show(tr("Continuer"))
        }

        override fun stop() {
            if (_state.value.followClock) _commands.tryEmit(MidiCommand.Stop)
            show(tr("Arrêt"))
        }
    }

    /** Tests: MIDI bytes as if the connected controller had sent them (named [name]). */
    @androidx.annotation.VisibleForTesting
    internal fun receiveForTest(bytes: ByteArray, name: String = "Arturia MiniLab 3") {
        if (_state.value.controller?.name != name) {
            val profile = com.pocketstudio.model.midi.Controllers.detect(name)
            pads = PadMap(profile, pads.learnt)
            _state.update { it.copy(controller = ControllerInfo(name, "USB", profile)) }
        }
        testParser.feed(bytes)
    }

    private val testParser by lazy { MidiParser(listener) }

    private fun show(text: String) {
        _state.update { it.copy(last = text) }
    }

    override fun onCleared() {
        main.removeCallbacks(publishMonitor)
        // Another project opened within the ten seconds: the timer is cancelled with the scope,
        // so the search (battery, and this ViewModel kept by its callback) stops here.
        stopScan()
        manager?.unregisterDeviceCallback(callback)
        disconnect()
    }

    companion object {
        /**
         * The toast once a controller is ready: the model the app knows it as, else the device's own name
         * (Audit stage 2, Benjamin 2026-11-03: the name shown is always the real one, never « Clavier MIDI »
         * for a keyboard the phone names), « Clavier MIDI » only when the phone gives no name.
         */
        fun greeting(profile: com.pocketstudio.model.midi.ControllerProfile?, name: String?): String = when {
            profile != null -> tr("%s connecté — prêt à jouer", profile.name.removePrefix("Arturia "))
            !name.isNullOrBlank() && name != tr("Appareil MIDI") -> tr("%s connecté — prêt à jouer", name.trim())
            else -> tr("Clavier MIDI connecté — prêt à jouer")
        }

        const val DRUM_CHANNEL = 9
        /** Lines kept in the monitor. */
        const val MONITOR = 10
        /** The monitor shown at most this often (ms). */
        const val MONITOR_MS = 100L
        const val FIRST_PAD_NOTE = 36  // General MIDI kick: pad 1
        const val CC_ALL_SOUND_OFF = 120
        const val CC_ALL_NOTES_OFF = 123
        const val CC_MOD_WHEEL = 1
        const val CC_SUSTAIN = 64
        /** Pitch bend wheel range in semitones. */
        const val BEND_RANGE = 2f
        /** "Do3" for MIDI 60, or "C4" when the settings name notes C D E. */
        fun noteLabel(note: Int): String =
            com.pocketstudio.model.NoteNames.withOctave(note, com.pocketstudio.app.settings.SettingsStore.state.value.englishNotes)
    }
}

/** Which notes of the connected controller are pads: its profile's, or those « Apprendre mes pads » heard. */
class PadMap(val profile: com.pocketstudio.model.midi.ControllerProfile? = null, val learnt: List<Int>? = null) {
    fun padOf(channel: Int, note: Int): Int? = com.pocketstudio.model.midi.Controllers.padOf(channel, note, profile, learnt)
}
