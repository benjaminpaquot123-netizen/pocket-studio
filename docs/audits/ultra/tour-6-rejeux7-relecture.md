# Tour 6, lot du banc « rejeux7 » : relecture neuve
Relu : 033f94985a61103c00a43ae872a004704a74f5dd

Relecteur neuf (n'a rien écrit de ce lot), quatrième passage. Les deuxième et troisième passages sont gardés tels quels ; le premier passage (sur `b0590d2c`, puis `35c6e341`) est résumé plus bas.

## Quatrième passage (journaux sortis du suivi, `t6-ga14` reciblé sur `notice_ok`, intégration `ae8727624`)

Depuis la relecture `52b89855d`, trois commits sur la branche : `400bb7790` (journaux des rejeux sortis du suivi), `dc2523173` puis `033f94985` (`t6-ga14` reciblé), `8f4df40be` (fusion de l'intégration `ae8727624`).

- `git diff 52b89855d HEAD -- docs/audits/ultra/rejeux .gitignore` : exactement ce qui est annoncé. Les 16 fichiers de `rejeux/_journaux/` (4 rejeux) sortent du suivi, une ligne `.gitignore` (`docs/audits/ultra/rejeux/_journaux/`, avec son commentaire) ; dans `t6-ga14-phrases-batterie/` : `cmd-13` devient `touche notice_ok`, l'ancien 13 (`remix_open_board`) passe en 14, l'ancien 14 (`board_chop`) en 15, `oracle.txt` passe à `rejeu.n=15`. `rejeu.attendu` et `start.txt` inchangés. Aucun autre rejeu touché ; `rejeux/_non-tenu-1585bb46/` garde ses fichiers.
- Forme des gestes : un geste par fichier, `touche <étiquette>` sans retour à la ligne final, comme les autres rejeux (`t6-ec17`, `t4-26`) ; 15 fichiers `cmd-1` à `cmd-15`, sans trou. `python3 -I scripts/bench/test_socle.py` : 29 tests, OK (dont `ReplaysAimByTag`).
- `notice_ok` existe dans l'appli : `AppRoot.kt` l. 1092, le bouton « Compris » de la fenêtre d'avis (`rhythmVm::dismissNotice`). `remix_open_board` existe toujours (`RemixScreen.kt` l. 508).
- L'oracle lit toujours le défaut R6 : `rejeu.attendu=(Davul · 16 pad)`, la phrase de la batterie que la correction ajoute. Fermer la fenêtre d'avis ne fait qu'ôter un écran par-dessus : elle n'écrit pas « (Davul · 16 pad) ». Le geste ajouté ne peut donc pas faire tenir l'oracle à tort. Sans ce geste, le rejeu se perdait sous la fenêtre (le lot « coquille2 » l'ouvre après « Projeme ekle », voulu).
- Fusion `8f4df40be` : `git diff ae8727624 HEAD` hors fichiers du lot (`rejeux/`, `rejeux-attente/`, rapport, relecture, décisions, `.gitignore`) : vide. `git diff origin/ccr-80af1a1b-mn0wec HEAD --stat` : 100 fichiers, 939 lignes ajoutées, 0 retirée, tous du lot. `git merge-tree --write-tree origin/ccr-80af1a1b-mn0wec HEAD` : propre (code 0).

### Points de ce passage

- moyen : le rapport de zone (`tour-6-rejeux7-rapport.md`) n'a aucune ligne sur le reciblage. Sa ligne `t6-ga14` dit encore « G a (tr, Pro, 130 %), 1 à 14 » alors que le rejeu compte 15 gestes dont un (13, `notice_ok`) qui n'est pas un geste du siège ; et la ligne 17 dit « Les passages sur la tête sont dans `rejeux/_journaux/<id>/` », dossier désormais hors suivi (il n'existe que sur la machine qui a rejoué). À compléter par l'auteur du lot (non modifié ici).
- mineur : sur 1585bb46, la fenêtre d'avis n'existait pas ; le rejeu reciblé y tomberait au geste 13 (« notice_ok » refusé) au lieu de tomber sur l'oracle. Il resterait « non tenu », donc rien de faux ; mais la preuve « non tenu sur 1585bb46 » gardée dans `_non-tenu-1585bb46/t6-ga14-phrases-batterie/` vient de la version à 14 gestes.
- mineur : `rejeux-attente/_journaux/t6-ha19-cases-panneau/` reste suivi, la ligne `.gitignore` ne couvre que `rejeux/_journaux/`. Un passage de ce rejeu en attente salirait l'arbre comme avant.
- Les points ouverts des passages précédents restent valables (T4, part des cases, non corrigé sur la tête ; N4 et N5 « tentés »).

## Troisième passage (fusion de l'intégration `2fbf6eb17`)

Depuis la relecture `87ac81728`, un seul commit : `3f7adf5f3`, fusion de la tête d'intégration `2fbf6eb17` (le portail avait un conflit avec cette tête).

- Fichiers du lot (`rejeux/`, `rejeux-attente/`, rapport, relecture, décisions) : `git diff 87ac81728 HEAD` sur ces fichiers est vide. La fusion n'en change aucun.
- Tout le reste est identique à `2fbf6eb17` (`git diff 2fbf6eb17 HEAD` hors fichiers du lot : vide) ; tout ce que la fusion apporte vient de l'intégration (`scripts/bench/`, traces du portail).
- `git diff origin/ccr-80af1a1b-mn0wec HEAD --stat` : 114 fichiers, 1505 lignes ajoutées, 0 retirée, tous des docs du lot.
- `git merge-tree --write-tree origin/ccr-80af1a1b-mn0wec HEAD` : propre (code 0, arbre `7efb9031b`), aucun conflit.
- Aucun point neuf. Les points ouverts des passages précédents restent valables (T4, part des cases, non corrigé sur la tête ; N4 et N5 « tentés »).

## Deuxième passage (après le refus du portail, code 8)

Le portail avait refusé le lot : son rapport nommait N19, N4, N5, R3-EC17, R6 et T4 sans ligne « corrige: … » ni « ne corrige pas: <id> : <raison> ». Depuis `35c6e3410`, deux commits sur la branche : `4ea9c367` (le rapport) et `7dbb5c7f` (fusion de l'intégration `eb033ca6f`).

### Ce que change le lot

- `git diff 4ea9c367~1 4ea9c367` : un seul fichier, `tour-6-rejeux7-rapport.md`, 11 lignes ajoutées en fin de rapport (section « Défauts nommés, non corrigés par ce lot »), une ligne « ne corrige pas: » par défaut nommé. Rien d'autre.
- La fusion `7dbb5c7f` : aucun fichier du lot touché (`rejeux/`, `rejeux-attente/`, rapport, relecture, décisions identiques à `4ea9c367`). Contre la tête `eb033ca6f`, la branche n'ajoute que des lignes (114 fichiers, 0 ligne retirée), toutes sous `docs/`. Aucun conflit, aucun marqueur de conflit.

### Les six lignes sont-elles vraies ?

- La forme « ne corrige pas » est la forme honnête : le lot ne change aucun code de l'appli (rien hors `docs/`). Une ligne « corrige: » serait fausse.
- R3-EC17 et R6 : corrigés par « remixdecoupe » (son rapport, lignes 8 et 9). N19 : corrigé par « tutoconsignes » (`Tour6TutoPanelListenSkipTest`). Les trois lots sont fusionnés dans l'intégration (« remixdecoupe » `4966a019d`, « tutogrille » `1980dff63`, « tutoconsignes » `0b8ba680c`). Les rejeux `t6-ec17`, `t6-ga14`, `t6-da15` les rejouent : non tenus sur 1585bb46, tenus sur la tête (premier passage).
- T4, N4, N5 : « tutogrille » les a traités (son rapport, lignes 9 à 14). Aucun rejeu ne le prouve ici : N4 et N5 sont classés impossibles (touchers sans étiquette), et pour T4 la part des cases est **non tenue sur la tête** (`t6-ha19`, en attente).
- Le portail : `rapport-zone.py --corrections eq/tour-6-rejeux7 … base:eb033ca6f tete:eb033ca6f tour:tour-6 ajout:<les trois rejeux>` : code 0 ; même résultat avec la base d'origine `cc6175a4`. Le contrôle simple (sans `--corrections`) sort toujours les trois filtres, code 0 ; `--tuto` : code 0, rien (le lot ne touche pas le tuto).

### Points de ce passage

- moyen : la raison de la ligne T4 dit « la correction vient des lots déjà fusionnés ». C'est vrai pour une partie seulement : le rapport lui-même dit plus haut que T4, la part des cases, n'est pas corrigé sur la tête. La ligne n'est pas fausse sur ce que fait ce lot (il ne corrige pas T4), mais le chef ne doit pas en tirer « T4 corrigé ». À rendre au tri (zone tuto, `RhythmScreen.kt`), comme au premier passage.
- mineur : N4 et N5 sont dits corrigés par les lots fusionnés, sans rejeu qui le montre (impossibles ici). Selon la méthode, ils restent « tentés », pas « corrigés », tant qu'un testeur neuf ne l'a pas constaté.
- mineur : la même raison est copiée sur les six lignes et cite les trois lots à chaque fois ; chaque défaut n'a été traité que par l'un d'eux (R3-EC17 et R6 : remixdecoupe ; N19 : tutoconsignes ; T4, N4, N5 : tutogrille).

## Premier passage (résumé, sur `b0590d2c`)

- Le lot ne touche rien hors `docs/` ; `rejeux/t4-26-prise-clavier/` intact ; aucun rejeu retiré ni changé.
- Les trois rejeux ajoutés (`t6-ec17-un-bout-courbe`, `t6-ga14-phrases-batterie`, `t6-da15-ecoute-passee`) ont les gestes du siège, par étiquette, sans pixel ni option interdite ; les oracles collent aux défauts de `verdicts.tsv` ; non tenus sur 1585bb46, tenus sur la tête.
- `t6-ha19-cases-panneau` (T4, cases) rangé en attente à juste titre : non tenu sur la tête. T4 (notes du piano roll), N4, N5 : impossibles pour une raison qui tient. Aucun rejeu oublié.
- Rejoué soi-même : `t6-ga14` tenu sur la tête, non tenu sur 1585bb46 (mêmes 12 étiquettes, 0 refusé).
- Points laissés ouverts : moyen, T4 (cases) pas corrigé sur la tête ; moyen, l'oracle de `t6-ec17` ne prouve pas la hauteur de la courbe (le test `CutBoutBigTextTest` la couvre) ; mineurs, écrans de 1585bb46 non comparés à l'origine, chemins absolus `/tmp/…` dans les `verdict.txt`, un premier passage du self-test peut tomber si Robolectric ne télécharge pas son image.

Verdict : 0 bloquant, 0 grave
