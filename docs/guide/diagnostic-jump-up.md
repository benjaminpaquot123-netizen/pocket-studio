# Diagnostic : l'assistant pas à pas face au projet « Jump up DnB » de Benjamin

Sources : `benj-projets/jumpup1/project.json` (APK 181, `formatVersion` 1, `kitVersion` 3, `soundVersion` 2, `mixVersion` 2, `style` = `jump_up`) ; code de la branche `lot1` (`/home/user/ps-lot1`). Lignes citées sous la forme `Fichier.kt:L1-L2`.

Demande de Benjamin : « chaque étape de chaque piste scrutée et guidée : chaque son, quand, avec quel effet, combien de fois, si on veut l'utiliser ailleurs… le guide toujours présent, qui sait où on en est ». Son constat (« j'ai généré sans ajouter d'effet ») est en fait faux sur le fond : l'assistant **a** posé 25 effets, 2 bus, 10 courbes et un sidechain, mais **il ne lui a jamais dit un mot sur aucun d'eux, ni pendant, ni après**. Le problème est un problème de guidage, pas de moteur.

---

## 1. Ce que contient réellement son projet

### 1.1 Vue d'ensemble
- Style `jump_up`, 174 BPM, swing 0, tonalité Fa (`harmony.root` 5) **mineur** verrouillée (`scale: Minor`, `lock: true`). La recette demande phrygien pour l'ambiance Sombre (`StyleRecipes.kt:965`) : Benjamin a donc choisi une autre ambiance (Future → Minor, degrés 0-0-5-6) ou la gamme a été réécrite ; aucun écran ne le lui a dit.
- 8 voix : batterie (canal 0, 16 pads) + 7 pistes d'instrument : **Basse** (piste 0, canal 2), **Nappe** (5), **Lead** (6), **Sub** (7), **Basse 2** (8), **Souffle montant** (9), **Voix** (10), **Sub drop** (11). Correspondance canaux : `Tracks.kt:76`, `Project.kt:565-581`.
- 21 scènes : 8 sections (Intro, Montée, Drop, Break, Intro 2, Intro 3, Montée 2, Drop 2) + 2 « Crash » + 11 « Fill » d'une mesure (`DrumArrangement.withFillsAndCrashes`, `StarterWizard.kt:752`).
- 46 parties, **184 mesures ≈ 4 min 14** : I16 · L8 · D32 · B16 · L8 · D32 · B16 · L8 · D32 · O16. Les courbes d'automation font **216 points (= 216 mesures, un point par mesure, `Automation.kt:169-181`)** : le morceau généré faisait 216 mesures (`SongShape.kt:65`, `BUILD_LONG[0]` = I32…O32) et a été raccourci de 32 mesures (Intro et outro 32 → 16, sans doute via la fiche de partie). **À vérifier : les courbes n'ont pas suivi** (ex. le passage Basse → Basse 2 est écrit à la mesure 96, le Drop 2 commence maintenant à la 80 ; la coupure du Lead aux mesures 40-55 visait la première moitié du Drop 1, qui commence désormais à la 24).

### 1.2 Structure (parties → scènes)
| Mesures | Section | Scènes | Ce qui joue |
|---|---|---|---|
| 0-16 | Intro (3 paliers) | E, F, A | kick + toms (E) ; + perc (F) ; kick, charley, perc (A). Aucune piste mélodique. |
| 16-24 | Montée | B, G | caisse claire qui roule (B 24 coups → G 60), charley, Nappe (4 notes tenues), Souffle montant. Pas de kick. |
| 24-56 | Drop 1 | C (8) H (8) C (8) H (8) | tout : kick, caisse, clap, charley, perc, Basse, Sub, Basse 2 (muette par courbe), Nappe, Lead (6 notes sur 8 mesures), Sub drop (C) / Voix (H, 2 notes). Entre chaque bloc : 1 mesure Fill ou Crash. |
| 56-72 | Break | D | charley, Nappe, Souffle. |
| 72-80 | Montée | B, G | idem. |
| 80-112 | Drop 2 | C/H ×2 | idem Drop 1 ; Basse 2 remplace Basse à partir de la courbe (mesure 96). |
| 112-128 | Break | D | idem. |
| 128-136 | Montée | B, G | idem. |
| 136-168 | Drop 3 | C/H ×2 | idem. |
| 168-184 | Outro | A, F, E | l'intro à l'envers. |

