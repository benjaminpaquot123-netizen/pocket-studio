# Tour 9 : audit des scénarios du robot (discussion E)

Code lu : `eq/bentest-robot-2` @ 2c0cab99c (lecture seule). Fichiers : `Scenarios.kt`, `ToutTester.kt`, `Ecoute.kt`, `Feux.kt`, `Robot.kt`, tests `ScenariosTest.kt`, `ScenariosRemiseTest.kt`, `ScenariosVraisEcransTest.kt` ; côté appli `PartSheet.kt`, `LiveScreen.kt`, `LivePadsCard.kt`, `CallbackLoad.h`.

## Règles pour le catalogue

1. Chaque oracle de geste se déduit du modèle de l'appli (la fonction que le bouton appelle), jamais de ce que le robot croit ; un test sur les vrais écrans (`ScenariosVraisEcransTest`) par famille de gestes.
2. Chaque scénario part d'un état connu (projet neuf ou remis) et rend le projet comme il l'a trouvé ; la remise se vérifie (mémoire **et** disque), sinon rouge.
3. Un geste qui ne peut pas être défait = rouge du scénario, jamais « resté » en note.
4. Les noms d'éléments à l'écran viennent d'une seule table partagée avec l'appli (pas de `live_pad_$i` recopié à la main).
5. Pas de seuil de lecture « à moitié » : la lecture doit tenir sur toute la durée écoutée (au plus une lecture manquée sur quatre en début).
6. Une mesure vide (0 image, mémoire inconnue, 0 lecture) est « non mesuré », jamais vert.
7. Chaque compteur a un témoin (une valeur connue fausse qui doit sortir rouge) dans les tests.
8. Les listes d'ids (paquets, ratés) se calculent depuis la table des scénarios, jamais figées en dur.
9. Plus de 5 % de « non mesuré » dans un passage = passage en échec du robot.
10. Un scénario neuf n'entre qu'avec son test « faux studio » **et** son test « vrais écrans ».

## Fautes

### F1. La montée attend une partie de plus (C8)
- `Scenarios.kt` l.1160 : `partie_montee` joue `partie:part_buildup_4:1` et l'oracle commun de `partie()` attend `parties + 1`.
- L'appli : `PartSheet.kt` l.107, `PartIntent.BuildUp -> p.buildUpBefore(index, bars)` pose une courbe sur la partie, il n'ajoute pas de partie.
- Ce qui ment : un rouge à chaque passage alors que l'appli fait juste.
- Preuve que le test la cache : `ScenariosTest.kt` l.148, le faux studio fait `parties++` sur `part_buildup_4` : il recopie l'erreur du robot.

