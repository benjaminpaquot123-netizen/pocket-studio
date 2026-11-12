# Tour 6 : lots de correction proposés (tri du 2026-11-12)

Entrée : `lecture-recurrents.md` au rassemblement b283723c (86 défauts : sections 1 à 4, compléments C1.B et C1.E) et `verdicts.tsv` (une ligne par défaut, plus `R3-EC17` pour le bloquant du siège E c).
Verdicts (après la relecture neuve `tri-relecture.md`) : **67 réels**, 11 voulus, 3 artefacts du banc (dont R8 à confirmer par un test sans banc), 5 doublons, 1 déjà corrigé par un lot en attente (R2, lot langues `ccr-6f42e003-295715`). Gravité retenue : 1 bloquant (R3-EC17), 4 graves (T4, R6, N4, N5).

Règles communes à chaque lot : un commit « rouge: » (le test seul, qui échoue sur son assertion) puis « vert: » par défaut ; captures regardées en Simple et en Pro, à 100 % et 130 % (200 % pour le lot 4) ; les fichiers listés sont les SEULS que le lot touche (avec ses tests et ses captures) ; **deux lots ne partagent aucun fichier**. Un mot coupé (R1) ou un texte (R15, R16) qui vit dans le fichier d'un autre lot est traité par ce lot-là.

Ordre de priorité : 1, 2, 3 (tuto et Guide), 4 (bloquant de E c et grave R6), 5 (langues), 6 (le reste).

**Avant tout lot** : fusionner les lots en attente. La branche langues `ccr-6f42e003-295715` touche aussi `GuidePanel.kt`, `RhythmScreen.kt`, `SessionScreen.kt` et `LaunchChoice.kt` (lots 1, 3, 4, 6) ; la branche gaucher `ccr-4cc0bb93-xljmti` touche `AppRoot.kt` (lot 3).

---

## Lot 1 « tuto-grille » : ce que le panneau du tuto cache (tuto, priorité a)

Défauts : T4 (grave), N4 (grave), N5 (grave), T11, N8, T15 (doublon de T4), U6, R21, T16 (sa part dans `PianoRoll.kt`).
Fichiers : `app/src/main/java/com/pocketstudio/app/synth/PianoRoll.kt`, `app/src/main/java/com/pocketstudio/app/rhythm/RhythmScreen.kt`.

| défaut | test rouge |
|---|---|
| T4 / N4 | piano roll, téléphone 360 dp à 130 %, panneau du tuto ouvert : chaque note « en pointillé » de l'étape est dessinée entière au-dessus du haut du panneau (`spots.coverTop`), sinon la flèche ↑/↓ est montrée ; le défilement à la touche amène la note au-dessus du panneau |
| T4 (pads) / T15 | grille de pas, D b (phone360, 130 %) : la 2e rangée de cases et l'aide des pads sont au-dessus du panneau du tuto |
| N5 | « Je le fais » à l'étape à 2 mesures (house.json, pas 30) : la note visée est visible et la ligne dit la mesure de la note (2), pas celle du milieu de l'écran |
| T11 | « Je le fais » ouvre la grille sur la mesure que la consigne nomme (pas 4/8 ni la mesure du chapitre précédent) |
| N8 | « Encore N notes » compte les touches à faire (9), pas les notes du motif (36) |
| U6 | toucher une case pendant la lecture coupe « Suivre » (la grille reste sur la mesure du coup) |
| R21 | les pastilles « G 30 / D 45 » d'un pad ont une explication (aide ou nom parlé) |
| T16 (piano roll) | « Basse en fantôme » de la rangée d'outils du piano roll est entier en fr à 100 % (pas « Bas ») |

Laissé de côté : rien.

## Lot 2 « tuto-consignes » : ce que le tuto dit et vérifie (tuto, priorité a)

Défauts : T2, T3, T5, T6, T7, T8, T9, T14, T16, T17, T19, N2, N3, N6, N9, N10, N12, N13.
Fichiers : `project-model/src/main/resources/tutos/*.json`, `project-model/src/main/kotlin/com/pocketstudio/model/tuto/TutoChecks.kt`, `app/src/main/java/com/pocketstudio/app/tuto/TutoPanel.kt`, `app/src/main/java/com/pocketstudio/app/tuto/TutoController.kt`, `app/src/main/java/com/pocketstudio/app/library/PlainWords.kt`, `app/src/main/java/com/pocketstudio/app/guide/TrackSheet.kt`, et les textes du tuto `project-model/src/main/resources/i18n/*/tuto*.json` (tuto.json, tuto_autres, tuto_bass, tuto_hiphop, tuto_house, tuto_techno). Un mot de T16 qui vit dans un autre fichier de traduction (« Plaits » dans synth.json…) va au lot 5.

