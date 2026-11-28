> CORRECTIONS DU VÉRIFICATEUR (2026-11-18). Elles priment sur le texte dessous.
- Base : `git fetch origin ccr-80af1a1b-mn0wec`, tête f6c6d7c7f au moins. Rapports p8b : `git fetch origin eq/tour-7-controle-p8b` avant tout `git show ad3676b3:…` (ce commit n'est pas dans l'intégration).
- Preuve du vert : cette machine n'a pas de SDK Android, aucun test Gradle n'y tourne (même `:project-model:test`). Pousse ta branche, écris « prêt pour CI : <sha> » au chef ; il lance `gh workflow run ci.yml --ref <branche>`. Vert = job « Build, unit tests, screenshots » vert ET « Captures : 0 différentes » lu dans le résumé (la CI ne rougit jamais sur une capture).
- Captures : la liste des images changées se lit dans `results-summary.json` de l'artefact `screenshots-diff` (champ `golden_file_path`), jamais dans le résumé, qui aplatit les noms (156 noms en double). Tu ne réenregistres que les écrans de ta liste ; une autre image qui change = une ligne au chef.
- Textes : aucune phrase française neuve ou modifiée hors du lot T0c. Si un mot manque, demande au chef (leçon CI 360).
- T0a, fichiers : tout `PianoRoll.kt` côté tuto est permis, pas seulement l. 1460-1500 : le vrai chemin de « Touche ici » est l. 715-798 (placement, `LaunchedEffect(focus, view, …)`), `covered` l. 841/890, le zoom l. 304, `rollSight` l. 423. Aucun autre lot ouvert ne touche ce fichier.
- T0a, phrase d'acceptation, la même mot pour mot dans le plan et pour le siège : « téléphone, 100 %, house leçon 3 : à chaque note demandée, la note à poser est en vue au-dessus du panneau et le rond "Touche ici" est dessus ; je la pose d'un toucher ». Pas « je vois les 6 notes » : La 45 et La 57 sont à 13 rangées, c'est peut-être impossible sans réduire les rangées.
- T0a, rejeu : aucun `cmd-*.txt` de p8b n'existe. Avant la correction, le chef fait jouer un siège court (fr, téléphone, 100 %, simple, tuto:house:3) sur la tête de départ et range son dossier ; en attendant, le commit « rouge: » est un test unitaire `Tour8TutoTouchInSightTest` (project-model, TutoRollHint).
- T0a, compteur ja : le nombre vient de `left.size` (PianoRoll l. ~1474) ; ne touche pas `i18n/ja`.
- T0b : `fx/EffectsPanel.kt` n'existe pas ; le fichier est `app/src/main/java/com/pocketstudio/app/mixer/EffectsBox.kt`. AppRoot l. 1998 et 2100 : seulement après la fusion de M.
- T0c : aucun texte ne s'allonge (en lignes du panneau) dans aucune des 14 langues : le plafond Facile est 409 et le fichier a déjà 409 entrées, une de plus = rouge. Une clé française modifiée se change dans tous les fichiers qui la portent, le même jour, par ce seul lot (`aFrenchTextHasOneTranslationAcrossAreas`). Ne retire aucune étape de volume vérifiée : `Tour6TutoMixWordsTest` exige 18.
- Contrôle : T0a, T0b et T0c fusionnent chacun après son débutant neuf sur la branche du lot ; le contrôle complet (T-a à T-d) se joue une seule fois, après T0c.

---

# Fiche tour 8 : lot T0 « touche-ici » (zone tuto), en trois lots successifs

Le tuto est une seule zone : T0a, puis T0b, puis T0c, jamais deux en même temps. Chacun part de la tête poussée de l'intégration du moment, une fois le précédent fusionné.
Pourquoi découper : à p8b, a-V1 (« Touche ici ») est le seul défaut visé encore là. Autour, les 5 sièges ont trouvé une quinzaine de finitions qui touchent des fichiers différents : le geste du rouleau, les boutons « Je le fais », les textes de 14 langues. Les mettre ensemble ferait un lot trop gros et trop long à contrôler.

