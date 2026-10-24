# Banc de mesure du son : après S2 (un arrangement qui tombe)
## Ce que S2 change dans les morceaux (générateur seul)

- **Formes par famille** : techno et bass music de 4 min 30 à 6 min (intro DJ de 32 mesures, Drops de 32, Break de 16, un troisième Drop aux tempos rapides) ; house et le reste autour de 3 min ; forme « rap » (Refrain dès la mesure 9, Couplets de 16 sans accords, Pont) ; forme chanson avec Pré-refrain, Post-refrain et Pont, premier Refrain avant 45 s.
- **Montée** de 8 mesures avant chaque Drop (deux scènes de 4) : roulement de caisse claire noires → croches → doubles → 32es (rampe de vélocité du moteur), plafonné sous le Drop (forces du roulement ≤ 0,7, accords à 75 % de leur force, souffle plus doux), kick et basse coupés sur les 2 dernières mesures, un temps de silence total (le volume général du morceau à 0 sur ce temps, queues de réverbe comprises).
- **Drop en scènes de 8** : la 2e joue le hook doublé (`Version.Chorus2`), crash (cymbale du kit sur son pad quand aucun ride ne le prend), impact de souffle, sub drop (piste « Sub drop », sinus qui glisse de deux octaves), basse +1,5 dB par rapport au reste du morceau ; mute d'une ligne pendant 8 mesures en techno ; changement de basse au 2e Drop (déjà là).
- **Une couche de plus toutes les 8 mesures** dans les intros et Grooves longs, qui repartent dans l'outro ; la basse du Groove filtrée (passe-bas qui s'ouvre jusqu'au Drop).
- **Transitions** : descente de souffle au début du Break, faux drop en riddim, rembobinage (arrêt de bande sur le master) avant le 2e Drop en jungle.
- **Voix** (voix intégrée « Voix robot », piste « Voix ») : chops qui répondent au hook (house, deep, tech house, garages, jersey), stab toutes les 4 mesures (trap), shout au 2e temps du Drop (dnb, jump up), coup bitcrushé toutes les 2 mesures (phonk).

## S2 : le contraste des sections, avant et après la chaîne master

