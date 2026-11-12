Relecture du lot « niveaux » (tour 6, zone niveaux) par un relecteur neuf
Relu : a8efc3f53ca38aa439249d0a7dc4d9f4b6a86736

Portée : `git diff origin/ccr-80af1a1b-mn0wec HEAD` (6 fichiers, uniquement des documents ; aucun code, aucun script). Lu d'abord sur `aee38a7f`, puis corrigé par un commit du relecteur (`a8efc3f5`, 4 fichiers du lot).

## 1. Les chiffres du § 1 : recomptés, exacts
`sieges/tour-3` à `tour-6`, `sieges.tsv`, colonnes 6 et 8, commentaires et en-tête retirés :
- tour 3 : 24 sièges, 10 simple, 8 pro, 6 « - », 6 `libre` ;
- tour 4 : 24, 10, 10, 4, 10 `libre` ;
- tour 5 : 24, 9, 10, 5, 6 `libre` ;
- tour 6 : 8, 5, 1, 2, 3 `libre`.
Les quatre lignes du tableau sont justes.

## 2. Ce qui est dit du banc : vérifié
- `PLAN-TOUR-6.md` § 4, l. 72 : « A fini » n'est pas « a réussi » : la citation existe. § 2 point 7 : la CI demande bien la construction et la fumée sur émulateur. § 4 s'appelle « Les huit sièges ».
- `PLAN-RESTE.md` § 6, l. 141 : P2 (horloge) au tour 7 : exact.
- Consignes v2 (`GABARIT-CONSIGNES-TESTEUR-V2.md`) : faux moteur, pas d'horloge, aucun geste du téléphone, sélecteur limité à une chanson, un .zip et un .mid : exact.
- `scripts/live-bench.sh`, `scripts/real-projects.sh`, `scripts/human-act.py`, `sieges/tour-6/sieges-proposes.tsv`, `mesures.json` (écrit par `seat-metrics.py`), le workflow « Nightly real use », l'étage 3 « chaos » de `AUDIT.md`, « Questions à l'oreille » (`docs/decisions/tour-5-oreille.md`) : tous existent.
- L'oracle de `libre` (`seat-metrics.py`, `free`) : un son entendu, plus un changement gardé ou un fichier partagé : exact.
- Dans l'appli : projet vide, courbes, export MIDI et pistes séparées existent (recherche dans le code, pas d'essai).

## 3. Le tour 6 ne change pas : vérifié
- Aucun script ne lit `CLAUDE.md`, `AUDIT.md` ni `PLAN-NIVEAUX.md`. `PLAN-RESTE.md` est lu par `defauts-caches.py` (seulement les lignes de tableau « | X.n | » : la ligne ajoutée est une puce) et nommé par `test_socle.py` (règle de fusion, sans lecture du contenu). `verifie-chantier.py` et `pre-vol.sh` ne lisent aucun des fichiers modifiés, sauf le rapport et la fiche du lot.
- Principe 6 (gel) : le lot du banc est après le tour 6 et ne touche pas l'appli. Règle de sortie de `PLAN-TOUR-6.md` § 1 : non modifiée, et redite dans `PLAN-NIVEAUX.md`.

## 4. Cohérence interne
16 lignes de sièges (3 + 4 + 3 + 2 + 3 + 1) ; 8 missions neuves, les mêmes au § 2 et au § 3 ; version courte du § 7 : 6 sièges, 4 missions neuves, conforme au tableau.

## 5. Règles du portail
Décisions dans `docs/decisions/tour-6-niveaux.md` seulement, `docs/DECISIONS.md` intact, aucun lien symbolique, rien sous `docs/audits/ultra/portail/`.

