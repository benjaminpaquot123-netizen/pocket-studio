# Sons IA — Dubstep & DnB

Générateur : Stable Audio Open Small (Stability AI Community License), export ONNX fp32 d'atauser001 révision 8f9544d, 8 pas, CPU. Powered by Stability AI.
Mesure : Essentia/librosa contre les cibles d'eq/son-fin-essentia-01 @ faa70cb3a (zone-b-basses, zone-c-synthes), distance = moyenne quadratique des écarts réduits (p25–p75) au p50 de la ligne, bornés à ±6.

## Réglages
- 520 essais = 2 styles (dubstep 140, DnB 174) × 11 familles ; graines 2000+i (+500 en DnB) ; `{s}` = style + tempo, `{n}` = note F1…C2.
- Boucles coupées à 1, 2 ou 4 mesures exactes au tempo ; one-shots coupés à 1 à 6 s. Chaque coupe existe en « brut » et en « saturé » (tanh, gain 2,5).
- Rejets : silence (< -40 dB), clic, saturation numérique (plateaux), pas de basse (< 8 % sous 300 Hz, familles tonales), bruit sans hauteur, facteur de crête < 3 dB (onde carrée), distance > 3 avant ET après MP3.
- MP3 192k, crête ≤ -1 dBFS vérifiée après décodage. Gardés : 235 (144 brut, 91 saturé), classés par famille et style.
- Scripts : `scripts/son/ia-bass/` (gen.py, select.py, page.py, prompts.py).

## Prompts
- **Wobbles** (loop, 40 par style) : « slow wobble bass, {s}, half-time LFO, heavy and fat, {n} » / « slow dubstep wobble bass, quarter note LFO, deep and dirty, {n} » / « fast wobble bass, {s}, sixteenth note LFO, aggressive, {n} » / « fast wobble bassline, {s}, eighth note wub wub, distorted, {n} » / « triplet wobble bass, {s}, triplet LFO rhythm, gritty, {n} » / « wobble bass with triplet rhythm, {s}, rolling and fat, {n} » / « talking bass, {s}, vowel formant wobble, yapping, {n} » / « talking wobble bass, {s}, wah wah speaking bass, filthy, {n} » / « wobble bass with changing LFO speed, {s}, wub wub, {n} » / « classic wobble bass loop, {s}, low-pass filter LFO, saw bass, {n} »
- **Growls** (loop, 24 par style) : « growl bass, {s}, FM growl, aggressive, {n} » / « dirty growl bass one note, {s}, monster growl, {n} » / « metallic growl bass, {s}, comb filter, heavy, {n} » / « deep growl bass sweep, {s}, distorted, {n} » / « neuro growl bass, {s}, resampled, gritty, {n} » / « angry growling bass, {s}, formant growl, {n} »
- **Yoi / vowel bass** (loop, 24 par style) : « yoi bass, {s}, vowel formant bass saying yoi, {n} » / « vowel bass, {s}, formant filter a-e-i-o-u, fat, {n} » / « yoy bass stab, {s}, talking vowel, distorted, {n} » / « vocal formant bass, {s}, oi oi vowel sweep, {n} » / « wah vowel bass, {s}, formant growl, heavy, {n} » / « yoi yoi bass riff, {s}, vowel bass, {n} »
- **Reese / neuro reese** (oneshot, 24 par style) : « reese bass, {s}, detuned saw bass, wide and dark, sustained note {n} » / « neuro reese bass, {s}, phasing detuned saws, distorted, {n} » / « dark reese bass, {s}, slow phasing, deep, long note {n} » / « neurofunk reese, {s}, gritty and moving, {n} » / « reese bass with chorus, {s}, wide stereo, sustained {n} » / « evil reese bass, {s}, rumbling detuned bass, {n} »
- **Basses neuro / sales** (loop, 24 par style) : « neuro bass, {s}, dirty distorted bass, {n} » / « filthy bass, {s}, heavy distortion, fat low end, {n} » / « distorted saw bass, {s}, overdriven, gritty, {n} » / « neurofunk bass shot, {s}, resampled, crunchy, {n} » / « bitcrushed bass, {s}, dirty, heavy, {n} » / « fat dirty bass, {s}, saturated square bass, {n} »
- **Screech / riddim** (loop, 24 par style) : « screech bass, {s}, high pitched screaming bass, {n} » / « riddim bass, {s}, repetitive square bass, minimal, {n} » / « riddim dubstep bass loop, {s}, bouncy and dirty, {n} » / « screaming lead bass, {s}, distorted, metallic, {n} » / « tearout screech bass, {s}, aggressive, {n} » / « riddim wub bass, {s}, triplet bounce, {n} »
- **Sub drops** (oneshot, 16 par style) : « sub drop, {s}, deep sine pitch falling down, long » / « 808 sub drop, {s}, pitch dive, deep and clean » / « deep sub boom drop, {s}, falling low frequency » / « sub bass drop impact, {s}, descending sine »
- **Impacts** (oneshot, 16 par style) : « impact hit, {s}, huge boom with reverb tail » / « cinematic bass impact, {s}, explosion, heavy » / « drop impact fx, {s}, deep boom and crash » / « distorted impact hit, {s}, heavy low end »
- **Risers / FX** (oneshot, 28 par style) : « riser fx, {s}, white noise sweep up, building tension » / « pitch riser, {s}, screaming synth rising, intense » / « chaotic fx, {s}, glitchy sweeps all over the place » / « laser zap fx, {s}, wild pitch bends » / « downlifter sweep fx, {s}, falling noise » / « glitch stutter fx, {s}, random bleeps and zaps » / « alarm siren riser, {s}, build up »
- **Stabs gras** (oneshot, 24 par style) : « fat bass stab, {s}, short distorted hit, {n} » / « heavy bass shot, {s}, punchy, dirty, {n} » / « dubstep bass hit, {s}, gritty one shot, {n} » / « big distorted stab, {s}, short, fat, {n} » / « neuro bass stab, {s}, sharp attack, {n} » / « chord stab bass, {s}, massive, {n} »
- **Foley bass** (loop, 16 par style) : « foley bass, {s}, metallic clanks resampled into bass, {n} » / « organic bass, {s}, recorded objects mangled into a growl, {n} » / « junk metal bass, {s}, foley percussion bass hits, {n} » / « creature bass, {s}, foley sounds pitched down, gritty, {n} »

