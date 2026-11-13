# Tour 6, lot du banc « constate » : rapport de zone

Plan : `docs/audits/ultra/PLAN-REPARATIONS-CONSTATEES.md` § 4 (demande de Benjamin : une réparation n'est « faite » que quand un testeur la constate). Branche `eq/tour-6-constate`, partie de `2e29fdd7` avec le lot « portail4 » (`fd1c2637`) fusionné. Décisions : `docs/decisions/tour-6-constate.md`.

tests: aucun : lot du banc seul (scripts et gabarits), aucun test Gradle changé ; banc : `python3 -I scripts/bench/test_constate.py`, `bash scripts/bench/test_pre_vol.sh`, `python3 -I scripts/bench/test_socle.py`, `python3 -I scripts/bench/test_portail3.py`, `python3 -I scripts/bench/test_portail4.py`, `python3 -I scripts/bench/test_ultra_bench.py`

## Ce que fait chaque point

| Point | Ce qui change | Rouge | Vert |
|---|---|---|---|
| 1. Le registre | `corrections.tsv` : en-tête + `phrase_acceptation`, `siege_origine`, `constat` (lignes des tours 1 à 4 intactes) ; `scripts/bench/reparations.py` (quatre états, lecture, commande du chef `--constate`) ; `applique-corrections.py` lit les nouveaux états | `c7baadfe` | `7adffbb5` |
| 2. Le portail | refus neuf avant la fusion (sortie 11, `from_paquet 5`) d'un lot qui écrit « corrigé », « rejoué », « constaté », un mot inconnu, une phrase ou un siège d'origine qui ne sont pas du trieur ; après une fusion verte, le portail pose lui-même « rejoué » ; miroir dans `pre-vol.sh`, `KNOWN_RULES="18 5f49949e50"` | `2385e569` | `3c24f639` |
| 3. Les sièges de contrôle | `human-act.py`, `human-tester.sh` (joue `controle/<id>/a/`, écrit `mesures.json` de chaque siège), `sortie-de-siege.py`, `fuse-reports.py`, `tableau-de-bord.py`, verrou du tuto (`rapport-zone.py --tuto`), gabarit du rapport (rubrique `defauts-avant`) | `4475642e` | `42700d30` |
| 4. Le tableau de bord | une ligne « Réparations » par tour, quatre chiffres et « réparés : C sur N pris » | `a848cbe4` | `2da01534` |
| 5. Les fiches | fiche 1 (siège de contrôle), fiche 2 (« 4 bis » : phrase, conditions d'origine, « tenté »), fiche 4b (« 2 bis » : le trieur écrit la phrase et le siège d'origine) | `7ce1fee5` | `6ede7980` |
| 2 bis. Constat tardif | l'en-tête propre du registre (`COLUMNS`) n'est pas un changement du lot : sans cela, ce lot-ci aurait été refusé par sa propre règle dès le paquet 5 ; tout autre en-tête reste refusé | `037d4f73` | `696a6ee0` |

Hors paires : `04bb954d` et `fb6e2439` (`test_portail4.py` exigeait exactement 17 règles du portail ; il en exige au moins 17 ; le second remet une ligne que le premier avait effacée).

Chaque rouge a été vu rouge sur des assertions (`FAIL`, aucune `ERROR`) avant son vert : point 1, 9 échecs ; point 2, 24 échecs Python et 7 rouges de `test_pre_vol.sh` ; point 3, 11 ; point 4, 3 ; point 5, 4 ; paire 2 bis, 1.

## Les formats

Registre (`docs/audits/ultra/corrections.tsv`, une ligne par citation d'un testeur) :

```
tour	item	siege	ecran	etat	commit	test	note	cible	phrase_acceptation	siege_origine	constat
tour-6	A2	B/a	15	tenté	1234567	rejeu-a2		en espagnol, main gauche … : je touche « Voz » et la piste s'ouvre	tour-6/lotB/a	
```

| État | Qui le pose | Comment |
|---|---|---|
| `pas pris` | le trieur | à la main, avec la phrase et le siège d'origine |
| `tenté` | l'équipe | sa ligne, son commit, sa preuve dans `test` (id du rejeu ou `Classe#méthode`) |
| `rejoué` | le portail | `reparations.py --rejoue`, après la fusion verte, si la preuve a tenu ; note « rejoué au portail <sha> : <preuve> » |
| `constaté` | le chef | `reparations.py --constate <tour> <id> <dossier du siège de contrôle>` ; `constat` = « <dossier>#<écran> » |

Rubrique du rapport d'un siège de contrôle (gabarit `docs/audits/GABARIT-RAPPORT-TESTEUR.md`) :

```
defauts-avant:
  - <id du défaut visé> | <corrigé|autre|encore là|pas atteint> | ecran: <n°> | <ce que tu as vu, avec tes mots>
```

Les écritures du tour 6 se lisent aussi (`- A1 : corrigé`, `- A7 | corrigé (écran 21 : …)`, `- <id> | corrigé (écran 98) : …`).

Ligne de refus neuve du portail (`scripts/bench/fusion-equipes.sh`) :

```
  if from_paquet 5 && ! states_out="$(python3 -I scripts/bench/reparations.py --lot "eq/$tour-$zone" "base:$base" "tete:$(git rev-parse HEAD)")"; then echo "$zone: the lot gives itself a state of the repairs register: $(printf '%s' "$states_out" | tr '\n' ';'): not merged." >&2; exit 11; fi
```

Pré-vol : « le lot se donne un état réservé du registre des réparations : … », RECALÉ dès le paquet 5, « À VOIR … refusé à partir du paquet 5 » avant.

Tableau de bord, sous le tableau de chaque tour :

```
Réparations (registre corrections.tsv) : constaté 1, rejoué 1, tenté 2, pas pris 1 — réparés : 1 sur 4 pris (seul « constaté » se dit corrigé ; les pas pris à part)
```

Siège de contrôle joué par le banc : `docs/audits/ultra/sieges/<tour>/controle/<id>/a/start.txt`, lancé par `bash scripts/human-tester.sh docs/audits/ultra/sieges/<tour>/controle/<id>` ; le banc écrit en tête de `rapport.md` « code testé : <sha> » et « joué par : … », et `mesures.json` à la fin. Le verrou du tuto accepte `<id>` = « Ba » (un seul siège dans le dossier) ou « Ba/a ».

## Les contrôles cassés exprès

Chaque contrôle neuf a été cassé une fois (copie jetable, fichier remis ensuite), suite relancée :

- Point 1 (13 cassures) : accents, mot de la réponse, écran exigé, défaut « pas pris », colonne réécrite, fin de rubrique, filtre « tenté » d'`applique-corrections.py`, passage « constaté » → « corrigé » dans `base.tsv`, lecture de l'état, cases manquantes : toutes rouges. Trois restaient vertes au premier passage (`.strip()` de la ligne réécrite, fin de rubrique, lecture de l'état dans `applique-corrections.py`) : tests ajoutés, puis rouges.
- Point 2 (20 cassures, dont l en-tête propre de la paire 2 bis) : état réservé, lecture à la tête, phrase, siège d'origine, `constat`, en-tête, dernier verdict d'un rejeu, classe entière des filtres, filtre « tenté », lignes du lot seulement, comparaison des lignes : rouges dans `test_constate.py`. Portail sans `from_paquet 5`, « rejoué » non mis dans le commit, bloc « rejoué » retiré, pré-vol sans `rule_from 5`, pré-vol sans le bloc : rouges dans `test_pre_vol.sh`. `tete:$before` remplacé par `tete:$base` : vert dans `test_pre_vol.sh` (dans le jouet, base et tête sont le même commit), rouge dans `test_constate.py` (contrôle de la ligne du portail) ; `computed+=` du registre retiré : rouge dans `test_constate.py`. (Corrigé après la relecture, M6 : « refus d'une phrase effacée » était écrit ici sans test qui le montre ; le test existe depuis `16925f45`.)
- Point 3 (11 cassures) : chemin de contrôle de `human-act.py` (2), mesures à la fin du lot, message du dossier plat, nom du siège dans `sortie-de-siege.py`, nom dans les grappes et les incidents, rubrique `defauts-avant`, missions du tour sans les contrôles, plusieurs sièges dans un dossier, dossier imbriqué : toutes rouges.
- Point 4 (6 cassures) : meilleur état, tour, double compte, « pris » sans les « pas pris », lecture de l'état, ligne absente : toutes rouges ; le double compte restait vert au premier passage, ligne de test ajoutée.
- Point 5 (5 cassures) : chaque ligne neuve des fiches retirée, la ligne `defauts-avant` changée, un des quatre mots retiré : toutes rouges.

## Comptes, sur un arbre au repos

`bash scripts/bench/test_pre_vol.sh` : tout est vert (94 contrôles) ; `test_socle.py` 29 OK ; `test_portail3.py` 77 OK ; `test_portail4.py` 59 OK ; `test_constate.py` 64 OK ; `test_ultra_bench.py` « ultra bench scripts: OK ». Sur le vrai dépôt : `applique-corrections.py --dry` donne la même sortie qu'avant (347 citations, 315 clés, 0 sans clé, 0 ambiguës) ; `fuse-reports.py` sur le tour 6 nomme `controle/Da`, `controle/Ha` ; le tableau de bord des tours 5 et 6 imprime ses deux lignes « Réparations » (aujourd'hui tout à 0 : aucune ligne du tour 6 dans le registre).

## Ce que les programmes ne voient toujours pas

- Que la preuve d'un « tenté » a été jouée dans les conditions du siège d'origine (`start.txt`), ni qu'elle était rouge avant la réparation (règle 2) : le rapport du lot et sa relecture neuve le disent.
- Que le rejeu ou le test nommés sont bien ceux du défaut : un lot peut citer un rejeu tenu sans rapport.
- Un siège de contrôle « plat » (`controle/<id>/start.txt`) ne se joue pas : le banc dit où mettre le `start.txt`.
- Que le siège de contrôle était neuf, avait le même profil et le budget de la mission d'origine ; un « corrigé » se relit sur son écran, il ne se recalcule pas.
- La règle des allers-retours (règle 4 : un « autre » retourne à l'équipe, deux fois au plus) : rien ne la compte.
- Aujourd'hui (paquet 4), le refus et « rejoué » dorment : ils valent dès que le chef ouvre le paquet 5.
- La condition d'entrée de la règle 3 (aucun bloquant ni grave visé « autre » ou « encore là », aucun grave neuf) : au chef et à la relecture (m2).

## Relecture neuve (première passe sur 6e144989 : 1 grave, corrigé)

Deux paires : `18cffddd` / `d346c9ed` (G1) et `16925f45` / `936d4963` (le reste). Rouges vus : G1, 2 échecs sur des assertions ; le reste, 26 échecs, aucune `ERROR`.

Corrigé :
- G1 : les lignes d'une branche se lisent avec les colonnes du registre ; une ligne plus longue que l'en-tête de sa branche, ou que les 12 colonnes, est refusée (« merge the integration ») ; une branche à l'ancien en-tête qui ne touche pas au registre passe.
- M1 : comparaison ligne par ligne avec la base et la tête (clé : tour, défaut, siège, écran, cible) : pas d'ajout, pas de retrait, pas d'abaissement (« rejoué »/« constaté » → autre chose, « tenté » → « pas pris »), pas d'autre case que `etat`, `commit`, `test`, `note`.
- M4 : « pas pris » est au trieur ; un lot fait seulement passer sa propre ligne de « pas pris » à « tenté ». La colonne `constat` reste (écart déclaré).
- M2 : un seul lecteur (`reparations.load`) pour `reparations.py`, `applique-corrections.py` et `tableau-de-bord.py` : coupure sur « \n » seulement ; U+2028, U+2029, \v, \f, \x1c-\x1e, \x85, \r et les autres caractères de contrôle rendent le registre illisible (refus, sortie 2, ligne « illisible ») ; `git show` lu en octets (le mode texte cachait « \r »).
- M3 : « rejoué » par un test : un @Test (sans @Ignore) au commit fusionné, couvert par un filtre de la forme que le portail passe vraiment (`Classe`, `pkg.Classe`, `*Classe`, `Classe.méthode`, `pkg.Classe.méthode`).
- M5 : `--constate` refuse un dossier qui n'est pas sous `sieges/<même tour>/controle/`, un rapport sans « code testé » connu, un code qui ne contient pas le commit de la ligne (git, relation d'ancêtre), une ligne sans commit.
- M6 : test de la phrase du trieur effacée ; la phrase du rapport est corrigée.
- m1 : « corrigé ? non… » et « corrigé : non… » ne sont pas lus comme « corrigé ». m3 : plus de « \r » gardé par `rewrite()`.

Reste ouvert (une ligne chacun) :
- m2 : la condition d'entrée de la règle 3 n'est pas vérifiée par un programme.
- m4 : la courbe compte encore les sièges de contrôle (ce sont des sièges, leurs fiches sont des défauts) ; seul E les retire.
- La preuve dans les conditions d'origine, et le fait que le rejeu ou le test nommés soient bien ceux du défaut : non vus (rapport et relecture).

Contrôles cassés exprès pour cette passe : 25 cassures. 19 rouges du premier coup ; 6 restaient vertes et ont été traitées : la double condition de longueur de G1 (réduite à une seule, `min(en-tête, COLUMNS)`), l'abaissement et « tenté depuis rejoué » se couvraient l'un l'autre (deux tests ajoutés), la négation après « corrigé » (une ligne de test), le joker de `covers()` (code mort, retiré), et une cassure mal écrite sur « code testé » (refaite, rouge).

Essais à blanc, en lecture seule : ce lot passe sa propre règle (`--lot eq/tour-6-constate`, sortie 0) ; les 11 dernières vraies fusions d'équipe (ci3, tutoordre, portail3, tutofleches, emulateur, reparations, guidecoquille, tutoconsignes, ecoute, cablage, morceauexport), sortie 0, aucune ne touchait le registre ; une branche à l'ancien en-tête (`2e29fdd7`) contre la tête de ce lot : sortie 0.

## La liste des chemins

- chemin: banc › autotest du testeur humain (aucun code de l'appli touché) | test: BenchSelfTest#reachTagTouchesAVisibleElementInPlace

## Demandes

- Demande : `docs/audits/ultra/EN-COURS.md` : dire aux trieurs de remplir `phrase_acceptation` et `siege_origine` (état « pas pris ») et au chef la commande `reparations.py --constate` ; le chef seul écrit ce fichier.
- Demande : `docs/AIDE-FUSIONS-TESTS.md` (fiche des équipes, point 8 ou 11) : « dans `corrections.tsv`, ta ligne passe à tenté, jamais corrigé » ; hors de mes fichiers.
- Demande : `docs/audits/AUDIT-ULTRA.md` §13.5 : renvoyer aux formats de ce rapport (registre, rubrique `defauts-avant`, dossier `controle/<id>/a/`).
- Aucune demande Kotlin : le banc joue `<human.dir>/<a-f>` quel que soit le chemin.
