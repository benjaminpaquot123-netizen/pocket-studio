package com.pocketstudio.app.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.LevelMeter
import com.pocketstudio.ui.Neon

/** Below this the microphone hears too little (about −26 dB): « Trop faible ». */
const val INPUT_QUIET = 0.05f

/** Above this the take risks clipping (the meter's red end): « Trop fort ». */
const val INPUT_LOUD = 0.9f

/** How the microphone's [level] (0..1) is, in a word: « Trop faible », « Bien », « Trop fort ». */
fun inputLevelWord(level: Float): String = when {
    level < INPUT_QUIET -> tr("Trop faible")
    level > INPUT_LOUD -> tr("Trop fort")
    else -> tr("Bien")
}

/**
 * Audit ultra round 2, n° 20 (seat B b, a guitarist on a tablet: « un trait vert très fin, sans
 * libellé ni nom TalkBack »): the microphone's level during a take or its count-in, named
 * (« Niveau du micro »), a thick bar with its red end shown, and a word for how it is, which
 * TalkBack reads as its state.
 */
@Composable
fun InputLevel(level: Float, modifier: Modifier = Modifier) {
    val word = inputLevelWord(level)
    val name = tr("Niveau du micro")
    Column(
        modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = name; stateDescription = word }.testTag("input_level"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.Mic, contentDescription = null, tint = Neon.TextSecondary, modifier = Modifier.size(18.dp))
            Text(name, style = MaterialTheme.typography.bodyMedium, color = Neon.TextPrimary, modifier = Modifier.weight(1f))
            Text(
                word, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                color = when {
                    level > INPUT_LOUD -> Neon.RedText
                    level < INPUT_QUIET -> Neon.Amber
                    else -> Neon.Acid
                },
            )
        }
        LevelMeter({ level }, thickness = 18.dp, warnZone = true)
    }
}
