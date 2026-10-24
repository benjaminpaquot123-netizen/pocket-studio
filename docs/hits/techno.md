# Famille Techno : 10 tubes par style contre nos recettes et le générateur

Date : 2026-10-23. Dépôt lu en lecture seule : `/home/user/ps-p22` (`StyleRecipes.kt`, `StarterWizard.kt`, `WizardMix.kt`, `StyleMusic.kt`, `SoundLibrary.kt`), `docs/CORPS.md`, `docs/STYLE-FIXES.md`, `docs/plan/son.md`, `docs/son/banc/apres-etape2.md`, `docs/RETOURS.md`. Ce rapport **prolonge** `scratchpad/research/famille-techno.md` (2026-10-21 : batterie, basse, rumble, tempos) et ne le refait pas : il se concentre sur ce que Benjamin a entendu, « ça ressemble encore trop à de l'amateur, surtout les mélodies synthé » : accroches, mélodies, sons de synthé, structure, énergie, sonie.

Méthode : aucune écoute ; sources publiques écrites (Attack Magazine « Deconstructed » et « Beat Dissected », Sound On Sound « Classic Tracks », Hooktheory, Beatportal, Futureproof, The Producer School, Red Bull Music Academy, Roland, Wikipedia, bases BPM/tonalité mixgraph, songbpm, Beatport). Les chiffres sans source sont marqués ≈ (estimation) ou « n.d. » (non trouvé). Le quota de recherche web du tour a été épuisé à mi-chemin : quelques BPM/tonalités manquent, ils sont notés n.d. plutôt qu'inventés. Les tonalités des bases automatiques sont des ordres de grandeur (elles confondent souvent une tonalité et sa relative).

## 0. Ce que fait le générateur aujourd'hui (commun aux 10 styles)

Lu dans `StarterWizard.kt` / `StyleMusic.kt` / `WizardMix.kt` :
- **Lead** : `leadPattern` est une chaîne de pas (1 caractère = 1 double croche) jouée sur l'accord de chaque mesure (`withLine`). Un jeton sans `-` dure **une double croche** ; une majuscule = vélocité 0,8, une minuscule = 0,56 : **deux niveaux seulement**. Toutes nos recettes techno ont un motif d'**une mesure** répété à l'identique sur les 4 mesures de la scène, puis sur les 16 à 32 mesures de la section : aucune respiration, aucune variante, aucune question-réponse. (`barOf` accepte déjà des motifs de 32 ou 64 pas : 2 ou 4 mesures différentes ; aucune recette techno ne s'en sert pour le lead.)
- **Accords** : `chordRhythm` par mesure, voicing empilé sur la gamme (`Triad`, `Seventh`, `Power`, `Octave`, `Single`), vélocité 0,75 ; pas de renversement, pas de forme parallèle (m7/m9 « chord memory »).
- **Basse** : `bassPattern`, vélocité 0,9 / 0,63, longueur = 1 pas sauf `-`.
- **Lead sans motif** : arpège `UpDown` à la double croche d'un accord tenu.
- **Contre-chant** : accords en coups courts une octave au-dessus, seulement dans la 2e moitié du premier Drop.
- **Forme club** (`sectionsOf`) : Intro 16, Groove 16, Drop 32, Break 8, Drop 32, Intro 16 = 120 mesures, mais plafonnée à **3 min 30** (`MAX_SONG_SECONDS`) : à 132 BPM on retombe sur des Drops de **16 mesures** (88 mesures, 2 min 40 ; banc : techno 160 s, minimal 165, Detroit 168, industrielle 155, rétro 165). La batterie est écrite sur 4 mesures avec un fill sur le dernier temps de chaque phrase ; le Drop n'ajoute que les lignes `drop` ; le riser monte sur 4 mesures ; les `sweeps` ouvrent un filtre par section.
- **Sons** : un seul synthé par partie, rendu **mono** (unisson sommé en mono, `docs/plan/son.md` § 1.2 cause 3) ; élargisseur seulement sur les nappes tenues ; pompe sur la basse (House/Techno hors `OWN_PUMP`).
- **Mesures (banc après étape 2)** : Drop − Groove médian **−0,2 LU** (cible ≥ +2), côté/centre **−17,6 dB** (techno) à **−38,4 dB** (acidcore) pour une cible −9 à −15, corrélation 0,93–1,00 (cible 0,3–0,85), air > 6 kHz −18 à −25 dB (cible −16 à −20), LUFS −6,8 à −8,8.

---

## 1. Techno (`techno`, 132 BPM)

