# Aide : éviter les fusions et les tests recalés (demande de Benjamin, 2026-11-11)

Écrite sur la branche `ccr-50fbb648-4hacb8`, pour le chantier de la branche `ccr-80af1a1b-mn0wec` (le grand audit, ses équipes et son portail). Rien n'y a été changé : cette aide s'installe en une commande (voir la fin).

## Ce que l'enquête a trouvé

- **Fusions** : 13 lots sur 29 refusés à la première relecture (27 constats bloquants ou graves) ; 4 lots relus une deuxième fois seulement parce que leur branche avait pris du retard.
- **Tests sur GitHub** : 36 passages recalés lus, dont 28 lancés à la main depuis le 3 novembre. 14 fois le travail « moteur », dont 9 d'affilée les 3 et 4 novembre ; sur celui qui a été ouvert, l'étape en échec est `Run host tests`, qui se joue ici en quelques minutes. 20 fois la fumée sur émulateur. Le dernier (42936673557) : 64 minutes de tests verts, puis un rouge sur un contrôle de quelques secondes.
- **La cause de fond** : la consigne donnée aux équipes (`GABARIT-CONSIGNES-EQUIPES-V2.md`, 16 lignes) ne contient aucun des mots que le portail exige (« rouge: », « vert: », « corrigé », « Relu », « Verdict », « rejeu changé », « rouge constaté » : 0 fois chacun). Les équipes découvrent les règles en étant refusées.

Comptes faits par lecture des 29 relectures et 25 rapports du tour 5, du journal et des journaux GitHub ; ils n'ont pas été recomptés un par un. Sur GitHub, l'étape en échec n'a été lue que pour 9 travaux ; le reste est connu par le nom du travail.

## L'outil : `scripts/bench/pre-vol.sh`

