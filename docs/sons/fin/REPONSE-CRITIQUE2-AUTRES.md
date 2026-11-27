# Réponse à la seconde critique des sons hors batterie (2026-11-27)

Rejouer tout : `python3 -I scripts/sons/fin/cibles_autres.py --selftest`, puis régénérer le tsv et le comparer.
Lignes définitives : 99 avant, 78 après. Jointure définitives × `ecart_important` = oui : 0. Points refusés : aucun.

| point | réponse | comment le rejouer |
|---|---|---|
| Grave 1 : des cibles portées par un seul fabricant | corrigé | 5e condition dans `cibles_autres.py` ; colonnes `mediane_sans_gros_pack`, `tient_sans_gros_pack` ; self-test (pad techno, pluck house) |
| Grave 1 bis : bougé tout juste de 20 % | corrigé | « au moins 20 % » compte 20 % exact ; batterie techno durée 6,4 → 7,68 s tombe aussi ; self-test (arp techno +20 %) |
| Voix « un peu plus fortes que les boucles » | corrigé | conclusion : voix −19,3, boucles mélodiques −14,1, batterie −17,3 ; « crête −3 dB » n'est plus une cible |
| Critère de tempo (accord ≥ 90 %) non écrit | corrigé | conclusion, « Le tempo du fabricant » ; colonne `raison` (afro-house 57 %) |
| Quantiles de tempo sur les seules boucles confirmées | corrigé | p10–p90 sur toutes les boucles étiquetées (afro-house p75 120, drift-phonk médiane 125) ; self-test (lofi) |
| Sons sans style | corrigé | une ligne : pas de ligne de style, comptés dans « tous » |
| Mesures sans contre-mesure | corrigé | paragraphe « Ce qui n'a pas de contre-mesure » (tempo, MIDI, `range_st`, `voiced_pct`) |
| Autres écarts sans effet (sortent de p25–p75, bougent peu) | corrigé | 8 lignes restent définitives : bougent de moins de 1 dB ou 20 % (filtrer `tient_sans_gros_pack` = oui et comparer) |

## Les 21 lignes qui perdent leur statut
- Basse : durée dubstep, house, trap. Coups « - » tous : durée. Pad tous : volume.
- Synth house : durée, volume. Synth techno : durée, volume. Boucle mélodique dnb : volume.
- Batterie techno : volume, durée (+20 % exact, en plus de la critique). Effets rave : volume, crête.
- Voix tous : volume, crête. Voix dnb : durée, volume. Voix rave : durée, volume, crête.

## Ce qui reste définitif (78 lignes)
- Basse : tous, dnb, techno (3 mesures) ; dubstep, house, trap (volume, crête ; trap portée à 81 %).
- Coups mélodiques : chords, keys, lead, synth tous ; keys lofi ; lead dubstep (3 mesures) ;
  « - » (volume, crête ; portée à 82 %) ; pad (durée, crête) ; synth house et synth techno (crête).
- Boucles mélodiques : tous, dubstep, techno (3 mesures) ; dnb (durée, crête).
- Batterie : tous, dnb, dubstep, house (3 mesures ; house portée à 89 %) ; techno (crête).
- Voix : tous (durée, portée à 83 %) ; dnb (crête, portée à 83 %).
- Effets : tous, dnb, dubstep, techno (3 mesures) ; rave (durée, portée à 89 %).
