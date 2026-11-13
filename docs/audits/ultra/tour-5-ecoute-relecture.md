Relecture du lot « écoute », v2 : siège son du tour 6 et mesure après kick
Relu : 9c9ca24a7c3233729c6c00fc3367c54c7d234a8f

Portée : `git diff e2810bee HEAD -- scripts/son docs/audits/ultra/sieges/tour-6/son docs/decisions/tour-6-son.md` (16 fichiers, +1406). Relecteur neuf, rien écrit.

## Delta depuis c1a56711 (fa21409f, puis fusion 9c9ca24a)

- `fa21409f` (forme seulement) : les décisions de `docs/decisions/tour-6-son.md` sont passées à la fin de `docs/decisions/tour-5-ecoute.md`, et le fichier est supprimé. Vérifié par script : les 13 lignes de décision d'origine (8 du siège son et 5 de l'après-kick) sont toutes présentes, au caractère près. Seuls changent le titre de section (« # Siège son du tour 6… ») et le niveau du titre « Après le lot « kick » » (date retirée du titre, toujours dans chaque ligne).
- Renvois : les 4 lignes de `rapport-son.md`, `rapport-son-apres-kick.md` et `relecture.md` pointent désormais `tour-5-ecoute.md`, et rien d'autre n'y change. `grep -rn tour-6-son docs scripts` ne trouve plus rien, hors cette fiche. Dans `relecture.md`, le renvoi d'une relecture passée a été mis à jour : c'est acceptable, car le fichier cité n'existe plus.
- Fusion `9c9ca24a` : seuls `tableau-de-bord.md` (compteurs du portail) et `tour-6-morceauexport.md` (autre lot) changent sous `docs/decisions`, `sieges` et `scripts/son`. Ils viennent de l'intégration et sont hors du lot. Hors fusions, la branche n'a que `fa21409f` et `e2776eef` (cette fiche).
- Les constats ci-dessous restent valables. MINEUR 3 vise désormais la ligne du 2026-11-13 dans `docs/decisions/tour-5-ecoute.md`.

## Périmètre de la branche (relecture v2, jusqu'à c1a56711)

- `git log --no-merges --first-parent e2810bee..HEAD` : 15 commits, tous dans `scripts/son/`, `docs/audits/ultra/sieges/tour-6/son/`, `docs/decisions/tour-6-son.md` (et `4b825222`, l'ancienne fiche de relecture). Aucun ne touche `app/`, `engine/`, `scripts/bench/`, `scripts/bentest/`.
- `git log --no-merges e2810bee..HEAD -- app engine scripts/bench scripts/bentest` : uniquement des commits de l'intégration (rouge/vert T16, remixdecoupe, ci2, juge, lot kick `bb5ad266`…`ddc8cf11`), arrivés par les fusions. Aucun n'est dans la liste du premier parent.
- Pas de son commité : `git diff --diff-filter=A --name-only e2810bee HEAD | grep -iE '\.(wav|f32|m4a)$'` est vide (les `.wav` de `git ls-files` sont les échantillons des projets de test, déjà là avant).

## Méthode vérifiée

- `haut_parleur.sh` : ajoute `AT -1` puis `C 210 1 0 0` avant la ligne `RENDER` (même forme que `song-bench.sh --kit3`, ligne 107 ; `AT` est lu par `ps_render.cpp:175`), échoue si la ligne manque. Rendu sans 210 comparé par SHA-256 au `hash/` du banc d'avant : `build/son6c/hp/export.tsv` = 37 lignes, toutes « identique ». Reprise par style correcte (json écrit en dernier, par `mv`).
- Commande 210 = `SetSpeakerOutput` (`Engine.h:241`, `static_assert`). Outils de `build/son6c/tools` ; `845c8beb` contient `5957fac5` et est ancêtre de HEAD.
- `son6_kick.py` reproduit `ps_measure --punch` (`ps_measure.cpp` 466-620) : enveloppe RMS 5 ms au pas de 1 ms, montée > 4× (12 dB) sur le minimum des 30 ms, > 5 % du max, 80 ms d'écart ; puissance du mono, 40 ms contre [o−120, o−20] ms, sommés sur les coups de la partie, coup gardé si toute la fenêtre est dans la partie. Seul écart : fin de la dernière partie à la longueur du fichier, pas à `SONG frames=` (voir MINEUR 5).
- `--hits-from` : kick rendu seul depuis les `cmds` du banc (sans 210). Vérifié : `build/son6/kick.tsv` et `build/son6c/hp/kick.tsv` ont, triés, les mêmes (style, partie, coups) sur les 122 lignes. Les coups sont donc les mêmes avant et après.
- `avant_apres_kick.py build/son6 build/son6c/hp` relancé : sortie identique à l'octet à `kick-avant-apres.tsv`. `build/son6c/live/direct.tsv` identique à `kick-direct.tsv`.
- Direct : `*.hp.pslive` = `*.pslive` + `C 210 1 0 0` en ligne 4 ; les 12 journaux finissent par « OK: 0 flags over 150 s » et « ALLOC news=0 deletes=0 ».

## BLOQUANT

Aucun.

## GRAVE

Aucun.

## MOYEN

1. **Conseil 4 plus fort que la preuve.** « Leur kick reste au-dessus de 5 dB au téléphone, il y a de la marge » est faux pour l'acidcore : 4,65 au téléphone, et c'est le seul style dont le coup recule avec le lot (6,55 → 4,65). L'acid house est à 5,06 au téléphone et déjà à 2,70 sur le pire cas, donc sous les 3 dB visés. « Réduire d'un tiers » n'est pas chiffré dans son effet sur le coup. À écrire : « au-dessus de 4,6 dB au téléphone (acidcore 4,7, acid house 5,1). L'acid house est déjà à 2,7 dB sur le pire cas : à exclure, ou à vérifier par la même mesure (coup au téléphone ≥ 3 dB et pire cas sans recul) ».

## MINEUR

1. « Les plus gros gains au téléphone » : l'amapiano (+4,63) manque alors que le trap (+4,55) y est. Le tableau n'est pas trié par gain (lo-fi +5,89 en tête, puis deep house +5,20, minimal +4,92, drill +4,77, afro house +4,71, amapiano +4,63, trap +4,55).
2. « La charge du fil audio en direct n'a pas été mesurée ici » est trop prudent. Les journaux `ps_live` la donnent sur la machine Linux : moyenne de 7,3 à 11,5 %, à ±0,2 point près avec ou sans haut-parleur. Ce ne sont pas des chiffres de téléphone. Les `over`/`glitches` de l'hôte sont du même ordre éteint et allumé.
3. Décisions (désormais dans `docs/decisions/tour-5-ecoute.md`), « l'appli n'a changé que des écrans entre les deux » : `Headphones.kt` (route du haut-parleur), `RhythmViewModel.kt` (prise), `StemBoardRouter.kt` et `SongGuide.kt` (texte) ont aussi changé. Aucun ne touche les recettes ni l'export. L'égalité à l'octet ne prouve que le côté moteur. Écrire « rien qui fabrique ou exporte les morceaux ».
4. `rapport-son.md` : les points N4, N5 et N6 de `relecture.md` ne sont pas appliqués. Résumé ligne 1, « ni en écoute en direct » sans « (8 styles) » ; « 0,2 à 2,4 LU » sans « face aux cibles mesurées » ; phrase « ceux à écouter plus phonk… ».
5. `son6_kick.py` coupe la dernière partie à la longueur du fichier, alors que `ps_measure` borne à `SONG frames=`. Les coups de la traîne peuvent compter. L'écart est sans effet constaté : l'égalité sur 122 parties est notée en décision.
6. Conseils 1 et 2 : « +2 dB » sur le kick ou le clic « devrait suffire » est une estimation non mesurée. Elle est bien présentée comme non codée, avec son critère de contrôle. On pourrait ajouter « estimé ».

## Recalculé depuis les données (`build/son6`, `build/son6c/hp`, `build/son6c/live`)

- Téléphone : médiane 1,83 → 5,27 ; sous 3 dB 28 → 3 (gabber, jungle, phonk) ; sous 2 dB 23 → 0.
- Pire cas : médiane 0,96 → 3,93 ; sous 3 dB 26 → 12 ; sous 2 dB 24 → 4 (dnb 1,56, dubstep 1,93, jungle 1,01, phonk 1,28).
- Pleine bande : médiane 3,87 → 4,39 ; sous 3 dB 7 → 2 ; sous 2 dB 0 → 1 (phonk 1,84).
- Recul au téléphone et sur le pire cas : acidcore seul (6,55 → 4,65 ; 6,35 → 5,38). Gabber 2,79 → 2,80.
- Manques : phonk 0,77 et 1,72, jungle 0,68 et 1,99, gabber 0,20, dnb 1,44, dubstep 1,07. Export pleine bande : phonk 2,30, jungle 2,13.
- Exports : 37/37 identiques. Écrêtés 0 ; `non_finite` 0 ; silences et sauts 0/37 (`son6.tsv`). Crête après de −1,20 à −1,16 dBTP.
- Niveau : LUFS-I médiane +0,12 (de −0,19 à +0,57, jungle) ; Drop de −0,17 à +0,35.
- Bas-médium : médiane +2,19 ; Detroit +6,07, acid techno +5,83, acid house +5,59, acidcore +3,99 ; dnb +1,06, dubstep +0,71. Médium : +0,36.
- Basse et lead face au morceau : médiane −0,58 (de −2,68 afro house à +0,63) ; boom bap −1,82, tech house −1,81, house −1,63, deep house −1,60. Kick face au morceau : +8,88.
- Temps de rendu : rapport médian 1,006 (de 0,816 à 1,156).
- Direct : les 6 lignes du tableau, toutes conformes (phonk −0,18 → 3,13 et −0,51 → 2,10 ; dnb 2,52 → 6,56 et 1,29 → 4,66 ; house 5,35 → 9,60…).
- Avant (`rapport-son.md`) : 9 styles ≥ 3 dB au téléphone ; échantillon de 5 lignes du tableau des plus faibles conforme.

Verdict : 0 bloquant, 0 grave
