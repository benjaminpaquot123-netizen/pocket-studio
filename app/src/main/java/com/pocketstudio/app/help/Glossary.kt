package com.pocketstudio.app.help

import com.pocketstudio.ui.Help

/**
 * Every term of the app a beginner may not know, explained in plain French. Shown by the faded
 * "?" next to the term on screen, and all together in Outils › Aide › Lexique.
 */
object Glossary {
    // --- Building a song ---
    val SCENE = Help(
        "Scène",
        "Un petit bout de morceau (souvent 4 mesures) : la batterie, la basse et les notes que tu as écrites. " +
            "On passe d'une scène à l'autre en touchant son carré ; la suivante démarre à la fin de la mesure en cours.",
    )
    val SONG = Help(
        "Morceau",
        "Les scènes mises bout à bout, chacune répétée autant de fois que tu veux. " +
            "Dans la ligne de temps, chaque carré allume ou coupe une piste dans une partie du morceau.",
    )
    val LIVE_PADS = Help(
        "Pads live",
        "Des sons déjà enregistrés (boucles, voix, bouts de morceau) que tu lances et arrêtes d'un toucher, par-dessus ta scène. " +
            "Ils restent calés sur le tempo, comme chez un DJ.",
    )
    val LOOP = Help(
        "Boucle",
        "Le son recommence tout seul, calé sur le tempo, jusqu'à ce que tu l'arrêtes. « Une fois » le joue seulement une fois par toucher.",
    )
    val TEMPO = Help(
        "Tempo (BPM)",
        "La vitesse du morceau, en battements par minute. Vers 90 : hip-hop posé ; 125 : house ; 174 : drum & bass.",
    )
    val BAR = Help(
        "Mesure",
        "Un groupe de temps, en général 4 (« 1, 2, 3, 4 »). La grille de batterie montre une mesure découpée en 16 cases.",
    )
    val STEP = Help(
        "Pas",
        "Une case de la grille : un seizième de mesure. Allumer un pas fait sonner le son à ce moment-là. " +
            "Coups : allumer ou éteindre. Roulements : plusieurs coups rapides dans la case. Pan : à gauche ou à droite. Hauteur : plus aigu ou plus grave.",
    )
    val SWING = Help(
        "Swing",
        "Retarde un peu un pas sur deux : le rythme devient chaloupé au lieu d'être carré.",
    )
    val METRONOME = Help(
        "Métronome et décompte",
        "Le métronome fait un petit clic sur chaque temps pour jouer en rythme ; on ne l'entend jamais dans l'export. " +
            "Le décompte ajoute une mesure de clics avant le départ, pour se préparer.",
    )
    val COUNT_IN = Help(
        "Décompte",
        "Une mesure de clics avant le départ, pour se préparer à jouer ou à enregistrer.",
    )
    val TIME_SIGNATURE = Help(
        "Temps par mesure",
        "4 temps par mesure pour presque toute la musique actuelle ; 3 pour une valse ; 5 ou 7 pour des rythmes boiteux.",
    )
    val TIMELINE = Help(
        "Ligne de temps",
        "Une colonne par partie du morceau, une ligne par piste : un carré allumé, la piste joue dans cette partie ; éteint, elle se tait.",
    )