Le Drop est joué 3 fois **à l'identique** (mêmes 16 mesures C+H ×2) : 96 mesures sur 184 sont la même boucle de 8 mesures, seule la basse change de son au Drop 2.

### 1.3 Piste par piste (effets : numéro = `EffectType.ordinal`, `Effects.kt:63-380`)
| Piste (canal) | Son | Notes | Chaîne d'inserts dans l'ordre (réglages 0..1) | Sends R/É | Automation | Joue dans |
|---|---|---|---|---|---|---|
| **Batterie** bus (0) | kit `dnb` : Kick 13, Caisse claire 7, Clap 7, Charley 4, Percu 8 ; pans charley −0,3, perc +0,5, (kick lane pan −1 : à vérifier) | 2 mes. (64 pas) + 11 fills + 2 crashs | 2 Compresseur (seuil .6, ratio .23, att .83, rel .44, RMS) · 3 Saturation (.15, ton ouvert) · 28 Écrêteur doux (.125) = `BUS_GLUE_V2` + clip (`WizardMix.kt:292-300`) | 0 / 0 | volume .8 → .85 sur les 4 dernières mesures de chaque Montée (`DRUMS_PEAK`) | partout sauf Break (charley seul) |
| pad Kick (16) | Kick 13 | | 2 Compresseur (.45,.45,.69,.3) · 3 Saturation (.25) = preset `punch` (`FxPresets.kt:87`) ; **clé du sidechain** | 0 | — | |
| pad Caisse (18) | Caisse claire 7 | | idem `punch` | **R .15** | — | |
| pad Crash (28, lane 12) | cymbale | 1 coup au 1 de chaque bloc | aucun | R .2 / É .25 ; vol .95 | — | |
| **Basse** (2) | synthé « Basse growl » (glide .4) | 28 notes / 4 mes., Fa2 (41) + quinte/octave, vél. .57-1.0 | 19 Distorsion (.55,.6,.8,.75) · 28 Écrêteur (.125) · 24 OTT (.75,.5,.75,.7) · 5 Filtre (passe-haut .11 ≈ 80 Hz) · 13 EQ param (.26,.58,.24) = preset `growl` + clip + passe-haut S4 ; **le PUMP de la recette a été retiré** au profit du bus Musique (`honest`) | 0 / 0 | volume .73 (Drop) / .8 (Montée ?) puis **0 à partir de la mesure 96** | Drops (Drop 2 : muette → Basse 2) |
| **Sub** (7) | synthé sinus, −12 | mêmes 28 notes une octave sous la basse | **aucun insert** | 0 | volume .73/.8 | Drops |
| **Basse 2** (8) | « Basse wobble » (`bassSwitch`, `StyleRecipes.kt:978`) | identiques à Basse | même chaîne que Basse (growl + clip + OTT + filtre + EQ) | 0 | volume **0 jusqu'à la mesure 96**, puis .8/.73 | Drop 2-3 |
| **Lead** (6) | synthé « Basse wobble » (la « 2e basse » du jump up, `tips` `StyleRecipes.kt:974`) | 6 notes / 8 mes. (Drop) ; 12 au Drop 2 (doublure −12) | 19 Distorsion (.4,.35,.3,.6) · 24 OTT (.5,.5,.6,.6) = `fat_bass` ; 4 Chorus (.3,.4,.4,.4) · 28 Écrêteur (.42) · 21 Ping-pong (.42,.35,.5,.25) = WideMix S4 | R .15 / É .15 | volume .8 → **0 mesures 40-55** → .8 (Drop 1A sans lead) ; **send réverbe .15 → .4 toutes les 8 mesures** (fin de phrase, `WideMix.phraseEndLane`) | Drops |
| **Nappe** (5) | sample de pack « Nappe 3 · Future bass » (engine 2) | 8 accords tenus d'une mesure (Fa-Fa-Ré♭-Mi♭) | 11 Élargisseur (.75,.35) · 5 Filtre passe-bas (coupure 1.0 au repos) · 28 Écrêteur (.25) | **R .35** | **coupure du filtre** : 1.0 partout, 0.25 au début de chaque Montée (`Sweep("pad", 1 → 0.25..1.0)`, `StyleRecipes.kt:986`) | Montée, Drop, Break |
| **Souffle montant** (9) | bruit (synth) | 1 note tenue | 5 Filtre (coupure 0 au repos, réso .2) · 28 Écrêteur (.25) | R .25 | coupure 0 → .6 sur les 8 mesures de Montée, 1.0 (impact) au 1 du Drop ; volume .8 → .85 | Montée (+ impact Drop) |
| **Voix** (10) | Plaits modèle 11 (voix robot, `VOCAL_SOUND`) | 2 notes (shout de MC, scène H) | aucun | R .2 / É .3 | — | Drop 2e moitié |
| **Sub drop** (11) | synthé | 2 notes (glissé 53 → 29) au 1 du Drop | aucun | 0 | — | Drop (scène C) |
| **Master** (4) | | | 24 OTT (.25) · 2 Compresseur (.6,.2,.85,.55) · 3 Saturation (.2) = recette + `MASTER_GLUE` (`WizardMix.kt:163-167`) ; `masterDb` −1 ; cible **AUTO** ; **graves en mono < 120 Hz** | | master volume (courbe 864 pts, à 1.0) | |
| Bus Réverbe | plate (type .5), taille .45, amorti .5, pré-délai .1, retour coupé < 200 Hz et > 9 kHz | | | | | |
| Bus Écho | 1/8, feedback .4, ping-pong 1, ton .6, retour 150 Hz-8 kHz | | | | | |
| Sidechain | clé = pad Kick (16) ; bus « Musique » = canaux 1, 2, 5-10 (acide, Basse, Nappe, Lead, Sub, Basse 2, Souffle, Voix) ; profondeur .12, graves .55, relâche .25 (`WizardMix.musicDuck`, `WizardMix.kt:274-280`) | | | | | |

