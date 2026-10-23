# Testeur Pro 2 : ingé mix/mastering, Tech house « prêt à sortir »

Chemins relatifs à /home/user/ps-p22. Lecture seule, rien exécuté.

## Parcours réel
1. L'assistant génère « Tech house » (StyleRecipes.kt:269) : kit, basse ronde + sub (subBass), voix en lead, pas d'acide. Le mix de départ est auto (WizardMix.kt) : drums calés en LUFS par rôle (ROLE_LUFS), basse +2,5 dB, lead −1, nappe −1/−4, pan des hats/perc, bus drums = compresseur + saturation + EQ air, master = compresseur + saturation, pompe sur la basse et le sub, master −1 dB. C'est une vraie bonne base.
2. Mixeur (MixerScreen.kt:156) : en haut Partager + carte Automation, puis une carte par piste (fader dB, pan, Muet/Solo, VU peak, « Effets : … »), à droite/en bas « Sortie générale » (jauge peak, gain, ligne limiteur, 8 inserts, analyseur de spectre), Réverbe/Écho communs, Exporter.
3. Je cherche : GR du compresseur, sidechain réel, LUFS en direct, true peak, A/B, référence : rien. Je ne vois mon niveau en LUFS qu'APRÈS un export (MixerScreen.kt:~690 `export.loudness`).
4. Je tente de finir le master : seul le limiteur final (plafond −1 dBFS fixe) existe. Je pousse le gain général, le « Le limiteur travaille fort : baisser de 6 dB ? » me contredit (MixerScreen.kt:297). Je fais un export WAV « Sans limiteur » pour finir ailleurs : ça marche (32 bits float) et c'est ce qui sauve le parcours pro.

## Points durs

### 1. Pas de mesure de loudness pendant le mixage (LUFS / true peak / crête échantillon seule) — bloquant pour un pro
- Où : MixerScreen.kt:~160-185 (MasterMeter = peak entrée limiteur, MixerScreen.kt:218-250) ; LUFS seulement dans Loudness.kt appelé sur fichier exporté (MixerScreen.kt:~690). Aucun true peak nulle part (grep). Roadmap phase 22 annonce « mesure LUFS en direct » et « préréglages de mastering / Prêt pour le streaming (−14) » (docs/ROADMAP.md:175) : absents du code.
- Pourquoi : on mixe à l'aveugle, il faut exporter + attendre pour savoir si on est à −8 ou −14. Le « très fort / comme le club » (Loudness.describe) est bien pour un novice mais insuffisant.
- Fix : LUFS momentané/short-term/intégré calculés dans le moteur (K-weighting déjà écrit en Kotlin, à porter en C++ hors callback ou via file SPSC), + TP (suréchantillonnage 4x), + PLR/LRA ; affichés sous la jauge master. Cible sélectionnable (−14 streaming, −9 club) qui ajuste le gain avant limiteur.
- Pro : Youlean / iZotope Insight, Ozone Maximizer, Logic Loudness, tous les DAW donnent LUFS-I/S/M + TP en continu.

### 2. Compresseur : gain de compensation fixe qui s'ajoute même sans réduction, aucun indicateur GR — pénible/grave pour le gain staging
- Où : Effects.cpp:158-178 (`makeup_ = -seuil*(1-1/ratio)*0.5`, appliqué en permanence : `g = dbToGain(gr) * makeup_`, Effects.cpp:176) ; Effects.cpp:148-156 ; params sans makeup (Effects.kt:COMPRESSOR) ; `gainReductionDb()` existe (Effects.h:64) mais n'est jamais lu par l'UI (grep « reduction » côté app = 0).
- Pourquoi : le master par défaut (WizardMix.kt:~190, seuil −16 dB, ratio ≈2,4:1) ajoute ≈ +4,7 dB statiques AVANT le limiteur ; le bus batterie (≈ +5 dB) idem, plus la saturation Volume. Résultat : l'assistant livre un mix qui tape le limiteur (d'où le bandeau « travaille fort »). Le compresseur est un détecteur crête (pas RMS), genou fixe 6 dB, pas de mix parallèle, pas de HPF de détection, pas d'auto/manuel makeup, pas de lookahead. Le roadmap annonce « gain manuel et indicateur de réduction » : non livré.
- Fix : paramètre Makeup (±12 dB) + bouton Auto qui compense à l'écoute (gain 0 quand rien ne comprime), aiguille GR (dB) lue par atomique, sélecteur Peak/RMS, mix dry/wet, HPF sidechain 60-150 Hz.
- Pro : FabFilter Pro-C (GR en temps réel, makeup auto/manuel, mix), Ableton Compressor, SSL bus comp.