| défaut | test rouge |
|---|---|
| T5 | l'étape Swing se valide quand le curseur montre la valeur écrite par la consigne |
| T17 | pour chaque tuto, la valeur de swing que la consigne écrit est celle que le curseur affiche quand l'étape est validée (house 25 → 33 %, deep_house et uk_garage 30 → 40 %) ; la démo « Montre-moi » pose cette même valeur |
| T3 | `laneMatches` refuse un coup « doux » posé à 100 % ; l'étape des claps en techno a bien 2 mesures si la consigne le dit |
| T2 | chaque pad nommé par une consigne (« Perc », « Tambourin », « Bord de caisse ») est le nom affiché d'un pad du kit chargé (`tutoPadNames`) ; le nom donné au pad après le téléchargement du kit (`RhythmViewModel.loadTutoKit`) est au lot 6 |
| T6 | tout mot de métier d'une consigne (mesure, temps 2 et 4, stab, riff, accord…) a son explication dans `PlainWords.TERMS` |
| N12 | chaque onglet nommé par une consigne porte le nom affiché (« Mon morceau », pas « Le morceau ») |
| T7 | à l'étape « ton kit est déjà celui de… », la carte du kit n'est pas montrée, ou « Plus tard » est au-dessus du fondu « Lire la suite » |
| T8 | aucune étape n'a plus de 3 actions dans sa consigne (compte des verbes à l'impératif, liste blanche) |
| T9 | un chapitre fini par « Bravo » est coché dans la liste (Pro : écoutes non comptées) |
| N10 | le chapitre 9 a son écran « Bravo » (`markDone`) |
| T14 | l'étape du grondement : après la consigne, le nom du pad visé est à l'écran (pas sorti du bord) |
| T19 | « Écoute le riff avec la basse » : la piste citée par l'étape `lead_hear` est celle qui est allumée |
| N9 | aucune consigne du chapitre 9 n'écrit un calcul (« 16 + 8 ») sans mots |
| T16 | aucun mot anglais ou coupé dans les textes du tuto (« NEW », « Groove », « Plaits », « Perc ») dans les 15 langues |
| N2 | chaque consigne « à toi » dit une action (un verbe d'action dans le texte) |
| N3 | l'étape de tonalité nomme le bouton qui l'ouvre |
| N6 | chaque étape qui vérifie une autre partie ou un autre pad a un `target` (10 tutos) ; un coup posé ailleurs dit pourquoi il ne compte pas |
| N13 | « Fermer le tuto » est un bouton dessiné |

Laissé de côté : T10, T12, T18 (voulus : DECISIONS 2026-10-29, 2026-11-04 n° 34, 2026-11-05 et 2026-11-11) ; T1 (constat de durée, pas un défaut).

## Lot 3 « guide-et-coquille » : la Guide et l'écran principal (Guide, priorité a)

Défauts : G1, G2, G3, T13, N1, N15, N16, N17, R19 (sa part dans `AppRoot.kt`), U17, U27.
Fichiers : `app/src/main/java/com/pocketstudio/app/guide/GuidePanel.kt`, `app/src/main/java/com/pocketstudio/app/guide/GuideViewModel.kt`, `app/src/main/java/com/pocketstudio/app/ui/AppRoot.kt`, `app/src/main/java/com/pocketstudio/app/ui/AppSpaces.kt`, `app/src/main/java/com/pocketstudio/app/ui/DoneBar.kt`.

| défaut | test rouge |
|---|---|
| G1 | sur chaque écran (Plus, Live pads, clavier, parties, export), la carte de la Guide ne recouvre aucun élément touchable (comparaison des rectangles) |
| G2 | changer de langue réécrit l'étape de la Guide dans la nouvelle langue |
| G3 | la consigne de la Guide ne demande jamais de toucher « Plus tard » |
| T13 | un tuto en cours : pas de deuxième Guide en bas |
| N1 | pendant un tuto, « Ouvrir le Guide de mon morceau » montre le Guide ou dit pourquoi pas |
| N15 | le bandeau « Fait : … Annuler » ne recouvre aucun élément touchable (« Mes fichiers ») |
| N16 | Retour depuis Mes projets (ouvert par Plus) : si la musique s'arrête, un message le dit (décision dans docs/decisions/tour-6-tri.md) |
| N17 | « Passer en mode Simple » depuis Plus demande ou annonce le changement |
| R19 | une rangée ou des onglets qui sortent de l'écran montrent un signe de suite |
| U17 | Remix › projet existant › « Annuler » revient à la page Remix |
| U27 | le mixeur nomme les pistes comme l'écran des sons (Acid 303, Vocals) |

Laissé de côté : rien. R19 : les autres rangées coupées sont aux lots 5 (`SimpleMix.kt`, `LiveScreen.kt`, `FadingScroll.kt`) et 6 (`TrackBar.kt`).

## Lot 4 « remix-decoupe » : le bloquant de E c et où vont les morceaux (priorité b puis c)

Défauts : R3-EC17 (bloquant), R6 (grave), R3, U13 (doublon de R3), R7, R5, N14 (doublon de R6), U29, R1 (sa part dans `CutScreen.kt` et `RemixScreen.kt`).
Fichiers : `app/src/main/java/com/pocketstudio/app/remix/CutScreen.kt`, `app/src/main/java/com/pocketstudio/app/remix/RemixScreen.kt`, `app/src/main/java/com/pocketstudio/app/remix/StemBoardRouter.kt`, `app/src/main/java/com/pocketstudio/app/session/SessionScreen.kt`.

| défaut | test rouge |
|---|---|
| R3-EC17 | `CutScreen` en mode « Un bout », téléphone à 200 % (pt-BR) : la courbe (`CutZoom`) a au moins 120 dp et « Início / Fim / Ouvir o trecho » sont atteignables (la colonne défile au lieu d'écraser la courbe) ; même test à 100 % (B a écran 21) |
| R1 (Remix) | « Cortar a mú… », « Baixar o modelo (174 », « Guard / ar », « Bateri » entiers à 200 % (pt-BR) dans `CutScreen` et `RemixScreen` |
| R3 | mode « Phrases » : la ligne d'outils reste visible ou atteignable par défilement ; « Couper ce que je veux » n'est pas sous le bouton flottant |
| U13 | après « Voz 1 » posée, les puces et les noms des 4 boutons d'envoi restent |
| R6 | le message après l'envoi nomme le canal (« Batterie 16 pads ») et le pad Live quand c'est un pad Live |
| N14 | « Ton morceau est vide » n'est dit ni quand une piste audio existe (E c #27) ni pendant la lecture en boucle (C b #19) |
| R7 | puces Voix/Batterie et « Couper ce que je veux » à portée du pouce gauche (règle `SeatOptions.reachable`) |
| R5 | un seul « Télécharger le modèle », un seul « Choisir une chanson », un seul « Couper la chanson » par écran |
| U29 | `SongCard` montre un bouton pour ajouter une partie |

Laissé de côté : R4 va au lot 6 (son fichier y est).

## Lot 5 « langues » : textes coupés et mots restés en anglais ou en français (priorité d)

Défauts : R1, R14, R15, R16, R18, R19 (sa part dans ces fichiers), R20, U22 (sa part réelle), T16 (ses mots hors des fichiers du tuto).
Fichiers : `project-model/src/main/resources/i18n/*/*.json` sauf `tuto*.json` (lot 2), `app/src/main/java/com/pocketstudio/app/mixer/SimpleMix.kt`, `app/src/main/java/com/pocketstudio/app/settings/LanguagePicker.kt`, `app/src/main/java/com/pocketstudio/app/live/LiveScreen.kt`, `app/src/main/java/com/pocketstudio/app/export/MyExports.kt`, `ui-components/src/main/java/com/pocketstudio/ui/FadingScroll.kt`.

| défaut | test rouge |
|---|---|
| R1 | balayage des écrans cités à 130 % et 200 % (it, fr, pt-BR, de, en, tr, nl, pl, hi) : aucun mot coupé dans un mot (« Batteri / a », « Snar / e ») ; chaque cas dans un fichier d'un autre lot va à ce lot |
| R14 | un nom de fichier garde son extension visible |
| R15 / R16 | aucun mot anglais hors noms propres dans B c (ar, Pro), F b (pl), F a (it), G b (id), A a (hi) ; « La menor » → « Lá menor », « COMP. » → « COMPASES », « Önce ayrıştırma modeli » |
| R18 | la liste des langues garde la section « langue du téléphone » en tête |
| R20 | le compteur rond du Live a un nom et une unité |
| R19 | les rangées de `SimpleMix`, du Live et de `FadingScroll` montrent un signe de suite quand elles sortent de l'écran |
| U22 | « Mes fichiers » pendant la lecture des fichiers montre un signe d'attente, puis la liste ou « Rien ici pour l'instant » |
| T16 (mots) | « Plaits », « Groove », « NEW » traduits ou expliqués dans les fichiers de traduction hors tuto |

R2 est déjà corrigé par le lot langues ; R15 n'y est corrigé qu'en Mode simple pour 5 langues.

## Lot 6 « morceau-export-padlive » : le reste (priorité e)

Défauts : R4, U1, U2, U3, U4, U5, U11, U14, U16, U20, U23, U28, N7 (doublon de U4), U15 (doublon de U4), R10, R23, U9, U10, U18, U21, N18, R8 (à confirmer d'abord), T2 (sa part `loadTutoKit`), R19 (sa part dans `TrackBar.kt`).
Fichiers : `app/src/main/java/com/pocketstudio/app/rhythm/RhythmViewModel.kt`, `app/src/main/java/com/pocketstudio/app/export/ShareSheet.kt`, `app/src/main/java/com/pocketstudio/app/mixer/MixerScreen.kt`, `app/src/main/java/com/pocketstudio/app/padlive/PadLiveScreen.kt`, `app/src/main/java/com/pocketstudio/app/padlive/PadLiveRoute.kt`, `app/src/main/java/com/pocketstudio/app/onboarding/LaunchChoice.kt`, `app/src/main/java/com/pocketstudio/app/synth/TrackBar.kt`, `app/src/main/java/com/pocketstudio/app/projects/ProjectsScreen.kt`, `app/src/main/java/com/pocketstudio/app/projects/ProjectsViewModel.kt`.

| défaut | test rouge |
|---|---|
| R4 | `useSong` : une chanson plus longue que le morceau affiche « la fin sera coupée » (ou dit que le projet s'allonge) |
| U28 | « + Nouvelle partie » sur un morceau vide garde A avant B |
| U11 | « End » arrête la lecture |
| U3 | `ExportUi.madeHash` ne change pas quand seule la scène affichée change |
| U2 | le niveau dit en Simple est celui de l'export |
| U5 | le format choisi en Simple est gardé en Pro |
| U4 / U15 / N7 | la rangée M4A/WAV ne recouvre aucune ligne (« Autres fichiers », « .zip », « Nom du morceau ») (un test par écran cité) |
| U16 | partage fermé sans envoi : le champ ne dit pas « envoyé » |
| U1 | « Joue d'ici » annonce le saut à l'écran |
| U14 | « Appliquer à tout le morceau » met à jour la carte des accords |
| U20 | la bande « Performance gardée » reste après « Écouter » |
| R10 | un seul bouton Partager par écran du Pad Live |
| R23 | le Mix Pro s'ouvre en haut (tranches), pas sur l'export |
| U9 | la feuille d'effets ouverte depuis Pad Live est titrée « Pad Live », pas « Remix » |
| U10 | les icônes du choix de départ ont la même forme et « Défis » est aligné |
| U18 | « Fermer » n'a pas l'icône ⋯ |
| U21 | les pads gardent taille et place quand le bandeau « Set gardé » apparaît |
| U23 | « Mes projets » pendant la lecture du catalogue montre un signe d'attente, pas une liste vide |
| R8 | test SANS banc d'abord : un pad touché pendant qu'un autre joue montre « anneau pâle » jusqu'à sa ligne ; s'il rougit, corriger ici |
| T2 (kit) | après le téléchargement du kit du tuto, le pad du grondement garde le nom que la consigne utilise |
| R19 (pistes) | la rangée des pistes (`TrackBar`) montre un signe de suite quand elle sort de l'écran |
| N18 | l'icône ♪ de la piste a un nom visible |

Laissé de côté : U7, U8, U19, U24, U25, U26 (voulus, voir `verdicts.tsv`) ; N11 (voulu) ; R9 (voulu, DECISIONS 2026-11-03).

---

## À part : le banc (lot du banc séparé, aucun fichier de l'appli)

- **Artefacts du banc** (`verdicts.tsv`) :
  - R8 (à confirmer par le test sans banc du lot 6) : `HumanTester.settle` avance l'horloge d'au moins 1,1 s par toucher ; l'attente de la mesure (1,94 s à 124 BPM) est presque finie quand l'écran est lu. Il faut une lecture de l'écran juste après le toucher.
  - U12 : le faux menu de partage titre avec `uri.lastPathSegment` (le numéro de ligne MediaStore).
  - U22 (probable) : écran lu avant la fin de la lecture des fichiers ; sa part réelle (aucun signe d'attente) est au lot 5. U23 est réel (lot 6).
  - N15 est réel (lot 3) ; le banc le rend seulement plus fréquent (environ 1 s par toucher).
- **Incidents** : E1 (siège clos « rien ne change » alors que des notes sont posées), E3 (écrans en texte vide), E4 (notes du piano roll absentes du texte), E5 (« DOIGT rien » alors que le compteur baisse), E6 (dialogue d'aide absent du texte), E7 (effets live sans trace), E8 (plafond d'images et « attends »), E9 (appli non ouverte : lanceur pas démarré), E12 (« tape » de 4 coups compté pour 1).
- **Outils** :
  - `seat-metrics.py` change le verdict de 6 sièges quand on le relance : le dossier `etat/` n'est pas commité.
  - `tableau-de-bord.py` compte les promesses « 0/0 » : il ne lit pas les lignes `promesse:` avec `id=`.
  - `verifie-chantier.py` compte 28 écarts avec le résumé du lot F, contre 12 dans les selftests.
- **Hôtes** : mettre de côté les `start.txt` des autres sièges AVANT `selftest.sh`, sinon le tampon refuse le banc (vu au siège E c).
