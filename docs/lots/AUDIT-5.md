# Audit, étage 5 « utilisabilité experte » : note de suivi (équipe étage 5, 2026-11-04)

Branche `fx-s5` (worktree `/home/user/fx-s5`, depuis fx-int après l'aide revue). Rien de poussé, pas d'APK. Référence : `docs/audits/AUDIT.md` §4 étage 5 et §5 (gravité, cause racine). Travail repris d'un agent précédent qui l'avait laissé non testé : compilé, testé, trié ici.

## 1. Défauts trouvés et corrigés

| # | Gravité | Où / comment le voir | Cause racine | Correction | Test |
|---|---|---|---|---|---|
| 1 | Moyen | Live, chanson prête à enregistrer, téléphone 360 à 400 dp : « Ajouter à la suite » caché derrière un fondu | mise en page (rangée défilante dans une rangée à poids) | « La prise : » ne prend que sa largeur, les choix passent à la ligne (`FlowRow`) | `audit5/UsabilityFixesTest.bothTakeChoicesShowWhole…` (FR et DE) |
| 2 | Moyen | Barre du haut en Pro : les boutons ↶ ● ▶ sautent d'une ligne à deux en passant de Sons (puce de partie) à Structure ou Mix | mise en page : la décision « une ou deux lignes » dépendait de l'onglet | décidée comme si la puce était là dès qu'un autre onglet la montre (`sceneElsewhere`) | `nav/ModeChoiceTest.theProBarKeepsItsLinesFromTabToTab` (280 à 900 dp) |
| 3 | Moyen | Pad Live : « coup », « boucle », « tenir » écrits en blanc à 70 % sur un pad clair allumé : contraste 2,2:1 (WCAG 1.4.3) | couleur | voile sombre sous le mot (`PadWordScrim`, 55 % noir), texte plein | `audit5/UsabilityFixesTest.aPadsWordReadsOnItsColour` (≥ 4,5:1) |
| 4 | Mineur | Accueil « Que veux-tu faire ? » : deux cartes sans flèche (« c'est un bouton ? ») | cohérence (règle du design partout identique) | même flèche au bout de chaque carte | `audit5/UsabilityFixesTest.everyHomeCardSaysItOpensSomething` |
| 5 | Mineur | Feuille Partager : trois boutons orange (Partager plein, deux corrections tonales) : lequel d'abord ? | hiérarchie visuelle (une action principale par écran) | corrections en contour accent, jamais pleines | capture `nav/sheet_share.png` |
| 6 | Moyen (×N) | Chasse au jargon : « au Mixeur » dans 112 textes de tutos (l'onglet s'appelle Mix), « clip » (une case de la ligne du temps), « courbe d'automation », « Mix › Automation », « Console », « Fête », « chaque partie » pour une piste séparée | texte : un même mot pour la même idée pas tenu (une cause, donc une correction de fond) | les mots de l'interface partout : « dans Mix », « case », « courbe(s) », « Mix › Courbes », « Volumes », « Gros pads », « piste » ; 36 tutos et les 14 langues suivent | `project-model/i18n/LexiconWordsTest` (mots bannis du français, « Mix » dans chaque langue pour ≥ 90 textes de tutos) |

## 2. Décisions (style DECISIONS.md)
- 2026-11-04 · Le mode « Fête » du Pad Live devient « Gros pads » : le nom dit ce qu'il fait. Retour : remettre la clé « Fête ».
- 2026-11-04 · Le titre « Console » du Mix devient « Volumes » ; « automation » reste seulement dans le Lexique pour l'expliquer, l'interface dit « courbe ». Retour : textes seuls, aucune donnée touchée.
- 2026-11-04 · Les corrections de la feuille Partager sont en contour accent : un seul bouton plein par écran. Retour : `FilledTonalButton` dans `FixButton`.

## 3. Restes (pas faits dans ce lot)
- Parcours TalkBack et accès par contacteur complets, daltonismes simulés : pas faits sur appareil (pas d'émulateur ici) ; les tests Robolectric existants couvrent les cibles et les libellés.
- Relecture « locuteur natif » : les 14 langues suivent le français mot pour mot ; une relecture humaine reste à faire (étage 7).
- Tablette à dix doigts, clavier physique, souris, stylet : couverts par les tests existants (`TenFingersTest`, `KeyActionsGestureTest`), rien de nouveau.
