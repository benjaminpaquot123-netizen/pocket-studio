# Tour 9, lot miettesB (restes de L2 et L7, F111, L3 F031 et F041) : rapport

Branche : eq/tour-9-miettesB, partie de l'intégration ccr-80af1a1b-mn0wec (8801b1dc).
Rôle : correcteur. Fichiers occupés par d'autres lots (L13, L15, L3) et fichiers du tuto : pas touchés.

## Corrigé
- Reste de L7 (même cause que F116, sièges t9-58, t9-69) : « La feuille des couches d'une piste nomme le son que j'ai choisi, même après un bouton tourné, comme l'onglet de la piste ; jamais « Synth » ou « Plaits » à la place. »
  La feuille des couches (guide/TrackSheet.kt) prenait encore le nom du moteur ; elle prend maintenant le même nom que l'onglet (`trackSoundName`, L7). Rouge puis vert : `Tour9LayersOwnSoundNameTest` (« expected Lead but was Synthé » avant le correctif).

chemin: Sons > piste mélodique > Couches (feuille des couches) | test: Tour9LayersOwnSoundNameTest#theLayersEditorNamesTheChosenSoundLikeTheTab
chemin: Sons > piste neuve > Couches | test: Tour9LayersOwnSoundNameTest#aFreshTracksSoundIsNamedAsBefore

corrige: reste L7 (TrackSheet soundName) | gravité: mineur

## Déjà fait, rien à changer
- F111 (« Le message « nouvelle longueur » disparaît après quelques secondes ou au geste suivant. ») : corrigé par L12, entré. `Tour9DoneBarGoesTest` et `DoneBarNextTouchTest` verts sur la base de ce lot. Reste « tenté » jusqu'au constat d'un testeur neuf.
- Reste de L7, aide RECORD_KEYS de la barre d'enregistrement : ce « ? » est celui de la prise et parle d'enregistrer, à juste titre ; le « ? » du Clavier (F117, L7) explique jouer, gamme et accords. Pas un défaut.

## Sautés (au chef)
- Reste de L2, F136 côté AppRoot (passer `trackName` aux deux appels d'`EffectsCard` et fournir `LocalFxTrackName` à la boîte « Effets · nom ») : AppRoot.kt est ouvert dans L15. À faire après L15.
- F031 (force des coups doux) : le remède est dans le texte du tuto (tutos/*.json) ou dans TutoChecks.kt (L3) ; fichiers du tuto interdits à ce lot. Option recommandée au chef : texte Facile sans coups doux.
- F041 (cases guides avant « Passer à 2 mesures ») : RhythmScreen.kt (L15) et TutoChecks.kt (L3) occupés ; la phrase contredit la décision du tour 6 (`Tour6TutoCellsPanel100Test`, vert). Option recommandée au chef : garder le choix du tour 6 et retirer F041 de la liste.
- Autres restes de L7 (D06 à 200 % sur 360 dp, alignement Pro à 200 %, boucle « Eu faço », TutoJourneyTest capricieux) : TutoPanel.kt, TutoController.kt, TutoChecks.kt, tous du tuto et ouverts ailleurs.

tests: *Tour9LayersOwnSoundNameTest* *Tour9RollFinishTest* *Tour9DoneBarGoesTest* *DoneBarNextTouchTest* *Tour6TutoCellsPanel100Test*
