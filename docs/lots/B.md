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

## Prochaine sous-étape : B2 (la scène)
Effets à maintenir complets (Filtre XY de la maquette, Écho, Bégaiement, Montée, Chute, Roulement 1/4 à 1/16 et triolets), lancer une ligne de briques, « Enregistrer ce que je joue », tap tempo, horloge MIDI. Commandes moteur 150 à 169.

## Restes de B1
- Réglages par pad « hauteur », « à l'envers », attaque/fin : demandent le moteur (B2, commandes 150+).
- Les secondes (premier son < 5 s, rythme < 30 s) : à mesurer sur le téléphone de Benjamin (moment 1). Le jeu « Fête » se rend en quelques secondes à la création du projet.
- Packs : boucles rendues avec les sons intégrés du style (sans les packs téléchargés).
