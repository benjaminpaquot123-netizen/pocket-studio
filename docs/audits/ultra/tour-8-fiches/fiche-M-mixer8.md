> CORRECTIONS DU VÉRIFICATEUR (2026-11-18). Elles priment sur le texte dessous.
- Base : `git fetch origin ccr-80af1a1b-mn0wec`, tête f6c6d7c7f au moins. Rapports p8b : `git fetch origin eq/tour-7-controle-p8b` avant tout `git show ad3676b3:…` (ce commit n'est pas dans l'intégration).
- Preuve du vert : cette machine n'a pas de SDK Android, aucun test Gradle n'y tourne (même `:project-model:test`). Pousse ta branche, écris « prêt pour CI : <sha> » au chef ; il lance `gh workflow run ci.yml --ref <branche>`. Vert = job « Build, unit tests, screenshots » vert ET « Captures : 0 différentes » lu dans le résumé (la CI ne rougit jamais sur une capture).
- Captures : la liste des images changées se lit dans `results-summary.json` de l'artefact `screenshots-diff` (champ `golden_file_path`), jamais dans le résumé, qui aplatit les noms (156 noms en double). Tu ne réenregistres que les écrans de ta liste ; une autre image qui change = une ligne au chef.
- Textes : aucune phrase française neuve ou modifiée hors du lot T0c. Si un mot manque, demande au chef (leçon CI 360).
- Tes captures (mixer, Sons) se refont après la fusion de captures8, sur une tête qui la contient.
- AppRoot.kt : seulement `mixerStrips` (l. 4089-4101). Tu fusionnes avant T0b.
- Les noms « Batterie » et « Remix » de `mixerStrips` sont en dur : si tu les changes, c'est un texte, donc une ligne au chef d'abord.

---

# Fiche tour 8 : lot M « mixer8 » (branche `eq/tour-8-mixer8`)

> Chef d'orchestre → <NOM>, rôle CORRECTEUR. Lot tour-8 mixer8 : défauts D7/U27 (noms de pistes), rotation du Mixer, coût de la console. Aucune question à Benjamin : une ligne pour moi.

Rappels :
- Dépôt PUBLIC.
- Jamais `git add -A`, `git add .` ni `git commit -a` : on ajoute par chemin.
- Pas de push sur l'intégration (`ccr-80af1a1b-mn0wec`) ni sur `main`. Pas de PR.
- Aucun identifiant de modèle dans les commits.
- Commits « rouge: » puis « vert: ».
- Code et commits en anglais.

1. Base : `git fetch origin ccr-80af1a1b-mn0wec`, puis la branche neuve `eq/tour-8-mixer8` depuis `FETCH_HEAD`.
2. Lis :
   - `docs/audits/ultra/PLAN-TOUR-8-brouillon.md`, lot M et risques 4 et 5 ;
   - `docs/DECISIONS.md`, la ligne du 2026-11-17 sur D7 (les deux voies déjà essayées) ;
   - les deux fiches de `docs/AIDE-FUSIONS-TESTS.md`.
3. Tes fichiers, et eux seuls :
   - `app/src/main/java/com/pocketstudio/app/mixer/MixerScreen.kt` ;
   - `app/src/main/java/com/pocketstudio/app/mixer/Console.kt` ;
   - `app/src/main/java/com/pocketstudio/app/ui/AppRoot.kt`, seulement `mixerStrips` (l. ~4089) ;
   - la page des Sons qui affiche les noms : à localiser (`library/LibraryScreen.kt` ou `sounds/SoundBrowser.kt`). Envoie une ligne au chef avec le fichier exact avant d'y toucher ;
   - tests sous `app/src/test/java/com/pocketstudio/app/mixer/`, plus `ui/MixerFitStateTest.kt` et `ui/MixerMetersTest.kt` (ils lisent déjà `fitProbeCompositions` et `stripCompositions`).
   - Lus, jamais modifiés : `HumanRemixJourneyTest`, `KeyActionsGestureTest`.
   - Hors liste : rien. En particulier, pas le tuto, pas `remix/CutScreen.kt` (`cutCurveCompositions`, voir garde-fous), pas `synth/`.
