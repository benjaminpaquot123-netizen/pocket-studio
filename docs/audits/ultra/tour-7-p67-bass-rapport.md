# Tour 7, p67-bass : rapport de zone (bloc 3 de P6/P7, famille Bass, 2026-11-18)
code de départ : 3cd69a96 (ccr-80af1a1b-mn0wec) ; branche eq/tour-7-p67-bass.
Fait : consignes Facile des 7 cours Bass (dnb, dubstep, jump_up, jungle, liquid_dnb, neurofunk, riddim) raccourcies, une série de gestes par consigne, sans nom de bouton du moteur ; ancien texte gelé en textPro ; gloses passées dans le « pourquoi » de l'étape ; 14 langues traduites (ja, ko, ar, hi relues par un sous-agent neuf). Détail : docs/decisions/tour-7-p67-bass.md.
Exceptions Facile (lisibilité) de mes styles : 981 → 224 lignes, toutes des étapes partagées (partage, tonalité, niveaux, gloses communes), laissées au bloc 4.
Exceptions de moments de mes styles : 86 → 24 lignes, toutes partagées : dnb lead_do/levels/pad_do/pad_end ; dubstep gap/lead_2/levels/pad_do/pad_end ; jump_up levels/pad_2 ; jungle gap/levels/pad_do/pad_end ; liquid_dnb gap/lead_do/pad_do/pad_end ; neurofunk levels/pad_do/pad_end ; riddim pad_do/pad_end.
Pro hors budget (mes styles) : 1256 → 1247 (baisse : raccourcis d'une seule langue, même clé).
Garde-fous du modèle (project-model, hors chemins) : TutoValidationTest, TutoLot4Test, TutoLot5Test, I18nCatalogTest.
Batterie : project-model 224/0, app 251/0 ; PS_TUTO_FAULTS vide ; aucune capture commitée.
Après relecture (2 graves corrigés) : « pourquoi » d'étape = « pourquoi » du chapitre + glose (le chapitre n'est plus caché) ; astuce liquid hat_do juste. 12 nouvelles clés dans les 14 langues, 5 clés de gloses seules devenues orphelines retirées (ajoutées par ce lot). ko : espace avant 탭하세요 (7 clés). ja riddim song_order gardé court (« マイソング » ferait sortir du budget).
Cliquet PENDING_VERB (Tour6TutoYourTurnTest) : vert, aucune entrée de mes styles à retirer.
Tests adaptés pour lire « texte + pourquoi » (accord du chef, chaque exigence gardée) : TutoLot4Test#theFacileWordsAreExplained ; TutoLot5Test#theWordsSayWhatTheScreenShows (clap_do et key).
tests: :project-model:test --tests 'com.pocketstudio.model.tuto.*' --tests 'com.pocketstudio.model.i18n.I18nCatalogTest' --tests 'com.pocketstudio.model.i18n.KoreanRegisterTest' --tests 'com.pocketstudio.model.i18n.Tour7LanguagesTest' :app:testDebugUnitTest --tests 'com.pocketstudio.app.tuto.*'
chemin: panneau du tuto, consignes Facile des cours Bass | test: TutoReadabilityBudgetTest#everyFacileConsigneFitsOrIsAKnownException
chemin: panneau du tuto, consignes Pro des cours Bass | test: TutoReadabilityBudgetTest#proOutOfBudgetOnlyGoesDown

