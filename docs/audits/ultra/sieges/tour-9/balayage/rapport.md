# Tour 9 — balayage des 37 tutos (machine, avant tri)

Code testé : `e74dc120` (+ les trois tests du balayage, rien dans l'appli). Outil : `scripts/bench/tuto-sweep.sh [dossier]`
(lance `Tour9TutoSweepValidationTest`, `Tour9TutoSweepTextTest`, `Tour9TutoSweepScreenTest` avec `-Pbench.out`, ne
rend jamais la CI rouge : sans `-Pbench.out`, les trois tests sont sautés). Durée : 1 min 45.

Fichiers bruts à côté de ce rapport : `validation.tsv` (famille 3), `text.tsv` + `text-long200.tsv` (familles 1 et 2,
textes), `screen.tsv` (famille 2 à l'écran), `comptes.txt`.

## Connus retrouvés : 3/3

| Connu (tour 8) | Retrouvé | Où |
|---|---|---|
| maracas, « touche 4 cases » cochée à 1 sur 4 | oui | `validation.tsv` : house, Facile, ch. 12 « transitions », étape 3 « variation », `compte-faux` (+ `montre-moi-court` : « Montre-moi » pose 1 sur 4) |
| maracas, « Déjà fait, rien à toucher » une fois le travail repris | oui | même étape, `deja-fait-faux` (et 716 autres étapes, voir plus bas : c'est une cause unique) |
| P6-grille, ja 200 %, consigne dépliée qui couvre la grille | oui | `screen.tsv` : ja à 200 % sur les deux appareils, et plus large que prévu (toutes les langues, même à 100 %) |

## Comptes

| Famille | Gravité | Lignes |
|---|---|---|
| 3 — deja-fait-faux | grave | 717 (37 tutos, 381 Facile / 336 Pro) |
| 3 — deja-fait-faux | finition | 22 (étapes sans geste demandé) |
| 3 — velocite-non-verifiee | grave | 40 (+1 finition) |
| 3 — compte-faux | grave | 1 |
| 3 — validee-1-sur-n | finition | 3 |
| 3 — montre-moi-court | finition | 5 |
| 3 — jamais-validee / impasse / cochee-sans-geste / plantage | — | 0 |
| 2 — grille-cachee (écran) | grave | 26 |
| 2 — consigne-depliee-cache-grille (écran) | grave | 6 |
| 2 — texte-coupe (écran) | finition | 33 |
| 2 — trop-long-100 (consigne coupée même à 100 % sur 360 dp) | finition | 188 |
| 2 — trop-long-200 (« Lire la suite » à 200 %) | compté, pas listé | voir `text-long200.tsv` |
| 1 — texte-en-francais / traduction-identique / mots-francais | grave | 0 |
| 1 — nom-cite-absent | finition | 53 |

Graves de la famille 3 par tuto (toutes familles) : techno 27, disco 26, house 25, tech_house 25, afro_house 25,
uk_garage 24, liquid_dnb 24, deep_house 23, amapiano 23, drill 23, boom_bap 23, lofi 23, pop 23, detroit 22, jump_up 22,
dubstep 22, minimal 22, speed_garage 22, afrobeats 22, dnb 21, synthwave 21, industrial_techno 20, trance 20, jungle 20,
phonk 20, reggaeton 20, neurofunk 19, riddim 19, trap 19, hard_techno 18, grime 18, jersey_club 18, retro_techno 17,
gabber 16, acid_house 10, acid_techno 9, acidcore 7. La famille 3 ne dépend pas de la langue.

Graves à l'écran par langue (cas joués : ja, fr, de, ar, hi à 200 % ; ja, fr, de à 100 et 130 %) : de 8, fr 8, ja 8, ar 4,
hi 4 — c'est-à-dire tous les cas à 200 % et tous les cas sur 360 dp.

## Graves probables, triés

1. **La grille disparaît derrière le panneau du tuto** (famille 2, écran ; `techno` ch. 3 « Le clap », étape de grille).
   - Sur un téléphone de 360 dp, **même à 100 % et panneau replié**, aucune case de la grille n'est visible : le panneau
     (titre, consigne, bandeau « Télécharger le kit Techno (4 Mo) / Plus tard », « Pourquoi ? », « Lire la suite »,
     « Je le fais », « Écouter l'exemple / Montre-moi », « Précédent / Suivant ») descend jusqu'à la barre du bas.
     À confirmer au tri : sans le bandeau du kit (kit déjà téléchargé), il reste peut-être une rangée.
   - À 200 %, aucune case visible dès l'arrivée, dans les 5 langues, sur les deux appareils, aux deux niveaux.
   - Sur le pliable (690 dp), à 100 % : 16 cases avant « Lire la suite », **0 après** ; à 130 % : 8 avant, 0 après.
     C'est P6-grille, et il touche aussi fr et de, pas seulement ja.
   - Ce que voit l'utilisateur : « touche le pad Clap… » et aucun pad à toucher à l'écran.
2. **« Déjà fait, rien à toucher » alors que l'étape n'est plus faite** (717 étapes, les 37 tutos).
   Une seule cause : `TutoPlay.undone` répond « garder la coche » pour toute étape `changed:` et pour toute étape
   `lane:` / `notes:` hors partie nommée. L'utilisateur qui efface son travail après la coche garde « Déjà fait ».
   Par type de contrôle : lane 367, notes 325, changed 16, offset 4, velocity 3, triplet 2. Un seul lot de réparation.
3. **Compte faux, maracas** : house Facile ch. 12 étape 3 « variation » (« pad Shaker, touche 4 cases ») cochée après
   1 case. Seul cas sur toutes les étapes qui comptent des cases.
4. **Nuance demandée, pas vérifiée** (40 étapes, 21 tutos) : la consigne parle de coups doux / forts, l'étape est cochée
   avec les vélocités remises à plat (ou « Montre-moi » n'en change aucune) et aucune étape `velocity` ne suit.
   Surtout `hat_do`, `kick_do`, `bass_do`, `snare_do` en Pro (ex. detroit Facile ch. 2 étape 4 : « un coup fort, un coup
   doux, sans arrêt »).

## Finitions

- **Montre-moi court / validée à 1 sur n** : deep_house ch. 10 « toms » (Facile et Pro : « touche les 4 cases »,
  « Montre-moi » en pose 2, cochée avec 1), afro_house ch. 10 « toms » (3 sur 4), reggaeton Pro ch. 12 « fill » (2 sur 3,
  cochée avec 1).
- **Consigne coupée même à 100 % sur 360 dp** (188) : par langue ja 25, de 23, hi 21, it 12, fr 11, nl 11, es 11,
  pt-BR 11, pl 11, tr 10, id 10, ko 10, en 9, ar 7, zh-CN 6. Ex. dnb Pro ch. 2 étape 1 (506 signes en français).
- **« Lire la suite » à 200 %** : voulu par conception, compté seulement. Sur 360 dp, de 1 419 (zh-CN) à 2 167 (ja)
  consignes par langue ; même sur 320 dp, des centaines par langue en plus (`text-long200.tsv`, par tuto et langue).
- **Texte coupé à l'écran** (33) : surtout les onglets du haut (« Remix Séparer ») et des lignes du panneau à 200 %.
- **Nom cité absent** (53) : un nom de l'appli cité dans la consigne qui ne correspond pas au nom affiché.
  « Coupure » → l'écran dit « Cutoff » en de (9), « Effets » déclinés en pl (17, « Efektach » contre « Efekty » :
  probablement juste de la grammaire), « à la main », « Caisse », « Mix », « Fin », noms de basses en ar / ja / ko.
- **deja-fait-faux sans geste demandé** (22) : même cause que le grave n° 2, sur des étapes qui ne demandent rien à toucher.

## Famille 1 : rien de grave

Sur 6 405 textes (titres, pourquoi, consignes, « passer ») × 14 langues : aucun texte resté en français, aucune
traduction identique au français, aucun mot français visible (contrôle « touche, pointillé, à toi, ensuite, appui long,
maintenant » partout, et paires d'articles hors langues romanes).

## Limites de l'outil

- Famille 3 : le joueur est `TutoPlay.play`, le même chemin que l'appli ; « compte-faux » ne lit que les consignes qui
  disent un nombre (`4 cases`, `quatre coups`…) sur un pad de batterie.
- Écran : une seule étape de grille (`techno:3`) jouée, 5 langues sur 15, au lieu des 37 × 15 (le coût d'un écran
  Robolectric) ; les textes, eux, sont mesurés partout par `TutoReadability`.
- `nom-cite-absent` ne garde que les noms qui ont leur propre clé de traduction ; les phrases citées sont ignorées.
