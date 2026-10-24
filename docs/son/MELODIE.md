# Mélodies et basses : les règles et les mesures (chantier S1)

Mesuré sur les notes, sans écouter : `MelodyMetrics` (project-model) calcule les 20 mesures M1–M20 de `docs/hits/autres-et-melodie.md` (§ B13) sur chaque piste, et `MelodyRulesTest` vérifie les 37 styles × 4 ambiances (lead, basse, contre-chant, arpèges). Le tableau ci-dessous sort de `MelodyReportTest` (`build/melodie.md`, à relancer après chaque changement de recette).

## Ce que fait le générateur maintenant (`MelodyBuilder`)

- **Phrases de 8 mesures.** La recette donne une cellule (1, 2 ou 4 mesures) ; le générateur en fait une phrase A A′ B A″ : la question, la question avec sa fin changée, la réponse, une cadence ; puis la réponse décalée d'un degré, la question en séquence, la question qui monte, la cadence finale sur la tonique ou la tierce.
- **Des silences.** Rien dans les deux derniers temps des mesures 4 et 8 (le roulement de batterie tombe dans ce trou) ; au moins un temps de silence par 4 mesures.
- **Un seul sommet**, une note de l'accord placée sur un temps fort des mesures 6–7, juste au-dessus du reste ; les autres notes passent dessous.
- **Pas de saut gratuit** : un saut de plus d'une quarte est suivi d'un pas en sens inverse, un seul saut par phrase.
- **Les notes de l'accord sur les temps forts**, les autres en passage (une croche au plus, résolue d'un pas), jamais un demi-ton contre l'accord sur une note longue.
- **Conduite** : au changement d'accord, la mélodie garde une note commune ou avance d'un pas, au lieu de se transposer avec l'accord.
- **Forces vivantes** : 1,0 sur le temps, 0,82 sur le « et », 0,68 sur les doubles-croches ; le 1er temps plus fort que le 3e, puis les 2e et 4e ; montée vers la fin de la phrase (crescendo vers le refrain).
- **Micro-décalages** de 1 ou 2 ticks (5 à 10 ms), toujours en retard, toujours les mêmes à la même place, jamais sur le 1er temps ni sur un glissé. Graine = style + ambiance : le même morceau à chaque fois.
- **Trois versions du hook** : l'Intro joue la première phrase (aperçu), le Couplet 60 % des notes (les plus importantes), le Refrain tout. Une 4e version, le 2e Refrain doublé à l'octave (montée d'une octave pour un lead qui glisse ou un son de stab), est prête mais pas encore jouée : les deux Refrains partagent la même scène, c'est au chantier S2 (formes) de lui donner la sienne.
- **Contre-chant mélodique** : il reprend le rythme du motif et répond dans les silences du lead (jamais en même temps).
- **Arpèges** : `ArpMode.Pattern`, un motif fixe sur deux octaves, porte à 60 %, accent toutes les 4 notes ; plus jamais une triade qui monte et descend.
- **Basses de 8 mesures** : la ligne, puis sa variation (`bassVariation`) ou la même avec une note d'approche (quinte) à la fin des mesures 4 et 7 ; un temps de silence avant le roulement de la mesure 4, deux temps avant la reprise ; glissés conservés pour la 808 et le garage.
- **Deuxième basse au Drop 2** (`bassSwitch`, neurofunk, jump up, riddim) : une piste « Basse 2 » avec un autre son, qui prend le relais au deuxième Drop (deux courbes de volume, morceau seulement).
- **Accords** : voicings sans fondamentale (`rootless7`, `rootless9`), renversements qui bougent le moins possible (`voice_lead`), rythmes de 32 ou 64 pas, forces légèrement différentes à chaque coup.

## Les recettes (37 réécrites)

Lead en cellule de 2 ou 4 mesures avec tenues, silences et réponses dans le registre du style ; `type` hook (mélodie chantable : toutes les règles), riff (boucle courte : sauts et notes répétées permis), chops (voix) ; basses de 32–64 pas ; rythmes d'accords syncopés ; tempos et swings corrigés (house 0,25, deep 0,3, tech house 0,25, afro house 0,25, disco 116, reggaeton 96 et 8 %, drill 0,12, lo-fi 84, boom bap 92) ; lo-fi en « Rêveur » par défaut ; progressions des hits (synthwave i–VII–VI–iv, i–v–VII–IV, I–vi–IV–V, VI–i–VII–iv ; pop i–iv–VI–VII ; reggaeton i–VII–VI–V ; afrobeats IV–I ; liquid VI–v–i) ; neurofunk : « Noisia – Stigma (2010) » à la place de Machine Gun ; lead coché par défaut en techno, boom bap, lo-fi, phonk, grime, reggaeton ; amapiano : le log drum tombe au Drop.

## Avant / après : nombre de styles qui passent chaque règle

« Avant » : le générateur et les recettes d'avant S1, jugés comme des hooks (31 leads écrits). « Après » : 35 leads écrits (riddim et acid house n'ont pas de lead, par choix du genre), chacun jugé selon son type.