4. Rejeu : D7/U27 vient du siège 1d (#86, #99). S'il est classé grave, fais d'abord son rejeu avec `scripts/bench/rejeu-nouveau.sh <dossier du siège 1d> <n> t8-m-noms 'attendu=<le même nom au Mixer et aux Sons>'`, puis `rejeu-defauts.sh t8-m-noms` : « oracle non tenu » avant ta correction. Le chemin du siège est à demander au chef s'il n'est pas dans `docs/audits/ultra/sieges/`.
5. Défauts et phrases d'acceptation :
   - D7/U27 : « en de, deux mains, tablet8 à 130 %, Mode Pro : depuis le Mixer, puis les Sons, je vois les mêmes noms de pistes. » Plus : HumanRemixJourneyTest et KeyActionsGestureTest restent verts sans changement.
     - Ne pas réordonner les pistes.
     - Tu choisis seul la source unique du nom et tu notes une ligne dans `docs/decisions/tour-8-mixer8.md`.
     - Risque 4 du plan : prendre le nom le plus court. Si ça casse encore, D7 passe au tour 9 : une ligne au chef, et tu fais le reste.
   - Rotation (`MixerScreen.kt` l. ~187) : la bascule de `fit` ne doit plus perdre l'état (défilement, fiche de piste ouverte, Effets du master). Elle passe toujours par le même SubcomposeLayout.
     - Test Robolectric : fiche ouverte et colonnes défilées, rotation aller-retour : l'état est gardé.
     - Même test en créant un remix.
     - Risque 5 : plus une ligne de recette pour le téléphone de Benjamin, donnée au chef (pas à Benjamin).
   - Coût : « pendant un glissé de fader, la console n'est composée qu'une fois par image » (aujourd'hui 3 fois).
     - Compteur `stripCompositions` : au plus une composition par image sur 60 images de glissé.
     - La sonde de taille (`fitProbeCompositions`) lit des entrées qui ne dépendent pas des faders.
6. Tests ciblés : `bash scripts/bench/gradle-file.sh :app:testDebugUnitTest --tests '*mixer*' --tests '*MixerFitState*' --tests '*MixerMeters*' --tests '*HumanRemixJourney*' --tests '*KeyActionsGesture*'`. Captures : celles du Mixer (`mixer*`, `tablet8/land_mixer`…), regardées en entier, en Simple et en Pro, à 100 % et 130 %, sur 360 dp. Réenregistrement par la CI : même méthode que la fiche captures8, point 6.
7. Rapport `docs/audits/ultra/tour-8-mixer8-rapport.md`. Puis :
   - dans `corrections.tsv`, ta ligne passe à « tenté » ;
   - `PS_REJEUX_SELFTEST=$(mktemp -d) scripts/bench/rejeu-defauts.sh` ;
   - `pre-vol.sh --avant-relecture tour-8 mixer8` ;
   - relecture neuve en dernier commit ;
   - `pre-vol.sh tour-8 mixer8` doit dire PRÊT.
8. Parallèle : libre à côté de T0a, captures8 et garde-fous (fichiers disjoints). AppRoot.kt est partagé avec T0b, qui pourrait toucher « Je le fais » : M passe avant T0b, ou le chef sérialise.
   Contrôle : siège C-a (de, tablet8, 130 %, Pro, rejeu de 1d).
9. Une ligne : « <NOM> fini lot mixer8 : <branche> <sha>, corrigés N/3, rejeux ajoutés R, demandes : …, laissés de côté : … ; pré-vol PRÊT ; self-test V verts, 0 rouge ; rejeux tous tenus ».

> **Demander au chef.** Un fichier hors de ta liste, un doute sur la phrase d'acceptation, un conflit avec un autre lot, un test qui casse hors de ta zone : une ligne au chef (`send_message`), puis tu continues le reste de ta fiche en attendant sa réponse. Jamais de question à Benjamin, jamais de décision hors de ta fiche.
> **Travailler en silence.** Aucun récit, aucun résumé, aucun texte pour Benjamin dans ta discussion. Tu n'écris qu'au chef, une ligne, à trois moments : « prêt », « demande / bloqué », « fini ». Sorties de commandes filtrées (`tail`, `grep`).
