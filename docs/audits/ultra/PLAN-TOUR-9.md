# Plan du tour 9 : chaque tuto joué en entier

Décision de Benjamin (19 novembre) : les 37 tutos sont joués **en entier**, une fois, par des sièges testeurs, avant toute réparation. Les langues tournent sur les 15 langues de l'appli. Fiche de chaque siège : `docs/audits/ultra/tour-9-fiches/SIEGE.md` (le chef n'y colle que l'id).

Bilan : **76 sièges, 10 vagues, banc non modifié** (selftest inchangé).

## 1. Ce que le banc sait faire (vérifié dans le code du banc, sans le changer)

- `amenee=tuto:<style>:<N>` est générique : tout style qui a `tutos/<style>.json` est accepté, et le banc construit d'avance les chapitres sautés. Le mode choisit le niveau du cours : `mode=simple` donne Facile, `mode=pro` donne Pro. Un chapitre au-delà du cours est refusé au départ (`tutoAmeneeError`).
- Limite de gestes : `gestes.max` n'a pas de plafond dans le banc. La vraie limite est la mémoire du sous-agent testeur : au tour 8, les sièges ont tenu de 80 à 150 gestes. Ici : 150 à 200.
- `timeout.minutes` est l'attente d'**un** geste, pas la durée du siège : 30 suffit.
- Un dossier de siège n'a qu'un niveau sous `controle/` (leçon du tour 8) : `docs/audits/ultra/sieges/tour-9/controle/t9-NN/a/start.txt`. Les 76 start.txt sont déjà écrits dans cette branche.
- `mission`, `profil` et `niveau` sont ignorés par le banc (lus par `seat-metrics.py`).

## 2. Combien de gestes coûte un tuto entier

Mesures dans les journaux du tour 8 (11 sièges de tuto) : de 2,4 à 13 gestes par étape, médiane autour de 6. Les chapitres où l'on pose des notes coûtent le plus.

- Tuto médian : 37 étapes (Facile, 13 chapitres) ou 41 étapes (Pro, 14 chapitres), soit environ **220 à 300 gestes**. Il ne tient pas dans un siège.
- Découpage : `ceil(étapes / 24)` sièges par tuto, coupés à la frontière d'un chapitre, au plus près de parts égales. Résultat : 35 tutos en 2 sièges, 2 tutos en 3 sièges (12 à 26 étapes par siège).
- Budget : `gestes.max = 7 × étapes + 20`, borné entre 150 et 200.
- Chaque siège après le premier est amené au premier chapitre de sa tranche ; le premier est amené au chapitre 1 (pas de gestes perdus dans l'assistant).

## 3. Répartition

- Niveau : 22 tutos en Facile (`mode=simple`, profils curieux, débutant motivé, moyen) et 15 en Pro (`mode=pro`, profils confirmé, pro). Tous les sièges d'un tuto gardent la même langue, le même mode et le même profil ; l'appareil (phone, phone360, foldable, tablet8) et la police (100, 130, 200) tournent d'un siège à l'autre.
- Profils (sièges) : curieux 16, débutant motivé 14, moyen 14, confirmé 17, pro 15.
- Langues (tutos) : ar, ja, ko, zh-CN : 3 tutos chacune ; fr, en, nl : 3 ; es, pt-BR, de, it, pl, tr, id, hi : 2.
- Appareils : 19 sièges chacun. Polices : 100 ×25, 130 ×26, 200 ×25.

## 4. Ce que l'oracle lit pour « tranche finie »

Le banc écrit l'état du tuto sur chaque ligne `ROUTE` de `journal.txt` : `tuto=<style>/<Facile|Pro> active=… step=<id> done=<n> chapters=<1,2,3>` (`chapters=` : les chapitres dont toutes les étapes sont réglées, selon la règle de l'appli ; `chapters=-` quand aucun).

