# Tour 6 — lot « tuto4 » : relecture neuve
Relu : f18962ad85f952300527b079c685648aaacfa2af

Relecture par un sous-agent neuf qui n'a pas écrit le lot : commits du lot depuis `49e7c7996` (branches `t4-house`, `t4-techno`, fusions, rapport, décisions), lus avec les catalogues hors tuto (`i18n/*/*.json`), `RhythmScreen.kt` (feuille du pad, menu du coup), `SimpleMix.kt`, `ExportCheck.kt`, `ShareSheet.kt`, `Loudness.kt`, `TutoPanel.kt`, `TutoChecks.kt` et `AppRoot.kt` (niveau du tuto et Mode simple).

## Ce qui tient
- Fichiers : hors des catalogues `tuto_house` / `tuto_techno` (14 langues), seuls `tutos/house.json`, `tutos/techno.json`, les deux tests, le rapport et les décisions ; `CLAUDE.md`, `AUDIT-ULTRA.md` et `GABARIT-FICHES-V3.md` viennent de la fusion de l'intégration (307759b37), pas du lot. Aucun Kotlin de l'appli.
- Les phrases collent aux données et aux écrans : 8 cases du « Rim » (le test les compte sur l'indice de l'étape), 8 mesures et 4 pour la Montée, chapitre 12 « Les transitions » en techno (le ch. 15 Pro seul est après), « Créer la partie « %s » » (`TutoPanel.kt:386`), « Plus tard » de la carte du kit (`TutoPanel.kt:357`), menu du coup « Force / Doux / Roulement / Triolet » (`RhythmScreen.kt:1062-1075`), repères « +4 / 0 / −12 / −∞ » (`SimpleMix.kt:316,484`), « Pas de saturation. », « Niveau : %s », « aucun solo oublié » (`ExportCheck.kt`, `ShareSheet.kt:493`), « Effets du pad » › « Envoi réverbe ».
- M6 sans piège : l'étape « reverb » est Pro seulement (`levels: p`), et lancer le tuto Pro passe en Mode pro (`AppRoot.kt:811,823,2073`), où « Effets du pad » est affiché (`RhythmScreen.kt:1168`). La cible techno `clap` seule correspond à la phrase (pas d'« Accords » exigé).
- Langues : fr, ja, de lus en entier sur les phrases changées ; ar, hi, zh-CN, ko, it, es échantillonnés. Les libellés cités sont ceux des catalogues de chaque langue (ja パッドのエフェクト, リバーブ・センド, シェア, 音割れはありません, ベロシティ / ソフト / ロール / 3連符 ; de Pad-Effekte, Reverb-Send, Teilen, Keine Übersteuerung, Velocity / Sanft / Roll / Triole ; ar مؤثرات الباد, إرسال صدى المكان ; zh-CN 打击垫效果, 混响发送 ; hi garde « Rim » et « Clap » en latin comme son catalogue). Script : les 56 phrases neuves existent dans les 14 langues traduites, aucun reste de français, chiffres identiques (écarts seulement « 1 » écrit en chiffre en ja/ko/id), aucun `%` perdu.
- Les tests lisent les données (indice de l'étape, `lines` du cours, `song`, catalogues réels), pas des tautologies ; chaque méthode a sa paire rouge / vert.

## Bloquant
Aucun.

## Grave
Aucun. Aucun test existant ne change de couleur parmi ceux relancés ; ni sauvegarde ni son touchés ; aucune phrase ne mène un débutant vers un bouton absent.

## Moyen
- (corrigé, voir le delta) M7 pas raccord entre les deux tutos : la house dit « Partage ton morceau », la techno garde le titre « Exporte ton morceau » et son Pro dit « touche « Partager », exporte ton morceau et partage-le ». Responsable : zone tuto (techno), prochain lot du tuto.
- `Tour6Tuto4HouseTest#musicianWordsAreExplainedAtFirstSight` ne vérifie l'explication qu'en français ; les autres langues sont seulement « traduites » (lues à la main ici : ja et de ont bien les parenthèses). La techno compte les parenthèses par langue : même garde à ajouter côté house.
- House, « share » : « « −14 » veut dire aussi fort que la musique en streaming » ; l'écran ne montre « (−14) » que si le morceau est entre −16 et −12 LUFS (`Loudness.kt:28-34`) ; ailleurs le chiffre annoncé n'apparaît pas. Pas de blocage (aucun geste demandé), mais la phrase peut décrire un libellé absent.

## Mineur
- House « make_montee » Pro : « … jusqu'au silence d'avant le Drop » promet encore le silence que le ch. « Les transitions » fera ; la techno a été corrigée (« viendra avec les transitions »).
- House « make_drop2 » dit « Créer la partie » ; le bouton lit « Créer la partie « Drop 2 » » (la techno cite le libellé entier). de écrit « Teil Drop 2 erstellen » sans les guillemets intérieurs.
- ko et it/es : guillemets imbriqués identiques (« ““빌드업” 파트 만들기” », « “Crea la parte “Intro”” ») : lisible, peu élégant.
- Le rapport range en « pas pris » « en Mode simple, « Effets du pad » est caché » : sans objet, l'étape est Pro seulement et le tuto Pro met le Mode pro.

## Relancé et vu
- `ANDROID_HOME=/opt/android-sdk bash scripts/bench/gradle-file.sh :project-model:test --rerun` sur d709a3256 : BUILD SUCCESSFUL, 1147 tests, 0 échec, 1 ignoré (un premier passage sans `--rerun` était resté « up-to-date », non compté).
- `ANDROID_HOME=/opt/android-sdk bash scripts/bench/gradle-file.sh :app:testDebugUnitTest --tests '*Tuto*'` sur d709a3256 : BUILD SUCCESSFUL en 7 min 58 s, 372 tests, 0 échec, 3 ignorés (`Tour6TutoPanelAlreadyTest` vert dans cet ordre).
- `python3 scripts/bench/rapport-zone.py eq/tour-6-tuto4 docs/audits/ultra/tour-6-tuto4-rapport.md` : sortie 0.
- rouge constaté : 07e274167 : rejoué sur 4c4b20a44 (parent du vert 1a41c4260, `git worktree` jetable) : `Tour6Tuto4HouseTest#theNumbersAreTheStepsOwn` échoue sur son assertion, « fr: the order step does not say the parts' length comes next ».
- rouge constaté : 18ca0438f : rejoué sur 18ca0438f (parent du vert 078e11b81, `git worktree` jetable) : `Tour6Tuto4TechnoTest#reverbSendFromThePadsEffects` échoue, « 30 reverb steps that do not lead to the pad's effects » (fr « dans Mix … sur la tranche du pad « Clap » »).
- Arbres jetables retirés ; l'arbre de `wt-tuto4` n'a pas été touché avant ce commit.

## Delta e7355a9d → f18962ad
Commits relus : f1cbb3003 (rouge, `Tour6Tuto4TechnoTest#theShareChapterSaysShareLikeTheHouse`) et f18962ad8 (vert).
- Moyen n° 1 (M7) : **corrigé**. `tutos/techno.json`, chapitre « export » : titre « Partage ton morceau », Pro « À toi : touche « Partager » et envoie ton morceau. », mêmes clés que la house ; plus aucun « export » dans le titre ni les étapes du chapitre.
- Vert minimal : 2 lignes dans `tutos/techno.json` et, dans les 14 `i18n/*/tuto_techno.json`, la seule clé « À toi : touche « Partager », exporte ton morceau et partage-le. » retirée. Les 15 JSON touchés se lisent (`json.tool`).
- Clé retirée vraiment inutilisée : aucun `tutos/*.json` ne la cite plus ; les 35 autres styles gardent leurs propres phrases (« À toi : exporte ton morceau et partage-le. » et sa variante M4A/WAV), qui restent traduites dans leurs catalogues.
- Traduction : `I18n.table` fusionne toutes les zones (`AREAS`, dont `tuto_house`), donc le titre et le Pro de la techno se lisent ja « 曲をシェアしよう » / « あなたの番：「シェア」をタップして曲を送ろう。 », de « Teile deinen Track » / « Du bist dran: Tippe auf „Teilen“ und schick deinen Track. » ; le test vérifie les 14 langues.
- Le test lit les vraies données (chapitre, étapes, tables de langue) : pas une tautologie.
- rouge constaté : f1cbb3003 : rejoué sur f1cbb3003 (parent du vert f18962ad8, `git worktree` jetable, retiré ensuite) : `theShareChapterSaysShareLikeTheHouse` échoue sur son assertion, « 2 share texts: fr: « Exporte ton morceau » ; fr: « À toi : touche « Partager », exporte ton morceau et partage-le. » ».
- `ANDROID_HOME=/opt/android-sdk bash scripts/bench/gradle-file.sh :project-model:test --rerun` sur f18962ad8 : BUILD SUCCESSFUL, 1148 tests, 0 échec, 1 ignoré.

Verdict : 0 bloquant, 0 grave — fusion possible
