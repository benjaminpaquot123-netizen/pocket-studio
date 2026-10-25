package com.pocketstudio.app.help

import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Help

/**
 * Every term of the app a beginner may not know, explained in plain words, in the interface's language. Shown by the faded
 * "?" next to the term on screen, and all together in Outils › Aide › Lexique.
 */
object Glossary {
    // --- Building a song ---
    val SCENE: Help get() = Help(
        tr("Partie"),
        tr("Un petit bout de morceau (souvent 4 mesures) : la batterie, la basse et les notes que tu as écrites. Le morceau enchaîne les parties ; on passe de l'une à l'autre en touchant son carré, la suivante démarre à la fin de la mesure en cours."),
    )
    /** Lot 2: the grid of A to H, apart from the song's parts. */
    val BRICK: Help get() = Help(
        tr("Brique"),
        tr("Une des cases A à H de Structure › Parties : une boucle de batterie, de basse et de notes. Chaque partie du morceau joue une brique (le refrain peut rejouer la brique du couplet) ; changer une brique change toutes les parties qui la jouent."),
    )
    val SONG: Help get() = Help(
        tr("Morceau"),
        tr("Les parties mises bout à bout, chacune répétée autant de fois que tu veux. Dans la ligne du temps, chaque carré allume ou coupe une piste dans une partie."),
    )
    val LIVE_PADS: Help get() = Help(
        tr("Pads live"),
        tr("Des sons déjà enregistrés (boucles, voix, bouts de morceau) que tu lances et arrêtes d'un toucher, par-dessus ta partie. Ils restent calés sur le tempo, comme chez un DJ."),
    )
    val LOOP: Help get() = Help(
        tr("Boucle"),
        tr("Le son recommence tout seul, calé sur le tempo, jusqu'à ce que tu l'arrêtes. « Une fois » le joue seulement une fois par toucher."),
    )
    val TEMPO: Help get() = Help(
        tr("Tempo (BPM)"),
        tr("La vitesse du morceau, en battements par minute. Vers 90 : hip-hop posé ; 125 : house ; 174 : drum & bass."),
    )
    val BAR: Help get() = Help(
        tr("Mesure"),
        tr("Un groupe de temps, en général 4 (« 1, 2, 3, 4 »). La grille de batterie montre une mesure découpée en 16 cases."),
    )
    val STEP: Help get() = Help(
        tr("Pas"),
        tr("Une case de la grille : un seizième de mesure. Allumer un pas fait sonner le son à ce moment-là. Coups : allumer ou éteindre. Roulements : plusieurs coups rapides dans la case. Pan : à gauche ou à droite. Hauteur : plus aigu ou plus grave."),
    )
    val SWING: Help get() = Help(
        tr("Swing"),
        tr("Retarde un peu un pas sur deux : le rythme devient chaloupé au lieu d'être carré."),
    )
    val METRONOME: Help get() = Help(
        tr("Métronome et décompte"),
        tr("Le métronome fait un petit clic sur chaque temps pour jouer en rythme ; on ne l'entend jamais dans l'export. Le décompte ajoute une mesure de clics avant le départ, pour se préparer."),
    )
    val COUNT_IN: Help get() = Help(
        tr("Décompte"),
        tr("Une mesure de clics avant le départ, pour se préparer à jouer ou à enregistrer."),
    )
    val TIME_SIGNATURE: Help get() = Help(
        tr("Temps par mesure"),
        tr("4 temps par mesure pour presque toute la musique actuelle ; 3 pour une valse ; 5 ou 7 pour des rythmes boiteux."),
    )
    val TIMELINE: Help get() = Help(
        tr("Ligne du temps"),
        tr("Une colonne par partie du morceau, une ligne par piste : un carré allumé, la piste joue dans cette partie ; éteint, elle se tait."),
    )

