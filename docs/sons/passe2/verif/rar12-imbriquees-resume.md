# Contre-mesure de l'archive imbriquée de RAR 12 (pack p3, 22 lignes)

Méthode. Les 22 lignes viennent de l'archive rangée dans l'archive (`docs/sons/passe2/fin/imbriquees/n12_*.csv`). Leur `file_id` a été haché avec un nom de dossier d'ouverture qu'on ne connaît plus ; l'outil ne retrouvait donc pas le dossier. On a relancé `scripts/sons/tout.py` (non modifié) sur l'archive imbriquée rouverte hors dépôt, puis apparié chaque nouvelle ligne à l'ancienne par l'égalité de toutes les colonnes communes sauf `file_id` (la batterie a en plus `note_tag` côté ancien) : 22 sur 22, un pour un. La table de contre-mesure a été réindexée sur les nouveaux id le temps du calcul ; la colonne `cle` garde les anciens id, ceux des tables.

Lecture. Tout ce qui sert aux cibles est en accord à 100 % (durée, crête, RMS, centroïde, aigus, tonal, attaque/corps, t20, t60, type, sous-type). Seules les hauteurs sont en désaccord : hauteur d'attaque et tenue sur des percussions et claps bruités, f0 des coups mélodiques (5 vides côté table sur des sons d'accord, 2 « non » dont 39 Hz côté table : le même défaut sous 45 Hz que `r11-p1-2180e4d09e`). Ces colonnes sont déjà déclarées non fiables dans DESACCORDS-A/B/C et ne portent aucune cible. Un style lu « hardstyle » par la table et « rave » par le chemin (1 ligne) : désaccord de lecture du chemin, aucune cible par style ne repose sur ces 22 sons (pack p3 seul).

## Sortie de l’outil, RAR 12 imbriquée (outil `scripts/sons/passe2/verif/`)

Tolérances : `scripts/sons/passe2/verif/TOLERANCES.md`. « oui » = dans la tolérance ; « vide d'un côté » = une seule des deux mesures existe ; « octave » = f0 à une ou deux octaves près.

## Accord par type et colonne

