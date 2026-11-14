package com.pocketstudio.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * What TalkBack calls a switch or a slider whose words are beside it, not in it (audit ultra round 1,
 * n° 30b and 39: « interrupteur sans nom » next to « Utiliser cette ligne »): its own name, so that it
 * reads « Utiliser cette ligne, interrupteur, activé » and not « interrupteur, activé ».
 */
fun Modifier.named(name: String): Modifier = semantics { contentDescription = name }
