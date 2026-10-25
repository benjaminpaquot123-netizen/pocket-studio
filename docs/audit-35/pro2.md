# Testeur Pro 2, vague 3 : ingé mix/mastering, « prêt à sortir » (candidat de la PR #47)

Base : `/home/user/ps-audit`, branche `audit` (0aa9bc7 : phases 34 à 39 + lot 1). Lecture seule du code. **Toute écoute passe par le moteur comme le téléphone la joue** : `scripts/live-bench.sh` (37 styles × 90 s, puis 150 s pour les 9 styles dont le Drop arrive tard), et `scripts/song-bench.sh --premaster --m4a --before docs/son/banc/apres-S5b.json` pour l'export. Mesures ajoutées : haut-parleur de téléphone (passe-haut 300 Hz), Drops répétés, piste lead seule, direct contre export. Tout est dans `docs/audit-35/mesures.md` ; journaux et JSON dans `scratchpad/audit-35/`. J'ai donné mon avis avant de relire `docs/audit-34/pro2.md`.

Gravité : **grave** (un pro refuse de sortir le morceau ou est trompé), **moyen** (il le corrige à la main à chaque fois), **mineur**.

## Parcours réel

1. « Nouveau morceau › Tech house » puis « Écouter ». En direct, l'Intro démarre à **−18,9 dBFS** court terme et monte seule à **−12,2** en 14 s : c'est le gain live de la cible qui part de 0 dB et grimpe d'1 dB/s (`engine/src/main/cpp/core/MasterChain.h:151` `driveDb_ = 0`, `MasterChain.cpp:441,497`). Le premier Drop (61 s) sonne à −10,7 en direct, **−1,9 LU** sous ce que l'export donnera (−8,9).
2. Mix › Sortie générale (`mixer_master.png`) : « −8,1 · très fort », « le son est retenu de 1,9 dB », Volume visé Auto · Club · Streaming · Désactivée, gain ajouté. Le compresseur montre sa réduction (`compressor_editor.png`). Rien de nouveau ici depuis la vague 2, mais derrière, le mix de l'assistant a changé (S5) : colle de bus RMS à 25 ms (`project-model/.../WizardMix.kt:215-218`), master à 30 ms (`:221-224`), Punch à 10 ms (`:227`), écrêteurs doux sur batterie et basse, vrai sidechain au son du kick avec un bus « Musique » (`:274-280`, `Project.kt:541-560`), cible sur le Drop avec 3 LU de perte au plus (`MasterChain.h:58`). C'est un mix honnête.
3. Export WAV : partie la plus forte −8,9 LUFS (cible −8, plafonnée), true peak −1,16 dBTP, Drop − Break +3,9, Drop − Groove +1,0, LRA 4,1, perte 3,0 LU pour +7,0 dB de gain. Export M4A : **−4,5 LU** sous le WAV (banc, AAC FFmpeg).
4. Haut-parleur de téléphone (modèle passe-haut 300 Hz) sur le Drop : −3,9 LU, présence 1–4 kHz −5,7 dB.

## Points durs

### 1. Le son selon les familles

**1.1 [grave] En direct, l'appli ne joue pas ce qu'elle exporte : Intro 6,5 LU trop basse au départ, Drop 1,4 LU sous l'export.**
- Preuve (`mesures.md` tableau C, `scratchpad/audit-35/live-all.json`) : Intro du direct à 4 s moins Intro exportée : médiane **−6,5 LU** (−10,5 phonk, −10,4 grime, −10,2 afrobeats, −9,5 hard techno, −8,5 jump up) ; le gain live atteint son régime en 11 à 32 s (jump up 32 s, rétro techno 31 s). Premier Drop en direct contre partie forte exportée : médiane **−1,4 LU** (hard techno −3,5, jump up −2,9, acidcore −2,6, disco −2,3) ; lofi à l'inverse +3,3 (le gain live dépasse l'export). Gain live max **+9,4 dB** en médiane contre +7,0 à l'export, jusqu'à +15,5 (riddim) et +15,1 (dubstep) : le direct pousse les parties calmes plus que l'export.
- Où : `MasterChain.h:151` (départ à 0 dB), `MasterChain.cpp:441` et `:497` (1 dB/s en montée, 2 dB/s en descente), `estimateDrop` (`:447-500`) : une estimation en temps réel du Drop, alors que l'export le connaît d'avance (deux passes, `OfflineRenderer`).
- Pourquoi : Benjamin juge le son au téléphone. Il entend un fondu d'ouverture qui n'existe pas, puis un Drop moins fort et moins dense que ce qu'il partagera. C'est la cause probable d'une partie de « ça sonne amateur » sur les 15 premières secondes de chaque écoute.
- Fix : calculer le gain du Drop hors ligne à l'ouverture du morceau (rendu rapide de 8 mesures du premier Drop, ou relu du dernier export), le poser dès le ▶, et ne garder l'adaptation lente que pour les changements de mix ; afficher « estimation en cours » tant qu'il n'est pas figé (constat 8 de la vague 2, pas réglé). Banc : ajouter « Intro direct − Intro export ≥ −1 LU » et « Drop direct − Drop export ≥ −1 LU » aux critères de `live-bench.sh`.

