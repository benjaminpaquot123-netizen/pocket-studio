# Testeur Pro 3, vague 2 : architecture de l'information et parcours « morceau complet » (après la phase 33)

Lecture du code de `/home/user/ps-p22` (branche `ccr-80af1a1b-mn0wec`, phase 33 fusionnée) et de 35 captures (`app/screenshots/` : téléphone, i18n, scale130, tablette, tablette 8", paysage). Rien n'a été lancé. Chemins relatifs à `app/src/main/java/com/pocketstudio/app/`. J'ai formé mon avis avant de rouvrir mon rapport de la vague 1 (`docs/audit-33/pro3.md`).

## Diagnostic en une phrase

La phase 33 a réglé le **bout du parcours** (▶ joue le morceau partout, ⤴ Partager avec check-up, « Ton morceau est prêt », bandeau d'étape) sans toucher au **milieu** : les 7 onglets, les 3 vues d'arrangement, les 3 Live et le vocabulaire scène / partie / section sont exactement ceux de la vague 1 (le diff de la phase ne retire que 6 lignes de `SessionScreen.kt`, 0 de `TimelineScreen.kt` et `LiveScreen.kt`). Et chaque gain a été **ajouté par-dessus** au lieu de remplacer : on a maintenant deux transports à l'écran, deux bandeaux sous la barre et un 7e mot (« Partie ») à côté de « Scène ».

## Ce qui a vraiment changé (vu dans le code, diff `174f77d^..HEAD`)

- `ui/PlayStrip.kt` (nouveau, 344 l.), `ui/UnderTopBar.kt`, `ui/TempoSheet.kt`, `ui/RenameProjectBar.kt` : bandeau tempo / position / « ⟳ Partie » / Solo ; feuille du tempo ; renommer sur place.
- `ui/TopBar.kt` : ⤴ Partager, 4 boutons fixes, le tempo quitte la barre.
- `export/ShareSheet.kt` (nouveau, 466 l.) : « Prêt à partager ? ».
- `onboarding/SongReady.kt`, `onboarding/NextStepStrip.kt` (nouveaux) : carte d'arrivée, bandeau d'étape.
- `rhythm/RhythmViewModel.kt` : ▶ = morceau partout (`togglePlay`, `:5638`), `loopPart`, `playFrom`, `clearSolos`.
- `rhythm/RhythmScreen.kt` : la bascule « Écouter » et la carte des étapes partent ; **le transport propre à Rythme reste** (`:225-280`).
- `session/SessionScreen.kt` : −6 lignes (la bascule « Écouter »). Tout le reste de l'onglet Morceau est identique.
- Pas touché : `AppTab` (7 onglets, `ui/AppRoot.kt:172-179`), `TimelineScreen.kt`, `LiveScreen.kt`, `LivePadsFullScreen.kt`, `Glossary.kt`, catalogues i18n (« Scène » toujours dans `en/common.json:6-36`).
- `docs/DECISIONS.md:411` acte le report du vocabulaire et de l'onglet « Structure » à la phase 36.

## Constats

### A. Architecture et navigation (inchangées)

1. **[bloquant] Le morceau s'édite toujours à 3 endroits, et « Modifier le morceau » mène à deux vues différentes selon d'où on vient.**
   - Où : onglet Morceau, `session/SessionScreen.kt:204-227` (ordre imposé : `LiveEntry`, `TimelineEntry`, `SongCard`, `ScenesCard`, `LiveSection`) ; liste des parties `:324-399` ; grille Scènes `:554-604` ; `session/TimelineScreen.kt` plein écran. Captures `session.png` (2,5 écrans de défilement), `tablet/timeline.png`.
   - Nouveau depuis la phase 33 : le bouton « Modifier le morceau » de « Ton morceau est prêt » ouvre la **ligne du temps plein écran** (`ui/AppRoot.kt:850` : `tab = Morceau; showTimeline = true`), le même libellé dans « Prêt à partager ? » ouvre la **liste** de l'onglet Morceau (`ui/AppRoot.kt:831` : `toStructure = { tab = AppTab.Morceau }`, `export/ShareSheet.kt:356,360`). Même mot, deux écrans : l'utilisateur apprend deux fois.
   - Fix : une seule destination (la ligne du temps, avec la liste en bascule « Liste / Pistes »), et `toStructure` qui y mène aussi. En attendant la phase 36, retirer la grille des 24 scènes de la page (un tiroir « Mes parties ») et faire ouvrir l'onglet Morceau sur la liste sans le bouton Live devant.

2. **[bloquant] Le Live est toujours le premier et le plus gros bouton de l'onglet où l'on finit son morceau, et il existe toujours en 3 exemplaires.**
   - Où : `session/SessionScreen.kt:234-246` (bouton accent plein, 56 dp, « Live : jouer les sections piste par piste »), `:316-321` (« Jouer en direct » = `LivePadsCard` + `LivePad`), `live/LiveScreen.kt` (grille scènes × pistes, en-tête propre `:466-471`), `session/LivePadsFullScreen.kt:239-252` (3e en-tête de transport). Captures `session.png`, `live_grid.png`, `live_pads.png`, `live_pads_full.png`, `live_landscape.png`.
   - Pourquoi : DESIGN.md règle 1 dit « une action principale évidente par écran » ; sur Morceau, c'est la performance, pas la finition. Trois pages nommées « Live », « Pads live », « Plein écran » avec trois barres de transport différentes (▶ vert, « Lancer à », REC ; ▶ + ■ + ● ; ▶ cyan/magenta de la ligne du temps).
   - Fix : sortir `LiveEntry` du haut de la page (au bas, ou dans un menu ⋯ de la barre) dès maintenant ; une seule entrée « Live » qui ouvre `LiveScreen`, dont « Pads » est un onglet (déjà le cas) ; supprimer `LivePadsCard` de Morceau.

3. **[pénible] 7 onglets d'OUTIL, Morceau en 6e position, Outils hors écran en paysage.**
   - Où : `ui/AppRoot.kt:172-179` (ordre Rythme, Acide, Synthé, Mixeur, Audio, Morceau, Outils), `:258` (onglet d'arrivée = Rythme pour un projet ouvert à la main), `:957-958` (sous 58 dp par onglet, les libellés passent à `labelSmall × 0,85` ≈ 9,4 sp sur 360 dp : sous les 14 sp de DESIGN.md règle 3), `:1072-1074` (en paysage, « the rail scrolls » : le 7e onglet est hors écran).
   - Pourquoi : l'onglet central du parcours (Morceau, où l'assistant dépose l'utilisateur) est avant-dernier, à côté d'Outils. Les deux niveaux d'onglets d'Audio (`:1529-1533`) et d'Outils (`:1844-1853`, 7 sous-onglets) sont intacts.
   - Fix : phase 36 (5 espaces) ; d'ici là, mettre Morceau en premier et en faire l'onglet d'arrivée de tout projet qui a un morceau ; libellés d'onglets ≥ 11 sp sans réduction (icône seule sur les 2 onglets secondaires si la place manque).

### B. Densité et doublons (nouveau)

4. **[pénible] Sous la barre du haut, jusqu'à 4 bandes empilées, puis un deuxième transport dans l'écran.** Mesures (dp) :
   - Barre du haut 56 (`ui/TopBar.kt:186`) + bandeau de lecture 48 (`ui/PlayStrip.kt:117`) + bandeau « Prochaine étape » 48 (`onboarding/NextStepStrip.kt:98`, sur 6 onglets sur 7, `:49`) = **152 dp** ; + bandeau Solo 40 (`PlayStrip.kt:299`) = **192 dp** ; + barre de renommage ≈ 70 quand elle est ouverte (`ui/RenameProjectBar.kt`). Portrait 360 × 780 (676 dp utiles hors barre d'état et barre d'onglets de 80) : 22 à 28 % de l'écran avant le contenu.
   - Rythme ajoute sa propre carte de transport : « Jouer / Stop » 56 dp, ↶ ↷, tempo − / + / Taper (`rhythm/RhythmScreen.kt:225-280`, ≈ 140 dp mesurés sur `rhythm.png`). Total **292 à 332 dp avant le premier pad, soit 43 à 49 % de l'écran**. Le commentaire du code le reconnaît (`:244` : « What it plays … is said once, under the top bar ») : le bouton « Jouer » de Rythme ne dit plus ce qu'il joue, il ne sert plus qu'à doubler ▶.
   - Morceau a aussi son « Jouer le morceau / Arrêter » pleine largeur (`session/SessionScreen.kt:348-356`) et un ▶ dans la carte Scènes (`:558-560`) ; la ligne du temps (`TimelineScreen.kt:589-598`) et les deux pages Live ont le leur : **six boutons ▶ dans l'appli**, trois couleurs (cyan/vert, cyan/magenta, vert/rouge).
   - Paysage 780 × 360 (`rhythm_landscape.png`) : barre 56 + bandeau 48 = 104 dp (29 %), le bandeau d'étape est bien retiré (`AppRoot.kt:946`, M7) ; mais la carte de transport de Rythme ajoute 142 dp : **246 dp sur 360 avant la première rangée de pads**, qui est coupée en bas de l'écran.
   - Fix : un seul transport. Retirer la carte de Rythme (garder − / + au bord du tempo du bandeau, et « Taper » dans la feuille, déjà là), le bouton « Jouer le morceau » de Morceau et le ▶ de Scènes. Fusionner barre du haut et bandeau sur téléphone en une barre de 56 dp : `[📁 Nom ▾] [C · Refrain ⟳▾] ↶ ● ▶ ⤴`, et mettre tempo + position dans l'en-tête de Structure (c'est ce que prévoit `plan/clics.md:145`). Le bandeau d'étape devient une puce dans l'en-tête de Structure ou une feuille, pas une bande sur six onglets.

5. **[pénible] À 130 % de texte, le bandeau casse : la position devient « … / me » et le nom du projet disparaît.**
   - Où : `scale130/fr_play_strip.png` ligne 7 (position réduite à « … » et « me », coupée par la pastille Solo), `scale130/fr_top_bar.png` (dossier seul, plus de nom ; `ui/TopBar.kt:119-122` `squeezed`), `play_strip.png` ligne 1 (« Da F… », 4 lettres, `TopBar.kt:150` `PROJECT_MIN = 92.dp`).
   - Pourquoi : la barre a 4 boutons fixes de 48 dp (`:148-149`) + puce + dossier : il ne reste rien pour l'identité du projet ni le nom de la section (visible seulement ≥ 444 dp, `DECISIONS.md:388`). Le bandeau répète le problème avec 3 éléments à largeur fixe.
   - Fix : le nom de la partie a priorité sur le tempo quand la place manque (le tempo tient en 2 lignes « 124 / BPM » ou passe dans la feuille) ; `PositionLabel` ne doit jamais descendre sous ~96 dp ; en paysage, la pastille Solo remplace la puce « ⟳ Partie » plutôt que de rogner la position.

### C. Vocabulaire et cohérence

6. **[pénible] Un 7e mot est arrivé (« Partie ») sans que « Scène » parte : la barre dit « C ▾ » (scène) et, 8 dp dessous, « Couplet » (partie).**
   - Où : `ui/TopBar.kt:230-289` (« Changer de scène », « Scène à modifier », « Nouvelle scène vide », « Scène en cours »), `ui/PlayStrip.kt:228-250` (« ⟳ Partie »), `:166-200` (« Jouer depuis… » liste « 1 · Intro »), `session/SessionScreen.kt:330,366-371,557-601,691-700,813` (« scène » × 15), `onboarding/NextSteps.kt:22-31` (titres « Sections », « Morceau »), `:92-97` (conseils « duplique ta scène dans l'onglet Morceau », « Nomme tes sections »), `audiotracks/AudioTracksScreen.kt:207-229` (« La scène B · Couplet » et « À partir de la partie » dans la même carte), `help/Glossary.kt:12-19` (lexique : « Scène », « Morceau », pas d'entrée « Partie »), `settings/GuideDialog.kt:60` (« Morceau : les scènes, les pads live… »), `projects/ProjectsScreen.kt:264` (« 3 scènes »), `i18n/en/home.json:99,158,172`. Comptage dans les `tr(…)` de l'appli : « scène » 61, « partie » 50, « section » 88.
   - Trois façons de nommer la même partie : « 1 · Intro » (menu Jouer depuis), « A · Intro » (liste Morceau, grille), drapeau « A » (ligne du temps). « Ligne du temps » à l'écran (`SessionScreen.kt:270`) mais « Ligne de temps » dans le lexique (`Glossary.kt:59`).
   - Pourquoi : la DECISIONS M5 reporte à la phase 36 « pour ne le faire qu'une fois ». Mais chaque phase d'ici là ajoute des textes avec les anciens mots (NextSteps, AudioTracks, ShareSheet « ta scène part en boucle » `ShareSheet.kt:354`), et la dette grossit. Un débutant lit aujourd'hui, au même endroit, « Scène C » et « Partie Couplet ».
   - Fix : ne pas attendre : un commit « Partie » (chip, Scènes → « Parties », NextSteps, AudioTracks, Glossary, 3 catalogues), c'est une passe de `tr()` et de JSON, pas une refonte. Une seule forme « A · Intro » partout (le menu Jouer depuis compris).

7. **[détail] Couleurs et composants : le vert n'est plus seulement « ça joue ».**
   - Où : `projects/ProjectsScreen.kt:272` (« Partager mon morceau » vert dans Mes projets, capture `projects.png`), `mixer/MixerScreen.kt:710` (« Partager » de la carte Exporter), `onboarding/NextStepCard.kt:160` (« Partager mon morceau ») : **4 entrées de partage** après l'ajout de ⤴. ▶ cyan→vert en haut, cyan→magenta dans la ligne du temps (`TimelineScreen.kt:597`), vert→rouge sur les pads plein écran.
   - Fix : garder ⤴ et la ligne de Mes projets (en couleur de texte, pas en vert) ; retirer les deux autres.

### D. Retour d'information (bien mieux, deux réserves)

8. **[réglé] Où on est, ce qui joue, solo oublié** : position « Refrain · mes. 14/60 » verte en lecture, grise à l'arrêt, cyan en boucle (`PlayStrip.kt:146-200`), « ⟳ Partie » explicite, bandeau Solo d'un toucher (`:299`), « Sauvegardé » sous le nom. C'est la bonne réponse au constat 9 de la vague 1.
   - Réserve 1 : la position n'a pas de « ? » ni d'entrée de lexique ; « ⟳ Partie » non plus (aucun `WithHelp` dans `ui/*.kt`).
   - Réserve 2 : à l'arrêt, le libellé « Couplet mes. 9/60 » dit **d'où ▶ partira**, pas où on est ; rien ne le distingue visuellement d'une position de lecture figée (même typo, gris). Un préfixe « ▶ dès » ou l'icône ▶ pâle devant le nom lèverait le doute.

### E. Accueil et aide

9. **[mieux, pas réglé] « Ton morceau est prêt » existe, mais le morceau ne joue pas et deux de ses quatre boutons laissent l'utilisateur à mi-chemin.**
   - Où : `onboarding/SongReady.kt:76-83` (actions), `ui/AppRoot.kt:843-858` : « Écouter » est une bascule (pas de lecture automatique à l'ouverture ; `clics.md:25` voulait « le morceau joue déjà ») ; « Changer un son » = `selectTrack` + onglet Synthé (`:847-849`) : on atterrit sur le clavier, la liste des sons est sous le pli « Sons prêts à jouer » (4 puces Basse/Lead/Nappe/Pluck, capture `synth.png`), il reste défil. › famille › tuile ; « Modifier le morceau » ouvre la ligne du temps (constat 1). La carte est un `Dialog` modal (`:109`) : on ne voit pas le morceau défiler derrière.
   - Fix : lancer la lecture à l'ouverture (▶ devient ■, la carte reste), carte en feuille basse non modale au-dessus de Structure ; « Changer un son » ouvre directement une feuille « Changer le son » (tuiles du même rôle avec ▶), pas un onglet.

10. **[mieux] Le bandeau « Prochaine étape » suit enfin l'utilisateur (`NextStepStrip.kt`), mais ses conseils datent de la vague 1** : « duplique ta scène dans l'onglet Morceau », « onglet Audio, touche ● » (`NextSteps.kt:92-97`), et l'étape « Partager » pointe `AppTab.Mixeur` (`:97`) alors que ⤴ est partout (le bandeau appelle bien `share()`, `NextStepStrip.kt:73`, mais la carte dépliée garde l'onglet). Le guide page 3 dit encore « Mixeur : volumes, effets, et l'export » (`GuideDialog.kt:58`). Le lexique est une liste plate de 40 termes (`help_page.png`), sans « Partie » ni « Position ».

11. **[détail] Les gestes de la ligne du temps restent derrière « ? »** (`TimelineScreen.kt:731-747` : appui long sur clip, sur drapeau, deux doigts) ; aucune bulle à la première ouverture. Le menu d'appui long (`:1495-1556`) dit « Changer de scène… », « Insérer une scène après… ».

### F. Vérification

12. **[détail] Le filet de sécurité `GestureBudgetTest` ne couvre que les 6 actions de la phase**, sur une barre synthétique sans onglets (`app/src/test/java/com/pocketstudio/app/ui/GestureBudgetTest.kt:66-78`) ; « Partager = 2 » n'y est pas (testé fonctionnellement dans `ShareSheetTest`, pas en budget). Aucune des 11 actions à 3 gestes et plus n'a de budget.

## Densité, en chiffres (téléphone 360 × 780, portrait ; 780 × 360, paysage)

| Empilement | Portrait | Paysage |
|---|---|---|
| Barre du haut | 56 | 56 |
| Bandeau de lecture | 48 | 48 |
| Bandeau « Prochaine étape » | 48 (6 onglets sur 7) | 0 (retiré, M7) |
| Bandeau Solo | 40 (si solo) | 0 (pastille dans le bandeau) |
| **Sous-total avant le contenu** | **152 à 192 dp (22 à 28 % des 676 dp utiles)** | **104 dp (29 % de 360)** |
| + transport propre à Rythme (`RhythmScreen.kt:225-280`) | ≈ 140 | ≈ 142 |
| **Avant le premier pad sur Rythme** | **292 à 332 dp (43 à 49 %)** | **246 dp (68 %)** |
| + barre d'onglets (bas) | 80 | rail latéral, 7 onglets qui défilent |

## Comptage des gestes : les 15 actions-clés de `docs/plan/clics.md:119-136`

Même méthode que clics.md (1 geste = toucher, défilement, appui long, glisser ; départ : projet ouvert sur Morceau, 6 parties, téléphone portrait). « Vague 1 » = colonne « Aujourd'hui » de clics.md.

| # | Action | Vague 1 | **Vague 2 (phase 33)** | Cible | Chemin vague 2 |
|---|---|---|---|---|---|
| 1 | Écouter tout le morceau | 1-2 | **1** ✓ | 1 | ▶ partout (`RhythmViewModel.kt:5638`) |
| 2 | Jouer depuis une partie | 1-3 | **1 sur Morceau, 2 ailleurs** ✓ | 1 / 2 | position › partie (`PlayStrip.kt:166`) ; sur Morceau +1 défil. dès la 5e partie (Live et ligne du temps au-dessus) |
| 3 | Changer le son de la basse | 3-5 | **3-5** (4 depuis « Changer un son ») | 2 | Synthé › piste › défil. › famille › tuile |
| 4 | Changer le kit | 9 | **9** | 2 | Synthé › défil. › Instruments › (télécharger) › défil. › Kits › kit › « Mettre sur les 16 pads » › remettre Synthé (`instruments/InstrumentPanel.kt:163-182`) |
| 5 | Ouvrir la batterie du refrain | 3 | **3** (2 en lançant la lecture : position › Refrain, qui change la scène affichée si « suivre ») | 1 | puce scène ×2 + onglet |
| 6 | Couper une piste dans une partie | 2 | **2** (3 depuis un autre onglet) | 1 | Morceau › Ligne du temps › clip |
| 7 | Déplacer une partie de 3 places | 2-6 | **2-6** | 1 | ⋮ › Monter ×3 ; ou ligne du temps › appui long + glisser (caché) |
| 8 | Volume d'une piste | 2-4 | **2-4** | 2 | Mixeur › défil. 0-2 › curseur (une carte par voie, `mixer.png`) |
| 9 | Enregistrer une mélodie | 3-4 | **3-4** | 2 | Synthé › ● › ● ; métronome toujours dans Rythme (+4) |
| 10 | Enregistrer la voix | 5-6 | **5-6** | 3 | Audio › + Piste audio › Tout le morceau › Enregistrer › Arrêter et garder |
| 11 | Montée de filtre avant le drop | 11 | **11** | 2 | Mixeur › Automatiser › réglage › piste › Filtre › Morceau › Ligne › 2 touches › retrouver la mesure |
| 12 | Fill en fin de partie | 16 | **16** | 2 | inchangé |
| 13 | Partager | 4 + saisie | **2** ✓ (nom déjà rempli) | 2 | ⤴ › Partager |
| 14 | Exporter en WAV | 4 | **3 la 1re fois, puis 2** ✓ | 3 | ⤴ › WAV › Partager (format retenu) |
| 15 | Ouvrir le Live | 1-2 | **1-2** | 1 | Morceau › Live |
| | **Total (médianes)** | **≈ 74** | **≈ 70 (−6 %)** | **26** | |
| | *Ajouts phase 33* | | Tempo **2** ✓ · Renommer **2 + saisie** ✓ · Solo off **1** ✓ · Boucler la partie **1** ✓ | | |

Parcours « Nouveau projet → Partager » d'un débutant (objectif du plan : < 10 gestes avec un son changé et une fin ajoutée) : Projet (1) › Nouveau projet (2) › déplier la famille (3) › style (4) › Écouter (5) › Changer un son (6) › défil. (7) › famille (8) › tuile (9) › Morceau (10) › défil. (11) › « + A » comme fin (12) › ⤴ (13) › Partager (14) = **14 gestes** (vague 1 : ≈ 17 avec le guide à passer et le partage dans le Mixeur).

## Parcours cible proposé (mis à jour)

Le cap de la vague 1 (5 espaces Sons / Structure / Mix / Live / Plus) reste le bon, et le plan l'a adopté pour la phase 36. Ce qui change avec les acquis de la phase 33 :

**Une seule barre, un seul transport.**
- Téléphone : `[📁 Nom ▾] [C · Refrain ⟳ ▾] ↶ ● ▶ ⤴` sur 56 dp. La puce de partie absorbe « ⟳ Partie » (un appui = la boucle, ▾ = la liste des parties avec « Jouer depuis »). Tempo et position vont dans l'en-tête de Structure, et dans un bandeau **seulement pendant la lecture** (vert, 32 dp, qui disparaît à l'arrêt).
- Supprimer les transports locaux : carte de Rythme, « Jouer le morceau » et ▶ de Scènes dans Morceau, ▶ de la ligne du temps (elle garde ↶ ↷ et ⛶). Le Live garde le sien (page à part).
- Le bandeau « Prochaine étape » devient une puce « 3/8 → » dans l'en-tête de Structure et une feuille ; il n'occupe plus 48 dp sur six onglets.

**Vocabulaire tout de suite, pas en phase 36** : « Partie » (puce, cartes, NextSteps, AudioTracks, lexique, 3 catalogues) ; « Projet » pour Mes projets ; « Morceau » = l'enchaînement. Une forme unique « A · Intro ».

**Arrivée** : Nouveau (1) › style (2) : le morceau **joue**, feuille « Ton morceau est prêt » non modale au-dessus de Structure ; « Changer un son » ouvre la feuille des sons du rôle (tuile = 1 geste, avec ▶) ; « Modifier le morceau » = Structure, toujours la même vue ; « Partager » = ⤴.

**Structure** = ligne du temps + bascule Liste, scènes dans un tiroir, Live hors de la page. Feuille de partie (phase 34) avec Fin / Fill / Variation.

**Parcours visé** : Nouveau (1) › style (2, ça joue) › Changer un son (3) › tuile (4) › Structure (5) › dernière partie › Fin (7) › ⤴ (8) › Partager (9) = **9 gestes**, contre 14 aujourd'hui.

## Comparé à la vague 1

| Top 5 de la vague 1 | Vague 2 | Pourquoi |
|---|---|---|
| 1. Une seule « Structure » au lieu de 3 vues d'arrangement | **pareil** (un peu pire) | `SessionScreen.kt` −6 lignes, `TimelineScreen.kt` intact ; « Modifier le morceau » mène désormais à deux vues selon le point de départ (`AppRoot.kt:831` vs `:850`). |
| 2. Sortir le Live du flux de création | **pareil** | `LiveEntry` toujours en tête de Morceau (`SessionScreen.kt:211,222`), 3 Live, 3 en-têtes de transport. |
| 3. Vocabulaire unifié, onglet « Structure » | **pareil**, dette accrue | Reporté à la phase 36 (`DECISIONS.md:411`) ; « Partie » ajouté sous une puce qui dit encore « Scène » ; NextSteps et AudioTracks écrivent les deux. |
| 4. Chemin persistant + « Ton morceau est prêt » | **réglé** (avec réserves) | `NextStepStrip` sur 6 onglets, `SongReady` après l'assistant. Réserves : pas de lecture auto, « Changer un son » s'arrête à l'onglet Synthé, textes du fil datés, 48 dp de plus partout. |
| 5. Partager en haut avec vérification ; Mixeur en console ; « Ce qui joue » en un seul endroit | **mieux** (1 sur 3 réglé) | ⤴ + « Prêt à partager ? » (solo, muet, vide, court, saturation, LUFS) : exemplaire. Mixeur toujours en pile de cartes (`mixer.png`) ; « Ce qui joue » toujours en 3 endroits (`MixerScreen.kt:257`, `SessionScreen.kt:359,525`). |

Autres constats de la vague 1 : n° 9 (feedback : où suis-je, ça joue quoi, solo) **réglé** ; n° 10 (partage en 3 endroits sans vérification) **réglé** mais 4 entrées maintenant ; n° 8 (pages longues) **pire** sur Rythme (bandes + transport doublé) ; n° 4 (puce de scène tronquée) **pareil** (« C ▾ », nom du projet à 4 lettres, et pire à 130 %) ; n° 7 (Audio / Outils à deux niveaux), n° 6 (console), n° 12-14 **pareil**.

**Nouveau dans cette vague** : (a) le doublon de transport et les 152 à 192 dp de bandes (constat 4) ; (b) le bandeau qui casse à 130 % (constat 5) ; (c) « Modifier le morceau » à deux destinations (constat 1) ; (d) six boutons ▶ ; (e) libellés d'onglets à 9,4 sp sur 360 dp ; (f) la carte d'arrivée modale sans lecture automatique (constat 9) ; (g) le budget de gestes testé seulement sur la barre (constat 12).

## Top 5 (maintenant)

1. **Un seul transport et une seule barre** : retirer la carte de transport de Rythme, « Jouer le morceau » et le ▶ de Scènes, fusionner barre + bandeau sur téléphone (tempo et position dans l'en-tête de Structure), bandeau d'étape en puce : −140 à −190 dp sur chaque écran de création.
2. **Structure = une vue** : la ligne du temps avec la liste en bascule, scènes dans un tiroir, et « Modifier le morceau » qui y mène des deux endroits ; Live hors de la tête de page (même avant la phase 36).
3. **Vocabulaire maintenant** : « Partie » partout (puce, cartes, NextSteps, AudioTracks, lexique, catalogues), une seule forme « A · Intro » ; ne plus livrer de texte avec « scène » ou « section ».
4. **« Ton morceau est prêt » qui joue et qui finit ses gestes** : lecture automatique, feuille non modale, « Changer un son » → feuille des sons (tuile + ▶ = 1 geste), puis les feuilles de partie Fin / Fill de la phase 34 pour tenir les 9 gestes du parcours.
5. **Tenir à 360 dp et à 130 %** : priorité au nom de la partie dans le bandeau, nom du projet jamais sous 8 lettres, libellés d'onglets ≥ 11 sp, et un `GestureBudgetTest` sur l'appli entière (onglets compris) pour les 15 actions, pas seulement la barre.
