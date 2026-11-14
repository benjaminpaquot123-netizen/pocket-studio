# Tour 6 — lot « juge8 » : rapport

Lot du banc seul : `scripts/bench/verifie-chantier.py` (le juge) et `scripts/bench/test_ultra_bench.py` (ses jouets). Base : `bdb777a94` (« Batch 5 opens »), puis la tête poussée de l'intégration fusionnée (`8afb9abc6`, après langues2). Aucun fichier de l'appli, aucun test Gradle.

tests: aucun : lot du banc seul (le juge et ses jouets), aucun code de l'appli ni test Gradle changé ; banc : `python3 -I scripts/bench/test_ultra_bench.py`, `python3 -I scripts/bench/test_socle.py`, `python3 -I scripts/bench/test_portail3.py`, `bash scripts/bench/test_pre_vol.sh`

- chemin: banc › autotest du testeur humain (le juge lit les paires de ce siège) | test: BenchSelfTest#reachTagTouchesAVisibleElementInPlace

## Les trois défauts du juge (portail de « remix2 », lot refusé : le juge montait de 6)

1. **Plantage UTF-8.** `_git` lisait la sortie de git en texte strict. Entrées qui le tuent : un fichier de test ou un message de commit en Latin-1 (jouet), et, dans l'histoire réelle, la fusion `8c840dbd` sous `git show -U0` (git coupe l'en-tête de bloc à 80 octets au milieu d'un « é »). Sur les données de remix2 le plantage ne se reproduit pas ici (git 2.43, locale POSIX) : le jouet en donne l'entrée. Correction : `_git`, les appels à `tampon.sh` et chaque fichier ouvert par le juge lisent l'UTF-8 avec `errors="replace"`.
2. **Un rouge, plusieurs tests, un seul vert.** Règle : un vert qui ne ferme aucun rouge ouvert s'apparie à un rouge déjà fermé quand (a) il change un fichier ni test ni document, (b) il nomme en `Classe#méthode` un test de ce rouge qu'aucun vert n'a encore nommé, (c) il est du lot du rouge : le rouge est sur la ligne de ses premiers parents, et aucune fusion « Merge team … » ne descend du rouge en étant ancêtre du vert (`lot_line`, seconde passe G3), (d) le vert qui avait fermé ce rouge est sur cette ligne. Chaque test une fois ; un vert qui a fermé le rouge par son sujet, un thème, un rejeu ou une paire nommée prend tous ses tests.
3. **`rouges-constates/` trop étroit.** Le vert devait nommer le test ET ne toucher aucun test. Règle : le vert change un fichier ni test ni document ; parmi les tests, il ne change que des fichiers Kotlin de test qui existaient, et dans eux seulement des fonctions `@Test` simples autres que le test nommé (`without_other_tests` : même texte dans le parent et dans le vert) ; s'il ne nomme pas le test, `vert.xml` (le test passé sur le vert) le montre vert. La preuve ferme aussi le rouge qui ajoute ou nomme ce test (`red_has_test`).

## Paires

- `rouge: the judge reads git output that is not UTF-8` (c945c5d91) → `vert: …` (07dbf052c). Vu rouge : UnicodeDecodeError.
- `rouge: a red of several tests pairs a green for each of its tests` (86fd28df9) → `vert: …` (67e9d885b). Vu rouge : « a second green naming another of its tests is paired ».
- `rouge: rouges-constates regularises a green that touched tests or did not name its test` (4eba9c9d0) → `vert: …` (23119e421). Vu rouge : « … (touching another test of the file) is a pair ».
- `rouge: an already closed red pairs only a green of its own lot that names Classe#méthode and changes code` (be2577d42) → `vert: …` (4dc547f45). Vu rouge : « a green without code stays a gap » (0 écart), et le nom nu apparié (4 écarts pour 5).
- `rouge: rouges-constates lets a green change only other plain tests of the test files` (045359711) → `vert: …` (bba9b2026). Vu rouge : « … (helper) makes no pair » (0 écart).
- `rouge: a red already inside a « Merge team » merge gives a later lot nothing` (768b88518) → `vert: …` (0ede90223). Vu rouge : « a red already inside a « Merge team » gives a later lot nothing » (0 écart).
- Sans paire (pas de correction) : `test: the second pass's controls of rouges-constates each have a toy case that reddens without them` (7a2c2db18) ; `test: juge8's controls each have a toy case that reddens without them` (1e1f15900) ; `perf: …` (88fb0064e : les noms du message lus avant git, même résultat, le juge repasse de plusieurs minutes à 13 s).

Contrôles cassés exprès (copie du juge, jouet relancé, remis) : prises non soustraites, sujet qui ne prend que les tests nommés, `vert.xml` non lu, `_git` strict (première passe) ; G1 : sans code, « Merge team » traversé, rouge hors de la ligne, vert fermant hors de la ligne, nom nu : chacun rouge ; G2 : texte non comparé, toute fonction annotée retirée, test nommé retiré : rouge ; M2 : vert de tests ou documents seuls, `vert.xml` compté comme XML rouge, test Python ré-indenté, `red_has_test` sans la classe, arbre lu en strict : rouge. Le « # » exigé dans `test:` reste un double verrou : `test_in` refuse la même preuve, aucun jouet ne rougit sans lui seul (son cas est gardé en écart).