    // --- Drums and sounds ---
    val PAD: Help get() = Help(
        tr("Pad"),
        tr("Un des 16 carrés de la batterie. Chacun a son son (kick, caisse claire, charley…), qu'on peut changer."),
    )
    val VELOCITY: Help get() = Help(
        tr("Force (vélocité)"),
        tr("La force d'un coup ou d'une note : plus c'est fort, plus ça tape. Dans Rythme, outil « Force » ou appui long sur une case."),
    )
    val PAN: Help get() = Help(
        tr("Pan"),
        tr("Place le son à gauche ou à droite dans le casque. Au centre, il est des deux côtés."),
    )
    val PITCH: Help get() = Help(
        tr("Hauteur"),
        tr("Monte ou descend le son, en demi-tons, comme d'une touche à l'autre sur un clavier. +12 = une octave plus haut."),
    )
    val SAMPLE: Help get() = Help(
        tr("Sample du pad"),
        tr("Le son que joue le pad quand on le touche. Un « sample » (échantillon en français), c'est un son enregistré qu'on rejoue : un kick, une voix, un bout de morceau. Prends-le dans la bibliothèque, importe-le ou enregistre-le au micro ; sans sample, le pad joue le son de la boîte à rythmes."),
    )
    val ROLL: Help get() = Help(
        tr("Roulement"),
        tr("Plusieurs coups rapides dans une seule case (2, 3 ou 4), pour les charleys en rafale ou les montées."),
    )
    val FIT_TEMPO: Help get() = Help(
        tr("Calé au tempo"),
        tr("Le son est étiré ou resserré pour durer exactement le nombre de temps choisi, même si tu changes le tempo."),
    )
    val SLICE: Help get() = Help(
        tr("Découper en tranches"),
        tr("Coupe un son en morceaux, à chaque attaque, et pose chaque morceau sur un pad pour le rejouer dans un autre ordre."),
    )
    val ENVELOPE: Help get() = Help(
        tr("Enveloppe"),
        tr("Comment le son démarre et s'éteint : l'attaque (la montée), le déclin, le maintien (le niveau tenu) et le relâchement (la fin)."),
    )
    val CHOKE: Help get() = Help(
        tr("Groupe d'étouffement"),
        tr("Les pads d'un même groupe se coupent entre eux, comme le charley fermé qui arrête l'ouvert."),
    )
    val SOUND_LIBRARY: Help get() = Help(
        tr("Bibliothèque de sons"),
        tr("Tous les sons de l'appli rangés par famille (kick, caisse claire, basse…) : les sons intégrés et ceux des packs téléchargés."),
    )
    val SOUND_PACK: Help get() = Help(
        tr("Pack de sons"),
        tr("Une collection de sons pour un style (techno, UK garage, trap…), une machine célèbre (TR-808, Juno, DX7…) ou des sons actuels. À télécharger seulement si tu veux, et à supprimer quand tu veux."),
    )
    val KIT: Help get() = Help(
        tr("Kit"),
        tr("Les 16 sons de la batterie ensemble. Changer de kit change tous les pads d'un coup."),
    )

