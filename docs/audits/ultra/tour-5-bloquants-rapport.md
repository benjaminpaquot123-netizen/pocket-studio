# Lot « bloquants » : rapport de l'auteur

Date : 2026-11-10 · branche `eq/tour-5-bloquants` (base fx-int 9d50148) · un seul auteur, aucun sous-agent · rien n'est poussé.
Étape 2 du plan d'action (`PLAN-ACTION-AUDITS.md`), dernier lot de la première vague : les trois bloquants du tour 5 (`tour-5-synthese.md`, lignes 8, 33, 50, 98 et contre-vérifications 3 à 5).

## En trois lignes

1. Changer de partie en Mode simple marche sur l'appli d'aujourd'hui (pas reproduit, même sur le téléphone du siège) ; mais pendant la lecture rien ne disait que le choix était pris avant la mesure suivante : l'en-tête le dit maintenant.
2. La mélodie perdue après Pad Live n'est pas reproduite, même par le chemin exact du siège (corbeille comprise) : les 4 notes restent. Rien changé ; deux gardes ; essai téléphone à faire.
3. Le Swing du tuto Pro n'était pas absent mais caché sous la grille : « Je le fais » mène maintenant au réglage Swing, dans les 19 tutos qui le demandent.

Tous les tests sont dans `app/src/test/java/com/pocketstudio/app/human/Tour5BloquantsTest.kt` : la vraie appli sous Robolectric avec le banc (`HumanTester`), pilotée par gestes (touches, Retour, attentes), comme les tests du banc.

## 1. A a #27 : en Mode simple, choisir une autre partie ne change rien

- **Reproduction.** Trois tests refont le chemin du siège : morceau Lo-fi en Mode simple, « Écouter » du Guide (le morceau joue depuis A · Intro), onglet Clavier, en-tête « Partie : A · Intro » › C, puis › B, puis « + Nouvelle partie ». En français (`simplePartsPickedAfterTheGuidesListenAreEdited`), sur le téléphone du siège (hindi, 360 dp, police 130 %, `simplePartsPickedOnTheSeatsPhone`), et un plus court en Techno (`simplePartPickedWhileTheSongPlaysIsTheOneEdited`). **Sur l'appli et le banc d'aujourd'hui, la partie choisie prend bien la main** à la barre de mesure suivante, l'éditeur et l'en-tête la montrent.
- **Cause probable du bloquant.** Le choix pendant la lecture n'est appliqué que quand le moteur change de partie (`selectScene` → `stageScene`, l'écran suit le moteur). Au tour 5 (2026-11-05), le faux moteur du banc ne jouait pas ces changements : ils y ont été ajoutés le 2026-11-08 (`4168889`). C'est la même cause que T5-Fb-73 (Pro, même geste), classée « banc » et vérifiée sur téléphone par Benjamin (recette tel-13). Je ne l'ai pas prouvé en rejouant l'ancien banc (le rejeu `human-replay.sh` exige l'auto-test complet du banc, zone de l'autre agent).
- **Défaut réel trouvé et corrigé.** Juste après le toucher, l'en-tête nomme encore l'ancienne partie jusqu'à la barre suivante (près de 3 s à 84 BPM) : rien ne dit que le choix est pris. Test rouge `b9d4aed` (« + Nouvelle partie » : l'en-tête dit encore B ; C et B passaient selon le moment). Correction `1d3d4aa` : tant qu'une partie attend sa mesure (`sessionUi.pending`), la ligne sous l'en-tête dit « M arrive à la prochaine mesure… » (`partHeader(…, waiting)`), en Simple comme en Pro. Mots dans les 14 langues (nouvelle zone `bloquants`).
- **Captures regardées** (prises par le test, non commitées) : Clavier en français après « + Nouvelle partie » (« Partie : B · Couplet », dessous « M arrive à la prochaine mesure… », rien de coupé) ; la même en hindi à 130 % sur 360 dp (« M अगले बार से शुरू होगा… », tient sur une ligne). Aucune capture de référence ne change. **Erreur de cette version, corrigée après relecture (voir « Après relecture ») : la ligne s'affichait aussi pendant toute écoute du morceau, en nommant sa partie suivante.**
- **Garde dans `verdicts.tsv`** : aucune ligne pour A a #27. Rien écrit (§ 12.3).

## 2. E a #117 : prise de mélodie perdue après Pad Live

