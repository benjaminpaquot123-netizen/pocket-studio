# Testeur Pro 1, vague 2 (après la phase 33) : techno mélodique, 124 BPM, 3-4 min

Méthode : lecture de la branche `ccr-80af1a1b-mn0wec` (58a3f28, phase 33 fusionnée), des textes `tr("…")`, des captures `app/screenshots/`, de `docs/PLAN-AMELIORATION.md`, `ARCHITECTURE.md`, `RETOURS.md`, `son/BANC.md`. Rien lancé. J'ai fait mon parcours et mon avis d'abord, puis seulement ouvert `docs/audit-33/pro1.md` pour comparer. Chemins relatifs à `app/src/main/java/com/pocketstudio/app/` sauf mention. Ce que la phase 33 a touché (`git diff 1badb32..HEAD`) : TopBar, PlayStrip, TempoSheet, ShareSheet, SongReady, NextStepStrip, MasterLoudness, MixerScreen, EffectsBox, Effects.kt, LoudnessTarget, RhythmScreen (transport), AppRoot. Pas touché : SessionScreen (−6 lignes), TimelineScreen, PianoRoll, AutomationCard, SynthScreen, AcidScreen, AudioTracksScreen, Scenes.kt, Notes.kt.

## Parcours réel (gestes comptés)

1. **Démarrer.** Projet › Nouveau › déplier Techno › carte du style › assistant. À l'arrivée : « Ton morceau est prêt ! · 2:40 · 6 parties » avec Écouter / Changer un son / Modifier le morceau / Partager (SongReady.kt:138-168). 4-5 gestes, un morceau de 3:30 déjà structuré (Intro, Groove, Drop, Break…), piste « Montée » et courbes de filtre écrites par l'assistant. Nouveau, c'est le bon atterrissage.
2. **Écouter tout le morceau : 1 geste**, ▶ en haut sur tous les onglets ; le bandeau sous la barre dit « Drop · mes. 33/112 » en vert (PlayStrip.kt:120-200) et « Jouer depuis… » liste les parties (2 gestes). Avant : 2 gestes, et ▶ jouait la section. Réglé.
3. **Tempo 124 : 5 gestes** (toucher « 128 BPM » › 1 › 2 › 4 › OK, TempoSheet.kt:97-170), ou 1 geste pour 120/140. Avant : 2 à 17. Pas de −/+ ni de swing dans la feuille ; le swing reste dans la carte Rythme (RhythmScreen.kt:644).
4. **Boucler la partie que j'édite : 1 geste** « ⟳ Partie » (PlayStrip.kt:224-250). Piège : si je lance ▶ depuis Rythme sans l'avoir armé, le morceau part et « suivre le morceau » (par défaut, SessionScreen.kt:378) fait changer la scène affichée sous mes doigts à chaque partie ; et toucher une autre scène pendant la lecture bascule tout seul en ⟳ Partie (RhythmViewModel.kt:1703-1706), donc le prochain ▶ ne joue plus le morceau. Le chip le montre, mais rien ne le dit.
5. **Arrangement** (Morceau, SessionScreen.kt:318-410) : identique à la vague 1. Une partie = « A · Intro · × 2 · 4 mes. », −/+ sur les répétitions, ⋮ Monter / Descendre / Dupliquer / Insérer une scène après / Retirer (SessionScreen.kt:520-550). Ligne du temps (TimelineScreen.kt) : toucher un clip = couper/remettre la piste dans la partie (:1171-1177), appui long = Ouvrir dans Notes/Rythme, Changer de scène…, ×, Couper la partie, Dupliquer, Insérer, Boucler, Retirer (:1488-1560). Visuellement, elle est au niveau de FL Mobile (timeline_screen.png).
   - Break de 8 mesures sans kick qui garde les charleys : **≈ 15 gestes** (nouvelle scène, vider kick et caisse pad par pad, « Changer de scène… » pour la batterie dans la partie). Pareil.
   - Fill sur la dernière mesure du Groove : **≈ 16 gestes** (Couper la partie à la mesure N › scène › Rythme › Changer de scène…). Pareil.
   - Variation d'un Drop joué 3 fois : **≈ 9 gestes**, et toujours une scène entière de plus (24 max, Scenes.kt:36). Pareil.
   - Passer une partie de 8 à 12 mesures : impossible directement (répétitions seulement), il faut couper. Pareil.
   - Déplacer une partie de 3 places : ⋮ Monter ×3 (6 gestes) ou glisser le drapeau de la règle (caché, 2 gestes). Pareil.
