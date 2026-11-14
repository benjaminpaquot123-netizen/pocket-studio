# Tour 6, lot du banc « rejeux7 » : relecture neuve
Relu : 7dbb5c7fd1ebe3adfad9fb48b19bdea18e9b1293

Relecteur neuf (n'a rien écrit de ce lot), deuxième passage. Le premier passage (sur `b0590d2c`, puis `35c6e341`) est résumé plus bas.

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
