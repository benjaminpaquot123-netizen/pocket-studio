# Audit de la méthode « Mine d'or 2 » (2026-11-26)

Demande de Benjamin : vérifier que les neuf discussions « tout RAR 1 à 9 » (et les deux à venir)
tirent le maximum d'informations réelles des sons, pour aider le programme.

Lu pour cet audit : la fiche envoyée aux neuf discussions, `scripts/sons/mesure.py`,
`scripts/sons/mine_or.py`, `scripts/sons/selftest.py`, `docs/sons/ANALYSE-MINE-OR.md`
et les neuf tableaux `docs/sons/rar/rarN.csv` (branches `eq/mine-or-rarN`).
Au moment de l'audit, les neuf discussions venaient de démarrer : aucune branche `-tout` poussée.

## Verdict en une phrase

La batterie est bien mesurée (outil vérifié, 1 871 coups). Tout le reste est seulement **compté**
(type, durée, niveau) : la fiche demande un inventaire, pas une compréhension. Les informations les
plus utiles au programme (accords, rythmes, notes, qui joue avec qui) ne sont pas demandées.

## Ce qui est bien fait

- Outil de batterie `mesure.py` vérifié sur des signaux connus (140 contrôles verts) avant de mesurer.
- Règles de confiance : une queue coupée ou un son sans hauteur n'entre pas dans les moyennes.
- Intégrité des archives contrôlée, rien de jeté, rien de sensible dans le dépôt (lignes anonymes).
- Une discussion par archive, une branche par discussion : pas de mélange.

## Ce qui manque, par ordre d'utilité pour le programme

1. **MIDI** (le plus rentable : fiable, rapide, sans audio). La fiche demande le nombre de fichiers,
   de notes et de mesures. Il manque : la gamme, la suite d'accords en degrés (I, V, vi…), le rythme
   des accords, la tessiture, les vélocités, le swing, la densité de notes, le tout par style et par
   rôle (basse, accords, mélodie, batterie). C'est la nourriture directe du générateur de morceaux.
2. **Boucles de batterie**. Demandé : durée, niveau, tempo de l'étiquette. Manque : tempo mesuré et
   comparé à l'étiquette, nombre de mesures, grille de doubles croches (où tombent le kick, la
   caisse, les charleys), swing, densité. C'est ce qui donne les motifs par style.
3. **Kits de construction**. Demandé : un inventaire à plat. Manque : un numéro de kit commun à
   toutes ses pistes, le tempo et la tonalité du kit, les rôles présents, le niveau de chaque piste
   par rapport au kit entier (référence de mixage), la bande de fréquences occupée par chaque rôle.
4. **Sons mélodiques isolés** (basses, synthés, claviers, nappes). Demandé : durée et niveau.
   Manque : la note réellement jouée (et l'écart avec l'étiquette), l'enveloppe (attaque, tenue,
   relâchement), la brillance au fil du temps, les harmoniques, la largeur stéréo, bouclable ou non.
   C'est ce qui sert à régler les sons du synthé.
5. **Voix et effets**. Manque : sec ou réverbéré, hauteur moyenne et tessiture d'une voix, longueur
   en temps (une montée de 4 ou 8 mesures), forme de la montée (niveau et brillance au fil du temps).
6. **Préréglages** : seulement comptés par synthé. À garder ainsi (peu utile sans le synthé).

## Faiblesses de méthode

- **Neuf outils au lieu d'un.** La fiche dit « ton propre petit script » pour le niveau, la durée,
  le tempo, la tonalité. Neuf discussions = neuf calculs différents, non vérifiés, donc neuf
  tableaux qu'on ne peut pas additionner. C'est contraire à la règle « l'outil de mesure se vérifie
  avant de mesurer ».
- **Aucune liste de colonnes imposée.** Déjà visible sur la batterie : trois tableaux sur neuf
  n'ont pas la colonne `error`, les entêtes et les tris diffèrent.
- **Pas de critique à l'aveugle** demandée pour cette vague, alors que la première en avait une.
- **Tempo et tonalité « estimés si fiable »** sans définition de « fiable » : chacun jugera à sa façon.
- **Les fichiers sont effacés à la fin.** Toute mesure oubliée coûte un troisième téléchargement.
- **Aucune comparaison avec les sons actuels de l'appli** passés dans le même outil. Sans elle, on
  sait ce que valent les packs, pas ce qu'il faut changer dans le programme.

## Défauts trouvés dans les tableaux de batterie (première vague)

Chiffres relus sur les neuf tableaux (1 871 lignes) :

- 921 lignes (49 %) sans style (« unlabelled », le gros pack de batterie) : la moitié des données
  ne peut nourrir aucune cible par style.
- Kicks (415) : 84 ont la queue coupée (20 %), 88 n'ont pas de mesure d'harmoniques (21 %),
  25 ont une note sous 30 Hz et 11 au-dessus de 100 Hz (mesure fausse ou son mal classé : à vérifier).
- 98 % des kicks sont poussés au maximum : la colonne de niveau de crête n'apprend rien.
- 145 charleys rangés « hat » sans dire ouvert ou fermé, alors que la durée mesurée le dirait.
- Classement par le nom seulement : jamais contrôlé par la mesure (un « snare » qui sonne comme un clap passe).
- Le mot « bass » fait écarter le fichier : des 808 rangés hors du dossier des kicks ont pu être perdus.
- Les couches « attack » et « tail » sont écartées : ce sont pourtant les briques dont le moteur a besoin.
- Pas de courbe de chute du kick (combien de temps met la note à descendre, et avec quelle forme) :
  seulement deux points, le début et la fin.
- Harmoniques jamais recoupées sur de vrais fichiers ; note stable imprécise fichier par fichier
  (déjà écrit dans `ANALYSE-MINE-OR.md`, § 5).
- Ni drum and bass ni rave dans les neuf archives.

## Recommandations au chef d'orchestre

1. **Tout de suite, aux neuf discussions en cours** : ne pas effacer les fichiers avant d'avoir
   ajouté le MIDI détaillé (point 1) et le numéro de kit (point 3) ; ce sont des ajouts sûrs.
2. **Un outil commun** `scripts/sons/tout.py` avec son self-test (signaux fabriqués : une boucle à
   tempo connu, un accord connu, un MIDI connu) et **une liste de colonnes unique** par type
   (son isolé, boucle, piste de kit, voix, effet, MIDI). Une seule discussion l'écrit ; les autres
   le lancent. Les deux archives à venir partent avec lui.
3. **Étiquette contre mesure** : garder les deux colonnes (tempo, tonalité, note) et leur écart ;
   une estimation sans étiquette n'entre dans une moyenne que si l'outil a fait moins de 5 %
   d'erreur sur les fichiers étiquetés du même type.
4. **Batterie** : repasser les neuf tableaux avec le tri ouvert/fermé par la mesure, la courbe de
   chute du kick, les couches attack/tail gardées à part, le style du gros pack retrouvé par dossier.
5. **L'appli dans le même tableau** : rendre les sons actuels de BenStudio (batterie, basses, leads)
   et les mesurer avec le même outil ; l'écart, style par style, devient la liste de travail.
6. **Critique à l'aveugle** de la synthèse finale, comme pour la première analyse.

## Ce qui n'est pas vert

- Les neuf discussions travaillent en ce moment avec la fiche d'inventaire, sans outil commun.
- Aucune branche `-tout` n'était poussée au moment de l'audit : leur contenu réel n'a pas pu être relu.
- La moitié de la batterie est sans style ; drum and bass et rave absents.
- Cet audit ne change rien dans l'appli ni dans les outils : il propose, le chef décide.
