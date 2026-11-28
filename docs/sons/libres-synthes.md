# Synthés et effets libres : comparaison avec les sons fabriqués (fouille élargie)

Date : 2026-11-28. Branche : `eq/synthes-libres`. Remplace la version du même jour (21 MP3, Freesound seul, anciens synthés).

## En bref

- **Nos fabriqués tiennent la route presque partout** : sur 96 cases (8 familles × 12 styles), 72 sont à égalité
  (écart < 0,3) et le fabriqué est en tête dans 13 autres (à égalité avec un A ou un B ; 11 sont des arpèges où le libre « oscarsynth » reste un peu plus près). Il est distancé dans 11 cases :
  9 en stab ou effet (« A = B devant »), 1 « A devant » (arpège rnb), 1 « B devant » (ambiance house).
- **Un seul vrai trou** : nappe d'ambiance **house** (fabriqué 0,93, meilleur B 0,50, meilleur A 0,82). Lo-fi est limite
  (fabriqué 0,75, B 0,45 : écart juste sous 0,3).
- **Stab** : un *orchestra hit* libre (0,18) est plus près de la cible que tous nos stabs dans les 12 styles (notre meilleur : drill, 0,24),
  et nettement (écart > 0,3) dans 7 : boom-bap, house, techno, dnb, edm, reggaeton-afro, synthwave (nos stabs à 0,59 à 0,94).