- Tranche finie (toutes les tranches, depuis la réparation du 19 novembre) : une ligne `ROUTE` avec le bon `tuto=<style>/<niveau>` dont `chapters=` contient **tous les chapitres de la tranche**. Pour la dernière tranche, c'est aussi le « Bravo » du tuto : l'amenée coche maintenant les chapitres sautés.
- Pourquoi plus `step=` (12 premiers sièges, tous « non ») : une étape cochée reste affichée avec son « Bravo » jusqu'à « Chapitre suivant », un geste que la mission ne demande pas ; `step=<première étape du chapitre suivant>` n'arrivait donc jamais. La colonne « fin lue par l'oracle » du §6 reste vraie si le testeur fait ce geste, mais elle ne juge plus. `done=` égal au total ne juge plus non plus (en Pro, un exemple non écouté ne retient pas le « Bravo »).
- Chapitre par chapitre : le rapport du siège donne une ligne par chapitre (compris du premier coup / hésitation / bloqué) ; un chapitre « bloqué » arrête la lecture de l'oracle à ce chapitre.
- `seat-metrics.py` (oracle `tuto`) compte toujours « réussi à 3 chapitres » : il sert de garde-fou, pas de verdict. Le verdict de ce tour est le champ `chapters=` ci-dessus.

## 5. Ordre des vagues

