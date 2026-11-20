# Tour 9 : fiche du lot tuto « validation » (D03 D07 D09 D15 D18 S67 D05 D16)

Rédigée par le lecteur-trieur, 2026-11-20. Gabarit : `GABARIT-FICHES-V3.md`, fiche 2 « correcteur » (points 4, 4 bis, 5, 9 ; « Demander au chef », « Travailler en silence »). Voisine : `tour-9-fiche-tuto.md` (e4ba4c1a5).

Têtes lues :
- intégration `ccr-80af1a1b-mn0wec` = **c87c8f58** (notée « C ») ;
- `eq/tour-9-L13` = **6620654d** (notée « L13 »). L13 ne change, côté appli, que `TutoController.kt` (+19) et `TutoPanel.kt` (+312/-103). Toutes les autres lignes citées sont identiques sur les deux têtes.

Préfixes : `app:` = `app/src/main/java/com/pocketstudio/app/`, `model:` = `project-model/src/main/kotlin/com/pocketstudio/model/`, `res:` = `project-model/src/main/resources/tutos/`.

## 0. Dépendances d'entrée

- Comme la fiche tuto : le premier lot part **après** l'entrée de L13, L15 et base-rouges. Il se base sur l'intégration qui les contient, et les lignes à suivre sont celles de la colonne L13.
- Le tuto est **une seule zone** : un lot à la fois. Les lots V1 et V2 ci-dessous et le lot de `tour-9-fiche-tuto.md` passent donc **l'un après l'autre**, jamais en même temps.
- Chaque lot finit par « prêt pour contrôle » : un débutant neuf rejoue les chapitres touchés sur la branche du lot (0 chapitre bloqué, 0 grave neuf) avant l'entrée. En mode boost, ce contrôle est fait dans la grande vérification de fin ; il reste « tenté » jusque-là.
- Ordre de travail pour chaque défaut : rejeu d'abord pour tout bloquant ou grave (`scripts/bench/rejeu-nouveau.sh`, puis `scripts/bench/rejeu-defauts.sh`), puis un commit « rouge: sujet » avec son test ciblé, puis « vert: même sujet ».

## 1. Points tranchés

- **D16 : tranché, on corrige les textes.** Les cours ne promettent plus de « pistes séparées » (stems), puisque l'export ne les propose que pour un remix. On ne touche pas à `ShareSheet.kt`.
- **F041 : abandonné.** Rien à faire, et aucun test. Noté ici pour qu'aucun lot ne le reprenne.

## 2. Les défauts

### D03 (bloquant) : la ligne acid ne se valide pas, et « Montre-moi » ne l'écrit pas

- **Acceptation** : dans un cours acid (acid_house Facile, acidcore Facile, acid_techno Pro), à l'étape « À toi » de la 303, « Montre-moi » écrit les notes de la ligne dans l'écran Acid 303, devant l'utilisateur. Une ligne écrite à la main, ou faite avec « Générer », qui suit la consigne valide l'étape. « Je le fais » ouvre l'écran des pas, pas la fiche du son.
- **Symptômes** (clusters-complet.md) :
  - l.12, t9-01 : « Montre-moi » ouvre la fiche son « Acid 303 », aucune note écrite ;
  - l.13, t9-05 : la démo montre la grille de batterie ;
  - l.67, t9-01 #44 : une ligne faite avec « Générer » ne valide pas.
