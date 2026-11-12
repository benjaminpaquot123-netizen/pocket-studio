# Tour 6, siège son : les 37 morceaux de l'assistant, jugés à la mesure

Version jugée : `1585bb46` (tête de `ccr-80af1a1b-mn0wec` au 2026-11-12). Tableau par style : `par-style.tsv` (à côté). Cibles par genre et leurs sources : `cibles-genres.md`. Décisions : `docs/decisions/tour-6-son.md`.

## En bref (à lire à voix haute)

1. Rien de cassé : sur les 37 morceaux, aucun écrêtage, aucun silence, aucun saut, et la crête reste sous −1 dBTP partout.
2. Le défaut principal est le kick sur téléphone, et la mesure te donne raison : au téléphone, le kick perd environ 10 dB de son propre niveau, alors que le morceau entier n'en perd que 3. Sur 25 styles sur 37, un coup de kick soulève le morceau de moins de 3 dB, alors que le banc vise 3 dB au moins.
3. Les morceaux sont un peu plus doux que les vrais morceaux du genre : environ 1 à 2,5 LU de moins en dubstep, hard techno, drum and bass et house. La faute en revient au master, réglé pour ne pas écraser le son.
4. Les basses n'ont presque rien au-dessus de 2 kHz et les leads sont des sons très propres, presque sans bruit ni saturation. Cela va dans le sens de ton « années 90-2000 », mais aucune mesure publiée ne permet de le trancher.
5. À écouter en priorité sur ton téléphone : phonk, drum and bass, dubstep, trance et techno, en guettant d'abord le kick du Drop.

## Comment c'est mesuré

