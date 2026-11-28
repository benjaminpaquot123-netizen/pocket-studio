# Cibles zone C : synthés (stab, pluck, nappe, accords, boucles mélodiques, effets)

Pour l'équipe de production zone C. Mesures Essentia sur le stock (familles seulement, aucun son
individuel ici). Tableau complet p10–p90 : `zone-c-synthes.tsv`. Pour mesurer vos rendus avec le même
code : `scripts/son/essentia/cibles_desc.py` (wav/mp3 → json).

## Comment les familles sont tirées du stock (règles mesurées)
- **stab** : one-shots accords/synthé/clavier de moins de 0,6 s dont la tenue −20 dB est sous 0,35 s.
- **pluck**, **nappe** (pad), **accords** : one-shots du stock de ce type ; le reste des one-shots va dans
  **oneshot-synth** ou **oneshot-keys**.
- **boucle-<type>** : boucles mélodiques (lead, pad, synth, keys, pluck, chords). Sur la page :
  Arpège → boucle-synth, Lead → boucle-lead, Piano → boucle-keys.
- **fx-nappe** : effets de 2 s ou plus à attaque lente (≥ 200 ms), c'est-à-dire les nappes d'ambiance
  rangées avec la batterie ; **fx-effet** : les autres effets (montées, impacts, voix).
- Styles : dnb/drum-and-bass → dnb, techno → techno, edm → edm, boom-bap → hiphop, drill → trap.
  Une case existe seulement avec ≥ 8 sons et ≥ 2 packs ; sinon viser la ligne « tous » (c'est le cas
  de toutes les cases edm). Les familles stab (19 sons) et pluck (33) sont maigres : cibles indicatives.

## Ce que Benjamin a gardé ou rejeté (page précédente, ids de page)
- **Gardés** : synthes__dnb-accords-02, synthes__dnb-accords-03, synthes__edm-accords-02,
  synthes__techno-accords-01, synthes__techno-accords-03, synthes__dnb-pluck-02, synthes__dnb-pluck-03,
  synthes__edm-pluck-01, synthes__edm-pluck-03, synthes__techno-pluck-03, synthes__dnb-stab-03,
  synthes__edm-stab-01, synthes__edm-stab-03, synthes__techno-stab-01, synthes__dnb-pad-01,
  synthes__edm-pad-01, synthes__edm-arp-03, synthes__techno-arp-01, synthes__techno-arp-02,
  synthes__edm-lead-01, synthes__edm-lead-02, synthes__techno-lead-01, claviers-fx__edm-effet-04,
  claviers-fx__edm-piano-02.
- **Rejetés (non)** : synthes__dnb-arp-01, synthes__edm-arp-01, synthes__dnb-lead-01, synthes__dnb-lead-02,
  synthes__edm-pad-02, claviers-fx__edm-voix-01.
- **Bof** : synthes__edm-accords-01, synthes__dnb-arp-03, synthes__dnb-pad-02, synthes__techno-pad-01
  (« Craquement »), synthes__techno-lead-02, claviers-fx__edm-effet-01 (« Trop rapide la montée »),
  claviers-fx__edm-effet-03 (« Manque de kick »).

## Ce qui sépare « gardé » de « non »
- **Stab et pluck** : tous gardés, rien à corriger. Nos stabs étaient des boucles de plusieurs secondes,
  alors que la famille stab du stock est définie par sa brièveté : la comparer à elle n'apprend rien.
  Garder la recette des stabs gardés et viser une attaque nette, peu de grave (`low300` sous le p50).
  Les plucks gardés sont plus brillants que tout le stock (centroïde au-delà du p90).
- **Accords** : les gardés tiennent (tenue −20 dB au p50) et sont légers dans le grave (≈ p40) ;
  le « bof » est court (≈ p15) et lourd dans le grave (≈ p75).
- **Arpèges (boucle-synth)** : les gardés ont une attaque nette (≈ p15) ; les rejetés ont une attaque
  molle (≈ p70).
- **Leads** : les rejetés sont trop chargés en grave (≈ p70–p80 contre ≈ p35 pour les gardés).
- **Nappes (pad)** : les gardées démarrent vite et tiennent ; les rejetées ont une attaque plus lente et
  une tenue courte. Le « craquement » de techno-pad-01 est à corriger à l'oreille : la dissonance mesurée
  ne le distingue pas (écarts minimes, sans valeur de cible).
