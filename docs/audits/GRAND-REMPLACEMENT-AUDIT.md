# Audit du plan « Grand remplacement »

Relecture critique de `docs/audits/GRAND-REMPLACEMENT.md` (commit `c6b617bc`, branche `eq/grand-remplacement`), par un vérificateur qui n'a pas écrit le plan. Date : 2026-11-24.

Méthode : lecture du code et des recettes, recomptage par script des 37 recettes (`scripts/packs/recipes/*.json`), relecture des mesures du banc (`docs/son/banc/apres-S7.json`, `docs/son/banc/S7.md`). Aucun rendu neuf n'a été lancé : les chiffres de niveau viennent du dernier passage du banc, pas d'une mesure d'aujourd'hui.

En une phrase : le diagnostic est juste, les chiffres de niveau sont exacts, mais le découpage en lots ne tient pas tel quel (deux blocages), et le premier son neuf arrive trop tard pour Benjamin.

## Graves

### B1 (bloquant). Le lot 7 « Autres » n'a aucun fichier à lui

Les six styles du lot 7 vivent dans les fichiers des autres lots :

| Style | Fichier | Lot qui tient ce fichier |
|---|---|---|
| reggaeton, afrobeats, jersey_club | `scripts/packs/recipes/hiphop.json` | lot 3 |
| amapiano | `scripts/packs/recipes/house.json` | lot 4 |
| synthwave, pop | `scripts/packs/recipes/bass.json` | lot 6 |

La règle « un fichier = un seul lot ouvert » est donc violée dès que le lot 7 s'ouvre. Correction : supprimer le lot 7 et rattacher chaque style au lot qui tient son fichier (3, 4 ou 6). Autre voie, plus chère : couper d'abord les trois fichiers (un lot de pure découpe, sans changement de son).

### B2 (bloquant). Les lots de styles dépendent tous de fichiers que personne ne tient

- `project-model/.../StyleRecipes.kt` est fabriqué par `scripts/packs/recipes_to_kotlin.py` à partir des recettes. Chaque lot de style (3 à 7) le refabrique : c'est un fichier partagé par cinq lots. Correction : aucun lot de style ne le pousse ; le chef le refabrique une seule fois, à la fusion de chaque paquet.
- Les champs neufs des recettes (couches, boucles, textures, breaks, voix) demandent de changer `recipes_to_kotlin.py` et `StarterWizard.kt`. Or `StarterWizard.kt` est réservé au lot 1. Les lots 3 à 7 ne peuvent donc rien brancher de neuf tant que le lot 1 n'a pas posé et fusionné le format. Correction : écrire dans le plan « le lot 1 fige le format des recettes ; les lots de styles partent après sa fusion », et donner `recipes_to_kotlin.py` au lot 1.
- `scripts/packs/libraries.json`, `styles.json`, `build_index.py` et `instruments.json` : les trois premiers n'ont pas de lot, le quatrième n'est cité qu'au lot 4 alors que le lot 7 veut aussi une guitare. Correction : un seul lot « sources » (le lot 0) les tient tous ; les lots de styles lui demandent leurs ajouts.

### G1. Deux constats faux sur le trap, le style que Benjamin a rejeté

- Le tableau du §2 et le manque n° 6 disent « pas de voix » pour le trap. La recette a `"vocal": "stab"` et une contre-mélodie (`counter` : Nappe).
- La vraie faute est ailleurs : cette voix n'est pas un échantillon, c'est la voix de synthèse « Voix robot » (`StarterWizard.kt:212-228` : « no vocal sample is built in »). Elle vaut pour les 10 styles qui ont une voix.

Le remède change : il ne faut pas « ajouter une voix au trap », il faut remplacer la voix robot par de vrais chops, partout. À corriger avant d'ouvrir le lot 3, sinon il réparera la mauvaise chose.

### G2. Le lot 8 vise le mauvais banc

