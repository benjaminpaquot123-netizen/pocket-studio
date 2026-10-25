# Banc de mesure du son : après « des basses qui tapent » (branche `basses`)

Chantier P1 à P7 de `docs/hits/basses-qui-tapent.md` (§ 7, fait au § 11). **En attente de l'écoute de Benjamin** :
la branche n'est pas fusionnée.

Ce qui change (le générateur et le moteur ; les projets enregistrés gardent leur son) :
- **« Basse audible »** (exciteur de graves, effet 29) sur chaque basse, la Basse 2, le Sub, le Sub drop et le pad de
  la 808 des morceaux de l'assistant : 0,5 hip-hop (lo-fi, boom bap 0,2), 0,35 house / techno / pop, 0,25 bass music
  (et les valeurs du § 9 : deep house, disco, synthwave 0,3 ; amapiano, reggaeton 0,4 ; liquid, jungle 0,25…) ; un
  coupe-bas à 28 Hz devant lui quand rien ne coupe déjà le bas de la tranche.
- **La 808 du moteur, version 1**, dans les cinq styles 808 (le reggaeton en plus), la 808 du pack en 2e choix :
  chute de +18 à +24 demi-tons en 20 à 35 ms, attaque relevée pendant la chute, « tone » = harmoniques 4 à 8, drive
  0,7 (phonk 0,9, reggaeton 0,5).