Le banc mesure le morceau exporté, donc après la cible de sonie, l'écrêteur et le limiteur. Pour séparer ce que fait l'arrangement (S2) de ce que fait le master (S5), les mêmes `.pscmd` sont aussi rendus avec `ps_render --no-limiter` (ni gain de cible, ni écrêteur, ni limiteur ; les effets du master restent) : c'est la colonne « avant master ». « Avant » : le code d'avant S2 (après S1) ; « après » : S2 complet, moteur S3 fusionné (kit v2 toujours, le générateur S3 n'est pas encore fait). Les variantes comptent avec leur section (« Drop 2 » avec le Drop, « Montée 2 » avec la Montée).

| Mesure (médiane, 37 styles) | Avant master : avant | Avant master : après | Exporté : avant | Exporté : après | Cible |
|---|---|---|---|---|---|
| Drop − Groove, Couplet ou Montée (LU) | −0,2 | +0,8 | −0,2 | −0,2 | ≥ +2 |
| Drop − Break (LU) | +1,4 | +3,4 | −0,2 | +1,0 | ≥ +4 |
| Drop − Montée (LU) | −1,3 | +0,8 | −2,1 | −0,1 | > 0 |
| LRA (LU) | 4,0 | 3,8 | 1,8 | 2,1 | 3 à 6 |
| Durée (s) | 167,6 | 206,9 | | | techno et bass music 300–360, le reste ~180 |

| Critère | Avant master : avant | Avant master : après | Exporté : avant | Exporté : après |
|---|---|---|---|---|
| Drop − Groove ≥ +2 LU | 1 sur 37 | 3 sur 37 | 0 sur 37 | 0 sur 37 |
| Drop − Break ≥ +4 LU | 5 sur 25 | 9 sur 25 | 1 sur 25 | 2 sur 25 |
| Montée sous le Drop | 0 sur 7 | 23 sur 25 | 0 sur 7 | 9 sur 25 |
| LRA entre 3 et 6 LU | 27 sur 37 | 17 sur 37 | 6 sur 37 | 5 sur 37 |

### Par style (LU ; avant master, puis exporté)

| Style | Durée avant → après (s) | D − G avant master | D − B avant master | D − Montée avant master | LRA avant master | D − G exporté | D − B exporté | D − Montée exporté | LRA exporté |
|---|---|---|---|---|---|---|---|---|---|
| acid_house | 176 → 192 | −0,4 → +0,2 | +0,3 → +2,1 | – → +0,5 | 3,4 → 2,2 | −0,1 | +0,9 | +0,3 | 1,0 |
| acid_techno | 209 → 306 | −0,5 → +0,5 | +0,5 → +2,4 | – → +0,5 | 2,6 → 2,0 | −0,2 | +0,2 | +0,3 | 2,0 |
| acidcore | 208 → 280 | −0,2 → −0,0 | +0,1 → +0,5 | – → +1,0 | 1,0 → 0,7 | −0,8 | −0,5 | +0,9 | 1,6 |
| afro_house | 173 → 189 | −0,5 → +0,6 | +0,2 → +3,4 | – → +0,6 | 4,2 → 2,1 | −0,3 | +1,6 | −0,1 | 2,0 |
| afrobeats | 183 → 192 | +0,2 → +0,8 | – → – | – → – | 8,3 → 4,9 | −0,1 | – | – | 2,9 |
| amapiano | 189 → 206 | +3,4 → +3,4 | +9,2 → +9,0 | – → +3,5 | 5,4 → 6,3 | +0,6 | +4,5 | +0,6 | 2,3 |
| boom_bap | 209 → 167 | +0,0 → +0,8 | – → – | – → – | 6,9 → 5,8 | +0,4 | – | – | 4,7 |
| deep_house | 176 → 192 | −0,5 → +0,6 | −0,1 → +2,5 | – → +0,8 | 3,5 → 2,8 | +0,2 | +1,6 | +0,3 | 1,0 |
| detroit | 168 → 335 | −0,2 → +0,4 | +2,3 → +4,7 | – → +0,0 | 2,8 → 2,5 | −0,6 | +2,0 | −0,4 | 2,0 |
| disco | 182 → 199 | −0,3 → +0,3 | +1,4 → +3,2 | – → +0,6 | 4,3 → 3,6 | +0,1 | +1,0 | −0,2 | 2,6 |
| dnb | 166 → 298 | −0,5 → +2,4 | +3,8 → +3,8 | −0,5 → +2,4 | 4,0 → 4,5 | −1,1 | −1,4 | −1,1 | 2,0 |
| drill | 135 → 176 | +0,2 → +0,7 | – → – | – → – | 10,6 → 9,9 | −0,3 | – | – | 5,7 |
| dubstep | 206 → 302 | −1,6 → +1,5 | +3,2 → +3,4 | −1,6 → +1,5 | 4,7 → 5,2 | −1,3 | −0,9 | −1,3 | 1,7 |
| gabber | 165 → 296 | −1,1 → +1,9 | +2,2 → +2,3 | −1,1 → +1,9 | 4,0 → 2,9 | +0,7 | −0,8 | +0,7 | 2,2 |
| grime | 137 → 178 | +0,2 → +0,9 | – → – | – → – | 3,8 → 3,9 | +0,4 | – | – | 2,2 |
| hard_techno | 192 → 358 | −0,1 → +1,8 | −0,2 → +2,3 | – → +1,1 | 3,4 → 3,1 | +0,5 | −0,8 | +0,3 | 1,3 |
| house | 173 → 189 | −0,3 → +0,5 | +0,9 → +3,1 | – → +0,8 | 4,4 → 3,2 | −0,0 | +1,6 | −0,0 | 1,9 |
| industrial_techno | 155 → 311 | −0,3 → +0,0 | +0,1 → +1,2 | – → −0,0 | 1,5 → 0,7 | −0,1 | +0,3 | +0,1 | 0,9 |
| jersey_club | 137 → 178 | +0,2 → +0,5 | – → – | – → – | 4,9 → 5,9 | −0,2 | – | – | 2,7 |
| jump_up | 166 → 298 | −1,3 → +1,7 | +4,8 → +4,7 | −1,3 → +1,7 | 5,2 → 5,0 | −1,0 | −0,8 | −1,0 | 2,6 |
| jungle | 175 → 326 | −0,3 → +1,0 | +1,6 → +4,8 | – → +0,5 | 4,0 → 4,6 | −0,5 | +1,0 | −0,2 | 3,0 |
| liquid_dnb | 166 → 298 | −0,6 → +2,4 | +4,4 → +4,5 | −0,6 → +2,4 | 4,0 → 4,8 | −0,4 | +1,3 | −0,4 | 2,9 |
| lofi | 206 → 183 | −0,0 → +0,8 | – → – | – → – | 3,8 → 8,5 | +0,7 | – | – | 8,5 |
| minimal | 165 → 330 | −0,0 → +1,3 | +0,4 → +2,2 | – → +1,1 | 4,1 → 2,7 | −1,0 | +1,4 | −0,3 | 1,9 |
| neurofunk | 166 → 298 | −1,9 → +1,8 | +7,5 → +7,2 | −1,9 → +1,7 | 5,4 → 7,5 | −0,1 | +2,1 | −0,1 | 4,4 |
| phonk | 148 → 192 | −0,1 → +0,5 | – → – | – → – | 16,0 → 14,9 | −0,0 | – | – | 13,2 |
| pop | 166 → 207 | +0,3 → +0,9 | – → – | – → – | 3,3 → 2,9 | −0,2 | – | – | 1,2 |
| reggaeton | 200 → 210 | +0,1 → +0,4 | – → – | – → – | 4,8 → 4,2 | −0,0 | – | – | 0,8 |
| retro_techno | 165 → 330 | −0,5 → +1,1 | +2,1 → +9,3 | – → +0,2 | 4,2 → 2,9 | −0,5 | +5,2 | −0,1 | 2,0 |
| riddim | 206 → 309 | −3,9 → −0,1 | +4,3 → +5,1 | −3,9 → −0,1 | 7,8 → 7,3 | −1,1 | +1,7 | −1,1 | 4,0 |
| speed_garage | 162 → 177 | +0,5 → +1,3 | +1,6 → +5,2 | – → +1,0 | 3,8 → 3,0 | −0,5 | +2,3 | −0,5 | 2,1 |
| synthwave | 183 → 192 | +0,2 → +0,5 | – → – | – → – | 4,8 → 3,6 | +0,2 | – | – | 0,7 |
| tech_house | 168 → 183 | −0,4 → +0,5 | +0,6 → +3,8 | – → +0,8 | 3,7 → 2,9 | −0,1 | +2,1 | −0,1 | 2,5 |
| techno | 160 → 320 | −0,4 → +1,2 | +0,9 → +2,5 | – → +0,8 | 4,0 → 3,2 | −0,6 | +0,5 | +0,0 | 3,0 |
| trance | 209 → 306 | −0,1 → +1,6 | +0,2 → +1,7 | – → +1,0 | 3,6 → 3,8 | −0,2 | −0,7 | +0,0 | 2,0 |
| trap | 137 → 178 | +0,2 → +0,7 | – → – | – → – | 7,2 → 6,1 | −0,4 | – | – | 2,6 |
| uk_garage | 145 → 196 | +0,4 → +0,8 | – → – | – → – | 3,3 → 3,8 | −0,3 | – | – | 1,9 |

### Lecture

- **Ce que l'arrangement gagne (avant master)** : Drop − Break +1,4 → +3,4 LU (médiane), Drop − Groove −0,2 → +0,8, et la Montée passe sous son Drop dans 23 styles sur 25 (avant : 0 sur 7 ; en bass music elle était 1,3 LU au-dessus, elle est maintenant 1,5 à 2,4 LU en dessous, sauf le riddim à −0,1).
- **Ce que le master reprend** : après la cible de sonie, l'écrêteur et le limiteur (gain médian +8,3 dB, perte médiane 4 LU, jusqu'à 6,8 LU en bass music), Drop − Break retombe à +1,0, Drop − Groove à −0,2 et la Montée repasse au-dessus du Drop dans 16 styles sur 25 : le limiteur écrase surtout le Drop, la section la plus forte. C'est le chantier S5 (cible de sonie qui vise le Drop, perte au limiteur plafonnée à 3 LU) ; S2 ne touche pas au master.
- **Drop − Groove reste sous +2 LU même avant master** (+0,8) : le kick du kit (sinus long à −12 LUFS) et la basse font l'essentiel de la sonie de toutes les sections, les couches que le Drop ajoute (lead, ride, crash, contre-chant) pèsent peu en LUFS. La batterie de S3 (kick court, 808 et rumble qui n'arrivent qu'au Drop) et S5 sont les leviers suivants ; pousser plus fort ici voudrait dire baisser le kick du Groove, ce que les vrais morceaux ne font pas.
- **LRA** : 3 à 6 LU avant master pour 17 styles sur 37 (27 avant) : les morceaux longs passent plus de temps dans leurs Drops et les intros montent par couches, et 13 styles de club restent sous 3 LU. Exporté, le LRA reste à 2,1 (le limiteur, encore).
- Durée médiane : 168 → 207 s ; techno 160 → 320 s, drum & bass 166 → 298 s, house 173 → 189 s, trap 137 → 178 s.

