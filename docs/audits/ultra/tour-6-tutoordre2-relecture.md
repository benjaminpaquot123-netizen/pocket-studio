# Tour 6 — lot « tutoordre2 » : relecture neuve
Relu : 7193704ec4ea7b76135d52ab611e545a42578039

Relecture par un sous-agent neuf qui n'a pas écrit le lot : diff `2e29fdd7..7193704e` (trois lignes dans `TutoController.check()`, un test neuf `Tour6TutoOrderRaceTest`, rapport, décisions), lu avec `save()`, `tick()`, `check()` en entier et l'affichage de `made` / `already` dans `TutoPanel`.

## Bloquant
Aucun.

## Grave
Aucun.
- Diagnostic : juste. `check()` fige `u` avant `withContext(worker)` et filtre sur `u.progress.done` du départ. Un regard parti avant qu'un regard plus ancien ne réponde retrouve les étapes que celui-ci vient de cocher ; avant le lot, `found` n'était donc pas vide, `tick()` ne faisait rien (« Nothing new ») et le bloc « ce qui manque à l'ordre » (`found.isEmpty()`) était sauté. Rien dans la ligne du lot « tutoordre » (`_ui.value?.current?.id in found`, réservée à « Déjà fait ») ne peut empêcher ce message : c'est bien une course voisine, pas une régression de tutoordre. En production le `worker` est `Dispatchers.Default` (plusieurs fils) : la course existe aussi sur le téléphone, pas seulement sur une CI chargée.
- « Bravo » perdu ? Non. Retirer de `found` ce qui est déjà coché ne change rien à `tick()` : ces ids y étaient de toute façon sans effet (`done == u.progress.done` → retour). Le regard qui coche vraiment l'étape montrée garde son `bravo`.
- Texte d'ordre quand l'étape courante est cochée : le bloc ne parle que si `wanted != have` sur le projet regardé ; une étape « song: » cochée l'a été sur un ordre juste, donc rien n'est dit pour un projet à jour (voir Moyen pour un projet en retard).
- Aucun test existant ne change de couleur parmi ceux relancés ; ni sauvegarde (toujours par `tick()` → `save()`, même contenu) ni son touchés.

## Moyen
- « Déjà fait » remplacé par « Bravo » dans un cas étroit : un regard A parti sur l'étape d'avant (projet p_A), le projet change encore (p'), l'utilisateur arrive sur l'étape (arrivé = p'), le regard d'arrivée B (p') part avant que A réponde. A coche l'étape mais `arrived != p_A` : pas de « Déjà fait » ; avant le lot, B (p' == arrivé, étape dans `found`) le mettait ; maintenant l'étape est retirée de `found` et rien ne le met. Effet : « Bravo » pour un geste jamais fait, aucun geste en plus, pas de blocage. Parade simple pour un lot suivant : tester `_ui.value?.current?.id in seen` (et non `found`) sur la ligne « Déjà fait », la garde `arrived == p` suffisant à la rendre vraie.
- Course ancienne un peu élargie : avec `Dispatchers.Default`, un regard plus ancien (ordre encore faux) peut répondre après celui qui a coché « Mets les parties dans l'ordre » ; si tout ce qu'il a vu est déjà coché, `found` est maintenant vide et il écrit « Encore … » sous « Bravo » (avant le lot, il le faisait déjà quand il n'avait rien vu du tout). Un message faux sous « Bravo », « Étape suivante » reste là. Parade : ne dire l'ordre que si `u.current.id !in _ui.value?.progress?.done`.
- Le rapport classe la cause en « charge de la CI » ; elle vaut aussi sur téléphone (worker multi-fil) : formulation à corriger dans un prochain rapport, sans effet sur le code.

## Mineur
- Le test attend le message par une boucle `idle()` + `Thread.sleep(5)` bornée à 10 s : il ne peut pas passer vert à tort (l'ordre des regards est fixé par le dispatcher retenu, `Held`), seulement rougir sur une machine extrêmement lente.
- Le test ne vérifie pas le texte exact (« Encore » en tête seulement) ; `TutoLot5ControllerTest#aWrongOrderIsSaid` couvre le contenu.

## Relancé et vu
- `scripts/bench/gradle-file.sh :app:testDebugUnitTest --no-configuration-cache --rerun --tests …Tour6TutoOrderRaceTest --tests …TutoLot5ControllerTest --tests …Tour6TutoPanelAlreadyTest` sur `7193704e` : BUILD SUCCESSFUL ; OrderRace 1/1, Lot5Controller 5/5, PanelAlready 3/3, 0 échec.
- rouge constaté : 569ede09 : rejoué sur 9fa00472 : `TutoController.kt` de 9fa00472 remis seul, `Tour6TutoOrderRaceTest#whatTheOrderLacksIsSaidWhenAnEarlierLookAnswersLate` échoue, « what the order lacks is said: « null » » ; fichier remis à HEAD ensuite, arbre propre.
- Le test prouve la course : le dispatcher `Held` retient tous les regards jusqu'au changement des parties, puis ils répondent dans l'ordre ; rouge sans la correction, vert avec, sans dépendre du minutage.

Verdict : 0 bloquant, 0 grave — fusion possible
