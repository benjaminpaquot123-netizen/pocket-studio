# Tour 8 : verdict du contrôle final (T-a à T-d)

Code testé : intégration `c9d48c185` (CI 389 verte, 0 capture changée). Quatre sièges, joués chacun par un sous-agent neuf, self-test vert à chaque fois.

| Siège | Profil | Chapitres bloqués | Graves neufs | Défauts d'avant |
|---|---|---|---|---|
| T-a | fr, téléphone, 100 %, house ch. 4-5 | 0 | 0 | CTRL-a corrigé ; D4/P7 et D3 « autre » (finitions) |
| T-b | ko, téléphone 360, 130 %, techno ch. 9-10 | 0 | 0 | D2, ctrl-c-ch10, amenée ch. 9 corrigés |
| T-c | fr, téléphone, 100 %, house ch. 12 | 0 | 2 | G12-1, G12-2 corrigés ; G12-3 « autre » ; D8, M2 pas atteints |
| T-d | ja, pliable, 200 %, Pro, techno ch. 3-6 | 0 | 0 | P6-texte, T-ch3 corrigés ; P6-grille encore là |

Règle de clôture (Benjamin) : les défauts neufs sont notés et ne lancent rien (« pas pris »).

## Restes, pas pris (pour le tour suivant)
- T-c, grave : au ch. 12, étape 3, « touche 4 cases » de maracas validée dès le premier coup (« Déjà fait ! » à 1/4).
- T-c, grave : carte « rien à toucher » alors que 3 maracas manquent.
- T-d : P6-grille, la consigne dépliée couvre toute la grille (écran 42).
- T-d : le piano roll défile et zoome sous le doigt (note parasite) ; F aigu et F grave portent le même nom ; « キックの下では弱めに » validée sans vérifier la vélocité.
- Finitions : textes coupés à 130 % et 200 % (cartes, compteurs, barre du haut, nom du projet) ; « Motif posé ! » à moitié caché ; compteur « Encore 5 → 6 » ; flèche ← depuis le Bravo ; guillemets doublés en coréen ; D3 coupure en plein mot au ch. 4 ; D4/P7 8 cases sur 2 mesures au ch. 4.
- Banc : human-act n'accepte qu'un niveau sous `controle/` (dossiers `final-<x>/a`).

Verdict : tour 8 clôturé. 0 chapitre bloqué sur les quatre sièges. Deux graves neufs (T-c) et un défaut d'avant encore présent (P6-grille) sont mis de côté pour le prochain tour.
