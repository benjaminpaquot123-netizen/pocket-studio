# Plan du tour 8 : finir le tuto, fermer les restes du tour 7, élargir les sièges (BROUILLON)

Écrit le 2026-11-17, avant la fermeture du tour 7. Code de départ : `main` après la PR qui ferme le tour 7 (sha à remplir à la fusion). Lu pour l'écrire : `PLAN-TOUR-7.md`, `EN-COURS.md`, `docs/PROGRESS.md`, les rapports et relectures `tour-7-*.md`, `sieges/tour-7/synthese-tour-7.md`, `lots-correction.md`, `controle/tuto2/verdict.md` et les cinq rapports de contrôle, le lot Fable (`tour-7-fable-repar-rapport.md`, `-relecture.md`, `tour-7-fable-tuto-audit.md` § 3-4).

## En trois lignes
1. Corriger : finir le tuto par sa cause de fond (P8 `TutoKind` chez Fable, puis P6 lisibilité et P7 une consigne par moment chez une équipe Opus), plus le Mixer (D7/U27 et la rotation), le Clavier à 200 % (t4-26) et le banc (glissés inutiles, test capricieux).
2. Contrôler : un siège neuf constate chaque correction « tentée » du tour 7 (Fable et tuto2) et du tour 8, avant le portail pour le tuto.
3. Élargir : la moitié des sièges va là où les tours 6 et 7 ne sont presque pas allés : langues hors fr/ja/ko/de, arabe (droite à gauche), paysage, pliable, grand téléphone, Pro, niveaux confirmé et pro, entrées Remix, Pad Live, Mes projets.

## Avant de commencer (état attendu à la fermeture du tour 7)
- **Fusion accidentelle de tuto2.** Le code de `eq/tour-7-tuto2` (`f1b9fdb1f`) est entré dans l'intégration par la fusion de l'audit Fable (`3d6b1de6c`, branche de l'audit partie de tuto2), alors que son contrôle était « non tenu » (`controle/tuto2/verdict.md` : 2 chapitres bloqués, 4 graves neufs). La règle « tuto2 seulement après son siège de contrôle » n'a donc pas joué. On ne retire rien : `fable-repar` (sur tuto3, intégration `da76359f` fusionnée) répare ces graves. Mais tant qu'un siège neuf ne l'a pas constaté, D2, D3, D4, D8 et les 4 graves du contrôle restent **« tentés », pas « corrigés »**. Une ligne dans `docs/DECISIONS.md` le dit ; le portail du tour 8 vérifie qu'une branche d'audit (doc seule) ne porte aucun commit sous `app/src/main` ou `project-model/src/main` avant de la fusionner.
- Hypothèse : `fable-repar` (P1-P5, flèche de basse, « Réduire ») entre dans le tour 7. Sinon, il passe en tête du tour 8, seul, avant tout lot tuto.
- Captures de référence : une cinquantaine sont périmées dans l'intégration (rapport fable-repar : `projects*`, `remix*`, `take_done*`, `share_sheet*`, `tablet/*`, `scale130/*`, `tour6/shell_*`, `piano_roll*`…). Si la fermeture du tour 7 ne les a pas refaites et regardées, c'est le lot 0 ci-dessous.

## Les buts
1. Tuto : 0 chapitre bloqué et 0 grave neuf sur les quatre profils du contrôle tuto2 (fr House 3-5 et 12, ja Techno 3, ko phone360 130 % Techno 9-10, de tablet8 Pro Techno 8), constatés par des sièges neufs.
2. Tuto : D3 (consigne coupée) et D4 (trop de gestes) fermés par une règle vérifiée par test, plus par des corrections au cas par cas.
3. D7/U27, rotation du Mixer, t4-26 : corrigés et constatés ; le rejeu `t4-26-prise-clavier` repasse « oracle tenu ».
4. Banc : selftest vert 10 fois de suite, `OrdinaryTestsLeaveNothingTest#c` vert 20 fois sous charge, plus de glissés pour rien.
5. Couverture : au moins un siège dans 6 langues jamais jouées au tour 7, sur 4 appareils jamais joués, au moins 4 sièges Pro, au moins un siège confirmé et un pro.

## Les lots

Règles communes (celles du tour 7) : commit « rouge: » (le test seul, qui échoue) puis « vert: » ; les fichiers listés sont les SEULS que le lot touche, une ligne au chef pour tout autre ; captures regardées en Simple et Pro, 100 et 130 % (200 % quand le lot touche une mise en page) ; relecture neuve avant le portail ; CI du lot sur sa branche avant le portail (`AUDIT-PARALLELE.md`). Phrase d'acceptation : « en <langue>, <main>, <appareil> à <police> %, Mode <mode> : depuis <écran>, je <geste> et <ce que je vois> ».

**Départ :** « IMMÉDIAT » = peut partir la minute où le tour 7 est fusionné dans `main`. « APRÈS X » = attend la fusion de X dans l'intégration.

### Lot 0 « captures8 » (si besoin) · IMMÉDIAT, seul, avant les lots qui touchent l'écran
- Portée : refaire et regarder les captures de référence périmées venues de l'intégration (aucun code de l'appli).
- Fichiers : `app/screenshots/**` seulement.
- Acceptation : `./gradlew :app:verifyRoborazziDebug` vert sur la tête ; chaque capture refaite regardée et nommée dans le rapport.
- Parallèle : aucun lot qui change un écran ne fusionne avant lui (sinon on refait deux fois). Les lots du banc peuvent tourner à côté.

