# Audit du plan d'économie de quota (24 novembre 2026, vérificateur neuf)

Lu : `PLAN-ECONOMIE-QUOTA.md` (d84c3df8), `ultra/METHODE-V4.md` §4, `CLAUDE.md`. Rien n'a été mesuré de neuf : les calculs partent des chiffres du plan.

## Le point que le plan ne voit pas : les réécritures
Avec les poids de la méthode v4 (relu ×0,1, réécrit ×2, écrit ×5), le compteur du chef donne : 23 000 M relus = 2 300 M d'unités (53 %), 849 M réécrits = 1 698 M (39 %), 75 M écrits = 375 M (9 %).
- Le plan ne vise que les 53 %. Or un réveil du chef après plus d'une heure réécrit toute sa mémoire : à 160 k, environ 320 k unités, soit **vingt actions à chaud** (16 k chacune).
- Conséquence : « coût ≈ actions × mémoire » sous-estime les réveils à froid. Moins d'actions du chef, mais plus espacées, peut coûter autant.

## Verdict par levier
- **L1, le second : garder, corriger le gain.** 30 à 40 % ne tient que si le second écrit au chef **un message par paquet**, pas par lot, et si la machinerie fait bien la moitié des actions du chef (non mesuré). Crédible : 20 à 35 % du coût du chef. Vrai atout, non écrit dans le plan : les attentes de plus d'une heure (portail de 30 à 90 min) se paient à 60 k au lieu de 160 k.
- **L2, un seul message : garder, corriger.** Gain 5 à 10 % plausible (chaque relais = une action pleine). Mais « fusionne la branche voisine » fait entrer du code non vérifié dans le lot et casse « hors de sa liste, on ne touche à rien ». Écrire : lecture seule (`git show origin/eq/...:rapport`) ; fusion seulement si le plan du tour l'ordonne.
- **L3, pas de journal brut : garder, gain sous-estimé.** Une release de 133 k caractères, c'est environ 35 k jetons écrits ×2, relus à chaque action, et un compactage avancé. Étendre : tout résultat d'outil de plus de 5 k jetons est filtré ou confié à un sous-agent. Les deux scripts cités existent.
- **L4, Fable : corriger.** Ce n'est pas une économie mais un déplacement vers un autre compteur, déjà vidé le 15 novembre. Conflit avec CLAUDE.md (« Fable seulement quand il est nécessaire, en demandant d'abord à Benjamin »). Garder le cas par cas, ou poser la question à Benjamin une fois.
- **L5, effort : garder, gain à baisser.** Le texte écrit ne fait que 5 à 10 % du coût : gain réel 2 à 5 % sur ces lots, pas 5 à 15. Demande une ligne dans CLAUDE.md (l'effort ne se règle que sur consigne écrite). Jamais sur le second.
- **L6, attente par événement : garder, préciser.** La coupure à 30 minutes est le délai par défaut d'une tâche d'arrière-plan : écrire le délai maximal (2 h) dans la fiche du second pour tout portail.
- **L7, écoutes groupées : garder.** Aucun gain de quota ; ne pas le compter.

## Garde-fous à écrire pour le second (L1)
1. Un seul pousseur de l'intégration à la fois, nommé dans `EN-COURS.md` (« pousseur : second, session … »). Tant que la ligne existe, le chef ne pousse pas ; il la retire avant de reprendre la main.
2. Jamais deux seconds : l'ancien écrit son état, s'archive, puis le chef ouvre le neuf. Pas de relève pendant un portail en cours.
3. Le second pousse sans forcer, seulement une tête sortie verte du portail ; jamais `main`, jamais de PR, jamais d'APK, aucune fiche, aucune décision d'ordre.
4. `EN-COURS.md` et `PROGRESS.md` dans le même commit que chaque fusion : la règle du chef passe au second.
5. Les équipes écrivent « fini » au second. Son identifiant change à chaque relève : le lire dans `EN-COURS.md`, et écrire au chef si l'envoi échoue.
6. CI rouge : le second réveille lui-même le lot avec l'extrait de l'artefact, sinon un aller-retour par le chef ralentit.
7. Avant tout point à Benjamin, le chef vérifie lui-même la tête et la CI (`ci-etat.sh`, une ligne) : le message du second est une information, pas une preuve.
8. Retirer « relais des messages entre lots » de L1 : contredit L2.
9. CLAUDE.md : ajouter le rôle « second » à la liste des rôles et corriger « fusionne seule, pousse seule ». C'est une règle de Benjamin : une ligne dans le prochain point, et dans `DECISIONS.md` avec le retour arrière.

## Leviers oubliés
- **L0, la mémoire du chef elle-même (fort gain, aucun risque de coordination).** Le chef vit entre 100 et 165 k. Compacter plus bas (`/autocompact` vers 120 k) et écrire la fiche de reprise avant chaque longue attente : toutes ses actions et tous ses réveils à froid coûtent un tiers de moins. À mesurer sur un paquet : plus de compactages, donc quelques relectures de fichiers en plus.
- **Mesure du §3** : compter les réveils à froid du chef (écart de plus d'une heure) et, avant le second, la part de machinerie dans ses actions ; sinon le gain de L1 restera une impression.

## Ce qui peut ralentir la croisière
- L1 sans les garde-fous 2 et 6 (relève en plein portail, rouge qui passe par le chef). Le second ne compte pas parmi les « deux discussions au travail ».
- L2 avec fusion des branches voisines : conflits et relectures à refaire.
- L4 si la limite de Fable tombe au milieu d'une relecture : travail à refaire en Opus.
- L5 sur un lot « mécanique » mal jugé : un lot refait coûte plus que l'effort gagné.

**Ordre conseillé :** L0 et L3 tout de suite (sans risque), L1 avec ses neuf garde-fous, L2 corrigé, L6 précisé, L5 en dernier ; L4 seulement après l'accord de Benjamin.
