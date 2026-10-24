# Famille House : 10 hits par style, et ce qui manque à nos morceaux

Date : 2026-10-23. Dépôt lu en lecture seule : `/home/user/ps-p22` (`StyleRecipes.kt`, `StarterWizard.kt`, `WizardMix.kt`, `StyleMusic.kt`, `docs/CORPS.md`, `docs/STYLE-FIXES.md`, `docs/plan/son.md`, `docs/son/banc/apres-etape2.md`, `docs/RETOURS.md`).
Phrase de Benjamin (RETOURS, 2026-10-23) : « Ça ressemble encore trop à de l'amateur, surtout les mélodies synthé ou similaires. »

Méthode : pas d'écoute possible ; tout vient de sources écrites publiques (Attack Magazine « Beat Dissected » et « Passing Notes », EDMProd, Native Instruments, Beatportal, Roland, Producer School, Sound on Sound, Red Bull Music Academy, Hooktheory, Wikipedia, bases BPM : songbpm (données Spotify), Beatport, Tunebat, AudioKeychain, analyses Mixmeister/Traktor des archives UBC/Waterloo). Les tempos et tonalités des bases automatiques sont donnés tels quels, avec ≈ quand deux sources divergent ou que la mesure n'a pas pu être confirmée ; « nv » = non vérifié (aucune source trouvée ; le quota de recherche web de la session a été épuisé avant la fin, 49 recherches et ~100 pages lues). Rien n'est inventé : quand un chiffre manque, il manque.

Rappel de ce que fait aujourd'hui le générateur (valable pour les 9 styles, cité ensuite par son nom) :
- **Accords** : `withChordRhythm` : un accord par mesure, 4 mesures (`progressions[mood]`), empilement en position fondamentale par degrés de gamme (`Voicing.Seventh` = 0,2,4,6 ; `Ninth` = 5 notes), même forme à chaque mesure, `nearShift` ±1 octave, vélocité fixe 0,75 (minuscule ×0,75), rythme `chordRhythm` de 16 pas répété chaque mesure.
- **Basse** : `withLine` sur `bassPattern` (16 ou 32 pas), vélocité 0,9 / 0,63, glissé sur `/`, pompe fixe (0,45 ; 0,35) ajoutée par `WizardMix.withGlue`.
- **Lead** : `leadPattern` de 16 pas (ou 32) rejoué sur chaque mesure d'accord, notes = degrés de l'accord courant, vélocité 0,8 / 0,56 ; sans `leadPattern` : arpège `UpDown` d'une triade tenue, une note par double-croche (`arpRate = 24` ticks = 1 pas), vélocité 0,65. Aucune phrase de 2 ou 4 mesures, aucune réponse, aucune variation toutes les 8 mesures, aucun silence structurel (hors points du motif).
- **Batterie** : 3 vélocités (X = 1, x = 0,6, o = 0,35), swing global, pas de décalage en ms, roulement toutes les 4 mesures (`WizardMix.phrase`), pas de crash ; mêmes coups pendant 16 à 32 mesures.
- **Forme club** (`sectionsOf`) à 120–132 BPM : Intro 16 · Groove 16 · Drop 16 · Break 8 · Drop 16 · Intro 16 = 88 mesures (≈ 2:50) ; la forme 32 mesures de Drop ne passe pas sous 3:30. Forme chanson (UK garage) : Intro 8 · Couplet 16 · Refrain 16 · Couplet 16 · Refrain 16 · Intro 8 = 80 mesures. Chaque section = une scène de 4 mesures bouclée ; seul le « Drop 2 » (`dropTwoLane`), le souffle (`riserLane`) et les balayages bougent dedans.
- **Mesuré** (banc après étape 2) : Drop à −8 à −9 LUFS (bien), true peak −1,2 (bien), **côté/centre −22 à −31 dB** (cible −15 à −9), **Drop − Groove ≈ 0 LU** (cible ≥ +2), **Drop − Break ≈ 0 LU** (cible ≥ +4), air −19 à −22 dB (cible −20 à −16).

---

## 1. House (`house` · House · 122 BPM)

### 10 morceaux

| # | Artiste – Titre (année) | BPM | Tonalité | Source |
|---|---|---|---|---|
| 1 | Jesse Saunders – On and On (1984) | ≈ 120 (nv) | – | – |
| 2 | Marshall Jefferson – Move Your Body (1986) | 122 (songbpm) / 128 (Hooktheory) | Mi mineur, i–IV–i–iv–v | Hooktheory, songbpm |
| 3 | Frankie Knuckles & Jamie Principle – Your Love (1987) | 119 | – | songbpm |
| 4 | Joe Smooth – Promised Land (1987) | 125 | – | songbpm |
| 5 | Inner City – Good Life (1988) | 121 | Mi mineur | Tunebat (remaster 2024) |
| 6 | Black Box – Ride on Time (1989) | ≈ 119 | La mineur | Mixmeister (archive UBC) |
| 7 | Crystal Waters – Gypsy Woman (1991) | ≈ 120 (nv) | – | Wikipedia (pas de BPM) |
| 8 | CeCe Peniston – Finally (1991) | 120 | Si mineur | Beatport (7" mix) |
| 9 | Robin S – Show Me Love (Stonebridge mix, 1993) | 120 | La mineur (Mixmeister) / Fa majeur (songbpm, relatif) | archives UBC, songbpm |
| 10 | Armand Van Helden – U Don't Know Me (1999) | ≈ 126 (nv) | – | – |

Plage mesurée : **119–125 BPM** (notre 122 est juste au milieu). Tonalités : mineur dominant (Mi m, La m, Si m) ; Hooktheory donne pour Move Your Body une boucle **i–IV–i–iv–v** en Mi mineur avec une mélodie de piano entre La3 et Sol4 (10 demi-tons de registre seulement).

### Ce que font ces disques (sources)

- **Batterie** : 909 (et 707/DMX sur les plus anciens). Attack « Classic 90s NY/NJ house » : 120–125 BPM, **swing 70–80 %**, kick 4/4 avec **un kick en plus à contretemps en fin de 2e mesure**, clap sur 2 et 4 **avancé de quelques ms** et tiré encore plus en avant sur le 4 de la 2e mesure (le « jack »), charley ouvert 909 à chaque contretemps dont la longueur est réglée pour « glisser dans le clap », quelques charleys fermés **à vélocités variées** (« la clé du swing »), un 2e clap discret en 2e mesure, **crashs libéralement sur les tournes de 16 et 32 mesures**, bus batterie en saturation bande + compresseur SSL. Attack « 21st century Chicago house » (123 BPM, swing 55–60 %) : 707 kick baissé d'un ton, charleys fermés en doubles-croches **avec vélocités légèrement différentes**, **tambourin 707 à chaque contretemps en deux copies désaccordées pannées dur gauche/droite**, rim 707 autour des contretemps, un kick de plus **à faible vélocité** sur le dernier contretemps d'une section (tourne).
- **Accords (piano / orgue M1)** : Attack « Passing Notes » (stabs house, 120 BPM) : boucle de **8 mesures** Cm7 ×2 / Gm7 ×2 / Cm7 ×2 / Gm7 / **Em7** (« changer l'accord de la 8e mesure rend instantanément la progression accrocheuse »), voicings **sans fondamentale** (la basse l'a), Gm7 en **3e renversement** (« très utilisé par Kerri Chandler »), 4 ajoutée en cluster ou 9 en note du haut ; rythme **syncopé qui évite le 1er et le 4e temps, sur les doubles-croches en l'air**, mélange de **coups tenus et de coups courts** = question/réponse entre stabs, 4e mesure variée, 8e mesure qui ramène au début ; petites mélodies d'octaves entre les coups.
- **Lead / hook** : Your Love = séquence de double-croches sur synthé (arpège de 16 notes sur 2 mesures, répété tout le morceau) ; Show Me Love = basse d'orgue M1 **une octave qui saute** + vocal ; Move Your Body = piano « jouant » le riff en octaves sur 8 mesures, registre d'une dixième. Hook vocal d'un mot ou d'une ligne (« Show me love », « Finally », « la da dee la da da »), Wikipedia Gypsy Woman : le refrain est **une suite de syllabes sans sens sur la basse** ; critique : clavier « en morse », « piqué façon italo ».
- **Basse** : octaves (M1 Organ Bass), ou note sur chaque contretemps ; sidechain discret.
- **Forme** (forum/Mixed In Key/KVR) : blocs de 16/32 : Intro 16–32 · Build 16 · **Corps 32** · Break 16 · Build 16 · Corps 32 · Outro 16–32 ; des sections de 8 mesures où une couche entre ou sort ; le premier drop vers 1:00–1:15 à 122 BPM.
- **Sonie** (Mastering The Mix) : club −6 à −9 LUFS, TP −0,1 à −1 ; deep house un peu plus doux (−8 à −10).

### Notre recette et les écarts (impact décroissant)

Recette : kick `X...X...X...X...X...X...X...X.x.` (bien : le « jack ») ; clap `....X.......X...` ; open hat `..X...X...X...X.` ; hat `xo.oxo.oxo.oxo.o` ; shaker 707 `..x...x...x...x.` ; ride `..o...o...o...o.` et conga au Drop ; swing 0,15 ; `root 9 (La)`, Happy majeur `[0,3,5,4]` (I–IV–vi–V), Dark `[0,0,5,6]` ; `chordRhythm "X..X..X...X..X.."`, `Seventh`, octave 3 ; basse `R..R..O.R..R..O.`, Juno ; lead `Arpège`, `leadPattern ""`, `arpRate 24`.

1. **Le lead est un arpège mécanique de triade** (`ArpMode.UpDown`, une note par double-croche, vélocité 0,65, 16 mesures sans changement). Your Love est une *séquence* fixe de 2 mesures avec une forme (sauts, notes répétées), pas un va-et-vient de triade. **Changement** : `leadPattern` de 32 pas écrit comme riff (ex. `R.O.5.O.R.O.3.O.|R.O.5.O.7.O.5.3.`), vélocités alternées (majuscule/minuscule) et, dans le générateur, **une variante toutes les 8 mesures** (mesures 7–8 : fin différente, ou silence). Mesure : au moins 2 motifs de lead distincts par Drop ; aucune suite de 16 doubles-croches identiques en vélocité.
2. **Accords : 4 accords en 4 mesures, position fondamentale, même rythme à chaque mesure.** Les hits tournent sur **2 accords** (ou 8 mesures avec un accord surprise en 8e), voicings renversés sans fondamentale, rythme syncopé qui **évite le 1er temps** et varie à la 4e et 8e mesure. **Changement** : (a) permettre une progression de 2 ou 8 mesures dans la recette (`degrees` de longueur 2 → chaque accord 2 mesures ; 8 → 8 mesures) ; (b) nouveau `Voicing.Rootless7` (3, 5, 7 + 9 en haut) et `Voicing.Inv3` ; (c) `chordRhythm` de 64 pas (4 mesures, 4e mesure différente), avec `-` pour les tenues : ex. `..X-..X...X.X...|..X-..X...X.X...|..X-..X...X.X...|..X-..X.X-..X.X.` ; (d) vélocité par coup (X 0,9 / x 0,65 / o 0,45) au lieu de 0,75 fixe.
3. **Aucun décalage temporel ni vélocité humaine** : clap en avance de 3–5 ms, kick de tourne « à vélocité nettement plus basse », charleys fermés à vélocités variées sont *la* recette Attack. **Changement** : dans `StarterWizard.build`, graine fixe par projet : charleys/percus ±8 % de vélocité, clap −4 ms, percus +3 ms ; le kick « jack » (dernier `x`) à 0,45 au lieu de 0,6. Mesure : écart-type des vélocités de charley ≥ 0,05 (critère déjà prévu dans son.md étape 6).
4. **Pas de crash ni de variation de 8 mesures** : les hits mettent des crashs « libéralement » sur les tournes de 16/32 et changent une couche tous les 8. **Changement** : crash (voix cymbale) au 1er pas de chaque Drop et de chaque bloc de 16 ; `songOf` alterne scène / scène « jumelle » (M4 de CORPS) ; dans un Drop de 16 : mesures 1–8 sans ride, 9–16 avec ride + conga (déjà `drop` layers → les répartir en deux moitiés comme `dropTwoLane` le fait pour le lead).
5. **Tambourin/percus en mono au centre** : Attack panne deux copies désaccordées dur gauche/droite. **Changement** : `Role.Perc` → pan ±0,35 existe ; ajouter une 2e copie désaccordée (+7 cents) de l'autre côté, ou l'élargisseur sur le pad de la percu.
6. **Hook vocal absent** : 8 hits sur 10 ont un mot ou une syllabe répétée. **Changement** : coche `Lead` sur `vocal_chops:vocal` en 2e Drop (comme tech house), motif `R.......R..R....` sur 2 mesures puis silence 2 mesures (appel/réponse avec le piano).
7. **Swing 0,15 est faible** pour la house NY/NJ (70–80 % MPC ≈ 0,4–0,6 sur notre échelle 0–0,75) mais juste pour Chicago (55–60 %). **Changement** : 0,25 (et 0,2 sur la ride/percus seulement si le moteur le permet par piste).

---

## 2. Deep house (`deep_house` · 120 BPM)

### 10 morceaux

| # | Artiste – Titre (année) | BPM | Tonalité | Source |
|---|---|---|---|---|
| 1 | Mr. Fingers – Mystery of Love (1985) | ≈ 118 (nv) | – | – |
| 2 | Mr. Fingers – Can You Feel It (1986) | ≈ 116 | La mineur | AudioKeychain (base éditable) ; Juno-60 + TR-909 (Wikipedia) |
| 3 | Masters at Work – To Be In Love (MAW Dub, 1997) | ≈ 124 (nv) | Fm7–A♭maj7–Cm7 | Attack « deep house chords » |
| 4 | Kerri Chandler – Rain (1998) | ≈ 124 (nv) | – | – |
| 5 | Pépé Bradock – Deep Burnt (1999) | ≈ 120 (nv) | – | – |
| 6 | Kerri Chandler – Bar A Thym (2005) | 126 | Si♭ mineur | AudioKeychain ; Attack (« mouvement parallèle ») |
| 7 | Larry Heard – The Sun Can't Compare (2006) | ≈ 120 (nv) | – | – |
| 8 | Disclosure – Latch (2012) | 122 | Do♯ majeur | songbpm |
| 9 | Route 94 – My Love (2014) | 120 | – | songbpm |
| 10 | Black Loops – Under the Sea (2019, référence EDMProd) | 125 | Si mineur (projet du guide) | EDMProd |

Plage : **116–126 BPM** ; Attack « Rolling deep house » 125 (swing 60–70), « Dusted » 120–125 (swing 70–80 %), « Stripped workout » 120–125 (swing 50–60).

### Ce que font ces disques

- **Accords** : Attack « Kerri Chandler chords » : des boucles d'**une mesure à 2 accords** (D♯m7 → G♯m7 ; D♯m7(11) → Bm(13)), **7e en 3e renversement** (7e à la basse), **quintes omises**, 11e et 13e à la place, **fondamentale omise** (« la basse s'en charge »), le dernier stab de la mesure **revient tôt** sur l'accord 1 (anticipation) ; progressions tenues de 3 accords en **mouvement parallèle** (même forme déplacée : G♯m7 → D♯m7 → C♯m7, « Bar A Thym »), accord emprunté hors tonalité (A7add9) comme transition, voicings qui bougent par demi-tons. Conseils : **la basse ne tombe pas sur la fondamentale au 1er temps** ; alterner stabs et tenues. MusicRadar : accord min9 (5 notes) ouvert en montant les 2e et 3e notes d'une octave. Attack « deep house chords » : Am7 → Em7 → Dm7 sur Rhodes, 7e déplacée d'une octave pour que la mélodie Mi–Ré–Do ressorte.
- **Batterie** : 909, kick 4/4 ; **shaker en doubles-croches** qui prend le rôle du charley fermé (mute group), **charley à faible vélocité sur la 10e division** (= le shuffle), **tom 909 sur les divisions 3, 9, 11** (« rolling »), percu accordée sur le tom aux divisions 2 et 10 ; « Dusted » : kicks fantômes à faible vélocité, clap 808/909 avancé de quelques ms, charleys ouverts 808 courts sur chaque contretemps + charleys fantômes, **stab d'accord m7/m9 échantillonné en one-shot placé sur le 1er contretemps de chaque mesure**, boucle de percus très basse. EDMProd : groove « MPC4000 16 Swing 61 », deux claps superposés, charley fermé sur les contretemps doublés par endroits, open hat ajouté en 2e moitié de drop, **fill tom + caisse** entre sections A→B, répété 8 mesures plus loin, kick coupé comme signal.
- **Basse** : EDMProd : sub Serum, **notes entre les kicks**, même groove que la batterie, mono/legato, enveloppe courte, saturation, sidechain au kick (attaque/relâche rapides). Kerri : basse active contre des nappes tenues, évite la fondamentale au début.
- **Voix** : un **seul chop vocal court** avec réverbe et écho (EDMProd) ; Can You Feel It : spoken word optionnel.
- **Forme** (EDMProd, 125 BPM) : sections de 8 mesures, intro charleys + fond, break = beat sans kick, filtre qui s'ouvre.

