# Tour 7, p67-techno : rapport de zone (bloc 1 de P6/P7, 2026-11-18)
code de départ : 3cd69a96 (ccr-80af1a1b-mn0wec) ; branche eq/tour-7-p67-techno.
Fait : les dix cours techno (acid_techno, acidcore, detroit, gabber, hard_techno, industrial_techno, minimal, retro_techno, techno, trance) récrits en un moment par consigne Facile (aucun nom de bouton du moteur, une seule série de gestes), gloses et chiffres renvoyés dans « Pourquoi ? » ; Pro inchangé (ancien texte figé en textPro là où le Pro en dépendait) ; découpes : la première pièce garde l'identifiant. 104 textes neufs traduits dans les 14 langues, mesurés après traduction (de, hi, pt-BR, ja raccourcis).
Exceptions Facile (readability-facile-exceptions.txt) : 694 lignes à moi retirées, 173 restent, toutes sur des textes partagés entre familles (bloc 4) : techno/share 42, trance/levels 28, minimal/levels 28, gabber/song_order 15, et le « gap » partagé (acid_techno, acidcore, trance, minimal, gabber : 12 chacun).
Exceptions de moment (tuto-moment-exceptions.txt) : 78 lignes retirées ; 18 restent, toutes sur des textes partagés (bloc 4) : acid_techno/gap, acidcore/gap, detroit/lead_do, detroit/pad_2, gabber/gap, gabber/lead_do, industrial_techno/pad_2, minimal/bass_2, minimal/gap, minimal/levels, minimal/pad_end, retro_techno/bass_do, techno/bass_end, techno/lead_2, techno/pad_end, trance/gap, trance/levels, trance/pad_end.
Pro hors budget : 767 avant, 756 après (ne monte pas).
Choix notés : ja, quelques consignes perdent l'ouverture « あなたの番 » (« 長さを決めよう： ») et les espaces avant les chiffres, pour tenir en 5 lignes à 130 % ; hi, « व » pour « और » dans industrial_techno/kick_do et minimal/clap_do (place). Le texte techno/clap_do (clé de tuto_autres) a été reformulé pour devenir une clé propre à tuto_techno ; l'ancienne clé, devenue inutile, est retirée de tuto_autres.
Zones partagées (consigne du chef) : dans tuto_autres, suppressions seules de clés qui ne servaient qu'aux cours techno, sans réordonner ni reformater ; liste exacte ci-dessous, identique dans les 14 langues. tuto_bass : aucune clé retirée.
Batterie : project-model 224/0 (tests complets du modèle), app 3/0 ; fichiers PS_TUTO_FAULTS vides. Compte final : Facile 173 restantes (toutes bloc 4), moment 18 (bloc 4), Pro 767 → 756.
Tests adaptés (consigne du chef : lire « text + why », chaque exigence gardée) : Tour6Tuto4TechnoTest (padsNamedAsWritten, tradeWordsExplainedWhereTheyFirstShow, numbersTellTheTruth, noContradiction, marksAndExportWordsSaid, theRimAndTheTitlesWordsExplainedWhereTheyFirstShow) et Tour6TutoLongPressMenuWordsTest#aLongPressOnTheCellIsTheOneGesture.
Nouveaux « Pourquoi ? » propres à tuto_techno (14 langues) : techno ref, perc_do, gap, crash (gloses et noms de pads), et les 7 make_* (intro, intro2, drop, break, montee, drop2, fin) qui nomment leur bouton « Créer la partie « X » » ; make_break et make_fin ont une clé techno neuve, les anciennes clés partagées de tuto.json ne bougent pas. ja : bouton « エンディング » (comme l'interface), jamais 終了.
Relecture ja/ko/ar/hi (tour-7-p67-techno-relecture.md) : 0 grave ; légers non corrigés, laissés au chef : ja « 低くて高い、長い音 », ko verbe manquant dans « crée la Montée », ar « mets en Doux » et accord de « choisis la couleur », hi « हर 2 बार में एक बार » ambigu. Hors périmètre, tout le projet : ar الكلاب→التصفيق et الريف→الرايف ; ko 엔딩 dans les textes contre le bouton 끝.
tests: :project-model:test --tests 'com.pocketstudio.model.tuto.*' --tests 'com.pocketstudio.model.i18n.I18nCatalogTest' --tests 'com.pocketstudio.model.i18n.KoreanRegisterTest' --tests 'com.pocketstudio.model.i18n.Tour7LanguagesTest' :app:testDebugUnitTest --tests 'com.pocketstudio.app.tuto.TutoReadabilityBudgetTest' --tests 'com.pocketstudio.app.tuto.TutoOneMomentShownTest'
chemin: panneau du tuto, cours techno, chaque consigne Facile | test: TutoReadabilityBudgetTest#everyFacileConsigneFitsOrIsAKnownException
chemin: panneau du tuto, cours techno, consignes Pro | test: TutoReadabilityBudgetTest#proOutOfBudgetOnlyGoesDown
chemin: panneau du tuto, Facile, bouton de préparation attendu | test: TutoOneMomentShownTest#facileShowsTheNeedAloneAndProKeepsItsText

## Clés retirées de tuto_techno (48, toutes langues)
- À toi : choisis la couleur du morceau, sombre. « Je le fais » ouvre la fiche Tonalité : touche « Phrygienne » puis « La ».
- La 303 reste presque toujours sur la même note : la gamme lui donne ses quelques couleurs.
- À toi : dans « Mon morceau », sous « Ajouter », touche dans l'ordre les boutons qui finissent par Intro, Intro 2, Drop, Break, Montée, Drop 2 et Fin (par exemple « + B · Intro »). Les durées ne sont pas encore les bonnes : l'étape suivante les règle.
- À toi : le mix, c'est régler le volume de chaque piste. Dans l'onglet « Mix », baisse un peu le curseur de la 303, puis celui du pad « Clap ». Sous le curseur, « Normal » devient « −2 », « −4 »… : plus bas, moins fort.
- À toi : choisis la couleur du morceau, très sombre. « Je le fais » ouvre la fiche Tonalité : touche « Phrygienne » puis « Fa♯ ».
- Le kick et la 303 partagent la même note : ils sonnent comme un seul monstre.
- À toi : choisis la couleur du morceau : mélancolique mais lumineuse, comme Detroit. « Je le fais » ouvre la fiche Tonalité : touche « Dorienne » (une gamme mineure avec une note claire) puis « La ».
- Toutes tes notes suivront cette gamme : les cordes sonneront tristes et chaudes à la fois.
- À toi : dans « Mon morceau », sous « Ajouter », touche dans l'ordre les boutons qui finissent par Intro, Intro 2, Drop, Break, Drop 2 et Fin (par exemple « + B · Intro »). Les durées ne sont pas encore les bonnes : l'étape suivante les règle.
- À toi : le mix, c'est régler le volume de chaque piste. Dans l'onglet « Mix », baisse un peu le curseur de la piste « Mélodie », puis celui de la piste « Accords ». Sous le curseur, « Normal » devient « −2 », « −4 »… : plus bas, moins fort.
- À toi : choisis la couleur du morceau, sombre et agressive. « Je le fais » ouvre la fiche Tonalité : touche « Mineure » puis « Fa♯ ».
- Le kick est accordé sur la note du morceau : il fait la batterie et la basse à la fois.
- À toi : le mix, c'est régler le volume de chaque piste. Dans l'onglet « Mix », baisse un peu le curseur de la piste « Accords » (le hoover), puis celui de la piste « Mélodie ». Sous le curseur, « Normal » devient « −2 », « −4 »… : plus bas, moins fort.
- Le kick est accordé sur la note du morceau : grave et notes ne se battent pas.
- À toi : sur le pad « Rim », touche les cases en pointillé : une case sur trois. Elle ne tombe jamais au même endroit par rapport au kick.
- Trois contre quatre : la percussion tourne en rond, c'est le tournis de la schranz (la techno allemande très dure).
- À toi : dans « Mon morceau », sous « Ajouter », touche dans l'ordre les boutons qui finissent par Intro, Intro 2, Drop, Break, Montée, Faux drop, Drop 2 et Fin (par exemple « + B · Intro »). Les durées ne sont pas encore les bonnes : l'étape suivante les règle.
- À toi : choisis la couleur du morceau, la plus noire. « Je le fais » ouvre la fiche Tonalité : touche « Phrygienne » puis « Sol ».
- Ici, les notes comptent peu : la gamme sert surtout à accorder le grave.
- À toi : sur le pad « Kick », touche les cases en pointillé : une sur chaque temps, et deux petits coups doux juste avant les temps 2 et 4.
- À toi : dans « Mon morceau », sous « Ajouter », touche dans l'ordre les boutons qui finissent par Intro, Intro 2, Drop, Drop B, Drop, Break, Drop 2 et Fin (par exemple « + B · Intro »). Les durées ne sont pas encore les bonnes : l'étape suivante les règle.
- À toi : le mix, c'est régler le volume de chaque piste. Dans l'onglet « Mix », baisse un peu le curseur de la piste « Accords » (le bourdon), puis celui de la piste « Mélodie ». Sous le curseur, « Normal » devient « −2 », « −4 »… : plus bas, moins fort.
- À toi : choisis la couleur du morceau. « Je le fais » ouvre la fiche Tonalité : touche « Mineure » puis « Sol ».
- Un seul accord, une seule couleur : l'oreille se concentre sur les petits clics.
- À toi : choisis la couleur du morceau. « Je le fais » ouvre la fiche Tonalité : touche « Mineure » puis « Ré ».
- Trois accords qui montent vers le retour au Ré : la grille de toutes les raves.
- À toi : dans « Mon morceau », sous « Ajouter », touche dans l'ordre les boutons qui finissent par Intro, Intro 2, Drop, Mini-break, Drop, Break, Drop 2 et Fin (par exemple « + B · Intro »). Les durées ne sont pas encore les bonnes : l'étape suivante les règle.
- À toi : le mix, c'est régler le volume de chaque piste. Dans l'onglet « Mix », baisse un peu le curseur de la piste « Mélodie » (le hoover), puis celui de la piste « Basse ». Sous le curseur, « Normal » devient « −2 », « −4 »… : plus bas, moins fort.
- À toi : choisis la couleur du morceau, sombre comme la techno. « Je le fais » ouvre la fiche Tonalité : touche les deux boutons entourés, « Phrygienne » (une gamme sombre) puis « Fa » (la note de départ). Pas besoin de retenir ces mots.
- À toi : sur le pad « Kick » (le bouton qui joue le kick), touche les cases en pointillé : une sur chaque temps. Chaque case est un moment de la boucle.
- Un kick par temps (le « four-on-the-floor »), c'est le battement que tout le club suit avec les pieds.
- À toi : sur le pad « Rumble » (le grondement), touche les cases en pointillé : les mêmes que le kick, une sur chaque temps. Son long grondement sale et grave remplit le vide entre les coups.
- Le rumble remplit le grave entre les kicks : c'est le son des clubs de Berlin.
- À toi : sur le pad « Charley ouvert » (un « tsss » plus long), touche les cases en pointillé : juste entre les kicks, sur les contretemps.
- Kick, charley, kick, charley : ce va-et-vient pousse à danser.
- À toi : sur le pad « Rim » (un coup sur le bord du tambour : un petit « toc »), touche les cases en pointillé : il tombe à côté des temps, décalé.
- Un son décalé qui revient pareil à chaque mesure : c'est ce qui hypnotise.
- À toi : touche « Créer la piste Basse » (une piste = un instrument de ton morceau), puis les notes en pointillé de la mesure 1 : deux petites notes après chaque kick.
- Une basse qui ne joue jamais avec le kick laisse chaque coup claquer.
- À toi : touche « Créer la partie « Montée » » : sans kick ni basse, son clap va de plus en plus vite, mesure après mesure. Le temps de silence de la fin, tu le feras au chapitre 12.
- La tension monte parce que le kick manque et que le clap s'emballe.
- Un temps de rien, et le Drop 2 paraît deux fois plus fort.
- À toi : choisis la couleur du morceau : mélancolique et lumineuse. « Je le fais » ouvre la fiche Tonalité : touche « Mineure » puis « La ».
- Quatre accords, un par mesure, font tout le voyage : de la mélancolie à l'euphorie.
- À toi : dans « Mon morceau », sous « Ajouter », touche dans l'ordre les boutons qui finissent par Intro, Intro 2, Montée, Drop, Break, Montée, Drop 2 et Fin (par exemple « + B · Intro »). Les durées ne sont pas encore les bonnes : l'étape suivante les règle.
- À toi : dans « Mon morceau », sous « Ajouter », touche dans l'ordre les boutons qui finissent par Intro, Intro 2, Drop, Break, Montée, Drop 2 et Fin (par exemple « + B · Intro »). Chaque partie arrive avec la longueur de sa boucle (8 mesures, la Montée 4) : l'étape suivante lui donne sa vraie durée.
- À toi : le mix, c'est régler le volume de chaque piste. Dans l'onglet « Mix », baisse un peu le curseur de la piste « Accords », puis celui de la piste « Mélodie ». Sous le curseur, « Normal » devient « −2 », « −4 »… : plus bas, moins fort. Les repères à côté (+4, 0, −12, −∞) disent la même chose : 0 est le volume normal, −∞ le silence.
- À toi : dans la partie « Montée », dernière mesure, efface les 4 coups du pad « Clap » sur le dernier temps (touche les 4 croix rouges) : un temps de silence juste avant le Drop 2. C'est une transition (le petit passage qui annonce la partie suivante).

## Clés retirées de tuto_autres (39, toutes langues)
- À toi : touche « Donner les durées conseillées » : chaque partie prend sa durée : Intro 16 mesures, Intro 2 16, Drop 32, Break 12, Montée 4, Drop 2 32 et Fin 16.
- À toi : touche « Créer la piste Accords », puis la note du bas de chaque accord en pointillé des mesures 1 et 2 : tout l'accord se pose. Mesure 1 : un accord de La ; mesure 2 : un accord de Ré.
- À toi : touche « Créer la piste Basse », puis les 6 notes de la mesure 1 (sur La, avec des sauts vers le haut).
- À toi : touche les 6 notes de la mesure 2 (sur Ré). Puis « Copier sur les mesures suivantes ».
- À toi : écris les 4 notes de la mesure 2 (la dernière est longue), puis touche « Copier sur les mesures suivantes ».
- À toi : touche « Donner les durées conseillées » : chaque partie prend sa durée : Intro 16 mesures, Intro 2 16, Drop 32, Break 16, Drop 2 24 et Fin 16.
- À toi : touche « Passer à 2 mesures » si on te le propose, puis sur le pad « Caisse », touche les cases en pointillé à la fin de la 2e mesure : un roulement qui accélère.
- À toi : touche « Créer la piste Accords », puis la note du bas de chaque accord en pointillé des mesures 1 et 2 : tout l'accord se pose. Chaque coup, c'est la même note jouée grave et aiguë.
- À toi : touche « Copier sur les mesures suivantes » : il les recopie en suivant les accords (Ré, puis Mi).
- À toi : écris les 5 notes de la mesure 2 (la dernière est longue), puis touche « Copier sur les mesures suivantes ».
- À toi : touche « Donner les durées conseillées » : chaque partie prend sa durée : Intro 24 mesures, Montée 8, Drop 32, Break 24, Drop 2 32 et Fin 16.
- À toi : touche « Passer à 2 mesures » si on te le propose, puis sur le pad « Clap », touche les cases en pointillé des 2 mesures : les temps 2 et 4, et un petit roulement tout à la fin.
- À toi : touche « Copier sur les mesures suivantes » : il fait le reste, la mesure 3 monte d'un cran.
- À toi : touche « Créer la piste Mélodie », puis les 3 notes en pointillé de la mesure 1 : des notes longues.
- À toi : écris les 3 notes de la mesure 2, puis touche « Copier sur les mesures suivantes ».
- À toi : touche « Donner les durées conseillées » : chaque partie prend sa durée : Intro 16 mesures, Intro 2 16, Drop 32, Break 8, Montée 4, Faux drop 4, Drop 2 32 et Fin 16.
- À toi : touche « Créer la piste Accords », puis la note du bas de chaque accord en pointillé de la mesure 1 : tout l'accord se pose. C'est une note longue, jouée grave et aiguë.
- À toi : touche « Créer la piste Mélodie », puis la note en pointillé du début de la mesure 1.
- À toi : écris la note de la fin de la mesure 2, puis touche « Copier sur les mesures suivantes ».
- À toi : touche « Donner les durées conseillées » : chaque partie prend sa durée : Intro 16 mesures, Intro 2 16, Drop 16, Drop B 8, Drop 8, Break 16, Drop 2 32 et Fin 16.
- À toi : touche « Passer à 2 mesures » si on te le propose, puis sur le pad « Rim » (un « toc » de rim), touche les cases en pointillé des 2 mesures : il tombe à côté des temps, et la 2e mesure change un peu.
- À toi : touche « Créer la piste Basse », puis les 4 notes en pointillé de la mesure 1, entre les kicks.
- À toi : touche « Créer la piste Accords », puis la note du bas de chaque accord en pointillé de la mesure 1 : tout l'accord se pose. Il y a 2 accords.
- À toi : touche « Créer la piste Mélodie », puis les notes en pointillé de la mesure 1 : 6 notes, une case sur trois.
- À toi : écris les 5 notes de la mesure 2, puis touche « Copier sur les mesures suivantes ».
- À toi : touche « Donner les durées conseillées » : chaque partie prend sa durée : Intro 16 mesures, Intro 2 16, Drop 32, Break 12, Montée 4, Drop 2 24 et Fin 16.
- À toi : touche « Passer à 2 mesures » si on te le propose, puis sur le pad « Caisse », touche les cases en pointillé à la fin de la 2e mesure : un petit roulement qui accélère.
- À toi : touche « Copier sur les mesures suivantes » : il recopie la mesure 1 en suivant les accords : elle descend sur Si♭, puis remonte sur Do.
- À toi : touche « Créer la piste Mélodie », puis les 4 notes en pointillé de la mesure 1 : des notes longues.
- À toi : écris les 3 notes de la mesure 2, puis touche « Copier sur les mesures suivantes » : la phrase est recopiée en suivant les accords.
- À toi : touche « Donner les durées conseillées » : chaque partie prend sa durée : Intro 16 mesures, Intro 2 16, Drop 16, Mini-break 2, Drop 14, Break 16, Drop 2 24 et Fin 16.
- À toi : sur le pad « Clap », touche les cases en pointillé : les temps 2 et 4, et la dernière case, juste avant le tour suivant.
- À toi : ce dernier coup du pad « Clap » doit être doux : fais un appui long sur sa case ; dans le menu qui s'ouvre, sous « Force » (la force du coup), touche « Doux ». « Roulement » (des coups très rapides) et « Triolet » (trois coups serrés) ne servent pas ici.
- À toi : touche « Créer la piste Accords », puis, pour les 2 accords de la mesure 1, la note du bas en pointillé : tout l'accord se pose. Ce sont deux stabs (des accords courts et secs, comme un coup).
- À toi : touche « Créer la piste Mélodie », puis les 3 notes en pointillé de la mesure 1. Le riff en compte 3 à la mesure 1, 4 à la mesure 2.
- À toi : touche « Copier sur les mesures suivantes » : il recopie la mesure 1 en suivant les accords (La, Fa, Do, Sol).
- À toi : touche « Créer la piste Accords », puis la note du bas de chaque accord en pointillé de la mesure 1 : tout l'accord se pose. Il y a 6 accords.
- À toi : écris les 3 notes de la mesure 2, puis touche « Copier sur les mesures suivantes » : la phrase revient plus haut sur les deux accords suivants, une marche, comme dans les hymnes.
- À toi : touche « Donner les durées conseillées » : chaque partie prend sa durée : Intro 16 mesures, Intro 2 8, Montée 8, Drop 32, Break 24, Drop 2 32 et Fin 8.

## Clés retirées de tuto_bass (0)
