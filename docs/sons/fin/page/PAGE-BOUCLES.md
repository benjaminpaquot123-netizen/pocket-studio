# Page privée « Références boucles »

But : écouter à côté de chaque ligne de la page Boucles le son original de pack le plus proche.
La page est privée (adresse donnée au chef seulement, jamais ici). Aucun son, nom de fichier,
nom de pack ou de fabricant dans le dépôt : seulement la clé `rNpN` et des mesures.

## Ce qui est sur la page (état du 2026-11-27)

| ligne | pack | tempo fabricant | distance | son |
|---|---|---|---|---|
| bouclemelodique_tous | r5p1 | 150 | 0.104 | prêt, remesuré |
| bouclemelodique_dnb | r10p1 | 174 | 0.004 | en attente (archive bloquée) |
| bouclemelodique_dubstep | r9p3 | 140 | 0.001 | en attente (archive bloquée) |
| bouclemelodique_techno | r3p1 | 125 | 0.008 | prêt, remesuré |
| bouclebatterie_tous | r5p1 | 150 | 0.100 | prêt, remesuré |
| bouclebatterie_dnb | r1p1 | 174 | 0.030 | en attente (archive bloquée) |
| bouclebatterie_dubstep | r1p1 | 140 | 0.028 | en attente (archive bloquée) |
| bouclebatterie_house | r12p2 | 125 | 0.017 | en attente (archive bloquée) |
| bouclebatterie_techno | r5p1 | 150 | 0.000 | prêt, remesuré |

« En attente » : l'hébergeur des archives refuse le téléchargement (quota dépassé) ; le choix est
fait, le son sera ajouté quand l'archive sera de nouveau téléchargeable et remesurée.
Batterie techno n'est jugée que sur la crête (RMS −6.2 dBFS non jugé) ; mélodique dnb sur durée et crête.

## Règle de choix

- Candidats : même famille et même style (« tous » = tous les styles), sans les sons où la
  contre-mesure (`docs/sons/passe2/verif/`) n'est pas d'accord.
- dnb, dubstep, house : tempo fabricant = tempo sûr (174 / 140 / 125, à ±2 %).
- Cibles : médianes des mesures « définitive » de `docs/sons/fin/cibles-autres-definitives.tsv`.
- Distance : moyenne de |v − méd| / max(p75 − p25, plancher) ; plancher 0,5 dB, 5 % pour la durée.
- Plus petite distance ; à 0,1 près du meilleur, la plus petite archive gagne.

## Rejouer

    python3 -I scripts/sons/fin/page/choix_boucles.py --selftest
    python3 -I scripts/sons/fin/page/choix_boucles.py --out docs/sons/fin/page/originaux-boucles.csv
    python3 -I scripts/sons/fin/page/choix_boucles.py --list 10   # classement par ligne

`REMEASURED` : clés remesurées égales au tableau ; `NOT_FOUND` : clés sautées (la suivante est prise).
