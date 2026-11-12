# Tour 6 : lots de correction proposés (tri du 2026-11-12)

Entrée : `lecture-recurrents.md` au rassemblement b283723c (86 défauts : sections 1 à 4, compléments C1.B et C1.E) et `verdicts.tsv` (une ligne par défaut, plus `R3-EC17` pour le bloquant du siège E c).
Verdicts : **64 réels**, 12 voulus, 5 artefacts du banc, 5 doublons, 1 déjà corrigé par un lot en attente (R2, lot langues `ccr-6f42e003-295715`). Gravité retenue : 1 bloquant (R3-EC17), 4 graves (T4, R6, N4, N5).

Règles communes à chaque lot : un commit « rouge: » (le test seul, qui échoue sur son assertion) puis « vert: » par défaut ; captures regardées en Simple et en Pro, à 100 % et 130 % (200 % pour le lot 4) ; les fichiers listés sont les SEULS que le lot touche (avec ses tests et ses captures) ; **deux lots ne partagent aucun fichier**. Un mot coupé (R1) ou un texte (R15, R16) qui vit dans le fichier d'un autre lot est traité par ce lot-là.

Ordre de priorité : 1, 2, 3 (tuto et Guide), 4 (bloquant de E c et grave R6), 5 (langues), 6 (le reste).

---

## Lot 1 « tuto-grille » : ce que le panneau du tuto cache (tuto, priorité a)

Défauts : T4 (grave), N4 (grave), N5 (grave), T11, N8, T15 (doublon de T4), U6, R21.
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

Laissé de côté : rien.

## Lot 2 « tuto-consignes » : ce que le tuto dit et vérifie (tuto, priorité a)

Défauts : T2, T3, T5, T6, T7, T8, T9, T14, T16, T17, T19, N2, N3, N6, N9, N10, N12, N13.
Fichiers : `project-model/src/main/resources/tutos/*.json`, `project-model/src/main/kotlin/com/pocketstudio/model/tuto/TutoChecks.kt`, `app/src/main/java/com/pocketstudio/app/tuto/TutoPanel.kt`, `app/src/main/java/com/pocketstudio/app/tuto/TutoController.kt`, `app/src/main/java/com/pocketstudio/app/library/PlainWords.kt`, `app/src/main/java/com/pocketstudio/app/guide/TrackSheet.kt`, et les textes du tuto `project-model/src/main/resources/i18n/*/tuto*.json` (tuto.json, tuto_autres, tuto_bass, tuto_hiphop, tuto_house, tuto_techno). Un mot de T16 qui vit dans un autre fichier de traduction (« Plaits » dans synth.json…) va au lot 5.

| défaut | test rouge |
|---|---|
| T5 / T17 | pour chaque tuto, la valeur de swing que la consigne écrit est celle que le curseur affiche quand l'étape est validée (house 25 → 33 %, deep_house et uk_garage 30 → 40 %) ; la démo « Montre-moi » pose cette même valeur |
| T3 | `laneMatches` refuse un coup « doux » posé à 100 % ; l'étape des claps en techno a bien 2 mesures si la consigne le dit |
| T2 | chaque pad nommé par une consigne (« Perc », « Tambourin », « Bord de caisse ») est le nom affiché d'un pad du kit chargé (`tutoPadNames`), y compris après le téléchargement du kit |
| T6 / N12 | tout mot de métier d'une consigne (mesure, temps 2 et 4, stab, riff, accord…) a son explication dans `PlainWords.TERMS` ; « Le morceau » → nom réel de l'onglet |
| T7 | à l'étape « ton kit est déjà celui de… », la carte du kit n'est pas montrée, ou « Plus tard » est au-dessus du fondu « Lire la suite » |
| T8 | aucune étape n'a plus de 3 actions dans sa consigne (compte des verbes à l'impératif, liste blanche) |
| T9 / N10 | un chapitre fini par « Bravo » est coché dans la liste (Pro : écoutes non comptées) ; le chapitre 9 a son écran « Bravo » |
| T14 / T19 / N9 | consignes réécrites : le pad change de nom visible, la piste citée est celle qui est allumée, « 16 + 8 » dit en mots |
| T16 | aucun mot anglais ou coupé dans les textes du tuto (« NEW », « Groove », « Plaits », « Perc ») dans les 15 langues |
| N2 / N3 | chaque consigne dit l'action (« touche… ») ; l'étape de tonalité nomme le bouton qui l'ouvre |
| N6 | chaque étape qui vérifie une autre partie ou un autre pad a un `target` (10 tutos) ; un coup posé ailleurs dit pourquoi il ne compte pas |
| N13 | « Fermer le tuto » est un bouton dessiné |

