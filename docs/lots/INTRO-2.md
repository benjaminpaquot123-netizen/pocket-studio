# Intro 2, « Corde tendue » : note de suivi

Branche fx-anim, commit 2b8d6e7 (sur a6606d7 : hold de 300 ms + SLOW 1,3). Benjamin (2026-11-04) : « retravailler l'animation, la rendre encore mieux, dans le même genre ». Même marque, même esprit, plus de matière : un seul anneau de lumière frappé puis pincé, des pads qui s'enfoncent, une ombre qui suit le bond, une poussière de lumière dans le halo, un vignettage, un zoom vers l'appli.

## Ce qui change (BrandIntro.kt seulement)
- **Un seul anneau** (plus les deux anneaux + 19 étincelles) : né au 1er battement à 31u (épais, alpha 0,9), tendu à 40u en 420 ms (0,8u, alpha 0,30), **pincé** par le 2e battement (vibration radiale ± 4 % de période 120 ms amortie en ≈ 500 ms, flambée d'alpha et d'épaisseur), relâché quand la marque monte (1820 → 2180 : +3u et fondu).
- **Press** : chaque pad s'enfonce de 5 % (3,2 % au 2e battement) dans la cascade de 35 ms du flash, comme un pad frappé dans l'appli ; flash **blanc chaud** (0xFFFFF1D6), 0,5 / 0,32, le pad allumé × 1,2.
- **Ombres** des 4 pads dessinées d'abord, sous tous les pads ; décalage 1,6u × (1 + 2,2·punch + 0,5·montée), alpha 0,32·relief·(1 − 0,3·punch) : elles s'allongent au bond (5,1u au sommet), reviennent avec le ressort.
- **Poussière** : 18 grains (0,6 à 1,0u, deux tons chauds, arc haut −190° → +10°, 33 à 50u du centre), allumés au 1er battement, poussés de 7u puis 3u, scintillement (période 900–1700 ms), dérive vers le haut 3,5u/s, flambée × (1 + 0,6·beatGlow), plafond 0,6, à moitié éteints de 3980 à 4700. Jamais sous le nom (vérifié par test).
- **Vignettage** 0 → 0,25 aux coins (1000 → 1580), transparent jusqu'à 55 % de la demi-diagonale ; ne monte pas avec la marque.
- **Parallaxe de la montée** : pads de 37 dp, halo et poussière de 0,6 ×, lumière ambiante de 0,35 ×.
- **Sheen** linéaire (vitesse constante, comme une tête de lecture).
- **Nom** : opacité 2060 → 2540, posé (10 dp plus bas, scaleX 1,045, scaleY 1,01 → 1) en easeOutQuint jusqu'à 2780 ; lumière sur le nom 2540 → 3260 (bande = hauteur du texte, alpha 0,7) ; ligne 2780 → 3260 en easeOutQuint.
- **Fondu + zoom** 1 → 1,04 (4700 → 5000) : on entre dans l'appli au lieu d'éteindre l'écran.
- Supprimés : RING_MS, RING_ALPHA, SPARK_ALPHA, ringProgress(k, t), ringAlpha(k), sparkAlpha, `object Sparks`.

## Timeline (ms de timeline ; temps réel après le raccord = 300 + (t − 1000) × 1,3)
- 0 → 1100 : **inchangé** (pop, halo, inspiration). f000, f015, f030 identiques au pixel près avant/après.
- 1100 battement 1 (vibration « lub ») : bond × 1,15, flash, press, ombre, anneau né, onde 1 (0,7), poussière allumée et poussée.
- 1340 battement 2 (« dub ») : bond × 1,085, pince de l'anneau, onde 2 (0,4, 420 ms), poussée 3u.
- 1480 → 1980 sheen ; 1820 → 2300 montée (grille 1100 + n × 240) ; 1820 → 2180 anneau relâché.
- 2060 → 2780 nom ; 2300 respiration (période 2400) ; 2540 → 3260 lumière sur le nom ; 2780 → 3260 ligne.
- 3260 → 4700 calme habité ; 3980 → 4700 poussière à moitié éteinte ; 4700 → 5000 fondu + zoom ; 5000 onDone.
- **TOTAL 5000** (était 4700), FADE_START 4700 (était 4400) : 1000 + 300 + 4000 × 1,3 = **6,5 s après l'icône**. Repli si trop long : TOTAL 4800 / FADE_START 4500 (6,24 s) ; si trop court : 5200 / 4900 (6,76 s).