    // --- Drums and sounds ---
    val PAD = Help(
        "Pad",
        "Un des 16 carrés de la batterie. Chacun a son son (kick, caisse claire, charley…), qu'on peut changer.",
    )
    val VELOCITY = Help(
        "Force (vélocité)",
        "La force d'un coup ou d'une note : plus c'est fort, plus ça tape. Appui long sur une case pour la changer.",
    )
    val PAN = Help(
        "Pan",
        "Place le son à gauche ou à droite dans le casque. Au centre, il est des deux côtés.",
    )
    val PITCH = Help(
        "Hauteur",
        "Monte ou descend le son, en demi-tons, comme d'une touche à l'autre sur un clavier. +12 = une octave plus haut.",
    )
    val SAMPLE = Help(
        "Échantillon",
        "Un son enregistré qu'on rejoue : un kick, une voix, un bout de morceau. On dit aussi « sample ».",
    )
    val ROLL = Help(
        "Roulement",
        "Plusieurs coups rapides dans une seule case (2, 3 ou 4), pour les charleys en rafale ou les montées.",
    )
    val FIT_TEMPO = Help(
        "Calé au tempo",
        "Le son est étiré ou resserré pour durer exactement le nombre de temps choisi, même si tu changes le tempo.",
    )
    val SLICE = Help(
        "Découper en tranches",
        "Coupe un son en morceaux, à chaque attaque, et pose chaque morceau sur un pad pour le rejouer dans un autre ordre.",
    )
    val ENVELOPE = Help(
        "Enveloppe",
        "Comment le son démarre et s'éteint : l'attaque (la montée), le déclin, le maintien (le niveau tenu) et le relâchement (la fin).",
    )
    val CHOKE = Help(
        "Groupe d'étouffement",
        "Les pads d'un même groupe se coupent entre eux, comme le charley fermé qui arrête l'ouvert.",
    )
    val SOUND_LIBRARY = Help(
        "Bibliothèque de sons",
        "Tous les sons de l'appli rangés par famille (kick, caisse claire, basse…) : les sons intégrés et ceux des packs téléchargés.",
    )
    val SOUND_PACK = Help(
        "Pack de sons",
        "Une collection de sons pour un style (techno, UK garage, trap…). À télécharger seulement si tu veux, et à supprimer quand tu veux.",
    )
    val KIT = Help(
        "Kit",
        "Les 16 sons de la batterie ensemble. Changer de kit change tous les pads d'un coup.",
    )

