# Tour 6 : relecture neuve du tri (2026-11-12)

Relu : `verdicts.tsv` et `lots-correction.md` au commit fb0e8522, contre `lecture-recurrents.md`, les preuves des sièges et le code (lecture seule, sans Gradle). Branches en attente récupérées : ccr-6f42e003-295715 (langues), ccr-4cc0bb93-xljmti (gaucher), ccr-7bb3a043-2ay7kp (écoute), ccr-aad91c20-ow1wdm (kick).

## Verdicts revérifiés

| n° | verdict du tri | mon verdict | d'accord | raison (une ligne) |
|---|---|---|---|---|
| G1 | réel moyen, GuidePanel | réel moyen | oui | `GuidePanel` a la hauteur fixe `guidePanelHeightDp` et ses boutons sont en bas de son défilement ; la page au-dessus ne fait rien remonter. |
| T6 | réel mineur, PlainWords.TERMS | réel mineur | oui | `TERMS` n'a que 14 mots (BPM… 808) : rien pour mesure, temps, accord, riff, stab, gamme ; le tuto n'a pas d'autre glossaire (TutoPanel l. 453 n'appelle que `PlainWordsLines`). |
| R18 | réel moyen, LanguageList | réel moyen | oui | `initialFirstVisibleItemIndex = top + index - 2` fait défiler la section « Langue du téléphone » hors de vue, contre DECISIONS (lot langues : « proposée en tête »). |
| R19 | réel mineur, AppRoot | réel mineur | oui | Le `ScrollableTabRow` (AppRoot l. 3317) n'a pas de `fadingEdges`, TrackBar en a ; voir les lots pour les autres rangées. |
| U6 | réel moyen, StepGrid | réel moyen | oui | `follow` démarre à true et n'est coupé que par les puces de mesure ou le tuto, jamais par un toucher de case ; `followedBar` suit la tête. |
| R1 | réel mineur, SimpleMix.StripHead | réel mineur | oui | `maxLines = 2` + Ellipsis sur 76 dp (StripHead), `maxLines = 1` à 52 dp dans RemixThumbRow : la cause est bien une famille de cas. |
| N8 | réel mineur, PianoRoll TutoNotesLeft | réel mineur | oui | `tutoNotesLeft(left.size, …)` compte les notes, pas les touches d'accord. |
| U5 | réel mineur, ExportUi | réel mineur | oui | `ExportUi.format = ExportFormat.Wav` d'un côté, `SharePrefs` (M4A par défaut) de l'autre : deux réglages qui ne se parlent pas. |
| T4 | réel grave, PianoRoll | réel grave | oui | D b écran 60 : grille de y 518 à 700, panneau du tuto à 738, notes en pointillé hors de vue ; `spots.coverTop` ne sert qu'aux spots. |
| N10 | réel mineur, TutoController.markDone | réel mineur | oui | `markDone` → `tick(advance = true, bravo = false)` : l'étape suivante est prise tout de suite, donc jamais de « Bravo, chapitre fini ! » après une étape à cocher. |
| R8 | artefact du banc | **à prouver** (artefact probable) | non (mineur) | L'état d'attente se lit pourtant dans le texte du banc (« anneau pâle », vérifié par BenchSelfTest l. 148) ; le tri l'écrit lui-même « non tranché » : la règle de CLAUDE.md veut un test sans banc, pas un calcul de temps. Faire le test (lecture de l'écran juste après le toucher) dans le lot du banc avant de clore ; s'il est rouge, R8 va au lot 6. |
| U12 | artefact du banc | artefact du banc | oui | `shareMenu` titre avec `uri.lastPathSegment` ; `MyExports.share` envoie `ContentUris.withAppendedId` : « · 2 » est l'identifiant MediaStore. |
| U22 | artefact du banc | artefact probable + **reste réel mineur** | non (mineur) | `list == null` montre seulement `where()` pendant la lecture sur Dispatchers.IO : plausible côté banc, mais pas prouvé par un test ; et le reste « aucun signe d'attente » que le tri reconnaît n'est dans aucun lot alors que MyExports.kt est au lot 5. |
| U23 | artefact du banc | **réel mineur** | **non (grave)** | La preuve du tri dit elle-même que la file de ProjectsViewModel peut attendre 2 × 10 s (`ClosingSaves`/`TakeVault.awaitIdle`) sur un téléphone, et `EmptyProjects` n'apparaît que si `!ui.loading` : liste vide sans signe d'attente, réelle. ProjectsScreen.kt n'est dans aucun lot. |
| N15 | artefact du banc | **réel mineur** | **non (grave)** | `DoneBar` est le `snackbarHost` (AppRoot l. 2429) : il recouvre le contenu pendant 6 s sur un téléphone aussi ; le tri écrit « reste possible en 3 touches rapides ». Le banc rend le cas plus fréquent, il ne le crée pas. À mettre au lot 3 (AppRoot, plus DoneBar.kt). |
| T10 | voulu | voulu | oui | DECISIONS 2026-10-29 (l. 877) : le tuto part volontairement d'un tempo à régler (100 BPM si le défaut tombe sur le style) ; l'étape Tempo fait poser 132. |
| T12 | voulu | voulu | oui | DECISIONS 2026-11-04 (l. 1217) : fin de chapitre « repliée, un toucher déplie au lieu de sauter ». |
| T18 | voulu | voulu | oui (mineur) | Accueil : DECISIONS 2026-11-05 (l. 1268) couvre bien ; mais « ↓ 4 weitere » un par toucher relève de DECISIONS 2026-11-11 (l. 1446, `ScrollHintBox`), non citée ; « aucun style conseillé » n'est pas « voulu » par une règle, c'est une hésitation sans défaut. |
| R9 | voulu | voulu | oui (citation faible) | Deux fonctions différentes, chacune son « ? » ; mais la ligne de CLAUDE.md citée parle des captures, et DECISIONS 2026-11-03 définit « Enregistrer ce que je joue » sans trancher la coexistence des deux boutons. |
| U7 | voulu | voulu | oui | DECISIONS 2026-11-11 (l. 1418) : le ▶ du haut = « cette partie », ■ seulement en boucle ; pendant le morceau, c'est le ▶ du bas qui devient ■. |
| U8 | voulu | voulu | oui | DECISIONS 2026-11-05 (l. 1255) : « Son de X », allumé quand la piste sonne, et jamais M/S en Simple. |
| U19 | voulu | voulu | oui | DECISIONS 2026-11-05 (l. 1300) : le numéro d'un bout coche, toucher la courbe ne fait que placer la tête. |
| U24 | voulu | voulu | oui | DECISIONS 2026-11-10 (l. 1350) : « Mes fichiers » est sous Partager dans les deux modes, pas dans « Plus ». |
| U25 | voulu | voulu | oui | CLAUDE.md (Livraison) : changelog en français ; la Recette est la liste de Benjamin. |
| U26 | voulu | voulu | oui | DECISIONS 2026-11-05 (l. 1240) : mots de tous les jours en Simple (`I18n.EVERYDAY` a aussi Lead → Mélodie, Sub drop → Chute grave), le Pro garde les mots du métier. |
| N11 | voulu | voulu | oui | DECISIONS 2026-10-28 (l. 791) : « les parties se fabriquent d'un toucher ». |
| N16 | voulu | **réel mineur (en partie)** | **non (grave)** | Aucune ligne de DESIGN, SPEC ou DECISIONS : seulement un commentaire de code (« owner, 2026-10-29 »), et rien ne couvre la musique arrêtée sans le dire (`goHome` → `rhythmVm.stop()`). Renvoyer ça à Benjamin contredit CLAUDE.md (« prends les décisions pour moi ») : décider, l'écrire dans DECISIONS, et mettre l'arrêt muet au lot 3 (AppRoot). |
| R2 | déjà corrigé (langues) | déjà corrigé | oui | ccr-6f42e003 remplace le `Text(maxLines = 1, Ellipsis)` de `ThumbButton` par `WholeWordsText(minScale = 0.5f)` ; LanguagesScanTest balaie l'écran Simple à 200 % sur 360 dp (fr, de, ar, hi, zh-CN ; ni nl ni pt-BR, mais c'est le même composant). |

