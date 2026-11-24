# Tour 9, zone fluide1 (C6) : rapport

Mission C6 : ce qui redessine trop sur Mix et Live pendant la lecture. Analyse d'abord, une réparation.

Analyse (60 lectures de la lecture simulée, 2 s) :
- Live : 70 recompositions. Tout venait du compteur de la barre (« 3.2 », mesure) : recomposé à chaque pas (20), ses deux textes avec lui (50). Son anneau se dessinait dans la couche de toute la barre du haut, avec un tracé recréé à chaque image. Les cases du Live ont déjà leur propre couche (découpe) : rien à faire, gardé par un test.
- Mix : 77 recompositions, toutes de textes déjà limités à 4 changements par seconde (ligne du limiteur, pastille de volume). Rien à réparer : le chiffre doit rester lisible.

Réparation (une seule) : le compteur ne recompose plus que ses chiffres, une fois par temps ; l'anneau lit l'horloge au dessin, dans sa propre couche, son cadre construit une fois par taille. Live : 70 → 35. Aucun changement d'aspect (captures du Live inchangées).

- rouge : 82d9c9f3d
- vert : 1f6ebbbb1

tests: com.pocketstudio.app.fluide.LivePlayingRecomposeTest com.pocketstudio.app.live.LiveCounterNameTest

chemin: Live, lecture en cours, compteur de la barre | test: LivePlayingRecomposeTest#liveComposesLittleWhilePlaying
chemin: Live, lecture en cours, cases de la grille | test: LivePlayingRecomposeTest#liveCellsDrawInOwnLayers
chemin: Live, ce que TalkBack dit du compteur, dans chaque langue | test: LiveCounterNameTest#theCounterHasANameAndAUnitInEveryLanguage

Statut : tenté (aucun testeur neuf ne l'a encore constaté).
