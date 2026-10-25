# Amateur 3, vague 3 : House de « Partir d'un style », une soirée de retouches, puis un Remix et l'envoi

Même personne, même soirée : un morceau House fait par l'appli que je rends « à moi » (sons de basse et de batterie, accords, un break, ordre des parties, intro en fondu, montée de filtre avant le drop, mix, export WAV). Nouveau pour cette vague, à la demande de Benjamin : **remixer une chanson que j'aime** (le séparateur de pistes) et l'envoyer.

Méthode : l'appli a **tourné** (Robolectric, `AppRoot` entier via `AppHarness`, branche `audit`, commit `0aa9bc7`) sur un morceau de trois parties (Intro, Refrain, Drop) ; captures à chaque étape sur téléphone 400 dp, 130 %, Galaxy Tab A11 debout (600 × 934 dp) et couchée (1006 × 528 dp), et en néerlandais. Sans moteur audio : l'export s'arrête sur « Le moteur audio n'est pas prêt », la page Remix affiche son avertissement de mémoire (Robolectric déclare 0 Go) ; ces deux écrans sont jugés sur leur texte et leur code. `shots/` = `/tmp/claude-0/-home-user-pocket-studio/2ed29bda-5e9a-5850-a6a7-bbf3293c7a09/scratchpad/audit-35/shots/`, `app/…` = `app/src/main/java/com/pocketstudio/app/`.

Point de repère : en vague 2, les écrans Automation, Ligne du temps, Piano roll (accords), Instruments et Lexique n'avaient pas bougé. Cette fois, presque tout ce que je réclamais est arrivé.

## Parcours réel (gestes comptés : une touche, un défilement, un appui long, un glisser = 1)

1. **Ouvrir.** « Que veux-tu faire ? » › Continuer (1) (`shots/amateur3/phone_remix_01_ouverture.png`). J'arrive sur la ligne du temps de Structure (`…_02_structure.png`) : une rangée par piste, nom et M / S fixes à gauche, blocs « A · Intro / B · Refrain / C · Drop ».
2. **Changer le son de la basse.** Toucher le nom « Basse » dans la ligne du temps › un son : **2 gestes** (mesuré, `gestes.md`). Vague 2 : 5.
3. **Changer le kit.** Toucher « Batterie » › un kit : **2 gestes**. Vague 2 : 8 à 80.
4. **Volume du kick.** Sons › Batterie › appui long sur Kick › « Volume du pad » (`shots/amateur2/phone_batterie_03_feuille_pad.png`) : 3 gestes, **même avec le kit synthétisé**. Vague 2 : impossible.
5. **Accords.** Structure › Parties › « Accords du morceau » (`shots/amateur3/phone_parties_02_accords.png`) : « Tonalité : Do majeure. Les pistes qui suivent changent avec les accords, dans toutes les parties. » ; progressions avec **les noms** (« I–V–vi–IV · Do Sol La m Fa ») ; « Touche une progression : tu l'entends tout de suite, sur tout le morceau, avant de l'appliquer » ; « Appliquer à tout le morceau ». **3 gestes** pour tout le morceau. Vague 2 : ≈ 30.
6. **Break et variation.** Appui long sur le drapeau du Drop (1) › feuille « Partie 3 · C · Drop » (`shots/amateur3/phone_feuillepartie_02_feuille_partie.png`) : **Variation**, Fill à la fin, **Break après 4 / 8 mes.**, Montée avant 4 / 8 mes., **Fondu d'entrée**, Fondu de sortie, Ajouter une fin, Énergie 1 à 4, Longueur, Ce qui joue, Monter / Descendre, Dupliquer, Retirer. Chaque intention = **2 gestes**.
7. **Intro en fondu, montée avant le drop.** Même feuille : « Fondu d'entrée » sur l'Intro, « Montée avant · 4 mes. » sur le Drop : 2 gestes chacun (mesuré : 2). Vague 2 : ≈ 50 et 8.
8. **Réordonner.** Appui long puis glisser le drapeau : 1 geste (mesuré).
9. **Mix.** Mix › console (`shots/amateur3/phone_mix_01_console.png`) : faders côte à côte, M / S, « Glisse un fader pour le volume ; touche le nom d'une piste pour ses effets et ses courbes ». Feuille d'une voie (`…_02_feuille_voie.png`) : Muet, Solo, Volume, Panoramique « ? », Ce qui joue, Courbes, Effets, ambiances prêtes.
10. **Remix d'une chanson.** Depuis Structure : Sons (1) › puce « Voix · Micro » (2) › onglet « Remix » (3) (`shots/amateur3/phone_remix_03_sons.png` → `…_05_remix.png`) : « SÉPARER UNE CHANSON · Choisis un morceau de l'appareil : l'appli le découpe en batterie, basse, voix et le reste. » **3 gestes depuis Structure, 4 depuis l'ouverture, impossible en Mode simple** (`…/phone_remixsimple_01_menu_sans_remix.png`). Rien, avant le 3e geste, ne contient le mot « Remix ».
11. **Export WAV.** ⤴ › WAV › Partager : 3 gestes (`shots/amateur3/phone_partage_01_pret_a_partager.png`, `…_02_wav.png`). La boîte d'export nomme maintenant ses étapes (`shots/amateur3/tabL_exportetapes_01_fabrication.png` → `…_04_nouvel_essai.png`) : « Fabrication du son, puis compression en M4A » › « Compression M4A » › « Vérification » › « Nouvel essai 2/3 : compression », avec une barre et « Annuler ».

