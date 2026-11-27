# Anciennes cibles de la batterie et cibles définitives

Origine des anciennes cibles (`docs/sons/matin/cibles-batterie.tsv`, en 5 lignes) :
1. Aucun script ni aucune donnée ne les a produites dans le dépôt : les commits 89a1e56c, 581b692f et ea3cf655 n'apportent que le texte de CIBLES-BATTERIE.md.
2. Les médianes et p10/p90 retombent pourtant, à l'arrondi près, sur les tables de la passe 2 (`docs/sons/passe2/tout/`), avec techhouse = house + techno : elles ont été lues sur ces mêmes tables, à la main.
3. Les p25/p75 « estime » viennent d'une loi normale (médiane ∓ 0,526 × l'écart au p10/p90), pas des sons.
4. Le nombre de packs n'était pas écrit (« ? ») et aucune condition (nombre de sons, de packs, contre-mesure) n'était vérifiée.
5. t40 et la note n'ont pas de contre-mesure fiable : t40 n'est pas recompté, la note n'est d'accord que sur une partie des kicks.

Comparaison ligne par ligne (« d'accord » : la médiane de l'une tombe entre le p25 et le p75 de l'autre).
Cibles définitives : `docs/sons/fin/cibles-batterie-definitives.tsv` ; t20 → t20_ms, note → pitch_steady_hz.

| famille | style | mesure | ancienne médiane (p25–p75) | nouvelle médiane (p25–p75) | verdict |
|---|---|---|---|---|---|
| kick | house | t20 | 219 (195.3–239.5) | — | pas de cible : (2) |
| kick | house | t40 | 268 (244.3–298.5) | — | plus de cible (t40 non contre-mesuré) |
| kick | house | note | 42 (39.4–45.2) | — | pas de cible : (2) |
| kick | techhouse | t20 | 250 (218–375) | 250.48 (218.46–374.5875) | d'accord |
| kick | techhouse | t40 | 313 (265.7–356.7) | — | plus de cible (t40 non contre-mesuré) |
| kick | techhouse | note | 48 (44.3–54.3) | — | pas de cible : (4) |
| kick | techno | t20 | 373 (284.6–380.9) | — | pas de cible : (5) |
| kick | techno | t40 | 394 (313–396.1) | — | plus de cible (t40 non contre-mesuré) |
| kick | techno | note | 51 (47.3–57.8) | — | pas de cible : (4) |
| kick | trap | t20 | 97 (83.3–111.7) | — | pas de cible : (2) |
| kick | trap | t40 | 129 (111.6–143.2) | — | plus de cible (t40 non contre-mesuré) |
| kick | trap | note | 46 (40.2–49.7) | — | pas de cible : (2) |
| kick | dnb | t20 | 129 (113.7–157.4) | 158.855 (125.8275–181.915) | d'accord |
| kick | dnb | t40 | 178 (164.8–221.7) | — | plus de cible (t40 non contre-mesuré) |
| kick | dnb | note | 52 (47.3–58.8) | — | pas de cible : (4) |
| kick | dubstep | t20 | 226 (207.1–244.4) | 226.475 (208.695–238.205) | d'accord |
| kick | dubstep | t40 | 284 (257.2–317.1) | — | plus de cible (t40 non contre-mesuré) |
| kick | dubstep | note | 48 (43.8–51.7) | — | pas de cible : (4) |
| 808 | house | t20 | 1456 (1105.7–1891) | — | pas de cible : (1) |
| 808 | house | t40 | 1766 (1306.8–2299.9) | — | plus de cible (t40 non contre-mesuré) |
| 808 | techhouse | t20 | 1456 (1105.7–1891) | — | pas de cible : (1) |
| 808 | techhouse | t40 | 1766 (1306.8–2299.9) | — | plus de cible (t40 non contre-mesuré) |
| 808 | techno | t20 | 1456 (1105.7–1891) | — | pas de cible : (1) |
| 808 | techno | t40 | 1766 (1306.8–2299.9) | — | plus de cible (t40 non contre-mesuré) |
| 808 | trap | t20 | 1456 (1105.7–1891) | — | pas de cible : (1) |
| 808 | trap | t40 | 1766 (1306.8–2299.9) | — | plus de cible (t40 non contre-mesuré) |
| 808 | dnb | t20 | 1456 (1105.7–1891) | — | pas de cible : (1) |
| 808 | dnb | t40 | 1766 (1306.8–2299.9) | — | plus de cible (t40 non contre-mesuré) |
| 808 | dubstep | t20 | 1456 (1105.7–1891) | — | pas de cible : (1) |
| 808 | dubstep | t40 | 1766 (1306.8–2299.9) | — | plus de cible (t40 non contre-mesuré) |
| clap | house | t20 | 78 (69–91) | — | pas de cible : (2) |
| clap | house | t40 | 142 (111.5–179.9) | — | plus de cible (t40 non contre-mesuré) |
| clap | techhouse | t20 | 102 (78–125) | 101.79 (77.6375–124.6675) | d'accord |
| clap | techhouse | t40 | 168 (139.6–199.6) | — | plus de cible (t40 non contre-mesuré) |
| clap | techno | t20 | 116 (81–148) | — | pas de cible : (5) |
| clap | techno | t40 | 175 (157.6–209.2) | — | plus de cible (t40 non contre-mesuré) |
| clap | trap | t20 | 93 (69–123) | — | pas de cible : (1) |
| clap | trap | t40 | 136 (107.1–180.7) | — | plus de cible (t40 non contre-mesuré) |
| clap | dnb | t20 | 70 (71–121) | — | pas de cible : (5) |
| clap | dnb | t40 | 129 (104.3–157.4) | — | plus de cible (t40 non contre-mesuré) |
| clap | dubstep | t20 | 159 (110–213) | 158.695 (109.765–213.4925) | d'accord |
| clap | dubstep | t40 | 266 (205.5–369.6) | — | plus de cible (t40 non contre-mesuré) |
| caisse_claire | house | t20 | 71 (60–79.9) | — | pas de cible : (1) |
| caisse_claire | house | t40 | 114 (96.1–128.7) | — | plus de cible (t40 non contre-mesuré) |
| caisse_claire | techhouse | t20 | 65 (55.5–73.4) | — | pas de cible : (1) |
| caisse_claire | techhouse | t40 | 135 (113.4–155.5) | — | plus de cible (t40 non contre-mesuré) |
| caisse_claire | techno | t20 | 65 (55.5–73.4) | — | pas de cible : (1) |
| caisse_claire | techno | t40 | 135 (113.4–155.5) | — | plus de cible (t40 non contre-mesuré) |
| caisse_claire | trap | t20 | 123 (100.4–141.9) | — | pas de cible : (2) |
| caisse_claire | trap | t40 | 196 (159.7–220.7) | — | plus de cible (t40 non contre-mesuré) |
| caisse_claire | dnb | t20 | 57 (49.1–65.9) | — | pas de cible : (5) |
| caisse_claire | dnb | t40 | 100 (87.4–112.6) | — | plus de cible (t40 non contre-mesuré) |
| caisse_claire | dubstep | t20 | 248 (136–356) | 248.335 (136.0525–355.5525) | d'accord |
| caisse_claire | dubstep | t40 | 546 (413.4–631.7) | — | plus de cible (t40 non contre-mesuré) |
| charley_ferme | house | t20 | 54 (44–76) | — | pas de cible : (2) |
| charley_ferme | house | t40 | 85 (65–100.3) | — | plus de cible (t40 non contre-mesuré) |
| charley_ferme | techhouse | t20 | 68 (51–75) | 68.47 (50.63–75.485) | d'accord |
| charley_ferme | techhouse | t40 | 92 (74.6–111.5) | — | plus de cible (t40 non contre-mesuré) |
| charley_ferme | techno | t20 | 72 (66–77) | 71.915 (65.64–77.39) | d'accord |
| charley_ferme | techno | t40 | 96 (91.3–122.8) | — | plus de cible (t40 non contre-mesuré) |
| charley_ferme | trap | t20 | 46 (41–53) | — | pas de cible : (2) |
| charley_ferme | trap | t40 | 71 (65.7–86.8) | — | plus de cible (t40 non contre-mesuré) |
| charley_ferme | dnb | t20 | 40 (39–69) | 54.72 (39.285–67.915) | d'accord |
| charley_ferme | dnb | t40 | 77 (59.6–129.1) | — | plus de cible (t40 non contre-mesuré) |
| charley_ferme | dubstep | t20 | 50 (50–96) | 50.25 (45.6575–54.26) | d'accord |
| charley_ferme | dubstep | t40 | 82 (70.4–90.4) | — | plus de cible (t40 non contre-mesuré) |
| charley_ouvert | house | t20 | 103 (86–453) | — | pas de cible : (1) |
| charley_ouvert | house | t40 | 154 (136.1–167.2) | — | plus de cible (t40 non contre-mesuré) |
| charley_ouvert | techhouse | t20 | 131 (86–453) | — | pas de cible : (1) |
| charley_ouvert | techhouse | t40 | 210 (155.8–628.7) | — | plus de cible (t40 non contre-mesuré) |
| charley_ouvert | techno | t20 | 131 (86–453) | — | pas de cible : (1) |
| charley_ouvert | techno | t40 | 210 (155.8–628.7) | — | plus de cible (t40 non contre-mesuré) |
| charley_ouvert | trap | t20 | 865 (637–1279) | — | pas de cible : (2) |
| charley_ouvert | trap | t40 | 951 (778.5–1469.6) | — | plus de cible (t40 non contre-mesuré) |
| charley_ouvert | dnb | t20 | 131 (86–453) | — | pas de cible : (1) |
| charley_ouvert | dnb | t40 | 210 (155.8–628.7) | — | plus de cible (t40 non contre-mesuré) |
| charley_ouvert | dubstep | t20 | 113 (86–453) | — | pas de cible : (1) |
| charley_ouvert | dubstep | t40 | 138 (122.7–184.3) | — | plus de cible (t40 non contre-mesuré) |
| rim | house | t20 | 56 (42.3–60.2) | — | pas de cible : (1) |
| rim | house | t40 | 163 (118.8–220.9) | — | plus de cible (t40 non contre-mesuré) |
| rim | techhouse | t20 | 56 (42.3–60.2) | — | pas de cible : (1) |
| rim | techhouse | t40 | 163 (118.8–220.9) | — | plus de cible (t40 non contre-mesuré) |
| rim | techno | t20 | 56 (42.3–60.2) | — | pas de cible : (1) |
| rim | techno | t40 | 163 (118.8–220.9) | — | plus de cible (t40 non contre-mesuré) |
| rim | trap | t20 | 56 (42.3–60.2) | — | pas de cible : (1) |
| rim | trap | t40 | 163 (118.8–220.9) | — | plus de cible (t40 non contre-mesuré) |
| rim | dnb | t20 | 56 (42.3–60.2) | — | pas de cible : (1) |
| rim | dnb | t40 | 163 (118.8–220.9) | — | plus de cible (t40 non contre-mesuré) |
| rim | dubstep | t20 | 56 (42.3–60.2) | — | pas de cible : (1) |
| rim | dubstep | t40 | 163 (118.8–220.9) | — | plus de cible (t40 non contre-mesuré) |
| tom | house | t20 | 147 (122.8–172.2) | — | pas de cible : (1) |
| tom | house | t40 | 229 (189–282.1) | — | plus de cible (t40 non contre-mesuré) |
| tom | techhouse | t20 | 262 (186.8–457.7) | — | pas de cible : (1) |
| tom | techhouse | t40 | 514 (357.3–785.9) | — | plus de cible (t40 non contre-mesuré) |
| tom | techno | t20 | 262 (186.8–457.7) | — | pas de cible : (1) |
| tom | techno | t40 | 514 (357.3–785.9) | — | plus de cible (t40 non contre-mesuré) |
| tom | trap | t20 | 262 (186.8–457.7) | — | pas de cible : (1) |
| tom | trap | t40 | 514 (357.3–785.9) | — | plus de cible (t40 non contre-mesuré) |
| tom | dnb | t20 | 262 (186.8–457.7) | — | pas de cible : (1) |
| tom | dnb | t40 | 514 (357.3–785.9) | — | plus de cible (t40 non contre-mesuré) |
| tom | dubstep | t20 | 262 (186.8–457.7) | — | pas de cible : (1) |
| tom | dubstep | t40 | 514 (357.3–785.9) | — | plus de cible (t40 non contre-mesuré) |
| crash | house | t20 | 1151 (610–1277) | — | pas de cible : (1) |
| crash | house | t40 | 1693 (1242.2–2185.9) | — | plus de cible (t40 non contre-mesuré) |
| crash | techhouse | t20 | 1151 (610–1277) | — | pas de cible : (1) |
| crash | techhouse | t40 | 1693 (1242.2–2185.9) | — | plus de cible (t40 non contre-mesuré) |
| crash | techno | t20 | 1151 (610–1277) | — | pas de cible : (1) |
| crash | techno | t40 | 1693 (1242.2–2185.9) | — | plus de cible (t40 non contre-mesuré) |
| crash | trap | t20 | 1151 (489–701) | — | pas de cible : (1) |
| crash | trap | t40 | 1693 (1242.2–2185.9) | — | plus de cible (t40 non contre-mesuré) |
| crash | dnb | t20 | 486 (519–1113) | — | pas de cible : (1) |
| crash | dnb | t40 | 1075 (883–1258.6) | — | plus de cible (t40 non contre-mesuré) |
| crash | dubstep | t20 | 1213 (1075–1355) | 1213.12 (1075.275–1355.03) | d'accord |
| crash | dubstep | t40 | 1693 (1508.4–2022.8) | — | plus de cible (t40 non contre-mesuré) |

Bilan : 11 d'accord, 0 désaccord, 49 sans cible définitive (on garde le son actuel), 54 lignes t40 sans cible.