### 10 morceaux repères
| # | Artiste – Titre (année) | BPM | Tonalité | Source |
|---|---|---|---|---|
| 1 | Amelie Lens – Hypnotized (2019) | 132 | Si♭ mineur | mixgraph (5:39) |
| 2 | Adam Beyer & Bart Skils – Your Mind (2018) | 126 | Si mineur | mixgraph (8:22) |
| 3 | Ben Klock – Subzero (2009) | 125 | Do♯ mineur | mixgraph (6:23) |
| 4 | Laurent Garnier – Crispy Bacon (1997) | n.d. (≈ 135–140) | n.d. | – |
| 5 | Charlotte de Witte – Doppler (2021) | 135 | Do♯ (songbpm) / Si♭ maj–Sol min (mixgraph) | songbpm, mixgraph (7:13) |
| 6 | Enrico Sangiuliano – Astral Projection (2017) | 125 | La♭ mineur | mixgraph (8:03) |
| 7 | Slam – Positive Education (1993) | 127 | Mi♭ mineur | mixgraph (8:37) |
| 8 | Len Faki – My Black Sheep (2008) | n.d. | n.d. | – |
| 9 | Dave Clarke – Red 2 (Wisdom to the Wise) (1995) | n.d. | n.d. | – |
| 10 | Underworld – Rez (1993) | n.d. | n.d. | – |
(Joey Beltram – Energy Flash et Jeff Mills – The Bells servent aussi de modèles d'accroche techno ; ils sont détaillés en § 2 et § 3.)

### Traits de l'ensemble
- **Tempo** : 125–135, peak time 126–132, « commencer à 128 » (Beatportal, guide Drumcode). Durées 5:39 à 8:37.
- **Tonalité, harmonie** : tout en mineur ; un seul accord ou un bourdon, stabs mineur 7 ; aucune grille d'accords. Beatportal : « garder le riff minimal, une boucle de 1 ou 2 mesures suffit, le mouvement vient de l'automation ou d'un LFO sur cutoff et résonance ; superposer des textures discrètes (bruit blanc, granulaire) ».
- **Structure (mesures)** : Beatportal donne des durées : intro 1 min (≈ 32 mesures à 128 : kick, basse, percu minimale), montée 1 min (synthés, charleys, risers), drop 30 s (≈ 16), breakdown 1 min (on dépouille), second drop 1 min (variations), sortie. Energy Flash (Attack « Deconstructed », 123 BPM) : rim 707 à la mesure 5, clap 909 à 9 avec claps de fill à la fin de **chaque section de 4 mesures**, **le riff n'entre qu'à la mesure 25**, tambourin 33, crashs et rides 49, mini-breaks de 2 mesures sans kick (71, 77), break de 8, sortie de 8 ; arrangement en trois parties « comme un film ». The Bells (137,5 BPM, La mineur) : notes seules + kick, charleys ouverts à 9, claps à 17, mélodie à 25, basse à 65 : première moitié menée par la mélodie, seconde par la basse ; tout changement par **mute/unmute de boucles**, par blocs de 2 et 4 mesures dans des sections de 8. Elephorm : progression linéaire par vagues de 8 ou 16 mesures, pas de couplet/refrain.
- **Accroche / mélodie** : une note ou deux. Energy Flash : **une seule note** (La♭) pulsée, la **résonance du filtre monte et descend** « de façon robotique », réverbe courte et métallique, délai panné à droite, filtre plus erratique dans la 3e partie. The Bells : « simple aller-retour de notes », **deux couches de cloche** (une aiguë courte presque permanente, une grave plus longue qu'on retire et remet), basse en doubles croches « chuffy » qui fait aussi office de charley. Lead polymétrique (Production Music Live) : boucle de **7 notes** (Ré3 Fa3 Ré2 Ré3 Fa3 Fa2 Ré2) qui se décale à chaque mesure et retombe sur la grille à la 7e ; decay ≈ 220 ms, scie + carré PW 15 %, filtre 24 dB, chorus, délai 1/1, longue réverbe compressée par le kick. Stab techno (Attack/Hive) : **carré, une note Fa♯1**, en triolets, decay 66 / release 29, soft-clip, délai ping-pong pour la largeur, overdrive 13 %, saturateur 10 dB, réverbe prédélai 3 ms, **réverbe montée en automation sur la dernière note de la phrase**, copie du stab 4 mesures plus loin passée dans un passe-bande résonant. Registre : grave à médium (Fa♯1, Ré2–Fa3), jamais octave 5.
- **Basse** : doubles croches roulantes « avec quelques syncopes, en variant la longueur des notes pour un effet de pulsation », sinus ou scie saturée, compressée par le kick, passe-bas automatisé dans les breaks (Beatportal) ; ou rumble (second kick long → longue réverbe mouillée → passe-bas résonant + 4 dB de drive → Kickstart, Attack ?p=71776). Black Sea (Drexciya, 138) : basse 16e saturée dont **le filtre s'ouvre et se ferme tout au long du morceau**.
- **Batterie** : 909. Kick compressé, enveloppe de hauteur marquée, decay raccourci, passe-haut 50 Hz, −3 dB à 500 Hz (Attack rumble). Charleys fermés « autour du contretemps, **≤ 70 % de vélocité** », shaker au contretemps « **un coup sur deux plus bas** », ride en croches « **un coup sur deux plus court et plus doux** », charley ouvert long (Attack rumble) ; charleys 16e dont la vélocité **baisse sous le kick** (Attack chugging) ; rolling techno (131 BPM, **swing 20 %**) : kick fantôme avant 2 et 4, claps avec fantômes sur grille 16e sur 2 mesures, 2e charley ouvert **retardé et à ≈ 50 %**, toms filtrés alternés. Claps de fill en fin de chaque 4 mesures (Energy Flash). Swing 50–54 %.
- **Design sonore, effets** : risers = bruit blanc + passe-haut automatisé (Beatportal) ou filtre résonant « trempé dans une réverbe métallique » (Energy Flash) ; crash 909 au début des sections ; impacts et « sub drop » avant chaque drop ; Drum Buss drive 17 %, boom 45 % à 55 Hz (Attack rumble).
- **Sonie** : club ≈ −6 LUFS (Sweetwater), un ingénieur techno : ≈ −8 LUFS / −0,7 dBTP ; grave en mono sous 150 Hz (Futureproof).

### Notre recette (extraits)
`leadPattern = "R..O..R..O..R.O."` (Lead filtré, octave 4, lead décoché par défaut) · `pad = "Pluck"`, `chordRhythm = "...X......X....."`, `Seventh`, octave 3, preset `dub_echo` · `bassPattern = ".rRr.rRr.rRr.rRr"` (Basse analogique, octave 2, pompe) · `progressions Dark = Phrygian (0,0,0,1)` · `drop = OpenHat + Ride` · `breakWith = Hat + Perc + Pad + Lead` · `sweeps pad` · `riser`. Banc : Drop −8,2 LUFS, Drop − Groove −0,6 LU, côté/centre −17,6 dB, air −21,8.

### Écarts, du plus audible au moins audible
1. **L'accroche ne vit pas** : 6 blips de double croche identiques pendant 88 mesures. Modèles : une note dont le filtre respire (Energy Flash), deux couches qu'on retire/remet par blocs de 2–4 mesures (The Bells), réverbe qui monte sur la dernière note de phrase (Attack stab). → Recette : lead sur **2 mesures** avec tenues et silence, ex. `"R-..O-..R-......|R-..O-..R-..3-.."` (minuscules sur les réponses) ; générateur : **variante A′ toutes les 8 mesures** (une note changée ou un temps de silence), **automation du cutoff du lead** en cycle de 16 mesures (0,35 → 0,8 → 0,35) et **envoi réverbe du lead** 0,1 → 0,5 sur le dernier temps de chaque phrase de 8. Vérif : analyse des notes (≥ 2 variantes de 4 mesures par section, ≥ 1 temps de silence par 4 mesures), courbe d'automation présente sur le lead dans le Drop.
2. **Registre et longueur** : octave 5–4 et notes d'un pas, contre Fa♯1–Fa3 et decay ≈ 220 ms. → `leadOctave = 3`, notes tenues (`-`), decay du « Lead filtré » ≈ 220 ms ; ou motif polymétrique de **7 pas** (`"R.3.R.O"`) : le générateur doit accepter une longueur de motif ≠ 16 (aujourd'hui `barOf` tronque/complète à 16).
3. **Un seul synthé, mono** (côté/centre −17,6, corrélation 0,97). → Stab = **2 couches** (carré octave 2 court + Lead filtré octave 4), chorus ou copie désaccordée ±8 cents sur le lead, délai ping-pong 1/8 pointé 20 % ; cible côté/centre du Drop −12 dB (moteur : rendu stéréo du synthé, `son.md` étape 4).
4. **Le Drop n'est pas un drop** (−0,6 LU). → Drop = lead **activé par défaut** + ride + charley ouvert + **crash** sur la mesure 1 + filtre de basse 0,5 → 1 sur les 4 dernières mesures du Groove ; cible Drop − Groove ≥ +2 LU.
5. **Basse figée** : 16 notes identiques. → vélocités en cycle **100/70/85/60** sur les 4 doubles croches, longueurs alternées (`"rR-r"`), passe-bas 0,4 → 1 du Groove au Drop (modèle Black Sea), rumble coché par défaut en plus de la basse.
6. **Batterie sans vie sur 4 mesures** : seules les lettres X/x/o nuancent. → charleys ≤ 70 %, charley ouvert `"..X...x...X...x."` (2e à ≈ 50 %), ride `"o.x.o.x."` un coup sur deux plus court, claps fantômes sur 2 mesures `"....X.......X...|....X..o....X..o"`, claps de fill au dernier temps de **chaque** 4 mesures (déjà fait par `WizardMix.phrase` : garder).
7. **Forme trop courte** : 2 min 40, intro 16, break 8, drops 16 contre 5:39–8:37, intro ≈ 32, break 16–32, premier riff à la mesure 25. → plafond Techno 5 min 30 : Intro 32 (kick, percu, basse filtrée), Groove 32, Drop 32, Break 16, Drop 32, Sortie 32 = 176 mesures ≈ 5 min 20 à 132.
8. **Sources** (hors mélodie) : kick 909 court à enveloppe de hauteur + rumble en chaîne (réverbe 3–5 s → distorsion → passe-bas 150–200 Hz → pompe → mono), `son.md` étape 3 ; crash et sub-drop d'impact.

---

## 2. Rétro techno (`retro_techno`, 128 BPM)

### 10 morceaux repères
| # | Artiste – Titre (année) | BPM | Tonalité | Source |
|---|---|---|---|---|
| 1 | T99 – Anasthasia (1991) | 128 | n.d. | songbpm |
| 2 | L.A. Style – James Brown Is Dead (1991) | n.d. (≈ 128–132) | n.d. | – |
| 3 | Second Phase – Mentasm (1991) | 130 | n.d. | songbpm, RBMA |
| 4 | Joey Beltram – Energy Flash (1990) | 123 | La♭ (note unique) | Attack Deconstructed |
| 5 | Human Resource – Dominator (1991) | 123 | n.d. | songbpm |
| 6 | Cubic 22 – Night in Motion (1991) | n.d. | n.d. | – |
| 7 | Quadrophonia – Quadrophonia (1990) | n.d. | n.d. | – |
| 8 | Channel X – Rave the Rhythm (1991) | n.d. | n.d. | – |
| 9 | Moby – Go (1991) | n.d. (≈ 125–128) | n.d. | – |
| 10 | Outlander – Vamp (1991) | n.d. | n.d. | – |
Les titres à hoover attestés par Wikipedia (1991) : Mentasm, Dominator, Anasthasia, Acid Pandemonium (Mundo Muzique), Charly (Alley Cat Mix), Cactus Rhythm (Plexus), Inssomniak (DJPC).

### Traits
- **Tempo** 123–130 ; durées 5–6 min (recherche précédente).
- **Harmonie** : un riff mineur ou une seule note ; stabs = accords échantillonnés joués d'un doigt.
- **Le hoover** (Wikipedia « Hoover sound ») : patch « What the » de l'Alpha Juno, **3 oscillateurs espacés d'une octave**, scie à **PWM** modulée par un **LFO rapide qui pilote aussi un chorus épais** : c'est la « swirliness ». Mentasm (RBMA) : riff = **séquence MIDI avec pitch bends** jouée en boucle sur le Juno 1, puis la boucle entière est échantillonnée dans un **Casio FZ-1** dont le LFO et le filtre (DCF) sont bougés **à la main pendant l'enregistrement** ; un **slap delay** souligne le mouvement du filtre ; le balayage passe-bas « lourd » fait le drop ; batterie MPC60 (cowbell et snare type 808), motifs gardés « familiers » pour contrebalancer le chaos du riff.
- **Structure** (Energy Flash) : riff à la mesure 25, section du milieu (49–108) où **une nappe de cordes remplace le riff**, deux breaks de 2 mesures sans kick (71, 77), ligne de cordes à deux notes dissonantes (Fa / Sol♯) à 81, break de 8, 3e partie à 109 avec le riff au filtre « erratique », sortie de 8 mesures, **sections de 4 mesures** « contre la convention des 8/16 ».
- **Accroche** : 1 à 2 mesures, notes de croche/noire (pas de double croche en rafale), **glissés**, pitch bend, le filtre fait la dramaturgie sur 8–16 mesures.
- **Batterie** : 909 + 707 (rim 707, tambourin 909 en 16e, crashs, rides pannées) ; claps de fill en fin de 4 mesures ; risers = filtre résonant dans une réverbe métallique.
- **Espace** : réverbe « serrée et métallique », délai panné d'un côté : la percu est sèche, un seul clap aigu baigné de réverbe à droite « brouille le sens de l'espace » (Attack).

### Notre recette (extraits)
`lead = "Hoover"`, `leadPattern = "R..R..O/R..R..3."`, `leadGlide = true`, octave 4, signature · `pad = "Hoover"`, `chordRhythm = "X-------X-------"`, `Single` · `bassPattern = "R..r..R.R..r..R."` · snare `"............................x234"` (roulement toutes les 2 mesures), perc zap toutes les 2 mesures · `effects`: lead CHORUS + reverb 0,2, pad CHORUS · `sweeps lead` 0,25→0,6 / 0,6→0,85 / 0,35→0,9 · `Dark = Minor (0,0,5,6)`. Banc : −8,2 LUFS, côté/centre −29,5, corrélation 1,00.

### Écarts
1. **Riff en blips** : 6 doubles croches par mesure ; le vrai riff a des notes **tenues** et des **bends**. → `"R---..R---..O/R-|R---..R---..3---"` (2 mesures ; `-` = tenue, `/` = glissé déjà géré par `leadGlide`), `leadOctave = 3`.
2. **Hoover maigre** : une couche, UNISON 1 / détune 0,8, pas de PWM. → 2 couches **à l'octave** (lead octave 3 + nappe hoover octave 2 sur les mêmes notes, nappe cochée par défaut) ; préset : LFO2 rapide sur la largeur d'impulsion si le synthé le permet (sinon `OSC2_WAVE` carré + détune 0,52 comme « Lead carré ») ; chorus déjà posé : garder.
3. **Le filtre fait le drop** : nos `sweeps` sont linéaires sur toute une section ; Mentasm ouvre et ferme **à la main**, avec résonance. → résonance du sweep 0,25 → **0,5**, cycle de 8 mesures (0,3 → 0,9 → 0,3) dans le Drop plutôt qu'une rampe de 16 ; slap delay 1/16 sur le lead (`delay 0,2` → envoi délai court).
4. **Roulement trop fréquent** : toutes les 2 mesures, contre un roulement en fin de phrase de 8–16. → snare sur 64 pas, roulement seulement sur la 4e mesure (ou laisser `WizardMix.phrase` faire le fill de 4 mesures et retirer la ligne snare).
5. **Section du milieu absente** : Energy Flash remplace le riff par des cordes 60 mesures. → Break de 16 mesures avec **nappe (Machine à cordes) à la place du hoover**, deux notes dissonantes (`R` et `2` en phrygien) tenues.
6. **Mono total** (−29,5 dB) : délai panné d'un côté, un clap aigu en réverbe longue panné à droite, le reste sec ; cible −12 à −15 dB.
7. **Sections de 4 mesures et mini-breaks de 2 mesures sans kick** (Energy Flash 71, 77) → dans le Drop, kick coupé 2 mesures toutes les 16 (`phrase` ne coupe que le dernier temps).

---

## 3. Techno de Detroit (`detroit`, 126 BPM)

### 10 morceaux repères
| # | Artiste – Titre (année) | BPM | Tonalité / accords | Source |
|---|---|---|---|---|
| 1 | Rhythim Is Rhythim – Strings of Life (1987) | ≈ 122 | 4 accords, **sans basse** | songbpm, Attack, Wikipedia |
| 2 | Model 500 – No UFO's (1985) | 125 | La mineur | Attack Deconstructed |
| 3 | Jeff Mills – The Bells (1996) | 137,5 | La mineur | Attack Deconstructed |
| 4 | DJ Rolando – Knights of the Jaguar (1999) | n.d. (≈ 125–130) | Ré mineur (mixgraph) | mixgraph |
| 5 | Galaxy 2 Galaxy – Hi-Tech Jazz (1993) | n.d. | n.d. | – |
| 6 | Cybotron – Clear (1983) | n.d. | n.d. | – |
| 7 | Inner City – Good Life (1988) | 121 | Si mineur, i–v–VII–iv (le lien Hooktheory écrit « vii ») | Hooktheory |
| 8 | Derrick May – Nude Photo (1987) | n.d. | n.d. | – |
| 9 | Drexciya – Black Sea (≈ 1995) | 138 | La mineur, accords parallèles échantillonnés | Attack Deconstructed |
| 10 | Underground Resistance – The Final Frontier (1991) | 138 | n.d. | Attack Beat Dissected |

### Traits
- **Tempo** 121–138 (première vague 121–125, seconde 137–138). Swing 50 % (Final Frontier) à 55–60 % (recherche précédente).
- **Harmonie** : mineur ; Good Life i–v–VII–iv (mélodie Fa♯4–Si5, 0,57 temps/note, 97 % diatonique) ; Strings of Life « quatre accords » de piano puis cordes ; Black Sea : **accords échantillonnés au même voicing, déplacés en parallèle**, pannés à droite, « qui changent d'ordre vers la fin » ; recherche précédente : m7/m9 d'un doigt (Juno, DX100, Mirage).
- **Structure** : No UFO's : intro 8 (kick 808 en 16e seul), instrumental 8 (la basse entre), couplet 16, refrain 8 (l'énergie **baisse** au refrain, les charleys sortent), break instrumental 8, couplet 16, refrain 8, breakdown 16, sections finales de 4, fondu 8 ; « **pas de fill ni de trou : la variation vient du changement de motif au milieu d'une section** », crash de bruit blanc à chaque transition, claps dont le motif change à mi-couplet. The Bells : charleys à 9, claps à 17, cloches à 25, basse à 65, 2 moitiés. Strings of Life : piano seul → cordes qui **frappent le 1er temps de la phrase puis tiennent** → piano une octave plus haut → batterie → roulement de 2 mesures → break de 2 mesures (cordes tenues, kick 4 coups puis sort) → snare 909 sur 2 et 4 + charley 16e pendant 2 mesures → … → section « freestyle » (kick baissé, ride 4 coups) → nouvelle section piano/cordes + bongos 727 → retour du riff 8 mesures.
- **Accroche** : riff de piano répété **avec changement d'octave puis nouvelle mélodie sur le même rythme** ; cordes en contre-rythme ; cloches : deux couches, aller-retour de notes, changements par 2–4 mesures ; No UFO's : séquence aiguë d'une note sous la basse, mini-mélodie entre les lignes de voix.
- **Basse** : funky, analogique, **au centre, seule au milieu**, sauts d'octave (recherche précédente) ; The Bells : 16e « chuffy » ; Strings of Life : aucune (le kick tient le grave).
- **Batterie** : 909/808 mêlés ; Final Frontier (808, 138, swing 50 %) : **vélocité d'un charley sur deux ajustée**, fills de snare **plus doux**, rim sur 2 mesures panné droit, conga pannée gauche avec **délai 1/8 pointé, 70 % de feedback, 20 % wet**, clap avec réverbe prédélai 17 ms / decay 850 ms / 17 % wet, saturateur kick drive 7 dB mix 20 %, Glue 4 dB de réduction, Drum Buss drive 30 %, réverbe de pièce 7 %. Strings of Life : shaker qui reste pendant les breaks, roulements de snare hauts et bas, bongos 727.
- **Mix** : batterie sous-mixée d'un côté (8 pistes), basse seule au centre, deux motifs de charleys superposés.

