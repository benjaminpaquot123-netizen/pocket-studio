# T0 : BenStudio Test, robot intégré et essai éclair (2026-11-11)

Plan : `docs/PLAN-TEST-APK.md` (v2). Fait : T0 seulement, rien de T1 à T5.

## Construit
- `-PbenTest` : `com.pocketstudio.test`, « BenStudio Test », icône à fond vert, lanceur = écran de préparation ; sources `app/src/journey` (Journey + `JourneyDriver`) et `app/src/benTest` (robot). Sans la propriété : APK et tâches inchangés, ces sources ne servent qu'aux tests unitaires.
- `JourneyDriver`, six gestes ; `ComposeRuleDriver` (tests actuels, inchangés) et `InAppDriver` (arbre sémantique de toutes les fenêtres via `WindowInspector`, vrais `MotionEvent`).
- Carnet de bord sur disque (`files/bentest/carnet.tsv`), durée maximale par étape, plantage noté avec le nom de l'étape, « Reprendre » passe à la suivante.
- Zip de rapport < 15 Mo (`rapport.md`, `mesures.json`, carnet, journal, captures), partagé seulement sur « Envoyer le rapport ».
- Écran de préparation (micro, notifications), essai éclair : ouverture, 3 styles (jump up, house, techno, 30 s mesurées 4 fois/s), 1 export M4A, 5 écrans capturés, 1 question.

## Preuves (machine de l'agent, sans émulateur)
- Rouge puis vert : 13 tests neufs d'abord rouges sur leurs assertions (code neutralisé), puis verts : `PassageTest` (5), `RapportZipTest`, `EssaiEclairTest`, `JourneyDriverTest`, `InAppDriverTest` (5, Robolectric : vrais touchers dans l'activité et dans un dialogue, liste défilée ; chemin « Nouveau morceau › assistant › jump up » de l'appli ; les 5 écrans de l'essai capturés ; écran de préparation ; fil du robot vers le fil principal).
- Parcours existants toujours verts par le nouveau pilote des tests : `JourneyPathsTest`, `GuideJourneyTest`, `KickPanJourneyTest`.
- APK BenStudio Test produit ici en release R8 (`bash scripts/bench/gradle-file.sh -PbenTest :app:assembleRelease`), signé avec la clé de débogage locale : 53 Mo. `scripts/bentest/check-apk.sh` : paquet, signature, robot présent, 34 étiquettes de l'essai encore posées par le code de l'appli après R8.
- `scripts/check-jni.sh` refuse l'APK BenStudio Test (robot trouvé) et accepte la vraie release (aucune classe du robot).

## Reste à prouver (essai éclair de Benjamin)
- Les touchers du robot sur le Honor (MagicOS) et la création des 3 morceaux avec le vrai moteur (jamais faite par ce pilote ici : Robolectric n'a pas de moteur).
- Les captures : en Robolectric le dessin est vide ; vraies images seulement sur le téléphone.
- Livraison : l'APK n'est pas dans le dépôt ; il faut un moyen de le lui donner (repli du plan : compilation GitHub manuelle, sur son oui).
- Constantes d'action des services : toutes visent leur propre classe (intents explicites), pas de croisement avec la vraie appli.

## Essai éclair n° 1 de Benjamin (2026-11-11, HONOR BKQ-N49, Android 17) : `2026-11-11-eclair-1/`
- Résultat : 1 réussie, 6 ratées, 0 plantage ; aucun son (lecture jamais lancée), charge ≤ 9 %, 0 décrochage, aucune erreur. Prouvé sur le téléphone : le robot lit l'écran et ses touchers passent (R8 compris), carnet, rapport et partage du zip marchent.
- Cause réelle (reproduite en Robolectric, `PremiereInstallationTest`, mêmes messages) : appli tout juste installée, donc premier lancement. Le robot a vu le studio sous l'animation d'ouverture et a cru l'ouverture finie ; l'écran de bienvenue est arrivé après et il ne l'a jamais fermé. Sans réponse à la bienvenue, « ← » de Mes projets ne mène pas à « Que veux-tu faire ? » (`launch` absent), et l'appli reste en Mode simple (pas de `tab_Mix` ni `tab_Structure`).
- Défaut du robot, pas de l'appli : ses touchers atteignaient le studio à travers la fenêtre de bienvenue, ce qu'un doigt ne peut pas faire. La vraie appli n'a pas été modifiée.
- Corrigé : chaque étape part de ce qui est à l'écran (bienvenue, Simple ou Pro, Mes projets, accueil) ; chaque morceau est créé en Pro par l'accueil ; la lecture est prouvée par le moteur (transport en marche et niveau non nul) avant l'écoute ; la question n'est posée que si un morceau a joué.
- En cas d'échec, le carnet note ce qui était à l'écran (20 étiquettes au plus, la fenêtre du dessus d'abord) et tout toucher passé sous une autre fenêtre.
- Reste à prouver au 2e essai : la création des 3 morceaux et le son avec le vrai moteur, l'export M4A réel, les captures.

## Essai éclair n° 3 (2026-11-12, HONOR BKQ-N49, Android 17, commit 72d642ff) : `2026-11-12-eclair-3/`
- 7 réussies sur 7, 0 plantage : T0 prouvé. Charge moyenne (pire) : jump up 53 % (72 %), house 39 % (54 %), techno 39 % (52 %) ; 0 décrochage ; export M4A de 18 s en 7 s.
- Le journal de l'appli montrait 3 fois « Nouveau morceau impossible : Job was cancelled » : vrai défaut de l'appli (fausse erreur), reproduit (`NewSongOpenTest`) et corrigé (voir `docs/decisions/tour-5-testapk.md`).
