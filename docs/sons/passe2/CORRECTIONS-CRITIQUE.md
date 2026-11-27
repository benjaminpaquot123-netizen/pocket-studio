# Corrections après la critique de la deuxième passe (2026-11-27)

Tables rejouables en une commande : `python3 -I scripts/sons/passe2/fin/tables.py` (écrit `docs/sons/passe2/tout/`).
Règle de cible : n ≥ 20 sons **et** ≥ 2 packs distincts = « définitif » ; sinon « pas de cible ». Chaque chiffre de
`tout/chiffres.md` porte son n et son nombre de packs.

## Ce qu'on peut utiliser

**Chiffres sûrs** (remesurés à l'aveugle, accord dans la tolérance écrite avant) :
- durée, crête, RMS : 40/40 ; centroïde : 102/102 coups (tolérance ±10 %) ; transitoire/corps : 102/102 (±1 dB, écart max 0) ;
- médianes par famille marquées « définitif » (t20, centroïde, transitoire/corps) ; hauteur stable de la grosse caisse ;
- tempo **replié** des boucles de batterie (97 % d'accord avec l'étiquette) ; tempo cible par style pris de l'étiquette
  (dnb 174, dubstep 140, techno 150, house 125, lofi 77) ;
- grosse caisse par style quand « définitif » (dubstep, dnb, rave, lofi) ; charlestons rangés **par mesure**.

**À fourchette** (ordre de grandeur, pas une cible) :
- hauteur de snare, perc, tom (remesure : 14 à 22 % d'accord) ; t60 (dépend de la fin du fichier) ;
- tempo replié des boucles mélodiques (72 %, RAR 7 : 24 %).

**À ne pas utiliser** :
- tonalité estimée (18 % exact) ; tempo brut non replié ; hauteur des coups de bruit (clap, hats, shaker, ride, crash, cymbal, snap) ;
- tout kick_808 (un seul pack) ; tout style à un seul pack (house, drift-phonk, multi, afro-house, amapiano ; techno pour t20/t60) ;
- note des coups (66 étiquetées seulement) ; le 33,8 % de notes « justes » de la première version (invérifiable).

## Une ligne par faute

| n° | faute | correction | état |
|---|---|---|---|
| 1 | G1 tonalité | exact 18 %, même tonique 5 %, relative 7 %, autre 70 % (1 431 boucles) ; RAR 5 et 12 : 0 % exact. La tonique ne sauve pas l'accord : inutilisable | fait |
| 2 | G2 n de la hauteur | n = lignes réellement gardées ; hauteur retirée pour clap, hats, shaker, ride, crash, cymbal, snap | fait |
| 3 | M1 grosse caisse | une ligne par style (t20, t60, hauteur, durée) avec n, p25–p75, packs et part par RAR | fait |
| 4 | M2 kick_808 | 21/23 lignes d'un seul pack, t20/t60 de lui seul : marqué « un seul pack » | fait |
| 5 | M3 aberrants | règle écrite en tête de `tables.py` ; médianes refaites sans aberrants (grosse caisse : −3 et −8, hauteur 48.3 → 48.5 Hz) | fait |
| 6 | M4 charlestons | 95/408 (23 %) contredits ; chiffres par mesure : fermé 134 ms, ouvert 440 ms (t60) | fait |
| 7 | M5 RAR 12 | réécrit : les 22 fichiers étaient dans l'archive imbriquée, lue maintenant ; écart 0 | fait |
| 8 | M6 tempo | colonne `bpm_gap_folded_pct` ; accord brut (écart absolu) et replié ; tempo cible par style tiré de l'étiquette | fait |
| 9 | m1/m2/m3 | 2 040 lignes sans style ; 33,8 % retiré (invérifiable), note : 973/1 051 mélodiques d'accord, coups pas de cible ; « posé sur N lignes » précisé | fait |
| 10 | packs sans étiquette | aucun style proposé (MIDI seul, multi-genre, effets sans tempo, sous-dossiers déjà stylés, aucun mot de genre) | fait |
| 11 | archives imbriquées | mesurées par `tout.py` non modifié : RAR 12 +22, RAR 11 +2, RAR 11 p2 = presets ; lignes dans `passe2/fin/imbriquees/` | fait |
| 12 | 1re contre 2e passe, RAR 7/9/11 | par type dans `INVENTAIRE.md` ; lignes de kit hors compte des deux côtés ; RAR 7 classé autrement (même total) | fait |
| 13 | remesure RAR 12 | 40 sons, graine 20261127 (échantillon neuf), tolérances écrites avant ; durée/crête/RMS 40/40, tempo 15/16, tonalité 4/6, f0 1/7, hauteur stable 2/9 ; centroïde et transitoire/corps 102/102 (`fin/spectre.py`) | fait |