    // --- Notes, harmony and instruments ---
    val PIANO_ROLL: Help get() = Help(
        tr("Piano roll"),
        tr("La grille des notes : de gauche à droite le temps, de bas en haut la hauteur. Touche pour poser une note, glisse son bout pour l'allonger."),
    )
    val SCALE: Help get() = Help(
        tr("Gamme"),
        tr("Les 7 notes qui vont bien ensemble, par exemple Do majeur (joyeux) ou La mineur (plus triste). Le clavier montre celles qui sonnent juste."),
    )
    val SCALE_LOCK: Help get() = Help(
        tr("Verrouiller la gamme"),
        tr("Les notes hors de la gamme ne jouent plus : impossible de faire une fausse note."),
    )
    val CHORD: Help get() = Help(
        tr("Accord"),
        tr("Plusieurs notes jouées ensemble, souvent trois. C'est la couleur du morceau : joyeuse, triste, mystérieuse…"),
    )
    val PROGRESSION: Help get() = Help(
        tr("Progression"),
        tr("Une suite d'accords qui tourne en boucle, comme I–V–vi–IV qu'on retrouve dans des centaines de chansons."),
    )
    val ARPEGGIO: Help get() = Help(
        tr("Arpège"),
        tr("Les notes d'un accord jouées une par une, en montant ou en descendant, au rythme du morceau."),
    )
    val QUANTIZE: Help get() = Help(
        tr("Quantifier"),
        tr("Recale exactement sur la grille les notes jouées un peu à côté."),
    )
    val TRACK: Help get() = Help(
        tr("Piste"),
        tr("Une ligne du morceau avec son propre instrument et ses notes : la basse, la nappe, le lead… Chacune a sa voie au mixeur."),
    )
    val SOURCE: Help get() = Help(
        tr("Instrument"),
        tr("Ce qui joue les notes de la piste. Synthé : un synthé classique qui fabrique le son. Plaits : un synthé aux 24 modèles. Sample : un son enregistré joué sur tout le clavier (808, basses, stabs des packs). Instruments : 128 instruments classiques (piano, cordes…)."),
    )
    val SYNTH: Help get() = Help(
        tr("Synthé"),
        tr("Un instrument électronique qui fabrique son son (oscillateurs, filtre) au lieu de rejouer un enregistrement."),
    )
    val PLAITS: Help get() = Help(
        tr("Plaits"),
        tr("Un synthé aux 24 modèles (cordes, cloches, FM, voix, percussions…), repris d'un module de synthé célèbre."),
    )
    val GENERAL_MIDI: Help get() = Help(
        tr("Instruments General MIDI"),
        tr("Une banque de 128 instruments classiques (piano, guitare, cordes, cuivres…), téléchargée une fois."),
    )
    val SAMPLED: Help get() = Help(
        tr("Sample au clavier"),
        tr("Un son enregistré joué sur tout le clavier : plus la touche est haute, plus il monte. Idéal pour les 808, basses et stabs des packs."),
    )
    val ROOT_NOTE: Help get() = Help(
        tr("Note d'origine"),
        tr("La note à laquelle le son a été enregistré. L'appli la trouve toute seule ; corrige-la si le son joue faux."),
    )
    val UNISON: Help get() = Help(
        tr("Unisson"),
        tr("Plusieurs copies du son, très légèrement désaccordées, jouées ensemble : le son devient large et brillant. C'est le « supersaw » de la trance et de la future bass, et la basse Reese de la drum & bass."),
    )
    val GLIDE: Help get() = Help(
        tr("Glisse"),
        tr("La note glisse vers la suivante au lieu de sauter, quand tu tiens une touche et en appuies une autre. Le fameux effet des 808 du trap."),
    )
    val ACID: Help get() = Help(
        tr("Basse acide"),
        tr("Le son de la Roland TB-303 qui a fait naître l'acid house : une basse qui couine quand on ouvre le filtre."),
    )
    val FILTER: Help get() = Help(
        tr("Filtre"),
        tr("Rend le son plus sourd (fermé) ou plus brillant (ouvert). La résonance le fait siffler autour de sa fréquence."),
    )
    val ACCENT_SLIDE: Help get() = Help(
        tr("Accent et glisse"),
        tr("Accent : la note tape plus fort et plus brillante. Glisse : elle se lie à la suivante en glissant, typique de l'acide."),
    )
    val MIDI: Help get() = Help(
        tr("MIDI"),
        tr("La langue des claviers et des contrôleurs : branche un clavier pour jouer les instruments de l'appli."),
    )
    val MIDI_LEARN: Help get() = Help(
        tr("MIDI learn"),
        tr("Touche un réglage puis tourne un bouton de ton contrôleur : ce bouton pilotera ce réglage."),
    )
    val BEND: Help get() = Help(
        tr("Pitch bend"),
        tr("Fait monter ou descendre la note en glissant, puis revient au centre quand on lâche."),
    )
    val SUSTAIN: Help get() = Help(
        tr("Sustain"),
        tr("Comme la pédale du piano : les notes continuent de sonner après avoir lâché les touches."),
    )

