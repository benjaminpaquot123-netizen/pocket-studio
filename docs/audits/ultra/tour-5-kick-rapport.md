# Tour 5 — lot « kick » : rapport

Constat de Benjamin (HONOR, haut-parleur intégré, questions à l'oreille du 2026-11-12) : « ça tape mais pas assez,
surtout le kick » en house, trap et techno ; en Bluetooth « c'est beaucoup mieux le kick ». Le son de l'appli est
bon ; c'est le petit haut-parleur qui ne rend presque rien sous 200 Hz, là où le kick met son poids (45 à 65 Hz).

## Recherche (sources)
- Basse virtuelle (« fondamentale absente ») : l'oreille entend une note grave absente quand ses harmoniques sont là ;
  les systèmes pour petits haut-parleurs (téléphones, TV plates) ajoutent ces harmoniques par traitement non linéaire
  sous une fréquence de coupure, le signal d'origine gardé. Défaut connu : l'attaque peut s'étaler sur les sons
  percussifs. [NTU, Virtual Bass System](https://www3.ntu.edu.sg/home/ewsgan/VBS.html) ;
  [thèse NTU, petits haut-parleurs](https://core.ac.uk/works/108090091) ;
  [brevet USPTO 11349447, « Generating harmonics in an audio system »](https://image-ppubs.uspto.gov/dirsearch-public/print/downloadPdf/11349447).
- Mixage : sur un téléphone, ce qu'on entend du kick, c'est son clic (2 à 5 kHz) et les harmoniques d'une saturation
  douce ; plus de sub n'aide pas. [Sonarworks, petits haut-parleurs](https://www.sonarworks.com/blog/learn/make-your-mix-sound-great-on-small-speakers) ;
  [Icon Collective, kick](https://iconcollective.edu/kick-drum-mixing-tips) ;
  [Soundbridge, 808 sur petits haut-parleurs](https://www.soundbridge.io/make-your-808s-sound-better-on-small-speakers).
- Déjà au dépôt : `docs/hits/basses-qui-tapent.md` (même principe, l'effet « Basse audible » sur les basses).

## Ce qui est fait
- **Moteur** (`engine/src/main/cpp/core/SpeakerKick.h/.cpp`) : seulement quand l'appli sait que le son sort du
  haut-parleur du téléphone, la tranche du kick (pad 1 ; la tranche Batterie si ce pad n'a pas la sienne) reçoit,
  avant son volume : ses harmoniques (bande sous 150 Hz, tanh, gardées entre 180 et 1 500 Hz, au rythme de son
  enveloppe) et son clic (sa bande 1,2-4,5 kHz doublée). Les tranches des basses ne sont pas touchées (si le kick
  n'a pas de tranche à lui, toute la tranche Batterie est traitée, sa 808 comprise) ; muet, solo et volume du kick
  restent maîtres ; la chaîne du master garde le plafond. Aucune allocation, aucun verrou, aucun log.
- **Jamais dans une prise** « Enregistrer ce que je joue » (constat grave de la relecture) : tant qu'une prise de la
  sortie est armée, le traitement est coupé d'un coup ; il revient quand la prise est lâchée.
- **Commande 210** `SetSpeakerOutput` (1 : haut-parleur). **Appli** : `Headphones.route` (AudioManager, déjà suivi
  pour les conseils d'enregistrement) l'envoie au moteur en direct à chaque changement de sortie ; jamais à un export.
- **Essayé et écarté** (mesuré sur le banc en direct) : couper tout le mix sous 100 Hz au haut-parleur (−1 à −2 dB
  sur le kick au téléphone) ; les harmoniques sur le master au rythme des pas du kick (la basse excitée aussi, kick
  moins relevé : trap et jump up +2,6 dB seulement) ; le clic seul ne change presque rien aux chiffres d'énergie.

## Mesures (banc « écoute en direct », `ps_live`, 60 s, lu à partir de 4 s)
Morceaux de l'assistant enregistrés par `LiveSongRecorder` (comme `scripts/live-bench.sh`), joués par le moteur temps
réel ; « avant » = branche de départ, « après » = commande 210 à 1. Téléphone = passe-haut Butterworth d'ordre 4 à
300 Hz (téléphone moyen) ou 200 Hz (bon téléphone), puis pondération K. Énergie du kick = 100 ms après chaque coup
(coups lus sur le kick joué seul, tranche 16).

| Style | Kick seul au tél. 300 / 200 Hz | Mix : 100 ms après les coups (300 Hz) | Mix : coup / 100 ms avant | LUFS mix | LUFS au tél. 300 Hz | Crête |
|---|---|---|---|---|---|---|
| jump up | +5,8 / +6,5 dB | −14,5 → −9,8 dB (+4,7) | +9,2 dB | −13,0 → −13,2 | −17,1 → −17,3 | −1,18 dBFS |
| house | +7,1 / +6,9 dB | −15,4 → −9,6 dB (+5,7) | +5,6 dB | −10,8 → −10,8 | −17,9 → −15,3 | −1,19 dBFS |
| trap | +7,0 / +7,2 dB | −13,8 → −11,0 dB (+2,8) | +2,9 dB | −10,2 → −10,3 | −13,9 → −13,7 | −1,19 dBFS |
| techno | +8,7 / +8,9 dB | −18,4 → −9,9 dB (+8,5) | +8,4 dB | −12,8 → −11,2 | −24,1 → −17,1 | −1,19 dBFS |

- **Casque, Bluetooth, export** : rendu sans la commande (ou à 0) **identique au bit près** à la branche de départ
  sur les 4 styles (fichiers comparés octet par octet) : 0,0 dB d'écart, sous la tolérance de 0,5 dB.
- Crête avant : −1,18 à −1,20 dBFS ; après : −1,18 à −1,19 dBFS (le limiteur du master tient).
- Écoute en direct avec la commande : 0 alerte (NAN, GAIN, SILENCE, FADE, CLICK), 0 allocation en 30 000 rappels
  par style.
- Trap reste le plus faible au mix (+2,8 dB) : sa 808 occupe les mêmes fenêtres ; le kick lui-même gagne +7 dB.
- Niveau du mix au haut-parleur : inchangé à 0,2 LU près, sauf la techno, plus forte de 1,6 LU (son kick nu laissait
  de la marge au limiteur).

## À dire au relecteur
- Mesures faites avec des outils de travail hors dépôt (lecture WAV + modèle téléphone en Python, rendu « avant »
  dans un arbre de travail de la branche de départ) ; rien du banc (`scripts/bench/`) n'est changé.
- Le test moteur `sk_speaker_lifts_the_kick_on_a_phone` donne +6,1 dB (300 Hz) sur un motif synthétique.
- Non mesuré ici : le vrai haut-parleur du HONOR (essai téléphone ci-dessous).
- Laissé (moyen de la relecture) : la route est lue dès le démarrage de l'appli (`Headphones.watch`), la commande crée
  donc l'hôte audio un peu plus tôt qu'avant (il l'était de toute façon au premier écran) ; coût non mesuré.

## Essai téléphone à prévoir
Haut-parleur du téléphone, volume habituel : un morceau house, trap et techno de l'assistant, ▶ ; le kick doit taper
nettement plus qu'avant. Puis les mêmes en Bluetooth ou au casque : aucun changement.

tests: com.pocketstudio.app.audio.SpeakerKickRouteTest

chemin: Tout écran qui joue, son sur le haut-parleur du téléphone | test: SpeakerKickRouteTest#theRouteReachesTheLiveEngine
chemin: Tout écran qui joue, au casque ou en Bluetooth | test: SpeakerKickRouteTest#onlyThePhoneSpeakerGetsTheSpeakerKick
