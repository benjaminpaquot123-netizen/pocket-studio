# Tour 6 — lot 7 « restes » : relecture neuve
Relu : 347f060503d1ed5fe6290b8ab01845195d28b05f

Base 5957fac5, 7 commits (3 paires rouge/vert + docs). Diff lu en entier ; 14 JSON validés (`json.load`), chacun
n'ajoute que la clé « À taper : %s » (la ligne précédente ne change que par sa virgule). Aucun test relancé (pas
nécessaire pour les constats ci-dessous).

## Rouge avant vert : vérifié par lecture
- `Lot7ChallengePadTest` : sans le bloc `padlive_challenge_pads`, le texte lu est vide → rouge. Réel.
- `Lot7RumbleNameTest` : sans `PadVoice.NAMES` dans `EXACT`, `shown("Rumble")` rend « Rumble », pas « 럼블 » → rouge. Réel.
- `Lot7PlayStripNextTest` : sans `queued`/`next`, le test ne compile pas → rouge. Il prouve la fonction et la bande,
  pas l'appli (voir Moyen 1).

## Grave
1. **corrigé** (e610fbc0 rouge → 13535b10 vert : `shownSample`, test anglais « Air horn » sans « Klaxon », rouge
   avec le nom brut). Constat d'origine : `app/src/main/java/com/pocketstudio/app/padlive/Challenge.kt:380` : le nom du pad est pris brut (`clip.name`), sans
   `DefaultNames.shownSample`, alors que tous les autres affichages de nom de pad le passent (`PadLiveScreen.kt:435`,
   `PadModes.kt:51`). Les pads des défis sont surtout des sons de l'appli (`LiveStarter.PADS`, `PartySounds`, voix
   intégrées) sauvés en français : en anglais la ligne dit « Tap: A9 · Klaxon » pendant que le pad affiche son nom
   traduit ; en coréen « Rumble » au lieu de 럼블, ce qui contredit la correction T2 du même lot. Correction en une
   ligne : `DefaultNames.shownSample(it)` ; ajouter au test un cas hors français.

## Moyen
1. U1 n'est pas branché : les deux appelants (`app/src/main/java/com/pocketstudio/app/ui/UnderTopBar.kt:60`,
   `app/src/main/java/com/pocketstudio/app/report/ReportWiring.kt:43`) appellent `playPosition` sans `queued`, donc
   `next` est toujours nul dans l'appli. Le rapport le dit dans « Limite » (honnête), mais le range sous « Corrigé » et
   désigne `ui/AppRoot.kt` alors que le câblage se fait dans `UnderTopBar.kt` (`sessionUi.queued`, déjà collecté là).
   Suite (347f0605) : le rapport le range désormais en « Fait à moitié » et nomme les deux appelants. Reste ouvert :
   le câblage, renvoyé au lot 3.
2. `project-model/src/main/kotlin/com/pocketstudio/model/DefaultNames.kt:41` (+ `:82` et `:115`) : l'effet de bord est
   plus large que ne le dit `docs/decisions/tour-6-restes.md` (« Kick studio » seul). La table inverse
   `fromTranslation` reconnaît aussi « Hard kick », « Studio kick », « Studiokick », « Harte Kick », « Kick duro »,
   « Sert kick »… et `trailingNumber` leurs variantes numérotées (« Hard kick 2 ») : un son importé « Hard kick.wav »
   (nom = fichier sans extension, `PadSamples.kt:223`) s'affiche « Kick dur » en français. Casse exacte requise, et le
   mécanisme est celui déjà accepté pour les autres noms de l'appli : non bloquant, mais à noter honnêtement.
   Suite (347f0605) : la décision le dit désormais. Effet accepté, rien d'autre à faire.

## Mineur
1. Traductions (`project-model/src/main/resources/i18n/*/padlive.json:113`) : justes dans l'ensemble (de, en, es, id,
   it, ja, ko, nl, pt-BR, tr, zh-CN naturelles). À reprendre : **pl** « Stukaj: %s » (« frappe/cogne », imperfectif) →
   le reste de l'appli dit « Dotknij » (« Dotknij: %s ») ; **hi** « टैप करें » (registre poli) alors que le fichier tutoie
   (« करो », 13 occurrences) → « टैप करो: %s » ; **ar** « للنقر: %s » compréhensible mais peu naturel → « انقر على: %s ».
   Suite (9e36e697) : les trois reprises comme proposé ; les 14 JSON restent valides. Fermé.
2. `app/src/main/java/com/pocketstudio/app/ui/PlayStrip.kt:315` : quand une partie est en attente, la mesure disparaît
   jusqu'à ce qu'elle arrive (choix noté dans les décisions) ; la bande ne cache la mesure qu'en lecture de morceau
   avec une partie en attente différente de la courante — pas de masquage à tort constaté.
3. `Lot7ChallengePadTest.twoPadsAreBothNamed` dépend de `PadChallenge.make(2, …)` qui tire bien deux pads distincts ;
   l'assertion `names.size > 1` le protège, correct.

Verdict : 0 bloquant, 0 grave — fusion possible