    // --- Mixer and effects ---
    val MIXER: Help get() = Help(
        tr("Mixeur"),
        tr("Le volume, la place gauche-droite et les effets de chaque piste, et la sortie générale."),
    )
    val MUTE_SOLO: Help get() = Help(
        tr("Muet et solo"),
        tr("Muet coupe la piste. Solo n'écoute qu'elle (et les autres pistes en solo)."),
    )
    val INSERT: Help get() = Help(
        tr("Effet de piste"),
        tr("Un effet branché directement sur une piste : saturation, filtre, compresseur… Jusqu'à quatre par piste."),
    )
    val SEND: Help get() = Help(
        tr("Envoi"),
        tr("La part du son d'une piste envoyée vers la réverbe ou l'écho, partagés par toutes les pistes."),
    )
    val REVERB: Help get() = Help(
        tr("Réverbe"),
        tr("L'écho naturel d'une pièce : petite salle, grande cathédrale… Le son paraît plus loin et plus large."),
    )
    val DELAY: Help get() = Help(
        tr("Écho"),
        tr("Le son se répète en rythme, de plus en plus doucement."),
    )
    val COMPRESSOR: Help get() = Help(
        tr("Compresseur"),
        tr("Rapproche les passages forts et faibles : le son est plus serré et plus présent."),
    )
    val EQ: Help get() = Help(
        tr("Égaliseur (EQ)"),
        tr("Monte ou baisse les graves, les médiums et les aigus d'un son."),
    )
    val MACROS: Help get() = Help(
        tr("Macros"),
        tr("Quatre gros réglages par piste : Couleur (sombre ou brillant), Saleté (saturation), Mouvement (chorus) et Espace (réverbe et écho)."),
    )
    val XY_PAD: Help get() = Help(
        tr("Pad XY"),
        tr("Un carré qu'on touche et où l'on glisse le doigt : il règle deux macros à la fois, une par direction."),
    )
    val FX_PRESET: Help get() = Help(
        tr("Ambiance d'effets"),
        tr("Un réglage tout prêt (Sous l'eau, Lo-Fi Tape, Grande cathédrale…) qui pose plusieurs effets d'un coup."),
    )
    val AUTOMATION: Help get() = Help(
        tr("Automation"),
        tr("Un réglage qui bouge tout seul pendant la lecture : le volume qui monte, le filtre qui s'ouvre… On le dessine point par point."),
    )
    val PERFORMANCE: Help get() = Help(
        tr("Effets live"),
        tr("Des effets à jouer en direct sur tout le morceau : filtre, répétition, arrêt de bande, hachage."),
    )
    val MASTER: Help get() = Help(
        tr("Sortie générale"),
        tr("Tout le morceau mélangé, tel qu'il sort de l'appareil. Son limiteur l'empêche de saturer quand tout joue fort."),
    )
    val REVERB_DELAY: Help get() = Help(
        tr("Réverbe et écho"),
        tr("Deux effets partagés par toutes les pistes : la réverbe donne l'impression d'une salle, l'écho répète le son. Chaque piste y envoie plus ou moins de son (son « envoi »)."),
    )
    val HARMONY: Help get() = Help(
        tr("Gamme et accords"),
        tr("La tonalité du morceau (par exemple La mineur) et les accords qui vont avec. Le clavier montre les notes qui sonnent juste ; verrouillé, il ne joue plus qu'elles."),
    )
    val ACID_STEP: Help get() = Help(
        tr("Pas de la basse"),
        tr("Une case de la ligne de basse : une note ou un silence. Accent : elle tape plus fort et plus brillante. Slide : elle se lie à la suivante en glissant."),
    )
    val LIMITER: Help get() = Help(
        tr("Limiteur"),
        tr("Empêche la sortie générale de saturer quand tout joue fort."),
    )
    val SPECTRUM: Help get() = Help(
        tr("Analyseur"),
        tr("Montre où est l'énergie du son : les graves à gauche, les aigus à droite."),
    )

