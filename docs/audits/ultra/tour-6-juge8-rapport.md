# Tour 6 — lot « juge8 » : rapport

Lot du banc seul : `scripts/bench/verifie-chantier.py` (le juge) et `scripts/bench/test_ultra_bench.py` (ses jouets). Base : `bdb777a94` (tête poussée de l'intégration, « Batch 5 opens »). Aucun fichier de l'appli, aucun test Gradle.

tests: aucun : lot du banc seul (le juge et ses jouets), aucun code de l'appli ni test Gradle changé ; banc : `python3 -I scripts/bench/test_ultra_bench.py`, `python3 -I scripts/bench/test_socle.py`, `python3 -I scripts/bench/test_portail3.py`, `bash scripts/bench/test_pre_vol.sh`

- chemin: banc › autotest du testeur humain (le juge lit les paires de ce siège) | test: BenchSelfTest#reachTagTouchesAVisibleElementInPlace

## Les trois défauts du juge (portail de « remix2 », 31 → 37 écarts, lot refusé)

1. **Plantage UTF-8.** `_git` lisait la sortie de git en texte strict. Entrées qui le tuent : un fichier de test ou un message de commit en Latin-1 (jouet), et, dans l'histoire réelle, la fusion `8c840dbd` sous `git show -U0` (git coupe l'en-tête de bloc à 80 octets au milieu d'un « é »). Sur les données de remix2 le plantage ne se reproduit pas ici (git 2.43, locale POSIX) : le jouet en donne l'entrée. Correction : `_git`, les appels à `tampon.sh` et chaque fichier ouvert par le juge lisent l'UTF-8 avec `errors="replace"`.
2. **Un rouge, plusieurs tests, un seul vert.** Un vert ne fermait qu'un rouge ouvert ; le rouge fermé n'était plus regardé. Règle : un vert qui ne ferme aucun rouge ouvert s'apparie à un rouge déjà fermé quand il nomme un de ses tests (`Classe#méthode`, ou le nom de méthode de 12 lettres et plus, comme `red_names`) qu'aucun vert n'a encore nommé ; chaque test une fois. Un vert qui a fermé le rouge par son sujet, un thème, un rejeu ou une paire nommée prend tous ses tests.
3. **`rouges-constates/` trop étroit.** Le vert devait nommer le test ET ne toucher aucun test. Règle : le vert peut toucher des tests si la fonction du test nommé est le même texte dans son parent et en lui (`kotlin_test_text` / `py_test_text`, commentaires Kotlin blanchis), et s'il change un fichier qui n'est ni un test ni un document ; s'il ne nomme pas le test, `vert.xml` (le test passé sur le vert) le montre vert. La preuve ferme aussi le rouge qui ajoute ou nomme ce test (`red_has_test`).

## Paires

- `rouge: the judge reads git output that is not UTF-8` (c945c5d91) → `vert: …` (07dbf052c). Vu rouge : « the judge reads bytes that are not UTF-8 and goes on » (UnicodeDecodeError).
- `rouge: a red of several tests pairs a green for each of its tests` (86fd28df9) → `vert: …` (67e9d885b). Vu rouge : « a second green naming another of its tests is paired ». Cassé exprès (prises non vérifiées) : rouge sur « a test already claimed stays a gap ».
- `rouge: rouges-constates regularises a green that touched tests or did not name its test` (4eba9c9d0) → `vert: …` (23119e421). Vu rouge : « … (touching another test of the file) is a pair ». Cassé exprès : corps du test non comparé → « rewrites » apparié ; `vert.xml` non lu → « novert » apparié.

## Mesures

- Juge sur la base (`--resume` de la trace tutoordre2) : 51 écarts avant, 51 après (aucun écart fermé ; 6ed6127b reste un écart, voir décision 2).
- Juge sur remix2 fusionné dans cette branche (jetable) : 57 avant, 57 après : les verts de remix2 ne nomment aucun test et le lot n'a encore aucune preuve. Essai à blanc, jamais commité, avec quatre dossiers `rouges-constates/` fabriqués (XML inventés : mécanique seulement) : 57 → 52 ; restent les 51 de la base et `203fcac90` (il ajoute lui-même `CutBoutBigTextTest#aPieceJustAboveTheScrollThreshold` : son rouge n'a jamais été commité, l'écart est honnête). Une preuve qui nommerait `a5_spanToolsWholeOnArrival` pour `b6d6ede5a` est refusée : ce vert réécrit le corps de ce test.
- Suites : `test_ultra_bench.py` OK (trois jouets neufs : `judge_reads_any_bytes`, `one_red_several_greens`, `red_seen_regularises`), `test_socle.py` 29 OK, `test_portail3.py` 77 OK, `test_pre_vol.sh` 73 verts (« tout est vert »).

## Ce que les fiches doivent dire aux correcteurs

- Une paire par test : un commit `rouge: <sujet>` peut porter plusieurs tests, mais chaque `vert:` nomme le test qu'il fait passer, `vert: Classe#méthode …` (ou reprend mot pour mot le sujet de son rouge, et il prend alors tous les tests du rouge).
- « A8 », « seat B a #A2 » ne nomment aucun test : le juge ne lit que `Classe#méthode` ou le nom entier de la méthode.
- Régulariser un lot sans réécrire l'historique : pour chaque vert resté seul, un dossier `docs/audits/ultra/rouges-constates/<sha complet du vert>/` avec
  - `rouge.txt` : `parent: <sha complet du parent du vert>` et `test: Classe#méthode` (un test qui existe dans le parent et dans le vert, que le vert ne réécrit pas) ;
  - un XML JUnit (par ex. `TEST-<paquet>.<Classe>.xml`) du test lancé sur le parent : il échoue ;
  - `vert.xml`, le test lancé sur le vert : il passe (obligatoire quand le message du vert ne nomme pas le test) ;
  - et, dans la relecture, la ligne `rouge constaté : <sha> : rejoué sur <parent> : …` que le portail demande déjà.
  Pour remix2 : `e33521f24` → `RemixLeftThumbSeatTest#a8_oneCutButtonInProject`, `b6d6ede5a` → `…#a2_stemChipsReachableWithLeftThumb`, `f678d1f08` → `…#a3_cutHintWholeNotUnderThumb`, `43a70627c` → `…#a5_spanToolsWholeOnArrival` s'il échoue vraiment sur son parent, sinon le test de `CutBoutBigTextTest` qu'il remet au vert. `203fcac90` ne se régularise pas ainsi (test ajouté par le vert lui-même).

Demande : docs/AIDE-FUSIONS-TESTS.md : au point 3 de la fiche des équipes, ajouter « Un rouge peut porter plusieurs tests ; chaque vert nomme le sien : « vert: Classe#méthode … ». « A2 » ou « #A8 » ne nomment aucun test. »
Demande : docs/AIDE-FUSIONS-TESTS.md : au point 5, ajouter « Un vert resté seul se régularise sans réécrire l'historique : rouges-constates/<sha complet du vert>/ avec rouge.txt (« parent: <sha du parent> », « test: Classe#méthode »), le XML du test qui échoue sur le parent, et vert.xml où il passe sur le vert ; le vert ne réécrit pas ce test. »
Demande : docs/audits/ultra/GABARIT-FICHES-V3.md : au point 5 de la fiche du correcteur, ajouter « Un vert par test : son message nomme le test qu'il fait passer (« vert: Classe#méthode … ») ou reprend mot pour mot le sujet de son rouge. »

## Doutes

- Le portail fait rejouer le XML du parent par la relecture (`rouge constaté : … : rejoué sur <parent>`), pas encore `vert.xml` : c'est la parole du lot tant que la relecture ne l'écrit pas.
- Le corps d'un test Kotlin à corps d'expression est lu jusqu'à la ligne suivante moins indentée : une mise en forme exotique peut le refuser (jamais l'accepter à tort : texte différent = refus).
