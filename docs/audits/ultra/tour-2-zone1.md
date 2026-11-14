# Audit ultra, tour 2 : zone 1 (Pads, Pad Live, défis, remix)

Sièges : lot A c (enfant de 6 ans, tablette 11 pouces couchée, ne lit pas), A b (remix, espagnol), B d (thème nuit, Simple). Items 1 à 7 de la synthèse.

| N° | Verdict | Gravité | Correction | Test |
|---|---|---|---|---|
| 1 | banc (+ appli) | grave | Banc : il lâchait son moteur avant chaque toucher ; « Poser » fabrique le son du pad avec le moteur (`NativeSample.create`) → UnsatisfiedLinkError, coroutine morte, pad « occupé » pour toujours. Le banc garde son moteur tant que la bibliothèque est ouverte. Appli : tout échec de pose (Error comprise) est dit (ligne du pad, et fenêtre pour un pad de batterie : la carte du pad en Simple n'a pas de ligne), le pad est libre ensuite ; même chose pour le jeu de pads de Pad Live (plus de plantage possible). | `PlaceFromLibraryTest` (sans banc : avec moteur le son est posé, nom + Annuler ; moteur qui refuse = message, rouge avant), `BenchPlaceAndPadLiveTest` (rouge avant la correction du banc) |
| 2 | banc (revient) | grave | Avec un moteur qui reste, chaque chemin ouvre Pad Live sur ses pads (carte du premier lancement, carte de l'accueil, retour depuis un autre projet). Le banc n'avait pas de moteur quand le testeur ouvrait Pad Live lui-même (seule l'amenée fabriquait les pads) : il fabrique désormais le jeu de pads dès que Pad Live s'ouvre vide. Appli : le jeu ne se pose que sur le projet « Live » (évite de remplir le projet quitté pendant la reconstruction de l'écran). | `PadLiveReadyEveryWayTest` (2 cas, tablette couchée), `BenchPlaceAndPadLiveTest.padLiveTouchedFromTheWelcomeOpensOnItsPads` (rouge avant) |
| 3 | appli | bloquant | Carte « Défis » (trois étoiles dorées) sur l'accueil et au premier lancement → Pad Live ouvert sur son défi ; dans Pad Live, étoile dorée au lieu de la cible rouge ; boutons du défi avec image (oreille, main, coche, flèche). L'encadré « texte seul » (A c 42) était « il faut des pads coup » : pads vides du banc. | captures `padlive_home`, `padlive*`, `welcome` |
| 4 | appli | moyen | Pad Live depuis un morceau avec musique : « Pad Live a son propre projet « Live » : « Remix » t'attend dans Mes projets. » | (Toast, lu à la main) |
| 5 | appli | moyen | Un bout posé dit le son remplacé, son de kit compris : « « Voix 1 » remplace « Tom médium » sur le pad 9 (la flèche retour le remet). » (StemBoard et page Remix). | `StemBoardVmTest`, `StemBoardRouterTest` (mis à jour, rouges avant) |
| 6 | appli (demande de Benjamin) | moyen | Appui long = toujours la bibliothèque titrée « Changer le son » (plus jamais la fiche « Pad 1 », même pendant une pose) ; son actuel marqué « ✓ Sur le pad maintenant » dans la liste et coché dans la fiche ; « Volume du pad » = position du curseur. ⋮ › « Bibliothèque de sons » garde son nom. | `LibraryRound2Zone1Test`, captures `pad_sheet`, `library` |
| 7a | appli | moyen | ⋮ › « Ma bibliothèque » (Simple) ouvre les sons gardés, titrée ainsi. | lu à la main |
| 7b | appli | mineur | « Découper ce que je veux » : « « Boys » est dans ton projet, muette, pour la découper (la flèche retour l'enlève). » | — |
| 7c | appli | mineur | TalkBack des menus de coupe : « Coupes : Un bout », « Aimant : Au temps ». | — |
| 7d | non reproduit | mineur | Glisser la rangée des familles ne choisit rien hors banc. | `LibraryRound2Zone1Test.aSwipeAlongTheFamiliesScrollsWithoutChoosingOne` |

Traductions : `ultra2z1.json` dans les 14 langues ; deux phrases devenues inutiles retirées de `remixvoix` / `remixroute`.

## Reste
- 4, 7a, 7b : messages sans test d'écran (Toast et entrée de menu).
- Le marquage du son actuel dans la bibliothèque n'apparaît qu'une fois le dossier ouvert (l'accueil de la bibliothèque montre des dossiers) ; le titre dit déjà « Pour le pad 1 (Kick) ».
- Le drapeau orange de l'accueil (missions) reste un mot pour l'enfant : la carte « Défis » est l'entrée imagée.