### Notre recette (extraits)
`pad = "Machine à cordes"` signature, `chordRhythm = "X-.X-.X-..X-.X-."`, `Seventh`, octave 3 · `bassPattern = "R..O..R.r..O..R."`, Basse ronde, **octave 1** · `leadPattern = ""` (arpège Cloche UpDown à la double croche, octave 5, décoché) · `Happy = Dorian (0,3,0,3)` par défaut · swing 0,15 · ride au Drop · drums SATURATION. Banc : −8,1 LUFS, Drop − Groove 0,0, côté/centre −25,6, pente −3,30 (trop d'aigus), médium −9,2.

### Écarts
1. **Les cordes mitraillent** : 6 coups par mesure, chaque mesure, dès l'intro. Modèle : un coup sur le 1er temps de la phrase puis tenue, et une 2e couche en contre-rythme. → `chordRhythm` sur **2 mesures** : `"X-----------x---|X---------------"` (frappe + tenue), et contre-chant « Piano » (`counter = "Piano électrique"`, `counterRhythm = "..x..x....x...x."`) en coups courts **dès le Groove** ; variante d'octave de la nappe (+12) au 2e Drop (`dropTwo`).
2. **Accords empilés sur la gamme** au lieu de **formes parallèles m7/m9** (chord memory, Black Sea). → `Voicing` à intervalles fixes `0,3,7,10` / `0,3,7,10,14` transposés en bloc (STYLE-FIXES F11), avec voicing resserré sous une octave.
3. **Arpège de cloche** à la double croche = machine à coudre. → The Bells : lead sur 2 mesures `"R.3.R...5.3.R..."` + 2e cloche grave (octave 3) sur `"R.......5......."` qu'on **mute une phrase sur deux** (générateur : mute alterné par 4 mesures d'une piste = automation de volume comme `dropTwoLane`).
4. **Basse inaudible** (octave 1 : 55 Hz, CORPS −14 dB) → octave 2, pompe, sauts d'octave gardés, au centre.
5. **Variation sans fills** : changer le motif de clap/charley **à mi-section** (mesures 9–16 d'un Groove de 16) plutôt qu'un fill ; nos 4 mesures sont identiques. → lignes de batterie sur 32 pas avec 2e mesure différente, charleys à vélocité alternée, fills de snare plus doux (0,45–0,6 au lieu de 0,85).
6. **Effets par rôle** : conga/rim avec délai 1/8 pointé 70 % feedback, clap réverbe 850 ms, cordes réverbe 0,3 (déjà) ; aujourd'hui perc réverbe 0,15 seulement.
7. **Forme** : sections de 8 et 16 avec un refrain qui **baisse** l'énergie et un breakdown de 16 ; nos Drops de 16 à 126 BPM (2 min 48). → plafond 5 min, Break 16 (cordes tenues + shaker).

---

## 4. Techno industrielle (`industrial_techno`, 136 BPM)

### 10 morceaux repères
| # | Artiste – Titre (année) | BPM | Tonalité | Source |
|---|---|---|---|---|
| 1 | I Hate Models – Daydream (2016) | 136 | n.d. | songbpm (mixgraph donne 126 / Mi♭ maj : douteux) |
| 2 | Perc – Take Your Body Off (2014) | 136 | Fa♯ mineur | mixgraph (7:43) |
| 3 | Blawan – Why They Hide Their Bodies Under My Garage? (2012) | n.d. | n.d. | – |
| 4 | Surgeon – Force + Form (1999, album) | n.d. | n.d. | recherche précédente |
| 5 | Paula Temple – Colonized (2013) | n.d. | n.d. | – |
| 6 | Perc – My Head Is Slowly Exploding (2011) | n.d. | n.d. | Four Four |
| 7 | Perc – Resistor (2021) | n.d. | n.d. | Four Four (« hard noisy techno sans stabs rave ») |
| 8 | SNTS – Rationality (2018) | n.d. | n.d. | Electronic Beats (P. Temple) |
| 9 | Death Abyss – We Are What You Should Fear (2014) | n.d. | n.d. | Electronic Beats |
| 10 | Stave – Hardened Chord (Regis Remix) (2015) | n.d. | n.d. | Electronic Beats |

### Traits
- **Tempo** 130–140 (Daydream 136, TYBO 136 ; Perc/Regis ≈ 130 ; courant 2016+ 135–145).
- **Harmonie** : absente, bourdon, phrygien au plus. Perc (Four Four) revendique « sans stabs rave ni sons hardcore ».
- **Accroches** : du **bruit et du métal**, pas des notes : « machinerie grinçante qui entre à 2:43 » (Regis remix), « tonnerre hors axe » (Technoise), « basse overdrivée qui pousse contre le kick » (Death Abyss), voix de moine (SNTS), voix déformée (TYBO, chant de Dan Chandler), rythmes mécaniques enregistrés (Lakker).
- **Structure** : Technoise – System « ni intro, ni climax, ni descente », boucles filtrées ; Paula Temple **édite** ses morceaux pour allonger les passages les plus forts et accélère parfois le BPM ; recherche précédente : breaks = on retire le kick, le rumble ou le bruit reste.
- **Batterie / basse** : 909 saturé + kicks fantômes, rumble, toms filtrés, chocs métalliques, clap noyé de réverbe.
- **Sonie** : tout saturé ; dynamique plate mais **les couches entrent et sortent** (cf. Black Sea : « tout par les potards de volume et de decay »).

### Notre recette (extraits)
kick `"X..oX...X..oX..."` + distortion, toms, charley au contretemps, snare vapeur, rumble, perc métal DISTORTION · `pad = "Essaim"`, `Power`, `"X---------------"` octave 3, reverb 0,4 · `lead = "Cloche"` preset `robot`, `"R..2..R...R..2.."` octave 4 · bass sub optionnelle · `breakWith = Rumble + Pad + Lead` · `Dark = Phrygian (0,0,0,1)`. Banc : −7,7 LUFS, côté/centre −18,4, corrélation 0,97, LRA 0,6.

### Écarts
1. **Le lead « cloche robot » sonne jouet** : 4 blips par mesure. Les accroches du genre sont des **coups de métal rares à longue réverbe** et des bruits. → lead sur 2 mesures très creux : `"R.......|......2."`, son « Essaim » ou bruit (NOISE 1, SWEEP) court + DISTORTION + réverbe 0,5, octave 3 ; ou un échantillon de pack `fx` (choc métallique). Deux niveaux de vélocité ne suffisent pas : coup fort puis **écho à −12 dB**.
2. **Bourdon statique** : « Essaim » tenu sans bouger pendant 16 mesures. → passe-bas lent (sweep 0,3 → 0,8 → 0,3 sur 16 mesures, résonance 0,4) + couche de bruit (NOISE 0,3) + chorus ; cible : centroïde spectral de la nappe qui varie dans le Drop.
3. **Rien n'entre ni ne sort** (LRA 0,6 LU) : le genre vit de mutes de boucles toutes les 8 mesures. → générateur : dans le Drop, **une ligne retirée 8 mesures puis remise** (toms, puis perc) ; cible LRA 2–4 LU.
4. **Voix** : impossible sans échantillons ; « Voix robot » (Plaits) en 2 notes tenues avec DISTORTION au Break ferait l'affaire.
5. **Rumble** et kick : juste (recherche précédente) ; ajouter le passe-haut 30 Hz et la coupe au-dessus de 150–200 Hz du rumble (Futureproof) pour ne pas boucher le grave (sub −4,6 / grave −4,1 : le bas-médium à −10,8 est bon).
6. **Stéréo** : −18,4 dB, correct pour le genre ; les chocs métalliques pannés en alternance gauche/droite (`PANS` perc +0,35 / perc2 −0,35 : ajouter une 2e perc).

---

## 5. Hard techno (`hard_techno`, 150 BPM)

### 10 morceaux repères (top ventes Beatport hard techno 2024, Beatportal)
| # | Artiste – Titre (année) | BPM | Tonalité |
|---|---|---|---|
| 1 | 1 World, Coolio, Holy Priest – Gangsta's Paradise (Extended) (2024) | n.d. (≈ 150) | n.d. |
| 2 | Alt8 & Sara Landry – Heaven (2024) | n.d. (≈ 150–155) | n.d. |
| 3 | Remon Verhoeve – I Wanna Feel (2024) | n.d. | n.d. |
| 4 | Frankyeffe – Save Me (RiVid Remix) (2024) | n.d. | n.d. |
| 5 | DRAKK & Warp Brothers – Phatt Bass (Extended) (2024) | n.d. | n.d. |
| 6 | KlangKuenstler & Sant – Toter Schmetterling (2024) | n.d. | n.d. |
| 7 | Luciid – Era Of Us (2024) | n.d. | n.d. |
| 8 | SveTec – Your Favourite Little Chaos (2024) | n.d. | n.d. |
| 9 | Marco Leckbert – Schranz Will Never Die (2024) | n.d. | n.d. |
| 10 | EFESIAN – Violate (Extended) (2024) | n.d. | n.d. |
Mixgraph n'a aucune de ces pages ; Futureproof : 145–160, peak time **150–155** ; schranz 140–150 (Mixmag, « utilisation en rafale de boucles »). Six des dix sont des remakes ou des reprises (Gangsta's Paradise, Phatt Bass, Save Me…) : **la voix ou le riff connu est l'accroche**.

