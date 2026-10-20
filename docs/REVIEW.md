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