### Lot T1 « tutokind » (Fable) · EN COURS chez Fable, continue après la fusion
- Portée : P8 de l'audit Fable (§ 3.3 : une définition par type d'étape, `TutoChecks` en façade), plus M1, M2, M4 de la relecture de fable-repar et ses mineurs (pastille « Touche ici » sans effet, `lit` avant le bornage, `changes` inutilisée, `canShow` qui lit `show` au lieu de `demo`, `Tour7TutoMissingOfferedTest` sans niveau).
- Fichiers :
  - `project-model/src/main/kotlin/com/pocketstudio/model/tuto/TutoKind.kt` (neuf)
  - `project-model/src/main/kotlin/com/pocketstudio/model/tuto/TutoChecks.kt`
  - `project-model/src/main/kotlin/com/pocketstudio/model/tuto/TutoPlay.kt`
  - `app/src/main/java/com/pocketstudio/app/tuto/TutoController.kt`
  - `app/src/main/java/com/pocketstudio/app/tuto/TutoPanel.kt` (M2 seulement : la ligne « Posé : … »)
  - `app/src/main/java/com/pocketstudio/app/synth/PianoRoll.kt` (mineurs)
  - `project-model/src/main/resources/i18n/*/tuto.json` (M2 : une clé, 14 langues)
  - tests sous `project-model/src/test/.../tuto/` et `app/src/test/.../tuto/`
- Acceptation :
  - `TutoShowMeEverywhereTest` vert après chaque type migré ; fichiers `PS_TUTO_FAULTS` vides ; aucun fichier `tutos/*.json` touché.
  - M1 : temps de `TutoChecks.need` mesuré sur le pire chapitre ; soit sous 8 ms sur le fil principal, soit calculé hors du fil principal (comme `check()`).
  - M2 : « en fr, deux mains, phone à 100 %, Mode Simple : depuis le Rythme au chapitre 12 de house ouvert par la liste, je touche « Poser ce qui manque » et je lis ce qui a été posé (« Posé : … »), et Annuler le retire ».
  - M4 : la branche morte de `hintFault` retirée ou appuyée sur un calcul indépendant ; `Tour7TutoDottedInItsPartTest` étendu à house et hip-hop.
- Parallèle : seul dans la zone tuto. Libre à côté de tous les autres lots (aucun fichier commun), sauf T2.

### Lot T2 « tuto8 » (équipe Opus) · APRÈS T1
Le tuto est une seule zone : T2 est UN lot, une équipe, en deux temps dans cet ordre, car les deux touchent `TutoPanel.kt`, et P7 compte les moments que `TutoKind` expose.

