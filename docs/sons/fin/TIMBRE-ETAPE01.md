# Timbre : étapes 0 et 1

Plan : `RECHERCHE-TIMBRE.md` §6. Table : `timbre-familles.tsv`. Outil : `scripts/son/timbre/collection.py`
(`mesure`, `table`, `copie`, `etape1`), vérifié par `selftest_collection.py`. Aucun son, aucun nom de
pack, aucune mesure d'un son précis dans le dépôt : seulement des statistiques de famille.

## Ce qu'on a reçu

- 14 liens, 7 archives différentes (les autres sont des copies, reconnues par leur nom ou leur empreinte).
- 787 sons de batterie dans le périmètre (9 familles, 6 styles, exclusions retirées), venant de 8 packs ;
  0 son illisible. Deux archives n'ont aucun son dans la table de la collection : on n'a rien mesuré dedans.
- Par famille : kick 225, caisse claire 154, clap 177, charley fermé 146, charley ouvert 45, crash 37,
  808 2, rim 1, tom 0.

## Étape 0 : la table de famille

- Accord mesure / contre-mesure, sons comptés une fois : 99,7 % sur 15 287 valeurs (critère : 98 %).
  La moins bonne mesure, la tenue des graves (t20_low), est à 97,9 % : juste sous le critère.
  22 cases sur 1 080 sont sous 98 % ; 12 portent « pas de cible (4) », les 10 autres échouaient avant.
- 182 cibles définitives, réparties sur 15 cases famille × style sur 54. Les 39 autres n'ont pas de
  cible : trop peu de sons (808, rim, tom, crash et charley ouvert presque partout), un seul pack,
  ou une médiane qui bouge sans le plus gros pack.
- Seuil anti-copie (5e centile du plus proche voisin, carte 25 bandes × 9 tranches, en dB) :
  kick 1,07 ; caisse claire 1,96 ; clap 2,14 ; charley fermé 1,85 ; charley ouvert 1,55 ; crash 2,82.
  Pas de seuil pour 808, rim et tom (moins de 10 sons).
- Essai de copie (un son redessiné de plus en plus finement) : la carte 25 × 9 l'attrape dès une grille
  de 49 × 44, le garde « un voisin sort du lot » dès 22 × 24, le son lui-même est attrapé par tous les
  gardes. Les cartes plus fines (64 × 24, 128 × 48) et le test de soustraction ne l'attrapent qu'à
  l'identique. On garde donc la carte 25 × 9 et le garde du voisin dans la règle.

## Licences

- Les archives venaient toutes du même vendeur. Aucun texte de licence n'a été relu avant leur
  effacement (quelques fichiers texte étaient présents, ouverts par personne).
- Règle suivie quand même : on ne publie aucun son, aucun extrait et aucun nom, seulement des
  statistiques de famille. Les sons de l'appli restent fabriqués par nos formules.

## Étape 1 : la règle rejouée sur les 54 sons de l'appli

- Longueurs (t20, t60) dans la fourchette : 23 sur 23 (critère tenu).
- Cases avec une cible de timbre : 15. Gagnant « sûr » (le même à p25, médiane et p75) : 8 sur 15,
  soit 53 % (critère : 80 %).
- À la médiane, le son actuel de l'appli (« nouvelle ») gagne 12 fois et la version B 3 fois
  (kick techno, en « serré », puis caisse claire dnb et clap dnb, en « sûr »).
- Les 7 cases « serrées » : kick techno, kick dubstep, caisse claire dubstep, clap techhouse,
  clap techno, clap dubstep, charley fermé dnb. Le gagnant y change selon la cible choisie.
- Colonne « dans la fourchette » : elle juge le son actuel de l'appli, pas le gagnant. Sur les cases
  sûres, le son actuel a la moitié de ses mesures ou plus dans p10–p90, sauf caisse claire dnb (1 sur 9).

## Ce qui n'est pas vert

- « Sûr » à 53 %, loin des 80 % demandés : on ne peut pas encore changer de son sur la foi du timbre.
- 39 cases sur 54 sans cible de timbre ; 808, rim et tom n'ont presque aucun son dans la collection.
- Licences non relues (archives effacées) : à relire si les packs reviennent.
- Caisse claire dnb : aucun candidat n'est proche (notes toutes au-dessus de 2 ; B a 0 mesure sur 9).
- Tenue des graves à 97,9 % d'accord : à exclure de la règle ou à refaire avant l'étape 2.
- Kick house, kick trap, clap trap et les charleys de trap n'ont qu'un seul pack : pas de cible.

## Étapes 0-1, tour 2 (arrêté avant la fin)

Chantier arrêté le 27 novembre sur décision de Benjamin : on repart de zéro avec Essentia, dans une
autre discussion. Ce qui suit est une note de passage, pas un résultat validé. Les chiffres viennent
d'un rejeu provisoire (rangement « plat », où quelques sons sont mal rattachés à leur case) ; la
table `timbre-familles.tsv` n'a pas été refaite et aucune critique à l'aveugle n'a eu lieu.

**Longueurs par bande (t20_low, t20_mid, t20_high) : cause prouvée.** L'ancienne contre-mesure
passait par le même masque de bande et le même filtre que la mesure : elle n'était pas indépendante.
Sur les 10 désaccords de t20_low (463 accords sur 473, 97,9 %), 9 sont des sons où le grave pèse
entre -30 et -25 dB du son entier. Avec un filtre qui ne partage rien (Butterworth, phase nulle),
l'accord dépend de la pente du filtre partout où la bande ne porte que le bord du son, et pas là où
elle porte son corps : grave des kicks 224 sur 225 aux ordres 2, 4 et 8 ; médium des kicks 5, 171
puis 214 sur 225. Une longueur de bande n'appartient au son que là où la bande porte son corps ;
ailleurs elle mesure la jupe du filtre.

