# Plan d'amélioration (2026-10-23)

Ce plan découle de trois sources :
- l'audit de 8 testeurs (5 amateurs, 3 pros), dans `AUDIT-33.md` et `audit-33/` ;
- le comptage des gestes de 41 actions, dans `plan/clics.md` ;
- l'étude d'une quinzaine d'applis (GarageBand, BandLab, FL Studio Mobile, Ableton Note, Koala, Remixlive, Launchpad, Elektron, EP-133…), dans `plan/ailleurs.md`.

Pour le son, il s'appuie sur un diagnostic mesuré sur des rendus réels du moteur, dans `plan/son.md`.

Ces quatre documents gardent le détail : fichiers, lignes, sources, licences.

## Où on en est (mesuré)

- **Finir un morceau** demande trop de gestes et trop de notions.
  - 15 actions clés coûtent environ 75 gestes ; la cible est 26 (−65 %).
  - Un fill coûte 16 gestes, les accords de tout le morceau environ 30, un fondu d'entrée environ 30, changer de kit 9 (80 pad par pad).
  - Il y a 6 notions pour une seule idée : scène, partie, section, morceau, boucle, ligne du temps.
  - 3 vues font l'arrangement et 3 « Live » se mélangent à la création.
- **Le son** est à environ la moitié d'un vrai morceau.
  - Les Drops sortent à −10,7 LUFS en médiane, alors que la cible club est de −8 à −6.
  - Le true peak monte jusqu'à +0,7 dBTP : le M4A sature.
  - Les aigus sont ternes : −27 à −33 dB au-dessus de 6 kHz, pour une cible de −16 à −20.
  - Les instruments sont en mono et le kick est mou (une sinusoïde de 47 Hz qui sonne 1,5 s).
  - Le compresseur ajoute +3,6 dB même sans compresser.
  - Il n'y a qu'une réverbe, et elle est mono.
  - Le Drop n'est que de 0 à 3 LU plus fort que le Groove.

## Objectifs chiffrés (contrôlés par la CI)

1. **Gestes.** Une action fréquente se fait en 2 gestes au plus depuis l'écran où l'on est, et en 3 au plus depuis n'importe où. Le budget de chaque action, colonne « Cible » de `plan/clics.md`, est vérifié par un test qui compte les touchers.
2. **Son, mesuré au banc sur les 36 styles.**
   - True peak ≤ −1 dBTP sur 100 % des morceaux.
   - LUFS à ±1 de la cible du genre ; un préréglage « Streaming » à −14 LUFS.
   - Drop − Groove ≥ +2 LU et Drop − Break ≥ +4 LU.
   - Air (au-dessus de 6 kHz) entre −20 et −16 dB ; pente du spectre à −4,5 ±0,7 dB par octave.
   - Stéréo : side/mid entre −15 et −9 dB, et graves en mono (corrélation ≥ 0,95 sous 120 Hz).
   - Kick ≤ 500 ms ; repliement du synthé ≤ −60 dB.
3. **Parcours.** Un débutant va de « Nouveau projet » à « Partager » sans aide. Il change un son, ajoute une fin et partage en moins de 10 gestes au total.

## Principes (tirés du comptage et des autres applis)

1. **Toucher l'objet ouvre ses actions** : une partie, une piste, un pad, un pas ou une note, à l'endroit où on le regarde. L'appui long veut dire partout « options de cet objet ».
2. **Une intention, un bouton** : Break, Fill, Fin, Montée, Fondu, Énergie, Kit, Accords du morceau, Équilibrer, Masteriser. Les outils techniques (automation, scènes, inserts) restent dessous, et ce que font ces boutons y reste visible et modifiable.
3. **La portée par défaut est la plus large utile, et elle est dite.** Un son, un kit ou des accords valent pour tout le morceau. Si on modifie une partie jouée 3 fois, on demande : « toutes ou celle-ci ? ».
4. **▶ joue le morceau, toujours.** Boucler une partie est un choix visible (« ⟳ Partie »).
5. **Glisser plutôt que toucher N fois** : peindre des pas, déplacer, étirer, régler.
6. **Après chaque création ou prise, le choix suivant se fait en 1 geste** : Garder, Remettre en rythme, Refaire, Écouter.
7. **Mesurer avant de changer le son.** Chaque phase « son » donne des chiffres avant et après, et des A/B à écouter.

## Les phases

Chaque phase a deux chantiers indépendants, Interface et Son, faits l'un après l'autre en mode économie. Chaque phase se termine par une relecture neuve, une CI verte, une fusion, et un APK avec les A/B à écouter.

