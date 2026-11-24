# Grand remplacement : rien d'oublié, et comment remplacer tous les sons

Date : 2026-11-24. Rôle : lecteur-trieur (aucun code de l'appli modifié).
Code lu : tête `3d0bb823`. Mesures : `docs/son/banc/apres-S7.json` (37 styles), sans nouveau rendu.

## 1. En une page

1. **Le banc est tout vert, et Benjamin entend quand même « rétro » et « sans corps ».**
   Volume (−8,5 LUFS), largeur (−11,7 dB), contraste Drop/Break (+5 LU), perte sur téléphone (3,8 LU) :
   tout est dans la cible (`docs/son/banc/S7.md`). Donc ce qui manque **n'est pas ce que le banc mesure**.
2. **Ce qui manque, c'est la matière.** Presque tout ce qu'on entend est fabriqué par un synthé
   (kit de batterie synthétique, basses et nappes `SynthPatch`/Plaits) ou par une IA de petits sons
   (`scripts/packs/styles.json`, Stable Audio Open Small). Aucun vrai échantillon de production
   (`docs/plan/son.md` §1.2, cause n° 1). Le « rétro » vient de là : ça sonne boîte à rythmes.
3. **Changer un clap ne change rien au corps.** Un clap, c'est deux coups par mesure et moins de 1 %
   de ce qu'on entend. Le corps vient de ce qui joue **tout le temps** : la basse, le lit d'accords,
   les aigus qui roulent (charleys, shakers, rides en boucle), une texture de fond, et les transitions.
   C'est exactement là que l'appli est la plus pauvre (voir §3).
4. **Les packs de style ne contiennent que batterie + basse.** Pas de nappe, pas d'accords, pas de
   voix, pas de FX, pas de crash/ride/shaker propres au style (ex. trap = kick, snare, clap, hat,
   open_hat, perc, 808 ; boom bap = kick, snare, hat, open_hat, perc, bass). Tout le reste retombe
   sur les mêmes 6 à 8 sons de synthé partagés par tous les styles (« Nappe », « Cordes », « Piano FM »,
   « Cloche FM », « Pluck »…). D'où « c'est fort la même chose ».
5. **Trap et boom bap sont les plus maigres** (preuve en §2) : 4 ou 5 lignes de batterie, un trou
   dans le médium, pas d'air, et pour le boom bap ni vrai break de batterie ni échantillon « volé à un
   vinyle », alors que le style n'est que ça.
6. **Les doigts ont plu parce que c'était un vrai son enregistré**, dans un rôle neuf. C'est la
   leçon : de vrais enregistrements, dans des rôles qui manquaient, pas un clap synthétique de plus.

## 2. Pourquoi trap et boom bap « ne ressemblent pas à de la musique »

Bandes mesurées au Drop (dB par rapport au total), cible club entre parenthèses.

| Style | Lignes de batterie | Médium (−10/−12) | Haut-médium (−14/−16) | Air (−16/−20) | Ce qu'on entend |
|---|---|---|---|---|---|
| trap | 4 (kick, clap, charley, charley ouvert) | −10,3 | −15,3 | −18,6 | une 808, une cloche, des cordes de synthé : pas de snare, pas de perc, pas de voix |
| boom bap | 5 | **−16,3** (le plus creux des 37) | −16,0 | −19,2 | un piano électrique de synthé et une basse : pas de sample, pas de break, pas de vinyle |
| lofi | 4 | −15,0 | −17,8 | **−22,2** | pareil, sans grain |
| tech house | 7 | −8,1 | −15,6 | −15,3 | correct en mesures ; un orgue de synthé et des charleys de machine |
| amapiano | 5 | −9,7 | −16,5 | −18,4 | sub à **−15,8** (médiane −5,6) : le log drum n'a pas de fond |
| reggaeton | 6 | −8,7 | −12,4 | −16,0 | équilibré ; il manque guitare, voix, ambiance |

Lecture : le boom bap et le lofi ont un trou de 5 dB là où vit la musique (piano, voix, sample).
Le trap n'a que 4 lignes rythmiques quand un morceau du commerce en empile 8 à 12 (snare + clap
doublés, rimshot, percs, charleys en deux couches, voix « hey », FX inversés).
Sources : `scripts/packs/recipes/hiphop.json` (lignes `drums` de `trap`, `boom_bap`, `lofi`),
`docs/son/banc/apres-S7.json` (`styles.<id>.bands`).

