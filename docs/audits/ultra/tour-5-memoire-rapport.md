# Lot 2 « La mémoire » : rapport de l'auteur (2026-11-09)

Atelier `memoire`, branche `eq/tour-5-memoire`, au-dessus de 7013d46. Un seul auteur, rien n'est poussé. À relire par un relecteur neuf avant fusion.

## Partie 1 : les quatre « moyen » de la relecture du lot 1

1. **Câblage de `selftest.sh` vérifié par son comportement** (`test_ultra_bench.py::portal`, commit ae5ff0d). Le vrai `selftest.sh` tourne dans un dépôt jouet avec un faux Gradle. Mutations essayées à la main, chacune rend la suite rouge : liste attendue écrite en dur, verdict rouge ignoré, classes attendues non passées au verdict, sauts connus non passés, lancements Gradle ratés non passés, juge appelé sans résumé. Pas de paire rouge/vert : ces tests sont verts sur le code existant, c'est la mutation qui les prouve.
2. **Appel du juge dans `marque-corrige.sh`** (`test_ultra_bench.py::mark_fixed`, même commit). Mutation « appel sans `--resume` » : suite rouge.
3. **`selftest-sauts-connus.txt` borné** : seule une entrée `Classe#drive` d'une classe qui hérite de `HumanTester` est acceptée, sinon « saut connu refusé » et selftest rouge. rouge 609f50b / vert 01fcb3c (`test_ultra_bench.py::known_skips_bounded`). Mutation : un vrai test ajouté à la vraie liste est refusé.
4. **Selftest complet, une fois, en fin de lot** : voir « Commandes lancées ».

## Partie 2 : le lot

| Ligne | Ce qui a changé | Preuve |
|---|---|---|
| A.11 | T4-26 : **corrigé après relecture, voir « Après relecture »**. La première garde mesurait le faux moteur du test, pas l'appli : la ligne est repassée à « non-tranché », sans `@KnownDefect`. Gardes vertes normales dans `Tour4AppProofTest`. Clé `tab_simple_Clavier\|record_start\|rien` de `base.tsv` : « nouveau », plus « confirmé ». **Aucun émulateur ici : `JourneyTest` n'a pas été rejoué**, la ligne le dit. | rouge 67f2c82 / vert 4346479, puis rouge 7e34de9 / vert 1b8dd12 |
| A.14 | L'écran lu par les sièges finit par « (banc : les pads partent tout de suite, sans grille) » quand Pad Live est affiché. Même limite écrite dans `sieges/tour-5/hors-banc.tsv`, l'en-tête de `verdicts.tsv` et AUDIT-ULTRA § 12.3. Le juge affiche « verdict pad-timing sans preuve hors banc ». | écran : rouge c8c2f82 / vert 859700a ; juge : rouge 5f0c5f8 / vert 12d6cca |
| A.2 | T5-Ca-32 : « banc », « corrigé » (0f5efde) par `marque-corrige.sh`. « recette tel-17 » portée par T5-PADRETRIG seul (retirée de T5-Ca-32 et de T5-DEFI-ECOUTE). Nouvel écart du juge : « appli dont la preuve n'est ni @KnownDefect ni un essai ❌ daté ». | rouge 4587080 / vert 7a6864d |
| A.6 | Le juge lit `retours-benjamin.md` : 16 essais téléphone et 3 essais tablette marqués, tous du 2026-11-08. Chaque ligne qui cite un point porte « RECETTE tel-NN ✅/❌ appareil date ». 22 annotations manquantes au départ, 0 après. T4-16 disait « tablette+téléphone » pour tel-09 : corrigé en « téléphone », seul écrit dans les notes. tel-07 ✅ est noté sur T5-Cc-30 et T5-Ec-61 avec « l'essai ne dit pas WAV ou MIDI » : ces deux lignes restent « appli », à réconcilier par l'essai § 8 n° 4. | rouge 0a38188 / vert f9a7b86 |
| A.5 | T5-Fc-31 repassé « non-tranché » : essai téléphone § 8 n° 3 à faire, question « voulu ? ». PROGRESS.md, `retours-benjamin.md` (tel-16) et PLAN-RESTE R.1 disent la même chose. `recette.json` non touché. | données seulement (e1881c2) |

