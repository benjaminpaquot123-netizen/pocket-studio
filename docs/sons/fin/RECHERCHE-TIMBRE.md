# Recherche timbre : retrouver la brillance et le grain des sons de référence, sans les copier

Étude seule : rien n'est changé dans l'appli. Les essais tournent sur nos propres rendus et sur des
sons inventés pour l'étude, jamais sur un échantillon acheté. Tout se rejoue avec
`scripts/son/timbre/run.sh`.

## Résumé en 5 lignes

1. **Le constat.** La règle ne regarde que la longueur, et ça se voit : nos sons sont dans la
   fourchette pour 23 longueurs sur 23, mais pour 16 mesures de timbre sur 27. Sur un charley, 71
   recettes sur 75 sont « aussi bonnes » pour la règle alors que leur brillance va de 1 686 à 16 090 Hz.
2. **La méthode qui gagne.** Mesurer le timbre (17 nombres en plus des deux longueurs, dont 15
   vérifiés par un deuxième calcul), viser le portrait-robot de la famille, et laisser une recherche
   automatique régler la recette. Sur trois familles inventées, la voix d'aujourd'hui passe de 3, 5
   et 3 mesures dans la fourchette à 19 sur 19, 16 sur 17 et 17 sur 18 (comptées sur les sons qui
   ont servi à régler : à confirmer sur des sons mis de côté).
3. **La limite légale.** On ne garde aucun son, aucun morceau de son, et on vise toujours une famille
   (au moins 20 sons, 2 packs), jamais un son. C'est cette règle de travail qui protège : les deux
   garde-fous chiffrés repèrent une copie presque identique, pas une copie redessinée avec notre
   propre bruit.
4. **Le coût.** La recherche se fait sur la machine de développement : zéro calcul en plus sur le
   téléphone. Une voix plus riche (bruit en bandes, quelques résonances) coûterait de l'ordre de 1,5
   à 2 fois la voix d'aujourd'hui selon le son (chiffre de la machine de test, à refaire sur le
   téléphone) ; elle n'est à construire que là où la recherche plafonne.
5. **Ce qui n'est pas prouvé.** Rien n'a été mesuré sur la vraie collection : c'est l'étape 0 du
   plan. La note du kick se lit par deux chemins sur 5 de nos 6 kicks ; le sixième est trop court
   pour avoir une note. L'oreille de Benjamin reste le dernier juge (écoute A/B, étape 5). Une
   relecture à l'aveugle a fait corriger plusieurs affirmations trop belles : section 8.

## 1. D'où on part

### La voix

Chaque son de batterie sort de la même voix (`engine/src/main/cpp/core/MeasuredDrums.h`), réglée par
une recette de 19 nombres :

- un **corps** : une seule sinusoïde qui glisse d'une note à une autre, avec une saturation ;
- un **bruit** : du bruit blanc et six oscillateurs carrés « métal » à écarts fixes, passés dans un
  coupe-bas et un coupe-haut fixes ;
- un **clic** d'attaque ;
- des enveloppes « tient puis tombe ».

Ce que cette voix ne sait pas faire, pour le timbre : plusieurs résonances à la fois (une caisse
claire en a plusieurs), un bruit dont la couleur change pendant que le son meurt (les aigus qui
tombent plus vite que le médium), un métal dont on choisit les écarts.

### La règle

`docs/sons/fin/REGLE-BATTERIE.md` : la cible d'une mesure est la médiane de la famille, l'écart se
compte en largeur de fourchette, le plus petit score gagne. Seules deux longueurs comptent (t20 et
t60). La brillance est mesurée (centre de gravité du spectre, part au-dessus de 10 kHz, attaque
contre corps) mais ne pèse pas.

### La preuve que ça ne suffit pas

Essai `cibles` : nos 54 sons, mesurés avec l'outil du projet (`scripts/sons/mesure.py`), contre les
cibles définitives déjà publiées.

| Mesure | Nos sons dans la fourchette p10–p90 |
|---|---|
| t20 | 11 sur 11 |
| t60 | 12 sur 12 |
| centre de gravité (brillance) | 2 sur 8 |
| part au-dessus de 10 kHz | 6 sur 9 |
| attaque contre corps | 8 sur 10 |

Longueurs : 23 sur 23. Timbre : 16 sur 27. Le 23 sur 23 n'est pas un exploit : les recettes ont été
choisies sur ces longueurs-là, il dit seulement que le choix a été bien fait. Les plus loin pour le
timbre : la caisse claire dnb (centre de gravité à 440 Hz pour une fourchette de 1 930 à 4 069 Hz),
les claps dnb et techhouse (part d'aigus 6 à 7 dB sous le bas de la fourchette, environ 12 dB sous
la médiane), le crash dubstep (3 115 Hz pour 3 965 à 6 094). Les charleys sont dedans partout.

Essai `aveugle` : pour une case, 75 variantes de la recette choisie ; on garde celles que la règle
actuelle juge aussi bonnes, et on regarde leur timbre.

| Case | Variantes aussi bonnes sur les longueurs | Centre de gravité de ces variantes | Dans la fourchette de timbre |
|---|---|---|---|
| charley fermé techhouse | 71 sur 75 | 1 686 à 16 090 Hz (cible 6 853 à 10 190) | 26 sur 71 |
| clap techhouse | 70 sur 75 | 656 à 8 315 Hz (cible 2 292 à 4 726) | 27 sur 70 |
| kick techhouse | 75 sur 75 | 77 à 95 Hz (cible 86 à 168) | 25 sur 75 |

La règle actuelle ne fait donc pas la différence entre un charley sourd et un charley qui siffle.

Portée de cet essai : les variantes ne changent que les deux filtres du bruit, le clic et le métal,
jamais les enveloppes ; « aussi bonne » veut dire un score de longueur pas plus mauvais que celui de
la recette choisie (ou sous 0,5). Il montre que la règle laisse passer des timbres très différents,
pas combien de recettes en tout sont dans ce cas. La ligne du kick est la moins parlante : ses
variantes bougent peu.

