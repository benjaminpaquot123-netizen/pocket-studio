# Relecture neuve du lot remix (tour 5, chantier 10)
Relu : d00e160a

Constats (code lu ; les tests n'ont pas été relancés).

1. **grave** : RhythmViewModel.kt:6312 (`lengthenSongForRemix`). Le bouton « Allonger le morceau » modifie seulement le projet. Il ne prévient pas le moteur. À côté, `insertSongPart` (RhythmViewModel.kt:3496-3501) fait `songPlaying()` puis `followSongEdit`, qui relance `stageSongPart()`. Si la lecture est en cours dans la dernière partie, le moteur garde l'ancienne fin et s'arrête là. Les parties ajoutées ne se jouent qu'à la lecture suivante. Il faut copier ce que fait `insertSongPart` (le même suivi pour une partie ajoutée en fin de morceau).

2. **moyen** : RemixOverrun.kt:45 (`last.copy(bars = n, repeats = 1)`). La copie reprend aussi les champs `muted`, `play` et `from` de la dernière partie. Si le Remix est coupé dans cette partie (`muted` contient `remix`), ou si elle lit un autre passage de la chanson (`play[remix]`), les parties ajoutées sont muettes ou rejouent ce passage. Le message d'alerte disparaît mais la chanson ne s'entend toujours pas. `from` (mesures déjà jouées) est aussi recopié à tort. Mieux vaut vider ces trois champs sur les parties ajoutées, ou au moins retirer `remix` de `muted` et `play`.

3. **moyen** : RemixOverrun.kt:57-58 (`withRemixCutAtSongEnd`). Si la chanson entre au bout du morceau ou après (`atStep >= songSteps`), `end` vaut 0 ou moins et la fonction renvoie le projet inchangé. L'alerte reste affichée et « Couper la chanson à la fin » ne fait rien. Pourtant il y a bien un dépassement, et la bonne issue serait de retirer toutes les pièces. Il faut gérer ce cas : retirer tout, ou ne pas proposer le bouton.

4. **moyen** : RemixOverrun.kt:40-47 et RhythmViewModel.kt:6312. Quand le morceau a déjà 128 parties (`MAX_PARTS`), ou dépasse la limite après ajout, `withSongLengthenedForRemix` s'arrête sans rien dire. Le bouton rend un projet quasi inchangé, l'alerte reste, et l'utilisateur n'a aucune explication (`insertSongPart` affiche un avis dans ce cas). Il faut afficher un avis (« déjà 128 parties ») ou ne rien changer.

5. **moyen** : ShareSheet.kt:242 et RhythmViewModel.kt:6312-6320. L'alerte et les deux corrections utilisent `remix.seconds` seul. Les autres chemins (`remixSecondsNow`, `exportEnd`) retombent sur la longueur des pistes chargées quand le projet ne la connaît pas encore. Si `remix.seconds` vaut 0 alors que les pistes sont chargées, l'export dépasse la fin sans que la feuille le dise. La logique de repli est donc dupliquée et incohérente (risque faible, à confirmer sur le terrain).

6. **moyen** (tests) : RemixOverrunTest.kt et RemixChantier10Test.kt. Rien ne prouve : la mesure non 4/4 (`stepsPerBar` ≠ 16) ; le tempo suivi ou étiré (`songBpm` ≠ `tempoBpm`) ; `atStep` après la fin du morceau ; le plafond de 128 parties ; une dernière partie avec `muted` ou `play` ; le décalage d'une seule double-croche (la tolérance `over > 1`) ; ni `lengthenSongForRemix` / `cutRemixAtSongEnd` du ViewModel (la poussée moteur et l'étape d'annulation). Dans `RemixChantier10Test`, les captures ne comparent rien, et `buildSong` n'est jamais cliqué (le test vérifie seulement que le bouton existe). Les calculs eux-mêmes sont corrects pour le cas testé (480 pas, 23 mesures, coupe à 14 s).

7. **mineur** : RemixOverrun.kt:18. Un dépassement d'une double-croche exacte est ignoré (`over > 1`) alors que `remixEndBars` arrondit à la mesure ; l'export est coupé à la partie, donc c'est cohérent. À noter seulement : à tempo lent (40 BPM) une double-croche dure 0,375 s, ce qui peut s'entendre.

8. **mineur** : RhythmViewModel.kt:6317-6320. `cutRemixAtSongEnd` fait `session.edit` puis `pushRemix()` ; l'ordre est bon et une seule étape d'annulation est créée. En revanche l'annulation d'« Allonger » ne rejoue aucun `followSongEdit` (même défaut que le constat 1, côté annulation).

9. **mineur** : RemixFromHome.kt:259. `buildSong` appelle `vm.stop()` alors que `useSong` appelait `vm.pause()` : l'écoute est remise à zéro. Voulu, mais le chemin « chanson séparée à l'instant, on demande aussitôt » (`asking`) n'est pas couvert par le nouveau bouton ; vérifier qu'il n'y a pas deux dialogues.

10. **mineur** (textes) : remix10.json. Les traductions sont justes et gardent bien `%s` et `…`. Mais en anglais « Allonger le morceau » donne « Lengthen the track » alors que, dans la même feuille, « Modifier le morceau » donne « Edit the song », alors que « chanson » = « song » aussi (« Cut the song at the end »). Le mot « song » désigne alors deux choses (le morceau du projet et la chanson du Remix). Idem en allemand (« Song bearbeiten » / « Track verlängern »). Sans gravité, mais confus pour l'utilisateur. La clé `Utiliser cette chanson` a bien été retirée de `mysounds.json` et n'est plus utilisée dans le code (seulement dans des commentaires).

11. **mineur** (régressions de `heardEnd`/`piecesOf`) : `heardEnd` n'est appelé que par `remixSteps` (Remix.kt:133) et AppRoot.kt:4024. Le changement « une piste vidée compte 0 » est cohérent pour les deux : une piste entièrement coupée n'allonge plus la chanson. Un seul effet à connaître : si toutes les pistes sont vidées à la main, `remixSteps` vaut 0 et `remixEndBars` vaut 0, donc plus d'alerte (voulu, testé).

12. **mineur** (historique) : les deux commits « vert » au sujet légèrement différent de leur rouge (« Korean in 해요체», « exact label, screenshots ») sont bien des suites du même vert (même sujet de base, détail ajouté). Pas un problème.

Verdict : 0 bloquant, 1 grave
