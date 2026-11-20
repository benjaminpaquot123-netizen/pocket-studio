# Tour 9 : fiche du lot tuto (après L13)

Lecteur-trieur, 2026-11-20. Base lue : intégration `ccr-80af1a1b-mn0wec` (2428f6ee).
Aucun code modifié. Ce fichier sert de fiche d'ouverture au lot tuto suivant.

## Dépendances d'entrée (à lire d'abord)

- **L13** (`eq/tour-9-L13`, pas entré) change `tuto/TutoController.kt` (+19) et `tuto/TutoPanel.kt` (+312/-103 : repli F001/F002, plafond 60 %/85 % F003, flèche du repli F010).
- **L15** (`eq/tour-9-L15`, pas entré) change aussi `TutoController.kt`, `TutoPanel.kt`, `rhythm/RhythmScreen.kt`, `ui/AppRoot.kt` et `DefaultNames.kt` (shownOnPad, F132).
- **base-rouges** (pas entré) change `i18n/{de,hi,ja}/tuto_bass.json`, `ja/tuto_autres.json`, `ja/tuto_techno.json`, `TutoReadabilityBudgetTest`, `I18nCatalogTest`. Il laisse au lot du panneau le rouge `T9PanelTargetTest#fillCellsAboveThePanelIn360At200Japanese`.
- Conclusion : le lot part de l'intégration **après l'entrée de L13, L15 et base-rouges**. Sinon conflit sûr sur TutoPanel.kt, TutoController.kt et les JSON i18n.

## Défauts, dans l'ordre de passage conseillé

### 1. F041 : à retirer de la liste (pas de code)
- Acceptation : en mode Facile, les cellules du tuto à 100 % restent comme au tour 6.
- Cause : ce n'est pas un défaut. C'est le choix du tour 6, gardé par `Tour6TutoCellsPanel100Test` (vert). Recommandation de miettesB.
- Correctif : marquer F041 « retiré, choix tour 6 » dans `finitions.tsv` (ligne F041, lot L3).
- Test : `Tour6TutoCellsPanel100Test` reste vert.
- Conflit L13 : aucun.

### 2. F031 : le Facile parle de coups doux sans les vérifier
- Acceptation : en Facile, aucune consigne ne demande un coup doux, fort ou accentué que le tuto ne vérifie pas. Ce que dit la consigne, le tuto le contrôle.
- Cause : `TutoChecks.asksSoft(step)` (project-model/.../tuto/TutoChecks.kt:831) n'est vrai que si `text` ET `textPro` contiennent « « Doux » ». La force n'est contrôlée que dans ce cas (TutoKind.kt:315, `SOFT_HITS_CHECKED && asksSoft(step)` ; contrôle à TutoChecks.kt:813). 11 étapes `lane:*` parlent de force sans « Doux » : amapiano kick_do, deep_house hat_do, detroit hat_do, disco snare_do, gabber clap_do, industrial_techno kick_do, minimal clap_do, pop shaker_do, retro_techno hat_do, riddim kick_do, speed_garage snare_do.
- Correctif (recommandation miettesB) : retirer les mots de force du texte Facile de ces 11 étapes (le Pro garde sa consigne). Mettre à jour les clés dans les 14 langues (`i18n/*/tuto_*.json`, la clé est la phrase source française).
- Test : nouveau test project-model : toute étape `lane:*` dont le texte Facile cite une force (doux/fort/accent, 14 langues) doit satisfaire `asksSoft`. `I18nCatalogTest` garde les clés.
- Conflit : aucun avec L13 ; avec **base-rouges** sur `ja/tuto_techno.json` et `{de,hi,ja}/tuto_bass.json`. Attendre son entrée.

### 3. F032, reste synthwave : « pad des toms » mais le pad s'appelle « Rim »
- Acceptation : dans le tuto synthwave, la consigne nomme le pad tel qu'il s'affiche (« Rim »), et taper ce pad valide l'étape.
- Cause : `project-model/src/main/resources/tutos/synthwave.json:138-145`, étape `toms_do` : le texte dit « pad des toms », le check est `lane:perc`, le pad perc du kit synthwave s'affiche « Rim ».
- Correctif : texte de `synthwave.json` + les 14 `i18n/*/tuto_autres.json` (demandé par le rapport L17, lignes 32 et 41).
- Test : le nom affiché du pad perc du kit synthwave (DefaultNames) est cité dans la consigne de `toms_do`, dans les 14 langues.
- Conflit : `ja/tuto_autres.json` avec base-rouges ; `DefaultNames.kt` (shownOnPad) avec L15 : lire le nom après L15.

### 4. F101 : les glissés vers le haut ne font pas défiler la liste
- Acceptation : en tuto, un glissé vertical commencé en bas de l'écran (zone du panneau replié) fait défiler la liste derrière.
- Cause : pas la liste. Le panneau replié du tuto couvre le bas de l'écran et mange le glissé (rapport L12 lignes 11-31 ; rapport L6 ligne 31, « couche invisible du tuto »). Code : `TutoPanel.kt` (repli, réécrit par L13).
- Correctif : dans le repli de `TutoPanel.kt`, ne consommer que les tapes ; laisser passer les glissés verticaux à l'écran derrière (ou réduire la hauteur touchable du repli à sa ligne visible).
- Test : étendre `Tour9PartsListScrollsTest#italianSmallPhoneAt200InTheTutorial` à des glissés partis de y 505-595 : la liste défile.
- Conflit : **fort avec L13** (même code du repli). Partir de L13 entré.

