# LIVE-PLAN — Phase 27, mode Live (plan de l'architecte, sauvegardé par l'intégrateur)

Demandes de Benjamin : voir LIVE-NOTES.md. Numéros de commandes moteur provisoires (70+) : à renuméroter après la fusion de la phase 26 (fxsec prend 69, acidcore ajoute peut-être des commandes/kind 5 pour la 303).

## Résumé des choix
1. Moteur : réutiliser le staging de scène existant ; 15 « slots » par piste (Drums 0, Acid 1, Inst 2..9, Audio 10..13, Remix 14), chacun avec son pas de départ, un drapeau stop et un lancement armé sur un quantum global (≥ 1 mesure, choix 1/2/4).
2. Écriture dans le morceau : champs optionnels dans SongPart (`play: Map<String,Int>` scène par piste, `from: Map<String,Int>` mesures déjà jouées, `bars` longueur explicite) — option A, recommandée — plutôt que des scènes « mix ». `Project.partSteps(part)` remplace ~25 usages de `sceneSteps(scene) * repeats`. MAX_PARTS 32 → 128.
3. Gestes d'effets et de la 303 enregistrés dans des courbes « morceau » à un point par temps (`songPerBeat`, 1024 points, `hold` pour les contrôles on/off) ; lanes 8 → 16 (Automation sur le tas) ; nouveaux kinds 6 MACRO (macros + hachoir de voie) et 7 LIVE (PerformanceFx filtre/réso/répéter/division/freinage/hachoir + retour réverbe/écho) ; kind 5 = 303 (phase 26).
4. Navigation : page Live plein écran ouverte depuis Morceau (pas de 8e onglet), qui cache la barre du bas ; grille + panneau bas Effets / 303 / Pads + bouton ●.
5. Livraison en 4 étapes 27.1 → 27.4.

