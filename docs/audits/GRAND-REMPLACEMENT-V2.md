# Grand remplacement, plan v2 : remplacer tous les sons vieillots

Date : 2026-11-24. Rôle : rédacteur de plan (aucun code de l'appli modifié).
Sources : plan v1 `docs/audits/GRAND-REMPLACEMENT.md` (diagnostic, 10 manques), audit
`docs/audits/GRAND-REMPLACEMENT-AUDIT.md` (2 bloquants, 5 graves, chiffres, 25 ajouts), écoutes de
Benjamin du 2026-11-24 (§2). Le diagnostic du v1 reste valable ; ce v2 remplace son §6 (lots) et son §5.4
(acceptation), et corrige ses chiffres (§3).

Abréviations (`docs/CARTE-CODE.md`) : `M/` = `project-model/src/main/kotlin/com/pocketstudio/model/`,
`MT/` = `project-model/src/test/kotlin/com/pocketstudio/model/`, `C/` = `engine/src/main/cpp/core/`,
`A/` = `app/src/main/kotlin/com/pocketstudio/app/`, `R/` = `scripts/packs/recipes/`.

## 1. En une page

1. Benjamin n'entend pas de corps parce que tout est synthèse ou IA ; changer un clap seul ne s'entend pas
   (écoute du 24 : « pas mieux »). On livre donc **un style entier à la fois**, écouté en entier.
2. **D'abord ce qui marche sans clé Freesound** : percussions VCSL qui dorment, claquements de doigts
   OpenGameArt (« génial »), flûte VSCO-2, couches, octaves, réglages de mix déjà dans le moteur.
   Premiers styles : **trap, boom bap, tech house**, plus une **urgence techno** (le lead « Oxygène »).
3. **Le lot 1 fige le format des recettes et fusionne en premier** (bloquant B2). Aucun lot de style
   ne touche `StarterWizard.kt`, `recipes_to_kotlin.py` ni `StyleRecipes.kt` ; le chef refabrique
   `StyleRecipes.kt` une seule fois à chaque fusion de paquet.
4. **Plus de lot « Autres »** (bloquant B1) : chaque style va au lot qui tient son fichier.
5. **Un son que Benjamin a gardé ne se remplace plus** (ancien clap tech house, claquements de doigts,
   FX tech house, kicks F0).
6. Demande à Benjamin, par le chef, **tout de suite et en une fois** : la clé d'API Freesound (secret
   GitHub). Rien du début du plan ne l'attend.

## 2. Ce que Benjamin a dit le 2026-11-24 (fait foi)

| Sujet | Verdict | Conséquence dans ce plan |
|---|---|---|
| Claquements de doigts (pack `oga-snaps`, CC0, OpenGameArt et non Freesound) | « génial » | gardés ; entrent dans les recettes (L2) |
| Claps / charleys neufs | « pas mieux » | aucun clap neuf seul ; on change le corps autour |
| Clap tech house | l'ancien est meilleur | **garder l'ancien**, gelé (règle « son gardé ») |
| Trap et boom bap | « pas de corps » | couches, octaves, percussions, mix (L2) |
| FX tech house (montées, impacts) | validés, « une belle différence » | gardés ; modèle pour les autres styles |
| Techno | la mélodie fait « Oxygène », il n'entend qu'elle, ni kick ni basse | lead remplacé, kick et basse devant (L3) |
| Trap | « hyper bizarre » ; aime Future « Mask Off » ; flûte actuelle « pas agréable » | cible ci-dessous (L2, L6) |
| Trap, envies | flûte traversière pure et belle ; voix de rappeur (ad-libs, phrases, une ou plusieurs voix) ; peut-être un sifflement | lots L6 (flûte) et L7 (voix), après la recherche Fable des sources |