6. **Piano roll** (PianoRoll.kt:128-215) : tap = supprime (:213), copier/coller seulement en mode « ⬚ Sélectionner » (2 gestes pour y entrer), presse-papiers `remember` local (:135) perdu en changeant d'onglet, boucle 1/2/4/8 mesures (Notes.kt:152), Quantifier 50/100 %, Legato / Humaniser / Dupliquer / ±½ ton / ±octave en sélection (:244-277). Copier le motif du lead vers la nappe ou vers la scène du Break : **impossible**. Pareil.
7. **Batterie** (RhythmScreen.kt:500-540) : `combinedClickable` (tap, appui long = force), pas de peinture au glissé, roulements ×2 à ×4 décroissants (:615), longueur commune 1-4 mesures (Project.kt:231), un seul volume pour toute la batterie au mixeur. Pareil. Nouveau doublon : la carte transport de Rythme (RhythmScreen.kt:227-280 : Jouer/Stop, ↶ ↷, −/+ tempo, Taper) reste au-dessus des pads alors que la barre du haut a ▶ ↶ ↷ et le bandeau a le tempo : **3 lectures et 2 tempos sur le même écran**.
8. **Automation** (AutomationCard.kt) : cibles = volume, pan, envois, réglage d'un insert, bus, 303, macros, Live (Automation.kt:26-46). Toujours **aucun potard du synthé ni de Plaits**. Mode Morceau = 1 point par mesure (:510), axe en numéros de mesure sans nom de partie (automation.png). Montée de filtre avant le drop : **≈ 11 gestes** via un insert « Filtre » dont rien ne parle sur l'écran Synthé. Pareil.
9. **Mixage** (MixerScreen.kt:203-231, 440-540) : une carte par piste, 2 pistes par écran de téléphone (mixer.png), Muet / Solo / courbes / volume / pan / Effets. Compresseur refait (Effects.kt:77-96) : seuil, ratio, attaque, relâche, gain, gain auto, détection crête/RMS, filtre des graves, mélange, et un vumètre de réduction (compressor_editor.png : « Réduction −6,5 dB »). Nouveau et honnête. Pas de genou, pas de source sidechain : « Pompe » = grosse caisse du pad ou chaque temps (Effects.kt:127-135). Envois : 2 bus fixes réverbe + écho (Effects.kt:433), curseurs à 3-4 gestes dans Effets › « Pad XY, macros, réverbe et écho communs » (EffectsBox.kt:82, 296-316). Pas de bus de groupe. Solo oublié : bandeau ambre « Tout désactiver », 1 geste (PlayStrip.kt:291). Nouveau.
10. **Master** (MasterLoudness.kt, mixer_master.png) : « −8,1 LUFS · TP −1,2 » avec témoin de crête, lecture Maintenant / sur 3 s / depuis ▶, « Volume visé » Auto (style) · −8 / Club · −8 / Streaming · −14 / Désactivée, « Gain ajouté pour la cible : +3,2 dB », « Le limiteur protège la sortie (−1,9 dB) ». Chaîne : gain de cible → écrêteur ClipOnly2 à 4× → limiteur true peak −1 dBTP (ARCHITECTURE.md § Chaîne master). Nouveau, et c'est ce qui manquait le plus au mix. Réserve : le banc (docs/son/BANC.md:44-70) montre que la sonie vient du limiteur : +8,6 dB de gain médian pour 4,7 LU de perte, facteur de crête du Drop 11,4 → 7,7 dB, et **Drop − Break passe de +1,4 à −0,2 LU**. Pour une oreille pro : un bouton « fort » qui écrase le kick et aplatit la dynamique entre parties. Aucun réglage du plafond, du relâchement, ni du partage écrêteur/limiteur ; pas d'historique LUFS ni de LRA. Les projets de l'assistant démarrent en Auto (StarterWizard.kt:520), les projets vides en Désactivée (Project.kt:365).
11. **Partager : 3 gestes** (⤴ › nom pré-rempli › Partager › appli). La feuille « Prêt à partager ? » (ShareSheet.kt:274-366) donne durée, parties, LUFS, et vérifie solo oublié, piste coupée, morceau vide ou d'une seule partie, limiteur qui force, avec réparation en 1 geste. M4A (visé −2 dBTP) ou WAV. Nouveau, très bon. L'export complet reste dans Mixeur › Exporter en bas de page (mixer_export.png) : WAV / FLAC / M4A, 44,1 kHz, 16 bits, « Sans limiteur » = WAV flottant sans chaîne master (RhythmViewModel.kt:6164, 6220), stems avec ou sans effets de sortie, MIDI, rapport « −8,4 LUFS · crête vraie −1,2 dBTP · 3 min 34 s ». WAV 24 bits sans limiteur pour mastering ailleurs : **6 gestes** (⤴ › Autres exports › WAV › Avancé › Sans limiteur › Exporter). Stems : la batterie sort en **un seul fichier** (StemExports.kt:32), pas kick / caisse / charleys. Pas de gel de piste.
12. **Live** : la prise écrit toujours « Remplacer le morceau » par défaut (LiveScreen.kt:606) ; « M et les pads de l'onglet Pads ne sont pas enregistrés » (:575). Pareil.

