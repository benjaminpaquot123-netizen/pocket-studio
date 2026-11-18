> CORRECTIONS DU CHEF (2026-11-18, après le vérificateur ; elles priment sur tout le reste).
- Tu travailles dans ta propre discussion, sur ton clone du dépôt, branche neuve `eq/tour-8-captures8` depuis `origin/ccr-80af1a1b-mn0wec` (tête 65c361474 au moins). Tu pousses cette seule branche : `git push -u origin eq/tour-8-captures8`.
- Installe d'abord le SDK Android (une fois, ~5 min) :
  `D=/opt/android-sdk; mkdir -p $D/cmdline-tools && cd /tmp && curl -sSLo clt.zip https://dl.google.com/android/repository/commandlinetools-linux-9862592_latest.zip && unzip -qo clt.zip -d $D/cmdline-tools && mv $D/cmdline-tools/cmdline-tools $D/cmdline-tools/latest && yes | $D/cmdline-tools/latest/bin/sdkmanager --sdk_root=$D --licenses >/dev/null; yes | $D/cmdline-tools/latest/bin/sdkmanager --sdk_root=$D "platforms;android-37.0" "build-tools;37.0.0" "platform-tools" "ndk;30.0.16248370" "cmake;4.1.2"`
- Gradle : `ANDROID_HOME=/opt/android-sdk bash scripts/bench/gradle-file.sh <tâches>`. La résolution des dépendances rate parfois au hasard (« Could not find … » sur un fichier qui existe) : relance avec `--refresh-dependencies`, jusqu'à 8 fois (testé chez le chef : ça finit par passer).
- Donc les captures se refont EN LOCAL : `:app:recordRoborazziDebug` puis `:app:verifyRoborazziDebug` (chemins complets, plus de noms aplatis). Le commit « tmp: » et l'entrée `ci.yml` sont abandonnés.
- `scripts/screenshot-diff.py` donne maintenant le chemin sous `app/screenshots/` (commit 65c361474).
- La CI complète, c'est le chef qui la lance quand tu dis « fini ».
- Un `recordRoborazziDebug` réécrit TOUTES les images qui diffèrent : n'ajoute (par chemin) que les images de ta liste et celles que ton changement explique ; restaure les autres avec `git checkout -- <chemin>` et nomme-les au chef.

> CORRECTIONS DU VÉRIFICATEUR (2026-11-18). Elles priment sur le texte dessous.
- Base : `git fetch origin ccr-80af1a1b-mn0wec`, tête f6c6d7c7f au moins. Rapports p8b : `git fetch origin eq/tour-7-controle-p8b` avant tout `git show ad3676b3:…` (ce commit n'est pas dans l'intégration).
- Preuve du vert : cette machine n'a pas de SDK Android, aucun test Gradle n'y tourne (même `:project-model:test`). Pousse ta branche, écris « prêt pour CI : <sha> » au chef ; il lance `gh workflow run ci.yml --ref <branche>`. Vert = job « Build, unit tests, screenshots » vert ET « Captures : 0 différentes » lu dans le résumé (la CI ne rougit jamais sur une capture).
- Captures : la liste des images changées se lit dans `results-summary.json` de l'artefact `screenshots-diff` (champ `golden_file_path`), jamais dans le résumé, qui aplatit les noms (156 noms en double). Tu ne réenregistres que les écrans de ta liste ; une autre image qui change = une ligne au chef.
- Textes : aucune phrase française neuve ou modifiée hors du lot T0c. Si un mot manque, demande au chef (leçon CI 360).
- Préalable, par le chef, avant ce lot : une entrée `workflow_dispatch` dans `ci.yml` qui lance `recordRoborazziDebug` et dépose `app/screenshots/` avec les chemins complets. L'option « tmp: par `wanted()` » est retirée : `wanted()` est dans `ScreenshotTest.kt` (l. 79), fichier interdit par cette même fiche.
- 10 ou 14 : les deux. 14 périmées = 10 à réenregistrer telles quelles + 4 qui montrent les deux défauts (`share_sheet_ok`, `partage/loop_simple`, `scale130/fr_track_frozen`, `scale130/nl_track_frozen`), refaites après correction. Critère de fin : 0 différente sur la tête du lot.
- « + Piste » : ajoute à ta liste `app/src/test/java/com/pocketstudio/app/synth/Lot6UiTrackBarMoreTest.kt` (il exige la flèche au bord droit de la rangée et la puce dessous qui répond). La flèche est posée par-dessus exprès (R19) : donne-lui sa place hors de la rangée qui défile. TrackBar sert aussi dans `ui/AppSpaces.kt:116` et trois fois dans SynthScreen : leurs captures changent, c'est attendu, liste-les.
- Partage : vérifie d'abord si les 60 px viennent du montage de la capture (`ScreenshotTest.kt` l. ~1836, `padding(top = 16.dp)`) ; si oui, dis-le au chef, ne touche pas ShareSheet.
- Ordre : ce lot part seul et fusionne en premier (le plan dit « immédiat, seul »). M et T0a refont leurs propres écrans après avoir repris l'intégration.

