# Désaccords, contre-mesure B (RAR 5, 6, 7, 8)

Deux outils mesurent les mêmes sons :
- la **table** (le premier outil, `scripts/sons/tout.py` + `scripts/sons/mesure.py`) ;
- la **contre-mesure** (`scripts/sons/passe2/verif/mesure.py`).

Les tolérances sont celles de `TOLERANCES.md`. Le détail son par son est dans `rar5.csv` à `rar8.csv`, et les résumés dans `rarN-resume.md`. Les sons sont nommés par leur clé de table, jamais par leur nom de fichier.

## 1. Accord global (4 archives)

| colonne | comparés | accord |
|---|---|---|
| durée, crête, niveau moyen | 3 633 | 100 % |
| t20, t60, brillance, aigus, attaque/corps | 438 | 100 % |
| part tonale (tonal_db) | 437 | 97,3 % |
| hauteur du corps (pitch_steady) | 287 | 72,5 % |
| hauteur de l'attaque (pitch_attack) | 287 | 54,0 % |
| note des sons mélodiques (f0) | 438 | 66,2 % |

Toutes les colonnes de niveau et de temps sont d'accord à 100 %. Seules les hauteurs et la part tonale passent sous 98 %.

## 2. Colonnes sous 98 %, et qui se trompe

Les preuves se trouvent toutes dans le script de preuve du vérificateur : il fabrique des signaux dont on connaît la bonne réponse, puis les passe aux deux outils.

### Kick : hauteur de l'attaque (87 sons, 8 % d'accord)

- Dans 72 cas sur 74 désaccords, la contre-mesure donne plus bas que la table (rapport médian 0,77).
- Exemples : `r5-p1-0aaded21f2` (table 375 Hz, contre-mesure 285 Hz) et `r5-p1-21003e8925` (379 Hz contre 321 Hz).
- **Qui se trompe : la contre-mesure.**
- Preuve : on fabrique un kick dont la hauteur glisse de 200 à 50 Hz, et la vraie moyenne entre 2 et 12 ms vaut 145,9 Hz. La table trouve 144,0 Hz, la contre-mesure 121,9 Hz. Avec un second réglage, la vérité est 140,5 Hz : la table donne 138,4 Hz et la contre-mesure 123,5 Hz.
- Cause : la fenêtre d'attaque de la contre-mesure s'allonge d'une période de la note du corps, donc elle mord sur la partie déjà descendue.

### Kick : hauteur du corps (81,6 % d'accord, 6 sons vides côté contre-mesure)

- Exemples : `r5-p1-70d5992c73` (table 51,6 Hz, contre-mesure vide) et `r5-p1-21003e8925` (64,0 Hz contre 57,1 Hz).
- **Qui se trompe : la contre-mesure, sur les queues courtes.**
  - Preuve : pour un kick pur de 50 Hz qui s'éteint en 25 ms, la table trouve 50,1 Hz et la contre-mesure 46,6 Hz. Quand la queue dure 40 ms, les deux trouvent juste.
- **Personne, sur les queues qui glissent encore.**
  - Preuve : quand la queue descend de 59 à 47 Hz pendant la fenêtre de mesure, la table dit 53,7 Hz et la contre-mesure 51,3 Hz. Les deux valeurs sont dans la plage réelle : la « hauteur du corps » n'a pas de valeur unique pour ce son.

### Caisse claire, clap, percussion, rim, tom, crash, shaker : hauteurs

Le tableau donne les comparés, puis l'accord (attaque / corps).

| famille | comparés | accord attaque / corps | exemples (corps) |
|---|---|---|---|
| clap | 52 | 82,7 % / 71,2 % | `r5-p1-28f5210a09`, `r5-p1-2c56189955` |
| caisse claire | 41 | 68,3 % / 75,6 % | `r6-p1-06efdaac17`, `r6-p1-27f875a671` |
| percussion | 38 | 71,1 % / 63,2 % | `r5-p1-2c2c074753`, `r5-p1-2cf48cf5ae` |
| rim | 3 | 0 % / 0 % | `r6-p1-2d2fb183ef`, `r6-p1-7b11339531` |
| tom | 10 | 90 % / 90 % | `r8-p2-aa76a53abe`, `r8-p2-620b6581fb` |
| crash | 3 | corps 1 sur 3 | `r8-p1-7b1bb1af73`, `r8-p1-a827049598` |
| shaker | petit nombre | corps en désaccord | `r6-p1-6b2f1203a4` |
| percussions grave / médium / aiguë | 5 chacune | quelques désaccords | `r8-p2-0c14851aef`, `r8-p2-67ac647bef` |

