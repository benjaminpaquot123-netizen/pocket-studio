# Tour 9 : lot cat-effets (rapport)

Base : `eq/tour-9-robot-a2` à 50014995. Branche : `eq/tour-9-cat-effets`. Fiche : catalogue BenStudio Test, thème Effets (89 scénarios `fx_`). Banc seul (BenStudio Test) : ni l'appli, ni le moteur, ni project-model. Tout est « tenté » (aucun testeur neuf ne l'a encore constaté).

## Ce qui est ajouté (tous « tenté »)

| Famille | Nombre | Niveau | Oracle | Rouge | Vert |
|---|---|---|---|---|---|
| `fx_bouts_<type>` | 29 | pro | chaque réglage de l'effet à 0 puis à 1 par ses curseurs ; projet et disque ; joué aux deux bouts (son, pas de NaN, lecture qui tient) ; retiré par sa croix, piste comme avant sur le disque | f5daec0a | 87855597 |
| `fx_retrait_<type>` | 29 | pro | retiré par sa croix pendant la lecture (son, pas de NaN, la lecture continue) ; « Annuler » le remet, « Rétablir » le retire, chaque fois sur le disque | f5daec0a | 87855597 |
| `fx_paire_<a>_<b>` | 28 | mesure | deux effets lourds sur une piste en lecture : 0 décrochage, son, pas de NaN ; retirés par « Annuler » | f5daec0a | 87855597 |
| `fx_chaine_1..3` | 3 | mesure | quatre effets lourds sur une piste : mêmes oracles | f5daec0a | 87855597 |

Total du catalogue : 133 + 89 = 222 scénarios ; les `fx_` ne vont que dans le paquet complet (pas dans l'Essai éclair), 29 min 16 s estimées, sous leur plafond de 30 min.
Chaque scénario part d'un projet propre (remise à zéro du catalogue) et laisse les pistes comme il les a trouvées ; une piste pleine passe à la suivante ; sans place pour un effet long (limite de l'appli), « non mesuré ».

Hors de la fiche, un seul réglage de test : `ScenariosVraisEcransTest` écoute 1,5 s au lieu de 1 s dans ses scénarios d'effets (à 1 s, une seconde de lectures toutes les 250 ms en tient parfois 3 et le scénario entend « 0 s » : deux faux rouges vus ici sur des tests existants).

tests: --tests '*ScenariosEffetsTest*' --tests '*ScenariosTest*' --tests '*ScenariosVraisEcransTest*'

## Chemins

chemin: Mix, bandeau d'une piste, effets (catalogue fx, robot) | test: ScenariosVraisEcransTest#theEffectsCatalogueSetsKnobsRemovesUndoesAndLeavesTheStrips
chemin: Mix, réglages d'un effet à 0 et à 1 (studio simulé) | test: ScenariosEffetsTest#aKnobThatDoesNotTakeItsValueASilentEndAndABrokenRemoveAreRed
chemin: Mix, liste du catalogue fx (ids, durée, paquet) | test: ScenariosEffetsTest#theCatalogueCoversEveryEffectHasAWorkAndStaysUnderHalfAnHour
chemin: Mix, effet retiré en jouant, Annuler, Rétablir (studio simulé) | test: ScenariosEffetsTest#aRemovalThatStopsTheSongOrThatUndoCannotBringBackIsRed
chemin: Mix, paires et chaînes d'effets lourds (studio simulé) | test: ScenariosEffetsTest#dropoutsNanAndAnUndoThatLeavesThemAreRedNoUndoIsNotMeasured