## Clés retirées de tuto_autres (14 langues, suppressions seules)
Copies orphelines de mes anciens textes Bass :
- À toi : crée le Drop 2 : la même énergie que le Drop, avec une autre basse. Écris-la : les notes en pointillé de la mesure 1.
- À toi : sur le pad « Charley » (le « tss » de la batterie), touche les cases en pointillé. Astuce : le bouton « Remplir 1/2 » sous la grille met un coup sur deux d'un seul geste ; il reste ensuite à effacer celui du milieu et à ajouter le dernier.
- À toi : sur le pad « Charley » (le « tss » de la batterie), touche toutes les cases en pointillé des 2 mesures. Astuce : le bouton « Remplir 1/2 » sous la grille met un coup sur deux d'un seul geste ; il reste ensuite à effacer les cases sans pointillé.
- À toi : touche les 2 notes de la mesure 2 : une très longue, et une petite plus haute. Puis « Copier sur les mesures suivantes » : il fait le reste ; à la fin, la basse suit l'accord.
- À toi : touche les 3 notes de la mesure 2, la dernière plus aiguë. Puis « Copier sur les mesures suivantes » : il fait le reste, la basse suit les accords toute seule.
- À toi : touche les 4 notes de la mesure 2. Puis « Copier sur les mesures suivantes » : il fait le reste, la basse suit les accords toute seule.
- À toi : touche les 4 notes de la mesure 2. Puis « Copier sur les mesures suivantes » : il fait le reste.
- À toi : touche les 5 notes de la mesure 2. Puis « Copier sur les mesures suivantes » : il fait le reste.
- À toi : touche « Copier sur les mesures suivantes » : il répète la mesure 1 sur les mesures 2 et 3.
- À toi : touche « Copier sur les mesures suivantes » : il écrit les mesures 2 et 3 en suivant les accords. Touche ensuite les 5 notes de la mesure 4, qui remonte vers l'aigu.
- À toi : touche « Copier sur les mesures suivantes » : la mesure 1 se répète.
- À toi : touche « Copier sur les mesures suivantes » : la mesure 2 arrive, un peu plus bas.
- À toi : touche « Créer la piste Accords », puis la note du bas de chaque accord en pointillé de la mesure 1 : tout l'accord (3 notes jouées ensemble) se pose.
- À toi : touche « Créer la piste Basse » (une piste = un instrument de ton morceau), puis les 2 notes en pointillé de la mesure 1 : elles tombent avec le kick.
- À toi : touche « Créer la piste Basse » (une piste = un instrument de ton morceau), puis les 3 notes en pointillé de la mesure 1, toutes sur la même note.
- À toi : touche « Créer la piste Basse » (une piste = un instrument de ton morceau), puis les 3 notes en pointillé de la mesure 1.
- À toi : touche « Créer la piste Basse » (une piste = un instrument de ton morceau), puis les notes en pointillé de la mesure 1 : elles tombent avec le kick.
- À toi : touche « Créer la piste Mélodie », puis la note tenue en pointillé de la mesure 1.
- À toi : touche « Créer la piste Mélodie », puis les 2 notes en pointillé de la mesure 1 : elles répondent à la basse dans ses trous.
- À toi : touche « Créer la piste Mélodie », puis les 2 notes en pointillé de la mesure 1 : elles tombent dans les trous de la basse.
- À toi : touche « Créer la piste Mélodie », puis les 3 notes en pointillé de la mesure 1. La mesure 2 reste vide : c'est voulu.
- À toi : touche « Donner les durées conseillées » : chaque partie prend sa durée : Intro 16, Montée 8, Drop 32, Break 8, Drop 2 32 et Fin 8.
- À toi : touche « Donner les durées conseillées » : chaque partie prend sa durée : Intro 16, Montée 8, Faux drop 4, Drop 16, Break 16, Drop 2 16 et Fin 8.
- À toi : touche « Donner les durées conseillées » : chaque partie prend sa durée : Intro 24, Montée 8, Drop 32, Break 16, Drop 2 32 et Fin 16.
- À toi : touche « Donner les durées conseillées » : chaque partie prend sa durée : Intro 24, Montée 8, Drop 32, Break 8, Drop 2 32 et Fin 16.
- À toi : touche « Donner les durées conseillées » : chaque partie prend sa durée : Intro 28 mesures, Montée 4, Drop 32, Break 12, Drop 2 32 et Fin 16.
- À toi : touche « Donner les durées conseillées » : chaque partie prend sa durée : Intro 32, Drop 32, Break 12, Montée 4, Drop 2 32 et Fin 16.
- À toi : écris la note de la mesure 2, puis touche « Copier sur les mesures suivantes ».
- À toi : écris la note de la mesure 3, puis touche « Copier sur les mesures suivantes ».
- À toi : écris les 2 notes de la fin de la mesure 2, puis touche « Copier sur les mesures suivantes ».
- À toi : écris les 3 notes de la mesure 2, puis touche « Copier sur les mesures suivantes » : la réponse arrive, la même chose un peu plus bas, sur les accords suivants.
- À toi : écris les notes en pointillé de la mesure 4, puis touche « Copier » encore.
