package com.pocketstudio.model.guide

import com.pocketstudio.model.EffectType
import com.pocketstudio.model.LoudnessTarget
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.learn.SoundGlossary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

/** The parcours are data (docs/PLAN-GUIDE.md §3.5, §4.1): one per style, 30 to 40 cards in the pro order, every text translated, every action and detector known. */
class GuideParcoursTest {
    private val all: List<GuideParcours> = StarterWizard.STYLES.map { s -> GuideLibrary.forStyle(s.id) ?: error("no parcours for ${s.id}") }
    private val generic = GuideLibrary.forStyle(GuideParcours.GENERIC)!!
    private val alerts = GuideLibrary.forStyle(GuideLibrary.ALERTS)!!
    private val common = GuideLibrary.forStyle(GuideLibrary.COMMON)!!

    @Test
    fun everyStyleHasItsParcoursOfThirtyToFortyCardsInStageOrder() {
        assertEquals(37, all.size)
        for (p in all + generic) {
            assertEquals(p.style, GuideParcours.FORMAT, p.format)
            assertTrue("${p.style}: ${p.cards.size} cards", p.cards.size in 30..40)
            assertEquals(p.style, p.cards.map { it.stage }.sorted(), p.cards.map { it.stage })
            assertEquals(p.style, p.cards.size, p.cards.map { it.id }.toSet().size)
            assertTrue(p.style, (0 until GuideStages.COUNT).all { st -> p.cards.any { it.stage == st } || st == 2 })
            // The Mode simple: stages 1, 4, 6 and 7, and those only.
            assertEquals(p.style, p.cards.filter { it.stage in GuideStages.SIMPLE }.map { it.id }, p.cards.filter { it.simple }.map { it.id })
            assertTrue(p.style, p.cardsFor(true).size in 10..22)
            // The first card of stage 3 is « voilà ce que j'ai mis ».
            assertEquals("fx.review", p.cards.first { it.stage == 3 }.id)
        }
        assertEquals(listOf("kick", "snare", "hats", "bass", "pad", "lead", "vocal", "riser"), GuideLibrary.forStyle("jump_up")!!.cards.filter { it.stage == 1 }.map { it.target })
    }

    @Test
    fun everyCardHasTwoOrThreeOptionsWithKnownActionsAndDetectors() {
        val spots = mutableSetOf<String>()
        for (p in all + generic + alerts + common) for (c in p.cards) {
            // « C'est fait » (audit final) comes on top of a card whose options all open a screen.
            val choices = c.proposals.filterNot { it.label == "C'est fait" }
            assertTrue("${p.style}/${c.id}: ${c.proposals.size} options", choices.size in 2..3)
            if (c.done.isEmpty() && choices.all { it.action.type == GuideAction.OPEN }) {
                assertTrue("${p.style}/${c.id}: no way to tick it", (c.proposals + c.elsewhere).any { it.action.type == GuideAction.KEEP })
            }
            assertTrue("${p.style}/${c.id}", c.kind in setOf(GuideCard.DECIDE, GuideCard.REVIEW, GuideCard.CHECK))
            assertTrue("${p.style}/${c.id}", c.target == GuideRoles.SONG || c.target in GuideRoles.ORDER)
            assertTrue("${p.style}/${c.id}: lexicon ${c.lexicon}", c.lexicon.isEmpty() || SoundGlossary.ALL.any { it.key == c.lexicon })
            for (d in (c.applicable + c.done).flatMap { it.split('|') }) {
                val name = d.removePrefix("!").substringBefore(':')
                assertTrue("${p.style}/${c.id}: detector $d", name in GuideFacts.DETECTORS || name in GuideFacts.ROLE_DETECTORS)
            }
            for (o in c.proposals + c.elsewhere) {
                val a = o.action
                assertTrue("${p.style}/${c.id}/${o.id}: ${a.type}", a.type in GuideAction.TYPES)
                (listOf(a) + a.inserts).filter { it.effect.isNotEmpty() }.forEach { assertTrue(it.effect, EffectType.entries.any { e -> e.name == it.effect }) }
                if (a.type == GuideAction.TARGET) LoudnessTarget.valueOf(a.name)
                if (a.type == GuideAction.OPEN) spots += a.spot
                assertTrue("${p.style}/${c.id}/${o.id}", o.label.isNotBlank())
            }
            assertEquals("${p.style}/${c.id}", (c.proposals + c.elsewhere).size, (c.proposals + c.elsewhere).map { it.id }.toSet().size)
            // « Plus tard » puts the card off, it never ticks it (review M6).
            (c.proposals + c.elsewhere).filter { it.id == "later" }.forEach { assertEquals("${p.style}/${c.id}", GuideAction.LATER, it.action.type) }
            // A review card always offers to keep what is there (review M9).
            if (c.kind == GuideCard.REVIEW && c.stage == 3) assertTrue("${p.style}/${c.id}", c.proposals.any { it.action.type == GuideAction.KEEP })
        }
        // The screens the options open (the panel maps each one to a control, G2).
        assertTrue(spots.toString(), spots.all { it.matches(Regex("[a-z]+(\\.[a-z]+)?")) })
    }

