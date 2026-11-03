# Audit, étage 1 : contrôles automatiques massifs (équipe étage 1, 2026-11-03)

Branche `fx-s1` (worktree `/home/user/fx-s1`), code gelé à l'étiquette `audit-1`. Sans APK, sans push. Référence : `docs/audits/AUDIT.md` (§4 étage 1, §5 gravité). Fiches au format `docs/audits/GABARIT-FICHE-DEFAUT.md`, raccourcies (gravité, reproduire, cause, correction, test).

## Outils ajoutés (réutilisables aux tours suivants)

- **`LayoutScan`** (`app/src/test/java/com/pocketstudio/app/audit/LayoutScan.kt`) : lit la mise en page Compose au lieu de l'œil. Signale : texte coupé (« … », mot perdu sans « … », lignes tronquées, mot coupé en deux lignes « Monté/e », texte qui dépasse un parent qui coupe), textes l'un sur l'autre, texte qui passe sur un bouton voisin, cibles (touche < 48 dp ou visible < 24 × 24 dp, WCAG 2.2 AA 2.5.8), bouton sans nom pour TalkBack, contraste WCAG AA lu sur les pixels (4,5:1 ; 3:1 dès 18 sp ou 14 sp gras ; contrôles désactivés exclus). Tient compte des échelles (`graphicsLayer`) et du droite à gauche.
- **Matrice de captures par paires** (`CaptureMatrixTest.kt`, générateur `Pairwise.kt`, test `PairwiseTest`) : 29 écrans clés × 16 langues (15 + pseudo) × 5 appareils (téléphone 360 dp, téléphone 411 dp, téléphone couché 800 × 360, tablette 8" de Benjamin 600 × 1006, tablette 11" couchée 1280 × 800) × police (100 %, 130 %). Toute paire de valeurs est couverte : **465 cas** au lieu de 4 640 (produit complet). Chaque cas échoue s'il reste un défaut non accepté ; les acceptés sont dans `Allowed` avec leur raison. Images à regarder : `./gradlew :app:testDebugUnitTest --tests '*CaptureMatrix*' -Proborazzi.test.record=true` → `app/build/audit-captures/<appareil>/<langue>_<écran>.png` (jamais commitées).
- **`AccessibilityScanTest`** : les 29 écrans clés, téléphone 411 dp, français : cibles, noms TalkBack, contrastes.
- Les captures i18n existantes (`I18nScreenshotTest` et ses langues, pseudo et pseudo droite à gauche) passent aussi par `LayoutScan`.
- `ScreenshotTest` : crochets `fontScale` (taille de police du téléphone) et `inspect(name)` (appelé une fois l'écran dessiné).

Liste des paires : générée par `CaptureMatrixTest.ALL` (déterministe) ; 93 cas par appareil en moyenne ; chaque écran est vu dans chaque langue au moins une fois, chaque écran sur chaque appareil, chaque langue sur chaque appareil, les deux tailles de police partout.

## Défauts trouvés (étage 1)

Gravité : AUDIT §5. « Corrigé » = correction + test qui échouait avant.

### Grave
Aucun.

### Moyen

**D-1-01 : mots perdus sans « … » (texte sur une ligne qui passe à la ligne et perd la suite)**
- Reproduire : Live › Effets, police 130 % : le titre « L'EFFET » devient « THE » en anglais ; Pad Live en polonais : « uderzenie · 1 » perd « · 1 », « przytrzymaj » disparaît ; Mixeur : « Ce qui joue » / « Automatiser » en allemand, anglais, indonésien, japonais ; Morceau : « × 2 · 2 mes. · joue » perd « joue » dans 12 langues sur téléphone ; plein écran des pads : « Tout couper » en polonais et turc ; ligne du temps « mes. 7.2 / 22 » en espagnol.
- Cause racine (texte / mise en page) : `Text(maxLines = 1)` sans `softWrap = false` ni `overflow` : Compose coupe au dernier mot qui tient, sans rien montrer. 103 occurrences de ce motif dans le code, 9 vues fautives par la matrice.
- Correction (règle « partout », composant partagé) : `FitText` (plus petit, mots entiers, « … » seulement en dernier recours) sur les 9 ; `SectionTitle` du Live, boutons du Mixeur, ligne des parties, bouton « Tout couper », position de la ligne du temps, mot de jeu des pads de Pad Live.
- Test : `CaptureMatrix*` (constat « lines dropped »), échouait avant sur ces cas.

**D-1-02 : Fête et carte des pads live sans `FitText` (reste du lot D2)**
- Reproduire : Pad Live › Fête sur 360 dp : « Boucle de ki… », « Boucle Acid… », « Cri « Go… », « Monté/e » coupé au milieu du mot. Carte des pads live (Morceau) : « Kick 909 », « 2 batt. » qui sortent du bas du pad en hindi, coréen, police 130 %.
- Correction : `FitText` sur 3 lignes (Fête), `FitText` + `shrinkToFit` (nouveau, `ui-components/FitText.kt` : le contenu trop haut est dessiné entier et plus petit) sur la carte des pads live ; le nom d'un pad de Pad Live gagne 4 dp pour sa deuxième ligne quand « Plus » est ouvert.
- Test : `CaptureMatrixTest` (cas `padlive_party`, `live_pads`), nouveau constat « mot coupé en deux lignes ».

**D-1-03 : Rythme, le titre « PAS : … » passe sous « Tous les pads » (et toute `SectionLabel` avec « ? »)**
- Reproduire : Rythme en pseudo-traduction (ou un nom de son long) : le titre déborde sur le bouton voisin, son « ? » sort de la ligne.
- Cause : `WithHelp` donnait toute la ligne au titre, le « ? » était poussé dehors, sur le bouton d'à côté. Composant partagé : toutes les `SectionLabel(help = …)` de l'appli.
- Correction : le titre prend ce qui reste après son « ? » (`Components.kt`, `weight(1f, fill = false)`), le chevron de pli aussi.
- Test : nouveau constat « texte sur un bouton voisin » (`LayoutScan.overButtons`), sur les 8 écrans i18n × 7 langues/pseudo.

**D-1-04 : Morceau, cases des parties sur téléphone couché : la 3e ligne (« 1 mes. », « ensuite ») sort du bas**
- Reproduire : Morceau, téléphone couché, japonais, coréen, chinois, arabe, ou police 130 %.
- Correction : `shrinkToFit` sur le contenu de la case (dessiné entier, plus petit).
- Test : `CaptureMatrixPhoneLandTest` (cas `session`).

**D-1-05 : Live, « ■ Tout » coupé (« ■ Wszys ») et onglets du panneau qui débordent de 2 à 11 dp**
- Cause : `TextAutoSize.StepBased` + `softWrap = false` sans repli : au plus petit, le texte est rogné sans « … ». 9 occurrences.
- Correction (partout) : `WholeWordsAutoSize` + « … » sur les 9 ; « ■ Tout » sur deux lignes.
- Test : `CaptureMatrix*` (« wider than its box »).

**D-1-06 : boutons de la grille du Live sans nom pour TalkBack** (lignes A à G, « ■ Tout »)
- Cause : `launchOnPress` posait l'action sans fusionner les mots de l'intérieur.
- Correction : `semantics(mergeDescendants = true)` (`LivePress.kt`), toutes les cases et lignes du Live d'un coup.
- Test : `AccessibilityScanTest` (`live_grid`, `live_fx`).

**D-1-07 : contraste des mots de jeu de Pad Live (« boucle », « coup », « tenir ») : 2,2 à 3,4:1**
- Correction : voile sombre derrière le mot, texte plein. Test : `AccessibilityScanTest` (`padlive`, `padlive_more`).

**D-1-08 (non corrigé, passe de design dédiée) : contrastes sous WCAG AA**
- Morceau : titre de la partie qui joue sur fond vert (2,9:1) ; dialogue (Annuler/OK, 3,6:1) ; ligne du temps « Suivre », « Boucle » (4,1:1) ; Acid 303 : flèches ↑ ⌒ sur case orange (2,9:1) ; bibliothèque « Charleys fermés » (3,2:1) ; piano roll « 1/8 » (2,4:1) ; plein écran des pads, longueurs sur pad allumé (2,2 à 4,3:1). Accepté dans `Allowed` en attendant, à reprendre en une fois (couleurs du thème).

### Mineur

- **D-1-09** Sons : la puce de la piste ouverte ailleurs (fiche, « + Piste », après Remix) restait hors écran (reste « APK 228 »). Corrigé : la rangée défile jusqu'à elle (seule la rangée, jamais la page). Test `TrackBarRevealTest`.
- **D-1-10** Tuto : la rangée d'outils du piano roll à moitié sous « Partie : A » (reste « APK 228 »). Corrigé : l'amenée à l'écran garde 64 dp au-dessus de la grille. Vérifié sur `tuto_app_bass`.
- **D-1-11** Cibles sous 48 dp : octave −/+ du synthé (40), « Cases − / + » du Live (44), fermer le Défi (40). Corrigé (48 dp). Le reste vu par le scan : touches de clavier musical (exception « essentielle » de WCAG), poignée du panneau (18 dp vus, 48 au toucher).
- **D-1-12** `FitText` rapetissait le coréen pour rien (coupure entre syllabes prise pour un mot coupé). Corrigé (`breaksAnywhere` : hangul).
- **D-1-13** MINEUR 5 « Tuto tous styles » : un triolet au bout d'une boucle de pad raccourcie débordait sur le premier pas du tour suivant. Corrigé : `Hit.within` (seuls ses coups avant la fin), appliqué à chaque envoi au moteur (`pushHitNow`), le coup écrit reste entier. Test `HitWithinTest`.
- **D-1-14** MINEUR 8 « Tuto tous styles » : la 808 du trap ne tapait pas (notes liées). Corrigé : chaque note s'arrête une case avant la suivante, les deux glissés d'octave gardés. Test `Trap808Test`.
- Restes acceptés (raisons dans `Allowed`) : noms longs de l'utilisateur raccourcis par « … » ; nom de colonne du Live au plus petit (6 colonnes, police 130 %) ; pads courts de Pad Live avec « Plus » ouvert dans les langues longues ; « comp. » du piano roll en espagnol ; aide d'une ligne du Rythme couchée.

## Ce qui reste (étage 1)
- MINEUR 3 (course mode / `follow`) : machine à états, à l'étage 2. MINEUR 7 (étape « pompe » du tuto) : contenu de cours, non fait.
- « Sons manquants des fiches » (break échantillonné, log drum, orgue M1, stabs, rewind…) : nouveaux sons, hors contrôles automatiques.
- Mots « Scie » / « Triangle » : choix de mots (question de produit).

## Décisions à recopier dans DECISIONS
- 2026-11-03 · Cibles tactiles jugées sur deux règles : toucher ≥ 48 dp (Android) et visible ≥ 24 × 24 dp (WCAG 2.2 AA 2.5.8) ; touches d'un clavier musical exemptées (« essentiel »). Raison : Compose élargit déjà le toucher à 48 dp, seul le visible minuscule gêne. Retour : `LayoutScan` (seuils).
- 2026-11-03 · Contrôles d'écran par la mise en page Compose (`LayoutScan`) plutôt que l'ATF de Google. Raison : sans dépendance nouvelle, lit les textes coupés que l'ATF ne voit pas ; mêmes règles (cibles, libellés, contraste). Retour : ajouter `roborazzi-accessibility-check`.
- 2026-11-03 · Matrice de captures par paires (465 cas) dans la suite JVM, défauts acceptés listés avec leur raison (`Allowed`). Retour : supprimer `app/src/test/.../audit/`.
- 2026-11-03 · `Text(maxLines = 1)` qui peut être long → `FitText` ; `TextAutoSize.StepBased` → `WholeWordsAutoSize` + « … ». Raison : aucun mot perdu en silence.
