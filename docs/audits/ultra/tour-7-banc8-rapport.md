# Tour 7 — lot « banc8 » : rapport

Rôle : banc. Aucun fichier de l'appli touché (`app/src/main`, `project-model/src/main` intacts).

## B1 : l'amenée `tuto:<style>:<n>` (fait)
- Le siège qui commence au chapitre n trouve le projet des chapitres 1 à n−1, construit par les vérifications du tuto lui-même.
- Paire : `rouge: tuto anchor builds the skipped chapters' project` → `vert: …`.

## B2 : les rejeux du tour 6 qui rendaient la main trop tôt (fait)
- **Écran changé dans l'appli (normal)** : la boîte « Simple ou Pro ? » est descendue sous le pouce depuis le tour 6. Pour l'écrivain de la fiche : B/b rendait la main au geste 4 sur 17, B/c au geste 1 sur 7, parce que les boutons de cette boîte ont changé de place (mêmes mots, autre « @ x,y »).
- **Banc trop strict** : l'empreinte comparait aussi les positions. Pour un rejeu qui rend la main, elle les ignore maintenant ; un toucher sur un bouton étiqueté qui a bougé vise l'étiquette (ligne de journal « REJEU visé : … déplacé, touché en x,y »).
- Une main rendue avant la fin est écrite dans `rejeu.txt` : « rejeu non fidèle (k gestes de l'origine sur N rejoués) ; écran changé : <ligne> ». Ce siège ne compte pas comme rejeu fidèle.
- Rejoués sur la tête : B/c joue ses 7 gestes puis rend la main (avant : geste 1) ; B/b va jusqu'au geste 5 sur 17 (avant : 4), puis rend la main avec « rejeu non fidèle » : l'écran 6 diffère vraiment, l'icône « dossier ouvert » porte maintenant une coche (« icône (dossier ouvert) ✓ @ 125,42 »). Pour l'écrivain de la fiche : B/b n'est pas un rejeu fidèle.
- Les rejeux `t6-*` sont à oracle (ils lisent seulement le dernier écran). Un seul a dû être ré-ancré : `t6-ga14-phrases-batterie`, geste 5, `remix_pick` → `remix_pick_thumb` (tour 7, lot 2 D3 : « Choisir une chanson » est maintenant une boîte autour du bouton, et toucher la boîte ne faisait rien).
- Paire : `rouge: replay hand-back ignores moved buttons and says why` → `vert: …`.

## B3 et B4 : notés dans `sieges/tour-7/incidents-banc.tsv` (fait)
- Écrans au texte vide (A/a 3 et 102, B/d 42, 48, 55) : tous sur l'anneau du tuto (`tuto_spot_ring`), le lecteur de texte ne voit que lui. Cause banc probable, réparation laissée à un prochain lot banc.
- C/a 27 : l'image de l'écran 28 est en retard sur son texte (capture prise avant la fin du dessin).

## B6 : « Chargement de la liste… » qui reste (prouvé côté appli : pas de défaut)
- Test sans banc : le vrai modèle de « Mes projets », un projet enregistré, un projet neuf créé comme au début d'un cours, puis la liste redemandée comme à l'ouverture : l'attente finit et les deux projets sont listés ; un modèle refait aussi.
- Le modèle ne lit rien à son démarrage : la liste n'arrive qu'à la demande d'AppRoot, et cette demande passe après le travail de démarrage, qui peut attendre les sauvegardes de fermeture et les prises (jusqu'à 10 s chacune). Cause la plus probable chez B/d : une sauvegarde de fermeture encore en cours. Pas reproduit sans banc ; à revoir seulement si un siège le revoit au-delà de 20 s.
- Pas de paire rouge/vert : test de preuve, il ne corrige rien.

## À noter (moyen)
- Suite `com.pocketstudio.app.human.*` : 322 tests, 1 rouge, `OrdinaryTestsLeaveNothingTest#c_anOrdinaryTestThatLeavesTheAppsWorkRunningIsRedItself` (« red itself, by name: null »), pendant qu'un rejeu Gradle tournait à côté. Vert deux fois seul, avec `BenchProjectsListTest` dans la même passe : sensible à la charge, pas lié à ce lot.

## Laissé
- `t4-26-prise-clavier` (pas un rejeu du tour 6) diverge au geste 32 sur la tête : au Clavier à 200 %, les touches 62 et 65 sont refusées (« aucun point visible, à portée du pouce »), la prise ne démarre pas et « record_stop » n'existe pas. L'écran a changé depuis le tour 4 (panneau Guide au-dessus du clavier). Vrai défaut de l'appli (chef) : inscrit dans `incidents-banc.tsv`, cause « appli (guide) », à confier à guide2 ; rejeu laissé rouge, pas masqué.
- Relecture : 3 remarques moyennes laissées (voir `tour-7-banc8-relecture.md`).
- Réparer la lecture des écrans sous l'anneau du tuto et la capture en retard : prochain lot banc.

tests: com.pocketstudio.app.human.BenchAmeneesTest com.pocketstudio.app.human.BenchShortSeatTest com.pocketstudio.app.human.BenchProjectsListTest com.pocketstudio.app.human.*

chemin: Accueil › Tuto de A à Z › chapitre n (amenée tuto:<style>:<n>) | test: BenchAmeneesTest#tutoAnchorStartsWithTheSkippedChaptersBuilt
chemin: Accueil › boîte « Simple ou Pro ? » (boutons déplacés) | test: BenchShortSeatTest#aScreenWhoseWordsOnlyMovedIsTheOriginsForAReplayHandingBack
chemin: Accueil › boîte « Simple ou Pro ? » (bouton déplacé touché) | test: BenchShortSeatTest#aReplayedTouchOnAMovedButtonAimsAtItsTag
chemin: rejeu rendu tôt › rejeu.txt | test: BenchShortSeatTest#aSeatGivenBackEarlySaysTheLineAndIsNoFaithfulReplay
chemin: Mes projets › « Chargement de la liste… » | test: BenchProjectsListTest#theProjectsListIsReadAndShownWithoutTheBench

t6-da15-ecoute-passee : oracle tenu
t6-ec17-un-bout-courbe : oracle tenu
t6-ga14-phrases-batterie : oracle tenu
t6-ha19-cases-panneau : oracle tenu