    @Test
    fun everyTextOfTheParcoursHasItsEnglishAndDutch() {
        val texts = (all + generic + alerts + common).flatMap { p ->
            p.cards.flatMap { c ->
                listOfNotNull(c.title, c.why, c.simpleTitle, c.simpleWhy, c.whenText, c.howMuch, c.howOften) +
                    (c.proposals + c.elsewhere).flatMap { listOfNotNull(it.label, it.why, it.value, it.simpleLabel, it.simpleWhy) }
            } + p.whys.values.flatMap { it.values }
        }.filter { it.isNotBlank() }.toSet() + GuideStages.TITLES + GuideStages.SHORT + GuideStages.START_TITLE + GuideStages.START_SHORT +
            GuideStages.SIMPLE_TITLES.values
        for (lang in (Lang.entries - Lang.FR)) {
            val table = I18n.table(lang)
            val missing = texts.filter { table[it].isNullOrBlank() }
            assertTrue("${missing.size} without ${lang.code}: ${missing.take(10)}", missing.isEmpty())
        }
    }

    @Test
    fun theStylesRecipesSayWhatTheirCardsSay() {
        val jump = GuideLibrary.forStyle("jump_up")!!
        // Its growl, its sub, its second bass, its voice: each has its card.
        assertEquals("La basse : Growl", jump.card("fx.bass")!!.title)
        assertTrue(jump.card("fx.bass")!!.elsewhere.any { it.action.type == GuideAction.COPY_CHAIN && it.action.to == GuideRoles.BASS2 })
        listOf("fx.sub", "layers.bass", "fx.vocal", "arr.pad", "move.build", "sound.riser").forEach { assertTrue(it, jump.card(it) != null) }
        // Techno: the rumble, no voice; trap: the 808 under a kick, no Montée; house: a club Drop.
        val techno = GuideLibrary.forStyle("techno")!!
        assertTrue(techno.card("sound.rumble") != null && techno.card("sound.vocal") == null)
        val trap = GuideLibrary.forStyle("trap")!!
        assertEquals("layers.808", trap.cards.single { it.stage == 2 }.id)
        assertTrue(trap.card("move.build") == null)  // a rap song: no Montée
        assertTrue(trap.card("arr.subtract")!!.title.contains("Refrain"))
        val house = GuideLibrary.forStyle("house")!!
        assertTrue(house.card("arr.subtract")!!.title.contains("Drop"))
        // The why of each effect on a track, for the sheet.
        assertTrue(EffectType.entries.filter { it != EffectType.NONE }.all { jump.why("lead", it).isNotEmpty() })
        assertTrue(jump.why("bass", EffectType.FILTER) != jump.why("lead", EffectType.FILTER))
        // Since the leads chantier the jump up's lead is a Hoover, not a second bass: a lead's proposals,
        // and the first Drop's sparser start names it.
        assertTrue(jump.card("fx.lead")!!.proposals.any { it.label == "Plus d'écho" })
        assertTrue(jump.card("arr.differ")!!.proposals.first().label.contains("lead"))
        assertTrue(trap.card("mix.contrast")!!.proposals.first().label.contains("pont"))
        // The sidechain: kept as it is when the song already ducks.
        // Audit final: the ducking the assistant put is to hear (decided), not ticked by itself.
        assertEquals(listOf("sidechain", "by_hand|changed:sidechain"), jump.card("fx.sidechain")!!.done)
        // The style's sound cards are shown on its own song (decided, or the track changed by hand).
        assertEquals(listOf("has:kick", "by_hand|changed:kick"), jump.card("sound.kick")!!.done)
        // « Plus d'écho » of the lead no longer promises a send level the song does not have.
        assertTrue(jump.card("fx.lead")!!.howMuch.orEmpty().none { it.isDigit() })
        // « Poser ma voix » says what it does (the robot is not replaced by itself).
        assertTrue(jump.card("sound.vocal")!!.proposals.none { it.why.contains("à la place") })
        assertEquals("sounds.track", jump.card("sound.vocal")!!.proposals.first { it.id == "other" }.action.spot)
        // The chords' wait in the first Drop offered once (arr.pad has it).
        assertTrue(jump.card("arr.differ")!!.proposals.none { it.id == "pad_waits" })
    }

