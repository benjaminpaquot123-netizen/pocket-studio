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
  partage Android) devient « Envoyer le projet (.zip) ». Le vrai « Sauvegarder » de Mes projets est
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

tests: `app:testDebugUnitTest --tests 'com.pocketstudio.app.export.Tour6Exports*'` (+ `Lot6UiShareLevelTest`).
- chemin: page Partager (Mode simple) | test: Tour6ExportsSimpleTargetTest (E1 chips + chiffre)
- chemin: page Partager (Mode simple) | test: Tour6ExportsOtherFilesClearTest (E3)
- chemin: Mes fichiers | test: Tour6ExportsNameWrapTest (E4)
- chemin: page Partager, ligne projet | test: Tour6ExportsSaveLabelTest (E6)
- chemin: page Partager (Mode simple), niveau | test: Lot6UiShareLevelTest (mis à jour au nouveau format E1)

Note (conditions) : ces tests sont des tests d'écran (Robolectric), ils ne valent pas la preuve « dans
les conditions du siège, appli entière, par le banc » : état « tenté » jusqu'au rejeu d'un testeur neuf.

## Demandes au chef (fichiers hors de ma liste)

- E2 + N1 : `RhythmViewModel.kt` (madeHash/staleFor pour E2 ; format du Pro pour N1), `mixer/MixerScreen.kt`,
  `ui/AppRoot.kt` (exportStale).
- E7 : `ui/AppRoot.kt` (BackHandler/goHome après un export).
- N2 : `ui/AppRoot.kt` (ordre d'affichage de la barre « Guide » au-dessus des dialogues).

## Laissés de côté

- E5 (taille/« · 3 ») : artefact du banc, rien dans le code poussé ; à revérifier sur téléphone.
