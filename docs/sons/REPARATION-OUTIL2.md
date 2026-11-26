# Outil 2 : réparation après l'audit 3

Branche `eq/outil2-repare`, base `eq/outil2-reuni` `0112c240`. Chaque défaut : un commit « rouge: »
(test qui échoue, vu en local) puis « vert: » (même sujet). Tests : `selftest_tout_repare.py`.

## Corrigé

| N° | Défaut | Ce qui change |
|---|---|---|
| 1 | MIDI d'un kit sans tonalité écrite : tonalité devinée | le MIDI ne reçoit que la `key_tag` du kit (écrite ou confirmée par le son), sinon rien : `key_tag` et `scale_fit` vides |
| 2 | one-shot mélodique avec « bpm » dans le nom rangé en boucle | un son de moins de 8 s, nommé comme une boucle par son seul tempo, avec une seule attaque (`_attacks` : tranches de 50 ms qui montent de 9 dB) est un `oneshot_melodic` ; la note est gardée, pas de `bpm_tag` ; « 808 Long 140bpm C » reste `drum_oneshot` |
| 3 | style tiré du nom du pack trop gourmand | la console dit « style du pack : X, posé sur N lignes » ; `--no-pack-style` l'enlève, `--pack-style STYLE` l'impose ; le style d'un dossier intérieur gagne toujours |
| 4 | résultat différent selon qu'on donne le pack ou son parent | un dossier seul à la racine (sans son à côté) est le pack, pas un rôle ; exception : un simple dossier de rôle (« Hats ») reste un rôle |
| 5 | tempo écrit démenti par le son : `bars` absurde | `bars` vide quand l'écart n'est ni nul, ni moitié, ni double |
| 6 | « Loop 70 Am 140 » jouée à 140 : aucun tempo | parmi plusieurs nombres nus proches, celui égal au tempo entendu passe avant moitié/double |
| 7 | piste de batterie d'un kit avec la tonalité du kit | vidée (`key_tag`, `key_measured`, `key_match`) |
| 8 | lettre seule confirmée : `key_match` exact | non reproduit tel que décrit (la lettre seule sortait déjà juste) ; posé quand même : `key_match = root`, car seule la fondamentale est écrite |
| 11 | même `file_id` d'un pack à l'autre | le nom du dossier du pack entre dans le hachage (`_PACK_NAME`) ; `--trace` suit la même clé |
| 12 | Bb écrit, A# mesuré | `key_est` et `key_measured` écrits en bémols quand `key_tag` l'est |
| 13 | `b5_weird.py` écrivait 54 sons dans le dépôt | sous le dossier de travail (`AUDIT2_WORK`) |
| 14 | rejeux b1 à b8 toujours « réussis » | `lib.fault()` / `lib.done()` : sortie 1 dès qu'une vraie faute est vue ; une case vide n'est plus comptée fausse (b4) ; b8 attend `bars` = 4 ou vide |
| 18 | « end_cut vide » d'un one-shot à tempo | l'en-tête de `tout.py` dit que ce n'est pas un verdict |

Non fait (facultatif) : 10, le rôle lu dans le nom quand le son dément le type.

## Trous de test bouchés

`a3-06` (pistes d'un kit re-mesurées au tempo confirmé), `a3-13` (`file_id` de la ligne MIDI d'un kit,
retrouvée par `--trace`), `a3-16` (justesse d'une note de one-shot : 120 cents = faux), `a3-17`
(mesures du kit recomptées au tempo confirmé). Chacune appliquée à la main : le test tombe.
`test_mutations_run_every_selftest` fait tourner le vrai `run_one` de `mutate.py` sur un mini-dépôt
(faute vue par le test principal, par le test d'un lot seulement, par aucun : tuée, tuée, survit).
`test_doc_matches_fence` lit dans l'en-tête les types et les noms cités et vérifie chacun contre
`fence_row` et `classify`.

## Self-tests

MUT_SELFTESTS

## Mutations

MUT_RESULT

## Rejeux

REPLAYS

## Relecture neuve

REVIEW
