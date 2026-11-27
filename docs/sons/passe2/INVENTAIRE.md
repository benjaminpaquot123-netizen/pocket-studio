# Inventaire de la deuxième passe

Rejouer : `python3 -I scripts/sons/passe2/reunit.py` (lit les CSV des branches `eq/passe2-rarN` par `git show`,
aucun son). Sorties dans `docs/sons/passe2/tout/` : une table par type, `controle.csv`, `accuracy.csv` (table de
précision, pas une table de sons), `passe1_comptes.csv` et `chiffres.md` (tous les tableaux détaillés, écrits par le script).

État : **12 RAR sur 12** réunis.

Contrôles passés : 0 clé en double, 0 cellule d'erreur, lignes des tables = lignes du tableau de contrôle
(rows + kit_rows) pour chaque pack et chaque RAR.

## Fichiers et lignes par RAR et par pack

| RAR | pack | style du pack | audio | MIDI | lignes | lignes de kit | archives imbriquées (lues le 2026-11-27) |
|---|---|---|---|---|---|---|---|
| 1 | p1 | plusieurs (multi) | 1388 | 0 | 1388 | 0 | 0 |
| 2 | p1 | drift-phonk | 645 | 140 | 785 | 0 | 0 |
| 3 | p1 | techno | 285 | 85 | 370 | 0 | 0 |
| 4 | p1 | lofi | 226 | 24 | 250 | 0 | 0 |
| 5 | p1 | techno | 465 | 70 | 535 | 0 | 0 |
| 6 | p1 | amapiano | 487 | 136 | 623 | 0 | 0 |
| 7 | p1 | sans étiquette | 0 | 383 | 383 | 0 | 0 |
| 7 | p2 | sans étiquette | 1413 | 48 | 1461 | 5 | 0 |
| 7 | p3 | sans étiquette | 130 | 52 | 182 | 3 | 0 |
| 8 | p1 | dubstep | 700 | 20 | 720 | 0 | 0 |
| 8 | p2 | afro-house | 338 | 60 | 398 | 6 | 0 |
| 8 | p3 | sans étiquette | 100 | 0 | 100 | 0 | 0 |
| 9 | p1 | synthwave (forcé) | 234 | 98 | 332 | 26 | 0 |
| 9 | p2 | trap | 251 | 112 | 363 | 25 | 0 |
| 9 | p3 | dubstep | 610 | 20 | 630 | 0 | 0 |
| 10 | p1 | dnb | 341 | 35 | 376 | 0 | 0 |
| 10 | p2 | house | 254 | 104 | 358 | 26 | 0 |
| 10 | p3 | hardstyle | 206 | 85 | 291 | 26 | 0 |
| 11 | p1 | sans étiquette | 421 | 0 | 421 | 30 | 0 |
| 11 | p2 | dnb | 598 | 149 | 747 | 22 | 1 |
| 11 | p3 | rave | 611 | 126 | 737 | 22 | 1 (+2 lignes) |
| 12 | p1 | edm | 256 | 0 | 256 | 0 | 0 |
| 12 | p2 | house | 424 | 70 | 494 | 0 | 0 |
| 12 | p3 | rave | 58 | 0 | 58 | 0 | 1 (+22 lignes) |
| **total** | | | **10441** | **1817** | **12258** | **191** | **3** |

Une ligne par fichier ; les « lignes de kit » sont en plus : une ligne par kit de construction (pas un fichier).
Total des tables : 12425 lignes avant les archives imbriquées, **12449** après (voir plus bas).

Archives imbriquées, par RAR : RAR 11 → 2, RAR 12 → 1, tous les autres → 0. Elles sont maintenant lues (section dédiée) : +24 lignes.

## Par type

| type | lignes | où |
|---|---|---|
| coups de batterie (drum_oneshot) | 2426 | tous sauf le RAR 7 ; 919 viennent du RAR 1 |
| boucles de batterie (drum_loop) | 1858 | tous sauf le RAR 7 |
| boucles mélodiques (melodic_loop) | 1587 | tous sauf le RAR 1 ; 605 au RAR 7 |
| notes mélodiques seules (oneshot_melodic) | 1532 | surtout RAR 9 (421), RAR 8 (303), RAR 11 (291) |
| voix (vocal) | 844 | surtout RAR 7 (599) et amapiano (99) |
| effets (fx) | 772 | surtout RAR 12 (280) et RAR 8 (181) |
| kits de construction (kit) | 191 | RAR 7 (8), 8 (6), 9 (51), 10 (52), 11 (74) |
| pistes de kit (kit_stem) | 1398 | RAR 7 (269), 8 (48), 9 (280), 10 (289), 11 (512) |
| MIDI | 1817 | tous sauf RAR 1 |

