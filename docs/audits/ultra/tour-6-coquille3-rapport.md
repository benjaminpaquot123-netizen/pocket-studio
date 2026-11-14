# Tour 6 — lot « coquille3 » (restes de coquille2) : rapport

Branche `eq/tour-6-coquille3`, depuis `eq/tour-6-coquille2` (33e51731), une paire rouge/vert par test. Les phrases laissées « pas prises » par coquille2, avec l'accord du chef (P1 dans le piano roll : rejoué par un débutant neuf sur les chapitres qui y passent, avant l'entrée ; A10 : deux tests hors liste adaptés). Preuves rejouées par le banc dans les conditions du siège d'origine, appli entière (`Coquille3SeatTest`).

Fichiers touchés : `projects/ProjectsScreen.kt`, `ui/AppRoot.kt`, `export/ShareSheet.kt`, `remix/RemixScreen.kt`, `ui/AppSpaces.kt`, `synth/PianoRoll.kt`, `synth/SynthScreen.kt` ; tests `ui/Coquille3SeatTest.kt` (neuf), `audit/BenchContrastRemixLibraryTest.kt`, `nav/SimpleModeAuditTest.kt`, `remix/RemixModelAskTest.kt` (visent le bouton de téléchargement qui reste).

tests: com.pocketstudio.app.ui.Coquille3SeatTest com.pocketstudio.app.audit.BenchContrastRemixLibraryTest com.pocketstudio.app.nav.SimpleModeAuditTest com.pocketstudio.app.remix.RemixModelAskTest