Aucune ligne de code de l'appli (`app/src/main`) n'a été touchée.

## Commandes lancées, résultats vus

- `python3 -I scripts/bench/test_ultra_bench.py` : « ultra bench scripts: OK » après chaque vert ; rouge par assertion à chaque rouge.
- Gradle, toujours par `scripts/bench/gradle-file.sh`, ciblé : `Tour4AppProofTest` 7 tests verts avec la garde (9 après relecture) ; `BenchPlaceAndPadLiveTest` : le nouveau test rouge par assertion avant, 5 verts et `drive` sauté après.
- **Un `@KnownDefect` rendu vert à la main rend le selftest rouge** : l'assertion de `ChallengeControllerTest#touchesTooCloseAreSaidTooFast` neutralisée, classe jouée seule : Gradle sort 1, « T5-DEFI-TENDANCE no longer fails », `selftest-verdict.py` : « 7 verts, 1 rouges », sortie 1. Témoin sans modification : « 8 verts, 0 rouges », sortie 0. Fichier remis par `git checkout -- <fichier>`. Limite : c'est le verdict du selftest lancé sur une classe, pas un selftest complet rouge ; le lien verdict rouge › selftest rouge est tenu par `portal`.
- `bash scripts/bench/selftest.sh <lot>` une fois, sur 3580112, arbre propre : **« Selftest vert : 133 verts, 0 rouges, 0 en quarantaine, 41 sautés »**. Les 41 sautés sont les `drive` des sièges (30 cas de `BenchAmeneesProfilesTest#drive`, 11 autres classes), tous dans la liste bornée.
- `python3 -I scripts/bench/verifie-chantier.py --resume <lot>/selftest-resume.txt` : **23 écarts** (24 avant le lot).

## Ce qui reste (23 écarts, aucun effacé en affaiblissant une règle)

- 11 lignes « banc » sans preuve sans banc : T4-19, T4-16, T4-17, T5-Dc-4, T5-Fb-73, T5-Ac-61, T5-Db-16, T5-Fc-8, T5-Cc-41b, T5-Ab-14, T5-Ab-27. Il faut écrire un test de l'appli hors banc pour chacune.
- 2 lignes « appli » sans preuve : T4-01 (tel-12 est ✅ avec un défaut partiel, pas un ❌) et T5-TEL10 (« voulu », essai ✅). Le seul écart fermé par le lot est T5-Fc-31 (« non-tranché »).
- 6 paires rouge/vert historiques (0b1cbbc, dcc3367, 87b6693, 8f36fa3, df794a9, d503254).
- 4 lignes cochées du journal sans preuve (4.0 deux fois, 4.2, 4.3).
- Les trois nouvelles règles n'ajoutent aucun écart sur les données d'aujourd'hui. « pad-timing » en voyait un (T5-TEL10) avant l'annotation de son essai daté.
- À faire par Benjamin, plus tard, dans une seule demande : essais § 8 n° 3 (démo, T5-Fc-31), n° 4 (export MIDI, T5-Ec-61) et n° 5 (T4-26).
- `grep -c 'tab_simple_Clavier|record_start|rien' base.tsv` donne 2 et non 1 : la clé plus ancienne `synth/tab_simple_Clavier|record_start|rien` (tour 3) contient le même texte. La clé exacte, en début de ligne, n'y est qu'une fois.
- A.16 (les pads du banc suivent la grille) n'est pas dans ce lot : la limite est seulement écrite.

## Après relecture (2026-11-10)