37 morceaux de l'assistant (« Partir d'un style », sons intégrés, aucun pack), exportés par le code de l'appli (`SongBenchExport`), rejoués par le moteur sur Linux (`ps_render`) et mesurés par `ps_measure` (libebur128 pour la sonie ; true peak suréchantillonné ×4 comme ffmpeg). Le « Drop » est la section principale (Drop, sinon Refrain) : bandes, pente et stéréo y sont mesurées. Méthode : `docs/plan/son.md` § 2 ; relancer : `scripts/song-bench.sh`.

## Le banc complet (exporté)

### Résumé (médiane sur les styles)

| Mesure | Avant | Après | Écart | Min – max (après) | Cible |
|---|---|---|---|---|---|
| LUFS-I (morceau) | −8,3 | −8,3 | −0,0 | −12,1 – −6,7 | cible du genre ±1 |
| LUFS du Drop / Refrain | −8,7 | −8,6 | +0,1 | −11,4 – −7,3 | −8 à −6 en club |
| True peak (dBTP) | −1,17 | −1,17 | +0,00 | −1,19 – −1,12 | ≤ −1,0 |
| PLR (TP − LUFS-I) | 7,1 | 7,1 | +0,0 | 5,5 – 10,9 | 6 à 10 en club |
| LRA (LU) | 1,8 | 2,1 | +0,3 | 0,7 – 13,2 | 3 à 9 en club |
| Facteur de crête du Drop (dB) | 7,7 | 7,7 | −0,1 | 6,3 – 10,0 | ≥ 6 |
| Air > 6 kHz (dB rel.) | −20,1 | −20,6 | −0,5 | −26,7 – −14,7 | −20 à −16 |
| Pente 0,1–8 kHz (dB/oct) | −4,91 | −4,98 | −0,07 | −6,76 – −3,19 | −4,5 ±0,7 |
| Côté/centre (dB) | −23,1 | −23,6 | −0,5 | −38,7 – −14,0 | −15 à −9 |
| Corrélation L/R | 0,99 | 0,99 | +0,00 | 0,92 – 1,00 | 0,3 à 0,85 |
| Corrélation < 120 Hz | 1,000 | 1,000 | +0,000 | 0,991 – 1,000 | ≥ 0,95 |
| Perte en mono (LU) | −0,04 | −0,04 | +0,00 | −0,34 – 0,00 | ≥ −1,5 |
| Drop − Groove, Couplet ou Montée (LU) | −0,2 | −0,2 | +0,1 | −1,3 – 0,7 | ≥ +2 |
| Drop − Break (LU) | −0,2 | 1,0 | +1,2 | −1,4 – 5,2 | ≥ +4 |
| Continu (moyenne absolue) | 0,0000 | 0,0000 | −0,0000 | 0,0000 – 0,0000 | ≤ 0,001 |
| Gain de la cible (dB) | 8,6 | 8,3 | −0,3 | −0,4 – 14,7 | – |
| Perte dans la chaîne (LU) | 4,5 | 4,0 | −0,5 | 0,1 – 6,8 | ≤ 6 (plafond) |

