# Hits par style, famille Hip-hop : boom bap, trap, drill, lo-fi, phonk, grime, jersey club

Date : 2026-10-23. Base lue (lecture seule, aucun gradle) : `/home/user/ps-p22` : `StyleRecipes.kt`, `StarterWizard.kt`, `WizardMix.kt`, `StyleMusic.kt`, `Engine.cpp` (roulements), `docs/CORPS.md`, `docs/STYLE-FIXES.md`, `docs/plan/son.md`, `docs/son/banc/apres-etape2.md`, `docs/RETOURS.md`.
Phrase de Benjamin : « ça ressemble encore trop à de l'amateur, surtout les mélodies synthé ».

## Méthode et limites

- 10 morceaux repères par style (classiques et récents, FR et US/UK), puis les traits communs, puis l'écart avec notre recette et notre générateur, avec un changement précis et mesurable par écart.
- Sources : fiches BPM/tonalité (SongBPM = données Spotify, Hooktheory TheoryTab = transcriptions, Beatport, Mixed In Key, journaux promo radio Mixmeister), analyses de production (Attack Magazine « Beat Dissected », Native Instruments blog, EDMProd, Splice, RouteNote, Beatstorapon, Violet Recording, RBMA Daily, The Fader, Complex, Tidal, DJ Mag), Wikipedia (sections composition). Liste numérotée en fin de document ; les renvois sont entre crochets.
- Je n'écoute pas l'audio : tout vient de l'écrit. **≈** = estimation (à vérifier à l'oreille ou dans Tunebat) ; **n.t.** = non trouvé dans les sources atteintes. Le quota de recherche web a été atteint à mi-parcours : une dizaine de BPM restent n.t., les traits de style, eux, sont tous sourcés.
- Les BPM des bases Spotify sont souvent lus « double » ou « moitié » : je donne la valeur de la base et le ressenti (demi-temps) quand les deux coexistent.
- Mesures de nos morceaux : `docs/son/banc/apres-etape2.md` (37 morceaux, kit intégré, sans packs).

## 0. Ce que fait le générateur aujourd'hui (faits de code, communs aux 7 styles)

Référence pour les sections suivantes, pour ne pas le répéter sept fois.

- **Vélocités de batterie** : trois valeurs fixes, `X` = 1,0, `x` = 0,6, `o` = 0,35 (`StarterWizard.hits`, l. 485). Aucune humanisation, aucun micro-décalage (noté aussi dans `docs/plan/son.md` cause 7).
- **Roulements** : un chiffre `2`, `3`, `4` = autant de coups **dans un seul pas de double-croche**, à 0,6, chaque coup suivant ×0,85 (`Engine.cpp` l. 680-685) : `2` = triples croches, `3` = sextolet, `4` = quadruples croches. Donc : **pas de triolet de croche ni de double-croche à cheval sur plusieurs pas**, pas de roulement qui **monte** en volume, pas de roulement plus long qu'un pas.
- **Lignes de batterie** : 16 ou 32 pas (une ou deux mesures). Le beat d'un morceau avec mélodie est recopié sur 4 mesures (`WizardMix.phrase`, 64 pas) avec **le même fill à chaque fin de 4 mesures** : en forme « chanson » (tout le hip-hop) ce sont les charleys qui roulent (0,5 / 0,5 / 0,6×2 / 0,7×3), le kick se tait sur le dernier temps, un charley ouvert sur la dernière croche. Jamais de fill de caisse claire ni de crash en forme chanson.
- **Forme chanson** (`sectionsOf`, `SongForm.Song`) : Intro 8 (ou 4) – Couplet 16 – Refrain 16 – Couplet 16 (ou 8) – Refrain 16 – Intro 8 (ou 4) en outro, plafonné à 210 s. À 140 BPM : 80 mesures = 137 s ; à 90 : 72 mesures = 192 s ; à 80 : 64 mesures = 192 s. Le « Souffle » en forme chanson n'est qu'un **impact** sur le premier temps du Refrain, sans montée (`riserLane`).
- **Plan des sections** (`layeredPlan`) : Intro = la partie signature (si ce n'est pas la basse) + charleys, sinon kick + charleys + nappe ; Couplet = tout sauf la couche du refrain (le lead, ou la nappe sans lead) et sauf charley ouvert / percus ; Refrain = tout. **Pas de basse dans l'intro, jamais de coupure de basse ni de kick avant un refrain, aucune variation toutes les 8 mesures.**
- **Mélodie (lead)** : `leadPattern` = **un motif de 16 pas rejoué à chaque mesure**, transposé diatoniquement sur l'accord de la mesure (`withLine` : `R` = fondamentale de l'accord courant, `3`, `5`, `O`…). Vélocité 0,8 (minuscule 0,56), chaque note dure un pas sauf tenue par `-`. Conséquence : **aucune phrase de 2 ou 4 mesures, aucun silence d'une mesure, aucune question-réponse, et le « crochet » change de hauteur à chaque accord** (sur le VI il monte d'une sixte). Le modèle accepte pourtant des motifs de 32 ou 64 pas (`StepLine.barOf` alterne les mesures) : c'est le convertisseur de recettes qui les limite (`docs/STYLE-FIXES.md` F4).
- **Accords** : `chordRhythm` 16 pas rejoué chaque mesure, voicing Triad / Seventh / Ninth, vélocité 0,75. **Contre-chant** (`Melody.counter`) et **sous-basse** (`subBass`) existent dans le modèle mais **aucune recette hip-hop ne les utilise**.
- **Basse / 808** : `bassPattern` 16 ou 32 pas ; `/` = glissando (notes qui se chevauchent + portamento du synthé `BASS_GLIDE` 0,4 = 80 ms). Le son « Basse sub » est un synthé sinus à l'octave 1 ; le pack `trap:808` / `drill:808` / `phonk:808` est un one-shot **généré par IA** de 2 s (« 808 bass one shot, long sub, note C »), sans glissando quand la ligne n'est pas monophonique. **Il n'existe pas de voix 808 dédiée** (clic d'attaque, enveloppe de hauteur, saturation, longue tenue accordée) ; `docs/plan/son.md` étape 7 la prévoit.
- **Mix de départ** (`WizardMix`) : basse +2,5 dB, lead −1, nappe tenue −4 ; kick −12 LUFS, caisse/clap −16, charleys −27, percus −22 ; charleys pannés ±0,2 ; bus batterie compresseur léger + saturation 0,15 ; master compresseur + saturation 0,2 ; cible de sonie par genre (`LoudnessTarget.AUTO`).
- **Banc (kit intégré)** : trap −9,6 LUFS-I (plafonné : la chaîne retire déjà 6,3 LU), drill −9,7 (plafonné), phonk −8,5, grime −8,6, jersey −9,0, boom bap −10,0, lo-fi −12,0 ; Refrain − Couplet ≈ 0 LU partout ; côté/centre −23 (trap) à −38 (phonk) dB contre −15 à −9 visés ; bas-médiums 250-500 Hz −14 à −20 dB.

---

## 1. Boom bap (id `boom_bap`, 90 BPM)

### 1.1 Dix morceaux repères

| # | Artiste – Titre (année) | BPM | Tonalité / accords | Notes de production | Src |
|---|---|---|---|---|---|
| 1 | Pete Rock & CL Smooth – They Reminisce Over You (T.R.O.Y.) (1992) | n.t. (≈ 90-95) | boucle de sax + ligne de basse du sample Tom Scott « Today » | Pete Rock part de la basse du disque, le sax devient le crochet | [13] |
| 2 | Wu-Tang Clan – C.R.E.A.M. (1993) | n.t. (≈ 90) | piano + batterie des Charmels « As Long As I've Got You », bouclés **en continu du début à la fin** | RZA ; crochet chanté par Method Man | [11] |
| 3 | A Tribe Called Quest – Electric Relaxation (1993) | 98 | Sol♯ mineur ; VI – v7sus4 – ♯vi – IV(add9) – III(add9) (sample Ronnie Foster) ; mélodie 1,14 temps/note, ambitus 20 demi-tons | boucle jazz modale de 2 mesures | [9] |
| 4 | Nas – N.Y. State of Mind (1994) | 94 | n.t. ; boucle de piano (Joe Chambers « Mind Rain », attribution non confirmée) qui « ne résout pas » | DJ Premier : piano + crochet scratché (Rakim) | [8][16] |
| 5 | Gang Starr – Mass Appeal (1994) | n.t. (≈ 90-95) | sample Vic Juris « Horizon Drive » ; Premier : « I made the background melody real simplistic » | crochet scratché, 3 couplets | [12][16] |
| 6 | The Notorious B.I.G. – Juicy (1994) | 96 | Mi majeur (Hooktheory) / La majeur (SongBPM) ; IV7 – V7 – I – IV – I (Mtume « Juicy Fruit ») | boucle de synthé-basse et de clavier, refrain chanté | [10] |
| 7 | Mobb Deep – Shook Ones, Pt. II (1995) | 96-97 | Si♭ mineur ; i – VI – v ; boucle de **8 temps** (2 mesures), 0,89 temps/note | Havoc : piano Herbie Hancock « Jessica » (ralenti), cordes Quincy Jones, batterie Daly-Wilson ; **MPC60** + EPS-16 ; le « stove » est un charley | [5][6][7] |
| 8 | IAM – Petit frère (1997) | 88 (SongBPM 176) | Sol♯ mineur | boom bap marseillais, boucle sombre + refrain chanté | [14] |
| 9 | Nekfeu – On verra (2015) | 75 (SongBPM 150) | Do majeur | boom bap FR récent, plus lent, piano + basse | [15] |
| 10 | Joey Bada$$ – Paper Trail$ (2015, prod. DJ Premier) | n.t. (≈ 90) | boucle de piano + crochet scratché | retour du son Premier en 2015 | [16] |

### 1.2 Traits communs (sourcés)

- **Tempo** : 88-98 BPM pour le cœur du genre (T.R.O.Y., Juicy, Shook Ones, N.Y. State of Mind, Electric Relaxation), 75 pour la variante FR posée (Nekfeu). RouteNote conseille 80-105 et « 94 idéal » [3]. Attack (hip-hop poussiéreux) : 90-110 [4].
- **Swing** : 57-64 % sur les croches ou les doubles (RouteNote : 64 % = « extrême, au-delà ça devient une valse »), Premier subtil, Dilla lourd et non quantisé [3]. Notre `swing = 0,2` d'une double-croche ≈ MPC 60 % : juste.
- **Batterie** : kick 1 et 3 + **kick fantôme juste avant** un kick ou une caisse (« ba-boom »), quantisé à la double et tiré par le swing ; caisse 2 et 4, sèche, qui claque ; charleys à la croche ; samples de breaks 60s-70s, pas de sons 808 ; SP-1200 12 bits (bit-crusher anti-repliement), MPC60 ; compression parallèle ; réverbe surtout sur la caisse, un peu sur le charley, **aucune sur le kick** [1][2][3][4]. Attack : premier kick de la mesure pile sur la grille à vélocité pleine, les autres varient en vélocité et en timing ; caisse + clap empilés ; deux charleys ouverts « slurp » étouffés par le fermé ; petite conga/tom ; couche de craquement vinyle ; bus batterie compresseur + bande [4].
- **Harmonie / « sample »** : une boucle de **2 mesures** (Shook Ones : 8 temps ; Electric Relaxation : 4 accords riches ; Juicy : 5 accords de 7e) tirée d'un disque de jazz/soul, qui tourne **du début à la fin** (C.R.E.A.M.) ; accords mineurs i – VI – v ou modaux, ou majeurs avec 7es ; souvent une boucle qui « ne résout pas » (N.Y. State of Mind) [5][9][10][11].
- **Mélodie / crochet** : le crochet est la boucle elle-même (piano, sax) plus un **crochet scratché** de voix (Premier) ou chanté ; mélodie simple et peu dense (Premier : « real simplistic »), 1 note par temps environ (Electric Relaxation 1,14, Shook Ones 0,89) [9][5][12][16].
- **Basse** : la basse du disque samplé (T.R.O.Y.) ou une basse courte et ronde qui suit le kick, jamais une 808 longue [13][1].
- **Structure** : intro 4-8 mesures (boucle seule, scratch), couplets 16, refrain 8 (scratché ou chanté), ×3, 4-5 min (Juicy 5:02, T.R.O.Y. 4:45, Shook Ones 3:41) ; peu de variation d'arrangement, l'énergie vient du rappeur [11][13].
- **Sonie** : disques des années 90, masters dynamiques ; cible du projet −10 ±1,5 LUFS-I, PLR 9-11 (`docs/plan/son.md` § 2.4) : cohérente.

