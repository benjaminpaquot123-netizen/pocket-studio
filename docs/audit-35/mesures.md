# Audit 35 : les mesures, style par style (l'« avant » des chantiers basses et leads)

Ce que joue le candidat de la PR #47 (`audit`, 0aa9bc7) **aujourd'hui**, mesuré pour servir de point de comparaison quand les chantiers basses et leads (branches à part, RETOURS du 24/10) seront fusionnés. Même méthode à refaire après : mêmes scripts, mêmes styles, mêmes extraits.

## Méthode

- **Écoute en direct, comme le téléphone** (règle de CLAUDE.md) : `scripts/live-bench.sh` (37 styles × 90 s ; `LiveSongRecorder` enregistre ce que l'appli envoie au moteur après ▶ de « Ton morceau est prêt ! », `ps_live` le rejoue dans le vrai `Engine` réglé comme le téléphone). Le premier enregistrement a été tué deux fois par le manque de mémoire de la machine partagée (`EOFException`, `oom-kill` sur le `java` du test) pendant `PreviewLevelBench`, **après** que les 37 `.pslive` eurent été écrits ; ils ont été rejoués avec `--no-record`. Comme 9 styles n'atteignent pas leur premier Drop en 90 s, un second passage de 150 s a été fait pour eux (`--styles techno,detroit,acid_techno,hard_techno,industrial_techno,retro_techno,minimal,trance,jungle --seconds 150`), `PreviewLevelBench` compris (toutes les écoutes ramenées à −16,0 LUFS). Journaux, `.pslive` et rapports : `scratchpad/audit-35/live/`.
- **Export** : `scripts/song-bench.sh --premaster --m4a --before docs/son/banc/apres-S5b.json` (37 styles, sons intégrés, aucun pack ; WAV effacés après mesure). Rapport complet et JSON : `scratchpad/audit-35/bench/`.
- **Mesures ajoutées pour l'audit** (`scratchpad/audit-35/tools/`) : chaque export rendu à nouveau par `ps_render`, mesuré puis effacé (`phone.py`) ; la piste lead rendue seule (`ps_render --solo <META lead>`, `lead.py`) ; les journaux du direct confrontés aux sections de l'export (`live.py`).
  - **Haut-parleur de téléphone** : passe-haut Butterworth d'ordre 4 à 300 Hz (modèle de « basses qui tapent ») sur les 8 premières mesures du premier Drop (ou Refrain). « Perte tél. » = sonie pondérée K pleine bande moins sonie après le filtre (plus c'est petit, mieux le Drop passe) ; « présence » = part de 1–4 kHz dans ce qui reste ; « 300–800 Hz » = part de la bande où un kick et une basse « tapent » au téléphone.
  - **Drops répétés** : empreinte = corrélation des énergies par double croche dans 4 bandes (< 150 Hz, 150 Hz–1 kHz, 1–5 kHz, > 5 kHz) entre les 8 premières mesures du Drop 1 et celles du Drop 2 (1,00 = même arrangement) ; Δ LU = sonie du Drop 2 moins celle du Drop 1.
  - **Direct contre export** : niveau court terme (3 s) du direct 4 s après le début du premier Drop, moins la sonie de la partie la plus forte de l'export (`loud_lufs`) ; niveau de l'Intro du direct à 4 s moins la sonie de l'Intro exportée (`intro_lufs`). Les deux lectures ne sont pas identiques (fenêtre de 3 s contre 20 % des fenêtres les plus fortes) : l'écart sur le Drop est surestimé d'environ 0,5 LU ; celui sur l'Intro ne l'est pas.
