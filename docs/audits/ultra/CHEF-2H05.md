# Feuille de route du chef pour la nuit du 24 novembre (départ 2 h 05)

But : lancer tout le plan `PLAN-APRES-ROBOT.md` (sa section 7 fait foi) sans bug, sans attente inutile, sans perdre de quota. Le chef est la discussion principale (`session_01NqonxZBBysqqWBbnsC2VTP`) ; le rappel `trig_01CN1EkYPspt76rNhzHDSjSf` la réveille.

Consigne de Benjamin (2026-11-23, soir) : « Tout ce qu'on vient de parler, tu lances tout à 2h05 […] c'est toi le chef d'orchestre. » Il est à l'hôtel : pas de tablette, pas de test sur appareil avant son retour.

## 0. Pièges connus avant de commencer

1. **Disque presque plein** : 463 Mo libres sur 252 Go (23 novembre, 18 h). Sans place, aucune compilation ne passe. À 2 h 05, première action : libérer au moins 15 Go (caches Gradle des ateliers morts, `build/` des anciens `wt-*`, rapports dézippés déjà lus). Jamais `/home/user/fx-int2`, `/home/user/bt-build`, ni le dossier `secrets`.
2. **La base de A1 n'existe qu'ici** : `eq/bentest-robot-2` (2c0cab99c) est dans `/home/user/bt-build`, pas sur GitHub. Une discussion neuve ne peut pas la récupérer. Donc **A1 tourne en sous-agent du chef**, dans `/home/user/bt-build`, comme le robot qui se compile en local.
3. **La clé Freesound n'existe qu'ici** (`scratchpad/secrets/freesound.key`) : le chantier des sons (F0) tourne aussi en sous-agent du chef. Jamais la clé dans une fiche, un commit, un journal ou un message.
4. **La mémoire du chef est déjà grosse** : le chef garde les conclusions, pas les journaux. Toute longue lecture part à un sous-agent. Il ne lit jamais un rapport ou un journal entier.
5. **Pousser** : le chef pousse seulement sur `ccr-80af1a1b-mn0wec`. Les discussions poussent leur branche `eq/…` neuve, jamais l'intégration. Jamais la PR #60.

## 1. Vérifications de départ (5 minutes, dans cet ordre)

1. Place disque (point 0.1).
2. `/home/user/fx-int2` propre et à jour avec `origin/ccr-80af1a1b-mn0wec`.
3. CI de la tête d'intégration : `scripts/bench/ci-etat.sh`. Si elle est rouge, une ligne de note, et C et D partent quand même (elles ne fusionnent qu'après une CI verte).
4. La clé est présente (test de taille du fichier, sans l'afficher).
5. Relire la section 7 du plan, puis seulement les fiches.

## 2. Ce qui démarre à 2 h 05

| Qui | Où | Modèle | Rend au chef |
|---|---|---|---|
| E Audit des scénarios et des compteurs | discussion neuve, lecture seule | Opus 5.5 | une page de conclusions |
| A1 Robot, réparations | sous-agent du chef, `/home/user/bt-build` | Opus 5.5 | sha local + tests verts |
| B1 Moteur, mesure | discussion neuve | Fable 5.2 (voulu par Benjamin) | tableau de charge + sha |
| C Fluidité | discussion neuve | Opus 5.5 | sha poussé `eq/…` |
| D Service audio | discussion neuve | Opus 5.5 | sha poussé `eq/…` |
| F0 Essais d'écoute | sous-agent du chef, bloc-notes | Opus 5.5 | fichiers MP3 + liste d'origine |

Au plus deux sous-agents du chef en même temps (A1 et F0). Les discussions neuves tournent sur leurs propres machines.

**F0, essais d'écoute** (Benjamin, 2026-11-23 : « il me faut plus d'essais à écouter pour voir si tu es sur la bonne voie ») : le pilote passe de 3 à 8 styles. Pour chaque style, un MP3 d'environ 30 s : « avant » (le son actuel de l'appli), une seconde de blanc, puis « après » (le même rythme avec des sons synthétisés ou CC0 Freesound). Les deux moitiés au même volume. Les styles : trap/drill, Jersey Club, minimale/tech house, afro house/amapiano, UK garage, techno, lo-fi/boom bap, pop/reggaeton. Plus un fichier « kicks et basses », coup par coup. Rien dans git ; une liste d'origine par fichier ; les règles d'origine de la section F du plan (pas de disque, pas de break connu, grincement de vrai lit en CC0 permis). Envoi à Benjamin au réveil, pas la nuit.

Les fiches des discussions suivent `GABARIT-FICHES-V3.md` : rôle, branche de départ, fichiers permis (tableau « Qui a quel fichier »), constats, preuve attendue, ce qu'il ne faut pas toucher, message de fin en une ligne (« fini <sha> » ou « bloqué : … »).

## 3. Verrou E → catalogue

- A1 ne crée aucun scénario neuf.
- Quand E rend, le chef lit sa page, en tire au plus 10 règles, et les met dans la fiche de A3 (discussion neuve, plus tard).
- Si une conclusion de E touche une réparation de A1 en cours (un oracle), le chef la passe à A1 tout de suite.

## 4. Surveiller sans gaspiller

- Les discussions écrivent au chef (« fini » ou « bloqué ») ; les sous-agents réveillent le chef à la fin. Aucune minuterie périodique.
- Une seule minuterie de secours, à 50 minutes, seulement si rien d'autre ne peut prévenir.
- Alarmes et réaction :
  - disque sous 2 Go : nettoyer ;
  - test Gradle figé plus de 20 min : tuer, relancer une fois ;
  - équipe sans commit ni note depuis 45 min : regarder sa note, recadrer ou arrêter ;
  - même rouge deux fois sur un lot : prévenir Benjamin en une ligne et proposer Fable sur une discussion neuve (il décide).
- Pour lire une discussion : `list_events` avec `kinds: ["assistant","result"]`, jamais tout le fil.

## 5. Fusionner

- Les deux fiches de `docs/AIDE-FUSIONS-TESTS.md` font foi. Fusion par le portail (`scripts/bench/portail-file.sh`), trois lots au plus par paquet, CI complète verte avant le paquet suivant.
- Paquet 1 : robot (`eq/bentest-robot-2` + A1) et le lot fable5 (`ccr-56d43d5b-vfdmoc`, 0758fd6cf, seuil noté dans `DECISIONS.md`). Le robot passe d'abord sur une branche `eq/…` poussée par le chef, avec sa CI.
- Paquet 2 : B1, D, C.
- Rien n'entre sur une base rouge. On ne fusionne que la tête poussée.
- Le robot v8 se compile en local, depuis une tête d'intégration à CI verte seulement ; le commit joué est écrit dans le rapport.

## 6. Parler à Benjamin

- La nuit : rien, sauf un blocage qu'il est seul à pouvoir lever.
- Au réveil (heure belge), après lecture de la CI : le compteur en trois lignes, les essais d'écoute, ce qu'il doit faire (un test à la fois, quand il a ses appareils), et « Ce qui n'est pas vert ».
- Pas de test sur la tablette tant qu'il est à l'hôtel.

## 7. Clôture de la nuit

- Chaque discussion fermée à la fin de son lot (`archive_session`).
- `EN-COURS.md` et `PROGRESS.md` à jour, poussés.
- `/home/user/bt-build` gardé tant que le robot n'est pas fusionné.
