# Des basses qui tapent (recherche, 2026-10-24)

Demande de Benjamin : « l'appli ne fait jamais une basse qui tape » comme dans quatre vidéos YouTube. Il écoute
sur son téléphone (Honor Magic 8 Pro). Base lue, sans rien modifier : `/home/user/ps-p22` (phase 34 :
`StarterWizard.kt`, `StyleMusic.kt`, `StyleRecipes.kt`, `scripts/packs/recipes/*.json`, `WideMix.kt`,
`WizardMix.kt`, `StudioDrums.kt`, `SoundLibrary.kt`, `SynthPatch.kt`, `MelodyBuilder.kt`, `Effects.kt`,
`engine/src/main/cpp/core/StudioVoices.h`, `Synth.h/.cpp`, `MasterChain.h`), `docs/son/banc/apres-S5b.md`,
`docs/hits/*.md`.

Méthode : les pages des vidéos (titres, descriptions, chapitres, lus par l'API publique de YouTube), des articles
de sound design, et des **mesures faites ici** sur les sons de notre moteur, rendus hors ligne par le cœur
(`libps_core.a` de la phase 34). L'audio des vidéos n'a pas été téléchargé. Leurs transcriptions ne sont pas
lisibles d'ici : YouTube demande une connexion (« Sign in to confirm you're not a bot »). Les grilles du § 4
sont donc **reconstruites** à partir des titres des chapitres et de la pratique courante, pas recopiées.

---------------------------------------------------------------------------------------------------------------
## 0. En bref (à lire à Benjamin)

- **La basse est là, mais ton téléphone ne l'entend pas.** Un haut-parleur de téléphone ne joue presque rien sous
  200 à 300 Hz. Or nos basses mettent tout sous 120 Hz. Mesuré : à travers un « filtre téléphone », notre 808
  perd **22 dB** et la « Basse sub » **29 dB**. Les 808 des packs (des sinus purs générés par IA) perdent
  **32 à 45 dB** : sur le téléphone, ça fait presque du silence.
- **Ce qui « tape » sur un téléphone, ce sont les harmoniques** que crée la saturation, entre 150 et 800 Hz. Le
  cerveau reconstruit la note grave qui manque (la « fondamentale absente »). Les vidéos « bass boosted » sont
  saturées à fond : c'est pour ça qu'elles cognent même sur un téléphone.
- **Le remède n° 1 est simple** : un « exciteur de graves » (le principe du MaxxBass de Waves). Il fabrique ces
  harmoniques à partir du grave. Simulé sur nos sons : **+8 à +27 dB entendus au téléphone**, pour **+0,5 dB**
  seulement sur une grosse sono.
- Ensuite : une vraie attaque (chute de hauteur plus longue, clic, phase remise à zéro), une saturation plus
  franche sur la 808, plus de sons sous 30 Hz (inaudibles, mais ils mangent la marge), et une bibliothèque de
  lignes de basse roulantes.

**Les 5 changements prioritaires** (détails au § 7) :
1. **Exciteur de graves** (nouvel effet du moteur) sur toutes les basses de l'assistant, la 808 et les basses des
   packs comprises.
2. **La 808 qui tape** : la voix 808 du moteur par défaut à la place des 808 de packs (des sinus purs), une
   saturation 0,6 à 0,9, une chute de hauteur de 25 à 40 ms au lieu de 6, et des harmoniques hautes à la place du
   réglage « tone » (octave et douzième, encore inaudibles au téléphone).
3. **Synthé** : enveloppe de hauteur, phase remise à zéro à chaque note, vélocité qui ouvre le filtre, sub sinus
   (et jamais sous 30 Hz).
4. **Rien sous 28 Hz** : coupe-bas sur les basses et la 808 ; corriger les styles en octave 1 dont le sub carré
   tombe à 23-27 Hz (hard techno, acid techno, industrielle, detroit).
5. **Banc « basse au téléphone »** et **bibliothèque de basses roulantes** (10 grilles du § 4).

---------------------------------------------------------------------------------------------------------------
## 1. Les quatre vidéos : ce qu'on en sait

| # | Vidéo | Ce qu'elle montre (page, chapitres) |
|---|---|---|
| 1 | Matt Tinkler, « The Main Bass Sounds of Electronic Music (Explained!) » (13 k vues) | Chapitres : sons de soutien et sons de premier plan (0:25), **Sub** (2:07), **basse tenue** (2:55), **808** (4:03), **basse pluck** (5:33), **reese** (6:58), **wobble** (8:33), **wub / growl** (9:28), **basse-lead** (10:26), **acide** (11:59). Dans Ableton, avec les effets Sting 2 et Slippery Slope (iftah). |
| 2 | Echo Sound Works, « 10 Rolling Basslines Every Producer Should Know (Techno, House, Trance) » (81 k vues) | Chapitres : histoire de la basse roulante, groove de batterie, **doubles-croches**, **sans le temps fort**, **avec le temps fort**, **roulante trance**, **arpège roulant**, **3-2-2-3**, **deux notes**, **vélocité et modulation**, **longueurs variées**, **triolets**, **« Power Chord era »**. L'auteur note que « la basse est mixée un peu plus fort que d'habitude pour qu'on l'entende ». |
| 3 | Yogesh Shejulkar, « SUBWOOFER BASS TEST \| EXTREME HIGH BASS TEST » (17 M vues) | Remixes « bass boosted » : « insane drops, heavy remixes, nonstop bass energy that'll test your speakers ». Mixé et masterisé par l'auteur. |
| 4 | 5.1 Test & Clips, « DOLBY 5.1 SURROUND \| SUBWOOFER BASS TEST » (6,8 M vues) | « Test tones from **90 Hz down to 9 Hz** », couches infrasonores, « punchy mid-bass hits », canal LFE 5.1. Prévu pour un caisson de basses. |

Ce qu'il faut en retenir pour un téléphone :
- Les vidéos 3 et 4 sont faites pour un **caisson**. Leurs sons de 9 à 30 Hz ne passent par aucun téléphone ;
  ce qui passe, ce sont les **coups de médium-grave** (80-250 Hz) et surtout la **saturation** des remixes
  « bass boosted ». On y pousse le grave de 6 à 12 dB, puis on l'écrête : ça crée des harmoniques impaires de 150
  à 1 000 Hz. Ajoutons le **contraste** (un silence, puis le drop sur le premier temps) et les **descentes de
  hauteur** (sub drops, glissés de 808), qu'on entend par leurs harmoniques qui glissent.
- La vidéo 1 donne la **liste des familles** (§ 3). La vidéo 2 donne les **rythmes** (§ 4).

---------------------------------------------------------------------------------------------------------------
## 2. Pourquoi une basse « tape » : les six ingrédients

1. **Une attaque** : les 5 à 40 premières millisecondes plus fortes et plus brillantes que la suite. Pour ça : une
   chute de hauteur (808 : +12 à +24 demi-tons qui retombent en 20 à 60 ms ; kick-basse : 10 à 15 ms), un clic
   (bruit filtré de 2 à 5 ms), une enveloppe de filtre rapide (decay 20 à 80 ms). Aussi une **phase de départ fixe** :
   une sinusoïde qui part de zéro à chaque note, pour que chaque coup attaque pareil.
2. **Des harmoniques de 100 à 800 Hz** qui suivent la note. Ce sont elles qu'on entend sur un téléphone, un
   ordinateur portable ou une enceinte Bluetooth. Elles viennent d'une saturation (tanh, écrêtage, un peu
   asymétrique pour avoir aussi les harmoniques paires), d'une couche « médium » au-dessus de 250 Hz, ou d'un
   exciteur de graves.
3. **Un sub propre et mono de 40 à 60 Hz** (Mi1 41 Hz à Si1 62 Hz), une seule note à la fois. Les notes ne se
   chevauchent pas, sinon les phases s'annulent. Pas d'énergie sous 30 Hz : on ne l'entend pas, mais le limiteur la
   voit et retire de la sonie à tout le morceau.
