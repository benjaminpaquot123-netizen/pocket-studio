# Plan P6 + P7 : lisibilité du panneau et une consigne par moment

Repérage fait sur `/home/user/scout-p67` (tête détachée 258441802). Rien n'a été modifié dans le dépôt.
Références : audit `docs/audits/ultra/tour-7-fable-tuto-audit.md` §§ 1.5, 3.4, 3.5 et 4 (P6 = D3, P7 = D4) ; rapport `tour-7-fable-repar-rapport.md`.
P8 (`TutoKind`, refonte de `TutoStep` / `TutoValidation` / `TutoPanel`) est en cours dans une autre équipe : ce plan ne suppose **aucune forme de code** pour ces classes. Il fixe des règles, des chiffres, des textes et des contrats. Les noms de fonctions plus bas sont des propositions, à poser là où P8 les attend.

Scripts et sorties (scratchpad de la session) :
- `p6_measure.py` → `p6_output.txt`, `p6_lines.csv` (une ligne par style × niveau × étape × langue, lignes mesurées et fautes).
  Lancer : `python3 -I p6_measure.py <racine-du-dépôt> --csv p6_lines.csv`
- `p7_moments.py` → `p7_output.txt`, `p7_inventaire.md` (314 étapes, par cours, texte « avant » et découpe proposée).
  Lancer : `python3 -I p7_moments.py <racine-du-dépôt> --md p7_inventaire.md`

---

## 1. P6 : la règle de lisibilité

### 1.1 Ce que dit l'audit (§ 3.5), en règle

1. La consigne passe toujours en premier.
2. Dans chaque état du panneau, **au moins une ligne entière** de la consigne est lisible.
3. Si elle est coupée : elle finit par « … » et « Lire la suite » est affiché.
4. Les autres blocs (fiche kit, « Pourquoi ? », exemples, chapitres) se replient **avant** que la consigne soit coupée.
5. Le calcul sort de `TutoPanel.kt` et devient une fonction pure, testable sans Compose.

### 1.2 Les deux fonctions pures proposées

Elles ne dépendent ni de Compose ni de la forme de `TutoStep` : elles prennent des nombres et une chaîne. Elles vont dans `app/.../tuto/TutoReadability.kt`, ou dans `project-model` si P8 y met le panneau logique.

```
enum class PanelState { OPEN, GRID }        // OPEN : 240 dp de place pour les mots ; GRID : cases/croix/boutons à toucher en vue, 120 dp
data class Budget(val maxLines: Int, val charsPerLine: Int)

fun budget(state: PanelState, lang: Lang, fontScale: Float, widthDp: Int): Budget
fun fits(text: String, state: PanelState, lang: Lang, fontScale: Float, widthDp: Int): Boolean   // lines(text) <= maxLines

// Mise en page : ce qui reste visible, sans Compose (mesures injectées)
data class PanelLayout(val wordsLines: Int, val cut: Boolean, val showMore: Boolean,
                       val kitCard: Boolean, val why: Boolean, val examples: Boolean)
fun layout(roomDp: Int, lineHeightDp: Float, textLines: Int, kitDp: Int, whyDp: Int, examplesDp: Int, moreDp: Int = 32): PanelLayout
```

Règles de `layout` (ordre de repli) : exemples, puis fiche kit, puis « Pourquoi ? » ; et seulement après, les mots sont coupés. Si `cut` est vrai, alors `showMore` est vrai, `wordsLines >= 1` et le texte affiché finit par « … ».
`budget` est la projection de `layout` pour un texte seul. Le test de contenu (§ 4) n'utilise que `fits`.

### 1.3 Les constantes (tirées du code actuel)

| Donnée | Valeur | Source |
|---|---|---|
| Marge latérale de la colonne | 14 dp × 2 | `TutoPanel.kt` |
| Place des mots, état OPEN | 240 dp (290 dp avec la carte du morceau) | `most` dans `TutoPanel.kt` |
| Place des mots, état GRID | 120 dp | `GRID_WORDS_MAX` |
| Ligne « ▸ Pourquoi ? » épinglée | 36 dp retirés (écran ≥ 480 dp de haut) | `WHY_PINNED_SCREEN` |
| « Lire la suite » | 32 dp minimum | `MoreWords` |
| Corps de la consigne | bodyLarge, 16 sp, interligne 24 sp, Inter | `Theme.kt` |
| Facteur d'interligne, écritures de secours | ja, zh-CN, ko, ar ×1,1 ; hi ×1,2 | **estimé, à calibrer à l'écran** |
| Largeurs testées | étroit 320 dp, téléphone 360 dp, tablette 600 dp | |
| Tailles de texte | 100 %, 130 %, 200 % | |

