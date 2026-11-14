package com.pocketstudio.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Audit stage 2, points 6 and 7, read from the code (the project has no Konsist: plain source scans):
 * one way to do each thing, and nothing that does nothing. The shared mute / solo buttons and the
 * one pad style have their own tests (MuteSoloEverywhereTest, StillPadsEverywhereTest,
 * LiveGridStyleTest); the unused texts theirs (I18nCatalogTest.noTranslationIsLeftUnused).
 */
class ArchitectureRulesTest {
    private val sources = File("src/main/java").walkTopDown().filter { it.extension == "kt" }.toList()
    private fun text(name: String) = sources.first { it.name == name }.readText()
    private fun where(regex: Regex) = sources.flatMap { f ->
        f.readLines().withIndex().filter { regex.containsMatchIn(it.value) }.map { "${f.name}:${it.index + 1}" }
    }

    /**
     * One browser for the recorded sounds (Benjamin's n° 13 and 14: library, pads, live pads, a track's
     * sample, Ma bibliothèque, the assistant's « Choisis le son »): ShelfBrowser. The synths' presets have
     * theirs (SoundBrowser); the layers' picker and the instruments' panel are the remainder n° 14 names.
     * A new sound chooser fails here until it is one of them (or listed with its reason).
     */
    @Test
    fun oneSoundBrowser() {
        val names = sources.flatMap { f -> Regex("""fun ([A-Za-z]*(?:Browser|SoundPicker))\(""").findAll(f.readText()).map { "${f.name}/${it.groupValues[1]}" } }.toSet()
        assertEquals(
            "sound choosers",
            setOf(
                "SoundShelfBrowser.kt/ShelfBrowser", "SoundBrowser.kt/SoundBrowser", "InstrumentPanel.kt/Browser", "TrackSheet.kt/SoundPicker",
                // Builders of SoundBrowser's state (not screens).
                "AppRoot.kt/trackSoundBrowser", "AppRoot.kt/acidSoundBrowser",
            ),
            names,
        )
        val shelfUsers = sources.filter { "ShelfBrowser(" in it.readText() }.map { it.name }.toSet()
        assertTrue("the library and the assistant's sound step use the shared browser: $shelfUsers",
            shelfUsers.containsAll(setOf("LibraryScreen.kt", "WizardScreen.kt")))
    }

    /** One tempo control: the tempo sheet, opened the same way from the top bar, the Live and Pad Live. */
    @Test
    fun oneTempoControl() {
        assertEquals(listOf("TempoSheet.kt"), sources.filter { Regex("""fun TempoSheet\(""").containsMatchIn(it.readText()) }.map { it.name })
        val calls = where(Regex("""\bTempoSheet\("""))
            .filterNot { it.startsWith("TempoSheet.kt") }
        assertTrue("TempoSheet shown from the top bar, the Live and Pad Live: $calls", calls.size >= 3)
        // Every one of them starts the sheet on the ViewModel (one undo step per visit) and sets through it.
        sources.filter { f -> f.name != "TempoSheet.kt" && Regex("""\bTempoSheet\(""").containsMatchIn(f.readText()) }.forEach { f ->
            val t = f.readText()
            assertTrue("${f.name}: a tempo sheet without beginTempoSheet", "beginTempoSheet()" in t)
            assertTrue("${f.name}: a tempo sheet that sets the tempo another way", Regex("""TempoSheet\([^\n]*set = vm::setSheetTempo""").containsMatchIn(t))
        }
        // Nobody else draws the sheet's own controls.
        val presets = where(Regex(""""tempo_(preset|key|half|double)""")).filterNot { it.startsWith("TempoSheet.kt") }
        assertTrue("tempo controls outside the tempo sheet: $presets", presets.isEmpty())
    }

    /** One undo path: the project's history (ProjectSession), reached through RhythmViewModel.undo / redo. */
    @Test
    fun oneUndoPath() {
        val sessionUndo = where(Regex("""session\.(undo|redo)\(""")).map { it.substringBefore(':') }.toSet()
        assertEquals(setOf("RhythmViewModel.kt"), sessionUndo)
        // The only other undo: the cut screen's own (its cuts are not in the project until « Poser »).
        val otherUndos = where(Regex("""fun undo\(""")).map { it.substringBefore(':') }.toSet()
        assertEquals(setOf("RhythmViewModel.kt", "CutModel.kt"), otherUndos)
    }

    /** Numbers are formatted in the interface's language, never the phone's (lint NonObservableLocale). */
    @Test
    fun numbersFollowTheInterfacesLanguage() {
        val phone = where(Regex("""Locale\.getDefault\(\)"""))
        assertTrue("formatted with the phone's language rather than the app's: $phone", phone.isEmpty())
    }

    /**
     * The Mode simple and the note names read in composition are observed (lint
     * StateFlowValueCalledInComposition): the screens that showed the old ones until something else
     * redrew them read [com.pocketstudio.app.settings.observedSettings].
     */
    @Test
    fun settingsReadOnScreenAreObserved() {
        val stale = listOf("NewProjectPage.kt", "PlainWords.kt", "SynthScreen.kt", "VideoSheet.kt").flatMap { name ->
            text(name).lines().withIndex().filter { "SettingsStore.state.value" in it.value || "rhythmVm.project.value\n" in it.value + "\n" }
                .map { "$name:${it.index + 1}" }
        }
        assertTrue("settings or the project read without being observed: $stale", stale.isEmpty())
        val wizard = text("WizardScreen.kt").lines().withIndex().filter { "SettingsStore.state.value.simpleMode" in it.value }.map { it.index + 1 }
        assertTrue("WizardScreen reads the mode without observing it at $wizard", wizard.isEmpty())
    }

    /** Every setting changes something: each field of AppSettings is read outside the settings' own file. */
    @Test
    fun noSettingWithoutEffect() {
        val settings = text("Settings.kt")
        val body = settings.substringAfter("data class AppSettings(").substringBefore("\n)")
        val fields = Regex("""^\s*val (\w+)\s*:""", RegexOption.MULTILINE).findAll(body).map { it.groupValues[1] }.toList()
        assertTrue("AppSettings not parsed: $fields", fields.size >= 10)
        val elsewhere = sources.filter { it.name != "Settings.kt" }.joinToString("\n") { it.readText() }
        val dead = fields.filter { f -> !Regex("""\.$f\b""").containsMatchIn(elsewhere) }
        assertTrue("settings nothing reads: $dead", dead.isEmpty())
    }
}
