# Base de sons mesurée avec Essentia

Date : 2026-11-27. Script : `scripts/son/essentia/base.py`. Les mesures maison ne servent plus : cette base les remplace.

## En bref
- **10 414 sons distincts** mesurés, sans aucune erreur : les 14 archives du stock, soit les 24 dossiers d'origine. Les copies exactes (même CRC, même taille) ne sont comptées qu'une fois.
- **Tous les instruments sont couverts** : batterie, basses, claviers, pads, leads, voix, effets, boucles et pistes séparées de kits. Il y a 88 familles et 17 packs au plus par famille.
- **Contre-mesure librosa sur le spectre et le niveau** (brillance, coupure à 85 %, platitude, passages par zéro, niveau RMS) : de 99,9 à 100 % d'accord sur toute la base. Le critère de ≥ 98 % est tenu dans chaque famille d'au moins 5 sons.
- **Hauteur (f0)** : 97,1 % d'accord sur les trames voisées communes, 97,9 % en repliant les octaves. Le critère n'est pas tenu dans 8 familles de sons mélodiques (voir « Trous »).
- **Tempo** : 50 % d'accord seulement. Les deux battues sont trop différentes pour qu'on s'y fie (voir « Trous »).

## Méthode
- **Lecture** : chaque son est lu en place dans son archive, sans extraction. On garde les 30 premières secondes.
- **Classement** : on reprend les tables existantes `docs/sons/passe2/tout/*.csv` (type, rôle, sous-type, style), retrouvées par `file_id`. Pour un dossier (l9) dont les chemins ne retrouvent aucune ligne, on reclasse avec `tout.classify` (scripts/sons), la même règle que celle qui a fait les tables.
- **Famille** : `drum_oneshot/<sous-type>` pour les coups de batterie, `<type>/<rôle>` pour tout le reste (`autre` quand le rôle est vide).
- **Réglages fixes, identiques pour tous les sons** :
  - signal ramené en mono, rééchantillonné à 44,1 kHz par `Resample` d'Essentia, canal par canal ;
  - trame de 2048 points, pas de 512, fenêtre de Hann non normalisée ;
  - trames centrées sur k·512, avec k ≤ longueur/512. Le générateur d'Essentia ajoute une dernière trame presque vide : on l'enlève, sinon elle fausse les coups très courts ;
  - seules les trames actives comptent : celles dont le RMS est à moins de 60 dB du RMS de la trame la plus forte.

  **La taille de trame est fixée exprès.** Les parts de bande d'Essentia (`EnergyBand` 20-250, 250-4000 et 4000-20000 Hz) changent avec elle quand la bande ne porte que le bord du son (par exemple le médium d'un kick). Toute comparaison future doit garder 2048 / 512 / 44,1 kHz.