### Notre recette et les écarts

Recette : kick `X...X...X...X...`, clap doux, hat `o.Xoo.Xoo.Xoo.Xo`, percu `...o..x...o..x.o`, tom 909 `..x.....x.x.....` (bien : divisions 3, 9, 11 ✓), shaker `oxooxoxooxooxoxo` au Drop ; swing 0,2 ; `root 5 (Fa)`, Dreamy dorien `[0,3,6,4]`, `Ninth`, `chordRhythm "..X..X....X..X.."`, octave 4 ; basse `..R-..R-..R-.rO-` ; lead `Voix robot` / vocal chops, `leadPattern "..R.....5..3...."`, octave 5.

1. **Voicing `Ninth` = 5 notes empilées en position fondamentale, avec la fondamentale et la quinte, à l'octave 4** : l'inverse des voicings Chandler (3e renversement, sans fondamentale ni quinte, 11/13). **Changement** : `Voicing.KerriStab` = scale steps `[6, 7, 9, 11]` relatifs (7e, fondamentale, 3e, 5e en haut) ou mieux `[2, 4, 6, 8]` sans fondamentale (3, 5, 7, 9) et `chordOctave 3` ; tester les deux à l'oreille. Mesure : la note la plus grave de l'accord ≥ 1 octave au-dessus de la basse, aucune fondamentale doublée.
2. **4 accords différents en 4 mesures** : les hits tournent sur 2 accords dans une mesure, ou 3 en mouvement parallèle. **Changement** : `progressions` Dreamy → `[0, 3]` sur une mesure chacun (ou `[0,0,3,3]`), avec le **dernier stab de la mesure qui anticipe** l'accord suivant : `chordRhythm` 32 pas `..X..X....X..x.A|..X..X....X..x.A` où `A` = accord de la mesure suivante (nouveau jeton du convertisseur), ou plus simple : `..X..X....X..x..` + une tenue `X---` en 4e mesure.
3. **La basse tombe sur la fondamentale à chaque fois et joue la même mesure 16 fois** : Kerri évite la fondamentale au départ ; EDMProd la cale entre les kicks avec le groove. **Changement** : `bassPattern` 32 pas `..R-..R-..R-.rO-|..5-..R-..R-.r3-` (réponse en 2e mesure sur la quinte/tierce) ; vélocité `r` déjà 0,63 ✓.
4. **Lead « Voix robot » sur `..R.....5..3....` à chaque mesure** = un chop vocal toutes les mesures, sans écho rythmique. Les hits : un seul chop par 2 ou 4 mesures, long écho. **Changement** : `leadPattern` 64 pas : chop en mesure 1 (`..R-----........`), silence 2–3, réponse en 4 (`........5--3----`) ; `delay 0,35` ✓ existant, ajouter `reverb 0,3`.
5. **Pas de fill tom/caisse à 8 mesures ni de kick coupé en signal** (seul le roulement de 4 mesures de `WizardMix.phrase`). **Changement** : fill de tom 909 (`Role.Tom` existe) en fin de mesure 8 et 16 : `............x.xx` sur le tom + kick muet sur le dernier temps ; en Break, garder tom + shaker (déjà `breakWith` ✓).
6. **Swing 0,2 et vélocités fixes** : Attack met le swing à 60–80 % et un charley « à faible vélocité sur la 10e division ». Notre hat a `o` (0,35) sur les pas 1, 3, 4… mais le pas 9 (10e division) est `o` dans `o.Xoo.Xoo.Xoo.Xo` — c'est bon ; manque le ±8 % aléatoire et un kick fantôme (`o` au pas 14 une mesure sur 4).
7. **Largeur** : nappe `wide_alive` ✓, mais stabs de piano électrique mono. **Changement** : `WIDENER` 0,5 sur le Pad même en stabs (WizardMix ne l'ajoute que si `holds`).

---

## 3. Tech house (`tech_house` · 126 BPM)

### 10 morceaux

| # | Artiste – Titre (année) | BPM | Tonalité | Source |
|---|---|---|---|---|
| 1 | Green Velvet – La La Land (2001) | ≈ 128 | La mineur (« Pill Song », Mixmeister) | Mixgraph, archive Waterloo |
| 2 | Eats Everything – Entrance Song (2011) | ≈ 125 (nv) | – | – |
| 3 | Patrick Topping – Forget (2014) | 123 | Sol majeur | Beatport, AudioKeychain |
| 4 | Camelphat & Elderbrook – Cola (2017) | 122 | Fa♯ | songbpm |
| 5 | Fisher – Losing It (2018) | 125 | Ré | songbpm |
| 6 | Chris Lake – Turn Off the Lights (2018) | 125 | Si mineur | songbpm |
| 7 | Dom Dolla – Take It (2018) | 123 | Do | songbpm |
| 8 | Michael Bibi – Hanging Tree (2018) | ≈ 126 (nv) | – | – |
| 9 | Mau P – Drugs From Amsterdam (2022) | 125 (ext.) / 128 | 5B / 4B (Camelot) | Mixgraph, Mixprism |
| 10 | John Summit – Where You Are (2023) | ≈ 126 (nv) | – | – |

Plage : **122–128 BPM** (EDMProd : 125–128, l'auteur à 126 ✓ notre 126). Tonalités : mineur (Fm, F♯m, Gm conseillées par EDMProd ; phrygien « pour un côté sombre »).

### Ce que font ces disques

- **Forme (EDMProd, « like FISHER »)** : marqueurs tous les **8 mesures** ; **Intro 1 : 16, Intro 2 : 16 (un élément en plus)** = 32 pour le DJ ; **Break + build : 8** ; **Drop 1 : 32 = A/B/C/D de 8** ; re-break + build ; Drop 2. Dans le drop : **crash + réverbe + délai au 1er temps**, **riser à la fin de B**, **kick et basse coupés les 2 dernières mesures de B**, **shakers ajoutés en C et D**, « double kick » dans les mesures 56–57, dernier kick retiré avant le drop. Beatportal : intro 0:00–0:30, break 0:30–0:45 (la basse et le hook arrivent **dans le break**), drop 0:45, fills/roulements **tous les 8 ou 16**.
- **Basse** : EDMProd : « Square Pure Bass » Analog, **passe-bas agressif à ~150 Hz** (4× pente), **rythme syncopé à contretemps, legato + glide pour les glissés**, sidechain (LFO Tool/Kickstart). Beatportal : sinus/carré simple, « deep, funky, repetitive » ; Mau P : hook chanté au dictaphone, **sinus hypnotique** pour les 2e et 3e drops, « eerie, ravey, dirty ».
- **Lead/stabs** : saw + saw à l'octave, filtre LP avec enveloppe, distorsion, **quinte superposée**, réverbe ; stabs avec **octaves et quintes + chorus** ; **mélodie variée en 2e partie du drop** ; Beatportal : stabs courts, secs, « filtre qui s'ouvre progressivement » en build.
- **Voix** : phrases **chopées, calées sur la grille**, passe-haut, compression lourde + gate, OTT, **pitch down**, **couche parallèle distordue + couche une octave en dessous** ; hook d'un mot (« Losing it », « Cola », « Take it », « Drugs from Amsterdam »).
- **Batterie** : clap 2 et 4 **en deux couches** (claquant + brillant) limitées en groupe ; charleys à contretemps en MIDI pour la souplesse ; percus = one-shots + **boucles filtrées** avec **léger swing hors grille et vélocités variées** ; Attack (beat tech house) : swing 62 %, open hat sur les contretemps avec charley fermé sur la double-croche qui précède, **vélocité aléatoire**. Attack « Deep tech house » (125–130, swing 60–65 %) : shaker 16e à vélocités variées, caisse + clap 707 passe-haut 120 Hz sur pistes séparées (pour les muter/réintroduire), **caisse fantôme aiguë à faible niveau à des positions inhabituelles**, bus SSL ≈ 4 dB de réduction.

### Notre recette et les écarts

Recette : kick `X...X...X...X...` + `punch`, clap 2 mesures avec écho `o`, hat `ox.xox.xox.xox.xox.xox.xox.xo3..`, open hat, rim 2 mesures `...x..x....x..x....x..x...x..x..`, shaker `ooxoooxooox.ooxo`, ride au Drop ; swing 0,15 ; `root 7 (Sol)`, Dark mineur `[0,0,0,0]` ; orgue `...X......X.....` ; basse `..Rr..Rr..Rr..RO..Rr..Rr..Rr.5/O` glissé + sub ; lead vocal chops `R..R......R.....`, octave 4 ; `sweeps bass` au Break ; `drop` = open hat + ride ; `breakWith` = perc, hat, shaker + basse + lead.

1. **La basse (la signature) ne bouge pas de hauteur pendant 16 mesures** : `..Rr..Rr..Rr..RO` répète la fondamentale ; les hooks de basse tech house ont une forme de 2–4 mesures (fondamentale, 7e mineure, quinte, saut d'octave, glissé en fin de phrase). **Changement** : `bassPattern` 64 pas, ex. `..Rr..Rr..Rr..RO|..Rr..Rr..7r..5/|..Rr..Rr..Rr..RO|..Rr..3r..5r.O/R` (jetons existants, `/` = glissé) ; en Drop 2, la même ligne **une octave plus haut** sur les mesures 3–4 (automation ou 2e piste). Mesure : ≥ 3 hauteurs distinctes dans la ligne, ≥ 1 glissé par 4 mesures.
2. **Drop = Groove + 2 couches** ; EDMProd découpe le drop en A/B/C/D, coupe kick + basse 2 mesures avant C, ajoute les shakers en C/D. **Changement** : dans `inScenes`, le Drop de 16 mesures = 2 scènes de 8 : A/B (sans ride, sans shaker) puis C/D (tout) ; kick + basse muets sur les 2 dernières mesures de B (comme « the last kick bar removed ») ; crash au 1er pas de C. Mesure au banc : Drop − Groove ≥ +2 LU et C/D − A/B ≥ +1,5 LU.
3. **Pas de hook vocal travaillé** : nos chops jouent `R..R......R.....` toutes les mesures à la même hauteur. **Changement** : `leadPattern` 32 pas `R..R......R.....|................` (appel 1 mesure, silence 1 mesure) + jeton `r` (pitch −3 demi-tons) en réponse une mesure sur 4 ; `delay 0,3` ✓ ; ajouter une 2e piste « voix grave » = mêmes notes −12 (comme la piste `Sub` pour la basse) à −6 dB.
4. **Clap sur une seule couche, caisse fantôme absente** : Attack met une caisse fantôme aiguë à faible niveau à des positions inhabituelles, deux claps. **Changement** : `Role.Snare` (rim, voice 10) `.......o.....o..` à 0,3 en plus du clap ; clap doublé par un 2e échantillon brillant (pad voisin) −6 dB.
5. **Swing 0,15 vs 60–65 %** (≈ 0,3 sur notre échelle) sur les charleys/shaker. **Changement** : `swing 0,25`, et ±8 % de vélocité sur hat/shaker.
6. **Lead sans quinte ni octave superposées** : stabs tech house = saw + octave + quinte + chorus. **Changement** : préréglage « Stab tech » dans `SoundLibrary` (UNISON 0,5, OSC2 octave, chorus) et, pour le `counter` (piste `Contre-chant`), les mêmes notes +7 demi-tons à −9 dB.
7. **Intro 16 mesures sans 2e palier** : EDMProd veut 16 + 16 avec un élément en plus. Notre `sectionsOf` donne Intro 16 · Groove 16 : c'est bien l'équivalent ✓ si le Groove ajoute 1 couche seulement (il ajoute tout sauf open hat/ride : trop). **Changement** : `drop` élargi à `[open_hat, ride, perc2]` et la basse qui n'entre **qu'au Break** (Beatportal : « la basse et le hook arrivent dans le break ») : `drop = [bass]` + `breakWith` garde `bass` ✓ existant.

---

## 4. UK Garage (`uk_garage` · 132 BPM)

### 10 morceaux

| # | Artiste – Titre (année) | BPM | Tonalité | Source |
|---|---|---|---|---|
| 1 | Tina Moore – Never Gonna Let You Go (Kelly G Bump-N-Go mix, 1997) | 122 (dub Beatport) | Ré mineur | Beatport ; DJ Mag (« blueprint du 2-step ») |
| 2 | MJ Cole – Sincere (1998/2000) | ≈ 130 (nv) | – | – |
| 3 | Artful Dodger ft. Craig David – Re-Rewind (1999) | ≈ 132 (nv) | – | Wikipedia (synth bass, voix filtrées) |
| 4 | Shanks & Bigfoot – Sweet Like Chocolate (1999) | 130 | Ré mineur, i–VII–VI | Hooktheory ; Mixmeister Sol m (autre mix) |
| 5 | Sweet Female Attitude – Flowers (Sunship edit, 2000) | 132 | Ré♯ mineur (Mixmeister) | songbpm, archive UBC |
| 6 | Wookie – Battle (2000) | 130 | Do mineur | songbpm |
| 7 | Craig David – Fill Me In (2000) | 132 | La♭ | songbpm |
| 8 | So Solid Crew – 21 Seconds (2001) | 137 | – | songbpm |
| 9 | Daniel Bedingfield – Gotta Get Thru This (2001) | ≈ 135 (nv) | – | – |
| 10 | Eliza Rose & Interplanetary Criminal – B.O.T.A. (2022) | ≈ 134 (nv) | – | – |

Plage : **130–137 BPM** (NI : « typiquement ~130 », MusicRadar/Sample Focus 127–135, Producer School 130–135 ; notre 132 ✓). Tonalités mineures (Ré m, Do m, Ré♯ m) ; Hooktheory : mélodie de Sweet Like Chocolate entre Ré4 et Do5 (10 demi-tons).

### Ce que font ces disques

- **2-step (NI, 132 BPM, Battery)** : kick **sur le 1 et à la moitié du 3** (pas 0 et 10) ; caisse 2 et 4 ; **open hats à la moitié des temps 1, 2 et 4** (pas 2, 6, 14 — pas sur le 3 !) ; charleys fermés **« légèrement en retard » après les 2e, 4e, 6e doubles** + sur les temps 1 et 3 ; boucle de charleys superposée à −14 dB. Producer School : 2e kick **poussé légèrement en avance**, caisse **un poil en retard**, « kick et caisse ne sont pas sur la grille », swing MPC/707/909 comme référence, shakers/tambourin + notes fantômes ; Sample Focus : swing 50–60 %, 2e caisse autour pour accentuer les contretemps. Compression parallèle du bus, saturation légère.
- **Basse** : NI : Massive « Away », **« Dred » bass façon jungle, 3 notes sur 4 mesures bouclées sur 8**, référence RipGroove « le filtre qui s'ouvre fait sonner la basse à l'envers » ; Sample Focus : wobble + filtre **+ un sinus pur 1–2 octaves en dessous sans effet** ; Producer School : **syllabe vocale pitchée −12/−24 et filtrée**, **enveloppe de hauteur descendante au départ de chaque note = le « wub »**, couche médium brillante, sidechain léger, mono < 80–100 Hz.
- **Accords** : Producer School : piano/Rhodes **chaud et un peu feutré**, **7e et 9e simples**, stabs de piano ; nappes de cordes attaque lente, en retrait ; LiveSchool : « chord planing » (parallèle) commun en UKG. Organ : pas de source trouvée (nv) — l'orgue de notre recette est une hypothèse plausible, pas sourcée.
- **Voix** : Todd Edwards (Wikipedia, Napier) : **micro-samples, phrases jouées comme un instrument, syllabes inversées ou pitchées**, « honeycomb de hoquets » ; Producer School : chops **pitchés −3 à −5 demi-tons comme contre-mélodie / appel-réponse**, timing lâche ; Re-Rewind : « voix bégayées, filtrées ».
- **Forme** (NI) : blocs de **8**, **beat coupé une mesure avant le drop**, **drop mesure 25**, l'arpège de synthé **remplacé par la voix au drop**, couches ajoutées/retirées après, **outro sans basse à partir de la mesure 73**. Producer School : Intro 8–16 · Build 8 · Drop 16–32 · Break 8–16 · Drop 2 (variation) · Outro 8–16.

### Notre recette et les écarts

Recette : kick `X.........X.....X.........X....o` ✓ (pas 0 et 10), rims 2 mesures `....x..o....x.......x..o...ox.o.`, clap 2/4, hat `xo.o.o..xo.o.o..`, open hat `..x...x.......x.` ✓ (pas 2, 6, 14 : exactement NI), shaker `ooxoooxoooxoooxo` ; swing 0,3 ✓ ; `root 9 (La)`, Happy majeur `[1,4,0,5]` ; orgue `Seventh` `...X..X....X..X.`, octave 4 ; basse `R--....R..R--.O-` ; lead vocal chops `..R..5....R.3...` ; forme chanson.

Déjà bon : le 2-step, l'open hat, le swing, les rims. Les écarts sont surtout mélodiques et dans la forme.

1. **Pas de « wub » ni de sub pur** : la basse de garage = enveloppe de hauteur descendante à l'attaque + sinus en dessous ; notre `uk_garage:bass` / « Basse ronde » n'a ni l'un ni l'autre (`subBass` est false). **Changement** : `subBass true` (piste `Sub` existante) ; préréglage « Basse dred » (STYLE-FIXES F10 : attaque de filtre lente, FILTER_ENV élevé) + SWEEP de hauteur court (−5 demi-tons sur 40 ms) si le synthé a une enveloppe de pitch (sinon pitch LFO une fois : `SWEEP`). Mesure : spectre de la basse < 60 Hz ≥ −6 dB rel.
2. **Les chops vocaux rejouent la même cellule chaque mesure à la même hauteur** ; Todd Edwards/Producer School : syllabes pitchées −3/−5 en réponse, timing lâche, syllabes inversées. **Changement** : `leadPattern` 64 pas avec appel (mes. 1 : `..R..5..........`), réponse pitchée (mes. 2 : `........r..3....` minuscule = −3 demi-tons et −30 % vélocité : nouveau sens du jeton minuscule pour les chops), mes. 3 = mes. 1, mes. 4 variée ; ±10 ms de jitter sur les chops.
3. **Refrain = couplet + 3 couches** mais aucun **« beat coupé une mesure avant le drop »** ni **« la voix remplace l'arpège au drop »**. **Changement** : en forme chanson, `WizardMix.phrase` coupe **toute la batterie** sur la dernière mesure avant chaque Refrain (pas seulement le kick sur le dernier temps) ; le Couplet joue un arpège d'orgue (`Part.Lead` en arpège), le Refrain les chops (deux `leadPattern` : `verse` et `chorus` dans la recette).
4. **Accords d'orgue à l'octave 4 en position fondamentale** vs piano feutré 7/9 et chord planing. **Changement** : `chordOctave 3`, `Voicing` sans fondamentale (voir deep house), son « Piano électrique » plutôt qu'« Orgue » par défaut (l'orgue reste proposé) ; `chordRhythm` 32 pas avec une tenue en 2e mesure : `...X..X....X..X.|...X--....X-..x.`.
5. **Micro-timing** : 2e kick en avance, caisse en retard, charleys « légèrement en retard ». Le swing global décale les doubles-croches paires ; il ne décale **ni le kick du pas 10 ni la caisse**. **Changement** : décalage par ligne dans `build` : kick pas 10 −6 ms, caisse/clap +4 ms, open hat +8 ms.
6. **Outro** : NI finit **sans basse** sur 8+ mesures ; notre outro = Intro (hat + signature). ✓ proche ; rien à faire.

---

## 5. Speed garage (`speed_garage` · 130 BPM)

### 10 morceaux

| # | Artiste – Titre (année) | BPM | Tonalité | Source |
|---|---|---|---|---|
| 1 | CJ Bolland – Sugar Is Sweeter (Armand Van Helden mix, 1996) | ≈ 130 (nv) | – | Wikipedia (« premier vrai speed garage ») |
| 2 | Sneaker Pimps – Spin Spin Sugar (Armand's Dark Garage mix, 1997) | ≈ 130 (nv) | – | Wikipedia, Vinyl Factory |
| 3 | Double 99 – RipGroove (1997) | ≈ 127 (radio edit) / 128 | Fa mineur | Mixmeister (archive), getsongbpm |
| 4 | 187 Lockdown – Gunman (1997) | 128 | – | songbpm |
| 5 | Tina Moore – Never Gonna Let You Go (Kelly G, 1997) | 122 (dub) | Ré mineur | Beatport |
| 6 | Serious Danger – Deeper (1997) | ≈ 130 (nv) | – | – |
| 7 | Industry Standard – What You Want (1998) | ≈ 130 (nv) | – | – |
| 8 | 187 Lockdown – Kung-Fu (1998) | ≈ 130 (nv) | – | Wikipedia (n° 9 UK) |
| 9 | T2 ft. Jodie Aysha – Heartbroken (2007, bassline 4×4) | ≈ 140 (nv) | – | – |
| 10 | Interplanetary Criminal & Sammy Virji – Damager (2024) | ≈ 135 (nv) | – | Wikipedia (revival 2020s) |

Plage : **127–130** pour 1997 (Wikipedia : « la garage house jouée à 130 devint le speed garage » ; notre 130 ✓), **135–140** pour le revival et le bassline (Producer School : 138–140).

### Ce que font ces disques

- **Caractère** (Wikipedia) : rythme NY garage accéléré + breakbeats, caisses « agitées, claquantes » hors du kick, **basses gauchies, lourdes, influencées par la jungle et le reggae, « sweeping »**, **un breakdown où le beat est dépouillé puis remonte**, voix time-stretchées, **effets jungle/dub (coups de feu, sirènes)**, vocals ragga, spin-backs, batteries inversées. Vinyl Factory/Wikipedia : Van Helden en est l'architecte (« gros breakdown + bassline »).
- **Basse** (Producer School, 140 BPM) : « Reese » = **2 saws dont une −38 demi-tons, filtre très résonant**, distorsion **automatisée par macro**, **sauts d'octave (F5→F6) et glissé à la transition**, 2e couche de 2 sinus désaccordés passe-haut pour les petites enceintes ; « W swell » : carré + enveloppe sur le cutoff (attaque lente), tube, sidechain ; NI (UKG) cite RipGroove : **filtre qui s'ouvre = basse « à l'envers »**. Undrgrnd Sounds (130–135) : syllabe vocale pitchée −12/−24 + enveloppe de pitch descendante (« wub »).
- **Batterie** : kick 4/4 deep + distorsion légère, **top loop de caisses skippy à contretemps**, open hats sur les contretemps, **breakbeat passe-haut sous le pattern**, texture vinyle, **crash au 1er temps du drop + crash fill aux changements**.
- **FX / drop** : lead répétitif désaccordé sur la fondamentale, **sirène**, **downlifter de bruit blanc entre les deux moitiés du drop**, **backspin**, laser, **boucle vocale en 2e moitié du drop**, **kick inversé en sweep-in** à la 2e section.
- **Forme** : voir UKG (Intro 8–16, Build 8, Drop 16–32, Break 8–16, Drop 2, Outro).

### Notre recette et les écarts

Recette : kick 4/4, caisse `....X.......X.......X..o....X.o.`, hat `xo.oxo.oxo.oxo.o`, open hat, percu, shaker au Drop ; swing 0,25 ; `root 7`, Dark `[0,0,5,4]` ; orgue `...X......X.....` octave 3 ; **basse « Basse qui parle »** `R-----.R--/O-.R-R-----.R--/5-.O-` glissé + sub ✓ ; lead vocal chops `R.......R..R....` ; `drop = [perc2, bass]` ✓ (la basse tombe au Drop).

1. **La basse est bonne sur le papier mais sans l'ouverture « à l'envers » ni l'automation de distorsion** : notre `Basse qui parle` a un LFO2 synchronisé (½) sur le filtre (le « ouaou ») ; les hits : attaque de filtre lente (le « W swell »), saut d'octave en fin de phrase, drive qui monte sur le drop. **Changement** : `sweeps bass` déjà possible → ajouter `Sweep("bass", {1: 0.25→0.6, 2: 0.6→0.9})` (filtre qui s'ouvre sur Groove puis Drop) ; préréglage « Basse dred » (F10) ; `bassPattern` 64 pas avec saut d'octave en 4e mesure : `...|R-----.R--/O-.O-O-----.O--/5-.R-`.
2. **Aucun effet de drop** (sirène, backspin, downlifter, kick inversé) : notre souffle blanc 4 mesures est le seul FX. **Changement** : pack « FX de transition » (son.md étape 6) + dans `inScenes` : downlifter au milieu du Drop, kick inversé (sample retourné de la voix kick, 1 temps) avant chaque Drop, une sirène (`Sirène` existe dans SoundLibrary) 2 mesures en 2e moitié de Drop 2.
3. **Pas de top loop de caisses skippy ni de breakbeat sous le pattern** : notre caisse a 2 fantômes. **Changement** : ligne `Role.Snare` plus skippy sur 2 mesures `....X..o..o.X..o....X..o.o..X.o.` (fantômes à 0,3) ; `Role.Tom` en « breakbeat » passe-haut (`.x..x..x.x..x...` voix 8 à −14 dB, filtre HP 400 Hz via `StyleFx("tom", insert = "FILTER")`).
4. **Chops vocaux à la même hauteur chaque mesure** (voir UKG) ; les hits time-stretchent et ragga-isent : **Changement** : `leadPattern` 32 pas appel/réponse + `r` pitché ; **boucle vocale seulement en 2e moitié du Drop** (= `dropTwoLane` ✓ si Lead n'est pas signature : vérifier que `signature = Bass` laisse le lead au Drop 2 — oui, `dropTwoTrack` prend le Lead quand il n'est qu'au Drop).
5. **Crash absent** au 1er temps du drop et aux changements. **Changement** : commun (voir synthèse).
6. **Vélocités/micro-timing** : commun.

---

## 6. Acid house (`acid_house` · 120 BPM)

### 10 morceaux

| # | Artiste – Titre (année) | BPM | Tonalité | Source |
|---|---|---|---|---|
| 1 | Sleezy D – I've Lost Control (1986) | ≈ 120 (nv) | – | – |
| 2 | Phuture – Acid Tracks (1987) | 119–120 (≈128–130 à l'origine, ralenti par M. Jefferson) | – | Wikipedia, songbpm, Shazam, Line Noise |
| 3 | Armando – Land of Confusion (1988) | ≈ 120 (nv) | – | – |
| 4 | Bam Bam – Where's Your Child (1988) | ≈ 122 (nv) | – | – |
| 5 | A Guy Called Gerald – Voodoo Ray (1988) | 120 | ≈ Ré (songbpm) | songbpm ; RBMA (808, SH-101, 303, S-950) |
| 6 | D Mob – We Call It Acieed (1988) | ≈ 122 (nv) | – | – |
| 7 | Humanoid – Stakker Humanoid (1988) | ≈ 124 (nv) | – | – |
| 8 | Lil Louis – French Kiss (1989) | 124 | – | songbpm |
| 9 | Hardfloor – Acperience 1 (1992) | 125 | – | songbpm |
| 10 | Josh Wink – Higher State of Consciousness (1995) | ≈ 126 | Sol mineur (Mixmeister) / Do m (AcousticBrainz) | archive Waterloo |

Plage : **119–126 BPM** (notre 120 ✓). Acid Tracks dure **12:15** ; French Kiss 8:04 ; Acperience 9:03 : des morceaux longs dont la matière est **la variation lente d'une seule ligne**.

### Ce que font ces disques

- **Genèse d'Acid Tracks** (Wikipedia, Roland) : 303 + **707 et 727** ; la ligne est née en tournant les potards **pendant que le pattern joue** ; le son acide = cutoff/résonance/env mod bougés en continu. Voodoo Ray (RBMA) : 808, **basses SH-101 superposées avec des réverbes différentes** (« métallique, steel-drum »), 303 ajoutée en studio, **voix de Nicola Collier jouée à l'endroit et à l'envers au S-950 = le motif de stabs**, mix « joué en direct » avec sections mutées/démutées, « hard bottom, soft top ».
- **Écriture 303** (MusicRadar, MusicTech, Roland « Beyond Acid ») : **accent = volume + profondeur d'enveloppe de filtre**, **slide = portamento vers la note suivante**, **slide + saut d'octave = le « bend » classique**, « disperser accents, slides et octaves au hasard puis effacer », **pattern de longueur impaire (13 ou 15 pas) qui tourne contre la mesure** (« woozy »), **décaler le pattern d'un pas** change tout, **résonance haute et cutoff balayé lentement**, automation enregistrée en direct, une octave vers le haut seulement (comme le hardware), 909 en boucle, distorsion diode, réverbe courte, EQ (couper les bas-médiums).
- **Batterie** : 707/727 (Acid Tracks), 808 (Voodoo Ray), 909 (Hardfloor). Clap 2/4, cowbell/congas 727, charley 707 continu.
- **Forme** : pas de couplet/refrain : **entrées/sorties de couches et balayages sur 8–16 mesures**, breakdown où la 303 reste seule avec son filtre, puis le kick revient. Dans Higher State, la 303 est filtrée sur un long break (Mixmag ; détails nv).

### Notre recette et les écarts

Recette : 707 kick/clap/hat `xoxoxoxoxoxoxoxo`/cowbell, open hat 909 au Drop ; swing 0,1 ; 303 : `RROR.R7RR.5OR.3R`, accents `x.x...x....x..x.`, slides `..x.......xx....`, `moves` cutoff (0,25→0,35 Intro, 0,38→0,5 Groove, 0,3→0,6 Drop) et résonance (0,65→0,85), preset `crunch` + delay 0,2 + PUMP ; `parts = {}` (ni basse ni nappe) ; `chordRhythm "X---------------"` cordes en option.

1. **Une seule mesure de 303 pendant 88 mesures** (CORPS l'a déjà noté). Les hits vivent de la **variation** : accents/slides déplacés, pattern décalé d'un pas, longueur impaire, octaves. **Changement** : (a) pattern de 32 pas dans la recette (la syntaxe existe) : `RROR.R7RR.5OR.3R|RROR.R7RO.5OR.3R` (saut d'octave en 2e mesure) ; (b) dans `inScenes`, la scène du Drop 2 reçoit **le même pattern décalé d'un pas** (`AcidPattern` tourné de 1) et accents inversés ; (c) un mode « 15 pas » (`AcidPattern.length = 15`) au Break pour le « woozy » (le moteur sait-il boucler sur 15 ? vérifier `AcidPattern.sanitized`). Mesure : ≥ 3 variantes de ligne par morceau.
2. **Les potards ne bougent qu'entre valeurs de section** (linéaire de début à fin) : les hits sont tournés **à la main, sans cesse**, avec des retours. **Changement** : `acidLanes` → ajouter une modulation lente **en triangle** (période 8 mesures) sur le cutoff ±0,08 autour de la courbe de section, et un « coup » d'env mod +0,2 sur les mesures 15–16 avant chaque Drop ; résonance 0,85 en Drop ✓.
3. **Pas de basse sous la 303, pas de sub avec le kick 707** (CORPS : sub −12 dB en packs) : Voodoo Ray superpose des basses SH-101. **Changement** : `parts = {Bass}` avec « Basse sub » `R.......R.......` octave 1 à −6 dB sous la 303 (déjà proposé dans CORPS R8), PUMP ✓.
4. **Stabs vocaux inversés / sample en « mantra »** : rien chez nous. **Changement** : `Lead` = vocal chops avec un jeton « inversé » (le `SampledPatch` a-t-il un reverse ? sinon pack `vocal_chops` avec un fichier inversé) sur `........R.......` toutes les 2 mesures, octave 4, réverbe 0,3.
5. **Swing 0,1 et charley `xoxo` fixe** : le 707 est droit, c'est juste ; mais **cowbell/congas 727** à vélocités variées manquent (ligne `perc` cowbell `..x...x...x...x.` fixe). **Changement** : `perc2` conga 727 `o..x..o.x..o..x.` à vélocités mixtes au Drop.
6. **Forme** : le Break garde la 303 ✓ (`acidBreaks`), mais la 303 sort des 16 premières mesures (Intro = kick + hat). Dans Acid Tracks, la 303 est là dès le début et c'est la batterie qui s'ajoute. **Changement** : `signature` sur la ligne acide (nouveau : `acidInIntro true`) → forme `Rave` (déjà existante pour l'acidcore : 303 seule en intro) convient : `form = Rave` pour `acid_house`.

---

## 7. Afro house (`afro_house` · 122 BPM)

### 10 morceaux

| # | Artiste – Titre (année) | BPM | Tonalité | Source |
|---|---|---|---|---|
| 1 | Black Coffee ft. Bucie – Superman (2009) | ≈ 120 (nv) | – | Wikipedia (année) |
| 2 | Culoe De Song – Webaba (2009) | ≈ 122 (songbpm, à confirmer) | ≈ Mi | songbpm |
| 3 | Mafikizolo ft. Uhuru – Khona (2013) | 127 | – | songbpm |
| 4 | Black Motion – Rainbow (2016) | ≈ 124 (nv) | – | – |
| 5 | Black Coffee ft. David Guetta & Delilah Montagu – Drive (2018) | ≈ 118 (songbpm, à confirmer) | Si mineur | songbpm |
| 6 | Master KG ft. Nomcebo Zikode – Jerusalema (2019) | 124 | ≈ Ré♭ majeur (partition) | songbpm, Chordu, Musicnotes |
| 7 | Rampa – Everything (2019) | ≈ 122 (nv) | – | – |
| 8 | Zakes Bantwini & Kasango – Osama (2021) | ≈ 120 (nv) | – | Wikipedia (« penche vers la deep house ») |
| 9 | Keinemusik (&ME, Rampa, Adam Port) – Muyè (2021) | ≈ 122 (nv) | – | – |
| 10 | Adam Port & Stryv ft. Malachiii – Move (2024) | 120 | Sol♯ mineur | Mixgraph, songbpm, openmusic.academy |

Plage : **118–127 BPM** (NI : 110–125, « 118–122 souvent préféré », tutoriel à 122 ✓ notre 122 ; Beatportal 115–125). Tonalités : mineur (Sol m dans NI, Sol♯ m pour Move, Si m pour Drive) ou majeur gospel (Jerusalema, Ré♭).

### Ce que font ces disques

- **Batterie / percus** (Producer School « like Black Coffee », NI, Beatportal, Roland amapiano pour la clave) : kick **doux, propre, sans clic** ; **boucle de toms 808 qui pose le groove, passe-haut 80 Hz, sidechainée au kick** ; shaker bruité **avec vélocités MIDI variées** + bruit blanc Vocodex dont la relâche est automatisée ; **wood aigu + réverbe, toms sur 2 et 4, shaker one-shot en fin de mesure, boucle de shaker réel** ; plus tard tambourin, boucle de charley acoustique, clap bas, **toms supplémentaires toutes les 4 mesures** ; congas/bongos « hors grille », open hats sur les contretemps. Rolling deep house (Attack) pour le tom 909 : divisions 3, 9, 11.
- **Basse** : **une seule note tenue** (sub, saw filtré) puis la basse **suit les accords (Mi–La–Sol) une fois le refrain commencé** ; passe-haut sous le sub, Kickstart. NI : sub « Bump » sur Sol2 **sauf les 2 dernières notes Do3–Ré3** (4 mesures). Beatportal : sinus/carré bas-passé, boucle 1–2 mesures sur les fondamentales.
- **Accords** : NI : **un seul accord de Sol mineur étendu « joué live » (timing et vélocité humains)** sur Rhodes « Punchy Suitcase », nappe Gm9 à vélocités variées, passe-haut 142 Hz, arpège sur Sol3 tenu 4 mesures ; Producer School : nappe Mim bas volume **dont le filtre s'ouvre lentement**, piano « Closed Grand » sur Mi–La–Sol avec OTT, grande réverbe, sidechain subtil. Beatportal : nappes tenues 7/9, évolution lente par filtre.
- **Lead** : NI : ligne **de 3 notes (Sol3, La2, Sol2)** ; Beatportal : marimba / cordes pincées / cloches, **« peu de notes, simple, répétitif, émotionnel »** ; Producer School : lead « top » avec **automation du cutoff sur tout l'arrangement**, séquence aiguë avec **LFO de panoramique lent**, 2e piano en mélodie aiguë tard dans le morceau.
- **Forme** (Producer School, blocs de **16**) : 1–16 synthé filtré + kick + tom + hat bruit ; 17–32 + sub + lead filtré ; 33–48 + nappe + shaker, **filtres qui s'ouvrent** ; 49–64 **refrain : voix, la basse suit les accords** ; ensuite piano + mélodie aiguë + percus. Beatportal (en minutes) : intro 1:00, break 1:00–2:00 (kick/basse sortent, nappes/lead entrent), build, drop à 3:00, bridge, 2e drop 5:00, outro : **morceaux de 6–7 min, montée très progressive**. Textures : **field recordings**, vinyle, drone sur la fondamentale sous tout le morceau.

### Notre recette et les écarts

Recette : kick 4/4, congas `o..x..x.o..x..x.o..x..x.o.x.x.x.`, shaker `oxoxoxoxoxoxoxox`, clap doux, open hat discret, bois 3-3-2 `o..o..o.o..o..o.`, tom aigu 2 mesures ; swing 0,2 ; `root 4 (Mi)`, Dark mineur `[0,6,5,3]` ; nappe chaude `Ninth` tenue, octave 3, `wide_alive` + réverbe ; basse sub `..R-.R....R-.R-...R-.R....R-.5-.` + pump ; **lead Marimba `..O..5..3..5..R.` (signature)** octave 5, réverbe + délai ; `drop = [pad, lead]`, `breakWith` = tout sauf kick.

1. **Le marimba (signature) joue 5 notes par mesure, la même cellule, dans toutes les sections** : les hits ont **3 notes sur 4 mesures**, « peu de notes, répétitif », qui **n'arrivent qu'à 1:00–2:00**, filtrées puis ouvertes. **Changement** : `leadPattern` 64 pas `..O-------......|................|..5---..3-------|................` (deux notes par 2 mesures), `signature` retiré du Lead (laisser au Pad), `Sweep("lead", {1: 0.3→0.7, 2: 0.7→1})` ; vélocité `o`/`O` alternée. Mesure : ≤ 8 notes de lead par 4 mesures.
2. **4 accords en 4 mesures** : les hits tiennent **un accord étendu** (Gm9) pendant 16 mesures, puis 2–3 accords au refrain. **Changement** : progression Dark `[0,0,0,0]` en Groove et `[0,6,5,3]`... non : plus juste, `[0,0,0,3]` (i tenu 3 mesures, iv en 4e) ; nappe à **vélocité par note variée** (`withChordRhythm` : ±10 % par note, graine fixe) ; la basse **tient une note** en Groove (`R---------------` 16 pas) et ne suit les accords qu'au Drop (2e `bassPattern` « drop » dans la recette).
3. **Toms 909 roulants absents** (le tom aigu n'est qu'en fin de mesure) : le groove afro de Black Coffee est **une boucle de toms** sidechainée + toms supplémentaires toutes les 4 mesures. **Changement** : `Role.Tom` voix 8 (tom medium) `..x.....x.x.....` à 0,5 + `........x.......` aigu en 4e mesure (ligne 64 pas), PUMP sur le pad tom (`StyleFx("tom", insert = "PUMP")`).
4. **Shaker `oxox` fixe** : vélocités MIDI variées + bruit à relâche automatisée. **Changement** : ±10 % vélocité + swing 0,25 sur shaker/congas ; relâche du shaker plus longue au Drop (2 échantillons : court en Groove, long en Drop, comme deux pads).
5. **Pas de montée de 4 paliers de 16 mesures** : notre Intro 16 · Groove 16 · Drop 16 est proche, mais le Drop n'ajoute que nappe + lead sans filtres qui s'ouvrent. **Changement** : `sweeps` nappe `{0: 0.25→0.5, 1: 0.5→0.8, 2: 0.8→1}` ; `drop` = `[pad, lead, open_hat, clap]` ; la **voix** (chops) uniquement au Drop 2 (= `dropTwoLane`).
6. **Textures** : field recording / drone absents. **Changement** : piste « Ambiance » (bruit filtré `RISER_SOUND` à −30 dB, LP 2 kHz, tenue) sur tout le morceau — ou un pack FX ; faible priorité.

---

## 8. Disco / funk (`disco` · 120 BPM)

### 10 morceaux

| # | Artiste – Titre (année) | BPM | Tonalité | Source |
|---|---|---|---|---|
| 1 | Donna Summer – I Feel Love (1977) | ≈ 126–128 | Do majeur (changements de ton réguliers) | Wikipedia, songbpm 128, Mixmeister 126–127 |
| 2 | Cerrone – Supernature (1977) | 123 | Fa mineur | songbpm |
| 3 | Bee Gees – Stayin' Alive (1977) | ≈ 103–108 | Fa mineur (AcousticBrainz) | Wikipedia (103), SFR 103, AcousticBrainz 108 |
| 4 | Chic – Le Freak (1978) | 120 | La mineur (Mixmeister/Beatport) / Sol (Tunebat) | Tunebat, archive Waterloo |
| 5 | Earth, Wind & Fire – September (1978) | ≈ 126 (nv) | – | – |
| 6 | Sister Sledge – We Are Family (1979) | ≈ 117 (songbpm, à confirmer) | – | songbpm |
| 7 | Chic – Good Times (1979) | 110–111 (116 partition) | Mi mineur (dorien), Em7–A7 | Talking Bass, songbpm, Musicnotes |
| 8 | Michael Jackson – Don't Stop 'Til You Get Enough (1979) | ≈ 119 (nv) | – | – |
| 9 | Daft Punk – Get Lucky (2013) | 116 | Fa♯ mineur / Si dorien, Bm7–D–F♯m7–E | Wikipedia, songbpm |
| 10 | Dua Lipa – Don't Start Now (2019) | ≈ 124 (nv) | – | – |

Plage : **103–128 BPM**, médiane ≈ 117 ; notre 120 est dans la fourchette, mais le funk-disco de Chic est à **110–116** (Get Lucky 116). Tonalités : mineur/dorien dominant avec **2 accords** (Em7–A7, Bm7–D–F♯m7–E), 7e partout.

### Ce que font ces disques

- **Basse** (Talking Bass, Good Times) : riff de **4 mesures** : coups sur le temps + croches droites sur la montée, **anticipations en double-croche vers les mesures 2, 3 et 4**, sur Em7 montée par les notes de l'accord, sur A7 **6te majeure → 7e mineure** (« procédé funk »), **la basse porte le hook**. I Feel Love (Wikipedia, SOS) : **8 notes au séquenceur Moog**, **chaque note doublée par un délai, sec à gauche et répétition à droite** (« strobe »), kick joué par un batteur, charley = bruit blanc Moog, changements de ton réguliers, 8:15 en 12".
- **Guitare** (Rock Hall, Rolling Stone) : « chucking » = **doubles-croches continues, certaines étouffées, les accentuées sonnent**, **accords étendus (11e, 13e) en renversements jazz**.
- **Batterie** (MusicRadar/zZounds/Soundbrenner) : kick 4/4, caisse 2 et 4, **charley fermé en doubles ou croches qui s'ouvre sur le contretemps** (le « tsss » entre les kicks), tambourin, congas ; Stayin' Alive = **une boucle de 2 mesures** recopiée (Wikipedia : « Bernard Lupe »). Attack « cosmic nu-disco » (120–125, droit) : CR-78, clap **avancé de quelques ms**, laser Simmons sur le 1, charley ouvert sur les temps / fermé à contretemps (inversé), bus SSL 2:1.
- **Arrangement** : cordes (longues lignes, unisson d'octaves, montées vers le refrain : nv en détail), cuivres (Stayin' Alive), pré-refrain/refrain chantés, Get Lucky : **le pont superpose les deux hooks**, vocoder en outro ; Wikipedia : la guitare de Rodgers « n'a eu besoin d'aucun traitement », tout a été positionné autour d'elle.
- **Hook** : vocal chanté (« Good times », « Get lucky », « Stayin' alive ») + riff de basse/guitare.

### Notre recette et les écarts

Recette : kick 4/4, caisse `....X......oX..o`, open hat `..X...X...X...X.`, hat `x..ox..ox..ox..o`, congas, tambourin au Drop ; swing 0,05 ✓ ; `root 9 (La)`, Happy **dorien `[0,3,0,3]`** ✓ (i–IV, Get Lucky-like), Dark `[0,3,6,2]` ; pluck `Seventh` `..X.X..X..X.X..X` octave 3 ✓ ; **basse « Basse pincée » `R.O.R.O.R.O.R.O.R.O.R.O.R.O.RrOo`** octave 1 ; lead Cordes `O.......5-3-O---` octave 4.

1. **La basse = octaves en croches mécaniques** (R.O.R.O…) ; Good Times/Get Lucky = **ligne de 4 mesures avec anticipations en double-croche, 6te→7e, montée par l'accord**. **Changement** : `bassPattern` 64 pas sur 2 accords, ex. (Em7 / A7) `R...R.O.R.3.5.7O|...R.O.R.6.7.O.R|R...R.O.R.3.5.7O|..6.7.O.R.7.6.5.` — le jeton `6`/`7` existe ; vélocité `r`/`o` sur les anticipations. Octave 1 → **octave 2** (la basse pincée Plaits est faible et sans harmoniques en octave 1, CORPS T1). Mesure : ≥ 2 anticipations (notes sur un pas impair 15/31) par 2 mesures.
2. **Le « strobe » I Feel Love** : délai **sec à gauche / répété à droite** = largeur instantanée. **Changement** : préréglage d'effet « Strobe » (ECHO 1/8 ou 1/16 mix 100 % panné droit, signal sec gauche) sur le Pad pluck ou l'arpège ; un `FxPreset("strobe")` (STYLE-FIXES F10-like).
3. **Cordes en 4 notes par mesure** (`O.......5-3-O---`) : trop courtes et répétées 16 fois ; les cordes disco = **lignes longues sur 4–8 mesures** montant vers le refrain, en octaves. **Changement** : `leadPattern` 64 pas `O---------------|5-------3-------|O-------7-------|5---------------` puis Drop 2 une octave au-dessus (`dropTwoLane` + transposition +12 de la scène) ; `Voicing.Octave` sur le lead (nouvelle option `leadVoicing`) pour l'unisson d'octave ; vibrato ✓ (`Cordes` a VIBRATO 0,05).
4. **Guitare « chucking » absente** : le pluck joue `..X.X..X..X.X..X` à 0,75 fixe. Le chucking = doubles-croches continues à **3 niveaux** (étouffé 0,25 / normal 0,6 / accent 1). **Changement** : `chordRhythm` 16 pas en doubles-croches continues `oXoxoxXoxoXoxoXx` sur un son court (« Pluck » avec A_DECAY 0,2) — le jeton `o` n'existe pas pour les accords (`X`/`x` seulement) : l'ajouter à `withChordRhythm` (0,35).
5. **Tempo** : 120 est au bord haut ; Chic/Daft Punk 110–116. **Changement** : 116 BPM (Happy) ; garder 120 pour Dark (Supernature 123).
6. **Crash/caisse jouée, fills de batteur** (toms, caisse en double-croche sur la 8e mesure) : commun.

---

## 9. Amapiano (`amapiano` · 112 BPM)

### 10 morceaux

| # | Artiste – Titre (année) | BPM | Tonalité | Source |
|---|---|---|---|---|
| 1 | Sha Sha ft. DJ Maphorisa & Kabza De Small – Tender Love (2019) | ≈ 112 (nv) | – | – |
| 2 | Focalistic & Vigro Deep – Ke Star (2020) | 112 | Si♭ mineur | Beatport |
| 3 | Mr JazziQ & Busta 929 – VSOP (2020) | ≈ 113 (nv) | – | – |
| 4 | Kabza De Small & DJ Maphorisa ft. Ami Faku – Abalele (2021) | ≈ 112 (nv) | – | – |
| 5 | Young Stunna ft. Kabza De Small – Adiwele (2021) | ≈ 113 (nv) | – | – |
| 6 | Uncle Waffles – Tanzania (2022) | ≈ 112 (nv) | – | – |
| 7 | Musa Keys – Selema (Po Po) (2022) | ≈ 112 (nv) | – | – |
| 8 | Tyler ICU & Tumelo_za – Mnike (2023) | 113 | Fa mineur | Beatport (radio edit) |
| 9 | Tyla – Water (2023, « popiano ») | 117 | Ré♯ mineur, voix Sol♯3–Do♯5 | Wikipedia |
| 10 | Kelvin Momo – Amukelani (2023, private school) | ≈ 110 (nv) | – | – |

Plage : **110–117 BPM** (Roland : 108–115, exemples à 112 ✓ ; Beatstorapon : private school vers 108–110, bacardi/s'gija plus vite). Tonalités : **mineur** (Si♭ m, Fa m, Ré♯ m) ; Roland propose **Cm9–F7–B♭maj9–Gm7–G7♭9** (II–V–I jazz) et une nappe E♭m/A♭m à notes communes ; Beatstorapon **Em9–Cmaj7–Gmaj7–D6/9**.

### Ce que font ces disques

- **Batterie** (Roland, Beatstorapon) : kick 909 **doux et rond**, 4/4 au début puis **certains kicks retirés pour que percus + log drum impliquent la pulsation** ; **shaker en doubles-croches à vélocités alternées, swing subtil, coups manquants à l'occasion, petits changements de timing toutes les 2 ou 4 mesures** ; congas en clave (temps 2, « a » de 2, « et » de 3, temps 4) ; open hat sur les contretemps ; rims/bois **en question-réponse** (« un son pose une phrase, l'autre répond ») ; clap sec et discret ; **ne pas compresser les percus** tant que la vélocité n'est pas uniforme (elle ne doit pas l'être).
- **Log drum** : FM (DX10) / sinus + enveloppe de pitch rapide + distorsion ; **phrases de 2 mesures : la 1re pose, la 2e répond, la 4e ou 8e varie plus** ; **laisser des trous pour que la note suivante pèse plus** ; fondamentales d'abord, puis quintes, octaves, notes de passage, **notes aiguës = plus légères, graves = plus de poids** ; **pas de queue longue façon 808** ; il **n'entre pour de bon qu'à la section principale** (après 16–48 mesures), les autres éléments mélodiques s'effacent alors.
- **Piano / accords** : piano électrique/Rhodes, **7, 9, 11 et renversements larges** ; **stabs courts, accords qui entrent après le temps, alternance tenu/court, et les claviers se taisent pendant les sections de log drum** ; Roland : boucle de 4 mesures de Rhodes/vibes.
- **Voix** : phrases **courtes**, espace dans le médium, **trous pour les réponses du log drum**, moins de percus pendant les paroles ; sifflets, oiseaux (Roland).
- **Forme** (Beatstorapon, en mesures) : Intro 8–16 (percus, claviers filtrés, sans kick) · Fondation 16 (kick + percus + accords, log drum absent) · Développement 16–32 (voix, 2e percu, 1re réponse de basse) · **Section log drum 16–32** (le motif le plus fort, mélodie retirée) · Break 8–16 · Final 16–32 (nouvelle fin de log, contre-ligne) · Outro 8–16 : **morceaux de 6–8 min**, le log drum tombe vers 1:30–2:30.
- **Mix** : kick et log drum qui ne se masquent pas (déclins courts, sidechain léger, EQ dynamique), graves centrés, réverbe sélective, **pas de sur-limitation** pour garder le contraste percus douces / log lourd.

### Notre recette et les écarts

Recette : kick doux `x...x...x...x...`, shaker `xoxoxoxoxoxoxoxo`, clap 2/4, bois 2 mesures `......x..x...x.......x..x.x.x...` ✓ (question-réponse), conga au Drop ; swing 0,15 ; `root 0 (Do)`, Happy dorien `[0,3,6,4]`, Dark `[0,5,2,6]` ; piano électrique `Ninth` `...X..X...X.....` octave 4, `wide_alive` ; **log drum `..R-.R-O-.R-.5-...R-.R-..O-..R5-` = signature (joue dans toutes les sections)** ; lead Piano FM `..O..5...3..R...` octave 5 ; pas de pompe, pas de riser.

1. **Le log drum est « signature » donc présent dès l'Intro** : c'est exactement l'inverse du genre, où il **tombe** après 32–48 mesures et où tout le reste s'efface. **Changement** : `signature = Pad` (le piano), `drop = [bass]` (le log drum arrive au Drop), `breakWith = [pad, perc, hat]` ; au Drop, le piano passe en `o` (−6 dB) ou se tait sur les mesures 9–16 (« les claviers se taisent pendant le log drum »). Mesure : log drum absent sur ≥ 32 mesures avant son entrée ; Drop − Groove ≥ +3 LU.
2. **Pattern de log drum sans trous ni variation à 4/8 mesures** : 32 pas ✓ mais 9 notes par 2 mesures et la même chose 8 fois. **Changement** : 64 pas, mesures 1–2 = question/réponse (actuel), 3 = question, 4 = **réponse variée + silence d'un temps** : `..R-.R-O-.R-.5-.|..R-.R-..O-..R5-|..R-.R-O-.R-.5-.|....5-.3-..R----` ; vélocités : notes aiguës (`O`, `5`) en minuscule (plus légères), graves en majuscule ; **déclin court** (Plaits `Log drum` decay 0,6 → 0,45).
3. **Shaker `xoxo` strict** : le shaker amapiano a des coups manquants et des micro-changements toutes les 2–4 mesures. **Changement** : ligne shaker 64 pas avec 1 trou par 2 mesures et une double `3` à la fin de la 4e : `xoxoxoxoxoxoxoxo|xoxoxoxox.xoxoxo|xoxoxoxoxoxoxoxo|xoxoxoxoxoxox3xo` ; ±10 % vélocité ; swing 0,2.
4. **Piano : 3 coups par mesure, même rythme, position fondamentale à l'octave 4** ; les hits : 7/9/11 renversés larges, entrée **après le temps** ✓ (nos `...X` sont après le temps ✓), **alternance tenu/court**. **Changement** : `chordRhythm` 32 pas `...X--..X...X...|...X--------....` ; `Voicing` renversé sans fondamentale, octave 3 ; **une boucle de 2 accords** (ou le II–V–I de Roland : `[1, 4, 0, 0]` en Dorian/majeur relatif) : le `[0,3,6,4]` dorien actuel est acceptable, mais le passage i–IV–VII–V change à chaque mesure, trop pour le genre.
5. **Lead Piano FM `..O..5...3..R...` chaque mesure** = 4 notes par mesure pendant tout le Drop, en même temps que le log drum : les hits laissent des trous pour que le log drum réponde. **Changement** : `leadPattern` 64 pas `..O..5..........|................|.........3..R...|................` (2 mesures sur 4), et **muet dans la section log drum** (mesures 9–16 du Drop) : nouvelle règle de `inScenes` « lead en appel, basse en réponse » = le lead ne joue que dans les mesures paires du bloc de 8 quand `signature = Bass`… ici plutôt un champ `leadOff` = `[2]` (scènes où le lead se tait).
6. **Pas de pompe, pas de contraste de niveau** : la pompe existe par `withGlue` (famille House ✓ : le PUMP est bien posé sur la basse, CORPS l'avait noté absent avant la phase 29). OK. Mais **sur-limitation** : le banc donne LRA 1,4 LU et PLR 7,1 pour amapiano — le genre veut du contraste. **Changement** : cible de sonie amapiano −9 LUFS (au lieu de −8) et LRA ≥ 4 LU au banc (dérivé de la forme ci-dessus, pas du limiteur).
7. **Forme trop courte** : 88 mesures = 3:08 à 112 ; le genre vit sur 6–8 min avec 7 paliers. Impossible sous 3:30 ; **Changement minimal** : `sectionsOf` amapiano = Intro 8 · Groove 16 · Drop (sans log) 16 · Drop (log) 16 · Break 8 · Drop 16 · Outro 8 = 88 mesures mais **4 paliers** au lieu de 3 : fait avec les demi-Drops (synthèse, point 3).

---

## Synthèse famille : les 10 plus gros écarts (impact audible décroissant)

Lecture : « hits » = constats sourcés ci-dessus, communs à au moins 6 styles sur 9 ; « nous » = `StarterWizard` / `WizardMix` / recettes ; « vérifier » = critère mesurable au banc (`scripts/song-bench.sh`) ou en test unitaire sur le projet généré.

1. **La mélodie n'a pas de phrase : une cellule d'une mesure répétée 16 fois, ou un arpège de triade en doubles-croches.** Hits : phrases de 2–4 mesures **question/réponse**, 3 à 8 notes par 4 mesures en afro/amapiano/deep, riffs de 2 mesures avec sauts d'octave en house/tech house, fin différente à la 4e et 8e mesure, silences d'une mesure entière. **Nous** : `leadPattern` 16 pas rejoué chaque mesure, `arpRate 24` UpDown, vélocité 0,8/0,65 fixe. **Changement** : (a) `leadPattern` de **64 pas** accepté par le convertisseur et `withLine` (déjà possible techniquement : `barOf` prend des lignes de n×16) et réécrit pour les 9 styles comme ci-dessus ; (b) un champ `answer` (motif de réponse joué les mesures 2 et 4, ou par le `counter`) ; (c) dans `inScenes`, variante **toutes les 8 mesures** : mesures 7–8 muettes ou transposées (+12 en Drop 2) ; (d) arpège `UpDown` remplacé par une **séquence fixe** (`ArpMode.Pattern` ou un `leadPattern` écrit). **Vérifier** : test `StarterWizardTest` : pour chaque style, le lead a ≥ 2 mesures distinctes par 4 mesures, ≥ 25 % de pas vides, et aucune suite de 16 notes à vélocité identique ; banc : densité de notes de lead ≤ 12 par 4 mesures hors house/tech house.
2. **Les accords sont empilés en position fondamentale, un par mesure, 4 différents, au même rythme et à la même vélocité.** Hits : **2 accords** (ou 8 mesures avec surprise en 8e), **7e/9e en 3e renversement sans fondamentale ni quinte** (Kerri Chandler, Attack), rythme syncopé qui évite le 1er temps, **tenu/court alterné**, 4e mesure variée, vélocités « jouées ». **Nous** : `withChordRhythm`, `Voicing.Seventh/Ninth`, `chordRhythm` 16 pas, vélocité 0,75. **Changement** : `Voicing.Rootless7` `[2,4,6]`, `Rootless9` `[2,4,6,8]`, `Inv3` ; `degrees` de longueur 2 ou 8 ; `chordRhythm` 32/64 pas avec `-` (tenues) et `o` (doux) ; ±10 % de vélocité par note (graine fixe) ; `chordOctave 3` quand le voicing est sans fondamentale. **Vérifier** : test : aucune fondamentale doublée entre basse et accords ; note la plus grave de l'accord ≥ 12 demi-tons au-dessus de la basse ; banc : bas-médium 250–500 Hz ≥ −12 dB rel. (aujourd'hui −12 à −18).
3. **Le Drop est le Groove + une couche, et les 16 mesures sont la même boucle 4 fois.** Hits : drop en **A/B/C/D de 8** (EDMProd), kick + basse **coupés 2 mesures avant C**, shakers/ride en C–D, **crash au 1er temps**, downlifter au milieu (speed garage), **une couche entre ou sort tous les 8** ; amapiano/afro : 4 à 7 paliers de 16. **Nous** : `sectionsOf` + `dropTwoLane` (lead en 2e moitié du 1er Drop seulement). **Changement** : chaque Drop = 2 scènes de 8 (A/B puis C/D) : A/B sans `drop` layers, C/D avec, **kick + basse muets sur les 2 dernières mesures de B**, crash sur le 1er pas de A et de C ; Groove = 2 scènes aussi (palier 1 : + une couche). **Vérifier** : banc : Drop − Groove ≥ +2 LU, 2e moitié − 1re moitié de Drop ≥ +1,5 LU, Drop − Break ≥ +4 LU ; ≥ 6 mesures de batterie distinctes par morceau.
4. **Batterie sans vie : 3 vélocités, aucune avance/retard, un roulement tous les 4.** Hits : **clap 3–5 ms en avance**, open hat/charleys « légèrement en retard » (UKG), **vélocités variées sur chaque charley/shaker** (« la clé du swing »), kicks/caisses **fantômes** à faible vélocité, **fills toms/caisse à 8 et 16** (deep house : fill répété 8 mesures plus loin), **crashs sur les tournes de 16/32**, swing 55–80 % selon le style. **Nous** : `hits()` X/x/o, `swing` global, `WizardMix.phrase`. **Changement** : dans `build`, graine fixe : vélocité ±8 % (hats/percus) et ±4 % (clap), micro-timing par rôle (clap −4 ms, percus +3, open hat +6, 2e kick UKG −6) → nécessite un `Hit.offset` en ticks (STYLE-FIXES F12, moteur) ; fills à 8/16 plus gros que celui de 4 (toms + caisse 1/32) ; crash = voix cymbale raccourcie (`kitShaped` comme la ride). **Vérifier** : écart-type des vélocités de charley ≥ 0,05 ; test : chaque Drop a ≥ 1 crash et ≥ 2 fills distincts ; banc : air > 6 kHz entre −20 et −16 dB.
5. **La basse joue une mesure sans forme, toujours sur la fondamentale, et ne « tombe » pas.** Hits : house/tech house/garage : hook de 2–4 mesures avec **7e, quinte, saut d'octave et glissé en fin de phrase**, disco : **anticipations en double-croche**, deep : **évite la fondamentale au 1er temps**, afro : **une note tenue puis suit les accords au refrain**, amapiano : **log drum qui tombe après 32–48 mesures** avec trous, UKG/speed : **« wub »** (enveloppe de pitch) + **sub pur** + filtre qui s'ouvre. **Nous** : `bassPattern` 16/32 pas, `signature = Bass` en amapiano/tech house/speed garage, `subBass` sur 2 styles. **Changement** : `bassPattern` 64 pas par style (motifs proposés ci-dessus), 2e motif `bassDrop` pour afro/amapiano, `drop = [bass]` en amapiano, `subBass true` en UKG et acid house, préréglage « Basse dred » (F10) + enveloppe de pitch courte ; `Sweep("bass")` sur Groove/Drop en speed garage. **Vérifier** : test : ≥ 3 hauteurs distinctes par 4 mesures de basse (sauf afro Groove) ; banc : sub < 60 Hz −2 à −4 dB rel. en Drop, corrélation < 120 Hz ≥ 0,95.
6. **Synthés mono, une couche, sans mouvement.** Hits : stab = saw + octave + quinte + chorus (tech house), tambourin en deux copies désaccordées L/R (Chicago), délai « strobe » sec/gauche – répété/droite (I Feel Love), **cutoff automatisé sur tout le morceau** (afro, acid), LFO de pan lent, voix doublée −12 et distordue en parallèle. **Nous** : pistes rendues en mono (son.md § 1.2 n° 3), `wide_alive` sur quelques nappes, sweeps sur peu de pistes. **Changement** : moteur étape 4 (unisson stéréo) ; en attendant, dans `WizardMix.withPartMix` : lead → `WIDENER` 0,4 + délai 1/8 pointé 0,2 ; stabs → piste `Contre-chant` à +7 demi-tons (−9 dB) ; `FxPreset("strobe")` ; `Sweep("lead")` par défaut en afro/acid/tech house (0,4 → 1 sur Groove→Drop). **Vérifier** : banc : côté/centre −15 à −9 dB (aujourd'hui −22 à −31), corrélation 0,3–0,85, perte mono ≥ −1,5 LU.
7. **Pas de hook vocal, ou des chops qui rejouent la même syllabe à la même hauteur chaque mesure.** Hits : 7 styles sur 9 ont un mot/syllabe répété (« Show me love », « Losing it », « Cola », Gypsy Woman, Re-Rewind, Osama, « Water ») ; Todd Edwards/Producer School : chops **pitchés −3/−5 en réponse**, inversés, timing lâche, **la voix remplace l'arpège au drop** (NI UKG), **boucle vocale en 2e moitié du drop** (speed garage). **Nous** : `vocal_chops:vocal` sur un `leadPattern` de 16 pas. **Changement** : jeton de **transposition** dans `leadPattern` pour les chops (`r` = −3, `R` = 0, `O` = +12) ; appel/réponse sur 4 mesures ; chops uniquement en Drop 2 / 2e moitié (`dropTwoLane` ✓) ; fichier inversé dans le pack `vocal_chops`. **Vérifier** : test : la piste voix a ≥ 2 hauteurs et ≤ 6 notes par 4 mesures ; elle est muette en Intro et Groove.
8. **Les sections ne montent pas : filtres figés hors Break, pas de riser de 8 mesures, pas de downlifter, pas de kick inversé.** Hits : filtres qui s'ouvrent sur 16–32 mesures (afro, acid, tech house), riser à la fin de B, **beat coupé une mesure avant le drop** (UKG), spin-back/sirène/downlifter (speed garage). **Nous** : souffle blanc 4 mesures + impact, `breakSweeps` sur la nappe. **Changement** : `riser` 8 mesures (`RISER_BARS = 8`) avec passe-haut montant sur le bus musique (étape 5), silence total de la batterie sur la dernière mesure avant le Refrain/Drop en forme chanson, pack FX de transition (downlifter, kick inversé, spin-back) joué par `inScenes`. **Vérifier** : banc : les 4 dernières mesures avant le Drop montent de ≥ +3 LU ; le 1er temps du Drop a un pic ≥ +3 dB sur la moyenne.
9. **Swing et tempos : presque justes, à quelques réglages près.** Tempos : house 122 ✓ (119–125), deep 120 ✓, tech house 126 ✓, UKG 132 ✓, speed garage 130 ✓ (revival 135–140), acid 120 ✓, afro 122 ✓, amapiano 112 ✓, **disco 120 → 116** (Chic 110–116, Get Lucky 116). Swing : house NY 70–80 % et deep « dusted » 70–80 % vs nos 0,15–0,2 ; tech house 60–65 % vs 0,15. **Changement** : house 0,25, deep house 0,3, tech house 0,25, afro 0,25 ; disco `bpm 116`. **Vérifier** : test de recette (valeurs) ; à l'oreille (A/B court de Benjamin).
10. **Sonie et dynamique : le niveau est là, le contraste non.** Banc : Drops à −8/−9 LUFS ✓ et TP −1,2 ✓ (club −6 à −9 selon Mastering The Mix), mais **LRA 1–2 LU** et Drop − Break ≈ 0 : les genres doux (amapiano « pas de sur-limitation », deep house −8 à −10) veulent du relief. **Changement** : cibles par style (deep house −9, amapiano −9, afro −9, acid −8, house/tech/UKG/speed −8, disco −9) et un critère **LRA ≥ 4 LU** obtenu par les points 3 et 8 (sections) plutôt que par le limiteur ; retour de réverbe plus long en Break (send 0,4). **Vérifier** : banc : LRA ≥ 4, PLR 7–10, Drop − Break ≥ +4 LU.

Ordre de chantier conseillé : 1 + 2 (mélodie et accords : recettes + `withLine`/`withChordRhythm`/`Voicing`, sans moteur), puis 3 + 8 (demi-Drops, crash, riser 8), puis 5 + 7 (basses et voix 64 pas), puis 4 (vélocités, puis micro-timing avec `Hit.offset` côté moteur), 6 en parallèle avec l'étape 4 du plan son, 9 et 10 en réglages.

---

## Sources

Analyses et tutoriels : Attack Magazine [Classic 90s NY/NJ house](https://www.attackmagazine.com/?p=7290), [21st century Chicago house](https://www.attackmagazine.com/technique/beat-dissected/21st-century-chicago-house/), [Rolling deep house](https://www.attackmagazine.com/technique/beat-dissected/rolling-deep-house/), [Dusted deep house](https://www.attackmagazine.com/technique/beat-dissected/dusted-deep-house/), [Deep tech house](https://www.attackmagazine.com/technique/beat-dissected/deep-tech-house/), [Cosmic nu-disco](https://www.attackmagazine.com/technique/beat-dissected/cosmic-nu-disco/), [Deep house chords](https://www.attackmagazine.com/technique/passing-notes/passing-notes-deep-house-chords/), [Kerri Chandler chords](https://www.attackmagazine.com/technique/passing-notes/kerri-chandler-chords/), [Passing Notes : stabs](https://www.attackmagazine.com/?p=111276) ; [EDMProd tech house](https://www.edmprod.com/how-to-make-tech-house/), [EDMProd classic deep house](https://www.edmprod.com/how-to-make-classic-deep-house/) ; Native Instruments [UK garage](https://www.native-instruments.com/blogs/blog/uk-garage-music), [Afro house 101](https://www.native-instruments.com/blogs/blog/afro-house-production-101) ; Beatportal [afro house](https://www.beatportal.com/articles/647491-step-by-step-guide-to-creating-an-afro-house-track-keinemusik-black-coffee-caiiro-alex-wann-style), [tech house](https://www.beatportal.com/articles/692607-step-by-step-guide-to-producing-tech-house-like-fisher-chris-lake-cloonee-and-michael-bibi), [Mau P interview](https://www.beatportal.com/news/mau-p-delivers-a-beatport-number-1-with-drugs-from-amsterdam) ; Producer School [speed garage](https://theproducerschool.com/blogs/featured-blogs/how-to-make-speed-garage-complete-production-tutorial), [afro house like Black Coffee](https://theproducerschool.com/blogs/featured-blogs/how-to-make-afro-house-like-black-coffee-complete-tutorial) ; [Undrgrnd Sounds speed garage](https://undrgrndsounds.com/blogs/news/how-to-produce-speed-garage-the-complete-production-guide) ; [Sample Focus UKG](https://samplefocus.com/blog/how-to-produce-uk-garage-music) ; [Roland amapiano](https://articles.roland.com/?p=67237), [Beatstorapon amapiano](https://beatstorapon.com/blog/how-to-make-amapiano-beat/) ; [MusicTech acid bassline](https://musictech.com/guides/essential-guide/how-to-create-a-chicago-style-acid-house-bassline), [MusicRadar TB-303 guide](https://www.musicradar.com/news/producers-guide-to-the-roland-tb-303-and-clones), [Roland Beyond Acid](https://articles.roland.com/beyond-acid-pushing-the-tb-303-into-new-sonic-territory/) ; [Talking Bass Good Times](https://www.talkingbass.net/good-times-breakdown-of-a-bernard-edwards-classic/) ; [Rock Hall Nile Rodgers](https://rockhall.com/wp-content/uploads/2024/03/Nile_Rodgers_2017.pdf), [Rolling Stone AMA](https://www.rollingstone.com/music/music-news/nile-rodgers-digs-deep-in-reddit-ama-175635/) ; [Sound on Sound I Feel Love](https://www.soundonsound.com/techniques/classic-tracks-donna-summer-feel-love) ; RBMA Voodoo Ray [2013](https://daily.redbullmusicacademy.com/2013/01/key-tracks-guy-called-gerald-on-voodoo-ray), [2017](https://daily.redbullmusicacademy.com/2017/02/guy-called-gerald-voodoo-ray-feature) ; [DJ Mag Kelly G](https://djmag.com/features/how-kelly-gs-remix-tina-moores-never-gonna-let-you-go-laid-blueprint-2-step-garage) ; [Mastering The Mix club/streaming](https://www.masteringthemix.com/blogs/learn/song-mastering-for-cd-club-play-and-streaming) ; [Mixed In Key arrangement](https://mixedinkey.com/captain-plugins/wiki/how-to-arrange-a-dance-music-track/), [KVR structure](https://www.kvraudio.com/forum/viewtopic.php?p=6355221).
Wikipedia : [Acid Tracks](https://en.wikipedia.org/wiki/Acid_Tracks), [Voodoo Ray](https://en.wikipedia.org/wiki/Voodoo_Ray), [Speed garage](https://en.wikipedia.org/wiki/Speed_garage), [UK garage](https://en.wikipedia.org/wiki/UK_garage), [Amapiano](https://en.wikipedia.org/wiki/Amapiano), [Afro house](https://en.wikipedia.org/wiki/Afro_house), [Get Lucky](https://en.wikipedia.org/wiki/Get_Lucky_(Daft_Punk_song)), [I Feel Love](https://en.wikipedia.org/wiki/I_Feel_Love), [Stayin' Alive](https://en.wikipedia.org/wiki/Stayin%27_Alive), [Water (Tyla)](https://en.wikipedia.org/wiki/Water_(Tyla_song)), [Jerusalema](https://en.wikipedia.org/wiki/Jerusalema), [Gypsy Woman](https://en.wikipedia.org/wiki/Gypsy_Woman_(She%27s_Homeless)), [Losing It](https://en.wikipedia.org/wiki/Losing_It_(song)), [Move (Adam Port)](https://en.wikipedia.org/wiki/Move_(Adam_Port_and_Stryv_song)), [Can You Feel It](https://en.wikipedia.org/wiki/Can_You_Feel_It_(Larry_Heard_song)), [Todd Edwards](https://en.wikipedia.org/wiki/Todd_Edwards).
BPM / tonalités : [Hooktheory Move Your Body](https://www.hooktheory.com/theorytab/view/marshall-jefferson/move-your-body-(the-house-music-anthem)), [Hooktheory Sweet Like Chocolate](https://www.hooktheory.com/theorytab/view/shanks--and-amp-bigfoot/sweet-like-chocolate) ; songbpm (données Spotify) : [Acid Trax](https://songbpm.com/@phuture/acid-trax), [Losing It](https://songbpm.com/@fisher/losing-it), [Get Lucky](https://songbpm.com/@daft-punk/get-lucky), [Voodoo Ray](https://songbpm.com/@a-guy-called-gerald/voodoo-ray), [Your Love](https://songbpm.com/@frankie-knuckles/your-love), [French Kiss](https://songbpm.com/@lil-louis/french-kiss), [Supernature](https://songbpm.com/@cerrone/supernature), [Gunman](https://songbpm.com/@187-lockdown/gunman), [Cola](https://songbpm.com/@camelphat/cola), [Take It](https://songbpm.com/@dom-dolla/take-it), [Acperience 1](https://songbpm.com/@hardfloor/acperience-1), [Good Times](https://songbpm.com/@chic/good-times), [Khona](https://songbpm.com/@mafikizolo/khona), [Jerusalema](https://songbpm.com/@master-kg/jerusalema-feat-nomcebo-zikode), [Show Me Love](https://songbpm.com/@robin-s/show-me-love), [Move Your Body](https://songbpm.com/@marshall-jefferson/move-your-body), [Promised Land](https://songbpm.com/@joe-smooth/promised-land), [Ride on Time](https://songbpm.com/@black-box/ride-on-time), [My Love](https://songbpm.com/@route-94/my-love), [Latch](https://songbpm.com/@disclosure/latch), [Turn Off the Lights](https://songbpm.com/@chris-lake/turn-off-the-lights), [Flowers](https://songbpm.com/@sweet-female-attitude/flowers), [21 Seconds](https://songbpm.com/@so-solid-crew/21-seconds), [Battle](https://songbpm.com/@wookie/battle), [Fill Me In](https://songbpm.com/@craig-david/fill-me-in) ; Beatport : [Forget](https://www.beatport.com/track/forget/5825343), Ke Star, Mnike, Finally, Never Gonna Let You Go (Kelly G dub) ; [Tunebat Le Freak](https://tunebat.com/Info/Le-Freak-CHIC/7z3aH1QV2zwMKXxShBNOGe), [Tunebat Good Life](https://tunebat.com/Info/Good-Life-Remastered-Inner-City/6e7UgEwCg3idEOEhXEQaPJ) ; AudioKeychain : [Bar A Thym](https://www.audiokeychain.com/track/YKj/kerri-chandler-bar-a-thym), [Can You Feel It](https://www.audiokeychain.com/track/tIz/mr-fingers-can-you-feel-it) ; Mixgraph : [Move](https://www.mixgraph.io/tracks/adam-port-move), [Mau P](https://www.mixgraph.io/artists/mau-p), [La La Land](https://www.mixgraph.io/tracks/green-velvet-la-la-land) ; archives Mixmeister/Traktor UBC/Waterloo (Show Me Love, Ride on Time, Le Freak, RipGroove, Flowers, Higher State, I Feel Love).