Faders : **tous à 0,8 (0 dB)**, pan 0 partout (seuls les pads de batterie sont pannés). Le « mix » n'existe que par les courbes de volume et le master.

### 1.4 Ce qu'un jump up pro aurait, et l'écart (référence : `docs/hits/bass.md:371-437`, synthèse `:587-608`, `docs/PLAN-SON-PRO.md`)
Légende : ✅ présent · ⚠️ partiel · ❌ absent · 🔇 = jamais dit ni demandé à Benjamin.

| Piste | Son | Notes / motif | Effets (ordre, réglages) | Sends | Automation | Arrangement par partie | Niveau / pan | Jamais dit à l'utilisateur |
|---|---|---|---|---|---|---|---|---|
| Basse | ✅ growl choisi parmi la liste ; ⚠️ une seule couche médium (pro : 2 à 7 couches, womp + reese + sub) | ⚠️ question/réponse écrite (`bassPattern` 64 pas) mais **8 fois la même boucle de 4 mes. par Drop**, aucune variation 5-8, aucun « switch-up » toutes les 8-16 sauf Drop 2 | ✅ growl + OTT + clip + passe-haut 80 Hz ; ❌ distorsion 2 bandes, ❌ pas de PUMP visible (remplacé par le bus Musique, invisible dans sa tranche) | ✅ aucun send (juste) | ⚠️ volume seul ; ❌ aucune automation de synthé (coupure, LFO) ; ❌ wobble qui change de vitesse | ⚠️ coupée avant le Drop (Montée) ; ❌ 2 temps coupés fin de 16, ❌ Drop 1A basse+batterie seules (la Nappe joue partout) | ✅ 0 dB, centre | 🔇 tout : pourquoi growl, pourquoi OTT, ce qu'est un écrêteur, qu'il y a un sidechain, qu'une 2e basse existe et quand elle entre |
| Sub | ✅ sinus −12 | ✅ même MIDI −12 | ❌ **aucun insert** (pro : saturation légère 60-250 Hz, mono strict ; `WideMix.SUB_DRIVE` .4 annoncé mais absent de la tranche) | ✅ | ⚠️ volume | ✅ suit la basse | ✅ | 🔇 qu'une piste « Sub » existe et pourquoi |
| Basse 2 | ✅ wobble | ❌ mêmes notes exactement (pro : autre patch **et** réponse différente) | ✅ même chaîne | ✅ | ✅ fondu croisé Drop 2 (mais **décalé de 16 mes.** après le raccourcissement, §1.1) | ⚠️ | ✅ | 🔇 son existence (Benjamin voit 2 basses sans savoir pourquoi) |
| Lead (= 2e basse wobble) | ✅ | ⚠️ 6 notes / 8 mes. : la réponse est trop courte pour un dialogue (`bass.md:427-431`) | ✅ fat_bass + chorus + ping-pong + clip ; ⚠️ un chorus sur une basse wobble (mono < 250 Hz non garanti) | ✅ R/É .15 | ✅ throw de réverbe fin de phrase ; ✅ coupé Drop 1A | ⚠️ identique à chaque Drop | ✅ | 🔇 qu'il s'appelle « Lead » mais est une basse ; que la réverbe monte toute seule |
| Nappe | ✅ sample Future bass | ⚠️ triades tenues, 1 accord/mesure, 3 fois le même cycle | ✅ élargisseur + filtre + clip | ✅ R .35 | ✅ filtre qui s'ouvre dans la Montée | ❌ joue aussi dans tout le Drop (pro : absente du Drop 1A) ; ❌ aucune variation Break | ✅ | 🔇 le balayage de filtre, la réverbe à 35 % |
| Souffle | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | 🔇 (le SongGuide le mentionne seulement si on ouvre le guide) |
| Voix | ✅ voix robot | ❌ 2 notes « shout » en Drop 2 seulement ; pro : shouts de MC 1 fois / 8 mes., chops | ❌ aucun insert (pro : écho, filtre, bitcrush) | ✅ R .2 / É .3 | ❌ | ❌ | ✅ | 🔇 qu'il y a une piste Voix et qu'il peut y poser la sienne (`Sons › Voix`) |
| Sub drop | ✅ | ✅ | ❌ | ✅ | ❌ | ✅ | ✅ | 🔇 |
| Batterie | ✅ kit dnb, punch kick/caisse | ⚠️ 2 mesures + 11 fills ; ❌ caisse claire 1 couche + clap identique (pro : 3 couches, ghosts) ; ❌ aucune variante A/B de boucle entre Drop 1, 2, 3 | ✅ bus comp + sat + clip ; ✅ punch pads | ✅ caisse R .15 | ⚠️ volume +0,5 dB fin de Montée | ✅ Montée sans kick, fills, crashs | ✅ | 🔇 tout (effets de pads invisibles dans la console principale) |
| Master | — | — | ✅ OTT .25 + comp + sat + limiteur ; cible AUTO (−7 LUFS plafonné, `bass.md:52`) | — | ✅ courbe de volume (plate) | ⚠️ Drop − Montée mesuré **−2,1 LU** sur ce style au banc (`bass.md:52`) | ✅ −1 dB | 🔇 cible de volume, mono bass, qu'il existe un « Prêt à partager ? » |

