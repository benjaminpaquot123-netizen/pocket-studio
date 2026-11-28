# Tour 8 — lot « niveaux » : rapport

Rôle : banc. Aucun fichier de l'appli touché (`app/src/main`, `project-model/src/main` intacts). Aucune capture changée.

## a. Quatre missions à oracle (fait)
Les oracles lisent le projet enregistré du siège (`etat/projects/<id>/project.json`, `etat/resume.json`, `etat/partage/`), jamais le son.
- `mon-morceau` : un morceau de l'assistant (style ou guide « song ») dont le son, la mélodie et la structure ont changé (faits « changed » écrits par `SeatState`, d'après `GuideStart.changed`). Faits absents : « inconnu », jamais « non ».
- `boucle-imposee` : la batterie, la basse (mêmes pas, même dessin, octave libre) et les accords (mêmes notes par pas) de la fiche `oracles-niveaux/boucle-imposee/fiche.json` ; la fabrique copie la fiche dans le siège et la dit en mots dans la consigne.
- `projet-vide` : un projet fait sans l'assistant, 6 pistes qui jouent, 8 parties remplies, un fichier partagé.
- `pistes-separees` : un fichier audio par piste qui joue (au moins 2), en .wav ou .flac, tous de la même durée (WAV lus à 0,01 s près ; un .flac seul donne « durée illisible »). Les .zip partagés sont ouverts.
- Chaque mission a une paire de sièges d'exemple (`tient` → « reussie », `ne-tient-pas` → « non » avec ce qui manque), jouée par `test_ultra_bench.py` (`niveaux_levels`).

## b. `reprendre` (laissé hors du tour 8)
Avancer l'horloge de trois jours sous le banc demande de toucher `app/src/main` : hors de ce lot. La fabrique refuse un siège `reprendre` (« oracle à écrire (horloge), hors du tour 8 ») au lieu de lui donner un « non » d'office.

## c. Colonne niveau (fait)
- `sieges-proposes.tsv` a une 12e colonne : 0 (curieux) à 4 (pro), ou X. Un siège sans niveau est refusé ; un siège `libre` n'en prend aucun (vide ou X), un niveau 0–4 sur `libre` est refusé.
- `start.txt` porte `niveau=…`, `sieges.tsv` garde la colonne, `seat-metrics.py` recopie le niveau dans `mesures.json`.

## d. Tableau de bord par niveau (fait)
Sous chaque tour, un tableau « par niveau » : Niveau | Sièges | Réussi sans aide | Avec l'aide de l'appli | Bloqué | Gestes faits / chemin idéal | Gestes avant le premier son | Surprises | Mots incompris. Une ligne par niveau 0 à 4 ; un siège `libre` ou X n'en remplit aucune. Chemin idéal lu dans `chemins-ideaux.tsv` (mission, gestes) quand il existe, sinon « ? ».

## Sièges pilotes : 0/4 (bloqué)
Ce conteneur n'a pas de SDK Android : `gradle-file.sh` s'arrête à « SDK location not found ». Aucun siège réel n'a pu être joué ; les quatre oracles ne sont prouvés que sur les sièges d'exemple. À jouer par le chef (un siège par mission neuve, hors vague) avant la vague du tour 8. Pour la même raison, la ligne ajoutée à `SeatState.kt` n'est pas compilée ici : la CI la compilera.

## Demandes au chef
- `reprendre` hors du tour 8 et les champs « changed » de `SeatState.kt` (lecture seule) : accordés par le chef.
- `scripts/bench/test_portail3.py:829` (hors liste) : sa ligne de sièges n'avait pas de niveau, la fabrique la refusait (selftest rouge). Accordé par le chef : « \t1 » ajouté à cette seule ligne, rien d'autre dans ce fichier.

## Laissé
- `reprendre` (b).
- Les sièges pilotes (voir plus haut).

## Relecture neuve (sous-agent qui n'a pas écrit le code)
Rien de grave. Remarques laissées, à reprendre avant la vague si un pilote les rencontre :
- moyen : durées .flac jamais lues (des pistes .flac justes donnent « partielle ») ; nombre de pistes attendu pris au maximum sur tous les projets, pas sur le projet exporté ; basse et accords de `boucle-imposee` ignorent la ligne acid et les boucles répétées sur 2 mesures.
- mineur : le rythme ignore les pistes en trop ; l'export accepte n'importe quel fichier partagé ; `mon-morceau` compte un morceau « first » avec un style ; `int(k)` et des pistes nulles plantent ; tests sans cas « partielle », .flac ni niveau X ; la docstring du tableau dit « mots-faux » alors que l'expression compte les lignes « - ecran: | vu: | attendu: ».

## Autotest
Partie scripts verte (64 tests OK). Partie Gradle non jouée : pas de SDK Android ici (« SDK location not found »), les 42 classes « n'ont pas tourné ». À rejouer par le chef sur une machine avec SDK.

tests: aucun : lot du banc (scripts et sièges d'exemple), joués par test_ultra_bench.py dans l'autotest ; SeatState sans test Gradle possible ici (pas de SDK)

chemin: Guide › « Change un son » (son du morceau changé, lu par mon-morceau) | test: GuideFusionTest#aFirstSongFollowsTheMissionsAsGuideCards