Le lot 8 cite `scripts/bench/` et `selftest.sh`. C'est le banc des testeurs d'écran. Le banc du son est `scripts/song-bench.sh`, son rapport, `docs/son/BANC.md` et `docs/son/banc/*.json`. Tel quel, le lot 8 ouvrirait des fichiers d'un autre chantier (tour 9) et ne toucherait pas la mesure du son. De plus `scripts/packs/tests/` n'existe pas encore : c'est le lot 0 qui le crée, le plan doit le dire.

### G3. Le plus gros gain audible n'arrive pas en premier

- Les lots 0 et 8 passent d'abord : une journée d'outillage où Benjamin n'entend rien de neuf.
- Le lot 0 attend une clé Freesound, un secret que seul Benjamin peut créer. Tant qu'il ne l'a pas fait, toute la chaîne est à l'arrêt, et le plan ne le lui demande pas en premier.
- Pendant ce temps, du son prêt dort dans le dépôt : le pack VCSL `percussions_acoustiques` (shaker, tambourin, cabasa, conga, bongo, claves) n'est branché dans aucune recette, et les claquements de doigts que Benjamin a trouvés « géniaux » (pack OpenGameArt `oga-snaps`, CC0) ne sont dans aucun style.

Correction de l'ordre : (1) demander la clé à Benjamin tout de suite, en une demande ; (2) ouvrir en premier un lot « sans clé » : percussions qui dorment, claquements de doigts, doublage d'octave, couches kick + clap, sur trap, boom bap et tech house ; (3) le lot 1 (format + arrangement) en parallèle ; (4) le banc du son et Freesound ensuite.

### G4. Le critère d'acceptation ne protège pas ce que Benjamin aime déjà

« A/B morceau entier à volume égal, pareil = refusé » est mesurable, mais il manque :

