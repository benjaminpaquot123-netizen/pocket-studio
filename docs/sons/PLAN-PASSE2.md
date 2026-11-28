# Plan de la deuxième passe profonde (les 12 RAR)

Demandé par Benjamin le 2026-11-26 : « planifie un peu pour que ce soit clair et qu'on n'oublie rien ».
Périmètre : les 12 RAR seulement, pas les 2 zips. On ne touche ni à l'appli ni aux sons. Rien n'est supprimé du Drive.
Les liens Drive ne s'écrivent jamais dans le dépôt (ils sont dans les notes du chef).

## Combien de discussions
- 1 pour la répétition générale (un seul petit RAR).
- 11 en même temps pour les 11 autres RAR (une discussion par RAR).
- 1 pour réunir les résultats.
- 1 pour la critique à l'aveugle.
Au plus fort : 11 discussions ouvertes à la fois, sous le plafond de 31. Chacune est muette et s'archive seule.

## Les étapes, dans l'ordre
- [x] 0. Réparation finie (`eq/outil2-repare`, puis le raccord `eq/passe2-raccord`). Vérifié par le chef le 2026-11-26 : sondes à 0 échec, rejeux 10 sur 10, relecture neuve faite. Passage complet des mutations sur `d7047ced` (raccord `8e10dfcaa`, docs seulement) : 106 jouées, 103 tuées, 1 survivante devenue sans effet, 2 sautées (leur texte a changé) ; les trois sont à réécrire dans un prochain lot. À faire aussi dans ce lot : `audit4/accuracy.py` ne compte que « exact », il doit compter « root ». Cas de noms 7, 8 et 10 laissés ouverts exprès (« Termine maintenant », voir `docs/DECISIONS.md`).
- [ ] 1. Petit ménage du banc : étiquette « 11.5 MB » fausse dans `audit2/b5_weird.py`, ligne d'erreur `end_cut` retirée des rouges attendus.
- [x] 2. Dernier contrôle Fable à l'aveugle : levé par Benjamin le 2026-11-26 (« je ne vise pas la perfection »). Remplacé par : sondes à 0 échec, relecture neuve du lot de réparation, et la répétition générale de l'étape 4 comme vrai test. Raison : une passe refaite coûte une demi-heure, un audit de plus aussi. Retour : « contrôle Fable ».
- [x] 3. Branche de départ unique `eq/passe2-base` : outil réparé + lanceur (`eq/passe2-prep`) + rejeux. Tous les self-tests et `test_lance.py` verts dessus. Toutes les discussions de passe partent de cette même tête, notée ici : `d7047cedc` (2026-11-26 ; self-tests, `test_lance.py`, sondes et 10 rejeux verts, vérifiés par le chef).
- [x] 4. Répétition générale : le RAR 3 (le plus petit), avec `docs/sons/FICHE-PASSE2.md`. On regarde le temps, le disque, le tableau de contrôle. Changement du 2026-11-26 (« Termine maintenant ») : elle part en même temps que les 11 autres, de la même tête ; si elle montre une faute de l'outil, on ne relance que les paquets touchés.
- [x] 5. Les 11 autres RAR en même temps. Chaque discussion : plan d'abord (noms des packs et styles, comparés à `docs/sons/STYLES-PACKS.md`), puis la passe complète, puis ses CSV sur sa branche `eq/passe2-rarN`.
- [x] 6. Contrôle du chef, RAR par RAR (tableau ci-dessous) : message « fini » reçu, comptes justes partout, 0 erreur ou erreurs listées, nombre de fichiers cohérent avec la première passe (comparaison faite par la réunion sur 9 RAR : aucun écart, sauf le RAR 12 avec 22 fichiers de moins, une archive imbriquée non lue ; les RAR 7, 9 et 11 restent à comparer par elle).
- [x] 7. Réunion (1 discussion) : un seul grand tableau, `ANALYSE-MINE-OR.md` mis à jour, inventaire par style et par type (batterie, mélodies, boucles, voix, kits, MIDI). Fait le 2026-11-26 à 22 h 39 : `eq/passe2-reunion` `8d4b14d9e`, 12 425 lignes (10 417 sons, 1 817 MIDI, 191 lignes de kit), 0 doublon, 0 erreur ; contrôlé par le chef (ni son, ni lien, ni nom de fichier, 0 lien symbolique). Écarts avec la première passe : aucun sauf le RAR 12 (22 fichiers d'une archive imbriquée non lue). Restent : 5 packs sans style (2 547 fichiers), 14 coups d'écart au RAR 12 non expliqués.
- [x] 8. Critique à l'aveugle (1 discussion neuve qui n'a rien écrit) : elle contredit et remesure quelques chiffres clés. Ouverte le 2026-11-26 à 22 h 42 sur `eq/passe2-critique` (elle retélécharge le RAR 3 et remesure 40 sons avec son propre code). Finie le 2026-11-26 à 22 h 58 : `eq/passe2-critique` `eefa5745d`, 16 chiffres clés vérifiés, 8 tiennent, 6 fragiles, 2 faux (tonalité des boucles mélodiques : fausse 7 fois sur 10 ; hauteur des claps, charleys et cymbales : du bruit, avec un faux « n »). Remesure de 40 sons du RAR 3 : durée, crête et RMS 40 sur 40 ; lanceur rejoué, 8 CSV identiques octet par octet. Cibles sûres listées dans `docs/sons/passe2/CRITIQUE-AVEUGLE.md` ; transmises au lot `eq/matin-batterie2`. Reste à faire après la critique : corriger `chiffres.md` et `INVENTAIRE.md` (vrai « n », tonalité retirée des cibles, kick par style, 2 040 lignes sans style et non 2 547, fausse alerte du lanceur sur cinq RAR).
- [x] 9. Résumé à Benjamin, cases cochées dans `docs/PROMESSES.md`, discussions archivées. Fait le 2026-11-27 à 00 h 40 (résumé du matin avec la page des sons).

