# Tour 8 · lot « captures8 » : rapport

Branche `eq/tour-8-captures8`, base : `ccr-80af1a1b-mn0wec` à 2af35df0, plus 4d4f9610 (miroir Maven) fusionné.

## Fait (2 défauts sur 2)

| Défaut | Phrase d'acceptation (vue de l'utilisateur) | Rouge | Vert |
|---|---|---|---|
| « + Piste » | La flèche qui dit que la rangée de pistes continue ne cache le texte d'aucune pastille (fr, nl, ja), et reste visible ; 360, 400 et 600 dp, à 100, 130 et 200 %. | 06166466, 472d0164 | d564c58e, fa22bdff, 440c63f5, 32222750 |
| Partage | La feuille « Prêt à partager ? » n'a plus de bande vide sous « Mes fichiers », ni au-dessus de M4A / WAV quand tout tient sans défiler ; Simple et Pro, mêmes tailles. | e6fff38e, 472d0164 | 4af756e1, 32222750 |

## Captures

Enregistrement complet (`recordRoborazziDebug`, 55 min, vert) sur 32222750 : 99 captures changées, commit a82b33c0.
Les 14 de la liste y sont (projects, remix, take_done, i18n/nl_projects, tablet8 ×6, share_sheet_ok,
partage/loop_simple, scale130/fr_track_frozen, scale130/nl_track_frozen) ; les 85 autres montrent la rangée de pistes
(nav/sons*, tabA11/*, tablet/*, scale130/*_nav_*, acid_*, remix_*, tour6/shell_*, tour7/guide2_*, share_sheet, nav/sheet_share,
partage/*_loop_pro…). `tuto_app_bass.png` change aussi, seulement par la rangée de pistes : aucun code de tuto touché.
Écart noté : PROGRESS.md annonce 14 captures à refaire, REVIEW.md en annonce 10.

tests: com.pocketstudio.app.synth.Tour8CapturesTest com.pocketstudio.app.synth.Lot6UiTrackBarMoreTest

## Liste des chemins

chemin: Clavier, rangée de pistes (flèche →, fr/nl/ja, 3 appareils, 5 tailles) | test: Tour8CapturesTest#theRowsArrowCoversNoChipText
chemin: Clavier, rangée de pistes en allemand Simple (la flèche continue) | test: Lot6UiTrackBarMoreTest#simpleGerman
chemin: Clavier, rangée de pistes en coréen Pro | test: Lot6UiTrackBarMoreTest#proKorean
chemin: Envoyer, feuille de partage (bas de la feuille) | test: Tour8CapturesTest#theShareSheetEndsWhereItsContentDoes
chemin: Envoyer, feuille de partage (au-dessus de M4A / WAV) | test: Tour8CapturesTest#noEmptyBandAboveThePinnedFormatsWhenNothingScrolls
