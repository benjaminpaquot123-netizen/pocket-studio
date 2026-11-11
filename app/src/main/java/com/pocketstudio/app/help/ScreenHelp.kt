package com.pocketstudio.app.help

import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Help

/**
 * Lot « Aide à jour » (2026-11-03): the « ? » of the screens that came after the Lexique (Pad Live,
 * the Live's grid and its Remix cells, « Découper la chanson », the master pro, the file's final
 * form, « Couches »). Not words of the Lexique ([Glossary]) but what a group of controls does, in
 * one to three short sentences a beginner reads at once. Each is shown by a [com.pocketstudio.ui.HelpDot]
 * that names the controls it explains (HelpCoverageTest checks every control of those screens has one).
 */
object ScreenHelp {
    // --- Pad Live ---
    val PADLIVE_PAGES: Help get() = Help(
        tr("Pages et packs"),
        tr("Pad Live a 4 pages de 16 pads, A à D : touche une lettre pour changer de page. « Pack » remplit la page affichée avec les sons d'un style ; « Annuler » la remet comme avant. L'étoile « Défi » lance un défi."),
    )
    val PADLIVE_EFFECTS: Help get() = Help(
        tr("Jouer les effets live"),
        tr("Garde le doigt sur un effet : il joue tant que tu appuies, puis tout revient. Filtre et Roulement : monte le doigt pour régler le son ou la vitesse des coups. Avec « Plus » : Montée (un souffle qui monte), Chute (la musique freine), Bégaie (le son se hache)."),
    )
    val PADLIVE_PLAYED: Help get() = Help(
        tr("Enregistrer ce que je joue"),
        tr("Enregistre ce que tu joues sur les pads, en mesures entières, puis pose-le sur un pad libre ou dans une brique du Live. REC, en haut, enregistre toute ta perf pour en faire un morceau."),
    )
    val PADLIVE_MORE: Help get() = Help(
        tr("Plus"),
        tr("Tout couper arrête tous les pads. Briques ouvre le Live complet (pistes, notes, Acid 303). Effets règle les effets des pads. Gros pads met les pads sur tout l'écran pour jouer à plusieurs. Partager fait un fichier de ta perf."),
    )
    val PADLIVE_CHALLENGE: Help get() = Help(
        tr("Défi"),
        tr("L'appli joue un petit rythme sur les pads « coup » de la page : écoute, puis refais-le. Tu gagnes jusqu'à 3 étoiles selon ta précision."),
    )
    val PADLIVE_MODES: Help get() = Help(
        tr("Coup, boucle, tenir"),
        tr("Coup : le son joue une fois à chaque toucher. Boucle : un toucher le lance en rythme, un autre l'arrête. Tenir : il joue tant que ton doigt reste sur le pad."),
    )
    val PADLIVE_GROUPS: Help get() = Help(
        tr("Un seul à la fois"),
        tr("Les pads d'un même groupe se coupent entre eux : en lancer un arrête l'autre. Pratique pour deux boucles de batterie, ou un charley ouvert et fermé."),
    )
    val PADLIVE_SHAPE: Help get() = Help(
        tr("Attaque et fin"),
        tr("Attaque : le son arrive en douceur au lieu de taper d'un coup. Fin : quand le pad s'arrête, le son s'éteint doucement au lieu de couper net."),
    )
    val TEMPO_SET: Help get() = Help(
        tr("Régler le tempo"),
        tr("Le tempo est la vitesse du morceau. Écris-le, ou touche « Taper en rythme » à chaque temps. Branché à une autre machine en MIDI, l'appli peut suivre son tempo : page MIDI, « Suivre l'horloge MIDI »."),
    )

    // --- The Live's grid ---
    val LIVE_GRID: Help get() = liveGrid(remix = true)

