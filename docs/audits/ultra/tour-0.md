# Audit ultra, tour 0 : l'équipe de correction

Trois sièges (a : parent pressé, premier morceau, FR ; b : fan de remix, EN, 411 dp, 130 %, Simple ; c : producteur, Pad Live, tablette, Pro).
Grappes : `clusters.md` du tour. Règle §4 : corrigé = un test rouge avant, vert après. Banc = le banc ment là où un téléphone a son moteur.

## Corrigé
| Grappe | Verdict | Gravité | Test | Commit |
|---|---|---|---|---|
| b#46 « → Pad » sans fin, puis plantage du siège | banc : moteur du banc lâché pendant le travail du tableau | bloquant | `HumanRemixJourneyTest` | 34d56bb |
| b#51 « Découpe en phrases… », b#63 « → Live » sans fin | banc, même cause | grave | idem | 34d56bb |
| c#5 REC « Prêt » pour toujours | banc : la prise lit l'horloge du moteur ; le moteur du banc la donne et reste jusqu'aux pads rebondis | bloquant | `BenchAmeneesTest.padLiveLightsStopsAndRecordsAsOnAPhone` | 1b38738 |
| c#23 « Pas assez de mémoire pour enregistrer » | banc : pas d'enregistreur sans moteur natif ; `BenchResample` | bloquant | idem | 1b38738 |
| c#2 pad A1 pas allumé | banc : les lumières viennent du moteur | grave | idem | 1b38738 |
| c#43 « Partager » sans la perf | banc : conséquence de c#5 | grave | idem (la prise entre dans le morceau) | 1b38738 |
| c#28 « Arrêter » laisse les boucles | banc : sur un téléphone ■ coupe les pads (`toggleClip(-1)`) | moyen | idem | 1b38738 |
| c#43 mode Pro demandé, Simple vu | banc : le premier lancement remettait Simple | moyen | `BenchSeatOptionsTest.aProSeatIsInPro` | 1b38738 |
| c#34 Pack Techno, page B vide | banc : le pack se rend par le moteur ; `BenchPackMaker` (plan de l'appli, sons doux) | grave | `BenchAmeneesTest.aPackFillsThePageShown` | 1b38738 |
| a#5, a#13 chaque « Écouter » de l'assistant échoue | banc : écoutes rendues par le moteur, et l'AudioTrack de Robolectric refuse le float ; ombre `BenchAudioTrack` | grave | `BenchAssistantTest` | 1b38738 |
| a#16 guide « Écouter » : « Montée mes. 25 », rien ne joue | banc : `playSongPart` attend le moteur ; il vient pour ▶ et le guide | grave | idem | 1b38738 |
| a#17 « Jouer » joue « Intro mes. 1/8 » | banc, même cause | mineur | idem | 1b38738 |
| a#14 sons du pack « illisibles » | banc : pas de décodeur FLAC ; `BenchFlac` (durée lue dans STREAMINFO) | moyen | `HumanLucasBugsTest.aDownloadedPacksSoundsAreRead` | 1551290 |
| a#14 « Roland Juno-106 · Roland Juno-106 » dans le message | appli : le son d'une partie porte déjà le nom du pack | mineur | `HumanLucasBugsTest.aPackIsNamedOnceWhenItsSoundsCannotBeRead` | 1551290 |
| a#12 « C'est prêt : le pack Juno-106 » seul (4 demandés) | appli : la dernière note écrasait les autres ; 15 langues | moyen | `PackReadyNoteTest` | d1b8d60 |
| b#16 « Drag handle » truffé de marques | banc : « en » sans région lisait le pseudo-anglais ; siège en-rUS | mineur | `BenchSeatOptionsTest.anEnglishSeatReadsTheLibrariesPlainWords` | 34d56bb |
| b#16 curseur du volume du pad sans nom | appli | mineur | `BigTextLabelsTest.thePadSheetsSlidersHaveTheirNames` | aa5fc52 |
| a#9 « choix sans nom » des ambiances | appli : la carte est le choix | moyen | `ScreenScanTest[wizardMood]` | aa5fc52 |
| b#1 « Si… », « Keyboar/d », « Structur/e » à 130 % | appli : les mots rapetissent avant de casser | mineur | `BigTextLabelsTest` | aa5fc52 |
| b#29 « 4 onglets » pour 5 | appli | mineur | `WordsThatMatchTest.simpleModeCountsItsOwnTabs` | aa5fc52 |
| b#74 « Remix-morceau », « 808 modernes », « brick » | appli | mineur | `WordsThatMatchTest`, `PackCatalogTest`, `I18nCatalogTest.englishSaysBlockForABrique` | aa5fc52 |

## Pas un défaut / voulu / idée
- a#5 contraste 3,37:1 du message d'échec : pas un défaut (palette à 9,3:1, mesure sur une image en fondu) ; le message ne vient plus sur le banc.
- b#5 séparation lente : vitesse voulue du banc (`remixTimeScale`).
- c#17 menu de pad (muet, solo, quantification) : idée.
- c#2 « Jouer » quand seuls les pads tournent : voulu (« pads seuls »), à trancher si un 2e profil bute.

## Appli, reste (non corrigé ce tour)
- b#11 Remix en Simple : aucune voie vers les pads (grave pour le siège, un seul profil) : à confirmer au tour 1 par un 2e siège Simple (§4.4), puis trancher.
- b#36 écran « Découper » : refait le même jour (3056251, b9069ac), à revoir au tour 1 ; `ScreenScanTest` census `CutScreenshotTest.cutScreenOnePiece*` rouge (aide, contraste), lot de l'équipe découpe.
- b#35 la bulle « ● REC / ✎ / ⋯ » couvre les onglets Voix et Synthé en Pro (moyen).
- b#62 pistes du tableau écrasées à ~10 px (moyen) ; c#12 filtre tenu sans signe à l'écran (moyen).
- c#34 côté appli : un pack qui ne donne aucun son ne le dit pas (mineur).

## Banc, reste
- Le moteur du banc va et vient encore par geste : un enregistrement de voix pendant que le morceau joue le fait partir (morceau rejoué en boucle) ; les écoutes de la bibliothèque hors assistant n'ont pas de moteur.

## Vérifications (2026-11-04, soir)
- Verts : tout `com.pocketstudio.app.human.*` (dont `HumanRemixJourneyTest`), `PackReadyNoteTest`, `AppTextsTest`, `WizardPlainWordsTest`, `BigTextLabelsTest`, `WordsThatMatchTest`, `MuteSoloExclusiveTest`, `PackCatalogTest`, `I18nCatalogTest` (dont `englishSaysBlockForABrique`).
- Rouges avant correction, vérifiés : `aPackIsNamedOnceWhenItsSoundsCannotBeRead` (« Boom bap, Boom bap · Boom bap »), `PackReadyNoteTest` (« Roland TR-909 » seul).
