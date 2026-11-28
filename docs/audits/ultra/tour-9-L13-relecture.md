# Tour 9 — L13 : relecture neuve

Relu : d1525f73 (diff ed0ea8f3..d1525f73, TutoPanel.kt + Tour9L13KitShedTest.kt)

Relecteur : sous-agent neuf, qui n'a pas écrit le code (seconde relecture : les 2 graves de la première sont corrigés dans d1525f73, couverts par Tour9L13KitShedTest, rouge avant).

Tests : `com.pocketstudio.app.tuto.*` + TutoJourneyTest = 470 tests, 1 rouge, celui connu de la base (T9PanelTargetTest.fillCellsAboveThePanelIn360At200Japanese). Pré-vol : PRÊT.

## Bloquant
Aucun.

## Grave
Aucun. Les écritures faites pendant la mesure sont bornées (kitGrown ≤ 3, kitShed ≤ 1, regrown ≤ 2). Cacher l'en-tête ne relance ni kitShown ni held. Aucun plantage.

## Mineur (notés, pas corrigés)
- TutoPanel.kt:719 : le plafond grandit de la hauteur du kit sur toutes les étapes avec l'offre, borné à 85 %.
- TutoPanel.kt:841 : kitMissing peut atteindre 3 fois le manque si le plafond est déjà bloqué. Sans effet visible tant que la borne tient.
- TutoPanel.kt:649 : kitShed a pour clé `kit != null`, pas kitShown. Pendant Bravo ou une démo, l'en-tête reste caché.
- TutoPanel.kt:707 : kitTall sans clé, vieux d'une image (défaut d'avant).
- Tour9L13KitShedTest vérifie seulement le retour de l'en-tête. La promesse du lot (une ligne de consigne visible) est tenue par Tour7TutoKitBigFontTest et Tour7TutoKitWordsInSightTest.

Verdict : 0 bloquant, 0 grave