Laissé de côté : T10, T12, T18 (voulus : DECISIONS 2026-10-29, 2026-11-04 n° 34, 2026-11-05) ; T1 (constat de durée, pas un défaut).

## Lot 3 « guide-et-coquille » : la Guide et l'écran principal (Guide, priorité a)

Défauts : G1, G2, G3, T13, N1, N17, R19, U17, U27.
Fichiers : `app/src/main/java/com/pocketstudio/app/guide/GuidePanel.kt`, `app/src/main/java/com/pocketstudio/app/guide/GuideViewModel.kt`, `app/src/main/java/com/pocketstudio/app/ui/AppRoot.kt`, `app/src/main/java/com/pocketstudio/app/ui/AppSpaces.kt`.

| défaut | test rouge |
|---|---|
| G1 | sur chaque écran (Plus, Live pads, clavier, parties, export), la carte de la Guide ne recouvre aucun élément touchable (comparaison des rectangles) |
| G2 | changer de langue réécrit l'étape de la Guide dans la nouvelle langue |
| G3 | la consigne de la Guide ne demande jamais de toucher « Plus tard » |
| T13 / N1 | un tuto en cours : pas de deuxième Guide en bas ; « Ouvrir le Guide de mon morceau » montre le Guide ou dit pourquoi pas |
| N17 | « Passer en mode Simple » depuis Plus demande ou annonce le changement |
| R19 | une rangée ou des onglets qui sortent de l'écran montrent un signe de suite |
| U17 | Remix › projet existant › « Annuler » revient à la page Remix |
| U27 | le mixeur nomme les pistes comme l'écran des sons (Acid 303, Vocals) |

