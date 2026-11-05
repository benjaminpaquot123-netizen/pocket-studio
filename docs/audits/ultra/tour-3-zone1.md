# Audit ultra, tour 3 : zone 1 (Pad Live, défis, pads, remix vers pads)

Items 1 à 9 de la synthèse. Tests sans le banc : `Tour3Zone1Test` (vue-modèle + moteur qui note ses commandes), `CutZoomGestureTest`, `CutScreenTightTest`, `StemBoardRouterTest`, `StemBoardVmTest`, `RemixVoiceVmTest`, `PadLiveChallengeTest` (mis à jour).

| N° | Verdict | Gravité | Correction | Test |
|---|---|---|---|---|
| 1 ×4 | appli | grave | Retour de Pad Live va à l'accueil, dont le « silence » (`vm.stop()`) n'arrêtait que la lecture : les pads live qui tournent seuls continuaient. `stop()` coupe aussi les pads (même chemin : accueil, tuto, assistant, suppression du projet ouvert). | `Tour3Zone1Test.leavingPadLiveSilencesItsLoops` (rouge avant : aucune commande) |
| 2 ×4 | banc (verdict de l'équipe banc) | — | L'appli envoie bien `launchClipRow` / `toggleClip(-1)` (`Tour3AppProofTest` de l'équipe banc) ; le banc ne suivait pas ces commandes. « Tout couper » coupe les pads, pas la lecture (la musique a son ■ à côté) : non changé. | équipe banc |
| 3a | banc | bloquant (vu) | Le pad « Voz · listo » de Live › Pads : `toggleLive` envoie bien la commande au moteur ; le banc n'écoute les pads que dans Pad Live (`toggleForTest`), pas sur Live › Pads. À ajouter au banc. | `Tour3Zone1Test.aLivePadTouchedGoesToTheEngine` |
| 3b | appli | grave | Colonne du Live coupée dans Mix (chanson importée « muette » : ses 4 parties coupées) : son haut-parleur disait « Son ». La page Live l'affiche muette ; le toucher rend le son dans Mix (un pas d'annulation) et le dit. | `Tour3Zone1Test.aColumnCutInMix…` |
| 4 revient | appli | grave | Pourquoi ça revenait : « libre » voulait dire « pad de kit que le rythme ne joue pas » (Tom médium, Tom aigu ont un son). Désormais un bout « Sur les pads » va sur un pad VIDE (pad live), jamais sur un son de batterie sans le choisir (« Sur quel pad ? ») ; idem « → Pad » de la page Remix (toujours demandé) et plusieurs bouts (seul le pad choisi). Annuler efface le message (E d 22). Découpe ouverte « pour le pad live N » : « Dans le Live » va sur ce pad (E d 33). | `StemBoardRouterTest` (3 cas), `StemBoardVmTest.aStretchOnAPadNeverTheKick` (+ Annuler), `RemixVoiceVmTest` |
| 5 | appli + banc | grave | Rythme à refaire dessiné et qui reste (chaque coup à sa place dans la mesure, couleur du pad), lumière qui bat au tempo + « ♩ 120 », trois grandes étoiles toujours à la même place, liste des défis toujours visible (le 4e coup ne fait plus bouger l'écran sous le doigt), boutons et liste muets 1,2 s après le score (A a 9 « saute au niveau 5 », F a 41). Banc : 0 étoile à « Boum boum » = touches du banc espacées de plusieurs secondes ; boucle sous l'exemple = `stopLive` non suivi par le banc (l'appli coupe les pads à « Écouter »). | `Tour3Zone1Test.theScoreShows…`, `PadLiveChallengeTest` (rouge avant : « Défi suivant » immédiat changeait de défi) |
| 6 | non fait | moyen | « + » à l'ouverture : probablement le temps de fabrication du jeu (banc : doublure seulement après). Noms coupés à 200 %, « Gros pads » sous Plus : reste. | — |
| 7 ×4 | appli, corrigé par une autre équipe | moyen | Coin des pads nommé « Réglages du pad, pad … » (modification non commitée d'une autre équipe dans `PadLiveScreen.kt`, pas touchée). | — |
| 8 | non fait | moyen | Pack sans « Annuler » visible (il est sous Plus), colonne Remix de l'aide, Klaxon illisible : reste. | — |
| 9 | appli | bloquant | Mode simple sur téléphone : la courbe fixe de 210 dp poussait hors écran « Coupes », les outils et la liste des bouts (pas de retour à « Un bout », rien à cocher, envoi grisé) ; et toucher un bout sur la courbe ne le choisissait pas. Peu de hauteur : courbe 140 dp, colonne qui défile ; toucher un bout le coche (gauche-à-droite et droite-à-gauche) ; l'aide le dit. E d 17-18 : le testeur a pris la barre Début (x 132, Début à 145) : pas un défaut. | `CutScreenTightTest` (ltr, rtl), `CutZoomGestureTest.aTouchOnAPieceChooses…` (ltr, rtl) |

Traductions : `ultra3z1.json` dans les 14 langues (3 phrases).

## Reste
- 6, 8 ; « Découper la voix en phrases » (page Remix) pose encore ses phrases sur les pads 9 à 16 du kit (ses sons de kit sont remplacés, dit après coup).
- Aide du défi en texte seulement ; colonne Remix au nom de fichier ; « Remix en cours introuvable depuis Mon morceau » (B c 11).
- Banc : suivre les touches de Live › Pads (`toggleLive`) comme celles de Pad Live.
