# Consignes communes aux équipes de correction, v2 (à copier dans `sieges/tour-N/consignes-equipes.md`)

Méthode d'audit v2 (`docs/audits/AUDIT-ULTRA.md` §12). Remplace les consignes des tours 1 à 3 (six équipes dans un seul dossier, commits par morceaux).

- **Atelier** : le tien seul, `/home/user/eq-tour-N-<zone>` sur la branche `eq/tour-N-<zone>` (créé par le coordinateur avec `scripts/bench/equipe-atelier.sh cree tour-N <zone>`). Tu ne touches jamais `/home/user/fx-int` ni l'atelier d'une autre équipe. 3 équipes au plus par tour.
- **Fichiers** : ceux de ta zone d'après `docs/CARTE-CODE.md`. Les fichiers partagés de ce tour ont un propriétaire nommé ci-dessous ; si tu dois y toucher, demande-le dans ton rapport (section « Demandes ») au lieu de modifier.
  - Propriétaires du tour N : `AppRoot.kt` → zone …, `RhythmViewModel.kt` → zone …, catalogues i18n → zone … (rempli par le coordinateur).
- **Avant de coder, pour chaque défaut ≥ grave** :
  1. le rejeu du siège : `scripts/bench/rejeu-nouveau.sh <siège> <n> <id> 'absent=…'|'attendu=…'` (§12.5), puis `scripts/bench/rejeu-defauts.sh <id>` : il doit dire « oracle non tenu » ;
  2. la **liste des chemins** (§12.6) : tous les endroits qui lisent ou montrent la même donnée, chacun avec le test qui le couvrira ;
  3. si la cause peut être le banc : pas de verdict sans les trois preuves du §12.3 ; tu proposes, l'agent banc écrit la ligne de `docs/audits/ultra/verdicts.tsv`.
- **Tests** : un test qui échoue avant la correction et vérifie ce que le testeur lisait à l'écran (pas un total). Gradle **uniquement** par `scripts/bench/gradle-file.sh :app:testDebugUnitTest --tests …` (tests ciblés ; jamais la suite complète ; jamais `./gradlew` en direct).
- **Commits** : sur ta branche, par chemin (jamais `git add -A`/`.`/`commit -a` ; `git ls-tree -r HEAD | grep -c '^120000'` = 0). Rien de non commité à la fin. Tu ne pousses pas et tu ne fusionnes pas : c'est l'agent de fusion (`scripts/bench/fusion-equipes.sh`), après la relecture neuve de ta branche.
- Interface en français via `tr()`, toute nouvelle chaîne dans les 15 catalogues ; design selon `docs/DESIGN.md` ; captures ré-enregistrées seulement pour ce que tu changes, et regardées.
- **Rapport** `docs/audits/ultra/tour-N-<zone>.md` (≤ 50 lignes, sur ta branche) : par item, n° de synthèse, liste des chemins cochée, id du rejeu et son verdict avant/après, test, commit, verdict proposé (appli / banc / voulu), reste avec un propriétaire et un tour.
