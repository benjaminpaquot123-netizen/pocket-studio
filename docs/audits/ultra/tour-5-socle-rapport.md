# Tour 5, lot « socle » : le socle du banc (discussion A, 2026-11-11)

Consigne : `docs/PLAN-SESSIONS-PARALLELES.md` (prompt de la discussion A). But : supprimer les causes des refus du 11 novembre.
Branche : `claude/socle-du-banc-alqcr3`, partie de `ccr-80af1a1b-mn0wec` (0d52cebc). Décisions : `docs/decisions/tour-5-socle.md`.

## 1. Une base verte : les cinq captures instables et le renommage

Chaque image a été rejouée seule, puis sous charge (6 boucles actives sur 4 cœurs, `verifyRoborazziDebug`), pour trouver sa vraie cause.

| Image | Cause trouvée | Réglage |
|---|---|---|
| `cut_screen`, `cut_screen_magnifier` | **Périmées**, pas instables : enregistrées 30 min avant que les numéros des bouts choisis s'allument (4dc642b9 après 3eb8968d). Rouges seules aussi. | Réenregistrées, regardées. |
| `recette` | **Périmée à chaque jalon** : l'image lisait la vraie `recette.json` (« 18 essais » contre 10 aujourd'hui). | Lit une copie figée (`app/src/test/resources/capture/`) ; garde-fou dans `test_socle.py`. |
| `nav/rhythm_scrolled_under_part` (et `pad_settings_card`, `pad_sound_name_whole`) | **Course** : la ligne du Guide se calcule sur un fil à part, en temps réel ; sous charge la photo partait sur « Guide » seul. Reproduit à coup sûr sous charge. | `AppHarness.awaitGuide()` avant chaque photo de la classe. |
| `remix_stem_edit_menu` (et `remix_stem_edits`) | La même course du Guide, plus les halos des éléments choisis qui varient de quelques teintes sous charge (distance de couleur 0,104 au plus, jamais une forme ni une lettre). | `awaitGuide()` ; comparaison à 0,15 de distance par pixel et 0,1 % de l'image (au lieu de 1,5 % de pixels quelconques). |

Preuves (toutes ici, 2026-11-11) :
- sans `awaitGuide`, sous charge : `pad_sound_name_whole` rouge (« Guide » seul contre « Guide · Ajoute une mélodie 45 % ») ; avec : vert, 2 passages sous charge sur les 4 classes ;
- `structureEdits` avec la nouvelle comparaison : vert 3 fois sur 3 sous charge ;
- contre-essai : la référence abîmée exprès (un mot remplacé, 0,7 % de l'image) passait avec l'ancien 1,5 %, elle rougit maintenant ;
- images avant / après regardées une à une (Guide chargé, barre du pouce d'aujourd'hui, pastilles des bouts choisis).

Renommage (`Tour3RenameIdleTest`, `Tour3StudioRenameIdleTest`) : voir la section « Renommage » plus bas.

## 2. Décisions par lot, journaux partagés

- `docs/decisions/<tour>-<zone>.md` par lot ; le portail ajoute le lien à la fin de `docs/DECISIONS.md` (`decisions-index.sh`, dans le commit de ses traces). Écrire `DECISIONS.md` ou le fichier d'un autre lot : refusé à partir du paquet 2, dit par le pré-vol dès maintenant.
- `docs/DECISIONS.md` et `banc-fiable-journal.md` se fusionnent par union (`.gitattributes`).
- Pas pour `verdicts.tsv` ni PLAN-RESTE : lignes modifiées sur place, sept scripts lecteurs ; l'union garderait l'ancienne ligne et la nouvelle.
- Preuves : `test_socle.py` (deux lots qui ajoutent chacun une ligne : fusion sans conflit ; index écrit une fois, avec son compte) ; `test_pre_vol.sh` (4 cas neufs, dont 2 où le vrai portail refuse aussi).

## 3. Rejeux par étiquette

- `touche <étiquette>` (et `appui-long`, `double-touche`) dans `HumanTester` : le bouton est amené à l'écran (`reachTag`) puis touché en son milieu ; introuvable = « divergé » en rejeu.
- `t4-26-prise-clavier` : les 26 touchers pris dans les lignes « DOIGT » du siège d'origine (`sieges/tour-4/lotA/b`) ; ses 6 glissés de défilement deviennent « attends 1 ». Rejeu changé : t4-26-prise-clavier.
- `test_socle.py` refuse un rejeu qui touche encore en pixels.
- Preuve : voir « Rejeu joué » plus bas.

## 4. CI : les contrôles rapides d'abord

Travail « quick » (`pre-vol.sh --ci --rapide` : tests Python du banc, `test_socle.py`, `test_pre_vol.sh`, syntaxe des workflows ; 36 s ici). « engine » et « separation » l'attendent, donc tous les autres aussi. `test_socle.py` le vérifie.

## 5. Règles du portail gelées par paquet

`scripts/bench/portail-paquet.txt` (paquet 1). Une règle ajoutée pendant le paquet N porte `from_paquet N+1` sur sa ligne de refus ; `test_socle.py` le vérifie contre l'historique du fichier du paquet (rougit si on retire la garde : essayé). Le pré-vol dit la règle à venir en « À VOIR ». `KNOWN_RULES` : `12 61ed156166`.

## Rejeu joué

(à compléter)

## Renommage

(à compléter)
