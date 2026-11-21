# Tour 9, lot « flash » : relecture neuve

Relu : 28b38b0bb (base 8ee635e44)

## Ce qui a été vérifié
- Bienvenue (`Welcome.kt`) : contenu, menu des langues, choix et « Simple ou Pro ? » inchangés ; Retour passe par `BackHandler` sur le même `dismiss` que la fenêtre d'avant (« Plus tard », ou retour aux choix pendant « Simple ou Pro ? »). La `Surface` plein écran prend tous les touchers.
- Jamais bloqué derrière le studio couvert : chaque choix (guidé, j'apprends, démo, zéro, remix, Pad Live, voix, défis, plus tard) passe par `firstLaunchSeen()` ; `guideSeen` est lu de façon synchrone au démarrage (`PocketStudioApplication`) et jamais remis à faux ; le studio redevient visible et lisible par TalkBack dès que `guideSeen` est vrai. Rotation : test vert ; mort du processus : `guideSeen` est enregistré, Bienvenue revient sinon.
- Retour d'un habitué (`guideSeen` vrai) : `studioCovered` = `launchShown` comme avant, rien ne change.
- Placement : `firstLaunch()` est dans la `Box` de `MainActivity`, sous `BrandIntro` (dessiné après), donc caché par le logo puis découvert sans trou.
- Banc : `ecran()` saute les nœuds sous un `clearAndSetSemantics`. Dans l'appli, ce sont surtout des sondes de mesure jamais dessinées (table de mixage, tuiles de Plus) : les sauter est juste.
- Tests : `PremiereInstallationTest` dit maintenant que le studio couvert n'est pas à l'écran (c'est le comportement voulu) ; `BenchSelfTest` passe sur phone411 et vérifie toujours qu'une carte coupée est dite « coupé » sans flèche inventée : l'intention est gardée.

## Points mineurs (pas bloquants)
- `ecran()` ne nomme plus `tuto_text_cut` (le « … » visible du tuto porte son propre `clearAndSetSemantics`) : seule la note « à l'écran » du banc le perd, aucun test ne lit ce nom par `ecran()`.
- Pendant `leaving` (projet en cours d'ouverture après un choix), le studio redevient lisible par TalkBack alors que Bienvenue le couvre encore, le temps de la reconstruction.
- Une fenêtre automatique du studio (mise à jour, etc.) se dessine maintenant au-dessus de Bienvenue, et plus en dessous.

## Tests lancés
session.Tour9* (44), ui.Coquille2SeatTest (12), nav.HomeAndPadsTest (11) : 67 tests, 0 échec.

Verdict : 0 bloquant, 0 grave
