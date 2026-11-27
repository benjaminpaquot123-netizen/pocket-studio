# Seconde critique à l'aveugle : familles hors coups de batterie (2026-11-27)

Base jugée : `eq/son-fin-autres-v2` (cb042979b). Recalcul indépendant : `scripts/sons/fin/critique2_autres.py`
(rien d'importé des scripts jugés). Rejouer : `python3 scripts/sons/fin/critique2_autres.py` (écarts et
solidité dans `critique2-autres-ecarts.tsv`), `--selftest`, `--b1` (tirage du point 5).

## Grave

1. Des cibles « définitives » ne sont que la valeur d'un seul pack, et la conclusion ne le dit pas
   pour 6 d'entre elles. Test : médiane recalculée sans le plus gros pack ; si elle sort de p25–p75 et
   bouge d'au moins 1 dB (ou 20 % pour la durée), la cible change selon un seul fabricant.
   20 mesures sur 99 sont dans ce cas (colonne `ecart_important` = oui). Annoncé → sans le plus gros pack :
   - non marquées « portées surtout par un pack » (part ≤ 75 %), donc présentées comme solides :
     basse dubstep durée 0,86 → 1,71 s (r8p1, 59 %) ; basse house durée 1,05 → 0,72 s (r10p2, 58 %) ;
     pad tous RMS −16,9 → −14,0 dB (r2p1, 66 %) ; synth techno durée 2 → 0,48 s et RMS −20 → −17,4 dB
     (r5p1, 67 %) ; boucle mélodique dnb RMS −13,3 → −16,2 dB (r11p2, 55 %) ;
     boucle de batterie techno RMS −14,6 → −21,1 dB (r5p1, 65 %).
   - marquées (> 75 %) mais dites « elle tient » : basse trap durée 7,4 → 9,9 s ; coups « - » durée
     1,0 → 7,3 s ; synth house durée 1,8 → 0,96 s, RMS −19,1 → −21,3 dB ; voix tous RMS −19,3 → −16,1 dB
     et crête −3,0 → −0,1 dB (la « crête −3 dB » des voix est celle de r7p2 seul) ; voix dnb durée
     11 → 16,6 s, RMS −15,2 → −17,3 dB ; voix rave durée 3,1 → 1,1 s, RMS −16,1 → −23,3 dB,
     crête −0,1 → −2,9 dB ; effet rave RMS −16,1 → −27,2 dB, crête −0,1 → −7,1 dB.
   La règle est appliquée à la lettre (point 2 ci-dessous), mais elle ne garantit pas ce que
   « définitive » laisse entendre : le seuil « aucun pack > 90 % » laisse passer une ligne à 89 % dont
   le second pack n'a que 5 sons. Le marqueur à 75 % ne suffit pas : à 55-67 %, un pack décide encore.
   Rejouer : `python3 scripts/sons/fin/critique2_autres.py | grep IMPORTANT`.

## À corriger sans gravité

- Phrase « voix un peu plus fortes par défaut que les boucles » (Idées, voix) : les voix sont à
  −19,3 dB, plus douces que les boucles mélodiques (−14,1) et de batterie (−17,3). À reformuler.
- Tempo : un critère non écrit (accord ≥ 90 %) existe dans le calcul ; il ne touche qu'afro-house,
  qui n'a qu'un pack, donc sans effet. « tous » et techno sont refusés alors que 2 packs votent à
  ±2 % : plus strict que la règle, mais dit (« tous les packs à ±2 % », « tous : jamais une cible »).
- Quantiles de tempo non définitifs : afro-house p75 122 (moi 120), drift-phonk médiane 120 (moi 125).
- Les sons sans style (unlabelled) n'ont aucune ligne de style à eux (ex. 1 219 voix, 99 strings,
  99 effets) : choix défendable, mais à écrire en une ligne.
- 9 autres mesures sortent de p25–p75 sans écart important (crêtes à 0,1-0,9 dB près, keys lofi
  durée −18 %, boucles de batterie house) : sans effet sur une cible.

## Confirmé

- 481 lignes recalculées : 99 définitives, 3 tempos sûrs, 379 sans cible ; sons, packs, packs de 5,
  part du plus gros pack, liste des packs, p10 à p90 et accord identiques sur toutes les lignes hors tempo.
- Règle appliquée partout, sans exception à la main (statut recalculé = statut du tsv, 481 sur 481 hors tempo).
- Tempos sûrs : dnb 174, dubstep 140, house 125 (votes 125 et 124 à ±2 %). Techno 125/150 bien non sûr.
- 79 lignes définitives sur 99 tiennent sans leur plus gros pack (70 restent dans p25–p75, 9 en sortent
  de peu) ; 20 dépendent d'un seul pack (Grave 1).
- 20 chiffres de la conclusion relevés, tous justes à l'arrondi : bilan 99/3/379 ; basse 0,86 s,
  −6,8 dB ; keys −20,6 dB, 6,9 s ; synth −17,2 ; lead −16,5 ; pad 13,7 s (moitié 11,2-15 s) ; « keys
  3,4 dB plus doux que synth » ; boucles mélodiques 982, 7,7 s, −14 dB ; boucles de batterie 1 858,
  6,9 s, −17,3 dB ; voix 1 467, 4,7 s, −19,3, r7p2 83 % (1 221) ; effets 772, 7,7 s (3,4-11), −17,1 dB ;
  avant/après B1 (1 587 → 982 ; 845 → 1 467 ; coups « - » 105 → 88, 3,2 → 1,02 s) ; r7p2 à 71 % sans B1.
- Liste des lignes « portées surtout par un pack » (> 75 %) exacte : 38, 41, 251, 325, 348, 363, 383, 426.
- Les 6 graves de `CRITIQUE-AUTRES.md` sont corrigés : 1 tempo jamais « définitif » ; 2 vote par pack ;
  3 règle durcie (2 packs de 5, aucun > 90 %) appliquée ; 4 phrase « un seul pack : pas de cible »
  retirée ; 5 effet du déplacement écrit, chiffres justes ; 6 B1 dit comme changeant des cibles.
  Reste : le point 3 n'est corrigé que jusqu'à 90 % (voir Grave 1).
- B1 (622 sons vers « voix ») : tirage de 40 clés, graine 20261127, `random.sample(sorted(clés), 40)`.
  Tous en « voix », plus aucun en boucle ni en coup ; part chantée médiane 50,3 % (min 25 %), étendue
  5,2 demi-tons ; durée, crête et RMS identiques à l'ancienne table ; contre-mesure « yes » pour les 622.
  Chiffres de `DOUTES.md` (17 sons à 49,1 %, 605 à 55,2 %) retrouvés.

## Non vérifié

- Tempo : aucune contre-mesure indépendante n'existe (seulement étiquette contre estimation du même outil).
- MIDI, étendue (range_st) et part chantée (voiced_pct) : jamais contre-mesurés, pas de colonne.
- Étiquetage style et rôle des sons, et B1 lui-même : mesures de l'analyseur, aucune écoute.

VERDICT : il reste du grave.
