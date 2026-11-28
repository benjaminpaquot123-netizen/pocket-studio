# Cibles zone A : kicks et boucles de batterie

Pour l'équipe de production zone A. Mesures Essentia sur le stock (familles seulement, aucun son
individuel ici). Tableau complet p10–p90 : `zone-a-kicks-boucles.tsv`. Pour mesurer vos rendus avec le même
code : `scripts/son/essentia/cibles_desc.py` (wav/mp3 → json).

## Familles
- **kick** : one-shots et pistes de kit rangés « kick ».
- **boucle-<type>** : boucles de batterie du stock, par type (drums = complète, full, kick, snare, hats, perc).
- Styles de la page → stock : dnb/drum-and-bass → dnb, techno → techno, edm → edm, boom-bap → hiphop,
  drill → trap. Pas de case hiphop, trap ni edm assez fournie pour les kicks et boucles : viser la ligne
  « tous ». Une case existe seulement avec ≥ 8 sons et ≥ 2 packs.
- `bpm` n'est mesuré que pour les boucles (60–200) ; en dnb il est souvent lu à moitié ou aux deux tiers du vrai tempo : ne pas s'en servir comme cible.

## Ce que Benjamin a gardé ou rejeté (page précédente, ids de page)
- **Gardé** : batterie-elec__boom-bap-kick-01.
- **Rejetés (non)** : batterie-elec__drum-and-bass-kick-01, batterie-elec__drum-and-bass-kick-03,
  batterie-elec__edm-kick-01, batterie-elec__edm-kick-03, batterie-elec__techno-kick-01,
  batterie-elec__techno-kick-02 (note de Benjamin : « Elle sonne creu »), batterie-elec__boom-bap-boucle-01,
  batterie-elec__edm-boucle-01, batterie-elec__techno-boucle-01.
- **Bof** : batterie-elec__drill-boucle-01, batterie-elec__drum-and-bass-boucle-01.
- Pour info (autres zones de batterie, déjà réglées) : caisses claires et claps surtout gardés,
  charleys fermés drill/dnb gardés ; charleys edm/techno/boom-bap et percussions rejetés.

## Ce qui sépare « gardé » de « non »
- **Kick « creux »** : les kicks rejetés sont une sinusoïde presque pure : aucun bruit d'attaque (planéité
  tout en bas de l'échelle du stock, aigus au-dessus de 4 kHz quasi nuls, un seul pic spectral). Le kick
  gardé a un peu de bruit de frappe (planéité vers le milieu du stock). Cible : un clic ou un bruit
  de batte audible, planéité et part > 4 kHz au moins au p25 de la famille.
- **Queue** : tous nos kicks tiennent plus longtemps que tout le stock (tenue −20 dB au-delà du p90).
  Raccourcir la queue vers le p50 de la case.
