# Deuxième audit « ultra » de BenStudio (plan du 2026-11-03)

**Demande de Benjamin (2026-11-03)** : « Il faut que l'application soit testée par énormément d'utilisateurs qui n'y connaissent rien, qui connaissent un peu, ou qui connaissent beaucoup ; que tous les boutons soient connectés partout dans les tutos ; que le programme soit idéalement parfait. »

**Quand :** après le premier Audit (`AUDIT.md`, étages 0 à 6 verts), sur le signal de Benjamin, en mode ultra (beaucoup d'agents). Le Play Store vient après (consigne du 2026-11-03).
**Différence avec le premier Audit :** le premier vérifie que tout marche ; l'ultra vérifie que **tout le monde y arrive**, partout, et que **chaque bouton est relié, expliqué et utile**. Méthode différente : une foule de testeurs, des contradicteurs pour chaque défaut, une équipe rouge, et la comparaison avec la concurrence.

---

## 1. Le recensement de chaque bouton (avant la foule)
Un programme parcourt **tous les écrans** (machine à états de l'étage 2), en Simple et en Pro, et liste **chaque élément touchable** (bouton, puce, curseur, pad, case, menu, geste). Pour chacun, une fiche automatique :
- **ce qu'il fait** (changement mesuré du projet, du son ou de l'écran) : un bouton sans effet = défaut ;
- **annulable**, **sauvegardé**, **exporté** quand ça a un sens ;
- **libellé lisible** par TalkBack, cible de 48 dp, texte entier dans les 15 langues ;
- **expliqué** : l'aide « ? » le décrit, et au moins un tuto, le Guide ou « Montre-moi » le montre (sinon : soit on l'ajoute au tuto, soit on se demande si le bouton est utile) ;
- **même mot et même icône** qu'ailleurs (Lexique).

Sortie : `docs/audits/RECENSEMENT-BOUTONS.md` (une ligne par bouton) et un test qui échoue si un bouton nouveau n'a pas sa fiche. **Objectif : 100 % des boutons reliés, testés et expliqués.**

## 2. Les tutos au peigne fin
- **Les 37 tutos × Facile et Pro × 15 langues**, parcourus par des débutants IA qui ne lisent que l'écran (banc « testeur humain »).
- À chaque étape :
  - le bouton désigné est visible sans chercher ;
  - « Montre-moi » montre le bon geste au bon endroit ;
  - le geste réel fait ce que le texte promet, et le son change comme annoncé ;
  - « Étape suivante » n'arrive que quand c'est fait.
- **Croisement avec le recensement** : chaque bouton important apparaît au moins une fois dans un tuto, le Guide ou l'aide.
- **Le Guide** : chaque conseil mène au bon bouton en un toucher.

## 3. La foule de testeurs (cœur de l'ultra)
**Trois niveaux, chacun avec plusieurs profils :**
- **N'y connaît rien** :
  - enfant de 6 et 8 ans ;
  - senior qui découvre le tactile ;
  - ado qui veut « faire un son TikTok » ;
  - parent pressé ;
  - personne qui ne lit pas bien.
- **Connaît un peu** :
  - joueur de GarageBand ou Koala ;
  - DJ amateur ;
  - guitariste qui veut enregistrer ;
  - fan de remix ;
  - utilisateur du MiniLab 3 ou d'un autre clavier.
- **Connaît beaucoup** :
  - producteur FL Studio ou Ableton ;
  - ingénieur du son ;
  - beatmaker trap ;
  - DJ club ;
  - prof de musique ;
  - testeur QA professionnel.
- **Plus, à chaque tour** :
  - une personne malvoyante (TalkBack) ;
  - une personne daltonienne ;
  - une personne avec un tremblement (touchers imprécis) ;
  - un non-francophone par langue tournante.

