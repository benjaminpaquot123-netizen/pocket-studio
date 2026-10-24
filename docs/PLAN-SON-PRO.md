# Plan « son parfaitement pro » (2026-10-23)

Sources : l'écoute de Benjamin (« ça ressemble encore à de l'amateur, surtout les mélodies synthé »), le deuxième audit (`audit-34/`, surtout `pro2.md`), l'étude de 10 hits par style (`hits/`, 37 styles, 370 morceaux, plus la règle de la mélodie dans `hits/autres-et-melodie.md`), le banc de mesure (`son/BANC.md`) et le diagnostic de `plan/son.md`.

## Le diagnostic, en cinq causes

Les cinq familles de styles disent la même chose, chacune avec ses chiffres :

1. **Les mélodies et les basses sont des boucles d'une mesure.** Le lead rejoue une cellule d'une mesure 4 fois en la transposant sur chaque accord, avec 2 forces, sans silence, sans réponse ; 8 recettes partagent la même cellule rythmique ; la basse est une boucle de 1 à 2 mesures répétée 8 fois par Drop. Dans les hits : des phrases de 2 à 8 mesures en question / réponse, un motif court qui revient avec un changement, des silences, un seul sommet, une basse qui change toutes les 4 mesures et change de son au deuxième Drop. **C'est la première cause du son « jouet ».**
2. **Les sons sont mono, maigres et immobiles.** Côté / centre entre −17 et −38 dB (cible −15 à −9), une seule couche, aucun mouvement de filtre, un trou de 8 à 11 dB entre 60 et 250 Hz sur les basses de bass music, rien au-dessus de 12 kHz, une seule réverbe mono. Les hits empilent 2 ou 3 couches désaccordées, étalées en stéréo, avec des balayages de filtre par cycles de 8 mesures, un chorus, un délai en ping-pong.
3. **Le Drop ne tombe pas.** Drop − Groove ≈ 0 LU, Drop − Break ≈ 0 (cible ≥ +4), et dans 6 styles de bass music sur 7 la Montée est plus forte que le Drop. Les hits coupent le kick et la basse 2 mesures avant, laissent un temps de silence, posent un crash, ajoutent une couche toutes les 8 mesures. Nos formes sont aussi trop courtes pour la techno (2:40–3:28 contre 5:30–9 min) et trop plates pour le rap (Refrain − Couplet = −0,1 LU).
4. **La batterie est figée et le kick est mou.** Trois forces, aucun décalage, le même fill toutes les 4 mesures, pas de triolets ni de roulements montants, pas de vraie 808, un kick sinus de 1,5 s qui mange toute la marge sans donner de volume. Résultat mesuré : « la sonie est achetée, pas gagnée » : +8,6 dB de gain au limiteur, qui écrase le Drop et remonte le Break.
5. **Le mixage rabote quatre fois le même coup** (Punch sur le kick, bus batterie, master crête, puis écrêteur et limiteur), la pompe n'est pas un vrai sidechain, et la cible de volume vise la moyenne du morceau au lieu du Drop.

Le volume et la propreté acquis en phase 33 (true peak, cible par style, compresseur honnête) restent valables : ce sont les fondations. Ce plan construit dessus.

## Les principes

- **Les notes d'abord, le son ensuite, le mixage en dernier.** Une mélodie pro sur un synthé moyen sonne mieux qu'un arpège de triade sur un synthé pro.
- **Chaque règle est mesurable.** 20 métriques de mélodie calculées sur les notes (tests Kotlin sur 37 styles × 4 ambiances), et le banc pour le son. Rien ne se règle « à l'oreille de la machine ».
- **L'oreille de Benjamin tranche.** Après chaque chantier : A/B de 30 s sur 4 styles (techno, house, trap, jump-up), envoyés dans la conversation.
- **Jamais de volume par écrasement.** La cible de volume ne pousse que ce que les sons et l'arrangement permettent ; la perte au limiteur est plafonnée et affichée.
- **Fil audio intact** : tout ce qui touche au moteur respecte CLAUDE.md (préallocation, pas de verrou).

## Les chantiers, dans l'ordre

