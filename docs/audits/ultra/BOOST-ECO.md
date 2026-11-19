# Boost éco : le mode boost, au prix de l'économie de quota

Demande de Benjamin (2026-11-19) : « Trouve un moyen de rendre le mode boost comme il est, en mode économie de quota, même s'il faut diminuer le nombre de discussions, je serais d'accord. »

Le mode boost garde ses règles d'entrée (tests ciblés, self-test et relecture neuve verts ; une seule grande vérification à la fin ; « tenté » jusque-là ; `PS_JUGE_BOOST=1`). Seule change la façon de distribuer le travail. Vaut pour tout lot ouvert à partir de maintenant ; les lots déjà en vol finissent comme ils sont.

## 1. Ce qui a été mesuré (19 novembre, 21 h, heure belge ; `get_session`)

| Lot | Défauts confiés | Coût | Par défaut |
|---|---|---|---|
| L10 divers (3 écrans sans rapport) | 3 | 13,9 $ | 4,6 $ |
| L9 banc | 6 environ | 15,0 $ | 2,5 $ |
| L2 Mix | 15 environ | 34,3 $ | 2,3 $ |
| L4 grille rythme | 14 environ | 20,6 $ | 1,5 $ |

- Une discussion répare environ 3 défauts à l'heure, petite ou grande. Mais elle paie d'abord un prix fixe : lire les règles, apprendre la zone, chauffer les tests, la relecture, le pré-vol, la fusion. Sur 3 défauts ce prix fixe pèse deux à trois fois plus que sur 15.
- Les lots « restes de L2 », « finitions rendues par L3 » : une discussion neuve repaie tout le prix fixe pour finir le travail d'une autre.
- Le chef : 7 réveils en 8 minutes, 1,44 $ (environ 10 $ l'heure), chaque geste relit 148 k de mémoire. Un réveil pour « L18 s'est présentée » coûte 0,10 $ et n'apporte rien.
- Les lots confiés à des sous-agents du chef (L1, L7) réécrivent leur mémoire à chaque attente de plus de 5 minutes (tests Gradle) : 458 M de jetons réécrits ainsi dans l'histoire du chef.
- Quatre lots seulement : c'est un ordre de grandeur, pas une loi. À remesurer sur la première vague (§ 4).

## 2. Les règles

1. **Des lots pleins.** Une discussion de correction reçoit 8 à 15 défauts d'une même zone. On n'ouvre pas de discussion pour moins de 6 défauts : les miettes attendent et partent ensemble, par zone, dans un lot « restes » à la fin.
2. **Un fichier = un seul lot ouvert**, de nouveau. Autant de discussions que de zones séparées qui ont un lot plein (5 à 7 d'habitude), jamais plus. Plus de lot ouvert sur le fichier d'un autre, donc plus de conflits à régler ni de fusions à refaire.
3. **Un lot finit sa liste.** Il ne rend que ce qui est vraiment bloqué (une ligne : le défaut, la raison). Pas de lot « suite » ou « restes de L… » dans une discussion neuve : le chef rouvre la discussion du lot, qui connaît déjà la zone.
4. **La fiche est dans le message d'ouverture.** Plus de « prêt » ni d'attente de fiche. Un lot écrit au chef une seule fois : « fini <sha> », ou « bloqué : … ». Tout le reste est dans son rapport, sur sa branche.
5. **Le chef se tait et se réveille peu.** Aucun texte à un réveil qui n'est ni un jalon ni une demande de Benjamin. Une seule attente en arrière-plan pour tout le paquet (elle rend la main quand trois lots ont fini, ou après 45 minutes), pas une par lot. Mémoire du chef : `/autocompact 120k` ; l'état vit dans `EN-COURS.md`.
6. **Aucun lot de correction en sous-agent du chef.** Un lot qui attend Gradle est une discussion (mémoire gardée une heure). Les sous-agents servent aux lectures et aux relectures courtes.
7. **Dans un lot** : un test se lance en une seule commande qui attend la fin (ou en arrière-plan avec notification), jamais relu toutes les minutes ; sorties filtrées ; une seule relecture neuve, à la fin, pour tout le lot.
8. **À la grande vérification** : un siège de contrôle par zone, qui constate toutes les phrases d'acceptation de la zone (huit au plus), pas un siège par défaut.

Ce qui ne bouge pas : Opus 5.5 partout, équipes muettes et fermées seules, mémoire courte, rien sur `main`, APK d'une CI verte, thread audio, aucun test sauté.

## 3. Ce qu'on gagne, ce qu'on perd

- Gain attendu : un tiers de quota en moins pour le même nombre de défauts réparés, peut-être 40 % (lots pleins : le plus gros ; plus de lots « restes » ni de conflits ; chef deux fois moins réveillé).
- Prix : au plus fort, 5 à 7 discussions au lieu de 12 et plus, donc un débit d'environ 15 à 20 défauts à l'heure au lieu de 35. La fin d'une liste (les miettes) va moins vite.
- Retour arrière : Benjamin dit « boost plein » ; on retire la ligne « Boost éco » de `CLAUDE.md`.

## 4. La mesure qui tranche

À la fin de la première vague en boost éco, le chef note pour chaque lot (`get_session`, champ `usage.cost_usd`) : défauts confiés, défauts faits, coût. Repère du boost plein : 1,5 à 4,6 $ par défaut, vers 2,5 $ en moyenne. Si le boost éco ne descend pas sous 2 $, on le dit à Benjamin et on cherche ailleurs.
