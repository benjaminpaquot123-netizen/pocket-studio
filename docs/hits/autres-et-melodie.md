# Famille « Autres » (reggaeton, afrobeats, synthwave, pop) : 10 hits par style, écarts avec nos recettes, et spécification « mélodie pro »

Date : 2026-10-23. Base lue (lecture seule, dépôt non modifié, gradle non lancé) : `/home/user/ps-p22` — `StyleRecipes.kt` (recettes), `StarterWizard.kt` (générateur : `startingNotes`, `inLeadRange`, plan des scènes, `sectionsOf`), `StyleMusic.kt` (`withLine`, `withChordRhythm`, `StepLine`), `Notes.kt` (`Note`, `NoteClip`, 24 ticks par double-croche), `Harmony.kt` (`Performance`, `ArpMode`), `WizardMix.kt` (fill toutes les 4 mesures), `docs/CORPS.md`, `docs/STYLE-FIXES.md`, `docs/plan/son.md`, `docs/son/banc/apres-etape2.md`, fin de `docs/RETOURS.md`.

Méthode : je ne peux pas écouter. Tout vient de sources écrites publiques (analyses Hooktheory, Wikipédia, Reverb Machine, Notetracks, Hit Songs Deconstructed, MusicRadar, Loopcloud, Splice, Soundfly, Violet Recording, Audeobox, uJam, EDMProd, Ableton « Making Music », iZotope, Loopcloud, American Songwriter, Music Business Worldwide, Temperley & de Clercq 2013, SongBPM/Beatport). Les chiffres marqués ≈ sont des estimations ou des détections automatiques non recoupées ; « n. t. » = non trouvé. Le quota de recherche web s'est épuisé en cours de route : quelques BPM/tonalités (Love Nwantiti, Last Last, Rush, Accelerated, A Real Hero) n'ont pas pu être recoupés et sont signalés comme tels. Sources numérotées en fin de document.

Rappel du retour de Benjamin (RETOURS.md, 2026-10-23) : « ça ressemble encore trop à de l'amateur, surtout les mélodies synthé ». La partie B répond à ce point ; la partie A situe chaque style de la famille face à ses hits.

---

## Partie A — les quatre styles face à dix hits chacun

### A0. Ce que fait le générateur aujourd'hui (commun aux quatre styles)

Lu dans `StarterWizard.startingNotes` + `StyleMusic.withLine` / `withChordRhythm` :

- **Quatre mesures, un accord par mesure** (`progressions[mood]`, degrés de gamme). Le clip fait 64 pas ; `NoteClip` accepte jusqu'à 128 pas (8 mesures), ce qui laisse la place pour des phrases de 8 mesures.
- **Lead** : `leadPattern` est une cellule de 16 (ou 32) caractères, un par double-croche. Chaque jeton (`R`, `2`…`7`, `O`) est un degré **relatif à la fondamentale de l'accord de la mesure** ; la cellule est **recopiée à l'identique dans chacune des 4 mesures** (`StepLine.barOf`), donc transposée diatoniquement avec l'accord (contour identique à chaque mesure, mélodie « collée » à l'accord). Vélocité : deux niveaux seulement (0,8 ; minuscule = 0,56). Longueur = celle du jeton (1 pas par défaut), aucune variation. Aucune humanisation (le swing du projet décale seulement les doubles-croches impaires). `leadPattern` vide = arpège `ArpMode.UpDown` d'une **triade** (3 notes, 1 octave) à `arpRate`, vélocité fixe 0,65.
- **Même clip dans toutes les sections** : le plan des scènes allume ou éteint une partie, il ne la fait jamais varier (ni octave, ni densité, ni contre-chant, sauf `counter` qui rejoue les accords en coups brefs, jamais une mélodie).
- **Accords** : `chordRhythm` identique chaque mesure, voicing fixe, pas de renversement (`nearShift` déplace l'accord entier d'une octave) ; **basse** : cellule identique chaque mesure.
- **Batterie** : trois vélocités + roulements, fill à la fin de chaque phrase de 4 mesures (`WizardMix.phrase`), motifs `build` possibles dans la 2e section. C'est la seule couche qui « respire ».

Constat transversal : **huit recettes sur les 36 utilisent la même cellule tresillo d'arpège** (`O..5..3.`, `R..3..5.` : trance, trap, drill, reggaeton, afrobeats, phonk, lo-fi, UK garage). C'est un « tell » machine à lui seul (voir B12).

---

### A1. Reggaeton (recette : `reggaeton`, 95 BPM, Si mineur, lead Marimba éteint par défaut)

**1. Dix morceaux repères**