| # | Règle | Lead avant | Lead après | Basse avant | Basse après |
|---|---|---|---|---|---|
| M1 | Phrase de 8 mesures (128 pas) | 0/31 | 35/35 | 37/37 | 37/37 |
| M2 | Motif qui revient 3 à 6 fois sur 16 mesures (≤ 2 notes changées) | 0/31 | 35/35 | — | — |
| M3 | Pas deux mesures identiques de suite | 3/31 | 35/35 | 10/37 | 37/37 |
| M4 | Silences : ≥ 1 temps par 4 mesures, ≥ 2 temps en fin de phrase | 5/31 | 35/35 | 0/37 | 37/37 |
| M5 | Fin de phrase sur une note de l'accord, tenue ou suivie d'un silence | 4/31 | 35/35 | — | — |
| M6 | Densité : 0,35 à 2,5 temps par note | 4/31 | 35/35 | — | — |
| M7 | Un seul sommet, dans la 2e moitié, sur un temps fort | 0/31 | 35/35 | — | — |
| M8 | Pas conjoints, ≤ 1 saut par phrase, sauts résolus | 0/31 | 35/35 | — | — |
| M9 | Notes de l'accord sur les temps 1 et 3 (≥ 80 %) | 8/31 | 35/35 | 37/37 | 37/37 |
| M10 | Notes étrangères courtes et résolues, aucun frottement long | 22/31 | 35/35 | — | — |
| M11 | Ambitus 7–17 demi-tons, registre du style | 7/31 | 35/35 | 30/37 | 37/37 |
| M12 | Forces : plage ≥ 0,25, temps forts plus forts, crescendo | 0/31 | 35/35 | 6/37 | 37/37 |
| M13 | Micro-décalages de 1 à 3 ticks, toujours les mêmes par position | 1/31 | 35/35 | 37/37 | 37/37 |
| M14 | ≥ 3 durées différentes, aucune > 70 % | 1/31 | 35/35 | 21/37 | 37/37 |
| M15 | Syncopes | 30/31 | 35/35 | — | — |
| M16 | Contre-chant dans les silences du lead (< 30 % d'attaques communes) | 0/2 | 6/6 | — | — |
| M17 | Arpège : ≥ 4 notes, ≥ 19 demi-tons, motif fixe, accents | 0/0 | 1/1 | — | — |
| M18 | Conduite : note commune ou pas conjoint aux changements d'accord | 1/31 | 35/35 | — | — |
| M19 | Trois versions : intro = aperçu, couplet ≤ 60 %, refrain complet, 2e refrain doublé | 0/31 | 35/35 | — | — |
| M20 | Le hook s'entend dans ≥ 3 sections (≥ 2 en club) | 23/31 | 35/35 | — | — |

## Le tableau par style (ambiance par défaut)

| Style | Lead | Règles | Silences / 4 mes. | Motif ×16 | Vélocité | Ambitus | Notes d'accord | Basse : règles | Basse : silences | Basse : vélocité | Basse : hauteurs |
|---|---|---|---|---|---|---|---|---|---|---|---|
| techno | riff | ✓ | 3/2 | 8 | 0.54 | 15 | 1.00 | ✓ | 1/1 | 0.68 | 4 |
| detroit | riff | ✓ | 1/2 | 6 | 0.41 | 12 | 1.00 | ✓ | 1/1 | 0.50 | 6 |
| acid_techno | riff | ✓ | 7/6 | 6 | 0.41 | 12 | 1.00 | ✓ | 1/1 | 0.46 | 2 |
| hard_techno | riff | ✓ | 1/3 | 8 | 0.46 | 15 | 1.00 | ✓ | 1/1 | 0.68 | 4 |
| industrial_techno | riff | ✓ | 3/3 | 14 | 0.53 | 8 | 1.00 | ✓ | 1/1 | 0.62 | 4 |
| retro_techno | riff | ✓ | 1/3 | 6 | 0.46 | 14 | 1.00 | ✓ | 1/1 | 0.65 | 6 |
| acidcore | riff | ✓ | 7/7 | 10 | 0.34 | 9 | 1.00 | ✓ | 1/1 | 0.34 | 2 |
| trance | hook | ✓ | 1/1 | 4 | 0.46 | 12 | 0.94 | ✓ | 1/1 | 0.67 | 5 |
| house | riff | ✓ | 1/1 | 6 | 0.46 | 19 | 1.00 | ✓ | 1/1 | 0.50 | 10 |
| deep_house | chops | ✓ | 2/2 | 4 | 0.45 | 12 | 1.00 | ✓ | 1/1 | 0.67 | 6 |
| tech_house | chops | ✓ | 5/5 | 8 | 0.62 | 3 | 1.00 | ✓ | 1/1 | 0.68 | 5 |
| acid_house | — | — | — | — | — | — | — | ✓ | 8/7 | 0.49 | 2 |
| uk_garage | chops | ✓ | 3/2 | 10 | 0.55 | 11 | 1.00 | ✓ | 5/5 | 0.55 | 8 |
| dnb | riff | ✓ | 2/2 | 4 | 0.46 | 17 | 1.00 | ✓ | 1/1 | 0.43 | 6 |
| jump_up | riff | ✓ | 2/2 | 4 | 0.46 | 5 | 1.00 | ✓ | 3/3 | 0.43 | 6 |
| dubstep | riff | ✓ | 7/5 | 8 | 0.46 | 7 | 1.00 | ✓ | 1/1 | 0.39 | 4 |
| minimal | riff | ✓ | 1/1 | 6 | 0.55 | 7 | 1.00 | ✓ | 4/4 | 0.67 | 2 |
| gabber | riff | ✓ | 1/1 | 4 | 0.46 | 14 | 1.00 | ✓ | 1/1 | 0.34 | 5 |
| afro_house | riff | ✓ | 2/3 | 12 | 0.33 | 12 | 1.00 | ✓ | 5/4 | 0.55 | 4 |
| speed_garage | chops | ✓ | 6/4 | 8 | 0.62 | 5 | 1.00 | ✓ | 1/1 | 0.55 | 6 |
| amapiano | riff | ✓ | 3/3 | 12 | 0.55 | 17 | 1.00 | ✓ | 2/2 | 0.55 | 6 |
| disco | riff | ✓ | 1/1 | 8 | 0.28 | 6 | 1.00 | ✓ | 1/1 | 0.53 | 9 |
| liquid_dnb | hook | ✓ | 3/2 | 4 | 0.45 | 12 | 0.98 | ✓ | 1/1 | 0.50 | 9 |
| neurofunk | riff | ✓ | 3/3 | 6 | 0.55 | 6 | 1.00 | ✓ | 3/3 | 0.55 | 3 |
| jungle | riff | ✓ | 3/2 | 12 | 0.44 | 12 | 1.00 | ✓ | 1/1 | 0.46 | 7 |
| riddim | — | — | — | — | — | — | — | ✓ | 3/3 | 0.40 | 3 |
| trap | riff | ✓ | 3/2 | 6 | 0.46 | 12 | 0.97 | ✓ | 1/1 | 0.53 | 8 |
| drill | riff | ✓ | 4/4 | 12 | 0.46 | 11 | 1.00 | ✓ | 1/1 | 0.55 | 9 |
| phonk | riff | ✓ | 1/1 | 4 | 0.55 | 5 | 0.97 | ✓ | 1/1 | 0.46 | 7 |
| boom_bap | riff | ✓ | 3/3 | 4 | 0.34 | 12 | 1.00 | ✓ | 1/1 | 0.46 | 4 |
| grime | riff | ✓ | 1/1 | 6 | 0.46 | 10 | 0.96 | ✓ | 1/1 | 0.46 | 6 |
| jersey_club | chops | ✓ | 3/2 | 6 | 0.54 | 16 | 1.00 | ✓ | 1/1 | 0.50 | 7 |
| lofi | hook | ✓ | 2/2 | 6 | 0.37 | 9 | 1.00 | ✓ | 1/1 | 0.40 | 9 |
| reggaeton | riff | ✓ | 2/1 | 4 | 0.51 | 14 | 1.00 | ✓ | 1/1 | 0.46 | 9 |
| afrobeats | hook | ✓ | 1/2 | 6 | 0.46 | 11 | 0.91 | ✓ | 1/1 | 0.53 | 9 |
| synthwave | hook | ✓ | 3/2 | 6 | 0.44 | 8 | 0.96 | ✓ | 1/1 | 0.46 | 6 |
| pop | hook | ✓ | 1/1 | 6 | 0.46 | 10 | 0.93 | ✓ | 1/1 | 0.54 | 11 |

## Ce qui reste (honnêtement)

- **Notes de l'accord (M9) : trop sages.** Les hits ont 55–85 % de notes d'accord ; nos hooks sont à 91–100 % et nos riffs à 100 %. Le seuil haut est à 100 % pour l'instant : la prochaine étape est d'écrire des appogiatures et des 9es tenues (au lieu de ramener chaque note sur l'accord).
- **Pas conjoints (M8)** : seuil haut à 95 % (au lieu de 80 %), car les notes répétées comptent comme des pas ; quelques hooks (pop Rêveur, afrobeats Rêveur) sont presque tout en pas conjoints.
- **Riffs et chops** sont jugés avec des seuils plus larges que les hooks (sauts, notes répétées, une seule durée pour les chops) : c'est le genre qui le veut.
- Les seuils d'origine de B13 étaient plus stricts sur M9 (≤ 85 %) et M8 (≤ 80 %) : le « avant » a été mesuré avec eux.
