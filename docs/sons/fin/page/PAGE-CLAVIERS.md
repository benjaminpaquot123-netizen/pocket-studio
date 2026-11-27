# Page d'écoute « Références claviers » (lot `claviers`)

But : un son d'origine des packs par ligne « coup mélodique » (chords, keys, lead, synth), à écouter
à côté du son de l'appli. Analyse et écoute seulement : rien n'entre dans l'appli ni dans le dépôt.

## Le choix (8 lignes, toutes « définitive »)

| slug | pack | distance | mesures visées | son |
|---|---|---|---|---|
| chords_tous | r10p3 | 0,001 | durée, niveau moyen, crête | prêt, remesuré |
| keys_tous | r9p2 | 0,103 | durée, niveau moyen, crête | prêt, remesuré |
| keys_lofi | r4p1 | 0,101 | durée, niveau moyen, crête | prêt, remesuré |
| lead_tous | r8p1 | 0,064 | durée, niveau moyen, crête | prêt, remesuré |
| lead_dubstep | r8p1 | 0,020 | durée, niveau moyen, crête | prêt, remesuré |
| synth_tous | r5p1 | 0,096 | durée, niveau moyen, crête | prêt, remesuré |
| synth_house | r12p2 | 0,000 | crête seule | prêt, remesuré |
| synth_techno | r5p1 | 0,000 | crête seule | prêt, remesuré |

Détail (clé du son, mesures, médianes visées) : `originaux-claviers.csv`. Aucune ligne sans candidat.

## État de la page (2026-11-27)

Publiée en page privée (adresse donnée au chef seulement, jamais ici). Les 8 lignes ont un son.
Les six archives utiles répondaient « quota dépassé » : les sons sont venus d'un envoi de Benjamin,
puis ont été remesurés avec `scripts/sons/passe2/verif/mesure.py` ; les 8 sont égaux au tableau
(durée 0,01 s, crête 0,1 dB, niveau moyen 0,2 dB), donc `remesure_ok = oui` partout et aucun
remplacement par le suivant du classement. House et techno ne sont jugés que sur la crête.

## Rejouer le choix

```sh
python3 -I scripts/sons/fin/page/choix_claviers.py --selftest   # 0 failure(s)
python3 -I scripts/sons/fin/page/choix_claviers.py              # réécrit originaux-claviers.csv
python3 -I scripts/sons/fin/page/choix_claviers.py --plan       # rar, pack, file_id, clé des choix
```

Règle : candidats = sons du rôle (et du style) sans erreur, contre-mesure « yes » sur durée, niveau
moyen et crête ; distance = moyenne de |valeur − médiane| / max(p75 − p25, plancher) sur les mesures
« définitive » (plancher 0,5 dB, ou 5 % de la médiane pour la durée) ; à moins de 0,1 du meilleur, le
pack de la plus petite archive l'emporte. Un son remesuré « non » passe au suivant de la liste.
