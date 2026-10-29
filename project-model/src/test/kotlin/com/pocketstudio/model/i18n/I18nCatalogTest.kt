package com.pocketstudio.model.i18n

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Every text the code passes to [tr] (or [trCount]) has a translation in every language with the
 * same placeholders, and no text is built with a Kotlin template inside [tr] (its key would never match).
 */
class I18nCatalogTest {
    @After
    fun french() {
        I18n.lang = Lang.FR
    }

    private val roots = listOf("src/main", "../app/src/main", "../ui-components/src/main").map(::File).filter { it.isDirectory }
    private val call = Regex("""(?<![A-Za-z0-9_.])tr\(\s*"((?:[^"\\]|\\.)*)"""")

    /** trCount(n, "1 mesure", "%d mesures"…): both keys. */
    private val countCall = Regex("""(?<![A-Za-z0-9_.])trCount\([^,"]+,\s*"((?:[^"\\]|\\.)*)"\s*,\s*"((?:[^"\\]|\\.)*)"""")

    /** Every language but French (the keys). */
    private val translated = Lang.entries - Lang.FR

    /** The French keys used by the code (in the files [which] keeps), with where they are. */
    private fun keys(which: (File) -> Boolean = { true }): Map<String, String> {
        val found = linkedMapOf<String, String>()
        roots.forEach { root ->
            root.walkTopDown().filter { it.extension == "kt" && which(it) }.forEach { f ->
                f.readLines().forEachIndexed { i, line ->
                    (call.findAll(line).map { it.groupValues[1] } + countCall.findAll(line).flatMap { it.groupValues.drop(1) }).forEach { raw ->
                        // An unescaped $ in the source is a Kotlin template (%1\$s is a placeholder, not one).
                        val where = "${f.name}:${i + 1}" + if (Regex("""(?<!\\)\$[A-Za-z{]""").containsMatchIn(raw)) " TEMPLATE" else ""
                        found.putIfAbsent(unescape(raw), where)
                    }
                }
            }
        }
        return found
    }

    private fun unescape(s: String): String = buildString {
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (val n = s[i + 1]) {
                    'n' -> append('\n')
                    't' -> append('\t')
                    else -> append(n)
                }
                i += 2
            } else {
                append(c); i++
            }
        }
    }

    private val placeholder = Regex("""%(\d+\$)?[-#+ 0,(]*\d*(\.\d+)?[sdfxc%]""")
    private fun placeholders(s: String) = placeholder.findAll(s).map { it.value }.filter { it != "%%" }.sorted().toList()

    @Test
    fun everyTextHasItsTranslations() {
        val keys = keys()
        assertTrue("no tr() call found", keys.isNotEmpty())
        val problems = mutableListOf<String>()
        keys.forEach { (key, where) ->
            if (where.endsWith(" TEMPLATE")) problems += "$where: template inside tr(\"$key\"): use %s"
        }
        // TEMPORARY (debug of the tutorial, owner 2026-10-28): the tutorial's and the « Mode retours »' new
        // texts are French only for now; a missing translation of theirs falls back to French.
        val waiting = keys { pending(it) }.keys - keys { !pending(it) }.keys
        translated.forEach { lang ->
            val table = I18n.table(lang)
            keys.forEach { (key, where) ->
                val t = table[key]
                when {
                    t.isNullOrBlank() && key in waiting -> Unit
                    t.isNullOrBlank() -> problems += "$where: no ${lang.code} for \"$key\""
                    placeholders(t) != placeholders(key) -> problems += "$where: ${lang.code} placeholders differ for \"$key\" -> \"$t\""
                }
            }
        }
        // The whole list, for whoever translates an area: build/i18n-problems.txt.
        File("build").mkdirs()
        File("build/i18n-problems.txt").writeText(problems.joinToString("\n"))
        assertTrue("${problems.size} problems (all in project-model/build/i18n-problems.txt):\n" + problems.take(80).joinToString("\n"), problems.isEmpty())
    }