**Volume :**
- des tours de **12 à 16 testeurs neufs** (jamais deux fois le même), au moins **6 tours**, soit **environ 80 passages** au total ;
- chacun avec son appareil (petit téléphone, Honor, tablette, pliable), sa langue, son mode Simple ou Pro et sa mission.

**Missions** (tirées par paires pour tout couvrir) :
- faire son premier morceau en 10 minutes ;
- remixer une chanson et découper la voix ;
- jouer au Pad Live avec un enfant ;
- refaire un défi ;
- enregistrer sa voix ;
- exporter pour Spotify et pour le club ;
- reprendre un projet trois jours après ;
- brancher le MiniLab ;
- travailler sur tablette au clavier ;
- tout faire en Mode simple sans jamais passer en Pro.

**Ce que chaque testeur rend :**
- ce qu'il a voulu faire, où il a hésité (temps et touchers), où il a abandonné ;
- ce qui l'a surpris, et sa note sur 10 ;
- « l'œil » compare l'écran attendu à l'écran obtenu (bugs qui ne plantent pas) ;
- **mesures** : réussite par mission, temps, touchers, retours en arrière, appels à l'aide.

## 4. Chaque défaut passe par des contradicteurs
- **Trois relecteurs indépendants** essaient de **prouver qu'il n'existe pas** (reproduction, lecture du code). Un défaut est gardé si au moins deux sur trois échouent à le réfuter. Fin des fausses alertes.
- Un défaut gardé est **corrigé à la racine, partout** (règle « partout »), avec un test qui le reproduit. Un défaut d'ergonomie est noté par **au moins deux profils différents** avant de changer le design.

## 5. L'équipe rouge (casser exprès)
- appuyer partout très vite, dix doigts ;
- couper pendant une sauvegarde, un export ou une séparation ;
- remplir le téléphone ;
- fichiers piégés ;
- tempo extrême ;
- 200 pads ;
- un projet de 2 heures ;
- débrancher le MiniLab en jouant ;
- changer de langue en pleine lecture.

**Objectif :** aucun plantage, aucun projet perdu, aucun craquement.

## 6. La comparaison avec la concurrence
Des experts comparent, fonction par fonction, BenStudio à FL Studio Mobile, Koala Sampler, Remixlive, Groovepad, BandLab et GarageBand :
- ce qui manque ;
- ce qui demande plus de touchers chez nous ;
- ce que les avis négatifs de ces applis reprochent, et que nous évitons ou pas.

Sortie : une liste courte « à ajouter » ou « à simplifier », proposée à Benjamin (il tranche, un seul lot de questions).

## 7. La boucle et l'arrêt
- **Tour** : foule, contradicteurs, corrections, relecture neuve des corrections, puis tour suivant avec des testeurs neufs.
- **Arrêt** quand **deux tours de suite** finissent avec :
  - zéro Bloquant, zéro Grave, aucun Moyen nouveau ;
  - **au moins 90 % de missions réussies chez les débutants et 100 % chez les connaisseurs** ;
  - un recensement des boutons à 100 % ;
  - tous les tutos verts dans toutes les langues.
- **Garde-fou** : au 8e tour sans arrêt, point avec Benjamin.

## 8. Coût et rythme
- Mode ultra : beaucoup d'agents **en lecture et en jeu** (les testeurs ne compilent pas : ils pilotent l'appli déjà construite).
- **Deux équipes au plus** qui modifient le code (corrections), pour ne pas surcharger la machine.
- Les testeurs passent par lots de 4 à 6 en même temps. Les tests lourds tournent seuls.
- Chaque tour livre un tableau de bord (`docs/audits/GABARIT-TABLEAU-DE-BORD.md`) et un résumé de 5 lignes à Benjamin.

## 9. Ce que Benjamin fera
- Dire « Audit ultra » quand il veut le lancer.
- Trancher le lot de questions de la comparaison (§6).
- À la fin : le bug bash d'une heure avec sa fille et son ami (étage 7 du premier Audit), sur la version qu'il demandera.
