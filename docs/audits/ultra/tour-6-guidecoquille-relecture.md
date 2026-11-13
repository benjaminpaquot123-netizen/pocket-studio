# Relecture du lot « guidecoquille » (tour 6, lot 3)
Relu : f9a62e8cef888137a68f0ba126c4991bbece5c54

Trois relecteurs neufs (processus séparés, lecture seule, aucun n'a écrit le code) : la relecture complète ci-dessous a lu le lot à 461866df18e2dc08ac62177e7aacaa5cd4401efa ; deux relectures de delta ont lu les deux commits suivants, qui ne changent que le rapport de zone (jusqu'à f9a62e8cef888137a68f0ba126c4991bbece5c54, le commit nommé ci-dessus). Une première relecture complète, à 41bd7820, est résumée en fin de fichier. Seule la forme des titres « Bloquant » et « Grave » a été retouchée pour le portail.

J'ai lu le diff complet des cinq fichiers source et le code autour dans `AppRoot.kt`, `GuidePanel.kt`, `GuideViewModel.kt`, `DoneBar.kt`, `AppSpaces.kt`, plus `FadingScroll.kt` et `TutoController.kt` dont le lot dépend. J'ai lu les tests `Tour6*` sauf `Tour6GuideNextStepTest`, `Tour6MixerNamesTest`, `Tour6ModeSwitchAsksTest` et `Tour6GuideCoquilleLookTest`, les retouches de `SpacesTest` et `SimpleModeAuditTest`, le rapport, les décisions, la section « Lot 3 » et les verdicts. J'ai regardé trois captures entières (`shell_guide_panel_more_simple_100`, `shell_audio_pages_tr_130`, `shell_done_line_simple_130`). Rien n'a été lancé (lecture seule) : les chiffres de tests du rapport ne sont pas revérifiés ici.

## Bloquant : aucun

## Grave : aucun

