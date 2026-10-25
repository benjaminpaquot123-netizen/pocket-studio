# Audit 35 « morceau complet », 3e vague (2026-10-24, 5 amateurs + 3 pros + tablette + gestes + mesures)

Rapports détaillés : `docs/audit-35/` (amateur1-5, pro1-3, tablette, gestes, mesures). Version auditée : branche `audit`, 0aa9bc7 (tête de la PR #47, fusionnée depuis : `main` = e0ff315, seul `CrashReporter.kt` diffère). Cette fois l'appli a **tourné** (Robolectric, `AppRoot` entier, touche après touche) sur téléphone 360 / 400 dp, à 130 %, sur la Galaxy Tab A11 de Benjamin debout (600 × 934 dp) et couchée (1006 × 528 dp), et en néerlandais ; le son a été **écouté comme le téléphone le joue** (`scripts/live-bench.sh`, moteur temps réel) en plus du banc d'export. Plus l'enquête sur le kick tiré à gauche (commit 1eef67c).

Chemins abrégés : `shots/` = `/tmp/claude-0/-home-user-pocket-studio/2ed29bda-5e9a-5850-a6a7-bbf3293c7a09/scratchpad/audit-35/shots/` (captures de l'audit, hors dépôt) ; `app/…` = `app/src/main/java/com/pocketstudio/app/` ; `model/…` = `project-model/src/main/kotlin/com/pocketstudio/model/` ; `core/…` = `engine/src/main/cpp/core/`.

## 1. En trois lignes, pour Benjamin

1. L'appli se prend enfin en main : les 15 gestes clés tiennent leur budget sur ton téléphone comme sur ta tablette, et les cinq testeurs saluent le Mode simple, la fiche de partie (fin, break, montée en deux touches) et le gros bouton « Enregistrer ma mélodie ».
2. Ce qui cloche encore, c'est surtout le son : la mélodie « vieux synthé », des Drops tous pareils, une écoute sur le téléphone qui démarre trop bas, et le fichier envoyé un peu moins fort que prévu. Côté écran : des fenêtres qui ignorent ta taille 130 %, le clavier qui disparaît sur la tablette couchée, et le Remix introuvable.
3. Ton kick à gauche, ce n'était pas un fantôme : le geste « retour » depuis le bord gauche de l'écran attrape au passage le réglage gauche-droite du pad. On le corrige dans le prochain lot, avec tout le reste. Comme dirait Perceval : « C'est pas faux. »

## 2. Ce qui s'est amélioré depuis l'audit 34 (vague 2), et les régressions cherchées

**Réglé ou nettement mieux** (verdicts des 8 testeurs sur leur Top 5 de la vague 2) :
- **Gestes** : les 15 actions clés font **26 gestes**, pile le budget, sur les quatre appareils, même à 130 % et sur la tablette couchée (`gestes.md` ; vague 2 : ≈ 70). Retouches d'un morceau House : ≈ 35 gestes (≈ 150) ; un trap depuis un projet vide : 60 à 70 (120 à 150).
- **Navigation** : 5 espaces, une seule Structure, un seul ▶, « Partie » partout (0 « scène » dans les textes, 61 avant), Live hors du chemin de création, carte « Ton morceau est prêt » qui se replie pendant la lecture (ton retour du 24/10).
- **Mode simple** : 4 onglets, « Enregistrer ma mélodie » en gros, « Remettre en rythme / Refaire / Garder », Envoyer en 2 touches, familles repliées (lot 1).
- **Fiche de partie** : Variation, Fill, Break, Montée, Fondu d'entrée / de sortie, Ajouter une fin, Énergie 1 à 4, en **2 gestes** chacun (9 à 16 avant).
- **Accords du morceau** avec leurs noms, écoute avant, « Appliquer à tout le morceau » : 3 gestes (≈ 30).
- **Batterie et 808** : peindre au doigt, roulements ×2 à ×8 montants, rafale 1/32, chance de jouer, volume et pan par pad même en kit synthétisé, Kit ▾, « Basse 808 » qui glisse sans pack.
- **Live** : notes jouables, REC sur les pads plein écran, pads enregistrés en piste « Pads live », « Nouvelle version » par défaut.
- **Production** : automatiser un potard du synthé en 2 gestes (11), piano roll et presse-papiers communs, gel de piste.
- **Confiance** : « Que veux-tu faire ? » à l'ouverture, zéro projet permis, export aux étapes nommées avec Annuler, « Signaler un bug » clair et honnête.
- **Son** : chaîne master honnête (+7,0 dB de gain pour 3,0 LU de perte, contre +8,6 pour 4,7), Drop − Break +4,0 LU en médiane (−0,2 en vague 2), air −17,4 dB (−20,7), true peak 37/37, **0 alerte sur 37 en écoute directe** (le fondu jusqu'au silence est bien parti), formes techno de 5 à 6 min.

**Régressions cherchées** :
- **Aucune perte de fonction** (8 testeurs sur 8). Les 15 gestes n'ont pas bougé depuis la phase 36.
- **Son, petit recul mesuré depuis S5b** (même banc, `docs/son/banc/apres-S5b.json` contre `avant-audit35.json`) : Drop − Break 4,3 → 4,0 LU en médiane ; house 4,26 → 3,94, tech house 4,14 → 3,89, deep house 3,96 → 3,64, amapiano 11,3 → 10,4 ; la techno passe au plafond (partie forte −8,32 → −8,57 LUFS) ; Detroit Drop − Groove 0,94 → 0,70. Aucun commit de son annoncé entre S5b et le lot 1 (sans doute les fusions 36-39 : 303 qui suit les accords, boucles de 16 mesures) : **à expliquer en tête de S6**, sinon l'avant et l'après ne se comparent pas.
- **Perception** : la forme techno longue (choix S2) repousse le premier Drop après 94 à 123 s sur 9 styles.
- **Révélés par la nouvelle méthode** (pas des régressions, mais nouveaux dans les rapports) : le 130 % absent des fenêtres, le clavier perdu sur la tablette couchée, pas de « ? » sur Live, l'Intro qui monte seule en direct et le M4A plus bas (là depuis la vague 2, mesurés seulement maintenant), le kick tiré à gauche.

## 3. Les constats, classés

Lots : **Lot 2** (interface et bugs, branche `lot2`) · **S6 son** (direct = export, M4A, contrastes, largeur, téléphone, banc) · **Leads** (timbre, `docs/hits/leads-pros.md` § 8, branche `leads`) · **Basses** (branche `basses`, `docs/hits/basses-qui-tapent.md`) · **Guide G0** (déjà construit sur la branche `g0` : Drops qui diffèrent, chaînes par rôle, Drop 1A dépouillé, pans de départ ; G1a et les couches G1b sont sur la branche `guide`) · **Guide G2-G6** (plus tard) · **Plus tard** (outils de pro, hors plan pour l'instant).

### Graves

| # | Constat | Qui · appareil | Preuve | Code | Lot |
|---|---|---|---|---|---|
| G1 | **Kick tiré tout à gauche sans y toucher.** Le geste retour d'Android depuis le bord gauche tombe sur les curseurs pleine largeur « Pan du pad » : le curseur saute sous le doigt à −1, puis le système reprend le geste. Pad 0 = kick choisi par défaut, donc c'est toujours le kick. Aucune `systemGestureExclusion` dans l'appli. Dans son « Jump up DnB » : kick à −1 dans les parties A-D, G, H. | Benjamin · téléphone (navigation par gestes) | `app/src/test/…/audit35/KickPanJourneyTest.kt` échoue (reproduit son fichier partie par partie) ; `project-model/…/audit35/WizardKickPanTest.kt` passe (l'assistant ne touche pas au pan) | `app/…/rhythm/RhythmScreen.kt:826` (carte du pad choisi), `app/…/rhythm/KitAndPadSheets.kt:209` (feuille de pad) | **Lot 2**, en premier |
| G2 | **Le 130 % ne passe dans aucune fenêtre ni feuille** : « Que veux-tu faire ? », Bienvenue, feuille de partie, « Prêt à partager ? », export, kit, pad, tempo, Signaler un bug restent à 100 %, plus petites que l'écran derrière. | amateur 4, tablette · tous à 130 % | `shots/../cmp_launch.png`, `shots/amateur3/p130_feuillepartie_02_feuille_partie.png`, `shots/amateur3/tabP130_partage_01_pret_a_partager.png`, `shots/amateur3/tabP130_exportetapes_01_fabrication.png` | `app/…/ui/UiScale.kt:17-29` (`UiScaled` dans la fenêtre principale seulement ; chaque `Dialog` / `ModalBottomSheet` reprend la densité de l'appareil : 13 fichiers à feuilles, 21 à fenêtres) | **Lot 2** |
| G3 | **Tablette couchée : le clavier disparaît** après une prise (60 dp de touches à 100 %, rien à 130 %, même à l'ouverture), Mode simple et Sons ; bande vide de 80 dp au-dessus du bouton rouge. | amateur 4, tablette · Tab A11 couchée | `shots/amateur4/tabL_parcours_05_fin_de_prise.png`, `shots/amateur1/tabL130_clavier_01_clavier.png`, `shots/amateur1/tabL130_clavier_04_remis_en_rythme.png` | écran clavier (Sons et Mode simple) | **Lot 2** (avec M7 : pads en paysage) |
| G4 | **Remix introuvable** alors que tu veux le mettre en avant : 4 gestes depuis l'ouverture, 3 depuis Structure, **impossible en Mode simple** ; le mot « Remix » n'apparaît qu'au 3e geste (Sons › « Voix · Micro » › onglet Remix). | amateur 3, gestes, pro 3 · tous | `shots/amateur3/phone_remix_01_ouverture.png` → `…_05_remix.png`, `shots/amateur3/phone_remixsimple_01_menu_sans_remix.png` | `app/…/onboarding/LaunchChoice.kt:319-323`, `app/…/ui/AppRoot.kt:1212-1218`, `app/…/learn/Missions.kt` | **Lot 2** (carte d'accueil, menu « ⋯ » du Mode simple, puce « Remix » dans Sons) |
| G5 | **Le direct ne joue pas ce qu'on exporte** : l'Intro démarre 6,5 LU trop bas et monte seule en 11 à 32 s (un faux fondu d'ouverture) ; le premier Drop sonne 1,4 LU sous l'export (hard techno −3,5, jump up −2,9). | pro 2, pro 3, pro 1 · téléphone (écoute directe) | `docs/audit-35/mesures.md` tableau C ; `docs/son/banc/audit35/live-all.json` | `core/MasterChain.h:151` (`driveDb_ = 0`), `core/MasterChain.cpp:441,497` (1 dB/s), `estimateDrop` `:447-500` | **S6 son** (gain du Drop calculé hors ligne avant ▶) |
| G6 | **Le M4A partagé est 3,0 LU sous le WAV** (jusqu'à −5,4 en neurofunk) : pour tenir la crête, tout le morceau est encodé plus bas. Réserve : banc encodé par FFmpeg, le téléphone par MediaCodec. | pro 2 · tous (« Partager » propose le M4A) | `mesures.md` « En bref » et tableau A (colonne M4A) | `app/…/export/AacEncoder.kt:25-29` (`CEILING_DBTP`, `headroomGain`), `M4aGuard` | **S6 son** (limiteur sur le décodé ; écart affiché) |
| G7 | **Le lead « vieux synthé »** (ton mot) se mesure : techno centre à 296 Hz, brillance −19,9 dB, boue +12,6 dB ; tech house présence au téléphone −18,9 ; jump up 252 Hz et boue +20,3 ; neurofunk brillance −38,4. Brillance dans la cible pour 3 styles seulement (dnb, hard techno, trance). | pro 1, pro 2 · tous | `mesures.md` « La piste lead seule » ; extrait `scratchpad/audit-35/techno-avant.m4a` (Drop à 7,3 s) | `model/…/SynthPatch.kt:106` (sub 0,2), `model/…/WideMix.kt:46,59` (octave −1 à 30 %), `core/Synth.cpp:304` (phases figées), `core/Effects.cpp:335` (chorus en mono) | **Leads** (lot A, puis B, C ; D avec les couches) |
| G8 | **Les Drops sont identiques** : empreinte Drop 2 / Drop 1 = 0,97 en médiane (25 styles sur 37 ≥ 0,95), écart de volume +0,0 LU. | pro 1, pro 2, pro 3 · tous | `mesures.md` tableau B, dernière colonne | `model/…/SongShape.kt:328-331` (`drop2 by lazy`, une scène rejouée) | **Guide G0** (déjà construit : variation 0,06 → 0,62 au banc modèle) ; à remesurer avec l'empreinte audio |
| G9 | **« Écouter » joue 2 minutes d'intro** : le premier Drop arrive à 94-123 s sur 9 styles (toute la techno, trance, jungle), alors qu'on juge un morceau en 20-30 s. | pro 3, pro 1 · tous | `mesures.md` colonne « Drop à » | `app/…/onboarding/SongReady.kt:254` | **Lot 2** (voir question 2) ; le banc direct de 90 s ne voit pas ces Drops → **S6 son** |
| G10 | **Aucune capture à la taille de ta tablette** pour les 5 espaces, et des captures « A11 » qui montrent l'ancien écran Morceau (qui n'existe plus). | pro 3, tablette · Tab A11 | `app/screenshots/tablet8/session.png`, `timeline.png`, `land_timeline.png` | `app/src/test/…/nav/NavScreenshotTest.kt:111-128`, `app/src/test/…/ScreenshotTest.kt:1870-1893` | **Lot 2** (captures 600 × 934 et 1006 × 528, à 100 % et 130 %, une feuille ouverte à 130 %) |

### Moyens

| # | Constat | Qui · appareil | Preuve | Code | Lot |
|---|---|---|---|---|---|
| M1 | **Mode simple : la mélodie part dans « Synthé »** (la première piste), pas dans « Mélodie » ; message « La boucle passe à 4 mesures… ↶ en haut » incompris ; pas d'« Écouter ma prise » avant Garder. | amateur 1, amateur 4 · tous | `shots/amateur1/phone_clavier_02_enregistre.png`, `…_03_fin_de_prise.png`, `shots/amateur1/tabP130_clavier_02_enregistre.png` | — | **Lot 2** |
| M2 | **Étiquettes « ? » qui se chevauchent**, boutons flottants posés sur la liste ou les faders ; **pas de « ? » sur Live**. | amateur 1, amateur 5, tablette · tous | `shots/amateur1/phone_etiquettes_01_etiquettes.png`, `tabL_etiquettes_01_etiquettes.png`, `shots/amateur5/phone_etiquettes_02_arret_help_labels_button.png` | — | **Lot 2** |
| M3 | **Deux « Parties » sur le même écran** (la liste du morceau et la grille A à H) : nommer la grille « Briques ». | amateur 3, tablette · tous, surtout Tab couchée | `shots/amateur3/tabL_parties_01_liste_parties.png` | — | **Lot 2** |
| M4 | **Pas de kit trap ni drill sans pack** (Kit ▾ : Studio et Classique). | amateur 2 · tous | `shots/amateur2/phone_batterie_02_kit.png` | — | **Lot 2** (kits intégrés, sons du moteur 808 / studio) |
| M5 | **Assistant** : « Gamme Fa phrygienne · accords i – i – i – II » sous chaque ambiance, « Créer tout de suite » en lien maigre, « Étape 1 sur 13 » avant le choix du style. | amateur 1, amateur 4 · tous | `shots/amateur1/phone_assistant_03_etape_02.png`, `shots/amateur1/tabP_premier_03_apres_guide.png` | — | **Lot 2** (texte de l'assistant) |
| M6 | **Live** : « Lancer à » se change à l'aveugle (pas de puces) ; en tablette debout, REC et « Tout couper » = deux ronds rouges sans mot. | amateur 5 · tous / Tab debout | `shots/amateur5/phone_live_01_live.png`, `tabP_live_04_pads_plein_ecran.png` | `app/…/live/LiveScreen.kt:505` | **Lot 2** |
| M7 | **Tablette couchée, Sons › Batterie** : 4e rangée de pads coupée, outils de la grille coupés. | amateur 2, tablette · Tab couchée | `shots/amateur2/tabL_batterie_01_batterie.png` | (le Mode simple dimensionne déjà ses pads : `shots/amateur4/tabL_simple_02_rythme.png`) | **Lot 2** |
| M8 | **Latence rangée dans Plus** au lieu de l'écran Voix. | amateur 2 · tous | `shots/amateur2/tabP_voix_02_prise_voix.png` | — | **Lot 2** |
| M9 | **Drop à peine au-dessus du Groove** : +1,8 LU en médiane (15/37 ≥ +2), house +0,6, acid house +0,5, Detroit +0,7 ; Drop − Break 24/37 ≥ +4. Avant le master la médiane est +3,1 : la chaîne en retire 1,3. | pro 1, pro 2 · tous | `mesures.md` « En bref », tableau A | `model/…/StyleRecipes.kt` (techno : le Drop n'ajoute que charley ouvert et ride), `core/MasterChain.h:58` | **S6 son** (+ Guide G0 : Drop 1A) |
| M10 | **Étroit** : côté/centre du Drop −18,3 dB (8/37 dans la cible −15 à −9) ; acidcore −35,8, acid techno −30,1, house −23,3. G0 pose des pans par rôle mais ne change pas la largeur mesurée (−16,8 → −16,9 sur le morceau). | pro 1, pro 2 · tous | `mesures.md` tableau B ; `docs/son/banc/apres-G0.md` (branche `g0`) | `model/…/WizardMix.kt:391-400`, `core/Effects.cpp:335` | **S6 son** (ping-pong 303, chorus stéréo, élargisseur nappe house) ; **S7 son** : atteint, −11,7 dB, 33/37 (`docs/son/banc/S7.md`) |
| M11 | **Au haut-parleur du téléphone, le Drop perd 4,1 LU** en médiane : lofi 9,0, afro house et amapiano 7,1, boom bap 7,0, drill 6,5, trap 5,7 ; la 808 et la basse ronde disparaissent. | pro 2, pro 3 · téléphone | `mesures.md` tableau B ; extraits `*-avant-telephone.m4a` (trap −6,3 LU) | — | **Basses** (« Basse audible », 808 v2, coupe-bas) + **S6 son** (critère perte tél. ≤ 4 LU) ; **S7 son** : atteint, 3,8 LU, 23/37 (`docs/son/banc/S7.md`) |
| M12 | **Volume en partie acheté au master** : +7,0 dB de gain, 19 styles au plafond de perte, 7 plafonnés sous leur cible (hard techno −2,4 LU). | pro 2 · tous | `docs/son/banc/avant-audit35.md` « Critères » | `core/MasterChain.h:58` | **Basses** + **S6 son** |
| M13 | **La nappe et le pluck jouent dans tous les Drops**, dans le bas-médium déjà chargé par le lead. | pro 1, pro 2 · tous | boue du lead seul +4 à +20 dB (`mesures.md`) | `model/…/StarterWizard.kt:1548` | **Guide G0** (Drop 1A dépouillé en bass music) ; à étendre à la techno en **S6 son** |
| M14 | **Couches par piste absentes** (une piste = un son) ; ta demande du 24/10. | pro 1, pro 2, pro 3 · tous | — | `model/…/Tracks.kt:17-38` ; `Track.layers` sur la branche `guide` (G1b) | **PR Guide** (G1b déjà construit) |
| M15 | **Rien ne dit ce que l'assistant a posé** (25 effets, 2 bus, 10 courbes) ni ce qui manque (Drops pareils, nappe partout, pistes au centre). | pro 3 · tous | `docs/guide/diagnostic-jump-up.md` § 2 | `GuideAnalyzer` sur la branche `guide` (G1a) | **Guide G2-G6** |
| M16 | **Riffs qui « tombent juste »** : notes de l'accord sur les temps forts 1,00 pour 27 leads sur 35 (les hits : 0,55-0,85), même nuance (0,46) dans 13 styles. | pro 1 · tous | `docs/son/MELODIE.md` | `model/…/MelodyBuilder.kt` | **Plus tard** (après l'A/B des leads : timbre d'abord, tu l'as dit) |
| M17 | **Paysage et tablette mal remplis** : piano roll coupé (une mesure en paysage), ligne du temps à 3 pistes en paysage téléphone, console sur 2/3 de la largeur, 4 bandes avant le contenu. | pro 1, pro 3, amateur 3 · tél. paysage, tablettes | `app/screenshots/nav/land_structure.png`, `tablet/land_piano_roll.png`, `shots/amateur3/tabP_mix_01_console.png` | — | **Plus tard** (après les captures A11 du lot 2, qui les montreront) |
| M18 | **Outils de mixage** : une seule clé de sidechain, 2 envois, stems de batterie en un fichier, pas de vumètre par voie. | pro 1, pro 2 · tous | — | `model/…/Project.kt:545`, `Effects.kt:449`, `app/…/rhythm/StemExports.kt:37` | **Plus tard** |

### Mineurs (tous **Lot 2**, sauf mention)

- **Limiteur « retenu de 214748368,0 dB »** quand le moteur n'a pas encore répondu (compteur parti à 0, `log10(0)`), et fausse alerte possible dans « Prêt à partager ? » : `app/…/mixer/MixerScreen.kt:273` (`LimiterLine`, `:269-285`), `app/…/rhythm/RhythmViewModel.kt:5458`, `app/…/export/ShareSheet.kt:463` ; `shots/amateur3/tabL_mix_01_console.png`.
- **Textes** : « Autres exports … dans Mix » et fil d'Ariane « ← Plus · Aide » en Mode simple (qui n'a ni Mix ni Plus) ; « environ 3 minutes » pour un techno de 5 min 20 ; « (son illisible : Shaker) » montré à un débutant ; « Nouvel essai 2/3 : compression » qui se lit comme un échec ; « 1 erreurs » (`app/…/report/BugReportSheet.kt:368`) ; « Decay » au milieu du français ; « lus dans les notes » ; « + A · Intro » ; « Synthé », « Basse acide · 303 », « Bend », « Modul. », « +0,0 dB » à mettre dans les mots de tous les jours (`model/…/i18n/I18n.kt:57`) ; « Pads » du Live traduit « Akkoordpads » en néerlandais ; conseil « Basse acide » du fil d'étapes sur un projet trap (`app/…/onboarding/NextSteps.kt:118`).
- **Écrans** : Mes projets vide avec 3 boutons « nouveau » et 2 « importer » ; « Projet vide » tout en bas ; « Signaler un bug » en 11e ligne de Plus (un défilement) ; deux cartes d'automation dans Mix ; nom du projet coupé à « Mon … » à 130 % ; « Mon morceau » qui touche le bord du rail ; ⤴ ● ↶ sans mot sur tablette ; écrans d'accueil à moitié vides en tablette debout ; menu du coup trop étroit (« ×8 » seul sur sa ligne) ; puces coupées sans indice de défilement ; « Tout couper » sans mot en portrait.
- **Méthode** : le budget de gestes ne compte pas « Continuer » de « Que veux-tu faire ? » ni la barre des espaces (`app/src/test/…/ui/GestureBudgetTest.kt:66-84`) ; `docs/DESIGN.md` dit 4 effets par voie, l'appli en a 8.
- **Plus tard** : l'éditeur suit le morceau par défaut pendant la lecture (`app/…/rhythm/RhythmViewModel.kt:2004`) ; rapport d'export sans Drop − Break, LRA ni écart M4A/WAV (**S6 son** pour l'écart M4A).

### Le correctif du kick (G1), précisé

- Curseur de pan « sûr » partout où il y a un pan de pad : zone exclue du geste retour (`Modifier.systemGestureExclusion`) **ou** glisser seulement depuis le bouton (un toucher ailleurs ne saute plus) ; aimant au centre ; double-toucher = centre.
- Marge de 24 à 32 dp à gauche des curseurs pleine largeur (carte du pad, feuille de pad, et les autres curseurs pleine largeur en revue).
- Un pad décentré l'affiche sur sa case (« G 100 », « D 40 ») pour qu'on le voie sans ouvrir la feuille.
- Une ligne au journal (Signaler un bug) à chaque changement de pan, avec l'écran d'où il vient.
- `KickPanJourneyTest` passe au vert ; son morceau se répare en un geste (« Remettre le kick au centre » dans toutes les parties) ou à la main dans la feuille de pad.

## 4. Les mesures « avant » (à comparer avec l'« après »)

Tout le détail par style : `docs/audit-35/mesures.md`. Données figées dans le dépôt (le dossier de l'audit est temporaire) : `docs/son/banc/avant-audit35.json` et `.md` (banc d'export), `docs/son/banc/audit35/` (direct, téléphone, lead seul, extraits) et `docs/son/banc/audit35/outils/` (les scripts de l'audit, copiés tels quels : leurs chemins de travail sont en tête de fichier, à adapter).

| Mesure (médiane sur 37 styles) | Avant (0aa9bc7) | Cible | Lot qui doit la bouger |
|---|---|---|---|
| Sonie de la partie la plus forte | −8,5 LUFS (30/37 sans plafond) | cible du style ±1 | Basses, S6 |
| True peak export | −1,18 dBTP (37/37) | ≤ −1,0 | (garder) |
| M4A − WAV | **−3,0 LU** (−5,4 à −0,5) | 0 | S6 |
| Drop − Break exporté | +4,0 LU (24/37) | ≥ +4 | S6, G0 |
| Drop − Groove / Couplet | **+1,8 LU** (15/37) | ≥ +2 | S6, G0 |
| Drop − fin de Montée (le plus petit) | +2,3 LU (25/25) | ≥ +1 (bloquant) | (garder) |
| Perte dans la chaîne / gain de la cible | 3,0 LU (19 au plafond) / +7,0 dB | ≤ 3 | Basses |
| Côté/centre du Drop | **−18,3 dB** (8/37) | −15 à −9 | S6 |
| Corrélation L/R du morceau | 0,96 | 0,3 à 0,85 | S6 |
| Grave 60-250 Hz au Drop / air > 6 kHz | −3,7 dB / −17,4 dB | ≥ −6 / −20 à −16 | (garder) |
| Perte au haut-parleur de téléphone | **4,1 LU** (1,2 à 9,0) | ≤ 4 par famille | Basses, Leads |
| Présence 1-4 kHz au téléphone | −6,5 dB | — | Leads |
| Drop 2 contre Drop 1 : empreinte · Δ | **0,97** · +0,0 LU | < 0,9 | G0 |
| Direct : Intro à 4 s − Intro exportée | **−6,5 LU** | 0 (±1) | S6 |
| Direct : Drop − Drop exporté | **−1,4 LU** | 0 (±1) | S6 |
| Direct : gain live max | +9,4 dB (export +7,0) | — | S6 |
| Direct : alertes (fondu, silence, clic, NaN) | 0 (37/37 sur 90 s, 9/9 sur 150 s) | 0 | (garder) |
| Direct : premier Drop à | 55-123 s en club, 10-16 s en rap | — | Lot 2 (Écouter), S6 (banc) |

| Lead seul, premier Drop | Centroïde | Présence tél. | Brillance | Boue | Cible (`leads-pros.md` § 8) |
|---|---|---|---|---|---|
| techno | 296 Hz | −8,9 dB | −19,9 dB | +12,6 dB | présence ≥ −7 · brillance −8 à −14 · boue ≤ 0 |
| tech house | 525 Hz | −18,9 | −23,8 | +17,4 | idem |
| house | 563 Hz | −8,2 | −18,1 | +7,1 | idem |
| dnb | 602 Hz | −8,4 | −11,8 | +6,4 | idem |
| jump up | 252 Hz | −18,2 | −21,5 | +20,3 | idem |
| neurofunk | 409 Hz | −11,5 | −38,4 | +12,2 | idem |

Par famille (perte tél. · empreinte Drop 2 · Intro direct − export) : Techno 3,7 · 0,98 · −5,8 ; House 4,8 · 0,96 · −5,4 ; Bass music 3,1 · 0,91 · −7,7 ; Hip-hop 5,7 · 0,99 · −8,4 ; Autres 4,6 · 0,98 · −8,1.

Extraits à réécouter (30 s depuis 4 mesures avant le premier Drop, AAC 192 kbit/s, plus une version « haut-parleur » au même gain) : techno, house, trap, jump up, acid techno, dubstep, dans `scratchpad/audit-35/<style>-avant.m4a` et `-avant-telephone.m4a`. Ils se refont à l'identique depuis 0aa9bc7 (graines fixes) avec `extracts.py` si le dossier de l'audit a disparu.

**Commandes exactes de l'« après »** (même machine si possible, depuis la racine du dépôt) :

```bash
# 1. Banc d'export, 37 styles, comparé à l'avant (WAV, avant master, M4A décodé)
scripts/song-bench.sh --premaster --m4a --before docs/son/banc/avant-audit35.json --out build/bench
# 2. Écoute en direct comme le téléphone, 37 styles x 90 s
scripts/live-bench.sh --out build/live
# 3. Les 9 styles au Drop tardif, 150 s (jusqu'au premier Drop)
scripts/live-bench.sh --styles techno,detroit,acid_techno,hard_techno,industrial_techno,retro_techno,minimal,trance,jungle --seconds 150 --out build/live-long
# 4. Téléphone, largeur, Drops répétés (rend chaque morceau, mesure, efface ; régler B et T en tête du script)
bash docs/son/banc/audit35/outils/phone_all.sh
# 5. Lead seul, par style (piste notée META lead dans le .pscmd)
f=build/bench/cmds/techno.pscmd
build/bench/tools/ps_render --solo "$(awk '/^META lead/{print $3}' "$f")" "$f" /tmp/solo.wav
python3 -I docs/son/banc/audit35/outils/lead.py "$f" /tmp/solo.wav
# 6. Direct contre export, puis les tableaux de mesures.md et les extraits
python3 -I docs/son/banc/audit35/outils/live.py build/live/log build/bench/cmds
python3 -I docs/son/banc/audit35/outils/mdtables.py
python3 -I docs/son/banc/audit35/outils/extracts.py <dossier> build/bench/cmds <dossier des WAV> techno house trap jump_up acid_techno dubstep
```

Les extraits « après » se comparent à volume égal (±0,5 LU), au même endroit, en ordre tiré au sort (`leads-pros.md` § 9).

## 5. La liste de recette proposée (`app/src/main/assets/recette.json`, à réécrire au jalon « lot 2 + son »)

**Ajouter** :
- `kick_pan` · **Le kick reste au centre** : Sons › Batterie, fais le geste retour depuis le bord gauche plusieurs fois ; le pan du kick ne bouge pas. Un pad décentré affiche « G » ou « D » sur sa case ; double-toucher le curseur le remet au centre.
- `scale130` · **130 % partout** : Réglages › Taille 130 %, puis ouvre « Que veux-tu faire ? », une feuille de partie, ⤴ et l'export : tout est à la même taille que l'écran derrière.
- `tablet` · **Ta tablette, debout et couchée** : Mode simple › Clavier, enregistre une mélodie tablette couchée (100 % puis 130 %) : le clavier reste là ; Sons › Batterie couchée : les 16 pads tiennent.
- `remix` · **Remix en vitrine** : « Remixer une chanson » depuis « Que veux-tu faire ? » (1 touche), depuis Sons (1 touche), et en Mode simple (menu « ⋯ »).
- `simple_melody` · **Ma mélodie dans « Mélodie »** : Mode simple › Clavier › Enregistrer : la prise va dans la piste Mélodie, « Écouter ma prise » avant Garder, plus de message sur les boucles.
- `help_labels` · **Les « ? » lisibles** : sur Structure, Mix et Live, les étiquettes ne se chevauchent pas et ne cachent rien.
- `first_listen` · **Écouter tombe vite sur le Drop** : crée un Techno, touche Écouter : le Drop arrive en moins de 20 s (selon ta réponse à la question 2).
- `live_level` · **Ce que j'entends = ce que j'envoie** : écoute un morceau du début : l'Intro ne monte plus toute seule ; puis exporte-le en M4A et en WAV et écoute les deux : même volume.
- `drops_differ` · **Deux Drops différents** : écoute le Drop 1 puis le Drop 2 d'un Jump up et d'un Techno : le 2e ajoute ou change quelque chose.
- `ab_sound` · **Avant / après à l'oreille** : 6 extraits (techno, house, trap, jump up, acid techno, dubstep), au téléphone à mi-volume puis aux écouteurs : « lequel sonne le plus comme un vrai morceau ? » (remplace `song_sound`).
- `phone_bass` · **Les basses au haut-parleur** : un trap et une house sur le haut-parleur du téléphone : la basse s'entend.
- `layers` · **Plusieurs sons sur une piste** (si la PR Guide est fusionnée) : ajoute un son au lead, écoute avec / sans, retire-le.
- `trap_kit` · **Kits trap et drill** : Kit ▾ propose Trap et Drill sans téléchargement.
- `start_screens` · **Accueil, zéro projet, Signaler un bug** : « Que veux-tu faire ? » à l'ouverture, supprimer tous ses projets, envoyer un rapport de bug (vu par la tablette : jamais demandé jusqu'ici).

**Garder** (à faire sur ton appareil, pas encore confirmés) : `voice`, `keep_played`, `live_versions`, `instruments`, `export`, `video`, `midi`, `headphones`, `freeze`, `clipboard`.

**Retirer** (vus fonctionner pendant l'audit, sur téléphone et tablette) : `mode`, `spaces`, `missions` (repris par `help_labels`), `part_sheet`, `kit_pad` (repris par `kick_pan`), `chords`, `poly`, `track808`, `glossary`, `long_club` (repris par `first_listen`), `record` (repris par `simple_melody`), `song_sound` (remplacé par `ab_sound`).

## 6. Les questions pour Benjamin (réponse du type « 1A, 2B, 3A, 4A »)

1. **Ton projet « Jump up DnB » comme modèle de test** (le dépôt est public) :
   - A (conseillé) : il reste hors du dépôt ; les tests le refabriquent à l'identique (c'est ainsi qu'on a trouvé le kick). → Rien à faire pour toi, rien de toi n'est publié.
   - B : il entre dans le dépôt, chiffré comme la clé de signature. → La CI rejoue ton vrai fichier ; tu ajoutes un secret GitHub (2 minutes).
   - C : il entre en clair. → N'importe qui peut le télécharger.
2. **Où démarre « Écouter » sur « Ton morceau est prêt »** :
   - A (conseillé) : 8 mesures avant le premier Drop, avec « Depuis le début » juste à côté. → Tu entends le Drop en 15 s environ, même en techno.
   - B : au début, avec un 2e bouton « ▶ Le Drop ». → Deux boutons ; la première écoute reste 2 minutes d'intro en techno.
   - C : on ne change rien. → Jusqu'à 2 minutes avant le premier Drop.
3. **Le nom de la carte Remix** sur l'écran d'accueil et en Mode simple :
   - A (conseillé) : « Remixer une chanson », avec dessous « sépare voix, batterie, basse et rejoue-les ». → Le mot que tu emploies, et ce que ça fait.
   - B : « Séparer une chanson ». → Plus exact, moins envie de cliquer.
   - C : « Remix » seul. → Court, mais rien ne dit ce que ça fait.
4. **Quand veux-tu l'APK** (ta règle : rien sans ton feu vert) :
   - A (conseillé) : un seul, quand le lot 2, le son (S6, basses, leads) et le Guide sont prêts, avec les extraits avant / après. → Une seule recette, la comparaison complète d'un coup.
   - B : un dès le lot 2 (kick, tablette, 130 %, Remix), le son ensuite. → Tu retrouves ton kick au centre plus tôt, deux recettes.
   - C : un à chaque étape. → Le plus suivi, mais quatre ou cinq recettes.