Bilan de mon parcours : écouter, caler le tempo, vérifier le niveau et partager sont devenus pro. Tout ce qui est **entre** les deux (variations, fills, breaks, notes, automation du synthé) coûte exactement le même nombre de gestes qu'avant.

## Points durs

### 1. Variations, fills, breaks : toujours une scène entière à dupliquer (bloquant)
- Où : SessionScreen.kt:520-550 (menu ⋮), TimelineScreen.kt:1488-1560 (menu de clip), Scenes.kt:13-45 (une partie = scène × répétitions).
- Pourquoi : aucun clip indépendant par piste, ni « rendre unique », ni « fill », ni « break ». Un morceau de 3-4 min avec 2 variations par section mange les 24 scènes. Rien n'avertit qu'une scène est jouée 3 fois.
- Fix : la feuille de partie prévue en phase 34 (Variation, Fill, Break, Montée, Fin) + bandeau « jouée 3 fois : toutes / celle-ci ». FL Mobile : « Make unique » sur un clip ; Ableton : chaque clip est indépendant, Ctrl+D.

### 2. Potards du synthé et de Plaits non automatisables, 1 point par mesure, pas de noms de parties (bloquant pour un lead mélodique)
- Où : Automation.kt:26-46 (`AutoTarget.isValid`), AutomationCard.kt:510 (« Une valeur par mesure »), automation.png (axe « mesure 1 5 9 13 »).
- Pourquoi : le filtre qui s'ouvre sur 16 mesures est le geste du genre ; il faut un insert « Filtre » (non dit sur l'écran Synthé) et viser les mesures sans repère de partie. Pas de coup de filtre sur une noire, pas d'automation de tempo.
- Fix : appui long sur un potard › « Automatiser » (prévu phase 37), ¼ de mesure, noms de parties sur l'axe. Ableton : clic droit › Show automation ; FL : clic droit › Create automation clip.

