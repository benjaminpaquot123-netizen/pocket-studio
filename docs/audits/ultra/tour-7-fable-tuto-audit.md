# Tuto : audit de fond (tour 7)

Code lu : `origin/eq/tour-7-tuto2` (`f1b9fdb1f`). Rapports lus : `origin/ccr-80af1a1b-mn0wec`,
`docs/audits/ultra/sieges/tour-7/controle/tuto2/` (verdict et sièges a à e).
Aucun code modifié : ce fichier est le seul ajout de la branche.

## En bref

- **Le tuto dit la même chose à dix endroits, et rien ne les oblige à rester d'accord.** Une étape, c'est un mot
  (`"check": "gap"`) que dix fonctions lisent chacune à sa façon : l'une dit « c'est réussi », une autre joue
  Montre-moi, une autre dessine les pointillés, une autre choisit le bouton, et le texte est écrit à la main à côté.
  Chaque tour corrige un endroit, et un autre se décale.
- **Tout le moteur suppose qu'on joue le cours dans l'ordre.** Dès qu'on ouvre un chapitre par la liste ou qu'on
  saute une étape, il manque une partie ou une piste, et l'étape se bloque ou se valide toute seule.
- **Les tests ne jouent que le chemin dans l'ordre, et sautent les étapes déjà validées.** Ils ne peuvent donc pas voir
  ces pannes. Les testeurs humains, eux, arrivent par la liste : ils les trouvent à chaque tour.
- Mesure faite ici, avec une sonde qui joue chaque étape de chaque style comme l'appli : **557 étapes fautives sur
  1130** (37 styles). Dans l'ordre, 28 étapes disent « Bravo » sans geste. Hors de l'ordre : 62 étapes valident à tort
  ou ne valident jamais, et 495 n'offrent ni bouton ni Montre-moi.
- **Proposition** : d'abord la sonde devient un test permanent (un jour de travail, il attrape tout) ; puis quatre
  règles dans le moteur qui ferment les familles de pannes ; puis une seule définition par type d'étape, d'où tout le
  reste se déduit. D3 et D4 reçoivent chacun leur règle vérifiée par test, au lieu d'une rustine par siège.

## Comment la mesure a été faite

Une sonde (hors dépôt, décrite au § 3.1 pour la refaire en test) joue le moteur du tuto (`TutoChecks`) avec les mêmes
appels et le même ordre que l'appli (`TutoController`) : état de départ noté à l'arrivée sur l'étape, bouton principal
tant qu'il y en a un, sinon la dernière image de Montre-moi, puis la question « l'étape est-elle validée ? ».
37 styles, 2 niveaux, 1921 étapes jouées (style × niveau × étape), soit 1130 étapes dans les fichiers. Les étapes
« C'est fait », « Écoute » et « Partage » sont hors mesure (rien à vérifier dans le projet).

Sept arrivées possibles sur une étape :

| Code | Arrivée | Ce que ça imite |
|---|---|---|
| O | dans l'ordre, depuis le projet vide du tuto | le parcours des tests actuels |
| S | l'étape d'avant sautée (« Suivant » deux fois) | un débutant pressé |
| V | chapitre ouvert par la liste, projet vide | le siège d (chapitre 12) |
| K | chapitre ouvert par la liste, un seul kick posé | le siège d après son premier coup |
| B | chapitre ouvert par la liste, boucle faite, parties pas encore créées | le siège c (chapitres 9 et 10) |
| k, b | une étape ouverte seule (pas la première du chapitre), sur un kick ou sur la boucle | « Aller à l'étape » |

Résultats (nombre d'étapes jouées) :

| Arrivée | Validée après Montre-moi | « Bravo » sans geste | Jamais validée | Impasse (ni bouton ni Montre-moi) |
|---|---|---|---|---|
| O | 1893 | **28** | 0 | 0 |
| S | 1752 | 23 | 4 | 40 |
| V | 986 | 8 | 0 | **927** |
| K | 1400 | 33 | 20 | 468 |
| B | 778 | 4 | 16 | 103 |
| k | 1046 | 0 | 28 | 315 |
| b | 419 | 0 | 26 | 92 |

En plus : 52 étapes (`gap`, `fill`, `crash`) validées dès le bouton « Créer la partie », sans aucun geste dans la
partie. Et 628 essais « le bon geste, fait dans une autre partie » : **aucun** ne valide (cette moitié de la règle 2
tient).

Limites, dites franchement : la sonde joue le modèle, pas l'écran. Elle ne voit ni la mise en page (D3, lu dans le
code), ni les phrases de secours de l'appli. Ses résultats recoupent les sièges c et d point par point (§ 2.5).

## 1. Pourquoi ces défauts reviennent

### 1.1 Cause n° 1 : une étape, dix lectures

Une étape du cours tient en une ligne de `tutos/<style>.json` : un texte, et un mot de vérification (`check`). Ce mot
est relu par dix fonctions séparées de `project-model/.../tuto/TutoChecks.kt` (2512 lignes), chacune avec son propre
`when` :

| Question | Fonction | Ligne |
|---|---|---|
| L'étape est-elle réussie ? | `done()` | 904 |
| Quel est l'état final ? | `show()` | 1294 |
| Que joue Montre-moi, image par image ? | `demo()` | 2314 |
| Quel bouton faut-il d'abord ? | `need()` | 2170 |
| Que fait ce bouton ? | `fulfil()` | 2261 |
| Où sont les pointillés ? | `hint()`, `ghostNotes()` | 2385, 2286 |
| Où sont les croix rouges ? | `toErase()` | 2398 |
| Dans quelle partie travaille-t-on ? | `sceneFor()`, `citedParts()`, `absentPart()` | 2128, 2215, 2223 |
| Quel est l'état de départ d'un « change quelque chose » ? | `signature()` | 607 |

À cela s'ajoutent `TutoController.kt` (ses propres cas : `hint()` l. 645, `goFor`, `noteFor`, `skipText`,
`partMade`), et **le texte, écrit à la main**, puis traduit.

Rien, ni le compilateur ni un test, n'oblige ces lectures à dire la même chose. Trois exemples, tous vus par les sièges :

- **« Bravo » sur une Montée vide** (siège d). `fulfil()` crée la Montée déjà silencieuse sur sa fin (`built()`,
  l. 1796-1806). `done()` pour `gap` ne regarde qu'une absence : « aucun coup dans les dernières cases »
  (l. 993-997). Une Montée qui vient de naître, ou vide, est donc réussie d'office. Le texte, lui, dit « efface les
  coups, touche chaque croix rouge ». Trois lectures, trois histoires.
- **Étape jamais validée** (siège d, Drop 2). `done()` pour `changed:drums:Drop 2` compare à un état de départ
  (l. 966-970). Cet état est pris **une seule fois, à l'arrivée** (`TutoController.kt` l. 780), et vaut « rien » si la
  partie n'existe pas encore (`signature()`, l. 607-614). Le bouton « Créer la partie Drop 2 » la crée ensuite
  (`fulfilNeed()`, l. 537-548), mais l'état de départ n'est pas repris. Résultat : « rien » pour toujours, et
  l'étape ne peut plus jamais se valider, même en imitant Montre-moi case par case.
