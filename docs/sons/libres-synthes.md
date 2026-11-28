# Synthés et effets libres : comparaison avec les sons fabriqués

Date : 2026-11-28. Branche : `eq/synthes-libres`.

## But

Pour chaque famille de synthé (stab, pluck, nappe, accords, boucle lead, arpège) et pour les effets et ambiances :
mettre le meilleur son **fabriqué** par l'appli face au meilleur son **libre** trouvé, avec la même distance aux cibles,
garder le meilleur, et lister les sortes de sons qui manquent à l'appli.

## Méthode

- Cibles : `docs/sons/fin/cibles/zone-c-synthes.{md,tsv}` (branche `eq/son-fin-essentia-01` @ `faa70cb3a`).
- Mesure : `scripts/son/essentia/cibles_desc.py`, la même pour tous les sons.
- Distance : même calcul que `scripts/son/cible/c/tgt_c.py` (branche `eq/son-cible-c`) : écart de chaque mesure à la cible
  ramené à l'étalement de la cible (IQR/1,349, avec planchers), écrêté, moyenne quadratique pondérée. **Plus bas = plus près.**
- Cellules : style propre quand la cible existe (nappe/trap, nappe/dnb, accords/dnb, boucle/dnb, fx-nappe/lofi, house,
  techno, dnb, fx-effet/techno, dnb), sinon la cellule « * » (tous les styles).
- Sons fabriqués comparés : les 144 synthés de la page précédente (préréglages Surge, GPL-3.0-or-later) et les 20 effets
  fabriqués (œuvre neuve). La sortie de `eq/son-cible-c` n'était pas encore poussée (tête `3243f2c6`, pas de
  `docs/sons/cible/c`) : ces nouveaux sons ne sont donc pas dans la comparaison.
- Sons libres : Freesound, filtre **CC0 seulement**, 528 sons téléchargés (aperçus HQ MP3 128 k), triés par mots du titre
  (stab, pluck, pad, chord, lead, arp, ambient, riser, impact…) et sans acoustique, voix, batterie ni prise de terrain.
  Chaque son retenu a été vérifié CC0 sur sa page.

## Résultats par famille et par style

