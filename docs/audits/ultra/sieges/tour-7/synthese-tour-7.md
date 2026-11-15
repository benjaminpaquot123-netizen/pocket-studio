# Tour 7 : synthèse du tri (lecteur-trieur, 2026-11-15)

Entrée : les 10 rapports `lot{A,B,C}/*/rapport.md`, leurs fiches (`briefs/`), et, pour vérifier un défaut précis, `journal.txt` et `screen-N.txt` du siège. Rien n'a été rejoué ni recompilé.

**Code testé.** Certains rapports écrivent « code testé : 8a8542fe » au lieu de 98e83c21, 28f12607 ou 85497e2f. Les quatre commits portent le même code d'appli, seule la doc change entre eux : tous les rapports jugent donc la même appli.

## En bref

| | nombre |
|---|---|
| défauts réels | **47** : 0 bloquant, **3 graves**, 44 moyens (les « mineurs » du tour 6 comptent ici comme moyens) |
| voulus | 3 |
| artefacts du banc | **11** (+ 1 doute à prouver : B6) |
| doublons | rangés sous leur défaut (T15 → T4, U13 → R3, N14 → R6, coupures du panneau → T3) |
| défauts d'avant (tour 6) | **22 corrigés**, **24 encore là** (dont 3 corrigés dans une langue seulement), 4 pas atteints |

Niveaux couverts (dérogation du plan) : curieux 2d, 3a ; débutant 1a, 1b, 1c, 2a ; moyen 2b, 2c, 3b ; pro 1d. Confirmé : aucun siège.

## Les trois questions posées

**A/b (chapitres 12 à 14 impossibles, Montée, Drop et Drop 2 absents) : artefact du banc.**
L'ancrage `amenee=tuto:house:12` ouvre la liste des chapitres et touche directement le chapitre voulu (`HumanTester.amenee()` : `tuto_chapters`, puis `tuto_chapter_${chapitre-1}`). Les chapitres 10 et 11, qui créent les parties Intro, Montée et Drop, ne sont jamais joués. Côté appli, `TutoController.goTo()` (l. 289) ne fait que déplacer l'étape courante et ne prépare rien.
Preuve, `lotA/b/journal.txt` :
- l. 7-8 : `AMENEE > touche … (tuto_chapters)` puis `(tuto_chapter_11)` ;
- l. 10 : `step=gap done=0` ;
- l. 25-26, geste 3 : le seul choix de partie est « Partie : A », alors que la consigne parle de la « Montée ».

A/d est dans le même cas (`lotA/d/journal.txt` l. 7-8 : `tuto_chapter_7`, `done=1`, et le rapport le note : « projet fourni vide ») : le bouton « Teil „Intro" erstellen » absent, aucune piste Mélodie en Automation, pas de fader Akkorde/Melodie. Il en va de même pour le Mix de A/b au chapitre 14, sans Accords ni Mélodie.
Ce qui reste réel : un vrai utilisateur peut sauter des chapitres par la même liste, et le chapitre ouvert ne dit pas qu'il lui manque une partie ou une piste. Ce reste devient le défaut D8 du lot tuto (moyen).

**C/b (« aucun signe d'attente » devant « Rien ici pour l'instant ») : le banc, et l'écran est juste.**
`MyExports.kt` l. 162-168 montre bien un sablier et « Chargement de la liste… » tant que la liste est lue (`list == null`). Sur le banc, la lecture est instantanée, donc on ne le voit pas.
La liste était d'ailleurs vraiment vide : aucun partage avant le geste 12 (`lotC/b/journal.txt`, gestes 1 à 11 : accueil, assistant, menu, Partager). U22 compte donc comme corrigé, puisque le vide est dit avec l'endroit des fichiers.

