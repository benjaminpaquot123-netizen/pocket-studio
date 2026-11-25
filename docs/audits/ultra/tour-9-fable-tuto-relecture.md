# Relecture neuve : lot fable-tuto (tour 9)

Relu : 130463ab8

Relecteur : sous-agent neuf, n'a pas écrit le code. Diff relu : `git diff d87dea73f 130463ab8 -- app/src project-model/src docs/audits/ultra/rejeux`.

- cible-autre-partie : `partPro` et `count` ont une valeur par défaut, les 30 autres guides se lisent comme avant ; tous les chemins qui vérifient une étape passent par `chaptersFor` ; « Drop » ne valide pas une cymbale posée dans « Drop 2 ».
- d-fin : `DrumsChanged.done` compte les coups neufs, minimum 1 : les étapes sans `count` gardent l'ancien seuil ; « Je le fais » pose `count` coups sans sortir des bornes.
- a-V1, modèle : `fits` ne plante pas sur une liste vide ; la note cachée n'est cadrée sans toucher que si toutes tiennent.
- a-V1, PianoRoll.kt : le pliage ne part que quand `focus` change, pas de boucle de recomposition ; un glisser du doigt annule l'animation, le rôle ne se bat pas contre l'utilisateur.
- Thread audio : aucun fichier moteur ou natif touché.
- Tests : T9FableLevelPartsTest parcourt tous les styles et les deux niveaux, avant et après ; Tour8 lit la position réelle à l'écran, Simple et Pro, basse et mélodie, trois écrans dont le gros texte.

Mineurs (notés, pas bloquants) :
- PianoRoll.kt:864 et 885 : au pliage d'ouverture, la note éclairée est la note mise de côté, pas la note demandée (gêne visuelle).
- PianoRoll.kt:865 : le pliage change d'office `rowDp` (gardé), les rangées peuvent rester pliées (jusqu'à 5 dp) aux étapes suivantes.
- `count` n'est pas gardé à la reprise d'une étape : 2 coups posés, on revient, il en faut 4 neufs.
- « une par temps de la mesure 1 » ne se suit pas à la lettre si le Shaker du Drop 2 a déjà des coups sur ces temps (rien ne bloque).
- TutoSightHintTest ne couvre plus la flèche ↓/↑ côté interface (seul le test du modèle la couvre).
- Tour8TutoTouchHere : `touchHere` part maintenant d'une mesure déjà en vue, il prouve surtout qu'un second toucher ne fait pas basculer le rôle.

Verdict : 0 bloquant, 0 grave
