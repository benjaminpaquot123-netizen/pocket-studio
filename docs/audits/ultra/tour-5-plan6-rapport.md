# Tour 5, zone « plan6 » : tout ce qu'il faut au tour 6 avant le lancement (2026-11-12)

Atelier `eq-tour-5-plan6`, branche `eq/tour-5-plan6`. Rien de lancé : ni testeur, ni siège, ni CI. Décisions : `docs/decisions/tour-5-plan6.md`.

tests: aucun : documents et fiches seulement (aucun code, aucun script touché)

## Fait
- `sieges/tour-6/sieges-proposes.tsv` : les 8 sièges du § 4 (A a, A b, A c, B a, B b, B c, C a, C b), cinq langues (hi, fr, es, de, ar), appareils `phone360`, `phone`, `tablet8`, polices 100/130, A a et A b aux réglages d'origine de #27 et #117, B c et A a en `fresh=reel`, C a et C b en mission `libre`, `gestes.max=100`.
- `consignes-testeur.md` (gabarit v2, gabarit de rapport v2, partie « Round 6 »), `promesses-numerotees.tsv` (24 promesses, oui/non, écran), `oeil-neuf-gabarit.tsv` (10 questions, règle de grave), `ecrans-oeil-neuf-carte.tsv` (36 écrans de `carte-ecrans.tsv`).
- `depart.txt`, `telephone.tsv` (6 lignes), `hors-banc.tsv` (`recette_points_rouges` 4) ; B24 à B26 dans `defauts-caches.tsv`.
- « Liste de lancement » à la fin de `PLAN-TOUR-6.md` ; § 2.5 passé à « 7 écarts nommés ».

## Chemins (endroits qui lisent les mêmes données : les start.txt que fabrique-sieges.py écrit depuis sieges-proposes.tsv)
- chemin: Banc › start.txt d'un siège du tour 6 (lang, device, font, mode, main=gauche) | test: SeatOptionsParseTest#everyOption
- chemin: Banc › start.txt avec fresh=reel (A a, B c : le siège choisit son mode) | test: SeatOptionsParseTest#freshLeavesTheSettingsToThePerson
- chemin: Banc › siège arabe sur tablette à 130 % (lecture des options du siège) | test: BenchSeatOptionsTest#anArabicSeatOnATabletAt130

## Commandes du banc jouées
- `python3 -I scripts/bench/fabrique-sieges.py <scratchpad>/tour-6 --tour tour-6` (essai à blanc, le script n'a pas d'option « dry run ») → `8 sièges`.
- `python3 -I scripts/bench/fabrique-sieges.py docs/audits/ultra/sieges/tour-6 --tour tour-6` → `8 sièges` ; les 8 `start.txt` relus par `options()` et `seat_label()` de `scripts/human-act.py` (appareil, langue, police, mode, `fresh`, `main`) ; `sieges.tsv` à 11 colonnes, ids `t6-Aa-1` … `t6-Bc-4`. Fichiers générés retirés ensuite (décision : refaits au lancement).
- `python3 -I scripts/bench/tableau-de-bord.py docs/audits/ultra/sieges/tour-5 docs/audits/ultra/sieges/tour-6 --out <scratchpad>/tdb.md` → passe, tour 6 tout en « ? ».
- `python3 -I scripts/bench/defauts-caches.py` → `0 écart(s)`, 26 défauts, taux 85 % inchangé.
- `python3 -I scripts/bench/verifie-chantier.py --resume docs/audits/ultra/portail/2026-11-12-tour-5-mixwake/selftest-resume.txt` → `chantier : 45 écarts` (résumé d'un autre code ; à refaire au lancement).
- `python3 -I scripts/bench/test_defauts_caches.py` → `test_defauts_caches : vert` ; `python3 -I scripts/bench/test_socle.py` → 15 tests, OK.
- `python3 -I scripts/bench/rapport-zone.py eq/tour-5-plan6 docs/audits/ultra/tour-5-plan6-rapport.md` → accepté.

## Ce qui manque avant le lancement
rejeux6 (fusion, rejeux par étiquette, journal des tenus), retest v6 de Benjamin, juge à 7 écarts nommés, pré-vol `--ci` PRÊT, CI build et émulateur verts, commit de départ.
