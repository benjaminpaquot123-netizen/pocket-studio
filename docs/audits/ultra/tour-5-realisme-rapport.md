# Lot banc « réalisme » (étape 3, suite de l'audit A.17 : R1 à R4 et MT1)

Atelier `/home/user/eq-tour-5-realisme`, branche `eq/tour-5-realisme`, partie de fx-int `f1ff40b`. Auteur : un agent ; relecture neuve à faire avant fusion. Sources : `audit-banc2/realisme-testeur.md` (R1 à R5), `audit-banc2/methode.md` (MT1), `retours-benjamin.md` (recette du 8 novembre).

## En trois lignes (pour Benjamin)

- Le testeur IA ne lit plus le nom caché des icônes : il voit « ⋮ », « ▶ », « (micro) », comme toi ; et un texte qu'on peut toucher sans cadre s'écrit « texte », pas « bouton » (ton « Mes exports qui ressemble à une phrase »).
- Nouveau compte : pour chaque défaut que tu as trouvé sur le téléphone, le banc l'avait-il vu ? Aujourd'hui 22 défauts, 10 cachés au banc ; critère : il doit en montrer au moins 80 %.
- Le reste (temps qui s'arrête pendant que le testeur lit, gestes du téléphone comme quitter l'appli) est écrit dans les consignes et dans le plan.

## R1 (grave) : l'écran en texte écrit ce qui est dessiné