`maxLines(state, lang, scale) = floor((place(state) − 36) / (24 × scale × facteur(lang)))`, avec un minimum de 1.

| maxLines OPEN / GRID | 100 % | 130 % | 200 % |
|---|---|---|---|
| Latin (fr, en, de, es, it, nl, pl, pt-BR, tr, id) | 8 / 3 | 6 / 2 | 4 / 1 |
| ja, zh-CN, ko, ar | 7 / 3 | 5 / 2 | 3 / 1 |
| hi | 7 / 2 | 5 / 2 | 3 / 1 |

Caractères par ligne mesurés, téléphone 360 dp (étroit 320 dp entre parenthèses) :

| Langue | 100 % | 130 % | 200 % |
|---|---|---|---|
| fr | 45 (40) | 35 (30) | 22 (20) |
| de | 44 (38) | 34 (29) | 22 (19) |
| ja | 21 (19) | 16 (14) | 10 (9) |
| zh-CN | 23 (20) | 18 (15) | 11 (10) |
| ko | 29 (26) | 23 (20) | 14 (13) |

**Repères d'écriture en français, par phrase de moment** : environ 110 caractères au plus dans l'état GRID (3 lignes) et 170 dans l'état OPEN (6 lignes à 130 %). En japonais, environ 55 caractères au plus dans l'état GRID.

### 1.4 Le budget qui fait échouer le test (Facile)

Une consigne Facile doit tenir (`fits`) dans les trois cas suivants :

1. Téléphone 360 dp, 100 %, dans **son** état (GRID si l'étape montre des cases, des croix ou des boutons à toucher, sinon OPEN).
2. Téléphone 360 dp, 130 %, état OPEN.
3. Étroit 320 dp, 100 %, état OPEN.

À 200 %, et en GRID à 130 %, rien n'est exigé sur la longueur : seule la règle de mise en page s'applique (une ligne entière, « … » et « Lire la suite »). C'est le test d'écran (§ 4) qui le vérifie.
Pro : même mesure, mais en **cliquet** (le nombre de textes hors budget ne peut que baisser). Point de départ : 3 733.

**Calcul de l'état.** Le script déduit l'état du type de vérification de l'étape :
- GRID pour `lane`, `gap`, `fill`, `crash`, `changed:drums`, `velocity`, `triplet`, `offset`, `swing` et tout `changed:` (boutons).
- OPEN pour le reste.

Le vrai test doit demander l'état à la même source que le panneau, c'est-à-dire la « vue de l'étape » de P8. À noter : `levels` (`changed:volumes:…`, dans l'onglet Mix) est compté GRID par le script. Il faut vérifier ce que le panneau fait réellement dans l'onglet Mix.

### 1.5 Ce qui dépasse aujourd'hui (Facile)

- **Textes mesurés : 20 595** (fr plus 14 langues × 1 373).
- **Hors budget : 2 793** textes, soit **337 étapes distinctes**.
- **En français : 203 étapes** (138 en GRID, 65 en OPEN).

Par langue :

| Langue | Hors budget |
|---|---|
| fr | 203 |
| ar | 120 |
| de | 265 |
| en | 156 |
| es | 203 |
| hi | 271 |
| id | 208 |
| it | 191 |
| ja | 204 |
| ko | 124 |
| nl | 186 |
| pl | 206 |
| pt-BR | 217 |
| tr | 167 |
| zh-CN | 72 |

Par contrainte :

| Contrainte | Dépassements |
|---|---|
| téléphone 100 % en GRID | 1 810 |
| téléphone 130 % en OPEN | 1 526 |
| étroit 100 % en OPEN | 65 |
| téléphone 100 % en OPEN | 20 |

Par style (textes Facile hors budget, toutes langues) :

