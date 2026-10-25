# Testeur Amateur 1 (débutant total, Mode simple) : vague 3, avant la fusion de la PR #47

Même profil, même mission que les vagues 1 (2026-10-23) et 2 (après la phase 33) : faire un morceau d'environ 3 minutes, le rendre « à moi » (changer des sons et la mélodie, un drop qui tape plus fort, une vraie fin), puis l'envoyer à un ami. Cette fois je reste en **Mode simple**, puisque l'appli me le propose dès le premier lancement.

Méthode, nouvelle pour cette vague : **l'appli a vraiment tourné** (Robolectric, `AppRoot` entier via `AppHarness`, branche `audit`, commit `0aa9bc7`), touche après touche, avec une capture à chaque étape, sur un téléphone de 400 dp, à 130 %, sur la Galaxy Tab A11 debout et couchée, et en néerlandais. Limite : pas de moteur audio dans ce banc (rien ne sonne, l'export s'arrête sur « Le moteur audio n'est pas prêt », la création d'un style s'arrête sur « son illisible : Shaker ») ; ce qui dépend du son est vérifié dans le code ou laissé à Benjamin.

Chemins abrégés : `shots/` = `/tmp/claude-0/-home-user-pocket-studio/2ed29bda-5e9a-5850-a6a7-bbf3293c7a09/scratchpad/audit-35/shots/` ; `…/` = `app/src/main/java/com/pocketstudio/app/`. Un geste = une touche, un appui long, un glisser ou un défilement.

Résumé en une phrase : **le Mode simple tient sa promesse (4 onglets, un gros « Enregistrer ma mélodie », « Remettre en rythme », une feuille de partie avec Fill, Break, Montée, Fondu et « Ajouter une fin », « Envoyer » en un onglet), et mes trois blocages des vagues 1 et 2 sont levés ; ce qui reste, ce sont des mots techniques qui traînent, des étiquettes « ? » qui se marchent dessus, et un Remix que je ne trouverai jamais.**

---

## Parcours réel

### 1. Premier lancement (1 geste)
`shots/amateur1/phone_premier_01_bienvenue.png` : langue, « Comment veux-tu travailler ? » (Simple coché, Complet), puis trois grosses cartes. Clair, et le Mode simple est choisi d'office. « Créer mon premier morceau (guidé) » (1) ouvre l'assistant sur **« Étape 1 sur 13 »** (`phone_premier_03_apres_guide.png`) avec, enfin, la famille **« Calme (Chill, Lo-fi) »** en tête.

### 2. Les fois suivantes : « Que veux-tu faire ? » (1 geste)
`shots/amateur1/phone_ouverture_01_que_veux_tu_faire.png` : « Continuer · Mon premier rythme · 0:06 · 3 parties · modifié à l'instant » en grand, « Nouveau morceau », « Mes projets », « Missions », et le choix du mode. C'est exactement ce que Benjamin demandait. Rien ne joue tant que je n'ai pas choisi. Le bas de l'écran est vide sur un téléphone (la moitié), mais ça ne gêne pas.

### 3. Nouveau morceau d'un style (3 gestes)
Nouveau morceau (1) › « Calme (Chill, Lo-fi) » (2) › « Créer le morceau » sur Lo-fi hip-hop (3) (`phone_ouverture_02_nouveau_projet.png`, `…_03_famille_calme.png`). Les familles arrivent toutes repliées (retour de Benjamin traité). Dans ce banc, la création s'arrête sur « Le morceau n'a pas pu être préparé (son illisible : Shaker) » (`…_05_morceau_pret.png`) : c'est l'absence de moteur, pas l'appli ; mais le message lui-même montre un nom de son brut à un débutant.