### Traits
- **Tonalité** : kick **accordé** sur la tonique (La ou Do courants, Futureproof ; Fa♯ chez Producer School) ; harmonie minimale, i–bII ; trance euphorique dans les breaks (recherche précédente).
- **Structure (Futureproof, en mesures)** : intro **16–32** (kick + rumble, percu), 1re section **32–64** (basse acide, lead, voix ou choc atonal), breakdown **16–32** (kick coupé, risers, bruit blanc, boucles filtrées), re-entrée **32–64** (nouveau lead ou pile de distorsion), sortie 16–32 ; **6 à 8 min** ; « l'énergie vient des breakdowns et des re-entrées, pas de montées de 16 mesures au roulement ». Producer School, « faux drop » : fill de snare + risers → impact avec bribes de lead → crash, kicks et batterie saturée **mais grave filtré** → court hook vocal/tonal → **vrai drop avec le grave**. Intro : **pitch bend** du lead qui glisse.
- **Lead (Producer School, Serum)** : **2 scies en unisson 16 + 15 voix**, bruit large en FM ≈ 133 % sur l'oscillateur A, filtre passe-bande qui pousse les hauts-médiums, distorsion à lampe, délai, réverbe, **automation du filtre**, joué **sur la tonique Fa♯** (une note, tenue). Hoovers, sirènes, supersaw hard trance, remakes (recherche précédente). Basse-lead saturée 100–400 Hz (Futureproof).
- **Basse** : le rumble (envoi parallèle du kick → réverbe **3–5 s 100 % wet** → distorsion lourde → passe-haut 30 Hz + coupe plateau au-dessus de **150–200 Hz** → sidechain sur le kick → **mono**) ; ou 303 mineure 8/16 pas, accents, slides, **cutoff automatisé sur 8–16 mesures**, passe-haut 150 Hz.
- **Kick** : court, saturé, enveloppe de hauteur sur **10–20 ms**, accordé ; 3 couches (base accordée Fa♯ + Decapitator 50 % wet ; top court et brillant + petite room ; mid très saturé à decay raccourci).
- **Batterie** : ride 909 en boucle à **vélocités « rebondies »**, transitoire durci, distorsion ; boucle de break empilée, sidechain serré ; percu schranz à 3 pas.
- **Mix / sonie** : **tout en mono sous 150 Hz**, le rumble est le seul sub ; sidechain rumble + basse + parfois lead ; **−6 à −5 LUFS**, soft-clipping étagé + limiteur sur les crêtes ; M/S : couper 200–400 Hz sur les côtés, plateau haut à partir de 8 kHz sur les côtés.

### Notre recette (extraits)
`lead = "Supersaw"`, `"R..R..R.O..O..5."`, octave 5 · `pad = "Hoover"`, `Octave`, `"X..X..X...X..X.."` octave 3 · `bassPattern = ".rRr.rRr.rRr.rRr"` sub, `fat_bass` + PUMP (décochée par défaut) · rumble coché · ride au Drop · clap avec roulement toutes les 2 mesures · OTT master · `sweeps pad` Break 0,3→0,9 · `Dark = Phrygian (0,0,1,0)`. Banc : **−8,8 LUFS** (plafonné : la chaîne retire déjà 6,2 LU), Drop −8,8, côté/centre −27,6, corrélation 1,00, air −18,3.

### Écarts
1. **Lead supersaw en doubles croches à l'octave 5** : 6 blips/mesure, cible = **une note tenue sur la tonique** avec filtre qui bouge, ou riff de 2 mesures à la hard trance. → `"R-------R---..O-|R-------R---5---"` octave **4**, `leadGlide` avec un `/` dans l'intro (bend), automation cutoff 16 mesures (0,3 → 0,9), 2e couche octave au-dessous (nappe hoover sur les mêmes notes) ; unisson 7 voix détune 0,45 déjà là : ajouter chorus + bruit (NOISE 0,2) + DISTORTION légère + passe-bande (filtre 0,55, résonance 0,5).
2. **Stabs hoover en mitraille** (6/mesure, chaque mesure). → `"X-------........|........X---..x."` (2 mesures), ou hoover tenu 1 mesure sur 2.
3. **Faux drop / impact** : riser 4 mesures + impact existent ; manquent le **crash**, le **grave filtré pendant les 4 premières mesures du Drop** (filtre passe-haut sur le bus batterie 0,3 → 0 puis saut) et le hook vocal court (hors portée : remplacer par 2 notes de « Voix robot »).
4. **Sonie plafonnée** : −8,8 au lieu de −6 à −5 ; la perte de 6,2 LU vient d'un kick/kit sans densité (son.md étape 3) ; soft-clipper étagé avant le limiteur (étape 2 bis), ride et percu plus présents dans l'air (−18,3 → −16).
5. **Ride 909 figée** : `"..o...o...o...o."` à une vélocité. → `"..x.o.x.o.x.o.x."` avec decay court sur les faibles.
6. **Forme** : 192 s avec intro 16 / break 8 ; cible intro 32, break 16–32 avec **boucles filtrées** (passe-haut sur la batterie) plutôt que silence ; plafond 5 min 30.
7. **Mono < 150 Hz** : déjà (corrélation < 120 Hz = 1,000) ; mais tout le reste aussi (−27,6 dB) : élargir lead, hoover, ride.

---

## 6. Acid techno (`acid_techno`, 138 BPM)