## Constats
- (moyen) **Clavier d'un écran court : la barre du Guide part toute seule sous les doigts.** `AppRoot.kt:2115` : `keysTakeGuideLine` dépend de `guideSpot == null`, et `guideSpot` s'éteint seul après 8 s (`GUIDE_SPOT_MS`, l. 2246). Après « Voir le réglage » vers le Clavier (`GuideGo.Record`, `GuideGo.Tab` avec repères) sur un écran de moins de 700 dp, la barre reste 8 s puis disparaît. La page gagne alors sa hauteur pendant que le débutant joue, donc les touches bougent sous le doigt. À voir en 360 x 600 dp : carte qui mène au Clavier, attendre 8 s. À changer : ne rendre la ligne qu'au prochain changement d'écran, pas à l'extinction du repère. Aucun test ne joue ce cas.
- (moyen) **R19 en arabe : l'estompe est probablement du mauvais côté.** `fadingEdges` (`FadingScroll.kt:23`) dessine en coordonnées physiques (`canScrollForward` = bord droit). `MoreAside` est aligné sur `CenterEnd` (`AppRoot.kt:3395`), donc à gauche en RTL. Par lecture du code, en arabe « › » serait à gauche et l'estompe à droite, sur le premier onglet entièrement visible. Les tests ne couvrent que le turc et le français. À vérifier par une capture en arabe à 130 % ; correctif dans `FadingScroll.kt` (lot 5), qui touche aussi les rangées déjà estompées.
- (moyen) **T13 : dans le projet d'un tuto quitté, le Guide disparaît tant que la progression existe.** `tuto.resumable` (`TutoController.kt:205`) reste vrai tant qu'une progression est enregistrée pour le projet, et je n'ai trouvé aucun effacement dans ce fichier. `guideFree` (`AppRoot.kt:2100`) masque alors la barre. Les ouvertures automatiques (`AppRoot.kt:379`, `389`, `399`) mettent `guideOpen = true` sans rien montrer. Seul Plus › Aide rouvre le Guide. Le rapport le note comme « moyen laissé » ; c'est cohérent avec « un seul guide à la fois », mais à suivre au prochain tour.
- (moyen) **U27 rouvre un défaut déjà signalé.** Le filtre retiré (`AppRoot.kt:3272`) venait des testeurs du lot « tuto 4 » (b/139 : tranche muette dans le projet du tuto). La décision est écrite et réversible, mais aucun test ne garde l'ancien grief et le rapport ne dit pas qu'on échange un défaut contre l'autre.
- (moyen) **Rapport plus large que la preuve sur G1.** Le test rouge demandé couvrait « Plus, Live pads, clavier, parties, export ». Le lot prouve Plus, une page de Plus et le clavier. « Parties », « export » et le cas « Annuler » du siège A c #21 ne sont ni testés ni listés dans « Reste ».
- (moyen) **`GestureBudgetTest#aStrokeCutShortIsStillItsOwnUndoStep` rouge sur la branche.** Le rapport l'attribue à un autre lot par raisonnement (« aucun fichier du lot sur son chemin »), sans passage sur la base `e57f1bc`. Plausible, non prouvé : à trancher à l'intégration avant fusion sur main.
- (moyen) **U17, limite connue confirmée.** Après « Annuler », la page Remix rouverte se ferme sur le projet (`closeRemixHome`, `AppRoot.kt:1638`, après `launch.settled()`), pas sur l'accueil. Sur le chemin « autre projet », le projet a déjà changé. Ce chemin n'est pas joué par le test, et le rapport le dit.
- (mineur) « ↓ La suite » fait 40 dp (`GuidePanel.kt:274`), sous les 48 dp de `docs/DESIGN.md` (principe 4).
- (mineur) Sur la capture `shell_guide_panel_more_simple_100`, la phrase « À toi : touche « Ouvrir le clavier » » nomme un bouton caché sous le pli. « La suite » le signale, mais le bouton nommé devrait être visible sans geste.
- (mineur) Sur la capture `shell_audio_pages_tr_130`, « › » est posé sur les lettres du mot coupé (« Tun›e »). Le signe se lit, le mot moins.
- (mineur) La ligne « La suite » fait grandir puis rétrécir le panneau de 40 dp en bout de carte (`GuidePanel.kt:234`). Pas de boucle (la fenêtre de défilement garde sa taille), mais la page saute pendant le geste. La ligne « Fait » fait de même pendant 6 s. Déjà noté au rapport.
- (mineur) `GuidePanel.kt:457` : si « Plus tard » est le premier choix et qu'un vrai choix le suit, la carte n'a plus aucun gros bouton. Le gros bouton devrait aller au premier choix non « later ».
- (mineur) Tests qui peuvent passer sans rien prouver : les trois `theOpenGuidesCutCardSaysMoreWaits*` et `theGuidesOwnWayToAScreenKeepsItsBar` sortent par `Assume` si la carte tient ou n'ouvre rien. `SimpleModeAuditTest#theKeysAreAboveTheGuidesBarAt130` accepte désormais l'absence de barre (`guide == null ||`), donc à 130 % il ne vérifie plus que la hauteur des touches.
- (mineur) `tutoHoldsTheLine` appelle `tuto.resumable` (lecture des préférences) à chaque recomposition d'`AppRoot` (`AppRoot.kt:2098`), alors qu'avant c'était seulement dans la barre du bas.
- (mineur) Ouvrir le Live avec le Guide ouvert le replie maintenant (changement de `tab`), alors que le commentaire l. 2101 dit encore « the panel only hides there, it stays open ».
- Vérifié, juste : les quatre textes neufs (« La suite », « Pad Live », « Arrêté », « Ton morceau est sauvegardé. ») existent dans les 14 langues hors français ; aucun fichier de traduction touché.
- Vérifié, juste : G2. `I18n.lang` est posé dans `SettingsStore.update` avant l'émission (`Settings.kt:141`), donc le recalcul lit la bonne langue ; le test compare titre, raison et barre.
- Vérifié, juste : repli du panneau. `guideOpenedOn` est en `remember`, donc rotation et reconstruction repartent de l'écran courant. `guideGo` réécrit l'écran après ses propres déplacements (`go` est synchrone). L'ouverture sur un morceau neuf est rattrapée par le collecteur `welcomes` (l. 375) si le repli passe avant la reconstruction.
- Vérifié, juste : N1/T13. `guideOverTuto` retombe à chaque repli (l. 2083) ; la pastille et le côté en paysage suivent `guideShown`.
- Vérifié, juste : N16. Le toast part sur le fil principal après `flushNow`, et « sauvegardé » n'est dit que si `unsaved` est faux et sans erreur. Le test « rien ne jouait » existe.
- Vérifié, juste : N17 (la carte ferme et ne change que si le mode diffère), U9 (drapeau remis à zéro à la fermeture, conservé à la restauration), U17 (origine posée à chaque départ d'import, plus de drapeau persistant).
- Vérifié, juste : N15. La ligne est dans la colonne de la barre du bas, et le test compare les rectangles sur tous les onglets, en Simple, Pro et 130 %.
- Vérifié, juste : rien n'est ajouté au thread audio ; code et commentaires en anglais.

## Première relecture (41bd7820, autre relecteur neuf, avant le correctif)
Elle avait trouvé un défaut de gravité haute, repris ici pour mémoire ; la relecture ci-dessus a lu le code d'après.

### Grave : le panneau ne se repliait plus après un choix du Guide qui ne change pas d'écran
`guideGo` effaçait `guideOpenedOn` pour toutes ses destinations, y compris celles qui ne changent pas d'écran (« Écouter », le tempo, le guide du morceau) ; l'écran suivant choisi par l'utilisateur devenait alors « chez lui » pour le panneau, qui restait ouvert par-dessus (le défaut du siège A a #16 revenait). **corrigé** : test rouge 19e35771 (`Tour6GuideCoversTest#afterAChoiceThatChangesNoScreenThePanelStillFoldsOnLeaving`), commit vert suivant (`guideGo` écrit l'écran où le panneau se trouve, seulement s'il reste ouvert).

### Ses autres constats
Ses constats moyens sur U17 (drapeau qui pouvait rester levé), N16 (phrase « sauvegardé » non vérifiée) et l'icône « ‹ » ont été repris dans le commit de8eb29a ; les autres sont dans le rapport de zone, section « Après la relecture neuve ».


## Relecture du delta (461866df → 23234e1f)

J'ai lu le diff entre les deux commits, le rapport entier à 23234e1f, puis vérifié ses phrases neuves dans le dépôt : `GestureBudgetTest.kt` (lignes 472-510), `Tour6GuideCoversTest.kt` en entier, `docs/audits/ultra/tour-5-jank-rapport.md`, `scripts/bench/fusion-equipes.sh` et `rapport-zone.py`, les traces du portail de morceauexport, l'histoire git, et le tri sur la branche locale `eq/tour-6-tri`. Rien n'a été compilé ni lancé : les passages de tests cités restent sur parole.

- « juste » : (1) un seul commit (23234e1f), un seul fichier, `docs/audits/ultra/tour-6-guidecoquille-rapport.md`, 4 lignes remplacées ; aucun code, test, capture ni fichier du banc.
- « juste » : `GestureBudgetTest#aStrokeCutShortIsStillItsOwnUndoStep` dessine bien `RhythmScreen` seul dans `PocketStudioTheme`, avec le ViewModel de `vmWithSong()`, sans `AppRoot` ni `AppHarness`. Aucun des cinq fichiers du lot n'est sur son chemin.
- « juste » : ce test est bien une ligne « chemin: » de `tour-5-jank-rapport.md` (ligne 46), et `*GestureBudgetTest` est dans la ligne « tests: » de ce rapport.
- « juste » : « a7f8ed62 puis e57f1bc6 » : les deux sont ancêtres de la tête, par les fusions f34d4696 et 461866df. ea294587 est aussi dans l'histoire. Le diff de e57f1bc6 à la tête sous `app/src/main` ne touche que les cinq fichiers annoncés.
- « juste » : les écrans cités pour G1 comme sans test (parties, export, « Annuler » pendant la fabrication, A c #21) ne sont pas joués par `Tour6GuideCoversTest`. Ses dix tests couvrent Plus (pouce, haut, Réglages en Pro), « Voir le réglage », la carte coupée et le clavier (court et grand).
- (moyen) « le portail rejoue ce test (chemin du lot « jank » du tour 5) » est plus fort que sa preuve. `fusion-equipes.sh` ne rejoue que l'auto-test du banc, les rejeux et les filtres du rapport de la zone fusionnée, plus ceux passés à la main. La ligne « tests: » de guidecoquille ne contient pas `GestureBudgetTest`, et le rapport du tour 5 n'est pas relu à une fusion du tour 6. Le rapport dit lui-même le test rouge sur e57f1bc6, alors que le portail a fusionné morceauexport sans le voir (filtres de `fusion.txt` sans `GestureBudget`). À écrire plutôt : « le portail ne le rejoue pas tout seul ; à passer en filtre ou à la grande vérification ».
- (moyen) La liste des écrans de G1 sans test est incomplète. Le tri cite aussi Live pads (`lots-correction.md` ligne 67 ; « Wszystkie pady », F b #1), les cases des pas coupées (B c écran 4) et la ligne de la partie B (F a œil neuf). Aucun n'a de test dans `Tour6GuideCoversTest`, et la phrase ne les nomme pas.
- (moyen) « la barre et le panneau y prennent leur ligne sans rien recouvrir, et les pages défilent » est affirmé sans test, dans la phrase même qui dit qu'il n'y en a pas. Le verdict G1 du tri dit justement que sur ces écrans « rien ne remonte la cible ». À ramener à « non vérifié par un test ».
- (mineur) « rouge de la même façon avec les cinq fichiers du lot remis à leur version de l'intégration […] : il ne vient pas du lot » : la déduction est bonne si le passage a eu lieu, mais aucune trace n'en reste (ni journal, ni commit, ni ligne dans `docs/decisions/tour-6-guidecoquille.md`). « attendu 11, obtenu 10 » colle avec l'assertion `depth + 1` de la ligne 503, sans plus.
- (mineur) « deux passages verts […] 203 verts, 0 rouge, 0 en quarantaine, 44 sautés » : aucune trace du second passage dans le dépôt. La trace du portail sur le même commit e57f1bc6 donne d'autres chiffres (`selftest-resume.txt` : 176 verts, 17 sautés). L'écart tient peut-être à un périmètre différent, mais le rapport ne le dit pas, et un seul jeu de chiffres est donné pour deux passages.
- (mineur) La phrase retirée (« RhythmScreen.kt a changé avec le lot 1 ») était une piste utile et vérifiable : 5c72d686, « a cell touched while playing keeps its bar in sight », touche `RhythmScreen.kt` et est dans l'intégration. La nouvelle version ne dit plus où chercher.


Bilan de ce delta : aucun bloquant, aucun défaut de gravité haute. Ses trois constats moyens ont été repris dans le rapport au commit suivant.

## Relecture du second delta (23234e1f → f9a62e8c)

- « juste » Le delta ne touche qu'un fichier, `docs/audits/ultra/tour-6-guidecoquille-rapport.md` (3 lignes retirées, 3 ajoutées), et chacune des trois phrases affirme maintenant moins qu'avant :
  - **Écrans de G1** : la phrase dit qu'ils n'ont pas de test à eux et ne sont pas vérifiés par ce lot. `Tour6GuideCoversTest.kt` ne couvre en effet que les pages de Plus, le pliage du panneau, la carte coupée et la page du clavier ; rien sur les pads du Live, les cases des pas, la ligne de la partie B, les parties, l'export ni « Annuler ».
  - **Piste du test rouge** : 5c72d686 (« vert: a cell touched while playing keeps its bar in sight ») ne modifie que `RhythmScreen.kt` (11 lignes), est bien dans e57f1bc6 et y est arrivé par tutogrille, que son rapport nomme « lot 1 ». `GestureBudgetTest` n'est pas dans la ligne « tests: », dessine bien `RhythmScreen`, et figure dans la ligne « tests: » du rapport du lot « jank » du tour 5. Le mot « Piste » ne prétend pas que ce commit est la cause.
  - **Selftest** : la phrase reconnaît elle-même que ses traces ne sont pas dans le dépôt.
- (mineur) Les renvois aux sièges ajoutés dans la première phrase (« Wszystkie pady » F b #1, B c écran 4, F a, A c #21) ne se relisent pas dans le dépôt : `lots-correction.md` et `verdicts.tsv` sont absents de `docs/audits/ultra/sieges/tour-6/` à f9a62e8c, et « Wszystkie pady » n'apparaît nulle part ailleurs sous `docs/audits/ultra`. L'attribution au tri repose donc sur la parole du rapport. Cela n'affaiblit pas l'aveu « non vérifié ».
- (mineur) Trois constats restent sans preuve relisible : « 203 verts, 44 sautés à chacun des deux », « lancé avec un dossier de lot vide », et le passage rouge sur la base e57f1bc6. Le rapport le dit lui-même, donc la phrase n'est pas plus forte que sa preuve, mais ce sont des affirmations de session. La grande vérification devra rejouer `GestureBudgetTest`, comme le texte le demande.


Bilan de ce delta : aucun bloquant, aucun défaut de gravité haute.

Verdict : 0 bloquant, 0 grave — fusion possible
