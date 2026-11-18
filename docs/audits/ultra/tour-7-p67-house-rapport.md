# Tour 7, lot p67-house : rapport

Correcteur, branche `eq/tour-7-p67-house` (base 3cd69a96). Zone : les consignes Facile des 9 styles house (acid_house, afro_house, amapiano, deep_house, disco, house, speed_garage, tech_house, uk_garage) et leurs traductions (14 langues).

tests: :project-model:test --tests 'com.pocketstudio.model.tuto.*' --tests 'com.pocketstudio.model.i18n.*' (269/0, dont Tour6Tuto4HouseTest, Tour6Tuto4HouseRound3Test, TutoOneMomentTest, TutoShowMeEverywhereTest, TutoValidationMomentTest, TutoSplitKeepsIdsTest, TutoCourseTest, I18nCatalogTest, KoreanRegisterTest, Tour7LanguagesTest) ; :app:testDebugUnitTest --tests '*TutoReadabilityBudgetTest' --tests '*TutoOneMomentShownTest' (3/0)

chemin: Tuto Facile, panneau de consigne des styles house (360 dp à 100 %, ouvert à 130 %, 320 dp), 15 langues | test: TutoReadabilityBudgetTest#everyFacileConsigneFitsOrIsAKnownException
chemin: Tuto Pro, consignes house inchangées (texte d'origine gardé en Pro) | test: TutoReadabilityBudgetTest#proOutOfBudgetOnlyGoesDown
chemin: Tuto Facile, une seule série de gestes par consigne house, Pro garde son texte | test: TutoOneMomentShownTest#facileShowsTheNeedAloneAndProKeepsItsText

## Ce qui a changé
- Consignes Facile raccourcies dans les 9 styles ; l'ancien texte gelé en `textPro`. Les 348 étapes Pro house montrent le même texte qu'avant (0 différence).
- Traductions : 87 nouvelles clés dans `i18n/<langue>/tuto_house.json`, clés orphelines retirées ; ja, ko, ar, hi relues par un relecteur neuf (0 bloquant ; 3 graves en hindi, corrigés).
- Listes d'exceptions : 366 lignes house retirées de `readability-facile-exceptions.txt`, 63 de `tuto-moment-exceptions.txt`.
- Pro hors budget : 3725 avant, 3725 après (cliquet 3725).

## Restant pour le bloc 4 (textes partagés et moments)
- `readability-facile-exceptions.txt` : les lignes `house/gap` (fr en nl es pt-BR de it pl tr id hi ja) : texte partagé.
- `tuto-moment-exceptions.txt` : 10 lignes : amapiano/pad_2, deep_house/bass_do, deep_house/pad_end, disco/pad_2, house/lead_do, house/make_drop2 (Tour6 M4 exige « Créer la partie »), tech_house/lead_do, tech_house/pad_end, uk_garage/pad_end (nom de bouton du moteur), house/gap (série, texte partagé).

## Tests adaptés
- Aucun (aucun vieux test n'a eu besoin de lire « text + why »).

## Hors liste, avec l'accord du chef
28 clés retirées de `i18n/<14 langues>/tuto_autres.json` (suppressions seules) : seules les anciennes consignes house les montraient.
- « À toi : touche les 4 notes de la mesure 2. Puis « Copier sur les mesures suivantes » : il fait la mesure 3 (sur Mi♭). »
- « À toi : touche les 4 notes de la mesure 2. Puis « Copier sur les mesures suivantes » : il les fait suivre les accords (La, puis Sol). »
- « À toi : touche les 5 notes de la mesure 2. Puis « Copier sur les mesures suivantes » : il refait la mesure 1 en mesure 3. »
- « À toi : touche les 5 notes de la mesure 2. Puis « Copier sur les mesures suivantes » : il refait les 2 mesures sur l'accord suivant (Si♭). »
- « À toi : touche les 7 notes de la mesure 2, sur Ré. Puis « Copier sur les mesures suivantes » : il refait la mesure 1 en mesure 3. »
- « À toi : touche les 8 notes de la mesure 2. Puis « Copier sur les mesures suivantes » : il refait la mesure 1 en mesure 3. »
- « À toi : touche « Copier sur les mesures suivantes » : il fait la mesure 2, et la même chose sur Ré en mesure 3. Écris enfin la mesure 4 : elle finit sur Mi et Sol pour revenir vers La. »
- « À toi : touche « Copier sur les mesures suivantes » : il refait la mesure 1 sur chaque accord (Fa, Ré, Mi). Écris enfin les 4 notes de la mesure 4. »
- « À toi : touche « Copier sur les mesures suivantes » : le reste se fait tout seul. »
- « À toi : touche « Copier sur les mesures suivantes », puis écris les 2 notes plus graves de la mesure 4 : la réponse. »
- « À toi : touche « Créer la piste Accords », puis la note du bas de chaque accord en pointillé de chaque mesure : tout l'accord se pose. Il y a 2 coups par mesure. »
- « À toi : touche « Créer la piste Accords », puis la note du bas de chaque accord en pointillé de la mesure 1 : tout l'accord se pose. Il y a 2 coups. »
- « À toi : touche « Créer la piste Accords », puis la note du bas de chaque accord en pointillé de la mesure 1 : tout l'accord se pose. Il y a 4 coups. »
- « À toi : touche « Créer la piste Accords », puis la note du bas de chaque accord en pointillé des mesures 1 et 2 : tout l'accord se pose. Il y a 4 coups par mesure. »
- « À toi : touche « Créer la piste Accords », puis la note du bas de chaque accord en pointillé des mesures 1 et 2 : tout l'accord se pose. Il y a 6 coups par mesure, un autre accord à la mesure 2. »
- « À toi : touche « Créer la piste Accords », puis la note du bas de chaque accord en pointillé des mesures 1 et 2 : tout l'accord se pose. Mesure 1 : 3 coups ; mesure 2 : un accord long. »
- « À toi : touche « Créer la piste Accords », puis la note du bas de l'accord en pointillé de chaque mesure : tout l'accord se pose. Un seul accord long par mesure. »
- « À toi : touche « Créer la piste Basse » (le son « Log drum » est déjà choisi), puis les 5 notes de la mesure 1. »
- « À toi : touche « Créer la piste Basse », puis les 4 notes de la mesure 1. »
- « À toi : touche « Créer la piste Basse », puis les 4 notes en pointillé de la mesure 1. »
- « À toi : touche « Créer la piste Basse », puis les 8 notes de la mesure 1 : la montée La, Do, Mi, Sol, La. »
- « À toi : touche « Créer la piste Basse », puis les 8 notes en pointillé de la mesure 1. »
- « À toi : touche « Créer la piste Basse », puis les notes en pointillé de la mesure 1 (La grave, La aigu). »
- « À toi : touche « Créer la piste Mélodie », puis les 3 notes en pointillé de la mesure 1 : l'appel. »
- « À toi : touche « Créer la piste Mélodie », puis les 3 notes longues en pointillé des mesures 1 et 2. »
- « À toi : touche « Donner les durées conseillées » : chaque partie prend sa durée : Intro 8, Groove 12 (16 la 2e fois), Montée 4, Drop 8 (16 à la fin), Break 16, Fin 8. »
- « À toi : écris les 2 notes de la mesure 2, la réponse plus grave, puis touche « Copier ». »
- « À toi : écris les 8 notes de la mesure 2, puis touche « Copier sur les mesures suivantes » : le reste se fait tout seul. »
