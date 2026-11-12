# Tour 6, siège son : les 37 morceaux de l'assistant, jugés à la mesure

Version jugée : `1585bb46` (tête de `ccr-80af1a1b-mn0wec` au 2026-11-12). Tableau par style : `par-style.tsv`, coups de kick partie par partie : `kick-par-partie.tsv` (à côté). Cibles par genre et leurs sources : `cibles-genres.md`. Décisions : `docs/decisions/tour-6-son.md`. Relecture neuve : `relecture.md`.

## En bref (à lire à voix haute)

1. Rien de cassé : sur les 37 morceaux, ni écrêtage, ni silence, ni saut, ni à l'export ni en écoute en direct, et la crête reste sous −1 dBTP partout.
2. Le défaut principal est le kick au téléphone, et la mesure te donne raison sur presque tous les styles. Au téléphone, le kick perd environ 12 dB de son propre niveau, quand le morceau entier n'en perd que 4. Sur 28 styles sur 37, un coup de kick ne soulève plus le morceau des 3 dB visés.
3. Les morceaux sont un peu plus doux que les vrais morceaux du genre, de 0,2 à 2,4 LU, surtout en hard techno, dubstep et house. Le master est réglé pour ne pas écraser le son.
4. Les basses n'ont presque rien au-dessus de 2 kHz et les leads sont des sons très propres, presque sans bruit ni saturation. Cela va dans le sens de ton « années 90-2000 », mais aucune mesure publiée ne permet de le trancher.
5. À écouter en priorité sur ton téléphone : phonk, dubstep, trance, techno et drum and bass. Guette d'abord le kick du Drop ou du Refrain.

## Comment c'est mesuré

- **Les morceaux :** faits par le vrai code de l'appli (« Partir d'un style », morceau de l'assistant), rendus par le moteur sur Linux et mesurés par le banc (`scripts/song-bench.sh`, `ps_measure`). C'est le chemin du banc, aucun son n'est gardé.
- **L'écoute en direct, avec le moteur comme sur le téléphone (`scripts/live-bench.sh`, 150 s) :** 8 styles, ceux à écouter plus phonk, house, riddim et acidcore, soit 8 sur 8 sans alerte, 0 fondu, 0 silence, 0 saut. Les 29 autres styles n'ont été jugés que sur l'export, rendu par le même moteur avec les commandes de l'appli.
- **Le téléphone :** les mêmes mesures sont refaites à travers les deux haut-parleurs que proposent les sources (cibles § 7). Le premier est un téléphone récent qui coupe sous 300 Hz (comme le « téléphone 300 Hz » du banc), le second un pire cas qui coupe sous 500 Hz. Ta vraie courbe, mesurée par « Écoute au micro », les remplacera dès qu'elle existera (`scripts/son/son6.py --curve`).
- **Les pistes seules :** le kick, la basse, le lead et le morceau avant le master sont rendus seuls, comme le banc le fait.
- **Le coup de kick :** même calcul que le banc. C'est de combien un coup de kick soulève le morceau exporté, sur ses 40 ms contre les 100 ms d'avant, sommé sur les coups d'une partie. En pleine bande, il redonne les chiffres du banc sur les 122 parties.
- **Pour relancer :** `scripts/son/son6.py`, `son6_kick.py`, `tableau_son6.py`.

## Défauts

### Bloquant : aucun

- Écrêtage, NaN, continu : 0 sur 37. Crête vraie entre −1,20 et −1,16 dBTP (cible ≤ −1).
- Silences de plus d'une seconde et sauts de 12 dB au milieu d'un morceau : 0 sur 37 à l'export, 0 sur 8 en direct.

### Grave

**G1. Le kick ne ressort plus au téléphone, sur la plupart des styles** (toi : « kick trop faible sur tous les styles »).

- **Ce que perd le kick :** au téléphone, le kick seul perd 12,2 dB de son niveau (médiane, de 6,3 à 14,7). Le morceau entier ne perd que 3,9 dB. Sur le pire cas, c'est 14,8 dB contre 4,6.
- **Sa place dans le morceau :** en pleine bande, le kick se tient 1,4 LU sous le morceau (médiane). Au téléphone, il tombe 9,8 LU dessous, et 11,6 sur le pire cas.
- **Le coup de kick dans la partie principale (Drop, sinon Refrain) :**

  | Haut-parleur | Médiane | Sous 3 dB (visé) | Sous 2 dB (minimum) |
  |---|---|---|---|
  | Pleine bande | 3,9 dB | 7 styles | aucun |
  | Téléphone | 1,8 dB | 28 styles | 23 styles |
  | Pire cas | 1,0 dB | 26 styles | 24 styles |

