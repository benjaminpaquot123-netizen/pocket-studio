# Remix dans un projet : la chanson devient un vrai morceau de travail

Demande de Benjamin (2026-10-30, vidéo APK 233) : une fois importée, la chanson séparée doit faire partie du projet à part entière, et rester facile à trouver. Les quatre pistes doivent être accessibles, retouchables, isolables et découpables, quel que soit le choix fait à l'écoute. « Recommence ta réflexion et pousse encore plus. »

## Ce qui ne va pas aujourd'hui
- **Introuvable.** La chanson n'existe que comme une voie « Remix » dans Mix et une colonne dans le Live. Structure affiche « morceau vide » et Sons ne la montre pas.
- **Incohérent.** Le choix fait à l'écoute (la voix seule) se perd au premier ▶ : tout le morceau rejoue.
- **Éparpillé.** Beaucoup de briques existent déjà, mais chacune est dans son coin et on ne les trouve pas :
  - la séparation (Demucs) ;
  - la détection du tempo et du premier temps (`Analysis.h`, `detectTempo`) ;
  - la détection de la tonalité (`detectKey`) ;
  - la détection des attaques (`OnsetDetector`) ;
  - le « Tableau des pistes » (`StemBoard`) : choisir un passage, l'envoyer sur un pad, un instrument, une piste ou un pad live, découper la voix en phrases ;
  - « Ma bibliothèque ».

## Le principe
**La chanson importée est un objet du projet, comme une piste de batterie.** Elle a un nom (« Boys »), une couleur et un dossier de quatre pistes audio : Voix, Batterie, Basse, Reste. On la retrouve au même endroit, avec le même nom, sur chaque écran :

| Écran | Ce qu'on y voit |
|---|---|
| **Sons** | Une carte « Chanson : Boys » avec ses quatre pistes. Chaque piste a sa forme d'onde, Solo, Muet et Volume. Un toucher sur la carte ouvre le Tableau des pistes. |
| **Structure** | Un dossier « Boys » repliable. Ses quatre lignes audio sont calées sur les mesures du projet. |
| **Mix** | Un groupe « Boys » : un fader pour toute la chanson et un fader par piste, avec leurs effets. |
| **Live** | Une colonne par piste (ou une seule colonne « Boys » repliable). Chaque partie détectée devient une case. |
| **Pads** | Les découpes (phrases de la voix, coups de la batterie) se posent sur des pads. |
| **Ma bibliothèque** | Tout ce qu'on « garde » de la chanson, rangé sous « Boys ». |

Le choix fait à l'écoute ne sert qu'à décider quelles pistes arrivent en muet. **Les quatre pistes entrent toujours dans le projet.**

## L'import, en un seul écran clair : « Mettre Boys dans mon projet »
1. **Le tempo (précision de Benjamin).** Le tempo se cale automatiquement sur le premier son à tempo qu'on pose (sur le Live ou dans le projet) : une boucle ou une chanson fixe le tempo général. Un son court (un coup, un cri) ne le fixe pas. Un bandeau l'annonce : « Tempo calé sur Boys : 113 · Annuler ».
   - Les sons posés ensuite sont étirés sur ce tempo (signalsmith-stretch) et restent calés.
   - Le premier temps détecté est posé sur la mesure 1, réglable au besoin.
   - **Le tempo général se règle facilement depuis le Live.** On touche le BPM en haut : − et + en grand, un glisser, « Taper » au doigt, ½ et ×2, et « Revenir au tempo de Boys ». Toutes les boucles et la chanson suivent en direct.
2. **La tonalité.** « Boys est en La mineur. » Proposé : « Mes synthés et basses jouent en La mineur » (le panneau d'harmonie se règle). Autre choix : « Transposer Boys ».
3. **La place.** « Toute la chanson, à partir de la brique A » (proposé par défaut), « Seulement un passage » (on le choisit sur la forme d'onde) ou « Juste la voix sur mon beat » (l'acapella, le remix classique).
4. **Les parties (le gros plus).** L'appli repère l'intro, les couplets, les refrains et les drops (analyse de structure : changements d'énergie et répétitions, calés sur les mesures). Elle propose : « Créer les briques Intro, Couplet, Refrain… à partir de Boys ». Chaque partie devient une brique, jouable tout de suite dans le Live et réutilisable dans le morceau.

Tout se fait en un seul écran, avec des choix par défaut sûrs. Un bouton « Mettre dans mon projet » valide, et « Annuler » défait tout en une fois.

## Retoucher : la chanson se travaille comme le reste
- **Sur chaque piste :**
  - solo, muet, volume, place, effets ;
  - couper (ciseaux sur la mesure), déplacer, dupliquer ;
  - boucler un passage, fondus, inverser ;
  - monter ou descendre en tonalité ;
  - étirer.
- **Aimantation aux mesures** par défaut, que l'on peut désactiver pour un réglage fin.
- **Non destructif.** Le fichier d'origine n'est jamais modifié : les retouches sont des réglages, et l'annulation marche partout.

## Aller plus loin : réutiliser la chanson comme matière
- **Voix en phrases sur les pads** : un toucher. Ça existe déjà dans le Tableau des pistes, il faut le rendre visible.
- **Batterie en kit** : les coups du kick, de la caisse et des charleys sont extraits de la piste Batterie, puis rangés en kit « Boys ». On les joue dans la grille de pas.
- **Basse en notes** : la ligne de basse devient des notes (suivi de hauteur). On peut la rejouer avec n'importe quel son de basse ou la 303.
- **Mélodie de la voix en notes** : même principe, pour la doubler au synthé.
- **« Garder »** depuis n'importe quel passage, vers Ma bibliothèque.

## Rien ne doit se perdre
- Les pistes séparées sont rangées dans le dossier du projet. Elles sont sauvegardées, exportées dans le zip du projet et comptées dans l'export du morceau et des pistes séparées.
- Chaque étape longue (séparation, analyse) montre sa progression, peut être annulée et reprend si l'appli est fermée en route.
- Au moment de partager publiquement, une ligne discrète rappelle que la chanson d'origine appartient à ses auteurs. Pas de blocage : c'est un usage personnel.

## Ordre de travail proposé
1. **R1, l'essentiel (le bug et la visibilité).**
   - Les quatre pistes deviennent des pistes audio du projet, regroupées sous « Boys ».
   - Elles sont visibles dans Sons, Structure, Mix et Live.
   - Le choix d'écoute ne fait que mettre en muet.
   - Le tempo est réglé et le premier temps calé.
   - Retouches de base : couper, déplacer, solo, muet, fondus.
   - Tests de bout en bout : import → sauvegarde → réouverture → export.
2. **R2, l'intelligence.**
   - L'écran « Mettre dans mon projet » (tempo, tonalité, place).
   - Les parties détectées deviennent des briques.
   - Le Live et les pads branchés.
3. **R3, la matière.**
   - Le kit de batterie extrait.
   - La basse et la voix en notes.
   - « Garder » partout, rangé sous le nom de la chanson.

Contrôles à chaque étape : les règles du n° 11 de la liste de corrections (tout connecté : enregistrement, bibliothèque, annulation, sauvegarde, export, MiniLab 3 et claviers MIDI) et le navigateur de sons unique (n° 14).