## Critères (docs/plan/son.md § 2.3 et étape 2)

| Critère | Morceaux conformes | Hors critère |
|---|---|---|
| True peak ≤ −1,0 dBTP (bloquant) | 37 sur 37 | – |
| LUFS-I à ±1 LU de la cible du style | 31 sur 37 | afrobeats (−1,0, plafonné), dubstep (−1,5, plafonné), hard_techno (−1,5, plafonné), neurofunk (−1,1, plafonné), riddim (−2,4, plafonné), trap (−1,1, plafonné) |
| LUFS-I à ±1 LU de la cible, ou plafonné (perte ≥ 6 LU) (bloquant) | 37 sur 37 | – |
| Continu ≤ 0,001 (bloquant) | 37 sur 37 | – |
| Aucun NaN (bloquant) | 37 sur 37 | – |
| Aucun échantillon écrêté (≥ 0,999) | 37 sur 37 | – |
| PLR ≥ 6 dB | 34 sur 37 | acid_techno, acidcore, gabber |
| Facteur de crête du Drop ≥ 6 dB | 37 sur 37 | – |

Plafonné : la chaîne retirait déjà 6 LU (écrêteur et limiteur) au gain trouvé ; pousser plus écraserait sans gagner de sonie (`MasterChain::kMaxLossDb`). Ces morceaux attendent une batterie plus dense (étape 3).

## Par style

Gain : ce que la cible de sonie ajoute avant l'écrêteur ; Perte : ce que l'écrêteur et le limiteur retirent (LU).

