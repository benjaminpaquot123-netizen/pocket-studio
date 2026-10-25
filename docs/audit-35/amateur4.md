# Testeur Amateur 4 — vague 3 (avant la fusion de la PR #47)

60 ans, peu à l'aise avec la technique, lit tout, **Galaxy Tab A11**, interface à **130 %**, debout et couchée (sur son support). Joue un peu de piano. Veut un morceau calme (« Chill » / lo-fi) avec SA mélodie jouée au clavier, l'écouter du début à la fin, l'envoyer à ses petits-enfants. Elle a choisi le **Mode simple**.

Méthode : l'appli a **tourné** (Robolectric, `AppRoot` entier via `AppHarness`, branche `audit`, commit `0aa9bc7`) dans la fenêtre réelle de sa tablette : 600 × 934 dp debout et 1006 × 528 dp couchée (1340 × 800 px, barre d'état 24 dp et barre de navigation 48 dp retirées), à 100 % puis à **130 %** (Réglages › Taille de l'interface), plus le téléphone à 130 % pour comparer. Sans moteur audio (rien ne sonne). `shots/` = `/tmp/claude-0/-home-user-pocket-studio/2ed29bda-5e9a-5850-a6a7-bbf3293c7a09/scratchpad/audit-35/shots/`, `app/…` = `app/src/main/java/com/pocketstudio/app/`. J'ai fait mon avis d'abord, puis relu la vague 2.

## Parcours réel

### 1. Ouvrir l'appli (1 geste) — mais pas à sa taille
« Que veux-tu faire ? » (`shots/amateur1/tabP130_ouverture_01_que_veux_tu_faire.png`, `shots/amateur4/tabP130_parcours_01_ouverture.png`) : « Continuer · Mon premier rythme » en grand, « Nouveau morceau », « Mes projets ». Le choix est bon. Mais **cet écran ignore ses 130 %** : il est dessiné à 100 % (on le voit à côté du téléphone à 100 % : `shots/../cmp_launch.png`, les deux de gauche ; le titre est même plus petit à 130 %). Sur la tablette debout il occupe le haut de l'écran en petit, la moitié du bas est vide.

### 2. Créer un morceau calme (3 gestes) — réglé
Nouveau morceau › **« Calme (Chill, Lo-fi) »**, la première famille › « Créer le morceau » sur Lo-fi hip-hop (`shots/amateur1/tabP130_ouverture_03_famille_calme.png`). Elle trouve « calme » du premier coup, et l'ambiance conseillée du Lo-fi est maintenant « Rêveur » (`project-model/…/StyleRecipes.kt:124`). Si elle passe par l'assistant guidé : « Étape 1 sur 13 » s'affiche **avant** qu'elle choisisse son style (le Lo-fi en a 10), et « Créer tout de suite (choix conseillés) » reste un lien maigre (`shots/amateur1/tabP_assistant_03_etape_02.png`).

### 3. Jouer SA mélodie et la garder (2 gestes + 2) — réglé debout, cassé couchée
Onglet **Clavier** (1) › gros bouton rouge **« ● Enregistrer ma mélodie »** (2) › elle joue › ■ › « Ta prise : 4 notes · **Garder · Remettre en rythme · Refaire** » › « Comme joué · 50 % · 75 % · 100 % » (`shots/amateur1/tabP130_clavier_01_clavier.png` → `…_04_remis_en_rythme.png`). C'est exactement la fenêtre qu'elle demandait depuis la vague 1. Trois réserves :
- la prise part dans la piste « **Synthé** » (la première puce, cochée d'office), pas dans « Mélodie » ;
- la bande grise « La boucle passe à 4 mesures pour ta prise, et cette partie aussi dans le morceau. ↶ en haut pour revenir à 1 mesure. » : elle la lit trois fois et ne comprend pas ;
- **tablette couchée** : le clavier est repoussé sous le bas de l'écran. À 100 %, il ne reste qu'un ruban de touches de 60 dp une fois la prise faite (`shots/amateur4/tabL_parcours_05_fin_de_prise.png`) ; **à 130 %, le clavier disparaît entièrement** et la carte de la prise est coupée avant ses boutons (`shots/amateur1/tabL130_clavier_01_clavier.png`, `…_04_remis_en_rythme.png`). Une bande vide d'environ 80 dp reste entre « Clavier / Notes » et le bouton rouge.

### 4. Écouter du début à la fin (1 geste) — réglé
▶ en haut, « Intro · mes. 1/3 » dans la bande, sur tous les onglets.

### 5. Envoyer aux petits-enfants (2 gestes) — réglé, sauf la taille
Onglet **Envoyer** (1) › « Partager » (2) (`shots/amateur4/tabP130_simple_04_envoyer.png`, `shots/amateur1/tabL130_envoyer_01_envoyer.png`) : nom pré-rempli, durée, « Tout est prêt : aucun solo oublié, aucune piste coupée », M4A coché. La boîte « Je prépare ton morceau… » qui suit est **à 100 %** (une fenêtre, comme « Que veux-tu faire ? »), `shots/amateur3/tabP130_exportetapes_01_fabrication.png`.

### 6. Toutes les fenêtres et feuilles restent à 100 %
Feuille de partie, feuille « Prêt à partager ? » (Mode complet), feuille du volume d'une piste, kit, pad, tempo, Signaler un bug, « Que veux-tu faire ? », Bienvenue, export : toutes s'ouvrent dans leur propre fenêtre (`Dialog`, `ModalBottomSheet`), et la taille 130 % n'y passe pas. Captures côte à côte : `shots/amateur3/p130_feuillepartie_02_feuille_partie.png` (la feuille est plus petite que l'écran derrière), `shots/amateur3/tabP130_partage_01_pret_a_partager.png`, `tabL130_partage_01_pret_a_partager.png`. Cause : `UiScaled` (`app/…/ui/UiScale.kt:17-29`) remplace `LocalDensity` dans la composition principale, mais chaque `Dialog` / `ModalBottomSheet` crée sa fenêtre qui reprend la densité de l'appareil (13 fichiers à feuilles, 21 à fenêtres dans `app/…`). Les tests à 130 % existants ne l'ont pas vu parce qu'ils dessinent les feuilles dans l'écran, pas dans leur fenêtre.

### 7. Aide, Plus, réglages
Plus › Aide : sur la tablette couchée, la liste à gauche et la page à droite (`shots/amateur4/tabL_plus_02_aide.png`) ; debout, une seule colonne (`tabP_plus_01_plus.png`). « Les mots de la musique, à écouter » : chaque mot avec ▶. Elle aime.

Où elle s'arrête probablement : **tablette couchée sur son support, à 130 %, onglet Clavier** — pas de clavier à l'écran.

## Points durs

| # | Gravité | Appareil | Étapes pour reproduire | Capture | Correction en une ligne |
|---|---|---|---|---|---|
| 1 | **grave** | 130 % partout | Réglages › Taille 130 %, puis ouvrir l'appli, une feuille de partie, ⤴, l'export | `../cmp_launch.png`, `shots/amateur3/p130_feuillepartie_02_feuille_partie.png`, `shots/amateur3/tabP130_partage_01_pret_a_partager.png`, `shots/amateur3/tabP130_exportetapes_01_fabrication.png` | Appliquer `UiScaled` aussi **dans** chaque `Dialog` / `ModalBottomSheet` (un `ScaledDialog` / `ScaledSheet` commun qui reprend l'échelle des réglages), et un test à 130 % qui ouvre une vraie feuille. |
| 2 | **grave** | Tab L (100 % et 130 %) | Mode simple › Clavier › ● Enregistrer ma mélodie › ■ | `shots/amateur4/tabL_parcours_05_fin_de_prise.png`, `shots/amateur1/tabL130_clavier_01_clavier.png`, `shots/amateur1/tabL130_clavier_04_remis_en_rythme.png` | En paysage, le clavier garde une hauteur minimale (≈ 180 dp) et la carte de prise / la bande « boucle » passent **à côté** (colonne de droite) ou se replient ; supprimer la bande vide au-dessus du bouton. |
| 3 | moyen | tous | Clavier › ● Enregistrer ma mélodie | `shots/amateur1/tabP130_clavier_02_enregistre.png` | La prise va dans « Synthé » : en Mode simple, choisir d'office la piste « Mélodie ». |
| 4 | moyen | tous | même chemin | `shots/amateur1/tabP130_clavier_03_fin_de_prise.png` | « La boucle passe à 4 mesures… ↶ en haut… » : ne pas l'afficher en Mode simple, ou « Ta mélodie dure 4 mesures, elle se répète. » |
| 5 | moyen | tous | ■ après la prise | `shots/amateur1/phone_clavier_03_fin_de_prise.png` | Il manque « Écouter » sur la carte de la prise (Garder / Remettre en rythme / Refaire) : un ▶ « Écouter ma prise ». |
| 6 | mineur | tél. 130 % | n'importe quel écran avec la puce de partie | `shots/amateur1/p130_monmorceau_01_mon_morceau.png`, `shots/amateur3/p130_remix_02_structure.png` | Le nom du projet tombe à « Mon … » (3 lettres) : le passer sur sa propre ligne à 130 % (pareil qu'en vague 2). |
| 7 | mineur | Tab L | Mode simple | `shots/amateur1/tabL_monmorceau_01_mon_morceau.png`, `shots/amateur4/tabL130_simple_01_mon_morceau.png` | Dans le rail, « Mon morceau » touche les bords (80 dp) : deux lignes ou « Morceau ». |
| 8 | mineur | tous | assistant guidé | `shots/amateur1/tabP_premier_03_apres_guide.png` | « Étape 1 sur 13 » avant le choix du style : « Étape 1 » seul tant que le style n'est pas choisi (pareil qu'en vague 2). |
| 9 | mineur | tous | barre du haut | `shots/amateur4/tabP_parcours_02_structure.png` | ⤴, ●, ↶ et le dossier n'ont toujours pas de mot ; sur tablette il y a la place (pareil). |
| 10 | mineur | Tab P | « Que veux-tu faire ? », Mes projets vide, Aide | `shots/amateur1/tabP_ouverture_01_que_veux_tu_faire.png`, `shots/amateur1/tabP_zero_02_mes_projets_vide.png` | La moitié basse de l'écran reste vide sur la tablette debout : centrer verticalement et agrandir les cartes au-delà de 600 dp. |