4. **De la place laissée au kick** : soit la ligne évite le temps fort (« K-B-B-B » : kick, puis trois doubles-croches
   de basse), soit la basse baisse de 4 à 10 dB pendant 50 à 150 ms sous chaque kick (sidechain). La note de basse
   s'arrête avant le kick suivant.
5. **Du contraste** : une coupure (un temps ou une mesure sans grave), puis la basse sur le premier temps du drop ;
   des notes de longueurs différentes ; des vélocités qui respirent (40 / 70 / 100 / 70) et qui ouvrent le filtre.
6. **Un niveau juste** : sub à −3 à −6 dB de l'énergie totale du Drop, grave (60-250 Hz) à −3 à −6 dB. Ces
   valeurs sont déjà atteintes au banc (sub −4,8, grave −3,8 en médiane) : **ce n'est pas la quantité de grave qui
   manque, c'est sa forme** (points 1, 2 et 4).

### Ce que le téléphone entend
- Un haut-parleur de téléphone est une membrane de 1 à 2 cm. Il descend jusqu'à 200-300 Hz sur un bon téléphone
  (amplificateur intelligent et égaliseur), 400-600 Hz sur un petit ; en dessous, la pente est de 12 à 24 dB par
  octave. Ce sont des **ordres de grandeur** : le Magic 8 Pro est à mesurer (§ 7, P10).
- **Fondamentale absente** : quand le haut-parleur joue 220, 330, 440 Hz… (harmoniques 2 à 6 d'une note), le cerveau
  entend la note de 110 Hz qui n'est pas là. Il faut au moins trois harmoniques voisines dans la bande que le
  haut-parleur joue, et qu'elles suivent l'enveloppe de la note (le « coup »).
- **MaxxBass (Waves)** fait exactement ça : un filtre de séparation, des harmoniques générées à partir de la bande
  basse (par une non-linéarité), la bande haute intacte, un mélange réglable, et des harmoniques qui « reprennent la
  dynamique et la sonie de la basse d'origine ». Waves annonce un grave perçu « jusqu'à 1,5 octave » plus bas que ce
  que joue le haut-parleur ; d'autres sources parlent de 2 octaves. Ce sont des chiffres de fabricant. Les brevets
  d'origine seraient expirés (2006-2008, d'après un forum : à vérifier). La saturation multibande (Saturn 2, Trash)
  fait la même chose à la main.
- Pour une 808 en Fa1 (43,7 Hz), les harmoniques qui tombent dans 200-600 Hz sont les **5e à 14e** (218, 262,
  306 Hz…). Une saturation douce les crée faiblement ; il faut une saturation franche **ou** un exciteur.

---------------------------------------------------------------------------------------------------------------
## 3. Ce que nos sons font aujourd'hui (mesuré)

Rendu hors ligne par le cœur du moteur, phase 34 (programmes dans
`/tmp/claude-0/-home-user-pocket-studio/2ed29bda-5e9a-5850-a6a7-bbf3293c7a09/scratchpad/bassbench/`, hors dépôt).
Les synthés jouent des noires à 128 BPM sur Fa2 (87 Hz ; leur sub joue Fa1), avec les valeurs de `SoundLibrary` et
les enveloppes `WideMix` des sons de bass music. La 808 joue une blanche sur Fa1 avec les réglages
`StudioDrums.voice808`. Aucun effet de mixage. Le **« filtre téléphone »** est un passe-haut de Butterworth d'ordre
4 à 200, 300 ou 500 Hz ; la sonie est pondérée K (BS.1770). « tél. 300 » est la perte de sonie à travers ce
filtre : plus elle est proche de 0, mieux le son passe sur un téléphone. Les bandes sont en dB rapportés à
l'énergie totale du son. L'« attaque » compare le niveau RMS sur 5 ms le plus fort des 40 premières ms au niveau
médian entre 80 et 250 ms.

| Son | tél. 200 | tél. 300 | tél. 500 | < 60 | 60-120 | 120-250 | 250-500 | 0,5-2 k | attaque |
|---|---|---|---|---|---|---|---|---|---|
| 808 trap (voix du moteur) | −15,2 | **−22,0** | −31,9 | −0,9 | −12,9 | −8,8 | −25,4 | −41,6 | +1,9 |
| 808 phonk | −13,0 | −18,3 | −26,7 | −1,2 | −11,6 | −7,9 | −21,1 | −34,4 | +2,5 |
| 808 drill | −15,0 | −21,9 | −32,0 | −1,0 | −11,8 | −8,4 | −25,2 | −41,4 | +2,1 |
| **808 des packs** (trap, 10 sons) | −22 à −35 | **−32 à −45** | – | 0 à −32 | – | – | – | 250-2 k : −40 à −56 | +0,7 à +3,8 |
| Basse sub | −21,9 | **−29,0** | −36,3 | −2,7 | −3,8 | −14,3 | −31,3 | −40,7 | +5,7 |
| Sub pur | −26,2 | −33,1 | −37,9 | (sinus) | | | | | +0,6 |
| Basse ronde | −8,8 | −15,0 | −24,1 | −2,4 | −6,7 | −7,4 | −15,1 | −29,0 | +5,9 |
| Basse qui parle | −10,6 | −17,0 | −29,2 | −3,8 | −3,8 | −8,5 | −15,9 | −36,9 | +5,4 |
| Basse wobble | −9,4 | −13,6 | −22,0 | −3,4 | −4,1 | −10,0 | −13,2 | −28,9 | +4,2 |
| Basse reese | −6,4 | −10,0 | −17,3 | −5,6 | −3,7 | −7,6 | −9,4 | −20,9 | +5,9 |
| Reese DnB (WideMix) | −5,6 | −8,2 | −14,3 | −2,6 | −7,2 | −8,4 | −10,0 | −19,2 | +3,3 |
| Basse riddim (WideMix) | −7,3 | −9,2 | −14,7 | −1,2 | −9,2 | −12,9 | −12,6 | −22,0 | +3,2 |
| Basse neuro (WideMix) | −1,8 | −2,8 | −6,6 | −5,1 | −7,6 | −10,8 | −4,8 | −9,7 | +2,2 |
| Basse growl | −4,9 | −7,9 | −12,9 | −26,5 | −2,3 | −7,2 | −7,4 | −14,7 | +2,2 |
| Basses du pack techno (8 sons) | −11 à −19 | −12 à −21 | – | – | 60-250 : 0 à −4 | – | 250-2 k : −15 à −27 | – | 0 à +3 |

Recettes « de manuel » synthétisées pour comparaison (`refs.py`, même mesure) :

| Référence | tél. 200 | tél. 300 | attaque |
|---|---|---|---|
| 808 sans saturation (sinus, chute de +12 demi-tons) | −26,7 | −27,2 | +8,0 |
| 808 saturée (tanh ×5 asymétrique, chute de +24 demi-tons en 25 ms, clic de 2 ms) | −12,6 | −17,1 | +4,0 |
| la même + exciteur 0,6 | −11,0 | −14,6 | +4,3 |
| Basse roulante K-B-B-B (scie + sinus, passe-bas 2,4 kHz → 180 Hz en 40 ms) | −8,4 | −11,3 | – |
| Reese sale (deux scies ±27 cents, passe-bas 700 Hz, tanh ×3, sinus dessous) | −7,5 | −10,1 | – |

Exciteur de graves simulé (bande < 120 Hz → redressement + tanh, harmoniques paires et impaires → passe-bande
150-1 000 Hz, niveau calé sur la bande basse, ajouté au son) :

| Son | tél. 300 sans | exciteur 0,25 | 0,5 | 1,0 | sonie pleine bande à 0,5 |
|---|---|---|---|---|---|
| 808 trap (moteur) | −22,0 | −18,5 | **−14,4** | −9,0 | +0,5 LU |
| 808 de pack n° 1 | −41,4 | −20,2 | **−14,2** | −8,2 | +0,7 LU |
| Basse sub | −29,0 | −17,8 | **−12,0** | −6,1 | +1,6 LU |
| Basse ronde | −15,0 | −13,8 | −11,8 | −7,9 | +0,6 LU |