| Famille | Cible (styles) | Meilleur fabriqué | Meilleur libre | Verdict |
|---|---|---|---|---|
| stab | stab/* (tous) | synthes__techno-stab-01 : **0.35** | [Shock Stab 06](https://freesound.org/people/nomiqbomi/sounds/578380/) — nomiqbomi, CC0 ([mp3](libres/mp3/freesound-578380.mp3)) : **0.42** | à égalité (écart < 0,3) |
| pluck | pluck/* (tous) | synthes__pop-pluck-01 : **0.44** | [EDM Pluck 1 One-shot F Key-Dry&Wet](https://freesound.org/people/CAT-FOX_ALEX/sounds/845410/) — CAT-FOX_ALEX, CC0 ([mp3](libres/mp3/freesound-845410.mp3)) : **0.65** | à égalité (écart < 0,3) |
| nappe | nappe/trap (trap, drill) | synthes__trap-pad-02 : **0.84** | [Glowing Pad.wav](https://freesound.org/people/staticpony1/sounds/249612/) — staticpony1, CC0 ([mp3](libres/mp3/freesound-249612.mp3)) : **0.75** | à égalité (écart < 0,3) |
| nappe | nappe/* (boom-bap, lo-fi, house, techno, edm, reggaeton-afro, pop, synthwave, rnb) | synthes__lofi-pad-01 : **1.11** | [Angel Awaken Pad](https://freesound.org/people/Kurlyjoe/sounds/425413/) — Kurlyjoe, CC0 ([mp3](libres/mp3/freesound-425413.mp3)) ⚠ stéréo en opposition de phase : **0.90** | à égalité (écart < 0,3) |
| nappe | nappe/dnb (dnb) | synthes__dnb-pad-01 : **1.93** | [Angel Awaken Pad](https://freesound.org/people/Kurlyjoe/sounds/425413/) — Kurlyjoe, CC0 ([mp3](libres/mp3/freesound-425413.mp3)) ⚠ stéréo en opposition de phase : **1.06** | **libre** |
| accords | accords/* (trap, drill, boom-bap, lo-fi, house, techno, edm, reggaeton-afro, pop, synthwave, rnb) | synthes__edm-accords-02 : **0.29** | [Chord Progression-Trance Chord Progression 1-124bpm](https://freesound.org/people/CAT-FOX_ALEX/sounds/849664/) — CAT-FOX_ALEX, CC0 ([mp3](libres/mp3/freesound-849664.mp3)) : **0.68** | **fabriqué** |
| accords | accords/dnb (dnb) | synthes__dnb-accords-03 : **1.04** | [Chord Progression-Trance Chord Progression 1-124bpm](https://freesound.org/people/CAT-FOX_ALEX/sounds/849664/) — CAT-FOX_ALEX, CC0 ([mp3](libres/mp3/freesound-849664.mp3)) : **0.88** | à égalité (écart < 0,3) |
| boucle | boucle-lead/* (trap, drill, boom-bap, lo-fi, house, techno, edm, reggaeton-afro, pop, synthwave, rnb) | synthes__trap-lead-02 : **0.30** | [Funk Lead Loop](https://freesound.org/people/ehohnke/sounds/376416/) — ehohnke, CC0 ([mp3](libres/mp3/freesound-376416.mp3)) : **0.77** | **fabriqué** |
| boucle | boucle-lead/dnb (dnb) | synthes__dnb-lead-01 : **1.25** | [135-trap-melody.wav](https://freesound.org/people/GENERALMiDiGUy/sounds/431187/) — GENERALMiDiGUy, CC0 ([mp3](libres/mp3/freesound-431187.mp3)) : **0.94** | **libre** |
| arpege | boucle-synth/* (tous) | synthes__pop-arp-02 : **0.92** | [Happy Trance - Bass Chord Progression (Arp2) - 120Bpm](https://freesound.org/people/elaineaeris/sounds/509466/) — elaineaeris, CC0 ([mp3](libres/mp3/freesound-509466.mp3)) : **1.92** | **fabriqué** |
| ambiance | fx-nappe/* (trap, drill, boom-bap, edm, reggaeton-afro, pop, synthwave, rnb) | aucun | [Dark_Texture_1.wav](https://freesound.org/people/DneproMan/sounds/256831/) — DneproMan, CC0 ([mp3](libres/mp3/freesound-256831.mp3)) ⚠ stéréo en opposition de phase : **1.02** | libre seul (rien de fabriqué) |
| ambiance | fx-nappe/lofi (lo-fi) | aucun | [Dark_Texture_1.wav](https://freesound.org/people/DneproMan/sounds/256831/) — DneproMan, CC0 ([mp3](libres/mp3/freesound-256831.mp3)) ⚠ stéréo en opposition de phase : **1.69** | pas de verdict (tous loin) |
| ambiance | fx-nappe/house (house) | aucun | [Dark_Texture_1.wav](https://freesound.org/people/DneproMan/sounds/256831/) — DneproMan, CC0 ([mp3](libres/mp3/freesound-256831.mp3)) ⚠ stéréo en opposition de phase : **2.67** | pas de verdict (tous loin) |
| ambiance | fx-nappe/techno (techno) | aucun | [Dark_Texture_1.wav](https://freesound.org/people/DneproMan/sounds/256831/) — DneproMan, CC0 ([mp3](libres/mp3/freesound-256831.mp3)) ⚠ stéréo en opposition de phase : **1.49** | libre seul (rien de fabriqué) |
| ambiance | fx-nappe/dnb (dnb) | aucun | [Dark_Texture_1.wav](https://freesound.org/people/DneproMan/sounds/256831/) — DneproMan, CC0 ([mp3](libres/mp3/freesound-256831.mp3)) ⚠ stéréo en opposition de phase : **0.56** | libre seul (rien de fabriqué) |
| effet | fx-effet/* (trap, drill, boom-bap, lo-fi, house, edm, reggaeton-afro, pop, synthwave, rnb) | claviers-fx__edm-effet-04 : **0.74** | [Lunar Downlifter FX 4.wav](https://freesound.org/people/maqsim/sounds/172157/) — maqsim, CC0 ([mp3](libres/mp3/freesound-172157.mp3)) : **0.61** | à égalité (écart < 0,3) |
| effet | fx-effet/techno (techno) | claviers-fx__edm-effet-04 : **1.08** | [Impacts.wav](https://freesound.org/people/Anuj_Shrestha1/sounds/555637/) — Anuj_Shrestha1, CC0 ([mp3](libres/mp3/freesound-555637.mp3)) : **0.78** | **libre** |
| effet | fx-effet/dnb (dnb) | claviers-fx__pop-effet-02 : **0.82** | [laser zap 2.wav](https://freesound.org/people/.Andre_Onate/sounds/135118/) — .Andre_Onate, CC0 ([mp3](libres/mp3/freesound-135118.mp3)) : **0.61** | à égalité (écart < 0,3) |

### Effets par sorte (cible fx-effet/*)

| Sorte | Meilleur libre | Distance |
|---|---|---|
| montant | [Scary Hits & Risers 002](https://freesound.org/people/Xinematix/sounds/396160/) — Xinematix, CC0 ([mp3](libres/mp3/freesound-396160.mp3)) | 0.77 |
| descendant | [Lunar Downlifter FX 4.wav](https://freesound.org/people/maqsim/sounds/172157/) — maqsim, CC0 ([mp3](libres/mp3/freesound-172157.mp3)) | 0.61 |
| impact | [Impacts.wav](https://freesound.org/people/Anuj_Shrestha1/sounds/555637/) — Anuj_Shrestha1, CC0 ([mp3](libres/mp3/freesound-555637.mp3)) | 0.66 |
| balayage | [Whoosh stereo light (transition)](https://freesound.org/people/xkeril/sounds/701104/) — xkeril, CC0 ([mp3](libres/mp3/freesound-701104.mp3)) | 0.77 |
| laser | [laser zap 2.wav](https://freesound.org/people/.Andre_Onate/sounds/135118/) — .Andre_Onate, CC0 ([mp3](libres/mp3/freesound-135118.mp3)) | 0.71 |
| arrêt bande | rien de libre trouvé | — |

## Ce qu'il faut retenir

Règle de verdict (après la critique) : « à égalité » si l'écart est sous 0,3 ; « pas de verdict » si même le meilleur est au-delà de 1,5.

- **Fabriqués nettement devant** : accords (cellule générale, 0,29 contre 0,68), boucle lead (0,30 contre 0,77), arpège (0,92 contre 1,92).
- **Libres nettement devant** : nappe dnb (1,06 contre 1,93), boucle lead dnb (0,94 contre 1,25) et effet techno (0,78 contre 1,08) :
  nos sons dnb sont les plus loin de leur cible.
- **À égalité** : stab, pluck, nappes trap et générale, accords dnb, effets généraux et dnb. Garder nos fabriqués ; les libres
  servent de second choix.
- **Ambiances : l'appli n'en fabrique aucune.** « Dark_Texture_1 » bouche le trou pour dnb (0,56), techno et la cellule générale ;
  pour lo-fi et house, rien ne colle (pas de verdict).
- Arrêt de bande (« tape stop ») : rien de libre et propre ; à fabriquer.
- Trois sons libres ont une stéréo en opposition de phase (⚠) : ils peuvent s'affaiblir sur le haut-parleur mono du téléphone.

## Critique à l'aveugle (sous-agent neuf)

- Remesure de 5 sons clés (425413, 849664, 172157, techno-stab-01, dnb-pad-01) : mêmes mesures (écart < 2 %), mêmes distances
  à 3 décimales. Débit MP3 comparable des deux côtés (~190 kbps).
- Corrigé ici : écarts trop faibles déclarés « à égalité » ; « Backrooms Ambience » (enregistrement de pièce) et
  « ahhhhhhhh--uplift » (voix probable) retirés ; drapeau ⚠ pour la stéréo en opposition de phase.
- Restent, à savoir en lisant les tableaux :
  1. tailles de pool inégales (cellule « * » : 24 fabriqués de tous styles ; cellule dnb : 3 ; côté libre : toute la famille,
     sans filtre de style, d'où des « dnb » libres qui n'en sont pas) ;
  2. une seule cible fx-effet pour toutes les sortes d'effets (montant, impact, laser…), d'où des égalités ;
  3. boucles et sons uniques comparés sur les mêmes mesures (attaque, déclin) ; « Impacts.wav » et « laser zap 2 » contiennent
     plusieurs coups ;
  4. « Happy Trance (Arp2) » est plutôt une boucle basse et accords.

## Sortes de sons qui manquent à l'appli

L'appli a déjà sirène, corne de brume, laser, montant et impact (sons de fête). Il lui manque :

| Sorte | Son libre proposé |
|---|---|
| hoover (rave) | [hardhouse hoover](https://freesound.org/people/woowah/sounds/11512/) — woowah, CC0 ([mp3](libres/mp3/freesound-11512.mp3)) |
| orchestra hit | [Orch Hit](https://freesound.org/people/BigDumbWeirdo/sounds/90741/) — BigDumbWeirdo, CC0 ([mp3](libres/mp3/freesound-90741.mp3)) |
| voix découpée (vocal chop) | [Chopped Vocal Loop 1](https://freesound.org/people/wenter13/sounds/161507/) — wenter13, CC0 ([mp3](libres/mp3/freesound-161507.mp3)) |
| cloche FM | [FM Elevator bell](https://freesound.org/people/CosmicD/sounds/500357/) — CosmicD, CC0 ([mp3](libres/mp3/freesound-500357.mp3)) |
| basse reese | [Mono Poly Reese Bass](https://freesound.org/people/Walter_Odington/sounds/25595/) — Walter_Odington, CC0 ([mp3](libres/mp3/freesound-25595.mp3)) |
| craquement vinyle | [Vinyl Dust 50](https://freesound.org/people/mitchanary/sounds/614342/) — mitchanary, CC0 ([mp3](libres/mp3/freesound-614342.mp3)) |
| glitch | [Glitch](https://freesound.org/people/Poligonstudio/sounds/392602/) — Poligonstudio, CC0 ([mp3](libres/mp3/freesound-392602.mp3)) |
| ambiance / drone | voir la famille ambiance ci-dessus |
| arrêt de bande, rafale de bruit | rien de libre et propre : à fabriquer |

## Fichiers et licence

Les 21 MP3 sont dans `docs/sons/libres/mp3/freesound-<numéro>.mp3` (aperçu HQ 128 k de Freesound, non retouché).
Tous sont sous **CC0 1.0** (domaine public, redistribution libre, attribution non obligatoire mais notée ici par politesse :
auteur et lien dans les tableaux). Rien d'acheté, rien en « usage personnel ».
