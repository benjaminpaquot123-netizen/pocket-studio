# Plan d'action après les passages du robot « BenStudio Test »

Demandé par Benjamin le 2026-11-23 (« Prévois le plan d'action avec tous les nouveaux tests […] comment analyser et réparer, et des améliorations de l'ensemble »). Démarrage : 2026-11-24 à 2 h 05 (heure belge), à plein quota, quatre discussions neuves : trois en Opus 5.5 et une en Fable 5.2 pour le moteur son.

## 1. D'où on part (constats mesurés)

Rapports gardés hors dépôt (bloc-notes du chef) : téléphone Xiaomi Android 17 (v6 complet, puis v7 « Refaire les ratés », son allumé) et tablette Samsung SM-X230 Android 16 (v6 rapide).

| N° | Constat | Appareil | Vient de |
|---|---|---|---|
| C1 | 87 coupures du son, charge moyenne 83 à 94 %, pire bloc à 226 % | tablette | appli (trop gourmande) |
| C2 | Le Live joue 4 s puis s'arrête seul | tablette | appli, à confirmer |
| C3 | 4 styles (acid techno, acidcore, drill, boom bap) : trou de 4,1 à 4,6 blocs pour 4 permis | téléphone | appli ou seuil, à trancher |
| C4 | Coupures quand le son part vers Android Auto | téléphone | appli (changement de sortie) |
| C5 | Délai du son non mesuré : « MicUnavailable » | téléphone | appli ou robot, à trancher |
| C6 | Images lentes : Mix et Live 100 %, Projets 51 %, Sons 39 %, Structure 28 à 42 % | les deux | appli |
| C7 | Mémoire : 281 à 487 Mo | les deux | appli |
| C8 | « Ajouter une montée » n'ajoute pas de partie (26 avant, 26 après) | téléphone | appli, à confirmer |
| C9 | Annuler puis rétablir : menu de la partie introuvable sur le téléphone, passe sur la tablette | téléphone | robot (petit écran) |
| C10 | Égaliseur refusé : trois effets déjà sur la piste | téléphone | robot (piste pas nettoyée) |
| C11 | Pads du Live introuvables (8 scénarios non mesurés) | les deux | robot, à confirmer |
| C12 | Fermer / rouvrir et arrière-plan non faisables | tablette | robot |
| C13 | Lecture longue comptée « plantage » quand Benjamin ferme l'appli à la main | téléphone | robot |
| C14 | Volume à 2 sur 15 : compté coupé, mais le test démarre quand même | tablette | robot |

## 2. Quatre discussions, quatre zones qui ne se touchent pas

Chaque discussion reçoit une fiche fermée (gabarit `GABARIT-FICHES-V3.md`) : ses fichiers, ses constats, la preuve attendue. Aucune ne pousse sur l'intégration ; le chef fusionne par le portail.

### Discussion A : « Robot » (l'outil de mesure d'abord)
- Zone : `app/src/benTest/**`, `app/src/test/java/com/pocketstudio/bentest/**`, `scripts/bentest/**`. Base : `eq/bentest-robot-2` (2c0cab99c).
- Réparer : C9, C10, C11, C12, C13, C14.
- Confort : bandeau « Test long en cours, ne touche à rien » ; refus de démarrer si le volume est trop bas, avec le geste à faire ; le pack « Refaire les ratés » se calcule tout seul depuis le dernier rapport (plus de liste écrite à la main).
- Nouveaux tests : section 3.
- Preuve : tests du robot verts ; chaque réparation a son test rouge avant, vert après.

### Discussion B : « Moteur son » (Fable 5.2, décision de Benjamin du 2026-11-23 : « il faut vraiment qu'il se consacre à ça, et donc en Fable »)
- Zone, à elle seule : `engine/**` (cœur C++ et tests Linux). Personne d'autre n'y touche.
- But : optimiser le moteur son au maximum, pour qu'une tablette d'entrée de gamme joue tous les styles sans coupure.
- Analyser d'abord, sans rien changer : profil de la charge par style et par brique (synthés, effets, réverbération, mixage) sur `./scripts/host-tests.sh` et `./scripts/live-bench.sh` avec un processeur bridé ; classement des briques par coût.
- Réparer et optimiser : C1 et C3 ; calcul vectorisé, tables au lieu de calculs répétés, voix inutiles coupées, effets au repos qui ne coûtent rien ; mode « appareil modeste » (tampon plus grand, qualité allégée) déclenché par une mesure atomique, sans allocation ni verrou dans le callback.
- Garde-fou : le rendu reste le même à l'oreille (comparaison avant / après sur les rendus hors-ligne), aucun NaN, aucune saturation.
- Preuve : tableau de charge avant / après par style ; écoute en direct sans trou avec le processeur bridé.