### Phase 33 : « Finir son morceau » (gains rapides) + son mesuré et honnête
**Interface :**
- ▶ joue le morceau partout. Une puce « ⟳ Partie » sert à boucler, et la position (mes. 14 / 60) reste visible pendant la lecture.
- ⤴ Partager dans la barre du haut, avec une feuille « Prêt à partager ? » qui affiche la durée et vérifie solo oublié, saturation et morceau vide, avec réparation en un geste. WAV et M4A sont proposés.
- Écran « Ton morceau est prêt » après l'assistant (Écouter, Changer un son, Structure, Partager), puis un fil « Étape suivante » visible sur tous les onglets.
- Toucher le tempo ouvre un pavé avec des puces (70, 90, 120, 140, 174, ½, ×2). Appui long sur le nom pour renommer.
- Bandeau « Solo actif : tout désactiver ».
- Vocabulaire : seulement « Partie », « Morceau » et « Projet ». L'onglet Morceau devient « Structure ».

**Son :**
- Étape 0, le banc de mesure : enregistrer les commandes du moteur pendant un test de l'export réel, les rejouer avec un petit programme hôte, et mesurer avec libebur128 (MIT). La CI publie un rapport sur les 36 styles.
- Étape 1, le gain staging : gain de compensation manuel ou automatique, indicateur de réduction, détecteur RMS ou crête, mélange sec / traité ; Saturation sans filtre caché et avec suréchantillonnage ; niveaux du kit revus.
- Étape 2, la chaîne master :
  - écrêteur doux suréchantillonné (Airwindows ClipOnly, MIT, avec HIIR) ;
  - limiteur true peak (Signalsmith, MIT, ou x42, GPL-3) ;
  - LUFS et true peak affichés en direct ;
  - cible de volume par genre, plus un préréglage Streaming.

**Critères :**
- Budgets de gestes tenus pour Écouter, Partager, Tempo et Renommer.
- True peak ≤ −1 dBTP sur 100 % des morceaux ; LUFS à ±1 de la cible ; les chaînes par défaut restent neutres (±0,3 dB).

### Phase 34 : « Une intention, un bouton » + batterie de studio
**Interface :**
- La feuille de partie (appui long ou ⋯) propose :
  - **Variation** : copie indépendante, avec l'avertissement « jouée 3 fois » ;
  - **Fill** sur la dernière mesure, **Break** (sans kick ni basse) et **Montée avant** (filtre et riser) ;
  - **Fondu d'entrée** et **Fondu de sortie** ;
  - **Fin**, qui ajoute une vraie fin ;
  - **Énergie 1 à 4**, qui retire ou ajoute des couches et de l'intensité ;
  - **Allonger** (étirer la longueur).
- Puce « Kit ▾ » en tête de la batterie. « Changer le son » depuis le nom d'une piste, avec une liste nommée et ▶.
- Appui long sur un pad : son, volume, pan.
- Peindre les pas en glissant le doigt. Roulements ×2 à ×8, montants ou descendants, plus 1/32.

**Son**, étape 3 :
- kits échantillonnés par genre (sources CC0 : Freesound CC0, VCSL, Sonic Pi…) ;
- kicks refaits (rendus hors ligne avec Geonkick, GPL-3) ;
- couches sub + attaque et clap + caisse claire ;
- niveaux normalisés par rôle, alternance d'échantillons et couches de vélocité.

**Critères :**
- Fill, Variation, Montée, Fondu, Fin et Kit en 2 gestes ; peindre une ligne en 1 geste.
- Kit refait : kick ≤ 500 ms, air du Drop entre −20 et −16 dB, préféré à l'aveugle au moins 8 fois sur 10.

### Phase 35 : « Enregistrer sans peur » + stéréo et espace
**Interface :**
- « ● Enregistrer » dans chaque éditeur (Clavier, Notes, 303, Audio), avec un décompte de 4 temps et le métronome par défaut. Le compteur indique « Couplet · mes. 5/16 ».
- Après la prise : Garder, Remettre en rythme (sans rien détruire), Refaire.
- « Garder ce que je viens de jouer » : l'appli se souvient des 8 dernières mesures jouées.
- Voix :
  - repère de mesure, recalage sur le tempo ;
  - punch-in « Refaire mes. 5-8 » ;
  - plusieurs prises, plus de 4 pistes.
- Live :
  - ● dans les pads en plein écran, et les pads sont enregistrés ;
  - chaque prise crée une nouvelle version : on n'écrase plus le morceau par défaut, et un journal liste les versions ;
  - jouer des notes en live sur un clavier ou des pads de notes ;
  - quantification à 1 temps et à ½ mesure.

**Son**, étapes 4 et 5 :
- pistes d'instruments en stéréo (unisson étalé, échantillons stéréo, SF2) ;
- réverbe plate (Mutable, MIT, déjà présent) et salle ; égaliseur de retour et graves en mono ;
- vrai sidechain déclenché par l'audio du kick, et un bus « Musique » qui ne pompe qu'une fois.

**Critères :**
- Enregistrer une mélodie et la remettre en rythme en 3 gestes ; aucune prise Live n'écrase le morceau sans retour possible.
- Side/mid entre −15 et −9 dB ; graves en mono ; la basse baisse de 4 à 8 dB en moins de 10 ms à chaque kick.