- **Effets** : l'effet gardé est fort (≈ p90 LUFS), long, léger dans le grave, avec du mouvement dans le
  grave ; les rejetés montent trop vite et sont sans grave rythmique (« manque de kick »).

## Précautions
- **Un pack domine** : pluck = 25 sons sur 33 d'un seul pack, et ce ne sont pas de vrais plucks (durée
  médiane ≈ 15 s, attaque ≈ 2 s) : ignorer les colonnes durée/attaque du pluck. Nappe = 40 sur 90 d'un pack.
- Les leads sont comparés dans des cases mélangées (styles différents, ligne « tous ») : écarts indicatifs.
- La dissonance n'est plus une cible : 71 % du stock est déjà au même niveau, les écarts sont du bruit.
- **Ce que valent les colonnes** : `width_db` très négatif = son mono (écrit « mono », la valeur n'a pas de sens) ;
  `attack_ms` se mesure par pas de 5 ms ; `n_peaks` plafonne à 60 (inutilisable comme cible quand la
  case est à 58–60) ; les parts d'énergie très faibles se lisent mieux en dB (`high4k_db`).
- **Rangs** : les positions « ≈ pNN » sont des rangs dans la case du stock ; nos fichiers de la page
  précédente faisaient tous à peu près la même longueur, donc leur durée ne se compare pas au stock (la tenue, elle, se compare entre sons de la page).

## Cibles par famille×style (p25–p50–p75 ; sons/packs = rNpN)
Viser la zone p25–p75, en penchant vers ce qui a été gardé.

| famille | style | sons/packs | dur_s p25–p50–p75 | attack_ms p25–p50–p75 | decay20_s p25–p50–p75 | centroid_hz p25–p50–p75 | high4k_db p25–p50–p75 | low300 p25–p50–p75 | lufs p25–p50–p75 | width_db p25–p50–p75 |
|---|---|---|---|---|---|---|---|---|---|---|
| accords | tous | r184p9 | 8.0–12.2–15.0 | 20–983–6479 | 0.73–2.4–6.1 | 312–606–1207 | -26–-18–-11 | 0.23–0.56–0.68 | -14.0–-12.1–-10.7 | -5.2–-0.58–1.3 |
| accords | dnb | r14p2 | 11.0–11.0–11.0 | 1979–3085–5320 | 0.55–1.2–7.4 | 558–830–1048 | -24–-14–-12 | 0.07–0.50–0.60 | -14.0–-12.9–-11.9 | -1.9–1.2–2.0 |
| boucle-chords | tous | r13p2 | 5.5–8.4–11.0 | 115–1187–6575 | 0.17–0.40–0.92 | 464–649–753 | -47–-44–-35 | 0.001–0.008–0.11 | -15.6–-14.0–-13.3 | -4.4–-3.5–-1.8 |
| boucle-keys | tous | r53p4 | 15.7–20.0–20.0 | 3377–7323–11948 | 0.69–1.2–2.1 | 335–447–551 | -52–-46–-39 | 0.08–0.22–0.47 | -16.6–-15.6–-13.8 | -6.7–-2.8–-1.2 |
| boucle-keys | lofi | r34p2 | 20.0–20.0–20.0 | 3724–10883–14128 | 0.77–1.3–2.5 | 389–483–556 | -50–-46–-39 | 0.06–0.15–0.36 | -17.2–-16.2–-15.0 | -5.6–-2.4–-1.1 |
| boucle-lead | tous | r245p9 | 7.7–7.7–11.0 | 1337–3552–5852 | 0.18–0.47–1.4 | 440–723–1331 | -31–-20–-13 | 0.03–0.24–0.50 | -14.4–-12.5–-10.1 | -9.3–-5.3–-0.61 |
| boucle-lead | dnb | r49p2 | 11.0–11.0–11.0 | 1502–4141–7418 | 0.26–0.69–1.4 | 792–1360–1985 | -21–-13–-8 | 0.002–0.02–0.19 | -12.4–-9.9–-8.7 | -3.4–-0.13–2.0 |
| boucle-lead | dubstep | r40p2 | 6.9–6.9–6.9 | 365–2844–3929 | 1.2–3.3–4.7 | 385–539–1397 | -32–-21–-13 | 0.14–0.40–0.57 | -13.2–-11.7–-10.4 | -8.9–-4.7–-2.0 |
| boucle-pad | tous | r118p8 | 7.7–10.3–13.7 | 1727–4864–8398 | 0.55–2.8–6.5 | 586–790–1229 | -34–-26–-17 | 0.004–0.09–0.33 | -12.8–-11.5–-10.3 | -2.9–-0.52–-0.01 |
| boucle-pad | techno | r25p2 | 6.4–7.7–7.7 | 1821–2888–5502 | 1.1–3.3–5.7 | 630–794–1140 | -30–-25–-16 | 0.003–0.04–0.26 | -12.5–-11.1–-9.5 | -3.2–-0.90–-0.10 |
| boucle-pluck | tous | r17p1 | 8.3–8.4–8.6 | 2200–4220–6296 | 0.15–0.20–0.26 | 567–843–1115 | -26–-17–-12 | 0.02–0.08–0.20 | -17.3–-15.2–-13.4 | -9.6–-6.2–-1.0 |
| boucle-synth | tous | r87p4 | 6.4–6.9–8.0 | 1559–3028–4974 | 0.17–0.27–0.68 | 579–891–1173 | -24–-16–-12 | 0.03–0.14–0.31 | -14.5–-11.9–-10.4 | -12.3–-10.1–-6.3 |
| fx-effet | tous | r286p18 | 2.6–5.4–7.7 | 10–25–110 | 0.28–0.77–1.6 | 322–1027–2347 | -28–-16–-7 | 0.009–0.18–0.80 | -14.7–-12.8–-10.3 | -8.2–-4.5–-1.2 |
| fx-effet | dnb | r29p2 | 5.5–7.9–11.0 | 15–50–180 | 0.12–0.38–3.3 | 191–507–3101 | -31–-26–-4 | 0.000–0.38–0.91 | -14.2–-13.6–-10.8 | -10.2–-7.1–-2.9 |
| fx-effet | dubstep | r62p3 | 2.0–2.6–3.4 | 5–15–39 | 1.2–1.9–3.0 | 1263–2092–3379 | -12–-9–-5 | 0.03–0.12–0.30 | -13.7–-10.9–-8.5 | -3.5–-1.2–-0.60 |
| fx-effet | rave | r14p2 | 6.2–6.2–6.2 | 115–147–170 | 0.15–0.39–0.56 | 93–127–3022 | -60–-32–-3 | 0.02–0.93–0.98 | -14.5–-12.2–-10.9 | -13.5–-3.0–-2.2 |
| fx-effet | techno | r24p2 | 5.0–6.4–6.4 | 5–15–32 | 0.56–0.85–1.2 | 315–1031–2555 | -31–-15–-4 | 0.006–0.06–0.76 | -17.9–-12.9–-11.7 | -8.2–-5.1–-3.7 |
| fx-nappe | tous | r500p20 | 5.1–7.7–13.8 | 858–2100–6114 | 0.40–1.1–3.1 | 565–1115–2426 | -24–-14–-7 | 0.002–0.11–0.53 | -13.3–-11.5–-9.8 | -4.3–-1.8–-0.04 |
| fx-nappe | dnb | r59p2 | 5.7–11.0–11.0 | 1372–3537–7066 | 0.20–1.00–2.2 | 658–1515–2319 | -30–-11–-5 | 0.000–0.02–0.18 | -13.4–-11.6–-8.8 | -8.3–-1.9–0.23 |
| fx-nappe | dubstep | r40p3 | 3.4–3.4–13.7 | 428–1093–2573 | 0.70–1.6–3.6 | 1202–1739–3462 | -13–-9–-6 | 0.03–0.25–0.60 | -11.1–-9.3–-8.6 | -2.6–-0.82–-0.02 |
| fx-nappe | house | r14p3 | 7.7–7.7–7.9 | 690–868–1974 | 0.12–0.44–0.84 | 2542–3891–5702 | -5–-3–-1 | 0.000–0.001–0.01 | -13.5–-12.2–-11.8 | -4.9–-2.0–0.09 |
| fx-nappe | lofi | r13p2 | 3.4–6.9–20.0 | 294–733–6346 | 0.08–0.41–1.0 | 851–1038–2294 | -15–-11–-7 | 0.15–0.20–0.29 | -19.7–-17.0–-14.5 | -10.4–-0.88–1.2 |
| fx-nappe | rave | r34p2 | 8.6–12.4–12.4 | 959–4163–7731 | 0.31–1.5–5.8 | 2229–2948–3964 | -8–-6–-3 | 0.000–0.002–0.01 | -12.3–-9.8–-8.1 | -16.1–-3.2–-0.07 |
| fx-nappe | techno | r16p2 | 3.7–4.8–6.4 | 938–1616–2518 | 0.05–0.38–0.78 | 1149–1813–3034 | -14–-6–-3 | 0.000–0.001–0.01 | -13.3–-12.4–-11.2 | -4.8–-2.3–-1.2 |
| fx-nappe | unlabelled | r102p2 | 16.0–16.0–16.0 | 5498–10324–12523 | 2.8–5.1–10.3 | 532–787–1064 | -30–-21–-16 | 0.15–0.29–0.53 | -12.0–-11.4–-10.8 | -2.3–-0.74–0.60 |
| nappe | tous | r90p11 | 11.2–13.7–16.0 | 1942–2906–6482 | 1.8–3.2–4.4 | 518–726–1053 | -41–-31–-23 | 0.010–0.22–0.57 | -12.3–-11.3–-10.6 | -1.8–-0.31–0.16 |
| nappe | ambient | r8p2 | 17.5–17.5–17.5 | 2216–8037–12727 | 0.15–0.34–2.4 | 634–793–1143 | -31–-26–-21 | 0.008–0.11–0.34 | -12.8–-11.7–-11.2 | -22.3–-16.0–-4.0 |
| nappe | dnb | r10p2 | 11.0–11.0–11.2 | 4078–5373–8993 | 3.2–6.9–7.4 | 228–270–570 | -53–-48–-38 | 0.18–0.70–0.77 | -11.0–-10.5–-9.9 | -1.3–-0.48–-0.08 |
| nappe | trap | r18p2 | 12.2–13.4–19.1 | 1393–4465–9745 | 1.8–2.9–4.8 | 548–738–839 | -39–-31–-21 | 0.006–0.29–0.48 | -12.6–-11.2–-10.0 | -6.1–-2.9–-0.87 |
| oneshot-keys | tous | r99p7 | 4.0–6.9–10.3 | 20–45–160 | 0.89–1.4–2.5 | 289–398–534 | -49–-44–-36 | 0.18–0.38–0.65 | -16.7–-15.6–-13.2 | -6.7–-4.3–-1.5 |
| oneshot-keys | lofi | r26p2 | 12.0–12.9–16.2 | 20–60–607 | 1.3–2.0–3.0 | 352–522–615 | -54–-45–-37 | 0.28–0.50–0.68 | -18.0–-15.6–-13.5 | -6.7–-5.2–-1.2 |
| oneshot-keys | trap | r20p2 | 6.9–7.6–10.2 | 10–167–854 | 0.98–1.6–3.0 | 393–497–643 | -44–-35–-27 | 0.005–0.36–0.80 | -15.3–-11.7–-9.8 | -8.1–-2.0–0.62 |
| oneshot-synth | tous | r123p11 | 1.6–2.3–3.1 | 10–20–237 | 0.21–0.51–0.81 | 363–618–1060 | -32–-22–-13 | 0.06–0.42–0.73 | -13.6–-11.6–-9.5 | -14.8–-9.3–-0.69 |
| oneshot-synth | house | r25p2 | 1.6–1.8–2.0 | 20–30–45 | 0.15–0.19–0.45 | 379–493–632 | -32–-23–-17 | 0.42–0.60–0.72 | -14.6–-13.2–-12.2 | -17.1–-10.5–-8.9 |
| oneshot-synth | techno | r22p2 | 1.9–2.2–2.7 | 0–5–10 | 0.21–0.37–0.43 | 325–616–877 | -33–-21–-15 | 0.03–0.08–0.46 | -15.5–-13.6–-12.4 | -15.9–-13.8–-10.4 |
| pluck | tous | r33p5 | 3.9–15.0–15.5 | 5–2065–7937 | 0.18–0.24–0.39 | 369–523–632 | -54–-47–-24 | 0.002–0.004–0.48 | -15.8–-14.1–-12.7 | -12.2–-8.0–-5.0 |
| stab | tous | r19p3 | 0.39–0.41–0.48 | 15–30–105 | 0.10–0.16–0.21 | 151–525–1048 | -46–-18–-10 | 0.35–0.69–0.86 | -14.0–-12.5–-11.1 | -14.6–-10.9–-3.3 |
