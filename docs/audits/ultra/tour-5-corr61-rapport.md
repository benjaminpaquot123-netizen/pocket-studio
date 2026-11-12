# Tour 5 : corrections.tsv, colonne « cible » (PLAN-RESTE 6.1)

Branche `eq/tour-5-corr61`. Outil : `python3 -I scripts/bench/applique-corrections.py --dry` (jamais lancé pour de vrai ici :
le pilote l'exécute après la fusion ; `applique-corrections.py` et `base.tsv` ne sont pas touchés).

## Comptes

| | avant | après |
|---|---|---|
| citations | 345 | 347 (deux citations dont la note nommait deux fiches ont été dédoublées, voir § Dédoublées) |
| clés résolues | 279 | 315 |
| lignes de base.tsv qui changeraient | 279 | 315 |
| sans clé | 0 | 0 |
| ambiguës | 43 | 0 |

Méthode : pour chaque citation ambiguë, lecture de la fiche du testeur à cet écran (`sieges/tour-N/lotX/s/rapport.md`,
champs cible / attendu / obtenu / symptome) et du libellé de l'item dans la synthèse du tour (`tour-1-synthese.md`,
`sieges/tour-2/synthese.md`, `sieges/tour-3/synthese.md`). La « cible » est un morceau de la cible de la clé (ou son
tag de bouton), choisi pour n'être contenu que dans une seule des clés de l'écran, casse ignorée. Vérification : chaque
cible ne retient qu'une clé (script de contrôle hors dépôt, résultat identique à celui d'`applique-corrections.py`).

Piège rencontré : « simple » seul ne suffisait pas pour T1-38 (la route `tab_simple_Rythme` le contient aussi) ; « mode
simple » tranche.

## Table : citation → cible → pourquoi