| Style | Textes | Style | Textes | Style | Textes |
|---|---|---|---|---|---|
| techno | 188 | dnb | 145 | jump_up | 141 |
| house | 140 | trap | 131 | liquid_dnb | 130 |
| neurofunk | 128 | jungle | 126 | dubstep | 111 |
| drill | 101 | riddim | 93 | reggaeton | 86 |
| boom_bap | 81 | afrobeats | 78 | pop | 73 |
| minimal | 70 | grime | 69 | hard_techno | 68 |
| lofi | 66 | synthwave | 66 | detroit | 64 |
| gabber | 61 | retro_techno | 57 | industrial_techno | 56 |
| jersey_club | 54 | acid_techno | 48 | trance | 48 |
| phonk | 47 | acidcore | 45 | uk_garage | 45 |
| afro_house | 39 | deep_house | 39 | disco | 30 |
| tech_house | 21 | speed_garage | 22 | amapiano | 18 |
| acid_house | 8 | | | | |

**Recouvrement avec P7 : 139 étapes** sont à la fois trop longues et à plusieurs moments. Le découpage P7 les règle en grande partie. Les **198 étapes « P6 seul »** demandent une réécriture plus courte, sans découpe. Les plus fréquentes : song_order 30, song_phrases 22, ref 16, hat_do 13, ready 11, kick_do 11, snare_do 10, perc_do 10, clap_do 7, crash 7.

### 1.6 Les 20 pires (Facile, téléphone 360 dp, 100 %, dans leur état)

Toutes langues confondues :

| # | Étape | Langue | Lignes / budget |
|---|---|---|---|
| 1 | techno/levels | it | 9 / 3 |
| 2 | techno/levels | fr | 8 / 3 |
| 3 | dnb/fill | fr | 8 / 3 |
| 4 | jump_up/fill | fr | 8 / 3 |
| 5 | dnb/fill | it | 8 / 3 |
| 6 | jump_up/fill | it | 8 / 3 |
| 7 | techno/levels | nl | 8 / 3 |
| 8 | techno/levels | de | 8 / 3 |
| 9 | techno/levels | es | 8 / 3 |
| 10 | dnb/fill | de | 8 / 3 |
| 11 | jump_up/fill | de | 8 / 3 |
| 12 | dnb/fill | id | 8 / 3 |
| 13 | jump_up/fill | id | 8 / 3 |
| 14 | techno/levels | tr | 8 / 3 |
| 15 | techno/levels | en | 8 / 3 |
| 16 | techno/levels | id | 8 / 3 |
| 17 | techno/levels | pl | 8 / 3 |
| 18 | techno/levels | pt-BR | 8 / 3 |
| 19 | dnb/fill | es | 8 / 3 |
| 20 | jump_up/fill | es | 8 / 3 |

Les 20 pires en français :

| Étape | Lignes / budget | Caractères |
|---|---|---|
| techno/levels | 8 / 3 | 338 |
| dnb/fill | 8 / 3 | 330 |
| jump_up/fill | 8 / 3 | 330 |
| neurofunk/fill | 7 / 3 | 294 |
| house/levels | 7 / 3 | 287 |
| trap/hat_do | 7 / 3 | 266 |
| techno/clap_2 | 7 / 3 | 262 |
| techno/gap | 7 / 3 | 255 |
| industrial_techno/levels | 7 / 3 | 253 |
| jump_up/clap_do | 6 / 3 | 265 |
| liquid_dnb/fill | 6 / 3 | 264 |
| jungle/fill | 6 / 3 | 259 |
| riddim/fill | 6 / 3 | 257 |
| dubstep/fill | 6 / 3 | 256 |
| house/kick_do | 6 / 3 | 254 |
| dnb/hat_do | 6 / 3 | 253 |
| gabber/levels | 6 / 3 | 252 |
| hard_techno/levels | 6 / 3 | 252 |
| retro_techno/levels | 6 / 3 | 250 |
| dubstep/hat_do | 6 / 3 | 247 |

La liste complète se filtre dans `p6_lines.csv` (colonne `faults` non vide, `level = f`).

### 1.7 Le défaut de code connu

`foldedLineHeight` mesure « Ag\nAg » : en japonais, la hauteur réelle d'une ligne est plus grande que celle mesurée, et la ligne repliée est coupée en deux. La fonction `layout` doit recevoir la hauteur de ligne **mesurée dans la langue affichée**, avec un texte témoin de l'écriture de la langue : 「あ」 pour ja, 「中」 pour zh-CN, 「한」 pour ko, « अ » pour hi et « ع » pour ar.