### 3. Doublons de transport créés par la phase 33 (pénible)
- Où : RhythmScreen.kt:227-280 (carte Jouer/Stop + ↶ ↷ + −/+ tempo + Taper) vs TopBar.kt:214-245 (▶ ↶ ↷) et PlayStrip.kt:148-165 (tempo). Sur Morceau : ▶ en haut, « Jouer le morceau » (SessionScreen.kt:360-370, `song_play`), ▶ de la carte Scènes (:553, `session_play`), bouton Live.
- Pourquoi : 3 lectures et 2 tempos sur Rythme, 4 lectures sur Morceau ; un pro se demande lequel joue quoi (la carte Rythme dit « Jouer » sans préciser, et joue le morceau si ⟳ Partie est éteint).
- Fix : retirer la carte transport de Rythme (garder Swing / Métronome / Décompte), retirer « Jouer le morceau » et le ▶ des scènes. FL Mobile et Ableton : un seul transport.

### 4. Éditer pendant la lecture : la scène affichée change sous les doigts (pénible)
- Où : RhythmViewModel.kt:1696-1710, SessionScreen.kt:378-384 (« suivre le morceau » par défaut).
- Pourquoi : ▶ joue le morceau partout, donc si je règle un charley dans Rythme sans armer ⟳ Partie, l'éditeur saute de A à B à C ; et toucher une scène pendant la lecture allume ⟳ Partie sans le dire.
- Fix : dans Rythme / Acide / Notes, ▶ propose « ⟳ Partie » par défaut, ou l'éditeur reste verrouillé sur la scène éditée pendant la lecture ; toast « ⟳ Partie activé » lors de la bascule automatique. Ableton : l'éditeur de clip ne suit pas la lecture de l'arrangement.

### 5. Piano roll : tap supprime, presse-papiers local, boucles 1/2/4/8 (pénible)
- Où : PianoRoll.kt:135, :212-213, Notes.kt:152.
- Fix : tap = sélection, appui long = suppression, presse-papiers dans le ViewModel partagé entre pistes et parties, boucle à la mesure près (prévu phase 37). FL Mobile : sélection par tap, copier/coller entre pistes.

### 6. Batterie : pas de glissé, roulements ≤ ×4, une seule longueur, un seul volume (pénible)
- Où : RhythmScreen.kt:519-536 (`combinedClickable`), :615 (roulements), Project.kt:231 (`MAX_STEPS = 64`), MixerScreen (une voie Batterie).
- Fix : peindre en glissant, ×6 ×8 montants et 1/32, longueur par pad, sous-voies par pad dans le mixeur (prévu phases 34 et 37). FL Mobile : note repeat jusqu'à 8, pattern length par canal.

### 7. La sonie vient du limiteur : le Drop est plus écrasé que le Break (pénible, son, nouveau)
- Où : core/MasterChain (ARCHITECTURE.md), docs/son/BANC.md:67-70, MasterLoudness.kt:80-120 (« Volume visé »).
- Pourquoi : +8,6 dB de gain médian, 4,7 LU de perte dans écrêteur + limiteur, Drop − Break −0,2 LU, PLR 7,2 dB. À l'écoute, c'est le « loudness war » d'un seul bouton ; le kick de 1,5 s en est la cause (étape 3 prévue). L'UI ne montre que « gain ajouté » et « limiteur protège (−x dB) », pas combien de LU le morceau perd.
- Fix : en attendant les kits de l'étape 3, plafonner le gain de la cible (+4 dB) et afficher « plafonné : la cible demande plus que le mix ne peut donner » ; un réglage écrêteur ↔ limiteur ; afficher la perte en LU et la différence Drop − Break dans « Prêt à partager ? ». Ableton / Ozone : on voit la réduction de gain du limiteur en continu et on choisit le gain.

