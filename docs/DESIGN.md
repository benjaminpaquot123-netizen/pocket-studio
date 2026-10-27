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
10. **Un mot par idée** (audit final) : « Partie » = un moment du morceau (Intro, Couplet, Drop…), « Brique » = une case A, B, C… qui en donne le contenu, « Place » (pas « Panoramique »), « Courbes » (le verbe reste « Automatiser »). Anglais : part, block, pan, curves ; la case de la ligne du temps = cell (néerlandais : deel, blok, pan, curven, vakje). Le Mode simple ne cite que ses onglets (Mon morceau, Rythme, Clavier, Envoyer, ⋮) et aucun chiffre technique (Hz, dB, LUFS).

## Composants
- `NeonCard` (panneau en relief, repliable par son premier `SectionLabel`), `SectionLabel` (barre de couleur + petites capitales), `ProSlider` (piste fine en dégradé, pouce lumineux), `pillColors` (puce choisie remplie d'accent), `LevelMeter` (LED vert → ambre → rouge).
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