## 3. Inventaire « rien d'oublié »

Priorité : **P1** = change le corps tout de suite ; **P2** = change la couleur (« moderne ») ;
**P3** = finition. Qualité : estimation à la lecture du code et des mesures, pas à l'oreille.

### 3.1 Sons

| # | Élément | Présent ? | Preuve | Qualité | Priorité |
|---|---|---|---|---|---|
| S1 | Boucles d'aigus (top loops : charleys + shaker + ride joués par un humain) | **Non** | aucun `kind` de boucle dans `scripts/packs/styles.json` | absent | **P1** |
| S2 | Shaker, tambourin, cabasa dans les styles | Sons présents (pack « percussions acoustiques », `scripts/packs/libraries.json`), **jamais utilisés par une recette** | recettes : rôles `perc`/`perc2` seulement | bons mais dormants | **P1** |
| S3 | Textures de fond (bruit de vinyle, salle, foule, pluie, souffle de bande) sous tout le morceau | Presque non | `SoundLibrary.kt` : « Pluie », « Vent », « Souffle » (synthé) ; effet VINYL `Effects.kt:235` ; aucune piste de fond dans `StarterWizard.kt` | faible | **P1** |
| S4 | Breaks de batterie échantillonnés (boom bap, jungle, dnb) | **Non** | packs `boom_bap`, `jungle` : one-shots seuls | absent | **P1** (pour ces styles) |
| S5 | Vrais kits de batterie de production par style (kick, snare, clap, charleys en couches) | Non : synthèse + IA | `engine/src/main/cpp/core/DrumKit.cpp` ; `styles.json` (IA) ; `docs/plan/son.md` §1.2 | kicks validés (F0), le reste « rétro » | **P1** |
| S6 | Montées, impacts, descentes, sub drops échantillonnés | Non : bruit blanc filtré | `StarterWizard.kt:1026` (`RISER_TRACK`), `:1035` (`RISER_SOUND`), `:1088`, `:1169` ; pack IA « fx » existe mais n'alimente pas l'assistant | faible, identique partout | **P1** |
| S7 | Accords/nappes propres à chaque style (stabs house, pianos M1, cordes trap, Rhodes réel) | Non : 4 nappes et 2 claviers pour 37 styles | `SoundLibrary.kt` (Nappe, Nappe chaude, Accords future bass, Cordes ; Orgue, Piano électrique) | mono-couleur | **P1** |
| S8 | Voix (chops, « hey », adlibs, phrases) | Partiel | pack IA `vocal_chops` ; `vocal` posé dans 13 recettes sur 37, aucune en boom bap, drill, reggaeton, amapiano | IA, peu crédible | **P1** |
| S9 | Crash, ride, cymbale inversée par style | Partiel : cymbale du kit synthétique | `StarterWizard.kt:1180` (`withCrash`) ; `ride` dans 11 recettes | synthétique | P2 |
| S10 | Claquements de doigts, rimshot, clave dans les styles | Doigts : oui depuis peu, hors recettes ; rimshot : non | `libraries.json` (règle `oga-snaps`, « Claquement de doigts ») | bon (validé par Benjamin) | P2 |
| S11 | Basse en deux couches (sub propre + couche médium saturée) | Oui | `StarterWizard.kt:729-744` (`withSubTrack`, `withBassOverSub`) ; `BASS_EXCITER` `Effects.kt:360` | bon ; amapiano sans fond | P2 |
| S12 | 808 moderne (longue, glissée, saturée) | Oui | `Track808.h` ; packs `trap`, `drill`, `modern_808` | bon en mesure | P3 |
| S13 | Guitare (reggaeton, afrobeats, pop, lofi, trap) | Seulement SoundFont GM | `disco` : `gm:Electric Guitar (muted)` ; rien dans `scripts/packs/instruments.json` | faible | P2 |
| S14 | Basse électrique réelle (disco, boom bap, funk) | SoundFont GM | `instruments.json` sans basse ; Karoryfer Meatbass prévu, pas fait (`docs/plan/son.md` §3.2) | faible | P2 |
| S15 | Piano électrique réel (Rhodes/Wurli), chœurs | Non (FM) | `instruments.json` : pianos, cordes, cuivres, bois, harpe seulement | moyen | P2 |
| S16 | Petits détails (« ear candy ») : coups inversés, blips, écho jeté sur un mot | Non | aucune génération dans `StarterWizard.kt` hors fills et crash | absent | P2 |
| S17 | Leads modernes (pluck large, supersaw en couches, cloche numérique) | Oui, refaits le 20 nov. | `docs/son/banc/apres-leads-1.md` | correct | P3 |
| S18 | Percussions du monde (conga, bongo, djembé) pour afro/amapiano/reggaeton | Sons présents (VCSL), peu câblés | `libraries.json` pack `percussions_acoustiques` | bons, dormants | P2 |