## 2. La limite entre « s'inspirer » et « copier »

Je ne suis pas juriste : ce qui suit est une lecture prudente, à faire confirmer si un jour l'appli
est vendue.

### Ce que dit le droit

- **Europe.** Arrêt Pelham (Cour de justice de l'Union européenne, C-476/17, 29 juillet 2019) :
  reprendre un extrait d'un enregistrement, même très court, est une reproduction, sauf s'il est
  modifié au point de ne plus être reconnaissable à l'oreille. C'est le droit du producteur de
  l'enregistrement (directive 2001/29, article 2 c). À ne pas étirer : cet arrêt parle d'extraits
  pris dans un enregistrement. Il ne dit rien d'un son refait de zéro qui imite, et son « plus
  reconnaissable à l'oreille » n'est pas un permis de déguiser une copie.
- **États-Unis.** 17 U.S.C. § 114(b) : le droit sur un enregistrement ne couvre pas un autre
  enregistrement fait de sons produits indépendamment, même s'il imite ou simule le premier. Pour
  les extraits très courts, les juges ne sont pas d'accord entre eux : Bridgeport (6e circuit,
  2005) ne tolère aucun extrait d'enregistrement, VMG Salsoul (9e circuit, 2016) en tolère de
  minuscules. Raison de plus pour n'en prendre aucun.
- **Le contrat.** Un coup de batterie isolé n'a en général pas de droit d'auteur comme une mélodie ;
  ce qui compte, c'est le droit sur l'enregistrement et la licence du pack. Ces licences interdisent
  souvent de refaire une banque de sons ou un instrument avec leur contenu, et de plus en plus
  d'entraîner une IA dessus. À relire pack par pack avant l'étape 0.

En clair : refaire soi-même un son qui ressemble est permis ; reprendre le son, ou un bout, ne l'est
pas. Notre politique (des cibles de famille) est plus prudente que ce que le droit exige.

### Pourquoi « on ne garde que des paramètres » ne suffit pas

Un MP3 aussi, ce sont des paramètres. Avec assez de nombres, on rejoue l'original. La vraie limite
n'est pas « nombres contre audio » mais : **est-ce que ces nombres décrivent une famille, ou un
son précis ?**

Essai `copie`, partie bruit : on redessine une caisse claire inventée avec notre propre bruit, en
recopiant son énergie sur une grille (bandes × tranches de temps) de plus en plus fine. On compare à
ce que valent ses 29 voisines de famille (la plus proche est à 0,42 en empreinte, 1,90 dB en carte).

| Grille (bandes × tranches) | Nombres pris | Distance d'empreinte au son | Distance de carte au son | Test d'annulation |
|---|---|---|---|---|
| 9 × 6 | 54 | 0,59 | 3,47 dB | −0,1 dB |
| 9 × 12 | 108 | 0,53 | 2,75 dB | −0,1 dB |
| 22 × 12 | 264 | 0,36 | 2,19 dB | −0,1 dB |
| 22 × 24 | 528 | 0,41 | 1,92 dB | −0,1 dB |
| 49 × 44 | 2 156 | 0,42 | 1,78 dB | −0,1 dB |
| 86 × 76 | 6 536 | 0,40 | 1,71 dB | −0,1 dB |
| 513 × 286 | 146 718 | 0,38 | 1,55 dB | −0,1 dB |
| le son lui-même, à peine filtré | tout | 0,02 | 0,03 dB | −41,5 dB |

Le seuil du garde-fou est l'écart des deux sons les plus proches de la famille : 0,28 en empreinte,
1,77 dB en carte. Trois leçons, dont deux contre nous :

- Le test d'annulation (soustraire l'original et voir ce qui reste) est **aveugle** pour un son de
  bruit : il ne voit jamais la copie redessinée.
- La distance d'empreinte **ne la voit pas non plus**. Aucune grille ne passe sous 0,28 (0,36 au
  mieux), et recopier 146 718 nombres ne rapproche pas plus qu'en recopier 264 : ce qui reste, c'est
  l'écart d'un autre tirage du bruit.
- La carte bandes-temps ne la voit que pour les deux grilles les plus fines (1,71 et 1,55 dB). Les
  grilles de 22 à 49 bandes, qui recopient déjà des centaines de nombres d'un seul son, passent.

Autrement dit : **aucun de nos chiffres ne repère de façon sûre une copie redessinée avec notre
propre bruit.**
Ils repèrent la copie presque identique (dernière ligne du tableau). Contre la copie redessinée, la
seule protection est la façon de travailler : ne jamais régler sur un son.

Essai `copie`, partie tonale : on colle des résonances sur le corps d'un kick inventé (ses 29 voisins
s'annulent contre lui de 3,2 dB en médiane, 6,9 dB au mieux).

| Résonances | Nombres pris | Annulation contre ce kick | Annulation contre son plus proche voisin |
|---|---|---|---|
| 1 | 4 | −2,2 dB | −1,1 dB |
| 2 | 8 | −4,0 dB | −2,3 dB |
| 3 | 12 | −7,2 dB | −4,6 dB |
| 4 | 16 | −14,6 dB | −7,0 dB |
| 8 | 32 | −18,6 dB | −7,0 dB |
| 32 | 128 | −31,6 dB | −6,9 dB |

**Seize nombres réglés sur UN kick suffisent à le rejouer** mieux que tout autre kick de sa famille.
« Peu de paramètres » ne protège donc pas pour un son tonal. Ce qui protège, c'est de régler sur la
famille.

### La règle proposée

