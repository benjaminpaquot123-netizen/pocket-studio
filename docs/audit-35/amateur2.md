# Testeur Amateur 2, vague 3 : ado trap / drill, projet VIDE, 808, pads, voix rap

(Branche `audit`, commit `0aa9bc7`, tête de la PR #47. Cette fois l'appli a **tourné** dans Robolectric, `AppRoot` entier via `AppHarness`, sans moteur audio : téléphone 400 dp, 130 %, Galaxy Tab A11 debout 600 × 934 dp et couchée 1006 × 528 dp, néerlandais. `shots/` = `/tmp/claude-0/-home-user-pocket-studio/2ed29bda-5e9a-5850-a6a7-bbf3293c7a09/scratchpad/audit-35/shots/`. Chemins de code relatifs à `app/src/main/java/com/pocketstudio/app/`. Un geste = une touche, un défilement, un appui long ou un glisser.)

Résumé : **la batterie et la 808 ont fait un bond énorme (Kit ▾, feuille de pad avec volume et pan, roulements ×6 / ×8 montants, rafale 1/32, chance de jouer, Basse 808 qui glisse sans pack). Ce qui me freine encore : pas de kit trap sans téléchargement, la latence rangée ailleurs, et un haut d'écran toujours chargé sur téléphone.**

## Parcours réel

| Étape | Ce que je fais | Gestes | Capture |
|---|---|---|---|
| 1. Projet vide | « Que veux-tu faire ? » › Nouveau morceau › défiler sous 5 familles, « Rythme de départ » et 3 démos › « Projet vide » | **3 + 1 défil.** | `shots/amateur2/phone_projetvide_01_nouveau_projet.png`, `…_02_projet_vide.png` |
| 2. Tempo 140 | « 120 BPM » › puce 140 | **2** | `shots/amateur2/phone_tempo_01_tempo.png` (70 / 90 / 120 / 140 / 174, ½, ×2, pavé, « Taper en rythme ») |
| 3. Changer le kit | Sons › Batterie › « Kit : Classique ▾ » › un kit | **3** (2 depuis le nom « Batterie » de Structure) | `shots/amateur2/phone_batterie_02_kit.png` |
| 4. Kick + clap + hats | une touche par case, **ou glisser pour peindre** (« Touche une case pour ajouter un coup, glisse le doigt pour en peindre plusieurs ») | **≈ 4** | `shots/amateur2/phone_batterie_01_batterie.png`, `tabL_batterie_01_batterie.png` |
| 5. Roulement de hi-hat | appui long sur un coup › ×6 ou ×8 › Montant | **3** | `shots/amateur2/phone_batterie_04_menu_coup.png` (Force, Roulement ×1 à ×8, Montant / Égal / Descendant, « Rafale de 1/32 sur le temps », Chance de jouer 100 à 25 %) |
| 6. Volume du kick | appui long sur le pad › « Volume du pad » | **2** | `shots/amateur2/phone_batterie_03_feuille_pad.png` (Son, Tous les sons…, Volume du pad, Pan du pad, Couper ce pad) |
| 7. Piste 808 qui glisse | Sons › « + Piste » › « Basse 808 · La basse du trap qui glisse d'une note à l'autre » | **3** | `shots/amateur2/phone_piste808_01_ajouter_piste.png` ; moteur 808 sans pack (`project-model/…/NewTracks.kt:95-98`, `ENGINE_808`) |
| 8. Boucle de mélodie | « + Piste » › « Écrire une mélodie pour moi » | **3** | même capture |
| 9. Rapper | Sons › Voix › ● (la piste « Voix » se crée ; « Enregistrer sur : La partie A · Intro / **Tout le morceau** », « À partir de la partie 1. A · Intro… ») | **3** | `shots/amateur2/phone_voix_01_voix.png`, `…_02_prise_voix.png` |
| 10. Latence | « Astuce : mesure la latence une fois (Plus › Latence) » en bas de l'écran Voix | **≈ 3, ailleurs** | `shots/amateur2/tabP_voix_02_prise_voix.png` |
| 11. Structure | appui long sur un drapeau › Variation / Break / Montée / Fin / Énergie | **2 par partie** | `shots/amateur3/phone_feuillepartie_02_feuille_partie.png` |
| 12. Partager | ⤴ › Partager | **2** | `shots/amateur3/phone_partage_01_pret_a_partager.png` |

Un morceau trap complet depuis un projet vide : environ **60 à 70 gestes** (contre 120 à 150 en vague 2), sans téléchargement obligatoire.

## Points durs

| # | Gravité | Appareil | Étapes pour reproduire | Capture | Correction en une ligne |
|---|---|---|---|---|---|
| 1 | moyen | tous | Sons › Batterie › Kit ▾ | `shots/amateur2/phone_batterie_02_kit.png`, `tabP_batterie_02_kit.png` | « Dans l'appli » : **Studio** et **Classique** seulement ; un kit trap / drill demande un pack : livrer un kit « Trap » et « Drill » intégrés (sons du moteur 808 / studio) dans cette feuille. |
| 2 | moyen | tous | Sons › Voix › ● | `shots/amateur2/tabP_voix_02_prise_voix.png` | La latence se mesure toujours dans Plus › Latence : bouton « Mesurer maintenant (1 min) » dans la carte « Enregistrer sur », proposé avant la première prise. |
| 3 | moyen | Tab L | Sons › Batterie, tablette couchée | `shots/amateur2/tabL_batterie_01_batterie.png`, `tabL_batterie_04_menu_coup.png` | La 4e rangée de pads (Cymbale, Shaker, Conga, Zap) est coupée en bas à 528 dp de haut : pads carrés plus petits (hauteur = hauteur libre / 4) en paysage. |
| 4 | mineur | tous | Nouveau projet | `shots/amateur2/phone_projetvide_01_nouveau_projet.png` | « Projet vide » toujours tout en bas, sous les démos (pareil qu'en vague 2) : une ligne « Projet vide » à côté de « Nouveau morceau guidé ». |
| 5 | mineur | tél., 130 % | Sons › Batterie | `shots/amateur2/phone_batterie_01_batterie.png`, `p130_batterie_01_batterie.png` | Outils « Coups · Roulements · Pan · Hauteur · Force » : « Hauteur » coupé au bord, « Force » caché (pareil) ; barre fixe à icônes + mots courts. |
| 6 | mineur | Tab P | appui long sur un coup | `shots/amateur2/tabP_batterie_04_menu_coup.png` | Le menu du coup est trop étroit : « ×8 » part seul sur une 2e ligne ; largeur mini 360 dp. |
| 7 | mineur | tél. | Sons › n'importe quelle puce | `shots/amateur2/phone_batterie_01_batterie.png` | Avant le 1er pad : barre (56) + bande de lecture (48) + « 5/10 · Ajoute une mélodie » (28) + puces (52) + « Partie : A · Intro » (28) ≈ 210 dp ; replier le fil « Prochaine étape » dans la bande de lecture. |
| 8 | mineur | tous | Sons › Voix | `shots/amateur2/phone_voix_01_voix.png` | La puce s'appelle « Voix · Micro » et cache aussi Remix, Accordeur, Générer : l'appeler « Voix et audio » ou sortir Remix (voir amateur3). |
| 9 | mineur | tous | ● de la barre du haut | `shots/amateur2/phone_voix_02_prise_voix.png` | Le même rond rouge veut dire micro sur Voix et « ce que je joue » ailleurs (pareil qu'en vague 2) : icône micro + mot sous le rond sur Voix. |
| 10 | mineur | tous | Plus › Aide › fil « Prochaine étape » | — (`onboarding/NextSteps.kt:118`) | Étape « Basse » : « génère une ligne dans Sons › Basse acide » même pour un projet trap : proposer « + Piste › Basse 808 » quand le tempo / le style est trap. |

## Ce qui manque pour finir un morceau trap / drill

- Un kit trap / drill complet en une touche **sans** pack (point 1).
- La latence mesurée depuis l'écran Voix (point 2).
- Des triolets dans la grille (pas vus ; les roulements et la rafale 1/32 sont là).
- Un haut d'écran plus fin sur téléphone (point 7).

## Comparé à la vague 2

| Top 5 de la vague 2 | Verdict | Pourquoi |
|---|---|---|
| 1. 808 dans « Ajouter une piste », glisse allumée, sans pack | **Réglé** | « Basse 808 · La basse du trap qui glisse » (capture), moteur 808 intégré avec glisse (`NewTracks.kt:95-98`). Le conseil du fil d'étapes parle encore de la basse acide (point 10). |
| 2. Peindre les cases, ×6 / ×8 montants, 1/32, kit trap en un geste | **Presque réglé** | Peinture au doigt, ×1 à ×8, Montant / Égal / Descendant, rafale 1/32, chance de jouer : tout est là. Kit ▾ existe mais sans kit trap intégré (point 1). |
| 3. « Faire le plan du morceau », un seul mot | **Mieux** | Un seul mot « Partie » ; la feuille de partie fait Variation, Break, Montée, Fin. Pas de bouton « plan intro / couplet / refrain / fin » en un geste. |
| 4. Voix : latence, compteur, refaire une phrase, prises, recaler | **Presque réglé** | « Enregistrer sur la partie / tout le morceau », « Refaire mes. 5-8 », Prises, « Recaler sur le tempo », 8 pistes audio (code : `audiotracks/AudioTracksScreen.kt:417, 542`). Reste la latence dans Plus (point 2). |
| 5. Haut d'écran : enlever le transport en double, rester sur ma partie | **Réglé / mieux** | Plus de carte transport sur Rythme ; l'édition pendant la lecture boucle la partie (`rhythm/RhythmViewModel.kt:1929-1943`). Il reste quatre bandeaux au-dessus des pads (point 7). |

Autres points de la vague 2 : ↷ qui disparaissait de la barre → **réglé** (visible sur toutes les captures, grisé quand rien à refaire) ; « Projet vide » en bas → **pareil** ; outils qui défilent → **pareil** ; ● à double sens → **pareil** ; « Importer » en double → non revu.

**Nouveau dans cette vague** : la 4e rangée de pads coupée sur la tablette couchée (point 3) et le menu du coup trop étroit sur la tablette debout (point 6).

## Top 5 maintenant
1. **Kit trap et drill intégrés** dans la feuille « Kit de batterie » (point 1).
2. **Latence depuis l'écran Voix**, proposée avant la première prise (point 2).
3. **Pads entiers sur la tablette couchée** (point 3).
4. **Haut d'écran allégé** sur téléphone : fil d'étapes replié, outils de la grille sans défilement (points 5, 7).
5. **« Projet vide » en haut** de Nouveau projet et conseil « Basse 808 » pour un projet trap (points 4, 10).