## Constats
1. **Grave**, **corrigé** (`a8efc3f5`) : `PLAN-NIVEAUX.md` § 1 disait « les douze missions du banc (`fabrique-sieges.py`, `GOALS`) ». `GOALS` en a onze : `reprendre` n'y est pas. Elle n'existe que dans `seat-metrics.py` (`MISSIONS`), où son oracle `resume_later` répond toujours « non » (« oracle à écrire »). Le siège N1 d la recevait comme une mission qui marche. Le § 1 dit maintenant où sont les douze, que `reprendre` n'a ni buts ni oracle, et qu'elle n'a servi qu'aux tours 3 et 4 ; N1 d et le § 6 portent « oracle à écrire ».
2. **Grave**, **corrigé** (`a8efc3f5`) : deux sièges à mission existante avaient un « Réussi si » que l'oracle actuel ne lit pas : `padlive` (N4 b, niveau pro) réussit sur « premier son en 1 geste », `voix` (N1 b) sur « une prise sur une piste ». Le § 3 ne prévoyait d'oracle que pour les missions neuves : un siège de niveau 4 aurait réussi comme un débutant, ce que la règle interdit. Ajout au § 3 et à `PLAN-RESTE.md` § 6 : trois oracles existants à écrire ou à durcir (`reprendre`, `voix`, `padlive`) ; les « Réussi si » de N0 b, N0 c et X se lisent dans les mesures et les fiches.
3. **Grave**, **corrigé** (`a8efc3f5`) : phrase plus forte que la preuve. « La prise est dans le fichier exporté » (N1 b) et « la courbe se retrouve dans l'export » (N2 c) supposent de lire le son d'un export, alors que le banc a un faux moteur (consignes v2, familles `son` et `chiffres` ; `fuse-reports.py` l. 51 : taille et durée d'un export sont celles du banc). Les deux « Réussi si » lisent maintenant le projet sauvé, et le § 3 dit la limite : le contenu sonore d'un export relève du rapport d'écoute au vrai moteur (§ 6).
4. **Grave**, **corrigé** (`a8efc3f5`) : le principe 9 de `AUDIT.md` et la ligne de `CLAUDE.md` disaient « tout tour de testeurs », sans l'exception écrite dans `PLAN-NIVEAUX.md` et dans la décision (tour de contrôle ciblé). Tel quel, le principe 9 contredisait le principe 7 (« on ne relance que ce que les corrections ont touché »). L'exception est maintenant dans les deux textes.
5. **Moyen** : « 1A » désigne ici la réponse de Benjamin sur « Nightly real use », alors que `PLAN-TOUR-6.md` § 11 a déjà une réponse « 1A » (les deux sièges œil neuf). Le lot le rattache bien à sa discussion, mais le raccord prévu à `PLAN-TOUR-6.md` devra lui donner un autre nom.
6. **Moyen** : § 7 « après le tour 6, quand sa règle de sortie est tenue » : or cette règle de sortie dit que le grand audit est alors fini, et `PLAN-TOUR-6.md` § 9 dit « pas de tour 7 automatique ». Le lot ne lance rien et ne change pas la règle de sortie, mais il ne dit pas qui décide du tour suivant.
7. **Moyen** : faisabilité non prouvée de `beat-sample` (un sample découpé sur les pads hors de la page Remix) et de `monstre` (8 pistes, 24 parties : limites de l'appli non vérifiées par le lot). À trancher par le lot du banc, test rouge d'abord.
8. **Mineur** : N0 b compte comme siège de niveau 0 avec la mission `libre`, qui « ne compte pour aucun niveau ». Sans effet : N0 a et N0 c couvrent le niveau.
9. **Mineur** : « à peu près le tour 5 en gestes » (§ 7) est une estimation, dite telle. La décision 2 parle de « ces deux fichiers » alors que le lot en a six (état au moment de la décision).
10. **Mineur** : « des buts de débutant » était un jugement (`synthe`, `tuto-pro`, `exports` ne sont pas des buts de premier jour) ; la phrase a été remplacée par ce que les oracles lisent, dans la correction du constat 1.

## Non vérifié
- Les rapports des sièges des tours 3 à 6 (le lot dit lui-même ne pas les avoir relus).
- Que l'export MIDI et l'export des pistes séparées rendent sous le banc un fichier lisible par un oracle : aucun essai, c'est le travail du lot du banc.
- Le commit `47fe20d` existe dans le dépôt ; son contenu n'a pas été relu.

Verdict : 0 bloquant, 0 grave
