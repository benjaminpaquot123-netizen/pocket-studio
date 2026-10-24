# Testeur Pro 2, vague 2 (après la phase 33) : ingé mix/mastering, Tech house « prêt à sortir »

Base : `/home/user/ps-p22`, branche `ccr-80af1a1b-mn0wec` (58a3f28). Lecture seule, rien modifié, gradle non lancé.
Sources de mesure : `docs/son/banc/apres-etape2.{md,json}` (37 styles), `apres-etape1.json` (le même morceau avant la chaîne master : il donne la sonie de chaque section **avant** le master), et les extraits A/B `scratchpad/ab33/*.wav` que j'ai mesurés moi-même (script `testeurs2/ab_analyse.py` : LUFS-I, crête, facteur de crête, côté/centre, bandes, sonie court terme min/max sur les 30 s).
Je donne d'abord mon avis formé sur le code et les chiffres, puis la comparaison avec mon rapport de la vague 1 (`docs/audit-33/pro2.md`).

## Parcours réel

1. « Partir d'un style › Tech house » (`StyleRecipes.kt:269-300`) : kit intégré v2, basse ronde + piste Sub, orgue, voix robot, souffle montant, 126 BPM. La recette pose « Pompe » sur la basse (préréglage `pump`, profondeur 0,8), « Punch » sur le pad du kick, un peu de réverbe sur le clap, un écho sur le lead ; `WizardMix.withGlue` ajoute la colle du bus batterie et du master, `masterDb = −1`, et le Sub reçoit sa propre Pompe (`StarterWizard.kt:787`).
2. Mixeur : la carte « Sortie générale » (capture `mixer_master.png`) montre enfin **−8,1 LUFS · TP −1,2**, « Maintenant / sur 3 s / depuis ▶ », « Le limiteur protège la sortie (−1,9 dB) », « Volume visé » Auto (style) · Club · Streaming · Désactivée, et « Gain ajouté pour la cible : +3,2 dB ». La boîte du compresseur (capture `compressor_editor.png`) a une barre de réduction (−6,5 dB sur le bus batterie), Gain, Gain auto, Détection, Filtre des graves, Mélange. C'est le mixeur que je réclamais.
3. Je lis ensuite ce que le banc dit de ce morceau (ligne `tech_house` du rapport) : LUFS-I −8,2 pour une cible −8, TP −1,16, PLR 7,1, mais **gain ajouté +7,5 dB pour 3,4 LU retirées** par l'écrêteur et le limiteur, facteur de crête du Drop 7,35 dB (11,4 avant), **LRA 1,0 LU**, **côté/centre −31,4 dB, corrélation 1,00**, air −20,2, pente −5,22. Sections après master : Intro −8,03, Groove −8,27, Drop −8,40, Break −8,53 : l'Intro est la section la plus forte du morceau, le Drop la troisième.
4. Export : WAV 24 bits, FLAC, 44,1 kHz, 16 bits, « Sans limiteur (pour le mastering) », M4A gardé sous −2 dBTP (`AacEncoder.kt:25`), rapport « Volume perçu −8,4 LUFS · Crête vraie −1,2 dBTP · durée ». Pour finir ailleurs, c'est propre. Pour sortir tel quel : non, voir plus bas.
5. Les A/B (`ab33`) confirment à l'oreille-par-les-chiffres : techno avant → après, −10,9 → −8,2 LUFS-I, facteur de crête 10,4 → 7,3 dB, plage de sonie court terme sur les 30 s du Drop **0,6 LU** (c'était 1,0) ; house : côté/centre −28 dB, corrélation 1,00 (mono) ; énergie au-dessus de 12 kHz −25 à −31 dB rel. ; 0,3 à 0,7 % des échantillons à moins de 1,5 dB de la pleine échelle après (0,01 à 0,07 % avant) : le limiteur travaille sur chaque coup.

Verdict court : la phase 33 a construit une vraie chaîne de mastering (honnête, true peak, sans continu, déterministe, mesurée) **devant un mix qui n'a pas changé**. Le volume de club est atteint (29 styles sur 37), mais il est **acheté par le limiteur, pas gagné par le mix** : +8,6 dB de gain médian pour 4,7 LU de perte, 55 % du gain ajouté est retiré aussitôt. Et le ciblage sur la sonie intégrée aplatit, voire inverse, la courbe d'énergie du morceau.