**1.2 [grave] Le M4A partagé est 3 LU plus bas que le WAV (jusqu'à 5,4).**
- Preuve (banc `--m4a`, `mesures.md` tableau A) : premier encodage AAC décodé : true peak médian **+0,4 dBTP** (jusqu'à +2,9) ; après la vérification de l'appli, −1,8 dBTP (37/37 ≤ −1), mais la sonie a perdu **3,0 LU** en médiane (neurofunk −5,4, tech house −4,5, rétro techno −4,4, phonk −4,3, detroit −4,1). Les extraits de l'audit ont dû être baissés de 1,4 à 4,0 dB pour la même raison.
- Où : `export/AacEncoder.kt:25-29` (`CEILING_DBTP = -2f`, `headroomGain`), et le réencodage plus bas quand le décodé dépasse −1 dBTP (`M4aGuard`).
- Réserve : le banc encode avec l'AAC de FFmpeg (« fast ») ; le téléphone avec MediaCodec, dont le dépassement peut être plus petit. À mesurer sur le téléphone (un export M4A de Benjamin, décodé).
- Pourquoi : « Partager » propose le M4A par défaut ; le morceau arrive sur WhatsApp 3 LU sous un titre du commerce, après tout le travail de la chaîne master.
- Fix : un limiteur true peak **sur le signal décodé** (ou sur la source suréchantillonnée ×8 avec 1 dB de marge propre à l'AAC) au lieu d'un gain global ; encoder à 256 kbit/s au moins ; écrire dans le rapport d'export « M4A −x LU sous le WAV ». Pros : Apple « Mastered for iTunes » (afclip) vérifie le décodé et corrige la crête, pas le niveau entier.

**1.3 [moyen] Le Drop ne monte pas au-dessus du Groove (15/37), et le Break remonte (24/37 au-dessus de +4).**
- Preuve (`mesures.md` A) : Drop − Groove médiane **+1,8 LU** (cible ≥ +2) ; techno +1,8, house **+0,6**, acid house +0,5, tech house +1,0, Detroit +0,7, synthwave +0,9 ; avant master la médiane est +3,1 : la chaîne en retire encore 1,3. Drop − Break +4,0 (trance **+1,6**, acidcore +0,5, hard techno +2,2, acid techno +3,0, house +3,9, tech house +3,9). Régression mesurée depuis S5b (`docs/son/banc/apres-S5b.json`) : Drop − Break 4,3 → 4,0 en médiane, house 4,26 → 3,94, tech house 4,14 → 3,89, deep house 3,96 → 3,64, amapiano 11,3 → 10,4 ; techno passe au plafond (partie forte −8,32 → −8,57).
- Où : en techno, le Drop n'ajoute que charley ouvert et ride (`project-model/.../StyleRecipes.kt`, techno : `drop = Layers(setOf(Role.OpenHat, Role.Ride), setOf())`) ; le plafond de perte de 3 LU agit sur le Drop seul (`MasterChain.h:58`), le Groove passe entier.
- Fix : un Drop qui ajoute une couche de grave ou de lead (basse +1,5 dB au Drop, prévu en S2 et pas tenu), Groove sans ride ni charley ouvert, et le critère Drop − Groove ≥ +2 rendu bloquant au banc (aujourd'hui seulement listé, `docs/son/BANC.md` « Critères bloquants »).

**1.4 [moyen] Toujours étroit : côté/centre du Drop −18,3 dB en médiane, 8 styles sur 37 dans la cible.**
- Preuve (`mesures.md` B, 8 premières mesures du Drop) : acidcore **−35,8**, acid techno −30,1, lofi −25,6, acid house −24,3, neurofunk −23,7, house **−23,3**, phonk −22,8 ; dans la cible : trance −10,9, dubstep −11,3, minimal −11,8, hard techno −12,0, dnb −12,5, jump up −7,4 (trop large : le lead « basse wobble » passe par un chorus et un ping-pong, voir pro1). Corrélation du morceau 0,96 (cible 0,3–0,85). Grave mono < 120 Hz : 1,000 partout, c'est bien.
- Où : aucune piste mélodique n'est pannée (`project-model/.../WizardMix.kt:391-400`, `PANS_V2` : la batterie seule) ; la 303 est mono ; le chorus somme en mono à l'entrée (`engine/src/main/cpp/core/Effects.cpp:335`, `chorus_.Process(0.5f * (l[i] + r[i]))`).
- Fix : pans par rôle pour les pistes mélodiques (nappe ±0,2 en doublure, contre-chant ±0,3, perc mélodiques), chorus stéréo à l'entrée, délai ping-pong sur la 303 (acid), Élargisseur sur la nappe des styles house.

**1.5 [moyen] Au haut-parleur de téléphone, le Drop perd 4,1 LU en médiane, jusqu'à 9 ; la 808 et la basse ronde disparaissent.**
- Preuve (`mesures.md` B ; extraits `*-avant-telephone.m4a`, même gain) : perte au passe-haut 300 Hz : lofi **9,0**, afro house 7,1, amapiano 7,1, boom bap 7,0, drill 6,5, afrobeats 6,1, deep house 6,1, minimal 5,8, trap 5,7 ; les moins touchés : jump up 1,2, hard techno 2,4, riddim 2,7, dubstep 2,8 (leur grave a déjà des harmoniques médium). Extrait trap : −6,3 LU entre la version normale et la version téléphone, house −4,7, techno −4,5, jump up −1,1.
- Pourquoi : les morceaux de rap et de house sont ceux qui « ne tapent pas » sur le téléphone, exactement ce que dit `scratchpad/basses-qui-tapent.md` (808 inaudibles à −32/−45 dB à 300 Hz).
- Fix : chantier basses (saturation à harmoniques 150–800 Hz qui suivent la note, couche médium, « tone » d'une 808 qui monte au-dessus de 200 Hz) ; critère de banc : perte tél. ≤ 4 LU et 300–800 Hz ≥ −4 dB au Drop, par famille.

**1.6 [moyen] Le volume reste en partie acheté au master : +7,0 dB de gain pour 3,0 LU de perte, 19 styles au plafond, 7 plafonnés sous leur cible.**
- Preuve : rapport du banc, « Perte dans la chaîne » 3,0 LU médiane ; partie forte sous la cible : hard techno **−2,4 LU** (−8,4 pour −6), riddim −2,3, dubstep −1,3, neurofunk −1,3, jump up −1,2, techno −1,1, dnb −1,0. En direct, le limiteur descend jusqu'à **−8,5 dB** (gabber), −8,3 (tech house), −7,9 (techno) sur une crête (`mesures.md` C).
- Mieux que la vague 2 (+8,6 dB pour 4,7 LU, Drop − Break −0,2) : la chaîne ne retourne plus la courbe d'énergie. Mais les kicks et basses n'apportent pas encore la densité qui rendrait ce gain inutile.
- Fix : kits v3 et basses du chantier (déjà sur branche) ; cible hard techno ramenée à −7 tant que le kit n'est pas plus dense ; afficher la perte au Drop dans « Prêt à partager ? ».

**1.7 [mineur] L'air est bon, le grave aussi.** Air > 6 kHz −17,4 dB (cible −20 à −16), grave 60–250 Hz −3,7 (≥ −6) : réglés depuis la vague 2 (−20,7 et trou de 8 à 11 dB en bass music). Exceptions : afrobeats air −24,2, lofi −22,8, minimal −22,3 (volontaires pour lofi, pas pour afrobeats).

**1.8 [grave] Bass music : la « réponse » au lead est une basse sourde, et la mélodie DnB que Benjamin trouve « très cheap » se mesure.**
- Preuve (lead seul, premier Drop, `mesures.md`) : jump up, le lead est la « Basse wobble » à l'octave 3 : centroïde **252 Hz**, présence au téléphone **−18,2 dB**, boue **+20,3 dB** ; neurofunk (lead « Basse growl ») 409 Hz, brillance **−38,4 dB** ; dnb (Supersaw) 602 Hz, brillance −11,8 (dans la cible) mais boue +6,4 ; liquid 618 Hz, brillance −19,6. Le jump up est aussi le seul style trop large au Drop (côté/centre −7,4 dB, corrélation 0,74) : chorus et ping-pong posés sur une basse wobble (`docs/guide/diagnostic-jump-up.md` § 1.3, `WideMix`), avec un grave pourtant bien mono (< 120 Hz : 1,000). En bass music le lead ne joue que 15 ou 16 mesures sur 31 du premier Drop.
- Croisé avec `diagnostic-jump-up.md` § 1.4 : basse rejouée 8 fois par Drop sans variation, Sub sans saturation (`WideMix.SUB_DRIVE` annoncé, absent de la tranche du projet de Benjamin), Voix sans effet ; tout est toujours vrai dans la PR #47 (mêmes recettes).
- Fix : le chantier leads (lot A : sub et octave −1 retirés, coupures ouvertes ; lot D : couche médium large au-dessus de 250 Hz pour la « 2e basse ») et le chantier basses (switch toutes les 8 mesures, saturation du Sub) ; en jump up, chorus et ping-pong seulement au-dessus de 250 Hz.

### 2. Les choix musicaux du générateur, vus du mix

**2.1 [grave] Trois Drops identiques.** Empreinte d'énergie Drop 2 / Drop 1 **0,97** en médiane, 25 styles sur 37 ≥ 0,95, Δ sonie +0,0 LU (`mesures.md` B). Seul hard techno (0,65, +0,9 LU), grime (0,74), neurofunk (0,82, Basse 2) bougent. Code : `project-model/.../SongShape.kt:328-331`, la scène `drop2` est construite une fois (`by lazy`) et rejouée à chaque Drop. Un ingé ne peut pas « mixer » un deuxième Drop qui n'existe pas ; un pro ajoute une couche, change la basse, monte le lead d'une octave, ouvre un filtre. Fix : une scène Drop par passage (Drop 2 = basse de remplacement ou couche, lead à l'octave, charleys 1/16 ; Drop 3 = tout + crash et impacts) ; critère banc empreinte ≤ 0,9.

**2.2 [moyen] La nappe joue dans tous les Drops.** `project-model/.../StarterWizard.kt:1548` : la scène Drop est `lines to parts` (toutes les parties). La nappe remplit 150–500 Hz sous la basse et le lead : boue mesurée sur les leads +4 à +20 dB (`mesures.md`, lead seul), et `docs/guide/diagnostic-jump-up.md` § 1.4 (« Nappe partout »). Fix : nappe absente du Drop 1A ou filtrée passe-haut 300 Hz au Drop, retour au Break.

**2.3 [moyen] Pas une piste mélodique pannée, un seul niveau (faders à 0 dB).** `diagnostic-jump-up.md` § 1.3 (« Faders : tous à 0,8 ») ; `WizardMix.kt:391` (pans de batterie seulement). Le mix de l'assistant repose sur des courbes de volume et le master. Fix : balance de départ par rôle (lead −2, nappe −6, sub −3) et pans, posés par `WizardMix` et dits dans le guide.

### 3. Outils de production

**3.1 [moyen] Sidechain : une seule clé pour tout le morceau, pas d'entrée sidechain sur le compresseur, pas de genou.** `Project.kt:545` (`sidechainKey` unique), `Effects.kt:77-89` (compresseur : seuil, ratio, attaque, relâche, gain, auto, détection, filtre, mélange). La pompe sur une nappe déclenchée par la caisse, ou une basse qui s'efface sous la voix, sont impossibles. Fix : champ « Écoute » (une voie) sur le compresseur et la Pompe ; genou doux/dur.

**3.2 [moyen] Deux envois seulement, pré-délai en millisecondes.** `Effects.kt:449` (`Buses.COUNT = 2`) ; réverbe plate, salle ou hall et égaliseur de retour (`:460-470`) : **mieux** qu'en vague 2 (Freeverb seul, pas d'EQ). Manquent une 2e réverbe (courte + longue) et un pré-délai au tempo. Fix : 4 bus, pré-délai en 1/64–1/16.

**3.3 [moyen] Pas de couches par piste.** Une piste = un son (`project-model/.../Tracks.kt:17-38`) ; `Track.layers` existe sur la branche `g1b` (545c2f2, 254f9d9 pour le moteur), pas dans la PR #47. Un lead pro, c'est 2 ou 3 couches (`docs/hits/leads-pros.md` § 8 lot D). Fix : fusionner G1b avant le chantier leads, avec écoute « avec / sans » par couche.

**3.4 [moyen] Stems : la batterie en un seul fichier.** `app/.../rhythm/StemExports.kt:37` (« batterie » = toute la batterie). Pour un mastering ailleurs, kick / caisse / charleys séparés sont la base. Fix : un fichier par pad joué (ou par groupe kick / snare-clap / hats / perc), option « stems de batterie ».

**3.5 [mineur] Le compteur du limiteur peut afficher « retenu de 214748368,0 dB ».** Capture `tablet/nav_mix.png` ; `rhythm/RhythmViewModel.kt:5458` (compteurs à 0 au départ) et `mixer/MixerScreen.kt:273` (`log10(0)`). Avec un moteur indisponible, « Prêt à partager ? » compterait aussi ce 0 comme un limiteur qui force (`export/ShareSheet.kt:463`). Fix : compteur limiteur initialisé à 1.

**3.6 [mineur] Le rapport d'export ne dit ni Drop − Break, ni LRA, ni l'écart M4A/WAV.** Pareil qu'en vague 2 (n° 10).

### 4. L'interface pour mixer (téléphone et tablette)

**4.1 [moyen] La console tient 4,5 voies sur téléphone, 5 sur la tablette 11" portrait (moitié gauche de l'écran), 6 en paysage.** `nav/mix.png` (« Lea » coupé), `tablet/nav_mix.png` (la moitié droite empile Automation et Sortie générale), `nav/land_mix.png` (4,5 voies sous 3 bandes). Aucun vumètre de voie sur les faders (seulement le master). Fix : en largeur ≥ 600 dp, console pleine largeur ; vumètre par voie ; envois réverbe/écho sur la tranche.

**4.2 [moyen] Aucune capture du Mix à la taille de la Galaxy Tab A11** (`tablet8/` n'a que l'ancien `mixer.png`, sans la Sortie générale) : voir pro3 n° 8.

**4.3 [mineur] LUFS / TP seulement dans Mix › Sortie générale.** Pas dans le bandeau de lecture (vague 2 n° 8, pareil).

### 5. Régressions depuis la vague 2

- Mesurées depuis S5b (même banc) : Drop − Break −0,3 LU en médiane, house, deep house et tech house repassent sous +4 ; techno passe au plafond. Petit, mais à surveiller : aucun commit de son n'est annoncé entre S5b et le lot 1.
- Nouvelles : le direct qui démarre à 0 dB de gain (1.1) et le M4A plus bas (1.2) étaient déjà là en vague 2 mais n'étaient pas mesurés ; ils le sont maintenant.
- Aucune régression du true peak (37/37), du continu, des NaN, des alertes du direct (0 sur 37).

## Ce qui sépare encore nos morceaux d'une vraie production (par impact audible)

| # | Écart | Preuve | Vraie production |
|---|---|---|---|
| 1 | **Leads sombres et boueux** (vieux synthé) | centroïde du lead 252–845 Hz en club (296 en techno), brillance −12 à −38 dB (cible atteinte : dnb, hard techno, trance), boue +4 à +20 dB sauf deep house (`mesures.md`, lead seul) | brillance −8 à −14, boue ≤ 0, présence tél. ≥ −7 |
| 2 | **Le direct n'est pas l'export** | Intro −6,5 LU au départ, Drop −1,4 LU | ce qu'on entend = ce qu'on sort |
| 3 | **Drops identiques, nappe partout** | empreinte 0,97 ; Δ 0,0 LU ; `SongShape.kt:328-331`, `StarterWizard.kt:1548` | chaque Drop ajoute ou change une couche |
| 4 | **M4A 3 LU plus bas** | médiane −3,0, jusqu'à −5,4 | crête vraie corrigée sur le décodé |
| 5 | **Étroit** | côté/centre Drop −18,3 dB, 8/37 dans la cible | −15 à −9 |
| 6 | **Ne tape pas au téléphone (rap, house)** | perte tél. 5,7 (hip-hop), 4,8 (house), jusqu'à 9,0 | harmoniques 150–800 Hz qui suivent la note |
| 7 | **Drop − Groove trop faible** | +1,8 médiane, house +0,6 | +2 à +4 LU |
| 8 | **Plafond de sonie** sur 7 styles | hard techno −2,4 LU sous cible | 1 à 3 dB de limiteur suffisent |

## Fonctions manquantes (classées)

1. Gain du direct figé sur le Drop dès le ▶ (calcul hors ligne), « estimation en cours » affiché.
2. M4A corrigé par un limiteur sur le décodé ; écart M4A/WAV dans le rapport.
3. Une scène par passage de Drop, et une balance de départ (pans, faders) posée par l'assistant.
4. Couches par piste (`Track.layers`) avec A/B, puis le lot A du chantier leads.
5. Sidechain par effet (« Écoute : voie »), genou, 4 envois, pré-délai au tempo.
6. Stems de batterie par pad.
7. Vumètres par voie, LUFS/TP dans le bandeau, console pleine largeur sur tablette.
8. Critères de banc : direct = export (±1 LU), perte tél., empreinte des Drops ≤ 0,9, Drop − Groove ≥ +2 bloquant.

## Peut-on avoir un bon mix sans être expert ?

Plus qu'avant : le mix de l'assistant est maintenant celui d'un ingé prudent (une seule compression sur le coup de kick, un vrai sidechain, une cible sur le Drop, 3 LU de perte au plus, 0 alerte en direct). Le non-expert ne peut toujours pas l'**entendre** tel quel : le téléphone joue une Intro qui monte seule et un Drop plus bas que l'export, puis le M4A qu'il partage perd encore 3 LU. Et il ne peut pas l'**améliorer** sans savoir où agir : rien ne lui dit que ses Drops sont identiques, sa nappe partout, ses pistes au centre.

## Comparé à la vague 2 (mon Top 5 du 23/10)

| Vague 2 | Vague 3 | Détail |
|---|---|---|
| 1. Sonie achetée (+8,6 dB pour 4,7 LU, kick sinus 47 Hz de 1,5 s) | **mieux** | +7,0 dB pour 3,0 LU, perte plafonnée au Drop ; kits v3, kicks accordés et 808 (S3). Reste : 7 styles plafonnés sous leur cible. |
| 2. Cible intégrée qui écrase le Drop et remonte le Break | **réglé** | Cible sur la partie la plus forte, Drop − Break +4,0 (−0,2 avant), LRA 4,9 (1,7), Drop − fin de Montée +2,3, bloquant à +1. |
| 3. Quatre étages sur le même transitoire | **réglé** | Punch à 10 ms, colles RMS à 25/30 ms (`WizardMix.kt:215-227`), écrêteurs doux de bus. |
| 4. Mono et fermé | **mieux** | Air −17,4 (−20,7), côté/centre du morceau −16,8 (−24,8) ; mais le Drop reste à −18,3 et aucune piste mélodique n'est pannée. |
| 5. Pompe par motif, double ; gain adaptatif en direct | **mieux / pareil** | Vrai sidechain au son du kick, bus Musique (réglé) ; le gain adaptatif du direct, lui, est pareil et maintenant mesuré (1.1). |

**Nouveau depuis la vague 2** : banc d'écoute en direct en CI (37/37 sans alerte) ; M4A vérifié sous −1 dBTP ; réverbes plate et salle avec égaliseur de retour ; automation des potards du synthé ; gel de piste.

## Top 5 (vague 3)

1. **Le direct = l'export** : gain du Drop calculé avant le ▶, plus d'Intro qui monte seule (−6,5 LU au départ), plus de Drop 1,4 LU sous l'export ; critère au banc d'écoute.
2. **Le timbre des leads** (le « vieux synthé ») : brillance, boue, sub et octave −1 retirés, ladder remplacé dès qu'il y a de l'unisson (lot A de `leads-pros.md`), puis les couches.
3. **Des Drops qui changent et une nappe qui s'efface** : une scène par passage, la nappe hors du Drop 1A, Drop − Groove ≥ +2 bloquant.
4. **M4A à la sonie du WAV** : limiteur sur le décodé, écart affiché.
5. **Largeur et téléphone** : pans par rôle, chorus stéréo, et les basses du chantier pour que le rap et la house tapent au haut-parleur (perte tél. ≤ 4 LU).
