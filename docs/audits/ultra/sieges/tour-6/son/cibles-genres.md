# Cibles chiffrées par famille de genre (masters 2018-2026)

Recherche web du 2026-11-12. But : juger les rendus de Pocket Studio contre des masters commerciaux actuels.

Légende :
- **[M]** = mesuré ou publié par la source (chiffre repris tel quel).
- **[D]** = déduit par moi d'un chiffre publié (calcul indiqué).
- **[E]** = estimation (consensus d'ingénieurs, valeur sans mesure). À traiter comme fourchette indicative, pas comme vérité.
- Les numéros [n] renvoient à la liste des sources en bas.

## 0. À lire avant les tableaux

1. **Les sources ne mesurent pas pareil.** Trois jeux de données récents donnent pour la house : -8,0 LUFS [2] (morceaux entiers, 563 titres), -9,2 LUFS [3] (extraits de 2 min de boutique, MP3 96 kbit/s), -9,1 LUFS [1] (modèle de correction, ~0,5 dB d'erreur annoncée). Écart réel d'environ 1 dB entre sources : une cible doit être une **fourchette**, pas un point.
2. **Normalisation streaming** : Spotify ramène tout à -14 LUFS (ITU-R BS.1770) et conseille une crête vraie sous -1 dBTP (-2 dBTP si le master est plus fort que -14 LUFS) [6]. Apple -16 LUFS et YouTube -14 LUFS ne sont **pas** documentés officiellement ; chiffres répétés par l'industrie [7] **[E]**. Les masters réels restent bien plus forts que -14 (voir tableaux) : la normalisation baisse le volume, elle ne change pas la cible de mastering.
3. **Crête vraie** : dans la pratique, la majorité des masters actuels **dépassent 0 dBTP** [2][4][5]. La règle « -1 dBTP » est une recommandation, pas l'usage mesuré. Pour une appli, viser -1 dBTP reste le bon choix (encodage AAC 128 kbit/s : +2,3 dB de crête en médiane, 90 % des titres passent au-dessus de 0 dBTP en partant de -1 dBTP [3] **[M]**).
4. **LRA (plage de sonie)** : 50 % des tubes du Billboard Global 200 (S1 2024) entre 3,7 et 6,8 LU [4] **[M]** ; moyenne 6,0 LU pour les tubes 2020 [5] **[M]**. Les LRA de [3] sont calculés sur 2 min d'extrait : ils **sous-estiment** la vraie LRA d'un morceau entier.
5. **Pente spectrale générale** : enregistrements commerciaux = décroissance d'environ **5 dB/octave entre 100 Hz et 4 kHz**, plus raide au-dessus, et coupure basse nette vers 60 Hz [8] **[M]** (corpus 1950-2010, 772 titres : sert de repère général, pas de cible 2020). Électro et hip-hop ont une pente plus douce que la pop (voir §6).
6. **Tendance 2020** : les tubes de 2020 ont le plus de grave et de bas-médium, le moins d'aigu et de haut-médium des quatre décennies (1990, 2000, 2010, 2020) ; PLR (crête/sonie) 9,3 LU en 2010 et 2020 contre 12,6 LU en 1990 [5] **[M]**.

## 1. Hip-hop (boom bap, trap, drill, lo-fi, phonk, grime, jersey club)

| Style | LUFS-I | dBTP | LRA | Spectre | Stéréo | Notes |
|---|---|---|---|---|---|---|
| Hip-hop / rap (général) | **-9,7** médiane, 50 % entre -11,2 et -8,5 (48 263 titres) [1] **[M]** | non publié par [1] ; tubes 2024 : 0,00 à +0,75 [4] **[M]** (tous genres) | 3,7-6,8 LU [4] **[M]** (tous genres) | Pente la plus douce et grave le plus étendu avec l'électro [8] **[M]** ; « les mix les plus brillants » [8] | Hip-hop = le plus étroit des genres du top 2024 [4] **[M]** (pas de chiffre par genre) | Le plus étroit en stéréo : basse et voix au centre |
| Trap / future bass | **-9,2** médiane, 50 % entre -10,3 et -6,7 (22 titres) [3] **[M]** | -1 à 0 [E] | 5,7 LU (extrait 2 min) [3] **[M]** | 808 très présent sous 60 Hz [E] | Sub mono [E] | Petit échantillon |
| Boom bap | -10 à -12 [E] | -1 à 0 [E] | 5-8 LU [E] | Moins de sub que la trap, caisse claire 1-3 kHz [E] | Assez étroit [E] | Aucune source mesurée trouvée |
| Drill (UK / NY) | -8 à -10 [E] | -1 à +0,5 [E] | 4-6 LU [E] | 808 glissant, sub dominant [E] | Sub mono [E] | Aucune source mesurée trouvée |
| Lo-fi hip-hop | -12 à -14 [E] | -1 à -2 [E] | 5-8 LU [E] | Aigus atténués (filtre passe-bas, bande ~8-10 kHz) [E] | Modérée [E] | Aucune source ; genre volontairement doux |
| Phonk | -7 à -9 [E] | 0 à +1 (saturé exprès) [E] | 3-5 LU [E] | Cowbell 500 Hz-2 kHz, 808 saturé [E] | Étroit [E] | Aucune source |
| Grime | -8 à -10 [E] | -1 à 0 [E] | 4-6 LU [E] | Basses carrées riches en harmoniques [E] | Étroit [E] | Proche « UK bass » : -8,1 [3] **[M]** (46 titres, catégorie voisine) |
| Jersey club | -8 à -10 [E] | -1 à 0 [E] | 4-6 LU [E] | Kick et « bed squeak » aigus [E] | Modérée [E] | Aucune source |

## 2. House (house, deep, tech, UK garage, speed garage, acid, afro, disco/funk, amapiano)

| Style | LUFS-I | dBTP | LRA | Spectre (part d'énergie) | Stéréo | Notes |
|---|---|---|---|---|---|---|
| House | **-8,0** médiane (P25-P75 -9,0 à -7,1), 54 titres [2] **[M]** ; -9,2 [3] ; -9,1 (12 508 titres) [1] **[M]** | **+1,08** médiane [2] **[M]** | 4,8 LU (extrait) [3] **[M]** | sub 20-60 Hz 34,8 %, basse 60-250 Hz 43,1 %, >250 Hz ~22 % [2] **[M]** | largeur 0,24 (échelle 0-1), perte mono 0,46 LU, corrélation sub 0,998-1,0 [2] **[M]** | Tempo médian 126 BPM ; kick fondamental 51-54 Hz [2] |
| Tech house | **-8,8** médiane [2] ; -8,9 (P25-P75 -10 à -7,9) [3] **[M]** | **+1,22** [2] **[M]** | 4,6 LU [3] **[M]** | sub 34,9 %, basse 44,9 %, >250 Hz ~20 % [2] **[M]** | perte mono 0,32 LU [2] **[M]** | |
| Deep house | **-10,3** (-10,9 à -9,3), 41 titres [3] **[M]** | non publié | 4,3 LU [3] **[M]** | moins d'aigu que tech house [E] | modérée [E] | |
| Afro house / organic house | organic house -10,1 (-10,9 à -9,4) [3] **[M]** ; afro house non mesuré, -9 à -10 [E] | -1 à +0,5 [E] | 5,6 LU [3] | percussions 200 Hz-5 kHz [E] | assez large (percussions panoramiques) [E] | |
| UK garage / speed garage | -8 à -9,5 [E] (catégorie voisine « breaks / UK bass » -8,1 [3] **[M]**) | -1 à +0,5 [E] | 4-6 LU [E] | basse « reese » et sub fort [E] | sub mono [E] | Aucune source dédiée |
| Acid house | -9 à -10 [E] | -1 à 0 [E] | 5-7 LU [E] | TB-303 : énergie 200 Hz-3 kHz, résonance mobile [E] | étroit (303 mono) [E] | Aucune source dédiée |
| Disco / funk (nu-disco, indie dance) | indie dance **-9,9** (-10,8 à -8,5) [3] **[M]** | non publié | 5,8 LU [3] **[M]** | | | |
| Amapiano | -9 à -10,5 [E] | -1 à 0 [E] | 5-7 LU [E] | « log drum » très grave 40-80 Hz [E] | | Aucune mesure trouvée |

[D] En dB (house, d'après [2]) : sub ≈ +2 dB et basse ≈ +2,9 dB au-dessus de toute l'énergie > 250 Hz réunie (10·log10(34,8/22) et 10·log10(43,1/22)). La source parle d'un spectre pondéré en bandes de Bark ; le rapport exact dépend de cette pondération.

## 3. Techno (techno, rétro, Detroit, industrielle, hard, acid, gabber, acidcore, minimal, trance)

| Style | LUFS-I | dBTP | LRA | Spectre (part d'énergie) | Stéréo | Notes |
|---|---|---|---|---|---|---|
| Peak-time techno | **-7,7** médiane (308 titres) [2] ; -8,6 (-9,9 à -7,6) [3] **[M]** | **+0,96** [2] **[M]** | 5,6 LU [3] **[M]** | sub 43,0 %, basse 44,7 %, >250 Hz ~12 % [2] **[M]** | perte mono 0,30 LU, corrélation sub ≈ 1,0 [2] **[M]** | 131 BPM médian [2] |
| Groove / raw / hypnotic techno | groove **-7,7** [2] ; raw/deep/hypnotic -8,7 (-9,3 à -8,0) [3] **[M]** | +0,47 [2] **[M]** | 3,2 LU [3] **[M]** | sub 39,9 %, basse 51,9 %, >250 Hz ~8 % [2] **[M]** | largeur 0,16 (la plus étroite) [2] **[M]** | |
| Melodic techno | **-8,8** (-9,9 à -8,0) [2] ; melodic house & techno -10,5 [3] **[M]** | +0,67 [2] **[M]** | 5,4 LU [3] | sub 46,6 %, basse 42,0 %, >250 Hz ~11 % [2] **[M]** | largeur 0,20 [2] **[M]** | |
| Hard techno | **-6,9** (-8,0 à -6,4) [2] ; -7,3 (-8,2 à -6,2) [3] **[M]** | +0,79 [2] **[M]** | 4,5 LU [3] **[M]** | sub 40,2 %, basse 48,0 %, >250 Hz ~12 % [2] **[M]** | perte mono 0,26 LU [2] | 141 BPM [2] |
| Minimal / deep tech | **-10,3** (-11,4 à -9,4) [3] **[M]** | non publié | 5,9 LU [3] **[M]** | | | |
| Trance | main floor **-7,5** (-8,1 à -6,7) ; psy-trance -7,9 [3] **[M]** | non publié | 4,8-4,9 LU [3] **[M]** | supersaw large 200 Hz-8 kHz [E] | large au-dessus de 200 Hz [E] | |
| Rétro / Detroit techno | -9 à -10,5 [E] | -1 à 0 [E] | 5-7 LU [E] | moins de sub, plus de médium que la peak-time [E] | | Production actuelle « à l'ancienne » ; aucune source |
| Industrielle | -7 à -8,5 [E] | 0 à +1 [E] | 4-6 LU [E] | médium saturé, bruit large bande [E] | | Aucune source |
| Acid techno / acidcore | -7 à -8 [E] | 0 à +1 [E] | 4-6 LU [E] | 303 saturée 300 Hz-4 kHz [E] | 303 mono [E] | Aucune source |
| Gabber / hardcore | -5 à -7 [E] | 0 à +1,5 [E] | 3-5 LU [E] | kick distordu : énergie forte 100 Hz-2 kHz (pas seulement du sub) [E] | étroit [E] | Aucune source ; kick = saturation volontaire |

## 4. Bass music (DnB, liquid, neurofunk, jungle, jump up, dubstep, riddim)

| Style | LUFS-I | dBTP | LRA | Spectre | Stéréo | Notes |
|---|---|---|---|---|---|---|
| Drum & bass (général) | **-6,5** médiane (-7,9 à -5,2), 55 titres [3] **[M]** | non publié | 6,8 LU (extrait) [3] **[M]** | sub très fort ; reese 100 Hz-2 kHz [E] | sub mono [E] | Extraits de boutique : partie forte du morceau, donc légèrement plus fort que l'intégré complet |
| Liquid DnB | -8 à -9,5 [E] | -1 à 0 [E] | 5-7 LU [E] | plus d'air et de pads [E] | plus large [E] | Aucune source dédiée |
| Neurofunk / jump up | -5,5 à -7 [E] | 0 à +1 [E] | 4-6 LU [E] | basses très riches en harmoniques, mouvement rapide [E] | sub mono, médium large [E] | Aucune source dédiée |
| Jungle | -8 à -10 [E] | -1 à 0 [E] | 6-8 LU [E] | breaks (amen) 1-5 kHz, sub sinusoïdal [E] | | Aucune source |
| Dubstep | **-6** médiane (-6,5 à -4), 29 titres [3] **[M]** | > 0 fréquent (« beaucoup d'artistes dépassent 0 dB ») [9] **[E]** (témoignages) | 6 LU [3] **[M]** | | | |
| Riddim | -4 à -6 [E] | 0 à +1,5 [E] | 3-5 LU [E] | basse saturée plein médium [E] | | Proche dubstep ; aucune mesure dédiée |
| Breaks / UK bass (repère) | **-8,1** (-9,9 à -7,3), 46 titres [3] **[M]** | | 4,8 LU [3] | | | |

## 5. Autres (reggaeton, afrobeats, synthwave, pop)

| Style | LUFS-I | dBTP | LRA | Spectre | Stéréo | Notes |
|---|---|---|---|---|---|---|
| Reggaeton (catégorie « Latin ») | **-9,4** (-11,0 à -8,2), 15 065 titres [1] **[M]** | non publié | | kick + caisse « dembow » 100 Hz-3 kHz [E] | | « Latin » ≠ seulement reggaeton |
| Afrobeats | **-9,5** (-10,8 à -8,4), 2 310 titres [1] **[M]** | non publié | | | | |
| Synthwave | -9 à -11 [E] (électronique général -9,3, -11,0 à -8,0 [1] **[M]**) | -1 à 0 [E] | 6-8 LU [E] | caisse claire avec réverbération, pads larges [E] | large [E] | Aucune source dédiée |
| Pop | **-9,5** (-11,2 à -8,1), 46 316 titres [1] ; top 10 Billboard Global 2024 : **-8,3** ± 1 [4] ; tubes 2020 : -8,8 [5] **[M]** | 50 % entre **0,00 et +0,75** ; 20 % sous 0 [4] ; 103/120 tubes > 0 dBTP [5] **[M]** | 3,7-6,8 LU [4] ; 6,0 LU [5] **[M]** | 2020 : plus de grave et bas-médium, moins d'aigu [5] **[M]** | côté-milieu (S-M) moyen **-8,5 LU**, 50 % entre -10,9 et -6 ; la pop est la plus large [4] **[M]** | Court terme max ≈ intégré + 2 à 4 LU [4] |
| Dance / EDM (repère) | **-8,7** (-10,4 à -7,4), 18 514 titres [1] **[M]** ; mainstage -7,4 [3] | | 5,4 LU [3] | | | |

## 6. Spectre et stéréo : synthèse utilisable

- **Pente** : environ -5 dB/octave de 100 Hz à 4 kHz pour la musique enregistrée [8] **[M]**. Pente de régression log-log 100 Hz-10 kHz : électro -0,75, hip-hop -0,81, disco -0,80, pop -0,94, rock -0,98 [8] **[M]**. **[D]** Si cette pente porte sur l'amplitude (log10), elle vaut ×6,02 dB/octave : électro ≈ -4,5, hip-hop ≈ -4,9, pop ≈ -5,7 dB/octave. L'unité n'est pas écrite noir sur blanc dans l'article : à vérifier avant d'en faire un seuil strict.
- **Musique de club (house/techno)** : 78 à 92 % de l'énergie sous 250 Hz [2] **[M]**. Une techno avec plus de ~25 % d'énergie au-dessus de 250 Hz est hors norme ; une house peut monter vers ~22 %.
- **Analyseur réglé à 4,5 dB/octave** : une production moderne y apparaît à peu près plate (règle d'usage) [10] **[E]**.
- **Sub mono** : corrélation 0,998 à 1,0 dans la bande sub, pour tous les styles house/techno mesurés [2] **[M]**. « Mono sous 100-150 Hz » : consensus d'ingénieurs **[E]** (aucune source mesurée qui fixe la fréquence ; [2] mesure jusqu'à 60 Hz pour le sub).
- **Largeur** : house/techno 0,16-0,24 sur 0-1 ; perte de sonie en mono 0,18-0,46 LU (règle usuelle : < 1 LU) [2] **[M]**. Pop/hip-hop : S-M -8,5 LU en moyenne [4] **[M]**.

## 7. Téléphone et kick

**Ce que reproduit un haut-parleur de téléphone :**
- iPhone 7 Plus : la réponse « chute vite sous 600 Hz », bande utile ~500 Hz-10 kHz ; pour simuler un téléphone, filtrer **400 Hz-6 kHz** [11] **[M]** (mesure de l'auteur, 2020).
- HTC One (2013) : ~230 Hz-10 kHz à ±3,5 dB, bosse entre 150 et 230 Hz [12] **[M]** (extrait de recherche, page non ouverte : 403).
- Micro-haut-parleur Knowles : résonance **~900 Hz** dans 1 cm³ de volume arrière, ~500 Hz à dos ouvert [13] **[M]** (fiche technique, lue en extrait). Plus le volume arrière est petit, plus la résonance monte [13][14].
- **Synthèse [D]** : sur un téléphone, rien sous ~200-300 Hz dans le meilleur cas, souvent rien d'utile sous ~400-600 Hz. **Cible de test proposée : passe-haut 300 Hz (téléphone récent) et 500 Hz (pire cas), passe-bas 8-10 kHz** [E] à partir de [11][12].

**Conséquence pour le kick et la basse :**
- Le kick club a sa fondamentale à **48-62 Hz** (médiane 51-54 Hz en house/techno) [2] **[M]**. Sur téléphone, seuls ses harmoniques à partir du 5e-10e rang (et la queue du transitoire) passent [D].
- Le cerveau reconstitue une fondamentale absente si les harmoniques sont là (« missing fundamental ») : technique utilisée pour les petits haut-parleurs [15][16] **[M]** (articles AES, résumés). Limite : un transitoire n'a pas de hauteur définie, l'effet ne s'applique pas au clic [17] **[M]**.
- Techniques (articles d'ingénieurs, **[E]**) : saturation du kick et de la basse pour créer des harmoniques, en gardant le sub propre ; découpe à 100 Hz et saturation de la partie haute seulement ; couche « attaque » ou clic **vers 5 kHz** ; doubler un 808 pauvre au-dessus de 400 Hz avec un kick plus attaqué [11] ; harmoniques à 2× et 3× la fondamentale (ex. 90 et 135 Hz pour un sub à 45 Hz) [18] ; légère chaleur 100-300 Hz [19].
- **Critère de test proposé [E]** : un kick de Pocket Studio « s'entend sur téléphone » si, après passe-haut 400 Hz, son énergie dans 400 Hz-6 kHz reste à moins de ~20 dB sous celle du kick non filtré et que son attaque (1-5 kHz) dépasse nettement la basse au même instant. Seuil à calibrer sur des morceaux de référence, aucune source ne le chiffre.

## 8. Caractère des synthés actuels contre années 90-2000

Aucune étude mesurée trouvée qui compare directement les synthés de musique électronique des années 90 et 2020 (centroïde, flux spectral, largeur, saturation). Ce qui existe :
- Tubes 2020 : **plus sombres**, plus de grave et bas-médium, moins de haut-médium et d'aigu que 1990 ; tendance vers des sons « plus sombres, plus traités », contre les timbres « plus naturels » des tubes des années 90 [5] **[M]** (Billboard, 120 titres, iZotope Tonal Balance Control).
- Spectre de plus en plus plat et compression croissante au fil des décennies ; zone de présence 1,5-4 kHz dont la pente s'adoucit (plus de définition) [8] **[M]**.
- Dance music depuis 1985 : hausse de la sonie, du tempo et de la « brillance » (1re composante de timbre) [20] **[M]** (extrait). Attention : contredit en partie [5] ; [20] porte sur les tubes dance, [5] sur tous les tubes.
- PLR 12,6 LU (1990) contre 9,3 LU (2020) [5] **[M]** : sons plus denses, plus compressés.

**Estimation [E]** (consensus d'ingénieurs, aucune source chiffrée) pour juger un synthé « actuel » :
- richesse harmonique plus forte (saturation ou distorsion de bus, basses à harmoniques élevés) ;
- mouvement spectral : filtres et tables d'ondes modulés en permanence (LFO, enveloppes), pas de son statique sur plusieurs mesures ;
- largeur : unisson/désaccord et chorus sur les nappes et leads au-dessus de ~200 Hz, sub strictement mono ;
- transitoires nets, peu de réverbération longue sur les basses ; réverbération et délais en « side-chain » pour ne pas brouiller le grave ;
- ce qui sonne « années 90 » : son plus statique, plus de haut-médium, moins de sub, moins de saturation, stéréo plus étroite (à l'exception des sons rétro voulus : synthwave, rétro techno, acid).

## 9. Ce qui reste estimé

- Aucune mesure trouvée pour : boom bap, drill, lo-fi, phonk, grime, jersey club, UK garage, speed garage, acid house, afro house, amapiano, rétro/Detroit, industrielle, acid techno, gabber, acidcore, liquid, neurofunk, jungle, jump up, riddim, synthwave. Valeurs [E] ci-dessus.
- Pas de chiffre de crête vraie par genre hors house/techno [2].
- Pas de niveaux en dB par bande publiés par genre (Tonal Balance Control d'iZotope ne publie que des courbes en image [4][5]).
- Fréquence « mono sous X Hz » : consensus, pas de mesure.
- Pour fiabiliser : mesurer soi-même 10-20 titres de référence par famille (BS.1770-4, EBU 3342) avec le même outil que le banc de Pocket Studio.

## Sources

1. Alexander Almgren, « Average LUFS by Genre: We Measured 314,876 Tracks (2026 Data) », Freshly Baked Studios, 22 août 2026. https://freshlybakedstudios.com/blog/average-lufs-by-genre (médianes ; valeurs projetées par modèle de correction, ~0,5 dB d'erreur moyenne annoncée).
2. TrackSensei, « We Analyzed 563 Released Techno and House Tracks », 12 juin 2026 (auteur non nommé). https://tracksensei.com/blog/we-analyzed-563-techno-house-tracks (BS.1770, 44,1 kHz ; attention : la source donne aussi une échelle « mono » ~3,3 LU plus basse, non reprise ici).
3. Mixgraph (NC Digital Limited), « LUFS meter » : tableau des médianes par genre et test de crête après encodage, mesuré le 2026-08-11. https://www.mixgraph.io/tools/lufs-meter (extraits de 120 s MP3 96 kbit/s de boutique ; BS.1770-4, EBU Tech 3342).
4. Ian Stewart, « Mastering trends in 2024: what you should be doing », iZotope, 16 juillet 2024. https://www.izotope.com/en/learn/mastering-trends (54 titres du top 10 Billboard Global 200, S1 2024).
5. Marc Alexandru Țînț, Stela Drăgulin, « From Data to Hits: Understanding Mixing and Mastering Techniques that Make a Song Chart-Worthy », Bulletin of the Transilvania University of Brașov, Series VIII, vol. 18(67), 2025. https://doi.org/10.31926/but.pa.2025.18.67.3.24 (PDF : https://webbut.unitbv.ro/index.php/Series_VIII/article/download/11335/6760/20717).
6. Spotify for Artists, « Loudness normalization on Spotify » (page d'aide, consultée 2026). https://support.spotify.com/artists/article/loudness-normalization/
7. Orphiq, « Mastering for Streaming: Loudness and Format Guide » (Apple -16 LUFS non documenté par Apple). https://orphiq.com/resources/mastering-for-streaming ; MeterPlugs, « YouTube changes loudness reference to -14 LUFS », 2019. https://www.meterplugs.com/blog/2019/09/18/youtube-changes-loudness-reference-to-14-lufs.html
8. Pedro D. Pestana, Zheng Ma, Joshua D. Reiss, Álvaro Barbosa, Dawn A. A. Black, « Spectral Characteristics of Popular Commercial Recordings 1950-2010 », AES 135th Convention, paper 8960, New York, 2013. https://joshreiss.github.io/documents/2013/Pestana%20Ma%20Reiss%20-%20AES135.pdf
9. Reddit r/edmproduction, « Clipping in dubstep », 16 juillet 2024 (témoignages, pas une mesure). https://lr.us.psf.lt/r/edmproduction/comments/1e4rhm3/clipping_in_dubstep
10. « Spectrum Analyzer Slopes in Audio Mixing », Sound and Design (Medium). https://medium.com/sound-and-design/spectrum-analyzer-slopes-in-audio-mixing-d6df8892ea3
11. Eli Krantzberg, « Make Your Mix Sound Great On Small Speakers », Sonarworks Blog, 15 avril 2020. https://www.sonarworks.com/blog/learn/make-your-mix-sound-great-on-small-speakers
12. Sound & Vision, « HTC One Video and Audio Measurements », 2013 (lu en extrait de recherche, page en 403). https://soundandvision.com/node/111446
13. Knowles, fiche technique micro-haut-parleur (réf. 2403-260-00058, famille 1318), lue en extrait de recherche. https://wmsc.lcsc.com/wmsc/upload/file/pdf/v2/lcsc/2312201805_Knowles-2403-260-00058_C19422743.pdf
14. Brevet US 10667028, « Electric device including microspeaker module… » (petit volume arrière = résonance plus haute). https://image-ppubs.uspto.gov/dirsearch-public/print/downloadPdf/10667028
15. Arora et al. (Samsung), AES 122nd Convention, 2007, bass enhancement par « missing fundamental » (résumé). https://www.aes.org/e-lib/browse.cfm?elib=14009
16. Bharitkar et al., AES, synthèse d'harmoniques pour fondamentale perçue (résumé). https://aes2.org/publications/elibrary-page/?id=19278
17. E. Cansev, P. Dürst, « Adaptive dynamic processing for psychoacoustic low-frequency enhancement of small speakers », HSR Rapperswil (OST), 2016. https://www.ost.ch/fileadmin/dateiliste/97_daten/abstracts/e_e.cansev_p.d__rst_f_2016.pdf
18. Carlo Giannico, « Psychoacoustic Mixing for Club and Smartphone Playback », Digital Renaissance, 4 mars 2026. https://www.digitalrenaissance.education/magazine/psychoacoustic-tricks-for-smartphone-translation-without-killing-club-power
19. Violet Recording, « How to fix a weak kick » (conseil pratique, extrait de recherche). https://violetrecording.com/how-to-fix-a-weak-kick/
20. D. Herremans, D. Martens, K. Sörensen, « Dance Hit Song Prediction » (arXiv 1905.08076 ; lu en extrait de recherche). https://arxiv.org/pdf/1905.08076
