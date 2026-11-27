# Batterie définitive : la règle (écrite avant tout calcul)

Écrite le 2026-11-27, avant de calculer la moindre cible. Seuls les comptes de sons par
famille, style et pack étaient connus (pas une médiane, pas un quantile).

Source unique : les tables corrigées `docs/sons/passe2/tout/drum_oneshot.csv` (un son par ligne)
et la contre-mesure `docs/sons/passe2/verif/rar*.csv`. Rien d'autre, aucune estimation.
Script : `scripts/sons/fin/cibles_batterie.py` → `cibles-batterie-definitives.tsv`.

## Familles et styles

- Familles (sous-type de la table) : kick (kick), 808 (kick_808), caisse_claire (snare),
  clap (clap), charley_ferme (hat_closed), charley_ouvert (hat_open), crash (crash), rim (rim),
  tom (tom).
- Styles : house, techno, dnb et dubstep prennent les sons de leur propre style dans la table.
  Tech house et trap : voir (c).
- Un pack = un couple archive × pack (`rarN/pN`). Les noms réels ne s'écrivent jamais.

## (a) Une cible « définitive »

Une cible = une famille × un style × une mesure. Elle est **définitive** seulement si les
cinq conditions tiennent (précision du 2026-11-27, `docs/DECISIONS.md`) :

1. au moins **20 sons** ont une valeur pour cette mesure ;
2. au moins **2 packs** ont **chacun au moins 5 sons** ;
3. **aucun pack** ne porte plus de **90 %** des sons ;
4. la **contre-mesure retrouve la valeur au moins 98 fois sur 100** : parmi ces sons, au moins
   98 % ont `<mesure>_ok = yes` (un son absent de la contre-mesure, « nc », « no » ou
   « one-empty » compte comme désaccord) ;
5. la **médiane refaite sans le plus gros pack** ne sort pas de p25–p75 en bougeant d'au moins
   **1 dB** (mesures en dB : crête, RMS, aigus, attaque/corps) ou d'au moins **20 %** (durée et
   toute mesure en ms ou en Hz). Elle échoue seulement si les deux arrivent ensemble : hors de
   p25–p75 **et** déplacement au-dessus du seuil.

Mesures possibles : durée (`dur_s`), crête (`peak_dbfs`), RMS (`rms_dbfs`), t20 (`t20_ms`),
t60 (`t60_ms`), centre spectral (`centroid_hz`), aigus (`hf_db`), attaque/corps
(`transient_body_db`) ; la note (`pitch_steady_hz`) **pour le kick seulement**.
t40 n'est pas contre-mesuré : il n'est plus une cible.

Sinon : **« pas de cible, on garde le son actuel »**, avec la raison écrite (la première
condition qui manque). Le son actuel est la voix de l'appli
(code 9, « actuel » dans `kChoice`), comme dans la règle du matin.

Quantiles : p10, p25, médiane, p75, p90 par interpolation linéaire sur les valeurs triées
(méthode « linear » habituelle), jamais estimés par une loi normale.

## (b) Le choix d'une variante

Rejoué par `scripts/son/matin_choix.sh` avec les cibles définitives. Adaptations, écrites ici :

- les versions candidates (nouvelle, A, B, deep pour le kick techno) sont rendues par le moteur
  puis mesurées **avec l'outil des tables** (`scripts/sons/mesure.py`, fonction `measure`) :
  mêmes définitions de t20, t60 et note que les cibles (avant, le mesureur du matin avait sa
  propre définition de t20) ;
- mesures du choix : t20 et t60, plus la note pour le kick, **seulement celles dont la cible est
  définitive** (t40 retiré : pas de contre-mesure). Durée du fichier, crête et RMS dépendent du
  rendu et du volume, pas du son ; centre spectral, aigus et attaque/corps sont affichés à titre
  d'indication (les variantes A/B changent surtout la longueur et la hauteur) ;
- écart d'une mesure : 2 |v − T| / (p90 − p10) ; score = moyenne ; le plus petit gagne ;
  moins de 0,05 d'écart avec « nouvelle » : « nouvelle » gagne ;
- **sûr** si le gagnant est le même avec T au p25, à la médiane et au p75 ;
  sinon **serré**, et on garde « nouvelle » ;
- si « nouvelle » retenue tombe hors p25–p75 sur une mesure du choix, sa recette se corrige dans
  `MeasuredDrums.cpp` (et seulement là).

Une famille × style sans aucune mesure définitive parmi t20, t60 (et note) : « actuel ».

## (c) Les styles sans pack à eux : tech house et trap

Aucune des 12 archives n'a de pack tech house, et la trap n'a presque aucun coup de batterie
(1 caisse claire). L'ancien jeu de cibles ne permet pas de « garder l'ancien » : ses packs sont
tous « ? », il n'a donc **aucun pack identifiable** (il en faudrait 2). Règle appliquée :
**suivre le style le plus proche**, et le dire :

- **tech house = house + techno réunies** : la tech house est littéralement entre les deux
  (même tempo que la house, kick et charleys de la techno). Les sons des deux styles comptent,
  avec les mêmes cinq conditions.
- **trap = trap + drift-phonk réunies** : la phonk vient de la trap (808, charleys roulés,
  caisse claire sèche), c'est le style des archives le plus proche. Mêmes seuils.

Ces deux styles sont des **regroupements** : ils passent les mêmes cinq conditions, et le
tableau des cibles l'écrit dans la colonne `statut` (« regroupement house+techno » ou
« regroupement trap+drift-phonk »).

Si le style proche n'atteint pas les seuils, c'est « pas de cible » comme ailleurs.

## (d) Les étiquettes douteuses

Les sons dont l'étiquette est mise en doute par DESACCORDS-A, B ou C sont retirés avant tout
calcul et listés dans `exclusions-batterie.tsv` avec la raison. Seules les étiquettes comptent
(une famille peut-être fausse) : un doute sur la seule hauteur (kicks `r10-p1-582ac7a8de`,
`r9-p3-d439f2d798`) n'exclut pas le son, il fait déjà tomber la note par l’accord (a.4).
DESACCORDS-A ne met aucune étiquette en doute. Les 4 sons de rar6/p1 rangés « hat ouvert »
dont le chemin dit « hat fermé » (DESACCORDS-B) n'ont pas leur clé écrite : ils sont du style
amapiano, qui ne nourrit aucune cible ; ils ne changent donc rien.

## Originaux de la page

Pour chaque famille × style, l'original est le son des tables le plus proche de la cible
définitive : distance = moyenne de |v − médiane| / (p90 − p10) sur les mesures contre-mesurées
de la cible (celles qui sont définitives). D'abord dans le style (ou la réunion du (c)) ;
sinon dans tous les styles (`tous_styles`). Sans cible : le son le plus proche de la médiane
de la famille, tous styles réunis, sur les mesures que la contre-mesure retrouve à 98 %,
marqué `tous_styles` et « à titre d'exemple ».
Boucles : la boucle du style (ou de la réunion du (c)) la plus proche de la médiane des boucles
de ce style, sur les seules colonnes contre-mesurées des boucles (durée, crête, RMS).
Étiquettes exclues : jamais choisies.
