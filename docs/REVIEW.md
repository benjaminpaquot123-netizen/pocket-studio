# Revues de fin de phase

## Phase 0 (2026-10-18)
Relecteur : sous-agent neuf, n'ayant pas écrit le code. Bilan : 4 graves, 9 moyens, 10 mineurs. Callback audio jugé sain (aucune allocation, verrou, log ni JNI ; pas de course entre threads).

### Corrigé
| # | Gravité | Problème | Correction |
| --- | --- | --- | --- |
| 2 | Grave | Le test émulateur passait même sans son | Exige flux démarré, callbacks > 0, crête > 0,1 après Jouer, pas de NaN ; émulateur lancé avec l'audio |
| 3 | Grave | Tests anti-clic incapables d'échouer | Nouveaux tests à 1 kHz plein volume, arrêt sur une crête, rampe vérifiée à 1 ms ; vérifié qu'un moteur sans lissage les fait échouer |
| 4 | Grave | UBSan ne faisait jamais échouer la CI | `-fno-sanitize-recover=all` |
| 5 | Moyen | Micro pouvant rester ouvert en arrière-plan après une mesure interrompue | Mesure annulée à la sortie de l'appli, fermeture garantie (`finally`), le moteur refuse d'ouvrir le micro si l'appli n'est plus visible |
| 6 | Moyen | La hauteur revenait à 440 Hz après une mesure | `Engine::prepare` conserve fréquence, volume et lecture ; test dédié |
| 7 | Moyen | Son mort si la réouverture échoue après un (dé)branchement | 5 tentatives espacées de 200 ms |
| 8 | Moyen | `start()` disait « OK » sur un flux arrêté | Vérifie l'état, ferme et réessaie |
| 9 | Moyen | Écran figé pendant l'ouverture des flux (lecture des stats) | `info()` ne bloque plus : renvoie la dernière valeur si le verrou est pris |
| 10 | Moyen | Sessions d'installation abandonnées qui s'accumulent | `abandon()` en cas d'échec, APK temporaire supprimé |
| 11 | Moyen | Plantages natifs et blocages (ANR) non signalés | Lus dans l'historique système au lancement suivant |
| 12 | Moyen | Publication possible malgré un test émulateur rouge | Job `publish` qui attend `build` et `smoke` |
| 13 | Moyen | Test de latence moteur ne vérifiait que l'échec | Boucle simulée (retard 777, bruit) à travers `Engine::process`, ±1 échantillon, retour du son vérifié |
| 14 | Mineur | Compteur de décrochages qui reculait | Cumul conservé à la reconnexion |
| 15 | Mineur | Ancienne demande de mesure rejouée | `prepare()` l'écarte ; test dédié |
| 16 | Mineur | Entrée multicanal = dépassement de tampon | Entrée refusée si elle n'est pas mono |
| 17 | Mineur | Sans navigateur : plantage en boucle | Message au lieu du plantage, rapport effacé avant l'ouverture |
| 18 | Mineur | Connexions HTTP non fermées, code HTTP non vérifié | `disconnect()` et exigence d'un 200 |
| 20 | Mineur | CI : forks, annulation pendant une Release, titre de PR injecté dans le shell | Forks sans publication, pas d'annulation sur main, titre passé par variable d'environnement |
| 21 | Mineur | Tests Linux compilés autrement que sur le téléphone | Mêmes options (`-O3 -ffast-math`) |

### Non retenu
| # | Problème signalé | Décision |
| --- | --- | --- |
| 1 | Keystore de signature dans le dépôt public | Maintenu. Fichier chiffré en AES-256 (PBKDF2-SHA256), mot de passe aléatoire de 28 caractères (~166 bits) gardé en secret GitHub : force brute hors de portée. Évite de saisir un long fichier base64 sur téléphone. À revoir si le mot de passe fuit. |

### Restant (à traiter plus tard)
- `start()`/`stop()` du flux appelés sur le thread principal : à déplacer sur un thread dédié en phase 1 (service de lecture).
- Confirmation d'installation perdue si l'appli est en arrière-plan pendant le téléchargement : ajouter une notification de secours.
- Une seule pré-release `preview` partagée par toutes les PR (suffisant avec une PR par phase).
- versionCode = numéro d'exécution : repartirait de 1 si le workflow était recréé.
- Corrélation de latence en force brute (~100 ms sur téléphone) : FFT ou fenêtre de recherche réduite si besoin.
- La rotation de l'écran coupe brièvement le son : disparaîtra avec le service de lecture (phase 1).
- Captures d'écran enregistrées mais jamais comparées : comparaison à activer quand l'interface sera stabilisée.
- Pas encore de tests pour la logique de reconnexion d'`AudioHost` (dépend d'Android) ni pour `LatencyViewModel`.
