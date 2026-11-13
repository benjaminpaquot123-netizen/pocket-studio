# Audit : plus d'audits en même temps, sans se marcher dessus

## Le verdict en six lignes

1. Ton intuition est juste aux trois quarts : sur 51 fichiers de code touchés cette nuit, 38 ne l'ont été que par un seul lot (mesuré, git).
2. Elle est fausse à deux endroits : deux fichiers géants que tout le monde traverse (`RhythmViewModel.kt`, `AppRoot.kt`) et les lots « textes », qui passent dans tous les écrans.
3. Trouver n'est pas le goulot. Une vague de 2 h 40 a donné plus de 13 h de réparations (mesuré). Plus de testeurs à la fois ne ferait que grossir la file.
4. Le vrai goulot est l'entrée : un seul portail (20 min par lot) puis une vérification GitHub de 2 h, sur la machine du chef qui redémarre.
5. On peut auditer et réparer en même temps, à une condition : jamais dans la même zone. Le gel de toute l'appli pendant une vague n'est pas nécessaire.
6. Les audits qu'on peut multiplier sans limite sont les programmes sur GitHub : zéro quota. Les testeurs joués par des agents restent rares et ciblés.

Lecture seule, aucun Gradle lancé. « Mesuré » = lu dans git, les traces du portail, les journaux ou GitHub. « Estimé » = calcul de ma part.

## 1. La carte des zones

Fichiers des lots : `git diff --name-only <fusion>^1 <fusion>` sur les 18 lots entrés du 12 au soir au 13 à 11 h 48 (mesuré). Sièges : `sieges/tour-6/sieges.tsv` (mesuré).

| Zone | Fichiers principaux | Sièges du tour 6 | Lots de la nuit | Sort de sa zone par |
|---|---|---|---|---|
| Tuto | `app/…/tuto/` (3 fichiers), `model/tuto/`, 37 `tutos/*.json`, `i18n/*/tuto*.json` | D a, D b, H a, B b, D c, 3 suites, 3 contrôles | tutogrille, tutoconsignes, reparations, emulateur, tutofleches | `PianoRoll.kt`, `RhythmScreen.kt`, `PlainWords.kt`, `TrackSheet.kt` |
| Pad Live | `app/…/padlive/` (13), `live/`, `session/LivePad*` | A b, E a, G b | restes, morceauexport | `RhythmViewModel.kt`, `PlayStrip.kt` |
| Remix | `app/…/remix/` (20), `model/Remix*`, `StemEdits` | B a, E c, G a | remixdecoupe | `SessionScreen.kt` |
| Exports et Mix | `export/` (18), `mixer/` (16) | A c, E b | morceauexport | `RhythmViewModel.kt` |
| Synthé, grille, piano roll | `synth/` (12), `rhythm/` (18) | B c, F b, une part de B b | tutogrille, langues | `PianoRoll.kt` et `RhythmScreen.kt` (rangés dans la zone Tuto) |
| Coquille, accueil, Guide | `ui/` (22), `onboarding/`, `guide/`, `projects/` | A a, F a, C a, C b | gaucher, guidecoquille, cablage, moyens2 | `AppRoot.kt`, `TopBar.kt`, `LaunchChoice.kt`, `GuidePanel.kt` |
| Textes | `i18n/*/*.json` hors tuto, `model/i18n/` | les 19 (15 langues) | langues, langues2 | tous les écrans (voir plus bas) |
| Moteur du son | `engine/`, pont JNI | le siège son (un programme) | kick | rien |
| Banc | `app/src/test/…/human` (60 fichiers), `scripts/bench`, `scripts/human-*` | self-test, juge | juge7, ci2, banc7, ecoute | rien |

Les vrais points de collision (fichiers de code touchés par deux lots ou plus, mesuré) :

