# Rapport du lot fable-repar (tour 7, zone tuto)

Branche : `eq/tour-7-fable-repar`, partie de `eq/tour-7-tuto3` (13dcec5f), intégration fusionnée (da76359f).
Référence : `docs/audits/ultra/tour-7-fable-tuto-audit.md` § 4. Décisions : `docs/decisions/tour-7-fable-repar.md`.

## Ce qui est fait

- [x] **P1, test « Montre-moi partout »** (33e4b848) : `TutoShowMeEverywhereTest` joue chaque étape des 37 cours, aux 2 niveaux, depuis 7 arrivées, par `TutoPlay`, le même chemin que l'appli (`TutoController`). Il écrit ses fautes dans `PS_TUTO_FAULTS` ; à la fin du lot, ces fichiers sont vides.
- [x] **P2, R-C, ce qui manque est toujours offert** (rouge 14e68f8d, vert 9168a324 et 764eb403) : un seul bouton « Poser ce qui manque » avant le bouton de l'étape.
- [x] **P3, R-B, une partie ne naît jamais faite** (rouge 8f3e18d0, vert 3dcc5560).
- [x] **P4, R-A, une étape qui demande un geste est à faire quand on y arrive** (rouge 5a0ac1b5, vert fa24c776) : le niveau du cours est passé partout.
- [x] **P5, R-D, rien de l'étape hors de sa partie** (rouge 8c10a4fe, vert d6cd432d) : ni pointillé ni croix dans une autre partie.
- [x] **Note de basse hors écran** (rouge 84906407, vert b52ac006, puis 1cdf6800 et cfb46c58) : la ligne montre une flèche vers la note cachée ; toucher la ligne fait défiler jusqu'à elle, sans jamais cacher la note demandée. « Je le fais » et « Copier » ne déplacent pas le rouleau vers elle.
- [x] **Panneau déplié qui cache la grille** (rouge 33ea7f6b, vert 14238dde) : lien « Réduire », traduit dans les 14 langues.
- [x] **Guillemets doublés en coréen** : déjà corrigés par tuto3 (49561114), rien à refaire.

## Exceptions du test P1 (justifiées une par une)

- Arrivées « boucle faite, aucune partie » : jouées à partir du premier chapitre qui crée une partie (les chapitres de la boucle y sont déjà faits).
- Break de `retro_techno` : garde ce que son étape garde (voulu par le cours).
- Silence de `synthwave` : n'existe qu'en Pro.
- Étapes « C'est fait », écoutes et partage : hors de portée de « Montre-moi », jouées dans l'appli par `TutoJourneyTest`.

## Ce qui n'est pas fait

- **P6 (règle de lisibilité), P7 (une consigne par moment), P8 (`TutoKind`)** : facultatives dans la fiche. P8 touche chaque type d'étape et demande un lot à elle seule ; P6 et P7 changent des textes dans 37 cours et 14 langues. Proposées comme lot suivant, sous la garde du test P1.

## Phrases d'acceptation (à constater par un siège neuf : « tenté », pas « corrigé »)

- Arrivé sur n'importe quelle étape, je vois soit un bouton qui fait quelque chose, soit « Montre-moi », et l'étape se coche après.
- Une partie que le cours vient de créer n'a pas déjà son roulement ni son crash : l'étape suivante me les demande.
- Je ne vois jamais le pointillé ni les croix d'une étape dans une autre partie que la sienne.
- Basse : quand une note en pointillé est plus haut ou plus bas que l'écran, la ligne porte une flèche ; je touche la ligne, le rouleau y va et la note demandée reste visible.
- Panneau déplié sur la grille : je touche « Réduire », les cases reviennent.

## Vérifications de fin

Tête vérifiée : 8a58abe1 (intégration da76359f fusionnée).

- **Tests du tuto et du rouleau** (`*Tuto*`, `*PianoRoll*`, appli et modèle) : verts. Test P1 : fichiers de fautes vides.
- **Flèche coupée sur un téléphone de 360 dp** (vue sur la capture `tuto_app_bass` : « Encore 4 notes en pointillé à to… ») : rouge 5be1679c, vert bf63b75e. La flèche passe en tête de ligne (« ↑ Encore 4 notes… »). Capture refaite et regardée (8a58abe1).
- **M3 de la relecture** : l'assertion retirée de `Tour7TutoPartMadeTest` est remise sous une forme équivalente (74aaea7f) : la ligne ne nomme que ce que la partie contient.
- **Tous les tests et toutes les captures** (`verifyRoborazziDebug`, 9131 tests) : 51 échecs, dont 50 captures « changées » et la tâche elle-même. Une seule capture est de ce lot (`tuto_app_bass`, refaite). Les autres sont hors de la zone tuto et viennent de l'intégration (références non refaites par d'autres lots) : `projects*`, `remix*`, `take_done*`, `share_sheet*`, `partage/*loop*`, `tablet/*`, `tablet8/*`, `scale130/*`, `i18n/nl_projects`, `en_projects`, `hi_mixer`, `tour6/shell_*`, `tabA11/130_nav_mix`, `folded_cards`, `welcome`, `piano_roll*` (ordre des boutons « Basse en fantôme » / « En rythme », que ce lot ne touche pas). Non refaites ici : hors de la liste de fichiers. Dit au chef.
- **Rejeux des défauts** : un rouge, `t4-26-prise-clavier` (le bouton « Enregistrer ma mélodie » a bougé, le doigt tombe sur une touche). Rejoué deux fois : rouge. Rejoué sur l'intégration seule (da76359f) : rouge aussi. Il ne vient donc pas de ce lot. Dit au chef. Les autres : oracle tenu.

## Points ouverts de la relecture (`tour-7-fable-repar-relecture.md`)

- M1 : le coût de `TutoChecks.need` sur le fil principal n'est pas mesuré.
- M2 : « Poser ce qui manque » ne dit pas ce qu'il a posé.
- M4 : la branche « pas la partie de l'étape » de `hintFault` n'est jamais atteinte dans `TutoShowMeEverywhereTest`.
- Mineurs : un toucher qui ne peut rien déplacer laisse la pastille « Touche ici » ; `lit` lu avant le bornage (`PianoRoll.kt`) ; variable `changes` inutilisée ; `canShow` lit `show` et non `demo`.

## Ce qui n'est pas vert

- Le rejeu `t4-26-prise-clavier` (rouge aussi sur l'intégration seule).
- Les captures hors zone tuto venues de l'intégration.
- M1, M2, M4 et les mineurs de la relecture ; P6 à P8 non faites.
