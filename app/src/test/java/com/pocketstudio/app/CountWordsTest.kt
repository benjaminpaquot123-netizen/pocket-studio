package com.pocketstudio.app

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Audit ultra round 3, n° 38: a count in words goes through the language's plural rule, never through
 * a sentence written once for « many » (« Timeline: 1 parts », « 1 parti »), nor through a test on
 * « 1 » alone (Polish says « 2 takty » but « 5 taktów », Arabic has a dual). Read from the sources:
 * - every `tr("… %d word …")` is a [com.pocketstudio.model.i18n.trCount] (or listed below, with why its
 *   number never needs a plural: a unit, a number that is never 1, a word that does not change);
 * - every count given to `trCount` has its « one » text in every language, and the extra forms of
 *   Polish (few, many) and Arabic (two, few, many).
 */
class CountWordsTest {
    private val sources = listOf(File("src/main/java"), File("../project-model/src/main/kotlin"))

    /** `tr(` (not `trCount(`, not `xtr(`) and its first literal. */
    private val call = Regex("""(?<![A-Za-z_])tr\(\s*"((?:[^"\\]|\\.)*)"""")
    private val pair = Regex("""trCount\(\s*[^,"]+,\s*"((?:[^"\\]|\\.)*)",\s*"((?:[^"\\]|\\.)*)"""")

    /** A number, then a word that a count changes (« %d mesures », « %2$d pistes »). */
    private val counted = Regex("""%(?:\d\\?\$)?d\s+(\p{L}[^\s%,.;:)«»"/·]*)""")

    /** Words after a number that no language here changes with it: units and signs. */
    private val units = setOf(
        "s", "min", "h", "Mo", "Ko", "ko", "kb", "bits", "Hz", "kHz", "BPM", "dB", "ms", "x", "éch",
        // Not the counted word: the sentence goes on (« le pad %d et sa longueur », « la mesure %d de la chanson »).
        "à", "sur", "et", "de", "du", "des", "n'ont", "ou",
    )

    /**
     * Counts whose number is never 1 (a limit, a size of 2 or more), or said as a label (« Voix : 3 »):
     * French key › why. A new entry needs its reason.
     */
    private val never1 = mapOf(
        "Le morceau a déjà %d parties : retires-en une pour en ajouter une autre." to "the parts' limit (SongPart.MAX_PARTS)",
        "Les %d parties sont déjà prises : vide-en une pour recommencer." to "the scenes' limit (Scene.MAX)",
        "Les %d parties sont déjà prises : plus de place pour une copie." to "the scenes' limit (Scene.MAX)",
        "Le morceau est plein (%d parties) : la fin de la prise n'a pas été écrite." to "the parts' limit",
        "%d effets au maximum sur ce son : retires-en un pour en ajouter un autre." to "the inserts' limit",
        "%d réglages bougent déjà : retires-en un dans Mix pour en automatiser un autre." to "the lanes' limit",
        "Tu as déjà %d favoris de ce son : supprime-en un pour en garder un nouveau." to "the favourites' limit",
        "%d images au plus : les autres ne sont pas ajoutées." to "the images' limit",
        "%d caractères au plus" to "a name's limit",
        "%d minutes au maximum. Les %d derniers morceaux séparés sont gardés, et ceux qu'un projet utilise." to "two limits",
        "Morceau trop long (%s) : %d minutes au maximum." to "the separation's limit",
        "Trop long pour un pad : %d secondes au plus. Choisis un morceau plus court." to "a pad's limit",
        "Trop long pour une piste : %d minutes au plus. Choisis un morceau plus court." to "a track's limit",
        "Morceau trop long pour geler une piste (%d minutes au plus)." to "the freeze's limit",
        "Ce fichier dure plus de %d minutes : seul le début est gardé." to "a file's limit",
        "Plus de place pour d'autres réglages (16 au plus) : %d n'ont pas été écrits." to "said only for 2 or more",
        "Pour débuter : %d onglets, de gros boutons et des mots de tous les jours." to "the Mode simple's five tabs",
        "Simple : %d onglets" to "the Mode simple's five tabs",
        "Jusqu'à %d sons qui jouent les mêmes notes, chacun à sa place." to "the layers' limit",
        "gardés %d jours" to "the bin's days",
        "Stockage plein : il faut %d Mo libres." to "a unit",
        "%d octaves" to "the keyboard's choices (2 or more)",
        "Les %d derniers écrans et gestes, et aucune erreur" to "the journal's length",
        "Les %d derniers écrans et gestes, et 1 erreur" to "the journal's length",
        "Les %1\$d derniers écrans et gestes, et %2\$d erreurs" to "the journal's length; 2 errors or more",
        "Polyrythme : ce pad repart tous les %d pas, les autres tous les %d." to "a pad's own loop differs from the pattern's 16",
        "%d / %d vérifications faites" to "a score",
        "Une valeur par temps (¼ de mesure) sur tout le morceau (%d mesures) ; elle glisse d'un point à l'autre." to "a song of 2 bars or more",
        "Une valeur par mesure sur tout le morceau (%d mesures) ; elle glisse d'un point à l'autre." to "a song of 2 bars or more",
        "La même courbe à chaque tour de boucle (%d pas)." to "a loop of 16 steps or more",
        "%d secondes autour de « %s »" to "the video's lengths (15 s or more)",
        "%d secondes de ta partie en boucle" to "the video's lengths (15 s or more)",
        "Passer à %d mesures" to "the tutorial asks for 2 bars or more",
        "%1\$s · %2\$s · %3\$d pistes · tout est fait" to "the Guide's song has its drums, bass and melody",
        "%1\$s · %2\$s · %3\$d pistes · à corriger : %4\$s" to "the Guide's song has its drums, bass and melody",
        "%1\$s · %2\$s · %3\$d pistes · étape %4\$d/%5\$d : %6\$s" to "the Guide's song has its drums, bass and melody",
        "%d pistes, toutes au centre : rien n'est placé dans la stéréo." to "said for 2 tracks or more",
        "La basse rejoue la même boucle de %1\$d mesures %2\$d fois de suite dans %3\$s." to "said for 4 times or more",
        "Des courbes durent %1\$d mesures pour un morceau de %2\$d : elles sont sans doute décalées." to "longer than a song of 1 bar or more",
        "%1\$d %2\$s sur %3\$d sont identiques." to "said for 2 or more",
        "La basse de %1\$s sur l'Acid 303 : %2\$d notes." to "a bass line found has 2 notes or more",
        "La voix de %1\$s en %2\$d notes sur une piste à elle : change son son comme tu veux." to "a melody found has 2 notes or more",
        "La basse de %1\$s en %2\$d notes sur une piste à elle : change son son comme tu veux." to "a bass line found has 2 notes or more",
        "%1\$s : %2\$d règle(s) de mélodie sur 20 à revoir (%3\$s)." to "written with (s)",
        "Couches (%1\$d/%2\$d sons)" to "the layers' limit after the slash",
        "%1\$d / %2\$d images ajoutées. « Prendre une capture » : va sur l'écran à montrer, puis touche 📸." to "the images' limit after the slash",
        "%1\$d / %2\$d images : c'est le maximum." to "the images' limit after the slash",
        "%d mesures" to "the song import's length, 4 to 64 bars (SongImportSheet); everywhere else through trCount",
        "Jouée %d fois dans le morceau. Modifier :" to "said for 2 or more (ui.played > 1)",
        "jouée %d fois" to "said for 2 or more (ui.played > 1)",
        "Une touche, un morceau complet de %1\$d à %2\$d minutes selon le style, prêt à écouter." to "a range (2 minutes or more)",
        "il y a %d jours" to "said for 2 days or more (1 has its own text)",
        "%d son(s) du projet ne sont plus sur l'appareil : le .zip part sans eux." to "written with (s)",
        "Projet importé : « %s », sans %d son(s) absent(s) du .zip." to "written with (s)",
        "%d fichier(s) illisible(s) ignoré(s)." to "written with (s)",
        "Plus que %d parties possibles : la prise s'arrêtera ensuite." to "said for 2 or more",
        "Tape tes pads l'un après l'autre (%d appris)…" to "a participle, the same in every language here",
        "Triolet : %1\$d coups réguliers sur %2\$d cases (la case d'après en fait partie)" to "a triplet: 3 hits on 2 steps or more",
        "×%1\$d : %1\$d coups dans la case, des 1/%2\$d" to "a roll: 2 hits or more",
        "triolet, %1\$d coups sur %2\$d cases" to "a triplet: 3 hits on 2 steps or more",
        "%d répétitions" to "said for 2 or more (hit.repeats > 1)",
        "%d tranches → pads %d à %d (Annuler possible)" to "said for 2 or more (count <= 1 has its own text)",
        "Tout télécharger (%1\$d packs, %2\$s)" to "the catalogue's packs (2 or more)",
        "%1\$d packs, %2\$s en tout. Ils arrivent l'un après l'autre ; si la connexion coupe, ça reprend où ça s'était arrêté." to "the catalogue's packs (2 or more)",
        "%d sons · %s" to "a pack has 2 sounds or more",
        "%d sons · %s sur l'appareil" to "a pack has 2 sounds or more",
        "%s · %d sons" to "a pack has 2 sounds or more",
        "Pack %s : %d sons sur l'appareil." to "a pack has 2 sounds or more",
        "Plus de sons %s : un pack de %d sons (%s), à télécharger seulement si tu veux." to "a pack has 2 sounds or more",
        "%d styles" to "a family has 2 styles or more",
        "Le son du pad « %s » sera remplacé par l'enregistrement de tout ce qui joue sur %d mesures : la batterie, l'Acid 303, les synthés." to "the resample's lengths (2 bars or more)",
        "Calé au tempo : %d temps à %d BPM, sans changer la hauteur" to "a loop of 2 beats or more",
        "Les %d projets de la corbeille et leurs sons seront effacés pour de bon. Impossible de revenir en arrière." to "said for 2 or more (1 has its own text)",
        "%1\$d bouts gardés dans Ma bibliothèque, dans le dossier %2\$s." to "said for 2 or more (1 has its own text)",
        "%d bouts gardés dans Ma bibliothèque." to "said for 2 or more (1 has its own text)",
        "%1\$d bouts sur la nouvelle piste audio « %2\$s », à leur place dans la chanson." to "said for 2 pieces or more (one goes alone)",
        "Copier ces %d notes" to "said for 2 or more (1 has its own text)",
        "Supprimer ces %d notes" to "said for 2 or more (1 has its own text)",
        "Partager les %d fichiers" to "said for 2 or more (1 has its own text)",
        "Voir les %d autres réglages" to "said for 2 or more (1 has its own text)",
    )

    private fun unescape(s: String) = s.replace("\\$", "$").replace("\\\"", "\"").replace("\\n", "\n")

    private fun files() = sources.flatMap { root -> root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList() }

    @Test
    fun aCountedWordGoesThroughThePluralRule() {
        val bad = mutableListOf<String>()
        for (f in files()) {
            f.readLines().forEachIndexed { i, line ->
                for (m in call.findAll(line)) {
                    val key = unescape(m.groupValues[1])
                    val words = counted.findAll(m.groupValues[1]).map { it.groupValues[1] }.filter { it !in units }.toList()
                    if (words.isEmpty() || key in never1) continue
                    bad += "${f.path}:${i + 1}: tr(\"$key\")"
                }
            }
        }
        assertTrue(
            "A number and its word through tr(): « 1 parts » in English, « 2 takty / 5 taktów » lost in Polish. Use " +
                "trCount(n, \"1 …\", \"%d …\") (a sentence with two counts: one trCount each, put in with %s), or list " +
                "the text in never1 with why its number is never 1:\n" + bad.joinToString("\n"),
            bad.isEmpty(),
        )
    }

    @Test
    fun everyCountHasItsFormsInEveryLanguage() {
        val pairs = files().flatMap { f -> pair.findAll(f.readText()).map { unescape(it.groupValues[1]) to unescape(it.groupValues[2]) }.toList() }.toSet()
        assertTrue(pairs.size > 20)
        val missing = mutableListOf<String>()
        for (lang in Lang.entries - Lang.FR) {
            val table = I18n.table(lang)
            val plurals = I18n.area(lang, "plurals")
            val extra = when (lang) { Lang.PL -> listOf("few", "many"); Lang.AR -> listOf("two", "few", "many"); else -> emptyList() }
            for ((one, other) in pairs) {
                if (one !in table) missing += "${lang.code}: « $one »"
                if (other !in table) missing += "${lang.code}: « $other »"
                // A count shown with a number (« %d … »): its other forms; one that only ever says « 1 » or a limit needs none.
                if ("%" in other) extra.filter { "$other|$it" !in plurals }.forEach { missing += "${lang.code} plurals.json: « $other|$it »" }
            }
        }
        assertTrue("Counts without all their forms:\n" + missing.joinToString("\n"), missing.isEmpty())
    }
}
