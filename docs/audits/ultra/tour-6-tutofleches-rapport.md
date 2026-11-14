# Tour 6 — lot « tutofleches » (lot 10, zone tuto) : rapport

Cibles : les trois sièges de contrôle du tuto (`docs/audits/ultra/sieges/tour-6/controle/{Ha,Da,Bb}/rapport.md`, joués sur `9f26c244` / `d28a6a27`).
Fichiers d'appli touchés : `app/src/main/java/com/pocketstudio/app/synth/PianoRoll.kt`, `app/src/main/java/com/pocketstudio/app/tuto/TutoController.kt`, `app/src/main/java/com/pocketstudio/app/tuto/TutoPanel.kt`, `project-model/src/main/kotlin/com/pocketstudio/model/tuto/TutoChecks.kt`, 17 `project-model/src/main/resources/tutos/*.json`, les `i18n/*/tuto*.json` (15 langues). `RhythmScreen.kt` : inchangé. Aucune capture de référence réenregistrée.
Lot repris après un redémarrage de la machine : la dernière paire (« Accords ») a été finie par un second agent, le reste est du premier.

## Défauts visés

### Corrigés
1. **Grave — flèches qui se contredisent dans le rouleau** (D a ch. 5 : 14 gestes, « もっと上 ↑ » pour une note à droite ; D a ch. 7 : 12 gestes, ↑ puis → puis ↓ pour la même note). Une seule règle, dans le modèle (`TutoRollHint`, `TutoChecks.kt`) : la ligne parle d'UNE note, la mesure d'abord (« plus loin → » / « ← plus tôt »), les rangées ensuite (« plus haut ↑ » / « plus bas ↓ »), aucune flèche quand une note en pointillé est entière à l'écran ; panneau ouvert par-dessus le rouleau : ni haut ni bas. Test de propriété sur toutes les étapes de notes de tous les tutos (`Tour6TutoFlechesTest`, project-model), et le vrai rouleau suivi jusqu'à la dernière note (`Tour6TutoFlechesRollTest`).
   - rejeu impossible : les sièges D a (gestes 77-78, 110-111) touchent des notes du rouleau, dessinées sans étiquette : `rejeu-nouveau.sh` les refuse (« under the finger: rien »), et le lot change la place du rouleau, donc l'endroit de ces touchers. Remplacé par `Tour6TutoFlechesRollTest#technoBassEveryNoteIsReachedByFollowingTheLine` et `#technoRiffEveryNoteIsReachedByFollowingTheLine`.
2. **Grave — après « Copier sur les mesures suivantes », l'écran reste « mesure 1/8 »** (H a, geste 98 : 8 gestes perdus). Le rouleau va de lui-même à la prochaine note à écrire.
   - rejeu impossible : même raison (H a, gestes 96-97 sur des notes sans étiquette). Remplacé par `Tour6TutoFlechesRollTest#houseBassAfterTheCopyTheRollShowsTheNextNote`.
3. **Note à moitié hors du cadre, fausse note posée à côté** (H a, geste 114) : une mesure tient dans la largeur du téléphone et la ligne cale la mesure au bord gauche ; les rangées du bas viennent au-dessus du panneau.
4. **Grave — doubles croches refusées au chapitre 4 du tuto Pro** (B b, geste 46 : 4 gestes). La consigne dit maintenant lesquelles : « sur les doubles croches, sauf les contretemps (le charley ouvert y joue déjà) » (techno, acidcore, 15 langues). La vérification ne change pas (voir les décisions).
   - rejeu impossible : essayé (`rejeu-nouveau.sh … Bb 45`, attendu « außer auf die Offbeats ») ; il diverge au pas 21, avant le chapitre 4 : l'étape du swing, validée à 4 % sur le siège, reste « Schritt 4/4 » ici. Retiré. Remplacé par `Tour6TutoSixteenthsTest#aPadSaidInSixteenthsAcceptsEverySixteenth` (project-model).