- **Qui se trompe : aucun des deux n'est fiable pour la hauteur du corps.**
  - Preuve : une caisse claire synthétique dont le corps est à 200 Hz, plus du bruit, tirée 5 fois. La table trouve entre 207 et 254 Hz, la contre-mesure entre 223 et 276 Hz : les deux s'écartent de 4 à 38 %. Sur ces familles, le bruit domine et la « hauteur du corps » dépend de la fenêtre.
- Sur la même caisse claire, la hauteur de l'attaque est juste chez les deux (192 à 204 Hz).
- Les désaccords d'attaque sur ces familles viennent donc du son lui-même (bruit, plusieurs claquements pour un clap), pas d'une erreur d'un outil en particulier.

### Part tonale sur les familles bruitées

Le tableau donne les comparés et l'accord.

| famille | comparés | accord | exemples |
|---|---|---|---|
| crash | 13 | 84,6 % | `r6-p1-6d60d8072a`, `r6-p1-b80683991e` |
| shaker | 14 | 92,9 % | `r6-p1-b17e479cb2` |
| percussion | 87 | 95,3 % | `r5-p1-f59cb0cb98`, `r6-p1-9a6b8165de` |
| hat fermé | 42 | 95,2 % | `r6-p1-e9537ea33d`, `r8-p1-993307831c` |
| hat ouvert | 24 | 95,8 % | `r6-p1-157014b9e4` |
| clap | 84 | 97,6 % | `r8-p1-70d0439f57`, `r8-p1-d0284ea7f1` |

- **Qui se trompe : personne. C'est une différence de convention sur les sons stéréo.**
- Les deux outils choisissent différemment entre le mélange des deux canaux et le canal le plus fort :
  - la table garde le mélange tant qu'il ne perd pas plus de la moitié de la puissance moyenne ;
  - la contre-mesure compare le mélange au canal le plus fort.
- Preuve : un signal stéréo avec une note à 3 kHz dans le canal gauche seulement. La table garde le mélange et trouve -25,3 dB ; la contre-mesure prend le canal gauche et trouve -20,5 dB. Sur le canal gauche seul, les deux trouvent -20,5 dB.

### Note des sons mélodiques (f0) : 438 sons, 66,2 % d'accord

- 105 désaccords, 25 à une octave près et 18 vides d'un côté. La contre-mesure est plus haute dans 102 cas sur 129.
- Exemples : `r5-p1-01d909656a` (table 58 Hz, contre-mesure 456 Hz) et `r5-p1-03144f23a5` (37 Hz contre 219 Hz).
- **Qui se trompe : la contre-mesure.**
- Preuve : une note de 110 Hz dont la fondamentale est faible. La table (méthode YIN) trouve 110 Hz, la contre-mesure 220 Hz. Même chose à 65,41 Hz, où la contre-mesure trouve 130,82 Hz. Quand la fondamentale est forte (220 Hz), les deux trouvent juste.
- Cause : la contre-mesure prend une harmonique au lieu de la fondamentale.

**Bilan : aucune colonne de la table n'est à corriger.** Ce bilan rejoint celui du vérificateur A.

## 3. Étiquettes contredites par la mesure

Les seuils viennent de mes propres mesures sur les 4 archives :
- t60 des hats fermés : de 51 à 194 ms (médiane 96 ms) ;
- t60 des hats ouverts : de 74 à 491 ms (médiane 164 ms) ;
- t60 des kicks : au plus 421 ms.

| contradiction | rar5 | rar6 | rar7 | rar8 |
|---|---|---|---|---|
| kick qui sonne comme un 808 (t60 > 700 ms et tonal > -10 dB) | 0 | 0 | — | 0 |
| 808 qui sonne comme un kick (t60 < 300 ms) | 0 | 0 | — | 0 |
| hat ouvert très court (t60 < 150 ms) | 0 | 0 | — | p1 : 7, p2 : 2 |
| hat fermé long (t60 > 400 ms) | 0 | 0 | — | 0 |
| caisse claire / clap | non mesurable | non mesurable | — | non mesurable |