### 4. L'assistant pas à pas (13 étapes en Techno)
`shots/amateur1/phone_assistant_02_etape_01.png` à `…_13_etape_12.png` (et `../sheet_assist_*.png`). Ce qui a changé depuis la vague 2 : le **Lead est allumé par défaut** en Techno (« Créer cette piste », étape 12), le résumé dit **« QUATRE PARTIES »** (plus « sections ») et annonce « environ 5 min 20 ». Ce qui n'a pas bougé : « Étape 2 sur 13 » avec « Gamme Fa phrygienne · accords i – i – i – II » sous chaque ambiance, « Créer tout de suite (choix conseillés) » toujours en lien orange fin au-dessus de « Retour / Suivant », et les sons de basse décrits « Scie · 5 voix · tenu », « Basse reese », « Basse growl ».

### 5. Mon morceau (Mode simple)
`shots/amateur1/phone_monmorceau_01_mon_morceau.png` : « MES PISTES » d'abord (Batterie, Basse acide, Synthé, Basse, Mélodie, chacune avec « Changer le son » et 🔊), puis « LE MORCEAU » (accords, parties). Le volume d'une piste : 🔊 › feuille « Volume +0,0 dB · Couper · Écouter seule · Changer le son » (2 gestes, `…_03_volume_piste.png`). Toucher une partie la fait jouer (`…_02_partie_touchee.png`) ; l'appui long ouvre la feuille de partie (vue en Mode complet, `shots/amateur3/phone_feuillepartie_02_feuille_partie.png`) : **Variation, Fill à la fin, Break après 4/8 mes., Montée avant 4/8 mes., Fondu d'entrée, Fondu de sortie, Ajouter une fin, Énergie 1 à 4**. C'est tout ce que je réclamais depuis la vague 1.

### 6. Enregistrer ma mélodie (Clavier, 2 gestes) et la remettre en rythme (+2)
`shots/amateur1/phone_clavier_01_clavier.png` → `…_04_remis_en_rythme.png`. Onglet Clavier (1) : un **gros bouton rouge « ● Enregistrer ma mélodie »** au-dessus du clavier (2). Pendant la prise, une bande rouge « Enregistrement · ■ Arrêter ». Après ■ : « Ta prise : 4 notes · **Garder · Remettre en rythme · Refaire** », puis « Comme joué · 50 % · 75 % · 100 % ». Réglé. Mais : la piste choisie est « **Synthé · Synthé** » (la première), pas « Mélodie » ; une bande grise dit « La boucle passe à 4 mesures pour ta prise, et cette partie aussi dans le morceau. ↶ en haut pour revenir à 1 mesure. » ; sous le clavier, « Bend » et « Modul. ».

### 7. Envoyer (2 gestes)
`shots/amateur1/phone_envoyer_01_envoyer.png` : « Fini ? Envoie ton morceau », nom, « 0:06 · 3 parties », « Écouter », « Tout est prêt : aucun solo oublié, aucune piste coupée », M4A / WAV, gros « Partager », « Vidéo pour les réseaux · 30 s », et… « Autres exports · Piste par piste, MIDI, FLAC… **dans Mix** » : le Mode simple n'a pas d'espace Mix.

### 8. Le menu « ⋯ », l'aide, les missions, les « ? »
- `phone_menu_01_menu_points.png` : Live, Mes projets, Réglages, Aide et missions, Passer en mode complet, Signaler un bug. Pas de Remix.
- `phone_menu_02_aide.png` : le fil d'Ariane dit « ← **Plus** · Aide » alors que le Mode simple n'a pas de « Plus ».
- `phone_missions_01_mission_1.png` : « Mission 2/6 · Écoute ton morceau », « Passer cette étape », « Arrêter les missions ». Clair.
- `phone_etiquettes_01_etiquettes.png` : le « ? » pose une quinzaine d'étiquettes, mais elles **se chevauchent** (« Mes projets » posée sur « 120 BPM », « Jouer d'une partie » sur « Tempo », « Écouter » coupée par le bord du haut) et les deux boutons flottants « Masquer les étiquettes » / « Signaler un bug » cachent la liste des pistes.