Rappels communs aux trois lots (à recopier dans chaque discussion) :
- Dépôt PUBLIC : aucun secret, aucune chanson de Benjamin, aucune donnée perso.
- Jamais `git add -A`, `git add .` ni `git commit -a` : on ajoute par chemin.
- Pas de push sur l'intégration (`ccr-80af1a1b-mn0wec`) ni sur `main`. Pas de PR.
- Aucun identifiant de modèle dans les commits.
- Pour chaque défaut : commit « rouge: <sujet> » (le test seul, vu rouge), puis « vert: <même sujet> ».
- Code, commentaires et commits en anglais.
- Gradle seulement par `scripts/bench/gradle-file.sh`.

---

## T0a : « Touche ici » met toute la mesure en vue (branche `eq/tour-8-t0`)

> Chef d'orchestre → <NOM>, rôle CORRECTEUR. Lot tour-8 t0 : défauts a-V1 (fr), a-V1 ja (siège b), compteur ja, alternance La 45 / La 57. Aucune question à Benjamin : une ligne pour moi.

1. Base : `git fetch origin ccr-80af1a1b-mn0wec`, puis la branche neuve `eq/tour-8-t0` depuis `FETCH_HEAD`. Tu ne fusionnes jamais autre chose que cette tête poussée.
2. Lis :
   - les rapports des sièges a et b du contrôle p8b : `git show ad3676b3:docs/audits/ultra/sieges/tour-7/controle/p8b/a/rapport.md` et `…/p8b/b/rapport.md`, puis `…/p8b/verdict.md` ;
   - `docs/audits/ultra/tour-7-fable-repar-relecture-p8.md`, rubrique « À ranger » (alternance La 45 / La 57 ; le test qui ne modélise ni le panneau ni la marge `covered`) ;
   - les deux fiches de `docs/AIDE-FUSIONS-TESTS.md` (« équipes » et « relecteur ») : elles font foi.
3. Tes fichiers, et eux seuls :
   - `project-model/src/main/kotlin/com/pocketstudio/model/tuto/TutoChecks.kt`, seulement `object TutoRollHint` (vers la l. 2353 : `next`, `pool`, `aside`, `also`, `way`, `brought`, `lit`) ;
   - `app/src/main/java/com/pocketstudio/app/synth/PianoRoll.kt`, seulement la ligne « tuto_notes_left » et sa pastille `tuto_notes_left_go` (l. ~1460-1500), la marge `covered` (l. ~887-890), et la clé `TutoHints.active` du `LaunchedEffect` (mineur de REVIEW.md, tour 7) ;
   - `app/src/main/java/com/pocketstudio/app/tuto/TutoPanel.kt`, seulement `tutoRollWhere` (l. ~294), si le calcul de la vue en dépend ;
   - les tests : `project-model/src/test/kotlin/com/pocketstudio/model/tuto/Tour7TutoTouchMovesTest.kt`, `Tour7TutoTouchShowsNewNoteTest.kt` ; `app/src/test/java/com/pocketstudio/app/tuto/Tour7TutoArrowInSightTest.kt`, `Tour6TutoLineGoTest.kt` ; plus un test neuf `Tour8TutoTouchHereTest.kt` (app, Robolectric) ;
   - le compteur « あと %d 個 » : seulement la ligne du code qui calcule le nombre affiché. S'il faut toucher `i18n/ja/*.json` : demande au chef (texte partagé = T0c).
   - Hors liste : rien. En particulier, pas `TutoController.kt`, pas `AppRoot.kt`, pas `PlayStrip.kt`, pas `tutos/*.json` ni `i18n/*` (T0b, T0c), pas `synth/TrackBar.kt` (lot captures8).
4. Rejeu d'abord (a-V1 est un grave, visé deux fois). Les gestes des sièges p8b ne sont pas dans le dépôt (seuls les `rapport.md` le sont, commit ad3676b3). Donc :
   - demande au chef un dossier de siège avec ses `cmd-*.txt`, rejoué par un banc ou un testeur : fr, phone, 100 %, Simple, `tuto:house:3`, jusqu'au premier « Touche ici » du chapitre 4 (écran 66 du siège a) ;
   - ensuite : `scripts/bench/rejeu-nouveau.sh <ce dossier> <n du geste "Touche ici"> t8-t0-touche-ici 'attendu=<texte de l'écran une fois toutes les notes en vue>' 'absent=plus bas'`, puis `scripts/bench/rejeu-defauts.sh t8-t0-touche-ici` : « oracle non tenu » avant ta correction ;
   - en attendant le dossier, commence par le test unitaire rouge (point 5).
