# Relecture neuve, tour 9, lot L12
Relu : a02bcd3361664cdfa8f3b2cdccc6beb9ce3e21ad

Relecteur : sous-agent neuf (n'a pas écrit le code). Portée : `git diff origin/ccr-80af1a1b-mn0wec...HEAD` (DoneBar.kt, une ligne d'AppRoot.kt, trois classes de test, rapport, décisions).

## F111 : le message « Fait : … · Annuler » part au geste suivant

Vérifié sans défaut :
- « Annuler » annule toujours : le clic du bouton part dans la passe Main du lever, la passe Final de l'enveloppe vient après sur tout l'arbre ; `undoDone` remet le message à null, donc l'enveloppe ne rappelle même pas `dismissDone`. Garde verte : `Tour9DoneBarGoesTest#itsAnnulerStillUndoes` (la longueur revient).
- Un message dit par le geste même reste : le texte du début du geste est comparé à celui du lever, lu sur le StateFlow (pas sur l'état composé). Garde : `DoneBarNextTouchTest#theMessageThisTouchSaysStays` (et 0 appel à dismiss).
- Aucun geste mangé ni bloqué : `awaitFirstDown(requireUnconsumed = false, Final)` puis `awaitPointerEvent(Final)`, rien n'est consommé ; la fiche ne prend la main sur aucun enfant. Les voisins qui touchent la ligne restent verts (`Zone3Round2Test`, `Tour6RecordStopStaysTest`, `Tour6GuideCoversTest`, `DoneBarTouchThroughTest`).
- Pas de fuite ni de plantage : la boucle vit dans le `pointerInput` du Scaffold, annulée avec lui ; clés `done` (même StateFlow) et `rhythmVm::dismissDone` (références liées égales entre deux compositions) : pas de relance à chaque recomposition. Multi-doigts : le message part au lever du dernier doigt.
- Un seul `DoneBar` dans l'appli (AppRoot), donc tous les « Fait : … » suivent la même règle ; les 6 s restent pour qui ne touche rien (`withoutATouchItGoesAfterAFewSeconds`).
- Tests qui prouvent : les deux tests de geste rougissent sans la correction (rejoué ci-dessous) ; `DoneBarNextTouchTest` isole la règle (touche, glissé, message du geste, rien à laisser partir).
- Rapport : « tenté » et non « corrigé », chemins `chemin: … | test: Classe#méthode` sur des `@Test` présents, ligne `tests:`, `corrige: F111 | gravité: mineur`, limite (feuille ou dialogue = autre fenêtre) écrite. Décisions dans `docs/decisions/tour-9-L12.md`, pas dans `docs/DECISIONS.md`.

Mineurs (ne bloquent pas) :
1. mineur, DoneBar.kt `letDoneGoAtNextTouch` : si un geste sur la page redit exactement le même texte que le message affiché (même action refaite au même endroit), `done.value == shown` au lever et le message repart aussitôt ; avant, il restait (le StateFlow ne réémet pas un texte égal). Rare (les actions qui disent « Fait : nouvelle longueur … » passent par la feuille de partie, une autre fenêtre) ; parade possible : un compteur de message plutôt que le texte.
2. mineur, DoneBar.kt : un appui sur la carte du message (hors « Annuler ») la laisse partir aussi, puisque la carte laisse passer le doigt à la page. Cohérent avec « au geste suivant », à garder en tête si un siège veut relire le message en le touchant.
3. mineur, Tour9DoneBarGoesTest#itsAnnulerStillUndoes ne départage pas l'ordre clic / passe Final (`undoDone` met null de toute façon) : il prouve que l'annulation marche, pas l'ordre. Suffisant pour la phrase d'acceptation.

## F101 : non reproduit, « ne corrige pas »

`Tour9PartsListScrollsTest` (trois réglages, dont l'endroit du siège t9-53) garde « la liste défile d'où que parte le doigt » ; la contre-épreuve décrite au rapport (mutation de SessionScreen.kt, trois rouges) n'a pas été rejouée ici. La raison (glissés partis du panneau replié du tuto) est écrite au rapport avec la note pour TutoPanel.kt, hors zone. SessionScreen.kt n'est pas modifié.

## Ce que j'ai relancé et vu

- Sur HEAD a02bcd336, avec `--rerun` (une première passe sortait du cache de Gradle, non comptée) : `Tour9DoneBarGoesTest` 4/4 verts, `Tour9PartsListScrollsTest` 3/3 verts, `DoneBarNextTouchTest` 4/4 verts ; voisins `Zone3Round2Test` 11/11, `Tour6GuideCoversTest` 10/10, `Tour6RecordStopStaysTest` 4/4, `DoneBarTouchThroughTest` 2/2 verts. Seul sauté : `drive`, le pilote du banc hérité de HumanTester (« only with -Phuman.dir »), comme partout.
- Rouge rejoué sur le parent du vert, dans un dossier de travail temporaire (git worktree, supprimé ensuite) : vert e7ee3e6e926800147c384c1b92e63b7415f64905, parent 6092ce42ea2148de0739641ff25762ee6e78a22d. `Tour9DoneBarGoesTest` : 2 rouges, `frenchTheNextTouchLetsTheMessageGo` (« « Fait : … Annuler » still shown after the next touch ») et `polishTheNextSwipeLetsTheMessageGo` (« « Gotowe: nowa długość … Anuluj » still shown after a swipe ») ; `withoutATouchItGoesAfterAFewSeconds` et `itsAnnulerStillUndoes` verts. Aucune preuve `rouges-constates/` n'existe pour ce lot.
- Captures : aucun écran ne change de forme (le message part plus tôt, rien de neuf n'est dessiné) ; pas de capture à regarder.

Verdict : 0 bloquant, 0 grave