### F2. Les pads du Live ont un nom faux (C11)
- `Scenarios.kt` l.113 `toucherPad(id, "live_pad_$i")`, l.627 `Regex("live_pad_\\d+")`, l.1162 pads 2 à 8.
- L'appli : `LiveScreen.kt` l.1108 `tag = "live_pad_${c.key}_$scene"` (piste puis ligne). `live_pad_$i` n'existe que dans `LivePadsCard.kt` l.207 (carte de la session, pas l'écran Live).
- Ce qui ment : sur l'écran Live, `live_pad_\\d+` (match entier, `InAppDriver.kt` l.122) ne trouve rien → « pas de pad » ou rouge, sans rapport avec l'appli.
- Preuve que le test la cache : `ScenariosTest.kt` l.119 crée `live_pad_$it` dans son faux Live, l.476 attend « pad live_pad_0 : le son sort ». Aucun test « vrais écrans » pour les pads.

### F3. Les effets remplissent les pistes et la piste pleine retombe sur 0 (C10, cause neuve)
- `Scenarios.kt` l.340 `effets()` : `firstOrNull { ... } ?: 0` prend la piste 0 même pleine ; il compte sur `PISTES = 4` alors que `place()` l.580 compte sur `PISTES_MAX = 8`.
- `Scenarios.kt` l.855 `effet()` : retrait par « Annuler » seulement s'il est possible ; sinon une note « resté sur la piste » et pas de rouge. `effets()` et `effetsPlusieurs()` ne retirent jamais leurs effets.
- Ce qui ment : chaque passage laisse des effets ; au bout de quelques scénarios toutes les pistes sont pleines et les suivants rougissent sur la piste 0 pleine. Le rouge vient des restes du robot, pas de l'appli.

### F4. Le morceau grossit d'un scénario à l'autre
- `Scenarios.kt` l.316 `structure()` ajoute un break et ne le retire pas ; l.536 `annulerRetablir()` finit sur « Rétablir », donc garde le break.
- Ce qui ment : les scénarios suivants partent d'un morceau toujours plus long (26 parties vues avec C8) ; les durées, les temps d'export et les pires blocs mesurés ensuite ne sont pas comparables d'un passage à l'autre.

### F5. Remises jamais vérifiées
- `tempo()` l.449 vérifie seulement `après < avant`, pas le retour à `avant` ; `tempoLecture()` l.472 remet le tempo dans un `runCatching` sans lire le résultat ; `curseur()` l.825 remise non vérifiée ; `partie()` vérifie « Annuler » dans la mémoire, pas sur le disque.
- Ce qui ment : un scénario vert peut laisser un tempo ou un curseur faux à tous les suivants.

### F6. La lecture à moitié compte comme lecture (seuil indulgent)
- `playing * 2 >= reads` dans `lecture`, `tempoLecture`, `rotation`, `lectureEcrans`, `tempoExtreme` ; le silence est jugé par « au moins une seconde au-dessus de `SILENCE_DB` » (`ToutTester.kt` l.100).
- Ce qui ment : une lecture qui s'arrête à la moitié, ou un son d'une seule seconde sur dix, sort vert.

### F7. Départ et reprise
- `jouerArreter()` l.974 : un départ raté n'est vu que si `lancerLecture` jette ; aucune lecture de `playing` après chaque départ.
- `fondReprise()` l.998 : `return nonMesure` au milieu de la boucle, après des feux déjà posés : le scénario finit « non mesuré » avec un rouge déjà écrit (ou l'inverse : un rouge écrasé par du gris).

### F8. Le Live écouté 4 s et arrêté par le robot (C2)
- `Scenarios.kt` l.33/385 `liveEcouteMs = 4_000` ; l.386 le robot arrête lui-même le Live ; `rotationLecture` écoute aussi 4 s.
- Ce qui ment : 16 lectures, la moitié suffit (F6) ; un Live qui tombe après 3 s est vert.

### F9. Liste des ratés figée
- `Scenarios.kt` l.1083 `RATES_IDS` : 78 ids écrits en dur, dont `pad_2..8` et `partie_montee` (les deux fautes F1, F2).
- Ce qui ment : un scénario renommé ou ajouté sort du paquet « ratés » sans rien dire.

### F10. « Non mesuré » sans garde
- `resultats()` : « non mesuré » seulement si feu GRIS et fin REUSSI ; aucune limite au nombre de « non mesuré » dans un passage.
- Ce qui ment : un robot qui ne mesure presque rien rend un passage sans rouge.

### F11. Copies plutôt que scénarios
- 33 `STYLES_NEUFS` + 3 du paquet rapide : même geste, même oracle (`ts.style`), seule la donnée change. `pistes` : volume/pan sur 4 pistes, muet 1..3, solo 0, 2, 3, même oracle. Effets : un par `EffectType` (sauf EQ), même oracle.
- Ce n'est pas faux, mais 400 « scénarios » faits ainsi ne testent que quelques gestes.

## Compteurs

### K1. Images lentes : vide = vert (C6)
- `ToutTester.kt` l.319 `images()` : `imagesFin() ?: (0 to 0)` puis `part = 0.0` si `n == 0` → `Feux.imagesLentes(0.0)` = VERT.
- `Robot.kt` l.613-615 et l.661 : seuil fixe `SLOW_FRAME_NS = 16_700_000`, quel que soit le rafraîchissement de l'écran (sur un écran 90/120 Hz, une image de 15 ms est déjà en retard ; sur 60 Hz, une image à 16,8 ms comptée lente ne l'est pas vraiment), pas de témoin.
- Ce qui ment : aucun écouteur posé (fenêtre introuvable) ou 0 image = feu vert.

### K2. Écart entre blocs (C3)
- `CallbackLoad.h` l.33-35 : écart divisé par `lastLasted_`, la durée du bloc **d'avant**. Avec des blocs de taille variable (Oboe peut en changer), un petit bloc suivi d'un grand gonfle le chiffre sans retard réel.
- `Ecoute.kt` l.147 : le bilan garde le max des lectures ; chaque `takeWorstGap()` remet à zéro, donc une autre lecture (l'écran Santé de l'appli) vole le pire écart au robot. `Feux.ecart` : orange > 2, rouge > 4 « blocs » (seuils posés le 2026-11-22 d'après un seul téléphone).

### K3. Mémoire : chiffre sans feu (C7)
- `Robot.kt` l.596-599 : `totalPss / 1024`, total seul (ni natif, ni Java, ni graphique) ; `ToutTester.kt` l.351 : écrit dans le carnet, aucun feu, aucun seuil, mesuré une fois à la fin de `images()`.
- Ce qui ment : une fuite (mémoire qui monte d'un scénario à l'autre) ne se voit jamais.

### K4. Délai du son
- `ToutTester.kt` l.218 `latence()` : médiane de 3 ; si plus de la moitié ratent, « non mesuré ». Correct, mais `Feux.latence` vert jusqu'à 50 ms, rouge au-delà de 70 ms, sans témoin (une boucle à délai connu).

## Le bon dessin pour 400 scénarios

- Un scénario = une ligne de données : `{id, état de départ, geste (élément de la table de l'appli), oracle (fonction du modèle de l'appli appliquée à l'état de départ), remise}`. L'oracle s'écrit `attendu = intent.applyTo(projetAvant)` (comme `PartSheet.kt` l.100), puis on compare au projet lu en mémoire **et** sur le disque.
- Les 400 se génèrent par produit : gestes (~40 réels) × états de départ (projet neuf, long, plein d'effets, en lecture, après rotation…) × écran (portrait, paysage). Ce sont les états qui multiplient, pas les copies de styles.
- Remise par snapshot : on garde le projet avant, on le recharge après ; différence ≠ 0 = rouge du scénario (règle 2), donc plus de restes (F3, F4, F5).
- Paquets et ratés calculés depuis la table (règle 8) ; garde « > 5 % non mesuré » (règle 9).
- Tests : un test unique paramétré sur la table, deux fois : faux studio, puis vrais écrans (Robolectric `AppHarness`), pour que le faux studio ne puisse plus recopier une erreur du robot (F1, F2).