- **Effets** : le « faller » CC0 de Wyzowl (0,39 sur le WAV d'origine, 0,41 sur le MP3 commité) devance nos effets en lo-fi (0,72) et rnb (0,85).
- **Arpège** : le libre « oscarsynth » (0,50, mais 1,7 s seulement) est plus près que notre meilleur arpège dans les 12 styles ;
  à égalité dans 11, nettement devant en rnb (notre 0,89).
- Les sons B (MusicRadar, gratuits mais non redistribuables) ne gagnent seuls nulle part, sauf l'ambiance house.

## Méthode

- **Fabriqués** : les 768 sons de la zone C (`docs/sons/cible/c/index.json`, branche `eq/son-cible-c` @ `8def77b2`),
  12 styles × 8 familles × 8 variantes. Pour chaque famille et style, le meilleur des 8 (sa `distance_adn_sigma`,
  remesurée ici à l'identique : techno-stab-01 = 1,058, dnb-boucle-01 = 0,781).
- **Cibles et distance** : `docs/sons/fin/cibles/zone-c-synthes.{md,tsv}` (branche `eq/son-fin-essentia-01` @ `faa70cb3a`),
  calcul `scripts/son/cible/c/tgt_c.py` : écart de chaque mesure Essentia à la cible ramené à son étalement, écrêté, moyenne
  quadratique pondérée. **Plus bas = plus près.** Cellule du style quand elle existe (nappe/trap, nappe/dnb, accords/dnb,
  boucle-lead/dnb, fx-nappe lofi/house/techno/dnb, fx-effet techno/dnb), sinon la cellule « * ».
- **Libres** : chaque son mesuré une fois (`cibles_desc.from_bytes`), comparé à toutes les cellules de ses familles.
  2 573 sons distincts mesurés :
  - Freesound, filtre **CC0** et durée 0,2 à 40 s, 14 familles de recherche (les 8 familles, plus hoover, orchestra hit,
    vocal chop, cloche FM, vinyle, glitch), 3 pages par requête : 1 700 résultats, 1 492 gardés après tri des titres
    (sans acoustique, batterie ni voix parlée), aperçus HQ MP3 ;
  - archive.org : ESQ-1 « piano 2 chord » (Paisley Computer, 93 sons) et « VHS sample pack » (Freezywaves, 27 sons),
    marqués *Public Domain Mark 1.0* par leurs auteurs ;
  - Wyzowl SFX Kit (HuggingFace, CC0 1.0, 78 effets) ;
  - classe B, mesurés mais **jamais commités** : MusicRadar SampleRadar « 00s synths » (arps, accords, nappes,
    multisamples), « synth impacts », « drum'n'bass FX », « glitch vocals » (938 fichiers).
- **Verdict** : « égalité » quand tous sont à moins de 0,3 du meilleur ; sinon on nomme ceux qui sont en tête
  (à moins de 0,3 du meilleur) ; « pas de verdict » si même le meilleur dépasse 1,5.
- Titres vérifiés à la main : retirés des A les voix, guitare, piano, harmonica, cloche d'école, allumette, « Rock Loop ».
- Python lancé en `-I` sur tous les fichiers téléchargés ; aucun son téléchargé hors scratchpad sauf les A choisis.

## Résultats par famille et par style

### Stab

| Style | Cellule cible | Meilleur fabriqué | Meilleur A (libre) | Meilleur B (gratuit, non redistribuable) | Verdict |
|---|---|---|---|---|---|
| trap | stab/* | 0,42 `trap-stab-02` | 0,18 [Orchestra Hit DIY](libres/mp3/freesound-794524.mp3) ([fiche](https://freesound.org/s/794524/), CC0) | 0,33 [SI_SynHitProc4-B](https://www.musicradar.com/news/sampleradar-free-synth-impacts-samples) (MusicRadar, non redistribuable) | égalité |
| drill | stab/* | 0,24 `drill-stab-01` | 0,18 [Orchestra Hit DIY](libres/mp3/freesound-794524.mp3) ([fiche](https://freesound.org/s/794524/), CC0) | 0,33 [SI_SynHitProc4-B](https://www.musicradar.com/news/sampleradar-free-synth-impacts-samples) (MusicRadar, non redistribuable) | égalité |
| boom-bap | stab/* | 0,94 `boom-bap-stab-01` | 0,18 [Orchestra Hit DIY](libres/mp3/freesound-794524.mp3) ([fiche](https://freesound.org/s/794524/), CC0) | 0,33 [SI_SynHitProc4-B](https://www.musicradar.com/news/sampleradar-free-synth-impacts-samples) (MusicRadar, non redistribuable) | A = B à égalité, devant |
| lo-fi | stab/* | 0,38 `lo-fi-stab-01` | 0,18 [Orchestra Hit DIY](libres/mp3/freesound-794524.mp3) ([fiche](https://freesound.org/s/794524/), CC0) | 0,33 [SI_SynHitProc4-B](https://www.musicradar.com/news/sampleradar-free-synth-impacts-samples) (MusicRadar, non redistribuable) | égalité |
| house | stab/* | 0,59 `house-stab-03` | 0,18 [Orchestra Hit DIY](libres/mp3/freesound-794524.mp3) ([fiche](https://freesound.org/s/794524/), CC0) | 0,33 [SI_SynHitProc4-B](https://www.musicradar.com/news/sampleradar-free-synth-impacts-samples) (MusicRadar, non redistribuable) | A = B à égalité, devant |
| techno | stab/* | 0,72 `techno-stab-03` | 0,18 [Orchestra Hit DIY](libres/mp3/freesound-794524.mp3) ([fiche](https://freesound.org/s/794524/), CC0) | 0,33 [SI_SynHitProc4-B](https://www.musicradar.com/news/sampleradar-free-synth-impacts-samples) (MusicRadar, non redistribuable) | A = B à égalité, devant |
| dnb | stab/* | 0,70 `dnb-stab-07` | 0,18 [Orchestra Hit DIY](libres/mp3/freesound-794524.mp3) ([fiche](https://freesound.org/s/794524/), CC0) | 0,33 [SI_SynHitProc4-B](https://www.musicradar.com/news/sampleradar-free-synth-impacts-samples) (MusicRadar, non redistribuable) | A = B à égalité, devant |
| edm | stab/* | 0,70 `edm-stab-04` | 0,18 [Orchestra Hit DIY](libres/mp3/freesound-794524.mp3) ([fiche](https://freesound.org/s/794524/), CC0) | 0,33 [SI_SynHitProc4-B](https://www.musicradar.com/news/sampleradar-free-synth-impacts-samples) (MusicRadar, non redistribuable) | A = B à égalité, devant |
| reggaeton-afro | stab/* | 0,73 `reggaeton-afro-stab-02` | 0,18 [Orchestra Hit DIY](libres/mp3/freesound-794524.mp3) ([fiche](https://freesound.org/s/794524/), CC0) | 0,33 [SI_SynHitProc4-B](https://www.musicradar.com/news/sampleradar-free-synth-impacts-samples) (MusicRadar, non redistribuable) | A = B à égalité, devant |
| pop | stab/* | 0,39 `pop-stab-03` | 0,18 [Orchestra Hit DIY](libres/mp3/freesound-794524.mp3) ([fiche](https://freesound.org/s/794524/), CC0) | 0,33 [SI_SynHitProc4-B](https://www.musicradar.com/news/sampleradar-free-synth-impacts-samples) (MusicRadar, non redistribuable) | égalité |
| synthwave | stab/* | 0,88 `synthwave-stab-05` | 0,18 [Orchestra Hit DIY](libres/mp3/freesound-794524.mp3) ([fiche](https://freesound.org/s/794524/), CC0) | 0,33 [SI_SynHitProc4-B](https://www.musicradar.com/news/sampleradar-free-synth-impacts-samples) (MusicRadar, non redistribuable) | A = B à égalité, devant |
| rnb | stab/* | 0,46 `rnb-stab-02` | 0,18 [Orchestra Hit DIY](libres/mp3/freesound-794524.mp3) ([fiche](https://freesound.org/s/794524/), CC0) | 0,33 [SI_SynHitProc4-B](https://www.musicradar.com/news/sampleradar-free-synth-impacts-samples) (MusicRadar, non redistribuable) | égalité |

### Pluck

| Style | Cellule cible | Meilleur fabriqué | Meilleur A (libre) | Meilleur B (gratuit, non redistribuable) | Verdict |
|---|---|---|---|---|---|
| trap | pluck/* | 0,25 `trap-pluck-01` | 0,50 [Pluck-Bubule Pluck E Maj Chord](libres/mp3/freesound-840529.mp3) ([fiche](https://freesound.org/s/840529/), CC0) | 0,48 [00s_SkweelyLead_D#4](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| drill | pluck/* | 0,35 `drill-pluck-04` | 0,50 [Pluck-Bubule Pluck E Maj Chord](libres/mp3/freesound-840529.mp3) ([fiche](https://freesound.org/s/840529/), CC0) | 0,48 [00s_SkweelyLead_D#4](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| boom-bap | pluck/* | 0,22 `boom-bap-pluck-01` | 0,50 [Pluck-Bubule Pluck E Maj Chord](libres/mp3/freesound-840529.mp3) ([fiche](https://freesound.org/s/840529/), CC0) | 0,48 [00s_SkweelyLead_D#4](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| lo-fi | pluck/* | 0,22 `lo-fi-pluck-03` | 0,50 [Pluck-Bubule Pluck E Maj Chord](libres/mp3/freesound-840529.mp3) ([fiche](https://freesound.org/s/840529/), CC0) | 0,48 [00s_SkweelyLead_D#4](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| house | pluck/* | 0,28 `house-pluck-01` | 0,50 [Pluck-Bubule Pluck E Maj Chord](libres/mp3/freesound-840529.mp3) ([fiche](https://freesound.org/s/840529/), CC0) | 0,48 [00s_SkweelyLead_D#4](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| techno | pluck/* | 0,28 `techno-pluck-01` | 0,50 [Pluck-Bubule Pluck E Maj Chord](libres/mp3/freesound-840529.mp3) ([fiche](https://freesound.org/s/840529/), CC0) | 0,48 [00s_SkweelyLead_D#4](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| dnb | pluck/* | 0,37 `dnb-pluck-04` | 0,50 [Pluck-Bubule Pluck E Maj Chord](libres/mp3/freesound-840529.mp3) ([fiche](https://freesound.org/s/840529/), CC0) | 0,48 [00s_SkweelyLead_D#4](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| edm | pluck/* | 0,33 `edm-pluck-02` | 0,50 [Pluck-Bubule Pluck E Maj Chord](libres/mp3/freesound-840529.mp3) ([fiche](https://freesound.org/s/840529/), CC0) | 0,48 [00s_SkweelyLead_D#4](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| reggaeton-afro | pluck/* | 0,25 `reggaeton-afro-pluck-03` | 0,50 [Pluck-Bubule Pluck E Maj Chord](libres/mp3/freesound-840529.mp3) ([fiche](https://freesound.org/s/840529/), CC0) | 0,48 [00s_SkweelyLead_D#4](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| pop | pluck/* | 0,24 `pop-pluck-03` | 0,50 [Pluck-Bubule Pluck E Maj Chord](libres/mp3/freesound-840529.mp3) ([fiche](https://freesound.org/s/840529/), CC0) | 0,48 [00s_SkweelyLead_D#4](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| synthwave | pluck/* | 0,21 `synthwave-pluck-02` | 0,50 [Pluck-Bubule Pluck E Maj Chord](libres/mp3/freesound-840529.mp3) ([fiche](https://freesound.org/s/840529/), CC0) | 0,48 [00s_SkweelyLead_D#4](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| rnb | pluck/* | 0,20 `rnb-pluck-02` | 0,50 [Pluck-Bubule Pluck E Maj Chord](libres/mp3/freesound-840529.mp3) ([fiche](https://freesound.org/s/840529/), CC0) | 0,48 [00s_SkweelyLead_D#4](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | fabriqué = B à égalité, devant |

### Nappe

| Style | Cellule cible | Meilleur fabriqué | Meilleur A (libre) | Meilleur B (gratuit, non redistribuable) | Verdict |
|---|---|---|---|---|---|
| trap | nappe/trap | 0,39 `trap-nappe-05` | 0,54 [Pad-Aurora_Cmaj](libres/mp3/freesound-875696.mp3) ([fiche](https://freesound.org/s/875696/), CC0) | 0,57 [00s_SkweelyLead_C4](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| drill | nappe/trap | 0,40 `drill-nappe-04` | 0,54 [Pad-Aurora_Cmaj](libres/mp3/freesound-875696.mp3) ([fiche](https://freesound.org/s/875696/), CC0) | 0,57 [00s_SkweelyLead_C4](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| boom-bap | nappe/* | 0,65 `boom-bap-nappe-02` | 0,56 [Really cool smooth pad synth](libres/mp3/freesound-572965.mp3) ([fiche](https://freesound.org/s/572965/), CC0) | 0,85 [00s_SkweelyLead_F#4](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| lo-fi | nappe/* | 0,66 `lo-fi-nappe-01` | 0,56 [Really cool smooth pad synth](libres/mp3/freesound-572965.mp3) ([fiche](https://freesound.org/s/572965/), CC0) | 0,85 [00s_SkweelyLead_F#4](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| house | nappe/* | 0,84 `house-nappe-03` | 0,56 [Really cool smooth pad synth](libres/mp3/freesound-572965.mp3) ([fiche](https://freesound.org/s/572965/), CC0) | 0,85 [00s_SkweelyLead_F#4](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| techno | nappe/* | 0,60 `techno-nappe-01` | 0,56 [Really cool smooth pad synth](libres/mp3/freesound-572965.mp3) ([fiche](https://freesound.org/s/572965/), CC0) | 0,85 [00s_SkweelyLead_F#4](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| dnb | nappe/dnb | 0,92 `dnb-nappe-08` | 0,73 [Pad Double Chord Stretch.wav](libres/mp3/freesound-396538.mp3) ([fiche](https://freesound.org/s/396538/), CC0) | 0,74 [00s_SkweelyLead_D#2](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| edm | nappe/* | 0,55 `edm-nappe-03` | 0,56 [Really cool smooth pad synth](libres/mp3/freesound-572965.mp3) ([fiche](https://freesound.org/s/572965/), CC0) | 0,85 [00s_SkweelyLead_F#4](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| reggaeton-afro | nappe/* | 0,73 `reggaeton-afro-nappe-01` | 0,56 [Really cool smooth pad synth](libres/mp3/freesound-572965.mp3) ([fiche](https://freesound.org/s/572965/), CC0) | 0,85 [00s_SkweelyLead_F#4](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| pop | nappe/* | 0,63 `pop-nappe-02` | 0,56 [Really cool smooth pad synth](libres/mp3/freesound-572965.mp3) ([fiche](https://freesound.org/s/572965/), CC0) | 0,85 [00s_SkweelyLead_F#4](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| synthwave | nappe/* | 0,69 `synthwave-nappe-03` | 0,56 [Really cool smooth pad synth](libres/mp3/freesound-572965.mp3) ([fiche](https://freesound.org/s/572965/), CC0) | 0,85 [00s_SkweelyLead_F#4](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| rnb | nappe/* | 0,61 `rnb-nappe-01` | 0,56 [Really cool smooth pad synth](libres/mp3/freesound-572965.mp3) ([fiche](https://freesound.org/s/572965/), CC0) | 0,85 [00s_SkweelyLead_F#4](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |

### Accords de synthé

| Style | Cellule cible | Meilleur fabriqué | Meilleur A (libre) | Meilleur B (gratuit, non redistribuable) | Verdict |
|---|---|---|---|---|---|
| trap | accords/* | 0,25 `trap-accords-02` | 0,43 [Synth pad chord](libres/mp3/freesound-727082.mp3) ([fiche](https://freesound.org/s/727082/), CC0) | 0,33 [00s_JaySawz[140]-A](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| drill | accords/* | 0,23 `drill-accords-07` | 0,43 [Synth pad chord](libres/mp3/freesound-727082.mp3) ([fiche](https://freesound.org/s/727082/), CC0) | 0,33 [00s_JaySawz[140]-A](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| boom-bap | accords/* | 0,19 `boom-bap-accords-05` | 0,43 [Synth pad chord](libres/mp3/freesound-727082.mp3) ([fiche](https://freesound.org/s/727082/), CC0) | 0,33 [00s_JaySawz[140]-A](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| lo-fi | accords/* | 0,32 `lo-fi-accords-04` | 0,43 [Synth pad chord](libres/mp3/freesound-727082.mp3) ([fiche](https://freesound.org/s/727082/), CC0) | 0,33 [00s_JaySawz[140]-A](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| house | accords/* | 0,22 `house-accords-01` | 0,43 [Synth pad chord](libres/mp3/freesound-727082.mp3) ([fiche](https://freesound.org/s/727082/), CC0) | 0,33 [00s_JaySawz[140]-A](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| techno | accords/* | 0,20 `techno-accords-02` | 0,43 [Synth pad chord](libres/mp3/freesound-727082.mp3) ([fiche](https://freesound.org/s/727082/), CC0) | 0,33 [00s_JaySawz[140]-A](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| dnb | accords/dnb | 0,46 `dnb-accords-02` | 0,52 [Spaceship Pads, Chord Progression](libres/mp3/freesound-503242.mp3) ([fiche](https://freesound.org/s/503242/), CC0) | 0,73 [00s_JaySawz[125]-A](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| edm | accords/* | 0,34 `edm-accords-04` | 0,43 [Synth pad chord](libres/mp3/freesound-727082.mp3) ([fiche](https://freesound.org/s/727082/), CC0) | 0,33 [00s_JaySawz[140]-A](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| reggaeton-afro | accords/* | 0,24 `reggaeton-afro-accords-01` | 0,43 [Synth pad chord](libres/mp3/freesound-727082.mp3) ([fiche](https://freesound.org/s/727082/), CC0) | 0,33 [00s_JaySawz[140]-A](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| pop | accords/* | 0,27 `pop-accords-01` | 0,43 [Synth pad chord](libres/mp3/freesound-727082.mp3) ([fiche](https://freesound.org/s/727082/), CC0) | 0,33 [00s_JaySawz[140]-A](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| synthwave | accords/* | 0,25 `synthwave-accords-01` | 0,43 [Synth pad chord](libres/mp3/freesound-727082.mp3) ([fiche](https://freesound.org/s/727082/), CC0) | 0,33 [00s_JaySawz[140]-A](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| rnb | accords/* | 0,32 `rnb-accords-01` | 0,43 [Synth pad chord](libres/mp3/freesound-727082.mp3) ([fiche](https://freesound.org/s/727082/), CC0) | 0,33 [00s_JaySawz[140]-A](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |

### Boucle mélodique

| Style | Cellule cible | Meilleur fabriqué | Meilleur A (libre) | Meilleur B (gratuit, non redistribuable) | Verdict |
|---|---|---|---|---|---|
| trap | boucle-lead/* | 0,43 `trap-boucle-03` | 0,49 [Supersaw/square synth loop 1 (120 BPM)](libres/mp3/freesound-709165.mp3) ([fiche](https://freesound.org/s/709165/), CC0) | 0,35 [00s_Arp[125]-E](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| drill | boucle-lead/* | 0,30 `drill-boucle-02` | 0,49 [Supersaw/square synth loop 1 (120 BPM)](libres/mp3/freesound-709165.mp3) ([fiche](https://freesound.org/s/709165/), CC0) | 0,35 [00s_Arp[125]-E](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| boom-bap | boucle-lead/* | 0,35 `boom-bap-boucle-01` | 0,49 [Supersaw/square synth loop 1 (120 BPM)](libres/mp3/freesound-709165.mp3) ([fiche](https://freesound.org/s/709165/), CC0) | 0,35 [00s_Arp[125]-E](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| lo-fi | boucle-lead/* | 0,33 `lo-fi-boucle-01` | 0,49 [Supersaw/square synth loop 1 (120 BPM)](libres/mp3/freesound-709165.mp3) ([fiche](https://freesound.org/s/709165/), CC0) | 0,35 [00s_Arp[125]-E](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| house | boucle-lead/* | 0,24 `house-boucle-02` | 0,49 [Supersaw/square synth loop 1 (120 BPM)](libres/mp3/freesound-709165.mp3) ([fiche](https://freesound.org/s/709165/), CC0) | 0,35 [00s_Arp[125]-E](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| techno | boucle-lead/* | 0,43 `techno-boucle-06` | 0,49 [Supersaw/square synth loop 1 (120 BPM)](libres/mp3/freesound-709165.mp3) ([fiche](https://freesound.org/s/709165/), CC0) | 0,35 [00s_Arp[125]-E](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| dnb | boucle-lead/dnb | 0,57 `dnb-boucle-04` | 0,59 [Melo2XX.wav](libres/mp3/freesound-411454.mp3) ([fiche](https://freesound.org/s/411454/), CC0) | 0,40 [GV_StripVocPad_120-Dmaj](https://www.musicradar.com/news/sampleradar-glitch-vocal-samples) (MusicRadar, non redistribuable) | égalité |
| edm | boucle-lead/* | 0,31 `edm-boucle-01` | 0,49 [Supersaw/square synth loop 1 (120 BPM)](libres/mp3/freesound-709165.mp3) ([fiche](https://freesound.org/s/709165/), CC0) | 0,35 [00s_Arp[125]-E](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| reggaeton-afro | boucle-lead/* | 0,45 `reggaeton-afro-boucle-05` | 0,49 [Supersaw/square synth loop 1 (120 BPM)](libres/mp3/freesound-709165.mp3) ([fiche](https://freesound.org/s/709165/), CC0) | 0,35 [00s_Arp[125]-E](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| pop | boucle-lead/* | 0,27 `pop-boucle-02` | 0,49 [Supersaw/square synth loop 1 (120 BPM)](libres/mp3/freesound-709165.mp3) ([fiche](https://freesound.org/s/709165/), CC0) | 0,35 [00s_Arp[125]-E](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| synthwave | boucle-lead/* | 0,34 `synthwave-boucle-01` | 0,49 [Supersaw/square synth loop 1 (120 BPM)](libres/mp3/freesound-709165.mp3) ([fiche](https://freesound.org/s/709165/), CC0) | 0,35 [00s_Arp[125]-E](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |
| rnb | boucle-lead/* | 0,23 `rnb-boucle-03` | 0,49 [Supersaw/square synth loop 1 (120 BPM)](libres/mp3/freesound-709165.mp3) ([fiche](https://freesound.org/s/709165/), CC0) | 0,35 [00s_Arp[125]-E](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | égalité |

### Arpège

| Style | Cellule cible | Meilleur fabriqué | Meilleur A (libre) | Meilleur B (gratuit, non redistribuable) | Verdict |
|---|---|---|---|---|---|
| trap | boucle-synth/* | 0,69 `trap-arpege-03` | 0,50 [oscarsynth.wav](libres/mp3/freesound-484756.mp3) ([fiche](https://freesound.org/s/484756/), CC0) | 0,86 [00s_Arp[125]-E](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | fabriqué = A à égalité, devant |
| drill | boucle-synth/* | 0,52 `drill-arpege-01` | 0,50 [oscarsynth.wav](libres/mp3/freesound-484756.mp3) ([fiche](https://freesound.org/s/484756/), CC0) | 0,86 [00s_Arp[125]-E](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | fabriqué = A à égalité, devant |
| boom-bap | boucle-synth/* | 0,66 `boom-bap-arpege-04` | 0,50 [oscarsynth.wav](libres/mp3/freesound-484756.mp3) ([fiche](https://freesound.org/s/484756/), CC0) | 0,86 [00s_Arp[125]-E](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | fabriqué = A à égalité, devant |
| lo-fi | boucle-synth/* | 0,66 `lo-fi-arpege-01` | 0,50 [oscarsynth.wav](libres/mp3/freesound-484756.mp3) ([fiche](https://freesound.org/s/484756/), CC0) | 0,86 [00s_Arp[125]-E](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | fabriqué = A à égalité, devant |
| house | boucle-synth/* | 0,57 `house-arpege-01` | 0,50 [oscarsynth.wav](libres/mp3/freesound-484756.mp3) ([fiche](https://freesound.org/s/484756/), CC0) | 0,86 [00s_Arp[125]-E](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | fabriqué = A à égalité, devant |
| techno | boucle-synth/* | 0,55 `techno-arpege-02` | 0,50 [oscarsynth.wav](libres/mp3/freesound-484756.mp3) ([fiche](https://freesound.org/s/484756/), CC0) | 0,86 [00s_Arp[125]-E](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | fabriqué = A à égalité, devant |
| dnb | boucle-synth/* | 0,61 `dnb-arpege-04` | 0,50 [oscarsynth.wav](libres/mp3/freesound-484756.mp3) ([fiche](https://freesound.org/s/484756/), CC0) | 0,86 [00s_Arp[125]-E](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | fabriqué = A à égalité, devant |
| edm | boucle-synth/* | 0,79 `edm-arpege-01` | 0,50 [oscarsynth.wav](libres/mp3/freesound-484756.mp3) ([fiche](https://freesound.org/s/484756/), CC0) | 0,86 [00s_Arp[125]-E](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | fabriqué = A à égalité, devant |
| reggaeton-afro | boucle-synth/* | 0,53 `reggaeton-afro-arpege-05` | 0,50 [oscarsynth.wav](libres/mp3/freesound-484756.mp3) ([fiche](https://freesound.org/s/484756/), CC0) | 0,86 [00s_Arp[125]-E](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | fabriqué = A à égalité, devant |
| pop | boucle-synth/* | 0,58 `pop-arpege-01` | 0,50 [oscarsynth.wav](libres/mp3/freesound-484756.mp3) ([fiche](https://freesound.org/s/484756/), CC0) | 0,86 [00s_Arp[125]-E](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | fabriqué = A à égalité, devant |
| synthwave | boucle-synth/* | 0,77 `synthwave-arpege-01` | 0,50 [oscarsynth.wav](libres/mp3/freesound-484756.mp3) ([fiche](https://freesound.org/s/484756/), CC0) | 0,86 [00s_Arp[125]-E](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | fabriqué = A à égalité, devant |
| rnb | boucle-synth/* | 0,89 `rnb-arpege-01` | 0,50 [oscarsynth.wav](libres/mp3/freesound-484756.mp3) ([fiche](https://freesound.org/s/484756/), CC0) | 0,86 [00s_Arp[125]-E](https://www.musicradar.com/news/sampleradar-00s-synth-samples) (MusicRadar, non redistribuable) | A devant |

### Nappe d'ambiance

| Style | Cellule cible | Meilleur fabriqué | Meilleur A (libre) | Meilleur B (gratuit, non redistribuable) | Verdict |
|---|---|---|---|---|---|
| trap | fx-nappe/* | 0,24 `trap-ambiance-01` | 0,51 [ambience_atonal_shimmery.wav](libres/mp3/freesound-557104.mp3) ([fiche](https://freesound.org/s/557104/), CC0) | 0,22 [DNB_FX_37](https://www.musicradar.com/news/tech/sampleradar-100-free-drum-n-bass-fx-samples-444650) (MusicRadar, non redistribuable) | égalité |
| drill | fx-nappe/* | 0,27 `drill-ambiance-02` | 0,51 [ambience_atonal_shimmery.wav](libres/mp3/freesound-557104.mp3) ([fiche](https://freesound.org/s/557104/), CC0) | 0,22 [DNB_FX_37](https://www.musicradar.com/news/tech/sampleradar-100-free-drum-n-bass-fx-samples-444650) (MusicRadar, non redistribuable) | égalité |
| boom-bap | fx-nappe/* | 0,33 `boom-bap-ambiance-01` | 0,51 [ambience_atonal_shimmery.wav](libres/mp3/freesound-557104.mp3) ([fiche](https://freesound.org/s/557104/), CC0) | 0,22 [DNB_FX_37](https://www.musicradar.com/news/tech/sampleradar-100-free-drum-n-bass-fx-samples-444650) (MusicRadar, non redistribuable) | égalité |
| lo-fi | fx-nappe/lofi | 0,75 `lo-fi-ambiance-04` | 0,81 [Vinyl Crackle (Real).mp3](libres/mp3/freesound-434822.mp3) ([fiche](https://freesound.org/s/434822/), CC0) | 0,45 [DNB_FX_37](https://www.musicradar.com/news/tech/sampleradar-100-free-drum-n-bass-fx-samples-444650) (MusicRadar, non redistribuable) | fabriqué = B à égalité, devant |
| house | fx-nappe/house | 0,93 `house-ambiance-02` | 0,82 [Whoosh stereo light (transition), xkeril](libres/mp3/freesound-701104.mp3) ([fiche](https://freesound.org/s/701104/), CC0) | 0,50 [DNB_FX_99](https://www.musicradar.com/news/tech/sampleradar-100-free-drum-n-bass-fx-samples-444650) (MusicRadar, non redistribuable) | B devant |
| techno | fx-nappe/techno | 0,68 `techno-ambiance-01` | 0,80 [ambience_atonal_shimmery.wav](libres/mp3/freesound-557104.mp3) ([fiche](https://freesound.org/s/557104/), CC0) | 0,56 [DNB_FX_69](https://www.musicradar.com/news/tech/sampleradar-100-free-drum-n-bass-fx-samples-444650) (MusicRadar, non redistribuable) | égalité |
| dnb | fx-nappe/dnb | 0,33 `dnb-ambiance-01` | 0,38 [Inerstellar_Ambient_Drone_13](libres/mp3/freesound-872314.mp3) ([fiche](https://freesound.org/s/872314/), CC0) | 0,38 [DNB_FX_84](https://www.musicradar.com/news/tech/sampleradar-100-free-drum-n-bass-fx-samples-444650) (MusicRadar, non redistribuable) | égalité |
| edm | fx-nappe/* | 0,24 `edm-ambiance-02` | 0,51 [ambience_atonal_shimmery.wav](libres/mp3/freesound-557104.mp3) ([fiche](https://freesound.org/s/557104/), CC0) | 0,22 [DNB_FX_37](https://www.musicradar.com/news/tech/sampleradar-100-free-drum-n-bass-fx-samples-444650) (MusicRadar, non redistribuable) | égalité |
| reggaeton-afro | fx-nappe/* | 0,33 `reggaeton-afro-ambiance-01` | 0,51 [ambience_atonal_shimmery.wav](libres/mp3/freesound-557104.mp3) ([fiche](https://freesound.org/s/557104/), CC0) | 0,22 [DNB_FX_37](https://www.musicradar.com/news/tech/sampleradar-100-free-drum-n-bass-fx-samples-444650) (MusicRadar, non redistribuable) | égalité |
| pop | fx-nappe/* | 0,37 `pop-ambiance-04` | 0,51 [ambience_atonal_shimmery.wav](libres/mp3/freesound-557104.mp3) ([fiche](https://freesound.org/s/557104/), CC0) | 0,22 [DNB_FX_37](https://www.musicradar.com/news/tech/sampleradar-100-free-drum-n-bass-fx-samples-444650) (MusicRadar, non redistribuable) | égalité |
| synthwave | fx-nappe/* | 0,38 `synthwave-ambiance-01` | 0,51 [ambience_atonal_shimmery.wav](libres/mp3/freesound-557104.mp3) ([fiche](https://freesound.org/s/557104/), CC0) | 0,22 [DNB_FX_37](https://www.musicradar.com/news/tech/sampleradar-100-free-drum-n-bass-fx-samples-444650) (MusicRadar, non redistribuable) | égalité |
| rnb | fx-nappe/* | 0,39 `rnb-ambiance-03` | 0,51 [ambience_atonal_shimmery.wav](libres/mp3/freesound-557104.mp3) ([fiche](https://freesound.org/s/557104/), CC0) | 0,22 [DNB_FX_37](https://www.musicradar.com/news/tech/sampleradar-100-free-drum-n-bass-fx-samples-444650) (MusicRadar, non redistribuable) | égalité |

### Effet

| Style | Cellule cible | Meilleur fabriqué | Meilleur A (libre) | Meilleur B (gratuit, non redistribuable) | Verdict |
|---|---|---|---|---|---|
| trap | fx-effet/* | 0,43 `trap-effet-descendant-01` | 0,39 [risers-and-fallers_faller-4s-strong](libres/mp3/wyzowl-faller-4s-strong.mp3) (Wyzowl, CC0) | 0,42 [SI_SynHitProc5-D02](https://www.musicradar.com/news/sampleradar-free-synth-impacts-samples) (MusicRadar, non redistribuable) | égalité |
| drill | fx-effet/* | 0,55 `drill-effet-descendant-02` | 0,39 [risers-and-fallers_faller-4s-strong](libres/mp3/wyzowl-faller-4s-strong.mp3) (Wyzowl, CC0) | 0,42 [SI_SynHitProc5-D02](https://www.musicradar.com/news/sampleradar-free-synth-impacts-samples) (MusicRadar, non redistribuable) | égalité |
| boom-bap | fx-effet/* | 0,52 `boom-bap-effet-descendant-01` | 0,39 [risers-and-fallers_faller-4s-strong](libres/mp3/wyzowl-faller-4s-strong.mp3) (Wyzowl, CC0) | 0,42 [SI_SynHitProc5-D02](https://www.musicradar.com/news/sampleradar-free-synth-impacts-samples) (MusicRadar, non redistribuable) | égalité |
| lo-fi | fx-effet/* | 0,72 `lo-fi-effet-descendant-01` | 0,39 [risers-and-fallers_faller-4s-strong](libres/mp3/wyzowl-faller-4s-strong.mp3) (Wyzowl, CC0) | 0,42 [SI_SynHitProc5-D02](https://www.musicradar.com/news/sampleradar-free-synth-impacts-samples) (MusicRadar, non redistribuable) | A = B à égalité, devant |
| house | fx-effet/* | 0,58 `house-effet-descendant-01` | 0,39 [risers-and-fallers_faller-4s-strong](libres/mp3/wyzowl-faller-4s-strong.mp3) (Wyzowl, CC0) | 0,42 [SI_SynHitProc5-D02](https://www.musicradar.com/news/sampleradar-free-synth-impacts-samples) (MusicRadar, non redistribuable) | égalité |
| techno | fx-effet/techno | 0,62 `techno-effet-descendant-01` | 0,41 [risers-and-fallers_faller-4s-strong](libres/mp3/wyzowl-faller-4s-strong.mp3) (Wyzowl, CC0) | 0,55 [SI_SynHitProc5-D02](https://www.musicradar.com/news/sampleradar-free-synth-impacts-samples) (MusicRadar, non redistribuable) | égalité |
| dnb | fx-effet/dnb | 0,56 `dnb-effet-descendant-01` | 0,53 [Digital Data Whoosh](libres/mp3/freesound-222493.mp3) ([fiche](https://freesound.org/s/222493/), CC0) | 0,40 [SI_SynHitProc5-B](https://www.musicradar.com/news/sampleradar-free-synth-impacts-samples) (MusicRadar, non redistribuable) | égalité |
| edm | fx-effet/* | 0,64 `edm-effet-descendant-01` | 0,39 [risers-and-fallers_faller-4s-strong](libres/mp3/wyzowl-faller-4s-strong.mp3) (Wyzowl, CC0) | 0,42 [SI_SynHitProc5-D02](https://www.musicradar.com/news/sampleradar-free-synth-impacts-samples) (MusicRadar, non redistribuable) | égalité |
| reggaeton-afro | fx-effet/* | 0,61 `reggaeton-afro-effet-descendant-02` | 0,39 [risers-and-fallers_faller-4s-strong](libres/mp3/wyzowl-faller-4s-strong.mp3) (Wyzowl, CC0) | 0,42 [SI_SynHitProc5-D02](https://www.musicradar.com/news/sampleradar-free-synth-impacts-samples) (MusicRadar, non redistribuable) | égalité |
| pop | fx-effet/* | 0,55 `pop-effet-descendant-02` | 0,39 [risers-and-fallers_faller-4s-strong](libres/mp3/wyzowl-faller-4s-strong.mp3) (Wyzowl, CC0) | 0,42 [SI_SynHitProc5-D02](https://www.musicradar.com/news/sampleradar-free-synth-impacts-samples) (MusicRadar, non redistribuable) | égalité |
| synthwave | fx-effet/* | 0,59 `synthwave-effet-descendant-02` | 0,39 [risers-and-fallers_faller-4s-strong](libres/mp3/wyzowl-faller-4s-strong.mp3) (Wyzowl, CC0) | 0,42 [SI_SynHitProc5-D02](https://www.musicradar.com/news/sampleradar-free-synth-impacts-samples) (MusicRadar, non redistribuable) | égalité |
| rnb | fx-effet/* | 0,85 `rnb-effet-montant-01` | 0,39 [risers-and-fallers_faller-4s-strong](libres/mp3/wyzowl-faller-4s-strong.mp3) (Wyzowl, CC0) | 0,42 [SI_SynHitProc5-D02](https://www.musicradar.com/news/sampleradar-free-synth-impacts-samples) (MusicRadar, non redistribuable) | A = B à égalité, devant |

## Sortes en plus (hoover, orchestra hit, vocal chop, cloche FM, vinyle, glitch)

Ces sortes n'ont pas de cible propre : chacune est mesurée sur la cellule « * » de la famille la plus proche.

| Sorte | Famille de mesure | Meilleur A (libre, commité) | Meilleur B |
|---|---|---|---|
| hoover | stab | 0,65 [hardhouse hoover](libres/mp3/freesound-11512.mp3) ([fiche](https://freesound.org/s/11512/), CC0) | — (aucun hoover B trouvé) |
| orchestra hit | stab | 0,18 [Orchestra Hit DIY](libres/mp3/freesound-794524.mp3) ([fiche](https://freesound.org/s/794524/), CC0) | 0,33 SI_SynHitProc4-B (synth impacts) |
| vocal chop | boucle | 0,39 [Gated vocal sample](libres/mp3/freesound-623055.mp3) ([fiche](https://freesound.org/s/623055/), CC0) | 0,44 GV_StripVocPad_130-Cmaj (glitch vocals) |
| cloche FM | pluck | 0,52 [DX10SDP5 (C)](libres/mp3/freesound-806082.mp3) ([fiche](https://freesound.org/s/806082/), CC0) | — |
| vinyle | ambiance | 0,81 (cellule lofi) [Vinyl Crackle (Real)](libres/mp3/freesound-434822.mp3) ([fiche](https://freesound.org/s/434822/), CC0) | — |
| glitch | effet | 0,76 [Glitch SFX 0.46 s](libres/mp3/freesound-673771.mp3) ([fiche](https://freesound.org/s/673771/), CC0) | 0,43 GV_VocMashB_170-03 (glitch vocals, cellule dnb) |
| basse reese | — | [Mono Poly Reese Bass](libres/mp3/freesound-25595.mp3) ([fiche](https://freesound.org/s/25595/), CC0), hors cible (> 4) | — |
| arrêt de bande | — | rien de libre et propre : à fabriquer | — |

## Sources fouillées

| Source | Classe | Ce qu'on en a tiré |
|---|---|---|
| [Freesound](https://freesound.org) (filtre CC0) | A | 1 492 sons mesurés, 20 commités |
| [archive.org ESQ-1 piano 2 chord](https://archive.org/details/esq-1-piano-2-chord) | A (PD Mark, posé par l'auteur) | 93 mesurés, aucun en tête |
| [archive.org VHS sample pack](https://archive.org/details/vhs-sample-pack) | A (PD Mark, posé par le label) | 27 mesurés, aucun en tête |
| [Wyzowl SFX Kit](https://huggingface.co/datasets/wyzowl/sfx-kit) | A (CC0 1.0) | 78 mesurés, le « faller 4 s » commité |
| [VCSL](https://github.com/sgossner/VCSL) (CC0, cloche TX81Z), [Sample Pi](https://github.com/alex-esc/sample-pi) (CC0) | A | non téléchargés : GitHub hors du périmètre de la session |
| Kenney Sci-Fi / Digital SFX, Juhani Junkala 512 SFX (OpenGameArt, CC0) | A | non téléchargés : site bloqué (403) |
| BigSoundBank, Freepats, Producerspace (CC0) | A | repérés ; peu de synthés (surtout bruitages, instruments acoustiques) ; Producerspace 1,6 Go non pris |
| Beckstrom modular (archive.org, CC-BY) | A (attribution) | repéré, non mesuré (CC-BY, pas CC0) |
| [MusicRadar SampleRadar](https://www.musicradar.com/news/sampleradar) (00s synths, synth impacts, dnb FX, glitch vocals) | B | 938 mesurés, jamais commités |
| 99Sounds, Legowelt, Sonniss GDC, Cymatics, Looperman, SampleSwap, BBC SFX | B (ou licence floue) | repérés ; non pris (inscription, courriel, usage non commercial ou licence floue) |
| KVR (fils p=9182158, p=9179000, p=8253774), uzu.lurk.org (Beckstrom) | — | lus : renvois vers les sources ci-dessus |
| Reddit (r/WeAreTheMusicMakers, r/edmproduction, r/synthesizers…), Gearspace, Bandcamp | — | **illisibles** depuis la session (403/406, recherche refusée) ; Gearspace vu en extraits seulement |

Leçon : hors de Freesound, aucune source A propre pour hoover, stab, pluck, arpège ou boucle de synthé ; les packs
gratuits de producteurs sont presque tous en « libre de droits, redistribution interdite » (B).

## À savoir en lisant les tableaux

1. Les A et B ne sont pas filtrés par style : le même libre peut être « meilleur » dans 10 styles de la cellule « * ».
2. Familles de substitution pour les sortes en plus (ex. vinyle mesuré comme une ambiance).
3. Le *PD Mark* d'archive.org est posé par celui qui a déposé le fichier, pas vérifié par un tiers.
4. « Orchestra Hit DIY » est un orchestra hit, pas un stab de synthé : il prouve que la cible stab aime les coups courts
   et brillants, pas qu'il faut remplacer nos stabs.
5. Le meilleur vrai stab de synthé libre est « Shock Stab 06 » ([mp3](libres/mp3/freesound-578380.mp3),
   [fiche](https://freesound.org/s/578380/)) à 0,42 ; l'ancienne progression trance
   ([mp3](libres/mp3/freesound-849664.mp3), [fiche](https://freesound.org/s/849664/)) est gardée pour l'écoute, à 0,68.
6. Deux verdicts « devant » sont à la limite de 0,3 : pluck rnb (fabriqué 0,195 contre A 0,50) et ambiance lo-fi
   (fabriqué 0,75 contre B 0,45). Le faller Wyzowl est mesuré sur son WAV (0,39) ; le MP3 commité donne 0,41, ce qui
   met l'effet lo-fi à la limite (écart 0,31).
7. « oscarsynth » (arpège) ne dure que 1,7 s ; « Gated vocal sample » est une voix hachée (vocal chop), pas une boucle lead.
8. Boucles et sons uniques sont comparés sur les mêmes mesures (attaque, déclin).

## Fichiers et licence

23 MP3 dans `docs/sons/libres/mp3/` : `freesound-<numéro>.mp3` (aperçu HQ de Freesound, non retouché) et
`wyzowl-faller-4s-strong.mp3` (WAV CC0 converti en MP3 192 k). Tous **CC0 1.0** (attribution notée par politesse ; licence de chaque fiche Freesound revérifiée le 2026-11-28, y compris 25595 et 701104 gardés de la version précédente).
Aucun son B, rien des packs privés de Benjamin, rien d'acheté.

## Critique à l'aveugle

Un relecteur neuf, qui n'a pas écrit ce rapport, a remesuré 6 sons : orchestra hit 794524 (0,178), accords 727082 (0,430),
B 00s_Arp[125]-E (0,352 et 0,860), B SI_SynHitProc4-B (0,331), fabriqué rnb-pluck-02 (0,194) : identiques au rapport.
Le faller Wyzowl donne 0,41 sur le MP3 contre 0,39 sur le WAV (noté). Corrections reprises : le stab libre bat nos stabs
dans les 12 styles (pas 7) ; en arpège le libre est plus près partout ; deux verdicts à la limite signalés ; titre de 701104
ajouté ; licences CC0 de 25595 et 701104 revérifiées sur Freesound.