| Fichier | Taille | Lots |
|---|---|---|
| `synth/PianoRoll.kt` | 1 419 lignes | tutogrille, reparations, tutofleches |
| `rhythm/RhythmViewModel.kt` | 11 901 lignes | morceauexport, moyens2 |
| `ui/AppRoot.kt` | 4 959 lignes | gaucher, guidecoquille |
| `rhythm/RhythmScreen.kt` | 1 542 lignes | langues, tutogrille |
| `session/SessionScreen.kt` | 965 lignes | langues, remixdecoupe |
| `guide/GuidePanel.kt` | 822 lignes | langues, guidecoquille |
| `onboarding/LaunchChoice.kt` | 606 lignes | langues, morceauexport |
| `ui/TopBar.kt`, `ui/PlayStrip.kt` | 583, 486 lignes | langues + moyens2, langues + restes |
| `library/PlainWords.kt` | 74 lignes | tutoconsignes, emulateur |
| `TutoPanel`, `TutoController`, `TutoChecks`, tutos, textes du tuto | | tutoconsignes puis tutofleches (même zone, l'un après l'autre) |

Ce que ces chiffres disent :
- Sur ces 13 fichiers, 6 sont des suites dans une même zone (5 dans le tuto, 1 dans la coquille). Il reste 7 collisions entre zones, et six viennent du seul lot « langues » (mesuré). Un lot de textes n'est pas une zone : il traverse tout.
- Le lot « morceauexport » couvrait six dossiers (Pad Live, export, Mix, projets, accueil, synthé). Un lot taillé par famille de défauts crée des collisions ; un lot taillé par dossier n'en crée pas.
- 16 des 77 défauts réels du tri pointent `RhythmViewModel.kt` (9) ou `AppRoot.kt` (7) : un défaut sur cinq (mesuré, `verdicts.tsv`). `RhythmViewModel.kt` n'est dans aucune zone du § 13.3.
- Coût mesuré d'une collision : le lot morceauexport refusé pour `RhythmViewModel.kt`, 17 min et une relecture de plus (audit de la nuit).
- Captures de référence : 8 sur 128 touchées par deux lots, un conflit, seize périmées (mesuré).
- Doublons entre testeurs : 11 grappes sur 179 vues par deux sièges ou plus, 5 doublons sur 98 verdicts (mesuré). Dix sièges qui disent la même chose, ce n'est pas arrivé. Ce qui est arrivé : cinq sièges arrêtés au même mur (le budget de 120 gestes).

## 2. Les sortes d'audit

