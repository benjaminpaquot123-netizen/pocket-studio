package com.pocketstudio.bentest

import com.pocketstudio.bentest.Scenarios.Scenario
import com.pocketstudio.model.StarterWizard

/**
 * The catalogue's « screens » and « endurance » themes (tour 9, lot cat-400).
 * Screens: every tab of the studio opened while the song plays, alone, in pairs, in tours, in bursts, and the studio
 * sent behind another app from each of them. Oracle of each: the transport plays on every screen, the sound comes out
 * (no NaN), and, back from behind, the same screen is there and still plays.
 * Endurance: the repeated gestures at higher counts, and one song listened to for minutes in one go.
 */
object ScenariosEcrans {
    const val PREFIXE_ECRANS = "ecr_"
    const val PREFIXE_ENDURANCE = "end_"
    private const val GESTE = "ecr"
    private const val TOUT = "tout"

    /** The studio's tabs: short name → (the tag that opens it, the tag that says it is there; ending in « _ »: a prefix). */
    val ECRANS: Map<String, Pair<String, String>> = linkedMapOf(
        "structure" to ("tab_Structure" to "structure_"),
        "sons" to ("tab_Sons" to "track_"),
        "mix" to ("tab_Mix" to "mixer"),
        "live" to ("tab_Live" to "live_screen"),
        "plus" to ("tab_Plus" to "plus"),
    )

    private val NOMS = ECRANS.keys.toList()

    /** Tabs already crossed alone while playing by the base catalogue (« lecture_mix », « lecture_live »). */
    private val DEJA_SEULS = setOf("mix", "live")

    /** Mix ↔ Structure is the base catalogue's « lecture_va_et_vient »; Structure to the background its « arriere_plan ». */
    private val DEJA_PAIRE = setOf("mix", "structure")
    private val DEJA_FOND = setOf("structure")

    /**
     * The screens of a table line « ecr:<names, comma-separated, or tout>:<times> », in order, [times] over; null when
     * a name is unknown or the count is not a positive number.
     */
    fun parcours(g: List<String>): List<Pair<String, String>>? {
        if (g.firstOrNull() != GESTE || g.size != 3) return null
        val noms = if (g[1] == TOUT) NOMS else g[1].split(',')
        val fois = g[2].toIntOrNull()?.takeIf { it > 0 } ?: return null
        val tabs = noms.map { ECRANS[it] ?: return null }
        return List(fois) { tabs }.flatten()
    }

    private fun nom(n: String) = n.replaceFirstChar { it.uppercase() }

    private fun estime(ecrans: Int) = 12 + 2 * ecrans

    /** A long listen's estimate: the song made (about 20 s), then the listen itself. */
    private fun estimeEcoute(min: Int) = 20 + min * 60

    /** Long listens: style, minutes. */
    val LONGUES = listOf("house" to 3, "techno" to 3, "jump_up" to 3, "dnb" to 3, "house" to 10)

    /** Every other style listened to for one whole minute (the full run plays each only a few seconds). */
    val MINUTES: List<String> get() = StarterWizard.STYLES.map { it.id }.filter { st -> LONGUES.none { it.first == st } }

    private fun minute(st: String) = Scenario(
        "${PREFIXE_ENDURANCE}ecoute_${st}_1min", "Mesure : un morceau ${st.replace('_', ' ')} écouté une minute d'une traite",
        Scenarios.NIVEAU_MESURE, 420_000L + 75_000L, estimeEcoute(1),
        "la lecture chaque seconde (1 s d'arrêt au plus), ni NaN, du son la moitié du temps au moins", "end:long:$st:60",
    )