    // --- Export and audio ---
    val FORMATS: Help get() = Help(
        tr("WAV, FLAC, M4A"),
        tr("WAV : qualité studio, lu partout, mais lourd. FLAC : exactement la même qualité, bien plus léger (souvent deux fois). M4A : environ dix fois plus léger, pour partager."),
    )
    val LUFS: Help get() = Help(
        tr("Volume perçu (LUFS)"),
        tr("Le volume tel que l'oreille le ressent, mesuré comme le font Spotify ou YouTube. Vers −14 : le niveau du streaming ; plus près de 0 : plus fort."),
    )
    val STEMS: Help get() = Help(
        tr("Piste par piste"),
        tr("Un fichier par piste (batterie, basse, synthé…), pour continuer le morceau dans un autre logiciel."),
    )
    val EXPORT_MIDI: Help get() = Help(
        tr("Fichier MIDI"),
        tr("Seulement les notes, sans le son : un autre logiciel peut les rejouer avec ses propres instruments."),
    )
    val REMIX: Help get() = Help(
        tr("Remix"),
        tr("L'appli sépare une chanson en 4 pistes (batterie, basse, voix, le reste) pour en jouer des bouts avec ton projet."),
    )
    val AUDIO_TRACK: Help get() = Help(
        tr("Piste audio"),
        tr("Une piste qui joue un enregistrement (micro ou fichier) : une fois à chaque passage de sa partie, ou d'un bout à l'autre du morceau. Enregistre avec des écouteurs."),
    )
    val GENERATE: Help get() = Help(
        tr("Générer un son"),
        tr("Une IA fabrique un son à partir d'une phrase (« kick de techno sombre »), directement sur l'appareil."),
    )
    val TUNER: Help get() = Help(
        tr("Accordeur"),
        tr("Montre la note que tu chantes ou joues, et de combien elle est trop haute ou trop basse."),
    )
    val LATENCY: Help get() = Help(
        tr("Latence"),
        tr("Le petit retard entre ton toucher et le son. Plus il est bas, mieux on joue en direct."),
    )

    /** Every term, in the order the Lexique shows them. */
    val ALL: List<Help> get() = listOf(
        SCENE, BRICK, SONG, TIMELINE, LIVE_PADS, LOOP, TEMPO, BAR, STEP, SWING, METRONOME, COUNT_IN, TIME_SIGNATURE,
        PAD, KIT, VELOCITY, PAN, PITCH, SAMPLE, ROLL, FIT_TEMPO, SLICE, ENVELOPE, CHOKE, SOUND_LIBRARY, SOUND_PACK,
        TRACK, SOURCE, PIANO_ROLL, SCALE, SCALE_LOCK, CHORD, PROGRESSION, ARPEGGIO, QUANTIZE, SYNTH, PLAITS, GENERAL_MIDI,
        UNISON, SAMPLED, ROOT_NOTE, GLIDE, ACID, FILTER, ACCENT_SLIDE, MIDI, MIDI_LEARN, BEND, SUSTAIN,
        MIXER, MUTE_SOLO, INSERT, SEND, REVERB, DELAY, COMPRESSOR, EQ, MACROS, XY_PAD, FX_PRESET, AUTOMATION, PERFORMANCE,
        MASTER, REVERB_DELAY, HARMONY, ACID_STEP, LIMITER, SPECTRUM, FORMATS, LUFS, STEMS, EXPORT_MIDI, REMIX, AUDIO_TRACK, GENERATE, TUNER, LATENCY,
    )
}
