# Page d'écoute « voix et effets » : sons originaux du lot voixfx

But : poser, à côté du son de l'appli, le son original du pack le plus proche de la cible
(« À côté le son original pour voir si ça ressemble bien. »). Page privée, jamais dans le dépôt.

## Ce qui est sur la page (2026-11-27)

| ligne | pack | distance | son |
|---|---|---|---|
| voix_tous | r7p2 | 0.016 | prêt, remesuré |
| voix_dnb | r11p2 | 0.018 | prêt, remesuré |
| effet_tous | r5p1 | 0.062 | prêt, remesuré |
| effet_dnb | r10p1 | 0.001 | prêt, remesuré |
| effet_dubstep | r8p1 | 0.003 | prêt, remesuré |
| effet_techno | r5p1 | 0.175 | prêt, remesuré |
| effet_rave | r11p3 | 0.019 | prêt, remesuré |

Les 7 lignes ont un son (`originaux-voixfx.csv`). Les sons sont venus d'un envoi de Benjamin
(archives bloquées par un quota) : les 3 premiers du classement de chaque ligne, tous remesurés
dans les tolérances de `scripts/sons/passe2/verif/TOLERANCES.md` ; aucun écarté.
Effet « tous styles » : r5p1 à 0.062 plutôt que le meilleur (0.025), règle de la plus petite archive.
La page est privée (adresse donnée au chef seulement, jamais ici). Aucun son, nom de fichier,
nom de pack ou de fabricant dans le dépôt : seulement la clé `rNpN` et des mesures.

## Règle de choix

Candidats : lignes famille + style des tables passe 2, moins celles que la contre-mesure contredit.
Cibles : médianes des mesures définitives ; une seule définitive = les deux autres à poids 0,5
(poids écrits dans le csv). Distance : moyenne pondérée de |v − médiane| / max(IQR, plancher),
plancher 0,5 dB (5 % de la médiane pour la durée). À 0,1 près du meilleur : plus petite archive.

## Rejouer

    python3 -I scripts/sons/fin/page/choix_voixfx.py --selftest
    python3 -I scripts/sons/fin/page/choix_voixfx.py            # réécrit le csv
    python3 -I scripts/sons/fin/page/choix_voixfx.py --rang 2   # candidat suivant

Après remesure : remplir REMESURE (clé -> oui/non) et EXCLUS (clé -> raison) dans le script,
régénérer le csv, puis MP3 160 kb/s sans changement de volume (coupe à 16 s, fondu 0,3 s).
