package com.pocketstudio.model.i18n

import com.pocketstudio.model.ArpMode
import com.pocketstudio.model.Buses
import com.pocketstudio.model.DefaultNames
import com.pocketstudio.model.Demos
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.FxPresets
import com.pocketstudio.model.Macros
import com.pocketstudio.model.PlaitsPatch
import com.pocketstudio.model.Scale
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SoundCategories
import com.pocketstudio.model.SoundLibrary
import com.pocketstudio.model.SongShape
import com.pocketstudio.model.SoundPacks
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.SynthPatch
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The model's data texts (style names and explanations, effects, presets, sounds, categories,
 * scales, moods, parts…) stay French in the model and are shown through `tr(value)`: every one
 * of them has an English and a Dutch translation. The texts are read from the real data, so a new
 * style or preset without its translations fails here.
 */
class ModelTextsTest {
    @After
    fun french() {
        I18n.lang = Lang.FR
    }

    /** Every French text of the model's data that a screen shows. */
    private fun modelTexts(): Set<String> {
        val texts = linkedSetOf<String>()
        fun add(vararg s: String) = s.filter { it.isNotBlank() }.forEach { texts += it }
        fun addAll(s: Iterable<String>) = s.forEach { add(it) }

        // The assistant: styles (references are song titles: not translated), lines, moods, parts, sections.
        addAll(StarterWizard.FAMILIES)
        StarterWizard.STYLES.forEach { style ->
            add(style.name, style.family, style.description, style.melody.bass, style.melody.pad, style.melody.lead)
            style.lines.forEach { add(it.tip) }
            addAll(style.melody.tips.values)
        }
        StarterWizard.Role.entries.forEach { add(it.title, it.explanation) }
        StarterWizard.Mood.entries.forEach { add(it.label, it.description) }
        StarterWizard.Part.entries.forEach { add(it.title, it.explanation); addAll(it.categories) }
        StarterWizard.SongForm.entries.forEach { addAll(it.labels); addAll(it.roles) }
        add(StarterWizard.SongForm.BREAK_LABEL, StarterWizard.SongForm.BREAK_ROLE)
        add(StarterWizard.SongForm.BREAK_ROLE_WITH_BASS, StarterWizard.SongForm.GROOVE_ROLE_NO_BASS)
        add(StarterWizard.ScenePlan.ACID_TITLE, StarterWizard.ScenePlan.RISER_TITLE, StarterWizard.ScenePlan.IMPACT_TITLE, StarterWizard.RISER_TRACK)
        add(StarterWizard.ScenePlan.COUNTER_TITLE, StarterWizard.COUNTER_TRACK)
        // The song's variants (S2) and its own tracks.
        add(SongShape.MONTEE, SongShape.PRE_CHORUS, SongShape.POST_CHORUS, SongShape.FAKE_DROP, StarterWizard.SongForm.BRIDGE_LABEL)
        add(StarterWizard.SUB_DROP_TRACK, StarterWizard.VOCAL_TRACK)
        addAll(StarterWizard.KIT_MELODIC.values)
        // The assistant's quick 303 sounds, and the kinds of track "+ Piste" offers (with their names).
        addAll(com.pocketstudio.model.WizardAcid.SOUNDS.map { it.first })
        com.pocketstudio.model.TrackKind.entries.forEach { add(it.trackName, it.title, it.description) }
        // Phase 38: the multizone instruments of the chooser and their families.
        com.pocketstudio.model.MultiInstruments.KNOWN.forEach { add(it.name, it.description) }
        addAll(com.pocketstudio.model.MultiInstruments.CATEGORY_LABELS.values)
        // The sounds the assistant proposes ("Cloche (Plaits)"…).
        StarterWizard.STYLES.forEach { style -> StarterWizard.Part.entries.forEach { p -> addAll(StarterWizard.soundsFor(p, style).map { it.name }) } }

        // Effects, sends, macros, ready-made treatments.
        EffectType.entries.forEach { e -> add(e.label, e.blurb); addAll(e.params.map { it.label }) }
        com.pocketstudio.model.EffectGroup.entries.forEach { add(it.label) }
        com.pocketstudio.model.SoundKind.entries.forEach { add(it.label) }
        addAll(Buses.NAMES)
        Buses.PARAMS.forEach { list -> addAll(list.map { it.label }) }
        addAll(Macros.NAMES)
        Macros.ENDS.forEach { (a, b) -> add(a, b) }
        addAll(FxPresets.CATEGORIES)
        (FxPresets.ALL + FxPresets.NONE).forEach { add(it.name, it.description, it.category) }

        // Sound libraries and their families.
        add(SoundCategories.FAVORITES)
        addAll(SoundCategories.SYNTH + SoundCategories.PLAITS + SoundCategories.ACID)
        SoundLibrary.SYNTH.forEach { add(it.name, it.category) }
        SoundLibrary.PLAITS.forEach { add(it.name, it.category) }
        SoundLibrary.ACID.forEach { add(it.name, it.category) }
        addAll(SynthPatch.PRESETS.map { it.first })
        addAll(SoundPacks.KIND_LABELS.values)
        addAll(PlaitsPatch.MODELS)
        addAll(PlaitsPatch.FAMILIES.map { it.first })
        addAll(PlaitsPatch.PARAMS)

        // Templates and demos.
        Demos.ALL.forEach { add(it.first, it.second) }

        // Names the app gives (saved in French, shown translated) and song sections.
        addAll(DefaultNames.TEXTS)
        addAll(Scene.SECTIONS)
        addAll(Scene.DRUM_LABELS)

        // Harmony.
        Scale.entries.forEach { add(it.label) }
        ArpMode.entries.forEach { add(it.label) }
        return texts
    }

    @Test
    fun everyDataTextHasItsTranslations() {
        val texts = modelTexts()
        assertTrue("no model text found", texts.size > 300)
        val problems = mutableListOf<String>()
        listOf(Lang.EN, Lang.NL).forEach { lang ->
            val table = I18n.table(lang)
            texts.forEach { if (table[it].isNullOrBlank()) problems += "no ${lang.code} for \"$it\"" }
        }
        // The list of texts, for whoever adds data: build/model-texts.txt.
        File("build").mkdirs()
        File("build/model-texts.txt").writeText(texts.joinToString("\n"))
        assertTrue("${problems.size} problems:\n" + problems.take(80).joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun dataIsShownInTheChosenLanguage() {
        val style = StarterWizard.style("boom_bap")!!
        I18n.lang = Lang.EN
        assertTrue(tr(style.description) != style.description)
        assertTrue(tr(EffectType.EQ.label) != EffectType.EQ.label)
        val plan = StarterWizard.scenePlan(setOf(StarterWizard.Role.Kick), setOf(StarterWizard.Part.Bass), acid = false)
        assertTrue(plan.title(1) != plan.labels[1])
        I18n.lang = Lang.FR
        assertTrue(plan.title(1) == "Couplet")
        assertTrue(plan.sections(setOf(1, 2)) == "Couplet et Refrain")
    }
}
