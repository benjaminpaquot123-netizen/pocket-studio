# Relecture neuve du lot banc « réalisme » (R1 à R4, MT1, R.8)
Relu : 52b502be972b3d06a5559cef0ee10d6a5e0fce1e

Relecteur neuf (n'a pas écrit ce code). Base fx-int `f1ff40b`, tête `52b502b`, 17 commits. Lus : le rapport `tour-5-realisme-rapport.md`, `git log`/diff complet, `audit-banc2/realisme-testeur.md`, `audit-banc2/methode.md`, `retours-benjamin.md`, `verdicts.tsv`, rapports de sièges du tour 5 cités par `defauts-caches.tsv`.

## Ce qui a été rejoué

- `bash scripts/bench/gradle-file.sh :app:testDebugUnitTest --tests 'com.pocketstudio.app.human.*' --tests 'com.pocketstudio.app.retours.RetoursClearOfControlsTest' --continue` : BUILD SUCCESSFUL (7 min 12 s), 46 classes, 267 tests (dont 4 d'une sonde temporaire de relecture), 55 sautés, **0 échec, 0 erreur**. `BenchSelfTest` 6 tests (1 sauté : `drive`), `HumanPadLivePersonasTest`, `BenchSeatOptionsTest`, `BenchTour*`, `RetoursClearOfControlsTest` verts.
- Sonde temporaire (non commitée, fichier retiré) : écran en texte **et** image de 13 écrans réels (accueil, accueil défilé, projet Simple / Rythme, Mix Simple, Partager, menu ⋮, Mes projets, projet Pro, Mix Pro, Sons Pro, Pad Live), comparés image par image.
- `python3 -I scripts/bench/test_defauts_caches.py` : vert. `python3 -I scripts/bench/test_ultra_bench.py` : `ultra bench scripts: OK`. `python3 -I scripts/bench/defauts-caches.py` : 0 écart, 13/18 = 72 %, critère 9 non rempli.
- `git merge-tree 52b502b 7e993b8` (tête actuelle de l'atelier portail) : conflits de contenu dans `PLAN-RESTE.md`, `DECISIONS.md`, `banc-fiable-journal.md`.

## Constats

### Bloquant

Aucun.

### Grave

**G1. L'état des pads de Pad Live (joue / en attente / prêt) a disparu de l'écran en texte, alors qu'une personne le voit.** Les pads disent leur état seulement dans leur nom TalkBack (`PadLiveA11y.kt` l. 14-24 : `padDescription` → « Boucle de kick, boucle, joue, pad A1 »), et le dessinent (lumière, anneau de progression, clignotement d'attente : `PadLiveScreen.kt` `.padLight(light, color, playing = …, waiting = …, progress = …)`). R1 retire tout nom TalkBack de la ligne et ne garde que `StateDescription`, que ces pads n'ont pas. Sonde, écran Pad Live après l'amenée `padlive` :
```
bouton @ 77,256
texte « boucle » @ 86,207
dessin @ 100,210
texte « Boucle de kick » @ 77,287
```
Le même pad, quel que soit son état, s'écrit `bouton @ 77,256`. Avant le lot, 22 écrans commités du tour 5 donnaient cet état aux sièges (`grep -rlE "(joue|en attente), pad [A-Z][0-9]" sieges/ --include=screen-*.txt` → 22 ; ex. `bouton « Boucle de kick, boucle, joue, pad A1 »`). Effet : un siège ne voit plus qu'une boucle tourne ni qu'un coup attend la grille (A.16, T5-PADRETRIG), alors que c'est le premier signal visuel de Pad Live (B02, B04, B05 de `defauts-caches.tsv` sont tous sur cet écran) ; il risque de faux défauts (« le pad ne répond pas ») et de nouveaux défauts cachés, l'inverse exact du but de R1 et de MT1. Le rapport dit « état allumé ou vide » gardé (vrai pour les cases du séquenceur, qui ont un `stateDescription`), pas pour ces pads ; aucune garde ne le voit (la vue Live et « Fête » utilisent le même `padDescription`). Correction attendue : lire l'état dessiné (paramètres de `padLight`, ou une marque de lumière testée comme `DRAWN_TAGS`) ou, à défaut, garder dans la ligne la partie « état » du nom quand le nœud la dessine ; une paire rouge/vert sur Pad Live (une boucle lancée s'écrit autrement qu'un pad au repos).

### Moyen

**M1. Les mots de rôle « case » et « interrupteur » restent des rôles TalkBack, pas ce qui est dessiné.** Le rapport dit « `champ`, `case`, `interrupteur`, `onglet`, `curseur`… inchangés (ils se dessinent ainsi) ». Faux sur les écrans vus :
- Partager : `case « 4 »`, `case « 8 » [sélectionné]`, `case « M4A · pour partager » [sélectionné]` : l'image montre des pastilles rondes et des boutons pleins, aucune case à cocher ;
- Rythme : `case « Tous les pads »` : un bouton à contour arrondi ;
- en haut de chaque page du projet : `interrupteur « ? » [non coché]` : un « ? » dans un rond ;
- Mix : `interrupteur (haut-parleur et ondes) [coché, état « activé »]` : un bouton carré avec un haut-parleur et le mot « Son » dessous ; « coché » ne se voit pas.
C'est la même famille que R1 (le texte nomme ce que seul TalkBack entend). Moins grave que les noms d'icônes (le mot reste « touchable »), mais la phrase du rapport et de la consigne testeur (« ils se dessinent ainsi ») est inexacte. À faire : même règle que « bouton » (cadre lu dans la mise en page) pour `Role.Checkbox/Switch/RadioButton` sans case ni piste dessinée, ou au moins corriger la consigne.

**M2. Marques dessinées au `Canvas` perdues dans une ligne.** `DRAWN_TAGS` ne connaît que `link_chevron`. Le chevron ⌄/⌃ du `Disclosure` (« Avancé », `Disclosure.kt` l. 79, Canvas sans étiquette, seul signe dessiné ouvert/fermé) n'apparaît pas ; l'icône de la carte « Pad Live » et de « Défis » de l'accueil non plus (`bouton « Pad Live NOUVEAU … » →`, sans la grille dessinée à gauche) ; le ⚠ de « Rien à entendre… » (icône sans nom hors d'un nœud ligne) non plus. 46 `Canvas(` dans l'appli. Le rapport ne parle que d'une icône Canvas « sans étiquette » écrite « dessin » ; ici elle est muette. Lien direct avec B17 (« Avancé » pas assez voyant).

**M3. Conflit de numéros A.20 / A.21 avec le lot portail, non noté.** L'atelier portail (`7e993b8`) ajoute aussi `A.20`, `A.21`, `A.22` à `PLAN-RESTE.md` § 1 ter, avec un autre contenu : son A.20 attend `top_more` écrit `bouton (icône ⋮)` (ce lot écrit `icône ⋮`), son A.21 couvre « MT1 + MT3 » (MT3, état « corrigé, à confirmer », n'est pas fait ici et disparaîtrait à la résolution). `git merge-tree` : conflits dans `PLAN-RESTE.md`, `DECISIONS.md`, `banc-fiable-journal.md`. Le rapport ne dit rien de ce conflit (`grep -i conflit` → rien). À la fusion : garder le texte de ce lot pour A.20, rétablir MT3 (ligne A.21 du portail ou nouvelle ligne), garder A.22 ; `defauts-caches.tsv` (B12, B14 → `A.20`) suit les numéros retenus.

