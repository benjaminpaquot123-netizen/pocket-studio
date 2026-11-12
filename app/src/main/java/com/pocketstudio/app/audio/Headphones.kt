package com.pocketstudio.app.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the music is heard on: the phone's speaker, wired (or USB) headphones, or Bluetooth. */
enum class Listening { Speaker, Wired, Bluetooth }

/** Follows the headphones plugged in or paired, for the advice shown when recording. */
object Headphones {
    private val _state = MutableStateFlow(Listening.Speaker)
    val state: StateFlow<Listening> = _state.asStateFlow()
    private var watching = false
    private var read: (() -> Unit)? = null

    /** The outputs read again now (the engine saw its output reopen before the device callback came). */
    fun refresh() {
        read?.invoke()
    }

    private val BLUETOOTH = setOf(
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_HEARING_AID,
        AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLE_SPEAKER, AudioDeviceInfo.TYPE_BLE_BROADCAST,
    )
    private val WIRED = setOf(
        AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_ACCESSORY, AudioDeviceInfo.TYPE_LINE_ANALOG, AudioDeviceInfo.TYPE_LINE_DIGITAL,
    )

    /** The outputs of [types] (AudioDeviceInfo.TYPE_*): Bluetooth takes the music when paired (Android's own order). */
    fun classify(types: Collection<Int>): Listening = when {
        types.any { it in BLUETOOTH } -> Listening.Bluetooth
        types.any { it in WIRED } -> Listening.Wired
        else -> Listening.Speaker
    }

    /** Starts following the outputs (once, for the whole app). */
    fun watch(context: Context) {
        if (watching) return
        val audio = context.getSystemService(AudioManager::class.java) ?: return
        watching = true
        fun update() {
            _state.value = classify(audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).map { it.type })
        }
        read = ::update
        // A device change also wakes the engine's revival when the sound was cut and could not
        // come back yet (audit 13 M3): a headset that took seconds to connect is heard at once.
        audio.registerAudioDeviceCallback(object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                update()
                com.pocketstudio.engine.NativeEngine.retryOutput()
            }
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                update()
                com.pocketstudio.engine.NativeEngine.retryOutput()
            }
        }, null)
        update()
    }
}