    // --- Notes, harmony and instruments ---
    val PIANO_ROLL = Help(
        "Piano roll",
        "La grille des notes : de gauche à droite le temps, de bas en haut la hauteur. Touche pour poser une note, glisse son bout pour l'allonger.",
    )
    val SCALE = Help(
        "Gamme",
        "Les 7 notes qui vont bien ensemble, par exemple Do majeur (joyeux) ou La mineur (plus triste). Le clavier montre celles qui sonnent juste.",
    )
    val SCALE_LOCK = Help(
        "Verrouiller la gamme",
        "Les notes hors de la gamme ne jouent plus : impossible de faire une fausse note.",
    )
    val CHORD = Help(
        "Accord",
        "Plusieurs notes jouées ensemble, souvent trois. C'est la couleur du morceau : joyeuse, triste, mystérieuse…",
    )
    val PROGRESSION = Help(
        "Progression",
        "Une suite d'accords qui tourne en boucle, comme I–V–vi–IV qu'on retrouve dans des centaines de chansons.",
    )
    val ARPEGGIO = Help(
        "Arpège",
        "Les notes d'un accord jouées une par une, en montant ou en descendant, au rythme du morceau.",
    )
    val QUANTIZE = Help(
        "Quantifier",
        "Recale exactement sur la grille les notes jouées un peu à côté.",
    )
    val TRACK = Help(
        "Piste",
        "Une ligne du morceau avec son propre instrument et ses notes : la basse, la nappe, le lead… Chacune a sa voie au mixeur.",
    )
    val SOURCE = Help(
        "Source du son",
        "D'où vient le son de la piste. Synthé : un synthé classique qui fabrique le son. Plaits : un synthé aux 24 modèles. " +
            "Échantillon : un son enregistré joué sur tout le clavier (808, basses, stabs des packs). Instruments : 128 instruments classiques (piano, cordes…).",
    )
    val SYNTH = Help(
        "Synthé",
        "Un instrument électronique qui fabrique son son (oscillateurs, filtre) au lieu de rejouer un enregistrement.",
    )
    val PLAITS = Help(
        "Plaits",
        "Un synthé aux 24 modèles (cordes, cloches, FM, voix, percussions…), repris d'un module de synthé célèbre.",
    )
    val GENERAL_MIDI = Help(
        "Instruments General MIDI",
        "Une banque de 128 instruments classiques (piano, guitare, cordes, cuivres…), téléchargée une fois.",
    )
    val SAMPLED = Help(
        "Son échantillonné",
        "Un son enregistré joué sur tout le clavier : plus la touche est haute, plus il monte. Idéal pour les 808, basses et stabs des packs.",
    )
    val ROOT_NOTE = Help(
        "Note d'origine",
        "La note à laquelle le son a été enregistré. L'appli la trouve toute seule ; corrige-la si le son joue faux.",
    )
    val UNISON = Help(
        "Unisson",
        "Plusieurs copies du son, très légèrement désaccordées, jouées ensemble : le son devient large et brillant. " +
            "C'est le « supersaw » de la trance et de la future bass, et la basse Reese de la drum & bass.",
    )
    val GLIDE = Help(
        "Glisse",
        "La note glisse vers la suivante au lieu de sauter, quand tu tiens une touche et en appuies une autre. Le fameux effet des 808 du trap.",
    )
    val ACID = Help(
        "Basse acide",
        "Le son de la Roland TB-303 qui a fait naître l'acid house : une basse qui couine quand on ouvre le filtre.",
    )
    val FILTER = Help(
        "Filtre",
        "Rend le son plus sourd (fermé) ou plus brillant (ouvert). La résonance le fait siffler autour de sa fréquence.",
    )
    val ACCENT_SLIDE = Help(
        "Accent et glisse",
        "Accent : la note tape plus fort et plus brillante. Glisse : elle se lie à la suivante en glissant, typique de l'acide.",
    )
    val MIDI = Help(
        "MIDI",
        "La langue des claviers et des contrôleurs : branche un clavier pour jouer les instruments de l'appli.",
    )
    val MIDI_LEARN = Help(
        "MIDI learn",
        "Touche un réglage puis tourne un bouton de ton contrôleur : ce bouton pilotera ce réglage.",
    )
    val BEND = Help(
        "Pitch bend",
        "Fait monter ou descendre la note en glissant, puis revient au centre quand on lâche.",
    )
    val SUSTAIN = Help(
        "Sustain",
        "Comme la pédale du piano : les notes continuent de sonner après avoir lâché les touches.",
    )

    // --- Mixer and effects ---
    val MIXER = Help(
        "Mixeur",
        "Le volume, la place gauche-droite et les effets de chaque piste, et la sortie générale.",
    )
    val MUTE_SOLO = Help(
        "Muet et solo",
        "Muet coupe la piste. Solo n'écoute qu'elle (et les autres pistes en solo).",
    )
    val INSERT = Help(
        "Effet de piste",
        "Un effet branché directement sur une piste : saturation, filtre, compresseur… Jusqu'à quatre par piste.",
    )
    val SEND = Help(
        "Envoi",
        "La part du son d'une piste envoyée vers la réverbe ou l'écho, partagés par toutes les pistes.",
    )
    val REVERB = Help(
        "Réverbe",
        "L'écho naturel d'une pièce : petite salle, grande cathédrale… Le son paraît plus loin et plus large.",
    )
    val DELAY = Help(
        "Écho",
        "Le son se répète en rythme, de plus en plus doucement.",
    )
    val COMPRESSOR = Help(
        "Compresseur",
        "Rapproche les passages forts et faibles : le son est plus serré et plus présent.",
    )
    val EQ = Help(
        "Égaliseur (EQ)",
        "Monte ou baisse les graves, les médiums et les aigus d'un son.",
    )
    val MACROS = Help(
        "Macros",
        "Quatre gros réglages par piste : Couleur (sombre ou brillant), Saleté (saturation), Mouvement (chorus) et Espace (réverbe et écho).",
    )
    val XY_PAD = Help(
        "Pad XY",
        "Un carré qu'on touche et où l'on glisse le doigt : il règle deux macros à la fois, une par direction.",
    )
    val FX_PRESET = Help(
        "Ambiance d'effets",
        "Un réglage tout prêt (Sous l'eau, Lo-Fi Tape, Grande cathédrale…) qui pose plusieurs effets d'un coup.",
    )
    val AUTOMATION = Help(
        "Automation",
        "Un réglage qui bouge tout seul pendant la lecture : le volume qui monte, le filtre qui s'ouvre… On le dessine point par point.",
    )
    val PERFORMANCE = Help(
        "Effets live",
        "Des effets à jouer en direct sur tout le morceau : filtre, répétition, arrêt de bande, hachage.",
    )
    val LIMITER = Help(
        "Limiteur",
        "Empêche la sortie générale de saturer quand tout joue fort.",
    )
    val SPECTRUM = Help(
        "Analyseur",
        "Montre où est l'énergie du son : les graves à gauche, les aigus à droite.",
    )

