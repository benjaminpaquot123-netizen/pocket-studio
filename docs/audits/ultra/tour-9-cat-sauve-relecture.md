# Relecture neuve : lot cat-sauve (tour 9)

Relu : 92fe4138

Relecteur : sous-agent neuf, n'a pas écrit le code. Fichiers : ScenariosSauve.kt, la ligne ajoutée dans Scenarios.kt, ScenariosSauveTest.kt.

- travail() comprend « export:style:format » (Scenarios.kt, branche « export ») ; les 18 styles existent (StarterWizard), wav, flac, m4a sont dans ToutTester.PREVU.
- Ids uniques (préfixe sav_), aucun couple style-format repris de EXPORTS_STYLES ni de house.
- Ordre d'initialisation sans risque : ScenariosSauve ne lit que des const val de Scenarios, jamais LISTE.
- Le test vérifie de vraies choses (geste, style, format, doublons, trois formats, paquet complet seulement).
- Hors lot, connus : ScenariosTest.kt:262 (133 en dur) et :275 (somme des estimations > 65 min avec 18 × 75 s) : laissés au pilote.