| Style | LUFS-I | Cible | Gain | Perte | Drop | TP | PLR | LRA | Air | Pente | Côté/centre | Corr. | Corr. < 120 | Drop − avant | Drop − Break |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| acid_house | −8,1 | −8,0 | +6,1 | 1,3 | −7,7 (+0,1) | −1,19 | 6,9 | 1,0 (−0,6) | −20,9 (−0,2) | −4,34 | −30,3 (+0,3) | 1,00 | 1,000 | −0,1 (+0,1) | 0,9 (+1,1) |
| acid_techno | −7,1 (+0,1) | −7,0 | +8,3 | 2,5 | −7,4 | −1,17 | 5,9 (−0,1) | 2,0 (+1,1) | −20,6 (−0,3) | −4,39 | −34,8 (−0,2) | 1,00 | 1,000 | −0,2 (+0,1) | 0,2 (+0,8) |
| acidcore | −7,1 | −7,0 | +1,5 | 1,3 | −7,4 (+0,1) | −1,16 | 5,9 | 1,6 (+0,1) | −15,9 | −4,98 | −38,7 (−0,3) | 1,00 | 1,000 | −0,8 (+0,1) | −0,5 (+0,2) |
| afro_house | −8,3 | −8,0 | +8,3 | 3,0 | −8,7 | −1,18 | 7,1 | 2,0 (+0,5) | −21,9 (−0,1) | −4,90 | −22,0 (+0,1) | 0,99 | 0,999 | −0,3 (+0,2) | 1,6 (+1,8) |
| afrobeats | −9,5 (+0,3) | −8,5 | +9,1 | 6,5 | −9,3 | −1,12 | 8,4 (−0,3) | 2,9 (−2,4) | −24,6 (−0,3) | −5,50 (+0,25) | −25,9 (−0,2) | 0,99 | 1,000 | −0,1 (−0,1) | – |
| amapiano | −8,5 (+0,2) | −8,0 | +12,8 | 5,4 | −8,2 (−0,1) | −1,17 | 7,3 (−0,2) | 2,3 (+0,5) | −21,3 (−0,1) | −5,90 (−0,05) | −23,9 (−0,1) | 0,99 | 1,000 | 0,6 | 4,5 (+0,2) |
| boom_bap | −10,1 (+0,1) | −10,0 | +4,9 | 2,4 | −9,6 | −1,16 | 9,0 (−0,1) | 4,7 (−0,5) | −22,5 | −5,81 | −35,4 (−0,2) | 1,00 | 1,000 | 0,4 (+0,3) | – |
| deep_house | −9,1 | −9,0 | +5,6 | 1,6 | −8,8 (+0,2) | −1,19 | 7,9 | 1,0 (−0,9) | −21,2 | −5,85 (−0,16) | −22,0 (+0,3) | 0,99 | 1,000 | 0,2 (+0,5) | 1,6 (+1,8) |
| detroit | −8,1 | −8,0 | +5,3 | 2,2 | −8,4 | −1,17 | 7,0 | 2,0 (+0,8) | −19,3 (−0,5) | −3,90 (−0,15) | −27,5 (−0,3) | 1,00 | 1,000 | −0,6 (−0,4) | 2,0 (+1,8) |
| disco | −8,2 | −8,0 | +8,1 | 3,7 | −8,8 (+0,1) | −1,16 | 7,0 | 2,6 (+0,4) | −20,1 | −5,73 (+0,07) | −19,5 (+0,4) | 0,98 | 1,000 | 0,1 (+0,3) | 1,0 (+1,5) |
| dnb | −7,9 (+0,3) | −7,0 | +13,4 | 6,8 | −8,6 (+0,1) | −1,17 | 6,8 (−0,3) | 2,0 (−0,3) | −18,1 (−0,5) | −5,01 (−0,08) | −25,3 (−0,2) | 0,99 | 1,000 | −1,1 (+1,0) | −1,4 (−0,2) |
| drill | −9,4 | −8,5 | +10,2 | 6,3 | −9,2 | −1,17 | 8,3 | 5,7 (−1,8) | −21,4 (−0,2) | −4,59 (−0,07) | −22,3 (+0,2) | 0,99 | 1,000 | −0,3 (−0,1) | – |
| dubstep | −8,5 | −7,0 | +13,4 | 6,3 | −9,0 | −1,18 | 7,3 | 1,7 (−0,9) | −18,8 (−0,4) | −4,75 (−0,17) | −17,5 (+0,7) | 0,96 | 1,000 | −1,3 (+1,1) | −0,9 (−0,3) |
| gabber | −6,7 | −6,0 | +13,2 | 5,0 | −7,3 (−0,1) | −1,17 | 5,5 | 2,2 (+0,4) | −19,4 (−0,8) | −4,98 (−0,20) | −25,9 (−0,1) | 0,99 | 0,999 | 0,7 (+1,1) | −0,8 (−0,2) |
| grime | −8,6 | −8,5 | +5,2 | 1,5 | −8,1 (+0,1) | −1,17 | 7,4 | 2,2 (−0,6) | −14,7 (+0,7) | −3,19 (+0,15) | −14,5 (+1,1) | 0,93 | 1,000 | 0,4 (+0,3) | – |
| hard_techno | −8,5 (+0,1) | −7,0 | +12,6 | 6,3 | −8,4 (+0,1) | −1,17 | 7,3 (−0,1) | 1,3 (−0,2) | −19,0 (−0,2) | −4,88 | −28,5 (−0,2) | 1,00 | 1,000 | 0,5 (+0,6) | −0,8 (+0,7) |
| house | −8,2 | −8,0 | +7,8 | 3,2 | −8,4 (+0,1) | −1,17 | 7,0 | 1,9 (+0,5) | −20,8 (−0,1) | −5,56 (−0,06) | −27,9 (+0,1) | 1,00 | 1,000 | −0,0 (+0,2) | 1,6 (+1,9) |
| industrial_techno | −7,6 (+0,1) | −7,0 | +8,0 | 4,2 | −7,7 | −1,16 | 6,4 | 0,9 (+0,4) | −18,7 (−0,2) | −5,48 (−0,08) | −19,1 (−0,3) | 0,98 | 0,994 | −0,1 (+0,1) | 0,3 (+0,3) |
| jersey_club | −8,9 | −8,5 | +7,8 | 3,6 | −8,8 | −1,17 | 7,7 | 2,7 | −23,8 (−0,2) | −4,77 (−0,18) | −16,0 (+1,0) | 0,95 | 1,000 | −0,2 (−0,2) | – |
| jump_up | −7,9 (+0,2) | −7,0 | +14,3 | 6,6 | −8,7 | −1,16 | 6,7 (−0,2) | 2,6 (+0,2) | −19,2 (−0,5) | −4,36 (−0,18) | −21,6 (−0,3) | 0,99 | 1,000 | −1,0 (+1,1) | −0,8 (−0,6) |
| jungle | −7,9 (+0,1) | −7,0 | +12,6 | 6,5 | −8,4 | −1,15 | 6,7 (−0,1) | 3,0 (+1,2) | −17,5 (−0,2) | −4,63 | −31,4 (+0,4) | 1,00 | 1,000 | −0,5 (−0,3) | 1,0 (+1,4) |
| liquid_dnb | −8,8 | −8,0 | +12,7 | 6,7 | −8,9 | −1,18 | 7,6 | 2,9 (+0,2) | −16,9 (−0,2) | −5,18 (−0,09) | −22,0 | 0,99 | 1,000 | −0,4 (+1,5) | 1,3 (−0,2) |
| lofi | −12,1 | −12,0 | −0,4 | 0,1 | −11,4 (+0,5) | −1,19 | 10,9 | 8,5 (+4,7) | −25,5 (−0,1) | −6,34 | −29,7 (−0,6) | 1,00 | 1,000 | 0,7 (+0,8) | – |
| minimal | −8,1 | −8,0 | +7,2 | 2,1 | −8,6 | −1,18 | 6,9 | 1,9 (+0,5) | −26,7 (−1,9) | −6,76 (−0,33) | −14,0 | 0,92 | 0,994 | −1,0 (−0,1) | 1,4 (+1,5) |
| neurofunk | −8,1 (−0,1) | −7,0 | +14,6 | 6,7 | −8,4 | −1,18 | 6,9 (+0,1) | 4,4 (+1,3) | −18,2 (−0,5) | −4,61 (−0,28) | −23,7 (+1,9) | 0,99 | 1,000 | −0,1 (+1,7) | 2,1 (−0,8) |
| phonk | −8,7 (−0,1) | −8,5 | +5,2 | 1,8 | −8,3 (+0,4) | −1,17 | 7,5 (+0,1) | 13,2 (−2,0) | −23,0 | −5,17 | −27,7 (−0,6) | 1,00 | 1,000 | −0,0 (+0,3) | – |
| pop | −9,0 (−0,2) | −8,5 | +7,3 | 4,5 | −9,0 (+0,1) | −1,15 | 7,8 (+0,1) | 1,2 | −22,6 (+0,4) | −5,73 (+0,08) | −20,7 (−0,6) | 0,98 | 0,999 | −0,2 (+0,1) | – |
| reggaeton | −9,0 | −8,5 | +9,0 | 5,6 | −9,1 (+0,1) | −1,17 | 7,9 | 0,8 | −20,6 | −4,61 | −20,4 (−0,6) | 0,98 | 0,999 | −0,0 | – |
| retro_techno | −8,2 (+0,1) | −8,0 | +7,9 | 3,1 | −8,5 (+0,1) | −1,17 | 7,0 | 2,0 (+0,7) | −20,6 (−0,6) | −4,37 (−0,11) | −29,2 (−0,1) | 1,00 | 1,000 | −0,5 (−0,2) | 5,2 (+4,6) |
| riddim | −9,4 (−0,3) | −7,0 | +14,7 | 6,0 | −9,5 (+0,1) | −1,17 | 8,3 (+0,3) | 4,0 (−0,2) | −16,8 (−0,3) | −3,84 (−0,16) | −18,6 | 0,97 | 0,991 | −1,1 (+1,9) | 1,7 (−0,4) |
| speed_garage | −8,1 | −8,0 | +7,6 | 2,6 | −8,5 | −1,18 | 6,9 | 2,1 (+0,6) | −19,1 (−0,3) | −5,17 (−0,13) | −29,6 (+0,4) | 1,00 | 1,000 | −0,5 (+0,2) | 2,3 (+2,2) |
| synthwave | −8,4 (+0,1) | −8,0 | +7,9 | 4,0 | −8,3 | −1,17 | 7,2 (−0,1) | 0,7 (−0,7) | −21,0 (+0,2) | −4,82 | −20,3 (−0,1) | 0,98 | 1,000 | 0,2 (+0,1) | – |
| tech_house | −8,2 | −8,0 | +8,1 | 3,2 | −8,5 | −1,18 | 7,0 | 2,5 (+1,2) | −19,9 (−0,1) | −5,04 (−0,14) | −30,6 (+0,7) | 1,00 | 1,000 | −0,1 (+0,2) | 2,1 (+1,9) |
| techno | −7,6 | −7,0 | +10,1 | 5,1 | −8,4 | −1,15 | 6,4 | 3,0 (+0,8) | −22,4 (−0,5) | −5,78 (−0,13) | −16,8 (−0,1) | 0,96 | 1,000 | −0,6 (−0,3) | 0,5 (+1,1) |
| trance | −8,1 (+0,2) | −8,0 | +9,5 | 3,6 | −8,6 (+0,1) | −1,17 | 7,0 (−0,1) | 2,0 (+0,7) | −18,9 (−0,7) | −5,26 (−0,08) | −20,1 (+0,1) | 0,98 | 1,000 | −0,2 (+0,1) | −0,7 (+0,7) |
| trap | −9,6 | −8,5 | +9,2 | 6,2 | −9,5 | −1,17 | 8,5 | 2,6 (−3,2) | −22,8 (−0,1) | −4,62 (−0,16) | −19,8 (+0,8) | 0,98 | 1,000 | −0,4 (−0,2) | – |
| uk_garage | −8,6 (−0,1) | −8,0 | +11,1 | 6,2 | −8,9 (+0,2) | −1,18 | 7,5 | 1,9 | −17,3 (−0,3) | −5,32 (−0,17) | −23,6 (−0,5) | 0,99 | 1,000 | −0,3 | – |