### Discussion D : « Service audio » (Opus 5.5)
- Zone : le côté Kotlin du son (service audio, sorties, micro) ; rien dans `engine/**`, ni dans les écrans, ni dans `benTest`.
- Réparer : C2 (Live qui s'arrête seul), C4 (reprise propre au changement de sortie), C5 (micro indisponible).
- Preuve : rejeu rouge puis vert pour chaque constat.

### Discussion C : « Fluidité et mémoire »
- Zone : les écrans Compose (Mix, Live, Projets, Sons, Structure) et leurs modèles ; rien dans `engine/**` ni dans `benTest`.
- Analyser d'abord : trouver ce qui redessine trop (compteurs de recomposition, traces) sur Mix et Live en premier.
- Réparer : C6, C7, C8, et « Rétablir » si C9 révèle aussi une faute de l'appli.
- Preuve : tests unitaires et captures des seuls écrans touchés ; pas de capture changée sans raison.

## 3. Nouveaux tests du robot (v8)

| Test | Ce qu'il lit |
|---|---|
| Appareil modeste | charge et coupures avec le mode allégé, comparées au mode normal |
| Changement de sortie | le son reprend en moins de 2 s après un passage haut-parleur ↔ Bluetooth ou voiture |
| Interruption | une notification ou un appel simulé ne laisse ni silence ni plantage |
| Endurance | 10 minutes de lecture : coupures, mémoire au début et à la fin |
| Fuite mémoire | 20 ouvertures et fermetures de projet : la mémoire ne monte pas de plus de 20 % |
| Démarrage à froid | temps jusqu'au premier écran utilisable |
| Live 30 secondes | le Live joue 30 s sans s'arrêter seul |
| Sauvegarde après arrêt brutal | le projet rouvert est identique |
| Images lentes par écran | un seuil par écran, au lieu d'un simple constat |
| Délai du son | avec la permission du micro demandée avant |
| Économie d'énergie | lecture sans coupure quand le téléphone est en mode économie |
| Export vidéo | le fichier existe, dure la bonne durée, contient du son |

### Le grand catalogue (Benjamin, 2026-11-23 : « genre 500 scénarios […] t'en fais moins si tu juges que j'exagère »)

Choix du chef : viser environ 400 scénarios (131 aujourd'hui), pas 500 à tout prix : un scénario n'entre que s'il a un oracle précis et s'il peut trouver une faute qu'aucun autre ne trouve. Ils sont fabriqués par croisement, pas écrits un par un :
- chaque style × (lecture, changement de tempo, export) ;
- chaque effet × (ajout, réglage aux deux bouts, retrait, annuler) et les paires d'effets les plus lourdes ;
- chaque écran × (rotation, arrière-plan, fermeture brutale) ;
- chaque geste de la structure et du piano roll × annuler / rétablir ;
- les cas limites : tempo le plus bas et le plus haut, projet vide, projet énorme, 16 pistes, noms très longs, stockage presque plein.

Rangés en paquets par thème de 20 minutes au plus (Son, Effets, Écrans, Structure, Live, Exports, Limites, Endurance), plus « Rapide » et « Refaire les ratés ». Le tout passe en une nuit, téléphone branché.

Chaque test neuf a son oracle précis et passe d'abord sur le banc (`scripts/bench/selftest.sh` vert) avant d'aller sur le téléphone.

## 4. Ordre de marche

1. 2 h 05 : le chef ouvre A, B, C et D avec leurs fiches. Toutes commencent toutes par l'analyse et rendent une page de conclusions au chef avant de réparer.
2. Réparations : « rouge: sujet » puis « vert: même sujet », relecture neuve en dernier commit, CI complète du lot verte sur une tête qui contient l'intégration.
3. Fusion par paquets de trois lots au plus, CI verte entre deux paquets. D'abord `eq/bentest-robot-2` et le lot fable5 (`ccr-56d43d5b-vfdmoc`, 0758fd6cf), avec la décision de seuil notée dans `DECISIONS.md`.
4. Le chef compile le robot v8 en local et l'envoie à Benjamin.
5. Benjamin rejoue sur le téléphone et sur la tablette, son à moitié, sans toucher. Le chef compare avant / après, constat par constat.
6. « Corrigé » ne se dit que pour un constat que le robot v8 a vu passer sur l'appareil ; sinon « tenté ».

## 5. Améliorations de l'ensemble

- Le robot devient le juge habituel : un passage rapide avant chaque version envoyée.
- Un tableau avant / après par appareil, gardé hors dépôt, pour voir les progrès d'un passage à l'autre.
- Les seuils (4 blocs, images lentes, mémoire) sont écrits et justifiés dans `DECISIONS.md`, pas dans le code seul.
- La tablette devient l'appareil de référence « modeste » : ce qui passe chez elle passe partout.

## 6. Ce que Benjamin est seul à pouvoir faire

- Installer le robot v8 sur le téléphone et la tablette, lancer le test, envoyer les deux rapports.
- Pendant le test : son à moitié, pas de voiture, ne pas fermer l'appli.
