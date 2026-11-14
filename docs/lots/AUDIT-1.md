# Audit, étage 1 : contrôles automatiques massifs (équipe étage 1, 2026-11-03)

Branche `fx-s1` (worktree `/home/user/fx-s1`), code gelé à l'étiquette `audit-1`. Sans APK, sans push. Référence : `docs/audits/AUDIT.md` (§4 étage 1, §5 gravité). Fiches au format `docs/audits/GABARIT-FICHE-DEFAUT.md`, raccourcies (gravité, reproduire, cause, correction, test).

## Outils ajoutés (réutilisables aux tours suivants)

- **`LayoutScan`** (`app/src/test/java/com/pocketstudio/app/audit/LayoutScan.kt`) : lit la mise en page Compose au lieu de l'œil. Signale : texte coupé (« … », mot perdu sans « … », lignes tronquées, mot coupé en deux lignes « Monté/e », texte qui dépasse un parent qui coupe), textes l'un sur l'autre, texte qui passe sur un bouton voisin, cibles (touche < 48 dp ou visible < 24 × 24 dp, WCAG 2.2 AA 2.5.8), bouton sans nom pour TalkBack, contraste WCAG AA lu sur les pixels (4,5:1 ; 3:1 dès 18 sp ou 14 sp gras ; contrôles désactivés exclus). Tient compte des échelles (`graphicsLayer`) et du droite à gauche.
- **Matrice de captures par paires** (`CaptureMatrixTest.kt`, générateur `Pairwise.kt`, test `PairwiseTest`) : 29 écrans clés × 16 langues (15 + pseudo) × 5 appareils (téléphone 360 dp, téléphone 411 dp, téléphone couché 800 × 360, tablette 8" de Benjamin 600 × 1006, tablette 11" couchée 1280 × 800) × police (100 %, 130 %). Toute paire de valeurs est couverte : **465 cas** au lieu de 4 640 (produit complet). Chaque cas échoue s'il reste un défaut non accepté ; les acceptés sont dans `Allowed` avec leur raison. Images à regarder : `./gradlew :app:testDebugUnitTest --tests '*CaptureMatrix*' -Proborazzi.test.record=true` → `app/build/audit-captures/<appareil>/<langue>_<écran>.png` (jamais commitées).
- **`AccessibilityScanTest`** : les 29 écrans clés, téléphone 411 dp, français : cibles, noms TalkBack, contrastes.
- Les captures i18n existantes (`I18nScreenshotTest` et ses langues, pseudo et pseudo droite à gauche) passent aussi par `LayoutScan`.
- `ScreenshotTest` : crochets `fontScale` (taille de police du téléphone) et `inspect(name)` (appelé une fois l'écran dessiné).

Liste des paires : générée par `CaptureMatrixTest.ALL` (déterministe) ; 93 cas par appareil en moyenne ; chaque écran est vu dans chaque langue au moins une fois, chaque écran sur chaque appareil, chaque langue sur chaque appareil, les deux tailles de police partout.

## Défauts trouvés (étage 1)

Gravité : AUDIT §5. « Corrigé » = correction + test qui échouait avant.

### Grave
Aucun.

### Moyen

**D-1-01 : mots perdus sans « … » (texte sur une ligne qui passe à la ligne et perd la suite)**
- Reproduire : Live › Effets, police 130 % : le titre « L'EFFET » devient « THE » en anglais ; Pad Live en polonais : « uderzenie · 1 » perd « · 1 », « przytrzymaj » disparaît ; Mixeur : « Ce qui joue » / « Automatiser » en allemand, anglais, indonésien, japonais ; Morceau : « × 2 · 2 mes. · joue » perd « joue » dans 12 langues sur téléphone ; plein écran des pads : « Tout couper » en polonais et turc ; ligne du temps « mes. 7.2 / 22 » en espagnol.
- Cause racine (texte / mise en page) : `Text(maxLines = 1)` sans `softWrap = false` ni `overflow` : Compose coupe au dernier mot qui tient, sans rien montrer. 103 occurrences de ce motif dans le code, 9 vues fautives par la matrice.
- Correction (règle « partout », composant partagé) : `FitText` (plus petit, mots entiers, « … » seulement en dernier recours) sur les 9 ; `SectionTitle` du Live, boutons du Mixeur, ligne des parties, bouton « Tout couper », position de la ligne du temps, mot de jeu des pads de Pad Live.
- Test : `CaptureMatrix*` (constat « lines dropped »), échouait avant sur ces cas.

**D-1-02 : Fête et carte des pads live sans `FitText` (reste du lot D2)**
- Reproduire : Pad Live › Fête sur 360 dp : « Boucle de ki… », « Boucle Acid… », « Cri « Go… », « Monté/e » coupé au milieu du mot. Carte des pads live (Morceau) : « Kick 909 », « 2 batt. » qui sortent du bas du pad en hindi, coréen, police 130 %.
- Correction : `FitText` sur 3 lignes (Fête), `FitText` + `shrinkToFit` (nouveau, `ui-components/FitText.kt` : le contenu trop haut est dessiné entier et plus petit) sur la carte des pads live ; le nom d'un pad de Pad Live gagne 4 dp pour sa deuxième ligne quand « Plus » est ouvert.
- Test : `CaptureMatrixTest` (cas `padlive_party`, `live_pads`), nouveau constat « mot coupé en deux lignes ».

**D-1-03 : Rythme, le titre « PAS : … » passe sous « Tous les pads » (et toute `SectionLabel` avec « ? »)**
- Reproduire : Rythme en pseudo-traduction (ou un nom de son long) : le titre déborde sur le bouton voisin, son « ? » sort de la ligne.
- Cause : `WithHelp` donnait toute la ligne au titre, le « ? » était poussé dehors, sur le bouton d'à côté. Composant partagé : toutes les `SectionLabel(help = …)` de l'appli.
- Correction : le titre prend ce qui reste après son « ? » (`Components.kt`, `weight(1f, fill = false)`), le chevron de pli aussi.
- Test : nouveau constat « texte sur un bouton voisin » (`LayoutScan.overButtons`), sur les 8 écrans i18n × 7 langues/pseudo.

**D-1-04 : Morceau, cases des parties sur téléphone couché : la 3e ligne (« 1 mes. », « ensuite ») sort du bas**
- Reproduire : Morceau, téléphone couché, japonais, coréen, chinois, arabe, ou police 130 %.
- Correction : `shrinkToFit` sur le contenu de la case (dessiné entier, plus petit).
- Test : `CaptureMatrixPhoneLandTest` (cas `session`).

**D-1-05 : Live, « ■ Tout » coupé (« ■ Wszys ») et onglets du panneau qui débordent de 2 à 11 dp**
- Cause : `TextAutoSize.StepBased` + `softWrap = false` sans repli : au plus petit, le texte est rogné sans « … ». 9 occurrences.
- Correction (partout) : `WholeWordsAutoSize` + « … » sur les 9 ; « ■ Tout » sur deux lignes.
- Test : `CaptureMatrix*` (« wider than its box »).

**D-1-06 : boutons de la grille du Live sans nom pour TalkBack** (lignes A à G, « ■ Tout »)
- Cause : `launchOnPress` posait l'action sans fusionner les mots de l'intérieur.
- Correction : `semantics(mergeDescendants = true)` (`LivePress.kt`), toutes les cases et lignes du Live d'un coup.
- Test : `AccessibilityScanTest` (`live_grid`, `live_fx`).

**D-1-07 : contraste des mots de jeu de Pad Live (« boucle », « coup », « tenir ») : 2,2 à 3,4:1**
- Correction : voile sombre derrière le mot, texte plein. Test : `AccessibilityScanTest` (`padlive`, `padlive_more`).

**D-1-08 (non corrigé, passe de design dédiée) : contrastes sous WCAG AA**
- Morceau : titre de la partie qui joue sur fond vert (2,9:1) ; dialogue (Annuler/OK, 3,6:1) ; ligne du temps « Suivre », « Boucle » (4,1:1) ; Acid 303 : flèches ↑ ⌒ sur case orange (2,9:1) ; bibliothèque « Charleys fermés » (3,2:1) ; piano roll « 1/8 » (2,4:1) ; plein écran des pads, longueurs sur pad allumé (2,2 à 4,3:1). Accepté dans `Allowed` en attendant, à reprendre en une fois (couleurs du thème).

### Mineur

- **D-1-09** Sons : la puce de la piste ouverte ailleurs (fiche, « + Piste », après Remix) restait hors écran (reste « APK 228 »). Corrigé : la rangée défile jusqu'à elle (seule la rangée, jamais la page). Test `TrackBarRevealTest`.
- **D-1-10** Tuto : la rangée d'outils du piano roll à moitié sous « Partie : A » (reste « APK 228 »). Corrigé : l'amenée à l'écran garde 64 dp au-dessus de la grille. Vérifié sur `tuto_app_bass`.
- **D-1-11** Cibles sous 48 dp : octave −/+ du synthé (40), « Cases − / + » du Live (44), fermer le Défi (40). Corrigé (48 dp). Le reste vu par le scan : touches de clavier musical (exception « essentielle » de WCAG), poignée du panneau (18 dp vus, 48 au toucher).
- **D-1-12** `FitText` rapetissait le coréen pour rien (coupure entre syllabes prise pour un mot coupé). Corrigé (`breaksAnywhere` : hangul).
- **D-1-13** MINEUR 5 « Tuto tous styles » : un triolet au bout d'une boucle de pad raccourcie débordait sur le premier pas du tour suivant. Corrigé : `Hit.within` (seuls ses coups avant la fin), appliqué à chaque envoi au moteur (`pushHitNow`), le coup écrit reste entier. Test `HitWithinTest`.
- **D-1-14** MINEUR 8 « Tuto tous styles » : la 808 du trap ne tapait pas (notes liées). Corrigé : chaque note s'arrête une case avant la suivante, les deux glissés d'octave gardés. Test `Trap808Test`.
- **D-1-15** Le même triolet était écrit entier dans l'export MIDI (le fichier ne jouait pas comme le direct). Corrigé : `MidiFile` passe par `Hit.within`. Test `HitWithinTest.theMidiFileWritesTheTripletAsItIsHeard`.
- Restes acceptés (raisons dans `Allowed`) : noms longs de l'utilisateur raccourcis par « … » ; nom de colonne du Live au plus petit (6 colonnes, police 130 %) ; pads courts de Pad Live avec « Plus » ouvert dans les langues longues ; « comp. » du piano roll en espagnol ; aide d'une ligne du Rythme couchée.

## Tests instables (suite JVM complète × 3, `--max-workers=4`, une à la fois)

- Passe 1 (code gelé `audit-1`) : 3 137 réussis, 0 échec (34 min). Passe 2 (avec les corrections de l'étage 1) : 1 échec. Passe 3 (après fusion de `fx-int`, étages 2 et 3) : 0 échec ; Gradle s'est figé 30 min après le dernier test (blocage interne du travailleur de tests, aucun test en cours), tué, résultats complets lus.
- **Instable : `HumanRemixJourneyTest.aSongsVoiceGoesIntoTheProjectWithTouchesOnly`** (« the waveforms never came », 325 s, 1 passe sur 3). Cause trouvée et reproduite à coup sûr (moteur du banc retiré pendant « Mettre dans mon projet ») : le banc ne prête son moteur qu'à la page Remix ; une chanson entrée dans le projet pendant que le moteur était rendu n'était jamais chargée (le ViewModel ne charge les pistes que pour un moteur présent), donc les ondes n'arrivaient jamais. Pas un défaut de l'appli (sur téléphone le moteur est toujours là). Correction (`HumanTester.engineDone`) : quand la page Remix a le moteur et que les pistes de la chanson du projet manquent, le banc resynchronise le ViewModel comme au démarrage du moteur d'un téléphone (une fois par chanson). Échouait à coup sûr avant (scénario forcé), passe après ; le message d'échec dit maintenant l'état (moteur, chanson, ondes, chargement, page).
- Autres suspects (RemixSongEverywhere, Tuto, bancs CPU) : stables sur les 3 passes et sur 3 relances ciblées.

### Suite de la chasse (2026-11-04)

Deux causes racines, chacune corrigée une fois pour toute sa famille (aucun test mis de côté, aucune assertion affaiblie) :

- **Mémoire des tests trop petite** (cause : plateforme de test). Gradle donne 512 Mo à son travailleur de tests ; la suite JVM y fait tourner des centaines de classes Robolectric : la vieille génération pleine à 99,9 %, des centaines de GC complets, `OutOfMemoryError` (`TutoKickSoundTest`), et des tests qui rampent (`TutoJourneyTest[liquid_dnb Pro]` : 1 130 s au lieu de 15 s) jusqu'à dépasser leurs attentes : c'était « example played: not in time » des cours Pro (riddim, jungle, liquid_dnb, neurofunk, jump_up, speed_garage) et `BugReportFlowTest`. Correction : 2 Go par travailleur (`app/build.gradle.kts`). Attentes de `TutoJourneyTest` (120 s) et `BugReportFlowTest` (60 s) : toujours sur l'état réel, le plafond n'arrête qu'un blocage. Sous charge (6 processus qui mangent le processeur en plus) : les 6 cours Pro en 13 à 56 s, verts.
- **Banc « testeur humain » : « attends 2 » ne laissait que 160 ms réelles au travail de fond** (cause : test qui attend l'horloge au lieu de l'état). `attends N` avance l'horloge du fil principal de N s d'un coup, mais un projet créé, un pack lu, des pistes chargées tournent hors du fil principal, en temps réel. Sur machine chargée, l'écran était lu trop tôt. Reproduit à coup sûr en lançant le paquet `human` seul (2 passes sur 2 rouges) : `HumanLucasBugsTest` (« still in the assistant », « nothing to touch: Aide et premiers pas »), `HumanRemixJourneyTest` (« nothing to touch: the Remix chip »). Correction (`HumanTester.backgroundQuiet`, tous les sièges du banc) : après chaque geste, attendre en temps réel qu'aucun fil des coroutines (Default/IO) ne travaille (une lecture réseau, le modèle qui descend du serveur du banc, compte comme une attente, comme sur un téléphone) ; plafond de 5 s. `HumanRemixJourneyTest` relève les états de la séparation toutes les 5 ms sur son propre fil (un état court n'est plus manqué entre deux gestes). Après : paquet `human` 5 passes sur 5 vertes (dont 2 sous charge), pas plus lent (1 min 56 à 2 min 26, contre 2 min 34 avant).
- `ZeroProjectsTest`, `BenchShareTest.annulerStopsAndClosesAtOnce`, `BackupRulesTest.everyFileUnderFilesDirIsClassified` : déjà réglés par le lot FIX-3 (`docs/lots/FIX-3.md`, une vraie régression pour `BenchShareTest`) ; verts ici, sous charge.
- **« the waveforms never came » (5 min, `HumanRemixJourneyTest`)** : pas revu en 14 passes depuis (4 du test seul, dont 3 sous charge ; 10 du paquet `human`, dont 2 sous charge). Cause non prouvée : la machine virtuelle avait la chanson et ses ondes (`overview=true`), l'écran pas. Le message d'échec dit maintenant le cycle de vie de l'activité, le nombre de nœuds `waveform_3` (ou l'erreur de lecture) et la dernière panne de l'écran, et garde une capture (`app/build/remix-journey-timeout.png`). À relire à la prochaine suite complète.

## Son : banc des 37 styles (2026-11-04)

`scripts/song-bench.sh --before docs/son/banc/apres-son8.json` (37 morceaux de « Partir d'un style », exportés par le code de l'appli, rejoués par le cœur du moteur, passe basse et passe kick ; 80 min sur ce conteneur chargé). Rapport et `bench.json` gardés hors dépôt (`scratchpad/s1/bench37/`), audio jamais commité.

- **Tous les critères bloquants passent, 37 sur 37** : true peak ≤ −1 dBTP (−1,20 à −1,16), sonie à la cible ou plafonnée, perte dans la chaîne sous son plafond, ni NaN, ni écrêtage, ni continu, rien sous 30 Hz, exciteur ≤ +1 LU, chaque Drop au-dessus de la fin de sa Montée (au téléphone aussi en bass music), **le kick ressort du mix complet dans chaque partie où il joue**.
- Médianes stables depuis `apres-son8` (sonie −10,3 LUFS-I, Drop −8,6, PLR 9,1, pente −4,75 dB/oct). Mieux : la basse s'efface sous le kick (B9, médiane −0,1 → −3,2 dB) ; les Drops de bass music plus haut que leur Groove (+2 à +3 LU : dnb, dubstep, gabber, jump up, liquid, neurofunk, riddim) ; Intros plus en retrait (techno, acidcore, Detroit…).
- Montées plates de deep house, Detroit, disco, minimal (+4 à +6 LU avant, −0,1 à +1,3 maintenant) : **attendu**, c'est la transition légère décidée le 2026-10-27 (DECISIONS : filtre qui se ferme, sans souffle ni coupure). Le critère « +2 LU » du banc ne la connaît pas : listé, pas un défaut.
- Listés, connus, inchangés : Drop − Break sous +4 LU (acid house 3,8, gabber 3,2, hard techno 3,3, trance 2,9) ; kick sous 3 dB dans 7 styles (2 dB, le seuil bloquant, toujours tenu) ; basse au téléphone d'industrial techno (B1 −11,5, B2 −15,4 dB, la cible −14, le bloquant −16 tenu) ; régularité de l'attaque de la basse (B6) hors cible dans 30 styles, déjà le cas avant (médiane 20,9 dB à `apres-son8`, 20,4 maintenant).
- **D-1-16 (Moyen, non corrigé, contenu des cours)** : banc `TutoMidsBench` (nouveau, `-Pbench.render`) : le Drop que fait un cours Pro de bass music avec « Montre-moi » seul, contre celui de l'assistant, part du mix entre 500 et 2 000 Hz : **liquid_dnb −8,5 dB, jungle −4,6 dB**, techno −3,2, jump up −1,1 ; dnb +3,1, neurofunk +5,2. C'est le « MOYEN 5 » de la revue « Tuto tous styles » (« jungle 7 dB sous l'assistant dans les médiums »), mesuré pour la première fois par le code de l'appli. Cause probable, à confirmer au début du lot : ce qui joue en médium au Drop (nappe, accords, stabs) manque au morceau du cours. À corriger dans un lot de cours (une étape « nappe » ou « accords » au Drop) ; le banc est le test (échoue au-delà de 3 dB).

## Exports : matrice par paires (2026-11-04)

`scripts/export-matrix.sh` : format (WAV, FLAC, M4A) × fréquence (44,1, 48, 96 kHz) × bits (16, 24, 32) × cible (Spotify −14, Apple −16, club −9, libre −11, « comme le style », aucune) × plafond (−1, −0,5, −0,3 dBTP) sur 5 styles, toute paire couverte : **30 cas sur 30 conformes** (sonie à ±0,5 de la cible sur le fichier encodé, ou le plafond de la chaîne atteint et dit ; crête sous le plafond après encodage, M4A décodé ; ffprobe relit format, fréquence, bits, durée, titre, artiste). **Pistes séparées : 5 styles sur 5**, la somme des pistes égale le mix sans master (résidu −75 à −237 dB, seuil −60). 13 min. Deux cas « aucune cible » (32 bits flottants) passent au-dessus de 0 dBFS (+2,9 et +2,5) : voulu, sans limiteur le flottant garde ce qui dépasse. Allers-retours MIDI et projet : dans la suite JVM (`MidiFileTest`, `ProjectModelTest`, `ProjectCatalogTest`), verts.

## Ce qui reste (étage 1)
- MINEUR 3 (course mode / `follow`) : machine à états, à l'étage 2. MINEUR 7 (étape « pompe » du tuto) : contenu de cours, non fait.
- D-1-16 (médiums des cours liquid et jungle) : lot de cours. D-1-08 (contrastes) : passe de couleurs.
- « Direct = export » et saccades par écran : pas refaits ici (bancs `live-bench.sh` et du téléphone, à l'étage 4 et sur l'émulateur).
- « Sons manquants des fiches » et mots « Scie » / « Triangle » : tranchés (voir Décisions), à faire dans leurs lots.

## Décisions à recopier dans DECISIONS
- 2026-11-04 · Travailleur des tests JVM à 2 Go (`maxHeapSize`, 3 Go pour les bancs). Raison : à 512 Mo la suite complète passait son temps au ramasse-miettes et des tests dépassaient leurs attentes. Retour : la ligne dans `app/build.gradle.kts`.
- 2026-11-04 · Banc « testeur humain » : après chaque geste, attendre en temps réel que les fils des coroutines se reposent (plafond 5 s, lecture réseau exclue). Raison : « attends N » avance l'horloge sans laisser au travail de fond le temps réel qu'il lui faut. Retour : `HumanTester.backgroundQuiet`.
- 2026-11-04 · Sons manquants des fiches de styles (break échantillonné CC0, log drum, orgue M1, stabs, effets rewind/scratch/bande, swing de la batterie seule) : option recommandée retenue, **un lot « sons » après l'Audit**, pas de son nouveau pendant l'Audit (il gèle le code pour le mesurer). Ordre : break échantillonné et log drum d'abord (jungle et amapiano s'en servent à chaque Drop), puis stabs et orgue M1, puis les effets ; chaque son sous licence vérifiée dans `THIRD_PARTY.md`. Règle d'ici là : un cours ne nomme que des sons que l'appli a (le plus proche sinon) ; à vérifier au début de ce lot. Raison : ce sont des ajouts au catalogue, pas des défauts ; les faire pendant l'Audit fausserait ses mesures. Retour : les ajouter dans le lot en cours si Benjamin en réclame un.
- 2026-11-04 · Mots « Scie », « Triangle », « Carré » (sous-titres des sons de l'assistant, `SoundLooks.describe`) : option recommandée retenue, **des mots d'oreille en Mode simple** (« Scie » → « Brillant », « Triangle » → « Doux », « Carré » → « Creux »), les noms de forme d'onde gardés en Pro (ce sont les mots des synthés), dans les 15 langues. Raison : Benjamin et les débutants ne savent pas ce qu'est une scie ; en Pro, ce sont les mots qu'on retrouve partout. Retour : remettre les noms de forme d'onde partout (une table de mots). À faire dans le prochain lot de textes (pas de son touché).
- 2026-11-03 · Cibles tactiles jugées sur deux règles : toucher ≥ 48 dp (Android) et visible ≥ 24 × 24 dp (WCAG 2.2 AA 2.5.8) ; touches d'un clavier musical exemptées (« essentiel »). Raison : Compose élargit déjà le toucher à 48 dp, seul le visible minuscule gêne. Retour : `LayoutScan` (seuils).
- 2026-11-03 · Contrôles d'écran par la mise en page Compose (`LayoutScan`) plutôt que l'ATF de Google. Raison : sans dépendance nouvelle, lit les textes coupés que l'ATF ne voit pas ; mêmes règles (cibles, libellés, contraste). Retour : ajouter `roborazzi-accessibility-check`.
- 2026-11-03 · Matrice de captures par paires (465 cas) dans la suite JVM, défauts acceptés listés avec leur raison (`Allowed`). Retour : supprimer `app/src/test/.../audit/`.
- 2026-11-03 · `Text(maxLines = 1)` qui peut être long → `FitText` ; `TextAutoSize.StepBased` → `WholeWordsAutoSize` + « … ». Raison : aucun mot perdu en silence.
