# Tour 6, lot 5 « langues » : textes coupés et mots restés en anglais ou en français (2026-11-13)

Consigne : `docs/audits/ultra/sieges/tour-6/lots-correction.md` (eq/tour-6-tri-gaucher, 974986c4), section « Lot 5 », plus le complément 3 n° 6 de `lecture-recurrents.md`.
Branche : `ccr-09a30009-jy4aua`, partie de `ccr-80af1a1b-mn0wec` (5957fac5). Décisions : `docs/decisions/tour-6-langues2.md`.

tests: *MyFilesNamesTest *MyFilesWaitTest *LanguageListPhoneOnTopTest *LanguageListNamesWholeTest *LiveCounterNameTest *LiveWholeWordsTest *SimpleMixWholeWordsTest *FadingRowsTest *Tour6WordsOnScreenTest *LanguagesScanTest *CaptureMatrixPhone360Test

## Défauts du lot
| défaut | état | ce qui change |
|---|---|---|
| R1 | corrigé dans les fichiers du lot | Mix simple : noms des tranches, des sons, mots de « Sortie » et dB rapetissent avant qu'un mot se coupe ; « Batterie ▾ » ouverte se place au début des tranches. Live : légende REC, lettres et noms des parties, « ■ Tout », noms de pistes et choix de prise sur deux lignes, onglets du dock et du panneau en icônes (nom lu par TalkBack) quand un mot ne tient pas. Live : un nom de piste d'un seul mot (« Syntezator ») reste sur une ligne, plus petit. Liste des langues : chaque nom sur deux lignes au plus |
| R14 | corrigé | « Mes fichiers » : le nom se coupe entre ses mots, deux lignes au plus, l'extension toujours visible ; chaque ligne dans le sens de lecture du nom ; « Renvoyer » passe sous le nom quand la place manque |
| R15 | corrigé | arabe (Pro compris : environ 800 textes, les pads « كاوبل », « ريم », « زاب », les effets du Live « ترديد », « فرملة », « لفّ عكسي »…, « المترونوم », « فيدر »), italien (« cassa », « passo », « linea del tempo », « schede »), polonais (« Syntezator »), indonésien (« birama », « rekaman »), hindi (fade, fin) |
| R16 | corrigé sauf « La menor » | it « su il » reformulé, pl « 5 zakładek », es « COMPASES 2 A 5 », tr titre « Önce ayrıştırma modeli gerekli » |
| R18 | corrigé | la langue du téléphone reste en tête de la liste des langues, hors du défilement |
| R19 | corrigé dans les fichiers du lot | les rangées du Mix simple et du Live fondent du côté où la suite attend, aussi de droite à gauche (`FadingScroll.kt` ignorait le sens d'écriture : en arabe, le premier élément s'effaçait et la suite était coupée net) |
| R20 | corrigé | le compteur rond du Live écrit « mesure » sous son chiffre ; TalkBack lit « Mesure 30 · temps 2 » |
| U22 | corrigé | « Mes fichiers » montre une roue et « Chargement de la liste… » pendant la lecture, puis la liste ou « Rien ici pour l'instant » |
| T16 (mots) | corrigé hors tuto | « NOUVEAU » en japonais et coréen ; « Plaits » présenté comme un synthé dans 14 langues ; « Groove » dans l'écriture de l'arabe, du hindi et du chinois |
| complément 3 n° 6 | défaut réel, corrigé | les noms des parties du morceau dans l'écriture de l'arabe, du hindi et du chinois (comme le japonais et le coréen) |
| Complément 2 | rien pour ce lot | N19 à N28 sont aux lots 2 et 6 |

ne corrige pas: N19 : grave du lot 2 « tuto-consignes » (TutoController.kt) seulement cité dans la ligne « Complément 2 »

