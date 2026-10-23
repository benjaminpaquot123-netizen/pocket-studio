# Testeur Amateur 5 : performeur live, Jump up DnB

## Parcours réel
1. Assistant, Jump up DnB. On arrive sur Morceau (liste A/B/C/D, section « Jouer en direct » repliable, bandeau orange « Live : jouer les sections piste par piste » en haut : SessionScreen.kt:245).
2. Page Live (LiveScreen.kt) : barre ← Morceau, ▶/■, « Lancer à 1 mesure », compteur 3.4, REC. Grille scènes x pistes (Batterie, Acide, Synthé, Basse, Voix, Remix). Un pad = lance cette section de cette piste à la prochaine mesure. M = muet, ■ = stop piste, « ■ Tout ». Fiable et lisible sur la capture live_grid.png (anneau de progression, couleurs par piste). Sur téléphone, la grille est coupée en bas (Remix à moitié visible) et le panneau Effets/303/Pads mange 45 % de l'écran.
3. Jam : lancer des pads OK. Effets : Filtre DJ (XY), Répéter, Dub, Montée, Tenus, Espace. Verrouiller / Tout remettre. 303 : XY coupure x résonance + 4 curseurs.
4. ● REC : un toucher arme, la prise démarre à la prochaine ligne de lancement, ■ Fin écrit. Avant, on choisit « Remplacer le morceau » ou « Ajouter à la suite » (bandeau « La prise : », qui défile en horizontal sur petit écran).
5. Résultat : « Morceau écrit : 7 parties, 3 réglages · Annuler · Écouter ». Annuler marche seulement tant que rien d'autre n'a changé.
6. Retour dans Morceau : la prise est une liste de parties (A x2, B x2, « mélange »…), puis la Ligne du temps (clips par piste). Retouches : appui long sur un clip (couper la piste dans cette partie, changer de scène, répéter, couper la partie, dupliquer, supprimer), curseur +/- de répétitions, Monter/Descendre.
7. Export : pas dans le parcours Live ; il faut quitter vers l'écran d'export (non relu ici).

## Ce qui est enregistré (RhythmViewModel.kt:3085-3420)
- OUI : lancements de pads de la grille et stops de piste (en parties de morceau), gestes d'effets du panneau Effets, potards 303, volumes/pans/envois mixeur touchés pendant la prise. Écrits en courbes (16 réglages max, jusqu'à une certaine mesure seulement).
- NON : le bouton M (muet), tous les pads de l'onglet Pads / Pads plein écran (one-shots, boucles perso), toute note jouée. Le bandeau REC le dit : « M et les pads de l'onglet Pads ne sont pas enregistrés » (LiveScreen.kt:575), mais en petit gris sous le chrono.

## Points durs

