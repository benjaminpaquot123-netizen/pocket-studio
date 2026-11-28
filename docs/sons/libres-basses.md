# Basses libres contre nos basses (2026-11-28)

But : pour chaque famille (808, sub, reese, phrase de basse) et chaque style, garder le meilleur entre nos sons (branche `eq/son-cible-b`, `docs/sons/cible/basses/`) et les meilleurs sons gratuits trouvés sur le web ; repérer les types de basse que l'appli n'a pas du tout.

## En bref

- **808** : les nôtres gagnent partout, de loin (0,15 à 0,19 contre 0,38 au mieux pour le libre).
- **Sub** : les nôtres gagnent dans 7 styles sur 10. En dnb, techno et house, un sub gratuit fait mieux, mais seulement dans le groupe B (MusicRadar, non intégrable) ; en libre A, le meilleur fait un peu mieux en dnb (1,26 contre 1,66) et en techno (1,35 contre 1,45), mais tous restent loin de la cible.
- **Reese** : les nôtres gagnent sur 8 styles ; dnb et drill sont à égalité (écart < 0,04 sur la cible générique).
- **Phrase de basse** : les nôtres gagnent en dnb, drill, house, lo-fi, techno et trap ; un seul son libre (« Bass Loop B Major 140bpm », spaghettiSFX) fait mieux sur la cible générique (boom-bap, edm, pop, reggaeton) : c'est une seule victoire comptée quatre fois.
- **Types absents de l'appli** comblés par du libre A : growl dubstep, wobble, basse distordue, acid, Minimoog, contrebasse (boucle et pizz), basse électrique, donk, lo-fi 8 bits, pluck, saw, slap synthé, neuro, reese distordue (liste plus bas, MP3 dans `docs/sons/libres-basses/`).

## Groupes de licence

- **A** : CC0 (domaine public) ou licence libre redistribuable. On peut l'intégrer à l'appli ; les MP3 retenus sont dans `docs/sons/libres-basses/`.
- **B** : gratuit mais redistribution interdite (ici : packs MusicRadar SampleRadar, libres de droits pour faire de la musique, interdits de revente ou de redistribution). Mesurés et notés ; audio jamais commité.

## Méthode

- Même distance pour tous : `zb.distance` + `cible.tgt` + `cible.keys` de `eq/son-cible-b` (`scripts/son/cible/basses/`), mesures `cibles_desc` d'`eq/son-fin-essentia-01` (Essentia, librosa), cibles `docs/sons/fin/cibles/zone-b-basses.tsv`, audio décodé et ramené à 44,1 kHz, 20 premières secondes. Plus petit = plus près de la cible.
- Nos 320 sons remesurés depuis leurs MP3 : accord 0,995 avec les distances de l'index (moyenne 0,749 contre 0,735).
- 788 candidats libres A (Freesound filtre CC0, 773 ; archive.org « Source Guy bass collection » CC0, 15) + 30 notes CC0 de D. Smolken (contrebasse, basse électrique) + 439 sons B (MusicRadar « 808 weight » et « sub bass »).
- Filtres : titres de batterie, voix, guitare écartés ; part de grave minimale par famille ; sons faits par IA (Suno, « AI-generated ») écartés ; boucles avec batterie écartées. Licence CC0 vérifiée sur la page de chaque son retenu.
- Relecture à l'aveugle par un sous-agent neuf : 10 distances remesurées, toutes identiques ; pas de batterie détectée dans les boucles retenues (part percussive 2 à 17 %, comme les nôtres). Ses réserves sont reprises dans « Limites ».

## Tableau par famille et style

« Cible » : ligne de `zone-b-basses.tsv` utilisée (« tous » = ligne générique, faute de ligne propre au style). Verdict « égalité » quand l'écart avec le nôtre est sous 0,05.

### 808