- **Niveau** : les rejetés sont plus faibles (bas de l'échelle LUFS) et plus pointus (crête haute) ;
  le gardé est au milieu. Viser LUFS et crête au p50.
- **Boucles rejetées** : bien plus sombres que tout le stock (centroïde et coupure haute en bas de
  l'échelle), trop chargées en sub, plus faibles et mono. Cible : centroïde et part > 4 kHz au moins au p25,
  part sous 120 Hz au p50, un peu de largeur stéréo.

## Cibles par famille×style (p25–p50–p75 ; sons/packs = rNpN)
Viser la zone p25–p75, en penchant vers ce qui a été gardé.

| famille | style | sons/packs | dur_s p25–p50–p75 | decay20_s p25–p50–p75 | centroid_hz p25–p50–p75 | flatness_db p25–p50–p75 | high4k p25–p50–p75 | sub120 p25–p50–p75 | crest_db p25–p50–p75 | lufs p25–p50–p75 | width_db p25–p50–p75 | bpm p25–p50–p75 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| boucle-drums | tous | r536p12 | 5.5–6.4–7.7 | 0.07–0.12–0.21 | 742–1801–6349 | -24.8–-19.2–-12.3 | 0.03–0.10–0.62 | 0.000–0.05–0.71 | 9.6–16.7–19.8 | -16.8–-12.8–-8.5 | -26.1–-15.8–-10.4 | 115–125–151 |
| boucle-drums | dnb | r108p3 | 5.5–5.5–5.5 | 0.06–0.12–0.16 | 1622–3651–7508 | -23.1–-19.5–-16.6 | 0.07–0.26–0.86 | 0.000–0.004–0.60 | 8.8–15.7–19.4 | -13.9–-11.8–-9.2 | -74.7–-55.5–-12.9 | 86–116–116 |
| boucle-drums | dubstep | r24p2 | 1.7–1.7–1.7 | 0.10–0.17–0.20 | 420–646–1410 | -28.9–-24.1–-16.2 | 0.04–0.07–0.08 | 0.28–0.44–0.57 | 12.8–14.8–16.3 | -14.3–-12.8–-11.2 | -16.2–-12.7–-10.3 | — |
| boucle-drums | house | r44p2 | 7.7–7.7–7.7 | 0.13–0.19–0.22 | 362–468–624 | -29.3–-24.7–-21.6 | 0.02–0.02–0.03 | 0.79–0.81–0.85 | 6.6–7.3–7.8 | -8.4–-8.0–-7.6 | -28.4–-25.0–-22.9 | 123–124–125 |
| boucle-drums | techno | r119p3 | 6.4–6.4–6.4 | 0.07–0.21–2.5 | 723–3472–7195 | -17.8–-9.6–-5.9 | 0.09–0.27–0.59 | 0.000–0.002–0.67 | 6.4–17.7–19.7 | -16.1–-8.4–-5.0 | -15.8–-13.3–1.5 | 125–150–151 |
| boucle-full | tous | r217p9 | 6.2–6.9–8.3 | 0.07–0.13–0.20 | 667–2616–4593 | -22.6–-18.5–-13.9 | 0.02–0.06–0.13 | 0.54–0.71–0.85 | 8.0–10.0–12.9 | -12.5–-10.0–-8.6 | -28.9–-19.3–-13.1 | 100–116–140 |
| boucle-full | dnb | r52p2 | 5.5–5.5–5.5 | 0.09–0.13–0.19 | 2913–3956–4502 | -20.6–-18.6–-12.0 | 0.07–0.09–0.12 | 0.55–0.62–0.67 | 9.8–10.3–11.3 | -10.4–-9.2–-8.5 | -36.8–-23.9–-16.8 | 87–87–87 |
| boucle-full | dubstep | r40p2 | 6.9–6.9–6.9 | 0.17–0.21–0.26 | 695–1986–4895 | -21.1–-16.0–-13.5 | 0.03–0.06–0.14 | 0.53–0.74–0.85 | 8.6–10.5–12.2 | -10.4–-10.0–-9.1 | -15.2–-13.0–-11.9 | 140–140–140 |
| boucle-hats | tous | r362p8 | 6.4–7.5–8.0 | 0.05–0.07–0.15 | 6729–7608–8741 | -14.9–-12.7–-10.9 | 0.84–0.92–0.96 | 0.000–0.000–0.000 | 17.0–19.2–21.8 | -17.0–-14.8–-12.6 | -21.9–-11.9–-5.8 | 119–128–150 |
| boucle-hats | dubstep | r50p3 | 6.9–6.9–6.9 | 0.06–0.14–0.37 | 6661–7780–8526 | -14.8–-12.2–-10.9 | 0.81–0.93–0.97 | 0.000–0.000–0.001 | 17.0–18.6–20.9 | -15.5–-13.7–-12.0 | -15.0–-8.0–-4.4 | 116–140–140 |
| boucle-hats | house | r53p2 | 7.7–7.7–7.7 | 0.04–0.05–0.07 | 7384–8514–9600 | -18.5–-12.7–-10.0 | 0.91–0.95–0.97 | 0.000–0.000–0.000 | 16.9–18.2–19.5 | -16.2–-15.2–-14.4 | -123.0–-26.2–-12.3 | 123–124–125 |
| boucle-hats | techno | r53p2 | 6.4–6.4–6.4 | 0.08–0.15–0.18 | 6880–7448–8261 | -14.3–-12.3–-11.2 | 0.90–0.95–0.97 | 0.000–0.000–0.000 | 13.7–14.3–15.5 | -9.8–-9.0–-7.9 | -115.8–-22.3–-10.7 | 150–151–151 |
| boucle-kick | tous | r350p12 | 6.4–7.4–8.0 | 0.07–0.13–0.20 | 50–60–82 | -77.2–-63.6–-47.0 | 0.000–0.001–0.006 | 0.76–0.84–0.89 | 5.1–6.2–9.2 | -11.0–-9.2–-8.0 | -77.6–-37.5–-21.1 | 100–123–140 |
| boucle-kick | dnb | r24p2 | 5.5–5.5–6.9 | 0.07–0.09–0.12 | 52–59–64 | -60.2–-52.6–-44.8 | 0.001–0.003–0.006 | 0.77–0.82–0.85 | 7.4–8.2–9.5 | -12.8–-11.6–-11.0 | -36.3–-32.4–-26.8 | 82–84–86 |
| boucle-kick | dubstep | r44p3 | 6.9–6.9–6.9 | 0.15–0.20–0.24 | 73–181–585 | -46.9–-36.3–-23.7 | 0.006–0.01–0.04 | 0.62–0.77–0.86 | 6.9–8.6–10.3 | -10.4–-9.6–-8.9 | -17.4–-14.2–-12.1 | 87–139–140 |
| boucle-kick | house | r44p2 | 7.7–7.7–7.7 | 0.13–0.17–0.20 | 45–47–52 | -93.6–-78.0–-66.8 | 0.000–0.000–0.001 | 0.79–0.82–0.85 | 5.1–5.5–6.6 | -8.6–-8.2–-7.9 | -139.5–-77.3–-44.3 | 123–123–125 |
| boucle-kick | techno | r64p3 | 6.4–6.4–7.7 | 0.15–0.20–0.60 | 54–62–67 | -77.2–-69.9–-62.5 | 0.000–0.001–0.003 | 0.76–0.81–0.90 | 5.1–5.6–6.5 | -10.6–-5.7–-5.2 | -138.8–-73.3–-50.8 | 125–150–151 |
| boucle-perc | tous | r264p7 | 6.4–7.7–8.0 | 0.05–0.09–0.19 | 574–1155–2306 | -44.3–-32.6–-20.6 | 0.006–0.04–0.19 | 0.000–0.001–0.007 | 15.8–18.4–22.2 | -18.5–-15.9–-11.8 | -19.0–-9.8–-5.2 | 111–125–149 |
| boucle-perc | house | r44p2 | 7.7–7.7–7.7 | 0.03–0.04–0.07 | 917–1457–2329 | -30.5–-24.5–-20.1 | 0.02–0.08–0.21 | 0.000–0.001–0.003 | 19.3–21.0–22.4 | -18.4–-17.7–-16.7 | -13.9–-5.1–-3.3 | 124–125–125 |
| boucle-perc | techno | r45p2 | 6.4–6.4–6.4 | 0.14–0.18–0.26 | 2309–2783–3413 | -17.9–-16.0–-14.6 | 0.18–0.24–0.34 | 0.001–0.001–0.003 | 16.4–17.0–17.7 | -12.3–-11.1–-10.5 | -8.1–-6.5–-5.0 | 150–151–151 |
| boucle-snare | tous | r180p8 | 6.2–6.9–7.7 | 0.05–0.09–0.13 | 1816–2679–3546 | -23.0–-15.5–-12.0 | 0.12–0.21–0.30 | 0.000–0.000–0.003 | 13.3–15.7–19.3 | -17.4–-14.5–-12.5 | -94.1–-21.0–-11.8 | 79–100–125 |
| boucle-snare | dnb | r26p2 | 5.5–5.5–5.6 | 0.04–0.05–0.07 | 2564–3202–3935 | -15.7–-14.2–-12.8 | 0.19–0.26–0.33 | 0.000–0.000–0.001 | 14.0–14.9–16.1 | -15.8–-15.1–-14.3 | -17.3–-13.9–-11.2 | 87–87–87 |
| boucle-snare | house | r24p2 | 7.7–7.7–7.7 | 0.07–0.08–0.10 | 1854–2247–3377 | -18.9–-14.4–-10.8 | 0.15–0.22–0.30 | 0.000–0.000–0.001 | 14.0–14.9–17.7 | -16.0–-15.0–-14.2 | -125.2–-123.9–-122.6 | 125–125–125 |
| boucle-snare | techno | r44p3 | 6.4–6.6–7.7 | 0.06–0.10–0.14 | 2901–3642–4382 | -11.7–-9.7–-8.1 | 0.17–0.26–0.38 | 0.000–0.000–0.000 | 12.8–17.1–19.4 | -22.3–-16.9–-10.1 | -20.0–-14.2–-9.0 | 76–77–125 |
| kick | tous | r532p18 | 0.30–0.42–0.50 | 0.08–0.13–0.18 | 44–50–61 | -92.1–-72.7–-53.4 | 0.000–0.000–0.000 | 0.93–0.98–0.99 | 5.7–7.6–10.7 | -14.1–-11.6–-8.9 | -101.5–-39.7–-30.4 | — |
| kick | dnb | r58p2 | 0.24–0.34–0.34 | 0.09–0.12–0.13 | 44–50–55 | -70.0–-61.1–-50.9 | 0.000–0.000–0.000 | 0.98–0.99–1.00 | 6.2–7.5–8.9 | -12.4–-11.3–-10.3 | -121.0–-64.7–-37.4 | — |
| kick | dubstep | r80p2 | 0.29–0.50–0.50 | 0.17–0.19–0.21 | 44–48–53 | -101.3–-95.2–-89.7 | 0.000–0.000–0.000 | 0.99–0.99–1.00 | 5.5–6.0–7.1 | -11.4–-8.8–-8.1 | -38.6–-34.0–-31.0 | — |
| kick | hardstyle | r11p2 | 0.39–0.39–0.39 | 0.25–0.30–0.37 | 70–73–81 | -54.3–-42.1–-35.9 | 0.000–0.001–0.001 | 0.90–0.91–0.92 | 3.6–3.9–5.4 | -7.7–-6.8–-6.4 | -95.0–-49.6–-32.9 | — |
| kick | house | r21p2 | 0.48–0.48–0.48 | 0.13–0.17–0.19 | 43–45–46 | -98.0–-89.1–-74.3 | 0.000–0.000–0.000 | 0.93–0.96–0.96 | 4.8–5.1–5.8 | -8.8–-8.2–-8.0 | -136.3–-73.5–-49.6 | — |
| kick | lofi | r28p2 | 0.25–0.42–0.43 | 0.06–0.09–0.11 | 40–46–54 | -93.2–-47.1–-39.1 | 0.000–0.000–0.000 | 0.96–0.98–0.99 | 5.9–11.5–13.6 | -17.3–-14.3–-11.0 | -133.7–-105.9–-34.1 | — |
| kick | rave | r36p2 | 0.39–0.40–1.2 | 0.09–0.14–0.20 | 58–75–96 | -73.1–-56.3–-46.1 | 0.000–0.001–0.004 | 0.81–0.89–0.95 | 5.2–6.0–7.7 | -9.1–-8.7–-7.8 | -26.6–-22.2–-16.7 | — |
| kick | techno | r30p2 | 0.40–0.40–0.48 | 0.13–0.17–0.23 | 50–60–63 | -76.8–-72.0–-62.8 | 0.000–0.000–0.000 | 0.87–0.88–0.99 | 5.3–5.6–6.2 | -10.5–-7.3–-6.9 | -137.0–-77.1–-54.1 | — |
