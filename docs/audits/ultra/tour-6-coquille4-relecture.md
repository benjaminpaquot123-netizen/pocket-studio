# Relecture neuve — lot coquille4 (tour 6)
Relu : e707fbd4

Portée : `git diff eq/tour-6-coquille3...HEAD`. 3e passe : ajouts depuis 20ff33ea relus. Ce sont la fusion d43b8408 (traces du portail de coquille3), 48e5a734 (l'avis : `notice_close` et `HelpAbout`), c6970419 (`sendToWhatsApp()` dans Coquille2/3SeatTest) et e707fbd4 (rapport). Gradle n'a pas été lancé. Les passes 1 et 2 tiennent toujours (motif « écrit sur le fil principal », test `StateWrittenOnMainTest` rouge puis vert, rejoué par script).

## Fusion d43b8408
- `origin/ccr-80af1a1b-mn0wec` contient `eq/tour-6-coquille3`. Le côté apporté ne touche que des documents : `DECISIONS.md`, les traces du portail coquille3 et les tableaux du tour 6. Aucun fichier de l'appli ni de test, aucun conflit avec ce lot.

## Recensement (`RemixSongScreenshotTest.matter`, HELP 45 > 42 + marge 2)
- `notice_ok` → `notice_close` (`AppRoot.kt:1095`) : « Compris » appelle `dismissNotice`, la même action que fermer la fenêtre. Le classer « évident » par le motif `.*_close` de `ScreenScanTest.OBVIOUS` est honnête : c'est bien une sortie. Plus aucune trace de `notice_ok` dans `app/src` ; `HumanRemixJourneyTest` suit.
- `HelpAbout(["notice_lengthen"])` posé sur la phrase de l'avis (`AppRoot.kt:1103`), seulement quand « Allonger le morceau » est proposé. Sur le fond, la phrase explique bien le bouton (« sa fin sera coupée » / « elle continue de jouer pendant que la boucle se répète »). Voir le constat moyen ci-dessous sur la forme.

## Tests de sièges (`sendToWhatsApp()`)
- Avant, les tests touchaient l'écran à l'aveugle (`touche 294 543`) après 40 « attends » sans rien vérifier. Désormais, ils attendent que le fichier soit fait (`!running && files.isNotEmpty()`) et l'**affirment**, sinon ils échouent avec l'état de l'export. Ils touchent « WhatsApp » par sa ligne à l'écran quand le menu du banc est là. C'est plus fort qu'avant, pas plus faible.
- e2 (`export_stale` absent) : l'affirmation « fichier fait » empêche maintenant un vert à vide (sans fichier, pas de « a changé » non plus). Le choix « WhatsApp » ne change rien à `export_stale`. Prouve toujours ce qu'il dit.
- n1 et n5 (`sentThenPro`) : ils lisent le format de l'export Pro et la place de « détails » ; le menu de partage n'y joue aucun rôle. Prouvent toujours ce qu'ils disent.
- n4 et n4b (« Mes fichiers ») : la liste lit les fichiers faits (`MyExports.list`), que le partage ait été touché ou non. L'attente en temps réel et les affirmations n'ont pas bougé. Prouvent toujours ce qu'ils disent.
- `runCatching { tap("share_bilan_ok") }` dans e2 : même tolérance que dans les tests de Coquille3 déjà relus. Le geste suivant (`share_other`, `performScrollTo`) échoue bruyamment si la page n'est pas la bonne.
- La raison donnée (le `shareMenu()` du banc ne lit que la prochaine activité lancée, `HumanTester.kt:1274-1276`) est conforme au code. La réparation est laissée au lot Banc, comme il se doit (hors zone).

## Constats
- **moyen** **corrigé** (passe 2) — `export/video/VideoSheet.kt:218` : même motif avec `Dispatchers.Default`. Regex étendue à `IO|Default` et aperçu écrit sur le fil principal ; rouge puis vert vérifiés.
- **moyen** — `ui/AppRoot.kt:1103` : `HelpAbout` est défini (`ui-components/.../Help.kt:42-46`, KDoc de `HelpCoverageTest`) comme « les contrôles qu'un point « ? » explique ». Le poser sur un `Text` sans « ? » sort de ce contrat. La phrase explique bien le bouton, donc ce n'est pas une acceptation déguisée sur le fond, mais elle passe par le code au lieu de la voie prévue par le recensement (« fix them, or accept them with a reason » dans `recensement-acceptes.tsv`). À faire, au choix :
  - élargir la KDoc de `HelpAbout` à « un « ? » ou la phrase qui l'explique dans la même fenêtre », pour que la règle soit écrite ;
  - remettre la phrase sans `HelpAbout` et noter `notice_lengthen` avec sa raison dans `recensement-acceptes.tsv`.
- **mineur** — `Coquille2SeatTest.kt:43-55` / `Coquille3SeatTest.kt:42-54` : `sendToWhatsApp()` est copié deux fois. Sa KDoc dit « started by « Partager », not one left from before », mais rien ne le vérifie : un `files` laissé par un export d'avant dans la même VM arrêterait l'attente tout de suite. Sans effet aujourd'hui (une seule exportation par test, VM neuve). Plus tard, côté banc : le mettre dans `HumanTester` et comparer aux fichiers d'avant `share_go`.
- **mineur** — `StateWrittenOnMainTest.kt:12` : `* *` en double dans la KDoc.
- **mineur** — `StateWrittenOnMainTest.kt` : faux négatifs connus (`x.y = …`, `rememberCoroutineScope().launch`, `NonCancellable + Dispatchers.Default`, toute mention de `Dispatchers.Main` dans le bloc, accolades dans les chaînes, lecture cachée derrière une fonction). Ils sont listés dans les limites du rapport, sauf `NonCancellable + …`. Aucun cas dans les écrans aujourd'hui.

Verdict : 0 bloquant, 0 grave
