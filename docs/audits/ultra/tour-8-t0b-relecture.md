# Tour 8, t0b « gestes du tuto » : relecture neuve
Relu : b962454d

Portée : `git diff ff64ff0a...d641f3cc -- app project-model` (17 fichiers, +642 −18) : TutoController.kt (« Je le fais »), TutoPanel.kt (bouton DO, partie vide, anneau), TutoPlay.kt (`doHere`, `emptyPart`, `ringed`), TutoChecks.kt (`placed`, `TutoRollHint.counted`), RhythmScreen.kt (outil, anneau, marques de AllPadsGrid), PianoRoll.kt (compteur), les tests neufs Tour8TutoGestures* (DoIt, Tool, EmptyPart, AllPads, Montee, Counter, Ring), les retouches de TutoJourneyTest et TutoLot4Test, et `docs/decisions/tour-8-t0b.md`. Joué : `:project-model:test` sur Tour7TutoMadePart* et Tour8TutoGestures* (tout vert : Tour7TutoMadePartPlacedTest 1/1, Counter 2/2, Montee 1/1, Ring 4/4). Tests app lus, non joués.

Points vérifiés sans défaut :
- Rien dans le moteur ni le callback audio ; aucun `tr(` neuf ou changé, aucune ressource, aucune chaîne i18n.
- « Je le fais » (`TutoController.kt:573-584`) : l'effet `go` est émis, le panneau se replie, puis `edit { doHere(…) ?: this }` et `look()` ; un résultat nul ne touche pas au projet. DO n'apparaît que si l'étape n'est pas faite et sans `need` (`TutoPanel.kt:1060`).
- `doHere` pour l'envoi et l'insert : seule la tranche visée est copiée depuis `show` ; les autres tranches et le reste du projet restent ceux de l'élève.
- SendTo (`maxOf(v, 0.3/0.25)`) et PutInsert : au-dessus du seuil `sends > 0.05f` du contrôle, donc l'étape passe à « fait » après le geste.
- Partie vide : `partEmpty` est calculé à l'arrivée et au `look` ; la barre d'action cache le bouton (`TutoPanel.kt:1104`) au lieu d'un geste sans effet.
- Anneau : `ringed` donne la plus petite case non remplie de la mesure ; la pulsation est lue dans le dessin du Canvas (pas de recomposition à chaque image).
- Compteur (`PianoRoll.kt:1480-1482`) : une seule règle, `TutoRollHint.counted`, calculée dans le `derivedStateOf`, sans écriture d'état.
- `placed` : Tour7TutoMadePartPlacedTest (tous styles et niveaux, partie renommée) reste vert avec la nouvelle règle.
- TutoJourneyTest l.453-461 : quand `show == null`, le test exige `done(...)` vrai ; aucune branche ne passe sans vérification.
- Commits rouges (c6508406, f5ae49d4, f2d1af61, e9e38ec2, 44e205ee, 929befd9, aa3e456b) : tests seuls, la correction vient après. Décisions tracées dans `docs/decisions/tour-8-t0b.md`.

Constats :
1. moyen, `TutoPlay.kt` (`doHere`, branche automation) et `TutoController.kt:582` : pour une courbe, « Je le fais » prend tout `TutoChecks.show` (le projet passé par `clean`), et non la seule courbe. La courbe de même cible déjà posée par l'élève est écrasée (`TutoChecks.kt:1484-1515`). Le risque est limité, puisque DO est caché une fois l'étape faite, mais le geste est plus large que l'étape. À resserrer comme pour l'envoi : copier la seule automation et l'insert FILTER.
   Corrigé en 509081a0 (rouge a50897e9) : `doHere` ne prend plus du show que la liste des courbes et les inserts des tranches visées par une courbe neuve. `curve` ne change rien d'autre (`withInsert` touche seulement les inserts de la tranche ; la 303 vise `ACID_CHANNEL` sans insert), donc rien d'utile n'est perdu et l'étape coche toujours. Bornes sûres (`mapIndexed` + `getOrNull`). Courbe de même cible : remplacée, comme le fait `curve`. Plus de place pour une courbe (`MAX_LANES`) : aucun changement, `doHere` rend null. Le test (techno, hard_techno, acid_techno, style de 80 lettres que `clean` coupe à 64) échoue sur l'ancien code et passe : 3/3 vert sur b962454d. `TutoController.doIt` inchangé, même signature.
2. moyen, `TutoChecks.kt:2264` (`placed`) : la partie créée se place désormais juste avant la suivante dans l'ordre du cours, donc après les parties hors cours qui la précèdent. Exemple : un Fill d'une mesure posé avant le Drop 2 se retrouve avant la Montée créée. C'est voulu par la fiche et Tour7 reste vert ; aucun test ne couvre une partie hors cours entre les deux.
3. mineur, `RhythmScreen.kt:604` : `LaunchedEffect(tutoStep, tutoLeft.isNotEmpty())` remet l'outil sur « Coups » au premier affichage, et aussi chaque fois que `tutoLeft` redevient non vide pendant l'étape (étape de roulements, retour sur la grille ou sur le pad). L'élève peut perdre l'outil qu'il venait de choisir.
4. mineur, `TutoPlay.kt` (`emptyPart`) : la fonction lit `p.allScenes[scene]`. Tant que le projet n'est pas synchronisé, la scène affichée peut être en retard si la Montée est la partie à l'écran ; le bouton reste alors caché ou visible une image de trop.
5. mineur, `RhythmScreen.kt:751-757` (`ringStep`) : l'anneau tient une case couverte (`covering`) pour remplie, alors que `tutoLeft` et les pointillés ne la comptent pas ainsi. L'anneau peut sauter une case encore pointillée.
6. mineur : les rouges de l'anneau et du compteur (`Tour8TutoGesturesRingTest`, `Tour8TutoGesturesCounterTest`) n'échouent que par compilation (fonctions absentes). Le branchement de l'anneau dans RhythmScreen n'est prouvé que par une capture (`tuto_app_snare.png`), sans test de logique.
7. mineur : TutoLot4Test et TutoJourneyTest étaient rouges sur la branche entre 0a172989 et 12c5ca6a / c6949e52, deux commits « test: » ajustés après le vert. Le seuil de TutoLot4Test (envoi > 0f) est faible, mais ses deux « ✓ » couvrent le seuil réel de 0.05.
8. mineur, `TutoPlay.kt` (`doHere`, courbe) : la liste des courbes vient entière du show, donc passée par `AutomationLane.sanitize`. Les autres courbes de l'élève peuvent être retouchées par ce nettoyage (seulement si elles étaient hors bornes). Le test part d'un projet sans courbe et ne couvre pas une courbe déjà posée.

Verdict : 0 bloquant, 0 grave — fusion possible
