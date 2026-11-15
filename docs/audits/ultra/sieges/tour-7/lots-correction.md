# Tour 7 : lots de correction proposés (tri du 2026-11-15)

Entrée : `synthese-tour-7.md`, qui donne les verdicts et les rejeux.
Défauts réels : **47** (0 bloquant, 3 graves, 44 moyens). Artefacts du banc : 11, plus un doute.

Six lots. Leurs fichiers ne se touchent pas. 38 défauts sont rangés dans un lot ; 9 sont laissés de côté avec leur raison.

## Règles communes à chaque lot
- Un commit « rouge: » (le test seul, qui échoue sur son assertion), puis « vert: » pour chaque défaut.
- Pour un grave, le rejeu de la synthèse doit être « non tenu » avant et « tenu » après.
- Captures regardées en Simple et en Pro, à 100 % et 130 % (200 % pour le lot 2).
- Les fichiers listés sont les SEULS que le lot touche. Pour tout autre fichier, envoyer une ligne au chef.
- Le tuto est une seule zone, donc un seul lot. Il n'entre que si un débutant neuf a rejoué ses chapitres sur la branche du lot : 0 chapitre bloqué, 0 grave neuf.

Phrase d'acceptation : « en <langue>, <main>, <appareil> à <police> %, Mode <mode> : depuis <écran>, je <geste> et <ce que je vois> ». Le siège d'origine suit entre crochets.

Ordre de priorité :
1. lot 1, le tuto (deux graves) ;
2. lot 3, le Guide et la coquille (un grave) ;
3. les lots 2, 4, 5 et 6.

---

## Lot 1 « tuto » (zone tuto, 8 défauts, dont 2 graves)

Fichiers :
- `app/src/main/java/com/pocketstudio/app/tuto/TutoController.kt`
- `app/src/main/java/com/pocketstudio/app/tuto/TutoPanel.kt`
- `project-model/src/main/kotlin/com/pocketstudio/model/tuto/TutoChecks.kt`
- `project-model/src/main/resources/tutos/*.json`
- `project-model/src/main/resources/i18n/*/tuto*.json`
- `app/src/main/java/com/pocketstudio/app/synth/PianoRoll.kt`
- `app/src/main/java/com/pocketstudio/app/rhythm/RhythmScreen.kt`
- `app/src/main/java/com/pocketstudio/app/library/PlainWords.kt`