## Bandes du Drop (dB relatifs à l'énergie totale)

| Style | Sub < 60 | Grave 60–250 | Bas-méd. 250–500 | Médium 0,5–2 k | Haut-méd. 2–6 k | Air > 6 k |
|---|---|---|---|---|---|---|
| acid_house | −1,8 | −6,7 | −18,3 | −11,8 | −14,4 | −20,9 |
| acid_techno | −1,6 | −6,6 | −18,8 | −14,6 | −15,2 | −20,6 |
| acidcore | −9,7 | −1,6 | −12,2 | −13,2 | −13,8 | −15,9 |
| afro_house | −1,5 | −6,4 | −15,5 | −19,5 | −20,5 | −21,9 |
| afrobeats | −2,0 | −6,1 | −11,6 | −15,1 | −17,6 | −24,6 |
| amapiano | −3,8 | −3,4 | −12,3 | −13,8 | −19,8 | −21,3 |
| boom_bap | −2,1 | −5,1 | −13,5 | −18,2 | −20,9 | −22,5 |
| deep_house | −2,1 | −6,2 | −13,6 | −11,0 | −23,4 | −21,2 |
| detroit | −1,5 | −7,9 | −16,5 | −12,1 | −14,8 | −19,3 |
| disco | −4,0 | −5,8 | −7,3 | −10,9 | −19,0 | −20,1 |
| dnb | −2,8 | −6,7 | −8,2 | −11,1 | −17,3 | −18,1 |
| drill | −1,6 | −7,0 | −16,8 | −13,8 | −21,2 | −21,4 |
| dubstep | −2,3 | −8,9 | −7,5 | −11,1 | −19,4 | −18,8 |
| gabber | −3,4 | −7,0 | −7,4 | −8,8 | −17,9 | −19,4 |
| grime | −3,7 | −5,7 | −14,7 | −8,9 | −10,0 | −14,7 |
| hard_techno | −3,4 | −6,0 | −8,4 | −9,6 | −19,0 | −19,0 |
| house | −2,5 | −6,0 | −9,8 | −12,0 | −19,5 | −20,8 |
| industrial_techno | −4,4 | −3,9 | −11,5 | −9,5 | −18,2 | −18,7 |
| jersey_club | −1,7 | −10,2 | −11,5 | −9,3 | −21,5 | −23,8 |
| jump_up | −2,1 | −10,0 | −8,1 | −10,2 | −17,9 | −19,2 |
| jungle | −2,6 | −5,0 | −12,2 | −15,6 | −15,3 | −17,5 |
| liquid_dnb | −2,8 | −5,4 | −10,9 | −12,0 | −18,2 | −16,9 |
| lofi | −2,3 | −4,4 | −14,8 | −22,3 | −23,4 | −25,5 |
| minimal | −2,3 | −5,9 | −9,8 | −13,8 | −25,6 | −26,7 |
| neurofunk | −1,4 | −8,6 | −10,4 | −17,1 | −18,3 | −18,2 |
| phonk | −1,8 | −6,0 | −14,9 | −13,8 | −20,2 | −23,0 |
| pop | −2,7 | −4,6 | −11,7 | −14,8 | −19,7 | −22,6 |
| reggaeton | −1,9 | −6,7 | −13,2 | −11,8 | −17,5 | −20,6 |
| retro_techno | −1,7 | −6,1 | −16,0 | −15,4 | −18,3 | −20,6 |
| riddim | −1,8 | −8,8 | −11,1 | −11,9 | −17,2 | −16,8 |
| speed_garage | −2,2 | −6,0 | −12,0 | −13,3 | −18,2 | −19,1 |
| synthwave | −3,1 | −5,5 | −12,8 | −8,6 | −15,6 | −21,0 |
| tech_house | −1,7 | −7,3 | −12,7 | −11,8 | −19,6 | −19,9 |
| techno | −2,9 | −4,9 | −9,9 | −13,5 | −21,2 | −22,4 |
| trance | −3,1 | −6,8 | −8,6 | −8,8 | −19,1 | −18,9 |
| trap | −1,6 | −8,5 | −18,0 | −13,2 | −20,0 | −22,8 |
| uk_garage | −3,2 | −5,4 | −11,9 | −8,7 | −18,7 | −17,3 |
| **médiane** | −2,3 | −6,1 | −12,0 | −12,0 | −18,7 | −20,6 |