### 10 morceaux repères
| # | Artiste – Titre (année) | BPM | Tonalité | Source |
|---|---|---|---|---|
| 1 | Josh Wink – Higher State of Consciousness (1995) | 125–126 | Sol maj (mixgraph ; ≈ Mi min) | mixgraph (9:02), songbpm |
| 2 | Hardfloor – Acperience 1 (1992) | 125–127 | Mi♭ maj (mixgraph ; ≈ Do min) | mixgraph, songbpm |
| 3 | Emmanuel Top – Acid Phase (1994) | n.d. | n.d. | – |
| 4 | Plastikman – Krakpot (1993) | n.d. | n.d. | – |
| 5 | Hardfloor – Lost in the Silverbox (≈ 1993) | n.d. | n.d. | – |
| 6 | Emmanuel Top – Turkish Bazar (1994) | n.d. | n.d. | – |
| 7 | Union Jack – Two Full Moons & a Trout (1994) | n.d. | n.d. | – |
| 8 | Chris Liberator – Spectrum (Bag Records, ≈ 1995) | n.d. | n.d. | RBMA |
| 9 | D.O.M. – Acid War (Stay Up Forever SUF005, ≈ 1995) | n.d. | n.d. | RBMA |
| 10 | Lochi – London Acid City (≈ 1995–97) | n.d. | n.d. | RBMA |

### Traits
- **Tempo** : classiques 125–127 (Higher State, Acperience) ; Londres 135–145 ; morceaux de **9 min** (Higher State 9:02, « énergie 20 % : warm-up » selon mixgraph : tout est dans la montée).
- **Harmonie** : la 303 reste sur la tonique ; mineur.
- **La 303 est la basse et l'accroche** (Wikipedia) : résonance haute, cutoff bas, accent, slide, octave ; **303 superposées** (RBMA : « underpinned with the acidic squelch of layered 303s » ; Hardfloor jusqu'à 6, recherche précédente) ; Futureproof : motif **8 ou 16 pas**, accents, slides, notes fantômes, **cutoff automatisé sur 8–16 mesures**, passe-haut 150 Hz sous un rumble ; Robert Hood (MusicRadar) : mono-synthé, enveloppe de filtre forte « type 303 », **modulation aléatoire du cutoff** pour que la boucle ne lasse pas.
- **Structure** : la 303 entre fermée et s'ouvre ; breaks où elle reste seule ; Stay Up Forever : « structures fluides, pas en boucles », synthés mélodiques « uplifting » par moments, breaks Amen sur les faces B.
- **Batterie** : 909 saturée, kick 4/4, clap 2 et 4, charley ouvert au contretemps, fermés serrés ; fills de clap/snare.
- **Son** : distorsion/saturation sur la 303 (recherche précédente), délai.

### Notre recette (extraits)
`acid = "RRO.RO.3R.OR7.O5"` (1 mesure), accents `"..x....x..x...x."`, slides `".x.........x..x."`, moves cutoff : Groove 0,30→0,42, Drop 0,45→0,55, Break 0,28→0,65 · fx acid `crunch` + delay 0,2 + PUMP · kick crunch · aucune partie cochée (lead « Lead acide » `"O..O..O..O..O.5."` et stab en option) · `Dark = Phrygian (0,0,0,0)`. Banc : −7,2 LUFS, Drop − Groove −0,2, côté/centre **−34,6**, corrélation 1,00, LRA 0,9.