Constats :
1. **Ce qui se joue en trap, drill, phonk, reggaeton et jersey club est inaudible au téléphone.** Les 808 de
   packs sont des sinus presque purs (250-2 000 Hz à −40 à −56 dB du son) ; or l'assistant les choisit en premier
   dès que le pack du style est installé (`soundsFor` : « the style's own pack sound first »), et la voix 808 du
   moteur ne joue que si le son de basse est celui de la recette (`StudioDrums.bassOn808`). Le banc ne le voit
   pas : il ne rend que les sons intégrés.
2. **La voix 808 du moteur** : sa saturation (0,3) est faible et son réglage « tone » ajoute l'octave et la
   douzième (87 et 131 Hz pour Fa1). **Ces deux harmoniques restent sous 200 Hz : au téléphone, « tone » à 1 ne
   gagne rien** (−22,1 → −23,6 dB à 300 Hz). Seul le drive aide : 0,8 → −15,2 dB. Sa chute de hauteur part de
   2,5 fois la note avec une constante de temps de 6 ms (le commentaire du code dit 12 ms) : c'est un clic, pas le
   « boum » de 30 à 60 ms d'une 808 de trap. Résultat : une attaque à +1,9 dB, la plus molle du tableau.
3. **« Basse sub » et « Sub pur » sont faites pour un caisson.** En octave 1 (hard techno, acid techno,
   industrielle ; « Basse ronde » en detroit), le sub carré du synthé joue une octave sous la note : **23 à 31 Hz**.
   Mesuré en Fa#1 : **la moitié de l'énergie de « Basse sub » est sous 30 Hz** (−3,2 dB), et cette partie fait
   presque toute sa crête (−5,4 dBFS sur −4,5). Le limiteur réagit à un son que personne n'entend. La chaîne master
   n'a qu'un bloqueur de continu à 3 Hz, pas de coupe-bas vers 25-30 Hz.
4. **Les sons de bass music passent bien** (reese, growl, riddim, neuro : −3 à −10 dB) grâce à leur couche
   médium (`MID_LAYER`) et à leur filtre ouvert. Ce sont les basses « simples » (sub, ronde, 808) qui disparaissent,
   et ce sont justement celles du hip-hop, de la house, de la techno et de la pop.
5. **Toutes les attaques sont faibles** (+2 à +6 dB). Il n'y a ni enveloppe de hauteur ni clic dans le synthé, et
   la phase des oscillateurs n'est pas remise à zéro à chaque note (seulement avec l'unisson). Chaque note démarre
   donc à une phase différente : un coup sur deux attaque moins fort, ou fait un petit clic.

---------------------------------------------------------------------------------------------------------------
## 4. Les basses roulantes : grilles de 16 pas

Notation des recettes (`StepLine`) : `R` fondamentale, `O` octave, `5` quinte, `3` tierce, `7` septième ;
minuscule = plus doux (×0,7) ; `-` prolonge ; `.` silence ; `/` glisse vers la note suivante. Le kick est en 4/4
(`X...X...X...X...`) sauf mention contraire. « Vél. » donne les vélocités par double-croche d'un temps (le kick
tombe sur la 1re). « Durée » est la longueur jouée d'une double-croche (le gate) : les notes **ne se touchent pas**
et finissent avant le kick suivant.