- **Reproduction tentée.** `aMelodyTakeLeftForPadLiveIsKept` : Mode simple, Clavier, « Enregistrer ma mélodie », 4 touches pendant la prise, Retour (la prise tourne encore), « Pad Live » sur l'accueil (son projet est créé), Retour, « Mes projets » › le morceau. `aMelodyTakeLeftForPadLiveIsKeptInASongBackFromTheTrash` : le chemin du siège (gestes 90 à 149) : un deuxième morceau par « Créer et écouter », supprimé depuis « Mes projets » pendant qu'il est ouvert, « OK », restauré de la corbeille et rouvert, puis la prise, Retour, Pad Live, réouverture. Les deux vérifient les notes dans le projet **et ce que le siège a regardé** (Clavier, piste Mélodie, Notes, « Grille des notes »).
- **Résultat : vert dans les deux cas.** Après réouverture, la partie montrée est « I · Montée » comme chez le siège, et la grille dit « 4 notes » (le siège lisait « 0 notes »).
- **Pas de correction.** Le code de la prise (`recordTake`, `stopRecordingTake`, `goHome`, `switchProject`, `ProjectSession`) n'a pas changé depuis le tour 5 (`git log e9bae3e..` sur ces fichiers : seulement le passage du changement de projet sur le fil principal, `fb1ec39`). Le banc, lui, a beaucoup changé depuis (horloge du morceau, fin de prise, positions locales à la partie : `4168889`, `87b6693`, `2e650ca`, `7bfe58b`, `b8a403f`…). Je n'ai pas trouvé laquelle de ces différences explique la perte ; je ne peux pas exclure un défaut de l'appli qui ne se montre que sur un vrai téléphone (moteur réel, vraie horloge).
- **Gardes.** Les deux tests restent (commit `7915041`, « garde: », pas de paire rouge/vert puisqu'il n'y a pas eu de rouge).
- **Garde dans `verdicts.tsv`** : aucune ligne pour E a #117. Rien écrit (§ 12.3) ; le verdict appli ou banc revient à l'agent banc.

## 3. B c #23 : le réglage Swing introuvable dans le tuto Pro

