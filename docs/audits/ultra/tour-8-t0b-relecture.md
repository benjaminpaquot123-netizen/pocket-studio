# Tour 8, t0b « gestes du tuto » : relecture neuve
Relu : cefa3587

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

## Contrôle e, ch.13 : courbe par partie

Portée : `git diff b49ce1e0..59f039e7 -- app project-model` (5 fichiers, +122 −9) : TutoChecks.kt (`LOOP_SHAPE`, `curveDrawn`, `shaped`, `curve`, `withPartCurve`), TutoPlay.kt (`doHere` montre le Drop), techno.json et acid_techno.json (« filter » : `automation:wave` → `automation:loop`), Tour8TutoGesturesDoItTest.kt.
Paires rouge → vert :
- ab1db486 → 81f2133a : étape « filter » (ch.13 techno, ch.9 acid_techno, Pro) en `automation:loop` ; `curve` dessine une courbe `PER_SCENE` sur la boucle d'une mesure du Drop ; `curveDrawn` refuse une piste « Morceau » pour cette étape ; `doHere` affiche le Drop.
- 37155b29 → 59c46d03 : « filter_intro » (acid_techno, `automation:rise` sur l'Intro 2, même coupure de la 303) : sur une piste déjà « Par partie », la montée va dans la boucle de l'Intro 2 et garde celle du Drop (`withPartCurve`).
- 2aa35c3d → 59f039e7 : le moteur tient une valeur « Par partie » jusqu'à la suivante sans glisser (`Engine::applyAutomation`), donc chaque pas est posé : triangle par mesure pour la boucle, rampe pour la montée.
Joué (par le chef, vert) : Tour8TutoGestures*, TutoJourneyTest, TutoLot4Test, tests *Tuto* de project-model. Les rouges ne touchent que le test ; le vert 81f2133a retouche aussi l'ancien test « Je le fais » des courbes (scène attendue = le Drop), ce que le changement demandé impose.

Points vérifiés sans défaut :
- Pas de coche sans geste : `curveDrawn(…, "loop")` exige une piste `PER_SCENE` sur la coupure avec une forme montée-descente dans les valeurs du Drop ; une piste « Morceau » ou « Boucle » ne coche pas (testé). Aucune étape antérieure ne pose une telle piste.
- Pas de plantage : le Drop vide ou absent → `sceneLabelled` < 0 → null ; `bar` ≥ 8 (2/4 à 7/4), donc `bar / 2` ≥ 4 et `bar - 1 - bar / 2` ≥ 3 (et la branche descendante n'est jamais prise pour `bar` = 2) ; `coerceIn(bar, 64)` sûr car `bar` ≤ 28 ; la rampe a `steps` ≥ 2, donc pas de division par zéro ; `sceneValues` gardent 64 pas, `null` au-delà de `Scene.stepsFor`.
- `switchScene` passe par `syncScenes` : la partie à l'écran est rangée avant de montrer le Drop, rien n'est perdu ; seules les courbes et les inserts des tranches visées viennent du show.
- `withPartCurve` remplace la seule boucle de la partie visée et garde celles des autres parties (testé : Drop gardé après la montée de l'Intro 2, les deux étapes cochées).
- Les autres styles : la branche `PER_SCENE` ne s'ouvre que si une piste « Par partie » existe déjà sur la cible, et seulement pour `rise`/`fall` ; sans elle, `wave` et `rise` tracent comme avant une courbe « Morceau ».
- Le test vérifie vraiment : mode, scène montrée, triangle pas à pas (montée puis descente, chaque pas posé), coche après le geste et refus de l'ancienne courbe « Morceau ».

Constats :
9. mineur, `TutoChecks.kt` (`curve`, branche `PER_SCENE` rise/fall) et `acid_techno.json` (« filter_intro ») : la montée est tracée sur la boucle de l'Intro 2 et rejouée à chaque tour de boucle ; si cette boucle fait une mesure, le filtre se referme à chaque mesure pendant les 16 mesures de l'Intro 2, alors que le texte dit « une courbe qui monte du début à la fin : la ligne arrive fermée et s'ouvre ». Conséquence d'une piste à un seul mode ; reformuler le texte (« sur la boucle de l'Intro 2 ») ou l'assumer dans le « pourquoi ». De plus la piste garde `loopSteps = 0` : le moteur et la carte d'automation lisent la courbe sur la boucle de batterie ; si la plus longue boucle de la partie (`stepsFor`) dépasse la batterie, la fin de la rampe n'est ni jouée ni vue.
10. mineur, `TutoChecks.kt` (`curveDrawn`, `LOOP_SHAPE`) : la forme est cherchée dans les 64 valeurs du Drop, y compris au-delà de sa boucle que le moteur ne joue pas ; une vague dessinée sur les pas 16-63 d'un Drop d'une mesure coche sans s'entendre. Peu probable à la main.
11. mineur, `TutoChecks.kt` (`withPartCurve`) : `filterNot { it.target == t }` retire une piste « Morceau » ou « Boucle » de même cible déjà posée par l'élève (ex. une courbe libre sur le filtre de la Mélodie avant le ch.13). Seulement par « Je le fais » ; l'appli ne tient de toute façon qu'un mode par cible.
12. mineur, `TutoChecks.kt` (`curve`, forme `wave`) : avec une piste déjà « Par partie » sur la cible (élève qui l'a passée lui-même en « Par partie », autres styles), `wave` retombe dans la branche « Morceau » et remplace toute la piste, boucles des parties comprises. Antérieur au lot, hors de la nouvelle branche (rise/fall seulement).
13. mineur, `TutoPlay.kt` (`doHere`, boucle) : quand aucune piste n'a de place (`MAX_LANES` = 16 et cible neuve), `take(MAX_LANES)` jette la nouvelle piste, mais la scène est quand même passée au Drop : « Je le fais » change de partie sans rien dessiner et l'étape reste ouverte (avant, null : rien ne bougeait). Par ailleurs « Montre-moi » (`demo`) finit sur `show` sans montrer le Drop : la courbe peut se dessiner sur une partie qui n'est pas à l'écran.
14. mineur, `Tour8TutoGesturesDoItTest.kt` : rien sur un Drop de plus d'une mesure, une autre mesure que 4/4, `MAX_LANES` plein, ni le chemin « Montre-moi » de l'étape boucle.

## Contrôle d : anneau dans « Tous les pads » (59f039e7..b685bad7)
Rouge afbd3fec (test seul : `theFirstDottedCellIsRingedOnAllThePads` échoue sur l'ancien code), vert b685bad7 (AllPadsGrid, même calcul que `TutoPlay.ringed`), docs cefa3587 (corrections.tsv : d-a-V1-anneau tenté, d-montee voulu, d-22-maracas rangé hors zone). Joué : Tour8Tuto*, TutoJourneyTest, TutoGrilleTest, tout vert. Relecture neuve : 0 bloquant, 0 grave ; pas de texte d'interface neuf ; rien dans le moteur.

15. mineur, `RhythmScreen.kt` (AllPadsGrid) : la case remplie ne compte que `l.hits[it]`, pas `l.covering(it)` comme StepGrid ; un triolet qui couvre une case pointée laisse l'anneau dessus. AllPadsGrid pointait déjà ces cases avant le lot.
16. mineur, `Tour8TutoGesturesAllPadsTest.kt` : pas de test de triolet ni de mesure toute remplie (aucun anneau attendu).
17. mineur : le test de la vue d'un pad (`theFirstDottedCellIsRingedOnThePadsOwnGrid`) est une garde : StepGrid avait déjà l'anneau, il passe aussi sur l'ancien code.

Commits rouges du lot : c6508406, f5ae49d4, f2d1af61, e9e38ec2, 44e205ee, 929befd9, aa3e456b, a50897e9, ab1db486, 37155b29, 2aa35c3d, afbd3fec.

Verdict : 0 bloquant, 0 grave — fusion possible