### S1 · Mélodies et basses vivantes (générateur et recettes) — effort L
- Le générateur écrit des **phrases de 8 mesures** (A A′ B A″ ou question / réponse), avec un motif de 3 à 5 notes qui revient 3 à 6 fois avec ≤ 2 notes changées, des silences (≥ 1 temps par 4 mesures, ≥ 2 temps en fin de phrase), un seul sommet par phrase, 50–80 % de pas conjoints, des notes d'accord sur les temps forts, une conduite douce entre accords, un registre par style.
- **Trois versions par section** : aperçu du hook en intro, 60 % au couplet, complet au refrain avec doublure à l'octave ; un contre-chant dans les trous ; l'arpège n'est plus jamais seul comme lead.
- **Forces vivantes** : profil par position (1,0 / 0,85 / 0,7), plage ≥ 0,25, crescendo vers le refrain, micro-décalages déterministes.
- **Basses** : motifs de 4 mesures (64 pas) avec variation aux mesures 5–8, changement de son au Drop 2, deux derniers temps de chaque 16 sans basse, glissés pour la 808 et le garage.
- **Recettes** : les 37 réécrites avec les traits des hits (phrases, accords riches sans fondamentale doublée, rythmes d'accords syncopés sur 32–64 pas, tempos et swings corrigés, référence neurofunk corrigée).
- **Vérification** : les 20 métriques M1–M20 de `hits/autres-et-melodie.md` en tests Kotlin ; test « tells » (aucune mesure recopiée 4 fois, aucune cellule partagée entre styles).
- Gain attendu : le plus audible ; c'est la réponse directe au retour de Benjamin.

### S2 · Un arrangement qui tombe (générateur) — effort M
- **Courbe d'énergie** par section : Montée plafonnée, kick et basse coupés 2 mesures avant le Drop, un temps de silence total, crash au 1, sub drop, basse +1,5 dB au Drop, une couche nouvelle toutes les 8 mesures, Drop en 2 scènes de 8, mutes de 8 mesures en techno.
- **Formes par famille** : Club long pour la techno (32/32/32/16/32/32, plafond 5:30), `SongForm.Rap` (Intro 8 – Refrain 8 – Couplet 16…, couplet sans nappe, coupure kick + 808 avant chaque refrain), Song avec pré-refrain, pont et post-refrain, premier refrain ≤ 45 s.
- **Transitions** : pack FX (crash + délai, cymbale inversée, impact, sub drop, descente, rembobinage), roulements de 4 à 8 mesures, faux drop (riddim).
- **Voix** : chops transposés en réponse (house, jersey), stabs toutes les 4 mesures (trap), shouts (DnB), hit bitcrushé (phonk).
- **Vérification au banc** : Drop − Groove ≥ +2 LU, Drop − Break ≥ +4, Montée < Drop, LRA 3–6, +3 LU sur les 4 dernières mesures de montée.

### S3 · Batterie vivante, kicks et 808 de studio (générateur + moteur) — effort L
- **Humanisation** à graine fixe : cycles de force 100/70/85/60 sur charleys, claps à −3/5 ms, charleys en retard, fantômes, fills aux 4/8/16 avec 3 ou 4 fins différentes, crash au 1 de chaque 16, variantes A/B toutes les 8, swing par voie (drill : charleys seuls), caisse « laid-back » (lo-fi, boom bap).
- **Moteur** : `Hit.offset` (micro-décalage) et rampe de vélocité sur les roulements (×2 à ×8, montants, 1/32, triolets), **voix 808 dédiée** (enveloppe de hauteur, clic, saturation, glissé 60–200 ms, accordée), kick 909 court à enveloppe de hauteur accordé (hard, gabber), rumble filtré.
- **Kits échantillonnés par genre** (sources CC0 : Freesound CC0, VCSL, Sonic Pi ; kicks rendus avec Geonkick), couches sub + attaque et clap + caisse, normalisation par rôle, alternance d'échantillons, couches de vélocité ; breaks découpés pour la jungle.
- **Vérification** : σ des vélocités de charley ≥ 0,05, ≥ 6 mesures de batterie distinctes par morceau, kick ≤ 500 ms, 60–250 Hz ≥ −6 dB (808), air entre −20 et −16, préférence à l'aveugle ≥ 8/10.

### S4 · Synthés larges et vivants (moteur + recettes) — effort M–L
- **Largeur** : unisson stéréo du synthé, doublure automatique à l'octave −1, chorus et délai ping-pong par défaut sur les leads, échantillons et SF2 en stéréo, réverbe plate (Mutable, déjà présent) + salle, égaliseur de retour, graves en mono.
- **Mouvement** : balayages de filtre par cycles de 8 mesures, LFO lent sur la coupure, réverbe du lead montée en fin de phrase, automation des potards du synthé et de Plaits (phase 37 du plan global, avancée ici).
- **Basses de bass music** : deuxième couche médium large (passe-haut 250 Hz), basse 80 Hz au lieu de 120, wobble carré (riddim) et périodes 1/8 → 1/4 → 1/16 par bloc de 8, voix reese (DnB) et formant (neuro).
- **Suréchantillonnage** du synthé (HIIR), filtres sst-filters (GPL-3).
- **Vérification** : côté / centre entre −15 et −9 dB, corrélation 0,4–0,85 (≥ 0,95 sous 120 Hz), centroïde du lead variant ≥ ±15 %, 60–250 Hz ≥ −6 dB, repliement ≤ −60 dB.

### S5 · Mixage et master honnêtes (moteur + WizardMix) — effort M
- **Un seul étage** sur le coup de kick (retrait du Punch 2 ms + bus 2 ms doublon), écrêteur doux sur le bus basse avant OTT, saturation de bus.
- **Vrai sidechain** déclenché par l'audio du kick (source au choix), un bus « Musique » qui ne pompe qu'une fois.
- **Cible de volume qui vise le Drop** (court terme) et non la moyenne, perte au limiteur plafonnée à 3 LU et affichée, gain plafonné ; cibles par style revues (deep / afro / amapiano / disco −9, hard techno −6, phonk −7, trap/drill −8,5 sans plafonnement) ; le gain adaptatif en direct ne dérive plus pendant le mixage.
- **Vérification** : LUFS ±1 de la cible sans plafonnement sur 37/37, PLR ≥ 6, Drop − Break ≥ +4 conservé après master, M4A décodé jamais > −1 dBTP.

### S6 · Vérifier et faire juger — effort S, en continu
- Banc étendu : pistes séparées, métriques de notes, courbes de référence par genre, contraste de sections ; rapport CI bloquant sur les nouveaux critères.
- A/B de 30 s (techno, house, trap, jump-up) après S1, S2, S3+S4, S5, envoyés à Benjamin.
- À la fin : audit par des pros (producteur, ingénieur du son, mélodiste), puis une troisième vague des 8 testeurs.

## Ordre et articulation avec le plan global

- S1 → S2 → S3 → S4 → S5, chacun avec son A/B. S1 + S2 ne touchent que Kotlin (générateur et recettes) et donnent déjà « ça ressemble à un morceau » ; S3 + S4 donnent « ça sonne » ; S5 donne « prêt à sortir ».
- Ces chantiers remplacent les parties « son » des phases 34, 37 et 38 de `PLAN-AMELIORATION.md` ; les parties « interface » continuent en parallèle (phase 34 : un seul transport, vocabulaire Partie / Morceau unifié, édition qui ne bouge plus pendant la lecture, boutons en un geste).
- Pas d'APK avant la fin de tout (règle de Benjamin) ; les fusions sur GitHub continuent par phase.

## Ce que Benjamin aura à faire

- Écouter chaque A/B (4 extraits de 30 s) et dire : mieux / pareil / pire, et ce qui cloche encore avec ses mots.
- Répondre à une question : la durée des morceaux de techno et de club (voir ci-dessous).

## Question ouverte

1. **Durée des morceaux club (techno, house, bass music)** : A) passer à 5–6 minutes comme les vrais titres (intro 32, break 16–32) ; B) garder ~3 minutes avec les mêmes proportions. A est plus pro mais plus long à écouter sur un téléphone ; B garde l'écoute courte. Recommandé : A pour la techno et la bass music, B pour la house et tout le reste.