- **Les plus faibles au téléphone (pleine bande → téléphone → pire cas) :**

  | Style | Partie | Pleine bande | Téléphone | Pire cas |
  |---|---|---|---|---|
  | phonk | Refrain | 2,3 | −0,8 | −1,1 |
  | jungle | Drop | 2,1 | 0,2 | −1,3 |
  | dubstep | Drop | 3,1 | 0,3 | −0,6 |
  | speed garage | Drop | 3,6 | 0,4 | 0,4 |
  | trance | Drop | 2,8 | 0,5 | 0,2 |
  | techno | Drop | 2,9 | 0,5 | 0,8 |
  | drum and bass | Drop | 3,6 | 0,6 | −0,4 |
  | disco | Drop | 3,7 | 0,9 | 1,0 |
  | jersey club | Refrain | 5,1 | 0,9 | 0,5 |
  | grime | Refrain | 4,9 | 1,1 | −0,2 |

- **Ceux qui tiennent au téléphone, au-dessus de 3 dB (9 styles) :** riddim 6,6, acidcore 6,5, afro house 5,5, boom bap 5,3, lo-fi 4,2, acid techno 4,2, house 3,6, afrobeats 3,4 et neurofunk 3,0. Leur kick est plus saturé, ou son attaque est plus claire.
- **Pourquoi, d'après la mesure :**
  - Le kick vit presque tout entier sous 120 Hz, que le téléphone ne rend pas.
  - Au téléphone, la bande la plus forte du morceau est le bas-médium (250 à 800 Hz, −2,9 dB de part en médiane). Elle remplit le temps entre les coups, donc rien ne « soulève » le coup.
  - Les sources (cibles § 7) disent qu'un kick s'entend sur un petit haut-parleur grâce à ses harmoniques et à un clic vers 2 à 5 kHz, que ces kicks ont peu.

### Moyen

**M1. Plus doux que le genre** (LU sous le bas de la plage du genre, colonne `lu_sous_cible`) :

- Cibles mesurées :

  | Style | Mesure | Mesuré | Cible | Écart |
  |---|---|---|---|---|
  | hard techno | morceau | −10,4 | −8,0 à −6,4 | −2,4 |
  | dubstep | Drop | −8,6 | −6,5 à −4 | −2,1 |
  | house | morceau | −10,2 | −9,0 à −7,1 | −1,2 |
  | drum and bass | Drop | −8,6 | −7,9 à −5,2 | −0,7 |
  | trance | Drop | −8,3 | −8,1 à −6,7 | −0,2 |

- Cibles estimées seulement, sans mesure publiée, donc laissées en moyen : riddim −5,2, jump up −3,7, neurofunk −3,1, gabber −2,5, acidcore −1,9, acid techno −1,6, amapiano −1,5, et moins de 1 LU pour 6 autres.
- **Cause :** le master s'arrête exprès quand il retirerait plus de 3 LU. Le banc l'indique « plafonné » pour hard techno, jump up et riddim. Les rendre plus forts écraserait le son : c'est un choix, pas une panne.
- 19 styles sont dans la plage de leur genre, dont trap, pop, reggaeton, afrobeats, deep house, minimal, tech house, disco et techno.

**M2. Au téléphone, le Drop ne passe plus au-dessus de la fin de sa Montée** (mesure du banc à 300 Hz, listée sans bloquer : moins de +1,5 dB) :
- 13 styles : house (−1,5 / −0,6), speed garage, rétro techno, afro house, techno, amapiano, tech house, acid techno, deep house, acid house, industrielle, trance, hard techno (premier Drop).
- Sur un téléphone, l'effet « ça tombe » du Drop vient surtout du grave, que le téléphone ne rend pas. Même cause que G1.

**M3. Contrastes un peu courts** (critères du banc) :
- Drop contre Groove : phonk +1,8 et jungle +2,0, pour un seuil de +2.
- Drop contre Break : trance +2,9, gabber +3,2, hard techno +3,3 et acid house +3,8, pour un seuil de +4.
- Montée qui ne monte pas : acid techno +1,8, pour un seuil de +2. Deep house, Detroit, disco et minimal ne montent pas non plus, mais c'est voulu : leur Montée est « légère » par conception.

**M4. Basse au téléphone** (critères B du banc, à 300 Hz) :
- La techno industrielle est la seule sous la cible, avec trois écarts :

  | Critère | Mesuré | Cible |
  |---|---|---|
  | B2 Basse au téléphone 300 Hz | −15,4 dB | −14 |
  | B1 Basse au téléphone 200 Hz | −11,5 dB | −10 |
  | B4 Harmoniques de la basse | −15,7 dB | −14 |

- La hard techno est juste sous la cible B1 (−10,3).
- B6, la régularité de l'attaque de la basse, n'est tenue que par 7 styles sur 37 (valeurs de 0,7 à 71 dB). C'est une mesure du banc à revoir avant d'en tirer quoi que ce soit.

