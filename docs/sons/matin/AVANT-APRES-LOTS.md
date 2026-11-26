# Avant / après des 5 lots du matin (2026-11-26)

Chaque fichier : 40 s du passage principal **avant** le lot, 1 s de silence, puis les mêmes 40 s **après** le lot.
Les sons sont joués comme sur le téléphone (chemin de `scripts/live-bench.sh` puis `ps_live`), sans normalisation :
une différence de volume entre les deux moitiés est donc vraie.

- Avant : `ccr-80af1a1b-mn0wec` (d9ac1b3a5).
- Commande (depuis la racine du dépôt) : `scripts/son/avant_apres_styles.sh DOSSIER_SORTIE [lot…]`
  (lots : house2, bass, l12, techno, l6 ; sans lot = les cinq).
- Durée du passage complet : 15 min 53 s, 24 MP3, aucun lot en échec.
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
| house2_house.mp3 | house2 | house | 8,5 s | −9,5 → −9,4 |
| house2_tech_house.mp3 | house2 | tech_house | 8,5 s | −9,8 → −9,8 |
| house2_acid_house.mp3 | house2 | acid_house | 8,5 s | −9,3 → −9,0 |
| house2_afro_house.mp3 | house2 | afro_house | 16,5 s | −10,1 → −10,1 |
| bass_dnb.mp3 | bass | dnb | 12,5 s | −8,9 → −8,9 |
| bass_liquid_dnb.mp3 | bass | liquid_dnb | 12,5 s | −8,7 → −8,9 |
| bass_neurofunk.mp3 | bass | neurofunk | 12,5 s | −9,2 → −9,2 |
| bass_jungle.mp3 | bass | jungle | 12,5 s | −8,3 → −7,8 |
| bass_jump_up.mp3 | bass | jump_up | 12,5 s | −9,3 → −9,4 |
| bass_dubstep.mp3 | bass | dubstep | 28,5 s | −8,9 → −8,9 |
| bass_riddim.mp3 | bass | riddim | 14,5 s | −11,5 → −11,8 |
| bass_synthwave.mp3 | bass | synthwave | 30,5 s | −8,5 → −8,8 |
| bass_pop.mp3 | bass | pop | 19,5 s | −9,6 → −9,5 |
| techno_techno.mp3 | techno | techno | 30,5 s | −8,9 → −9,1 |
| techno_retro_techno.mp3 | techno | retro_techno | 16,5 s | −8,6 → −8,6 |
| techno_detroit.mp3 | techno | detroit | 16,5 s | −8,5 → −8,5 |
| techno_industrial_techno.mp3 | techno | industrial_techno | 16,5 s | −7,9 → −7,9 |
| techno_hard_techno.mp3 | techno | hard_techno | 25,5 s | −9,4 → −9,6 |
| techno_acid_techno.mp3 | techno | acid_techno | 27,5 s | −8,2 → −8,3 |
| techno_gabber.mp3 | techno | gabber | 13,5 s | −8,3 → −8,3 |
| techno_acidcore.mp3 | techno | acidcore | 12,5 s | −8,5 → −8,4 |
| techno_minimal.mp3 | techno | minimal | 16,5 s | −8,6 → −8,6 |
| techno_trance.mp3 | techno | trance | 27,5 s | −8,5 → −8,5 |
| l6_trap.mp3 | l6 | trap | 16,5 s | −9,9 → −10,1 |

En bref : les lots changent le son (les deux moitiés diffèrent), presque sans changer le volume.
Les plus gros écarts : jungle +0,5 dB, acid house +0,3 dB, synthwave et riddim −0,3 dB.
Le volume moyen (rms) du hard techno monte de 1,5 dB alors que sa sonie bouge à peine : le lot remplit davantage le passage sans le rendre plus fort.

## Styles identiques ou en échec

- **Identiques (pas de fichier)** : les 8 styles essayés pour le lot **l12** (boom_bap, house, techno, dnb, reggaeton, trap, deep_house, retro_techno).
  Ses nouveaux effets ne sont pas encore utilisés dans les morceaux faits par l'assistant : l'avant et l'après sont le même son, à l'échantillon près.
  Pour l'entendre, il faudra un morceau qui pose ces effets.
- **En échec** : aucun.