## Défauts

| Style | NaN | Écrêtés | DC | Fin (dBFS, 500 ms) | Durée (s) |
|---|---|---|---|---|---|
| acid_house | 0 | 0 | 0,0000 | −33,6 | 192,0 |
| acid_techno | 0 | 0 | 0,0000 | −34,5 | 306,1 |
| acidcore | 0 | 0 | 0,0000 | −33,7 | 280,2 |
| afro_house | 0 | 0 | 0,0000 | −32,8 | 188,8 |
| afrobeats | 0 | 0 | 0,0000 | −45,0 | 192,0 |
| amapiano | 0 | 0 | 0,0000 | −40,2 | 205,7 |
| boom_bap | 0 | 0 | 0,0000 | −35,7 | 167,0 |
| deep_house | 0 | 0 | 0,0000 | −36,9 | 192,0 |
| detroit | 0 | 0 | 0,0000 | −36,8 | 335,2 |
| disco | 0 | 0 | 0,0000 | −36,2 | 198,6 |
| dnb | 0 | 0 | 0,0000 | −34,0 | 297,9 |
| drill | 0 | 0 | 0,0000 | −33,7 | 175,8 |
| dubstep | 0 | 0 | 0,0000 | −33,9 | 301,7 |
| gabber | 0 | 0 | 0,0000 | −32,5 | 296,2 |
| grime | 0 | 0 | 0,0000 | −40,3 | 178,3 |
| hard_techno | 0 | 0 | 0,0000 | −32,4 | 358,4 |
| house | 0 | 0 | 0,0000 | −36,7 | 188,8 |
| industrial_techno | 0 | 0 | 0,0000 | −33,3 | 310,6 |
| jersey_club | 0 | 0 | 0,0000 | −37,4 | 178,3 |
| jump_up | 0 | 0 | 0,0000 | −34,7 | 297,9 |
| jungle | 0 | 0 | 0,0000 | −33,2 | 325,8 |
| liquid_dnb | 0 | 0 | 0,0000 | −34,0 | 297,9 |
| lofi | 0 | 0 | 0,0000 | −41,4 | 182,9 |
| minimal | 0 | 0 | 0,0000 | −36,2 | 330,0 |
| neurofunk | 0 | 0 | 0,0000 | −35,9 | 297,9 |
| phonk | 0 | 0 | 0,0000 | −38,0 | 192,0 |
| pop | 0 | 0 | 0,0000 | −37,2 | 206,9 |
| reggaeton | 0 | 0 | 0,0000 | −33,9 | 210,0 |
| retro_techno | 0 | 0 | 0,0000 | −33,6 | 330,0 |
| riddim | 0 | 0 | 0,0000 | −35,3 | 308,6 |
| speed_garage | 0 | 0 | 0,0000 | −35,9 | 177,2 |
| synthwave | 0 | 0 | 0,0000 | −36,8 | 192,0 |
| tech_house | 0 | 0 | 0,0000 | −33,8 | 182,9 |
| techno | 0 | 0 | 0,0000 | −34,1 | 320,0 |
| trance | 0 | 0 | 0,0000 | −34,5 | 306,1 |
| trap | 0 | 0 | 0,0000 | −35,1 | 178,3 |
| uk_garage | 0 | 0 | 0,0000 | −33,1 | 196,4 |

