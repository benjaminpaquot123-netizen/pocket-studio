# Tour 10 : plan du tour de contrôle (après la réparation du tour 9)

Lecteur-trieur, 20 novembre 2026. Base lue : intégration `ccr-80af1a1b-mn0wec` à `c87c8f58` (toutes les fusions de lots du tour 9 jusqu'à miettesB `cc99fcfa`), plus les branches `eq/tour-9-*` non entrées (L13, L18, L19, textes, base-rouges, fiche-tuto).
Liste gelée : `tour-9-tri/defauts.tsv` (24 défauts : D01…D18, S17, S30, S34, S56, S67, S76 ; les lignes de regroupement F-TXT, F-COUPE, F-AUTRE ne comptent pas) et `tour-9-tri/finitions.tsv` (F001…F142) = **166**.

Mots : **tenté** = corrigé dans l'intégration, pas encore vu par un testeur neuf. **tenté, pas entré** = corrigé sur une branche de lot qui n'est pas dans l'intégration. **non reproduit** = le lot n'a pas revu le défaut ; un test de garde reste. **pas pris** = aucun lot ne l'a corrigé. **abandonné (proposé)** = le lot propose de le retirer de la liste ; au chef de trancher.

## Totaux

| état | nombre |
|---|---|
| tenté | 138 |
| tenté, pas entré | 10 |
| non reproduit | 7 |
| pas pris | 10 |
| abandonné (proposé) | 1 |
| **total** | **166** |

Sur les 24 défauts (D et S) : 16 tenté, 8 pas pris. Sur les 142 finitions : 122 tenté, 10 tenté, pas entré, 7 non reproduit, 2 pas pris, 1 abandonné (proposé).

À noter pour le chef :
- **Les six défauts de validation du tuto (D03, D07, D09, D15, D18, S67) n'ont été pris par aucun lot** : le lot L3 n'a traité que des finitions. D09 vient de 21 sièges. D05 (contenu, prévu pour L5) et D16 (pistes séparées, décision jamais prise) sont dans le même cas. Ces 8 défauts (2 bloquants : D03, D07 ; 5 graves ; D15 moyen) restent à réparer avant le contrôle.
- **D10 n'est tenté qu'à moitié** : le menu « A ▾ » sait renommer, mais `TopBar` ne reçoit jamais `renameScene` depuis `AppRoot.kt` (seul `SessionActions.rename` est branché) ; l'entrée « Renommer » reste donc cachée. Le siège qui le contrôle le verra encore rouge.
- **D06** : L7 l'a encore vu à 360 dp / 200 % (fr, speed_garage ch.3) après L1 ; la fiche tuto le garde en reprise.
- **L13 (F001…F011)** n'est pas entré (`eq/tour-9-L13`, tête `6620654d`). F084 (côté espagnol) attend sur `eq/tour-9-textes` ; F136 (effets ouverts hors du Mix) n'a qu'un test rouge sur `eq/tour-9-L18`.
- `corrections.tsv` dit encore « pas pris » pour F078, F081, F084, F086, F087, F089, F093, F099 : L16 les a tentés (voir le tableau).

## 1. Recomptage des 166

| id | gravité | zone / lot prévu | état | lot (fusion) | preuve (commit, test, rejeu) |
|---|---|---|---|---|---|
| D01 | bloquant (douteux app/banc) | L2 Mix Pro | tenté | L2 b7848dda | banc réparé (BenchMixFaderCoverTest) ; Tour9MixFaderNoBenchTest#d01_* |
| D02 | bloquant | L1 Panneau tuto | tenté | L1 485cf04d | T9PanelTargetTest ; rejeu t9-d02-cases-grille |
| D03 | bloquant | L3 Validation/démo | pas pris | L3 prévu | le lot L3 n'a traité que des finitions |
| D04 | bloquant | L4 Feuille du coup | tenté | L4 f921e7b2 | tests Compose rouge puis vert (rapport L4) |
| D05 | grave | L5 Contenu cours | pas pris | L5 prévu | aucun lot L5 ne l'a pris (note L15) |
| D06 | bloquant | L7 Piano roll | tenté | L1 485cf04d | rejeu t9-d06-pointilles-200 (L7 : encore vu à 360 dp / 200 %, fr) |
| D07 | bloquant/grave | L3 Validation/démo | pas pris | L3 prévu | le lot L3 n'a traité que des finitions |
| D08 | grave | L6 Structure | tenté | L6 6120a5b2 | Tour9AddKeepsPlaceTest |
| D09 | grave (fréquence ~21 sièges) | L3 Validation/démo | pas pris | L3 prévu | le lot L3 n'a traité que des finitions |
| D10 | grave | L6 Structure | tenté | L6 6120a5b2 + a7672e44 | renommer prévu, mais TopBar renameScene jamais branché dans AppRoot : l'entrée reste cachée |
| D11 | grave | L2 Mix Pro | tenté | L2 b7848dda | Tour9MixFaderNoBenchTest#d11_* |
| D12 | grave | L2 Mix Pro (ou L5 texte) | tenté | L2 b7848dda | Tour9MixFaderNoBenchTest#d12_aPadsSendsAreFoundFromTheMix |
| D13 | grave | L1 Panneau (visibilité) + L5 texte | tenté | L1 485cf04d | rejeux t9-d13-volume-cible-nl, t9-d13-volume-cible-id |
| D14 | moyen (1 grave : 44) | L1 Panneau tuto | tenté | L1 485cf04d | T9ReadMoreTest ; rejeu t9-d14-lire-la-suite |
| D15 | moyen | L3 Validation | pas pris | L3 prévu | le lot L3 n'a traité que des finitions |
| D16 | grave (à trancher) | L5 Contenu cours (ou export) | pas pris | à trancher | décision « pistes séparées » jamais prise |
| D17 | grave (douteux banc) | Kits (à vérifier) | tenté | L9 a3f09d3a + L11 86d71a17 | banc BenchFlac ; rejeu t9-75-kit |
| D18 | grave | L3 Validation/démo | pas pris | L3 prévu | le lot L3 n'a traité que des finitions |
| S17 | grave | L7 Piano roll | tenté | L7 32350f94 | rejeu t9-s17-glisse-pointille |
| S30 | grave | L2 Mix simple | tenté | L2 b7848dda | Tour9MixFaderNoBenchTest#s30_* (fd71211f) |
| S34 | moyen | L2 Mix Pro | tenté | L2 b7848dda | Tour9MixFaderNoBenchTest#s34_* |
| S56 | grave | L4 Feuille/grille rythme | tenté | L4 f921e7b2 | tests Compose rouge puis vert (rapport L4) |
| S67 | grave | L3 Validation | pas pris | L3 prévu | le lot L3 n'a traité que des finitions |
| S76 | grave | L4 Grille rythme | tenté | L4 f921e7b2 | tests Compose rouge puis vert (rapport L4) |
| F001 | finition | L1 Panneau tuto | tenté, pas entré | L13 eq/tour-9-L13 (6620654d) | rouge/vert sur la branche ; pas dans l'intégration |
| F002 | finition | L1 Panneau tuto replié | tenté, pas entré | L13 eq/tour-9-L13 (6620654d) | rouge/vert sur la branche ; pas dans l'intégration |
| F003 | finition | L1 Panneau tuto | tenté, pas entré | L13 eq/tour-9-L13 (6620654d) | rouge/vert sur la branche ; pas dans l'intégration |
| F004 | finition | L1 Panneau tuto | tenté, pas entré | L13 eq/tour-9-L13 (6620654d) | rouge/vert sur la branche ; pas dans l'intégration |
| F005 | finition | L1 Panneau tuto | tenté, pas entré | L13 eq/tour-9-L13 (6620654d) | rouge/vert sur la branche ; pas dans l'intégration |
| F006 | finition | L1 Panneau tuto | tenté, pas entré | L13 eq/tour-9-L13 (6620654d) | rouge/vert sur la branche ; pas dans l'intégration |
| F007 | finition | L1 Panneau tuto (étapes restantes) | tenté, pas entré | L13 eq/tour-9-L13 (6620654d) | rouge/vert sur la branche ; pas dans l'intégration |
| F008 | finition | L1 Panneau tuto | non reproduit | L13 | garde sur la branche L13 (pas entrée) |
| F009 | finition | L1 Panneau tuto (Pourquoi ?) | tenté, pas entré | L13 eq/tour-9-L13 (6620654d) | rouge/vert sur la branche ; pas dans l'intégration |
| F010 | finition | L1 Panneau tuto (démo) | tenté, pas entré | L13 eq/tour-9-L13 (6620654d) | rouge/vert sur la branche ; pas dans l'intégration |
| F011 | finition | L1 Carte des parties / courbe d'énergie | tenté, pas entré | L13 eq/tour-9-L13 (6620654d) | rouge/vert sur la branche ; pas dans l'intégration |
| F012 | finition | L1 Carte « Bravo » | tenté | L14 30b80081 | rouge/vert, rapport L14 |
| F013 | finition | L1 Panneau tuto (Arrêter la démo) | tenté | L14 30b80081 | rouge/vert, rapport L14 |
| F014 | finition | L1 Panneau tuto / barre du haut | tenté | L14 30b80081 | rouge/vert, rapport L14 |
| F015 | finition | L1 Panneau tuto replié | non reproduit | L14 | test de garde gardé |
| F016 | finition | L1 Bandeau « Télécharger le kit » | tenté | L14 30b80081 | rouge/vert, rapport L14 |
| F017 | finition | L1 Bandeau « Télécharger le kit » | tenté | L14 30b80081 | rouge/vert, rapport L14 |
| F018 | finition | L1 Feuille « Tonalité du morceau » | tenté | L14 30b80081 | rouge/vert, rapport L14 |
| F019 | finition | L1 Fin du tuto | tenté | L14 30b80081 | rouge/vert, rapport L14 |
| F020 | finition | L1 Panneau tuto (compteur d'étape) | pas pris | L14 | compteur dans TutoChecks.kt, hors fichiers du lot |
| F021 | finition | L1 Feuille d'effets + panneau tuto | tenté | L14 30b80081 | rouge/vert, rapport L14 |
| F022 | finition | L1 Panneau tuto | non reproduit | L14 | test de garde gardé |
| F023 | finition | L2 Mix | tenté | L2 b7848dda | Tour9MixFinishTest |
| F024 | finition | L2 Mix | tenté | L2 b7848dda | Tour9MixScrollTest |
| F025 | finition | L2 Mix (feuille d'une piste) | tenté | L2 b7848dda | Tour9MixFinishTest |
| F026 | finition | L2 Mix simple | tenté | L2 b7848dda | Tour9MixFinishTest |
| F027 | finition | L2 Mix simple | tenté | L2 b7848dda | Tour9MixFinishTest |
| F028 | finition | L2 Mix (envoi réverb) | tenté | L2 b7848dda | Tour9SendRailTest |
| F029 | finition | L2 Effets d'une piste | tenté | L2 b7848dda | Tour9EffectAddTest (2dfd1643) |
| F030 | finition | L2+L8 Mix / Partager (arabe) | tenté | L2 b7848dda + L8 92587924 | Tour9MixFinishTest ; ShareSignedRtlTest |
| F031 | finition | L3 Grille rythme (étapes coups doux / accents) | pas pris | L3 | tenté puis retiré à la relecture (3 gestes par coup) : au chef de trancher |
| F032 | finition | L3 Validation d'étape | tenté | L17 d9a532f2 | 76d693c0 + 518d76d0 |
| F033 | finition | L3 « Je le fais » / démo | tenté | L3 a7c4ab95 | rouge/vert dfcb7741 |
| F034 | finition | L3 Démo « Montre-moi » | tenté | L17 d9a532f2 | e50d28b0 |
| F035 | finition | L3 Démo « Montre-moi » | non reproduit | L3 | Tour9TutoAfterTheDemoTest / Tour9TutoGoToThePartTest gardés |
| F036 | finition | L3 Structure (après démo) | tenté | L17 d9a532f2 | d44b4466 |
| F037 | finition | L3 Structure + Annuler | tenté | L3 a7c4ab95 | rouge/vert 47ca6a1d |
| F038 | finition | L3 « Aller à la partie » | non reproduit | L3 | Tour9TutoAfterTheDemoTest / Tour9TutoGoToThePartTest gardés |
| F039 | finition | L3 Étape d'écoute | tenté | L17 d9a532f2 | a77d9d13 |
| F040 | finition | L3 Éditeur Acid | tenté | L17 d9a532f2 | 9b33c7de T9L17AcidTapTest |
| F041 | finition | L3 Grille rythme | abandonné (proposé) | L3 | contredit Tour6TutoCellsPanel100Test (choix du tour 6) : retrait proposé |
| F042 | finition | L3 Effets (conseil) | tenté | L17 d9a532f2 | be6f1050 |
| F043 | finition | L3 Ajout de piste | tenté | L3 a7c4ab95 | rouge/vert 32e55352 |
| F044 | finition | L3 Piano roll (copie d'octave) | tenté | L17 d9a532f2 | 7fbb6174 |
| F045 | finition | L3 Message « partie créée » | tenté | L3 a7c4ab95 | rouge/vert 877b7796 |
| F046 | finition | L3 Grille rythme (compteur) | tenté | L17 d9a532f2 | e3383914 |
| F047 | finition | L4 Grille rythme (liste des pads) | tenté | L4 f921e7b2 | rouge/vert, rapport L4 |
| F048 | finition | L4 Grille rythme | tenté | L15 c3ed0541 | rouge bbdd21a4, vert e1f142d3 |
| F049 | finition | L4 Grille rythme (en-tête) | non reproduit | L15 | plus vu après 36c00640 ; Tour9L15GuardTest#f049* |
| F050 | finition | L4 Grille rythme (aide) | tenté | L15 c3ed0541 | rouge bbdd21a4, vert e1f142d3 |
| F051 | finition | L4 Grille rythme (arabe) | tenté | L4 f921e7b2 | rouge/vert, rapport L4 |
| F052 | finition | L4 Grille rythme | tenté | L15 c3ed0541 | vert 3d861e49 |
| F053 | finition | L4 Grille rythme (▶ mesure) | tenté | L4 f921e7b2 | rouge/vert, rapport L4 |
| F054 | finition | L4 Grille rythme | tenté | L15 c3ed0541 | vert 6758f7ce |
| F055 | finition | L4 Grille rythme (Remplir) | tenté | L4 f921e7b2 | rouge/vert, rapport L4 |
| F056 | finition | L4 Grille rythme (effacer) | tenté | L4 f921e7b2 | rouge/vert, rapport L4 |
| F057 | finition | L4 Grille rythme (Swing) | tenté | L4 f921e7b2 + L15 c3ed0541 | vert 3d861e49, da310a7c |
| F058 | finition | L5 Panneau tuto (toutes langues) | tenté | L5a 1f3b5b7a | corrections.tsv (t9-L5a-*) |
| F059 | finition | L5 Grille / piano roll (pointillés) | tenté | L5a 1f3b5b7a | corrections.tsv (t9-L5a-*) |
| F060 | finition | L5 Consigne / grille rythme | tenté | L5a 1f3b5b7a | corrections.tsv (t9-L5a-*) |
| F061 | finition | L5 Panneau tuto (kit) | tenté | L5a 1f3b5b7a | corrections.tsv (t9-L5a-*) |
| F062 | finition | L5 Panneau tuto (texte) | tenté | L5a 1f3b5b7a | corrections.tsv (t9-L5a-*) |
| F063 | finition | L5 Clavier / consigne | tenté | L5a 1f3b5b7a | corrections.tsv (t9-L5a-*) |
| F064 | finition | L5 Pistes (tuto basse) | tenté | L5a 1f3b5b7a | corrections.tsv (t9-L5a-*) |
| F065 | finition | L5 Panneau tuto (vocabulaire) | tenté | L5a 1f3b5b7a | corrections.tsv (t9-L5a-*) |
| F066 | finition | L5 Consigne (ja) | tenté | L5a 1f3b5b7a | corrections.tsv (t9-L5a-*) |
| F067 | finition | L5 Intro ch.2 (ja) | tenté | L5a 1f3b5b7a | corrections.tsv (t9-L5a-*) |
| F068 | finition | L5 Consigne Roll (en, de) | tenté | L5b 571c5abd | 46c0c308 |
| F069 | finition | L5 Grille (roll) | tenté | L5a 1f3b5b7a | corrections.tsv (t9-L5a-*) |
| F070 | finition | L5 Partie de fin | tenté | L5a 1f3b5b7a | corrections.tsv (t9-L5a-*) |
| F071 | finition | L5 Partie Couplet (nl) | tenté | L5a 1f3b5b7a | corrections.tsv (t9-L5a-*) |
| F072 | finition | L5 Piano roll (ko) | tenté | L5a 1f3b5b7a | corrections.tsv (t9-L5a-*) |
| F073 | finition | L5 Intro (hi) | tenté | L5b 571c5abd | 511539be |
| F074 | finition | L5 Noms de parties (ko) | tenté | L5a 1f3b5b7a | corrections.tsv (t9-L5a-*) |
| F075 | finition | L5 Consigne Mix (ar) | tenté | L5b 571c5abd | aabe6ff3 |
| F076 | finition | L5 Chapitre Refrain (de) | tenté | L5b 571c5abd | f9a19436 |
| F077 | finition | L5 Feuille Tonalité (it, hi, pl) | tenté | L5b 571c5abd | 13ff280b |
| F078 | finition | L5 Panneau (es, Faltan) | tenté | L16 c3d27d66 | 001a8c94 Tour9L16TutoOrderNamesTest |
| F079 | finition | L5 Glossaire (ar) | tenté | L5a 1f3b5b7a | corrections.tsv (t9-L5a-*) |
| F080 | finition | L5 Grille (pt-BR) | tenté | L5a 1f3b5b7a | corrections.tsv (t9-L5a-*) |
| F081 | finition | L5 Grille (ko) | tenté | L16 c3d27d66 | e5ae6b3d |
| F082 | finition | L5 Panneau (ja) | tenté | L5b 571c5abd | 6843ee0e |
| F083 | finition | L5 Écoute des parties (en) | tenté | L5a 1f3b5b7a | corrections.tsv (t9-L5a-*) |
| F084 | finition | L5 Boutons stop (es) | tenté | L16 c3d27d66 | e5ae6b3d (es : reste sur eq/tour-9-textes, pas entré) |
| F085 | finition | L5 Consigne (de) | tenté | L5b 571c5abd | 5deda19a |
| F086 | finition | L5 Pourquoi ? (nl) | tenté | L16 c3d27d66 | 9e8b0ff4 Tour9L16TutoOffsetWhyTest |
| F087 | finition | L5 Fin de chapitre (tr) | tenté | L16 c3d27d66 | e5ae6b3d |
| F088 | finition | L5 Noms (ar) | tenté | L5b 571c5abd | 403f4117 |
| F089 | finition | L5 Message (fr) | tenté | L16 c3d27d66 | a2ccda05 |
| F090 | finition | L5 Grille (ja, nl) | tenté | L5b 571c5abd | cfa4e6a3 |
| F091 | finition | L5 Swing (ja) | tenté | L5a 1f3b5b7a | corrections.tsv (t9-L5a-*) |
| F092 | finition | L5 Swing (pliable) | tenté | L5a 1f3b5b7a | corrections.tsv (t9-L5a-*) |
| F093 | finition | L5 Consigne (zh) | tenté | L16 c3d27d66 | bfce7c01 |
| F094 | finition | L5 Compteur (zh) | tenté | L5b 571c5abd | 72465f24 |
| F095 | finition | L5 Validation (en) | tenté | L5a 1f3b5b7a | corrections.tsv (t9-L5a-*) |
| F096 | finition | L5 Mix (nl) | tenté | L5b 571c5abd | b8dd3696 |
| F097 | finition | L5 Carte des parties (tr) | tenté | L5b 571c5abd | ffcfbd41 |
| F098 | finition | L5 Mix (tr) | tenté | L5b 571c5abd | 4cec0d5f |
| F099 | finition | L5 Bouton (tr) | tenté | L16 c3d27d66 | a2ccda05 |
| F100 | finition | L6 Structure (Ajouter) | tenté | L6 6120a5b2 | rouge/vert, rapport L6 |
| F101 | finition | L6 Structure | non reproduit | L6, L12 | les glissés partaient du panneau tuto ; reste prévu dans la fiche tuto |
| F102 | finition | L6 Structure | tenté | L6 6120a5b2 | rouge/vert, rapport L6 |
| F103 | finition | L6 Structure | tenté | L6 6120a5b2 | rouge/vert, rapport L6 |
| F104 | finition | L6 Structure (rangée de partie) | tenté | L6 6120a5b2 | rouge/vert, rapport L6 |
| F105 | finition | L6 Barre du haut (indicateur de partie) | tenté | L6 + 14357d61 | rouge e450751d, vert 14357d61 |
| F106 | finition | L6 Structure | tenté | L6 + cac10bde | rouge dd1b2b89, vert cac10bde |
| F107 | finition | L6 Barre du haut | tenté | L6 + a7672e44 | moitié faite (rapport L6) |
| F108 | finition | L6 Structure (ar) | tenté | L6 6120a5b2 | rouge/vert, rapport L6 |
| F109 | finition | L6 Structure (blocs) | tenté | L6 6120a5b2 | rouge/vert, rapport L6 |
| F110 | finition | L6 Structure (rangées) | tenté | L6 6120a5b2 | rouge/vert, rapport L6 |
| F111 | finition | L6 Structure (message) | tenté | L12 d1e635d8 | DoneBarNextTouchTest, Tour9DoneBarGoesTest |
| F112 | finition | L7 Piano roll | tenté | L7 32350f94 | rouge/vert, rapport L7 |
| F113 | finition | L7 Piano roll (accords) | tenté | L7 32350f94 | rouge/vert, rapport L7 |
| F114 | finition | L7 Piano roll | tenté | L7 32350f94 | rouge/vert, rapport L7 |
| F115 | finition | L7 Piano roll (clavier) | tenté | L7 32350f94 | rouge 03772d18 |
| F116 | finition | L7 Onglet de piste | tenté | L7 32350f94 | rouge/vert, rapport L7 |
| F117 | finition | L7 Clavier | tenté | L7 32350f94 | rouge/vert, rapport L7 |
| F118 | finition | L7 Piano roll (coller) | tenté | L7 32350f94 | rouge/vert, rapport L7 |
| F119 | finition | L8 Feuille Tempo | tenté | L8 92587924 | TempoTypingStaysTest |
| F120 | finition | L8 Feuille de son | tenté | L8 92587924 | SheetCloseSeparateTest |
| F121 | finition | L8 Partager / Mix (volume) | tenté | L8 92587924 | Tour9L8ShareWordsTest |
| F122 | finition | L8 Partager | tenté | L8 92587924 | Lot6UiShareLevelTest |
| F123 | finition | L8 Partager | tenté | L8 92587924 | ShareNameFieldSeenTest |
| F124 | finition | L8 Partager | tenté | L8 92587924 | ShareCancelReadableTest |
| F125 | finition | L8 Partager | tenté | L8 92587924 | ShareCheckOkMeaningTest |
| F126 | finition | L8 Partager | tenté | L8 92587924 | ShareCheckOkMeaningTest |
| F127 | finition | L2 Automation (dessin) | tenté | L2 b7848dda | Tour9CurveDrawTest#f127 |
| F128 | finition | L2 Automation | tenté | L2 b7848dda | Tour9AutomationFinishTest (fd71211f) |
| F129 | finition | L2 Automation | tenté | L2 b7848dda | Tour9AutomationFinishTest (fd71211f) |
| F130 | finition | L2 Automation (option) | tenté | L2 b7848dda | Tour9AutomationFinishTest (fd71211f) |
| F131 | finition | L10 Retour Android | tenté | L11 86d71a17 | T9F131BackUnfoldsTutoTest (8b8bada5) |
| F132 | finition | L8 Liste des sons | tenté | L15 c3ed0541 | vert a12c97d0 |
| F133 | finition | L8 Nouvelle piste | tenté | L8 92587924 | TrackNameNoRepeatTest |
| F134 | finition | L8 Barre des pistes | tenté | L8 92587924 | TrackAddWholeTest |
| F135 | finition | L8 Barre « Mode retours » | tenté | L8 92587924 | RetoursStripReadableTest |
| F136 | finition | L2 Effets rapides | tenté | L2 + 52c58e73 | Tour9EffectsCardNameTest (côté Mix) ; côté hors Mix : rouge seul sur eq/tour-9-L18, pas entré |
| F137 | finition | L5 Tuto techno, Intro 2 | tenté | L5a 1f3b5b7a | corrections.tsv (t9-L5a-*) |
| F138 | finition | L3 Tuto, étapes de courbe | tenté | L3 a7c4ab95 | Tour9TutoCurveKeepsMineTest (094c2f1a) |
| F139 | finition | L10 Découpe | tenté | L10 d12e3c01 | CutScreen, rapport L10 |
| F140 | finition | L4 Grille rythme, anneau du tuto | tenté | L4 f921e7b2 | rouge/vert, rapport L4 |
| F141 | finition | L2 Mix | tenté | L2 b7848dda | Tour9MixFinishTest |
| F142 | finition | L10 Synthé, grandes lettres | tenté | L7 32350f94 | rouge/vert, rapport L7 |

## 2. Sièges de contrôle

Règle de groupement (le moins de sièges possible) :
- un défaut de **contenu de cours** (lots L3 et L5 : texte, validation, démo d'une étape précise) se rejoue sur **son** siège d'origine (même tuto, même tranche, même langue, appareil, police et mode) ;
- un défaut **d'écran** (panneau tuto, grille, Mix, Structure, piano roll, partage…) se rejoue sur tout siège qui a **le même profil** (langue, appareil, police, mode) qu'un de ses sièges d'origine ; le siège y va après sa tranche ou quand l'écran s'ouvre en chemin ;
- choix glouton (le siège qui couvre le plus de défauts restants d'abord). Résultat : **46 sièges** pour les 137 défauts tentés observables, plus 1 siège ajouté (C47) pour le côté Facile de F137.

Chaque siège reprend le `start.txt` de son siège d'origine (`docs/audits/ultra/sieges/tour-9/controle/<siège>/a/start.txt` : langue, appareil, police, mode, amenée, profil, niveau), sous `docs/audits/ultra/sieges/tour-10/controle/<C..>/a/`. Mission : celle du tour 9 (`tuto-facile` ou `tuto-pro`), avec en plus la liste de ses phrases d'acceptation à constater, une par une (« vu : oui / non / pas atteint »). Oracle : la fin de tranche du tour 9 (colonne « oracle »), qui lit une étape ou un compteur précis. Ce tour déroge à la règle « une mission par niveau » de `PLAN-NIVEAUX.md` : c'est un tour de contrôle ciblé ; les cinq niveaux y sont quand même (voir le compte plus bas).

| siège | reprend | tuto, mode, chapitres | langue | appareil | police | profil (niveau) | oracle | défauts à constater |
|---|---|---|---|---|---|---|---|---|
| C01 | t9-01 | acid_house, Facile, 1-3 (9) | ar | phone360 | 130 | curieux (0) | `step=groove_name` | F040 F051 F120 |
| C02 | t9-32 | hard_techno, Facile, 8-13 (13) | ar | phone | 200 | curieux (0) | Bravo (done=39) | F088 |
| C03 | t9-41 | jungle, Facile, 1-8 (15) | en | phone360 | 200 | curieux (0) | `step=brick` | F052 |
| C04 | t9-42 | jungle, Facile, 9-15 (15) | en | foldable | 100 | curieux (0) | Bravo (done=42) | F037 F068 F083 |
| C05 | t9-53 | phonk, Facile, 5-13 (13) | it | phone360 | 200 | curieux (0) | Bravo (done=32) | F135 |
| C06 | t9-62 | speed_garage, Facile, 1-6 (13) | ar | foldable | 200 | curieux (0) | `step=map_intro` | D06 F013 F055 F063 F065 F108 F115 |
| C07 | t9-63 | speed_garage, Facile, 7-13 (13) | ar | tablet8 | 100 | curieux (0) | Bravo (done=36) | F030 F075 F079 |
| C08 | t9-74 | trap, Facile, 8-15 (15) | en | foldable | 200 | curieux (0) | Bravo (done=34) | F095 F123 |
| C09 | t9-05 | acidcore, Facile, 1-5 (11) | ko | phone360 | 200 | débutant motivé (1) | `step=make_intro` | F017 F117 |
| C10 | t9-06 | acidcore, Facile, 6-11 (11) | ko | foldable | 100 | débutant motivé (1) | Bravo (done=31) | F081 |
| C11 | t9-15 | deep_house, Facile, 1-6 (13) | es | tablet8 | 100 | débutant motivé (1) | `step=map_intro` | F018 F059 F113 F114 |
| C12 | t9-26 | dubstep, Facile, 9-15 (15) | tr | foldable | 200 | débutant motivé (1) | Bravo (done=43) | F016 F047 F050 F058 F087 F107 F110 F122 |
| C13 | t9-35 | industrial_techno, Facile, 1-6 (13) | ko | tablet8 | 200 | débutant motivé (1) | `step=brick` | F064 F072 F134 |
| C14 | t9-46 | lofi, Facile, 1-5 (13) | es | foldable | 130 | débutant motivé (1) | `step=groove_hear` | F092 |
| C15 | t9-47 | lofi, Facile, 6-13 (13) | es | tablet8 | 200 | débutant motivé (1) | Bravo (done=32) | F026 F078 F084 |
| C16 | t9-56 | reggaeton, Facile, 1-7 (15) | tr | phone | 200 | débutant motivé (1) | `step=groove_hear` | S56 F053 |
| C17 | t9-57 | reggaeton, Facile, 8-15 (15) | tr | phone360 | 100 | débutant motivé (1) | Bravo (done=40) | F054 F060 F097 F098 F099 F100 F126 F140 |
| C18 | t9-67 | tech_house, Facile, 6-13 (13) | ko | tablet8 | 130 | débutant motivé (1) | Bravo (done=38) | F056 F074 |
| C19 | t9-20 | disco, Facile, 5-13 (13) | de | phone | 200 | moyen (2) | Bravo (done=36) | F014 F076 F112 |
| C20 | t9-29 | grime, Facile, 1-5 (13) | hi | phone360 | 200 | moyen (2) | `step=groove_hear` | F077 |
| C21 | t9-30 | grime, Facile, 6-13 (13) | hi | foldable | 100 | moyen (2) | Bravo (done=33) | S30 F073 |
| C22 | t9-39 | jump_up, Facile, 1-8 (15) | fr | tablet8 | 100 | moyen (2) | `step=brick` | F132 |
| C23 | t9-40 | jump_up, Facile, 9-15 (15) | fr | phone | 130 | moyen (2) | Bravo (done=45) | F027 F089 |
| C24 | t9-51 | neurofunk, Facile, 9-15 (15) | de | tablet8 | 100 | moyen (2) | Bravo (done=41) | F085 |
| C25 | t9-72 | trance, Facile, 7-13 (13) | fr | phone | 100 | moyen (2) | Bravo (done=40) | F036 |
| C26 | t9-03 | acid_techno, Pro, 1-5 (12) | ja | tablet8 | 100 | confirmé (3) | `step=make_intro` | F066 F067 |
| C27 | t9-23 | drill, Pro, 1-8 (16) | pl | tablet8 | 200 | confirmé (3) | `step=brick` | F057 F133 |
| C28 | t9-33 | house, Pro, 1-9 (17) | ja | phone360 | 100 | confirmé (3) | `step=map_intro` | D11 F021 F033 F091 |
| C29 | t9-34 | house, Pro, 10-17 (17) | ja | foldable | 130 | confirmé (3) | Bravo (done=45) | F032 F042 F082 F090 |
| C30 | t9-43 | liquid_dnb, Pro, 1-6 (17) | nl | tablet8 | 130 | confirmé (3) | `step=pad_do` | F043 |
| C31 | t9-44 | liquid_dnb, Pro, 7-11 (17) | nl | phone | 200 | confirmé (3) | `step=song_order` | F136 |
| C32 | t9-45 | liquid_dnb, Pro, 12-17 (17) | nl | phone360 | 100 | confirmé (3) | Bravo (done=52) | F096 |
| C33 | t9-55 | pop, Pro, 9-16 (16) | pl | tablet8 | 130 | confirmé (3) | Bravo (done=45) | F125 F131 |
| C34 | t9-64 | synthwave, Pro, 1-8 (16) | ja | phone | 130 | confirmé (3) | `step=brick` | F029 F046 F119 |
| C35 | t9-75 | uk_garage, Pro, 1-4 (14) | nl | tablet8 | 100 | confirmé (3) | `step=lead_do` | D04 D17 F044 F086 |
| C36 | t9-76 | uk_garage, Pro, 5-14 (14) | nl | phone | 130 | confirmé (3) | Bravo (done=39) | S76 F071 |
| C37 | t9-17 | detroit, Pro, 1-7 (14) | pt-BR | phone360 | 200 | pro (4) | `step=make_intro` | S17 |
| C38 | t9-18 | detroit, Pro, 8-14 (14) | pt-BR | foldable | 100 | pro (4) | Bravo (done=41) | S34 F019 F062 F080 F129 |
| C39 | t9-27 | gabber, Pro, 1-7 (14) | id | tablet8 | 100 | pro (4) | `step=make_intro` | F039 F061 F102 F106 F118 |
| C40 | t9-28 | gabber, Pro, 8-14 (14) | id | phone | 130 | pro (4) | Bravo (done=40) | F103 |
| C41 | t9-38 | jersey_club, Pro, 8-15 (15) | zh-CN | foldable | 200 | pro (4) | Bravo (done=33) | F025 F034 F045 F104 |
| C42 | t9-49 | minimal, Pro, 8-14 (14) | pt-BR | phone360 | 130 | pro (4) | Bravo (done=41) | D01 D08 D12 D13 F012 F023 F024 F105 F111 F128 F139 |
| C43 | t9-58 | retro_techno, Pro, 1-6 (13) | id | foldable | 130 | pro (4) | `step=make_intro` | F069 F116 |
| C44 | t9-59 | retro_techno, Pro, 7-13 (13) | id | tablet8 | 200 | pro (4) | Bravo (done=41) | F070 F121 |
| C45 | t9-68 | techno, Pro, 1-5 (16) | zh-CN | phone | 200 | pro (4) | `step=pad_do` | D02 D10 D14 F048 F093 F094 F109 F137 F142 |
| C46 | t9-70 | techno, Pro, 11-16 (16) | zh-CN | foldable | 130 | pro (4) | Bravo (done=52) | F028 F124 F127 F130 F138 |
| C47 | neuf | techno, Facile, chapitre de l'Intro 2 | fr | phone | 130 | débutant motivé (1) | `step=` de l'étape Intro 2 | F137 (côté Facile ; C45 voit le côté Pro) |

Sièges par niveau : curieux 8, débutant motivé 11, moyen 7, confirmé 11, pro 10. Les cinq niveaux ont au moins un siège.

Vagues proposées : 6 vagues de 8 sièges (la dernière de 7), langues dures réparties ; les sièges d'un même tuto (C.. en tranches a et b) dans la même vague. Avant la première vague : `scripts/bench/selftest.sh` vert.

### Phrases d'acceptation par siège

**C01** (reprend t9-01 : acid_house Facile ch. 1-3 (9), ar, phone360, police 130, curieux)

- F040 : « Toucher une case de l'éditeur Acid y pose ou change bien la note. »
- F051 : « En arabe, la consigne dit que la grille se lit de gauche à droite (ou la grille suit le sens de lecture). »
- F120 : « Fermer » est séparé du texte et facile à toucher. »

**C02** (reprend t9-32 : hard_techno Facile ch. 8-13 (13), ar, phone, police 200, curieux)

- F088 : « Les noms de sons en arabe sont les bons mots, sans contresens. »

**C03** (reprend t9-41 : jungle Facile ch. 1-8 (15), en, phone360, police 200, curieux)

- F052 : « Le repère vers une autre mesure dit s'il faut y ajouter ou y enlever une case. »

**C04** (reprend t9-42 : jungle Facile ch. 9-15 (15), en, foldable, police 100, curieux)

- F037 : « Si j'annule la création d'une partie, l'étape redevient à faire. »
- F068 : « La consigne du roll est une phrase complète et utilise le même mot que le bouton. »
- F083 : « Le tuto fait écouter les parties dans l'ordre de la carte. »

**C05** (reprend t9-53 : phonk Facile ch. 5-13 (13), it, phone360, police 200, curieux)

- F135 : « Les onglets du bas restent lisibles en entier, barre de retours comprise. »

**C06** (reprend t9-62 : speed_garage Facile ch. 1-6 (13), ar, foldable, police 200, curieux)

- D06 : « En Simple à 200 %, les notes pointillées restent visibles et le compteur « encore N notes » baisse à chaque note touchée. »
- F013 : « Arrêter une démo ne déclenche aucun message « étape pas encore faite ». »
- F055 : « Toucher « Remplir » pose des coups que je vois et entends, ou dit pourquoi il ne peut pas. »
- F063 : « Le nom de note dans la consigne s'écrit comme sur les touches. »
- F065 : « Chaque mot technique est expliqué la première fois qu'il apparaît. »
- F108 : « Le bouton d'ajout se lit en entier, même en arabe. »
- F115 : « Les noms de touches et de mesures se lisent sans se chevaucher, même à 200 %. »

**C07** (reprend t9-63 : speed_garage Facile ch. 7-13 (13), ar, tablet8, police 100, curieux)

- F030 : « En arabe, les valeurs négatives s'écrivent avec le signe moins du bon côté, au Mix comme au Partage. »
- F075 : « La consigne nomme l'onglet Mix comme il est écrit en bas. »
- F079 : « Les mots expliqués sous la consigne sont ceux du chapitre, avec le même sens. »

**C08** (reprend t9-74 : trap Facile ch. 8-15 (15), en, foldable, police 200, curieux)

- F095 : « La consigne et la valeur acceptée sont les mêmes. »
- F123 : « Le champ du nom et les boutons M4A / WAV sont séparés et lisibles. »

**C09** (reprend t9-05 : acidcore Facile ch. 1-5 (11), ko, phone360, police 200, débutant motivé)

- F017 : « Pendant le téléchargement, je vois la progression ; le bouton ne se relance pas et « Plus tard » disparaît. »
- F117 : « Le clavier tient à l'écran à 200 % et son « ? » explique ce qu'il fait. »

**C10** (reprend t9-06 : acidcore Facile ch. 6-11 (11), ko, foldable, police 100, débutant motivé)

- F081 : « Le message dit simplement combien de coups enlever, sans laisser croire à une faute. »

**C11** (reprend t9-15 : deep_house Facile ch. 1-6 (13), es, tablet8, police 100, débutant motivé)

- F018 : « Dans la feuille Tonalité, rien n'est choisi d'avance, la puce choisie est marquée et la feuille ne bouge pas sous mon doigt. »
- F059 : « Les pointillés sont exactement les cases ou notes que la consigne décrit. »
- F113 : « La note mise en évidence est celle que la consigne demande de toucher. »
- F114 : « Une piste mélodique s'ouvre sans basse fantôme sélectionnée. »

**C12** (reprend t9-26 : dubstep Facile ch. 9-15 (15), tr, foldable, police 200, débutant motivé)

- F016 : « Le bandeau du kit ne revient pas après « Plus tard », ne couvre jamais la carte de l'étape et sa taille tient sur une ligne. »
- F047 : « Le nom de chaque pad se lit en entier, les pastilles ne passent jamais dessus. »
- F050 : « La phrase d'aide de la grille se lit en entier, même à 200 %. »
- F058 : « Une consigne qui cite un bouton n'a jamais deux guillemets collés. »
- F087 : « Chapitre et partie ont deux mots différents. »
- F107 : « Le nom du projet et « Enregistré » tiennent dans la barre. »
- F110 : « Les rangées du morceau gardent « N mesures » sur une seule ligne. »
- F122 : « Je sais avant d'exporter que le nom tapé renomme aussi le projet, et je peux l'éviter. »

**C13** (reprend t9-35 : industrial_techno Facile ch. 1-6 (13), ko, tablet8, police 200, débutant motivé)

- F064 : « Après « Créer la piste Basse », le tuto dit laquelle des basses il vise (ou il n'y en a qu'une). »
- F072 : « La consigne dit tout ce qu'il faut faire pour valider l'étape. »
- F134 : « + Piste » reste lisible, quel que soit le nombre de pistes. »

**C14** (reprend t9-46 : lofi Facile ch. 1-5 (13), es, foldable, police 130, débutant motivé)

- F092 : « La consigne dit où est le Swing sans se tromper de place. »

**C15** (reprend t9-47 : lofi Facile ch. 6-13 (13), es, tablet8, police 200, débutant motivé)

- F026 : « Toutes les étiquettes du Mix simple ont la même taille de texte. »
- F078 : « La liste « il manque » ne nomme que des parties qui existent. »
- F084 : « Le même geste a le même mot partout. »

**C16** (reprend t9-56 : reggaeton Facile ch. 1-7 (15), tr, phone, police 200, débutant motivé)

- S56 : « Tümünü doldur » remplit seulement ce que le tuto demande. »
- F053 : « Toucher ▶ change seulement de mesure ; aucun coup ne bouge. »

**C17** (reprend t9-57 : reggaeton Facile ch. 8-15 (15), tr, phone360, police 100, débutant motivé)

- F054 : « Le repère vers le pad demandé m'y emmène d'un toucher. »
- F060 : « Le nom du pad dans la consigne est mot pour mot celui écrit sur le pad. »
- F097 : « La partie s'appelle « Nakarat sonrası ». »
- F098 : « La phrase turque se lit correctement (« altındaki »). »
- F099 : « Le bouton dit « Tamam ». »
- F100 : « Le titre de la liste d'ajout ne change pas après un ajout. »
- F126 : « Chaque image de l'onglet Partager a un sens visible (texte ou bouton). »
- F140 : « L'anneau du tuto montre toujours une case que je peux toucher, roulements compris. »

**C18** (reprend t9-67 : tech_house Facile ch. 6-13 (13), ko, tablet8, police 130, débutant motivé)

- F056 : « Toucher une case pleine l'efface du premier coup (le réglage de force passe par l'appui long). »
- F074 : « Les noms de parties à l'écran sont ceux de la consigne, dans ma langue. »

**C19** (reprend t9-20 : disco Facile ch. 5-13 (13), de, phone, police 200, moyen)

- F014 : « Les boutons côte à côte ont la même taille de texte ; le nom du projet tient sur une ligne. »
- F076 : « Le chapitre et la partie utilisent le même mot. »
- F112 : « L'en-tête du piano roll reste visible quoi que je fasse glisser. »

**C20** (reprend t9-29 : grime Facile ch. 1-5 (13), hi, phone360, police 200, moyen)

- F077 : « La consigne nomme la feuille Tonalité exactement comme son titre. »

**C21** (reprend t9-30 : grime Facile ch. 6-13 (13), hi, foldable, police 100, moyen)

- S30 : « Le fader du Mix simple répond dès le premier glissé, sans zone morte. »
- F073 : « La phrase dit clairement quels instruments l'intro garde. »

**C22** (reprend t9-39 : jump_up Facile ch. 1-8 (15), fr, tablet8, police 100, moyen)

- F132 : « Les noms de sons d'un même kit suivent la même forme. »

**C23** (reprend t9-40 : jump_up Facile ch. 9-15 (15), fr, phone, police 130, moyen)

- F027 : « Toutes les tranches du Mix tiennent dans l'écran, alignées à la même hauteur, même à 130 %. »
- F089 : « Le message ne répète pas « C'est prêt ». »

**C24** (reprend t9-51 : neurofunk Facile ch. 9-15 (15), de, tablet8, police 100, moyen)

- F085 : « La consigne allemande dit clairement quelles cases effacer. »

**C25** (reprend t9-72 : trance Facile ch. 7-13 (13), fr, phone, police 100, moyen)

- F036 : « Après une démo, « Créer la partie « X » » crée du premier coup la partie nommée X et l'étape avance. »

**C26** (reprend t9-03 : acid_techno Pro ch. 1-5 (12), ja, tablet8, police 100, confirmé)

- F066 : « La consigne nomme l'onglet comme il est écrit. »
- F067 : « L'intro du chapitre décrit le kit et la ligne 303 tels qu'ils sont à l'écran. »

**C27** (reprend t9-23 : drill Pro ch. 1-8 (16), pl, tablet8, police 200, confirmé)

- F057 : « Le curseur Swing montre la valeur visée et permet de l'atteindre à 1 % près. »
- F133 : « Une piste neuve n'a jamais un mot répété dans son nom. »

**C28** (reprend t9-33 : house Pro ch. 1-9 (17), ja, phone360, police 100, confirmé)

- D11 : « Toucher le nom d'une piste dans le Mix ouvre ses effets et courbes (ou le texte d'aide ne le promet plus). »
- F021 : « Quand une feuille couvre la carte du tuto, la validation de l'étape se voit quand même (bandeau ou carte au premier plan). »
- F033 : « Je le fais » m'emmène au bon endroit mais me laisse faire le geste ; rien n'est coché avant que je le fasse. »
- F091 : « Les valeurs de swing du texte et du réglage demandé sont les mêmes. »

**C29** (reprend t9-34 : house Pro ch. 10-17 (17), ja, foldable, police 130, confirmé)

- F032 : « L'étape ne se valide qu'une fois tout ce que dit la consigne fait, sur le bon pad. »
- F042 : « Le conseil d'effet nomme la piste et le réglage à toucher. »
- F082 : « La phrase dit sans ambiguïté ce qui joue. »
- F090 : « La consigne nomme les cases à effacer comme l'écran les montre (barrées). »

**C30** (reprend t9-43 : liquid_dnb Pro ch. 1-6 (17), nl, tablet8, police 130, confirmé)

- F043 : « Après la création d'une piste par le tuto, l'écran ouvre cette piste. »

**C31** (reprend t9-44 : liquid_dnb Pro ch. 7-11 (17), nl, phone, police 200, confirmé)

- F136 : « Les effets rapides proposés nomment la piste ouverte. »

**C32** (reprend t9-45 : liquid_dnb Pro ch. 12-17 (17), nl, phone360, police 100, confirmé)

- F096 : « Le texte néerlandais écrit « overstuurt ». »

**C33** (reprend t9-55 : pop Pro ch. 9-16 (16), pl, tablet8, police 130, confirmé)

- F125 : « Le nombre de parties dans Partager est celui du morceau. »
- F131 : « Retour ferme d'abord la feuille ou l'éditeur ouvert ; il ne sort du projet qu'en dernier. »

**C34** (reprend t9-64 : synthwave Pro ch. 1-8 (16), ja, phone, police 130, confirmé)

- F029 : « Après avoir ajouté un effet, la liste me montre l'effet ajouté. »
- F046 : « Le compteur de cases compte dès le départ toutes les cases de l'étape, toutes mesures comprises. »
- F119 : « La feuille Tempo reste en place pendant la saisie et marque la valeur choisie. »

**C35** (reprend t9-75 : uk_garage Pro ch. 1-4 (14), nl, tablet8, police 100, confirmé)

- D04 : « Un appui long sur un coup ouvre une feuille avec « Décalage du coup » ; le régler valide snare_late / uk_garage ch2. »
- D17 : « Toucher « Télécharger le kit UK Garage » charge le kit ou affiche une erreur claire. »
- F044 : « Quand le chapitre dit « Fini », il ne reste aucune note en pointillé. »
- F086 : « L'explication parle du réglage de l'étape. »

**C36** (reprend t9-76 : uk_garage Pro ch. 5-14 (14), nl, phone, police 130, confirmé)

- S76 : « Je peux vider une mesure en un geste. »
- F071 : « La partie créée garde les instruments que le texte dit qu'elle garde. »

**C37** (reprend t9-17 : detroit Pro ch. 1-7 (14), pt-BR, phone360, police 200, pro)

- S17 : « Une note longue sur une cible large valide ; « Eu faço » ne reboucle pas sur les notes déjà posées. »

**C38** (reprend t9-18 : detroit Pro ch. 8-14 (14), pt-BR, foldable, police 100, pro)

- S34 : « Le Mix montre qu'on peut défiler à l'horizontale. »
- F019 : « Quand le tuto est fini, je vois seulement « Tuto fini », « Recommencer » et « Chapitres », sans compteur d'étape ni « Reprendre ». »
- F062 : « Ce que dit le texte (instruments, longueurs, ce qui joue) est ce que l'écran montre. »
- F080 : « La consigne ne parle d'une mesure que si l'écran permet de la choisir. »
- F129 : « La durée et l'axe de temps disent la même chose. »

**C39** (reprend t9-27 : gabber Pro ch. 1-7 (14), id, tablet8, police 100, pro)

- F039 : « Une étape « écoute le kick » se valide quand j'écoute le kick. »
- F061 : « Le texte sur le kit correspond au kit vraiment chargé, et un kit déjà chargé n'est plus proposé. »
- F102 : « Un appui long sur une partie ouvre son menu, sans l'ajouter. »
- F106 : « La phrase d'aide de la Structure se lit sous la barre du haut. »
- F118 : « Le bouton Coller est traduit correctement dans ma langue. »

**C40** (reprend t9-28 : gabber Pro ch. 8-14 (14), id, phone, police 130, pro)

- F103 : « Le bouton dit ce qu'il fait : copie ou partie vide. »

**C41** (reprend t9-38 : jersey_club Pro ch. 8-15 (15), zh-CN, foldable, police 200, pro)

- F025 : « La feuille ouverte depuis une tranche porte le nom de la piste en titre, lisible en entier. »
- F034 : « Montre-moi » montre exactement le geste demandé, sur le bon bouton. »
- F045 : « Le message « partie créée » est écrit entièrement dans ma langue, avec sa ponctuation. »
- F104 : « Toucher une partie ouvre ses réglages (longueur) ; la lecture a son propre bouton. »

**C42** (reprend t9-49 : minimal Pro ch. 8-14 (14), pt-BR, phone360, police 130, pro)

- D01 : « Pendant le tuto, je glisse le fader d'une piste du Mix et son volume change ; les étapes levels/meters/buses se valident. »
- D08 : « Après un « + partie », la liste « Ajouter à la fin » ne saute pas : le bouton suivant reste sous mon doigt. »
- D12 : « L'envoi reverb/écho du pad demandé (Caixa, Rim, Clap) se trouve dans le Mix là où le tuto le dit. »
- D13 : « Le « Volume cible » (Auto/Club) demandé est visible à l'endroit indiqué par le tuto, pas sous le panneau ni seulement dans Partager. »
- F012 : « Le message « Bravo » se lit en entier, au premier plan. »
- F023 : « Dans le Mix, chaque tranche garde son nom lisible en entier, même après avoir fait défiler. »
- F024 : « Faire défiler le Mix ne change aucun réglage ; seul un geste qui part du curseur le bouge. »
- F105 : « L'indicateur du haut nomme la partie que j'édite, avec ses mesures lisibles en entier. »
- F111 : « Le message « nouvelle longueur » disparaît après quelques secondes ou au geste suivant. »
- F128 : « Les valeurs de l'axe se lisent en entier. »
- F139 : « L'écran de découpe ne défile que vers le message du moment. »

**C43** (reprend t9-58 : retro_techno Pro ch. 1-6 (13), id, foldable, police 130, pro)

- F069 : « Les cases guides du roll sont dans la mesure que dit le texte. »
- F116 : « L'onglet de piste garde le nom de la piste et du son choisi. »

**C44** (reprend t9-59 : retro_techno Pro ch. 7-13 (13), id, tablet8, police 200, pro)

- F070 : « La partie de fin contient ce que le texte annonce. »
- F121 : « Chaque valeur de volume dit en mots simples ce qu'elle veut dire. »

**C45** (reprend t9-68 : techno Pro ch. 1-5 (16), zh-CN, phone, police 200, pro)

- D02 : « En grande police, la case, la note ou le réglage demandé par l'étape est visible au-dessus du panneau sans rien replier. »
- D10 : « Depuis le menu « A ▾ » je peux renommer la partie, comme le tuto le demande. »
- D14 : « Lire la suite » déplie toute la consigne de l'étape. »
- F048 : « Je vois toujours quel pad j'édite, même quand un message s'affiche. »
- F093 : « La consigne dit « appui long » quand il faut un appui long. »
- F094 : « Le compteur s'accorde avec le nombre de cases. »
- F109 : « Chaque bloc du morceau montre au moins la lettre ou le nom de sa partie. »
- F137 : « La recette de l'Intro 2 est la même en mode simple et Pro, et suit la vérification part:Intro 2:less+bass. »
- F142 : « Après un enregistrement, l'espacement du synthé revient à la normale. »

**C46** (reprend t9-70 : techno Pro ch. 11-16 (16), zh-CN, foldable, police 130, pro)

- F028 : « Un envoi à 0 % montre quand même sa piste de glisse, et je peux le tirer depuis le début. »
- F124 : « Annuler » se lit bien sur le fond. »
- F127 : « Dessiner une courbe d'automation ne fait jamais défiler la page. »
- F130 : « Les options d'automation nomment la piste éditée. »
- F138 : « Je le fais » garde mes courbes et l'étape se coche même quand toutes les pistes de courbe sont prises. »

**C47** (neuf : techno Facile, fr, phone, police 130, débutant motivé)

- F137 : « La recette de l'Intro 2 est la même en mode simple et Pro, et suit la vérification part:Intro 2:less+bass. »

### Si L13 entre avant le contrôle

Ses 10 finitions tentées se rangent sur des sièges déjà prévus, par profil : F001 → C34, C40, C41 ; F002 → C15 ; F003 → C03, C16, C20 ; F004 → C10 ; F005 → C08, C22, C23 ; F006 → C04, C07, C09 ; F007 → C20, C21, C40 ; F009 → C01, C31 ; F011 → C05, C06, C08, C09, C17, C24, C25, C29, C33, C45. **F010** (siège d'origine t9-24 seulement) demande un siège de plus : reprendre t9-24.

## 3. Ce qu'aucun siège ne peut constater

| id | état | pourquoi aucun siège ne le voit | comment le fermer |
|---|---|---|---|
| F141 | tenté | « Le Mix ne crée rien de neuf à chaque image » : c'est du travail interne (allocations à chaque image), invisible à l'écran pour un testeur. | Le test `Tour9MixFinishTest` (L2) fait foi ; « vérifié » par la CI complète de fin, pas par un siège. |
| D01, D11 (partie banc) | tenté | La moitié « c'est le banc » (le doigt du banc tombait sur le capot du fader) ne se voit pas dans l'appli. | Déjà prouvée par `BenchMixFaderCoverTest` et `selftest.sh`. Le côté appli (le fader suit le doigt) est, lui, sur C42 (D01) et C28 (D11). |
| D17 (partie banc) | tenté | Le FLAC du kit lu par le banc (`BenchFlac`, L9) est un outil de mesure, pas un écran. | Prouvé par le rejeu `t9-75-kit` ; le message du kit dans l'appli est sur C35. |
| F001…F007, F009…F011 | tenté, pas entré | Le code est sur `eq/tour-9-L13`, pas dans l'intégration : un siège qui joue l'intégration verrait l'ancien défaut. | Faire entrer L13, puis les ranger comme dit en §2 (« Si L13 entre »). |
| F084 (espagnol) | tenté en partie | Les textes espagnols « stop » attendent sur `eq/tour-9-textes`. Les autres langues sont tentées (L16). | Constat espagnol après l'entrée de la branche textes ; les autres langues sur C15. |
| F136 (hors Mix) | tenté en partie | Les effets ouverts hors du Mix ne nomment pas la piste : seul un test rouge existe (`eq/tour-9-L18`). | Lot à finir ; C31 constate seulement le côté Mix. |
| D03, D05, D07, D09, D15, D16, D18, S67, F020, F031 | pas pris | Rien n'a changé : un siège reverrait le défaut d'origine. | Lots à ouvrir (validation du tuto pour D03 D07 D09 D15 D18 S67 ; contenu pour D05 ; décision pour D16 ; `TutoChecks.kt` pour F020 ; décision pour F031). |
| F041 | abandonné (proposé) | La phrase contredit le choix du tour 6 (`Tour6TutoCellsPanel100Test`). | Au chef : retirer de la liste. |
| F008, F015, F022, F035, F038, F049, F101 | non reproduit | Rien n'a changé ; un siège peut le revoir mais ne prouverait aucune réparation. | Garder leurs tests de garde ; F101 reste dans la fiche tuto (les glissés partaient du panneau). Un siège du contrôle qui le revoit le signale comme neuf. |

Attendu rouge dès maintenant : **D10** (l'entrée « Renommer » du menu « A ▾ » reste cachée tant que `TopBar` ne reçoit pas `renameScene` depuis `AppRoot.kt`) et peut-être **D06** à 360 dp / 200 %.