    /** The first argument of every `tr(...)` call that is not a text literal, as "File.kt: expression". */
    private fun dynamicCalls(): Map<String, String> {
        val found = linkedMapOf<String, String>()
        val start = Regex("""(?<![A-Za-z0-9_.])tr\(""")
        roots.forEach { root ->
            root.walkTopDown().filter { it.extension == "kt" }.forEach { f ->
                val src = f.readText()
                start.findAll(src).forEach { m ->
                    var j = m.range.last + 1
                    var depth = 0
                    while (j < src.length) {
                        val c = src[j]
                        if (c == '"') {
                            if (src.startsWith("\"\"\"", j)) { j = src.indexOf("\"\"\"", j + 3) + 3; continue }
                            j++
                            while (src[j] != '"') j += if (src[j] == '\\') 2 else 1
                            j++
                            continue
                        }
                        if (c in "([{") depth++
                        else if (c in ")]}") { if (depth == 0) break; depth-- }
                        else if (c == ',' && depth == 0) break
                        j++
                    }
                    val arg = src.substring(m.range.last + 1, j).trim().replace(Regex("""\s+"""), " ")
                    if (arg.isNotEmpty() && !arg.startsWith("\"") && !arg.startsWith("fr:")) found.putIfAbsent("${f.name}: $arg", arg)
                }
            }
        }
        return found
    }

