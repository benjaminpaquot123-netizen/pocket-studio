# Batterie libre de droits : kicks et boucles (2026-11-28)

Question : existe-t-il des kicks et des boucles de batterie gratuits, redistribuables, plus proches des cibles que ceux de l'appli ?

Réponse courte :
- **Kicks** : sur 9 styles, 6 ont un kick libre plus proche à la cible médiane (house, DnB, dubstep, rave, hardstyle, trap). Mais avec la règle complète (seuil 0,05, gagnant stable à p25, p50 et p75), **seuls 2 styles ont un kick libre nettement meilleur : dubstep et hardstyle**. Les 4 autres sont « serrés » : la règle dit de garder l'actuel. L'appli gagne nettement en techno ; lofi et « tous » sont serrés.
- **Boucles** : l'appli n'en livre aucune. Nos boucles fabriquées (`eq/son-cible-a`) battent les boucles libres en techno, house, DnB, trap et lofi. Les boucles libres gagnent en boom-bap/hip-hop et couvrent seules dubstep, breakbeat, acoustique et percussions afro-latines.

Tous les fichiers gardés ici sont **CC0** (groupe A) : aucune attribution n'est exigée, on la note quand même.

## Méthode

- Cibles : `docs/sons/fin/cibles/zone-a-kicks-boucles.tsv` (branche `eq/son-fin-essentia-01` @ `faa70cb3a`), descripteurs mesurés avec `scripts/son/essentia/cibles_desc.py` de la même branche.
- Distance (règle de `REGLE-BATTERIE.md`) : d = moyenne sur les descripteurs de |valeur − p50| / (p90 − p10), par famille et par style. Plus d est petit, mieux c'est.
- Descripteurs kick : decay20_s, attack_ms, centroid_hz, rolloff_hz, flatness_db, high4k (en dB), sub120, crest_db, lowflux, low_beat.
- Descripteurs boucle (famille boucle-drums) : decay20_s, centroid_hz, rolloff_hz, flatness_db, high4k (dB), sub120, low300, crest_db, lowflux, low_beat.
- Écartés exprès : durée (le fichier est coupé), LUFS, RMS et crête (le gain est un fader), largeur stéréo, nombre de pics, dissonance.
- Styles cibles : tous, techno, house, dnb, dubstep, lofi, rave, hardstyle. Trap, drill, hip-hop et EDM n'ont pas de cible propre : on les mesure contre « tous ».
- Les mêmes outils ont mesuré :
  - les 640 kicks actuels (20 packs de style, release `sons-packs-6`) ;
  - les kicks et boucles fabriqués ;
  - 227 kicks et 149 boucles libres.

## Kicks : par style