5. Défauts et phrases d'acceptation (tu répares la phrase, tu ne la réécris pas) :
   - a-V1 (fr) : « en fr, deux mains, phone à 100 %, Mode Simple : depuis le tuto house, chapitre 4, Basse mesure 1 (puis mesure 4), je touche une fois « Touche ici » et je vois les 6 notes en pointillé de la mesure, sans flèche ↓ ni second « Touche ici » ; chaque note se pose ensuite d'un toucher. »
   - a-V1 ja (siège b, écran 47) : même phrase en ja, techno, chapitre 3 : un seul toucher au lieu de trois.
   - compteur ja : « en ja, phone à 100 %, Mode Simple : le nombre « あと N 個 » est égal au nombre de notes en pointillé que je vois dans la mesure » (siège b : 8 annoncées, 7 visibles).
   - Alternance La 45 / La 57 (relecture p8) : « deux notes visées à une octave d'écart : toucher sans poser ne fait plus alterner la vue grave / aigu ; la vue les tient toutes les deux, ou le rouleau se replie pour les montrer. »
   - Test : le test neuf dessine le panneau du tuto ET la marge `covered` (c'était le trou de Tour7TutoTouchShowsNewNoteTest), sur phone 360 dp et phone, à 100 % et 130 %.
6. Doute à faire trancher par le chef (une ligne, puis tu continues) : le plan dit « la note se pose », le siège attend « les notes visées à l'écran ». Je propose de garder « en vue » ; poser à la place de l'élève enlèverait l'exercice.
7. Tests ciblés : `bash scripts/bench/gradle-file.sh :project-model:test --tests '*Tour7TutoTouch*'` et `:app:testDebugUnitTest --tests '*Tour8TutoTouchHere*' --tests '*Tour7TutoArrowInSight*' --tests '*Tour6TutoLineGo*' --tests '*Tour7TutoPanelFold*'`. Captures : celles du rouleau en tuto (`app/screenshots/**/tuto_*roll*` ou celles qui changent), regardées en Simple et en Pro, à 100 % et à 130 %, sur 360 dp. Ne pas réenregistrer celles de captures8.
8. Ensuite, comme le gabarit, points 4 bis à 10 :
   - dans `corrections.tsv`, ta ligne passe à « tenté » ;
   - rapport `docs/audits/ultra/tour-8-t0-rapport.md` ;
   - décisions dans `docs/decisions/tour-8-t0.md` ;
   - self-test des rejeux, puis `pre-vol.sh --avant-relecture tour-8 t0` ;
   - relecture neuve en dernier commit ;
   - `pre-vol.sh tour-8 t0` doit dire PRÊT.
9. Lot du tuto : tu dis « prêt pour contrôle », pas « fini ». Siège de contrôle : un testeur neuf avec le `start.txt` de `docs/audits/ultra/sieges/tour-7/controle/tuto2/a/start.txt` (fr, phone, 100, simple, tuto:house:3, 120 gestes), plus un siège ja avec celui de `…/tuto2/b/start.txt`.

---

## T0b : « tuto-gestes » (après la fusion de T0a, branche `eq/tour-8-t0b`)

Même trame que T0a, points 1, 2 et 4 à 9.
Défauts :
- e ch.13 : « Ich mach's » ouvre seulement la feuille d'effet (envoi à 0 %, courbe non tracée).
- e ch.12 : l'éditeur reste sur l'onglet Rolls, et un toucher pose ×2.
- d : la Montée créée se pose après le Break au lieu de juste avant le Drop 2.
- d-V2 (« autre ») : « Il n'y a pas de partie « Montée » » s'affiche alors que la partie existe, vide.
- c : aucune croix rouge au ch.12 en ko.
- Morceau d'exemple : sa Montée est moins dense, à réécouter par `scripts/live-bench.sh`.

Fichiers :
- `app/src/main/java/com/pocketstudio/app/tuto/TutoController.kt` (`doIt` l. ~571, `fulfilNeed`) ;
- `project-model/src/main/kotlin/com/pocketstudio/model/tuto/TutoPlay.kt` ;
- `TutoChecks.kt`, hors `TutoRollHint` (placement de partie, morceau d'exemple l. ~1748) ;
- les tests `Tour8TutoGestures*Test`.
- `AppRoot.kt` (l. ~1998 et ~2100, « Je le fais » des envois) et `mixer/EffectsBox.kt` : seulement sur accord du chef. AppRoot est aussi touché par le lot M, le chef choisit l'ordre.

Phrases d'acceptation :
- ch.13 : « en de, deux mains, tablet8 à 130 %, Mode Pro : au tuto techno chapitre 13, je touche « Ich mach's » et l'envoi passe à la valeur demandée, la courbe est tracée, l'étape se coche. »
- ch.12 : « …au chapitre 12, l'éditeur s'ouvre sur l'onglet demandé par l'étape, et un toucher pose un seul roll. »
- Montée : « en fr, phone à 100 %, Mode Simple : au tuto techno chapitre 12, je crée la Montée et elle se place juste avant le Drop 2. »

Doute pour le chef : « A mesure 1/16 » (siège a) et « Drop 2 Takt 81/128 » (siège e) viennent de `ui/PlayStrip.kt` l. 321. C'est la position de lecture du morceau, hors zone tuto, et c'est peut-être voulu. Le chef tranche : rien, ou un lot hors tuto.
Contrôle : sièges neufs d et e (start.txt de `…/controle/tuto2/d/` et `…/tuto2/e/`).

---

## T0c : « tuto-mots » (après la fusion de T0b, branche `eq/tour-8-t0c`)

Textes seulement, dans `project-model/src/main/resources/tutos/*.json` et `i18n/*/tuto*.json`. Une phrase partagée n'a qu'un maître (leçon du tour 7 : deux lots qui écrivent la même phrase cassent I18nCatalogTest).
Défauts :
- « la dernière rafale » est encore là (grep au 18/11) : dnb ×2, drill, dubstep, gabber, jump_up ×2, jungle, liquid_dnb, neurofunk, riddim, techno, trance, et leurs 14 langues ;
- `jungle.json` l. ~329 : « la batterie roule de plus en plus vite » ;
- c : le texte de création de la Montée dit le dernier temps silencieux, alors que la mesure 4 a 16 claps ;
- ko : 엔딩 (partie) contre le bouton 끝, à séparer dans `i18n/ko/{tuto_techno,tuto_house,tuto_bass,tuto_autres,tuto_moments,tuto,parts,home,finish}.json` ;
- ko : « 인트로 2 » ne cite pas la cymbale retirée ;
- de : les 10 mots faux, liste dans `git show ad3676b3:…/p8b/e/rapport.md` ;
- ja : les mots faux du siège b ;
- c-V3 : l'étape roll courte en Facile.
Fichiers d'exception (garde-fous, voir `fiche-garde-fous.md`) : à faire ici ou juste après.
Tests :
- étendre `Tour7TutoBuildNotRolledTest` à toutes les langues (aujourd'hui fr seul) ;
- étendre `Tour6Tuto4TechnoTest` (l. ~392 : traductions Facile non gardées) ;
- `I18nCatalogTest`.
Contrôle : sièges neufs b (ja), c (ko, phone360, 130 %) et e (de).

---

> **Demander au chef.** Un fichier hors de ta liste, un doute sur la phrase d'acceptation, un conflit avec un autre lot, un test qui casse hors de ta zone : une ligne au chef (`send_message`), puis tu continues le reste de ta fiche en attendant sa réponse. Jamais de question à Benjamin, jamais de décision hors de ta fiche.
> **Travailler en silence.** Aucun récit, aucun résumé, aucun texte pour Benjamin dans ta discussion. Tu n'écris qu'au chef, une ligne, à trois moments : « prêt », « demande / bloqué », « fini ». Sorties de commandes filtrées (`tail`, `grep`).