| type | colonne | comparés | accord % | non | vide d'un côté | octave |
|---|---|---|---|---|---|---|
| drum_oneshot | centroid_hz | 13 | 100.0 | 0 | 0 | 0 |
| drum_oneshot | dur_s | 13 | 100.0 | 0 | 0 | 0 |
| drum_oneshot | hf_db | 13 | 100.0 | 0 | 0 | 0 |
| drum_oneshot | peak_dbfs | 13 | 100.0 | 0 | 0 | 0 |
| drum_oneshot | pitch_attack_hz | 8 | 0.0 ⚠ | 6 | 2 | 0 |
| drum_oneshot | pitch_steady_hz | 8 | 25.0 ⚠ | 4 | 2 | 0 |
| drum_oneshot | rms_dbfs | 13 | 100.0 | 0 | 0 | 0 |
| drum_oneshot | t20_ms | 13 | 100.0 | 0 | 0 | 0 |
| drum_oneshot | t60_ms | 13 | 100.0 | 0 | 0 | 0 |
| drum_oneshot | tonal_db | 13 | 100.0 | 0 | 0 | 0 |
| drum_oneshot | transient_body_db | 13 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:clap | centroid_hz | 1 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:clap | dur_s | 1 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:clap | hf_db | 1 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:clap | peak_dbfs | 1 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:clap | pitch_attack_hz | 1 | 0.0 ⚠ | 1 | 0 | 0 |
| drum_oneshot:clap | pitch_steady_hz | 1 | 0.0 ⚠ | 1 | 0 | 0 |
| drum_oneshot:clap | rms_dbfs | 1 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:clap | t20_ms | 1 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:clap | t60_ms | 1 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:clap | tonal_db | 1 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:clap | transient_body_db | 1 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:hat_closed | centroid_hz | 1 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:hat_closed | dur_s | 1 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:hat_closed | hf_db | 1 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:hat_closed | peak_dbfs | 1 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:hat_closed | rms_dbfs | 1 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:hat_closed | t20_ms | 1 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:hat_closed | t60_ms | 1 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:hat_closed | tonal_db | 1 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:hat_closed | transient_body_db | 1 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:hat_open | centroid_hz | 1 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:hat_open | dur_s | 1 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:hat_open | hf_db | 1 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:hat_open | peak_dbfs | 1 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:hat_open | rms_dbfs | 1 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:hat_open | t20_ms | 1 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:hat_open | t60_ms | 1 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:hat_open | tonal_db | 1 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:hat_open | transient_body_db | 1 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:kick | centroid_hz | 3 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:kick | dur_s | 3 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:kick | hf_db | 3 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:kick | peak_dbfs | 3 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:kick | pitch_attack_hz | 3 | 0.0 ⚠ | 1 | 2 | 0 |
| drum_oneshot:kick | pitch_steady_hz | 3 | 33.3 ⚠ | 0 | 2 | 0 |
| drum_oneshot:kick | rms_dbfs | 3 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:kick | t20_ms | 3 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:kick | t60_ms | 3 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:kick | tonal_db | 3 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:kick | transient_body_db | 3 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:perc | centroid_hz | 4 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:perc | dur_s | 4 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:perc | hf_db | 4 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:perc | peak_dbfs | 4 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:perc | pitch_attack_hz | 4 | 0.0 ⚠ | 4 | 0 | 0 |
| drum_oneshot:perc | pitch_steady_hz | 4 | 25.0 ⚠ | 3 | 0 | 0 |
| drum_oneshot:perc | rms_dbfs | 4 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:perc | t20_ms | 4 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:perc | t60_ms | 4 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:perc | tonal_db | 4 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:perc | transient_body_db | 4 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:snare | centroid_hz | 3 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:snare | dur_s | 3 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:snare | hf_db | 3 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:snare | peak_dbfs | 3 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:snare | rms_dbfs | 3 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:snare | t20_ms | 3 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:snare | t60_ms | 3 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:snare | tonal_db | 3 | 100.0 | 0 | 0 | 0 |
| drum_oneshot:snare | transient_body_db | 3 | 100.0 | 0 | 0 | 0 |
| oneshot_melodic | dur_s | 8 | 100.0 | 0 | 0 | 0 |
| oneshot_melodic | f0_hz | 8 | 12.5 ⚠ | 2 | 5 | 0 |
| oneshot_melodic | peak_dbfs | 8 | 100.0 | 0 | 0 | 0 |
| oneshot_melodic | rms_dbfs | 8 | 100.0 | 0 | 0 | 0 |
| vocal | dur_s | 1 | 100.0 | 0 | 0 | 0 |
| vocal | peak_dbfs | 1 | 100.0 | 0 | 0 | 0 |
| vocal | rms_dbfs | 1 | 100.0 | 0 | 0 | 0 |

## Accord par colonne, tous types

| colonne | comparés | accord % |
|---|---|---|
| dur_s | 22 | 100.0 |
| peak_dbfs | 22 | 100.0 |
| rms_dbfs | 22 | 100.0 |
| t20_ms | 13 | 100.0 |
| t60_ms | 13 | 100.0 |
| centroid_hz | 13 | 100.0 |
| hf_db | 13 | 100.0 |
| transient_body_db | 13 | 100.0 |
| tonal_db | 13 | 100.0 |
| pitch_steady_hz | 8 | 25.0 |
| pitch_attack_hz | 8 | 0.0 |
| f0_hz | 8 | 12.5 |

## Fichiers de l'archive contre lignes

| pack | audio | midi | lignes audio | lignes midi | écarts |
|---|---|---|---|---|---|
| p3 | 22 | 0 | 22 | 0 | files without row 0, rows without file 0, kit rows without folder 0 |

## Lecture indépendante des étiquettes (chemin du fichier)

- type : 22 / 22 (100.0 %)
- sous-type des coups de batterie : 13 / 13 (100.0 %)
- style (quand le chemin en nomme un) : 21 / 22 (95.5 %)

Confusions de sous-type (table → ma lecture), les plus fréquentes :

| table | ma lecture | n |
|---|---|---|

Paires surveillées : kick→kick_808 0, kick_808→kick 0, snare→clap 0, clap→snare 0, hat_closed→hat_open 0, hat_open→hat_closed 0

Styles (table → ma lecture) en désaccord :

| table | ma lecture | n |
|---|---|---|
| hardstyle | rave | 1 |

## Sons dont le chemin dit « 808 »

| pack | type | n |
|---|---|---|