## Performance (vérifié en relisant)
- Rien d'alloué par image : les 12 Stroke de l'anneau (0,6 à 1,8u), les 2 Color de la poussière, la brosse du vignettage, les Path et brosses des pads sont faits une fois (Kit / drawWithCache / init). Par image : des Float, `scale`/`translate`/`clipPath` (inline), des `Offset` (value class).
- Couches Offscreen : le wordmark (SrcAtop) + la Column du nom pendant son fondu OU la racine pendant le fondu final : 2 au plus.
- Une seule horloge (`clock.value`), lue dans onDrawBehind / graphicsLayer / snapshotFlow seulement.

## Tests (adaptés, aucun supprimé)
- `BrandTest.theIntroPlaysOverTheAppThenEndsByItself` : images clés à 130, 1150, **1380**, 1800, **2900**, visible encore à **4850**, fini à TOTAL + 100 ; durée 6 à 7 s après l'icône (1000 + wallMs(TOTAL, from = 1000) = 6500 ; la formule de la spec, `1000 + wallMs(TOTAL)`, comptait la première seconde deux fois : corrigée).
- `IntroSoundTest` : `sixToSevenSecondsFromTheIcon` (remplace `atMostOneSecondLongerThanBefore`), `theSplashHandsOverWithoutAJump` (valeurs neutres de ring/dust/vignette/shadow/rise/fade/punch/press pour t ≤ 1000), `theTwoDrawnBeatsAreTheSoundsTwoBeats` (anneau frappé puis pincé, press), `theDustIsLaidOutOnceOverTheMark` (remplace les étincelles : arc haut, 33–50u, 0,6–1u, plafond, jamais sous le nom).
- Captures ré-enregistrées : intro_1_beat, intro_2_beat, intro_3_rise, intro_4_name. Identiques : intro_0_pop (au pixel), intro_still, welcome, language_list.
- Lancé : `:app:testDebugUnitTest --tests '*BrandTest' --tests '*IntroSoundTest'` (19 tests, vert). Pas la suite complète ni assembleRelease (règle de l'atelier).

## Aperçu
- Scratchpad : `anim/frames/f000..f150.png` (30 i/s), `anim/intro.mp4` (360 px), `anim/intro.gif` (15 i/s, 300 px), `anim/compare_before_after.png` (8 images avant / 8 après), `anim/before/` (images de a6606d7).
- Regardé : 1000 (raccord), 1150, 1380, 1800, 2067, 2300, 2667, 2900, 3980, 4000, 4850. Rien de coupé (poussière à 60u max, écran de 360 dp = 67u de demi-largeur).

## Décisions à recopier dans DECISIONS.md (2026-11-04)
- Durée de l'ouverture : 6 à 7 s après l'icône (TOTAL 5000 / FADE_START 4700) remplace « 1 s de plus au maximum » du 2026-10-29. Retour : TOTAL 4700 / FADE_START 4400.
- La poussière (18 grains, deux tons chauds, arc haut, alpha ≤ 0,42 hors flambées) est la seule « particule » de l'appli ; coupable en une ligne (`DUST_MAX = 0f`) ; repli : un point de lumière sur le trait.
- Vignettage 0,25 aux coins, coupable en une ligne (`VIGNETTE_MAX = 0f`) ; pince de l'anneau coupable en une ligne (`PLUCK_AMP = 0f`).
- `render_brand.py` garde sa propre copie de la timeline (0–1000 seulement, pour le splash et son aperçu SVG) ; la vérité de l'intro entière est `BrandTest.previewFrames`.

## Restes (pour l'orchestrateur)
- `app/src/main/assets/changelog.txt` (5 lignes proposées dans la spec), `docs/DECISIONS.md` (4 lignes ci-dessus), `docs/PROGRESS.md`, PR avec les 5 captures et `compare_before_after.png`, CI (suite complète, verifyRoborazzi, assembleRelease, fumée), pré-release `preview`, APK signée pour Benjamin (tâche n° 70) avec cette intro dedans.
- Pour Benjamin, sur le Honor : les 2 vibrations tombent sur les 2 bonds ; rien ne saccade après le hold ; la corde se voit (sinon alpha 0,30 / 0,8u à monter) ; la poussière ne fait pas « sale » (sinon `DUST_MAX = 0f`) ; les coins ne font pas de bandes (sinon `VIGNETTE_MAX = 0f`).
- La lumière sur le nom est invisible sur « Studio » (blanc sur blanc), visible sur « Ben » seulement : comme avant, choix de la spec ; la « tête de lecture » qui écrit le nom reste pour une phase suivante.
