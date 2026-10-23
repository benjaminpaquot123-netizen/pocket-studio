# Banc de mesure du son

Le banc rend sur Linux, sans téléphone, les vrais morceaux de l'assistant et les mesure. Il sert à prouver chaque étape du plan « son » (`docs/plan/son.md` § 2 et § 4).

## Comment ça marche

1. **Enregistrer** : le test `SongBenchExport` (app, Robolectric) fait pour chaque style le morceau de « Partir d'un style » (choix conseillés, sons intégrés, aucun pack), l'ouvre comme l'appli puis l'exporte comme « Exporter › Morceau ». Le code est celui de l'export (`describe`, `syncEngine`, `captureInto`). Seule différence : `NativeEngine.sink` est un `RecordingSink`. Chaque appel au moteur est écrit dans `<style>.pscmd` (texte, une commande par ligne) et chaque son dans `samples/<empreinte>.f32`. Les voix du kit que l'assistant transforme en sons sont rendues par `ps_render`.
2. **Rejouer** : `engine/bench/ps_render` poste ces commandes dans l'`OfflineRenderer` du cœur, comme le pont JNI, et écrit un WAV float 32 bits.
3. **Mesurer** : `engine/bench/ps_measure` sort un JSON :
   - sonie intégrée, LRA (libebur128 1.2.6, MIT) ;
   - true peak suréchantillonné ×4 comme ffmpeg ;
   - PLR, facteur de crête ;
   - bandes (sub, grave, bas-médium, médium, haut-médium, air) et pente spectrale ;
   - côté/centre, corrélation, corrélation et côté/centre sous 120 Hz, perte en mono ;
   - sonie de chaque section, Drop − Groove (ou Couplet, ou Montée) et Drop − Break ;
   - NaN, échantillons écrêtés, DC, niveau de fin.
4. **Rapport** : `scripts/bench/report.py` fait le tableau Markdown et `bench.json`. Avec `--before`, il ajoute l'écart à un passage précédent ; avec `--renders`, le gain de la cible de sonie et la perte dans la chaîne (écrêteur et limiteur) de chaque morceau, lus dans le rapport de fin d'export que `ps_render --report` écrit. Il sort en erreur sur un critère bloquant (voir plus bas).
5. **Pister un son** : `ps_render --no-limiter` rend avant la chaîne master, `--solo <voie>` une seule voie du mixeur (c'est ainsi que les sources du continu ont été trouvées), `--as-app` écrit le fichier au format demandé par l'appli (ligne `FORMAT`).

Lancer : `scripts/song-bench.sh` (tous les styles, environ 5 min, dans `build/bench/`).
Options : `--styles techno,house`, `--ffmpeg` (contre-vérification), `--before docs/son/banc/<fichier>.json`, `--no-record` (rejouer les mêmes `.pscmd` après un changement du moteur seul).
Les rendus restent dans `build/` : jamais dans git.
La CI le fait tourner sur 5 styles (job « Build », étape « Song bench ») et publie le rapport en artefact `song-bench`.

## Vérifié

- Deux passages complets (enregistrement et rendu) donnent les mêmes `.pscmd` et les mêmes WAV, octet pour octet (empreintes SHA-256 dans les rapports).
- Sonie intégrée et true peak à ±0,05 de `ffmpeg -af ebur128=peak=true` sur les 37 styles (ffmpeg arrondit au dixième).
- 37 morceaux sur 37 rendus, aucun NaN, aucun échantillon écrêté.
- Après l'étape 2 : deux passages complets sur le même code donnent encore les mêmes `.pscmd` et les mêmes WAV (l'export en deux passes, la recherche du gain comprise, est déterministe).

## Rapports