    /**
     * A text passed to [tr] in a variable is not seen by [everyTextHasItsTranslations]: each such
     * call is listed here once its source is checked (the model's data by ModelTextsTest, the app's
     * by AppTextsTest, names by DefaultNames). A new one fails until it is checked and added.
     */
    @Test
    fun everyTextPassedInAVariableComesFromACheckedSource() {
        val calls = dynamicCalls()
        assertTrue("no dynamic tr() found", calls.size > 40)
        val unknown = calls.keys - CHECKED_SOURCES
        assertTrue(
            "tr() with a variable whose texts nobody checks (add the data to ModelTextsTest or AppTextsTest, then here):\n" +
                unknown.joinToString("\n"),
            unknown.isEmpty(),
        )
        // A fallback written inside such a call ("Piste", "réglage") is a key too.
        val literal = Regex(""""((?:[^"\\]|\\.)*)"""")
        translated.forEach { lang ->
            val table = I18n.table(lang)
            calls.values.flatMap { arg -> literal.findAll(arg).map { it.groupValues[1] } }
                .filter { it.any(Char::isLetter) }
                .forEach { assertTrue("no ${lang.code} for fallback \"$it\"", !table[it].isNullOrBlank()) }
        }
    }

    /**
     * Audit (textes): no translation is left that nothing shows. Each French key must be written
     * somewhere in the app's code or data (Kotlin, the Guide's parcours, the assets, the packs'
     * scripts), as it is or escaped; an old text (the Missions, a tab renamed) is removed with its
     * screen. [KEPT_UNUSED]: keys kept on purpose.
     */
    @Test
    fun noTranslationIsLeftUnused() {
        val sources = (roots + listOf("../engine/src/main/java", "../separation/src/main", "../scripts").map(::File).filter { it.isDirectory })
            .flatMap { root ->
                root.walkTopDown().filter { f ->
                    f.isFile && f.extension in setOf("kt", "json", "java", "py") && !f.invariantSeparatorsPath.contains("/resources/i18n/")
                }.toList()
            }.map { it.readText() }
        assertTrue("no source found", sources.size > 100)
        // Each word (3 letters or more) → the sources holding it: a key is looked for only where its rarest word is.
        val word = Regex("""\p{L}{3,}""")
        val where = HashMap<String, MutableSet<Int>>()
        sources.forEachIndexed { i, text -> word.findAll(text).forEach { where.getOrPut(it.value) { HashSet() }.add(i) } }
        fun written(key: String): Boolean {
            val forms = setOf(key, key.replace("$", "\\$"), key.replace("\"", "\\\"").replace("\n", "\\n"))
            val words = word.findAll(key).map { it.value }.toList()
            val candidates = if (words.isEmpty()) sources.indices.toSet()
            else words.map { where[it].orEmpty() }.minByOrNull { it.size }.orEmpty()
            // The index finds whole words only (« Decay » inside « DecayKnob » is not one): a miss looks everywhere.
            return candidates.any { i -> forms.any { sources[i].contains(it) } } || sources.any { src -> forms.any { src.contains(it) } }
        }
        // A plural form ("%d mesures|few") is its count's key.
        val unused = translated.flatMap { lang ->
            I18n.AREAS.flatMap { area -> I18n.area(lang, area).keys.map { "${lang.code}/$area.json: $it" to it.substringBeforeLast('|') } }
        }.distinctBy { it.second }.filter { (_, key) -> key !in KEPT_UNUSED && !written(key) }.map { it.first }
        assertTrue("${unused.size} translations nothing shows (remove them):\n" + unused.take(60).joinToString("\n"), unused.isEmpty())
    }

    /** Audit (textes): no text sends to a tab that no longer exists (Morceau, Acide, Outils, Mixeur, Synthé › became Structure, Sons, Mix, Plus). */
    @Test
    fun noTextNamesAnOldTab() {
        val old = listOf("nglet Morceau", "onglet Acide", "Outils ›", "dans le Mixeur", "Synthé ›", "Mixeur, Automation")
        val found = keys().filter { (key, _) -> old.any { it in key } }.map { (key, where) -> "$where: $key" }
        assertTrue("old tabs named:\n" + found.joinToString("\n"), found.isEmpty())
    }

    /** Audit (textes): Dutch « refrein » is a het-word; the languages use their own quotes, not « » (Arabic's are « »). */
    @Test
    fun dutchAndEnglishWording() {
        val deRefrein = Regex("""\b(de|De|deze|Deze|elke|Elke|die|Die)( \p{L}+)? [Rr]efrein\b""")
        val nl = I18n.table(Lang.NL).filterValues { deRefrein.containsMatchIn(it) }
        assertTrue("« het refrein »:\n" + nl.values.joinToString("\n"), nl.isEmpty())
        (translated - Lang.AR).forEach { lang ->
            val quoted = I18n.table(lang).filterValues { '«' in it || '»' in it }
            assertTrue("${lang.code}: « » in\n" + quoted.values.joinToString("\n"), quoted.isEmpty())
        }
    }

    /** A file of the areas whose translations wait ([PENDING_TRANSLATION_AREAS]): its folder is named after the area. */
    private fun pending(f: File): Boolean = f.parentFile?.name in PENDING_TRANSLATION_AREAS

    companion object {
        /**
         * TEMPORARY (owner, 2026-10-28, tutorial debug): the areas whose new texts are in French only
         * for now (the app shows French where a translation is missing). Translate them, then empty
         * this set, before any public release (docs/DECISIONS.md).
         */
        val PENDING_TRANSLATION_AREAS = emptySet<String>()

        /** Kept although no code writes them: names an older version may have saved in English or Dutch. */
        private val KEPT_UNUSED = setOf("Charleston", "Cloche (Plaits)")

        /** Calls of tr() with a variable, each one's texts checked elsewhere (see the test above). */
        private val CHECKED_SOURCES = setOf(
            "AppRoot.kt: com.pocketstudio.model.PlaitsPatch.PARAMS.getOrElse(target.param - 1) { \"réglage\" }",
            "AutomationCard.kt: Buses.NAMES.getOrElse(t.slot) { \"?\" }",
            "AutomationCard.kt: Macros.NAMES[macro]",
            "AutomationCard.kt: effect.label",
            "AutomationCard.kt: it",
            "ChordPads.kt: key.scale.label",
            "SongChords.kt: base.harmony.chordKey.scale.label",  // a scale name (Scale, checked by ModelTextsTest)
            "RemixScreen.kt: h.scale.label",  // a scale name (Scale, checked by ModelTextsTest)
            "WhatPlays.kt: it",  // an effect's blurb (EffectType, checked by ModelTextsTest)
            "WhatPlays.kt: e.label",
            "WhatPlays.kt: Macros.NAMES[a.macro]",
            "WhatPlays.kt: side",  // a macro's end (Macros.ENDS, checked by ModelTextsTest)
            // The Guide's cards and stages (guides/*.json, GuideStages: checked by GuideParcoursTest).
            "GuideParcours.kt: fr",
            // « Tuto de A à Z »: the course's texts (tutos/*.json, checked by TutoCourseTest) and the styles' names (ModelTextsTest).
            "TutoPanel.kt: fr",
            // The key sheet's scales (Scale, checked by ModelTextsTest).
            "TutoPanel.kt: sc.label",
            "TutoPanel.kt: harmony.scale.label",
            // A word's alias in the « ? » helps (SoundGlossary.forHelp): compared with the shown term, never shown itself.
            "SoundGlossary.kt: it",
            // G4: « Voilà ce que j'ai mis »: an effect's name and a send's bus (EffectType, Buses: checked by ModelTextsTest).
            "GuideViewModel.kt: t.label",
            "GuideViewModel.kt: Buses.NAMES.getOrElse(bus) { \"Réverbe\" }",
            "DefaultNames.kt: COPY",
            "DefaultNames.kt: first",  // a stem label (DefaultNames.TEXTS) or a kit's preset name (shown as it is when unknown)
            "DefaultNames.kt: fr",
            "DefaultNames.kt: it",
            "DefaultNames.kt: name",
            "DefaultNames.kt: rest",  // a kit pad's name (DefaultNames.KIT_PADS, in DefaultNames.TEXTS)
            "Effects.kt: type.label",  // an effect name (EffectType, checked by ModelTextsTest)
            "EffectsBox.kt: group.label",
            "EffectsBox.kt: Macros.NAMES[m]",
            "EffectsBox.kt: ins.effect.blurb",
            "EffectsBox.kt: it.name",
            "EffectsBox.kt: param.format(ins.params[k])",
            "EffectsBox.kt: param.label",
            "EffectsBox.kt: type.label",
            "EffectsPanel.kt: Buses.NAMES[b]",
            "EffectsPanel.kt: Buses.NAMES[bus]",
            "EffectsPanel.kt: p.format(params[k])",
            "EffectsPanel.kt: p.label",
            "InstrumentPanel.kt: name",
            // Phase 38: the instruments' names and blurbs (MultiInstruments.KNOWN, checked by ModelTextsTest).
            "RealInstruments.kt: e.description",
            "RealInstruments.kt: e.name",
            "Tracks.kt: multi.name.ifBlank { \"Instrument\" }",
            "LibraryScreen.kt: label",
            "MySoundLibrary.kt: label",  // a family of sounds (SoundPacks.KIND_LABELS, checked by ModelTextsTest)
            "LibraryScreen.kt: option.name.substringBefore(\" · \")",
            // The style's name, as in the assistant (checked by ModelTextsTest).
            "LibraryScreen.kt: style.name",
            "PianoRoll.kt: g.scale.label",
            "PianoRoll.kt: m.label",
            "PianoRoll.kt: pr.label",
            "PianoRoll.kt: sc.label",
            "PlaitsPanel.kt: PlaitsPatch.MODELS[model]",
            "PlaitsPanel.kt: family",
            "PlaitsPanel.kt: label",
            // « Nouveau projet »: the starters (AppTextsTest) and the assistant's styles and families (ModelTextsTest).
            "NewProjectPage.kt: f",
            "NewProjectPage.kt: it",
            "NewProjectPage.kt: s.description",
            "NewProjectPage.kt: s.name",
            "NewProjectPage.kt: s.title",
            "QuickFx.kt: FxPresets.NONE.name",
            "QuickFx.kt: Macros.NAMES[m]",
            "QuickFx.kt: a",
            "QuickFx.kt: b",
            "QuickFx.kt: c",
            "QuickFx.kt: it",
            "QuickFx.kt: it.name",
            "QuickFx.kt: kind.label",
            "QuickFx.kt: name",
            "QuickFx.kt: p.name",
            "RhythmViewModel.kt: STEM_LABELS.getOrElse(stem) { \"Piste\" }",
            "RhythmViewModel.kt: kit.name",
            "Scenes.kt: label",
            "Scenes.kt: section",
            "SessionScreen.kt: s",
            "SongGuide.kt: styleName",
            "SoundBrowser.kt: f",
            "SoundBrowser.kt: s.name",
            // Pack names from the index (the free-library ones are checked by PackCatalogTest; a style pack shows its French name).
            "LibraryScreen.kt: entry.name",
            "LibraryScreen.kt: packName",
            "LibraryScreen.kt: queue.current",
            "LibraryScreen.kt: hit.pack.name",
            "SoundLibraryViewModel.kt: SoundPacks.KIND_LABELS[kind] ?: kind",
            "SoundLibraryViewModel.kt: SoundPacks.KIND_LABELS[role.packKind] ?: role.title",
            "SoundLibraryViewModel.kt: sound.name",
            "SoundLooks.kt: it",
            "StarterWizard.kt: ACID_TITLE",
            "StarterWizard.kt: COUNTER_TITLE",
            "StarterWizard.kt: IMPACT_TITLE",
            "StarterWizard.kt: RISER_TITLE",
            "StarterWizard.kt: label",
            "StarterWizard.kt: roleKey(index)",
            "StarterWizard.kt: it.title",
            "SynthScreen.kt: name",
            "TrackBar.kt: t.soundLabel",
            "SynthScreen.kt: t.soundLabel",
            // G3: a layer's name, a library sound's (SoundLibrary, checked by ModelTextsTest); an effect's name (EffectType, same).
            "Layers.kt: name",
            "GuideSheets.kt: libraryName",
            "GuideHistory.kt: effect.label",
            // The track sheet: an effect's and its first setting's names, a ready-made chain's (EffectType, FxPresets:
            // ModelTextsTest), what a curve moves (GuideSheets.laneWhat: those, Buses.NAMES, or a fallback below).
            "TrackSheet.kt: e.type.label",
            "TrackSheet.kt: e.valueLabel",
            "TrackSheet.kt: o.preset!!",
            "TrackSheet.kt: l.what",
            "WizardScreen.kt: StarterWizard.progressionOf(style, m).first.label",
            "WizardScreen.kt: StarterWizard.progressionOf(style, mood).first.label",
            "WizardScreen.kt: it",
            // The assistant's quick 303 sounds (WizardAcid.SOUNDS, in ModelTextsTest).
            "WizardScreen.kt: acidSoundLabel",
            "WizardScreen.kt: grid.name",
            "WizardScreen.kt: grid.description",
            // "+ Piste": the kinds of track (TrackKind, in ModelTextsTest).
            "TrackAdd.kt: kind.title",
            "TrackAdd.kt: kind.description",
            "WizardScreen.kt: it.name",
            "WizardScreen.kt: line.role.title",
            "WizardScreen.kt: StarterWizard.ScenePlan.ACID_TITLE",
            "WizardScreen.kt: f",
            "StyleFamilies.kt: family",
            "WizardScreen.kt: tip",
            "WizardScreen.kt: m.description",
            "WizardScreen.kt: m.label",
            "WizardScreen.kt: mood.label",
            "WizardScreen.kt: option.name.substringBefore(\" · \")",
            "WizardScreen.kt: part.explanation",
            "WizardScreen.kt: part.title",
            "WizardScreen.kt: role.explanation",
            "WizardScreen.kt: role.title",
            "WizardScreen.kt: row!!.sound!!.name.substringBefore(\" · \")",
            "WizardScreen.kt: row!!.sound.name",
            "WizardScreen.kt: s.description",
            "WizardScreen.kt: s.name",
            "WizardScreen.kt: sound.name",
            "WizardScreen.kt: style.name",
            "LaunchChoice.kt: it.name",
            // trCount's two keys: literals at each call, checked by everyTextHasItsTranslations (countCall).
            "I18n.kt: one",
            "I18n.kt: other",  // « Que veux-tu faire ? »: the project's style (StarterWizard.STYLES, checked by ModelTextsTest)
        )
    }

    @Test
    fun theLanguageIsFollowedAndFrenchIsTheFallback() {
        assertEquals(Lang.FR, Lang.of(null))
        assertEquals(Lang.NL, Lang.of("nl"))
        I18n.lang = Lang.FR
        assertEquals("Texte inconnu", tr("Texte inconnu"))
        I18n.lang = Lang.EN
        assertEquals("Texte inconnu", tr("Texte inconnu"))
        assertEquals("Zz 3 (aucune traduction)", tr("Zz %d (aucune traduction)", 3))
    }

    @Test
    fun aFrenchTextHasOneTranslationAcrossAreas() {
        // The areas are merged: the same key with two meanings would show the wrong one somewhere.
        translated.forEach { lang ->
            val seen = mutableMapOf<String, MutableSet<String>>()
            I18n.AREAS.forEach { area -> I18n.area(lang, area).forEach { (k, v) -> seen.getOrPut(k) { mutableSetOf() } += v } }
            val clashes = seen.filterValues { it.size > 1 }
            assertTrue("${lang.code}: ${clashes.size} keys translated differently: $clashes", clashes.isEmpty())
        }
    }

    @Test
    fun aShownTextLeadsBackToItsFrenchKey() {
        I18n.lang = Lang.EN
        val table = I18n.table(Lang.EN)
        val (fr, en) = table.entries.first { e -> table.values.count { it == e.value } == 1 }
        assertEquals(fr, I18n.frenchOf(en))
        assertEquals("Pas traduit", I18n.frenchOf("Pas traduit"))
    }

    @Test
    fun catalogsAreValidAndHaveTheSameKeysInEveryLanguage() {
        (I18n.AREAS - "plurals").forEach { area ->
            val en = I18n.area(Lang.EN, area).keys
            translated.forEach { lang ->
                val keys = I18n.area(lang, area).keys
                assertEquals("area $area: en and ${lang.code} differ: ${(en - keys) + (keys - en)}", en, keys)
                assertTrue("${lang.code}/$area.json: an empty text", I18n.area(lang, area).values.none { it.isBlank() })
            }
        }
    }

    /**
     * A language's extra plural forms ("%d mesures|few") are those of a count the code shows with
     * [trCount], in a category the language has, with no placeholder the count's key has not (the
     * Arabic dual may say the number in words).
     */
    @Test
    fun pluralFormsBelongToACountAndALanguagesCategory() {
        val counts = roots.flatMap { root ->
            root.walkTopDown().filter { it.extension == "kt" }.flatMap { f -> countCall.findAll(f.readText()).map { unescape(it.groupValues[2]) } }.toList()
        }.toSet()
        assertTrue("no trCount() found", counts.size > 10)
        val problems = mutableListOf<String>()
        Lang.entries.forEach { lang ->
            val categories = (0L..300L).map { pluralOf(lang.code, it).key }.toSet() - "one" - "other"
            I18n.area(lang, "plurals").forEach { (key, form) ->
                val count = key.substringBeforeLast('|')
                val category = key.substringAfterLast('|', "")
                if (count !in counts) problems += "${lang.code}: \"$key\": no trCount shows \"$count\""
                if (category !in categories) problems += "${lang.code}: \"$key\": ${lang.code} has no « $category » (only $categories)"
                if (!placeholders(count).containsAll(placeholders(form))) problems += "${lang.code}: \"$key\" -> \"$form\": placeholders"
                if (form.isBlank()) problems += "${lang.code}: \"$key\" empty"
            }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
        // Polish and Arabic have their forms for the bars (« 2 takty », « 5 taktów »; « مازورتان »).
        assertTrue(I18n.area(Lang.PL, "plurals").keys.containsAll(listOf("%d mesures|few", "%d mesures|many")))
        assertTrue(I18n.area(Lang.AR, "plurals").keys.containsAll(listOf("%d mesures|two", "%d mesures|few", "%d mesures|many")))
    }
}
