package com.pocketstudio.app.export

import com.pocketstudio.app.rhythm.ExportFade
import com.pocketstudio.app.rhythm.ExportFormat
import com.pocketstudio.app.rhythm.ExportUi
import com.pocketstudio.app.rhythm.ExportWhat
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.Demos
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectCatalog
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.i18n.tr

/** « Partager mon morceau » in one touch: what it renders and when it asks for a name first. */
object ShareSong {
    /** A scene alone is looped up to at least this many bars, so the file sounds like a piece. */
    const val MIN_LOOP_BARS = 8

    /**
     * The whole song when there is one, else the scene looped (long enough for the audio clips
     * heard, which end on bar [audioBars]); M4A (small, plays everywhere) with a short fade.
     */
    fun settings(hasSong: Boolean, sceneBars: Int, audioBars: Int = 0): ExportUi = ExportUi(
        what = if (hasSong) ExportWhat.Song else ExportWhat.Loop, whatChosen = true,
        format = ExportFormat.M4a,
        fade = ExportFade.Short, fadeChosen = true,
        bars = loopBars(sceneBars, audioBars), barsChosen = true,
    ).forScene(sceneBars)

    /**
     * The scene repeated whole until it lasts [MIN_LOOP_BARS] bars, or longer when an audio clip
     * (a recorded voice) ends later, on bar [audioBars] (at most the longest export).
     */
    fun loopBars(sceneBars: Int, audioBars: Int = 0): Int {
        val scene = sceneBars.coerceIn(1, RhythmViewModel.MAX_EXPORT_BARS)
        val times = (MIN_LOOP_BARS + scene - 1) / scene
        return maxOf(scene * times, audioBars).coerceAtMost(RhythmViewModel.MAX_EXPORT_BARS)
    }

    /** [fr] and its translation in every language: a default name stays one whatever the language it was given in. */
    private fun everyLanguage(fr: String): List<String> =
        listOf(fr) + Lang.entries.mapNotNull { I18n.table(it)[fr]?.takeIf { t -> t.isNotBlank() } }

    private val NUMBERED: Regex by lazy {
        val names = listOf("Mon projet", "Mon morceau", "Nouveau projet").flatMap(::everyLanguage).distinct()
        Regex("(" + names.joinToString("|") { Regex.escape(it) } + ")( \\d+)?")
    }

    /** Names of the genre templates « Nouveau projet » offered before the assistant's styles took their place. */
    private val OLD_TEMPLATE_NAMES = listOf("House", "Techno", "Drum & bass", "Trap", "Lo-fi", "Synthwave")

    /** Names the app gives by itself: a song shared under one of them is asked for a real name first. */
    private val TEMPLATE_NAMES: Set<String> by lazy {
        buildList {
            add(Project().name)
            add("Mon premier rythme")
            // The one-scene genre templates of earlier versions named their projects so.
            addAll(OLD_TEMPLATE_NAMES)
            Demos.ALL.forEach { add(it.first) }
            StarterWizard.STYLES.forEach { add(it.name) }
        }.flatMap(::everyLanguage).toSet()
    }

    /** True when [name] was not chosen by the user (empty, "Mon projet 3", a template's or a style's name, in any language). */
    fun isDefaultName(name: String): Boolean {
        val clean = name.trim()
        return clean.isEmpty() || NUMBERED.matches(clean) || clean in TEMPLATE_NAMES
    }

    /** What the « Nomme ton morceau » field starts with: the style's name is a fine start, a number is not. */
    fun suggestedName(name: String): String {
        val clean = name.trim()
        return if (clean.isEmpty() || NUMBERED.matches(clean)) tr("Mon morceau") else clean.take(ProjectCatalog.MAX_NAME)

    }
}