### 1.3 Notre recette (extraits)

```
Kick    "X.....X...X.....X.x.......X..x.."   Snare "....X.......X.......X......oX..o"
Hat     "x.x.x.xox.x.x.xo"                   OpenHat "..............................x."
Perc2   "..o...o...o...o." (shaker, refrain)  swing 0.2, kit snare sp1200, hat mpc60
Melody  root 2 (Ré), Dark Minor [0,0,5,5] ; chordRhythm "X------.X---.X-." Seventh oct 3, pad "Piano FM"
        bassPattern "R-----R-..R---.." oct 2, pack boom_bap:bass ; leadPattern "O.......5.....3." Marimba oct 5 (lead off)
Fx      pad warm_vinyl + reverb 0.15 ; signature = Pad
```

### 1.4 Écarts et changements, par impact audible

1. **La « boucle samplée » n'en est pas une.** Les accords jouent le **même rythme à chaque mesure** sur un Piano FM, en triades + 7e, sur i – i – VI – VI, sans ligne mélodique dedans ; les hits bouclent **2 mesures** avec une mélodie interne (piano/sax), des 7es-9es, et un son filtré/poussiéreux. Changement (recette) : `chordRhythm` sur 32 pas `"X------.X---.X-.|X--.X---..X---.."`, progression Dark `[0,5,4,4]` (i – VI – v – v, Shook Ones) et Dreamy `[3,2,5,1]`, voicing Ninth, `chordOctave` 3, son « Piano électrique » (ou `gm:Acoustic Grand Piano`) avec `preset "warm_vinyl"` **puis** insert FILTER passe-bas ≈ 0,55 et SATURATION 0,2 ; et un **lead coché par défaut** qui joue la mélodie de la boucle (voir 2). Mesure : 250-500 Hz ≥ −12 dB relatifs (aujourd'hui −13,1) ; la section « Couplet » doit contenir 2 mesures d'accords distinctes (test : onsets des mesures 1 et 2 différents).
2. **Le lead est un motif d'une mesure rejoué** (`"O.......5.....3."`, 3 notes par mesure, transposé par accord) : les hits ont une phrase de 2 mesures suivie d'un vide ou d'un scratch. Changement : `leadPattern` 64 pas, phrase sur les mesures 1-2, silence mesure 3, réponse courte mesure 4 : `"O.......5...3...|R-----..........|................|....5...3...R---"`, son « Piano électrique » (vibraphone GM si dispo), vélocité 0,7, `StyleFx("lead", reverb 0.2, delay 0.15)`, **lead on par défaut** (`defaults.lead = true`, déjà proposé dans CORPS). Et dans `startingNotes`, option « le lead reste dans la tonalité, pas transposé par l'accord » (passer `degrees = List(4){0}` comme le fait `acidLine`) : champ `Melody.leadOnKey = true` pour ce style. Mesure : ≥ 1 silence ≥ 2 temps par 4 mesures ; la hauteur de la 1re note du lead identique aux mesures 1 et 3.
3. **Batterie trop propre, pas de « ba-boom ».** Il manque le kick fantôme juste avant le 1 de la mesure 2 et avant les caisses, la saturation 12 bits, la réverbe de caisse, la compression parallèle. Changement : kick `"X.....X...X....oX.x.......X..xo."`, snare `"....X......o.X.......X.....oX..o"` ; `StyleFx("drums", preset "punch")` + `StyleFx("snare", reverb 0.12)` ; dans `WizardMix`, humanisation par graine fixe (plan étape 6) : charleys ±8 % de vélocité, ±4 ms, caisse +3 ms (laid-back). Mesure : écart-type des vélocités de charley ≥ 0,05 ; crête du bus batterie après punch ≤ 9 dB.
4. **Refrain = Couplet + un charley ouvert** (CORPS). Les hits font 16 de couplet + 8 de refrain avec un crochet (scratch/chant) et la même boucle. Changement : forme chanson « rap » dans `sectionsOf` : Intro 4 (boucle seule) – Couplet 16 – Refrain **8** – Couplet 16 – Refrain 8 – Pont 8 (boucle filtrée, kick coupé) – Refrain 8 – Outro 4 ; Refrain = + lead (crochet) + shaker + charley ouvert, et **1 mesure de batterie coupée** (kick + caisse muets, charley seul) avant chaque refrain à la place du fill de charleys ; fill de caisse claire 1/16 → 1/32 crescendo toutes les 16 mesures seulement. Mesure : Refrain − Couplet ≥ +2 LU ; aucun bloc de 8 mesures identique sur toutes les pistes.
5. **Stéréo et air** : côté/centre −31 dB, air −25. Changement : `StyleFx("pad", insert "CHORUS")` léger (0,2) avant warm_vinyl, charleys pannés ±0,3, couche de craquement vinyle continue (insert VINYL sur le bus batterie à 0,15 ou piste « Pluie »/bruit à −30 dB). Mesure : côté/centre ≥ −22 dB, air ≥ −23.
6. **Tempo** : 90 est dans la plage ; proposer 92-94 par défaut (Nas 94, Biggie 96, Mobb Deep 96) et garder 75-80 pour une humeur « posée ». Sans mesure.

---

## 2. Trap (id `trap`, 140 BPM)

### 2.1 Dix morceaux repères

| # | Artiste – Titre (année) | BPM (ressenti) | Tonalité / accords | Notes de production | Src |
|---|---|---|---|---|---|
| 1 | Waka Flocka Flame – Hard in da Paint (2010, Lex Luger) | n.t. (≈ 135-150) | riff de synthé « preset de clavier », cuivres/cordes synthétiques | enregistré 2009, Luger 16 ans ; « drill 'n' bass 808 trills, claps, dense synthesizers » | [25] |
| 2 | Kaaris – Zoo (2013, Therapy) | 126 (SongBPM) | Do♯/Ré♭ | trap FR fondatrice, 808 lourde, nappe sombre | [24] |
| 3 | PNL – Le monde ou rien (2015) | 103 (SongBPM) | Fa♯ mineur | cloud/trap FR, piano + 808, lente | [24] |
| 4 | Booba – DKR (2016) | 102 (SongBPM) | Mi♭ mineur | trap FR lente, mélodie de synthé qui tourne | [24] |
| 5 | Migos – Bad and Boujee (2016, Metro Boomin) | 127 (demi-temps ≈ 64) | Mi♭ mineur ; « boom-heavy, moody-keyboard creeper » | structure « stop-and-go » : le beat s'arrête pour laisser le crochet ; 5:34 | [19] |
| 6 | Future – Mask Off (2017, Metro Boomin) | 150 (ressenti 75) | Ré mineur ; i – iv – v ; mélodie de flûte La3-La4 (12 ½ tons), **1,00 temps/note** | boucle de flûte samplée (Tommy Butler « Prison Song », 1976) sur une trap « sparse » ; 3:23 | [18] |
| 7 | Lil Uzi Vert – XO Tour Llif3 (2017, TM88) | 155 (ressenti 77,5) | Si mineur ; **Solmaj7 – Mim – Sim – La** | FL Studio + Gross Beat (volume/temps), beat accéléré, 3:02 | [20] |
| 8 | Cardi B – Bodak Yellow (2017, J. White) | 125 (promo radio) | Mi mineur | « synthy and minimal », beaucoup d'espace pour la voix | [21] |
| 9 | Travis Scott – SICKO MODE (2018) | 155 (Hooktheory : intro 138, partie 2 155) | **trois mouvements** : Si♭ majeur → Do♯ phrygien (II – i en power chords) → Mi♭ mineur ; 0,40 temps/note | 3 beats, 3 producteurs, « beat switches » ; 5:12 | [22] |
| 10 | Travis Scott – FE!N (2023) | 148 (ressenti 74) | Ré♯ mineur ; intro i – v6sus4, refrain **i – VI** ; refrain Fa♯3-Ré♯4 (9 ½ tons), 1,41 temps/note, 81 % notes d'accord | « alarmes » de synthé, chant hypnotique répété, refrain = texture rythmique plus que mélodie | [23] |

### 2.2 Traits communs

- **Tempo** : 125-160 BPM en double temps (ressenti 62-80) ; 140 est le « type » (Wikipedia : 50 (100) à 88 (176), typique 70 (140)) ; la trap FR 2013-2016 est plus lente (102-126) [17][24]. Programmée en demi-temps pour affiner les charleys [17].
- **Tonalité** : mineur naturel, harmonique ou phrygien (Production Music Live : « 3 scales : minor, harmonic minor, phrygian ») ; 2 à 4 accords, i – VI, i – iv – v, i – VI – III – VII, ♭VImaj7 – iv – i – ♭VII (XO) [18][20][23][27].
- **Mélodie / crochet** : **une boucle de 2 ou 4 mesures rejouée à l'identique** pendant tout le morceau (la flûte de Mask Off, les cloches de Bad and Boujee), ambitus étroit (9-12 ½ tons), **≈ 1 note par temps** (Mask Off 1,00 ; FE!N 1,41), notes d'accord, question-réponse A/B (PML : « a question phrase, then a similar answer »), **le crochet ne bouge pas quand l'accord change**, une couche de soutien (nappe, cordes, chœur) et souvent un doublage à l'octave ; sons : cloches, plucks, flûte, piano, cordes, cuivres, orgues « horror » ; réverbe longue + délai [18][23][27][26].
- **808** : une seule note longue par kick, 1 à 3 par mesure, **accordée à la tonalité**, saturée pour sortir sur un téléphone, glissandos à l'octave ou à la quinte, « trigger-finger » ; kick et 808 = un seul instrument, raccourcir la note qui chevaucherait la suivante ; sidechain kick→808 léger ; mono sous 120 Hz [17][26][28].
- **Batterie** : caisse/clap **sur le 3** (demi-temps) ; charleys base croches, **puis triolets, doubles, triples croches en rafales**, vélocités alternées, « que deux mesures ne se ressemblent pas » ; charley ouvert par temps dans le drop ; reverse snare avant le crochet ; chant/ad-lib samplé sur le 4e temps de la 1re mesure de chaque phrase de 4 [17][26].
- **Structure** : intro 4-8 (mélodie seule + tag), crochet 8-16, couplet 16, crochet, couplet, crochet, outro ; **« stop-and-go »** : le beat se coupe sous le crochet (Bad and Boujee), coupure de 808 de 2 mesures avant le crochet, **beat switch** (SICKO MODE) ; 3:00-3:30 aujourd'hui (XO 3:02, Mask Off 3:23) [19][22][20][18][26].
- **Sonie** : masters trap/rap modernes ≈ −7 à −9 LUFS-I ; les titres Spotify 2022 les plus écoutés : −8,4 LUFS-I en moyenne, refrains −6,1 LUFS court terme ; TP ≤ −1 dBTP [50]. Cible projet −8,5 ±1 : juste ; mais **plafonnée à −9,6** au banc.

### 2.3 Notre recette

```
Kick  "X......X..X....."   Clap "........X......."
Hat   "X.x.X.x.X.x.X.x.X.x.X.x.X3x.2222" (build couplet "x.x.x.x.…")   OpenHat "......................X........."
Melody root 5 (Fa), Dark Minor [0,0,5,6] ; chords "Cordes" triad "X-------X-------" oct 3
       bass "Basse sub" oct 1 "R------R-/O----." bassGlide, pack trap:808 ; lead "Cloche" oct 5 "O..5..3.R..3..5." (signature)
Fx    bass crunch ; lead reverb 0.3 + delay 0.2 ; clap reverb 0.15 ; pad wide_alive ; riser (= impact seul)
```

