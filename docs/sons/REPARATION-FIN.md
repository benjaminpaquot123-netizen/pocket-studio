# Réparation finale de l'outil de mesure (branche `eq/son-fin-outil`)

Base : `eq/passe2-critique` eefa5745d. Tout est prouvé sur des fichiers fabriqués, aucun son acheté.

1. **Fausse alerte du lanceur (M7)** — cause : le compte des lignes « style du pack » ignorait la règle « titre après le tempo », d'où « 370 contre 369 » ; test : rouge 8e984d26 puis vert d77e10f3 ; résultat : plus d'alerte sur une table juste, l'alerte reste sur un vrai écart.
2. **Trois mutations réécrites** — cause : deux mutations sautées (le texte visé avait changé) et un mutant « tonique seule » qui survivait ; test : rouge d2593404 puis vert c19d7b52 ; résultat : les trois mutants sont tués.
3. **Détail de la justesse de tonalité** — cause : un seul taux global, impossible de voir où ça casse ; test : rouge 626846c0 puis vert d103ea30 ; résultat : `audit4/accuracy.py --tables` donne exact / même tonique / relative / autre par type et par RAR.
4. **Note des coups de batterie (m2)** — cause : une lettre de variante en fin de nom (« Snare A », « Kick 01 B ») était lue comme une note, et l'étiquette n'était pas gardée ; test : rouge 741114cf puis vert f9ca86b3 (et 41706873 pour le rejeu b1) ; résultat : lettre seule ignorée sauf pour les 808, nouvelle colonne `note_tag` dans la table des coups.
5. **Libellé « 11.5 MB » du rejeu b5** — cause : le libellé avait déjà été corrigé dans la base (9a2564d8), donc pas de vrai rouge possible ; test : garde ajoutée en vert seul 0425ec04 (libellé comparé à la taille mesurée, à 2 %) ; résultat : l'ancien libellé faux ferait échouer b5.
6. **Ligne end_cut des rouges attendus** — cause : déjà retirée dans la base (`EXPECTED_RED` vide, `erreurs_admises.csv` ne garde que l'en-tête) ; test : aucun ; résultat : rien à faire.
7. **Tempo à moitié ou au double (M6)** — cause : 62,5 pour une boucle à 125 comptait comme une erreur pure ; test : rouge a2a2cd29 puis vert 607c29db ; résultat : nouvelle colonne `error_fold_pct` (erreur après repli ×2 / ÷2), le taux brut et la porte « utilisable » ne changent pas, `accuracy.py --tables` montre aussi exact / moitié-double / autre pour le tempo.
8. **Auto-tests** — tous les self-tests verts (0 échec), rejeux b1 à b8 : 0 en échec sur 10 ; banc de mutations rapide : 40 tués, 0 survivant, 66 non lancés (budget de 9 min dépassé, le banc entier ne tient pas en 10 min).
