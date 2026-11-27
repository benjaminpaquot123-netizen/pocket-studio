# Page d'écoute « Références claviers » (lot `claviers`)

But : un son d'origine des packs par ligne « coup mélodique » (chords, keys, lead, synth), à écouter
à côté du son de l'appli. Analyse et écoute seulement : rien n'entre dans l'appli ni dans le dépôt.

## Le choix (8 lignes, toutes « définitive »)

| slug | pack | distance | mesures visées |
|---|---|---|---|
| chords_tous | r10p3 | 0,001 | durée, niveau moyen, crête |
| keys_tous | r9p2 | 0,103 | durée, niveau moyen, crête |
| keys_lofi | r4p1 | 0,101 | durée, niveau moyen, crête |
| lead_tous | r8p1 | 0,064 | durée, niveau moyen, crête |
| lead_dubstep | r8p1 | 0,020 | durée, niveau moyen, crête |
| synth_tous | r5p1 | 0,096 | durée, niveau moyen, crête |
| synth_house | r12p2 | 0,000 | crête seule |
| synth_techno | r5p1 | 0,000 | crête seule |

Détail (clé du son, mesures, médianes visées) : `originaux-claviers.csv`. Aucune ligne sans candidat.

## État de la page (2026-11-27)

Pas encore publiée. Les six archives utiles (RAR 4, 5, 8, 9, 10, 12) répondent toutes « quota
dépassé » au téléchargement ; seuls les morceaux de 16 Mo au plus passent, et on ne contourne pas
cette limite en découpant. Colonne `remesure_ok` vide tant que les sons ne sont pas remesurés.
À reprendre quand le quota est revenu : télécharger, n'extraire que les fichiers choisis, remesurer
avec `scripts/sons/passe2/verif/mesure.py`, noter `oui` / `non` dans `REMESURE` du script, rejouer.

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