### 3. La « pompe » n'est pas un vrai sidechain — pénible
- Où : Effects.cpp:387-425 (Ducker : déclenché par évènement du séquenceur, enveloppe exponentielle fixe, attaque 2 ms figée) ; Engine.cpp:659-661 (`pattern_[0][patternStep] > 0` = voie kick du pad 1 uniquement).
- Pourquoi : (a) si le kick vient d'un autre pad, d'une piste instrument, d'un clip audio ou d'un échantillon remixé, la basse ne pompe pas ; (b) pas de courbe (forme du duck), pas d'attaque/lookahead réglable, pas de « Source » choisissable ; (c) la pompe est sur la basse ET sur le sub (StarterWizard.kt:~782) à profondeur 0,7 + la basse a sa propre pompe 0,45 (WizardMix.kt:~210) : double ducking non coordonné. (d) impossible de ducker une nappe/réverbe/retour via le kick (pompe uniquement en insert de piste, jamais sur les bus Réverbe/Écho ni sur le master).
- Fix : vrai sidechain (envelope follower du bus kick vers l'insert), choix de source (pad kick / piste), courbe dessinable ou 4 formes, attaque/relâche en ms, option « sur retour réverbe ». À défaut : pompe sur bus.
- Pro : Kickstart / LFOTool / Trackspacer, sidechain Ableton/Logic Compressor.

### 4. Limiteur master figé, aucun mastering — bloquant pour « release-ready »
- Où : Mixer.h:34-39 (plafond −1 dBFS en crête échantillon, lookahead 1 ms, relâche 120 ms fixes) ; Mixer.cpp:648-683.
- Pourquoi : pas de true peak (le plafond −1 dBFS en échantillon laisse passer des inter-sample peaks → M4A/AAC 256 kbps (AacEncoder.kt:19) peut écrêter) ; ni ceiling, ni release, ni gain de sortie, ni mode (transparent/agressif) réglables ; un limiteur 1 ms agit quasi en écrêteur doux sur la grosse caisse (distorsion sur les basses). Le gain avant limiteur est le seul levier (MasterGain, −24..+6). Pas de compresseur multibande, pas de M/S EQ, pas de EQ master avec plus que le « 3 bandes » fixe (100 Hz/1 kHz/8 kHz, Effects.cpp:114-121) ou une seule cloche (PARAM_EQ).
- Fix : limiteur à 4x suréchantillonnage, plafond −1/−0,3 réglable, release auto, lookahead 5 ms, gain de sortie ; préréglages « Club / Streaming −14 / Démo » qui posent EQ + comp + limiteur + gain en un tap (promis par la roadmap).
- Pro : FabFilter Pro-L 2, Ozone Maximizer, Logic Adaptive Limiter ; Ozone « Master Assistant ».

### 5. EQ trop pauvre pour corriger un mix — pénible
- Où : Effects.cpp:98-135 (Eq3 : bas shelf 100 Hz, cloche 1 kHz Q 0,9, haut shelf 8 kHz, ±12 dB) ; Effects.cpp:75-96 (Param EQ : UNE cloche + un coupe-bas 12 dB/oct) ; courbe de réponse en Effects UI EqCurve.kt (jolie) mais sans analyseur par piste.
- Pourquoi : pas d'EQ multibandes à fréquences libres, pas de coupe-haut, pas de pente, pas de shelves réglables, pas de Q sur les shelves, pas de mode M/S. Pour nettoyer 250 Hz sur un bus ou poser un air shelf à 12 kHz il faut empiler 2-3 PARAM_EQ (8 slots). Les presets (FxPresets.kt) n'ont que des EQ 3 bandes.
- Fix : « EQ 6 bandes » unique avec poignées glissables sur la courbe, spectre en fond, HPF/LPF 12/24, solo de bande.
- Pro : FabFilter Pro-Q 3, Ableton EQ Eight.

### 6. Niveaux par défaut : bonne idée, mais incohérences entre pistes ajoutées après coup — pénible
- Où : WizardMix.kt:47-53 (partDb) ; WizardMix.kt:~200 pump 0,45 ; docs/RETOURS.md:« Piste Acide ajoutée après coup … beaucoup plus fort que la nappe » (corrigé en phase 30 mais la calibration vit dans SoundLibrary/Acid.kt:38, pas dans le fader). 
- Pourquoi : le mix « calibré en LUFS » est cuit dans le gain du son (SynthPatch.GAIN, Plaits.gain) et NON dans le fader (« les faders restent à 0 dB », WizardMix.kt:~109). Un pro ouvre le mixeur : tous les faders à 0 dB, rien ne dit que le sub est à −x. On ne voit pas où est le niveau « réel » de chaque son ; il faut régler un paramètre caché (GAIN, non exposé comme bouton, SynthPatch.kt:39-42). Kick vs basse : le kick est à −12 LUFS-solo, la basse + sub ne sont mesurés qu'en solo, jamais ensemble ; pas de contrôle de phase kick/sub.
- Fix : afficher le « niveau calibré » (trim) en plus du fader ; bouton « Remettre au niveau assistant » par piste ; trim de 0 dB visible, fader libre.
- Pro : trim/gain + fader séparés (Pro Tools, Logic), « Gain Staging » plug-in.

### 7. Envois : seulement 2 bus, 4 réglages chacun, pas de pre/post, pas d'EQ de retour — pénible
- Où : Effects.kt:Buses (Taille/Amorti/Largeur/Pré-délai ; Écho Temps/Feedback/Ping-pong/Tonalité) ; Mixer.cpp:332-339 (envoi post-fader, au carré) ; reverb = Reverb::process Effects.cpp:1054.
- Pourquoi : pas de HPF/LPF sur le retour de réverbe (la réverbe qui brouille les graves est LE problème classique en house), pas de pompe sur le retour, pas de mode pre-fader, pas de niveau de retour accessible hors Live (setBusReturn seulement dans le performance FX, Mixer.cpp:~565), un seul type de réverbe (hall) et un seul écho. Les envois sont donnés en % au carré : le curseur n'indique pas de dB.
- Fix : HPF/LPF sur chaque retour (défauts 200 Hz / 8 kHz), curseur « Retour » + « Duck par kick », pre/post, 3 réverbes (chambre/plate/hall).
- Pro : Ableton Return A/B + EQ Eight + Glue/compressor sidechain.

### 8. Pan law +3 dB sur les côtés, pas de mono/phase — détail
- Où : Pan.h:9-13 (equal power × √2 : centre à 0 dB, côté +3 dB).
- Pourquoi : une percu pannée à fond est 3 dB plus forte qu'au centre ; pas d'outil de corrélation de phase ni bouton Mono/Mid/Side sur la sortie, alors que l'Élargisseur (Effects.kt WIDENER) peut casser la mono-compatibilité (club = mono).
- Fix : loi −3/−4,5 dB réglable ; goniomètre/corrélation sur le master ; bouton « Écouter en mono ».
- Pro : Logic/Ableton Utility, Pro Tools pan law, Voxengo SPAN, iZotope Insight.

### 9. Mesure par piste : crête seulement, ni RMS ni stéréo — détail
- Où : MixerScreen.kt:~345 `LevelMeter({ meter() / 0.89f })` ; holdMeterPeaks MixerScreen.kt:~745 (chute 0,85 par lecture, plancher −60 dB).
- Pourquoi : un VU à crête ne dit pas « qui est plus fort ». Pas de mono L/R, pas de maintien de crête, pas de clip LED, pas d'échelle graduée (dB) sur les pistes. L'analyseur de spectre n'existe que sur le master (MixerScreen.kt:~170) et sans comparaison.
- Fix : crête + RMS superposés, graduation −60..0, maintien 2 s, spectre par piste (superposition), « Solo + spectre ».
- Pro : Pro Tools meters, Ableton, Voxengo SPAN.

### 10. Sound design : modulation figée, pas de matrice — pénible (profondeur)
- Où : SynthPatch.kt:23-30 (29 paramètres : 2 oscillateurs + sub + bruit + unison, 1 filtre, 2 enveloppes, 2 LFO à destinations fixes : vibrato / balayage filtre, LFO2 sync tempo) ; SynthScreen.kt.
- Pourquoi : on ne peut pas router LFO → coupure / pitch / PWM / volume ; pas de LFO → onde ; pas de 2e filtre, pas d'ADSR à destinations libres, pas de wavetable, pas de FM ; seuls les oscillateurs PolyBLEP de DaisySP (propres mais basiques, Synth.cpp:16-18 ; le suréchantillonnage n'existe pas → un sync/saturation en aval alias). Pour la 303 : coupure/résonance/env/déclin/accent/accord/onde/saturation/volume (AcidScreen.kt:97) = fidèle, bien, mais pas de overdrive pédale ni delay interne.
- Fix : matrice de modulation à 4 slots (source LFO1/LFO2/Env2/Vélocité/Aftertouch → cible), un 2e LFO libre, filtre 12/24 dB sélectionnable ; saturation interne en 2x oversampling.
- Pro : Serum, Vital (gratuit/open source, modèle idéal à copier), Massive, Diva.

