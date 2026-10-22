# Audit « corps » : les morceaux de l'assistant ont-ils assez de corps ?

Question de Benjamin : dans les 36 styles, le morceau complet (≈ 3 min) fait par l'assistant a-t-il assez de corps (plein, poids, énergie, richesse), ou sonne-t-il maigre, vide ou répétitif à côté de vrais morceaux du genre ?

Réponse courte : **non, pas encore**. La structure (couches qui s'empilent, Drop, Break, montée) est juste, mais quatre défauts transversaux enlèvent le corps, dans tous les styles :
1. **Le mix n'est pas équilibré** : les sons de la bibliothèque ont jusqu'à 33 dB d'écart de volume entre eux, le kick intégré est aussi fort à lui seul qu'un morceau masterisé, les charleys intégrés sont presque muets. Résultat mesuré : avec le kit intégré, la basse seule est en médiane 10 dB sous le mix du Drop (jusqu'à −29 en disco et −34 en minimal), le lead 15 dB (jusqu'à −25), et le limiteur écrase tout (facteur de crête médian 6 dB, 3,7 à 9).
2. **Chaque section est une seule boucle de 1 à 4 mesures répétée 16 à 32 fois** : aucun fill, aucune crash, aucun changement toutes les 8 mesures. Un morceau de 64 à 160 mesures contient 4 à 16 mesures de matière unique.
3. **Le Drop n'est pas plus gros que le Groove** : dans 16 styles sur 36 il n'ajoute qu'une couche, et aucune pour le phonk ; la sonie mesurée du Drop est celle du Groove à ±1 LU (au mieux +3 LU), souvent même celle de l'Intro (kick seul).
4. **Il manque le « liant »** : tout est presque mono (côté/centre médian −33 dB en kit, −20 en packs, contre −10 à −15 dans un vrai morceau), trou dans les bas-médiums 250-500 Hz (médiane −16,5 dB relatifs en kit, jusqu'à −23 ; cible ≈ −10), pas de compression de bus ni de master (sauf acidcore), pompe absente en house, amapiano, UK garage, minimal, Detroit, acid.

Les recettes, elles, sont musicalement bonnes (rythmes, harmonies, choix des sons à quelques exceptions près : basse du disco et du minimal) : l'essentiel du gain est dans le mix par défaut et dans l'arrangement, pas dans la réécriture des styles.

---

## Méthode

- Code lu : `project-model/.../StarterWizard.kt` (plan des scènes, sections, effets), `StyleRecipes.kt` (généré depuis `scripts/packs/recipes/*.json`), `FxPresets.kt`, `SoundLibrary.kt`, `SynthPatch.kt`, moteur `engine/src/main/cpp/core/` (DrumKit, Synth, PlaitsSynth, Mixer, OfflineRenderer). Dépôt **non modifié** (`/home/user/ps-p22`, main après la phase 28).
- Sonde structurelle `probe/Body.kt` (compilée contre les sources de `project-model`) : les 36 styles, ambiance par défaut, parties cochées par défaut **et** tout coché ; par section : couches, coups de batterie et notes par mesure, registres, couverture grave/médium/aigu, effets, mixeur. Sorties : `out/body.txt`, `out/body.csv`.
- **Rendu audio réel** : `probe/Render.kt` traduit chaque scène du projet en commandes moteur (même encodage que `NativeEngine.kt` / `RhythmViewModel.syncEngine`), `harness/render.cpp` les joue avec `OfflineRenderer` (cœur C++ compilé sur Linux, comme `engine/host-tests`), `tools/analyze_batch.py` mesure (2e boucle seulement) : sonie intégrée (LUFS, pondération K de la BS.1770, sans portillon), facteur de crête, énergie par bande (sub < 60 Hz, grave 60-250, bas-médium 250-500, médium 500-2 k, haut-médium 2-6 k, air > 6 k, en dB relatifs au total), largeur stéréo (énergie côté/centre). Pour chaque Drop/Refrain : chaque couche seule (kick, batterie, basse, accords, lead, 303). Deux cas : **kit** (sons intégrés seulement) et **packs** (packs du style installés : sons de batterie et basses de packs choisis comme le fait l'assistant, note fondamentale détectée par `RootNote`). ≈ 1 000 rendus. Données : `stats/done.jsonl`, tableaux : `tools/summary.py`, `tools/report.py`.
- Limites du rendu : les courbes du morceau (filtres qui s'ouvrent, 303 qui bouge, souffle montant) ne sont pas jouées (filtres à leur valeur de Drop, souffle muet) ; la ride intégrée est la cymbale brute (pas la version raccourcie) ; la cloche 808 jouée au clavier (phonk) n'est pas rendue ; les sons « voix découpées » des packs non plus. Les mesures sont faites au tempo du style, une scène à la fois.
- Volume de chaque son de la bibliothèque mesuré seul (`probe/Levels.kt`, même figure par famille : basse Do2 à la noire, nappe accord tenu, lead/pluck Do4 à la noire) et de chaque voix du kit (`kitlvl/`).
- Prototype de correction (`proto/`) : le même Drop de techno avec un mix par défaut corrigé (voir § Preuve).

Références de genre : `scratchpad/research/famille-*.md`, les références de chaque recette, et les ordres de grandeur habituels d'un master de club (≈ −8 à −6 LUFS en techno/DnB, −9 à −7 en house, facteur de crête 7 à 10 dB, sub et grave ≈ −4 à −6 dB relatifs chacun, bas-médium ≈ −10, médium ≈ −10 à −12, haut-médium ≈ −14 à −16, air ≈ −16 à −20, largeur −10 à −15 dB).

---

## Constats transversaux (mesurés)

### T1. Volumes des sons : 33 dB d'écart dans la bibliothèque

Volume de chaque son seul, même figure, fader à 0 dB (`lvl/levels.jsonl`) :

| Famille | Son | LUFS | | Famille | Son | LUFS |
|---|---|---|---|---|---|---|
| Basses | Basse sub | −17,6 | | Leads | Supersaw | −14,1 |
| | Basse wobble | −18,3 | | | Lead filtré (P) | −13,8 |
| | Basse qui parle | −20,6 | | | Lead chiptune (P) | −11,3 |
| | Basse reese | −20,8 | | | Lead, Lead carré, Lead trance, Hoover | −17 à −19 |
| | Basse analogique (P) | −20,9 | | | Voix robot (P) | −19,5 |
| | Reese moderne | −21,3 | | | **Lead acide** | **−24,8** |
| | Basse ronde | −21,4 | | Pluck | **Pluck** | **−30,9** |
| | Basse FM (P) | −16,0 | | | **Pluck future** | **−31,0** |
| | **Log drum (P)** | **−24,4** | | | **Arpège** | **−34,7** |
| | **Basse pincée (P)** | **−42,1** | | | Cloche / Cloche (P) | −26 / −21 |
| Nappes | Nappe d'accords (P) | −8,5 | | | **Piano FM (P)** | **−26,7** |
| | Accords future bass | −9,0 | | | **Marimba (P)** | **−26,2** |
| | Nappe chaude | −9,9 | | Claviers | Orgue | −11,1 |
| | Cordes | −10,2 | | | Piano électrique | −17,7 |
| | Nappe | −11,2 | | | | |
| | Machine à cordes (P) | −16,0 | | | | |
| | Essaim (P) | −14,6 | | | | |

Conséquences dans les morceaux (couche seule moins le mix du Drop, kit intégré) : la basse pincée du **disco** est à −29 dB (inaudible : la basse est la star du disco), le lead Piano FM du **drill** à −24, le lead Pluck de la **pop** à −23, les stabs Pluck du **reggaeton** à −22 et de la **techno** à −20, la basse analogique de la **trance** à −13 ; à l'inverse les nappes (Cordes, Nappe, Supersaw) passent devant la basse en DnB, dubstep, trap et trance.
Le réglage LEVEL du synthé ne monte que de +4,4 dB (0,6 → 1,0) et le fader de +3,9 dB : on ne peut pas rattraper 15 à 25 dB sans toucher aux sons.

### T2. Le kit intégré : kick énorme, charleys muets

Chaque voix seule, 4 coups par mesure à 128 BPM (`kitlvl/k.jsonl`) : Kick **−5,5 LUFS** (un morceau masterisé entier fait −8 à −6 !), Kick 2 −11,9, Toms −10 à −11, Conga −14, Caisse 2 −14, Clap −16, Caisse −17, Shaker −18, Zap −22, Cowbell −24, Cymbale −26, Charley ouvert −30,5, Rim −32, **Charley fermé −45**.
- Le kick seul tape le limiteur (−1 dBFS) : en kit intégré le facteur de crête du Drop est en médiane de 6 dB (3,7 à 9 ; cible 7 à 10 ; médiane 10,8 avec les packs), et le Drop n'est **pas plus fort que l'Intro** (techno : Intro −5,4 LUFS, Drop −6,5).
- Les charleys sont 40 dB sous le kick : « air » (> 6 kHz) en médiane à −27,5 dB relatifs dans les Drops en kit intégré (−20 à −39), −14 avec les packs. Le haut du spectre manque, c'est une grande part du « maigre ».
- Avec les packs, l'équilibre est bien meilleur (kick de pack −13 à −16 LUFS, crête 9-10 dB) mais d'autres trous apparaissent (pas de sub en trance, acid house, disco, amapiano : kick de pack léger et basse faible).
- Commentaire du code (`DrumKit.cpp:78`) : le kick a été poussé suite à « pas assez puissants ». Il est puissant, mais tout le reste est maintenant relativement trop faible.

### T3. Une boucle par section, aucune variation

- `StarterWizard.inScenes` / `songOf` : chaque section = une scène de 1 à 4 mesures répétée (`SongPart.repeats`) ; la batterie est une boucle de 1 ou 2 mesures. Seules les courbes (filtres, 303, souffle) bougent dans une section.
- Matière unique par morceau : 4 mesures (acid house, acid techno par défaut : **une seule mesure de batterie et de 303 pendant 88 à 120 mesures**), 8 (acidcore), 12 (styles « chanson »), 16 (club). Un Drop de 32 mesures = la même boucle 8 fois.
- Pas de fill en fin de 8 ou 16 mesures, pas de crash au début des sections, pas de « Drop 2 » différent du premier, pas de question-réponse sur 8 mesures. Les fills écrits dans les recettes (roulement de caisse claire « x234 », charley « 3 » « 22 ») reviennent **à chaque mesure ou toutes les 2 mesures**, ce qui use l'effet.
- Le refrain des styles « chanson » est presque identique au couplet : phonk (identique, 0 couche ajoutée), boom bap (+1 coup de charley ouvert toutes les 2 mesures), lo-fi (+shaker), reggaeton (+un fill de tom), grime et jersey club (+une percu).

### T4. Le Drop n'est pas plus gros

Couches Groove/Couplet → Drop/Refrain (réglages par défaut) : +1 seule couche en house (piano), disco (lead), trance (lead), amapiano (piano), minimal (stabs), deep house (percu), afro house (nappe), acid house (cloche), acid techno (charley ouvert), acidcore (charley ouvert), industrial (nappe), reggaeton, lo-fi, grime, jersey club, boom bap ; 0 en phonk. Mesuré : sonie Drop − Groove = −0,6 à +2,1 LU en kit (seule la jungle dépasse +1,5), −0,8 à +2,9 en packs.
Avec le limiteur, un Drop ne peut être « plus gros » que par la densité, la largeur et le spectre : il faut donc qu'il ajoute vraiment des couches (contre-chant, ride, percu 2, accords doublés) et que le Groove en retienne.

### T5. Mono, trou dans les bas-médiums, pas de liant

- Largeur (côté/centre) des Drops : médiane −33 dB en kit (de −19 au mono parfait : phonk, afrobeats, boom bap, grime, jersey club), médiane −20 en packs. Rien n'est panoramiqué (aucune recette ne peut régler un pan), seuls quelques effets (réverbe, `wide_alive` sur la liquid) ouvrent.
- Bas-médiums (250-500 Hz) du Drop : médiane −16,5 dB relatifs en kit, −12 en packs ; −19 à −23 en techno, house, pop, reggaeton, acid house/techno, minimal, retro techno, jersey (cible ≈ −10). Cause : accords à l'octave 4 au-dessus d'une basse en octave 1-2, **1,4 à 3 octaves de vide** en trap, phonk, reggaeton, disco, boom bap, trance, synthwave, minimal, hard techno, gabber, acidcore, acid techno, industrial, jersey club.
- Aucune compression de bus batterie sauf jungle (punch), tech house et hard techno (kick seul), lo-fi (vinyle), Detroit et industrial (saturation). Master vide partout sauf acidcore (OTT + saturation). La pompe (sidechain) manque sur la basse en house, amapiano, UK garage, minimal, Detroit, acid house et acid techno (sur la 303), disco ; elle n'est jamais mise sur la nappe sauf trance et synthwave.

### T6. Breaks qui s'effondrent, intros sans grave

- Break − Drop en kit : riddim −34,5 dB (charley + rim seuls), minimal −20, techno −19,7, rétro techno −16,8, house −14,5 ; −10 à −12 avec les packs (house, techno, minimal, hard techno, gabber). Un vrai break perd 4 à 8 dB, garde une nappe large, une réverbe, souvent la percu ou un sub filtré, et la montée.
- Les intros « chanson » (trap, drill, phonk, reggaeton, afrobeats, boom bap, synthwave, jersey) n'ont que le charley et le lead : normal pour 4 à 8 mesures, mais le grave n'arrive qu'au couplet et ne bouge plus ensuite.

### T7. « Tout coché » ne change presque rien

Avec toutes les lignes et parties cochées, le lead est gardé pour le Drop et la nappe arrive au Groove (club), ou la nappe et le lead rejoignent le Refrain (chanson). Mesuré sur les 30 styles qui ont un Groove/Couplet : Drop − Groove reste à −0,1 LU de médiane (kit) et +0,2 (packs), la largeur ne bouge pas, les bas-médiums gagnent à peine 1 dB. Les couches ajoutées sont justement les plus faibles (leads médian −15 dB sous le mix en kit, −12 en packs ; T1) : on les coche, on ne les entend pas.

---

## Preuve : un mix par défaut corrigé suffit à redonner du corps (techno, kit intégré)

Même Drop, mêmes notes, seulement des réglages de mixeur et deux effets (`proto/techno2.json`, appliqué en commandes moteur) : kick −8 dB (sa tranche), charleys +3 à +4 dB, clap +2, panoramiques (charley −0,25, charley ouvert +0,2, percu +0,35, ride +0,25), fader basse et stabs +3,9 dB, LEVEL des stabs 1,0, élargisseur sur les stabs, compresseur de liant + saturation légère sur le master, gain master +4 dB.

| Drop techno (kit) | avant | après |
|---|---|---|
| Sonie | −6,5 LUFS | −8,2 LUFS |
| Facteur de crête | 4,4 dB (écrasé) | 7,1 dB |
| Basse seule − mix | −12,4 dB | +1,0 dB |
| Stabs seuls − mix | −19,8 dB | −10,8 dB |
| Bas-médium / médium | −22,9 / −21,1 | −15,9 / −13,4 |
| Haut-médium / air | −25,3 / −31,8 | −18,2 / −26,1 |
| Largeur | −33 dB | −24 dB |
| Break − Drop | −19,7 dB | −10,7 dB |

L'air reste faible : il faut aussi revoir les charleys du kit (moteur) ou leur volume par défaut.

---

## Fiches par style

Lecture des chiffres : « kit » = sons intégrés, « packs » = packs du style installés ; Drop (ou Refrain) des réglages par défaut. « basse −12 » = la basse seule est 12 dB sous le mix du Drop (cible : −2 à −5 ; accords −6 à −10 ; lead −5 à −9). Bandes en dB relatifs (sub / grave / bas-médium / médium / haut-médium / air). Note /10 = corps du morceau par défaut aujourd'hui, comparé aux références de la recette. Les corrections générales (M1/E1 volumes, M2 mix par défaut, M3 liant, M4 fills, M5 Drop 2) valent pour tous et ne sont pas répétées ; seules les modifications propres au style sont listées.

| Style | Note | Défaut principal |
|---|---|---|
| techno | 5 | stabs inaudibles (−20), basse −12 en kit, une boucle, break qui s'effondre |
| house | 4 | Drop = Groove + piano, pas de pompe ni ride, piano −14, mono |
| trap | 5 | charleys muets en kit, lead −14 sous la nappe, trou 90-280 Hz |
| drill | 4 | lead (la signature) à −24, nappe trop forte, refrain +2 couches |
| pop | 4 | lead (le refrain) à −23, accords −15, mix creux en packs (crête 13) |
| phonk | 4 | refrain identique au couplet, mono, trou de 1,9 octave |
| dnb | 5 | la nappe couvre la basse (−2,7 contre −9,5), pas de sub en packs |
| reggaeton | 3 | stabs à −22, médium −28 : batterie + 808 seuls, mono |
| afrobeats | 5 | aucun effet, mono total, lead −15 |
| lofi | 6 | voulu doux ; basse −7 à −10, mono |
| dubstep | 5 | la nappe passe devant le wobble, pas de lead, break = nappe seule |
| disco | 3 | **basse inaudible (−29)**, pas de sub, stabs −19 |
| trance | 4 | basse −13, pas de sub en packs, lead trop aigu, ni ride ni percu |
| deep_house | 5 | basse −12/−8, pas d'aigus (air −38 en kit) |
| tech_house | 6 | complet (9 couches, ride, shaker, pompe) ; basse −9, lead −11 |
| boom_bap | 4 | mono, piano −14, refrain +1 coup |
| synthwave | 4 | arpège (la signature) à −25, basse −12 |
| acid_house | 4 | une mesure pendant 88 mesures, pas de basse, pas de sub en packs |
| hard_techno | 6 | bon poids (rumble), stabs −9/−11, peu de variation |
| amapiano | 5 | log drum faible (sub −20 en packs), pas de pompe, Drop = +piano |
| uk_garage | 6 | assez équilibré ; mono, pas de pompe |
| minimal | 3 | **basse inaudible (−34)**, stabs −20 |
| afro_house | 6 | percus riches ; trou dans le médium, lead −17 |
| jungle | 5 | basse correcte (−5) ; pas de vrai break, pas de montée |
| retro_techno | 5 | hoover −17 en kit (bien avec le pack), basse −14 |
| acid_techno | 4 | une mesure en boucle, kick écrasant, pas de basse |
| gabber | 5 | volontairement écrasé ; ni basse ni lead, peu de matière |
| acidcore | 5 | 303 + OTT, efficace mais 4 couches au Drop |
| liquid_dnb | 5 | arpège −23, piano −11, mix creux en packs |
| detroit | 5 | basse −14, nappe correcte (−8) |
| grime | 3 | **aucun charley**, stabs −23, mono |
| industrial_techno | 6 | bon poids (rumble + saturation) ; peu de couches |
| neurofunk | 4 | la basse (la star) à −14, la nappe devant |
| jersey_club | 4 | mono, lead −12, sans nappe par défaut |
| speed_garage | 5 | basse qui parle −10, mono, Drop ≈ Groove |
| riddim | 4 | wobble −10 sous le kick, Break = trou de −35 dB |

Moyenne ≈ 4,6/10. Avec M1/E1 (volumes), M2-M3 (mix et liant) et M4-M5 (variation), l'estimation est de 7 à 8/10 pour la plupart des styles sans changer une note des recettes.

### Techno (techno.json) — 5/10
- Drop : kick, clap, charleys, charley ouvert, percu, ride, basse, stabs (8 couches, +3 sur le Groove : bien). Kit : −6,5 LUFS, crête 4,4, basse −12,4, stabs −19,8, bas-médium −23, air −32, largeur −33, Break −20 dB. Packs : basse −2,5 (bien), stabs −17,6.
- Manques : stabs Pluck inaudibles (son à −31 LUFS), pas de liant, Break = stabs + charleys à −26 LUFS, boucle d'une mesure.
- Recette : `"chords": {"sound": "pack:techno:stab|Orgue"}` (ou garder Pluck après M1) ; `"fx"` + `{"on":"drums","preset":"punch"}` ; `"break": ["chords","lead","hat","perc","ride"]`.

### House (house.json) — 4/10
- Drop : kick, clap, charleys, charley ouvert, shaker, basse, piano (Groove + piano seulement). Kit : basse −12,3, piano −14,5, largeur −39, Break = piano seul −14,5 dB.
- Manques : pas de pompe, pas de ride ni de 2e percu, lead décoché, rien ne s'élargit, pas de montée.
- Recette : `"fx"` + `{"on":"bass","preset":"pump"}`, `{"on":"chords","insert":"PUMP","params":[0.45,0.4,0]}`, `{"on":"drums","preset":"punch"}` ; nouvelles lignes `{"role":"ride","pattern":"..o...o...o...o."}` et `{"role":"perc2","voice":"shaker","pattern":"ooxoooxoooxoooxo"}` ; `"drop": ["ride","perc2","open_hat"]` ; `"defaults": {"lead": true}` ; `"riser": true` ; `"break": ["chords","lead","hat","perc"]`.

### Trap (hiphop.json) — 5/10
- Refrain : kick, clap, charleys, charley ouvert, 808, cordes, cloche (+2 sur le couplet). Kit : 808 −7,6 (packs +1,2 : bien), nappe −1,9 (trop forte), lead −13,8, air −34 (charleys muets en kit).
- Manques : trou entre 808 (≤ 87 Hz) et nappe (≥ 277 Hz), lead enterré, mono.
- Recette : `"chords": {"octave": 3}` ; `"fx"` + `{"on":"chords","preset":"wide_alive"}`, `{"on":"lead","delay":0.2}` ; hat `"build": "X...X...X...X...X...X...X...X..."` (couplet en croches, refrain en roulements).

### Drill (hiphop.json) — 4/10
- Refrain : kick, caisse, charleys, rim, 808, cordes, piano. Kit : lead −24,4 (Piano FM, la mélodie qui fait le morceau), nappe −1,6, air −28.
- Recette : `"lead": {"sound": "Piano électrique"}` (ou Piano FM après M1) ; `"chords": {"octave": 3}` ; `"fx"` + `{"on":"chords","preset":"wide_alive","reverb":0.25}` ; `"defaults"` inchangé.

### Pop (bass.json) — 4/10
- Refrain : 6 lignes, basse, piano, lead (+3 couches : bien). Kit : lead −23,4 (Pluck), piano −14,7, basse −8,6 ; packs : −12,5 LUFS, crête 12,8 (creux).
- Recette : `"lead": {"sound": "Lead carré"}` ; `"chords": {"sound": "Piano électrique"}` ; `"fx"` + `{"on":"chords","preset":"wide_alive"}`, `{"on":"drums","preset":"punch"}` ; nouvelle ligne `{"role":"perc2","voice":"shaker","pattern":"....x.......x..."}` (tambourin sur 2 et 4, refrain).

### Phonk (hiphop.json) — 4/10
- Refrain **identique** au couplet (kick, clap, caisse, charleys, 808, cloche) ; nappe décochée. Kit : mono parfait, bas-médium −20.
- Recette : `"defaults": {"chords": true}` (la nappe devient la couche du refrain) ; `"chords": {"octave": 3}` ; nouvelle ligne `{"role":"open_hat","pattern":"..............x...............x."}` ; hat `"build": "x...x...x...x...x...x...x...x..."` ; `"fx"` + `{"on":"drums","preset":"punch"}`.

### Drum & bass (bass.json) — 5/10
- Drop : kick, caisse, charleys, charley ouvert, rim, reese, nappe. Kit : nappe −2,7 contre basse −9,5 ; packs : sub −9,7, kick −10,9 (pas de poids). Break = nappe seule.
- Recette : `"fx"` + `{"on":"chords","insert":"PUMP","params":[0.55,0.4,0]}`, `{"on":"drums","preset":"punch"}` ; nouvelle ligne `{"role":"ride","pattern":"..x...x...x...x...x...x...x...x."}` dans `"drop"` ; `"defaults": {"lead": true}` ; `"break": ["chords","lead","hat","perc"]`. Modèle : M7 (sub sous la reese).

### Reggaeton (hiphop.json) — 3/10
- Refrain : kick, caisse, charley, fill de tom, 808, stabs Pluck. Kit : stabs −22,3, médium −28, largeur −55 : il ne reste que le dembow et la 808.
- Recette : `"chords": {"sound": "Orgue", "octave": 3}` (ou un stab de pack) ; `"defaults": {"lead": true}` (marimba) ; nouvelles lignes `{"role":"perc2","voice":"shaker","pattern":"x.xxx.xxx.xxx.xx"}` (güira) et `{"role":"open_hat","pattern":"..............x."}` ; `"fx"` + `{"on":"chords","preset":"wide_alive","reverb":0.2}`.

### Afrobeats (hiphop.json) — 5/10
- Refrain : kick, rim, shaker, congas, basse, piano électrique, marimba (+2). Kit : mono total (aucun effet dans la recette), lead −14,7, basse −9,5.
- Recette : `"fx": [{"on":"chords","reverb":0.25},{"on":"lead","reverb":0.2,"delay":0.15},{"on":"perc","reverb":0.15},{"on":"drums","preset":"punch"}]`.

### Lo-fi hip-hop (hiphop.json) — 6/10
- Voulu doux et feutré : ce n'est pas un défaut d'être à −13,7 LUFS en packs. Basse −7 à −10, mono, refrain +shaker.
- Recette : hat `"build": "x...x.o.x...x.o."` (couplet plus nu) ; `"fx"` + `{"on":"chords","preset":"wide_alive"}` (après le VINYL : mettre le preset avant l'insert).

### Dubstep (bass.json) — 5/10
- Drop : kick, caisse sur le 3, charleys, rim, wobble, nappe. Kit : nappe −3,1 devant la basse −5,6. Lead décoché ; Break = nappe seule.
- Recette : `"defaults": {"lead": true}` ; `"fx"` + `{"on":"bass","preset":"fat_bass"}` (le synthé « Basse wobble » fait déjà le wob : ici distorsion + OTT pour le poids), `{"on":"chords","insert":"PUMP","params":[0.6,0.45,0]}` ; `"break": ["chords","lead","hat","perc"]`. Modèle : M7 (sub sinus sous le wobble).

### Disco / funk (house.json) — 3/10
- Drop : kick, caisse, charleys, charley ouvert, congas, basse, stabs, cordes. **Basse −28,9 en kit, −24,4 en packs** (« Basse pincée » à −42 LUFS) : la ligne d'octaves qui fait le disco ne s'entend pas ; sub −15 en packs.
- Recette : `"bass": {"sound": "Basse FM"}` (ou `"pack:disco:bass|Basse FM"`) ; `"chords": {"octave": 3}` ; `"fx"` + `{"on":"bass","insert":"PUMP","params":[0.4,0.35,0]}`, `{"on":"chords","preset":"wide_alive"}` ; nouvelle ligne `{"role":"perc2","voice":"shaker","pattern":"x.x.x.x.x.x.x.x."}` (tambourin), `"drop": ["perc2"]`.

### Trance (techno_hard.json) — 4/10
- Drop : kick, clap, charleys, charley ouvert, basse, supersaw, lead. Kit : basse −13,4, supersaw −1,6 (devant tout), lead A5-Do7 (trop aigu) ; packs : sub −12,4.
- Recette : `"bass": {"sound": "Basse ronde"}` ; `"chords": {"octave": 3}` ; `"lead": {"octave": 4}` ; nouvelles lignes `{"role":"ride","pattern":"..x...x...x...x."}`, `{"role":"perc2","voice":"shaker","pattern":"oxoxoxoxoxoxoxox"}`, `"drop": ["ride","perc2"]` ; `"fx"` + `{"on":"master","insert":"OTT","params":[0.25,0.5,0.5,0.5]}`.

### Deep house (house.json) — 5/10
- Drop : kick, clap, charleys, congas, tom, basse, piano électrique. Kit : air −38 (charleys muets), basse −12 ; packs : basse −8, air −19.
- Recette : nouvelle ligne `{"role":"perc2","voice":"shaker","pattern":"oxooxoxooxooxoxo"}` ; `"defaults": {"lead": true}` ; `"fx"` + `{"on":"chords","preset":"wide_alive","reverb":0.3}` (puis PUMP).

### Tech house (house.json) — 6/10
- Drop : 7 lignes (dont ride et shaker), basse, lead voix (+3 couches) : le plus complet. Kit : basse −9,5, lead −10,7, air −20 ; packs : crête 12 (un peu creux).
- Recette : `"defaults": {"chords": true}` (orgue) ou garder ; `"fx"` + `{"on":"drums","preset":"punch"}` (le kick a déjà le sien). Modèle : M7 (sub).

### Boom bap (hiphop.json) — 4/10
- Refrain = couplet + un coup de charley ouvert toutes les 2 mesures. Kit : mono parfait, piano −13,6, basse −10 ; packs : crête 13 (creux).
- Recette : `"chords": {"octave": 3}` ; `"defaults": {"lead": true}` ; nouvelle ligne `{"role":"perc2","voice":"shaker","pattern":"..o...o...o...o."}` ; `"fx"` + `{"on":"drums","preset":"punch"}` (le grain SP-1200), `{"on":"chords","reverb":0.15}`.

### Synthwave (bass.json) — 4/10
- Refrain : kick, caisse, clap, charleys, tom, basse, nappe, arpège. Kit : arpège −24,6 (la signature), basse −11,6, nappe −11,2.
- Recette : `"lead": {"sound": "Lead carré"}` (ou Arpège après M1) ; `"chords": {"octave": 3}` ; nouvelle ligne `{"role":"open_hat","pattern":"..x...x...x...x."}` ; `"fx"` + `{"on":"chords","preset":"wide_alive"}`.

### Acid house (house.json) — 4/10
- Par défaut : batterie 707 + 303, **une seule mesure** de batterie et de 303 pendant 88 mesures ; basse et nappe décochées. Packs : sub −12 (le kick 707 n'a pas de grave), 303 au-dessus de tout.
- Recette : `"defaults": {"bass": true}` ; motif 303 sur 2 mesures (32 pas, la syntaxe existe) ; nouvelle ligne `{"role":"open_hat","source":"tr909","pattern":"..X...X...X...X."}` dans `"drop"` ; `"fx"` + `{"on":"acid","insert":"PUMP","params":[0.5,0.35,0]}`.

### Hard techno (techno_hard.json) — 6/10
- Drop : kick + rumble, clap, charleys, charley ouvert, rim, ride, stabs hoover. Bon poids (crête 9) ; stabs −8,6/−11 ; une seule boucle.
- Recette : `"defaults": {"lead": true}` (supersaw en 2e Drop, voir M5) ; `"fx"` + `{"on":"master","insert":"OTT","params":[0.25,0.5,0.5,0.5]}`.

### Amapiano (house.json) — 5/10
- Drop : kick, shaker, clap, rim, log drum, piano (+piano seulement). Kit : log drum −11,6 ; packs : sub −19,6 (le log drum Plaits à −24 LUFS).
- Recette : `"bass": {"octave": 1}` si M1 n'est pas fait ; `"defaults": {"lead": true}` ; `"fx"` + `{"on":"bass","insert":"PUMP","params":[0.35,0.35,0]}`, `{"on":"chords","preset":"wide_alive","reverb":0.3}` ; nouvelle ligne `{"role":"perc2","voice":"conga","pattern":"...x..x....x..x."}` dans `"drop"`.

### UK garage (house.json) — 6/10
- Refrain +3 couches (shaker, charley ouvert, lead) : bien. Kit : orgue −3,9, basse −9,4, lead −10,4 ; mono −30.
- Recette : `"fx"` + `{"on":"bass","insert":"PUMP","params":[0.45,0.35,0]}`, `{"on":"chords","preset":"wide_alive"}`  (forme « chanson » : pas de souffle montant possible, c'est M4 qui apportera fill + crash avant chaque refrain).

### Minimal (techno_hard.json) — 3/10
- Drop : kick, charley, rim, clap, basse, stabs. **Basse −34 en kit, −25 en packs** (« Basse pincée ») ; stabs −20.
- Recette : `"bass": {"sound": "Basse ronde"}` ; `"chords": {"octave": 3}` ; `"fx"` + `{"on":"bass","preset":"pump"}` ; nouvelle ligne `{"role":"perc2","voice":"shaker","pattern":"o.o.o.o.o.o.o.o."}` dans `"drop"` ; `"riser": true`.

### Afro house (house.json) — 6/10
- Drop : 7 lignes de percussions, basse, nappe, marimba (10 couches). Kit : médium −23,8 (trou), lead −16,6, basse −10.
- Recette : `"chords": {"octave": 3}` ; `"fx"` + `{"on":"chords","preset":"wide_alive","reverb":0.35}` ; `"riser": true` ; `"drop": ["chords","lead"]` (le Groove les garde pour le Drop).

### Jungle (bass.json) — 5/10
- Drop : kick, caisse (grille Amen), charleys, charley ouvert, basse, nappe. Basse −4,8 : bien. Pas de vrai break découpé, pas de montée, Break = nappe seule.
- Recette : `"riser": true` ; nouvelle ligne `{"role":"ride","pattern":"x.x.x.x.x.x.x.x."}` ; `"break": ["chords","lead","hat"]` ; `"defaults": {"lead": true}`.

### Rétro techno (techno.json) — 5/10
- Drop : 6 lignes, basse, hoover. Kit : hoover −16,8, basse −14,1 ; packs (stab du pack) : −6,7, bien.
- Recette : `"bass": {"sound": "Basse ronde"}` ; `"fx"` + `{"on":"drums","preset":"punch"}`.

### Acid techno (techno.json) — 4/10
- Par défaut : batterie + 303, **une mesure** en boucle pendant 120 mesures ; kit : crête 4,5.
- Recette : motif 303 sur 2 mesures ; `"defaults": {"bass": true}` ; nouvelles lignes `{"role":"perc","pattern":"...x..x....x..x."}`, `{"role":"ride","pattern":"..o...o...o...o."}` dans `"drop"` ; `"fx"` + `{"on":"acid","insert":"PUMP","params":[0.5,0.35,0]}`.

### Gabber (techno_hard.json) — 5/10
- Volontairement écrasé (kit −4,7 LUFS, crête 3,7 : c'est le genre). Par défaut : kick, charleys, clap, roulement, stabs hoover ; ni basse ni lead.
- Recette : `"defaults": {"lead": true}` ; `"chords": {"octave": 3}` ; `"fx"` + `{"on":"master","insert":"OTT","params":[0.3,0.5,0.5,0.5]}`.

### Acidcore (techno_hard.json) — 5/10
- Drop : kick, charleys, charley ouvert, 303 (4 couches) ; OTT + saturation au master : efficace mais peu de matière.
- Recette : nouvelle ligne `{"role":"clap","pattern":"....x.......x..."}` dans `"drop"` ; `"defaults": {"chords": true}` ; `"chords": {"octave": 3}`.

### Liquid DnB (bass.json) — 5/10
- Drop : 5 lignes, basse, piano électrique, arpège pluck. Kit : arpège −23,3, piano −10,9 ; packs : crête 13,7 (creux).
- Recette : `"lead": {"sound": "Piano électrique"}` (ou Pluck après M1) ; nouvelle ligne `{"role":"ride","pattern":"x.x.x.x.x.x.x.x."}` (ride de l'intro et du drop) ; `"fx"` + `{"on":"chords","insert":"PUMP","params":[0.45,0.4,0]}`.

### Techno de Detroit (techno.json) — 5/10
- Drop : 6 lignes dont ride, basse, cordes. Kit : basse −14,2 (Basse ronde en octave 1 : fondamentale à 55 Hz, peu d'harmoniques), nappe −7,7.
- Recette : `"bass": {"octave": 2}` ; `"fx"` + `{"on":"bass","insert":"PUMP","params":[0.45,0.35,0]}` ; `"defaults": {"lead": true}`.

### Grime (hiphop.json) — 3/10
- Refrain : kick, clap, rim, basse carrée, stabs Pluck. **Aucun charley** (pas de haut du spectre), stabs −22,9, mono.
- Recette : nouvelle ligne `{"role":"hat","pattern":"x.x.x.x.x.x.x.x.","build":"x...x...x...x..."}` ; `"chords": {"sound": "Orgue", "octave": 3}` ; `"defaults": {"lead": true}` ; `"fx": [{"on":"chords","reverb":0.2},{"on":"lead","delay":0.2}]`.

### Techno industrielle (techno_hard.json) — 6/10
- Drop : kick + rumble, toms, charley, caisse, nappe essaim. Bon poids (crête 7,6), mais 6 couches et une boucle.
- Recette : `"defaults": {"lead": true}` ; nouvelle ligne `{"role":"perc","pattern":"..x...x...x...x."}` (métal) avec `{"on":"perc","insert":"DISTORTION"}`.

### Neurofunk (bass.json) — 4/10
- Drop : kick, caisse, charleys, rim, basse qui parle, nappe, voix robot. Kit : **basse −13,6** (la star du genre), nappe −5,7 devant ; packs : basse −9,4.
- Recette : `"defaults": {"chords": false}` (la nappe n'a rien à faire au Drop en neuro, la garder pour Montée/Break via `"break"`) ; `"fx"` garder fat_bass + PUMP. Modèle : M1 + M7.

### Jersey club (hiphop.json) — 4/10
- Refrain : kick, clap, charley, rim, basse sub, voix. Kit : mono parfait, basse −14, lead −12,5 ; nappe décochée.
- Recette : `"defaults": {"chords": true}` ; nouvelle ligne `{"role":"open_hat","pattern":"......x.......x."}` ; `"fx": [{"on":"chords","preset":"wide_alive"},{"on":"lead","delay":0.2},{"on":"drums","preset":"punch"}]`.

### Speed garage (house.json) — 5/10
- Drop : 5 lignes, basse qui parle, orgue, voix. Kit : basse −9,7, lead −13,9, mono −39 ; Drop ≈ Groove (−0,5 LU).
- Recette : `"chords": {"octave": 3}` ; `"riser": true` ; nouvelle ligne `{"role":"perc2","voice":"shaker","pattern":"ooxoooxoooxoooxo"}` dans `"drop"`. Modèle : M7 (sub).

### Riddim (bass.json) — 4/10
- Drop : kick, caisse + clap sur le 3, charley, rim, wobble (2 notes par mesure, voulu). Kit : wobble −9,7 sous un kick qui domine, crête 8,4 ; **Break = charley + rim à −45 LUFS (−34,5 dB)** : un trou ; packs : −14,5 LUFS, crête 15 (creux).
- Recette : `"break": ["bass","hat","perc"]` + `"sweeps": [{"on":"bass","break":[0.15,0.5]}]` (le wobble filtré tient le Break) ; `"fx"` + `{"on":"drums","preset":"punch"}`, `{"on":"master","insert":"OTT","params":[0.3,0.5,0.5,0.5]}`. Modèle : M1 + M7 (sub sous le wobble).

---

## Plan de correction, par ordre de priorité

Ordre conseillé : les volumes d'abord (M1 + E2, M2, E1), car toutes les autres améliorations s'entendent mal tant qu'une couche est 20 dB trop bas ; puis le liant (M3), la variation (M4) et un Drop plus gros (M5, M6) ; les recettes (partie 1) peuvent avancer en parallèle. Les mesures se refont avec les outils de `scratchpad/corps/` (relancer `probe/Render.kt` puis `tools/driver.sh`), et un test « équilibre » peut entrer dans `engine/host-tests` (voir M10).

### Partie 1 — Recettes (syntaxe d'aujourd'hui, `scripts/packs/recipes/*.json`, puis `recipes_to_kotlin.py`)

Règles générales (à appliquer style par style, le détail est dans les fiches) :
- **R1. Pompe** : `{"on": "bass", "preset": "pump"}` partout en house et techno où elle manque (house, amapiano, uk_garage, minimal, detroit, disco en léger : `{"on":"bass","insert":"PUMP","params":[0.45,0.35,0]}`), sur la 303 en acid (`{"on":"acid","insert":"PUMP","params":[0.5,0.35,0]}`), sur la nappe tenue en house, deep house, trance, dnb, liquid (`{"on":"chords","insert":"PUMP","params":[0.5,0.4,0]}`, après le preset de la nappe s'il y en a un).
- **R2. Liant** : bus batterie `{"on": "drums", "preset": "punch"}` (compresseur + saturation) en techno, house, tech house, dnb, dubstep, riddim, neurofunk, trap, drill, pop, reggaeton, afrobeats ; master `{"on":"master","insert":"COMPRESSOR","params":[0.6,0.2,0.7,0.45]}` + `{"on":"master","insert":"SATURATION","params":[0.2,0.85,1.0,0.8]}` dans tous les styles (OTT léger `{"on":"master","insert":"OTT","params":[0.25,0.5,0.5,0.5]}` en plus pour la bass music, la hard techno, le gabber). Préférer la règle automatique M3 pour ne pas répéter 36 fois la même ligne.
- **R3. Largeur** : nappes tenues `{"on":"chords","preset":"wide_alive","reverb":0.3}` ou `{"on":"chords","insert":"WIDENER","params":[0.75,0.35]}` (pas sur les stabs secs de techno, Detroit gardé en réverbe) ; lead `"delay": 0.2` à `0.3` là où il n'en a pas.
- **R4. Bas-médiums** : descendre les accords d'une octave quand ils sont à plus d'une octave de la basse ou que le médium mesuré est sous −18 dB : `"chords": {"octave": 3}` (4 → 3) en trap, drill, phonk, reggaeton, disco, boom_bap, trance, synthwave, minimal, gabber, acidcore, acid_techno, jersey_club, grime, afro_house, speed_garage. Vérifier à l'oreille que la nappe ne bave pas sur la basse (sinon garder 4 et passer en `"voicing": "seventh"`/`"ninth"`, qui ajoute des notes graves).
- **R5. Couches du Drop** : ajouter les lignes qui manquent pour remplir le haut (ride, shaker, tambourin, charley ouvert) et les mettre dans `"drop": [...]` pour que le Drop ait plus que le Groove ; cocher le lead par défaut (`"defaults": {"lead": true}`) là où il manque et où il porte le Drop/Refrain : house, deep house, dnb, dubstep, jungle, amapiano, detroit, reggaeton, boom bap, grime, hard techno, gabber, industrial (et le monter de volume partout : T1).
- **R6. Couplet plus léger que le refrain** (styles « chanson ») : un `"build"` plus creux sur le charley ou la percu (joué au Couplet seulement), pour que le Refrain arrive avec la ligne complète.
- **R7. Breaks** : `"break": [...]` qui garde charley et percu (et la basse filtrée en house/tech house) ; `"riser": true` là où il manque (house, deep house, speed_garage, jungle, afro_house, detroit, minimal, disco ; formes club seulement) ; un balayage sur la nappe en Break (`"sweeps": [{"on":"chords","break":[0.3,0.9]}]`).
- **R8. Sons trop faibles ou mal choisis** (tant que M1 n'est pas fait) : disco `"bass": {"sound": "Basse FM"}` (la « Basse pincée » est à −42 LUFS) ; reggaeton/techno/minimal stabs « Pluck » → « Orgue » ou « Piano électrique » ou un stab de pack (`pack:<style>:stab|Orgue`) ; pop lead « Pluck » → « Lead carré » ; drill lead « Piano FM » → « Piano électrique » ; trance basse « Basse analogique » → « Basse ronde » ; acid house : cocher la basse (`"defaults": {"bass": true}`) car le kick 707 n'a pas de sub.

### Partie 2 — Assistant et modèle (`project-model`)

| # | Changement | Fichiers | Taille |
|---|---|---|---|
| **M1** | **Volumes des sons calibrés** : ramener chaque son de `SoundLibrary.SYNTH`/`PLAITS` à une cible par famille (basse −16, lead/pluck −16, nappe/clavier −14 LUFS avec les figures de `probe/Levels.kt`). Il faut un gain d'appoint au-delà du LEVEL (+4,4 dB max) : ajouter un paramètre `GAIN` (dB) à `SynthPatch` et `PlaitsPatch` (ajouté en fin de liste, 0 dB par défaut : les anciens projets ne changent pas), rempli par preset (Pluck +15, Pluck future +15, Arpège +19, Basse pincée +26, Piano FM +11, Marimba +10, Cloche +10 (Plaits +5), Lead acide +9, Log drum +8, Basse ronde/reese/analogique/qui parle +5, nappes et orgue −3 à −6). Test : chaque son de la bibliothèque à ±3 LU de sa cible (rendu hôte). | `SynthPatch.kt`, `Plaits.kt`, `SoundLibrary.kt`, `Synth.cpp`/`PlaitsSynth.cpp` (lire le gain), `NativeEngine`/sync, tests | M |
| **M2** | **Mix par défaut par rôle** dans `StarterWizard.build` (nouvelle étape `withMixDefaults` après `withEffects`) : faders cibles en dB relatifs (kick 0 dB réf., clap/caisse −4, charley −12, charley ouvert −14, ride −16, percu −14, basse −2 à −4, accords −8, lead −6, 303 −3), calculés à partir d'une table de sonie par voix du kit et par son (M1) ; pour le kit intégré, kick −6 dB et charleys +6 dB via les tranches de pads (`Mixer.padChannel`, la tranche ne coûte que si elle change le son). Panoramiques par défaut : charley −0,2, charley ouvert +0,2, percu +0,35, percu 2 −0,35, ride +0,25, toms ±0,3 (`DrumLane.pan`). | `StarterWizard.kt`, nouveau `MixDefaults.kt`, `StarterWizardTest` | M |
| **M3** | **Liant automatique** quand la recette ne le fait pas : bus batterie compression légère + saturation, master compresseur (≈ −16 dB, 1,8:1, 11 ms, 100 ms) + saturation douce, OTT léger en bass music / hard. Pompe sur la basse par défaut en famille House et Techno (sauf gabber, acidcore, industrial qui ont la leur), sur la nappe tenue en house/trance/dnb. | `StarterWizard.withEffects` | S |
| **M4** | **Fills et variation toutes les 8 mesures** : pour chaque section, une scène « jumelle » dont la dernière mesure a un fill (roulement caisse claire/clap sur le 4e temps, kick coupé sur le dernier temps, charley ouvert sur la dernière croche), et une crash (voix Cymbale, version raccourcie comme la ride) sur le 1er temps de chaque Drop et de chaque bloc de 16. `songOf` alterne [scène ×1, jumelle ×1] au lieu de [scène ×4]. 4 sections → 8 scènes (≤ 24). Le fill dépend du style (recette `"fill"` optionnelle ; par défaut, dérivé de la ligne de caisse claire/clap). | `StarterWizard.inScenes`, `songOf`, `sectionsOf`, `recipes_to_kotlin.py` (champ `fill`), tests | M |
| **M5** | **Drop 2 différent du Drop 1** : la seconde scène de Drop (après le Break) ajoute une couche retenue au premier (lead une octave plus haut, ride, percu 2, accords en stabs), ou le premier Drop retient le lead jusqu'à sa 2e moitié. Pareil pour le 2e refrain. | `StarterWizard.layeredPlan`/`buildPlan`, `ScenePlan`, `inScenes` | M |
| **M6** | **Couche de contre-chant / stabs du Drop** : quand le lead n'est pas coché, une piste « Stabs » qui joue les accords du Drop en coups courts une octave au-dessus (rythme de la recette ou `LEAD_STABS`), seulement au Drop/Refrain ; quand il est coché, une réponse du lead toutes les 2 mesures (question-réponse) via un 2e motif de recette `"answer"`. | `StarterWizard.startingNotes`, `Part`/piste interne comme le souffle montant, i18n | M |
| **M7** | **Sub sous les basses médium** : en DnB, neurofunk, dubstep, riddim, jungle, tech house, speed garage (et toute basse de pack sans sub), une piste « Sub » (Basse sub, mêmes notes une octave plus bas, coupée au-dessus de 120 Hz, mono, pompée) ; la basse médium reçoit un passe-haut à 120 Hz. | `StarterWizard.build`, `withEffects`, `trackName`, tests | M |
| **M8** | **Refrain des styles « chanson »** : si le refrain n'ajoute aucune couche (`chorusParts` vide et pas de `CHORUS_LINES`), le couplet joue la batterie allégée (charley à la croche au lieu de la double, percu retenue) ou le refrain double le lead à l'octave. Test : chaque style a au moins 2 couches de plus au Refrain/Drop qu'au Couplet/Groove (avec les réglages par défaut). | `StarterWizard.layeredPlan`, `StarterWizardTest` | S |
| **M9** | **Breaks** : par défaut garder charley + percu + nappe élargie + réverbe longue (send 0,4), et le souffle montant sur les 4 dernières mesures ; jamais un pluck seul. | `layeredPlan` (règle du Break), `withEffects` | S |
| **M10** | **Test de non-régression « corps »** : rendu hôte du Drop de chaque style (sans pack), vérifier : crête ≥ 6 dB, basse seule ≥ mix −8 dB, chaque partie cochée ≥ mix −14 dB, air ≥ −24 dB, bas-médium ≥ −18 dB. Les commandes viennent d'une petite exportation JSON du projet (comme `probe/Render.kt`). | `engine/host-tests`, `project-model` (export), CI | M |

### Partie 3 — Moteur (`engine/src/main/cpp/core`)

| # | Besoin | Fichiers | Taille |
|---|---|---|---|
| **E1** | **Kit intégré rééquilibré** : kick −6 dB (ou drive 2,2 → 1,6 et queue max 1,5 s → 0,7 s : son sub à 47 Hz traîne sous la basse), charley fermé +20 dB et un peu plus long, charley ouvert +8, rim +10, cymbale +4. Change le son des projets existants : soit accepté et annoncé dans le changelog, soit un « kit v2 » choisi pour les nouveaux projets. | `DrumKit.cpp` (`kLevels`, `kMaxSeconds`, `kKickDrive`), `engine_tests` | S |
| **E2** | **Gain d'appoint des synthés** (pour M1) : lire un paramètre de gain en dB dans `Synth` et `PlaitsSynth` (aujourd'hui `Level*1.4` et `Level*0.5/32768`). | `Synth.cpp/.h`, `PlaitsSynth.cpp/.h`, `Params.h` | S |
| **E3** | **Master** : le limiteur seul écrase le kick intégré (4 à 7 % du temps au plafond) ; un écrêteur doux avant le limiteur et un relâchement un peu plus long rendraient le kick plus gros sans pomper le reste. À faire après E1/M2 et seulement si les mesures le demandent encore. | `Mixer.cpp` (chaîne master) | S |

Rien d'autre n'est nécessaire côté moteur : panoramique par pad, tranches de pads, compresseur, OTT, saturation, élargisseur, chorus, pompe déclenchée par le kick, « Joue dans » par scène existent déjà.

---

## Fichiers de l'audit (`scratchpad/corps/`)

- `probe/Body.kt` → `out/body.txt`, `out/body.csv` : structure par style, section, configuration (défaut / tout coché), effets, mixeur.
- `probe/Render.kt` → `cmds/*.cmd` (une scène = des commandes moteur), `cmds/*.song` (sections et mesures), `cmds/notes.txt` (sons de packs retenus).
- `harness/render.cpp` + `harness/CMakeLists.txt` (lie `ps_core` de `engine/host-tests`) → binaire `hbuild/render <cmd> <wav> <boucles>`.
- `tools/analyze_batch.py` (sonie, crête, bandes, largeur), `tools/driver.sh` (rendu + mesure de tous les styles), `tools/summary.py`, `tools/report.py`, `tools/patch.py` (ajoute des réglages à une scène pour essayer un correctif).
- Résultats : `stats/done.jsonl` (990 rendus), `stats/summary.txt`, `stats/report_{kit,packs}_{default,all}.txt`, `lvl/levels.jsonl` (sons de la bibliothèque), `kitlvl/k.jsonl` (voix du kit), `proto/techno2.json` + `proto/techno2.jsonl` (prototype du mix corrigé).
- Packs utilisés : `scratchpad/packs/*.zip` décompressés dans `packs/`, convertis en `raw/` (float).