### 3.2 Effets et traitements

| # | Élément | Présent ? | Preuve | Priorité |
|---|---|---|---|---|
| E1 | Sidechain vrai (la basse s'efface sous le kick) | Oui | `engine/src/main/cpp/core/Effects.h:193`, `Engine.h:177` ; `Effects.kt:134` (PUMP) | fait |
| E2 | Saturation, distorsion, écrêteur | Oui | `Effects.kt:101`, `:258`, `:344` | fait |
| E3 | OTT / compression multibande | Oui | `Effects.kt:302` | fait |
| E4 | Compression de bus et colle | Oui | `WizardMix.withGlue` (`StarterWizard.kt:751`) | fait |
| E5 | Chaîne de master (écrêteur + limiteur vrai-crête) | Oui | `engine/src/main/cpp/core/MasterChain.h:17-27` | fait |
| E6 | Réverbe et écho en envoi | Oui, **une seule réverbe et un seul écho** | `PlateReverb.cpp` ; `docs/plan/son.md` §1.2 cause 5 | **P1** : une petite salle courte en plus de la grande |
| E7 | Filtres automatisés, balayages | Oui | `Effects.kt:123` ; `StarterWizard.kt:974-992` | fait |
| E8 | Largeur stéréo | Oui | `Effects.kt:180` (WIDENER) | fait |
| E9 | Arrêt de bande, hacheur, bitcrusher, vinyle, radio | Oui | `Effects.kt:320`, `:247`, `:191`, `:235`, `:241` | fait |
| E10 | **Sculpteur d'attaque (transient shaper)** | **Non** | aucun résultat dans `Effects.kt` ni `engine/src/main/cpp/core/*.h` | **P1** : c'est lui qui rend une batterie « moderne et sèche » |
| E11 | **Compression parallèle de la batterie** (la batterie doublée, écrasée, mélangée dessous) | **Non** | `withGlue` : un seul compresseur en série | **P1** : donne du poids sans tuer l'attaque |
| E12 | Vocodeur vrai, changement de hauteur, autotune, formants | Non (seulement « Robot ») | `Effects.kt:213` | P3 |
| E13 | Réverbe scintillante, granulaire | Non | aucun résultat | P3 |
| E14 | Réverbe inversée / queue coupée (gated) sur snare et clap | Non | `GATE` existe (`Effects.kt:158`) mais pas de préréglage | P2 |
| E15 | Égalisation dynamique / chasse au masquage (la nappe se creuse sous la voix ou le lead) | Non | — | P2 |

### 3.3 Gestes d'arrangement

| # | Geste | Présent ? | Preuve | Priorité |
|---|---|---|---|---|
| A1 | Montée + impact + Drop 2 | Oui | `StarterWizard.kt:753-765`, `SongShape` | fait (sons à remplacer : S6) |
| A2 | Roulements et relances de batterie | Oui | `DrumArrangement.withFillsAndCrashes` (`:797`) | fait |
| A3 | Contre-mélodie | Écrite dans **7 recettes sur 37** | champ `counter` des recettes | **P1** : la généraliser |
| A4 | **Lit d'aigus continu** au Drop (shaker ou ride en doubles croches, bas dans le mix) | Rare | `ride` dans 11 recettes, shaker dans 0 | **P1** |
| A5 | **Doublage** : clap + snare + doigts ensemble, kick en deux couches (attaque + corps) | Non | une ligne = un son | **P1** |
| A6 | **Accords doublés à l'octave** et note tenue aiguë au Drop (remplissage du médium) | Non | `chords` : une seule piste, une octave | **P1** (bouche le trou du boom bap et du trap) |
| A7 | Texture de fond du début à la fin | Non | voir S3 | **P1** |
| A8 | Vélocités et micro-décalages humains | Partiel | `DrumHumanizer` ; `docs/plan/son.md` cause 7 | P2 |
| A9 | Variation toutes les 4 ou 8 mesures (une note changée, un coup retiré, un filtre) | Partiel | fills seulement | P2 |
| A10 | Appel-réponse voix / lead | 6 recettes (`vocal: answer`) | `StarterWizard.kt:216`, `:299` | P2 |
| A11 | Coupure d'une mesure avant le Drop (silence, ou seule la voix) | À vérifier à l'écoute | `RISER_BARS = 4` (`:1043`) | P2 |

## 4. Les 10 manques les plus importants

1. Pas de lit d'aigus continu : ni boucles de charleys, ni shaker dans une seule recette (S1, S2, A4).
2. Pas de texture de fond sous le morceau : vinyle, salle, souffle (S3, A7).
3. Batteries hors kick encore synthétiques ou IA : il faut de vrais kits par style, en couches (S5, A5).
4. Accords et nappes : 6 sons de synthé pour 37 styles, sur une seule octave (S7, A6).
5. Boom bap, lofi, jungle, dnb sans vrai break ni sample au grain vinyle : médium à −16 dB (S4).
6. Trap à 4 lignes de batterie : ni snare, ni perc, ni voix, ni doublage (A5, S8).
7. Pas de sculpteur d'attaque ni de compression parallèle sur la batterie (E10, E11).
8. Montées, impacts et descentes : le même bruit blanc filtré partout (S6).
9. Voix absentes de 24 styles sur 37, et celles qui existent sont générées par IA (S8).
10. Une seule réverbe : pas de petite salle courte pour coller la batterie et les claviers (E6).

Viennent ensuite : guitare et basse électrique réelles (S13, S14), contre-mélodie partout (A3),
sub de l'amapiano (S11), détails d'oreille (S16).

## 5. Méthode pour aller vite

### 5.1 Sources permises

| Source | Licence | Pour quoi | Remarque |
|---|---|---|---|
| Freesound | **CC0 et CC-BY 4.0 seulement**, jamais NC, jamais « Sampling+ » | one-shots, boucles d'aigus, textures, FX, voix | filtre de licence dans la requête de l'API **et** revérifié sur chaque fiche avant d'accepter |
| Sonic Pi samples | CC0 | breaks, percussions, ambiances | `docs/plan/son.md` §3.2 |
| Virtuosity Drums, VSCO-2 CE, VCSL | CC0 | batterie acoustique, orchestre, percussions | VCSL et VSCO déjà là |
| Karoryfer Meatbass | CC0 | basse électrique | prévu, pas fait |
| Synthèse maison | à nous | sub, 808, kicks (validés), montées en couches | garder ce que Benjamin a validé |

Écartés : jRhodes3d (NC), 99Sounds, tout pack « royalty free » sans licence écrite.
Les sons IA (`styles.json`) passent en second choix : on ne les supprime pas, on ne les met plus en premier.

Règle du dépôt public : **aucun fichier audio dans git.** Git ne garde que la liste (identifiant,
auteur, adresse, licence, somme de contrôle) et le script ; les sons vivent dans les packs
téléchargeables, comme aujourd'hui. Chaque son CC-BY apparaît dans `THIRD_PARTY.md` et dans
l'écran des crédits. La clé Freesound est un secret GitHub, jamais dans le dépôt.

### 5.2 La chaîne (à écrire une fois, lot 0)

1. `scripts/packs/freesound.json` : par famille et par style, la requête, le filtre (licence, durée,
   fréquence d'échantillonnage ≥ 44,1 kHz, pas de mono 8 bits), et le nombre voulu.
2. `scripts/packs/freesound_pack.py` : télécharge 20 à 30 candidats par case, les normalise, coupe les
   silences, mesure (crête, durée, centre spectral, bruit de fond) avec `sound_character.py`, garde les
   5 meilleurs par `rank_sounds.py`, écrit la liste des crédits.
3. Tri à l'oreille **dans un morceau**, jamais son par son : le script rend le Drop du style avec
   chaque candidat (`scripts/song-bench.sh`), et on ne garde que ce qui passe le banc.
4. Un seul changement de recette par style à la fin (`scripts/packs/recipes/*.json` →
   `recipes_to_kotlin.py` → `StyleRecipes.kt`).

### 5.3 Pourquoi par style entier et pas par famille de sons

L'écoute d'aujourd'hui l'a montré : un clap neuf dans un morceau inchangé ne s'entend pas, ou s'entend
« pas mieux ». Donc on livre **un style complet à la fois** (sons + couches + arrangement), et on le
fait écouter en entier. Les familles (lot 0 à 2) ne sont que la préparation commune.

### 5.4 Critère d'acceptation (l'oreille de Benjamin)

- Deux MP3 du **même morceau entier** (mêmes notes, même tempo) : avant et après.
- **Même volume** : les deux ramenés à la même mesure LUFS, sinon le plus fort gagne toujours.
- Ordre caché (A et B tirés au sort), 3 styles par envoi au plus.
- Benjamin répond par style : « B mieux », « pareil », « A mieux ». **« Pareil » = refusé.**
- Phrase d'acceptation : « on dirait un morceau qu'on entend en soirée ou en playlist, pas une démo ».
- En plus, une écoute à côté d'un vrai morceau du style (référence des recettes), au même volume,
  sans rien livrer de ce morceau dans le dépôt.
- Les mesures du banc restent un garde-fou (rien ne doit passer au rouge), plus un critère de réussite.

Trois mesures à ajouter au banc pour suivre « le corps » (elles manquent aujourd'hui) :
nombre de pistes qui sonnent en même temps au Drop ; part du temps où l'aigu (> 6 kHz) est occupé
(lit continu ou coups isolés) ; niveau du fond entre deux coups (une texture le remonte).

## 6. Plan de lots

Boost éco : lots pleins, un fichier = un lot ouvert. Les lots 3 à 7 touchent des fichiers de recettes
différents et peuvent tourner ensemble ; `StarterWizard.kt` n'appartient qu'au lot 1.

| Lot | Contenu | Fichiers touchés | Preuve attendue |
|---|---|---|---|
| **0. Chaîne Freesound** | script de collecte, filtre de licence, crédits, test | `scripts/packs/freesound.json`, `scripts/packs/freesound_pack.py`, `scripts/packs/tests/`, `THIRD_PARTY.md`, `docs/DECISIONS.md` | test : un son NC ou sans licence est refusé (rouge avant, vert après) ; aucun fichier audio dans `git status` ; liste de crédits produite |
| **1. Corps commun (assistant)** | piste de texture de fond (A7), lit d'aigus au Drop (A4), doublage clap/snare/doigts et kick en couches (A5), accords doublés à l'octave (A6), contre-mélodie par défaut (A3), FX de transition pris dans un pack (S6) | `project-model/.../StarterWizard.kt`, `DrumArrangement`, `WizardMix`, tests du modèle | tests ciblés du modèle ; banc : pistes simultanées au Drop +3 au moins, aigu occupé > 80 % du Drop, aucune cible S7 au rouge ; A/B sur 3 styles |
| **2. Moteur : attaque et poids** | sculpteur d'attaque (E10), compression parallèle du bus batterie (E11), seconde réverbe courte (E6) | `engine/src/main/cpp/core/Effects.h`, `ExtraEffects.cpp`, `Mixer.cpp`, `engine/host-tests/`, `project-model/.../Effects.kt` | `./scripts/host-tests.sh` vert (ni NaN, ni allocation dans le callback) ; crête du kick conservée, niveau moyen batterie +1 à +2 LU ; `live-bench.sh` = export |
| **3. Hip-hop** (trap, boom bap, drill, lofi, phonk, grime) | kits réels en couches, snare + percs + voix en trap, break et sample au grain vinyle en boom bap/lofi, texture vinyle | `scripts/packs/recipes/hiphop.json`, pack(s) du lot 0, `StyleRecipes.kt` régénéré | boom bap et lofi : médium entre −10 et −13 dB ; trap : 8 lignes rythmiques au moins ; A/B morceau entier « B mieux » sur trap et boom bap |
| **4. House** (house, deep, tech house, garage, acid, afro, disco) | boucles d'aigus, shaker/tambourin, stabs et pianos par style, voix réelles, basse électrique disco ; **garder l'ancien clap tech house** (préféré par Benjamin) | `scripts/packs/recipes/house.json`, `scripts/packs/instruments.json` | A/B « B mieux » sur tech house et house ; haut-médium deep house remonté de −21 vers −16 dB |
| **5. Techno** (10 styles) | rides et charleys réels, grondement (rumble) en couches, textures industrielles, impacts | `scripts/packs/recipes/techno.json`, `techno_hard.json` | A/B « B mieux » sur techno ; aucune cible de volume perdue |
| **6. Bass music** (dnb, liquid, neuro, jungle, jump up, dubstep, riddim) | breaks réels découpés, montées longues, impacts lourds | `scripts/packs/recipes/bass.json` | jungle et dnb jouent un break échantillonné ; A/B « B mieux » sur dnb |
| **7. Autres** (reggaeton, afrobeats, amapiano, jersey, synthwave, pop) | percussions du monde déjà là (VCSL) enfin câblées, guitare, shaker amapiano, fond du log drum | la partie « autres » de `scripts/packs/recipes/` (fichier à confirmer par `docs/CARTE-CODE.md`) | amapiano : sub entre −4 et −8 dB ; afrobeats : air remonté de −23 vers −18 dB ; A/B « B mieux » sur reggaeton et amapiano |
| **8. Banc « corps »** | les trois mesures de §5.4 | `scripts/bench/`, `docs/son/BANC.md` | `scripts/bench/selftest.sh` vert ; test rouge avant, vert après sur un morceau volontairement vide |

Ordre conseillé : 0 et 8 d'abord (une journée), puis 1 et 2 ensemble, puis **3 en premier parmi les
styles** (c'est là que Benjamin a dit « pas de la musique »), 4, 7, 5, 6.
Écoute de Benjamin après le lot 1 (3 styles), puis une par lot de styles : cinq écoutes en tout.

## 7. Ce qui n'est pas vert

- Rien n'a été écouté ni rendu pour ce document : les qualités sont estimées par la lecture et les
  mesures du 20 novembre. Les écoutes F0 du 23 novembre ne sont pas dans git.
- A11 (coupure avant le Drop) et le fichier exact des recettes « autres » restent à vérifier.
- La collecte Freesound demande une clé d'API : un secret que seul Benjamin peut créer.
- Le compte « 8 à 12 lignes dans un morceau du commerce » est un ordre de grandeur du métier, pas une
  mesure faite ici.
