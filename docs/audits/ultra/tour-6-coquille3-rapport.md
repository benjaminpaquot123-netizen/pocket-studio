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
chemin: Partager › Mes fichiers après un envoi (fr, Mode simple) | test: Coquille3SeatTest#n4_myFilesListTheSongJustSent

## Pas reproduit
- **« Mes fichiers » vide après un envoi** (A c geste 27, contrôle Ac-3 geste 25 ; les deux sièges l'ont marqué « banc? temps ») : sur ce code, le banc liste le morceau tout de suite. Gardé comme test de garde du chemin du siège (`#n4_myFilesListTheSongJustSent`, vert sans changement, commit ad1447fb). À regarder sur le téléphone si un contrôle le revoit.

Ces défauts portent ici leur nom de siège, pas les numéros N1 / N4 / N5 de la fiche, qui sont aussi ceux d'autres défauts du tri (`verdicts.tsv`).