- **Cause probable** (rejeu d'abord) :
  - `AcidNotes.done` exige la ligne exacte (`acidMatches`) sur toutes les scènes ;
  - pour `notes:acid`, `goFor`/`showMe` mènent à la fiche du son au lieu de l'écran des pas.
- **Fichiers** (C / L13) :

  | Fichier | Ancre | C | L13 |
  |---|---|---|---|
  | `app:tuto/TutoController.kt` | `goFor` | 392 | 403 |
  | `app:tuto/TutoController.kt` | briques acid | 591 | 595 |
  | `app:tuto/TutoController.kt` | `doIt` | 623 | 627 |
  | `app:tuto/TutoController.kt` | `showMe` | 673 | 677 |
  | `app:tuto/TutoController.kt` | `acidHints` | 782 | 786 |
  | `model:TutoKind.kt` | parse `notes:acid` → `AcidNotes` | 240 | = |
  | `model:TutoKind.kt` | `AcidNotes` | 414-430 | = |
  | `model:TutoChecks.kt` | `acidMatches` | 1013 | = |
  | `model:TutoPlay.kt` | `showMe` / `canShow` / `acid` | 92 / 95 / 176 | = |
  | `app:acid/AcidScreen.kt` | en lecture seulement, à toucher si la démo doit s'y écrire | | |
  | `res:acid_house.json` | `acid_do` | 57 | = |
  | `res:acid_techno.json` | `acid_show` / `acid_do` | 126 / 135 | = |
  | `res:acidcore.json` | `acid_show` / `acid_do` | 116 / 125 | = |

- **Rejeu** : sièges t9-01 (acid_house Facile, ar), t9-05 (acidcore Facile, ko), t9-03 et t9-04 (acid_techno Pro, ja).
- **Test rouge attendu** :
  - test unitaire modèle : une ligne « Générer » conforme et une ligne écrite à la main valident `acid_do` ;
  - test de démo : après `showMe` sur `acid_show`/`acid_do`, l'écran courant est Acid 303 et les 13 notes sont posées.

### D07 (bloquant/grave) : une étape de réglage ne se valide pas si la valeur est déjà en place

- **Acceptation** : quand la consigne demande un réglage (detune, glide, son de basse, fin du ch. 7), l'étape se valide dès que le réglage demandé est en place. Elle se valide aussi si l'utilisateur l'avait réglé juste avant, ou après un « Montre-moi » suivi d'un retour en arrière.
- **Cause probable** (rejeu d'abord) : `SynthKnobs` compte les boutons « tournés depuis le début de l'étape ». Une valeur déjà en place, ou remise par la restauration de « Montre-moi », ne compte jamais.
- **Fichiers** :

  | Fichier | Ancre | C | L13 |
  |---|---|---|---|
  | `model:TutoKind.kt` | parse `changed:synth` → `SynthKnobs` | 250 | = |
  | `model:TutoKind.kt` | `SynthKnobs` | 596-600 | = |
  | `model:TutoChecks.kt` | `knobsShort` / `knobsOf` | 653 / 669 | = |
  | `app:tuto/TutoController.kt` | `showMe` (restauration) | 673 | 677 |
  | `app:tuto/TutoController.kt` | `doIt` | 623 | 627 |

- **Rejeu** : t9-58 (retro_techno Pro, id), t9-43 (liquid_dnb Pro, nl), t9-54 (pop Pro, pl), t9-23 (drill Pro, pl).
- **Test rouge attendu** : un test unitaire par étape citée (detune/glide, `bass_do`, fin du ch. 7), dans deux cas : valeur déjà en place au début de l'étape, et valeur posée après la restauration de « Montre-moi ».

### D09 (grave, ~21 sièges) : « Copier sur les mesures suivantes » ne fait parfois rien

- **Acceptation** : un seul appui sur « Copier sur les mesures suivantes » (bandeau du tuto, replié ou non, ou bouton du piano roll) copie le motif, exactement comme le « Copier » de l'éditeur, et l'étape avance.
- **Symptômes** : `tuto_roll_copy` et `tuto_need_folded` ne font rien, alors que le Copier de l'éditeur marche (clusters-complet.md l.35, 52, 63, 72).
- **Cause probable** (rejeu d'abord) : `fulfilNeed` sort en silence quand `TutoPlay.button(...)` renvoie null, parce que le regard (`look`) est périmé.
- **Fichiers** :

  | Fichier | Ancre | C | L13 |
  |---|---|---|---|
  | `app:tuto/TutoController.kt` | `fulfilNeed` | 633 | 637 |
  | `app:tuto/TutoController.kt` | ligne Copy focus | 659 | 663 |
  | `app:tuto/TutoController.kt` | `look()` | 959 | 963 |
  | `app:tuto/TutoPanel.kt` | « Copier sur les mesures suivantes » | 219, 551, 1158 | 218, 498, 1149 |
  | `app:tuto/TutoPanel.kt` | `tuto_grid_copy` | 326 | 309 |
  | `app:tuto/TutoPanel.kt` | `tutoMotifDone` | 379 | 326 |
  | `app:tuto/TutoPanel.kt` | `tuto_need_folded` | 1162 | 1173 |
  | `app:AppRoot.kt` | `TutoHints.copy = tuto::fulfilNeed` | 2081-2084 | = |
  | `app:AppRoot.kt` | `need = { tuto.fulfilNeed() … }` | 2632 | = |
  | `app:AppRoot.kt` | `copyMotif` | 3245 | = |
  | `app:rhythm/RhythmScreen.kt` | copie de grille | 728 | = |
  | `app:roll/PianoRoll.kt` | `tutoMotifDone` / `tuto_roll_copy` | 1620 / 1631 | = |
  | `model:TutoPlay.kt` | `button` / `press` | 52 / 55 | = |

- **Rejeu** : `rejeu-defauts.sh` sur trois des sièges cités (l.35, 52, 63) : un au roll, un au bandeau replié, un à la grille.
- **Test rouge attendu** :
  - test unitaire `TutoController` : `fulfilNeed()` avec un regard périmé copie quand même (il recalcule le regard ou appelle la copie directement) ;
  - test Compose « un seul appui » sur `tuto_roll_copy` et sur `tuto_need_folded`.

### D15 (moyen) : « Exemple passé sans l'écouter » alors qu'il a été écouté

- **Acceptation** : si l'utilisateur a écouté l'exemple, passer à l'étape suivante ne lui dit jamais « Exemple passé sans l'écouter ». S'il manque seulement l'écoute de son propre essai, le message le dit, ou rien n'est dit.
- **Cause probable** : `move()` affiche le message dès que l'étape n'est pas « entendue ». Or, avec `hearMine`, `heardPart` n'est vrai que si l'exemple **et** l'essai ont été écoutés.
- **Fichiers** :

  | Fichier | Ancre | C | L13 |
  |---|---|---|---|
  | `app:tuto/TutoController.kt` | `move()` | 343 | 354 |
  | `app:tuto/TutoController.kt` | « Exemple passé sans l'écouter » | 352 | 363 |
  | `app:tuto/TutoController.kt` | `listen()` | 797 | 801 |
  | `app:tuto/TutoController.kt` | `heardPart(step, EXAMPLE)` | 828 | 832 |
  | `app:tuto/TutoController.kt` | `heardMine` | 857 | 861 |
  | `app:tuto/TutoController.kt` | `heardPart` | 867 | 871 |

- **Rejeu** : non obligatoire (moyen) ; sièges t9-73 (trap, en), t9-12 (amapiano, en), t9-27 (gabber, id) pour le constat.
- **Test rouge attendu** : test unitaire `TutoController` : exemple écouté, essai non écouté, `move()` vers l'étape suivante → aucun message « sans l'écouter ».

### D18 : le remplissage (« fill ») du ch. 12 de reggaeton est refusé

- **Acceptation** : au ch. 12 « Les transitions » de reggaeton, trois cases de timbales à la suite, juste avant le dernier temps de la dernière mesure, valident l'étape « fill ». Les étapes « gap » et « crash » qui suivent restent atteignables.
- **Cause probable** (rejeu d'abord) : la fenêtre `fillRole`/`fillEnd` ne correspond pas au texte (rôle du pad des timbales ou position « juste avant le dernier temps »).
- **Fichiers** :

  | Fichier | Ancre | C | L13 |
  |---|---|---|---|
  | `model:TutoKind.kt` | parse `fill` → `DrumFill` | 257 | = |
  | `model:TutoKind.kt` | parse `gap` → `DropGap` | 260 | = |
  | `model:TutoKind.kt` | `DrumFill` / `DropGap` | 738 / 807 | = |
  | `model:TutoChecks.kt` | `fillRole` / `fillEnd` | 983-997 | = |
  | `model:TutoChecks.kt` | gap / fill | 1629-1648 | = |
  | `res:reggaeton.json` | ch. 12, à partir de 431 ; étape `fill` | ~435-438 | = |

- **Rejeu** : t9-57 (reggaeton, tr).
- **Test rouge attendu** : test unitaire modèle : le geste décrit par le texte de reggaeton ch. 12 (3 cases de timbales juste avant le dernier temps) valide `fill`. Le même test boucle sur tous les cours qui ont un `check: fill`.

### S67 : Drop B de tech_house, le charley ouvert entre les kicks est refusé

- **Acceptation** : dans tech_house Facile, à l'étape « Drop B, ajoute le charley ouvert entre les kicks », des charleys ouverts posés entre les kicks du Drop B valident l'étape.
- **Cause probable** (rejeu d'abord) : le contrôle de piste `open_hat` regarde la mauvaise partie (Drop A au lieu de Drop B), ou ses cases attendues.
- **Fichiers** :

  | Fichier | Ancre | C | L13 |
  |---|---|---|---|
  | `res:tech_house.json` | étape Drop B, cible `open_hat` | 441-444 | = |
  | `model:TutoChecks.kt` | `laneMatches` (avec les rolls) | 803-815, 843 | = |
  | `model:TutoKind.kt` | parse `Lane` / classe `Lane` | 236 / 312 | = |

- **Rejeu** : t9-67 (tech_house Facile, ko).
- **Test rouge attendu** : test unitaire modèle : les charleys ouverts sur les contretemps du Drop B valident l'étape.

### D05 (grave) : les noms de pads ne correspondent pas aux textes, et le Roll ×4 est refusé

- **Acceptation** :
  - chaque pad nommé dans un cours (« pad des timbales », « Percussion », « rim »…) existe sous ce nom dans le kit que le cours installe ;
  - à une étape de roll, un Roll ×4 posé sur le dernier coup de charley, ou sur les 4 cases du dernier temps, valide l'étape ;
  - « Montre-moi » fait le même geste que celui que le texte décrit.
- **Symptômes** :
  - noms : sièges t9-10, t9-13, t9-54, t9-76, t9-65, t9-07 ;
  - roll : l.15, t9-24 (seule la 16e case vide est acceptée) et l.31, t9-20 (les 4 cases du dernier temps sont refusées, et « Montre-moi » fait un Roll ×4).
- **Fichiers** :

  | Fichier | Ancre | C | L13 |
  |---|---|---|---|
  | `model:StarterWizard.kt` | noms et rôles des pads (`Perc`, `Perc2`…) | 43-47, 63 | = |
  | `model:TutoChecks.kt` | `laneMatches` avec rolls | 803-815 | = |
  | `model:TutoKind.kt` | `Lane` | 312 | = |
  | `res:*.json` | textes qui nomment un pad | | |

- **Rejeu** :
  - noms : t9-10 (afrobeats, fr), t9-13 (boom_bap, nl), t9-54 (pop, pl), t9-76 (uk_garage, nl), t9-65 (synthwave, ja), t9-07 (afro_house, zh) ;
  - roll : t9-20 (disco, de), t9-24 (drill, pl), t9-61 (riddim, hi).
- **Test rouge attendu** :
  - un test de cohérence sur **tous** les cours : chaque nom de pad cité dans un `text`/`textPro` existe dans le kit du cours ;
  - un test unitaire roll : un Roll ×4 sur le dernier coup ou sur les 4 cases du dernier temps valide l'étape.

### D16 (tranché : on corrige les textes) : les cours promettent des « pistes séparées » que l'export n'offre pas

- **Acceptation** : aucun cours ne promet d'export en pistes séparées. Ce que le cours annonce pour l'export correspond à ce que la feuille de partage propose vraiment pour un morceau (pas un remix).
- **Constat** : 24 occurrences, une par cours, dans les `text` (par exemple `acid_techno.json:408`, `acidcore.json:398`, `boom_bap.json:444`). L'export n'offre les stems que pour un remix (`app:export/ShareSheet.kt` 177-240, **à lire seulement**).
- **Fichiers** : `res:*.json` (24 cours) et les clés i18n des mêmes textes (traductions des cours). Rien dans `ShareSheet.kt`.
- **Rejeu** : t9-38 (jersey_club, zh), pour le constat.
- **Test rouge attendu** : un test qui parcourt tous les cours et toutes les langues et échoue si un texte mentionne des pistes séparées ou des stems (la liste des mots interdits est dans le test, par langue).

## 3. Recouvrements avec `tour-9-fiche-tuto.md`

| Ici | Là-bas | Fichier commun | Conséquence |
|---|---|---|---|
| D03, D07, D09, D15 | F034 (`showMe`, défilement song_add), S17 (« Eu faço » qui boucle) | `TutoController.kt` (`showMe` 673/677, `doIt` 623/627) | Un seul lot ouvert sur `TutoController.kt` : les lots ne se chevauchent jamais. S17 est proche de D03/D07, car démo et validation passent par `TutoPlay`. |
| D09 | F101 (le pli du bandeau mange les glissements), D06 (`lowRoom` TutoPanel:565) | `TutoPanel.kt` (`tuto_need_folded`) | Même fichier. Si D09 ne touche que `TutoController`, le recouvrement disparaît. Le lot le dit dans son premier commit. |
| D09 | D06 (`TutoHints.rollInSight`) | `PianoRoll.kt` | Même fichier, pas les mêmes lignes (1620/1631 contre `rollInSight`). |
| D05, S67, D18 | F031 (Facile : coups doux, `TutoChecks.asksSoft` 831, `TutoKind` 315, 11 étapes de piste) | `TutoChecks.kt` (`laneMatches` 803-815 contre 831), `TutoKind.kt` (`Lane` 312 contre 315) | Lignes voisines : le même lot doit les porter, ou ils passent l'un après l'autre. |
| D05, D16 | F031, F032 (`synthwave.json:138-145`, `toms_do` → « Rim ») | `res:*.json`, clés i18n, noms de pads | F032 et D05 parlent tous les deux de noms de pads dans les textes. Un seul lot. |
| D16 | — | `res:*.json` (24 cours) | Touche tous les cours : aucun autre lot sur les JSON pendant ce temps. |

F041 : abandonné, comme dans la fiche tuto (retrait).

## 4. Découpage proposé : deux lots, l'un après l'autre

Les deux lots passent après L13, L15 et base-rouges, et sans lot tuto ouvert en même temps.

**Lot V1 « validation » (6 défauts) : D03, D07, D09, D15, D18, S67**
- Fichiers : `TutoController.kt`, `model:TutoKind.kt`, `model:TutoChecks.kt`, `model:TutoPlay.kt`, `AppRoot.kt` (chemin de copie), `PianoRoll.kt` (1620-1631), `TutoPanel.kt` seulement si D09 l'exige, `AcidScreen.kt` seulement si D03 l'exige, `res:reggaeton.json` et `res:tech_house.json` seulement si le texte est en cause.
- Rejeux d'abord : D03, D07, D09, D18 (bloquants ou graves).
- Contrôle : chapitres acid (3 cours), synthé de retro_techno, liquid_dnb, pop et drill, un chapitre de copie, reggaeton ch. 12, tech_house Drop B.

**Lot V2 « contenu » (2 défauts, jusqu'à 4 avec F031 et F032) : D05, D16 (+ F031, F032 si le chef les y met)**
- Fichiers : `res:*.json` (tous les cours), clés i18n des cours, `model:StarterWizard.kt`, `model:TutoChecks.kt` (`laneMatches`/rolls, `asksSoft`), `model:TutoKind.kt` (`Lane`).
- V2 part après V1, parce qu'ils partagent `TutoChecks.kt` et `TutoKind.kt`.
- F031 et F032 y vont naturellement (mêmes JSON, mêmes noms de pads, `asksSoft` à côté de `laneMatches`). Ils sont alors retirés du lot de `tour-9-fiche-tuto.md`.
- Contrôle : un chapitre de batterie par cours cité (afrobeats, boom_bap, pop, uk_garage, synthwave, afro_house, disco, drill, riddim) et la fin d'un cours pour l'export.

Pour la suite avec la fiche tuto : son lot (F101, D06, F034, S17, et F031/F032 s'ils y restent) passe **avant** V1, ou **entre** V1 et V2, jamais en même temps qu'eux, puisque `TutoController.kt` et `TutoPanel.kt` sont partagés.