Total sur mes retouches : environ **35 gestes**, contre ≈ 150 en vague 2. C'est le plus gros progrès des trois vagues.

## Points durs

| # | Gravité | Appareil | Étapes pour reproduire | Capture | Correction en une ligne |
|---|---|---|---|---|---|
| 1 | **grave** | tous | Ouvrir l'appli › chercher le Remix | `shots/amateur3/phone_remix_01_ouverture.png` → `…_05_remix.png`, `tabP_remix_*`, `tabL_remix_*`, `phone_remixsimple_01_menu_sans_remix.png` | Le Remix, que Benjamin veut « mettre en avant », est à 3 gestes de Structure, 4 de l'ouverture, absent du Mode simple, des Missions et de « Que veux-tu faire ? » : une carte « Remixer une chanson » dans « Que veux-tu faire ? » et le menu « ⋯ », et une puce « Remix » à part dans Sons. |
| 2 | moyen | tous | Structure › Parties (en bas sur téléphone, à droite sur la tablette couchée) | `shots/amateur3/tabL_parties_01_liste_parties.png` | Deux choses s'appellent « Parties » sur le même écran : la liste du morceau (« A · Intro × 1 · 1 mes. ») et la grille des briques A à H (« Une partie = la batterie, l'acide et les notes… vide ») : nommer la grille « Briques » (ou « Mes boucles »). |
| 3 | moyen | tous | ⤴ › Partager, export long | `shots/amateur3/tabL_exportetapes_04_nouvel_essai.png` | « Nouvel essai 2/3 : compression » se lit comme un échec : « Réglage du volume final (2/3) », et une durée estimée à côté de la barre. |
| 4 | mineur | tous | Mix | `shots/amateur3/phone_mix_01_console.png`, `phone_mix_03_courbes.png` | Deux cartes « Automation et effets » puis « AUTOMATION » répètent « Automatiser » et « Ce qui joue » sous la console : n'en garder qu'une. |
| 5 | mineur | Tab L (et tout appareil quand le moteur est fermé) | Mix, défiler jusqu'à « Sortie générale » | `shots/amateur3/tabL_mix_01_console.png` | « Le son est retenu de 214748368,0 dB » : `LimiterLine` (`app/…/mixer/MixerScreen.kt:269-285`) ne protège pas un gain de 0 (log10(0)) ; afficher « — » quand le gain lu vaut 0. |
| 6 | mineur | tous | ⤴, choisir WAV (banc sans moteur) | `shots/amateur3/phone_partage_02_wav.png` | « Le son risque de saturer : il est trop fort pour le fichier » apparaît sans mesure (même cause que le point 5 ?) : à vérifier sur l'appareil ; ne rien affirmer sans mesure. |
| 7 | mineur | tous | Accords du morceau, bas de la feuille | `shots/amateur3/phone_parties_02_accords.png` | « A · Intro · lus dans les notes » : dire « trouvés dans tes notes ». |
| 8 | mineur | tous | Structure › Parties | `shots/amateur3/phone_parties_01_liste_parties.png` | « Ajouter à la fin : + A · Intro + B · Refrain… » garde les lettres : « + Intro », « + Refrain ». |
| 9 | mineur | doc | feuille d'une voie | `shots/amateur3/phone_mix_02_feuille_voie.png` | « Effets en place : 0 sur 8 » alors que DESIGN.md dit « quatre effets par voie au maximum » : mettre DESIGN.md à jour. |
| 10 | mineur | Tab P | Mix | `shots/amateur3/tabP_mix_01_console.png` | Console sur 2/3 de la largeur, faders de 130 dp sur une tablette : faders plus hauts et répartis sur toute la largeur (voir `tablette.md`). |

