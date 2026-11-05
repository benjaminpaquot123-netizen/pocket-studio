package com.pocketstudio.app.ui

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.takeWhole
import com.pocketstudio.ui.Neon

/**
 * A name to type over (a project, a track, a part): Audit ultra round 1, n° 26 (seats E b, F d).
 * - The name is selected whenever the field takes the focus, so what is typed replaces it (« Audio 1Chitarra »).
 * - Its limit is said before anything is cut: « 12/40 » under it, then « 40 caractères au plus » once reached
 *   (« rythme » became « rythm » without a word).
 */
@Composable
fun NameField(
    name: String,
    onName: (String) -> Unit,
    max: Int,
    label: String,
    modifier: Modifier = Modifier,
    onDone: (() -> Unit)? = null,
) {
    var field by remember { mutableStateOf(TextFieldValue(name, TextRange(0, name.length))) }
    if (field.text != name) field = TextFieldValue(name, TextRange(name.length))
    // The focus just came (a touch on the field): the touch's own cursor does not undo the selection.
    var focusing by remember { mutableStateOf(false) }
    var full by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = field,
        onValueChange = { v ->
            // The touch's cursor only (relecture tour 1, mineur: a CJK or Korean keyboard's first
            // composition, same text, was swallowed with it).
            if (focusing && v.text == field.text && v.composition == null) {
                focusing = false
                return@OutlinedTextField
            }
            focusing = false
            full = v.text.length >= max
            // Cut on whole letters (an emoji's pair, a letter and its sign never split).
            val kept = if (v.text.length > max) v.text.takeWhole(max).let { TextFieldValue(it, TextRange(it.length)) } else v
            field = kept
            if (kept.text != name) onName(kept.text)
        },
        singleLine = true,
        label = { Text(label) },
        supportingText = {
            Text(
                if (full) tr("%d caractères au plus", max) else "${field.text.length}/$max",
                color = if (full) Neon.Amber else Neon.TextSecondary,
            )
        },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = if (onDone != null) ImeAction.Done else ImeAction.Default),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        modifier = modifier.onFocusChanged { f ->
            if (f.isFocused) {
                field = field.copy(selection = TextRange(0, field.text.length))
                focusing = true
            }
        },
    )
}

/**
 * Audit ultra round 2, n° 26 (seat B c « Nom du son », back from round 1): every field that holds a
 * name or a number to type over (a kept sound, my sound, the song to share, a part's bars) is this
 * one: what it holds is selected when it takes the focus, so what is typed replaces it
 * (« Mon son 1Pluie » before). [value] in, the text typed out ([onValueChange], free to cut it).
 */
@Composable
fun SelectAllTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    supportingText: (@Composable () -> Unit)? = null,
) {
    var field by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    if (field.text != value) field = TextFieldValue(value, TextRange(value.length))
    // The focus just came (a touch on the field): the touch's own cursor does not undo the selection.
    var focusing by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = field,
        onValueChange = { v ->
            // The touch's cursor only (a CJK or Korean keyboard's first composition, same text, goes through).
            if (focusing && v.text == field.text && v.composition == null) {
                focusing = false
                return@OutlinedTextField
            }
            focusing = false
            field = v
            if (v.text != value) onValueChange(v.text)
        },
        enabled = enabled,
        singleLine = singleLine,
        label = label,
        supportingText = supportingText,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier.onFocusChanged { f ->
            if (f.isFocused) {
                field = field.copy(selection = TextRange(0, field.text.length))
                focusing = true
            }
        },
    )
}