### 9. Zéro projet (lot 1)
`shots/amateur1/phone_zero_01_zero_projet_ouverture.png` : « Aucun projet pour l'instant : commence un nouveau morceau », sans « Continuer » : bien. `…_02_mes_projets_vide.png` : **trois** boutons pour créer (« Nouveau morceau guidé », « Nouveau projet : style, démo ou vide », « Nouveau morceau ») et **deux** pour importer (« Importer un projet (.zip) », « Importer un projet ») sur le même écran.

---

## Points durs

| # | Gravité | Appareil | Étapes | Capture | Correction en une ligne |
|---|---|---|---|---|---|
| 1 | moyen | tous | Mode simple › Clavier › ● Enregistrer ma mélodie | `shots/amateur1/phone_clavier_02_enregistre.png` | La prise part sur la première piste (« Synthé ») : choisir d'office la piste « Mélodie » (ou demander « Sur quelle piste ? ») en Mode simple. |
| 2 | moyen | tous | même chemin, puis ■ | `…/phone_clavier_03_fin_de_prise.png` | Le message « La boucle passe à 4 mesures… ↶ en haut pour revenir à 1 mesure » reste du jargon : en Mode simple, ne rien dire (ou « Ta mélodie dure 4 mesures »). |
| 3 | moyen | tous | ouvrir le Mode simple, toucher « ? » | `…/phone_etiquettes_01_etiquettes.png`, `tabP_etiquettes_01_etiquettes.png`, `tabL_etiquettes_01_etiquettes.png` | Étiquettes qui se chevauchent et boutons flottants sur la liste : poser les étiquettes sous la cible en évitant les collisions, boutons « Masquer / Signaler » en bas de l'écran. |
| 4 | moyen | tous | Nouveau projet ou assistant | `…/phone_assistant_03_etape_02.png` | « Gamme Fa phrygienne · accords i – i – i – II » sous chaque ambiance : cacher derrière un « ? » (demandé en vague 2, pareil). |
| 5 | moyen | tous | assistant, toutes les étapes | `…/phone_assistant_03_etape_02.png` | « Créer tout de suite (choix conseillés) » reste un lien : en faire un vrai bouton dès l'étape 2 (pareil qu'en vague 2). |
| 6 | mineur | tous | Mode simple › Envoyer | `…/phone_envoyer_01_envoyer.png` | « Autres exports … dans Mix » n'existe pas en Mode simple : ouvrir la page des exports directement, ou dire « en Mode complet ». |
| 7 | mineur | tous | Mode simple › ⋯ › Aide et missions | `…/phone_menu_02_aide.png` | Fil d'Ariane « Plus · Aide » en Mode simple : écrire « ← Retour » ou « Aide ». |
| 8 | mineur | tous | Mode simple › Clavier | `…/phone_clavier_01_clavier.png` | Mots restés techniques en « mots de tous les jours » : « Synthé », « Basse acide · 303 », « Bend », « Modul. », « Sustain », « Gamme », « +0,0 dB » ; compléter la table `EVERYDAY` (`project-model/…/i18n/I18n.kt:57`). |
| 9 | mineur | tous | ouvrir « Mes projets » sans projet | `…/phone_zero_02_mes_projets_vide.png` | Trois boutons « nouveau » et deux « importer » : garder la carte du milieu seule. |
| 10 | mineur | tous | Nouveau projet › Partir d'un style | `…/phone_ouverture_02_nouveau_projet.png` | « Une touche, un morceau complet (environ 3 minutes) » alors qu'un Techno dure 5 à 6 min : « de 3 à 6 minutes selon le style ». |
| 11 | mineur | tous | Nouveau projet › un style qui échoue | `…/phone_ouverture_05_morceau_pret.png` | « (son illisible : Shaker) » : dire « Un son n'a pas pu être lu ; réessaie » sans le nom technique. |
| 12 | mineur | tél. | n'importe quel écran | `…/phone_monmorceau_01_mon_morceau.png` | Le nom du projet tombe à « Mon prem… » (7 lettres) dès qu'une puce de partie est là : le mettre sur sa propre ligne à 400 dp. |

