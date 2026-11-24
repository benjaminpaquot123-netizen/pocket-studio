# Tour 9 : lot robot-a2 (rapport)

Base : `eq/tour-9-robot1b` à 9d88787a. Branche : `eq/robot-a2` (alias `eq/tour-9-robot-a2`). Fiche : lot A2 de `PLAN-APRES-ROBOT.md` §3. Banc seul (BenStudio Test) : ni l'appli, ni le moteur, ni project-model. Tout est « tenté » (aucun testeur neuf ne l'a encore constaté).

## Points de la fiche (tous « tenté »)

| Point | Ce qui change | Rouge | Vert |
|---|---|---|---|
| 1 | perte du son : une notification baisse le son (la lecture continue), un appel le prend (la lecture s'arrête), puis ▶ le ramène avec du son | f7529666 | 8de119b3 |
| 2 | changement de kit : deux kits posés sur la batterie depuis son nom pendant que le morceau joue, sans arrêt ni coupure | f7529666 | 8de119b3 |
| 3 | endurance (écoute longue, mémoire lue aux deux bouts), fuite mémoire (Mes projets puis « Continuer », en boucle, même morceau, mémoire qui ne monte pas), démarrage froid, arrêt brutal (tempo monté puis relu après relance, remis ensuite), vidéo avec son, tampon large (« Très sûr » puis « Rapide » remis), images lentes à côté d'un écran témoin (Réglages) | 74d9a463 | a238b78b |
| 4 | paquet guidé « Tests guidés » : changer de sortie (le son revient en moins de 2 s, ▶ relancé si le téléphone a arrêté la lecture) et économie d'énergie (la lecture tient) ; jamais dans la nuit ni dans Tout tester | 5e85cdaf | 1ec18547 |
| A (chef) | compteur « Test n sur N » et sa barre sur l'écran du robot pendant un passage | 23225130 | c6921e23 |
| B + C (chef) | écran « Retester les corrections » : les rouges, les non concluants et les tests neufs jamais joués cochés d'office, les verts non ; boutons Tout cocher / Tout décocher ; cochés d'abord (« À refaire »), puis chaque scénario sous son groupe ; un scénario choisi se joue dans le même passage | e9ddb979 | da0a4d22 |
| Relecture (grave 2) | changement de kit : la sonde est lue toutes les 250 ms pendant les gestes (trou entendu même pendant que la feuille des kits est ouverte) ; test tablette remis (grave 1) | a3cd887a | cfd47687 |
| D (chef) | scénarios 2b `changer_partie_10` (10 fois une autre partie par son ▶ sur la Structure pendant la lecture : aucun trou > 200 ms, la partie jouée est la choisie, pas de plantage) et 2c `quitter_en_jouant` (Mes projets pendant la lecture, un autre projet ouvert, puis le premier : pas de son après ■, projet intact sur le disque) ; moins de 3 min chacun, proposés dans Retester | dfbc48e4 | 65144179 |

Les épreuves du point 3 sont dans « Tout tester » ; aucune n'est assez courte pour l'Essai éclair (rien n'y est ajouté).

tests: com.pocketstudio.bentest.*

## Chemins

chemin: perte du son (robot) | test: ScenariosVraisEcransTest#theSoundTakenByAnotherAppComesBackWithPlay
chemin: Batterie, changement de kit (robot) | test: ScenariosVraisEcransTest#twoKitsArePutOnTheDrumsWhileTheSongPlays
chemin: endurance (robot) | test: ScenariosVraisEcransTest#theEnduranceListensThroughAndReadsTheMemory
chemin: Batterie, trou pendant le changement de kit (robot) | test: ScenariosVraisEcransTest#aHoleWhileTheKitIsChangedIsRed
chemin: Mes projets, fuite mémoire (robot) | test: ScenariosVraisEcransTest#theProjectIsOpenedAndClosedAgainAndAgain
chemin: arrêt brutal et démarrage froid (robot) | test: ScenariosVraisEcransTest#theHardStopPutsTheTempoBackWhenItCannotRestart
chemin: Réglages, tampon large (robot) | test: ScenariosVraisEcransTest#theLargeBufferIsListenedThenTheNormalOneComesBack
chemin: images lentes, écran témoin (robot) | test: ScenariosVraisEcransTest#eachScreenIsCountedNextToTheStillOne
chemin: BenStudio Test, compteur (robot) | test: CompteurTest
chemin: BenStudio Test, Retester les corrections (robot) | test: RetestCandidatsTest
chemin: Structure, changer de partie pendant la lecture (robot) | test: ScenariosVraisEcransTest#thePartIsChangedTenTimesWhileTheSongPlays
chemin: Mes projets, quitter pendant la lecture (robot) | test: ScenariosVraisEcransTest#theSongIsLeftWhilePlayingForAnotherProjectThenComesBackIntact
chemin: Tests guidés, sortie (robot) | test: ScenariosVraisEcransTest#theSoundIsBackAfterTheOutputChanged
chemin: Tests guidés, casque débranché (robot) | test: ScenariosVraisEcransTest#anUnpluggedHeadsetStopsThenPlayBringsTheSoundBack
chemin: Tests guidés, économie d'énergie (robot) | test: ScenariosVraisEcransTest#theBatterySaverIsReadOnThenTheSongPlaysThrough

Les vrais écrans (téléphone et tablette, `ScenariosVraisEcransTabletteTest`) passent avec toute la suite `com.pocketstudio.bentest.*`.

## Notes

- Il n'y a pas de tampon 8 dans Réglages : « tampon large » = `buffer_6` (« Très sûr »), puis `buffer_2` remis.
- La page Plus se souvient de sa dernière page : revenir à Réglages ne retouche pas la tuile quand Réglages est déjà ouvert.
- Le kit choisi ne se lit pas depuis le banc : l'oracle lit que le projet a changé et que la feuille des kits est fermée.
- `nouveauMorceau` est injecté (les tests le remplacent) ; l'endurance et la fuite mémoire partent d'un morceau neuf sur le téléphone.
- Arrêt brutal : `relancerStudio` relance l'écran du studio, pas le processus. Un vrai arrêt du processus demande un crochet dans l'appli (hors de ce lot, à confier à un lot appli) ; sans relance possible, l'épreuve dit « non mesuré » et remet le tempo.
- Vidéo avec son, démarrage froid et arrêt brutal : sous Robolectric, leurs oracles sont lus par des tests unitaires ou disent « non mesuré » ; seul le téléphone dit le reste.
- Paquet guidé : bouton « Tests guidés » dans BenStudio Test, passage `guides`, les questions passent par la bannière (`poser`) ; une question sans « oui » = non mesuré, jamais rouge.
- Retester : l'ordre « cochés d'abord » est celui de l'ouverture (une ligne ne saute pas sous le doigt quand on la coche ou la décoche).
- 2b : la partie jouée est lue dans l'écran de l'appli (`sessionUi.shownSongIndex`) ; la Sonde n'a pas de champ « partie » côté moteur. Un tel crochet moteur relève d'un lot appli. Sans lecture possible, le scénario le note, sans rouge.
- 2c : ouvrir un autre projet reconstruit l'activité (`recreate`), ce que la règle Compose de Robolectric ne suit pas ; le faux téléphone du test la simule (projet enregistré, liste fermée, autre projet lu sur le disque). Le téléphone fait la vraie reconstruction. L'autre projet est choisi parmi les lignes visibles de « Mes projets » (liste paresseuse).
- Les commits « rouge: » de ce lot sont des rouges de compilation (épreuves appelées avant d'exister).

## Captures

Aucun écran de l'appli n'est touché (banc seul) : rien à réenregistrer.