- **Synthé** : 8 réglages ajoutés (enveloppe de hauteur, phase à zéro, vélocité → coupure, sub sinus / triangle et
  plancher à 30 Hz qui prend aussi l'oscillateur 2 une octave dessous, couche médium réglable), neutres par défaut ;
  cinq sons (Basse roulante, Basse pluck, 808 synthé, Donk, Sub chaud) ; toutes les basses de la bibliothèque qui ont
  un sub ont le plancher. Recettes : Basse roulante (tech house, hard techno, techno industrielle, trance), Basse
  pluck (minimal, afro house), Sub chaud (acid techno, acid house), un Sub sous la basse du grime ; la techno, la
  rétro techno, l'afrobeats et le boom bap gardent leur son (mesuré : il passait mieux, ou le Drop perdait 1,2 LU).
- **« Motifs de basse »** : les 11 grilles du § 4 proposées à l'étape Basse de l'assistant (la ligne de la recette par
  défaut : les morceaux du banc ne changent pas de ligne).
- **Master** : le passe-haut à 25 Hz existe (`SetMixVersion 3`, l'export compris) mais n'est pas donné aux morceaux :
  au banc il coûtait 0,6 à 1,3 LU au Drop (acid techno −7,6 → −8,9 LUFS, perte du limiteur 2,5 → 3,0 LU ; à 20 Hz ou
  d'ordre 2 encore −0,6 LU). Les coupe-bas par rôle (RoleMix : 30 Hz ou 0,7 × la note la plus grave, aucun sur le
  Sub), celui du pad 808 et le plancher des subs font le travail (B8 ci-dessous). Après la relecture neuve : l'exciteur
  passe après ces coupe-bas, une grille de basse ne glisse plus, le banc a été refait sur cette version.

## Méthode

- **Avant** : le code de 456206d (l'exciteur et la 808 v2 écrits, pas utilisés), banc complet `scripts/song-bench.sh`
  (37 styles, sons intégrés, passage basse B1-B12) et écoute en direct `scripts/live-bench.sh` (90 s par style).
  JSON : `avant-basses.json`.
- **Après** : 4f3389a (Bass grids: plain words under each grid, power chord hidden for gliding basses, tested). JSON : `apres-basses.json` (la référence de la CI). Le banc complet a tourné sur le code du
  commit « the exciter and the low cut move into place with the song's curves » ; techno, rétro techno, detroit,
  afrobeats et boom bap ont été refaits sur le code final (leurs recettes et le plancher de l'oscillateur 2 ont changé
  ensuite) et remplacent les leurs (`build/bench-apres4`, `-apres5`, `build/live-apres3`, `-apres4`, `-apres5`).
- **Écoute en direct, colonne « basse au téléphone »** : corrigée sur cette branche. L'appli renvoie le solo (coupé) de
  chaque tranche en synchronisant son mixeur après le chargement, ce qui annulait le solo du banc : la colonne lisait
  tout le morceau. `ps_live --solo` ignore désormais les solos de l'enregistrement ; l'« avant » a été rejoué avec son
  propre `ps_live` sur des copies des enregistrements sans ces commandes (même effet).
- **Les sons seuls et les packs** : `basses-calib/sounds_phone.py` et `packs_phone.py` (scratchpad, hors dépôt) :
  chaque son rendu par le cœur, puis l'exciteur et le coupe-bas du moteur comme l'assistant les pose ; les basses et
  808 des packs (Release `sons-packs-4`) décodées par ffmpeg. Le banc des morceaux ne peut pas les charger (l'export
  Robolectric ne décode pas le FLAC).
- Filtre « téléphone » : passe-haut de Butterworth d'ordre 4 à 300 Hz (B2, téléphone moyen) ou 200 Hz (B1), puis
  pondération K ; perte de sonie contre la basse en pleine bande (plus près de 0 = mieux). B3 : la basse filtrée contre
  le morceau filtré pareil (sa place au téléphone).

## Résumé

| Mesure | Avant | Après | Cible |
|---|---|---|---|
| B2 basse au téléphone 300 Hz (dB, médiane) | −12,2 | **−10,4** | ≥ −14 |
| B1 basse au téléphone 200 Hz (dB) | −7,2 | **−6,2** | ≥ −10 |
| B3 place de la basse au téléphone (LU) | −10,5 | **−8,0** | ≥ −10 / −14 |
| B4 harmoniques 200–800 / 30–100 Hz (dB) | −8,4 | **−6,1** | ≥ −14 |
| B5 attaque (dB) | 3,7 | **4,8** | ≥ +2 (+4 808) |
| B8 infra < 30 Hz du mix (dB) | −23,5 | **−25,5** | ≤ −20 |
| Sonie de la partie forte (LUFS) | −8,5 | **−8,5** | cible du style |
| Perte dans la chaîne master (LU) | 3,0 | **2,4** | ≤ 3 |
| B12 sonie ajoutée par l'exciteur (LU, médiane) | – | **+0,05** | ≤ +1 |
| Styles à B2 ≥ −14 dB (cible) | 22 sur 34 | **33 sur 34** | tous |
| Styles à B2 ≥ −16 dB (bloquant) | 27 sur 34 | **34 sur 34** | tous |
| Styles à B8 ≤ −20 dB | 28 sur 37 | **36 sur 37** | tous |
| Écoute en direct : styles sans alerte | 37 sur 37 | **37 sur 37** | 37 |
| Écoute en direct : basse au téléphone 300 Hz (dB, médiane) | −11,9 | **−9,9** | ≥ −14 |

## Chaque son seul (synthé sur Fa2, 808 sur Fa1)

| Son | tél. 300 avant | tél. 300 son seul | tél. 300 après (exciteur) | tél. 200 avant | tél. 200 après | attaque avant | attaque après | exciteur |
|---|---|---|---|---|---|---|---|---|
| Basse ronde | −15,0 | −15,0 | **−12,5** | −8,8 | −7,5 | +5,9 | +6,4 | 0,35 |
| Basse sub | −29,0 | −29,0 | **−13,7** | −21,9 | −9,8 | +5,2 | +5,5 | 0,35 |
| Sub pur | −33,1 | −33,1 | **−16,7** | −26,2 | −14,4 | +0,6 | +1,5 | 0,25 |
| Basse roulante | – | −13,2 | **−11,1** | – | −6,9 | – | +8,1 | 0,35 |
| Basse pluck | – | −12,6 | **−10,5** | – | −6,8 | – | +11,6 | 0,35 |
| 808 synthé | – | −22,1 | **−11,3** | – | −8,5 | – | +0,2 | 0,5 |
| Donk | – | −19,2 | **−13,5** | – | −9,7 | – | +5,5 | 0,4 |
| Sub chaud | – | −32,0 | **−13,9** | – | −11,6 | – | +4,2 | 0,35 |
| 808 trap | −22,1 | −10,5 | **−9,6** | −15,2 | −5,3 | +1,9 | +7,4 | 0,5 |
| 808 drill | −22,0 | −10,0 | **−9,5** | −15,1 | −5,3 | +2,1 | +6,7 | 0,5 |
| 808 phonk | −18,4 | −8,8 | **−8,3** | −13,0 | −4,9 | +2,5 | +7,6 | 0,5 |
| 808 jersey_club | −22,5 | −10,6 | **−10,0** | −15,7 | −5,5 | +1,8 | +5,7 | 0,5 |
| 808 reggaeton | – | −10,8 | **−10,1** | – | −5,8 | – | +6,5 | 0,4 |

## Les basses et 808 des packs (médiane par pack)

| Pack | sons | exciteur | tél. 300 avant | tél. 300 après | tél. 200 avant | tél. 200 après | pleine bande |
|---|---|---|---|---|---|---|---|
| acid_house | 8 bass | 0,35 | −16,4 | **−13,0** | −11,9 | −9,4 | +0,2 |
| acid_techno | 8 bass | 0,35 | −13,4 | **−11,6** | −9,3 | −7,8 | +0,2 |
| afrobeats | 8 bass | 0,35 | −5,9 | **−5,9** | −4,3 | −3,9 | +0,1 |
| amapiano | 8 bass | 0,4 | −14,5 | **−12,0** | −10,4 | −8,2 | +0,4 |
| boom_bap | 8 bass | 0,2 | −19,4 | **−16,7** | −16,2 | −13,0 | −0,2 |
| deep_house | 8 bass | 0,3 | −28,3 | **−15,9** | −20,9 | −11,5 | −0,0 |
| detroit | 8 bass | 0,35 | −16,1 | **−13,1** | −11,1 | −9,0 | +0,3 |
| disco | 8 bass | 0,3 | −17,5 | **−13,5** | −12,1 | −10,0 | +0,0 |
| dnb | 8 bass | 0,25 | −11,2 | **−10,9** | −7,2 | −6,8 | +0,1 |
| drill | 10 808 | 0,5 | −35,4 | **−13,8** | −25,6 | −9,4 | +0,2 |
| dubstep | 18 808/bass | 0,25 | −25,6 | **−16,5** | −18,9 | −12,9 | +0,0 |
| grime | 8 bass | 0,3 | −11,7 | **−10,7** | −7,1 | −6,8 | +0,3 |
| house | 8 bass | 0,35 | −14,5 | **−12,4** | −12,7 | −8,7 | +0,2 |
| industrial_techno | 8 bass | 0,35 | −8,6 | **−8,1** | −7,4 | −6,2 | +0,0 |
| jungle | 8 bass | 0,25 | −3,0 | **−3,1** | −1,9 | −2,0 | +0,1 |
| juno | 8 bass | 0,35 | −11,2 | **−9,9** | −9,4 | −7,6 | +0,3 |
| jupiter | 8 bass | 0,35 | −33,2 | **−15,2** | −25,8 | −11,0 | +0,1 |
| lofi | 8 bass | 0,2 | −17,2 | **−15,2** | −11,1 | −10,3 | −0,0 |
| minimoog | 8 bass | 0,35 | −12,7 | **−10,7** | −10,3 | −7,9 | −0,1 |
| modern_808 | 20 808 | 0,5 | −20,4 | **−12,1** | −15,4 | −8,3 | +0,6 |
| neurofunk | 8 bass | 0,25 | −10,1 | **−9,2** | −8,7 | −7,2 | −0,3 |
| phonk | 10 808 | 0,5 | −14,9 | **−11,2** | −11,7 | −7,9 | +0,9 |
| pop | 8 bass | 0,35 | −13,1 | **−11,3** | −11,8 | −8,9 | +0,2 |
| reggaeton | 10 808 | 0,4 | −35,5 | **−13,3** | −25,2 | −10,2 | +0,3 |
| retro_techno | 8 bass | 0,35 | −7,2 | **−7,0** | −6,1 | −5,6 | +0,3 |
| riddim | 8 bass | 0,25 | −2,3 | **−2,1** | −1,7 | −1,6 | −0,1 |
| speed_garage | 8 bass | 0,35 | −3,9 | **−3,9** | −2,9 | −2,7 | +0,3 |
| synthwave | 8 bass | 0,3 | −8,2 | **−7,3** | −7,3 | −6,3 | −0,0 |
| tb303 | 16 bass | 0,35 | −9,8 | **−9,2** | −8,2 | −6,9 | +0,2 |
| tech_house | 8 bass | 0,35 | −16,0 | **−12,9** | −13,7 | −9,5 | −0,1 |
| techno | 8 bass | 0,35 | −15,4 | **−12,3** | −13,7 | −9,6 | +0,1 |
| tr808 | 10 808 | 0,5 | −34,6 | **−12,3** | −28,6 | −8,6 | +0,7 |
| trance | 8 bass | 0,35 | −11,2 | **−10,2** | −9,9 | −8,0 | +0,1 |
| trap | 10 808 | 0,5 | −39,2 | **−12,7** | −28,5 | −9,1 | +0,3 |
| uk_garage | 8 bass | 0,35 | −6,0 | **−5,7** | −5,7 | −5,0 | −0,2 |

## Style par style (banc des morceaux, au Drop ; écoute en direct, 90 premières secondes)

« → » : avant → après. B2 : cible ≥ −14 dB, bloquant sous −16 dB. B8 : cible ≤ −20 dB, bloquant au-dessus de −15 dB.
B12 ≤ +1 LU bloquant. « Direct » : médiane des secondes où la basse joue (– : pas de basse dans les 90 s). Les attaques
de +23 à +85 dB (detroit, house, tech house) sont des lectures faussées par des notes très courtes ; « – » en hard
techno, gabber : la basse n'a pas de note assez longue pour la lire.

| Style | B2 tél. 300 | B1 tél. 200 | B3 place (300) | B4 harmoniques | B5 attaque | B8 infra | B12 exciteur | Drop (LUFS) | Direct, tél. 300 / 200 |
|---|---|---|---|---|---|---|---|---|---|
| acid_house | – → **–** | – → **–** | – → **–** | – → **–** | – → **–** | −22,4 → −23,9 | – | −8,1 → −8,0 | – → **–** |
| acid_techno | – → **–** | – → **–** | – → **–** | – → **–** | – → **–** | −24,8 → −26,7 | – | −7,6 → −7,5 | – → **–** |
| acidcore | – → **–** | – → **–** | – → **–** | – → **–** | – → **–** | −17,4 → −28,9 | – | −7,9 → −7,5 | – → **–** |
| afro_house | −26,4 → **−10,8** | −19,3 → **−7,2** | −25,5 → **−9,1** | −25,0 → **−6,3** | +2,6 → **+5,8** | −23,4 → −26,1 | +0,02 | −9,0 → −9,0 | −25,0 / −18,8 → **−10,3 / −6,8** |
| afrobeats | −14,2 → **−11,7** | −8,3 → **−6,7** | −10,6 → **−7,2** | −9,4 → **−7,0** | +5,6 → **+4,6** | −7,6 → −27,9 | +0,09 | −9,2 → −9,1 | −13,8 / −7,9 → **−11,4 / −6,5** |
| amapiano | −14,2 → **−11,6** | −9,7 → **−7,9** | −9,2 → **−6,8** | −10,6 → **−7,1** | +7,6 → **+7,7** | −28,9 → −29,1 | +0,16 | −9,0 → −9,0 | −14,6 / −11,0 → **−11,5 / −8,3** |
| boom_bap | −14,4 → **−12,7** | −8,1 → **−7,1** | −9,9 → **−8,0** | −8,4 → **−6,8** | +4,7 → **+3,8** | −11,1 → −25,1 | +0,03 | −10,1 → −10,0 | −14,4 / −8,0 → **−12,8 / −7,2** |
| deep_house | −13,6 → **−12,1** | −7,8 → **−7,2** | −11,5 → **−10,5** | −8,5 → **−7,0** | +3,7 → **+2,8** | −24,1 → −25,3 | +0,06 | −9,0 → −9,0 | −13,1 / −7,3 → **−12,0 / −7,2** |
| detroit | −12,1 → **−10,5** | −7,4 → **−6,1** | −15,4 → **−12,9** | −7,8 → **−5,3** | +4,7 → **+85,4** | −13,3 → −26,4 | +0,03 | −8,0 → −8,1 | – → **–** |
| disco | −5,7 → **−5,8** | −2,4 → **−2,5** | −4,8 → **−4,7** | 12,2 → **12,4** | +5,8 → **+5,9** | −12,4 → −25,3 | +0,02 | −9,0 → −9,0 | −5,7 / −2,2 → **−5,7 / −2,2** |
| dnb | −7,3 → **−7,2** | −4,7 → **−4,5** | −7,1 → **−6,8** | −4,4 → **−3,8** | +3,6 → **+3,5** | −26,3 → −26,3 | +0,05 | −8,5 → −8,5 | −7,1 / −4,5 → **−7,1 / −4,3** |
| drill | −14,4 → **−8,9** | −10,1 → **−6,2** | −9,8 → **−4,6** | −13,5 → **−7,3** | – → **+2,5** | −26,3 → −25,2 | +0,03 | −8,6 → −8,6 | −15,5 / −10,9 → **−9,3 / −6,3** |
| dubstep | −6,1 → **−6,0** | −4,0 → **−3,7** | −7,3 → **−6,7** | −5,3 → **−4,4** | +3,9 → **+3,9** | −25,8 → −25,9 | +0,10 | −8,8 → −8,8 | −5,8 / −3,7 → **−5,8 / −3,5** |
| gabber | −31,3 → **−12,8** | −24,2 → **−9,8** | −52,7 → **−33,6** | −44,6 → **−13,3** | – → **–** | −29,5 → −29,6 | +0,00 | −7,9 → −7,9 | −21,6 / −9,3 → **−12,8 / −8,5** |
| grime | −6,9 → **−6,6** | −5,5 → **−4,5** | −9,0 → **−9,1** | −7,2 → **−6,0** | +3,6 → **+4,7** | −22,2 → −20,3 | +0,07 | −8,5 → −8,6 | −6,8 / −5,4 → **−6,2 / −4,0** |
| hard_techno | −33,0 → **−13,4** | −25,3 → **−10,3** | −53,6 → **−33,5** | −46,0 → **−13,9** | – → **–** | −26,5 → −26,7 | +0,00 | −8,4 → −8,3 | – → **–** |
| house | −12,3 → **−11,0** | −7,0 → **−6,3** | −12,2 → **−11,2** | −5,7 → **−4,2** | +2,6 → **+85,5** | −23,1 → −25,0 | +0,05 | −8,4 → −8,3 | −12,2 / −6,8 → **−10,6 / −6,2** |
| industrial_techno | −36,0 → **−15,4** | −23,9 → **−11,5** | −55,7 → **−34,8** | −49,3 → **−15,7** | – → **–** | −29,7 → −29,6 | +0,00 | −7,6 → −7,6 | – → **–** |
| jersey_club | −20,7 → **−10,8** | −14,7 → **−6,8** | −23,0 → **−11,7** | −19,2 → **−8,9** | +1,2 → **+3,9** | −22,9 → −24,6 | +0,01 | −8,5 → −8,5 | −22,5 / −15,7 → **−11,0 / −6,9** |
| jump_up | −2,8 → **−2,8** | −1,8 → **−1,7** | −6,9 → **−6,8** | −0,5 → **−0,3** | +8,3 → **+8,1** | −22,2 → −23,8 | +0,01 | −8,7 → −8,7 | −1,6 / −1,1 → **−1,7 / −1,1** |
| jungle | −11,1 → **−10,2** | −6,8 → **−5,9** | −10,9 → **−9,6** | −7,3 → **−5,8** | +3,3 → **+3,8** | −23,4 → −23,6 | +0,11 | −8,0 → −8,0 | −10,9 / −6,7 → **−9,8 / −5,9** |
| liquid_dnb | −11,9 → **−11,0** | −7,3 → **−6,4** | −10,5 → **−9,3** | −7,3 → **−5,6** | +3,9 → **+4,6** | −26,0 → −26,7 | +0,12 | −8,4 → −8,4 | −11,7 / −7,3 → **−10,8 / −5,9** |
| lofi | −14,7 → **−13,5** | −8,2 → **−7,7** | −7,0 → **−6,0** | −9,8 → **−8,5** | +4,9 → **+5,7** | −24,4 → −24,5 | +0,02 | −12,1 → −12,1 | −14,8 / −8,2 → **−13,5 / −7,8** |
| minimal | −13,3 → **−10,5** | −8,1 → **−6,8** | −12,3 → **−9,6** | −10,8 → **−7,0** | +4,6 → **+6,1** | −25,2 → −27,2 | +0,11 | −8,8 → −8,4 | −12,4 / −7,1 → **−10,0 / −6,5** |
| neurofunk | −3,8 → **−3,8** | −2,4 → **−2,3** | −6,9 → **−6,8** | −2,5 → **−2,1** | +6,5 → **+5,7** | −23,5 → −23,8 | +0,05 | −8,8 → −8,8 | −8,2 / −5,2 → **−7,1 / −4,4** |
| phonk | −8,0 → **−5,5** | −6,5 → **−3,9** | −5,1 → **−2,1** | −9,8 → **−5,0** | – → **–** | −21,5 → −21,8 | +0,00 | −7,0 → −7,0 | −8,0 / −6,5 → **−5,5 / −3,9** |
| pop | −13,6 → **−11,4** | −7,7 → **−6,7** | −10,8 → **−9,3** | −8,4 → **−6,1** | +4,0 → **+4,0** | −18,4 → −21,2 | +0,09 | −9,0 → −9,0 | −13,4 / −7,6 → **−11,3 / −6,7** |
| reggaeton | −29,0 → **−8,5** | −21,6 → **−6,1** | −30,2 → **−7,7** | −28,5 → **−6,6** | +3,6 → **+3,4** | −9,5 → −26,5 | −0,01 | −9,0 → −9,0 | −30,1 / −22,5 → **−8,7 / −6,2** |
| retro_techno | −7,5 → **−7,4** | −6,2 → **−5,6** | −7,7 → **−7,5** | −8,8 → **−6,4** | +2,5 → **+5,5** | −19,4 → −21,9 | +0,12 | −8,2 → −8,0 | – → **–** |
| riddim | −3,2 → **−2,9** | −2,4 → **−2,1** | −4,6 → **−4,8** | −3,2 → **−2,3** | +10,2 → **+5,8** | −14,3 → −19,6 | +0,07 | −9,8 → −9,7 | −5,3 / −4,2 → **−4,5 / −3,5** |
| speed_garage | −11,4 → **−9,6** | −6,4 → **−5,3** | −10,1 → **−8,1** | −5,7 → **−3,7** | +3,2 → **+4,8** | −25,4 → −25,5 | +0,12 | −8,2 → −8,2 | −11,5 / −6,5 → **−9,5 / −5,2** |
| synthwave | −7,8 → **−7,4** | −5,8 → **−5,4** | −7,0 → **−6,6** | −6,4 → **−5,0** | +3,6 → **+3,4** | −26,4 → −27,6 | +0,04 | −8,1 → −8,0 | −7,9 / −5,8 → **−7,5 / −5,5** |
| tech_house | −12,2 → **−11,5** | −6,4 → **−7,2** | −12,9 → **−12,5** | −7,0 → **−8,6** | +5,4 → **+6,4** | −22,4 → −22,1 | +0,07 | −8,9 → −8,9 | −12,3 / −6,3 → **−11,4 / −7,0** |
| techno | −7,4 → **−6,7** | −6,0 → **−5,0** | −6,4 → **−6,0** | −9,3 → **−6,2** | +1,3 → **+5,6** | −25,4 → −26,1 | +0,01 | −8,6 → −8,6 | −7,3 / −6,0 → **−6,6 / −4,9** |
| trance | −11,7 → **−9,1** | −7,1 → **−6,0** | −14,3 → **−14,0** | −8,1 → **−5,0** | +3,2 → **+2,7** | −25,9 → −26,3 | +0,07 | −8,4 → −8,2 | −10,4 / −5,9 → **−8,5 / −5,6** |
| trap | −18,0 → **−10,7** | −12,5 → **−7,0** | −15,3 → **−7,7** | −16,6 → **−9,7** | +0,1 → **+3,9** | −24,1 → −24,9 | +0,03 | −8,5 → −8,6 | −18,8 / −12,8 → **−10,7 / −7,1** |
| uk_garage | −12,1 → **−10,4** | −5,9 → **−5,1** | −12,2 → **−10,0** | −2,2 → **−0,5** | +3,7 → **+5,8** | −25,8 → −25,9 | +0,14 | −8,4 → −8,4 | −11,7 / −5,8 → **−10,0 / −5,0** |

## Bloquants du banc (`scripts/bench/report.py`)

Bloquants : **B2** (basse au téléphone ≥ −16 dB, cible −14), **B8** (infra ≤ −15 dB, cible −20 : l'acidcore,
−17,4 dB avant, passe à −28,9 depuis les coupe-bas par rôle du Guide ; une basse retombée sous 30 Hz, −3 à −13 dB avant, bloque) et **B12** (exciteur ≤ +1 LU
avant master) : ils se lisent sur la basse et le mix entiers, ne dépendent pas de la ligne jouée, et restent
stables d'un passage à l'autre. Listés seulement : B1, B3, B4, B5 (attaque : des notes trop courtes ou glissées la
faussent), B6 (son écart mesure des notes différentes, pas un même coup répété : 0,1 à 60 dB), B7, B9 (un kick
absent ou doublé par la basse) et B11.

Avant, le même rapport aurait bloqué : B2 Basse au téléphone (300 Hz) ≥ −16 dB (cible −14): afro_house, gabber, hard_techno, industrial_techno, jersey_club, reggaeton, trap; B8 Rien sous 30 Hz : infra ≤ −15 dB du Drop (cible −20): afrobeats, boom_bap, detroit, disco, reggaeton, riddim.

Après : **aucun** (37 sur 37 sur B2, B8 et B12). Restent listés : B2 ≥ −14 (industrial_techno −15,4), B1 (hard_techno, industrial_techno), B4 (industrial_techno −15,7), B8 ≤ −20, la cible (riddim −19,6), B6 et B9 (voir plus haut).

## Extraits d'écoute

`/tmp/claude-0/-home-user-pocket-studio/2ed29bda-5e9a-5850-a6a7-bbf3293c7a09/scratchpad/ab-basses/` : techno, house,
trap (Refrain), jump up ; 30 s à partir de 4 mesures avant le premier Drop, avant et après, chacun aussi à travers
le « téléphone » (`-telephone`, la paire au même gain, la plus forte à −16 LUFS) ; AAC 192 kbit/s, crête vraie décodée
≤ −1 dBTP (méthode de `abtools/abfinal.py`, script `abtools/ab_basses.py`, mesures `ab-basses.json`).

| Extrait | Gain (dB) | LUFS-I avant / après | Crête vraie avant / après (dBTP) | Court terme : fin de la section d'avant → Drop (LU) |
|---|---|---|---|---|
| `techno-avant.m4a` / `techno-apres.m4a` | −3,7 / −3,7 | −13,3 / −13,3 | −2,3 / −2,4 | +3,4 / +3,5 |
| `techno-avant-telephone.m4a` / `techno-apres-telephone.m4a` | −3,6 / −3,5 | −17,7 / −17,4 | −1,0 / −1,2 | +0,2 / +0,4 |
| `house-avant.m4a` / `house-apres.m4a` | −1,4 / −1,8 | −10,6 / −11,0 | −1,5 / −2,2 | +3,2 / +3,1 |
| `house-avant-telephone.m4a` / `house-apres-telephone.m4a` | −3,1 / −3,1 | −16,4 / −16,3 | −1,1 / −1,3 | −0,7 / −0,9 |
| `trap-avant.m4a` / `trap-apres.m4a` | −1,6 / −1,6 | −11,2 / −11,3 | −1,6 / −2,0 | +5,8 / +5,8 |
| `trap-avant-telephone.m4a` / `trap-apres-telephone.m4a` | −3,4 / −3,4 | −18,1 / −17,8 | −1,8 / −1,4 | +2,6 / +2,6 |
| `jump_up-avant.m4a` / `jump_up-apres.m4a` | −4,0 / −4,0 | −13,6 / −13,5 | −3,1 / −2,4 | +2,3 / +2,2 |
| `jump_up-avant-telephone.m4a` / `jump_up-apres-telephone.m4a` | −5,6 / −5,6 | −16,3 / −16,3 | −2,9 / −2,9 | +1,1 / +1,1 |

## Reste à faire

- L'écoute de Benjamin (au téléphone d'abord) ; puis, selon elle, les quantités d'exciteur par famille.
- P8 (sidechain selon la grille), P9 (triolets et accords de puissance dans la notation des recettes, oscillateur 2
  en demi-tons, mode mono), P10 (le haut-parleur du Magic 8 Pro), les nouveaux réglages du synthé à l'écran.
- Couches : un sub sinus sous une basse médium (« Basse roulante » sur « Sub chaud ») est pour G1b (`Track.layers`).