| Style | Cible | Meilleur de l'appli (d) | Meilleur fabriqué (d) | Meilleur libre (d) | Source, licence | Libres meilleurs que l'appli |
| --- | --- | --- | --- | --- | --- | --- |
| techno | techno | pack techno, kick 27 (0,208) | techno-kick-08 (0,680) | Kick - Whale (0,252) | [Freesound 452713](https://freesound.org/people/Hippokopter/sounds/452713/), Hippokopter, CC0 | 0 : l'appli garde |
| house | house | pack deep house, kick 17 (0,336) | house-kick-01 (0,694) | **kick_think (0,313)** | [Freesound 611641](https://freesound.org/people/harrisonlace/sounds/611641/), harrisonlace, CC0 | 3 |
| DnB | dnb | pack dnb, kick 12 (0,222) | drum-and-bass-kick-02 (0,480) | **JH_Kick_6_F (0,165)** | [Freesound 691738](https://freesound.org/people/Johnnie_Holiday/sounds/691738/), Johnnie_Holiday, CC0 | 3 (aussi KICK_2 0,181 et sdbkit-kick 0,192) |
| dubstep | dubstep | pack riddim, kick 06 (0,541) | aucun | **fan_kick (0,225)** | [Freesound 745491](https://freesound.org/people/harrisonlace/sounds/745491/), harrisonlace, CC0 | 24 |
| lofi | lofi | pack lofi, kick 01 (0,104) | lo-fi-kick-01 (0,180) | Boom_Bap_Kick_1 (0,139) | [Freesound 509597](https://freesound.org/people/tripjazz/sounds/509597/), tripjazz, CC0 | 0 : l'appli garde |
| rave | rave | pack retro techno, kick 32 (0,187) | aucun | **3xOsc_Kick (0,156)** | [Freesound 272461](https://freesound.org/people/Hard3eat/sounds/272461/), Hard3eat, CC0 | 6 |
| hardstyle | hardstyle | pack gabber, kick 12 (0,564) | aucun | **Hard_Drum_Kick - Destroyer (0,489)** | [Freesound 536545](https://freesound.org/people/angelkunev/sounds/536545/), angelkunev, CC0 | 4 |
| trap | tous | pack drill, kick 01 (0,110) | trap-kick-06 (0,336) | **Mushroom-kick (0,087)** | [Freesound 369043](https://freesound.org/people/Br4dzdestroyer/sounds/369043/), Br4dzdestroyer, CC0 | 1 |
| tous | tous | pack techno, kick 12 (0,086) | edm-kick-03 (0,300) | Mushroom-kick (0,087) | idem | 0 : égalité |

Test de solidité (règle complète de `REGLE-BATTERIE.md` : écart 2|v − T|/(p90 − p10), cible T prise au p25, au p50 et au p75, gain d'au moins 0,05 aux trois). Gain du libre sur l'appli, à p25 / p50 / p75 :

| Style | Gain p25 | Gain p50 | Gain p75 | Verdict |
| --- | --- | --- | --- | --- |
| dubstep | +0,48 | +0,63 | +0,46 | **libre, sûr** |
| hardstyle | +0,12 | +0,15 | +0,52 | **libre, sûr** |
| house | +0,09 | +0,05 | −0,07 | serré : garder l'actuel |
| DnB (JH_Kick_6_F) | +0,00 | +0,12 | +0,07 | serré : garder l'actuel |
| rave | +0,02 | +0,06 | +0,11 | serré : garder l'actuel |
| trap | +0,04 | +0,05 | −0,05 | serré : garder l'actuel |
| techno | −0,07 | −0,09 | −0,07 | **appli, sûr** |
| lofi | +0,03 | −0,07 | −0,11 | serré |
| tous | −0,06 | −0,00 | +0,05 | serré |

Relecture à l'aveugle (agent neuf, mesures refaites avec librosa) :
- les calculs à la main redonnent les mêmes distances ;
- le centroïde et la tenue −20 dB sont retrouvés à ±4 Hz et à l'identique ;
- la planéité lue par librosa est 1,4 à 8 dB plus haute (définition différente) ;
- « dubstep » est solide ;
- « DnB » est dans le bruit : une fenêtre de 10 ms pour la tenue inverse le classement, et un pas de rolloff (10,8 Hz) vaut 0,5 d'écart en DnB ;
- les cases DnB et dubstep ne reposent que sur 2 packs ;
- les correspondances retro_techno → rave et gabber → hardstyle sont discutables.

Remarques :
- Nos kicks fabriqués sont loin derrière partout (0,18 à 0,69). Ça confirme l'abandon de la fabrication de kicks.
- Hardstyle et dubstep restent loin de leur cible, même avec le meilleur libre (0,49 et 0,23). Pour le hardstyle, rien de libre ne colle vraiment.
- Le meilleur libre est choisi parmi tous les candidats, quel que soit son style annoncé. Le meilleur de l'appli est choisi dans les packs du style.

## Boucles : par style

| Style | Cible | Meilleure fabriquée (d) | Meilleure libre (d) | Source, licence |
| --- | --- | --- | --- | --- |
| techno | techno | **techno-boucle-07 (0,293)** | Minimal Techno Drum Loop (0,347) | [Freesound 665072](https://freesound.org/people/Solar01/sounds/665072/), Solar01, CC0 |
| house | house | **house-boucle-03 (0,233)** | 01_drumloop_house (0,506) | [Freesound 251642](https://freesound.org/people/frosthardr/sounds/251642/), frosthardr, CC0 |
| DnB | dnb | **drum-and-bass-boucle-05 (0,178)** | drumhorn120bpm (0,247) | [Freesound 152474](https://freesound.org/people/dayvonjersen/sounds/152474/), dayvonjersen, CC0 |
| dubstep | dubstep | aucune | **CAT_RATS_DRUM_LOOP (0,231)** | [Freesound 152355](https://freesound.org/people/supermatt1896/sounds/152355/), supermatt1896, CC0 |
| trap/drill | tous | **drill-boucle-05 (0,159)** | Hip-Hop Drum Loop 179 bpm (0,363) | [Freesound 573970](https://freesound.org/people/blakengouda/sounds/573970/), blakengouda, CC0 |
| boom-bap/hip-hop | tous | boom-bap-boucle-04 (0,157) | **Boom-Bap Drum Loop 2 (0,127)** | [Freesound 459419](https://freesound.org/people/ethanchase7744/sounds/459419/), ethanchase7744, CC0 |
| lofi | tous | **lo-fi-boucle-05 (0,144)** | Drums 170 BPM (0,198) | [Stargate sample pack](https://github.com/stargatedaw/stargate-sample-pack), microlag, CC0 |
| EDM | tous | **edm-boucle-02 (0,132)** | aucune boucle complète libre trouvée | |
| reggaeton/afro | tous | **reggaeton-afro-boucle-08 (0,177)** | percussions seules, voir plus bas | |
| breakbeat | tous | aucune | **funk_beat (0,172)** | [Freesound 441049](https://freesound.org/people/jnvrbs/sounds/441049/), jnvrbs, CC0 |
| acoustique/rock | tous | aucune | **Trip_Acoustic_Beat (0,235)** | [Freesound 385676](https://freesound.org/people/Scydan/sounds/385676/), Scydan, CC0 |
| générique 120 | tous | aucune | **120-crossed (0,197)** | [Stargate sample pack](https://github.com/stargatedaw/stargate-sample-pack), Fugue State Audio, CC0 |

Attention : nos boucles fabriquées ont été réglées sur ces mêmes cibles. Leur avance est donc en partie mécanique. Une écoute départagera.

### Ce qui manque à l'appli et existe en libre

- Toute boucle : l'appli n'en livre aucune aujourd'hui (les packs de style ne contiennent que des one-shots).
- Styles sans boucle fabriquée, mais avec du libre redistribuable :
  - **dubstep** (5 boucles CC0) ;
  - **breakbeat / funk** (7) ;
  - **acoustique / rock** (9) ;
  - **percussions afro-latines** : 17 boucles de pandeiro, congas et shaker, la meilleure étant [Loop_Pandeiro_197](https://freesound.org/people/jacksonjust/sounds/109981/) (jacksonjust, CC0, d tous 0,215).
- Types de boucles absents (ni appli, ni fabriqué), présents en libre :
  - boucles de charley seules (16) ;
  - percussions seules (24) ;
  - caisse claire seule (7) ;
  - « top loops » (3) ;
  - boucles de kick seules (3).
  - Mesurées ici contre la famille boucle-drums seulement. Il faudrait les comparer aux familles boucle-hats et boucle-perc avant de les adopter.
- Rien de libre et propre trouvé pour : mesures impaires, EDM complète, hardstyle.

## Fichiers gardés dans le dépôt (groupe A, tous CC0)

`docs/sons/libres/kicks/` : un fichier par style gagnant ou proche, nommé `<style>_<source><id>.mp3`. `docs/sons/libres/boucles/` : même principe.

- Les fichiers Freesound sont les aperçus MP3 haute qualité (l'original exige un compte). Les mesures portent sur ces aperçus.
- Les deux fichiers Stargate ont été convertis de WAV en MP3 192 kb/s.
- Attribution, même si CC0 ne l'exige pas :

| Fichier | Auteur | Source |
| --- | --- | --- |
| kicks/techno_fs452713.mp3 | Hippokopter | freesound.org/s/452713 |
| kicks/house_fs611641.mp3 | harrisonlace | freesound.org/s/611641 |
| kicks/dnb_fs691738.mp3 | Johnnie_Holiday | freesound.org/s/691738 |
| kicks/dnb_fs512175.mp3 | kopreusz | freesound.org/s/512175 |
| kicks/dnb_stargate_fuguestate_sdbkit.mp3 | Fugue State Audio | stargate-sample-pack |
| kicks/dubstep_fs745491.mp3 | harrisonlace | freesound.org/s/745491 |
| kicks/lofi_fs509597.mp3 | tripjazz | freesound.org/s/509597 |
| kicks/rave_fs272461.mp3 | Hard3eat | freesound.org/s/272461 |
| kicks/hardstyle_fs536545.mp3 | angelkunev | freesound.org/s/536545 |
| kicks/trap_fs369043.mp3 | Br4dzdestroyer | freesound.org/s/369043 |
| boucles/techno_fs665072.mp3 | Solar01 | freesound.org/s/665072 |
| boucles/house_fs251642.mp3 | frosthardr | freesound.org/s/251642 |
| boucles/dnb_fs152474.mp3 | dayvonjersen | freesound.org/s/152474 |
| boucles/dubstep_fs152355.mp3 | supermatt1896 | freesound.org/s/152355 |
| boucles/trap_fs573970.mp3 | blakengouda | freesound.org/s/573970 |
| boucles/boombap_fs459419.mp3 | ethanchase7744 | freesound.org/s/459419 |
| boucles/lofi_stargate_microlag_170.mp3 | microlag | stargate-sample-pack |
| boucles/generique_stargate_fuguestate_120.mp3 | Fugue State Audio | stargate-sample-pack |
| boucles/breakbeat_fs441049.mp3 | jnvrbs | freesound.org/s/441049 |
| boucles/acoustique_fs385676.mp3 | Scydan | freesound.org/s/385676 |
| boucles/afro-latin-perc_fs109981.mp3 | jacksonjust | freesound.org/s/109981 |

Les autres candidats mesurés sont restés hors du dépôt : Hydrogen (GPL-2+ et CC-BY 3.0), Chris Beckstrom Volca (CC-BY 4.0) et TamTam/OLPC (CC-BY 3.0). Aucun ne gagnait un style.

## Groupe B : gratuit mais non redistribuable (jamais d'audio dans le dépôt)

Ces sources n'ont **pas été téléchargées ni mesurées** dans cette session : aucune distance. Les licences sont résumées de mémoire et à revérifier sur chaque page avant tout usage.

| Source | Lien | Licence (résumé) | Styles |
| --- | --- | --- | --- |
| MusicRadar SampleRadar | https://www.musicradar.com/news/tech/free-music-samples-royalty-free-loops-hits-and-multis-to-download-sampleradar | libre dans un morceau, pas de redistribution des samples | tous |
| Looperman | https://www.looperman.com/ | libre dans un morceau, revente ou redistribution interdite | tous (boucles) |
| 99Sounds | https://99sounds.org/ | libre en production, redistribution interdite | boîtes à rythmes, percussions |
| SampleFocus | https://samplefocus.com/ | libre en musique, pas de redistribution | tous |
| Bedroom Producers Blog | https://bedroomproducersblog.com/free-samples/ | selon le pack, souvent « ne pas redistribuer » | 808, 909, lofi |
| Reverb Drum Machines | https://reverb.com/software/samples-and-loops/reverb/3514-reverb-drum-machines-the-complete-collection | gratuit avec compte, pas de redistribution | boîtes à rythmes vintage |
| SampleSwap | https://sampleswap.org/ | envois mélangés, droit de redistribution flou | breaks, dnb, hip-hop |
| Bandcamp « prix libre » | https://bandcamp.com/tag/sample-pack | selon l'artiste, rarement redistribuable | tous |

Écartés : sources sans licence claire ou contenant des breaks non libérés (type Amen). Ce sont Dirt-Samples, danigb/samples, Audiophob, ainsi que trois réenvois sur archive.org (Analog Drum Loops, STRANJAH, 55+kicks).

Pas encore vérifiés : SCC Drums, les kits CC0 de Renoise, OpenGameArt et Wikimedia Commons.

## Limites

- Une seule mesure par fichier. Une différence de d inférieure à environ 0,03 n'est pas significative : en DnB et en trap l'écart est net, pour « tous » c'est une égalité.
- Les aperçus Freesound sont des MP3 : les aigus au-delà de 16 kHz sont coupés, ce qui pèse un peu sur rolloff et high4k.
- La distance ne remplace pas l'oreille : avant de remplacer un kick de l'appli, écouter le candidat dans un morceau du style.
