# Lot « bloquants » : relecture neuve
Relu : ce51e8c905fcf0aa7a129d83535665ad703036b3

Date : 2026-11-10 · relecteur qui n'a pas écrit le code · base fx-int 9d50148 · 8 commits (`b9d4aed` … `ce51e8c`).
Lus : le rapport de l'auteur, `git log` et le diff complet, `tour-5-synthese.md` (lignes 8, 33, 50, 98, 198-200), `Spotlight.kt`, `PartHeader.kt`, `RhythmViewModel.kt` (cycle de `sessionUi.pending`), `RhythmScreen.kt`, `GuideNav.kt`, `TutoValidation.kt`, les 19 étapes `check: swing`, les 14 `bloquants.json`.
Le code n'a pas été modifié. Une sonde temporaire (`ZzRelectureProbeTest`, effacée, jamais commitée) a pris les captures ci-dessous dans le dossier de travail du relecteur ; les mutations ont été annulées fichier par fichier (`git status` propre en dehors des lignes « T engine/third_party »).

## Ce qui a été lancé (file Gradle `scripts/bench/gradle-file.sh`, passages ciblés)

| Passage | Résultat |
|---|---|
| Tête ce51e8c : `Tour5BloquantsTest`, `TutoJourneyTest` (74, tous les styles Pro et Facile), `GuideNavTest`, `GuideJourneyTest`, `GuideAuditTest`, `TutoBigFontTest`, `TutoScreenTest` | 103 verts, 0 rouge (`drive` sauté comme prévu) |
| Mutation 1 : les 3 lignes de `dd31ec4` retirées de `SpotNode.reveal` ; `TutoJourneyTest*techno*` | `theWholeCourse[techno Pro]` **rouge** : « step_cells coupé en haut, sous l'en-tête » ; les 9 autres verts |
| Mutation 2 : `PartHeader.kt` et `AppRoot.kt` remis à 9d50148 ; `simplePartsPickedAfterTheGuidesListenAreEdited` | **rouge** sur « + Nouvelle partie » : « l'en-tête nomme M : Partie : B · Couplet / Elle tourne en boucle… » (C et B passent : le rouge ne tient qu'à « + Nouvelle partie », comme le rapport le dit) |
| Mutation 3 : `techno.json` remis sur `rhythm.pattern` ; `theTutorialsSwingStepShowsTheSwingSetting` | **rouge** (« Ich mach's » ne montre pas le Swing) : le rouge actuel rougit bien sur l'ancien comportement ; ce test n'a pas changé entre `ffa273e` et la tête (vérifié par `git diff ffa273e ce51e8c`) |
| Sonde : en-tête pendant la lecture, avant tout choix (Simple et Pro) | voir constat G1 |

## Constats

### Bloquant

Aucun.

### Grave

**G1. La phrase « … arrive à la prochaine mesure… » s'affiche pendant toute lecture du morceau, nomme la partie suivante du morceau et ment sur le moment (Simple et Pro).**
- Cause : `partHeader(…, waiting = sessionUi.pending)`. Or `pending` ne vaut pas seulement « partie choisie au toucher » : en mode morceau, `stageSongPart()` y met **la prochaine partie du morceau** (`_sessionUi.value = ui.copy(pending = upcoming.scene…)`, `RhythmViewModel.kt` vers la ligne 3929) pendant toute la partie en cours.
- Preuve 1, Mode simple (Lo-fi, « Écouter » du Guide, Clavier, sans rien toucher) : pendant 6 s, en-tête « Partie : A · Intro » et dessous « C · Refrain arrive à la prochaine mesure… » (`SessionUi(pending=2, songIndex=0)`), capture `simple-before-pick.png` regardée. C arrive à la fin de l'Intro, pas à la mesure suivante.
- Preuve 2, Mode Pro (Techno, ▶ en haut, onglet Sons) : 8 s de « Partie : E · Intro 2 / F · Intro 3 arrive à la prochaine mesure… », puis « Partie : F · Intro 3 / A · Intro arrive à la prochaine mesure… » (`songIndex=1, pending=0`), capture `pro-song-playing.png` regardée.
- Conséquences : (a) un renseignement faux sur l'écran le plus vu pendant l'écoute, en Simple comme en Pro ; (b) il remplace la phrase des audits d'avant « Le morceau joue : touche la grille et cette partie reste sous tes doigts, en boucle. » (la sonde la voit revenir seulement quand la partie suivante est la même que l'actuelle) ; (c) couché, la ligne de l'en-tête ne s'affichait que « quand elle a quelque chose de plus à dire » : elle apparaît maintenant pendant toute lecture et prend de la hauteur à l'éditeur ; (d) en Pro sans « suivre le morceau », elle annonce une partie qui ne viendra jamais à l'écran.
- Le rapport (§ 1 « la ligne n'apparaît que pendant l'attente ») et DECISIONS (« jusqu'à cette barre ») décrivent donc mal le comportement. Aucun test ne regarde l'en-tête pendant une lecture sans choix ; le test de A a #27 passe même par hasard si la partie choisie est la suivante du morceau.
- Piste (non appliquée) : ne passer `waiting` que hors mode morceau (`sessionUi.songIndex < 0`), et ajouter une garde « ▶ du morceau, sans choix : la phrase n'apparaît pas, « Le morceau joue… » reste ».

### Moyen

**M1. A a #27 : la phrase reste si le moteur ne bascule jamais.** Elle ne dépend que de `pending` et de `playing` : ■ l'efface (sonde : après un choix puis ■, note « En boucle (⟳ Boucler)… »), un changement de projet remet `SessionUi()` à zéro ; mais un échange jamais fait la laisse jusqu'au ■. Le rapport le signale lui-même (§ 6). À garder en tête pour l'essai téléphone.

**M2. Paire rouge/vert de A a #27 : honnête mais étroite.** Le rouge `b9d4aed` rougit à coup sûr et pour la bonne raison sur « + Nouvelle partie » seulement (mutation 2) ; pour C et B, il dépend du moment du toucher (dit dans le rapport). Rien dans la paire ne protège contre G1.

### Mineur

- **m1. `SpotNode.reveal` (dd31ec4).** Pas de régression trouvée : les 74 parcours de tuto (Pro et Facile), Guide (Nav, Journey, Audit), tuto en police agrandie, et la sonde Swing sur tablette 8″ allemand, téléphone 360 dp à 130 % et tablette 11″ couchée : le réglage Swing entier au-dessus du panneau (trois captures regardées). La mutation prouve que le correctif est nécessaire. Reste non testé à part : cible plus haute que l'écran qui remonte depuis le bas (la 3ᵉ mise en vue ramène maintenant toujours son haut, ce qui va dans le sens de la règle « son haut d'abord » du commentaire), feuille du bas, ancre qui bouge pendant la mise en vue. Le `withFrameNanos` dans le `runCatching` est sans risque (annulation absorbée comme avant).
- **m2. Traductions.** EN « comes in at the next bar », NL « komt erin bij de volgende maat », DE « kommt im nächsten Takt », ZH, JA justes et cohérentes avec « Mesure suivante » de chaque catalogue. HI « अगले बार से शुरू होगा » suit le mot « बार » du catalogue, mais se lit facilement « la prochaine fois » ; KO sans particule après le nom (« %s 다음 마디부터… ») : correct à l'oral, un peu sec. Aucune n'est fausse. (Le sens est faux partout tant que G1 n'est pas réglé.)
- **m3. E a #117, gardes `7915041`.** Elles font bien les gestes du siège (premier morceau, « Créer et écouter », suppression pendant qu'il est ouvert, OK, corbeille, restauration, prise, Retour pendant la prise, Pad Live, réouverture) et regardent la grille comme le siège (Clavier, piste Mélodie, Notes, ligne « Grille des notes »). Faiblesse : la grille est vérifiée « pas 0 notes », pas « autant de notes que la prise » ; elle lit la description de la grille, pas une image. La conclusion est bien écrite « pas reproduit », jamais « corrigé » (rapport § 2 et § 6, DECISIONS, message de commit « garde: … not reproduced »).
- **m4. Repère `rhythm.swing`.** Juste pour les 19 tutos (`check: swing`, les 19 changés, rien d'autre) ; `TutoValidation.SPOTS`, `GuideNav.SPOTS` et le `when` de `GuideNav` à jour ; `AUTHORING.md` à jour. Les 5 étapes Swing sans `levels` (boom_bap, dubstep, jungle, liquid_dnb, lofi) servent aussi en Facile : la carte Swing est rendue en Simple (`GrooveCard(…, simple)`) et les parcours Facile de `TutoJourneyTest` passent.
- **m5. Historique.** `ffa273e` amendé une fois, dit dans le rapport ; le test de la corbeille a encore changé dans `7915041` (ajout de la vérification de la grille), normal pour un « garde: ».

## Contrôles divers

- Thread audio : aucun fichier `engine/` ni `.cpp` touché.
- `verdicts.tsv` : pas touché. Banc (`scripts/bench`) : pas touché.
- Aucune image (`.png`) commitée, aucun lien symbolique, aucun identifiant de modèle dans les commits.
- DECISIONS : honnête sur E a #117 et B c #23 ; la ligne A a #27 décrit mal quand la phrase apparaît (G1).
- Captures regardées par le relecteur : Clavier juste après « + Nouvelle partie » (« Partie : B · Couplet », dessous « M arrive à la prochaine mesure… », rien de coupé : correct dans ce cas) ; Clavier pendant « Écouter » sans choix (G1) ; Sons en Pro pendant ▶ (G1) ; écran Swing du tuto en DE tablette 8″, FR 360 dp 130 %, FR tablette 11″ couchée (correct).

Verdict : 0 bloquant, 1 grave — fusion impossible