---

# Fiche tour 8 : lot 0 « captures8 » (branche `eq/tour-8-captures8`)

> Chef d'orchestre → <NOM>, rôle CORRECTEUR. Lot tour-8 captures8 : 10 captures à réenregistrer et 2 défauts d'écran. Aucune question à Benjamin : une ligne pour moi.

Rappels :
- Dépôt PUBLIC.
- Jamais `git add -A`, `git add .` ni `git commit -a` : on ajoute par chemin (une image = un chemin).
- Pas de push sur l'intégration (`ccr-80af1a1b-mn0wec`) ni sur `main`. Pas de PR.
- Aucun identifiant de modèle dans les commits.
- Commits « rouge: » puis « vert: ».
- Code et commits en anglais.

1. Base : `git fetch origin ccr-80af1a1b-mn0wec`, puis la branche neuve `eq/tour-8-captures8` depuis `FETCH_HEAD`.
2. Lis :
   - `docs/REVIEW.md`, section « Tour 7 : revue de fin de tour », ligne « Captures » ;
   - le commit e3182572 (38 captures refaites, run CI 44237862362) ;
   - les deux fiches de `docs/AIDE-FUSIONS-TESTS.md`.
3. Tes fichiers, et eux seuls :
   - `app/src/main/java/com/pocketstudio/app/export/ShareSheet.kt` ;
   - `app/src/main/java/com/pocketstudio/app/synth/TrackBar.kt`, seulement `RowMoreSign` (l. ~293) et l'endroit où il se pose sur la rangée (l. ~145-160) ;
   - `app/screenshots/**` ;
   - tests : `app/src/test/java/com/pocketstudio/app/export/ShareScreenshotTest.kt` (lu ; s'il faut le modifier, demande au chef) et un test neuf `Tour8CapturesTest.kt`.
   - Ne touche pas `ScreenshotTest.kt`, `.github/workflows/*`, ni aucun écran du tuto.
4. Défauts et phrases d'acceptation :
   - Partage : « en fr, phone à 100 %, Mode Simple : depuis Partager, la feuille de partage n'a plus de bande vide (environ 60 px) entre son contenu et son bord. » Captures : `share_sheet_ok`, `partage/loop_simple`. Test rouge d'abord : la hauteur de la feuille égale celle de son contenu, à 1 dp près.
   - « + Piste » : « en fr et en nl, phone à 130 %, Mode Simple : depuis le Synthé, la flèche blanche « → » (signe que la rangée continue) ne couvre plus le texte de « + Piste » ; elle reste visible en bout de rangée. » Captures : `scale130/fr_track_frozen`, `scale130/nl_track_frozen`. Test rouge d'abord : le rectangle de `RowMoreSign` ne coupe aucun texte de puce (nœuds sémantiques). Même famille vue en ja (« + トラ→ » coupé, siège b de p8b) : le test couvre aussi ja.
   - Une erreur se vérifie partout (CLAUDE.md) : le test passe sur tous les appareils (phone, phone360, tablet8) et toutes les polices (100, 130, 200 %), pas seulement dans le cas vu.
5. Les 10 captures à réenregistrer : `projects`, `remix`, `take_done`, `i18n/nl_projects`, et six sous `tablet8/`. Écart à signaler au chef : PROGRESS.md dit 14, REVIEW.md dit 10. REVIEW.md fait foi ; la liste exacte vient de `screenshot-diff.py`.
6. Refaire les captures sans Gradle local (pas de SDK Android ici) :
   - D'abord, regarder si la machine a un SDK (`echo $ANDROID_HOME`, `ls ~/Android/Sdk`). Si oui : `bash scripts/bench/gradle-file.sh :app:recordRoborazziDebug`, puis `:app:verifyRoborazziDebug`.
   - Sinon, par la CI :
     a. pousser la branche ;
     b. demander au chef de lancer la CI à la main (`ci.yml` a `workflow_dispatch` : `gh workflow run ci.yml --ref eq/tour-8-captures8`). Le job « Build, unit tests, screenshots » lance `compareRoborazziDebug`, puis `scripts/screenshot-diff.py` liste les images changées ;
     c. récupérer l'artefact `screenshots-diff` (`gh run download <run> -n screenshots-diff -D <dossier hors dépôt>`), et prendre les `*_actual.png` de `app/build/outputs/roborazzi/` ;
     d. le piège du tour 7 : dans cet artefact, les noms sont aplatis. `projects.png`, `tablet/projects.png` et `tablet8/projects.png` donnent le même `projects_actual.png`, et le dernier écrit gagne. Pour ces 10 noms, deux voies, à faire choisir par le chef :
        - (1, recommandée) un commit temporaire « tmp: » sur ta branche, qui garde un seul jumeau par passage par le `wanted()` des tests de capture (une passe sans `tablet8/`, une passe `tablet8/` seule). Deux CI, puis tu retires le commit temporaire avant la relecture ;
        - (2) le chef ajoute à `ci.yml` (hors lot) le téléversement de `app/screenshots/` après un `recordRoborazziDebug`, sur `workflow_dispatch`.
     e. vérifier chaque image avant de l'ajouter : nom, appareil (taille de l'image), langue ;
     f. ensuite une CI de plus : `screenshot-diff.py` ne liste plus rien.