| défaut | phrase d'acceptation [siège d'origine] | test rouge |
|---|---|---|
| [x] D1 (grave) T4/N4 et T15 : notes en pointillé sous le panneau | en fr, deux mains, phone à 100 %, Mode Simple : depuis le Clavier à l'étape « Basse » (house, chapitre 4), je touche « Basse · mesure 4/8 » et je vois chaque note en pointillé entière au-dessus du panneau, ou une flèche qui dit où elle est ; en ja (techno, chapitre 3), la grille n'est jamais cachée en entier ; en ko, phone360 à 130 %, l'aide des pads et la 2e rangée sont lisibles [1a #45, 1c #35, 2a #38 et #80] | piano roll et grille de pas avec le panneau ouvert : chaque cible de l'étape est au-dessus de `spots.coverTop`, sinon la flèche est montrée ; plus le rejeu `t7-aa44-pointilles-panneau` |
| [x] D2 (grave) : partie « Intro » refusée | en ko, deux mains, phone360 à 130 %, Mode Simple : depuis Mon morceau, chapitre 10 étape 1/7, je touche « “인트로” 파트 만들기 » et je vois « 성공 », puis l'étape 2/7 [2a #75] | `TutoChecks` : sur le projet du siège (Groove fait depuis le chapitre 5), `makePart` puis `partDone("Intro:less+nobass")` est vrai ; plus le rejeu `t7-ba75-intro-refusee` |
| [x] D3 T3/T7 : le haut du panneau (texte replié chevauché par « ▸ Pourquoi ? », lignes coupées par le bas du panneau, bouton du kit coupé ou caché derrière deux « Lire la suite ») | en fr, deux mains, phone à 100 %, Mode Simple : depuis le Rythme au début d'un chapitre, je lis la consigne repliée et je vois chaque ligne entière, « ▸ Pourquoi ? » dessous sans la toucher, et « Télécharger le kit House (4 Mo) » et « Plus tard » sans avoir à toucher « Lire la suite » [1a #1 et #26, 1c #1, 2a #2 et #62, 2d #3 et #5] | capture : panneau replié en fr, ja et ko, aucun texte « [coupé] » ni chevauché ; la carte du kit est visible sans dépliage |
| D4 T8 : 4 à 5 gestes dans une seule consigne (la mesure 3 de la basse est sautée) | en fr, deux mains, phone à 100 %, Mode Simple : depuis le Clavier à l'étape Basse, je lis la consigne et je vois au plus deux gestes ; le reste vient à l'étape suivante [1a #34 et #43, 1c #51, 2a #25] | `tutos/*.json` : aucune étape Facile ne demande plus de deux gestes (compte des « puis » et des verbes à l'impératif) |
| [x] D5 : la consigne compte des cases ou des mesures que la grille n'a pas (« 8 cases (4 par mesure) » pour 1 mesure, « 2 mesures » pour 1) | en fr, deux mains, phone à 100 %, Mode Simple : depuis le Rythme au chapitre 1 ou 3, je lis la consigne et je vois le nombre de cases ou de mesures que la grille montre [1a #21, écran 101] | pour chaque étape qui cite N cases ou N mesures, la grille de l'étape en a N (ou le besoin « Passer à 2 mesures » apparaît) |
| [x] D6 T11 : « Toucher ici » mène à la mesure 2 alors qu'il manque une note à la mesure 1 | en ja, deux mains, phone à 100 %, Mode Simple : depuis le Clavier (techno, chapitre 3), je touche 「ここをタップ」 et je vois la mesure où il reste une note à poser [1c #67] | `TutoNotesLeft` : la cible est la première note manquante dans l'ordre des mesures |
| [x] D7 : « Suivant » sur une écoute la compte faite sans un mot | en de, deux mains, tablet8 à 130 %, Mode Pro : depuis une étape « Écoute », je touche « Weiter » sans écouter et je vois une ligne qui dit que l'exemple est passé (et le chapitre ne se dit pas fini sans le dire) [1d #1-2 et #17, 1c #20, 1b #30] | `TutoController.move` : « Suivant » sur une écoute non faite pose le texte « exemple passé » (`made`) ; « 次の章 » et « 次へ » ne sont pas montrés côte à côte |
| [x] D8 : un chapitre ouvert par la liste ne dit pas qu'il lui manque une partie ou une piste (reste réel de l'artefact de l'ancrage) | en fr, deux mains, phone à 100 %, Mode Simple : depuis la liste des chapitres, je touche le chapitre 12 sans avoir fait le 10 ni le 11 et je vois que la partie « Montée » manque, avec un bouton pour la créer ou pour aller au chapitre qui la fait [1b #3, 1d #33] | `goTo` vers un chapitre dont une étape cite une partie ou une piste absente : le panneau le dit et propose de la créer |

Laissé de côté (tuto, moyens, pour le lot tuto suivant : le plafond de 8 est atteint) :
- L1 N13 : « Quitter le tuto » est un texte gris, pas un bouton [2d #20].
- L2 N25 : « Ich mach's » au Build-up ouvre la partie B sans rien dire [1d #62].
- L3 N23 : la fin montre « Étape 1/1 » puis « Presque fini ! », sans « Bravo » [1b #48-50].
- L4 N21 : un même mot nommé deux ou trois fois dans les textes du tuto [1c #52, 1d #13].
- L5 : la liste des chapitres est coupée au 11, sans signe qu'il y a une suite [2d #20].
- L6 : « Phrygienne » n'apparaît pas comme choisie [2d #15].
- L7 : la consigne du clap est réduite et « appui long… Doux » caché [2d #55].
- L8 : textes coupés dans le tuto :
  - « Écouter mon morc… », les puces de longueur, la carte du chapitre 14 défilée, « Mesure 2/2 · Caisse » sous l'en-tête [1b #32, #43, #33, #13] ;
  - longueurs de la carte cachées, mot coupé au bord droit [2a #70, écran 28] ;
  - 「フィルターのか…」 [1c #64] ;
  - « Das Kit Techno » [1d #101].
- L9 : « Cette étape n'est pas encore faite » ne dit pas ce qui manque [1b #15].
- L10 N26 : « Der ganze Groove spielt. » sur un Groove vide [1d #45].
- L11 : mots du tuto : « Techno 킷 », guillemets doublés « ““인트로” 파트 만들기” », 노트 et 음 mêlés, Roll et Rolls, Pad „Clap" et Clap FX [2a, 1d].

## Lot 2 « remix-découpe » (zone Remix, 7 défauts)

Fichiers :
- `app/src/main/java/com/pocketstudio/app/remix/CutScreen.kt`
- `app/src/main/java/com/pocketstudio/app/remix/RemixScreen.kt`
- `app/src/main/java/com/pocketstudio/app/remix/StemBoardRouter.kt`
- `app/src/main/java/com/pocketstudio/app/session/SessionScreen.kt`

| défaut | phrase d'acceptation [siège d'origine] | test rouge |
|---|---|---|
| D1 R3-EC17 / U13 : à 200 %, la courbe et Início/Fim/Ouvir ne sont pas visibles ensemble | en pt-BR, deux mains, phone à 200 %, Mode Simple : depuis « Um pedaço », je place la tête et je vois la courbe et Início, Fim et Ouvir ensemble sans défiler [2b #18] | capture à 200 % : la courbe et les trois boutons tiennent dans l'écran |
| D2 R7 : hors de portée du pouce gauche | en es, main gauche, phone à 100 %, Mode Simple : depuis la découpe, je touche les puces Frases, « Un trozo ▾ » et la poignée Fin sans quitter la zone du pouce [2c #15, #20, #33] | positions des puces, du menu et des poignées dans la moitié basse en main gauche |
| D3 R5 : boutons en double (deux « Escolher uma música », deux ← ou ▶, deux Deshacer) | en pt-BR, deux mains, phone à 200 %, Mode Simple : depuis le Remix, je cherche « Escolher uma música » et je n'en vois qu'un, avec un seul ← et un seul ▶ [2b #11 et #14, 2c #3 et #9] | arbre de l'écran Remix : chaque libellé apparaît une seule fois |
| D4 : « Início aqui » ne fait rien | en pt-BR, deux mains, phone à 200 %, Mode Simple : depuis « Um pedaço », je touche « Início aqui » et je vois le début du bout se poser à la tête [2b #16] | `CutScreen` : l'action pose le début à la position de la tête |
| D5 N14/R6 : « Sua música está vazia » juste après avoir posé Voz 1 | en es, deux mains, phone à 100 %, Mode Simple : depuis le Remix, je pose « Voz 1 » sur une pista puis j'ouvre Mi canción et je ne vois plus « Tu canción está vacía » [2b #27, 2c #41] | `SongCard` : une piste avec un bout posé, donc pas de message « vide » |
| D6 : « no se colocó nada » et « está en el pad » dans la même bulle | en es, main gauche, phone à 100 %, Mode Simple : depuis le Remix, je pose une voix sur un pad et je vois un seul message, juste [2c #30] | `StemBoardRouter` : un seul message selon le résultat |
| D7 : la puce « Resto » est coupée sans signe | en pt-BR, deux mains, phone à 200 %, Mode Simple : depuis la découpe, je vois la puce « Resto » entière [2b #14] | capture à 200 % : pas de puce coupée dans la rangée |

Laissé de côté : rien.

## Lot 3 « guide-coquille » (Guide, barre du haut, choix du mode : 7 défauts, dont 1 grave)

Fichiers :
- `app/src/main/java/com/pocketstudio/app/ui/ModeChoice.kt`
- `app/src/main/java/com/pocketstudio/app/guide/GuidePanel.kt`
- `app/src/main/java/com/pocketstudio/app/guide/GuideViewModel.kt`
- `app/src/main/java/com/pocketstudio/app/ui/AppRoot.kt`
- `app/src/main/java/com/pocketstudio/app/ui/AppSpaces.kt`
- `app/src/main/java/com/pocketstudio/app/ui/TopBar.kt`

| défaut | phrase d'acceptation [siège d'origine] | test rouge |
|---|---|---|
| [tenté, rouge+vert ; rejeu à faire sur émulateur] D1 (grave) : grand vide sous « Simple ou Pro ? » | en fr, deux mains, phone à 100 %, Mode (pas encore choisi) : depuis l'accueil d'une appli neuve, je touche « J'apprends » et je vois le titre, le texte et les deux cartes rangés depuis le haut, sans grand vide sombre [2d #1, #25, #34] | capture et positions : le texte d'aide suit le titre, sans trou de plus de 48 dp ; plus le rejeu `t7-bd01-grand-vide` |
| [non fait : la page sous le panneau est hors des fichiers du lot] D2 G1 : le Guide ouvert couvre ce qu'il demande de toucher | en hi, deux mains, phone360 à 130 %, Mode Simple : depuis Mon morceau, Guide ouvert à « एक साउंड बदलो », je vois « मेलोडी का साउंड » sans fermer le Guide [3a #11] | la cible du Guide est hors de la zone du panneau, ou le panneau se replie sur elle |
| [non fait : la barre a sa ligne, ce sont les pages Mix/Sons qui coupent, hors lot] D3 : la barre du Guide coupe le bas de l'écran | en fr, deux mains, phone à 100 %, Mode Pro : depuis Mix, je vois « Voir les courbes » et « Ce qui joue » entiers au-dessus de la barre du Guide ; idem pour « मेरी लाइब्रेरी » (hi) et la consigne de la Guía (es) [3b #42, 3a #62, 2c #9 et #16] | capture : aucun bouton sous la barre du Guide |
| [tenté, rouge+vert ; hi non reproduit] D4 : texte du Guide coupé | en pt-BR, deux mains, phone à 200 %, Mode Simple : depuis l'accueil du Guide, je lis « Crie uma batida… » entier et « a tela » n'est pas sous « Baixar o modelo » ; en hi, « गाइड » n'est pas coupé à gauche [2b #6, 3a #8] | capture : aucun « [coupé] » dans `GuidePanel` à 200 % |
| [tenté, rouge+vert] D5 : « Live: tocar en directo » hors de portée du pouce dans « Más » | en es, main gauche, phone à 100 %, Mode Simple : depuis le menu Más, je touche « Live: tocar en directo » sans quitter la zone du pouce gauche [2c #39] | position de l'entrée dans la moitié basse en main gauche |
| [tenté, rouge+vert] D6 : « Mes fichiers » absent du menu ⋮ | en fr, deux mains, phone à 100 %, Mode Simple : depuis le menu ⋮, je vois « Mes fichiers » [3b #10, #42] | `TopBar` : l'entrée « Mes fichiers » existe et ouvre la liste |
| [L : laissé de côté, réordonner le Mixer pousse Basse hors d’un écran 360 dp (KeyActionsGestureTest) ; à trancher] D7 U27 : pas les mêmes noms au Mixer et aux Sons (Synth contre Vocals) | en de, deux mains, tablet8 à 130 %, Mode Pro : depuis le Mixer puis les Sons, je vois les mêmes noms de pistes [1d #86, #99] | `AppRoot.mixerStrips` : mêmes libellés que les pages de Sons |

Laissé de côté : rien.

## Lot 4 « lecture » (1 défaut)

Fichiers :
- `app/src/main/java/com/pocketstudio/app/rhythm/RhythmViewModel.kt`
- `app/src/main/java/com/pocketstudio/app/ui/PlayStrip.kt`

| défaut | phrase d'acceptation [siège d'origine] | test rouge |
|---|---|---|
| D1 U28 : « ▶ morceau » joue depuis la partie choisie, pas depuis le début | en hi, deux mains, phone360 à 130 %, Mode Simple : depuis le Rythme, partie B choisie, je touche « ▶ गाना » et j'entends le morceau depuis la mesure 1 (A puis B) [3a #50, #41, #61] | `RhythmViewModel` : la lecture du morceau part de la mesure 1, ou dit à l'écran qu'elle part de la partie choisie |

## Lot 5 « export-projets » (4 défauts)

Fichiers :
- `app/src/main/java/com/pocketstudio/app/export/ShareSheet.kt`
- `app/src/main/java/com/pocketstudio/app/export/ShareSongUi.kt`
- `app/src/main/java/com/pocketstudio/app/export/MyExports.kt`
- `app/src/main/java/com/pocketstudio/app/export/WavExporter.kt`
- `app/src/main/java/com/pocketstudio/app/projects/ProjectsScreen.kt`

| défaut | phrase d'acceptation [siège d'origine] | test rouge |
|---|---|---|
| D1 : « Partager le morceau · 2 » ne dit pas quel fichier | en fr, deux mains, phone à 100 %, Mode Simple : depuis Partager, je vois le nom du fichier qui part (style, niveau, format) [3b #34] | le libellé du bouton contient le nom du fichier |
| D2 : après la feuille de partage fermée, « C'est prêt » répété, et rien ne dit que rien n'est parti | en fr, deux mains, phone à 100 %, Mode Simple : depuis la feuille de partage d'Android, je reviens sans rien envoyer et je vois « Pas envoyé : le fichier est dans Mes fichiers » [3b #19] | retour du partage sans cible : un seul message, qui le dit |
| D3 : pas les mêmes mots dans les feuilles d'export Simple et Pro | en fr, deux mains, phone à 100 %, Mode Pro : depuis Exporter, je vois les mêmes mots qu'en Simple pour le même choix [3b #32] | mêmes clés de texte dans les deux feuilles |
| D4 : « Nouveau projet » dans la liste, « Nouveau morceau » sur le bouton | en fr, deux mains, phone à 100 %, Mode Simple : depuis Mes projets, je vois le même mot que sur l'accueil [2d #32] | `ProjectsScreen` : un seul libellé |

## Lot 6 « langues » (5 défauts)

Fichiers :
- `project-model/src/main/resources/i18n/*/*.json`, sauf `tuto*.json` (ceux-là sont au lot 1)
- `app/src/main/java/com/pocketstudio/app/guide/TrackSheet.kt` (« Plaits » écrit en dur)

| défaut | phrase d'acceptation [siège d'origine] | test rouge |
|---|---|---|
| D1 R15 : « bar » en anglais | en hi, deux mains, phone360 à 130 %, Mode Simple : depuis le Rythme, je vois le mot hindi à côté des boutons 1 à 4 [3a #27] | catalogue hi : aucune valeur « bar » seule |
| D2 : « Live » et « Remix » en anglais dans le menu और | en hi, deux mains, phone360 à 130 %, Mode Simple : depuis le menu और, je lis les entrées en hindi (ou avec le nom en écriture hindi) [3a #62] | catalogue hi des entrées du menu |
| D3 : « La menor » au lieu de « Lá menor » | en pt-BR, deux mains, phone à 200 %, Mode Simple : depuis le choix de tonalité, je lis « Lá menor » [2b] | catalogue pt-BR : les notes avec accent |
| D4 : « pad live 1 » nommé comme un nom technique | en pt-BR, deux mains, phone à 200 %, Mode Simple : depuis le Remix, je pose une voix et je lis un nom de pad compréhensible [2b #25, 2c] | catalogues pt-BR et es du nom des pads |
| D5 T16 : Plaits et Acid 303 en anglais | en ja, deux mains, phone à 100 %, Mode Simple : depuis la fiche d'une piste, je lis les noms des sons en japonais (ou le nom propre avec son explication) [1c #27] | `TrackSheet` : plus de « Plaits » en dur ; catalogue ja |

## Défauts non rangés (fichier non localisé, moyens)

Siège 3a, hi, phone360 à 130 %, Simple :
- « ▶ सुनो » sans libellé visible (#64) ;
- −/+ grisés sans explication (#14) ;
- aucune preuve à l'écran que la piste T a été ajoutée (#23) ;
- « 27 पार्ट » affiché pour 20 parties (#65).

À localiser par un lecteur avant le tour suivant.

---

## À part : le banc

| n° | quoi | preuve | ce qu'il faut |
|---|---|---|---|
| B1 | l'ancrage `amenee=tuto:<style>:<n>` saute directement au chapitre n par la liste. Il ne joue pas les chapitres d'avant : pas de parties Intro, Montée et Drop, pas de pistes Accords ni Mélodie, Groove vide en Pro. Cinq « mission impossible » viennent de là (A/b 12.1 à 12.3 et chapitre 14, A/d chapitre 10, Automation, Mixer) | `lotA/b/journal.txt` l. 7-10 (`tuto_chapter_11`, `done=0`), geste 3 (« Partie : A » seule) ; `lotA/d/journal.txt` l. 7-8 (`tuto_chapter_7`, `done=1`) | l'ancrage prépare le projet des chapitres sautés (les faire par « Montre-moi » ou partir d'un projet fourni), ou la fiche du siège dit « chapitre isolé » et le testeur ne juge pas ce qui dépend des chapitres d'avant ; test du banc : rouge si, après l'ancrage au chapitre 12 de house, la partie « Montée » manque |
| B2 | le rejeu du tour 6 rend la main trop tôt : B/b au geste 4 sur 17, B/c au geste 1 sur 7 | `lotB/b/journal.txt` l. 23 ; `lotB/c/journal.txt` l. 9 (« écran différent de l'origine ») | dire à l'écrivain de la fiche quel écran a changé, et ne pas compter le siège comme rejeu fidèle ; voir si l'appli a changé l'écran entre-temps (normal) ou si le banc compare trop strictement |
| B3 | écrans au texte vide | A/a 3 et 102 ; B/d 42, 48, 55 | même famille que les incidents du tour 6 : relever dans `incidents-banc.tsv` |
| B4 | image en retard sur le texte | C/a 27 | relever ; capture prise avant la fin du dessin |
| B5 | pas d'attente visible devant « Rien ici » (U22) | `MyExports.kt` l. 162-168 (l'état d'attente existe) ; liste vraiment vide (aucun partage avant le geste 12) | rien côté appli ; dans la fiche du testeur : « une lecture instantanée ne montre pas d'attente » |
| B6 (doute) | « Chargement de la liste… » des projets reste affiché | B/d 23, 30, 31 | prouver par un test sans banc (Robolectric, liste des projets lue puis montrée) avant d'en faire un défaut de l'appli |