| # | Artiste – Titre (année) | BPM | Tonalité / accords | Source |
|---|---|---|---|---|
| 1 | Daddy Yankee – Gasolina (2004) | 96–97 | Fa (Beatport : Fa majeur ; Hooktheory : Fa phrygien, i–II–vii) | [R1][R2] |
| 2 | Don Omar feat. Lucenzo – Danza Kuduro (2010) | ≈130 | Do majeur ; kuduro/merengue, accordéon | [R3][R4] |
| 3 | Enrique Iglesias feat. Nicky Jam – El Perdón (2015) | ≈90 | La♭ (site d'accords, non recoupé) | [R5] |
| 4 | Luis Fonsi feat. Daddy Yankee – Despacito (2017) | 89 | Si mineur ; Bm–G–D–A (i–VI–III–VII) | [R6][R7] |
| 5 | J Balvin & Willy William – Mi Gente (2017) | 105 | Mi mineur (Beatport) ; moombahton | [R1][R8] |
| 6 | Daddy Yankee – Dura (2018) | ≈95 | Ré♭ majeur (SongBPM) | [R9] |
| 7 | Daddy Yankee & Snow – Con Calma (2019) | 94 | Sol♯ mineur ; remake d'« Informer », parti d'une boucle de batterie | [R10][R11] |
| 8 | Karol G & Nicki Minaj – Tusa (2019) | 101 | Ré majeur ; piano + cordes, prod. Ovy on the Drums | [R12][R13] |
| 9 | Bad Bunny & Jhay Cortez – Dákiti (2020) | 110 | Mi mineur ; « riff de synthé tranchant, sons électroniques ondulants, beats lourds » (Tainy) | [R14][R15] |
| 10 | Bad Bunny – Tití Me Preguntó (2022) | 104–107 | Fa mineur ; intro i–VII–VI–V ; dembow dominicain + changement de beat | [R16][R17] |
| + | Bad Bunny feat. Chencho Corleone – Me Porto Bonito (2022) | ≈92 | Ré♭ mineur ; « du perreo actuel vers un beat de fête de rue old school » | [R18][R19] |

**2. Ce que les dix ont en commun**

- **Tempo** : 89 → 110, médiane ≈ 96 ; les guides disent 90–100, « 94 par défaut » [R20]. Danza Kuduro (130) est l'exception kuduro.
- **Tonalités / accords** : mineur dominant (Bm, Em, Fm, G♯m), quelques majeurs (Tusa, Dura, Gasolina). Boucles de 4 accords simples : i–VI–III–VII (Despacito), i–VII–VI–V (Tití), ou vamp de 2–3 accords (Gasolina i–II–vii, mélodie sur **3 demi-tons** seulement [R2]). Triades, pas d'extensions ; guitare/cuatro, piano ou pads de synthé [R6][R12].
- **Structure** : verse–chorus–bridge hérité du hip-hop, hook répété [R21] ; guides : intro / couplet / pré-refrain / refrain (le « drop » : dembow complet + 808) / pont / dernier refrain, batterie allégée dans l'intro et les couplets, timbales en fin de phrase, filtre qui s'ouvre vers le refrain [R20]. Despacito : cuatro seul en intro, guitare à l'entrée du chant, ralenti sur « despacito » au début de chaque refrain, side-chain au kick [R6].
- **Mélodie / hook** : vocale, **ambitus étroit** (Despacito refrain Fa♯3–La4 = 15 demi-tons, 76 % de notes d'accord, 0,37 temps par note, « conjoint, répétitif, syncopes marquées » [R7] ; Gasolina 3 demi-tons, 62 % [R2]). Hook instrumental = **riff de 2–4 mesures répété** : cuatro/guitare (Despacito), riff de synthé (Dákiti), riff d'intro de Tití (Do4–Fa5, 96 % de notes d'accord, 0,40 temps/note, « conjoint, répétitif » [R17]), sample vocal (Mi Gente). Les guides : « hook chantable et répétitif, motif de 2–4 mesures ; l'instrumental soutient le chanteur » [R20].
- **Basse** : 808 courte et rythmique, calée sur le kick (pas de longs glissés trap), fondamentales, « octave ou fondamentale → quinte », glissés rares, staccato [R20][R22].
- **Batterie (dembow)** : 3+3+2 (tresillo) [R23][R21]. Sur 16 pas : caisse claire/rim aux pas 3, 6, 11, 14 (0-based), kick sur 0 et 8 ou sur chaque temps ; Despacito : güira + guache en continu, cowbell, timbales, charley synchronisé sur la güira [R6] ; notes fantômes de caisse claire « un peu aléatoires et changeantes », sections où la batterie s'arrête, dembow transféré à d'autres instruments dans les passages légers [R22] ; swing 8–16 % sur charleys/percus, claps et congas légèrement en retard [R20]. Depuis 2018, « dembow minimaliste » plus syncopé (Te Boté) [R21].
- **Sound design** : kick rond sans attaque, caisse claire courte 1–3 kHz, 808 saturée, güira « l'élément le plus important », plucks, marimba, « piano reggaeton » (stab), vocal chops filtrés [R20][R22] ; Dákiti : pads atmosphériques + basse profonde + flûte sifflée [R14].
- **Voix → instrumental** : le hook vocal est remplacé par le riff (cuatro/synthé) qui existe déjà dans les vrais morceaux, en intro et en post-refrain ; ad-libs = petites réponses de percussions (timbales) dans les trous.
- **Sonie** : pas de mesure publiée propre au reggaeton ; normes pop/hip-hop ≈ −11 LUFS intégré, crête vraie ≤ −1 dBTP [L1][L2] ; plateformes −14 (Spotify/YouTube/Amazon/Tidal), −15 Deezer, −16 Apple [L1].

**3. Écarts avec notre recette, classés par impact audible**

Recette : kick `X...X...X...X...`, snare `...X..X....X..X.`, hat `..x...x...x...x.`, perc timbales `............................x2x3`, güira `x.xxx.xxx.xxx.xx` (refrain), open hat `..............x.` ; `swing = 0.0` ; `bassPattern = "R-----R-R----/5-"`, 808 ; `chordRhythm = "...X..X....X..X."` Orgue Triad oct 3 ; `leadPattern = "R..3..5.O..5..3."` Marimba oct 5 ; `parts = {Bass, Pad}` ; Dark = i–VI–III–VII.

1. **Pas de hook instrumental** (lead éteint, et quand il est coché c'est un arpège tresillo d'une mesure répété 4×). Changement : lead allumé par défaut, présent en Intro et Refrain (aperçu + hook), **riff de 2 mesures** question (mes. 1) / réponse (mes. 2, finit sur 5 ou 7 de l'accord suivant), silence ≥ 1 temps en fin de mes. 2, densité 0,35–0,6 temps/note, ≥ 90 % notes d'accord (Tití), centre ≈ Do5. Son : pluck/guitare nylon plutôt que marimba. Mesure : métriques B13 (M2, M4, M9) vertes.
2. **Accords uniquement en coups sur le dembow** → rien ne tient. Hits : guitare strummée en tresillo + nappe/pad tenu. Changement : `chordRhythm = "X--x--X-X--x--X-"` (coups de 3, 3, 2 pas, le 2e plus doux) et nappe douce tenue si un 2e son d'accords est possible ; sinon `voicing = Seventh` pour épaissir. Mesure : ratio notes tenues ≥ 50 % de la mesure.
3. **Swing 0** : mettre `swing = 0.08` (8–16 % conseillé) et claps/congas 1–2 ticks en retard (B8).
4. **Batterie figée** : ajouter notes fantômes `...X..X.o..X..X.` (o = 0,35) qui changent d'une phrase à l'autre, güira dès le Couplet (elle est continue dans les hits), **2 mesures sans batterie** avant le dernier Refrain, roulement de timbales à chaque fin de phrase de 4 mesures (déjà toutes les 2 mesures : garder mais plus doux).
5. **Basse** : raccourcir et coller au kick : `R---..R-R---..5-` (bar 1) / `R---..R-R---./O-` (bar 2, un seul glissé sur 2 mesures). Mesure : longueur médiane ≤ 4 pas, ≤ 1 glissé par 2 mesures.
6. **Progressions** : garder Dark (Despacito) ; Future → i–VII–VI–V (`listOf(0, 6, 5, 4)`, Tití) ; Happy → I–V–vi–IV ; Dreamy → I–vi–IV–V. Tempo 95 → 96 (médiane).
7. **Forme** : intro = riff seul 4 mesures (cuatro), refrain avec impact + güira + open hat (déjà), 1er refrain avant 45 s (ici 8 + 16 mesures à 95 BPM = 60 s : intro 4 mesures).

---

### A2. Afrobeats (recette : `afrobeats`, 105 BPM, La majeur, lead Marimba au refrain)

**1. Dix morceaux repères**

| # | Artiste – Titre (année) | BPM | Tonalité / accords | Source |
|---|---|---|---|---|
| 1 | Wizkid – Ojuelegba (2014) | ≈112 | Do majeur ; prod. Legendury Beatz ; « afropop gospel » | [A1][A2] |
| 2 | Tekno – Pana (2016) | ≈73 (détection douteuse, probablement moitié/double) | Fa♯ mineur | [A3] |
| 3 | Davido – Fall (2017) | 106 | Fa majeur ; prod. Kiddominant ; sample « Dun Talking » | [A4][A5] |
| 4 | Burna Boy – Ye (2018) | 101 (SongBPM détecte 202 = double) | Fa mineur ; beat = kick + « piano synths » + quelques snares, fait en < 2 h (Phantom) | [A6][A7] |
| 5 | CKay – Love Nwantiti (2019) | n. t. (≈ 93) | n. t. ; prod. CKay & Tempoe ; hit TikTok 2021 | [A8] |
| 6 | Wizkid feat. Tems – Essence (2020) | 104 | Do majeur ; Legendury Beatz + P2J (« plus de percussions et une 2e ligne de basse pour la dynamique ») ; Tems a écrit tout le hook ; « accords doux, syncopes voletantes » | [A9][A10][A11] |
| 7 | Fireboy DML – Peru (2021) | 100 | n. t. ; prod. Shizzi ; fait « en moins d'une heure » | [A12][A13] |
| 8 | Rema – Calm Down (2022) | 107 | Si majeur ; **vamp de 2 accords E–B (IV–I)** ; chant Ré♯4–Fa♯5 ; mélodie d'abord, paroles ensuite (Andre Vibez) ; cordes ajoutées par London | [A14][A15][A16] |
| 9 | Burna Boy – Last Last (2022) | n. t. (≈ 108) | sample Toni Braxton « He Wasn't Man Enough » ; prod. Chopstix et al. | [A17] |
| 10 | Ayra Starr – Rush (2022) | n. t. (médiane Beatport de l'artiste 96) | prod. Hoops & Andre Vibez ; « afro-pop avec charleys trap » | [A18] |

**2. Ce que les dix ont en commun**

- **Tempo** : 100–112 pour les vérifiés ; guides : 100–118, « 105–110 » [A19], 95–115, 108 [A20].
- **Tonalités / accords** : majeur très fréquent (Do, Fa, Si, Do) ; progressions courtes, souvent **2 accords** (Calm Down IV–I) ou I–V–vi–IV ; triades simples, guitare claire staccato + piano électrique/« bell pad » 8–10 dB sous la guitare [A19][A20]. Hooktheory Calm Down : 70 % notes d'accord, 100 % diatonique, tension mélodie/accord 0 dans le couplet et 72 dans le refrain (le refrain monte sur des notes hors accord) [A15].
- **Structure (guide détaillé)** : intro 4–8 (guitare seule puis percus une à une) / couplet 16 / pré-hook 4 (percus plus denses + nouvel élément) / hook 8 / couplet 2 16 / hook 8 / pont 8 (réduit au log drum + guitare puis percus qui reviennent) / hook final 8 / outro 4 [A20]. Violet : intro percus + accords, couplet réduit à kick + basse + percus légères, refrain plein, transitions reverse cymbal / fill / roulement, **coupures de 2 mesures** [A19].
- **Mélodie / hook** : guitare électrique claire porte le hook, phrases courtes qui bouclent (« la répétition est une qualité »), runs de 2–3 notes en doubles-croches entre les accords, **question–réponse à trois niveaux** : mesure (guitare mes. 1–2, log drum répond mes. 3–4), temps (clave sur le temps, congas à contretemps), section (couplet et hook ont des réponses différentes) [A20][A21]. Marimba/kalimba/pluck/guitare muette/EP pour la mélodie [A19]. Ambitus vocal Calm Down : couplet Si4–Sol♯5, refrain Do♯5–Do♯6 (le refrain monte) [A15]. Hooks vocaux pentatoniques (Liveschool : refrain sur la pentatonique = gamme sans 2 et 6, appels ascendants, réponses non linéaires, lignes 1 et 3 = appel, 2 et 4 = réponse) [M9].
- **Basse** : ronde, « bassiste » plus que 808 : fondamentale sur le 1 de chaque accord, note d'approche sur le 3, runs occasionnels de 2–3 doubles-croches, ambitus Do1–Mi2, pas de glissé ni de sustain, side-chain 3–4 dB au kick ; variante 808 mélodique avec courts portamentos ; log drum (amapiano) qui rebondit entre 2–3 notes dans les trous du kick [A19][A20].
- **Batterie** : kick sur 1, « et » de 2, 3 (75–85 %), « et » de 4 une mesure sur deux ; rim/clap doux sur 2 et 4, ghost à 40 % sur la double-croche avant le 4 une mesure sur deux ; charley croches (70–80 % / 50–60 %), **open hat sur le « et » de 2** ; shaker en doubles-croches avec cycle de vélocité 60/40/70/40 ; **clave/woodblock 3-2** (1, « et » de 1, « et » de 2, 4, « et » de 4) ; congas accentuées sur 2 et 3 avec fantômes 35–50 % ; rim dans les trous ; log drum sur « et » de 1, 2, « et » de 3 [A20]. Swing 8–20 % sur charleys/shakers/percus, « la quantification stricte est le n° 1 de ce qui rend un afrobeats raide » [A19][A21]. Wikipédia : clave 3-2 ou 2-3 courante, « le beat est un élément de premier plan » [A22].
- **Sound design / mix** : percus chacune sur sa place (pan L20–R15), bus percus compressé 2:1, guitare passe-haut 200 Hz + delay 1/16 à 15–20 % + chorus léger, log drum 150–400 Hz, master −10 à −12 LUFS, ceiling −0,3 dB [A20] ; mixes « propres, punchy, chauds », voix devant, effets automatisés plutôt que noyés [A19].
- **Voix → instrumental** : le hook guitare est déjà le hook instrumental ; ad-libs = réponses de pluck/synthé et de log drum ; doublage octave + pan des ad-libs pour la largeur [A19].
- **Sonie** : −10 à −12 LUFS cible de guide [A20] ; plateformes −14/−16 [L1].

**3. Écarts avec notre recette**

Recette : kick `X.....X.X.....x.`, rim `....x.......x...`, shaker `XoxoXoxoXoxoXoxo`, congas 2 mesures ; `swing = 0.15` ; basse `R-----R-R-.5-O-.` (pack afrobeats:bass) ; `chordRhythm = "X-.X-.X--.X-.X-."` Piano électrique Seventh oct 3 ; `leadPattern = "5.O.5.3...5.3.R."` Marimba oct 5, `parts = {Bass, Pad, Lead}`, `signature = Pad` ; Happy = I–V–vi–IV.

1. **Le hook n'est pas la signature et n'a pas de réponse**. Hits : la guitare est le hook, dès l'intro, en question–réponse. Changement : `signature = Lead` (ou lead en Intro + Refrain), riff de **2 mesures** (appel mes. 1 ascendante, réponse mes. 2 descendante finissant sur la tierce ou la quinte, silence ≥ 1 temps), pentatonique (1 2 3 5 6) pour le refrain, runs de 2–3 doubles-croches vers l'accord suivant une mesure sur deux ; son : « Guitare » si le pack en a une, sinon pluck + delay 1/16 à 15–20 % (déjà 0,15). **Contre-chant** : `counter` ne sait jouer que les accords en coups ; il faut une réponse mélodique (B9) dans les mesures 3–4 (log drum / pluck grave).
2. **Batterie trop simple** : ajouter clave 3-2 `x..x..x...x.x...` (Perc2, voice woodblock/cowbell doux), open hat `......x.........`, ghost rim 40 % `....x......o.x..` une mesure sur deux (32 pas), congas accentuées 2 et 3 avec fantômes. Mesure : ≥ 5 lignes au Refrain, ≥ 3 vélocités distinctes sur shaker (déjà : 1,0/0,35/0,6).
3. **Basse** : supprimer l'octave `O` (trop 808) : `R-----R-R---5-6-` (approche 5→6 vers la fondamentale suivante) / variante 2e mesure `R-----R-R-..R5-6` (run) ; `release` court, pas de glissé ; pompe 3–4 dB (`StyleFx("bass", insert = "PUMP", params = listOf(0.3f, 0.35f, 0f))`).
4. **Accords** : garder le piano électrique en coups ; ajouter pad tenu 8–10 dB dessous (un 2e son d'accords n'existe pas dans le modèle → à noter comme besoin : « couche nappe discrète »). Progressions : ajouter le vamp IV–I (`Mood.Happy to listOf(0, 4, 5, 3)` reste, `Mood.Dreamy to listOf(3, 0, 3, 0)`), tempo 105 ✓.
5. **Forme** : couplet réduit à kick + basse + shaker (motifs `build` : congas et clave arrivent au Refrain), pré-hook 4 mesures (percus denses : utiliser l'impact + un `build` plus dense sur la dernière phrase), pont 8 mesures « log drum + guitare » avant le dernier refrain (forme Song sans Break aujourd'hui : voir B14 ; en attendant, Break possible seulement en forme Club).
6. **Mix** : cible −10 à −12 LUFS, percus pannées (fait), guitare passe-haut 200 Hz.

---

### A3. Synthwave (recette : `synthwave`, 105 BPM, La mineur, lead = arpège)

**1. Dix morceaux repères (+ ancêtres)**

| # | Artiste – Titre (année) | BPM | Tonalité / accords | Source |
|---|---|---|---|---|
| 1 | Kavinsky – Nightcall (2010) | 92 | La mineur ; couplet Am–G/B–F–Dm (i–VII6–VI–iv), refrain F–G–Em–F ; 77 % notes d'accord ; prod. avec Guy-Manuel de Homem-Christo | [S1][S2][S3] |
| 2 | College feat. Electric Youth – A Real Hero (2010) | ≈86 (sources contradictoires 86/112) | ≈ Fa majeur F–Dm–B♭–C (transcription amateur) | [S4][S5] |
| 3 | M83 – Midnight City (2011) | 105 | Si mineur ; VI–i–VII–iv ; riff = voix distordue ; **36 % notes d'accord** (riff très tendu), chant Si3–Fa♯5, sax en outro | [S6][S7] |
| 4 | Miami Nights 1984 – Accelerated (2012) | n. t. | n. t. | — |
| 5 | Perturbator – Future Club (2014) | 100 | Fa mineur | [S8] |
| 6 | Carpenter Brut – Turbo Killer (2015) | 147 | Fa mineur | [S9] |
| 7 | Gunship – Tech Noir (2015) | 86 | Si♭ majeur | [S10] |
| 8 | The Midnight – Los Angeles (2014) / Shadows (2017) | ≈116 / 162 (= 81 half-time) | n. t. (Sunset : ≈114, Fa ou Do majeur selon source) | [S11][S12] |
| 9 | FM-84 feat. Ollie Wride – Running in the Night (2016) | ≈133 (détection douteuse) | Ré mineur | [S13] |
| 10 | Kyle Dixon & Michael Stein – Stranger Things (2016) | 84 | Mi mineur ; arpège de **Do maj7 (VI)** en doubles-croches continues, joué à la main, propulsion par filtre et couches | [S14][S15] |
| 11 | The Weeknd – Blinding Lights (2019) | 171 (≈ 85,5 ressenti) | Fa mineur (Hooktheory : Fa dorien) ; Fm–Cm–E♭–B♭ (i–v–VII–IV) ; 59 % notes d'accord ; Max Martin / Oscar Holter | [S16][S17][S18] |

**2. Ce que les dix ont en commun**

- **Tempo** : 84–116 en ressenti (Stranger Things 84, Tech Noir 86, Nightcall 92, Future Club 100, Midnight City 105, The Midnight ≈114–116) ; « 80–118, beaucoup autour de 100–110 ; 128–140 pour l'outrun énergique » [S19][S20] ; Turbo Killer 147 et Blinding Lights 171 (double-temps).
- **Tonalités / accords** : mineur quasi systématique (La m, Si m, Fa m ×3, Ré m, Mi m) ; boucles de 4 accords **avec le VII et le VI** : i–VII–VI–iv (Nightcall), VI–i–VII–iv (Midnight City), i–v–VII–IV (Blinding Lights) ; majeur « dreamwave » I–vi–IV–V (A Real Hero). Basse en slash chord (G/B) pour l'urgence [S1]. « Harmonie simple, quelques accords mineurs en boucle, l'arpège et la mélodie portent le morceau » [S20].
- **Structure** : Blinding Lights (Notetracks, temps en secondes) : intro 0:00–0:17 (synthé sombre qui ne revient jamais, **hook de synthé en teaser à 0:05**, batterie 0:11, riser 0:14) ; couplet 1 ≈ 22 s ; pré-refrain 11 s (tout sauf la batterie coupé pour le riser) ; refrain 1 ≈ 29 s ; **post-refrain = le hook de synthé** ; couplet 2 moitié plus court avec le filtre de l'arpège qui s'ouvre ; pont avec tambourin et question–réponse vocale ; outro sans batterie où **voix et hook de synthé jouent ensemble pour la première fois** ; « un nouvel élément toutes les ~8 mesures », chaque transition différente (riser, cymbale, sinus montant, reverse cymbal) [S17]. HSD : le hook de synthé entre **dans les 8 dernières mesures de l'intro**, revient aux breaks instrumentaux et en outro [S18]. Guides : « construire comme une musique de film : intro atmosphérique, puis basse + arpège, puis batterie, puis la mélodie ; retenir des parties pour que le payoff arrive, ne pas remplir chaque mesure » [S20].
- **Mélodie / hook** : (a) **l'arpège-ostinato** : Stranger Things = Do maj7 (4 notes) en doubles-croches, « à la fois mélodie et ostinato », évolution par automation de filtre [S14] ; Nightcall = motif de **8 notes** qui dessine les accords (Juno pluck, 3 couches dont une 2 notes plus douce et une sub-octave mono) [S1] ; (b) **le lead chantable** : Blinding Lights = hook de **6 mesures** : mes. 1–2 et 3–4 = deux phrases similaires **ascendantes**, mes. 5–6 = deux phrases similaires **descendantes** ; ses deux dernières phrases préfigurent les lignes 2 et 4 du refrain vocal, ses deux premières les lignes 2–4 du pré-refrain ; le lead « suit le même motif tout le morceau, **en s'arrêtant à la fin de chaque 4 mesures** » [S18][S21] ; motifs A.M.P. (alternance de deux hauteurs voisines) dans les couplets, E.O.L.A. (changement en fin de ligne) [S18]. Midnight City : riff de voix saturée à 36 % de notes d'accord → une tension assumée, pas un arpège. Guides : « une mélodie forte et chantable, un seul lead sous les projecteurs à la fois », lead scie/carré avec léger portamento, couche saw + pulse + sub-octave, détunés de quelques cents et pannés [S20].
- **Basse** : croches régulières (Blinding Lights, Nightcall) ; Nightcall couplet : Prophet mono, 2 scies à l'octave, filtre 727 Hz, LFO 5,29 Hz sur pitch et filtre, piano qui double les fondamentales à l'octave [S1] ; « bounce d'octave » = arpégiateur sur une note tenue, octave +1 (Blue Monday) [S22] ; sub sinus + couche brillante légèrement distordue [S20] ; pompe au kick [S20][S22].
- **Batterie** : LinnDrum/707/Roland, 4/4 simple « comme une boîte des années 80 », kick sur chaque temps, **caisse claire à reverb gatée** (reverb forte + gate : ≈ 0,5 s de tenue puis fermeture rapide ; AMS RMX16 NonLin2 sur les disques des années 80) [S23][S24][S25] ; Blinding Lights : kick/snare électro façon TR-707, charley croches dans le refrain seulement, clap à contretemps à la fin de chaque 4e mesure, tambourin remplace le charley au pont [S21][S17] ; toms électroniques, claps [S20]. LinnDrum : 1982–85, échantillons 8 bits 28–35 kHz [S26].
- **Sound design** : Juno chorus I/II, scies détunées (≈12 cents), unisson, EQ 3–5 kHz sur les plucks, delay léger, Redux, saturation générale « tout le morceau est légèrement distordu » [S1] ; pads cordes attaque 44 ms ; delay en croche pointée sur les plucks, chorus, tape/VHS [S22][S20] ; filtre de l'arpège fermé en couplet, ouvert en pré-refrain [S17].
- **Voix → instrumental** : le genre est « principalement instrumental » [S19] ; le hook de synthé joue le rôle du refrain (Blinding Lights le place en intro, post-refrain et outro).
- **Sonie** : masters électroniques ≈ −11 LUFS [L2] ; notre `docs/plan/son.md` vise −8 ±1 pour synthwave/trance (cible interne).

**3. Écarts avec notre recette**

Recette : kick `X...X...X...X...`, snare `....X.......X...` (kit linndrum, reverb 0,5), clap, hat `o.x.o.x.o.x.o.x.`, toms `............................XxXx`, open hat Refrain ; `bassPattern = "R.R.R.R.R.R.R.R."` Basse analogique (PUMP) ; `chordRhythm = "X---------------"` Nappe chaude Triad oct 3 (wide_alive, PUMP) ; `leadPattern = ""` → arpège UpDown triade à 1/16, `leadOctave = 5`, Arpège, delay 0,25, `signature = Lead` ; Dark = i–VI–III–VII, Happy = I–I–V–IV.

1. **L'arpège est tout le lead, et c'est une triade sur une octave qui monte-descend pareil chaque mesure.** Hits : Do maj7 (4 notes), motifs de 8 notes sur 2 octaves, accent et filtre qui bougent. Changement : `voicing` de l'arpège → `Seventh` ou `Power` (quinte + octave) sur **2 octaves** (étendue ≥ 19 demi-tons), motif fixe de 8 notes plutôt que UpDown (ex. `R 5 O 3' 5' O' 5' 3'`), **accent** (vélocité +0,15) sur 1 note sur 4, gate ≈ 60 %, et **balayage de filtre par section** via `sweeps` (`Sweep("lead", mapOf(0 to (0.3f to 0.45f), 1 to (0.45f to 0.8f), 2 to (1f to 1f), 3 to (0.4f to 0.9f)))` : la cible `lead` est acceptée par `withSweepFilters`). Mesure : M17.
2. **Pas de mélodie chantable au-dessus de l'arpège.** Changement : une piste **hook** (B14) de 4–6 mesures sur le modèle Blinding Lights (2 phrases ascendantes similaires + 2 descendantes similaires, silence en fin de 4 mesures), son « Lead carré » ou « Lead » + léger portamento, en Refrain, en aperçu dans les 4 dernières mesures de l'Intro et seul en outro. Mesure : M1–M7.
3. **Caisse claire sans gate.** `reverb = 0.5` donne une traîne, pas un « gated snare ». Changement : soit un effet « Gate » en insert après la réverbe (tenue ≈ 150–300 ms, fermeture rapide), soit un échantillon « snare gatée » pré-rendu dans le pack linndrum (reverb 2 s coupée à 200 ms). Mesure : queue à −40 dB ≤ 300 ms après le coup.
4. **Progressions** : Dark → i–VII–VI–iv (`listOf(0, 6, 5, 3)`, Nightcall) ; Future → i–v–VII–IV (`listOf(0, 4, 6, 3)`, Blinding Lights) ; Happy → I–vi–IV–V (`listOf(0, 5, 3, 4)`, A Real Hero) ; Dreamy → VI–i–VII–iv (`listOf(5, 0, 6, 3)`, Midnight City). Tempo 105 ✓ (ou 100).
5. **Basse** : garder les croches ; variante « bounce » en Refrain sur 32 pas : `R.R.R.R.R.R.R.R.R.R.R.R.R.O.R.O.` ; vibrato lent (LFO ≈ 5 Hz faible) si le synthé le permet.
6. **Forme** : Intro = nappe + arpège (filtre fermé) sans kick 8 mesures, Couplet = + basse + batterie, Refrain = + hook + open hat ; un élément nouveau toutes les 8 mesures (charley croches, clap de fin de 4e mesure `..............x.` sur 64 pas, tambourin) ; outro = hook + nappe sans batterie (la forme Song joue l'Intro en outro : OK si l'Intro contient le hook en aperçu).
7. **Sound design** : unisson stéréo / chorus (chantier moteur, son.md étape 4), saturation légère du bus, delay croche pointée sur l'arpège (0,25 déjà : vérifier le temps).

---

### A4. Pop (recette : `pop`, 116 BPM, Mi majeur, lead Pluck au refrain)

**1. Dix morceaux repères**

| # | Artiste – Titre (année) | BPM | Tonalité / accords | Source |
|---|---|---|---|---|
| 1 | Ed Sheeran – Shape of You (2017) | 96 | Do♯ mineur ; i–iv–VI–VII ; riff de marimba en tresillo ; kick seulement au 1er refrain (~1 min), basse encore après ; 55 % notes d'accord | [P1][P2][P3] |
| 2 | Billie Eilish – bad guy (2019) | 135 (132–138 ; pont à 60) | Sol mineur ; hook de basse i–iv–V ; turnarounds de 2 mesures sans accompagnement ; 73 % notes d'accord ; refrain Sol3–Ré4 (**7 demi-tons**) | [P4][P5] |
| 3 | The Weeknd – Blinding Lights (2019) | 171 | Fa mineur ; i–v–VII–IV (voir A3) | [S16][S17][S18] |
| 4 | Dua Lipa – Don't Start Now (2019) | 124–125 | Si mineur ; couplet iv–i–VI–III–VII (Em7 Bm7 G D), refrain i7–III–iv7–v7–VI7 ; basse funk Scarbee MM ; intro 4 mesures ; 58 % notes d'accord | [P6][P7][P8] |
| 5 | Angèle – Balance ton quoi (2018) | 116 | Do majeur (SongBPM ; tablature en Ré mineur) ; piano + percussions ; prod. Tristan Salvati | [P9][P10] |
| 6 | Dua Lipa – Levitating (2020) | 103 | Si mineur ; i7–v7–iv7 ; refrain La3–Fa♯4 (9 demi-tons), 73 % notes d'accord, 0,50 temps/note | [P11] |
| 7 | Harry Styles – As It Was (2022) | 174 (≈ 87 ressenti) | La majeur ; ii–V–I–IV (comparé à Take On Me) ; riff Moog One instrumental La4–Mi5 (7 demi-tons, complexité 12) ; 42 % notes d'accord global | [P12][P13] |
| 8 | Taylor Swift – Anti-Hero (2022) | 97 | Mi majeur ; IV7–I–V–vi7 ; OB-8, LinnDrum + trémolo ; refrain Mi3–La4 ; 59 % notes d'accord | [P14][P15] |
| 9 | Miley Cyrus – Flowers (2023) | 118 | La mineur ; i–iv–VII–III ; refrain La3–La4 ; 70 % notes d'accord, 0,87 temps/note | [P16] |
| 10 | Sabrina Carpenter – Espresso (2024) | 104 (swing léger de doubles-croches) | La mineur ; iv9–v7–i9 (Dm7 Em7 Am7) ; refrain Sol3–Mi4 (9 demi-tons), **83 % notes d'accord** ; construit tempo → accords → boucle de guitare → basse | [P17][P18][P19] |
| + | Chappell Roan – Good Luck, Babe! (2024) | ≈117 | Ré majeur ; synthés 80s + cordes ; chant La3–Fa♯5 | [P20] |

**2. Ce que les dix ont en commun**

- **Tempo** : 96–135, médiane ≈ 110 ; deux hits en double-temps (171/174, ressentis ≈ 86).
- **Tonalités / accords** : moitié mineur, moitié majeur ; **4 accords en boucle**, souvent avec 7e ou 9e sur claviers (Anti-Hero IV7–I–V–vi7, Levitating i7–v7–iv7, Espresso iv9–v7–i9, Don't Start Now) ; rotations de I–V–vi–IV (Despacito vi–IV–I–V, Anti-Hero IV–I–V–vi) ; Hooktheory : 99–100 % diatonique partout.
- **Structure** : intro 9–14 s en moyenne (14,1 s en 2011, 9 s au T3 2014) [P21][P22] ; **1er refrain à 0:33 en moyenne (T3 2014), idéal ≤ 45 s** [P22][P21] ; durée 3:07 (2019) → 3:35 (T1 2024), intros « multi-parties » 36 % en 2024 [P23] ; Don't Start Now : intro 4 mesures puis couplet nu (basse, batterie, voix) [P8] ; Shape of You : kick retenu jusqu'au 1er refrain, basse encore plus tard, « de longues sections à 2–3 instruments » [P3] ; bad guy : turnarounds de 2 mesures qui coupent tout pour le titre, break instrumental, pont en demi-temps [P5] ; Max Martin : 2e refrain plus gros, dernier le plus gros, contraste net entre sections adjacentes avec 1–2 éléments qui passent, mélodie du refrain « prévisualisée » dans le couplet [P24].
- **Mélodie / hook (vocal)** : ambitus par section **7–17 demi-tons** (bad guy 7, Levitating 9, Espresso 9, Flowers 12, Anti-Hero 17, Blinding Lights 10–14) ; **55–83 % de notes d'accord**, 0,37–1,11 temps par note ; « conjoint, consonant, syncopes lâches » presque partout (Hooktheory) ; motifs A.M.P. (deux hauteurs voisines alternées : Blinding Lights, Say So, Dance Monkey, Sucker) et E.O.L.A. [S18] ; note la plus haute gardée pour la 2e moitié de section [P24] ; 4 lignes appel/réponse (1 et 3 = appel ascendant, 2 et 4 = réponse) [M9].
- **Hook instrumental** : riff court (As It Was : 7 demi-tons, complexité 12 ; Shape of You marimba tresillo ; Blinding Lights 6 mesures) placé **en intro et en post-refrain**, répété tel quel [S18][P12][P1].
- **Basse** : fondamentales calées sur le kick (Anti-Hero « rubbery bass »), funk à octaves (Don't Start Now), hook de basse synthé (bad guy i–iv–V), sub + 808 (bad guy) [P4][P8][P15].
- **Batterie** : backbeat 2/4 clap + caisse, kick 1/3 + anticipation (Anti-Hero, Flowers) ou **4/4 disco** (Don't Start Now, Levitating, Blinding Lights 707), snaps dans ≈ 1/3 des top 10 pop (bad guy) [P5], LinnDrum/707 pour la couleur [P15][S21], percussions ajoutées au refrain (cowbell, tambourin, shaker).
- **Sound design** : piano + pads doublés par des synthés et basses synthé tenues (Don't Start Now) [P8] ; « tout est empilé en plusieurs couches avec réverbes, pans, filtres différents » (Espresso, 10–20 pistes de voix) [P18] ; OB-8 « orgue de stade » [P15].
- **Voix → instrumental** : prendre le hook vocal comme lead (Blinding Lights outro : lead + voix à l'unisson) ; le riff instrumental existant (marimba, Moog) sert d'intro/post-refrain ; ad-libs = petites réponses de pluck en fin de ligne.
- **Sonie** : pop ≈ −11 LUFS [L2], hits souvent plus forts (≈ −9, anecdotique) [L3] ; TP ≤ −1 dBTP ; −12 LUFS « sera légèrement baissé » par les plateformes [L1].

**3. Écarts avec notre recette**

Recette : kick `X.....x.X.......`, snare+clap 2/4, hat `x.o.x.o.x.o.x.o.`, open hat fin, shaker et tambourin au Refrain ; `swing = 0.0` ; basse `R-----r-R-----5-` Basse ronde ; `chordRhythm = "X-.X-.X--.X-.X-."` Piano FM Triad oct 4 ; `leadPattern = "5-3-R-3-5---O-5-"` Pluck oct 5 (Refrain) ; Happy = I–V–vi–IV, Dark = i–III–VI–VII ; forme Song 8/16/16/16/16/8.

1. **Le lead est un arpège brisé (5-3-1-3-5…O-5) qui change de hauteur avec chaque accord** : aucun hit ne fait ça. Changement : hook de **4 mesures** (2 phrases similaires + 1 réponse + fin longue), ambitus 7–12 demi-tons, 60–85 % notes d'accord, A.M.P. autorisé (2 hauteurs alternées sur une mesure), note la plus haute en 2e moitié, dernière note ≥ 2 temps sur un degré stable ; présent en **Intro (aperçu 4 mesures) + Refrain + post-refrain** (B11). Mesure : M1–M7, M16.
2. **Accords sans 7e/9e, à l'octave 4** : `voicing = Seventh`, `chordOctave = 3` (CORPS : trou 250–500 Hz), et nappe tenue sous le piano (besoin modèle : 2e couche d'accords) ; `Dark → listOf(0, 3, 5, 6)` (Shape of You i–iv–VI–VII) ou `listOf(0, 3, 6, 2)` (Flowers).
3. **Basse trop statique** : `R---r-O-R---r-5-` (octave sur le « et » de 2, quinte en fin de mesure) pour la pop dansante ; conserver la ronde pour les ballades. Mesure : ≥ 3 hauteurs distinctes par 4 mesures.
4. **Kick** : variante 4/4 au Refrain (`pattern = "X...X...X...X..."`, `build = "X.....x.X......."` pour le Couplet) : le modèle `Line.build` joue déjà un motif différent dans la 2e section.
5. **Forme** : 1er refrain à 49,7 s (8 + 16 mesures à 116) → Intro 4 mesures (≤ 45 s), **pont de 8 mesures** (nappe + hook, sans kick) avant le dernier Refrain, **post-refrain** 8 mesures avec le hook seul au-dessus de la batterie (B14). Le 2e Refrain doit ajouter une couche (tambourin + contre-chant : « 2e refrain plus gros »).
6. **Nuances** : vélocités à deux niveaux → profil par temps + crescendo vers le Refrain (B8) ; humanisation ±1–2 ticks.

---

### A5. Synthèse de la famille « Autres »

1. Les quatre styles sont **à voix** : dans les hits, le hook instrumental (cuatro, guitare, marimba, riff Moog, hook de synthé) existe déjà en intro et en post-refrain, et c'est lui que notre lead doit imiter — pas un arpège.
2. **Tempos** de nos recettes OK (95 / 105 / 105 / 116) ; hits : reggaeton 89–110, afrobeats 100–112, synthwave 84–116 (ou double), pop 96–135.
3. **Harmonie** : 4 accords diatoniques en boucle (100 % diatonique sur Hooktheory), mineur avec VII/VI en synthwave et reggaeton, vamps de 2 accords en afrobeats, 7e/9e aux claviers pop ; nos progressions sont presque justes, à corriger pour synthwave (i–VII–VI–iv, i–v–VII–IV) et à enrichir (Seventh en pop).
4. **Mélodies de hits** : ambitus 7–17 demi-tons par section, 55–83 % de notes d'accord, 0,4–1,1 temps par note, motif court répété avec un changement en fin de ligne, silence en fin de phrase, 4 lignes appel/réponse ; **nos leads : 1 mesure recopiée 4×, transposée avec l'accord, 2 vélocités, pas de silence de phrase, pas de réponse**.
5. **Batterie** : reggaeton et afrobeats de nos recettes sont justes dans le squelette (dembow 3,6,11,14 ; kick afro 1/« et »2/3) mais manquent les couches qui font vivre : ghosts, clave 3-2, open hat sur « et » de 2, güira continue, swing 8–16 % (reggaeton à 0), coupures de 2 mesures ; synthwave : il manque le **gate** sur la caisse claire ; pop : 4/4 au refrain.
6. **Arrangement** : hits = un nouvel élément toutes les 8 mesures, 1er refrain ≤ 45 s, 2e refrain plus gros, pont/break de 8 mesures, outro = hook. Nos formes Song (8/16/16/16/16/8) n'ont ni pré-refrain, ni pont, ni post-refrain, et le lead n'apparaît qu'au Refrain.
7. **Son** : couches détunées/chorus/unisson stéréo (chantier moteur), pompe au kick (déjà en synthwave), saturation légère de bus, −10 à −12 LUFS (afrobeats/pop), ≤ −1 dBTP.
8. Priorité d'impact : (1) le hook mélodique (partie B), (2) les couches rythmiques manquantes et le swing, (3) la forme (aperçu en intro, post-refrain, pont), (4) le gate synthwave et les 7e pop, (5) le son stéréo.

---

## Partie B — Spécification « mélodie pro » pour le générateur (tous styles)

### B0. Diagnostic : les « tells » machine de `startingNotes`, mesurables

| Tell | Où dans le code | Ce qu'on entend |
|---|---|---|
| Cellule d'1 mesure recopiée 4× | `withLine` → `StepLine.barOf(pattern, bar)` | « chaque mesure identique » |
| Transposition diatonique rigide avec l'accord | `harmony.degreeRoot(degree + step)` | mélodie qui « suit » l'accord sans note commune ni conduite |
| 2 vélocités (0,8 / 0,56) | `withLine`: `if (e.token.isLowerCase()) velocity * 0.7f` | nuances plates |
| Longueur = 1 pas sauf `-` | `e.length * TICKS_PER_STEP` | pas de note longue en fin de phrase |
| Aucun décalage | positions = `step * 24` | grille parfaite |
| Même clip dans toutes les scènes | `inScenes` ne fait qu'effacer `notes` | couplet = refrain |
| Arpège triade UpDown 1 octave, vélocité 0,65 | `Performance(arp = UpDown, arpRate)` + `Voicing.Triad` | arpège « preset » |
| `counter` = accords en coups | `withCounterTrack` → `withChordRhythm` | pas de réponse mélodique |
| 8 recettes avec la même cellule tresillo | `leadPattern` de trance, trap, drill, reggaeton, afrobeats, phonk, lo-fi, UK garage | tous les styles ont « le même air » |

### B1. Architecture de phrase

Sources : iZotope (couplet de 8 mesures = quatre phrases de 2 : poser, répéter, contraster, revenir ; silences comme ponctuation ; levée avant le temps fort) [M1] ; EDMProd (Garrix : silences aux mesures 2 et 4, mes. 3 répète mes. 1, mes. 4 reprend le rythme de mes. 2) [M2] ; HSD (hook de 6 mesures = 2+2 ascendantes puis 2 descendantes ; pause en fin de 4 mesures) [S18][S21] ; Liveschool (lignes 1/3 appel, 2/4 réponse) [M9] ; Loopcloud (appel qui finit non résolu, réponse qui conclut) [M3].

Règles :
- **R1** Le lead est écrit sur **8 mesures** (`NoteClip.steps = 128`) en forme **A A' B A''** (2 mesures chacune) ou **a a' b** (4 + 4 : antécédent qui finit sur un degré instable 2, 5 ou 7 ; conséquent qui finit sur 1 ou 3, note ≥ 2 temps).
- **R2** Chaque phrase de 4 mesures se termine par un **silence ≥ 1 temps** ou une note ≥ 2 temps ; chaque 8 mesures par un silence ≥ 2 temps (le fill de batterie de `WizardMix.phrase` tombe dans ce trou : respiration + fill = la transition pro).
- **R3** Une **levée** (note qui commence sur le pas 14 ou 15 de la mesure précédente) ouvre au moins une phrase sur deux (pop, afrobeats, reggaeton) ; jamais en synthwave arpégé.

### B2. Motif et développement

Sources : Splice (motif de 3 notes, varier d'abord le rythme, puis une hauteur, étendre à 8 mesures avec un fill de 2 temps toutes les 4 mesures et un saut d'octave vers la fin, doubler à l'octave grave) [M4] ; American Songwriter (un motif par section ; séquence, extension/troncature, rythme gardé / hauteurs changées) [M5][M6] ; Loopcloud (répéter 4–8 mesures puis changer la 1re ou la plus haute note, le rythme, les intervalles, la longueur d'une note) [M3] ; Top40Theory sur Max Martin (répétitions « très proches mais pas identiques », deux variations tolérées : adaptation aux paroles, changement de fonction en fin de phrase) [M7] ; HSD (E.O.L.A. : changement en fin de ligne) [S18].

Règles :
- **R4** Un **motif = 3 à 5 notes** (2 à 4 hauteurs) sur 1 mesure maximum, construit sur les notes de l'accord 1 + au plus une note voisine.
- **R5** Le motif revient **3 à 6 fois en 16 mesures**, chaque retour avec **≤ 2 notes changées** parmi : dernière note (vers la note de l'accord suivant), rythme (une durée doublée ou un silence ajouté), octave (+12 sur la dernière occurrence avant le refrain), transposition (séquence un degré plus haut ou plus bas, en gardant une note commune).
- **R6** Jamais **plus de 2 occurrences strictement identiques consécutives** (répétition exacte « comme une épice », EDMProd) ; jamais 8 mesures sans variation.
- **R7** Le **dernier motif de la section** = le premier avec sa fin modifiée (E.O.L.A.) : conclusion sur 1 ou 3 de l'accord 1, ≥ 2 temps.

### B3. Contour

Sources : Ableton « Creating Melodies 1 » (équilibre montée/descente, saut suivi d'un pas en sens inverse, **un seul sommet**, souvent sur un temps fort) [M8] ; EDMProd (60–80 % de mouvement conjoint ; sauts sur temps faibles moins tendus) [M2] ; Loopcloud (1/2 à 2/3 de pas, sauts gardés pour les moments forts) [M3] ; Liveschool (appel ascendant, réponse non linéaire / descendante) [M9] ; Max Martin (note la plus haute dans la 2e moitié de la section) [P24].

Règles :
- **R8** Par phrase de 8 mesures : **un sommet unique** (la hauteur max apparaît ≤ 2 fois), placé dans la 2e moitié et sur un temps 1 ou 3 (ou sa levée).
- **R9** **50–80 % d'intervalles conjoints** (≤ 2 demi-tons) hors arpèges ; **≤ 1 saut > 5 demi-tons par phrase de 4 mesures**, et tout saut > 5 est suivi dans les 2 notes d'un pas (≤ 2) en sens inverse. Exception : sauts entre notes de l'accord (arpège) et saut d'octave de fin de section.
- **R10** L'appel (mes. 1–2) a une **pente nette ≥ 0** (dernière note ≥ première), la réponse (mes. 3–4) finit **plus bas** que son début ; la note finale de 8 mesures est ≤ la note initiale + 2.

### B4. Rythme

Sources : EDMProd (2–3 cellules rythmiques par mélodie ; rythme tendu > rythme des accords) [M2] ; Loopcloud (syncope, note qui commence une croche avant la mesure = « push ») [M3] ; iZotope (durées variées : « on ne parle pas en rythme régulier ») [M1] ; Hooktheory (hits : 0,37–1,11 temps par note ; « syncopes marquées » Despacito, « lâches » pop) ; Audeobox (runs de 2–3 doubles-croches entre accords, afrobeats) [A20] ; Espresso (swing léger de doubles-croches) [P18].

Règles :
- **R11** **2 à 3 cellules rythmiques** distinctes par 8 mesures (ni 1 ni > 4).
- **R12** Densité du lead (hors arpège) : **0,35 à 1,2 temps par note** en moyenne sur 8 mesures ; synthwave lead 0,5–1,5 ; trap/lo-fi 0,8–2 ; trance/liquid pluck 0,25–0,5.
- **R13** **≥ 1 note syncopée par mesure** (début sur un pas impair ou sur le « et » : pas 2, 6, 10, 14 ou impairs) en reggaeton, afrobeats, pop dansante, house ; **≥ 1 anticipation** (pas 15 ou 14) par 4 mesures en pop/afrobeats ; triolets réservés au trap (charleys) et aux fills, jamais dans le lead par défaut.
- **R14** **≥ 3 durées distinctes** par 8 mesures (ex. 1, 2 et 4 pas), part de la durée la plus fréquente ≤ 70 %.

### B5. Degrés, tensions, notes à éviter

Sources : Temperley & de Clercq 2013 (corpus rock : degré 1 le plus fréquent puis 5 ; ♭7 plus fréquent que 7 dans les **mélodies** ; échelle « pentatonic union » 1-2-♭3-3-4-5-6-♭7 ; ♭2 et ♯4 les plus rares) [M10] ; EDMProd (notes hors accord sur temps faibles, résolues vite, tension sur temps fort = à résoudre immédiatement) [M2] ; Loopcloud (fondamentale et quinte « les plus fortes » ; note extérieure brève résolue par demi-ton ou ton vers une note d'accord) [M3] ; Liveschool (hook de refrain sur la **pentatonique**, couplet sur la gamme entière) [M9] ; Hooktheory (99–100 % diatonique dans tous les hits lus).

Règles :
- **R15** 100 % diatonique par défaut (la tonalité est verrouillée, `Harmony.lock = true`) ; le **refrain/hook** utilise de préférence la **pentatonique** (majeure 1 2 3 5 6 ; mineure 1 ♭3 4 5 ♭7) ; le couplet peut ajouter 2/4/6 (7 en majeur) comme notes de passage.
- **R16** Degrés 1, 5, 3 de l'accord = **≥ 80 % des notes sur les temps 1 et 3** ; la **dernière note de chaque phrase** est 1, 3 ou 5 de l'accord courant (antécédent : 5 ou 2 ; conséquent : 1 ou 3).
- **R17** Une note hors accord dure **≤ 2 pas (une croche)** et est suivie dans la même mesure d'une note d'accord à ≤ 2 demi-tons (broderie ou passage) ; interdit : une note à **1 demi-ton au-dessus** d'une note de l'accord tenue ≥ 1 temps (4 sur majeur, ♭6 sur mineur i en note longue, 7 sur un accord sans 7e… = « mélodie qui se bat avec l'accord ») ; en mineur, préférer ♭7 à 7 dans le lead (sauf mineur harmonique drill).

### B6. Notes d'accord vs notes de passage (cibles)

- **R18** Sur 8 mesures, **55–85 % des notes (pondérées par durée) sont des notes de l'accord de la mesure** (hits lus : 55 % Shape of You, 58–59 % Don't Start Now / Anti-Hero / Blinding Lights, 70–77 % Calm Down / Flowers / Nightcall / Despacito, 83 % Espresso, 96 % riff Tití) ; styles à riff (reggaeton, trance, afrobeats guitare) 75–95 %, pop/synthwave lead 55–80 %, lo-fi/liquid 50–70 % (9e, 13e tolérées car l'accord est Ninth).
- **R19** **Conduite à travers les accords** (au lieu de la transposition rigide) : sur 4 mesures, **≥ 1 changement d'accord où le lead garde une note commune** (pédale) et **≥ 1 où il entre sur la nouvelle note d'accord par un pas ≤ 2 demi-tons** ; les ensembles de hauteurs des mesures 2–4 ne sont **pas** tous des transpositions diatoniques de la mesure 1 (au plus 2 sur 4).

### B7. Registre et ambitus par style

Sources : Hooktheory (ambitus de section 3–17 demi-tons, voir tableaux A) ; `inLeadRange` (plafond 96 / médiane ≤ 88) ; Violet synthwave (lead saw/carré, sub-octave) [S20] ; Audeobox (guitare afro, log drum Do2–Do4, basse Do1–Mi2) [A20].

- **R20** Ambitus du lead **par section : 7 à 17 demi-tons** ; sur tout le morceau ≤ 24 ; **centre du refrain 2 à 7 demi-tons au-dessus du centre du couplet** (ou +12 par octave).
- **R21** Centre (médiane des hauteurs, MIDI) par style : pop / reggaeton pluck **72–81** (Do5–La5) ; afrobeats guitare/marimba **64–76** ; synthwave lead **67–79**, arpège **48–72** sur 2 octaves ; trance lead 72–84 (après baisse d'octave de `inLeadRange`), trap/phonk cloche 72–84, lo-fi 60–72 (Rhodes), liquid pluck 72–84, deep house stabs 60–72, basse jamais > 55. `inLeadRange` reste le garde-fou.

### B8. Vélocité, dynamique, humanisation

Sources : Unison (vélocité de base 75–80 variant 65–95 ; accents 85–95 sur les temps, contretemps 65–75, fantômes 35–50, caisse 100–115 ; ±5–12 ms sur le piano, charleys +4–6 ms, caisse −8–10 ms, quantification 50–65 % ; longueurs variées ±20 ticks/960 ; éviter vélocités identiques, longueurs identiques, copier-coller d'une boucle) [M11] ; Audeobox (shaker 60/40/70/40, charley 70–80/50–60, kick 75–85 sur le 3) [A20] ; Violet (swing 8–20 % afro, 8–16 % reggaeton, claps/congas en retard) [A19][R20] ; Attack (deep afro house : charleys des mesures 4 et 12 20–30 % plus doux) [M12] ; forum Ardour (variations patternées, pas aléatoires : « un bon batteur ne randomise pas ») [M13].

Règles (unités du modèle : 24 ticks par pas, 96 par temps ; à 116 BPM, 1 tick = 5,4 ms ; à 95 BPM, 6,6 ms) :
- **R22** Profil de vélocité par position dans le temps : pas 0 × 1,0 ; pas 2 (« et ») × 0,85 ; pas 1 et 3 × 0,7 ; temps 1 > temps 3 > temps 2/4 (× 1,0 / 0,92 / 0,85). Plage **max − min ≥ 0,25** sur 8 mesures ; **≤ 60 % des notes** à la même vélocité (arrondie à 0,05).
- **R23** **Crescendo** : vélocité moyenne des mesures 7–8 ≥ mesures 1–2 + 0,05 avant un Refrain/Drop ; le premier coup de section à 1,0 ; notes fantômes ≤ 0,45.
- **R24** **Humanisation déterministe** (graine = hash(style, mood, piste)) : décalage **±1 à 2 ticks** (≈ 5–12 ms) sur les notes hors temps 1, jamais sur le temps 1 d'une mesure ; décalage **constant par position** (profil, pas bruit blanc) ; retard +1 à +2 ticks sur claps/congas/rim ; **max |décalage| ≤ 3 ticks**. Les notes de basse et les glissés (`'/'`) ne sont pas décalés (contrainte `isLine` / `SLIDE_OVERLAP` = 12 ticks).
- **R25** Longueurs : ±2 ticks sur les notes ≥ 1 pas (pas sur les stabs d'1 pas), legato (chevauchement 5–10 ticks) sur les notes conjointes du lead si le son a un portamento.

### B9. Contre-chant et question–réponse

Sources : Loopcloud (appel non résolu, réponse qui conclut « souvent sur un autre instrument ») [M3] ; Audeobox (guitare mes. 1–2, log drum mes. 3–4) [A20] ; Blinding Lights (pont en question–réponse ; voix + hook ensemble seulement en outro) [S17] ; Ableton « Linear rhythm in melodies » (hocket : aucune note en même temps sur deux instruments) [M14] ; neurofunk/jump up de nos recettes (dialogue basse/lead déjà écrit en mots).

Règles :
- **R26** Quand le lead se tait ≥ 2 temps, **une autre couche répond** : contre-chant (2–4 notes, une octave sous le lead ou au-dessus des accords), fill de basse (5→6→R), stab d'accord ou percussion (timbales, pluck). Recouvrement des attaques lead/contre-chant **< 30 %**.
- **R27** Le contre-chant reprend **le motif du lead** (même rythme, hauteurs changées, ou même hauteurs à l'octave) : jamais un matériau étranger. `counter` doit donc devenir mélodique (B14).
- **R28** Basse et lead : **≥ 1 note d'attaque commune par mesure** (le 1) et **≤ 50 % d'attaques communes** sur la mesure (sinon les deux « parlent en même temps »).

### B10. Arpèges

Sources : Stranger Things (Cmaj7, doubles-croches continues, propulsion par filtre, 84 BPM) [S14][S15] ; Nightcall (motif de 8 notes, 3 couches : principal, 2 notes douces, sub-octave mono) [S1] ; Violet/babyaud (doubles-croches régulières ; bounce d'octave +1 sur la basse ; delay croche pointée) [S20][S22] ; nos recettes house/liquid (arpège à 1/8 pour liquid, 1/16 ailleurs).

Règles :
- **R29** Arpège de lead : **≥ 4 hauteurs** (7e ou octave ajoutée : `Seventh`, `Power`, ou triade + octave) sur **≥ 1,5 octave** (étendue ≥ 19 demi-tons) ; motif de 8 ou 16 pas **fixe et asymétrique** (ex. `R 5 O 3' 5' O' 5' 3'`) plutôt que UpDown ; UpDown accepté si l'étendue ≥ 2 octaves.
- **R30** Vitesse : 1/16 pour synthwave 84–116 BPM, trance, house ; 1/8 pour liquid DnB (174) et les tempos ≥ 160 ; gate **50–70 %** de la vitesse (pluck) ; **accent** +0,15 toutes les 4 notes (ou sur le 1er de chaque temps) ; hasard (`Random`) interdit par défaut.
- **R31** L'arpège **évolue par section** (filtre : `Sweep("lead", …)` Intro 0,3 → Couplet 0,5–0,8 → Refrain 1,0 ; ou octave +12 au Refrain) et **ne joue pas seul** au Refrain : un lead chantable ou le contre-chant le surplombe (synthwave), ou il arrive seulement au Drop (liquid).

### B11. Variation par section

Sources : Max Martin (prévisualisation mélodique dans le couplet ; 2e refrain plus gros ; contraste entre sections : registre, rythme, production) [P24] ; HSD (hook en intro, post-refrain, outro ; hook + voix ensemble à la fin) [S18] ; Notetracks (couplet 2 moitié plus court ; nouvel élément toutes les 8 mesures ; retirer avant les grosses sections) [S17] ; Audeobox (couplet et hook = réponses différentes) [A20].

Règles :
- **R32** Trois versions du même clip : **Intro** = hook seul (ou ses 4 premières mesures, filtre fermé) ; **Couplet** = **sous-ensemble ≤ 60 % des notes** du hook (aperçu : même rythme, notes retirées) ou contre-chant seul ; **Refrain** = hook complet, **+12 demi-tons ou doublé à la tierce/sixte** au 2e Refrain, densité ≥ 1,3 × celle du couplet.
- **R33** **Post-refrain / outro** = le hook sans la voix (dans notre cas, le hook à la place de la voix) : il doit apparaître **≥ 3 fois** dans le morceau (intro, après chaque refrain, outro).
- **R34** Chaque passage de section apporte **un** élément nouveau au lead (octave, doublure, filtre ouvert, delay plus long) et en retire un autre juste avant (silence des 2 dernières mesures avant le Refrain : « enlever pour que ça revienne frais »).

### B12. À éviter (les « tells » machine)

1. **Toutes les mesures identiques** (> 2 répétitions exactes de suite ; 4 mesures recopiées).
2. **Même vélocité** partout (plage < 0,2) et **même longueur** partout (> 70 % d'une seule durée).
3. **Aucun silence** ≥ 1 temps sur 4 mesures ; mélodie qui remplit chaque pas.
4. **Notes au hasard sans motif** (0 occurrence du motif ; `ArpMode.Random`).
5. **Mélodie qui se bat avec l'accord** : < 50 % de notes d'accord ; note hors accord ≥ 1 temps ; demi-ton au-dessus d'une note d'accord tenu.
6. **Transposition rigide** du motif avec chaque accord (contour identique × 4) ; aucune note commune.
7. **Arpège triade UpDown 1 octave** comme seul lead ; **cellule tresillo partagée** par 8 styles.
8. **Pas de sommet** (hauteur max atteinte > 3 fois) ou plusieurs sommets égaux.
9. **Lead présent de la 1re à la dernière mesure** sans variante, et absent de l'intro.
10. **Aucun décalage** et **pas de swing** dans les styles qui en ont (reggaeton 8–16 %, afrobeats 8–20 %, Espresso-pop léger) ; ou du hasard pur à la place d'un profil.

### B13. Métriques : calcul depuis un `NoteClip` et seuils

Conventions : `T = NoteClip.TICKS_PER_STEP = 24`, mesure = 384 ticks, temps = 96, phrase = 1 536 (4 mesures). Notes triées par `start` ; pour un lead monophonique, l'« intervalle » est la différence de `pitch` entre notes consécutives (sautant les attaques simultanées). `chordTones(bar)` = classes de hauteur de `harmony.voiced(degrees[bar % 4], 0, Voicing.Seventh)` (la 7e comptée comme note d'accord si le style est en `Ninth/Seventh`, sinon `Triad`). `strong(n) = n.start % 192 == 0` (temps 1 et 3). Seuils pour le lead de hook (les arpèges relèvent de M17 seulement).

| # | Métrique | Calcul | Seuil |
|---|---|---|---|
| M1 | Longueur de phrase | `clip.steps` | = 128 (8 mesures) pour lead/hook ; basse 64 ou 128 |
| M2 | Occurrences du motif | motif = (séquence d'intervalles, séquence de durées en pas) des notes de la mesure 1 ; pour chaque fenêtre glissante d'une mesure, distance d'édition sur les deux séquences ; occurrence si ≤ 2 opérations | 3 ≤ n ≤ 6 sur 16 mesures (clip 8 mesures joué 2×) |
| M3 | Répétitions exactes | mesures `i` et `i+1` identiques (mêmes `start%384`, `length`, `pitch`, `velocity`) | ≤ 1 paire consécutive par 8 mesures ; ≤ 50 % des mesures identiques à une autre |
| M4 | Silences | gaps `next.start − n.end` : ≥ 96 ticks | ≥ 1 par 4 mesures ; ≥ 1 gap ≥ 192 dans les mesures 4 et 8 |
| M5 | Fin de phrase | dernière note des mesures 4 et 8 : `length ≥ 192` ou suivie d'un gap ≥ 192 ; `pitch % 12 ∈ chordTones` | les deux vrais |
| M6 | Densité | `(ticks joués / 96) / nombre de notes` | 0,35–1,2 (voir R12 par style) |
| M7 | Sommet | nombre de notes à `pitch == max` ; position du 1er max (`start ≥ 768` et `strong`) | ≤ 2 ; dans la 2e moitié sur temps fort |
| M8 | Conjoint / sauts | part des intervalles avec `|Δ| ≤ 2` ; sauts `|Δ| > 5` par phrase ; pour chaque saut, l'un des 2 intervalles suivants a signe opposé et `|Δ| ≤ 2` | 50–80 % ; ≤ 1 par 4 mesures ; 100 % des sauts résolus (hors octave de fin) |
| M9 | Notes d'accord | somme des `length` des notes dont `pitch%12 ∈ chordTones(bar)` / somme totale ; même ratio restreint à `strong` | 55–85 % (riffs 75–95) ; ≥ 80 % sur temps forts |
| M10 | Tensions | notes hors accord : `length ≤ 48` et note suivante d'accord à `|Δ| ≤ 2` ; aucune note avec `(pitch − c) % 12 == 1` pour un `c` de l'accord et `length ≥ 96` | 100 % résolues ; 0 frottement long |
| M11 | Ambitus / centre | `max − min` par section ; médiane des `pitch` | 7–17 par section ; centre dans la plage R21 ; refrain − couplet ∈ [2, 12] |
| M12 | Vélocité | `max(v) − min(v)` ; part de la vélocité modale (arrondie 0,05) ; moyenne temps forts − temps faibles ; moyenne mes. 7–8 − mes. 1–2 | ≥ 0,25 ; ≤ 60 % ; ≥ 0,08 ; ≥ 0,05 avant un Refrain |
| M13 | Humanisation | `start % 24` ≠ 0 pour les notes hors temps 1 ; `|start − grille| ≤ 3` ; même décalage pour la même position dans ≥ 75 % des mesures | ≥ 30 % des notes décalées ; max 3 ticks ; profil stable |
| M14 | Durées | nombre de `length` distincts (arrondis au pas) ; part de la plus fréquente | ≥ 3 ; ≤ 70 % |
| M15 | Syncopes | notes dont `(start % 96) ∈ {24, 48, 72}` (hors « 0 ») par mesure ; levées `start % 384 ≥ 336` | ≥ 1 par mesure (styles dansants) ; ≥ 1 par 4 mesures (pop/afro) |
| M16 | Réponse | pour chaque gap du lead ≥ 192, nombre d'attaques du contre-chant/basse-fill/stab dans ce gap ; part des attaques du contre-chant simultanées au lead | ≥ 1 ; < 30 % |
| M17 | Arpège | hauteurs distinctes ; étendue ; motif ≠ UpDown pur ; `arpRate` ; accents (vélocité max − min) | ≥ 4 ; ≥ 19 ; motif fixe ou étendue ≥ 24 ; 24 ou 48 selon tempo ; ≥ 0,15 |
| M18 | Conduite harmonique | aux changements d'accord : note commune (`pitch` tenu ou répété) ; entrée par pas ≤ 2 ; mesures dont le multiset de `pitch − root(bar)` égale celui de la mesure 1 | ≥ 1 et ≥ 1 par 4 mesures ; ≤ 2 mesures sur 4 |
| M19 | Variation par section | notes du Couplet ⊂ notes du Refrain (même `start`, `pitch`) ; `|couplet| / |refrain|` ; octave ou doublure au 2e Refrain | sous-ensemble ; ≤ 0,6 ; ≥ 1 changement |
| M20 | Présence du hook | nombre de sections où le clip hook est non vide (Intro, Refrain, post-refrain/outro) | ≥ 3 |

Ces mesures se calculent en Kotlin pur sur `Project.scenes[*].trackNotes[lead]` (test `:app:testDebugUnitTest` ou `project-model`), pour les 36 styles × 4 moods, sans rendu audio. Un test « tells » échoue si l'un de B12 (1–8) est vrai.

### B14. Proposition d'implémentation (pour celui qui corrigera le générateur)

1. **Grammaire de hook dans la recette** : remplacer `leadPattern` (1 mesure) par `hook = Hook(cell = "R..3..5.O.......", answer = "..5..3..2-R-....", form = "AA'BA''", rests = …)` ou, mieux, **génération** : (a) motif de 3–5 notes depuis les notes d'accord de la mesure 1 + 1 voisine (graine déterministe par style+mood) ; (b) cellule rythmique du style (3-3-2 reggaeton, croches pointées afro, croches/doubles pop, 1/16 arpège synthwave) avec 1 silence ≥ 1 temps ; (c) opérations de développement dans l'ordre : répéter, changer la dernière note vers l'accord suivant, séquence +1 degré en gardant une note commune, saut d'octave sur l'occurrence 4, fill de 2 temps en mesure 8 ; (d) profil de vélocité R22 + crescendo ; (e) humanisation R24 ; (f) `inLeadRange`.
2. **Conduite** : `withLine` doit accepter des jetons **absolus de gamme** (ex. `r3` = 3e degré de la tonalité, pas de l'accord) et un jeton « tenir la note précédente si elle est dans le nouvel accord » (`=`), pour casser la transposition rigide.
3. **Trois clips par partie** (Intro / Couplet / Refrain) produits par `startingNotes(part, style, mood, harmony, sound, section)` ; `inScenes` écrit la version de chaque scène au lieu d'effacer.
4. **`counter` mélodique** : `withCounterTrack` joue le motif du lead (rythme identique, hauteurs à l'octave ou tierce) **dans les trous du lead** (hocket), et non les accords ; `counterRhythm` reste pour les styles « stabs ».
5. **Arpège** : `Performance` avec `ArpMode.Pattern(List<Int>)` (indices de notes de l'accord sur 2 octaves), `gate` (0,5–0,7) et `accentEvery = 4` ; `Sweep("lead", …)` par défaut pour synthwave/trance.
6. **Forme Song** : ajouter `Pont` (8 mesures, nappe + hook, sans kick) et `post-refrain` (hook seul sur batterie, 8 mesures) ; Intro à 4 mesures quand le 1er Refrain tomberait après 45 s.
7. **Tests** : un test par métrique M1–M20 sur toutes les recettes, plus un test « tells » (B12) ; seuils par famille dans une table ; les projets des phases précédentes (démos) ne sont pas touchés.

---

## Sources

Reggaeton
- [R1] Beatport (Gasolina 96 BPM Fa majeur ; Mi Gente 105 Mi mineur) — https://www.beatport.com/track/-/28478810
- [R2] Hooktheory, Gasolina — https://www.hooktheory.com/theorytab/view/daddy-yankee/gasolina
- [R3] SongBPM, Danza Kuduro — https://songbpm.com/@don-omar/danza-kuduro
- [R4] Wikipédia, Danza Kuduro — https://en.wikipedia.org/wiki/Danza_Kuduro
- [R5] Jamzone/Music Will, El Perdón — https://jamzone.musicwill.org/songs/inthestyleof-elperdon-by-nickyjam
- [R6] Wikipédia, Despacito (composition, production) — https://en.wikipedia.org/wiki/Despacito
- [R7] Hooktheory, Despacito — https://hooktheory.com/theorytab/view/luis-fonsi/despacito
- [R8] Wikipédia, Mi Gente — https://en.wikipedia.org/wiki/Mi_Gente_(J_Balvin_and_Willy_William_song)
- [R9] SongBPM, Dura — https://songbpm.com/@daddy-yankee/dura
- [R10] SongBPM, Con Calma — https://songbpm.com/@daddy-yankee/con-calma
- [R11] Wikipédia, Con Calma — https://en.wikipedia.org/wiki/Con_Calma
- [R12] Wikipédia, Tusa — https://en.wikipedia.org/wiki/Tusa_(song)
- [R13] SongBPM, Tusa — https://songbpm.com/@karol-g/tusa
- [R14] Remezcla / Lamezcla / Singles Jukebox, Dákiti — https://remezcla.com/music/bad-bunny-teams-jhay-cortez-dakiti-music-video/ ; https://lamezcla.com/?p=5658 ; https://thesinglesjukebox.com/?p=8860
- [R15] SongBPM, Dákiti — https://songbpm.com/@bad-bunny/dakiti ; Wikipédia — https://en.wikipedia.org/wiki/D%C3%A1kiti
- [R16] Music Gateway, Tití Me Preguntó (107 BPM Fa mineur) — https://www.musicgateway.com/song-key-bpm/bad-bunny/titi-me-pregunto
- [R17] Hooktheory, Tití Me Preguntó — https://www.hooktheory.com/theorytab/view/bad-bunny/titi-me-pregunto
- [R18] Music Gateway, Me Porto Bonito — https://www.musicgateway.com/song-key-bpm/bad-bunny/me-porto-bonito
- [R19] Wikipédia, Me Porto Bonito — https://en.wikipedia.org/wiki/Me_Porto_Bonito
- [R20] Violet Recording, How to Make Reggaeton Beats — https://violetrecording.com/how-to-make-reggaeton-beats/
- [R21] Wikipédia, Reggaeton — https://en.wikipedia.org/wiki/Reggaeton
- [R22] uJam, How to Make a Dembow Groove — https://www.ujam.com/tutorials/how-to-make-a-dembow-groove/
- [R23] Wikipédia, Dembow beat — https://en.wikipedia.org/wiki/Dembow_beat
- (non lu, tronqué) MusicRadar, 22 reggaeton tips — https://www.musicradar.com/news/reggaeton-tips-bad-bunny

Afrobeats
- [A1] SongBPM, Ojuelegba — https://songbpm.com/@wizkid/ojuelegba
- [A2] Wikipédia, Ojuelegba — https://en.wikipedia.org/wiki/Ojuelegba_(song)
- [A3] SongBPM, Pana — https://songbpm.com/@tekno/pana
- [A4] SongBPM, Fall — https://songbpm.com/@davido/fall
- [A5] Wikipédia, Fall — https://en.wikipedia.org/wiki/Fall_(Davido_song)
- [A6] Beatport (Ye 101 BPM Fa mineur) — https://www.beatport.com/track/update/22140687 ; SongBPM (202) — https://songbpm.com/@burna-boy/ye
- [A7] Wikipédia, Ye (song) / Phantom — https://en.wikipedia.org/wiki/Ye_(song) ; https://en.wikipedia.org/wiki/Phantom_(producer)
- [A8] Wikipédia, Love Nwantiti — https://en.wikipedia.org/wiki/Love_Nwantiti
- [A9] SongBPM / Beatport, Essence (104 BPM Do majeur) — https://songbpm.com/@wizkid/essence ; https://www.beatport.com/track/essence/21390812
- [A10] Wikipédia, Essence / Rolling Stone (oral history, cité via résumé de recherche ; page payante) — https://en.wikipedia.org/wiki/Essence_(Wizkid_song)
- [A11] Stereogum, Essence — https://stereogum.com/?p=2158899
- [A12] SongBPM, Peru — https://songbpm.com/@fireboy-dml/peru
- [A13] Wikipédia, Peru — https://en.wikipedia.org/wiki/Peru_(song)
- [A14] Wikipédia, Calm Down — https://en.wikipedia.org/wiki/Calm_Down_(Rema_song)
- [A15] Hooktheory, Calm Down — https://www.hooktheory.com/theorytab/view/rema/calm-down
- [A16] OkayAfrica, Andre Vibez — https://www.okayafrica.com/rema-calm-down-andre-vibez/ ; Songfacts — https://www.songfacts.com/facts/rema/calm-down
- [A17] Wikipédia, Last Last — https://en.wikipedia.org/wiki/Last_Last
- [A18] Wikipédia, Rush — https://en.wikipedia.org/wiki/Rush_(Ayra_Starr_song) ; Mixgraph (médiane 96) — https://www.mixgraph.io/artists/ayra-starr
- [A19] Violet Recording, How to Make Afrobeats / Afrobeat Beats — https://violetrecording.com/how-to-make-afrobeats/ ; https://violetrecording.com/how-to-make-afrobeat-beats/
- [A20] Audeobox, How to Make Afrobeats in FL Studio (grille, basse, structure, mix) — https://www.audeobox.com/learn/fl-studio/how-to-make-afrobeats-in-fl-studio/
- [A21] Liveschool (voir M9)
- [A22] Wikipédia, Afrobeats — https://en.wikipedia.org/wiki/Afrobeats

Synthwave
- [S1] Reverb Machine, How Kavinsky Created Nightcall — https://reverbmachine.com/blog/how-kavinsky-created-nightcall/
- [S2] Hooktheory, Nightcall — https://www.hooktheory.com/theorytab/view/kavinsky/nightcall
- [S3] Wikipédia, Nightcall — https://en.wikipedia.org/wiki/Nightcall_(song) ; Point Blank/MusicTech — https://musictech.com/tutorials/point-blanks-weekly-tutorial-deconstructing-kavinsky/
- [S4] Chordu (transcription piano, ≈86) — https://chordu.com/chords-tabs--hd-a-real-hero-drive-movie-soundtrack-piano-solo-id_8gvA25cUkHA ; Audiokeychain (112, 8B) — https://www.audiokeychain.com/track/gC/college-a-real-hero
- [S5] Wikipédia, A Real Hero — https://en.wikipedia.org/wiki/A_Real_Hero
- [S6] Hooktheory, Midnight City — https://www.hooktheory.com/theorytab/view/m83/midnight-city
- [S7] Wikipédia, Midnight City — https://en.wikipedia.org/wiki/Midnight_City
- [S8] SongBPM, Future Club — https://songbpm.com/@perturbator/future-club
- [S9] SongBPM, Turbo Killer — https://songbpm.com/@carpenter-brut/turbo-killer
- [S10] SongBPM / SongData, Tech Noir — https://songbpm.com/@gunship/tech-noir ; https://songdata.io/track/3PYYcyaQNhfwuO6YnA2eaC/Tech-Noir-Instrumental-by-Gunship
- [S11] Chordu, Los Angeles (116) — https://chordu.com/tabs/los-angeles-chords-by-the-midnight-id_0x19f5a ; SongBPM, Shadows — https://songbpm.com/@the-midnight/shadows
- [S12] SongData, Sunset — https://songdata.io (via recherche) ; themidnight.wiki
- [S13] SongBPM, Running in the Night — https://songbpm.com/@fm-84/running-in-the-night
- [S14] Splice, Recreating the theme from Stranger Things — https://splice.com/blog/recreating-stranger-things-theme
- [S15] Soundfly (résumé de recherche) — https://flypaper.soundfly.com/produce/rebuild-the-spooky-arpeggiated-synth-stranger-things/ ; DJ Mag — https://djmag.com/news/stranger-things-composers-break-down-theme-song-production-listen
- [S16] Hooktheory, Blinding Lights — https://www.hooktheory.com/theorytab/view/the-weeknd/blinding-lights ; Wikipédia — https://en.wikipedia.org/wiki/Blinding_Lights
- [S17] Notetracks, Behind the Track: Blinding Lights — https://notetracks.com/blog/behind-the-track-the-weeknds-blinding-lights-production-and-arrangement-breakdown
- [S18] Hit Songs Deconstructed, « Blinding Lights: The Art of the Hook » (PDF) — https://www.hitsongsdeconstructed.com/wp-content/uploads/2025/01/blinding-lights-the-art-of-the-hook.pdf
- [S19] Wikipédia, Synthwave — https://en.wikipedia.org/wiki/Synthwave
- [S20] Violet Recording, How to Make Synthwave — https://violetrecording.com/how-to-make-synthwave/
- [S21] Loopcloud, Unpacked: Blinding Lights — https://www.loopcloud.com/cloud/blog/5227-Unpacked-The-Weeknd-Blinding-Lights-Recreated-with-Loopcloud
- [S22] Baby Audio, How to Make Synthwave — https://babyaud.io/blog/how-to-make-synthwave
- [S23] Wikipédia, Gated reverb — https://en.wikipedia.org/wiki/Gated_reverb
- [S24] MusicTech, vintage drum gated reverb — https://musictech.com/tutorials/tips/how-to-create-authentic-vintage-drum-gated-reverb-for-synthwave-chillwave-music-styles
- [S25] Loopmasters, How to Make a Gated Reverb Snare — https://loopmasters.com/articles/2928-How-to-Make-a-Gated-Reverb-Snare
- [S26] Wikipédia, LinnDrum — https://en.wikipedia.org/wiki/LinnDrum

Pop
- [P1] Wikipédia, Shape of You — https://en.wikipedia.org/wiki/Shape_of_You
- [P2] Hooktheory, Shape of You — https://www.hooktheory.com/theorytab/view/ed-sheeran/shape-of-you
- [P3] MusicRadar, Hits decoded: Shape of You — https://www.musicradar.com/news/hits-decoded-how-ed-sheerans-shape-of-you-topped-the-charts
- [P4] Hooktheory, bad guy — https://www.hooktheory.com/theorytab/view/billie-eilish/bad-guy ; MusicRadar (résumé) — https://musicradar.com/news/how-to-sound-like-billie-eilish-bad-guy
- [P5] Hit Songs Deconstructed, Bad Guy's Prosody — https://reports.hitsongsdeconstructed.com/articles/technique-spotlight-bad-guys-prosody/
- [P6] Hooktheory, Don't Start Now — https://www.hooktheory.com/theorytab/view/dua-lipa/dont-start-now
- [P7] Point Blank / DJ Mag, Don't Start Now deconstruction — https://djmag.com/pointblank/point-blank-track-deconstruction-dua-lipa-don-t-start-now-w-ski-oakenfull
- [P8] MusicRadar, Don't Start Now (résumé de recherche) — https://www.musicradar.com/artists/the-next-morning-we-woke-up-and-were-like-we-have-to-make-a-disco-song-digging-into-the-dua-lipa-smash-that-triggered-a-disco-revival
- [P9] SongBPM, Balance ton quoi — https://songbpm.com/@angele/balance-ton-quoi
- [P10] Wikipédia (fr), Balance ton quoi — https://fr.wikipedia.org/wiki/Balance_ton_quoi ; Ultimate Guitar (Ré mineur) — https://es.ultimate-guitar.com/tab/2486049
- [P11] Hooktheory, Levitating — https://www.hooktheory.com/theorytab/view/dua-lipa/levitating
- [P12] Hooktheory, As It Was — https://www.hooktheory.com/theorytab/view/harry-styles/as-it-was
- [P13] Wikipédia, As It Was — https://en.wikipedia.org/wiki/As_It_Was
- [P14] Hooktheory, Anti-Hero — https://www.hooktheory.com/theorytab/view/taylor-swift/anti-hero
- [P15] Wikipédia, Anti-Hero — https://en.wikipedia.org/wiki/Anti-Hero_(song)
- [P16] Hooktheory, Flowers — https://www.hooktheory.com/theorytab/view/miley-cyrus/flowers
- [P17] Hooktheory, Espresso — https://www.hooktheory.com/theorytab/view/sabrina-carpenter/espresso
- [P18] MusicRadar, un professeur analyse Espresso (résumé de recherche) — https://www.musicradar.com/artists/singers-songwriters/how-does-the-vocal-melody-fit-the-chords-it-kind-of-doesnt-a-music-professor-breaks-down-sabrina-carpenters-grammy-nominated-espresso
- [P19] Music Week, Hitmakers: Espresso — https://www.musicweek.com/interviews/read/hitmakers-the-songwriting-secrets-behind-sabrina-carpenter-s-espresso/090901
- [P20] Wikipédia, Good Luck, Babe! — https://en.wikipedia.org/wiki/Good_Luck,_Babe!
- [P21] Secrets of Songwriting, intro length — https://www.secretsofsongwriting.com/?p=4369
- [P22] Hit Songs Deconstructed, Q3 2014 report — https://reports.hitsongsdeconstructed.com/articles/the-hit-songs-deconstructed-trend-report/
- [P23] Hit Songs Deconstructed, Q1 2024 (résumé de recherche) — https://hitsongsdeconstructed.com/hsd_wire/seven-trends-from-the-hit-songs-deconstructed-q1-new-arrivals-report ; Billboard 2019 — https://www.billboard.com/?p=8514407
- [P24] Music Business Worldwide, Max Martin techniques (Asaf Peres) — https://www.musicbusinessworldwide.com/how-max-martins-songwriting-techniques-are-used-to-write-hit-after-hit-after-hit

Mélodie (littérature)
- [M1] iZotope, Making Your Melodies Memorable, Part I — https://www.izotope.com/en/learn/songwriting-tips-making-your-melodies-memorable-part-1
- [M2] EDMProd, Advanced Melody Guide: Chord Tones, Motifs — https://www.edmprod.com/advanced-melodies-chord-tones-motifs/
- [M3] Loopcloud, 12 Simple Tips for Writing Masterful Melodies — https://www.loopcloud.com/cloud/blog/5354-12-Simple-Tips-for-Writing-Masterful-Melodies
- [M4] Splice, How to develop a melody — https://splice.com/blog/how-to-develop-a-melody/
- [M5] American Songwriter, Measure for Measure: Motivate Your Melody — https://americansongwriter.com/measure-for-measure-motivate-your-melody
- [M6] iZotope, How to Create Strong Melodies (développement de motif) — https://www.izotope.com/community/blog/how-to-create-strong-melodies
- [M7] Top40Theory, Max Martin's melodic math repetition tweaks — https://www.top40theory.com/blog/max-martins-melodic-math-repetition-tweaks-in-taylor-swifts-shake-it-off
- [M8] Ableton « Making Music », Creating Melodies 1: Contour — https://makingmusic.ableton.com/creating-melodies-1-contour
- [M9] Liveschool, Writing strong melodies and hooks (Elizabeth Rose) — https://www.liveschool.net/in-the-box-writing-strong-melodies-and-hooks-with-elizabeth-rose/
- [M10] Temperley & de Clercq, Statistical Analysis of Harmony and Melody in Rock Music, JNMR 2013 — https://www.midside.com/publications/temperley_declercq_2013.pdf
- [M11] Unison, How to Humanize MIDI — https://unison.audio/how-to-humanize-midi/
- [M12] Attack Magazine, Beat Dissected: Deep Afro House — https://www.attackmagazine.com/technique/beat-dissected/deep-afro-house/
- [M13] Forum Ardour, humanize — https://discourse.ardour.org/t/how-do-i-humanize-a-midi-track/100123
- [M14] Ableton « Making Music », Linear Rhythm in Melodies (hocket) — https://makingmusic.ableton.com/linear-rhythm-in-melodies
- [M15] Hooktheory, Song metrics — https://www.hooktheory.com/song-metrics/about
- [M16] FeelYourSound, quick tricks (rythme d'abord, appel/réponse, doubler puis tordre la fin) — https://feelyoursound.com/quick-tricks-to-come-up-with-new-melodies
- [M17] eMastered, trap chord progressions (boucles de 2–4 mesures, mineur naturel/harmonique) — https://emastered.com/blog/trap-chord-progressions

Sonie
- [L1] iZotope, Mastering for streaming platforms — https://www.izotope.com/en/learn/mastering-for-streaming-platforms.html
- [L2] RoEx, What LUFS should I master at — https://www.roexaudio.com/blog/what-lufs-should-i-master-at
- [L3] KVR forum (anecdotique) — https://www.kvraudio.com/forum/viewtopic.php?p=9028512