Relecture lue : `tour-5-memoire-relecture.md` (f13a4d5), non modifiée. Les deux graves et les deux moyens sont corrigés.

- **Grave 1, T4-26.** Le faux moteur de `Tour4AppProofTest` ne faisait jamais le changement de partie armé. Il le fait maintenant à la barre suivante, comme le vrai (rien de changé dans `app/src/main`). Résultat : **le défaut n'est pas reproduit**. À la fin de la prise, l'appli arme le déplacement de l'écran, le moteur change à la barre, l'écran arrive sur la partie qui porte les notes : au plus une mesure d'attente (550 ms vues). d3eccff couvre donc ce chemin sous Robolectric. T4-26 est « non-tranché » : l'échec du test de fumée sur émulateur (CI 243) reste inexpliqué, à diagnostiquer à l'étape 2. `@KnownDefect` retiré, la garde est un test vert normal. Paire rouge 7e34de9 / vert 1b8dd12.
- **Début de la prise, vu avec le moteur corrigé.** Au départ : rien d'armé côté moteur (pending=0), le son est armé pour rejoindre la partie affichée à la barre suivante. Prise courte avant la barre : 3 notes dans la partie entendue, l'écran y va. Prise après la barre : 3 notes dans la partie affichée. Prise à cheval sur la barre : les 3 notes sont gardées, 1 dans la partie entendue avant et 2 dans la partie affichée. Ce partage est noté dans la ligne, pas tranché : aucune règle écrite ne dit s'il est voulu.
- **Grave 2.** « sorti par Retour » remplacé partout par « a quitté la liste sans choisir de style (consigne tel-16 : Retour ou croix) », y compris une ligne du journal antérieure au lot. Seul le fichier de relecture cite encore l'ancienne phrase.
- **Moyen 3, règle inverse.** Toute annotation « RECETTE tel-N » de `verdicts.tsv` doit retrouver son essai (numéro, ✅/❌, appareil, date) dans `retours-benjamin.md`, sinon écart. Rouge 6132c9f / vert 4a5d273. Elle a d'abord signalé une annotation vraie (T5-Ed-40, tablette, tel-11, « Résultat retenu : ❌ » hors d'une ronde) : corrigé en apprenant cette forme au lecteur des notes, pas en affaiblissant la règle (rouge afb811a / vert 70f6d44). L'annotation inventée de la relecture donne bien un écart.
- **Moyen 4, mutations.** Cas ajoutés (c5c3ef4, fc32a7f) : appareil ignoré (J13), date ignorée (J14), essai ❌ sans numéro, essai daté sans marque ou sans numéro, clé `pad_`, borne et lignes « appli » de la règle pad, formes du lecteur des notes. Chaque mutation rejouée rend la suite rouge.
- **Mineur.** Reste « tel-17 » sans date retiré de la note de T5-Ca-32.
- **`docs/DECISIONS.md`** : une ligne ajoutée, parce que la ligne T4-26 du même jour était devenue fausse ; elle le dit et la remplace.

Résultats vus à la fin : suite Python « ultra bench scripts: OK » ; `Tour4AppProofTest` 9 tests verts ; juge : **23 écarts**, les mêmes que ci-dessus.

Limites :

- Pas d'émulateur : `JourneyTest` non rejoué, l'échec CI 243 non expliqué.
- Le selftest complet n'a pas été relancé (seuls `Tour4AppProofTest` et les scripts Python ont changé). Son résumé date de 3580112, avant les nouveaux noms de tests de `Tour4AppProofTest`.
- La règle inverse se tait si `retours-benjamin.md` est absent.
- Deux mutations survivent, équivalentes sur les cas possibles : la coupe par segment dans `records_trial` et « un essai de n'importe quel numéro » dans `annotation_gaps`.
- Mineur 6 de la relecture (empreinte du portail, N7) : pas traité.
- Essais de Benjamin § 8 n° 3, 4 et 5 : toujours à faire.