- Exemples de hats ouverts très courts : `r8-p1-5173dd889b` (74 ms), `r8-p1-5a63b95989` (127 ms), `r8-p1-ae2f7d1768` (135 ms).
- Le seuil de 150 ms tombe dans la zone où les deux familles se chevauchent. Ces 9 sons sont donc douteux, pas forcément faux (voir § 6).
- La lecture du chemin des fichiers trouve aussi, dans rar6 (p1), 4 sons rangés « hat ouvert » par la table alors que leur chemin dit « hat fermé ». Elle ne trouve aucune confusion kick / 808 ni caisse claire / clap.
- Aucune colonne mesurée ne sépare une caisse claire d'un clap : le nombre de claquements n'est pas mesuré.
- RAR 7 n'a aucun coup de batterie (`—` dans le tableau). Ses types :
  - 605 boucles mélodiques ;
  - 599 voix ;
  - 269 pistes de kits ;
  - 70 notes seules ;
  - des MIDI.

## 4. Nombre de sons « 808 » par pack

| rar | pack | sous-type 808 dans la table | chemin qui dit « 808 » |
|---|---|---|---|
| 5 | p1 | 0 | 0 |
| 6 | p1 | 0 | 0 |
| 7 | p1, p2, p3 | 0 | 0 |
| 8 | p1, p2, p3 | 0 | 0 |

Ces quatre archives ne contiennent aucun 808.

## 5. Fichiers sans ligne, lignes sans fichier

| rar | pack | audio | midi | fichiers sans ligne | lignes sans fichier |
|---|---|---|---|---|---|
| 5 | p1 | 465 | 70 | 0 | 0 |
| 6 | p1 | 487 | 136 | 0 | 0 |
| 7 | p1 | 0 | 383 | 0 | 0 |
| 7 | p2 | 1 413 | 40 | 0 | 8 (MIDI) |
| 7 | p3 | 130 | 52 | 0 | 0 |
| 8 | p1 | 700 | 20 | 0 | 0 |
| 8 | p2 | 338 | 60 | 0 | 0 |
| 8 | p3 | 100 | 0 | 0 | 0 |

- Tous les fichiers ont leur ligne.
- RAR 7 p2 :
  - La table y a 48 lignes MIDI pour 40 fichiers MIDI dans l'archive. Il n'y a ni doublon de clé, ni fichier caché, ni archive dans l'archive, et le test d'intégrité de l'archive passe.
  - Les 8 lignes en trop portent des rôles de kit (accords, basse, mélodie). Exemples : `r7-p2-13760b9820`, `r7-p2-d218135a2d`.
  - Cause non trouvée (voir § 6).
- Les 5 lignes de kit de RAR 7 p2 qui n'ont pas de fichier désignent des dossiers de kit. C'est normal, et chaque dossier existe bien.

## 6. Ce qui reste douteux

1. **RAR 7 p2 : type des petites voix d'ornement.**
   - Un dossier dont le nom désigne des petites voix contient 524 fichiers. La table range 512 d'entre eux en « boucle mélodique » et 12 en « note seule ».
   - Le même pack range en « voix » les voix complètes des mêmes chanteuses et chanteurs.
   - C'est probablement une erreur d'étiquette de la table, mais je n'en ai pas de preuve mesurée : rien dans les colonnes ne distingue une voix d'un instrument.
   - Je n'ai pas relevé d'exemples par clé : l'extraction était déjà effacée quand j'ai vu cet écart. À confirmer à l'oreille, ou au prochain passage sur cette archive.
2. **RAR 7 p2 : 8 lignes MIDI sans fichier.** Cause non trouvée (§ 5).
3. **RAR 8 : 9 hats ouverts très courts** (t60 < 150 ms). Ils sont peut-être mal nommés, ou peut-être des hats ouverts très serrés : à trancher à l'oreille.
4. **Lecture du type par le chemin, sur RAR 7 (31,8 %).** Ce chiffre bas vient de ma propre lecture du chemin, pas de la table :
   - elle ne connaît pas les pistes de kit ;
   - elle ne reconnaît pas « phrases » ou « mots parlés » comme des voix ;
   - elle prend pour des boucles les notes seules rangées dans un dossier de kit qui porte un tempo.
   
   Sur ces cas, la table a raison. Seul le cas n° 1 reste douteux.
5. **Style lu par le chemin :** 11 sons techno lus « drift-phonk » dans rar5, et 40 sons dubstep lus « funk » dans rar8. Ma règle de lecture est grossière (« drift », « funk » dans un nom), donc ce n'est pas une preuve contre la table.
