# Plan d'action après les passages du robot « BenStudio Test »

Demandé par Benjamin le 2026-11-23 (« Prévois le plan d'action avec tous les nouveaux tests […] comment analyser et réparer, et des améliorations de l'ensemble »). Démarrage : 2026-11-24 à 2 h 05 (heure belge), à plein quota, cinq discussions neuves : quatre en Opus 5.5 (E, A1, C, D) et une en Fable 5.2 pour le moteur son (B1).

Relu et corrigé le 2026-11-23 par un auditeur neuf : voir la section 7, « Audit Fable ». Le plus important : quatre constats sur quatorze venaient du robot et pas de l'appli, et trois mesures pouvaient mentir. On répare donc d'abord l'outil de mesure.

## 1. D'où on part (constats mesurés)

Rapports gardés hors dépôt (bloc-notes du chef) : téléphone Xiaomi Android 17 (v6 complet, puis v7 « Refaire les ratés », son allumé) et tablette Samsung SM-X230 Android 16 (v6 rapide). Le robot est une vraie version optimisée de l'appli (pas une version de mise au point) : ses chiffres de charge comptent.

« Sûr » = lu dans le code. « À prouver » = personne ne répare avant la preuve.

| N° | Constat | Appareil | Vient de | Sûr ? |
|---|---|---|---|---|
| C1 | 87 coupures du son, charge moyenne 83 à 94 %, pire bloc à 226 % | tablette | appli (trop gourmande) | à prouver par un passage témoin : même morceau, robot au repos |
| C2 | « Le Live joue 4 s puis s'arrête » | tablette | sans doute une mauvaise lecture du rapport | le scénario du Live écoute 4 s par construction (`liveEcouteMs = 4_000`), puis c'est le robot qui arrête le Live. Sa phrase de réussite est « le Live joue (…), puis s'arrête ». À relire dans le rapport avant tout travail |
| C3 | 4 styles (acid techno, acidcore, drill, boom bap) : trou de 4,1 à 4,6 blocs pour 4 permis | téléphone | mesure douteuse | l'écart est divisé par la durée du bloc d'avant (`CallbackLoad.h`), et la taille des blocs n'est pas fixe : un petit bloc suivi d'une attente normale se lit « 4 blocs ». Sans coupure comptée, ce n'est pas un rouge |
| C4 | Coupures quand le son part vers Android Auto | téléphone | appli (changement de sortie, `AudioHost.cpp`) | à rejouer |
| C5 | Délai du son non mesuré : « MicUnavailable » | téléphone | robot d'abord | le robot lance la mesure sans vérifier la permission du micro, et en v7 il écoute déjà lui-même par le micro |
| C6 | Images lentes : Mix et Live 100 %, Projets 51 %, Sons 39 %, Structure 28 à 42 % | les deux | appli, compteur à prouver | seuil fixe de 16,7 ms quel que soit l'écran ; sur la tablette, mesuré pendant que le son sature (C1) |
| C7 | Mémoire : 281 à 487 Mo | les deux | inconnu | mémoire totale, robot compris, sans seuil ni détail (appli, moteur et sons chargés, images) |
| C8 | « Ajouter une montée » n'ajoute pas de partie (26 avant, 26 après) | téléphone | **robot (oracle faux)** | sûr : la montée ajoute une courbe, jamais une partie (`buildUpBefore`, `PartIntents.kt`). L'appli fait ce qu'elle doit |
| C9 | Annuler puis rétablir : menu de la partie introuvable sur le téléphone, passe sur la tablette | téléphone | robot (petit écran), à prouver | si un doigt ne peut pas l'atteindre non plus, c'est l'appli |
| C10 | Égaliseur refusé : trois effets déjà sur la piste | téléphone | robot (piste pas nettoyée) | sûr : quand toutes les pistes sont pleines, il prend la piste 1 quand même |
| C11 | Pads du Live introuvables (8 scénarios non mesurés) | les deux | **robot** | sûr : il cherche `live_pad_3`, l'écran du Live nomme ses cases `live_pad_<piste>_<ligne>` |
| C12 | Fermer / rouvrir et arrière-plan non faisables | tablette | robot | |
| C13 | Lecture longue comptée « plantage » quand Benjamin ferme l'appli à la main | téléphone | robot | |
| C14 | Volume à 2 sur 15 : compté coupé, mais le test démarre quand même | tablette | robot | |
| C15 | Délai du son de 958 ms lu sur un passage d'avant (noté dans `EN-COURS.md` le 22 novembre) | téléphone | à trancher après C5 | la limite du robot est 70 ms |

