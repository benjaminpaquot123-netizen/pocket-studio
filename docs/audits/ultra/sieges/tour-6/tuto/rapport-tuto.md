# Tour 6 : balayage des tutos (Facile et Pro, 15 langues)

## En bref

- Les 37 tutos se jouent jusqu'au bout par les gestes, en Facile et en Pro, dans les 15 langues : 1 110 parcours, tous réussis.
- Aucune traduction de tuto ne manque, aucune phrase n'est restée en français.
- 6 défauts, tous moyens, aucun bloquant ni grave.
- Le plus visible : dans le plan du morceau, les noms des parties sont coupés sur petit téléphone (« Breal » pour « Break »), surtout à 130 % et en japonais, coréen et chinois.
- L'appli n'a pas été modifiée : les défauts sont rangés dans deux listes, et les deux nouveaux tests échouent si un défaut apparaît ou disparaît.

## Ce qui existait déjà

| Test | Ce qu'il couvre |
|---|---|
| `TutoJourneyTest` (appli) | les 37 styles × Facile et Pro, joués par les gestes ; en français seulement, sauf si on lui donne d'autres langues |
| `TutoAllCoursesTest` (modèle) | les 37 styles × Facile et Pro, faits par « Montre-moi », sans écran |
| `TutoCourseTest` | chaque texte des tutos a une traduction dans les 14 autres langues (existence seulement) |
| `TutoPanelFitsTest` | les 8 étapes les plus longues de chaque langue, dessinées dans le panneau, en paires (appareil, police) |
| `TutoBigFontTest`, `TutoScreenTest`, `TutoChapterEndTest`… | des cas précis (grande police, fin de chapitre, kit…) |

Ce qui manquait : les gestes dans les autres langues, chaque étape dessinée (pas seulement les 8 plus longues) et la qualité des traductions.

## Ce qui est joué, ce qui est seulement lu

**Joué par les gestes** (`TutoJourneyTest`, `-Ptuto.langs=` avec les 14 autres langues), pour 37 styles × 2 niveaux × 15 langues. Pour chaque étape :
- elle se termine ;
- « Bravo » et « Étape suivante » apparaissent ;
- rien n'avance tout seul ;
- « Montre-moi » ne laisse rien ;
- l'exemple joue un son ;
- les cases à toucher sont visibles au-dessus du panneau.

Résultat : 1 110 parcours sur 1 110 réussis, en 67 minutes.

**Dessiné** (`TutoSweepPanelTest`, nouveau) : chaque étape différente, soit 2 396 panneaux par cas, sur le plus petit téléphone (phone360), à 100 % et 130 %, dans les 15 langues, l'arabe de droite à gauche. Le Mode simple est allumé en Facile, éteint en Pro. Le test cherche les mots coupés, les textes l'un sur l'autre, et les consignes qui défilent sans « Lire la suite ».

**Lu seulement** (`TutoSweepTextTest`, nouveau) : 6 288 textes, soit 2 664 phrases françaises différentes, dans 14 langues. Cela couvre les titres, les consignes, « Pourquoi ? », « ce qui manquera », et les mots du panneau lui-même. Le test vérifie que chaque texte :
- a une traduction ;
- n'est pas resté en français, ni en anglais dans une autre langue ;
- ne garde pas de mots français ;
- garde les mêmes nombres à remplir que le français ;
- est bien dans l'écriture de la langue (hindi, arabe, japonais, coréen, chinois).

**Ni joué ni dessiné** : « Pourquoi ? » ouvert, la ligne repliée et « Bravo » ne sont pas contrôlés pour les coupures (leurs textes sont lus). Le son lui-même n'est pas écouté : le banc ne sait que dire si un son part.

## Défauts trouvés (6, tous moyens)

| # | Défaut | Où | Gravité |
|---|---|---|---|
| 1 | Plan du morceau : nom de partie coupé dans sa case (« Breal », « Intro » collé au bord) ; le nom entier est écrit dessous | toutes les langues à 130 % ; dès 100 % en japonais, coréen, chinois, polonais, turc (5 786 étapes × langues) | moyen |
| 2 | Plan du morceau : chiffre de mesures qui dépasse sa case en arabe (« 16 »), et « 끝 » en coréen | arabe et coréen à 130 % (193) | moyen |
| 3 | « Lire la suite » posé sur « ▸ Pourquoi ? » : on voit la moitié du bouton, le toucher lit la suite au lieu d'ouvrir l'explication | 25 étapes avec le plan du morceau, à 130 %, 11 langues | moyen |
| 4 | Hindi : 22 titres de chapitre restés en anglais (« Snare », « Hi-hats », « Cymbals », « Key » pour « Tonique »…) | tous les styles qui les ont | moyen |
| 5 | Chinois : 11 titres restés en anglais (« Lead », « Hoover Riff », « Chiptune Lead »…) | idem | moyen |
| 6 | Arabe : dans le plan du morceau, les noms des parties restent en lettres latines (« Intro », « Drop », « Break », « Build-up »), alors qu'ils sont traduits en japonais (vu sur image, pas compté par le test) | arabe | moyen |

**Pas des défauts** : 33 mots identiques à l'anglais ou au français, qui sont le mot employé dans la langue : « Pro », « La 303 », « Mastering », « Stop » en polonais, « Later » en néerlandais, « Bar » en indonésien. Ils sont listés dans le tableau avec la mention « pas un défaut ».

**Fausse alerte écartée** : le détecteur signalait 54 consignes coupées en japonais. Sur les images, elles s'affichent en entières (la boîte de ligne de la police japonaise dépasse, pas les lettres). Le test a été corrigé pour ne plus les compter (`allowed()`).

## Fichiers

- `tableau-tuto.tsv` : chaque constat, une ligne : source (gestes / panneau / texte), langue, police, niveau, style, étape, ce qui est vu, gravité.
- `connus-panneau.tsv`, `connus-textes.tsv` : les listes des défauts connus que les deux tests comparent.
- Tests : `app/src/test/java/com/pocketstudio/app/tuto/TutoSweepPanelTest.kt` et `TutoSweepTextTest.kt`. Avec `-Proborazzi.test.record=true`, le premier panneau de chaque sorte de constat est enregistré en image (`app/build/tuto-sweep/images/`).
- Décisions : `docs/decisions/tour-6-tuto.md`.

## Pour relancer

```
scripts/bench/gradle-file.sh :app:testDebugUnitTest --tests "com.pocketstudio.app.tuto.TutoSweep*"        # 4 min
PS_STALL_MIN=150 scripts/bench/gradle-file.sh :app:testDebugUnitTest --tests "com.pocketstudio.app.tuto.TutoJourneyTest" -Ptuto.langs=en,nl,es,pt-BR,de,it,pl,tr,id,hi,ar,ja,ko,zh-CN   # 67 min
```

`PS_STALL_MIN` : le parcours n'écrit rien pendant plus de 20 minutes. Sans ce réglage, le lanceur le prend pour bloqué et l'arrête (deux essais perdus ici).