    /** Round 3, n° 34 (seat D d): the Remix column spoken of only where the grid has one (a project with a remix). */
    fun liveGrid(remix: Boolean): Help = Help(
        tr("Briques et cases"),
        tr("Chaque ligne est une brique (A, B, C…), chaque colonne une piste. Touche une case pour la lancer, la lettre pour lancer toute la ligne, ■ pour arrêter.") +
            if (remix) " " + tr("La colonne Remix joue un passage de la chanson : appui long sur une de ses cases pour le choisir.") else "",
    )
    val LIVE_LAUNCH: Help get() = Help(
        tr("Lancer à et Cases"),
        tr("Ce que tu lances part au prochain départ choisi (la mesure, 2 ou 4 mesures, un temps, une demi-mesure) : tout reste en rythme. « Cases − + » montre plus ou moins de briques dans la grille."),
    )
    val REMIX_CELL: Help get() = Help(
        tr("Case Remix"),
        tr("Cette case joue un passage de la chanson séparée : choisis la piste (voix, batterie…), la mesure où il commence et sa longueur. Il part sur la mesure et suit le tempo."),
    )

    // --- « Découper la chanson » ---
    val CUT_AUTO: Help get() = Help(
        tr("Coupes et aimant"),
        // « Un bout » first (Benjamin, 2026-11-04), then the auto cuts into many pieces.
        tr("Coupes : « Un bout » (au départ) donne deux barres, Début et Fin, à glisser ; ce qui est entre elles part tel quel. Phrases, Attaques ou Mesures coupent la piste en plein de bouts. Aimant : les barres et les coupes se posent sur le temps ou la mesure ; « Libre » les laisse où tu touches."),
    )
    val CUT_TOOLS: Help get() = Help(
        tr("Outils de coupe"),
        tr("Place la tête orange ou écoute, puis : Couper ici coupe à cet endroit, Début et Fin raccourcissent le bout, Enlever le retire, Boucle le fait tourner. Pince la vue pour zoomer.") +
            // Audit ultra round 3, zone 1, n° 9: how a piece is chosen, said where the tools are.
            " " + tr("Touche le numéro d'un bout, en haut de la courbe, pour le choisir, puis mets-le sur un pad ou une piste."),
    )
    val CUT_BOUT: Help get() = Help(
        tr("Un bout"),
        tr("Glisse les barres Début et Fin, ou touche la courbe puis « Début ici » ou « Fin ici » : l'aimant les pose sur les temps de la règle. Le gros ▶ joue la piste depuis la ligne orange, « Écouter le bout » juste le bout. Puis envoie-le. Coupes › Phrases, Attaques ou Mesures : plein de bouts."),
    )
    val CUT_SEND: Help get() = Help(
        tr("Mettre les bouts sur…"),
        tr("Coche les bouts que tu veux, puis mets-les : sur les pads (un par pad), dans le Live, dans « Garder » (ta bibliothèque) ou sur une piste du morceau."),
    )

    // --- Master pro and the file ---
    val MASTER_TARGET: Help get() = masterTarget(com.pocketstudio.model.LoudnessTargets.DEFAULT_LUFS)

