# Tour 7, p67-hiphop-autres : rapport de zone (bloc 4 de P6/P7, 2026-11-18)
code de départ : 3cd69a965 ; branche eq/tour-7-p67-hiphop-autres.
Fait : consignes Facile des 11 cours hip-hop et autres (boom_bap, drill, grime, jersey_club, lofi, phonk, trap, afrobeats, pop, reggaeton, synthwave) réécrites pour tenir le budget de lecture et un seul moment ; textes Pro gardés tels quels (`textPro` = ancien texte) ; 14 langues ; nouvelles phrases communes dans tuto_moments.
Exceptions vidées : 851 lignes Facile (readability-facile-exceptions.txt) et 113 lignes de moments (tuto-moment-exceptions.txt) de ces cours. Exceptions Facile restantes à ce lot : 0.
Pro hors budget : 1122 avant, 1122 après.
Relecture des langues ja, ko, ar, hi par un agent neuf : 1 grave (ko, durées des parties de pop coupées), corrigé (460142ab6) ; 7 mineurs, 2 repris (ja), les autres laissés (tournures comprises, budget serré).
Batterie : project-model 1238/0, app ciblés 3/0 ; PS_TUTO_FAULTS vide.

Tests adaptés (Kotlin de test, accord du chef) : Tour6TutoYourTurnTest, quatre entrées retirées de PENDING_VERB (suppression seule) : boom_bap/ohat_do/f, grime/perc_do/f, lofi/perc_do/f, lofi/snare_do/f.

Anciennes clés gardées dans tuto_autres (encore utilisées par d'autres familles ; le dernier lot fusionné les retirera) :
- « À toi : touche « Copier sur les mesures suivantes » : les mesures 2 et 3 s'écrivent. » : industrial_techno, jump_up, minimal, techno (et textPro de boom_bap, drill, lofi)
- « À toi : touche « Copier sur les mesures suivantes » : la mesure 3 s'écrit (la même, plus haut). » : idem
- « À toi : touche « Copier sur les mesures suivantes » : les 2 notes reviennent plus haut à la mesure 3. » : idem

Phrases communes ajoutées dans tuto_moments (14 langues) :
- À toi : recopie sur les mesures suivantes.
- À toi : touche les N notes en pointillé de la mesure 1. (N = 2, 3, 4, 5, 6)
- À toi : écris les N notes de la mesure 2. (N = 4, 5)
- À toi : touche la note du bas de chaque accord en pointillé de la mesure 1 : tout l'accord se pose.
- À toi : « Je le fais » ouvre la fiche Tonalité : touche les deux boutons entourés.
- À toi : sur le pad « Kick » / « Caisse » / « Charley », touche les cases en pointillé des 2 mesures.
- À toi : sur le pad « Kick studio » / « Caisse », touche les cases en pointillé. ; À toi : sur le pad du kick, touche les cases en pointillé.
- À toi : sur le pad « Cymbale », touche la toute première case.
- À toi : dernière mesure, efface les coups du dernier temps.
- À toi : sous « Ajouter », touche dans l'ordre : … (deux ordres, pop)
- À toi : donne à chaque partie sa durée : Intro 4 mesures, Couplet 8, Pré-refrain 4, Refrain 16 (8 la 2e fois, 16 la 3e), Post-refrain 4, Pont 8, Fin 4.
- À toi : dans l'onglet « Mix », baisse un peu les curseurs « Accords » et « Mélodie ». Sous le curseur, « Normal » devient « −2 ».

tests: :project-model:test :app:testDebugUnitTest --tests '*TutoReadabilityBudgetTest' --tests '*TutoOneMomentShownTest'
chemin: panneau du tuto, chaque consigne Facile hip-hop et autres dans son état | test: TutoReadabilityBudgetTest#everyFacileConsigneFitsOrIsAKnownException
chemin: panneau du tuto, consignes Pro | test: TutoReadabilityBudgetTest#proOutOfBudgetOnlyGoesDown
chemin: panneau du tuto, Facile, bouton de préparation attendu | test: TutoOneMomentShownTest#facileShowsTheNeedAloneAndProKeepsItsText