### 8. Mixeur en cartes verticales, LUFS en bas de page, envois enfouis (pénible)
- Où : MixerScreen.kt:203-231 (ordre : pistes, automation, master, export), :440-540 (carte par piste), EffectsBox.kt:296-316 (envois).
- Pourquoi : 8-10 pistes = 5 écrans de défilement ; la lecture LUFS/TP n'est visible qu'au fond du Mixeur ; régler un envoi réverbe = 3-4 gestes.
- Fix : la puce « −8,1 LUFS · TP −1,2 » dans le bandeau de lecture sur tous les onglets (elle existe déjà, `LoudnessChip`) ; console à faders verticaux (phase 36) avec envois sur la voie. FL Mobile : mixeur horizontal, envois sur la tranche.

### 9. Compresseur sans sidechain externe ni genou, « Pompe » limitée, 2 envois, pas de groupes (pénible)
- Où : Effects.kt:77-96 (compresseur), :127-135 (`PUMP`), :433 (`Buses.COUNT = 2`).
- Pourquoi : pas de « Source : piste » ; la pompe ne suit pas une 808 ou un pad ; réverbe courte + longue impossibles.
- Fix : champ source sur le compresseur et la pompe (phase 35 : « vrai sidechain déclenché par l'audio du kick », bus Musique), 4 envois, bus de groupe. Ableton : « Audio From » ; FL : Peak Controller.

### 10. Stems : batterie en un fichier ; pas de gel ni de bounce de piste (pénible)
- Où : StemExports.kt:32 (« batterie » = toute la batterie), aucune fonction « Geler » (prévue phase 37).
- Fix : stems par pad (kick / caisse / charleys / percus) ou par groupe d'étouffement ; « Geler la piste » en clip audio annulable. Ableton : Freeze/Flatten, export par piste ; FL : « Split mixer tracks ».

### 11. Vocabulaire : « Structure » et « Partie » promis, « Morceau » et « scène » restent (détail)
- Où : AppRoot.kt:188 (onglet « Morceau »), TopBar.kt:290-335 (« Scène à modifier », « Nouvelle scène vide »), SessionScreen.kt (« Insérer une scène après », carte « Scènes », « Une scène = la batterie, l'acide et le piano roll »). 62 textes `tr()` contiennent « scène » contre 54 « partie ».
- Fix : finir le renommage de la phase 33 (plan : « seulement Partie, Morceau, Projet » ; onglet « Structure »).

### 12. Feuille du tempo sans −/+ ni swing ; export WAV sans limiteur à 6 gestes (détail)
- Où : TempoSheet.kt:97-170, RhythmScreen.kt:644 (swing), ShareSheet.kt:332 (« Autres exports » → Mixeur).
- Fix : −1/+1 et swing dans la feuille ; puce « WAV 24 bits sans limiteur » directement dans « Prêt à partager ? » (format mémorisé).

## Fonctions manquantes, classées

1. Clips indépendants par piste et par partie (variation, fill, break, montée, fin en 1-2 gestes).
2. Automation des potards du synthé / Plaits, résolution au ¼ de mesure, noms de parties sur l'axe, automation de tempo.
3. Longueur de partie libre (tirer le bord d'un clip) et copier/coller de clips entre pistes et parties.
4. Sidechain à source choisie, genou du compresseur, bus de groupes, 4 envois.
5. Gel / bounce d'une piste en audio ; stems de batterie par pad ; plus de 4 pistes audio.
6. Batterie : longueur par pad, peinture au glissé, roulements ×8 et 1/32, probabilité, volume par pad au mixeur.
7. Piano roll : tap = sélection, presse-papiers global, boucle à la mesure près.
8. Master : réglage du plafond et du relâchement du limiteur, écrêteur ↔ limiteur, historique LUFS court terme et LRA, LUFS dans le bandeau.
9. Transposition par partie / accords du morceau (phase 37).
10. Live : prise en nouvelle version par défaut, pads enregistrés.

## Comparé à la vague 1

Top 5 de la vague 1 :
1. Variations par piste (clip rendu unique + fill en 1 tap) : **pareil**. Aucune ligne changée dans SessionScreen / TimelineScreen / Scenes.kt ; toujours ≈ 9 gestes pour une variation, ≈ 16 pour un fill.
2. Automation des potards du synthé + résolution < mesure : **pareil**. `AutoTarget` inchangé (Automation.kt:26-46), 1 point par mesure, axe sans nom de partie.
3. Étirer la longueur d'une partie + copier/coller de clips : **pareil**. Répétitions seulement, menu ⋮ identique.
4. Sidechain réel + bus + envois : **mieux sur le compresseur** (gain, gain auto, RMS, filtre des graves, mélange, vumètre de réduction : Effects.kt:77-96 ; il n'ajoute plus +3,6 dB à vide), **pareil** pour la source sidechain, les 2 envois, l'absence de bus.
5. Piano roll (tap = sélection, presse-papiers global, boucle à la mesure) et batterie (glissé, longueur par pad) : **pareil**. PianoRoll.kt:135 et RhythmScreen.kt:519-536 identiques.

Autres points de la vague 1 : « pas de longueur de partie directe » pareil ; « transitions hors Live » pareil ; « lisibilité de la structure » pareil (toujours « × 4 · 4 mes. », pas de « mes. 17-24 ») ; « scène partagée sans avertissement » pareil ; export : **mieux** (44,1 kHz, 16 bits, M4A sous −2 dBTP, rapport true peak + durée, stems avec/sans effets master).

**Nouveau depuis la vague 1 (réglé ou ajouté) :**
- ▶ joue le morceau partout, « ⟳ Partie », position « Drop · mes. 33/112 », « Jouer depuis… » : réglé (c'était « ▶ joue la section » dans le résumé AUDIT-33 point 2).
- ⤴ Partager dans la barre + « Prêt à partager ? » avec contrôles et réparations ; « Ton morceau est prêt » ; fil « Prochaine étape » ; bandeau « Solo actif » ; renommer par appui long ; tempo au pavé.
- Chaîne master true peak (37/37 morceaux ≤ −1 dBTP au banc), « Volume visé » par genre / Club / Streaming, LUFS et TP en direct avec témoin de crête, gain ajouté affiché : c'est ce qu'un pro demandait pour juger le niveau. Réserve au point dur 7.
- Saturation sans passe-bas caché, option « Volume constant ».

**Nouveaux problèmes (créés ou révélés par la phase 33) :** doublons de transport (point 3), édition pendant la lecture qui suit le morceau et bascule silencieuse en ⟳ Partie (point 4), sonie obtenue par le limiteur avec Drop − Break négatif (point 7), renommage « Structure / Partie » inachevé (point 11).

## Top 5 maintenant

1. **Clips par piste : Variation, Fill, Break, Montée, Fin en 1-2 gestes** (feuille de partie de la phase 34) ; sans cela 3-4 minutes soignées restent ≈ 9-16 gestes par idée et une scène par variation.
2. **Automation des potards du synthé / Plaits**, ¼ de mesure, noms de parties sur l'axe.
3. **Un seul transport** : retirer la carte Jouer/tempo de Rythme et les ▶ de Morceau ; dans un éditeur, ▶ boucle la partie éditée (ou la verrouille) et la bascule ⟳ Partie est dite.
4. **Sonie honnête** : plafonner le gain de la cible, montrer la perte en LU et Drop − Break, réglage écrêteur ↔ limiteur ; en parallèle kick court et kits (étape 3). Mettre la puce LUFS/TP dans le bandeau de lecture.
5. **Piano roll et batterie pro** : tap = sélection, presse-papiers global entre pistes et parties, boucle à la mesure ; glissé, roulements ×8, longueur et volume par pad ; stems de batterie par pad et gel de piste.
