# Page d'écoute « voix et effets » : sons originaux du lot voixfx

But : poser, à côté du son de l'appli, le son original du pack le plus proche de la cible
(« À côté le son original pour voir si ça ressemble bien. »). Page privée, jamais dans le dépôt.

## Choix (fait, `originaux-voixfx.csv`)

| ligne | pack | distance |
|---|---|---|
| voix_tous | r7p2 | 0.016 |
| voix_dnb | r11p2 | 0.018 |
| effet_tous | r5p1 | 0.062 |
| effet_dnb | r10p1 | 0.001 |
| effet_dubstep | r8p1 | 0.003 |
| effet_techno | r5p1 | 0.175 |
| effet_rave | r11p3 | 0.019 |

Candidats : lignes famille + style des tables passe 2, moins celles que la contre-mesure contredit.
Cibles : médianes des mesures définitives ; une seule définitive = les deux autres à poids 0,5
(poids écrits dans le csv). Distance : moyenne pondérée de |v − médiane| / max(IQR, plancher),
plancher 0,5 dB (5 % de la médiane pour la durée). À 0,1 près du meilleur : plus petite archive.

## État de la page (2026-11-27)

Pas encore publiée : aucun son téléchargé. Le site de stockage refuse chaque archive
(« quota dépassé » sur les 5 archives utiles : r5, r7, r8, r10, r11). `remesure_ok` est vide.

## Rejouer

    python3 -I scripts/sons/fin/page/choix_voixfx.py --selftest
    python3 -I scripts/sons/fin/page/choix_voixfx.py            # réécrit le csv
    python3 -I scripts/sons/fin/page/choix_voixfx.py --rang 2   # candidat suivant

Après remesure : remplir REMESURE (clé -> oui/non) et EXCLUS (clé -> raison) dans le script,
régénérer le csv, puis MP3 160 kb/s sans changement de volume (coupe à 16 s, fondu 0,3 s).