- **Vérification d'abord.** Pas absent : le réglage Swing existe, dans la carte « Métronome · Décompte · Swing » sous la grille des pas, en bas de Sounds › Drums. L'étape « swing » des tutos pointait `rhythm.pattern` : « Je le fais » montrait la grille des pas, en haut, et le Swing restait loin dessous. C'était donc « trop caché », et l'étape menait au mauvais endroit.
- **Test rouge** `ffa273e`, `theTutorialsSwingStepShowsTheSwingSetting` : tablette 8 pouces en allemand, Pro, tuto Techno, chapitre 1 comme le siège (Weiter, tempo 132, fa phrygien, Geschafft, « Später » s'il est proposé), étape swing › « Ich mach's » : le mot « Swing » doit être à l'écran en entier, au-dessus du panneau du tuto. Rouge, avec exactement l'écran 25 du siège.
- **Correction** `8f3cd94` (+ `b5bf7b1` pour l'espacement) : le réglage Swing est un spot à lui (`rhythm.swing`, étiquette « Swing ») ; `GuideNav` l'ouvre sur l'onglet Rythme ; `TutoValidation` l'accepte ; les 19 tutos qui demandent du swing (`check: swing`) le pointent. « Je le fais » le fait venir à l'écran (même mécanisme que les autres étapes), « Montre-moi » y pose le doigt. `docs/tuto/AUTHORING.md` à jour.
- **Effet de bord trouvé par la suite complète, corrigé.** L'étape Swing fait maintenant descendre la page ; l'étape suivante (la grille des pas, plus haut) remontait avec le haut de la grille sous « Partie : A » : `TutoJourneyTest#theWholeCourse[techno Pro]` rouge (« coupé en haut, sous l'en-tête ») dans la suite complète sur `b5bf7b1`, et rouge encore avec la classe seule sans correctif (74 tests, 1 rouge). Correction `dd31ec4` dans `SpotNode.reveal` (`Spotlight.kt`) : après les deux mises en vue, si le haut de la cible est encore coupé, sa première ligne est ramenée. Vert : 74 sur 74. Ce changement touche toutes les mises en vue du tuto et du Guide (d'où la deuxième suite complète).
- **Capture regardée** (prise par le test) : après « Ich mach's », la carte « Metronom · Einzähler · Effekte / Swing 0 % » et son curseur sont entiers juste au-dessus du panneau « Tempo und Tonart, Kapitel 1/16 · Schritt 4/4 ».
- **Garde dans `verdicts.tsv`** : aucune ligne pour B c #23. Rien écrit.

## 4. Contrôles

| Contrôle | Résultat |
|---|---|
| Suite complète 1 (`:project-model:test :app:testDebugUnitTest :app:verifyRoborazziDebug`, PS_STALL_MIN=70), tête `b5bf7b1`, 26 min | app : 8 156 tests, 6 rouges (5 images instables connues + `TutoJourneyTest#theWholeCourse[techno Pro]`, effet de bord corrigé dans `dd31ec4`) ; project-model : 1 074 tests, 0 rouge |
| `TutoJourneyTest` seule, sans puis avec `dd31ec4` | 74 tests : 1 rouge (techno Pro) sans, 0 rouge avec |
| Suite complète 2, même commande, tête `dd31ec4`, 25 min | app : 8 156 tests, **4 rouges**, 4 480 sautés : `ScreenshotTest#recette`, `CutScreenshotTest#cutScreen` et `#cutScreenMagnifier`, `PadHoldAndTrackDeleteTest#scrolledThePadsFadeUnderThePartLine` (images instables connues, hors lot, non réenregistrées) ; project-model : 1 074 tests, 0 rouge (`I18nCatalogTest`, `LexiconWordsTest`, validation des 37 tutos comprises) |
| `Tour5BloquantsTest` dans la suite 2 | 6 verts (+ `drive` sauté hors banc) |
| Images enregistrées | aucune (aucune capture de référence ne change) |
| Liens symboliques | aucun commité |

Le banc (`scripts/bench`, `verifie-chantier.py`, `Tour3RenameIdleTest`, `AuditFideliteTmpTest`) n'est pas touché. `verdicts.tsv` n'est pas touché.

## Commits

| Commit | Quoi |
|---|---|
| `b9d4aed` | rouge: `Tour5BloquantsTest#simplePartsPickedAfterTheGuidesListenAreEdited` (A a #27) |
| `ffa273e` | rouge: `Tour5BloquantsTest#theTutorialsSwingStepShowsTheSwingSetting` (B c #23 ; le fichier contient aussi les tests de E a #117) |
| `1d3d4aa` | vert: A a #27 (ligne « … arrive à la prochaine mesure… », 14 langues) |
| `8f3cd94` | vert: B c #23 (spot `rhythm.swing`, 19 tutos) |
| `7915041` | garde: E a #117 non reproduit (deux tests, aucun changement de l'appli) |
| `b5bf7b1` | l'espacement de la carte Swing gardé ; 3 lignes DECISIONS |
| `dd31ec4` | vert: `TutoJourneyTest#theWholeCourse[techno Pro]` (mise en vue qui remonte) |
| (dernier) | ce rapport, 1 ligne DECISIONS |

Honnêteté : le commit `ffa273e` a été amendé une fois avant tout autre commit (le test de la corbeille ne compilait pas : `hasTestTag` n'a pas de `substring`) ; le rouge du Swing avait été vu sur la version précédente du fichier, ce test-là n'a pas changé.

## 5. Essais à demander à Benjamin plus tard (pas maintenant)

1. Mode simple, un morceau qui joue, Clavier : toucher « Partie : A » et choisir une autre partie. Est-ce qu'on y passe à la mesure suivante, et la petite ligne « … arrive à la prochaine mesure… » se voit-elle avant ?
2. Mode simple, Clavier, « Enregistrer ma mélodie », jouer 4 notes, appuyer sur Retour sans arrêter, ouvrir Pad Live, revenir, rouvrir le morceau : les 4 notes sont-elles là (Clavier › Notes) ?
3. Tuto Pro (Techno), chapitre 1, étape du swing : « Je le fais » montre-t-il le réglage Swing tout de suite ?

## 6. Doutes

- A a #27 et E a #117 : « pas reproduit » sur le banc d'aujourd'hui n'est pas « corrigé ». Pour A a #27 la cause banc est très probable (même cause que T5-Fb-73, vérifié sur téléphone) ; pour E a #117 je n'ai pas de cause.
- La ligne « arrive à la prochaine mesure » dépend de `sessionUi.pending` : si un jour le moteur ne fait jamais le changement (moteur arrêté en route), la ligne resterait jusqu'au prochain ■. Je ne l'ai pas vu arriver.
- Les traductions de « %s arrive à la prochaine mesure… » sont de moi, sans relecture (anglais et néerlandais vérifiés pour le sens).
- Le test de A a #27 ne voit l'en-tête en retard que sur « + Nouvelle partie » de façon sûre ; pour C et B, il dépend du moment du toucher dans la mesure.

## 7. Classes à rejouer à la fusion

`Tour5BloquantsTest` (les 6 tests), `PlayStripLogicTest`, `ScreenshotTest` (en-têtes d'éditeurs), `NavScreenshotTest` et ses variantes (Rythme, tuto), `TutoKickSoundTest`, `JourneyPathsTest`, les tests de tuto (`Tuto*`), `GuideNav*`, `BenchRecetteTest#proPartPickedWhilePlayingTakesOver`, et côté catalogues et tutos `:project-model:test` (`I18nCatalogTest`, `LexiconWordsTest`, la validation des tutos).

## Après relecture (2026-11-10)

Relecture : `tour-5-bloquants-relecture.md` (1 grave, 2 moyens, 5 mineurs). Ce fichier n'est pas touché.

| Constat | Ce qui a changé | Preuve |
|---|---|---|
| **Grave G1** : « … arrive à la prochaine mesure… » pendant toute écoute du morceau, en nommant sa partie suivante | Cause : en mode morceau, `sessionUi.pending` est la partie suivante du morceau, préparée pendant toute la partie en cours (`stageSongPart`). Un choix de partie pendant le morceau quitte d'abord le mode morceau (`selectScene` pose `SessionUi(pending = …)`, `songIndex` à −1) ; un choix de partie du morceau (« jouer depuis ») ne fait que la mettre en file pour la fin de la partie, pas à la mesure suivante. Le bon critère est donc bien « hors mode morceau » : `partHeader` ne dit la phrase que si `playing && !songPlaying`. En mode morceau, « Le morceau joue : touche la grille… » revient. | Rouge `0793def` : `noWaitingLineWhileTheSongJustPlaysInSimple` (« Partie : A · Intro / C · Refrain arrive… », `SessionUi(pending=2, songIndex=0)`) et `…InPro` (« Partie : E · Intro 2 / F · Intro 3 arrive… ») ; vert `9bc590e` (même noms) ; `simplePartsPickedAfterTheGuidesListenAreEdited` et `…OnTheSeatsPhone` restent verts (la phrase après « + Nouvelle partie » est toujours là). |
| G1, textes | La ligne DECISIONS de A a #27 et la § 1 de ce rapport corrigées (la phrase ne vient que pour une partie choisie hors du mode morceau). | — |
| Moyen M1 (la phrase reste si le moteur ne bascule jamais) | Non changé, déjà dans les doutes (§ 6) ; à voir à l'essai téléphone n° 1. | — |
| Moyen M2 (paire rouge/vert de A a #27 étroite) | Les deux nouveaux tests couvrent maintenant l'écoute sans choix, en Simple et en Pro. | ci-dessus |
| Mineur m3 (gardes E a #117 : « pas 0 note ») | Les deux gardes vérifient maintenant que la grille montre autant de notes que la prise en a écrit. | dans `0793def`, verts au passage ciblé |
| Mineurs m1, m2, m4, m5 | Non changés (constats sans défaut, ou déjà dits). | — |

**Captures regardées** (prises par les tests, non commitées) : Simple pendant « Écouter » sans choix (Clavier, « Partie : C · Refrain », dessous « Le morceau joue : touche la grille et cette partie reste sous tes doigts, en boucle. ») ; Simple juste après « + Nouvelle partie » (« Partie : B · Couplet », dessous « M arrive à la prochaine mesure… ») ; Simple après C (« Elle tourne en boucle pendant que tu la modifies… »). Aucune capture de référence ne change.

**Passages ciblés** (tête `9bc590e`) :
- `testDebugUnitTest` : `Tour5BloquantsTest` 9 tests (8 verts, `drive` sauté), `TutoJourneyTest` 74 verts, `PlayStripLogicTest` 6 verts, `ScreenshotTest` 133 verts.
- `verifyRoborazziDebug` : `Tour5BloquantsTest`, `ScreenshotTest`, `NavScreenshotTest` : 1 rouge, `ScreenshotTest#recette` (image instable connue, hors lot) ; `NavScreenshotTest` 30 verts.
- Pas de nouvelle suite complète après ce correctif (une ligne de `PartHeader.kt` et des tests).