7. Chaque capture refaite est regardée en entier (grille « œil neuf ») et nommée dans le rapport, avec ce qui a changé. Captures du partage et de « + Piste » : en Simple et en Pro, à 100 % et 130 %, sur un téléphone de 360 dp. Pas de pastille « ● REC · Noter · ⋯ » sur les captures montrées.
8. Rapport `docs/audits/ultra/tour-8-captures8-rapport.md`. Décisions dans `docs/decisions/tour-8-captures8.md`. Puis :
   - `PS_REJEUX_SELFTEST=$(mktemp -d) scripts/bench/rejeu-defauts.sh` ;
   - `scripts/bench/pre-vol.sh --avant-relecture tour-8 captures8` ;
   - relecture neuve en dernier commit ;
   - `pre-vol.sh tour-8 captures8` doit dire PRÊT.
9. Ordre (plan) : aucun lot qui change un écran ne fusionne avant celui-ci. Parallèle : libre à côté de T0a, M et des garde-fous. TrackBar.kt est dans `synth/` comme PianoRoll.kt (T0a), mais ce sont deux fichiers différents.
10. Une ligne : « <NOM> fini lot captures8 : <branche> <sha>, captures refaites N/10, défauts 2/2, CI <run> sans écart, pré-vol PRÊT ».

> **Demander au chef.** Un fichier hors de ta liste, un doute sur la phrase d'acceptation, un conflit avec un autre lot, un test qui casse hors de ta zone : une ligne au chef (`send_message`), puis tu continues le reste de ta fiche en attendant sa réponse. Jamais de question à Benjamin, jamais de décision hors de ta fiche.
> **Travailler en silence.** Aucun récit, aucun résumé, aucun texte pour Benjamin dans ta discussion. Tu n'écris qu'au chef, une ligne, à trois moments : « prêt », « demande / bloqué », « fini ». Sorties de commandes filtrées (`tail`, `grep`).