| ligne | tour · item | siège#écran | état | cible | clé retenue | pourquoi (fiche du testeur) |
|---|---|---|---|---|---|---|
| 11 | tour-1 · déjà réglé (gel ▶) | A/d#18 | banc | `top_record` | `timeline_screen/tab_Structure\|top_record\|rien` | fiche « Enregistrer ce que je joue » symptôme bloque : « la lecture repart en boucle sur l'Intro… écran figé » = la note |
| 21 | tour-1 · T1-03 | B/a#18 | corrigé | `feuille pad 1` | `dialogue:library\|مكتبتي feuille pad 1…\|perdu` | fiche « مكتبتي » : « bibliothèque de l'app seulement, aucune piste de ma chanson » = bibliothèque sans les pistes séparées |
| 35 | tour-1 · T1-08 | C/d#24 | corrigé | `padlive_fx_riser` | `padlive\|padlive_fx_riser\|rien` | fiche « Montée · 4 mesures » : aucun changement à l'écran = montée sans retour visuel (PadLiveRiserShownTest) |
| 45 | tour-1 · T1-13 | A/c#58 | corrigé | `fertig` | `dialogue:« Partager…\|fertig drum--bass…\|sans-aide` | fiche « Fertig: … .m4a » : « les deux exports ne diffèrent que par le format » = la note |
| 47 | tour-1 · T1-13 | F/c#63 | corrigé | `streamingu` | `mixer/tab_Mix\|poziom jak muzyka w streamingu −14\|incompris` | fiche « Poziom: jak muzyka w streamingu (−14) » : rapport streaming pour un export club |
| 51 | tour-1 · T1-15 | A/c#58 | corrigé | `fertig` | idem ligne 45 | même fiche : « aucune cible ni mesure, seul le type de fichier s'affiche » = la note |
| 53 | tour-1 · T1-15 | F/c#63 | corrigé | `lufs` | `mixer/tab_Mix\|−12 9 lufs · szczyt — dbtp\|incompris` | fiche « −12,9 LUFS · szczyt — dBTP » : crête « — », 1,1 LU d'écart sans explication = la note |
| 60 | tour-1 · T1-17 | A/c#58 | corrigé | `enregistrer dans fichiers` | `dialogue:share_progress\|partager enregistrer dans fichiers\|incompris` | fiche « Partager… / Enregistrer dans Fichiers » : « le & du titre devient -- » est dans son obtenu |
| 62 | tour-1 · T1-18 | A/c#58 | banc | `enregistrer dans fichiers` | idem ligne 60 | même fiche : « menu de partage en allemand attendu, textes en français » = feuille Partager en français |
| 68 | tour-1 · T1-20 | A/d#18 | corrigé | `le kick` | `timeline_screen/tab_Structure\|guide · le kick son motif…\|affichage` | fiche « Guide · Le kick… » : titre recouvert par la pastille REC / Noter |
| 74 | tour-1 · T1-20 | F/a#1 | corrigé | `etiqueter` | `rhythm/tab_simple_Rythme\|etiqueter l ecran\|affichage` | fiche « ? (Étiqueter l'écran) » : à moitié caché sous la pastille REC / Noter |
| 91 | tour-1 · T1-25 | C/c#26 | corrigé | `enregistre une melo` | `?\|guide · enregistre une melo\|affichage` | fiche « Guide · Enregistre une mélo… » : Guide ouvert sur la moitié basse, cache « Enregistrer » |
| 100 | tour-1 · T1-29 | E/b#18 | banc | `audio_add` | `audio_tracks/tab_Sons\|audio_add\|rien` | fiche « Registra » symptôme rien (clé = bouton du geste précédent) : rien ne change = micro absent du banc |
| 102 | tour-1 · T1-30 | A/d#18 | corrigé | `ce que je joue` | `timeline_screen/tab_Structure\|enregistrer ce que je joue\|sans-aide` | fiche sans-aide : « aucune mention de voix, micro… seul enregistrement visible = ce que je joue » |
| 105 | tour-1 · T1-31 | E/b#18 | corrigé | `chitarra` | `audio_tracks/tab_Sons\|carte chitarra piste audio\|sans-aide` | fiche carte « Chitarra » : « décompte caché dans Impostazioni » = la note |
| 106 | tour-1 · T1-31 | C/c#26 | corrigé | `more_voice` | `dialogue:top_more_menu\|more_voice\|incompris` | fiche « Rien n'a été joué pendant la prise » symptôme incompris (clé = bouton ⋮ Ma voix du geste précédent) |
| 113 | tour-1 · T1-34 | A/b#50 | corrigé | `clap` | `tab_simple_Rythme\|a toi sur le pad clap t\|affichage` | fiche « À toi : sur le pad Clap, t… » : consigne repliée sur une ligne |
| 114 (nouvelle) | tour-1 · T1-34 | A/b#50 | corrigé | `pas 5` | `tab_simple_Rythme\|pas 5 2 case en pointille\|affichage` | fiche « Pas 5 (case en pointillé) » : 1re rangée cachée sous « Partie : A » ; même commit et même test (`…AndTheDottedCellsComeWholeUnderThePartLine`) |
| 135 | tour-1 · T1-38 | F/a#1 | corrigé | `mode simple` | `rhythm/tab_simple_Rythme\|simple mode simple\|affichage` | fiche « Simple (Mode Simple) » : « Sim… » coupé |
| 150 | tour-1 · T1-40 | B/a#18 | corrigé | `live_chooser_mine` | `dialogue:live_pad_chooser\|live_chooser_mine\|incompris` | fiche « لـالـPad الحي 1 » symptôme incompris : article doublé |
| 173 | tour-2 · n° 7b | A/b#14 | corrigé | `cut_song` | `?\|cut_song_3a02…\|incompris` | fiche « Cortar lo que quiero » : la chanson entière est ajoutée sans message |
| 174 | tour-2 · n° 7c | A/b#14 | corrigé | `cortes` | `cut_screen\|un trozo / en el tiempo noms cortes / iman\|sans-nom` | fiche « Cortes: / Imán: » : rien après les deux-points (TalkBack) |
| 175 | tour-2 · n° 8a | A/d#31 | banc | `export` | `mixer/tab_Mix\|export\|incompris` | fiche « −12.9 LUFS integrated… same as streaming export » ; les 4 fiches incompris de l'écran 31 partagent cette clé |
| 177 | tour-2 · n° 8b | A/d#31 | banc | `length` | `mixer/tab_Mix\|length 3 s\|affichage` | fiche « Length: 3 s » : 3 s sur les trois exports |
| 178 | tour-2 · n° 8c | A/d#31 | banc | `export` | idem ligne 175 | fiche « peak −12.0 dBTP with −12.9 LUFS » : chiffres pas crédibles |
| 181 | tour-2 · n° 9c | A/d#31 | corrigé | `export` | idem ligne 175 | fiche « Level: like streaming music (−14) » pour un export club |
| 182 | tour-2 · n° 9d | A/d#31 | corrigé | `export` | idem ligne 175 | fiche « ✓ Ready to share (green) » au-dessus d'un écart de 5,4 LU |
| 194 | tour-2 · n° 13d | B/a#9 | corrigé | `deux boutons retour` | `dialogue:wizard\|deux boutons retour…\|incompris` | fiche « deux boutons Retour » = la note |
| 196 | tour-2 · n° 13f | B/a#9 | corrigé | `etape 3 sur 10` | `dialogue:wizard\|etape 3 sur 10 grille 1 2 3 4\|incompris` | fiche « Étape 3 sur 10 » : pas de titre d'instrument |
| 208 | tour-2 · n° 18 | B/b#7 | corrigé | `audio_stop` | `audio_tracks\|audio_stop\|incompris` | fiche « 테이크 03:37 · 5.5 s vs 녹음: 5.0초 » symptôme incompris : unité « s » vs « 초 » |
| 209 | tour-2 · n° 19a | B/b#14 | corrigé | `interrupteur` | `audio_tracks\|interrupteur 사운드 audio 1 소리 끄기 etat 꺼짐\|incompris` | fiche interrupteur « 사운드 » : « 꺼짐 » affiché alors que la piste sonne |
| 211 | tour-2 · n° 20a | B/b#5 | corrigé | `barre de niveau` | `audio_tracks\|barre de niveau d entree sous audio 1…\|sans-nom` | fiche barre de niveau d'entrée : trait fin, sans libellé ni nom TalkBack |
| 212 | tour-2 · n° 20b | B/b#5 | corrigé | `record_choice_mic` | `dialogue:record_choice\|record_choice_mic\|incompris` | fiche piste « Audio 1 » symptôme incompris : nom laissé en anglais |
| 213 | tour-2 · n° 21 | B/b#1 | corrigé | `cartes de l accueil` | `dialogue:welcome\|cartes de l accueil\|sans-aide` | fiche « cartes de l'accueil » : rien sur l'enregistrement |
| 218 | tour-2 · n° 23c | A/a#38 | corrigé | `rumble` | `tuto_spot_ring\|a toi sur le pad rumble\|incompris` | fiche « À toi : sur le pad Rumble » : les pads sont cachés |
| 228 | tour-2 · n° 28 | B/b#7 | corrigé | `노멀라이즈` | `audio_tracks\|노멀라이즈 리버스 선택 부분 남기기\|sans-aide` | fiche « 노멀라이즈, 리버스 » : jargon translittéré |
| 229 | tour-2 · n° 29 | B/b#1 | corrigé | `당신의` | `dialogue:welcome\|아직 음표가 없어요…당신의…\|incompris` | fiche « 아직 음표가 없어요… » : « 당신의 » mot à mot |
| 230 | tour-2 · n° 29 | B/b#14 | corrigé | `네 차례` | `audio_tracks\|네 차례 들려야 할 소리는…\|incompris` | fiche « 네 차례… » : 반말, espace « ▶ 를 » |
| 234 | tour-3 · T3-01 | D/b#39 | corrigé | `padlive_back` | `padlive\|padlive_back\|incompris` | fiche « accueil après Geri de Pad Live » : pads 1 et 3 continuent |
| 243 | tour-3 · T3-03b | E/d#36 | corrigé | `casilla voz` | `live_grid/tab_Live\|casilla voz · 2 bloque a\|rien` | fiche casilla « Voz · 2 » : voix coupée dans Mix, rien ne le dit dans le Live |
| 255 | tour-3 · T3-07 | F/c#6 | corrigé | `coin haut droit` | `padlive\|16 bouton sans nom sur les pads coin haut droit\|sans-nom` | fiche 16 « bouton sans nom » au coin des pads |
| 262 | tour-3 · T3-11 | A/d#45 | corrigé | `my projects` | `dialogue:projects\|my projects / options\|perdu` | fiche « My projects / Options » : aucune liste d'exports, carte disparaît après Cancel |
| 292 | tour-3 · T3-17 | B/a#4 | voulu | `barre du bas` | `rhythm/tab_simple_Rythme\|barre du bas annuler\|rien` | fiche barre du bas « Annuler » : aucun « Rétablir » à l'écran (voulu : pas d'Annuler préalable) |
| 330 | tour-3 · T3-37 | B/b#7 | banc | `barre du haut` | `session/tab_simple_Morceau\|barre du haut annuler enregistrer…\|sans-nom` | fiche barre du haut lue « bouton » |
| 331 (nouvelle) | tour-3 · T3-37 | B/b#7 | banc | `sur chaque piste` | `session/tab_simple_Morceau\|sur chaque piste rond ▶ et reglages ≡ lus bouton\|sans-nom` | fiche ▶/≡ de chaque piste lus « bouton » ; même verdict banc (lit=non tait les noms) |