1. **Bloquant** : les one-shots et pads live ne s'enregistrent pas. Écran Pads live plein écran + onglet Pads (LivePadsFullScreen.kt:228-253, LiveScreen.kt:575). Le plein écran, le plus beau et le plus fun pour jouer, n'a même pas de bouton ● : on joue, et rien ne reste. Un performeur croit enregistrer un impact/une voix et ça disparaît. Idée : ● dans la barre du plein écran, et écrire les déclenchements de pads comme clips sur une piste « Pads » de la Ligne du temps (position au pas près). À défaut, un avertissement clair avant ●.
2. **Bloquant** : aucune note jouable en live. L'onglet 303 (LiveAcid.kt:94-128) = filtre et 4 curseurs seulement, pas de clavier ni de pad de notes ; Synthé/Lead/Voix ne se jouent que par clips préparés. Impossible d'improviser une mélodie ou un riff. Idée : mini clavier / pads de gammes dans le panneau Live (gamme du morceau), avec enregistrement des notes dans le clip de la piste (quantifié au 1/16).
3. **Pénible** : quantification trop grosse et peu claire. « Lancer à » ne propose que 1, 2 ou 4 mesures (Live.kt:21, LiveScreen.kt:484), un seul toucher fait tourner 1 → 2 → 4 sans voir les options. Pas de 1/2 ou 1 temps pour les effets/one-shots. Idée : puces visibles « Direct · 1 temps · 1/2 mesure · 1 mes. · 2 · 4 », réglage par piste possible.
4. **Pénible** : après la prise, nettoyer est dur. Les parties sont découpées à chaque lancement : beaucoup de petites lignes « mélange » (SessionScreen.kt:508). On ne voit pas ce qui a été écrit en effets : « 3 réglages » seulement ; il faut ouvrir le Mixeur par la courbe (TimelineScreen.kt:932, 1549) pour les corriger, pas de gomme ni de rejouer un passage. Idée : bouton « Réenregistrer cette partie » / ● sur une zone bouclée ; rendre les parties fusionnables ; afficher les courbes d'effet sous les clips avec un trait de couleur par effet.
5. **Pénible** : « Annuler » de la prise disparaît dès qu'on touche autre chose (RhythmViewModel.kt:3351-3358) ; ensuite message « utilise ↶ en haut ». Et « Remplacer le morceau » est le choix par défaut : un second ● écrase le morceau généré (récupérable par Annuler seulement). Idée : demander confirmation ou proposer par défaut « nouveau morceau (copie) » ; garder la prise précédente en version.
6. **Pénible** : le ● est petit, étiqueté « REC » et se trouve à droite de la barre, loin du pouce ; le plein écran des pads montre un gros bouton rouge « Tout couper » (StageButton, LivePadsFullScreen.kt:251) qui ressemble à un REC. Confusion REC / stop. Idée : REC rond rouge distinct, « Tout couper » en gris + libellé.
7. **Pénible** : téléphone portrait, grille Live coupée en bas (Remix, voire pistes) et panneau d'effets large (live_grid.png). Le plein écran existe (LiveScreen.kt:389) mais le chemin est caché dans une petite icône. Idée : plein écran par défaut en portrait au lancement du Live, panneau effets en tiroir.
8. **Pénible** : lien pads / pistes pas clair. Il y a deux mondes : la grille (une colonne = une piste du morceau) et les Pads live (boucles/one-shots à part, 8 pads, remplis par l'assistant : Batterie, Basse, Nappe, Sub, Impact, Morceau 4). Les noms se ressemblent (Batterie, Basse) mais ce ne sont pas les mêmes sons ni les mêmes boutons. Idée : un seul mot (« Clips » vs « Boucles perso »), ou fusionner les pads comme une ligne de plus de la grille.
9. **Détail** : l'avertissement de limite d'effets (« Après la mesure N, les effets ne sont plus enregistrés », RhythmViewModel.kt:3316-3325) arrive en cours de prise. Pour un set long (>~ 4 min), on perd les effets sans pouvoir y remédier.
10. **Détail** : Done « Écouter » repart de la première partie de la prise ; pas de « boucler cette prise » ni de « garder / jeter » en deux boutons clairs.
11. **Détail** : le bandeau « La prise : Remplacer / Ajouter à la suite » défile horizontalement (fadingHorizontalScroll) : le second choix peut être caché sur petit écran.
12. **Détail** : export hors du flux Live ; pas de bouton « Exporter » depuis Morceau terminé.

## Ce qui manque
- Enregistrer les pads (one-shots, boucles perso) et le muet M.
- Jouer des notes en live (clavier / pads de notes / gamme) et les enregistrer.
- Quantification fine pour pads et effets (1 temps, 1/2 mesure) et quantifier la prise après coup.
- Rejouer / corriger une zone de la prise (ré-enregistrement local, « punch in »).
- Voir les gestes d'effet enregistrés sur la Ligne du temps de façon lisible (couleur par effet), les effacer d'un geste.
- Sauvegarde de plusieurs prises / « prise 1, 2, 3 » au lieu d'écraser.
- Un seul parcours « Live → Prise → Nettoyer → Exporter » avec un bouton Exporter à la fin.
- Mode plein écran Live (grille + effets) pensé pour le portrait.

## Top 5
1. Les pads live (one-shots, plein écran) ne s'enregistrent pas et le plein écran n'a pas de ● : ajouter l'enregistrement des pads.
2. Impossible de jouer des notes en live (303/synthé : curseurs seulement) : ajouter un clavier ou des pads de notes enregistrables.
3. « Remplacer le morceau » par défaut écrase la base générée ; prévoir copie ou confirmation et garder plusieurs prises.
4. Nettoyage après prise trop lourd : trop de parties « mélange », effets cachés dans le Mixeur ; ajouter ré-enregistrement d'une zone et courbes visibles sur la Ligne du temps.
5. Quantification limitée à 1/2/4 mesures et cachée derrière un toucher cyclique ; afficher des puces et ajouter 1 temps / 1/2 mesure.
