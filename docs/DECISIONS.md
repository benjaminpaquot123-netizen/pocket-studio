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