Conclusion §1 : la **chaîne de production est quasi complète côté machine**, avec trois trous musicaux (basse qui tourne 8× sans switch, aucune automation de synthé, Nappe partout) et un trou de guidage **total** : rien dans l'appli ne décrit à Benjamin ce qui a été posé, pourquoi, ni ce qu'il reste à faire piste par piste.

---

## 2. Inventaire du guidage actuel

### 2.1 Les surfaces, ce qu'elles disent, ce qu'elles savent
| Surface | Fichiers | Ce qu'elle guide | État connu | Persistant ? | Lit le projet ? |
|---|---|---|---|---|---|
| **Assistant « Nouveau morceau, pas à pas »** | `StarterWizard.kt:523-540` (étapes : Style, Ambiance, une par ligne de batterie, Acide, une par partie Basse/Nappe/Lead, Résumé), `WizardScreen.kt:158-862` | choix du style (description, 10 références), ambiance (gamme + accords, écoute), **son** de chaque ligne et partie (écoute « avec le rythme »), 303, résumé (tempo, parties, sections, durée). Textes : `WizardScreen.kt:241, 383, 501, 659-660, 701-708, 750-862`. Astuce par partie : `Melody.tips` (`StyleRecipes.kt:974`). | l'étape courante en mémoire (`ui.step`) | **non** : fermer = « Tes choix seront perdus » (`WizardScreen.kt:173-174`) | non (crée le projet, puis disparaît) |
| **« Ton morceau est prêt »** (bandeau) | `SongReady.kt:188-271` | Écouter · Changer un son · Modifier le morceau · Partager · « Lire le guide de ton morceau » | — | se ferme ; ne revient pas | non |
| **Guide du morceau** (5 pages) | `SongGuide.kt:38-119` | les sections et ce que chacune ajoute, ▶ / ⟳ Partie, Rythme/Synthé/Acide, « petits plus » (fills, Souffle, contre-chant, coupure avant le Drop), « pour aller plus loin » (filtres qui bougent, voix). **Seule mention des effets : une ligne « Filtres : certains sons s'ouvrent et se ferment tout seuls (Mixeur, Automation) »** (`SongGuide.kt:111`). | — | `rememberSaveable` le temps de l'activité (`SongGuide.kt:7-9`) ; perdu ensuite | non (texte généré une fois à partir du `ScenePlan`) |
| **« Prochaine étape »** (bandeau 1 ligne + carte) | `NextSteps.kt:12-140`, `NextStepStrip.kt:48-60`, `NextStepCard.kt:48-86` | 8 à 10 étapes : Rythme, Basse, Mélodie, Parties, Morceau, puis pour un morceau fait par l'assistant : Écoute, Un son, **Volumes**, Voix, Partager. Textes `NextSteps.kt:110-133`. | fait/pas fait **calculé sur le projet** (coups, notes graves/aiguës, parties nommées, ≥ 3 parties, clip audio, **fader ≠ 0,8 ou pan ≠ 0** = « volumes réglés ») + coches manuelles | **oui** : SharedPreferences `next_steps` par projet (fermé, coches) | oui, mais grossièrement (jamais les effets, l'automation, le master, la structure interne) |
| **Missions « Mon premier morceau »** (phase 36) | `Missions.kt:10-62, 65-115, 122-127, 185-198`, `Spotlight.kt:4-45` | 6 missions : Style, Écoute, Change un son, Enregistre une mélodie, Arrange (Fill/Break/Montée), Partage ; chacune pointe un contrôle réel (`targets`) | étape courante, « signature » du projet au départ de l'étape (hash des sons / nb de notes / `song`+`automation.size`), étapes passées | **oui** : SharedPreferences `missions` (global, **pas par projet**) | oui : « fait » = le projet a changé depuis le début de l'étape (`missionDone`) |
| **Aide « ? »** | `Glossary.kt` (76 entrées : INSERT, SEND, REVERB, DELAY, COMPRESSOR, EQ, MACROS, FX_PRESET, AUTOMATION, MASTER, LIMITER, LUFS…) | définition d'un mot, « ▶ Écouter un exemple » quand `SoundGlossary` a le mot | — | — | non |
| **Lexique « Les mots de la musique »** | `SoundGlossary.kt` (35 mots : kick…drop, sidechain, reverb, echo, filter, compressor, saturation, automation, mix, master), `GlossaryPage.kt`, `LearnLayer.kt`, `LearnViewModel.kt` | explication + exemple **sans/avec** rendu par un moteur privé | — | — | non (exemples génériques, pas sur SON morceau) |
| **Guide des premiers pas** | `GuideDialog.kt:37-85` | barre du haut, espaces, 3 gestes, « et ensuite » | page | non | non |
| **Accueil / « Que veux-tu faire ? »** | `Welcome.kt:93-184`, `LaunchChoice.kt:249-350` | Continuer, Nouveau, Mes projets, Missions, Mode simple/complet | — | — | affiche durée + nb de parties |
| **Fiche de partie** | `PartSheet.kt:1-40, 168-180, 236-280`, `PartIntents.kt:3-9` | « Une intention, un bouton » : Variation, Fill, Break 4/8, Montée 4/8, fondus, Fin, **Énergie 1-4**, longueur, « Ce qui joue », dupliquer… ; dit « Fait : … » après coup (`PartSheet.kt:116-125`) | — | — | oui (ce que la partie contient : jouée n fois, finit sur un fill, énergie) |
| **« Ce qui joue »** | `WhatActs.kt:3-8, 77-100`, `WhatPlays.kt:81-100` | **inventaire fidèle** de tout ce qui agit (effets, sends, macros, courbes avec leur forme en %, master), par partie ou pour tout le morceau, avec le lien vers l'endroit où ça se règle | — | — | **oui, exhaustif** ; mais il décrit, il ne juge ni ne recommande |
| **Console / effets** | `MixerScreen.kt`, `FxPresets.suggestedFor` (`FxPresets.kt:122-139`) | presets conseillés par type de son (BASS : fat_bass, growl, wob…) | — | — | par type de son seulement |
| **« Prêt à partager ? »** | `ShareSheet.kt:158-215` | 6 vérifications : solo oublié, pistes muettes, rien à entendre, pas de morceau, trop court (< 30 s), saturation/true peak | — | — | oui (mais rien de musical) |
| **Changelog / recette** | `assets/changelog.txt`, `recette.json` | — | — | — | — |

