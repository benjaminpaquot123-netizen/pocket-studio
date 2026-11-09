# Relecture neuve : tour 5, colonne « cible » de corrections.tsv (corr61, PLAN-RESTE 6.1)
Relu : 6c2ea01

Relecteur qui n'a pas écrit le commit. Branche `eq/tour-5-corr61`, un commit (6c2ea01) sur ad092b5 : `docs/audits/ultra/corrections.tsv`
(colonne 9 « cible », 45 valeurs, deux lignes dédoublées) et `docs/audits/ultra/tour-5-corr61-rapport.md`. Ni code ni `base.tsv` touchés.

## Ce qui a été vérifié

1. **Sortie du script** : `python3 -I scripts/bench/applique-corrections.py --dry` dans le worktree → « 347 citations, 315 clés, 315 lignes de
   base.tsv changées, 0 sans clé, 0 ambiguës » (avant, avec l'ancien fichier : 345 / 279 / 279 / 0 / 43). `scripts/bench/test_ultra_bench.py` : OK.
2. **Forme du TSV** : 348 lignes, toutes à 9 champs séparés par une tabulation, aucun retour chariot, aucun espace collé à une tabulation.
   Les 8 premières colonnes sont identiques à ad092b5 sauf les deux dédoublements (lignes 113-114 et 330-331) : rien d'autre n'a bougé.
   45 cibles = 43 citations ambiguës + 2 lignes nées du dédoublement. Un script de contrôle indépendant (même `fuse()`) confirme que
   chaque cible ne retient qu'une clé sur son écran, et la bonne (la cible ne tombe jamais dans la route : « mode simple » et non « simple »).
3. **Cibles contre les fiches et les synthèses** : les 45 relues, pas 12, chaque fois avec la fiche du testeur (`sieges/tour-N/lotX/s/rapport.md`,
   écran cité) et le libellé de l'item dans la synthèse du tour. Toutes désignent la fiche dont parlent l'item et le commit. Les plus délicates :
   - partage A/c#58 (4 citations) : `fertig` = fiche « Fertig: … .m4a » (exports identiques, aucune mesure : T1-13, T1-15) ; `enregistrer dans
     fichiers` = fiche « Partager… / Enregistrer dans Fichiers » (« & » → « -- » : T1-17 ; feuille en français : T1-18) ;
   - mixer F/c#63 : `streamingu` = rapport club écrit « streaming −14 » (T1-13), `lufs` = « −12,9 LUFS · szczyt — dBTP » (T1-15) ;
   - mixer A/d#31 (tour 2, 5 citations, 2 clés) : `length` = fiche « Length: 3 s » (8b) ; `export` pour 8a, 8c, 9c, 9d = les quatre fiches
     « incompris » de l'écran, toutes sous le bouton du geste précédent : aucun autre choix possible ;
   - dialogues : `feuille pad 1` / `live_chooser_mine` (B/a#18 : bibliothèque sans les pistes T1-03, article doublé T1-40) ; `deux boutons
     retour` / `etape 3 sur 10` (B/a#9 : 13d, 13f) ; `barre de niveau` / `record_choice_mic` (B/b#5 : 20a, 20b) ; `cartes de l accueil` /
     `당신의` (B/b#1 : 21, 29) ; `my projects` (A/d#45, T3-11) ;
   - clés à tag de bouton (`top_record`, `audio_add`, `more_voice`, `audio_stop`, `padlive_back`, `cut_song`) : à chaque fois la seule clé
     de l'écran dont le symptôme est celui de la fiche (bloque/rien/incompris) ; cohérent avec la manière dont `fuse-reports.py` nomme ces fiches.
4. **Dédoublements** : justifiés, pas inventés. A/b#50 : la fiche du testeur a bien deux défauts (« À toi : sur le pad Clap, t… » repliée ;
   « Pas 5 » sous « Partie : A »), tous deux dans l'item 34 de la synthèse du tour 1, même commit f6bf551, et le test
   `TutoBigFontTest#theFoldedLineSaysTheStepWholeAndTheDottedCellsComeWholeUnderThePartLine` (app/src/test/…/tuto/TutoBigFontTest.kt:109) couvre les deux.
   B/b#7 (tour 3) : deux fiches « sans-nom » (barre du haut ; ▶/≡ de chaque piste), toutes deux dans l'item 37 (« à vérifier sans le banc »), même état banc.
5. **base.tsv avant / après** (copies dans un dossier à part, script lancé pour de vrai sur les copies, ancien fichier via `git show ad092b5:…`) :
   38 lignes diffèrent. 36 clés passent de « nouveau » à un état (30 corrigé, 5 banc, 1 voulu) ; 2 clés déjà renseignées changent d'état
   (voir constats 1 et 2). Sept lignes relues en entier (export, share_progress, top_record, barre du haut, barre du bas, audio_add, my projects) :
   état, note « tour item commit » et `corrige_au` plausibles et conformes à la fiche.

## Constats

| Gravité | Constat | Suite |
|---|---|---|
| mineur | `dialogue:record_choice\|record_choice_mic\|incompris` : base.tsv disait **banc, tour-3** (T3-24b, décompte « 1… », garde 531564f) ; la nouvelle cible de la ligne 212 (tour-2 n° 20b, « Audio 1 » non traduit, corrigé 69a60ab) y mène aussi, et la priorité du script (corrigé > banc, aveugle au tour) la fait passer à **corrigé, tour-2**. Même clé pour deux défauts différents (limite v1 des clés), pas une mauvaise cible. Effet : en refusionnant les tours 3-4, cette clé sortirait « revient ? » au lieu de « banc contredit ? » (candidat dans les deux cas) ; au tour 5 le lecteur a déjà répondu « non » dans revient.tsv pour cette clé, en la comparant justement à 20a/20b. La note garde les deux citations. | Rien avant la fusion. Plus tard : faire gagner le tour le plus récent quand deux citations d'états différents se disputent une clé, ou ajouter les lignes « non » dans revient.tsv au moment de la refusion. |
| mineur | `mixer/tab_Mix\|export\|incompris` : quatre défauts de nature différente (8a, 8c banc : chiffres du banc ; 9c, 9d corrigé : libellés) sous une seule clé ; l'état devient corrigé (tour-2 n° 9c), le verdict banc de 8a reste seulement dans la note. Un retour des chiffres LUFS se lirait « revient ? (9c) » plutôt que « banc contredit ? », toujours un candidat à lire. Le rapport le dit lui-même. | Aucune ; une ligne dans DECISIONS.md si le pilote veut tracer le choix. |
| mineur (hors périmètre, préexistant) | Le rapport délègue au pilote l'exécution réelle du script après la fusion. Cette exécution, avec l'ancien comme avec le nouveau fichier, remet aussi `dialogue:project_name_field\|name_clear\|rien` de **corrigé** (banc fiable 3.2, essai sur le téléphone de Benjamin, APK 339) à **banc** (lignes T4-19 de corrections.tsv, état banc). `corrige_au` reste tour-4, donc `verifie-chantier.py` ne le signalera pas. Pas le fait de ce commit. | Avant le lancement réel : mettre l'état des trois lignes T4-19 en accord avec verdicts.tsv (etat corrigé), ou faire que le script ne rétrograde pas un état posé par les verdicts. |
| info | Neuf cibles sont des tags de bouton (`top_record`, `audio_add`, `more_voice`, `audio_stop`, `record_choice_mic`, `padlive_back`, `cut_song`, `padlive_fx_riser`, `live_chooser_mine`) : elles tiennent à la façon dont `fuse-reports.py` nomme les fiches « geste » par le bouton du geste précédent. Si cette règle change, le script les redira « ambiguës » (échec visible, jamais silencieux). | Aucune. |
| info | La nouvelle ligne 114 recopie la colonne test tronquée « TutoBigFontTest#theFoldedLine… » de la ligne 113 (forme préexistante). La méthode existe. | Aucune ; le nom complet si un outil lit un jour cette colonne. |

## Conclusion

Les 45 cibles désignent la bonne fiche, les deux dédoublements sont fondés sur la fiche du testeur et la synthèse, le fichier est propre et le
script sort « 0 sans clé, 0 ambiguës ». Les deux clés qui changent d'état (constats 1 et 2) tiennent à la règle de priorité du script et à des
clés partagées, pas à une cible fausse ; aucune ne cache un retour : dans les deux cas la fusion continue de proposer un candidat à lire.

Verdict : 0 bloquant, 0 grave — fusion possible