**Preuve rouge** (`95de177`, `rouge: theScreenInTextWritesWhatIsDrawnNotTalkBacksNames`) : `BenchSelfTest` lit quatre vrais écrans de l'appli (bouton de langue de l'accueil, « Plus tard » de l'accueil, ⋮ de la barre du haut en Mode simple, ligne-lien « Mes fichiers » de l'onglet Partager). Avant : `bouton « 🇫🇷   Français » (nom « Choisis ta langue ») @ 458,78` (le premier écart, le test s'arrêtait là) ; les écrans commités du tour 5 montraient déjà `bouton « Plus de choix » @ 543,42` et `bouton « Mes exports : retrouver et renvoyer un fichier »` (14 écrans).

**Correction** (`ScreenText.kt`, `IconShapes.kt`) :
- l'apparence est lue dans la mise en page de Compose, pas dans ce que le nœud dit à TalkBack : le peintre de chaque icône (nom de l'`ImageVector` → signe ou forme : `⋮`, `▶`, `✕`, `(micro)`, `(trois points reliés)`), les marques dessinées connues (`link_chevron` → `›`), et le cadre (fond visible ou contour, sur le nœud ou sur une partie qui couvre au moins un quart de sa surface : la pastille dessinée dans sa cible de 48 dp) ;
- mot de la ligne : `bouton` seulement s'il y a un cadre ; sinon `icône` (une icône qu'on touche, son mot après elle), `texte` (une phrase touchable sans cadre ni icône, comme une phrase qu'on ne touche pas), `image`, `dessin` (un nœud qui n'a qu'un nom TalkBack et rien de reconnaissable) ; `champ`, `case`, `interrupteur`, `onglet`, `curseur`… inchangés (ils se dessinent ainsi) ;
- plus jamais `(nom « … »)`, `« nom »` d'une icône seule, `(icône)` ni `sans nom` ; les noms TalkBack restent dans le journal (`DOIGT`, `under()`), pour un futur siège TalkBack (P4) ;
- gardé : `état « … »` (les mots sont ceux de TalkBack, mais ce qu'ils disent est dessiné : un pas allumé ou vide, « Déplié ») ; le premier essai qui l'avait retiré faisait perdre au testeur l'état des cases du séquenceur (vu sur l'écran Rythme).
- `lit=non` : mêmes icônes (une personne qui ne lit pas les voit), mêmes `▭▭▭` ; le test `aSeatThatDoesNotReadSeesBarsAndNoNames` reste vrai.
- `IconShapes` connaît les 141 icônes Material de l'appli et ses 6 vecteurs maison ; `BenchSelfTest#everyIconOfTheAppHasItsShape` lit les sources et refuse une icône sans forme.

**Vert** : `778c01e` (`vert: theScreenInTextWritesWhatIsDrawnNotTalkBacksNames`, le test inchangé entre rouge et vert) ; garde `a00ea97` (`everyIconOfTheAppHasItsShape`). Paquet `com.pocketstudio.app.human` + `RetoursClearOfControlsTest` : 263 tests, 0 échec, 54 sautés (les `drive` des sièges et les tests réservés au banc en marche).

Écrans vus (extraits, Mode simple, téléphone 392 dp) :

```
=== accueil
bouton « 🇫🇷   Français » ⌄ @ 458,78
bouton (étincelles) « Créer mon premier morceau (guidé) Choisis un style, … » @ 294,447
bouton (tête qui parle) « Extraire la voix d'une chanson … » [coupé] @ 294,1224
=== Mode simple, onglet Partager
icône (dossier ouvert) ✓ « Mon projet 1 Sauvegardé » @ 134,42
bouton « Simple » @ 311,42
bouton ● @ 399,42
bouton ▶ @ 471,42
icône ⋮ @ 543,42
champ « Nom du morceau (et du projet) » contenu « Mon morceau » @ 294,271
icône ✕ @ 522,278
bouton (trois points reliés) «   Partager » @ 294,786
bouton (dossier) « Mes fichiers Retrouver et renvoyer : M4A, WAV, vidéo, .zip » › @ 272,888
icône ↶ « Annuler » [désactivé] @ 95,1059
onglet (trois points reliés) « Partager » [sélectionné] @ 534,1158
=== Pad Live
icône ← @ 51,45
bouton ★ @ 534,135
bouton ▶ @ 550,256
bouton ⋯ « Plus » @ 497,1167
=== menu ⋮ (avant : « bouton « Plus de choix » », « (nom « Mes projets ») », « bouton « Annuler la dernière action » »)
texte « Mes projets » @ 445,630
```

**Tests du banc adaptés** : `HumanPadLivePersonasTest` lisait les étoiles du défi par leur nom TalkBack (« Étoiles : »). Il vérifie maintenant ce qui est dessiné (`★`/`☆` à l'écran en texte) **et**, à part, que le nom TalkBack existe sur l'arbre (rien d'affaibli : les deux vérités séparées). Aucun autre test du banc ne lisait un nom TalkBack ou le mot « bouton » : les 263 tests du paquet passent sans autre changement (les gardes lisent des mots affichés : « mesure N/T », « 0:40 · 120 BPM », « Klaxon »).

**Consignes** : `GABARIT-CONSIGNES-TESTEUR-V2.md`, ligne « écran en texte » de la table (ce que le texte dit et ne dit pas ; demander l'image avant de juger qu'un bouton se trouve ou se reconnaît) ; `AUDIT-ULTRA.md` § 3.1 (l. 68) réécrit.

## R2 à R4 (moyens)

| id | Vérifié | Corrigé ici | Reste |
|---|---|---|---|
| R2 temps de l'appli arrêté pendant que le testeur lit | lu et vu : `HumanTester.kt` l. 284-287, la boucle d'attente du geste ne fait que `Thread.sleep(100)` (aucune horloge avancée) ; **vu** : `BenchSelfTest#theAppsClockStandsStillWhileTheTesterReads` (`55ad386`) : 1,5 s de temps réel laissent l'horloge de l'appli où elle était, `attends 2` l'avance de 2 s (garde verte dès l'écriture : une limite constatée, pas une réparation) | ligne « temps de l'appli » des consignes (≈ 1 s d'appli par geste, `attends N`, `banc?: temps`) | option `lecture=reelle` (avancer l'horloge du temps réel écoulé, plafonné) : touche `HumanTester`, à faire avec sa paire rouge/vert |
| R3 le testeur peut lire le code | lu : les consignes lancent `human-act.py` depuis `/home/user/fx-int` ; rien ne contrôle les lectures | consigne : liste des outils interdits, chaque outil hors `human-act.py` écrit dans `incidents-banc:` | lancer les sièges depuis un dossier sans le dépôt (orchestration, `human-tester.sh`), compter les lectures dans le transcript |
| R4 aucun geste du système | vu : `grep -n "recreate\|moveToState\|orientation" HumanTester.kt` → rien ; gestes de `human-act.py` : `regarde, touche, appui-long, tape, glisse, ecris, retour, attends, fin` (+ `double-touche`) | ligne « système » des consignes (missions non données, jamais conclure « rien n'est perdu quand je quitte ») | gestes `quitte`, `tue`, `tourne` (une mission par tour qui les utilise) : touche le cœur du banc, reporté |

## MT1 (grave, méthode) : les défauts que le banc cache sont comptés

**Preuve rouge** : `bb09927` (`rouge: test_defauts_caches` : le vérificateur n'existe pas, 10 échecs) ; `92d65e4` (`rouge: test_defauts_caches dashboard` : pas de ligne J, « A à I »).

**Correction** :
- `docs/audits/ultra/defauts-caches.tsv` : 22 défauts de l'appli vus sur l'appareil (les 8 essais ❌ ou partiels du 8 novembre, et 14 remarques d'appareil), chacun « vu par le banc : oui / partiel / non », la cause d'un « non », la preuve, le rejeu qui le montre aujourd'hui, réparable ou non, la ligne du plan ;
- `scripts/bench/defauts-caches.py` : écarts (essai ❌ sans ligne, valeur hors liste, caché réparable sans ligne de plan, rejeu introuvable) et taux ;
- `PLAN-RESTE.md` § 5, **critère 9** (seuil ≥ 80 % des défauts visibles au banc montrés par le banc, justifié dans DECISIONS) ; lignes R.8, R.9 (§ 1 bis), A.20, A.21 (§ 1 ter) ;
- `scripts/bench/tableau-de-bord.py` : **ligne J** (même taux, remplie à 80 % sans écart ; l'arrêt de l'audit demande A à J). `tableau-de-bord.py` n'est pas un script du portail : branché ici.

**Vert** : `c094a6c`, `c8bca8a` ; `python3 -I scripts/bench/test_defauts_caches.py` → vert ; `test_ultra_bench.py` → `ultra bench scripts: OK`.

**Chiffres du 8 novembre** : 22 défauts ; à l'essai, 9 vus, 3 en partie, 10 cachés ; 18 visibles au banc (4 hors banc par construction : fichiers et menus du système, goût, page Recette) ; 56 % vus à l'essai, 72 % montrés aujourd'hui (13/18 ; rejeux : B08 pads sur la grille, A.16 ; B14 R1 et B09 R.8, ce lot). Causes des 13 cachés ou partiels : écran en texte 4 (B09, B12, B14, B17), hors mission 4, fichier 2, jugement 2, faux moteur 1.

**À brancher après la fusion du portail** : `selftest.sh` ne lance que `test_ultra_bench.py` ; ajouter `test_defauts_caches.py` à la même étape (ou l'appeler depuis `test_ultra_bench.py`), et `verifie-chantier.py` peut lire `defauts-caches.py` (0 écart) comme critère 9. Ces deux scripts sont ceux du lot « portail » : non touchés ici.

## R.8 (trouvé en faisant MT1) : la flèche « la suite plus bas »

Défaut caché B09 (« l'accueil ne signale pas que la liste continue ») : le banc écrivait « ↓ la suite plus bas » sous chaque liste qui défile (1 235 fois dans les écrans du tour 5), une flèche que l'appli ne dessine pas.

**Rouge** `19d33b8` (`rouge: theScreenInTextSaysWhatIsCutNotAnArrowTheAppDoesNotDraw`) : sur l'accueil, `  ↓ la suite plus bas (glisse vers le haut pour la voir)`. **Vert** `f2a0a9a` : plus de flèche ; une ligne dessinée en partie seulement dit `[coupé]` (`bouton (tête qui parle) « Extraire la voix d'une chanson … » [coupé] @ 294,1224`). Vu aussi sur Clavier (`case « 2 octaves » [coupé, sélectionné]`, rangée en bas d'un panneau qui défile) et Partager (`texte « Rien à entendre … » [coupé]`) : ce sont bien des éléments coupés à l'écran. Effet sur les sièges : quand rien n'est coupé au bord, rien ne dit qu'il y a une suite, comme pour une personne. Paquet `human` : 263 tests, 0 échec. B09 a son rejeu dans `defauts-caches.tsv` (`2c49ddd`).

## Ce qui reste

- R2 `lecture=reelle`, R3 sièges hors du dépôt, R4 gestes du système, R5 (doigt qui tremble) : non faits, raisons au tableau ci-dessus.
- Les fenêtres en vues Android (menus du système, `viewLines`) écrivent encore « bouton » pour toute vue cliquable ; rares (menus déroulants de Compose passent par l'arbre sémantique).
- Une icône dessinée au `Canvas` sans étiquette connue reste « dessin » ; une icône dont le nom manque à la table s'écrit « (forme non décrite) » (garde : `everyIconOfTheAppHasItsShape`).
- Les « état « … » » restent : ce sont des mots de TalkBack, gardés parce qu'ils décrivent un état dessiné.
- Critère 9 : 67 % aujourd'hui, < 80 % ; R.9 (missions « hors mission ») et R.3 (liste « œil neuf ») sont les lignes qui le feront monter.

## Classes à rejouer à la fusion

`BenchSelfTest` (dont les deux nouvelles gardes), `HumanPadLivePersonasTest`, `BenchSeatOptionsTest` (`lit=non`), et tout le paquet `com.pocketstudio.app.human` (l'écran en texte change pour tous les sièges et toutes les gardes qui le lisent) ; `RetoursClearOfControlsTest` ; `python3 -I scripts/bench/test_defauts_caches.py` et `test_ultra_bench.py`.

## Vérifications

Sur la tête `26ba875` (code identique à celle du rapport) :
- `bash scripts/bench/selftest.sh <scratchpad>/lot-realisme` : **169 verts, 0 rouge, 0 en quarantaine, 41 sautés** ; les 5 tests de `BenchSelfTest` verts (dont les 3 nouveaux) ; juge du selftest : 22 écarts.
- `python3 -I scripts/bench/verifie-chantier.py --resume <lot>/selftest-resume.txt` : **22 écarts** (les mêmes qu'avant le lot : lignes « banc » sans preuve sans banc, paires et journal historiques ; aucun nouveau).
- `PS_STALL_MIN=70 bash scripts/bench/gradle-file.sh :app:testDebugUnitTest` (suite complète, une fois, à la fin) : **491 classes, 8 160 tests, 0 échec**, BUILD SUCCESSFUL en 23 min 54 s.
- `python3 -I scripts/bench/test_ultra_bench.py` : `ultra bench scripts: OK` ; `python3 -I scripts/bench/test_defauts_caches.py` : vert ; `defauts-caches.py` : 0 écart, 72 %, critère 9 non rempli (attendu).
- Aucun fichier de `app/src/main` ni des scripts du portail (`fusion-equipes.sh`, `tampon.sh`, `verifie-chantier.py`, `selftest.sh`, `test_ultra_bench.py`) touché ; aucun lien symbolique commité.
