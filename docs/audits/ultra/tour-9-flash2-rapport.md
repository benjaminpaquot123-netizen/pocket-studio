# Tour 9, lot « flash2 » (les deux rouges de la CI 410) : rapport

Branche : eq/tour-9-flash2 (venue de adba37efb). Rouge ad290f7b7, vert 1e9f33333.
Tout est « tenté » : aucun « corrigé » tant qu'un testeur neuf ne l'a pas constaté.

tests: *ProjectsQueueTest *Coquille2SeatTest *Tour5BloquantsTest *FirstLaunch* onboarding.* *LaunchFlow* *HomeAndPadsTest *Tour3*Idle* *Tour5* ui.Coquille* human.BenchSelfTest

## Chemins

chemin: Mes projets, après une opération interrompue : la liste se charge quand même | test: ProjectsQueueTest#theListStillLoadsAfterAnOperationWasCancelledOnItsOwn
chemin: Pad Live, puis un morceau, puis Mes projets : le projet Live se rouvre | test: Coquille2SeatTest#p5_theLiveProjectOpensAgainOnPadLive

## Ce qui change (« tenté »)
- Les deux rouges sont plus anciens que « flash » : p5 rouge pareil en CI 398 et 403 ; Tour5 « partie neuve » déjà instable (tour-9-rouges3-rapport.md). En local sur adba37efb, les deux passent (deux fois, aussi sous charge).
- p5 : la CI montre « Chargement de la liste… » pendant 60 s et « aucun fil de l'appli au travail ». La file des projets n'a qu'un ouvrier ; une opération finie sur une annulation à elle (modèle encore vivant) l'arrêtait pour de bon, et chaque lecture suivante restait en file.
- `ProjectsViewModel.kt` : l'ouvrier ne s'arrête qu'avec le modèle ; sinon il note l'interruption, enlève « Chargement » et passe à l'opération suivante. Test rouge puis vert : `ProjectsQueueTest`.
- Tour5 `simplePartsPickedAfterTheGuidesListenAreEdited` : aucune cause trouvée, rien de changé ; ni sauté ni affaibli.

## Tests lancés
- ProjectsQueueTest : rouge sur ad290f7b7, vert sur 1e9f33333 ; ProjectsSafetyTest, ProjectsTrashTest, NewSong* : verts.
- Lot de vérification sur 1e9f33333 (27 classes, dont Tour5BloquantsTest, Coquille2SeatTest, Coquille3SeatTest, onboarding.*, LaunchFlowTest, HomeAndPadsTest, Tour3*Idle*, Tour5*, FirstLaunchTest, BenchSelfTest) : 172 tests, 0 échec, 5 sautés.

## Ce qui n'est pas vert
- Cause de p5 probable, pas prouvée sur la CI (aucune reproduction locale) : « tenté ».
- Tour5 « partie neuve » : intermittent, cause inconnue, non traité.
- CI complète non lancée.

## Demandes (hors de mes fichiers)
Aucune.
