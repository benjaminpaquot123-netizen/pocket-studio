# Avant / après des 5 lots du matin (2026-11-26)

Chaque fichier : 40 s du passage principal **avant** le lot, 1 s de silence, puis les mêmes 40 s **après** le lot.
Les sons sont joués comme sur le téléphone (chemin de `scripts/live-bench.sh` puis `ps_live`), sans normalisation : une différence de volume entre les deux moitiés est donc vraie.
Le passage commence un quart de seconde avant le début de la partie la plus forte.

- Avant : `ccr-80af1a1b-mn0wec` (d9ac1b3a5).
- Commande (depuis la racine du dépôt) : `scripts/son/avant_apres_styles.sh DOSSIER_SORTIE [lot…]`
  (lots : house2, bass, l12, techno, l6 ; sans lot = les cinq).
- Durée du passage complet : 11 min 26 s, 24 MP3, aucun lot en échec (dossier de travail : 2,2 Go au plus, plafond 3 Go).
- Tous les fichiers durent 81 s, le silence du milieu est bien muet, aucune moitié muette ni écrêtée, aucune alerte de `ps_live` dans les passages choisis.
- Volume : sonie (LUFS) de chaque moitié ; plus proche de 0 = plus fort. Un écart de moins de 0,3 dB ne s'entend presque pas.
- Les sons ne sont pas dans le dépôt (il est public) : ils restent dans le dossier de sortie de la discussion qui les a rendus.
- Attention : sans les packs de sons, le lead du trap joue la Cloche FM de secours, pas la flûte. La nouvelle flûte du lot l6 ne s'entend donc qu'avec les packs.

## Ce que chaque lot est censé changer

| Lot | Branche (tête) | Changement visé |
|---|---|---|
| house2 | eq/tour-9-gr-house2 (02f920cf6) | recettes house retouchées après l'écoute de Benjamin (acid house moins agressive, doublure des accords) |
| bass | eq/tour-9-gr-bass (ae0c62716) | drop des musiques bass avec kick et basse devant (dnb, dubstep, synthwave, pop) |
| l12 | eq/tour-9-gr-l12 (d9506fe34) | nouveaux effets du moteur (attaque, poids, préréglage « Corps ») |
| techno | eq/tour-9-gr-techno (c772d3692) | recettes techno et techno dure : le corps garde son sub, mixage large qui trouve le bon lead, accents acid sur des notes jouées |
| l6 | eq/tour-9-gr-l6 (ea803f663) | nouveau motif du lead trap ; flûte à partir de la note 60, attaque plus douce |

## Un fichier par style (24)

| Fichier | Lot | Style | Départ du passage | Volume avant → après (LUFS) |
|---|---|---|---|---|
| house2_house.mp3 | house2 | house | 7,75 s | −9,6 → −9,4 |
| house2_tech_house.mp3 | house2 | tech_house | 7,75 s | −9,8 → −9,8 |
| house2_acid_house.mp3 | house2 | acid_house | 7,75 s | −9,3 → −9,0 |
| house2_afro_house.mp3 | house2 | afro_house | 15,75 s | −10,1 → −10,0 |
| bass_dnb.mp3 | bass | dnb | 11,75 s | −8,9 → −8,9 |
| bass_liquid_dnb.mp3 | bass | liquid_dnb | 11,75 s | −8,8 → −8,9 |
| bass_neurofunk.mp3 | bass | neurofunk | 11,75 s | −9,2 → −9,2 |
| bass_jungle.mp3 | bass | jungle | 11,75 s | −8,4 → −7,8 |
| bass_jump_up.mp3 | bass | jump_up | 11,75 s | −9,2 → −9,4 |
| bass_dubstep.mp3 | bass | dubstep | 27,75 s | −8,9 → −8,9 |
| bass_riddim.mp3 | bass | riddim | 13,75 s | −11,5 → −11,8 |
| bass_synthwave.mp3 | bass | synthwave | 29,75 s | −8,5 → −8,8 |
| bass_pop.mp3 | bass | pop | 18,75 s | −9,5 → −9,5 |
| techno_techno.mp3 | techno | techno | 29,75 s | −8,9 → −9,1 |
| techno_retro_techno.mp3 | techno | retro_techno | 15,75 s | −8,6 → −8,6 |
| techno_detroit.mp3 | techno | detroit | 15,75 s | −8,5 → −8,5 |
| techno_industrial_techno.mp3 | techno | industrial_techno | 15,75 s | −7,9 → −7,9 |
| techno_hard_techno.mp3 | techno | hard_techno | 24,75 s | −9,5 → −9,6 |
| techno_acid_techno.mp3 | techno | acid_techno | 26,75 s | −8,2 → −8,3 |
| techno_gabber.mp3 | techno | gabber | 12,75 s | −8,3 → −8,3 |
| techno_acidcore.mp3 | techno | acidcore | 11,75 s | −8,5 → −8,3 |
| techno_minimal.mp3 | techno | minimal | 15,75 s | −8,6 → −8,6 |
| techno_trance.mp3 | techno | trance | 26,75 s | −8,5 → −8,5 |
| l6_trap.mp3 | l6 | trap | 15,75 s | −9,9 → −10,0 |

En bref : les lots changent le son (les deux moitiés diffèrent), presque sans changer le volume.
Les plus gros écarts : jungle +0,6 dB, acid house +0,3 dB, synthwave et riddim −0,3 dB.

## Styles identiques ou en échec

- **Identiques (pas de fichier)** : les 8 styles essayés pour le lot **l12** (boom_bap, house, techno, dnb, reggaeton, trap, deep_house, retro_techno).
  Ses nouveaux effets ne sont pas encore utilisés dans les morceaux faits par l'assistant : l'avant et l'après sont le même son, à l'échantillon près, sur tout le morceau (90 s).
  Pour l'entendre, il faudra un morceau qui pose ces effets.
- **En échec** : aucun.