Permis (s'inspirer) :

- la cible est le **portrait-robot d'une famille** : médiane et fourchette, sur au moins 20 sons
  venus d'au moins 2 packs (les cinq conditions de la règle actuelle) ;
- peu de nombres par famille (une vingtaine), les mêmes pour tout le monde ;
- le son sort de nos oscillateurs et de notre bruit ;
- personne ne reconnaît un échantillon précis.

Interdit (copier, même déguisé) :

- garder l'audio, un bout d'audio, une attaque échantillonnée, un cycle d'onde extrait ;
- garder le spectre complet image par image, des centaines de pistes de sinusoïdes suivies, une
  réponse impulsionnelle tirée d'un original ;
- réutiliser le « reste » d'un original après en avoir ôté les sinusoïdes ;
- régler quoi que ce soit sur **un seul** son, ou entraîner un réseau sur les packs.

Deux garde-fous, à passer pour chaque son avant qu'il entre :

1. **Plus proche voisin** : notre son ne doit pas être plus près d'une référence (empreinte et carte
   bandes-temps) que les deux références les plus proches de la famille ne le sont entre elles.
2. **Annulation** : contre chaque référence, notre son ne doit pas s'annuler plus profondément que
   les références entre elles. Ce test ne vaut que pour les sons tonaux (kick, 808, tom).

Ce que valent ces garde-fous (essai `copie`) : ils arrêtent une copie presque identique, et un
réglage tonal collé sur un seul son (le kick à 16 nombres s'annule de 14,6 dB, bien plus profond que
son meilleur voisin, 6,9 dB). Ils **n'arrêtent pas** une copie redessinée avec notre bruit. Et le
test d'annulation ne cherche le meilleur calage que sur 5 ms de part et d'autre, à vitesse égale :
une copie décalée de 1 à 2 % en hauteur lui échapperait sans doute (pas mesuré ici). Ils ne
remplacent donc pas la **règle de travail**, qui passe avant eux :

- aucune analyse d'un son précis ne règle la synthèse : les cibles sont des tableaux de famille,
  rien d'autre ;
- les techniques de la classe D (section 3) restent refusées, quoi que disent les distances.

Les mesures faites sur un son précis ne sortent jamais de la machine de mesure : seul le tableau de
famille (médiane, fourchettes, nombre de sons) est publié, comme aujourd'hui.

## 3. Les techniques, classées

Coût : mesuré sur la machine de test (`run.sh bench`, un cœur à 2,1 GHz). La voix d'aujourd'hui y
coûte 30 ns par échantillon, soit 0,14 % d'un cœur. Un téléphone est plus lent ; on ne donne donc
que des rapports à la voix d'aujourd'hui (« ×1 »). Le coût sur téléphone reste une estimation tant
qu'il n'est pas mesuré dessus.

| Bloc candidat | Coût mesuré | Rapport |
|---|---|---|
| voix d'aujourd'hui | 29,9 ns | ×1 |
| 8 résonances | 5,6 ns | ×0,19 |
| 16 résonances | 8,2 ns | ×0,27 |
| 32 résonances | 15,9 ns | ×0,53 |
| bruit en 9 bandes | 15,6 ns | ×0,52 |
| bruit en 24 bandes | 18,2 ns | ×0,61 |
| réponse courte, 64 points | 20,7 ns | ×0,69 |
| réponse courte, 256 points | 110 ns | ×3,7 |
| réponse courte, 2 048 points | 1 242 ns | ×41 |

Ces chiffres sont fragiles, pour trois raisons. Le « ×1 » est une seule recette (une caisse claire) :
le relecteur, avec son propre banc (hors dépôt), trouve de 25 ns (charley fermé) à 50 ns (kick)
selon la recette, donc un même bloc pèse du simple au double selon le son auquel on le compare.
D'un passage à l'autre, sur une machine occupée, un bloc peut varier de moitié (32 résonances : 16 à
25 ns chez lui ; le bruit en 24 bandes lui a donné 16,4 ns, pas 18,2). Enfin le banc tourne sur un
processeur de serveur, sans les conditions d'un téléphone. À retenir : l'ordre de grandeur, pas la
décimale.

Tous ces blocs sont écrits comme le veut le thread audio : tableaux de taille fixe, aucune
allocation, aucun verrou. Un piège trouvé en les mesurant : une résonance qu'on laisse mourir
pendant des secondes tombe dans des nombres minuscules et coûte dix fois plus cher ; une vraie voix
doit s'arrêter quand elle est inaudible.

### Classe A : à faire d'abord (rien de plus à calculer sur le téléphone)

**A1. Recherche automatique de la recette.** Un programme essaie des milliers de recettes sur la vraie
voix et garde celle dont l'empreinte de timbre est la plus près du portrait-robot (méthode CMA-ES,
qui n'a pas besoin de dérivées).
- Fidélité : la meilleure du lot pour l'effort. Voir la section 5.
- Légalité : propre, tant que la cible est la famille.
- Coût téléphone : zéro, seuls les 19 nombres de la recette changent.
- Effort : petit. Le rendu hors-ligne existe (`render_voice.cpp`), la recherche aussi (`timbre.py`).
  Il reste à brancher les vraies cibles.

**A2. Le timbre dans la règle.** Les mesures de la section 4, avec leurs contre-mesures.
- Fidélité : c'est ce qui rend A1 possible ; sans cible de timbre, pas de recherche.
- Légalité : propre (tableau de famille).
- Coût : zéro. Effort : moyen, c'est surtout de la vérification d'outil.

**A3. Égalisation par bandes sur la médiane de famille** (« matching EQ »). Ce sont les neuf niveaux
par octave de l'empreinte : la recherche A1 les vise déjà. Pas de bloc à part.

### Classe B : enrichir la voix, seulement là où A plafonne

