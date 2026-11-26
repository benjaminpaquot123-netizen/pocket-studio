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
| 13 | `b5_weird.py` écrivait 54 sons dans le dépôt | passé à un autre lot (consigne du chef : `audit2/` hors de ce lot sauf `mutate.py`) |
| 14 | rejeux b1 à b8 toujours « réussis » | passé à un autre lot (même consigne) |
| 18 | « end_cut vide » d'un one-shot à tempo | l'en-tête de `tout.py` dit que ce n'est pas un verdict |

### Ajouts de l'audit 4 (même fichier, donc ce lot)

| N° | Défaut | Ce qui change |
|---|---|---|
| A1 | boucle 120 : charlestons 6 dB sous la caisse claire, grille des charlestons = la caisse claire | chaque bande garde ses pas au-dessus d'un seuil relatif à son propre maximum |
| A2 | kick + charlestons sans caisse claire : coups de caisse claire inventés | même règle par bande : une bande presque vide ne prend pas le débord des autres |
| A3 | MIDI à 3 temps sans signature : `bars` faux | accents (temps plus fort ou plus grave tous les 3 temps) lus ; sinon 4/4 |
| A4 | fichier de moins de 400 ms : pas de porte -70 LUFS | un bloc, porte -70 LUFS appliquée |
| A5 | silence numérique : ligne « propre » | ligne d'erreur « digital silence, no sound » |
| A6 | RF64 / RIFX : « not a wav » | erreur nommée « RF64/RIFX/BW64 non pris en charge » |
| A7 | mesure « utilisable » sur trop peu de fichiers | borne haute de Wilson à 95 % unilatérale < 5 % : 52 sur 52 au moins |
| R1 | « 00bpm » dans le nom : ZeroDivisionError | tempo nul ignoré |
| R2 | dossier « Loops & One Shots » pris pour un dossier de one-shots | le nom du pack n'entre pas dans la règle |
| R3 | ligne legato à une attaque prise pour un one-shot | il faut aussi une seule hauteur tenue (`_one_pitch`, 80 % des trames à ±100 cents) |
| R5 | BW64 non nommé | dans la même erreur nommée que RF64/RIFX |
| R6 | MIDI de batterie accentué tous les 3 temps : `bars` en 3/4 | canal 10 seul : pas de lecture des accents |

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