Il lit, ne change rien, répond en quelques secondes, et dit **toutes** les raisons d'un refus d'un coup (le portail s'arrête à la première). Chaque ligne « RECALÉ » donne sa parade.

| Quand | Commande |
|---|---|
| L'équipe dit « j'ai fini », **avant** de demander la relecture | `scripts/bench/pre-vol.sh --avant-relecture <tour-N> <zone>` |
| Après la relecture, avant le portail | `scripts/bench/pre-vol.sh <tour-N> <zone> [<zone> …]` (dans l'ordre prévu pour la fusion) |
| Avant de lancer la CI à la main | `scripts/bench/pre-vol.sh --ci` (`--long` ajoute les projets réels) |
| Première étape de la CI de GitHub (travail « quick ») | `scripts/bench/pre-vol.sh --ci --rapide` (environ une minute) |
| Avant une fusion vers `main` | `scripts/bench/pre-vol.sh --main` |

Ce qu'il contrôle :
- **avant le portail** : les refus du portail, avec les mêmes règles (relecture commitée, « Relu : », rien de changé après, verdict lu par `relecture-verdict.py`, rejeux, traces, liens symboliques, rapport de zone lu par `rapport-zone.py`), plus : la fusion essayée en mémoire (conflits, et qui a touché le fichier), les lots qui se marchent dessus quand on en donne plusieurs, l'atelier de l'équipe non commité, le disque, la base qui a bougé ;
- **avant la CI** : rien de non commité ni de non poussé, la CI de ce commit pas déjà rouge, les tests Python du banc joués comme sur une machine neuve (dossier personnel vide, pas d'identité git), les tests du moteur s'il a changé depuis leur dernier vert ici ;
- **avant `main`** : les conflits, et le cas de la fusion « squash » (voir plus bas).

Il prévient si le portail a reçu de nouvelles règles depuis son écriture (ligne `KNOWN_RULES`).

## La fiche des équipes (à coller dans la consigne)

Avant de dire « j'ai fini » :
1. Tout est commité dans ta branche, par chemin. Aucun lien symbolique, rien sous `docs/audits/ultra/portail/`.
2. Si l'intégration a bougé, fusionne-la dans ta branche **maintenant**, résous, rejoue tes tests ciblés. Un conflit résolu après la relecture coûte une deuxième relecture.
3. Chaque correction a un test qui rougit sans elle : un commit « rouge: <sujet> » (des tests seulement), puis « vert: <le même sujet> » (le code). Vérifie-le : retire la ligne corrigée, le test doit rougir.
4. Le selftest se relance après ton **dernier** commit de code.
5. Tu retires ou changes un rejeu, tu apportes une preuve dans `rouges-constates/` : dis-le au relecteur, il doit l'écrire.
6. N'écris rien de plus fort que ta preuve (verdicts, rapport, DECISIONS).
7. Regarde en entier les captures des écrans touchés, en Simple et en Pro.
8. Le suivi est à jour dans le lot : `verdicts.tsv`, PLAN-RESTE, journal.
9. Tes décisions vont dans `docs/decisions/<tour-N>-<zone>.md` (une ligne par choix), **jamais** dans `docs/DECISIONS.md` : le portail y ajoute lui-même le lien vers ton fichier. Refusé à partir du paquet 2.
10. Un rejeu touche un bouton par son étiquette de test (`touche <étiquette>`, aussi `appui-long`, `double-touche`), jamais par ses pixels : un changement de mise en page ne le casse plus.
11. Ton rapport de zone `docs/audits/ultra/<tour-N>-<zone>-rapport.md`, commité **dans la branche**, contient (refusé sinon à partir du paquet 2 ; `scripts/bench/rapport-zone.py`) :
    - une ligne `tests: <filtres Gradle>` (tes tests ciblés, que le portail rejoue après la fusion), ou `tests: aucun : <raison>` ;
    - la liste des chemins : une ligne `chemin: <écran> | test: Classe#méthode` par endroit qui lit ou montre la même donnée ; chaque `Classe#méthode` est un `@Test` sans `@Ignore` de `app/src/test` dans ta branche (la méthode dans cette classe-là) ;
    - pour chaque rejeu que tu ajoutes à `docs/audits/ultra/rejeux/` (souvent venu de `rejeux-attente/` avec ta correction), la ligne que `rejeu-defauts.sh` a écrite en le jouant avec ta correction : `<id> : oracle tenu`. La dernière ligne de cet id fait foi ; « non tenu », ou pas de ligne du tout, est refusé : un défaut pas corrigé reste dans `rejeux-attente/`.

## La fiche du relecteur

- Ton fichier : `docs/audits/ultra/<tour-N>-<zone>-relecture.md`, commité **dans la branche**.
- Ligne 2 : « Relu : <sha du commit que tu as lu> ».
- Chaque constat bloquant ou grave corrigé porte « **corrigé** » en gras, sans « pas » ni « non » devant ; dans un tableau, la dernière case commence par « corrigé ».
- Rejeu retiré ou changé : une ligne « rejeu retiré : <id> : <raison> » ou « rejeu changé : <id> : <raison> ».
- Preuve rouge apportée : « rouge constaté : <sha du vert> : rejoué sur <sha de son parent> : <ce que tu as vu> ».
- Dernière ligne, seule : « Verdict : 0 bloquant, 0 grave » (« — fusion possible » est permis).
- Après toi, plus rien ne bouge dans la branche, sauf ton fichier et une fusion de l'intégration.

## Pourquoi ça recale, et la parade

| Cause | Combien | Parade |
|---|---|---|
| Trou dans un script du banc ou du portail | 14 graves, 6 lots | Un test par contrôle neuf, qui rougit quand on casse le contrôle (fiche, point 3) |
| Phrase ou verdict plus fort que la preuve | 5 | Fiche, point 6 |
| Test qui ne prouve rien (vert même sans la correction) | 4 graves ; vu dans 9 lots | Fiche, point 3 |
| Défaut visible créé par la correction | 3 | Fiche, point 7 |
| Branche en retard, conflits dans les documents partagés | 4 lots relus deux fois | `pre-vol.sh --avant-relecture` |
| Relecture refusée pour sa forme | 4 | Fiche du relecteur, puis `pre-vol.sh` |
| Pas de selftest vert sur le dernier commit | 3 | Fiche, point 4 |
| État laissé par un test au suivant | 6 (dont 2 CI rouges) | Une suite complète ici par paquet de fusions, avant la CI |
| Tests capricieux (renommage, règle Compose v1) | 5 au moins | À régler une fois sur la base (ci-dessous) |
| Captures instables, rouges à chaque suite complète | 5 lots | À régler une fois sur la base |
| Machine : identité git, Gradle tué, disque | 7 | `pre-vol.sh --ci`, et le disque contrôlé avant le portail |

## À décider une fois, côté orchestrateur du chantier

1. **Coller les deux fiches dans la consigne des équipes et du relecteur.** C'est le gain le plus sûr.
2. **Geler les règles du portail pendant un paquet de lots** : une règle neuve vaut pour le paquet suivant. Trois relectures déjà fusionnées sont devenues refusables après coup.
3. **Une base verte** : les 5 captures instables et le test du renommage se règlent ou se mettent en quarantaine une fois ; aujourd'hui chaque lot refait le même tri.
4. **Documents partagés** (DECISIONS, PLAN-RESTE, journal, `verdicts.tsv`) : chaque lot écrit dans un fichier à lui, le portail les assemble. Plus de conflit entre deux lots menés ensemble.
5. **CI** : mettre les contrôles de quelques secondes en première étape (les tests Python du banc sont à la 64e minute). Ne jamais relancer un commit déjà rouge.
6. **Fumée sur émulateur** (20 échecs) : elle ne se rejoue pas ici (pas d'émulateur). Lire le nom du test en échec avant toute relance.
7. **Avant la fusion vers `main`** : aujourd'hui 270 fichiers en conflit, parce que `main` est une fusion « squash » d'un ancien état de la branche et n'apporte rien de neuf. `pre-vol.sh --main` donne la commande qui règle ça sans changer un seul fichier (essayée sur une copie).

## Fait par le lot « socle » (2026-11-11)

- **Décisions par lot** : `docs/decisions/<tour-N>-<zone>.md` ; le portail le liste à la fin de `docs/DECISIONS.md` (`scripts/bench/decisions-index.sh`). `docs/DECISIONS.md` et le journal du banc se fusionnent par union (`.gitattributes`) : deux lots qui y ajoutent chacun une ligne ne se marchent plus dessus. `verdicts.tsv` et PLAN-RESTE restent partagés : leurs lignes changent sur place, l'union y garderait l'ancienne et la nouvelle (voir `docs/decisions/tour-5-socle.md`).
- **Règles gelées par paquet** : `scripts/bench/portail-paquet.txt` dit le paquet en cours. Une règle ajoutée au portail pendant le paquet N s'écrit `from_paquet N+1` sur sa ligne de refus : elle ne vaut qu'au paquet suivant ; d'ici là le pré-vol la dit en « À VOIR » (« refusé à partir du paquet N+1 »). `scripts/bench/test_socle.py` refuse une règle neuve sans cette garde. Seule la discussion principale change le numéro, quand elle ouvre un paquet (après la CI du précédent) ; une règle reformulée compte comme neuve.
- **Rejeux par étiquette** : voir la fiche, point 10. `t4-26-prise-clavier` est passé aux étiquettes.
- **CI** : le travail « quick » passe en premier, tous les autres l'attendent.
- **Base verte** : voir `docs/audits/ultra/tour-5-socle-rapport.md` pour les cinq captures et le test du renommage.

## Après chaque fusion (point 7.1 du plan)

Le portail relance `fuse-reports.py docs/audits/ultra/sieges/<tour-N> --tour <tour-N> --maj-base` (les nouvelles clés vont dans `base.tsv`) puis `tableau-de-bord.py` sur tous les tours, et met ce qu'ils écrivent (`base.tsv`, `clusters*.md`, `promesses.tsv`, `incidents-banc.tsv`, `tableau-de-bord.md`) dans le commit des traces. Si l'un d'eux échoue, la fusion est annulée comme pour toute trace.

## Ce que le pré-vol ne voit pas

La qualité d'une correction, un test qui ne prouve rien, un test capricieux, la fumée sur émulateur, et le selftest avec ses rejeux joués après la fusion (environ 40 minutes de Gradle). Le portail reste le juge ; le pré-vol évite seulement de le déranger pour rien.

## Preuves

- `scripts/bench/test_pre_vol.sh` : 25 contrôles en 5 secondes, sans Gradle. Chaque refus est aussi donné au vrai portail, qui doit refuser lui aussi. Deux contrôles cassés exprès : le test rougit sur ces deux-là.
- Sur le vrai dépôt : le lot `banc6d`, que le portail a accepté, sort « PRÊT » ; sept variantes piégées de ce lot sortent « RECALÉ » avec la raison du vrai portail.
- Le commit du passage 42936673557 : le pré-vol retrouve en 27 secondes l'erreur que GitHub a signalée après 64 minutes (identité git absente) ; sur le commit corrigé, il ne dit rien.
- Non essayé ici : les tests du moteur et les projets réels lancés pour de vrai par le pré-vol (leurs scripts ont été remplacés par des doublures) ; la lecture de l'état de la CI, qui demande l'outil `gh` connecté.

## Installer sur la branche du chantier

```
git fetch origin ccr-50fbb648-4hacb8
git checkout origin/ccr-50fbb648-4hacb8 -- scripts/bench/pre-vol.sh scripts/bench/test_pre_vol.sh docs/AIDE-FUSIONS-TESTS.md
scripts/bench/test_pre_vol.sh
```
Trois fichiers neufs, aucun fichier existant touché, donc aucun conflit.
