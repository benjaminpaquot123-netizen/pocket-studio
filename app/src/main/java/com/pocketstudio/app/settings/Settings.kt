package com.pocketstudio.app.settings

import com.pocketstudio.model.i18n.tr
import android.content.Context
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Look of the app (see Palettes in ui-components). */
enum class AppTheme {
    Studio, Night, Colorblind;

    val label: String get() = when (this) {
        Studio -> tr("Studio (gris et orange)")
        Night -> tr("Nuit (plus sombre)")
        Colorblind -> tr("Couleurs pour daltoniens")
    }
}

/**
 * The synth keyboard: two octaves of piano, one wider octave with every note named, or only the
 * notes of the song's key as wide bars (GarageBand's "Scale", Push's "In key"): no wrong note.
 */
enum class KeyLayout {
    TwoOctaves, OneOctave, Scale;

    /** The chip's name when the full piano shows [wideOctaves] octaves (3 on a tablet or lying down). */
    fun label(wideOctaves: Int): String = when (this) {
        TwoOctaves -> if (wideOctaves == 2) tr("2 octaves") else tr("%d octaves", wideOctaves)
        OneOctave -> tr("1 octave")
        Scale -> tr("Gamme")
    }
}

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
    val keyLayout: KeyLayout = KeyLayout.TwoOctaves,
    /** Language of the interface (French unless chosen). */
    val language: Lang = Lang.FR,
    /** A light vibration on each beat while the Live page plays (off: it can annoy). */
    val liveBeatHaptics: Boolean = false,
    /** The ready-made effects starred in the effects box (their ids). */
    val favouriteFx: Set<String> = emptySet(),
) {
    companion object {
        val UI_SCALES = listOf(0.85f, 1f, 1.15f, 1.3f)
        val BUFFERS: List<Pair<Int, String>> get() = listOf(2 to tr("Rapide"), 4 to tr("Sûr"), 6 to tr("Très sûr"))
    }
}

/** Reads and writes [AppSettings] (SharedPreferences "settings"); the app follows [state]. */
object SettingsStore {
    private const val PREFS = "settings"
    private val _state = MutableStateFlow(AppSettings())
    val state: StateFlow<AppSettings> = _state.asStateFlow()
    private val _favouriteFx = MutableStateFlow<Set<String>>(emptySet())
    /** The starred ready-made effects alone (the effects box follows them). */
    val favouriteFx: StateFlow<Set<String>> = _favouriteFx.asStateFlow()
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
            keyLayout = runCatching { KeyLayout.valueOf(p.getString("keyLayout", d.keyLayout.name)!!) }.getOrDefault(d.keyLayout),
            language = Lang.of(p.getString("language", d.language.code)),
            liveBeatHaptics = p.getBoolean("liveBeatHaptics", d.liveBeatHaptics),
            favouriteFx = p.getStringSet("favouriteFx", d.favouriteFx)?.toSet() ?: d.favouriteFx,
        )
        I18n.lang = _state.value.language
        _favouriteFx.value = _state.value.favouriteFx
        loaded = true
        return _state.value
    }

    fun update(context: Context, transform: (AppSettings) -> AppSettings) {
        val s = transform(load(context))
        I18n.lang = s.language
        _state.value = s
        _favouriteFx.value = s.favouriteFx
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
            .putString("keyLayout", s.keyLayout.name)
            .putString("language", s.language.code)
            .putBoolean("liveBeatHaptics", s.liveBeatHaptics)
            // A copy: SharedPreferences must not keep the set it is given.
            .putStringSet("favouriteFx", HashSet(s.favouriteFx))
            .apply()
    }

    /** Ready-made effect [id] starred, or no longer. */
    fun toggleFavouriteFx(context: Context, id: String) =
        update(context) { s -> s.copy(favouriteFx = if (id in s.favouriteFx) s.favouriteFx - id else s.favouriteFx + id) }

    /** Tests: read the preferences again at the next [load], as a new start of the app would. */
    @androidx.annotation.VisibleForTesting
    internal fun forgetForTests() {
        loaded = false
        _state.value = AppSettings()
        _favouriteFx.value = emptySet()
    }

    /** Back to the defaults (the guide is not shown again). */
    fun reset(context: Context) = update(context) { AppSettings(guideSeen = true, language = it.language, favouriteFx = it.favouriteFx) }
}