## Points durs

### 1. La cible de sonie vise l'intégré du morceau entier avec un seul gain : elle écrase le Drop et remonte le Break (grave, c'est LE problème de la chaîne)
- Où : `engine/src/main/cpp/core/OfflineRenderer.cpp:338-366` (`target − music.integratedLufs()`, deux essais puis sécante, perte permise jusqu'à `MasterChain::kMaxLossDb = 6` dB, `MasterChain.h:50`) ; en direct `MasterChain.cpp:387-411` (`estimate()`, même logique sur une estimation intégrée à mémoire de 60 s).
- Pourquoi : un gain statique + un limiteur, c'est une compression de la **macro-dynamique**. Les sections pleines (Drop) perdent 4 à 6 LU dans l'écrêteur et le limiteur ; les sections creuses (Break, Intro, Montée) n'y entrent pas et prennent le gain entier. Preuves, sonie par section avant (`apres-etape1.json`) → après (`apres-etape2.json`) :
  - dnb : Drop −12,85 / Break −16,92 (+4,1 LU) → Drop −8,88 / **Break −7,84** (−1,0 LU : le Break est plus fort que le Drop) ; Montée −6,70, la plus forte du morceau.
  - jump_up : +4,9 LU → −0,15 LU. riddim : Drop −17,15 / Montée −13,33 → Drop −9,65 / **Montée −6,58** (3 LU au-dessus du Drop). neurofunk : +7,2 → +2,1. amapiano : +6,3 → +3,1.
  - tech_house : Intro −14,13 → −8,03, Drop −12,07 → −8,40 : l'Intro gagne 6,1 dB, le Drop 3,7 ; l'Intro finit 0,4 LU **au-dessus** du Drop.
  - Médiane 37 styles : Drop − Break +1,4 → −0,2 LU ; LRA 4,0 → 1,7 LU (cible 3 à 9) ; facteur de crête du Drop 11,4 → 7,7 dB.
- Piste : viser **la sonie court terme de la section la plus forte** (le Drop : moyenne des fenêtres de 3 s du décile le plus fort, comme Matchering qui travaille sur les « loudest pieces », ou Ozone qui parle de short-term sur la section la plus forte), pas l'intégré ; perte permise ≤ 3 LU sur cette section ; l'intégré devient un résultat affiché (« Drop −7, Break −12, morceau −8,3 »). Si la cible n'est pas atteignable à ≤ 3 LU de perte, dire « le mix n'est pas assez dense » plutôt que pousser. Tenir au banc la règle de l'étape 6 (Drop − Break ≥ +4) **après** master, pas seulement avant.
- Pros : un ingé règle le limiteur sur le Drop (2 à 4 dB de réduction sur les coups les plus forts) et laisse le Break là où le mix le met ; Pro-L 2 et Ozone Maximizer affichent la réduction en continu et ne « normalisent » jamais un morceau entier à un seul gain sans le dire ; LANDR et Matchering calent le niveau sur les passages les plus forts.

### 2. La sonie vient du limiteur parce que le kick n'a pas changé : un sinus à 47 Hz qui traîne 1,5 s (grave, le point n° 1 à l'oreille)
- Où : `DrumKit.cpp:90` (`kick_.configure(…, 47.0f, 3.2f, 0.035f, 0.42f, 0.35f)`), `DrumKit.cpp:39` (`kMaxSeconds` : kick 1,5 s), `DrumKit.cpp:75-79` (clic = carré naïf, non limité en bande). Le kit v2 (`kLevelsV2`, `applyVersion`) ne change que les niveaux et les charleys.
- Pourquoi : à 126 BPM un temps dure 476 ms ; avec une constante de 0,42 s le kick précédent est encore à −10 dB quand le suivant tombe, par-dessus le Sub. Un sinus de 47 Hz a un facteur de crête énorme pour sa sonie, et la pondération K le compte 3 à 4 dB en dessous (passe-haut RLB à 38 Hz) : il **prend tout le headroom et ne donne presque pas de LUFS**. C'est exactement ce que dit le banc : tech_house, retirer le kick et le clap dans le Break ne coûte que 0,25 LU avant master (−12,07 → −12,32) ; les 8 styles bass music plafonnent à 6 LU de perte en restant 1,1 à 2,2 LU sous leur cible. Sur un téléphone, 47 Hz est inaudible : le morceau sonne « mou » et « petit » en même temps.
- Piste : l'étape 3 du plan, en priorité absolue : kick court (300 à 450 ms, enveloppe de hauteur 150 → 50 Hz en deux temps), couche d'attaque 2 à 5 kHz, écrêtage doux dans la source ; kits échantillonnés CC0 (Sonic Pi, Freesound) ; couches sub + attaque. Critère : le même −8 LUFS avec 4 à 6 dB de gain en moins et un facteur de crête ≥ 8 dB sur le Drop.
- Pros : un kick de tech house moderne, c'est 60 à 120 Hz de corps, un clic, 250 à 400 ms, souvent deux couches ; le sub est tenu par la basse, pas par la queue du kick.

### 3. Trois compresseurs à attaque rapide en série avant le limiteur : le transitoire est mangé avant d'arriver à la chaîne master (moyen-grave)
- Où : `FxPresets.kt:87-88` (préréglage « Punch » posé sur le pad du kick par la recette, `StyleRecipes.kt:296`) : seuil −22 dB, ratio 3,85:1, **attaque 2 ms**, relâche 60 ms, gain auto, puis Saturation avec **passe-bas à 10,9 kHz** (tonalité 0,8) ; `WizardMix.kt:158` `BUS_GLUE` : seuil −16 dB, 2,1:1, **attaque 2,0 ms**, relâche 60 ms, crêtes, filtre 89 Hz, la capture montre **−6,5 dB** de réduction ; `WizardMix.kt:164` `MASTER_GLUE` : seuil −16 dB, 1,8:1, attaque 10,8 ms, relâche 105 ms, détection crêtes. (Valeurs par `expMap`, `Effects.cpp:16`, et `Compressor::update`, `Effects.cpp:157-177`.)
- Pourquoi : à 2 ms d'attaque un compresseur attrape le clic du kick et de la caisse, pas sa queue : c'est l'anti-punch. Un « glue » qui réduit 6,5 dB à 2 ms sur un bus batterie, puis un master à −16 dB de seuil en détection crête qui pompe de 4 à 6 dB sur chaque coup (relâche 105 ms, audible à 126 BPM), puis un écrêteur, puis un limiteur : quatre étages qui rabotent le même transitoire. Le compresseur est honnête maintenant (0 dB sous le seuil, bravo), mais ses réglages par défaut travaillent contre le mix.
- Piste : « Punch » = attaque 20 à 30 ms, relâche 40 à 80 ms, ratio 4:1, 3 à 5 dB de réduction, ou un vrai transient shaper (attaque +, sustain −) ; bus batterie : RMS, attaque 10 à 30 ms, relâche auto ou 100 ms, 1 à 3 dB ; master : 1,5 à 2:1, attaque ≥ 30 ms, RMS, ≤ 2 dB. Faire un test hôte qui mesure la réduction de chaque colle sur le Drop d'un morceau de l'assistant (≤ 3 dB bus, ≤ 2 dB master).
- Pros : SSL G bus comp à 30 ms / auto / 2:1 et 2 dB ; Glue d'Ableton ; sur un kick on **ralentit** l'attaque pour le faire claquer.

### 4. La « Pompe » n'est toujours pas un sidechain, et la basse pompe deux fois (moyen, inchangé)
- Où : `Effects.cpp:501-520` (`Ducker::process` : enveloppe fixe, attaque 2 ms figée) ; `Engine.cpp:667` (`pattern_[0][patternStep] > 0` : seule la voie kick du pad 1 déclenche) ; basse : préréglage `pump` profondeur 0,8 (`FxPresets.kt:85`) ; Sub : `Insert.of(PUMP)` profondeur 0,7 (`StarterWizard.kt:787`) ; `WizardMix.kt:201` ajoute 0,45 si aucune.
- Pourquoi : kick d'un pack sur un autre pad, kick d'une piste, kick remixé : rien ne pompe ; basse et Sub pompent chacun avec leur propre enveloppe sans coordination ; rien sur les retours ni sur un bus Musique. Le banc n'a pas encore de mesure stems (énergie < 100 Hz de la basse dans les 50 ms après le kick) : impossible de prouver le creux.
- Piste : étape 5 du plan (clé audio au choix, courbe, bus « Musique », pompe sur les retours). Ajouter la mesure stems au banc dès maintenant.
- Pros : Kickstart, Trackspacer, sidechain des compresseurs de tout DAW.

### 5. Toutes les pistes d'instruments en mono : côté/centre −24,8 dB médian, corrélation 0,99 (moyen-grave, inchangé)
- Où : `Synth.h:57` (`render(float* mono…)`, l'unisson à 7 voix sommé), `SampleInstrument.cpp:284` (stéréo repliée), `SoundFontPlayer.cpp:24` (`TSF_MONO`), `PlaitsSynth.cpp:206`, 303 mono ; seul l'Élargisseur des nappes tenues (`WizardMix.withPartMix`) et les pans de batterie ouvrent l'image.
- Preuves : banc, Drop : côté/centre −14,7 à −38,4 dB (cible −15 à −9), corrélation 0,93 à 1,00 (cible 0,3 à 0,85) ; A/B house −28 dB / 1,00, tech_house −31,4 dB. Perte en mono −0,03 LU : compatible mono, forcément.
- Piste : étape 4 (unisson réparti, échantillons stéréo gardés, `TSF_STEREO_INTERLEAVED`, Ensemble de Plaits, bas mono < 120 Hz sur le master).
- Pros : une supersaw ou une nappe s'ouvre à −10/−12 dB de côté/centre ; le bas reste au centre.

### 6. L'air manque toujours : air −20,7 dB médian, pente −5,0 dB/oct, presque rien au-dessus de 12 kHz (moyen)
- Où : charleys DaisySP `HiHat` (`DrumKit.cpp:118-134`, tonalité 0,9 en v2 (`applyVersion`, l.176)), aucun échantillon ; `FxPresets.kt:88` (Saturation « Punch » avec passe-bas 10,9 kHz sur le kick) ; réverbe Freeverb sans EQ de retour.
- Preuves : banc air −26,6 à −15,8 (cible −20 à −16), pente −6,71 à −3,30 (cible −4,5 ±0,7) ; A/B > 12 kHz −25 à −31 dB rel. Le limiteur enlève encore 1 dB d'air (il travaille sur les transitoires, qui sont aigus).
- Piste : vrais charleys et cymbales échantillonnés (étape 3), étagère d'air sur le master dans la courbe du genre (étape 8), retirer le passe-bas des préréglages « Punch »/« Crunch ».

### 7. Réverbe et espace inchangés : Freeverb, entrée mono, pas d'EQ de retour, pré-délai hors tempo (moyen)
- Où : `Effects.cpp:1160` (`(inL + inR) * kFixedGain` : la stéréo d'entrée est perdue), `Effects.cpp:1130-1138` (aucun filtre), `Buses.DEFAULTS` (`Effects.kt:444` : taille 0,6, amorti 0,5, pré-délai 20 ms fixes).
- Pourquoi : la réverbe envoie du grave et du bas-médium dans le mix (brouille 100 à 300 Hz : bas-médium du Drop −13,2 dB médian contre −10 visé), et sa queue métallique est reconnaissable. Pas de plate, pas de room.
- Piste : étape 4 (passe-haut 250 Hz, passe-bas 9 kHz sur le retour, pré-délai 1/64–1/32, plate Mutable, pompe sur le retour).
- Pros : Valhalla Room/Plate, retour toujours filtré, pré-délai calé.

### 8. Le gain adaptatif en direct : ce que j'entends en mixant n'est pas ce que j'exporte (moyen)
- Où : `MasterChain.cpp:387-411` (`estimate()` : le drive suit l'estimation intégrée à 1 dB/s puis 0,5 dB/s, mémoire 60 s) ; l'export, lui, tient un drive fixe en deux passes (`OfflineRenderer.cpp`).
- Pourquoi : c'est un AGC lent sur le master. Pendant un Break de 16 mesures (30 s) l'estimation baisse et le drive monte ; au Drop suivant la chaîne écrase quelques secondes de trop, puis redescend. Le panneau affiche « Gain ajouté +3,2 dB » qui bouge pendant qu'on règle un fader : un pro ne peut pas juger un mix avec un gain qui dérive. Pendant les 3 à 7 premières secondes, rien n'est affiché (relecture m13).
- Piste : en direct, un drive **figé** : calculé une fois sur 8 mesures du Drop (rendu hors ligne rapide) ou repris du dernier export, bouton « Recalculer » ; ou au minimum l'afficher comme « en cours d'estimation ». Garder l'AGC seulement pour la boucle libre du Live.
- Pros : un limiteur de mastering a un gain d'entrée fixe ; l'« auto gain » n'existe que dans les assistants hors ligne (Ozone Master Assistant écoute, puis fige).

### 9. Dynamique de jeu : vélocités X/x/o = 1 / 0,6 / 0,35 et loi du kit qui les écrase (moyen)
- Où : `StarterWizard.kt:487-489` ; `DrumKit.cpp:321` (`gain = level * (0.4f + 0.6f * velocity)` : un « o » à 0,35 ne joue que **−4,3 dB** sous un « X », un « x » −2,4 dB) ; aucun micro-timing dans `Hit` (`Project.kt:323`, vélocité et roulements seulement) ; `NoteEdits.humanize` existe pour l'utilisateur, l'assistant ne s'en sert pas.
- Pourquoi : les notes fantômes de charley et de shaker sonnent presque aussi fort que les accents : groove mécanique, et densité uniforme qui mange du headroom sans relief. Le banc le mesure indirectement : plage court terme de 0,6 LU sur 30 s.
- Piste : loi en dB (v² ou −12 dB à 0,35) sur le kit intégré, graine fixe ±5–8 % sur charleys/percus, ±2–5 ms de micro-timing, accents sur le temps (étape 6).

### 10. Export et métrologie : presque pro, deux manques (faible)
- Le rapport d'export donne LUFS-I, TP, durée (`MasterLoudness.kt:ExportReportLine`) mais ni LRA ni PLR ni « Drop / Break », les seuls chiffres qui auraient dénoncé le point 1. Pas de métadonnées (titre, BPM, tonalité), pas de normalisation à une cible sans passer par le master.
- Le plafond M4A −2 dBTP et le `headroomGain` de secours (`AacEncoder.kt:25-29`) sont la bonne pratique ; 44,1 kHz / 16 bits dither TPDF : bien.
- Loi de pan toujours +3 dB sur les côtés (`Pan.h:11-12`), pas de corrélation ni d'écoute mono dans le mixeur (l'analyseur de spectre du master existe, `MixerScreen.kt:226`).

## Ce qui sépare encore nos morceaux d'une vraie production (par impact audible)

| # | Écart | Preuve mesurée (banc après étape 2, 37 styles) | Vraie production |
|---|---|---|---|
| 1 | **Batterie synthétique, kick sinus 47 Hz qui traîne** : pas de corps 60–120 Hz, pas de clic, pas de couches, pas de round-robin | Gain médian +8,6 dB pour 4,7 LU de perte ; 8 styles plafonnés (perte ≥ 6 LU) à 1,1–2,2 LU sous la cible ; facteur de crête du Drop 7,7 dB ; retirer le kick coûte 0,25 LU | Kick 250–400 ms, 2 couches ; 1 à 3 dB de limiteur suffisent ; crête 8–9 dB sur le Drop |
| 2 | **Courbe d'énergie plate ou inversée** : le master la finit d'aplatir | LRA 1,7 LU (cible 3–9) ; Drop − Break −0,2 LU médian, Break > Drop en dnb, jump_up, Montée > Drop sur 6 styles bass music ; plage court terme 0,6 LU sur 30 s de Drop (A/B techno) | LRA 4–7 ; Drop +4 à +8 LU au-dessus du Break ; la Montée reste sous le Drop |
| 3 | **Tout au centre** | Côté/centre −24,8 dB médian (−15 à −9 visé), corrélation 0,99 (0,3–0,85) | Nappes, leads et FX larges, bas mono |
| 4 | **Son fermé, pas d'air** | Air −20,7 (−20 à −16), pente −5,0 (−4,5), > 12 kHz −25 à −31 dB rel. (A/B) | Charleys et cymbales réels, étagère d'air, réverbe brillante |
| 5 | **Transitoires rabotés quatre fois** (Punch 2 ms → bus 2 ms, −6,5 dB → master 10 ms → écrêteur → limiteur) | Capture `compressor_editor.png` (−6,5 dB) ; valeurs de `BUS_GLUE`/`MASTER_GLUE`/`punch` | Colle 1–3 dB à 30 ms, master ≤ 2 dB, transient shaper sur le kick |
| 6 | **Kick et basse qui se marchent dessus** : pompe par motif, double, pas de bus Musique | Pas encore mesuré (pas de stems au banc) ; sub-basse laisse −0,01 de continu avant master | Sidechain audio 4–8 dB sur 10 ms, une seule pompe |
| 7 | **Bas-médium brouillé** : réverbe non filtrée, pas de passe-haut par rôle | Bas-médium 250–500 Hz −13,2 dB médian (−10 visé… mais jusqu'à −8,1 en dubstep/jump_up), grave 60–250 −5,9 | Passe-haut nappes 150 Hz, lead 200 Hz, retour réverbe 250 Hz |
| 8 | **Jeu mécanique** : 3 vélocités, loi écrasée, pas de micro-timing, 1 fill par 4 mesures | Plage court terme 0,6 LU ; `DrumKit.cpp:321` | Vélocités et timing humains, fills de 8/16, crash, reverse, downlifter |
| 9 | Synthés PolyBLEP sans suréchantillonnage, GM mono | Repliement −37 à −53 dB en 7e octave (plan § 1.1 C) | Leads propres jusqu'à 20 kHz |
| 10 | PLR < 6 sur acidcore et gabber ; 303 saturée par `tanh` à 1× (`AcidTrack.cpp:163`) | PLR 5,6–5,9 | Cible PLR ≥ 6, saturation suréchantillonnée |

## Fonctions manquantes (classées)

1. **Cible sur la section la plus forte** (short-term du Drop), perte plafonnée à 3 LU, rapport « Drop / Break / morceau » après export et au banc.
2. **Kits échantillonnés par genre et kick refait** (étape 3) avec couches et round-robin.
3. **Vrai sidechain** (clé audio au choix, courbe, bus « Musique », pompe sur les retours) et mesure stems au banc.
4. **Instruments stéréo** + réverbes filtrées, pré-délai au tempo, plate (étape 4).
5. **Drive figé en direct** (ou calculé sur le Drop), affichage « estimation en cours ».
6. **Préréglages et colle recalés** : « Punch » à attaque lente, bus 1–3 dB, master ≤ 2 dB ; test hôte de réduction sur un morceau de l'assistant.
7. **Production** : vélocités/micro-timing humains, loi de vélocité en dB du kit, fills 8/16, crash et impact au Drop, Montée sous le Drop, Break sans kick ni basse pour les styles club (étape 6).
8. **« Équilibrer mon mix » et « Masteriser »** avec conseils en mots simples (étape 8) ; **référence et A/B calé en sonie** ; bouton « Écouter en mono » et corrélation sur le master.
9. **EQ multibande graphique** avec passe-haut/bas et M/S ; passe-haut par rôle posé par l'assistant.
10. Export : LRA/PLR dans le rapport, métadonnées, loi de pan −3 dB.

## Peut-on avoir un bon mix sans être expert ?

Mieux qu'avant : « Volume visé » en quatre puces est la bonne abstraction, la puce LUFS/TP et la barre de réduction du compresseur parlent, « Prêt à partager ? » attrape le solo oublié et la saturation. Mais le non-expert n'a toujours **aucun bouton** pour remettre un mix d'équerre après avoir changé un son, aucun conseil (« ta basse cache ton kick »), aucune référence ; et le préréglage « Punch » qu'on lui propose en premier pour la batterie (`FxPresets.suggestedFor DRUMS`) fait l'inverse de son nom. La cible Auto lui donne un volume de club immédiat, ce qui est trompeur : plus fort sonne mieux, et il ne verra pas que son Break est devenu aussi fort que son Drop.

## Comparé à la vague 1 (mon Top 5 du 2026-10-23, avant la phase 33)

| Vague 1 | État | Détail |
|---|---|---|
| 1. Aucune mesure de sonie en direct (ni LUFS ni true peak) | **Réglé** | LUFS M/S/I + TP en direct (`MasterLoudness.kt`, `Loudness.h`), puce ambre sur dépassement, rapport d'export LUFS + TP + durée ; banc et ffmpeg concordent à 0,05. Manquent LRA/PLR/section, corrélation, écoute mono. |
| 2. Compresseur à gain fixe caché, sans indicateur | **Réglé** (mais réglages par défaut à revoir) | 0 dB sous le seuil, Gain, Gain auto, RMS, filtre des graves, Mélange, réduction affichée ; migration des anciens projets. Le bus batterie montre −6,5 dB à 2 ms d'attaque : honnête, mais trop et trop vite (point 3). |
| 3. La « Pompe » n'est pas un sidechain, double ducking | **Pareil** | `Ducker` et `Engine.cpp:667` inchangés ; basse (0,8) + Sub (0,7) toujours chacun leur pompe. |
| 4. Master figé : limiteur crête échantillon, rien de réglable, pas de préréglage | **Mieux** (chaîne : réglé ; résultat : à moitié) | Écrêteur ClipOnly2 ×4 + limiteur true peak avec maintien sur le grave, −1 dBTP tenu sur 37/37, bloqueurs de continu, cibles Auto/Club/Streaming/Off, M4A sous −2 dBTP. Mais la cible intégrée écrase le Drop et remonte le Break (nouveau point 1), aucune EQ de master, plafond et relâche non exposés. |
| 5. Pas d'auto mix, pas de référence/A-B | **Pareil** | Rien ; « Prêt à partager ? » vérifie, mais ne corrige pas un mix. |

**Nouveau depuis la vague 1 :**
- Le banc (`docs/son/BANC.md`) : 37 morceaux rendus et mesurés, déterministes, CI bloquante sur TP, continu, NaN, cible. C'est l'outil qui manquait ; il m'a permis ce rapport. Il lui manque les stems (kick/basse), les packs (les kicks IA ne sont pas mesurés) et les courbes de référence par genre.
- Le ciblage intégré + perte 6 LU inverse la courbe d'énergie (Break ou Montée plus forts que le Drop sur 6 styles) : c'est la régression de la phase, mesurable, et elle se corrige dans `OfflineRenderer.cpp:340` sans toucher au DSP.
- Le kit v2 n'a changé que les niveaux : le diagnostic « sinus 47 Hz » du plan reste entier, et la phase le prouve en chiffres (+8,6 dB pour 4,7 LU).
- Le gain adaptatif en direct (AGC) : nouveau comportement, différent de l'export.
- La Montée (souffle blanc à 0,9 de vélocité, `StarterWizard.kt:701-733`) finit au-dessus du Drop après master sur les styles bass music.
- Saturation et Distorsion à ×4 avec antidérivée (`DriveShaper.h`) : propre (repliement ≤ −70 dB au banc), le compresseur et la 303 sans continu : du vrai travail d'ingénierie.

## Top 5 (vague 2)

1. **La sonie est achetée, pas gagnée** : +8,6 dB de gain pour 4,7 LU retirées, 8 styles plafonnés, parce que le kick est toujours un sinus de 47 Hz de 1,5 s (`DrumKit.cpp:90`) qui prend le headroom sans donner de LUFS. Étape 3 avant tout.
2. **La cible intégrée écrase le Drop et remonte le Break** (`OfflineRenderer.cpp:340`, `kMaxLossDb = 6`) : LRA 1,7 LU, Drop − Break −0,2 LU, Break > Drop en dnb/jump_up, Montée > Drop sur 6 styles. Viser le court terme du Drop, perte ≤ 3 LU.
3. **Quatre étages rabotent le même transitoire** : « Punch » à 2 ms sur le kick, bus batterie à 2 ms et −6,5 dB, master à 10 ms et détection crête, puis écrêteur et limiteur (`FxPresets.kt:87`, `WizardMix.kt:158-170`). Attaques lentes, 1 à 3 dB.
4. **Mono et fermé** : côté/centre −24,8 dB, corrélation 0,99, air −20,7, rien au-dessus de 12 kHz (`Synth.h:57`, `SoundFontPlayer.cpp:24`, Freeverb `Effects.cpp:1160`). Étape 4 et charleys réels.
5. **Pompe par motif, double, et gain adaptatif en direct** : `Engine.cpp:667`, basse 0,8 + Sub 0,7, et un drive qui dérive pendant qu'on mixe (`MasterChain.cpp:387-411`). Sidechain audio, bus Musique, drive figé.