---

## 2. P7 : une consigne par moment

### 2.1 Règle

**Moments d'une étape :**
- chaque bouton de préparation que le moteur demande (`TutoNeed` : Bars « Passer à N mesures », Track « Créer la piste … », Part, Name, Lengths, Scene « Aller dans la partie », Copy « Copier sur les mesures suivantes », Course « Poser ce qui manque ») ;
- puis chaque série de gestes.

**Règle Facile :** un seul moment à l'écran, soit un bouton, soit une série, jamais les deux.

**Contrat (abstrait, indépendant de P8) :**
1. La phrase affichée est le libellé du **premier besoin non rempli** (déjà produit par le moteur : `needLabel()`).
2. Sinon, c'est le texte de l'étape.
3. Le JSON **ne nomme plus** les boutons de préparation. La phrase du bouton est générée, donc déjà traduite, et toujours juste quel que soit l'ordre d'arrivée.
4. `TutoValidation` refuse un texte Facile qui nomme un bouton du moteur (« Créer la piste », « Passer à », « Copier sur ») ou qui contient plus d'une série de gestes.

### 2.2 Chiffres (`p7_output.txt`)

- Étapes Facile hors écoute : **998**. Avec au moins un geste : 897. Contenant « puis » : 262.
- Étapes Facile à **plus d'un moment : 314**, toutes à 2 moments. **212 textes français distincts**, dont 35 partagés par plusieurs cours.

Par catégorie :

| Catégorie | Étapes |
|---|---|
| A1 Créer la piste | 95 |
| A2 Passer à N mesures | 24 |
| A3 Copier | 51 |
| A4 Créer partie | 5 |
| B Deux séries | 42 |
| E Explication entre parenthèses | 25 |
| K Tonalité | 37 |
| S Partage | 35 |

Par type de vérification :

| Vérification | Étapes |
|---|---|
| notes | 145 |
| key | 37 |
| shared | 35 |
| changed | 30 |
| lane | 29 |
| gap | 25 |
| fill | 7 |
| part | 6 |

Par cours :

| Cours | Étapes | Cours | Étapes | Cours | Étapes |
|---|---|---|---|---|---|
| acid_house | 2 | acid_techno | 5 | acidcore | 5 |
| afro_house | 8 | afrobeats | 9 | amapiano | 7 |
| boom_bap | 10 | deep_house | 7 | detroit | 9 |
| disco | 6 | dnb | 11 | drill | 10 |
| dubstep | 10 | gabber | 8 | grime | 8 |
| hard_techno | 9 | house | 10 | industrial_techno | 8 |
| jersey_club | 6 | jump_up | 15 | jungle | 12 |
| liquid_dnb | 10 | lofi | 9 | minimal | 11 |
| neurofunk | 12 | phonk | 7 | pop | 8 |
| reggaeton | 9 | retro_techno | 8 | riddim | 7 |
| speed_garage | 9 | synthwave | 7 | tech_house | 8 |
| techno | 9 | trance | 8 | trap | 9 |
| uk_garage | 8 | | | | |

Identifiants les plus répétés : key 37, share 35, lead_do 33, bass_do 31, pad_do 31, levels 30, gap 25, lead_2 23, bass_2 16, kick_do 10, fill 7.

L'inventaire complet, étape par étape (cours / chapitre / étape, vérification, catégorie, texte avant, découpe automatique), est dans `p7_inventaire.md`. Cette découpe automatique est une **ébauche**. Les modèles ci-dessous priment.

**Mesure.** Le script porte en Python le compteur de `Tour7TutoTwoGesturesTest` (verbes en tête de proposition, boutons « Copier » et « Passer à », gestes sans verbe enchaînés par « puis »). Il retire les verbes de lecture (écoute, regarde, vérifie, compare). Une première version ajoutait un moment « moteur » à chaque étape `notes:*` : elle comptait faux (372 étapes, à tort sur les `_2` et `_end`) et a été retirée. Les arrivées dans le désordre sont couvertes par le bouton de besoin de P2.

### 2.3 Modèles de réécriture (français), par catégorie

