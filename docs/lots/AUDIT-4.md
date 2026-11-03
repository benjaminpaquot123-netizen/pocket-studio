# Audit, étage 4 « audio expert » : note de suivi (équipe étage 4, 2026-11-03)

Branche `fx-s4` (worktree `/home/user/fx-s4`, depuis fx-int après l'étage 3). Rien de poussé, pas d'APK. Référence : `docs/audits/AUDIT.md` §4 étage 4, §5, §6 ; `docs/lots/FIX-3.md` (bancs CPU limites). L'étage 1 (banc des 37 styles, matrice d'export) est fait par l'autre équipe : pas refait ici.

## 1. Marge du processeur du fil audio (but principal)

### Règle tenue : le son ne change pas du tout
Le §6 demande « le rendu maître identique ». Chaque gain est **bit à bit** : les trois projets de référence rendus avant (fx-int) et après, export, master Spotify (−14 LUFS sur tout le morceau) et master club (−9), soit 9 rendus : différence nulle (résidu −∞) à chaque commit. Script de comparaison : rendus de référence par le `ps_render` de fx-int, puis `scripts/ecoute.sh --against-dir`.

### Ce qui a été fait (dans l'ordre des commits)
| Pièce | Idée | Gain mesuré (seule, ce conteneur) |
|---|---|---|
| `DriveShaper::processPair` (saturation, distorsion, clipper, drive large du synthé) | trois passes sur 64 trames au plus (montée ×4, courbe, descente) au lieu d'une trame après l'autre : le processeur recouvre les longues chaînes de dépendances des filtres | saturation −30 %, distorsion −25 %, clipper −30 % |
| `Oversampler4xPair`, `DriveShaperPair` | les filtres HIIR des deux canaux dans un seul registre (vecteur de 2 flottants, extension GCC/Clang : SSE, NEON), voie par voie les mêmes opérations | saturation −38 % en tout, clipper −30 à −45 % |
| `Biquad2` (OTT, EQ 3 bandes) | idem pour les biquads gauche/droite | OTT −25 % |
| `KWeighting2`, montée du clipper du master par paire | idem en double pour la pondération K des mesures de sonie | chaîne master −7 % d'instructions |
| `ClipLauncher` | les « queues » vides des pads ne sont plus appelées | petit |