**Cible trap** (références : Future « Mask Off », Metro Boomin ; Travis Scott « Goosebumps ») :
808 longue, accordée à la tonalité, avec glissés ; charleys en roulements (triolets, 1/32, rafales) ;
clap/snare sec sur le 3 ; mélodie sombre et discrète (flûte ou cloche douce) ; **beaucoup d'espace**
(peu d'éléments à la fois, des trous). Le « corps » du trap vient de la 808 et du clap doublé, pas
d'un mur de sons.

Aujourd'hui (`R/hiphop.json`, `trap`) : lead `Cloche FM` en riff à l'octave 5, contre-mélodie `Nappe`,
accords `Cordes`, voix `stab` = la « Voix robot » de synthèse (`StarterWizard.kt:212-228`), 4 lignes
de batterie. Techno (`R/techno.json`, `techno`) : lead `Lead filtré`, riff `R-..O-..R-..` (sauts
d'octave répétés : c'est l'effet « Oxygène »), sans contre-mélodie.

## 3. Chiffres corrigés (audit, à reprendre tels quels)

Voix : 10 recettes sur 37 (dnb, jump_up, trap, phonk, jersey_club, house, deep_house, tech_house,
uk_garage, speed_garage), absentes de 27, et toutes sont la « Voix robot » de synthèse (G1) : le remède
est de **remplacer la voix robot par de vrais chops partout**, pas « d'ajouter une voix au trap ».
Contre-mélodie : 6 recettes. Ride : 10. Accords : 18 sons différents, presque tous de synthèse, sur une
seule octave. Chute de sub : existe (`withSubDropTrack`, vers `StarterWizard.kt:1169`), en synthèse.
Montée : clé `riser` dans 28 recettes sur 37. Roulements : appel `StarterWizard.kt:798`, définition
`M/DrumArrangement.kt:72`. Compression parallèle : le réglage « Mélange » existe (`Effects.kt:78`,
`C/Effects.h:90`) mais `BUS_GLUE` et `MASTER_GLUE` le laissent à 1 (`M/WizardMix.kt:161-170`).
Réverbe : trois types (hall, plaque, pièce, `C/Effects.h:839-849`). Boucles et breaks : le moteur joue
déjà des clips calés au tempo (`M/AudioTracks.kt:95-131`, `engine/.../AudioTracks.h:37`) ; il manque
le chemin recette → clip. Sources de batterie d'une recette : seulement les packs de style et de
machine (`PACKS`, `recipes_to_kotlin.py:152`) ; les bibliothèques (VCSL, `oga-snaps`) ne sont pas
encore permises. Niveaux cités : banc S7 (`docs/son/banc/apres-S7.json`), pas une mesure du jour.

## 4. Règles communes à tous les lots

### 4.1 Fichiers

- Un fichier = un seul lot ouvert (boost éco). Tableau des propriétaires en §6.
- `StarterWizard.kt`, `recipes_to_kotlin.py`, `M/DrumArrangement.kt`, `M/WizardMix.kt`, `M/AudioTracks.kt`
  appartiennent au **lot 1** seulement. Après sa fusion, un lot qui a besoin d'un champ neuf le demande
  au chef (lot de format suivant), il ne le code pas.
- `M/StyleRecipes.kt` : **aucun lot ne le pousse**. Chaque lot le régénère en local pour ses tests ;
  le chef le régénère une fois à la fusion du paquet (`python3 scripts/packs/recipes_to_kotlin.py`).
- `scripts/packs/libraries.json`, `styles.json`, `instruments.json`, `build_index.py`,
  `scripts/packs/tests/` : au **lot « sources »** ouvert du moment (L0, puis L5, puis L6/L7).
  Un lot de style qui veut un son neuf l'écrit dans sa note au chef.
- Un lot ne retire jamais un son : l'ancien reste choisissable (retour arrière sans reconstruire).

### 4.2 Dépôt public et licences

- **Aucun fichier audio dans git** ; git garde la liste (identifiant, auteur, adresse, licence, somme
  de contrôle) et les scripts. Rendus et MP3 d'écoute dans `build/` ou le bloc-notes, jamais commités.
- Liste blanche : CC0 et CC-BY 4.0. Refusés : NC, « Sampling+ », licence sans nom. Le filtre est un
  **test qui casse la construction du pack** (rouge avant, vert après), pas une consigne.
- Breaks, voix et boucles : contrôle à la main (auteur, description, autres dépôts du compte), liste
  des refus écrite ; en cas de doute, on ne prend pas (un « amen » ou une phrase chantée peut être
  un extrait de disque).
- CC-BY : crédit **et** mention des modifications (coupe, normalisation) dans `THIRD_PARTY.md` et dans
  l'écran « À propos et licences » (`A/settings/`).
- Morceaux de référence du commerce : écoutés hors git, on n'en garde que des chiffres.
- Packs « machine » (tr909, sp1200, mpc60, linndrum) : noms de marques sur des sons IA, à renommer
  avant toute publication (pas bloquant ici).

### 4.3 Acceptation (oreille de Benjamin)

- **A/B à l'aveugle, morceau entier** : même morceau (mêmes notes, même tempo), avant et après.
- **Volume égal** : LUFS intégré du banc (`ps_measure`), écart de 0,5 dB au plus après gain.
- **Tirage** : le chef tire A/B au sort par style et note la clé dans `docs/son/ECOUTES.md` (date,
  style, sha, clé) ; la clé est écrite *après* la réponse de Benjamin dans le message, jamais avant.
- 3 styles par envoi au plus. Réponse par style : « B mieux », « pareil », « A mieux ».
  **« Pareil » = refusé.** « A mieux » = on revient à l'ancien **son par son** (on demande lequel),
  pas morceau par morceau, et ce son-là est gelé.
- Phrase d'acceptation commune : « on dirait un morceau qu'on entend en soirée ou en playlist, pas une
  démo ». Chaque lot a en plus sa phrase propre (§5).
- Un style n'est « remplacé » qu'après **sa propre** écoute ; sinon il reste « tenté ».
- Écoute aussi sur haut-parleur de téléphone : un morceau qui y perd son kick est refusé (ajout 24).

### 4.4 Mesure du banc du son (pas le banc des testeurs)

Banc : `scripts/song-bench.sh --styles <ids> --before docs/son/banc/apres-S7.json` (rapport
`build/bench/`, mode d'emploi `docs/son/BANC.md`), plus `scripts/live-bench.sh` sur les mêmes styles
(règle du 2026-10-24 : jamais seulement l'export). Garde-fou pour tout lot : aucune cible S7 ne passe
au rouge (sonie, true peak, largeur, Drop − Break, perte téléphone), ni `NAN`/`CLICK`/`SILENCE`.
Les mesures « corps » (L4) s'ajoutent dès qu'elles existent.

### 4.5 Déroulé d'un lot

Fiche fermée (fichiers exacts, tests, interdits) ; commit « rouge: » puis « vert: » ; tests ciblés
(`./gradlew :project-model:test --tests '<classe>'`, `scripts/packs/tests/`) ; banc sur ses styles ;
relecture neuve en dernier commit ; « fini <sha> » au chef. Le chef fait les MP3 A/B et l'écoute.

## 5. Les lots, dans l'ordre

Ordre : **L0 et L1 tout de suite en parallèle ; L2 et L3 dès la fusion de L1** (L3 peut démarrer sa
partie « sans champ neuf » avant) ; L4 en parallèle de L2/L3 ; L5 à l'arrivée de la clé ; L6/L7 au
retour de la recherche Fable ; puis L8 à L11 (styles complets) et L12 (moteur) au fil des fichiers
libres. Écoutes de Benjamin : après L2+L3 (trap, boom bap, tech house, techno), puis une par lot de
styles.

### L0. Sources sans clé (outillage + sons qui dorment)

- **Contenu** (8) : rendre utilisables par une recette les bibliothèques déjà là : `percussions_acoustiques`
  (VCSL : shaker, tambourin, cabasa, conga, bongo, claves), `oga-snaps` (claquements), flûte VSCO-2
  (`FluteSusVib.sfz`, déjà dans `instruments.json`) ; inscrire ces sources dans l'index des packs ;
  test de liste blanche de licence (CC0 / CC-BY seulement, NC, Sampling+ et sans-nom refusés) ; test
  « aucun audio dans `git status` » ; crédits dans `THIRD_PARTY.md` ; créer `scripts/packs/tests/`.
- **Fichiers** : `scripts/packs/libraries.json`, `scripts/packs/styles.json`, `scripts/packs/instruments.json`,
  `scripts/packs/build_index.py`, `scripts/packs/tests/` (neuf), `THIRD_PARTY.md`, `docs/DECISIONS.md`.
- **Ne touche pas** : les recettes `R/*.json`, `recipes_to_kotlin.py`, `StyleRecipes.kt`, tout `M/`, tout `C/`.
- **Preuve** : test de licence rouge sur une fiche NC fabriquée, vert après ; index régénéré contenant
  les trois sources ; `git status` sans audio.
- **Phrase vue par Benjamin** : aucune écoute (outillage) ; elle sert L2.

### L1. Format des recettes et corps commun (fusionne en premier)

- **Premier commit = le format gelé**, écrit en tête de `recipes_to_kotlin.py` : (a) `source` d'une
  ligne de batterie peut nommer une bibliothèque (`lib:<pack>:<son>`) ; (b) `layers` sur une ligne :
  plusieurs sons joués ensemble (clap + snare + claquement ; kick clic + corps) ; (c) rôles neufs
  `shaker`, `snap`, `rim`, `conga` ; (d) `chords.double` : doublage à l'octave et note aiguë tenue au
  Drop ; (e) `texture` : piste de fond du début à la fin ; (f) `loop` : clip de boucle (top loop, break)
  via `AudioTracks` ; (g) `vocal.sample` : chop réel à la place de la « Voix robot » ; (h) `mix` :
  pompe sur la 808 / les accords, mélange parallèle du bus batterie, saturation du bus.
  Les lots de styles partent de ce commit.
- **Contenu ensuite** (arrangement par défaut) : lit d'aigus au Drop (A4), doublage (A5), accords à
  l'octave (A6), contre-mélodie par défaut (A3), texture (A7), FX de transition pris dans un pack (S6,
  sur le modèle des FX tech house validés), compression parallèle employée (`BUS_GLUE` mélange < 1),
  pompe 808 en trap/drill, graves en mono sous 120 Hz.
- **Fichiers** : `M/StarterWizard.kt`, `M/DrumArrangement.kt`, `M/WizardMix.kt`, `M/AudioTracks.kt`,
  `M/Layers.kt` (s'il sert ; à vérifier), `scripts/packs/recipes_to_kotlin.py`, leurs tests `MT/`.
- **Ne touche pas** : `R/*.json` (sauf une recette d'essai dans les tests), `StyleRecipes.kt` (régénéré
  en local seulement), les fichiers de L0, `C/`.
- **Preuve** : tests du modèle (chaque champ neuf lu, refusé s'il est faux) ; banc sur 3 styles :
  aucune cible au rouge ; ancien comportement identique quand la recette n'a pas les champs neufs.
- **Phrase** : pas d'écoute seule ; jugé à travers L2.

### L2. Sans clé : trap, boom bap, tech house

- **Contenu** :
  - *Trap*, vers « Mask Off » : 808 longue accordée avec glissés (déjà `glide`), charleys en
    roulements (triolets, 1/32, rafales, vélocités), clap sec + snare + claquement doublés **sur le 3**,
    perc VCSL rare, **plus d'espace** (retirer la `Nappe` en contre-mélodie, accords `Cordes` bas ou
    absents au Drop), lead `Cloche FM` remplacé par la flûte VSCO-2 discrète (provisoire, en attendant
    L6), pompe 808 sous le kick ;
  - *Boom bap* : clap + snare + claquement, shaker/tambourin VCSL, accords doublés à l'octave pour
    remplir le médium, texture vinyle (effet VINYL), swing MPC, batterie sale et compressée en parallèle ;
  - *Tech house* : **ancien clap gelé**, FX validés gardés, shaker et conga VCSL en lit d'aigus,
    claquements sur les relances, stabs d'orgue doublés.
- **Fichiers** : `R/hiphop.json` (styles `trap`, `boom_bap` seulement), `R/house.json` (`tech_house`
  seulement).
- **Ne touche pas** : les autres styles de ces fichiers, `StarterWizard.kt`, `recipes_to_kotlin.py`,
  `StyleRecipes.kt` (le chef), les sources (L0).
- **Banc** : boom bap médium de −16,25 vers −10/−13 dB ; trap : 6 lignes rythmiques au moins (kick,
  clap, snare, hat, open_hat, snap/perc) mais pas plus de 5 éléments au même pas au Drop ; tech house :
  aucune cible perdue ; `live-bench.sh` sans alerte.
- **Phrase** : trap : « ça sonne comme un beat de Metro Boomin : la 808 tient tout, ça respire » ;
  boom bap : « ça a du corps, ça tape comme un vrai beat de rap » ; tech house : « B mieux, et je
  retrouve mon clap ». A/B à l'aveugle, « pareil » = refusé.

### L3. Urgence techno (techno et techno dure, sans clé)

- **Contenu** (techno d'abord, puis les styles du même fichier qui ont le même lead) : lead `Lead filtré`
  retiré du premier plan (riff sans sauts d'octave répétés, plus bas, filtré, entrée tardive, ou supprimé
  au Drop) ; kick et basse devant (niveaux, pompe, rumble sous le kick) ; charleys métalliques et ride
  VCSL ; stabs filtrés en accords ; automation de filtre avant le Drop ; vérification des 10 styles
  techno pour la même faute (règle « erreur signalée : vérifier partout »).
- **Fichiers** : `R/techno.json`, `R/techno_hard.json`.
- **Ne touche pas** : les autres recettes, `StarterWizard.kt`, `StyleRecipes.kt`, `C/`.
- **Banc** : solo du lead (`ps_render --solo`) au moins 6 dB sous le kick au Drop ; bandes sub et grave
  remontées ou égales ; aucune cible perdue. Avant L1, seuls les champs existants ; après, le reste.
- **Phrase** : « j'entends le kick et la basse d'abord ; la mélodie ne me fait plus penser à Oxygène ».

### L4. Banc « corps » (banc du son)

- **Contenu** : trois mesures neuves : pistes qui sonnent ensemble au Drop ; part du temps où l'aigu
  (> 6 kHz) est occupé ; niveau du fond entre deux coups. Chaque seuil prouvé rouge sur un morceau
  d'aujourd'hui (boom bap S7) et vert sur un morceau de référence libre (hors git).
- **Fichiers** : `engine/bench/ps_measure.cpp`, `scripts/bench/report.py`, `scripts/song-bench.sh`,
  `docs/son/BANC.md`. **Pas** `scripts/bench/selftest.sh` ni le banc des testeurs (tour 9).
- **Ne touche pas** : `C/`, les recettes, `M/`.
- **Preuve** : test rouge avant, vert après ; rapport S7 relu avec les trois colonnes.
- **Phrase** : aucune écoute.

### L5. Chaîne Freesound (dès que la clé est là)

- **Contenu** : `freesound.json` (requêtes par famille et par style, filtres : licence, durée,
  ≥ 44,1 kHz, pas de mono 8 bits) ; `freesound_pack.py` (20 à 30 candidats par case, normalisés,
  silences coupés, mesurés par `sound_character.py`, classés par `rank_sounds.py`, 5 gardés) ; tri **dans
  un morceau** par le banc ; crédits CC-BY avec mention des modifications ; écran des crédits.
- **Fichiers** : `scripts/packs/freesound.json`, `scripts/packs/freesound_pack.py`, `scripts/packs/tests/`,
  `scripts/packs/libraries.json`, `build_index.py`, `THIRD_PARTY.md`, `A/settings/` (crédits),
  `docs/DECISIONS.md`. Clé = secret GitHub, jamais dans le dépôt.
- **Ne touche pas** : recettes, `M/`, `C/`.
- **Preuve** : test licence rouge/vert sur une réponse d'API simulée (NC, Sampling+, sans nom) ; aucune
  clé ni audio dans le diff.

### L6. Flûte trap (attend la recherche Fable des sources)

- **Contenu** : flûte traversière pure (candidats : VSCO-2 déjà là, puis les sources de la recherche) ;
  jeu legato, vibrato léger, réverbe pièce ; phrase sombre façon « Mask Off » écrite pour le lead trap ;
  variante sifflement à tester ; mêmes styles proches (drill, phonk) vérifiés.
- **Fichiers** : `scripts/packs/instruments.json` (+ `libraries.json` si source neuve), `R/hiphop.json`
  (champ `lead` des styles trap, drill, phonk). S'ouvre quand L0/L5 et le lot qui tient `hiphop.json`
  sont fermés.
- **Ne touche pas** : `StarterWizard.kt`, `StyleRecipes.kt`, autres rôles des recettes.
- **Banc** : lead solo propre (ni souffle ni clic), au moins 4 dB sous la 808 au Drop.
- **Phrase** : « la flûte est belle et douce, j'ai envie de la réécouter ».

### L7. Voix trap (attend la recherche Fable)

- **Contenu** : ad-libs (« yeah », « skrrt », cris), phrases rappées courtes, une ou plusieurs voix,
  réaccordées si chantées ; remplacement de la « Voix robot » par ces chops là où le style en a une
  (les 10 recettes, en commençant par trap, phonk, drill) ; contrôle d'origine à la main, liste des refus.
- **Fichiers** : `scripts/packs/libraries.json` / `styles.json` (source des voix), `R/hiphop.json`
  (champ `vocal`) ; le chemin de lecture est livré par L1 (`vocal.sample`).
- **Ne touche pas** : `StarterWizard.kt` (sauf retour au chef si L1 ne suffit pas), autres fichiers de recettes.
- **Banc** : voix au moins 3 dB sous le clap ; aucun `CLICK`.
- **Phrase** : « on dirait un vrai rappeur derrière le beat ». Si L6 et L7 font moins de 6 points
  chacun, une seule discussion les fait à la suite (mêmes fichiers).

### L8. Hip-hop complet (`R/hiphop.json` : drill, lofi, phonk, grime, reggaeton, afrobeats, jersey club, et reprise de trap/boom bap)

- **Contenu** : kits réels en couches (Freesound), breaks échantillonnés boom bap/lofi via `loop`,
  « sample » maison rejoué puis filtré, texture vinyle lofi, guitare, marimba et percussions à la main
  pour reggaeton/afrobeats, shaker afrobeats, ad-libs drill/phonk/jersey club (L7).
- **Fichiers** : `R/hiphop.json`. **Ne touche pas** : le reste.
- **Banc** : lofi médium vers −10/−13 et air vers −18 ; afrobeats air de −23,26 vers −18 ; boom bap et
  jungle jouent un break échantillonné.
- **Phrase** : par style écouté, « B mieux » ; « pareil » = refusé.

### L9. House complet (`R/house.json` : house, deep, garage, speed garage, acid, afro house, disco, amapiano)

- **Contenu** : top loops, shaker/tambourin, stabs et pianos par style, voix réelles (L7), basse
  électrique disco (Meatbass, CC0), log drum amapiano avec fond.
- **Fichiers** : `R/house.json`. Tech house : rien ne change sans nouvelle écoute (gelé).
- **Banc** : amapiano sub de −15,79 vers −4/−8 dB ; deep house haut-médium de −21,17 vers −16.
- **Phrase** : « B mieux » sur house et amapiano.

### L10. Techno, deuxième passe (`R/techno.json`, `R/techno_hard.json`)

- **Contenu** : rides et charleys réels Freesound, rumble en couches, textures industrielles, impacts.
- **Banc et phrase** : aucune cible perdue ; « B mieux » sur techno et une techno dure.

### L11. Bass music (`R/bass.json` : dnb, liquid, neuro, jungle, jump up, dubstep, riddim, synthwave, pop)

- **Contenu** : breaks réels découpés, reese, growls, supersaw en couches, OTT par piste, montées
  longues, impacts lourds ; synthwave et pop : guitare, basse électrique, voix (L7).
- **Banc et phrase** : jungle et dnb jouent un break ; « B mieux » sur dnb et pop.

### L12. Moteur : attaque et poids

- **Contenu** : sculpteur d'attaque (E10), saturation douce du bus batterie, égalisation milieu/côtés
  (graves mono), bus par famille si L1 le demande ; la réverbe pièce existe déjà (employée par L1).
- **Fichiers** : `C/Effects.h`, `C/ExtraEffects.cpp`, `C/Mixer.cpp`, `engine/host-tests/`, `M/Effects.kt`,
  `M/FxPresets.kt`.
- **Ne touche pas** : recettes, `StarterWizard.kt`.
- **Preuve** : `./scripts/host-tests.sh` vert (ni NaN, ni allocation dans le callback) ; crête du kick
  gardée, niveau moyen batterie +1 à +2 LU ; `live-bench.sh` = export.

## 6. Propriétaires des fichiers (un seul lot ouvert à la fois)

| Fichier | Lots, dans l'ordre |
|---|---|
| `M/StarterWizard.kt`, `M/DrumArrangement.kt`, `M/WizardMix.kt`, `M/AudioTracks.kt`, `recipes_to_kotlin.py` | L1 |
| `M/StyleRecipes.kt` | le chef, à chaque fusion |
| `libraries.json`, `styles.json`, `build_index.py`, `scripts/packs/tests/` | L0 → L5 → L7 |
| `instruments.json` | L0 → L6 |
| `R/hiphop.json` | L2 → L6 → L7 → L8 |
| `R/house.json` | L2 → L9 |
| `R/techno.json`, `R/techno_hard.json` | L3 → L10 |
| `R/bass.json` | L11 |
| `engine/bench/ps_measure.cpp`, `scripts/bench/report.py`, `scripts/song-bench.sh`, `docs/son/BANC.md` | L4 |
| `C/Effects.h`, `C/ExtraEffects.cpp`, `C/Mixer.cpp`, `M/Effects.kt`, `M/FxPresets.kt` | L12 |
| `THIRD_PARTY.md` | L0 → L5 (puis chaque lot sources) |

Paquets de fusion (3 lots au plus) : P1 = L0 + L1 ; P2 = L2 + L3 + L4 ; puis au fil des lots.

## 7. Ce qui n'est pas vert

- Rien n'a été rendu ni écouté pour ce plan ; niveaux = banc S7 du 20 novembre.
- La clé Freesound n'existe pas encore (secret que seul Benjamin crée) : L5, L8 à L11 en dépendent en partie.
- L6 et L7 attendent la recherche Fable (sources et licences de flûte et de voix de rappeur).
- La flûte VSCO-2 n'a jamais été écoutée dans un trap : provisoire, peut être refusée.
- Le chemin recette → clip (`loop`) n'est pas écrit ; l'emploi réel de `M/Layers.kt` n'est pas vérifié.
- Les seuils « corps » (L4) ne sont pas encore prouvés ; `docs/son/ECOUTES.md` (clés A/B) est à créer par le chef.
- A8, A9, A11 du v1 (vélocités, variations, coupure avant le Drop) restent à vérifier à l'écoute.
