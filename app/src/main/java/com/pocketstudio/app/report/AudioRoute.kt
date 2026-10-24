package com.pocketstudio.app.report

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import com.pocketstudio.app.audio.Listening

/**
 * Where the sound goes, for report.md (in French): the output Android routes music to and the
 * outputs connected. Only the kinds of device are said, never their names (a Bluetooth headset is
 * often called after its owner).
 */
object AudioRoute {
    /** A device kind (AudioDeviceInfo.TYPE_*), in French. */
    fun typeName(type: Int): String = when (type) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE -> "haut-parleur du téléphone"
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "écouteur d'appel"
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "casque filaire"
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "casque filaire avec micro"
        AudioDeviceInfo.TYPE_USB_HEADSET -> "casque USB"
        AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_ACCESSORY, AudioDeviceInfo.TYPE_DOCK -> "appareil USB"
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth (A2DP)"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth (appel, SCO)"
        AudioDeviceInfo.TYPE_BLE_HEADSET -> "Bluetooth LE (casque)"
        AudioDeviceInfo.TYPE_BLE_SPEAKER -> "Bluetooth LE (enceinte)"
        AudioDeviceInfo.TYPE_BLE_BROADCAST -> "Bluetooth LE (diffusion)"
        AudioDeviceInfo.TYPE_HEARING_AID -> "appareil auditif"
        AudioDeviceInfo.TYPE_HDMI, AudioDeviceInfo.TYPE_HDMI_ARC, AudioDeviceInfo.TYPE_HDMI_EARC -> "HDMI"
        AudioDeviceInfo.TYPE_LINE_ANALOG, AudioDeviceInfo.TYPE_LINE_DIGITAL, AudioDeviceInfo.TYPE_AUX_LINE -> "sortie ligne"
        AudioDeviceInfo.TYPE_TELEPHONY -> "téléphonie"
        AudioDeviceInfo.TYPE_REMOTE_SUBMIX -> "capture interne"
        else -> "appareil de type $type"
    }

    /** What the music is heard on, in one word (the app's own reading, [Listening]). */
    fun listeningName(l: Listening): String = when (l) {
        Listening.Speaker -> "haut-parleur"
        Listening.Wired -> "casque filaire"
        Listening.Bluetooth -> "Bluetooth"
    }

    /**
     * « Bluetooth (A2DP) (branchés : haut-parleur du téléphone, Bluetooth (A2DP)) »: [routed], the
     * outputs music goes to (empty: unknown), then the [connected] ones; null when nothing is known.
     */
    fun describe(routed: List<Int>, connected: List<Int>): String? {
        val now = routed.map(::typeName).distinct()
        val all = connected.map(::typeName).distinct()
        return when {
            now.isEmpty() && all.isEmpty() -> null
            now.isEmpty() -> "inconnue (branchés : ${all.joinToString(", ")})"
            all.isEmpty() || all == now -> now.joinToString(" + ")
            else -> now.joinToString(" + ") + " (branchés : ${all.joinToString(", ")})"
        }
    }

    /** Now, from Android (null: it would not say). */
    fun current(context: Context): String? = runCatching {
        val audio = context.getSystemService(AudioManager::class.java) ?: return null
        val connected = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).map { it.type }
        val routed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val music = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
            audio.getAudioDevicesForAttributes(music).map { it.type }
        } else {
            emptyList()
        }
        describe(routed, connected)
    }.getOrNull()
}