### Écarts
1. **Une mesure de 303 pendant 120 mesures** (CORPS). → motif **32 pas** (2 mesures) + **variante B** toutes les 8 mesures (2 notes changées, 1 silence) ; générateur : `AcidRecipe.variant` joué aux répétitions paires (ou 2e moitié de scène de 8 mesures).
2. **Le filtre bouge à peine** : 0,30 → 0,42 dans le Groove, 0,45 → 0,55 dans le Drop ; le geste du style est l'ouverture complète sur 8–16 mesures puis la fermeture. → cycles : Groove 0,2 → 0,6, Drop 0,35 → 0,85 → 0,4 (deux cycles de 8), Break 0,15 → 0,9 ; résonance 0,6 → 0,8 pendant l'ouverture ; `AcidMove` sur param 1 (résonance) en plus.
3. **Une seule 303** : cocher par défaut le lead « Lead acide » en **question-réponse** dans les trous de la ligne (`"....5.......O..."` octave 4, delay 0,3), 2e Drop seulement (`dropTwo` le fait déjà si le lead est coché).
4. **Mono absolu** (−34,6 dB) : délai ping-pong 1/8 pointé sur la 303 (envoi 0,25), réverbe courte sur le clap, charleys pannés.
5. **Rien n'entre ni ne sort** (LRA 0,9) : la 303 devrait jouer **seule, fermée, dans l'intro** (comme la forme Rave de l'acidcore) ; aujourd'hui `acidScenes = {1, 2}` + Break. → forme Rave pour acid_techno ou `acid` dans l'intro avec cutoff 0,1 → 0,3.
6. **Batterie** : juste ; ajouter un roulement de clap (`"....X.......X.......X.......X.2."`) et la ride au Drop (déjà).
7. **Forme** : 3 min 28 ; cible intro 32 avec 303 seule, Drop 32–64.

---

## 7. Gabber (`gabber`, 175 BPM)

### 10 morceaux repères
| # | Artiste – Titre (année) | BPM | Source |
|---|---|---|---|
| 1 | Rotterdam Termination Source – Poing (1992) | ≈ 147 | songbpm ; n° 1 NL (Wikipedia) |
| 2 | Euromasters – Amsterdam Waar Lech Dat Dan? (1992) | ≈ 142 | songbpm |
| 3 | Holy Noise – James Brown Is Still Alive (1991) | n.d. | – |
| 4 | Technohead – I Wanna Be A Hippy (1995) | ≈ 190 | songbpm ; 5:03, n° 1 NL/DE/AT (Wikipedia) |
| 5 | Mescalinum United – We Have Arrived (1990) | n.d. | « premier morceau hardcore » (Wikipedia Hardcore techno) |
| 6 | Charly Lownoise & Mental Theo – Wonderful Days (1994) | n.d. (≈ 160) | – |
| 7 | Paul Elstak – Rainbow in the Sky (1995) | n.d. (≈ 170) | – |
| 8 | DJ Chosen Few – Chosen Anthem (Against Racism) (1993) | n.d. | Wikipedia Gabber (Mokum) |
| 9 | Neophyte – Braincracking (≈ 1995) | n.d. | – |
| 10 | Angerfist – Incoming (≈ 2004) | n.d. | – |

### Traits
- **Tempo** : 140–190 (Wikipedia), les tubes de 1992 à 142–147, 1995 à 170–190, 2000+ (mainstream hardcore) plus lent et « sophistiqué », 2010+ **160–180 puis 200+** (Wikipedia Hardcore techno).
- **Kick** : « kick scie saturé », 909 → distorsion lourde, compression, **couches**, queue accordée (recherche précédente) ; le kick est la basse.
- **Harmonie** : mineur sombre ; happy hardcore en majeur.
- **Accroches** : hoover (T99 « a introduit un des composants les plus caractéristiques du gabber », Wikipedia), **riffs de synthé agressifs, simples** (Bassgorilla), **échantillons de films et de voix** à des moments clés, charleys « house » et **breakbeats en remplissage** (Wikipedia) ; I Wanna Be A Hippy : voix de David Peel, « radio mix fait pour blaguer ».
- **Structure** : « minimaliste » ; intro kick, montée roulement, riff, break voix, reprise ; 4–6 min.

### Notre recette (extraits)
kick distortion, charley ouvert contretemps, clap doux, charleys 909, snare `"............................x234"` · `pad = "Hoover"`, `Octave`, `"X-------........"` octave 3 · `lead = "Lead carré"`, `"R.O.R.5.R.3.R.2."`, octave 5 (décoché) · basse non · `form = Build` · OTT master · `sweeps pad` Montée 0,3→1,0 · `Dark = Minor (0,0,5,6)`. Banc : −6,8 LUFS, PLR 5,6, côté/centre −24,8, air −17,8.

### Écarts
1. **Riff carré en doubles croches alternées** à l'octave 5 : ça pique mais ça ne chante pas. Les riffs gabber sont en **croches**, 2 mesures, avec sauts d'octave et une note longue en fin de phrase. → `"R-R-O-R-5-R-3-R-|R-R-O-R-7-------"` octave **4**, lead coché par défaut au Drop ; 2e couche hoover à l'octave en dessous sur `"X-------X-------"`.
2. **Hoover d'une frappe par mesure** : juste pour « Mentasm », mais nos stabs n'ont ni bend ni mouvement. → `leadGlide` sur la nappe (slide `/` au début de la mesure 3 : `"X-------........|........X/X-----"`), sweep 0,3→1,0 gardé, résonance 0,5.
3. **Le Break manque sa voix** : impossible sans échantillon → nappe hoover seule + roulement de snare (déjà) ; ajouter un bruit « Souffle » filtré en riser (déjà).
4. **Charleys « house » de remplissage** : `"o..oo..oo..oo..o"` est bien ; ajouter sur 32 pas une 2e mesure en 16e `"xoxoxoxoxoxoxoxo"` au Drop (fillers).
5. **Kick** : la distorsion est là ; il manque l'**accord sur la tonique** et la **queue longue** (F♯ du kit intégré à 47 Hz = tonique par chance, recherche précédente). Pack kick réaccordé (STYLE-FIXES F15).
6. **Sonie** : −6,8 et PLR 5,6 : conforme au genre.

---

## 8. Acidcore (`acidcore`, 185 BPM)

Documentation publique très mince : le genre se joue en live sur sound systems et peu de titres sont « des tubes ». Wikipedia (Hardcore techno) : « **acidcore et hardtechno : lancés par des membres de Spiral Tribe, qui ont durci un son acid-breakbeat** » ; Network 23 (1994–96), styles « free tekno, spiral hardcore, French hardcore ». Free tekno : **150–185 BPM**, « kick martelé et répétitif, la distorsion du kick par écrêtage est moins utilisée que dans le mainstream hardcore », échantillons de films placés aux moments clés.

### 10 repères (années vérifiées sauf ≈)
| # | Artiste – Titre / sortie (année) | BPM | Source |
|---|---|---|---|
| 1 | Spiral Tribe – Forward the Revolution (1992, Big Life) | n.d. | Wikipedia Spiral Tribe |
| 2 | Spiral Tribe – Breach the Peace (1992) | n.d. | Wikipedia |
| 3 | Spiral Tribe – Sirius 23 (1993) | n.d. | Wikipedia |
| 4 | R-Zac (Crystal Distortion & 69db) – Trailer Trax (album, 1993) | n.d. | Mixmag / DJ Mag |
| 5 | 69db – Le Fuck Techno Import Mix (live, ≈ 1996) | n.d. | testpressing |
| 6 | Stormcore 3 (Network 23, 1996) : « hardcore freetekno > 180 BPM » | > 180 | deejay.de |
| 7 | Crystal Distortion / 69db – réédition Network 23 « four classic tracks » (1994–96) | n.d. | deejay.de |
| 8 | Mescalinum United – We Have Arrived (1990) | n.d. | racine hardcore (recette) |
| 9 | Hardfloor – Acperience 1 (1992) | 125–127 | racine acide (recette) |
| 10 | Confusion A – Out of World (1997) | n.d. | **non vérifié** (référence de la recette) |

### Traits (par recoupement)
- **Tempo** 150–185, > 180 (Stormcore 3) ; sets « lo-fi, distordus » (Sound Metaphors, 121–160 BPM pour les débuts).
- **303 en furie** : lignée acid techno (motifs 8/16 pas, accents, slides, octaves, cutoff ouvert sur 8–16 mesures, plusieurs 303 en couches) à la vitesse du hardcore ; **kick de gabber** mais moins écrêté.
- **Structure** : live, boucles qui mutent, échantillons aux moments clés, pas de drop « EDM ».

### Notre recette (extraits)
`acid = "RRORROR2RROR3RO7RRORROR2RO5O3O2O"` (2 mesures), accents, slides, son « Hurleur », knobs, moves cutoff Intro 0,08→0,45, Groove 0,4→0,5, Drop 0,55→0,68, Break 0,2→0,85 + param 2 · `form = Rave` (303 seule à l'intro) · kick gabber distortion, charleys 16e au Drop, clap · OTT + saturation master · nappe hoover et lead acide en option. Banc : −7,1 LUFS, PLR 5,9, côté/centre **−38,4**, corrélation 1,00.

### Écarts
1. **Une seule 303** sur tout le morceau (4 couches au Drop, CORPS). → lead « Lead acide » coché par défaut en **réponse** (`"....O.......5..."` octave 4, delay 0,2, cutoff automatisé en décalage d'une demi-phrase), hoover au Drop 2.
2. **Le motif ne mute pas** : 2 mesures × 100. → variante B toutes les 8 mesures (générateur, comme acid techno) ; un pas de silence de plus au Groove.
3. **Mono absolu** (−38,4 dB, le pire de la famille) : délai ping-pong sur la 303, réverbe courte sur le clap, charleys pannés ±0,2 ; cible −15.
4. **Filtre** : le mouvement est le bon geste (Intro 0,08 → 0,45) ; ajouter un **cycle de 8 mesures** dans le Drop (0,5 → 0,85 → 0,55) plutôt qu'une rampe de 32 mesures.
5. **Kick** : accordé Fa♯ ✓ ; écrêtage moindre que gabber → distortion 0,7 au lieu de 1,0 et saturation à la place.
6. **Échantillons aux moments clés** : hors portée ; un impact + silence d'un temps avant chaque Drop (riser existant) fait le minimum.

---

## 9. Minimal (`minimal`, 128 BPM)

### 10 morceaux repères
| # | Artiste – Titre (année) | BPM | Tonalité | Source |
|---|---|---|---|---|
| 1 | Ricardo Villalobos – Fizheuer Zieheuer (2006) | 124 | Si♭ majeur (sample de cuivres en majeur, ostinato) | mixgraph (37:11), Wikipedia |
| 2 | Plastikman – Spastik (1993) | n.d. | sans mélodie, 808, 9:22, « lente montée puis kicks monstrueux » | Wikipedia |
| 3 | Robert Hood – Rhythm of Vision (1994) | n.d. | n.d. | RBMA (recherche précédente) |
| 4 | DBX – Losing Control (1994) | n.d. | n.d. | – |
| 5 | Basic Channel – Phylyps Trak (1993) | n.d. | n.d. | – |
| 6 | Ricardo Villalobos – Dexter (2003) | n.d. | n.d. | – |
| 7 | Dubfire – Roadkill (2007) | n.d. | n.d. | – |
| 8 | Minilogue – Hitchhiker's Choice (2006) | n.d. | n.d. | – |
| 9 | Robert Hood – Minus (1994) | n.d. | n.d. | – |
| 10 | Marc Houle – Bay of Figs (≈ 2004) | n.d. | n.d. | – |

### Traits
- **Tempo** 124–130 (Hawtin : 128) ; durées 9 à 37 min.
- **Harmonie / accroche** : un **échantillon en ostinato** (cuivres majeurs, Fizheuer : « se répète presque tout le morceau, une 2e figure de cuivres apparaît 3 fois en tout, le reste est de la **percussion nuancée passée dans de multiples permutations de filtres et de délais** ») ; stab dub m7 (Basic Channel) ; Hood : « juste des batteries, des basses et des grooves funky » ; lead Hood (MusicRadar) : mono, enveloppe type 303, **cutoff modulé aléatoirement**.
- **Batterie (Attack « Sub-Zero », 125–130, swing 55–70 %)** : kick à fort transitoire + saturation bande en parallèle ; charley 1 brossé (shaker) avec **fantôme sur la croche**, charley 2 sec au contretemps ; snare 808 sur 2 et 4 + **fantôme juste avant chaque mesure** (sa position suit le swing) ; rim « quirky » ; cymbale inversée très basse ; stab vocal « breathy » en fin de section 8/16.
- **Basse** : courte, médium (recherche précédente).
- **Structure** : par soustraction, très long ; Spastik : montée lente de plusieurs minutes.

### Notre recette (extraits)
kick sec, hat `"..x...x...x...x."`, rim 2 mesures, clap `"....o.......o..o"`, shaker au Drop · `bass = "Basse pincée"`, `"..R...r...R..r.."`, octave 2 · `pad = "Pluck"`, `"...X......X....."`, `Seventh`, octave 3, `dub_deep` · `lead = "Cloche"`, `"R..R..R..R..R..R"` (3 contre 4), octave 5 (décoché) · `counter = "Cloche"` `"......x.......x."` · swing 0,15 · `Dark = Minor (0,0,0,0)`. Banc : −8,1 LUFS, côté/centre −14,7 (le meilleur), air **−25,4**, pente −6,71 (trop sombre), basse −34 dB (CORPS).

### Écarts
1. **Basse inaudible** (−34) : `Basse ronde` octave 2 + pompe (CORPS) ; sinon aucune mélodie ne tient.
2. **Rien ne se permute** : le style vit des filtres/délais sur la percu qui changent tous les 8–16 mesures. → générateur : automation **envoi délai du rim** 0 → 0,4 → 0 par 8 mesures, filtre passe-haut sur le shaker ouvert/fermé par 16 ; mute du rim une phrase sur quatre.
3. **Cloche 3-contre-4** : bon geste, mais 16 blips identiques. → `"R..R..R..R..5..R"` + cutoff modulé aléatoirement (LFO2 lent non synchronisé 0,1 sur le cutoff, `SWEEP`), decay court.
4. **Trop sombre** (air −25,4, pente −6,7) : charleys/shaker plus brillants et plus forts de 3 dB (étagère 8 kHz), cymbale inversée très basse avant chaque 8 mesures (bruit « Souffle » inversé = riser court de 1 mesure).
5. **Ostinato échantillonné** : hors portée ; le stab Pluck m7 avec `dub_deep` en tient lieu, à condition qu'il **n'arrive qu'après 16 mesures** et parte dans le Break (déjà : parts du Groove).
6. **Swing** : 0,15 ≈ 57 % ; Attack va jusqu'à 70 % → 0,2 sur la minimal « 2000 » (par ligne : charleys plus swingués que le kick, manque appli).
7. **Forme** : 2 min 45 ; cible intro 32 kick + charleys, sections de 32.

---

## 10. Trance (`trance`, 138 BPM)

### 10 morceaux repères
| # | Artiste – Titre (année) | BPM | Tonalité, accords (Hooktheory) | Mélodie (Hooktheory) |
|---|---|---|---|---|
| 1 | Tiësto – Adagio for Strings (2005) | 140–144 | Si♭ min ; iv–Vsus4–VI7–iv6–VI | La4–Mi♭5, **2,59 temps/note**, conjointe, « repeaty » |
| 2 | ATB – 9 PM (Till I Come) (1998) | 132 | La min ; i–iv–III (VI) | Fa4–Mi5, 0,76 temps/note, conjointe |
| 3 | Paul van Dyk – For An Angel (1994/98) | 138 | La min (i–v–VII) ou Sol maj (Tunebat) | Sol4–Do5 (5 demi-tons !), 0,80 temps/note |
| 4 | System F – Out of the Blue (1999) | 140 | Sol maj ; **I–vi7sus2–IV(add9)** | Sol4–Si5, **2,37 temps/note** sur 64 temps |
| 5 | Binary Finary – 1998 (1998) | 138 (remix PvD, Sol♭ min, Beatport) | n.d. | n.d. |
| 6 | Robert Miles – Children (1995) | 137–138 | Fa min ; **VI–iv–i** (Ré♭–Si♭m–Fam), Fm9 | Fa4–Sol♯5, 1,39 temps/note sur **128 temps** (32 mesures de mélodie) |
| 7 | Energy 52 – Café del Mar (1993) | 138 | Fa min ; **i–VI–VII–iv** (Fm–Ré♭–Mi♭–Si♭m) | La♯5–La♯6 (refrain), **0,25 temps/note** (arpège) |
| 8 | Darude – Sandstorm (1999) | 136 | Mi min ; **i–iv7–III–VII** (Em–Am7–G–D) | La4–Mi5, **0,29 temps/note** (le bégaiement en 16e) |
| 9 | Rank 1 – Airwave (1999) | 136 | Ré min ; i–v–VI–iv–VII | Do4–Do5, **3,76 temps/note** (notes longues) |
| 10 | Above & Beyond – Sun & Moon (2011) | 128 (Hooktheory, ≈) | Fa♯ min ; i–III–iv–i–III | La3–Fa♯4, 1,28 temps/note |

### Traits
- **Tempo** 132–144, la plupart 136–140.
- **Tonalité / accords** : mineur (La, Si♭, Fa, Mi, Ré, Fa♯) et un majeur (Sol) ; **3 à 5 accords** : i–VI–VII–iv, VI–iv–i, i–iv–III–VII, I–vi–IV ; couleurs **sus2 / add9 / m9** (Out of the Blue, Children) ; chaque accord 1 à 2 mesures.
- **Mélodie** : **conjointe** (« stepwise » partout), étendue **5 à 16 demi-tons** (≈ une octave), **deux familles** : (a) **lignes longues** 2,4–3,8 temps par note (Airwave, Out of the Blue, Adagio), (b) **arpèges/bégaiements** 0,25–0,3 temps par note (Café del Mar, Sandstorm) ; souvent les deux superposées (arpège Nord Rack 2 + lead « da-da-da-da-daa » dans Sandstorm). Phrases de **8 mesures**, mélodie sur 32 mesures (Children : 128 temps), « repeaty » : A A′ B A″.
- **Sons (SOS « Classic Tracks » Sandstorm)** : lead esquissé dans FastTracker puis passé dans un **Quadrafuzz** (distorsion multibande) et rééchantillonné dans un ASR-10 ; **cordes supersaw JP-8080** (les accords Korg sonnaient « mous ») ; arpège Nord Rack 2 ; basse **carré creux** JV-1080 ; kick 909 **+60 Hz / −240 Hz** doublé d'un sample « explosif » ; snare rolls dans les drops, boucle de batterie « chugging » filtrée dans la version longue, **kick + crash à grosse réverbe aux points clés** ; balayages de filtre et faders **à la main**, enregistrés d'une traite.
- **Supersaw** : JP-8000 = 7 scies désaccordées (fait établi ; la page Perfect Circuit n'a pas pu être lue) ; recettes : **7 voix détune ≈ 20 %** + 2e oscillateur **une octave en dessous, 3 voix, 7 %**, réverbe courte **500 ms à 30–40 %** (Faderpro) ; **3 × 9 voix** légèrement décalées (= 27), **couche de bruit blanc +4 octaves passe-haut**, passe-haut 12 dB, **decay rapide pour le transitoire**, compression, grande réverbe aux aigus amortis (Syntorial) ; 16 + 16 voix à 20 % + oscillateur mono de base, chorus, soft-clip, **automation du cutoff** (Vital) ; « au-delà de ≈ 50 % de détune, ça dissonne et on perd la puissance ».
- **Basse (Producer School)** : contretemps (pas 3 et 7), double contretemps (3-4, 7-8), **16e roulantes avec sidechain obligatoire**, **saut d'octave en croches avec glissé** ; le kick doit finir « dans les deux premiers pas » et la note de basse **s'arrêter avant le kick suivant**.
- **Structure** : 7–10 min, intros/sorties de 1–2 min (≈ 32–64 mesures ; source faible), breakdowns 8/16/32, montées 8/16/32 avec **pause de 4 temps avant le drop** (bonedo) ; gabarit : intro 16, break/montée 16–32, **drop 32**, break/montée 16–32, drop 32, sortie 16 ; **premier drop à la mesure 33–49** ; changement majeur toutes les 16, mineur toutes les 8 (Gearspace).
- **Sonie** : −8 à −6 LUFS en club (≈).

### Notre recette (extraits)
`lead = "Lead trance"`, `"O..5..3.O..5..3."`, octave 4 · `pad = "Supersaw"`, `Triad`, `"X-.X-.X.X-.X-.X."` octave 3, pompe + reverb 0,35 · `bassPattern = "..R...R...R...R."`, Basse ronde, octave 2, pompe · `counter = "Pluck"` `"..x...x...x...x."` · `Dark = Minor (0,5,2,6)` = i–VI–III–VII ✓, `Happy = Major (5,3,0,4)` = vi–IV–I–V ✓ · `drop = Ride + Perc2` · riser · `sweeps pad` Groove 0,35→0,8, Break 0,3→1,0 · OTT master. Banc : −8,3 LUFS, Drop −8,7, Drop − Groove −0,3, côté/centre −19,4, corrélation 0,98.

### Écarts
1. **La mélodie n'est pas une mélodie** : 3 blips d'un pas par mesure (`O..5..3.`), identiques 4 mesures, aucune note tenue, aucune phrase de 8. Les hits : lignes **conjointes** de 2–4 temps par note sur 8 mesures, puis variante. → lead sur **64 pas** (4 mesures, 4 accords) avec tenues et respiration, ex. `"5-------3---2---|R-----------..O-|7---5---3---2---|R-----------...."`, vélocités X/x, `leadOctave = 4` ; générateur : A′ sur la 2e passe (dernière mesure différente), **1 mesure de silence** avant le Drop (fin de montée = « pause de 4 temps »), lead **seul avec la nappe dans le Break** (déjà prévu : `breakWith` null → pad + lead ✓).
2. **Deuxième couche mélodique** : l'arpège 16e (Café del Mar, Sandstorm) manque. → `counter` = « Arpège » en `UpDown` à la double croche sur les accords (0,25 temps/note), octave 4, seulement au Drop (déjà le cas du contre-chant), **réverbe longue amortie** ; le contre-chant actuel (Pluck en croches) devient la 3e couche.
3. **Lead maigre** : « Lead trance » UNISON 0,5, mono. → 3 couches : supersaw 7–9 voix détune 0,3–0,45 (**Supersaw** existant) + même ligne **une octave en dessous** (3 voix, 7 %) + couche de bruit passe-haut ; réverbe 500 ms 30–40 % + grande réverbe amortie ; délai 1/8 pointé 25 % ; stéréo du synthé (moteur) ; cible côté/centre −10 à −12 dB (−19,4 aujourd'hui).
4. **Nappe** : le gate 3-3-2 est bon ; les accords manquent de **sus2/add9** (Out of the Blue) → `Voicing.Sus2`/`Ninth` sur les degrés III et VI, 1 accord par 2 mesures au Break (tenu, `"X---------------"` + sweep).
5. **Basse** : contretemps d'un pas : juste en forme, trop court. → note de **2 pas** (`"..R-..R-..R-..R-"`) qui s'arrête avant le kick ; au Drop, 16e roulantes `"rRrRrRrRrRrRrRrR"` avec pompe 0,6 ou saut d'octave `"R.O.R.O."` avec glissé (`bassGlide`), vélocités 100/60.
6. **Drop pas plus gros** (−0,3 LU) : Drop = ride + shaker + **arpège** + **kick + crash à grosse réverbe** sur la mesure 1 + bruit « chugging » (boucle filtrée) ; cible ≥ +2 LU ; montée : snare roll de 8 mesures (`rollHits` existe pour la forme Build : l'appliquer à la trance sur les 4 dernières mesures du Groove) puis **1 mesure vide**.
7. **Forme** : 3 min 28 avec intro 16 et break 8 ; cible intro 32, break **16–32** (nappe + lead seuls, filtre fermé → ouvert), montée 8, drop 32, premier drop mesure 49.
8. **Kick** : 909 +60 Hz / −240 Hz doublé d'un sample : étape 3 de `son.md`.

---

## Synthèse famille : les 10 plus gros écarts (par impact audible)

| # | Écart (nos morceaux → vrais morceaux) | Changement générateur / moteur | Vérification |
|---|---|---|---|
| 1 | **Accroche = 1 mesure de blips d'une double croche, répétée tout le morceau** (10/10 styles) → riffs de 1–2 notes qui respirent (Energy Flash, The Bells), riffs de 2 mesures en croches avec tenues et bends (hoover, gabber, hard), mélodies conjointes de 8 mesures à 2–4 temps/note (trance), 3-contre-4 ou 7 pas (minimal, techno) | Recettes : `leadPattern` sur **32–64 pas** avec `-` (tenues), `.` (silences), `/` (glissés), octave 3–4. Générateur : **variante A′** toutes les 8 mesures (clip de 8 mesures ou 2e passe modifiée), **1 temps à 1 mesure de silence** avant chaque Drop, longueur de motif ≠ 16 (polymétrie), 3 niveaux de vélocité (`X`/`x`/`o` pour les lignes comme pour la batterie) | Analyse des notes du lead généré : durée moyenne ≥ 1/8 (trance ≥ 1 temps), ≥ 1 silence d'un temps par 4 mesures, ≥ 2 contenus différents de 4 mesures par section, ≥ 3 vélocités distinctes |
| 2 | **Synthés d'une seule couche, en mono** (côté/centre −17 à −38 dB, corrélation 0,93–1,0 ; cible −9 à −15, 0,3–0,85) → 2–3 couches (octave en dessous, bruit passe-haut, copie désaccordée), unisson 7–27 voix à 20–45 % **réparti en stéréo**, chorus, réverbe courte 500 ms + longue amortie, délai ping-pong | Moteur : `Synth::render(L,R)` avec unisson réparti (`son.md` étape 4) ; générateur : **partie « doublure »** automatique (même ligne, octave −1, 3 voix) pour lead et hoover ; chorus et ping-pong par défaut sur le lead ; bruit (NOISE) dans Supersaw / Hoover | Banc : côté/centre du Drop ≥ −15 dB, corrélation 0,4–0,85, perte mono ≥ −1,5 LU, corrélation < 120 Hz ≥ 0,95 |
| 3 | **Aucun mouvement sur les sons tenus ou répétés** → filtre qui s'ouvre/ferme sur 8–16 mesures (303, hoover, basse Black Sea), résonance « robotique » (Energy Flash), cutoff aléatoire (Hood), réverbe montée sur la dernière note de phrase (stab) | Générateur : `sweeps` en **cycles de 8 mesures** à résonance 0,4–0,6 (pas une rampe par section), automation de **l'envoi réverbe/délai** du lead par phrase, LFO2 lent sur le cutoff des leads de techno/minimal, `AcidMove` sur résonance en plus du cutoff | Courbes d'automation présentes sur lead/acid/basse dans le Drop ; centroïde spectral de la tige lead variant ≥ ±15 % sur 16 mesures |
| 4 | **Le Drop n'est pas plus gros que le Groove** (−0,2 LU médian ; cible ≥ +2) → tout entre, crash + impact + sub-drop, grave qui s'ouvre, lead par défaut, pause de 4 temps avant | Générateur : lead **coché par défaut** aussi en techno, minimal, acid techno et acidcore (il n'arrive qu'au Drop ; trance, Detroit, hard, industrielle, gabber et rétro l'ont déjà), `drop` = ≥ 2 lignes + arpège/contre-chant dès le 1er Drop, **crash** sur la mesure 1 (voix kit cymbale), filtre de basse 0,5 → 1 sur les 4 dernières mesures du Groove, **1 mesure de silence** (sauf riser) avant le Drop, roulement de snare 4–8 mesures pour trance/hard | Banc : Drop − Groove ≥ +2 LU, Drop − Break ≥ +4 LU, air du Drop ≥ air du Groove + 2 dB |
| 5 | **Vélocités et articulations plates** sur batterie et basse (2 niveaux ; 4 mesures identiques) → charleys ≤ 70 % en cycle 100/70/85/60, ride un coup sur deux plus court/doux, charley ouvert n° 2 à 50 % et retardé, claps fantômes sur 2 mesures, fills **plus doux**, basse à longueurs alternées et notes qui s'arrêtent avant le kick | Recettes : lignes sur **32 pas** (2e mesure différente) ; générateur : cycle de vélocité automatique par rôle (hat, ride, shaker), `bassPattern` avec `-` et minuscules, humanisation ±2 ms (`Hit.offset`, STYLE-FIXES F12) | Écart-type des vélocités par ligne > 0,1 sur 4 mesures ; 2e mesure ≠ 1re sur ≥ 2 lignes ; notes de basse ≥ 2 longueurs |
| 6 | **Forme trop courte et symétrique** : 2 min 40 – 3 min 28, intro 16, break 8, Drop 16 à 132 BPM → 5 min 30 à 9 min, intro/sortie 32, sections 32–64, break 16–32, premier riff/drop vers la mesure 25–49, variation par mute/unmute toutes les 8 | Générateur : `MAX_SONG_SECONDS` par famille (Techno **330 s**), `sectionsOf` club = 32/32/32/16/32/32, mini-breaks de 2 mesures sans kick toutes les 16 dans le Drop (techno), Break = nappe + lead seuls avec filtre, **mute d'une ligne 8 mesures sur 16** dans le 2e Drop | Durée 4:30–5:30 ; LRA 3–6 LU (0,6–2,0 aujourd'hui) ; nombre de mesures « uniques » (contenu différent) ≥ 20 % du morceau |
| 7 | **Accords empilés sur la gamme** à l'octave 3, 1 accord par mesure → formes **parallèles m7/m9** (Detroit, dub), **sus2/add9** (trance), renversements serrés, 1 accord par 2 mesures dans les breaks | Générateur : `Voicing` à intervalles fixes transposés (STYLE-FIXES F11), inversion automatique pour minimiser le mouvement entre accords, `chordRhythm` 32 pas | Analyse : étendue d'un accord ≤ 12 demi-tons, ≥ 1 note commune entre accords voisins |
| 8 | **Registre et longueur des leads** : octave 5 et 1/16 → Fa♯1–Fa3 (stabs techno), Ré2–Fa3 (lead polymétrique), Sol4–Do5 (trance), decay ≈ 220 ms, notes de 1/8 à 1 temps | Recettes : `leadOctave` 3 (techno, industrielle, minimal), 4 (trance, hard, gabber, rétro) ; presets : decay « Lead filtré » 220 ms, « Lead carré » release 0,3 | Note médiane du lead entre Do3 et Do5 ; durée médiane ≥ 1/8 |
| 9 | **Sources de batterie et kick** (hors mélodie, mais c'est la signature « démo ») : kick 909 court à enveloppe de hauteur 10–20 ms, accordé sur la tonique (hard, gabber), rumble en chaîne (réverbe 3–5 s → distorsion → HP 30 Hz + coupe > 150–200 Hz → pompe → mono), +60 Hz/−240 Hz (trance), kick doublé d'un sample d'impact | Moteur/kit : `son.md` étape 3 (kits échantillonnés, kick refait, couches) ; preset `rumble` : ajouter le passe-haut 30 Hz et le plateau 150–200 Hz ; `F15` kick accordé | Banc : PLR 7–9 (hard/gabber 5–6 acceptés), sonie hard techno −6 à −5 sans plafonnement (perte chaîne < 6 LU), sub −4 à −6 dB |
| 10 | **Espace par rôle** : réverbe/délai « un seul réglage pour tous » → clap réverbe 850 ms / prédélai 17 ms / 17 %, conga délai 1/8 pointé 70 % fb 20 %, riff réverbe « serrée et métallique » + délai panné d'un côté, hoover slap delay, arpège grande réverbe amortie, risers filtre résonant dans la réverbe, crash 909 au début des sections | Générateur (`WizardMix.withPartMix` / `withDrumMix`) : envois par rôle (clap 0,2 réverbe, perc 0,2 délai pointé, lead délai ping-pong 0,25, arpège réverbe 0,35), 2 réverbes (courte 0,3–0,5 s, longue 2–4 s amortie), crash au début de chaque section | Banc : énergie de queue de réverbe dans le Break > Drop ; air du Drop −16 à −20 ; aucune tige > −1,5 LU de perte en mono |

Les trois premiers suffisent à changer l'impression « mélodies synthé amateur » ; les points 1, 4, 5, 6 et 7 sont des changements de recette et de générateur Kotlin sans toucher au fil audio ; 2 et 9 passent par le moteur (`son.md` étapes 3 et 4).

## Sources
- Attack Magazine : Deconstructed Energy Flash https://attackmagazine.com/?p=79387 · The Bells https://www.attackmagazine.com/technique/deconstructed/jeff-mills-the-bells/ · No UFO's https://www.attackmagazine.com/technique/deconstructed/model-500-no-ufos/ · Drexciya Black Sea https://www.attackmagazine.com/technique/deconstructed/drexciya-black-sea/ · Strings of Life https://www.attackmagazine.com/technique/deconstructed/rhythim-is-rhythim-strings-of-life-derrick-may/ · Beat Dissected rolling techno https://www.attackmagazine.com/technique/beat-dissected/rolling-techno/ · dark techno rumble https://attackmagazine.com/?p=71776 · chugging analogue techno https://www.attackmagazine.com/technique/beat-dissected/chugging-analogue-techno/ · Sub-Zero minimal https://attackmagazine.com/?p=7929 · Final Frontier https://www.attackmagazine.com/technique/beat-dissected/underground-resistance-the-final-frontier/ · ethereal techno https://www.attackmagazine.com/technique/beat-dissected/ethereal-techno/ · techno synth stabs https://www.attackmagazine.com/technique/synth-secrets/techno-synth-stabs/ · portamento lead (Diva) https://www.attackmagazine.com/technique/synth-secrets/analogue-portamento-lead-with-u-he-diva/ · guide TR-909 https://www.attackmagazine.com/technique/tutorials/the-beginners-guide-to-the-tr-909/
- Sound On Sound, Classic Tracks : Darude « Sandstorm » https://www.soundonsound.com/node/4922441
- Hooktheory : Sandstorm, Adagio for Strings, Sun and Moon, Café del Mar (DJ Kid Paul Mix), For An Angel, Children, Good Life, Airwave, Out of the Blue, 9 PM (Till I Come) — https://www.hooktheory.com/theorytab/
- Beatportal : guide peak time techno https://www.beatportal.com/articles/783088-… · top hard techno 2024 https://www.beatportal.com/articles/780594-hard-techno-the-top-selling-tracks-artists-and-labels-of-2024
- Futureproof Music School, hard techno https://futureproofmusicschool.com/blog/making-hard-techno-a-path-to-unique-sound-design · The Producer School : bass patterns hard house/trance https://theproducerschool.com/blogs/featured-blogs/5-essential-bass-patterns-that-define-hard-house-and-trance-music · hard techno guide https://theproducerschool.com/blogs/featured-blogs/how-to-create-hard-techno-a-step-by-step-guide
- Production Music Live, lead polymétrique https://productionmusiclive.com/blogs/news/hypnotic-polymetric-lead-in-analog-melodic-techno-ableton-tutorial
- Supersaw : Faderpro https://blog.faderpro.com/?p=2049 · Syntorial https://www.syntorial.com/tag/trance-lead/ · SoundBridge https://soundbridge.io/en/design-your-super-saw-lead-sound
- Red Bull Music Academy : Mentasm https://daily.redbullmusicacademy.com/2014/05/key-tracks-mundo-muzique-on-mentasm · London acid techno https://daily.redbullmusicacademy.com/2016/06/london-acid-techno-feature · Roland, Energy Flash https://articles.roland.com/?p=7356
- Wikipedia : Hoover sound, Gabber, Hardcore techno, Free tekno, Industrial techno, Acid techno, Spiral Tribe, Network 23, Sandstorm, Children, Adagio for Strings (Tiësto), Spastik, Strings of Life, I Wanna Be a Hippy, Fizheuer Zieheuer
- Electronic Beats (Paula Temple, 10 industrial tracks) https://www.electronicbeats.net/10-industrial-tracks-paula-temple · Four Four (10 quintessential Perc tracks) https://fourfourmag.com/10-quintessential-perc-tracks/ · Mixmag schranz https://mixmag.net/feature/10-essential-schranz-tracks-selected-by-nikolina
- Structure : forum Ableton https://forum.ableton.com/viewtopic.php?p=1580518 · Elephorm cheatsheet https://www.elephorm.com/… · Gearspace « house music structure » · Mixed In Key ?p=5367
- Sonie : Sweetwater (club ≈ −6 LUFS) https://www.sweetwater.com/insync/loudness-standards-lu/ · Mastering The Mix (mediums) · Futureproof (−6 à −5 LUFS, mono < 150 Hz)
- BPM/tonalités : mixgraph.io (Hypnotized, Your Mind, Subzero, Doppler, Astral Projection, Positive Education, Take Your Body Off, Higher State, Acperience 1, Children, Fizheuer Zieheuer), songbpm.com (Doppler, Daydream, Poing, Euromasters, I Wanna Be A Hippy, Anasthasia, Mentasm, Dominator, Energy Flash), Beatport (1998, Airwave, Out of the Blue), Tunebat (For An Angel)