- **Mesures Essentia (une médiane par son, sur les trames actives)** :
  - `Centroid`, `RollOff` (85 % de l'énergie), `Flatness` (sur la puissance), `ZeroCrossingRate`, `RMS` ;
  - parts de bande basse, médium et aigu (`EnergyBand`) ;
  - MFCC 1 à 5.
- **Mesures Essentia sur le son entier** :
  - `LoudnessEBUR128` intégré (LUFS, sons d'au moins 0,4 s) ;
  - `LogAttackTime`, `EffectiveDuration`, crête.
- **Hauteur** : `PitchYin`, dans le domaine temporel, sur des trames de 4096 points, sur les 3 premières secondes, en gardant les valeurs de confiance > 0,5. Elle n'est mesurée que pour les sons mélodiques, les 808 et les basses. `PitchYinFFT` a été écarté : il donnait des valeurs fausses de 50 à 120 cents sous 70 Hz.
- **Boucles de 4 s ou plus** : `RhythmExtractor2013` (méthode degara) pour le BPM, `KeyExtractor` pour la tonalité.

## Contre-mesure (librosa 1.0.1)
On reprend les mêmes trames, avec STFT centrée et remplissage par zéros. Pour librosa, la coupure se calcule sur la puissance, comme chez Essentia. librosa choisit ses propres trames actives.

Tolérances :

| Mesure | Tolérance |
| --- | --- |
| brillance, coupure 85 % | ±5 % ou ±25 Hz |
| passages par zéro | ±5 % ou ±0,002 |
| platitude | ±1 dB |
| RMS | ±0,5 dB |
| f0 (`pyin`) | ±50 cents |
| BPM (`beat_track`) | ±2 % après repli ×½ / ×1 / ×2 |

Pour f0, deux lignes figurent dans `essentia-accord.tsv`. `f0_hz` compare chaque outil sur ses propres trames voisées. `f0_common_hz` compare seulement les trames que les deux outils jugent voisées : c'est la ligne qui fait foi.

Une première passe sans la correction de la dernière trame donnait 94 à 97 % sur les coups courts (kick, caisse claire, percussions, shakers). Les trames étaient alignées, mais Essentia en avait une de plus, presque vide, en fin de son. Après la correction, les sons de moins de 3 s ont été remesurés : 3 953 sons. Les sons plus longs ont gardé leur mesure, car une trame de plus sur au moins 260 ne déplace pas la médiane.

Accord sur toute la base :

| Mesure | Comparés | Accord |
| --- | ---: | ---: |
| centroid_hz | 10412 | 100.0 % |
| rolloff_hz | 10412 | 99.9 % |
| flatness_db | 10412 | 99.9 % |
| zcr | 10412 | 99.9 % |
| rms_db | 10412 | 100.0 % |
| f0_hz | 2076 | 92.4 % |
| f0_common_hz | 2066 | 97.1 % |
| bpm | 4479 | 50.1 % |

## Sons par type
| Type | Sons |
| --- | ---: |
| drum_oneshot | 2426 |
| drum_loop | 1858 |
| melodic_loop | 1604 |
| oneshot_melodic | 1514 |
| kit_stem | 1396 |
| vocal | 844 |
| fx | 772 |
| **Total** | **10414** |

Styles (rangement des tables existantes, détail par famille dans `essentia-styles.tsv`) :

| Style | Sons |
| --- | ---: |
| sans étiquette | 1 924 |
| dubstep | 1 339 |
| multi | 1 179 |
| dnb | 958 |
| techno | 775 |
| house | 706 |
| drift-phonk | 644 |
| rave | 635 |
| amapiano | 482 |
| afro-house | 338 |
| lofi | 319 |
| trap | 316 |
| edm | 307 |
| synthwave | 234 |
| hardstyle | 214 |
| hiphop, ambient, rnb, funk, disco | 44 |

## Accord par famille
« Spectre » donne le plus faible des 5 accords spectre et niveau.

| Famille | Sons | Packs | Spectre (min des 5) | f0 | BPM |
| --- | ---: | ---: | ---: | ---: | ---: |
| drum_loop/drums | 519 | 12 | 99.6 % | – | 63 % (n=435) |
| drum_loop/full | 214 | 8 | 99.5 % | – | 76 % (n=214) |
| drum_loop/hats | 362 | 8 | 100.0 % | – | 83 % (n=361) |
| drum_loop/kick | 350 | 12 | 99.1 % | – | 65 % (n=349) |
| drum_loop/pad | 6 | 3 | 100.0 % | – | 0 % (n=6) |
| drum_loop/perc | 224 | 6 | 100.0 % | – | 42 % (n=221) |
| drum_loop/snare | 180 | 8 | 99.4 % | – | 69 % (n=172) |
| drum_loop/synth | 3 | 1 | 100.0 % | – | 100 % (n=3) |
| drum_oneshot/clap | 298 | 13 | 100.0 % | – | – |
| drum_oneshot/cowbell | 67 | 3 | 100.0 % | – | – |
| drum_oneshot/crash | 65 | 9 | 100.0 % | – | – |
| drum_oneshot/cymbal | 20 | 2 | 100.0 % | – | – |
| drum_oneshot/hat_closed | 245 | 14 | 100.0 % | – | – |
| drum_oneshot/hat_closed:attack | 1 | 1 | 100.0 % | – | – |
| drum_oneshot/hat_open | 160 | 9 | 100.0 % | – | – |
| drum_oneshot/kick | 505 | 14 | 99.8 % | – | – |
| drum_oneshot/kick:attack | 6 | 2 | 100.0 % | – | – |
| drum_oneshot/kick:tail | 1 | 1 | 100.0 % | – | – |
| drum_oneshot/kick_808 | 23 | 2 | 100.0 % | 95.2 % (n=21) | – |
| drum_oneshot/perc | 329 | 13 | 99.4 % | – | – |
| drum_oneshot/perc_high | 5 | 1 | 100.0 % | – | – |
| drum_oneshot/perc_low | 5 | 1 | 100.0 % | – | – |
| drum_oneshot/perc_mid | 5 | 1 | 100.0 % | – | – |
| drum_oneshot/ride | 105 | 7 | 100.0 % | – | – |
| drum_oneshot/rim | 5 | 2 | 100.0 % | – | – |
| drum_oneshot/shaker | 106 | 5 | 100.0 % | – | – |
| drum_oneshot/snap | 12 | 3 | 100.0 % | – | – |
| drum_oneshot/snare | 408 | 11 | 100.0 % | – | – |
| drum_oneshot/snare:attack | 1 | 1 | 100.0 % | – | – |
| drum_oneshot/tom | 54 | 2 | 100.0 % | – | – |
| fx/chords | 6 | 1 | 100.0 % | – | – |
| fx/drums | 3 | 1 | 66.7 % (n=3) | – | – |
| fx/fx | 763 | 17 | 100.0 % | – | – |
| kit_stem/arp | 28 | 5 | 100.0 % | – | 43 % (n=28) |
| kit_stem/autre | 57 | 4 | 100.0 % | – | 26 % (n=57) |
| kit_stem/bass | 222 | 9 | 100.0 % | 98.2 % (n=218) | 34 % (n=222) |
| kit_stem/bell | 12 | 2 | 100.0 % | – | 67 % (n=12) |
| kit_stem/brass | 7 | 4 | 100.0 % | – | 71 % (n=7) |
| kit_stem/chords | 118 | 8 | 100.0 % | – | 44 % (n=118) |
| kit_stem/drums | 37 | 7 | 100.0 % | – | 80 % (n=30) |
| kit_stem/full | 208 | 9 | 100.0 % | – | 60 % (n=208) |
| kit_stem/fx | 26 | 6 | 100.0 % | – | 25 % (n=24) |
| kit_stem/guitar | 11 | 2 | 100.0 % | – | 64 % (n=11) |
| kit_stem/hats | 43 | 7 | 100.0 % | – | 74 % (n=43) |
| kit_stem/keys | 83 | 6 | 100.0 % | – | 57 % (n=83) |
| kit_stem/kick | 20 | 7 | 100.0 % | – | 70 % (n=20) |
| kit_stem/lead | 231 | 9 | 100.0 % | – | 46 % (n=231) |
| kit_stem/pad | 29 | 8 | 100.0 % | – | 21 % (n=29) |
| kit_stem/perc | 10 | 3 | 100.0 % | – | 80 % (n=10) |
| kit_stem/pluck | 21 | 4 | 100.0 % | – | 33 % (n=21) |
| kit_stem/snare | 18 | 7 | 100.0 % | – | 89 % (n=18) |
| kit_stem/strings | 122 | 3 | 100.0 % | – | 34 % (n=122) |
| kit_stem/synth | 15 | 6 | 100.0 % | – | 20 % (n=15) |
| kit_stem/vocal | 78 | 6 | 100.0 % | – | 34 % (n=76) |
| melodic_loop/808 | 3 | 2 | 100.0 % | 100.0 % (n=3) | 67 % (n=3) |
| melodic_loop/arp | 2 | 2 | 100.0 % | – | 100 % (n=2) |
| melodic_loop/autre | 643 | 4 | 100.0 % | – | 12 % (n=429) |
| melodic_loop/bass | 351 | 12 | 100.0 % | 96.2 % (n=344) | 38 % (n=296) |
| melodic_loop/bell | 12 | 1 | 100.0 % | – | 33 % (n=12) |
| melodic_loop/brass | 7 | 2 | 100.0 % | – | 43 % (n=7) |
| melodic_loop/chords | 7 | 2 | 100.0 % | – | 14 % (n=7) |
| melodic_loop/drums | 17 | 2 | 100.0 % | – | 59 % (n=17) |
| melodic_loop/guitar | 3 | 2 | 100.0 % | – | 33 % (n=3) |
| melodic_loop/keys | 53 | 4 | 100.0 % | – | 49 % (n=53) |
| melodic_loop/lead | 245 | 9 | 100.0 % | – | 43 % (n=244) |
| melodic_loop/pad | 112 | 8 | 100.0 % | – | 19 % (n=111) |
| melodic_loop/perc | 40 | 1 | 100.0 % | – | 58 % (n=40) |
| melodic_loop/pluck | 17 | 1 | 100.0 % | – | 71 % (n=17) |
| melodic_loop/strings | 2 | 1 | 100.0 % | – | 50 % (n=2) |
| melodic_loop/synth | 87 | 4 | 100.0 % | – | 58 % (n=87) |
| melodic_loop/vocal | 3 | 2 | 100.0 % | – | 33 % (n=3) |
| oneshot_melodic/arp | 19 | 5 | 100.0 % | 94.7 % (n=19) | – |
| oneshot_melodic/autre | 86 | 3 | 100.0 % | 97.7 % (n=86) | – |
| oneshot_melodic/bass | 749 | 16 | 100.0 % | 97.9 % (n=725) | – |
| oneshot_melodic/bell | 12 | 2 | 100.0 % | 91.7 % (n=12) | – |
| oneshot_melodic/brass | 8 | 4 | 100.0 % | 100.0 % (n=8) | – |
| oneshot_melodic/chords | 68 | 6 | 100.0 % | 100.0 % (n=67) | – |
| oneshot_melodic/drums | 1 | 1 | 100.0 % | 100.0 % (n=1) | – |
| oneshot_melodic/full | 1 | 1 | 100.0 % | 100.0 % (n=1) | – |
| oneshot_melodic/guitar | 11 | 2 | 100.0 % | 100.0 % (n=11) | – |
| oneshot_melodic/keys | 98 | 7 | 100.0 % | 98.0 % (n=98) | – |
| oneshot_melodic/lead | 139 | 9 | 99.3 % | 97.1 % (n=139) | – |
| oneshot_melodic/pad | 61 | 7 | 100.0 % | 98.4 % (n=61) | – |
| oneshot_melodic/perc | 3 | 2 | 100.0 % | 100.0 % (n=1) | – |
| oneshot_melodic/pluck | 12 | 2 | 100.0 % | 100.0 % (n=12) | – |
| oneshot_melodic/strings | 106 | 3 | 100.0 % | 100.0 % (n=106) | – |
| oneshot_melodic/synth | 140 | 11 | 100.0 % | 88.7 % (n=133) | – |
| vocal/vocal | 844 | 11 | 99.4 % | – | – |

## Trous (dit franchement)
- **Hauteur** : sous 98 % dans 8 familles mélodiques :

  | Famille | Accord f0 |
  | --- | ---: |
  | oneshot_melodic/synth | 88,7 % |
  | oneshot_melodic/bell | 91,7 % |
  | oneshot_melodic/arp | 94,7 % |
  | drum_oneshot/kick_808 | 95,2 % |
  | melodic_loop/bass | 96,2 % |
  | oneshot_melodic/lead | 97,1 % |
  | oneshot_melodic/autre | 97,7 % |
  | oneshot_melodic/bass | 97,9 % |

  Les écarts viennent surtout des sons à plusieurs notes ou très riches (accords de synthé, cloches, boucles de basse), où il n'existe pas une seule bonne f0. Une partie est une erreur d'octave : en repliant les octaves, l'accord passe à 97,9 % sur toute la base. Usage conseillé : se fier à f0 pour les basses et les 808 en un coup, la prendre comme indice ailleurs.
- **Tempo** : 50 % d'accord (de 12 à 83 % selon la famille). Les deux battues automatiques divergent sur les boucles courtes, sur les boucles sans batterie (pads, voix) et sur les rythmes ternaires. Ni l'une ni l'autre ne sert de référence : le BPM fiable reste celui du nom de fichier ou de la table, quand il existe. Les BPM d'Essentia sont gardés dans les statistiques, mais pas comme vérité.
- **Pas de contre-mesure librosa** pour le LUFS, la tonalité, l'attaque (`LogAttackTime`), les MFCC (les définitions diffèrent : filtres, normalisation, échelle) ni les parts de bande. Ces valeurs viennent d'Essentia seul.
- **Petites familles** : en dessous de 5 sons, aucune statistique n'est publiée et l'accord ne compte pas pour le critère. Une seule a un écart : fx/drums, RMS, 2 sur 3.
- **Classement** : le dossier l9 (1 412 sons) est reclassé par la règle `tout.classify`, pas par une table relue à la main. 1 924 sons n'ont pas d'étiquette de style.

## Fichiers
- `essentia-familles.tsv` : par famille et par mesure, le nombre de sons, le nombre de packs et les percentiles p10, p25, p50, p75 et p90. Les familles de moins de 5 sons n'y figurent pas.
- `essentia-styles.tsv` : le nombre de sons par famille et par style.
- `essentia-accord.tsv` : l'accord Essentia / librosa par famille et par mesure, avec une ligne TOUT.
- Aucune mesure d'un son précis, aucun nom de fichier, de pack ni de vendeur. Les packs sont notés rNpN ou lN.

Pour refaire la mesure (venv isolé, Python avec -I) :
`venv-essentia/bin/python -I scripts/son/essentia/base.py mesure STOCK OUT.jsonl`, puis `… stats OUT.jsonl docs/sons/fin`.