---

## Ce qui manque pour finir un morceau complet

- Plus rien de bloquant : fin, drop, variation, fondus, énergie, enregistrement, partage sont là.
- Une mélodie qui va dans la piste « Mélodie » sans que je la cherche.
- Un assistant plus court en Mode simple (13 étapes en Techno ; 10 en Lo-fi) ou le raccourci en vrai bouton.
- Le Remix, que Benjamin adore, n'existe pas en Mode simple (voir `gestes.md`).

---

## Comparé à la vague 2

Top 5 de la vague 2, un verdict par ligne :

1. **Stabiliser l'édition pendant la lecture** → **réglé** : toucher une case ou une note pendant que le morceau joue allume « ⟳ Partie » tout seul sur la partie affichée (`…/rhythm/RhythmViewModel.kt:1929-1943`, `loopOnEdit`), et chaque éditeur dit « Partie : A · Intro » en orange au-dessus.
2. **« Ajouter une fin » et un « + » visible** → **réglé** pour la fin (feuille de partie : « Ajouter une fin », « Fondu de sortie ») ; **mieux** pour le « + » (« Ajouter à la fin : + A · Intro + B · Refrain… » reste en lettres, mais la feuille a « Variation » et « Dupliquer »).
3. **« Plus fort / Énergie » par partie** → **réglé** : « Énergie : 1 · nappes / 2 · + basse / 3 · + batterie / 4 · à fond ».
4. **Un seul mot et un onglet « Structure »** → **réglé** : l'espace s'appelle Structure (« Mon morceau » en Mode simple), « Partie » partout dans ce que j'ai vu (l'assistant dit « QUATRE PARTIES »). Seule exception repérée : la grille des briques s'appelle aussi « Parties » (voir amateur3, point 3).
5. **Changer un son sans chercher, Lead allumé, fil « Prochaine étape »** → **réglé** pour le son (« Changer le son » sur chaque piste, 2 gestes) et le Lead ; **mieux** pour le fil (10 étapes, « Pose ta voix (si tu veux) » ; il dit « 5/10 · Ajoute une mélodie » sur un morceau qui a déjà une piste Mélodie, mais elle est vide dans le banc).

Autres points de la vague 2 : carte « Ton morceau est prêt » perdue d'un toucher → **mieux** (repliée en bande, retour de Benjamin du 24/10) ; deux ▶ sur Rythme → **réglé** (plus de carte de transport) ; jargon (« Acide », « 303 », « Quantifier », « LUFS ») → **mieux** (Quantifier devient « Remettre en rythme », LUFS rangé sous « détails »), « Basse acide · 303 » reste.

**Nouveau dans cette vague (à corriger)** : étiquettes « ? » qui se chevauchent (point 3), « Autres exports … dans Mix » en Mode simple (6), fil d'Ariane « Plus » en Mode simple (7), doublons de l'écran zéro projet (9), texte « environ 3 minutes » devenu faux (10).

---

## Top 5 maintenant

1. **La mélodie dans la bonne piste** (point 1) : en Mode simple, « ● Enregistrer ma mélodie » vise la piste « Mélodie », et le message de boucle à 4 mesures disparaît (point 2).
2. **Des « ? » lisibles** (point 3) : étiquettes sans chevauchement, boutons flottants rangés en bas.
3. **Un assistant qui ne fait pas peur** (points 4, 5) : chiffres romains derrière un « ? », « Créer avec les choix conseillés » en vrai bouton.
4. **Le Remix en Mode simple** : une ligne « Remixer une chanson » dans le menu « ⋯ » et dans « Que veux-tu faire ? » (voir `gestes.md`).
5. **Finir la traduction en mots de tous les jours** (points 6, 7, 8) : Synthé, 303, Bend, Modul., dB, « dans Mix », « Plus ».
