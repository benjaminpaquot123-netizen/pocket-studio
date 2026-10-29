# Écrire un cours « Tuto de A à Z »

Un style = un fichier : `project-model/src/main/resources/tutos/<style>.json`, où `<style>` est l'identifiant du style dans l'assistant (`techno`, `uk_garage`, `boom_bap`…, voir `StarterWizard.ORDER`).
Rien d'autre à coder : dès que le fichier existe, le style apparaît dans « J'apprends » (les autres restent sous « Bientôt pour les autres styles »), et les tests le valident puis le jouent de bout en bout, en Facile et en Pro.

Modèle complet : `tutos/jump_up.json` (poli par trois tours de testeurs). Preuve minimale sur un style Club : `tutos/techno.json`.

Les textes sont en français (la zone `tuto` attend ses traductions). Le code reste en anglais.

---

## 1. Les règles des testeurs (à ne jamais casser)

Elles viennent de `docs/testeur-humain/` (lots « tuto 3 » à « tuto 5 »).

1. **Rien n'avance tout seul.** Une étape faite affiche « Bravo », « Écouter ton résultat » et « Étape suivante » ; c'est l'utilisateur qui passe à la suite. N'écris jamais un texte qui dit « la suite arrive ».
2. **Une étape = un geste.** « Mets le tempo à 132 », pas « règle le tempo, la tonalité et le swing ».
3. **Une mesure à la main, puis « Copier ».** Les notes à écrire restent courtes (4 à 8 notes par geste). Une ligne qui se répète s'écrit une fois ; le bouton « Copier sur les mesures suivantes » fait le reste. Écris donc des phrases de 1, 2 ou 4 mesures (`lines`, `lineBars`).
4. **Le détecteur est exigeant.** Une étape n'est cochée que si tout ce qui est en pointillé est écrit (toutes les cases, toutes les notes, les deux mesures si la boucle en a deux).
5. **« Montre-moi » et « Écouter l'exemple » partout où c'est possible.** Toute étape avec un `check` autre que `manual`, `listen`, `changed:…` ou `shared` a son « Montre-moi ». Donne un `listen` à presque toutes.
6. **Deux niveaux.** Facile (`"levels": "f"`) : les mots et les écrans du Mode simple (pas de piano roll ni de mixeur détaillé, pas de roulements, pas de décalage de coup, pas d'éditeur de 303). Pro (`"p"`) : les mots du producteur et tous les outils. `"fp"` (par défaut) : les deux. `textPro` remplace `text` en Pro quand les mots diffèrent.
7. **Le morceau doit ressembler au style dès le chapitre 2.** Le projet de départ a déjà le kit et les sons du style (voir § 6).
8. **Pas de détail répété dans une boucle courte** (un crash, une variation : dans la partie où il doit sonner une fois, cf. `crash` et `part`).
9. **Une vraie Montée** : roulement qui accélère, puis un silence d'au moins un temps avant le Drop (`build`, `fill`, `gap`).
10. **Aucune note hors de la gamme**, sub immobile (une octave sous la basse, sans sauts).

---

## 2. Le fichier

```json
{
  "format": 1,
  "style": "techno",
  "mood": "dark",
  "key": "F phrygian",
  "chords": "i VII",
  "lines": { "bass": "2:F2:1 6:F2:1 10:F2:1 14:Ab2:1" },
  "lineBars": { "bass": 1 },
  "follow": ["bass"],
  "chapters": [ … ]
}
```

| Champ | Rôle |
|---|---|
| `style` | L'identifiant du style, **égal au nom du fichier**. |
| `mood` | Facultatif. L'humeur de l'assistant qui choisit les accords : `dark`, `happy`, `future`, `dreamy`. Vide : celle de l'assistant pour ce style. |
| `key` | Facultatif. La tonalité du cours si ce n'est pas celle de l'assistant : `"A minor"`, `"F# dorian"`, `"Eb phrygian"`, `"A harmonic minor"`. Gammes : `major`, `minor`, `dorian`, `phrygian`, `harmonic minor`, `minor pentatonic`, `major pentatonic`, `blues`. |
| `chords` | Facultatif. La grille, un accord par mesure, en chiffres romains : `"i iv"`, `"i VI III VII"`. Les extensions (9e, 7e) ne s'écrivent pas : c'est la manière de jouer les accords du style qui les donne. |
| `lines` | Les lignes du cours par rôle (§ 4). |
| `lineBars` | La longueur d'une phrase en mesures (1, 2, 4 ou 8), si ce n'est pas le plus petit nombre qui contient ses notes. |
| `follow` | Les rôles dont la phrase courte **suit les accords** quand elle se répète (« Copier » la transpose d'un accord à l'autre). |
| `drums` | La batterie du cours (§ 4 bis), à la place des lignes de la recette. |
| `chapters` | Les chapitres (§ 3). |

`key`, `chords` et `mood` changent le morceau cible du tuto (l'exemple, les notes en pointillé, le contrôle de « Tonalité ») sans toucher aux recettes de l'assistant.

---

## 3. Chapitres et étapes

```json
{
  "id": "drums",
  "title": "La batterie",
  "titlePro": "Le beat",
  "why": "Pourquoi ce chapitre (une ou deux phrases).",
  "levels": "fp",
  "map": false,
  "steps": [
    { "id": "kick_do", "text": "À toi : un kick sur chaque temps.", "check": "lane:kick",
      "spot": "rhythm.pattern", "target": "kick", "listen": "drums:kick" }
  ]
}
```

Chapitre : `id` (unique), `title`, `titlePro`, `why` (« Pourquoi ? »), `levels`, `map` (la carte du morceau sous le chapitre).

Étape :

| Champ | Rôle |
|---|---|
| `id` | Unique dans le fichier (chapitres compris). |
| `text`, `textPro` | « À toi : … », en Facile / en Pro. |
| `why` | Son « Pourquoi ? ». |
| `levels` | `f`, `p`, `fp`. |
| `check` | Le détecteur (§ 5). Par défaut `manual` (« C'est fait »). |
| `spot` | L'écran de « Je le fais » (§ 7). Vide : pas de « Je le fais ». |
| `target` | Le rôle visé (piste, pad, ou plusieurs séparés par des virgules selon le détecteur). |
| `listen` | Ce que joue « Écouter l'exemple » (§ 8). |
| `keep`, `lanesFrom`, `transpose`, `build` | Comment une partie est fabriquée (§ 9). |
| `song` | L'ordre et les durées du morceau : `"Intro:16,Drop:32,Break:16,Drop:32,Fin:16"`. |
| `part` | La ou les parties où travaille une transition (`fill`, `gap`, `automation`, `crash`) : `"Break"`, `"Drop,Drop 2"`. |
| `length` | En cases (16es) : le silence d'un `gap` (défaut : un temps, un demi-temps sous 100 BPM ; 16 = une mesure), la fin d'un `fill`. |
| `demo` | `"kick"` : la comparaison kick long / kick court (seulement pour un style qui démarre sur un kit de `TutoTarget.KITS`, le jump up). |
| `hearMine` | Une étape d'écoute qui joue d'abord la musique de l'utilisateur, puis l'exemple ; cochée quand les deux ont été entendus. |
| `skip` | Ce que le morceau perd si « Suivant » laisse l'étape non faite. |

---

## 4. Les lignes (`lines`)

Rôles : `bass`, `pad` (les accords), `lead`, `acid` (la 303), et deux pistes de plus :
`pad2` (une **nappe** à côté des accords : piste « Nappe », son de nappe du style) et `counter` (un **contre-chant** à côté de la mélodie : piste « Contre-chant »). Ces deux-là n'existent que par leur ligne dans `lines`.
La `sub` n'a pas de ligne : c'est la basse une octave plus bas (`track:sub`), immobile.

Une ligne = des entrées séparées par des espaces : `pas:Note:longueur[:extra]`.

- `pas` : la 16e où la note commence, depuis le début de la phrase (0 à 15 pour la 1re mesure, 16 à 31 pour la 2e…).
- `Note` : `F2`, `Gb2`, `C#3` (C-1 = 0, `C4` = 60). Un **accord** : `F3+Ab3+C4`.
- `longueur` : en 16es (`1` = une double croche, `4` = un temps, `16` = une mesure).
- `extra` (facultatif) : la force (`0.7`, ou de 1 à 127 : `100`), et pour la 303 `a` (accent) et `s` (slide) : `4:C3:1:as`.

Exemples :

```json
"bass": "0:A1:2:110 3:A1:1:70 6:A2:2:90 10:A1:2:60",
"pad":  "0:A3+C4+E4:16 16:F3+A3+C4:16",
"lead": "0:E5:2 2:G5:2 4:A5:4 12:G5:2 14:E5:2",
"acid": "0:A2:1 2:A3:1:a 3:A2:1 6:C3:1:s 7:A2:1"
```

**Phrase et « Copier ».** La phrase dure 1, 2, 4 ou 8 mesures (`lineBars`, sinon la plus courte qui contient ses notes). Elle est répétée sur toute la piste. Les mesures identiques à une mesure déjà écrite (ou transposées, avec `follow`) sont écrites par « Copier sur les mesures suivantes » ; les autres sont à écrire à la main. Une phrase d'une mesure = 1 mesure à la main, puis « Copier ».

**Suivre les accords** (`"follow": ["bass"]`) : une basse d'une mesure sur une grille de quatre accords est transposée, mesure après mesure, le long de la gamme, de l'accord où elle a été écrite à celui de chaque mesure. « Copier » fait cette transposition ; le détecteur l'attend.

**808 (trap, drill, phonk, jersey club, reggaeton).** Le rôle `bass` est la 808 : « Créer la piste Basse » fait une piste 808 (le son 808 du style), écrite au piano roll. **Glissé** : une note qui dure jusqu'après le début de la suivante glisse vers elle (sa durée de glissé est celle du style, pas réglable note par note).

**303** (`acid`) : seulement pour un style qui a une 303 (`acid_techno`, `acid_house`, `acidcore`). Les pas à écrire sont en pointillé dans l'onglet Acide (Pro), avec leur note, leur accent (`!`) et leur slide (`⌒`). Le détecteur `notes:acid` vérifie la note, l'octave, l'accent et le slide de chaque pas.

Sans ligne dans le cours, les notes visées sont celles de l'assistant (sauf `pad2` et `counter`, qui en demandent une).

---

## 4 bis. La batterie du cours (`drums`)

```json
"drums": {
  "*":     { "kick": "X...X...X...X...", "openhat": "..x...x...x...x.", "hat": "x.x.x.x.x.x.x.x.", "shaker": "o.x.o.x.o.x.o.x." },
  "Break": { "hat": "x...x...x...x...", "crash": "X..............." },
  "Drop":  { "kick": "X...X...X...X...", "clap": "....X.......X...", "hat": "xxxxxxxxxxxxxxxx", "crash": "X..............." }
}
```

- `"*"` : la batterie de tout le morceau : chaque rôle nommé **remplace la ligne de la recette** (le groove que l'utilisateur écrit, les `lane:…`, les exemples `drums:…`) ; les rôles non nommés gardent celle de la recette, un rôle nouveau s'ajoute.
- `"<Partie>"` : la batterie de cette partie (et de ses secondes : « Drop » vaut aussi pour « Drop 2 »), **exactement ces pads** (les autres se taisent dans l'exemple). « Créer la partie » y pose ces coups sur les pads que l'utilisateur joue ; une étape `lane:<rôle>` avec `"part": "<Partie>"` les fait écrire dans cette partie.
- Une case par pas : 16, 32 ou 64 cases (1, 2 ou 4 mesures). `X` fort, `x` normal, `o` doux, `2` `3` `4` un roulement, `T` / `t` un triolet sur cette case et la suivante (qui reste `.`), `.` rien.
- Rôles : `kick`, `snare`, `clap`, `hat`, `openhat` (ou `open_hat`), `ride`, `perc`, `rim` (= `perc`, le pad Rim), `perc2`, `shaker`, `tom`, `rumble`, `808`, et `crash`. **`shaker`** = la ligne de la recette jouée par le shaker (les charleys de l'afrobeats), sinon `perc2` (le pad Shaker). Une ligne gardée garde sa voix de kit (les congas de l'afrobeats). Deux rôles sur le même pad (`rim` et `perc`) : erreur.
- **`crash`** dans une partie : un coup joué **une seule fois** au début de la partie, jamais en boucle (dans l'exemple ; l'utilisateur le pose avec l'étape `crash:once`). Dans `"*"` : en boucle sur la cymbale ; avec un `ride` (le kit n'a qu'une cymbale), ses coups deviennent des coups forts du ride.

---

## 5. Les détecteurs (`check`)

| `check` | Coché quand… | « Montre-moi » |
|---|---|---|
| `manual` | « C'est fait » / « J'ai compris » (l'utilisateur). | — |
| `listen` | L'exemple a été entendu (avec `hearMine` : sa boucle aussi). | — |
| `tempo` | Le tempo du style à ±2 BPM. | oui |
| `key` | La tonalité et la gamme du cours. | oui (gamme puis tonique) |
| `swing` | Le swing du style (à moitié de sa valeur près, au plus 5 points). Seulement pour un style qui swingue. | oui |
| `lane:<rôle>` | Toutes les cases de la ligne du style sur ce pad (un peu de surplus toléré), sur toute sa longueur (« Passer à 2 mesures » proposé). Rôles : `kick`, `snare`, `clap`, `hat`, `open_hat`, `perc`, `perc2`, `tom`, `ride`, `rumble`, `808` (ceux que le style joue). | oui |
| `lane:<rôle>` + `part` | Pareil, dans cette partie, avec sa batterie à elle (`drums.<Partie>`) ; le bouton mène d'abord à la partie. | oui |
| `lane:<rôle>:rolls` | Pareil, et chaque roulement de la ligne (« 3 » : 3 coups dans une case ; « t » : un triolet sur 2 cases) roulé pareil (Pro). | oui |
| `velocity:hat` | Les charleys des temps plus doux que ceux d'entre, sur chaque mesure (« Copier » après la 1re). | oui |
| `triplet:<rôle>` | Au moins un **triolet** sur le pad de ce rôle, dans n'importe quelle partie : 3 coups sur 2 cases (ou 6 sur un temps), choisi par l'appui long sur un coup › « Triolet ». Rôles : ceux de `lane:` (`hat` le plus souvent). | oui (les triolets de la ligne du style, sinon un sur le dernier temps de la 1re mesure) |
| `offset:<rôle>:late` / `:early` | Au moins la moitié des coups du pad joués en retard / en avance d'au moins 5 ms (réglage « Décalage du coup », Pro). | oui |
| `notes:bass` / `notes:pad` / `notes:lead` / `notes:pad2` / `notes:counter` | Toutes les notes de la ligne écrites sur la piste de ce rôle (« Créer la piste … » la fait : Basse, ou 808 dans les styles 808, Accords, Mélodie, Nappe, Contre-chant). | oui, mesure par mesure |
| `notes:acid` | Toute la ligne 303 : notes, accents, slides. | oui |
| `track:sub` | Une piste Sub = la basse une octave plus bas (un bouton). | oui |
| `changed:synth:<rôle>:<BOUTON+BOUTON>` | Les boutons nommés montés d'au moins 15 sur 100 : `CUTOFF`, `RESONANCE`, `OSC2_DETUNE`, `GLIDE`, `A_ATTACK`, `UNISON_DETUNE`. | — |
| `insert:<EFFET>` | L'effet sur chaque piste/pad de `target` (`"sub,bass"`, `"clap"`). Effets : noms de `EffectType` (`PUMP`, `OTT`, `FILTER`, `DISTORTION`…). | oui |
| `send:0` / `send:1` | Envoi réverbe / écho. Par défaut : réverbe = Accords + caisse claire, écho = Mélodie. Avec `target` à plusieurs rôles (`"pad,clap"`) : ces pistes et pads-là. | oui |
| `automation` (`:rise`), `automation:fall`, `automation:wave` | Une courbe de filtre sur la piste de `target` (ou la coupure de la 303 : `"acid"`), qui monte / descend / monte puis redescend, sur la partie `part` (défaut : Montée). | oui |
| `changed:volumes:<rôles>` | Chaque piste nommée un peu baissée depuis le début de l'étape. | oui |
| `changed:drums:<Partie>` | Un nouveau coup de charley ouvert ou de percussion dans cette partie. | oui |
| `master` | Un effet sur le master. | oui |
| `loudness:<CIBLE>` | Volume visé du master : `CLUB`, `STREAMING`, `AUTO`. | oui |
| `label:<Nom>` | Une partie porte ce nom (« Groove » : la boucle principale). | oui |
| `part:<Partie>:<mode>` | La partie existe et respecte son mode (§ 9). | oui (un bouton la crée) |
| `song:order` | Les parties de `song` dans l'ordre (d'autres peuvent être entre). | oui |
| `song:phrases` | L'ordre **et** chaque durée de `song`. | oui (bouton « Durées conseillées ») |
| `fill` | Un roulement dans le dernier temps de la partie `part` (avant le silence `length`, ou celui de son étape `gap`) : un coup ×3 ou plus ; sur un pad qui n'accélère pas dans la partie (`target: "tom"`), 3 coups ou plus. | oui |
| `gap` | Les dernières `length` cases de la partie `part` silencieuses, sur les pads des rôles de `target` (vide : tous). | oui (cases barrées) |
| `crash` | La cymbale (pad 12) sur la 1re case d'une des parties `part` (défaut : Drop, Drop 2), **dans la boucle** (elle revient à chaque tour : le jump up). | oui |
| `crash:once` | La cymbale **une seule fois**, au début de chaque passage de la partie `part` dans le morceau : sa 1re mesure joue une mesure « Crash », puis la partie continue (comme l'assistant). Après l'étape `song:order`. | oui |
| `shared` | Le morceau exporté et partagé (l'appli le marque). | — |

---

## 6. Le projet de départ (`TutoTarget.blank()`)

Vide (aucun coup, aucune note, aucune partie) mais avec :

- **le kit du style**, comme le règle l'assistant : ses voix intégrées (kick accordé à la tonalité en house et techno, kick dur en hard techno / gabber / acidcore, rumble, 808), leurs volumes et leurs effets ;
- le **kit intégré** qui remplace le pack de batterie du style quand il en existe un (`TutoTarget.KITS` : aujourd'hui le pack `dnb` → « Kit : Drum & bass », celui du jump up) ;
- le **son de la 303** du style (sans notes) ;
- les pistes que le cours crée (« Créer la piste Basse / Accords / Mélodie ») prennent le son, le volume et l'effet que l'assistant donne à ce rôle dans le style.

**Le kit du style** (`TutoKit`) : au départ du tuto, si les packs d'où l'assistant prend la batterie du style (ses machines, TR-909…, et son pack à lui) ne sont pas sur le téléphone, une carte propose « Télécharger le kit <style> (taille) » ou « Plus tard ». Une fois là, chaque pad prend le son du pack que l'assistant lui donnerait. **Les exemples jouent toujours les sons de batterie du projet** : avec le pack s'il est chargé, avec les sons intégrés sinon. Hors ligne, tout marche avec les sons intégrés. Le choix est gardé pour le projet.

Tempo, tonalité et swing sont réglés par des étapes.

---

## 7. Les écrans (`spot`)

`tempo`, `tuto.key` (la feuille de tonalité du tuto), `rhythm.pattern` (la grille, aussi pour le swing et les coups), `rhythm.kit`, `notes.track`, `sounds.track`, `sounds.layers`, `song.parts`, `song.add`, `song.automation`, `mixer.strip`, `mixer.master`, `mixer.whatplays`, `acid`, `acid.generate`, `record.voice`, `record.melody`, `share.check`, `share.export`, `share.video`.

---

## 8. Les exemples (`listen`)

| `listen` | Joue |
|---|---|
| `song` | Un extrait du morceau fini : la partie avant le 1er Drop (ou Refrain) dans l'ordre du cours, puis ce Drop. |
| `drums:kick,hat` | Ces pads seuls, avec le kit du tuto, au tempo et au swing du style. |
| `parts:kick,bass,pad,acid` | Batterie, pistes et 303 mêlées. |
| `part:<Partie>` | Cette partie du morceau cible (`Refrain 2` joue le Refrain si elle n'existe pas ; `Fin` joue l'Intro). |
| `vs:<A>:<B>` | La partie A, puis la partie B. |
| `fx:reverb` | Caisse claire et accords sans, puis avec la réverbe. |

Les parties nommées doivent exister dans le morceau de l'assistant pour ce style (la validation le vérifie).

---

## 9. Fabriquer les parties

Une étape `part:<Partie>:<mode>` propose « Créer la partie « X » » : une copie du Groove, n'y gardant que les briques de `keep`.

- `keep` : rôles de batterie (`kick`, `open_hat`, `rumble`…), `perc` (les pads que le style ne nomme pas), `drums` (tous les pads), `bass`, `sub`, `pad`, `lead`, `acid`, ou `*` (tout).
- `lanesFrom` : prend la batterie de cette partie du morceau cible (sur les pads gardés).
- `build` : ce pad accélère mesure après mesure, jusqu'au silence final (la Montée).
- `transpose` : la basse de la partie est à réécrire, déplacée de N demi-tons comptés le long de la gamme (le Drop 2 du jump up).

Modes, combinables avec `+` (`"part:Couplet:nopad+less"`) :

| Mode | La partie… |
|---|---|
| `any` | existe et joue quelque chose |
| `less` | joue moins de briques que la partie la plus pleine |
| `full` | joue autant que la plus pleine, kick compris |
| `nokick` | sans kick |
| `nobass` | sans basse ni sub |
| `nopad` / `nolead` / `noacid` | sans accords / sans mélodie / sans 303 |
| `bass` / `pad` / `lead` / `acid` | avec cette brique |
| `drop` | kick et basse, sans accords (le Drop du jump up) |
| `differs` | différente de la partie dont elle est la seconde (`Drop 2` / `Drop`, `Refrain 2` / `Refrain`) |

**Formes.** N'importe quels noms de parties : Club (`Intro`, `Groove`, `Montée`, `Drop`, `Break`, `Fin`), Rap et Song (`Intro`, `Couplet`, `Refrain`, `Pré-refrain`, `Pont`, `Fin`), un Drop en deux moitiés (`Drop` puis `Drop B` avec `lanesFrom`)… Le morceau peut commencer par le Couplet. `song` impose l'ordre et les durées (`"Couplet:32,Refrain:8,…"`), sans dépendre de l'assistant. Une `Fin` que le cours ne fabrique pas est jouée par l'Intro. Une partie qui revient (`Montée` deux fois) est la même partie.

**Carte du morceau** (`map: true`) : les parties de la 1re étape qui a un `song`, avec l'énergie de la partie du même nom du morceau cible ; une Montée ou un Pré-refrain monte vers la partie suivante, un `Drop 2` / `Refrain 2` a l'énergie de son premier.

---

## 10. Vérifier son cours

```bash
# La validation (tous les fichiers de tutos/) et le parcours « Montre-moi » de chaque cours, Facile et Pro :
flock <lock> ./gradlew --max-workers=3 --no-configuration-cache :project-model:test --tests '*TutoAllCoursesTest*'

# Le parcours dans l'appli, comme un utilisateur (chaque cours, Facile et Pro) :
flock <lock> ./gradlew --max-workers=3 --no-configuration-cache :app:testDebugUnitTest --tests '*TutoJourneyTest*'
# Un seul style : --tests '*TutoJourneyTest*techno*'
```

`TutoAllCoursesTest.everyCourseIsValid` liste, par fichier, ce qui ne va pas (`TutoValidation.problems`) : détecteur inconnu, rôle que le style ne joue pas, partie qu'aucune étape ne fabrique, exemple impossible, note illisible, écran inconnu… Le corriger jusqu'à ce que la liste soit vide.

`TutoJourneyTest` joue le cours dans l'appli : bouton principal, « Je le fais », cases et notes touchées, « Montre-moi » qui ne laisse rien, « Écouter l'exemple » qui sonne, aucune étape cochée avant d'être faite, aucune qui avance seule.

---

## 11. Ce qu'un cours ne peut pas encore dire

- **Triolets** : `triplet:<rôle>` vérifie qu'il y en a un, pas sa place exacte ; pour la place, `lane:<rôle>:rolls` (la ligne de la recette, où `t` / `T` = un triolet sur 2 cases, doit être refaite triolet pour triolet).
- **Une variante dans une partie longue** (couper une ligne 8 mesures sur 32, un mini-break de 2 mesures) : il faut une partie de plus.
- **Le décalage d'un coup, les roulements et la 303** ne s'écrivent qu'en Pro (le Mode simple n'a pas ces réglages).
- **Force des coups** : trois niveaux (X / x / o) dans `drums`, pas de force fine.
- **Sons de batterie hors kit** (conga, cowbell, güira…) : `drums` nomme les pads par leur rôle ; un pad joue le son du kit (ou du pack) à sa place.
- **Glissé 808** : sa durée est celle du style, pas réglable note par note ; pas de pitch bend.
- **Swing par piste** : seulement le swing global.
- **Accords enrichis** (m9, sus2) : la grille se donne en degrés, la couleur vient du style.
- **Packs** : seule la batterie vient du pack du style ; les sons des pistes (basse, accords…) restent ceux du synthé.
