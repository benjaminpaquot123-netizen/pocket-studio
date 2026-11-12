# Lot B, Pad Live (n° 25) : note de suivi

## Fait : B1, le cœur (2026-11-03, branche fx-b)
- Accueil : carte « Pad Live · NOUVEAU » sous « Remix » (`onboarding/LaunchChoice.kt`). Un toucher : le projet « Live » est créé (la 1re fois) ou repris (`ProjectInfo.padLive`, le plus récent), Pad Live s'ouvre en plein écran (`AppRoot` : `showPadLive`, `PadLiveRoute`), et son premier coup sonne dès qu'il est chargé (`PadLiveController.welcome`). « Continuer » sur un projet « Live » rouvre Pad Live.
- Écran (`padlive/PadLiveScreen.kt`, `PadLivePages.kt`, `PadModes.kt`) conforme à la maquette `padlive-trois.png` : ← Pad Live, ▶/■, − 124 +, ● REC (prise du Live, gardée) ; pages A B C D ; « Pack : Fête ▾ » ; 16 pads ; Filtre (glisser grave ↔ aigu), Écho, Roulement ; « Plus » : Montée, Chute, Bégaie, Tout couper, Annuler, Briques, Effets. Défi et « Enregistrer ce que je joue » : B3 et B2.
- Modes de pad (coup, boucle, tenir), groupes « un seul à la fois » (1 à 4, toutes pages), réglages par pad (coin du pad ; appui long pour TalkBack) : longueur, volume, changer le son, vider. Pad vide : le navigateur unique (pages B à D : `PadLiveController.placeSound`).
- Packs (`M/PadLive.kt`) : Fête (le jeu prêt à jouer existant), Techno, Trap, House, Drum & bass : boucles rendues au tempo du projet depuis la recette du style (`StarterWizard` + `LivePadPlan`, moteur privé hors ligne) + 4 sons de fête ; posés sur la page affichée en une étape d'annulation.
- Modèle : `LivePads.more` (pages B à D), `LiveClip.hold` et `group`, `Project.padLive` (`PadLiveSet` : home, page, pack), en fin de fichier, valeurs par défaut absentes du fichier, sans nouveau numéro de format. `ProjectInfo.padLive`.
- Moteur (inévitable pour les pages) : `ClipLauncher::kSlots` 16 → 64, aucune nouvelle commande ; le rendu ne parcourt que les emplacements jusqu'au dernier qui sonne. Banc `m17_worst_case_live_cpu_bench` mesuré avant/après en alternance : même moyenne (≈ 20 %), toujours à la limite p99 sur ce conteneur (déjà noté dans REVIEW, pas de recul).
- Règle de design partout : composant unique `ui-components/PadLight.kt` (lumière intérieure, anneau d'avancement, onde 200 ms, voile d'effet maintenu ; rien sur un pad inactif). Branché sur : Rythme (pads, pas, « Tous les pads »), Live (briques : halo qui respirait, reflet glissant, barre, bord clignotant retirés), pads live (carte et plein écran : lueur qui respirait, flash, rebond retirés), Pad Live.

## Règle « partout » (où le défaut pouvait se reproduire → test)
- Pads ou cases qui bougent à l'arrêt : `LiveScreen.LivePadCell`, `LivePadsFullScreen.StagePad`, `LivePadsCard.LivePad`, `RhythmScreen.DrumPad`, `StepGrid`, `AllPadsGrid`, `PadLiveScreen.PadLivePad` → `T/ui/StillPadsEverywhereTest` (chaque composant appelle `padLight(`, aucun motif d'animation propre ; images identiques au pixel près à 3 s d'écart pour des pads inactifs, l'onde disparaît après 200 ms, un pad qui joue change ; Pad Live sans rien qui joue immobile 4 s). L'ancien `StillEmptyCellsTest` reste.
- Tête de lecture sur une case vide (pas à pas, « Tous les pads ») : retirée, seule une case avec un coup s'allume (même test).

