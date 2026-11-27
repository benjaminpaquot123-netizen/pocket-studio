# Page privée « Références basses » (lot basses)

Un son original de pack par ligne, à écouter à côté du rendu de l'appli.
La page est privée ; son adresse n'est pas dans le dépôt. Aucun son ici.

## Ce qui est sur la page (14 sons sur 14 lignes)

| Ligne | Statut | Pack | Distance |
|---|---|---|---|
| basse_tous | définitive | r8p1 | 0.000 |
| basse_dnb | définitive | r10p1 | 0.121 |
| basse_dubstep | définitive | r8p1 | 0.000 |
| basse_house | définitive | r12p2 | 0.052 |
| basse_techno | définitive | r5p1 | 0.006 |
| basse_trap | définitive | r9p2 | 0.000 |
| pad_tous | définitive | r2p1 | 0.000 |
| coupdivers_tous | définitive | r11p1 | 0.004 |
| arp_tous | exemple, pas de cible | r9p1 | 0.045 |
| strings_tous | exemple, pas de cible | r11p1 | 0.004 |
| bell_tous | exemple, pas de cible | r9p2 | 0.075 |
| pluck_tous | exemple, pas de cible | r10p2 | 0.204 |
| guitar_tous | exemple, pas de cible | r9p2 | 0.045 |
| brass_tous | exemple, pas de cible | r9p2 | 0.126 |

Lignes sans son utilisable : aucune. Les 14 sons remesurés concordent
avec la table (durée, RMS, crête : `remesure_ok = oui`).
MP3 160 kb/s, niveau d'origine, aucun au-delà de 16 s (pas de coupe).

## Rejouer le choix

    python3 -I scripts/sons/fin/page/choix_basses.py --selftest
    python3 -I scripts/sons/fin/page/choix_basses.py          # réécrit le CSV
    python3 -I scripts/sons/fin/page/choix_basses.py --besoin # clés à chercher

Cibles : médianes de `cibles-autres-definitives.tsv` (mesures « définitive »
de la ligne ; durée, RMS et crête pour un exemple). Distance : moyenne de
|v − médiane| / max(p75 − p25, plancher 0,5 dB ou 5 % de la médiane pour la
durée). Contre-mesure en désaccord exclue ; à 0,1 près, la plus petite archive.
Un échec de remesure s'écrit « non » dans `REMESURE` et passe au suivant.