## Ce qui manque

- Ses 130 % **partout**, y compris dans les fenêtres (point 1).
- Le clavier visible sur la tablette couchée pendant et après la prise (point 2).
- Sa mélodie dans la piste « Mélodie », sans message sur les boucles (points 3, 4).
- « Écouter ma prise » avant « Garder » (point 5).
- Des mots sous les icônes de la barre sur tablette (point 9).

## Comparé à la vague 2

Top 5 de la vague 2 :
1. **Sa mélodie gardée telle qu'elle la joue (gros bouton, décompte, Écouter / Remettre en rythme / Refaire / Garder)** → **mieux** : gros bouton nommé sur l'écran Clavier, carte de fin de prise, « Remettre en rythme » et « Comme joué ». Restent : la prise boucle encore sur 4 mesures (le message le dit), pas de bouton « Écouter » sur la carte, la piste visée (« Synthé »). Le décompte 4-3-2-1 n'a pas pu être vu ici (pas de moteur).
2. **Trouver « calme » dans l'assistant** → **réglé** pour la famille « Calme (Chill, Lo-fi) » en tête et le Lo-fi « Rêveur » ; **pareil** pour « Étape 1 sur 13 » et le lien « Créer tout de suite ».
3. **130 % qui respecte le texte** → **mieux dans les écrans** (onglets lisibles, « Intro · mes. 1/3 » sur deux lignes, 5 espaces au lieu de 7), **pire dans les fenêtres** : toutes les feuilles et boîtes restent à 100 % (point 1) ; le nom du projet tombe à 3 lettres (point 6).
4. **Les mots** → **mieux** : Mode simple avec « Mon morceau, Rythme, Clavier, Envoyer », « Mélodie » au lieu de « Lead », « Remettre en rythme » au lieu de « Quantifier » ; restent « Synthé », « Bend », « Modul. », « Basse acide · 303 », « +0,0 dB » et les icônes sans mot.
5. **Son chemin, pas celui du rappeur** → **réglé** : le Mode simple met « Mon morceau » d'abord, Live et Ligne du temps sont hors de sa route ; le fil « Prochaine étape » dit « Pose ta voix (si tu veux) ».

**Nouveau dans cette vague** : la taille 130 % qui ne passe pas dans les fenêtres (point 1, sans doute présent depuis longtemps mais visible maintenant que tout passe par des feuilles) et le clavier écrasé sur la tablette couchée (point 2).

## Top 5 maintenant

1. **Le 130 % dans toutes les fenêtres et feuilles** (point 1) : c'est le réglage qu'elle a choisi pour lire.
2. **Le clavier en entier sur la tablette couchée**, pendant et après la prise (point 2).
3. **La prise dans « Mélodie »**, sans message de boucle (points 3, 4).
4. **« Écouter ma prise »** sur la carte de fin de prise (point 5).
5. **Des mots sous ⤴ ● ↶** et des écrans d'accueil qui remplissent la tablette debout (points 9, 10).
