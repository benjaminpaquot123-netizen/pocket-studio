# Audit 35 · La tablette de Benjamin (Galaxy Tab A11), écran par écran

Demande de Benjamin (2026-10-24) : « l'audit doit aussi couvrir l'apparence sur tablette (Galaxy Tab A11, portrait et paysage) ».

## Comment

- Appareil simulé : Galaxy Tab A11, 8,7", 1340 × 800 px, densité ≈ 1,33 (tvdpi), soit **600 × 1006 dp debout**. On retire ce que le système prend (barre d'état 24 dp, barre de navigation 48 dp) : la fenêtre réelle de l'appli est **600 × 934 dp debout** et **1006 × 528 dp couchée**. C'est ce qui a été rendu (et non 600 × 1006, comme le font les captures `tablet8/` du dépôt, qui ajoutent ces bandes en noir autour d'un écran seul).
- L'appli entière (`AppRoot`, ses espaces, ses feuilles et fenêtres), pilotée touche après touche dans Robolectric via `AppHarness`, branche `audit`, commit `0aa9bc7`, à 100 % et à 130 % ; sans moteur audio.
- Images : `shots/<persona>/tabP_*` (debout), `tabL_*` (couchée), `tabP130_*`, `tabL130_*` (130 %), avec `shots/` = `/tmp/claude-0/-home-user-pocket-studio/2ed29bda-5e9a-5850-a6a7-bbf3293c7a09/scratchpad/audit-35/shots/`. Planches de comparaison : `…/audit-35/tp/1.png` à `7.png` (debout), `…/audit-35/tl/1.png` à `14.png` (couchée), `…/audit-35/t130/1.png` à `4.png` (130 %).
- Le dépôt n'a pas de capture de l'appli entière à la taille de la Tab A11 : `nav/NavScreenshotTest` passe la tablette en 800 × 1280 dp (11") ; les captures `tablet8/` sont des écrans seuls. Les tests jetables de cet audit ont été retirés (voir la fin).

## Verdict

**Debout (600 × 934)** : **bon**. Tout tient, rien n'est coupé, les cibles sont grandes ; c'est un grand téléphone. Défaut principal : de la place perdue (écrans d'accueil et console du Mix à moitié vides).

**Couchée (1006 × 528)** : **correct partout sauf deux écrans**. Le rail à gauche et les deux colonnes (Parties, Aide, Live, Rythme du Mode simple) marchent bien. Mais **l'écran du clavier perd son clavier** dès qu'une prise est faite (et dès l'ouverture à 130 %), et la 4e rangée de pads est coupée dans Sons › Batterie.

**À 130 % (debout et couchée)** : **à corriger** : toutes les fenêtres et feuilles (« Que veux-tu faire ? », Bienvenue, feuille de partie, « Prêt à partager ? », export, kit, pad, tempo, Signaler un bug) restent à 100 %, plus petites que l'écran derrière.

## Écran par écran

| Écran | Debout (600 × 934) | Couchée (1006 × 528) | Captures | Gravité / correction |
|---|---|---|---|---|
| Bienvenue (1er lancement) | Bien, tout tient. | Colonne centrée de 550 dp ; « Partir de zéro » sous la ligne de flottaison (défilement). | `amateur1/tabP_premier_01_bienvenue.png`, `tabL_premier_01_bienvenue.png` | mineur : en paysage, choix du mode à gauche, trois cartes à droite. |
| « Que veux-tu faire ? » | Cartes en haut, **moitié basse vide**. | Deux colonnes (Continuer à gauche, Nouveau / Mes projets à droite) : bien. | `amateur1/tabP_ouverture_01_que_veux_tu_faire.png`, `tabL_ouverture_01_…`, `tabP130_ouverture_01_…` | mineur : centrer verticalement debout ; **à 130 % reste à 100 %** (voir plus bas). |
| Zéro projet, Mes projets vide | Haut de l'écran seulement ; 3 boutons « nouveau », 2 « importer ». | Idem, colonne centrée. | `amateur1/tabP_zero_02_mes_projets_vide.png`, `tabL_zero_02_…` | mineur : une seule carte. |
| Nouveau projet (styles) | Bien, familles repliées, cartes lisibles. | Bien. | `amateur1/tabP_ouverture_02_nouveau_projet.png`, `tabL_ouverture_03_famille_calme.png` | — |
| Assistant pas à pas (13 étapes en Techno) | Bien ; les cartes de sons sur deux colonnes. | Une seule carte visible à la fois au-dessus de « Retour / Suivant » ; le choix du son passe sous la ligne de flottaison. | `amateur1/tabP_assistant_*.png`, `tabL_assistant_*.png` | mineur : en paysage, l'explication à gauche et les sons à droite. |
| Structure, ligne du temps | Pistes qui remplissent la hauteur, colonne fixe, outils sur une ligne. | Rail + ligne du temps pleine largeur : bien. Les drapeaux « A · Int / B · Re / C · Dr » coupés (barres de 48 dp au plus). | `amateur3/tabP_remix_02_structure.png`, `tabL_remix_02_structure.png` | mineur : nom de partie sur deux lignes quand la barre est étroite. |
| Structure, Parties | Une colonne. | **Deux colonnes** : le morceau à gauche, la grille des briques à droite, toutes deux titrées « Parties ». | `amateur3/tabP_parties_01_liste_parties.png`, `tabL_parties_01_liste_parties.png` | moyen : renommer la grille « Briques » (voir amateur3). |
| Feuille de partie | Pleine largeur, tout visible. | Feuille centrée ; Longueur / Dupliquer / Retirer sous la ligne de flottaison. | `amateur3/tabP_feuillepartie_02_feuille_partie.png`, `tabL_feuillepartie_02_…` | mineur. |
| Sons › Batterie (Mode complet) | Pads 4 × 4 de 120 dp, grille dessous : bien. | Pads à gauche, grille à droite ; **4e rangée de pads coupée**, outils de la grille coupés (« Force »). | `amateur2/tabP_batterie_01_batterie.png`, `amateur3/tabL_remix_03_sons.png`, `amateur2/tabL_batterie_04_menu_coup.png` | **moyen** : pads dimensionnés sur la hauteur libre (Mode simple le fait déjà : `amateur4/tabL_simple_02_rythme.png`). |
| Kit, feuille de pad, menu du coup | Bien ; le menu du coup est étroit (« ×8 » seul sur une 2e ligne). | Bien. | `amateur2/tabP_batterie_02_kit.png`, `tabP_batterie_04_menu_coup.png`, `tabL_batterie_04_menu_coup.png` | mineur : menu de 360 dp mini. |
| Sons › clavier (synthé) | 2 octaves, touches hautes, « Enregistrer ma mélodie » au-dessus : bien. | 3 octaves, mais une bande vide de ≈ 80 dp sous « Clavier / Notes » ; **après une prise, la carte de prise et la bande « boucle » repoussent le clavier à 60 dp de touches**. | `amateur4/tabP_parcours_03_synthe.png`, `tabL_parcours_03_synthe.png`, `tabL_parcours_05_fin_de_prise.png` | **grave** (paysage) : hauteur mini du clavier, carte de prise à côté. |
| Sons › Voix | Bien. | Bien (carte « Enregistrer sur » pleine largeur). | `amateur2/tabP_voix_02_prise_voix.png`, `tabL_voix_02_prise_voix.png` | — |
| Sons › Voix › Remix | Une carte en haut, le reste vide (ici l'avertissement de mémoire de Robolectric). | Idem. | `amateur3/tabP_remix_05_remix.png`, `tabL_remix_05_remix.png` | Trouvabilité : **grave** (voir `gestes.md`). |
| Mix, console | Faders de ≈ 130 dp sur **les deux tiers** de la largeur, la console n'occupe qu'un tiers de la hauteur. | Console à gauche (la voie Remix coupée, il faut faire défiler), Automation et Sortie à droite ; « retenu de 214748368,0 dB » sans moteur. | `amateur3/tabP_mix_01_console.png`, `tabL_mix_01_console.png` | moyen : faders pleine largeur et plus hauts debout ; garde contre le gain 0 (amateur3, point 5). |
| Feuille d'une voie, effets | Bien. | Feuille centrée, lisible. | `amateur3/tabP_mix_02_feuille_voie.png`, `amateur5/tabL_effets_02_ajouter_effet.png` | — |
| Live | Grille + panneau « Effets et 303 » côte à côte : bien. | Grille à gauche, panneau à droite : bien ; la ligne « La prise : Nouvelle version » n'apparaît qu'une fois armé. | `amateur5/tabP_live_01_live.png`, `tabL_live_01_live.png` | mineur. |
| Pads live plein écran | 8 grands pads ; **REC et « Tout couper » = deux ronds rouges sans mot côte à côte**. | « Tout couper » nommé, « Effets » à droite : bien. | `amateur5/tabP_live_04_pads_plein_ecran.png`, `tabL_live_05_pads_plein_ecran.png` | moyen : le mot partout. |
| Plus | Une colonne (pas de liste + page à 600 dp). | Liste à gauche, page à droite : bien. | `amateur4/tabP_plus_01_plus.png`, `tabL_plus_02_aide.png` | — |
| Lexique | Bien. | Bien. | `amateur5/tabP_glossaire_01_glossaire.png`, `tabL_glossaire_01_glossaire.png` | — |
| Signaler un bug | Bien, tout est expliqué. | Feuille centrée, bien. | `amateur5/tabP_bug_01_signaler_bug.png`, `tabL_bug_01_signaler_bug.png` | mineur : « 1 erreurs ». |
| « Prêt à partager ? » / Envoyer | Bien. | Bien (feuille centrée ; onglet Envoyer en colonne de 600 dp). | `amateur3/tabP_partage_01_pret_a_partager.png`, `tabL_partage_01_…`, `amateur1/tabL_envoyer_01_envoyer.png` | — |
| Export, étapes | Boîte centrée, barre et étape nommée. | Idem. | `amateur3/tabL_exportetapes_01_fabrication.png` → `…_04_nouvel_essai.png` | mineur : « Nouvel essai » (amateur3). |
| Mode simple, Mon morceau | Bien. | **Deux colonnes** (Le morceau / Mes pistes) : bien ; « Mon morceau » touche les bords du rail. | `amateur1/tabP_monmorceau_01_mon_morceau.png`, `tabL_monmorceau_01_mon_morceau.png` | mineur. |
| Mode simple, Rythme | Bien. | Pads et grille côte à côte, **tout tient** (mieux que le Mode complet). | `amateur4/tabP_simple_02_rythme.png`, `tabL_simple_02_rythme.png` | — |
| Mode simple, Clavier | Bien. | Même défaut que le synthé : clavier repoussé sous le bas après la prise. | `amateur1/tabL_clavier_04_remis_en_rythme.png` | **grave** (paysage). |
| Étiquettes « ? » | Se chevauchent, « Écouter » coupée en haut. | Idem ; « Mes projets » posée loin de son dossier ; elles couvrent le rail. | `amateur1/tabP_etiquettes_01_etiquettes.png`, `tabL_etiquettes_01_etiquettes.png` | moyen. |

## À 130 % sur la tablette

| Écran | Debout (≈ 461 × 718 dp utiles) | Couchée (≈ 774 × 406 dp utiles) | Captures |
|---|---|---|---|
| Écrans principaux | Lisibles, rien de coupé. | Ligne du temps, Live, Mon morceau : lisibles ; la colonne « Lead » du Live coupée. | `amateur4/tabP130_parcours_02_structure.png`, `tabL130_parcours_02_structure.png`, `amateur5/tabL130_live_01_live.png` |
| Clavier | Bien. | **Le clavier est hors de l'écran** à l'ouverture (bord des touches seulement), et entièrement après la prise. | `amateur1/tabL130_clavier_01_clavier.png`, `tabL130_clavier_04_remis_en_rythme.png` |
| Fenêtres et feuilles | **À 100 %**, plus petites que l'écran derrière. | Idem. | `amateur1/tabP130_ouverture_01_que_veux_tu_faire.png`, `amateur3/tabP130_partage_01_pret_a_partager.png`, `tabL130_partage_01_…`, `amateur3/tabP130_exportetapes_01_fabrication.png` |

Cause du dernier point : `UiScaled` (`app/src/main/java/com/pocketstudio/app/ui/UiScale.kt:17-29`) ne vaut que dans la fenêtre principale ; chaque `Dialog` et `ModalBottomSheet` ouvre sa propre fenêtre, qui reprend la densité de l'appareil. Même chose sur le téléphone (`shots/amateur3/p130_feuillepartie_02_feuille_partie.png`, `…/audit-35/cmp_launch.png`).

## Les trois corrections qui comptent pour la tablette

1. **grave** · Clavier en paysage : hauteur minimale du clavier, carte de prise et bande « boucle » à côté ou repliées (Sons et Mode simple).
2. **grave** · 130 % dans toutes les fenêtres et feuilles : une enveloppe commune qui réapplique l'échelle des réglages dans `Dialog` / `ModalBottomSheet`.
3. **moyen** · Batterie en paysage (pads sur la hauteur libre) et Mix debout (console pleine largeur, faders plus hauts).

Et pour ne plus le rater : des captures de l'appli entière à la taille réelle de la Tab A11 (600 × 934 et 1006 × 528, à 100 % et 130 %) dans `nav/NavScreenshotTest`, à côté du 11" actuel, avec au moins une feuille ouverte à 130 %.

## Annexe : la liste de recette du jalon (`app/src/main/assets/recette.json`), item par item

Coché = vu fonctionner à l'écran dans ce banc (téléphone et tablette). « Code » = présent dans le code mais pas exerçable sans moteur. « Appareil » = à faire par Benjamin (son, casque, MIDI, vidéo).

| Item | Verdict | Ce qui a été vu |
|---|---|---|
| mode — Premier lancement | ☑ | Bienvenue avec Simple / Complet ; « Mode Simple / Complet » dans « Que veux-tu faire ? » ; changer de mode ne touche pas au projet (`nav/LaunchFlowTest`). |
| spaces — Les 5 espaces | ☑ | Structure, Sons, Mix, Live, Plus ; deux touches sur un bloc ouvrent l'éditeur, toucher la règle joue de là (mesuré, `gestes.md`). |
| missions — Missions et « ? » | ☑ avec réserve | Mission 2/6 affichée, étiquettes « ? » posées ; elles se chevauchent (amateur1 point 3) et Live n'a pas de « ? » (amateur5 point 1). |
| song_sound — Le son de l'assistant | ☐ Appareil | À l'oreille. |
| long_club — Morceaux club longs | ☑ avec réserve | Résumé de l'assistant Techno « environ 5 min 20 » ; mais « Partir d'un style » dit « environ 3 minutes » (amateur1 point 10). |
| part_sheet — Feuille de partie | ☑ | Fill, Montée avant, Fondu de sortie, Ajouter une fin, Énergie, Variation, Break. Annulation : non exercée. |
| kit_pad — Kit et pads | ☑ | Kit ▾, appui long sur un pad (son, volume, pan), ×8 Montant ; peindre : consigne à l'écran (non glissé dans le banc). |
| record — Enregistrer une mélodie | ☑ avec réserve | ● Enregistrer, ■, « Remettre en rythme », « Comme joué » ; le décompte 4-3-2-1 n'est pas visible sans moteur. |
| keep_played — Garder ce que je viens de jouer | Code | Bouton « Garder ce que je viens de jouer » (`record/RecordBar.kt:116`). |
| voice — Voix et prises | Code | « Enregistrer sur », « Refaire mes. 5-8 », « Recaler sur le tempo » (`audiotracks/AudioTracksScreen.kt:417, 542`) ; prises à faire sur l'appareil. |
| live_versions — Versions du morceau | ☑ avec réserve | « Nouvelle version » par défaut ; « Versions du morceau » non exercé. |
| chords — Accords du morceau | ☑ | Grilles avec noms d'accords, « Appliquer à tout le morceau », pistes qui suivent (la 303 « Ligne acide » dans la liste). |
| clipboard — Copier-coller | Code | Non exercé. |
| poly — Polyrythme et hasard | ☑ avec réserve | « Chance de jouer 100 / 75 / 50 / 25 % » vu ; « Ce pad boucle sur » dans le code (`rhythm/RhythmScreen.kt:700`). |
| freeze — Geler la piste | Code | Menu de la piste (appui long sur la puce) › « Geler la piste ». |
| instruments — Instruments réels | Appareil | Téléchargement. |
| track808 — Piste 808 | ☑ | « Basse 808 » dans « Ajouter une piste » ; la glisse à l'oreille. |
| export — Export propre | Appareil | « Prêt à partager », crête et « crêtes tassées » sont dans le code (`export/ExportCheck.kt:253-265`, `mixer/MasterLoudness.kt:161`). |
| video — Vidéo 9:16 | Appareil | Bouton « Vidéo pour les réseaux · 30 s » vu. |
| glossary — Glossaire | ☑ | Plus › Les mots de la musique, chaque mot avec ▶ ; « Sidechain (pompe) » sans / avec dans le code. |
| midi — Potards MIDI | Appareil | — |
| headphones — Casque | Appareil | — |

Ce que la liste de recette ne demande pas et qu'elle devrait demander, vu les retours de Benjamin : « Que veux-tu faire ? » à l'ouverture, zéro projet, Signaler un bug, les étapes de l'export, **le Remix**, et **un tour sur la tablette debout et couchée, à 130 %**.

## Tests jetables

Les tests de cet audit (`app/src/test/java/com/pocketstudio/app/audit35/`) ont été **retirés** avant le commit : ils dépendaient de chemins de cette machine et d'étapes fragiles. Ce qui mérite d'être gardé est décrit ci-dessus (captures de l'appli entière à la taille de la Tab A11, une feuille ouverte à 130 %).