## Empreintes des rendus (SHA-256, WAV float 32 bits)

Deux passages du banc sur le même code donnent les mêmes octets.

```
2f066c382b67367d6ecdac72a20fbf1c5e90acf6e3e061ff5436a8fbb0596da0  acid_house.wav
ca3b673491a9cb0fd59836bb02a73b9be1930f367622d2763f1cd4b87b62d58c  acid_techno.wav
f7f828b1e20e716058ee1ca6f64104c90054574934d4f3db15d2c048021a07a8  acidcore.wav
10a926522416ea055bc2efd809a3901aeb17f146510264e220b0f6929a1ccfc5  afro_house.wav
c5c0e35f64a1c7437c26e0ad275c73cd1e544c589062aad3a5fbb3bc3aa0242f  afrobeats.wav
cb248bcdb64757e5f26ee289d10ddb42fbdd69805903180d5f7240eabb1968f7  amapiano.wav
8ef99a62c918ee948cfdaa74abb9cff27d76c526f8856aca0831e03a60f9839e  boom_bap.wav
6878afbc1b1a58ecf220a8e76f9c8bce40ca1ee355ddc012e481e4693c1fd727  deep_house.wav
ee9812f396f69ee0f832fbbdfa6bdb0bf9cfb1c8be2047ad0549219d1e0921be  detroit.wav
097e618cd5425e14d33ad7022bf66b720a29b0d4a974c611ed0c609ba4ba5514  disco.wav
73dc57a96357d7adc3411488ab7a7e0f589b002fce2439c7a9cb0177a359150e  dnb.wav
939e7c9a3a4837d32882505cf32c5ea817a27e2c011568185a80591d80a8492d  drill.wav
64089df3a9d030d021e27f4ac1b1ceaa3bf2bf83671b5585ecfeeda55af6e947  dubstep.wav
eefe7f772f93427fb23d38d1cd5b7535aa4a8af7d69512dd8dd741068435390a  gabber.wav
90c9129b1f7318d348ea02e56a9be32f1cc0f8844aef1c320c5d8e84428149bf  grime.wav
4067d39e4c7f468ad2d8646dbccca286e9a6b02ee8472a3ccbb01a93b6ddea8d  hard_techno.wav
39a5d15952ea2ee8ea932385a879164a77990bd73cb782223ac658f6addcf46e  house.wav
6914f1dd8640a7d5e71a750730f66f5e1e8d7b1bdacdfe3ef81bd570691dc97d  industrial_techno.wav
608c3b1b46ab845009c9d717e42f7f1363eae97c5fbbacc4d17b29e36b0e91d0  jersey_club.wav
f75deedd4274ff5a8104a6ec40f42afa94c2e25a43a7d18dced8160b7bd3fc9a  jump_up.wav
5274f0bfe36b37b47eff5f518be84c3a50f4545b08177b33e9204f6c209052f9  jungle.wav
63da3c203774e9fbf22ab0adf6a2f7910b076e13db257902433e72a350e7b043  liquid_dnb.wav
2f318fcb8866f7f2f6f7e5d85b056a4d714d2c35041f4351800008f3cb6fa2ad  lofi.wav
8f85d4416d540d03d5a92b09ea01f0db8d297e7e0a88a4fbfc596fb0bf759ef0  minimal.wav
6fe54fa2b4e03dbdfca3457eb7dda3cfd11604104d6146aa32ba179bbb36896e  neurofunk.wav
34122fe2022b1b3da6f0a8c4c50208019e4a9a7434d3bd14f696df50841bf2b8  phonk.wav
35eff17487372db66e9fd5dd7bc0471dae735d7c5a53ca26b708a982f773ffe4  pop.wav
feddc2dfea2d6fce3e772161d59d4e25afa0a0a0c4008bdae1a84fdd1f2f62fa  reggaeton.wav
673cd5beff08d175e1429ac3911b0ea33e1173f623ccf95f2fc6dbd2d046e9e0  retro_techno.wav
a2548cc5e44e05eb19e6a3b57837e4ddfb3985b37ab16385ded849eaaf8359fc  riddim.wav
8d0a1e9ff0b8837a2d9c46708c7db3d0f961c3abb928872d7312f0c242fee0c9  speed_garage.wav
612a4e6b71c8c32b64c8c33bf695def3320cf695d19ee754b051a5aad8cbc959  synthwave.wav
e2f2e5dc4e8ee284c99125f19f38564fcf2152a6130815157c3b6221c0bb429f  tech_house.wav
1fccfd7a48cc5cee15f3968af558bd1b7209d0b638157958b2730b34e413856b  techno.wav
4abb5aa052928bd4058096363d450fc9da93a3f978ba5732b8c0bf6df5d61f01  trance.wav
e336cc41246ab57d067707bad5c8f411632931da7170a99997a148ff26f9cda8  trap.wav
12d64d3b0c166a6afa4537ada860548ae8ba2b29194ce106d8f30f44be60e757  uk_garage.wav
```
