# Tour 6, lot du banc « rejeux7 » : un rejeu par bloquant ou grave corrigé

Branche `ccr-a548b78b-rejeux7`, partie de la tête poussée `cc6175a4` (`ccr-80af1a1b-mn0wec`). Fichiers : `docs/audits/ultra/rejeux/`, `docs/audits/ultra/rejeux-attente/`, ce rapport, la relecture, `docs/decisions/tour-6-rejeux7.md`. Rien dans `app/src`, `engine/src`, `project-model/src`, `ui-components/src`, ni dans `scripts/bench/`.

Défauts visés (gravité retenue de `sieges/tour-6/verdicts.tsv`, corrigés par les lots fusionnés « remixdecoupe », « tutogrille », « tutoconsignes ») : R3-EC17 (bloquant), T4, R6, N4, N5, N19 (graves).

## Rejeux ajoutés à `rejeux/`

Chacun fait avec `scripts/bench/rejeu-nouveau.sh` depuis le dossier du siège, gestes par étiquette (aucun pixel, aucune option `--cible`, `--saute` ni `--commande`).

| id | défaut | siège, gestes | oracle | 1585bb46 | tête cc6175a4 |
|---|---|---|---|---|---|
| `t6-ec17-un-bout-courbe` | R3-EC17 (bloquant) : en « Un bout » à 200 %, la courbe et ses barres n'ont aucune place | E c (pt-BR, 200 %), 1 à 17 | attendu « Pince para ampliar » (la ligne de la courbe) | non tenu : absent, l'écran 18 montre les puces puis la barre d'envoi, comme le siège | tenu |
| `t6-ga14-phrases-batterie` | R6 (grave) : « 9 ile 14 arası pad'lerinde 6 cümle. » ne dit pas que ce sont les pads de la batterie | G a (tr, Pro, 130 %), 1 à 14 | attendu « (Davul · 16 pad) » | non tenu : absent | tenu |
| `t6-da15-ecoute-passee` | N19 (grave) : une écoute passée par « Suivant » n'est pas comptée, le chapitre reste inachevé (puis « もう少しで完成！ » à la fin) | D a (ja, Simple, 100 %), 1 à 15 | attendu « チャプター 1/15 · 完了 » | non tenu : « ステップ 3/3 » puis « 成功 次のステップ », le chapitre 1 reste à 2/3 | tenu (« よくできました、章クリア！ ») |

Les passages sur 1585bb46 sont gardés dans `rejeux/_non-tenu-1585bb46/<id>/` (journal, `rejeu.txt`, verdict) : joués dans un worktree détaché avec le banc de ce commit, selftest vert ; aucun geste refusé, aucune étiquette introuvable : le défaut est reproduit, le rejeu n'a pas divergé. Les passages sur la tête sont dans `rejeux/_journaux/<id>/`.

Dernier passage sur la tête (`PS_REJEUX_SELFTEST=… scripts/bench/rejeu-defauts.sh`, self-test : 185 verts, 0 rouge, 26 sautés) :

t4-26-prise-clavier : oracle tenu
t6-da15-ecoute-passee : oracle tenu
t6-ec17-un-bout-courbe : oracle tenu
t6-ga14-phrases-batterie : oracle tenu

## Rangé en attente (`rejeux-attente/`)

- `t6-ha19-cases-panneau` (T4, la part des cases) : H a-suite (fr, téléphone, 100 %), gestes 1 à 19 ; oracle : des cases en pointillé à l'écran (« en pointillé : à toucher »), aucune « [coupé, état « vide, en pointillé : à toucher »] ». Non tenu sur 1585bb46 (`rejeux-attente/_non-tenu-1585bb46/`) **et non tenu sur la tête** (`rejeux-attente/_journaux/`) : à l'étape du kick, la première rangée de cases à toucher est encore coupée. Le lot « tutogrille » ne l'avait vérifié qu'à 360 dp et 130 % (« déjà tenu »). T4 n'est donc pas corrigé pour ce cas : à rendre au tri (zone tuto, `RhythmScreen.kt`).

## Impossibles

