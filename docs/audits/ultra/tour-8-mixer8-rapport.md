# Tour 8, lot mixer8 : rapport

Correcteur, branche `eq/tour-8-mixer8` (base 2af35df0, contient f6c6d7c7f). Zone : le Mixer (`mixer/MixerScreen.kt`, `AppRoot.mixerStrips`).

tests: :app:testDebugUnitTest --tests '*mixer*' --tests '*MixerFitState*' --tests '*MixerMeters*' --tests '*HumanRemixJourney*' --tests '*KeyActionsGesture*'

chemin: Mixer, téléphone tourné couché puis debout (tiroir de tranche ouvert, console défilée, Effets du master ouverts) | test: Tour8MixerRotationTest#turningThePhoneThereAndBackKeepsTheMixersState
chemin: Mixer, téléphone tourné pendant qu'un remix se crée (pistes du remix sous la console) | test: Tour8MixerRotationTest#turningThePhoneWhileARemixIsMadeKeepsTheMixersState
chemin: Mixer, un fader glissé pendant 60 images | test: Tour8MixerRotationTest#aFaderDragComposesTheConsoleOncePerFrame
chemin: Mixer puis Sons, première piste vide supprimée : mêmes pistes | test: Tour8MixerNamesTest#theDeletedFirstTrackIsNotAStripEither
chemin: Mixer d'un projet neuf : la première piste reste | test: Tour8MixerNamesTest#aNewProjectStillShowsItsFirstTrack
chemin: Tuto en cours, 1re piste vide cachée : Mixer puis Sons, mêmes pistes | test: Tour8MixerNamesTest#aTutorialHidingTheFirstTrackHidesItsStripToo

corrige: U27 | gravité: mineur | rejeu impossible : défaut mineur au tri du tour 6 (verdicts.tsv l.68), preuve par Tour8MixerNamesTest

## Ce qui a changé
- D7/U27 (avec l'accord du chef, une ligne hors liste : AppRoot.kt l.3330, `mixerStrips(project, hideFirst = tutoHidesFirst)`) : `mixerStrips` suit la règle de la page Sons : la première piste vide cachée (supprimée, ou cachée par le tuto) n'a plus de tranche. Aucun ordre changé, aucun nom changé ; `HumanRemixJourneyTest` et `KeyActionsGestureTest` non touchés.
- Rotation : les deux volets du Mixer sont des contenus déplaçables (`movableContentWithReceiverOf`) ; en passant d'une colonne à deux, le tiroir d'une tranche, le défilement de la console et les Effets du master restent tels quels.
- Coût : la sonde qui mesure la hauteur des faders lit les canaux au repos, sans vu-mètres ni actions ; un fader glissé ne recompose plus les sondes invisibles.

## Laissés de côté
- Le défilement vertical de la page lors du passage une colonne ↔ deux colonnes : il vit dans `AdaptivePanes` (`ui-components/Adaptive.kt`), hors de ma liste.
- « Vocals » des Sons (la page micro) n'a pas de tranche au Mixer tant qu'aucune prise n'existe : pas de nouvelle tranche vide créée (ce serait un nouveau texte et un nouvel ordre).

- Preuve locale (2026-11-18) : sur le code d'avant la correction, les deux tests de rotation sont rouges (« lying down: the strip's sheet closed »), verts après. Le test du fader glissé reste vert sur l'ancien code : la mémoïsation de la sonde n'est prouvée par aucun test (mineur).
- Sous Robolectric, un vrai toucher sur « ⋯ » après défilement de la page laissait le tiroir fermé ; le test ouvre par l'action de clic. Cause non établie, à faire constater sur téléphone (recette ci-dessous).

## Recette pour le téléphone de Benjamin
Mixer, ouvrir « ⋯ » d'une tranche, faire glisser la console, ouvrir « Effets » du master, tourner le téléphone puis le remettre droit : tout reste ouvert et à la même place.
