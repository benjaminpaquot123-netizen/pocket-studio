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
- Essai de copie (un son inventé, redessiné de plus en plus finement), sur trois familles :
  - caisse claire : la carte 25 × 9 l'attrape dès une grille de 49 × 44, le garde « un voisin sort
    du lot » dès 22 × 24 ; les cartes plus fines et le test de soustraction seulement à l'identique ;
  - kick : seuil de la carte 0,98 ; les copies redessinées restent à 2,41 à 2,85, une copie
    transposée d'un demi-ton à 3,75 : aucune grille n'est attrapée ;
  - charley fermé : seuil 0,84 ; même à la grille la plus fine (513 × 286), la copie est à 1,16 ;
    seuls la carte 128 × 48, le garde du voisin et le test de soustraction l'attrapent, et
    seulement à cette grille-là.
  - Donc « on garde la carte 25 × 9 » ne tient que pour la caisse claire : en kick et en charley,
    la carte ne reconnaît pas une copie redessinée. Le garde anti-copie est à refaire avant de
    s'en servir (carte plus fine par famille, ou garde par la forme d'onde).

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

## Critique

Relecture à l'aveugle par un sous-agent neuf, qui a recalculé avec son propre script.
- Confirmés : 787 sons et le compte par famille ; 182 cibles sur 15 cases ; seuil kick 1,07 ;
  8 sûrs sur 15 ; longueurs 23 sur 23 ; médiane 12 « nouvelle » contre 3 B ; les 7 cases serrées.
- Corrigé : l'accord affiché d'abord (99,8 % sur 18 378) recomptait deux fois les sons house et techno
  dans la case techhouse. Sur sons uniques : 99,7 %, et la tenue des graves tombe à 97,9 %.
- Corrigé : « 1 mesure sur 9 » parlait du son actuel, pas du gagnant B (0 sur 9).
- Reste vrai : 3 cases techhouse sur 15 (55 cibles sur 182) réutilisent les sons house et techno.
  Le 8 sur 15 compte donc des cases qui ne sont pas indépendantes.
- Tour 2 : l'essai de copie couvre maintenant kick et charley fermé ; il contredit « on garde la
  carte 25 × 9 » hors caisse claire (voir étape 0).
- Reste vrai : la note des candidats vient des fichiers bruts, la fourchette du rendu de l'appli.
  Les deux diffèrent un peu, ce qui est peut-être un simple décalage.

## Rejouer

    python3 -I scripts/son/timbre/collection.py mesure <dossier> --rar auto --out <hors dépôt>.jsonl
    python3 -I scripts/son/timbre/collection.py table <lignes>.jsonl ... --out docs/sons/fin/timbre-familles.tsv --carte <hors dépôt>
    python3 -I scripts/son/timbre/collection.py copie
    python3 -I scripts/son/timbre/collection.py etape1 docs/sons/fin/timbre-familles.tsv <sons bruts> <render_voice> --out <hors dépôt>
