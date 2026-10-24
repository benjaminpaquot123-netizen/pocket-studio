# Analyse de hits : famille « Bass music » (dnb, liquid_dnb, neurofunk, jungle, jump_up, dubstep, riddim)

Date : 2026-10-23. Base lue (lecture seule, rien modifié, gradle non lancé) : `/home/user/ps-p22`
(`StyleRecipes.kt`, `StarterWizard.kt`, `WizardMix.kt`, `StyleMusic.kt`, `SoundLibrary.kt`, `FxPresets.kt`,
`Effects.kt`, `engine/src/main/cpp/core/Synth.cpp`, `docs/CORPS.md`, `docs/STYLE-FIXES.md`, `docs/plan/son.md`,
`docs/son/banc/apres-etape2.md`, `docs/RETOURS.md`) et la recherche précédente
`scratchpad/research/famille-dnb-dubstep.md` (phase 24-25), sur laquelle on s'appuie.

Méthode : aucune écoute possible ; tout vient de sources écrites publiques (fiches Attack Magazine « Beat
Dissected », tutoriels EDMProd, Native Instruments, Loopmasters, Mode Audio, Violet Recording, The Mix Review,
Hooktheory, Wikipedia, interviews UKF / RA / iZotope, bases songbpm / Beatport / AudioKeychain). Les BPM et
tonalités viennent de bases automatiques : ils sont marqués ≈ quand une seule base les donne ou quand deux
bases se contredisent. Le quota de recherche web s'est épuisé après une cinquantaine de requêtes ; le reste
a été lu directement (WebFetch). Tunebat, Beatport et Musicstax refusent la lecture directe (403).

Rappel de la phrase de Benjamin : « ça ressemble encore trop à de l'amateur, surtout les mélodies synthé ».
Dans cette famille, la « mélodie synthé », c'est la basse : c'est elle qui parle, et c'est là que l'écart
est le plus grand.

---------------------------------------------------------------------------------------------------------------
## 0. Ce que le générateur fait aujourd'hui pour toute la famille (pour juger les écarts)

Lu dans le code (phase 33) :
- **Forme** `SongForm.Build` pour dnb, liquid_dnb, neurofunk, jump_up, dubstep, riddim ; jungle reste en `Club`
  avec `drop = Layers(setOf(Role.Ride), setOf(Part.Bass))`. Build = Intro 16 · Montée 8 · Drop 32 · Break 8 ·
  Montée 8 · Drop 32 · Intro 16 = 120 mesures (2 min 45 à 174, 3 min 26 à 140 ; `sectionsOf`).
- **Une scène = une boucle de 4 mesures** (un accord par mesure). `bassPattern` et `leadPattern` font 16 ou 32
  pas et se répètent sur les 4 mesures (`StepLine.barOf`) ; le Drop de 32 mesures joue donc **8 fois la même
  boucle de basse**. Aucun champ de variation, de « switch » de basse, ni de phrase de 8 mesures.
- **Batterie** : la boucle de 1-2 mesures est écrite sur 64 pas et reçoit le **même fill à chaque fin de 4
  mesures** (`WizardMix.phrase` : roulement de caisse claire sur le dernier temps, kick coupé, charley ouvert).
  Aucun fill de 8 ou 16, aucune crash, aucune variante A/B, aucune note décalée de la grille.
- **Montée** : `rollHits` (noires → croches → doubles croches → roulements 2-3-4, vélocité 0,3 → 1,0) + charleys
  + nappe + « Souffle montant » (bruit blanc, filtre 0 → 0,9) + impact sur le 1 du Drop ; le dernier pas seul
  est silencieux.
- **Drop 2** : la 2e moitié du premier Drop ajoute le contre-chant ou le lead quand il ne joue qu'au Drop
  (`dropTwoLane`). Le Drop 2 est identique au Drop 1.
- **Basse** : une seule piste mono (synthé soustractif, 2 oscillateurs + sub carré, 1 filtre, LFO2 **triangle
  uniquement**, `Synth.cpp` ligne 46), plus, avec `subBass`, une piste « Sub » (« Sub pur », sinus, une octave
  plus bas, PUMP) ; la basse principale reçoit alors un passe-haut ≈ 120 Hz (`SUB_HIGH_PASS` 0,18 →
  40 × 450^0,18 ≈ 120 Hz). Glide 80 ms où la ligne dit `/`. Aucune automation de paramètre de synthé.
- **Vélocités** : X = 1, x = 0,6, o = 0,35 ; basse 0,9 (minuscule 0,63), lead 0,8, nappe 0,75. `NoteEdits.humanize`
  existe mais `build()` ne l'appelle jamais.
- **Liant** (`WizardMix.withGlue`) : bus batterie comp + sat ; master OTT 0,25 + comp + sat en Bass music.

Mesuré au banc (`docs/son/banc/apres-etape2.md`, sons intégrés) :

| Style | LUFS-I (cible) | Drop − Montée (cible ≥ +2) | Drop − Break (≥ +4) | Côté/centre (−15 à −9) | 60–250 Hz au Drop (repère ≈ −5) |
|---|---|---|---|---|---|
| dnb | −8,3 (−7, plafonné) | **−2,2** | −1,0 | −25,6 | −6,6 |
| liquid_dnb | −8,7 (−8) | −1,6 | 1,7 | −22,6 | −4,5 |
| neurofunk | −8,1 (−7, plafonné) | **−2,0** | 2,1 | −24,6 | −8,5 |
| jungle | −7,8 (−7) | −0,8 | −0,4 | −31,8 | −5,0 |
| jump_up | −8,5 (−7, plafonné) | **−2,1** | 0,1 | −21,9 | **−10,8** |
| dubstep | −8,6 (−7, plafonné) | **−2,5** | −0,7 | −18,7 | −8,9 |
| riddim | −9,2 (−7, plafonné) | **−3,1** | 1,4 | −18,1 | −8,4 |

Deux chiffres résument l'« amateur » : **le Drop est 2 à 3 LU plus faible que la Montée qui le précède**
(l'inverse d'un vrai morceau), et la bande 60-250 Hz est creuse de 4 à 6 dB dans les styles à basse
« growl / wobble » (sub sinus sous 60 Hz + basse coupée à 120 Hz = trou entre les deux).

Repères de sonie des vrais masters (sources) : drops de drum & bass et dubstep ≈ −4 à −6 LUFS intégré
(Mastering The Mix : « around −4 to −5 LUFS integrated in the drops » en DnB ; KVR : dubstep −5,5 à −6,5 ;
Soundcamps : bass/dubstep −4 à −7), true peak −1 dBTP, un ingénieur plus prudent à −7,5 / −9 ; The Mix Review sur
« Baddadan » : la basse carrée **écrête le master presque tout le temps** (soft-clip, harmoniques impaires
masquées par le carré), kick centré sur 60 Hz (40 Hz chez Skrillex).

---------------------------------------------------------------------------------------------------------------
## 1. Drum & bass (`dnb`, 174 BPM)

### 10 titres repères

| # | Artiste – Titre (année) | BPM | Tonalité | Source |
|---|---|---|---|---|
| 1 | Roni Size & Reprazent – Brown Paper Bag (1997) | ≈ 170 (85 ×2, re-edit 2008) | n/d | getsongbpm |
| 2 | Shimon & Andy C – Body Rock (2001) | ≈ 174 | ≈ La# mineur (mix Fabriclive) | AcousticBrainz |
| 3 | Pendulum – Hold Your Colour (2005) | 174 | Fa mineur | songbpm |
| 4 | Pendulum – Tarantula (2005) | 174 | ≈ Ré (10B) | AudioKeychain (base ouverte) ; prod. DJ Fresh, voix $pyda & Tenor Fly (Wikipedia) |
| 5 | Sub Focus – Timewarp (2008) | 174 | n/d | songbpm |
| 6 | DJ Fresh – Gold Dust (2008 / 2010) | ≈ 174 | n/d | Wikipedia (3× platine UK) |
| 7 | Sub Focus – Rock It (2009) | 174 | n/d | songbpm |
| 8 | Rudimental – Feel the Love (2012) | ≈ 174 (Hooktheory note 90) | Si♭ mineur (intro) / Ré♭ majeur (refrain) | Hooktheory ; structure intro 8, build 24, drop 32 (Mixed In Key) |
| 9 | Wilkinson – Afterglow (2013) | 174 | ≈ Sol majeur | songbpm |
| 10 | Chase & Status x Bou – Baddadan (2023) | 175 | ≈ Do# mineur ; mode phrygien dominant | songbpm ; The Mix Review |

### Traits du corpus
- **Tempo** : 174-175 partout (Beatportal / Sub Focus : « 174, the sweet spot » ; NI : « set the project tempo to
  174 BPM »). Les bases comptent souvent à moitié (87).