## Résultats (distance au MP3, plus bas = plus proche)
| Style | Famille | Essais | Gardés | Meilleur | Médiane | Rejets |
|---|---|---|---|---|---|---|
| dnb | Foley bass | 16 | 10 | 0.77 | 1.02 | — |
| dnb | Growls | 24 | 12 | 0.44 | 0.63 | — |
| dnb | Impacts | 16 | 10 | 1.18 | 1.34 | — |
| dnb | Basses neuro / sales | 24 | 12 | 0.67 | 0.76 | silence 1, bruit sans hauteur 1 |
| dnb | Reese / neuro reese | 24 | 12 | 1.43 | 1.69 | — |
| dnb | Risers / FX | 28 | 14 | 0.68 | 0.82 | — |
| dnb | Screech / riddim | 24 | 12 | 0.34 | 0.76 | trop loin de la famille 2 |
| dnb | Stabs gras | 24 | 12 | 0.58 | 0.70 | — |
| dnb | Sub drops | 16 | 10 | 0.70 | 0.97 | — |
| dnb | Wobbles | 40 | 16 | 0.38 | 0.63 | — |
| dnb | Yoi / vowel bass | 24 | 12 | 0.43 | 0.70 | — |
| dubstep | Foley bass | 16 | 2 | 1.22 | 1.57 | trop loin de la famille 13, trop loin après MP3 1 |
| dubstep | Growls | 24 | 11 | 1.74 | 2.54 | trop loin de la famille 10, trop loin après MP3 1 |
| dubstep | Impacts | 16 | 2 | 2.58 | 2.74 | trop loin de la famille 11, trop loin après MP3 2 |
| dubstep | Basses neuro / sales | 24 | 3 | 2.38 | 2.71 | trop loin de la famille 16, trop loin après MP3 1 |
| dubstep | Reese / neuro reese | 24 | 12 | 1.34 | 1.55 | — |
| dubstep | Risers / FX | 28 | 14 | 0.92 | 1.26 | — |
| dubstep | Screech / riddim | 24 | 12 | 0.97 | 1.60 | trop loin de la famille 7 |
| dubstep | Stabs gras | 24 | 12 | 1.61 | 2.72 | trop loin de la famille 6 |
| dubstep | Sub drops | 16 | 10 | 0.57 | 0.73 | saturation numérique 1 |
| dubstep | Wobbles | 40 | 16 | 2.16 | 2.54 | pas de basse 1, trop loin de la famille 15 |
| dubstep | Yoi / vowel bass | 24 | 9 | 1.75 | 2.60 | trop loin de la famille 14 |

Les 3 meilleurs : dnb-screech-01 (0,34, « riddim wub bass »), dnb-wobble-01 (0,38, « talking bass »), dnb-wobble-02 (0,42, « talking bass »).

## Limites
- Les lignes cibles DnB sont très sombres (centroïde p50 ≈ 120–190 Hz) : leurs distances favorisent les sons ternes ; les lignes dubstep sont claires (≈ 1,4 kHz). Comparer les scores dans un même style seulement.
- Reese : la ligne dubstep n'a que 2 packs et la DnB manque, donc les deux styles visent la ligne « * » (8 packs).
- Dubstep foley, impacts et neuro : peu de sons passent (2 à 3) ; le modèle les rend trop loin des cibles.
- Critique à l'aveugle (sous-agent neuf, 5 sons) : scores retrouvés à l'identique, boucles à la mesure, basse et hauteur présentes. Corrigé ensuite : 21 MP3 saturés dépassaient 0 dBFS (contrôle après décodage), sub drops dubstep en onde carrée (filtre crête), seuil appliqué aussi après MP3. Restant : la saturation domine les wobbles dubstep.