    /**
     * Audit ultra round 2 (n° 9, seat A d: the chip said « Club · −7,5 », this help « Club (−8 LUFS) »): the
     * values of this project, the chips' own ([com.pocketstudio.model.LoudnessTargets.shown]), named as the panel names it.
     */
    fun masterTarget(styleLufs: Float): Help = com.pocketstudio.model.LoudnessTargets.let { t ->
        Help(
            tr("Volume visé"),
            tr(
                "Le volume visé, en LUFS : plus près de 0, plus fort. Une cible par projet, la même dans Mix et au partage. Auto (%1\$s LUFS, ton style) et Club (%2\$s LUFS) se mesurent sur le passage le plus fort ; Streaming (−14 LUFS), Apple Music (−16) et Libre sur tout le morceau. Si le son devait s'écraser, l'appli s'arrête avant et le dit.",
                t.formatTarget(styleLufs), t.formatTarget(t.clubLufs(styleLufs)),
            ),
        )
    }
    val MASTER_CEILING: Help get() = Help(
        tr("Plafond de crête"),
        tr("Le point le plus haut que le son a le droit de toucher. −1 dBTP garde une marge : le fichier ne grésille pas une fois compressé (M4A, streaming)."),
    )
    val MASTER_CHAIN: Help get() = Help(
        tr("La chaîne du master"),
        tr("Les effets qui finissent le morceau, dans l'ordre : égaliseur, compresseur, largeur, saturation, puis le limiteur. « Grave en mono » rend les basses solides en club. Attaque et relâchement règlent la vitesse du limiteur."),
    )
    val MASTER_AB: Help get() = Help(
        tr("Écouter A/B"),
        tr("A avec le master, B sans, pour comparer. « Au même volume » les met au même niveau : tu juges le son, pas la force. Une référence est un morceau que tu aimes, mis au même volume pour comparer."),
    )
    val PRO_READINGS: Help get() = Help(
        tr("Mesures pro"),
        tr("LUFS : le volume moyen. LRA : l'écart entre les passages calmes et forts. Crête vraie : le point le plus haut, sûr sous −1 dBTP. PLR : l'écart entre la crête et le volume ; petit, le son est très tassé."),
    )
    val FINAL_FILE: Help get() = Help(
        tr("Fichier final"),
        tr("Comment le fichier est fabriqué : fréquence et bits (48 kHz, 24 bits suffit presque toujours), la partie exportée, les fondus et silences au début et à la fin, et les infos (artiste, pochette)."),
    )
    val STEMS_SENDS: Help get() = Help(
        tr("Avec ou sans les envois"),
        tr("Avec les envois, chaque piste garde sa réverbe et son écho. Sans, ils sont dans des fichiers à part : tu les règles toi-même dans l'autre logiciel."),
    )

    // --- The pad sheet (Rythme) ---
    val LAYERS: Help get() = Help(
        tr("Couches"),
        tr("D'autres sons pour le même pad, selon la force du coup : les coups doux gardent le son du pad, les coups forts en jouent un autre. La batterie sonne plus vivante."),
    )

    // --- Where a beginner gets stuck (hands-on audit M5; Benjamin, tel-10: « le compteur « 1 » n'est pas expliqué ») ---
    val RECORD_KEYS: Help get() = Help(
        tr("Enregistrer au clavier"),
        tr("Touche le bouton rouge, puis joue : l'appli écrit tes notes dans la partie, tu les retrouves dans « Notes ». Le chiffre à côté est le décompte : les mesures entendues avant de jouer. « Garder ce que je viens de jouer » reprend ce que tu viens de jouer, sans le refaire."),
    )
    val SIMPLE_MIX: Help get() = Help(
        tr("Le Mix simple"),
        tr("Monte le curseur de ce qui doit s'entendre, baisse ce qui gêne. Sous « Sortie », un mot juge le volume final : « Bien » ou « Fort et propre », c'est prêt à partager ; « Ça sature », baisse la sortie. « Tout à zéro » remet les volumes du départ."),
    )
    val MIX_LOOP: Help get() = Help(
        tr("Partie en boucle"),
        tr("La partie en cours tourne en boucle pendant que tu règles : tu entends tout de suite ce que tu changes. « ■ Arrêter » coupe la boucle."),
    )

    /** Every one (the test checks their length and that each is shown somewhere). */
    val ALL: List<Help> get() = listOf(
        PADLIVE_PAGES, PADLIVE_EFFECTS, PADLIVE_PLAYED, PADLIVE_MORE, PADLIVE_CHALLENGE, PADLIVE_MODES, PADLIVE_GROUPS, PADLIVE_SHAPE, TEMPO_SET,
        LIVE_GRID, LIVE_LAUNCH, REMIX_CELL, CUT_AUTO, CUT_TOOLS, CUT_BOUT, CUT_SEND,
        MASTER_TARGET, MASTER_CEILING, MASTER_CHAIN, MASTER_AB, PRO_READINGS, FINAL_FILE, STEMS_SENDS, LAYERS,
        RECORD_KEYS, SIMPLE_MIX, MIX_LOOP,
    )
}
