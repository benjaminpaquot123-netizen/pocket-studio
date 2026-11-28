# Cibles zone B : basses (phrase, sub, reese, 808, basse synthé)

Pour l'équipe de production zone B. Mesures Essentia sur le stock (familles seulement, aucun son
individuel ici). Tableau complet p10–p90 de chaque descripteur : `zone-b-basses.tsv`.

## Comment les familles sont tirées du stock (règles mesurées)
- **808** : les one-shots rangés « 808 » dans le stock.
- **sub-basse** : part sous 120 Hz ≥ 0,80, au plus 6 pics spectraux (son quasi pur), centroïde < 250 Hz.
- **basse-reese** : durée ≥ 1 s, part sous 120 Hz < 0,80, part sous 300 Hz ≥ 0,35, battement du grave
  (écart/moyenne de l'enveloppe basse) ≥ 0,25, au moins 10 pics (désaccord riche), tenue ≥ 0,8 s à −20 dB.
- **phrase-de-basse** : les boucles de basse restantes. **basse-synthe** : les one-shots restants.
- Styles de la page → stock : dnb/drum-and-bass → dnb, techno → techno, boom-bap → hiphop, drill → trap.
  Le stock n'a pas de basse « edm » : pour edm, viser la ligne « tous » de la famille.
- Une case famille×style n'existe que si ≥ 8 sons et ≥ 2 packs ; sinon on prend la ligne « tous ».

## Ce que Benjamin a gardé ou rejeté (page précédente, ids de page)
- **Gardés** : basses__dnb-reese-02, basses__dnb-reese-03, basses__edm-reese-01, basses__edm-reese-02,
  basses__dnb-sub-01, basses__techno-sub-01, basses__techno-sub-02, basses__techno-phrase-01.
- **Rejetés (non)** : basses__edm-basse-synthe-02, basses__edm-basse-synthe-03,
  basses__techno-basse-synthe-01, basses__techno-basse-synthe-03, basses__edm-phrase-01.
- **Bof** : basses__dnb-phrase-01, basses__dnb-sub-02.

## Ce qui sépare « gardé » de « non » (positions sur l'échelle du stock)
- **Reese** : les 4 gardées sont toutes très larges en stéréo et lourdes dans le sub, mais plus sombres et
  plus courtes que la reese du stock. Garder la largeur et le poids ; viser une durée et une brillance
  proches du milieu (p50) du stock.
- **Sub** : les gardés tiennent (tenue −20 dB nettement plus longue), sont forts (haut de l'échelle LUFS)
  et gardent un peu d'harmoniques (coupure haute vers p25–p50, pas tout en bas). Le « bof » est un sub
  plat, court, sans vie : à éviter.
- **Basse synthé** : les 4 rejetées sont trop dans le sub (≈ p90 de la famille), mono et ternes
  (centroïde vers p10). Viser le milieu de la famille : plus de médiums, un peu de largeur.
- **Phrase** : la gardée a une attaque franche (≈ 15 ms) et un grave stable, plus brillante que le stock.
  La rejetée a une attaque très lente et un grave qui « bat » ; la « bof » bat aussi. Règle : attaque
  nette, grave sans ondulation.
- Les originaux (boucles de 8 s) sont en général plus sombres que le stock.

## Cibles par famille×style (p25–p50–p75 ; sons/packs = rNpN)
Viser la zone p25–p75, en penchant vers ce qui a été gardé (ci-dessus).

| famille | style | sons/packs | dur_s p25–p50–p75 | attack_ms p25–p50–p75 | decay20_s p25–p50–p75 | centroid_hz p25–p50–p75 | sub120 p25–p50–p75 | width_db p25–p50–p75 | lufs p25–p50–p75 | n_peaks p25–p50–p75 |
|---|---|---|---|---|---|---|---|---|---|---|
| 808 | tous | r23p2 | 1.6–1.9–2.3 | 50–165–274 | 0.79–1.3–1.6 | 37–44–65 | 0.99–1.00–1.00 | -163.3–-160.7–-149.2 | -11.8–-9.8–-8.2 | 3.0–3.0–5.5 |
| basse-reese | tous | r52p8 | 1.7–6.9–11.6 | 178–1324–6010 | 1.2–1.6–3.0 | 311–1546–2005 | 0.34–0.36–0.47 | -11.7–-10.2–-5.9 | -10.3–-4.2–-3.1 | 58.0–60.0–60.0 |
| basse-reese | dubstep | r35p2 | 1.7–3.4–6.9 | 157–464–2734 | 1.3–1.6–3.1 | 1546–1794–2122 | 0.34–0.35–0.38 | -11.6–-10.7–-9.3 | -4.2–-3.2–-2.9 | 60.0–60.0–60.0 |
| basse-synthe | tous | r635p16 | 0.86–0.86–1.7 | 65–160–469 | 0.27–0.49–0.80 | 323–1133–1871 | 0.36–0.51–0.65 | -16.6–-11.8–-8.5 | -7.9–-5.0–-3.5 | 48.8–59.5–60.0 |
| basse-synthe | dnb | r29p2 | 0.69–0.69–1.8 | 25–224–649 | 0.17–0.25–0.47 | 91–188–274 | 0.16–0.52–0.80 | -32.5–-7.5–-0.91 | -13.0–-11.7–-10.3 | 12.0–15.5–19.5 |
| basse-synthe | dubstep | r473p2 | 0.86–0.86–0.86 | 70–185–469 | 0.29–0.56–0.79 | 989–1369–2260 | 0.34–0.47–0.58 | -14.0–-11.1–-8.3 | -5.5–-4.4–-3.1 | 59.0–60.0–60.0 |
| basse-synthe | house | r24p2 | 0.81–1.7–7.8 | 20–27–64 | 0.19–0.27–0.72 | 49–61–91 | 0.84–0.89–0.95 | -33.4–-18.8–-10.6 | -14.9–-13.4–-12.6 | 8.0–8.2–14.2 |
| basse-synthe | lofi | r8p2 | 12.6–13.1–14.3 | 42–70–8714 | 0.41–1.2–6.4 | 69–71–75 | 0.89–0.94–0.95 | -112.8–-109.2–-104.6 | -12.4–-10.0–-7.0 | 8.0–10.0–11.1 |
| basse-synthe | techno | r11p2 | 0.48–0.80–0.80 | 47–180–254 | 0.07–0.13–0.24 | 64–69–72 | 0.83–0.93–0.95 | -131.8–-130.6–-113.3 | -15.2–-13.2–-12.0 | 7.0–8.5–11.5 |
| basse-synthe | trap | r21p2 | 5.1–6.9–9.0 | 165–2280–4245 | 0.92–1.7–3.4 | 81–88–133 | 0.63–0.80–0.88 | -71.7–-12.2–-2.3 | -10.9–-9.4–-7.9 | 7.0–10.5–14.5 |
| phrase-de-basse | tous | r437p18 | 6.9–8.6–13.0 | 1093–3722–6261 | 0.16–0.33–1.4 | 71–104–284 | 0.60–0.80–0.89 | -49.1–-19.8–-10.3 | -13.0–-10.9–-7.9 | 11.0–18.5–53.0 |
| phrase-de-basse | dnb | r70p2 | 11.0–11.0–11.0 | 1185–3739–6932 | 0.33–1.4–5.6 | 94–120–210 | 0.61–0.78–0.86 | -29.1–-16.9–-4.9 | -11.7–-10.7–-9.1 | 15.6–27.0–51.9 |
| phrase-de-basse | dubstep | r91p2 | 3.4–3.4–6.9 | 529–1966–3978 | 0.27–0.61–2.0 | 859–1483–2545 | 0.32–0.49–0.63 | -14.0–-11.4–-9.6 | -6.3–-5.0–-3.3 | 57.5–60.0–60.0 |
| phrase-de-basse | house | r49p2 | 7.7–15.4–15.5 | 1112–4210–7418 | 0.16–0.24–1.5 | 50–66–76 | 0.83–0.86–0.94 | -50.2–-27.2–-13.1 | -13.6–-12.1–-10.5 | 8.5–10.0–13.0 |
| phrase-de-basse | lofi | r8p3 | 20.0–20.0–20.0 | 6195–11534–18755 | 0.16–0.30–0.58 | 55–64–72 | 0.90–0.95–0.98 | -107.7–-105.1–-89.9 | -15.7–-13.7–-11.0 | 7.9–9.0–11.6 |
| phrase-de-basse | techno | r43p2 | 7.7–7.7–7.7 | 2913–4265–6114 | 0.10–0.16–0.22 | 64–71–85 | 0.83–0.87–0.93 | -91.2–-42.5–-26.6 | -14.6–-13.0–-11.7 | 9.0–13.0–17.5 |
| phrase-de-basse | trap | r21p2 | 20.0–20.0–20.0 | 2574–5063–11589 | 0.18–0.64–1.1 | 75–85–94 | 0.83–0.86–0.90 | -69.4–-17.2–-2.0 | -12.8–-11.6–-10.3 | 9.0–13.0–19.5 |
| sub-basse | tous | r206p17 | 3.0–6.4–10.0 | 15–177–3273 | 0.14–0.37–3.0 | 52–65–71 | 0.92–0.98–1.00 | -97.1–-83.2–-34.9 | -12.2–-10.5–-7.8 | 3.0–3.8–5.0 |
| sub-basse | dnb | r14p3 | 0.96–11.0–11.0 | 253–1604–3746 | 0.26–0.35–4.4 | 48–54–72 | 0.96–0.96–0.99 | -38.5–-24.2–-19.1 | -10.1–-9.1–-8.2 | 3.0–3.0–4.9 |
| sub-basse | house | r40p2 | 0.98–2.0–7.7 | 15–45–3818 | 0.18–0.22–0.48 | 45–56–65 | 0.92–0.98–0.99 | -66.4–-39.9–-26.0 | -13.3–-12.2–-11.3 | 3.0–4.0–4.6 |
| sub-basse | lofi | r11p2 | 11.8–20.0–20.0 | 45–140–5759 | 0.41–0.72–0.94 | 50–66–67 | 0.94–0.99–1.00 | -114.5–-102.1–-86.8 | -12.9–-10.6–-9.2 | 2.5–3.0–4.8 |
| sub-basse | techno | r46p2 | 0.80–6.4–6.4 | 200–332–3466 | 0.06–0.07–0.12 | 55–62–67 | 0.98–0.99–1.00 | -135.1–-133.4–-130.8 | -12.7–-11.5–-10.8 | 3.0–3.8–4.5 |
