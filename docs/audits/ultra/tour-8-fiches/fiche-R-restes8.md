> CORRECTIONS DU RÉDACTEUR (2026-11-18). Elles priment sur le texte dessous.
- Base : `git fetch origin ccr-80af1a1b-mn0wec`, sur une tête qui contient captures8 (R refait des captures de la découpe, du Guide et de la page Plus).
- SDK et Gradle : comme le bloc « CORRECTIONS DU CHEF » de `fiche-captures8.md`, puis `ANDROID_HOME=/opt/android-sdk bash scripts/bench/gradle-file.sh …`. Une dépendance qui tombe au hasard : relance avec `--refresh-dependencies` (8 fois au plus).
- Vert = job « Build, unit tests, screenshots » vert ET « Captures : 0 différentes » ; la liste des images changées se lit dans `results-summary.json` de l'artefact `screenshots-diff` (champ `golden_file_path`). Tu ne réenregistres que les captures de la découpe (`cut*`, `remix*` en « Un bout »), du Guide et de la page Plus ; toute autre image changée = une ligne au chef.
- Textes : aucune phrase française neuve ou modifiée (lot T0c seul). Un libellé manquant (« ▶ सुनो », −/+) qui demande un mot neuf = une ligne au chef, pas d'invention.
- Lignes vérifiées (2026-11-18) : `LaunchedEffect(said)` est à `remix/CutScreen.kt` l. 236 (fonction `curve`, l. ~229-240) ; le `Column` de `guide/GuidePanel.kt` est l. 447 ; l'estimation des rangées de la page Plus n'est plus à `ui/AppSpaces.kt` l. 244 (c'est devenu la liste `PlusEntry`) : elle est l. 404-405 (`PLUS_TILE_CHROME_DP = 58f`, `PLUS_TILE_TEXT_DP = 36f`), utilisée l. ~319 et ~411. Défaut décrit dans `tour-7-guide2-relecture.md` l. 7 : `58 + 36 × fontScale` sous-estime une tuile de deux à trois lignes au-delà de 1,3.
- `remix/CutScreen.kt` est aussi dans garde-fous B (`cutCurveCompositions`, l. 230, proposé « voulu », sans code) : tu ne touches pas à ce compteur ; si garde-fous B change finalement du code, le chef sérialise.

---

# Fiche tour 8 : lot R « restes8 » (branche `eq/tour-8-restes8`)

> Chef d'orchestre → <NOM>, rôle CORRECTEUR. Lot tour-8 restes8 : moyen decoupe2, mineurs guide2, et les défauts du siège 3a une fois localisés. Facultatif. Aucune question à Benjamin : une ligne pour moi.

Rappels :
- Dépôt PUBLIC.
- Jamais `git add -A`, `git add .` ni `git commit -a` : on ajoute par chemin.
- Pas de push sur l'intégration (`ccr-80af1a1b-mn0wec`) ni sur `main`. Pas de PR.
- Aucun identifiant de modèle dans les commits.
- Commits « rouge: » puis « vert: ».
- Code et commits en anglais.

1. Base : `git fetch origin ccr-80af1a1b-mn0wec`, puis la branche neuve `eq/tour-8-restes8` depuis `FETCH_HEAD`.
2. Lis :
   - `docs/audits/ultra/PLAN-TOUR-8-brouillon.md`, lot R (l. ~121-125) ;
   - les relectures decoupe2 et guide2 du tour 7 (`docs/audits/ultra/tour-7-decoupe2-relecture.md`, `tour-7-guide2-relecture.md` l. 7 et 9) ;
   - les deux fiches de `docs/AIDE-FUSIONS-TESTS.md`.
3. Tes fichiers, et eux seuls :
   - `app/src/main/java/com/pocketstudio/app/remix/CutScreen.kt` (l. ~229-240 seulement) ;
   - `app/src/main/java/com/pocketstudio/app/guide/GuidePanel.kt` (l. ~440-450) ;
   - `app/src/main/java/com/pocketstudio/app/ui/AppSpaces.kt` (l. ~300-420, la page Plus) ;
   - tests sous `app/src/test/java/com/pocketstudio/app/remix/`, `guide/` et `ui/` (fichiers neufs, ou ceux qui testent déjà ces trois écrans).
   - Pour les défauts du siège 3a : seulement les fichiers que le lecteur aura trouvés, et seulement après une ligne au chef.
   - Hors liste, jamais : `ui/TrackBar.kt` (captures8 ; appelé par `AppSpaces.kt` l. ~116), `ui/AppRoot.kt` (M), `remix/RemixScreen.kt`, `synth/*` (K, T0a), `tuto/*` et `mixer/*` (T0a/T0b, M), les textes `i18n/*`, `tutos/*`, `guides/*` (T0c).
4. Rejeu : decoupe2 est moyen, pas de rejeu obligatoire ; si le chef le classe grave, `scripts/bench/rejeu-nouveau.sh <siège decoupe2> <n> t8-r-decoupe 'attendu=<la consigne en haut>'` puis « oracle non tenu » avant ta correction.
5. Défauts et phrases d'acceptation (commit « rouge: » puis « vert: » pour chacun) :
   - decoupe2, moyen : `LaunchedEffect(said)` fait descendre la découpe pour un message resté d'avant (il part à la première composition, pas seulement quand un message arrive).
     - « en es, main gauche, phone à 100 %, Mode Simple : j'arrive en « Un trozo » avec un message resté d'avant et je vois la consigne en haut, sans saut. »
     - Test Robolectric : ouvrir « Un bout » avec un message déjà là → défilement à 0 ; un message neuf qui arrive ensuite → la colonne descend jusqu'au message (le but du tour 7, lot 2 D3, reste tenu).
   - guide2, mineur : `Column` vide de `GuidePanel.kt` l. 447, toujours émise ; le `spacedBy(6.dp)` du parent ajoute 6 dp de trop. Correctif proposé par la relecture : ne l'émettre que si `step != null || canAlone || named != null`.
     - « en fr, deux mains, phone à 130 %, Mode Simple : depuis le Guide sans étape en cours, je ne vois pas de blanc entre le titre et la suite. » Test : aucun nœud vide de hauteur non nulle à cet endroit.
   - guide2, mineur : estimation des rangées de la page Plus.
     - « en fr, deux mains, phone360 à 200 %, Mode Simple : sur la page Plus, les rangées de tuiles tiennent comme l'estimation le dit (pas de tuile coupée sous le pli). » Test : à 100 %, 130 %, 160 % et 200 %, la version compacte se déclenche dès que les rangées mesurées dépassent le pli (mesurer comme FitFaders, ou au moins `20f * 2` sp par titre).
   - siège 3a, quatre défauts « non localisés » (« ▶ सुनो » sans libellé, −/+ grisés sans mot, piste T ajoutée sans preuve, « 27 पार्ट » pour 20 parties ; siège 3a = tour 7, hi, phone360, 130 %, Simple, gestes #64, #14, #23, #65 ; liste dans `sieges/tour-7/lots-correction.md` l. 150-158, rapport `sieges/tour-7/lotC/a/rapport.md`) : un lecteur (sous-agent, lecture seule) les localise d'abord ; une ligne au chef avec fichier et ligne ; seuls ceux hors des fichiers des autres lots viennent ici, avec leur phrase.
6. Tests ciblés : `ANDROID_HOME=/opt/android-sdk bash scripts/bench/gradle-file.sh :app:testDebugUnitTest --tests '*Cut*' --tests '*Guide*' --tests '*Plus*' --tests '*AppSpaces*'`. Captures : celles des écrans touchés, regardées en entier, Simple et Pro, 100 %, 130 %, 200 %, sur 360 dp.
7. Rapport `docs/audits/ultra/tour-8-restes8-rapport.md`. Puis :
   - dans `corrections.tsv`, tes lignes passent à « tenté » ;
   - `PS_REJEUX_SELFTEST=$(mktemp -d) scripts/bench/rejeu-defauts.sh` ;
   - `pre-vol.sh --avant-relecture tour-8 restes8` ;
   - relecture neuve en dernier commit ;
   - `pre-vol.sh tour-8 restes8` doit dire PRÊT.
8. Parallèle et place :
   - libre à côté de M, K, T0a, T0b, T0c, B, S, N (fichiers disjoints) ;
   - après captures8 (captures) ; jamais à côté de garde-fous B si celui-ci change `CutScreen.kt` ;
   - place proposée : après le paquet « M + T0a », dans le paquet de T0b (trois lots au plus), ou en dernier si le tour est chargé (lot facultatif).
   - Contrôle : siège E-b (zh-CN, phone360, 100 %, main gauche, Simple, `amenee=remix`).
9. Une ligne : « <NOM> fini lot restes8 : <branche> <sha>, corrigés N/3 (+ 3a : K/4), demandes : …, laissés de côté : … ; pré-vol PRÊT ; self-test V verts, 0 rouge ; rejeux tous tenus ».

> **Demander au chef.** Un fichier hors de ta liste, un doute sur la phrase d'acceptation, un conflit avec un autre lot, un test qui casse hors de ta zone : une ligne au chef (`send_message`), puis tu continues le reste de ta fiche en attendant sa réponse. Jamais de question à Benjamin, jamais de décision hors de ta fiche.
> **Travailler en silence.** Aucun récit, aucun résumé, aucun texte pour Benjamin dans ta discussion. Tu n'écris qu'au chef, une ligne, à trois moments : « prêt », « demande / bloqué », « fini ». Sorties de commandes filtrées (`tail`, `grep`).