    /**
     * Audit (textes): the Mode simple says everyday words: no Hz, dB, LUFS, M4A, master, OTT or
     * ratio in what a simple card shows (its title, why, options and their values, its stage).
     */
    @Test
    fun theModeSimpleSpeaksWithoutJargon() {
        val jargon = Regex("""\b(k?Hz|dB|dBTP|LUFS|M4A|OTT|[Mm]aster|[Rr]atio|fader)\b|élargisseur|pré-délai""")
        val bad = (all + generic + common).flatMap { p ->
            p.cards.filter { it.simple }.flatMap { c ->
                (listOf(c.titleFor(true), c.whyFor(true), GuideStages.title(c.stage, simple = true)) +
                    (c.proposals + c.elsewhere).flatMap { o -> listOfNotNull(o.labelFor(true), o.whyFor(true), o.valueFor(true)) })
                    .filter { jargon.containsMatchIn(it) }.map { "${p.style}/${c.id}: $it" }
            }
        }.distinct()
        assertTrue("${bad.size} texts of the Mode simple with jargon:\n" + bad.take(20).joinToString("\n"), bad.isEmpty())
        // The full mode keeps its figures.
        val mono = GuideLibrary.forStyle("house")!!.card("mix.mono")!!.proposals.first { it.id == "narrow" }
        assertEquals("150 Hz", mono.valueFor(false))
        assertEquals(null, mono.valueFor(true))
    }

    /** Audit (textes): a sung song has no Drop: the common cards and the effects' whys say « le grand moment ». */
    @Test
    fun aSungSongHearsNoDrop() {
        val pop = GuideLibrary.forStyle("pop")!!
        val texts = (pop.cards + common.cards).filter { it.id != "move.build" }.flatMap { c ->
            listOfNotNull(c.title, c.why, c.simpleTitle, c.simpleWhy) + (c.proposals + c.elsewhere).flatMap { listOfNotNull(it.label, it.why, it.simpleLabel, it.simpleWhy) }
        } + listOf("riser", "pad").mapNotNull { pop.whys[it]?.get("FILTER") }
        val drops = texts.filter { "Drop" in it }
        assertTrue("Drop in a pop song: $drops", drops.isEmpty())
    }

    /** The parcours files and i18n/{en,nl}/guide.json are made by scripts/guides/make_guides.py: an edit of one without the other fails here. */
    @Test
    fun theParcoursAreWhatTheGeneratorWrites() {
        val root = File("..").canonicalFile
        val script = File(root, "scripts/guides/make_guides.py")
        assumeTrue("generator not found", script.isFile)
        val out = java.nio.file.Files.createTempDirectory("guides").toFile()
        try {
            val res = File(root, "project-model/src/main/resources")
            val process = runCatching { ProcessBuilder("python3", "-I", script.path, out.path).redirectErrorStream(true).start() }.getOrNull()
            assumeTrue("python3 is not available", process != null)
            val log = process!!.inputStream.bufferedReader().readText()
            assumeTrue("python3 did not finish", process.waitFor(120, TimeUnit.SECONDS))
            assertEquals("the generator failed:\n$log", 0, process.exitValue())
            val made = File(out, "guides").listFiles().orEmpty().map { it.name }.sorted()
            assertEquals(File(res, "guides").listFiles().orEmpty().map { it.name }.sorted(), made)
            for (name in made) assertEquals("regenerate guides/$name with scripts/guides/make_guides.py", File(out, "guides/$name").readText(), File(res, "guides/$name").readText())
            for (lang in listOf("en", "nl")) assertEquals("regenerate i18n/$lang/guide.json", File(out, "i18n/$lang/guide.json").readText(), File(res, "i18n/$lang/guide.json").readText())
        } finally {
            out.deleteRecursively()
        }
    }
}
