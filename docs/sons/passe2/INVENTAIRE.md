# Inventaire de la deuxième passe

Rejouer : `python3 -I scripts/sons/passe2/reunit.py` (lit les CSV des branches `eq/passe2-rarN` par `git show`,
aucun son). Sorties dans `docs/sons/passe2/tout/` : une table par type, `controle.csv`, `accuracy.csv` (table de
précision, pas une table de sons), `passe1_comptes.csv` et `chiffres.md` (tous les tableaux détaillés, écrits par le script).

État : **9 RAR sur 12** réunis (1, 2, 3, 4, 5, 6, 8, 10, 12). Les RAR 7, 9 et 11 viendront ensuite.

Contrôles passés : 0 clé en double, 0 cellule d'erreur, lignes des tables = lignes du tableau de contrôle
(rows + kit_rows) pour chaque pack et chaque RAR.

## Fichiers et lignes par RAR et par pack

| RAR | pack | style du pack | audio | MIDI | lignes | lignes de kit |
|---|---|---|---|---|---|---|
| 1 | p1 | plusieurs (multi) | 1388 | 0 | 1388 | 0 |
| 2 | p1 | drift-phonk | 645 | 140 | 785 | 0 |
| 3 | p1 | techno | 285 | 85 | 370 | 0 |
| 4 | p1 | lofi | 226 | 24 | 250 | 0 |
| 5 | p1 | techno | 465 | 70 | 535 | 0 |
| 6 | p1 | amapiano | 487 | 136 | 623 | 0 |
| 8 | p1 | dubstep | 700 | 20 | 720 | 0 |
| 8 | p2 | afro-house | 338 | 60 | 398 | 6 |
| 8 | p3 | sans étiquette | 100 | 0 | 100 | 0 |
| 10 | p1 | dnb | 341 | 35 | 376 | 0 |
| 10 | p2 | house | 254 | 104 | 358 | 26 |
| 10 | p3 | hardstyle | 206 | 85 | 291 | 26 |
| 12 | p1 | edm | 256 | 0 | 256 | 0 |
| 12 | p2 | house | 424 | 70 | 494 | 0 |
| 12 | p3 | rave | 36 | 0 | 36 | 0 |
| **total** | | | **6151** | **829** | **6980** | **58** |

Une ligne par fichier ; les « lignes de kit » sont en plus : une ligne par kit de construction (pas un fichier).
Total des tables : 7038 lignes.

## Par type

| type | lignes | où |
|---|---|---|
| coups de batterie (drum_oneshot) | 1931 | tous ; la moitié vient du RAR 1 (919) |
| boucles de batterie (drum_loop) | 1523 | tous |
| boucles mélodiques (melodic_loop) | 808 | tous sauf le RAR 1 |
| notes mélodiques seules (oneshot_melodic) | 750 | surtout RAR 8 (303) et RAR 10 (199) |
| voix (vocal) | 175 | surtout amapiano (99) |
| effets (fx) | 627 | surtout RAR 12 (280, dont 254 edm) et RAR 8 (181) |
| kits de construction (kit) | 58 | RAR 8 (6) et RAR 10 (52) |
| pistes de kit (kit_stem) | 337 | RAR 8 (48) et RAR 10 (289) |
| MIDI | 829 | tous sauf RAR 1 |

### Croisé par style (détail complet dans `tout/chiffres.md`)

| style | coups | boucles batterie | boucles mélo | notes mélo | voix | effets | kits | pistes de kit | MIDI |
|---|---|---|---|---|---|---|---|---|---|
| multi (RAR 1) | 869 | 310 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| techno | 130 | 346 | 179 | 60 | 20 | 40 | 0 | 0 | 155 |
| house | 80 | 213 | 78 | 129 | 21 | 23 | 26 | 162 | 175 |
| drift-phonk | 212 | 150 | 140 | 98 | 10 | 34 | 0 | 0 | 140 |
| dubstep | 200 | 85 | 80 | 303 | 0 | 59 | 0 | 0 | 20 |
| amapiano | 58 | 131 | 142 | 25 | 99 | 27 | 0 | 0 | 131 |
| dnb | 135 | 109 | 44 | 29 | 6 | 37 | 0 | 0 | 36 |
| afro-house | 100 | 77 | 78 | 0 | 10 | 25 | 6 | 48 | 60 |
| lofi | 117 | 53 | 66 | 24 | 0 | 19 | 0 | 0 | 27 |
| hardstyle | 0 | 0 | 0 | 79 | 0 | 0 | 26 | 127 | 85 |
| edm | 2 | 0 | 0 | 1 | 0 | 254 | 0 | 0 | 0 |
| rave | 22 | 0 | 0 | 0 | 9 | 5 | 0 | 0 | 0 |
| sans étiquette | 0 | 0 | 1 | 0 | 0 | 99 | 0 | 0 | 0 |
| autres (hiphop, trap, ambient, disco, funk, rnb) | 6 | 49 | 0 | 2 | 0 | 5 | 0 | 0 | 0 |

