# Testeur Amateur 5 (vague 3) : performeur live et bricoleur curieux (effets, automation, Live, lexique)

Même profil qu'en vagues 1 et 2 (un set Jump up DnB joué en live, puis nettoyé et envoyé), élargi à ce que Benjamin demande pour cette vague : le curieux qui ouvre tout (effets d'une voie, automation, « ? », lexique, « Signaler un bug »).

Méthode : l'appli a **tourné** (Robolectric, `AppRoot` entier via `AppHarness`, branche `audit`, commit `0aa9bc7`) sur un morceau de trois parties ; captures à chaque étape sur téléphone 400 dp, 130 %, Galaxy Tab A11 debout (600 × 934 dp) et couchée (1006 × 528 dp), et en néerlandais. Sans moteur audio : rien ne joue vraiment, la grille Live s'allume sans son. `shots/` = `/tmp/claude-0/-home-user-pocket-studio/2ed29bda-5e9a-5850-a6a7-bbf3293c7a09/scratchpad/audit-35/shots/`, `app/…` = `app/src/main/java/com/pocketstudio/app/`.

Constat de base : en vague 2, rien n'avait bougé dans `live/`. Cette fois, mes deux « bloquants » sont levés.

## Parcours réel (touchers)

1. **Live = 1 toucher** depuis n'importe quel espace (barre du bas ou rail ; Mode simple : ⋯ › Live). `shots/amateur5/phone_live_01_live.png` : « ← Retour · ▶ · Lancer à 1 mesure · compteur · REC », **la bande de lecture « Intro · mes. 1/3 »** (absente en vague 2), « La prise : **Nouvelle version** | Ajouter à la suite », la grille parties × pistes (Batterie, Acide, Synthé, Basse, Lead) avec M et ■, puis le panneau Effets | 303 | Pads | Notes.
2. **Notes jouables en live** : onglet Notes (1) (`…/phone_live_04_panneau_notes.png`) : choix de la piste (Synthé, Basse, Lead), « Gamme | Clavier », Do4 − +, huit touches Do à Do, « Joue en direct. Avec ●, tes notes vont dans la prise. » Réglé.
3. **303** : onglet 303 (`…/phone_live_02_panneau_303.png`) : pad Coupure × Résonance, « Deux touchers : son enregistré », curseurs Enveloppe, Decay, Accent, Saturation.
4. **Pads live** : onglet Pads (`…/phone_live_03_panneau_pads.png`), « Plein écran » (`shots/amateur5/tabP_live_04_pads_plein_ecran.png`, `tabL_live_05_pads_plein_ecran.png`) : **REC sur la page plein écran**, et les pads joués pendant la prise deviennent une piste audio « Pads live » (`app/…/rhythm/RhythmViewModel.kt:3751-3759`). Réglé.
5. **Prise** : REC (1) › « Armé : la prise part à la prochaine ligne de lancement » (`…/phone_liverec_01_arme.png`), le choix par défaut est « Nouvelle version » (l'ancien morceau reste dans « Versions du morceau »).
6. **Effets d'une voie** : Mix › nom de la voie › feuille (`…/phone_effets_01_effets_voie.png`) : « Effets en place : 0 sur 8 », « + Ajouter un effet » (`…_02_ajouter_effet.png` : familles « Distorsion & couleur », « Filtres & mouvement »…), ambiances prêtes « Pour : Lead », styles Acide / Techno / DnB / Dubstep.
7. **Automation** : Mix › « Automatiser un réglage » (`…/phone_automation_01_automation.png`) : « Fais bouger un réglage tout seul : choisis-le, puis dessine sa courbe (au doigt, ou une ligne en deux touches) ». Et surtout, la feuille de partie fait « Montée avant », « Fondu », « Fill » sans courbe à dessiner.
8. **Lexique** : Plus › « Les mots de la musique, à écouter » (`…/phone_glossaire_01_glossaire.png`) : recherche, Kick, Caisse claire, Charley, Clap, 808… chacun avec ▶. Très bien.
9. **« ? »** sur Mix (`…/phone_etiquettes_01_etiquettes_mix.png`) : Console « Effets, courbes », « Glisse : volume »… **mais pas de « ? » sur la page Live** : le test a cherché le bouton et ne l'a pas trouvé (`…/phone_etiquettes_02_arret_help_labels_button.png`).
10. **Signaler un bug** : Plus › défiler › « Signaler un bug » (3 gestes ; raccourci appui long sur « ? », invisible) (`…/phone_bug_01_signaler_bug.png`, `tabP_bug_01_signaler_bug.png`) : « Qu'est-ce qui s'est passé ? », capture de l'écran jointe d'office, « Prendre une capture », « Joindre le projet » éteint, « Ce qui sera envoyé » en clair, « Rien ne part tout seul ». Honnête et clair.

## Points durs

| # | Gravité | Appareil | Étapes pour reproduire | Capture | Correction en une ligne |
|---|---|---|---|---|---|
| 1 | moyen | tous | Live, chercher « ? » | `shots/amateur5/phone_etiquettes_02_arret_help_labels_button.png`, `tabL_live_01_live.png` | La page la plus chargée de l'appli n'a pas de « ? » (sa barre remplace la bande de lecture qui le porte) : ajouter le « ? » dans la barre Live, avec des étiquettes pour REC, « Lancer à », M, ■, « La prise ». |
| 2 | moyen | Tab P | Live › Pads › Plein écran | `shots/amateur5/tabP_live_04_pads_plein_ecran.png` | En portrait, deux ronds rouges côte à côte : REC et « Tout couper » (sans mot) ; en paysage il est nommé (`tabL_live_05_pads_plein_ecran.png`) : garder le mot « Tout couper » partout et une icône ■ blanche. |
| 3 | moyen | tous | Live, choisir quand lancer | `shots/amateur5/phone_live_01_live.png` | « Lancer à 1 mesure » se change en touchant à l'aveugle (1 → 2 → 4 mesures → un temps → une demi-mesure, `app/…/live/LiveScreen.kt:505`) : des puces visibles « 1 temps · ½ mes. · 1 · 2 · 4 ». |
| 4 | mineur | tél. | Live | `shots/amateur5/phone_live_01_live.png` | « La prise : Nouvelle version | Ajoute… » : la 2e puce est coupée au bord (pareil qu'en vague 2) ; les mettre l'une sous l'autre. |
| 5 | mineur | tél., Tab L 130 % | Live › panneau Effets | `shots/amateur5/phone_live_01_live.png`, `tabL130_live_01_live.png` | Puces d'effets coupées (« Pi… », « Montée » à moitié), onglet « Notes » estompé ; la colonne « Lead » coupée à 130 % couchée. |
| 6 | mineur | tous | Live › 303 | `shots/amateur5/phone_live_02_panneau_303.png` | « Enveloppe », « Decay », « Accent » : « Decay » en anglais au milieu du français ; « Durée de la note ». |
| 7 | mineur | tous | Plus › Signaler un bug | `shots/amateur5/tabP_bug_01_signaler_bug.png` | « Les 136 derniers écrans et gestes, et 1 erreurs » : pluriel faux (`app/…/report/BugReportSheet.kt:368`). |
| 8 | mineur | tous | Plus | `shots/amateur4/phone_plus_01_plus.png` | « Signaler un bug » est en 11e ligne, sous Latence et Son de test : le remonter sous « Aide » ; dire le raccourci « appui long sur ? » dans l'Aide. |
| 9 | mineur | tous | Mix › « ? » | `shots/amateur5/phone_etiquettes_01_etiquettes_mix.png`, `tabP_etiquettes_01_etiquettes_mix.png` | Étiquettes qui se recouvrent (« Mes projets » sur le tempo, « Écouter » coupée en haut) et boutons flottants posés sur les faders. |
| 10 | mineur | NL | Live › Pads | `shots/amateur5/nl_live_01_live.png` | L'onglet « Pads » du Live devient « Akkoordpads » (pads d'accords) en néerlandais : traduire « Pads » du Live par « Pads » (ou « Live-pads »). |

## Ce qui manque
- Le « ? » et ses étiquettes sur la page Live (point 1).
- Une quantification visible (point 3).
- Une gomme de courbe sur la ligne du temps (non revu en détail cette fois).

## Comparé à la vague 2 (Top 5 d'alors)
1. Pads live non enregistrés, pas de ● en plein écran → **réglé** : REC sur la page plein écran, et les pads de la prise deviennent la piste audio « Pads live ».
2. Aucune note jouable en live → **réglé** : panneau Notes (Gamme ou Clavier), notes écrites dans la prise.
3. « Remplacer le morceau » par défaut, Annuler fragile → **réglé** : « Nouvelle version » par défaut et « Versions du morceau » pour ramener l'ancien.
4. Deux ▶ aux sens différents, pas de position sur Live → **mieux** : la bande « Intro · mes. 1/3 » est sur Live ; non vérifié ici ce que fait le ▶ du plein écran (pas de moteur).
5. Nettoyage lourd et quantification en trois endroits → **mieux** : la feuille de partie (Variation, Break, Fin) aide à nettoyer ; « Lancer à » gagne « un temps » et « une demi-mesure », mais se change toujours à l'aveugle (point 3).

Autres points de la vague 2 : « Tout couper » sans mot → **mieux** (nommé en paysage, pas en portrait, point 2) ; M de Live collé après ← → non revu (il faut le moteur) ; puces qui défilent → **pareil** (point 4).

**Nouveau dans cette vague** : pas de « ? » sur Live (point 1), pluriel faux dans le rapport de bug (point 7), traduction néerlandaise des pads live (point 10).

## Top 5 (maintenant)
1. **« ? » sur Live** avec des étiquettes pour chaque commande (point 1).
2. **Quantification visible** en puces (point 3).
3. **« Tout couper » nommé partout**, jamais confondu avec REC (point 2).
4. **Signaler un bug plus haut** dans Plus, raccourci dit dans l'Aide, pluriel corrigé (points 7, 8).
5. **Étiquettes « ? » sans chevauchement** sur Mix et ailleurs (point 9).
