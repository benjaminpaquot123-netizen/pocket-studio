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

## Phase 2 (2026-10-19)
Relecteur : sous-agent neuf (74 fichiers). Bilan : 2 graves, 10 mineurs. Fil audio jugé sain (Sampler, NoteTrack, synthé, TinySoundFont avec voix préallouées : ni allocation ni verrou) ; files de retrait à un seul écrivain et un seul lecteur en pratique.

### Corrigé
| # | Gravité | Problème | Correction |
| --- | --- | --- | --- |
| 1 | Grave | Renvoyer les pads (annuler, retour dans l'appli, tempo) coupait net tous les sons en cours, boucles comprises | `Sampler::install` ignore un son identique (référence en trop rendue) ; test Linux |
| 2 | Grave | Copies calées au tempo perdues (mémoire native) si le tempo bougeait pendant le calcul | Calcul + enregistrement en `NonCancellable` ; la dernière copie reste jusqu'à la suivante |
| m1 | Mineur | Un pad calé rejouait l'original le temps du recalcul | La copie précédente continue de jouer |
| m2 | Mineur | Moteur hors-ligne du rééchantillonnage non libéré si annulé | Fermeture en `NonCancellable` sur le fil principal |
| m3 | Mineur | Calcul d'étirement lisant une table du fil principal | Source cherchée sur le fil principal et passée au calcul |
| m4 | Mineur | WAV 8, 24 ou 32 bits entiers lus comme du 16 bits (bruit) | Encodage lu dès le format d'entrée, 8/24/32 bits gérés |
| m5 | Mineur | Projet illisible non déplacé : sons du dossier effacés au nettoyage | Pas de nettoyage quand le chargement a échoué |
| m6 | Mineur | Note coincée si un note-off était perdu (file pleine) | Arrêt du transport = tout relâcher (synthé et instrument) |
| m8 | Mineur | Mémoire insuffisante au rendu = plantage | `OutOfMemoryError` attrapée, message clair |
| m9 | Mineur | Règle « un seul fil » de TinySoundFont implicite | Écrite dans `SoundFontPlayer.h` |
| m10 | Mineur | Rien ne prouvait l'absence d'allocation dans `process()` | Test Linux qui compte les allocations (0 sur 400 blocs, tout actif) ; vérifié qu'il échoue si on en ajoute une |

### Reste à faire (noté, non bloquant)
| # | Problème | Piste |
| --- | --- | --- |
| m7 | Rééchantillonnage « best » lent à l'import et refait à chaque lancement | Mettre en cache la version 48 kHz |
| — | Le test émulateur du rééchantillonnage vérifie le nom, pas le son | Vérifier un pic non nul en rejouant le pad |
| — | Limite de 60 s par son | Stockage 16 bits ou lecture en flux (phase 3, utile au remix) |

## Revue de fin de phase 3 (remix) — 2026-10-19

Relecture par un sous-agent neuf sur tout le diff de la phase (séparation, service, analyse, stems dans le moteur, banques, acide 128 pas). Callback audio : aucune allocation, aucun verrou (confirmé, et testé avec réinstallations pendant la mesure).

Corrigé :
- GRAVE : plantage au téléchargement du modèle sur Android 16 (rapport n° 8). `ServiceCompat` d'androidx efface le type « mediaProcessing » → appel direct à `startForeground`.
- GRAVE : toute erreur autre qu'E/S dans le service (codec, accès au fichier retiré) faisait planter l'appli → attrapée, message clair.
- GRAVE : le ménage du cache pouvait effacer les pistes utilisées par le projet → morceau du projet épinglé, date d'usage rafraîchie quand on réutilise un morceau déjà séparé, échec de renommage signalé.
- Changer de morceau pendant un chargement ne chargeait jamais le nouveau ; ancien morceau libéré tout de suite (pic mémoire) ; plus de fuite si l'écran se ferme pendant un chargement.
- Mémoire de la séparation : lecture du son par morceaux et copies libérées avant le modèle (2 copies du morceau de moins).
- `onTimeout` arrête le service tout de suite ; exceptions C++ bloquées aux frontières JNI ; permission de lecture du fichier rendue après usage.

Reste noté (mineur) :
- Les stems d'un long morceau restent en mémoire (0,77 Mo/s) ; lecture en flux si des téléphones manquent de mémoire.
- Pas d'étirement des stems si le tempo du projet change (avertissement affiché).

## Revue de fin de phase 4 (production) — 2026-10-20

Relecture par un sous-agent neuf sur tout le diff de la phase (effets, pompe, automation, scènes et morceau, effets live, correctifs Remix). Fil audio : aucune allocation, aucun verrou, indices bornés, protection des nombres dénormaux en place (confirmé ; test sans allocation avec tous les effets, l'automation et les effets live).

Corrigé :
- Arrêt juste après une bascule de scène : l'écran pouvait montrer la scène A pendant que le moteur gardait B → bascule rattrapée et projet renvoyé au moteur à l'arrêt.
- Mode « scène en attente » qui pouvait rester bloqué si la file de commandes débordait → le moteur le coupe à l'armement et à l'arrêt ; syncEngine le coupe aussi ; test Linux.
- Mode morceau : les modifications de la scène suivante (même scène répétée) étaient défaites à la bascule → la scène en attente est renvoyée après les modifications.
- Répéteur tenu plus de 3–4 s : le tampon écrasait la boucle → le tampon est figé pendant la répétition.
- Un NaN entrant dans les effets rendait la sortie muette pour toujours → entrées nettoyées avant les effets.
- Bascule armée en retard : attend la mesure suivante au lieu de couper une mesure.
- Test d'automation qui ne pouvait pas échouer, réparé ; tests ajoutés : acide et notes en attente, fin du mode attente à l'arrêt.
- Journal : rotation (on garde les lignes récentes) ; noms de morceaux et de fichiers masqués dans les rapports de plantage publics.

Reste noté (mineur) :
- Écriture du journal sur le fil principal (petites écritures, quelques par minute).

## Revue de fin de phase 6 (2026-10-20, sous-agent neuf, `c2fc7cd..5d2d1b0`)
Verdict : moteur et sauvegarde sains (rien d'interdit dans le fil audio, aucune perte de projet trouvée à l'ouverture, au renommage, à la suppression ; le nettoyage des sons couvre toutes les scènes et les pads live).

Corrigé :
- GRAVE : import d'un .zip avec un project.json énorme → plantage mémoire. project.json plafonné à 4 Mo, dossier nettoyé même sur OutOfMemoryError ; doublons refusés ; projet assaini avant la vérification des sons (tests ajoutés).
- Noms des pistes exportées coupés avec un nom de projet long (fichiers écrasés) ; drapeau d'annulation resté levé ; annulation M4A affichée comme un échec ; M4A incomplet annoncé comme réussi.
- « Mes projets » qui se rouvrait après l'ouverture d'un projet ; Annuler/Rétablir désactivés pendant un enregistrement ; latence de sortie compensée à l'enregistrement (Bluetooth).
- Remix qui suit le tempo : échec mémorisé (plus de nouvel essai à chaque réglage), écart de tempo limité de la moitié au double avec un message clair.
- Roulements transmis avec le coup (une seule commande au lieu de deux : la file du moteur ne déborde pas) ; test « stop pendant un roulement » ajouté.
- Réglage du tampon audio séparé du volume du métronome ; fichiers MIDI à longueur de piste incohérente refusés.

Restant (mineur) :
- Couloir de force du piano roll : remplace toute la boucle ; pendant un enregistrement, une note tout juste ajoutée peut être écrasée par un geste simultané. Le geste en cours de la grille redémarre quand une note arrive.
- Import d'un son de 2 minutes : pic de mémoire Java ~110 Mo (attrapé : message « pas assez de mémoire » au lieu d'un plantage).
- Export MIDI : deux notes de même hauteur qui se chevauchent, la première coupe la seconde.

## Revue de fin de phase 5 (2026-10-20, sous-agent neuf, relecture ciblée)
Verdict : fil audio propre (aucune allocation mesurée, files et atomiques respectés) ; trois défauts graves trouvés et corrigés.

Corrigé :
- GRAVE : pads live calés sur la mauvaise mesure quand la lecture démarre (horloge à vide décalée) → l'horloge des pads se cale sur le temps du séquenceur (cycle de 64 temps), les boucles repartent avec la mesure ; test `live_pads_bar`.
- GRAVE : Plaits, une note très courte (appui et relâche dans le même bloc de 12 échantillons) restait muette → le relâchement attend le bloc suivant ; test ajouté.
- GRAVE : Remix, changer de morceau pendant le chargement de l'écoute laissait les pistes en mémoire → chargement non annulable puis libéré s'il n'est plus voulu.
- Favoris : écritures l'une après l'autre (toujours la dernière liste), synchronisées sur le disque ; 49e favori refusé avec un message au lieu d'être perdu ; suppression avec « Annuler ».
- MIDI : état mis à jour sans course entre fils ; un appareil qui s'ouvre en retard pour une ancienne connexion est refermé ; notes tenues suivies sous le même verrou que leur envoi.
- Plaits : voix au-delà de la limite d'un nouveau modèle éteintes en quelques ms ; file d'événements portée à 256.
- Audio vers notes : une seule transcription à la fois ; copie du modèle par fichier temporaire puis renommage.

Restant (mineur) :
- Arrêt de l'écoute Remix : petit clic possible (les pistes sont retirées tout de suite après la coupure).
- Pads live : les sons remplacés partent dans une file de retour de 64 places ; si elle déborde (64 remplacements sans retour à l'écran), le son est gardé jusqu'à la fermeture (fuite bornée, pas de plantage).

## Revue de fin de phase 7 (2026-10-20, sous-agent neuf, `4137502..5280cba`)
Verdict : rien d'interdit dans le fil audio (0 allocation mesurée) ; le tampon d'enregistrement n'est jamais libéré sous le moteur ; numéros de pistes et de voies cohérents partout (moteur, JNI, Kotlin, modèle, anciens projets).

Corrigé :
- GRAVE : boucle de batterie forcée à 16 pas par le moteur (3/4 et 2/4 décalés) → 8 pas minimum, test d'une valse d'une mesure.
- GRAVE : projet fermé pendant une prise → micro resté ouvert et 34 Mo perdus ; prise non récupérée (flux arrêté) → tampon gardé puis libéré dès que le moteur l'a lâché.
- GRAVE : mémoire qui grossit à chaque prise ou retouche → les sons que le projet n'utilise plus sont libérés, Annuler les recharge ; copie de la prise hors du fil principal, mémoire insuffisante attrapée.
- GRAVE : micro qui réoriente le son (certains téléphones) → la prise rouvre son micro après la remise en route de la sortie, sans fausser le test de latence.
- Clips audio en mesures de la signature (départ, boucle, longueur) ; changement de mesure appliqué à toutes les scènes, cases au-delà de la boucle non éditables.
- Morceau en lecture : une resynchronisation (Annuler, réglage d'un clip) garde les pistes coupées de la partie.
- Changer de piste ne coupe plus les notes des autres pistes (chaque touche se relâche sur sa piste).
- Ouverture et fermeture du micro hors du fil principal (plus de risque de blocage).
- Banques General MIDI jamais fermées pendant un export ; MIDI : bonnes coupures de parties ; mixeur : seules les voies utilisées sont envoyées au moteur.
- Tests renforcés (fondu d'entrée vérifié, contrôle « OU » retiré, conversion de mesure).

Restant (mineur) :
- Pendant la lecture du morceau, modifier les notes d'une piste coupée dans la partie en cours la fait entendre jusqu'à la partie suivante.
- La file de commandes du moteur (4096) n'est pas surveillée : un très gros projet (8 pistes pleines de 512 notes) pourrait perdre des commandes à la resynchronisation.
- Aucun test pour la disposition des vumètres côté JNI ni pour le solo avec des voies inutilisées.

## Revue de fin de phase 8 (2026-10-20, sous-agent neuf, `8031713..1ed60da`)
Verdict : effets, anneaux d'analyse et arpège sans allocation ni verrou dans le fil audio (0 allocation mesurée) ; effets bornés (lecture du flanger, phaser près de Nyquist) ; anciens projets et numéros d'effets compatibles.

Corrigé :
- GRAVE : note bloquée quand l'arpège s'allume avec une touche tenue → la touche se relâche normalement ; test ajouté.
- GRAVE : automation « Morceau » décalée quand le morceau reboucle → le moteur compte les pas depuis le début du morceau (remis à zéro au retour de la première partie) ; lecture, curseur et enregistrement utilisent ce même compteur ; test ajouté.
- GRAVE : micro de l'accordeur resté ouvert si on quitte pendant l'ouverture → ouverture et fermeture sur un seul fil, dans l'ordre, jamais annulées à moitié.
- GRAVE : courbe « Morceau » sans morceau → le moteur reçoit les 8 mesures affichées ; longueur du morceau comptée en pas (scènes en 3/4).
- Horloge MIDI : plus de rafale de tops passés quand on l'active en cours de lecture (Continue) ; suivre et envoyer l'horloge s'excluent.
- Arpège : plus de double note au bord d'un bloc ; noise gate : temps d'attaque et de relâche conformes à l'affichage ; MIDI learn : cibles abîmées d'un fichier écartées ; dessin d'une courbe de morceau : seule la courbe touchée est renvoyée ; commandes du jeu en direct et courbe de morceau dans le test « aucune allocation ».

Restant (mineur) :
- Arrêter le transport oublie les touches tenues de l'arpège (il faut les reposer).
- Pas de pitch bend pour les instruments General MIDI.

## Phase 9 (génération), relecture de fin de phase

Corrigé :
- Une séparation demandée pendant le téléchargement du modèle de génération était ignorée sans message (et gardait l'accès au fichier) : message clair, accès rendu.
- Génération et séparation en même temps (risque de manquer de mémoire) : chacune refuse quand l'autre tourne.
- Délai Android dépassé pendant le téléchargement : l'erreur s'affiche sur le bon écran.
- Tokeniseur : une suite de caractères inconnus donne un seul `<unk>` comme la référence (vérifié sur emoji et japonais) ; `max` borné.
- Le son généré ne se joue plus tout seul si on a quitté la page ; écoute impossible signalée ; « Supprimer le modèle » grisé pendant une génération.
- « Piste audio » ne crée plus de piste vide quand une autre pose est en cours.
- Progression : un palier à 85 % après le chargement du décodeur.
- Script du modèle : empreintes de LICENSE et NOTICE vérifiées.

Restant (mineur) :
- Les tests du vrai modèle (`generate_test`, comparaison du tokeniseur) ne tournent qu'en local (`STABLE_AUDIO_DIR`, `T5_TOKENIZER`), pas en CI (858 Mo).
- « Annuler » de la notification arrête le travail en cours du service, quel qu'il soit.
- Écoute du son généré sans focus audio ; « Piste audio » qui ajoute une piste fait deux pas d'annulation.
- Pas de test unitaire de la machine d'états de GenerateViewModel.

## Phase 10 (débutants et terrain de jeu), relecture de fin de phase

Corrigé :
- GRAVE : le filtre de la macro Couleur (Svf de DaisySP avec sa saturation interne) pouvait diverger près de sa coupure haute sur un son fort ; le NaN gagnait la réverbe, l'écho et la sortie, silence jusqu'au redémarrage. Saturation interne coupée ; et toute voie dont les effets produisent un NaN est remise à zéro (un bloc muet, effets réinitialisés sans allocation) au lieu de contaminer les bus. Test : bruit ±3 sur cinq réglages de couleur, toujours fini.
- GRAVE : CI rouge (fonction de test dans un bloc exclu sous AddressSanitizer) ; et le moteur était devenu trop lourd pour la pile des tests sous ASan : inserts et macros du mixeur sur le tas, alloués une fois à la création.
- Macros sans clic : filtre en fondu de 10 ms, saturation qui continue jusqu'à ce que sa compensation de volume revienne à 1, remise à zéro à l'activation.
- Pads d'accords et MIDI verrouillé : une note partagée par deux pads ou deux touches tient tant que l'une est appuyée.
- Mixeur : résumé des effets calculé une fois par changement (plus 21 comparaisons par image).
- Assistant : ses choix restent jusqu'à l'écriture du projet ; rendu hors ligne libéré même si la préparation échoue.
- Tests : fichiers de l'ancienne version (sans pan, hauteur, macros, verrou) relus ; sons stéréo gardés en stéréo.

Restant (mineur) :
- Un coup qui relance une voix synthétisée encore en train de sonner change son pan d'un coup (petit clic possible avec des pans alternés).
- Le filtre d'insert existant (SweepFilter, résonance jusqu'à 0,95) garde la saturation interne de DaisySP ; la remise à zéro de la voie le rattrape s'il diverge.
- La file de commandes (4096) n'est pas surveillée : un très gros projet resynchronisé pourrait perdre des détails de coups (joués au centre).
- Écoute des sons (AudioTrack statique) : une écoute très longue (> 1 min) peut être refusée ; deux appuis très rapides peuvent rejouer l'ancien son.
- Hauteur par coup réglable de −12 à +12 à l'écran (le format accepte ±24).

## Phase 11 (bibliothèques de sons, FLAC, volume perçu), relecture de fin de phase

Vérifié par le relecteur : FLAC sans perte sur 16 fichiers (1 à 4097 images, mono, stéréo, 16 et 24 bits, flottant, 5 minutes), CRC validés par ffmpeg ; volume perçu à 0,05 LU de ffmpeg ebur128 sur 6 signaux ; chaîne des packs épinglée de bout en bout par empreinte. Rien de grave.

Corrigé :
- Le test FLAC décode vraiment le fichier (décodeur de test : CONSTANT, FIXED, Rice partitionné, stéréo, CRC-8 et CRC-16) et compare chaque échantillon, en 16, 24, 32 bits, flottant, mono, longueurs 1 à 13 522.
- FLAC plus léger : codage de Rice partitionné (ordre 0 à 8) et stéréo gauche/côté, droite/côté ou milieu/côté au choix par bloc ; en-têtes dans le sous-ensemble « streamable » (fréquence et taille dans chaque bloc) ; tampons réutilisés ; progression en pourcentage pendant la conversion. Texte : « bien plus léger (souvent deux fois) ».
- Annuler pendant la mesure du volume d'un export WAV arrête bien l'export ; une conversion en cours s'arrête si l'écran est détruit, sans fichier oublié.
- Plus de vieux « Volume perçu » sous un export MIDI ; partage d'un FLAC en audio/flac.
- « Poser » quand un son est encore en cours : la bibliothèque reste ouverte avec un message en haut (les messages de la bibliothèque sont maintenant en haut).
- Tests du volume perçu : un 30 Hz (filtre passe-haut) et une moitié 30 dB plus douce (porte relative), valeurs de ffmpeg.
- Test qui lie l'index des packs épinglé à la copie lue par les tests du modèle.
- Packs : un dossier à moitié installé (appli tuée) est nettoyé à l'ouverture de la bibliothèque ; l'ancien pack n'est effacé qu'une fois le nouveau en place.

Restant (mineur) :
- Le FLAC n'utilise que les prédicteurs fixes (pas de LPC) : un peu moins compact que l'encodeur officiel.

## Phase 12 (son échantillonné, aides, pads live), relecture de fin de phase

Vérifié par le relecteur : fil audio sans allocation ni verrou (test sans allocation avec une piste échantillonnée qui glisse), durée de vie des sons (installation, file de retrait, destructeurs, commandes perdues), sauvegarde et conservation des fichiers, détection de la note d'origine. Rien de grave.

Corrigé :
- Notes répétées de même hauteur coupées par la fin de la précédente (arrondi du séquenceur, swing) : la note qui tient encore finit là où la suivante commence. Visible sur les 808 au tempo trap (1 note sur 7 à 140 BPM), aussi pour le synthé. Test : 15 notes enchaînées à 140 BPM, avec et sans glisse (2 coupées avant la correction, 0 après).
- Glisse activée ou coupée pendant que des touches sont tenues : les notes sont relâchées (plus de note bloquée) ; un relâchement libère aussi toute autre voix de la même touche. Test.
- Son de la bibliothèque mis sur une piste supprimée entre-temps : abandonné avec un message, au lieu d'arriver sur la première piste.
- Écoute d'un son de pack dans l'assistant : rééchantillonnage hors du fil principal, son natif toujours libéré.
- File de commandes pleine : attente jusqu'à 100 ms (rappels Bluetooth espacés de 40 ms).
- Bibliothèque refermée pendant le chargement d'un son : le son n'arrive plus nulle part (ni ne ferme la bibliothèque rouverte).
- Workflow des packs : un pack réutilisé mal téléchargé est régénéré au lieu de manquer.
- Hauteur des voix recalculée seulement quand elle change.
- Plantage natif au changement de sortie audio (rapport #22, casque Bluetooth) : le flux que le système ferme est mis de côté avant la fermeture, plus aucun appel dedans.

Restant (mineur) :
- Après Annuler, une piste échantillonnée joue le synthé le temps que son fichier soit relu.

## Phase 13 (machines, sons actuels, unisson), relecture de fin de phase

Vérifié par le relecteur : fil audio de l'unisson sans allocation, 26 réglages envoyés partout (chargement, export, assistant, MIDI), anciens projets inchangés, index des packs et les 51 archives (empreintes, fichiers, familles). Rien de grave.

Corrigé :
- Unisson décentré (plus bas) avec 2, 4 ou 6 voix : la pile est recentrée sur la note. Test : centre à 440,0 Hz pour un La.
- Une note rejouée pendant qu'elle sonne garde ses phases (plus de petit clic) ; test.
- Nombre de voix changé pendant une note : volume en fondu, les copies suivent toujours la note.
- Écart minimal de ±3 cents : à « Écart » 0 le volume ne varie plus d'une note à l'autre.
- Assistant : les nappes des packs se relâchent en 0,4 s ; l'indication des packs aussi pour les nappes.
- Bibliothèque : « Packs de sons » triés par nom, texte d'aide général quand une famille est vide.
- Décision : « identique à l'échantillon près » corrigé en « le même aux arrondis près ».

## Phase 14 (nouveau look, blocs repliables, écoute dans l'assistant), relecture de fin de phase

Vérifié par le relecteur : sons natifs de l'écoute libérés sur tous les chemins (annulation comprise), commandes moteur sur le fil principal, rien dans le fil audio, pas de note bloquée quand le clavier qui suit disparaît, pas de boucle de recomposition, aucun tag utilisé par les tests supprimé.

Corrigé :
- Grave : à l'écoute dans l'assistant, la nappe et le lead étaient muets (pistes 1 et 2 éteintes dans le moteur d'export). Allumées.
- La fin silencieuse n'est plus coupée que pour les sons de la bibliothèque et le micro : une boucle importée ou générée garde sa longueur (sinon elle rebouclait trop tôt). Test de la coupe (silence, une image, stéréo, sons longs).
- Bloc replié : plus d'ouverture puis fermeture animée à chaque affichage (animation seulement après un toucher, hauteur du titre mémorisée et mesurée par la carte ou le titre, le dernier placé). Test : toucher le titre replie la carte, la retoucher la rouvre.
- Assistant : toucher une deuxième fois un son en préparation n'annule plus l'écoute ; le retoucher pendant qu'il joue l'arrête. Une écoute abandonnée arrête son rendu ; une erreur d'écoute n'arrête plus l'appli.
- Clavier qui suit : glisser sur une touche ne le range plus (seulement l'en-tête).
- « Son » de l'acide et « Son » des réglages ne se replient plus ensemble. Roue de préparation nommée pour TalkBack.
- CI : le test sur émulateur retélécharge l'émulateur et son image tant qu'elles sont incomplètes (serveur de Google qui envoie des archives cassées), et les garde en cache.

Restant (mineur) :
- Le clavier qui suit se base sur toute la carte Clavier (avec l'arpège ouvert, les touches peuvent être hors écran sans qu'il apparaisse).
- Clavier qui suit en mode Pads : tags de test en double avec les pads de la carte (aucun test ne s'en sert).
- Cibles tactiles sous 48 dp : familles de sons (36), octave −/+ (40), lecture du haut (42), pastille « Clavier ».
- Pas de « Tout déplier » dans les réglages.
- Performances : halo des pads recomposé à chaque image pendant un coup ; grilles de vignettes non paresseuses (gros packs) ; préférences de pli lues au démarrage sur le fil principal ; sons de batterie des packs redécodés à chaque écoute.

## Phase 15 (recettes de style, sons classés, claviers, sample au clavier, effets par pad), relecture de fin de phase

Vérifié par le relecteur :
- Fil audio : rien n'est alloué ni verrouillé dans le rendu, les 32 tranches sont tenues partout (vumètres, commandes, solo et muet), et une note qui produit un NaN est coupée avant d'atteindre la batterie.
- Batterie : son volume, ses effets, son muet et son solo s'appliquent une seule fois aux pads.
- Anciens projets : ils sont complétés à 32 tranches. L'export, le rééchantillonnage et l'annulation suivent les effets des pads.
- Recettes : `StyleRecipes.kt` est identique à ce qu'on régénère depuis les recettes, l'index `sons-packs-4` est conforme et les notes restent dans la gamme pour les 35 styles et les 4 ambiances.

Corrigé :
- Grave : enregistrer l'automation d'un effet ou d'un envoi d'un pad déplaçait un autre réglage (la voie 16 à 31 débordait sur le type de cible), par exemple l'effet du master. L'automation des voies de pad est refusée à l'enregistrement, et les pistes d'automation fautives sont écartées au chargement. Test.
- La réverbe et l'écho d'un pad suivent maintenant le fader de la batterie (avant, baisser la batterie laissait la réverbe d'un pad à plein). Test qui échoue sans la correction.
- Tests du moteur plus exigeants :
  - batterie à mi-volume avec saturation, identique que le pad passe par sa voie ou non ;
  - un effet sur la voie d'un pad ne touche pas les autres pads ;
  - le solo d'une autre voie coupe les pads ;
  - aucune allocation avec des pads routés, des effets et des envois, ni quand on bascule leur voie pendant la lecture.
- « Jouer ce sample au clavier » attend qu'un son en préparation soit prêt (plus de double traitement, plus de message qui efface l'état « occupé »).
- Le convertisseur de recettes refuse une source inconnue, une ligne acide de plus d'une mesure, des accents ou glissés mal formés.
- Texte de l'ambiance Joyeux : « des accords lumineux » (plusieurs styles la jouent en dorien, avec un premier accord mineur).
- Garde à la compilation : autant de voies de pad que de pads, de voix de kit et de samples.
- Tests du modèle : ligne 303 de l'assistant (notes, accents, rien sans ambiance) ; voie propre d'un pad seulement s'il a un réglage ; anciens projets complétés à 32 voies.

Restant (mineur) :
- Les effets insérés sur la batterie ne traitent pas la réverbe et l'écho envoyés par un pad (comme un envoi pré-groupe sur une console).
- Une note tenue d'une mesure à l'autre dans une ligne de deux mesures est coupée à la barre (aucune recette n'en a aujourd'hui).
- Pas encore de test Kotlin du découpage et de l'inversion de « Jouer ce sample au clavier ».

## Phase 16 : audit « en production » (moteur audio et usage réel)

Deux auditeurs neufs. Le premier a relu le moteur et lancé des essais compilés contre le cœur ; le second a suivi une vraie séance de production dans l'appli.

Vérifié sain :
- Fil audio :
  - aucune allocation, aucun verrou, aucun appel Java ;
  - les sons, les stems et les instruments sont rendus au fil principal par des files.
- Sauvegarde : écriture atomique avec copie de secours, sauvegarde forcée en arrière-plan.
- Séquenceur : à l'échantillon près quelle que soit la taille de bloc.
- Redémarrage du flux : il ne perd rien.
- Export : il suit le même chemin de commandes que le direct.

Corrigé (graves) :
- Solo fantôme : une piste mise en solo puis supprimée (ou un autre projet ouvert) rendait tout muet, sans moyen d'enlever le solo. La voie est remise à neutre, et le moteur reçoit « pas de solo » pour toute voie inutilisée. Test.
- Fuites de mémoire native à l'annulation. Elles touchaient :
  - l'écoute d'un morceau du Remix ;
  - l'étirement au tempo ;
  - le chargement des stems ;
  - les pads calés ;
  - les sons du projet.

  Une aide « finir ou libérer » les rend toujours. Test.
- Historique d'annulation : chaque étape gardait une copie complète du projet, jusqu'à 1,6 Mo par geste. Elle ne garde plus que ce qui change. Test.
- Export « Piste par piste » : les stems ne gardent plus les solos et les muets du mixage.
- Prise micro, qui pouvait être perdue :
  - par Annuler (désormais désactivé pendant la prise) ;
  - par l'ouverture d'un autre projet (la prise est maintenant arrêtée et gardée avant) ;
  - par un arrêt pendant qu'un son se posait (l'arrêt n'est plus refusé, et le micro est bien libéré).
- Prise micro avec un casque Bluetooth : la voix arrivait 150 à 200 ms en retard. Elle est maintenant compensée selon la sortie utilisée. Test.

Corrigé (moyens et mineurs) :
- Casque débranché ou appel : la lecture s'arrête (focus audio, « casque débranché »).
- Annuler, Rétablir ou un retour dans l'appli pendant la lecture ne coupent plus les notes tenues : seuls les instruments qui changent sont renvoyés.
- La pédale, la molette et l'arpège d'un projet ne restent plus actifs dans le suivant.
- Enregistrement en direct : une note jouée juste avant le « 1 », ou tirée avant lui par la latence, tombe sur le « 1 » au lieu d'être perdue ou de partir en fin de boucle. Test du moteur (position pendant le décompte).
- « Filtre » du son général et filtre DJ : sur un mix très fort, ils divergeaient en NaN et le son restait muet pour de bon. Les filtres n'ont plus de saturation interne, et un garde-fou NaN est ajouté sur le son général (signalé aux rapports). Test qui échoue sans la correction.
- Coups de batterie perdus avec les gros blocs du Bluetooth (rouleaux, swing fort) : le moteur rend par tranches de 256 images. Test.
- Pédale relâchée sur plus de 64 notes : des notes restaient bloquées. Les files ont maintenant 256 places. Test.
- Clavier MIDI : une note est relâchée sur la piste où elle a commencé, même si on change de piste en la tenant.
- Générateur aléatoire sans mémoire par fil : plus de réservation de mémoire ni de verrou au premier son d'un nouveau flux.
- Exports :
  - longueur juste en 3/4 et dans les autres mesures ;
  - sans les coupures de la partie du morceau en cours de lecture ;
  - lancés seulement une fois les sons, banques et stems du projet chargés ;
  - fin de la queue en fondu (test).
- « Pas d'instrument » sur une piste qui n'en a pas ne coupe plus ses notes.
- Fichier audio de plus de 2 minutes, ou plus long que 64 mesures au tempo : un message le dit. Une prise s'arrête à 64 mesures.
- Effets live (pad XY, Répéter, Hacher, Stop bande) toujours relâchés, même si l'écran change pendant l'appui.

Restant (à reprendre) :
- Niveau général : le limiteur travaille environ 8 dB sur le kit et l'acide d'origine, avec un léger pompage. Le vumètre est placé après le limiteur. Voir DECISIONS.
- Changer le tempo pendant la lecture :
  - un pad calé au tempo fait un clic et sa boucle se tait jusqu'au coup suivant ;
  - le Remix qui suit le tempo saute de position.
- Mode morceau :
  - un coup joué au changement de partie peut aller dans l'ancienne scène ;
  - modifier une piste coupée dans la partie en cours la rend audible jusqu'à la partie suivante.
- Mesures autres que 4/4 : le piano roll et la ligne acide ont des longueurs en multiples de 16 pas.
- Arrière-plan :
  - une prise micro avec l'écran éteint enregistrait du silence : réglé en phase 17 (service « micro » pendant une prise) ;
  - un long export en arrière-plan n'a pas de service de premier plan.
- Remix : un seul morceau est épinglé pour toute l'appli, et l'étirement peut figer l'interface quand on le retire.
- Mineurs :
  - un clic à l'ajout ou au retrait d'un effet pendant la lecture ;
  - un son remplacé pendant qu'il joue est coupé net ;
  - les stems démarrent sans fondu ;
  - le métronome passe avant les effets du son général ;
  - TinySoundFont est limité à 48 voix ;
  - la file de commandes refuse tout après 100 ms sans rappel audio ;
  - un resynchronisation complète arrive dans un seul rappel (à mesurer sur le téléphone) ;
  - l'export MIDI ne contient ni arpèges ni roulements ;
  - la rotation arrête l'enregistrement micro d'un pad ;
  - « Partager » échoue pour un export de plus d'une heure ;
  - chaque mouvement de fader renvoie tout le mixeur.

### Relecture de fin de phase 16 (sous-agent neuf)

Vérifié par le relecteur : rien de neuf dans le fil audio n'alloue ni ne verrouille ; tranches de 256 images cohérentes partout ; position du décompte juste ; instruments envoyés seulement s'ils changent sans cas faux ; partage de l'historique sans effet sur la sauvegarde ; les 5 nouveaux tests du moteur échouent bien sans leur correction.

Corrigé :
- Filtre DJ remis à zéro (garde-fou NaN, réouverture du flux) : ses réglages sont réappliqués aussitôt (il pouvait devenir un résonateur qui siffle tant que le doigt ne bougeait pas).
- Filtres sans saturation interne : plus forts à haute résonance (jusqu'à +10 dB) ; le niveau est compensé selon la résonance (10 dB à fond, 3,6 dB à 0,6). À écouter sur le téléphone.
- Annuler reste grisé tant qu'une prise arrêtée n'est pas écrite sur sa piste.
- L'export attend aussi le calage au tempo (Remix et pads calés).
- Instruments : retenus comme envoyés seulement si le moteur les a pris, et tout est renvoyé quand le son redémarre.
- Micro ouvert pendant un changement de projet : refermé. Latence de la prise relevée à son début (un casque débranché pendant la prise ne la fausse plus).
- Plafond « 64 mesures » d'une piste audio juste dans toutes les mesures (3/4…).
- Une note tenue pendant un changement de piste s'enregistre sur la piste où elle a commencé.

Restant (mineur) :
- Swing fort avec un roulement sur un contretemps : deux coups d'un même pad peuvent tomber dans la même tranche de 256 images (un flam peu audible) ; frappes MIDI en direct groupées dans un même rappel Bluetooth.
- Garde-fou NaN : avec une source de NaN permanente, il remet tout à zéro à chaque tranche (sans danger depuis la correction des filtres) ; pas encore de test qui injecte un NaN.
- Le focus audio met en pause les autres applis de musique au lancement de la lecture (voir DECISIONS) ; les pads live joués sans transport ne sont pas couverts.

## Phase 17 (lecteur multimédia, activité en direct « REC », prise micro écran éteint), relecture de fin de phase

Relecture neuve : rien de grave (aucun chemin de plantage trouvé, fil audio non touché).

Corrigé :
- Notification de lecteur orpheline : une mise à jour postée juste avant Arrêter pouvait reposer un lecteur que plus aucun service ne tenait ; le service l'efface en s'arrêtant (test qui rejoue la course).
- Pastille « REC » invisible sans l'autorisation des notifications (Android 13+) : demandée au premier enregistrement.
- Centre de contrôle : vrai bouton « Arrêter » (action de la session) en plus de pause ; Arrêter, pause et le bouton du casque arrêtent aussi le service.
- Service : repli sans plantage si Android refuse le premier plan ; premier état jamais perdu ; au plus 4 mises à jour par seconde, canal créé une fois.
- Chrono de la prise : démarre avec l'enregistrement, pas pendant le décompte.
- Tests : Arrêter (notification, lecteur), type « micro » pendant une prise, chrono pendant le décompte, notification effacée à l'arrêt.

Restant (mineur) :
- Enregistrement en direct lancé à l'arrêt : le chrono compte aussi la mesure de décompte.
- Ce que MagicOS fait de la notification promue (pastille, Capsule magique ou rien) : à voir sur le Magic 8 Pro.

## Phase 18 (assistant en deux scènes, styles connus d'abord, repli partout), relecture de fin de phase

Relecture neuve : les données sont saines (14 620 constructions essayées : chaque ligne et partie dans une seule scène, aucune scène vide, aller-retour JSON et changement de scène identiques).

Corrigé :
- Grave : le guide de fin ne s'affichait jamais (ouvrir le projet recrée l'activité et effaçait l'état) ; il est gardé par `rememberSaveable` (test qui échoue avec un simple `remember`).
- Le guide décrit le morceau tel qu'il a été construit (plan pris au même instant que les choix) ; « Créer » reste bloqué jusqu'à la fermeture de l'assistant (plus de double projet), et un échec d'écriture rend la main avec un message.
- La 808 reste avec le kick dans la scène A quand il n'y a pas de mélodie.
- « Choisis le son » se replie ligne par ligne (avant : replié une fois, replié partout) ; le titre d'un groupe repliable se touche sur toute la largeur.
- Test invariant : chaque choix joue dans la scène prévue, aller-retour JSON et changement de scène, pour tous les styles.

Restant (mineur) :
- Replier la carte d'une ligne cache aussi « Utiliser cette ligne » et le repère de scène (le titre reste visible pour rouvrir).
- Un groupe « Les plus connus » replié le reste la fois suivante (mémorisé, comme les cartes).
- Après la relecture, à la demande de Benjamin : trois scènes au lieu de deux (tests réécrits : partage par style, copies des scènes vides, aller-retour JSON et changement de scène) ; pastilles qui passent à la ligne ou s'estompent au bord.
- Bug audio de la vidéo de Benjamin (22 h 54) : 808 posée sur des accords lue 16 fois trop vite (note d'origine La 0 sous des notes autour du Do 4) → « tic » d'un dixième de seconde par mesure puis silence. Corrigé partout où un son devient un instrument (bibliothèque, fichier, pad au clavier, assistant et ses écoutes), avec un test du cas exact, et calage automatique des projets existants à l'ouverture (testé sur le projet « Drum & bass » de Benjamin) ; écoute qui saturait par-dessus la musique : −6 dB.

## Phase 19 (effets fous, bouton Effets dans le mixeur, écoutes de l'assistant), relecture de fin de phase

Relecture neuve : rien de grave (aucune allocation, verrou ni NaN dans les nouveaux effets ; ordre des types identique au moteur ; 1,5 Mo de mémoire en plus pour les 128 emplacements).

Corrigé :
- Hachoir : la longueur du temps vient du séquenceur à chaque bloc (avant : mesurée entre deux temps, fausse juste après l'avoir choisi ou après un arrêt) ; un temps ne fait que relancer le motif.
- Vieux vinyle : pas de retard quand le pleurage est à zéro (un retard fixe faisait un effet de peigne et décalait la piste) ; craquements indépendants de la fréquence d'échantillonnage.
- Talkie-walkie : le niveau ne monte plus avec le grain. Wah-wah : coefficients calculés tous les 8 échantillons comme prévu.
- Bouton Effets : allumé seulement quand les effets sont montrés.
- Tests : craquements sur du silence (et silence sans eux), vinyle sans retard, Hachoir calé par le tempo avec un temps au milieu d'un bloc.

Restant (mineur) :
- Hachoir sur une grille droite : avec du swing, les doubles-croches paires ne suivent pas le swing.
- Un événement de temps peut rester en attente si la voie ne joue pas dans ce bloc (sans effet audible : le suivant le remplace).

## Phase 20 : audit d'utilisation complet (quatre parcours)

Quatre auditeurs neufs, chacun sur un parcours réel (créer/éditer ; jouer/enregistrer/son ; mixer/exporter/projets ; interface/robustesse), avec des essais pour prouver les bugs. Trois correcteurs en parallèle (branches séparées), puis une relecture neuve des corrections.

Graves corrigés :
- Assistant : plantage quand toutes les lignes sont désactivées.
- Barre du haut : le bouton « Mes projets » disparaissait sur petit écran ou texte agrandi ; onglets inaccessibles en paysage.
- Mise à jour : téléchargement bloqué pour toujours sans réseau, sans moyen d'annuler.
- Remix : séparer d'autres chansons effaçait les pistes d'un autre projet ; un appui supprimait des pistes encore utilisées.

Moyens corrigés (avec tests) : Annuler pendant la lecture du morceau, partie supprimée en cours de lecture, 3/4 puis 4/4 qui réécrivait la batterie, prise en direct coupée en deux par une modification, guide de fin (scène D), duplication en deux étapes, scènes vides en trop, étapes de mélodie sans mélodie ; autorisations refusées silencieuses, rotation qui coupait la prise, projet ouvert sans sauvegarde, guides sans défilement, mise à jour qui revenait ; corbeille, copie ratée, automation sur un effet remplacé, export de scène à 4 mesures ; filtre XY qui claque, pleurage du vinyle qui grésille, accords qui se coupent, vol de voix, prise décalée après un changement de sortie.

Restant (mineur) :
- Pads live joués sans lecture : casque débranché, ils continuent (le service de lecture ne tourne pas).
- Scènes vides entre A et une scène lointaine remplie : elles restent (pas encore de « Supprimer la scène »).
- Pas de menu « Supprimer la scène » ; pas de confirmation sur la suppression d'un pack testée par un test.

Relecture neuve des corrections : aucun grave. Corrigés : micro resté ouvert si une prise démarre pendant l'accordeur (compte des utilisateurs), temps muets en passant à 5/4 ou 7/4 (la boucle se répète), coups cachés comptés comme contenu (export, « Vider »), duplication annulée en pleine lecture qui basculait sur une scène vide, mise à jour « oubliée » par un simple retour.
Restant (mineur, noté) :
- Scène vide en fin de liste supprimée même si sa longueur a été changée (la longueur revient par défaut).
- StemStore relit chaque projet en entier pour savoir qui utilise une séparation (lent avec beaucoup de projets) ; une épingle périmée bloque la suppression en silence.
- Import de pack zip : pas de plafond sur le nombre de fichiers parcourus.
- Rotation pendant une prise : les instruments GM sont reconstruits (notes tenues coupées sur ces pistes).
- Filtre XY passe-bas → passe-haut : ~10 ms de fondu avec la mauvaise coupure.
- `HeldKeysTest` teste une copie de la logique plutôt que le vrai ViewModel.

## Phase 21 : du brouillon au morceau

Trois audits neufs (débutant, moyen, confirmé), puis trois correcteurs en parallèle (assistant et accueil ; morceau ; barre du haut et partage). Relecture neuve : aucun grave.

Moyens corrigés :
- Une modification du morceau juste après un changement de partie (ou la fin) partait de l'ancienne partie : l'app rattrape d'abord le moteur.
- « Rester sur ma scène » : une modification faite juste avant le passage de sa scène pouvait ne pas s'entendre ; elle est renvoyée à la prise de relais.
- Lancer le morceau laissait une étape d'annulation ; le guide disait encore que ▶ joue la section affichée.
- Insérer une partie refusée décalait la lecture ; copier sur une scène nommée effaçait son nom.

Restant (mineur, noté) :
- La logique de lecture du morceau dans le ViewModel (garde des modifications, remappage des parties pendant la lecture) n'est testée qu'à l'arrêt : sortir ces calculs en fonctions pures.
- `songIndexAfterInsert` est testé mais le ViewModel a ses propres lambdas.
- Annuler pendant la lecture du morceau oublie la partie choisie « ensuite ».
- Enregistrer pendant « rester sur ma scène » : les notes vont dans la scène affichée, calées sur celle qui joue.
- Partage terminé en arrière-plan : la feuille de partage peut s'ouvrir plus tard ; le fichier partagé apparaît aussi dans la liste des exports.
- La durée annoncée par l'assistant (« environ 2 min 40 ») peut différer un peu de la durée réelle.
- Le menu ⋮ d'une partie reste à l'ancienne place après un déplacement.
- La fenêtre « Nommer la scène » n'a pas de test d'écran (Robolectric ne se stabilise pas avec un champ de texte dans une fenêtre).

## Phase 22 : langues (FR, EN, NL) et tablette

Quatre traducteurs en parallèle (données du modèle ; accueil et outils ; rythme et synthé ; mixeur et audio), un dernier pour le Morceau et le ratissage, un correcteur tablette. Relecture neuve : aucun grave (placeholders, `%` littéraux, templates vérifiés par script ; rien de figé à l'initialisation).

Moyens corrigés :
- Changer de langue renvoyait à l'onglet Rythme (état perdu) : l'activité est recréée, l'état d'écran reste.
- L'assistant enregistrait les noms de sections traduits : ils restent en français, traduits à l'affichage (test).
- Même mot français, deux sens selon la zone (Accord, Voix, Cloche…) : « Accordage » pour la 303, « Cowbell » pour le pad, « Hauteur de voix » pour l'effet robot ; un test refuse désormais deux traductions pour une même clé.
- Les cartes repliées gardent leur état quand la langue change (clé = texte français).

Restant (mineur, noté) :
- La fenêtre « Nommer la scène » préremplit le nom français d'une section standard.
- Générer et Remix : sur téléphone, l'écran remonte en haut quand le premier résultat arrive.
- Téléphone couché (≥ 760 dp) : deux colonnes aussi ; pas encore de capture dédiée.
- Quelques textes traduits deux fois (sans effet aujourd'hui) ; dates en jj/MM dans toutes les langues.
- Anglais : « track » désigne parfois le morceau dans des descriptions de styles ; Colour/Color mélangés.

## Phase 23 : audit à cinq (après les langues)

Cinq auditeurs neufs, sans se voir : Léa (16 ans, premier beat sur téléphone), Karim (22 ans, rap avec voix), Marc (45 ans, guitariste, tablette en anglais), Sofia (productrice pro) et un contrôleur des langues. Rapports complets lus, synthèse, puis quatre correcteurs en parallèle (prises audio et position dans le morceau ; export pro et édition ; écrans débutants et tablette ; langues et noms de notes), réunis par l'intégrateur.

Graves corrigés :
- Prise micro perdue si l'appli était fermée pendant la prise : écriture hors de l'écran (`TakeVault`, fichier d'attente), reprise au lancement suivant ; le moteur rend la prise même flux fermé.
- Morceau lancé depuis une partie N : automation lue et enregistrée au mauvais endroit (écrasait l'intro). Le moteur reçoit la position de la partie de départ ; l'état d'automation des parties précédentes est appliqué.
- Pistes séparées passées par les effets et le limiteur du master : elles redonnent maintenant le mix (test moteur). Au passage : une voie coupée fuyait 100 ms au début des rendus hors ligne.
- Noms de notes « C D E » ignorés dans le piano roll, les pads d'accords, la gamme et la note d'origine : une seule fonction `NoteNames`.

Moyens corrigés : voix « du morceau » (une fois, d'un bout à l'autre), enregistrer sur tout le morceau, nouvelle prise propre (ancienne prise muette, réglages gardés), « une fois par partie », conseils écouteurs et Bluetooth, « Décaler la prise » ±200 ms, ● du haut = micro sur l'onglet Audio, partage sans morceau qui couvre la voix ; volume général, export sans limiteur (WAV 32 bits flottant), fin d'export automatique, quantifier près de la fin, coller sans perte silencieuse, notes gardées quand on raccourcit la boucle, outil Force, MIDI fidèle (boucles acides, swing) ; grille visible sans défiler, « Écouter : cette section / tout le morceau » à côté de Jouer, « Son du pad » avec « Changer le son », assistant compact avec ▶ par style et « Créer tout de suite », « ✓ Enregistré », onglet Aide, mémoire vérifiée avant le Remix, synthé complet sur tablette couchée, accordeur guitare ; noms par défaut gardés en français et traduits à l'affichage, 48 traductions revues, pluriels, dates, anglais britannique, test qui refuse un `tr(variable)` non vérifié.

Gardé pour plus tard (vraies nouvelles fonctions) : instruments en stéréo ; automation des réglages d'instruments (303, synthé) et points libres sur le morceau ; vraie ligne de temps avec clips libres ; un seul mot pour section/scène/partie ; un stem par pad ; garder le temps avant le « 1 » d'une prise ; noms de prises « Prise HH:mm » encore enregistrés traduits ; messages ponctuels retraduits après un changement de langue.

Relecture neuve (sous-agent qui n'a pas écrit le code) : fil audio sain (0 allocation), numéros de commandes cohérents, calculs de position dans le morceau justes, export identique au direct, stems qui redonnent le mix, fin d'export bornée, anciens projets lisibles.
- Grave corrigé : une prise qui n'avait pas pu être posée revenait au lancement suivant et remplaçait une prise plus récente, effacée ensuite. Désormais la prise est posée et enregistrée d'abord, la note d'attente n'est retirée qu'après un enregistrement réussi, et une prise en attente ne remplace que le clip sur lequel elle a été enregistrée (test).
- Moyens corrigés : note d'attente gardée si l'enregistrement échoue ; accords, ligne de basse, audio vers notes et import MIDI n'ajoutent plus les notes cachées ; boucles de clips en attente remises à zéro à l'arrêt ; un nom avec un nombre énorme ne fait plus planter.
- Mineurs corrigés : nettoyage des sons qui épargne les prises en attente ; M4A et FLAC gardent toujours le limiteur (texte qui l'explique) ; noms « Prise HH:mm » gardés en français ; voix « du morceau » muette quand une scène boucle seule ; « ✓ Enregistré » seulement quand tout est écrit ; un test qui ne vérifiait rien.
- Restant : attente de 3 s au plus au chargement si une prise s'écrit encore ; une voix « du morceau » reste à sa mesure si on insère une partie avant elle.

## Phase 24 : nouvel audit général et audit des 35 styles

Demande de Benjamin : refaire un audit après la phase 23, regarder sa capture de l'étape « style », puis auditer chaque style par un pro et par un débutant. Quinze auditeurs neufs : cinq généraux (débutante téléphone, rappeur, pianiste sur tablette, producteur pro, langues) et dix sur les styles (un pro et un débutant par famille : hip-hop, house, garage/bass, techno, divers). Notes de départ des styles : 4 à 8/10.

Graves corrigés : prise de scène qui jouait dans tout le morceau ; écouter une scène seule jouait et écrasait l'automation de l'intro ; automation et voix qui ne suivaient pas les changements d'arrangement ; riddim (et d'autres) avec nappe et lead imposés ; son signature absent des couplets.

Corrigé aussi : l'assistant suit la recette (pistes, ambiance, signature, forme avec Break), morceaux de 2 min 20 à 3 min 30, roulements, son de kit par ligne (prioritaire sur les packs), glissé de 808 et portamento, balayage de filtre calé au tempo (Basse wobble), Basse qui parle, Log drum, Cowbell 808, effets posés par la recette, ▶ du style avec basse et nappe, cartes de style lisibles avant de choisir, filtres par famille ; 35 recettes réécrites par famille (batteries distinctes, basses audibles, références vérifiées, textes sans jargon) ; prises jusqu'à 5 min, prise précédente, ● qui n'écrase plus un autre passage, prise visible sur la ligne de temps ; Galaxy Tab A11 (600 × 1006 dp) avec captures dédiées (tablet8/), piano roll jouable et plus grand ; langues (Sauvegardé, réglages de prise en mots simples, noms importés jamais traduits). Retours de Benjamin au téléphone : outils de scène compacts, Partager sur une ligne, « Vider » une piste dans la scène, chaque onglet garde son état, cartes repliées sans contenu qui dépasse, « Charley » pour Plaits.

Restant : instruments en stéréo, automation des réglages d'instruments, vraie ligne de temps à clips libres, grille en triolets, breaks échantillonnés (jungle), pont pour la pop, un seul mot pour section/scène/partie ; sons nouveaux à écouter sur téléphone (Log drum, Basse qui parle, Basse wobble).

Relecture neuve (phase 24) : sous-agent qui n'a pas écrit le code. Fil audio sain (0 allocation sur 400 blocs), commandes Kotlin/C++ cohérentes, anciens projets lisibles. Rien de grave.
- Moyens corrigés : une touche du clavier du piano roll relâche sa note même quand l'écran change pendant l'appui (onglet, rotation) et suit le mode accords courant (test) ; le glissé du synthé (Glide) ne se déclenche plus entre deux notes qui se touchent : fin de note sur la même grille swinguée que les débuts, tolérance de 2 échantillons, fins avant débuts au même instant ; un « / » écrit glisse toujours (test moteur, swing 0 / 0,25 / 0,6) ; une prise voix sur une nouvelle scène vide garde sa scène et cette scène n'est plus proposée comme « nouvelle » (test).
- Mineurs corrigés : le bandeau « La boucle passe à 4 mesures » dit aussi quand les parties du morceau s'allongent ; couper le début d'une voix de scène ne la pousse plus après la fin de la scène ; deux appuis rapides sur « Prise précédente » ne laissent plus un son inutile en mémoire ; test du changement de mesure par la session (clip au-delà de la mesure 64) ; commentaire de `clean()` remis en place.
- Vérifié faux : la vérification de silence d'une prise n'emballait pas chaque échantillon (Kotlin parcourt l'intervalle sans objet) ; boucle simple quand même, test d'allocation ajouté.
- Restant : ● sur le synthé allonge encore la scène (et ses parties) à 4 mesures, c'est dit mais pas évité, et si le morceau joue, le changement de scène armé garde l'ancienne longueur ; `followArrangement` ignorerait une courbe modifiée dans la même transformation (aucun appelant aujourd'hui) ; anciens noms de pads de stems « Batterie Song » (sans point) plus traduits ; en 3/4, une boucle de notes d'une mesure en 4/4 devient deux mesures (16 pas non convertis).

## Phase 25 : la boîte à effets partout, huit effets nouveaux, huit emplacements

Demande de Benjamin : la même boîte à effets sur tout ce qui fait du son, les effets les plus utilisés en musique électronique, pouvoir en empiler beaucoup, un Synthé cohérent entre Clavier et Notes, des scènes qu'on déplace au doigt.

Fait :
- Une seule boîte à effets (chaîne d'effets, « + Ajouter un effet » par familles, ambiances suggérées selon le son, effets coupables et déplaçables avec leurs courbes) sur le Synthé, l'Acide, la batterie et chaque pad, les pistes audio, le remix et les pads live, le Mixeur (sortie comprise).
- Huit effets nouveaux : Distorsion, Écho, Écho ping-pong, Réverbe, Auto-pan, OTT multibande, Décalage de fréquence, Arrêt de bande ; six ambiances nouvelles. Écho, réverbe et arrêt de bande empruntent une unité à une réserve préparée avant le flux (12 / 12 / 6), jamais d'allocation dans le fil audio.
- Huit emplacements par voie au lieu de quatre (anciens projets complétés) ; le moteur mesure sa charge et la boîte prévient au-delà de 80 % (téléphone trop chargé).
- Synthé : Clavier et Notes montrent les mêmes réglages du son, dans le même ordre ; « Jeu des accords » (arpège, vitesse, strum) expliqué, désactivé sans accord ; la gamme propose de ranger les notes hors gamme.
- Scènes déplaçables au doigt (case vide : déplacement ; pleine : Échanger ou Remplacer) ; parties du morceau, automation par scène, prises et lecture suivent, en une annulation.

Relecture neuve (sous-agent qui n'a pas écrit le code), tests du moteur aussi sous ASan/UBSan : fil audio sain (ni allocation, ni verrou, ni log dans les nouveaux effets et la réserve), une unité jamais partagée, commandes Kotlin/C++ cohérentes, anciens projets lisibles. Rien de grave.
- Moyens corrigés :
  - Des voies que l'app n'envoyait plus au moteur (piste supprimée, annulée, autre projet) gardaient leurs échos et réverbes : un nouvel effet restait muet en direct alors que l'export le jouait. Une commande `ClearInserts` vide ces voies à chaque synchro (tests moteur et appli).
  - Annuler ou rétablir un déplacement de scène pendant la lecture sautait sur une autre scène : l'étape d'historique porte le déplacement et la scène entendue suit (test pendant la lecture).
  - La limite 12 / 12 / 6 n'était vérifiée qu'au bouton « + Ajouter » : rallumer un effet coupé, une ambiance, dupliquer une piste pouvaient laisser un effet muet sans rien dire. Le ViewModel refuse partout avec le même message ; un projet déjà au-delà le dit à l'ouverture et après Annuler/Rétablir (tests).
- Mineurs corrigés : premier écho à l'heure, sans glissement de hauteur, en direct comme à l'export (test : trame 6000 pile au lieu de 6727 / 6126) ; plus de double remise à zéro des unités, et l'écho et l'arrêt de bande se remettent à zéro sans effacer leurs secondes de mémoire (plus de ~20 Mo effacés dans un seul callback au chargement) ; un bouton MIDI appris suit l'effet déplacé ; « Remplacer » retire les prises audio de la scène remplacée (Annuler les rend, la boîte de dialogue le dit) ; déplacer une scène refusé pendant un enregistrement ; paramètres des emplacements vides ou coupés plus envoyés ; « Non » de l'arpège et du strum toujours actifs ; moteurs d'export et d'aperçu construits hors du fil principal.
- Tests renforcés : « export = direct » avec un vrai écho et une vraie réverbe à mi-mélange ; borne avant le limiteur pour les effets électroniques ; le test des anciens projets enlevait un champ qui n'existait pas sous cette forme (il ne testait rien, corrigé) ; tests de chrono sur le temps CPU du fil, non comptés sur une machine surchargée.
- Restant : le rééchantillonnage du morceau construit encore son moteur sur le fil principal, et chaque moteur privé (aperçus de kit, assistant) prépare toute la réserve (~28 Mo, pas paresseuse) ; la réverbe empruntée est encore vidée dans le callback (0,15 Mo chacune, 1,8 Mo au pire) ; deux touches très rapides dans la liste d'effets peuvent viser le même emplacement ; la table des cases de scène du glisser n'est jamais purgée ; l'avertissement de charge peut rester allumé si le flux audio s'arrête ; la garde « pas de déplacement pendant un enregistrement » n'a pas de test (il faut le moteur) ; la charge mesurée par le moteur reste en temps d'horloge.

## Phase 26 : l'acide sous les doigts

Relecture neuve (sous-agent qui n'a pas écrit le code) : fil audio sain (0 allocation, automation 303 identique en direct et à l'export), anciens projets lisibles. 1 grave, 3 moyens, 9 mineurs ; tout est corrigé avec un test qui échoue avant.
- Grave corrigé : toucher une 2e fois un son déjà entendu (assistant, bibliothèque) laissait la roue « préparation » tourner pour toujours et l'appui suivant ne faisait rien. L'écoute ne démarre plus qu'une fois connue comme l'écoute en cours (test).
- Moyens corrigés :
  - Changer d'humeur après avoir écrit la 303 : ses notes passent dans la nouvelle gamme (accents, glisses et octaves gardés), la 303 du morceau reste juste (tests).
  - Une scène remplacée laisse un effet qui ne joue plus nulle part : la scène remplacée suit la scène déplacée (c'était déjà le cas), et la boîte d'effets le dit en clair, « Ne joue dans aucune scène : touche une lettre ou « Tout » » ; aucune puce allumée à tort ; un tel projet se recharge tel quel (tests, captures).
  - « Coupure » qui revient toute seule pendant le morceau : reprise à la main. Tant que le doigt tient un bouton (ou curseur) de la 303, sa courbe d'automation le laisse au doigt ; relâché, la courbe reprend au pas suivant. En enregistrement, le geste est toujours écrit. Commande moteur générique `SetAutomationHold` (70), réutilisable pour le mode Live et le mixeur (test moteur). Une ligne sous les boutons dit lesquels bougent tout seuls ; le guide des styles acides le dit aussi.
- Mineurs corrigés : « 1 mesure » gardée par le générateur ; 2 → 1 → 2 mesures rend la 2e mesure écrite ; un effet changé ou une voie vidée rejoue partout dans le moteur ; seuls les boutons qu'un couloir pilote dans la scène jouée restent à la valeur de l'automation ; l'écoute « Avec le rythme » passe par les effets du style (303, batterie, sortie) ; exemption i18n resserrée ; densité remise à 80 % en changeant de style ; tests ajoutés (enregistrement du geste, boutons gardés, projets anciens nettoyés).
- Pic de CPU au retour d'un effet : l'écho et l'arrêt de bande se remettaient déjà à zéro en temps constant ; la remise à zéro ne touche plus que l'effet en place (avant : les 22 effets possibles de l'emplacement, dont le chorus). Le chemin « Joue dans » au changement de scène avec écho, réverbe et arrêt de bande est dans le test « aucune allocation ».
- Restant : la réverbe empruntée est encore vidée dans le callback (0,15 Mo chacune) ; les boutons de l'assistant n'ont pas de reprise à la main (aperçu hors ligne, sans courbe) ; dans l'aperçu « Avec le rythme », les effets propres à un pad ne sont pas rendus (pads non routés vers leur voie) ; un bouton par scène peut sauter à sa valeur du projet pendant la dernière mesure de la scène d'avant (réglage 303 non mis en attente) ; au-delà de 8 réglages tenus à la fois, les suivants ne sont pas repris.