**B1. Bruit en bandes.** Neuf à vingt-quatre bandes de bruit, chacune avec son niveau et sa vitesse de
chute. C'est un vocodeur dont les réglages sont des médianes de famille, pas le suivi d'un original.
- Fidélité : donne la couleur du bruit et son évolution (aigus qui tombent d'abord), ce que deux
  filtres fixes ne peuvent pas.
- Légalité : propre à 9 bandes avec une chute par bande (18 nombres). À ne pas affiner jusqu'à
  suivre un son image par image : voir le tableau de la section 2.
- Coût : ×0,5 à ×0,6 en plus. Effort : moyen (un bloc, 18 à 48 champs de recette).

**B2. Banc de résonances** (synthèse modale). Quatre à seize sinusoïdes amorties, chacune avec sa
note, son niveau et sa durée.
- Fidélité : le corps des caisses claires, toms, rims, et le métal des charleys.
- Légalité : **le point sensible.** Seize nombres réglés sur un son le rejouent. Les résonances se
  règlent donc par la recherche A1 contre la famille, jamais par analyse d'un son.
- Coût : ×0,2 à ×0,3. Effort : moyen.

**B3. Attaque réglée à part.** Le clic existe ; lui donner sa propre couleur, visée par deux mesures
(brillance des 20 premières millisecondes, attaque contre corps).
- Fidélité : le « claquant ». Légalité : propre si le clic est synthétisé, interdit s'il est
  échantillonné. Coût : négligeable. Effort : petit.

**B4. Kick : glissé et harmoniques.** La note d'arrivée, la vitesse du glissé et le niveau des
harmoniques 2 et 3 (la saturation), que `mesure.py` mesure déjà.
- Fidélité : le caractère du kick est là. Légalité : propre. Coût : zéro (la voix sait déjà).
  Effort : petit côté voix, moyen côté mesure (section 4).

**B5. Métal réglable.** Remplacer les six écarts fixes par des écarts choisis, ou par une modulation
de fréquence. Coût nul ou faible, effort petit ; utile seulement si les charleys ou le crash
restent dehors après A1 (aujourd'hui, les charleys sont dedans).

### Classe C : outils d'analyse et réserves

| Technique | À quoi elle sert ici | Pourquoi pas en premier |
|---|---|---|
| Sinusoïdes + bruit (SMS), séparation sinus / attaque / bruit, harmonique / percussif | Savoir, par famille, quelle part du son est tonale, bruitée, ou attaque : ça dit quels blocs B servent | Comme resynthèse, elle rejoue un son précis : interdite sous cette forme |
| ESPRIT, Prony (trouver les résonances d'un son) | Compter les résonances d'une famille et leurs zones | Sur un son tonal propre, précis à 0,05 centième de demi-ton ; sur un son de bruit, 512 résonances n'expliquent que 9,5 dB : inutile pour le bruit |
| LPC, cepstre, MFCC (enveloppes spectrales compactes) | Deuxième avis sur la couleur ; un filtre LPC sur du bruit est une variante de B1 | Les niveaux par octave disent la même chose en plus lisible |
| Spectres multi-résolution, transformée de diffusion (distances fines de la littérature) | Arbitre final entre deux réglages proches | Point par point, elles confondent « autre tirage du bruit » et « autre son » (section 4) |
| Optimisation par gradient (DDSP) | Si un jour la voix a des centaines de réglages | Demande de réécrire la voix dans un autre outil et de garder les deux pareilles ; pour une trentaine de réglages, la littérature donne CMA-ES devant |
| Recherche bayésienne | Quand un essai coûte cher | Un essai coûte ici une fraction de seconde |
| Réponse courte synthétisée (convolution) | Une couleur de « boîte » ou de pièce, fabriquée par nous | 64 points coûtent autant que B1 pour moins de réglages ; au-delà de 256 points, trop cher |
| Tables pré-rendues depuis notre synthé | Zéro calcul | On perd les réglages en direct |
| Modèle physique (membrane, plaque) | Batteries acoustiques ; une membrane dont la tension se relâche donne toute seule le glissé de note d'un kick ou d'un tom | Lourd, et nos familles sont des sons de machines ; le glissé réglé à la main (B4) fait le même travail avec deux nombres |
| Bruit dans un filtre qui change au fil du son (LPC image par image) | Une couleur de bruit qui évolue finement | Réglé sur des médianes de famille, c'est B1 en plus cher ; réglé en suivant un son, c'est la classe D |
| Grain « vieille machine » (résolution réduite, saturation) | Une part de la couleur des références | À ajouter seulement si une mesure le réclame |

### Classe D : refusé

Vocodeur qui suit un original image par image, spectre complet et reconstruction de phase, pistes
de sinusoïdes suivies, réponse impulsionnelle tirée d'un original, table d'onde ou grains extraits,
attaque échantillonnée, réseau entraîné sur les packs (codec, diffusion, transfert de timbre). Tous
gardent le son lui-même sous une autre forme ; le dernier ajoute un risque de licence et un coût
hors de portée d'un téléphone.

## 4. Les mesures à ajouter à la règle

### L'empreinte : 19 nombres par son

| Nombre | Ce qu'il dit | Tolérance de la contre-mesure |
|---|---|---|
| t20, t60 | longueurs (déjà dans la règle) | celles de la règle |
| centre de gravité, son entier | brillance | 5 % |
| centre de gravité, 20 premières ms | brillance de l'attaque | 5 % |
| centre de gravité, après 40 ms | brillance de la queue | 5 % |
| part au-dessus de 10 kHz | l'air | à écrire (étape 0) |
| attaque contre corps (10 ms) | le claquant | à écrire (étape 0) |
| t20 du grave, du médium, de l'aigu | ce qui meurt en premier | 10 % |
| niveau de 9 bandes d'une octave | la couleur | 1 dB |

Même formule que la règle actuelle (écart = 2 × |valeur − médiane| ÷ (p90 − p10)), mêmes cinq
conditions par case pour qu'une cible soit « définitive ».

### Les contre-mesures : un deuxième chemin pour 15 nombres sur 17

Le premier chemin découpe les bandes dans le spectre ; le second passe le son dans des filtres
écrits dans le temps, et calcule les centres de gravité sans spectre du tout. Deux nombres n'ont
pas encore de second chemin (part au-dessus de 10 kHz, attaque contre corps), et les longueurs t20
et t60 gardent la contre-mesure de la règle actuelle, qui n'est pas rejouée ici. Essai `contre`, sur
144 sons (nos 54 et 90 inventés) :

| Mesure | Accord | Pire écart |
|---|---|---|
| les trois centres de gravité | 144 sur 144 | 2,35 % |
| t20 du grave | 86 sur 86 | 8,1 % |
| t20 du médium | 116 sur 116 | 7,9 % |
| t20 de l'aigu | 93 sur 93 | 0,0 % |
| bande la plus grave (20 à 100 Hz) | 99 sur 100 | 1,14 dB |
| les huit autres bandes | toutes | 0,26 dB |

Réserve relevée par le relecteur : pour les bandes et leurs t20, les deux chemins filtrent
différemment mais partagent le dessin des bandes, le repérage du début du son et le calcul de la
chute. Ce tableau prouve donc que le **filtrage** est juste, pas le reste : un défaut dans ces trois
morceaux communs passerait sans bruit. Une contre-mesure complète (autre repérage du début, autre
calcul de la chute) est à écrire à l'étape 0.

### L'outil s'est fait prendre par sa propre contre-mesure, trois fois

C'est la règle du projet (« l'outil se vérifie avant de mesurer »), et elle a servi. Première
version : accord de 15 % sur le t20 du grave, 22 % sur celui du médium, 81 % sur la brillance.

1. Le filtre par spectre faisait le tour du son et déposait un écho avant l'attaque à la fin du
   fichier : un son de 20 ms se lisait 1 000 ms. Corrigé par du silence ajouté autour.
2. La contre-mesure de brillance mesurait autre chose (une moyenne d'angles). Remplacée par le même
   centre de gravité, calculé dans le temps.
3. Des bords de bandes francs rendaient la mesure dépendante de la façon de filtrer quand le corps
   d'un tambour est posé sur le bord (200 Hz). Corrigé par des bords en fondu, dans les deux chemins.

Après correction : 99 à 100 % partout. Chaque défaut a son test dans `selftest.py`.

### Ce qu'une mesure peut voir : le tirage du bruit

Un son de bruit est un coup de dés. Douze tirages du même son inventé donnent une brillance de 2 428
à 2 655 Hz, une part d'aigus qui bouge de 0,8 dB, un t20 de 141 à 146 ms. **Aucune cible ne doit être
plus serrée que 5 % ou 1 dB.**

Essai `bruit` : chaque distance, entre « le même son, autre tirage » et « un son 30 % plus brillant ».
Plus le rapport est grand, mieux la distance voit le timbre plutôt que les dés.

| Distance | Même son, autre tirage | Son plus brillant | Rapport |
|---|---|---|---|
| spectre point par point | 0,228 | 0,377 | 1,7 |
| carte bandes-temps | 1,44 dB | 2,07 dB | 1,4 |
| neuf bandes, son entier | 0,33 dB | 0,87 dB | 2,6 |
| brillance de l'attaque | 6,6 % | 29,6 % | 4,5 |
| brillance de la queue | 1,5 % | 33,9 % | 22 |
| brillance du son entier | 0,7 % | 21,8 % | 33 |

D'où le choix : la cible est faite de nombres résumés (ceux de l'empreinte). La carte bandes-temps
et le spectre point par point servent de **contrôle** (garde-fou anti-copie), pas de cible.

### La note du kick, par deux chemins

Aujourd'hui « la note du kick n'a pas de contre-mesure fiable ». Essai `note` : premier chemin dans
le temps (durée de chaque période de la queue grave), second dans le spectre (le pic le plus fort de
cette queue, avec un autre filtre). Une note ne compte que si les deux chemins en voient une et sont
d'accord à 3 % (un demi demi-ton).

| Son | Note demandée | Chemin 1 | Chemin 2 | Écart | Queue lue |
|---|---|---|---|---|---|
| kick house | 41,2 Hz | 41,37 | 40,94 | 1,03 % | 2,1 périodes |
| kick techhouse | 49,0 | 49,09 | 48,82 | 0,56 % | 2,9 périodes |
| kick techno | 49,0 | 49,06 | 49,10 | 0,09 % | 2,2 périodes |
| kick trap | 43,6 | pas de note | pas de note | | 1,4 période |
| kick dnb | 49,0 | 48,95 | 48,91 | 0,08 % | 2,5 périodes |
| kick dubstep | 43,6 | 43,93 | 43,54 | 0,90 % | 2,3 périodes |
| les six 808 | 41,2 à 55,0 | exact | exact | 0,00 % | 15 à 20 périodes |
| les six toms | 95 à 117 | exact | exact | 0,02 % au pire | 10 à 24 périodes |

Sur 30 kicks inventés, avec ou sans souffle à −40 dB : 30 sur 30 d'accord, pire écart 0,07 %.

**Pourquoi le kick est difficile : sa queue lisible ne contient que deux périodes environ.** En
dessous de deux, il n'y a pas de note à lire, et un chemin seul peut se tromper sans le savoir (sur
un son de 45 Hz qui meurt en 12 ms, le premier chemin lit 48 Hz ; c'est le désaccord avec le second
qui le dit). La règle proposée dit donc « pas de note » plutôt qu'une note fausse. Sur les vrais
kicks, tout reste à prouver à l'étape 4.

Le **t40** est une longueur, pas du timbre : sa contre-mesure s'écrit comme celles du t20 et du t60.
Elle se valide à l'étape 0, sur la vraie collection.

## 5. Ce que montrent les essais de recherche

Essai `famille` : trois familles inventées (30 sons, 3 « packs » inventés chacune), fabriquées par un
synthétiseur caché plus riche que notre voix. On vise leur portrait-robot.

« Dans la fourchette » = nombre de mesures de l'empreinte entre p10 et p90 de la famille. Un vrai
membre de la famille en a 15 sur 19 en médiane (caisse claire), 14 sur 17 (charley), 14 sur 18 (kick),
en le comptant lui-même dans la fourchette.

| Réglage | Caisse claire | Charley | Kick |
|---|---|---|---|
| notre recette actuelle la plus proche | 1 sur 19, score 2,89 | 5 sur 17, score 3,30 | 10 sur 18, score 2,62 |
| voix d'aujourd'hui, recherche sur les longueurs seules (la règle actuelle) | 3 sur 19, score 3,66 | 5 sur 17, score 11,01 | 3 sur 18, score 16,29 |
| voix d'aujourd'hui, recherche sur toute l'empreinte | 19 sur 19, score 0,25 | 16 sur 17, score 0,17 | 17 sur 18, score 0,20 |
| voix plus riche : bruit posé sur les médianes, corps repris de la ligne du dessus, sans recherche | 14 sur 19, score 0,79 | 14 sur 17, score 0,43 | 12 sur 18, score 2,24 |
| voix plus riche, après recherche | 19 sur 19, score 0,15 | 17 sur 17, score 0,10 | 18 sur 18, score 0,14 |

Ce qu'on en tire :

- **À lire avec prudence : le 19 sur 19 est presque automatique.** La recherche réduit exactement ces
  écarts-là, sur les 30 sons qui définissent la fourchette, et on garde le meilleur de trois
  départs avec ce même score. Aucun son n'a été mis de côté, et une seule mesure n'a jamais été
  visée (la carte bandes-temps). Ce chiffre dit que la voix *peut* atteindre la cible, pas que le
  son est bon. Il fait même mieux qu'un vrai membre de la famille (15 sur 19), ce qui doit mettre
  la puce à l'oreille.
- **Viser les longueurs seules peut empirer le timbre** : le score du charley passe de 3,30 à 11,01,
  celui du kick de 2,62 à 16,29 (il ne reste que 3 mesures sur 18 dans la fourchette, au lieu de 10).
- **La recherche sur toute l'empreinte suffit presque partout, avec la voix d'aujourd'hui.** C'est la
  surprise de l'étude : le plus gros gain ne demande aucun changement du moteur.
- La voix plus riche gagne les dernières mesures (17 sur 17, 18 sur 18), mais elle n'est pas
  nécessaire sur ces familles. Et elle ne se règle pas toute seule : posée sur les médianes sans
  recherche, elle donne 14 sur 19 et 14 sur 17, mais seulement 12 sur 18 pour le kick.
- **Un meilleur score n'est pas toujours un meilleur son.** La carte bandes-temps, gardée hors
  cible, dit que la voix plus riche est plus loin de sa référence la plus proche que la voix
  d'aujourd'hui pour la caisse claire (3,87 dB contre 2,84) et pour le kick (3,19 contre 1,97) ; elle
  n'est plus près que pour le charley (1,99 contre 2,30). C'est le signe qu'une recherche peut
  « gagner aux points » sans se rapprocher à l'oreille : d'où l'étape 5.
- Trois départs différents de la recherche arrivent à des scores voisins pour la caisse claire (0,25
  à 0,33) et le charley (0,17 à 0,24). Pour le kick, un départ sur trois s'arrête moins bien (0,20,
  0,22 et 0,36) : on lance toujours trois départs et on garde le meilleur.
