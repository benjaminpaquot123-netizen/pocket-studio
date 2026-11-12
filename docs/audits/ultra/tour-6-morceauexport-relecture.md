# Tour 6 — lot 6 « morceau-export-padlive » : relecture neuve
Relu : d81a5839d264c3acd05cc0aadb308097c0432191

Base 5957fac5. Lu : les 47 commits, le diff de `app/src/main` (7 fichiers touchés sur les 9 permis : PadLiveRoute.kt et
ProjectsViewModel.kt inchangés ; rien d'autre dans `app/src/main`), les 18 tests Lot6*, NewSongPartSceneTest, la fiche
du lot, les verdicts, le rapport et les décisions. Tests non relancés (lecture seule).

## Ordre rouge → vert
Chaque correction a son commit « rouge » (test seul, ou test plus un `testTag` / un champ vide nécessaires à la
compilation : 0fc659f9 ajoute `padlive_more_dots`, 96298b5c le champ `exportLufs`) avant le « vert ». Lus un par un,
les tests rougiraient sans la correction (Fin laissait jouer, Écouter remettait `Ready`, B seul dans le morceau, aucune
notice dans `useSong`, deux `padlive_share`, `padlive_more_dots` ouvert, grille poussée par la bande, etc.).
Deux retouches de test après le vert, légitimes : 43684e6f (l'édition « autre partie » passe par le ViewModel, plus
forte) ; ba20c4c3 (`aRowThatFitsSaysNothing` : nom court et sans bouton ♪ pour une rangée qui tient vraiment).
R8 : test vert du premier coup, dit tel quel dans le rapport (non reproduit sans banc) — conforme à la règle.

## Bloquant
Aucun.

## Grave
Aucun. Points sensibles vérifiés :
- `RhythmViewModel.kt:10721` écouteur `SharedPreferences` : référence forte dans un champ (le registre est faible),
  inscrit dans `init` après `_export`, retiré dans `onCleared` : pas de fuite, pas d'accès avant initialisation.
- `finishPerformance(stop)` : l'arrêt passe par `toggleTransport` alors que `_take` vaut `Writing` et `performance`
  est nul : `endPerformanceOnStop` ne rappelle pas `finishPerformance` (pas de boucle) ; l'arrêt par ■ passe
  `stop = false` ; `pushAutomation` toujours fait. Écriture du morceau (`editQuietly`, version gardée) inchangée.
- `madeHash` (`:447`) : `switchScene(0)` passe par `syncScenes`, aucune édition de la partie affichée n'est perdue
  dans l'empreinte (le test 43684e6f le prouve par le ViewModel).
- Aucun code du thread audio touché.

## Moyen
- `PadLiveScreen.kt:274-276` (U21, couché) : la décision dit « debout et couché », le test ne couvre que debout
  360×780. Couché, la grille est limitée par la hauteur (`side = min(byWidth*1.15, byHeight)`) : quand « Perf gardée »
  ou un message apparaît sous elle, les pads gardent leur haut mais rétrécissent encore. Même chose debout sur un
  petit écran où la hauteur limite. Le rapport (« les pads gardent taille et place ») est plus fort que la preuve.

## Mineur
- `RhythmViewModel.kt:447` (U3) : avec un morceau, un export Pro « Boucle » choisi à la main (`ExportWhat.Loop`,
  `:11289`) rend la partie affichée ; changer de partie ne dit plus « Le morceau a changé depuis cet export ».
- `RhythmViewModel.kt:10721` (U5) : l'écouteur ne se déclenche que si la valeur change ; Simple en M4A, Mix passé en
  FLAC, puis M4A re-choisi en Simple : le Mix reste en FLAC. Clé « format » dupliquée (`SHARE_FORMAT_KEY` contre la
  clé privée de `SharePrefs`) : à garder en phase.
- `ShareSheet.kt:530` (U4/U15/N7) : la bande `scroll_hint_fade` est une Box vide (un repère pour le test) ; le fondu
  vu vient de `fadingVerticalEdges`, que le test ne mesure pas. La ligne reste coupée, estompée : le rapport dit « ne
  recouvre plus rien », c'est « se voit coupée avec un signe ». Le fondu de 24 dp est discret (noté « À surveiller »).
- `MixerScreen.kt:663-665` (R23) : `filesAtOpen` est mémorisé par composition de la carte ; un fichier refait sous le
  même nom (même seconde) ne serait pas ramené à l'écran. Improbable (nom horodaté).
- `RhythmViewModel.kt:3498` (U28) : sur un morceau vide, un `at` donné est ignoré (place 1). Sans appelant connu.
- `PadLiveScreen.kt:276` : `Spacer(Modifier.height(0.dp))` ne sert qu'à garder l'écart de 8 dp de `spacedBy` ; un
  `padding(bottom = 8.dp)` sur `notes` le dirait mieux.
- Rapport : la section « Laissé de côté (3) » liste cinq puces (U1, U9, R8, plus « Boum boum » et T2 hors fr/en/de,
  qui ne sont pas des défauts du lot) ; le compte 21/24 est juste.
- NewSongPartSceneTest (c1f2ed69) : modification légitime, l'attente suit le nouveau comportement voulu (A avant B) et
  ce qu'il prouve (la copie ne prend jamais la partie vide, celle-ci reste vide) est inchangé.

Mise à jour (d81a5839, rapport seul) : les remarques sur le rapport sont corrigées (U4 « estompée », le test vérifie le repère ; U21 « debout 360×780 testé, couché non corrigé » ; titre « Laissé de côté » juste). Les constats sur le code restent ouverts.

Verdict : 0 bloquant, 0 grave — fusion possible