### Essayé puis retiré
- **Logarithme par table** dans `SoftClipShape::F` (64 cases + série, erreur 5e-16, résidu −147 à −183 dBFS sur l'effet seul, −11 % de temps de la saturation). Sur un morceau entier, ce bruit d'un bit déplace des décisions franches du clipper du master (s'arme ou non sur un échantillon) : résidu crête **−46 à −48 dBFS** sur des échantillons isolés (RMS −102 à −131 dBFS, sonie et gain identiques à 0,001 LU près). Inaudible, mais plus « le rendu maître identique » : retiré.
- **Sélection sans branchement** dans la courbe (les deux candidats calculés) : plus lent (le clipper surtout), retiré.
- **OTT en deux passes** (séparation des bandes puis gains) : aucun gain mesurable, retiré.
- Dénormaux : déjà réglés (`DenormalGuard` : FTZ/DAZ sur x86, FZ sur ARM, posé à chaque rappel dans `Engine::process`) ; rien à faire.

### Chiffres avant / après (3 passages alternés, même machine, juste après l'un l'autre)
Voir le tableau du §5 (fin de lot).

### Restes CPU
- La saturation reste le premier poste (≈ 25 % des instructions du pire cas) : son coût est maintenant le `log` en double de l'antidérivée (≈ 40 % de l'effet) et la division de l'ADAA. Un `log` plus rapide change le son au bit près (voir plus haut) : à ne reprendre que si le téléphone l'exige, avec une décision de Benjamin sur « rendu identique ».
- `s4_no_alloc_and_cpu` (synthé large 8 voix × 7 copies, borne 15 %) : 14,4 à 19,2 % sur ce conteneur, avant comme après (instructions identiques à 0,1 % près) ; le coût est dans DaisySP (`Oscillator::Process` 28 %, `LadderFilter` 16 %), pas dans le drive. Limite connue (REVIEW, « 808 de wide_tests »), borne non touchée.
- `master_cpu_budget` se lit à l'horloge murale : sur une machine à charge 2 à 3 il varie de ±10 % d'un passage à l'autre ; la mesure sûre est le nombre d'instructions (−7 %).

## 2. Vérifications sur ordinateur (AUDIT étage 4)
| Point | Résultat | Test |
|---|---|---|
| Tampons de 64 à 1024, 44,1 et 48 kHz, pire cas (16 pads, 8 boucles, chanson séparée étirée 120 → 128, 4 effets par tranche et sur le master, envois, effets du Live) | 14 configurations : ni NaN, sous 0 dBFS, 0 allocation ; moyenne 16 à 23 % du temps du tampon (charge 5, machine partagée) | `a4_worst_case_at_every_buffer_and_rate` (nouveau) |
| Changement de tempo en direct | déjà couvert (copie étirée remise au bon temps) ; le pire cas ci-dessus change aussi le tempo en jouant | `m17_copies_keep_the_phase_after_a_live_tempo_change`, `m17_worst_case_live_cpu_bench` |
| 10 minutes à l'échantillon près | 48 kHz (déjà) ; **44,1 kHz avec un tampon qui change à chaque rappel (64 à 1024)** : 0,94 trame au pire, au début comme à la fin, 0 allocation | `m17_ten_minutes_do_not_drift`, `a4_ten_minutes_at_44k_changing_buffers` (nouveau) |
| Zéro allocation, verrou, log dans le rappel | allocations comptées dans tous les bancs (0) ; aucun `mutex`/`lock`/`printf` dans `core/` (le seul fichier ouvert : export et analyse, hors rappel) ; `rand()` libc absent (vérifié par `host-tests.sh`) ; TSan : `scripts/host-tests-tsan.sh` (déjà) | voir colonne |
| Charge de l'interface pendant le son | simulée : un fil qui brasse 64 Mo toutes les 16 ms (une image Compose) à côté du pire cas ; voir le §5 | mesure, pas un test (dépend de la machine) |
| Compilation ARM | les nouveaux fichiers compilent pour `aarch64-linux-android26` (NDK 30, `-Werror`) ; le compilateur fait des `fmla` NEON 2 voies | à confirmer par `:app:assembleRelease` au rangement |

Note ARM : sur le téléphone, le compilateur fusionne multiplications et additions (FMA) dans le code vectoriel comme dans le code scalaire ; un écart d'un bit avec l'ancien binaire du téléphone n'est pas exclu (le téléphone n'est de toute façon pas identique au bit près à l'ordinateur). Inaudible ; rien à faire.

## 3. `ecoute` (AUDIT §3)
`scripts/ecoute.sh <morceau.pscmd | morceau.wav> [--masters] [--against-dir DIR] [--out DIR]` : rendu par le vrai moteur (`ps_render`), mesure (`ps_measure` : LUFS-I, LRA, crête vraie, crête, écrêtage, DC, corrélation, perte en mono, bandes, pente, infra-basses), spectre moyen au tiers d'octave et résidu contre une référence (`scripts/bench/ecoute_report.py`, numpy), spectrogramme PNG (ffmpeg `showspectrumpic`), rapport `report.md`. `--masters` ajoute les masters Spotify et club. Les WAV sont effacés après mesure (sauf `--keep-wav`). Les `.pscmd` viennent de l'appli (`RealProjectsTest` avec `-Preal.projects=…/reference-projects`, ou `SongBenchExport`).

## 4. Rapport d'ingénieur du son (3 projets de référence)
`vide.json` n'a rien à rendre (projet vide : `RealProjectsTest` ne prend que les dossiers). Le troisième morceau mesuré est donc `brouillon-jumpup` (projet de test synthétique, `real-projects/`).

Voir le §5 pour le tableau final (rapport `ecoute`).

Constats :
- **petit** (31 s) : corrélation 1,00, c'est-à-dire **mono** (aucune largeur stéréo : side −35 dB sous le mid) ; sommet du spectre à 63 Hz, aigus bas (air −24 dB, pente −6,2 dB/octave, plus sombre que les deux autres à −4,4/−4,9) ; LRA 1 à 2 LU (une boucle courte : normal). Export −9,3 LUFS, crête −1,17 dBTP : propre.
- **enorme** (138 s) : **infra-basses sous 30 Hz à −12 dB du total** (−22 à −24 pour les deux autres) et sous-graves à −3,5 dB : une grande part de l'énergie est sous l'audible, le limiteur la paie. Conséquence mesurée : **le master club sort à −10,6 LUFS au lieu de −9** (perte au limiteur et au clipper 6,1 dB : l'estimation s'arrête de pousser) ; l'export « comme le style » vise le Drop à −7,5 et l'atteint à −9,5 (perte 3 dB, le plafond voulu). Spotify −14,2 (dans ±0,5). Le texte de l'appli pour « Club » dit « l'export y est exactement » : ce n'est pas vrai pour ce morceau (voir la décision à recopier).
- **brouillon-jumpup** (298 s) : sain. Export −9,0 LUFS, LRA 10 à 13 LU (de la dynamique entre les parties), PLR 7,8 à 12,8 dB, corrélation 0,77, crête −1,19 dBTP ; club −9,1, Spotify −14,0.
- Partout : crête vraie sous −1 dBTP, aucun échantillon à pleine échelle, aucun NaN, DC < 1e-4.

## 5. Chiffres de fin de lot
(non complétés : lot arrêté avant sa dernière mesure ; chiffres avant/après relevés par l’orchestrateur au rangement, voir REVIEW)

## Décisions à recopier (DECISIONS.md)
- 2026-11-03 : optimisations du fil audio seulement bit à bit (rendu maître identique, §6) ; le logarithme par table de la saturation, plus rapide mais pas identique au bit sur un morceau entier, est retiré. Retour arrière : le commit « table logarithm » de fx-s4.
- 2026-11-03 : paires stéréo dans un registre (extension vectorielle GCC/Clang, pas d'intrinsèques) : un seul code pour x86 et ARM. Retour arrière : revenir aux deux objets scalaires (`DriveShaper` ×2, `Biquad` ×2, `KWeighting` ×2).

## Défauts trouvés
- **Moyen** (son, cible de sonie) : « Club » promet « l'export y est exactement », mais un morceau chargé en infra-basses (enorme) sort à −10,6 LUFS pour −9 : le garde-fou contre l'écrasement (perte maximale) l'emporte, sans le dire. Pas corrigé ici (choix de Benjamin) : question proposée ci-dessous.
- **Mineur** (son) : `enorme` a 12 dB d'infra-basses de trop sous 30 Hz ; un passe-haut à 25–30 Hz sur sa basse (ou une coupe des infra-basses au master pour le Club) rendrait la marge.
- Corrigé pendant le lot (jamais livré) : la première version de la saturation par blocs multipliait `post × wetGain` dans un autre ordre (un bit de temps en temps) : trouvé par la comparaison des morceaux entiers, corrigé avant commit final.

## Question pour Benjamin (lot suivant)
1. Quand un morceau ne peut pas atteindre « Club » sans s'écraser (enorme : −10,6 au lieu de −9) :
   A (recommandé) : l'appli le dit (« −10,6 LUFS : plus fort, il s'écraserait ») et propose de couper les infra-basses ; conséquence : rien ne change au son, le texte ne ment plus.
   B : on pousse quand même jusqu'à −9 ; conséquence : plus fort, mais pompage et grésillement possibles.
   C : on coupe automatiquement sous 30 Hz au master « Club » ; conséquence : la cible est tenue, les infra-basses (inaudibles au téléphone) disparaissent.

## À écouter sur le téléphone (moment 1)
1. `enorme` en master **Club** puis en export normal : le Club doit être plus fort sans pomper sur les coups de basse (s'il pompe : réponse 1B à éviter) ; noter si les basses « tiennent » au casque.
2. `petit` au casque : il est **mono** ; est-ce voulu (une boucle de test) ou un réglage de largeur oublié ?
3. Un morceau avec saturation, OTT et EQ (le pire cas du Live, 16 pads + 8 boucles) : aucun craquement pendant 2 minutes, en tournant les boutons de ces effets (le moteur est plus léger, le son ne doit pas avoir bougé du tout).
4. Les masters **Spotify** et **Club** de `brouillon-jumpup` l'un après l'autre, volume du téléphone fixe : le Club plus fort, le Spotify plus aéré, aucun grésillement dans les aigus.
5. OboeTester / trace Perfetto (déjà demandé) : la charge du fil audio dans le pire cas du Live, à comparer au chiffre d'avant ce lot.