### Croisé par style (détail complet dans `tout/chiffres.md`)

| style | coups | boucles batterie | boucles mélo | notes mélo | voix | effets | kits | pistes de kit | MIDI |
|---|---|---|---|---|---|---|---|---|---|
| sans étiquette | 0 | 0 | 606 | 223 | 597 | 99 | 34 | 400 | 81 |
| dubstep | 390 | 165 | 150 | 523 | 2 | 109 | 0 | 0 | 80 |
| dnb | 295 | 224 | 98 | 69 | 36 | 88 | 22 | 148 | 216 |
| multi (RAR 1) | 869 | 310 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| techno | 130 | 346 | 179 | 60 | 20 | 40 | 0 | 0 | 192 |
| house | 80 | 213 | 78 | 129 | 21 | 23 | 26 | 162 | 210 |
| drift-phonk | 212 | 150 | 140 | 98 | 10 | 34 | 0 | 0 | 140 |
| rave | 157 | 140 | 50 | 45 | 49 | 45 | 22 | 149 | 126 |
| amapiano | 58 | 131 | 142 | 25 | 99 | 27 | 0 | 0 | 131 |
| trap | 1 | 22 | 0 | 124 | 0 | 6 | 26 | 163 | 172 |
| afro-house | 100 | 77 | 78 | 0 | 10 | 25 | 6 | 48 | 107 |
| lofi | 117 | 53 | 66 | 39 | 0 | 19 | 1 | 25 | 78 |
| edm | 2 | 0 | 0 | 24 | 0 | 254 | 1 | 27 | 68 |
| synthwave | 0 | 0 | 0 | 92 | 0 | 0 | 26 | 142 | 98 |
| hardstyle | 10 | 0 | 0 | 79 | 0 | 0 | 26 | 127 | 85 |
| trance | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 33 |
| autres (hiphop, ambient, funk, disco, rnb) | 5 | 27 | 0 | 2 | 0 | 3 | 1 | 7 | 0 |

Les styles qui ne sont pas celui du pack (trance, autres, et une partie des lignes de lofi, edm, hardstyle…)
viennent du nom du dossier du fichier.

### Coups de batterie par famille

Table pour la batterie synthétisée : `tout/drum_oneshot.csv` (une ligne par coup, colonne `style`), médianes
par famille dans `tout/chiffres.md`.

| famille | n | | famille | n |
|---|---|---|---|---|
| kick | 512 | | ride | 105 |
| snare | 409 | | cowbell | 67 |
| perc | 344 | | crash | 65 |
| clap | 298 | | tom | 54 |
| hat_closed | 246 | | kick_808 | 23 |
| hat_open | 160 | | cymbal | 20 |
| shaker | 106 | | snap | 12 |
| | | | rim | 5 |

« perc » regroupe perc, perc_high, perc_mid et perc_low.

## Fichiers sautés (non lus, par extension)