**M5. Perte en mono au-dessus de la règle usuelle de 1 LU** (cibles § 6) :
- 4 styles : dubstep 1,5, neurofunk 1,3, grime 1,2 et gabber 1,1.
- Le banc tolère jusqu'à 1,5 LU, donc rien de bloquant : sur une seule enceinte, ces 4 styles perdent un peu de leur largeur.
- Le grave reste en mono partout : corrélation sous 120 Hz de 0,996 à 1,000.

## Le caractère des timbres (toi : « ça sonne années 90-2000 »)

Ce que la mesure dit. Ce sont des constats, sans note, faute de référence publiée en chiffres (cibles § 8).

- **Basses très propres.**
  - Presque rien au-dessus de 2 kHz : −43 dB de part en médiane (de −87 à −19). Leur centre de gravité est vers 140 Hz.
  - Leur spectre est très « tonal » : platitude −61 dB, c'est-à-dire peu de bruit et peu de saturation.
  - Les sources disent qu'une basse actuelle se sature pour créer des harmoniques, et que c'est aussi ce qui la fait entendre au téléphone. Au téléphone, la basse seule perd 9,7 dB (médiane).
  - Les basses les plus riches sont celles de phonk, grime, synthwave, jump up, techno et rétro techno, entre −19 et −22 dB au-dessus de 2 kHz. Les plus pures sont celles de la techno industrielle (−87), du gabber (−77), de la hard techno (−69) et du lo-fi (−56).
- **Leads propres et assez larges.**
  - Platitude −35 dB en médiane : des formes d'onde nettes, peu de bruit.
  - Leur largeur stéréo est déjà moderne : côté contre centre −2 dB.
  - Leur brillance bouge d'environ une demi-octave au fil du morceau. Il y a donc du mouvement, sans pouvoir dire s'il est « assez ».
  - Les leads les plus sombres, avec moins de −15 dB au-dessus de 2 kHz : tech house, UK garage, speed garage, jersey club, deep house, industrielle, lo-fi et phonk. Ce sont des sons filtrés, qui peuvent sonner « vieux » sur un téléphone.
- **Ensemble.**
  - La pente du spectre est de −4,75 dB par octave en médiane, dans la norme des musiques électroniques actuelles : environ −4,5 à −5 (cibles § 6).
  - Le grave pèse dans la norme pour la house : 81 % de l'énergie sous 250 Hz, pour un repère de 78 %. Il pèse moins que le repère en techno dure : 50 % pour la hard techno et 74 % pour la techno, pour un repère de 88 %. Ce repère est calculé avec une autre pondération, donc c'est indicatif.
- **Ce que la mesure ne peut pas dire.**
  - Si un son « fait années 90 » : c'est une question d'oreille et de références.
  - Pour trancher, il faudrait mesurer 10 à 20 vrais morceaux récents par famille avec ces mêmes outils, puis comparer basse, lead et platitude.
  - Une autre discussion travaille sur le remplacement des sons : ces chiffres sont pour elle.

## Les 5 styles à écouter en priorité sur ton téléphone

1. **Phonk**, au Refrain : le kick. C'est le plus faible des 37 au téléphone : son coup ne soulève plus rien (−0,8 dB). Écoute aussi le lead, très sombre.
2. **Dubstep**, au Drop : le kick (0,3 dB au téléphone), puis la basse. Il est aussi plus doux que son genre (2,1 LU sous la plage) et c'est celui qui perd le plus en mono.
3. **Trance**, au Drop : le kick (0,5 dB) et le lead. Le Drop se distingue mal du Break (+2,9) et de la fin de la Montée au téléphone (+0,9).
4. **Techno**, au Drop : le kick (0,5 dB), puis la basse. Au téléphone, le Drop passe sous la fin de la Montée (−0,9).
5. **Drum and bass**, au Drop : le kick (0,6 dB au téléphone, −0,4 sur le pire cas) et la basse, pour savoir si on la sent encore.

Pour comparer, écoute la house, qui tient (3,6 dB au téléphone), et la riddim, la meilleure (6,6 dB).

## Ce qui n'a pas pu être fait ici

- Ta vraie courbe de téléphone : l'« Écoute au micro » n'a pas encore tourné, donc les deux haut-parleurs sont ceux que proposent les sources.
- L'écoute en direct ne couvre que 8 styles sur 37 ; les 29 autres sont jugés sur l'export.
- Aucune comparaison avec de vrais morceaux du genre mesurés par ces outils : les cibles viennent de publications, et 22 sous-styles n'ont que des estimations.
- Ni la sonie ni les bandes ne disent si un son « fait vieux ».
