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

## Phase 27 (écrans et styles)

Relecture neuve des écrans Live, de la frise du morceau et des styles : 2 graves, 9 moyens, 14 mineurs. Corrigés ici (sauf M7, côté prise, traité à part), chacun avec un test qui échoue avant quand c'est testable.
- Graves corrigés :
  - « Hachoir de voie » hachait toujours la première piste choisie (le geste gardait l'ancienne action). Tous les boutons à maintenir et le filtre DJ appellent maintenant l'action à jour (test : changer de piste puis maintenir).
  - Le souffle montant soufflait quand une section tournait en boucle (bruit continu dans la Montée, claquement à chaque tour du Drop). Sa courbe est « morceau seulement » : une scène jouée seule le garde fermé ; il monte et claque seulement quand tout le morceau joue (test sur tous les styles qui en ont).
- Moyens corrigés :
  - « Annuler » de « Morceau écrit » n'annule que la prise, et disparaît dès qu'autre chose a changé ; la ligne se ferme (✕) et part en quittant la page (tests).
  - Quitter la page Live pendant une prise demande « Finir la prise ? » (Quitter / Rester) ; « Générer » depuis les pads finit la prise d'abord (test).
  - La grille Live ne se recompose plus à chaque image ni 30 fois par seconde : horloge et clignotement lus au dessin, clignotement seulement si un lancement attend (test qui compte les recompositions des pads).
  - Cibles de 48 dp : onglets du panneau, puces de cible, « Remplacer / Ajouter », bouton M (en-têtes de 80 dp).
  - Pads XY (Live, 303, Effets rapides) : les mots des bouts sont autour du pad, le point ne les cache plus jamais.
  - Ride : la cymbale du kit devient une ride courte (plus aiguë, éteinte en 0,3 s, plus douce) ; Detroit sur 1 et 3, hard techno plus bas.
  - « Grondement » n'est plus proposé sur la voie Batterie entière.
  - Frise du morceau : blocs de 48 dp, le choix de scène survit à la rotation.
- Mineurs corrigés : Répéter relâché remet la division au repos ; pads lus par TalkBack avec piste, ligne et état ; curseurs 303 lisibles et réglables par TalkBack ; le double toucher de la 303 doit tomber près du premier ; le hachoir de voie s'allume aussi quand il est tenu ailleurs ; 303 sans ligne : la page le dit et l'onglet est grisé ; NL « Gewapend » et « spoor » partout ; la piste s'appelle « Souffle montant » (EN/NL « Riser ») et la section « Montée » se dit « Build-up » / « Opbouw » ; cocher le Rumble décoche la Basse ; en techno industrielle, le Rumble reste dans le Break ; le convertisseur de recettes refuse proprement un nombre mal écrit et une basse de secours qui n'est pas une basse ; bords fondus quand la grille, les effets ou la 303 défilent.
- Restant : réglage de volume des styles chargés (hard techno, neurofunk, riddim, acidcore) à vérifier par un rendu hôte du Drop (côté moteur) ; techno : deux filtres en série sur la nappe, à l'oreille ; un vrai son de ride 909 dans le pack tr909 ; le compteur de recompositions est un simple compteur de test dans le code.
## Phase 27 (moteur et morceau)

Relecture neuve du moteur, du modèle et de la prise Live (sous-agent qui n'a pas écrit le code) : fil audio sain (aucune allocation, verrou ou log dans les lancements, le hachoir de voie, les retours), anciens projets lisibles. 1 grave, 6 moyens, 13 mineurs. Tests du moteur aussi sous ASan/UBSan.
- Grave corrigé : pendant le morceau, la grille Live croyait toutes les pistes sur la scène de la partie, au pas lu par la surveillance (souvent en retard) : une prise lancée là écrivait des pistes jamais entendues (batterie coupée qui revient, acide sur la mauvaise scène ou décalée). La grille se construit maintenant à partir de la partie (pistes coupées arrêtées, scène de chaque piste) et des départs exacts du moteur, au changement de partie, à ▶ et quand le Live prend la main (test : ● armé pendant une partie mélangée et vue en retard).
- Moyens corrigés :
  - Les décalages déjà préparés pour la partie suivante étaient écrasés par un renvoi de la partie en cours (casque Bluetooth, effacer une scène…) : `SetSlotOffset` suit maintenant la mise en attente (test moteur).
  - Une prise de plus de 128 parties était coupée sans rien dire : elle s'arrête quand le morceau est plein et le message le dit ; une partie de plus de 256 mesures continue dans la suivante (tests).
  - File de commandes : l'envoi attendait déjà le moteur (jusqu'à 100 ms) ; les courbes partent maintenant en paliers et en rampes (une commande par point dessiné, plus une par temps), et une rafale perdue (flux bloqué) est renvoyée (test du nombre de commandes).
  - Une prise ne détruit plus les courbes existantes : courbe du morceau gardée jusqu'au premier geste, courbe en boucle ou par scène « cuite » temps par temps (ses pas gardés), réglage tenu jusqu'à la fin de la prise (ce qu'on entend est ce qui est écrit). « Remplacer le morceau » coupe les courbes de l'ancien morceau après la fin de la prise (tests).
  - Le Remix arrêté pendant la prise est écrit (coupé dans la partie, nouvelle commande `SetRemixOn`) (tests moteur et modèle).
  - Courbes par scène et effets « Joue dans » envoyés à la scène que joue chaque piste (piste arrêtée laissée telle quelle) (test).
- Mineurs corrigés : marque du morceau posée avant ▶ et suivie si le moteur la prend une mesure plus tard ; valeurs bornées dans le moteur (décalages, pas du morceau, NaN) ; `ArmLaunch` termine une mise en attente ; 32 réglages tenus au lieu de 8 ; hachoir de voie allumé entre deux temps en phase avec le temps ; plus de 16 couloirs : le message le dit ; courbe du morceau calculée en un passage.
- Restant : la colonne Remix coupée n'apparaît pas encore dans la frise du morceau (écran du morceau) ; une file d'événements de lancement pleine perd ces lancements pour la prise ; les clips audio « une fois » et une piste audio reprise en cours (`from`) restent approximatifs ; `automationBefore` ignore `play`/`from` ; scènes déplacées pendant une prise non suivies ; une courbe en boucle cuite perd le détail au double-croche ; pas de test bout en bout Kotlin → moteur (le test moteur rejoue les commandes comme l'appli les envoie).
## Phase 27 (effets du Live)

Relecture neuve des effets du Live (sous-agent qui n'a pas écrit le code) : son en temps réel sain (aucune allocation, verrou ou log ajouté), réglages et sauvegardes bornés. 2 graves, 7 moyens, 10 mineurs. Corrigés avec un test qui échoue avant ; tests du moteur aussi sous ASan/UBSan.
- Graves corrigés :
  - Montée puis Drop (ou filtre DJ verrouillé, retours remis par Drop / « Tout remettre ») : le morceau glissait vers le repos pendant le dernier temps (les roulements s'arrêtaient avant le drop). Une courbe de prise peut maintenant « sauter » à un point : le moteur tient le temps d'avant (nouvelle commande `SetAutomationSongFlat`, champ `songJumps` sauvegardé). Drop, « Tout remettre », un lâcher au repos et un geste après un temps immobile sont écrits en saut (tests modèle, ViewModel et moteur « prise contre morceau » avec gestes hors temps, et l'effet change bien le son).
  - Quitter la page Live laissait la Montée (et ce que le verrou gardait) jouer en cachette : la fermeture de la page remet tout au repos, écrit dans une prise (`leaveLive`, test).
- Moyens corrigés :
  - Drop puis Stop avant la mesure : le moteur règle le Drop en attente à l'arrêt, et Stop envoie toujours la remise à zéro (tests moteur et ViewModel qui capte les commandes).
  - Rotation : verrou, page, mode verrou et boutons gardés de piste survivent (état sauvegardé ; on ne relâche plus à la destruction, mais à la fermeture de la page) (test).
  - Les boutons de piste gardés s'éteignent (et perdent le cadenas) quand Drop ou Stop les lâche ; un seul toucher les relance (test).
  - Un nouvel effet juste après un effet de fin (écho, freinage, rembobinage) coupe cette fin : plus de quasi-silence jusqu'à la mesure (test moteur).
  - Filtre DJ et Montée ne recomposent plus toute la page : l'état des effets est lu dans les petits contrôles (test qui compte les pads de la grille).
  - Tests ajoutés : Stop avec capture des commandes moteur, prise contre morceau avec relâchés hors temps, clic au Drop.
  - Geste entre Drop et sa mesure : il joue jusqu'à la mesure puis s'éteint à l'écran, écrit au repos sur la mesure (comme l'export) ; un appui dans le dernier demi-temps n'est ni joué ni écrit (le Drop l'effacerait) (test).
- Mineurs corrigés : la Montée s'efface en 2 ms au Drop (plus de clic, test) ; fin du bruit qui monte en 4 ms ; Drop ne relâche plus les réglages tenus avant sa mesure ; boutons tenus, pads Répéter et pads XY utilisables avec TalkBack (toucher = allumer/éteindre, pas à pas et « Remettre ») ; spectre mesuré seulement quand un pad XY est à l'écran (test) ; « Deux touchers : son enregistré » et « Drop à la prochaine mesure… » ne sont plus coupés (tablette 8").
- Restant : `nextGridLine` sur une ligne exacte (rare) ; les effets tenus finissent au lâcher, et la grille à la double croche est écrite au temps le plus proche (jusqu'à 2 double croches d'écart dans l'export, limite connue) ; écouter une ambiance en maintenant n'a pas d'équivalent TalkBack ; fondu du crossover des coupes (inaudible) ; réglages des effets changés pendant une prise peut-être dans la même annulation (à vérifier) ; la page se replie toujours sur les puces de pages qui défilent.
## Phase 28 (musique)

Audit « musique » d'un producteur (0 grave, 5 moyens, 8 mineurs), tous vérifiés dans le code puis corrigés avec un test qui échouait avant.
- Moyens corrigés :
  - Progressions, pads, clavier en mode accords : les accords de la gamme elle-même ; Penta mineure et Blues prennent ceux de la mineure, « Libre » et Penta majeure ceux de la majeure (plus de La majeur dans I–V–vi–IV en « Libre »). Étiquettes des progressions calculées dans la gamme, accord diminué remplacé par le plus proche (test sur toutes les gammes, toniques et progressions).
  - Titre des pads : « Accords de La mineure » en Penta mineure / Blues.
  - Acide › Générer reste dans la gamme du projet (depuis la tonique de la 303, pas verrouillés gardés, feel du style) (test sur 1 600 lignes).
  - Leads de l'assistant bornés (note haute ≤ Do7, médiane ≤ Mi6), hoover de la jungle descendu ; piano roll jusqu'à Do8.
  - Lead en arpège avec un stab de pack : une frappe par accord, sans arpège.
- Mineurs corrigés : noms d'accords et de notes épelés selon la tonalité (Sol♯ m en Mi majeur, Sol♭ en Mi♭ mineur), accord augmenté « III+ / Do+ » ; la 303 reste dans le Break d'acid house / acid techno avec son balayage ; phrase « joue dans toutes les sections » seulement si c'est vrai ; plus de section muette (Rumble seul, nappe seule en acidcore) ; lo-fi 3 min 12 (second couplet de 8 mesures) ; sus2 phrygien → sus4 ; basse house/UK garage sous les accords ; piste d'accords nommée Piano / Orgue / Stabs / Nappe… selon le son.
- Test « sonde » : chaque style × ambiance × parties × 303, et des sous-ensembles de lignes : notes dans la gamme, plages (basse, nappe, lead ≤ Do7), chaque partie et ligne choisie entendue, aucune section muette, ≤ 3 min 30 (`MusicTheoryTest`).
- Restant : l'export MIDI sans arpèges ni roulements (déjà noté phase 16) ; le style d'une ligne acide n'est retrouvé que par le nom du projet (renommé : feel classique, gamme gardée) ; le clavier peut encore écrire au-dessus de Do8 (octave 7, deux octaves) ; une 808 de trap descend à Do♯1 (35 Hz), voulu par le genre.
## Phase 28 (Live)

Audit pro « live » (2 graves, 6 moyens, 6 mineurs) et deux points de l'audit « données » (prise Live perdue à la fermeture, réglages du Live défaits par ↶). Chaque correction a un test qui échoue avant ; tests du moteur aussi sous ASan/UBSan.
- Graves corrigés :
  - Après une retouche du Morceau (partie insérée, supprimée, déplacée, répétée, scène allongée), le Drop glissait de nouveau : les sauts de la courbe suivent maintenant leurs points (test sur chaque retouche).
  - Une prise longue s'arrêtait seule à 256 mesures sans rien dire : les courbes tiennent 1024 mesures (moteur et modèle), et au-delà la prise continue (lancements écrits) ; la ligne REC prévient 16 mesures avant la fin des effets et 8 parties avant la limite des 128 parties, le message de fin le redit (tests moteur et modèle).
  - Prise Live perdue si l'appli est fermée ou tuée pendant la prise : elle est gardée sur disque toutes les 5 s, en arrière-plan et à la fermeture, et écrite dans le morceau à la réouverture ; les dernières modifications non sauvées partent aussi à la fermeture (test).
- Moyens corrigés : ■ Fin pendant qu'un pad attend (le lancement a lieu à la mesure, test moteur) ; « Tout remettre » annule le Drop en attente (plus d'effet tenu coincé, test) ; appui court lâché avant sa ligne : ni joué ni écrit (test) ; pad Wobble, « Effets… », volume, panoramique et manette MIDI écrits pendant la prise (test) ; pads lancés à l'appui, repris si le doigt fait défiler la grille (test d'interface) ; pad seul : effets de toutes les voies de sa piste et réglages 303 remis comme dans le morceau (test).
- Mineurs corrigés : anneau d'attente avec le « Lancer à » du lancement armé ; « Ajouter à la suite » refusé pendant l'armement si le morceau est plein ; pads lancés un par un sur une scène : elle devient la ligne et son master suit ; batterie arrêtée : la partie suivante garde son compte pour les courbes en boucle ; ligne REC « M et les pads de l'onglet Pads ne sont pas enregistrés » ; « Lancer à » et réglages des effets Live gardés après ↶ / ↷ d'autre chose (tests).
- Restant : la limite de 16 couloirs d'automatisation (un set riche la dépasse, le message le dit) ; le master qui suit une ligne faite pad par pad est envoyé au relevé du lancement (~30 ms après), pas à l'échantillon ; batterie arrêtée sur une autre scène que la ligne : le compte écrit suit la boucle de la ligne ; un lancement déjà passé quand le doigt glisse reste joué ; un projet de 1024 mesures de courbes par temps grossit le JSON (≈ 60 Ko par courbe).
## Phase 28 (sauvegarde)

Audit « données » (perte et corruption du travail) : 1 grave, 6 moyens, 8 mineurs. Le grave (prise Live perdue à la fermeture) et « Lancer à / réglages Live défaits par ↶ » sont traités avec la page Live. Le reste corrigé ici, chacun avec un test qui échoue avant.
- Moyens corrigés :
  - Le .zip d'un gros projet ne se réimportait pas (« projet trop gros ») : le projet est écrit compact (4 fois plus petit) et la limite passe à 32 Mo (les anciens fichiers indentés passent aussi) (test : 24 scènes × 8 pistes).
  - Enregistrement sans pause : la sauvegarde automatique ne partait jamais. Elle part maintenant au plus 2,5 s après la plus ancienne modification non écrite, et une sauvegarde en cours n'est plus annulée par le geste suivant (test : une note toutes les 300 ms).
  - Scènes non affichées non nettoyées au chargement (export MIDI qui bouclait sans fin, grille Live qui plantait) : chaque scène est nettoyée ; l'export MIDI et la grille Live tiennent même sur des données jamais nettoyées (tests).
  - Corbeille : « Supprimer définitivement » par projet et « Vider la corbeille », avec confirmation ; le message « appareil plein » y renvoie (tests écran et ViewModel).
  - Sauvegarde Android : règles `data_extraction_rules.xml` et `backup_rules.xml` ; le cloud (25 Mo au plus) ne garde que projets, favoris et préférences ; modèles, packs, banques, morceaux séparés, caches et latence du téléphone exclus ; un test classe chaque dossier de `filesDir`.
  - Favoris illisibles : mis de côté (`favorites-illisible-<date>.json`) et signalés, jamais écrasés.
- Mineurs corrigés : `.wav.tmp` effacé quand l'écriture échoue et au ménage (plus d'une heure) ; dossiers de projet inachevés (sans project.json, plus d'une heure) effacés à l'ouverture de « Mes projets », un premier enregistrement complet coupé juste avant le renommage est remis ; NaN remplacés avant d'écrire (tempo, swing, voies, force des coups, synthé, acide) ; le .zip d'un Remix emporte son morceau séparé (remis dans `stems` à l'import s'il manque) ; un son absent du .zip ne refuse plus tout l'import (pad remis au son du kit, message) et l'export le dit ; « Dupliquer » et « Envoyer (.zip) » refusent si le projet ouvert n'a pas pu être sauvegardé ; nettoyage d'un projet déjà propre : chaque partie propre est rendue telle quelle (plus de tri ni de comparaison note à note à chaque geste) (test d'identité).
- Restant : un projet dont les sons dépassent 25 Mo n'est toujours pas dans la sauvegarde cloud (Android n'accepte pas de filtre par fichier ; le transfert d'appareil à appareil, lui, emporte tout) ; si le renommage « illisible » d'un projet échoue, l'autosave l'écrase toujours au bout de deux sauvegardes (rare) ; le .zip d'un Remix peut peser plusieurs centaines de Mo (4 pistes en float) ; `withFittedSampleRoots` et le nettoyage du mixeur et des courbes restent recalculés à chaque geste (petits).
## Phase 28 (appli Android)

Audit Android (coque de l'appli, cycle de vie, permissions, accessibilité, mises en page) : 0 grave, 5 moyens, 12 mineurs. Corrigés avec un test qui échoue avant, sauf mention.
- Moyens corrigés :
  - Taille de l'interface 115 / 130 % (et téléphones de 360 dp) : l'écran voit enfin la largeur qu'il a vraiment (`UiScaled` : densité et `LocalConfiguration` ensemble). La barre du Live garde toujours ● REC, le compteur et ▶ (← seul et « Lancer à » qui s'abrège quand c'est étroit). Les libellés rapetissent au lieu de couper un mot en deux (`WholeWordsText` : pads, Importer / Micro, Effets du pad, Bibliothèque, Partager, pistes audio) ; « Taper » passe sous le tempo ; puces des sons du synthé et effets en place sur deux lignes. Nouvelles captures `scale130/` (FR et NL, 18 écrans) ; tests `UiScaleTest`, `LiveTopBarTest`.
  - Téléphone couché, navigation à 3 boutons : la barre du haut évite la barre de navigation et l'encoche (`safeDrawing`), ainsi que les bandeaux dessous (test `TopBarInsetsTest`).
  - Mixeur : les vumètres sont lus au dessin seulement et publiés seulement quand ils bougent ; à l'arrêt, plus rien n'est recomposé (test `MixerMetersTest`).
  - Partager mon morceau : la fenêtre de partage attend le retour dans l'appli, et l'étape n'est cochée que si elle s'est ouverte (test `ShareWhileShownTest`).
  - M3 (sauvegarde Google) : traité par le correcteur « données ».
- Mineurs corrigés : « Autoriser » des notifications ouvre les réglages après deux refus, canal « Lecture » coupé = « à faire » ; accordeur qui relit le micro au retour (sans test) ; réinitialiser garde C D E en anglais et néerlandais ; recherche Bluetooth MIDI arrêtée au changement de projet ; double toucher à l'accueil = un seul projet ; son de test 440 Hz coupé en quittant sa page ; lien GitHub raccourci aussi pour les longs commentaires ; « Passer » vide retiré ; cibles de 48 dp (▶ du haut, puce de scène, carte « Prochaine étape », Partager du mixeur) ; texte rouge et texte secondaire à 4,5:1 (`Neon.RedText`) ; un son en chargement ne se pose plus après la fermeture de la bibliothèque (jeton dans le ViewModel, sans test).
- Restant : ouverture d'un projet toujours sur le fil principal (jusqu'à 3 s après une longue prise) : la rendre asynchrone touche la sauvegarde et la première écriture automatique, à faire avec le correcteur « données » ; sur la carte des pads live, « Genereren » / « Van het apparaat » se coupent encore à 130 % (page Live, autre correcteur) ; « Taper » seul sur sa ligne à 130 % (lisible, un peu de place perdue).
## Phase 28 (moteur)

Audit du moteur audio (sondes compilées contre le cœur, reprises en tests hôtes) : 1 grave, 3 moyens, 5 mineurs. Chaque correction a un test qui échoue avant ; tests du moteur aussi sous ASan/UBSan.
- Grave corrigé : export « Piste par piste » d'une prise Live avec Montée, Riser ou Downlifter : leur souffle était dans chaque fichier (6 pistes ≈ +15 dB de souffle). Les pistes le laissent maintenant de côté et un fichier de plus, « Effets live », le contient seul (`SetMaster a=3`) : la somme des fichiers redonne le mix (écart −130 dB au lieu de −17 dB).
- Moyens corrigés :
  - Écho d'insert « 1/2 » et « 1/4 pointée » en avance sous 75 BPM (et écho d'envoi « 1/2 » sous 48 BPM) : tampons de 3,05 s (une blanche à 40 BPM), commentaire faux corrigé ; +6,7 Mo par moteur (test : chaque valeur, 40 à 300 BPM, à 1 ms près).
  - Roulement sur un pas swingué : ses coups restent dans le pas (répartis sur sa vraie longueur), plus de flam avec le pas suivant (test à swing 0 / 0,3 / 0,6 / 0,75).
  - Garde-fou NaN du son général : vérifié avant les effets du Live (qui effaçaient le NaN sans rien dire) et après eux ; la source est remise à zéro, le son revient au bloc suivant et c'est signalé aux rapports (test).
- Mineurs corrigés :
  - Réglages NaN ignorés partout (aide commune `clampSetting`) : volume, panoramique, envois, réverbe et écho d'envoi, son de test, synthé, acide, Plaits, effets, stems ; les deux plantages (temps de l'écho NaN → `kSteps[INT_MIN]`, Plaits decay/couleur NaN) ont disparu (test « fuzz » de 1 435 cas : NaN, ±Inf, ±1e30).
  - Synthé : une note prend sa hauteur et son filtre dès sa première trame (plus 15 trames à l'ancienne hauteur) (test).
  - Export = direct échantillon pour échantillon quelle que soit la taille des blocs (192, 7, 997 contre 512) : batterie (rien calculé avant un coup ni après sa fin, bruit propre à chaque voix, volume d'un nouveau coup à sa trame), synthé (tic de contrôle compté d'un appel à l'autre), Plaits (note au premier bloc de 12 à ou après elle, bruit propre), wah (test : écart 0).
  - LFO libres (flanger, phaser, trémolo, robot, vinyle, décaleur, LFO du synthé) et bruits remis au départ à ▶, comme l'export (test « prise après avoir joué » contre export neuf).
  - Charge CPU : le téléphone applique au plus 1 024 commandes par rappel (un changement d'effet compte 8 ; une scène en attente passe entière) : 4 096 bascules réverbe/aucun coûtaient 7,8 ms dans un rappel, 1,4 ms maintenant ; 4 096 réglages de synthé 2,7 → 1,7 ms (mesure sur PC partagé). Le coût en régime établi (≈ 1,2 à 3,6 ms par rappel de 192 trames dans le pire projet) ne change pas.
- Restant : le chorus (DaisySP) et la macro « Mouvement » gardent leur LFO libre (le remettre efface leur ligne à retard) ; les charlestons de la batterie gardent la phase de leurs oscillateurs d'un coup à l'autre (écart ≤ 0,03, inaudible) ; le hasard partagé de DaisySP et de Plaits n'est déterministe que si un seul moteur joue à la fois (un export pendant qu'on joue mélange les deux, sans effet audible) ; sauter les effets au repos (gain CPU possible) demanderait de faire avancer leurs LFO à part : pas fait ; à ▶, un LFO remis au départ peut faire un tout petit saut sur un son qui traverse déjà l'effet (une queue de réverbe, une note tenue au clavier).
## Phase 28 (relecture)

Relecture neuve de toute la phase (`git diff ad8e680 HEAD`) : 2 graves, 4 moyens, 9 mineurs. Corrigés avec un test qui échoue avant, sauf mention.
- Graves corrigés :
  - G1 : les sous-modules remplacés par des liens locaux à la fusion : remis par l'intégrateur (commit a94bbad).
  - G2 : une prise Live finie (ou annulée) pouvait être réécrite dans le morceau à l'ouverture suivante (une écriture du journal arrivée après « ■ Fin »), et les retouches faites ensuite perdues. Un seul écrivain pour les journaux (`TakeJournalStore` : un fil, dans l'ordre demandé), chaque prise a un identifiant ; une prise finie est « scellée » (son écriture en cours est jetée juste avant de remplacer le fichier) ; le journal n'est effacé qu'une fois le morceau qui la contient enregistré ; le projet garde l'identifiant de la dernière prise écrite (`Project.liveTake`) : un journal retrouvé après coup n'est jamais écrit deux fois (tests : écriture en cours à « ■ Fin », journal retrouvé après la sauvegarde, après une récupération, écran fermé pendant la prise).
- Moyens corrigés :
  - M1 : écritures du journal sur un même `.tmp` : un fichier temporaire par écriture, remplacé d'un coup (`ATOMIC_MOVE`), jamais d'effacement avant ; même écrivain pour l'arrière-plan et la fermeture (test : 40 écritures à la suite, la dernière gardée, aucun `.tmp` restant).
  - M2 : journal encodé sur le fil principal toutes les 5 s, de plus en plus gros : le fil principal ne copie plus que les listes (0,02 ms), l'écrivain encode ; la prise ne garde que 3 gestes par réglage et demi-temps (le premier, le premier d'une autre valeur, le dernier), ce qui écrit exactement le même morceau (test sur 300 prises au hasard) ; un set de 30 min avec un XY et un bouton tournés la moitié du temps : 32 500 gestes gardés sur 162 700, journal de 2,7 Mo (test de coût).
  - M3 : passer de 100 % à une autre taille d'interface recomposait toute l'appli (onglet, page Réglages et dialogues perdus) : `UiScaled` garde toujours le même arbre (test : onglet et dialogue gardés en passant 100 → 115 → 100 → 130 %).
  - M4 : opérations de « Mes projets » en parallèle : une file unique (verrou), boutons de la corbeille, « Annuler » et « Supprimer » coupés pendant qu'une opération tourne (test : « Restaurer » pendant « Vider la corbeille » attend, rien de restauré à moitié).
- Mineurs corrigés :
  - m1 : journal effacé seulement après la sauvegarde de la prise (avec G2).
  - m2 : journaux des projets supprimés pour de bon (et `.tmp` laissés par un arrêt brutal) effacés : à « Vider la corbeille », « Supprimer définitivement » et à l'ouverture de « Mes projets » ; ceux de la corbeille restent (test).
  - m5 : une sauvegarde en cours d'écriture compte aussi à la fermeture d'un écran (`ProjectSession.settled`) : le suivant l'attend ; mettre à la corbeille attend les dernières sauvegardes de l'écran fermé (tests).
  - m6 : chaque doigt garde la grille d'avant son propre appui (un 2ᵉ doigt n'empêche plus de reprendre le 1ᵉʳ) (test) ; un doigt qui arrête un défilement lancé ne lance rien (test sur le geste).
  - m7 : tests des pièges de chemins `stems/` (`..`, barres inverses, `.`, niveaux en trop) ; `project.json` d'un .zip passe par un fichier et se lit au fil de l'eau (plus de texte de 32 Mo en mémoire).
  - m8 : le hasard de Plaits (`stmlib::Random`) est un atomique relâché (c'était une variable ordinaire écrite par deux fils : course de données) ; test hôte sur deux fils, propre sous ThreadSanitizer.
  - m9 : tests manquants ajoutés (ci-dessus) ; ménage des dossiers sans project.json déjà couvert.
- Mémoire native par moteur (m3, documenté) : environ 35 à 40 Mo résidents (inserts 9,7 Mo, `LongFxPool` 18,4 Mo dont +6,7 Mo cette phase, automation par temps 0,34 Mo, tampons des effets du Live). Jusqu'à trois moteurs à la fois (le direct, un export ou une piste séparée, un aperçu de la bibliothèque ou un rééchantillonnage) : ≈ 110 Mo natifs. Pas d'OOM Java, mais un risque de « low memory killer » sur un téléphone de 2-3 Go en arrière-plan.
- Restant :
  - m3 : un moteur « léger » pour les aperçus (sans `LongFxPool`, ou construit à la demande) : pas fait.
  - m4 : priorité aux lancements et notes derrière une grosse resynchro, et poids plus fort des effets longs dans le budget de commandes : pas fait (moteur, phase suivante).
  - m8 : le hasard de Plaits et de DaisySP reste partagé entre moteurs (pas de stockage par fil : bionic réserve de la mémoire et prend un verrou à son premier usage dans une bibliothèque chargée par dlopen) : un export pendant qu'on joue mélange leurs bruits, sans danger ni effet audible.
  - m6 : deux doigts sur la même colonne, ou une ligne puis un pad : le retrait du premier reprend aussi le second (rare).
  - Un journal Live de 30 min de gestes continus pèse encore ~2,7 Mo, réécrit toutes les 5 s hors du fil principal.

## Phase 29 (relecture unique)
Relecture neuve : 0 grave, 3 moyens, 4 mineurs. Fil audio propre (limiteur, vumètres, fondus, pitch bend, gain par son), anciens projets inchangés.
- Corrigé : échantillon absurde ou non fini avant le limiteur remis à 0 (plus de NaN possible) ; file des chansons séparées retirées portée à 64 (plus de fuite pendant un long glissé de tempo).
- Restant : `asNewProject` considère « 0 dB » comme « non choisi » (aucun appelant ne le règle exprès aujourd'hui) ; un clic possible à l'écrasement de la queue d'un pad ; fondu d'entrée d'un échange qui peut démarrer un peu tard ; canal de notification d'export recréé à chaque mise à jour.
- Corps : le Break n'est que 2 dB sous le Drop quand une scène tourne seule (le balayage n'agit que dans le morceau) ; charleys du kit encore ternes ; leads un peu forts en trap et drill. À écouter sur téléphone.

## Phase 30 (relecture unique, légère)
Relecture neuve : 0 grave, 4 moyens, 8 mineurs (détail dans la sortie de la relecture, résumé ici).
- Corrigé : un pack en échec reste proposé par « Tout compléter » (seuls les packs installés sont marqués vus) ; place libre vérifiée avant chaque pack (zip + sons) ; le chrono du morceau suit l'arrêt (lecture du transport observée).
- Restant : une vieille copie locale du catalogue passe avant l'index épinglé (plus ancien en pratique ; URL et SHA-256 restent vérifiés) ; tests manquants : échec puis « vu », reprise HTTP Range, zip piégé à `unpack` (l'extraction est déjà aplatie).

## Phase 31 (relecture unique, légère)
Relecture neuve : 0 grave, 3 moyens, 8 mineurs. Aucun changement du moteur ni du fil audio ; barres système rendues en quittant Live et Ligne du temps ; moteurs hors-ligne libérés.
- Corrigé : les pads live du morceau se calculent sur le projet tel qu'il est après le chargement des sons ; la boucle de la Ligne du temps s'arrête en la quittant ; aperçus des clips simplifiés en zoom arrière (plus de milliers de dessins par image) ; « ouvrir dans l'éditeur » pendant la lecture n'ajoute plus d'étape d'annulation ; la carte des familles s'ouvre sur un style en préparation.
- Restant : un Annuler juste après le remplissage des pads live peut redemander leur calcul au prochain chargement (sans perte) ; état écrit pendant la composition (`mixerVisit`, onglet vu) ; piste de travail non suivie dans Synthé ; `stepsPerBar` figé dans le suivi et animations de zoom superposées (corrigés en phase 32) ; boucle non recalée après coupe/insertion/déplacement d'une partie ; vue Notes forcée une fois après restauration ; halo du potard qui peut revenir ; tests manquants : `fillLivePads`, recalage de la boucle, grille Live sans défilement.

## Phase 32 (relecture unique, légère)
Relecture neuve : 0 grave, 3 moyens, 7 mineurs. Pas de changement du moteur.
- Corrigé : un projet avec une prise Live en cours n'est jamais supprimé sans corbeille ; si la sauvegarde échoue avant la suppression du projet ouvert, il reste en place ; barres du téléphone comptées (pads plein écran par-dessus le Live ne les font plus réapparaître).
- Restant : un projet vide seulement renommé part sans corbeille ; tests de `isUntouched` légers ; deux sondages d'état quand les pads plein écran s'ouvrent depuis le Live ; `PadClock` sans test dédié ; toucher un pad vide en plein écran relâche les effets ; `drawPreview` reparcourt les boucles depuis 0 ; état de la vue écrit pendant la composition.

## Phase 33 (relecture neuve, complète)
Relecture neuve de toute la phase (« Finir son morceau » + son mesuré et honnête) : 1 grave, 8 moyens, 22 mineurs. Le cœur DSP est propre (aucune allocation, verrou, journal ni JNI dans `process()` ; tout préalloué dans `prepare()` ; paramètres qui glissent) ; les chiffres tiennent (cible −8 à l'export : −8,04 LUFS, TP −1,16 dBTP ; en direct −8,99 après 60 s ; repliement ≤ −70 dB ; 44,1 kHz / 16 bits : TP −1,10). Les problèmes étaient aux coutures entre les quatre chantiers.
- Corrigé (interface, modèle) :
  - G1 : les pads live d'un morceau de l'assistant étaient masterisés un par un à la cible du morceau (chaque boucle de piste seule aussi forte que le mix, écrasée, puis remasterisée en direct) : `stemView` retire la cible, `fillLivePads` la redit au moteur privé ; tests modèle et appli (faux moteur qui enregistre les commandes).
  - M1 : « Prêt à partager ? » lit enfin la mesure du master (LUFS sous la durée, true peak > −1 dBTP = saturation) ; avec une cible, l'alerte « limiteur » se tait et « Baisser le volume général » n'est plus proposé.
  - M5 : vocabulaire (« Partie », onglet « Structure ») reporté à la phase 36 avec la nouvelle navigation (DECISIONS) ; en attendant « Modifier la structure » devient « Modifier le morceau » (le bouton nomme l'onglet qui existe).
  - M6 : bandeau de lecture à 48 dp, chaque commande touchable sur 48 × 48 dp au moins (test `PlayStripTouchTargetsTest`).
  - M7 : en paysage, « Prochaine étape » n'est plus empilé (il revient sur Rythme à la demande d'Outils › Aide) et le bandeau Solo se replie en pastille dans le bandeau de lecture ; capture `rhythm_landscape`.
  - Mineurs : m7 (crête récente affichée : la puce LUFS s'allume en ambre une seconde après un dépassement de −1 dBTP), m18 (position verte en lecture, comme ▶), m20 (le champ dit « Nom du morceau (et du projet) » : partager renomme le projet), m21 (test de l'ancienne clé `listenSong`).
- Corrigé côté moteur (agent du banc) : M2 (budget CPU de la chaîne master et des effets de drive), M3 (gain auto du compresseur qui gonflait 1,5 s après un silence), M4 (rapport du banc après l'étape 2, CI qui échoue sur TP > −1 dBTP ou LUFS hors cible).
- M8 : changelog, recette, PROGRESS livrés dans le commit de livraison.
- Restant (mineurs, à reprendre quand on retouche ces fichiers) :
  - m1, m2 : `Saturator::update()` recalcule 64 sinus à chaque `setParam` ; `wetGain_` saute pendant que `drive_` glisse (≤ 1 dB à drive fort avec « Volume constant »).
  - m3 : pendant le fondu de porte d'un insert (`processInserts`), le sec n'est pas retardé des 4 trames de Saturation/Distorsion : peigne à 6 kHz le temps du fondu.
  - m4, m5, m6 : latence de la chaîne master = 68 trames (les docs disent 64) ; commentaire « 24 taps » pour 32 dans `TruePeak.h` et `static_assert(kClipDelay < 8)` ; `static_assert` sur la numérotation à part de `SetKitVersion`.
  - m8 : défaut moteur du compresseur (Auto à 0) différent du défaut de l'appli (Auto à 1), sans effet puisque tout est envoyé.
  - m9, m10 : garde-fou `saved.size <= 4` d'`upgraded()` à tester sur toutes les recettes et préréglages ; préréglage « Punch » qui perd son +8 dB fixe (cohérent avec la décision, à écrire).
  - m11 : le `.pscmd` du banc ne porte ni fréquence ni bits (un export 44,1 kHz / 16 bits n'est pas rejouable).
  - m12 : la CI n'installe pas ffmpeg pour les tests moteur (`loudness_matches_ffmpeg` passe en silence s'il manque).
  - m13 : pendant la montée du gain vers la cible (jusqu'à ~7 s), le panneau ne dit rien tant que le drive est à 0.
  - m14 : seules deux voix du kit v2 sont contrôlées par un test hôte, pas la table entière.
  - m15, m16 : fichier `.premaster` laissé à côté du WAV si l'appli meurt pendant l'export (`WavExporter` ne l'efface pas) ; trois allocations d'une `MasterChain` par export.
  - m17 : regarder une scène à l'arrêt déplace le départ de ▶ sur sa première partie (voulu, testé, dit sous ▶) : à surveiller en recette.
  - m19 : sur l'onglet de l'étape, la carte « Prochaine étape » se déplie dans la colonne du haut et fait descendre tout l'écran.
  - m22 : `read16` (arrondi + dither + écrêtage avec gain) sans test.
## Phase 33 (son : chaîne master, banc)
Relecture neuve de la phase (M2, M3, M4 côté moteur et banc) appliquée ; les chiffres sont dans `docs/son/banc/apres-etape2.md`.
- Corrigé : lecteurs true peak portés (chaîne master 2,0 → 1,6 % d'un cœur hôte) ; « Gain auto » du compresseur qui gonflait après chaque silence ; continu de la 303 (bloqueur après sa saturation) et continu laissé par l'écrêteur et le limiteur (bloqueur sub-sonique 0,05 Hz, avec cible) ; le banc fait échouer la CI sur TP > −1, continu > 1e-3, NaN et sonie hors cible sans plafond ; format d'export noté dans le `.pscmd` ; `.premaster` orphelins nettoyés.
- Restant : les effets de drive (Saturation 0,4 %, Distorsion 0,8 % d'un cœur hôte par insert, soit 1,5 à 3 % d'un cœur de téléphone contre 1 % visé) : l'antidérivée en double avec un logarithme par point suréchantillonné est le coût ; pistes : ×2 au lieu de ×4 sous drive 0,5, courbe tabulée, ou chemin direct quand le drive est au plancher ; à mesurer sur le téléphone d'abord. La basse du synthé laisse −0,01 de continu et des oscillations sub-soniques (sub à 20 Hz) : à régler avec les sources de l'étape 7 (bloqueur à la sortie des voix). 8 styles restent 1 à 2 LU sous leur cible, plafonnés par la perte de 6 LU : c'est le kick (étape 3). Drop − Break passe de +1,4 à −0,2 LU médian : le limiteur écrase plus le Drop ; la courbe d'énergie est l'étape 6. Le HIIR de l'écrêteur monte toujours même quand il ne peut pas écrêter (continuité de ses filtres) : un réchauffement sur les 8 dernières trames permettrait de le sauter. PLR < 6 sur acid_techno, acidcore et gabber : mécanique (cible −7/−6 avec un plafond à −1,2 dBTP visé), le tableau PLR de son.md § 2.4 est à relire pour ces familles.

## Phase 34 (relecture neuve, complète)
Relecture neuve de toute la phase (feuilles en un geste, S1 mélodies, S2 formes, S3 moteur et batterie, S4 synthés larges, S5 mix et master) : 0 grave, 3 moyens, 16 mineurs. Le fil audio est propre : aucune allocation, aucun verrou, aucun journal ni JNI dans les nouveaux chemins (`StudioVoices.h`, `DrumKit`, roulements et décalages, `KeyedDucker`, `SoftClipper`, `PlateReverb`, `Synth` S4, mode Drop) ; tous les index venus de l'appli sont bornés (pas, pads 16..31, clé de sidechain, membres du bus Musique, voix et réglages des pads, couches de l'échantillonneur, paramètres de synthé automatisés) ; NaN filtrés à l'entrée, `DenormalGuard` en place. Les données sont sûres : chaque nouveau champ est omis du fichier à sa valeur par défaut, borné au chargement, et les anciens projets gardent kit v1/v2, son v1 (mono), mix v1 (cible sur tout le morceau), bus à 4 réglages complétés par des valeurs neutres, patchs de synthé à 29 valeurs complétés par des réglages S4 éteints (tests d'aller-retour et d'anciens fichiers présents).
- Corrigé (moteur, avec tests hôtes qui échouent sans la correction) :
  - M1 : quitter le morceau en cours de lecture (« ⟳ Partie » armé tout seul par une retouche, une autre partie choisie, fin d'une prise) laissait le volume général là où la courbe du morceau l'avait mis : pendant un fondu d'entrée ou sur le temps de silence avant un Drop, la partie bouclait en silence. `Engine::leaveSongNow` remet le volume général à l'unité (le modèle le dit déjà : une partie seule joue à son niveau).
  - M2 : le coup en avance (décalage négatif) du premier pas de la mesure où le morceau s'arrête était quand même programmé depuis le pas d'avant et sonnait après la fin (export compris). La lecture anticipée s'arrête sur la mesure de fin.
- Restant :
  - M3 (corrigé ensuite : `routesPad()` compte le solo) : un pad mis en solo sans tranche à lui (`routesPad()` ne compte pas le solo) fait taire toute la batterie, lui compris ; l'appli n'offre pas encore le solo d'un pad (seul le test hôte le fait, avec `SetPadStrip`). À corriger avant d'exposer ce solo : `routesPad() = hasOwnFx() || mute || solo`. Les solos de pads, sans effet avant la phase 34, comptent maintenant : un vieux fichier qui en porterait un sonnerait autrement (aucun chemin de l'interface ne permettait d'en écrire).
  - m1 (corrigé, chantier « restes » : niveau moyen corrigé du biais, le poids s'efface dans le silence ; test `restes_soft_clipper_after_silence`) : `SoftClipper` part d'un niveau moyen nul après un silence : les premières millisecondes de chaque attaque après un blanc sont écrêtées fort (plafond à −60 dBFS × marge), un grain sur les basses détachées. Piste : partir du niveau de crête récent, ou ne pas écrêter tant que la moyenne n'est pas établie.
  - m2 (corrigé : le chemin séparé entre en fondu de 10 ms et reste jusqu'au `reset` ; test `restes_keyed_duck_split`) : `KeyedDucker` repasse du chemin séparé (passe-tout) au chemin direct dès que les deux profondeurs redeviennent égales (le commentaire dit « pour de bon ») : petit saut de phase possible quand on règle le bus.
  - m3 (corrigé : réverbe en fondu enchaîné de 20 ms, réglages de la cuve glissés, ligne de pré-délai à la même échelle pour la plaque et les cuves ; largeur et couche médiane du synthé glissées, retour au mono une fois le côté éteint ; suréchantillonnage pris entre deux tranches après un creux de 3 ms ; tests `restes_reverb_type_crossfade`, `restes_synth_switches`) : changement de type de la réverbe d'envoi (salle, plaque, pièce), mise en largeur ou retour au mono d'un synthé, et automatisation de « Suréchantillonnage » (réinitialise les oscillateurs) : bascule sans fondu, clic possible.
  - m4 (corrigé : le second coup attend et les pads sont rendus en morceaux, chaque coup à sa trame, frappe en direct comprise ; test `restes_two_hits_one_slice`) : deux coups d'un même pad dans un même bloc (décalage positif d'un pas suivi d'un décalage négatif du suivant, ou roulement sur plusieurs pas recouvrant un coup) : le moteur n'en garde qu'un.
  - m5 (corrigé : file de 512, compteur `leaked()` ; test `restes_layered_kit_swaps`) : file `retired_` de l'échantillonneur à 64 : changer de kit à couches (16 × 8 sons) avant un ramassage ferait fuir les sons en trop (l'appli n'envoie pas encore de couches).
  - m6 : `SetKitVersion` remet les réglages des kicks de studio du kit v3 ; un `SetPadVoiceParam` sur un pad resté « voix du kit » serait perdu (l'appli ne le fait pas).
  - m7 : la voix 808 endormie par `DrumKit` (3 s) garde son enveloppe : un coup glissé après coup repart de l'ancien niveau.
  - m8 (corrigé : `decodedTruePeakDb` et `encodeChecked` retirés, plus aucun appelant, un seul compteur de crête vraie `TruePeakMeter` ; `encode` libère ce qu'il a ouvert si l'étape suivante échoue) : `AacEncoder.decodedTruePeakDb` ne libère pas l'extracteur ni le décodeur si leur mise en place échoue avant le `try` ; la relecture ×4 en Kotlin coûte quelques secondes par encodage sur un long morceau.
  - m9 (corrigé : le tracé se termine dans un `finally`, une étape d'annulation à lui même interrompu ; test `aStrokeCutShortIsStillItsOwnUndoStep`) : `paintStep` : si le geste est annulé (écran quitté en plein tracé), `paintEnd` n'est pas appelé et le tracé suivant fusionne dans une seule étape d'annulation tout ce qui a été fait entre-temps (pas de perte : chaque case est sauvegardée).
  - m10 (corrigé : commentaires remis à jour) : `Hit.ramp` dit « 0 même force » alors que le moteur joue 0 comme avant (chaque coup 0,85 × le précédent) ; commentaire de `AutoTarget.isMasterVolume` périmé (« ne fait rien encore »).
  - m11 (corrigé : comparé aussi à `reference/synth_plain_pre_s4.f32`, rendu par le synthé du commit 88d750f, identique à l'échantillon près) : test `s4_plain_synth_is_unchanged` : compare `renderLegacy` à lui-même (les deux appels passent par le même chemin) ; il vérifie le canal droit muet, pas le son d'avant S4. Une référence enregistrée avant S4 le rendrait utile.
  - m12 (corrigé : le test compte les attaques entendues d'un clic) : le test des 8 coups par pas à 300 BPM compte les déclenchements, pas les coups entendus.
  - m13 : mode Drop en direct : « établi » n'est remis à zéro qu'au changement de projet (`resetEstimate`), pas au changement de version de mix.
  - m14 : `chooseKit("builtin:…")` vide les sons de tous les pads (annulable par « Annuler », dit dans la barre « Fait »).
  - m15 : `testKitV3CpuBudget` et les budgets CPU sont mesurés sur l'hôte : à confirmer sur le téléphone.
  - m16 : les boucles `above`/`below` de `MelodyBuilder` ne s'arrêtent que si l'ensemble des notes d'accord est atteignable dans la gamme (vrai pour toutes les harmonies livrées ; une garde éviterait un blocage sur une harmonie future mal écrite).

## Phases 35 à 39 (et 36)
Relecture neuve en trois volets (moteur, modèle, appli) de la branche p36, appliquée sur `restes` (qui contient aussi le chantier de finition). Bilan : 2 graves annoncés (1 réel), 13 moyens, une trentaine de mineurs. Chaque correction d'un grave ou d'un moyen a un test qui échoue sans elle (vérifié en retirant la correction) : tests hôtes `r3539_*` (`engine/host-tests/review3539_tests.inc`), `Review3539ModelTest`, `Review3539AppTest`, `RollMeterTest`.

### Moteur
- M1 (moyen, corrigé) : `Track808::prepare` remettait les réglages de la 808 par défaut, et `Engine::prepare` l'appelle après avoir appliqué les commandes : réglages perdus à chaque réouverture du flux (casque, Bluetooth, latence). Les défauts sont posés une fois à la construction. Test `r3539_808_settings_survive_prepare`.
- M2 (moyen, corrigé) : `ClearZones` recomptait les 512 zones à chaque zone (262 000 tours dans le fil audio). `count_` remis à 0 d'abord : 4 µs. Test `r3539_clear_zones_cheap`.
- M3 (moyen, corrigé) : une voix volée (33e note) était coupée net (clic). 8 emplacements de réserve : la voix volée s'éteint en 2 ms pendant que la nouvelle démarre ; si toutes les réserves s'éteignent encore, la plus proche de la fin est coupée. Test `r3539_stolen_voice_fades`.
- m4 (corrigé) : une zone de relâche bouclée ne s'arrêtait jamais (pas d'enveloppe à elle) : elle joue une fois. Test `r3539_looped_release_zone_ends`.
- m5 (corrigé) : les tours des round robins repartent au départ du transport (`resetRoundRobin`), même après des notes jouées à l'arrêt. Test `r3539_round_robin_transport`.
- m6 (corrigé) : file d'événements pleine (multizone 256, 808 128) : une touche relâchée prend la place du dernier appui, jamais perdue (note tenue). Test `r3539_full_queue_keeps_note_offs`.
- m7 (corrigé) : les zones lâchées par `ClearZones` (jusqu'à 100 Mo) sont libérées 0,5 s puis 11 s après (nouvelle JNI `collectGarbage`, fil principal), sans attendre la prochaine installation.
- m8 (corrigé) : pas de plafond global : 8 pistes multizones = 256 voix stéréo, 21 % d'un cœur hôte (trop pour un téléphone). Plafond global `kMultiVoices` = 128, partagé entre les pistes qui jouent un instrument (32 chacune au plus, 16 avec huit) : 9,7 % hôte. Test CHECK_TIMING `r3539_eight_multizone_tracks`.
- m9 (corrigé en partie) : `stretchPcm` lit la prise sans copie quand le système la prête, libère l'entrée planaire avant la copie entrelacée, et reçoit la vraie fréquence de la prise. Laissé : la limite de 7 minutes de sortie (une prise de 5 minutes ralentie doit tenir).
- m10 (corrigé) : `sampleCreate` / `sampleCreateCompact` attrapent un manque de mémoire (0 rendu, pas de plantage à travers JNI).
- m11 (laissé, noté) : la clé de tirage d'un coup anticipé peut différer de celle du coup joué à l'heure (rare, sans conséquence audible).
- m12 (corrigé) : les valeurs reçues sont bornées avant `lround` (1e30 donnait la touche 0 sur x86). Test `r3539_huge_zone_values`.
- Non vérifiable ici : les changements JNI (m7, m9, m10) ne sont compilés que par `assembleRelease`, sans test hôte.

### Modèle
- M1 (moyen, corrigé) : supprimer une piste gelée laissait son audio gelé (`frozenFrom`) ; une nouvelle piste au même numéro passait pour gelée. `removeTrack` retire l'audio gelé ; au chargement, un audio gelé sans sa piste redevient une piste audio ordinaire ; `Freeze.audioOf` exige la piste.
- M2 (moyen, corrigé) : `Freeze.signature` hachait des énumérations (mode d'arpège) dont le `hashCode` change à chaque lancement : toute piste gelée paraissait « pas à jour ». Hachage du texte des valeurs (stable ; test avec une valeur attendue fixe). Les pistes gelées avant paraîtront « pas à jour » une fois.
- M3 (moyen, corrigé) : `ChordTrack.reharmonized` écrite sur toute la scène gardait les notes cachées, qui revenaient dans l'ancienne tonalité par-dessus les copies. Seules restent cachées celles au-delà de la nouvelle fin, et elles suivent les accords.
- M4 (moyen, corrigé) : la limite de 512 notes coupait en silence. La réécriture sur la scène n'a lieu que si tout tient ; coller, dupliquer et « Garder ce que je viens de jouer » ne prennent que la place libre (jamais une note déjà là ; le collage compte ce qui n'a pas tenu, la capture garde les premières).
- M5 (moyen, corrigé) : la 9e prise efface la plus ancienne, dont le fichier disparaissait au lancement suivant. Message « Huit prises gardées : la plus ancienne (« … ») est oubliée. Annuler la ramène. » (`takeForgets`) ; et les sons que le projet n'utilise plus attendent une semaine dans `samples-retired/` (à côté de `samples/`, jamais dans un .zip) avant d'être effacés, et reviennent d'eux-mêmes si le projet les nomme de nouveau (Annuler après relancement, version ramenée).
- M6 (moyen, corrigé) : ramener une version remettait toute l'automation (courbes de boucle dessinées depuis comprises, pistes supprimées depuis). Seules les courbes du morceau reviennent ; une courbe d'une piste supprimée reste partie ; les notes des pistes supprimées étaient déjà filtrées au nettoyage.
- m7 (corrigé) : version bornée après `take(Scene.MAX)` ; ses scènes et ses parties nettoyées comme celles du projet.
- m8 (corrigé) : deux versions de suite identiques ne sont gardées qu'une fois.
- m9 (corrigé) : une prise Live sur un morceau encore vide garde quand même les scènes d'avant (une version sans morceau) ; `keepingVersion` part de la liste du projet écrit.
- m10 (corrigé) : `PlayedMemory.capture` avec 0 mesure ne plante plus ; une capture plus longue que la plus longue boucle garde ses dernières mesures au lieu de les replier les unes sur les autres.
- m11 (corrigé) : « Comme joué » après un changement de longueur de boucle ou une réécriture par les accords : la place jouée suit (`Note.movedTo`, `NoteClip.withSteps`), y compris pour une note jouée juste avant le retour de la boucle.
- m12 (corrigé) : une prise ou un choix de prise sur l'audio gelé d'une piste la dégèle (`frozenFrom` remis à zéro).
- m13 (corrigé) : une scène qui n'a que des accords n'est plus « vide ».
- m14 (laissé, voulu) : `routesPad` compte le solo.
- m15 (corrigé en partie) : en Mode simple, `frenchOf` ramène le mot simple à celui de l'appli (« Mélodie » → « Lead ») : l'aide « ? » et la bibliothèque retrouvent leur mot. Faux pour les titres de groupes du glossaire : `GROUPS` est déjà recalculé à chaque lecture (`get()`), il suit la langue.
- m16 (tranché, corrigé) : une scène vidée et la mesure d'un fill remettent la boucle propre d'un pad (`DrumLane.length`) à 0 : une scène vide repart carrée, un fill joue sa mesure droite.

### Appli
- G1 (annoncé grave, en partie faux) : une version ramenée joue avec les sons de pads du projet (les sons sont partagés par toutes les scènes, `showing`) : aucun pad ne devient muet. Les noms de sons gardés dans les scènes d'une version sont des copies sans effet ; leur fichier pouvait pourtant manquer et un .zip fait après « Ramener » annonçait des sons manquants. Corrigé quand même à peu de frais : ces fichiers sont gardés sur le disque et dans le .zip (`versionSampleFiles`), et « Ramener » recharge ce qui manque. Test `aVersionsPadSoundStaysOnDisk`.
- G2 (grave, corrigé) : les accords essayés étaient sauvegardés sans « Appliquer » (sauvegarde automatique, arrière-plan, fermeture) : l'appli tuée, l'essai devenait définitif. Pendant un essai, la session écrit la chanson d'avant tant que le projet est exactement l'essai (`ProjectSession.saveAs`) ; « Appliquer » écrit l'essai, « Annuler » la chanson d'avant ; une autre modification faite entre-temps est sauvegardée telle quelle, essai compris. Test `theChordsTriedReachTheDiskOnlyWithAppliquer`.
- M1 (moyen, corrigé) : un instrument multizone lu pendant la fermeture de l'écran n'était jamais libéré (~200 Mo natifs : `runCatching` avalait l'annulation). Lu jusqu'au bout puis libéré (`finishOrRelease`). Test `anInstrumentReadWhileTheScreenClosesIsFreed`.
- M2 (moyen, corrigé) : le budget de 200 Mo ignorait les instruments en cours de lecture : deux ouverts ensemble passaient. Leur taille est réservée dès le début. Test `twoInstrumentsOpenedTogetherStillKeepToTheBudget`.
- M3 (moyen, corrigé) : copier, coller, choisir une mesure et la règle du piano roll comptaient des mesures de 16 doubles-croches, même en 3/4 ou 7/4 : ils suivent la mesure du projet. Laissé : la longueur de boucle reste en multiples de 16 doubles-croches (format des notes), affichée « mes. » en 4/4. Test `RollMeterTest`.
- M4 (moyen, corrigé) : vidéo et export pouvaient tourner ensemble, et la vidéo n'avait pas de service de premier plan. La vidéo bloque les exports (`videoBusy`, message « Une vidéo est en cours ») et passe par `ExportRun` (notification, « Annuler », écran éteint). Test `theVideoKeepsExportsWaitingRunsInTheForegroundAndLeavesNothingWhenCancelled`.
- Mineurs corrigés : la feuille « Partager » de la vidéo ne s'ouvre plus de nouveau à la rotation (état dans le ViewModel) ; `VideoEncoder` passe la taille du tampon quand l'image a des lignes plus larges que la vidéo (à vérifier sur le téléphone : non testable ici) ; `renderMix` décrit le projet dans le `try` (moteur privé toujours fermé) ; la feuille « Automatiser » garde la commande, pas un indice (un Annuler ne montre jamais une autre courbe) ; `SongChords` ignore une liste d'accords vide ; « Refaire » rend les notes et les forces de coups que la prise avait remplacées (test `refaireGivesBackTheNotesTheTakeReplaced`) ; `useMultiInstrument`, `use808` et `editTrack` sur une piste supprimée ne touchent plus la première (test `aSheetLeftOpenOnADeletedTrackNeverChangesTheFirstOne`) ; `PadBounce` compte au moins un temps par passage.
- Mineur laissé, documenté : un réglage changé entre deux essais d'accords (potard MIDI) repart avec l'essai suivant (la feuille repart de la chanson d'avant : c'est un moment d'écoute).
- Tests ajoutés : `LearnViewModel` (arrêt et fermeture pendant un rendu : moteur privé fermé), annulation de la vidéo (rien ne reste, service arrêté), budget et fuite des instruments, essais d'accords non sauvegardés, sons cités par une version gardés. Laissé : `placePunch` dans le ViewModel (il faut le chemin micro de `TakeVault`, absent sous Robolectric ; le collage lui-même est testé dans `TakeAudioTest`). À noter : `SongArrangementTest` dépend d'un délai de 600 ms (double toucher).

## Signaler un bug (relecture neuve)
1 grave et 5 moyens corrigés avec tests, mineurs listés.
- G1 (grave, corrigé) : après un plantage, le bandeau remplaçait la fenêtre « Aïe » mais l'accueil, « Que veux-tu faire ? » et le guide attendaient encore `pendingCrash == null` : cachés tant que le bandeau était là, puis « Que veux-tu faire ? » surgissait en plein travail (sous la fiche après « Envoyer »). La proposition de mise à jour était aussi cachée. Le bandeau ne bloque plus rien. Test `aCrashBannerHoldsNothingBackAndNothingPopsUpLater`.
- M1 (moyen, corrigé) : la copie du journal toutes les 10 s était fabriquée sur le fil de l'interface (~300 Ko, un `SimpleDateFormat` par ligne). Fabriquée et écrite sur le fil d'entrée-sortie, un seul `DateTimeFormatter`.
- M2 (moyen, corrigé) : pendant une lecture sans geste, la copie n'était pas réécrite (seuls les événements la faisaient bouger) : un plantage natif ne trouvait que du son vieux de plusieurs minutes. Les mesures du son avec du son qui sort la font aussi bouger (`LiveCopyClock`, `HealthMonitor.version`). Test `theLiveCopyFollowsTheSoundEvenWithoutAGesture`.
- M3 (moyen, corrigé) : « Ce qui sera envoyé » taisait que le nom, le style, le tempo et la position du projet sont toujours dans report.md (coché ou non), et comptait 50 événements alors que journal.json en garde jusqu'à 300. Ligne « Le nom et les réglages du projet ouvert » ajoutée (FR/EN/NL), compte réel. Test `theProjectFileGoesOnlyWhenTickedAndThePicturesAreCleanedAfterSharing`.
- M4 (moyen, corrigé) : les messages d'erreur portaient des chemins complets (noms de morceaux et de dossiers) et `Journal.error` ne nettoyait ni ne coupait. `LogTail.redact` remplace /storage, /sdcard, /mnt, /data (sauf /data/app : le code de l'appli, utile pour un plantage natif) par `<chemin>` et content:// par `<document>` ; `Journal.error` nettoie et coupe à 600 caractères ; la connexion MIDI ne note que le genre d'appareil (USB, Bluetooth, virtuel), plus son nom. Tests `pathsDocumentsAndAddressesNeverLeaveThePhone`, `theCrashHandlerWritesTheJournalThenCallsThePreviousOne`.
- M5 (moyen, corrigé) : tests ajoutés (`BugReportViewModelTest`) : 7 images choisies → 5 gardées et un message ; project.json absent quand « Joindre le projet » est décoché ; captures effacées du cache après « Partager » ; report.md en français avec l'appli en néerlandais ; le gestionnaire de plantage écrit le journal puis passe la main au précédent.
- Mineurs corrigés au passage : le cache `bugreport/` est vidé de ses vieilles images au démarrage de la fiche ; une miniature impossible à faire montre une icône (plus de rond qui tourne sans fin) ; choisir plus d'images que de places restantes le dit (« 5 images au plus : les autres ne sont pas ajoutées. ») ; `Journal.version` atomique ; `vm.done` ne rejoue plus sa dernière valeur dans le journal (`drop(1)`) ; trouvés en écrivant les tests : `WavExporter.share` renvoie « impossible » au lieu de planter quand un fichier est hors des dossiers partagés, et un fournisseur d'images qui refuse de dire son type n'empêche plus l'ajout.
- Mineurs laissés : les bitmaps des captures ne sont pas recyclés (le ramasse-miettes s'en charge) ; une copie d'image ratée laisse un fichier partiel dans le cache (purgé au démarrage suivant) ; le sélecteur s'ouvre avec 5 places même s'il en reste 2 (le surplus est dit, plus ignoré en silence) ; le journal est en partie dans la langue de l'appli (seul report.md est garanti en français) ; le zip reste dans `cache/exports` jusqu'au prochain export ; code mort `MainViewModel.crashReportUri()` et anciennes phrases de la fenêtre « Aïe » dans song.json ; `bugProject` relit `rhythmVm.project` ; `JournalWatch` ne note pas « Que veux-tu faire ? » ni « Ton morceau est prêt » ; l'aperçu ne montre que report.md (pas journal.json) ; le journal d'un plantage natif est remplacé si l'appli est relancée deux fois sans l'envoyer.
## Lot 1 de bugs (APK 181) : relecture neuve

0 grave, 2 moyens corrigés avec un test chacun, mineurs corrigés et restants.

- M1 (moyen, corrigé) : la vérification du M4A (décodage pour lire la crête) relançait l'erreur d'un décodeur bloqué, ce qui faisait échouer tout l'export alors que le fichier était déjà encodé et bon (les autres échecs de lecture gardaient le fichier). Un blocage pendant la vérification est désormais noté dans le journal et le fichier est gardé (`M4aGuard.checkOrKeep`) ; l'erreur ne reste que pour l'encodage. Test : `aStuckVerificationKeepsTheFileEncoded`.
- M2 (moyen, corrigé) : la montre de rendu figé de la vidéo 9:16 démarrait avant l'attente des sons du projet (jusqu'à 15 s et plus, à 0 %) : après 60 s le moteur était annulé avant même de rendre (« fabrication bloquée » à tort). La montre est armée quand le rendu commence vraiment (`StallWatch(armed = false)`, `renderForVideo(onRenderStart)`). Test : `aWatchNotYetArmedNeverFlagsTheWaitBeforeTheRender`.
- Mineurs corrigés : (3) `AacEncoder` : un encodeur fini sans avoir donné son format (rien d'écrit) lève « fichier M4A incomplet » au lieu d'un succès vide ; (5) la barre des étapes M4A est mise à jour par `update { }` depuis le fil d'encodage (plus d'écriture perdue contre le fil principal) ; (7) un budget de temps par rendu (chaque piste séparée a le sien), compté une fois les sons chargés ; (9) le banc des écoutes ne compte pas en erreur un son tenu sous le niveau par sa propre crête (≥ −1,05 dBFS : le lead 303 à −17,4) ; (10) le niveau d'écoute est calculé une fois par son chargé (cache par identité, `levelled`) ; les sons longs passent par Dispatchers.Default, les courts (moins de 2 s, quelques ms) restent sur place car une voix de kit en cache doit jouer sans spinner (`WizardAcidStepTest.aSoundAlreadyHeardIsNotLeftPreparing`) ; (11) `PreviewLevel.gainDb` rend 0 si le gain n'est pas fini.
- Mineurs restants (notés) : (4) `AudioDecoder.scan` passe chaque échantillon par une `Function2` (boxing) : à remplacer par un puits typé si le profil le montre ; (6) une annulation après `copyToMusic` laisse la copie dans Musique ; (8) `CodecPump.END_MS` : 10 s après la dernière entrée sans fin de flux, à confirmer sur de vrais codecs lents ; (12) l'écart entre le niveau d'écoute (−16 LUFS) et le niveau du son une fois posé sur une piste peut surprendre ; (13) les missions passent sur le projet vierge en mémoire quand il n'y a plus de projet, et « Que veux-tu faire ? » n'offre pas « Continuer » après un import tant qu'aucun projet n'est ouvert.
## Tests d'usage réel (relecture neuve, 2026-10-24)

0 grave, 6 moyens corrigés, mineurs corrigés sauf ceux listés en fin.

- M1 (moyen, corrigé) : la demande d'autorisation des notifications (Android 13+, première prise) restait par-dessus l'appli : `POST_NOTIFICATIONS` donnée d'avance dans `JourneyTest` et `EnduranceTest` (`RealUse.PERMISSIONS`).
- M2 (moyen, corrigé) : le retour d'arrière-plan ne s'appuie plus sur `moveToState(RESUMED)` : l'appli est relancée comme par le lanceur (`singleTask`, même écran) et le test attend l'état RESUMED.
- M3 (moyen, corrigé) : le seuil de sous-alimentations n'est plus qu'écrit dans le journal sur l'émulateur de la CI (« ranchu ») ; il reste bloquant sur un téléphone.
- M4 (moyen, corrigé) : plus d'attentes fixes : les sons attendus (`awaitSoundsLoadedForTest`), la rotation attendue (orientation de l'écran, puis la barre du bas), la bascule Ligne du temps / Parties attendue (la ligne du temps disparue).
- M5 (moyen, corrigé) : `scripts/emulator-tests.sh` grossit le tampon du journal, écrit les lignes des tests au fil de l'eau et limite chaque test (`timeout_msec`) ; le job « Build » a un `timeout-minutes`.
- M6 (moyen, corrigé) : le dépôt est public : le .zip complet du projet de Benjamin (clip « Voix ») n'est publié nulle part ; `complets.txt` le garde en commentaire, à activer seulement avec son accord et sur un hébergement privé.
- Mineurs corrigés : `freshInstall` attend les sauvegardes en cours et vide aussi corbeille, morceaux séparés et exports ; le fichier exporté est le plus récent ; le Mode simple exige la carte « Ton morceau est prêt ! » ; le fil principal est surveillé toutes les 500 ms pendant chaque tour ; `RealProjectsTest` exige au moins un projet dans le dépôt et ne tourne plus pendant l'enregistrement des captures ; `real-projects.sh` sûr sous `pipefail`, `--dir` et `--fetch` ensemble ; le soak exige la ligne ALLOC ; `ps_live` compte par fil (`thread_local`) et au niveau de `malloc`/`calloc`/`realloc`/allocations alignées/`free` (donc tous les `new`) ; `nightly.yml` : commentaire du soak corrigé, `soak_minutes` vérifié (entier, ≥ 20) ; durée d'endurance par défaut à 2 min comme la PR ; `emulator-tests.sh` compatible avec un vieux bash.
- Laissé : la ligne « regarder la nuit au début de chaque session » dans CLAUDE.md n'est pas ajoutée par la session des tests (CLAUDE.md ne change que sur demande de Benjamin ou de la session principale) : proposée dans TESTS-REELS.md. `valloc`/`pvalloc` ne sont pas comptés par `ps_live` (le moteur ne les emploie pas). Les tests sur émulateur n'ont tourné qu'en CI (pas d'émulateur en local).


- 2026-10-25 · Tests d'usage réel · Prise au clavier en mode complet sur l'émulateur de la CI : les touches du clavier de Sons n'arrivent pas à la prise (0 note gardée, avis « Rien n'a été enregistré »), alors que la prise du Mode simple garde ses 4 notes dans le même passage et que le même chemin garde ses notes en Robolectric (3 tailles d'écran, horloge du moteur simulée, morceau en lecture, partie en boucle, arrêt). L'étape est journalisée, pas vérifiée, dans `JourneyTest.aFirstSongWithEveryTool` jusqu'à l'arrivée du rapport de prise du lot 2 (« Prise keys : N touches, N notes ») ; à réactiver alors. Benjamin le vérifie sur son téléphone (recette).
- 2026-10-25 · Tests d'usage réel · Sur l'émulateur de la CI, dans le parcours en mode complet, les touchers après l'arrivée sur Sons n'agissent plus (clavier, puces et bouton d'export du Mix : l'état ne change pas), alors qu'ils agissent avant (Structure : Fill, Montée) et dans le parcours du Mode simple. Les nœuds n'y ont aucune position lisible (bornes 0,0,0,0). Le parcours le dit dans le journal (« tap on … did not land ») et fait alors l'export par le ViewModel pour vérifier quand même l'export sur l'appareil ; le partage de ce parcours est journalisé. À reprendre (tâche « Réactiver la vérification de la prise… ») avec le rapport de prise du lot 2.
- 2026-10-25 · Tests d'usage réel · Lot 2 : le parcours journalise maintenant le rapport de prise (« Prise keys : N touches, N notes, N coups »). « 0 touches » = les touchers n'arrivent pas à l'appli (émulateur) ; « N touches, 0 notes » = l'appli les reçoit et les écarte (vrai bug). La vérification revient dès qu'une exécution de la CI a tranché.
- 2026-10-25 · Tests d'usage réel · Tranché par la CI du lot 2 (#50) : en mode complet, « Prise keys : 1 touches, 1 notes » pour 4 touches envoyées. Les touches qui arrivent sont gardées ; ce sont les touchers injectés par le test qui n'arrivent pas à l'appli sur l'émulateur (pas un bug de prise). Le test de fumée a eu la même chose sur « track_0 » : il choisit alors la piste par le ViewModel et le journalise. La prise en mode complet reste journalisée, pas vérifiée, sur l'émulateur ; elle est vérifiée en Robolectric (`SonsKeyboardTakeTest`) et par Benjamin sur son téléphone.
- 2026-10-25 · Tests d'usage réel · Piste trouvée : depuis la puce « Remix » (lot 2), la puce « Synthé » de Sons dépasse le bord droit d'un écran étroit ; un élément à moitié visible compte comme « affiché » et le toucher, envoyé en son centre, tombe hors de l'écran. Les tests font maintenant toujours défiler jusqu'à l'élément avant de le toucher (puces, touches du clavier). Si la prise en mode complet garde ses 4 notes sur l'émulateur, sa vérification revient.
## G1a — cerveau du Guide (2026-10-24)

Revue neuve de `GuideState`, des parcours, de `GuideAnalyzer`, `GuideActions` et `GuideViewModel` : 2 graves et 10 moyens corrigés avec tests, mineurs corrigés sauf ceux listés en fin.

- G1 (grave, corrigé) : la signature du master utilisait le `hashCode` d'une énumération (différent à chaque lancement) : « faite ailleurs » se cochait tout seul après un redémarrage. Ordinal à la place. Test `theSignaturesAreTheSameInEveryRun` (valeur fixe).
- G2 (grave, corrigé) : l'historique gardait la tranche entière et « Ramener » / « Annuler mon choix » la réécrivait, effaçant ce qui avait été fait après. Désormais un réglage par ligne (emplacement d'effet, envoi, pan, volume, solo, coupures des seules parties touchées, cibles des courbes, paramètres des bus), regroupés par option appliquée (`GuideChange.id`), et ramenés seulement si chacun vaut encore ce que le Guide y a mis ; sinon refus et message (`GuideOutcome.problem`, `GuideViewModel.notices`). Tests `bringingAChoiceBackNeverErasesWhatCameAfter`, `bringingBackAfterALaterEditOfTheSameStrip`, `aRefusedUndoIsSaid`.
- M1 (corrigé) : une alerte ne garde plus « faite » ni « sautée » : elle revient avec son fait ; seul « C'est voulu » (refus) l'écarte. Test `anAlertComesBackAndCentresEveryTrackItNames`.
- M2 (corrigé) : « Remettre au centre » vise toutes les pistes du fait, pas le seul rôle de la première. Même test.
- M3 (corrigé) : les modifications du Guide lui-même mettent à jour les signatures vues (sauf celles que la carte surveille) : « Coupe-bas partout » ne coche plus fx.pad, fx.lead ni move.throw. Test `theGuidesOwnEditsDoNotTickOtherCards`.
- M4 (corrigé) : le décalage des courbes dû au coupe-bas inséré en tête est dans l'historique et revient avec lui (d'un bloc). Test `bringingAChoiceBackNeverErasesWhatCameAfter`.
- M5 (corrigé) : une modification du Guide suit le même chemin qu'une annulation (`afterGuideEdit` : partie en cours et suivante renvoyées, effets longs au-delà de la limite signalés) ; une copie de chaîne qui dépasserait les effets longs du moteur est refusée ; les courbes d'effets devenues fausses après une copie partent comme au mixeur. Test `aCopyPastTheLongEffectsLimitIsRefused`.
- M6 (corrigé) : « À l'oreille, plus tard » met la carte à plus tard au lieu de la cocher. Test dans `GuideParcoursTest`.
- M7 (corrigé) : l'historique ne garde que les réglages touchés, et reste sous ~48 000 caractères (groupes entiers).
- M8 (corrigé) : les actions se calculent hors du fil principal ; le moteur n'est resynchronisé que si autre chose que l'état du Guide a changé (Sauter, Plus tard, une note : rien). Test `theAnalysisFollowsTheProjectAndTheCardsAreOneUndoStepEach`.
- M9 (corrigé) : fx.sidechain propose « Garder » et se coche quand le morceau a déjà son sidechain.
- M10 (corrigé) : tests ajoutés pour un projet vide, un morceau en 3/4, un style inconnu, « Ramener » après une autre modification de la même piste.
- Mineurs corrigés : seuil du filtre passe-haut à 2/3 ; durée d'un projet sans morceau = sa scène ; le style « alerts » n'est jamais un parcours ; une option qui ne change rien ne coche pas la carte ; une annulation refusée ne remet pas la carte à faire ; « Ramener » prend l'identifiant du groupe, plus un index ; une carte dont toutes les options sont refusées est sautée ; pistes caisse claire / clap tirées de `StarterWizard.Role` ; `scenesOf` retiré ; fourchette 12–20 cartes à décider dans le test ; textes (trap « Alléger le pont », jump up « la 2e basse attend », pas de « Plus d'écho » sur une 2e basse).
- Mineurs laissés : fx.vocal et fx.sub se cochent dès que la piste a un effet, même un simple coupe-bas posé par « Coupe-bas partout » (détecteur `fx:` grossier, à affiner en G4) ; une alerte « plus tard » reste mémorisée même quand son fait disparaît (sans effet visible) ; le pourcentage d'un morceau de l'assistant intact est ~45 % (40 à 50 %), pas exactement 40 %.
## G1b — couches (relecture neuve)
1 grave et 4 moyens corrigés avec tests, mineurs corrigés pour la plupart, les restants notés.
- G1 (grave, corrigé) : un instrument multizone fini de lire n'arrivait qu'aux pistes, pas aux couches : une couche jouait son synthé de secours en direct pendant que l'export jouait le vrai instrument. `loadMultiBanks` l'envoie aussi aux couches qui le jouent. Test `aLayersMultizoneInstrumentReachesItOnceRead`.
- M1 (moyen, corrigé) : « après les effets de la piste » veut dire après ses inserts seulement : réglages rapides, coupe, kills, fader, pan et envois de la piste agissent encore sur la somme. C'est écrit (Mixer.h, Layers.kt), et `subLayerProblem` refuse de replier un Sub quand la basse a des envois, un pan, des réglages rapides bougés, ou quand le Sub a les siens. Test hôte `g1b_sub_layer_sounds_as_the_sub_track` : basse + piste Sub contre basse + couche Sub, rendus hors ligne, écart 5e-7 ; refus testés dans `LayersTest`.
- M2 (moyen, corrigé) : une couche retirée gardait son sample et les zones de son instrument dans la mémoire du moteur (jusqu'à 100 Mo), d'un projet à l'autre. Ils sont lâchés. Test `aLayerGoneLetsGoOfItsSoundsAndItsListening`.
- M3 (moyen, corrigé) : le synthé d'une couche jouait 4 notes au plus : un accord de 5 ou 6 notes perdait des notes. Monté à 6 (`kLayerVoices`), exposé (`NativeEngine.LAYER_VOICES`, `Layers.MAX_VOICES`) pour que G3 prévienne au-delà.
- M4 (moyen, corrigé) : le test de budget CPU avait des bornes absolues. Il mesure maintenant en alternance (A B A B, le meilleur de 3 passes) et compare des rapports : synthé lourd ×4,4 à 4,9, Plaits ×1,8 à 2,0, 808 ×1,25 à 1,3, mélange ×1,3 à 1,35 sur l'hôte chargé (bornes 5,8 / 2,4 / 1,4 / 1,6, plus larges que celles proposées, que les mesures dépassaient de peu sous charge).
- Mineurs corrigés : l'écoute d'une couche seule est oubliée quand la pile change (une annulation la déplaçait sur une autre couche) ; `Layers.FIRST_CHANNEL` vient de `Mixer.CHANNELS` ; commentaire de mémoire du mixeur (60 × 8 cases, 12 à 18 Mo) ; `renderLayerAB` attend les sons comme un export ; le nom et le rôle d'une couche ne périment plus une piste gelée ; le sample d'une couche se cale sur l'octave des notes qu'elle joue (`withFittedSampleRoots`) ; le test export / direct prépare le direct comme l'export (100 ms, `settle`) et vérifie maintenant un écart de 1e-6 (mesuré 0) ; une transposition qui sort du clavier laisse tomber la note au lieu de mettre deux touches sur la même note de couche (test `g1b_transposition_past_the_ends`).
- Restants, documentés : les courbes d'automation visent le son propre de la piste (son synthé, son Plaits), pas ses couches ; la clé du sidechain ne compte pas les couches ; une couche coupée puis rallumée pendant sa fin de note reprend sans relancer les notes tenues ; le test sans clic couvre mute, écoute et coupure, mais pas le fondu de 20 ms au bout des 4 s de fin de note ; une piste gelée : ses couches se taisent avec ses notes (les parties du morceau les coupent), mais leurs sons tournent à vide comme celui de la piste.
## G0 — corrections du diagnostic (2026-10-24)

Relecture neuve de la branche `g0` : 0 grave, 3 moyens corrigés avec tests, mineurs corrigés ou laissés (documentés). Le relecteur a confirmé que le morceau de Benjamin fait 216 mesures et que ses courbes n'ont jamais été décalées.

- M1 (moyen, corrigé) : les longs morceaux perdaient des fills « à la 8 » (réserve de scènes pleine : Benjamin perdait les mesures 135, 159, 175 ; la scène propre au dernier Drop en prenait encore). Quand les scènes manquent, chaque sorte de crash et de fill dans chaque sorte de section a d'abord sa scène, puis les autres partagent : le même fill écrit (sans les millisecondes du batteur), sinon celui du même temps de la même scène, sinon celui de la même section. Rien ne change quand les scènes suffisent. Test `DropFillsTest` (37 styles : chaque crash et chaque fill attendu a sa scène ; le morceau de Benjamin montre les fills perdus).
- M2 (moyen, corrigé) : une piste qui ne joue rien recevait quand même son coupe-bas et son envoi (la piste 0 d'un morceau sans mélodie : une basse jouée là plus tard aurait été amincie). Elle garde sa tranche. Test `aTrackThatPlaysNothingKeepsItsStrip`.
- M3 (moyen, corrigé) : le pan d'un pad vaut pour tout le morceau (`drums.lanes[i].pan` ; les copies par scène ne jouent pas) : le kick de Benjamin est à fond à gauche partout, posé sans geste voulu par le geste « retour » d'Android qui tombe sur le curseur de pan (enquête dans docs/AUDIT-35.md ; seuls les deux curseurs de pan des pads l'écrivent). `withPadPan` s'aimante au centre sous ±4 %, comme la console. Test `aPadsPanSnapsToTheCentreForTheWholeSong`. À lire pour le Guide (G1a) : `drums.lanes[i].pan`, pas les scènes.
- Mineurs corrigés : une seule scène « Drop N » pour le dernier Drop (plus deux scènes presque identiques) ; le Drop 1A ne coupe plus un premier Drop de 8 mesures entier ; le coupe-bas de la 303 suit ses notes ; `lowestPitch` documente qu'il ignore SUB et OSC2_OCTAVE ; la couche de batterie suit la mesure du projet ; le test des courbes refuse une courbe vidée ; la variation entre Drops est testée sur les 4 ambiances (minimum 0,31, pop Future) ; acidcore passe de 0,25 à 0,47 au banc (0,5 dans le modèle ; un Drop du milieu de 32 mesures finit sur la scène du dernier sur ses 16 dernières mesures) ; 184 → 216 mesures dans `DropVariation` ; médiane au banc dans DECISIONS ; la CI compare à `apres-G0.json`.
- Mineurs laissés, documentés : la couche ajoutée au dernier Drop joue le son intégré du pad (charley ouvert, percussion), pas celui d'un pack ; `whatActs` (« Ce qui joue ») ignore les pistes muettes d'une partie (Drop 1A) ; le guide du morceau (`SongGuide`) ne parle ni du Drop 1A ni de la scène du dernier Drop ; la métrique ignore les vélocités, les courbes autres que le volume et les effets (deux Drops qui ne diffèrent que par un filtre comptent pareils), et compare mesure contre mesure (un Drop décalé d'une mesure compte tout différent).

## Lot 2 (audit 35) : relecture neuve

Bilan : 0 grave, 3 moyens corrigés avec tests, mineurs corrigés sauf ceux listés en « restants ».

Moyens (corrigés) :
- Tablette couchée à 130 % : la ligne « Kit ▾ » coupée sous les pads (une seule colonne à 130 %, l'ajustement ne s'appliquait qu'à deux colonnes). Les pads s'ajustent sur toute tablette couchée, en une ou deux colonnes, 48 dp au moins sur la vitre. Test : `TabletLyingTest.theFourRowsOfPadsFitLyingDownAt130`.
- Étiquettes « ? » couchées : « Masquer » et « Signaler un bug » au milieu de l'écran (le rail était pris pour la barre du bas). Seule une rangée d'au moins trois espaces compte comme barre ; sinon les boutons vont en bas, là où ils couvrent le moins de commandes ; les commandes sans étiquette (le « ? ») sont évitées aussi. Test : `HelpLabelsTest.lyingDownTheButtonsGoToTheBottom`.
- Curseur de pan et TalkBack : il disait « 50 % » ; il dit son nom (« Pan du pad », « Panoramique ») et son côté (« G 30 »), se règle au clavier (← →), se dit désactivé. Test : `PanSliderTest.talkBackAndAKeyboardKnowIt`, `disabledItSaysSoAndTakesNothing`.

Mineurs (corrigés) :
- Geste repris par le système après un glissé : plus d'étape d'annulation vide (`History.push` retire l'étape revenue à son départ). Test : `HistoryNoopTest`.
- Début d'un geste de pan oublié à l'annulation et au rétablissement.
- Test de recherche des fenêtres : dossiers vérifiés, au moins 150 fichiers lus, imports renommés (« as ») repérés, Windows.kt exclu par son chemin.
- Remix : « ← Mon morceau » ramène à Mon morceau, la carte de fin des Missions propose le Remix (tests).
- Mode simple › Clavier : « Mélodie » choisie une fois par projet (gardée à la rotation), une autre piste choisie ensuite reste (test).
- Étiquettes du Live : un identifiant par commande, une seule étiquette pour M et ■ (ceux de la batterie).
- « Partager » ne se pose plus à moitié sur le « ? ».
- Capture d'une prise sur téléphone couché et sur la Tab A11 (`nav/land_sons_synth_take`, `tabA11/*sons_synth_take`) ; la rangée du clavier couché prend la hauteur des touches, la carte de la prise défile à côté (elle poussait les touches hors d'un téléphone couché) ; la bande vide de 60 à 80 dp au-dessus de « Clavier / Notes » couché venait de « Clavier | Notes » qui prenait toute la ligne et écrasait l'octave : il prend maintenant ce qui reste.
- « Kit ▾ › Trap (808) » sur un morceau qui a déjà une piste Basse 808 : une ligne le dit (test).

Restants (notés) :
- Les étiquettes « ? » de la barre du haut sur téléphone : faute de place, certaines (« Enregistrer », « ⋯ ») ne s'affichent pas plutôt que de se poser sur un autre bouton.
- En néerlandais et en anglais, la puce des pads d'accords du synthé dit « Pads » comme le Live (le français aussi).
- Sur un téléphone couché, la carte de la prise défile à côté des touches (110 dp de haut) : « Garder » demande un petit défilement.
- `LiveVmTest.aScreenClosedDuringATakeKeepsItsJournal` (et une fois `RenderWaitsForInstrumentsTest`) ont échoué dans la suite complète sur la machine partagée et passent seuls : tests à délais, à fiabiliser.
## Basses qui tapent : relecture neuve

Relecture neuve de la branche `basses` (rebasée sur `guide`) : 0 grave, 5 moyens corrigés avec tests, mineurs corrigés pour la plupart, les restants notés.

- M1 (moyen, corrigé) : « Power chord » sur une basse qui glisse (tech house, speed garage, dnb…) glissait en 80 ms de la fondamentale à l'octave au lieu de sonner trois notes. Une grille de « Motifs de basse » coupe le glissé du synthé (basse et Sub), et le power chord n'est plus proposé aux styles dont la basse glisse (`BassPatterns.offered`). Tests `BassesTest.theRollingBasslinesNeverTouchNorCoverTheKick` (tech house + power : GLIDE 0, trois notes ensemble) et `WizardBassGridTest`.
- M2 (moyen, corrigé) : deux propriétaires pour le bas du spectre (RoleMix et l'exciteur). Maintenant RoleMix seul pose les coupe-bas des pistes (adaptatifs : 30 Hz ou 0,7 × la note la plus grave), le Sub n'en a aucun, le sub drop a sa saturation ; l'exciteur passe en dernier et ne fait que se mettre devant le clipper et ramener l'EQ coupe-bas devant lui. Le pad de l'808 (que RoleMix ne traite pas) a son coupe-bas à min(30 Hz, 0,7 × sa note la plus grave). Tests `BassesTest`, `RoleMixTest`, `StarterWizardTest`.
- M3 (moyen, corrigé) : `SubLayers.bassOf` cherchait la basse −12, alors que la piste Sub joue la note la plus grave de chaque attaque, jamais sous Do1 : le Sub d'un power chord ou du grime n'était plus reconnu. Les deux comparent maintenant avec la même transformation (`StarterWizard.subNotes`) ; le repli en couche (une transposition, −12) ne se fait que si le Sub est exactement la basse une octave plus bas, sinon les deux pistes restent. 7 styles sur 10 replient (dnb et tech house compris). Test `LayersTest.aSubUnderAPowerChordOrNotesFoldedUpStaysATrack`.
- M4 (moyen, corrigé) : le test hôte « sous 20 Hz » de l'export mesurait total − copie passe-haut, qui devenait négatif (−345 dB) : la vérification passait toujours. Il mesure maintenant la bande 5–19 Hz (Goertzel sur 4 s) : version 2 −37,6 dB, version 3 −50,9 dB. `MIX_V3` (passe-haut du master) reste inutilisé par l'assistant (il coûtait 0,6 à 1,3 LU au Drop) : pas de comparaison direct / export sous 30 Hz tant qu'aucun morceau ne l'a.
- M5 (moyen, corrigé) : « Motifs de basse » : le sous-titre brut (`.rRr.rRr…`) remplacé par quelques mots simples (FR/EN/NL : « Entre les kicks », « Ça chaloupe »…), les grilles conseillées nommées sous la grille ; capture `wizard_bass_grids` (téléphone, 130 % FR et NL) ; test ViewModel `WizardBassGridTest` ; un instrument multizone fait entendre la grille sur son son de secours (son aperçu propre n'est qu'un échantillon) ; un son de pack la jouait déjà.
- Mineurs corrigés : « Fréquence » et « Haut » de l'exciteur glissent (20 ms) au lieu de recalculer les filtres d'un coup (test : pas de marche plus grande que réglages fixes) ; un NaN en entrée remet ses filtres à zéro au lieu de le faire taire pour de bon ; il n'ajoute rien sous 28 Hz (test : −93 dB de sa 3e harmonique) ; les harmoniques 4 à 8 de l'808 v2 s'effacent avant 0,45 × la fréquence d'échantillonnage (une note haute sur sa chute de +36 demi-tons) ; une piste 808 envoie aussi sa version et sa chute (sans elles : version 0, comme avant) ; l'808 sur « Triolets » garde ses triolets (décalage fin des pas, test à 140 BPM : 35,7 ms) ; `BassesTest` compare les effets visés par les courbes avant / après l'exciteur ; le Guide connaît « Basse audible » (carte du Sub, pourquoi de l'effet).
- Restants, notés : le Sub et sa basse ont chacun un exciteur, leurs harmoniques s'additionnent entre 100 et 600 Hz (à juger à l'écoute) ; la carte du Guide « Le sub » propose encore une saturation par-dessus l'exciteur ; jungle a une courbe sur une case vide de la tranche 4 (avant comme après l'exciteur, hors basses) ; pas de test spectral de repliement pour l'808 haute (couvert par construction) ; « Doubles-croches » se coupe en deux lignes dans sa tuile sur téléphone.

## Leads lot 1 : relecture neuve (2026-10-25)
- Rien de grave. Corrigé : le gain de vélocité des voix FM glisse dans le bloc (plus de marche de 30 % au bord d'un bloc) ; l'égaliseur des leads et des nappes ne coupe jamais au-dessus de leur note la plus basse (comme les coupe-bas de G0) ; changelog : le stab métallique ne suit pas la force du toucher.
- Reste (mineur) : pas de test pour une couche Plaits FM qui suit la vélocité en version 3 ; deux vérifications faibles (`play(1, 0.4f) == none`, aller-retour JSON dans `WideMixTest`) ; `FxPresets.soundKind` et le sélecteur Plaits ne reconnaissent plus l'ancien « Piano FM » (effets proposés et surbrillance seulement, jamais le son) ; la piste « Nappe » de la couche de nappe porte le même nom qu'une piste d'accords « Nappes » ; `WideMix.withChorus` ne vérifie pas `instrument == null`.

## S6 son : points restants (2026-10-25)
- **M10, largeur non atteinte** : côté/centre du Drop −18,4 → −17,9 dB (8/37 dans −15 à −9). Les échos ping-pong (303, nappes) ne suffisent pas : kick, basse, clap et lead restent au centre. Suite : un chorus stéréo dans le moteur (`core/Effects.cpp`, le chorus somme en mono), derrière une version de son pour ne pas changer les anciens projets.
- **M11, perte au téléphone non atteinte** : 4,1 LU en médiane (18/37 ≤ 4), « Basse audible » +0,1 n'y change que 0,1 à 0,3 LU. Une seule tentative (le reste est le chantier basses : sons de basse plus riches en 300-800 Hz).
- **G5** : 11 styles ont encore leur Intro à plus de 1 LU de l'export dans le direct (10 en dessous, afrobeats +1,1 au-dessus) (riddim −2,2, acidcore −2,1, uk garage −1,9) alors que le gain est le même : à chercher (premières secondes du flux, queue de réverbe absente au départ). Le gain estimé vient d'un rendu de 8 à 16 mesures (quelques secondes sur un téléphone) : si ▶ arrive avant, le gain le rejoint en 20 ms au moment où il arrive.
- **G6** : le banc encode avec FDK AAC ; l'encodeur du téléphone peut être un autre (fabricant). `M4aGuard` vérifie toujours le fichier décodé ; à confirmer sur le Honor (journal « M4A : crête décodée trop haute »).
- Le banc d'export lance aussi, dans `SongBenchExport`, l'estimation du gain du direct de chaque morceau (inutile pour l'export, quelques secondes par style) ; ses fichiers temporaires ne s'appellent plus `.pscmd`.

## S6 son : relecture neuve (2026-10-25)
- Rien de grave. Corrigé : une estimation du gain du direct qui échoue n'est plus relancée à chaque ▶ ; un nouvel essai du M4A à un plafond plus bas ne tasse plus davantage la partie forte (1 dB de plus au plus) ; « Écouter » ne démarre jamais sur le Drop lui-même (une partie longue avant lui) ; la CI ne casse plus si `fdkaac` n'est pas installable (le banc passe par ffmpeg).
- Reste (mineur) : l'estimation rend 8 à 16 mesures dans un tableau jeté (jusqu'à ~21 Mo à 70 BPM) ; elle ne se refait pas après un changement de son ou de mix (le gain du direct reste un peu haut si le nouveau son est plus doux) ; pas de test d'appli qui vérifie qu'un projet en version de mix 1 n'est pas estimé.

## S7 son : points restants (2026-10-25)
- **M10 atteint** (côté/centre du Drop −17,9 → −11,7 dB, 33/37 dans −15 à −9). Hors cible par le bas : deep house (−15,5), dubstep (−16,0), neurofunk (−16,8), phonk (−16,9) : leur Drop est surtout kick, basse et 808, qu'on garde au centre. La perte en mono passe de −0,15 à −0,54 LU en médiane (pire −0,96) : à écouter sur le Honor en mono (un seul haut-parleur).
- **M11 atteint** (perte au téléphone 4,1 → 3,8 LU, 23/37 ≤ 4) par le haut du spectre, pas par les basses : lo-fi 6,6, boom bap 6,2, afro house 6,1, deep house 5,6 restent loin ; la suite reste le chantier basses (B2).
- **G5** : deux Intro du direct encore à plus de 1 LU de l'export. Riddim (−1,6) : le gain estimé (Drop rendu seul) est 1,2 dB au-dessus de celui de l'export et l'Intro perd 5,5 à 6,4 LU en direct contre ≈ 3,7 dans l'export : la musique qui entre dans la chaîne n'a pas les mêmes crêtes, à chercher (la Montée, le faux drop, la 808 ?). Jersey club (−1,3) : le gain estimé sur le Refrain seul est 1,1 dB sous celui de l'export du morceau entier. Une estimation sur plus d'une partie (le Refrain et ce qui le précède) rapprocherait les deux.
- Les morceaux faits avant S7 (version de mix 2) gardent l'ordre fader puis chaîne en direct : leurs Intro restent à −1 à −2 LU de l'export (house, acidcore, uk garage…) tant qu'ils ne sont pas refaits.
- La deuxième Montée de l'acid techno monte de +1,7 LU (+1,9 en S6, déjà sous +2) ; acidcore Drop − Groove +1,84 (+1,96).


## S7 son : relecture neuve (2026-10-25)
- Rien de grave. Corrigé : l'étalement rallumé (potard, automation) repart du silence, sans le bout d'audio d'avant ni de « tic » ; sa ligne tient en 4 Ko au lieu de 8 dans chaque emplacement d'effet (la plus longue prise, 912 échantillons, y tient à 48 kHz) ; un élargisseur posé avec deux réglages n'hérite plus de l'étalement du précédent ; commentaire faux sur les exports corrigé.
- Reste (mineur) : l'étalement change sans lissage quand on tourne le potard (léger grésillement possible) ; en version 4 le clic du métronome n'est plus atténué par le fader du master ; la phase du chorus n'est pas remise à zéro au départ du transport (direct et export diffèrent un peu) ; pas de test qui éteint puis rallume l'étalement ni qui compare un rendu direct v3 bit à bit avec le fader sous l'unité.

## Guide G2 : relecture neuve (2026-10-25)
- Grave, corrigé : l'écoute « Sans / Avec » continuait pendant une prise (le panneau était seulement caché) ; elle s'arrête maintenant dès qu'une prise commence, sur la page Live et quand le morceau démarre.
- Corrigé aussi : un message fermé ne revient plus après une rotation ni sur un autre morceau ; une écoute préparée pour un morceau qui n'est plus ouvert n'est pas jouée ; les prises d'écoute d'un autre morceau ne restent plus en mémoire.
- Reste (mineur) : une action du Guide lancée sur un morceau peut se rejouer sur le suivant s'il vient de s'ouvrir (code G1) ; le Mode simple analyse deux fois par modification ; les cartes qui changent le niveau du master ou les courbes ne s'entendent pas dans l'écoute (▶ à cacher pour elles) ; une très longue écoute peut être refusée par le téléphone (« Écoute impossible », sans plantage) ; le réglage éclairé ne s'éteint pas au changement d'espace ; pas de test d'égalité de volume sur le chemin du Guide ni d'arrêt pendant la préparation.

## Guide G3 : relecture neuve (2026-10-25)
- Grave, corrigé : copier la chaîne d'une piste sur une autre (ou ajouter un favori) ouvrait une fenêtre « Compris » par-dessus la fiche, qui bloquait tout toucher (le test avait été réordonné autour) ; la fiche montre son message dans sa propre ligne, la fenêtre ne s'ouvre plus tant que la fiche ou l'éditeur de couches est ouvert, et le test touche de nouveau une case après la copie.
- Corrigé aussi : la puce « Seule » d'une couche s'éteint quand la pile change (annuler, « Ramener »), comme le moteur.
- Reste (mineur) : un « Ramener » refusé depuis la fiche ne s'affiche que dans le panneau du Guide ; une ligne d'historique peut regrouper plusieurs gestes de suite ; chaque image d'un glissé compare tout le morceau (≈ 1 ms sur un gros morceau) ; la fiche reste ouverte après un changement de projet ; « Annuler » n'est pas à portée depuis la fiche ; pas de test de copie entre pistes de natures différentes.
- 2026-10-25 · Captures : `nav/mission_sound.png` dépend du moment où le Guide a fini de lire le morceau (barre « Guide » seule ou « Étape 1/22 … ») : capture instable, à rendre déterministe (attendre l'analyse du Guide avant la capture).

## Guide G4 : points restants (2026-10-25)
- Corrigé depuis les revues précédentes : fx.sub et fx.vocal ne se cochent plus avec un simple coupe-bas (détecteur `colour:`) ; les cartes dont le changement ne s'entend pas dans l'écoute n'ont plus de ▶ ; un « Ramener » refusé depuis la fiche s'affiche dans la fiche.
- Reste : défaire un Break ajouté laisse parfois un point de courbe en plus, qui tient la même valeur (le son est le même, testé courbe par courbe) ; une scène créée puis défaite peut laisser l'effet « joue dans » qui la nommait.
- Reste : les mesures (≈ 15 rendus courts) ne sont prises que musique arrêtée ; leur durée sur un vrai téléphone n'est pas mesurée ; la mesure automatique en échec ne dit rien (seul « Placer chaque piste » le dit).
- Reste : le « throw » vise le lead, sinon la voix ou la nappe ; la respiration du filtre agit sur toute la sortie, pas sur une piste ; la balance ne règle que les instruments, la 303 et les pistes audio (pas les pads).
- Reste : la référence n'est pas gardée avec le projet ; le rééchantillonnage est linéaire (pour comparer, pas pour mixer).
- 2026-10-25 · Test `GuideViewModelTest.theEffectsAreListedAndTheBalanceIsMeasuredFirst` : a échoué une fois dans la suite complète en attendant l'écran (« mesures périmées ») ; l'attente vise maintenant le modèle directement.

## Guide G4 : relecture neuve (2026-10-25)
- Rien de grave. Corrigé : « Placer chaque piste pour moi » touché pendant une mesure attend la fin de la mesure au lieu de ne rien faire (test `aBalanceAskedDuringAMeasureWaitsForItAndAMeasureCanBeStopped`) ; une mesure (une quinzaine de rendus) s'arrête quand la musique démarre ou que le panneau se replie, et ne se lance pas pendant une prise (« Le Guide écoutera le morceau après la prise ») ; après l'équilibrage, seuls les niveaux des pistes restent connus : le volume du morceau, les parties et le mono sont effacés puis mesurés de nouveau (les cartes du contraste et de la marge ne lisent plus de vieux chiffres) ; le ▶ de l'équilibrage n'apparaît qu'une fois les pistes mesurées.
- Reste (mineur) : défaire un Break ajouté pendant que la scène du Break est affichée ; le décodage d'une référence de 120 s tient tout en mémoire (≈ 46 Mo) ; pas de tests sur des morceaux inhabituels (une seule partie, pas de kick, que des pistes audio).

## Guide G5 : points restants (2026-10-25)
- Fait : « Ton morceau est prêt », les Missions et la « Prochaine étape » sont des cartes du Guide (« Pour commencer ») ; leurs anciens réglages sur le téléphone sont lus une fois (missions en cours) puis effacés ; la capture instable `nav/mission_sound.png` (G3) n'existe plus, les nouvelles captures du Guide attendent la fin de l'analyse.
- Reste : le guide du morceau est gardé dans la langue où il a été écrit (changer de langue ne le traduit pas) ; « Changer un son » et « Enregistre une mélodie » n'ont que des liens « Voir le réglage », pas de bouton principal ; la création d'un morceau par l'assistant jusqu'à l'ouverture du Guide n'est testée que par morceaux (l'activité recréée n'est pas rejouée en Robolectric) : à vérifier sur émulateur en G6.

## Guide G5 : relecture neuve (2026-10-27)
- Grave, corrigé : la signature des notes comptait aussi la partie affichée, qui change quand le morceau défile : « Écouter » ou un toucher sur une partie cochait « Enregistre une mélodie », et « Mon premier morceau » sautait cette étape. Seules les parties comptent (test `showingAnotherPartDoesNotTickTheMelody`).
- Corrigé aussi : toutes les mesures (et un équilibrage qui en attend une) sont sous un même parent, arrêtées ensemble ; la mesure après l'équilibrage passe par `measure()` (pas pendant la musique ni une prise) ; « Ton morceau est prêt » est décidé dès que le morceau a joué (▶ du haut compris) et mis « plus tard » quand on replie le panneau dessus : le Guide ne s'ouvre plus tout seul à chaque lancement (tests `theTopPlayButtonDecidesTheReadyCard`, Mode simple) ; l'exemple d'un mot et « Sans / Avec » se coupent l'un l'autre, l'exemple s'arrête quand le morceau démarre, qu'une prise commence ou que le panneau se replie ; les anciennes missions ne sont reprises qu'une fois un projet ouvert ; `next_steps.xml` retiré des règles de sauvegarde ; fermer l'assistant sans morceau annule « Mon premier morceau » pour le suivant ; « Reprendre Mon premier morceau » disparaît quand ses cartes sont toutes décidées (ou sur un autre projet) ; la signature du son (un JSON de tout le morceau) n'est plus calculée deux fois par analyse.
- Reste (mineur) : plusieurs marques du Guide sont des modifications discrètes (« partagé », « écouté », « plus tard » au repli, la reprise des missions), pas des étapes d'annulation : « Annuler » ne les défait pas (voulu, mais à savoir) ; le guide du morceau reste dans la langue où il a été écrit ; quelques tests vérifient l'état plus que l'écran (le panneau ouvert tout seul après une vraie création n'est vérifiable que sur émulateur, G6).


## Guide G6 : vérification (2026-10-27)
- Corrigé : revenu un jour ou plus après, le Guide s'ouvre seul sur « Te revoilà » (§ 3.6), une fois par visite.
- Trouvé par les tests, pas corrigé (gestes, `docs/guide/G6-verification.md`) : une 3ᵉ option, le lien « La fiche de la piste », « Voir les couches » et l'historique d'une fiche sont sous le pli sur un téléphone (un défilement de plus) ; ajouter une couche conseillée coûte 4 gestes (2 visés) ; avec le panneau ouvert (40 %) sur un petit téléphone, la 5ᵉ piste de la ligne du temps est cachée (« Enregistrer une mélodie » : 3 au lieu de 2). Pistes : options plus compactes, le lien de la fiche dans l'en-tête de la carte, la carte des couches qui ouvre l'éditeur sur le rôle conseillé, un panneau au tiers comme le dit le plan.
- Son (banc `docs/son/banc/guide-G6.md`, morceaux suivis jusqu'au bout) : Drop − Break sous +4 pour acidcore, gabber, hard techno, trance ; 12 styles sans Break ; 15 styles à deux Drops seulement. La carte « Le contraste » dépend des mesures du téléphone, que le banc ne fait pas.
- Mineurs : une courbe sur tout le morceau est listée dans la fiche sans les parties où elle bouge ; lo-fi et deep house ne se créent pas dans Robolectric (l'assistant rend des sons du kit avec le moteur) : le Mode simple est testé sur jump up ; la fabrique de ViewModels par défaut garde l'application du premier test Robolectric (les tests sur `MainActivity` l'oublient, `GuideJourneyTest`).
- Reste à faire avec un téléphone : le parcours sur émulateur ou téléphone (processus vraiment tué), la durée des mesures, les 8 testeurs.
## Son 8 : points restants (2026-10-27)
- **Leads lots B à D faits** (moteur, recettes, couches), mesurés dans `docs/son/banc/apres-son8.md`. Pas atteint : le rapport de vélocité des leads (1,07 à 1,13, cible 1,15 ; le Supersaw ouvert à 10 kHz n'en a pas : 0,96), le mouvement des nappes sur 8 mesures (médiane 2,12 → 2,27 dB, cible 2,5 ; 12/30), le centroïde des leads dans 1,5–3 kHz (10/34), l'attaque des leads mesurée dans les morceaux (la médiane des attaques ne bouge pas : beaucoup de leads sont des voix Plaits ; à la sonde, Supersaw 1,3 → 10,9 dB).
- Pas fait du lot D : la couche médium sous un stab reese ou wobble ; la paire FM 2 opérateurs du synthé n'est dans aucune recette (le stab neuro reste celui de Plaits) ; le piano VCSL.
- **Direct = export (reste de S7)** : riddim et jersey club ont toujours leur Intro à plus de 1 LU de l'export en direct (jersey −1,27 ; le gain estimé sur le Refrain seul reste 1 dB sous celui de l'export). Aucune nouvelle tentative ici : l'estimation sur plus d'une partie reste à faire.
- Le chorus mono (4 réglages, projets d'avant) ne repart toujours pas avec le transport : le relancer changeait leurs exports. Seul le chorus stéréo (nouveaux et assistant) repart.
- Le grave d'un son de lead joué en basse (grime) garde son son d'avant (sans les touches des lots B–C) ; un son de lead joué en basse ailleurs (un choix de l'utilisateur) les prend.
- Restes de la relecture S7 faits : « Étalement » lissé, clic du métronome sous le fader en version 4, chorus (stéréo) relancé avec le transport ; reste du S6 fait : estimation refaite 1,5 s après un changement de son ou de mix.

- Tests de temps hôtes : `master_cpu_budget` et `s4_no_alloc_and_cpu` échouent sur la machine de l'agent ce jour-là, avec le code de départ aussi (chaîne master 2,2–2,4 % d'un cœur, synthé large 16,7–18,3 % ; son 8 : 2,1–2,6 % et 18,2–19,1 %). Le chemin `renderLeads` (supersaw JP, ladder propre, FM) n'a pas de mesure de CPU dédiée : à mesurer sur le Honor.

## Son 8 : relecture neuve (2026-10-27)
- Grave, corrigé : la clé de la nouvelle estimation du gain du direct comprenait tout le mixeur (mute, solo, faders) : mettre le charley en solo pendant la lecture relançait une estimation sur ce solo, puis le gain montait et le mix entier entrait dans le limiteur une fois le solo levé. La clé ne prend plus que les sons, effets, envois, macros, master et bus ; une estimation qui ferait monter le gain pendant la lecture ou une prise attend le ▶ suivant (une qui le baisse est prise). Test `LiveDriveEstimateTest.aSoloMuteOrFaderIsNotEstimatedAgain`.
- Corrigé aussi : l'export ne cède son gain qu'à une estimation qui démarre vraiment ; phases de la paire FM ramenées par floor, indice FM borné par la hauteur (pas de repliement) ; seuil de la primitive du drive avant filtre relatif à l'entrée ; plucks suivis à 70 % (Do6 −64 dB) ; le passe-haut du clic repart avec le transport ; test `leadsAtRest()` aux valeurs par défaut et rendu doré du synthé au repos (empreinte prise sur le moteur de départ).
- Reste (noté) : le chorus stéréo relancé au départ du transport efface ses 50 ms de queue ; basculer « Entrée » Mono/Stéréo pendant la lecture fait un petit saut ; CPU du chemin `renderLeads` (supersaw JP, ladder propre, FM) à mesurer sur le Honor.


## Tuto lot 5, équipe contenu : points restants (2026-10-28)
- Cymbale à l'entrée du Drop : retirée du tuto. Un coup sur la 1re case d'un Drop de 2 mesures revient toutes les 2 mesures (Théo). Il faut une option « 1re fois seulement » sur un coup (modèle + moteur), puis remettre l'étape.
- Détail « toutes les 8 ou 16 mesures » : même limite (boucles de 2 à 4 mesures) ; l'étape « variation » demande maintenant un coup de percussion ou de charley ouvert dans le Drop 2, sans parler de 8 ou 16 mesures.
- Montée : une seule boucle de 4 mesures (une boucle de batterie ne dépasse pas 4 mesures) ; pas de hauteur qui monte (les sons de batterie synthétisés ignorent la hauteur d'un coup).
- Idées de Théo, pas faites : réglage du temps de l'écho visible (1/8, 3/16), glissé en millisecondes et mode « legato seulement », riff et accords plus riches (décision 1A de Benjamin).
- Mots du Facile hors tuto : « Scie », « Triangle », « Carré » dans les sous-titres des sons (assistant, `SoundLooks.describe`) ; il faudrait des mots simples en Mode simple (et leurs 14 traductions).
- « Gain ajouté pour la cible : −100,0 dB » (d/284) : sans doute une estimation faite sur du silence (le faux moteur du banc ?), à vérifier sur téléphone ; au minimum, ne rien afficher tant que la valeur n'a pas de sens (`MasterLoudness`, `drive`).
- « Les pistes sont éclatées » (d §8) : ce n'est pas le tuto, c'est la rangée de pistes de Sons (`SonsHeader`, `AppSpaces.kt`). Batterie, Basse acide, Voix et Remix sont en tête ; Basse, Sub, Accords et Mélodie viennent après, hors de l'écran d'un téléphone (« Remix · Sépare… » coupé, d/111), et la rangée ne défile pas jusqu'à la piste ouverte. Au Mix, elles sont toutes là. Pistes : amener la puce allumée dans l'écran, ou mettre les pistes d'instrument en tête ; le nom de la piste en cours au-dessus du piano roll.

## APK 228 : restes après l'intégration (2026-10-28)
- Tuto : la cymbale d'entrée du Drop attend une option « une seule fois » sur un coup (modèle et moteur) ; la Montée de 4 mesures n'est qu'une case étroite sur la carte des parties.
- Tuto : capture `tuto_app_bass` : la rangée d'outils du piano roll est à moitié cachée sous « Partie : A » quand la page défile vers les notes en pointillé.
- Tableau des pistes : « → Pad » prend toujours le premier pad libre (pas de choix précis) ; pas de tableau pour un morceau séparé hors projet ; « → Pad / → Live » existent encore sur la page Remix à côté du tableau (à unifier).
- Sons : la rangée des pistes ne défile pas jusqu'à la piste ouverte (Basse, Sub, Accords, Mélodie après Remix, hors écran).
- Mots de l'assistant hors tuto (« Scie », « Triangle ») : à simplifier avec leurs 14 traductions. « −100 dB » au master (d/284) : à vérifier sur téléphone.

## Tuto tous styles : relecture neuve (2026-10-29)
Bilan du relecteur : 0 GRAVE, 6 MOYEN, 8 MINEUR. Corrigés :
- MOYEN 1 : le kit du style ne remplace plus en silence les sons d'un tuto commencé. Seul un tuto vierge (aucune étape faite, aucun son à soi sur un pad) le reçoit tout seul ; sinon la carte demande « Mettre les sons du kit … sur les pads » / « Plus tard » (`TutoKit.offer`). Un kit téléchargé depuis la carte arrive sur les pads.
- MOYEN 2 et 3 : `TutoAllCoursesTest` vérifie l'ordre réel des parties et leurs durées, fait aussi les étapes « changed:… » (comme l'appli, depuis leur point de départ), dit pourquoi les autres sont sautées (« C'est fait », exemples, partage) et échoue si plus de la moitié le sont ; contenu vérifié : kick et basse là où l'étape les garde, aucun kick dans une partie « nokick », aucune note hors gamme, aucune basse sous Mi1.
- MOYEN 4 : `TutoKitTest` (carte, « Plus tard », hors ligne, tuto vierge en une étape d'annulation, tuto commencé, téléchargement demandé).
- MOYEN 5 : médiums du drum & bass et de la jungle, mesurés sur le morceau Pro (bande 500-2000 Hz, part du mix) : drum & bass -14,3 → -10,3 dB (assistant -8,0), jungle -21,4 → -16,0 dB (assistant -9,2). Causes : la piste Sub du tuto était le son de la basse une octave plus bas (le grave mangeait le limiteur) ; les pistes Basse, Accords, Mélodie avaient le son nu de la bibliothèque, pas celui que l'assistant finit (reese, couche médium) ; la courbe de filtre de la Montée gardait le filtre fermé avant elle (les accords de la jungle muets dans le 1er Drop) ; accords et mélodie de la jungle une octave sous ceux de l'assistant.
- MOYEN 6 : « Copier » ne descend jamais la basse sous Mi1 (ou sous la note la plus grave que le cours écrit lui-même) ; ligne de basse de la pop une octave plus haut (registre de l'assistant).
- MINEUR 1 (`loadTutoKit` une seule fois à la fois), MINEUR 2 (le mode Simple/Pro d'un projet supprimé pour de bon est oublié), MINEUR 4 (les tests des cours échouent sans cours), MINEUR 6 (étape « Streaming » de la pop : « un peu moins fort que la démo, c'est le niveau des plateformes »).

Restes :
- Jungle : bande médium encore 7 dB sous l'assistant. Le Drop du cours n'a pas la mélodie (c'est la nouveauté du Drop 2), l'étape des volumes baisse accords et mélodie de 2 dB, et la chaîne d'effets de l'assistant (égaliseur, clipper, élargisseur) n'est pas reprise : la reprendre occuperait des emplacements que les étapes « insert:… » attendent libres.
- Basse au-dessus de la Sub : l'assistant coupe le grave de la basse (passe-haut 80-120 Hz), pas le tuto. Un filtre sur la piste Basse serait pris par l'étape « automation » de la basse (speed garage, tech house), qui le balaierait comme un passe-bas.
- MINEUR 3 : course possible entre « choisir le mode » et l'effet `follow` (`AppRoot.kt`), à reprendre en ne posant le mode qu'aux choix explicites.
- MINEUR 5 : un triolet peut déborder d'un coup dans la boucle suivante si on raccourcit ensuite la boucle du pad ; couper `span` dans `sanitized` (modèle de la batterie).
- MINEUR 7 : pas de sidechain sur la basse dans le tuto (étape Pro « pompe » à ajouter au chapitre mix des styles club).
- MINEUR 8 : la 808 du trap ne « tape » pas (notes longues et liées) ; raccourcir d'une case dans le cours.
- Styles 808 (trap Ré♭1, drill et jersey club Mi♭1 / Ré♭1) : notes écrites sous Mi1 par leur cours, gardées (la saleté et le clic de la 808 la portent sur un téléphone).
- `HumanRemixJourneyTest` : la forme d'onde arrive après la lecture des pistes, hors du fil principal ; le test attend jusqu'à 10 s (1 s avant, trop court quand toute la suite tourne).

## Enregistrement d'écran sans son (Benjamin, 2026-10-29)
- Réglé : c'était un paramètre de l'enregistreur du téléphone (MagicOS 11), pas l'appli. Sa vidéo suivante a bien le son.

## Corrections à faire (liste de Benjamin)
1. **Pads absents dans un projet vide** (précision de Benjamin) : on ouvre un projet déjà créé mais vide (rien dedans), puis l'onglet Rythme. Aucun pad. On choisit un kit : toujours aucun pad. On va sur Clavier (Synthé) et on revient : les pads apparaissent. À reproduire dans un test (projet vide créé avant, rouvert, onglet Rythme, choix d'un kit), puis à corriger.
2. **Clignotement après l'ouverture** : à la fin de l'animation (logo et vibrations), on voit d'abord la page d'avant (le dernier écran de la session précédente ?), puis un clignotement avant l'accueil et les projets. Il faut passer de l'animation à l'écran final sans montrer d'écran intermédiaire.
3. **La 303 introuvable** (Benjamin, « ce n'est pas la première fois ») : en Mode simple, aucun chemin ne mène à la « Basse acide 303 ». La pastille « Basse acide · 303 » n'existe qu'en Pro (`AppSpaces.kt`, `if (!simple)`), et « + Piste » ne la propose pas. Benjamin (impératif depuis le début) : elle doit être **visible et mise en avant** dans les deux modes, **surtout en Mode simple**, où on doit la trouver tout de suite, en jaune, avec un look « acide » ; à mettre dans « + Piste », et en pastille propre en Mode simple. Nom choisi par Benjamin : **« Acid 303 »** (« TB-303 » est une marque de Roland, donc écarté pour le Play Store).
4. **Pas de retour à l'accueil depuis un projet** (Benjamin) : depuis un projet, « Mes projets » ne mène pas à l'accueil « Que veux-tu faire ? », avec sa carte Remix. Le retour arrière ne sort pas non plus : il faut tuer l'appli. Il faut un chemin évident vers l'accueil, par exemple « ← » de « Mes projets » vers l'accueil, ou une entrée « Accueil ». Le Remix, lui, doit être accessible directement depuis un projet en Mode simple : aujourd'hui, il est caché dans « ⋯ › Extraire la voix d'une chanson ». À renommer « Remix » dans ce menu.
5. **Toujours là (APK 233, vidéo de Benjamin)** : « le pad n'est pas ouvert directement ». Dans la vidéo, « Continuer › Mon projet 3 » (projet vide, Simple) ouvre sur « Mon morceau » ; les pads n'apparaissent qu'en touchant Rythme (ils sont bien là à ce moment-là). Choix de Benjamin (1A) : un projet vide s'ouvre sur les pads (Rythme) ; un projet déjà rempli s'ouvre sur « Mon morceau ». À faire au prochain lot : test qui le reproduit, puis correction. Vidéo : `/root/.claude/uploads/2ed29bda-5e9a-5850-a6a7-bbf3293c7a09/19112db3-SVID_20261029_225056_1.mp4`.
6. **Kick qui disparaît dans le mix complet (APK 233, projet Acidcore de Benjamin)** : seul, le gros kick est là ; avec toutes les pistes, il disparaît. Cause (corrigée, efea930) : la saturation de la 303 du style Acidcore n'avait pas « Volume constant » : la 303 montait de +10,7 LU, passait 10 LU au-dessus de la batterie, et le master l'écrasait avec le kick (un coup de kick ne faisait plus bouger le mix : 0,1 dB, contre 4,7 dB batterie seule ; 3,0 dB après correction). Projets Acidcore déjà enregistrés réparés à l'ouverture (mix v5). Reste : la saturation du master Acidcore est aussi sans « Volume constant » (laissée, la cible de sonie compense) ; les tests actuels vérifient les réglages, pas encore le son. **Demande de Benjamin : une fois la cause trouvée, vérifier partout** pour éviter ce genre de problème : les 37 styles (morceaux de l'assistant et du « en un toucher »), chaque partie (Groove, Drop…), en direct et à l'export. Mesure automatique à ajouter aux contrôles : le kick garde son impact (bande 40–120 Hz, attaque) dans le mix complet par rapport à la batterie seule ; un test qui échoue si un style le noie (303, hoover, basse, nappe, compression ou saturation du master, ducking).
7. **Pas à pas : le kick choisi ne sonne pas comme dans le morceau (APK 233, Acidcore)** : à l'étape Kick, le kick intégré s'écoute propre, alors que le morceau lui met une forte saturation (gabber) ; seuls « Sons intégrés » et le pack déjà téléchargé (Tech house) sont proposés. À faire : écouter chaque son avec les effets du style, et mettre en avant les packs du style (Gabber, TR-909) au bon endroit.
8. **Live : les boutons du bas « en tas » (APK 233, capture)** : Effets / Acid 303 / Pads / Notes, puis Tout remettre / Verrouiller / ⚙, puis Pad / Tenus / Répéter / Dub / Montée / Pistes, puis Filtre DJ / Espace : quatre rangées de boutons sans ordre clair, on ne sait pas à quoi elles servent. À réorganiser (design délégué) : une seule rangée d'onglets clairs, le reste rangé dessous avec des titres.
9. **Créer le morceau depuis le Live (APK 233, Benjamin)** : il pensait pouvoir construire tout le morceau depuis le Live (poser des sons, des effets, des petits sons déjà enregistrés de « Ma bibliothèque ») ; aujourd'hui une case vide ne fait rien, il faut créer ailleurs puis revenir. Réponses de Benjamin : 1A (case vide = petit menu : « Jouer et enregistrer ici », « Mettre un son de ma bibliothèque », « Copier une autre case ») ; 2 « pas de limite » (on joue, on retouche la case pour arrêter, aucune durée maximale ; la fin se cale sur la mesure pour que la boucle reste en rythme) ; 3A (les effets bougés pendant l'enregistrement sont gravés dans la case et rejoués à chaque fois).
