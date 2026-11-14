# Tour 5, lot « jank » : relecture neuve
Relu : f5e4878734c99be1272a314739c099c9b52831ef

Relecture du diff 193fd4d1..f5e48787 (RhythmScreen, MasterLoudness, AppRoot, `rememberChangesOf`, `WholeWordsText` à `ColorProducer`, `PlayingRecomposeTest`).
Tests rejoués ici sur f5e48787 : `*PlayingRecomposeTest`, `*.rhythm.RhythmBeginnerTest`, `*GestureBudgetTest`, `*.nav.SpacesTest` : BUILD SUCCESSFUL, 0 échec. Captures non réenregistrées.

## Constats

| Point | Gravité | Constat | Suite |
|---|---|---|---|
| Lecture périmée (lambda, clés) | aucun | `rememberChangesOf` lit `latest()` (un `rememberUpdatedState`) dans le `snapshotFlow` : un changement de `follow`, `tutoLeft`, `playing`, `padLoop` ou du projet est donc revu au prochain changement d'état lu ; `StepCellPlaying` clé sur `step` et `playStep` ; `DrumPad` lit `lane in live().litPads`, la voie d'un pad ne change pas à place égale. Un pas, un pad ou une page ne peuvent pas rester faux. | rien |
| Retard d'une image | mineur | La première valeur est lue tout de suite ; les suivantes arrivent une image après (écrites par le collecteur). Un pas ou un pad s'allume donc une image plus tard que le moteur : invisible à l'oreille et à l'œil. | noté |
| Fuites, plantage | aucun | Pas d'écouteur ni de coroutine hors composition : `LaunchedEffect(state)` est annulé avec la composition, `snapshotFlow` aussi. `Snapshot.withoutReadObservation` pour la valeur initiale : sans risque. | rien |
| Onglet retiré sous « Mes projets » | mineur | `SaveableStateProvider` quitte la composition : `rememberSaveable` est sauvé (comme un changement d'onglet), le projet vit dans `rhythmVm` (rien de perdu, aucun projet non sauvé touché). L'onglet était déjà reconstruit à la fermeture (clé `coverSeen`). Seul changement : les `onDispose` de l'onglet (`remixVm.stop()` de la page Remix, `generateVm.stopPreview()`, `synthAllOff()`) jouent à l'ouverture des projets au lieu de la fermeture : l'écoute Remix s'arrête sous « Mes projets » au lieu de continuer sous la fenêtre. Voulu, plutôt un gain. | noté |
| Le test prouve-t-il quelque chose | aucun | Compte les portées recomposées de toute la fenêtre pendant 60 lectures ; avant (96b8934d, commit rouge) 2296 sur Sons et 1934 sur Mix contre des bornes de 400 et 450 ; après 253 et 374. Le test Mes projets exige aussi 0 `timeline_screen` dessous (rouge sur l'ancien code) et le retour sur la même page. Bornes un peu larges (marge ×1,6 sur Sons, ×1,2 sur Mix) mais très en dessous de l'ancien. Je n'ai pas rejoué le rouge sur le parent. | rien |
| Aspect visuel | aucun | Les captures de lecture (`currentStep = 5`, `litPads`) ne changent pas, selon le rapport ; le halo lu au dessin donne les mêmes couleurs (`lerp` identiques, bande et nom lus au dessin). `Acid 303` garde `WithLive` : hors lot. | rien |

Aucun constat bloquant ni grave.

Verdict : 0 bloquant, 0 grave — fusion possible