- Qui tire au sort A et B, et où la clé est notée (sinon ce n'est pas à l'aveugle).
- Le cas « A est mieux » : l'écoute d'aujourd'hui l'a produit (il préfère l'ancien clap tech house). Règle à écrire : un son que Benjamin a gardé ne se remplace plus ; on revient à l'ancien, son par son, pas morceau par morceau.
- La couverture : cinq écoutes sur trois styles ne disent rien des 34 autres. Règle à écrire : un style n'est « remplacé » qu'après sa propre écoute ; sinon « tenté ».
- Les trois mesures neuves de « corps » n'ont pas de seuil. Chaque seuil doit être prouvé par un test rouge sur un morceau d'aujourd'hui (boom bap : médium à −16,25, le plus bas des 37) et vert sur un morceau de référence libre.
- Les morceaux de référence du commerce ne peuvent pas entrer dans le dépôt : dire où ils vivent (hors git) et qu'on n'en garde que des chiffres.
- Volume égal : dire l'outil (LUFS intégré du banc, écart de 0,5 dB au plus).

### G5. Licences : trois trous

- Une étiquette CC0 ou CC-BY sur Freesound ne prouve pas l'origine. Un break (« amen »), une phrase chantée ou un stab peuvent être un extrait de disque déposé par quelqu'un qui n'en a pas le droit. Pour les breaks, les voix et les boucles : contrôle à la main (auteur, description, autres dépôts du compte) et liste des refus ; en cas de doute, on ne prend pas.
- CC-BY 4.0 demande le crédit et la mention des modifications (coupe, normalisation). Le crédit doit être visible dans l'appli, pas seulement dans `THIRD_PARTY.md`.
- Le filtre « jamais NC » doit être un test qui casse la construction du pack (licence lue par l'API, liste blanche CC0 / CC-BY), pas une consigne. Les « Sampling+ » et les licences sans nom se refusent aussi.

À noter : les packs dits « machine » (tr909, sp1200, mpc60, linndrum) sont fabriqués par IA et portent des noms de marques. Sans gravité pour un usage personnel, à renommer avant toute publication.

## À corriger

Chiffres et renvois faux ou flous dans le plan (aucun ne change le diagnostic) :

| Le plan dit | Le code dit | Preuve |
|---|---|---|
| voix dans 13 recettes sur 37 | 10 (dnb, jump_up, trap, phonk, jersey_club, house, deep_house, tech_house, uk_garage, speed_garage) | champ `vocal` des recettes |
| voix absentes de 24 styles | 27 | même compte |
| contre-mélodie dans 7 recettes | 6 (liquid_dnb, trap, afrobeats, detroit, minimal, trance) | champ `counter` |
| `ride` dans 11 recettes | 10 | rôles des lignes de batterie |
| « 6 sons de synthé pour 37 styles » (accords) | 18 sons d'accords différents ; « Accords future bass » n'est pris par aucun. Le vrai défaut : presque tous sont de la synthèse, sur une seule octave (3 ou 4) | champ des accords |
| S6 : pas de chute de sub | une piste de chute de sub existe, en synthèse | `StarterWizard.kt` vers la ligne 1169 (`withSubDropTrack`) |
| un souffle montant partout | la clé `riser` n'est que dans 28 recettes sur 37 | recettes |
| A2 : `DrumArrangement.withFillsAndCrashes (:797)` | appel à `StarterWizard.kt:798`, définition à `DrumArrangement.kt:72` | lecture |
| E11 : pas de compression parallèle | le compresseur a un réglage « Mélange » (parallèle) ; ce qui manque, c'est son emploi : `BUS_GLUE` et `MASTER_GLUE` le laissent à 1 (tout compressé) | `Effects.kt:78`, `Effects.h:90`, `WizardMix.kt:161-170` |
| E6 : une seule réverbe | trois types (hall, plaque, pièce) | `Effects.h:839-849` |
| S1, S4 : boucles et breaks comme un chantier neuf | le moteur joue déjà des clips audio calés et étirés au tempo ; il manque seulement le chemin recette → clip | `AudioTracks.kt:95-131`, `engine/.../AudioTracks.h:37` |
| claquements de doigts « Freesound » (contexte) | ils viennent d'OpenGameArt (`oga-snaps`, CC0) | `THIRD_PARTY.md:30` |

À ajouter au §7 « pas vert » du plan : les niveaux cités datent du banc S7, pas d'une mesure du jour.

## Ajouts proposés

Ce que les producteurs d'électro font couramment en 2025-2026 et que le plan ne nomme pas, ou pas assez. Classé par gain à l'écoute.

Corps du morceau (le reproche de Benjamin)
1. Bus par famille (batterie, basse, musique, voix, effets) avec leur propre traitement ; aujourd'hui seule la piste 0 de batterie et le master sont collés (`WizardMix.kt:177`).
2. Compression parallèle sur le bus batterie : le réglage existe, il suffit de l'employer (voir E11).
3. Saturation et écrêtage doux sur le bus batterie, avant le master.
4. Kick et 808 accordés à la tonalité du morceau, et 808 qui s'efface sous le kick (sidechain déjà possible : `Effects.h:193`, `Engine.h:177`) : aujourd'hui la pompe n'est posée qu'en house et techno (`WizardMix.kt:199`), pas en trap ni en drill.
5. Sidechain aussi sur les nappes et les accords, pas seulement sur la basse.
6. Couches : kick (clic + corps + sub), clap (clap + snare + claquement), accords doublés à l'octave et élargis. `Layers.kt` existe : dire s'il sert.
7. Graves en mono sous 120 Hz environ, largeur dans le haut (égalisation milieu / côtés) : la médiane côté/milieu de −11,7 dit un mix étroit.
8. OTT sur les basses et les leads par piste (bass music, trap), pas seulement léger sur le master.

Vie et mouvement
9. Vélocités et micro-décalages des charleys, roulements de charley du trap (triolets, 1/32, glissés de hauteur), notes fantômes de caisse claire.
10. Automations dans le morceau généré (`Automation.kt` existe) : filtre qui s'ouvre avant le drop, volume, envoi de réverbe et d'écho sur le dernier mot (« throw »), hauteur qui monte sur le roulement.
11. Gabarits de swing par style (MPC pour le boom bap, garage, amapiano).
12. Place stéréo de chaque percussion (panoramique fixe par rôle).

Transitions et sucreries
13. Cymbale et réverbe à l'envers, impacts en couches, montées de hauteur, coupure d'une demi-mesure avant le drop, tape stop (l'effet existe, `Effects.kt:320`).
14. « Ear candy » : petits sons uniques toutes les 4 ou 8 mesures (bruitages, bouts de voix, percussions isolées).
15. Textures de fond par style : souffle de vinyle (l'effet existe, `Effects.kt:235`), ambiance de salle, pluie, foule.

Voix
16. Vrais chops de voix, hachés et réaccordés à la tonalité, avec formant ; remplacent la « Voix robot » (voir G1).
17. Tags, cris et « ad-libs » courts (trap, drill, phonk, jersey club), CC0 et contrôlés à la main.

Son par style (à écrire dans chaque lot, avec une phrase d'acceptation)
18. Trap, drill : 808 long et glissé, charleys roulés, caisse claire sèche, contre-mélodie sombre (cloche, flûte, piano feutré).
19. Boom bap, lofi : batterie sale et compressée, « sample » maison rejoué puis filtré, basse ronde, souffle. Le médium à −16,25 est le premier chiffre à remonter.
20. Amapiano : le log drum porte le grave (sub à −15,79 contre −5,61 de médiane), shakers, piano jazzy.
21. House, tech house : basse de la house d'aujourd'hui, percussions en boucle, stabs d'orgue et de piano ; garder l'ancien clap tech house.
22. Techno : rumble sous le kick, charleys métalliques, stabs filtrés. Bass music : reese, growls, supersaw en couches.
23. Afro house, afrobeats, reggaeton : guitare, marimba ou kalimba, percussions à la main, shakers.

Garde-fous
24. Écoute sur haut-parleur de téléphone à chaque lot (perte médiane 3,8 dB) : un morceau qui perd son kick au téléphone est refusé.
25. Un lot ne retire jamais un son : l'ancien reste choisissable, pour revenir en arrière sans reconstruire.

## Vérifié

Exact dans le code ou dans le banc S7 :

- 37 recettes, dans 5 fichiers.
- Niveaux par bande : trap médium −10,25 / haut-médium −15,3 / air −18,62 ; boom bap −16,25 (le plus bas des 37) / −16,01 / −19,16 ; lofi −15,03 / −17,79 / −22,17 ; tech house −8,09 / −15,59 / −15,25 ; amapiano sub −15,79 (médiane −5,61) ; reggaeton −8,74 / −12,44 / −16,01 ; deep house haut-médium −21,17 ; afrobeats air −23,26.
- Médianes du §1 : −8,5 LUFS, côté/milieu −11,7, drop − break +5,0, perte au téléphone 3,8 (`docs/son/banc/S7.md:23-29`).
- Nombre de lignes de batterie : trap 4, boom bap 5, lofi 4, tech house 7, amapiano 5, reggaeton 6. 39 lignes sans `source` (kit de synthèse).
- `StarterWizard.kt:1026-1035` : la piste « Souffle montant » et son bruit blanc de synthèse ; `:1043` 4 mesures ; `:729-744`, `:751`, `:753-765`, `:1180` conformes.
- Tous les renvois de lignes de `Effects.kt` (101 à 360), `Effects.h:193`, `Engine.h:177`, `MasterChain.h:17-27`.
- Aucun rôle shaker, tambourin, conga, claquement ou rimshot dans les recettes ; le pack VCSL de percussions dort.
- Pas de voix dans boom bap, drill, reggaeton, amapiano. « answer » dans 6 recettes.
- `instruments.json` : pianos, cordes, pizzicati, cuivres, bois, harpe seulement.
- Pas de sculpteur de transitoires, de shimmer, de granulaire ni d'égaliseur dynamique dans le moteur.
- `libraries.json` porte bien un champ `licence` par source (CC0).

Pas vérifié : aucun rendu hors-ligne ni `host-tests.sh` relancé ; l'emploi réel de `Layers.kt` par le générateur ; les points A8, A9, A11 du plan.

Verdict : 2 bloquants, 5 graves
