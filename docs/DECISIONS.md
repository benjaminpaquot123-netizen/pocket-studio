# Journal des décisions

| Date | Décision | Raison | Retour arrière |
| --- | --- | --- | --- |
| 2026-10-18 | targetSdk/compileSdk 37 (Android 17), minSdk 31 | Dernière version stable, imposé par la spec | Changer `compileSdk`/`targetSdk` dans les `build.gradle.kts` |
| 2026-10-18 | AGP 9.4.1, Gradle 9.8.1, Kotlin 2.4.21, NDK r30 | Dernières versions stables au démarrage | Versions dans `gradle/libs.versions.toml` et wrapper |
| 2026-10-18 | Oboe via dépendance Maven (prefab) plutôt que sous-module | Build plus rapide, mises à jour en une ligne | Passer en sous-module git si un patch d'Oboe devient nécessaire |
| 2026-10-18 | Modules `project-model`, `dsp`, `separation` créés à leur phase | Pas de module vide à maintenir | Aucun |
| 2026-10-18 | Signature : keystore PKCS12 chiffré dans le dépôt + mot de passe aléatoire (28 caractères) en secret GitHub `SIGNING_PASSWORD` | Un seul secret court à saisir au lieu d'un long fichier en base64 ; sans le mot de passe, le fichier est inexploitable | Régénérer une clé (oblige à désinstaller une fois) |
| 2026-10-18 | versionCode = numéro d'exécution de la CI ; tag `v0.<phase>.<versionCode>` | Toujours croissant, aperçus et stables s'installent les uns sur les autres | Aucun |
| 2026-10-18 | Mise à jour intégrée via l'API GitHub `releases/latest` + PackageInstaller | Pas de serveur à maintenir, dépôt public | Désactiver `UpdateChecker` |
| 2026-10-18 | Rapports via lien « nouvelle issue » pré-rempli (pas de jeton dans l'appli) | Aucun secret dans l'APK, l'envoi reste un geste volontaire | Aucun |
| 2026-10-18 | Mesure de latence par salve de bruit fenêtrée + corrélation croisée, 3 mesures, médiane | Robuste au bruit ambiant, testable sur Linux avec un retard simulé | Remplacer par la méthode « pulse » d'OboeTester |
| 2026-10-18 | Buffer = 2 bursts | Compromis classique latence/sûreté ; à ajuster avec les mesures de décrochages | `setBufferSizeInFrames` dans `AudioHost.cpp` |
| 2026-10-18 | Captures d'écran Roborazzi enregistrées dans `app/screenshots/` et commitées | Visibles directement dans la pull request | Ne plus les commiter, garder l'artefact CI |
| 2026-10-18 | Couleurs : cyan #00E5FF, magenta #FF2BD6, vert acide #B6FF3B sur fond #0A0A12 | Thème sombre néon demandé | `ui-components/.../Theme.kt` |
| 2026-10-18 | Jalon « Acide » (Open303, MIT) inséré entre la phase 1 et la phase 2 | Décision de Benjamin (D5) | Déplacer le jalon dans ROADMAP.md |
| 2026-10-18 | Aucun nom de marque Roland dans l'interface : on parle de « basse acide » | Demande de Benjamin | Aucun |
| 2026-10-18 | Revue critique par un sous-agent neuf à chaque fin de phase | Règle permanente demandée par Benjamin | Aucun |
| 2026-10-18 | Mesure de latence : entrée micro enregistrée bout à bout (plus de remplissage par des zéros), 5 configurations de micro essayées tour à tour, cause d'échec précise affichée et jointe aux rapports | Échec « signal non détecté » sur le Honor sans cause identifiable | Revenir à l'alignement trame par trame dans `RoundTripMeter::process` |
| 2026-10-18 | Micro lu par son propre callback Oboe vers une file sans verrou (`SpscFloatRing`) au lieu de `read()` depuis le callback de sortie ; configuration suivante essayée si le micro ne livre rien | Sur le Honor, le micro s'ouvrait mais `read()` depuis le fil de sortie renvoyait 0 échantillon | Revenir à `read()` non bloquant dans `AudioHost::onAudioReady` |
| 2026-10-18 | Mesure de latence : haut-parleur démarré avant le micro, attente que le micro livre vraiment, coupures de flux détectées ; 5 combinaisons micro/haut-parleur dont des modes partagés, jusqu'à 6 essais | Sur le Honor, démarrer le micro coupait le flux exclusif du haut-parleur après ~20 ms | Revenir à un seul réglage exclusif dans `kLatencyConfigs` |
| 2026-10-18 | Correction de diagnostic : les échecs de latence venaient d'une course entre l'interface et le moteur (l'interface lisait « pas en cours » avant que le moteur ait pris l'ordre, et fermait tout). `startLatencyTest` attend désormais que la mesure tourne ; l'interface attend « terminé ». La rotation de réglages et la détection de coupure sont conservées par sécurité | Une seule mesure réussie sur ~20 sur le Honor (43,6 ms) | Aucun : c'était un bug |
| 2026-10-18 | `project-model` en Kotlin pur (sans Android), kotlinx.serialization, projet immuable | Tests rapides sur la JVM ; l'immuabilité rend annuler/rétablir et la sauvegarde triviaux et sûrs | Aucun |
| 2026-10-18 | Sauvegarde : fichier temporaire + fsync, copie de secours `.bak`, remplacement atomique ; lecture de secours sur le `.bak` | Aucune perte même si l'appli est tuée en pleine écriture | Aucun |
| 2026-10-18 | Design « moderne mais pas trop, facile à prendre en main » : palette adoucie (gris-bleu, cyan doux, rose pour « ça joue »), règles dans `docs/DESIGN.md` | Choix de Benjamin | Couleurs dans `ui-components/.../Theme.kt` |
| 2026-10-18 | Palette par défaut `Palettes.Studio` : inspirée de FL Studio Mobile en plus clair (gris moyens neutres, accent orange, vert pour « ça joue ») ; palettes interchangeables via `LocalPalette` | Demande de Benjamin (« couleurs plus claires », « exemple FL Studio Mobile ») | Changer la palette par défaut dans `Theme.kt` |
| 2026-10-18 | Design délégué : Benjamin valide le look « FL Studio Mobile en plus clair » et laisse les choix de design à l'agent, sans question | « Tu as toute ma confiance pour le design » | Benjamin peut reprendre la main à tout moment |
| 2026-10-18 | DaisySP en sous-module git figé (2c72eaf), seuls les modules utiles sont compilés | Règle d'or ; versions maîtrisées | Changer le commit du sous-module |
| 2026-10-18 | `rand()` de DaisySP redirigé vers un générateur sans verrou (`-include daisysp_rand_override.h` + `core/ps_rand.c`) ; `host-tests.sh` échoue si `rand`/`random` de la libc réapparaît | Le `rand()` de bionic prend un verrou, interdit dans le fil audio ; sans modifier DaisySP | Retirer l'override si DaisySP change de générateur |
| 2026-10-18 | Durée maximale par son de batterie avec fondu de 60 ms, puis mise en veille | La caisse claire analogique de DaisySP garde un souffle constant ; économise aussi le processeur | Allonger `kMaxSeconds` dans `DrumKit.cpp` |
| 2026-10-18 | Nombres dénormaux mis à zéro pendant chaque callback audio (`DenormalGuard`) | Le kit complet passait de 15,5 % à 5,9 % du temps réel sur x86 | Aucun |
| 2026-10-18 | Clap, rimshot, cloche et shaker faits maison (bruit + filtres), le reste en voix DaisySP | DaisySP n'a pas ces sons | Remplacer par des voix d'une autre brique si meilleure |
