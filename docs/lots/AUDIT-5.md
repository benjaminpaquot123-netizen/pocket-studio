# Audit, étage 5 « utilisabilité experte » : note de suivi (équipe étage 5, 2026-11-04)

Branche `fx-s5` (worktree `/home/user/fx-s5`, depuis fx-int après l'aide revue). Rien de poussé, pas d'APK. Référence : `docs/audits/AUDIT.md` §4 étage 5 et §5 (gravité, cause racine). Travail repris d'un agent précédent qui l'avait laissé non testé : compilé, testé, trié ici.

## 1. Défauts trouvés et corrigés

| # | Gravité | Où / comment le voir | Cause racine | Correction | Test |
|---|---|---|---|---|---|
| 1 | Moyen | Live, chanson prête à enregistrer, téléphone 360 à 400 dp : « Ajouter à la suite » caché derrière un fondu | mise en page (rangée défilante dans une rangée à poids) | « La prise : » ne prend que sa largeur, les choix passent à la ligne (`FlowRow`) | `audit5/UsabilityFixesTest.bothTakeChoicesShowWhole…` (FR et DE) |
| 2 | Moyen | Pad Live : « coup », « boucle », « tenir » écrits en blanc à 70 % sur un pad clair allumé : contraste 2,2:1 (WCAG 1.4.3) | couleur | voile sombre sous le mot (`PadWordScrim`, 55 % noir), texte plein | `audit5/UsabilityFixesTest.aPadsWordReadsOnItsColour` (≥ 4,5:1) |
| 3 | Mineur | Accueil « Que veux-tu faire ? » : deux cartes sans flèche (« c'est un bouton ? ») | cohérence (règle du design partout identique) | même flèche au bout de chaque carte | `audit5/UsabilityFixesTest.everyHomeCardSaysItOpensSomething` |
| 4 | Mineur | Feuille Partager : trois boutons orange (Partager plein, deux corrections tonales) : lequel d'abord ? | hiérarchie visuelle (une action principale par écran) | corrections en contour accent, jamais pleines | capture `nav/sheet_share.png` |
| 5 | Moyen (×N) | Chasse au jargon : « au Mixeur » dans 112 textes de tutos (l'onglet s'appelle Mix), « clip » (une case de la ligne du temps), « courbe d'automation », « Mix › Automation », « Console », « Fête », « chaque partie » pour une piste séparée | texte : un même mot pour la même idée pas tenu (une cause, donc une correction de fond) | les mots de l'interface partout : « dans Mix », « case », « courbe(s) », « Mix › Courbes », « Volumes », « Gros pads », « piste » ; 36 tutos et les 14 langues suivent | `project-model/i18n/LexiconWordsTest` (mots bannis du français, « Mix » dans chaque langue pour ≥ 90 textes de tutos) |

## 2. Décisions (style DECISIONS.md)
- 2026-11-04 · Le mode « Fête » du Pad Live devient « Gros pads » : le nom dit ce qu'il fait, et « Fête » ne désigne plus que le pack prêt à jouer (un mot, une idée). Retour : remettre la clé « Fête ».
- 2026-11-04 · Fusion avec l'étage 1, qui avait aussi posé un voile sous le mot du pad (45 % noir) : on garde sa mise en page (`FitText`) et le voile de l'étage 5 (55 %, 4,5:1 vérifié par test).
- 2026-11-04 · Le titre « Console » du Mix devient « Volumes » ; « automation » reste seulement dans le Lexique pour l'expliquer, l'interface dit « courbe ». Retour : textes seuls, aucune donnée touchée.
- 2026-11-04 · Les corrections de la feuille Partager sont en contour accent : un seul bouton plein par écran. Retour : `FilledTonalButton` dans `FixButton`.

## 3. Restes (pas faits dans ce lot)
- **Barre du haut en Pro qui saute (Moyen, non corrigé)** : sur un téléphone de 411 dp, ↶ ● ▶ sont sur une ligne en Structure et Mix, sur deux en Sons (la puce de partie prend la place). Essai retiré : décider « deux lignes partout » coûtait une ligne d'écran sur tous les onglets et faisait passer « Changer le son de la basse » de 2 à 3 gestes (`KeyActionsGuideOpenTest`). Piste : en Sons, céder le nom du projet plutôt que la ligne.
- **Captures de la matrice d'audit (étage 1) sous `verifyRoborazziDebug`** : corrigé ici, elles n'écrivent d'image qu'en enregistrement (`CaptureMatrixTest.RECORDING`), sinon les 493 cas échouaient faute d'image de référence (jamais commitée).
- Parcours TalkBack et accès par contacteur complets, daltonismes simulés : pas faits sur appareil (pas d'émulateur ici) ; les tests Robolectric existants couvrent les cibles et les libellés.
- Relecture « locuteur natif » : les 14 langues suivent le français mot pour mot ; une relecture humaine reste à faire (étage 7).
- Tablette à dix doigts, clavier physique, souris, stylet : couverts par les tests existants (`TenFingersTest`, `KeyActionsGestureTest`), rien de nouveau.

## 4. Vérification (2026-11-04)
- Contrôle complet (`:project-model:test :app:testDebugUnitTest :app:verifyRoborazziDebug`) après fusion de fx-int : 7 005 tests, 11 échecs, tous des tests lents connus pour être instables sous charge (`TutoJourneyTest` ×3, `HumanRemixJourneyTest`, `Tuto*` emportés par un exécuteur tombé à court de mémoire, deux captures dont « Sauvegardé » apparaît ou non). Relancés seuls : tous verts.
- Captures réenregistrées et regardées une à une : seuls les changements voulus (Volumes, Gros pads, voile du mot du pad, flèches de l'accueil, choix de la prise entiers, corrections en contour).