## Tenté, preuve rejouée (banc, conditions du siège)
- **E7** (A c, fr, Simple ; contrôle Ac-2 écran 31), tranché dans le sens du propriétaire (« ← Mes projets » mène à l'accueil, 2026-10-29) : la flèche le dit, « Accueil » avec sa maison, plus « Retour ». `#e7_myProjectsArrowSaysItGoesHome` (rouge 457cfa11, vert e1e703a6).
- **Format en Pro** (A c, geste 13, écran 17) : passer en Pro par « Autres fichiers » garde le format choisi (M4A), jamais WAV tout seul. `#n1_proKeepsTheFormatChosenOnTheSharePage` (rouge 22a7c51f, vert 1eebea06).
- **« détails ? »** (contrôle Ac-3, écran 45, Pro) : la phrase du volume entière, « détails » et son « ? » dessous. `#n5_theVolumeSentenceIsWholeBeforeItsDetails` (rouge 1c023157, vert 25b653a3).
- **A10** (B a, es, main gauche ; contrôle Ba-2 écran 3) : un seul « Descargar el modelo (174 MB) », celui du pouce. `#a10_oneDownloadButtonOnTheRemixPanel` (rouge e445b7e0, vert 8dd54051).
- **A11** (B a ; contrôle Ba-5 écran 6) : « Remix » de « Más » sous le pouce gauche (les tuiles en bas de la page). `#a11_remixOfMoreIsUnderTheLeftThumb` (rouge 35d4d85b, vert a61f5908).
- **P1** (A b, fr, Simple ; geste 34, écran 43) : « Notes » montre les notes jouées (mesures 2 à 4), plus une mesure 1 vide. `#p1_notesShowsEveryNotePlayed` (rouge 4336389b, vert 22087def). La mélodie est posée par `replaceNoteClip` (le banc n'enregistre pas les notes jouées au clavier).

chemin: Plus › Mes projets › ← (fr, Mode simple) | test: Coquille3SeatTest#e7_myProjectsArrowSaysItGoesHome
chemin: Partager › Autres fichiers (Pro, Mix › export) (fr) | test: Coquille3SeatTest#n1_proKeepsTheFormatChosenOnTheSharePage
chemin: Pro › Partager (phrase du volume) (fr) | test: Coquille3SeatTest#n5_theVolumeSentenceIsWholeBeforeItsDetails
chemin: Remix, avant le modèle (es, Mode simple, main gauche) | test: Coquille3SeatTest#a10_oneDownloadButtonOnTheRemixPanel
chemin: Más (es, Mode simple, main gauche) | test: Coquille3SeatTest#a11_remixOfMoreIsUnderTheLeftThumb
chemin: Clavier › Notes (fr, Mode simple) | test: Coquille3SeatTest#p1_notesShowsEveryNotePlayed
chemin: Clavier › Notes › zoom › Rythme › Clavier (fr, Mode simple) | test: Coquille3SeatTest#p1_theZoomChosenAfterNotesStaysWhenTheGridComesBack
chemin: Mix (Pro, FLAC) › Partager › Autres fichiers (fr) | test: Coquille3SeatTest#n1_aFlacChosenInProStays
chemin: Partager › Mes fichiers après un envoi (fr, Mode simple) | test: Coquille3SeatTest#n4_myFilesListTheSongJustSent

## Pas reproduit
- **« Mes fichiers » vide après un envoi** (A c geste 27, contrôle Ac-3 geste 25 ; les deux sièges l'ont marqué « banc? temps ») : sur ce code, le banc liste le morceau tout de suite. Gardé comme test de garde du chemin du siège (`#n4_myFilesListTheSongJustSent`, vert sans changement, commit ad1447fb). À regarder sur le téléphone si un contrôle le revoit.

Ces défauts portent ici leur nom de siège : les numéros de la fiche du chef (N1, N4, N5) sont aussi ceux d'autres défauts du tri (`verdicts.tsv`, piano roll du tuto), que ce lot ne touche pas :
ne corrige pas: N4 : le N4 du tri (flèche « plus bas » du piano roll du tuto) n'est pas ce lot ; « Mes fichiers » vide (le N4 de la fiche) n'est pas reproduit, gardé par un test
ne corrige pas: N5 : le N5 du tri (mesure dite par la ligne du piano roll du tuto) n'est pas ce lot ; « détails ? » (le N5 de la fiche) est tenté ci-dessus

## Relecture neuve (1re passe) : suites
- Grave P1 : la demande de cadrage est celle du toucher, faite une fois ; la grille qui revient d'un autre onglet garde le zoom choisi ; le tuto ne demande jamais. `#p1_theZoomChosenAfterNotesStaysWhenTheGridComesBack`.
- Moyen (FLAC) : un FLAC choisi en Pro reste (la page Partager n'en propose pas). `#n1_aFlacChosenInProStays`.
- Fix : la liste « Plus » du Pro reprend sa mise en page d'avant (`ScreenScanTest` census `NavScreenshotTest.guideFirst`).
- Mineurs laissés (noté) : sans modèle mais avec une chanson dans le projet, le panneau n'a plus de « Télécharger » visible (« Choisir une chanson » le demande) ; « Accueil » sort de l'appli sans projet avant le guide (cas d'avant) ; le Mixeur ouvert directement reste en WAV par défaut ; les tests touchent « WhatsApp » par coordonnées (feuille du téléphone simulée).

## Contrôle (Testeur 1, ccr-a548b78b-controle-coq4 384e3825, code fd8c57a6) : suites
- Constatés : A10, A11.
- **E7, autre** (écrans 6 et 8 : une maison seule, sans mot) : « Accueil » écrit à côté de la maison (mot déjà traduit dans les 14 langues). `Coquille3SeatTest#e7_theHomeWordIsWrittenBesideTheHouse`.
chemin: Plus › Mes projets (le mot à côté de la maison) (fr, Mode simple) | test: Coquille3SeatTest#e7_theHomeWordIsWrittenBesideTheHouse

## Contrôle (Testeur 3, ccr-6a8c6318-dvnp3y fcc443ec, code fd8c57a6 ; Testeur 2, P1) : suites
- Constatés : format en Pro (et FLAC), « détails ? », P1.
- **« Mes fichiers » sur « Chargement de la liste… »** (Cq3, écrans 14-16 et 60-62) : reproduit au banc 1 fois sur 5 (JVM froide), par le chemin du siège (« Mes fichiers » ouvert avant l'envoi puis après). Cause : la liste est lue hors du fil principal (MediaStore) ; le « attends » du banc avance son horloge, pas celle du mur (« attends 5 » prend bien moins de 5 s réelles), donc une lecture lente reste « Chargement » quel que soit le nombre de « attends ». Avec du temps réel, la liste vient toujours. Pas un défaut de l'appli : temps du banc, prouvé par `Coquille3SeatTest#n4_myFilesOpenedBeforeAndAfterASendLoadsInRealTime`. À proposer au banc : « attends » qui laisse aussi finir le travail en arrière-plan.
- **« Autres fichiers » absent au 2e passage (Pro)** (Cq3, écrans 41, 45, 46) : la ligne est là, sous le pli de la partie qui défile (« Tout est prêt » ajoute une ligne au 2e passage) ; les glissés du siège partaient des rangées fixes (M4A/WAV, « Mes fichiers »), qui ne font pas défiler. Pas changé ici ; à voir avec un siège si la ligne doit remonter.
- **Zoom « − » coupé au bord droit en japonais** (Testeur 2, écran 124) : la rangée d'outils du piano roll défile de côté ; non changé (zone du piano roll), à regarder sur le téléphone.
chemin: Partager › Mes fichiers avant puis après un envoi (fr, Mode simple) | test: Coquille3SeatTest#n4_myFilesOpenedBeforeAndAfterASendLoadsInRealTime
