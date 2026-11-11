# Lot « verdicts » (étape 3, lot 6 suite : 6.3, reste de la relecture banc6b) : rapport de l'auteur

Atelier `/home/user/eq-tour-5-verdicts`, branche `eq/tour-5-verdicts`, partie de `92258b19`. Zone : `app/src/test`, `scripts/bench`, `docs`. Rien dans `app/src/main`.

## Verdicts (6.3)
- `Tour5AppProof3Test` (test: da37f314, verts par `gradle-file.sh`) : l'appli et un moteur scripté comme le téléphone, ni `HumanTester` ni `BenchEngine`. Preuves sans banc de T4-16, T4-17, T5-Ab-14, T5-Fc-8 (compteur 1/2 puis 2/2, arrêt à la fin), T5-Fb-73 (B pris à la barre), T4-19, T5-Dc-4, T5-Ac-61, T5-Db-16 (✕ vide le champ, curseur gardé, dans la fenêtre de l'appli), T5-Cc-41b (« Mes exports » dit la taille du fichier), T5-TEL10 (notes gardées après avoir quitté l'écran pendant la prise).
- T4-01 reste « appli » : `@KnownDefect T4-LIVESTOP` (« ■ Tout » laisse tourner le transport) ; essais tel-12 « défaut partiel » et tel-08 ❌.
- Juge durci (rouge bf9dbfd3 / vert 28435ec9) : une classe qui hérite de `HumanTester` n'est jamais une preuve sans banc.
- Pas tranchés ici, sans téléphone nécessaire (tests sans banc à écrire) : T5-Ca-14, T5-Ad-13, T5-Fa-5, T5-Fa-11, T5-Aa-43, T5-Cc-23, T5-Ca-1, T5-Cb-67, T5-Cc-41c, T5-DEMO-VIDE, T5-Bb-58.

## Juge : écarts historiques
- Journal 4.0 (×2), 4.2, 4.3 : « preuve: script scripts/bench/test_ultra_bench.py » avec les cas qui les prouvent.
- Trois paires rouge/vert des 8-9 novembre écrites avec d'autres mots : règle « paire rejouée » (rouge 2cac6138 / vert e7f47614, cas `replayed_pairs`) ; chaque rouge rejoué sur son commit puis sur son vert en clone jetable, XML sous `paires-rejouees/<vert>/` : df794a90 rouge (« 16 pads » coupé non trouvé) → dcc33677 vert ; 8f36fa35 rouge (temps 73 affiché, 41 joué) → 0b1cbbc3 vert ; d503254d rouge (24,5 s, taille, prise) → 87b66934 vert.

## Relecture banc6b
- M1 (test: b5f55df4) : carte « 3 Ko » sans « vide », carte « vide » sans taille ; mutations `TINY_BYTES = 0` et `EMPTY` vide : chacune rougit sa carte.
- M2 (db852f22 / 0c3b2d76) : `negated()` saute les adverbes ; « non / jamais / pas encore rejoué sur », « pas du tout / pas vraiment **corrigé** » refusés ; 29 relectures inchangées.
- M3 (2fe5defa / 70dadbb6) : exception du banc : « SCREEN ERROR » et « GEL » du geste écrits.
- « attends » (9f4ac07b / 05487030) : `waitHeldBy` nomme le fil qui retient (nom, regards, 12 trames, battement), imprimé et dans le message d'échec de `aStuckStandIn…`.

## Juge
- Avant : 22 écarts. Après : **0** (selftest final sur 68461606 : 185 verts, 0 rouge, 41 sautés ; juge lancé par le selftest). 12 lignes de verdicts, 6 paires, 4 lignes du journal fermées ; rien ajouté à une liste d'exceptions.
- `Tour5AppProof3Test#theKeysOfATakeLeftRunning…` d'abord rouge dans le selftest (vert seul à 10,2 s : le modèle de vue effacé fil principal bloqué) : réécrit sur le chemin de l'appli (flush de ON_STOP, notes vérifiées sur le disque), 0,4 s.

## Essais à demander à Benjamin (rien demandé ici)
- tel-20 (T5-PREMIER-ECRAN), § 8 n° 3 (T5-Fc-31), § 8 n° 5 (T4-26) ; à refaire après correction : tel-06 (T5-Cc-27), tel-03 (T5-Cd-68/83, T5-Cc-41a), § 8 n° 4 (T5-Ec-61, T5-Cc-30).

## Reste
- 6.2 (lecteur « revient ? », A d #21) et 6.5 (`cases_vides` : pas de CARTE-CODE dans le dépôt ; écrans « œil neuf ») non faits ; les onze lignes « non-tranché » ci-dessus.