### 2.2 Les écarts avec la demande de Benjamin

**(a) Couverture : guidé ↔ silencieux**
- Guidé (par l'assistant) : style, ambiance, **le son** de chaque ligne et partie, garder ou non la 303. Guidé après coup (NextSteps/Missions) : écouter, changer un son, enregistrer, Fill/Break/Montée, volumes « en gros », partager.
- **Silencieux** (fait par la machine, jamais montré ni proposé) : effets par piste (lesquels, pourquoi, dans quel ordre, réglés comment), les 2 sends et leurs bus, le sidechain et le bus Musique, les 10 courbes d'automation, la Basse 2 et son fondu, la coupure du Lead au Drop 1A, le master (OTT, cible, mono bass), la balance (faders tous à 0 dB), le pan (0 partout), la variation entre Drop 1/2/3 (aucune), la réutilisation d'un son ailleurs (« si on veut l'utiliser ailleurs » : aucune notion de « ce son dans une autre partie »), l'export (hors « Prêt à partager »). Le `SongGuide` dit « Filtres : certains sons s'ouvrent… » et c'est tout.
- Le `StyleFx` (`StarterWizard.kt:135-157`) et `withEffects` (`:1307-1341`) posent les effets **sans aucune étape dans `steps()`** (`:536-540`) : l'étape « effets » n'existe pas dans l'assistant, ni une étape « mix », ni « automation », ni « arrangement ». Le résumé (`WizardScreen.kt:620-681`) ne liste que tempo, sons et sections.

**(b) Persistance : reprendre après des heures ou des jours**
- L'assistant ne se reprend pas (`WizardScreen.kt:173`). Le `SongGuide` meurt avec l'activité. `SongReady` disparaît.
- `NextStepPrefs` (par projet) et `MissionPrefs` (global : **changer de projet garde la mission en cours**) sont les deux seuls états persistants. Aucun des deux ne sait : ce que l'utilisateur a **vu**, ce qu'il a **sauté volontairement**, ce qu'il a **décidé** (ex. « pas de réverbe sur la basse, c'est voulu »), ni **où il en est** piste par piste.
- Dans le projet (`Project.kt:18-145`) il n'y a aucun champ pour un état de guide ; `style`, `kitVersion`, `soundVersion`, `mixVersion` disent juste « fait par l'assistant, version N ».
- Après des jours : « Continuer » rouvre le projet, la bande « Prochaine étape » dit « 7/10 · Règle les volumes » si rien n'a bougé. C'est le seul « où j'en suis ».

**(c) Explication : le pourquoi et l'avant/après**
- Pourquoi : les `tips` par partie (`StyleRecipes.kt:974`) et `Line.tip` (`:959-963`) expliquent le **rôle musical** du son dans le style, pas le traitement. Les presets ont une phrase (`FxPresets.kt:115` « Une basse qui grogne… »), jamais montrée dans un parcours.
- Avant/après : seul le lexique (`SoundGlossary`, « Sans / Avec ») le fait, sur des exemples génériques. Rien ne joue **sa** basse sans et avec son OTT. Le bypass existe dans le modèle (`Insert.bypass`, `Effects.kt:21-29`) : la mécanique d'A/B est là, pas l'écran.
- La fiche de partie dit « Fait : … » (bon réflexe) mais pas « écoute la différence ».

**(d) Adaptativité : regarder le projet ou suivre un script**
- `nextSteps()` lit le projet mais avec des critères binaires (il y a des coups, une piste grave, ≥ 3 parties, un fader bougé). Il ne détecte **aucun** des manques du §1.4 : Nappe partout, basse sans variation, Sub sans saturation, Voix sans effet, courbes décalées, 3 Drops identiques, aucune piste pannée, faders tous égaux.
- `missionDone` ne regarde que « quelque chose a changé » (hash).
- `shareChecks` : 6 règles techniques, aucune musicale.
- `WhatActs` est le seul analyseur exhaustif, et il ne dit jamais « il manque ».
- Les analyseurs qui **jugent** existent côté générateur mais ne sont branchés sur aucun écran : `MelodyRules`/`MelodyMetrics` M1-M20, les métriques du banc (Drop − Montée, 60-250 Hz, côté/centre), `DrumHumanizer.Feel`.

---

## 3. Points d'accroche techniques pour un guide persistant et conscient du projet

**Où ranger l'état du guide**
- `Project` (`Project.kt:18-145`) : ajouter un champ `@EncodeDefault(NEVER) guide: GuideState? = null` à côté de `songVersions` (`:120`) ; le format JSON est tolérant (`ProjectJson.kt`), l'autosave est gratuit (`ProjectSession.kt:1-25`, 400 ms). Modèle minimal : par piste × étape (son, notes, effets, sends, automation, arrangement, niveau) un statut `vu / fait / sauté(raison) / à revoir`, le dernier écran, la date, les décisions libres (« pas de réverbe ici »).
- Alternative sans toucher au format : SharedPreferences par projet comme `NextStepPrefs` (`NextStepCard.kt:48-86`) : perdu à la copie/export du projet, donc la première option est préférable.
- `style` + `mixVersion`/`soundVersion` (`Project.kt:88-114`) disent quelle recette a produit le projet → le guide peut recharger la `Style` (`StarterWizard.style(id)`, `:583`) et comparer ce qui **devrait** être là (`effects`, `sweeps`, `breakWith`, `tips`) à ce qui **est** là.

**Analyseurs déjà écrits (Kotlin pur, testables sur Linux)**
- `Project.whatActs(scene)` + `AutomationLane.shapeIn` (`WhatActs.kt:77-130`) : inventaire effets / sends / macros / courbes par partie, avec forme (monte, descend, bouge…) — base d'un « état des lieux » par piste.
- `MelodyMetrics` M1-M20 + `MelodyRules` (`MelodyMetrics.kt`, `MelodyRules.kt`) : juge un `NoteClip` (phrases, motif, silences, répétitions identiques) → détecte « ta basse rejoue 8 fois la même mesure ».
- `DrumHumanizer.Feel` et `DrumArrangement` : savent ce qu'un beat vivant contient (ghosts, variantes A/B) → comparer scène C et H.
- `ChordTrack.guess` (`ChordTrack.kt`) : les accords réels d'une scène → dire « Nappe et Basse ne sont pas sur le même accord ».
- `StarterWizard.songSections` (`StarterWizard.kt:798`) et `SongShape` : sections nommées → « Drop 1, 2 et 3 sont identiques ».
- `FxPresets.suggestedFor(kind, ch)` (`FxPresets.kt:122-139`) + `Buses`/`WideMix` constantes : quoi proposer par type de son et ce que le style attend.
- `shareLevels`/`shareChecks` (`ShareSheet.kt:140-215`) + `LoudnessTarget(s)` + mesure LUFS/true peak du moteur (`NativeEngine.LOUDNESS_*`) : les chiffres du master. Le banc (`docs/son/banc`, scripts hors appli) mesure Drop − Montée et 60-250 Hz : à porter en rendu hors-ligne dans l'appli (`OfflineExport.renderToArray`, déjà utilisé par le lexique, `LearnViewModel.kt:112-140`) pour un vrai avant/après sur **son** morceau.
- `Mixer.longFxOverLimit` (`Effects.kt:434`) : garde-fou existant sur le nombre d'échos/réverbes.

**Flux d'événements / historique**
- `History<T>` (`History.kt`) : annuler illimité avec `tag` par pas (`lastTag`) → le guide peut étiqueter ses propres actions (« effet posé par le guide ») et savoir si l'utilisateur les a défaites.
- `ProjectSession.project: StateFlow<Project>` : tout changement observable (c'est ce que `MissionWatcher` utilise, `Missions.kt:185-198`) → une « signature » par piste × étape pour marquer « fait » sans toucher d'OK.
- `AppLog` (`AppLog.kt`, 200 lignes, `persistTo`) : journal texte des actions (lecture, export, sauvegarde) joint aux rapports ; pas structuré, mais une source de « quand a-t-il écouté en entier ».
- `SongVersion` (`SongVersions.kt`) : versions du morceau mises de côté (avant une prise Live, restauration) → mécanisme réutilisable pour « avant/après » d'un chantier de guide.
- `PlayedMemory`, `TakeJournalStore` : ce qui a été joué/enregistré, utile pour « tu as joué sans enregistrer ».
- `SpotRegistry` (`Spotlight.kt`) : pointer le contrôle exact (ex. `track_sound_6`, `tab_Structure`) depuis une étape du guide — déjà fait par les missions.
- Chaîne d'affichage prête : `NextStepStrip` sur tous les écrans de fabrication (`STRIP_TABS`, `NextStepStrip.kt:48`) ; `PartSheet` et son « Fait : … » ; `WhatPlays` pour le détail.

**Manques techniques à combler pour l'avant/après**
- Un rendu hors-ligne court (8 mesures) d'une partie avec un `Insert.bypass` basculé, puis lecture via `PcmPlayer` : tout existe séparément (`LearnViewModel.render`), pas assemblé pour le projet ouvert.
- Un « diff » projet ↔ recette du style (effets attendus par `StyleFx`, sweeps, `breakWith`) : fonction pure à écrire à côté de `withEffects`.
- Recaler les courbes (`songValues`, un point par mesure) quand une partie est raccourcie : à vérifier dans `PartIntents.withPartBars` / `lengthenPart` (`PartIntents.kt:436-452`) — symptôme §1.1.

---

## 4. Rappel : numéros d'effet → noms (`Effects.kt:63-380`)
1 Égaliseur · 2 Compresseur · 3 Saturation · 4 Chorus · 5 Filtre · 6 Pompe · 7 Flanger · 8 Phaser · 9 Noise gate · 10 Trémolo · 11 Élargisseur · 12 Bitcrusher · 13 EQ paramétrique · 14 Robot · 15 Wah-wah · 16 Vieux vinyle · 17 Talkie-walkie · 18 Hachoir · 19 Distorsion · 20 Écho · 21 Écho ping-pong · 22 Réverbe · 23 Auto-pan · 24 OTT multibande · 25 Décalage de fréquence · 26 Arrêt de bande · 27 Wobble · 28 Écrêteur doux.
Cibles d'automation (`Automation.kt:60-72`) : 0 volume · 1 pan · 2 send · 3 paramètre d'effet · 4 bus · 5 303 · 6 macro · 7 Live · 8 synthé · 9 Plaits.