## Dédoublées (deux fiches sous une seule citation)

Deux lignes de `corrections.tsv` citaient un écran dont la note nommait **les deux** fiches du testeur ; une seule
cible n'aurait couvert qu'une clé et l'autre fiche aurait perdu son état. Chaque ligne a été dédoublée (mêmes tour, item,
siège, écran, état, commit, test ; note coupée en deux, une cible par fiche) :

- tour-1 T1-34 A/b#50 (corrigé `f6bf551`) : « consigne repliée « t… » » (`clap`) et « 1re rangée sous Partie : A »
  (`pas 5`). Le test `TutoBigFontTest#theFoldedLineSaysTheStepWholeAndTheDottedCellsComeWholeUnderThePartLine` couvre
  les deux, la synthèse du tour 1 (n° 34) aussi.
- tour-3 T3-37 B/b#7 (banc) : « barre du haut lue bouton » (`barre du haut`) et « ▶/≡ de chaque piste lus bouton »
  (`sur chaque piste`). La synthèse du tour 3 (n° 37) range les deux sous le même « à vérifier sans le banc, lit=non ».

Retour arrière si le pilote préfère une seule ligne : supprimer la ligne 114 (ou 331) ; la clé correspondante reste
alors « nouveau » dans base.tsv.

## Indécidables

Aucune : chaque citation ambiguë correspondait à une fiche identifiable par son champ obtenu ou par le libellé de l'item
dans la synthèse. Points à connaître, pas des doutes :

- Écran 31 du siège A/d (tour 2) : cinq fiches du testeur mais deux clés seulement (les quatre fiches « incompris »
  prennent le bouton du geste précédent, `export`). Les citations 8a, 8c, 9c, 9d visent donc la même clé
  `mixer/tab_Mix|export|incompris` ; priorité du script : corrigé (9c, 9d) l'emporte sur banc (8a, 8c). C'est la
  mécanique de la fusion v1, pas un choix de cette passe.
- Lignes 100 (T1-29, `audio_add`) et 106 (T1-31, `more_voice`) : la clé porte le bouton du geste précédent, pas le mot
  du testeur (« Registra », « Rien n'a été joué ») ; la cible nomme ce bouton parce que c'est la seule clé de la fiche.