**T2a, P6 : la règle de lisibilité** (audit Fable § 3.5)
- Portée : sortir de `TutoPanel.kt` le calcul de ce qui se montre et sur quelle hauteur, dans une fonction pure ; la consigne passe avant la carte du kit, « Pourquoi ? » et les exemples ; la hauteur d'une ligne se mesure dans la langue affichée. Inclut les restes de lisibilité : D3 « autre » du contrôle (consigne repliée absente sous la carte du kit, ch. 3 et 4 ; coupée en plein mot en japonais), panneau déplié qui cache la grille, L8 (textes coupés), et du siège e : « SONG-EINSTELLUNGEN » coupé par la carte du tuto.
- Fichiers : `app/src/main/java/com/pocketstudio/app/tuto/TutoPanel.kt`, `app/src/main/java/com/pocketstudio/app/tuto/TutoPanelLayout.kt` (neuf), tests et captures `tuto_*`.
- Acceptation : test pur sur tous les états du panneau (replié, déplié, carte du kit, « Pourquoi ? », bouton principal, Montre-moi, Bravo) ; test d'écran fr, ja, de, ko × 100, 130, 200 % × phone360, phone, tablet8 : on lit au moins une ligne entière de la consigne, et si elle est coupée elle finit par « … » avec « Lire la suite ». Phrase : « en ja, deux mains, phone à 100 %, Mode Simple : depuis le Clavier (techno, ch. 3), je lis la consigne repliée et je ne vois aucun mot coupé sans « … » [contrôle tuto2 b] ».

**T2b, P7 : une consigne par moment** (audit Fable § 3.4)
- Portée : le texte d'une étape devient une courte liste, une phrase par moment ; le panneau montre la phrase du moment en cours ; `TutoValidation` refuse un texte Facile qui parle de deux moments ; le test compte les moments du moteur, plus les verbes français. Inclut D4 (« encore là » au contrôle a : 1 bouton + 6 notes dans une consigne), les finitions du siège e (deux boutons qui avancent, « Nächster Schritt » et « Weiter » ; « Schon erledigt » ou « Dein Song hatte schon… » sur une écoute sautée ; même coche pour un chapitre à écoute sautée ; tournure allemande fausse) et ce qui reste de L1 à L9 et L11 après relecture (à cocher un par un, plafond 8).
- Fichiers :
  - `project-model/src/main/kotlin/com/pocketstudio/model/tuto/TutoCourse.kt` (`TutoStep`)
  - `project-model/src/main/kotlin/com/pocketstudio/model/tuto/TutoValidation.kt`
  - `project-model/src/main/resources/tutos/*.json`
  - `project-model/src/main/resources/i18n/*/tuto*.json`
  - `app/src/main/java/com/pocketstudio/app/tuto/TutoPanel.kt`, `TutoController.kt`
  - `app/src/test/.../tuto/Tour7TutoTwoGesturesTest.kt` (remplacé par le compte des moments)
