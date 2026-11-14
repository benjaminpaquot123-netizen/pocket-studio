# Tour 5 — lot « access » (chantier 9) : rapport

Source : `PLAN-ACTION-AUDITS.md` étape 4 ligne 9 ; `AUDIT-PRISE-EN-MAIN.md` G10 et M5 ; règle 22 de `docs/DESIGN.md` ;
tour 5 siège E d #45 ; Benjamin, tel-10. Lot repris en route : le premier auteur a écrit le rouge et l'essentiel du code.

## 1. Les feuilles défilent en paysage (G10) — fait
- `ScaledBottomSheet` reçoit `scrolls` et `header` (ce qui reste en haut). 18 feuilles sur 22 défilaient déjà
  elles-mêmes ; posé sur les 4 autres : Tempo, prise de Pad Live, tonalité du tuto, choix d'un pad du Live.
- Tempo sur un téléphone couché : titre, tempo et « Fermer » sur une ligne qui reste ; pavé à gauche (OK compris),
  tempos prêts et « Taper en rythme » à droite. Tient à 100 % et en gros texte ; à 130 % le bas défile. Debout : inchangé.

## 2. Trois fiches « ? » là où un débutant bloque (M5) — fait
- « Enregistrer au clavier » (barre rouge du Clavier : où vont les notes, le chiffre = décompte en mesures, « Garder ce
  que je viens de jouer »), « Le Mix simple » (curseurs, mots sous « Sortie », « Tout à zéro »), « Partie en boucle ».
- 14 langues (`i18n/*/access.json`). L'audit proposait « le chiffre = notes gardées » : faux, le texte dit le décompte.

## À dire au relecteur
- Le rouge 425d9922 mesurait mal (`assertWhole` lisait la boîte intérieure : « tempo_tap », qui a une marge interne,
  ne pouvait jamais être entier). Corrigé dans un commit de test à part (ade1a174), avant le vert (cee8fb0e).
- Aucun rejeu retiré ni changé ; rien sous `portail/` ; fusion de fx-int : un conflit (`I18n.kt`, les deux côtés gardés).
- Non fait : le choix d'un pad du Live (`PadChooser`, privé) défile mais n'a ni test ni capture à lui ; les 18 autres
  feuilles n'ont pas de capture couchée (la règle 22 en demande une par feuille) : chantier à part.

## Tests à rejouer
- Nouveaux : `*LyingSheetsScrollTest`, `*LyingSheetsScreenshotTest` (6 captures neuves, `app/screenshots/lying/`).
- Touchés : `*HelpCoverageTest`, `*ScreenScanTest` (6 lignes au recensement), `*I18nCatalogTest`, `*GestureBudgetTest`.
- Captures réenregistrées (35, regardées ; Simple et Pro, debout et couché, 130 %, tablettes) : `*SimpleMix*ScreenshotTest`,
  `*Nav*ScreenshotTest` (simpleKeys, sonsSynth), `*TrackSheetTabletScreenshotTest`, `*AcidScreenshotTest`.
- Suite entière après la fusion (9 364 tests), rouges hors lot, non touchés : les cinq captures interdites, `remix_home`,
  `RemixChantier10Test#eachFixOfTheCutEndIsOneTouch` (référence absente sous verify), `BenchSeparationWaitTest` (charge).