- **T4, la part du piano roll** (D b #59, D a #82, H a #86/#94) : ce qui est faux, ce sont les notes en pointillé dessinées sous le panneau ; les notes du piano roll ne sont pas dans le texte de l'écran (incident E4 du banc, choix « réalisme » R1 gardé par le lot « banc7 ») : aucun oracle d'écran ne peut le dire. La part des cases est en attente (plus haut). Le rejeu des cases de D b (geste 48) diverge sur la tête au geste 40 : le siège pose fort le coup doux du clap, que la tête refuse maintenant (T3, corrigé).
- **N4** (H a-suite #140, B b-suite écran 128) : même cause qu'E4 (la note visée n'est pas dans le texte ; la correction fait défiler les notes en vue OU montre ↑/↓ : la ligne de texte peut rester la même), et les gestes 121 à 139 de H a-suite (comme ceux de B b-suite) posent des notes sur le piano roll, une surface sans étiquette : on ne les rejoue qu'en pixels (interdit, point 10), et la correction déplace justement ces notes.
- **N5** (H a-suite #162, « mesure 3/8 » au lieu de « mesure 2/8 ») : l'oracle tient dans le texte, mais le chemin passe par une trentaine de touchers de notes sans étiquette sur le piano roll (gestes 121 à 161) : pixels interdits, et déplacés par la correction T4/T11.
- **N19 depuis D a-suite #199** (l'écran « もう少しで完成！ ») : mêmes touchers de notes sans étiquette (gestes 97 à 121) ; le rejeu `t6-da15-ecoute-passee` prend le même siège au geste 15, où la même cause se voit déjà.

## Tests

Aucun test écrit par ce lot : ses rejeux protègent des corrections dont les tests sont déjà dans l'intégration ; ils sont cités ici pour que le portail les rejoue.

tests: com.pocketstudio.app.remix.CutBoutBigTextTest com.pocketstudio.app.remix.StemBoardRouterTest com.pocketstudio.app.tuto.Tour6TutoPanelListenSkipTest

## Liste des chemins

- chemin: Remix › Couper la chanson › Un bout (200 %) | test: CutBoutBigTextTest#aPieceAtTwiceTheTextOnAPhone
- chemin: Remix › Couper la chanson › Un bout (100 %) | test: CutBoutBigTextTest#aPieceAtTheUsualTextOnAPhone
- chemin: Remix › Tableau des canaux › Phrases sur les pads | test: StemBoardRouterTest#piecesOnPadsSayTheyAreOnTheDrumsSixteenPads
- chemin: Tuto › étape d'écoute › Suivant | test: Tour6TutoPanelListenSkipTest#suivantOnEveryExampleLeavesNoneForTheEnd

## Défauts nommés, non corrigés par ce lot

Ce lot ne change aucun code de l'appli : il rejoue des défauts corrigés par d'autres lots déjà fusionnés.

- ne corrige pas: R3-EC17 : lot de rejeux seul, aucun code de l’appli changé ; la correction vient des lots déjà fusionnés (remixdecoupe, tutogrille, tutoconsignes), ce lot la rejoue
- ne corrige pas: T4 : lot de rejeux seul, aucun code de l’appli changé ; la correction vient des lots déjà fusionnés (remixdecoupe, tutogrille, tutoconsignes), ce lot la rejoue
- ne corrige pas: R6 : lot de rejeux seul, aucun code de l’appli changé ; la correction vient des lots déjà fusionnés (remixdecoupe, tutogrille, tutoconsignes), ce lot la rejoue
- ne corrige pas: N4 : lot de rejeux seul, aucun code de l’appli changé ; la correction vient des lots déjà fusionnés (remixdecoupe, tutogrille, tutoconsignes), ce lot la rejoue
- ne corrige pas: N5 : lot de rejeux seul, aucun code de l’appli changé ; la correction vient des lots déjà fusionnés (remixdecoupe, tutogrille, tutoconsignes), ce lot la rejoue
- ne corrige pas: N19 : lot de rejeux seul, aucun code de l’appli changé ; la correction vient des lots déjà fusionnés (remixdecoupe, tutogrille, tutoconsignes), ce lot la rejoue