**A1, Créer la piste (95).** Retirer « touche « Créer la piste X » (…), puis ». Le moteur affiche « Créer la piste X » comme moment 1, puis le texte de l'étape.
- Avant (dnb/bass_do) : « À toi : touche « Créer la piste Basse » (une piste = un instrument de ton morceau), puis les 2 notes en pointillé de la mesure 1 : elles tombent avec le kick. »
- Après : « À toi : touche les 2 notes en pointillé de la mesure 1 : elles tombent avec le kick. »
- La glose « une piste = un instrument… » part dans `why`.

**A2, Passer à N mesures (24).** Retirer « Touche « Passer à 2 mesures », puis ». Le besoin Bars parle.
- Après : « À toi : sur le pad « Kick », touche les cases en pointillé des 2 mesures. »
- Les explications (« une mesure = un tour… ») vont dans `why`.

**A3, Copier (51).**
- Avant : « écris les 4 notes de la mesure 2, puis touche « Copier… » ».
- Après : la phrase de la série seule. Le besoin Copy donne le moment « Copier » une fois la série faite.
- **À vérifier avec P8** : que Copy est bien un besoin **après** la série, et pas seulement avant. Sinon, découper en deux pièces `levels: "f"` (série, puis copie).

**K, Tonalité (37).**
- Après : « À toi : mets le morceau en Fa phrygien, la couleur sombre de la techno. « Je le fais » ouvre la fiche Tonalité. »
- Choisir dans une seule fiche compte pour une série. La glose sur les mots de gamme va dans `why`.
- Le libellé de tonalité varie par cours : un modèle commun, avec la tonalité propre à chaque cours.

**S, Partage (35).**
- Après : « À toi : partage ton morceau. « Je le fais » ouvre l'export. »
- L'explication M4A/WAV, Niveau et Saturation va dans `why`, ou dans la fiche d'export elle-même.
- Texte probablement identique dans 35 cours : à écrire une seule fois (bloc 0).

**E, Explication entre parenthèses (25).**
- Retirer « (touche les 3 croix rouges) » et « dans la partie « … » ». Le besoin Scene s'en charge.
- Après (techno/gap) : « À toi : efface les 3 coups du pad « Charley » sur le dernier temps : un silence juste avant le Drop 2. »
- La phrase « C'est une transition… » va dans `why`.

**A4, Créer partie (5).** Faux positifs : « puis » décrit la partie.
- Réécrire sans « puis », par exemple « … et un temps de silence à la fin ».

**B, Deux séries (42).** Découper en pièces Facile seules (`levels: "f"`, même vérification), avec le mécanisme existant. Il y a déjà 83 pièces de ce genre ; `TutoCourse.stepsFor` les regroupe pour le niveau Pro.
- **levels** :
  - `levels` (f) : « À toi : dans l'onglet « Mix », baisse un peu le curseur de la piste « Accords ». »
  - `levels_lead` (f) : « Maintenant celui de la piste « Mélodie ». »
  - Le texte actuel est gardé en Pro (`textPro`).
  - L'explication des repères (Normal, −2, −∞) va dans `why`.
  - **À vérifier** : la vérification `changed:volumes:pad,lead` doit pouvoir se cocher en deux temps. Sinon, la première pièce a besoin de `changed:volumes:pad`.
- **fill** :
  - pièce 1 : « À toi : pad « Caisse », dernière mesure : appui long sur le dernier coup. »
  - pièce 2 : « Dans le menu, sous « Roulement », touche « ×4 ». »
  - La phrase sur la transition va dans `why`.
- **clap_2** : même schéma (appui long, puis « Doux »).
- **Faux positifs** : hat_do et snare_do classés B (« Astuce… », « 3, 3 puis 2 »). Reformuler sans « puis ».

**Règle de garde pour les découpes.** La première pièce **garde l'identifiant d'origine** : les `done` déjà sauvegardés restent valides. Les pièces suivantes prennent un suffixe (`_b`, `_c`).

### 2.4 Les 198 étapes « P6 seul »

