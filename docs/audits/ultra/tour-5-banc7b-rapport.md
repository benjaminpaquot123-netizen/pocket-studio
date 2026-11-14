# Tour 5, lot « banc7b » : fin de l'étape 3, second paquet (7.6, 7.7, critères 1 et 8)

Branche : `claude/socle-du-banc-alqcr3` (contient socle et banc7, fusionnés dans l'intégration ; base de l'étape 4 refusionnée). Décisions : `docs/decisions/tour-5-banc7.md` (lignes 7.6, 7.7, BenchAmeneesTest).

| Point | Fait | Preuve |
|---|---|---|
| 7.6 | Essai à blanc de bout en bout par les scripts : rejeu non tenu → correction → tenu → rapport → relecture → pré-vol → portail (paquet 2) → `marque-corrige.sh` → tableau ; 4 casses des scripts trouvées et réparées | `tour-5-essai-journal.md` ; paires e7476667/a16cc034, 45cda86d/8c07513b, 77310338/1d7d8de0, 49c5ccd8/45d79172 |
| 7.7 | Un seul autotest par zone (celui du lot de rejeux, sur le code fusionné) : 3 zones en 2511 s (42 min, cible 60 ; environ 81 min avant) | f95b81b1/9fcbbd9e ; journal de l'essai |
| Test capricieux | `BenchAmeneesTest#aTapDuringTheExampleIsNotCounted` (1 rouge sur 7) : course entre le fil réel de l'exemple et l'horloge du banc, réglée dans le test (l'appli ne compte bien aucune touche pendant l'exemple) | 5f52a0bf/97b71eb6 ; 3/3 rouge au pire cas avant, 10/10 vert sous charge après |
| Pré-vol `--ci` | Les liens (« T ») et `rejeux/_echecs/` ne comptent plus comme travail non commité (signalé par la discussion principale) | test_pre_vol.sh, rouge puis vert 0b25ef87 |
| Critère 1 | **Tenu** : 10 autotests verts de suite sur 0b25ef87, chacun 196 verts, 0 rouge, 0 quarantaine, 43 sautés connus, environ 12 min | `sieges/tour-5/critere1/bilan.txt`, résumé du 10e |
| Critère 8 | **Presque** : voir ci-dessous | `sieges/fumee-finale/` |

## Critère 8 : le tour de fumée final

4 sièges aux briefs de `fabrique-sieges.py` (ids P du catalogue) : A a (hindi, petit téléphone, 130 %, premier morceau), A b (voix, français), A c (Pad Live, tablette, Pro, anglais), A d (libre, allemand, 200 %). Autotest du lot vert (178 verts, 0 rouge) avant ; 4 testeurs neufs.

- 0 incident « banc » (aucun testeur n'en a écrit ; `incidents-banc.tsv` vide hors sortie de siège ci-dessous) ; 0 abandon.
- Sortie de siège : le détecteur a tourné (instantané d'avant pris par `human-tester.sh`) ; aucun testeur n'est sorti ; il signale les 5 fichiers que j'ai écrits ensuite à la racine du tour (fusion et tableau), ce qui montre qu'il voit.
- « revient ? » : 1 candidat (A/c#13, tiroir « More »), lu : non (≠ T1-40, traductions) ; ligne C du tableau à 0.
- **1 gel, non tranché** : siège A d, au geste #0 (ouverture, allemand, 200 %), 789 ms (seuil 700) dans `WholeWords.getFontSize`, le code de l'appli qui ajuste la taille du texte. Sur le banc, la mise en page du texte est bien plus lente que sur un téléphone : je ne peux pas dire ici si c'est le banc ou l'appli. À trancher (essai téléphone : ouvrir l'appli en allemand, texte 200 %), ou garde du banc avec preuve sans banc. Tant qu'il n'est pas tranché, le critère 8 n'est pas tenu à la lettre.
- Missions (oracle `mesures.json`) : A a et A b réussies ; A c partielle (premier son en 2 gestes, visé 1 : l'appli) ; A d partielle (le gel ci-dessus).
- Tableau : C, F, J remplies ; D et E non (4 sièges seulement, 12 promesses vues sur 120) ; G « ? » = relecture neuve (humaine) ; H = CI et téléphone (hors banc).
- Défauts de l'appli vus (pour les équipes) : Mode simple à 5 onglets au lieu de 4 (P025) ; bouton du style hors écran à 200 % ; Guide qui cache les pistes qu'il demande de toucher (A a) ; pas de piste voix visible pour poser sa voix (A b) ; tiroir « More » refermé après une prise (A c).

tests: aucun : lot du banc (scripts, autotests, tour de fumée), les tests touchés sont joués par l'autotest

## Liste des chemins

- chemin: défi du Pad Live, tapes pendant l'exemple | test: BenchAmeneesTest#aTapDuringTheExampleIsNotCounted