### 11. Pas d'« auto mix » / pas de guide mixage pour non-expert — pénible
- Où : aucun bouton (grep auto-mix = 0). Seuls l'assistant (une fois, à la création) et les FxPresets par piste (FxPresets.kt:suggestedFor).
- Pourquoi : si l'utilisateur ajoute une piste, remplace un son ou modifie les niveaux, il n'a aucun moyen de « remettre d'équerre » ; pas de conseil du type « ta basse masque ton kick à 60 Hz » ni de « recaler le master à −14 LUFS ».
- Fix : bouton « Équilibrer mon mix » (rejoue WizardMix sur le projet courant, avec annuler) + carte de conseils (masquage kick/basse, limiteur écrase, master trop faible).
- Pro : Logic « Mix Assistant », iZotope Neutron « Mix Assistant », Ozone « Master Assistant », LANDR.

### 12. Pas de A/B ni de référence — pénible
- Où : aucun code ; AudioTracksScreen peut importer un audio, mais il passe par le master (limiteur + effets) et se mélange au mix, aucune voie « Réf » hors-mix avec gain calibré.
- Fix : « Piste de référence » : un clip audio qui contourne master/export, bouton A/B (fondu), LUFS de la ref affiché et gain auto-calé sur celui du mix.
- Pro : Reference (Mastering the Mix), Ableton « Reference », Ozone Match EQ.