Laissé de côté : N16 (voulu, choix du 2026-10-29 noté dans le code seulement : à écrire dans DECISIONS ; la musique qui s'arrête sans le dire reste à trancher par Benjamin).

## Lot 4 « remix-decoupe » : le bloquant de E c et où vont les morceaux (priorité b puis c)

Défauts : R3-EC17 (bloquant), R6 (grave), R3, U13 (doublon de R3), R7, R5, N14 (doublon de R6), U29, R4.
Fichiers : `app/src/main/java/com/pocketstudio/app/remix/CutScreen.kt`, `app/src/main/java/com/pocketstudio/app/remix/RemixScreen.kt`, `app/src/main/java/com/pocketstudio/app/remix/StemBoardRouter.kt`, `app/src/main/java/com/pocketstudio/app/session/SessionScreen.kt`.

| défaut | test rouge |
|---|---|
| R3-EC17 | `CutScreen` en mode « Un bout », téléphone à 200 % (pt-BR) : la courbe (`CutZoom`) a au moins 120 dp et « Início / Fim / Ouvir o trecho » sont atteignables (la colonne défile au lieu d'écraser la courbe) ; même test à 100 % (B a écran 21) |
| R3 | mode « Phrases » : la ligne d'outils reste visible ou atteignable par défilement ; « Couper ce que je veux » n'est pas sous le bouton flottant |
| U13 | après « Voz 1 » posée, les puces et les noms des 4 boutons d'envoi restent |
| R6 / N14 | le message après l'envoi nomme le canal (« Batterie 16 pads ») ; « Ton morceau est vide » n'est pas dit quand une piste audio existe |
| R7 | puces Voix/Batterie et « Couper ce que je veux » à portée du pouce gauche (règle `SeatOptions.reachable`) |
| R5 | un seul « Télécharger le modèle », un seul « Choisir une chanson », un seul « Couper la chanson » par écran |
| U29 | `SongCard` montre un bouton pour ajouter une partie |

Laissé de côté : R4 va au lot 6 (son fichier y est).

## Lot 5 « langues » : textes coupés et mots restés en anglais ou en français (priorité d)

Défauts : R1, R14, R15, R16, R18, R20.
Fichiers : `project-model/src/main/resources/i18n/*/*.json` sauf `tuto*.json` (lot 2), `app/src/main/java/com/pocketstudio/app/mixer/SimpleMix.kt`, `app/src/main/java/com/pocketstudio/app/settings/LanguagePicker.kt`, `app/src/main/java/com/pocketstudio/app/live/LiveScreen.kt`, `app/src/main/java/com/pocketstudio/app/export/MyExports.kt`.

| défaut | test rouge |
|---|---|
| R1 | balayage des écrans cités à 130 % et 200 % (it, fr, pt-BR, de, en, tr, nl, pl, hi) : aucun mot coupé dans un mot (« Batteri / a », « Snar / e ») ; chaque cas dans un fichier d'un autre lot va à ce lot |
| R14 | un nom de fichier garde son extension visible |
| R15 / R16 | aucun mot anglais hors noms propres dans B c (ar, Pro), F b (pl), F a (it), G b (id), A a (hi) ; « La menor » → « Lá menor », « COMP. » → « COMPASES », « Önce ayrıştırma modeli » |
| R18 | la liste des langues garde la section « langue du téléphone » en tête |
| R20 | le compteur rond du Live a un nom et une unité |

À faire après la fusion du lot langues `ccr-6f42e003-295715` (R2 y est déjà corrigé ; R15 n'y est corrigé qu'en Mode simple pour 5 langues).

## Lot 6 « morceau-export-padlive » : le reste (priorité e)

Défauts : R4, U1, U2, U3, U4, U5, U11, U14, U16, U20, U28, N7 (doublon de U4), U15 (doublon de U4), R10, R23, U9, U10, U18, U21, N18.
Fichiers : `app/src/main/java/com/pocketstudio/app/rhythm/RhythmViewModel.kt`, `app/src/main/java/com/pocketstudio/app/export/ShareSheet.kt`, `app/src/main/java/com/pocketstudio/app/mixer/MixerScreen.kt`, `app/src/main/java/com/pocketstudio/app/padlive/PadLiveScreen.kt`, `app/src/main/java/com/pocketstudio/app/padlive/PadLiveRoute.kt`, `app/src/main/java/com/pocketstudio/app/onboarding/LaunchChoice.kt`, `app/src/main/java/com/pocketstudio/app/synth/TrackBar.kt`.

| défaut | test rouge |
|---|---|
| R4 | `useSong` : une chanson plus longue que le morceau affiche « la fin sera coupée » (ou dit que le projet s'allonge) |
| U28 | « + Nouvelle partie » sur un morceau vide garde A avant B |
| U11 | « End » arrête la lecture |
| U3 | `ExportUi.madeHash` ne change pas quand seule la scène affichée change |
| U2 / U5 | le niveau dit en Simple est celui de l'export ; le format choisi en Simple est gardé en Pro |
| U4 / U15 / N7 / U16 | la rangée M4A/WAV ne recouvre aucune ligne (« Autres fichiers », « .zip », « Nom du morceau ») ; partage fermé sans envoi : le champ ne dit pas « envoyé » |
| U1 | « Joue d'ici » annonce le saut à l'écran |
| U14 | « Appliquer à tout le morceau » met à jour la carte des accords |
| U20 | la bande « Performance gardée » reste après « Écouter » |
| R10 | un seul bouton Partager par écran du Pad Live |
| R23 | le Mix Pro s'ouvre en haut (tranches), pas sur l'export |
| U9 / U10 / U18 / U21 | effets du Pad Live titrés « Pad Live » ; icônes du choix de départ de même forme ; « Fermer » sans ⋯ ; pads à taille fixe avec le bandeau |
| N18 | l'icône ♪ de la piste a un nom visible |

Laissé de côté : U7, U8, U19, U24, U25, U26 (voulus, voir `verdicts.tsv`) ; N11 (voulu) ; R9 (voulu, décision du 2026-11-03).

---

## À part : le banc (lot du banc séparé, aucun fichier de l'appli)

- **Artefacts du banc** (`verdicts.tsv`) :
  - R8 : `HumanTester.settle` avance l'horloge d'au moins 1,1 s par toucher ; l'attente de la mesure (1,94 s à 124 BPM) est presque finie quand l'écran est lu. Il faut une lecture de l'écran juste après le toucher.
  - U12 : le faux menu de partage titre avec `uri.lastPathSegment` (le numéro de ligne MediaStore).
  - U22, U23 : écran lu avant la fin d'une lecture de fichiers. Il reste un point mineur côté appli : l'état « en lecture » n'a aucun signe d'attente.
  - N15 : environ 1 s par toucher, donc un bandeau de 6 s reste longtemps.
- **Incidents** : E1 (siège clos « rien ne change » alors que des notes sont posées), E3 (écrans en texte vide), E4 (notes du piano roll absentes du texte), E5 (« DOIGT rien » alors que le compteur baisse), E6 (dialogue d'aide absent du texte), E7 (effets live sans trace), E8 (plafond d'images et « attends »), E9 (appli non ouverte : lanceur pas démarré), E12 (« tape » de 4 coups compté pour 1).
- **Outils** :
  - `seat-metrics.py` change le verdict de 6 sièges quand on le relance : le dossier `etat/` n'est pas commité.
  - `tableau-de-bord.py` compte les promesses « 0/0 » : il ne lit pas les lignes `promesse:` avec `id=`.
  - `verifie-chantier.py` compte 28 écarts avec le résumé du lot F, contre 12 dans les selftests.
- **Hôtes** : mettre de côté les `start.txt` des autres sièges AVANT `selftest.sh`, sinon le tampon refuse le banc (vu au siège E c).
