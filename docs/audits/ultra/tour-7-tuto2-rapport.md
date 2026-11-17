# Tour 7 · lot « tuto2 » : rapport

Branche `eq/tour-7-tuto2`, base : tête de `ccr-80af1a1b-mn0wec` (fusionnée, déjà à jour).

## Fait (4 sur 4 demandés, plus L10)

| Défaut | Phrase d'acceptation (vue de l'utilisateur) | Rouge | Vert |
|---|---|---|---|
| D3 carte du kit | Au Clavier, en 130 %, je vois « Télécharger le kit … » et « Plus tard » sans toucher « Lire la suite ». | 58292036 | cc825600 |
| D4 deux gestes | En Mode Simple (Facile), chaque consigne demande au plus deux gestes ; le reste vient à l'étape suivante. Vrai dans les 36 styles. | 333ef460 | 0a87087f |
| D4 accords | Dans une étape d'accords, « Accords » est déjà allumé : toucher la note du bas pose l'accord. | 333ef460 | e04c85de |
| D4 Pro intact | En Pro, les étapes gardent leur texte et leur longueur d'avant la découpe (basse house, basse drill, « Doux » techno). | 31b6b2f1 | 6dac2e7e |
| N26 / L10 | Sur un Groove vide, la consigne du Drop ne dit plus que « tout le Groove joue » ; « Partie … créée » dit ce qu'il y a dedans. | c17b70b0 | d3d07645 |

Reggaeton et afrobeats Facile : couverts par la découpe D4 (même test, tous les styles).

Traductions : 166 consignes neuves dans les 14 langues (`tuto_autres.json`), reprises des traductions relues ; 102 anciennes clés retirées.

## Laissés de côté

L1 à L9 et L11 (liste de `lots-correction.md` l. 50 à 62) : non traités, plafond du lot.

## À juger par le chef

- Sur le chemin Facile de la basse house, la copie se fait à l'étape `bass_2` par le bouton « Copier » du panneau ; le bouton « Copier » du piano (`tuto_roll_copy`) n'est plus exercé par ce test, car le piano n'est pas à l'écran quand `bass_2` s'ouvre (changement de navigation dans AppRoot.kt, hors de mes fichiers).
- La phrase « tout le Groove y joue » du message de partie reste (clé partagée dans `ultra3z5.json`, hors de mes fichiers) ; elle est maintenant suivie de « Dedans : … ».

## Vérifications

Code testé : `adb0b1d47` (captures `ec4ba6de6`), base `9c65e9b87` fusionnée.

- Tests du modèle : verts. Tests de l'appli : suite complète verte avant la relecture ; après les deux corrections de la relecture (`adb0b1d47`), les 68 fichiers de tests du tuto et les tests ciblés (partie, « Accords ») sont verts.
- Banc : selftest vert ; rejeux 4 sur 5, t4-26 diverge (`record_stop`), incident connu de la base (noté par 05e6a935), sans lien avec le tuto.
- Captures : les 10 captures `tuto_*` touchées par le lot sont réenregistrées et regardées. `verifyRoborazziDebug` signale 47 autres écarts hors tuto, déjà présents sur la base : non réenregistrés (hors de mes fichiers).
- Téléphone 360 dp, Simple et Pro, 100 % et 130 % : la carte du kit (« Télécharger le kit Jump up DnB (2 Mo) » + « Plus tard ») et les boutons sont visibles partout sans « Lire la suite ». En Simple 130 %, la consigne est coupée (« Lire la suite » reste pour elle), la carte reste au-dessus.
- Relecture neuve : points 1 (visibilité) et 4 (« Accords ») corrigés ; point 3 (« Dedans : . ») latent, gardé par un test. Laissés, mineurs : 2 (offre du kit cachée pendant la démo et le Bravo), 5 (`stepsFor` fusionne aussi les étapes de vide/remplissage), 6 (étape basse avec `wanted` vide).

## Relecture neuve finale (diff 9c65e9b87..6bd79c169)

Verdict : ok, aucun grave. Le Pro garde ses étapes à l'identique (rejoué), toutes les consignes sont traduites dans les 14 langues, et les tests vérifient vraiment quelque chose. Cinq points mineurs sont laissés :
1. `TutoCourse.stepsFor` fusionne aussi une étape réservée au Pro dans le Facile (acid_do dans acid_show). Aujourd'hui, cela ne change rien. Plus tard, il faudra ne fusionner que les morceaux `levels == "f"`.
2. Jump up `make_drop2_3` : la Sub du Drop 2 est exigée, mais la consigne ne la nomme pas. Le défaut existait avant ce lot.
3. À la fin du tuto, « Accords » s'éteint, même si l'utilisateur l'avait allumé lui-même.
4. Sur un petit écran en paysage, la carte du kit, « Pourquoi ? » et « Lire la suite » réduisent la zone de texte : à revoir en capture paysage.
5. `PianoRoll` teste deux fois `TutoHints.active`. C'est redondant mais sans effet.