- Acceptation : en Facile, un moment à l'écran (un bouton ou une série, jamais les deux), compté par le moteur sur les 37 cours ; `TutoShowMeEverywhereTest` toujours vert ; traductions dans les 14 langues. Phrases : « en fr, deux mains, phone à 100 %, Mode Simple : depuis le Clavier à l'étape Basse (house, ch. 4), je lis « Touche Créer la piste Basse », puis, la piste créée, « Touche les notes en pointillé de la mesure 1 » [contrôle a #72] » ; « en de, deux mains, tablet8 à 130 %, Mode Pro : depuis une écoute sautée, je reviens en arrière et je lis encore « Beispiel übersprungen », avec un seul bouton pour avancer [contrôle e #3-5] ».
- Contrôle avant le portail : quatre sièges neufs (T-a à T-d plus bas) sur la branche de T2 : 0 chapitre bloqué, 0 grave neuf. Sinon, le lot ne passe pas.
- Parallèle : seul dans la zone tuto ; libre à côté de M, K, B, S.

### Lot M « mixer8 » · IMMÉDIAT
- Portée :
  - D7/U27 : mêmes noms de pistes au Mixer et aux Sons (Synth contre Vocals) **sans réordonner** ni le Mixer ni les puces des Sons (les deux voies déjà essayées cassent `HumanRemixJourneyTest` et `KeyActionsGestureTest`, décision du 2026-11-17 dans `DECISIONS.md`). Le lot décide seul la source unique du nom et note une ligne dans `DECISIONS.md`.
  - Rotation du Mixer (relecture guide2, moyen) : `MixerScreen.kt` l. 187, la bascule de `fit` fait perdre l'état (défilement, fiche de piste ouverte, Effets du master) en tournant le téléphone ou en créant/quittant un remix ; toujours passer par le même `SubcomposeLayout`.
  - Coût du Mixer (même relecture, moyen) : la console composée trois fois à chaque image pendant qu'on glisse un fader ; sonde mesurée sur des entrées indépendantes des faders.
- Fichiers :
  - `app/src/main/java/com/pocketstudio/app/mixer/MixerScreen.kt`
  - `app/src/main/java/com/pocketstudio/app/mixer/Console.kt`
  - `app/src/main/java/com/pocketstudio/app/ui/AppRoot.kt` (seulement `mixerStrips`, l. ~4089)
  - la page des Sons qui montre les noms de pistes : à localiser en tête de lot (`library/LibraryScreen.kt` ou `sounds/SoundBrowser.kt`), une ligne au chef pour l'ajouter à la liste
  - tests sous `app/src/test/.../mixer/` ; `HumanRemixJourneyTest.kt` et `KeyActionsGestureTest.kt` lus, jamais modifiés
- Acceptation :
  - « en de, deux mains, tablet8 à 130 %, Mode Pro : depuis le Mixer puis les Sons, je vois les mêmes noms de pistes [1d #86, #99] » ; `HumanRemixJourneyTest` et `KeyActionsGestureTest` verts sans changement.
  - Test Robolectric : fiche de piste ouverte et colonnes défilées, rotation portrait vers paysage puis retour : la fiche est encore ouverte, le défilement gardé ; idem en créant un remix.
  - Compteur `stripCompositions` : pendant 60 images de glissé de fader, au plus une console composée par image.
- Parallèle : libre à côté de tout (seul lot qui touche `mixer/` et `AppRoot.kt`).

### Lot K « clavier200 » (t4-26) · IMMÉDIAT
- Portée : au Clavier en Mode Simple à 200 %, l'en-tête (pistes, partie, consigne, Clavier/Notes, Enregistrer, « Nouveau son ») pousse les touches sous le pli ; le doigt du rejeu tombe sur une touche au lieu de « Enregistrer ma mélodie ». Défaut de l'appli (`incidents-banc.tsv`, cause « appli (guide) », puis guide2 : en-tête du Clavier).
- Fichiers : `app/src/main/java/com/pocketstudio/app/synth/SynthScreen.kt`, `app/src/main/java/com/pocketstudio/app/record/RecordBar.kt` (à confirmer en tête de lot), tests sous `app/src/test/.../synth/`. Pas `PianoRoll.kt` (zone tuto).
- Acceptation : « en fr, deux mains, phone à 200 %, Mode Simple : depuis le Clavier, je touche les touches 62 et 65 puis « Enregistrer ma mélodie » et la prise démarre » ; rejeu `t4-26-prise-clavier` « oracle tenu » sans retoucher le rejeu.
- Parallèle : libre à côté de tout.

### Lot B « banc9 » · IMMÉDIAT
- Portée :
  - banc8b, moyen : `HumanTester.kt` l. 906-912, cible au milieu non dessiné sans conteneur défilable : 8 glissés verticaux pour rien (~12 s), puis toucher du milieu caché ; viser le centre de la partie dessinée ; plus les mineurs (l. 904 défilement pour toute cible pas entière, l. 919 `drawnAt` muet en cas d'échec, `human-act.py` l. 132, un seul cas dans `BenchAmeneesTest`).
  - banc8, moyens : `handBackNote` avec `replayN = 0` (`SeatOptions.kt` l. 134) ; rendre la main quand un élément sans étiquette a bougé (`fingerGone`) ; `reaimTag` avec `dans:` ; étiquette de conteneur intouchable (`remix_pick` autour de `remix_pick_thumb`) ; `lastDiff` jamais remis à zéro ; écrans d'audit copiés en ressources de test (`BenchShortSeatTest`).
  - TutoAnchor : journaliser les étapes que l'amenée n'a pas su faire.
- Fichiers : `app/src/test/java/com/pocketstudio/app/human/{HumanTester,SeatOptions,ScreenText,TutoAnchor,BenchShortSeatTest,BenchAmeneesTest}.kt`, `app/src/test/resources/bench/**`, `scripts/human-act.py`, `scripts/bench/*`, `docs/audits/ultra/rejeux/*`, `sieges/tour-7/incidents-banc.tsv`.
- Acceptation : test rouge puis vert pour chaque point ; une amenée `tuto:techno:9` en ko, phone360, 130 % arrive sans glissé inutile (compte des glissés dans le journal = 0) ; selftest vert ; rejeux : tous « oracle tenu » sauf t4-26 tant que K n'est pas fusionné.
- Parallèle : libre à côté des lots de l'appli. PAS en même temps que S (même dossier `human/`, même suite de tests) ni que N (mêmes scripts).

### Lot S « stable8 » (test capricieux) · APRÈS B (même dossier ; peut partir IMMÉDIAT si B tarde, en ne touchant que ses deux fichiers)
- Portée : `OrdinaryTestsLeaveNothingTest#c_anOrdinaryTestThatLeavesTheAppsWorkRunningIsRedItself` rouge sous charge (« red itself, by name: null ») pendant un rejeu Gradle à côté ; vert seul.
- Fichiers : `app/src/test/java/com/pocketstudio/app/human/OrdinaryTestsLeaveNothingTest.kt`, `app/src/test/java/com/pocketstudio/app/StrictTestApplication.kt`.
- Acceptation : cause écrite (attente en temps réel, ordre des tests ou nom perdu) ; 20 passages de `com.pocketstudio.app.human.*` verts avec un rejeu Gradle lancé à côté ; aucune quarantaine.

### Lot N « niveaux » (banc, version courte de `PLAN-NIVEAUX.md` § 7) · APRÈS B
- Portée : 4 missions neuves avec oracle qui lit le projet sauvé (`mon-morceau`, `boucle-imposee`, `projet-vide`, `pistes-separees`), oracle de `reprendre` écrit, colonne `niveau` dans les sièges et le tableau de bord par niveau. Condition du « Prochain départ » d'`EN-COURS.md`.
- Fichiers : `scripts/bench/fabrique-sieges.py`, `scripts/bench/seat-metrics.py`, `scripts/bench/test_ultra_bench.py`, gabarits des sièges, tests du banc pour ces missions.
- Acceptation : chaque oracle rouge sur un projet qui ne tient pas la mission, vert sur un qui la tient ; un siège pilote par mission (§ 13.4).
- Si N n'est pas prêt quand les sièges partent : sièges P-c et P-d joués en `libre`, dérogation écrite, niveau confirmé non couvert (comme au tour 7).

### Lot R « restes8 » (moyens des relectures du tour 7) · IMMÉDIAT, facultatif
- Portée : decoupe2 (moyen) : `LaunchedEffect(said)` fait descendre la découpe pour un message resté d'avant ; guide2 (mineurs) : `Column` vide de `GuidePanel.kt` l. 447, estimation des rangées de `AppSpaces.kt` l. 244. Plus les quatre défauts du siège 3a « non localisés » (« ▶ सुनो » sans libellé, −/+ grisés sans mot, piste T ajoutée sans preuve, « 27 पार्ट » pour 20 parties) : d'abord localisés par un lecteur (lecture seule), puis rangés ici s'ils tombent hors des fichiers des autres lots.
- Fichiers : `app/src/main/java/com/pocketstudio/app/remix/CutScreen.kt`, `app/src/main/java/com/pocketstudio/app/guide/GuidePanel.kt`, `app/src/main/java/com/pocketstudio/app/ui/AppSpaces.kt`, plus ceux que le lecteur trouve (une ligne au chef).
- Acceptation : par défaut, sa phrase et son test (ex. « en es, main gauche, phone à 100 %, Mode Simple : j'arrive en « Un trozo » avec un message resté d'avant et je vois la consigne en haut, sans saut »).
- Parallèle : libre, sauf avec un lot qui toucherait les mêmes fichiers (aucun prévu).

### Qui peut tourner ensemble
| | T1 | T2 | M | K | B | S | N | R |
|---|---|---|---|---|---|---|---|---|
| T1 tutokind | | non (zone tuto) | oui | oui | oui | oui | oui | oui |
| M mixer8 | oui | oui | | oui | oui | oui | oui | oui |
| K clavier200 | oui | oui | oui | | oui | oui | oui | oui |
| B banc9 | oui | oui | oui | oui | | non | non | oui |
| R restes8 | oui | oui | oui | oui | oui | oui | oui | |

Au plus quatre discussions à la fois (règle du tour 7) : au départ, T1 (Fable, déjà ouvert), M, K, B ; R prend la place du premier qui finit ; puis T2 après T1, S et N après B.

## Ordre
1. Fusion du tour 7 dans `main` ; ligne « tuto2 fusionné par l'audit Fable » dans `DECISIONS.md`. Lot 0 si les captures ne sont pas refaites.
2. **Départ immédiat, en parallèle** : M, K, B (et R si une place se libère). T1 continue chez Fable.
3. Portail dans l'ordre d'arrivée, CI du lot verte d'abord ; M et K chacun suivi de son siège de contrôle (C-a, C-b) avant le portail.
4. T1 fusionné → T2 ouvert (P6 puis P7) ; B fusionné → S, puis N.
5. Sièges, vague 1 (élargissement, sièges E-a à E-d) dès que M, K et B sont dans l'intégration : ils ne dépendent pas du tuto.
6. T2 fini → contrôle du tuto sur sa branche (T-a à T-d) → portail.
7. Sièges, vague 2 (P-a à P-d, plus ce qui reste) sur l'intégration complète ; tri, lots du tour 9.
8. CI complète, PR vers `main`, revue de fin de phase, fusion ; unités par défaut corrigé mesurées (repère 22 M).

## Les sièges (détail à écrire dans `sieges/tour-8/briefs/`)

Dérogation à la règle des niveaux si N n'est pas prêt (voir lot N). Sinon : curieux (E-b, E-d), débutant (T-a, T-b, T-c, C-b), moyen (E-a, C-a, P-b), confirmé (P-c), pro (T-d, P-d), casse-tout (E-c).

Ce que les tours 6 et 7 ont le moins couvert : langues ar, zh-CN, en, it, nl, pl, tr, id (un siège au tour 6, aucun au tour 7) ; appareils phone411, phoneLand, foldable, tablet11Land (jamais) ; Pro (3 sièges sur 15 au tour 7) ; niveaux confirmé (0) et pro (1) ; entrées Remix, Pad Live, Mes projets, « Fais-moi un morceau ».

| Siège | Langue, appareil, police, main, mode | Entrée | Ce qu'il contrôle ou cherche | Vague |
|---|---|---|---|---|
| T-a | fr, phone, 100 %, deux mains, Simple | `tuto:house:4` puis jusqu'au 5 | D4 et P7 (un moment à l'écran), D3 sous la carte du kit, basse en pointillé hors champ (contrôle a) | contrôle T2 |
| T-b | ko, phone360, 130 %, deux mains, Simple | `tuto:techno:9` jusqu'au 10 | D2 « 인트로 », chapitre 10 bloqué (contrôle c), amenée réparée par banc8b | contrôle T2 |
| T-c | fr, phone, 100 %, deux mains, Simple | `tuto:house:12` | D8, les 3 graves du chapitre 12 (contrôle d), « Poser ce qui manque » (M2) | contrôle T2 |
| T-d | ja, foldable, 200 %, deux mains, Pro | `tuto:techno:3` | P6 en japonais à 200 % (contrôle b), tuto Pro, appareil jamais joué | contrôle T2 |
| C-a | de, tablet8, 130 %, deux mains, Pro | rejeu de 1d (tour 7) | D7/U27 noms Mixer et Sons ; Mixer en Pro | contrôle M |
| C-b | fr, phone, 200 %, deux mains, Simple | rejeu `t4-26-prise-clavier` | t4-26 : touches et « Enregistrer ma mélodie » | contrôle K |
| E-a | ar, phone411, 130 %, deux mains, Simple | aucune (premier lancement) | droite à gauche, premier morceau, Guide ; grand téléphone jamais joué | 1 |
| E-b | zh-CN, phone360, 100 %, main gauche, Simple | `amenee=remix` | Remix et découpe (decoupe2) en chinois, pouce gauche | 1 |
| E-c | en, phoneLand, 100 %, deux mains, choisi par le siège | `amenee=padlive` | casse-tout en paysage : Pad Live, Mix en paysage (rotation vérifiée par test, pas par le banc) | 1 |
| E-d | nl, phone, 130 %, deux mains, Simple | aucune | 55 ans « un morceau calme pour ma fille » (N0 c), textes coupés, Mes projets puis reprendre | 1 |
| P-a | it, tablet11Land, 100 %, deux mains, Pro | aucune | Pro sur grande tablette en paysage, Mixer et courbes, export | 2 |
| P-b | pl, phone, 100 %, deux mains, Pro | aucune | moyen venu de FL Studio Mobile (`boucle-imposee` si N, sinon `libre`) | 2 |
| P-c | tr, phone, 130 %, deux mains, Pro | projet vide | confirmé, refuse l'assistant (`projet-vide` si N) | 2 |
| P-d | id, tablet8, 100 %, deux mains, Pro | aucune | pro, pistes séparées (`pistes-separees` si N), exports | 2 |

14 sièges ; quatre au plus en même temps (lots A à D du banc, une lettre par lot). Avant chaque lot : `scripts/bench/selftest.sh sieges/tour-8/lot<X>`, Gradle par `scripts/bench/gradle-file.sh`. Rapport au gabarit `GABARIT-RAPPORT-TESTEUR.md`, rubrique `defauts-avant:` pour les contrôles. Texte seul dans le dépôt. Si une amenée ne mène pas au bon écran : siège normal du même profil, noté.

## Sorties
- `sieges/tour-8/synthese-tour-8.md` : tentés devenus corrigés (dont tuto2 et fable-repar), encore là, pas atteints ; défauts neufs avec verdict et gravité ; une ligne par niveau si N est fusionné.
- `sieges/tour-8/lots-correction.md` : lots aux fichiers disjoints pour le tour 9 ; ce qui relève du banc à part.

## Les risques
1. **T1 (P8) déborde** : migration type par type d'un moteur de 2 700 lignes. T2 attend, donc tout le tuto attend. Parade : T1 fusionne par tranches (types migrés, test P1 vert à chaque tranche) ; si T1 n'a rien fusionné après une journée de quota, T2a (P6, qui ne touche que `TutoPanel.kt`) part sur la dernière tranche fusionnée, et M2 de T1 lui est confié pour ne pas partager `TutoPanel.kt`.
2. **T2b touche 37 cours × 14 langues** : traductions longues et coûteuses ; risque de textes faux en langues peu relues. Parade : reprendre les traductions relues (comme tuto2), un sous-agent relecteur par famille de langues, P7 d'abord en Facile seulement.
3. **Une nouvelle fusion accidentelle** (branche d'audit partie d'un lot non contrôlé). Parade : le portail refuse une branche de doc qui porte du code de l'appli ; chaque branche d'audit part de la tête de l'intégration.
4. **D7 sans réordonner** peut ne pas tenir sur 360 dp si les noms communs sont plus longs. Parade : prendre le nom le plus court des deux ; si ça casse encore un des deux tests gardés, D7 reste au tour 9 avec une ligne dans `DECISIONS.md` (troisième essai = alternative suivante).
5. **La rotation ne se joue pas au banc** (aucun geste « tourner »). Elle n'est prouvée que par le test Robolectric du lot M ; à mettre dans la liste de recette du téléphone de Benjamin (un seul essai, groupé avec les autres).
6. **Le test capricieux masque d'autres rouges** pendant le tour (portail bloqué à tort ou rouge ignoré). Parade : S passe tôt ; d'ici là, un rouge de `#c` seul se relance une fois, et toute autre classe rouge arrête le portail.
7. **Captures périmées** : un lot qui refait des captures hors de sa liste recouvre un autre lot. Parade : lot 0 d'abord ; ensuite, chaque lot ne refait que ses captures.
8. **Quota** : 14 sièges plus 7 lots dépassent le tour 7 (≈ 18 M de sièges). Version courte : T-a à T-d, C-a, C-b et E-a à E-d (10 sièges) ; la vague 2 passe au tour 9.
9. **Langues jamais jouées** : défauts de traduction en masse dans les sièges E (ar, zh-CN, nl). Ils sont triés « moyens » et rangés dans un lot langues du tour 9, sans gonfler le tour 8.

## Ce que le tour 8 ne regarde pas
Le son et les décrochages (téléphone seulement), le thème sombre, la rotation au banc (test seulement), les écrans qu'aucun siège n'atteint (listés « non vus »).

- Banc (vu sur t4-26) : HumanTester annonce « DOIGT » sur un bouton recouvert par un élément non cliquable (carte du « Fait ») ; le chevauchement ne sort qu’en avertissement « LAYOUT OVERLAP » dans le journal. Faire échouer le doigt quand un élément dessiné couvre le point visé.
