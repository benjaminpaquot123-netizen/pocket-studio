package com.pocketstudio.app.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Look of the app (see Palettes in ui-components). */
enum class AppTheme(val label: String) { Studio("Studio (gris et orange)"), Night("Nuit (plus sombre)"), Colorblind("Couleurs pour daltoniens") }

/** User preferences, the same for every project. */
data class AppSettings(
    val theme: AppTheme = AppTheme.Studio,
    /** Size of everything on screen: 0.85, 1, 1.15 or 1.3. */
    val uiScale: Float = 1f,
    /** Note names C D E instead of Do Ré Mi. */
    val englishNotes: Boolean = false,
    /** Light vibration on pads and long presses. */
    val haptics: Boolean = true,
    /** The screen stays on while the music plays. */
    val keepScreenOn: Boolean = true,
    /** Click volume 0..1 (0.5 = default). */
    val metronomeLevel: Float = 0.5f,
    /** Bars of count-in before a recording started while stopped (0 = none). */
    val recordCountIn: Int = 1,
    /** Output buffer in bursts: 2 fast, 4 safe, 6 very safe. */
    val bufferBursts: Int = 2,
    /** The first-steps guide was seen (it opens at the first launch). */
    val guideSeen: Boolean = false,
) {
    companion object {
        val UI_SCALES = listOf(0.85f, 1f, 1.15f, 1.3f)
        val BUFFERS = listOf(2 to "Rapide", 4 to "Sûr", 6 to "Très sûr")
    }
}

/** Reads and writes [AppSettings] (SharedPreferences "settings"); the app follows [state]. */
object SettingsStore {
    private const val PREFS = "settings"
    private val _state = MutableStateFlow(AppSettings())
    val state: StateFlow<AppSettings> = _state.asStateFlow()
    private var loaded = false

    fun load(context: Context): AppSettings {
        if (loaded) return _state.value
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // Older versions kept the note names elsewhere.
        val oldEnglish = context.getSharedPreferences("ui", Context.MODE_PRIVATE).getBoolean("notation_english", false)
        val d = AppSettings()
        _state.value = AppSettings(
            theme = runCatching { AppTheme.valueOf(p.getString("theme", d.theme.name)!!) }.getOrDefault(d.theme),
            uiScale = p.getFloat("uiScale", d.uiScale).takeIf { it in AppSettings.UI_SCALES } ?: d.uiScale,
            englishNotes = p.getBoolean("englishNotes", oldEnglish),
            haptics = p.getBoolean("haptics", d.haptics),
            keepScreenOn = p.getBoolean("keepScreenOn", d.keepScreenOn),
            metronomeLevel = p.getFloat("metronomeLevel", d.metronomeLevel).coerceIn(0f, 1f),
            recordCountIn = p.getInt("recordCountIn", d.recordCountIn).coerceIn(0, 2),
            bufferBursts = p.getInt("bufferBursts", d.bufferBursts).takeIf { b -> AppSettings.BUFFERS.any { it.first == b } } ?: d.bufferBursts,
            guideSeen = p.getBoolean("guideSeen", d.guideSeen),
        )
        loaded = true
        return _state.value
    }

    fun update(context: Context, transform: (AppSettings) -> AppSettings) {
        val s = transform(load(context))
        _state.value = s
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("theme", s.theme.name)
            .putFloat("uiScale", s.uiScale)
            .putBoolean("englishNotes", s.englishNotes)
            .putBoolean("haptics", s.haptics)
            .putBoolean("keepScreenOn", s.keepScreenOn)
            .putFloat("metronomeLevel", s.metronomeLevel)
            .putInt("recordCountIn", s.recordCountIn)
            .putInt("bufferBursts", s.bufferBursts)
            .putBoolean("guideSeen", s.guideSeen)
            .apply()
    }

    /** Back to the defaults (the guide is not shown again). */
    fun reset(context: Context) = update(context) { AppSettings(guideSeen = true) }
}
