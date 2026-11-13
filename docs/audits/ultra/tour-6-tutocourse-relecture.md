# Tour 6 — lot « tutocourse » : relecture neuve
Relu : 85e26da67ca982bbb5135df81fca1ef8d6266078

Relecture par un sous-agent neuf qui n'a pas écrit le lot : diff `326fadd4..85e26da6` (`TutoController.check()`, test neuf `Tour6TutoLateAnswerTest`, rapport, décisions), lu avec `init` (collecteur `project.debounce`), `start`/`show`, `goTo`/`move`, `save()`, `tick()`, `fulfilNeed()`, la fin de « Montre-moi », et le branchement réel (`AppRoot` → `RhythmViewModel.tutoEdit`, `project = session.project`).

## Bloquant
Aucun.

## Grave
Aucun.
- Un regard ignoré laisse-t-il l'interface sans suite ? Non. Il n'est ignoré que si `project.value` a changé (ni identique ni égal) depuis son départ ; or tout changement passe par le `StateFlow` que suit le collecteur `project.debounce(...)` : après 250 ms de calme il reçoit la dernière valeur, et ce regard-là porte sur `project.value` (jamais ignoré, sauf nouveau changement, qui en appelle un autre). Les regards lancés par `show`/`start`, `save` (arrivée sur une étape, `goTo`, `move`), `fulfilNeed`, l'écoute et la fin de « Montre-moi » ne sont que des regards en plus : s'ils tombent, celui du collecteur suit. `tutoEdit` met le projet à jour tout de suite (`session.edit` synchrone) : `fulfilNeed` regarde bien le projet qu'il vient de faire. Pas d'autre source qui change le projet en boucle pendant la lecture (la partie entendue est tenue à part, `heardScene`).
- « Bravo » perdu ? Non. Un regard qui aurait coché l'étape montrée et qui tombe est suivi par celui du projet actuel, qui la coche avec `bravo` si elle tient encore ; si le changement l'a défaite, pas de « Bravo », c'est juste.
- « Déjà fait » (`arrived == p`) : un regard d'arrivée qui tombe parce que le projet a changé entre-temps cède à un regard sur un projet différent de `arrived` : « Bravo » au lieu de « Déjà fait », ce qui est juste puisque le projet a bougé. Le moyen 1 de la relecture tutoordre2 (regard parti sur l'étape d'avant, répondu après l'arrivée) est réglé par ce lot : ce regard-là tombe désormais, et le regard d'arrivée (`p == arrived`) met « Déjà fait ».
- « Montre-moi » : inchangé (retour en tête de `check()` pendant la démo, puis un regard relancé à sa fin sur `project.value`).
- Étape « changed:… » : `base()` est pris au départ du regard ; ignorer un regard ne touche ni `baseline` ni `arrived`.
- Besoin : quand l'étape montrée est dans `found`, `tick()` → `save()` recalcule `need` sur `project.value` (égal à `p` grâce à la garde) : même résultat qu'avant. Quand d'autres étapes sont cochées sans elle, `need` suit maintenant le projet (avant : figé) — c'est le but.
- Texte d'ordre sous une étape cochée : plus jamais (`u.current.id !in _ui.value?.progress?.done`), ce qui règle le moyen 2 de tutoordre2 ; il est dit aussi quand le regard coche d'autres étapes (les durées) sans l'étape montrée.
- Aucun test relancé ne change de couleur ; sauvegarde (toujours `tick()` → `save()`, même contenu) et son non touchés.

## Moyen
- Le test peut passer vert à tort sur une machine très chargée : la dernière assertion ne vérifie pas que le regard retenu a bien répondu (attente bornée par `idle()`, environ 275 ms) ; s'il répond plus tard, l'assertion lit encore « Copier » sans la correction. Il ne peut pas rougir à tort. Parade pour un lot suivant : compter dans `Late` les blocs finis et attendre ce compte avant l'assertion. Ici il rougit bien sur le parent (voir plus bas).

## Mineur
- Coût de `p != project.value` : égalité profonde d'un `Project` sur le fil principal, seulement quand les deux ne sont pas le même objet (projet changé pendant le regard, cas rare) ; du même ordre que la comparaison que fait déjà le `StateFlow` à chaque modification et que la ligne `arrived == p`.
- Cas étroit : projet p1 → p2 → égal à p1 en moins de 250 ms (annuler aussitôt) : le collecteur peut ne rien recevoir (le `StateFlow` saute une valeur égale à la dernière vue) et le regard du bouton sur p2 tombe ; l'interface garde l'état calculé pour p1, qui est celui du projet : rien de faux.
- Le rapport dit « un regard plus récent suit toujours » : vrai par le collecteur, sauf pendant « Montre-moi » (sa fin relance un regard) ; à préciser dans un prochain rapport, sans effet sur le code.

## Relancé et vu
- `scripts/bench/gradle-file.sh :app:testDebugUnitTest --no-configuration-cache --rerun --tests …Tour6TutoLateAnswerTest --tests …Tour6TutoOrderRaceTest --tests …TutoLot5ControllerTest --tests …Tour6TutoPanelAlreadyTest --tests …Tour6TutoFlechesRollTest` sur `85e26da6` : BUILD SUCCESSFUL ; LateAnswer 1/1, OrderRace 1/1, Lot5Controller 5/5, PanelAlready 3/3, FlechesRoll 6/6, 0 échec (résultats horodatés du passage).
- rouge constaté : 5838dd92 : rejoué sur ea3883b2 : `TutoController.kt` de ea3883b2 remis seul, `Tour6TutoLateAnswerTest#aLateLookNeverTakesBackWhatANewerOneOffered` échoue à la dernière assertion, « a late look took « Copier » back expected:<Copy(role=bass)> but was:<null> » (le regard retenu a bien répondu et repris « Copier ») ; fichier remis à HEAD ensuite, rien d'autre de modifié.
- Le test prouve la course sans pause fixe pour l'ordre : le regard du bouton est retenu par un verrou (`CountDownLatch`) jusqu'après la réponse du regard sur le motif ; seules les attentes de fin sont bornées (moyen ci-dessus).

Verdict : 0 bloquant, 0 grave — fusion possible
