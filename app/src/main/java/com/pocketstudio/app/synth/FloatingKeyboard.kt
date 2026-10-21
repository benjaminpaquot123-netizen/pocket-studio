package com.pocketstudio.app.synth

import com.pocketstudio.model.i18n.tr

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.settings.KeyLayout
import com.pocketstudio.model.Harmony
import com.pocketstudio.ui.Neon

/**
 * The keyboard that follows: when the real one is out of reach while sounds are being chosen, a
 * compact one slides up from the bottom (a spring, a fade, a shadow) so a sound is heard without
 * scrolling back. Its handle folds it into a small "Clavier" button; dragging it down does too.
 */
@Composable
internal fun FloatingKeyboard(
    visible: Boolean,
    folded: Boolean,
    onFold: (Boolean) -> Unit,
    octave: Int,
    octaves: Int,
    keysHeight: Dp,
    actions: SynthActions,
    harmony: Harmony,
    chordPads: Boolean,
    keyOn: (Int, Float) -> Unit,
    keyOff: (Int) -> Unit,
    modifier: Modifier = Modifier,
    keyLayout: KeyLayout = KeyLayout.TwoOctaves,
    english: Boolean = false,
) {
    val shape = RoundedCornerShape(22.dp)
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.BottomEnd) {
        AnimatedVisibility(
            visible = visible && !folded,
            enter = slideInVertically(spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)) { it } +
                fadeIn(tween(200)) +
                scaleIn(spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow), initialScale = 0.94f, transformOrigin = TransformOrigin(0.5f, 1f)),
            exit = slideOutVertically(tween(200, easing = FastOutLinearInEasing)) { it } + fadeOut(tween(160)),
        ) {
            Column(
                Modifier
                    .padding(horizontal = 6.dp, vertical = 6.dp)
                    .shadow(20.dp, shape, ambientColor = Neon.Cyan.copy(alpha = 0.35f), spotColor = Neon.Cyan.copy(alpha = 0.35f))
                    .clip(shape)
                    .background(Brush.verticalGradient(listOf(Neon.SurfaceHigh, Neon.Surface)))
                    .border(1.dp, Neon.Outline, shape)
                    .testTag("float_keyboard")
                    .padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
            ) {
                // A firm drag down on the header (not on the keys) folds it, like a bottom sheet.
                val dragToFold = Modifier.pointerInput(Unit) {
                    var dragged = 0f
                    detectVerticalDragGestures(
                        onDragStart = { dragged = 0f },
                        onVerticalDrag = { _, dy -> dragged += dy },
                        onDragEnd = { if (dragged > 48.dp.toPx()) onFold(true) },
                    )
                }
                Box(Modifier.fillMaxWidth().then(dragToFold).padding(top = 6.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Neon.TextSecondary.copy(alpha = 0.5f)))
                }
                Row(dragToFold, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Piano, contentDescription = null, tint = Neon.Cyan, modifier = Modifier.size(18.dp))
                    Text(
                        if (chordPads) tr("  Pads d'accords") else tr("  Clavier"),
                        style = MaterialTheme.typography.labelLarge, color = Neon.TextPrimary, modifier = Modifier.weight(1f),
                    )
                    OctaveControl(octave, actions)
                    IconButton(onClick = { onFold(true) }, modifier = Modifier.testTag("float_keyboard_fold")) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = tr("Ranger le clavier"))
                    }
                }
                if (chordPads) ChordPads(harmony, 12 * (octave + 1), actions.noteOn, actions.noteOff, height = keysHeight)
                else PlayKeys(keyLayout, octave, octaves, harmony, english, keyOn, keyOff, keysHeight, tag = "float_keyboard_keys", keyTag = "float_key")
            }
        }
        AnimatedVisibility(
            visible = visible && folded,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) + fadeIn(tween(160)),
            exit = scaleOut(tween(140)) + fadeOut(tween(140)),
        ) {
            val pill = RoundedCornerShape(50)
            Row(
                Modifier
                    .padding(12.dp)
                    .shadow(12.dp, pill, ambientColor = Neon.Cyan, spotColor = Neon.Cyan)
                    .clip(pill)
                    .background(Neon.Cyan)
                    .clickable(role = Role.Button) { onFold(false) }
                    .testTag("float_keyboard_open")
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.Piano, contentDescription = null, tint = Neon.OnAccent, modifier = Modifier.size(18.dp))
                Text(tr("Clavier"), style = MaterialTheme.typography.labelLarge, color = Neon.OnAccent)
            }
        }
    }
}