Bilan honnête : l'appli a sûrement un vrai problème de charge sur la tablette (C1) et sans doute de fluidité (C6). C8, C10 et C11 ne sont pas des bugs de l'appli. C2, C3, C5, C7 et C15 ne se réparent pas avant d'être prouvés.

Les constats de l'appli entrent dans la liste gelée du tour 9 (`docs/audits/ultra/tour-9-tri/`) : un seul compteur.

## 2. Cinq discussions cette nuit, cinq zones qui ne se touchent pas

Chaque discussion reçoit une fiche fermée (gabarit `GABARIT-FICHES-V3.md`) : ses fichiers, ses constats, la preuve attendue. Aucune ne pousse sur l'intégration ; le chef fusionne par le portail. Un lot = une discussion, fermée à la fin du lot : la suite (A2, A3, B2, B3, F) part dans des discussions neuves.

Base : B1, C et D partent de l'intégration (`ccr-80af1a1b-mn0wec`). A1 part de `eq/bentest-robot-2` (2c0cab99c), qui n'est pas encore dans l'intégration.

### Qui a quel fichier

| Fichiers | Cette nuit |
|---|---|
| `app/src/benTest/**`, `app/src/journey/**`, `app/src/test/java/com/pocketstudio/bentest/**`, `scripts/bentest/**`, le bloc `benTest` de `app/build.gradle.kts` | A1 |
| `engine/src/main/cpp/core/**`, `engine/host-tests/**`, `engine/bench/**`, `scripts/host-tests.sh`, `scripts/live-bench.sh`, `scripts/cpu-bench.sh` | B1 |
| `engine/src/main/cpp/android/**` (`AudioHost`, `JniBridge.cpp`), `engine/src/main/java/**` (`NativeEngine.kt`…), `A/playback/`, `A/audio/Headphones.kt`, `A/audio/MicRecorder.kt`, `A/latency/`, `A/settings/Settings.kt` | D |
| `A/mixer/`, `A/live/`, `A/projects/`, `A/library/`, `A/sounds/`, `A/session/`, `A/ui/` (dont `AppRoot.kt`), `ui-components/**` | C |
| `A/rhythm/RhythmViewModel.kt` (10 903 lignes, tout le monde en dépend) | C seul cette nuit. D écrit sa demande dans sa note, le chef la passe à C |
| `project-model/**` | personne cette nuit (une demande au chef) |
| `docs/DECISIONS.md`, `THIRD_PARTY.md`, `EN-COURS.md` | le chef |

Deux pièges que la première version du plan avait : `engine/**` était donné en entier à B alors que les réparations de D (changement de sortie, micro) sont dans `engine/src/main/cpp/android/AudioHost.cpp` ; et C et D pouvaient toutes deux ouvrir `RhythmViewModel.kt`.

C garde tous les noms que le robot lit à l'écran (`testTag`) : en changer un casse le robot sans toucher à ses fichiers.