## Tests et résultats
- Moteur : `padlive_page_d_plays_with_page_a`, `padlive_64_pads_without_allocating` verts ; suite complète verte sauf les deux bancs CPU sensibles à la charge (`master_cpu_budget` vert seul ; `m17_worst_case_live_cpu_bench` rouge aussi sans le changement, voir plus haut).
- `MT/PadLiveTest` (pages, modes, groupes, packs, sauvegarde du modèle), `T/padlive/PadLiveSaveUndoTest` (pack sur la page B = 1 annulation, mode et groupe, refaire, sauvegarde → nouveau ViewModel, catalogue), `T/padlive/PadLiveTouchCountTest` (premier son : 1 toucher ; premier rythme : 2 ; toutes les fonctions ≤ 3), `T/ui/StillPadsEverywhereTest`, i18n (14 langues, `I/padlive.json`). Lot ciblé : 497 tests app (live, ui, onboarding, rhythm, remix routeur, textes…) verts.
- Captures (regardées) : `padlive_home`, `padlive`, `padlive_more`, `padlive_settings` ; réenregistrées car touchées : `rhythm`, `rhythm_landscape`, `all_pads`, `live_*` (grille, effets, prise…), `live_pads*`.

## Décisions à recopier dans DECISIONS
- 2026-11-03 · Premier son en 1 toucher : la carte « Pad Live » ouvre Pad Live et joue une fois le premier « coup » de la page affichée (le pad s'allume). Raison : sortie de B1 et « Ça sonne tout de suite » de la maquette. Retour : retirer `padLiveWelcome` dans `AppRoot`.
- 2026-11-03 · Pages B à D dans `LivePads.more`, la page A reste `clips` (16) : les anciens écrans et le Remix (« vers un pad ») ne voient que la page A. Raison : aucun code de C ni des autres sections à toucher. Retour : fusionner les listes plus tard (D).
- 2026-11-03 · Moteur : 64 emplacements de pads live sans nouvelle commande. Raison : les boucles d'une page continuent quand on en affiche une autre. Retour : `kSlots = 16`.
- 2026-11-03 · Réglages d'un pad par son coin (« boucle », « coup », « tenir ») plutôt que l'appui long : un pad « tenir » se tient longtemps. Retour : appui long seul.
- 2026-11-03 · Tête de lecture du pas à pas : seule une case avec un coup s'allume, plus rien sur les cases vides (règle « rien ne bouge »). Retour : bordure blanche dans `StepGrid`.
- 2026-11-03 · Effets du niveau simple : Filtre, Écho (sortie en écho), Roulement 1/16 ; « Plus » : Montée, Chute (freinage), Bégaie (hachoir). Les effets à maintenir complets (pad XY, divisions) : B2.

## Fichiers touchés hors liste stricte (à savoir au rangement)
- `RhythmViewModel.kt` : `fitWants` (section Step 4e) lit `live.all` au lieu de `live.clips` (une ligne : les boucles des pages B à D suivent le tempo) ; `padsSoundingNow` passé `internal`.
- `AppRoot.kt` : `LiveStateHost` passé `internal` ; branchement de la bibliothèque pour les pads 17 à 64.
- `engine/host-tests/engine_tests.cpp` : une ligne `#include` + une ligne de liste ; nouveau `padlive_tests.inc`.

## Fait : B2, la scène (2026-11-03, branche fx-b)
- **Effets à maintenir** (`padlive/HeldFx.kt`, `PadLiveScreen.kt`) : Filtre (doigt sur le bouton : grave ↔ aigu comme en B1 ; glissé vers le haut dans le panneau « FILTRE DJ · TANT QUE TU APPUIES » de la maquette : coupure en travers, résonance en hauteur, « Passe-haut 1,8 kHz · résonance 40 % »), Écho (sortie en écho), Roulement (glissé dans son panneau : 1/4, 1/8, 1/8 T, 1/16, 1/16 T, 1/32 ; la dernière reste pour le prochain appui), Montée (sur la longueur du projet, « 4 mesures »), Chute (freinage), Bégaie (division du projet). Lâcher : tout revient (filtre et résonance au repos). Le panneau se pose sur les dernières rangées de pads : rien ne bouge sous le doigt.
- **Reste de B1 corrigé** : « souffle qui monte » coupé → « %d mesures » ; et la règle partout a trouvé « grave ↔ aigu » coupé lui aussi (360 dp) : titres et sous-titres des 6 boutons rétrécissent (jusqu'à 9 sp) avant d'être coupés.
- **Réglages par pad** (`PadModes.kt`) : Hauteur (−12 à +12 demi-tons, la longueur change avec), À l'envers, Attaque (0 à 1 s), Fin (0 à 2 s). Moteur : `SetClipParam` 3 à 6 (aucune nouvelle commande), lecture interpolée, attaque à chaque passage, fin quand on arrête le pad. Modèle : `LiveClip.pitch/reverse/attack/release` en fin, absents du fichier au repos. Une prise du Live qui garde les pads les rend pareil (`padlive/PadSound.kt`, sauf la Fin, voir restes).
- **Lancer une ligne** : un ▶ au bout de chaque rangée de 4 pads ; tous partent ensemble sur la prochaine barre de mesure (un pad qui attendait y est déplacé, une boucle qui joue continue) ; si toutes ses boucles jouent, ▶ devient ■ et les arrête. Commande 150 `LaunchClipRow`. Groupes « un seul à la fois » respectés. Une prise du Live l'écrit (`LivePadTake.Tap.bar`).
- **Enregistrer ce que je joue** (`padlive/PlayedTake.kt`, `PlayedTakeSheet.kt`) : le gros bouton rouge de la maquette. Armé, il part sur la prochaine barre où la musique joue (dans le silence, il attend le premier pad : l'horloge repart sur une barre avec lui) ; retouché, il finit sur la barre suivante : mesures entières, sortie complète (effets compris). Puis « Sur le pad vide A13 » (boucle au tempo, qui le suit ensuite) / « Dans une brique du Live » / « Jeter la prise ». 16 mesures ou 60 s au plus. Commandes 151 `ResampleArm`, 152 `ResampleStop`.
- **Tap tempo** : « 124 » › « Taper » (la feuille de tempo existante), une étape d'annulation, les boucles suivent.
- **Horloge MIDI** : en sortie, le moteur la compte sur le fil audio, sur les temps du transport (jamais swinguée), Start au premier pas, 24 tops par temps, Stop ; chaque message a le moment où son échantillon s'entend, envoyé avec l'horodatage Android + la latence de sortie (`midi/MidiClockOut.kt`, commande 153 `SetMidiClockOut`). Remplace l'ancienne horloge Kotlin qui dérivait. En entrée : déjà là (Start, Continue, tempo, Stop), testée depuis Pad Live.
- **MiniLab** : ses 16 pads jouent la page affichée de Pad Live (mode, groupe, force ; « tenir » s'arrête au relâché : `MidiCommand.PadUp`) au lieu de la page A sans mode.
- **Moteur, au passage** : les boucles des pads live partaient 1 échantillon en retard à chaque temps (horloge additionnée) : même tolérance que les barres, à l'échantillon près (aucun autre banc n'a bougé).

## Règle « partout » (B2)
- Mots coupés sur un téléphone : les 6 boutons d'effets (titre et sous-titre), 14 langues, 360 dp → `T/padlive/PadLiveSceneTest.noHeldEffectsWordsAreCutOnAPhoneInAnyLanguage` (mesure réelle des polices ; il a trouvé « grave ↔ aigu » en plus de la Montée). Même composant `FitText` pour les 12 textes.
- Pads de la manette qui ne suivaient pas la page ni le mode : seul Pad Live a des pages et des modes ; `PadLiveController.midi` est le seul chemin (testé), les autres écrans gardent le leur.
- Retard d'un échantillon des pads live : un seul endroit (`ClipLauncher::render`), couvert par `padlive_row_starts_on_the_bar` et `padlive_resample_from_bar_to_bar` (clics à 24000 pile).

## Tests et résultats (B2)
- Moteur (`engine/host-tests/padlive_tests.inc`) : `padlive_row_starts_on_the_bar`, `padlive_pad_pitch_reverse_attack_release`, `padlive_resample_from_bar_to_bar` (prise = 96000 échantillons pile, clic au même décalage qu'à la sortie), `padlive_midi_clock_out_on_the_beat` (tops tous les 960 échantillons pile à 125 BPM avec swing, aucun perdu quand le tempo change, Stop, Continue), `padlive_scene_without_allocating` (64 pads hauteur/envers/attaque/fin, lignes, prises, horloge, changement de tempo : 0 allocation). Suite complète : verte sauf les deux bancs CPU sensibles à la charge (`s4_no_alloc_and_cpu`, `m17_worst_case_live_cpu_bench`), mesurés en alternance avant/après pendant la grande vérification de fx-int : mêmes chiffres (14,2 à 15,8 % des deux côtés), donc du bruit.
- App : `T/padlive/PadLiveTakeTest` (prise → pad : une annulation, refaire, sauvegarde → nouveau ViewModel ; prise → brique : dans l'export du morceau ; trop courte / lâchée / jetée : rien ; tap tempo ; son du pad cuit comme le moteur), `T/padlive/PadLiveMidiTest` (MiniLab : page affichée, modes, force ; ligne = une commande ; horloge en sortie et en entrée), `T/padlive/PadLiveSceneTest` (Filtre glissé dans son panneau, Roulement et ses divisions, ▶ de ligne, prise, mots qui tiennent), `T/padlive/PadLiveTouchCountTest` (nouvelles fonctions ≤ 3 touchers), `MT/PadLiveTest`, `MT/LivePadTakeTest`, `T/CommandIdsTest`, i18n (14 langues).

## Décisions à recopier dans DECISIONS (B2)
- 2026-11-03 · Effets maintenus : le panneau (XY du Filtre, divisions du Roulement) s'ouvre sous le doigt qui tient le bouton et se pilote en glissant vers le haut, sans lever le doigt. Raison : la maquette (3e écran) et « appuie et maintiens ». Retour : `HeldFxPanel` retiré, B1 inchangé dessous.
- 2026-11-03 · « Enregistrer ce que je joue » = la sortie complète (effets compris), d'une barre à l'autre, et dans le silence elle attend le premier son. Raison : ce qu'on entend est ce qu'on garde ; pas de silence au début. Retour : `ResampleArm` a = 0.
- 2026-11-03 · Une prise posée sur un pad est une boucle « qui suit le tempo » (bpm de la prise). Retour : `bpm = 0` dans `PlayedTake.placeOnPad`.
- 2026-11-03 · Horloge MIDI en sortie comptée par le moteur (à l'échantillon) au lieu d'une minuterie Kotlin. Raison : la règle « jamais de minuteries Kotlin » et la dérive de l'ancienne. Retour : rétablir `startClock` d'avant (git).
- 2026-11-03 · Les pads de la manette jouent la page affichée de Pad Live avec ses modes. Retour : `PadLiveController.midi` → false.
- 2026-11-03 · Horloge des pads live : même tolérance (1e-9 temps) pour les départs que pour les barres : plus d'échantillon de retard. Retour : retirer `+ 1e-9` dans `ClipLauncher::render`.

## Fichiers touchés hors liste stricte (B2, à savoir au rangement)
- `A/audio/` : aucun ; `A/ui/AppRoot.kt` : une ligne (les commandes MIDI passent d'abord par Pad Live) + la branche `PadUp`.
- `RhythmViewModel.kt` : section Live pads (`launchLiveRow`, `pushLive` envoie le son du pad), section Lot 4b (`livePadTapped` prend `bar`), section Enregistrer la performance (le son du pad cuit avant `PadBounce`).

## Restes de B2
- La « Fin » d'un pad n'est pas rendue dans la prise du Live gardée sur piste audio (`PadBounce` coupe en 5 ms) : à faire avec E (export) ou D.
- Manette : ni ligne ni « Enregistrer ce que je joue » au MiniLab (ses 16 pads sont les 16 pads de la page) ; à voir avec ses lumières (D).
- Horloge MIDI en sortie : à vérifier sur un vrai appareil MIDI (liste de Benjamin) ; l'horodatage suppose la latence annoncée par le téléphone.
- Les secondes de B1 (premier son, premier rythme) restent à mesurer sur le téléphone.

## Restes de B1
- Réglages par pad « hauteur », « à l'envers », attaque/fin : demandent le moteur (B2, commandes 150+).
- Les secondes (premier son < 5 s, rythme < 30 s) : à mesurer sur le téléphone de Benjamin (moment 1). Le jeu « Fête » se rend en quelques secondes à la création du projet.
- Packs : boucles rendues avec les sons intégrés du style (sans les packs téléchargés).

## Fait : B3, le jeu et le partage (2026-11-03, branche fx-b)
- **Défi « refais ce rythme »** (`M/PadChallenge.kt`, `A/padlive/Challenge.kt`) : bouton « 🎯 Défi » de la maquette (à droite des pages ; sa cible seule sous 350 dp). 6 défis d'une mesure (Boum boum, Ping-pong, Le galop, Le contretemps, Le roulement, Le boss) sur les pads « coup » de la page, au tempo du projet borné à 80–120. « Écouter » : la musique s'arrête, l'exemple (rendu d'avance depuis les vrais sons des pads, joué par l'aperçu du moteur) allume ses pads l'un après l'autre ; « À toi » : les touchers sur les pads (écran ou manette) sont chronométrés, notés à la fin du nombre de coups ou à « J'ai fini » : 0 à 3 étoiles, « N sur M », « un peu trop vite / lent » (dit par TalkBack). Meilleures étoiles gardées dans le projet (`PadLiveSet.stars`, hors annulation) ; le défi suivant = le premier sans 2 étoiles. Le panneau remplace les effets tant qu'il est ouvert.
- **Lumières qui suivent le son** : le moteur publie le niveau de chaque pad (crête qui retombe en 120 ms, un atomique par pad écrit une fois par bloc, sans commande : `ClipLauncher::level`, JNI `clipLevels`) ; `PadLight(level = …)` : la lumière intérieure d'un pad qui joue va d'une lueur (30 %) au plein ; un pad inactif ne dessine toujours rien, et rien n'est lu quand rien ne joue.
- **Mode « Fête »** (`A/padlive/PartyMode.kt`) : « Plus » › « Fête » : les 16 pads de la page en plein écran (gros pads, noms en grand), ✕, pages A–D, ▶/■ ; la lumière de la pièce suit le pad le plus fort ; Retour ou ✕ revient à Pad Live.
- **Export de la perf** (`A/padlive/PerfShare.kt`) : après ● REC → Fin, une ligne « Perf gardée · 0:42 · Écouter · Partager · ✕ » ; « Plus » › « Partager » aussi. « Partager ma perf » : « Le son » (M4A/WAV, l'export du morceau écrit par la prise, effets compris, avec sa fenêtre de progression) et « Pistes séparées » : un WAV par pad joué, rendu de nouveau depuis la prise (`PadTakeRecord` gardé avec le projet dans `PadLiveSet.take`, annulé avec la prise), copié dans Musique › BenStudio puis menu de partage. Les fichiers additionnés redonnent la piste « Pads live » à l'échantillon près (test).
- **Reste de B2 fait** : la « Fin » d'un pad est rendue dans la prise gardée (`PadBounce` : la dernière passe continue après l'arrêt et s'éteint sur la durée de la Fin, comme `ClipLauncher::stopSlot`).
- **Accessibilité** : chaque pad dit « son, mode, groupe, état, pad A9 » (même texte sur Pad Live et en Fête, `PadLiveA11y.kt`) ; double toucher TalkBack = un doigt ; résultats du défi et des pistes en zone annoncée ; vibration `Feel.Hit` à chaque pad (réglage Vibrations) ; gros pads = « Fête ».
- **Sur le chemin (personas)** : Pad Live ajouté au premier lancement (`Welcome.kt`) ; ← de Pad Live ramène à « Que veux-tu faire ? » ; la bulle du « Mode retours » ne se pose plus sur la rangée des pages (Défi, Pack) ; « Plus » sur deux rangées de trois (« Briques » coupé en deux à 360 dp) ; noms des pads qui rétrécissent sur un pad court.

## Personas (B3, banc « testeur humain », `T/human/HumanPadLivePersonasTest.kt`, touchers sur les pixels de l'écran)
Captures regardées (dossier `-Ppersona.dir`). Le banc n'a pas de moteur : le jeu prêt à jouer est remplacé par un son court par pad (comme le banc le fait pour le kit), la prise ● REC n'y avance pas (horloge du moteur) : elle est testée par `T/padlive/PadLiveExportTest` à travers le ViewModel.
- **Léo, 8 ans** (installe l'appli, veut taper sur des pads) : Grave trouvé et corrigé : Pad Live absent du premier lancement (seulement sur « Que veux-tu faire ? ») ; Grave trouvé et corrigé : la bulle « REC · Noter » couvrait « Défi », son toucher ne l'ouvrait pas. Ensuite : un pad, Défi, Écouter, ses pads allumés, à lui, résultat avec étoiles, Fête, sortie : OK.
- **Karim, DJ** : ▶ d'une ligne, tempo +, Filtre glissé, page B puis A, « Partager » sans perf (« Pas encore de perf gardée : touche ● REC… »), défi direct « À moi ! » puis « J'ai fini » : rien de Bloquant ni de Grave.
- **Claire, débutante** : pad, Plus, Fête, page B en Fête, Retour (quitte la Fête d'abord), ← : Moyen trouvé et corrigé : ← la laissait dans la grille de batterie du projet « Live » ; maintenant l'accueil.
- Reste : aucun Bloquant ni Grave.

## Règle « partout » (B3)
- Pads qui bougent à l'arrêt : le nouveau `PartyMode.PartyPad` ajouté à `T/ui/StillPadsEverywhereTest` (lumière partagée, aucune animation propre) + « Fête » immobile 4 s sans rien qui joue, même avec des niveaux lus.
- Fonctions à plus de 3 touchers : Défi (2), Partager ma perf (3), Fête (3) ajoutés à `PadLiveTouchCountTest`.
- Mots coupés à 360 dp : capture `padlive_small` regardée (« Plus » à deux rangées, page/pack/défi sur une ligne).
- La bulle « Mode retours » sur des boutons : seul Pad Live est en plein écran sans barre du haut ; la rangée des pages est déclarée à éviter (`Retours.keepClear`), comme « Boucler » l'était.

## Tests et résultats (B3)
- Moteur : `padlive_lights_follow_the_sound` (niveau fort ≈ 0,5, faible ≈ 0,05, jamais joué 0, retombe sous 0,01 en ½ s, lecture sans allocation) ; padlive_* verts.
- `MT/PadChallengeTest` (niveaux, score, premier toucher en retard pardonné, faux pad, trop vite/lent, étoiles gardées, enregistrement JSON par défaut absent), `T/padlive/PadLiveChallengeTest` (exemple entendu pads allumés, rejoué 3 étoiles, gardé hors annulation, défi suivant, raté, page sans « coup »), `T/padlive/PadLiveExportTest` (Fin rendue ; prise → un WAV par pad dont la somme = la piste à 1e-5 près, sauvegarde → nouveau ViewModel, annuler ; « Le son » = l'export du morceau avec les pads ; sans prise : message), `PadLiveTouchCountTest`, `StillPadsEverywhereTest`, onboarding, i18n (14 langues).
- Captures (regardées) : `padlive_challenge`, `padlive_challenge_done`, `padlive_perf`, `padlive_party`, `padlive_levels`, `padlive_share`, `padlive_small` ; réenregistrées car touchées : `padlive`, `padlive_more`, `padlive_filter`, `padlive_roll`, `padlive_take`, `padlive_recording`.

## Décisions à recopier dans DECISIONS (B3)
- 2026-11-03 · Défi : l'exemple est un son rendu d'avance depuis les sons des pads et joué par l'aperçu du moteur ; les touchers ne sont que chronométrés (écarts), aucune minuterie ne tient le temps de la musique. Retour : retirer le bouton Défi.
- 2026-11-03 · Défi au tempo du projet borné 80–120, noté dès le dernier coup (ou « J'ai fini »), premier toucher pris comme départ ; 2 étoiles ouvrent le suivant. Retour : `PadChallenge.MIN_BPM/MAX_BPM`, `score`.
- 2026-11-03 · Lumières qui suivent le son : niveau par pad publié par le moteur (atomique, une fois par bloc), sans commande. Retour : `level = null` dans Pad Live.
- 2026-11-03 · « Gros pads » = le mode Fête (les 16 pads en plein écran) plutôt qu'un réglage de plus. Retour : un réglage.
- 2026-11-03 · Pistes séparées d'une perf = un WAV 32 bits flottant par pad, rendus depuis la prise gardée (`PadLiveSet.take`), au gain de la piste « Pads live » ; pas d'effets de scène dedans (ils sont dans « Le son »). Raison : 8 pistes audio au plus, 64 pads. Retour : retirer « Pistes séparées ».
- 2026-11-03 · Pad Live sur le premier lancement aussi (sans « Simple ou Pro ? »), et ← de Pad Live mène à l'accueil. Raison : personas Léo et Claire. Retour : `WelcomeScreen` / `goHome()` dans `close`.

## Idées (B3, à recopier dans REVIEW et DECISIONS)
- **Vidéo de l'écran** : pas tentée au-delà de l'étude : il faut MediaProjection (fenêtre d'accord système) + encodeur vidéo + son du moteur synchronisé ; rien de cela ne se teste ici (pas d'émulateur, Robolectric sans codec vidéo). La « Vidéo pour les réseaux » existante (forme d'onde, 30 s) reste le partage vidéo. Idée : réutiliser le chemin d'enregistrement d'écran du « Mode retours » (`RetoursService`) pour une perf.
- **Ableton Link** : SDK C++ (GPL-2.0, compatible usage perso) mais il faut multidiffusion UDP sur le Wi-Fi, verrou multicast Android et un second appareil Link pour vérifier : non testable ici, demi-journée non engagée. Idée pour D/E avec un appareil de Benjamin.

## Restes de B3
- Pistes séparées : seulement les pads ; les briques jouées pendant la prise restent dans « Piste par piste » de Mix. Le volume de la tranche « Pads live » du mixeur n'est pas appliqué aux fichiers (le gain du clip, si).
- Petit téléphone (360 dp) avec « Plus » ouvert : les pads deviennent courts, les noms longs (« Boucle de kick ») sont abrégés ; à revoir avec la tablette (D).
- Le banc ne fait pas avancer une prise ● REC (pas d'horloge moteur) : un siège de banc avec le moteur hôte serait utile (D).
- Résultat du défi sur le téléphone : à essayer par Benjamin (liste du moment 1 : un enfant, un défi).

## Fichiers touchés hors liste stricte (B3, à savoir au rangement)
- `A/retours/` : aucun (seulement `Retours.keepClear` appelé depuis `PadLivePages.kt`).
- `A/onboarding/Welcome.kt` (carte Pad Live) et `LaunchChoice.kt` (`PadLiveCard` passé `internal`) ; `A/ui/AppRoot.kt` (premier lancement → Pad Live ; ← → accueil).
- `RhythmViewModel.kt` : section Enregistrer la performance seulement (la prise garde `PadLiveSet.take`).
- `app/build.gradle.kts` : propriété `persona.dir` passée aux tests.
