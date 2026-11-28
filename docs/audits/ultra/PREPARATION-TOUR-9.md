# Tour 9 : vérification avant départ (2026-11-19)

Demande de Benjamin : « Pour le tour 9, vérifie que tout est optimisé. » Trois lecteurs neufs ont regardé le coût, la méthode et la CI ; la préparation de machine a été jouée pour de vrai. Base lue : intégration `26e315e22` (tour 8 clôturé). Le chef lit cette page avant d'écrire le plan du tour 9.

## 1. Le tour 8, enfin mesuré

| | Tour 6 | Tour 8 |
|---|---|---|
| Unités dépensées par les discussions | environ 1 100 M | **88 M** (25 discussions, 245 $) |
| Bugs constatés corrigés | une cinquantaine | 9 |
| Unités par bug (sans le chef) | 22 M | **environ 10 M** |

- Cible de `METHODE-V4.md` : 9 à 12 M par bug. Elle est atteinte, sans compter le chef.
- Testeurs : 13 discussions, 29,6 M (médiane 2,3 M). Correcteurs : 8 discussions, 33,5 M (médiane 4,9 M). Banc : 4 discussions, 10,7 M.
- La plus chère : « T0b tuto-gestes », 14,4 M (trois fois la médiane d'un correcteur) : relue quatre fois, sièges rejoués deux fois.
- Toutes les discussions ont fini sous 144 k de mémoire : le compactage automatique tient.
- **Le chef n'est pas mesuré par tour.** Son compteur depuis octobre : 4 271 M d'unités au 19 novembre, 06 h 20 UTC. Ce chiffre sert de point zéro pour le tour 9.
- Fuite probable : dans 8 discussions, plus d'un tiers de la dépense est de la mémoire réécrite (lot G 67 %, paquet 4 52 %, lot S 45 %). Ce sont des discussions qui ont attendu leur CI et se sont réveillées à froid ; la part exacte des réveils n'est pas mesurée.

## 2. Ce qui est déjà bon (ne pas y toucher)

- Mémoire courte : un lot = une discussion, compactage à 144 k.
- Paquets de trois lots au plus, rien n'entre sur du rouge, « corrigé » seulement après un siège.
- Faux « introuvable » de Maven : réglé par la copie Google (`settings.gradle.kts`).
- Rythme de croisière : un réveil unique par CI, jamais d'heure creuse.

## 3. Ce qui a été changé le 19 novembre

1. **Machine prête à l'arrivée.** Un script de préparation de l'environnement « Parpocket-studio » installe le SDK Android et remplit le cache des tests (approuvé par Benjamin le 19 novembre). Mesuré ici en partant de zéro : voir §7. Avant, chaque discussion le faisait elle-même, ou travaillait sans test.
2. **Fiches de reprise raccourcies.** `EN-COURS.md` : de 13 000 à 4 000 signes ; `PROGRESS.md` : de 66 000 à 1 500. L'histoire est dans `docs/archive/`.
3. **Lecture d'arrivée.** Une discussion à fiche lit `CLAUDE.md`, `EN-COURS.md` et sa fiche, rien d'autre. `PROGRESS.md`, les issues et le test de nuit sont pour le chef.
4. **Une seule CI par paquet.** La CI de la branche du paquet (intégration + trois lots au plus) vaut CI de chaque lot. Avant : une CI par lot, puis une CI du paquet (57 minutes deux fois de suite).
5. **Pas de CI pour du texte seul.** Une CI verte sur un commit parent compte si l'écart ne touche que `docs/`. Au tour 8, 8 CI sur 45 ont rejoué 58 minutes pour un rapport de relecture.
6. **Un lot n'attend pas sa CI.** Il pousse, écrit « fini » et se tait : aucun réveil, aucune minuterie. Le chef lit la CI et ne réveille le lot que si elle est rouge, avec l'extrait de l'artefact.
7. **Un rouge voulu ne passe pas par la CI.** Le commit « rouge: » se prouve par son test ciblé, sur la machine du lot (une CI entière y est passée au tour 8).

## 4. Ce qui reste à optimiser : le lot « machinerie » (avant tout siège)

Un lot du banc, une discussion neuve en Opus, fichiers : `scripts/bench/ci-etat.sh`, `scripts/bench/pre-vol.sh` (texte), `.github/workflows/ci.yml`, leurs tests. Il remplace le lot G : la branche `eq/tour-8-guetteur` ne contient qu'un essai sur un test du moteur, rien sur la CI ; elle est à laisser.

1. `ci-etat.sh` ne répond jamais (l'outil `gh` n'est pas connecté) : la garde du pré-vol lit toujours « absent ». Le faire lire l'état d'une CI sans `gh`. Attention : l'adresse des machines est partagée, la limite sans compte est basse.
2. Y inscrire la règle 5 (CI verte d'un parent, écart dans `docs/` seulement).
3. Un script d'attente : il suit une CI jusqu'à sa fin et rend la main ; le chef le lance en arrière-plan à la place du réveil de 50 minutes.
4. `ci.yml` : durée maximale sur les trois tâches qui n'en ont pas, ménage du disque avant la grande étape (une CI est morte disque plein).

Puis, en lots séparés, dans cet ordre :

- **CI en deux moitiés** : l'étape « tests et captures » dure 51 minutes sur 58 ; la couper sur deux machines rend 20 à 25 minutes par CI. Une demi-journée de travail, équilibre à mesurer.
- **Test capricieux `Coquille2SeatTest.p5`** : l'attente s'arrête dès que « Chargement » n'est plus à l'écran, même si la liste n'est pas là. Remède : attendre la ligne voulue, jusqu'à 20 secondes ; rouge prouvé en retardant la liste de 600 ms (`ProjectsViewModel.kt:61`).
- **Captures qui bougent** (4 à 6 par CI) : cause non vérifiée, sans doute un vumètre ou une tête de lecture pris au vol. Les figer dans l'appli de test, réenregistrer une fois.
- **Banc** : `gradle-file.sh` arrête tout avant et après chaque appel (départ à froid à chaque fois) ; `selftest.sh` lance trois Gradle ; `pret.sh` en enchaîne quatre. Gain estimé 1 à 3 minutes par appel, non mesuré : à mesurer avant de changer.
- **Registre** : `corrections.tsv` n'a pas reçu les verdicts T-a à T-d (17 « pas pris », 23 « tentés » jamais confirmés, 12 colonnes annoncées pour 9). Le portail doit l'écrire lui-même ; deux « tentés » sur le même défaut = alerte « on tourne en rond ».

## 5. Le tour 9 lui-même : ordre conseillé

1. Ranger : les 9 branches de sièges du tour 8 pas entrées (`aud/tour-8-ctrl-t0b-*`, `aud/tour-8-ctrl-t0c-*`, `eq/tour-8-controle-t0a` : rapports seuls), la revue de fin de phase du tour 8, puis la PR du tour 8 vers `main` (185 commits d'avance).
2. Lot « machinerie » (§4), puis la CI en deux moitiés.
3. **Élargir d'abord** : les sièges jamais joués (E-a à E-d, C-a, C-b, P-a à P-d, les 4 pilotes des niveaux). Le tour 8 a joué 14 sièges de contrôle et aucun siège neuf. Un siège par niveau (curieux, débutant, moyen, confirmé, pro).
4. Trier, puis réparer par zone, une zone = un lot :
   - **tuto, maracas** : les 2 graves de T-c (« touche 4 cases » validée à 1 sur 4 ; carte « rien à toucher » alors qu'il manque 3 maracas). Même famille de code (`TutoKind.kt`, suivi des changements de batterie) : un seul lot.
   - **tuto, consigne dépliée** : P6-grille (japonais, pliable, 200 %).
   - **piano roll** : il défile sous le doigt, deux Fa au même nom, vélocité validée sans vérification (T-d).
   - **finitions de texte** à 130 % et 200 %, et le lot de langues ar / zh-CN / nl.
   - **tests creux** : `Tour8PlusRowsFitTest:65`, `Tour8GuideNoEmptyStepTest:37`.
5. Un contrôle par lot du tuto, une seule fois : un siège court qui démarre au chapitre visé (`amenee=tuto:<style>:<chapitre>`), pas un tuto entier.

## 6. Règles de dépense du tour 9

- Avant le départ, le chef note son compteur ; à la clôture, il le renote. Coût du tour = discussions + écart du chef.
- Toute discussion du tour porte l'étiquette `tour-9` (au tour 8, la plus chère n'en avait pas).
- Une relecture neuve par lot, pas quatre. Un défaut tenté deux fois sans « corrigé » : le dire à Benjamin, proposer Fable sur une discussion neuve.
- Une fiche se réécrit, elle ne se rapièce pas avec un bloc « CORRECTIONS » qui contredit le dessus.
- Opus partout. Le lot G du tour 8 avait tourné sur Fable sans demande.

## 7. Préparation de machine : mesure

Jouée deux fois sur une machine vidée (SDK, cache et dossiers de compilation effacés) : 5 min 40 du début à la fin (SDK 24 s, premier test `BenchSelfTest` 5 min 15), puis un test ciblé repart en 35 secondes. 2,9 Go de SDK, 1,6 Go de cache. Prix : environ 6 minutes au démarrage de chaque discussion neuve de l'environnement. Le script ne contient aucun secret et finit toujours sans erreur (une discussion démarre même si le téléchargement échoue). Décision : `docs/DECISIONS.md`, 2026-11-19.

## Ce qui n'est pas vert

- Le tour 8 n'est pas dans `main` et sa revue de fin de phase n'est pas faite.
- 2 graves du tuto (maracas) et P6-grille sont connus et pas corrigés.
- `Coquille2SeatTest.p5` capricieux ; 4 à 6 captures bougent d'une CI à l'autre.
- La CI dure 58 minutes ; rien n'y a été changé aujourd'hui.
- `ci-etat.sh` ne marche pas ; les règles 4 à 7 du §3 sont écrites mais aucun script ne les fait encore respecter.
- Le script de préparation est posé, mais il n'a pas encore été vu tourner au démarrage d'une vraie discussion neuve : la première discussion du tour 9 le prouve (`ls /opt/android-sdk/platforms`).
- `test_constate.py` lancé par pytest donne 1 faux rouge (`ControlSeatsInTheRound`) ; lancé comme la CI le lance (unittest), 64 verts. À ne lancer que comme la CI.
- Sur une machine du cloud (historique git coupé à 50 commits), `pre-vol.sh --ci --rapide` est recalé par `test_portail4.py` (`RealLots`, lots du tour 6) : le test trouve la fusion mais pas son point de départ, et rougit au lieu de passer son tour. Déjà vrai avant cette vérification ; vert sur la CI (historique entier). Pour le lot « machinerie » : faire passer son tour au test quand le point de départ manque.
- Le coût du chef pendant le tour 8 n'est pas connu.
- Gains du §4 « banc » : estimés, pas mesurés.
