# Tour 6, lot 2 « tuto-consignes » : rapport de l'auteur

Branche `ccr-6f42e003-tutoconsignes`, partie de `ccr-80af1a1b-mn0wec` avec le lot langues (`ccr-6f42e003-295715`) et le lot gaucher (`ccr-4cc0bb93-xljmti`) fusionnés d'abord, puis la tête de l'intégration fusionnée avant la relecture. Fait par l'orchestrateur et quatre équipes sur des fichiers séparés (contenu et vérifications ; panneau et déroulé ; restes de parcours ; en-tête du panneau), fusionnées ici. Décisions : `docs/decisions/tour-6-tutoconsignes.md` (une ligne par choix, toutes équipes).

tests: *Tour6Tuto* *Tour6Plai* *TutoJourneyTest *TutoTest *TutoLot4Test *TutoPanelFitsPhoneLandTest *TutoKickSoundTest

Tests du module `project-model` (le portail ne rejoue que l'appli : `:project-model:test --tests '*Tuto*' --tests '*I18nCatalogTest' --tests '*LexiconWordsTest'`) : `Tour6TutoSwingTest`, `Tour6TutoSoftHitTest`, `Tour6TutoVariationTargetTest`, `Tour6TutoActionsTest`, `Tour6TutoRumbleTest`, `Tour6TutoSumTest`, `Tour6TutoTabNamesTest`, `Tour6TutoYourTurnTest`, `Tour6TutoKeyStepTest`, `Tour6TutoExampleTest`, `Tour6TutoForeignWordsTest`, `Tour6TutoMixWordsTest`, `Tour6TutoBarNameTest`, `Tour6TutoShowLengthTest`, et `TutoLot4Test` mis à jour.

## Les 18 défauts du lot, et les ajouts

| Défaut | Fait | Test (rouge puis vert) |
|---|---|---|
| T5, T17, N27 swing | la règle lit le pourcentage du curseur à ±3 points ; 13 styles écrivent ce nombre (house 33 %, deep house et UK garage 40 %), la démo pose la même valeur | `Tour6TutoSwingTest` |
| T3 coup doux, 2 mesures | un coup « doux » posé à 100 % ne valide pas ; le parcours joue « Doux » par l'appui long ; le clap techno ne parle plus de 2 mesures | `Tour6TutoSoftHitTest` (les deux) |
| T2 pad nommé absent (en partie) | consignes : les pads cités entre guillemets (Perc → Rim, Ride/Crash → Cymbale, Tom → Tom médium, Maracas → Shaker) ; panneau : quand une phrase ne cite qu'une chose, le nom de pad cité par la traduction (ja, ko) devient le nom affiché ; jamais quand elle en cite plusieurs (relecture, grave : l'ordre des mots change d'une langue à l'autre) | `Tour6TutoPadNamesTest`, `Tour6TutoPanelPadQuotesTest`, `Tour6TutoPanelQuoteOrderTest` |
| T6 mots non expliqués | puces : mesure, temps, accord, riff, stab, pad, effet, gamme, tonalité, dorien, boucle, 909, 707, swing, break | `Tour6PlainWordsTest` |
| N12 onglets | « Mon morceau » en Facile, « Structure » en Pro, jump up compris | `Tour6TutoTabNamesTest` |
| T7 kit | pas de carte du kit à l'étape qui dit le kit prêt | `Tour6TutoPanelKitReadyTest` |
| T8 trop d'actions | 27 consignes découpées en deux étapes (accords de 21 styles, basses, mélodie lofi) ; « Montre-moi » de la première moitié ne pose que ses mesures | `Tour6TutoActionsTest`, `Tour6TutoShowLengthTest` |
| T9 chapitre coché | la liste compte comme la fin et le Bravo | `Tour6TutoPanelProListTest` |
| N10 Bravo du chapitre 9 | « C'est fait » sur la dernière étape montre le Bravo | `Tour6TutoPanelManualEndTest` |
| T14 grondement | la consigne dit les cases une fois et nomme le pad | `Tour6TutoRumbleTest` |
| T19 écoute avant d'écrire | « de l'exemple » dans la consigne | `Tour6TutoExampleTest` |
| N9 calcul sans mots | plus de « 16 + 8 » | `Tour6TutoSumTest` |
| T16 mots anglais | 114 titres de chapitre traduits (hi, ar, zh-CN, ja, ko) ; « Groove » en ja/ko ; « Autres synthés » au lieu de « Plaits » en Mode simple | `Tour6TutoForeignWordsTest` (deux), `Tour6PlaitsWordTest` |
| N2 « À toi » sans verbe | techno, house et textes Pro partagés | `Tour6TutoYourTurnTest` |
| N3 tonalité | « « Je le fais » ouvre la fiche Tonalité » | `Tour6TutoKeyStepTest` |
| N6 cible des variations | `target` dans les 10 étapes ; le panneau ouvre ce pad | `Tour6TutoVariationTargetTest`, `Tour6TutoPanelVariationPadTest` |
| N13 Fermer le tuto | bouton encadré (et « Revenir à l'étape ») | `Tour6TutoPanelCloseTest` |
| N19 (grave) écoutes passées | « Suivant » sur une écoute la compte : plus de piège en fin de tuto | `Tour6TutoPanelListenSkipTest` |
| N20 déjà fait | « Déjà fait ! » et pourquoi | `Tour6TutoPanelAlreadyTest` (deux) |
| N22 chapitres | « Chapitres » écrit sous l'icône de liste, sans pousser le titre | `Tour6TutoPanelChaptersTest`, `Tour6TutoPanelFitHeaderTest` (deux) |
| N23 en-tête après Bravo | « Chapitre 7/15 · Fait » | `Tour6TutoPanelHeaderTest` |
| N24 mot sous le curseur | « Sous le curseur, « Normal » devient « −2 »… » | `Tour6TutoMixWordsTest` |
| N26 partie créée | « (copie du Groove) » quand l'étape en retire | `Tour6TutoPanelPartMadeTest` |
| N28 « Takt 4 » | « dernière mesure » | `Tour6TutoBarNameTest` |
| N29, N30 plan du morceau | nom et nombre mesurés comme dessinés : les deux, le nombre seul, ou rien | `Tour6TutoPanelMapNamesTest`, `Tour6TutoPanelMapBarsTest` |
| N31 Lire la suite sur Pourquoi (et TalkBack) | « Pourquoi ? » puis « Lire la suite » sous le texte, jamais coupés ; aucun vide quand le texte tient | `Tour6TutoPanelMoreWordsTest`, `Tour6TutoPanelFitWhyTest` (trois), `Tour6TutoPanelFitRoomTest` (deux), `Tour6TutoPanelReadOnSaidTest` |

Laissés de côté :
- N21 : les textes du tuto sont cohérents ; le mot divergent est dans `i18n/ja/audit1z56.json`, `i18n/de/audit1z56.json` et `ultra3z5.json`, hors des fichiers du lot (lot 5).
- T8 pour 4 consignes de jump up (`bass_do`, `pad_do`, `make_drop2`) : `TutoLot5Test` et `TutoLot4Test` (project-model) attendent « Copier » dans la même étape ; à découper quand ces tests seront adaptés.
- N2 et T19 : 151 et 46 consignes d'autres styles restent dans les listes d'attente des tests (`PENDING_VERB`, `PENDING_EXAMPLE`), qui ne peuvent que rétrécir.
- T16 : les textes des étapes (pas les titres) gardent des mots latins en hindi et en arabe : à reprendre dans un lot de traduction.
- T2 : des consignes nomment encore sans guillemets des pads absents du kit intégré (« pad du bois » afro house, « pad du tambourin » disco, « pad de la güira » et « pad des timbales » reggaeton) : non couvert par le test, à reprendre. Avec le kit d'un style téléchargé, un pad peut afficher le nom du son du pack (non vérifiable ici) ; le nom donné au pad au téléchargement est au lot 6.

- T3 : un coup doux posé fort laisse l'étape non cochée sans rien dire (le geste « Doux » est joué pour de vrai par le parcours de test) ; un message serait mieux.
- N20 : « Déjà fait ! » aussi pour un geste fait juste avant de quitter le tuto et pas encore coché ; aucun test ne vérifie qu'un vrai geste dit « Bravo » et pas « Déjà fait ».
- N19 : les écoutes passées avant cette version restent dues à la fin du tuto.

## Captures regardées
`verifyRoborazziDebug` sur tous les tests de captures après la fusion de l'intégration : 19 écarts. Les 10 du tuto viennent du lot et sont regardées (images de comparaison) puis enregistrées : « Chapitres » sous l'icône de liste (le chapeau décoratif retiré, le titre garde une ligne à 100 %), « ▸ Pourquoi ? » puis « Lire la suite » sous le texte, jamais coupés, aucun vide quand le texte tient, plan du morceau lisible, boutons encadrés de la carte de fin, panneau pas plus haut qu'avant (Facile et Pro, 100 % et 130 % de droite à gauche). Les 9 autres (`nl_nav_*`, `fr_guide_simple`, `fr_simple_mix`, `folded_cards`) ne viennent pas du lot (coche « ✓ » sur le dossier, livrée par l'intégration) : pas réenregistrées ici. Après la dernière fusion de l'intégration, `tuto_app_bass` change encore (grille défilée vers le haut, puce « Basse en fantôme » allumée : venu d'un autre lot) ; enregistrée ; à voir côté lot 1 : la consigne y dit « plus haut ↑ » alors que les notes en pointillé sont visibles. Une première version du panneau élargissait l'en-tête et gardait un vide sous « Pourquoi ? » : vue sur ces captures, corrigée (`Tour6TutoPanelFit*`).