### Discussion E : « Audit de la fabrique de scénarios et des compteurs » (Opus 5.5, lecture seule ; Benjamin, 2026-11-23 : « il faut que ce soit synchro, l'un et puis l'autre »)
- Démarre à 2 h 05 avec les autres. Elle relit la façon de fabriquer et de juger les scénarios (`Scenarios.kt` et ses tests) : scénarios vraiment distincts ou copies, oracles qui peuvent mentir, restes laissés d'un scénario au suivant, recherche des éléments à l'écran, seuils, et le bon dessin pour passer à environ 400.
- En plus (ajout de l'audit) : elle compare chaque oracle de geste au modèle de l'appli (l'erreur de C8 : attendre une partie de plus là où l'appli ajoute une courbe), et elle relit les quatre compteurs : images lentes, écart entre blocs, mémoire, délai du son.
- **Verrou** : aucun scénario neuf tant que l'audit n'est pas rendu et que le chef n'a pas transmis ses conclusions. En attendant, A1 fait seulement ses réparations.

### Discussion A1 : « Robot, réparations » (Opus 5.5 ; l'outil de mesure d'abord)
- Par paquets de trois réparations, tests du robot verts entre deux paquets, dans cet ordre :
  1. Oracles faux : C8 (lire la courbe de montée, puis son retrait par « Annuler »), C11 (les vrais noms des cases du Live), C10 (piste nettoyée avant, ou « non mesuré » dit clairement).
  2. Mesures : C5 (permission du micro vérifiée et micro du robot libéré avant la mesure), C2 (le Live écouté 30 s, rouge seulement si le moteur cesse de jouer avant que le robot l'arrête), C3 (l'écart affiché en millisecondes à côté de la taille du tampon ; sans coupure comptée, orange au plus).
  3. Compteurs : images lentes jugées contre le rythme réel de l'écran (pas 16,7 ms partout), avec un écran témoin qui doit lire près de 0 % et un minimum d'images, sinon « non mesuré » ; mémoire détaillée (appli, moteur, images) lue au début et à la fin.
  4. C9, C12, C13, C14.
- Garde neuve : aucun scénario ne part sur le téléphone sans un passage vert sur les vrais écrans dans les tests (`ScenariosVraisEcransTest`, téléphone étroit et tablette). C8, C10 et C11 auraient été vus là, sans déranger Benjamin.
- Garde neuve : un paquet avec plus de 5 % de « non mesuré » est un échec du robot, écrit en tête du rapport (8 pads gris étaient passés sans bruit).
- Preuve : chaque réparation a son test rouge avant, vert après.
- Confort (bandeau « Test long en cours », refus de démarrer si le volume est trop bas, « Refaire les ratés » calculé depuis le dernier rapport) : après les réparations, s'il reste de la place ; sinon lot A2.

### Discussion B1 : « Moteur son, la mesure » (Fable 5.2, décision de Benjamin du 2026-11-23 : « il faut vraiment qu'il se consacre à ça, et donc en Fable »)
- Zone : le cœur du moteur seulement (tableau plus haut). Fiche courte : le problème, les fichiers, la preuve.
- But de la série B : qu'une tablette d'entrée de gamme joue tous les styles sans coupure.
- Cette nuit, sans rien changer au son :
  1. Profil de la charge par style et par brique (synthés, effets, réverbération, mixage) avec `./scripts/host-tests.sh`, `./scripts/live-bench.sh` et `scripts/cpu-bench.sh` ; classement des briques par coût. Le chef met dans la fiche la charge par style lue sur la tablette, pour comparer.
  2. C3 : un test du moteur avec des blocs de tailles inégales, qui dit si l'écart mesuré ment ; s'il ment, mesure corrigée (rouge, puis vert).
  3. Un garde-fou durable : un budget de charge par style dans les tests du moteur, pour qu'une optimisation ne se perde pas plus tard.
- Limite à dire : le classement fait sur Linux vaut pour l'ordre des briques, pas pour les chiffres. La seule preuve de C1 est la tablette.
- Elle rend son tableau et se ferme.

### Suite du moteur (discussions Fable neuves, une par lot, quand le lot d'avant est fusionné)
- **B2, optimiser** : les trois briques les plus chères du tableau de B1, pas plus, puis la vérification complète. Garde-fou : le rendu reste le même à l'oreille (comparaison avant / après sur les rendus hors-ligne), aucun NaN, aucune saturation, rien de neuf qui alloue ou verrouille dans le callback. Preuve : tableau de charge avant / après par style ; écoute en direct sans trou avec le processeur bridé.
- **B3, mode « appareil modeste »** : seulement après B2, et seulement si la tablette coupe encore. D'abord le plus simple, qui existe déjà : le réglage « tampon » (2 par défaut, jusqu'à 8), que D rend automatique quand les coupures montent. Ensuite seulement la qualité allégée (réverbération, nombre de voix), décidée par une mesure atomique, sans allocation ni verrou dans le callback.

### Discussion D : « Service audio » (Opus 5.5)
- Zone : la colle Android du moteur et le côté Kotlin du son (tableau plus haut) ; rien dans le cœur du moteur, ni dans les écrans, ni dans le robot.
- Réparer : C4 (reprise propre au changement de sortie) ; tampon automatique (monter d'un cran quand les coupures s'accumulent, sans redémarrer le son en boucle).
- C5 et C2 : seulement si A1 prouve que le robot est innocent. Si la cause de C2 est dans le cœur (`ClipLauncher`), une ligne au chef, qui la passe à la série B.
- Preuve : rejeu rouge puis vert pour chaque constat. C4 ne se prouve pas sans voiture ni casque : un test simule la perte de la sortie, et Benjamin confirme par le test guidé « Changement de sortie » (section 3).