### 2.4 Écarts et changements

1. **La 808 n'est pas une 808.** « Basse sub » = sinus à l'octave 1 + crunch ; pas de clic, pas de chute de hauteur à l'attaque, pas d'harmoniques de saturation (bande 60-250 Hz à −8,1 dB, 250-500 à −19,9), glissando de 80 ms (trop court pour un slide d'octave audible, les hits glissent sur 1/8 à 1/4 de mesure ≈ 100-200 ms), et le one-shot IA du pack ne glisse pas. Changement (moteur, plan étape 7) : **voix 808 dédiée** = sinus + enveloppe de hauteur (−3 ½ tons en 40 ms) + clic court + saturation tanh réglable + glide 60-200 ms selon la longueur du `/` + tenue 1-2 s + accord sur la tonalité, mono sous 120 Hz ; recette : `bassPattern` 32 pas `"R-----------/O--|R-----..R---/5--"` (notes longues, un slide par mesure vers l'octave puis la quinte), `glideMs = 150`, `StyleFx("bass", preset "crunch", insert "PUMP" [0.3, 0.15, 0])` (sidechain court sur le kick). Mesure : 60-250 Hz ≥ −6 dB rel., 250-500 ≥ −14 ; trajectoire de hauteur mesurée sur la piste basse seule : ≥ 1 glissando ≥ 7 ½ tons par 2 mesures d'une durée 100-200 ms.
2. **Charleys : pas de triolets, roulements figés.** Les rafales sont dans un seul pas (1/32, sextolet, 1/64), à 0,6 décroissant, **au même endroit toutes les 2 mesures** (CORPS : « use l'effet »). Changement (moteur, STYLE-FIXES F12) : `Hit.offset` en ticks (triolets de croche = 2 pas sur 3 positions, triolets de double) ou « roulement sur N pas » ; `Hit.ramp` : vélocité du k-ième coup = v × (0,55 + 0,45 k/n) (crescendo) ; recette : ligne de charley **64 pas** (F4) avec 4 fins de mesure différentes : `…X.x.X.x.X3x.x.x.` / `…X.x.X.x.X.x.2222` / `…X.x.X.x.XTx.x.x.` (T = triolet de double sur 2 pas) / `…X.x.X.x.X.x.X444` + charley ouvert `o` sur le « et » du 2 toutes les 4 mesures ; humanisation ±8 %. Mesure : écart-type des vélocités de charley ≥ 0,05 ; au moins 3 mesures de charley distinctes par 4 ; présence d'événements à 1/12 de temps (triolet).
3. **Le crochet de cloche est un motif d'une mesure transposé à chaque accord** (sur le VI il monte d'une sixte : ça « sonne faux » et mécanique) ; 6 notes par mesure en 3-3-2 sans respiration. Les hits : 2-4 mesures, 1 note par temps, même hauteur quand l'accord change, une note tenue, un silence. Changement : `Melody.leadOnKey = true` (le motif reste sur la gamme/tonique, comme `acidLine`), `leadPattern` 32 pas `"O---..5-..3-....|R---..3-..5-..7-"` (notes tenues 4 pas, 1 note par temps environ, dernière note = 7e de tension), vélocités `O` 0,85 / `5` 0,7 / `3` 0,75 ; **contre-chant** `counter = "Nappe"` octave au-dessus sur `counterRhythm "X.......X......."` à −5 dB (déjà supporté, `COUNTER_DB`) ; doublage du lead à l'octave inférieure avec « Pluck » à −8 dB (2e piste lead). Mesure : hauteur de la 1re note du lead identique sur les 4 mesures ; densité 3-5 notes par mesure ; ≥ 1 silence ≥ 1 temps par 2 mesures ; piste lead ≥ −10 dB sous le mix (CORPS : −13,8).
4. **Pas de stop-and-go, Refrain ≈ Couplet (−0,1 LU).** Changement (`scenePlan` forme « rap », voir synthèse G4) : Couplet = lead + kick + clap + charleys en croches + 808 **sans nappe** ; Refrain = + nappe + contre-chant + charley ouvert + roulements ; **coupure de kick + 808 sur les 2 dernières mesures avant chaque Refrain** (reverse/souffle 2 mesures : `riserLane` en forme chanson monte sur 2 mesures au lieu de l'impact seul), crash (voix cymbale raccourcie) sur le 1 du Refrain ; au 2e Refrain, variante « beat switch » : 808 à l'octave + lead à l'octave. Mesure : Refrain − Couplet ≥ +2 LU ; énergie < 120 Hz des 2 mesures avant le Refrain ≤ −20 dB par rapport au Refrain.
5. **Son du lead mono et maigre** : une Cloche seule, côté/centre −23,4 dB. Changement : `StyleFx("lead", preset "pluck_pingpong")` (réverbe + ping-pong) **ou** insert CHORUS 0,25 + reverb 0,35 + delay 0,25 (croche pointée) ; nappe « Cordes » `wide_alive` gardée, à −6 dB. Mesure : côté/centre ≥ −18 dB au Refrain ; corrélation L/R 0,7-0,9.
6. **Sonie plafonnée** (−9,6 pour −8,5 visé, perte 6,3 LU) : conséquence des points 1-2 (808 sans harmoniques, charleys muets au kit : air −23,3). Après 1-2 : re-mesurer ; attendu −8,5 ±0,5, air ≥ −20.
7. **Durée** : 137 s pour des hits à 3:00-3:30. Changement : forme « rap » Intro 8 – Refrain 8 – Couplet 16 – Refrain 8 – Couplet 16 – Pont 8 – Refrain 16 – Outro 8 = 88 mesures = 151 s à 140 (ou Couplets 16 + Refrains 16 : 104 mesures = 178 s, sous 210). Mesure : durée 150-200 s.

---

## 3. Drill (id `drill`, 142 BPM)

### 3.1 Dix morceaux repères

| # | Artiste – Titre (année) | BPM (ressenti) | Tonalité / accords | Notes de production | Src |
|---|---|---|---|---|---|
| 1 | Chief Keef – I Don't Like (2012, Young Chop) | 132 (promo 66) | Fa♯ **mineur harmonique** ; i – iv – V6 ; mélodie Fa♯3-Do♯5, 0,77 temps/note | Chicago drill : cuivres/cloches synthétiques, caisse chargée, crash fréquent | [31][28] |
| 2 | Chief Keef – Love Sosa (2012, Young Chop) | 128 | Si mineur ; i – VI ; **mélodie Ré4-Sol4 = 5 ½ tons**, 0,62 temps/note | chœur/cloches, 808 « trigger-finger » | [31] |
| 3 | 67 ft. Giggs – Let's Lurk (2016) | 135 (SongBPM) | Do (SongBPM) ≈ La mineur | sommet de la 1re phase UK, beat Mazza/GottiOnEm | [35][28] |
| 4 | Unknown T – Homerton B (2018) | n.t. (≈ 140-142) | n.t. | 1er single UK drill classé ; 808 glissante, caisse sur le 4 | [28] |
| 5 | Pop Smoke – Welcome to the Party (2019, 808Melo) | 143 (SongBPM) | n.t. | « haphazard and bass-heavy », 808 **glissante** (808Melo) ; 3:34 | [32] |
| 6 | Pop Smoke – Dior (2019, 808Melo) | 142 (SongBPM) | Sol (Mixed In Key : majeur ; ≈ mineur à l'oreille) | « bass wobble », charleys, **violon samplé inversé et découpé** ; 3:36 | [33] |
| 7 | Freeze Corleone – Freeze Raël (2020) | 137 (SongBPM) | Si mineur | drill FR, nappe froide, 808 | [35] |
| 8 | Tion Wayne & Russ Millions – Body (2021, Gotcha) | n.t. (≈ 140) | n.t. | 1er n°1 UK drill ; « rapid, clattering beats » ; 3:03 | [36] |
| 9 | Central Cee – Doja (2022) | 140 (Beatport 70) | Mi♭ mineur | sample pop (Eve) coupé en demi-temps, 808 glissante ; 1:37 | [34] |
| 10 | Gazo – DIE (2022) | n.t. (les autres titres de Gazo : 138-140, Si♭m / Do♯m) | n.t. | drill FR dominante ; charleys 3-3-2, 808 glissante | [35] |

### 3.2 Traits communs

- **Tempo** : UK/NY 138-151 (Wikipedia), « sweet spot » 140-142, ressenti 70 ; Chicago plus lent, 128-132 (Love Sosa, I Don't Like) [28][30].
- **Caisse claire** : **sur le 3 de la 1re mesure et sur le 4 de la 2e** (Wikipedia : « snares land on the fourth beat rather than the third, every two bars » ; Beatstorapon : « the delayed second hit creates the two-bar pull ») + un rim/contre-caisse plus faible en relance ; **notes fantômes dans la dernière mesure** (Attack) [28][29][30].
- **Charleys** : motif « 3+3+2 » (Attack : « two spaces, two spaces, one space, repeated » ; Wikipedia : polyrythmie 3+3+2), vélocités **variées** (Attack : Vol<Vel 79 %), 2-3 charleys retirés par 2 mesures pour faire des trous, **un roulement court (double ou triolet) avant la caisse principale, un autre différent en fin de mesure 2**, un charley ouvert ou une percu d'accent, shaker syncopé qui bouche les trous, triangle « money shot » sur le 1, swing 10-20 % sur les contretemps + micro-décalages de quelques ms [29][30].
- **Kick** : rare, pas sous chaque 808, aux relances et fins de phrase [30].
- **808** : « plenty of glides » (Attack) ; mono, legato, **un slide court montant vers la fondamentale/quinte/octave près de la fin de la mesure + un long slide descendant sur la boucle de 8 mesures** ; accordée ; sidechain ou kick court cliquant par-dessus ; saturation pour les petites enceintes ; « bass wobble » (Dior) [29][30][33].
- **Mélodie** : mineur (naturel, harmonique, phrygien), **motif de 2-4 notes** (fondamentale, tierce, quinte + 1 tension), boucle de 2-4 mesures, **au moins un trou par mesure, une note changée en mesure 4 ou 8**, registre sous la voix, **mélodies en demi-temps** ; sons : piano, cloches/boîte à musique, cordes, chœurs, nappes filtrées, **samples inversés** (Dior) ; réverbe + délai ; une seule couche de soutien (corde grave, nappe lointaine, contre-pluck) [28][29][30][33].
- **Structure** (Beatstorapon, 80 mesures) : Intro 8 (motif filtré sans batterie) – Hook 8 (tout, 808 la plus forte) – Couplet 16 (une couche en moins, 808/charleys simplifiés) – Hook 8 (+ un petit lift) – Couplet 16 (contre-mélodie, fin de 808 alternative) – Pont 8 (basse coupée, sample filtré) – Hook final 8 – Outro 8 ; **changer quelque chose toutes les 4 ou 8 mesures** (kick muet une demi-mesure, charleys retirés, mélodie raccourcie, queue inversée, impact) ; Violet : « mute the 808 for two bars before a drop » ; hits réels 3:00-3:36 → crochets de 16 ou couplets plus longs [30][32][33][36].
- **Mix** : monter la balance à partir de 808 + kick, puis caisse, puis charleys ; saturation douce sur la 808 ; contre-caisses derrière ; vérifier en mono et au téléphone [30].

### 3.3 Notre recette

```
Kick  "X.........X.......X....X........"   Snare "........X...................X..."   Perc rim "......x.......x.......x....o...."
Hat   "X..x..x.X..x..x.X..x..x.X.33.3x."
Melody root 7 (Sol), Dark HarmonicMinor [0,5,3,4] ; chords "Cordes" triad "X---------------" oct 3
       bass "Basse sub" oct 1 "R-------..R--/O-..R---/5----...." bassGlide, pack drill:808 ; lead "Piano FM" oct 5 "R..3..2.R..5..3." (signature)
Fx    bass crunch ; lead reverb 0.35 ; pad wide_alive + reverb 0.25 ; riser (= impact)
```

La grille de caisse (3 puis 4 sur 2 mesures), le kick clairsemé, le rim de relance, le charley 3-3-2 et la basse à deux slides sont **justes** : c'est le meilleur squelette de la famille. Les écarts sont dans le son et la vie du motif.

### 3.4 Écarts et changements

1. **808** : même constat qu'en trap (voix dédiée, saturation, glide 100-200 ms) ; en drill elle est **la** signature avec le piano. Recette : `bassPattern` 32 pas `"R-------..R--/O-..R---/5----..R/"` + 2e variante pour le 2e couplet (`bassBuild`) avec un long slide descendant `"O-------------/R|--------........"` ; `StyleFx("bass", preset "crunch", insert "PUMP" [0.3,0.15,0])`. Mesure : comme trap G1.
2. **Le lead signature est inaudible et mécanique** : Piano FM à −24 dB sous le mix (CORPS), 6 notes par mesure sans trou, motif d'une mesure transposé par accord. Changement : son « Piano électrique » (ou « Cloche » oct 5) à +6 dB (correction de `SYNTH_GAIN_DB` du Piano FM, plan T1), `leadOnKey = true`, `leadPattern` 64 pas, 2-4 notes/mesure, un trou par mesure, variation mesure 4 : `"R---..3-........|5---..3-..2-....|R---..3-........|5-..7-..O-------"`, vélocités `R` 0,8 / autres 0,65 ; `StyleFx("lead", reverb 0.4, delay 0.25)` ; couche de soutien « Cordes » en nappe à −8 dB (déjà), attaque lente. Mesure : piste lead ≥ −10 dB rel. ; 2-4 notes/mesure ; mesure 4 ≠ mesure 2.
3. **Charleys vivants** : notre roulement `33.3` est le même toutes les 2 mesures et à vélocité fixe ; il manque les trous, la variation de vélocité, le shaker. Changement : `swing = 0.12` (le moteur ne swingue que les pas impairs : exactement « les contretemps »), hat 64 pas avec 2 charleys retirés par 2 mesures, un `2` avant la caisse du 3 (pas 7) et un `3` en fin de mesure 2 seulement une fois sur deux ; `Perc2` shaker `"..o..o...o..o..."` au Refrain ; humanisation ±8 % / ±4 ms ; moteur : triolets + rampe (G3). Mesure : écart-type vélocité ≥ 0,05 ; ≥ 2 pas vides de charley par 2 mesures ; 4 mesures de charley distinctes.
4. **Arrangement** : Refrain = Couplet + nappe (+2 couches) sans coupure, intro = piano + charleys (bien) ; il manque la coupure de 808 de 2 mesures avant le hook, le changement toutes les 8 mesures, le pont sans basse, la crash. Changement : forme « rap » (synthèse G4) avec `dropOutBars = 2` et un Pont 8 mesures (nappe filtrée 0,35→0,85 comme le Break club, kick coupé) ; crash sur le 1 de chaque Refrain. Mesure : Refrain − Couplet ≥ +2 LU ; LRA 4-8 (aujourd'hui 6,0 mais dû à l'intro faible, pas au contraste).
5. **Caisse** : une seule caisse IA ; les hits empilent caisse + clap/rim et une réverbe courte. Changement : `Clap "........x..................." ` (0,6) sous la caisse du 3 seulement, `StyleFx("snare", reverb 0.12)`, `StyleFx("drums", preset "punch")`. Mesure : 2-6 kHz ≥ −18 dB (aujourd'hui −20,5).
6. **Durée** : 135 s contre 3:00-3:36 → forme « rap » de 88-104 mesures (149-176 s).

---

## 4. Lo-fi hip-hop (id `lofi`, 80 BPM)

### 4.1 Dix morceaux repères

| # | Artiste – Titre (année) | BPM | Tonalité / accords | Notes de production | Src |
|---|---|---|---|---|---|
| 1 | Nujabes – Aruarian Dance (2004) | 100 (Hooktheory) | Fa♯ majeur ; **ii9 – V13** (deux accords) ; mélodie de guitare samplée 0,52 temps/note, ambitus 36 ½ tons | guitare Laurindo Almeida bouclée, batterie douce, « godfather » du lo-fi | [38][37] |
| 2 | Nujabes – Feather (2005) | ≈ 90 (Hooktheory 181) | Ré♭ majeur ; **I7 – ii7sus2 – IV9 – V – vi** ; 0,73 temps/note | piano jazz, rap posé, boucle qui ne résout pas | [38] |
| 3 | Nujabes – Luv(sic) Part 3 (2005) | n.t. (≈ 90) | majeur, 7es | piano + cordes, même recette | [37] |
| 4 | J Dilla – Don't Cry (2006, Donuts) | ≈ 86 (SongBPM 173) | Do♯/Ré♭ majeur | sample des Escorts **coupé en croches**, ralenti puis accéléré, pianos empilés ; 1:59 ; MPC3000 **sans quantisation** | [39][37] |
| 5 | Tomppabeats – Harbor (2016) | n.t. (≈ 80) | n.t. | boucle de piano ultra-courte, craquement, 2 min | — |
| 6 | Jinsang – Affection (2016) | 84 (SongBPM) | n.t. | Rhodes + guitare, batterie feutrée ; **1:57** | [39] |
| 7 | idealism – controlla (2017) | n.t. (≈ 85) | n.t. | piano électrique, swing lourd | — |
| 8 | potsu – i'm closing my eyes (2017) | n.t. (≈ 90) | n.t. | voix samplée + Rhodes, le hit YouTube | — |
| 9 | Joey Pecoraro – Finding Parking (2017) | 64 (SongBPM 128) | La♭ majeur | boucle de clavier lente, très peu de batterie ; 2:53 | [39] |
| 10 | In Love With a Ghost – flowers feat. nori (2017) | 99 (SongBPM) | La♭ majeur | piano + voix, plus lumineux ; 4:12 | [39] |

### 4.2 Traits communs

- **Tempo** : 64-100, cœur 80-92 (Affection 84, Don't Cry ≈ 86, Feather ≈ 90) ; Attack (chillout poussiéreux) 90-110, swing 50-60 % [4] ; Splice cite des exemples à 68 et 90 [40].
- **Harmonie** : **majeur** dominant (7 titres sur les 8 renseignés), accords **étendus** (maj7, m7, 9, 11, 13 : Splice, NI), **ii – V** et cadences jazz, boucles de 2 à 4 accords qui **ne résolvent pas** (NI : IVmaj7 – iii7 – vi7 « peut continuer pour toujours » ; Aruarian Dance : ii9 – V13 seulement), inversions et large étalement d'octaves, chords en commun pour des transitions douces (Dm7 → Fmaj7) [38][40].
- **Rythme des accords** : Rhodes/piano samplé, accords anticipés, 1-2 par mesure, tenus ; sample ralenti (Dilla « slowed then sped up ») [37][39].
- **Batterie** : feutrée, **non quantisée / « drunk »** (Dilla MPC3000 quantize off), kick sur 1 et 3 (+ fantôme), caisse souvent rim/brossée sur 2 et 4, charleys doubles swinguées avec fantômes, deux charleys ouverts « slurp », vélocités variables, craquement vinyle en couche, bus batterie compresseur + bande [4][37].
- **Mélodie** : un fragment samplé (guitare, flûte, piano, voix) de 2-4 mesures, notes longues, beaucoup de silence, parfois plus dense que les accords (Aruarian 0,52 temps/note = la guitare brésilienne) [38].
- **Basse** : douce, suit la fondamentale, 1-3 notes par mesure, sinus/doigts ; souvent la basse du sample [37].
- **Structure** : **très court** (1:57, 1:59, 2:53) ; intro 2-4 mesures (crackle + accords), A 8-16, B 8 (filtre, coupe du sample, batterie retirée), A, fin abrupte ou tape-stop ; variations toutes les 8 mesures par soustraction [37][39].
- **Son** : SP-303/SP-404 (bouton lo-fi), craquement, passe-bas, pleurage de bande, bitcrush léger, large stéréo sur les claviers, peu d'aigus ; sonie faible (cible projet −12 ±1,5 : juste) [37][40].

### 4.3 Notre recette

```
Kick  "X......x..X.....X.x.......X....."   Snare "....x.......x..."   Hat "x.oxx.o.x.oxx.oo" (build "x...x.o.x...x.o.")   Perc shaker "...o...o...o...o"   swing 0.25
Melody root 5 (Fa), Dark Minor [0,3,6,2] (Happy Major [1,4,0,0], Dreamy Major [3,5,1,4], Future Dorian [0,0,3,3]) ; chords "Piano électrique" Ninth "X-------X----X--" oct 3
       bass "Basse ronde" oct 2 "R---------R-5---", pack lofi:bass ; lead "Marimba" oct 5 "........5.3.2.R." (off) ; signature = Pad
Fx    pad wide_alive + VINYL + reverb 0.2 ; drums warm_vinyl ; lead reverb 0.3
```

Tempo, swing (≈ 62 %), batterie fantôme, voicing Ninth, vinyle : la base est bonne (CORPS : 6/10, le meilleur de la famille).

### 4.4 Écarts et changements

1. **Humeur par défaut mineure, progression de triades.** Dark = i – iv – VII – III en mineur naturel ; les hits sont majeurs avec ii – V et maj7/9. Changement : `mood = Mood.Dreamy` par défaut pour ce style ; `Dreamy = Major [3,2,5,1]` (IVmaj7 – iii7 – vi7 – ii7, NI n°4), `Happy = Major [1,4,0,0]` → `[1,4,0,5]` (ii9 – V13 – Imaj7 – vi7), `Dark = Dorian [0,3,0,3]` (i7 – IV7, Nujabes), `Future = Minor [0,5,3,6]`. Mesure : ≥ 4 notes par accord (Ninth) ; 2 accords au moins avec 7e majeure dans Dreamy/Happy.
2. **Rythme d'accords identique à chaque mesure.** Changement : `chordRhythm` 32 pas avec anticipation `"X-------x-----X-|------X---x...X-"` (minuscule = plus doux, l'accord de la fin de mesure 1 anticipe la mesure 2) ; piano électrique avec vélocité 0,7. Mesure : onsets des mesures 1 et 2 différents.
3. **Pas de phrase mélodique** : lead off, motif d'une mesure. Changement : `defaults.lead = true`, son « Piano électrique » ou vibraphone GM, octave 4, `leadOnKey = true`, 64 pas : `"....5---3-------|................|....5---3---2---|R---------------"` (phrase, silence, réponse, note longue) ; Refrain : même phrase à l'octave ; `StyleFx("lead", reverb 0.35, delay 0.2)`. Mesure : ≥ 1 mesure vide par 4 ; notes ≥ 2 pas.
4. **Batterie quantisée.** Changement (générateur, plan étape 6) : humanisation à graine fixe, charleys ±8 % / ±6 ms, caisse +5 ms (traînée Dilla), kick 0 ; fantômes `o` de caisse `"....x......o.x.o"`. Mesure : écart-type vélocité charley ≥ 0,05 ; décalage moyen de la caisse +3 à +6 ms.
5. **Texture et B-section.** Il manque le tape-stop / filtre de la section B et le craquement continu ; la largeur mesurée (−31 dB) contredit le `wide_alive` : à vérifier (ordre des inserts : preset puis VINYL, l'un écrase-t-il l'autre ?). Changement : `preset "lofi_tape"` sur la nappe (pleurage) puis VINYL ; Couplet 2 = nappe filtrée (FILTER 0,4 → 0,8 sur 8 mesures, mécanisme `Sweep` existant) ; insert TAPE_STOP sur le master la dernière mesure avant le 2e Couplet (automation INSERT_PARAM) ; Refrain = + lead + shaker + charley ouvert `"..............x."`. Mesure : côté/centre ≥ −22 dB ; ≥ 2 blocs de 8 mesures distincts.
6. **Durée et tempo** : 192 s pour des hits de 2 min ; 80 BPM est bas dans la plage. Changement : forme courte pour le lo-fi (Intro 4 – A 16 – B 8 – A 16 – Outro 4 = 48 mesures = 144 s à 80) ; tempo 84-86 par défaut. Sans mesure autre que la durée (120-160 s).

---

## 5. Phonk (id `phonk`, 130 BPM)

### 5.1 Dix morceaux repères

| # | Artiste – Titre (année) | BPM | Tonalité / accords | Notes de production | Src |
|---|---|---|---|---|---|
| 1 | Three 6 Mafia – Tear da Club Up '97 (1997) | 161 (SongBPM, ressenti 80) | Do♯/Ré♭ | Memphis : TR-808, cowbell, sub, caisse numérique sèche, chant répété, cassette lo-fi | [44][41] |
| 2 | SpaceGhostPurrp – Mysterious Phonk (2012, album) | — | — | nomme le genre : samples Memphis + chopped & screwed | [41] |
| 3 | Kaito Shoma – Scary Garry (2016) | n.t. | n.t. | « un des premiers drift phonk » | [41] |
| 4 | $uicideboy$ – Paris (2017) | n.t. | n.t. | phonk vocal, 808 saturée, samples Memphis | — |
| 5 | Freddie Dredd – Cha Cha (2019) | 130 (SongBPM) | n.t. | cowbell + voix, 2:54 | [44] |
| 6 | PlayaPhonk – PHONKY TOWN (2020) | n.t. (≈ 130) | n.t. | cowbell, le hit drift TikTok | — |
| 7 | Dxrk ダーク – RAVE (2021) | n.t. (≈ 140) | n.t. | single drift notable (Wikipedia) | [41] |
| 8 | INTERWORLD – METAMORPHOSIS (2021) | n.t. (≈ 140) | n.t. | single drift notable (Wikipedia) | [41] |
| 9 | DVRST – Close Eyes (2021) | 120 (Hooktheory) | Ré mineur ; **i – VI7 – III – ii° – VII6** ; mélodie Sol3-Do5, 0,70 temps/note | référence du tutoriel NI ; cowbell + nappe contre-chant | [43][42] |
| 10 | Kordhell – Murder in My Mind (2022) | 150 (Hooktheory) | Ré♯ mineur ; **isus2 – VI(add11)** ; ligne transcrite Fa4-Sol♯4 (3 ½ tons), 2,67 temps/note | 1er phonk dans le top 500 Spotify ; platine US ; 2:25 ; référence EDMProd | [43][42] |

(Les BPM n.t. : quota de recherche atteint ; les tutoriels placent le drift phonk à 120-150, NI à 90-120 en comptage demi-temps.)

### 5.2 Traits communs

- **Tempo** : Memphis 160/80 en demi-temps ; drift phonk 120-150 (EDMProd : « 120 to 150 », projet 130 ; 170-180 « feels like half-time ») ; Close Eyes 120, Cha Cha 130, Murder 150 [42][43][44].
- **Tonalité** : mineur, **phrygien / phrygien dominant** (EDMProd : degrés 1, ♭2, 5, ♭6 « sonnent le mieux » ; NI : mode phrygien) ; 2 à 5 accords (Murder : 2 ; Close Eyes : 5) [42][43].
- **Cowbell** : 808 cowbell **accordée** dans un sampler, légèrement désaccordée, distordue, motif de 1-2 mesures, **doublée à l'octave supérieure dans la 2e partie**, enveloppe courte (attaque 0, decay 0,54 s), chorus → compresseur → distorsion → réverbe, petite enveloppe de hauteur, LFO sur le filtre [42].
- **808** : **sur chaque kick**, surtout la fondamentale, clipping + distorsion + saturation, sub mono, sidechain au kick ; glissandos d'octave optionnels [42].
- **Batterie** : kick lourd ; **caisse = caisse + 2e caisse + clap + « reverb clap »** (NI : 2e caisse un coup sur deux) ; charleys = boucles coupées, OTT + saturation lourde, vélocités variables ; « tout est distordu » (EDMProd : Decapitator sur chaque bus) ; Memphis : caisse numérique sèche, rythmes syncopés [42][41].
- **Voix** : acapellas Memphis **coupées**, couche grave + couche aiguë, mono, formant/pitch, EQ radio, distorsion + bitcrush, sidechain kick/caisse, « souvent enterrées et incompréhensibles » [42].
- **Contre-mélodie** : nappe ou lead clairsemé (NI « Cassy Ohs ») + réverbe + délai [42].
- **Structure** (EDMProd, 2:25) : pré-intro 2 temps – **intro 8** (roulement de kick avant le drop, risers toutes les 4 mesures, tout retiré juste avant) – drop 8 – drop + voix 8 (lead à l'octave) – **switch 8** (autre mélodie de lead, voix, nappes ; on garde beat, son du lead, 808) – drop 8 – drop + voix 8 – switch 16 – outro 4 ; NI : entrées/sorties toutes les 8 ou 16 mesures, swishes de bruit blanc [42].
- **Mix / sonie** : OTT sur les bus, sidechain du lead, clipping + brickwall, « pousser plus fort que raisonnable » ; estimation ≈ −6 à −8 LUFS-I (club : −6 à −9 selon Mastering The Mix) [42][50]. Cible projet −8,5 : **trop sage pour le phonk**.

### 5.3 Notre recette

```
Kick  "X.....X...X....."   Clap "....X.......X..."   Snare "............X..."   Hat "x.x.x.x.x.x.x.xxx.x.x.x.x.x.x.22"   OpenHat "..............x...............x."
Melody root 5 (Fa), Dark Phrygian [0,0,1,0] ; pad "Nappe" off ; bass "Basse sub" oct 1 "R-----R---R-5/O-" glide, pack phonk:808 ; lead kit:11 (cowbell) oct 5 "R..R..R.3..3..2." (signature)
Fx    bass distortion ; lead crunch + reverb 0.2 ; riser (= impact)
```

Cowbell en phrygien, 808 distordue, clap 2-4 + caisse de renfort, roulement : les ingrédients sont là.

### 5.4 Écarts et changements

1. **Cowbell : motif d'une mesure, mono, sans variation ni doublage.** Changement : `leadOnKey = true`, `leadPattern` 32 pas `"R..R..R.3..3..2.|R..R..R.2..2..R-"` et un **second motif pour le Refrain** (`leadBuild`, nouveau champ miroir de `Line.build`) `"R..R.3..R..2..R-|R..R.3..6-..5-.."`, octave 4 (cowbell ≈ 500-800 Hz), `StyleFx("lead", preset "distortion", reverb 0.3, delay 0.25)` + insert CHORUS 0,2 ; au Refrain, doublage à l'octave supérieure (2e piste lead, −6 dB). Mesure : côté/centre ≥ −20 dB (aujourd'hui −37,8) ; 2 motifs de cowbell distincts par 16 mesures.
2. **Trop propre, pas assez fort.** Tout le phonk est écrêté ; nous n'avons que la distorsion sur la basse. Changement : `StyleFx("drums", preset "punch")` + insert SATURATION 0,4 sur le bus batterie ; `StyleFx("master", insert "OTT" [0.3,0.5,0.5,0.5])` ; caisse sur **2 et 4** `"....X.......X..."` empilée sur le clap + `StyleFx("clap", reverb 0.25)` ; cible de sonie phonk −7 ±1 (table § 2.4 du plan son). Mesure : LUFS-I −7 ±1, crête du Drop ≤ 7 dB, TP ≤ −1.
3. **808 sur chaque kick, voix dédiée** (G1) ; la ligne suit déjà le kick. Ajouter `insert "PUMP"` court. Mesure : 60-250 ≥ −5 dB.
4. **Aucune « voix » Memphis.** On ne peut pas embarquer d'acapellas ; mais un hit vocal bitcrushé ferait l'effet. Changement : ligne `Perc2` avec un son du pack `vocal_chops` (ou « Voix robot » tenue 1 pas) pitché −12, `preset "phone"` + insert BITCRUSH, motif `"......x.........|..............x."` au Couplet 2 et au Refrain. Mesure : événement 300 Hz-3 kHz présent 1 fois par 2 mesures sur la piste.
5. **Refrain identique au Couplet, intro trop faible** (LRA 12,1 : la cowbell seule en intro). Changement : intro = cowbell + charleys + **kick en croches sur la dernière mesure** (roulement `rollHits` réutilisé sur 1 mesure), Refrain = + nappe (`defaults.chords = true`, « Nappe » à −8 dB, wide_alive) + charley ouvert + doublage octave ; switch toutes les 8 mesures (motif B) ; souffle de 2 mesures avant chaque Refrain. Mesure : Refrain − Couplet ≥ +2 LU ; LRA ≤ 8.

---

## 6. Grime (id `grime`, 140 BPM)

### 6.1 Dix morceaux repères

| # | Artiste – Titre (année) | BPM | Tonalité / accords | Notes de production | Src |
|---|---|---|---|---|---|
| 1 | Youngstar / Musical Mob – Pulse X (2002) | n.t. (≈ 140) | — | « année zéro » du grime : **une seule boucle de 16 mesures** : 8 en 2-step, 8 avec **kick + caisse seuls** ; batterie ReBirth ; sample vocal toutes les 32 mesures | [45] |
| 2 | Wiley – Eskimo (2002) | 138 (SongBPM) | Si♭ mineur | archétype eskibeat : synthé glacé, stop-start, série de dubplates | [45][46] |
| 3 | Dizzee Rascal – I Luv U (2003) | 136 (SongBPM) | n.t. | fait en 30 min ; cordes « à la Timbaland », basse lourde, voix bégayée en intro, influence Three 6 Mafia / crunk | [45][46] |
| 4 | Dizzee Rascal – Fix Up, Look Sharp (2003) | 105 (SongBPM) | n.t. | **hors norme** : sample Billy Squier « The Big Beat », rock | [46] |
| 5 | Lethal Bizzle – Pow! (Forward) (2004, Dexplicit) | 138 (SongBPM) | Do♯/Ré♭ | FL Studio < 1 h, **808**, hip-hop US samplé ; **« eight-bar rally » sans refrain, le beat alterne deux boucles de 8 mesures** | [45][46] |
| 6 | Ruff Sqwad / XTC – Functions on the Low (2004) | 140 (SongBPM) | Sol♯ mineur | l'instru la plus vocalisée du grime ; mélodie de synthé mélancolique | [46] |
| 7 | Kano – P's and Q's (2005) | n.t. (≈ 140 ; SongBPM 89 incohérent) | n.t. | eskibeat, kick/caisse secs | — |
| 8 | Skepta ft. JME – That's Not Me (2014) | 139 (SongBPM) | Do (SongBPM) ≈ La mineur | « throwback » volontaire au premier grime, clip à 80 £ | [45][46] |
| 9 | Skepta – Shutdown (2015) | 138 | **Sol mineur harmonique** ; i – V – III+ – i – vii° **en « no3no5 »** (notes seules : Sol – Ré – Mi♭ – Sol – Fa♯) ; 0,40 temps/note (flow) | Ragz Originale + Skepta ; stabs d'une note, sample Vine | [46] |
| 10 | Stormzy – Shut Up (2015, beat XTC) | 140 (via Functions ; SongBPM 74/148) | Sol♯ mineur | freestyle sur Functions on the Low | [45][46] |

### 6.2 Traits communs

- **Tempo** : 136-140 (Eskimo 138, Pow 138, Functions 140, That's Not Me 139), « 140 parce que c'était le tempo par défaut de FL Studio » (Wiley) ; feel double-temps ou demi-temps ; outlier à 105 [45][46].
- **Tonalité** : mineur, mineur harmonique ; **lignes d'une note (bass + stab « no3no5 ») plutôt que des accords** ; riffs de 3-5 notes qui doublent ou répondent à la basse [46].
- **Structure** : boucles de **8 mesures** qui alternent (Pow : deux boucles de 8), ou 16 mesures dont 8 dépouillées (Pulse X : kick + caisse seuls) ; « 8-bar » = le beat change toutes les 8 mesures ; pas de refrain, relais de MC de 8 ou 16 mesures ; sample vocal toutes les 32 [45].
- **Batterie** : breakbeats syncopés 4/4 hérités du 2-step (kick décalé, caisse 2-4 ou déplacée, pas de charley continu), parfois demi-temps, sons 808/909 secs, « clattering », peu de réverbe ; FL Studio / ReBirth [45].
- **Basse** : sub lourd (≈ 40 Hz « sublow »), **carré/scie** grognant, stop-start, souvent la mélodie elle-même [45].
- **Sons** : textures « jagged » agressives, synthés et cordes (Timbaland), **chiptune/ringtone** (scie, 3xOsc), eskibeat glacé (Eskimo), coups de feu, stabs d'orchestre ; sinogrime (motifs est-asiatiques) [45].
- **Sonie** : moderne ≈ −8 LUFS-I (estimation ; cible projet −8,5 juste).

### 6.3 Notre recette

```
Kick  "X......X..X.....X.........X..X.."   Clap "....X.......X..."   Perc clic "..x...x...x...x."   Hat "x.x.x.x.x.x.x.x." (build "x...x...x...x...")
Melody root 4 (Mi), Dark Minor [0,0,5,4] ; chords "Orgue" triad "X.....X........." oct 3 (signature) ; bass "Lead carré" oct 2 "R-.R-.R---5---3-" ; lead "Marimba" oct 5 ".O..5..O..3..5.." (off)
Fx    pad reverb 0.2 ; lead delay 0.2
```

### 6.4 Écarts et changements

1. **Pas de stab eski ni de « voix »** : des triades d'orgue douces (stabs à −23 dB, CORPS) là où le grime a des stabs d'une note glacés, carrés/scie, et des cordes. Changement : nappe = « Lead carré » (ou « Carré saturé ») en `Voicing.Single` + octave (`Voicing` « octave », STYLE-FIXES gabber) sur `chordRhythm` 32 pas répondant à la basse `"X.....X.........|....X.....X...X."`, `StyleFx("pad", insert "CHORUS" 0.3, reverb 0.15)` ; `defaults.lead = true` avec « Lead chiptune » oct 5 jouant le riff de la basse à l'octave `"R-.R-.R---5---3-"` au Refrain seulement. Mesure : piste stabs ≥ −12 dB rel. ; 2-6 kHz ≥ −16 dB (aujourd'hui −19,3).
2. **Basse : carré seul, sans sub.** Changement : `subBass = true` (mécanisme `withSubTrack` existant : sinus une octave sous la basse, passe-haut 120 Hz sur la carrée) ; ligne 32 pas stop-start `"R-.R-.R---5---3-|R-.R-.R---2---R."`. Mesure : sub < 60 Hz ≈ −2 dB rel. et 60-250 ≥ −5 (aujourd'hui −3,7 : correct), corrélation < 120 Hz ≥ 0,98.
3. **Aucune alternance de 8 mesures.** Changement : forme « 8-bar » pour le grime : Couplet = A 8 (tout) + B 8 (stabs muets, basse variante, kick + caisse seuls comme Pulse X), Refrain = A avec lead ; `scenePlan` : scène « B » = Couplet moins la nappe moins les charleys. Mesure : ≥ 2 blocs de 8 mesures distincts par 16 ; Refrain − Couplet ≥ +2 LU.
4. **Batterie trop polie** : hits = 808/909 secs et durs, « clattering ». Changement : `kit = {Kick: tr909, Clap: tr909}`, `StyleFx("drums", preset "punch")` ; charley au Couplet en noires (déjà `build`), + charley ouvert `"......x.........|..............x."` au Refrain ; coup de feu / impact = riser impact déjà disponible (`riser = true`). Mesure : crête du bus batterie ≤ 9 dB ; air ≥ −22 (aujourd'hui −24).
5. **Largeur** : −34,8 dB. Les stabs en chorus + délai 1/8 ping-pong sur le lead (`preset "pluck_pingpong"`) suffisent. Mesure : côté/centre ≥ −22.

---

## 7. Jersey club (id `jersey_club`, 140 BPM)

### 7.1 Dix morceaux repères

| # | Artiste – Titre (année) | BPM | Tonalité / accords | Notes de production | Src |
|---|---|---|---|---|---|
| 1 | DJ Tameil – Dat Butt EP (2001) | ≈ 130 | — | parmi les premières productions jersey ; Tameil = fondateur (Brick Bandits) | [47] |
| 2 | Unicorn151 & DJ K-Swift – Tote It Remix (2008) | n.t. | — | premier rap sur un beat jersey | [47] |
| 3 | DJ Frosty – Ride That Wave (2009) | n.t. | — | version de « Make It Jump » passée en radio ; exemple du motif de kick (NI) | [47][48] |
| 4 | Cookiee Kawaii – Vibe (If I Back It Up) (2019) | 160 (SongBPM, douteux ; ≈ 140) | Sol mineur (SongBPM) | viral TikTok 2020 ; voix chantée + kicks | [49][47] |
| 5 | Bandmanrill – Heartbroken (2021) | n.t. (≈ 140) | n.t. | « Jersey drill » : rap + kicks jersey | [47] |
| 6 | UNIIQU3 – Microdosing (2021) | n.t. (≈ 140) | n.t. | défilé Versace × Fendi ; breakdown Point Blank (inaccessible) | [47] |
| 7 | Drake – Sticky (2022, Gordo) | n.t. (≈ 135-140) | n.t. | figure de kick à cinq coups ; Wikipedia le classe Baltimore club ; 4:03 | [49][47] |
| 8 | Drake – Currents (2022) | n.t. | n.t. | **bed squeak** de « Some Cut » | [47] |
| 9 | Lil Uzi Vert – Just Wanna Rock (2022, MCVertt & Synthetic) | n.t. (≈ 150) | n.t. | « synthy, queasy high-BPM beat », synthés 70s, un seul couplet court + crochet chuchoté ; **2:03** ; n°10 Hot 100 | [49][47] |
| 10 | PinkPantheress & Ice Spice – Boy's a Liar Pt. 2 (2023, Mura Masa) | n.t. (≈ 133) | n.t. | beat jersey + chiptune/hyperpop, lo-fi ; 2:11 ; n°3 Hot 100 | [49][47] |

### 7.2 Traits communs

- **Tempo** : 130-140 (Orphiq, Music Origins), 135-140 (Recording Arts), projet NI 142 ; Baltimore parent 125-130 [47][48].
- **Kick** : **cinq coups par mesure sur les doubles 1, 5, 9, 12, 15** (espacements 4-4-3-3-2 ; NI et Orphiq : « This is the genre. Everything else is decoration ») ; kick empilé et saturé, **passe-haut (NI : cutoff 16,6 %) pour laisser le sub à une 808 qui double le kick** [48].
- **Caisse / clap** : contre la course de kicks, pas un backbeat plat : NI : pas 9 et 12 (mesures 1 et 3), pas 9, 11 (vél. 84) et 12 (mesures 2 et 4) ; crash sur le 1 de la mesure 1 [48].
- **Charleys** : rares, fermés sur 2 et 4 ; le kick remplit déjà [48].
- **Bed squeak** : grincement de lit de Trillville « Some Cut » (2004, Lil Jon), coupé par Tameil, en percussion sur ou entre les kicks, « léger ou à sauter » [47][48].
- **Voix** : **hachis de 1-2 syllabes, staccato, répétés** ; motif de 2 mesures (NI : pas 3 et 7 | 3, 4 (vél. 68) et 7, en Do3) ; **pas de topline chantée**, les chops portent le crochet ; appels de danse ; breaks de Lyn Collins « Think » / Gaz « Sing Sing » [47][48].
- **Autres couches** (NI) : lead « breath » (souffle) qui suit le rythme du kick moins la dernière note, Fa3/Fa4, attaque et release minimales ; 808 Fa2 sur le rythme du kick, Sol♯2 / Sol2 aux mesures 5-6 ; nappe « Mystic Vision » Fa3 puis Fa2 ; effet « Counting Comets » dernière mesure [48].
- **Structure** : entrées **toutes les 8 mesures**, voix + lead retirés à la mesure 41, retour à 49, puis retraits toutes les 8 ; « rhythmic breaks » fréquents ; morceaux pop très courts (2:03, 2:11) ; souvent des remix de rap/R&B [47][48].
- **Tonalité** : peu d'harmonie ; une note de basse tenue sur 4-8 mesures ; majeur pop (Just Wanna Rock, Boy's a Liar) ou mineur ; master NI « Target EDM » (sonie club ≈ −7/−8 LUFS, estimation) [48][50].

### 7.3 Notre recette

```
Kick  "X...X...X..X..X."   Clap "........X..X............X.xX...."   Hat "..x...x...x...x."   Perc toc "..o...o..o..o..."   OpenHat "......x.......x."
Melody root 5 (Fa), Dark Minor [0,0,6,5] ; pad "Nappe chaude" Seventh "X-------X-------" oct 4 (off) ; bass "Basse sub" oct 1 "R...R...R..R..O.", pack tr808:808 ; lead "Voix robot" oct 4 "..R...R..R..O..." pack vocal_chops:vocal (signature)
Fx    pad wide_alive ; lead delay 0.2 ; riser (= impact)
```

Le kick (1-5-9-12-15), le clap (pas 8/11 puis 8/10/11 : **exactement NI**), la 808 qui double le kick et les chops sur les pas 2/6 : la grille est **juste**.

### 7.4 Écarts et changements

1. **Le crochet vocal est un synthé « Voix robot » sur un motif d'une mesure.** Les hits : syllabes coupées, 2 mesures avec un coup plus doux, bégaiement, deux chops différents, octave. Changement : lead = pack `vocal_chops` par défaut quand présent (sinon « Voix robot » pitché −5) ; `leadPattern` 32 pas `"..R...R..R..O...|..R.r.R..5..O.3."` (minuscule = coup doux comme NI vél. 68) ; `StyleFx("lead", preset "pluck_pingpong")` ou insert CHOPPER (porte 1/16) + delay 0,25 ; 2e piste « souffle » (« Souffle » du synthé, notes d'un pas) sur le rythme du kick sans le dernier coup `"X...X...X..X...."` oct 4 à −9 dB (le « Beta Breath » de NI). Mesure : côté/centre ≥ −20 dB (aujourd'hui −37,5) ; ≥ 2 vélocités distinctes sur la piste lead ; 2 mesures de lead distinctes.
2. **Graves faibles et kick/808 qui se battent** : 60-250 Hz à −9,9 dB (le plus bas des 7) ; le kick du kit est un sinus 47 Hz comme la 808. Changement : kick du pack `tr808` (ou kit) avec `StyleFx("kick", insert "FILTER" passe-haut ≈ 0,15)` et la 808 (`tr808:808` ou voix 808 G1) qui porte le sub, saturation légère ; nappe Seventh à l'octave 3 au lieu de 4 (trou de bas-médium −14,4). Mesure : 60-250 ≥ −6 dB, 250-500 ≥ −12.
3. **Pas de respiration toutes les 8 mesures ni de « reset ».** Changement : Couplet 1 = kick + 808 + chops (sans clap les 8 premières mesures : scène B), Refrain = + clap + charleys + nappe + souffle + charley ouvert ; **1 mesure de « reset »** (kick seul sur le 1, tout le reste muet) avant chaque Refrain et au milieu du Refrain ; crash sur le 1 (impact riser déjà là, le rendre plus court et clair). Mesure : Refrain − Couplet ≥ +2 LU ; 1 mesure par 16 avec énergie > 200 Hz ≤ −20 dB.
4. **Bed squeak** : notre rim fait le job rythmique ; un pack « jersey_club » pourrait inclure un grincement court (son généré « squeaky bed spring one shot ») en voix `Perc`. Sans mesure (à l'oreille).
5. **Durée / tempo** : 137 s correct pour un style à 2:03-2:11 ; 140 ✓ (NI 142).

---

## 8. Synthèse famille : les 10 plus gros écarts, par impact audible

Chaque écart : ce qu'il implique (recette R, générateur G = `StarterWizard`/`WizardMix`, moteur M), et comment le vérifier au banc (`scripts/song-bench.sh`, mesures de `docs/son/banc`) ou par un test unitaire.

| # | Écart (styles touchés) | Changement | Vérification |
|---|---|---|---|
| 1 | **Pas de vraie 808** : sinus + crunch, glide 80 ms, pack IA qui ne glisse pas (trap, drill, phonk, jersey ; la basse de 4 styles sur 7) | **M** : voix « 808 / Sub » dédiée (plan étape 7) : sinus + enveloppe de hauteur (−3 ½ tons / 40 ms) + clic + saturation tanh réglable + glide 60-200 ms proportionnel à la longueur du `/` + tenue 1-2 s, accordée, mono < 120 Hz ; **R** : lignes 808 sur 32 pas en notes longues avec un slide par mesure (octave, quinte), PUMP court sur le kick | 60-250 Hz ≥ −6 dB rel. et 250-500 ≥ −14 au Drop ; sur la piste basse seule, ≥ 1 glissando ≥ 7 ½ tons de 100-200 ms par 2 mesures ; corrélation < 120 Hz ≥ 0,98 (déjà) |
| 2 | **Mélodies mécaniques** : motif d'une mesure rejoué 4 fois, **transposé sur chaque accord**, sans silence ni question-réponse, 3 vélocités (les 7 styles ; c'est la plainte de Benjamin) | **G** : `Melody.leadOnKey` (le motif reste sur la gamme, comme `acidLine`), motifs 32/64 pas acceptés par le convertisseur (F4 étendu au lead), vélocité par note (majuscule/minuscule déjà, + accent `!` = 0,95), **`leadBuild`** = second motif pour le Refrain ; **R** : phrases de 2-4 mesures à ≈ 1 note par temps, un trou ≥ 1 temps par 2 mesures, note tenue en fin de phrase, variation en mesure 4 (trap, drill, lo-fi, phonk, boom bap) ; contre-chant `counter` activé (trap, drill) | Test unitaire : 1re note du lead à la même hauteur aux mesures 1 et 3 ; ≥ 1 silence ≥ 1 temps par 2 mesures ; mesure 4 ≠ mesure 2 ; densité 2-5 notes/mesure (drill 2-4) |
| 3 | **Charleys morts** : pas de triolet, roulements figés dans un pas, à 0,6 décroissant, même fill toutes les 2-4 mesures, aucune humanisation (trap, drill, phonk, boom bap, lo-fi) | **M** : `Hit.offset` en ticks pour triolets de croche/double (F12) + `Hit.ramp` (vélocité croissante dans le roulement) ; **G** : humanisation à graine fixe (±8 % vél., ±4-6 ms charleys/percus, kick 0, caisse lo-fi +5 ms) ; **R** : lignes de charley 64 pas avec 3-4 fins de mesure différentes, charley ouvert accentué, swing 0,1-0,2 par style | Écart-type des vélocités de charley ≥ 0,05 ; ≥ 3 mesures de charley distinctes par 4 ; présence d'événements à 1/12 de temps en trap ; test moteur : roulement de 4 avec rampe → vélocités croissantes |
| 4 | **Forme chanson plate** : Refrain ≈ Couplet (−0,1 LU), pas de coupure de kick/808 avant le refrain, aucune variation toutes les 8 mesures, pas de crash, 137 s à 140 BPM (tous) | **G** : `SongForm.Rap` : Intro 8 – Refrain 8 – Couplet 16 – Refrain 8 – Couplet 16 – Pont 8 – Refrain 16 – Outro 8 (88 mesures ; lo-fi : forme courte 48) ; Couplet = sans nappe, charleys simplifiés, 808 simplifiée ; Refrain = + nappe + contre-chant + charley ouvert + roulements + crash ; **`dropOutBars = 2`** (kick + basse muets, souffle qui monte sur 2 mesures via `riserLane`) avant chaque Refrain ; scène B toutes les 8 mesures (grime : kick + caisse seuls ; phonk : motif B ; jersey : reset 1 mesure) ; fill de caisse 1/16→1/32 toutes les 16 (forme rap : caisse, pas charleys) | Refrain − Couplet ≥ +2 LU ; énergie < 120 Hz des 2 mesures avant le Refrain ≤ −20 dB ; aucun bloc de 8 mesures identique sur toutes les pistes ; durée 150-200 s (lo-fi 120-160) ; LRA 4-8 |
| 5 | **Synthés maigres et mono** : lead = un son, côté/centre −23 à −38 dB contre −15 à −9 (tous) | **R** : lead doublé (octave inférieure « Pluck » / nappe, −6 à −8 dB) ; `preset "pluck_pingpong"` ou CHORUS 0,2-0,3 + reverb 0,3-0,4 + delay 0,25 (croche pointée) sur le lead en trap/drill/phonk/jersey ; nappe `wide_alive` partout ; **G** : vérifier que le preset puis l'insert VINYL (lo-fi) ne referment pas la stéréo | Côté/centre ≥ −18 dB au Refrain (lo-fi, boom bap ≥ −22) ; corrélation L/R 0,7-0,9 ; piste lead ≥ −10 dB sous le mix |
| 6 | **« Boucle samplée » irréaliste** : accords triades/7e au même rythme chaque mesure, mineur par défaut en lo-fi, pas de filtre ni d'anticipation (boom bap, lo-fi, drill nappe) | **R** : `chordRhythm` 32 pas avec anticipation, voicing Ninth, progressions 2 mesures (i – VI – v – v ; ii9 – V13 – Imaj7 – vi7 ; IVmaj7 – iii7 – vi7 – ii7), lo-fi par défaut Dreamy (majeur), son piano/Rhodes + warm_vinyl ou lofi_tape + FILTER passe-bas 0,55 + SATURATION 0,2 ; Pont/B-section = nappe filtrée (Sweep 0,35→0,85) + TAPE_STOP | Onsets d'accords différents entre mesures 1 et 2 ; ≥ 4 notes par accord ; 250-500 Hz ≥ −12 dB (boom bap, lo-fi) |
| 7 | **Batterie trop propre, sans caractère de machine** : pas de 12 bits, de clipping, de caisse empilée, de réverbe de caisse, de fantômes « ba-boom » (boom bap, phonk, drill, grime) | **R** : `drums` preset `punch` (boom bap, drill, grime, jersey), + SATURATION 0,4 et master OTT 0,3 (phonk), kit `tr909`/`tr808` (grime, jersey), caisse + clap empilés (phonk, drill), `snare`/`clap` reverb 0,12-0,25, kicks/caisses fantômes `o` avant les coups forts (boom bap, lo-fi) ; **M** (plan étape 3) : kits échantillonnés CC0 avec couches de vélocité | Crête du bus batterie ≤ 9 dB ; 2-6 kHz ≥ −18 dB ; air ≥ −22 (lo-fi exclu) ; phonk LUFS-I −7 ±1 |
| 8 | **Pas de « voix »** : aucun crochet vocal (jersey : synthé robot ; phonk : aucune voix Memphis ; boom bap : aucun scratch ; trap : aucun ad-lib) | **R** : pack `vocal_chops` par défaut en jersey (chops 2 mesures, 2 vélocités, bégaiement CHOPPER) ; hit vocal bitcrushé `phone` + BITCRUSH en phonk (`Perc2`) ; « stab » vocal court toutes les 4 mesures en trap (4e temps de la 1re mesure, EDMProd) ; boom bap : stab de sample court (`stab` du pack) en guise de scratch au Refrain ; nouveau pack « hiphop_vox » (one-shots CC0, licence dans THIRD_PARTY) | Présence d'un événement 300 Hz-3 kHz sur la piste voix ≥ 1 fois par 2 mesures au Refrain ; test à l'oreille (Benjamin) |
| 9 | **Sonie plafonnée et spectre creux** : trap/drill −9,6/−9,7 pour −8,5 visé (perte 6 LU), air −21 à −27, bas-médiums −14 à −20 (trap, drill, phonk, jersey) | Conséquence de 1, 3, 5, 7 : harmoniques de 808, charleys audibles, nappes à l'octave 3, lead doublé ; **R** : `chordOctave` 3 partout (déjà), cible phonk −7, lo-fi −12 (déjà) ; **G** : `WizardMix` charleys −25 au lieu de −27 en trap/drill/phonk | LUFS-I à ±0,5 de la cible sans plafonnement (perte ≤ 5 LU) ; air ≥ −20 (trap, drill, phonk, jersey) ; 250-500 ≥ −14 |
| 10 | **Swing et fantômes** : swing global seulement (le moteur retarde les pas impairs de toutes les pistes), pas de swing par ligne, pas de « laid-back » de caisse (boom bap, lo-fi, drill) | **M** : swing par voie (ou masque de voies : charleys/percus seulement en drill 10-20 %) ; **G** : décalage de caisse +3-6 ms (lo-fi, boom bap) via l'humanisation ; **R** : boom bap 0,2 (MPC 60 %), lo-fi 0,25, drill 0,12 charleys seuls, trap 0 | Test moteur : avec swing 0,12 « charleys seuls », le kick reste sur la grille (±0 échantillon) ; décalage moyen de la caisse mesuré +3 à +6 ms en lo-fi |

**Ordre conseillé** : 2 et 4 (générateur seul, aucune dépendance moteur, répondent directement à « mélodies amateur » et « arrangement plat »), puis 5-6-7 (recettes), puis 1 et 3 (moteur : 808 et triolets/rampe), 8-9-10 ensuite. Recette par recette, régénérer `StyleRecipes.kt` par le convertisseur, jamais à la main.

**Points à vérifier à l'oreille par Benjamin** (A/B de 30 s au Refrain) : trap et drill après 1+2 (la 808 qui glisse, la cloche qui reste en place), lo-fi après 6 (majeur + Rhodes anticipé), phonk après 7 (distorsion générale), jersey après 8 (chops).

---

## 9. Sources

Bases BPM/tonalité : SongBPM = analyse Spotify (fiabilité moyenne : lectures double/moitié, tonalité relative), Hooktheory = transcription humaine (fiable pour tonalité, progression, ambitus, temps/note).

1. Wikipedia – Boom bap : https://en.wikipedia.org/wiki/Boom_bap
2. RBMA Daily – In Search of Boom Bap (2013) : https://daily.redbullmusicacademy.com/2013/11/in-search-of-boom-bap
3. RouteNote – How to make 90s hip-hop boom bap drums : https://create.routenote.com/blog/?p=12433
4. Attack Magazine – Beat Dissected : Chillout dusty hip hop : https://www.attackmagazine.com/technique/beat-dissected/chillout-dusty-hip-hop/
5. Hooktheory – Shook Ones Pt II : https://www.hooktheory.com/theorytab/view/mobb-deep/shook-ones-pt-ii
6. Wikipedia – Shook Ones, Part II : https://en.wikipedia.org/wiki/Shook_Ones,_Part_II ; Complex (Havoc, le « stove ») : https://www.complex.com/music/a/markelibert/havoc-stove-sound-shook-ones-part-ii
7. SongBPM – Shook Ones Pt II : https://songbpm.com/@mobb-deep/shook-ones-pt-ii
8. SongBPM – N.Y. State of Mind : https://songbpm.com/@nas/n-y-state-of-mind ; Wikipedia : https://en.wikipedia.org/wiki/N.Y._State_of_Mind
9. Hooktheory – Electric Relaxation : https://www.hooktheory.com/theorytab/view/a-tribe-called-quest/electric-relaxation ; SongBPM : https://songbpm.com/@a-tribe-called-quest/electric-relaxation
10. SongBPM – Juicy : https://songbpm.com/@the-notorious-b-i-g/juicy-2005-remaster ; Hooktheory : https://www.hooktheory.com/theorytab/view/notorious-big/juicy ; Wikipedia : https://en.wikipedia.org/wiki/Juicy_(The_Notorious_B.I.G._song)
11. Wikipedia – C.R.E.A.M. : https://en.wikipedia.org/wiki/C.R.E.A.M.
12. Wikipedia – Mass Appeal (song) : https://en.wikipedia.org/wiki/Mass_Appeal_(song)
13. Wikipedia – They Reminisce Over You (T.R.O.Y.) : https://en.wikipedia.org/wiki/They_Reminisce_Over_You_(T.R.O.Y.)
14. SongBPM – IAM Petit frère : https://songbpm.com/@iam/petit-frere
15. SongBPM – Nekfeu On verra : https://songbpm.com/@nekfeu/on-verra
16. Wikipedia – DJ Premier : https://en.wikipedia.org/wiki/DJ_Premier
17. Wikipedia – Trap music : https://en.wikipedia.org/wiki/Trap_music
18. Hooktheory – Mask Off : https://www.hooktheory.com/theorytab/view/future/mask-off ; Wikipedia : https://en.wikipedia.org/wiki/Mask_Off ; Time (sample) : https://time.com/4725237/future-mask-off-challenge ; SFR Beats (76 BPM Dm) : https://sfrbeats.com/tools/bpm/future/mask-off
19. Wikipedia – Bad and Boujee : https://en.wikipedia.org/wiki/Bad_and_Boujee ; audiokeychain (127 BPM Ebm) : https://www.audiokeychain.com/track/YTsZ/migos-bad-and-boujee-feat-lil-uzi-vert ; LiFTED (stop-and-go) : https://liftedasia.com/article/metro-boomin-complexcon-hong-kong
20. Wikipedia – XO Tour Llif3 : https://en.wikipedia.org/wiki/XO_Tour_Llif3 ; SongBPM : https://songbpm.com/@lil-uzi-vert/xo-tour-llif3 ; The Fader (TM88) : https://www.thefader.com/2017/06/09/tm88-explains-xo-tour-llif3-beat
21. Hooktheory – Bodak Yellow : https://www.hooktheory.com/theorytab/view/cardi-b/bodak-yellow ; Wikipedia : https://en.wikipedia.org/wiki/Bodak_Yellow ; promo radio (125 BPM Em) : https://cs.uwaterloo.ca/~dtompkin/music/track/CHRRADIO_388/CHRRADIO_388-07.html
22. Wikipedia – Sicko Mode : https://en.wikipedia.org/wiki/Sicko_Mode ; Hooktheory : https://www.hooktheory.com/theorytab/view/travis-scott/sicko-mode
23. Hooktheory – FE!N : https://www.hooktheory.com/theorytab/view/travis-scott/fein-ft-playboi-carti ; Melodics : https://melodics.com/learn-to-play/fe-n-by-travis-scott-on-drums
24. SongBPM – Kaaris Zoo : https://songbpm.com/@kaaris/zoo ; Booba DKR : https://songbpm.com/@booba/dkr ; PNL Le monde ou rien : https://songbpm.com/@pnl/le-monde-ou-rien
25. Wikipedia – Hard in da Paint : https://en.wikipedia.org/wiki/Hard_in_da_Paint ; Lex Luger : https://en.wikipedia.org/wiki/Lex_Luger_(music_producer) ; The Fader GEN F : https://thefader.com/2011/12/20/gen-f-lex-luger
26. EDMProd – How to make trap : https://www.edmprod.com/how-to-make-trap/
27. Production Music Live – 3 essential scales : https://productionmusiclive.com/blogs/news/trap-beat-guide-melodies-3-essential-scales-for-making-melodies ; RouteNote – trap melodies : https://create.routenote.com/blog/how-to-make-trap-melodies/
28. Wikipedia – UK drill : https://en.wikipedia.org/wiki/UK_drill ; Drill music : https://en.wikipedia.org/wiki/Drill_music
29. Attack Magazine – Beat Dissected : UK drill : https://www.attackmagazine.com/technique/beat-dissected/uk-drill/
30. Beatstorapon – How to make a drill beat : https://beatstorapon.com/blog/how-to-make-a-drill-beat/ ; Violet Recording – UK drill : https://violetrecording.com/how-to-make-uk-drill-beats/
31. Hooktheory – I Don't Like : https://www.hooktheory.com/theorytab/view/chief-keef/i-dont-like ; Love Sosa : https://www.hooktheory.com/theorytab/view/chief-keef/love-sosa ; promo radio (132 BPM F#m) : https://cs.uwaterloo.ca/~dtompkin/music/track/CHRRADIO_269/CHRRADIO_269-07.html
32. SongBPM – Welcome to the Party : https://songbpm.com/@pop-smoke/welcome-to-the-party ; Wikipedia : https://en.wikipedia.org/wiki/Welcome_to_the_Party_(Pop_Smoke_song) ; Wikipedia – 808Melo : https://en.wikipedia.org/wiki/808Melo
33. SongBPM – Dior : https://songbpm.com/@pop-smoke/dior ; Mixed In Key : https://mixedinkey.com/?p=13797 ; Wikipedia : https://en.wikipedia.org/wiki/Dior_(Pop_Smoke_song)
34. Beatport – Central Cee Doja (70 BPM Ebm) : https://www.beatport.com/track/which-one/20820629 ; SongBPM (instru m.h, 140) : https://songbpm.com/@mh/central-cee-doja-instrumenta----swCxOAadUS
35. SongBPM – Freeze Raël : https://songbpm.com/@freeze-corleone/freeze-rael ; 67 Let's Lurk : https://songbpm.com/@67/lets-lurk ; Gazo INTRO : https://songbpm.com/@gazo/intro ; Gazo A$AP : https://songbpm.com/@gazo/a-ap
36. Wikipedia – Body (Tion Wayne & Russ Millions) : https://en.wikipedia.org/wiki/Body_(Tion_Wayne_and_Russ_Millions_song)
37. Wikipedia – Lo-fi music : https://en.wikipedia.org/wiki/Lo-fi_music ; Nujabes : https://en.wikipedia.org/wiki/Nujabes ; J Dilla : https://en.wikipedia.org/wiki/J_Dilla ; Donuts : https://en.wikipedia.org/wiki/Donuts_(album)
38. Hooktheory – Aruarian Dance : https://www.hooktheory.com/theorytab/view/nujabes/aruarian-dance ; Feather : https://www.hooktheory.com/theorytab/view/nujabes/feather
39. SongBPM – J Dilla Don't Cry : https://songbpm.com/@j-dilla/don-t-cry ; Jinsang Affection : https://songbpm.com/@jinsang/affection ; Joey Pecoraro Finding Parking : https://songbpm.com/@joey-pecoraro/finding-parking ; In Love With a Ghost flowers : https://songbpm.com/@in-love-with-a-ghost/flowers
40. Splice – lo-fi chord progressions : https://splice.com/blog/?p=40762 ; Native Instruments – lo-fi chord progressions : https://www.native-instruments.com/blogs/blog/lo-fi-chord-progressions ; eMastered (ii–V–I) : https://emastered.com/blog/lofi-chord-progressions
41. Wikipedia – Phonk : https://en.wikipedia.org/wiki/Phonk ; Memphis rap : https://en.wikipedia.org/wiki/Memphis_rap ; Three 6 Mafia : https://en.wikipedia.org/wiki/Three_6_Mafia
42. EDMProd – How to make phonk : https://www.edmprod.com/how-to-make-phonk/ ; Native Instruments – Phonk : https://www.native-instruments.com/blogs/blog/phonk-music
43. Hooktheory – Murder in My Mind : https://www.hooktheory.com/theorytab/view/kordhell/murder-in-my-mind ; Close Eyes : https://www.hooktheory.com/theorytab/view/dvrst/close-eyes ; Wikipedia – Murder in My Mind (song) : https://en.wikipedia.org/wiki/Murder_in_My_Mind_(song)
44. SongBPM – Tear da Club Up '97 : https://songbpm.com/@three-6-mafia/tear-da-club-up-97 ; Freddie Dredd Cha Cha : https://songbpm.com/@freddie-dredd/cha-cha
45. Wikipedia – Grime music : https://en.wikipedia.org/wiki/Grime_music ; Pulse X : https://en.wikipedia.org/wiki/Pulse_X ; Eskimo (grime beat) : https://en.wikipedia.org/wiki/Eskimo_(grime_beat) ; Pow! (Forward) : https://en.wikipedia.org/wiki/Pow!_(Forward) ; I Luv U : https://en.wikipedia.org/wiki/I_Luv_U_(Dizzee_Rascal_song) ; That's Not Me : https://en.wikipedia.org/wiki/That%27s_Not_Me_(Skepta_song) ; Shutdown : https://en.wikipedia.org/wiki/Shutdown_(Skepta_song) ; Shut Up : https://en.wikipedia.org/wiki/Shut_Up_(Stormzy_song)
46. SongBPM – Eskimo : https://songbpm.com/@wiley/eskimo ; I Luv U : https://songbpm.com/@dizzee-rascal/i-luv-u ; Fix Up Look Sharp : https://songbpm.com/@dizzee-rascal/fix-up-look-sharp ; Pow : https://songbpm.com/@lethal-bizzle/pow-forward ; Functions on the Low : https://songbpm.com/@ruff-sqwad/functions-on-the-low ; That's Not Me : https://songbpm.com/@skepta/that-s-not-me ; Shutdown : https://songbpm.com/@skepta/shutdown ; Shut Up : https://songbpm.com/@stormzy/shut-up ; Hooktheory – Shutdown : https://www.hooktheory.com/theorytab/view/skepta/shutdown
47. Wikipedia – Jersey club : https://en.wikipedia.org/wiki/Jersey_club ; Tidal – How Jersey Club Won : https://tidal.com/magazine/article/jersey-club-won/1-90611 ; DJ Mag : https://djmag.com/features/endless-evolution-jersey-club ; Vice/Noisey : https://noisey.vice.com/en/article/4x8egj/part-three-of-our-jersey-club-documentary-delves-into-how-the-genre-went-global ; Music Origins : https://musicorigins.org/jersey-club-jersey-trap-and-tang-dance/ ; Recording Arts : https://recordingarts.com/from-newarks-streets-to-the-world-the-birth-and-rise-of-jersey-club/
48. Native Instruments – Jersey club (Tim Cant) : https://www.native-instruments.com/blogs/blog/jersey-club ; Orphiq – What is Jersey club : https://orphiq.com/resources/what-is-jersey-club ; Beatstorapon : https://beatstorapon.com/blog/how-to-make-a-jersey-club-beat/
49. SongBPM – Cookiee Kawaii Vibe : https://songbpm.com/@cookiee-kawaii/vibe-if-i-back-it-up ; Wikipedia – Just Wanna Rock : https://en.wikipedia.org/wiki/Just_Wanna_Rock ; Sticky : https://en.wikipedia.org/wiki/Sticky_(Drake_song) ; Cookiee Kawaii : https://en.wikipedia.org/wiki/Vibe_(If_I_Back_It_Up) ; Boy's a Liar : https://en.wikipedia.org/wiki/Boy%27s_a_Liar
50. Mastering The Mix – loudness par plateforme, titres Spotify 2022 à −8,4 LUFS-I : https://www.masteringthemix.com/blogs/learn/76296773-mastering-audio-for-soundcloud-itunes-spotify-and-youtube ; iZotope – Mastering for streaming : https://www.izotope.com/en/learn/mastering-for-streaming-platforms.html ; iZotope – What are LUFS : https://www.izotope.com/en/learn/what-are-lufs.html