Corrigés : 9 sur 9, avec les restes ci-dessous (fichiers d'autres lots).

## Liste des chemins
chemin: Mes fichiers › noms longs avec leur extension en fr de nl ar hi | test: MyFilesNamesTest#eachNameKeepsItsExtension
chemin: Mes fichiers › lecture des fichiers puis « Rien ici pour l'instant » | test: MyFilesWaitTest#whileTheFilesAreReadAWaitSignShowsThenNothingHereYet
chemin: Mes fichiers › lecture des fichiers puis la liste | test: MyFilesWaitTest#whileTheFilesAreReadAWaitSignShowsThenTheList
chemin: Réglages › Langue › la langue du téléphone en tête | test: LanguageListPhoneOnTopTest#thePhonesLanguageIsOnTopAtTheOpening
chemin: Réglages › Langue › chaque nom de langue entier à 200 % | test: LanguageListNamesWholeTest#eachLanguagesOwnNameIsWhole
chemin: Live › compteur rond : mesure et temps dans les 15 langues | test: LiveCounterNameTest#theCounterHasANameAndAUnitInEveryLanguage
chemin: Live › textes entiers avec la police du téléphone à 130 % | test: LiveWholeWordsTest#everyTextIsWholeWithThePhonesFontAt130
chemin: Live › textes entiers avec la police du téléphone à 200 % | test: LiveWholeWordsTest#everyTextIsWholeWithThePhonesFontAt200
chemin: Live › textes entiers avec la taille de l'appli à 130 % | test: LiveWholeWordsTest#everyTextIsWholeAtTaille130
chemin: Live › noms des parties entiers dans les rangées hautes | test: LiveWholeWordsTest#everyBrickNameIsWholeInTallRows
chemin: Live › nom de piste d'un seul mot en polonais : Syntezator entier sur 6 colonnes | test: LiveWholeWordsTest#aOneWordTrackNameIsNeverSplit
chemin: Live › noms des parties entiers dans les rangées hautes à 130 % | test: LiveWholeWordsTest#everyBrickNameIsWholeInTallRowsWithThePhonesFontAt130
chemin: Live › onglets du panneau entiers avec la taille de l'appli à 130 % | test: LiveWholeWordsTest#thePanelsTabsAreWholeAtTaille130
chemin: Live › onglets du panneau entiers avec la police à 130 % | test: LiveWholeWordsTest#thePanelsTabsAreWholeWithThePhonesFontAt130
chemin: Live › onglets du panneau entiers avec la police à 200 % | test: LiveWholeWordsTest#thePanelsTabsAreWholeWithThePhonesFontAt200
chemin: Mix simple › textes entiers avec la police du téléphone à 130 % | test: SimpleMixWholeWordsTest#everyTextIsWholeWithThePhonesFontAt130
chemin: Mix simple › textes entiers avec la police du téléphone à 200 % | test: SimpleMixWholeWordsTest#everyTextIsWholeWithThePhonesFontAt200
chemin: Mix simple › textes entiers avec la taille de l'appli à 130 % | test: SimpleMixWholeWordsTest#everyTextIsWholeAtTaille130
chemin: Mix simple › batterie ouverte : son nom au début des tranches | test: SimpleMixWholeWordsTest#theOpenDrumsSayTheirNameOnScreen
chemin: Rangée qui défile › fondu du côté de la suite dans les deux sens | test: FadingRowsTest#aRowFadesOnlyOnTheSideWhereMoreWaitsInBothDirections
chemin: Mix simple › tranches qui dépassent : fondu du côté de la suite | test: FadingRowsTest#theMixStripsFadeWhereMoreStripsWait
chemin: Mix simple en arabe › tranches qui dépassent : fondu du côté de la suite | test: FadingRowsTest#theMixStripsFadeWhereMoreStripsWaitInArabic
chemin: Mix simple › tranches qui tiennent : aucun fondu | test: FadingRowsTest#theMixStripsThatFitFadeNowhere
chemin: Mix simple en arabe › tranches qui tiennent : aucun fondu | test: FadingRowsTest#theMixStripsThatFitFadeNowhereInArabic
chemin: Live › colonnes qui dépassent : fondu du côté de la suite | test: FadingRowsTest#theLiveColumnsFadeWhereMoreColumnsWait
chemin: Live en arabe › colonnes qui dépassent : fondu du côté de la suite | test: FadingRowsTest#theLiveColumnsFadeWhereMoreColumnsWaitInArabic
chemin: Structure › nommer une partie : puces des sections en ar hi zh ja ko | test: Tour6WordsOnScreenTest#thePartChipsAreWrittenInTheLanguagesOwnScript
chemin: Rythme en arabe Mode pro › « Tous les pads » | test: Tour6WordsOnScreenTest#theArabicRhythmScreenSaysAllPadsInArabic
chemin: Piano roll en arabe › aucun mot anglais | test: Tour6WordsOnScreenTest#theArabicPianoRollHasNoEnglishWord
chemin: Que veux-tu faire ? › carte Pad Live : « NOUVEAU » en japonais et coréen | test: Tour6WordsOnScreenTest#newIsSaidInJapaneseAndKorean
chemin: Réglages en polonais › puce du Mode simple : 5 zakładek | test: Tour6WordsOnScreenTest#thePolishSettingsCountFiveTabsRight
chemin: Mix › courbe sur le morceau en espagnol : compases 1 a 20 | test: Tour6WordsOnScreenTest#theSpanishSongCurveSaysWhichBars
chemin: MIDI › bouton réglé sur Plaits : « synthé » dans la langue | test: Tour6WordsOnScreenTest#theMidiKnobOnPlaitsSaysItIsASynth
chemin: Tous les écrans en fr de ar hi zh à 100 et 200 % › mots entiers et mots du métier | test: LanguagesScanTest#screenReadsWhole

## Laissé de côté (fichiers d'autres lots ou choix notés)
- « La menor » (pt-BR) : le nom de note vient de `NoteNames.kt`, hors du lot ; il faut y ajouter le solfège pt-BR (Dó, Fá, Lá).
- « Plaits » écrit en dur dans `InstrumentPanel.kt`, `TrackSheet.kt` (lot 2) et `Tracks.kt`.
- Les fichiers du tuto (lot 2) citent encore les anciens mots (« Snare », « Drop 2 », « bar »). Le passage se fait à la fusion avec le lot 2 par `docs/audits/ultra/outils/tour-6-langues2/apply_to_tutos.sh <dossier i18n> apply` (essai à blanc : ar 1165, hi 422, zh 463, id 581 textes ; pour l'italien, seulement « kick »).
- Trois débordements de mots arabes à 200 % sur tablette, lisibles en entier, dans les fichiers d'autres lots : le pad « طبل كبير » (`RhythmScreen.kt`, lot 1), la pastille de partie « A · مقدمة » (`AppRoot.kt`, lot 3), le bouton de boucle « تكرار » (`PlayStrip.kt`, déjà ouvert au tour 5). Ils sont dans la liste `OPEN` de `LanguagesScanTest`, avec leur raison.
- Environ 500 mots latins restent en arabe : noms de genres et d'effets (« Acid », « Compressor », « Piano roll », « Log drum »). Le hindi en Pro garde « Kick » et « Snare » (décision du tour 5). Le chinois garde « Lead » et « Pad » hors des noms de parties.
- « 3,2 Mo » peut passer à la ligne entre le nombre et l'unité (`Tour5AppProof3Test` exige l'espace normale).
- Le titre de la fenêtre Remix en turc n'a pas de test à l'écran (fenêtre ouverte seulement après le téléchargement du modèle) ; il est prouvé par `Tour6LanguagesTest` (project-model).
- Les mots coupés et les fondus vus par les équipes dans des fichiers d'autres lots, pour ces lots :
  - `LiveDock.kt` : « BPM » à 200 % (15 langues), « Lancer à » à 130 et 200 % ;
  - `LiveGridLook.kt` : « Cases » en arabe à 200 % ;
  - `ui-components/Help.kt` : le « ? » sort de son rond à 130 et 200 % ;
  - `ui-components/WholeWords.kt` : `splitsAWord` ne voit pas les signes combinants, un mot hindi se coupe avant sa voyelle (« आउटपुट ») partout où `WholeWordsText` sert (contourné par `FitText` dans les fichiers du lot) ;
  - `AppRoot.kt` (lot 3) : onglets sans fondu (« Ma chanson préférée », « Üret », « Remix Ayrıştır ») ;
  - `CutScreen.kt` (« Guard / ar », pt-BR 200 %), `RemixScreen.kt` (« Baixar o modelo (174 », pt-BR 200 %), `ShareSheet.kt` (« Tout est prêt : aucun solo oublié… »), `PadLiveScreen.kt` (« EFFETS LIVE »), `RecordBar.kt` et `AppRoot.kt` (« Garder ce que je viens… »), `TrackAdd.kt` et `AppSpaces.kt` (« + Pis… ») ;
  - rangées sans signe de suite, non vérifiées : « Instellingen » (nl), « + 트랙 » (ko), « 303 basu » (pl), « Boucle », « Geschlossene Hi-Hats » (de) ; et la fin des listes de « Mes fichiers » et des langues.
- « Syntezator » (polonais, R15) se coupe en « Syntezato / r » dans une tranche de la console du Mix Pro sur un téléphone de 360 dp : `Console.kt`, dans les fichiers d'aucun lot (règle notée dans `CaptureMatrixTest`).

## Preuves
- Chaque défaut : un commit « rouge: » (le test seul, qui échoue sur son assertion) puis « vert: ».
- Les textes : `Tour6LanguagesTest` (project-model, 8 tests) et `Tour6WordsOnScreenTest` (appli, 7 écrans), ce dernier vu rouge sur les catalogues de 5957fac5 puis vert.
- Captures regardées en entier, téléphone 360 dp, à 100 %, 130 % et 200 % : « Mes fichiers » en fr, de, nl, ar, hi ; la liste des langues en ar, de, pl, hi ; le Mix simple (plié, ouvert) et le Live en de, it, pt-BR, hi, ar ; les 8 captures arabes réenregistrées (Rythme, Mix, Live effets, Structure, Réglages, Guide, carte et panneau du Guide), regardées en entier avant et après ; les débordements des balayages regardés sur image (Sons, Structure et Plus en arabe à 200 % ; Mix Pro et Live en polonais à 360 dp ; Live en japonais à 8 cases).