### Phase 36 : la nouvelle navigation (et le Mode simple)
**Interface :**
- Quatre ou cinq espaces au lieu de 7 onglets : **Structure**, **Sons**, **Mix**, **Live**, **Plus**.
  - **Structure** est la seule vue d'arrangement : la ligne du temps, avec la liste des parties en simple bascule, M/S sur les pistes, et un double-toucher sur un clip pour l'éditer.
  - **Sons** regroupe Rythme, Acide, Synthé et Audio, avec des puces de pistes en haut ; il rouvre la dernière piste éditée.
  - **Mix** est une console : faders côte à côte, et une feuille par voie (effets, automation).
  - **Live** est sorti du flux de création, avec une seule entrée.
  - **Plus** regroupe projets, réglages, aide, MIDI et latence.
- Le **Mode simple**, au choix au premier lancement et dans les réglages, a 4 onglets : Mon morceau · Rythme · Clavier · Envoyer. Il utilise des mots français (pas « Lead », « 303 » ni « Quantifier ») et ajoute une famille « Calme (Chill, Lo-fi) ».

**Critère :** le total des 15 actions clés passe d'environ 75 à 26 gestes au plus.

### Phase 37 : « Écrire comme un pro » + production de l'assistant
**Interface :**
- **Accords du morceau** : des accords nommés, que toutes les parties et pistes suivent (comme la piste d'accords de Logic), avec une écoute avant d'appliquer.
- Piano roll :
  - toucher sélectionne, appui long supprime ;
  - presse-papiers commun aux pistes et aux parties ;
  - boucles d'une longueur libre à la mesure près ;
  - vélocités en glissé.
- Batterie : une longueur par pad (polyrythme) et une probabilité par pas.
- Automation :
  - tous les potards du synthé et de Plaits, par appui long sur un potard puis « Automatiser » ;
  - résolution au ¼ de mesure, et noms des parties sur les courbes.
- « Geler la piste » : un rendu audio, que l'on peut annuler.

**Son**, étape 6 :
- humanisation de la vélocité et du placement ;
- fills toutes les 8 et 16 mesures ;
- crash, impacts, cymbale inversée et descente ;
- montées de 8 mesures et courbe d'énergie par partie.

**Critères :** les accords de tout le morceau en 3 gestes ; Drop − Groove ≥ +2 LU, Drop − Break ≥ +4 LU ; au moins 6 mesures de batterie différentes par morceau.

### Phase 38 : le son « présentable »
**Son**, étapes 7, 8 et 9 :
- **Synthé :** suréchantillonnage (HIIR ou Signalsmith), filtres sst-filters (GPL-3), une voix 808 / sub dédiée avec glide.
- **Instruments échantillonnés multizones :** piano Splendid (domaine public), Salamander (CC-BY) et VSCO-2 (CC0).
- **« Équilibrer mon mix »** : mesure de chaque piste, gain et passe-haut selon son rôle, avec Annuler.
- **« Masteriser »** : égaliseur vers la courbe du genre, cible de volume, A/B.
- **Export propre :** vérification du true peak avant le M4A, rapport de fin d'export, 44,1 kHz, 16 bits avec dither.

**Interface :** une piste 808 en un geste dans « Ajouter une piste » (glide actif, sans pack à télécharger) et le bouton « Équilibrer » dans Mix.

**Critères :**
- Repliement ≤ −60 dB pour le synthé et ≤ −50 dBc pour la distorsion, dans le budget CPU.
- Chaque partie à ±1,5 LU de sa cible ; LUFS à ±0,5 ; true peak ≤ −1.
- Le M4A décodé ne sature jamais.

### Phase 39 : apprendre, partager, le clavier MIDI
- Missions « Mon premier morceau ».
- Un « ? » qui étiquette l'écran.
- Glossaire avec un exemple sonore pour chaque mot.
- Export vidéo 9:16 (30 s du refrain, forme d'onde, noms des parties).
- Potards MIDI assignés automatiquement selon l'écran (idée gardée de côté par Benjamin).

## Ce que Benjamin aura à faire (une demande groupée par phase)

- Écouter les A/B avant / après fournis dans chaque PR, au téléphone et au casque, et dire lequel il préfère.
- Optionnel : donner 2 ou 3 titres de référence par style qu'il aime. Seules les courbes mesurées entrent dans le dépôt.
- Optionnel : une clé API Freesound gratuite, pour accélérer le choix des kits.

## Décisions prises en attendant la réponse de Benjamin (modifiables)

- 1A : nouvelle navigation à 5 espaces, en phase 36, après les gains rapides.
- 2A : Mode simple, en phase 36.
- 3A : d'abord finir un morceau, avec le son mené en parallèle à chaque phase. Benjamin a insisté sur le son.