- Les morceaux sont faits par le vrai code de l'appli (« Partir d'un style », morceau de l'assistant), puis rendus par le moteur sur Linux et mesurés par le banc (`scripts/song-bench.sh`, `ps_measure`). C'est le chemin du banc, et aucun son n'est gardé.
- Les mêmes mesures sont refaites à travers deux haut-parleurs de téléphone estimés : un « téléphone » qui coupe sous 220 Hz et un « pire cas » qui coupe sous 400 Hz (le haut-parleur de la plupart des téléphones ne rend rien d'utile sous 230 à 600 Hz, voir les cibles § 7). Ta vraie courbe, mesurée par « Écoute au micro », les remplacera dès qu'elle existera (`scripts/son/son6.py --curve`).
- Le kick, la basse, le lead et le morceau avant le master sont rendus seuls, comme le banc le fait (`scripts/son/son6.py`, `son6_kick.py`).
- Le coup de kick se calcule comme dans le banc : de combien un coup de kick soulève le morceau exporté, sur ses 40 ms contre les 100 ms d'avant. En pleine bande, mon calcul redonne au centième près les chiffres du banc (vérifié sur drum and bass et house).
- Les outils et la façon de les relancer : `scripts/son/son6.py`, `son6_kick.py`, `tableau_son6.py`.

## Défauts

### Bloquant : aucun

- Écrêtage, NaN, continu : 0 sur 37. Crête vraie : entre −1,20 et −1,16 dBTP partout (cible ≤ −1).
- Silences de plus d'une seconde et sauts de 12 dB au milieu d'un morceau : 0 sur 37.
- Image stéréo saine : basses en mono (corrélation sous 120 Hz de 0,996 à 1,000) ; perte en mono de 0,6 LU en médiane (au pire 1,5), pour une règle usuelle de moins de 1 LU (cibles § 6).

### Grave

**G1. Le kick ne ressort plus au téléphone, sur la plupart des styles** (toi : « kick trop faible sur tous les styles »).

- **Ce que perd le kick :** à travers le téléphone, le kick seul perd 10,0 dB de son niveau (médiane, entre 5,1 et 12,2), et 14,0 dB sur le pire cas. Le morceau entier ne perd que 3,3 dB (4,4 dB sur le pire cas).
- **Sa place dans le morceau :** en pleine bande, le kick se tient 1,4 LU sous le morceau (médiane). Au téléphone, il tombe 8,2 LU dessous, et 10,7 LU sur le pire cas.
- **Le coup de kick dans la partie principale (Drop ou Refrain) :**

  | Haut-parleur | Médiane | Sous 3 dB (visé) | Sous 2 dB (minimum) |
  |---|---|---|---|
  | Pleine bande | 3,9 dB | 7 styles | aucun |
  | Téléphone | 2,35 dB | 25 styles | 14 styles |
  | Pire cas | 1,3 dB | — | 25 styles |

- **Les pires (pleine bande → téléphone → pire cas) :**

  | Style | Partie | Pleine bande | Téléphone | Pire cas |
  |---|---|---|---|---|
  | phonk | Refrain | 2,3 | −0,3 | −1,1 |
  | jungle | Drop | 2,1 | 1,2 | −0,6 |
  | dubstep | Drop | 3,1 | 1,3 | −0,3 |
  | drum and bass | Drop | 3,6 | 1,2 | 0,1 |
  | trance | Drop | 2,8 | 0,9 | 0,2 |
  | speed garage | Drop | 3,6 | 0,6 | 0,4 |
  | techno | Drop | 2,9 | 0,5 | 0,6 |
  | disco | Drop | 3,7 | 0,7 | 1,0 |

- **Ceux qui tiennent :** acidcore, riddim, afro house, boom bap et house restent au-dessus de 3 dB au téléphone. Leur kick est plus saturé, ou son attaque est plus claire.
- **Pourquoi, d'après la mesure :**
  - Le kick vit presque tout entier sous 120 Hz, que le téléphone ne rend pas.
  - Le haut de la basse et du bas-médium (250 à 800 Hz) remplit le temps entre les coups. Au téléphone, c'est la bande la plus forte (−3,0 dB de part en médiane), donc rien ne « soulève » le coup.
  - Les sources (cibles § 7) disent qu'un kick s'entend sur un petit haut-parleur grâce à ses harmoniques et à un clic vers 2 à 5 kHz, que ces morceaux ont peu.
  - La preuve par style est dans `par-style.tsv`, colonnes `coup_kick_*` et `kick_contre_mix_tel*`, et partie par partie dans `kick-par-partie.tsv`.

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

- Cibles estimées seulement, sans mesure publiée : riddim −5,2, jump up −3,7, neurofunk −3,1, gabber −2,5, acidcore −1,9, acid techno −1,6, amapiano −1,5. Je les laisse en moyen, la cible n'étant qu'une estimation.
- **Cause :** le master s'arrête exprès quand il retirerait plus de 3 LU (plafonné dans le rapport du banc : hard techno, jump up, riddim). Rendre ces styles plus forts écraserait le son. C'est un choix, pas une panne.
- Le reste est dans la plage de son genre : trap, pop, reggaeton, afrobeats, deep house, minimal, tech house, afro house, disco, techno.

**M2. Au téléphone, le Drop ne passe plus au-dessus de la fin de sa Montée** (le banc le liste sans bloquer : moins de +1,5 dB) :
- 13 styles : house (−1,5 / −0,6), speed garage, rétro techno, afro house, techno, amapiano, tech house, acid techno, deep house, acid house, industrielle, trance, hard techno (premier Drop).
- Sur un téléphone, l'effet « ça tombe » du Drop vient surtout du grave, que le téléphone ne rend pas. Même cause que G1.

**M3. Contrastes un peu courts** (critères du banc) :
- Drop contre Groove : phonk +1,8 et jungle +2,0, pour un seuil de +2.
- Drop contre Break : trance +2,9, gabber +3,2, hard techno +3,3 et acid house +3,8, pour un seuil de +4.
- Montée qui ne monte pas sur ses 4 dernières mesures : acid techno +1,8, pour un seuil de +2. Deep house, Detroit, disco et minimal ne montent pas non plus, mais c'est voulu : leur Montée est « légère » par conception.

**M4. Basse au téléphone (critères B du banc).**
- La techno industrielle est la seule sous la cible, avec trois écarts :

  | Critère | Mesuré | Cible |
  |---|---|---|
  | B2 Basse au téléphone 300 Hz | −15,4 dB | −14 |
  | B1 Basse au téléphone 200 Hz | −11,5 dB | −10 |
  | B4 Harmoniques de la basse | −15,7 dB | −14 |

- La hard techno est juste sous la cible B1 (−10,3).
- B6, la régularité de l'attaque de la basse, n'est tenue que par 7 styles sur 37. C'est une mesure du banc à revoir avant d'en tirer quoi que ce soit : ses valeurs vont de 0,7 à 71 dB.

## Le caractère des timbres (toi : « ça sonne années 90-2000 »)

Ce que la mesure dit. Ce sont des constats, sans note, faute de référence publiée en chiffres (cibles § 8).

- **Basses très propres.**
  - Presque rien au-dessus de 2 kHz : −43 dB de part en médiane (entre −87 et −19). Leur centre de gravité est vers 140 Hz.
  - Leur spectre est très « tonal » : platitude −61 dB, c'est-à-dire peu de bruit et peu de saturation.
  - Les sources disent qu'une basse actuelle se sature pour créer des harmoniques, et que c'est aussi ce qui la fait entendre au téléphone.
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

1. **Phonk**, au Refrain : le kick. C'est le plus faible des 37 au téléphone : son coup ne soulève plus rien (−0,3 dB). Écoute aussi la cloche, le lead, qui est très sombre.
2. **Drum and bass**, au Drop : le kick (1,2 dB au téléphone, 0,1 sur le pire cas) et la basse, pour savoir si on la sent encore.
3. **Dubstep**, au Drop : le kick (1,3 dB, et −0,3 sur le pire cas), puis la basse. C'est aussi le style le plus doux face à son genre : 2,1 LU sous la plage.
4. **Trance**, au Drop : le kick (0,9 dB) et le lead. Le Drop se distingue mal du Break (+2,9) et de la fin de la Montée au téléphone (+0,9).
5. **Techno**, au Drop : le kick (0,5 dB au téléphone), puis la basse. Au téléphone, le Drop passe sous la fin de la Montée (−0,9).

Pour comparer, écoute la house, qui tient (3,3 dB au téléphone), et l'acidcore, le meilleur (6,5 dB).

## Ce qui n'a pas pu être fait ici

- Ta vraie courbe de téléphone : l'« Écoute au micro » n'a pas encore tourné, donc les deux haut-parleurs sont estimés.
- L'écoute en direct (`live-bench`) n'a pas été rejouée ici : les mesures viennent de l'export, rendu par le même moteur et les mêmes commandes.
- Aucune comparaison avec de vrais morceaux du genre mesurés par ces outils : les cibles viennent de publications, et 22 sous-styles n'ont que des estimations.
- Ni la sonie intégrée ni les bandes ne disent si un son « fait vieux ».