- [Référence avant l'étape 1](banc/avant-etape1.md) (`banc/avant-etape1.json`).
- [Après l'étape 1 : gain staging honnête, kit v2](banc/apres-etape1.md) (`banc/apres-etape1.json`).
- [Après l'étape 2 : chaîne master true peak, cibles par genre](banc/apres-etape2.md) (`banc/apres-etape2.json`). C'est la référence de la CI.

## Critères bloquants

`scripts/bench/report.py` fait échouer le banc (donc la CI) sur un true peak au-dessus de −1 dBTP, un continu au-dessus de 0,001, un NaN, ou une sonie à plus de 1 LU de la cible du style alors que la chaîne avait encore de la marge. Un morceau « plafonné » (la chaîne lui retirait déjà 6 LU, `MasterChain::kMaxLossDb`) est listé, pas en échec : pousser plus écraserait sans gagner de sonie, c'est à la batterie de l'étape 3 de le rendre plus dense. `scripts/bench/test_report.py` vérifie ces sorties sur deux mesures minuscules.

## Étapes 1 et 2 en bref (médianes sur 37 styles, sons intégrés)

| Mesure (médiane, 37 styles) | Avant l'étape 1 | Après l'étape 1 | Après l'étape 2 | Cible |
|---|---|---|---|---|
| LUFS du Drop / Refrain | −10,9 | −12,1 | **−8,6** | −8 à −6 en club |
| LUFS-I du morceau | −11,6 | −12,7 | **−8,3** | cible du style ±1 |
| True peak (dBTP) | −0,08 | −0,15 | **−1,17** | ≤ −1,0 |
| Air > 6 kHz (dB rel.) | −22,8 | −19,7 | −20,7 | −20 à −16 |
| Pente 0,1–8 kHz (dB/oct) | −5,26 | −4,98 | −5,04 | −4,5 ±0,7 |
| Facteur de crête du Drop (dB) | 10,4 | 11,4 | 7,7 | ≥ 6 |
| PLR (TP − LUFS-I) | 11,6 | 12,6 | 7,2 | 6 à 10 en club |
| Drop − Break (LU) | 0,2 | 1,4 | −0,2 | ≥ +4 |
| Continu (moyenne absolue) | 0,0082 | 0,0066 | **0,0000** | ≤ 0,001 |

| Critère | Avant l'étape 1 | Après l'étape 1 | Après l'étape 2 |
|---|---|---|---|
| True peak ≤ −1,0 dBTP | 0 sur 37 | 0 sur 37 | **37 sur 37** |
| LUFS-I à ±1 LU de la cible du style | 2 sur 37 | 1 sur 37 | **29 sur 37** (les 8 autres plafonnés) |
| Continu ≤ 0,001 | 4 sur 37 | 4 sur 37 | **37 sur 37** |
| Aucun NaN, aucun échantillon écrêté | 37 sur 37 | 37 sur 37 | 37 sur 37 |
| PLR ≥ 6 dB | 37 sur 37 | 37 sur 37 | 35 sur 37 |
| Facteur de crête du Drop ≥ 6 dB | 37 sur 37 | 37 sur 37 | 37 sur 37 |

- Le plafond −1 dBTP tient sur les 37 morceaux (le plus haut lit −1,11), lu comme ffmpeg. L'export M4A de l'appli vise −2 dBTP en plus, parce que l'AAC décodé dépasse sa source.
- La sonie de club est atteinte sur 29 styles ; les 8 autres (dnb, drill, dubstep, hard_techno, jump_up, neurofunk, riddim, trap) restent 1,1 à 2,2 LU sous leur cible, plafonnés : la chaîne leur retirait déjà 6 LU.
- Le gain que la cible ajoute avant l'écrêteur est de +8,6 dB médian (29 styles au-dessus de +6, jusqu'à +13,8 sur riddim), pour 4,7 LU de perte médiane dans l'écrêteur et le limiteur : la sonie vient aujourd'hui du limiteur, parce que le kick intégré (un sinus de 47 Hz qui traîne 1,5 s) a des crêtes énormes pour sa sonie. Le facteur de crête du Drop passe de 11,4 à 7,7 dB et Drop − Break de +1,4 à −0,2 LU : le Drop est plus écrasé que le Break. C'est le travail de l'étape 3 (kits échantillonnés, kick court) et de l'étape 6 (courbe d'énergie) : avec une batterie dense, le même volume demandera 4 à 6 dB de gain en moins.
- Le continu disparaît : la 303 a un bloqueur après sa saturation (la source principale, 0,07 sur acidcore), la chaîne master en a un à 3 Hz en tête et un sub-sonique à 0,05 Hz après le limiteur (l'écrêtage du côté haut des kicks en laissait 0,001 à 0,003). La basse du synthé laisse encore −0,01 avant master (sub à 20 Hz) : étape 7.
- PLR sous 6 dB sur acidcore et gabber : mécanique, une cible à −7 ou −6 LUFS avec un plafond visé à −1,2 dBTP ne laisse que 5,8 dB ; le tableau PLR de son.md § 2.4 est à relire pour ces familles.
- Air et pente n'ont pas bougé (−1 dB d'air : le limiteur travaille sur les transitoires) : étape 3.

## Étape 1 en bref (médianes sur 37 styles, sons intégrés)

| Mesure | Avant | Après | Écart | Cible |
|---|---|---|---|---|
| LUFS du Drop / Refrain | −10,9 | −12,1 | −1,2 | −8 à −6 en club |
| LUFS-I du morceau | −11,6 | −12,7 | −1,1 | cible du genre ±1 |
| True peak (dBTP) | −0,08 | −0,15 | −0,07 | ≤ −1,0 |
| Air > 6 kHz (dB rel.) | −22,8 | −19,7 | +3,2 | −20 à −16 |
| Pente 0,1–8 kHz (dB/oct) | −5,26 | −4,98 | +0,28 | −4,5 ±0,7 |
| Facteur de crête du Drop (dB) | 10,4 | 11,4 | +0,9 | ≥ 6 |
| Drop − Break (LU) | 0,2 | 1,4 | +1,2 | ≥ +4 |

- Plus d'air, une pente plus proche de la cible. Le passe-bas caché de la Saturation est parti, et les charleys du kit v2 sont de vrais charleys : clairs et au niveau de leur rôle.
- Les morceaux sont 1,2 LU moins forts : c'est voulu. Le compresseur n'ajoute plus +3,6 dB (master) et +4,2 dB (bus batterie) en douce, donc le limiteur travaille moins (facteur de crête +0,9 dB). Le volume de club viendra de la chaîne master de l'étape 2 (écrêteur, limiteur true peak, cible par genre). Elle partira de ces chiffres.
- Le true peak restait au-dessus de −1 dBTP partout : réglé à l'étape 2 (tableau ci-dessus).