### Discussion C : « Fluidité » (Opus 5.5)
- Zone : les écrans (tableau plus haut) ; rien dans le moteur ni dans le robot.
- Analyser d'abord : trouver ce qui redessine trop (compteurs de recomposition, traces) sur Mix et Live, pendant la lecture.
- Réparer : C6, Mix et Live en premier. Trois réparations au plus, puis la vérification complète.
- Pas cette nuit : C8 (ce n'était pas l'appli) ; C7 (la mémoire attend le détail du robot v8 : si elle est surtout dans le moteur et les sons chargés, c'est un lot de la série B, pas des écrans).
- C9 : seulement si A1 montre qu'un doigt ne peut pas atteindre le menu.
- Preuve : tests unitaires et captures des seuls écrans touchés ; pas de capture changée sans raison. « Corrigé » attend le compteur réparé du robot v8.

### Chantier F : « Sons actuels » (Benjamin, 2026-11-23 : les sons font « cheap », pour tous les styles)
Recherche faite le 23 novembre (licences lues sur les pages). Vérifié dans le dépôt : les packs de styles sortent bien de Stable Audio Open Small en 8 pas (`scripts/packs/generate_pack.py`), sont fabriqués par `.github/workflows/packs.yml` et publiés sur les Releases ; kicks, 808 et basses sont synthétisés dans `StudioVoices.h` et `DrumKit`.

Conclusions gardées :
- Aucun vendeur ne publie de licence « son dans une appli » : les packs payants (Splice, Loopmasters, 99Sounds, MusicRadar…) interdisent de les mettre dans une appli. Les 100 € restent en réserve, à n'utiliser que contre un accord écrit d'un vendeur.
- Plan à 0 € : synthèse pour les kicks et les basses, sons libres (CC0) pour le reste.

Rien de F ne part cette nuit. Ordre corrigé :

0. **Pilote sur 3 styles d'abord** (techno dure, trap, phonk), pas les 37 d'un coup. « Cheap » est un jugement d'oreille : le juge est Benjamin, qui écoute avant / après sans savoir lequel est lequel. On ne généralise que s'il préfère le neuf.
1. **Synthèse (lot F1)** : kicks modernes et rumble (« peak-time », « hard »), 808 plus saturée, basse « rominimal » refaite, cloche phonk en FM. Ce qui coûte le moins de calcul gagne. Discussion neuve, après la fusion de B2, jamais dans la discussion de B : B promet « le même son à l'oreille », F1 change le son exprès, et les deux touchent les mêmes fichiers (`StudioVoices.h`, `DrumKit`, les rendus de référence, plus `StudioDrums.kt` et `SoundLibrary.kt` côté modèle). Fable seulement si Benjamin le confirme pour ce lot, sinon Opus 5.5. Chaque son neuf passe par le budget de charge de B1 : pas de coupure de plus sur la tablette.
2. **Sons libres Freesound (lot F2)** pour hats, claps, percussions : discussion neuve (Opus 5.5), zone `scripts/packs/**` et `.github/workflows/packs.yml` ; `THIRD_PARTY.md` reste au chef (F2 écrit dans sa note). Règles :
   - Choisir à l'écoute sur les extraits (la clé simple suffit), puis ne télécharger les originaux que pour les sons gardés : quelques centaines au plus, donc la limite de 500 par jour ne gêne pas. Les extraits sont compressés : jamais dans un pack.
   - Les originaux demandent que Benjamin se connecte une fois à Freesound (autorisation OAuth2, à refaire si elle expire).
   - Téléchargés une seule fois, puis rangés sur une Release à part, comme les packs. La CI ne parle jamais à Freesound : pas de secret dans la CI, pas de limite au moment de construire.
   - Dépôt public : aucun son dans git, aucune clé dans le dépôt ni dans les journaux. Dans le dépôt, seulement une liste : numéro du son, auteur, adresse, licence lue le jour du téléchargement, empreinte du fichier.
   - « CC0 » sur Freesound est la parole de celui qui a déposé le son. On refuse tout son dont la description cite un disque, un pack du commerce ou un CD d'échantillons, et tout ce qui ressemble à un extrait de morceau.
   - Retirés du plan pour cette raison : les breaks connus (Amen et les autres viennent de disques), le grincement de lit de la Jersey Club (le son célèbre vient d'un disque du commerce) et les voix découpées de chansons. À la place : breaks programmés avec des frappes libres, grincement enregistré ou synthétisé, voix CC0 déjà au dépôt (Kenney) ou synthèse.
   - Taille : 73 Mo aujourd'hui ; 20 Mo de plus au maximum, frappes en mono 16 bits.
3. **Refaire les packs avec une IA plus grosse : pas maintenant.** Les machines de la CI n'ont pas de carte graphique, le gain sur des frappes de batterie n'est pas prouvé, et la licence du modèle plus gros est à relire avant. Seulement comme essai sur un pack, si F1 et F2 ne suffisent pas à l'oreille de Benjamin.
4. Écoute avant / après de chaque style touché, par le chemin d'écoute en direct, avant d'entrer dans l'appli.

## 3. Nouveaux tests du robot (v8, lot A2, après A1)

Le robot vit dans l'appli : il ne peut ni brancher un casque, ni passer un appel, ni allumer le mode économie. Ces tests-là sont « guidés » : le robot dit à Benjamin le geste à faire et mesure. Ils sont dans un paquet à part, jamais dans la nuit sans surveillance.

| Test | Ce qu'il lit | Comment | Attend |
|---|---|---|---|
| Live 30 secondes | le moteur joue encore à 30 s, sans que le robot ait touché | seul (c'est la réparation de C2) | A1 |
| Délai du son | la permission du micro demandée avant | seul (existe déjà, réparation de C5) | A1 |
| Images lentes par écran | les seuils existent déjà (5 % et 15 %) ; neuf : rythme réel de l'écran et écran témoin | seul | A1 |
| Endurance | 10 minutes de lecture (2 aujourd'hui) : coupures, mémoire au début et à la fin | seul | A1 |
| Fuite mémoire | 20 ouvertures et fermetures de projet : la mémoire ne monte pas de plus de 20 % | seul | A1 (mémoire détaillée) |
| Démarrage à froid | temps jusqu'au premier écran utilisable | seul, après une relance | C12 |
| Sauvegarde après arrêt brutal | le projet rouvert est identique | seul : le robot tue l'appli puis se relance | C12 |
| Perte du son au profit d'une autre appli | le robot prend lui-même le son un instant (comme une notification) : ni silence durable ni plantage au retour | seul. Un vrai appel ne se simule pas : test guidé si Benjamin le veut | |
| Tampon large (appareil modeste) | charge et coupures avec le tampon à 8, comparées au tampon normal | seul (le réglage existe déjà) ; le mode allégé s'ajoute après B3 | |
| Export vidéo | existe déjà (fichier, durée, crête, format 9:16) ; neuf : le fichier contient du son | seul | |
| Changement de sortie | le son reprend en moins de 2 s après un passage haut-parleur ↔ Bluetooth ou voiture | guidé | D |
| Économie d'énergie | lecture sans coupure quand le téléphone est en mode économie | guidé (le robot vérifie que le mode est bien allumé) | |

### Le grand catalogue (lot A3, après l'audit E ; Benjamin, 2026-11-23 : « genre 500 scénarios […] t'en fais moins si tu juges que j'exagère »)

Choix du chef : viser environ 400 scénarios (131 aujourd'hui), pas 500 à tout prix : un scénario n'entre que s'il a un oracle précis et s'il peut trouver une faute qu'aucun autre ne trouve. Ils sont fabriqués par croisement, pas écrits un par un :
- chaque style × (lecture, changement de tempo, export) ;
- chaque effet × (ajout, réglage aux deux bouts, retrait, annuler) et les paires d'effets les plus lourdes ;
- chaque écran × (rotation, arrière-plan, fermeture brutale) ;
- chaque geste de la structure et du piano roll × annuler / rétablir ;
- les cas limites : tempo le plus bas et le plus haut, projet vide, projet énorme, 16 pistes, noms très longs, stockage presque plein.

Règles ajoutées par l'audit :
- Le catalogue n'est pas pour cette nuit. Cette nuit : l'audit E et les réparations A1. Le catalogue vient ensuite, dans une discussion neuve.
- Chaque scénario part d'un projet propre (la leçon de C10) et passe d'abord sur les vrais écrans dans les tests.
- Durée honnête : 131 scénarios tiennent en 65 minutes au plus, donc 400 font environ 3 h 30 sur le téléphone, plus sur la tablette. Paquets par thème de 30 minutes au plus (Son, Effets, Écrans, Structure, Live, Exports, Limites, Endurance), enchaînés tout seuls, plus « Rapide », « Refaire les ratés » et « Guidés ». Le tout passe en une nuit, téléphone branché.

Les tests du robot (`app/src/test/java/com/pocketstudio/bentest/`) sont verts avant tout envoi ; `scripts/bench/selftest.sh` reste la règle pour le banc des testeurs.

## 4. Ordre de marche

0. Avant 2 h 05 : le chef met `EN-COURS.md` à jour (les cinq discussions, leurs zones) et relit C2 dans le rapport de la tablette.
1. 2 h 05 : le chef ouvre E, A1, B1, C et D avec leurs fiches. Toutes commencent par l'analyse et rendent une page de conclusions au chef avant de réparer.
2. Réparations : « rouge: sujet » puis « vert: même sujet », relecture neuve en dernier commit, CI complète verte sur une tête qui contient l'intégration.
3. Fusion par paquets de trois lots au plus, CI verte entre deux paquets, téléphone simulé compris :
   - paquet 1 : `eq/bentest-robot-2` avec les réparations A1, et le lot fable5 (`ccr-56d43d5b-vfdmoc`, 0758fd6cf) avec la décision de seuil notée dans `DECISIONS.md` ;
   - paquet 2 : B1 (mesure corrigée, budget de charge), D, C ;
   - ensuite, un lot à la fois : B2, A2, B3, A3, F1, F2.
4. Le chef compile le robot v8 en local (exception notée dans `DECISIONS.md` le 22 novembre : c'est un autre paquet que l'appli), mais seulement depuis une tête d'intégration dont la CI est verte, et il écrit le commit joué dans le rapport.
5. Benjamin rejoue sur le téléphone et sur la tablette, son à moitié, sans toucher. Le chef compare avant / après, constat par constat.
6. « Corrigé » ne se dit que pour un constat que le robot v8 a vu passer sur l'appareil ; sinon « tenté ». Tant que le compteur d'un constat n'est pas prouvé (C3, C6, C7), on dit « mesure à refaire », ni « corrigé » ni « bug ».

Ce qui est réaliste pour cette nuit : l'audit E, les réparations du robot (paquets 1 et 2 de A1 au moins), le tableau de charge de B1, C4 et le tampon automatique chez D, Mix et Live chez C. Pas cette nuit : les tests neufs, le catalogue, les sons.

## 5. Améliorations de l'ensemble

- Le robot devient le juge habituel : un passage rapide avant chaque version envoyée.
- Un tableau avant / après par appareil, gardé hors dépôt, pour voir les progrès d'un passage à l'autre.
- Les seuils (écart entre blocs, images lentes, mémoire, délai du son) sont écrits et justifiés dans `DECISIONS.md`, pas dans le code seul. La mémoire n'a pas encore de seuil : il se décide après le premier détail du robot v8.
- Chaque compteur a son témoin : une situation connue qui doit lire « bon ». Un compteur sans témoin ne fait pas un rouge.
- La tablette devient l'appareil de référence « modeste » : ce qui passe chez elle passe partout.

## 6. Ce que Benjamin est seul à pouvoir faire

- Maintenant, cinq minutes, sur la tablette : Réglages de l'appli, tampon au plus grand, puis rejouer « Décrochages » du robot. S'il n'y a plus de coupure, le remède rapide est le tampon automatique ; s'il en reste, c'est bien la charge du moteur.
- Installer le robot v8 sur le téléphone et la tablette, lancer le test, envoyer les deux rapports.
- Pendant le test : son à moitié, pas de voiture, ne pas fermer l'appli.
- Les tests guidés, quand ils existeront : brancher un casque Bluetooth ou la voiture, allumer le mode économie, quand le robot le demande.
- Pour le chantier F (pas avant le pilote) : créer un compte gratuit Freesound et une clé d'accès, à donner par le formulaire sécurisé, jamais dans la conversation ; se connecter une fois quand on le lui demande, pour les originaux ; écouter les 3 styles pilotes avant / après.

## 7. Audit Fable (2026-11-23)

Relecture critique par un auditeur neuf, code lu sur `eq/bentest-robot-2` (2c0cab99c) et sur l'intégration. Chaque changement et sa raison :

1. **C8 passe de « appli » à « robot ».** `buildUpBefore` ajoute une courbe de montée, jamais une partie ; le scénario `partie_montee` attendait une partie de plus. Sans cela, C aurait cherché toute la nuit un bug qui n'existe pas.
2. **C11 passe de « à confirmer » à « sûr ».** Le robot cherche `live_pad_<numéro>` ; l'écran du Live nomme ses cases `live_pad_<piste>_<ligne>` (`LiveScreen.kt`). L'ancien nom n'existe que sur les vieux pads.
3. **C2 passe de « appli » à « à relire ».** Le scénario du Live écoute 4 s par construction et le robot arrête le Live lui-même. Retiré des réparations de D ; remplacé par le test « Live 30 secondes ».
4. **C3 devient « mesure douteuse ».** L'écart est divisé par la durée du bloc d'avant et la taille des blocs n'est pas fixée ; le tampon vaut 2 blocs par défaut. B1 le prouve par un test, A1 change le feu. On n'optimise pas le moteur pour un chiffre qui ment.
5. **C5 passe à « robot d'abord ».** Le robot lance la mesure sans vérifier la permission, alors qu'il utilise lui-même le micro.
6. **C6 et C7 : compteurs à prouver.** Seuil d'images fixe à 16,7 ms, sans témoin ; mémoire totale, robot compris, sans seuil ni détail. C7 sort de la zone des écrans tant qu'on ne sait pas où est la mémoire.
7. **C15 ajouté** : le délai de 958 ms noté dans `EN-COURS.md` manquait au tableau.
8. **Zones refaites, avec un tableau.** `engine/**` entier chez B empêchait D de réparer C4 et C5, qui sont dans `engine/src/main/cpp/android/` ; C et D se croisaient dans `RhythmViewModel.kt` ; le robot vit aussi dans `app/src/journey/**`, oublié ; `packs.yml` manquait à F ; `THIRD_PARTY.md` appartient au chef (`CARTE-CODE.md` §3).
9. **B découpé en B1 (mesure), B2 (optimiser), B3 (mode modeste).** Une seule discussion ne pouvait pas tout porter (règle de la mémoire courte, « un lot = une discussion », Fable sur fiche courte, trois réparations au plus). Le mode modeste commence par le réglage de tampon qui existe déjà.
10. **F1 sorti de la discussion de B.** B promet le même son, F1 le change : dans le même lot, la comparaison avant / après ne voudrait plus rien dire.
11. **F durci** : pilote sur 3 styles jugé à l'oreille par Benjamin ; originaux rangés une fois sur une Release, la CI ne parle pas à Freesound ; liste d'origine par son ; breaks connus, grincement Jersey Club et voix de chansons retirés (ils viennent de disques, même sous une étiquette CC0) ; plafond de taille ; pas d'IA plus grosse pour l'instant ; connexion OAuth2 de Benjamin ajoutée à sa liste.
12. **A découpé en A1 (réparations), A2 (tests neufs), A3 (catalogue).** Réparer six constats, corriger trois compteurs, écrire douze tests et 270 scénarios en une nuit n'était pas réaliste, et le catalogue doit attendre l'audit E.
13. **Tests neufs triés** : quatre des douze existaient déjà (délai du son, images lentes avec seuils, export vidéo, lecture longue) ; trois ne peuvent pas être joués par un robot qui vit dans l'appli (changement de sortie, appel, mode économie) et deviennent « guidés ».
14. **Deux gardes ajoutées** : passage obligatoire sur les vrais écrans dans les tests avant le téléphone ; plus de 5 % de « non mesuré » = échec du robot.
15. **Durée du catalogue corrigée** : 400 scénarios font environ 3 h 30, ce qui ne tient pas en huit paquets de 20 minutes.
16. **Règles du projet rappelées** : cinq discussions et non quatre ; E manquait à l'étape 1 ; le robot v8 ne part que d'un commit à la CI verte ; les constats de l'appli entrent dans la liste gelée du tour 9 ; `EN-COURS.md` à mettre à jour avant le départ.
17. **Un test de cinq minutes pour Benjamin** (tampon au plus grand sur la tablette) qui dit tout de suite si les coupures viennent du tampon ou de la charge.

Ce que l'audit n'a pas pu vérifier : les rapports des appareils (hors dépôt), donc la phrase exacte de C2 et le nombre de coupures des 4 styles de C3 ; le contenu du lot fable5 (`0758fd6cf`, absent de cette machine) ; les limites de Freesound (500 par jour, OAuth2) et la licence du modèle d'IA plus gros, reprises de la recherche ; les chiffres « 53 packs, 73 Mo ».