**B/a, chapitre 10 (partie « 인트로 » créée mais l'étape reste à 1/7) : réel, grave.** Ce n'est pas l'ancrage seul. Le siège a joué les chapitres 5 à 9 lui-même (`done=16`).
- Il touche le bouton du tuto « “인트로” 파트 만들기 » (geste 75). L'appli répond « “인트로” 파트를 만들었어요 (그루브 복사) » (écran 77) et montre la partie (« + B · 인트로 », écran 82).
- Pourtant l'étape `make_intro` reste à 1/7 et « 성공 » n'apparaît jamais, même après un second appui (geste 78).
- Le contrôle est `part:Intro:less+nobass` (`TutoChecks.partDone`). La cause exacte est à trouver par le test rouge. Piste : les chapitres 1 à 4 n'ont pas été joués (ancrage au chapitre 5), donc le Groove copié n'est peut-être pas « plus riche » que la copie.
- Dans tous les cas, l'appli crée elle-même une partie qu'elle refuse ensuite, sans un mot.

## Verdicts

Gravité : bloquant = on ne peut pas finir ; grave = on finit mal, ou on croit l'appli cassée ; moyen = gêne, mot faux, coupure.

### Graves (3)

| n° | défaut | sièges (geste/écran) | verdict |
|---|---|---|---|
| G-A | T4/N4 : notes en pointillé à moitié sous le panneau du tuto, sans flèche ; la grille est cachée en entier en ja | A/a 45, 59, 85 ; A/c 35 ; B/a 38 (3e note de la mesure 1) | réel, encore là en fr et ja à 100 % (corrigé en ko au 360 dp, B/a 22) |
| G-B | chapitre 10 : la partie « Intro » faite par le bouton du tuto n'est pas validée, l'étape reste à 1/7 | B/a 75-82 | réel (voir plus haut) |
| G-C | « Simple ou Pro ? » : titre en haut, puis grand vide sombre jusqu'à mi-écran ; on croit que ça charge (retour de Benjamin du 2026-11-14) | B/d 1-2, 25, 34 (même après « attends 5 ») | réel, encore là |

### Moyens (44)

Rangés par lot dans `lots-correction.md` (sigle Dn) ou laissés de côté (sigle Ln) ; le siège et le geste y sont donnés. Ce tableau dit seulement le verdict de ce qui a demandé un examen.

| défaut | siège | verdict |
|---|---|---|
| « Suivant » sur une étape d'écoute la compte faite sans un mot ; le chapitre se termine sans qu'on ait écouté | A/c 20 ; A/d 1-2, 17 ; A/b 30-31 | réel, moyen. C'est la correction de N19 (une écoute passée est comptée) qui marche, mais rien ne le dit. A/d la juge « pas corrigée » : c'est le même constat. |
| R3-EC17 (bloquant au tour 6) : à 200 % la courbe et Início/Fim/Ouvir ne sont jamais entiers ensemble | B/b 18-19 | réel, rétrogradé en moyen : les boutons se touchent après défilement (B/b 23, promesse tenue) |
| « Sua música está vazia » / « Tu canción está vacía » juste après avoir posé Voz 1 | B/b 27-28 ; B/c 41-42 | réel (N14, doublon de R6) |
| « Der ganze Groove spielt. » alors que le Groove est vide | A/d 45-46 | réel (N26) : le message ment, même si le Groove vide vient de l'ancrage |
| fin du parcours : « Étape 1/1 », puis « Presque fini ! 35 étapes » | A/b 48-50 | moitié artefact (35 étapes non faites à cause du saut), moitié réelle (N23 : l'en-tête « Étape 1/1 » de fin) |

### Voulus (3)
- « Pince para ampliar » (B/b 14) : « pince » est l'impératif portugais correct de *pinçar*, ce n'est pas du français.
- Pas de niveau de sortie à choisir en Simple à la fin du tuto (A/b 48) : Simple garde « −14 » sans choix, comme annoncé ; le choix existe en Pro et à l'export (C/b 26).
- « Rien ici pour l'instant » sans attente visible (C/b 13, 15) : la liste était vide et l'état d'attente existe (voir plus haut).

### Artefacts du banc (11), détail dans `lots-correction.md` § « À part : le banc »
1. A/b, chapitres 12.1 à 12.3 impossibles (Montée absente) : ancrage.
2. A/b, chapitre 14, Mix sans Accords ni Mélodie : ancrage.
3. A/d, chapitre 10, pas de bouton « Teil „Intro" erstellen » (le Groove est vide, donc `TutoNeed.Part` n'est pas proposé) : ancrage.
4. A/d, Automation sans piste « Melodie » ni Cutoff : ancrage.
5. A/d, Mixer sans Akkorde ni Melodie (et donc N24 lu ici) : ancrage.
6. A/b, fin à « 35 étapes » restantes : ancrage (la partie « Étape 1/1 » reste réelle, voir N23).
7. B/b : le rejeu du tour 6 s'arrête au geste 4 au lieu de 17 (`lotB/b/journal.txt` l. 23 : « REJEU main rendue : écran 5 différent de l'origine »).
8. B/c : le rejeu s'arrête après le geste 1 au lieu de 7 (`lotB/c/journal.txt` l. 9).
9. Écrans au texte vide : A/a 3 et 102 ; B/d 42, 48 et 55.
10. C/a 27 : image en retard sur le texte.
11. C/b 13 et 15 : pas d'attente visible, lecture instantanée sur le banc.

Doute (B6) : « Chargement de la liste… » des projets reste affiché (B/d 23, 30, 31). C'est peut-être le temps du banc ; à prouver par un test sans banc avant d'en faire un défaut.

## Rejeux des graves

Les graves seulement, à lancer par le chef (le trieur ne lance pas le banc). Chaque rejeu doit dire « oracle non tenu » avant la correction.

| défaut | siège, de → à | gestes | commande |
|---|---|---|---|
| G-A | 1a (fr, phone, 100 %, Simple, `amenee=tuto:house:4`) : de l'écran 1 (Rythme, chapitre 4, étape `ohat_do`) à l'écran 45 (Clavier, étape `bass_do`) | 1 à 44, le dernier : `tuto_notes_left` « Basse · mesure 4/8 » | `scripts/bench/rejeu-nouveau.sh docs/audits/ultra/sieges/tour-7/lotA/a 44 t7-aa44-pointilles-panneau 'absent=dessin [coupé] @ 321,682'` |
| G-B | 2a (ko, phone360, 130 %, Simple, `amenee=tuto:techno:5`) : de l'écran 1 (`bass_hear`) à l'écran 76 (Morceau, étape `make_intro`) | 1 à 75, le dernier : `tuto_need` « “인트로” 파트 만들기 » | `scripts/bench/rejeu-nouveau.sh docs/audits/ultra/sieges/tour-7/lotB/a 75 t7-ba75-intro-refusee 'attendu=성공 다음 단계'` |
| G-C | 2d (fr, phone, `fresh=reel`) : de l'accueil (écran 1) à « Simple ou Pro ? » (écran 2) | 1 : `welcome_learn` « J'apprends » | `scripts/bench/rejeu-nouveau.sh docs/audits/ultra/sieges/tour-7/lotB/d 1 t7-bd01-grand-vide 'absent=sur la pastille en haut. » @ 294,720'` |

Notes sur les oracles :
- G-A et G-C lisent une position, faute de texte qui montre la géométrie. Si le correcteur trouve un oracle plus parlant, il le prend.
- G-A : le dessin coupé à 321,682 est la note en pointillé touchée au geste 45.
- G-C : le texte d'aide posé à y = 720 sous un vide de 650 px.

## Défauts d'avant (tour 6)

**Corrigés (22)**
- Tuto :
  - N8 (A/a 66) ; T2 (A/a 21) ;
  - T6 (A/a 57, B/a 59, B/d 14 : « Tonique » et « 빌드업 » pas encore expliqués) ;
  - N19 : une écoute passée est comptée (A/b 31, A/c 63 ; voir la réserve ci-dessus) ;
  - N12 (A/b 44) ; T9 (A/d 30) ; T13 (B/d 18).
- Remix :
  - R19 (B/a 28) ; R6 (B/b 24, B/c 24) ;
  - R3 à 100 % (B/c 22) ; R16 (B/c 20).
- Morceau et export :
  - U29 (C/a 22, mais le bouton est tout en bas) ; U1 (C/a 33) ;
  - U2 (C/b 26) ; U4, U15 et N7 (C/b 12) ; U3 (C/b 30) ; U5 (C/b 33) ;
  - R14 (C/b 27) ; N15 (C/b 38) ;
  - U22 (C/b 13 : le vide est dit, l'attente est le banc).

**Encore là (24)**
- Tuto :
  - T4 et N4 (A/a 45, A/c 35 ; corrigé en ko, B/a 22) ;
  - T11 (A/c 67 ; corrigé en fr, A/a 35) ;
  - T8 (A/a 34, B/a 26 ; corrigé en ja, A/c 38) ;
  - T3 (A/c 1) ; T16 (A/c 27) ; N21 (A/c 52, A/d 14) ;
  - N25 (A/d 63) ; N26 (A/d 46) ; N23 (A/b 50) ;
  - T15 (B/a 81) ; T7 (B/d 7) ; N13 (B/d 21).
- Remix :
  - R3-EC17 (B/b 19, contournable en défilant, B/b 23) ; U13 (B/b 15) ;
  - R5 (B/b 15, B/c 10) ; N14 (B/b 28) ; R7 (B/c 21).
- Accueil : grand vide de « Simple ou Pro ? » (B/d 2).
- Morceau, Guide et langues : U28 (C/a 51) ; G1 (C/a 11) ; R15 (C/a 28) ; R1 en hi (C/a 65 ; corrigé en pt-BR, B/b 15) ; U27 (A/d 86, 99 : Synth au Mixer, Vocals aux Sons).

**Pas atteints (4)** :
- N20 (A/b, pas vu) ;
- N28 (A/b 101 : la consigne cite la Montée, absente à cause de l'ancrage ; A/d pas vu) ;
- N24 (A/d 99, faders absents à cause de l'ancrage) ;
- R4 (B/c : les poignées hors du pouce l'ont empêché).

## Ce qui reste douteux
- G-B : la cause exacte du refus (`less` ou `nobass`, et le rôle des chapitres 1 à 4 non joués). C'est au test rouge de trancher.
- B6 : la liste des projets qui reste en « Chargement… », peut-être le banc.
- Les oracles de G-A et G-C, qui reposent sur une position.
- « Suivant » sur une écoute : voulu (la décision N19) ou défaut ? Le classement en moyen tient seulement au fait que rien ne le dit à l'écran.
- Les petits défauts de C/a (« ▶ सुनो » invisible, −/+ grisés sans explication, aucune preuve que la piste T a été ajoutée) : leur fichier n'est pas localisé, ils sont laissés de côté.
