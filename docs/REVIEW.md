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

## Phase 1 (2026-10-18)
Relecteur : sous-agent neuf, n'ayant pas écrit le code (89 fichiers). Bilan : 2 graves, 12 mineurs. Callback audio jugé sain de bout en bout (séquenceur, batterie, Open303, synthé, mixeur : aucune allocation, verrou, log ni JNI) ; poignée native de l'export sans risque d'usage après libération.

Pendant la phase, le test émulateur a aussi attrapé un plantage du bouton Exporter (fonctions JNI `internal` renommées par Kotlin) : corrigé, et `scripts/check-jni.sh` vérifie désormais en CI que chaque fonction native a son code C++.

### Corrigé
| # | Gravité | Problème | Correction |
| --- | --- | --- | --- |
| 1 | Grave | Note du synthé coincée si le geste est annulé (octave changée doigt posé, changement d'onglet, sortie de l'appli) | `noteOff` dans un `finally` ; tout le synthé relâché en quittant l'onglet ou l'appli |
| 2 | Grave | Projet illisible remplacé en silence par le projet de départ, puis écrasé à la première sauvegarde | Le dossier est mis de côté (`projet-1-illisible-…`) et un message prévient Benjamin ; test Robolectric |
| 4 | Mineur | Renommage et copie de secours non synchronisés sur disque | fsync de la copie et du dossier |
| 5 | Mineur | Mixeur non réparé au chargement (fichier abîmé → plantage) | `Mixer.sanitized()` : 16 tranches, volume et pan bornés ; test |
| 7 | Mineur | Lecture/arrêt très rapides : risque de plantage du service de premier plan | `startForeground` dès `onCreate` |
| 8 | Mineur | Générateur aléatoire partagé entre le fil audio et l'export | État par fil (`_Thread_local`) |
| 10 | Mineur | Échec de préparation du fichier d'export : export bloqué jusqu'au redémarrage | Fichier préparé avant de créer le moteur hors-ligne, message d'erreur |
| 11 | Mineur | Un nouvel export effaçait un fichier encore en cours de partage | Seuls les exports de plus d'une heure sont effacés |
| 3 | Mineur | Erreur de sauvegarde jamais affichée ; une annulation comptée comme erreur | Bandeau orange en haut tant que la sauvegarde échoue ; annulation relancée, pas notée (2026-10-19) |

### Reste à faire (noté, non bloquant)
| # | Problème | Piste |
| --- | --- | --- |
| 6 | Retour de `nativePost` ignoré : ~1 200 commandes à chaque synchro, le moteur peut diverger si la file déborde flux coupé | Commande groupée « charger le motif » ou resynchro signalée |
| 9 | Annuler illimité : la mémoire grandit sur de très longues sessions | Gardé illimité (exigé par la SPEC) : les états immuables partagent leurs parties inchangées, quelques Ko par étape ; à mesurer quand les samples arriveront (phase 2) |
| 12 | Tête de lecture acide = pas batterie modulo longueur acide | Lire le pas acide depuis le moteur quand les longueurs deviendront réglables |
| 13 | `transportRunning()`, `synthActiveVoices()`, `acidNotesPlayed()` non atomiques | Réservés aux tests ; les rendre atomiques avant tout usage UI |
| 14 | Tests : la sauvegarde en arrière-plan passerait sans `flush()` ; rien ne vérifie l'absence d'allocation dans le callback ; pas de test du service | Délai d'autosave allongé en test ; crochet `malloc` dans les tests Linux ; test émulateur du service |