    private fun plus(geste: String, fois: Int) = when (geste) {
        "jouer" -> Scenario(
            "${PREFIXE_ENDURANCE}jouer_$fois", "Mesure : $fois fois jouer puis arrêter", Scenarios.NIVEAU_MESURE, 60_000L + fois * 8_000L, 10 + fois * 3,
            "chaque ▶ part, chaque ■ arrête ; ensuite le son, ni NaN", "end:jouer:$fois",
        )
        "fond" -> Scenario(
            "${PREFIXE_ENDURANCE}fond_$fois", "Mesure : $fois fois au fond puis devant, en jouant", Scenarios.NIVEAU_MESURE, 60_000L + fois * 25_000L, 10 + fois * 8,
            "la musique au fond et le studio revenu, chaque fois", "end:fond:$fois",
        )
        else -> Scenario(
            "${PREFIXE_ENDURANCE}partie_$fois", "Mesure : $fois fois changer de partie en jouant", Scenarios.NIVEAU_MESURE, 60_000L + fois * 15_000L, 10 + fois * 5,
            "aucun trou de plus de 200 ms, la partie choisie jouée chaque fois", "end:partie:$fois",
        )
    }

    val LISTE: List<Scenario> by lazy {
        buildList {
            for (n in NOMS.filter { it !in DEJA_SEULS }) add(
                Scenario(
                    "${PREFIXE_ECRANS}lecture_$n", "Moyen : ouvrir ${nom(n)} en jouant", Scenarios.NIVEAU_MOYEN, 90_000, estime(1),
                    "le transport joue sur ${nom(n)}, le son sort (ni NaN ni silence)", "$GESTE:$n:1",
                ),
            )
            for (i in NOMS.indices) for (k in i + 1 until NOMS.size) {
                val a = NOMS[i]
                val b = NOMS[k]
                if (setOf(a, b) == DEJA_PAIRE) continue
                add(
                    Scenario(
                        "${PREFIXE_ECRANS}paire_${a}_$b", "Moyen : 3 allers-retours ${nom(a)} ↔ ${nom(b)} en jouant", Scenarios.NIVEAU_MOYEN, 120_000, estime(6),
                        "le transport joue à chacun des 6 écrans, puis le son sort", "$GESTE:$a,$b:3",
                    ),
                )
            }
            for (fois in listOf(1, 3)) add(
                Scenario(
                    "${PREFIXE_ECRANS}tour_$fois", "${if (fois == 1) "Débutant" else "Mesure"} : les cinq onglets à la suite en jouant, $fois fois",
                    if (fois == 1) Scenarios.NIVEAU_DEBUTANT else Scenarios.NIVEAU_MESURE, 60_000L + fois * 20_000L, estime(5 * fois),
                    "le transport joue à chacun des ${5 * fois} écrans, puis le son sort", "$GESTE:$TOUT:$fois",
                ),
            )
            for (n in NOMS.filter { it !in DEJA_FOND }) add(
                Scenario(
                    "${PREFIXE_ECRANS}fond_$n", "Moyen : la musique au fond depuis ${nom(n)}, puis retour", Scenarios.NIVEAU_MOYEN, 120_000, 25,
                    "le son au fond (ni NaN ni silence), le studio revenu sur ${nom(n)}, toujours en lecture", "$GESTE:fond:$n",
                ),
            )
            add(plus("jouer", 25))
            add(plus("fond", 10))
            add(plus("partie", 20))
            for ((st, min) in LONGUES) add(
                Scenario(
                    "${PREFIXE_ENDURANCE}ecoute_${st}_${min}min", "Mesure : un morceau ${st.replace('_', ' ')} écouté $min minutes d'une traite",
                    Scenarios.NIVEAU_MESURE, 420_000L + min * 75_000L, estimeEcoute(min),
                    "la lecture chaque seconde (1 s d'arrêt au plus), ni NaN, du son la moitié du temps au moins ; les décrochages", "end:long:$st:${min * 60}",
                ),
            )
            // One-minute listens spread between the shorter ones, so no part of 50 piles them up.
            val courts = toList()
            clear()
            val minutes = MINUTES.map(::minute)
            // Listen m goes before the short one at m * courts / minutes: evenly spread over the whole theme.
            var m = 0
            courts.forEachIndexed { i, c ->
                while (m < minutes.size && m * courts.size / minutes.size <= i) add(minutes[m++])
                add(c)
            }
            while (m < minutes.size) add(minutes[m++])
        }
    }
}