**M4. Taux MT1 : deux lignes « réparable non » contestables qui sortent du dénominateur.** Calcul de `defauts-caches.py` vérifié à la main : visibles = vu oui (9) + réparables (9) = 18 ; montrés = 9 + B08, B09, B14 (rejeux) + ½ B17 + ½ B21 = 13 → 72 %, conforme au rapport. Mais B10 (icônes de l'accueil dépareillées, « jugement », réparable **non**) alors que B21 (« jugement ») est réparable par R.3 (« œil neuf » sur les images), et B11 (pas de chemin vers la Recette en Simple, « hors-mission », **non**) alors que B18 à B20 (« hors-mission ») sont réparables par R.9 (une mission). La règle (DECISIONS) ne sort que « fichiers et menus du système, goût, H2 » ; ni l'un ni l'autre n'en est clairement. Comptés, le taux est 13/20 = 65 % (au lieu de 72 %). Le critère 9 n'est rempli dans aucun cas, donc rien n'est faussement vert aujourd'hui ; mais le chiffre donné à Benjamin est flatteur. Aussi : la remarque « l'ordre des cartes n'est pas top » (2026-11-06) n'a pas de ligne (goût, hors compte, mais à écrire pour être complet).

### Mineur

- **m1. `HumanPadLivePersonasTest` un peu affaibli**, contrairement à « rien d'affaibli » : avant, une ligne de l'écran contenait « Étoiles : » (le nœud `Stars`) ; après, `screenLines().any { "★" in it || "☆" in it }` est vrai avec n'importe quelle étoile de l'écran (le bouton ★ du haut de Pad Live, `bouton ★ @ 534,135`). Viser la ligne de `padlive_challenge_stars` (`lineAt`), comme `BenchSelfTest`.
- **m2. Texte caché mais présent** : une ligne `[coupé]` garde tous ses mots, même presque invisible. Accueil défilé : `bouton « Défis Écoute un rythme, … » ★ ★ ★ → [coupé] @ 294,1270` alors que l'image ne montre que le bord haut de la carte (≈ 8 px). Existait avant le lot ; « coupé » le signale, mais le siège lit ce qu'une personne ne voit pas. Seuil 0,9 : une carte coupée de moins de 10 % n'est pas marquée.
- **m3. Mots dessinés au Canvas avec un nom TalkBack perdus** : graduations du fader de Mix (`SimpleMix.kt` l. 487 `drawText`, « +4, 0, −12, −∞ ») : la ligne devient `curseur [état « +0,0 dB », à 80 %]`. Peu de cas (`AutomationCard`, `Waveform`).
- **m4. Incohérences de texte** : `DECISIONS.md` (ligne R1) et `PLAN-RESTE.md` A.20 disent que les « état « … » » quittent l'écran en texte ; le code et le rapport les gardent. Le rapport dit 72 % puis « Critère 9 : 67 % aujourd'hui » (§ Ce qui reste), DECISIONS dit 67 % (avant le rejeu B09).
- **m5. Preuves de `defauts-caches.tsv`** : B22 dit « sièges à 130 % » (un seul siège, A a #25, `rapport.md` l. 35) ; B16 cite « rien de visible dans Mix avant d'ouvrir Avancé », qui vient de la fumée `fumee-banc-fiable/lotA/c` et parle de l'export MIDI, pas de la longueur (la vraie preuve, C c écran 27 « seulement « (8 bars) » … Mix dit 16 », suffit). B04 « oui » : le siège E d #11 a vu le transport continuer (« le bouton reste « Arrêter » ») mais décrit « la musique joue en boucle » là où Benjamin entend le son coupé : « oui » défendable, symptôme différent. `test_exists` vérifie qu'un rejeu existe, pas qu'il soit vert ni qu'il montre le défaut (B14 : le rejeu montre « Plus tard » écrit « texte », pas « Mes exports », que l'appli dessine aujourd'hui avec un fond).
- **m6.** `visibleBackground` juge un fond « visible » par son alpha seul (> 0,05), pas par son contraste avec ce qui est derrière ; `brush` non nul = visible. Sans cas faux vu sur les 13 écrans (la `LinkRow` à 55 % se voit bien sur l'image de Partager).

## Points vérifiés et tenus

- **R1, l'essentiel tient** : sur les 13 écrans, les icônes sont écrites par leur forme et correspondent aux images (⋮, ▶, ●, ↶ grisé `[désactivé]`, (dossier ouvert), (étincelles), (casque), (micro), (tête qui parle), (trois points reliés), ›) ; aucun nom TalkBack d'icône ; « bouton » pour les cartes de l'accueil, la langue (contour), « Simple » (contour), les pads du Rythme, « Partager » plein, la `LinkRow` « Mes fichiers » (fond + ›), la carte de projet (contour), « Nouveau projet » (contour) ; « texte » pour les entrées du menu ⋮ et « Plus tard » ; « icône » pour ⋮, ↶ et le lien orange « Partager mon morceau » de Mes projets. Boutons désactivés : `icône ↶ « Annuler » [désactivé]`. `IconShapes` : la garde `everyIconOfTheAppHasItsShape` lit les sources et passe ; Star/StarBorder, PlayArrow/Stop, VolumeUp/VolumeOff distincts : l'état dessiné par l'icône est conservé.
- **Paire rouge/vert honnête** : `95de177` n'ajoute que le test ; `778c01e` ne touche pas `BenchSelfTest.kt` (`git diff 95de177 778c01e -- BenchSelfTest.kt` vide) ; l'ancien `ScreenText` écrivait `(nom « Choisis ta langue »)` sans ⌄ : rouge certain. Même chose pour `19d33b8` / `f2a0a9a` (R.8).
- **R.8 juste** : la flèche « ↓ la suite plus bas » (1 238 fois dans `sieges/tour-5`) n'était pas dessinée ; `[coupé]` correspond aux images (carte « Extraire la voix » coupée en bas de l'accueil ; avertissement « Rien à entendre… » réellement rogné sur Partager, un vrai défaut d'affichage désormais visible ; rangée de pas sous la barre du Guide en Pro). Une liste qui s'arrête pile au bord ne dit plus rien, comme pour une personne. Restent m2 (contenu d'une ligne presque cachée).
- **MT1** : 8 lignes contrôlées contre `retours-benjamin.md` et `verdicts.tsv` : B01 (tel-03, C c #27 « .zip absent de My exports »), B02 (tel-05, T5-Ad-43), B03 (tel-06, C c promesse t5-Cc-2 « la longueur reste 8 mesures »), B04 (tel-08, E d #11), B05 (tel-11, T5-Ed-40), B06 (tel-12, T4-01 « défaut partiel »), B07 (tel-14, T5-Fb-13), B22 (A a #25) : sources et verdicts exacts ; aucun « oui » inventé (réserves en m5). Tous les essais ❌ et le « défaut partiel » ont leur ligne (tel-03, 05, 06, 08, 11, 12, 14, 17). Ligne J de `tableau-de-bord.py` : même calcul, « oui » seulement à ≥ 80 % et 0 écart ; l'arrêt passe à « A à J ».
- **Périmètre** : aucun fichier de `app/src/main` ni `ui-components/src/main` ; aucun script du portail (`fusion-equipes.sh`, `tampon.sh`, `verifie-chantier.py`, `selftest.sh`, `test_ultra_bench.py`) ; `tableau-de-bord.py` n'est pas touché par le portail (pas de conflit). Aucun lien symbolique (modes `100644`/`100755` seulement), aucune chanson ni projet de Benjamin, aucun identifiant de modèle dans les messages.
- **R2 à R4** : seules les consignes changent, ce que le rapport dit ; la garde `theAppsClockStandsStillWhileTheTesterReads` est une limite constatée, verte dès l'écriture, annoncée comme telle.

## Pour la suite (non bloquant pour cette relecture)

Après G1 : rejouer une amenée `padlive` avec une boucle lancée et vérifier que l'écran en texte la distingue d'un pad au repos ; au passage, M1 et M2 (rôles et chevrons) peuvent suivre la même règle « lu dans la mise en page ».

Verdict : 0 bloquant, 1 grave — fusion impossible