- **Pointillé dans la mauvaise partie** (siège d). Pour `crash`, c'est `TutoController.hint()` (l. 645-651) qui pose
  le pointillé sur la première case de la cymbale **de la partie affichée**, quelle qu'elle soit. `done()` (l. 992),
  lui, ne regarde que les parties citées par l'étape. L'un montre la partie A, l'autre attend le Drop.

La preuve que la méthode « une rustine par siège » est en place : 192 lignes de commentaires de ces trois fichiers
nomment un lot ou un siège de testeur (54 dans `TutoChecks.kt`, 38 dans `TutoController.kt`, 100 dans
`TutoPanel.kt`). Chaque rustine est juste là où elle est posée. Aucune ne peut empêcher la lecture d'à côté de dériver.

### 1.2 Cause n° 2 : le moteur suppose le parcours dans l'ordre

Chaque étape compte sur ce que les étapes d'avant ont fabriqué : la boucle, la piste Basse, la partie Drop. Mais
l'appli laisse ouvrir n'importe quel chapitre (`goTo()`, `TutoController.kt` l. 289 : il change l'étape courante et
c'est tout) et sauter une étape (« Suivant » deux fois). Quand ce qui manque n'est pas là, le moteur n'a pas de
réponse commune. Chaque fonction a la sienne :

- `need()` n'offre « Créer la partie » que si la boucle existe (l. 2204), et `makePart()` refuse en silence si elle
  est vide (l. 1735-1740). Sur projet vide : ni bouton, ni Montre-moi. **927 étapes sur 1921** sont dans ce cas
  quand leur chapitre est ouvert par la liste. C'est le chapitre 12 du siège d (« seulement Je le fais »).
- Sur un projet presque vide (un seul kick), le bouton « Créer la partie Intro » s'affiche, mais ne fait rien : une
  Intro « sans kick » tirée d'une boucle d'un seul kick serait vide. 426 étapes jouées finissent ainsi dans la mesure.
  C'est le chapitre 10 du siège c.
- Une étape qui règle un bouton du synthé (`changed:synth:lead:…`), envoie un écho (`send:1`) ou pose la Pompe
  (`insert:PUMP`) suppose la piste écrite à l'étape d'avant. Étape d'avant sautée : le texte parle de la piste
  « Mélodie », qui n'existe pas, et rien n'offre de la créer (40 étapes).

### 1.3 Cause n° 3 : les tests ne peuvent pas voir ces pannes

Le dépôt a 106 fichiers de test pour le tuto (42 côté modèle, 64 côté appli). Le seul qui joue tous les cours,
`TutoAllCoursesTest.showMe()`, a quatre angles morts :

- il part toujours du projet vide et joue **dans l'ordre** (l. 107-111) : les arrivées S, V, K, B, k, b n'existent pas ;
- une étape déjà validée à l'arrivée est **sautée** (`continue`, l. 115) : c'est exactement le « Bravo » sans geste ;
- il appelle `need()` sans l'état de départ (l. 119), alors que l'appli le passe ;
- il joue `show()` (l'état final, l. 122), pas `demo()`, ce que Montre-moi joue vraiment à l'écran.

Le garde « aucune étape réussie sur projet vide » (l. 106) ne regarde que le projet vide du début, pas une partie
qui vient de naître. Les autres tests rejouent chacun le cas d'un siège : ils prouvent que ce cas-là est réparé, pas
que la règle tient partout. D'où le cycle : les testeurs trouvent, on répare le cas, le test du cas passe au vert, et
le tour suivant trouve le cas voisin.

### 1.4 D3 (consigne coupée ou cachée) : une mise en page calculée à la main

`TutoPanel.kt` (1576 lignes) calcule lui-même la hauteur du panneau, avec beaucoup d'états qui se croisent : ligne
repliée ou dépliée, hauteur « tenue », salle basse, carte du kit épinglée, « Pourquoi ? », bouton principal,
Montre-moi en cours, Bravo ; le tout multiplié par la taille du texte, la langue et la largeur de l'écran. Chaque
siège fait corriger un croisement (commentaires « Tour 6 N31 », « Tour 7 D3 », « Da-10 »…), et le croisement voisin
reste. Trois endroits précis :

- l. 649-655 : la carte du kit épinglée « prend sa place sur les mots au-dessus ». La consigne disparaît au début
  des chapitres 3 et 4 (siège a).
- l. 871-879 : panneau replié avec un bouton principal, la ligne ne montre que le nom du bouton, plus la consigne.
- l. 903-910 : la hauteur repliée est mesurée sur deux lignes de lettres latines (« Ag »). En japonais, la ligne est
  plus haute : texte coupé en plein mot, sans « … » (siège b).

Il manque **une règle unique, vérifiée par test** : « la consigne se lit toujours : au moins une ligne entière, et si
elle est coupée, “…” et “Lire la suite” ». Aucun test ne la pose sur tous les états à la fois.

### 1.5 D4 (trop de gestes) : le texte est fixe, le moteur ne l'est pas

La règle « deux gestes au plus en Facile » est vérifiée par `Tour7TutoTwoGesturesTest`. Mais ce test **compte les
verbes du texte français** (une expression régulière sur « touche », « puis les »…). Il ne sait pas ce que le moteur
exige. Or une étape `notes:*` porte, par construction, un bouton (« Créer la piste ») **et** une série de notes : pour
le texte, « touche Créer la piste Basse, puis les notes de la mesure 1 » fait deux gestes ; pour le doigt, un bouton
et cinq à huit notes.

Mesuré : en Facile, **191 étapes sur 880** demandent au moins un bouton et un geste dans la même étape (90 fois
« Créer la piste » + des notes, 52 fois « Copier » plusieurs fois, etc.), et 429 textes Facile sur 1440 contiennent
« puis ». Tant que le texte est une phrase fixe pour une étape qui a plusieurs moments, D4 reviendra : on raccourcit
une phrase, la suivante déborde.

## 2. Les deux règles, étape par étape

Règle 1 : « faire exactement ce que fait Montre-moi valide l'étape ».
Règle 2 : « une étape ne se valide jamais sur un état vide ou dans la mauvaise partie ».

Le compte, en étapes des fichiers (style + nom d'étape), chacune comptée une fois :

| Famille | Étapes | Liste |
|---|---|---|
| Règle 2 : validée sans le geste demandé | 40 | annexe A1 |
| Règle 1 : jamais validée, même après le bouton et Montre-moi | 22 | annexe A2 |
| Impasse sur un projet non vide (ni bouton qui marche, ni Montre-moi) | 307 | annexe A3 |
| Impasse seulement sur projet vide | 188 | annexe A3 |
| **Total** | **557 sur 1130** | |

Par style et niveau (une étape Facile et sa jumelle Pro comptées deux fois) : 976 sur 1921.

### 2.1 Règle 2, dans l'ordre : 28 « Bravo » sans geste

Ce sont les plus graves : un débutant sage, qui suit le cours du début à la fin, les rencontre.

- **`gap`, 14 étapes** (techno, acid_techno, hard_techno, retro_techno, acidcore, trance, gabber, boom_bap,
  reggaeton, synthwave). L'étape d'avant crée la partie déjà silencieuse sur sa fin ; l'étape `gap` demande
  d'effacer des coups qui n'y sont plus. Preuve : `TutoChecks.kt` l. 1796-1806 (la partie naît avec son silence) et
  l. 993-997 (réussi = « aucun coup », jamais « un coup effacé »).
- **`crash`, 3 étapes Pro** (techno, detroit, liquid_dnb) et **`fill`, 3 étapes Pro** (trap, drill, phonk). Même
  schéma : la partie arrive déjà dans l'état demandé. Pour `crash`, l. 992 : une seule des parties citées suffit
  (`any`), alors que le texte Pro demande le Drop **et** le Drop 2.
- **`velocity:hat` (3), `triplet:hat` (2), `lane:clap` (1), `notes:bass` (1), `insert:PUMP` (1)**. L'étape d'avant,
  jouée avec Montre-moi ou « Copier », écrit déjà tout le modèle, y compris ce que l'étape suivante devait apprendre.
  Exemple : techno `clap_2` (`techno.json:120`) demande de passer le dernier clap en « Doux » ; `done()` pour `lane:`
  ne regarde pas la force du coup, donc l'étape est réussie dès que l'étape d'avant a posé les claps.

### 2.2 Règle 2, hors de l'ordre

- La même famille revient dès qu'on arrive autrement : 23 étapes après une étape sautée, 33 sur un chapitre ouvert
  avec un kick posé.
- **52 étapes validées par le seul bouton « Créer la partie »** : `gap` sur Montée, Couplet, Pré-refrain, Break,
  Faux drop ; `crash` sur Drop ; `fill` sur Pré-refrain. C'est mot pour mot le grave du siège d : « Bravo dès la
  création de la Montée ».
- La mauvaise partie : le geste fait ailleurs ne valide jamais (628 essais, 0 faute). La faute vue par le siège d
  est un **pointillé** posé dans la partie A (`TutoController.kt` l. 645-651), pas une validation.

### 2.3 Règle 1 : 22 étapes jamais validées

- **`changed:drums:<Partie>` : 10 étapes sur 10** (16 sur 16 en comptant Facile et Pro) : house, deep_house,
  speed_garage, jump_up, dubstep, neurofunk, jungle, afro_house, amapiano (« Drop 2 ») et tech_house (« Drop B »).
  Dès que la partie
  n'existe pas à l'arrivée, l'étape est perdue : on crée la partie, on pose les quatre coups que Montre-moi pose,
  rien. Preuve : état de départ pris une fois (`TutoController.kt` l. 780), vide si la partie manque
  (`TutoChecks.kt` l. 607-614), jamais repris après le bouton (l. 537-548), et « vide » = faux pour toujours
  (l. 966-970). C'est l'étape 3/3 du chapitre 12 du siège d.
- **`song:phrases`** (disco, drill, lofi, phonk, synthwave) : le bouton « Donne les durées » se presse sept fois de
  suite sans valider, parce que l'ordre des parties n'est pas celui du cours, et que rien ne le répare (l. 2207).
- **`part:…:less`** (acid_house, uk_garage, dubstep, afro_house, amapiano, disco) : sur une boucle trop pauvre,
  « Créer la partie » se presse quatorze fois : la partie créée ne peut pas être « plus légère » que la boucle.

### 2.4 Impasses : ni bouton, ni Montre-moi

- **Projet vide, chapitre ouvert par la liste : 927 étapes jouées sur 1921.** Une seule cause : ce qui manque (la
  boucle) n'est jamais offert. Par type : parties 406, morceau 148, « change quelque chose » 91, nom de partie 74,
  `gap` 58, `crash` 46, `fill` 38, automation 31, Pompe 13, écho 12, Sub 10.
- **Projet non vide : 319 étapes des fichiers.** Parties 122 (bouton affiché qui ne fait rien), réglages de synthé
  et variations 50, morceau 35, automation 31, Pompe 27, écho 23, Sub 13, `crash` 9, `fill` 4, décalage 4, `gap` 1.
- **Étape d'avant sautée : 40 étapes.** La piste dont parle le texte n'existe pas (techno Pro 7.3 `lead_knobs`,
  house Pro 6.1 `bass_sub`, house Pro 8.2 `lead_delay`…), et aucun bouton ne propose de la créer.

### 2.5 Recoupement avec les sièges

| Siège | Ce qu'il a vu | Ce que la sonde trouve |
|---|---|---|
| c (ko, techno 9-10) | chapitre 10 bloqué, pas de bouton « Intro » à l'étape 1/7 | techno Facile 10.1, 10.2, 10.7 : bouton « Créer la partie » sans effet (K), rien du tout (V) |
| d (fr, house 12) | arrivé sur projet vide : « seulement Je le fais » | house 12.x : impasse (V) |
| d | « Bravo » dès la création de la Montée | house `gap` (`house.json:442`) validée par le seul bouton (K, k) |
| d | Drop 2 : quatre maracas posées, jamais validé | house `variation` : jamais validée dès que le Drop 2 manque à l'arrivée (k, b) |
| d | pointillé de la cymbale dans la partie A | `TutoController.kt` l. 645-651 |
| a (fr, house 3-5) | D4 : bouton + six notes dans une étape | 191 étapes Facile « bouton + gestes » |
| a, b | D3 : consigne cachée ou coupée | `TutoPanel.kt` l. 649-655, 871-879, 903-910 |

## 3. Conception proposée : rendre ces pannes impossibles

Trois étages, du moins cher au plus solide. Le premier se fait tout de suite et protège les deux autres.

### 3.1 Étage 1 : le test « Montre-moi partout » (la sonde devient un test)

Un test du modèle (rapide, sans Android) joue **chaque étape de chaque style, aux deux niveaux, depuis les sept
arrivées**, et vérifie quatre choses :

1. **Règle 1** : boutons puis Montre-moi, et l'étape est validée.
2. **Règle 2** : l'étape n'est jamais validée à l'arrivée, ni après les seuls boutons de préparation, quand son
   texte demande un geste. Et le bon geste recopié dans une autre partie ne valide pas.
3. **Pas d'impasse** : à tout moment il y a un bouton qui change vraiment le projet, ou Montre-moi, ou une phrase
   « il manque ceci » avec son bouton.
4. **Pointillés** : ce que montrent les pointillés et les croix est exactement ce que Montre-moi va toucher, dans la
   partie de l'étape.

Le cœur du test, tel que la sonde le joue (mêmes appels que `TutoController`) :

```kotlin
// One step, played as the app plays it: the baseline taken on arrival, the main button while there is one,
// else the last frame of « Montre-moi ».
fun play(step: TutoStep, from: Project, target: TutoTarget): Outcome {
    var p = from
    val baseline = step.check.takeIf { it.startsWith("changed:") }?.let { TutoChecks.signature(it.removePrefix("changed:"), p) }
    if (TutoChecks.done(step, p, target, baseline)) return Outcome.Already
    var gestures = 0
    repeat(14) {
        val need = TutoChecks.need(step, p, target, baseline)
        val next = if (need != null) {
            TutoChecks.fulfil(need, step, p, target)?.first?.sanitized()?.takeIf { it != p } ?: return Outcome.DeadEnd("button $need does nothing")
        } else {
            gestures++
            TutoChecks.demo(step, p, target)?.frames?.lastOrNull()?.sanitized() ?: return Outcome.DeadEnd("no button, no Montre-moi")
        }
        p = next
        if (TutoChecks.done(step, p, target, baseline)) return if (gestures == 0 && asksAGesture(step)) Outcome.DoneWithoutGesture else Outcome.Done(p)
    }
    return Outcome.NeverDone
}
```

Les arrivées : O = l'état laissé par l'étape d'avant ; S = l'état laissé par l'avant-dernière ; V = `target.blank()` ;
K = `blank()` plus un kick en case 1 ; B = l'état O juste avant la première étape `part:` du niveau ; k et b = K et B
pour chaque étape seule. Le test doit vivre à côté d'une fonction `play` **partagée avec le contrôleur**, pour que
le test ne puisse pas jouer autrement que l'appli.

Il naît rouge, avec la liste de l'annexe. C'est voulu : méthode « rouge, puis vert » de la maison. Chaque règle de
l'étage 2 en éteint une famille entière, et le test interdit ensuite tout retour, pour tout style ajouté.

À corriger dans le test actuel `TutoAllCoursesTest.showMe()` : remplacer le `continue` de la l. 115 par un échec,
passer l'état de départ à `need()` (l. 119), jouer `demo()` au lieu de `show()` (l. 122).

### 3.2 Étage 2 : quatre règles dans le moteur

- **R-A. L'état de départ se prend quand la chose existe.** Pour toute étape « change quelque chose », l'état de
  départ est repris dès que la partie ou la piste surveillée apparaît (après « Créer la partie », « Créer la
  piste »), pas seulement à l'arrivée. Ferme les 10 étapes `changed:drums:*` et les réglages de synthé après étape sautée.
  Fichiers : `TutoController.kt` (l. 537-548 et 780), `TutoChecks.kt` (l. 607-614, 966-971).
- **R-B. Une partie ne naît jamais réussie.** Deux moitiés. (1) `gap`, `fill` et `crash` ne se valident que si
  l'état demandé est là **et** qu'il n'y était pas quand la partie est apparue (même état de départ que R-A). (2)
  Quand un cours a une étape `gap` sur une partie, « Créer la partie » la fabrique **avec** les coups à effacer : le
  débutant a de vraies croix rouges à toucher. Si l'état est déjà bon à l'arrivée, l'appli le dit (« C'est déjà fait
  dans ta Montée : écoute ») au lieu de « Bravo ». Ferme les 28 de l'ordre et les 52 « validées par le bouton ».
  Fichiers : `TutoChecks.kt` (l. 981-997, 1735-1740, 1796-1806), `TutoValidation.kt`.
- **R-C. Ce qui manque est toujours offert.** `need()` ne renvoie un bouton que si ce bouton change le projet
  (vérifié dans `need()` même, en appelant `fulfil()`). Quand il manque la boucle ou une piste, un besoin neuf le
  dit et le répare : « Il te manque la boucle du chapitre 3 » avec deux boutons, « Poser la boucle du cours » (le
  moteur sait déjà l'écrire : c'est Montre-moi des chapitres d'avant) et « Aller au chapitre 3 ». Ferme les
  impasses V, K, B, k, b et S : les chapitres 10 et 12 se débloquent. Fichiers : `TutoChecks.kt` (l. 2170-2230,
  2261), `TutoController.kt` (`goTo()` l. 289, `fulfilNeed()`), `TutoPanel.kt` (le bouton), les textes traduits.
- **R-D. Un pointillé ne sort jamais de la partie de l'étape.** `TutoController.hint()` (l. 645-651) ne montre la
  case de la cymbale que si la partie affichée est une de celles de l'étape ; sinon c'est le bouton « Aller dans le
  Drop » qui parle. Fichier : `TutoController.kt`.

### 3.3 Étage 3 : une seule définition par type d'étape

C'est ce qui rend la dérive impossible, au lieu de la rattraper. Aujourd'hui, ajouter un type d'étape, c'est écrire
dix branches dans dix `when`. Demain, c'est écrire **un objet**, et le compilateur refuse qu'il en manque un morceau.

```kotlin
/** One kind of step ("gap", "lane:…", "changed:drums:…"), parsed once from `check`. Everything else derives from it. */
interface TutoKind {
    /** What must exist before the gesture, in order: the loop, a track, a part, the part shown. Each can always be made. */
    fun requires(step: TutoStep, p: Project, target: TutoTarget): List<Prereq>
    /** The gesture left to do, as edits in a named part: cells to add, cells to erase, notes, a knob, a send. Empty: nothing left. */
    fun remaining(step: TutoStep, p: Project, target: TutoTarget, armed: Any?): List<Edit>
    /** For « change something » kinds: what is watched, taken when the prerequisites are met. */
    fun arm(step: TutoStep, p: Project): Any? = null
}
```

Et le reste n'est plus écrit, il est **déduit**, une fois pour tous les types :

| Aujourd'hui (une branche par type) | Demain (une fonction pour tous) |
|---|---|
| `need()` / `fulfil()` | le premier `requires` non rempli, et son fabricant |
| `demo()` | les `remaining`, joués un par un |
| `show()` | les `remaining`, joués d'un coup |
| `hint()`, `ghostNotes()`, `toErase()` | les cases et notes de `remaining` (ajouts en pointillé, effacements en croix), dans leur partie |
| `done()` | tous les `requires` remplis, `remaining` vide, **et** il y a eu quelque chose à faire depuis l'armement |
| partie de travail | la partie des `remaining` |
| nombre de gestes (D4) | la taille de `requires` + `remaining` |

Avec cela, « faire ce que fait Montre-moi valide l'étape » n'est plus une règle à tester, c'est une identité :
Montre-moi joue `remaining`, et l'étape est validée quand `remaining` est vide. « Jamais sur un état vide » aussi :
une étape qui n'a rien eu à faire n'est pas « réussie », elle est « déjà faite ».

Le chantier se fait type par type, sans toucher aux fichiers `tutos/*.json` (les mêmes mots `check`, lus une fois),
sous la garde du test de l'étage 1 : un type migré, le test reste vert, on passe au suivant. `TutoChecks` reste la
façade que l'appli appelle.

### 3.4 D4 : une consigne par moment, comptée par le moteur

- Une étape a des **moments** : chaque bouton de préparation, puis chaque série du geste (une mesure, un pad). Le
  panneau montre la phrase **du moment en cours**, pas une phrase fixe pour toute l'étape. « Touche Créer la piste
  Basse », puis, une fois la piste là, « Touche les cinq notes en pointillé de la mesure 1 ».
- Dans les fichiers : le texte d'une étape devient une courte liste, une phrase par moment (`TutoStep`, `tutos/*.json`).
  `TutoValidation` refuse un texte Facile qui parle de deux moments.
- Le test ne compte plus les verbes du français : il compte les moments que le moteur exige. Règle Facile : un
  moment à l'écran, un bouton ou une série, jamais les deux.

### 3.5 D3 : une règle de lisibilité, vérifiée sur tous les états

- **La règle** : la consigne passe avant tout. Dans tout état du panneau, on lit au moins une ligne entière de la
  consigne ; si elle est coupée, elle finit par « … » et « Lire la suite » est là. Les autres blocs (carte du kit,
  « Pourquoi ? », exemples) se replient d'abord.
- **Dans le code** : sortir le calcul de `TutoPanel.kt` dans une fonction pure (« avec cet état, cette place, cette
  taille de texte, voici ce qu'on montre et sur quelle hauteur »). Elle se teste sans écran, sur tous les
  croisements d'un coup. Les trois endroits du § 1.4 se corrigent en passant par elle ; la hauteur d'une ligne se
  mesure dans la langue affichée.
- **Le test d'écran** : tous les états × français, japonais, allemand, coréen × texte à 100, 130 et 200 % ×
  téléphone étroit, téléphone, tablette. Il lit la mise en page réelle : aucune consigne coupée sans « … ».

## 4. Ordre des changements, fichiers et tests

| Priorité | Changement | Fichiers | Test qui le prouve |
|---|---|---|---|
| 1 | Test « Montre-moi partout » (§ 3.1), fonction `play` partagée | nouveau `project-model/src/test/.../tuto/TutoShowMeEverywhereTest.kt` ; `TutoChecks.kt` (fonction `play`) ; `TutoAllCoursesTest.kt` l. 115, 119, 122 | lui-même : rouge avec la liste de l'annexe |
| 2 | R-C : ce qui manque est toujours offert | `TutoChecks.kt`, `TutoController.kt`, `TutoPanel.kt`, textes | arrivées V, K, B, k, b, S : 0 impasse ; côté appli : chaque chapitre ouvert par la liste sur projet vide offre un bouton ou Montre-moi |
| 3 | R-B : une partie ne naît jamais réussie | `TutoChecks.kt`, `TutoValidation.kt` | 0 « Bravo » à l'arrivée ; 0 validation par le seul bouton ; refus d'un cours où `gap` suit une partie déjà silencieuse |
| 4 | R-A : état de départ repris quand la chose existe | `TutoController.kt`, `TutoChecks.kt` | les 10 étapes `changed:drums:*` validées depuis toutes les arrivées, aux deux niveaux ; test du contrôleur : house 12, « Créer la partie Drop 2 », quatre coups, coché |
| 5 | R-D : pointillé dans la partie de l'étape | `TutoController.kt` | test du contrôleur : pas de pointillé quand la partie affichée n'est pas celle de l'étape |
| 6 | D3 : règle de lisibilité (§ 3.5) | `TutoPanel.kt`, nouvelle fonction de mise en page | test pur sur tous les états ; test d'écran langues × tailles × largeurs |
| 7 | D4 : consigne par moment (§ 3.4) | `TutoStep`, `TutoValidation.kt`, `TutoPanel.kt`, `tutos/*.json`, `Tour7TutoTwoGesturesTest.kt` | un moment à l'écran en Facile, compté par le moteur |
| 8 | Une définition par type d'étape (§ 3.3) | nouveau `TutoKind.kt`, `TutoChecks.kt` en façade | le test de la priorité 1 reste vert à chaque type migré |

Les priorités 1 à 5 suffisent pour tenir le critère du contrôle (0 chapitre bloqué, 0 grave neuf) : elles ferment
les 557 étapes de l'annexe. Les priorités 6 et 7 ferment D3 et D4. La priorité 8 est ce qui empêche le tout de
revenir au prochain type d'étape ajouté ; elle peut attendre la fin de l'audit, mais pas être oubliée.

Conseil de méthode : la priorité 1 d'abord, seule, dans son lot. Tant qu'elle n'existe pas, chaque correction du
tuto se fait à l'aveugle, et le prochain contrôle coûtera encore cinq sièges pour retrouver ce qu'un test trouve en
quelques minutes.

## 5. Ce qui n'est pas vert

- Rien n'est corrigé : c'est un diagnostic. Les 557 étapes de l'annexe sont fautives sur `f1b9fdb1f`.
- La sonde n'est pas dans le dépôt (consigne : la branche ne contient que ce rapport). Le § 3.1 suffit à la réécrire.
- D3 est lu dans le code, pas mesuré à l'écran (pas de rendu Android dans cette session).
- Les textes n'ont pas été relus un par un contre les gestes : seulement comptés (§ 1.5).

## Annexes : toutes les étapes fautives

Lecture : `fichier.json:ligne` est la ligne du `"id"` de l'étape dans
`project-model/src/main/resources/tutos/`. « F 12.1 » = niveau Facile, chapitre 12, étape 1 ; « P » = Pro.
Scénarios : O dans l'ordre ; S étape d'avant sautée ; V chapitre ouvert sur projet vide ; K chapitre ouvert, un kick
posé ; B chapitre ouvert, boucle faite ; k et b une étape ouverte seule (un kick, boucle faite).

### A1. Règle 2 : validées sans le geste demandé (40 étapes)

L'étape dit « Bravo » à l'arrivée, ou dès le bouton « Créer la partie », sans le geste que son texte demande.

| Style | Étape (fichier:ligne) | Vérification | Partie | Où (F = Facile, P = Pro) | Scénarios |
|---|---|---|---|---|---|
| techno | `clap_2` (`techno.json:120`) | `lane:clap` | – | F 3.2 | O V K |
| techno | `gap` (`techno.json:511`) | `gap` | Montée | F 12.1 / P 12.2 | O S K B k |
| techno | `crash` (`techno.json:520`) | `crash` | Drop | P 12.3 | O S B b |
| detroit | `crash` (`detroit.json:395`) | `crash` | Drop | P 10.2 | O S B b |
| acid_techno | `gap` (`acid_techno.json:302`) | `gap` | Montée | F 8.1 / P 8.1 | O S K B |
| hard_techno | `gap` (`hard_techno.json:414`) | `gap` | Montée | F 10.1 / P 10.2 | O S K B k |
| retro_techno | `gap` (`retro_techno.json:360`) | `gap` | Break | F 9.1 | O S B |
| acidcore | `gap` (`acidcore.json:292`) | `gap` | Montée | F 8.1 / P 8.1 | O S K B |
| trance | `gap` (`trance.json:410`) | `gap` | Montée | F 10.1 / P 10.2 | O S K B k b |
| house | `gap` (`house.json:442`) | `gap` | Montée | F 12.1 / P 13.2 | K k |
| deep_house | `hat_vel` (`deep_house.json:94`) | `velocity:hat` | – | P 2.5 | O V K |
| deep_house | `bass_pump` (`deep_house.json:179`) | `insert:PUMP` | – | P 3.4 | S |
| tech_house | `bass_pump` (`tech_house.json:189`) | `insert:PUMP` | – | P 4.3 | S |
| tech_house | `gap` (`tech_house.json:421`) | `gap` | Montée | F 10.1 / P 11.2 | K k |
| dnb | `gap` (`dnb.json:481`) | `gap` | – | F 12.2 / P 13.2 | K k |
| jump_up | `hat_vel` (`jump_up.json:128`) | `velocity:hat` | – | P 4.3 | O S V K |
| jump_up | `gap` (`jump_up.json:518`) | `gap` | – | F 12.2 / P 13.2 | K k |
| dubstep | `gap` (`dubstep.json:460`) | `gap` | – | F 12.2 / P 13.2 | K k |
| gabber | `gap` (`gabber.json:363`) | `gap` | Montée | F 10.1 / P 10.2 | O S K B k b |
| afro_house | `bass_pump` (`afro_house.json:171`) | `insert:PUMP` | – | P 3.3 | O V K |
| speed_garage | `gap` (`speed_garage.json:405`) | `gap` | Montée | F 10.1 / P 11.2 | K k |
| liquid_dnb | `gap` (`liquid_dnb.json:497`) | `gap` | – | F 12.2 / P 13.2 | K k |
| liquid_dnb | `crash` (`liquid_dnb.json:505`) | `crash` | Drop | P 13.3 | O S B b |
| neurofunk | `gap` (`neurofunk.json:465`) | `gap` | – | F 12.2 / P 13.2 | K k |
| jungle | `gap` (`jungle.json:466`) | `gap` | – | F 12.2 / P 13.2 | K k |
| riddim | `gap` (`riddim.json:434`) | `gap` | Faux drop | F 11.2 / P 12.2 | k |
| trap | `hat_triplet` (`trap.json:127`) | `triplet:hat` | – | F 4.2 | O V K |
| trap | `fill` (`trap.json:385`) | `fill` | Pré-refrain | P 12.1 | O S B |
| drill | `hat_triplet` (`drill.json:120`) | `triplet:hat` | – | F 4.2 | O V K |
| drill | `bass_2` (`drill.json:179`) | `notes:bass` | – | F 5.3 | O V K |
| drill | `fill` (`drill.json:415`) | `fill` | Pré-refrain | P 12.1 | O S B |
| phonk | `fill` (`phonk.json:341`) | `fill` | Pré-refrain | P 10.1 | O S B |
| boom_bap | `gap` (`boom_bap.json:350`) | `gap` | Couplet | F 10.1 / P 10.1 | O S K B |
| grime | `gap` (`grime.json:326`) | `gap` | Couplet 2 | F 10.1 / P 10.1 | K |
| lofi | `gap` (`lofi.json:338`) | `gap` | Couplet | F 10.1 / P 10.1 | K |
| reggaeton | `gap` (`reggaeton.json:436`) | `gap` | Pré-refrain | F 12.2 / P 12.2 | O S K B k b |
| afrobeats | `gap` (`afrobeats.json:411`) | `gap` | Pré-refrain | F 12.2 / P 12.2 | K k |
| synthwave | `hat_vel` (`synthwave.json:121`) | `velocity:hat` | – | P 4.2 | O V K |
| synthwave | `gap` (`synthwave.json:446`) | `gap` | Pré-refrain | F 12.2 | O S B b |
| pop | `gap` (`pop.json:447`) | `gap` | Pré-refrain | F 12.2 / P 12.2 | K k |

### A2. Règle 1 : jamais validées après le bouton et Montre-moi (22 étapes)

Le bouton principal pressé tant qu'il s'offre, puis Montre-moi imité jusqu'à treize fois : toujours pas validée.

| Style | Étape (fichier:ligne) | Vérification | Partie | Où (F = Facile, P = Pro) | Scénarios |
|---|---|---|---|---|---|
| house | `variation` (`house.json:461`) | `changed:drums:Drop 2` | – | F 12.3 / P 13.4 | k b |
| deep_house | `variation` (`deep_house.json:412`) | `changed:drums:Drop 2` | – | F 10.3 / P 10.3 | K B k b |
| tech_house | `variation` (`tech_house.json:437`) | `changed:drums:Drop B` | – | F 10.3 / P 11.4 | k b |
| acid_house | `make_groove` (`acid_house.json:191`) | `part:Groove:less` | – | F 6.2 / P 6.2 | K k |
| uk_garage | `make_intro` (`uk_garage.json:303`) | `part:Intro:less+nobass+nokick` | – | F 8.1 / P 8.1 | K |
| jump_up | `variation` (`jump_up.json:526`) | `changed:drums:Drop 2` | – | P 13.3 | K B k b |
| dubstep | `make_intro` (`dubstep.json:361`) | `part:Intro:less` | – | F 10.1 / P 11.1 | K |
| dubstep | `make_fin` (`dubstep.json:409`) | `part:Fin:less` | – | F 10.6 / P 11.6 | k |
| dubstep | `variation` (`dubstep.json:468`) | `changed:drums:Drop 2` | – | P 13.3 | K B k b |
| afro_house | `variation` (`afro_house.json:307`) | `changed:drums:Drop 2` | – | F 8.4 / P 8.4 | S k b |
| afro_house | `make_groove` (`afro_house.json:324`) | `part:Groove:less+nopad` | – | F 8.6 / P 8.6 | K k |
| speed_garage | `variation` (`speed_garage.json:423`) | `changed:drums:Drop 2` | – | F 10.3 / P 11.4 | k b |
| amapiano | `variation` (`amapiano.json:300`) | `changed:drums:Drop 2` | – | F 8.4 / P 8.4 | S k b |
| amapiano | `make_groove` (`amapiano.json:317`) | `part:Groove:less+nobass` | – | F 8.6 / P 8.6 | K k |
| disco | `make_groove` (`disco.json:293`) | `part:Groove:less+nolead` | – | F 8.2 / P 8.2 | K k |
| disco | `song_phrases` (`disco.json:343`) | `song:phrases` | – | F 9.2 / P 9.2 | K B k b |
| neurofunk | `variation` (`neurofunk.json:473`) | `changed:drums:Drop 2` | – | P 13.3 | K B k b |
| jungle | `variation` (`jungle.json:474`) | `changed:drums:Drop 2` | – | P 13.3 | K B k b |
| drill | `song_phrases` (`drill.json:399`) | `song:phrases` | – | F 11.2 / P 11.2 | B b |
| phonk | `song_phrases` (`phonk.json:325`) | `song:phrases` | – | F 9.2 / P 9.2 | B b |
| lofi | `song_phrases` (`lofi.json:322`) | `song:phrases` | – | F 9.2 / P 9.2 | B b |
| synthwave | `song_phrases` (`synthwave.json:421`) | `song:phrases` | – | F 11.2 / P 11.2 | B b |

### A3. Impasses : ni bouton qui marche, ni Montre-moi

Par fichier : d'abord les étapes en impasse sur un projet non vide (arrivées S, K, B, k, b ; 319 étapes, dont 12 déjà
dans A1 ou A2), puis celles en impasse seulement quand le chapitre est ouvert sur projet vide (arrivée V). Chaque
étape est suivie de sa ligne dans le fichier.

- **techno.json** : projet non vide (8) : `bass_cutoff`:229, `lead_knobs`:312, `make_intro`:403, `make_intro2`:412, `make_drop2`:450, `make_fin`:459, `filter`:559, `levels`:578. Projet vide seulement (9) : `groove_name`:335, `make_drop`:421, `make_break`:430, `make_montee`:439, `song_order`:475, `song_phrases`:485, `fill`:501, `gap`:511, `crash`:520.
- **detroit.json** : projet non vide (6) : `make_intro`:302, `make_intro2`:310, `make_drop2`:336, `make_fin`:345, `filter`:426, `levels`:445. Projet vide seulement (7) : `groove_name`:248, `make_drop`:319, `make_break`:327, `song_order`:360, `song_phrases`:370, `gap`:386, `crash`:395.
- **acid_techno.json** : projet non vide (7) : `make_intro`:212, `make_intro2`:220, `make_break`:236, `make_drop2`:253, `make_fin`:261, `filter`:333, `filter_intro`:344. Projet vide seulement (7) : `groove_name`:160, `make_drop`:228, `make_montee`:244, `song_order`:276, `song_phrases`:286, `gap`:302, `crash`:311.
- **hard_techno.json** : projet non vide (9) : `lead_knobs`:218, `lead_dist`:227, `make_intro`:304, `make_intro2`:312, `make_faux`:346, `make_drop2`:355, `make_fin`:363, `filter`:461, `levels`:480. Projet vide seulement (9) : `groove_name`:251, `make_drop`:320, `make_break`:328, `make_montee`:336, `song_order`:378, `song_phrases`:388, `fill`:404, `gap`:414, `crash`:423.
- **industrial_techno.json** : projet non vide (8) : `lead_delay`:207, `make_intro`:282, `make_intro2`:290, `make_dropb`:306, `make_drop2`:323, `make_fin`:331, `filter`:412, `levels`:431. Projet vide seulement (7) : `groove_name`:230, `make_drop`:298, `make_break`:315, `song_order`:346, `song_phrases`:356, `gap`:372, `crash`:381.
- **retro_techno.json** : projet non vide (7) : `lead_knobs`:196, `make_intro`:271, `make_intro2`:279, `make_drop2`:311, `make_fin`:319, `filter`:407, `levels`:426. Projet vide seulement (8) : `groove_name`:219, `make_drop`:287, `make_mini`:295, `make_break`:303, `song_order`:334, `song_phrases`:344, `gap`:360, `crash`:369.
- **acidcore.json** : projet non vide (9) : `make_intro`:202, `make_intro2`:210, `make_break`:226, `make_drop2`:243, `make_fin`:251, `song_order`:266, `song_phrases`:276, `filter_intro`:323, `filter`:334. Projet vide seulement (5) : `groove_name`:150, `make_drop`:218, `make_montee`:234, `gap`:292, `crash`:301.
- **trance.json** : projet non vide (11) : `lead_knobs`:225, `make_intro`:308, `make_intro2`:316, `make_break`:343, `make_drop2`:351, `make_fin`:359, `song_order`:374, `song_phrases`:384, `delay`:441, `riser`:457, `levels`:475. Projet vide seulement (6) : `groove_name`:249, `make_montee`:324, `make_drop`:335, `fill`:400, `gap`:410, `crash`:419.
- **house.json** : projet non vide (8) : `bass_sub`:183, `bass_pump`:191, `lead_delay`:262, `make_intro`:341, `make_drop2`:380, `make_fin`:391, `riser`:500, `levels`:518. Projet vide seulement (10) : `groove_name`:285, `make_montee`:350, `make_drop`:362, `make_break`:371, `song_order`:407, `song_phrases`:417, `fill`:432, `gap`:442, `crash`:451, `variation`:461.
- **deep_house.json** : projet non vide (9) : `bass_sub`:170, `bass_pump`:179, `lead_delay`:238, `make_intro`:308, `make_drop`:326, `make_drop2`:343, `make_fin`:354, `filter`:443, `levels`:461. Projet vide seulement (8) : `groove_name`:261, `make_montee`:316, `make_break`:334, `song_order`:369, `song_phrases`:378, `toms`:393, `gap`:403, `variation`:412.
- **tech_house.json** : projet non vide (9) : `bass_glide`:173, `bass_sub`:181, `bass_pump`:189, `lead_ott`:223, `lead_delay`:232, `make_intro`:326, `make_fin`:372, `riser`:460, `levels`:487. Projet vide seulement (11) : `groove_name`:280, `make_montee`:334, `make_drop`:345, `make_dropb`:353, `make_break`:364, `song_order`:387, `song_phrases`:396, `fill`:411, `gap`:421, `crash`:429, `variation`:437.
- **acid_house.json** : projet non vide (5) : `make_fin`:207, `song_order`:222, `song_phrases`:231, `filter`:253, `filter_drop`:263. Projet vide seulement (4) : `groove_name`:142, `make_intro`:183, `make_groove`:191, `make_break`:199.
- **uk_garage.json** : projet non vide (9) : `snare_late`:126, `bass_sub`:175, `lead_delay`:233, `make_couplet`:311, `make_pre`:320, `make_fin`:337, `song_order`:352, `song_phrases`:361, `levels`:418. Projet vide seulement (4) : `groove_name`:256, `make_intro`:303, `make_pont`:329, `gap`:376.
- **dnb.json** : projet non vide (10) : `bass_knobs`:205, `bass_sub`:213, `bass_pump`:221, `lead_echo`:289, `make_intro`:382, `make_drop`:403, `make_drop2`:421, `make_fin`:430, `riser`:529, `levels`:546. Projet vide seulement (8) : `groove_name`:313, `make_montee`:391, `make_break`:412, `song_order`:446, `song_phrases`:456, `fill`:472, `gap`:481, `crash`:489.
- **jump_up.json** : projet non vide (14) : `bass_knobs`:196, `bass_sub`:204, `bass_pump`:212, `lead_knobs`:290, `lead_ott`:299, `make_intro`:392, `make_drop`:413, `make_drop2`:431, `make_drop2_2`:444, `make_drop2_3`:456, `make_fin`:467, `delay`:558, `riser`:574, `levels`:591. Projet vide seulement (8) : `groove_name`:323, `make_montee`:401, `make_break`:422, `song_order`:483, `song_phrases`:493, `fill`:509, `gap`:518, `variation`:526.
- **dubstep.json** : projet non vide (8) : `bass_knobs`:193, `bass_sub`:202, `bass_pump`:210, `make_drop`:382, `make_drop2`:400, `make_fin`:409, `riser`:516, `levels`:533. Projet vide seulement (9) : `groove_name`:292, `make_intro`:361, `make_montee`:370, `make_break`:391, `song_order`:425, `song_phrases`:435, `fill`:451, `gap`:460, `variation`:468.
- **minimal.json** : projet non vide (6) : `make_intro`:292, `make_intro2`:300, `make_drop2`:333, `make_fin`:342, `filter`:424, `levels`:443. Projet vide seulement (8) : `groove_name`:239, `make_drop`:308, `make_break`:316, `make_montee`:325, `song_order`:357, `song_phrases`:367, `gap`:383, `crash`:392.
- **gabber.json** : projet non vide (6) : `lead_knobs`:188, `make_intro`:269, `make_drop2`:304, `make_fin`:312, `riser`:401, `levels`:419. Projet vide seulement (9) : `groove_name`:211, `make_montee`:277, `make_drop`:288, `make_break`:296, `song_order`:327, `song_phrases`:337, `fill`:353, `gap`:363, `crash`:372.
- **afro_house.json** : projet non vide (9) : `bass_pump`:171, `lead_delay`:213, `make_intro`:282, `make_drop2`:298, `make_fin`:316, `song_order`:339, `song_phrases`:348, `filter`:386, `levels`:413. Projet vide seulement (5) : `groove_name`:236, `make_break`:290, `variation`:307, `make_groove`:324, `toms`:363.
- **speed_garage.json** : projet non vide (8) : `bass_knobs`:175, `bass_dist`:183, `bass_sub`:191, `lead_delay`:241, `make_intro`:311, `make_fin`:356, `bass_filter`:460, `levels`:478. Projet vide seulement (11) : `groove_name`:264, `make_montee`:319, `make_drop`:330, `make_break`:338, `make_drop2`:347, `song_order`:371, `song_phrases`:380, `fill`:395, `gap`:405, `crash`:414, `variation`:423.
- **amapiano.json** : projet non vide (6) : `lead_delay`:167, `make_drop2`:291, `make_fin`:309, `song_order`:332, `song_phrases`:341, `levels`:380. Projet vide seulement (5) : `groove_name`:234, `make_intro`:275, `make_break`:283, `variation`:300, `make_groove`:317.
- **disco.json** : projet non vide (7) : `lead_reverb`:222, `make_intro`:285, `make_montee`:301, `make_break`:310, `make_fin`:319, `song_order`:334, `levels`:401. Projet vide seulement (4) : `groove_name`:245, `make_groove`:293, `song_phrases`:343, `fill`:359.
- **liquid_dnb.json** : projet non vide (10) : `bass_sub`:210, `bass_pump`:218, `pad_chorus`:253, `lead_echo`:305, `make_intro`:398, `make_drop`:419, `make_drop2`:437, `make_fin`:446, `riser`:544, `levels`:561. Projet vide seulement (8) : `groove_name`:329, `make_montee`:407, `make_break`:428, `song_order`:462, `song_phrases`:472, `fill`:488, `gap`:497, `crash`:505.
- **neurofunk.json** : projet non vide (10) : `bass_knobs`:190, `bass_flanger`:198, `bass_sub`:206, `bass_pump`:214, `make_intro`:366, `make_drop`:387, `make_drop2`:405, `make_fin`:414, `riser`:512, `levels`:529. Projet vide seulement (8) : `groove_name`:297, `make_montee`:375, `make_break`:396, `song_order`:430, `song_phrases`:440, `fill`:456, `gap`:465, `variation`:473.
- **jungle.json** : projet non vide (9) : `bass_sub`:208, `bass_pump`:216, `make_intro`:367, `make_drop`:388, `make_drop2`:406, `make_fin`:415, `delay`:506, `riser`:522, `levels`:539. Projet vide seulement (8) : `groove_name`:298, `make_montee`:376, `make_break`:397, `song_order`:431, `song_phrases`:441, `fill`:457, `gap`:466, `variation`:474.
- **riddim.json** : projet non vide (8) : `bass_knobs`:175, `bass_sub`:183, `sub_tremolo`:191, `bass_pump`:199, `make_drop`:355, `make_drop2`:373, `riser`:483, `levels`:500. Projet vide seulement (10) : `groove_name`:249, `make_intro`:325, `make_montee`:334, `make_fake`:346, `make_break`:364, `make_fin`:382, `song_order`:398, `song_phrases`:408, `fill`:424, `gap`:434.
- **trap.json** : projet non vide (10) : `bass_pump`:186, `make_intro`:324, `make_couplet`:333, `make_pre`:343, `song_order`:359, `song_phrases`:369, `fill`:385, `crash`:395, `delay`:426, `levels`:443. Projet vide seulement (1) : `groove_name`:270.
- **drill.json** : projet non vide (11) : `bass_pump`:208, `make_intro`:354, `make_couplet`:363, `make_pre`:373, `song_order`:389, `song_phrases`:399, `fill`:415, `crash`:425, `delay`:456, `intro_filter`:465, `levels`:483. Projet vide seulement (1) : `groove_name`:302.
- **phonk.json** : projet non vide (9) : `bass_pump`:153, `make_intro`:282, `make_couplet`:291, `make_pre`:299, `song_order`:315, `song_phrases`:325, `fill`:341, `crash`:351, `levels`:399. Projet vide seulement (1) : `groove_name`:236.
- **boom_bap.json** : projet non vide (6) : `snare_late`:100, `make_intro`:298, `make_couplet`:307, `song_order`:324, `song_phrases`:334, `levels`:401. Projet vide seulement (2) : `groove_name`:252, `gap`:350.
- **grime.json** : projet non vide (8) : `bass_sub`:138, `make_intro`:258, `make_couplet`:266, `song_order`:300, `song_phrases`:310, `crash`:334, `delay`:356, `levels`:382. Projet vide seulement (4) : `groove_name`:212, `make_couplet2`:275, `make_pont`:284, `gap`:326.
- **jersey_club.json** : projet non vide (8) : `bass_pump`:156, `make_couplet`:268, `make_pre`:277, `song_order`:301, `song_phrases`:311, `crash`:327, `delay`:349, `levels`:366. Projet vide seulement (3) : `groove_name`:214, `make_intro`:260, `make_pont`:286.
- **lofi.json** : projet non vide (8) : `snare_late`:138, `make_intro`:280, `make_couplet`:288, `song_order`:312, `song_phrases`:322, `delay`:371, `intro_filter`:380, `levels`:398. Projet vide seulement (3) : `groove_name`:233, `make_pont`:297, `gap`:338.
- **reggaeton.json** : projet non vide (10) : `bass_sat`:199, `make_intro`:348, `make_couplet`:356, `make_pre`:366, `song_order`:401, `song_phrases`:411, `crash`:445, `delay`:476, `riser`:485, `levels`:503. Projet vide seulement (5) : `groove_name`:287, `make_post`:376, `make_pont`:385, `fill`:427, `gap`:436.
- **afrobeats.json** : projet non vide (10) : `conga_late`:128, `bass_pump`:177, `make_couplet`:334, `make_pre`:343, `song_order`:376, `song_phrases`:386, `crash`:420, `delay`:451, `chorus`:460, `levels`:477. Projet vide seulement (6) : `groove_name`:266, `make_intro`:326, `make_post`:353, `make_pont`:361, `fill`:402, `gap`:411.
- **synthwave.json** : projet non vide (14) : `bass_pump`:188, `pad_attack`:229, `lead_knobs`:272, `make_intro`:357, `make_couplet`:366, `make_pre`:376, `song_order`:411, `song_phrases`:421, `fill`:437, `gap`:446, `crash`:455, `delay`:496, `riser`:512, `levels`:530. Projet vide seulement (3) : `groove_name`:295, `make_post`:386, `make_pont`:395.
- **pop.json** : projet non vide (9) : `bass_pump`:228, `make_intro`:356, `make_couplet`:365, `song_order`:413, `song_phrases`:423, `crash`:456, `delay`:487, `riser`:496, `levels`:514. Projet vide seulement (6) : `groove_name`:293, `make_pre`:376, `make_post`:388, `make_pont`:397, `fill`:438, `gap`:447.