### 5. D06 à 200 % (et alignement Pro « Falta tocar »)
- Acceptation : en Simple, à 200 % sur 360 dp, quand le tuto demande une note du rouleau, au moins une note en pointillé est visible au-dessus du panneau, et le compteur baisse quand on la tape.
- Cause : le panneau ne sait pas que le rouleau est en vue. `TutoHints` (TutoPanel.kt:167, remise à zéro 234) a `gridInSight` mis par RhythmScreen.kt:701-702, rien pour le rouleau ; `lowRoom` (TutoPanel.kt:565-566) ne replie donc pas assez bas. Rapport L7 lignes 44 et 74-75.
- Correctif : drapeau `TutoHints.rollInSight` mis dans `synth/PianoRoll.kt` (comme gridInSight) ; `lowRoom` le prend en compte ; haut du panneau plus court à 200 %. Même cause pour l'alignement Pro « Falta tocar ».
- Test : `PianoRollBigTextDotted#speedGarageBassSimple` à 360 dp / 200 % : une note en pointillé en vue, compteur qui baisse. Le même lot rend vert `T9PanelTargetTest#fillCellsAboveThePanelIn360At200Japanese` (laissé par base-rouges).
- Conflit : **fort avec L13** (TutoPanel.kt) et L15 (TutoPanel.kt). `PianoRoll.kt` : vérifier qu'aucun lot ouvert ne le tient.

### 6. F034, reste (sièges t9-08 zh, t9-38 zh, t9-65 ja) : « Montre-moi » montre une zone vide
- Acceptation : sur une étape « ajoute une partie », « Montre-moi » fait défiler Structure et pose le doigt sur le bouton d'ajout, visible au-dessus du panneau replié.
- Cause : `TutoController.showMe()` (TutoController.kt:666-700) replie le panneau et passe par `goFor` ; ligne 419, `song:order -> TutoGo(GuideOpen(SONG_ADD_SPOT, ""))` (SONG_ADD_SPOT = "song.add", ligne 1100) ; GuideNav.kt:100-101 vise le tag `song_add`, mais rien ne le fait défiler en vue : il reste sous le panneau.
- Correctif : avant de poser le doigt, faire défiler Structure jusqu'à `song_add` (bringIntoView) au-dessus du repli.
- Test : Compose/Robolectric en zh et ja : le doigt de la démo recoupe le nœud `song_add`, visible, au-dessus du panneau replié.
- Conflit : **fort avec L13 et L15** (TutoController.kt).

### 7. S17, seconde moitié (reste L7) : « Eu faço » repasse en boucle
- Acceptation : « Eu faço » (« Fais-le pour moi ») sur une étape de notes déjà posées passe à l'étape suivante, sans rejouer les mêmes notes en boucle.
- Cause probable : la démo ne tient pas compte des notes déjà justes (TutoChecks / TutoController, siège 17, geste 157). Rejeu d'abord.
- Correctif : la démo saute les notes déjà posées et valide si l'étape est déjà juste.
- Test : rejeu du siège 17 jusqu'au geste 157, puis test ciblé sur le contrôleur : étape déjà juste + « Fais-le » = étape suivante, une seule fois.
- Conflit : TutoController.kt avec L13 / L15.

### 8. F078 et F086 : déjà tentés par L16 (entré), constat seulement
- F078 (Outro -> bouton Intro) : `TutoController.orderLine` (~1046), commits 60842d8f / 001a8c94, `Tour9L16TutoOrderNamesTest`.
- F086 (pourquoi du décalage, uk_garage + afrobeats, 14 langues) : dd4d67fc / 9e8b0ff4, `Tour9L16TutoOffsetWhyTest`.
- À faire : aucun code. Le faire constater par un testeur neuf. Mettre aussi à jour `corrections.tsv`, qui dit encore « pas pris » pour F081/F084/F087/F089/F093/F099, tentés par L16.

### 9. À noter, hors lot : TutoJourneyTest capricieux
- `TutoJourneyTest` (jump_up) échoue parfois sous charge (reste L7). C'est un problème de banc, pas un défaut de l'appli : à donner au lot banc, avec une mesure (10 passes sous charge) avant tout correctif.

## Restes L2
Le rapport L2 n'a aucun reste tuto.

## Bilan
- Prêts à coder (après L13 + L15 + base-rouges) : F031, F032, F101, D06, F034, S17 (6).
- Sans code : F041 (retrait), F078 et F086 (constat).
- Hors lot : TutoJourneyTest (banc).
- Fichiers du lot : `tuto/TutoPanel.kt`, `tuto/TutoController.kt`, `synth/PianoRoll.kt`, `tutos/synthwave.json` + 11 tutos du point 2, `i18n/*/tuto_*.json`, tests neufs. Le tuto est une seule zone : un débutant neuf rejoue les chapitres avant l'entrée.