- RAR 7 p2 : 2 .ini et 11 .txt.
- RAR 8 p1 : 35 presets de synthé (.fxp) et 1 .pdf.
- RAR 9 p3 : 30 presets de synthé (.fxp) et 1 .pdf.
- RAR 10 p1 : 1 .txt.
- RAR 11 p2 : 1 .pdf, 1 pack de presets (.serumpack, l'archive imbriquée) et 30 presets (.serumpreset).
- RAR 11 p3 : 1 .pdf et 1 pack de presets (.serumpack, l'archive imbriquée).
- RAR 12 p3 : 1 .pdf et 1 pack de presets (.serumpack, l'archive imbriquée).

## Archives imbriquées (lues le 2026-11-27)

Ouvertes hors dépôt, mesurées par `scripts/sons/tout.py` sans modification, sons effacés ensuite. Lignes ajoutées par
`scripts/sons/passe2/fin/tables.py` depuis `docs/sons/passe2/fin/imbriquees/` (pack p3, clé `rN-p3-<id>`).
- RAR 12 p3 : 22 fichiers audio (13 coups, 8 sons mélodiques, 1 voix). Écart avec la première passe : 0.
- RAR 11 p3 : 2 sons mélodiques. La première passe ne les avait pas lus non plus : la 2e passe a donc 2 lignes de plus.
- RAR 11 p2 : 30 presets, aucun son (rien à ajouter).

## Packs sans étiquette de style (aucun style proposé, faute de preuve)

| pack | lignes | pourquoi pas de style |
|---|---|---|
| RAR 7 p1 | 383 | uniquement du MIDI : pas de tempo ni de son à mesurer |
| RAR 7 p2 | 1461 | tempos mélangés d'un dossier à l'autre (pack multi-genre) |
| RAR 7 p3 | 182 | les sous-dossiers portent déjà leur style ; rien à poser au pack |
| RAR 8 p3 | 100 | effets sans tempo |
| RAR 11 p1 | 421 | aucun mot de genre, tempos sans signature d'un style |

Seules les lignes qui ont un style par leur propre dossier en ont un. Au total, 2 040 lignes sur 12 449 n'ont aucun style.

## À savoir

- `pack_line_ok` = NO sur le RAR 3 (1 ligne) et le RAR 10 p1 (2 lignes) : fausse alerte du lanceur ; les tables
  donnent le même nombre de lignes au style du pack que `tout.py`.
- « posé sur N lignes » = nombre de lignes qui **reçoivent** le style du pack parce qu'elles n'en avaient pas.
  Les lignes qui avaient déjà un style par leur dossier gardent le leur et ne sont pas comptées.
- RAR 9 p1 : style synthwave forcé par le chef (le pack n'en avait pas).
- Tonalité : même en comptant « même tonique » (root) avec l'accord exact, l'estimation ne rattrape pas l'étiquette :
  exact 18 %, même tonique 5 %, relative 7 %, autre 70 % (1 431 boucles étiquetées ; RAR 5 et 12 : 0 % exact).
  La tonalité estimée est **inutilisable** ; seule l'étiquette compte. Détail par RAR : `tout/chiffres.md` (G1).

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
| 7 | 2026 | 2026 | 0 | les 13 .ini/.txt sont exclus des deux côtés |
| 8 | 1218 | 1218 | 0 | les 35 presets et le .pdf sont exclus des deux côtés |
| 9 | 1325 | 1325 | 0 | les 30 presets et le .pdf sont exclus des deux côtés |
| 10 | 1025 | 1025 | 0 | le .txt est exclu des deux côtés |
| 11 | 1903 | 1905 | +2 | les 2 sons de l'archive imbriquée du pack 3, lus maintenant ; la 1re passe ne les avait pas |
| 12 | 808 | 808 | 0 | les 22 fichiers de l'archive imbriquée du pack 3, lus des deux côtés depuis le 2026-11-27 |
| **total** | **12256** | **12258** | **+2** | |

Coups de batterie par RAR (le total de fichiers ne bouge pas, seul le type change) :

| RAR | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1re passe | 921 | 210 | 50 | 67 | 80 | 53 | 0 | 300 | 190 | 141 | 305 | 116 |
| 2e passe | 919 | 213 | 50 | 67 | 80 | 58 | 0 | 300 | 190 | 142 | 305 | 115 |

- RAR 1 (−2), 2 (+3), 6 (+5), 10 (+1) : fichiers reclassés entre coup de batterie et autre type (même total de fichiers).
- RAR 12 (−1) : avec les 13 coups de l'archive imbriquée, il reste 1 fichier classé autrement (même total de fichiers).
- RAR 7 : aucun coup de batterie seul, dans les deux passes (voix, boucles mélodiques, pistes de kit, MIDI).

Par type, RAR 7, 9 et 11 (les lignes de kit ne comptent dans aucune des deux colonnes) :
- RAR 7 : 1re passe voix 1221, pistes de kit 192, boucles mélodiques 109, sons mélodiques 21 (+ MIDI 483) ;
  2e passe boucles mélodiques 605, voix 599, pistes de kit 269, sons mélodiques 70. Même total, classement différent
  (la 2e passe lit le dossier, la 1re le nom de fichier) : ne pas comparer les types du RAR 7 d'une passe à l'autre.
- RAR 9 : même total (1325) ; la 2e passe range les stems en pistes de kit (280) et ajoute 51 lignes de kit, hors compte.
- RAR 11 : type pour type identique (stems 512 = pistes de kit, coups 305, boucles de batterie 255) ; 74 lignes de kit, hors compte.

