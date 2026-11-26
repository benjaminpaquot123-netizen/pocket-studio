# Outil 2 : réunion des cinq lots de `tout.py`

Branche `eq/outil2-reuni`, base `453d2b33`. Les lots A à E sont fusionnés sans conflit à la main
(les cinq fusions sont l'arbre de la fusion automatique).

## Branchements faits

1. `confirm_tags` passe sur chaque ligne mesurée (boucles, sons seuls, kits et pistes de kit) :
   un nombre nu devient le tempo quand on l'entend, une lettre seule devient la tonalité ou la
   note quand le son la confirme, les bémols restent écrits en bémols.
2. Le nom du dossier du pack arrive à `style_of` (pack donné directement : « techno » trouvé).
3. `file_id` est écrit sur les lignes `kit` et `kit_stem`.
4. `audit2/mutate.py` joue `selftest_tout.py` et tous les `selftest_tout_*.py` (plus `selftest.py`
   pour `mesure.py`), et refuse de mesurer si un test est rouge avant toute faute.
5. Défaut 18 : `end_cut` vidé pour les mixes entiers, les coups nommés avec un tempo et les boucles.
6. L'en-tête de `tout.py` dit les règles de la clôture telles qu'elles sont codées.
7. Restes du lot A couverts par `selftest_tout_reuni.py`.

## Mutations

103 fautes semées. Premier passage complet : 92 attrapées, 11 passées (4 seuils de la fin coupée,
largeur stéréo inversée, nombre de canaux, arrondi des mesures, signe de l'écart de tempo, swing par
défaut, 2 seuils de « utilisable »). Chacune a maintenant son contrôle dans
`test_mutation_survivors`. Passage complet final sur le code corrigé : 103 attrapées, 0 passée.

Piège rencontré : sur une copie sans marqueur de dépôt, `selftest_tout.py` est rouge avant toute
faute et « tue » tout. D'où le garde-fou du point 4.

## Relecture neuve

Une faute grave trouvée et corrigée (rouge puis vert) : `confirm_tags` passe deux fois sur les sons
seuls, et la seconde fois comparait « Bb » à « A# » ; une note bémol seule, pourtant juste, perdait
son étiquette. Restes mineurs, non corrigés :

- la précision des notes des sons seuls se compte après confirmation (une lettre démentie par le son
  ne compte pas comme une étiquette fausse), les boucles comptent avant ;
- `test_mutations_run_every_selftest` ne lit que `tests_for` ; `test_doc_matches_fence` ne cherche
  que des phrases ; la re-mesure des pistes d'un kit après confirmation du tempo n'a pas de test propre.

## Rejeux b1 à b8

Conformes à `AUDIT-TOUT-2.md` : b3, b3b, b3c, b4, b5, b6, b7, b8. Écarts, hors du périmètre de la réunion :

- b8 imprime encore 20 « WRONG » : son libellé attend `bars = 4` sur des nappes sans tempo écrit ;
  le lot C a choisi de laisser vide ce qu'on ne sait pas. C'est le script de rejeu qui est périmé.
- Un tempo écrit « sûr » (dossier `140/`, « 125 Gm », kit « 100 bpm ») n'est jamais retiré quand le
  son dit autre chose : il reste affiché avec son écart (`bpm_gap_pct`), et les mesures se comptent
  depuis lui. L'audit ne demande rien là-dessus ; à trancher dans un lot à part.
- `Synth/Bass 120 C.wav` (2 s) sort en boucle mélodique à 120 ; `FX/Reverse Crash 01` est bien `fx`
  mais garde le rôle `hats`.