| Audit | Faut-il geler ? | Coût | Ce qu'il est seul à trouver |
|---|---|---|---|
| Vague de sièges (agents, banc texte) | Sa zone seulement. Joue un commit vert nommé | 19 sièges en 2 h 40 (mesuré). 4 sièges par machine. Le plus cher en quota : environ 5 à 8 millions de tokens relus par siège de 110 gestes (estimé d'après 108 écrans de 2,5 ko) | L'incompréhension, l'hésitation, les mots, le parcours entier |
| Siège pilote | Non | 20 min, un siège | Une fiche ou un budget faux, avant d'en payer cinq |
| Siège de contrôle | Non : il joue la branche du lot | 30 min pour un tuto entier (mesuré : 191 à 259 gestes, 5 h 58 à 6 h 30). Une machine par branche (un seul Gradle) | Si la correction aide vraiment le débutant |
| Rejeux (`rejeux/`) | Non | Programme, dans chaque portail, zéro quota. Un seul existe (mesuré) | Le retour d'un défaut déjà corrigé |
| Balayage des tutos | Non | Programme : 1 110 parcours, 15 langues (mesuré) | Tuto injouable, phrase non traduite, nom coupé. Pas l'hésitation |
| Scans d'écrans et de langues (`ScreenScanTest`, `LanguagesScanTest`, `CaptureMatrixTest`, `TutoPanelFitsTest`) | Non | Programmes, dans la vérification GitHub | Texte coupé, textes superposés, cible trop petite, contraste, mot anglais |
| Captures de référence | Non | Dans la vérification (43 min avec les tests, mesuré). Un écart n'est qu'un avertissement : il faut regarder | Un écran qui change sans qu'on l'ait voulu |
| Bancs du son | Non | Morceau 21 min, écoute en direct 10, vrais projets 3, endurance 5 (mesuré, passage 43402800379) | Silence, clic, fondu, niveau. Pas l'oreille |
| Téléphone simulé | Non | 17 min (mesuré). Plus le « singe » de 5 000 gestes, à la demande | Ce que le banc accepte et qu'un téléphone refuse (le plantage du 13 au matin) |
| Gardes statiques (motifs refusés par le téléphone, règles d'architecture, numéros de commande) | Non | Secondes | Une famille entière de fautes, pour toujours |
| Lecture de code par zone (agent, lecture seule) | Non : un commit nommé | Ni Gradle ni machine. Environ 0,3 à 1 million de tokens par zone (estimé) | Sauvegarde, thread audio, chemins morts, écarts entre banc et téléphone |
| Téléphone de Benjamin | Oui : une version verte, figée | Ton temps | L'oreille, la latence, le Bluetooth |

Un seul audit a besoin d'un gel : la vague de sièges, et seulement pour sa zone. Tout le reste tourne quand on veut, sur un commit vert nommé.

## 3. Où part le temps

- Vague : 19 h 30 à 22 h 11, 161 min pour 19 sièges (mesuré, audit de la nuit).
- File du portail : 22 h 14 à 5 h 55, 461 min, dont 268 de passages réussis, 103 perdus (redémarrages, refus), 90 à vide (mesuré, audit de la nuit).
- Un passage réussi au portail : 11 à 42 min, 20 en moyenne, 333 min pour 17 fusions (mesuré : heure de la fusion contre heure du commit de traces).
- Vérification GitHub : 92 à 165 min sur les sept derniers passages complets, 119 pour le dernier vert (mesuré, `gh run list`). Un passage sur trois est rouge : 4 sur les 12 derniers (mesuré).
- Détail du dernier passage vert : contrôles rapides 1 min, moteur 27 (dont 22 de vérifications de mémoire), puis en parallèle téléphone simulé 17 et « build » 89 (tests et captures 43, banc du morceau 21, écoute 10, vrais projets 3, endurance 5, APK 6).
- 27 min d'attente pure : « build » et le téléphone simulé attendent la fin du moteur (`ci.yml`, `needs: engine`). Et « build » fait six choses à la suite.
- Deux vérifications sur la même branche se suivent, elles ne tournent pas ensemble (`ci.yml` l. 14 à 17). Sur deux branches différentes, elles tournent ensemble.
- Une ligne fausse a coûté cinq heures : téléphone simulé rouge à 6 h 44, vert à 11 h 47 (mesuré, `EN-COURS.md`).
- Machine du chef : 5 redémarrages en 12 h (ton chiffre). Le lot écrit sur cette machine a duré de 23 h 06 à 5 h 41 (mesuré).
- Un tour de paquet aujourd'hui : trois portails (60 min) plus la vérification (120 min), soit environ 3 h pour trois lots (calculé sur les mesures).
- Le quota n'est mesuré nulle part dans le dépôt.

Les vrais goulots, dans l'ordre : (1) la vérification de 2 h, une seule à la fois ; (2) le portail, un lot à la fois, sur la machine la moins sûre ; (3) le tuto, zone d'un seul tenant : 5 des 18 lots, forcément l'un après l'autre ; (4) le chef seul pour lire, distribuer, fusionner ; (5) loin derrière, le nombre de sièges.

## 4. Propositions, de la plus rentable à la moins rentable

| n° | Quoi | Gain | Coût | Risque |
|---|---|---|---|---|
| 1 | Geler par zone, plus toute l'appli | Audit et correction en même temps, tout de suite | Une colonne dans `EN-COURS.md` | Faible : 1 verdict périmé sur 98 la nuit où 5 lots attendaient (mesuré) |
| 2 | Vérification GitHub de chaque lot sur sa propre branche, avant le portail | Un rouge se voit dans le lot, pas 2 h après la fusion. Plusieurs lots vérifiés ensemble | Zéro quota, zéro machine (dépôt public, mesuré) | Faible. La vérification du paquet reste obligatoire |
| 3 | La machine du chef ne fait que le portail | Plus de 103 min perdues, plus de lot qui traîne six heures | Les correcteurs sont des discussions à part | Faible |
| 4 | Raccourcir la vérification : rien n'attend le moteur ; « build » coupé en trois (tests et captures ; bancs du son ; APK) | 119 min → environ 55 (estimé) | Un lot du banc, seul dans son paquet | Moyen : on touche la chaîne qui publie. Mêmes contrôles, rien de retiré |
| 5 | Lots taillés par dossier ; un lot de textes passe seul, en tête de paquet | Supprime 6 collisions entre zones sur 7 (mesuré) | Rien | Faible |
| 6 | Trois sièges par zone, juste avant que son correcteur soit libre | Moins de rapports qui vieillissent, quota divisé | Rien | Faible |
| 7 | Chaque défaut qu'un programme peut voir devient une garde | Zéro quota ensuite, toutes les langues | Un test par famille | Faible |
| 8 | Sièges courts : arriver droit dans la zone (« amenée » : existe pour premier morceau, Remix, Pad Live ; manque chapitre N du tuto, export, Mix) | Un contrôle de 60 gestes au lieu de 300 : le quota suit le carré des gestes, environ 25 fois moins (estimé) | Un lot du banc | Moyen : un débutant posé au chapitre 6 hésite plus. Garder un tuto entier par paquet |
| 9 | Portail par paquet : trois fusions, un seul passage de tests | 60 min → environ 30 par paquet (estimé) | Un lot du banc, délicat | Moyen. À faire après le n° 2 |
| 10 | Captures de référence refaites une fois par paquet, par le vérificateur | Plus de conflit d'images ni de captures périmées | Rien | Faible : le lot regarde quand même les siennes |

Le détail de ce qui compte :

- **Combien de zones de front.** Un cycle de zone dure environ 3 h : trois sièges 40 min, tri 25, lot 60, relecture 20, contrôle 30 (estimé). Un paquet dure aussi 3 h et prend trois lots. Donc **trois zones de front aujourd'hui**, cinq ou six quand la vérification sera à 55 min. Au-delà, les lots font la queue et vieillissent.
- **Combien de machines.** Sept : le chef (portail seul), trois testeurs (les trois chaudes), trois correcteurs. Douze sièges possibles à la fois (3 × 4), mais neuf suffisent : trois par zone.
- **Les trois attelages qui ne se touchent pas** (mesuré sur la nuit) : (A) tuto avec grille et piano roll, un lot à la fois ; (B) coquille, accueil, Guide ; (C) écrans de création : Pad Live, Remix, exports et Mix, synthé. Dans C, deux lots de front si un seul touche `RhythmViewModel.kt`. Textes, moteur et banc passent entre deux.
- **Un testeur et un correcteur par zone** : oui, mais le correcteur ne trie pas ses propres rapports (la nuit du 12, le tri fait par un correcteur excusait trop l'appli : 4 « faute du banc » contestés sur 5). Un trieur neuf sert les trois zones, l'une après l'autre.
- **Ne pas payer dix fois le même défaut.** Jamais deux sièges avec la même mission, la même langue et le même appareil. Un pilote avant chaque mission. Le dédoublonnage reste fait par programme (`fuse-reports.py`). On ne donne pas la liste des défauts connus au testeur : il ne serait plus neuf.
- **Ce qui peut être parallèle sans danger** : les sièges sur un commit nommé, les vérifications GitHub par branche, les programmes d'audit, les lectures de code, les lots de zones différentes, les contrôles de lots différents sur des machines différentes.
- **Ce qui doit rester à la file** : l'ordre des fusions, la poussée par le chef seul, un seul lot dans une zone d'un seul tenant, la vérification du paquet avant le paquet suivant, ton APK seulement depuis du vert.
- **À jouer par des programmes plutôt que par des agents** : textes coupés dans les 15 langues, flèches du tuto, cible cachée sous le panneau, tuto jouable, motifs refusés par le téléphone, son. Aux agents : comprendre, hésiter, juger les mots.

Ce qui serait dangereux, dit sans détour :
- Deux portails en même temps, ou deux machines qui poussent l'intégration : un lot refusé est déjà entré dans l'histoire une fois (`ea294587a`).
- Sauter la vérification du paquet parce que chaque lot était vert seul : des fichiers séparés peuvent se casser entre eux (le test voisin resté rouge quatre heures).
- Auditer « la tête », ou une zone qui a un lot ouvert : le siège son a jugé un code périmé.
- Deux lots à la fois dans le tuto : trois l'ont fait, 12 défauts corrigés sur 32 et trois graves neufs.
- Plus d'une douzaine de sièges de vague à la fois : on trouve cinq fois plus vite qu'on ne répare.
- Plus de deux machines froides ouvertes ensemble (Maven répond 429).
- Découper maintenant les deux fichiers géants : utile, mais cela touche tout. Plus tard, seul dans son paquet, tout au vert.
- Retirer des contrôles de la vérification pour aller vite. On les met côte à côte, on n'en enlève aucun.

## 5. Texte de règle proposé pour le § 13

### 13.10 Auditer et corriger en même temps, par zone

1. Le gel est par zone. Une zone est dans un seul état, écrit dans `EN-COURS.md` : audit, tri, correction ou contrôle. Aucun siège de vague dans une zone qui a un lot ouvert. Cette règle remplace le gel de toute l'intégration du 13.1.
2. Un siège de vague joue le dernier commit vert, téléphone simulé compris, dans un dossier à part posé sur ce commit. Jamais la tête.
3. Au tri, tout défaut dont le fichier a changé entre le commit joué et la tête est revérifié sur la tête.
4. Trois zones de front au plus, tant que la vérification dure plus d'une heure. Une vague de zone : trois sièges au plus, jamais deux avec la même mission, la même langue et le même appareil.
5. Un lot se taille par dossier, pas par famille de défauts. Un lot de textes, de gaucher ou d'accessibilité traverse les écrans : il passe seul, en tête de paquet.
6. Fichiers carrefours : `RhythmViewModel.kt`, `AppRoot.kt`, `PianoRoll.kt`, `RhythmScreen.kt`, `SessionScreen.kt`, `PlayStrip.kt`, `TopBar.kt`, `LaunchChoice.kt`, `GuidePanel.kt`, `PlainWords.kt`. Un seul lot par paquet touche chacun d'eux ; les autres écrivent « Demande ».
7. Dès qu'un lot dit « fini », le chef lance la vérification GitHub sur la branche du lot. Le lot passe au portail quand elle est verte. La vérification du paquet reste obligatoire.
8. La machine du chef ne lance Gradle que pour le portail. Aucun correcteur n'y travaille.
9. Qui corrige une zone ne trie pas les rapports de cette zone.
10. Tout défaut qu'un programme peut voir reçoit sa garde dans le lot qui le corrige. Les programmes d'audit (bancs du son, balayages, téléphone simulé, endurance) se lancent sur GitHub, sur un commit vert nommé, à toute heure.

### Les trois changements à faire d'abord

1. **Le gel par zone** (règles 1 à 6) : un tableau des zones dans `EN-COURS.md`, et la prochaine vague part par zone, trois sièges, pendant que les autres zones réparent. Aucun code à écrire.
2. **La vérification GitHub par lot, et la machine du chef réservée au portail** (règles 7 et 8) : dès le paquet 4. Une commande par lot, zéro quota.
3. **Le lot du banc « ci3 »** : la vérification passe de 2 h à environ 55 min (estimé), seul dans son paquet, prouvé par sa propre vérification verte.

## Ce que je n'ai pas vérifié

- Le quota : aucune mesure dans le dépôt. Mes chiffres de tokens sont des ordres de grandeur.
- Le temps de la vérification une fois coupée en trois : estimé, chaque morceau recompile de son côté.
- Le nombre de tâches que GitHub accepte ensemble pour ce compte (vingt en principe, non lu dans le dépôt).
- La cause des redémarrages de la machine du chef.
- Qu'une vérification lancée à la main sur une branche de lot ne publie rien : lu dans `ci.yml` (l. 434 et 452), non essayé sur une branche de lot.
