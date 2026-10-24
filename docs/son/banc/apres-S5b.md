# Banc de mesure du son : après S5b (le Drop au-dessus de sa Montée, la Montée qui monte)

Correctif de S5 après le test d'écoute A/B de Benjamin : en techno, house et jump up, la fin de la Montée sonnait plus fort que le Drop qui la suit (sonie court terme 3 s : techno −9,0 → −10,0, house −8,8 → −10,0, jump up −8,8 → −11,3), et l'Intro de la trap restait autour de −29 LUFS court terme. Au banc, 24 Montées sur 25 finissaient au-dessus de leur Drop après master : la fin d'une Montée n'a ni kick ni basse, le limiteur la laisse entière quand il retire 2 à 4 LU au Drop, et S5 y faisait monter le volume général au niveau du Drop, le souffle de +3,9 dB et la batterie de +3 dB.

Ce qui change (générateur seulement, le moteur est inchangé) :
- **Une Montée de club vidée puis remplie** : le kick et la basse quittent toute sa seconde moitié (ses 4 dernières mesures, au lieu de 2 ; une Montée qui n'aurait plus rien à jouer les garde sur ses 2 premières) ; le 303 des styles acid joue jusqu'au temps de silence (il porte leur montée). Comme les vrais builds (techno, tech house, trance) : on vide, puis le roulement, le souffle et le filtre font monter.
- **Volume général** : une Montée de club redescend doucement de 2,5 dB sous son haut pendant sa première moitié (4 en acid house et acid techno, 3 en trance), puis remonte sur ses 4 dernières mesures jusqu'à −2 dB (au lieu de 0, le niveau du Drop) ; en bass music (la Montée sans kick ni basse du tout) de −5,5 à −3,5 dB ; chaque style ajuste ce niveau (−4 à +2 dB, mesuré au banc : `WizardMix.MONTEE_TRIM_DB`), jamais au-dessus du Drop. Le temps de silence avant le Drop est gardé.
- **Souffle** : +1 dB en haut de Montée (au lieu de +3,9), vélocité 0,5 (0,6), filtre à 1,6 kHz en haut (2,1) ; l'impact du Drop inchangé (filtre grand ouvert, vélocité 1).
- **Roulement** : il accélère sans grossir. Un pas de 2, 3 ou 4 coups est joué plus léger pour peser un seul coup ; son coup le plus fort suit le temps fort du Drop du style (une mesure de roulement pèse au plus deux mesures de sa caisse claire ou de son clap) ; la batterie ne monte plus que de +1 dB.
- **Intro du rap** : le hook entier et ses accords sur les charleys (trap, drill, phonk), au lieu de sa première phrase puis 4 mesures de charleys seuls ; kick, clap et 808 attendent le Refrain.

Méthode : `scripts/song-bench.sh --premaster --m4a`, 37 styles, sons intégrés. **Avant** : le code de S5 (d19c9ab) rejoué et mesuré avec le `ps_measure` de S5b (les WAV sont octet pour octet ceux d'`apres-S5.md`). Deux passages complets du banc donnent les mêmes WAV pour les 19 styles que la seconde étape (la Montée de club) ne touche pas (empreintes plus bas). JSON : `apres-S5b.json` (la référence de la CI).

Nouvelles mesures (`ps_measure`, `report.py`) :
- **Drop − fin de Montée** (`drop_minus_montee_end`) : pour chaque Drop qui suit une Montée, la sonie de ses 4 premières mesures moins celle des 2 dernières mesures de la Montée, sans son temps de silence (environ 3 s chacune, la fenêtre court terme), après master ; un faux drop entre les deux (riddim) est sauté. **Bloquant à +1 LU**, +1,5 visé.
- **La montée d'une Montée** (`montee_rise`, de S5) : listée à **+2 LU** au lieu de +3, **sans bloquer** : la mesure manque à 170 BPM et plus (4 mesures plus courtes que ses deux fenêtres de 3 s : toute la drum and bass), et elle varie de 2 à 3 LU entre styles d'une même famille (house +2,9 à +4,9) ; le critère qui protège le Drop, lui, bloque.
- **Intro − Drop / Refrain** (`intro_minus_main`) : l'Intro du début entière (sans l'outro, qui rejoue sa scène) contre les sections Drop ou Refrain ; listée, non bloquante (≥ −8 LU visé).

## Résumé

| Mesure (médiane) | Avant (S5) | Après S5b | Cible |
|---|---|---|---|
| Drop − fin de Montée, le plus petit du morceau (LU) | −2,0 | +2,3 | ≥ +1 (+1,5 visé) |
| Drop − fin de Montée avant master (LU) | −1,2 | +5,0 | – |
| Fin de Montée (LUFS, 2 dernières mesures) | −6,9 | −11,0 | – |
| Début du Drop (LUFS, 4 premières mesures) | −8,7 | −8,5 | – |
| Montée sur ses 4 dernières mesures, exporté (LU) | +2,4 | +3,6 | ≥ +2 (listé) |
| Intro − Drop / Refrain (LU) | −2,9 | −2,9 | ≥ −8 |
| Sonie de la partie la plus forte (LUFS) | −8,2 | −8,5 | cible ±1 |
| Écart à la cible (LU) | −0,1 | −0,1 | ±1 |
| LUFS-I (morceau) | −9,7 | −10,0 | – |
| LUFS des sections Drop / Refrain | −8,7 | −8,6 | – |
| Perte dans la chaîne (LU) | 2,4 | 3,0 | ≤ 3 |
| Gain de la cible (dB) | 6,6 | 7,3 | – |
| PLR (dB) | 8,6 | 8,8 | ≥ 6 |
| True peak (dBTP) | −1,2 | −1,2 | ≤ −1,0 |
| M4A décodé, après la vérification (dBTP) | −1,8 (`apres-S5.md`) | −1,7 | ≤ −1,0 |
| Drop − Break exporté (LU) | +4,4 | +4,3 | ≥ +4 |
| Drop − Break avant master (LU) | +6,4 | +6,4 | ≥ +4 |
| Drop − Groove exporté (LU) | +1,4 | +1,8 | ≥ +2 |
| LRA (LU) | 4,2 | 5,2 | 3 à 9 |
| Côté/centre (dB) | −16,9 | −16,9 | −15 à −9 |
| Grave 60–250 Hz au Drop (dB rel.) | −3,8 | −3,8 | ≥ −6 |
| Air > 6 kHz (dB rel.) | −17,3 | −17,4 | −20 à −16 |

La montée des Montées : médiane sur les 17 styles où elle se mesure (moins de 170 BPM) ; la plus petite d'un style : +2,0.

| Critère | Avant (S5) | Après S5b |
|---|---|---|
| Drop − fin de Montée ≥ +1 LU, chaque Drop (bloquant) | 1 sur 25 | **25 sur 25** |
| Drop − fin de Montée ≥ +1,5 LU, chaque Drop | 1 sur 25 | 25 sur 25 |
| Chaque Montée à +2 LU sur ses 4 dernières mesures (exporté, 17 styles mesurables) | 9 sur 17 | 16 sur 17 (acid techno : +1,9 / +2,1) |
| Intro à moins de 8 LU sous le Drop ou le Refrain | 33 sur 37 | 34 sur 37 |
| Sonie de la partie la plus forte à ±1 LU de la cible, ou plafonnée (bloquant) | 37 sur 37 | 37 sur 37 |
| … sans plafonnement | 36 sur 37 | 32 sur 37 |
| Perte ≤ 3 LU (+0,2) (bloquant) | 37 sur 37 | 37 sur 37 |
| True peak ≤ −1 dBTP, M4A décodé ≤ −1 dBTP (bloquants) | 37 sur 37 | 37 sur 37 |
| Drop − Break ≥ +4 LU exporté | 28 sur 37 | 28 sur 37 |
| Drop − Groove ≥ +2 LU exporté | 9 sur 37 | 17 sur 37 |

## Ce que ça dit

- **Le Drop tombe au-dessus de sa Montée, partout.** Drop − fin de Montée : −2,0 → **+2,3 LU** en médiane (+1,8 à +6,5 ; avant master +5,0). Les trois styles du test A/B : techno −1,3 → **+2,9**, house −1,2 → **+2,2**, jump up −2,5 → **+1,9**. Le Drop n'a pas bougé (ses 4 premières mesures : −8,7 → −8,5 LUFS en médiane), c'est la fin de la Montée qui descend (−6,9 → −11,0 LUFS) : le souffle et le roulement ne passent plus au-dessus de ce que le limiteur laisse au Drop.
- **Et la Montée monte.** Sur ses 4 dernières mesures : +2,4 → **+3,6 LU** en médiane (+2,0 à +5,4 par style ; techno +2,7, house +3,0, trance +2,1, tech house +3,8). Elle part plus bas (son kick et sa basse sortis, le volume général 2,5 dB sous son haut) et c'est le roulement qui accélère, le souffle qui s'ouvre et le volume qui revient qui la font monter, sans jamais repasser au-dessus du Drop. En bass music (sa Montée n'a ni kick ni basse), +3 à +4 LU (dubstep, riddim ; à 174 BPM, ses 4 mesures sont trop courtes pour cette mesure).
- **Les Intros de rap** : trap −10,2 → **−6,3 LU** sous son Refrain (−18,9 → −14,9 LUFS sur l'Intro entière), drill −16,5 → −9,5, phonk −17,1 → −8,4. Lo-fi reste à −10,2 (son Intro de 4 mesures, piano et charleys, inchangée).
- **La sonie** : la partie la plus forte passe de −8,2 à −8,5 LUFS en médiane et la perte de 2,4 à 3,0 LU, sans que le Drop ne bouge (sections Drop −8,7 → −8,6) : avant, les fins de Montée (fortes et que le limiteur laissait entières) faisaient partie des 20 % les plus forts du morceau et allégeaient la perte lue ; maintenant cette partie est le Drop seul, plafonné à 3 LU dans 19 styles. Cinq styles de bass music et le hard techno restent 1,2 à 2,3 LU sous leur cible, plafonnés (bloquant respecté).
- **Le reste tient** : Drop − Break exporté +4,4 → +4,3 (28 styles sur 37 au-dessus de +4, comme avant ; deep house passe à +4,0, tech house remonte à +4,1), Drop − Groove +1,4 → +1,8 (17 sur 37, 9 avant), LRA 4,2 → 5,2 (plus de contraste), true peak, M4A décodé, largeur, grave et air inchangés.

## Ce qui n'est pas atteint

- **Les Intros de drill, phonk et lo-fi** restent 8,4 à 10,2 LU sous leur Refrain (non bloquant) : le Refrain y apporte kick, caisse et 808, très forts dans ces genres.
- **Amapiano** (+6,5) finit sa Montée bien sous le Drop, même remontée au niveau du Drop sur le volume général (+2 dB, le maximum) : sa Montée est très dépouillée. Acid techno monte de +1,9 sur sa première Montée.
- **La montée des Montées à 170 BPM et plus** (drum and bass, jump up, neurofunk, gabber) ne se mesure pas : ses 4 dernières mesures (5,5 s) sont plus courtes que les deux fenêtres de 3 s.

## Par style

Drop − fin de Montée : une valeur par Drop, exporté, puis avant master ; Montée +4 mesures : sa montée sur ses 4 dernières mesures (exporté ; – à 170 BPM et plus, où la mesure ne tient pas).

| Style | Drop − fin de Montée avant | après | avant master (après) | Fin de Montée avant → après (LUFS) | Montée +4 mesures après | Intro − Drop avant → après |
|---|---|---|---|---|---|---|
| acid_house | −0,9 / −0,8 | +1,9 / +1,8 | +3,9 / +3,8 | −7,2 / −7,3 → −9,8 / −9,8 | +2,2 / +2,0 | −1,4 → −1,4 |
| acid_techno | −0,8 / −0,8 | +2,1 / +2,1 | +3,6 / +3,6 | −6,8 / −6,9 → −9,6 / −9,6 | +1,9 / +2,1 | +0,0 → +0,1 |
| acidcore | +3,8 / +3,9 / +3,9 | +2,4 / +2,3 / +2,3 | +4,8 / +4,8 / +4,8 | −11,9 / −12,1 / −12,1 → −10,5 / −10,5 / −10,5 | – | +0,3 → +0,3 |
| afro_house | −1,4 / −1,3 | +3,2 / +3,2 | +4,2 / +4,3 | −7,9 / −8,0 → −12,2 / −12,2 | +4,4 / +4,9 | −1,5 → −1,3 |
| afrobeats | – | – | – | – | – | −7,9 → −7,9 |
| amapiano | +0,2 / +0,5 | +6,5 / +6,6 | +7,4 / +7,5 | −9,6 / −9,9 → −15,8 / −16,0 | +2,9 / +3,2 | −4,3 → −4,3 |
| boom_bap | – | – | – | – | – | −5,4 → −5,4 |
| deep_house | −0,8 / −0,6 | +3,8 / +4,1 | +4,8 / +5,3 | −8,5 / −8,6 → −13,0 / −13,1 | +4,3 / +4,7 | −2,8 → −2,6 |
| detroit | −2,6 / −2,7 | +2,5 / +2,5 | +4,5 / +4,5 | −6,0 / −6,0 → −10,9 / −10,9 | +3,6 / +3,7 | −0,5 → −0,3 |
| disco | −2,0 / −1,7 | +2,5 / +2,7 | +3,6 / +3,8 | −7,7 / −7,8 → −11,8 / −11,8 | +3,9 / +3,8 | −3,3 → −3,0 |
| dnb | −2,4 / −2,3 / −2,4 | +2,2 / +2,4 / +2,4 | +5,1 / +5,4 / +5,6 | −6,3 / −6,4 / −6,3 → −10,9 / −11,0 / −11,0 | – | −2,8 → −2,9 |
| drill | – | – | – | – | – | −16,5 → −9,5 |
| dubstep | −2,6 / −2,5 / −2,4 | +2,2 / +2,2 / +2,4 | +5,0 / +5,3 / +5,6 | −6,3 / −6,3 / −6,3 → −10,9 / −10,9 / −11,1 | +3,2 / +3,2 / +3,0 | −3,1 → −3,1 |
| gabber | −2,0 / −2,0 / −1,9 | +2,1 / +2,2 / +2,3 | +4,4 / +5,2 / +5,2 | −6,2 / −6,1 / −6,1 → −10,2 / −10,2 / −10,2 | – | −0,6 → −0,6 |
| grime | – | – | – | – | – | −5,7 → −3,9 |
| hard_techno | −2,4 / −1,6 / −1,6 | +2,4 / +3,1 / +3,1 | +5,5 / +6,4 / +6,4 | −7,0 / −7,0 / −7,1 → −11,7 / −11,7 / −11,7 | – | −3,8 → −3,8 |
| house | −1,2 / −1,3 | +2,5 / +2,2 | +5,3 / +5,3 | −7,2 / −7,1 → −10,6 / −10,5 | +2,9 / +3,1 | −0,9 → −0,9 |
| industrial_techno | −0,8 / −0,7 | +3,0 / +2,9 | +3,6 / +3,6 | −6,9 / −7,0 → −10,6 / −10,6 | +2,2 / +2,2 | −0,4 → −0,3 |
| jersey_club | – | – | – | – | – | −4,6 → −5,2 |
| jump_up | −2,5 / −2,5 / −2,5 | +1,9 / +2,0 / +2,2 | +5,1 / +5,2 / +5,3 | −6,2 / −6,2 / −6,1 → −10,6 / −10,7 / −10,8 | – | −4,7 → −4,7 |
| jungle | −2,3 / −1,5 / −2,4 | +3,5 / +4,0 / +3,4 | +6,3 / +7,0 / +6,4 | −5,8 / −6,5 / −5,9 → −11,5 / −12,3 / −11,5 | – | −3,5 → −3,5 |
| liquid_dnb | −2,4 / −2,2 / −2,2 | +2,0 / +2,1 / +2,1 | +5,1 / +5,2 / +5,0 | −6,2 / −6,3 / −6,3 → −10,5 / −10,5 / −10,5 | – | −3,1 → −3,1 |
| lofi | – | – | – | – | – | −10,2 → −10,2 |
| minimal | −2,2 / −2,0 | +2,8 / +3,0 | +6,0 / +6,3 | −6,5 / −6,6 → −11,5 / −11,6 | +5,1 / +5,6 | −1,4 → −1,5 |
| neurofunk | −3,6 / −3,2 / −3,1 | +1,9 / +2,3 / +2,3 | +4,6 / +5,5 / +5,5 | −5,6 / −5,6 / −5,6 → −11,0 / −11,0 / −11,0 | – | −2,2 → −2,2 |
| phonk | – | – | – | – | – | −17,1 → −8,4 |
| pop | – | – | – | – | – | −3,2 → −3,2 |
| reggaeton | – | – | – | – | – | −2,9 → −2,6 |
| retro_techno | −2,6 / −2,6 | +2,2 / +2,2 | +5,5 / +5,5 | −6,0 / −6,0 → −10,5 / −10,5 | +3,6 / +3,6 | −0,8 → −0,7 |
| riddim | −4,1 / −4,0 / −4,1 | +2,1 / +2,3 / +2,2 | +5,8 / +5,6 / +5,8 | −5,8 / −5,8 / −5,8 → −11,9 / −11,9 / −11,9 | +4,0 / +4,1 / +4,1 | −4,0 → −4,0 |
| speed_garage | −2,4 / −2,5 | +2,5 / +2,3 | +5,6 / +5,7 | −6,4 / −6,4 → −10,6 / −10,6 | +4,3 / +4,3 | −2,5 → −2,2 |
| synthwave | – | – | – | – | – | −1,4 → −1,3 |
| tech_house | −1,8 / −1,6 | +3,0 / +3,0 | +6,2 / +6,4 | −7,1 / −7,1 → −11,9 / −11,9 | +3,8 / +3,8 | −3,1 → −3,1 |
| techno | −1,3 / −1,3 | +2,9 / +3,1 | +5,8 / +6,0 | −7,1 / −7,0 → −11,3 / −11,4 | +2,8 / +2,6 | −2,0 → −2,1 |
| trance | −0,9 / −0,8 | +2,0 / +2,1 | +5,0 / +5,1 | −7,5 / −7,5 → −10,5 / −10,5 | +2,2 / +2,1 | −2,4 → −2,4 |
| trap | – | – | – | – | – | −10,2 → −6,3 |
| uk_garage | – | – | – | – | – | −1,4 → −1,4 |


## Le banc complet (exporté)

## Critères (docs/plan/son.md § 2.3 et étape 2)

| Critère | Morceaux conformes | Hors critère |
|---|---|---|
| True peak ≤ −1,0 dBTP (bloquant) | 37 sur 37 | – |
| True peak du M4A décodé ≤ −1,0 dBTP (bloquant) | 37 sur 37 | – |
| Sonie à ±1 LU de la cible du style (le Drop depuis S5), sans plafonnement | 32 sur 37 | dubstep (−1,3, plafonné), hard_techno (−2,3, plafonné), jump_up (−1,2, plafonné), neurofunk (−1,2, plafonné), riddim (−2,2, plafonné) |
| Sonie à ±1 LU de la cible, ou plafonnée (perte au plafond : 3 LU au Drop, 6 avant S5) (bloquant) | 37 sur 37 | – |
| Perte dans la chaîne sous son plafond (bloquant) | 37 sur 37 | – |
| Continu ≤ 0,001 (bloquant) | 37 sur 37 | – |
| Aucun NaN (bloquant) | 37 sur 37 | – |
| Aucun échantillon écrêté (≥ 0,999) | 37 sur 37 | – |
| PLR ≥ 6 dB | 37 sur 37 | – |
| Facteur de crête du Drop ≥ 6 dB | 37 sur 37 | – |
| Drop − Break ≥ +4 LU (exporté ; sans Break : –) | 28 sur 37 | acid_house (+2,3), acid_techno (+3,0), acidcore (+0,5), deep_house (+4,0), dubstep (+3,6), gabber (+2,7), hard_techno (+2,3), minimal (+2,8), trance (+1,5) |
| Drop − Groove, Couplet ou Montée ≥ +2 LU (exporté) | 17 sur 37 | acid_house (+0,5), acid_techno (+0,9), acidcore (+0,7), afrobeats (+0,9), boom_bap (+1,8), detroit (+0,9), disco (+1,8), house (+0,6), industrial_techno (+1,7), jersey_club (+1,3), jungle (+1,2), phonk (+0,8), reggaeton (+1,1), retro_techno (+1,2), synthwave (+1,0), tech_house (+1,2), techno (+1,6), trance (+1,9), trap (+1,5), uk_garage (+1,0) |
| Chaque Montée monte d'au moins +2 LU sur ses 4 dernières mesures (exporté ; sans Montée : –) | 36 sur 37 | acid_techno (+1,9 / +2,1) |
| Chaque Drop au-dessus de la fin de sa Montée : Drop − fin de Montée ≥ +1 LU (exporté ; sans Montée : –) (bloquant) | 37 sur 37 | – |
| Intro à moins de 8 LU sous le Drop ou le Refrain (exporté) | 34 sur 37 | drill (−9,5), lofi (−10,2), phonk (−8,4) |

Plafonné : la chaîne retirait déjà son plafond (écrêteur et limiteur) au gain trouvé : 3 LU sur la partie la plus forte depuis S5 (`MasterChain::kMaxLossDropDb`, le Drop), 6 LU sur le morceau avant (`kMaxLossDb`). Pousser plus écraserait sans gagner de sonie : c'est aux sons et au mix d'être plus denses.

## Par style

Gain : ce que la cible de sonie ajoute avant l'écrêteur ; Perte : ce que l'écrêteur et le limiteur retirent (LU).

| Style | LUFS-I | Cible | Gain | Perte | Drop | TP | PLR | LRA | Air | Pente | Côté/centre | Corr. | Corr. < 120 | Drop − avant | Drop − Break |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| acid_house | −9,1 (−0,1) | −8,0 (Drop) | +7,4 | 2,7 | −8,1 (+0,2) | −1,17 | 7,9 (+0,1) | 2,4 (+0,3) | −17,6 | −4,62 | −24,4 (+0,1) | 0,99 | 1,000 | 0,5 (−0,2) | 2,3 (−0,2) |
| acid_techno | −8,3 | −7,5 (Drop) | +7,8 | 2,5 | −7,8 (+0,1) | −1,18 | 7,1 | 3,1 (+0,6) | −17,8 | −4,71 | −31,2 | 1,00 | 1,000 | 0,9 (−0,1) | 3,0 (−0,1) |
| acidcore | −8,6 (−0,1) | −7,5 (Drop) | +2,6 | 3,0 | −8,2 | −1,16 | 7,4 (+0,1) | 2,5 (+1,3) | −15,0 | −4,84 | −37,2 | 1,00 | 1,000 | 0,7 | 0,5 |
| afro_house | −10,5 (+0,1) | −9,0 (Drop) | +5,8 | 0,8 | −9,2 (+0,4) | −1,19 | 9,3 (−0,1) | 5,0 (+1,1) | −18,6 (−0,1) | −5,55 | −17,0 (+0,1) | 0,96 | 0,999 | 2,1 (−0,3) | 4,8 (−0,2) |
| afrobeats | −10,2 | −9,0 (Drop) | +5,8 | 3,0 | −9,4 | −1,13 | 9,1 | 5,0 | −24,1 | −5,97 | −16,4 | 0,95 | 1,000 | 0,9 | – |
| amapiano | −12,0 (−0,3) | −9,0 (Drop) | +5,8 | 0,7 | −9,4 | −1,19 | 10,8 (+0,3) | 9,4 (+1,9) | −19,2 | −6,28 | −19,9 | 0,98 | 1,000 | 5,2 | 11,3 |
| boom_bap | −11,7 | −10,0 (Drop) | +3,1 | 1,6 | −10,4 | −1,18 | 10,5 | 5,3 | −20,0 | −5,41 | −19,3 | 0,98 | 1,000 | 1,8 | – |
| deep_house | −10,8 (−0,1) | −9,0 (Drop) | +4,9 | 0,9 | −9,1 (+0,2) | −1,19 | 9,7 (+0,1) | 4,4 (+0,4) | −18,4 (−0,1) | −5,95 | −16,8 (+0,1) | 0,96 | 0,999 | 2,4 (−0,2) | 4,0 (−0,4) |
| detroit | −9,1 (+0,1) | −8,0 (Drop) | +5,5 | 1,6 | −8,3 (+0,3) | −1,19 | 8,0 (−0,1) | 5,2 (+2,3) | −16,4 (+0,1) | −3,88 | −19,9 (+0,2) | 0,98 | 1,000 | 0,9 (−0,4) | 5,0 (−0,6) |
| disco | −11,1 | −9,0 (Drop) | +3,8 | 1,1 | −9,3 (+0,3) | −1,18 | 9,9 | 5,4 (+1,2) | −17,2 (+0,1) | −5,54 | −14,3 (+0,1) | 0,93 | 1,000 | 1,8 (−0,2) | 5,2 (−0,3) |
| dnb | −10,2 (−0,4) | −7,5 (Drop) | +7,3 | 3,0 | −8,6 | −1,19 | 9,0 (+0,4) | 5,4 (+0,3) | −15,6 | −4,91 | −12,2 | 0,89 | 1,000 | 5,3 (+4,8) | 4,5 (+0,2) |
| drill | −10,2 (−0,5) | −8,5 (Drop) | +4,5 | 0,4 | −8,7 | −1,19 | 9,0 (+0,5) | 10,0 (−2,9) | −20,1 | −5,22 | −20,9 (−0,2) | 0,98 | 1,000 | 2,1 (+0,1) | – |
| dubstep | −10,3 (−0,6) | −7,5 (Drop) | +8,6 | 3,0 | −8,9 | −1,19 | 9,2 (+0,6) | 6,1 | −17,4 | −4,79 | −10,7 | 0,84 | 1,000 | 5,1 (+5,2) | 3,6 |
| gabber | −8,8 (−0,2) | −7,0 (Drop) | +8,9 | 3,0 | −8,0 | −1,18 | 7,6 (+0,2) | 3,3 (+0,2) | −16,5 | −4,88 | −12,4 | 0,89 | 0,999 | 4,9 (+4,4) | 2,7 |
| grime | −10,3 (−0,1) | −8,5 (Drop) | +4,1 | 1,0 | −8,9 | −1,17 | 9,1 (+0,1) | 5,5 (−1,8) | −13,9 (+0,1) | −3,65 | −15,2 (−0,2) | 0,94 | 1,000 | 2,2 (−0,1) | – |
| hard_techno | −10,1 (−0,3) | −6,0 (Drop) | +9,1 | 3,0 | −8,6 | −1,19 | 8,9 (+0,3) | 4,3 (−0,1) | −14,5 | −4,61 | −10,1 | 0,82 | 0,998 | 2,6 | 2,3 |
| house | −9,3 (−0,2) | −8,0 (Drop) | +7,6 | 3,0 | −8,3 (+0,1) | −1,17 | 8,1 (+0,2) | 3,9 (+1,8) | −16,5 | −5,52 | −20,0 | 0,98 | 1,000 | 0,6 (−0,1) | 4,3 (−0,2) |
| industrial_techno | −8,6 (−0,1) | −7,5 (Drop) | +4,4 | 0,3 | −7,7 (+0,1) | −1,19 | 7,4 (+0,1) | 4,4 (+1,5) | −18,1 | −5,26 | −16,9 | 0,96 | 0,997 | 1,7 | 6,5 |
| jersey_club | −9,8 (−0,1) | −8,5 (Drop) | +6,1 | 2,0 | −8,6 (+0,1) | −1,18 | 8,6 (+0,1) | 7,1 (+0,9) | −19,2 | −5,03 | −17,4 | 0,96 | 1,000 | 1,3 (−0,1) | – |
| jump_up | −10,6 (−0,6) | −7,5 (Drop) | +8,4 | 3,0 | −8,8 | −1,19 | 9,4 (+0,6) | 5,9 (−0,1) | −13,5 | −4,03 | −8,3 | 0,74 | 0,999 | 4,6 (+4,7) | 4,8 (+0,2) |
| jungle | −9,8 (−0,4) | −7,5 (Drop) | +7,9 | 3,0 | −8,2 | −1,18 | 8,6 (+0,4) | 5,8 | −14,3 | −4,36 | −14,9 | 0,94 | 1,000 | 1,2 (+0,1) | 6,4 (+0,1) |
| liquid_dnb | −10,3 (−0,4) | −8,0 (Drop) | +7,5 | 3,0 | −8,6 | −1,18 | 9,2 (+0,4) | 6,8 (+0,4) | −15,3 | −4,97 | −14,4 | 0,93 | 1,000 | 5,2 (+4,5) | 5,9 |
| lofi | −13,9 | −12,0 (Drop) | −1,1 | 0,1 | −12,3 | −1,19 | 12,7 | 9,3 | −22,8 | −5,83 | −25,6 | 0,99 | 1,000 | 2,7 | – |
| minimal | −9,9 (−0,3) | −8,0 (Drop) | +7,9 | 3,0 | −8,7 | −1,18 | 8,7 (+0,3) | 3,2 (+0,3) | −21,7 (−0,1) | −6,31 | −11,3 | 0,86 | 0,997 | 2,0 (+0,1) | 2,8 (+0,1) |
| neurofunk | −10,3 (−0,4) | −7,5 (Drop) | +9,4 | 3,0 | −9,0 | −1,19 | 9,1 (+0,4) | 8,7 (+1,0) | −13,6 | −4,00 | −13,3 | 0,91 | 0,999 | 5,9 (+6,1) | 6,1 |
| phonk | −8,2 (−0,5) | −7,0 (Drop) | +8,1 | 2,8 | −7,1 | −1,16 | 7,0 (+0,5) | 8,8 (−2,3) | −19,7 | −4,91 | −22,7 | 0,99 | 1,000 | 0,8 (+0,1) | – |
| pop | −10,3 (−0,1) | −9,0 (Drop) | +3,4 | 1,1 | −9,2 | −1,18 | 9,1 (+0,1) | 3,3 | −19,5 | −5,52 | −19,3 | 0,98 | 1,000 | 2,3 | – |
| reggaeton | −9,9 (+0,1) | −9,0 (Drop) | +4,8 | 2,0 | −9,2 (+0,1) | −1,18 | 8,8 (−0,1) | 3,2 (−0,4) | −17,4 | −4,28 | −15,4 (+0,1) | 0,94 | 1,000 | 1,1 (−0,2) | – |
| retro_techno | −9,2 | −8,0 (Drop) | +8,0 | 3,0 | −8,3 (+0,2) | −1,17 | 8,0 (−0,1) | 5,7 (+2,4) | −16,4 (+0,2) | −4,37 | −19,5 (+0,2) | 0,98 | 0,999 | 1,2 (−0,7) | 7,2 (−1,1) |
| riddim | −11,3 (−0,6) | −7,5 (Drop) | +11,1 | 3,0 | −9,9 (+0,1) | −1,18 | 10,2 (+0,6) | 7,3 (−0,7) | −12,9 | −3,58 | −13,0 | 0,91 | 0,999 | 6,0 (+7,2) | 4,0 (−0,2) |
| speed_garage | −10,0 (+0,1) | −8,0 (Drop) | +7,6 | 3,0 | −8,3 (+0,5) | −1,18 | 8,8 (−0,1) | 6,6 (+1,4) | −15,7 (+0,1) | −5,12 | −20,0 (+0,1) | 0,98 | 1,000 | 2,2 (−1,1) | 6,8 (−1,6) |
| synthwave | −8,9 | −8,0 (Drop) | +6,2 | 2,4 | −8,2 | −1,17 | 7,7 | 2,1 (−0,1) | −19,0 | −4,93 | −15,4 | 0,94 | 1,000 | 1,0 | – |
| tech_house | −10,6 (−0,5) | −8,0 (Drop) | +7,3 | 3,0 | −8,9 | −1,16 | 9,4 (+0,5) | 4,3 (+0,2) | −15,0 | −5,20 | −18,7 | 0,97 | 1,000 | 1,2 | 4,1 (+0,2) |
| techno | −9,7 (−0,2) | −7,5 (Drop) | +7,4 | 3,0 | −8,4 | −1,17 | 8,5 (+0,2) | 3,8 (+0,4) | −18,7 | −5,50 | −15,1 | 0,94 | 0,999 | 1,6 | 4,0 |
| trance | −9,7 (−0,2) | −8,0 (Drop) | +8,1 | 3,0 | −8,4 | −1,18 | 8,5 (+0,2) | 3,0 (+0,1) | −15,7 (+0,1) | −5,19 | −10,6 (+0,1) | 0,84 | 0,999 | 1,9 | 1,5 |
| trap | −10,0 (−0,2) | −8,5 (Drop) | +5,4 | 1,4 | −8,7 | −1,19 | 8,8 (+0,2) | 7,0 (−2,6) | −19,7 | −5,03 | −18,7 (−0,1) | 0,97 | 1,000 | 1,5 (+0,1) | – |
| uk_garage | −9,2 (−0,2) | −8,0 (Drop) | +7,8 | 3,0 | −8,5 | −1,18 | 8,0 (+0,2) | 3,4 (−0,1) | −14,0 | −5,05 | −18,9 | 0,97 | 1,000 | 1,0 (+0,1) | – |

## Montées, Drops et Intro (S5b)

Fin de Montée : la sonie de ses 2 dernières mesures (sans son temps de silence) ; Drop : celle de ses 4 premières mesures (un faux drop entre les deux est sauté) ; une valeur par Drop, exporté (avant master entre parenthèses). Bloquant : Drop − fin de Montée ≥ +1 LU. Intro − Drop : l'Intro du début entière contre les sections Drop (ou Refrain), à titre indicatif.

| Style | Fin de Montée (LUFS) | Début du Drop (LUFS) | Drop − fin de Montée | Intro − Drop |
|---|---|---|---|---|
| acid_house | −9,8 / −9,8 | −8,0 / −8,0 | +1,9 / +1,8 (+3,9 / +3,8) | −1,4 (−0,1) |
| acid_techno | −9,6 / −9,6 | −7,5 / −7,5 | +2,1 / +2,1 (+3,6 / +3,6) | +0,1 |
| acidcore | −10,5 / −10,5 / −10,5 | −8,1 / −8,1 / −8,2 | +2,4 / +2,3 / +2,3 (+4,8 / +4,8 / +4,8) | +0,3 |
| afro_house | −12,2 / −12,2 | −8,9 / −8,9 | +3,2 / +3,2 (+4,2 / +4,3) | −1,3 (+0,1) |
| afrobeats | – | – | – | −7,9 |
| amapiano | −15,8 / −16,0 | −9,4 / −9,3 | +6,5 / +6,6 (+7,4 / +7,5) | −4,3 |
| boom_bap | – | – | – | −5,4 |
| deep_house | −13,0 / −13,1 | −9,2 / −9,0 | +3,8 / +4,1 (+4,8 / +5,3) | −2,6 (+0,2) |
| detroit | −10,9 / −10,9 | −8,4 / −8,4 | +2,5 / +2,5 (+4,5 / +4,5) | −0,3 (+0,2) |
| disco | −11,8 / −11,8 | −9,3 / −9,1 | +2,5 / +2,7 (+3,6 / +3,8) | −3,0 (+0,3) |
| dnb | −10,9 / −11,0 / −11,0 | −8,7 / −8,6 / −8,6 | +2,2 / +2,4 / +2,4 (+5,1 / +5,4 / +5,6) | −2,9 (−0,1) |
| drill | – | – | – | −9,5 (+7,0) |
| dubstep | −10,9 / −10,9 / −11,1 | −8,8 / −8,7 / −8,7 | +2,2 / +2,2 / +2,4 (+5,0 / +5,3 / +5,6) | −3,1 |
| gabber | −10,2 / −10,2 / −10,2 | −8,1 / −8,0 / −7,9 | +2,1 / +2,2 / +2,3 (+4,4 / +5,2 / +5,2) | −0,6 |
| grime | – | – | – | −3,9 (+1,8) |
| hard_techno | −11,7 / −11,7 / −11,7 | −9,3 / −8,6 / −8,6 | +2,4 / +3,1 / +3,1 (+5,5 / +6,4 / +6,4) | −3,8 |
| house | −10,6 / −10,5 | −8,2 / −8,3 | +2,5 / +2,2 (+5,3 / +5,3) | −0,9 |
| industrial_techno | −10,6 / −10,6 | −7,6 / −7,6 | +3,0 / +2,9 (+3,6 / +3,6) | −0,3 |
| jersey_club | – | – | – | −5,2 (−0,6) |
| jump_up | −10,6 / −10,7 / −10,8 | −8,7 / −8,7 / −8,6 | +1,9 / +2,0 / +2,2 (+5,1 / +5,2 / +5,3) | −4,7 (−0,1) |
| jungle | −11,5 / −12,3 / −11,5 | −8,0 / −8,2 / −8,2 | +3,5 / +4,0 / +3,4 (+6,3 / +7,0 / +6,4) | −3,5 (−0,1) |
| liquid_dnb | −10,5 / −10,5 / −10,5 | −8,5 / −8,4 / −8,4 | +2,0 / +2,1 / +2,1 (+5,1 / +5,2 / +5,0) | −3,1 |
| lofi | – | – | – | −10,2 |
| minimal | −11,5 / −11,6 | −8,7 / −8,6 | +2,8 / +3,0 (+6,0 / +6,3) | −1,5 (−0,1) |
| neurofunk | −11,0 / −11,0 / −11,0 | −9,1 / −8,7 / −8,7 | +1,9 / +2,3 / +2,3 (+4,6 / +5,5 / +5,5) | −2,2 |
| phonk | – | – | – | −8,4 (+8,7) |
| pop | – | – | – | −3,2 |
| reggaeton | – | – | – | −2,6 (+0,3) |
| retro_techno | −10,5 / −10,5 | −8,3 / −8,4 | +2,2 / +2,2 (+5,5 / +5,5) | −0,7 (+0,2) |
| riddim | −11,9 / −11,9 / −11,9 | −9,8 / −9,7 / −9,7 | +2,1 / +2,3 / +2,2 (+5,8 / +5,6 / +5,8) | −4,0 |
| speed_garage | −10,6 / −10,6 | −8,1 / −8,3 | +2,5 / +2,3 (+5,6 / +5,7) | −2,2 (+0,3) |
| synthwave | – | – | – | −1,3 (+0,1) |
| tech_house | −11,9 / −11,9 | −8,9 / −8,8 | +3,0 / +3,0 (+6,2 / +6,4) | −3,1 |
| techno | −11,3 / −11,4 | −8,4 / −8,3 | +2,9 / +3,1 (+5,8 / +6,0) | −2,1 |
| trance | −10,5 / −10,5 | −8,5 / −8,5 | +2,0 / +2,1 (+5,0 / +5,1) | −2,4 |
| trap | – | – | – | −6,3 (+3,9) |
| uk_garage | – | – | – | −1,4 |

## Bandes du Drop (dB relatifs à l'énergie totale)

| Style | Sub < 60 | Grave 60–250 | Bas-méd. 250–500 | Médium 0,5–2 k | Haut-méd. 2–6 k | Air > 6 k |
|---|---|---|---|---|---|---|
| acid_house | −6,1 | −3,0 | −13,8 | −8,9 | −12,2 | −17,6 |
| acid_techno | −4,8 | −2,9 | −14,1 | −12,4 | −13,6 | −17,8 |
| acidcore | −10,6 | −1,9 | −10,4 | −11,6 | −12,6 | −15,0 |
| afro_house | −7,2 | −1,7 | −10,9 | −16,3 | −18,9 | −18,6 |
| afrobeats | −2,9 | −4,7 | −10,2 | −14,4 | −19,0 | −24,1 |
| amapiano | −14,7 | −0,9 | −11,4 | −13,1 | −19,7 | −19,2 |
| boom_bap | −3,2 | −3,8 | −12,3 | −18,0 | −17,4 | −20,0 |
| deep_house | −3,8 | −4,0 | −12,1 | −10,2 | −21,8 | −18,4 |
| detroit | −4,0 | −4,5 | −13,0 | −9,5 | −12,7 | −16,4 |
| disco | −8,9 | −3,7 | −5,9 | −10,7 | −16,1 | −17,2 |
| dnb | −5,6 | −4,3 | −7,3 | −9,8 | −14,7 | −15,6 |
| drill | −4,0 | −3,0 | −13,9 | −14,6 | −18,6 | −20,1 |
| dubstep | −4,3 | −6,4 | −5,8 | −10,2 | −17,6 | −17,4 |
| gabber | −6,0 | −5,2 | −6,3 | −7,9 | −16,3 | −16,5 |
| grime | −8,6 | −3,1 | −12,3 | −7,8 | −9,9 | −13,9 |
| hard_techno | −9,0 | −5,0 | −5,6 | −6,7 | −16,4 | −14,5 |
| house | −5,4 | −3,4 | −8,5 | −11,7 | −18,1 | −16,5 |
| industrial_techno | −6,1 | −3,3 | −10,1 | −8,1 | −17,0 | −18,1 |
| jersey_club | −3,1 | −5,7 | −11,4 | −8,4 | −18,8 | −19,2 |
| jump_up | −6,5 | −7,1 | −5,9 | −6,6 | −12,7 | −13,5 |
| jungle | −5,4 | −3,2 | −10,2 | −13,8 | −12,3 | −14,3 |
| liquid_dnb | −4,6 | −3,7 | −9,8 | −12,4 | −15,7 | −15,3 |
| lofi | −3,6 | −3,0 | −13,7 | −21,1 | −19,4 | −22,8 |
| minimal | −4,3 | −3,9 | −8,3 | −12,3 | −22,5 | −21,7 |
| neurofunk | −3,9 | −5,8 | −7,5 | −12,8 | −13,4 | −13,6 |
| phonk | −2,7 | −5,0 | −13,0 | −11,6 | −17,7 | −19,7 |
| pop | −4,7 | −3,1 | −10,0 | −14,3 | −17,2 | −19,5 |
| reggaeton | −3,4 | −5,0 | −11,1 | −10,6 | −14,3 | −17,4 |
| retro_techno | −4,3 | −3,5 | −12,0 | −12,3 | −14,7 | −16,4 |
| riddim | −4,1 | −6,4 | −9,8 | −7,9 | −14,8 | −12,9 |
| speed_garage | −5,2 | −3,1 | −10,5 | −12,8 | −15,3 | −15,7 |
| synthwave | −5,9 | −3,4 | −10,5 | −8,2 | −14,5 | −19,0 |
| tech_house | −4,8 | −3,9 | −10,2 | −9,7 | −16,9 | −15,0 |
| techno | −4,3 | −3,8 | −9,1 | −12,2 | −18,9 | −18,7 |
| trance | −5,5 | −4,5 | −7,5 | −8,4 | −19,0 | −15,7 |
| trap | −2,3 | −5,3 | −15,0 | −12,3 | −18,3 | −19,7 |
| uk_garage | −6,5 | −3,5 | −10,8 | −7,6 | −16,1 | −14,0 |
| **médiane** | −4,8 | −3,8 | −10,4 | −11,6 | −16,4 | −17,4 |

## Largeur et mouvement (S4)

Côté/centre −15 à −9 dB, corrélation 0,4 à 0,85 (≥ 0,95 sous 120 Hz), lead dont le centroïde bouge d'au moins ±15 % d'une mesure à l'autre (sa piste seule, quand l'enregistrement la nomme), grave 60–250 Hz ≥ −6 dB en bass music.

| Style | Côté/centre | Corr. | Corr. < 120 | Lead ±% | Grave 60–250 |
|---|---|---|---|---|---|
| acid_house | −24,4 (+0,1) | 0,99 | 1,000 | – | −3,0 |
| acid_techno | −31,2 | 1,00 | 1,000 | – | −2,9 |
| acidcore | −37,2 | 1,00 | 1,000 | – | −1,9 |
| afro_house | −17,0 (+0,1) | 0,96 | 0,999 | 31,7 | −1,7 |
| afrobeats | −16,4 | 0,95 | 1,000 | 22,1 | −4,7 |
| amapiano | −19,9 | 0,98 | 1,000 | 26,6 | −0,9 |
| boom_bap | −19,3 | 0,98 | 1,000 | 21,1 | −3,8 |
| deep_house | −16,8 (+0,1) | 0,96 | 0,999 | 32,3 | −4,0 |
| detroit | −19,9 (+0,2) | 0,98 | 1,000 | 23,8 | −4,5 (−0,1) |
| disco | −14,3 (+0,1) | 0,93 | 1,000 | 34,5 | −3,7 |
| dnb | −12,2 | 0,89 | 1,000 | 18,3 | −4,3 |
| drill | −20,9 (−0,2) | 0,98 | 1,000 | 32,4 | −3,0 |
| dubstep | −10,7 | 0,84 | 1,000 | 17,5 | −6,4 |
| gabber | −12,4 | 0,89 | 0,999 | 13,9 | −5,2 |
| grime | −15,2 (−0,2) | 0,94 | 1,000 | 53,9 | −3,1 |
| hard_techno | −10,1 | 0,82 | 0,998 | 14,5 | −5,0 |
| house | −20,0 | 0,98 | 1,000 | 34,1 | −3,4 |
| industrial_techno | −16,9 | 0,96 | 0,997 | 14,5 | −3,3 |
| jersey_club | −17,4 | 0,96 | 1,000 | 8,9 | −5,7 (−0,1) |
| jump_up | −8,3 | 0,74 | 0,999 | 21,1 | −7,1 |
| jungle | −14,9 | 0,94 | 1,000 | 15,9 | −3,2 |
| liquid_dnb | −14,4 | 0,93 | 1,000 | 22,7 | −3,7 |
| lofi | −25,6 | 0,99 | 1,000 | 23,8 | −3,0 |
| minimal | −11,3 | 0,86 | 0,997 | – | −3,9 |
| neurofunk | −13,3 | 0,91 | 0,999 | 22,4 | −5,8 |
| phonk | −22,7 | 0,99 | 1,000 | 20,6 | −5,0 |
| pop | −19,3 | 0,98 | 1,000 | 23,0 | −3,1 |
| reggaeton | −15,4 (+0,1) | 0,94 | 1,000 | 29,7 | −5,0 |
| retro_techno | −19,5 (+0,2) | 0,98 | 0,999 | 37,7 | −3,5 (−0,1) |
| riddim | −13,0 | 0,91 | 0,999 | 0,1 | −6,4 |
| speed_garage | −20,0 (+0,1) | 0,98 | 1,000 | 7,6 | −3,1 (−0,1) |
| synthwave | −15,4 | 0,94 | 1,000 | 19,4 | −3,4 |
| tech_house | −18,7 | 0,97 | 1,000 | 5,1 | −3,9 (+0,1) |
| techno | −15,1 | 0,94 | 0,999 | 31,2 | −3,8 |
| trance | −10,6 (+0,1) | 0,84 | 0,999 | 14,1 | −4,5 (+0,3) |
| trap | −18,7 (−0,1) | 0,97 | 1,000 | 24,4 | −5,3 |
| uk_garage | −18,9 | 0,97 | 1,000 | 13,8 | −3,5 |

## Défauts

| Style | NaN | Écrêtés | DC | Fin (dBFS, 500 ms) | Durée (s) |
|---|---|---|---|---|---|
| acid_house | 0 | 0 | 0,0000 | −32,8 | 192,0 |
| acid_techno | 0 | 0 | 0,0000 | −35,6 | 306,1 |
| acidcore | 0 | 0 | 0,0000 | −33,0 | 280,2 |
| afro_house | 0 | 0 | 0,0000 | −39,2 | 188,8 |
| afrobeats | 0 | 0 | 0,0000 | −50,9 | 192,0 |
| amapiano | 0 | 0 | 0,0000 | −44,9 | 205,7 |
| boom_bap | 0 | 0 | 0,0000 | −37,4 | 167,0 |
| deep_house | 0 | 0 | 0,0000 | −42,5 | 192,0 |
| detroit | 0 | 0 | 0,0000 | −42,9 | 335,2 |
| disco | 0 | 0 | 0,0000 | −44,7 | 198,6 |
| dnb | 0 | 0 | 0,0000 | −33,8 | 297,9 |
| drill | 0 | 0 | 0,0000 | −42,3 | 175,8 |
| dubstep | 0 | 0 | 0,0000 | −41,5 | 301,7 |
| gabber | 0 | 0 | 0,0000 | −38,9 | 296,2 |
| grime | 0 | 0 | 0,0000 | −43,9 | 178,3 |
| hard_techno | 0 | 0 | 0,0000 | −34,3 | 358,4 |
| house | 0 | 0 | 0,0000 | −41,8 | 188,8 |
| industrial_techno | 0 | 0 | 0,0000 | −36,3 | 310,6 |
| jersey_club | 0 | 0 | 0,0000 | −42,2 | 178,3 |
| jump_up | 0 | 0 | 0,0000 | −32,4 | 297,9 |
| jungle | 0 | 0 | 0,0000 | −36,9 | 325,8 |
| liquid_dnb | 0 | 0 | 0,0000 | −33,3 | 297,9 |
| lofi | 0 | 0 | 0,0000 | −41,9 | 182,9 |
| minimal | 0 | 0 | 0,0000 | −42,1 | 330,0 |
| neurofunk | 0 | 0 | 0,0000 | −34,1 | 297,9 |
| phonk | 0 | 0 | 0,0000 | −42,0 | 192,0 |
| pop | 0 | 0 | 0,0000 | −42,7 | 206,9 |
| reggaeton | 0 | 0 | 0,0000 | −36,2 | 210,0 |
| retro_techno | 0 | 0 | 0,0000 | −37,4 | 330,0 |
| riddim | 0 | 0 | 0,0000 | −40,7 | 308,6 |
| speed_garage | 0 | 0 | 0,0000 | −43,6 | 177,2 |
| synthwave | 0 | 0 | 0,0000 | −39,8 | 192,0 |
| tech_house | 0 | 0 | 0,0001 | −42,3 | 182,9 |
| techno | 0 | 0 | 0,0000 | −40,0 | 320,0 |
| trance | 0 | 0 | 0,0000 | −43,2 | 306,1 |
| trap | 0 | 0 | 0,0000 | −43,2 | 178,3 |
| uk_garage | 0 | 0 | 0,0000 | −34,5 | 196,4 |

## Empreintes des rendus (SHA-256, WAV float 32 bits)

Deux passages du banc sur le même code donnent les mêmes octets.

```
036aa71366bd4217fffe531d7964f39822bc0abd37c01aac044f432bb81b9906  acid_house.wav
8cd6521c0e1419519638605556fe5863441c2d5181bededfc8a4cbfa1439b520  acid_techno.wav
42278ea5c8106b344954c2f97aa450b55d15b4a8f7838d9e095a593442af96f9  acidcore.wav
e5d0a137ab3c88d55c3e66d1d9a30b356eda56dbda21f1b8414debfe7660ae4e  afro_house.wav
40faa9fed144029e64a695d498705c51b3967066e7f4e7674e8b443e9e32b9d9  afrobeats.wav
0e4cc817975dee383c3dd28ca666b924751af00c2a6b858bc7bbe989ae252175  amapiano.wav
f92d17f703feb6ed10b674caa93bc25082f4adb1e9b54494ce02752380cd71f6  boom_bap.wav
0beafbb03e56bc47658607ca8d3b41c74c5c3c4412b70bcff9d0791f335e82b4  deep_house.wav
1b987bb10ba0ef2439ff94cc2ceb110dc80fc68e9aad83a89153a3239d82b572  detroit.wav
93ab2d1af7fcc7561eb388bc0f9d11ebd14ce32ce193eba485686a9fff9f4f48  disco.wav
75c876468dd5ca5e1066e3f9da0302a177d8c4b7592351ff3910b8da56234ac6  dnb.wav
d8f321e7394a3def85fb659df354ddcd4c8d282c241f40f47db561ca7e149d87  drill.wav
64f86dd308e4d695116de54ba93831cabe0aa1b64c555ede37b8ae231cd83828  dubstep.wav
0c275b5543ce90338a57e2ed14cb010462146a0c484d82a672255daa3caeb6c7  gabber.wav
a02d4f53823f9da47ceb1257da4d0231695cc513f1e84436e606f018a24b64c2  grime.wav
3609a3f24e8708855080ad1f15f77696bb0caad099b0d73546b1d3a6358d0787  hard_techno.wav
0d99642dd4c93f98d9e44159045ab53395172c5c4699cfa64ae5608635aacfda  house.wav
9fc5d6feb2b39dcfb2398880ba530a19e93d819cfeff491f8d8c1424e500f919  industrial_techno.wav
3b9675049429cb4c175f49674b8723aee403596c7c1e0ba97308e2b60efdd5f5  jersey_club.wav
611a3a4913968bccc193ecab1e992ef4c562dbb6771bfdb47f2466577c4f0a72  jump_up.wav
3d1443a0876825304e451f9d016202a860b5e98faa66a5d7a117d6abace0ecdc  jungle.wav
55c09322dca9bae3461b22bbcdf3ed87ba55339229d4417d76d6ac017ce79da2  liquid_dnb.wav
5ec4fe556523250c1631d1cf44d97928dcd66a3e587b94a12368a9d88673ac03  lofi.wav
ecef0b9609d2a223331aee398e997b50757202c6ec7c1c7c5d973b8624a0aaa7  minimal.wav
40c9b8397e3647c4709c2aff6efdf7ff86066e5354c4ca45e709c060a103ad91  neurofunk.wav
429a95d7a4674b0d83ac98f99301db5cba02628fec2821ca4c21aa72765b8f37  phonk.wav
e90ca7daee0b129dd47c0c387aa4ff411a8229edb58e2a9e1a75cc3a6d5b4403  pop.wav
87532b0ff3fb416f6758cd35c536b0531e2c2d0a5adc2e8c870b9f9eb2a7ee8d  reggaeton.wav
6694d4d9738504c666e090f600d0497a5f64897419b5eee2a3964564b71f4d46  retro_techno.wav
f6b913af12d9ca4d49fb5859c8f915380c89986dc04c136b26fb1d810c19786c  riddim.wav
a9112dc6a27da2a371995fc374517422507ab59e69e8d571d91d69738ad0be5d  speed_garage.wav
0116a9c90ebe54d0ae3ab5bd7df70bd0f85e22d6a70d62f6e5144e3d0c0b0413  synthwave.wav
cc71e638f870b5c11f519be94343cb08fa7657d3a4bd4bb276b4cecdab2dcda3  tech_house.wav
25229f4766dea28cb2f7fdf16cf892188ec8da7f277394725736d5458ef80884  techno.wav
203ca0fa56770046a05011b8e68b0b5d27f6dd7e9daa4c5a9f6e42668d3cc8fe  trance.wav
21b1b9bb1f42eaad74d2876d6ea2de8a558bee474208df22ce62b428bdb15cd4  trap.wav
a6fa6486eec4423d34a296ab5887d7499523e248980d8f7a5ba7ff2d7a22145a  uk_garage.wav
```