- **Extraits à écouter** (méthode de `abtools/abfinal.py`) : 30 s depuis 4 mesures avant le premier Drop (trap : Refrain), fondu 10 ms / 1 s, AAC 192 kbit/s, true peak décodé ≤ −1 dBTP ; plus une version « haut-parleur de téléphone » (même passe-haut 300 Hz, **même gain** : ce qui manque s'entend manquer). Fichiers : `scratchpad/audit-35/<style>-avant.m4a` et `<style>-avant-telephone.m4a` (techno, house, trap, jump_up, acid_techno, dubstep) ; détail dans `extraits.json`.

## En bref (médianes, 37 styles)

| Mesure | Médiane | Min – max | Cible | Commentaire |
|---|---|---|---|---|
| Sonie de la partie la plus forte (LUFS) | −8,5 | −12,1 – −7,0 | cible du style ±1 | 30/37 sans plafonnement ; plafonnés : dnb, dubstep, hard techno, jump up, neurofunk, riddim, **techno** (nouveau depuis S5b) |
| LUFS-I du morceau | −10,1 | −13,9 – −8,2 | – | |
| True peak export (dBTP) | −1,18 | −1,19 – −1,13 | ≤ −1,0 | 37/37 |
| True peak M4A décodé, 1er encodage → après vérification (dBTP) | +0,4 → −1,8 | −1,1 – +2,9 → −3,5 – −1,0 | ≤ −1,0 | 37/37 après vérification, mais… |
| M4A − WAV (LU) | **−3,0** | −5,4 – −0,5 | 0 | … le M4A partagé est 3 LU plus bas que le WAV (encodé plus bas pour tenir la crête) |
| Drop − Break exporté (LU) | +4,0 | +0,5 – +10,4 | ≥ +4 | 24/37 (S5b : 28/37 : house, deep house, tech house repassent sous +4) |
| Drop − Groove / Couplet (LU) | +1,8 | +0,5 – +6,2 | ≥ +2 | 15/37 |
| Drop − fin de Montée, le plus petit (LU) | +2,3 | +1,8 – +6,1 | ≥ +1 (bloquant) | 25/25 |
| LRA (LU) | 4,9 | 1,9 – 9,5 | 3 à 9 | |
| PLR (dB) | 8,9 | 7,0 – 12,7 | 6 à 10 | |
| Perte dans la chaîne master (LU) | 3,0 | 0,1 – 3,0 | ≤ 3 | 19 styles au plafond |
| Gain de la cible (dB) | +7,0 | −1,1 – +10,8 | – | la sonie reste en partie achetée au master |
| Côté/centre du Drop (dB) | **−18,3** | −35,8 – −7,4 | −15 à −9 | 8/37 dans la cible ; acid et house les plus étroits |
| Corrélation L/R | 0,96 | 0,74 – 1,00 | 0,3 à 0,85 | |
| Grave 60–250 Hz au Drop (dB rel.) | −3,7 | −7,1 – −0,9 | ≥ −6 en bass music | |
| Air > 6 kHz (dB rel.) | −17,4 | −24,2 – −13,0 | −20 à −16 | |
| Perte au haut-parleur de téléphone (LU) | 4,1 | 1,2 – 9,0 | – | hip-hop 5,7, house 4,8, techno 3,7, bass music 3,1 |
| Présence 1–4 kHz au téléphone (dB) | −6,5 | −11,7 – −2,4 | – | dubstep, dnb, afrobeats, disco les plus sourds |
| Drop 2 contre Drop 1 : empreinte | **0,97** | 0,65 – 1,00 | < 0,9 souhaité | 25/37 ≥ 0,95 ; Δ sonie médiane +0,0 LU |
| Direct : gain live max (dB) | +9,4 | +2,7 – +15,5 | – | export : +7,0 |
| Direct : Intro à 4 s − Intro exportée (LU) | **−6,5** | −10,5 – −0,8 | 0 | le gain live part de 0 dB et monte en 11 à 32 s |
| Direct : Drop − Drop exporté (LU) | **−1,4** | −3,5 – +3,3 | 0 | jump up −2,9, hard techno −3,5 |
| Direct : alertes (fondu, silence, clic, NaN) | 0 | – | 0 | 37/37 sur 90 s, 9/9 sur 150 s |
| Direct : premier Drop à (s) | 55–123 en club | 10–16 en rap | – | 9 styles après 90 s |

## Par famille (médianes)

| Famille | Partie forte (LUFS) | Drop − Break | Drop − Groove | Côté/centre Drop | Perte tél. | Présence tél. | Empreinte Drop 2 | Gain live max | Intro direct − export |
|---|---|---|---|---|---|---|---|---|---|
| Techno (10) | −8,1 | +3,0 | +1,7 | −15,5 | 3,7 | −6,2 | 0,98 | +9,9 | −5,8 |
| House (9) | −8,9 | +4,3 | +1,8 | −19,7 | 4,8 | −6,5 | 0,96 | +9,4 | −5,4 |
| Bass music (7) | −8,7 | +4,7 | +5,2 | −14,7 | 3,1 | −6,7 | 0,91 | +13,9 | −7,7 |
| Hip-hop (7) | −8,5 | – | +1,8 | −19,5 | 5,7 | −5,9 | 0,99 | +9,0 | −8,4 |
| Autres (4) | −9,0 | – | +1,0 | −16,0 | 4,6 | −6,5 | 0,98 | +7,0 | −8,1 |

## La piste lead seule (premier Drop, mesures où elle joue)

Cibles de `docs/hits/leads-pros.md` § 8 : brillance (4–10 kHz contre 1–4 kHz) −8 à −14 dB, boue (150–500 Hz contre 1–4 kHz) ≤ 0 dB, présence au téléphone ≥ −7 dB.

| Style | Mesures jouées / Drop | Centroïde (Hz) | Présence tél. (dB) | Brillance (dB) | Boue (dB) | Côté/centre (dB) |
|---|---|---|---|---|---|---|
| techno | 16/31 | 296 | −8,9 | −19,9 | +12,6 | −13,0 |
| house | 9/16 | 563 | −8,2 | −18,1 | +7,1 | −7,1 |
| tech_house | 8/15 | 525 | −18,9 | −23,8 | +17,4 | −12,4 |
| deep_house | 6/16 | 845 | −5,2 | −26,1 | −3,0 | −5,9 |
| trance | 32/32 | 608 | −9,1 | −12,3 | +7,2 | −6,3 |
| detroit | 31/31 | 584 | −7,2 | −20,4 | +5,4 | −7,5 |
| minimal | 1/32 | – | – | – | – | – |
| hard_techno | 17/32 | 594 | −8,5 | −12,8 | +7,5 | −6,4 |
| dnb | 16/31 | 602 | −8,4 | −11,8 | +6,4 | −6,2 |
| jump_up | 15/31 | 252 | −18,2 | −21,5 | +20,3 | −10,4 |
| liquid_dnb | 31/31 | 618 | −7,8 | −19,6 | +4,3 | −8,0 |
| neurofunk | 15/31 | 409 | −11,5 | −38,4 | +12,2 | −5,5 |
| dubstep | 16/31 | 644 | −9,3 | −17,4 | +6,8 | −5,4 |
| trap | 15/15 | 1278 | −2,1 | −18,6 | −10,0 | −5,4 |
| synthwave | 16/16 | 963 | −3,4 | −15,9 | −3,4 | −9,3 |
| pop | 16/16 | 507 | −12,2 | −16,3 | +11,9 | −8,1 |

Ce qu'on en lit : la brillance n'est dans la cible que pour la dnb, le hard techno et la trance (−11,8 à −12,8 dB) ; la boue est positive partout sauf deep house, trap et synthwave (+4 à +20 dB) ; le lead techno a son centre à **296 Hz** et celui du jump up (une 2e basse wobble) à 252 Hz ; dans 9 styles de club sur 12 mesurés, le lead ne joue que la **moitié** des mesures du premier Drop (coupé sur la première moitié, comme le « Drop 1A sans lead » de `docs/guide/diagnostic-jump-up.md` § 1.3). Minimal : la voie notée `META lead` ne joue qu'une mesure du Drop : pas de mesure.

## Extraits à écouter (l'« avant »)

| Style | Départ dans le morceau (s) | Drop à (s de l'extrait) | Gain appliqué (dB) | LUFS-I / TP de l'extrait | Version téléphone : LUFS-I (écart) |
|---|---|---|---|---|---|
| techno | 109,1 | 7,3 (Drop) | −3,7 | −13,3 / −2,3 | −17,8 (−4,5) |
| house | 55,1 | 7,9 (Drop) | −1,4 | −10,6 / −1,5 | −15,3 (−4,7) |
| trap | 6,9 | 6,9 (Refrain) | −1,6 | −11,2 / −1,6 | −17,5 (−6,3) |
| jump_up | 49,7 | 5,5 (Drop) | −4,0 | −13,6 / −3,1 | −14,7 (−1,1) |
| acid_techno | 104,3 | 7,0 (Drop) | −2,9 | −11,3 / −2,3 | −16,1 (−4,8) |
| dubstep | 61,7 | 6,9 (Drop) | −2,8 | −12,4 / −2,5 | −14,9 (−2,5) |

Fichiers : `/tmp/claude-0/-home-user-pocket-studio/2ed29bda-5e9a-5850-a6a7-bbf3293c7a09/scratchpad/audit-35/<style>-avant.m4a` et `<style>-avant-telephone.m4a`. Le gain négatif vient de l'AAC : la première version encodée dépasse −1 dBTP (jusqu'à +2,9 dBTP au banc), l'extrait est réencodé plus bas, comme le fait l'appli.

## Par style

### A. Export (master, sonie, contraste)

| Style | Partie forte (cible) | LUFS-I | Drop − Break | Drop − Groove | Drop − fin de Montée (min) | True peak | M4A : TP / écart LUFS | Gain cible / perte | LRA | PLR |
|---|---|---|---|---|---|---|---|---|---|---|
| **Techno** | | | | | | | | | | |
| techno | −8,6 (−8) | −10,0 | +4,0 | +1,8 | +3,0 | −1,17 | −1,8 / −3,3 | +6,8 / 3,0 | 3,8 | 8,8 |
| detroit | −8,0 (−8) | −9,0 | +4,5 | +0,7 | +2,2 | −1,18 | −2,6 / −4,1 | +5,9 / 2,1 | 4,7 | 7,8 |
| acid_techno | −7,6 (−8) | −8,3 | +3,0 | +0,9 | +2,1 | −1,18 | −1,8 / −3,6 | +7,8 / 2,5 | 3,1 | 7,1 |
| hard_techno | −8,4 (−6) | −10,2 | +2,2 | +2,6 | +2,4 | −1,19 | −1,4 / −2,6 | +9,0 / 3,0 | 4,3 | 9,0 |
| industrial_techno | −7,6 (−8) | −8,6 | +6,4 | +1,7 | +2,9 | −1,19 | −2,6 / −3,3 | +4,4 / 0,4 | 4,4 | 7,4 |
| retro_techno | −8,2 (−8) | −9,2 | +7,0 | +1,2 | +2,2 | −1,17 | −3,0 / −4,4 | +7,9 / 3,0 | 5,7 | 8,1 |
| acidcore | −7,9 (−8) | −8,6 | +0,5 | +0,7 | +2,3 | −1,16 | −1,0 / −2,1 | +2,6 / 3,0 | 2,5 | 7,4 |
| trance | −8,4 (−8) | −9,8 | +1,6 | +1,9 | +2,2 | −1,18 | −1,3 / −1,9 | +7,9 / 2,9 | 3,1 | 8,6 |
| minimal | −8,8 (−8) | −10,0 | +2,9 | +1,9 | +3,1 | −1,18 | −1,4 / −2,3 | +7,5 / 3,0 | 3,4 | 8,9 |
| gabber | −7,9 (−7) | −8,8 | +2,6 | +4,8 | +2,1 | −1,18 | −1,3 / −3,6 | +8,8 / 3,0 | 3,2 | 7,6 |
| **House** | | | | | | | | | | |
| house | −8,4 (−8) | −9,4 | +3,9 | +0,6 | +2,3 | −1,17 | −2,3 / −2,8 | +7,1 / 3,0 | 3,9 | 8,2 |
| deep_house | −9,0 (−9) | −10,8 | +3,6 | +2,2 | +3,7 | −1,18 | −1,9 / −0,9 | +4,9 / 1,2 | 4,1 | 9,6 |
| tech_house | −8,9 (−8) | −10,6 | +3,9 | +1,0 | +2,9 | −1,16 | −2,5 / −4,5 | +7,0 / 3,0 | 4,1 | 9,4 |
| acid_house | −8,1 (−8) | −9,1 | +2,3 | +0,5 | +1,8 | −1,17 | −2,2 / −3,1 | +7,4 / 2,7 | 2,4 | 7,9 |
| uk_garage | −8,4 (−8) | −9,3 | – | +1,0 | – | −1,18 | −2,9 / −3,6 | +7,6 / 3,0 | 3,5 | 8,1 |
| afro_house | −9,0 (−9) | −10,6 | +4,6 | +2,1 | +3,4 | −1,19 | −3,1 / −2,4 | +5,6 / 0,8 | 4,8 | 9,4 |
| speed_garage | −8,2 (−8) | −10,1 | +6,1 | +2,0 | +2,4 | −1,18 | −1,9 / −2,4 | +7,4 / 3,0 | 6,3 | 8,9 |
| amapiano | −9,0 (−9) | −11,9 | +10,4 | +5,0 | +6,1 | −1,19 | −1,5 / −0,5 | +5,8 / 0,8 | 8,9 | 10,7 |
| disco | −9,0 (−9) | −11,1 | +5,2 | +1,8 | +2,5 | −1,18 | −1,6 / −0,9 | +3,8 / 1,1 | 5,4 | 9,9 |
| **Bass music** | | | | | | | | | | |
| dnb | −8,5 (−8) | −10,2 | +4,4 | +5,4 | +2,3 | −1,19 | −1,4 / −3,0 | +7,3 / 3,0 | 5,3 | 9,0 |
| jump_up | −8,7 (−8) | −10,6 | +4,7 | +4,6 | +1,9 | −1,18 | −2,1 / −4,7 | +8,4 / 3,0 | 6,0 | 9,4 |
| dubstep | −8,8 (−8) | −10,4 | +3,5 | +5,2 | +2,3 | −1,19 | −1,3 / −2,8 | +8,4 / 3,0 | 6,2 | 9,2 |
| liquid_dnb | −8,4 (−8) | −10,3 | +5,8 | +5,2 | +2,0 | −1,18 | −1,1 / −2,8 | +7,5 / 2,9 | 6,7 | 9,2 |
| neurofunk | −8,8 (−8) | −10,3 | +5,6 | +6,0 | +2,0 | −1,19 | −3,5 / −5,4 | +9,3 / 3,0 | 8,2 | 9,1 |
| jungle | −8,0 (−8) | −9,8 | +6,2 | +1,3 | +3,4 | −1,17 | −1,1 / −3,2 | +7,8 / 3,0 | 5,7 | 8,6 |
| riddim | −9,8 (−8) | −11,4 | +4,0 | +6,2 | +2,4 | −1,18 | −1,8 / −3,8 | +10,8 / 3,0 | 7,4 | 10,3 |
| **Hip-hop** | | | | | | | | | | |
| trap | −8,5 (−8) | −10,0 | – | +1,5 | – | −1,19 | −1,9 / −1,7 | +5,4 / 1,4 | 6,9 | 8,8 |
| drill | −8,6 (−8) | −10,2 | – | +2,0 | – | −1,19 | −1,9 / −3,8 | +4,5 / 0,5 | 9,5 | 9,0 |
| phonk | −7,0 (−7) | −8,2 | – | +0,8 | – | −1,16 | −2,2 / −4,3 | +8,1 / 2,8 | 8,5 | 7,0 |
| boom_bap | −10,1 (−10) | −11,7 | – | +1,8 | – | −1,18 | −1,6 / −1,5 | +3,1 / 1,6 | 5,3 | 10,5 |
| grime | −8,5 (−8) | −10,2 | – | +2,2 | – | −1,17 | −1,5 / −1,7 | +4,2 / 1,0 | 5,0 | 9,1 |
| jersey_club | −8,5 (−8) | −9,8 | – | +1,3 | – | −1,18 | −1,4 / −3,4 | +6,1 / 2,1 | 7,0 | 8,6 |
| lofi | −12,1 (−12) | −13,9 | – | +2,7 | – | −1,19 | −1,6 / −0,6 | −1,1 / 0,1 | 9,3 | 12,7 |
| **Autres** | | | | | | | | | | |
| reggaeton | −9,0 (−9) | −9,9 | – | +1,0 | – | −1,18 | −1,4 / −3,3 | +4,9 / 2,2 | 3,1 | 8,7 |
| afrobeats | −9,2 (−9) | −10,3 | – | +0,9 | – | −1,13 | −1,1 / −1,0 | +5,6 / 3,0 | 4,9 | 9,2 |
| synthwave | −8,1 (−8) | −8,8 | – | +0,9 | – | −1,17 | −2,4 / −3,0 | +6,4 / 2,6 | 1,9 | 7,7 |
| pop | −9,0 (−9) | −10,3 | – | +2,3 | – | −1,18 | −1,7 / −2,8 | +3,4 / 1,2 | 3,2 | 9,1 |

### B. Le Drop : largeur, haut-parleur de téléphone, Drops répétés

| Style | Drop à (s) | Drops | Côté/centre Drop (dB) | Corrélation (morceau) | Grave 60–250 (dB rel.) | Air > 6 k (dB rel.) | Perte tél. 300 Hz (LU) | Présence 1–4 k au tél. (dB) | 300–800 Hz au tél. (dB) | Drop 2 / Drop 1 : empreinte · Δ LU |
|---|---|---|---|---|---|---|---|---|---|---|
| **Techno** | | | | | | | | | | |
| techno | 116 | 2 | −14,1 | 0,92 | −3,9 | −19,1 | 5,0 | −6,0 | −3,6 | 0,97 · +0,1 |
| detroit | 122 | 2 | −20,2 | 0,98 | −4,5 | −16,6 | 3,1 | −2,4 | −7,9 | 0,90 · −0,1 |
| acid_techno | 111 | 2 | −30,1 | 1,00 | −2,9 | −17,8 | 4,6 | −3,0 | −6,9 | 0,99 · +0,0 |
| hard_techno | 102 | 3 | −12,0 | 0,82 | −5,0 | −14,5 | 2,4 | −6,0 | −3,4 | 0,65 · +0,9 |
| industrial_techno | 113 | 2 | −16,6 | 0,96 | −3,3 | −18,1 | 3,5 | −8,4 | −2,8 | 1,00 · −0,1 |
| retro_techno | 120 | 2 | −20,5 | 0,98 | −3,5 | −16,4 | 4,4 | −4,9 | −5,6 | 0,99 · +0,0 |
| acidcore | 83 | 3 | −35,8 | 1,00 | −1,9 | −15,0 | 3,9 | −6,3 | −4,3 | 1,00 · −0,0 |
| trance | 111 | 2 | −10,9 | 0,85 | −4,3 | −16,0 | 3,2 | −7,5 | −2,9 | 0,97 · +0,0 |
| minimal | 120 | 2 | −11,8 | 0,87 | −3,7 | −22,3 | 5,8 | −8,6 | −3,0 | 0,99 · +0,2 |
| gabber | 55 | 3 | −14,3 | 0,89 | −5,2 | −16,6 | 3,4 | −7,3 | −2,8 | 0,84 · +0,1 |
| **House** | | | | | | | | | | |
| house | 63 | 2 | −23,3 | 0,98 | −3,4 | −16,9 | 4,8 | −7,8 | −2,9 | 0,98 · −0,2 |
| deep_house | 64 | 2 | −19,8 | 0,96 | −3,9 | −18,7 | 6,1 | −8,1 | −3,1 | 0,93 · +0,3 |
| tech_house | 61 | 2 | −18,9 | 0,97 | −3,7 | −15,4 | 3,9 | −5,7 | −4,1 | 0,91 · +0,1 |
| acid_house | 64 | 2 | −24,3 | 0,99 | −3,0 | −17,6 | 3,6 | −2,7 | −6,7 | 1,00 · +0,0 |
| uk_garage | 44 | 3 | −18,5 | 0,97 | −3,5 | −14,1 | 3,2 | −5,9 | −4,3 | 0,94 · −0,1 |
| afro_house | 63 | 2 | −17,5 | 0,96 | −1,8 | −19,0 | 7,1 | −6,5 | −5,6 | 0,97 · +0,0 |
| speed_garage | 59 | 2 | −20,9 | 0,98 | −3,1 | −15,9 | 5,2 | −7,0 | −4,4 | 0,96 · −0,1 |
| amapiano | 69 | 2 | −19,7 | 0,98 | −0,9 | −19,4 | 7,1 | −4,8 | −5,2 | 0,98 · +0,0 |
| disco | 66 | 2 | −17,0 | 0,93 | −3,7 | −17,2 | 4,1 | −9,6 | −3,2 | 0,95 · +0,2 |
| **Bass music** | | | | | | | | | | |
| dnb | 55 | 3 | −12,5 | 0,89 | −4,3 | −15,6 | 3,1 | −10,0 | −2,3 | 0,97 · +0,1 |
| jump_up | 55 | 3 | −7,4 | 0,74 | −7,1 | −13,5 | 1,2 | −6,5 | −3,0 | 0,91 · −0,0 |
| dubstep | 69 | 3 | −11,3 | 0,84 | −6,4 | −17,4 | 2,8 | −11,7 | −1,5 | 0,95 · +0,0 |
| liquid_dnb | 55 | 3 | −14,7 | 0,93 | −3,6 | −15,4 | 4,4 | −8,1 | −3,4 | 0,95 · +0,1 |
| neurofunk | 55 | 3 | −23,7 | 0,90 | −5,9 | −13,6 | 3,5 | −6,7 | −7,3 | 0,82 · +0,3 |
| jungle | 93 | 3 | −16,9 | 0,94 | −3,2 | −14,3 | 4,3 | −6,4 | −6,9 | 0,90 · −0,1 |
| riddim | 75 | 3 | −16,4 | 0,90 | −6,4 | −13,0 | 2,7 | −6,6 | −4,4 | 0,91 · +0,2 |
| **Hip-hop** | | | | | | | | | | |
| trap | 14 | 3 | −19,5 | 0,97 | −5,3 | −19,7 | 5,7 | −4,1 | −5,0 | 0,99 · −0,0 |
| drill | 14 | 3 | −21,0 | 0,98 | −3,0 | −20,1 | 6,5 | −5,9 | −4,4 | 0,98 · −0,0 |
| phonk | 15 | 3 | −22,8 | 0,99 | −5,0 | −19,7 | 5,0 | −5,1 | −3,8 | 0,99 · +0,0 |
| boom_bap | 10 | 3 | −19,2 | 0,98 | −3,8 | −20,0 | 7,0 | −7,4 | −4,2 | 0,99 · +0,1 |
| grime | 14 | 3 | −16,9 | 0,94 | −3,1 | −14,0 | 2,9 | −4,2 | −5,8 | 0,74 · −0,2 |
| jersey_club | 14 | 3 | −18,3 | 0,96 | −5,7 | −19,2 | 4,1 | −7,1 | −2,3 | 0,99 · −0,0 |
| lofi | 11 | 3 | −25,6 | 0,99 | −3,0 | −22,8 | 9,0 | −7,9 | −4,0 | 0,99 · +0,1 |
| **Autres** | | | | | | | | | | |
| reggaeton | 40 | 3 | −15,5 | 0,94 | −5,0 | −17,5 | 3,3 | −5,9 | −3,5 | 0,99 · +0,0 |
| afrobeats | 37 | 3 | −16,4 | 0,95 | −4,7 | −24,2 | 6,1 | −10,0 | −2,1 | 0,98 · +0,0 |
| synthwave | 37 | 3 | −15,4 | 0,94 | −3,5 | −19,1 | 3,7 | −4,4 | −4,2 | 0,99 · −0,1 |
| pop | 41 | 3 | −19,7 | 0,97 | −3,1 | −19,6 | 5,5 | −7,0 | −3,4 | 0,97 · −0,1 |

### C. Écoute en direct (ps_live, comme le téléphone)

| Style | Alertes | Gain live max (dB) | Intro : niveau à 4 s → après la montée du gain (dBFS c.t.), en s | Intro direct à 4 s − Intro exportée (LU) | Drop direct (4 s après) − partie forte exportée (LU) | Limiteur le plus profond (dB) |
|---|---|---|---|---|---|---|
| **Techno** | | | | | | |
| techno | 0 | +11,8 | −15,5 → −9,7 en 20 s | −4,7 | −1,0 | −7,9 |
| detroit | 0 | +6,8 | −13,1 → −8,8 en 22 s | −4,6 | −1,7 | −3,8 |
| acid_techno | 0 | +7,2 | −13,9 → −9,1 en 13 s | −6,1 | −1,6 | −4,8 |
| hard_techno | 0 | +11,1 | −21,9 → −13,1 en 16 s | −9,5 | −3,5 | −3,7 |
| industrial_techno | 0 | +5,1 | −12,9 → −9,9 en 11 s | −4,8 | −1,7 | −4,2 |
| retro_techno | 0 | +9,2 | −14,8 → −10,0 en 31 s | −5,8 | −1,4 | −6,1 |
| acidcore | 0 | +2,7 | −10,7 → −9,9 en 14 s | −2,8 | −2,6 | −6,3 |
| trance | 0 | +12,3 | −17,3 → −9,9 en 20 s | −6,4 | −1,7 | −5,7 |
| minimal | 0 | +11,5 | −16,3 → −9,8 en 23 s | −5,8 | −1,4 | −5,0 |
| gabber | 0 | +10,5 | −16,6 → −8,7 en 16 s | −8,0 | −1,5 | −8,5 |
| **House** | | | | | | |
| house | 0 | +9,4 | −14,9 → −9,6 en 22 s | −5,5 | −1,2 | −7,0 |
| deep_house | 0 | +9,7 | −15,7 → −10,2 en 18 s | −4,0 | −0,5 | −4,7 |
| tech_house | 0 | +8,7 | −18,9 → −12,2 en 14 s | −6,8 | −1,9 | −8,3 |
| acid_house | 0 | +9,4 | −14,7 → −9,7 en 16 s | −5,2 | −2,0 | −1,6 |
| uk_garage | 0 | +9,5 | −16,8 → −12,8 en 15 s | −6,7 | −1,8 | −6,1 |
| afro_house | 0 | +8,1 | −16,0 → −10,8 en 20 s | −5,4 | −0,3 | −6,4 |
| speed_garage | 0 | +10,4 | −15,5 → −10,0 en 21 s | −5,0 | −1,7 | −5,4 |
| amapiano | 0 | +12,6 | −19,4 → −11,1 en 19 s | −6,0 | −0,5 | −7,3 |
| disco | 0 | +8,4 | −15,5 → −10,0 en 24 s | −3,2 | −2,3 | −6,1 |
| **Bass music** | | | | | | |
| dnb | 0 | +13,9 | −18,4 → −11,4 en 19 s | −7,0 | −1,7 | −5,4 |
| jump_up | 0 | +11,1 | −22,1 → −14,5 en 32 s | −8,5 | −2,9 | −5,7 |
| dubstep | 0 | +15,1 | −20,4 → −12,5 en 20 s | −8,4 | −1,3 | −5,6 |
| liquid_dnb | 0 | +13,5 | −18,2 → −11,4 en 25 s | −6,5 | −1,5 | −4,9 |
| neurofunk | 0 | +14,0 | −18,4 → −11,1 en 23 s | −7,1 | −2,1 | −5,8 |
| jungle | 0 | +11,6 | −21,0 → −11,0 en 27 s | −9,2 | −1,2 | −4,7 |
| riddim | 0 | +15,5 | −21,8 → −15,0 en 20 s | −7,7 | −1,9 | −6,0 |
| **Hip-hop** | | | | | | |
| trap | 0 | +9,1 | −22,7 → −15,6 en 15 s | −7,9 | +0,7 | −6,8 |
| drill | 0 | +9,0 | −26,0 → −18,5 en 14 s | −8,4 | +0,9 | −4,9 |
| phonk | 0 | +10,1 | −25,7 → −17,8 en 15 s | −10,5 | −0,6 | −7,8 |
| boom_bap | 0 | +6,1 | −19,7 → −14,2 en 12 s | −3,9 | −0,9 | −6,1 |
| grime | 0 | +8,3 | −22,8 → −14,3 en 14 s | −10,4 | −1,5 | −4,7 |
| jersey_club | 0 | +9,1 | −22,5 → −14,5 en 15 s | −8,8 | −0,4 | −6,1 |
| lofi | 0 | +7,1 | −23,2 → −13,1 en 13 s | −0,8 | +3,3 | −4,3 |
| **Autres** | | | | | | |
| reggaeton | 0 | +6,9 | −19,3 → −12,0 en 13 s | −7,7 | −0,4 | −6,1 |
| afrobeats | 0 | +7,1 | −27,5 → −10,3 en 13 s | −10,2 | −0,1 | −4,8 |
| synthwave | 0 | +7,2 | −17,9 → −11,7 en 15 s | −8,5 | −1,4 | −4,6 |
| pop | 0 | +5,5 | −15,3 → −12,9 en 11 s | −3,0 | −0,2 | −4,9 |

## Les notes (rappel, sans écoute)

`docs/son/MELODIE.md` (`MelodyReportTest`) : les 35 leads écrits et les 37 basses passent M1–M20 sur 4 ambiances. Ce que les règles laissent passer et qu'on entend :
- **Notes de l'accord sur les temps forts (M9) : 1,00 pour 27 leads sur 35** (0,91 à 0,98 pour les 8 autres) (les hits : 0,55 à 0,85) : pas d'appogiature, pas de 9e tenue, tout « tombe juste ».
- **Plage de vélocité du lead : 0,46 dans 13 styles sur 35, 0,44 à 0,46 dans 17** (même profil 1,0 / 0,82 / 0,68 partout, `MelodyBuilder`) : la nuance ne dépend pas du style.
- **Basse : 2 à 4 hauteurs** en techno, acid techno, acidcore, acid house, minimal, neurofunk, riddim : correct pour le genre, mais 8 répétitions par Drop (voir les Drops répétés, tableau B).

## À refaire après les chantiers basses et leads

Les mêmes commandes, sur le même ordinateur si possible : `scripts/live-bench.sh` (90 s puis 150 s pour les 9 styles longs), `scripts/song-bench.sh --premaster --m4a --before scratchpad/audit-35/bench/bench.json`, puis `phone.py`, `lead.py`, `live.py`, `mdtables.py` et `extracts.py` (même départ : 4 mesures avant le premier Drop). Les extraits « après » se comparent à niveau égal (même gain si les true peaks le permettent).