5. **« Accords » reste allumé sur une étape de notes simples** (H a ch. 7, D a) : une étape dont les notes en pointillé viennent une par une commence avec « Accords » éteint ; rallumé par l'utilisateur, il reste.
6. **Chapitre 10 : le message de réussite dit l'inverse de la consigne** (D a, geste 155) : « Partie … créée : sans … » se lit dans la partie créée, par son nom.
7. **Montée d'une mesure au lieu de quatre** (D a geste 164, B b geste 163) : une partie qui « monte » dure 4 mesures.
8. **« Chaque partie arrive avec 8 mesures »** (D a) : phrase remplacée (17 tutos, 15 langues).
9. **Tempo : « House 122 BPM » choisi, « 100 » à l'écran** (H a, D a) : l'étape du tempo dit le tempo de départ du projet.

### Pas faits (laissés, avec leur raison)
- « tambourin » alors que le pad s'appelle « Bord de caisse » (H a ch. 2 ; le ch. 4 fait déjà le lien : « sur le pad « Rim » (il joue le tambourin) »), « nomme cette scène » (B b), « Tock », « Ride » / « Becken » (B b), « relances de fin de phrase » (H a), « Encore 9 notes » pour des accords posés d'un toucher (H a), « Offbeats (Steps 4 und 11) » (B b) : il faut changer la phrase française, donc quinze langues à chaque fois. À un lot « textes du tuto ».
- « gepunktete Note » (B b) : `i18n/de/audit1z56.json`, zone Textes. Demande : `i18n/de/audit1z56.json` : « gepunktet » → « gestrichelt » (5 lignes).
- « 手拍子たち » (D a) : `i18n/ja/ultra2z6.json`, zone Textes. Demande : `i18n/ja/ultra2z6.json` : « Claquements de mains ».
- « Rim », « Plaits », « Groove » en lettres latines, « Bass Plaits » (D a, B b) : noms de pads et de pistes, hors des fichiers du lot.
- Le réglage « Reverb-Send » du pad absent du Mix Pro (B b ch. 13, bloqué contourné), « 少し下げる » sous le panneau, le bloc du kit sous le fondu, « Chapitre suivant » dans la bande repliée, le swing validé à 4 % : hors des fichiers du lot ou choix de design.
- Saut d'octave silencieux du rouleau (B b, geste 117) : non repris ; la ligne du rouleau dit maintenant où est la note, mais les deux rangées « F » restent sans chiffre d'octave.

## Paires
- `rouge: the roll's hint leads to the next dotted note, every tutorial step` → `vert: one rule for the roll's hint, the bar first, then the rows`.
- `rouge: after « Copier » the roll shows the next note to write (seat H a)` → `vert: « Copier » done, the roll goes to the next note to write`.
- `rouge: a part made while the song plays is told with its own bricks (seat D a)` → `vert: the part made is told by its name, shown or not`.
- `rouge: a part that builds up « mesure après mesure » is 4 bars long` → `vert: a part that builds up is 4 bars long, a bar for each speed`.
- `rouge: the song step promises no length the parts do not have` → `vert: the song step no longer promises 8 bars a part`.
- `rouge: a pad said « en doubles croches » accepts every sixteenth (seat B b)` → `vert: the closed hats' step says which sixteenths (techno, acidcore)`.
- `rouge: the tempo step says the tempo the project has (seats D a, H a)` → `vert: the tempo step says the tempo a new project starts at`.
- `rouge: on the real roll, following the line reaches every dotted note` → `vert: a bar fits the phone's width, and the lowest rows come above the panel`.
- `rouge: Accords goes off when a step of single notes begins` → `vert: Accords goes off when a step of single notes begins`.
- Quatre de ces paires n'ont pas le même sujet mot pour mot (écrites avant la reprise, histoire non rangée après coup) : le juge les apparie par le nom du test.
- Hors paire : `Tuto roll hint: the line's rule and its scroll moved to the model, unchanged` (déplacement sans changement), `Tuto panel: room for a note under the step's words`, `Acidcore's closed hats now have a verb: out of the pending list`.