Les styles « autres » viennent du nom du dossier du fichier, pas du pack.

### Coups de batterie par famille

| famille | n | | famille | n |
|---|---|---|---|---|
| kick | 427 | | ride | 75 |
| snare | 324 | | cowbell | 67 |
| perc | 254 | | tom | 54 |
| clap | 228 | | crash | 40 |
| hat_closed | 194 | | kick_808 | 23 |
| hat_open | 132 | | cymbal | 20 |
| shaker | 76 | | snap | 12 |
| | | | rim | 5 |

« kick » compte 6 couches d'attaque et 1 couche de queue ; « snare » 1 couche d'attaque ; « hat_closed » 1 couche
d'attaque ; « perc » regroupe perc, perc_high, perc_mid et perc_low (5 chacun).

## Fichiers sautés (non lus, par extension)

- RAR 8 p1 : 35 presets de synthé (.fxp) et 1 .pdf.
- RAR 10 p1 : 1 .txt.
- RAR 12 p3 : 1 .pdf et 1 pack de presets (.serumpack, c'est l'archive imbriquée ci-dessous).

## Archives imbriquées

- RAR 12 p3 : 1 archive imbriquée, non ouverte. Elle contient les 22 fichiers audio que la première passe avait lus.

## Packs sans étiquette de style

- RAR 8 p3 (100 fichiers) : aucun style posé. Seules les lignes qui ont un style par leur propre dossier en ont un.

## À savoir (déjà connu du chef, pas une erreur)

- `pack_line_ok` = NO sur le RAR 3 (1 ligne) et le RAR 10 p1 (2 lignes) : fausse alerte du lanceur ; les tables
  donnent le même nombre de lignes au style du pack que `tout.py`.
- « posé sur N lignes » ne compte que les lignes qui reçoivent le style du pack, pas celles qui ont déjà un style par leur dossier.

## Comparaison avec la première passe

Source de la première passe : branche `eq/mine-or-rarN-tout` (RAR 10 : `eq/mine-or-rar10`, pas de `-tout`).
Compte : lignes de `rarN.csv` + `rarN-tout.csv` sans les types doc, junk et preset, plus `rarN-midi.csv` quand
`-tout` n'a pas de MIDI ; `rarN-drums.csv` est un sous-ensemble de `-tout`, pas compté.

| RAR | 1re passe | 2e passe | écart | explication |
|---|---|---|---|---|
| 1 | 1388 | 1388 | 0 | — |
| 2 | 785 | 785 | 0 | — |
| 3 | 370 | 370 | 0 | — |
| 4 | 250 | 250 | 0 | — |
| 5 | 535 | 535 | 0 | — |
| 6 | 623 | 623 | 0 | — |
| 7 | 2026 | à venir | | |
| 8 | 1218 | 1218 | 0 | les 35 presets et le .pdf sont exclus des deux côtés |
| 9 | 1325 | à venir | | |
| 10 | 1025 | 1025 | 0 | le .txt est exclu des deux côtés |
| 11 | 1903 | à venir | | |
| 12 | 808 | 786 | −22 | les 22 fichiers audio de l'archive imbriquée du pack 3, lus en première passe, pas en deuxième |

Coups de batterie par RAR (le total de fichiers ne bouge pas, seul le type change) :

| RAR | 1 | 2 | 3 | 4 | 5 | 6 | 8 | 10 | 12 |
|---|---|---|---|---|---|---|---|---|---|
| 1re passe | 921 | 210 | 50 | 67 | 80 | 53 | 300 | 141 | 116 |
| 2e passe | 919 | 213 | 50 | 67 | 80 | 58 | 300 | 142 | 102 |

- RAR 1 (−2), 2 (+3), 6 (+5), 10 (+1) : fichiers reclassés entre coup de batterie et autre type (même total de fichiers).
- RAR 12 (−14) : une partie des 22 fichiers de l'archive imbriquée étaient des coups ; le reste de l'écart est pas expliqué
  fichier par fichier (les clés des deux passes ne se correspondent pas).