## Les lots

1. **Aucun fichier dans deux lots** : vrai pour les listes des six lots.
2. **Chaque réel ou doublon dans un lot** : vrai pour les 64 réels et les 5 doublons de `verdicts.tsv` (R4 est au lot 6 comme annoncé). Manquent les trois que je fais passer en réels (U23 → lot 6 ou un lot à part pour ProjectsScreen.kt ; N15 et N16 → lot 3), plus le reste de U22 (lot 5, MyExports.kt).
3. **Des défauts qui débordent sur le fichier d'un autre lot sans y être listés** :
   - T16 : « Basse en fantôme » coupé vit dans `PianoRoll.kt:ToolRow` (lot 1), absent du tableau du lot 1.
   - T2 : le pad nommé « Techno · 스네어 2 » vient de `RhythmViewModel.loadTutoKit` (lot 6) ; le test du lot 2 « y compris après le téléchargement du kit » ne peut passer sans toucher ce fichier.
   - R19 : seul AppRoot est au lot 3 ; les autres rangées citées sont dans SimpleMix.kt et LiveScreen.kt (lot 5) et TrackBar.kt (lot 6), et `ui-components/FadingScroll.kt` n'est dans aucun lot.
   - R1 : les cas de CutScreen.kt et RemixScreen.kt (« Guard / ar », « Baixar o modelo (174 ») vont au lot 4, mais ce lot n'a pas de test rouge pour eux.
4. **Tests rouges** : le test de R6/N14 ne couvre que « une piste audio existe » ; le cas de C b #19 (« Ton morceau est vide » pendant la boucle) n'est pas testé. Des lignes regroupent des défauts sans rapport en un seul test (U9/U10/U18/U21 ; T14/T19/N9 « consignes réécrites ») : il faut un test rouge par défaut, comme la règle commune le dit.
5. **Priorités** : l'ordre 1-2-3 (tuto, Guide), 4 (R3-EC17 bloquant puis R6 grave), 5 (langues), 6 (le reste) respecte la consigne ; les graves T4, N4 et N5 sont au lot 1, donc en premier.
6. **Branches en attente** : le lot langues touche aussi GuidePanel.kt (lot 3), RhythmScreen.kt (lot 1), SessionScreen.kt (lot 4) et LaunchChoice.kt (lot 6) ; le lot gaucher touche AppRoot.kt (lot 3). Il faut les fusionner avant les lots 1, 3, 4 et 6, pas seulement avant le lot 5.

Verdict : 3 désaccords graves (U23, N15, N16 : chacun ajoute un défaut réel à un lot), 11 mineurs (R8, U22, T18, R9, plus 7 points sur les lots : T16, T2, R19, R1, le test de N14, les tests regroupés, les branches en attente).
