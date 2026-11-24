# Plan d'économie de quota (24 novembre 2026)

Demande de Benjamin : « fais un plan d'économie de quotas, tout en gardant la vitesse de croisière qu'on a, fais-le auditer par Fable, et applique-le ».
Base : `docs/audits/ultra/METHODE-V4.md` §4 (mesure du 15 novembre : coût ≈ nombre d'actions × taille de la mémoire ; les relectures de mémoire font 68 à 80 % du coût).

## 0. Constat du jour (mesuré par `get_session` sur la discussion principale)
- Discussion principale (le chef) : mémoire à 158 k sur une fenêtre utile de 180 k (compactage à 165,6 k). Total depuis octobre : 23 milliards de jetons relus, 849 M réécrits, 75 M écrits.
- Chaque action du chef relit donc entre 100 et 165 k. Une notification lue, un message à Benjamin, un `get_job_logs` : chacun coûte une action pleine.
- Aujourd'hui, le chef fait aussi du travail de « petite main » : lire les notifications une à une, relayer des messages entre équipes, surveiller une CI, relancer un portail coupé, lire des journaux bruts (un journal de job de 80 lignes, une release de 133 k caractères).
- Les équipes (discussions neuves en Opus) sont déjà courtes et fermées à la fin du lot : le levier 1 de la méthode v4 est en place pour elles.

## 1. Les leviers, classés par gain estimé

### L1. Un « second » qui porte la machinerie, le chef ne garde que les décisions (gain estimé : 30 à 40 % du coût du chef)
- Une discussion neuve, « second », en Opus, mémoire courte (renouvelée dès 120 k : elle écrit son état dans `docs/audits/ultra/EN-COURS.md` puis le chef en ouvre une neuve).
- Elle fait : portails (`fusion-equipes.sh`), pré-vols, pushes sur l'intégration après portail vert, attente des CI, lecture des CI rouges, relais des messages entre lots, alias `eq/tour-9-<zone>`.
- Elle n'écrit au chef qu'un message groupé par événement utile : « lot X entré, CI verte » ou « lot X refusé : raison en une ligne ».
- Le chef garde : la parole à Benjamin, les fiches de lot, les décisions, l'ordre des lots.
- Raison : une action du second coûte 30 à 80 k au lieu de 160 k.
- Retour arrière : si le second se trompe deux fois sur un même portail, le chef reprend la main.
- Limite à vérifier par l'audit : CLAUDE.md dit que seule la discussion principale fusionne et pousse sur l'intégration. Il faut donc l'accord écrit de Benjamin (le présent plan, une fois validé) et une ligne dans CLAUDE.md.

### L2. Messages des équipes : un seul, à la fin, au chef ou au second (gain : 5 à 10 %)
- Plus de relais d'information d'une équipe à l'autre par le chef. Une équipe qui a besoin du travail d'une autre fusionne sa branche (`git fetch origin eq/...`) et lit son rapport.
- Les fiches nomment les branches voisines dès le départ.

### L3. Le chef ne lit jamais un journal brut (gain : 3 à 5 %)
- Interdits pour le chef : `get_job_logs`, `get_release_by_tag` sans filtre, `cat` d'un journal entier.
- Pour une CI : `scripts/bench/ci-etat.sh` ou `ci-rouge.sh` (une ligne par job). Le reste part à un sous-agent ou au second.

### L4. Fable pour les recherches et audits, sur discussion neuve (gain : 5 à 10 % du quota Opus)
- Fable a son propre compteur. Recherche, audit de plan et relecture « œil neuf » d'un gros lot partent sur Fable, en discussion neuve, avec une fiche courte.
- Les lots de code restent en Opus. Ils écrivent beaucoup et relisent beaucoup.
- Retour arrière : si Fable atteint sa limite, Opus reprend, et une ligne est dite à Benjamin.

### L5. Effort adapté au rôle (gain : 5 à 15 % sur les rôles concernés)
- Lots mécaniques (rangement de documents, régénération de fichiers, relais) : effort « medium ».
- Lots de code du moteur ou du tuto et relectures : effort par défaut.
- Raison : moins de réflexion écrite, donc moins de jetons écrits (×5).

### L6. Attente par événement, déjà la règle (rappel)
- Pas de `sleep`, pas de minuterie sur une grosse discussion. Les tâches longues (portail : 30 à 90 min) sont lancées en arrière-plan avec un délai assez long pour ne pas être coupées. Le portail coupé à 30 minutes le 24 novembre a coûté un tour complet.

### L7. Écoutes groupées pour Benjamin (gain : temps de Benjamin, et moins d'allers-retours)
- Un seul dossier d'écoute par paquet de lots (A/B morceau entier), pas un envoi par lot.

## 2. Ce qui ne bouge pas (vitesse et sécurité)
- Deux équipes en parallèle minimum quand c'est possible ; le Grand remplacement garde son ordre (plan v2).
- Relecture neuve, pré-vol, portail, CI verte avant toute entrée ; jamais de push sur `main`.
- Le chef reste le seul à parler à Benjamin.

## 3. Mesure
- Avant : chiffres `get_session` du chef ci-dessus (24 novembre, 16 h 15).
- Après un paquet de lots du Grand remplacement : même mesure pour le chef, le second et les équipes. On compare le coût par lot entré.

## 4. Ordre d'application
1. Audit par Fable (discussion neuve), corrections.
2. Ligne dans CLAUDE.md (rôle du second, L3), ligne dans `docs/DECISIONS.md`.
3. Ouvrir le second et lui confier les portails en cours (ci-rapide, robot1b, gr-l0).
4. Fiches suivantes du Grand remplacement : L2, L4, L5.

## 5. Version appliquée après l'audit Fable (`PLAN-ECONOMIE-QUOTA-AUDIT.md`)
- Tous les correctifs de l'audit sont pris : gains revus (L1 20 à 35 %, L5 2 à 5 %, L7 sans gain de quota), L2 en lecture seule, L3 étendu (> 5 k jetons), L6 à 2 h, neuf garde-fous du second, « relais entre lots » retiré de L1.
- L0 ajouté : compactage plus tôt (fenêtre 140 k, compactage vers 116 k) et fiche de reprise avant une longue attente.
- L4 (Fable) : au cas par cas ; Benjamin (24 novembre) autorise le chef à lancer Fable seul sur un problème délicat.
- Mesure : on compte aussi les réveils à froid du chef et la part de machinerie dans ses actions.