## Mesures

- Juge sur la base (la tête poussée fusionnée, `--resume` de la trace langues2) : juge de l'intégration 51 écarts, juge du lot 51, sorties identiques. Le compte inclut la ligne « résumé refusé » (ce résumé est d'un autre code que cet arbre). Aucun écart fermé.
- Juge sur remix2 fusionné dans cette branche (jetable) : 57, les mêmes 6 écarts neufs qu'au portail (le rouge `4eeee030d`, les verts `e33521f24`, `b6d6ede5a`, `f678d1f08`, `43a70627c`, `203fcac90`) : les verts ne nomment aucun test et le lot n'a encore aucune preuve. Essai à blanc, jamais commité, quatre dossiers `rouges-constates/` fabriqués (XML inventés : mécanique seulement) : 57 → 52 ; reste `203fcac90` (il ajoute lui-même `CutBoutBigTextTest#aPieceJustAboveTheScrollThreshold` : son rouge n'a jamais été commité, l'écart est honnête). Une preuve qui nommerait `a5_spanToolsWholeOnArrival` pour `b6d6ede5a` est refusée.
- Attaques de la relecture (`atk/attack.py`) sur le juge du lot : A1, A2, A3, A5, A6, A8 refusées ; A7 (un test ajouté à côté) apparié, voulu ; A4 (`vert.xml` d'un vert qui ne nomme rien ferme le rouge ouvert) apparié : c'est la règle gardée, voir M1.
- Suites : `test_ultra_bench.py` OK (jouets neufs : `judge_reads_any_bytes`, `one_red_several_greens`, `closed_red_same_lot_only`, `red_seen_regularises`, `red_seen_other_tests_only`, `juge8_controls`), `test_socle.py` 29 OK, `test_portail3.py` 77 OK, `test_pre_vol.sh` 73 verts (« tout est vert »). `test_portail4.py` et `test_constate.py` n'existent pas sur cette base.

## Relecture neuve (première passe sur 63db6470 : 2 graves, corrigés)

- G1 (grave) : un vert seul s'appariait à tout rouge fermé de tout lot en citant un test non pris. Corrigé : conditions (a) à (d) ci-dessus, définies par git seul ; pas de fenêtre datée, la règle reste vraie pour un lot futur. Les trois tests non pris de l'histoire (`aTappedRhythmIsScored`, `theRestIsWordsAndWholeNumbers`, `renameAfterTheClearButton`) ne s'ouvrent plus qu'à un vert de leur lot, déjà fusionné.
- G2 (grave) : seul le corps du test nommé était comparé. Corrigé : tout le fichier de test, moins les fonctions `@Test` simples autres que le test nommé, reste le même texte ; aucun fichier de test ajouté, retiré ou Python. C'est la règle la plus stricte qui régularise les quatre verts de remix2 : trois ne touchent aucun test, `b6d6ede5a` retire des lignes de journal dans les corps de trois autres tests du siège (a3, a5, a8) ; « aucun fichier de test » ou « ajouts seulement » le laissaient en écart.
- M1 (moyen) : `vert.xml` gardé, seul chemin des verts de remix2 qui ne nomment pas leur test ; la Demande ci-dessous le fait rejouer par la relecture.
- M2 (moyen) : jouet `juge8_controls`, un cas par contrôle (voir « Paires »).
- m1 : la décision 1 dit la limite des noms entre accents graves. m2 : dit ici, la règle « un vert du même sujet prend tous les tests » est entrée dans un vert, avec son cas. m3 : dit dans « Mesures ».

## Seconde passe : G3, corrigé

- G3 (grave) : un lot parti de la pointe de `eq/k` d'un lot déjà fusionné (au lieu de l'intégration) avait le rouge et le premier vert de K sur sa ligne ; son vert seul s'appariait au test non pris (B1). Corrigé : refus quand une fusion « Merge team … » descend du rouge et est ancêtre du vert. Le contrôle de la première passe (« Merge team » sur la ligne) en est un cas, retiré. Cassé exprès : rouge sur « a green across a « Merge team » stays a gap » ; B1 dans le jouet `closed_red_same_lot_only`.
- M3 (moyen) : jouet `juge8_controls_second` : fichier de test Kotlin ajouté (vide) ou retiré (un `@Test` seul au niveau du fichier), fonction `@Before @Test` réécrite. Cassés exprès : statut autre que M accepté → rouge (« added », « removed ») ; toute fonction portant `@Test` retirée du texte comparé → rouge (« multiann »).
- Mesures de la seconde passe : base 51 écarts, 52 lignes identiques au juge de l'intégration ; remix2 fusionné 57 (les mêmes 6) ; essai à blanc 52 ; suites vertes (`test_ultra_bench.py` OK, `test_socle.py` 29 OK, `test_portail3.py` 77 OK, `test_pre_vol.sh` 73 verts).
- M1 (moyen, ouvert) : `vert.xml` reste la parole du lot ; les Demandes ci-dessous le font rejouer par la relecture et vérifier par `relecture-verdict.py`.
- M4 (moyen, ouvert) : un test nommé qui appelle une autre fonction `@Test` (C6), ou une aide plus indentée sous un autre test à corps d'expression (C8), passe par un commit de préparation ; pas plus fort qu'une aide de `src/main` affaiblie par le vert.
- M5 (moyen, ouvert, d'avant ce lot) : le vert peut changer `app/build.gradle.kts` et y exclure la classe du test (C5, accepté aussi par le juge de la base).

## Ce que les fiches doivent dire aux correcteurs

- Une paire par test : un commit `rouge: <sujet>` peut porter plusieurs tests, mais chaque `vert:` nomme le test qu'il fait passer, `vert: Classe#méthode …`, et change le code ; ou il reprend mot pour mot le sujet de son rouge, et prend alors tous les tests du rouge. Le vert d'un test suivant reste dans le même lot.
- « A8 », « seat B a #A2 » ou un nom de méthode seul ne suffisent pas : `Classe#méthode`.
- Régulariser un lot sans réécrire l'historique : pour chaque vert resté seul, un dossier `docs/audits/ultra/rouges-constates/<sha complet du vert>/` avec
  - `rouge.txt` : `parent: <sha complet du parent du vert>` et `test: Classe#méthode` (un test qui existe dans le parent et dans le vert) ;
  - un XML JUnit (par ex. `TEST-<paquet>.<Classe>.xml`) du test lancé sur le parent : il échoue ;
  - `vert.xml`, le test lancé sur le vert : il passe (obligatoire quand le message du vert ne nomme pas le test) ;
  - et, dans la relecture, la ligne `rouge constaté : …` ci-dessous.
  Le vert ne change, parmi les tests, que d'autres fonctions `@Test` simples de fichiers Kotlin existants. Pour remix2 : `e33521f24` → `RemixLeftThumbSeatTest#a8_oneCutButtonInProject`, `b6d6ede5a` → `…#a2_stemChipsReachableWithLeftThumb`, `f678d1f08` → `…#a3_cutHintWholeNotUnderThumb`, `43a70627c` → `…#a5_spanToolsWholeOnArrival` s'il échoue vraiment sur son parent, sinon le test de `CutBoutBigTextTest` qu'il remet au vert. `203fcac90` ne se régularise pas ainsi (test ajouté par le vert lui-même).

Demande : docs/AIDE-FUSIONS-TESTS.md : au point 3 de la fiche des équipes, ajouter « Un rouge peut porter plusieurs tests ; chaque vert nomme le sien, « vert: Classe#méthode … », change le code et reste dans le lot du rouge. « A2 », « #A8 » ou un nom de méthode seul ne nomment aucun test. »
Demande : docs/AIDE-FUSIONS-TESTS.md : au point 5, ajouter « Un vert resté seul se régularise sans réécrire l'historique : rouges-constates/<sha complet du vert>/ avec rouge.txt (« parent: <sha du parent> », « test: Classe#méthode »), le XML du test qui échoue sur le parent, et vert.xml où il passe sur le vert. Parmi les tests, ce vert ne change que d'autres fonctions @Test de fichiers Kotlin existants. »
Demande : docs/AIDE-FUSIONS-TESTS.md : dans la fiche du relecteur, sous « Preuve rouge apportée », ajouter « Si le dossier porte un vert.xml : « rouge constaté : <sha du vert> : rejoué sur <sha de son parent> : <ce que tu as vu> ; rejoué sur <sha du vert> : <ce que tu as vu> » : le test échoue sur le parent et passe sur le vert. »
Demande : scripts/bench/relecture-verdict.py : quand rouges-constates/<sha>/vert.xml existe, exiger dans la ligne « rouge constaté : <sha> » le second « rejoué sur <sha du vert> ».
Demande : docs/audits/ultra/GABARIT-FICHES-V3.md : au point 5 de la fiche du correcteur, ajouter « Un vert par test : son message nomme le test qu'il fait passer (« vert: Classe#méthode … ») ou reprend mot pour mot le sujet de son rouge. »

## Doutes

- `vert.xml` reste la parole du lot tant que la relecture ne le rejoue pas (Demandes ci-dessus).
- La règle ancienne d'un rouge OUVERT s'apparie toujours par un nom de méthode nu (12 lettres et plus), quelle que soit la classe : hors de ce lot, inchangée.
- Un rouge fait sur une branche annexe fusionnée par « Merge branch » n'est pas sur la ligne des premiers parents : ses tests suivants ne s'apparient plus par la règle (2) ; refus, jamais fausse paire.
