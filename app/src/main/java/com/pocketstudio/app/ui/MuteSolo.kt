package com.pocketstudio.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon

/**
 * The one mute / solo button of the app (Benjamin's list n° 21, « M et S pas intuitifs »): a speaker
 * (sound on) or a crossed speaker (muted) to cut a track, headphones to hear it alone; never the
 * letters M and S. TalkBack says what it does and whether it is on; in Mode simple a short word
 * sits under the icon. Remix, stems board, Mix, Structure, the Live headers: all of them use it
 * (MuteSoloEverywhereTest).
 */
object MuteSolo {
    /** The icon of a mute button: the crossed speaker while the track is silent. */
    fun muteIcon(muted: Boolean): ImageVector = if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp

    val soloIcon: ImageVector get() = Icons.Filled.Headphones

    /** The Mode simple's word under the icon. */
    fun muteCaption(muted: Boolean): String = if (muted) tr("Muet") else tr("Son")

    fun soloCaption(): String = tr("Seule")
}

/** Whether the Mode simple's words show under the icons (read from the settings; tests may force it). */
@Composable
private fun captions(caption: Boolean?): Boolean {
    if (caption != null) return caption
    val settings by com.pocketstudio.app.settings.SettingsStore.state.collectAsState()
    return settings.simpleMode
}

/**
 * Cut track [name] or give it back: [muted] lights it ([color], the crossed speaker). [size] is the
 * button's own (Compose widens the touch to 48 dp around it). For TalkBack it is the switch « Son de
 * [name] », on while the track sounds (audit ultra round 2, n° 19, seat B b: « Son », « désactivé »
 * was read while the track played); what a touch does is its action's words.
 */
@Composable
fun MuteToggle(
    muted: Boolean,
    name: String,
    tag: String,
    modifier: Modifier = Modifier,
    color: Color = Neon.Amber,
    size: DpSize = DpSize(36.dp, 36.dp),
    caption: Boolean? = null,
    description: String? = null,
    toggle: () -> Unit,
) = IconToggle(
    on = muted, icon = MuteSolo.muteIcon(muted), color = color, size = size, tag = tag, modifier = modifier,
    description = description ?: tr("Son de %s", name),
    caption = if (captions(caption)) MuteSolo.muteCaption(muted) else null, toggle = toggle,
    checked = !muted, action = if (muted) tr("Remettre le son de %s", name) else tr("Couper le son de %s", name),
)

/** Hear track [name] alone ([solo] lights it, headphones), or every track again. */
@Composable
fun SoloToggle(
    solo: Boolean,
    name: String,
    tag: String,
    modifier: Modifier = Modifier,
    color: Color = Neon.Cyan,
    size: DpSize = DpSize(36.dp, 36.dp),
    caption: Boolean? = null,
    toggle: () -> Unit,
) = IconToggle(
    on = solo, icon = MuteSolo.soloIcon, color = color, size = size, tag = tag, modifier = modifier,
    description = if (solo) tr("Réécouter toutes les pistes") else tr("Écouter la piste %s seule", name),
    caption = if (captions(caption)) MuteSolo.soloCaption() else null, toggle = toggle,
)

/**
 * [on]: lit; [checked]: the switch's state for TalkBack (a mute switch is « Son », on while the track
 * sounds, so not lit); [action]: what a touch does, said by TalkBack (« Couper le son de Voix »).
 */
@Composable
private fun IconToggle(
    on: Boolean, icon: ImageVector, color: Color, size: DpSize, tag: String, modifier: Modifier,
    description: String, caption: String?, toggle: () -> Unit, checked: Boolean = on, action: String? = null,
) {
    Column(
        modifier.clickable(role = Role.Switch, onClickLabel = action, onClick = toggle)
            .semantics(mergeDescendants = true) {
                contentDescription = description
                toggleableState = ToggleableState(checked)
                stateDescription = if (checked) tr("activé") else tr("désactivé")
            }
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Box(
            Modifier.size(size).clip(RoundedCornerShape(8.dp)).background(if (on) color else Neon.SurfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = if (on) Color.Black else Neon.TextSecondary,
                modifier = Modifier.size(minOf(size.width, size.height) * 0.62f))
        }
        if (caption != null) {
            Text(caption, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, lineHeight = 11.sp, color = if (on) color else Neon.TextSecondary,
                maxLines = 1, textAlign = TextAlign.Center)
        }
    }
}