### 13. Export : bien, mais incomplet — détail/pénible
- Où : ExportCard MixerScreen.kt:~455-690 ; WavWriter.cpp:69-110 (24 bits TPDF, sinon 32 float) ; AacEncoder.kt:19 (256 kbps) ; ExportService.
- Bien : WAV 24 bits avec dither TPDF, FLAC, « Sans limiteur (pour le mastering) » en 32 bits float, pistes séparées « avec / sans effets master », MIDI, LUFS mesuré après coup.
- Manques : pas de choix de fréquence (44,1/48 — la roadmap parle de 44,1 kHz), pas de profondeur choisie (16/24/32 selon le format = `floatWav = !settings.limited`, RhythmViewModel.kt:6082, pas d'option claire), pas de normalisation à une cible LUFS, pas de MP3, pas de métadonnées (titre/artiste/BPM/tonalité), pas de « bounce de région », pas de dither choisi pour 16 bits, M4A 256 kbps non réglable. Le chiffre LUFS n'indique pas la crête vraie ni la plage dynamique.
- Fix : bloc « Format avancé » (fréquence, profondeur, cible LUFS, normaliser), métadonnées, rapport (LUFS-I, TP, LRA, PLR, clipping) sur l'écran de fin.
- Pro : Logic Bounce, Ableton Export, Reaper Render (norm. + dither).

### 14. Qualité sonore des instruments (écoute impossible, lecture du code) — à vérifier
- Batterie : synthèse DaisySP/DrumKit.cpp + échantillons/packs ; la calibration WizardMix.KIT_LUFS montre un kick à −5,5 LUFS solo et un charley fermé à −45 : l'écart de 40 dB est compensé par fader + shelf 8 kHz (WizardMix.kt:~76-85), c'est-à-dire que le hat « brillant » est un hat très faible remonté de +9 dB par EQ → souffle/aigu aigre possible.
- 303 : oscillateur PolyBLEP, filtre ladder-like : OK ; clics : le séquenceur utilise des smoothers (Smoother.h) et un denormal guard ; rien d'inquiétant visible. Pas de test d'alias en suraiguë (Bitcrusher/Distorsion/Saturation sans suréchantillonnage : « Repli » et « Écrêtage dur » aliasent forcément).
- Fix : oversampling 2x sur Distorsion/Saturation/Bitcrusher/Repliement ; kit à niveaux d'origine plus homogènes.
- Pro : Saturn 2 (oversampling), Decapitator.

### 15. Fatigue de navigation du mixeur — détail
- Où : MixerScreen.kt:150-190 : tout est en colonne sur téléphone ; le master (jauge, gain, comp, limiteur) est sous TOUTES les pistes en phone (AdaptivePanes) ; chaque piste prend ≈ 700 px de haut (mixer.png) avec 2 curseurs larges : impossible de voir 6 pistes à la fois, pas de vue « console » avec faders verticaux.
- Fix : vue console horizontale (faders verticaux + VU + M/S + insert dots), master toujours épinglé à droite.
- Pro : FL Studio Mobile mixer, Ableton Live mobile, BandLab.

## Fonctions manquantes (classées)
1. LUFS (M/S/I) + true peak + PLR en direct sur le master, cible réglable (−14 / −9) avec bouton « caler le master ».
2. Indicateur de réduction de gain et makeup manuel/auto sur le compresseur (et compresseur master avec sidechain HPF, mix).
3. Vrai sidechain (source au choix, courbe réglable) y compris sur les retours et le bus de nappes.
4. Limiteur master réglable (plafond, relâche, TP 4x) + préréglages de mastering (Club / Streaming / Démo) + EQ master (M/S, shelves libres).
5. Bouton « Équilibrer mon mix » (auto mix) + conseils de mixage lisibles.
6. Piste de référence + A/B calé en loudness.
7. EQ multi-bandes graphique + analyseur de spectre par piste.
8. Retours avec EQ (HPF/LPF), niveau de retour, pre/post, plus de types de réverbe.
9. Mesure RMS/gradués + mono/phase/corrélation + écoute en mono.
10. Matrice de modulation dans le synthé + LFO libres ; oversampling des effets de saturation.
11. Export : fréquence/profondeur/normalisation LUFS/métadonnées/rapport.
12. Vue console (faders verticaux) + groupes/bus de sous-mixage (batterie, basses, musique).

## Top 5
1. Aucune mesure de loudness en direct (ni LUFS ni true peak) : on mixe à l'aveugle, le LUFS n'arrive qu'après l'export (Loudness.kt, MixerScreen.kt ~690), alors que la roadmap phase 22 le promettait.
2. Le compresseur ajoute un gain fixe même sans compression (Effects.cpp:176) et n'affiche aucune réduction : le master et le bus batterie par défaut poussent le limiteur de ~5 dB chacun.
3. La « pompe » n'est pas un sidechain : déclenchée par la seule voie kick du pad 1 (Engine.cpp:661), enveloppe fixe, double ducking basse + sub, impossible sur réverbe/nappe.
4. Master figé : limiteur −1 dBFS en crête (sans true peak), 1 ms, rien de réglable ; pas de préréglage de mastering, EQ 3 bandes fixes ou une seule cloche.
5. Pas d'auto mix, pas de référence/A-B : après la création par l'assistant, tout ajout ou changement te laisse seul sans conseil ni bouton « rééquilibrer ».
