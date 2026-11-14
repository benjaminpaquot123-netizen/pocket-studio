# Relecture tour 5, zone « retest »
Relu : 1bae09e501791372cd2f124abaef240f09e468af

Diff lu : db66a3ce..1bae09e5 (rouge eb75dfb3, vert c805a807, docs). Suite `com.pocketstudio.bentest.*` rejouée ici : BUILD SUCCESSFUL.

## Contrôles
1. Rien ne fuit dans l'appli réelle : tout le code touché est dans `app/src/benTest` (sauf tests sous `app/src/test`) ; aucun fichier `main` modifié. OK.
2. Filtre `isShown` de `InAppDriver.roots()` : les dialogues, feuilles et popups ont leur propre fenêtre dont la vue racine est visible (isShown vrai : le parent est le ViewRootImpl) ; la bannière de l'oreille vit dans la fenêtre du studio (Tracker), donc reste lue tant que le studio est devant. Seul le décor d'une activité arrêtée est INVISIBLE sous Android, c'est bien ce qui est écarté. Le filtre est déjà employé par `ecran()` (l.118) et Robot (l.517) : pas de nouvelle exigence. Aucune régression trouvée.
3. Lecture des anciens carnets/zips : `Carnet.lire` ignore les lignes à ≠ 4 champs ou à date illisible ; `Retest.repli` enveloppe le zip dans `runCatching` (fichier absent, tronqué, sans `carnet.tsv` : repli sur « rien »), `Retest.lire` aussi. Pas de plantage.
4. RetestTest : les deux assertions changées attendent « ouverture » en tête et testent toujours l'ordre, « écran éteint » en dernier et le fait que seules les étapes choisies tournent (la liste exacte est comparée). Réelles.
5. Tests : `RetestApresEclairTest` rejoue la séquence du téléphone (essai éclair puis 5 étapes), et son rouge cherche les messages du téléphone (« nothing tagged launch », « is not there once ») ; le commit rouge ne touche que des tests et compile sans le code neuf. Les tests de liste (repli carnet, rien coché, ligne entière) vérifient des valeurs précises.

## Constats
- Mineur (non bloquant) : le test simule le masquage de la fenêtre arrêtée à la main (`montrer`, décor INVISIBLE), pas par le vrai cycle Android ; le rapport l'annonce « non prouvé sur téléphone ». Honnête, à confirmer au prochain passage de Benjamin.
- Mineur : aucun test direct d'un zip de rapport corrompu ou sans `carnet.tsv` pour `Retest.repli` (le code est sûr à la lecture, mais non couvert) ; `readBytes` d'une entrée géante n'est pas protégé contre un manque de mémoire (peu probable).
- Mineur : si le dernier carnet est celui d'un essai éclair tout vert, rien n'est coché et l'aide s'affiche : voulu (décision notée).

rejeu changé : aucun. Rejeu retiré : aucun.

Verdict : 0 bloquant, 0 grave — fusion possible