Une vague = des tutos entiers (toutes leurs tranches dans la même vague, puisque l'amenée rend les tranches indépendantes), 6 à 8 sièges. Les langues dures sont réparties sur toutes les vagues.

| vague | sièges | tutos | langues | ids |
|---|---|---|---|---|
| 1 | 8 | acid_house, acid_techno, acidcore, afro_house | ar, ja, ko, zh-CN | t9-01 à t9-08 |
| 2 | 8 | afrobeats, amapiano, boom_bap, deep_house | en, es, fr, nl | t9-09 à t9-16 |
| 3 | 8 | detroit, disco, dnb, drill | de, it, pl, pt-BR | t9-17 à t9-24 |
| 4 | 8 | dubstep, gabber, grime, hard_techno | ar, hi, id, tr | t9-25 à t9-32 |
| 5 | 8 | house, industrial_techno, jersey_club, jump_up | fr, ja, ko, zh-CN | t9-33 à t9-40 |
| 6 | 7 | jungle, liquid_dnb, lofi | en, es, nl | t9-41 à t9-47 |
| 7 | 8 | minimal, neurofunk, phonk, pop | de, it, pl, pt-BR | t9-48 à t9-55 |
| 8 | 8 | reggaeton, retro_techno, riddim, speed_garage | ar, hi, id, tr | t9-56 à t9-63 |
| 9 | 7 | synthwave, tech_house, techno | ja, ko, zh-CN | t9-64 à t9-70 |
| 10 | 6 | trance, trap, uk_garage | en, fr, nl | t9-71 à t9-76 |

Première vague : t9-02 et t9-04 sont les premières amenées hors house et techno (voir §7) ; s'ils échouent au départ, arrêter la vague et ouvrir un lot de banc.

## 6. Les sièges

| siège | vague | tuto | niveau | chapitres (sur) | étapes | langue | appareil | police | profil | gestes.max | fin lue par l'oracle |
|---|---|---|---|---|---|---|---|---|---|---|---|
| t9-01 | 1 | acid_house | Facile | 1-3 (9) | 12 | ar | phone360 | 130 | curieux | 150 | `step=groove_name` |
| t9-02 | 1 | acid_house | Facile | 4-9 (9) | 12 | ar | foldable | 200 | curieux | 150 | Bravo (done=24) |
| t9-03 | 1 | acid_techno | Pro | 1-5 (12) | 19 | ja | tablet8 | 100 | confirmé | 153 | `step=make_intro` |
| t9-04 | 1 | acid_techno | Pro | 6-12 (12) | 19 | ja | phone | 130 | confirmé | 153 | Bravo (done=38) |
| t9-05 | 1 | acidcore | Facile | 1-5 (11) | 17 | ko | phone360 | 200 | débutant motivé | 150 | `step=make_intro` |
| t9-06 | 1 | acidcore | Facile | 6-11 (11) | 14 | ko | foldable | 100 | débutant motivé | 150 | Bravo (done=31) |
| t9-07 | 1 | afro_house | Pro | 1-5 (14) | 19 | zh-CN | tablet8 | 130 | pro | 153 | `step=groove_hear` |
| t9-08 | 1 | afro_house | Pro | 6-14 (14) | 20 | zh-CN | phone | 200 | pro | 160 | Bravo (done=39) |
| t9-09 | 2 | afrobeats | Facile | 1-8 (15) | 20 | fr | phone360 | 100 | moyen | 160 | `step=brick` |
| t9-10 | 2 | afrobeats | Facile | 9-15 (15) | 19 | fr | foldable | 130 | moyen | 153 | Bravo (done=39) |
| t9-11 | 2 | amapiano | Facile | 1-5 (12) | 18 | en | tablet8 | 200 | curieux | 150 | `step=groove_hear` |
| t9-12 | 2 | amapiano | Facile | 6-12 (12) | 16 | en | phone | 100 | curieux | 150 | Bravo (done=34) |
| t9-13 | 2 | boom_bap | Pro | 1-4 (14) | 17 | nl | phone360 | 130 | confirmé | 150 | `step=lead_do` |
| t9-14 | 2 | boom_bap | Pro | 5-14 (14) | 19 | nl | foldable | 200 | confirmé | 153 | Bravo (done=36) |
| t9-15 | 2 | deep_house | Facile | 1-6 (13) | 19 | es | tablet8 | 100 | débutant motivé | 153 | `step=map_intro` |
| t9-16 | 2 | deep_house | Facile | 7-13 (13) | 18 | es | phone | 130 | débutant motivé | 150 | Bravo (done=37) |
| t9-17 | 3 | detroit | Pro | 1-7 (14) | 23 | pt-BR | phone360 | 200 | pro | 181 | `step=make_intro` |
| t9-18 | 3 | detroit | Pro | 8-14 (14) | 18 | pt-BR | foldable | 100 | pro | 150 | Bravo (done=41) |
| t9-19 | 3 | disco | Facile | 1-4 (13) | 18 | de | tablet8 | 130 | moyen | 150 | `step=lead_do` |
| t9-20 | 3 | disco | Facile | 5-13 (13) | 18 | de | phone | 200 | moyen | 150 | Bravo (done=36) |
| t9-21 | 3 | dnb | Facile | 1-8 (15) | 21 | it | phone360 | 100 | curieux | 167 | `step=brick` |
| t9-22 | 3 | dnb | Facile | 9-15 (15) | 21 | it | foldable | 130 | curieux | 167 | Bravo (done=42) |
| t9-23 | 3 | drill | Pro | 1-8 (16) | 20 | pl | tablet8 | 200 | confirmé | 160 | `step=brick` |
| t9-24 | 3 | drill | Pro | 9-16 (16) | 20 | pl | phone | 100 | confirmé | 160 | Bravo (done=40) |
| t9-25 | 4 | dubstep | Facile | 1-8 (15) | 22 | tr | phone360 | 130 | débutant motivé | 174 | `step=brick` |
| t9-26 | 4 | dubstep | Facile | 9-15 (15) | 21 | tr | foldable | 200 | débutant motivé | 167 | Bravo (done=43) |
| t9-27 | 4 | gabber | Pro | 1-7 (14) | 22 | id | tablet8 | 100 | pro | 174 | `step=make_intro` |
| t9-28 | 4 | gabber | Pro | 8-14 (14) | 18 | id | phone | 130 | pro | 150 | Bravo (done=40) |
| t9-29 | 4 | grime | Facile | 1-5 (13) | 16 | hi | phone360 | 200 | moyen | 150 | `step=groove_hear` |
| t9-30 | 4 | grime | Facile | 6-13 (13) | 17 | hi | foldable | 100 | moyen | 150 | Bravo (done=33) |
| t9-31 | 4 | hard_techno | Facile | 1-7 (13) | 23 | ar | tablet8 | 130 | curieux | 181 | `step=make_intro` |
| t9-32 | 4 | hard_techno | Facile | 8-13 (13) | 16 | ar | phone | 200 | curieux | 150 | Bravo (done=39) |
| t9-33 | 5 | house | Pro | 1-9 (17) | 22 | ja | phone360 | 100 | confirmé | 174 | `step=map_intro` |
| t9-34 | 5 | house | Pro | 10-17 (17) | 23 | ja | foldable | 130 | confirmé | 181 | Bravo (done=45) |
| t9-35 | 5 | industrial_techno | Facile | 1-6 (13) | 17 | ko | tablet8 | 200 | débutant motivé | 150 | `step=brick` |
| t9-36 | 5 | industrial_techno | Facile | 7-13 (13) | 19 | ko | phone | 100 | débutant motivé | 153 | Bravo (done=36) |
| t9-37 | 5 | jersey_club | Pro | 1-7 (15) | 16 | zh-CN | phone360 | 130 | pro | 150 | `step=brick` |
| t9-38 | 5 | jersey_club | Pro | 8-15 (15) | 17 | zh-CN | foldable | 200 | pro | 150 | Bravo (done=33) |
| t9-39 | 5 | jump_up | Facile | 1-8 (15) | 22 | fr | tablet8 | 100 | moyen | 174 | `step=brick` |
| t9-40 | 5 | jump_up | Facile | 9-15 (15) | 23 | fr | phone | 130 | moyen | 181 | Bravo (done=45) |
| t9-41 | 6 | jungle | Facile | 1-8 (15) | 21 | en | phone360 | 200 | curieux | 167 | `step=brick` |
| t9-42 | 6 | jungle | Facile | 9-15 (15) | 21 | en | foldable | 100 | curieux | 167 | Bravo (done=42) |
| t9-43 | 6 | liquid_dnb | Pro | 1-6 (17) | 18 | nl | tablet8 | 130 | confirmé | 150 | `step=pad_do` |
| t9-44 | 6 | liquid_dnb | Pro | 7-11 (17) | 21 | nl | phone | 200 | confirmé | 167 | `step=song_order` |
| t9-45 | 6 | liquid_dnb | Pro | 12-17 (17) | 13 | nl | phone360 | 100 | confirmé | 150 | Bravo (done=52) |
| t9-46 | 6 | lofi | Facile | 1-5 (13) | 17 | es | foldable | 130 | débutant motivé | 150 | `step=groove_hear` |
| t9-47 | 6 | lofi | Facile | 6-13 (13) | 15 | es | tablet8 | 200 | débutant motivé | 150 | Bravo (done=32) |
| t9-48 | 7 | minimal | Pro | 1-7 (14) | 22 | pt-BR | phone | 100 | pro | 174 | `step=make_intro` |
| t9-49 | 7 | minimal | Pro | 8-14 (14) | 19 | pt-BR | phone360 | 130 | pro | 153 | Bravo (done=41) |
| t9-50 | 7 | neurofunk | Facile | 1-8 (15) | 20 | de | foldable | 200 | moyen | 160 | `step=brick` |
| t9-51 | 7 | neurofunk | Facile | 9-15 (15) | 21 | de | tablet8 | 100 | moyen | 167 | Bravo (done=41) |
| t9-52 | 7 | phonk | Facile | 1-4 (13) | 15 | it | phone | 130 | curieux | 150 | `step=pad_do` |
| t9-53 | 7 | phonk | Facile | 5-13 (13) | 17 | it | phone360 | 200 | curieux | 150 | Bravo (done=32) |
| t9-54 | 7 | pop | Pro | 1-8 (16) | 23 | pl | foldable | 100 | confirmé | 181 | `step=brick` |
| t9-55 | 7 | pop | Pro | 9-16 (16) | 22 | pl | tablet8 | 130 | confirmé | 174 | Bravo (done=45) |
| t9-56 | 8 | reggaeton | Facile | 1-7 (15) | 19 | tr | phone | 200 | débutant motivé | 153 | `step=groove_hear` |
| t9-57 | 8 | reggaeton | Facile | 8-15 (15) | 21 | tr | phone360 | 100 | débutant motivé | 167 | Bravo (done=40) |
| t9-58 | 8 | retro_techno | Pro | 1-6 (13) | 22 | id | foldable | 130 | pro | 174 | `step=make_intro` |
| t9-59 | 8 | retro_techno | Pro | 7-13 (13) | 19 | id | tablet8 | 200 | pro | 153 | Bravo (done=41) |
| t9-60 | 8 | riddim | Facile | 1-8 (14) | 26 | hi | phone | 100 | moyen | 200 | `step=make_intro` |
| t9-61 | 8 | riddim | Facile | 9-14 (14) | 15 | hi | phone360 | 130 | moyen | 150 | Bravo (done=41) |
| t9-62 | 8 | speed_garage | Facile | 1-6 (13) | 18 | ar | foldable | 200 | curieux | 150 | `step=map_intro` |
| t9-63 | 8 | speed_garage | Facile | 7-13 (13) | 18 | ar | tablet8 | 100 | curieux | 150 | Bravo (done=36) |
| t9-64 | 9 | synthwave | Pro | 1-8 (16) | 24 | ja | phone | 130 | confirmé | 188 | `step=brick` |
| t9-65 | 9 | synthwave | Pro | 9-16 (16) | 23 | ja | phone360 | 200 | confirmé | 181 | Bravo (done=47) |
| t9-66 | 9 | tech_house | Facile | 1-5 (13) | 18 | ko | foldable | 100 | débutant motivé | 150 | `step=groove_hear` |
| t9-67 | 9 | tech_house | Facile | 6-13 (13) | 20 | ko | tablet8 | 130 | débutant motivé | 160 | Bravo (done=38) |
| t9-68 | 9 | techno | Pro | 1-5 (16) | 18 | zh-CN | phone | 200 | pro | 150 | `step=pad_do` |
| t9-69 | 9 | techno | Pro | 6-10 (16) | 21 | zh-CN | phone360 | 100 | pro | 167 | `step=song_order` |
| t9-70 | 9 | techno | Pro | 11-16 (16) | 13 | zh-CN | foldable | 130 | pro | 150 | Bravo (done=52) |
| t9-71 | 10 | trance | Facile | 1-6 (13) | 19 | fr | tablet8 | 200 | moyen | 153 | `step=brick` |
| t9-72 | 10 | trance | Facile | 7-13 (13) | 21 | fr | phone | 100 | moyen | 167 | Bravo (done=40) |
| t9-73 | 10 | trap | Facile | 1-7 (15) | 18 | en | phone360 | 130 | curieux | 150 | `step=groove_hear` |
| t9-74 | 10 | trap | Facile | 8-15 (15) | 16 | en | foldable | 200 | curieux | 150 | Bravo (done=34) |
| t9-75 | 10 | uk_garage | Pro | 1-4 (14) | 19 | nl | tablet8 | 100 | confirmé | 153 | `step=lead_do` |
| t9-76 | 10 | uk_garage | Pro | 5-14 (14) | 20 | nl | phone | 130 | confirmé | 160 | Bravo (done=39) |

## 7. Ce qui n'est pas vert

- L'amenée n'est prouvée par un test que pour house et techno (`BenchAmeneesTest`). Pour les 35 autres styles, le code est générique mais non testé : le premier siège amené de chaque style sert de preuve. Lot de banc possible (hors `scripts/bench/`, donc pas fait ici) : rendre ce test paramétré sur `TutoCourse.STYLES`.
- Le coût en gestes vient de 11 sièges du tour 8 seulement ; un siège qui s'épuise avant sa fin de tranche se note « budget épuisé au chapitre N » et le chef relance la suite (amenée au chapitre N).
- `fabrique-sieges.py` refuse un siège de tuto sous 250 gestes ; les start.txt de ce tour sont écrits à part, volontairement, puisqu'un siège ne joue qu'une tranche.