| # | Nom (chapitre de la vidéo 2) | Grille basse (16 pas) | Vél. | Durée | Son | Styles |
|---|---|---|---|---|---|---|
| 1 | **Sans le temps fort** (K-B-B-B, techno, psytrance) | `.rRr.rRr.rRr.rRr` | 0 / 70 / 100 / 75 | 60-75 % | scie filtrée + sub sinus, filtre 2 kHz → 180 Hz en 30-50 ms | techno, retro_techno, hard_techno, industrial_techno, trance (Drop) |
| 2 | **Avec le temps fort** (doubles-croches pleines, sidechain obligatoire) | `rrRrrrRrrrRrrrRr` | 45 / 70 / 100 / 70 | 50-60 % | idem, sidechain de 8 à 10 dB sur 80 ms | tech_house, hard_techno, gabber (sous le kick dur) |
| 3 | **Doubles-croches** (une seule note, toutes égales) | `RRRRRRRRRRRRRRRR` | 100 partout | 50 % | pluck court, sidechain | minimal, acid (sous la 303) |
| 4 | **Trance roulante** (suit les accords, approche en fin de mesure) | `.rRr.rRr.rRr.rR5` (mesures 1-3) + `.rRr.rRr.rRO.5R3` (mesure 4) | 0 / 65 / 100 / 75 | 70 % | scie + carré −12, filtre ouvert sur 8 mesures | trance, acid_techno |
| 5 | **Arpège roulant** (les notes de l'accord) | `.R5O.R5O.R5O.R53` | 0 / 80 / 100 / 80 | 60 % | pluck | trance, synthwave, retro_techno |
| 6 | **3-2-2-3** (groupes de 3 et 2 doubles-croches) | `R--R-R-R--R--R--` (3+2+2+3+3+3) | accent au début de chaque groupe | 80 % du groupe | basse ronde, glide léger | house, tech_house, afro_house, deep_house |
| 7 | **Deux notes** (fondamentale / octave en alternance) | `.rOr.rOr.rOr.rOr` ou `R.O.R.O.R.O.R.O.` (rebond d'octave) | 0 / 70 / 100 / 70 | 50 % | basse ronde ou M1 | house, disco, synthwave, speed_garage |
| 8 | **Vélocité et modulation** (la grille 1, qui bouge) | `.rRr.rRr.rRr.rRr` | cycle 40 / 70 / 100 / 70, **vélocité → coupure** 0,5, filtre lent sur 8 mesures | 60 % | idem 1 | techno, tech_house, minimal |
| 9 | **Longueurs variées** | `.R-r.Rr-.R--.rR-` | 0 / 100 / – / 70 … | variable | basse ronde, filtre qui se ferme sur les notes longues | tech_house, deep_house, minimal |
| 10 | **Triolets** (3 croches de triolet par temps, la 1re vide) | par temps : `. T T` aux ticks 0 / 32 / 64 (24 ticks par double-croche) | 0 / 80 / 100 | 60 % | pluck, sub | afro_house, amapiano, techno (pont) |
| 11 | **« Power chord »** (fondamentale + quinte + octave ensemble) | `.rRr.rRr…` avec chaque note doublée à la quinte et à l'octave dans la couche médium | idem 1 | 60 % | médium saturé + sub seul en fondamentale | hard_techno, gabber, industrial_techno, hard trance |

Remarques :
- Le n° 10 tient dans les notes (`NoteClip`, 24 ticks par double-croche : un triolet de double-croches = 16 ticks)
  mais pas dans la notation des recettes : il faut un jeton de triolet (`StepLine`, § 6).
- Le n° 11 demande un second oscillateur réglable en demi-tons (aujourd'hui `OSC2_OCTAVE` ne fait que −1, 0 ou +1
  octave) ou une piste « médium » qui joue la quinte.
- **Nos recettes** : techno `.rR-.rRr…` est déjà la grille 1 (bien). Trance `..R-..R-` est la basse de contretemps
  du Groove ; au Drop, prendre la grille 4. Hard techno `.rR-.rRr` est la grille 1, mais en **octave 1 avec
  « Basse sub »** : sub à 23 Hz (constat 3). Tech house `..Rr..Rr..` est juste ; ajouter la grille 8 (vélocité →
  coupure).

Autres grilles de référence :
- **Basse de contretemps (house, trance des années 90)** : `..R...R...R...R.` (une croche sur chaque « et »), durée
  1 à 2 pas ; variante « double contretemps » `..RR..RR..RR..RR`.
- **Donk / hard house** : `..O...O...O...O.` (octave sur le contretemps, son à chute de hauteur), réponse `..R...O...R...5.`.
- **Two-step garage** : `R--....R..R--.O-` (notre recette uk_garage, bien).
- **808 trap** : `R-----------/O--R-----..R---/5--` (2 mesures, glissé d'octave puis de quinte ; notre recette, bien).
- **DnB reese** : `R-------..R-----R-------..R--/O-` (notes longues, notre recette, bien).
- **Dubstep** : demi-temps, notes longues dont la durée fixe le nombre de « wobs » (notre recette, bien).

---------------------------------------------------------------------------------------------------------------
## 5. Les familles de basse, une par une

Valeurs de nos paramètres (0..1) : coupure = 20 × 900^v Hz (0,30 → 154 Hz ; 0,34 → 200 ; 0,40 → 300 ; 0,50 → 600 ;
0,58 → 1 000 ; 0,68 → 2 000) ; temps d'enveloppe = 5000^v ms (0,19 → 5 ms ; 0,27 → 10 ; 0,40 → 30 ; 0,43 → 40 ;
0,48 → 60 ; 0,54 → 100 ; 0,62 → 200) ; `FILTER_ENV` = v × 5 octaves ; `DRIVE` : gain 1 + 15 v² ; `GLIDE` =
0,5 v² s (0,40 → 80 ms ; 0,55 → 150 ms) ; `OSC2_DETUNE` = v × 30 cents ; `UNISON_DETUNE` = 3 + 47 v cents. Les
paramètres en **gras** n'existent pas encore (§ 6).

### 5.1 Sub sinus (Sub Bass)
- **Ce qui le fait taper** : rien de spectaculaire seul. Il porte le poids sous un kick ou une couche médium.
  Sinus pur, mono, 40-60 Hz, une note à la fois, attaque de 2-5 ms, release de 20-50 ms, sidechain sous le kick.
- **Au téléphone** : invisible (−29 à −33 dB). Il lui faut toujours une couche qui parle au-dessus (médium, 808
  saturée), ou un exciteur.
- **Chez nous** : « Sub pur » = triangle filtré à 126 Hz (presque un sinus). Bien pour la piste `Sub`. Manquent : une
  **forme sinus** pour le sub du synthé (`SUB` est un carré une octave dessous), le **sub jamais sous 30 Hz**, la
  **phase à zéro** à chaque note.
- **Recette** : `OSC1_WAVE` 1 (triangle) ou **sinus**, `OSC_MIX` 0, `SUB` 0, `CUTOFF` 0,27, `RESONANCE` 0,
  `A_ATTACK` 0,1 (2 ms), `A_SUSTAIN` 1, `A_RELEASE` 0,3 (13 ms), `DRIVE` 0,25 (sa 3e harmonique, à
  130-180 Hz), `MONO_BELOW` par défaut ; coupe-bas à 28 Hz sur la tranche.
- **Styles** : la piste Sub de dnb, liquid, neuro, jungle, jump up, dubstep, riddim, tech house, garage.

### 5.2 Basse tenue (Sustained)
- **Ce qui la fait taper** : un corps de 80 à 250 Hz qui tient. Scie ou carré filtré vers 300-600 Hz, un peu de
  résonance, saturation douce, sidechain (pop, synthwave, deep house).
- **Chez nous** : « Basse ronde » (−15 dB à 300 Hz) ; « Basse analogique » (Plaits, non mesurée ici).
- **Recette** : `OSC1_WAVE` 0 (scie), `OSC2_WAVE` 0,5 (carré) `OSC2_OCTAVE` 0,1 (−1 octave), `OSC_MIX` 0,35,
  **sub sinus** 0,5, `CUTOFF` 0,45 (≈ 430 Hz), `RESONANCE` 0,25, `FILTER_ENV` 0,3, `F_DECAY` 0,54 (100 ms),
  `F_SUSTAIN` 0,4, `A_RELEASE` 0,3, `DRIVE` 0,35, **exciteur** 0,3.
- **Styles** : pop, synthwave, deep_house, lofi, afrobeats.

### 5.3 808 (le « boum » qui glisse)
- **Ce qui la fait taper** : un sinus qui **tombe** de +12 à +24 demi-tons sur sa note en 20 à 60 ms (le « boum »),
  un clic de 2 à 5 ms, une note tenue de 0,8 à 2 s accordée à la tonalité (40-60 Hz), des **glissés** de 100 à
  200 ms vers l'octave ou la quinte, et surtout la **saturation** (tanh, écrêtage, parfois deux étages) qui
  l'« équerre » et la fait sortir d'un téléphone. Mono. Kick court par-dessus, ou rien (la 808 est le kick).
  Sidechain léger ou découpage pour que kick et 808 ne se chevauchent pas.
- **Chez nous** : la voix `Bass808` (StudioVoices.h) a tout ça en petit. Chute de 2,5× avec une constante de 6 ms
  (trop courte), drive de 0,3 à 0,5 (trop doux pour le téléphone), « tone » = octave + douzième (inaudibles au
  téléphone). Glide 110-180 ms (bien), tenue 60 % puis chute (bien), mono (bien). **Et elle ne joue pas quand le
  pack du style est installé** : c'est alors un sinus de pack, encore plus pur.
- **Recette dans le moteur** (nouveaux réglages de la voix, voir P2) : chute de hauteur **+18 à +24 demi-tons** en
  **25 ms** (trap), 35 ms (drill), 20 ms (jersey club) ; clic 0,5 ; drive **0,7** (trap, drill), **0,9** (phonk,
  avec un 2e étage dur), 0,5 (reggaeton) ; **harmoniques** (exciteur interne) 0,5 ; « tone » recalculé en
  harmoniques 4 à 8 (175-350 Hz pour Fa1) ; tenue selon le style (déjà là). Sur sa tranche : coupe-bas 28 Hz,
  écrêteur doux (déjà là), exciteur si le son est un échantillon.
- **Styles** : trap, drill, phonk, jersey_club, reggaeton ; en option grime, afrobeats.

### 5.4 Pluck (basse pincée)
- **Ce qui la fait taper** : une attaque nette et une queue courte (150-300 ms) ; enveloppe de filtre rapide
  (2-3 octaves qui retombent en 30-80 ms), sans sustain ; un peu de chute de hauteur (2-5 demi-tons en 10 ms)
  donne le « tok ».
- **Chez nous** : le preset « Pluck » est un lead ; « Basse pincée » (Plaits) a un gain de calibrage de +26 dB, signe
  d'un son très faible. Il manque une **basse pluck** au synthé.
- **Recette** : scie, `CUTOFF` 0,33, `RESONANCE` 0,35, `FILTER_ENV` 0,55, `F_DECAY` 0,45 (45 ms), `F_SUSTAIN` 0,
  `A_DECAY` 0,6 (170 ms), `A_SUSTAIN` 0, `A_RELEASE` 0,35, **chute de hauteur 3 demi-tons / 10 ms**, sub sinus 0,4,
  `DRIVE` 0,3.
- **Styles** : minimal, deep_house, tech_house, afro_house, boom_bap.

### 5.5 Reese
- **Ce qui la fait taper** : deux scies désaccordées (±0,27 demi-ton, Saunderson 1988) qui battent, un passe-bas
  vers 600-1 000 Hz, saturation, chorus ou flanger, **sinus séparé dessous** ; la largeur au-dessus de 250 Hz
  seulement.
- **Chez nous** : « Reese DnB » (WideMix) passe bien (−8 dB à 300 Hz). Sa saturation est sur tout le son, sub
  compris ; la bass music a déjà sa piste `Sub`. Manque : une **saturation au-dessus d'une fréquence** (aujourd'hui
  `MID_LAYER` = passe-haut fixe à 250 Hz, drive fixe à 3).
- **Recette** : comme aujourd'hui, avec `SUB` 0 quand la piste `Sub` joue (sinon deux subs se battent), `DRIVE`
  0,35, **fréquence de la couche médium** 200 Hz.
- **Styles** : dnb, liquid_dnb (plus doux), jungle.

### 5.6 Wobble
- **Ce qui le fait taper** : un filtre (souvent avec distorsion après) balayé par un LFO calé sur le tempo (1/4, 1/8,
  1/16, triolets), la vitesse qui change de note en note, un sub sinus mono dessous ; le creux de 100-300 Hz est
  comblé par la distorsion.
- **Chez nous** : « Basse wobble » + `wobbleLane` (vitesse changée toutes les 8 mesures) + `LFO2_SHAPE` (S4). Bien.
  Manque : une distorsion **après** le filtre qui grossit quand il s'ouvre (`DRIVE` le fait sur tout le son), la
  vitesse **par note** (la longueur de note qui choisit la division).
- **Styles** : dubstep, riddim (forme carrée, déjà là).

### 5.7 Wub / growl / FM
- **Ce qui le fait taper** : une source riche (FM, wavetable) saturée, un filtre formant (voyelles), du mouvement
  rapide, souvent rééchantillonné ; une chute de hauteur à l'attaque (« wub » du garage : −5 demi-tons en 40 ms) ;
  le sub séparé.
- **Chez nous** : « Basse growl », « Basse neuro » (formant, S4) passent très bien au téléphone (−3 à −8 dB). Manque
  la **FM** dans le synthé (Plaits model 10 « Basse FM » existe) et l'**enveloppe de hauteur** pour le « wub ».
- **Styles** : neurofunk, jump_up, riddim (2e basse), uk_garage, speed_garage (le « wub »).

### 5.8 Basse-lead
- **Ce qui la fait taper** : une basse jouée dans l'octave 2-3 comme une mélodie, saturée, avec glide, souvent en
  octaves (grime : carré, eskibeat ; G-house).
- **Chez nous** : grime prend « Lead carré » en octave 2, **sans sub** : il manque le poids. Mettre `subBass`.
- **Styles** : grime, jump_up (réponse), speed_garage.

### 5.9 Acide (303)
- **Ce qui la fait taper** : résonance haute, accents (qui ouvrent le filtre et montent le volume ×2,2), slides,
  saturation après le filtre. C'est une basse-lead : sous elle, un sub ou une basse simple.
- **Chez nous** : Open303 + `AcidSound` (« Hurleur », « Carré saturé »). Bien. En acid techno, la basse dessous est
  « Basse sub » octave 1 : sub à 27 Hz (constat 3). La passer en octave 2 sans `SUB`, ou en « Sub pur ».
- **Styles** : acid_house, acid_techno, acidcore.

### 5.10 Donk (hard house, bounce)
- **Ce qui le fait taper** : un son court et accordé à chute de hauteur (« boing » : +12 à +24 demi-tons en
  20-40 ms), decay de 150-250 ms, sur les contretemps, en octaves. C'est la même famille que le log drum de
  l'amapiano.
- **Chez nous** : « Log drum » (Plaits, modèle de grosse caisse joué sur les notes) est le plus proche. Au synthé, il
  manque l'**enveloppe de hauteur**.
- **Recette** : triangle ou sinus, **chute +18 demi-tons / 30 ms**, `A_DECAY` 0,62 (200 ms), `A_SUSTAIN` 0, `DRIVE`
  0,5, exciteur 0,4, grille `..O...O...O...O.`.
- **Styles** : amapiano (le log drum), afro_house (option) ; pas de style hard house chez nous.

### 5.11 Basse de contretemps (offbeat)
- **Ce qui la fait taper** : elle tombe **entre** les kicks (`..R.`), courte (1-2 pas), ronde, et finit avant le
  kick suivant ; le kick et elle forment un seul « boum-ta ».
- **Chez nous** : trance `..R-..R-` (bien en Groove), house en octaves (bien). Ajouter la règle « la note finit au
  moins une double-croche avant le kick suivant » (aujourd'hui `..R-` va jusqu'au pas 3 et le kick tombe au pas 4 :
  bien ; `..R---` déborderait).

### 5.12 Basse roulante (rolling) : techno, tech house, trance, psytrance
- **Ce qui la fait taper** : le K-B-B-B (§ 4) et une note très courte (gate de 60-75 %, jamais de chevauchement), une
  enveloppe de filtre vive (passe-bas de 24 dB, 2-3 octaves qui retombent en 30-60 ms), des **vélocités qui
  ouvrent le filtre** (la 1re double-croche après le kick plus douce : 70-80 sur 100), un sub sinus séparé, kick et
  basse **en phase** (le kick fini dans les deux premières doubles-croches). En psytrance (138-145 BPM) : attaque
  0, pas de decay, sustain plein, release très court, filtre bas ouvert par une enveloppe très courte, mono.
- **Chez nous** : la grille est là (techno). Manquent : vélocité → coupure, phase à zéro, et un préréglage « Basse
  roulante ». « Basse analogique » (Plaits) pour la techno : à mesurer.
- **Recette « Basse roulante »** : `OSC1_WAVE` 0, `OSC_MIX` 0, **sub sinus** 0,5, `CUTOFF` 0,34 (200 Hz), `RESONANCE`
  0,3, `FILTER_ENV` 0,55 (2,75 oct.), `F_ATTACK` 0, `F_DECAY` 0,42 (36 ms), `F_SUSTAIN` 0, `A_ATTACK` 0,
  `A_DECAY` 0,5, `A_SUSTAIN` 0,6, `A_RELEASE` 0,2 (5 ms), `DRIVE` 0,35, **vélocité → coupure 0,5**, **phase à zéro**,
  exciteur 0,3 ; sidechain léger (la grille laisse déjà le temps fort libre).
- **Styles** : techno, retro_techno, hard_techno, industrial_techno, tech_house, trance (Drop), minimal.

### 5.13 Sub drop et « bass test »
- **Ce qui le fait taper** : un silence (un temps ou une mesure), puis un sinus très grave qui **descend** d'une
  octave sur 1 à 2 mesures (le « sub drop »), sous le premier temps du drop, avec un impact. Sur un caisson, c'est
  la pression ; sur un téléphone, c'est le silence d'avant et les harmoniques qui glissent.
- **Chez nous** : la piste « Sub drop » (Sub pur, glide de 0,5 s) et le temps de silence (S5b) existent. Manque : la
  saturation du sub drop (sinon il est muet au téléphone). Mettre `DRIVE` 0,5 et l'exciteur sur sa tranche.

---------------------------------------------------------------------------------------------------------------
## 6. Les écarts, dans le moteur, les sons et les recettes

### Moteur
| # | Manque | Pourquoi ça compte | Effort |
|---|---|---|---|
| E1 | **Exciteur de graves** (nouvel effet) : séparation à 80-200 Hz, harmoniques de la bande basse (redressement + tanh, paires et impaires, suréchantillonné ×4 comme `DriveShaper`), passe-bande 150-1 000 Hz, suiveur d'enveloppe pour garder la dynamique, mélange. Réglages : Quantité, Fréquence, Couleur (paires / impaires), Mélange. | +8 à +27 dB au téléphone pour +0,5 à +1,6 LU en pleine bande (§ 3). Marche pour **toutes** les sources : synthé, 808, échantillons de packs, General MIDI, Plaits. | M |
| E2 | **808** : chute de hauteur réglable (quantité 0-36 demi-tons, temps 5-150 ms ; aujourd'hui figée à 2,5× et 6 ms), « tone » en harmoniques 4 à 8, 2e étage d'écrêtage, exciteur interne. | Le « boum », et la 808 entendue au téléphone. | S |
| E3 | **Enveloppe de hauteur du synthé** (quantité ±48 demi-tons, decay 1-500 ms, sur tous les oscillateurs). | 808 au synthé, donk, wub du garage, pluck, kick-basse. | S |
| E4 | **Phase remise à zéro** à chaque note (option, par défaut pour les basses). | Chaque coup attaque pareil, sans clic. | S |
| E5 | **Vélocité → coupure** (0-1 : jusqu'à +2 octaves). | Les basses roulantes qui « galopent » ; aujourd'hui la vélocité ne joue que sur le volume (0,3 + 0,7 v). | S |
| E6 | **Sub en sinus** (forme du sub : sinus, triangle, carré) et **jamais sous 30 Hz** (remonté d'une octave). | Le sub carré une octave sous la note crée des harmoniques que personne ne veut et descend à 23 Hz en octave 1. | S |
| E7 | **Couche médium réglable** : fréquence (150-400 Hz) et drive (aujourd'hui figés à 250 Hz et 3). | Saturer le haut sans salir le sub. | S |
| E8 | Second oscillateur en **demi-tons** (−12..+19), pour la quinte (power chord). | Basse roulante « power chord », hard techno. | S |
| E9 | Mode **mono / legato** explicite (une voix, la note suivante coupe la précédente en 2 ms). | Les notes de basse ne se chevauchent jamais (aujourd'hui seule la glisse prend la même voix). | S |
| E10 | Coupe-bas de 25-30 Hz sur le master de l'assistant. | Rien d'inaudible dans le limiteur. Aujourd'hui : bloqueur de continu à 3 Hz seulement. | S |

### Sons (préréglages)
- À ajouter : « Basse roulante » (5.12), « Basse pluck » (5.4), « 808 synthé » (sinus + chute de hauteur + drive 0,7,
  avec E3), « Donk » (5.10), « Sub chaud » (sinus + drive 0,3 : sa 3e harmonique), « Basse power » (E8).
- À corriger : « Basse sub » (son sub carré une octave dessous : en octave 1, 23-31 Hz) ; « Sub pur » (drive 0,25
  pour sa 3e harmonique) ; « Basse pincée » (Plaits, +26 dB de calibrage : à vérifier à l'oreille).
- Les 808 des packs (trap, drill, phonk, reggaeton, tr808) : des sinus de 32 et 64 Hz sans harmoniques. Ne plus les
  choisir par défaut ; ou les passer dans l'exciteur et un écrêteur sur leur tranche ; ou les refaire (pack 808
  saturées, CC0).

### Recettes et générateur
- **Octave 1 avec sub carré** : hard_techno, acid_techno, industrial_techno (Basse sub), detroit (Basse ronde ; déjà
  signalé dans `docs/hits/techno.md` : « octave 1 → octave 2 », pas fait). Passer en octave 2, ou garder l'octave 1
  avec `SUB` 0.
- **trap, drill, phonk, reggaeton, jersey_club** : la voix 808 du moteur même quand le pack est là (le pack en
  2e choix).
- **grime** : `subBass = true` sous « Lead carré ».
- **trance** : la grille 4 au Drop (16e roulantes), la grille de contretemps au Groove (déjà).
- **tech_house, techno, minimal** : vélocités en cycle 40/70/100/70 et vélocité → coupure (E5) ; « Basse
  roulante ».
- **uk_garage, speed_garage** : chute de hauteur de −5 demi-tons sur 40 ms à l'attaque (le « wub », E3).
- **amapiano** : le log drum avec exciteur 0,4 (sa fondamentale est grave, sa chute de hauteur est sa signature).
- **Bibliothèque de grilles** : `BassPatterns` (les 11 du § 4) dans le modèle, que les recettes nomment (`"pattern":
  "rolling_kbbb"`) au lieu de recopier les chaînes ; la variation (`bassVariation`) en tire une autre de la même
  famille.
- **Notation** : un jeton de triolet dans `StepLine` (grille 10) ; un jeton d'accord de puissance (grille 11).

### Banc
- Il ne rend que les sons intégrés : **il ne voit pas les 808 de packs que Benjamin entend**. Ajouter un passage
  « avec packs » (`--packs`).
- Il ne mesure ni la basse seule, ni ce qui passe par un téléphone, ni l'attaque, ni le grave sous 30 Hz (§ 8).

---------------------------------------------------------------------------------------------------------------
## 7. Plan, par priorité

| # | Changement | Où | Effort | Vérifier |
|---|---|---|---|---|
| **P1** | **Exciteur de graves** (E1), posé par l'assistant sur chaque basse, la tranche 808, le Sub drop et les basses de packs. Quantité par famille : hip-hop 0,5 ; house, techno, pop 0,35 ; bass music 0,25 (les médiums parlent déjà) ; lo-fi, boom bap 0,2. Fréquence 120 Hz, couleur mi-paire. | `Effects.cpp/.h` (nouveau type), `EffectType`, `WizardMix.honest` | M | Host-test : sinus 45 Hz → harmoniques 2 à 10 présentes, sonie pleine bande +≤ 1 LU, aucune allocation ; banc : « basse au téléphone » (§ 8) ≥ cible. |
| **P2** | **La 808 qui tape** : voix 808 par défaut dans les 5 styles 808 (le pack en 2e choix) ; chute de hauteur réglable (E2) à +18/+24 demi-tons en 20-35 ms ; drive 0,7 (0,9 phonk, 0,5 reggaeton) ; « tone » en harmoniques 4 à 8 ; clic 0,5. | `StudioVoices.h`, `StudioDrums.voice808`, `StarterWizard.soundsFor` / `bassOn808` | S | Mesure ci-dessus : 808 trap tél. 300 ≥ −14 dB (aujourd'hui −22), attaque ≥ +4 dB (+1,9). La phase 34 n'est pas encore publiée : on peut changer la voix sans casser de projet. |
| **P3** | **Synthé** : enveloppe de hauteur (E3), phase à zéro (E4), vélocité → coupure (E5), sub sinus et ≥ 30 Hz (E6), couche médium réglable (E7). Paramètres ajoutés en fin de liste : à leurs valeurs par défaut, le son ne change pas (comme S4). | `Synth.h/.cpp`, `SynthPatch`, JNI | M | Host-tests : attaque identique à ±0,5 dB sur 16 notes ; chute de hauteur mesurée (YIN) ; aucune allocation ; `plain()` inchangé aux défauts. |
| **P4** | **Rien sous 28 Hz** : coupe-bas (`PARAM_EQ`, réglage « Coupe-bas » qui existe déjà) à 28 Hz sur les basses, la 808 et le Sub de l'assistant ; master de l'assistant à 25 Hz (E10) ; hard techno, acid techno, industrielle et detroit en octave 2, ou `SUB` 0. | `WizardMix`, `StyleRecipes` / JSON | S | Banc : énergie < 30 Hz ≤ −20 dB du Drop ; perte de la chaîne en baisse sur ces 4 styles. |
| **P5** | **Préréglages** : Basse roulante, Basse pluck, 808 synthé, Donk, Sub chaud ; corriger Basse sub et Sub pur. Gains calibrés à −16 LUFS comme les autres (`SYNTH_GAIN_DB`). | `SoundLibrary` | S (après P3) | Mesure du § 3 sur chacun : tél. 300 ≥ −14 dB, attaque ≥ +4 dB. |
| **P6** | **Bibliothèque de basses roulantes** (§ 4) et choix par style (§ 9) ; vélocités en cycle ; gate de 60-75 % ; la note finit avant le kick. | `BassPatterns.kt` (nouveau), recettes, `MelodyBuilder.bass` | S-M | Test modèle : aucune note de basse ne chevauche la suivante ni ne couvre le pas d'un kick (grilles 1, 4, 5, 8) ; ≥ 2 vélocités par mesure. |
| **P7** | **Banc « basse »** : rendu de la basse seule (`ps_render --solo` existe), mesures du § 8, passage « avec packs ». | `ps_measure.cpp`, `report.py`, `song-bench.sh` | S-M | Les chiffres du § 3 retrouvés sur les morceaux. |
| **P8** | Kick et basse ensemble : en trap, la 808 part sur l'échantillon du kick, avec un kick court (clic) par-dessus ; en house et techno, profondeur du sidechain selon la grille (grille 1 : 3-5 dB ; grille 2 : 8-10 dB). | `WizardMix.musicDuck` | S | Banc : creux de la basse sous le kick (§ 8). |
| **P9** | Notation : triolets et accords de puissance (grilles 10 et 11), oscillateur 2 en demi-tons (E8), mode mono (E9). | `StepLine`, `Synth` | M | Tests de `withLine`. |
| **P10** | **Mesurer le haut-parleur du Magic 8 Pro** : un balayage de 40 Hz à 2 kHz joué par le haut-parleur et enregistré par le micro (comme la mesure de latence), pour caler le « filtre téléphone » du banc. Le micro coupe aussi le grave : comparer à un balayage au casque. | Outils › Latence (même code) | S | Courbe notée dans ce fichier ; filtre du banc recalé. |

Ordre conseillé : P1 et P2 d'abord (c'est ce que Benjamin entendra tout de suite sur son téléphone), puis P4 (gain
gratuit), P3 + P5, P6, P7 en même temps que P1 pour prouver le gain, P8, P9, P10. Test A/B à faire écouter à
Benjamin après P1 + P2 : trap, tech house, techno, dnb, 16 mesures de Drop, **au téléphone**.

---------------------------------------------------------------------------------------------------------------
## 8. Cibles mesurables pour le banc

Toutes sur la **basse seule** (pistes de basse + Sub + tranche 808 + Sub drop, rendues ensemble avec `--solo`) au
Drop ou au Refrain, sauf mention contraire. Filtre téléphone : passe-haut de Butterworth d'ordre 4, puis pondération
K. Le recaler après P10.

| Mesure | Définition | Aujourd'hui (§ 3, sons seuls) | Cible |
|---|---|---|---|
| B1 Basse au téléphone (bon téléphone) | sonie(basse filtrée à 200 Hz) − sonie(basse) | 808 −15, sub −22, ronde −9, bass music −2 à −8 | **≥ −10 dB** |
| B2 Basse au téléphone (téléphone moyen) | même chose à 300 Hz | 808 −22, sub −29, packs −32 à −45 | **≥ −14 dB** |
| B3 Place de la basse au téléphone | sonie(basse filtrée 300 Hz) − sonie(mix filtré 300 Hz) | à mesurer | ≥ −10 LU en hip-hop et bass music, ≥ −14 LU en house et techno (à confirmer à l'écoute) |
| B4 Harmoniques au téléphone | énergie 200-800 Hz − énergie 30-100 Hz de la basse | 808 trap −18, phonk −16 ; Basse sub −27 ; Basse ronde −11 ; Reese DnB −7 ; 808 de pack −47 | **≥ −14 dB** (808, sub sous une couche : ≥ −16) ; ≥ −12 dB (basses roulantes, tenues) |
| B5 Attaque | RMS 5 ms le plus fort des 40 premières ms d'une note − médiane 80-250 ms | 808 +1,9 ; synthés +2 à +6 | **≥ +4 dB** (808, pluck, roulante), ≥ +2 (sub, tenue) |
| B6 Régularité de l'attaque | écart-type de B5 sur 16 notes de même hauteur | à mesurer (phase non remise à zéro) | ≤ 0,5 dB |
| B7 Sub | énergie 40-60 Hz du **mix** au Drop | sub < 60 : −4,8 en médiane | −6 à −3 dB (garder) |
| B8 Infra | énergie < 30 Hz du mix au Drop | Basse sub en Fa#1 : −3 dB de son énergie sous 30 Hz | **≤ −20 dB** |
| B9 Creux sous le kick | RMS de la basse 0-50 ms après chaque kick − RMS 100-200 ms après | à mesurer | ≤ −6 dB en house, techno, trance ; ≤ −3 dB ailleurs |
| B10 Pas de chevauchement | notes de basse qui sonnent en même temps (test modèle) | glisse seulement | 0 hors glissés |
| B11 Mono du grave | corrélation sous 120 Hz | ≥ 0,997 partout | ≥ 0,95 (garder) |
| B12 Pleine bande | sonie du mix avec exciteur − sans | – | ≤ +1 LU |

Bloquants proposés : B2 (≥ −16 dB pendant un passage de transition, puis −14), B8, B12. Les autres listés.

---------------------------------------------------------------------------------------------------------------
## 9. Quelle basse pour chacun des 37 styles

| Style | Famille visée | Aujourd'hui (recette) | Changement |
|---|---|---|---|
| house | contretemps / octaves (grille 7) | Basse ronde ou pack Juno, `R-.R..O.` oct. 2 | exciteur 0,35 ; phase à zéro |
| deep_house | sub rond entre les kicks, pluck (grille 9) | Basse ronde `..R-..R-` | Basse pluck ou ronde, exciteur 0,3 |
| tech_house | roulante + vélocité (grilles 8, 6) | Basse ronde/pack `..Rr..Rr`, sub | Basse roulante, vélocités 40/70/100/70, vélocité → coupure |
| uk_garage | wub (chute de hauteur) + sub | Basse ronde/pack, sub | chute −5 demi-tons / 40 ms (E3) |
| speed_garage | basse qui parle + wub | Basse qui parle, sub | idem uk_garage |
| acid_house | 303 + sub simple | Basse sub `R-......r.......` oct. 2 | exciteur 0,35 ; Sub chaud |
| afro_house | pluck / log drum, triolets (grille 10) | Basse sub | Basse pluck ; triolets (P9) |
| disco | octaves funky (grille 7) | GM basse au doigt / Basse pincée | exciteur 0,3 |
| amapiano | log drum (donk) | Log drum (Plaits) | exciteur 0,4 ; à mesurer |
| techno | roulante K-B-B-B (grille 1) | Basse analogique/pack `.rR-.rRr` | Basse roulante, vélocité → coupure, exciteur 0,35 |
| retro_techno | roulante analogique (grilles 1, 5) | Basse analogique | idem techno |
| acid_techno | 303 + sub | Basse sub **oct. 1 (sub 27 Hz)** | oct. 2 ou Sub chaud, `SUB` 0 |
| detroit | basse funky en octaves | Basse ronde **oct. 1 (sub 27 Hz)** | oct. 2 (déjà demandé dans techno.md) |
| industrial_techno | rumble + roulante saturée | Basse sub **oct. 1** | oct. 2 ou Basse roulante + drive ; coupe-bas 28 Hz |
| hard_techno | roulante saturée / power (grilles 2, 11) | Basse sub **oct. 1 (sub 23 Hz)** `.rR-.rRr` | Basse roulante, drive 0,5, power chord (E8) |
| gabber | le kick est la basse | Basse analogique oct. 1 | garder discret ; coupe-bas 28 Hz |
| acidcore | kick + 303 | Basse analogique oct. 1 | idem gabber |
| minimal | pluck court (grilles 3, 9) | Basse ronde `..R-..r.` | Basse pluck, vélocité → coupure |
| trance | contretemps (Groove) puis roulante (Drop, grille 4) | Basse ronde `..R-..R-` | grille 4 au Drop ; Basse roulante |
| dnb | reese + sub | Basse reese, sub | `SUB` 0 sous la piste Sub ; couche médium à 200 Hz (E7) |
| liquid_dnb | sub rond + reese douce | Basse ronde, sub | exciteur 0,25 |
| neurofunk | growl / FM / formant | pack ou Basse qui parle, sub, switch growl | garder ; FM plus tard |
| jungle | sub + reese / dred | Basse ronde, sub | exciteur 0,25 ; glissés |
| jump_up | growl + sub | Basse growl, sub, switch wobble | garder (passe déjà au téléphone) |
| dubstep | wobble + sub | Basse wobble, sub | drive après le filtre ; Sub drop saturé |
| riddim | wobble carré + sub | Basse wobble (riddim), switch growl | garder |
| boom_bap | basse courte et ronde | pack boom_bap / Basse ronde | Basse pluck, exciteur 0,2 |
| trap | 808 | pack trap:808 (**sinus pur**) ou voix 808 | voix 808 par défaut, P2, exciteur 0,5 |
| drill | 808 qui glisse | pack drill:808 ou voix 808 | idem trap, glissé 180 ms (déjà) |
| lofi | basse chaude et ronde | pack lofi / Basse ronde | exciteur 0,2 |
| phonk | 808 très saturée | pack phonk:808 ou voix 808 | drive 0,9 + 2e étage, exciteur 0,5 |
| grime | basse-lead carrée | Lead carré oct. 2, **pas de sub** | `subBass = true`, exciteur 0,3 |
| jersey_club | 808 courte qui double le kick | pack tr808:808 ou voix 808 | voix 808, chute de 20 ms |
| reggaeton | 808 / sub du dembow | pack reggaeton:808 | voix 808 drive 0,5, exciteur 0,4 |
| afrobeats | sub chaud + log drum | pack afrobeats / Basse ronde oct. 1 | Sub chaud, exciteur 0,35 |
| synthwave | octaves en croches (grille 7) | Basse analogique `R-R.R-R.` | drive 0,3, exciteur 0,3 |
| pop | basse tenue | Basse ronde | exciteur 0,35 |

Pas de psytrance, de hard house ni de bounce parmi nos 37 styles. Leurs basses (grilles 1 et donk) servent
quand même la techno, la trance et l'amapiano.

---------------------------------------------------------------------------------------------------------------
## 10. Ce qui n'a pas pu être vérifié

- L'audio et les transcriptions des vidéos (YouTube refuse la lecture sans connexion). Les grilles 6 (« 3-2-2-3 »),
  10 et 11 sont des lectures du titre du chapitre ; à confirmer en regardant la vidéo 2 (Benjamin, 14 min).
- Le haut-parleur du Magic 8 Pro (P10) : le filtre téléphone est un modèle.
- Les sons Plaits (Basse analogique, Log drum, Basse pincée) et les basses General MIDI : non mesurés ici.
- Les morceaux entiers : seules les basses seules ont été mesurées ; B3 et B9 demandent le banc (P7).

---------------------------------------------------------------------------------------------------------------
## 11. Ce qui est fait (branche `basses`, 2026-10-24)

Mesures avant / après : `docs/son/banc/apres-basses.md` (+ `.json`). En attente de l'écoute de Benjamin.

- **P1** : « Basse audible » (effet 29) posée par l'assistant sur chaque basse, la Basse 2, le Sub, le Sub drop et le
  pad de la 808, échantillons de packs compris (`WizardMix.withAudibleBass`) : 0,5 hip-hop (lo-fi, boom bap 0,2),
  0,35 house / techno / pop, 0,25 bass music, et les valeurs du § 9 (deep house, disco 0,3 ; amapiano, reggaeton
  0,4…). Placée avant l'écrêteur et l'OTT.
- **P2** : la 808 du moteur en version 1 (chute +18 à +24 demi-tons en 20 à 35 ms, attaque relevée pendant la
  chute, « tone » = harmoniques 4 à 8, drive 0,7 / phonk 0,9 / reggaeton 0,5) joue la basse des cinq styles 808
  (le reggaeton entre dans `StudioDrums.BASS_808`), la 808 du pack en 2e choix. Version 0 inchangée au bit près.
- **P3** : les 8 réglages du synthé (enveloppe de hauteur, phase à zéro, vélocité → coupure, forme et plancher du
  sub, fréquence et drive de la couche médium) ajoutés à `SynthPatch`, neutres par défaut (test au bit près).
- **P4** : coupe-bas 28 Hz (EQ paramétrique) sur les basses sans passe-haut au-dessus d'un Sub ; mix version 3 =
  passe-haut d'ordre 4 à 25 Hz sur le master ; toutes les basses de la bibliothèque qui ont un sub ne le jouent plus
  sous 30 Hz (les styles en octave 1 gardent leur octave) ; un Sub n'est jamais sous Do1.
- **P5 / P6** : sons Basse roulante, Basse pluck, 808 synthé, Donk, Sub chaud (FR/EN/NL), placés par style (§ 9) ;
  les 11 grilles du § 4 (`BassPatterns`), proposées à l'étape Basse de l'assistant (« Motifs de basse ») ; la ligne
  de la recette reste le choix par défaut.
- **P7** : banc (B1-B12 dans `report.py`, colonne « basse au téléphone » de l'écoute en direct). Les packs ne
  passent pas par l'export du banc (Robolectric ne décode pas le FLAC) : leurs basses sont mesurées seules
  (`basses-calib/packs_phone.py`).
- **Couches** : une paire sub + médium (« Basse roulante » sur « Sub chaud ») est notée pour le chantier G1b
  (`Track.layers`) ; aucun second système de couches ici.
- Pas encore : P8 (sidechain selon la grille), P9 (triolets et accords de puissance dans la notation, oscillateur 2
  en demi-tons, mode mono), P10 (le haut-parleur du Magic 8 Pro), les réglages du synthé à l'écran.

---------------------------------------------------------------------------------------------------------------
## Sources

- Pages YouTube (titre, auteur, description, chapitres, lus par l'API oEmbed et l'API « next » de YouTube) :
  https://www.youtube.com/watch?v=TNdwGri1Eaw , https://www.youtube.com/watch?v=ZMJq8k3z4Lw ,
  https://www.youtube.com/watch?v=wBcIp8-y8eI , https://www.youtube.com/watch?v=sTLpan3ErAs
- Production Music Live, « How to make a rolling bassline » (4 niveaux : scie en doubles-croches, enveloppe de
  filtre plus longue que celle du volume, LP 24 ; « gallop » : 1re double-croche très douce, la 2e un peu plus,
  vélocité → coupure ; sub ajouté ; couches ; sauts d'octave et accents en contretemps) :
  https://www.productionmusiclive.com/blogs/news/how-to-make-a-rolling-bassline
- Elektronauts, « Psytrance kick bass parameters » et « Making psytrance with the Rytm » (scie redéclenchée,
  attaque immédiate, sustain plein, release très court, filtre bas ouvert par une enveloppe courte ; notes plus
  courtes qu'une double-croche ; la note sur le kick plus douce) : https://www.elektronauts.com/t/psytrance-kick-bass-parameters/253875 ,
  https://www.elektronauts.com/t/making-psytrance-with-the-rytm/31032
- Native Instruments Community, « Psytrance style kick and bass » (trois doubles-croches après chaque kick, la 1re à
  80 sur 100, basse monophonique, phase kick / basse) : https://community.native-instruments.com/discussion/comment/151033
- Production Expert, « How to make sure bass is heard on small speaker devices » (fondamentale absente, MaxxBass,
  saturation multibande) : https://www.production-expert.com/production-expert-1/how-to-make-sure-bass-is-heard-on-small-speaker-devices
- Waves MaxxBass (fiche Sweetwater ; manuel chez B&H) : https://www.sweetwater.com/store/detail/MaxxBass--waves-maxxbass-plug-in ,
  https://www.bhphotovideo.com/lit_files/984241.pdf
- Brevet US 10382857, « Automatic level control for psychoacoustic bass enhancement » (non-linéarité qui génère les
  harmoniques, contrôle de niveau) : https://image-ppubs.uspto.gov/dirsearch-public/print/downloadPdf/10382857
- diyAudio (harmoniques 2 à 5 atténuées, fondamentale de la bande retirée ; brevets d'origine expirés, à vérifier) :
  https://www.diyaudio.com/community/goto/post?id=3684217
- Proceedings IOA, Holland, Newell, Mapp, « Steady state and transient loudspeaker frequency responses » (petits
  radiateurs inefficaces dans le grave) : https://www.ioa.org.uk/system/files/proceedings/k_holland_p_newell_p_mapp_steady_state_and_transient_loudspeaker_frequency_responses.pdf
- Reese, 808, wobble : eMastered, « Reese bass » ; Native Instruments, « Reese bass » ; MusicRadar, « Classic reese
  bass like Renegade's Terrorist » ; EDMProd, « Synth bass » ; Unison, « Synth bass » :
  https://emastered.com/blog/reese-bass , https://blog.native-instruments.com/reese-bass/ ,
  https://musicradar.com/how-to/classic-reese-bass-renegade-terrorist , https://www.edmprod.com/synth-bass/ ,
  https://unison.audio/synth-bass/
- Recherches précédentes du projet : `docs/hits/bass.md` (reese ±0,27 demi-ton, sub séparé, wobble, riddim,
  switch-ups), `docs/hits/hiphop.md` (808 saturée « pour sortir sur un téléphone », glissés de 100 à 200 ms),
  `docs/hits/techno.md` (basse 16e roulante, rumble, trance « la note s'arrête avant le kick suivant »),
  `docs/hits/house.md` (wub du garage : « enveloppe de hauteur descendante au départ de chaque note »).
- Mesures : programmes et sons rendus dans
  `/tmp/claude-0/-home-user-pocket-studio/2ed29bda-5e9a-5850-a6a7-bbf3293c7a09/scratchpad/bassbench/`
  (`render.cpp`, `r808.cpp`, `analyse.py`, `refs.py`) et
  `/tmp/claude-0/-home-user-pocket-studio/2ed29bda-5e9a-5850-a6a7-bbf3293c7a09/scratchpad/packs/` (packs trap et techno
  de la Release `sons-packs-4`, empreinte de l'index vérifiée).