## Suivi des 12 RAR
| RAR | Contenu connu | Lancé | Fini (sha) | Comptes justes | Erreurs |
|---|---|---|---|---|---|
| 1 | Drum Hero 6 | 26-11 22 h 17 | `f08dac9d6` | oui (1388 = 1388) | 0 |
| 2 | Drift phonk | 26-11 22 h 17 | `e902e0c70` | oui (785 = 785) | 0 |
| 3 | Melodic techno (répétition) | 26-11 22 h 17 | `f1b112281` | oui (370 = 370) ; fausse alerte du lanceur pour 1 ligne de style | 0 |
| 4 | Lofi Cherry Blossom | 26-11 22 h 17 | `3e25cea17` | oui (250 = 250) | 0 |
| 5 | Blood Rave | 26-11 22 h 17 | `dcc0ebf24` | oui (535 = 535) | 0 |
| 6 | Amapiano | 26-11 22 h 17 | `6f588cac8` | oui (623 = 623) | 0 |
| 7 | UPB 2026 (le gros : voix, MIDI) | 26-11 22 h 17 | `0fa06f672` | oui (2026 fichiers, 2034 lignes dont 8 de kit) | 0 |
| 8 | Dubstep + afro-house | 26-11 22 h 17 | `9a804e452` | oui (1218 fichiers, 1224 lignes dont 6 de kit) | 0 |
| 9 | Deathstep | 26-11 22 h 17 | `6d4803c8b` | oui (1325 fichiers, 1376 lignes dont 51 de kit) ; pack 1 forcé en synthwave comme prévu | 0 |
| 10 | DnB, deep house, hardstyle | 26-11 22 h 17 | `d9f22701b` | oui (1025 fichiers, 1077 lignes dont 52 de kit) ; fausse alerte du lanceur pour 2 lignes de style (p1) | 0 |
| 11 | lot de packs 4-5-6 | 26-11 22 h 17 | `fc23b0808` | oui (1903 fichiers, 1977 lignes dont 74 de kit) ; le pack DnB est ici et non au RAR 10 comme l'écrit `STYLES-PACKS.md` | 0 |
| 12 | lot de packs 1-2-3 | 26-11 22 h 17 | `d05cf617a` | oui (786 = 786) | 0 |

Contrôle du chef du 2026-11-26, 22 h 30 (RAR 10 : 22 h 35), sur les tables poussées (un seul commit par branche, que des CSV dans `docs/sons/passe2/rarN/`, 0 lien symbolique, 0 identifiant en double, somme des lignes égale au tableau de contrôle). RAR 7 et 11 contrôlés de la même façon à 22 h 40. La comparaison du nombre de fichiers avec la première passe est faite par la discussion de réunion.

## Ce qui arrête tout
- Les sondes ne sont pas à 0 échec après la réparation.
- La répétition générale montre un compte faux ou une erreur de l'outil.
- Une discussion dit « bloqué » : le chef la traite seule, les autres continuent.
- Un son, une archive ou un lien Drive apparaît dans un commit : on arrête et on nettoie.

## Après la passe (pas dans ce plan)
Reprise du Grand Remplacement et de la bibliothèque de sons synthétisés, réglés sur ces mesures.
