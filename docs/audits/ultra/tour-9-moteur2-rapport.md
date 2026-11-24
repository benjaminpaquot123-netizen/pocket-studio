# Tour 9, lot B2 « Moteur, optimiser » : rapport

Branche `eq/tour-9-moteur2`, partie de `eq/tour-9-moteur1` (6c1ba159). Cible : les trois briques les plus
chères du tableau de B1 (synth 16,9 %, fx-saturation 14,0 %, mixer-strips 12,4 % du total).

## Les trois optimisations (même son, au bit près)

1. **fx-saturation / clipper / distorsion : un drive au repos après un long silence** (`DriveShaper.h`).
   Quand l'entrée vaut exactement 0 pendant `kQuietFrames` = 2048 échantillons (≈ 43 ms), le drive saute
   ses filtres 4x et sa courbe et sort des zéros exacts ; son état est alors celui d'un drive neuf, donc
   le retour du son est identique au bit. Le repos tombe sur l'échantillon exact, quelle que soit la
   taille des blocs. Jamais au repos : un NaN, un décalage non nul, une courbe qui ne passe pas par 0.
   Tests : `m2_drive_rests_after_silence`, `m2_drive_rest_only_on_true_silence`,
   `m2_drive_rest_same_at_every_block_size` (le premier essai, compté par bloc, avait fait rougir
   `a4_saturator_block_sizes_agree` : c'est la preuve « rouge »).
2. **mixer-strips : envois muets sautés, gains posés en constante** (`Mixer.cpp`). Un envoi dont le gain
   courant et la cible valent 0 n'est plus calculé ; un gain arrivé à sa cible (cible finie) est pris en
   constante au lieu d'être lissé échantillon par échantillon.
3. **synth : filtre d'état réglé seulement quand la voix joue** (`Synth.h`, `Synth.cpp`). La fréquence
   calculée à chaque pas de contrôle est notée, et `Svf::SetFreq` (DaisySP, cher : un `sin` et un
   `pow`) n'est appelé qu'avant un `Process`, et seulement si elle a changé. `SetFreq` et `SetRes`
   recalculent tous deux l'amortissement depuis (res, freq) : l'ordre ne change rien au résultat.

Rien de neuf dans le callback n'alloue ni ne verrouille (tableaux sur la pile, booléens, flottants).

## Même son

Les 37 styles rendus 60 s par `ps_live` avant et après chaque optimisation : **37/37 fichiers WAV
identiques octet par octet** (`cmp`) ; écart = 0, donc ni NaN ni saturation de plus.

## Charge par style, avant / après

`JOBS=2 scripts/cpu-bench.sh --bricks` (blocs de 96, 2 passages, même machine : Xeon 2,3 GHz, 4 cœurs).
Sur un PC, c'est l'écart qui compte, pas le chiffre absolu.

| Style | Avant % | Après % | Écart (pts) |
|---|---:|---:|---:|
| jump_up | 11,8 | 7,1 | -4,7 |
| liquid_dnb | 9,6 | 8,1 | -1,5 |
| neurofunk | 9,1 | 6,8 | -2,3 |
| dnb | 8,2 | 6,2 | -2,0 |
| dubstep | 8,1 | 6,1 | -2,0 |
| reggaeton | 8,1 | 7,7 | -0,4 |
| riddim | 8,1 | 5,8 | -2,3 |
| pop | 8,0 | 7,5 | -0,5 |
| trap | 7,8 | 7,3 | -0,5 |
| synthwave | 7,6 | 7,3 | -0,3 |
| phonk | 7,6 | 7,6 | +0,0 |
| jungle | 7,3 | 5,5 | -1,8 |
| tech_house | 7,3 | 6,0 | -1,3 |
| drill | 7,2 | 6,6 | -0,6 |
| house | 7,1 | 6,1 | -1,0 |
| lofi | 6,9 | 7,0 | +0,1 |
| afro_house | 6,8 | 5,4 | -1,4 |
| uk_garage | 6,8 | 6,3 | -0,5 |
| speed_garage | 6,7 | 6,0 | -0,7 |
| amapiano | 6,6 | 5,8 | -0,8 |
| grime | 6,6 | 6,4 | -0,2 |
| disco | 6,4 | 5,8 | -0,6 |
| gabber | 6,4 | 5,5 | -0,9 |
| industrial_techno | 6,3 | 5,1 | -1,2 |
| deep_house | 6,3 | 6,0 | -0,3 |
| afrobeats | 6,0 | 5,8 | -0,2 |
| trance | 6,0 | 4,8 | -1,2 |
| hard_techno | 5,9 | 4,9 | -1,0 |
| boom_bap | 5,8 | 5,9 | +0,1 |
| jersey_club | 5,7 | 5,5 | -0,2 |
| techno | 5,7 | 4,6 | -1,1 |
| retro_techno | 5,2 | 4,7 | -0,5 |
| detroit | 5,1 | 4,6 | -0,5 |
| minimal | 4,8 | 4,2 | -0,6 |
| acidcore | 4,5 | 4,0 | -0,5 |
| acid_house | 4,3 | 4,1 | -0,2 |
| acid_techno | 4,1 | 3,8 | -0,3 |
| **moyenne** | 6,81 | 5,89 | -0,92 |
| **pire style** | 11,8 | 8,1 | -3,7 |

Briques (part du total, avant → après) : synth 16,9 → 14,7 ; fx-saturation 14,0 → 13,6 ;
mixer-strips 12,4 → 12,5 (part stable mais total plus petit : charge moyenne 0,8 → 0,7) ;
fx-clipper 11,3 → 8,5.

**Gain : −0,9 point de charge en moyenne (−13,5 %), −3,7 points sur le style le plus lourd
(jump_up 11,8 → 7,1 ; le plus lourd est maintenant liquid_dnb à 8,1).**

Le reste du coût de synth est dans DaisySP (Svf::Process, Ladder, Oscillator ; callgrind sur jump_up),
une brique tierce qu'on ne modifie pas.

## Vérifications
