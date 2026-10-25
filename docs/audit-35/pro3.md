# Testeur Pro 3, vague 3 : architecture de l'information et parcours « morceau complet » (candidat de la PR #47)

Base : `/home/user/ps-audit`, branche `audit` (0aa9bc7, tête de la PR #47 : phases 34 à 39 + lot 1). Lecture du code, des captures Roborazzi (`app/screenshots/nav/`, `tablet/`, `tablet8/`, `scale130/`, regardées une à une, planches réduites dans `scratchpad/audit-35/shots/`) et **écoute comme le téléphone** : `scripts/live-bench.sh` (37 styles × 90 s, `ps_live`), puis `scripts/song-bench.sh --premaster --m4a` (37 exports). Chiffres complets : `docs/audit-35/mesures.md`. Chemins relatifs à `app/src/main/java/com/pocketstudio/app/` sauf mention. J'ai formé mon avis avant de rouvrir `docs/audit-34/pro3.md`.

Gravité : **grave** (empêche de finir ou trompe l'utilisateur), **moyen** (coûte du temps ou de la confiance à chaque fois), **mineur**.

## Diagnostic en une phrase

Le milieu du parcours a enfin été refait (5 espaces Structure / Sons / Mix / Live / Plus, une seule « Structure » avec Ligne du temps et Parties, « Partie » partout, un seul transport, fiche de partie à 2 gestes) : sur téléphone, c'est maintenant un outil qu'on comprend. Ce qui reste est ailleurs : **la première écoute fait attendre le Drop jusqu'à 2 minutes** sur 9 styles, **la tablette de Benjamin n'a aucune capture des 5 espaces** (les captures « A11 » montrent l'ancien écran Morceau), et l'appli ne **dit toujours pas** ce que l'assistant a posé (effets, bus, courbes) ni ce qui manque musicalement.

## Parcours réel (gestes, téléphone 360 dp)

Départ : « Que veux-tu faire ? » (`onboarding/LaunchChoice.kt`, capture `nav/launch.png`).

1. Nouveau morceau › famille › style : 3 gestes ; « Ton morceau est prêt ! » avec Écouter / Changer un son / Modifier le morceau / Partager (`onboarding/SongReady.kt:241-271`). La carte se replie en barre pendant la lecture (`SongReadyBar`, `:149`) : la plainte de Benjamin du 24/10 est réglée.
2. **Écouter : 1 geste, mais depuis le début.** En direct (`ps_live`, `scratchpad/audit-35/live.json`), le premier Drop arrive à **112 s** (acid techno), **114 s** (techno industrielle), **117 s** (techno), **121 s** (minimal, rétro techno), **123 s** (Detroit), 112 s (trance), 103 s (hard techno), 94 s (jungle). Ailleurs : 38 à 77 s en club, 12 à 16 s en rap. Un auditeur sur téléphone juge en 20 à 30 s : sur la famille Techno, il entend 2 minutes d'intro de DJ.
3. Changer un son de la carte : **2 gestes** (tenu par `GestureBudgetTest.changingASoundFromTheReadyCardIsTwoTouches`). Avant : 3-5.
4. Modifier le morceau → Structure (ligne du temps), la même destination partout. Avant : deux vues selon le point de départ. Réglé.
5. Variation, Fill, Break, Montée, Fin, Énergie, Allonger : **2 gestes** chacun depuis la fiche de partie (`GestureBudgetTest.aVariationIsTwoGestures` … `lengtheningIsTwoGestures`). Avant : 9 à 16. Réglé, c'est le plus gros gain de la vague.
6. Automatiser un potard du synthé : **2 gestes** (`automatingAKnobIsTwoGestures`). Avant : 11.
7. Partager : 2 gestes (⤴ › Partager). Inchangé, bon.
8. Remixer un morceau (le séparateur que Benjamin veut mettre en avant, RETOURS 24/10) : Sons › Voix › onglet « Remix » = **3 gestes**, rien sur « Que veux-tu faire ? » (`nav/launch.png` : Continuer, Nouveau morceau, Mes projets, Missions).

Comptage des 15 actions de `docs/plan/clics.md` : ≈ **28 gestes** en médiane (vague 2 : ≈ 70, cible 26). Réserve de méthode : les budgets sont tenus sur la barre du haut et des composants isolés (`ui/GestureBudgetTest.kt:41-84`, « counted on the top of the app »), pas sur l'appli entière avec la barre des espaces.

## Points durs

### A. Ce qu'on entend en premier (le son vu par le parcours)

1. **[grave] « Écouter » joue l'Intro de DJ : le premier Drop arrive après 94 à 123 s sur 9 styles.**
   - Où : `onboarding/SongReady.kt:254` (Écouter = ▶ depuis la mesure 1) ; formes longues `project-model/.../SongShape.kt` (`CLUB_LONG`, « I32 V24 L8 D32… »). Preuve : `ps_live`, colonne « Drop à » de `mesures.md`.
   - Pourquoi : la forme longue est juste pour un DJ, mais la première écoute est un test de 30 s sur un téléphone. C'est aussi pourquoi le banc d'écoute en direct de 90 s **ne contient aucun Drop** de ces 9 styles (voir pro2).
   - Fix : sur la carte « Ton morceau est prêt », un deuxième bouton « ▶ Le Drop » (`playFrom` existe déjà) ; ou lancer la première écoute 8 mesures avant le premier Drop.

2. **[moyen] En direct, l'Intro monte toute seule de 5 à 8 dB pendant 13 à 32 s.**
   - Où : gain adaptatif du direct (`engine/src/main/cpp/core/MasterChain.cpp`, `estimateDrop`) ; preuve `ps_live` : techno −15,5 → −9,7 dBFS court terme entre la 4e et la 20e seconde, dnb −18,4 → −11,4 (19 s), jump up −22,1 → −14,5 (32 s), trap −22,7 → −15,6. L'export, lui, garde un gain fixe.
   - Pourquoi : l'utilisateur entend un fondu d'ouverture que le morceau n'a pas ; s'il exporte, l'Intro est différente de ce qu'il a écouté.
   - Fix : démarrer le direct au gain du dernier export ou d'une estimation hors ligne rapide du Drop (8 mesures), et n'adapter qu'ensuite (voir pro2 § 2).

3. **[moyen] Rien ne dit ce que l'assistant a posé, ni ce qui manque.** Inchangé depuis `docs/guide/diagnostic-jump-up.md` § 2 : 25 effets, 2 bus, 10 courbes et un sidechain posés en silence ; « Prochaine étape » ne détecte ni les Drops identiques, ni la nappe partout, ni les pistes toutes au centre. Le plan « Guide toujours présent » (`docs/PLAN-GUIDE.md`) existe, rien n'est livré dans la PR #47 (`WhatActs.kt` décrit, ne juge pas).
   - Fix : brancher `MelodyMetrics`, `WhatActs` et les métriques du banc (Drop − Break, Drops répétés) sur une carte « Ton morceau, piste par piste » ; chantier G1 du plan Guide.

### B. Architecture et navigation (téléphone)

4. **[réglé] 5 espaces, une Structure, une seule lecture, « Partie » partout.** `ui/Spaces.kt:48-56` ; `tr()` contenant « scène » : **0** (vague 2 : 61) ; « partie » 119. Carte de transport de Rythme retirée (`rhythm.png` : Métronome / Décompte / Swing seulement). Live hors du chemin de création (espace à part, `nav/live.png`).

5. **[moyen] Toujours 4 bandes avant le contenu.** Barre du haut + bandeau tempo/position + « 5/10 · Ajoute une mélodie » + barre d'outils de l'espace (Ligne du temps / Parties / Suivre / ⛶) : la grille de la ligne du temps commence à ≈ 230 dp sur 890 (`nav/structure.png`), soit 26 % de l'écran, et 5 pistes sur 7 sont visibles. En paysage téléphone (`nav/land_structure.png`), **3 pistes** visibles sous 4 bandes (≈ 50 % de la hauteur).
   - Fix : en paysage, replier le bandeau d'étape en puce dans la barre et fusionner « Ligne du temps / Parties » dans la barre d'outils de la grille ; masquer la barre d'outils pendant la lecture.

6. **[mineur] Barres de puces coupées sans indice de défilement.** Piano roll (`piano_roll.png` : « Note 1/16 1/… », « Basse en fantôm… », « Zoom − » coupé), Live (`nav/live.png` : « Ajoute(r à la suite) », « Note(s) », « Pis(te) »), Rythme (« Haut(eur) »), console (`nav/mix.png` : 5e voie « Lea »), puces de pistes de Sons qui démarrent au milieu (« acide » coupé à gauche, `nav/sons_synth.png`).
   - Fix : un dégradé de bord + flèche, ou passer à deux lignes au-delà de 4 puces.

7. **[mineur] À 130 %** (`scale130/fr_nav_structure.png`, `fr_play_strip.png`) : le nom du projet passe sur sa ligne et la position reste lisible (réglé depuis la vague 2) ; restent « Pré-refrai… », la puce « Suivre » réduite à « S » dans la Structure, et la 4e voie de console coupée.

### C. Tablette (Galaxy Tab A11 de Benjamin et 11")

8. **[grave pour la vérification] Aucune capture des 5 espaces à la taille de la tablette de Benjamin.** `tablet8/` (600 × 1006 dp, son A11) ne contient ni Structure, ni Sons, ni Mix, ni Live en tant qu'espaces : `NavScreenshotTest` n'a que les variantes `tablet/nav_` (800 × 1280 dp, 11") et `tablet/land_nav_` (`app/src/test/java/com/pocketstudio/app/nav/NavScreenshotTest.kt:111-128`). Pire : `tablet8/session.png`, `tablet8/timeline.png`, `tablet8/land_timeline.png` et `tablet/timeline.png` montrent **l'ancien écran Morceau** (gros bouton orange « Live : jouer les parties piste par piste », grille des 24 scènes, « Effets live ») parce que `ScreenshotTest.timeline()` appelle `SessionScreen` avec ses valeurs par défaut (`ScreenshotTest.kt:1870-1893`), alors que l'appli montre « Parties » sans Live (`nav/structure_parts.png`). Benjamin a demandé que l'audit couvre sa tablette (RETOURS 24/10) : aujourd'hui aucune image ne montre ce qu'il verra.
   - Fix : `NavSmallTabletScreenshotTest` (`w600dp-h1006dp-tvdpi` et `w1006dp-h600dp-land-tvdpi`, avec le `chrome` de `SmallTabletScreenshotTest`), et retirer ou mettre à jour les captures `session`/`timeline` qui montrent un écran qui n'existe plus.

9. **[moyen] Tablette 11" portrait : un téléphone agrandi, avec 30 à 40 % de vide.** `tablet/nav_structure.png` : textes et pistes à la taille du téléphone sur un écran deux fois plus large, le bas de l'écran vide sur un tiers, puces de parties « A · Int », « B · Re », « C · Dr » tronquées alors qu'il reste 700 dp de large ; `tablet/nav_mix.png` : la console n'occupe que la moitié gauche et coupe la 5e voie (« Lead » → rien), la moitié droite empile Automation et Sortie générale ; `tablet/nav_sons_synth.png` : la bibliothèque de sons n'apparaît qu'en bas.
   - Fix : en largeur ≥ 600 dp, console sur toute la largeur (toutes les voies), parties sur la largeur réelle des clips, pistes à 120 dp de haut.

10. **[moyen] Tablette paysage : écrans mal remplis.** `tablet/land_nav_sons_synth.png` : une bande vide de 80 dp entre les pistes et « Clavier / Notes », aucun choix de son à l'écran, le clavier occupe le bas ; `tablet8/land_synth.png` : le clavier flottant **recouvre la carte « Réglages du son »** (on ne voit que les puces Basse / Lead / Nappe / Pluck) ; `tablet8/land_piano_roll.png` et `tablet/land_piano_roll.png` : barre d'outils sur 30 % de la hauteur, une seule mesure, couloir de force hors écran.
   - Fix : en paysage tablette, Sons en deux colonnes (liste des sons | clavier ou notes), piano roll avec la barre d'outils en rail vertical.

11. **[mineur] Live tablette portrait : deux pads XY à l'écran** (`tablet/nav_live.png` : « Filtre DJ » vide à gauche et le pad 303 « Coupure / Résonance » à droite) ; en paysage téléphone, le pad XY est coupé en bas (`nav/land_live.png`).

12. **[mineur] « La sortie est protégée : le son est retenu de 214748368,0 dB »** dans `tablet/nav_mix.png` et `tablet/land_nav_mix.png`. Le compteur du limiteur part à 0 (`rhythm/RhythmViewModel.kt:5458`, `FloatArray(METERS)`), et `mixer/MixerScreen.kt:273` calcule `-200 * log10(0)` = +∞ → `Int.MAX_VALUE`. Sur le téléphone, la première lecture du compteur corrige en 50 ms, mais un moteur indisponible laisse ce texte, et `export/ShareSheet.kt:463` compte ce 0 comme « limiteur qui force » (gain < 0,5 pendant 2 s) : fausse alerte possible dans « Prêt à partager ? ».
   - Fix : compteur limiteur initialisé à 1 et `g <= 0 → 0 dB` dans `LimiterLine`.

### D. Outils et réglages vus par le parcours

13. **[moyen] Le Remix (séparateur) reste caché** : 3 gestes (Sons › Voix › Remix), absent de « Que veux-tu faire ? », des missions et du Mode simple (`onboarding/LaunchChoice.kt`, aucune occurrence). Demande explicite de Benjamin (RETOURS 24/10).
   - Fix : 4e carte « Remixer un morceau » sur l'écran d'accueil.

14. **[moyen] Une piste = un son.** Benjamin veut « plusieurs sons avec des effets sur une piste, et y revenir » (RETOURS 24/10). `Track.layers` n'est pas dans la PR #47 (`project-model/.../Tracks.kt:17-38` : aucun champ) ; il vit sur la branche `g1b` (545c2f2, 5f0200e). Les leads restent « une couche, un son » (pro1).

15. **[mineur] Le budget de gestes n'est pas tenu sur l'appli entière** (vague 2 n° 12, pareil) : `GestureBudgetTest` compte depuis `TopBar` + `UnderTopBar` (`:66-84`), sans la barre des espaces ni les défilements.

## Le son, vu par l'utilisateur (résumé des mesures, détail dans `mesures.md`)

- **Écoute en direct : 37 styles sur 37 sans alerte** (ni fondu, ni silence, ni clic) sur 90 s. Mais 90 s ne couvrent pas le premier Drop de 9 styles (point 1).
- **Le Drop ne change pas d'un passage à l'autre** : l'empreinte énergie × bandes des 8 premières mesures du Drop 2 corrèle à 0,97 en médiane avec celle du Drop 1 (25 styles sur 37 ≥ 0,95), pour un écart de sonie de +0,0 LU (pro1, pro2) ; `SongShape.kt:328-331` réutilise la même scène.
- **Au haut-parleur du téléphone**, le Drop perd 4,1 LU en médiane (1,2 en jump up à 9,0 en lofi ; hip-hop 5,7, house 4,8, techno 3,7, bass music 3,1 : modèle passe-haut 300 Hz) ; la basse de bass music et la 808 sont les plus touchées (pro2).

## Comparé à la vague 2 (mon Top 5 du 23/10)

| Top 5 vague 2 | Vague 3 | Pourquoi |
|---|---|---|
| 1. Un seul transport, une seule barre | **mieux** | Carte de transport de Rythme et ▶ multiples retirés ; mais 4 bandes empilées restent (point 5). |
| 2. Structure = une vue, Live hors de la tête | **réglé** | Espace Structure (Ligne du temps / Parties), Live = espace à part ; « Modifier le morceau » mène toujours à la Structure. |
| 3. Vocabulaire « Partie » maintenant | **réglé** | 0 « scène » dans les `tr()` (61 en vague 2). |
| 4. « Ton morceau est prêt » qui joue et finit ses gestes | **mieux** | Changer un son en 2, carte qui se replie en lecture ; mais Écouter démarre à la mesure 1 (point 1). |
| 5. Tenir à 360 dp et à 130 %, budget sur l'appli entière | **mieux** | Nom et position lisibles à 130 % ; puces coupées (point 6) ; budget toujours partiel (point 15). |

Autres constats de la vague 2 : 7 onglets → 5 espaces **réglé** ; « Prochaine étape » sur 48 dp partout **pareil** ; 4 entrées de partage **mieux** (⤴ partout) ; conseils datés du fil **mieux**.

**Nouveau dans cette vague** : (a) la première écoute qui fait attendre le Drop 2 minutes ; (b) l'Intro qui monte seule en direct ; (c) la tablette A11 sans capture des espaces, et des captures d'un écran disparu ; (d) le compteur du limiteur absurde ; (e) le Remix toujours enfoui.

**Régressions** : aucune perte de fonction trouvée ; la seule régression de perception est la longueur des formes Techno (choix S2, 5-6 min) qui repousse le Drop au-delà de la première minute d'écoute.

## Top 5 (maintenant)

1. **« ▶ Le Drop » sur la carte d'arrivée** (ou première écoute 8 mesures avant le Drop) : 9 styles font attendre 94 à 123 s.
2. **Captures à la taille de la Galaxy Tab A11** pour les 5 espaces (portrait et paysage), et suppression des captures de l'ancien écran Morceau ; puis remplir la tablette (console pleine largeur, Sons en deux colonnes, clavier qui ne recouvre plus les réglages).
3. **Un guide qui juge** : Drops identiques, nappe partout, pistes au centre, effets posés par l'assistant, dits piste par piste (plan Guide G1).
4. **Moins de bandes** en paysage et sur la Structure (bandeau d'étape en puce, barre d'outils repliée en lecture).
5. **Remix sur l'écran d'accueil**, couches par piste (`Track.layers`) dans le parcours, et le compteur du limiteur corrigé.