## Chemins
chemin: tuto Facile et Pro › étape à écouter passée par Suivant | test: Tour6TutoPanelListenSkipTest#suivantOnEveryExampleLeavesNoneForTheEnd
chemin: tuto Pro › liste des chapitres | test: Tour6TutoPanelProListTest#aProChapterFinishedWithItsExampleUnheardIsTicked
chemin: tuto Facile › dernière étape à cocher d'un chapitre | test: Tour6TutoPanelManualEndTest#aChaptersLastStepTickedByHandHasItsBravo
chemin: tuto › étape kit prêt | test: Tour6TutoPanelKitReadyTest#theStepThatSaysTheKitIsReadyDoesNotOfferIt
chemin: tuto › carte de fin | test: Tour6TutoPanelCloseTest#closeTheTutorialIsADrawnButton
chemin: tuto japonais › pad cité | test: Tour6TutoPanelPadQuotesTest#japanese
chemin: tuto coréen › pad cité | test: Tour6TutoPanelPadQuotesTest#korean
chemin: tuto › consigne qui cite un pad | test: Tour6TutoPadNamesTest#aPadNamedIsAPadShown
chemin: tuto › en-tête après Bravo | test: Tour6TutoPanelHeaderTest#afterTheChaptersBravoTheHeaderSaysNoStepLeft
chemin: tuto › étape déjà faite | test: Tour6TutoPanelAlreadyTest#aStepTheProjectHeldAlreadyIsSaidAlreadyDone
chemin: tuto › panneau Déjà fait | test: Tour6TutoPanelAlreadyTest#thePanelSaysAlreadyDone
chemin: tuto › partie créée sans tout le Groove | test: Tour6TutoPanelPartMadeTest#aPartThatLeavesBricksOutIsNotSaidTheWholeGroove
chemin: tuto › panneau replié Chapitres | test: Tour6TutoPanelChaptersTest#theChaptersAreNamedOnTheFoldedPanel
chemin: tuto › en-tête sur téléphone 400 dp | test: Tour6TutoPanelFitHeaderTest#theTitleKeepsItsRoomOnA400dpPhone
chemin: tuto › en-tête sur téléphone 360 dp | test: Tour6TutoPanelFitHeaderTest#theTitleKeepsItsRoomOnA360dpPhone
chemin: tuto › Pourquoi et Lire la suite | test: Tour6TutoPanelMoreWordsTest#whyIsNeverUnderReadOn
chemin: tuto › Pourquoi sur téléphone 360 dp | test: Tour6TutoPanelFitWhyTest#whyIsNeverCutOnA360dpPhone
chemin: tuto › Pourquoi sur téléphone 400 dp | test: Tour6TutoPanelFitWhyTest#whyIsNeverCutOnA400dpPhone
chemin: tuto › Pourquoi à 130 % de droite à gauche | test: Tour6TutoPanelFitWhyTest#whyIsNeverCutWithTheInterfaceAt130PercentRightToLeft
chemin: tuto › texte qui tient en 360 dp | test: Tour6TutoPanelFitRoomTest#noEmptyRoomUnderTheWordsOnA360dpPhone
chemin: tuto › texte qui tient en 400 dp | test: Tour6TutoPanelFitRoomTest#noEmptyRoomUnderTheWordsOnA400dpPhone
chemin: tuto › texte lu jusqu'au bout avec TalkBack | test: Tour6TutoPanelReadOnSaidTest#readOnIsNotSaidOnceTheWordsAreRead
chemin: tuto › consigne qui cite plusieurs choses traduite | test: Tour6TutoPanelQuoteOrderTest#noQuoteIsTakenForAnotherInAConsigneQuotingSeveralThings
chemin: tuto › plan du morceau noms | test: Tour6TutoPanelMapNamesTest#everyPartsNameFitsItsBlock
chemin: tuto › plan du morceau mesures | test: Tour6TutoPanelMapBarsTest#everyBarsNumberFitsItsBlock
chemin: tuto › variation de batterie sur son pad | test: Tour6TutoPanelVariationPadTest#aDrumVariationStepHasItsPad
chemin: tuto › puces d'explication | test: Tour6PlainWordsTest#everyWordOfTheTradeInATutoStepHasItsPlainLine
chemin: tuto › puces sans mot courant | test: Tour6PlainWordsTest#aCommonWordAloneIsNotExplained
chemin: fiche de piste › son d'une couche en Mode simple | test: Tour6PlaitsWordTest#theModeSimpleSaysOtherSynthsInItsLanguage
