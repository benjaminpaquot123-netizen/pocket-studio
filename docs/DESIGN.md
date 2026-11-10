# Ligne graphique

Décision de Benjamin (2026-10-18) : **moderne, mais pas trop, et facile à prendre en main.** Modèle : **FL Studio Mobile, en plus clair.**

## Principes
1. **Une action principale évidente par écran** (gros bouton, en haut ou au centre). Le reste est secondaire.
2. **Pro, façon FL Studio Mobile en clair** (mise à jour du 2026-10-20, « que ça claque ») : panneaux graphite en relief (dégradé léger du haut vers le bas, liseré clair en haut, ombre douce, coins de 16 dp) sur un fond plus sombre, accent orange, vert réservé à « ça joue / en cours ». Les halos sont réservés à ce qui joue ou à ce qui est choisi (pads, pas sous la tête de lecture, scène en cours, bouton Lecture, vignette choisie). Police Inter, chiffres à chasse fixe pour tempos et décibels.
3. **Lisible** : texte d'au moins 14 sp, contraste suffisant, majuscules réservées aux petites étiquettes de section.
4. **Fait pour les doigts** : cibles tactiles d'au moins 48 dp, pads d'au moins 64 dp, espace entre les zones tactiles.
5. **Des mots, pas du jargon** : icône + mot en français courant (« Jouer », « Annuler »), le détail technique derrière « Avancé ».
6. **Retour immédiat** : chaque geste se voit (état, couleur) et peut se sentir (légère vibration sur les pads).
7. **Animations courtes et utiles** (150 à 250 ms), jamais décoratives ; fluides à 120 Hz.
8. **Cohérence** : tous les écrans utilisent les composants de `ui-components` (cartes, boutons, potards), jamais de style ad hoc.
9. **Repliable** : chaque carte à titre se replie en touchant son titre ; la flèche est petite et pâle, pour ne pas gêner.
10. **Un mot par idée** (audit final) : « Partie » = un moment du morceau (Intro, Couplet, Drop…), « Brique » = une case A, B, C… qui en donne le contenu, « Place » (pas « Panoramique »), « Courbes » (le verbe reste « Automatiser »), « Partager » = sortir un fichier (réponse 1A de Benjamin, 2026-11-09 : plus d'« Envoyer » ni d'« Exporter » sur le chemin du partage ; la liste s'appelle « Mes fichiers », le .zip du projet « Sauvegarder le morceau en .zip »). Anglais : part, block, pan, curves ; la case de la ligne du temps = cell (néerlandais : deel, blok, pan, curven, vakje). Le Mode simple ne cite que ses onglets (Mon morceau, Rythme, Clavier, Mix, Partager, ⋮) et aucun chiffre technique (Hz, dB, LUFS).
11. **Pareil que les voisins** (Benjamin, 2026-11-06, accueil : icônes rondes et carrées mélangées) : tout ce qu'on ajoute à un écran (carte, bouton, icône, puce) se compare d'abord à ses voisins sur la capture de l'écran **entier** : même forme et même taille d'icône, titres alignés au même endroit, mêmes marges, mêmes coins, même famille de couleurs, même place pour la flèche. Une exception (une carte « mise en avant ») se voit par sa couleur, jamais par une forme ou une taille dépareillée.

Règles 12 à 22 : audit de prise en main (2026-11-09, `docs/audits/AUDIT-PRISE-EN-MAIN.md` § 6), ajoutées par le lot « composants » (2026-11-10).

12. **Un lien a l'air d'un lien** : tout ce qui ouvre un autre écran est une `LinkRow` (fond, icône, titre, chevron) ; jamais une phrase colorée seule.
13. **Un repli montre ce qu'il cache** : `Disclosure` avec titre + résumé des valeurs ; un réglage dont dépend le résultat (longueur, format) n'est jamais sous « Avancé ».
14. **Une action = un bouton par écran** : un seul ▶, un seul Partager, un seul ⋮ visibles à la fois ; en portrait c'est la barre du pouce qui les porte.
15. **Une icône par action, celle d'Android** (Partager = `Icons.Filled.Share`) ; une couleur par sens : vert = joue ou réussi, orange = choisi ou action ; aucune troisième couleur d'accent sans ligne dans DECISIONS.md.
16. **Jamais de hauteur fixe autour d'un texte** : `heightIn(min)` et padding ; chaque composant à texte se vérifie à 1,3 et 1,5.
17. **Tout défilement se voit** : fondu en bas + puce « ↓ n autres » dès que du contenu dépasse l'écran.
18. **Après chaque action, une preuve et une suite** : un message qui nomme le résultat (fichier, courbe, prise) et un bouton vers lui ; un message vide ne contredit jamais ce qu'on vient de faire.
19. **Un lien dit où il mène**, jamais ce qu'il espère (« Ouvrir Effets · Batterie », pas « Voir le réglage » si l'écran n'y va pas).
20. **Même chose, même endroit, même mot dans les deux modes** ; une liste des noms autorisés par fonction, sous test (Lot2TextsTest, LexiconWordsTest).
21. **Un geste de jeu n'ouvre jamais une fenêtre** : les réglages d'un pad sont un badge visible ; un glissé ne déclenche pas un effet (touch slop) ; un effet qui part vibre.
22. **En paysage et à 130 %, chaque feuille défile ou se réorganise** ; une capture de la matrice par feuille (phoneLand 1,0 et 1,3).

## Composants
- `NeonCard` (panneau en relief, repliable par son premier `SectionLabel`), `SectionLabel` (barre de couleur + petites capitales), `ProSlider` (piste fine en dégradé, pouce lumineux), `pillColors` (puce choisie remplie d'accent), `LevelMeter` (LED vert → ambre → rouge).
- `LinkRow` (ligne qui ouvre : fond, icône, titre, sous-ligne, chevron ›), `Disclosure` (repli pleine largeur : nom, résumé de ce qu'il cache, valeur changée en orange, chevron qui tourne), `TrackChip` (pastille de piste : point de couleur, nom, son ; hauteur qui suit la police). Partager = `Icons.Filled.Share`, partout.
- `SoundTile`, `FamilyChip`, `TileGrid` : vignettes de sons (icône et couleur de famille via `SoundLooks`).
- Écrans : `DrumPad` (pads en relief qui s'allument), touches de clavier en relief, `FloatingKeyboard` (clavier qui suit).

## Références
FL Studio Mobile (organisation, couleurs franches, look « outil »), en plus clair ; simplicité de Koala Sampler ; lisibilité de Material 3.

## Boîte à effets (phase 25)
Une seule boîte d'effets, la même partout où un son se choisit : carte « Effets » sous le son (Synthé, en Clavier comme en Notes, et Acide), boutons « Effets » de la batterie et de chaque pad (Rythme), de chaque piste audio (Audio › Pistes audio : la prise passe par la voie de sa piste), du remix et des pads live (ils partagent la voie « Remix »), voies et sortie générale du Mixeur. Dedans : les effets en place dans l'ordre où le son les traverse (toucher pour régler, ⏻ couper sans perdre les réglages, ↑ ↓ déplacer, ✕ retirer), « + Ajouter un effet » (tous les effets par famille, la liste reste ouverte pour en ajouter plusieurs d'affilée), puis les ambiances toutes prêtes, celles qui vont au son en premier (« Pour : Basse »). La version complète (Mixeur, bouton Effets) ajoute pad XY, macros et envois communs. Quatre effets par voie au maximum (limite du moteur).

### Les effets les plus utilisés en musique électronique, et ce que l'app couvre
| Effet | Pourquoi on s'en sert | Dans l'app |
|---|---|---|
| Égaliseur (EQ) | creuser, faire de la place, couper les graves inutiles | Égaliseur 3 bandes, EQ paramétrique |
| Compresseur | serrer, faire claquer | Compresseur |
| Sidechain / pompe | la nappe ou la basse qui respire sur le kick | Pompe (kick ou chaque temps) |
| Réverbe | l'espace, la profondeur | Réverbe en insert + envoi réverbe commun |
| Delay / écho calé sur le tempo | 1/8 pointée, ping-pong, dub | Écho et Écho ping-pong en insert (1/16 à 1/2, croches pointées) + envoi écho commun |
| Distorsion / saturation | grain, mordant, leads qui crient, basses grasses | Distorsion (overdrive, fuzz, écrêtage dur, repliement), Saturation |
| Filtre (passe-bas / haut, balayé) | montées, breaks, filtre DJ | Filtre, filtre DJ du pad live, macro Couleur, automation |
| OTT / compression multibande | le son énorme et brillant de l'EDM | OTT multibande (3 bandes, vers le haut et le bas) |
| Chorus | épaissir, élargir | Chorus |
| Flanger / phaser | balayages, avion à réaction | Flanger, Phaser |
| Bitcrusher / lo-fi | grain numérique, lofi | Bitcrusher, Vieux vinyle, Talkie-walkie |
| Élargisseur stéréo | largeur, basses au centre | Élargisseur |
| Auto-pan / trémolo | mouvement gauche-droite, pulsation | Auto-pan calé sur le tempo, Trémolo |
| Gate rythmique / trance gate | hacher en rythme | Hachoir |
| Arrêt de bande (tape stop) | fin de phrase, transition | Arrêt de bande (bouton « Arrêt » ou automation), arrêt du pad live |
| Beat repeat / stutter | répétitions de drop | Répétition du pad live (sortie générale) |
| Modulateur en anneau | sons métalliques, voix robot | Robot |
| Décalage de fréquence | métal, cloches, tourbillon | Décalage de fréquence |
| Wah / filtre d'enveloppe | funk, basse qui parle | Wah-wah |
| Noise gate | couper les queues, nettoyer | Noise gate |
| Pitch shift / harmoniseur | voix plus aiguë ou plus grave | pas encore (pitch des pads et des prises seulement) |
| Vocoder, granulaire, convolution | voix robot musicale, textures, vraies salles | pas encore |
| Limiteur / maximiseur | volume final | limiteur de la sortie générale (fixe) |

## Lumière des pads et des cases (n° 25, lot B1)
Un seul composant, `PadLight` (`ui-components/.../PadLight.kt`), sur tous les écrans à pads et à cases (Rythme : pads, pas, « Tous les pads » ; Live : briques, pads live, plein écran ; Pad Live). Pad ou case inactifs : rien ne bouge, jamais (ni onde, ni souffle, ni clignotement). Qui joue : lumière intérieure douce et fin anneau d'avancement de sa boucle ; en attente de son temps : l'anneau pâle, immobile ; touché : petite onde de 200 ms au point du doigt ; effet maintenu : léger voile de couleur sur les bords. Allumage 150 ms, extinction 250 ms. Test « partout » : `StillPadsEverywhereTest`.
