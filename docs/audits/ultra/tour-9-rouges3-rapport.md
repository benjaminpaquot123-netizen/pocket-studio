# Tour 9, lot « rouges3 » (tests rouges de la CI 404) : rapport

Branche : eq/tour-9-rouges3 (venue de 1f1d6ac15), à jour de l'intégration `ccr-80af1a1b-mn0wec` (f6d0a30b4, fusion aeef1423d).
Tout est « tenté » : aucun « corrigé » tant qu'un testeur neuf ne l'a pas constaté.

tests: *ScaledWindowsTest *TutoPanelFitsBigFontTest *T9PanelTargetTest *Coquille2SeatTest *Tour9L14TutoPanelTest

## Chemins

chemin: Fenêtre des effets, « étape cochée » par-dessus | test: ScaledWindowsTest#noWindowIsOpenedOutsideTheScaledWrappers
chemin: Panneau du tuto à 200 % sur 360 dp, « Pourquoi ? » et « Lire la suite » | test: TutoPanelFitsTest#panelFits
chemin: Tuto, chapitre 12 « fill », cases au-dessus du panneau (japonais, 200 %) | test: T9PanelTargetTest#fillCellsAboveThePanelIn360At200Japanese
chemin: Grille de la Montée, ligne du pad (siège Coquille2) | test: Coquille2SeatTest#cymbalGridLineNamesThePadShown

## Ce qui change (tous « tentés »)
- `ScaledWindowsTest` (cause : L14, F021, 0082f3638) : `TutoTickedOverWindow` ouvrait un `Popup` brut ; il passe par `ScaledPopup` (surcharge avec `PopupPositionProvider` dans `Windows.kt`). a49ba41f2.
- `TutoPanelFitsBigFontTest` fr_5 (cause : L13, plafond 60 % du panneau, F003) : la consigne avait 0 px, « Lire la suite » ou « ▸ Pourquoi ? » coupé. Ce qui manque à sa ligne et demie relève le plafond, avec ou sans offre de kit (`TutoPanel.kt`). dfb7f05ff.
- `T9PanelTargetTest` fill (cause : rouge depuis L14, 30b800810) : à très grande police, la mise en vue ramenait toute la tête (pages de mesures sur deux rangs et ligne du tuto), plus haute que la place au-dessus du panneau compact ; elle ne ramène plus que la ligne du tuto la plus proche (`Spotlight.kt`). 870e384bc.
- `Coquille2SeatTest` : vert sur f6d0a30b4 (lot « difficiles », b122cb4c1), sans changement ici.

## Ce qui n'est pas vert
- Contrôle du tuto (débutant neuf) : non joué (`TutoPanel.kt` touché) ; à faire à la grande vérification, comme L13 et « difficiles ».
- CI complète non lancée (tests ciblés seulement).

## Demandes (hors de mes fichiers)
Aucune.