| Style | Cible | Le nôtre (d) | Meilleur libre A (d, auteur) | Meilleur gratuit B (d) | Verdict |
|---|---|---|---|---|---|
| boom-bap | 808/tous | boom-bap-808-05 (0.175) | [fs353777](https://freesound.org/people/EKVelika/sounds/353777/) 808 Superbass Serum (0.379, EKVelika) | 0.516 MusicRadar « 808W_OneShot_E-04 » | **le nôtre** |
| dnb | 808/tous | dnb-808-04 (0.155) | [fs353777](https://freesound.org/people/EKVelika/sounds/353777/) 808 Superbass Serum (0.379, EKVelika) | 0.516 MusicRadar « 808W_OneShot_E-04 » | **le nôtre** |
| drill | 808/tous | drill-808-05 (0.158) | [fs353777](https://freesound.org/people/EKVelika/sounds/353777/) 808 Superbass Serum (0.379, EKVelika) | 0.516 MusicRadar « 808W_OneShot_E-04 » | **le nôtre** |
| edm | 808/tous | edm-808-06 (0.181) | [fs353777](https://freesound.org/people/EKVelika/sounds/353777/) 808 Superbass Serum (0.407, EKVelika) | 0.534 MusicRadar « 808W_OneShot_E-04 » | **le nôtre** |
| house | 808/tous | house-808-08 (0.168) | [fs353777](https://freesound.org/people/EKVelika/sounds/353777/) 808 Superbass Serum (0.379, EKVelika) | 0.516 MusicRadar « 808W_OneShot_E-04 » | **le nôtre** |
| lo-fi | 808/tous | lo-fi-808-05 (0.151) | [fs353777](https://freesound.org/people/EKVelika/sounds/353777/) 808 Superbass Serum (0.379, EKVelika) | 0.516 MusicRadar « 808W_OneShot_E-04 » | **le nôtre** |
| pop | 808/tous | pop-808-03 (0.188) | [fs353777](https://freesound.org/people/EKVelika/sounds/353777/) 808 Superbass Serum (0.379, EKVelika) | 0.516 MusicRadar « 808W_OneShot_E-04 » | **le nôtre** |
| reggaeton-afro | 808/tous | reggaeton-afro-808-08 (0.179) | [fs353777](https://freesound.org/people/EKVelika/sounds/353777/) 808 Superbass Serum (0.379, EKVelika) | 0.516 MusicRadar « 808W_OneShot_E-04 » | **le nôtre** |
| techno | 808/tous | techno-808-08 (0.172) | [fs353777](https://freesound.org/people/EKVelika/sounds/353777/) 808 Superbass Serum (0.379, EKVelika) | 0.516 MusicRadar « 808W_OneShot_E-04 » | **le nôtre** |
| trap | 808/tous | trap-808-08 (0.153) | [fs353777](https://freesound.org/people/EKVelika/sounds/353777/) 808 Superbass Serum (0.379, EKVelika) | 0.516 MusicRadar « 808W_OneShot_E-04 » | **le nôtre** |

### Sub

| Style | Cible | Le nôtre (d) | Meilleur libre A (d, auteur) | Meilleur gratuit B (d) | Verdict |
|---|---|---|---|---|---|
| boom-bap | sub-basse/tous | boom-bap-sub-02 (0.492) | [fs246299](https://freesound.org/people/unfa/sounds/246299/) Synth Doublebass A (0.718, unfa) | 0.623 MusicRadar « Firm Sub C2 » | **le nôtre** |
| dnb | sub-basse/dnb | dnb-sub-01 (1.659) | [fs510041](https://freesound.org/people/blakengouda/sounds/510041/) Sub Bass 9 - C.mp3 (1.255, blakengouda) | 0.920 MusicRadar « MS-20 Low End 12 » | gratuit B (non intégrable) |
| drill | sub-basse/tous | drill-sub-06 (0.370) | [fs246299](https://freesound.org/people/unfa/sounds/246299/) Synth Doublebass A (0.718, unfa) | 0.623 MusicRadar « Firm Sub C2 » | **le nôtre** |
| edm | sub-basse/tous | edm-sub-02 (0.383) | [fs246299](https://freesound.org/people/unfa/sounds/246299/) Synth Doublebass A (0.675, unfa) | 0.605 MusicRadar « Ms-20 Sub C2 » | **le nôtre** |
| house | sub-basse/house | house-sub-04 (0.736) | [fs339946](https://freesound.org/people/spinozz/sounds/339946/) E_distorto.wav (0.876, spinozz) | 0.641 MusicRadar « Firm Sub C2 » | gratuit B (non intégrable) |
| lo-fi | sub-basse/lofi | lo-fi-sub-08 (0.523) | [fs83550](https://freesound.org/people/zgump/sounds/83550/) Sub Mania 06.wav (1.192, zgump) | 0.878 MusicRadar « Whistle Sub D#2 » | **le nôtre** |
| pop | sub-basse/tous | pop-sub-02 (0.362) | [fs246299](https://freesound.org/people/unfa/sounds/246299/) Synth Doublebass A (0.718, unfa) | 0.623 MusicRadar « Firm Sub C2 » | **le nôtre** |
| reggaeton-afro | sub-basse/tous | reggaeton-afro-sub-03 (0.373) | [fs246299](https://freesound.org/people/unfa/sounds/246299/) Synth Doublebass A (0.718, unfa) | 0.623 MusicRadar « Firm Sub C2 » | **le nôtre** |
| techno | sub-basse/techno | techno-sub-01 (1.452) | [fs83550](https://freesound.org/people/zgump/sounds/83550/) Sub Mania 06.wav (1.350, zgump) | 1.110 MusicRadar « Low End 03 » | gratuit B (non intégrable) |
| trap | sub-basse/tous | trap-sub-05 (0.497) | [fs246299](https://freesound.org/people/unfa/sounds/246299/) Synth Doublebass A (0.718, unfa) | 0.623 MusicRadar « Firm Sub C2 » | **le nôtre** |

### Reese

| Style | Cible | Le nôtre (d) | Meilleur libre A (d, auteur) | Meilleur gratuit B (d) | Verdict |
|---|---|---|---|---|---|
| boom-bap | basse-reese/tous | boom-bap-reese-05 (0.709) | [fs528751](https://freesound.org/people/El_Bardo/sounds/528751/) Gnarlybass.wav (0.844, El_Bardo) | — | **le nôtre** |
| dnb | basse-reese/tous | dnb-reese-02 (0.876) | [fs528751](https://freesound.org/people/El_Bardo/sounds/528751/) Gnarlybass.wav (0.844, El_Bardo) | — | égalité |
| drill | basse-reese/tous | drill-reese-04 (0.882) | [fs528751](https://freesound.org/people/El_Bardo/sounds/528751/) Gnarlybass.wav (0.844, El_Bardo) | — | égalité |
| edm | basse-reese/tous | edm-reese-06 (0.669) | [fs528751](https://freesound.org/people/El_Bardo/sounds/528751/) Gnarlybass.wav (0.844, El_Bardo) | — | **le nôtre** |
| house | basse-reese/tous | house-reese-04 (0.654) | [fs528751](https://freesound.org/people/El_Bardo/sounds/528751/) Gnarlybass.wav (0.844, El_Bardo) | — | **le nôtre** |
| lo-fi | basse-reese/tous | lo-fi-reese-06 (0.520) | [fs528751](https://freesound.org/people/El_Bardo/sounds/528751/) Gnarlybass.wav (0.844, El_Bardo) | — | **le nôtre** |
| pop | basse-reese/tous | pop-reese-07 (0.748) | [fs528751](https://freesound.org/people/El_Bardo/sounds/528751/) Gnarlybass.wav (0.844, El_Bardo) | — | **le nôtre** |
| reggaeton-afro | basse-reese/tous | reggaeton-afro-reese-03 (0.646) | [fs528751](https://freesound.org/people/El_Bardo/sounds/528751/) Gnarlybass.wav (0.844, El_Bardo) | — | **le nôtre** |
| techno | basse-reese/tous | techno-reese-01 (0.794) | [fs528751](https://freesound.org/people/El_Bardo/sounds/528751/) Gnarlybass.wav (0.844, El_Bardo) | — | **le nôtre** |
| trap | basse-reese/tous | trap-reese-06 (0.698) | [fs528751](https://freesound.org/people/El_Bardo/sounds/528751/) Gnarlybass.wav (0.844, El_Bardo) | — | **le nôtre** |

### Phrase de basse

| Style | Cible | Le nôtre (d) | Meilleur libre A (d, auteur) | Meilleur gratuit B (d) | Verdict |
|---|---|---|---|---|---|
| boom-bap | phrase-de-basse/tous | boom-bap-phrase-04 (0.817) | [fs795746](https://freesound.org/people/spaghettiSFX/sounds/795746/) Bass Loop - B Major - 140bpm - (0.610, spaghettiSFX) | 0.750 MusicRadar « Sub loop 04 (75BPM) » | **libre A** (cible générique) |
| dnb | phrase-de-basse/dnb | dnb-phrase-03 (0.506) | [fs62432](https://freesound.org/people/fons/sounds/62432/) loop37.wav (1.211, fons) | 1.444 MusicRadar « Sub loop 34 (110BPM) » | **le nôtre** |
| drill | phrase-de-basse/trap | drill-phrase-06 (0.763) | [fs418950](https://freesound.org/people/@realdavidfloat/sounds/418950/) bass e gsharp dsharp.wav (1.260, @realdavidfloat) | 1.047 MusicRadar « Sub loop 41 (127BPM) » | **le nôtre** |
| edm | phrase-de-basse/tous | edm-phrase-01 (0.833) | [fs795746](https://freesound.org/people/spaghettiSFX/sounds/795746/) Bass Loop - B Major - 140bpm - (0.656, spaghettiSFX) | 0.753 MusicRadar « Sub loop 04 (75BPM) » | **libre A** (cible générique) |
| house | phrase-de-basse/house | house-phrase-05 (0.790) | [fs585154](https://freesound.org/people/Jackie-makes-noiz/sounds/585154/) deep bassline DRY.wav (0.935, Jackie-makes-noiz) | 0.872 MusicRadar « Sub loop 04 (75BPM) » | **le nôtre** |
| lo-fi | phrase-de-basse/lofi | lo-fi-phrase-08 (0.526) | [fs418950](https://freesound.org/people/@realdavidfloat/sounds/418950/) bass e gsharp dsharp.wav (1.694, @realdavidfloat) | 1.054 MusicRadar « Sub loop 40 (110BPM) » | **le nôtre** |
| pop | phrase-de-basse/tous | pop-phrase-04 (0.812) | [fs795746](https://freesound.org/people/spaghettiSFX/sounds/795746/) Bass Loop - B Major - 140bpm - (0.610, spaghettiSFX) | 0.750 MusicRadar « Sub loop 04 (75BPM) » | **libre A** (cible générique) |
| reggaeton-afro | phrase-de-basse/tous | reggaeton-afro-phrase-03 (0.934) | [fs795746](https://freesound.org/people/spaghettiSFX/sounds/795746/) Bass Loop - B Major - 140bpm - (0.610, spaghettiSFX) | 0.750 MusicRadar « Sub loop 04 (75BPM) » | **libre A** (cible générique) |
| techno | phrase-de-basse/techno | techno-phrase-03 (0.434) | [fs795746](https://freesound.org/people/spaghettiSFX/sounds/795746/) Bass Loop - B Major - 140bpm - (0.876, spaghettiSFX) | 0.672 MusicRadar « Sub loop 40 (110BPM) » | **le nôtre** |
| trap | phrase-de-basse/trap | trap-phrase-04 (0.845) | [fs418950](https://freesound.org/people/@realdavidfloat/sounds/418950/) bass e gsharp dsharp.wav (1.260, @realdavidfloat) | 1.047 MusicRadar « Sub loop 41 (127BPM) » | **le nôtre** |

## Types absents de l'appli, comblés par du libre A

L'appli a déjà : 808 et « Basse » par style (sons générés), l'acide Open303, les basses General MIDI (FluidR3). Elle n'a ni reese, ni sub pur, ni boucles de phrase de basse, ni les types ci-dessous. Distance : la plus petite sur les 10 styles, contre la cible « basse-synthé » (repère seulement : ces types sortent de nos cibles).

| Type | Fichier | Son (auteur) | Licence | d (style) |
|---|---|---|---|---|
| Growl dubstep | `growl-fs801942.mp3` | [Dispersed Modulated Dubstep Growl](https://freesound.org/people/Sauris/sounds/801942/) (Sauris) | CC0 | 0,851 (boom-bap) |
| Wobble | `wobble-fs384920.mp3` | [Ali BaBa Bass](https://freesound.org/people/Streetpoptunez/sounds/384920/) (Streetpoptunez) | CC0 | 1,109 (boom-bap) |
| Basse distordue | `distordue-fs442122.mp3` | [DistortedBass6](https://freesound.org/people/IgalBlech/sounds/442122/) (IgalBlech) | CC0 | 0,864 (boom-bap) |
| Acid (303) | `acid-fs361275.mp3` | [120_Dm_Hypno_05](https://freesound.org/people/SampleBasement/sounds/361275/) (SampleBasement) | CC0 | 1,136 (drill) |
| Minimoog | `moog-fs506109.mp3` | [OCT BASS](https://freesound.org/people/tripjazz/sounds/506109/) (tripjazz) | CC0 | 0,708 (drill) |
| Contrebasse (boucle) | `contrebasse-loop-fs620628.mp3` | [BGB Bass 85bpm](https://freesound.org/people/Bogotta/sounds/620628/) (Bogotta) | CC0 | 1,019 (dnb) |
| Contrebasse pizz | `contrebasse-pizz-smolken.mp3` | [Rubner double bass, pizz C1](https://danigb.github.io/samples/dsmolken/double-bass/dsmolkenrubnerbasspizz.websfz.json) (D. Smolken) | CC0 | 1,943 (techno) |
| Basse électrique | `basse-electrique-smolken.mp3` | [Growly bass, angry staccato E2](https://danigb.github.io/samples/dsmolken/glowlybass/growlybassangry.websfz.json) (D. Smolken) | CC0 | 2,506 (boom-bap) |
| Donk | `donk-sourceguy.mp3` | [Amazing Donk 1](https://archive.org/details/source-guy-bass-collection) (Source Guy) | CC0 | 1,140 (lo-fi) |
| Lo-fi 8 bits | `lofi-8bits-fs699753.mp3` | [Lo-fi Synth Bass - G](https://freesound.org/people/8bitmyketison/sounds/699753/) (8bitmyketison) | CC0 | 0,951 (drill) |
| Pluck modélisé | `pluck-fs424452.mp3` | [DeepPluckClsnBassF0160bpm](https://freesound.org/people/__stone__/sounds/424452/) (__stone__) | CC0 | 0,885 (techno) |
| Saw | `saw-fs73004.mp3` | [SYNTH BASS 0106](https://freesound.org/people/zgump/sounds/73004/) (zgump) | CC0 | 0,929 (dnb) |
| Slap synthé | `slap-synthe-fs721330.mp3` | [SPOOKY BASS WITH SLAP](https://freesound.org/people/tubbers/sounds/721330/) (tubbers) | CC0 | 0,665 (drill) |
| Neuro (boucle 160) | `neuro-fs876014.mp3` | [Neuro Bass Loop 4_160bpm](https://freesound.org/people/CAT-FOX_ALEX/sounds/876014/) (CAT-FOX_ALEX) | CC0 | 0,951 (boom-bap) |
| Reese distordue | `reese-distordue-fs367310.mp3` | [Reese Malfunction](https://freesound.org/people/PRIM_ORDial/sounds/367310/) (PRIM_ORDial) | CC0 | 0,919 (reese, tous) |

## MP3 retenus (`docs/sons/libres-basses/`)

Tous CC0 (aucune attribution exigée ; on cite quand même l'auteur). Freesound : aperçus HQ (MP3 ~128 kb/s), page vérifiée le 2026-11-28. Source Guy et D. Smolken : convertis en MP3 160 kb/s.

- Gagnants ou égalités contre les nôtres : `phrase-fs795746.mp3` ([Bass Loop - B Major - 140bpm - House Pluck](https://freesound.org/people/spaghettiSFX/sounds/795746/), spaghettiSFX), `sub-dnb-fs510041.mp3` ([Sub Bass 9 - C](https://freesound.org/people/blakengouda/sounds/510041/), blakengouda), `sub-techno-fs83550.mp3` ([Sub Mania 06](https://freesound.org/people/zgump/sounds/83550/), zgump), `reese-fs528751.mp3` ([Gnarlybass](https://freesound.org/people/El_Bardo/sounds/528751/), El_Bardo).
- Types absents : les 15 du tableau ci-dessus.

## Groupe B (mesuré, jamais commité)

- [MusicRadar SampleRadar « 473 free sub bass samples »](https://www.musicradar.com/news/sampleradar-sub-bass-samples) : meilleur que le nôtre en sub dnb (0,920 « MS-20 Low End 12 » contre 1,659), sub techno (1,110 « Low End 03 » contre 1,452), sub house (0,641 « Firm Sub C2 » contre 0,736). Licence : libre de droits dans un morceau, redistribution des samples interdite.
- [MusicRadar « 808 weight »](https://www.musicradar.com/news/sampleradar-808-weight-samples-1) : 0,516 au mieux, les nôtres gagnent partout.
- Repérés, non mesurés : Looperman (téléchargement réservé aux comptes), Samplefocus (accès refusé depuis la machine), 99Sounds « Bass City » (archive de 336 Mo, licence sans redistribution), Patches Zone 808, Cool WAV « Bass Shots », Patchbanks 808 (pages de vente à 0 €, licences sans redistribution). Reddit, KVR, Gearspace : pas de pack basse CC0 trouvé en plus de Freesound et archive.org.
- Écartés : sons faits par IA (Suno) sur Freesound, boucles avec batterie, archive.org « bass-2 » (origine incertaine), CD de samples Akai (droits flous).

## Limites (relecture à l'aveugle)

- Écart sous 0,05 = égalité (reese dnb, drill) ; une cible « tous » ne dit rien du style.
- Nos sons : le meilleur de 8 par style ; le libre : le meilleur de 90 à 190 candidats. Ce minimum sur plus de candidats avantage le libre : les petits écarts sont dans le bruit.
- Boucles : les libres durent 7 à 20 s (tronquées à 20 s), les nôtres ~5 s ; des fichiers longs donnent des médianes plus stables.
- Sub dnb : cible bâtie sur 14 sons de 3 packs, fragile. Sub techno : tous les candidats sont loin (> 1,1).
- À l'oreille : rien n'a été écouté par un humain ; à faire avant intégration.