- **Tonalités** : mineures en Fa, Mi, Fa# surtout (EDMProd : « E minor, F minor and F# minor » ; le sub entre
  Ré#1 = 38,9 Hz et Sol#1 = 51,9 Hz). Accords : peu, i – VI – VII – i (Scary-type) ; Baddadan en **phrygien
  dominant** (Fa, Sol♭, Si♭m, Mi♭m + Ré♭ dans l'intro et le hook) ; Feel the Love module mineur → majeur relatif
  au refrain.
- **Structure (en mesures)** : NI : intro DJ 1-32, breakdown 33-48 (lead LFO), **drop à 49**, « new elements
  added each 16-bar section », lead coupé à 97, outro sans basse à 113. EDMProd : intro 32, breakdown 16-32,
  drop 16-32, variante de drop 16-32, breakdown, drop, outro ; 4 à 7 min. Mixed In Key (Feel the Love) : 8 / 24 /
  32. WA Production : 16 / 16 / 32 / 16 / 16 / 32.
- **Batterie** : two-step kick sur 1 et sur le « et » du 3 (NI : « kicks on the first and sixth 8th notes »,
  Attack : pas 1 et 11), caisse claire 2 et 4 ; notes fantômes de caisse claire sur les doubles croches après
  les coups (Unison : 15-40 % de vélocité), charleys en doubles croches 70-90 % **randomisés sur 4-8
  mesures**, ride en croches passe-haut + sidechain kick/caisse (Attack), kick fantôme en fin de 8e mesure,
  notes fantômes **poussées en retard de 10-20 ms** (Attack, Unison). Caisse claire en **3 couches** (poids,
  claque, transitoire) + clap léger, bus caisse comp + sat ; snare « body » 150-200 Hz, « 100 Hz kick to 200 Hz
  snare » (Mr Crux, EDMProd). Un ou plusieurs **breaks** (Amen, Think, Apache) en « tops », passés au
  passe-haut, « bracketing EQ ». Fill : « a slinky downward roll closes the 8-bar phrase » (Attack).
- **Basse** : reese (deux oscillateurs désaccordés ±0,27 demi-ton, Attack ; origine Casio CZ, Kevin Saunderson
  1988) ou supersaw filtrée + chorus/phaser/flanger, distorsion, OTT, **sinus propre en dessous** (EDMProd) ;
  Butterherz passe-haut 250 Hz + sub « Touchdown » sur le même MIDI (NI). Ligne de 2 mesures bouclée (NI),
  coups de pitch, glissés à l'octave.
- **Mix** : sub mono, kick passe-haut ~85 Hz (Attack) pour laisser la place à la basse ; master −4 à −6 LUFS ;
  Baddadan : basse carrée qui écrête, kick à 60 Hz.

### Notre recette aujourd'hui
`Kick "X.........X.....X.........X....."`, `Snare "....X..o.o..X.....o.X..o....X.2."`, `Hat
"o.x.o.x.o.x.o...o.x.o.x.o.x.o..."`, `OpenHat`, `Perc`, `Ride "..x...x..."` ; swing 0,05 ;
`bassPattern "R-------..R-----R-------..R--/O-"`, `bass = "Basse reese"`, `bassGlide`, `subBass` ; nappe
`Voicing.Seventh` tenue ; `leadPattern "R..R..5..R..O.5."` sur Supersaw oct. 5 ; Dark = mineur `[0,0,5,6]` ;
fx : PUMP basse, reverb nappe 0,3, reverb caisse 0,15, PUMP nappe.

### Écarts, du plus audible au moins audible
1. **Le Drop ne tombe pas** (−2,2 LU sous la Montée, −1,0 sous le Break). Référence : drop à la mesure 49 =
   le moment le plus fort (+3 à +6 LU). Changement (générateur) : Montée à −6 LU (vélocités du roulement
   plafonnées à 0,7, souffle −6 dB, nappe −4 dB, passe-haut 200 → 60 Hz sur le bus musique pendant la Montée),
   **un temps entier de silence** sur toutes les pistes avant le Drop (aujourd'hui un seul pas), crash + sub
   drop au 1 ; Drop : basse +1,5 dB et ride/perc2 ajoutés. Vérifier au banc : Drop − Montée ≥ +2 LU,
   Drop − Break ≥ +4 LU.
2. **La basse répète 8 fois la même boucle de 4 mesures.** Référence : « new elements each 16-bar section »
   (NI), ligne de 2 mesures variée, coups de pitch, glissés. Changement (recette + générateur) : `bassPattern`
   de 64 pas (4 mesures distinctes) **et** un champ `bassVariation` (mesures 5-8, par défaut : la même ligne
   avec le dernier temps remplacé par un glissé `/O-` ou une réponse à la quinte) ; règle générateur : les 2
   derniers temps de chaque bloc de 16 mesures sans basse (« tourne »). Exemple dnb :
   `R-------..R-----R-------..R--/O-R-------..R-----R---5---..R-3-..` + variation
   `R-------..R-----R-------..R--/O-R---R---..R-----O-------..5-R-..`. Vérifier : ≥ 2 phrases de basse
   distinctes par Drop ; pas deux blocs de 8 mesures identiques.
3. **Une seule couche de basse, mono, triangle.** Référence : reese + chorus/flanger + distorsion + OTT, sinus
   dessous, médiums larges. Notre « Basse reese » : OSC2_DETUNE 0,75, SWEEP 0,2, LFO2 libre 0,15 : un seul filtre
   qui respire lentement, pas de chorus, pas de distorsion (le preset `reese_dnb` = CHORUS + WOBBLE 0,12 +
   DISTORTION existe dans `FxPresets` mais la recette met seulement PUMP). Changement (recette) :
   `StyleFx("bass", preset = "reese_dnb", insert = "PUMP")` ; (générateur, M) une 3e piste « Médium » = mêmes
   notes une octave au-dessus, `fat_bass` + WIDENER, passe-haut 250 Hz, −6 dB ; passe-haut de la basse principale
   à ≈ 80 Hz (SUB_HIGH_PASS 0,18 → 0,11) pour remplir 60-120 Hz. Vérifier : 60-250 Hz ≥ −6 dB rel. ; côté/centre
   du Drop ≤ −15 dB.
4. **Batterie figée** : 2 mesures + le même fill tous les 4. Référence : ghosts variés toutes les 4 mesures,
   kick fantôme fin de 8, roulement descendant fin de 8, crash au 1 de chaque 16, break « tops ». Changement
   (générateur, règle famille) : fill de 4 = léger (2 ghosts), fill de 8 = roulement caisse 1/16 + kick coupé
   (l'actuel), fill de 16 = roulement 1/32 + crash au 1 suivant ; variantes A/B de charley/perc toutes les 8 ;
   humanize graine fixe : charleys ±8 % de vélocité, ghosts ±5 ms en retard ; caisse claire doublée d'un clap
   (lane Clap, `....X.......X...`, −6 dB) par défaut en dnb/jump_up/neuro. Vérifier : ≥ 6 mesures de batterie
   distinctes par morceau ; écart-type des vélocités de charley ≥ 0,05.
5. **Lead : un motif d'une mesure répété** (`R..R..5..R..O.5.`), aucune respiration. Référence : riffs Pendulum
   / Sub Focus = phrases de 4-8 mesures, notes longues et syncopes, question (mes. 1-2) / réponse (3-4), fin
   sur une note tenue, delay 1/8 pointé, unisson large. Changement (recette) : `leadPattern` de 64 pas, p. ex.
   `R---..5---..O---..5-..3-R-------....O-..7-..5---..3-R-----------` ; lead `delay = 0,25`, preset
   `wide_alive` ; (générateur) `leadGlide` sur `/`. Vérifier à l'oreille (A/B 16 mesures).
6. **Sonie** : −8,3 LUFS plafonné (la chaîne retire 6 LU). Référence −5 ±1. Changement : écrêteur doux sur le
   bus basse (DISTORTION « soft » 0,15) avant l'OTT, et kick accordé 55-65 Hz, court, passe-haut à 40 Hz, pour
   que l'écrêteur master travaille moins ; cible dnb −6 (pas −7) tant que la batterie v2 n'est pas plus dense.
   Vérifier : LUFS-I à ±1 de −6, PLR ≥ 6.
7. **Références** : garder la liste ; ajouter Pendulum – Hold Your Colour (2005) et Wilkinson – Afterglow (2013)
   (tonalités vérifiées) si on veut 7 entrées.

---------------------------------------------------------------------------------------------------------------
## 2. Liquid DnB (`liquid_dnb`, 174 BPM)

### 10 titres repères

| # | Artiste – Titre (année) | BPM | Tonalité | Source |
|---|---|---|---|---|
| 1 | LTJ Bukem – Horizons (1995) | ≈ 165-170 | n/d | (base introuvable) |
| 2 | Calibre – Second Sun (≈ 2003) | 174 | ≈ Fa# mineur | songbpm |
| 3 | Logistics – Together (2006) | 174 | ≈ Do# mineur | songbpm |
| 4 | High Contrast – If We Ever (2007) | 173-174 | Ré mineur | songbpm ; Beatport (remaster 2018, 87 BPM) |
| 5 | London Elektricity – Just One Second (2008) | 173 | ≈ Mi majeur | songbpm |
| 6 | Netsky – Memory Lane (2010) | 175 | Do# mineur ; refrain **VI9 – v6/5 – i7** | Hooktheory |
| 7 | Camo & Krooked – All Night (2011) | 175 | ≈ Do mineur | songbpm |
| 8 | Etherwood – Begin by Letting Go (2013) | 173 | ≈ Mi mineur | songbpm |
| 9 | Hybrid Minds – Halo (2015) | ≈ 174 | n/d | (base introuvable) |
| 10 | Netsky – Rio (2016) | 173 | ≈ Do majeur | songbpm |

### Traits du corpus
- **Tempo** 170-175, « 174 the most common » (EDMProd), ou 170-172 « for a more liquid feel » (Unison).
- **Harmonie** : la seule sous-famille avec de vrais accords : 7e et 9e (Memory Lane : VI9 → v6/5 → i7), Rhodes /
  piano / nappes, voicings sans fondamentale (la basse la joue), Am9 sur une basse Do = Cmaj7 (MusicRadar,
  recherche précédente). Beaucoup de morceaux en majeur relatif ou dorien lumineux.
- **Batterie** (EDMProd, projet complet) : kick 1.1.1 et 1.3.3 sur 4 mesures ; caisse 2 et 4 ; **ghosts sur 2.4 et
  3.2 à partir de la mesure 8, copiés toutes les 4 mesures, −6 à −12 dB** ; kick fantôme fin de chaque 8e
  mesure ; charleys à la croche + 2e boucle de charley ; break **Think** passe-haut, bas en volume, + shaker ;
  crash au 1 de la boucle avec un delay de 2 temps ; dans la montée, kick build noires → croches → doubles sur
  les 4 dernières mesures, reverse crash + riser.
- **Basse** : sub ronde (PWM filtré), « pattern de 4 mesures bouclé sur 16, **une relance de basse en plus
  toutes les 2 × 4 mesures**, basse **coupée 2 temps à la fin de la phrase de 16** », niveau ≈ −4 dB sous la
  batterie, grands sauts et petits pas.
- **Structure** (EDMProd, sections de 16) : intro 16 (ride qui s'estompe, piano muet une mesure sur deux),
  montée 16 (batterie filtrée), **drop 1A / 1B / 1C de 16** (1B : 4 premières mesures sans batterie ni basse,
  + contre-chant de cloche ; la 2e phrase de 8 du contre-chant une octave plus haut), outro 16 (piano filtré).
- **Mix** : réverbe partout mais dosée (piano −16 dB d'envoi, cloche −5, batterie −31), delay noire sur la
  cloche, Pro-L2 transparent au master. Les kicks et caisses de Calibre « kept low in the mix », percussions
  légères, break « edited for space ».

### Notre recette aujourd'hui
Kick two-step + relance, caisse avec ghosts, charley en doubles croches `xoxo…`, charley ouvert, shaker (voice 13),
ride ; swing 0,1 ; `bassPattern "R-------..R---5-R-------.5..R-3-"`, « Basse ronde », pas de sub ; `pad = "Piano
électrique"`, `Voicing.Ninth`, `chordRhythm "X-----X-----X---"`, oct. 3 ; lead = arpège Pluck 1/8 ; `signature =
Pad` ; fx : wide_alive + reverb 0,35 nappe, delay 0,3 lead, PUMP basse et nappe ; `counter` : aucun.

### Écarts
1. **Le Drop n'ajoute rien de neuf au fil des 32 mesures** (Drop − Montée −1,6 LU). Référence : 1A / 1B / 1C,
   avec un **contre-chant** (cloche, delay noire, 2e phrase une octave plus haut) qui arrive en 1B, et 4 mesures
   « trou » (sans batterie ni basse) au début de 1B. Changement (recette) : `counter = "Cloche"`,
   `counterRhythm = "..X.....X...X..."` (déjà géré par `withCounterTrack` + `dropTwoLane`) ; (générateur) option
   `breath = 4` : les 4 premières mesures de la 2e moitié du Drop sans kick ni basse. Vérifier : Drop 1B
   audible comme nouveau (banc : une couche de plus ; A/B).
2. **Basse immobile sur 16 mesures.** Référence : relance toutes les 8, coupée 2 temps en fin de 16. Changement :
   `bassVariation` = `R-------..R---5-R-------.5..R-3-R-------..R---5-O-----7-..5-3-..` (la 2e moitié monte à
   l'octave et redescend), règle « 2 derniers temps du bloc de 16 silencieux » (famille, cf. dnb 2).
3. **Harmonie trop fermée** : `Ninth` en empilant 0,2,4,6,8 depuis la fondamentale, à l'octave 3 : cinq notes
   serrées avec la fondamentale en bas, alors que les voicings liquid laissent la fondamentale à la basse.
   Changement (générateur, S) : `Voicing.NinthOpen` = 3e, 7e, 9e (+ 5e une octave plus haut), fondamentale
   omise ; progression Dark `[0,5,2,6]` → proposer `[5,4,0,0]` (VI9 – v7 – i7 – i7, façon Memory Lane) et
   Dreamy `[0,2,5,3]` → `[0,2,5,2]` ; vérifier : aucune 9e bémol (la règle existe), spectre du piano sans pic
   < 200 Hz.
4. **Pas de sub** (`subBass` absent) alors que la ligne est en octave 2 (Fa2 ≈ 87 Hz) : la bande < 60 Hz tient
   au kick seul. Référence : sub sinus « Touchdown », « sub −4,5 dB ». Changement : `subBass = true` (M7
   existe), ou `bassOctave = 1` avec le sub carré du synthé. Vérifier : sub < 60 Hz ≈ −2 dB rel. au Drop.
5. **Batterie** : ghosts figés, pas de break Think en « tops », pas de crash + delay. Changement : règle
   famille (dnb 4) + une ligne `Perc2` shaker 1/16 `xoxoxoxo…` à vélocités alternées (le « Think » faute de
   break échantillonné, F13).
6. **Montée** : la nôtre (roulement de caisse claire) est celle du dubstep ; en liquid c'est une **batterie
   filtrée** + kick build. Changement (générateur) : `buildStyle = "filter"` : Montée = batterie complète sous
   un passe-bas qui s'ouvre (sweep sur le bus batterie 0,25 → 1,0) + kick noires/croches/doubles sur les 4
   dernières mesures, pas de roulement. Vérifier : balayage mesuré au banc (air du bus batterie monte ≥ 10 dB
   sur la Montée).

---------------------------------------------------------------------------------------------------------------
## 3. Neurofunk (`neurofunk`, 174 BPM)

### 10 titres repères

| # | Artiste – Titre (année) | BPM | Tonalité | Source |
|---|---|---|---|---|
| 1 | Ed Rush & Optical – Funktion (1997) | ≈ 172-174 | n/d | Wikipedia (fondateur) |
| 2 | Bad Company – The Nine (1999) | ≈ 174 | n/d | — |
| 3 | Konflict – Messiah (2000) | ≈ 174 | n/d | — |
| 4 | Black Sun Empire – Arrakis (2005) | 174 | n/d | songbpm |
| 5 | Phace & Misanthrop – Desert Orgy (2009) | 172 | ≈ Sol#/La♭ (Fa mineur relatif ; « Motor » = 172, Fa mineur) | songbpm ; AcousticBrainz |
| 6 | Noisia – Stigma (2010) | 172 | ≈ Si mineur | songbpm |
| 7 | Noisia – Diplodocus (2012) | 172 | ≈ La | songbpm |
| 8 | Mefjus – Emulation (2014) | 172 | n/d | getsongbpm |
| 9 | Mefjus – Suicide Bassline (2014) | 172 (86) | Sol mineur | Beatport |
| 10 | Noisia – Could This Be (2016) | ≈ 174 | n/d | — |

À noter : **« Noisia – Machine Gun (2010) » est à 128 BPM, Fa# mineur** (songbpm, AudioKeychain) : c'est un
morceau breaks/dubstep, pas du neurofunk. Remplacer la référence de la recette par « Noisia – Stigma (2010) ».

### Traits du corpus
- **Tempo** 172-174, swing 0 ; « ticks like a clock but rolls like a Ferrari ».
- **Harmonie** : quasi nulle : « one-note basslines in a minor key, sometimes Phrygian or Locrian, with rhythmic
  play on the same root » (EDMProd). La nappe est un fond d'intro.
- **Basse = la mélodie.** « Extremely difficult – if not impossible – to generate [these] basslines straight
  out of a synth without further processing and resampling » (Computer Music). Chaîne type : FM / wavetable →
  distorsion (Decapitator drive ≈ 7) → filtre en mouvement (FilterFreak) → **flanger « phase difference 600 » pour
  la largeur** → rééchantillonnage en 3 passes (son brut → effets → bounce, « factory conveyor belt »,
  BassGorilla) ; formant à faible mix ; **sub séparé et stable** ; Noisia : Trash sur « almost every bass
  sound », « dryer distortion keeps the low end transparent », multibande + Clip Control pour le sub, bande
  separator pour la largeur ; jusqu'à ≈ 100 pistes (EDMProd).
- **Batterie** : two-step très régulier, caisse sèche transposée (+5/+6 demi-tons, queue coupée), kick avancé
  avant le 3 en 2e mesure, **ghost kicks** = kick rebondi +1 octave raccourci de 50 %, dispersés (Deem) ; Noisia :
  vrais samples de batterie, overheads fortement distordus, un groupe par élément.
- **Arrangement** : comme la dnb, mais le **Drop 2 change de basse** (« switch-up ») et chaque 16 mesures apporte
  un nouveau timbre ; question / réponse entre deux basses médium sur une sub fixe.

### Notre recette aujourd'hui
Kick `X.........X.....X......X..x.....`, caisse `....X.......X.......X.......X..o`, charley, perc ; swing 0 ;
`bassPattern "R-.R--.R-.......R-.R--.R-...R-O-"`, « Basse qui parle » (LFO2 1/2 mesure, glide), pack neuro:bass,
`subBass` ; `leadPattern "..........O-.5-..........O-....."` « Voix robot » oct. 3 ; nappe Essaim Sus2 (par
défaut éteinte) ; phrygien `[0,0,1,0]` ; fx : fat_bass + PUMP basse, FLANGER lead.

### Écarts
1. **Un seul timbre de basse pendant 64 mesures de Drop.** Référence : le timbre bouge sans arrêt, Drop 2 =
   autre basse. Changement (générateur, L mais c'est LE point du style = F14) : automation de paramètres du
   synthé par section (kind 6 sur piste instrument, appliquée sans verrou) : `CUTOFF` 0,3 → 0,6 sur chaque bloc
   de 8, `LFO2_SYNC` 1/2 → 1/8 au Drop 2, `OSC2_DETUNE` ±0,1 par bloc ; **`bassSwitch`** (recette) : un second
   preset (« Basse growl ») qui remplace la basse au Drop 2. À défaut (S) : second Drop avec `fat_bass` →
   `wob_dubstep` sur la piste basse via un insert dont le mix est automatisé (INSERT_PARAM existe). Vérifier :
   enveloppe spectrale de la piste basse différente entre Drop 1 et Drop 2 (centroïde ± 20 %).
2. **Question / réponse plate** : la réponse « Voix robot » (Plaits, model 11) sonne synthé-jouet à côté d'une
   basse ; la référence répond avec une **deuxième basse médium** (formant / FM). Changement (recette) :
   `lead = "Basse growl"` (comme jump_up) oct. 3 avec `fat_bass` + FLANGER, ou pack `neurofunk:lead` ; garder
   la grille croisée. Vérifier à l'oreille.
3. **Pas de rééchantillonnage, pas de formant, pas de largeur** : côté/centre −24,6 dB. Changement
   (recette, maintenant) : `StyleFx("bass", preset = "fat_bass", insert = "FLANGER")` + PUMP sur le Sub
   seulement (il l'a déjà) ; (moteur, M) un **filtre formant** (vowel A/E/I/O/U, 2 passe-bande, morph) comme
   insert, balayé par automation : c'est ce qui fait « parler » ; largeur via WIDENER sur la basse médium (le
   Sub reste mono). Vérifier : corrélation du Drop ≤ 0,85, < 120 Hz ≥ 0,95.
4. **Batterie trop propre, trop régulière** : pas de ghost kicks, caisse non transposée. Changement (recette) :
   `Line(Role.Perc2, "..o.....o..o......o.....o.......", voice = kick2 pitch +12, decay court)` (ghost kicks),
   caisse claire pack `neurofunk` ou kit avec `pitch = +5` (la ligne `kit` permet `source`, la hauteur par
   ligne demande un champ `pitch` dans `Line` : S) ; fill de 8 = roulement de kick 1/16 plutôt que de caisse.
5. **Sonie** : −8,1 plafonné ; référence −5. Même remède que dnb 6 (écrêteur bus basse, kick 55-65 Hz).
6. **Références** : « Machine Gun » → « Stigma (2010) » ; ajouter « Noisia – Diplodocus (2012) ».

---------------------------------------------------------------------------------------------------------------
## 4. Jungle (`jungle`, 165 BPM)

### 10 titres repères

| # | Artiste – Titre (année) | BPM | Tonalité | Source |
|---|---|---|---|---|
| 1 | Origin Unknown – Valley of the Shadows (1993) | 160 | n/d | songbpm ; sons du CD Future Music, fait en 4 h (Wikipedia) |
| 2 | M-Beat feat. General Levy – Incredible (1994) | 165 | ≈ Mi | songbpm |
| 3 | Shy FX & UK Apache – Original Nuttah (1994) | ≈ 160-165 | n/d | Wikipedia (voix sur « Gangsta Kid », intro Goodfellas + cuivres) |
| 4 | Renegade – Terrorist (1994) | ≈ 160 | n/d | Attack / Wikipedia (reese de Saunderson samplée) |
| 5 | Goldie – Inner City Life (1994) | ≈ 155-160 | ≈ Sol | songbpm (155) ; sample « Funky Mule », 7:00 / suite de 21 min (Wikipedia) |
| 6 | Dead Dred – Dred Bass (1994) | ≈ 160 | n/d | (nom du preset « Basse dred » de STYLE-FIXES) |
| 7 | Remarc – R.I.P. (1994) | ≈ 160 | n/d | — |
| 8 | Dillinja – The Angels Fell (1995) | 160 | ≈ Si | songbpm |
| 9 | DJ Zinc – Super Sharp Shooter (1996) | ≈ 165 | n/d | — |
| 10 | Nia Archives – Baianá (2022) | ≈ 165-170 | n/d | DJ Mag / Mixmag (« harder amen break roller with a Reese bassline ») |

### Traits du corpus
- **Tempo** : 155-165 en 1993-95 (Wikipedia DnB : « roughly 155–165 by 1993 »), 170-174 dans les tutos actuels
  (EDMProd : « 174 the classic tempo for Jungle »). Notre 165 est juste pour la période.
- **Batterie = un break joué, pas programmé.** Amen (4 mesures, ≈ 136 BPM d'origine, 7 s) : mesures 1-2 =
  groove funk (kicks en contretemps de croche, le kick du 3 retiré et le « et » du 3 doublé ; caisse 2 et 4 avec
  pickups à la double croche avant le 3 et avant le 1 ; ride en croches), **mesure 3 : caisse du 4 retardée
  d'une croche**, **mesure 4 : pas de kick sur le 1, « et » du 1 doublé, charley ouvert sur le « et » du 3,
  crash en avance** (Ethan Hein, Wikipedia). « Hardly any [hit is] exactly where [it is] supposed to be » : le
  groove est dans le micro-timing et le timbre. Techniques : découpage et réordonnancement des tranches, pitch,
  **alternance de deux breaks chaque mesure** (Wikipedia : « switching »), Tramen ; Loopmasters : deux breaks
  superposés (Clyde + PushItKid) à 170, désaccordés de +20 cents, passe-haut 160 Hz sur le second.
- **Tourne toutes les 8 mesures** (EDMProd) : dernière mesure de chaque bloc de 8 = batterie retirée, autre
  break, filtre, basse coupée, reverse crash ou upsweep (à gauche). **16 premières mesures = batterie + basse
  seules**, cuivres et voix au 3e bloc de 8.
- **Basse** : contrebasse samplée, sub séparé, **transposée de −6 demi-tons pour que le sub tombe entre Mi et
  Sol** (EDMProd), mi-temps, petites phrases ; ou reese (Terrorist, Baianá).
- **Signature** : voix ragga (Incredible, Original Nuttah), sirènes, rewinds, cordes et nappes (Valley of the
  Shadows : cordes + arpège + « long dark tunnel »).
- **Mix** : OTT 35 % downward, saturation, réverbe « room » parallèle, transient shaper qui coupe le sustain,
  mélodie « duckée » par kick et caisse, reverse crash à gauche.

### Notre recette aujourd'hui
Grille Amen sur 2 mesures (kick `X.X.......Xx....X.X.......X.....`, caisse `....X..o.x..X..o....X..o.3....X.`,
charley, charley ouvert, ride) ; swing 0,1 ; `bassPattern "R-------..R---5-R-----3-..5---7-"` « Basse sub » oct. 2 ;
nappe chaude 7e ; lead Hoover `O---------------................` ; forme Club, `drop = ride + basse`,
`breakWith = charley + nappe + lead`, `riser` ; fx : punch batterie, PUMP basse, delay lead, reverb nappe.

### Écarts
1. **Des one-shots à la place d'un break** : c'est le défaut n° 1, déjà noté (F13). Changement (moteur + packs,
   L) : pack CC0 de breaks (Amen-like rejoué, Think-like), tranché par le Slicer, ligne de recette `source:
   "breaks"` avec tranche et hauteur par coup, **grille de 64 pas** (les 4 mesures de l'Amen, mesures 3-4
   différentes). En attendant (recette, S) : écrire les 4 mesures (`F4`, 64 pas) : mes. 3 caisse du 4 →
   `..............X.`, mes. 4 sans kick sur le 1, charley ouvert au « et » du 3 ; micro-timing : ghosts à +5 ms,
   kicks de contretemps à −3 ms (graine fixe). Vérifier : 4 mesures de batterie distinctes dans la boucle ;
   écoute A/B.
2. **Pas de tourne à 8 ni de 16 mesures batterie + basse seules.** Notre Club : Intro 16 (kick, charley, ride ?
   non : ride au Drop) sans basse, Groove 16 sans basse (`drop` retient la basse), Drop 32. Référence : batterie +
   basse dès le début, tourne à chaque 8. Changement (recette) : retirer `Part.Bass` de `drop` (garder Ride),
   passer en `form = Build` avec `breakWith = Layers(setOf(Role.Hat), setOf(Part.Pad))` ; (générateur, famille)
   fill de 8 = « tourne » : dernière mesure sans kick ni caisse, charley seul, reverse crash. Vérifier : Drop −
   Groove ≥ +2 LU grâce aux couches (ride, hoover, perc).
3. **Basse** : juste, mais « Basse sub » (carré + sub) coupée à 0,3 reste terne ; référence : contrebasse
   transposée −6 + sub, ou reese. Changement (recette) : sub séparé `subBass = true` + `bass = "Basse ronde"` oct.
   2 (le corps 80-250 Hz), ou pack `jungle:bass|Basse ronde` ; glissés `/` sur la fin de la 2e mesure (`glide`).
4. **Signature sound-system absente** : voix ragga, sirène, rewind. Changement (packs) : kind `vocal`/`shout`
   dans le pack jungle (CC0, voix synthétique acceptable) joué par `Perc2` 1 fois par 8 mesures ; « Sirène dub »
   (LFO1 → hauteur, F10) sur le lead en remplacement du Hoover pour Dark ; TAPE_STOP sur le bus batterie en fin
   de Break (rewind).
5. **Mono** (−31,8 dB) : réverbe room parallèle sur la batterie (preset `small_room` en parallèle, send 0,15) et
   WIDENER sur la nappe. Vérifier : côté/centre ≤ −18 dB au Drop (le genre reste assez centré).

---------------------------------------------------------------------------------------------------------------
## 5. Jump up DnB (`jump_up`, 174 BPM)

### 10 titres repères

| # | Artiste – Titre (année) | BPM | Tonalité | Source |
|---|---|---|---|---|
| 1 | Dillinja – Twist 'Em Out (≈ 2002) | ≈ 174 | n/d | — |
| 2 | DJ Hazard & D*Minds – Mr Happy (2007) | 174 (87) | Fa mineur (Beatport) / ≈ Ré♭ (songbpm) | Beatport, songbpm |
| 3 | DJ Hazard – Bricks Don't Roll (≈ 2008) | ≈ 174 | n/d | — |
| 4 | Macky Gee – Tour (2017) | 175 | Fa mineur | songbpm |
| 5 | Kanine – Steamroller (2018) | 175 | n/d | songbpm |
| 6 | Sub Focus & Dimension – Desire (2018) | 174 | n/d | songbpm |
| 7 | Dimension – Offender (2020) | 174 | ≈ Si♭ majeur (Sol mineur relatif) | songbpm |
| 8 | Bou – Veteran (2020) | 174 (87) | ≈ Si | songbpm |
| 9 | Chase & Status, Hedex & ArrDee – Liquor & Cigarettes (2023) | ≈ 174 | n/d | — |
| 10 | Chase & Status x Bou – Baddadan (2023) | 175 | ≈ Do# mineur ; phrygien dominant | songbpm ; The Mix Review |

### Traits du corpus
- **Tempo** 174-175 (Loopmasters : « Set your BPM to 175 »).
- **Harmonie** : une note (« one-note basslines… Phrygian or Locrian, rhythmic play on the same root »,
  EDMProd) ; Fa mineur très fréquent (Mr Happy, Tour) ; Baddadan phrygien dominant (Fa – Sol♭ – Si♭m – Mi♭m).
- **Batterie** « really basic » (dnbforum) : kick 1 et « et » du 3, grosse caisse claire 2 et 4 + clap, charleys
  croches, fill en fin de 2 mesures. Baddadan : kick centré 60 Hz, charley ouvert qui masque le « crunch » quand
  kick et basse tombent ensemble.
- **Basse = la vedette**, « really similar to the wobble sound in dubstep » ; **deux couches minimum** : médium
  (sample / FM / formant, « frog », « womp », hoover) + **sub = le même MIDI −12 demi-tons, 1 seule voix (aucune
  note qui se chevauche), départ de sample avancé pour attaquer vite, notes de sub longues et serrées**
  (Loopmasters) ; HL (Magnetic, 2026) : 7 couches au drop (2 womp, lead, sub épais, pulse, frog noise, reese),
  Trash 2 multibande (tube < 100 Hz, ampli 100 Hz-4 kHz, fizz en haut) ; **« switch-ups » = la même ligne sur un
  autre patch** toutes les 8-16 mesures ; phrases courtes, fortes, « call-and-response » basse ↔ batterie ;
  Baddadan : basse à caractère carré qui écrête le master.
- **Arrangement** : intro courte, montée claire, drop qui expose la basse tout de suite, section
  question/réponse, breakdown, drop 2 ; voix / shouts de MC (Baddadan : Irah, Flowdan, Trigga, Takura).

### Notre recette aujourd'hui
Kick `X.........X.....`, caisse `....X.......X.......X.......X.2.`, clap sur la caisse, charley, perc ; swing 0 ;
`bassPattern "R-.R..R-..R-....R-.R..R-..R--/O-"` « Basse growl » (LFO2 1/16, FILTER_ENV, résonance 0,6), glide,
`subBass` ; `leadPattern "............O-7-........5-......"` « Basse wobble » oct. 3 ; nappe triade (signature) ;
phrygien `[0,0,0,1]` ; fx : growl + PUMP basse, fat_bass + PUMP lead, punch kick/caisse, OTT master.

### Écarts
1. **Trou 60-250 Hz (−10,8 dB, le pire de la famille)** : growl sans sub propre (SUB 0) coupé à 120 Hz + sinus
   une octave plus bas = rien entre 50 et 120 Hz, alors que « le sub, c'est le même MIDI −12 » et que le corps
   100 Hz-4 kHz est saturé à part. Changement : passe-haut de la basse à 80 Hz ; Sub = « Basse sub » (carré
   filtré) plutôt que « Sub pur » en jump up, ou sinus + 2e harmonique (nouveau preset « Sub chaud », S) ;
   DISTORTION du growl en 2 bandes (le preset `growl` est large bande). Vérifier : 60-250 Hz ≥ −6 dB rel.
2. **Aucun switch-up** : 64 mesures de Drop sur un seul growl. Référence : « duplicate the bassline and assign a
   different patch ». Changement (recette) : `bassSwitch = "Basse wobble"` (ou pack `jump_up:bass`) au Drop 2, et
   au bloc 17-32 du Drop 1 un `bassVariation` (mêmes rythmes, réponse `O` → `5`). (Générateur : une 2e piste
   basse muette dont le fader s'ouvre au Drop 2, comme `dropTwoLane`.) Vérifier : spectre de la basse différent
   entre Drop 1 et Drop 2.
3. **Le wobble répond en même temps que la basse pose la question.** La grille croise bien les silences à
   l'intérieur de la mesure, mais 16 pas de « R-.R..R-..R-.... » puis une réponse sur 4 pas : trop court pour
   qu'on entende un dialogue. Référence : phrases « courtes, fortes, distinctes », réponse d'une mesure sur
   deux. Changement : `bassPattern` 64 pas avec mesures 1-2 question, 3 réponse basse, 4 silence pour le lead ;
   `leadPattern` 64 pas qui ne joue qu'en mesure 4 (`............O-7-` sur la mesure 4 seulement, le reste `.`).
4. **Montée > Drop** (−2,1 LU) : même remède famille (dnb 1) ; ici surtout : le lead « Basse wobble » +
   `fat_bass` + PUMP est aussi fort que la basse et arrive à la 2e moitié du Drop via `dropTwoLane` : bien, mais
   le Drop 1A doit être **basse + batterie seules** (ni nappe ni lead) : `signature = Pad` met la nappe partout ;
   retirer `signature` (la nappe reste en Intro/Montée/Break par `breakWith`). Vérifier : couches du Drop 1A =
   kick, caisse, clap, charley, perc, basse, sub.
5. **Caisse claire** : une seule couche + clap identique sur chaque coup ; référence : caisse énorme en 3
   couches, ghost percus. Changement : règle famille (clap −6 dB, punch), fill de 16 = roulement 1/32 crescendo +
   crash ; shouts de MC (pack `vocal`, Perc2, 1 par 8 mesures) : c'est ce qui fait « salle ».
6. **Références** : ajouter « Macky Gee – Tour » (déjà), « Bou – Veteran (2020) » ; les 7 actuelles sont bonnes.

---------------------------------------------------------------------------------------------------------------
## 6. Dubstep (`dubstep`, 140 BPM)

### 10 titres repères

| # | Artiste – Titre (année) | BPM | Tonalité | Source |
|---|---|---|---|---|
| 1 | Skream – Midnight Request Line (2005) | 140 | ≈ Ré♭ (songbpm) ; « key change » au refrain (Wire) | songbpm, RA, MusicRadar (FruityLoops, arpégiateur, « hitting two keys ») |
| 2 | Digital Mystikz – Anti War Dub (2006) | ≈ 140 | n/d | — |
| 3 | Benga & Coki – Night (2007) | 140 | ≈ La (Fa# mineur relatif) | songbpm ; disque de l'année 2007 de Gilles Peterson (Wikipedia) |
| 4 | Rusko – Cockney Thug (2007) | 139-140 | Fa# mineur | Beatport |
| 5 | Caspa & Rusko – Where's My Money (2007) | ≈ 140 | n/d | — |
| 6 | Coki – Spongebob (2007) | ≈ 140 | n/d | RA (LFO → hauteur + filtre, « heavy harmonics ») |
| 7 | Benga – 26 Basslines (2008) | 140 | ≈ Ré♭ | songbpm |
| 8 | Skrillex – Scary Monsters and Nice Sprites (2010) | 140 | Sol mineur ; VI – VII – i – VII | Hooktheory, AudioKeychain ; fait sur un laptop (Wikipedia) |
| 9 | Flux Pavilion – I Can't Stop (2010) | 140-141 | ≈ La♭ / Do mineur (bases contradictoires) | AudioKeychain, Hooktheory |
| 10 | Rusko – Woo Boost (2010) | 140 | ≈ Si mineur | songbpm |

### Traits du corpus
- **Tempo** 132-142, « 140 » (Wikipedia) ; ± 5 (Mode Audio).
- **Harmonie** : mineur ou **phrygien**, triton dans les riffs (Wikipedia) ; Mi mineur « pour le sub » (EDMProd) ;
  Scary Monsters en Sol mineur avec 4 accords (VI – VII – i – VII), Midnight Request Line = arpège de deux
  touches avec un changement de tonalité au refrain.
- **Batterie** : demi-temps, kick 1, **caisse + clap sur le 3** (caisse électronique grave 160-200 Hz + clap
  passe-haut réverbéré), charleys et shaker à vitesse normale, **shufflés (swing 50 %) et muets sur le 3**,
  tambourin, cajón coupé à 100 Hz, **une caisse acoustique une fois par 2 mesures dans un écho filtré**
  (Attack « Mystik ») ; **triolet de kicks entre le 1 et le 3**, 3 charleys égaux à la place de 2 (Mode Audio) ;
  dernier kick de la boucle moins fort ; compression bus −12 dB 3:1 + parallèle −15 dB 10:1.
- **Basse** : **sub sinus < 100 Hz** (Mi1/Fa1), mono ; au-dessus une couche médium : carré/triangle + distorsion,
  **LFO sur la coupure à 1/4, 1/8, 1/16 (et 1/8 pointé, EDMProd), forme sinus = rond, dent de scie = pluck,
  carré = gate ; vitesse automatisée note par note** (« A single repeating wobble gets old within a few
  seconds », Violet) ; carré 1/4 + triangle faible = la basse « parle » ; FM de l'oscillateur B, distorsion après
  le filtre (« intensifying the growl as the filter opens »), OTT, comb, Hyper Dimension ; **médium =
  rééchantillonnage du sub distordu et découpé** (EDMProd) ; macros coupure / FM / drive ; EQ : creux 100-300
  Hz, bosse 1-3 kHz ; **couches : le même MIDI copié sur plusieurs synthés, puis notes supprimées par couche
  pour que la basse saute d'un son à l'autre** (Mode Audio).
- **Écriture** : « a melody, then a variation over the next 8 bars » ; longueur de note = nombre de wobs.
- **Structure** : drop après la 16e ou 32e mesure, où la percussion s'arrête puis revient avec le sub
  (Wikipedia) ; EDMProd : intro 8 (hook sans basse, vocodeur, bruit de cassette, nappes), build 8 (kick par
  temps, doublé après 4 puis après 2 mesures, aigus retirés, riser, crashes, voix avant le drop), drop 1A/1B/1C
  de 8 ; rewinds.
- **Mix** : sidechain sub ↔ kick ; −5,5 à −6,5 LUFS (brostep) ; Skream : « unusually high-pitched bassline »
  faute de mixdown.

### Notre recette aujourd'hui
Kick 2 mesures `X.........x.....X.....x...x.....`, caisse `........X.......`, charley muet sur le 3, perc ; swing
0,15 ; `bassPattern "R-------..R---O-R-----------2-.."` « Basse wobble » (LFO2 triangle **1/8 fixe**, SUB 0,7),
`subBass` ; nappe triade (signature) ; lead Cloche `R.......3.....2.` oct. 5 + dub_echo ; phrygien ; fx : reverb +
delay caisse, reverb + delay nappe, fat_bass basse, PUMP nappe.

### Écarts
1. **Un seul wob, toujours à la même vitesse** : LFO2 triangle 1/8, jamais carré, jamais 1/4 ni 1/16, jamais
   automatisé. C'est exactement ce que Violet appelle « old within a few seconds ». Changement (moteur, S) :
   paramètre `LFO2_SHAPE` (tri / carré / dent de scie / sinus) dans `SynthPatch` + `Synth.cpp` (DaisySP a
   déjà `WAVE_SQUARE`) ; (générateur, M = F14) automation `LFO2_SYNC` par bloc : Drop 1A 1/8, 1B 1/4 sur les
   notes longues, 1C 1/16, dernière mesure de chaque 8 = 1/8 triolet ; (recette) `bassPattern` 64 pas avec
   notes de longueurs différentes (le nombre de wobs par note), p. ex. `R-------..R---O-R-----------2-..R---R---
   ..R-5---R-------....3-O-` et un `bassVariation`. Vérifier : taux de modulation de l'enveloppe de la basse
   change ≥ 2 fois par 16 mesures (banc : spectre de l'enveloppe RMS 10 ms).
2. **Le Drop est plus faible que la Montée (−2,5 LU)** et le Break est au niveau du Drop (−0,7). Référence : la
   percussion s'arrête, puis revient avec le sub ; silence avant le drop ; voix « pre-drop ». Changement : famille
   (dnb 1) + en dubstep **Montée sans caisse claire** (le roulement de caisse est jump-up/EDM) : `buildStyle =
   "kick"` (kick par temps doublé après 4 et 2 mesures, aigus coupés, souffle), 1 temps de silence, impact + sub
   drop (note sinus Mi1 glissant d'une octave sur 1 mesure, piste Sub, S). Vérifier : Drop − Montée ≥ +2,
   Drop − Break ≥ +4.
3. **Une couche médium + un sinus** ; référence : sub + médium rééchantillonné + couches qui se relaient. Sans
   rééchantillonnage, le plus proche : deux pistes médium (« Basse wobble » LFO 1/8 et « Basse growl » LFO
   1/16) qui **se partagent les notes** (mesures 1-2 / 3-4, ou notes longues / notes courtes) + WIDENER sur les
   médiums, Sub mono (générateur : `bassLayers = ["Basse wobble", "Basse growl"]`, M). Vérifier : 60-250 Hz ≥ −6,
   côté/centre ≤ −15.
4. **Batterie** : pas de triolet (grille 1/16 uniquement, F12), pas de caisse dub en écho tous les 2 mesures,
   clap absent. Changement (recette, S) : `Line(Role.Clap, "........X.......")` reverb 0,25 ; `Line(Role.Perc2,
   "..............X.", voice = snare2)` avec preset `dub_deep` (la caisse « dub » une fois par 2 mesures, 2e
   mesure) ; charleys à vélocités alternées `x.o.x.o...x.o.xo` ; (moteur, L) `Hit.offset` en ticks pour le
   triolet de kicks `X....T.T..X.....` (T = +8 ticks).
5. **Harmonie** : phrygien `[0,0,0,1]` juste pour le UK dubstep ; pour Scary Monsters-like (Happy/Future) un
   `[5,6,0,6]` (VI – VII – i – VII) en mineur serait plus juste que le dorien `[0,0,3,3]` : changer Future →
   `Minor to listOf(5,6,0,6)`.
6. **Lead** : la cloche « rare et inquiétante » est bien ; ajouter l'arpège « deux touches » de Skream comme
   option Dreamy : `leadPattern "R.5.O.5.R.5.O.5."` oct. 4, Pluck, delay 0,3.

---------------------------------------------------------------------------------------------------------------
## 7. Riddim (`riddim`, 140 BPM)

### 10 titres repères

| # | Artiste – Titre (année) | BPM | Tonalité | Source |
|---|---|---|---|---|
| 1 | Coki – Spongebob (2007) | ≈ 140 | n/d | RA, UKF (précurseur « tear-out ») |
| 2 | Bommer & Crowell – Yasuo (2014) | 140 (songbpm lit 210 = triolets) | ≈ Fa mineur | songbpm ; « quintessential » (Wikipedia) |
| 3 | Phiso – Jotaro (2015) | 140 | n/d | songbpm |
| 4 | Infekt – Orgalorg (2016) | 140 | ≈ La mineur | songbpm ; « quintessential » (Wikipedia) |
| 5 | Svdden Death – Behemoth (2018) | 140 | n/d | songbpm |
| 6 | Subtronics – Scream Saver (2019) | 150 (75 ×2) | ≈ Sol#/La♭ | songbpm |
| 7 | GRiZ & Subtronics – Griztronics (2019) | 150 (75 ×2) | ≈ Si mineur | songbpm |
| 8 | Marshmello & Svdden Death – Sell Out (2019) | 145 | ≈ Mi♭ mineur | songbpm ; 3:12, « no overarching melody » (Wikipedia) |
| 9 | Infekt – Raptor (≈ 2019) | 140 | ≈ Si♭ mineur | songbpm |
| 10 | Papa Khan – Rain (2020, colour bass) | ≈ 140-150 | n/d | Wikipedia (colour bass le plus écouté) |

### Traits du corpus
- **Tempo** 140-150 (Wikipedia) : les plus récents à 145-150.
- **Harmonie** : une note, Mi♭ / Ré# mineur fréquent (EDMProd : sub « single note D# ») ; nappe sombre en
  intro seulement ; « no overarching melody » (Sell Out).
- **Batterie** (EDMProd) : kick 1, clap + caisse sur le 3 avec **un kick plus doux caché sous le clap (le
  « whack »)**, charleys en contretemps + un charley sous chaque kick à −9 dB, percussions en triolets ;
  **claps écrêtés** (transitoire coupée), « clipping at each stage ».
- **Basse** : **un seul motif court répété tout le drop sur une seule note**, « LFO square waves », « wide
  delays », flanger et chorus (Wikipedia, Jayce Ullah-Blocks) ; EDMProd : sample bouclé, transposé −1 octave,
  Roar en mode « Noise » + feedback, OTT, auto-filtre enveloppe ; **chaque coup de basse fondu en entrée et en
  sortie pour laisser la place au kick et au clap** ; le **sub suit le même rythme (LFO 1/4 sur le niveau)** ; une
  variation = un coup retiré/remplacé, un 2e preset qui apparaît quelques mesures plus tard ; Infekt : « more
  space, atmosphere, super dark textures ».
- **Structure** (EDMProd, 82 mesures) : anacrouse 2, intro 8, montée synthés 8, montée batterie 8, **faux drop
  4**, drop 1A 8, 1B 8, montée batterie 8, faux drop 4, drop 2A 8, 2B 8, outro 8 ; reverse sweeps, impacts,
  chœur transposé qui monte, **batterie retirée juste avant le drop (trou de silence)**, bitcrush décroissant sur
  le lead.

### Notre recette aujourd'hui
Kick `X.......o.......`, caisse + clap sur le 3, charley `..x...x...x...x.`, perc tous les 3 pas ; swing 0 ;
`bassPattern "R-------....O-.."` « Basse wobble » (LFO2 triangle 1/8), **pas de `subBass`** ; mineur `[0,0,0,0]` ;
`parts = {Bass}` ; `breakWith = charley + perc + basse`, sweep basse 0,15 → 0,5 au Break ; fx : fat_bass +
FLANGER basse, reverb clap, OTT master 0,3.

### Écarts
1. **Le wob est triangulaire** : le riddim est un **LFO carré** (gate on/off) ; notre « Basse wobble » (tri 1/8)
   fait un wobble dubstep mou. Changement (moteur, S, même point que dubstep 1) : `LFO2_SHAPE` carré ; preset
   « Basse riddim » (F10) = « Basse wobble » + carré + `LFO2_SYNC` 1/8 triolet + résonance 0,55 + OSC2 carré ;
   recette `bass = "Basse riddim"`, FLANGER gardé + ECHO large (« wide delays ») en envoi 0,2. Vérifier :
   enveloppe de la basse en créneaux (temps de montée < 5 ms au banc).
2. **Pas de sub** : la seule basse (SUB 0,7 du synthé) porte tout, à −9,2 LUFS plafonné et avec un trou 60-250
   de −8,4. Référence : sub sur une note, **dont le niveau suit le même LFO 1/4**. Changement : `subBass = true`
   + (générateur) le Sub reçoit un WOBBLE « Carrée » 1/4 sur son niveau (insert TREMOLO/WOBBLE amplitude : le
   WOBBLE actuel est un filtre ; un passe-bas carré à 30 Hz fait le même effet sur un sinus). Vérifier : sub < 60
   Hz ≈ −2 dB rel., modulé.
3. **Le motif est trop court et jamais varié** : « R-------....O-.. » = 1 mesure ×4 ×8. Référence : un motif court,
   oui, mais avec **fondus autour du kick et du clap**, un coup retiré/remplacé à la mesure 4, un 2e preset au
   bloc suivant. Changement : `bassPattern` 32 pas `R--.R--.....O-..R--.R--.3-..O-..` (3 coups « triolet », trou
   sur le clap, réponse) + `bassVariation` (mesure 4 : `R--.R--.....O-5-`) + `bassSwitch = "Basse growl"` au Drop
   2 ; (générateur) release 20 ms et attaque 5 ms imposés au synthé de basse en riddim (fondus).
4. **Faux drop et silence** : notre Montée (roulement) → Drop direct. Référence : faux drop de 4 mesures, trou de
   silence. Changement (générateur, M) : `fakeDrop = 4` : après la Montée, 4 mesures de Drop (basse + kick + clap)
   puis 4 mesures de Montée courte, puis le vrai Drop ; 1 temps de silence avant chaque Drop. Vérifier : courbe de
   sonie du banc avec deux pics.
5. **Claps / whack** : le kick doux sous le clap est là ; manquent le clap écrêté (DISTORTION 0,3 « clip » sur la
   lane Clap), le charley −9 dB sous chaque kick (`Line(Role.Perc2, "o.......o.......", voice = hat)`), et le
   « 150 BPM » moderne (recette : 140 gardé, `references` OK ; un 2e style « riddim 150 » n'est pas utile).
6. **Références** : Yasuo et Orgalorg confirmés comme « quintessentiels » (Wikipedia) ; Sell Out est à 145 ;
   Griztronics à 150. OK.

---------------------------------------------------------------------------------------------------------------
## Synthèse famille : les 10 écarts les plus audibles, dans l'ordre

| # | Écart (mesuré ou sourcé) | Changement | Vérification |
|---|---|---|---|
| 1 | **Le Drop ne tombe pas** : Drop − Montée = −2 à −3 LU dans 6 styles sur 7 (cible ≥ +2), Drop − Break ≈ 0 (cible ≥ +4). Références : drop à la mesure 49 = le plus fort ; silence puis retour avec le sub. | Générateur : courbe d'énergie par forme Build (Intro −6, Montée −6 → −3, Drop 0, Break −6 LU) : Montée avec vélocités plafonnées 0,7, souffle −6 dB, nappe −4 dB, passe-haut 60 → 200 Hz sur le bus musique ; **un temps de silence total** avant chaque Drop ; crash + sub drop au 1 ; Drop : basse +1,5 dB, couches `drop` (ride, perc2, clap). | Banc : Drop − Montée ≥ +2 LU, Drop − Break ≥ +4 LU, pour les 7 styles. |
| 2 | **La basse (= la mélodie du genre) est une boucle de 1-2 mesures répétée 8 fois par Drop**, sans variation ni switch. Références : phrase + variation sur les 8 mesures suivantes, relance toutes les 8, 2 temps coupés en fin de 16, autre patch au Drop 2. | Recette + générateur : `bassPattern` jusqu'à 64 pas ; champs `bassVariation` (mesures 5-8) et `bassSwitch` (preset ou pack du Drop 2, piste muette dont le fader s'ouvre, comme `dropTwoLane`) ; règle famille : 2 derniers temps de chaque bloc de 16 sans basse. | Test modèle : ≥ 2 phrases de basse distinctes par Drop, aucun bloc de 8 mesures identique au précédent sur toutes les pistes. |
| 3 | **Un seul timbre, mono, LFO triangle fixe** : 60-250 Hz creux de −8 à −11 dB (jump_up, dubstep, riddim, neuro), côté/centre −18 à −32 dB. Références : 2 à 7 couches (sub mono + médiums larges), LFO carré / dent de scie / sinus, vitesse par note. | Moteur (S) : `LFO2_SHAPE` ; preset « Basse riddim » ; passe-haut de la basse sur sub à 80 Hz ; (M) 2e couche médium (`bassLayers`) une octave au-dessus, `fat_bass` + WIDENER, passe-haut 250 Hz ; `reese_dnb` appliqué en dnb ; filtre formant (insert) pour neuro. | Banc : 60-250 Hz ≥ −6 dB rel., côté/centre ≤ −15 dB, corrélation < 120 Hz ≥ 0,95. |
| 4 | **Aucune automation du synthé** : pas de wobble qui change de vitesse, pas de growl qui évolue (F14). | Automation kind 6 (paramètre d'instrument) appliquée sans verrou dans le fil audio ; recettes : `LFO2_SYNC` et `CUTOFF` par bloc de 8 (dubstep 1/8 → 1/4 → 1/16, neuro coupure 0,3 → 0,6). | Banc : enveloppe RMS de la piste basse change de période ≥ 2 fois par 16 mesures ; host-test sans allocation. |
| 5 | **Batterie figée** : 1 boucle de 2 mesures + le même fill tous les 4 ; vélocités en 3 valeurs ; aucune note hors grille ; caisse claire à une couche. Références : ghosts 15-40 % variés toutes les 4, kick fantôme fin de 8, roulement descendant fin de 8, crash + delay au 1 de chaque 16, charleys 70-90 % randomisés, ghosts +10-20 ms, caisse 3 couches + clap. | Générateur (étape 6 du plan son) : fills 4 / 8 / 16 hiérarchisés, crash au 1 de chaque 16, variantes A/B charley et perc toutes les 8, humanize à graine fixe (±8 % charleys/percs, +5 ms ghosts), clap couche sur la caisse en dnb/jump_up/neuro (−6 dB). | ≥ 6 mesures de batterie distinctes par morceau ; écart-type des vélocités de charley ≥ 0,05 ; jamais deux blocs de 8 identiques. |
| 6 | **Montée unique « roulement de caisse claire »** pour 6 styles ; références : liquid = batterie filtrée + kick build, dubstep = kick par temps doublé + souffle, riddim = faux drop de 4, jump up = roulement. | Recette `buildStyle` ∈ {roll, kick, filter} + `fakeDrop = 4` (riddim) ; reverse crash dans la dernière mesure ; voix/shout avant le drop quand un pack en a. | A/B par style ; banc : montée ≥ +3 LU sur ses 4 dernières mesures, puis silence d'un temps (−20 dB). |
| 7 | **Sonie plafonnée à −8 / −9 LUFS** (chaîne retire 6 LU) contre −4 à −6 dans le genre ; la basse des hits écrête (carré). | Écrêteur doux sur le bus basse avant l'OTT ; kick accordé 55-65 Hz, court, passe-haut 40 Hz (85 Hz en dnb pour laisser la basse) ; cible famille −6 ±1 en attendant la batterie v3. | Banc : LUFS-I à ±1 de la cible, PLR ≥ 6, TP ≤ −1. |
| 8 | **Lead « synthé-jouet »** (motif d'une mesure répété, 0,8 fixe ; Voix robot en neuro) ; dans ce genre le lead est une 2e basse ou un riff de 4-8 mesures avec respirations. | Recettes : `leadPattern` 64 pas, question/réponse sur 2+2 mesures, notes longues, `leadGlide` ; neuro et jump up : lead = « Basse growl » / pack bass ; liquid : `counter = "Cloche"` 8 mesures puis octave. | A/B ; test modèle : ≥ 25 % de silences dans la phrase de lead, ≥ 2 longueurs de notes. |
| 9 | **Jungle sans break** : one-shots sur 2 mesures au lieu d'un break de 4 mesures joué (mesures 3-4 différentes, micro-timing). | F13 (pack de breaks CC0 + Slicer + `source: "breaks"`) ; en attendant, grille de 64 pas avec les mesures 3-4 de l'Amen et micro-timing à graine fixe. | 4 mesures distinctes dans la boucle ; écoute A/B. |
| 10 | **Transitions pauvres** : souffle + impact seulement. Références : crash avec delay 2 temps, reverse crash, trou de silence, shouts de MC, rewind (TAPE_STOP), caisse dub en écho une fois par 2 mesures, sub drop. | Pack « FX de transition » (CC0) : crash, reverse crash, impact, sub drop ; lignes Perc2 pour la caisse dub et les shouts ; TAPE_STOP sur le bus batterie en fin de Break (jungle, dubstep). | Présence d'un événement de transition à chaque frontière de section (test modèle) ; A/B. |

Ordre de mise en œuvre conseillé : 1 (S, générateur), 2 + 8 (recettes + 2 champs), 3 (LFO carré S, couches M),
5 + 6 + 10 (étape 6 du plan son), 7 (chaîne), 4 (F14, L), 9 (F13, L). Les points 1, 2, 3 et 5 suffisent pour que
Benjamin entende la différence sur 30 s de Drop en dnb, jump up et dubstep.

Corrections de références : neurofunk « Noisia – Machine Gun » (128 BPM, breaks) → « Noisia – Stigma (2010) ».

---------------------------------------------------------------------------------------------------------------
## Sources

Fiches et tutoriels :
- Attack Magazine, Beat Dissected « Raw Drum & Bass » et « Incessant Drum & Bass Beat » (168-178 BPM, swing
  50-60 %, kick pas 1 et 11, caisse 3 couches, ride sidechain, breaks « bracketing EQ », roulement fin de 8) ;
  « Mystik Dubstep » (140, swing 50 %, caisse 160-200 Hz + clap sur le 3, charleys muets sur le 3, caisse dub
  en écho 1×/2 mesures) ; « Reese Bass Redux » (±0,27 demi-ton, mono ; PWM 3,2 Hz ; Saunderson 1988, Terrorist
  1994). https://www.attackmagazine.com/technique/beat-dissected/raw-drum-bass/ ,
  https://www.attackmagazine.com/technique/beat-dissected/incessant-drum-bass-beat/ ,
  https://www.attackmagazine.com/technique/beat-dissected/mystik-dubstep/ ,
  https://www.attackmagazine.com/technique/tutorials/reese-bass-redux/
- Native Instruments, « How to make drum and bass » (174 ; breakdown 33, drop 49, lead coupé 97, outro 113 ;
  Butterherz HP 250 Hz + sub Touchdown). https://www.native-instruments.com/blogs/blog/drum-and-bass
- EDMProd : « How to make drum and bass » (174-175 ; Mi/Fa/Fa# mineur ; sub D#1-G#1 ; reese + sinus ;
  structure 32/16-32/…), « Liquid DnB » (projet complet, ghosts 2.4 et 3.2, drop 1A/1B/1C, niveaux), « Dubstep »
  (Mi mineur, 8/8/8/8/8, LFO 1/8 pointé, FM, médium rééchantillonné, macros), « Jungle » (174, tourne à 8,
  contrebasse −6 demi-tons, OTT 35 %), « Riddim » (82 mesures, faux drops, Roar, sub D# LFO 1/4, fondus),
  « 6 drum tips » (ghost kicks +1 oct. −50 %, snare 150-200 Hz). https://www.edmprod.com/how-to-make-drum-and-bass/ ,
  https://www.edmprod.com/how-to-make-liquid-drum-and-bass/ , https://www.edmprod.com/how-to-make-dubstep/ ,
  https://www.edmprod.com/how-to-make-jungle-music/ , https://www.edmprod.com/how-to-make-riddim/ ,
  https://www.edmprod.com/6-drum-tips/
- Mode Audio, « Dubstep: 5 production essentials » (triolet de kicks, comp −12 dB 3:1, parallèle 10:1, sub sinus
  < 100 Hz, LFO 1/4-1/8-1/16, couches par suppression de notes). https://modeaudio.com/magazine/dubstep-5-production-essentials
- Violet Recording, « How to make a wobble bass » (formes de LFO, vitesse par note, sub mono < 100-120 Hz,
  « old within a few seconds »). https://violetrecording.com/how-to-make-a-wobble-bass/
- Loopmasters, « How to get the perfect jump up bass » (175, mid + sub −12, 1 voix, switch-ups) et « How to make
  jungle breaks » (170, deux breaks, +20 cents, HP 160 Hz). https://www.loopmasters.com/articles/4480-How-To-Get-the-Perfect-Jump-Up-Bass-A-D-B-Bass-Tutorial ,
  https://www.loopmasters.com/articles/4377-How-To-Make-Jungle-Breaks-A-Drum-Bass-Jungle-Break-Beat-Tutorial
- Unison, « DnB drum patterns » (vélocités ghosts 15-40 %, charleys 70-90 %, nudge 10-20 ms). https://unison.audio/dnb-drum-patterns/
- Magnetic Mag, « Fighting for Freedom: how it was made » (7 couches de basse, Trash 2 par bandes). https://magneticmag.com/2026/02/fighting-for-freedom-how-it-was-made/
- iZotope, « Noisia » (Trash sur presque toutes les basses, vrais samples, overheads distordus). https://www.izotope.com/en/learn/noisia
- BassGorilla, « How to make neurofunk bass » (rééchantillonnage en 3 passes). https://bassgorilla.com/how-to-make-neurofunk-bass/
- Ethan Hein, « Building the Amen break » (mesure 3 caisse retardée, mesure 4 sans kick, charley ouvert). https://ethanhein.substack.com/p/building-the-amen-break
- The Mix Review, « Baddadan » (phrygien dominant, basse carrée qui écrête, kick 60 Hz). https://themixreview.org/reviews/baddadan/
- Resident Advisor, Rewind « Coki – Spongebob » (LFO → hauteur) et « Skream – Midnight Request Line » (FruityLoops,
  arpégiateur, basse « unusually high-pitched »). https://es.ra.co/reviews/36157 , https://ra.co/reviews/23709
- UKF, « Origins: Coki ». https://ukf.com/read/origins-coki/
- Hooktheory : Scary Monsters (Sol mineur, 140, VI-VII-i-VII), Memory Lane (Do# mineur, 175, VI9-v6/5-i7), Feel
  the Love (Si♭ m / Ré♭ M), I Can't Stop (Do mineur, 140). https://www.hooktheory.com/theorytab/view/skrillex/scary-monsters-and-nice-sprites ,
  https://www.hooktheory.com/theorytab/view/netsky/memory-lane , https://www.hooktheory.com/theorytab/view/rudimental/feel-the-love ,
  https://www.hooktheory.com/theorytab/view/flux-pavilion/i-cant-stop
- Wikipedia : Drum and bass, Dubstep, Riddim (genre), Reese bass, Wobble bass, Amen break, Original Nuttah,
  Tarantula (Pendulum song), Inner City Life, Valley of the Shadows, Sell Out, Gold Dust, Scary Monsters and
  Nice Sprites (EP), Night (Benga and Coki song).
- Sonie : Mastering The Mix (DnB « −4 to −5 LUFS integrated in the drops », club −6 short-term, TP −1) ;
  KVR (dubstep −5,5 à −6,5) ; Soundcamps (bass/dubstep −4 à −7). https://www.masteringthemix.com/blogs/learn/am-i-mastering-loud-enough-how-to-hit-the-perfect-lufs-every-time
- Arrangement : Mixed In Key (Feel the Love 8/24/32), WA Production (16/16/32/16/16/32), forum Ableton.
  https://mixedinkey.com/captain-plugins/wiki/how-to-arrange-a-dance-music-track/ ,
  https://blog.waproduction.com/arrangement-blueprints-10-proven-song-maps-that-just-work
- BPM / tonalités : songbpm.com (pages @artiste/titre), Beatport (listes de résultats), AudioKeychain,
  AcousticBrainz, getsongbpm ; tous automatiques, d'où les ≈.
- Recherche précédente du projet : `scratchpad/research/famille-dnb-dubstep.md` (DnB Academy, Bonedo, eMastered,
  MusicRadar « liquid chords », dnbforum, Beatportal).