**Correction faite dans le code** (commits « rouge » puis « vert ») : `collection.counter_bands`,
contre-mesure indépendante d'ordre 4, pour les trois bandes ; la règle des 98 % n'a pas été touchée,
c'est elle qui trie. En provisoire, six lignes perdent « définitive » : t20_mid des kicks techhouse,
dnb et dubstep (64 à 80 %), t20_low de la caisse claire dubstep (93 %), t20_mid du charley fermé
techhouse et techno (89 à 93 %). Le t20_low des kicks reste (98 à 100 %), comme tous les t20_high.

**Témoin Essentia** (trames, `FrequencyBands`, même lecture) : même image. Grave des kicks 224 ou 225
sur 225, médium des claps 177 sur 177, aigu des caisses claires, claps, charleys et crashs 100 %.
Médium des kicks : 3, 95 puis 51 sur 225 pour des trames de 256, 1024 et 4096 points. Aucune taille
de trame ne convient aux trois bandes à la fois. `scripts/son/timbre/tiers_essentia.py` contient cet
essai ; ce fichier a été écrit mais pas rejoué tel quel avant l'arrêt.

**Les 7 cases serrées** (verdict au quartile bas / médiane / quartile haut) :
- kick dubstep : cause prouvée, le t20_mid (mesure de jupe) faisait basculer le quartile haut ; sans
  lui la case devient sûre (« nouvelle »), à confirmer sur la table refaite.
- kick techno : 30 sons, 2 packs, 5 mesures, aucune longueur ; le gagnant change quand on retire un
  pack ou une seule mesure, et même la médiane n'est pas tranchée. Trop peu de matière.
- clap techhouse : écart de 0,011 à la médiane, c'est la règle d'égalité qui décide. House seul et
  techno seul donnent le même verdict : le regroupement n'est pas la cause.
- clap techno (5 mesures), clap dubstep (8), charley fermé dnb, caisse claire dubstep : la médiane est
  nette, les quartiles basculent sur l'axe long/court (B au quartile bas, A au quartile haut). La
  fourchette est simplement plus large que l'écart entre candidats. Pas de faute d'outil trouvée.

**Compte provisoire : 9 cases sûres sur 15 (60 %), sous les 80 %.** Aucune règle n'a été touchée pour
monter. Cases sûres : kick techhouse, kick dnb, kick dubstep (à confirmer), caisse claire dnb (B),
clap dnb (B), charley fermé techhouse, techno et dubstep, crash dubstep.

**Pour les autres instruments.** Valent pour tout : une contre-mesure ne doit partager ni filtre ni
masque avec sa mesure ; une mesure par bande ou par partiel ne compte que là où le son a son corps ;
peu de sons, peu de packs ou peu de mesures donnent un gagnant instable ; juger un son réglé sur la
médiane de la même collection tourne en rond. À vérifier dans l'outil des instruments : ses deux
chemins d'enveloppe partagent leurs lecteurs de temps. Ne valent que pour la batterie : les trois
bandes fixes, les candidats A, B, « nouvelle » et « deep », le regroupement techhouse, les longueurs
de coups uniques.

**Ce qui n'est pas vert** : 60 % au lieu de 80 % ; table non refaite ; pas de critique à l'aveugle ;
le témoin Essentia non rejoué depuis le fichier commité ; Essentia pas encore dans `THIRD_PARTY.md`.

## Critique

Relecture à l'aveugle par un sous-agent neuf, qui a recalculé avec son propre script.
- Confirmés : 787 sons et le compte par famille ; 182 cibles sur 15 cases ; seuil kick 1,07 ;
  8 sûrs sur 15 ; longueurs 23 sur 23 ; médiane 12 « nouvelle » contre 3 B ; les 7 cases serrées.
- Corrigé : l'accord affiché d'abord (99,8 % sur 18 378) recomptait deux fois les sons house et techno
  dans la case techhouse. Sur sons uniques : 99,7 %, et la tenue des graves tombe à 97,9 %.
- Corrigé : « 1 mesure sur 9 » parlait du son actuel, pas du gagnant B (0 sur 9).
- Reste vrai : 3 cases techhouse sur 15 (55 cibles sur 182) réutilisent les sons house et techno.
  Le 8 sur 15 compte donc des cases qui ne sont pas indépendantes.
- Reste vrai : l'essai de copie repose sur une seule caisse claire inventée, avec des seuils
  calibrés sur cette famille. « On garde la carte 25 × 9 » est un indice, pas une preuve. Les cartes
  fines sont peut-être seulement mal réglées.
- Reste vrai : la note des candidats vient des fichiers bruts, la fourchette du rendu de l'appli.
  Les deux diffèrent un peu, ce qui est peut-être un simple décalage.

## Rejouer

    python3 -I scripts/son/timbre/collection.py mesure <dossier> --rar auto --out <hors dépôt>.jsonl
    python3 -I scripts/son/timbre/collection.py table <lignes>.jsonl ... --out docs/sons/fin/timbre-familles.tsv --carte <hors dépôt>
    python3 -I scripts/son/timbre/collection.py copie
    python3 -I scripts/son/timbre/collection.py etape1 docs/sons/fin/timbre-familles.tsv <sons bruts> <render_voice> --out <hors dépôt>
