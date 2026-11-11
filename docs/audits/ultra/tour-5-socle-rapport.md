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
- `docs/DECISIONS.md` se fusionne par union (`.gitattributes`) ; le journal du banc non, ses lignes changent sur place.
- Pas pour `verdicts.tsv`, PLAN-RESTE ni le journal : lignes modifiées sur place ; l'union garderait l'ancienne ligne et la nouvelle.
- Preuves : `test_socle.py` (deux lots qui ajoutent chacun une ligne : fusion sans conflit ; index écrit une fois, avec son compte) ; `test_pre_vol.sh` (4 cas neufs, dont 2 où le vrai portail refuse aussi).

## 3. Rejeux par étiquette

- `touche <étiquette>` (et `appui-long`, `double-touche`) dans `HumanTester` : le bouton est amené à l'écran (`reachTag`) puis touché en son milieu ; introuvable = « divergé » en rejeu.
- `t4-26-prise-clavier` : les 26 touchers pris dans les lignes « DOIGT » du siège d'origine (`sieges/tour-4/lotA/b`) ; ses 6 glissés de défilement deviennent « attends 1 ». Rejeu changé : t4-26-prise-clavier.
- `test_socle.py` refuse un rejeu qui touche encore en pixels.
- Preuve : voir « Rejeu joué » plus bas.

## 4. CI : les contrôles rapides d'abord

Travail « quick » (`pre-vol.sh --ci --rapide` : tests Python du banc, `test_socle.py`, `test_pre_vol.sh`, syntaxe des workflows ; 36 s ici). « engine » et « separation » l'attendent, donc tous les autres aussi. `test_socle.py` le vérifie.

## 5. Règles du portail gelées par paquet

`scripts/bench/portail-paquet.txt` (paquet 1). Une règle ajoutée pendant le paquet N porte `from_paquet N+1` sur sa ligne de refus ; `test_socle.py` le vérifie contre l'historique du fichier du paquet (rougit si on retire la garde : essayé). Le pré-vol dit la règle à venir en « À VOIR ». Un lot qui change lui-même le paquet est refusé à partir du paquet 2. Limites : la garde ne voit que les lignes de refus (« not merged », « undone ») et ne dit rien sur un clone superficiel (la CI). `KNOWN_RULES` : `13 551b6bf796`.

## Rejeu joué

- `scripts/bench/rejeu-defauts.sh` sur 28e64fe3 (testeur avec étiquettes) : autotest du banc vert (165 verts, 0 rouge, 0 en quarantaine, 14 sautés connus), puis « t4-26-prise-clavier : oracle tenu ».
- Le même rejeu avec le testeur d'avant (`HumanTester.kt` de 1bf20cb1) : « t4-26-prise-clavier : pas de verdict », chaque `touche <étiquette>` lu « geste mal écrit », le siège finit « rien ne change (5 écrans identiques) » au pas 20.
- Trouvé en route par l'autotest : le portail sortait en silence sans `portail-paquet.txt` (`set -e` sur la lecture du paquet) et sa copie-jouet n'avait pas les scripts neufs ; corrigé (b12923d1 rouge, abc05262 vert), garde dans `test_socle.py`.

## Renommage

`Tour3RenameIdleTest` et `Tour3StudioRenameIdleTest` étaient en quarantaine depuis le 8 novembre (rouges 4 passages combinés sur 5 au lot banc45). Le lot « banc-fin » (10 novembre) a changé leur règle Compose (v2 : un flux émis sur un autre fil reprend sur le fil principal), cause trouvée alors. Depuis : verts dans la suite complète de la CI 253 et 254 (l'étape « Unit tests and screenshots » ; le rouge de la 254 était « Real projects »), et dans l'autotest complet d'ici dont j'ai lu le détail (les deux classes « vert », 0 en quarantaine). Avec l'ancien taux (1 vert sur 5), trois verts d'affilée arrivent moins d'une fois sur 100 : réglé. Les deux lignes sortent de `selftest-quarantaine.txt` : un rouge du renommage redevient un vrai rouge (le renommage est un geste de Benjamin). Retour arrière : remettre les deux lignes (dans l'historique de ce fichier).

## Après la relecture neuve (0 bloquant, 1 grave)

- Grave 1 (la règle des décisions prouvée par un refus quelconque) : `test_pre_vol.sh` exige maintenant du vrai portail la raison « decisions written outside » au paquet 2, et son absence au paquet 1 et pour le fichier du lot lui-même ; `test_ultra_bench.py` fait écrire l'index par le portail complet de la maquette. Les mutations du relecteur (`-z`, `-lt`, `if false`) rougissent toutes ; **corrigé**.
- Moyens corrigés : annulation qui restaurait `DECISIONS.md` depuis l'index (`checkout HEAD`, garde dans la maquette avec un commit des traces refusé) ; journal retiré de l'union ; un lot qui change le paquet est refusé à partir du paquet 2.
- Fiche point 8 : ce lot ne touche aucun item de PLAN-RESTE ni ligne de `verdicts.tsv` ; rien à y suivre.
