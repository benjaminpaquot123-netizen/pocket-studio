# Tour 9 : lot cat-400 (rapport)

Base : `eq/tour-9-bentest-final`. Branche : `eq/tour-9-cat-400`. Fiche : catalogue BenStudio Test porté à environ 400 scénarios utiles, chacun avec son oracle, sans doublon, en parties de 50 au plus sous `DUREE_COMPLET_MAX_S`. Banc seul (`app/src/benTest`, tests `bentest`) : ni l'appli, ni le moteur, ni les packs, ni le tuto, ni la CI. Tout est « tenté » (aucun testeur neuf ne l'a encore constaté).

## Ce qui est fait

- Catalogue : 410 scénarios (thèmes sauve, mix, effets, limites, écrans), coupé en 9 parties de 50 au plus (`TAILLE_PARTIE = 50`), chaque partie sous son plafond (test `theCatalogueReachesAboutFourHundredInPartsOfFiftyUnderTheHourEach`).
- Fusion des lots mix (40), effets (89) et limites (72) ; thème écrans ramené à 60 ; écoutes d'une minute réparties entre les scénarios courts.
- Doublons retirés : `lim_refaire_break` (déjà joué par `lim_refaire_lecture_break`), la paire d'écrans mix/structure, le fond « structure ».
- Robustesse : un effet ou un curseur est remis même quand un contrôle échoue (`finally`) ; une remise ratée laisse une note au carnet ; la pile de parties se défait selon les parties réellement présentes ; le pilote défile de nouveau quand un long glissé s'arrête avant la cible (rouge 88f82a77 → vert ff60701b).

tests: --tests 'com.pocketstudio.bentest.*'

## Chemins

chemin: catalogue entier, parties de 50 sous le plafond | test: ScenariosEcransTest#theCatalogueReachesAboutFourHundredInPartsOfFiftyUnderTheHourEach
chemin: écrans et écoutes, onglets et styles connus | test: ScenariosEcransTest#eachScreenLineNamesKnownTabsAndEachListenAKnownStyle
chemin: limites, ids uniques et gestes connus | test: ScenariosLimitesTest#theCatalogueHasItsSeventyThreeScenariosWithUniqueIds
chemin: Structure, chaque geste annulé, rétabli, laissé comme avant | test: ScenariosLimitesVraisEcransTest#eachStructureGestureIsUndoneRedoneAndLeftAsItWas
chemin: Structure, gestes annulés pendant la lecture | test: ScenariosLimitesVraisEcransTest#theGesturesAreUndoneAndRedoneWhileTheSongPlays
chemin: tempo aux bouts, saisi, tenu, remis | test: ScenariosLimitesVraisEcransTest#theLowestAndHighestTemposAreTypedHeldAndPutBack
chemin: Mix, chaque curseur aux deux bouts puis remis | test: ScenariosTest#theMixCatalogueSetsEachSliderToBothEndsSavesItThenPutsItBack
chemin: Mix, effets réglés, retirés, annulés (catalogue fx) | test: ScenariosVraisEcransTest#theEffectsCatalogueSetsKnobsRemovesUndoesAndLeavesTheStrips

Points vus, hors banc (à trier) : en w360, le bandeau « done_undo » couvre le ⋮ d'une partie en bas de liste ; `effet_chorus` de ScenariosTest est instable par moments.
