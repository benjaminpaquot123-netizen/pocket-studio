# Tour 6 — zone Exports (lot « exports2 ») — rapport

Siège d'origine : tour-6 A c (`docs/audits/ultra/sieges/tour-6/lotA/c/`). Conditions du siège
(`start.txt`) : français, téléphone, police 100 %, Mode simple, mission « exports » (un fichier
« streaming », un fichier « club », retrouvés dans Mes exports).

Fichiers touchés : `app/src/main/java/com/pocketstudio/app/export/ShareSheet.kt`,
`app/.../export/MyExports.kt`, et leurs tests. Rien d'autre.

## Les sept phrases d'acceptation

- **E1** (sortir un fichier « streaming » et « club » en ≤ 2 gestes sans passer en Pro ; l'écran dit le
  volume du fichier, même mot et même chiffre qu'en Pro) : **tenté**.
  Le Mode simple n'offrait aucun réglage de volume : il fallait toucher « Autres fichiers » et basculer
  en Pro. Ajout de `SimpleTargetPicker` sur la page « Envoyer » : chips en mots simples (« Streaming »,
  « Club », « Auto »), sans LUFS, qui règlent la même cible du projet que le Pro (`chooseTarget`), donc
  un fichier streaming ou club est à deux gestes (chip + « Partager »). La ligne « Niveau » porte
  désormais le chiffre du fichier (« Niveau : −8,5, … »), comme le « Volume : −8,5, … » du Pro.
- **E2** (pas de « Le morceau a changé depuis cet export » si je n'ai rien changé) : **pas pris**.
  Le message et son calcul (`RhythmViewModel.madeHash` / `staleFor`, affichage dans
  `mixer/MixerScreen.kt`, `exportStale` dans `ui/AppRoot.kt`) sont hors de ma liste de fichiers.
  Demande : voir « Demandes au chef » ci-dessous.
- **E3** (la ligne « Autres fichiers » entière et lisible, jamais recouverte par les puces M4A/WAV) :
  **tenté**. Un espace défilant de la hauteur du fondu (`Spacer(SCROLL_FADE)`) en fin de contenu permet
  de faire défiler la dernière ligne entièrement au-dessus du fondu.
- **E4** (nom de fichier jamais coupé au milieu d'un mot/nombre : une ligne avec « … » au milieu) :
  **tenté**. `MyExportsList` : nom en `maxLines = 1`, `TextOverflow.MiddleEllipsis`.
- **E5** (taille vraie, pas « 1 Ko » pour un WAV ; pas de « · 3 » sans explication) : **pas pris**.
  `MyExports.detail()` affiche la taille réelle du fichier (`MediaStore.SIZE`) ; le « 1 Ko » du siège
  est un artefact du banc (ses WAV font réellement 1 Ko). Aucun « · N » sans explication n'existe dans
  « Mes fichiers » sur la tête poussée (la ligne est « projet · date · taille »). Rien à corriger dans
  le code ; à revérifier sur un vrai téléphone.
- **E6** (« Sauvegarder » sauvegarde ; un bouton qui envoie s'appelle « Partager »/« Envoyer ») :
  **tenté**. La ligne « Sauvegarder le morceau en .zip » de la page Partager (un geste = menu de
  partage Android) devient « Partager le projet (.zip) ». Le vrai « Sauvegarder » de Mes projets est
  conservé.
- **E7** (après un export, « Retour » revient à l'écran d'où je viens, pas à l'accueil) : **pas pris**.
  La navigation « Retour » (`BackHandler` → `goHome` dans `ui/AppRoot.kt`) est hors de ma liste.
  Demande : voir ci-dessous.

## Les deux défauts « pas revus » par le testeur (rapport, `revu: non`)

- **N1** (geste 13, écran 17) : en passant en Pro depuis Partager, le format choisi en Simple (M4A)
  devenait WAV sans décision de l'utilisateur. Phrase : « Quand je passe en Pro depuis l'écran
  Partager, le format reste celui que j'avais (M4A) ; il ne devient pas WAV tout seul. » Le format du
  Pro vient du Mixeur (`RhythmViewModel` / `mixer/MixerScreen.kt`), hors de ma liste. **Pas pris** —
  demande ci-dessous.
- **N2** (geste 21, écran 23) : pendant « Fabrication du son · 75 % », le bouton « Annuler » du dialogue
  était coupé sous la barre « Guide ». Phrase : « Pendant « Fabrication du son », le bouton « Annuler »
  reste entièrement visible, pas recouvert par la barre « Guide ». » Le dialogue est `ShareProgressDialog`
  (export/ShareSongUi.kt, dans ma zone) mais le recouvrement vient de l'ordre d'affichage de la barre
  « Guide » (overlay d'`ui/AppRoot.kt`). **Pas pris** — demande ci-dessous.

## Tests

tests: :app:testDebugUnitTest --tests 'com.pocketstudio.app.export.Tour6Exports*' --tests 'com.pocketstudio.app.export.Lot6UiShareLevelTest' --tests 'com.pocketstudio.app.human.BenchSelfTest'
chemin: page Partager (Mode simple) | test: Tour6ExportsSimpleTargetTest#theSimplePageOffersStreamingAndClubWithoutGoingPro
chemin: page Partager (Mode simple) | test: Tour6ExportsSimpleTargetTest#theSimpleLevelLineCarriesTheFilesNumber
chemin: page Partager (Mode simple) | test: Tour6ExportsOtherFilesClearTest#theOtherFilesLineCanClearTheFadeBand
chemin: page Partager, ligne projet | test: Tour6ExportsSaveLabelTest#theProjectZipLineIsNamedAfterWhatItDoesWhichIsToSend
chemin: page Partager (Mode simple), niveau | test: Lot6UiShareLevelTest#theSimpleLevelIsTheExportsTarget
chemin: C'est prêt (Mode simple) | test: Tour6ExportsBilanNumberTest#theSimpleReportGivesTheFilesNumber
chemin: Prêt à partager ? (Pro) | test: Tour6ExportsProLevelTest#theProLevelIsTheFilesTarget
chemin: Prêt à partager ? (Pro) | test: Tour6ExportsProLevelTest#theWordsUnderClubSayClub
chemin: page Partager (Mode simple) | test: Tour6ExportsTargetInSightTest#streamingAndClubAreInSightWhenThePageOpens
chemin: Prêt à partager ? (Pro) | test: Tour6ExportsWholeLinesTest#theProSheetOpensWithWholeLines
chemin: page Partager (Mode simple) | test: Tour6ExportsWholeLinesTest#theSimplePageOpensWithWholeLines
chemin: Mes fichiers | test: Tour6ExportsNameWrapTest#aLongNameBreaksOnlyBetweenItsPartsAndStaysWhole
chemin: Mes projets › ⋮ | test: Tour6ExportsZipMenuWordTest#theZipThatIsSentIsCalledSendEverywhere

Note (conditions) : ces tests sont des tests d'écran (Robolectric), ils ne valent pas la preuve « dans
les conditions du siège, appli entière, par le banc » : état « tenté » jusqu'au rejeu d'un testeur neuf.

## Demandes au chef (fichiers hors de ma liste)

- E2 + N1 : `RhythmViewModel.kt` (madeHash/staleFor pour E2 ; format du Pro pour N1), `mixer/MixerScreen.kt`,
  `ui/AppRoot.kt` (exportStale).
- E7 : `ui/AppRoot.kt` (BackHandler/goHome après un export).
- N2 : `ui/AppRoot.kt` (ordre d'affichage de la barre « Guide » au-dessus des dialogues).

## Laissés de côté

- E5 (taille/« · 3 ») : artefact du banc, rien dans le code poussé ; à revérifier sur téléphone.

## Aller-retour 1 (contrôle Ac-2, code testé d946d916)

- E1 corrigé (écran 8) ; le bilan « C'est prêt » n'avait pas le chiffre : ajout de « Volume · −8,1 » sous la ligne
  « Niveau » (Mode simple, sans LUFS).
- E3 encore là en Pro (écran 40, « Autres fichiers » coupé à l'ouverture) : le `Spacer` ne jouait qu'une fois défilé.
  Le défilement s'arrête désormais au-dessus d'une ligne entière que le bord couperait (`SnapColumn`/`snapHeight`,
  lignes marquées `WHOLE_LINE` : vidéo, « Autres fichiers », .zip, « Tout est prêt », « Volume visé » en Simple).
- E4 encore là (écran 19, « …clu…61113… » avec le « … » au milieu) : le nom est montré entier et ne passe à la ligne
  qu'entre ses parties (après « - »/« _ », avant « . »).
- E6 « autre » : le libellé vu était celui de Mes projets › ⋮ (accordé par le chef : ProjectsScreen.kt l. 338 et 345)
  et le titre du menu de partage (SaveProjectZip.kt) : tous disent « Partager le projet (.zip) », traduit dans les
  14 langues (partage.json, accordé).
- Neuf N3 (écran 40, Pro, « −13,0 comme streaming » sous Club) : les deux modes disent le volume du fichier (sa
  cible) avec les mêmes mots « Volume : −8,0, … » ; la mesure entendue reste sous « détails ».
- Pas pris (hors lot) : le texte d'aide « Partager › Sauvegarder le morceau en .zip » (clé d'aide de partage.json,
  écran d'aide) et le titre du menu de partage écrit dans `ui/AppRoot.kt` (projet illisible) gardent l'ancien mot.
- Rejeu du parcours du siège Ac-2 dans le banc (start.txt du siège : fr, téléphone, 100 %, Mode simple ; appli entière,
  46 gestes par étiquettes, attentes allongées pour les exports du banc, oracle à l'écran 47 = Pro « Prêt à partager ? »
  sans « Autres fichiers » coupé) : « oracle tenu ». Vu dans ce rejeu : chips Streaming/Club touchés en Simple sans
  défiler (après correction : la cible passe avant « Tout est prêt »), bilans « Niveau : à la cible (Club) » puis
  « Volume · −8,0 », Mes fichiers aux noms entiers coupés entre leurs parties, Mes projets › ⋮ « Partager le projet (.zip) ».
  Le rejeu a aussi montré « −8,0, très fort (les plateformes le baisseront) » sous Club en Pro : corrigé (mots de la cible).

## Contrôles

controle: Ac-2 | code testé: d946d916 | bloqués: 0 | graves neufs: 0
controle: Ac-3 | code testé: 35930493 | bloqués: 0 | graves neufs: 0

Verdict du contrôle Ac-3 (35930493), buts 3/3 :
- E1 constaté corrigé (écrans 12, 17) ; E4 constaté corrigé (32) ; E6 constaté corrigé (23) ; N3 constaté corrigé (44).
- E3 : tenté (le testeur n'a plus vu de ligne « Autres fichiers » coupée ni entière ; « Mes fichiers » entière en
  Simple, écran 8, et en Pro, écran 45) ; pas de nouvel essai.
- E2, E5, E7 : pas pris (hors liste de fichiers).
- Neufs notés, pas pris : « Mes fichiers » vide une fois juste après les envois, plein au 2e passage (écran 39) ;
  en Pro, « détails » et son « ? » s'insèrent au milieu de la phrase du volume (image 45).

Copie des sièges de contrôle (texte seul) : `docs/audits/ultra/sieges/tour-6/controle/Ac-2/` et `Ac-3/`
(branche ccr-6a8c6318-dvnp3y, c28d043a).

## Après le contrôle Ac-3 (CI 276 rouge sur 35930493)
- `LexiconWordsTest.oneWordPerThingInFrench` : « Envoyer le projet » est un mot ancien (réponse 1A de Benjamin,
  2026-11-09 : « Partager » est le seul verbe pour un fichier qui sort). Le libellé devient « Partager le projet (.zip) »
  partout (page Partager, Mes projets › ⋮, titre du menu de partage, 14 langues) : E6 tenu (« Partager »).
- `ScreenScanTest` (recensement de NavScreenshotTest.simpleSend) : les puces « Volume visé » du Mode simple n'avaient
  pas de « ? » : ajouté, en mots simples (Streaming, Club), sans LUFS.
chemin: page Partager (Mode simple) | test: Tour6ExportsSimpleTargetHelpTest#theSimpleTargetHasItsHelp
