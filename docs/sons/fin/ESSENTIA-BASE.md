# Base de sons mesurée avec Essentia

Date : 2026-11-27 (version corrigée après la critique à l'aveugle `ESSENTIA-CRITIQUE.md`). Script : `scripts/son/essentia/base.py`, tests : `scripts/son/essentia/test_base.py` (10 tests sur signaux synthétiques, tous verts). Les mesures maison ne servent plus : cette base les remplace.

## En bref
- **10 414 sons distincts** remesurés, sans aucune erreur : les 14 archives du stock (24 dossiers d'origine). Les copies exactes (même CRC, même taille) ne comptent qu'une fois.
- **70 familles** après le nouveau classement (88 avant) ; les 808 ont leur famille ; la case `melodic_loop/autre` passe de 643 à 16 sons.
- **Contre-mesure librosa indépendante** (son propre chargement, ses trames, sa porte) : brillance 99,9 %, coupure 85 % 99,4 %, platitude 100 %, passages par zéro 99,7 %, RMS 100 % sur toute la base.
- **Hauteur** : 97,1 % d'accord sur les trames voisées communes. Par son (≥ 80 % des trames d'accord) : 61,6 % seulement, à cause des basses (voir « Trous »).
- **Tempo** : face au BPM écrit dans le nom du fichier, Essentia trouve 66,7 % des boucles de batterie en strict et 84,1 % en repliant ; librosa 53,0 % et 81,8 %. Entre les deux outils : 49,3 % strict, 66,0 % replié.
- **Note** des one-shots mélodiques face au nom du fichier : Essentia 81,1 %, librosa 79,6 % (280 sons).

## Corrections apportées (critique à l'aveugle)
Chaque correction a un commit « rouge: » (test qui montre le bogue) puis « vert: ».

| # | Bogue | Correction |
| --- | --- | --- |
| 0 | Garde-fou de noms absent de la lignée | repris (cherry-pick) ; il passe sur tout l'arbre |
| 1 | `EnergyBand` recevait le spectre au carré, alors qu'il met déjà au carré : parts de bande à la puissance 4 | il reçoit le spectre ; parts de bande recalculées |
| 2 | `EffectiveDuration` lisait le signal brut (quelques ms pour un son qui dure) | il lit l'enveloppe |
| 3 | Coups courts décrits par leur queue ; coupure basse des kicks bloquée sur la case de 21,5 Hz | mode « coup » (ci-dessous) ; coupure des one-shots sur un spectre fin (2,7 Hz) |
| 4 | Contre-mesure non indépendante (librosa relisait le tableau d'Essentia) ; pas de vérité terrain ; pas de tempo de librosa trop grossier (pas de 512 : ±2,8 %) ; « replié » non calculé | librosa recharge et rééchantillonne lui-même (soundfile + soxr), ses trames, sa porte ; vérité terrain tirée des noms ; battue au pas de 128 échantillons (±0,7 % à 145 BPM) ; repli calculé |
| 5 | 808 noyés dans les basses et les kicks ; 643 sons en « autre » ; familles absurdes | règles de famille (ci-dessous) |
| 6 | (vu pendant la remesure) la platitude dépendait de la fréquence d'origine : un son à 48 kHz rééchantillonné perd la bande au-dessus de 20 kHz, −1 à −2 dB de platitude | platitude calculée jusqu'à 19 kHz seulement, chez les deux outils |

## Méthode
- **Lecture** : chaque son est lu en place dans son archive, sans extraction ; 30 premières secondes ; mono ; rééchantillonné à 44,1 kHz par `Resample` d'Essentia.
- **Deux modes de trames, fixés exprès** :
  - **mode « long »** (boucles, phrases, sons de plus de 0,5 s hors batterie) : trame 2048, pas 512, porte à −60 dB sous la trame la plus forte ;
  - **mode « coup »** (tous les coups de batterie, et tout son de moins de 0,5 s) : trame 512 complétée de zéros jusqu'à 2048, pas 128, porte à −20 dB sous la trame la plus forte. Raison : un charley de 30 ms suivi d'une queue faible était décrit par sa queue ; la porte relative au pic garde le corps du son.
  - Toute comparaison future doit garder ces réglages (les parts de bande changent avec la trame).
- **Coupure à 85 % des one-shots** : un seul spectre du corps (zone ouverte par la porte, Hann, 2 s au plus) complété jusqu'à 16 384 points, soit 2,7 Hz par case ; la tolérance de la contre-mesure tient compte de la case la plus grossière des deux.
- **Mesures Essentia (médiane sur les trames actives)** : `Centroid`, `RollOff` (85 %), `Flatness` (jusqu'à 19 kHz), `ZeroCrossingRate`, `RMS`, parts de bande `EnergyBand` 20-250, 250-4000, 4000-20000 Hz, MFCC 1 à 5.
- **Sur le son entier** : `LoudnessEBUR128` intégré (sons d'au moins 0,4 s), `LogAttackTime`, `EffectiveDuration` sur l'enveloppe, crête.
- **Hauteur** : `PitchYin` (trames de 4096, 3 premières secondes, confiance > 0,5), seulement pour les one-shots mélodiques (808 compris).
- **Boucles de 4 s ou plus** : `RhythmExtractor2013` (degara) pour le BPM, `KeyExtractor` pour la tonalité.

## Contre-mesure librosa (indépendante)
librosa lit le fichier lui-même (soundfile, rééchantillonnage soxr), calcule sa STFT centrée avec ses propres trames (mêmes tailles par mode) et choisit ses trames actives avec sa propre porte. Aucun tableau n'est partagé avec Essentia ; un test le prouve en faussant le rééchantillonnage d'Essentia.

| Mesure | Tolérance |
| --- | --- |
| brillance | ±5 % ou ±25 Hz |
| coupure 85 % | ±5 % ou 1,5 case de la plus grossière des deux |
| passages par zéro | ±5 % ou ±0,002 |
| platitude | ±1 dB |
| RMS | ±0,5 dB |
| f0 (`pyin`) | ±50 cents ; `f0_hz_octave` pardonne les octaves |
| f0 par son (`f0_trames`) | ≥ 80 % des trames voisées (par l'un ou l'autre outil) à ±50 cents |
| BPM (`beat_track`, pas 128) | `bpm` strict ±2 % ; `bpm_replie` ±2 % après ×½, ×⅔, ×1, ×1,5, ×2 |

Accord sur toute la base (`essentia-accord.tsv`) :

| Mesure | Comparés | Accord |
| --- | ---: | ---: |
| centroid_hz | 10412 | 99.9 % |
| rolloff_hz | 10412 | 99.4 % |
| flatness_db | 10412 | 100.0 % |
| zcr | 10412 | 99.7 % |
| rms_db | 10412 | 100.0 % |
| f0_hz | 1515 | 92.7 % |
| f0_hz_octave | 1515 | 93.5 % |
| f0_common_hz | 1505 | 97.1 % |
| f0_trames | 1541 | 61.6 % |
| bpm | 4071 | 49.3 % |
| bpm_replie | 4071 | 66.0 % |

## Vérité terrain (noms de fichiers)
Le BPM (« 145 BPM », ou un seul nombre de 60 à 200 dans le nom d'une boucle) et la note (lettre + altération, avec octave ou mode facultatifs) écrits dans le nom servent de vérité. Un nom ambigu (deux tempos possibles, lettre de variante comme « Kick_A ») ne donne pas de vérité. Comptés seulement en agrégé (`essentia-verite.tsv`), jamais son par son.

| Type | Mesure | Sons avec vérité | Essentia | librosa |
| --- | --- | ---: | ---: | ---: |
| drum_loop | bpm | 1907 | 66.7 % | 53.0 % |
| drum_loop | bpm replié | 1907 | 84.1 % | 81.8 % |
| kit_stem | bpm | 1346 | 43.1 % | 33.0 % |
| kit_stem | bpm replié | 1346 | 70.8 % | 60.7 % |
| melodic_loop | bpm | 926 | 46.0 % | 25.9 % |
| melodic_loop | bpm replié | 926 | 65.2 % | 51.1 % |
| oneshot_melodic | note | 280 | 81.1 % | 79.6 % |

## Règles de famille
Famille de départ : `drum_oneshot/<sous-type>` pour les coups de batterie, `<type>/<rôle>` pour le reste, classés par les tables `docs/sons/passe2/tout/*.csv` (ou `tout.classify` quand un chemin n'y est pas). Puis, dans l'ordre, sur des mots génériques du nom de fichier seulement (jamais un nom de pack ni de vendeur) :
1. **Variantes** : `kick:attack`, `kick:tail`, etc. rejoignent leur famille (`kick`).
2. **808** : « 808 » isolé dans le chemin d'un kick 808, d'une basse, d'un synthé ou d'un « autre » donne `oneshot_melodic/808` (un 808 est une basse accordée, pas un coup) ou `<type>/808` pour une boucle. Un charley ou une caisse « 808 » (boîte à rythmes) reste dans sa famille.
3. **Fusions** de familles absurdes ou trop petites : `drum_loop/pad` → `melodic_loop/pad` ; `drum_loop/synth` → `drum_loop/full` ; `fx/drums` → `fx/fx` ; `fx/chords` → `melodic_loop/chords` ; `melodic_loop/drums` → `drum_loop/drums` (top loops) ; `melodic_loop/perc` → `drum_loop/perc` ; `oneshot_melodic/drums` → `oneshot_melodic/bell` ; `perc_high/mid/low` → `perc` ; `snap` → `clap` ; `melodic_loop/vocal` → `vocal/vocal`.
4. **Cas nommés** : `oneshot_melodic/perc` → `fx/fx` si le nom dit fx, riser, sweep, impact, noise ou screech, sinon `bell` ; `oneshot_melodic/full` avec « bass » → `bass` ; `fx/fx` avec « bass hit/shot » → `oneshot_melodic/bass`.
5. **« autre »** : premier mot trouvé — voix (adlib, sung, vocal, vox, choir, chant → `vocal/vocal`), cordes, cuivres et bois, cloches et lames (kalimba, chime, vibraphone…), claviers, guitares, pads, percussions (claves, darbouka).

## Sons par type
| Type | Sons |
| --- | ---: |
| drum_oneshot | 2403 |
| drum_loop | 1909 |
| oneshot_melodic | 1541 |
| vocal | 1471 |
| kit_stem | 1394 |
| melodic_loop | 936 |
| fx | 760 |
| **Total** | **10414** |

Styles (tables existantes ; détail par famille dans `essentia-styles.tsv`) : sans étiquette 1 924, dubstep 1 339, multi 1 179, dnb 958, techno 775, house 706, drift-phonk 644, rave 635, amapiano 482, afro-house 338, lofi 319, trap 316, edm 307, synthwave 234, hardstyle 214, hiphop, ambient, rnb, funk et disco 44.

## Accord par famille
Familles d'au moins 5 sons. « Spectre » = le plus faible des 5 accords spectre et niveau. Percentiles p10 à p90 de chaque mesure : `essentia-familles.tsv`.

| Famille | Sons | Packs | Spectre (min des 5) | Platitude | f0 (communes) | f0 trames | BPM strict | BPM replié |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| drum_loop/drums | 452 | 12 | 99.4 % | 100.0 % | – | – | 64 % (n=452) | 83 % (n=452) |
| drum_loop/full | 217 | 9 | 100.0 % | 100.0 % | – | – | 70 % (n=217) | 88 % (n=217) |
| drum_loop/hats | 361 | 8 | 96.4 % | 100.0 % | – | – | 77 % (n=361) | 90 % (n=361) |
| drum_loop/kick | 349 | 12 | 99.1 % | 100.0 % | – | – | 63 % (n=349) | 76 % (n=349) |
| drum_loop/perc | 261 | 7 | 99.2 % | 100.0 % | – | – | 54 % (n=261) | 62 % (n=261) |
| drum_loop/snare | 172 | 8 | 98.9 % | 100.0 % | – | – | 38 % (n=172) | 78 % (n=172) |
| drum_oneshot/clap | 310 | 13 | 94.8 % | 100.0 % | – | – | – | – |
| drum_oneshot/cowbell | 67 | 3 | 98.5 % | 100.0 % | – | – | – | – |
| drum_oneshot/crash | 65 | 9 | 100.0 % | 100.0 % | – | – | – | – |
| drum_oneshot/cymbal | 20 | 2 | 100.0 % | 100.0 % | – | – | – | – |
| drum_oneshot/hat_closed | 246 | 14 | 100.0 % | 100.0 % | – | – | – | – |
| drum_oneshot/hat_open | 160 | 9 | 100.0 % | 100.0 % | – | – | – | – |
| drum_oneshot/kick | 512 | 14 | 99.0 % | 99.8 % | – | – | – | – |
| drum_oneshot/perc | 344 | 13 | 97.1 % | 100.0 % | – | – | – | – |
| drum_oneshot/ride | 105 | 7 | 99.0 % | 100.0 % | – | – | – | – |
| drum_oneshot/rim | 5 | 2 | 100.0 % | 100.0 % | – | – | – | – |
| drum_oneshot/shaker | 106 | 5 | 99.1 % | 100.0 % | – | – | – | – |
| drum_oneshot/snare | 409 | 11 | 93.6 % | 100.0 % | – | – | – | – |
| drum_oneshot/tom | 54 | 2 | 100.0 % | 100.0 % | – | – | – | – |
| fx/fx | 760 | 17 | 99.7 % | 100.0 % | – | – | – | – |
| kit_stem/arp | 28 | 5 | 100.0 % | 100.0 % | – | – | 79 % (n=28) | 79 % (n=28) |
| kit_stem/autre | 14 | 1 | 100.0 % | 100.0 % | – | – | 0 % (n=14) | 7 % (n=14) |
| kit_stem/bass | 222 | 9 | 100.0 % | 100.0 % | – | – | 36 % (n=222) | 52 % (n=222) |
| kit_stem/bell | 15 | 2 | 100.0 % | 100.0 % | – | – | 60 % (n=15) | 67 % (n=15) |
| kit_stem/brass | 10 | 4 | 100.0 % | 100.0 % | – | – | 20 % (n=10) | 60 % (n=10) |
| kit_stem/chords | 118 | 8 | 100.0 % | 100.0 % | – | – | 42 % (n=118) | 63 % (n=118) |
| kit_stem/drums | 30 | 7 | 100.0 % | 100.0 % | – | – | 57 % (n=30) | 87 % (n=30) |
| kit_stem/full | 208 | 9 | 100.0 % | 100.0 % | – | – | 53 % (n=208) | 76 % (n=208) |
| kit_stem/fx | 24 | 6 | 100.0 % | 100.0 % | – | – | 8 % (n=24) | 25 % (n=24) |
| kit_stem/guitar | 15 | 2 | 100.0 % | 100.0 % | – | – | 47 % (n=15) | 87 % (n=15) |
| kit_stem/hats | 43 | 7 | 100.0 % | 100.0 % | – | – | 79 % (n=43) | 84 % (n=43) |
| kit_stem/keys | 83 | 6 | 100.0 % | 100.0 % | – | – | 41 % (n=83) | 68 % (n=83) |
| kit_stem/kick | 20 | 7 | 100.0 % | 100.0 % | – | – | 70 % (n=20) | 70 % (n=20) |
| kit_stem/lead | 231 | 9 | 100.0 % | 100.0 % | – | – | 40 % (n=231) | 55 % (n=231) |
| kit_stem/pad | 29 | 8 | 100.0 % | 100.0 % | – | – | 7 % (n=29) | 41 % (n=29) |
| kit_stem/perc | 14 | 3 | 100.0 % | 100.0 % | – | – | 64 % (n=14) | 79 % (n=14) |
| kit_stem/pluck | 21 | 4 | 100.0 % | 100.0 % | – | – | 24 % (n=21) | 33 % (n=21) |
| kit_stem/snare | 18 | 7 | 100.0 % | 100.0 % | – | – | 50 % (n=18) | 94 % (n=18) |
| kit_stem/strings | 149 | 3 | 100.0 % | 100.0 % | – | – | 23 % (n=149) | 33 % (n=149) |
| kit_stem/synth | 15 | 6 | 100.0 % | 100.0 % | – | – | 7 % (n=15) | 27 % (n=15) |
| kit_stem/vocal | 76 | 6 | 100.0 % | 100.0 % | – | – | 29 % (n=76) | 40 % (n=76) |
| melodic_loop/808 | 3 | 2 | 100.0 % | 100.0 % | – | – | 33 % (n=3) | 67 % (n=3) |
| melodic_loop/arp | 2 | 2 | 100.0 % | 100.0 % | – | – | 100 % (n=2) | 100 % (n=2) |
| melodic_loop/autre | 16 | 3 | 100.0 % | 100.0 % | – | – | 44 % (n=16) | 69 % (n=16) |
| melodic_loop/bass | 296 | 12 | 100.0 % | 100.0 % | – | – | 31 % (n=296) | 45 % (n=296) |
| melodic_loop/bell | 12 | 1 | 100.0 % | 100.0 % | – | – | 33 % (n=12) | 50 % (n=12) |
| melodic_loop/brass | 9 | 2 | 100.0 % | 100.0 % | – | – | 67 % (n=9) | 67 % (n=9) |
| melodic_loop/chords | 13 | 2 | 100.0 % | 100.0 % | – | – | 8 % (n=13) | 23 % (n=13) |
| melodic_loop/guitar | 5 | 3 | 100.0 % | 100.0 % | – | – | 20 % (n=5) | 40 % (n=5) |
| melodic_loop/keys | 53 | 4 | 100.0 % | 100.0 % | – | – | 28 % (n=53) | 55 % (n=53) |
| melodic_loop/lead | 244 | 9 | 100.0 % | 100.0 % | – | – | 42 % (n=244) | 59 % (n=244) |
| melodic_loop/pad | 117 | 8 | 100.0 % | 100.0 % | – | – | 15 % (n=117) | 27 % (n=117) |
| melodic_loop/pluck | 17 | 1 | 100.0 % | 100.0 % | – | – | 47 % (n=17) | 76 % (n=17) |
| melodic_loop/strings | 2 | 1 | 100.0 % | 100.0 % | – | – | 50 % (n=2) | 50 % (n=2) |
| melodic_loop/synth | 87 | 4 | 100.0 % | 100.0 % | – | – | 52 % (n=87) | 67 % (n=87) |
| oneshot_melodic/808 | 21 | 2 | 100.0 % | 100.0 % | 95.2 % (n=21) | 87.0 % (n=23) | – | – |
| oneshot_melodic/arp | 19 | 5 | 100.0 % | 100.0 % | 94.7 % (n=19) | 94.7 % (n=19) | – | – |
| oneshot_melodic/autre | 1 | 1 | 100.0 % | 100.0 % | 100.0 % (n=1) | 100.0 % (n=1) | – | – |
| oneshot_melodic/bass | 757 | 16 | 99.6 % | 100.0 % | 97.8 % (n=733) | 40.0 % (n=757) | – | – |
| oneshot_melodic/bell | 18 | 4 | 100.0 % | 100.0 % | 87.5 % (n=16) | 72.2 % (n=18) | – | – |
| oneshot_melodic/brass | 11 | 4 | 100.0 % | 100.0 % | 100.0 % (n=11) | 90.9 % (n=11) | – | – |
| oneshot_melodic/chords | 68 | 6 | 100.0 % | 100.0 % | 100.0 % (n=67) | 91.2 % (n=68) | – | – |
| oneshot_melodic/guitar | 15 | 2 | 100.0 % | 100.0 % | 100.0 % (n=15) | 93.3 % (n=15) | – | – |
| oneshot_melodic/keys | 99 | 7 | 100.0 % | 100.0 % | 98.0 % (n=99) | 92.9 % (n=99) | – | – |
| oneshot_melodic/lead | 139 | 9 | 99.3 % | 100.0 % | 97.1 % (n=139) | 84.2 % (n=139) | – | – |
| oneshot_melodic/pad | 61 | 7 | 100.0 % | 100.0 % | 98.4 % (n=61) | 90.2 % (n=61) | – | – |
| oneshot_melodic/pluck | 12 | 2 | 100.0 % | 100.0 % | 100.0 % (n=12) | 83.3 % (n=12) | – | – |
| oneshot_melodic/strings | 178 | 3 | 100.0 % | 100.0 % | 99.4 % (n=178) | 86.0 % (n=178) | – | – |
| oneshot_melodic/synth | 136 | 11 | 100.0 % | 100.0 % | 88.7 % (n=133) | 58.6 % (n=140) | – | – |
| vocal/vocal | 1469 | 12 | 99.9 % | 100.0 % | – | – | – | – |

## Trous
- **Hauteur par son sur les basses** : `oneshot_melodic/bass` n'a que 40 % de sons dont les deux suivis de f0 s'accordent sur ≥ 80 % des trames (synthé 58,6 %). La médiane, elle, s'accorde à 97,8 % : les écarts viennent de trames isolées (glissés, sous-graves sous 40 Hz, attaque bruitée). Ne pas utiliser la f0 trame par trame des basses.
- **Mode « coup » et contre-mesure** : avec des trames de 512 et une porte à −20 dB, quelques coups très brefs s'accordent moins (spectre : caisse claire 93,6 %, clap 94,8 %, percussions 97,1 %, boucles de charley 96,4 %). Les écarts portent sur la coupure et les passages par zéro des sons de quelques trames ; le corps est bien décrit (test du charley à queue faible).
- **Tempo** : même contre la vérité terrain, Essentia rate un tiers des boucles de batterie en strict (16 % en replié) et plus de la moitié des boucles mélodiques ; librosa fait moins bien. Les BPM ne sont fiables qu'avec le nom du fichier.
- **Styles** : 1 924 sons sans étiquette ; le style n'est connu que par les tables existantes.

## Fichiers
- `essentia-familles.tsv` : percentiles p10..p90 par famille et mesure (familles d'au moins 5 sons).
- `essentia-styles.tsv` : sons par famille et style.
- `essentia-accord.tsv` : accord Essentia / librosa par famille et mesure.
- `essentia-verite.tsv` : accord avec la vérité terrain, par type (agrégé).
- Aucune mesure d'un son précis n'est dans le dépôt.
