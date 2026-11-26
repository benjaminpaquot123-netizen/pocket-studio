# Plan de la deuxième passe profonde (les 12 RAR)

Demandé par Benjamin le 2026-11-26 : « planifie un peu pour que ce soit clair et qu'on n'oublie rien ».
Périmètre : les 12 RAR seulement, pas les 2 zips. On ne touche ni à l'appli ni aux sons. Rien n'est supprimé du Drive.
Les liens Drive ne s'écrivent jamais dans le dépôt (ils sont dans les notes du chef).

## Combien de discussions
- 1 pour le dernier contrôle de l'outil (Fable, à l'aveugle, 30 minutes au plus).
- 1 pour la répétition générale (un seul petit RAR).
- 11 en même temps pour les 11 autres RAR (une discussion par RAR).
- 1 pour réunir les résultats.
- 1 pour la critique à l'aveugle.
Au plus fort : 11 discussions ouvertes à la fois, sous le plafond de 31. Chacune est muette et s'archive seule.

## Les étapes, dans l'ordre
- [ ] 0. Réparation finie (`eq/outil2-repare`, message « fini <sha> »). Le chef vérifie : sondes à 0 échec, rejeux 10 sur 10 sans rouge attendu, toutes les mutations tuées, relecture neuve faite.
- [ ] 1. Petit ménage du banc : étiquette « 11.5 MB » fausse dans `audit2/b5_weird.py`, ligne d'erreur `end_cut` retirée des rouges attendus.
- [ ] 2. Dernier contrôle à l'aveugle (Fable, réflexion extra, 30 minutes au plus) sur l'outil réparé. Il faut le verdict « bon pour usage ». Sinon : un tour de réparation, puis un nouveau contrôle. Pas de passe avant ce verdict.
- [ ] 3. Branche de départ unique `eq/passe2-base` : outil réparé + lanceur (`eq/passe2-prep`) + rejeux. Tous les self-tests et `test_lance.py` verts dessus. Toutes les discussions de passe partent de cette même tête, notée ici : ________.
- [ ] 4. Répétition générale : le RAR 3 (le plus petit) seul, avec `docs/sons/FICHE-PASSE2.md`. On regarde le temps, le disque, le tableau de contrôle. Un souci ici se corrige avant de lancer les autres.
- [ ] 5. Les 11 autres RAR en même temps. Chaque discussion : plan d'abord (noms des packs et styles, comparés à `docs/sons/STYLES-PACKS.md`), puis la passe complète, puis ses CSV sur sa branche `eq/passe2-rarN`.
- [ ] 6. Contrôle du chef, RAR par RAR (tableau ci-dessous) : message « fini » reçu, comptes justes partout, 0 erreur ou erreurs listées, nombre de fichiers cohérent avec la première passe.
- [ ] 7. Réunion (1 discussion) : un seul grand tableau, `ANALYSE-MINE-OR.md` mis à jour, inventaire par style et par type (batterie, mélodies, boucles, voix, kits, MIDI).
- [ ] 8. Critique à l'aveugle (1 discussion neuve qui n'a rien écrit) : elle contredit et remesure quelques chiffres clés.
- [ ] 9. Résumé à Benjamin, cases cochées dans `docs/PROMESSES.md`, discussions archivées.

## Suivi des 12 RAR
| RAR | Contenu connu | Lancé | Fini (sha) | Comptes justes | Erreurs |
|---|---|---|---|---|---|
| 1 | Drum Hero 6 | | | | |
| 2 | Drift phonk | | | | |
| 3 | Melodic techno (répétition) | | | | |
| 4 | Lofi Cherry Blossom | | | | |
| 5 | Blood Rave | | | | |
| 6 | Amapiano | | | | |
| 7 | UPB 2026 (le gros : voix, MIDI) | | | | |
| 8 | Dubstep + afro-house | | | | |
| 9 | Deathstep | | | | |
| 10 | DnB, deep house, hardstyle | | | | |
| 11 | Ghosthack 4-5-6 | | | | |
| 12 | Ghosthack 1-2-3 | | | | |

## Ce qui arrête tout
- Le dernier contrôle ne dit pas « bon pour usage ».
- La répétition générale montre un compte faux ou une erreur de l'outil.
- Une discussion dit « bloqué » : le chef la traite seule, les autres continuent.
- Un son, une archive ou un lien Drive apparaît dans un commit : on arrête et on nettoie.

## Après la passe (pas dans ce plan)
Reprise du Grand Remplacement et de la bibliothèque de sons synthétisés, réglés sur ces mesures.
