# Audit 35 · Les gestes (15 actions clés + 9 de plus), téléphone et tablette

Version auditée : branche `audit`, commit `0aa9bc7` (tête de la PR #47). Comptage **réel** dans l'appli entière (AppRoot, Robolectric), avec le même harnais que `nav/KeyActionsGestureTest` (`AppHarness`, morceau de trois parties Intro / Refrain / Drop, pistes Batterie, Basse acide, Synthé, Basse, Lead), ouverte sur Structure. Un geste = une touche, un appui long, un glisser ou un défilement nécessaire (compté quand la cible est hors de l'écran).

Appareils :
- **Tél. 360** : 360 × 780 dp (le petit téléphone du test de la phase 36).
- **Tél. 130 %** : 411 × 914 dp, Réglages › Taille 130 %.
- **Tab P** : Galaxy Tab A11 debout, fenêtre réelle 600 × 934 dp (1006 dp moins la barre d'état 24 et la barre de navigation 48).
- **Tab L** : la même couchée, fenêtre réelle 1006 × 528 dp.

Journal brut : `/tmp/claude-0/-home-user-pocket-studio/2ed29bda-5e9a-5850-a6a7-bbf3293c7a09/scratchpad/audit-35/shots/log/gestes_<appareil>.txt` (chemin exact de chaque action).

## Les 15 actions clés (budget de docs/NAVIGATION.md)

| # | Action | Budget | Tél. 360 | Tél. 130 % | Tab P | Tab L | Chemin |
|---|---|---|---|---|---|---|---|
| 1 | Écouter tout le morceau | 1 | 1 | 1 | 1 | 1 | ▶ |
| 2 | Jouer depuis une partie | 1 | 1 | 1 | 1 | 1 | toucher la règle sur la partie |
| 3 | Changer le son de la basse | 2 | 2 | 2 | 2 | 2 | nom « Basse » › un son |
| 4 | Changer le kit | 2 | 2 | 2 | 2 | 2 | nom « Batterie » › un kit |
| 5 | Ouvrir la batterie du refrain | 1 | 1 | 1 | 1 | 1 | deux touches sur le bloc |
| 6 | Couper une piste dans une partie | 1 | 1 | 1 | 1 | 1 | toucher le bloc |
| 7 | Déplacer une partie | 1 | 1 | 1 | 1 | 1 | appui long + glisser le drapeau |
| 8 | Volume d'une piste | 2 | 2 | 2 | 2 | 2 | Mix › fader |
| 9 | Enregistrer une mélodie | 2 | 2 | 2 | 2 | 2 | deux touches sur le bloc › ● Enregistrer ma mélodie |
| 10 | Enregistrer la voix | 3 | 3 | 3 | 3 | 3 | Sons › Voix › ● |
| 11 | Montée de filtre avant le drop | 2 | 2 | 2 | 2 | 2 | appui long sur le drapeau › Montée |
| 12 | Fill en fin de partie | 2 | 2 | 2 | 2 | 2 | appui long sur le drapeau › Fill |
| 13 | Partager | 2 | 2 | 2 | 2 | 2 | ⤴ › Partager |
| 14 | Exporter en WAV | 3 | 3 | 3 | 3 | 3 | ⤴ › WAV › Partager |
| 15 | Ouvrir le Live | 1 | 1 | 1 | 1 | 1 | Live |
| | **Total** | **26** | **26** | **26** | **26** | **26** | |

**Verdict : les 15 tiennent leur budget sur les quatre appareils**, sans un seul défilement en plus, même à 130 % et sur la tablette couchée (528 dp de haut). C'est la grande réussite de la phase 36 et elle n'a pas régressé.

Deux réserves honnêtes :
- Les 15 sont comptées **depuis Structure déjà ouvert**. Depuis que « Que veux-tu faire ? » s'affiche à chaque ouverture (retour de Benjamin du 2026-10-24), la première action d'une séance coûte **un geste de plus** (« Continuer »). C'est voulu, mais le budget de NAVIGATION.md ne le dit pas.
- Les gestes 1, 2, 5, 6, 7, 11, 12 se font sur la ligne du temps : ils sont rapides **quand on les connaît**. Rien à l'écran ne dit « appui long sur un drapeau » ni « deux touches sur un bloc » en dehors du « ? » et des missions (voir amateur1, amateur3).

## 9 actions de plus (demandées pour cet audit)

| Action | Budget visé | Tél. 360 | Tél. 130 % | Tab P | Tab L | Chemin | Verdict |
|---|---|---|---|---|---|---|---|
| **Remix depuis l'ouverture** | 2 (Benjamin) | **4** | **4** | **4** | **4** | Continuer › Sons › Voix › onglet Remix | **hors budget** |
| **Remix depuis Structure** | 2 | **3** | **3** | **3** | **3** | Sons › Voix › onglet Remix | **hors budget** |
| Remix en Mode simple | 2 | **impossible** | idem | idem | idem | aucun chemin (ni onglet, ni « ⋯ ») | **absent** |
| Voix depuis l'ouverture | 4 | 4 | 4 | 4 | 4 | Continuer › Sons › Voix › ● | ok |
| Nouveau morceau (un style) depuis l'ouverture | 3 | 3 | 3 | 3 | 3 | Nouveau morceau › famille › Créer le morceau | ok |
| Aide (le guide) | 2 | 2 | 2 | 2 | 2 | Plus › Aide | ok |
| Les mots de la musique | 2 | 2 | 2 | 2 | 2 | Plus › Les mots de la musique | ok |
| Signaler un bug | 2 | 3 | 3 | 3 | 3 | Plus › défilement › Signaler un bug (raccourci : appui long sur « ? » = 1 ; Mode simple : ⋯ › Signaler un bug = 2) | un défilement de trop, raccourci caché |
| Mode simple : partager | 2 | 2 | 2 | 2 | 2 | Envoyer › Partager | ok |
| Mode simple : enregistrer une mélodie | 2 | 2 | 2 | 2 | 2 | Clavier › ● Enregistrer ma mélodie | ok |
| Mode simple : changer le kit | 2 | 3 | 3 | 3 | 3 | Rythme › Kit ▾ › un kit | un de plus qu'en mode complet (pas de nom de piste à toucher) |

Note : « Signaler un bug » est rangé en 11e position de la liste Plus, sous Latence, Lecture en arrière-plan et Son de test ; il faut défiler sur tous les appareils, tablette comprise (la liste de Plus ne passe pas en deux colonnes à 600 dp de large).

## Remix : trouvable en deux gestes ?

**Non.** Le Remix (séparer une chanson en batterie, basse, voix, reste) est le 2e onglet de la page « Voix », elle-même 3e puce de l'espace Sons, sous l'étiquette « Voix · Micro ». Aucun mot « Remix » n'est visible avant le 3e geste :
- « Que veux-tu faire ? » : Continuer, Nouveau morceau, Mes projets, Missions, Mode. Pas de Remix (`app/…/onboarding/LaunchChoice.kt:319-323`).
- Mode simple : ni les 4 onglets, ni le menu « ⋯ » (`ui/AppRoot.kt:1212-1218`) n'y mènent. NAVIGATION.md l'écrit d'ailleurs : « — (Mode complet) ».
- Missions : 6 étapes, aucune sur le Remix (`learn/Missions.kt`).
- Le « ? » de Sons étiquette la puce « Voix » « Tes pistes », pas « Remix ».
- Plus › Aide : rien.

Captures : `shots/amateur3/phone_remix_01_ouverture.png` → `…_05_remix.png`, idem `tabP_remix_*`, `tabL_remix_*` ; `shots/amateur3/phone_remixsimple_01_menu_sans_remix.png`.

Correction en une ligne : une carte « Remixer une chanson » dans « Que veux-tu faire ? » (et dans le menu « ⋯ » du Mode simple), plus une puce « Remix » à part entière dans Sons (à côté de « Voix ») : 1 geste depuis l'ouverture, 2 depuis n'importe quel espace.
