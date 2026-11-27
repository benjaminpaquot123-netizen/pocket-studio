# Batterie définitive : conclusion

Règle : `docs/sons/fin/REGLE-BATTERIE.md` (cinq conditions). Cibles : `cibles-batterie-definitives.tsv`
(101 cibles définitives, 355 « pas de cible » sur toutes les mesures). Choix rejoué par
`scripts/son/matin_choix.sh`, qui finit par « table du moteur : identique ».
« Actuel » = la voix de l'appli, qui reste. « Sûr » = le gagnant ne change pas avec la cible au
p25, à la médiane et au p75 ; « serré » sinon.

## Style par style

1. **House** : tout reste actuel. Aucune famille n'a de cible définitive (un seul gros pack ou trop peu de sons).
   Changement : le kick, le clap, le charley fermé, le tom et le crash du matin repartent à l'actuel.
2. **Tech house** (house + techno réunies) : kick, clap et charley fermé nouveaux, tous serrés ;
   le reste actuel. Changement : charley ouvert, tom et crash repartent à l'actuel ; recette du kick ajustée.
3. **Techno** : clap et charley fermé nouveaux, sûrs ; le reste actuel.
   Changement : le kick repart à l'actuel (sans le plus gros pack, la médiane bouge trop) ;
   recette du charley fermé ajustée ; charley ouvert, tom et crash à l'actuel.
4. **Trap** (trap + drift-phonk) : tout reste actuel, aucune cible définitive.
   Changement : le kick, la caisse claire, les charleys, le tom et le crash du matin repartent à l'actuel.
5. **Drum and bass** : kick, clap et charley fermé nouveaux (serrés), caisse claire version A
   (plus grave et plus longue), sûre ; le reste actuel. Changements : le clap gagne une cible (il
   était actuel), la caisse claire passe de « nouvelle » à A, recette du kick ajustée ;
   charley ouvert et tom à l'actuel.
6. **Dubstep** : kick, charley fermé et crash nouveaux (sûrs), clap et caisse claire nouveaux
   (serrés) ; charley ouvert, tom, rim et 808 actuels. Recettes du kick et du crash ajustées.
7. Les styles voisins suivent comme avant : deep house → house, minimal et hard techno → techno,
   drill → trap, liquid → drum and bass, riddim → dubstep.
8. 808, charley ouvert, rim et tom : actuels partout (aucune cible définitive).

## Ce qui change par rapport au matin, en bref

- Beaucoup moins de sons remplacés : 14 cases sur 54 au lieu de 34 (table `kChoice` :
  les autres valent 9, « garder »).
- Les cibles ne portent plus que sur la longueur à −20 dB et à −60 dB ; t40 et la note du kick
  n'ont pas de contre-mesure fiable et ne comptent plus.
- Chaque cible vient maintenant d'au moins 2 packs de 5 sons, sans pack au-dessus de 90 %, et
  survit au retrait du plus gros pack.
- Volume : les rendus avant / après restent calés (dubstep : batterie −0,1 dB, best-of 0,0 dB).

## Ce qui reste douteux

- **Serrés** : 8 choix sur 14. La version gagnante n'est pas la même aux deux bouts de la plage ;
  on garde « nouvelle », comme la règle le dit.
- **House et trap sans aucun changement** : faute de packs variés, pas parce que l'actuel est
  juste. Plus de packs house et trap pourraient faire bouger ces deux styles.
- **Tech house** emprunte les sons house et techno : sa cible existe alors que celles de house
  seule et de techno seule (pour le kick) n'existent pas.
- **Caisse claire dnb en A** : A gagne nettement (0,19 contre 1,00), mais A est une variante d'écoute
  du matin ; à confirmer à l'oreille.
- Le timbre (brillance) n'entre toujours pas dans la règle : seules les longueurs comptent.
- Page des originaux (privée) : 5 boucles sur 60 lignes. Le transfert du 27 novembre ne contenait
  aucun coup isolé de la liste ; les 55 lignes batterie y sont marquées « manque ».