## Vérifié (sur `862b3f7fc`)
- Tests du lot : `:project-model:test --tests com.pocketstudio.model.tuto.*` vert ; `:app:testDebugUnitTest` sur `com.pocketstudio.app.tuto.*`, `com.pocketstudio.app.synth.*`, `TutoScreenshotTest`, `TutoPanelFitsTest`, `GestureBudgetTest`, `RhythmGrilleTest` : 280 tests, 3 sautés, **1 rouge une fois** : `TutoJourneyTest#theWholeCourse[trap Pro]` (« Créer la piste 808 » : le bouton n'était plus dans l'arbre au moment du toucher), pendant qu'un autre self-test tournait sur la machine. Relancée seule, la classe est verte (74 sur 74). Instable, cause non cherchée.
- Rouge de la dernière paire : non revu rouge par l'agent de reprise (commité rouge par le premier) ; à rejouer par la relecture sur `79a966cca`.
- Captures : `:app:verifyRoborazziDebug` sur `TutoAppScreenshotTest`, `TutoSightHintTest`, `TutoScreenshotTest`, `PianoRollZoomTest`, `ScreenshotTest` : 160 tests, une seule différence, `folded_cards` (écran Morceau, carte « Briques » : hors du lot, déjà notée par le lot « réparations » comme venant de l'intégration ; regardée, non réenregistrée). Les captures du tuto et du rouleau sont identiques à leurs références.
- Self-test du banc par `scripts/bench/rejeu-defauts.sh` : 179 verts, 0 rouge, 0 en quarantaine, 20 sautés ; rejeu `t4-26-prise-clavier : oracle tenu`. Aucun rejeu ajouté.
- Relecture neuve : **pas faite** par l'agent de reprise (il n'a pas d'outil pour lancer un sous-agent neuf sur cet atelier). À faire par le chef, puis le siège de contrôle (13.5).

tests: com.pocketstudio.app.tuto.Tour6TutoFlechesRollTest com.pocketstudio.app.tuto.Tour6TutoFlechesChordsTest com.pocketstudio.app.tuto.Tour6TutoPanelPartMadeTest com.pocketstudio.app.tuto.Tour6TutoPanelTempoNoteTest com.pocketstudio.app.tuto.TutoSightHintTest com.pocketstudio.app.tuto.TutoGrilleTest com.pocketstudio.app.tuto.TutoScreenTest com.pocketstudio.app.tuto.TutoJourneyTest

chemin: Tuto techno › basse › ligne du rouleau jusqu'à la dernière note (100 %) | test: Tour6TutoFlechesRollTest#technoBassEveryNoteIsReachedByFollowingTheLine
chemin: Tuto techno › basse › ligne du rouleau jusqu'à la dernière note (130 %) | test: Tour6TutoFlechesRollTest#technoBassEveryNoteIsReachedByFollowingTheLineAt130
chemin: Tuto techno › riff › ligne du rouleau jusqu'à la dernière note | test: Tour6TutoFlechesRollTest#technoRiffEveryNoteIsReachedByFollowingTheLine
chemin: Tuto trap › 808 › ligne du rouleau (130 %) | test: Tour6TutoFlechesRollTest#trapBassEveryNoteIsReachedByFollowingTheLineAt130
chemin: Tuto house › basse › « Copier sur les mesures suivantes » (100 %) | test: Tour6TutoFlechesRollTest#houseBassAfterTheCopyTheRollShowsTheNextNote
chemin: Tuto house › basse › « Copier sur les mesures suivantes » (130 %) | test: Tour6TutoFlechesRollTest#houseBassAfterTheCopyTheRollShowsTheNextNoteAt130
chemin: Tuto › accords puis riff › bouton « Accords » du rouleau | test: Tour6TutoFlechesChordsTest#aStepOfSingleNotesBeginsWithChordsOff
chemin: Tuto › étape d'accords › bouton « Accords » du rouleau | test: Tour6TutoFlechesChordsTest#theChordsStepKeepsChordsOn
chemin: Tuto › chapitre 10 › message « Partie … créée » | test: Tour6TutoPanelPartMadeTest#aPartThatLeavesBricksOutIsNotSaidTheWholeGroove
chemin: Tuto › chapitre 10 › message pendant la lecture | test: Tour6TutoPanelPartMadeTest#whatAPartLeavesOutIsReadFromThePartMadeNotFromTheOneShown
chemin: Tuto house › chapitre 1 › étape du tempo | test: Tour6TutoPanelTempoNoteTest#houseTheTempoStepSaysTheProjectsTempo
chemin: Tuto techno › chapitre 1 › étape du tempo | test: Tour6TutoPanelTempoNoteTest#technoTheTempoStepSaysTheProjectsTempo