    // --- Export and audio ---
    val FORMATS = Help(
        "WAV, FLAC, M4A",
        "WAV : qualité studio, lu partout, mais lourd. FLAC : exactement la même qualité, bien plus léger (souvent deux fois). M4A : environ dix fois plus léger, pour partager.",
    )
    val LUFS = Help(
        "Volume perçu (LUFS)",
        "Le volume tel que l'oreille le ressent, mesuré comme le font Spotify ou YouTube. Vers −14 : le niveau du streaming ; plus près de 0 : plus fort.",
    )
    val STEMS = Help(
        "Piste par piste",
        "Un fichier par piste (batterie, basse, synthé…), pour continuer le morceau dans un autre logiciel.",
    )
    val EXPORT_MIDI = Help(
        "Fichier MIDI",
        "Seulement les notes, sans le son : un autre logiciel peut les rejouer avec ses propres instruments.",
    )
    val REMIX = Help(
        "Remix",
        "L'appli sépare une chanson en 4 pistes (batterie, basse, voix, le reste) pour en jouer des bouts avec ton projet.",
    )
    val AUDIO_TRACK = Help(
        "Piste audio",
        "Une piste qui joue un enregistrement (micro ou fichier) à partir d'un endroit du morceau.",
    )
    val GENERATE = Help(
        "Générer un son",
        "Une IA fabrique un son à partir d'une phrase (« kick de techno sombre »), directement sur le téléphone.",
    )
    val TUNER = Help(
        "Accordeur",
        "Montre la note que tu chantes ou joues, et de combien elle est trop haute ou trop basse.",
    )
    val LATENCY = Help(
        "Latence",
        "Le petit retard entre ton toucher et le son. Plus il est bas, mieux on joue en direct.",
    )

    /** Every term, in the order the Lexique shows them. */
    val ALL: List<Help> = listOf(
        SCENE, SONG, TIMELINE, LIVE_PADS, LOOP, TEMPO, BAR, STEP, SWING, METRONOME, COUNT_IN, TIME_SIGNATURE,
        PAD, KIT, VELOCITY, PAN, PITCH, SAMPLE, ROLL, FIT_TEMPO, SLICE, ENVELOPE, CHOKE, SOUND_LIBRARY, SOUND_PACK,
        TRACK, SOURCE, PIANO_ROLL, SCALE, SCALE_LOCK, CHORD, PROGRESSION, ARPEGGIO, QUANTIZE, SYNTH, PLAITS, GENERAL_MIDI,
        UNISON, SAMPLED, ROOT_NOTE, GLIDE, ACID, FILTER, ACCENT_SLIDE, MIDI, MIDI_LEARN, BEND, SUSTAIN,
        MIXER, MUTE_SOLO, INSERT, SEND, REVERB, DELAY, COMPRESSOR, EQ, MACROS, XY_PAD, FX_PRESET, AUTOMATION, PERFORMANCE,
        LIMITER, SPECTRUM, FORMATS, LUFS, STEMS, EXPORT_MIDI, REMIX, AUDIO_TRACK, GENERATE, TUNER, LATENCY,
    )
}