## Ce qui manque

- Le Remix en évidence (point 1). C'est le seul point de ma liste qui touche directement une demande de Benjamin.
- Un seul nom pour la grille des briques (point 2).
- Une batterie séparable pad par pad dans la ligne du temps (toujours une seule rangée « Batterie », `shots/amateur3/phone_remix_02_structure.png`).

## Comparé à la vague 2

Top 5 de la vague 2 :

1. **Variation / Break d'une partie** → **réglé** : « Variation », « Break après 4 / 8 mes. » dans la feuille de partie. La batterie reste une seule rangée dans la ligne du temps (**pareil** pour cette moitié).
2. **Accords du morceau** → **réglé** : noms d'accords, écoute avant, « Appliquer à tout le morceau », pistes qui suivent.
3. **Finir le vocabulaire** → **mieux** : « Partie » partout, sauf la grille des briques (point 2) ; ▶ dit ce qu'il joue (bande « Intro · mes. 1/3 », « ⟳ Partie »).
4. **Les sons en deux gestes** → **réglé** : nom de piste › son (2), Kit ▾ (2), volume par pad même synthétisé.
5. **Fondus et montée en un geste** → **réglé** : Fondu d'entrée / de sortie, Montée avant le drop, Énergie, dans la feuille de partie (2 gestes).

Autres points de la vague 2 : « TP » sans « ? » → **mieux** (unités rangées sous « détails » dans la feuille de partage, `export/ShareSheet.kt:312`) ; toucher un bloc coupe sans prévenir → **mieux** (dit par le « ? » de la ligne du temps et les missions, toujours sans bulle la première fois) ; partage qui ne dit pas « gardé dans Musique » → non revu.

**Nouveau depuis la vague 2 (à corriger)** : Remix toujours caché alors que Benjamin le veut en vitrine (point 1), deux « Parties » côte à côte (point 2), étape d'export « Nouvel essai » (point 3), cartes d'automation en double (point 4), valeur absurde du limiteur (point 5).

## Top 5 maintenant

1. **Remix en vitrine** : « Que veux-tu faire ? », menu « ⋯ » du Mode simple, puce « Remix » dans Sons (point 1).
2. **« Briques » au lieu de « Parties » pour la grille A à H** (point 2).
3. **Export rassurant** : étapes en mots positifs et durée estimée (point 3).
4. **Mix plus net** : une seule carte d'automation, faders pleine largeur sur tablette, garde contre le gain à 0 (points 4, 5, 10).
5. **Batterie pad par pad dans la ligne du temps** (une sous-rangée par pad repliable).
