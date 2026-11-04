# Lot ULTRA-P0-A : le banc 2 (Audit ultra §3.1, §3.3, §3.4, §4)

Équipe A de la préparation P0. Tout est côté tests et scripts : l'appli n'a pas changé.

## Ce qui est fait
- **Options de siège** dans `start.txt` (lues par `SeatOptions.kt`, une option mal écrite est signalée dans le journal en `#0 OPTION ?`, jamais bloquante).
- **L'écran en texte** : `screen-N.txt` à côté de `screen-N.png`, `LayoutScan` à chaque écran (`#N LAYOUT`).
- **Journal enrichi** et **chien de garde** du fil principal.
- **Rejeu** : `scripts/human-replay.sh`.
- **Mesures et oracle de mission** : `scripts/bench/seat-metrics.py`.
- **Fusion des rapports** : `scripts/bench/fuse-reports.py`, base `docs/audits/ultra/base.tsv`.
- **Amenées** pour les 3 missions les plus fréquentes.
- Tests : `BenchSeatOptionsTest` (siège arabe, tablette, 130 % ; siège `lit=non`), `SeatOptionsParseTest`, `BenchAmeneesTest`, `scripts/bench/test_ultra_bench.py` (dans la CI).

## Les options de `start.txt`
| Option | Effet |
|---|---|
| `lang=fr` (15 codes) | langue de l'appli et du téléphone |
| `device=phone` | `phone` (le Honor, par défaut), `phone360`, `phone411`, `phoneLand`, `tablet8`, `tablet11Land`, `foldable` (Fold déplié, 690 × 829 dp) ; tous à 1,5 px par dp |
| `font=100\|130\|200` | taille du texte d'Android |
| `mode=simple\|pro` | Mode simple ou Pro réglé avant l'ouverture |
| `theme=studio\|night\|daltonien` | thème réglé avant l'ouverture |
| `fresh=reel` | rien d'accordé, rien de réglé : la langue est celle du téléphone, l'accueil est joué, et quand l'appli demande le micro ou les notifications, le banc montre la question d'Android (en français ou en anglais) ; la réponse est donnée à l'appli comme sur un téléphone. Mode et thème sont alors ignorés (la personne les choisit) |
| `gestes.max=60` | budget : au-delà, le banc écrit « abandon : budget » et ferme. Une image regardée compte un demi-geste, « attends » ne compte pas |
| `lit=non` | textes sous 18 sp en barres grises sur l'image, « ▭▭▭ » dans le texte, noms TalkBack tus ; pas de plafond d'images |
| `main=une\|gauche` | une main (droite ou gauche) : un toucher à plus de 500 dp du coin bas du pouce est refusé, avec un message |
| `amenee=premier-morceau\|remix\|padlive` | le banc amène l'appli au départ de la mission (projet vide dans le mode du siège ; page Remix ; Pad Live prêt) |
| `mission=…` | pour l'oracle (`seat-metrics.py`), pas pour le banc |
| `timeout.minutes=20` | attente maximale d'un geste |

## Comment s'en servir
- Lancer : `scripts/human-tester.sh <dossier du tour>` (sièges `a` à `f`, chacun avec son `start.txt`).
- Jouer : `scripts/human-act.py <siège> touche X Y` répond par l'écran en texte et le son. `regarde` relit l'écran ; `regarde image` donne l'image quadrillée (au premier écran d'une route nouvelle, sinon une image pour 5 gestes). Quand le banc ferme le siège, le script dit pourquoi (`fin.txt`).
- Rejouer : `scripts/human-replay.sh <siège> <n>` → « reproduit » ou « divergé au pas k ».
- Mesurer : `scripts/bench/seat-metrics.py <siège>` → `mesures.json` et `mission=reussie|partielle|non`.
- Fusionner : `scripts/bench/fuse-reports.py <dossier du tour> [--maj-base]` → `clusters.md` (60 lignes au plus). Les rapports s'appellent `rapport*.md` ou `rapport*.txt`.

## Ce que le journal dit, écran par écran
`ROUTE` (dialogue au-dessus, sinon page et onglet, avec le mode et l'empreinte), `SON`, `LAYOUT`, `>` (le geste), `DOIGT` (ce qui est sous le doigt, « rien » ou « désactivé »), `AIDE` (un « ? »), `RETOUR`, `REFUS` (hors du pouce), `PARTAGE`, `PERMISSION`, `GEL` (avec la pile), `RIEN NE CHANGE` (3 écrans identiques ; fin à 5), `FIN`. L'état laissé (`etat/`) : projets, préférences, fichiers partagés, `resume.json`.

## Décisions (aussi dans DECISIONS.md)
- Les réglages d'écran se font par `RuntimeEnvironment.setQualifiers` et `setFontScale` avant le lancement, pas par une classe par siège : les six classes `HumanTesterA…F` restent.
- Toutes les tailles d'appareil à 1,5 px par dp (comme le Honor du banc) : des images que le testeur lit vite ; les dp sont ceux de `CaptureMatrixTest`.
- Un gel = la même méthode de l'appli en haut de la pile plus de 700 ms (pas une recomposition qui avance, pas le chargement d'une classe par Robolectric).
- Une page marquée qui couvre la moitié de l'écran ou plus cache ce qui est dessiné avant elle dans son cadre (Pad Live sur le projet) ; `LayoutScan` ne garde que ce qui est vu.
- « Rien ne change » ignore les écrans après « attends » (attendre une séparation n'est pas tourner en rond). L'empreinte compte le son : un pad qui démarre est un changement.
- Sur un siège, les pads du Pad Live « sonnent » : le banc tient l'état des pads à la place du moteur (une boucle jusqu'au toucher suivant, un coup une fois).

## Tour 1 (équipe du banc, `docs/audits/ultra/tour-1-banc.md`)
- Les amenées partent de l'accueil ou de « Que veux-tu faire ? » (Mode Pro) et font défiler jusqu'à la cible ; `BenchAmeneesProfilesTest` les joue sur 10 profils.
- Un micro : `BenchMic` (voix de test au rythme du banc) pour les pads et les notes chantées, le moteur du banc pour les pistes audio (décompte, puis la voix).
- Le moteur du banc vient aussi pour « Exporter » (Mix), « Créer la vidéo », « Écouter » d'une performance, « Montre-moi » ; la ligne de son dit « Écouter le bout » et les pads joués par le moteur ; mesures en direct pendant la lecture (−14 LUFS fixes).
- « regarde » écrit comme geste n'est qu'un regard ; « Rien ne change » ne compte pas pendant un long travail.
- Oracles `tuto-facile`, `tuto-pro`, `synthe`, `libre` ; `defi` partiel quand un défi est ouvert sans étoile.

## Restes
- Questions de permission traduites en français et en anglais seulement (les autres langues voient l'anglais).
- Oracle « reprendre 3 jours après » à écrire (le banc n'avance pas encore l'horloge) ; « exports » compte les fichiers partagés, sans `ffprobe` ni mesure des cibles.
- Les pads du Pad Live continuent de « sonner » sur le banc quand on quitte Pad Live (le banc ne sait pas ce que le moteur ferait) ; les pads de batterie ne s'entendent toujours pas.
- `base.tsv` est vide : à remplir avec les clés des tours 0 à 5 avant le tour 1.
- Le test qui refuse un tour où tous les sièges ont la même langue n'est pas écrit (il va avec le tableau de bord).
- Le siège TalkBack (gestes `suivant / précédent / activer`) n'est pas fait : l'écran en texte en est la base.