- **Garde-fou du plus proche voisin tenu partout**, avec la réserve de la section 2 : il prouve
  l'absence de copie presque identique, rien de plus. Distance du son trouvé à sa référence la plus
  proche, contre la distance des deux références les plus proches entre elles :

| | Caisse claire | Charley | Kick |
|---|---|---|---|
| deux références les plus proches entre elles | 0,28 | 0,15 | 0,20 |
| voix d'aujourd'hui après recherche | 0,44 | 0,34 | 0,41 |
| voix plus riche après recherche | 0,35 | 0,25 | 0,35 |
| copie déguisée d'une référence (pour vérifier le garde-fou) | 0,017 | 0,050 | 0,010 |
| test d'annulation de la copie déguisée | −41,5 dB | −34,0 dB | −67,4 dB |

- **Garde-fou d'annulation tenu aussi.** Annulation la plus profonde (plus le chiffre est bas, plus
  les deux sons se ressemblent onde pour onde) :

| | Caisse claire | Charley | Kick |
|---|---|---|---|
| les références entre elles | −2,3 dB | −0,1 dB | −15,8 dB |
| voix d'aujourd'hui contre chaque référence | −1,1 dB | −0,1 dB | −9,5 dB |
| voix plus riche contre chaque référence | −0,4 dB | −0,1 dB | −6,5 dB |

  Nos sons s'annulent toujours moins profondément que les références entre elles. Pour le charley,
  et presque autant pour la caisse claire, le test ne dit rien (du bruit ne s'annule pas) ; il ne
  parle que pour le kick.

Ces chiffres sont optimistes : voir la section 7.

## 6. Le plan

Chaque étape a son critère chiffré ; on ne passe à la suivante que s'il est tenu.

**Étape 0. L'outil, sur la vraie collection** (dans la discussion de mesure, là où sont les sons).
Relire les licences des packs. Porter l'empreinte et ses contre-mesures dans la chaîne de mesure
existante, écrire celles qui manquent (part d'aigus, attaque contre corps, t40) et rendre les
autres vraiment indépendantes (autre repérage du début du son, autre calcul de la chute). Étalonner
le seuil de la carte bandes-temps sur les écarts entre voisins de chaque famille (par exemple leur
5e centile plutôt que leur minimum). Chercher une mesure qui attrape la copie redessinée de l'essai
`copie` ; aucune de celles essayées ici n'y arrive sous 86 bandes.
- Critère : `selftest.py` vert ; accord mesure / contre-mesure ≥ 98 % par mesure et par case ; le
  tableau de famille est publié avec le nombre de cases « définitives » par mesure ; le self-test
  dit, pour chaque grille de l'essai `copie`, si le garde-fou l'attrape (et si aucune mesure n'y
  arrive, c'est écrit dans la règle : seule la règle de travail protège). Aucun son ni aucune
  mesure d'un son précis dans le dépôt.

**Étape 1. Le timbre entre dans la règle.**
- Critère : la règle rejouée sur les 54 sons donne, pour chaque son, son nombre de mesures dans la
  fourchette ; le gagnant est « sûr » (le même avec la cible à p25, à la médiane et à p75) pour au
  moins 80 % des cases qui ont une cible définitive ; les longueurs restent à 23 sur 23.

**Étape 2. Recherche sur la voix d'aujourd'hui** (aucun changement du moteur, seulement des recettes).
- Critère par son : score ≤ 0,5 ; au moins autant de mesures de timbre dans la fourchette que le
  membre médian de sa famille ; les deux longueurs toujours dedans ; trois départs de recherche, on garde le meilleur, et au moins deux
  des trois finissent à moins de 0,15 de score l'un de l'autre (sinon on relance avec plus de
  tours) ; garde-fou du plus proche voisin tenu ; carte bandes-temps pas plus loin de la référence
  la plus proche qu'avant la recherche ; `live-bench.sh` sans alerte.
- Pour que ce critère ne soit pas gagné d'avance : chaque membre de la famille est compté sur une
  fourchette calculée sans lui (c'est ce chiffre qui fixe « le membre médian ») ; la recherche règle
  sur une moitié de la famille tirée au sort et le critère se lit sur l'autre moitié quand la case a
  au moins 40 sons, sinon on le rejoue en retirant un pack à la fois et il doit tenir à chaque
  fois ; le seuil de 0,5 (ce que vaut un son dont les mesures tombent au hasard dans la
  fourchette) est revu sur le score réel du membre médian ; la carte bandes-temps, jamais visée,
  est publiée à côté de chaque score.
- Ce qui ne tient pas ce critère part à l'étape 3, avec la liste des mesures restées dehors.

**Étape 3. Voix plus riche, seulement pour ces sons-là** (B1 d'abord, puis B2 si le corps manque).
- Critère : les mesures restées dehors rentrent ; coût ≤ 2 fois la voix d'aujourd'hui au banc
  (`run.sh bench`) ; aucune allocation ni verrou dans le rendu ; la voix s'arrête quand elle est
  inaudible ; les sons qui n'utilisent pas les nouveaux blocs sortent identiques au bit près ;
  tests du moteur verts.

**Étape 4. Kick, 808, toms.** La note par deux chemins, le glissé, les harmoniques 2 et 3.
- Critère : sur la vraie collection, quand les deux chemins voient une note, ils sont d'accord à 3 %
  pour au moins 98 % des sons ; la part de sons « sans note » est publiée ; en dessous de 98 %, la
  note reste hors de la règle.

**Étape 5. L'écoute A/B côte à côte.** Les chiffres ne décident pas seuls.
- Avant / après : un MP3 par style, 40 s avant, 1 s de pause, 40 s après, rendu par
  `scripts/live-bench.sh` puis `ps_live` (la règle du projet).
- Trouver l'intrus : 30 séries de trois sons par séance (deux références, le nôtre, dans le
  désordre), toutes familles mélangées, au moins 3 séries par famille. Critère : Benjamin désigne
  le nôtre comme l'intrus 14 fois sur 30 au plus. Si on ne l'entend pas (une chance sur trois), ce
  critère passe 96 fois sur 100 ; s'il le reconnaît deux fois sur trois, il ne passe que 2 fois sur
  100. Le détail par famille n'est qu'un indice : trop peu d'essais pour conclure. (Le brouillon
  disait « 5 sur 10 » : un son reconnu deux fois sur trois passait encore une fois sur cinq.) Ces
  fichiers contiennent des références : ils restent privés, envoyés à Benjamin seulement, jamais
  dans le dépôt.
- Si l'oreille dit non quand les chiffres disent oui, c'est l'empreinte qui a un trou : on cherche
  la mesure qui manque avant de retoucher le son.

Étape 0 faite, étape 1 rejouée (53 % « sûr », pas vert) : voir `TIMBRE-ETAPE01.md`.

## 7. Limites : ce qui n'est pas prouvé

- **Aucun chiffre ne vient de la vraie collection**, sauf les fourchettes déjà publiées (essai
  `cibles`). Les familles inventées sont plus simples que de vrais sons.
- Le synthétiseur caché qui fabrique les familles inventées ressemble à la « voix plus riche » : ses
  bons résultats sont flatteurs. Et le succès de la voix d'aujourd'hui (19 sur 19) dit qu'elle
  *peut* atteindre ce genre de cible, pas qu'elle y arrivera sur toutes les vraies familles.
- La recherche vise des chiffres : elle peut les atteindre par un chemin que l'oreille n'aime pas.
  D'où la carte bandes-temps gardée hors cible, et l'étape 5.
- L'empreinte ne voit pas tout : le grain du métal (la façon dont les partiels battent entre eux),
  les très petites variations de l'attaque.
- Le coût sur téléphone est estimé par des rapports mesurés sur un autre processeur, et ces rapports
  bougent du simple au double selon le son pris comme repère.
- **Aucune mesure d'ici ne repère de façon sûre une copie redessinée avec notre bruit** (section 2). Les garde-fous
  chiffrés sont un filet contre la copie grossière, pas une preuve d'innocence.
- Les contre-mesures des bandes partagent trois morceaux de code avec la mesure ; deux nombres de
  l'empreinte n'en ont pas du tout.
- Les « 19 sur 19 » sont comptés sur les sons qui ont servi à régler.
- La partie juridique est une lecture prudente, pas un avis d'avocat.
- La note du kick : validée sur nos rendus et sur des sons inventés seulement.

## 8. Critique

Un relecteur neuf, qui n'a pas écrit l'étude, a eu pour seule consigne de la contredire. Il a
rejoué les essais `cibles`, `aveugle`, `note`, `copie`, `bruit` et le banc de coût, et remesuré
quelques chiffres avec son propre code.

### Confirmé par une nouvelle mesure

- Les chiffres rejoués sortent identiques, sauf un coût (le bruit en 24 bandes).
- Recalculés par un autre code : centre de gravité de la caisse claire dnb 437 Hz (étude : 440), du
  clap dnb 2 078 Hz (2 082), du crash dubstep 3 115 Hz (3 115) ; part d'aigus du clap dnb −26,1 dB
  (−26,07).
- Note du kick par une troisième méthode (autocorrélation) : house 41,75 Hz (étude : 41,37), techno
  50,01 Hz (49,06, soit 1,9 % d'écart). Sur le kick trap, sa méthode seule lit 45 Hz dans une queue
  de 46 ms, là où l'étude répond « pas de note » : c'est le cas prévu, un chemin seul lit toujours
  quelque chose, les deux ensemble savent se taire.
- 30 kicks inventés : pire écart 0,07 %, comme annoncé.
- Le self-test n'est pas décoratif : ses tolérances sont serrées, et `run.sh` s'arrête s'il rougit.
- Règles du dépôt tenues : aucun son, aucun nom de vendeur, de pack ou de fichier, aucun lien.

### Contesté, et corrigé dans le texte

1. **Le garde-fou « plus proche voisin » ne voit pas la copie redessinée.** Le brouillon disait
   qu'il la voyait « dès 22 bandes ». Faux : il comparait au voisin du son copié (0,42) et non au
   seuil du garde-fou (0,28). Aucune grille ne passe sous ce seuil en empreinte, deux seulement en
   carte. Section 2 réécrite ; la règle de travail passe devant les chiffres ; l'étape 0 doit
   étalonner le seuil et chercher une mesure qui attrape cette copie.
2. **« Chaque nombre vérifié par un deuxième calcul » était faux.** C'est 15 sur 17, et les deux
   chemins partagent le dessin des bandes, le repérage du début et le calcul de la chute. Résumé,
   section 4 et étape 0 corrigés.
3. **Le « 19 sur 19 » est presque automatique** : mêmes sons pour régler et pour compter, meilleur
   de trois départs. Dit en tête de la section 5 ; l'étape 2 demande maintenant des sons mis de
   côté.
4. **Le coût « au plus 1,7 à 1,9 fois » était trop précis** : le repère est une seule recette, et la
   mesure bouge d'un passage à l'autre. Remplacé par « de l'ordre de 1,5 à 2 fois, à refaire sur le
   téléphone ».
5. **L'écoute « 5 sur 10 » ne prouvait rien** : un son reconnu deux fois sur trois passait encore
   une fois sur cinq. Remplacé par 30 séries et un seuil de 14.
6. **L'arrêt Pelham était un peu étiré** : il parle d'extraits pris, pas de sons refaits. Précisé,
   avec deux décisions américaines qui se contredisent sur les extraits très courts.
7. **Formules floues** : « aigus 6 à 12 dB trop bas » (c'est 6 à 7 dB sous la fourchette, 12 sous la
   médiane), « 23 sur 23 » présenté comme un résultat (les recettes ont été choisies là-dessus),
   portée de l'essai `aveugle` (les enveloppes n'y bougent pas). Précisé en section 1.
8. **Deux techniques manquaient au classement** : la membrane physique dont la tension se relâche,
   et le bruit dans un filtre qui change au fil du son. Ajoutées en classe C.

### Ce qui reste ouvert

- Le relecteur n'a pas rejoué `famille` ni `contre` (trop longs) : il les a lus. Il n'a pas relu
  les textes de loi, ni mesuré quoi que ce soit sur un téléphone.
- Aucune mesure ne sépare encore « s'inspirer d'une famille » de « redessiner un son avec notre
  bruit ». Tant que c'est le cas, la limite légale tient par la méthode, pas par un chiffre.
- Le chiffre de coût par recette du relecteur vient de son propre banc, qui n'est pas dans le
  dépôt : `run.sh bench` devrait mesurer plusieurs recettes avant l'étape 3.

## 9. Rejouer et sources

```
scripts/son/timbre/run.sh                 toute l'étude (une trentaine de minutes sur quatre cœurs)
scripts/son/timbre/run.sh cibles note     seulement ces essais
scripts/son/timbre/run.sh bench           coût des blocs candidats
```

Essais : `cibles`, `aveugle`, `bruit`, `modes`, `copie`, `contre`, `note`, `famille`. Fichiers :
`render_voice.cpp` (la vraie voix du moteur, hors-ligne, et le banc de coût), `timbre.py` (mesures,
contre-mesures, distances, recherche), `selftest.py` (chaque mesure sur un son dont on connaît la
réponse), `etude.py` (les essais). Rien n'est écrit dans le dépôt par ces scripts.

Sources :

- Cour de justice de l'Union européenne, Pelham c. Hütter, C-476/17, 29 juillet 2019 ; directive
  2001/29/CE, article 2 c.
- États-Unis, 17 U.S.C. § 114(b) ; Bridgeport Music c. Dimension Films (6e circuit, 2005) ; VMG
  Salsoul c. Ciccone (9e circuit, 2016).
- Serra et Smith, « Spectral Modeling Synthesis », Computer Music Journal, 1990 (sinusoïdes + bruit).
- Roy et Kailath, « ESPRIT », IEEE Transactions on Acoustics, Speech, and Signal Processing, 1989.
- Hansen, « The CMA Evolution Strategy: A Tutorial », arXiv 1604.00772.
- Engel et al., « DDSP: Differentiable Digital Signal Processing », ICLR 2020.
- Shier et al., synthèse différentiable de batterie avec attaques, arXiv 2309.06649.
- Han, Lostanlen, Lagrange, distance perceptive pour retrouver les réglages d'un tambour,
  arXiv 2311.14213.
- Comparaison CMA-ES / descente de gradient sur un synthétiseur à 28 réglages, arXiv 2603.15905.