Pas de découpe : on raccourcit. On garde la phrase d'action, on déplace le contexte vers `why`, on retire les gloses répétées d'une étape à l'autre.
Cas lourds :
- `song_order` (30 cours) : la liste « Intro, Montée, Drop… » est longue par nature. Viser OPEN et le budget de 130 %, quitte à confier la liste au contrôle « Ajouter » (cases cochées dans l'ordre).
- `song_phrases` (22).
- `ref` (16).

---

## 3. Découpage du travail

### 3.1 Contraintes qui décident du découpage

Elles viennent de `I18nCatalogTest` :
- `noTranslationIsLeftUnused` : une clé traduite que plus aucun texte n'affiche fait **échouer** le test. Chaque phrase française réécrite oblige donc à **retirer l'ancienne clé** dans les 14 langues. Exception : l'ancienne clé est encore utilisée ailleurs, par exemple un texte partagé par un cours d'une autre famille, ou par `textPro`.
- `catalogsAreValidAndHaveTheSameKeysInEveryLanguage` : même jeu de clés par zone dans les 14 langues. Pas de traduction partielle.
- `aFrenchTextHasOneTranslationAcrossAreas` : une même clé française présente dans deux zones doit y avoir **la même** traduction. Un texte partagé par deux familles (par exemple `share`, `key`) est donc à écrire **une seule fois**, dans une zone commune.
- `TutoCourseTest.everyTextHasItsTranslations` : chaque `title`, `titlePro`, `why`, `text`, `textPro` et `skip` est traduit dans toutes les langues sauf fr.

Les fichiers i18n sont rangés par famille, d'où un découpage par famille :

| Zone i18n | Styles |
|---|---|
| `tuto_techno` (10) | acid_techno, acidcore, detroit, gabber, hard_techno, industrial_techno, minimal, retro_techno, techno, trance |
| `tuto_house` (9) | acid_house, afro_house, amapiano, deep_house, disco, house, speed_garage, tech_house, uk_garage |
| `tuto_bass` (7) | dnb, dubstep, jump_up, jungle, liquid_dnb, neurofunk, riddim |
| `tuto_hiphop` (7) | boom_bap, drill, grime, jersey_club, lofi, phonk, trap |
| `tuto_autres` (4) | afrobeats, pop, reggaeton, synthwave, plus des textes communs à plusieurs familles |
| `tuto` | textes communs du panneau (157 clés) |

### 3.2 Les blocs

**Bloc 0, socle (seul, passe en premier, après P8 ou rebasé sur P8).** Un agent.

Code :
- `app/src/main/java/com/pocketstudio/app/tuto/TutoReadability.kt` (nouveau) : `budget`, `fits`, `layout`.
- `TutoPanel.kt` : appel de `layout`, hauteur de ligne mesurée dans l'écriture de la langue, phrase du moment = premier besoin. Coordonner avec P8.
- `project-model/.../tuto/TutoValidation.kt` : refus « bouton du moteur nommé dans un texte Facile » et « plus d'une série ». Il est **désactivé par une liste d'exceptions** que les blocs 1 à 4 vident ; à la fin du lot, la liste est vide.

Textes partagés entre familles :
- `share` (35), `key` (37, modèle commun avec la tonalité propre au cours), la phrase de `levels` (30) et les gloses déplacées vers `why` qui reviennent partout.
- Écrits dans une nouvelle zone `project-model/src/main/resources/i18n/<lang>/tuto_moments.json` × 14, ajoutée à `I18n.AREAS` dans `project-model/src/main/kotlin/com/pocketstudio/model/i18n/I18n.kt`.
- Les blocs 1 à 4 ne font que **pointer** ces phrases depuis leurs `tutos/*.json`, sans les retraduire.

Tests : ceux du § 4.
Doc : `docs/tuto/AUTHORING.md` (règle « un moment par consigne » et repères de longueur) et une ligne dans `docs/DECISIONS.md`.

**Blocs 1 à 4, contenu, en parallèle après le bloc 0.** Chaque agent ne touche **que** ses `tutos/*.json` et sa zone i18n dans les 14 langues :

| Bloc | Fichiers | Étapes P7 | Textes P6 hors budget (Facile, toutes langues) |
|---|---|---|---|
| 1 Techno | `tutos/{acid_techno,acidcore,detroit,gabber,hard_techno,industrial_techno,minimal,retro_techno,techno,trance}.json`, `i18n/*/tuto_techno.json` | 80 | 705 |
| 2 House | `tutos/{acid_house,afro_house,amapiano,deep_house,disco,house,speed_garage,tech_house,uk_garage}.json`, `i18n/*/tuto_house.json` | 65 | 362 |
| 3 Bass | `tutos/{dnb,dubstep,jump_up,jungle,liquid_dnb,neurofunk,riddim}.json`, `i18n/*/tuto_bass.json` | 77 | 874 |
| 4 Hip-hop + autres | `tutos/{boom_bap,drill,grime,jersey_club,lofi,phonk,trap,afrobeats,pop,reggaeton,synthwave}.json`, `i18n/*/tuto_hiphop.json`, `i18n/*/tuto_autres.json` | 92 | 852 |

Le bloc 3 a le plus de texte trop long (fill, hat_do) ; le bloc 4 le plus d'étapes. C'est équilibré à ±20 %.

Règles communes aux blocs 1 à 4 :
- Un texte trouvé aussi dans une autre famille ne se change **pas** sur place : il se signale au bloc 0, ou au chef.
- Retirer de sa zone les clés devenues orphelines, dans les 14 langues, dans le même commit.
- Vider ses lignes dans la liste d'exceptions du validateur.

### 3.3 Traductions (14 langues)

- La clé est la phrase française : toute phrase changée crée 14 nouvelles clés, et l'ancienne clé est retirée.
- Volume estimé :
  - environ 212 textes P7 distincts, moins environ 70 portés par le bloc 0 ;
  - plus environ 150 textes P6 seuls (198 étapes, avec des répétitions) ;
  - plus environ 120 phrases `why` nouvelles ou rallongées ;
  - soit **environ 400 phrases × 14 ≈ 5 600 traductions**.
- Les agents traduisent eux-mêmes, dans le ton existant : « Your turn: », « Du bist dran: », « Tocca a te: », « Te toca: », « Sua vez: », « Nu jij: », « Twoja kolej: », « Sıra sende: », « Giliranmu: ».
- Avant d'écrire, ils reprennent les noms d'onglets et de boutons tels qu'ils sont déjà traduits dans `common`, `tuto`, etc. Le test `theWordsOfTheTutoPanelAreTheInterfacesWords` le vérifie.
- **Mesurer après traduction** : la règle P6 s'applique à chaque langue. L'allemand, le hindi et le pt-BR débordent le plus. On raccourcit la traduction, jamais en coupant le sens.
- Gardes existantes à garder vertes :
  - `KoreanRegisterTest` : pas de « 너 », pas de banmal ;
  - `Tour7LanguagesTest` : hindi sans « bar » en lettres latines, noms des pads en pt et es, explications de Plaits et d'Acid en japonais ;
  - les formes plurielles.

---

## 4. Tests à ajouter

| Test (module) | Ce qu'il affirme |
|---|---|
| `TutoReadabilityBudgetTest` (app, test pur) | Pour chaque cours, chaque étape Facile, fr + 14 langues : `fits` dans les 3 cas du § 1.4. Message : cours/étape/langue, lignes mesurées et budget. Pro : nombre hors budget ≤ seuil figé (cliquet). |
| `TutoReadabilityRuleTest` (app, pur) | `layout` : (a) `wordsLines >= 1` dans tous les cas, même place minimale à 200 % ; (b) `cut ⇒ showMore` ; (c) repli dans l'ordre exemples → kit → pourquoi → coupe des mots, donc jamais de coupe tant qu'un autre bloc est visible ; (d) un texte qui tient n'est jamais coupé. Tableau de cas, pas d'aléatoire. |
| `TutoLineHeightByScriptTest` (app, Robolectric) | La hauteur de ligne passée à `layout` en ja, zh-CN, ko, hi et ar est ≥ celle mesurée sur un caractère témoin de l'écriture (corrige « Ag\nAg »). |
| `TutoPanelReadabilityShotsTest` (app, Roborazzi) | Matrice états {OPEN, GRID, replié} × {fr, ja, de, ko} × {100, 130, 200 %} × {320, 360, 600 dp}, sur les pires étapes du § 1.6. Assertion sémantique, en plus de la capture : le nœud de la consigne est visible, avec au moins une ligne entière ; s'il est tronqué, « Lire la suite » existe. **Captures à regarder soi-même.** |
| `TutoOneMomentTest` (app ou model), remplace `Tour7TutoTwoGesturesTest` | Pour chaque étape Facile : (a) le texte ne nomme aucun bouton du moteur ; (b) il contient au plus une série (compteur actuel, plafond 1, verbes de lecture exclus) ; (c) depuis chaque arrivée de `TutoShowMeEverywhereTest`, la phrase affichée est **soit** un libellé de besoin, **soit** le texte, jamais les deux. (c) compte les moments du moteur et non les verbes français : c'est ce que demande l'audit. |
| `TutoValidationMomentTest` (model) | `TutoValidation` refuse un texte Facile « touche « Créer la piste Basse », puis … » et un texte à deux séries ; accepte une pièce `levels: "f"` à une série ; la liste d'exceptions est vide à la fin du lot. |
| `TutoSplitKeepsIdsTest` (model) | Pour chaque étape découpée, la première pièce garde l'ancien identifiant ; en Pro, `stepsFor` donne la même liste d'identifiants qu'avant (liste figée en fixture) ; un `done` ancien coche toujours la même étape. |
| Existants, à garder verts | `TutoShowMeEverywhereTest` (P1, fichiers de fautes vides), `TutoCourseTest.everyTextHasItsTranslations`, `I18nCatalogTest` (clés inutilisées, mêmes clés partout, une traduction par clé), `KoreanRegisterTest`, `Tour7LanguagesTest`, `Tour6TutoPanelFit*Test`, `Tour6TutoPanelMoreWordsTest`, `Tour6TutoReadAllTest`, `Tour7TutoReduceTest`, `Tour7TutoPanelTopWholeTest`, `TutoJourneyTest`. |
| Facultatif, souple | À 200 % en français, la première ligne contient le verbe d'action (on lit quoi faire même replié). En avertissement seulement. |

## 5. Risques

1. **Dépendance à P8.** La phrase du moment (premier besoin) et l'état OPEN/GRID passent par le modèle de vue de P8. Il faut lancer le bloc 0 **après** la fusion de P8, ou le rebaser dessus. Les blocs 1 à 4 (contenu) ne dépendent que de la règle et peuvent commencer avec la liste d'exceptions.
2. **Mesure estimée pour les écritures non latines.** Largeurs ja, zh, ko, ar et hi, et interlignes ×1,1 / ×1,2 : à calibrer sur des captures avant de figer les seuils. Sinon, risque de faux rouges (ko et ar sont peut-être sous-estimés) ou de faux verts (hi). Le test pur doit utiliser des tables calibrées par le test d'écran.
3. **État de l'étape déduit du type de vérification.** `levels` compté GRID dans l'onglet Mix est peut-être faux. Prendre l'état à la même source que le panneau.
4. **Découpes d'étapes.** Le nombre d'étapes change en Facile, avec lui la progression des chapitres et les captures de chapitres. Les `done` sauvegardés restent valides si la première pièce garde l'identifiant. Une vérification qui ne se coche pas en deux temps (`changed:volumes:pad,lead`) demande une vérification par pièce, ce qui touche au moteur des vérifications et donc à P8.
5. **Besoin Copy après la série** (A3, 51 étapes) : à confirmer, sinon il faut découper.
6. **Volume de traduction** (environ 5 600 chaînes) et qualité : relecture par un sous-agent neuf par langue difficile (ja, ko, ar, hi). Les débordements en de, hi et pt-BR obligent à raccourcir après traduction.
7. **Clés partagées entre familles.** Elles créent des conflits de fusion et des échecs de `aFrenchTextHasOneTranslationAcrossAreas` / `noTranslationIsLeftUnused`. Il faut les sortir dans le bloc 0 (`tuto_moments`). Une ancienne clé encore utilisée par un `textPro` ne doit **pas** être retirée.
8. **Captures Roborazzi.** Beaucoup de captures tuto changent : refaire et **regarder**, sans confondre avec les captures hors tuto déjà rouges depuis l'intégration (voir le rapport fable-repar).
9. **Recouvrement des tests de comptage.** `Tour7TutoTwoGesturesTest` (plafond 2) et le nouveau `TutoOneMomentTest` (plafond 1) : supprimer l'ancien dans le même commit que le nouveau, pour ne pas garder deux règles contradictoires.
10. **Texte Pro.** Retirer un bouton du texte Facile ne doit pas changer le Pro : si `textPro` est absent, le Pro lit `text`. Avant de changer un `text`, il faut figer l'ancien comme `textPro` partout où le Pro en dépend.
