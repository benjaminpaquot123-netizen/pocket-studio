# Tour 9, lot L17 « finitions du tuto » : rapport

Branche : eq/tour-9-L17, partie de l'intégration ccr-80af1a1b-mn0wec (tête 4dd5be7d fusionnée).
Défauts visés : F032, F034, F036, F039, F040, F042, F044, F046. Tous « tentés », aucun « corrigé » (un testeur neuf le dira).

tests: :project-model:test --tests '*T9L17*' ; :app:testDebugUnitTest --tests '*T9L17*'
chemin: tuto Structure, « Créer la partie » pendant la démo | test: T9L17PartAfterDemoTest#theNeedTouchedWhileTheDemoHoldsMakesThePartAndKeepsIt
chemin: tuto, étape d'écoute du kick, pad joué | test: T9L17KickPadHeardTest#playingTheKickPadTicksListenToTheKick
chemin: éditeur Acid, case muette touchée | test: T9L17AcidTapTest#touchingASilentCellPutsANote
chemin: tuto house, conseil de réverbe | test: T9L17SendNoteNamesStripsTest#theHouseReverbNoteNamesTheClapNotTheSnare
chemin: grille rythme, compteur sur deux mesures | test: T9L17GridLineCountsEveryBarTest#cellsLeftHereAndInBar2BothSaid
controle: reporté (mode boost, grande vérification de fin)

Tests du modèle (project-model/src/test, hors des lignes « chemin ») :
- tuto trap Pro, triolets | test: T9L17WholeStepTest#trapsTripletWantsCells13And15
- tuto, crash sur Drop et Drop 2 | test: T9L17WholeStepTest#aCymbalOnTheDropAndTheDrop2WantsBoth
- tuto drill/phonk Pro, « Montre-moi » d'un fill à roulement | test: T9L17DemoShowsTheGestureTest#aFillAskingForARollShowsARoll
- tuto uk_garage Pro, la basse | test: T9L17ChapterDoneNoDottedTest#ukGarageBassChapterDoneLeavesNoDottedNote

## Ce qui change
- F032 (tenté) : les triolets ne valident qu'avec les deux cases demandées ; le crash qu'avec Drop et Drop 2.
- F034 (tenté) : « Montre-moi » d'un fill qui dit « Roulement » pose un vrai roulement.
- F036 (tenté) : « Créer la partie » touché pendant une démo arrête la démo et crée la partie nommée, gardée.
- F039 (tenté) : jouer le pad du kick coche l'étape « écoute le kick ».
- F040 (tenté) : toucher une case muette de l'Acid y pose une note.
- F042 (tenté) : le conseil d'effet nomme les vraies tranches (clap, pas caisse claire).
- F044 (tenté) : un chapitre « Fini » ne laisse plus de note en pointillé.
- F046 (tenté) : la ligne du compteur dit dès le départ les cases des autres mesures.
- F032, test existant : `TripletsTest#theTutorialDetectsAndShowsATriplet` est passé rouge avec F032 (il validait un triolet hors des cases du style) ; il dit maintenant la règle du style (trap : ses cases ; house : n'importe lequel).

## Ce qui n'est pas vert
- F032 synthwave (tom posé sur la ligne « Rim ») : pas corrigé, texte en 15 langues hors de mes fichiers.
- F034 t9-08, t9-38, t9-65 (démo qui pointe une zone vide pour créer une partie) : pas pris ici.
- F046 : une mesure pas encore créée (grille à 1 mesure avant « Passer à 2 mesures ») n'est pas comptée.
- Acid (F040) : choisir une case vide coûte maintenant 2 touches, et 2 touches sur une note la rendent muette ; à faire jouer par un testeur.
- `TutoPadEar.heard` (F039) : lien global vidé seulement à « Quitter » ; il peut passer d'un projet à l'autre.
- F046 : « Mesure 2 · N cases → » à côté de « Encore N cases… » peut être coupé sur un écran étroit.
- F036 : `Crash.shown` rend null quand rien ne change (sans effet visible, à relire).

## Demandes (hors de mes fichiers)
Demande : F032, synthwave, consigne du Perc sur la ligne « Rim » avec un son de tom : changer le texte dans synthwave.json et les 14 tuto_autres.json en même temps.
Demande : F034, t9-08, t9-38, t9-65 : démo de création de partie qui pointe une zone vide, à rejouer dans un lot Structure.

## Captures regardées (écran entier)
- Grille rythme (`rhythm`, `rhythmVelocity`, `rhythmTriplets`, `rhythm_after_wizard`), Acid (`acid`, `acid_editor`, `acid_panel`, `AcidScreenshotTest`) et tuto (`TutoScreenshotTest`, 12 vues) réenregistrées : aucune image de référence ne change. Grille et éditeur Acid regardés en entier : rien ne déborde, rien ne se chevauche.
