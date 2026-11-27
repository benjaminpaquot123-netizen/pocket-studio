# Critique à l'aveugle de la deuxième passe (12 RAR)

Base critiquée : `eq/passe2-reunion` 8d4b14d9e (tables `rarN/` des branches `eq/passe2-rarN`, `tout/*.csv`,
`chiffres.md`, `INVENTAIRE.md`, section « Deuxième passe » d'`ANALYSE-MINE-OR.md`, `STYLES-PACKS.md`, outils).
Rien n'a été corrigé ici : ce document ne fait que signaler. Les fichiers sont désignés par leur clé de table.

## Méthode (cinq lignes)

1. Recompte de toutes les tables avec mon propre script (`scripts/sons/passe2/critique/recompte.py`) : totaux, doublons, cellules absurdes, familles, sommes de contrôle, règles de confiance.
2. Remesure de 40 vrais sons du RAR 3 (tirés au hasard par type, graine 20261126) avec mon propre code numpy/scipy (`remesure.py`), tolérances écrites avant de lancer.
3. Rejeu du lanceur `lance.py` sur le RAR 3 dans un dossier vide : les 8 CSV sortis comparés octet par octet aux tables.
4. Attaque des conclusions : taux d'accord par type, dépendance à la fenêtre de mesure, sources qui dominent une médiane, écart du RAR 12.
5. Tout le matériel acheté a été effacé à la fin (dossiers de travail dans `/tmp`), rien n'est entré dans le dépôt.

## Chiffres clés et verdicts

| # | chiffre | verdict | pourquoi |
|---|---|---|---|
| 1 | 12 425 lignes de tables, 12 234 fichiers, 191 lignes de kit, 0 doublon | **tient** | recompté à l'identique |
| 2 | Sommes de contrôle par pack et par RAR (lignes = rows + kit_rows) | **tient** | recompté, 0 écart |
| 3 | Lignes par type et par style (INVENTAIRE, chiffres.md) | **tient** | recompté à l'identique |
| 4 | Durée, crête, RMS de chaque son | **tient** | remesure 40/40 sur chaque mesure |
| 5 | Le lanceur est reproductible | **tient** | rejeu RAR 3 : 8 CSV identiques octet par octet |
| 6 | Hauteur des notes mélodiques (f0) | **tient** | remesure 7/7 ; accord avec l'étiquette 96,5 % |
| 7 | Hauteur stable du kick ≈ 48 Hz | **tient** (à un demi-ton) | ~9 kicks sur 10 d'accord à un demi-ton près dans une fenêtre tardive ; dépend de la fenêtre (chute de hauteur) |
| 8 | 2 547 fichiers dans 5 packs sans étiquette | **tient** | recompté |
| 9 | Écart RAR 12 (−22 fichiers, −14 coups) et son explication | **fragile** | voir faute M5 |
| 10 | t20 / t60 du kick (194 / 290 ms) | **fragile** | médiane qui mélange les sources : t20 par groupe de 97 à 377 ms ; 217 kicks sur 512 viennent du RAR 1 |
| 11 | Médianes du kick_808 | **fragile** | 21 sur 23 viennent du RAR 1 : c'est le son d'un seul pack |
| 12 | Hauteur stable des snares et percs | **fragile** | remesure 1/7 d'accord sur les coups ; varie de 10 à 40 % selon la fenêtre |
| 13 | Tempo estimé des boucles | **fragile** | accord avec l'étiquette : 66,3 % batterie, 48,9 % mélodiques ; erreurs de moitié (62,5 pour 125) ; ma remesure 12/16 |
| 14 | Note des coups de batterie (33,8 % d'accord) | **fragile** | invérifiable depuis les tables (l'étiquette de note n'y est pas gardée) |
| 15 | Hauteur des claps, hats, shakers, rides, crashs | **faux** | du bruit n'a pas de hauteur ; hat : 6 lignes gardées sur 246 alors que la table affiche n = 246 |
| 16 | Tonalité estimée des boucles mélodiques | **faux** | fausse environ 7 fois sur 10 (voir faute G1) |

**Bilan : 16 chiffres clés vérifiés, 8 tiennent, 6 fragiles, 2 faux.**

### Ce qu'on peut utiliser dans `chiffres.md` pour régler un son

- Sans réserve : durée, crête, RMS ; hauteur des notes mélodiques.
- Avec leur fourchette [p25–p75] affichée : t20 / t60 des snares, claps, percs, hats fermés et ouverts.
- Kick : hauteur stable ≈ 48 Hz à un demi-ton près ; t20 / t60 à prendre **par style**, pas la médiane globale.
- À ne pas utiliser : hauteur des claps, hats, shakers, rides, crashs ; médianes kick_808 comme « le » 808 ; tonalité estimée ; note des coups.
- Non vérifiés (pas remesurés) : centroïde, hf dB, transitoire/corps, tonal dB. À prendre comme indication.

## Remesure du RAR 3 : taux d'accord

40 sons, 0 sans correspondance de clé.

| mesure | tolérance | accord |
|---|---|---|
| durée | ±0,01 s | 40/40 |
| crête | ±0,5 dB | 40/40 |
| RMS | ±0,5 dB | 40/40 |
| f0 des notes mélodiques | ±50 cents | 7/7 |
| tempo estimé (bpm_est) | ±2 % | 12/16 (75 %) ; mon tempo contre l'étiquette : 13/16 |
| tonalité estimée (key_est) | même tonique et même mode | 6/8 ; ma tonalité contre l'étiquette : 6/8 |
| hauteur stable des coups (pitch_steady) | ±50 cents | 1/7 |

Sensibilité de la hauteur stable à la fenêtre (les 31 coups tonaux du RAR 3, accord à 50 cents) :
0,02–0,1 s → 5/31 ; 0,05–0,2 s → 6/31 ; 0,1–0,3 s → 12/31 ; 0,05–0,4 s → 10/31.
Sur les tempos en désaccord, une partie des fautes est la mienne (boucles courtes de 1,92 s).

## Fautes

### Graves

- **G1 — Tonalité inutilisable.** Sur les 1 431 boucles mélodiques étiquetées : 259 exactes, 72 même tonique, 99 relatives, 1 001 autres. Exact + même tonique = 23,1 % ; RAR 5 et RAR 12 : 0 exacte. INVENTAIRE laisse croire que compter « root » rattraperait l'accord : ça ne change presque rien. Kits : 55,9 %.
- **G2 — Hauteur des coups présentée avec un faux n.** Le « n » de `chiffres.md` est la taille de la famille, pas le nombre de lignes gardées par les règles de confiance (hat : 6/246 ; snare : 271/409). La hauteur stable ne résiste pas à une remesure indépendante (1/7), et pour les claps, hats et cymbales c'est du bruit.
- **G3 — Nom d'un pack acheté dans le dépôt public.** Hors de la deuxième passe, mais trouvé pendant la critique : la ligne de commentaire en tête de `docs/sons/rar/rar12-tout.csv` sur la branche de première passe `eq/mine-or-rar12-tout` contient le nom de l'éditeur du pack. C'est contraire aux interdits ; le nom n'est pas recopié ici.

### Moyennes

- **M1 — Médiane du kick dominée par une source** : 217/512 kicks viennent du RAR 1 ; le t20 va de 97 à 377 ms selon le groupe. Ce n'est pas un kick type, c'est surtout celui du RAR 1.
- **M2 — kick_808 = un seul pack** (21/23 du RAR 1).
- **M3 — Aucune valeur aberrante retirée** : 24 coups ont une hauteur stable sous 20 Hz ; le titre le dit, mais les médianes servent de cibles.
- **M4 — Classement des hats** : 23 % des hats ont un type (fermé/ouvert) qui contredit leur mesure de durée.
- **M5 — Explication du RAR 12 incohérente** : INVENTAIRE dit que « le reste de l'écart n'est pas expliqué », alors que 14 coups manquants ≤ 22 fichiers manquants. L'archive imbriquée est un paquet de presets de synthé : ses 22 fichiers audio sont probablement des tables d'ondes, que la première passe aurait comptées comme coups (alors −14 serait une amélioration). La première passe n'a pas de colonne pack, donc ce n'est pas prouvable depuis les tables.
- **M6 — Tempo de moitié ou de double** non replié (ex. 62,5 pour une boucle à 125) : le taux de tempo sous-estime la justesse réelle, mais la colonne brute ne peut pas servir de cible telle quelle.
- **M7 — Fausse alerte du lanceur** (code 4, « 370 lignes contre 369 ») sur les RAR 3, 8, 10, 11 et 12 : les tables sont justes, mais une alerte qui sonne toujours finira par cacher une vraie erreur.

### Mineures

- **m1** — Lignes réellement « sans style » : 2 040, pas 2 547 (RAR 7 p1 et p3 tirent un style de leurs sous-dossiers). Un « 2 547 lignes sans style » serait faux ; `chiffres.md` dit bien « lignes du pack ».
- **m2** — La note des coups (33,8 %) ne peut pas être vérifiée : l'étiquette de note n'est pas gardée dans la table des coups. Cause probable : une lettre de variante en fin de nom lue comme une note.
- **m3** — RAR 10 p2 / p3 : « posé sur 37 / 27 lignes » alors que toutes les lignes portent le style du pack (par leur dossier). Pas une erreur, mais trompeur.

## Ce que je n'ai pas pu vérifier

- Remesure des autres RAR (seul le RAR 3 a été rejoué et remesuré).
- Centroïde, hf dB, transitoire/corps et tonal dB : pas remesurés.
- La cause exacte de l'écart du RAR 12, fichier par fichier (les clés des deux passes ne correspondent pas, pas de colonne pack en première passe).
- La note des coups (étiquette absente des tables).
- Le contenu des 3 archives imbriquées (non ouvertes).
