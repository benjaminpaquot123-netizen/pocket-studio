package com.pocketstudio.app.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState

/** True when notes are named C D E (Réglages › Notation): a screen that reads it follows a change at once. */
@Composable
fun englishNotes(): Boolean = SettingsStore.state.collectAsState().value.englishNotes
