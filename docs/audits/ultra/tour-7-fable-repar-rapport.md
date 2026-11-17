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

## Étape 4 : les deux phrases rendues par le chef (17 novembre, soir)

Le chef a rendu au lot la phrase du chapitre 12 (Facile) et la phrase Pro de la cymbale.

- Chapitre 12, étape 1, Facile (techno) : la phrase disait « 3 coups du pad Charley », l'écran barrait 4 coups du Clap. Elle dit maintenant « 4 coups du pad Clap (touche les 4 croix rouges) ». L'ancienne phrase reste pour le Pro, où elle est vraie. Rouge f9160320 (`Tour7TutoGapWordsMatchCrossesTest`, revenu tel quel), vert e40ab1a6.
- Cymbale, Pro (techno et detroit) : « un seul coup » était faux, la cymbale joue déjà dans la partie. La phrase dit « la toute première case, encore vide : un coup au moment où tout arrive ». Rouge fd4a8325 (`Tour7TutoCrashWordsMatchPadTest`), vert 4170af63.
- 14 langues mises à jour. Deux tests de `Tour6Tuto4TechnoTest` lisent maintenant la phrase Pro.
- Relecture neuve de ces quatre commits : 0 grave.

## Ce qui n'est pas vert

- Le rejeu `t4-26-prise-clavier` (rouge aussi sur l'intégration seule).
- Les captures hors zone tuto venues de l'intégration.
- M1, M2, M4 et les mineurs de la relecture ; P6 à P8 non faites.

---

# Suite du lot : points de la relecture, défauts du contrôle, P8

Départ : 25844180 (relecture neuve). Fin du travail : fe9502c2. Intégration f8552f55 fusionnée ensuite (3e3ce3b0), sans conflit.
Les sections ci-dessus décrivent le premier passage et restent telles quelles ; ce qui suit les complète et remplace leur « Ce qui n'est pas vert ».

## Étape 1 : points ouverts de la relecture (jusqu'à 26030d3f)

- [x] **Ménage** (70dd7a69) : la fonction `changes()` inutilisée est retirée, le commentaire de `chaptersOf` est remis à sa place, `Tour7TutoMissingOfferedTest` joue à son niveau.
- [x] **M1, coût du bouton de l'étape : mesuré, puis allégé** (mesure f2e9b5a4, allègement 80c3c407). `Tour7TutoNeedCostTest` calcule le bouton sur chaque étape de chaque cours, aux 2 niveaux : 5692 états. Sur la machine de test :

  | | médiane | 99 % sous | pire cas |
  |---|---|---|---|
  | avant | 0,17 ms | 6,05 ms | 11,63 ms |
  | après | 0,16 ms | 4,43 ms | 8,52 ms |

  Mêmes réponses qu'avant. Le bouton ne construit plus un « Montre-moi » entier juste pour savoir s'il existe, et la dernière réponse est relue quand on la redemande sur le même projet. Limites : environ 58 % de ce qui reste est dans `Project.sanitized()`, hors des fichiers du lot ; `save()` tourne toujours sur le fil principal.
- [x] **« Montre-moi » offert seulement là où il montre quelque chose** (rouge 57de3a9e, vert 72b7fff2 ; `Tour7TutoShowOfferedShowsTest`) : avec un Sub à soi et plus de piste libre, le bouton était dessiné et ne faisait rien. Le bouton du panneau, le bouton de l'étape et la démonstration lisent maintenant la même source (`TutoChecks.canShow`).
- [x] **M4, pointillés hors de leur partie : jugement indépendant** (6dd84be8, 84262802, 26030d3f). Le test P1 montre chaque autre partie à son tour et juge sans passer par la règle de l'appli. `Tour7TutoDottedInItsPartTest` joue trois styles (le troisième est pop : dnb n'a pas d'étape crash en Facile). Limite : la moitié « silence » du test n'est pas prouvée capable de passer au rouge.
- [x] **M2, « Poser ce qui manque » dit ce qu'il a posé** (rouge 6b0dd1e5, vert d3c7c378 ; `Tour7TutoMissingSaidTest`) : le panneau dit « Posé : … » avec le nom des lignes vraiment changées, dans les 14 `tuto.json`.
- [x] **Police à 200 % : la consigne garde sa place sous la carte du kit** (rouge bcf9eb03, vert 1747e343 ; `Tour7TutoKitBigFontTest`). Le correctif avait coupé les boutons du panneau sur un téléphone couché (`TutoPanelFitsPhoneLandTest` rouge) : réparé par 0d4bdbc9.
- [x] **Le mode Accords allumé par l'utilisateur reste allumé à la fin du tuto** (rouge 15443e4a, vert a58adb40 ; dans `Tour6TutoFlechesChordsTest`).
- [x] **Un cours fini avant le découpage des étapes Facile en morceaux (`_N`) reste fini** (rouge 3e15566c, vert c4a5ff5f ; `Tour7TutoOldProgressTest`). La progression enregistrée depuis n'est pas touchée.
- [x] **« Touche ici » déplace toujours le rouleau** (rouge d0297729, vert d2b34d01 ; `Tour7TutoTouchMovesTest`) : quand aucun déplacement ne garde la note demandée à l'écran, le toucher va à la note cachée.
- [x] **La note allumée après un toucher est visible au bord du clavier** (préparation cf1b8bc1, rouge 93964c1f, vert f8b55b4e ; `Tour7TutoLitInSightTest`).

## Étape 2 : défauts du contrôle à 5 sièges, joué sur 25844180 (jusqu'à 37d5bfcc)

- [x] **Une partie vide nommée « Montée » n'est plus « Déjà fait »** (siège d, V2 ; rouge e2e4a227, vert 494a125e ; `Tour7TutoEmptyPartNotDoneTest`, `Tour7TutoEmptyPartUndoneTest` sur tous les styles et niveaux).
- [x] **« Créer la partie » place la partie dans le morceau** (rouge 85e90088, vert e587ae27 ; `Tour7TutoMadePartInSongTest`, `Tour7TutoMadePartPlacedTest` : 261 boutons vérifiés).
- [x] **La partie qui manque est dite au-dessus du bouton** (rouge c167f816, vert 154dc199 ; `Tour7TutoMissingPartSaidTest`) : une phrase, dans les 14 langues ; l'étape qui crée la partie ne la dit pas.
- [x] **La consigne se voit au-dessus de l'offre de kit à 100 % et 130 %** (siège a, V2 ; 4af17daa ; `Tour7TutoKitWordsInSightTest`). Vert tout de suite sur le code du moment (réparé par 1747e343) ; vu rouge en retirant la place réservée.
- [x] **Une étape faite dans une partie n'est plus en pointillé dans une autre** (siège a, V4 ; rouge 781d2bd2, vert a3aafd9f ; `Tour7TutoDoneElsewhereTest`, `Tour7TutoDoneElsewhereEverywhereTest` sur tous les styles et niveaux).
- [x] **Un texte coupé le dit par « … »** (siège c, V4 ; rouge e3dfd439, vert 5d543c8f ; `Tour7TutoCutWordsSaidTest`) : vérifié en coréen et en japonais, à 100 % et 130 %.
- [x] **Toucher la ligne du rouleau atteint la note visée** (siège a, V1 ; 0127ad16 ; `Tour7TutoArrowReachesEverywhereTest`). Vert tout de suite : le défaut était réparé après le commit joué par les testeurs ; vu rouge en retirant le déplacement.
- [ ] **Chapitre 12, étape 1 : les mots ne disent pas ce que l'écran barre.** En techno Facile, la consigne parle de 3 coups du pad « Charley » ; l'écran barre 4 coups du pad « Clap ». Test écrit rouge (7801c5bf, `Tour7TutoGapWordsMatchCrossesTest`), puis retiré (37d5bfcc) : la réparation est une nouvelle phrase Facile dans chaque langue, donc un travail de l'équipe des textes (P6/P7). Le test attend dans 7801c5bf.

## Étape 3 : P8, une seule définition par type d'étape (jusqu'à fe9502c2)

- [x] **`TutoKind`** : chaque mot de vérification d'une étape (« lane:hat », « notes:bass », « gap »…) donne un seul objet, lu une fois, qui dit ce qui coche l'étape, ce que « Montre-moi » écrit, ce que le bouton demande d'abord et ce que l'écran met en pointillé ou barre. `TutoChecks` reste la façade.
- Migré famille par famille, sous la garde du test P1 (`TutoShowMeEverywhereTest` : 7 arrivées × 37 styles × 2 niveaux) :
  - b5db4376 : tempo, tonalité, swing, étapes à la main, écoutes, partage ;
  - 0842066a : lignes de batterie, coups doux, décalage, triolets ;
  - b1167109 : notes, ligne de la 303, piste Sub ;
  - 858f56f0 : effet, envoi, courbe, master, volume final, étapes « changé » ;
  - a161c14b : nom de partie, partie, morceau, roulement, crash, silence ;
  - fe9502c2 : anciennes branches et leur repli retirés.
- **`TutoKindTest`** : les 119 mots distincts des `tutos/*.json` donnent chacun un type connu ; un mot est lu une seule fois ; un mot inconnu donne le type « inconnu ».
- **Interface plus large que le croquis de l'audit (§ 3.3)** : voir la décision. En bref, cocher une étape est tolérant, « Montre-moi » écrit une réponse exacte, et les tests tiennent aux deux.
- **Différences de comportement, seulement sur des mots qu'aucun cours n'emploie** : `automationX` devient « inconnu » ; `send:0:x` ne se coche jamais ; un `crashX` inconnu vise la Montée par défaut.
- **Laissé pour plus tard** : une vingtaine de recherches par préfixe restent dans `TutoChecks` ; `writesBass` et `partLabel` existent en double ; les tests par préfixe de `TutoPlay.kt`, `TutoValidation.kt` et `TutoCourse.kt` ne sont pas convertis.

## Pas fait, exprès (consignes du chef)

- **P6, P7 et toute réécriture des textes des cours** : à l'équipe des textes (`docs/audits/ultra/tour-7-plan-p6-p7.md`).
- **Phrase Facile du chapitre 12, étape 1, et phrase Pro de la cymbale (« un seul coup »)** : à l'équipe des textes.
- **Mineurs du contrôle et note sur `AppRoot.kt` (vers la ligne 2035 : l'effet ne suit pas la partie affichée, contourné dans `TutoController`)** : tour 8, même document.

## Phrases d'acceptation de la suite (à constater par un siège neuf : « tenté », pas « corrigé »)

- Je ne vois « Montre-moi » que là où le toucher montre quelque chose.
- Après « Poser ce qui manque », le panneau me dit ce qu'il a posé.
- Police à 200 % : je lis encore la consigne sous la carte du kit ; téléphone couché, aucun bouton coupé.
- J'ai allumé les accords moi-même : ils restent allumés quand le tuto se termine.
- Un cours que j'avais fini avant la mise à jour est toujours fini.
- Je touche « Touche ici » : le rouleau bouge à chaque fois, et la note allumée est à l'écran.
- Une partie vide que j'ai nommée « Montée » ne fait pas dire « Déjà fait ».
- « Créer la partie » : la partie apparaît aussi dans le morceau ; s'il manque une partie, c'est dit au-dessus du bouton.
- Ce que j'ai fait dans une partie n'est pas redemandé en pointillé dans une autre.
- Un texte trop long pour sa place finit par « … ».

## Ce qui n'est pas vert

Vérifications de fin, sur la tête 4170af63 (intégration f8552f55 fusionnée) :
- Garde P1 (7 arrivées × 37 styles × 2 niveaux) : verte, les 7 fichiers de fautes sont vides.
- Pré-vol `--ci --rapide` : PRÊT (0 à voir).
- Rejeux : tous « oracle tenu », `t4-26-prise-clavier` compris cette fois.
- Tests : 9151 joués, 50 rouges. Les 50 sont des comparaisons de captures hors du tuto (projets, remix, partage, prise, navigation, piano roll, coquille du guide) ; aucune n'est un test de logique, aucune n'est une capture du tuto. Elles ne viennent pas de ce lot : hors intégration, le lot ne change dans l'appli que `PianoRoll.kt` (l'état « Accords » et la note allumée, pas l'ordre des boutons), `TutoController.kt` et `TutoPanel.kt`, et les images de référence sont identiques à celles de l'intégration. Exemple lu : `piano_roll.png` garde un ancien ordre des boutons (« Basse en fantôme » après « 100 % »). Non réenregistrées (hors zone) : au chef.

- P6 et P7 : non faites, à l'équipe des textes.
- Traductions des deux phrases neuves (chapitre 12, cymbale) : écrites ici, pas relues par des natifs. Plus aucun test ne garde le « 4 » et le pad Clap dans les 13 traductions du Facile (justes aujourd'hui, vérifiées à la main).
- Sept points du relecteur de P8 et deux du relecteur des phrases : rangés dans la relecture, pour le tour 8.
- M1 : environ 58 % du coût restant est dans `Project.sanitized()`, hors du lot ; `save()` toujours sur le fil principal.
- M4 : la moitié « silence » de `Tour7TutoDottedInItsPartTest` n'est pas prouvée capable de passer au rouge.
- P8 : une vingtaine de recherches par préfixe dans `TutoChecks`, les doublons `writesBass` et `partLabel`, les tests par préfixe de `TutoPlay.kt`, `TutoValidation.kt`, `TutoCourse.kt`.
- Mineurs du contrôle et note `AppRoot.kt` : tour 8.
- Tout ce lot est « tenté » : aucun siège neuf ne l'a encore constaté.
