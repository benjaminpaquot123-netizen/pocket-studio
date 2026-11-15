# Plan du tour 7 : contrôler les corrections du tour 6 (étape 1 : sièges et tri)

Écrit le 2026-11-15. Code testé : `46695c42` (`main` après la PR #57, qui porte les lots du tour 6). Aucune correction de l'appli dans ce tour : on joue, puis on trie.

## En trois lignes
1. Dix testeurs simulés, chacun joué par un sous-agent neuf, rejouent les endroits que le tour 6 a corrigés : le tuto (français, japonais, allemand Pro, coréen), la découpe du Remix à 200 % et à la main gauche, « Simple ou Pro ? », le Mode simple, les exports.
2. Sièges courts : ancrés au bon chapitre (`amenee=tuto:<style>:<chapitre>`) ou ramenés au bon écran par le rejeu du siège d'origine (`rejeu.puis=main`), 60 à 200 gestes.
3. Le tri classe chaque défaut (réel, voulu, artefact du banc, doublon), donne sa gravité, un rejeu aux bloquants et graves, et range les réels en lots aux fichiers disjoints.

## Dérogation à la règle des niveaux (`PLAN-NIVEAUX.md`)
Tour de contrôle ciblé : les sièges reprennent le profil de leur siège d'origine, pas un niveau tiré. Niveaux couverts quand même : curieux (2d, 3a), débutant motivé (1a, 1b, 1c, 2a), moyen (2b, 2c, 3b), pro (1d). Confirmé : non couvert, dit d'avance.

## Les dix sièges (détail : `sieges/tour-7/briefs/`)
| Siège | Langue, appareil, police, mode | Ancrage | Ce qu'il contrôle (origine tour 6) |
|---|---|---|---|
| 1a | fr, téléphone, 100 %, Simple | tuto ch. 4 | tambourin, basse, piano « Encore N notes » (H a, Ha-4) |
| 1b | fr, téléphone, 100 %, Simple | tuto ch. 12 | transitions, export « −14 » (H a, Ha-4) |
| 1c | ja, téléphone, 100 %, Simple | tuto ch. 3 | écoutes non comptées, consigne longue, saut de mesure (D a, Da-10) |
| 1d | de, tablette 8″, 130 %, Pro | tuto ch. 8 | fin du tuto Pro, jamais rejouée après Bb-9 (B b) |
| 2a | ko, petit téléphone, 130 %, Simple | tuto, chapitre des notes cachées | notes sous le panneau (D b) |
| 2b | pt-BR, téléphone, 200 %, Simple | rejeu de E c | **bloquant** : découpe sans forme d'onde ni repères (E c) |
| 2c | es, téléphone, main gauche | rejeu de B a | carte Simple et découpe au pouce gauche (B a) |
| 2d | fr, téléphone | aucun | « Nouveau projet → J'apprends → Simple ou Pro ? » : le grand vide sombre (retour de Benjamin du 2026-11-14) |
| 3a | hi, petit téléphone, 130 %, Simple, premier lancement | aucun | Mode simple : changer de partie, ▶ cette partie / le morceau (A a) |
| 3b | fr, téléphone | aucun | exports et « Mes fichiers » vide (A c) |

## Comment on joue
- Trois lots (4, 4, 2), quatre sièges au plus en même temps. Avant chaque lot : `scripts/bench/selftest.sh sieges/tour-7/lot<n>` (les autres sièges mis de côté), Gradle seulement par `scripts/bench/gradle-file.sh`.
- Chaque siège : un sous-agent neuf, qui ne lit que son brief, `sieges/tour-7/consignes-testeur.md` et les écrans du banc. Rapport au gabarit `docs/audits/GABARIT-RAPPORT-TESTEUR.md`, rubrique `defauts-avant:` (corrigé avec son écran, autre, encore là, pas atteint).
- Texte seul dans le dépôt (rapport, journal, mesures, cmd, screen, sound, start) ; commit par chemin après chaque lot, poussé sur `tour-7-sieges`.
- Si le siège court ancré ne compile pas ou ne mène pas au bon écran : siège normal du même profil, budget court, noté dans la synthèse.

## Sorties
- `sieges/tour-7/synthese-tour-7.md` : défauts du tour 6 corrigés, encore là, pas atteints ; défauts neufs avec leur verdict et leur gravité.
- `sieges/tour-7/lots-correction.md` : lots aux fichiers disjoints, phrase d'acceptation et siège d'origine pour chaque défaut ; ce qui relève du banc à part.

## Ce que le tour 7 ne regarde pas
Le son et les décrochages (téléphone seulement), le paysage, le thème sombre, les langues hors des six ci-dessus, les écrans qu'aucun siège n'atteint (listés « non vus »).