## 1. Moteur
- Engine.h : `SlotPlay { start, stopped, pendingTag, quantum, stagedOffset, tag }`, `slots_[15]`, atomiques `slotTag_/slotStart_`, `transportStep_`, `SpscQueue<LaunchEvent,256> launchEvents_` (audio → UI).
- Commandes : `ArmLaunch(a=masque slots, b=quantum pas, value=scène ou -1 stop)`, `CancelLaunch(masque)`, `SetSlotOffset(slot, pas déjà joués)` (suit SetStaging), `MarkSong(quantum, pas de morceau)` (début d'une prise), `SetRemixOn(value)`.
- onStep : avant tout son au pas s, pour chaque slot armé avec s % quantum == 0 → `swapSlot(k, s)` qui copie seulement la part du staging de ce slot (batterie, acide, piste instrument `swapIn()`, audioOn/loop, remixOn, courbes par scène dont owner == k, insertOn fxsec du canal), met `start = s - stagedOffset`, `stopped`, publie et pousse un LaunchEvent. Lancement de ligne (masque complet) = aussi master/bus, `sceneStart_ = s`, `sceneSwaps_++`.
- `swapScene()` (ArmSceneSwap, mode morceau) = tous les slots, annule les lancements en attente.
- Chaque piste compte depuis son slot : `localOf(k) = s - slots_[k].start` (acide avec drapeau `silent`, NoteTrack sauté si stop mais note-off tirés, batterie patternStep, AudioTracks avec un local par piste).
- Swing : starts/offsets multiples d'une mesure → parité inchangée. SetTransport/stopTransport remettent tout à zéro. < 3 Ko, aucune allocation/verrou/log/JNI.
- Coexistence : pendant la lecture du morceau, le premier toucher de pad termine le suivi du morceau (CancelLaunch + ArmSceneSwap(-1) + leaveSong sauf prise en cours). ▶ « Écouter le morceau » remet en mode ligne.
- JNI : transportStep(), slotStates(IntArray), drainLaunchEvents(IntArray); NativeEngine : armLaunch, cancelLaunch, setSlotOffset, markSong, setRemixOn, constantes SLOT_*.
- Tests hôte : lancement par slot à la mesure ; longueurs de boucle différentes ; stop == contenu vidé (échantillons identiques) pour chaque type ; export identique (lancements live vs swaps du morceau avec SetSlotOffset) ; la dernière commande gagne ; ArmSceneSwap annule ; courbe par scène suivant le slot acide ; insertOn ; compteur d'allocations ; file d'événements pleine ; slide acide vers stop relâche la note.

## 2. Modèle
- `Live.kt` : `LiveSlot`, `LiveState(row, slots, quantumBars)` + réducteurs purs ; `Project.cellHasContent(key, scene)` ; `Project.liveQuantum` (1/2/4 mesures) sauvegardé ; clé `SongPart.REMIX`.
- SongPart A : `play`, `from`, `bars` (défauts → anciens fichiers inchangés) ; `sceneFor(key)`, `partView`, `audioPlays`, pushPart/describe envoient `setSlotOffset` ; sanitize ; syncScenes/moveScene/copySceneTo tiennent compte de `play`.
- `PerformanceRecorder` : découpe [T0,T1) à chaque événement → parties (scene de la ligne, play, muted, from, bars ou repeats), fusion des parties identiques ; « Remplacer le morceau » (défaut) ou « Ajouter à la suite » ; gestes → lanes SONG (création, bascule de mode en gardant les anciennes valeurs, point de départ avant le premier geste, latence compensée).
- Annuler : une seule étape par prise (commitQuietEdits), Annuler désactivé pendant la prise.
- Tests : LiveTest, PerformanceTakeTest, extensions SongArrangementTest/SongFollowTest, compatibilité JSON, followArrangement par temps.

## 3. Interface
- Barre Live (56 dp) : ← Morceau, ▶/■, ● Enregistrer la performance, tempo, « Lancer à : 1 mesure », compteur mesure.temps avec anneau jusqu'au prochain lancement.
- Grille : colonne gauche collante « ▶ A · Refrain » par scène (ligne en cours marquée) ; en-tête par piste (nom, couleur, M = muet instantané non écrit) ; pied ■ par piste (stop à la mesure, enregistré) et « ■ Tout » ; pads ≥ 64 dp téléphone / 80 dp tablette : vide, contenu, en attente (clignote, →), joue (rempli + camembert de position).
- Panneau bas : Effets | 303 | Pads (repliable), la grille garde ≥ 3 lignes.
- Téléphone portrait : grille poids 1 (≥ 300 dp), panneau 340–360 dp ; tablet8 600×1006 : Effets et 303 côte à côte ; paysage : grille à gauche, panneau à droite.
- Effets (LiveFx.kt) : cibles « Tout le mix » + une puce par piste ; global : XY filtre DJ (réutilise LivePad), maintiens Répéter/Freinage/Hachoir, curseurs retour Réverbe/Écho ; piste : XY Couleur × macro, 4 macros 56 dp, envois, Hachoir de voie, bouton « Effets… » vers EffectsBox. Correction défaut 5 dans QuickFx.XyPad (étiquettes X au centre gauche/droite, Y en haut/bas centre ; « Pad XY, haut-bas : » sur sa propre ligne). Tous les gestes passent par le ViewModel (capture).
- 303 (LiveAcid.kt) : XY ≥ 220 dp coupure × résonance (sans retour au centre, double toucher = valeur sauvegardée) ; 4 grands curseurs 56 dp Enveloppe, Decay, Accent, Saturation ; « Basse arrêtée » si le slot acide est stoppé.
- Bouton ● : Prêt → Armé (décompte) → REC 0:42 · mesure 21 (■ Fin) → Écriture → Fait (« Morceau écrit : N parties, M réglages · Annuler · Écouter »).
- Morceau : cases de la timeline avec la lettre de scène si `play`, « ↪ » si `from` > 0, appui long = choisir la scène, badge « mélange ».

## 4. Étapes
- 27.1 La grille Live (jouer, pas encore enregistrer) : moteur slots + lancements + stop + remix ; page Live, ligne/pad/■/M/quantum ; editsWait par slot ; resync ; pads live dans l'onglet « Pads » ; défaut 5 ; tests + captures (téléphone, tablet8, tablette, paysage).
- 27.2 Effets et 303 sous les doigts : kinds 6/7 (+5), 16 lanes, hachoir de voie, retour de bus ; LiveFx, LiveAcid, puces de cible.
- 27.3 Enregistrer la performance : SongPart play/from/bars, partSteps (25 sites), PerformanceRecorder, MarkSong, file d'événements, lanes par temps, ● et annulation en une étape, timeline.
- 27.4 (option) Pads live et remix dans le morceau (liveCues), « Aplatir en scènes ».

Fichiers critiques : engine/.../core/Engine.cpp, Engine.h, project-model/.../Scenes.kt, app/.../rhythm/RhythmViewModel.kt, project-model/.../Arrangement.kt.

## Demandes de Benjamin

1. Toutes les pistes dans les pads live (grille colonne = piste : Batterie, Acide, chaque synthé, chaque piste audio, Remix ; ligne = section). Pad = lance la section de cette piste à la mesure suivante ; ligne entière = toute la section ; couper une piste par colonne. Moteur : sections différentes par piste en même temps.
2. Effets sous les doigts pendant le live : pad XY, 4 macros, réverbe/écho communs, filtre, hachoir — par piste choisie ou global. (Il « s'éclate avec les effets réverbe pendant que c'est sur Play ».)
3. Bouton ● « Enregistrer la performance » : lancements, coupures et mouvements d'effets écrits dans le morceau (ordre des parties, coupures, automation). Ensuite on compose avec les pads live.
4. ACIDE en live : pouvoir tourner les potards de la 303 pendant le live (coupure, résonance, enveloppe, decay, accent, saturation) — GRANDS contrôles pensés pour les doigts (redessinés pour le live si besoin : gros potards ou curseurs larges, voire pad XY coupure/résonance), enregistrés comme automation de la 303 (cibles ajoutées en phase 26). Les potards actuels sont trop petits pour les doigts.
5. Défauts vus : pad XY, « ↓ Sec » et « ← Sombre » se superposent en bas à gauche ; libellé « Pad XY, haut-bas : » coupé à gauche dans la rangée de puces.
