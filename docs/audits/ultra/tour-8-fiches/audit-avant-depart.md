# Tour 8 : vérification avant départ (2026-11-18, intégration f6c6d7c7f)

**Ce que j'ai fait tourner** : lecture et grep seulement. Aucun test : pas de SDK ici.

## Bloquant
1. **Aucun test ne tourne sur cette machine.** `./gradlew :project-model:test` → « SDK location not found » ; sans :engine → « Could not find kotlin-stdlib:2.4.21 » (`audit/gradle-pm.log`, `gradle-pm2.log`). La CI ne part pas sur un push de `eq/*` (ci.yml : PR, main, manuel). Correction : chaque fiche dit « le chef lance `gh workflow run ci.yml --ref <branche>` » ; le « En arrivant » d'EN-COURS (BenchSelfTest) et `selftest.sh` demandent une machine avec SDK.
2. **Rapports p8b introuvables.** ad3676b3 n'est pas dans l'intégration (`merge-base --is-ancestor` : non), seulement sur `origin/eq/tour-7-controle-p8b`. Correction : `git fetch origin eq/tour-7-controle-p8b` dans T0a, b, c.
3. **Sièges du contrôle.** `sieges/tour-8/` n'existe pas ; pré-vol exige un siège joué sur la branche pour tout lot du tuto, donc Gradle. Sans machine à SDK, aucun lot du tuto ne passe le pré-vol.

## Grave
4. **T0a, périmètre trop étroit.** La fiche permet PianoRoll l. 1460-1500 et 887-890 ; « Touche ici » est l. 715-798, zoom l. 304. Correction : tout PianoRoll.kt côté tuto.
5. **T0a, phrase d'acceptation.** « Je vois les 6 notes » : La 45 et La 57 = 13 rangées, peut-être impossible à 100 %. Correction : « la note demandée est en vue, le rond est dessus, je la pose d'un toucher », la même dans le plan et pour le siège.
6. **Captures, noms aplatis : tous les lots.** 156 noms en double sur 933 (`screenshot-diff.py` imprime le basename). L'option « tmp: par wanted() » de captures8 contredit son « ne touche pas ScreenshotTest.kt » (l. 79). Correction : entrée manuelle dans ci.yml par le chef, avant les lots ; liste lue dans `results-summary.json`.
7. **CI jamais rouge sur une capture.** Correction : « vert » = job vert ET « 0 différentes » dans le résumé.
8. **captures8, « + Piste ».** `Lot6UiTrackBarMoreTest.kt` (flèche au bord droit, puce dessous) n'est pas dans la liste et cassera ; TrackBar sert aussi dans AppSpaces.kt:116 et SynthScreen. Correction : fichier ajouté, captures voisines annoncées.
9. **captures8 : plan contre fiche.** Le plan dit « captures seulement, seul » ; la fiche ajoute du code et le parallèle. Correction : seul d'abord ; M et T0a refont leurs écrans après.
10. **T0b cite `fx/EffectsPanel.kt`, qui n'existe pas.** Le fichier est `mixer/EffectsBox.kt`.
11. **T0c et les plafonds.** Facile : plafond 409, 409 entrées déjà : un texte allongé dans une langue = rouge. `Tour6TutoMixWordsTest` exige 18 étapes de volume. `aFrenchTextHasOneTranslationAcrossAreas` (I18nCatalogTest l. 439) : une clé, une seule traduction dans tous les fichiers. Correction : règles ajoutées ; aucun texte hors T0c.
12. **Garde-fous A change l'appli.** `tuto-moment-exceptions.txt` est lu par `TutoMoments.kt`. Correction : après T0c, avec un débutant neuf.

## Mineur
13. Rejeu de a-V1 : 0 `cmd-*.txt` de p8b dans toutes les branches. Siège court à faire jouer d'abord ; rouge unitaire en attendant.
14. 10 ou 14 captures : les deux (10 telles quelles + 4 après correction).
15. « A mesure 1/16 » : PlayStrip.kt l. 321, hors tuto, voulu en Mode simple. Ne pas toucher au tour 8 ; une ligne pour le tour 9.
16. Partage : les 60 px viennent peut-être du montage de la capture (ScreenshotTest l. ~1836) : à vérifier avant de toucher ShareSheet.
17. Base : fx-int2 a un commit de docs non poussé (5016a124a) ; les fiches nomment f6c6d7c7f.

## Ordre conseillé (un seul contrôle)
ci.yml (chef) → captures8 seul → paquet M + T0a → T0b → T0c → garde-fous A → contrôle T-a à T-d une fois. Débutant neuf par lot du tuto avant fusion.

## Ce qui manque
Lots K (clavier200), B (banc9), S, N (niveaux, condition du « Prochain départ ») et R du plan : absents de l'ordre de départ de resume.md, sans fiche.

## Ce qui n'est pas vert
Rien n'a été exécuté. Points 5 et 16 sont des lectures de code, pas des mesures.
